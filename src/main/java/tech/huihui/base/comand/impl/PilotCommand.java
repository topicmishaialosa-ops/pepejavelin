package tech.huihui.base.comand.impl;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.impl.misc.PilotSettings;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.game.player.PlayerIntersectionUtil;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.game.player.rotation.Rotation;

public final class PilotCommand extends CommandAbstract {
    private static final int CLIMB_FIREWORK_COOLDOWN_TICKS = 50;
    private static final int GLIDE_RETRY_TICKS = 5;
    private static final double CRUISE_HEIGHT = 150.0;
    private static final double CRUISE_ALTITUDE_TOLERANCE = 2.0;
    private static final float CRUISE_CLIMB_PITCH = -12.0F;
    private static final float CRUISE_LEVEL_PITCH = -3.0F;
    private static final float CRUISE_DESCEND_PITCH = 4.0F;
    private static final double DESCENT_START_DISTANCE = 35.0;
    private static final double ARRIVAL_HORIZONTAL_DISTANCE = 8.0;
    private static final double ARRIVAL_VERTICAL_DISTANCE = 6.0;

    private boolean active;
    private boolean climbing;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double cruiseY;
    private int fireworkCooldown;
    private int glideRetryCooldown;

    public PilotCommand() {
        super("pilot");
        EventManager.register(this);
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("off").executes(context -> {
            stop("Пилот выключен");
            return 1;
        }));

        builder.then(arg("x", DoubleArgumentType.doubleArg())
                .then(arg("y", DoubleArgumentType.doubleArg())
                        .then(arg("z", DoubleArgumentType.doubleArg()).executes(context -> {
                            start(
                                    context.getArgument("x", Double.class),
                                    context.getArgument("y", Double.class),
                                    context.getArgument("z", Double.class)
                            );
                            return 1;
                        }))));
    }

    private void start(double x, double y, double z) {
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) {
            MessageUtil.displayInfo("Сначала зайди в мир");
            return;
        }
        if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            MessageUtil.displayInfo("Надень элитры");
            return;
        }
        if (PlayerInventoryUtil.getInventoryCount(Items.FIREWORK_ROCKET) == 0) {
            MessageUtil.displayInfo("Для полета нужны фейерверки");
            return;
        }

        targetX = x;
        targetY = y;
        targetZ = z;
        cruiseY = PilotSettings.INSTANCE.getFlightHeight();
        climbing = player.getY() < cruiseY - 3.0;
        active = true;
        fireworkCooldown = 0;
        glideRetryCooldown = 0;
        MessageUtil.displayInfo(climbing
                ? "Пилот включен: набираю высоту Y=" + (int) cruiseY + ", затем лечу к цели"
                : "Пилот включен: лечу к цели на высоте Y=" + (int) cruiseY);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!active) {
            return;
        }

        ClientPlayerEntity player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) {
            stop(null);
            return;
        }
        if (!player.isAlive()) {
            stop("Пилот выключен: игрок погиб");
            return;
        }
        if (player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            stop("Пилот выключен: элитры сняты");
            return;
        }

        if (fireworkCooldown > 0) {
            fireworkCooldown--;
        }
        if (glideRetryCooldown > 0) {
            glideRetryCooldown--;
        }

        if (!player.isGliding()) {
            startGliding(player);
            return;
        }

        if (climbing) {
            aim(player, targetX, cruiseY, targetZ, -35.0F);
            useFirework(player);
            if (player.getY() >= cruiseY - 3.0) {
                climbing = false;
                fireworkCooldown = Math.min(fireworkCooldown, 10);
                MessageUtil.displayInfo("Высота набрана, лечу к координатам");
            }
            return;
        }

        double dx = targetX - player.getX();
        double dz = targetZ - player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double verticalDistance = Math.abs(targetY - player.getY());
        if (horizontalDistance <= ARRIVAL_HORIZONTAL_DISTANCE
                && verticalDistance <= ARRIVAL_VERTICAL_DISTANCE) {
            stop("Пилот завершил маршрут");
            return;
        }

        if (horizontalDistance > DESCENT_START_DISTANCE) {
            float cruisePitch;
            if (player.getY() < cruiseY - CRUISE_ALTITUDE_TOLERANCE) {
                cruisePitch = CRUISE_CLIMB_PITCH;
            } else if (player.getY() > cruiseY + CRUISE_ALTITUDE_TOLERANCE) {
                cruisePitch = CRUISE_DESCEND_PITCH;
            } else {
                cruisePitch = CRUISE_LEVEL_PITCH;
            }
            if (PilotSettings.INSTANCE.isAvoidObstacles() && obstacleAhead(player, 12.0)) {
                cruisePitch = -20.0F;
            }
            aim(player, targetX, cruiseY, targetZ, cruisePitch);
        } else {
            aim(player, targetX, targetY, targetZ, null);
        }
        useFirework(player);
    }

    private boolean obstacleAhead(ClientPlayerEntity player, double distance) {
        net.minecraft.util.math.Vec3d start = player.getEyePos();
        float yaw = player.getYaw();
        double yawRad = Math.toRadians(yaw);
        double dx = -Math.sin(yawRad) * distance;
        double dz = Math.cos(yawRad) * distance;
        net.minecraft.util.math.Vec3d end = start.add(dx, 0.0, dz);
        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                start,
                end,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                player
        )).getType() != net.minecraft.util.hit.HitResult.Type.MISS;
    }

    private void startGliding(ClientPlayerEntity player) {
        if (player.isOnGround()) {
            player.jump();
            return;
        }
        if (!player.isTouchingWater() && glideRetryCooldown == 0) {
            PlayerIntersectionUtil.startFallFlying();
            glideRetryCooldown = GLIDE_RETRY_TICKS;
        }
    }

    private void useFirework(ClientPlayerEntity player) {
        if (fireworkCooldown > 0 || !player.isGliding()) {
            return;
        }
        if (PlayerInventoryUtil.getInventoryCount(Items.FIREWORK_ROCKET) == 0) {
            stop("Пилот выключен: закончились фейерверки");
            return;
        }
        PlayerInventoryUtil.swapAndUseLegit(Items.FIREWORK_ROCKET);
        if (climbing) {
            fireworkCooldown = CLIMB_FIREWORK_COOLDOWN_TICKS;
        } else {
            float perSecond = Math.max(PilotSettings.INSTANCE.getFireworksPerSecond(), 0.02F);
            fireworkCooldown = Math.max((int) Math.round(20.0F / perSecond), 20);
        }
    }

    private void aim(ClientPlayerEntity player, double x, double y, double z, Float forcedPitch) {
        double dx = x - player.getX();
        double dy = y - player.getEyeY();
        double dz = z - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = forcedPitch != null
                ? forcedPitch
                : (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        RotationComponent.update(
                new Rotation(
                        approachAngle(player.getYaw(), targetYaw, 8.0F),
                        MathHelper.clamp(approach(player.getPitch(), targetPitch, 5.0F), -60.0F, 60.0F)
                ),
                360.0F,
                360.0F,
                0,
                10
        );
    }

    private float approachAngle(float current, float target, float maximumChange) {
        return current + MathHelper.clamp(MathHelper.wrapDegrees(target - current), -maximumChange, maximumChange);
    }

    private float approach(float current, float target, float maximumChange) {
        return current + MathHelper.clamp(target - current, -maximumChange, maximumChange);
    }

    private void stop(String message) {
        boolean wasActive = active;
        active = false;
        climbing = false;
        fireworkCooldown = 0;
        glideRetryCooldown = 0;
        RotationComponent.instance.stopRotation();
        if (message != null && (wasActive || "Пилот выключен".equals(message))) {
            MessageUtil.displayInfo(message);
        }
    }
}
