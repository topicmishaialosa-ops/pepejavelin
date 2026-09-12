package org.blade.menu.core;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ClickGuiScreen extends Screen {
    private static final int BACKDROP_COLOR = 0xD90A0B10;

    public ClickGuiScreen() {
        super(Component.literal("Blade ClickGUI"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        guiGraphicsExtractor.fill(0, 0, this.width, this.height, BACKDROP_COLOR);
    }
}