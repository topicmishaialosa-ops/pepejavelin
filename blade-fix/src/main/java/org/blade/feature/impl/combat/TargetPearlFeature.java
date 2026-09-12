package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import org.blade.context.RotationContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;

public final class TargetPearlFeature extends Feature {
    public static final TargetPearlFeature INSTANCE = new TargetPearlFeature();

    public final ModeSetting mode = register(new ModeSetting(
            "Мод", "Хвх", "Хвх", "Легит"
    ));
    public final NumberSetting range = register(new NumberSetting(
            "Дистанция", 40.0D, 5.0D, 100.0D, 1.0D, "блоков"
    ));
    public final NumberSetting delay = register(new NumberSetting(
            "Задержка", 1.0D, 0.1D, 5.0D, 0.1D, " сек"
    ));
    public final BooleanSetting rotate = register(new BooleanSetting(
            "Ротация", true
    ));

    private long lastThrow = 0L;
    private long lastAim = 0L;
    private ThrownEnderpearl target;

    public TargetPearlFeature() {
        super("TargetPearl", "Перехватывает перлы врага", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    public void onDisable() {
        target = null;
        RotationContext.clear();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = event.getClient();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastThrow < (long) (delay.getValue() * 1000.0D)) {
            return;
        }

        if (target == null || !target.isAlive() || target.level() != mc.level) {
            target = findPearl(mc, player);
        }

        if (target == null || !hasPearl(player)) {
            return;
        }

        if (now - lastAim < 100L) {
            return;
        }

        RotationInfo aim = calculatePearlRotation(player, target);
        if (aim == null) {
            return;
        }
        lastAim = now;

        if (rotate.getValue()) {
            RotationContext.setRotation(aim.yaw, aim.pitch);
        }

        usePearl(mc, player);
        lastThrow = now;
    }

    private ThrownEnderpearl findPearl(Minecraft mc, LocalPlayer player) {
        if (mc.level == null) {
            return null;
        }

        ThrownEnderpearl best = null;
        double bestScore = Double.MAX_VALUE;
        double maxDist = range.getValue();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof ThrownEnderpearl pearl && pearl.getOwner() != player && pearl.isAlive()) {
                double distance = player.distanceToSqr(pearl);
                if (distance <= maxDist * maxDist && distance < bestScore) {
                    bestScore = distance;
                    best = pearl;
                }
            }
        }

        return best;
    }

    private boolean hasPearl(LocalPlayer player) {
        return player.getMainHandItem().is(Items.ENDER_PEARL)
                || player.getOffhandItem().is(Items.ENDER_PEARL)
                || findHotbarPearl(player) != -1;
    }

    private int findHotbarPearl(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.ENDER_PEARL)) {
                return i;
            }
        }
        return -1;
    }

    private void usePearl(Minecraft mc, LocalPlayer player) {
        if (player.getMainHandItem().is(Items.ENDER_PEARL)) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            return;
        }
        if (player.getOffhandItem().is(Items.ENDER_PEARL)) {
            mc.gameMode.useItem(player, InteractionHand.OFF_HAND);
            return;
        }

        int hotbar = findHotbarPearl(player);
        if (hotbar == -1) {
            return;
        }

        if (mode.is("Хвх")) {
            int previous = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(hotbar);
            player.connection.send(new ServerboundSetCarriedItemPacket(hotbar));
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            player.getInventory().setSelectedSlot(previous);
            player.connection.send(new ServerboundSetCarriedItemPacket(previous));
        } else {
            player.getInventory().setSelectedSlot(hotbar);
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    private RotationInfo calculatePearlRotation(LocalPlayer player, ThrownEnderpearl pearl) {
        Vec3 targetPos = pearl.getBoundingBox().getCenter();
        Vec3 targetVel = pearl.getDeltaMovement();
        Vec3 eye = player.getEyePosition();

        double dx = targetPos.x - eye.x;
        double dy = targetPos.y - eye.y;
        double dz = targetPos.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4D) {
            return null;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        double yawRad = Math.toRadians(yaw);

        for (float pitch = -80.0F; pitch <= 90.0F; pitch += 0.5F) {
            double pitchRad = Math.toRadians(pitch);
            Vec3 direction = new Vec3(
                    -Math.sin(yawRad) * Math.cos(pitchRad),
                    -Math.sin(pitchRad),
                    Math.cos(yawRad) * Math.cos(pitchRad)
            );
            Vec3 ourPos = eye;
            Vec3 ourVel = direction.scale(1.5D);
            Vec3 enemyPos = targetPos;
            Vec3 enemyVel = targetVel;

            for (int tick = 0; tick < 120; ++tick) {
                ourVel = ourVel.add(0.0D, -0.03D, 0.0D).scale(0.99D);
                ourPos = ourPos.add(ourVel);
                enemyVel = enemyVel.add(0.0D, -0.03D, 0.0D).scale(0.99D);
                enemyPos = enemyPos.add(enemyVel);
                if (ourPos.distanceToSqr(enemyPos) < 2.25D) {
                    return new RotationInfo(yaw, pitch);
                }

                if (ourPos.y < enemyPos.y - 8.0D) {
                    break;
                }
            }
        }

        return null;
    }

    private record RotationInfo(float yaw, float pitch) {
    }
}