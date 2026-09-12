package org.blade.feature.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.impl.combat.AuraFeature;
import org.blade.utils.game.player.MovingUtil;

public final class SpeedFeature extends Feature {
    public static final SpeedFeature INSTANCE = new SpeedFeature();

    private final ModeSetting mode = register(new ModeSetting("Mode", "Collision", "Metahvh", "Ванила", "Collision"));
    public final NumberSetting speed = register(new NumberSetting("Скорость", 0.36, 0.0, 20.0, 0.01, ""));
    private final float melonBallSpeed = 0.36F;

    public SpeedFeature() {
        super("Speed", "Ускоряет вас возле цели ауры", FeatureCategory.MOVEMENT, BindSetting.UNBOUND);
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (this.mode.is("Ванила")) {
            vanillaSpeed();
        } else if (this.mode.is("Metahvh")) {
            metahvhSpeed();
        } else {
            collisionSpeed();
        }
    }

    private void vanillaSpeed() {
        if (MovingUtil.hasPlayerMovement()) {
            MovingUtil.setVelocity(this.speed.getValue().doubleValue());
        }
    }

    private void metahvhSpeed() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack offHand = player.getOffhandItem();
        MobEffectInstance speedEffect = player.getEffect(MobEffects.SPEED);
        MobEffectInstance slowEffect = player.getEffect(MobEffects.SLOWNESS);
        String itemName = offHand.getHoverName().getString();

        float appliedSpeed;
        if (speedEffect != null) {
            if (speedEffect.getAmplifier() == 2) {
                appliedSpeed = this.melonBallSpeed * 1.155F;
                if (itemName.contains("Ломтик Дыни")) {
                    appliedSpeed = 0.41755F;
                }
            } else if (speedEffect.getAmplifier() == 1) {
                appliedSpeed = this.melonBallSpeed;
            } else {
                appliedSpeed = 0.0F;
            }
        } else {
            appliedSpeed = this.melonBallSpeed * 0.68F;
        }

        if (slowEffect != null) {
            appliedSpeed *= 0.835F;
        }
        if (!player.onGround()) {
            appliedSpeed *= 1.435F;
        }

        MovingUtil.setVelocity(appliedSpeed);
    }

    private void collisionSpeed() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        AuraFeature aura = AuraFeature.getEnabled();
        if (aura == null || aura.getTarget() == null) return;
        LivingEntity target = aura.getTarget();
        if (target == null || target == player) return;

        AABB area = player.getBoundingBox().inflate(1.2000000476837158D);
        if (player.isFallFlying() || target.getBoundingBox().intersects(area)) {
            if (player.isFallFlying() && (player.getEyePosition().distanceTo(target.getBoundingBox().getCenter()) > 2.5D)) {
                return;
            }
            velocityToward(player, target);
        }
    }

    private void velocityToward(LocalPlayer player, LivingEntity target) {
        double deltaX = target.getX() - player.getX();
        double deltaZ = target.getZ() - player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D;
        double radYaw = Math.toRadians(targetYaw);
        double force = 0.07200000107288361D;
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(
                velocity.x - Math.sin(radYaw) * force,
                velocity.y,
                velocity.z + Math.cos(radYaw) * force
        );
    }
}