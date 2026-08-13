package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.combat.AntiBot;
import tech.huihui.client.modules.impl.combat.Aura;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.predict.PredictUtils;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "ElytraPredict",
   category = Category.MOVEMENT,
   description = "Предугадывает траекторию полёта противника на элитрах, летит туда и подсвечивает точку"
)
public final class ElytraPredict extends Module {
   public static final ElytraPredict INSTANCE = new ElytraPredict();
   private final NumberSetting range = new NumberSetting("Дистанция", 80.0F, 10.0F, 300.0F, 5.0F);
   private final NumberSetting ticks = new NumberSetting("Тики предикта", 12.0F, 1.0F, 60.0F, 1.0F);
   private final NumberSetting speed = new NumberSetting("Скорость поворота", 90.0F, 10.0F, 360.0F, 10.0F);
   private final NumberSetting heightOffset = new NumberSetting("Высота предикта", 3.0F, -10.0F, 20.0F, 0.5F);
   private final NumberSetting boxSize = new NumberSetting("Размер квадрата", 2.0F, 0.5F, 8.0F, 0.5F);
   private final NumberSetting maxPredictDistance = new NumberSetting("Макс. дистанция предикта", 8.0F, 1.0F, 60.0F, 1.0F);
   private final BooleanSetting render = new BooleanSetting("Визуализация", true);
   private final BooleanSetting onlyAuraTarget = new BooleanSetting("Только цель ауры", false);
   private Vec3d predictedPoint;

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || !mc.player.isGliding()) {
         return;
      }

      LivingEntity target = this.findTarget();
      if (target == null || !target.isGliding()) {
         this.predictedPoint = null;
         return;
      }

      Vec3d predicted = PredictUtils.predict(target, target.getPos(), this.ticks.getCurrent());
      Vec3d targetPos = target.getPos();
      double rawDistance = targetPos.distanceTo(predicted);
      float maxDistance = this.maxPredictDistance.getCurrent();
      if (rawDistance > (double) maxDistance && rawDistance > 1.0E-4D) {
         predicted = targetPos.add(predicted.subtract(targetPos).multiply((double) maxDistance / rawDistance));
      }
      this.predictedPoint = predicted;
      Vec3d playerPos = mc.player.getPos();
      double playerDistance = playerPos.distanceTo(predicted);
      if (playerDistance < 4.0D) {
         predicted = targetPos;
      }
      Vec3d flyTo = new Vec3d(predicted.x, predicted.y + this.heightOffset.getCurrent(), predicted.z);
      double dx = flyTo.x - playerPos.x;
      double dz = flyTo.z - playerPos.z;
      double dy = flyTo.y - playerPos.y;
      if (dx == 0.0D && dz == 0.0D && dy == 0.0D) {
         return;
      }

      double horizontal = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
      if (horizontal < 1.0E-4D) {
         pitch = dy > 0.0D ? -90.0F : 90.0F;
      }

      RotationComponent.update(new Rotation(yaw, pitch), this.speed.getCurrent(), this.speed.getCurrent(), 360.0F, 0, 2);
   }

   @EventTarget
   private void onRenderWorld(EventRender3D event) {
      if (!this.render.isEnabled() || this.predictedPoint == null || mc.world == null || mc.player == null) {
         return;
      }

      int blue = 0xFF3E8BFF;
      int fill = 0x553E8BFF;
      float half = this.boxSize.getCurrent() / 2.0F;
      Vec3d p = this.predictedPoint;
      Vec3d c1 = new Vec3d(p.x - (double)half, p.y, p.z - (double)half);
      Vec3d c2 = new Vec3d(p.x + (double)half, p.y, p.z - (double)half);
      Vec3d c3 = new Vec3d(p.x + (double)half, p.y, p.z + (double)half);
      Vec3d c4 = new Vec3d(p.x - (double)half, p.y, p.z + (double)half);
      Render3DUtil.drawQuad(c1, c2, c3, c4, fill, false);
      Render3DUtil.drawLine(c1.x, p.y, c1.z, c2.x, p.y, c2.z, blue, 1.5F, false);
      Render3DUtil.drawLine(c2.x, p.y, c2.z, c3.x, p.y, c3.z, blue, 1.5F, false);
      Render3DUtil.drawLine(c3.x, p.y, c3.z, c4.x, p.y, c4.z, blue, 1.5F, false);
      Render3DUtil.drawLine(c4.x, p.y, c4.z, c1.x, p.y, c1.z, blue, 1.5F, false);
   }

   private LivingEntity findTarget() {
      if (this.onlyAuraTarget.isEnabled()) {
         Entity auraTarget = Aura.INSTANCE.getTarget();
         return auraTarget instanceof LivingEntity living ? living : null;
      }

      List<LivingEntity> candidates = new ArrayList<>();
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity living && this.isValid(living)) {
            candidates.add(living);
         }
      }

      LivingEntity nearest = null;
      double nearestDist = Double.MAX_VALUE;
      for (LivingEntity candidate : candidates) {
         double dist = mc.player.getPos().distanceTo(candidate.getPos());
         if (dist < nearestDist) {
            nearestDist = dist;
            nearest = candidate;
         }
      }
      return nearest;
   }

   private boolean isValid(LivingEntity entity) {
      if (entity == mc.player || !entity.isAlive()) {
         return false;
      }
      if (entity instanceof PlayerEntity player) {
         if (HuihuiClient.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
            return false;
         }
         if (AntiBot.INSTANCE.isBot(player)) {
            return false;
         }
      }
      return mc.player.getPos().distanceTo(entity.getPos()) <= (double) this.range.getCurrent();
   }

   private ElytraPredict() {
   }
}