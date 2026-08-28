package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.combat.Aura;

@ModuleAnnotation(
        name = "TPInfl",
        category = Category.COMBAT,
        description = "Телепортируется к цели при включенной ауре, работает только с ней"
)
public class TPInfl extends Module {
    public static final TPInfl INSTANCE = new TPInfl();

    public TPInfl() {
        super();
    }

    public final ModeSetting mode = new ModeSetting("Mode", "StepVH", "StepVH", "StepV", "StepH", "StepHG", "VanillaVH", "VanillaH");
    public final NumberSetting range = new NumberSetting("Range", 100f, 10f, 500f, 1f);

    private LivingEntity currentTarget = null;
    private Vec3d lastHandledVec = Vec3d.ZERO;

    @Override
    public void onEnable() {
        currentTarget = null;
        lastHandledVec = Vec3d.ZERO;
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        if (!Aura.INSTANCE.isEnabled()) {
            currentTarget = null;
            return;
        }

        LivingEntity target = findTarget();
        if (target != currentTarget) {
            currentTarget = target;
            if (target != null) {
                performTeleport(target);
            }
        }
    }

    private void performTeleport(LivingEntity target) {
        if (target == null || mc.player == null) return;
        double attackDistance = Aura.INSTANCE.getAttackDistance();
        Vec3d dest = targetWhitePos(target, attackDistance);
        teleportAction(dest, mode.get());
    }

    private Vec3d targetWhitePos(LivingEntity target, double distance) {
        double dist = distance - target.getHeight();
        if (dist < 0.0D) dist = 0.0D;

        Vec3d targetPos = target.getPos();
        double playerY = mc.player.getY();
        double positive = Math.abs(playerY - targetPos.y);

        Vec3d dest = new Vec3d(targetPos.x, targetPos.y, targetPos.z);

        if (positive > dist) {
            double num = positive - dist;
            double dx = mc.player.getX() - targetPos.x;
            double dz = mc.player.getZ() - targetPos.z;
            double max = dist - Math.sqrt(dx * dx + dz * dz);
            double clamp = MathHelper.clamp(num, 0.0D, max);
            double raise = 0.0D;
            while (raise < clamp) {
                raise += 0.1D;
                if (isSolid(new Vec3d(dest.x, dest.y + raise, dest.z))) {
                    raise -= 0.1D;
                    break;
                }
            }
            if (raise < 0.0D) raise = 0.0D;
            dest = dest.add(0.0D, raise, 0.0D);
        }

        return findFreePosition(dest);
    }

    private boolean isSolid(Vec3d pos) {
        if (mc.world == null) return false;
        BlockPos bp = BlockPos.ofFloored(pos.x, pos.y, pos.z);
        return !mc.world.getBlockState(bp).isAir() || !mc.world.getBlockState(bp.up()).isAir();
    }

    private Vec3d findFreePosition(Vec3d pos) {
        if (mc.world == null) return pos;
        for (int i = 0; i < 12; i++) {
            if (!isSolid(pos)) return pos;
            pos = pos.add(0.0D, 1.0D, 0.0D);
        }
        return pos;
    }

    private void teleportAction(Vec3d dest, String modeName) {
        if (mc.player == null) return;

        double dx = Math.abs(mc.player.getX() - dest.x);
        double dy = Math.abs(mc.player.getY() - dest.y);
        double dz = Math.abs(mc.player.getZ() - dest.z);
        int onGround = mc.player.isOnGround() ? 1 : 0;

        switch (modeName) {
            case "StepVH": {
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                int packets = (int) (dist / 9.64D) + 1;
                for (int i = 0; i < packets; i++) sendGroundPacket(false);
                if (onGround == 1) {
                    sendPositionPacket(dest.x, dest.y + 0.08D, dest.z, false);
                    sendPositionPacket(dest.x, dest.y + 0.01D, dest.z, false);
                }
                setPlayerPos(dest.x, dest.y + 0.01D, dest.z);
                lastHandledVec = new Vec3d(dest.x, dest.y + 0.01D, dest.z);
                break;
            }
            case "StepV": {
                double dist = Math.abs(dy);
                int packets = (int) (dist / 9.73D) + 1;
                if (onGround == 1) {
                    sendPositionPacket(mc.player.getX(), mc.player.getY() + 0.08D, mc.player.getZ(), false);
                }
                for (int i = 0; i < packets; i++) sendGroundPacket(false);
                double curX = mc.player.getX();
                double curZ = mc.player.getZ();
                sendPositionPacket(curX, dest.y + 0.01D, curZ, false);
                setPlayerPos(curX, dest.y + 0.01D, curZ);
                lastHandledVec = new Vec3d(curX, dest.y + 0.01D, curZ);
                break;
            }
            case "StepH": {
                double dist = Math.sqrt(dx * dx + dz * dz);
                int packets = (int) (dist / 8.953D) + onGround;
                for (int i = 0; i < packets; i++) sendGroundPacket(false);
                setPlayerPos(dest.x, dest.y, dest.z);
                lastHandledVec = new Vec3d(dest.x, dest.y, dest.z);
                break;
            }
            case "StepHG": {
                double dist = Math.sqrt(dx * dx + dz * dz);
                int packets = (int) (dist / 8.317D) + onGround;
                for (int i = 0; i < packets; i++) sendGroundPacket(false);
                if (onGround == 1) {
                    double y = dest.y - 1.0E-4D * packets;
                    sendPositionPacket(dest.x, y, dest.z, false);
                    lastHandledVec = new Vec3d(dest.x, y, dest.z);
                } else {
                    setPlayerPos(dest.x, dest.y, dest.z);
                    lastHandledVec = new Vec3d(dest.x, dest.y, dest.z);
                }
                break;
            }
            case "VanillaVH": {
                sendGroundPacket(false);
                sendPositionPacket(dest.x, dest.y, dest.z, false);
                sendPositionPacket(dest.x, dest.y, dest.z, false);
                lastHandledVec = new Vec3d(dest.x, dest.y, dest.z);
                break;
            }
            case "VanillaH": {
                double curY = mc.player.getY();
                sendGroundPacket(false);
                sendPositionPacket(dest.x, curY, dest.z, false);
                sendPositionPacket(dest.x, curY, dest.z, false);
                lastHandledVec = new Vec3d(dest.x, curY, dest.z);
                break;
            }
            default: {
                teleportAction(dest, "StepVH");
                break;
            }
        }
    }

    private LivingEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        PlayerEntity closest = null;
        float closestDist = Float.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;

            float dist = mc.player.distanceTo(player);
            if (dist <= range.getCurrent() && dist < closestDist) {
                closest = player;
                closestDist = dist;
            }
        }

        return closest;
    }

    private void sendPositionPacket(double x, double y, double z, boolean onGround) {
        mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround, mc.player.horizontalCollision)
        );
    }

    private void sendGroundPacket(boolean onGround) {
        mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.OnGroundOnly(onGround, mc.player.horizontalCollision)
        );
    }

    private void setPlayerPos(double x, double y, double z) {
        mc.player.setPosition(x, y, z);
    }

    @Override
    public void onDisable() {
        lastHandledVec = Vec3d.ZERO;
        currentTarget = null;
    }

    public String getDisplayInfo() {
        if (currentTarget != null) {
            return currentTarget.getName().getString();
        }
        return null;
    }
}
