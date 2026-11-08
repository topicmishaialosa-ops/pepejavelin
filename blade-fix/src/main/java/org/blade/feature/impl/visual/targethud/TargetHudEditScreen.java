package org.blade.feature.impl.visual.targethud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TargetHudEditScreen extends Screen {
    public TargetHudEditScreen() {
        super(Component.literal("Редактор таргетхуда"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
    }
}