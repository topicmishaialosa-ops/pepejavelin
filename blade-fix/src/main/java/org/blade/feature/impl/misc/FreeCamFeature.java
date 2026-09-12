package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.packet.PacketSendEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.lwjgl.glfw.GLFW;

public final class FreeCamFeature extends Feature {
    private final BooleanSetting noGravity = register(new BooleanSetting("No Gravity", true));

    private Vec3 pos;

    public FreeCamFeature() {
        super("FreeCam", "Обзор местности за фейк игрока", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            this.pos = player.position();
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            if (this.pos != null && !player.onGround()) {
                player.setPos(this.pos);
            }
            player.setNoGravity(false);
            player.noPhysics = false;
        }
        this.pos = null;
        super.onDisable();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || this.pos == null) {
            return;
        }

        player.setNoGravity(true);
        player.noPhysics = true;
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);

        if (!this.noGravity.getValue()) {
            return;
        }

        double forward = (isKeyDown(mc, GLFW.GLFW_KEY_W) ? 1.0D : 0.0D) - (isKeyDown(mc, GLFW.GLFW_KEY_S) ? 1.0D : 0.0D);
        double strafe = (isKeyDown(mc, GLFW.GLFW_KEY_D) ? 1.0D : 0.0D) - (isKeyDown(mc, GLFW.GLFW_KEY_A) ? 1.0D : 0.0D);
        double up = (isKeyDown(mc, GLFW.GLFW_KEY_SPACE) ? 1.0D : 0.0D) - (isKeyDown(mc, GLFW.GLFW_KEY_LEFT_SHIFT) ? 1.0D : 0.0D);

        double yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);
        Vec3 vel = new Vec3(
                -Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe,
                up,
                Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe
        );
        if (vel.lengthSqr() > 0.0D) {
            vel = vel.normalize().scale(0.32D);
        }
        player.setDeltaMovement(vel);
    }

    @EventTarget
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacket() instanceof ServerboundMovePlayerPacket) {
            event.cancel();
        }
    }

    private boolean isKeyDown(Minecraft mc, int key) {
        long handle = mc.getWindow().handle();
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }
}