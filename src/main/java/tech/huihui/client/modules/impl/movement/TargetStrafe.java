package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.player.EventMoveInput;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.combat.Aura;
import tech.huihui.utility.game.player.MovingUtil;

@ModuleAnnotation(
   name = "TargetStrafe",
   category = Category.MOVEMENT,
   description = "Кружит вокруг цели ауры"
)
public final class TargetStrafe extends Module {
   public static final TargetStrafe INSTANCE = new TargetStrafe();
   private final NumberSetting radius = new NumberSetting("Радиус", 2.5F, 1.0F, 6.0F, 0.1F);
   private final NumberSetting speed = new NumberSetting("Скорость", 1.2F, 0.2F, 3.0F, 0.1F);
   private final BooleanSetting noSpeeds = new BooleanSetting("Без спидов", true);
   private final BooleanSetting pushIn = new BooleanSetting("Вжиматься", false);

   @EventTarget
   private void onMoveInput(EventMoveInput event) {
      if (!this.noSpeeds.isEnabled() || mc.player == null || mc.player.isGliding() || mc.player.isSwimming() || mc.player.hasVehicle()) {
         return;
      }
      LivingEntity target = Aura.INSTANCE.getTarget();
      if (target == null || !target.isAlive()) {
         return;
      }
      Vec3d move = this.orbitDirection(target.getBoundingBox().getCenter(), this.effectiveRadius());
      float desired = (float)Math.toDegrees(Math.atan2(-move.x, move.z));
      this.setInputTowards(event, desired);
   }

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (this.noSpeeds.isEnabled() || mc.player == null || mc.player.isGliding() || mc.player.isSwimming() || mc.player.hasVehicle()) {
         return;
      }
      LivingEntity target = Aura.INSTANCE.getTarget();
      if (target == null || !target.isAlive()) {
         return;
      }
      Vec3d move = this.orbitDirection(target.getBoundingBox().getCenter(), this.effectiveRadius());
      mc.player.setVelocity(move.x * (double)this.speed.getCurrent(), mc.player.getVelocity().y, move.z * (double)this.speed.getCurrent());
   }

   private float effectiveRadius() {
      return this.pushIn.isEnabled() ? 0.1F : this.radius.getCurrent();
   }

   private Vec3d orbitDirection(Vec3d targetCenter, float orbitRadius) {
      double dx = mc.player.getX() - targetCenter.x;
      double dz = mc.player.getZ() - targetCenter.z;
      double dist = Math.max(Math.hypot(dx, dz), 1.0E-4D);
      double tx = dz / dist;
      double tz = -dx / dist;
      double rx = -dx / dist;
      double rz = -dz / dist;
      double w = MathHelper.clamp((dist - (double)orbitRadius) * 0.5D, -1.0D, 1.0D);
      double mx = tx + rx * w;
      double mz = tz + rz * w;
      double len = Math.hypot(mx, mz);
      return new Vec3d(mx / len, 0.0D, mz / len);
   }

   private void setInputTowards(EventMoveInput event, float targetAngle) {
      float yaw = mc.player.getYaw();
      float bestForward = 0.0F;
      float bestStrafe = 0.0F;
      float smallest = Float.MAX_VALUE;
      for (float f = -1.0F; f <= 1.0F; ++f) {
         for (float s = -1.0F; s <= 1.0F; ++s) {
            if (f == 0.0F && s == 0.0F) {
               continue;
            }
            double a = Math.toDegrees(MovingUtil.direction(yaw, f, s));
            float diff = Math.abs(MathHelper.wrapDegrees(targetAngle - (float)a));
            if (diff < smallest) {
               smallest = diff;
               bestForward = f;
               bestStrafe = s;
            }
         }
      }
      event.setForward(bestForward);
      event.setStrafe(bestStrafe);
   }
}
