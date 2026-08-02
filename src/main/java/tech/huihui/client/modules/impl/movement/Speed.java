package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.combat.Aura;
import tech.huihui.utility.game.player.MovingUtil;
import tech.huihui.utility.predict.PredictUtils;

@ModuleAnnotation(
   name = "Speed",
   category = Category.MOVEMENT,
   description = "Ускоряет вас возле цели ауры"
)
public class Speed extends Module {
   public static final Speed INSTANCE = new Speed();
   private final ModeSetting mode = new ModeSetting("Mode", "Collision", "Metahvh", "Ванила");
   public final NumberSetting speed = new NumberSetting("Скорость", 0.36F, 0.0F, 20.0F, 0.01F);
   private final float melonBallSpeed = 0.36F;

   @EventTarget
   private void onUpdate(EventUpdate ignored) {
      if (mc.player != null && mc.world != null) {
         if (this.mode.is("Ванила")) {
            this.vanillaSpeed();
         } else if (this.mode.is("Metahvh")) {
            this.metahvhSpeed();
         } else {
            this.collisionSpeed();
         }
      }
   }

   private void vanillaSpeed() {
      if (MovingUtil.hasPlayerMovement()) {
         MovingUtil.setVelocity(this.speed.getCurrent());
      }
   }

   @Native
   private void metahvhSpeed() {
      ItemStack offHandItem = mc.player.getOffHandStack();
      StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
      StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
      String itemName = offHandItem.getName().getString();

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

      if (!mc.player.isOnGround()) {
         appliedSpeed *= 1.435F;
      }

      MovingUtil.setVelocity(appliedSpeed);
   }

   @Native
   private void collisionSpeed() {
      Aura aura = Aura.INSTANCE;
      LivingEntity target = aura.getTarget();
      if (target != null && target != mc.player) {
         Box aABB = mc.player.getBoundingBox().expand(1.2000000476837158D);
         if ((mc.player.isGliding() || target.getBoundingBox().intersects(aABB)) && (!mc.player.isGliding() || !(mc.player.getEyePos().distanceTo(PredictUtils.predict(target, target.getPos(), Aura.INSTANCE.predict.getCurrent() - 0.3F)) > 2.5D) && !(mc.player.getEyePos().distanceTo(target.getBoundingBox().getCenter()) > 2.5D))) {
            Vec3d newVelocity = getVec3d(target);
            mc.player.setVelocity(newVelocity);
         }
      }
   }

   @NotNull
   private static Vec3d getVec3d(LivingEntity target) {
      double deltaX = target.getX() - mc.player.getX();
      double deltaZ = target.getZ() - mc.player.getZ();
      if (mc.player.isGliding() && target.isGliding()) {
         deltaX = PredictUtils.predict(target, target.getPos(), Aura.INSTANCE.predict.getCurrent()).x - mc.player.getX();
         deltaZ = PredictUtils.predict(target, target.getPos(), Aura.INSTANCE.predict.getCurrent()).z - mc.player.getZ();
      }

      float targetYaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
      double radYaw = Math.toRadians((double)targetYaw);
      double force = 0.07200000107288361D;
      Vec3d velocity = mc.player.getVelocity();
      return new Vec3d(velocity.x + -Math.sin(radYaw) * 0.07200000107288361D, velocity.y, velocity.z + Math.cos(radYaw) * 0.07200000107288361D);
   }
}
