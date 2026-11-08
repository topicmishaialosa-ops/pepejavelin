package org.blade.utils.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import org.blade.context.MinecraftContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.lifecycle.WorldLeaveEvent;
import org.blade.event.events.screen.ScreenCloseEvent;
import org.blade.feature.impl.movement.InventoryMoveFeature;

import java.util.ArrayDeque;
import java.util.Deque;

public final class DropAllInventoryController implements MinecraftContext {
    public static final DropAllInventoryController INSTANCE = new DropAllInventoryController();

    private final Deque<Integer> slots = new ArrayDeque<>();
    private InventoryScreen screen;
    private InventoryMenu menu;
    private boolean running;

    private DropAllInventoryController() {
    }

    public static boolean isRunning() {
        return INSTANCE.running;
    }

    public static void toggle(InventoryScreen screen) {
        if (INSTANCE.running) {
            INSTANCE.cancel();
        } else {
            INSTANCE.start(screen);
        }
    }

    public static boolean blocksInventoryOperations() {
        return INSTANCE.running || InventoryMoveFeature.isClickPipelineBusy() || InventoryButtonsOverlay.isBusy();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (!this.running) {
            return;
        }
        Minecraft client = event.getClient();
        if (client == null || client.player == null || client.gui.screen() != this.screen
                || client.player.containerMenu != this.menu || InventoryUtil.hasCarriedItem()) {
            cancel();
            return;
        }
        if (InventorySwap.isBusy()) {
            return;
        }

        while (!this.slots.isEmpty()) {
            int slotId = this.slots.removeFirst();
            if (!this.menu.isValidSlotIndex(slotId) || !this.menu.getSlot(slotId).hasItem()) {
                continue;
            }
            InventoryUtil.dropStack(slotId);
            return;
        }
        finish();
    }

    @EventTarget
    public void onScreenClose(ScreenCloseEvent event) {
        if (event.getScreen() == this.screen) {
            cancel();
        }
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {
        cancel();
    }

    private void start(InventoryScreen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || screen == null
                || client.player.containerMenu != screen.getMenu()
                || InventoryUtil.hasCarriedItem() || InventorySwap.isBusy()
                || InventoryButtonsOverlay.isBusy()) {
            return;
        }

        this.screen = screen;
        this.menu = screen.getMenu();
        this.slots.clear();
        for (int slotId = 0; slotId < this.menu.slots.size(); slotId++) {
            if (this.menu.getSlot(slotId).hasItem()) {
                this.slots.addLast(slotId);
            }
        }
        this.running = !this.slots.isEmpty();
    }

    private void finish() {
        this.running = false;
        this.slots.clear();
    }

    private void cancel() {
        finish();
        this.screen = null;
        this.menu = null;
    }
}
