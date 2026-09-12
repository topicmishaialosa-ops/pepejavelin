package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;

public final class VibeCodingScreen2 extends VibeCodingScreen {
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof VibeCodingScreen2) {
            return;
        }
        mc.gui.setScreen(new VibeCodingScreen2());
    }

    public VibeCodingScreen2() {
        super(true);
    }
}