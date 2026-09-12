package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.blade.event.EventTarget;
import org.blade.event.events.input.MouseInputEvent;
import org.blade.event.events.screen.ScreenRenderEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.mixin.accessor.AbstractContainerScreenAccessor;
import org.blade.utils.inventory.InventoryUtil;
import org.lwjgl.glfw.GLFW;

public final class ItemScrollerFeature extends Feature {
    private final BooleanSetting autoMove = register(new BooleanSetting("Авто-перенос", true));
    private final BooleanSetting onlyShift = register(new BooleanSetting("Только при Shift", true));

    private boolean mouseHold;

    public ItemScrollerFeature() {
        super("ItemScroller", "Перемещение предметов без задержки", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onMouse(MouseInputEvent event) {
        if (event.getButton() == 0) {
            if (event.getAction() == 1) { // press
                this.mouseHold = true;
            } else if (event.getAction() == 0) { // release
                this.mouseHold = false;
            }
        }
    }

    @EventTarget
    public void onScreenRender(ScreenRenderEvent event) {
        if (!this.autoMove.getValue() || !this.mouseHold || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (this.onlyShift.getValue() && !isShiftDown()) {
            return;
        }
        Slot hoveredSlot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            InventoryUtil.quickMoveSlot(hoveredSlot.index);
        }
    }

    private boolean isShiftDown() {
        long handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}