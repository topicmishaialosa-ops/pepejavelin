package org.blade.feature.impl.misc;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.input.KeyboardInputEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.InputBindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.utils.inventory.InventoryUtil;
import org.lwjgl.glfw.GLFW;

public final class ElytraHelperFeature extends Feature {
    private static final int CHEST_SLOT = 6;

    private static final List<Item> CHESTPLATES = List.of(
        Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
        Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE
    );

    private final InputBindSetting elytraBind = register(new InputBindSetting("Кнопка свапа", -1));
    private final InputBindSetting fireworkBind = register(new InputBindSetting("Кнопка фейерверка", -1));
    private final ModeSetting mode = register(new ModeSetting("Мод пуска феера", "Хвх", "Хвх", "Легит"));

    private boolean swap;
    private boolean useFirework;

    public ElytraHelperFeature() {
        super("ElytraHelper", "Помощник для элитр", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (elytraBind.matches(event.getKey())) {
            this.swap = true;
        } else if (fireworkBind.matches(event.getKey()) && mc.player.isFallFlying()) {
            this.useFirework = true;
        }
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (this.swap) {
            this.swap = false;
            doElytraSwap(player);
        }
        if (this.useFirework) {
            this.useFirework = false;
            if (player.isFallFlying()) {
                useFireworkRocket(mc, player);
            }
        }
    }

    private void doElytraSwap(LocalPlayer player) {
        boolean isElytra = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        if (isElytra) {
            int slot = findInHotbar(player, CHESTPLATES);
            if (slot != -1) InventoryUtil.swapWithHotbar(CHEST_SLOT, slot);
        } else {
            int slot = findInHotbar(player, List.of(Items.ELYTRA));
            if (slot != -1) InventoryUtil.swapWithHotbar(CHEST_SLOT, slot);
        }
    }

    private void useFireworkRocket(Minecraft mc, LocalPlayer player) {
        int slot = findInHotbar(player, List.of(Items.FIREWORK_ROCKET));
        if (slot == -1) return;
        int current = player.getInventory().getSelectedSlot();
        if (slot != current) {
            player.getInventory().setSelectedSlot(slot);
        }
        mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
    }

    private int findInHotbar(LocalPlayer player, List<Item> items) {
        for (int i = 0; i < 9; i++) {
            if (items.contains(player.getInventory().getItem(i).getItem())) return i;
        }
        return -1;
    }
}