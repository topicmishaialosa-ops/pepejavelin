package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.input.KeyboardInputEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public final class InstanceBotFeature extends Feature {

    public static InstanceBotFeature INSTANCE;

    public final BooleanSetting fallSimulation = register(new BooleanSetting("Симуляция падения", false));

    private boolean deferred;
    private boolean pendingReopen;
    private long pendingSince;

    public InstanceBotFeature() {
        super("InstanceBot", "Окно управления ботами", FeatureCategory.MISC, BindSetting.UNBOUND);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() == null) {
            InstanceBotScreen.open();
        } else {
            this.deferred = true;
        }
    }

    @Override
    public void onDisable() {
        this.deferred = false;
        this.pendingReopen = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof InstanceBotScreen) {
            mc.gui.setScreen(null);
        }
        super.onDisable();
    }

    public void markPendingReopen() {
        this.pendingReopen = true;
        this.pendingSince = System.currentTimeMillis();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (this.pendingReopen) {
            var screen = mc.gui.screen();
            if (screen instanceof net.minecraft.client.gui.screens.DisconnectedScreen) {
                this.pendingReopen = false;
                InstanceBotManager.INSTANCE.markLaunchFailed();
                InstanceBotScreen.open();
                return;
            }
            if (mc.level != null) {
                if (screen == null) {
                    this.pendingReopen = false;
                    InstanceBotManager.INSTANCE.onJoined();
                    InstanceBotScreen.open();
                }
                return;
            }
            if (System.currentTimeMillis() - this.pendingSince > 30_000L) {
                this.pendingReopen = false;
                InstanceBotManager.INSTANCE.markLaunchFailed();
                if (mc.gui.screen() == null) {
                    InstanceBotScreen.open();
                }
            }
            return;
        }
        if (this.deferred && mc.gui.screen() == null) {
            this.deferred = false;
            InstanceBotScreen.open();
        }
    }

    @EventTarget(priority = 2_000)
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT
                && Minecraft.getInstance().gui.screen() instanceof InstanceBotScreen) {
            event.cancel();
            Minecraft.getInstance().gui.setScreen(null);
        }
    }
}
