package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.combat.AntiBot;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.player.rotation.Rotation;

@ModuleAnnotation(
   name = "AntiElytraTarget",
   category = Category.MOVEMENT,
   description = "Смотрит в горизонт в сторону от противников, чтобы улететь на элитрах"
)
public final class AntiElytraTarget extends Module {
   public static final AntiElytraTarget INSTANCE = new AntiElytraTarget();
   private final NumberSetting range = new NumberSetting("Дистанция", 50.0F, 10.0F, 200.0F, 5.0F);
   private final NumberSetting speed = new NumberSetting("Скорость", 180.0F, 10.0F, 360.0F, 10.0F);

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || !mc.player.isGliding()) {
         return;
      }

      LivingEntity nearest = this.findNearestEnemy();
      if (nearest == null) {
         return;
      }

      Vec3d player = mc.player.getPos();
      Vec3d target = nearest.getPos();
      double dx = player.x - target.x;
      double dz = player.z - target.z;
      if (dx == 0.0D && dz == 0.0D) {
         return;
      }

      float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
      float pitch = 0.0F;

      RotationComponent.update(new Rotation(yaw, pitch), this.speed.getCurrent(), this.speed.getCurrent(), 360.0F, 0, 2);
   }

   private LivingEntity findNearestEnemy() {
      List<LivingEntity> enemies = new ArrayList<>();
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity living) {
            if (this.isValid(living)) {
               enemies.add(living);
            }
         }
      }

      LivingEntity nearest = null;
      double nearestDist = Double.MAX_VALUE;
      for (LivingEntity enemy : enemies) {
         double dist = mc.player.getPos().distanceTo(enemy.getPos());
         if (dist < nearestDist) {
            nearestDist = dist;
            nearest = enemy;
         }
      }
      return nearest;
   }

   private boolean isValid(LivingEntity entity) {
      if (entity == mc.player || !entity.isAlive()) {
         return false;
      }
      if (entity instanceof PlayerEntity) {
         PlayerEntity player = (PlayerEntity) entity;
         if (HuihuiClient.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
            return false;
         }
         if (AntiBot.INSTANCE.isBot(player)) {
            return false;
         }
      }
      return mc.player.getPos().distanceTo(entity.getPos()) <= (double) this.range.getCurrent();
   }
}
