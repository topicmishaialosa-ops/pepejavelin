package org.blade.utils.game.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.blade.utils.math.MathUtil;

public final class MovingUtil {
    private MovingUtil() {
    }

    public static boolean hasPlayerMovement() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer player = mc.player;
        return player != null && (player.input.keyPresses.forward()
                || player.input.keyPresses.backward()
                || player.input.keyPresses.left()
                || player.input.keyPresses.right());
    }

    public static void setVelocity(double velocity) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        float forward = (player.input.keyPresses.forward() ? 1.0F : (player.input.keyPresses.backward() ? -1.0F : 0.0F));
        float sideways = (player.input.keyPresses.right() ? 1.0F : (player.input.keyPresses.left() ? -1.0F : 0.0F));
        double radYaw = Math.toRadians(player.getYRot());
        double sinYaw = Math.sin(radYaw);
        double cosYaw = Math.cos(radYaw);
        double x = (-forward * sinYaw - sideways * cosYaw) * velocity;
        double z = (forward * cosYaw - sideways * sinYaw) * velocity;
        player.setDeltaMovement(x, player.getDeltaMovement().y, z);
    }
}
