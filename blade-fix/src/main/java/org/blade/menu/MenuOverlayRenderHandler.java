package org.blade.menu;

import net.minecraft.client.Minecraft;
import org.blade.context.RenderContext;
import org.blade.event.EventTarget;
import org.blade.event.events.render.FinalGuiRenderEvent;
import org.blade.menu.core.MenuOverlay;
import org.blade.menu.core.MenuAppearance;
import org.blade.utils.render.gui.Render2DUtil;

public final class MenuOverlayRenderHandler {
    @EventTarget
    public void onFinalGuiRender(FinalGuiRenderEvent event) {
        Minecraft minecraft = event.getClient();
        RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
        try {
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            Render2DUtil.beginFrame();
            Render2DUtil.setBackdropBlurScale(MenuAppearance.glassBlurScale());
            MenuOverlay.render(minecraft, event.getGuiGraphicsExtractor(), screenWidth, screenHeight);
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }
}
