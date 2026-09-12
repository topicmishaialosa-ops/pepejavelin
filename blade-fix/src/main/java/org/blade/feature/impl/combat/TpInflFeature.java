package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;

public final class TpInflFeature extends Feature {
    public static final TpInflFeature INSTANCE = new TpInflFeature();

    public final ModeSetting mode = register(new ModeSetting(
            "Mode", "StepVH",
            "StepVH", "StepV", "StepH", "StepHG", "VanillaVH", "VanillaH"
    ));
    public final NumberSetting range = register(new NumberSetting(
            "Range", 100.0D, 10.0D, 500.0D, 1.0D, ""
    ));

    private LivingEntity currentTarget = null;
    private Vec3 lastHandledVec = Vec3.ZERO;

    public TpInflFeature() {
        super("TPInfl", "Телепортируется к цели при включенной ауре, работает только с ней", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    public void onEnable() {
        currentTarget = null;
        lastHandledVec = Vec3.ZERO;
    }

    public void onDisable() {
        lastHandledVec = Vec3.ZERO;
        currentTarget = null;
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = event.getClient();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        AuraFeature aura = FeatureManager.INSTANCE.getEnabled(AuraFeature.class);
        if (aura == null || !aura.isEnabled()) {
            currentTarget = null;
            return;
        }

        LivingEntity target = findTarget(mc, player);
        if (target != currentTarget) {
            currentTarget = target;
            if (target != null) {
                performTeleport(mc, player, target, aura);
            }
        }
    }

    private void performTeleport(Minecraft mc, LocalPlayer player, LivingEntity target, AuraFeature aura) {
        if (target == null) {
            return;
        }
        double attackDistance = aura.attackRange.getValue();
        Vec3 dest = targetWhitePos(player, target, attackDistance);
        teleportAction(player, dest, mode.getValue());
    }

    private Vec3 targetWhitePos(LocalPlayer player, LivingEntity target, double distance) {
        double dist = distance - target.getBbHeight();
        if (dist < 0.0D) {
            dist = 0.0D;
        }

        Vec3 targetPos = target.position();
        double playerY = player.getY();
        double positive = Math.abs(playerY - targetPos.y);

        Vec3 dest = new Vec3(targetPos.x, targetPos.y, targetPos.z);

        if (positive > dist) {
            double num = positive - dist;
            double dx = player.getX() - targetPos.x;
            double dz = player.getZ() - targetPos.z;
            double max = dist - Math.sqrt(dx * dx + dz * dz);
            double clamp = Mth.clamp(num, 0.0D, max);
            double raise = 0.0D;
            while (raise < clamp) {
                raise += 0.1D;
                if (isSolid(player, new Vec3(dest.x, dest.y + raise, dest.z))) {
                    raise -= 0.1D;
                    break;
                }
            }
            if (raise < 0.0D) {
                raise = 0.0D;
            }
            dest = dest.add(0.0D, raise, 0.0D);
        }

        return findFreePosition(player, dest);
    }

    private boolean isSolid(LocalPlayer player, Vec3 pos) {
        if (player.level() == null) {
            return false;
        }
        BlockPos bp = BlockPos.containing(pos.x, pos.y, pos.z);
        BlockState state = player.level().getBlockState(bp);
        return !state.isAir() || !player.level().getBlockState(bp.above()).isAir();
    }

    private Vec3 findFreePosition(LocalPlayer player, Vec3 pos) {
        if (player.level() == null) {
            return pos;
        }
        for (int i = 0; i < 12; i++) {
            if (!isSolid(player, pos)) {
                return pos;
            }
            pos = pos.add(0.0D, 1.0D, 0.0D);
        }
        return pos;
    }

    private void teleportAction(LocalPlayer player, Vec3 dest, String modeName) {
        double dx = Math.abs(player.getX() - dest.x);
        double dy = Math.abs(player.getY() - dest.y);
        double dz = Math.abs(player.getZ() - dest.z);
        int onGround = player.onGround() ? 1 : 0;

        switch (modeName) {
            case "StepVH": {
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                int packets = (int) (dist / 9.64D) + 1;
                for (int i = 0; i < packets; i++) {
                    sendGroundPacket(player, false);
                }
                if (onGround == 1) {
                    sendPositionPacket(player, dest.x, dest.y + 0.08D, dest.z, false);
                    sendPositionPacket(player, dest.x, dest.y + 0.01D, dest.z, false);
                }
                setPlayerPos(player, dest.x, dest.y + 0.01D, dest.z);
                lastHandledVec = new Vec3(dest.x, dest.y + 0.01D, dest.z);
                break;
            }
            case "StepV": {
                double dist = Math.abs(dy);
                int packets = (int) (dist / 9.73D) + 1;
                if (onGround == 1) {
                    sendPositionPacket(player, player.getX(), player.getY() + 0.08D, player.getZ(), false);
                }
                for (int i = 0; i < packets; i++) {
                    sendGroundPacket(player, false);
                }
                double curX = player.getX();
                double curZ = player.getZ();
                sendPositionPacket(player, curX, dest.y + 0.01D, curZ, false);
                setPlayerPos(player, curX, dest.y + 0.01D, curZ);
                lastHandledVec = new Vec3(curX, dest.y + 0.01D, curZ);
                break;
            }
            case "StepH": {
                double dist = Math.sqrt(dx * dx + dz * dz);
                int packets = (int) (dist / 8.953D) + onGround;
                for (int i = 0; i < packets; i++) {
                    sendGroundPacket(player, false);
                }
                setPlayerPos(player, dest.x, dest.y, dest.z);
                lastHandledVec = new Vec3(dest.x, dest.y, dest.z);
                break;
            }
            case "StepHG": {
                double dist = Math.sqrt(dx * dx + dz * dz);
                int packets = (int) (dist / 8.317D) + onGround;
                for (int i = 0; i < packets; i++) {
                    sendGroundPacket(player, false);
                }
                if (onGround == 1) {
                    double y = dest.y - 1.0E-4D * packets;
                    sendPositionPacket(player, dest.x, y, dest.z, false);
                    lastHandledVec = new Vec3(dest.x, y, dest.z);
                } else {
                    setPlayerPos(player, dest.x, dest.y, dest.z);
                    lastHandledVec = new Vec3(dest.x, dest.y, dest.z);
                }
                break;
            }
            case "VanillaVH": {
                sendGroundPacket(player, false);
                sendPositionPacket(player, dest.x, dest.y, dest.z, false);
                sendPositionPacket(player, dest.x, dest.y, dest.z, false);
                lastHandledVec = new Vec3(dest.x, dest.y, dest.z);
                break;
            }
            case "VanillaH": {
                double curY = player.getY();
                sendGroundPacket(player, false);
                sendPositionPacket(player, dest.x, curY, dest.z, false);
                sendPositionPacket(player, dest.x, curY, dest.z, false);
                lastHandledVec = new Vec3(dest.x, curY, dest.z);
                break;
            }
            default: {
                teleportAction(player, dest, "StepVH");
                break;
            }
        }
    }

    private LivingEntity findTarget(Minecraft mc, LocalPlayer player) {
        if (mc.level == null) {
            return null;
        }

        Player closest = null;
        float closestDist = Float.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player) || entity == player || !entity.isAlive()) {
                continue;
            }
            Player other = (Player) entity;
            if (AntiBotFeature.INSTANCE.isEnabled() && AntiBotFeature.INSTANCE.isBot(other)) {
                continue;
            }

            float dist = player.distanceTo(other);
            if (dist <= range.getValue() && dist < closestDist) {
                closest = other;
                closestDist = dist;
            }
        }

        return closest;
    }

    private void sendPositionPacket(LocalPlayer player, double x, double y, double z, boolean onGround) {
        player.connection.send(new ServerboundMovePlayerPacket.PosRot(x, y, z, player.getYRot(), player.getXRot(), onGround, player.horizontalCollision));
    }

    private void sendGroundPacket(LocalPlayer player, boolean onGround) {
        player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(onGround, player.horizontalCollision));
    }

    private void setPlayerPos(LocalPlayer player, double x, double y, double z) {
        player.setPos(x, y, z);
    }

    public String getDisplayInfo() {
        if (currentTarget != null) {
            return currentTarget.getName().getString();
        }
        return null;
    }
}