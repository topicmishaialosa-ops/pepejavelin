package org.blade.feature.impl.misc;

import java.util.Locale;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.MultiSelectSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.utils.inventory.InventoryUtil;
import org.blade.utils.math.BladeTimer;

public final class AutoKitFeature extends Feature {
    private static final String[] KIT_COMMANDS = {
        "/kit free", "/kit hero", "/kit prince", "/kit killer",
        "/kit krushitel", "/kit kraken", "/kit alpha", "/kit rabbit",
        "/kit sponsor", "/kit pumpkin"
    };
    private static final String[] KIT_NAMES = {
        "free", "hero", "prince", "killer", "krushitel", "kraken", "alpha", "rabbit", "sponsor", "pumpkin"
    };

    private final MultiSelectSetting kits = register(new MultiSelectSetting("Киты", Set.of(), KIT_NAMES));
    private final MultiSelectSetting itemMode = register(new MultiSelectSetting("Предметы в /ec", Set.of(), "Зелье Викинга", "Чарка", "Ангел Хранитель"));
    private final NumberSetting delayMs = register(new NumberSetting("Задержка (мс)", 700.0, 250.0, 3000.0, 50.0, ""));

    private final BladeTimer timer = new BladeTimer();
    private int kitIndex;
    private Stage stage = Stage.KITS;
    private int ecReopenAttempts;
    private boolean firstTick;

    private enum Stage { KITS, OPEN_EC, MOVE_ITEMS_TO_EC, CI_1, CI_2, DONE }

    public AutoKitFeature() {
        super("AutoKit", "Берет киты, складывает предметы в /ec и чистит инвентарь", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.kitIndex = 0;
        this.stage = Stage.KITS;
        this.ecReopenAttempts = 0;
        this.firstTick = true;
        this.timer.reset();
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (this.firstTick) {
            this.firstTick = false;
            this.timer.reset();
        }
        if (!this.timer.finished((long) this.delayMs.getValue().doubleValue())) return;
        this.timer.reset();

        switch (this.stage) {
            case KITS -> {
                String cmd = nextSelectedKitCommand();
                if (cmd != null) {
                    player.connection.sendChat(cmd);
                    this.stage = Stage.OPEN_EC;
                    this.ecReopenAttempts = 0;
                } else {
                    this.stage = Stage.DONE;
                }
            }
            case OPEN_EC -> {
                player.connection.sendChat("/ec");
                this.stage = Stage.MOVE_ITEMS_TO_EC;
            }
            case MOVE_ITEMS_TO_EC -> {
                AbstractContainerMenu menu = player.containerMenu;
                if (menu.containerId == 0) {
                    if (this.ecReopenAttempts++ < 3) {
                        player.connection.sendChat("/ec");
                    } else {
                        this.stage = Stage.CI_1;
                    }
                    return;
                }
                moveSelectedItemsToEc(menu, player);
                player.closeContainer();
                this.stage = Stage.CI_1;
            }
            case CI_1 -> {
                player.connection.sendChat("/ci");
                this.stage = Stage.CI_2;
            }
            case CI_2 -> {
                player.connection.sendChat("/ci");
                this.stage = Stage.KITS;
            }
            case DONE -> this.setEnabled(false);
        }
    }

    private void moveSelectedItemsToEc(AbstractContainerMenu menu, LocalPlayer player) {
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot == null || slot.container != player.getInventory() || !slot.hasItem()) continue;
            String name = slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT);
            if (shouldMoveByMode(slot.getItem(), name)) {
                InventoryUtil.quickMoveSlot(i);
            }
        }
    }

    private boolean shouldMoveByMode(net.minecraft.world.item.ItemStack stack, String lowerName) {
        boolean isViking = lowerName.contains("зелье викинга");
        boolean isChar = stack.is(Items.ENCHANTED_GOLDEN_APPLE);
        boolean isTotem = lowerName.contains("ангел хранитель");
        if (itemMode.isSelected("Зелье Викинга") && isViking) return true;
        if (itemMode.isSelected("Чарка") && isChar) return true;
        return itemMode.isSelected("Ангел Хранитель") && isTotem;
    }

    private String nextSelectedKitCommand() {
        while (this.kitIndex < KIT_COMMANDS.length) {
            int idx = this.kitIndex++;
            if (kits.isSelected(KIT_NAMES[idx])) return KIT_COMMANDS[idx];
        }
        return null;
    }
}