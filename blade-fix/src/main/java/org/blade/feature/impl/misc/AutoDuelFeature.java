package org.blade.feature.impl.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.security.SecureRandom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.setting.TextSetting;
import org.blade.utils.inventory.InventoryUtil;
import org.blade.utils.math.BladeTimer;

public final class AutoDuelFeature extends Feature {
    private static final String TITLE_KIT_SELECT = "Выбор набора";
    private static final String TITLE_DUEL_SETUP = "Настройка поединка";
    private static final long MENU_DELAY_MS = 150L;
    private static final int MAX_BET_RETRIES = 3;

    private final ModeSetting priority = register(new ModeSetting("Приоритет", "Random", "Random", "По алфавиту"));
    private final ModeSetting kit = register(new ModeSetting("Кит", "Щит", "Щит", "Шипы 3", "Лук", "Тотем", "НоуДебаф", "Шары", "Классик", "Читерский рай", "Незер"));
    private final BooleanSetting betToggle = register(new BooleanSetting("Ставка", false));
    private final TextSetting betAmount = register(new TextSetting("Сумма ставки", ""));
    private final NumberSetting delay = register(new NumberSetting("Задержка команд (мс)", 500.0, 200.0, 9000.0, 50.0, ""));

    private final List<String> sentPlayers = new ArrayList<>();
    private final BladeTimer actionTimer = new BladeTimer();
    private final BladeTimer menuTimer = new BladeTimer();
    private final BladeTimer graceTimer = new BladeTimer();
    private final SecureRandom random = new SecureRandom();
    private boolean wasInMenu;
    private String lastTarget;
    private int betAttempts;

    public AutoDuelFeature() {
        super("AutoDuel", "Автоматически вызывает игроков на дуэль", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.sentPlayers.clear();
        this.wasInMenu = false;
        this.lastTarget = null;
        this.betAttempts = 0;
        this.actionTimer.reset();
        this.menuTimer.reset();
        this.graceTimer.reset();
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }

        if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            handleMenu(screen, mc);
            return;
        }
        if (this.wasInMenu) {
            this.wasInMenu = false;
            this.menuTimer.reset();
            return;
        }
        if (!this.actionTimer.finished((long) this.delay.getValue().doubleValue())) return;

        String target = pickTarget(mc);
        if (target == null) return;
        String command = "duel " + target;
        String amount = normalizedBet();
        if (this.betToggle.getValue() && !amount.isEmpty()) {
            command += " " + amount;
        }
        mc.player.connection.sendCommand(command);
        this.lastTarget = target;
        this.sentPlayers.add(target);
        this.betAttempts = 0;
        this.actionTimer.reset();
    }

    private void handleMenu(AbstractContainerScreen<?> screen, Minecraft mc) {
        this.wasInMenu = true;
        if (!this.menuTimer.finished(MENU_DELAY_MS)) return;
        String title = screen.getTitle().getString();
        if (title.contains(TITLE_KIT_SELECT)) {
            int slotIndex = findItemSlotByName(screen, kit.getValue());
            if (slotIndex != -1) {
                InventoryUtil.leftClickSlot(slotIndex);
                this.menuTimer.reset();
            }
        } else if (title.contains(TITLE_DUEL_SETUP)) {
            if (betToggle.getValue() && !normalizedBet().isEmpty()) {
                String signBet = findBetOnSign(screen);
                if (!signBet.isEmpty() && !signBet.equals(normalizedBet())) {
                    this.betAttempts++;
                    if (this.betAttempts < MAX_BET_RETRIES) {
                        retryDuel(mc);
                    } else {
                        cancelDuel(mc);
                    }
                    return;
                }
            }
            InventoryUtil.leftClickSlot(0);
            this.menuTimer.reset();
        }
    }

    private String findBetOnSign(AbstractContainerScreen<?> screen) {
        if (screen.getMenu().slots.size() <= 4) return "";
        Slot slot = screen.getMenu().slots.get(4);
        if (!slot.hasItem()) return "";
        String name = slot.getItem().getHoverName().getString();
        return name.replaceAll("[^0-9]", "");
    }

    private void cancelDuel(Minecraft mc) {
        this.wasInMenu = false;
        if (this.lastTarget != null) this.sentPlayers.add(this.lastTarget);
        if (mc.player != null) mc.player.closeContainer();
    }

    private void retryDuel(Minecraft mc) {
        this.wasInMenu = false;
        if (this.lastTarget == null || mc.getConnection() == null) { cancelDuel(mc); return; }
        if (mc.player != null) mc.player.closeContainer();
        String cmd = "duel " + this.lastTarget;
        String amount = normalizedBet();
        if (!amount.isEmpty()) cmd += " " + amount;
        mc.player.connection.sendCommand(cmd);
        this.actionTimer.reset();
        this.menuTimer.reset();
    }

    private String normalizedBet() {
        String value = betAmount.getValue();
        return value == null ? "" : value.trim().replaceAll("[^0-9]", "");
    }

    private String pickTarget(Minecraft mc) {
        List<String> names = new ArrayList<>();
        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            String name = entry.getProfile().name();
            if (!name.equals(mc.player.getGameProfile().name()) && !this.sentPlayers.contains(name)) {
                names.add(name);
            }
        }
        if (names.isEmpty()) return null;
        if (priority.is("Random")) Collections.shuffle(names, this.random);
        else Collections.sort(names);
        return names.getFirst();
    }

    private int findItemSlotByName(AbstractContainerScreen<?> screen, String itemName) {
        String lower = itemName.toLowerCase(Locale.ROOT);
        int containerSlots = screen.getMenu().slots.size();
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = screen.getMenu().slots.get(i);
            if (slot.hasItem() && slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(lower)) {
                return i;
            }
        }
        return -1;
    }
}