package tech.huihui.client.modules.impl.cosmetics;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.screens.animals.AnimalPickerScreen;
import tech.huihui.utility.render.model.ProceduralModel;

@ModuleAnnotation(
   name = "Animals",
   category = Category.COSMETICS,
   description = "Животные-компаньоны следуют за тобой"
)
public final class AnimalsModule extends Module {
   public static final AnimalsModule INSTANCE = new AnimalsModule();

   private final BooleanSetting visible = new BooleanSetting("Показывать", true);
   private final NumberSetting size = new NumberSetting("Размер", 1.0F, 0.5F, 2.5F, 0.05F);
   private final NumberSetting distance = new NumberSetting("Дистанция", 2.2F, 1.0F, 6.0F, 0.1F);
   private final NumberSetting followSpeed = new NumberSetting("Скорость", 1.0F, 0.4F, 2.0F, 0.05F);
   private final StringSetting selection = new StringSetting("Животные", "");
   private final ButtonSetting openPicker = new ButtonSetting("Открыть выбор животных", AnimalPickerScreen::open);

   private final Map<String, CompanionInstance> instances = new LinkedHashMap<>();
   private long lastTime;

   public List<String> getSelectedIds() {
      List<String> ids = new ArrayList<>();
      for (String part : this.selection.getValue().split(",")) {
         String id = part.trim();
         if (!id.isEmpty() && !ids.contains(id)) {
            ids.add(id);
         }
      }
      return ids;
   }

   public boolean isSelected(String id) {
      return this.getSelectedIds().contains(id);
   }

   public void setSelected(String id, boolean selected) {
      List<String> ids = new ArrayList<>(this.getSelectedIds());
      if (selected && !ids.contains(id)) {
         ids.add(id);
      } else if (!selected) {
         ids.remove(id);
      }
      this.selection.setValue(String.join(",", ids));
      this.rebuildInstances();
   }

   public void rebuildInstances() {
      this.instances.clear();
      if (mc.player == null || mc.world == null) {
         return;
      }
      for (String id : this.getSelectedIds()) {
         Companion companion = CompanionRegistry.byId(id);
         if (companion != null) {
            this.instances.put(id, new CompanionInstance(companion, mc.player.getX(), mc.player.getY(), mc.player.getZ()));
         }
      }
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.rebuildInstances();
   }

   @Override
   public void onDisable() {
      this.instances.clear();
      super.onDisable();
   }

   @EventTarget
   public void onWorld(EventRender3D event) {
      if (mc.player == null || mc.world == null || !visible.isEnabled() || this.instances.isEmpty()) {
         return;
      }

      this.updateInstances();

      Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();

      MatrixStack matrices = event.getMatrix();
      VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(262144));

      matrices.push();
      matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

      for (CompanionInstance instance : this.instances.values()) {
         float lerpX = (float) instance.x;
         float lerpY = (float) instance.y;
         float lerpZ = (float) instance.z;

         float time = instance.animTime;
         float hopY = 0.0F;
         if (instance.companion.hops) {
            hopY = Math.abs(MathHelper.sin(instance.walkDist * 5.0F)) * 0.45F;
         }

         float scale = instance.companion.scale * this.size.getCurrent();

         matrices.push();
         matrices.translate(lerpX, lerpY + hopY, lerpZ);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(instance.yaw));

         if (instance.companion.hops) {
            float s = hopY * 0.25F;
            matrices.scale(1.0F + s, 1.0F - s * 1.2F, 1.0F + s);
         }
         matrices.scale(scale, scale, scale);

         ProceduralModel model = instance.companion.model();
         instance.companion.animate(model, instance, time, instance.walkDist * 2.5F, instance.walkAmount);
         model.render(matrices, immediate, 0xF000F0, OverlayTexture.DEFAULT_UV);
         matrices.pop();
      }

      immediate.draw();
      matrices.pop();
   }

   private void updateInstances() {
      long now = System.nanoTime();
      float dt = (float) ((now - this.lastTime) / 1_000_000_000.0);
      this.lastTime = now;
      dt = MathHelper.clamp(dt, 0.001F, 0.1F);

      int index = 0;
      for (CompanionInstance instance : this.instances.values()) {
         instance.prevX = instance.x;
         instance.prevY = instance.y;
         instance.prevZ = instance.z;
         instance.age++;
         instance.animTime += dt * 3.0F;

         float px = (float) mc.player.getX();
         float pz = (float) mc.player.getZ();
         float distToPlayer = (float) Math.sqrt((instance.x - px) * (instance.x - px) + (instance.z - pz) * (instance.z - pz));

         if (distToPlayer > 16.0) {
            float angle = (float) (Math.random() * Math.PI * 2.0);
            instance.x = px + Math.cos(angle) * (this.distance.getCurrent() + 1.0);
            instance.z = pz + Math.sin(angle) * (this.distance.getCurrent() + 1.0);
         }

         float baseRadius = this.distance.getCurrent() + index * 0.7F;
         boolean following = instance.following;
         if (following) {
            following = distToPlayer > baseRadius + 3.5F;
         } else {
            following = distToPlayer > baseRadius + 5.0F;
         }
         instance.following = following;

         if (instance.idleTimer > 0.0F) {
            instance.idleTimer -= dt;
         }
         instance.thinkTimer -= dt;

         double dx = instance.targetX - instance.x;
         double dz = instance.targetZ - instance.z;
         double dist = Math.sqrt(dx * dx + dz * dz);
         double moved = Math.sqrt(instance.velX * instance.velX + instance.velZ * instance.velZ);

         if (following) {
            instance.targetX = px;
            instance.targetZ = pz;
            instance.idleTimer = 0.0F;
         } else if (instance.idleTimer <= 0.0F && (dist < 0.35F || instance.thinkTimer <= 0.0F)) {
            if (dist < 0.35F || moved < 0.6) {
               double angle = Math.random() * Math.PI * 2.0;
               double radius = baseRadius + 0.4F + Math.random() * 0.9F;
               instance.targetX = px + Math.cos(angle) * radius;
               instance.targetZ = pz + Math.sin(angle) * radius;
               instance.thinkTimer = 3.0F + (float) (Math.random() * 4.0F);
               instance.idleTimer = 1.5F + (float) (Math.random() * 2.5F);
            } else {
               instance.thinkTimer = 1.0F + (float) (Math.random() * 1.0F);
            }
         }

         double desiredSpeed = (following ? 4.0 : 2.2) * this.followSpeed.getCurrent();
         double dvx = 0.0;
         double dvz = 0.0;
         if (following || instance.idleTimer <= 0.0F) {
            dx = instance.targetX - instance.x;
            dz = instance.targetZ - instance.z;
            dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.5) {
               dvx = dx / dist * desiredSpeed;
               dvz = dz / dist * desiredSpeed;
            }
         }

         float k = Math.min(1.0F, 4.0F * dt);
         instance.velX += (dvx - instance.velX) * k;
         instance.velZ += (dvz - instance.velZ) * k;
         instance.x += instance.velX * dt;
         instance.z += instance.velZ * dt;

         moved = Math.sqrt(instance.velX * instance.velX + instance.velZ * instance.velZ);
         instance.walkDist += ((float) moved - instance.walkDist) * Math.min(1.0F, 6.0F * dt);
         instance.walkAmount = MathHelper.clamp((float) moved * 1.2F, 0.0F, 1.0F);

         double targetY = this.groundY(instance.targetX, instance.targetZ);
         double dy = targetY - instance.y;
         double lerp = Math.min(1.0, 12.0 * dt);
         instance.y += dy * lerp;
         if (instance.y < targetY - 0.5) {
            instance.y = targetY;
         }

         float moveYaw = (float) Math.toDegrees(Math.atan2(-instance.velX, -instance.velZ));
         float lookYaw = (float) Math.toDegrees(Math.atan2(px - instance.x, pz - instance.z));
         float lookWeight = MathHelper.clamp(1.0F - (float) moved * 8.0F, 0.0F, 1.0F);
         float targetYaw = MathHelper.lerpAngleDegrees(lookWeight, moveYaw, lookYaw);
         instance.yaw = MathHelper.lerpAngleDegrees(Math.min(1.0F, 8.0F * dt), instance.yaw, targetYaw);
         index++;
      }
   }

   private double groundY(double x, double z) {
      int top = mc.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(x, 320.0, z)).getY();
      if (top <= mc.world.getBottomY()) {
         return mc.player.getY() - 1.0;
      }
      return top + 0.05;
   }

   public void clearInstances() {
      this.instances.clear();
   }

   public Set<String> getInstanceIds() {
      return this.instances.keySet();
   }
}
