package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.other.EventSpawnEntity;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "TargetPearl",
   category = Category.COMBAT,
   description = "Перехватывает перлы врага"
)
public final class TargetPearl extends Module {
   public static final TargetPearl INSTANCE = new TargetPearl();
   private final ModeSetting mode = new ModeSetting("Мод", new String[]{"Хвх", "Легит"});
   private final NumberSetting range = new NumberSetting("Дистанция", 40.0F, 5.0F, 100.0F, 1.0F, "Максимальная дистанция до перла");
   private final NumberSetting delay = new NumberSetting("Задержка", 1.0F, 0.1F, 5.0F, 0.1F, "Пауза между бросками (в секундах)");
   private final BooleanSetting rotate = new BooleanSetting("Ротация", true);
   private final Timer timer = new Timer();
   private EnderPearlEntity target;

   @EventTarget
   private void onSpawn(EventSpawnEntity event) {
      Entity entity = event.getEntity();
      if (entity instanceof EnderPearlEntity pearl && this.isEnemyPearl(pearl)) {
         this.target = pearl;
      }
   }

   @EventTarget
   private void onUpdate(EventUpdate ignored) {
      if (mc.player == null || mc.world == null || !this.timer.finished((long)(this.delay.getCurrent() * 1000.0F))) {
         return;
      }

      if (this.target == null || !this.target.isAlive() || this.target.getWorld() != mc.world) {
         this.target = this.findPearl();
      }

      if (this.target == null || !this.hasPearl()) {
         return;
      }

      Rotation aim = this.calculatePearlRotation(this.target.getBoundingBox().getCenter());
      if (aim == null) {
         return;
      }

      if (this.rotate.isEnabled()) {
         mc.player.setYaw(aim.getYaw());
         mc.player.setPitch(aim.getPitch());
      }

      if (this.mode.is("Хвх")) {
         PlayerInventoryUtil.swapAndUseHvH(Items.ENDER_PEARL);
      } else {
         PlayerInventoryUtil.swapAndUseLegit(Items.ENDER_PEARL);
      }

      this.timer.reset();
   }

   private boolean isEnemyPearl(EnderPearlEntity pearl) {
      return pearl.getOwner() != mc.player;
   }

   private EnderPearlEntity findPearl() {
      if (mc.world == null) {
         return null;
      }

      EnderPearlEntity best = null;
      double bestScore = Double.MAX_VALUE;

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EnderPearlEntity pearl && this.isEnemyPearl(pearl) && pearl.isAlive()) {
            double distance = mc.player.distanceTo(pearl);
            if (distance <= (double)this.range.getCurrent() && distance < bestScore) {
               bestScore = distance;
               best = pearl;
            }
         }
      }

      return best;
   }

   private boolean hasPearl() {
      return mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL
            || mc.player.getOffHandStack().getItem() == Items.ENDER_PEARL
            || PlayerInventoryUtil.find(Items.ENDER_PEARL, 0, 45) != -1;
   }

   private Rotation calculatePearlRotation(Vec3d targetPos) {
      Vec3d eye = mc.player.getEyePos();
      double dx = targetPos.x - eye.x;
      double dy = targetPos.y - eye.y;
      double dz = targetPos.z - eye.z;
      double horizontal = Math.hypot(dx, dz);
      if (horizontal < 1.0E-4D) {
         return null;
      }

      float yaw = (float)(Math.toDegrees(Math.atan2(-dx, dz)));
      double yawRad = Math.toRadians((double)yaw);
      float bestPitch = 0.0F;
      double bestDistance = Double.MAX_VALUE;

      for (float pitch = -70.0F; pitch <= 90.0F; pitch += 0.5F) {
         double pitchRad = Math.toRadians((double)pitch);
         Vec3d direction = new Vec3d(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
         );
         Vec3d velocity = direction.multiply(1.5D);
         Vec3d position = eye;
         double closest = Double.MAX_VALUE;

         for (int tick = 0; tick < 200; ++tick) {
            velocity = velocity.add(0.0D, -0.03D, 0.0D).multiply(0.99D);
            position = position.add(velocity);
            double distanceSq = position.squaredDistanceTo(targetPos);
            if (distanceSq < closest) {
               closest = distanceSq;
            }

            if (position.y < targetPos.y - 4.0D) {
               break;
            }
         }

         if (closest < bestDistance) {
            bestDistance = closest;
            bestPitch = pitch;
         }
      }

      if (bestDistance > 9.0D) {
         return null;
      }

      return new Rotation(yaw, bestPitch);
   }
}
