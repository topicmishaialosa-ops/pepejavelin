package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.event.events.screen.ScreenRenderEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.mixin.accessor.AbstractContainerScreenAccessor;
import org.blade.utils.autobuy.AutoBuyConfig;
import org.blade.utils.autobuy.AutoBuyManager;
import org.blade.utils.autobuy.AutoBuyUtil;
import org.blade.utils.autobuy.ItemBuy;
import org.blade.utils.inventory.InventoryUtil;
import org.blade.utils.text.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AutoBuyFeature extends Feature {
    public static final AutoBuyFeature INSTANCE = new AutoBuyFeature();

    private final BooleanSetting reissueSetting = register(new BooleanSetting("Авто-перевыставление вещей", false));
    private final ButtonSetting editorButton = register(new ButtonSetting("Открыть редактор", "Открыть", null));
    private final List<ItemStack> reissueItems = new ArrayList<>();
    private final List<ItemBuy> entries = new ArrayList<>();
    private AutoBuyManager manager;
    private long lastReset = System.currentTimeMillis();
    private int quietTicks;
    private int reissueTicks;
    private ItemStack lastBought;
    private boolean needOpen;
    private boolean ahWaiting;
    private int lastSyncId = -1;
    private String cachedServerCategory = "";

    private AutoBuyFeature() {
        super("AutoBuy", "Автоматически скупает выбранные предметы по заданной цене", FeatureCategory.MISC, BindSetting.UNBOUND);
        this.editorButton.setChangeListener(() -> Minecraft.getInstance().gui.setScreen(new AutoBuyEditorScreen(0)));
        AutoBuyConfig.loadAutoBuy(this.entries);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.needOpen = true;
        this.ensureEntries();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.needOpen = false;
        this.lastBought = null;
        this.reissueItems.clear();
        AutoBuyConfig.saveAutoBuy(this.entries);
    }

    private void ensureEntries() {
        String category = serverCategory();
        if (category.equals(this.cachedServerCategory)) {
            return;
        }
        this.cachedServerCategory = category;
        this.entries.clear();
        if (category.equals("funtime")) {
            this.entries.addAll(autoBuyManager().getFuntime());
            this.entries.addAll(autoBuyManager().getVanilla());
        } else if (category.equals("hollyworld")) {
            this.entries.addAll(autoBuyManager().getHollyworld());
            this.entries.addAll(autoBuyManager().getVanilla());
        } else {
            this.entries.addAll(autoBuyManager().getVanilla());
        }
        AutoBuyConfig.loadAutoBuy(this.entries);
    }

    private AutoBuyManager autoBuyManager() {
        if (this.manager == null) {
            this.manager = new AutoBuyManager();
        }
        return this.manager;
    }

    private String serverCategory() {
        if (AutoBuyUtil.isFuntimeServer()) {
            return "funtime";
        }
        if (AutoBuyUtil.isHollyworldServer()) {
            return "hollyworld";
        }
        return "vanilla";
    }

    private boolean elapsed(long delay) {
        return System.currentTimeMillis() - this.lastReset >= delay;
    }

    private void resetCounter() {
        this.lastReset = System.currentTimeMillis();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.player.tickCount >= 220 && this.ahWaiting && !(mc.gui.screen() instanceof ContainerScreen) && this.needOpen && mc.player.tickCount % 20 == 0) {
            mc.player.connection.sendCommand("ah");
            this.ahWaiting = false;
        }

        this.quietTicks++;
        this.reissueTicks++;
        if (!(mc.gui.screen() instanceof ContainerScreen screen) || !this.needOpen) {
            return;
        }

        this.ensureEntries();
        var handler = screen.getMenu();
        String title = screen.getTitle().getString().replaceAll("\u00a7.", "").toLowerCase(Locale.ROOT).trim();
        boolean buy = mc.player.tickCount % 2 == 0;
        boolean reissue = this.reissueSetting.getValue() && this.elapsed(10000L);
        if (title.contains("аукцион")) {
            boolean found = false;
            int limit = Math.min(45, handler.slots.size());
            for (int i = 0; i < limit; i++) {
                ItemStack stack = handler.slots.get(i).getItem();
                ItemBuy find = findBuyable(stack);
                if (find != null && buy) {
                    found = true;
                    this.lastBought = stack.copy();
                    this.click(i, true);
                    break;
                }
            }
            if (!found && !reissue && this.lastSyncId == handler.containerId) {
                this.click(49, Math.random() * 100.0 < 25.0);
                this.lastSyncId = -1;
            }
        } else if ((title.contains("подтверждение покупки") || title.contains("подозрительная цена!")) && buy) {
            this.click(1, true);
        }

        if (this.reissueSetting.getValue() && reissue && mc.gui.screen() instanceof ContainerScreen handledScreen) {
            if (title.matches(".*а.*у.*к.*ц.*и.*о.*н.*")) {
                if (mc.player.tickCount % 10 == 0) {
                    this.click(46, false);
                    this.reissueTicks = 0;
                }
            } else if (title.matches(".*х.*р.*а.*н.*и.*л.*и.*щ.*е.*")) {
                if (this.reissueTicks % 20 == 10) {
                    this.click(52, false);
                } else if (this.reissueTicks % 20 == 0 && this.reissueTicks > 0) {
                    this.click(46, false);
                    this.resetCounter();
                }
            }
            this.quietTicks = 0;
        }
    }

    private ItemBuy findBuyable(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        var shulker = stack.get(DataComponents.CONTAINER);
        for (ItemBuy item : this.entries) {
            if (!item.isEnabled()) {
                continue;
            }
            if (item.isBuy(stack)) {
                if (this.isBuyable(stack)) {
                    return item;
                }
            } else if (shulker != null) {
                if (shulker.nonEmptyItemCopyStream().anyMatch(item::isBuy) && this.isBuyable(stack)) {
                    return item;
                }
            }
        }
        return null;
    }

    private boolean isBuyable(ItemStack stack) {
        long balance = AutoBuyUtil.getBalance();
        int price = AutoBuyUtil.getServerPrice(stack);
        return balance > 0L && price > 0 && (long) price * (long) Math.max(stack.getCount(), 1) <= balance;
    }

    private void click(int slotId, boolean quickMove) {
        if (quickMove) {
            InventoryUtil.quickMoveSlot(slotId);
        } else {
            InventoryUtil.leftClickSlot(slotId);
        }
        this.quietTicks = 0;
    }

    @EventTarget
    public void onPacket(PacketReceiveEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!this.needOpen) {
            return;
        }
        Object packet = event.getPacket();
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket inventoryPacket) {
            if (inventoryPacket.items().size() == 90 && this.quietTicks >= 7) {
                int anarchy = (int) (Math.random() * 100.0 <= 50.0 ? Math.random() * 26.0 + 205.0 : Math.random() * 20.0 + 305.0);
                if (mc.player != null) {
                    mc.player.connection.sendCommand("an" + anarchy);
                }
                ChatUtil.info("Обнаружили замедление аукциона, переходим на " + anarchy + " анархию");
                this.quietTicks = 0;
                this.ahWaiting = true;
            }
        } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket messagePacket) {
            String message = messagePacket.content().getString();
            if (this.lastBought != null && message.contains("Вы успешно купили")) {
                if (this.reissueItems.isEmpty() || !ItemStack.isSameItemSameComponents(this.reissueItems.getFirst(), this.lastBought)) {
                    ChatUtil.info("Успешно куплен предмет " + this.lastBought.getHoverName().getString()
                            + (this.lastBought.getCount() > 1 ? " ×" + this.lastBought.getCount() : "")
                            + " за " + String.format(Locale.US, "%,d", AutoBuyUtil.getServerPrice(this.lastBought)) + " $");
                    this.reissueItems.addFirst(this.lastBought);
                }
                this.lastBought = null;
            }
        } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundOpenScreenPacket openScreenPacket) {
            if (!(mc.gui.screen() instanceof ContainerScreen)) {
                this.quietTicks = 0;
            }
            this.lastSyncId = openScreenPacket.getContainerId();
        } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket soundPacket) {
            if (soundPacket.getSound().value().location().getPath().equals("block.note_block.basedrum") && mc.player != null) {
                this.lastSyncId = mc.player.containerMenu.containerId;
                event.cancel();
            }
        }
    }

    @EventTarget
    public void onScreenRender(ScreenRenderEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof ContainerScreen screen)) {
            return;
        }
        String title = screen.getTitle().getString().replaceAll("\u00a7.", "").toLowerCase(Locale.ROOT).trim();
        if (!title.contains("аукцион")) {
            return;
        }

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        var gfx = event.getGuiGraphicsExtractor();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        int count = accessor.getImageHeight() / 18;
        int x = accessor.getLeftPos() - 22;
        int y = accessor.getTopPos() + 3;
        int bottom = y + count * 18;
        int[][] edges = new int[][]{
                {x - 2, y, x + 20, bottom, -3750202},
                {x, y - 2, x + 18, bottom + 2, -3750202},
                {x - 1, y - 1, x + 19, y, -3750202},
                {x - 1, bottom, x + 19, bottom + 1, -3750202},
                {x, y - 2, x + 18, y - 1, -1},
                {x - 1, y - 1, x, y, -1},
                {x - 2, y, x - 1, bottom, -1},
                {x, bottom + 1, x + 18, bottom + 2, -11184811},
                {x + 18, bottom, x + 19, bottom + 1, -11184811},
                {x + 19, y, x + 20, bottom, -11184811}
        };
        for (int[] edge : edges) {
            gfx.fill(edge[0], edge[1], edge[2], edge[3], edge[4]);
        }
        for (int i = 0; i < count; i++) {
            if (i >= this.reissueItems.size()) {
                break;
            }
            int slotY = y + i * 18;
            ItemStack stack = this.reissueItems.get(i);
            gfx.item(stack, x + 1, slotY + 1);
            if (isInside(mouseX, mouseY, x + 1, slotY + 1, 16.0f, 16.0f)) {
                gfx.fillGradient(x + 1, slotY + 1, x + 17, slotY + 17, -2130706433, -2130706433);
                gfx.setTooltipForNextFrame(mc.font, stack, mouseX, mouseY);
            }
        }
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, float width, float height) {
        return (float) mouseX >= (float) x && (float) mouseX <= (float) x + width && (float) mouseY >= (float) y && (float) mouseY <= (float) y + height;
    }

    public List<ItemBuy> getEntries() {
        return this.entries;
    }
}