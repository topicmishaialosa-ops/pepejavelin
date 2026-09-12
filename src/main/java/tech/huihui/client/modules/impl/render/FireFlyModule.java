package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector4i;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.math.Timer;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "FireFly",
   category = Category.RENDER,
   description = "Светлячки, летающие вокруг игрока"
)
public final class FireFlyModule extends Module {
   public static final FireFlyModule INSTANCE = new FireFlyModule();

   private final NumberSetting amount = new NumberSetting("Amount", 100.0F, 1.0F, 300.0F, 1.0F);
   private final NumberSetting size = new NumberSetting("Size", 0.3F, 0.05F, 1.5F, 0.05F);
   private final NumberSetting lifeTime = new NumberSetting("Life Time", 10.0F, 1.0F, 30.0F, 1.0F);
   private final BooleanSetting randomColor = new BooleanSetting("Random Color", false);
   private final BooleanSetting spin = new BooleanSetting("Spin", false);
   private final BooleanSetting glow = new BooleanSetting("Glow", false);
   private final BooleanSetting blur = new BooleanSetting("Blur", false);

   private final List<Particle> particles = new ArrayList<>();
   private net.minecraft.client.world.ClientWorld lastWorld = null;

   @EventTarget
   public void onWorld(EventRender3D event) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (mc.world != lastWorld) {
         particles.clear();
         lastWorld = mc.world;
      }

      MatrixStack matrixStack = event.getMatrix();
      float partialTicks = event.getPartialTicks();

      while (particles.size() < (int) amount.getCurrent()) {
         particles.add(new Particle());
      }

      for (int i = particles.size() - 1; i >= 0; i--) {
         Particle p = particles.get(i);
         p.update(partialTicks);
         if (p.shouldRemove()) {
            particles.remove(i);
         }
      }

      Camera camera = mc.gameRenderer.getCamera();
      if (camera == null) {
         return;
      }

      Vec3d cameraPos = camera.getPos();
      Quaternionf cameraOrientation = camera.getRotation();
      float s = size.getCurrent();

      for (Particle p : particles) {
         float alpha = p.animate.getValue() / 255.0F;
         if (alpha <= 0.0F) {
            continue;
         }

         double x = (p.x + p.xSpeed * partialTicks) - cameraPos.x;
         double y = (p.y + p.ySpeed * partialTicks) - cameraPos.y;
         double z = (p.z + p.zSpeed * partialTicks) - cameraPos.z;
         int c = p.color.withAlpha((int) (alpha * 255.0F)).getRGB();

         matrixStack.push();
         matrixStack.translate(x, y, z);
         matrixStack.multiply(cameraOrientation);
         if (spin.isEnabled()) {
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(p.rotation));
         }
         Render3DUtil.drawTexture(matrixStack.peek(), HuihuiClient.id("icons/firefly.png"),
            -s, -s, s * 2.0F, s * 2.0F, new Vector4i(c, c, c, c), false);

         if (glow.isEnabled() || blur.isEnabled()) {
            int cg = p.color.withAlpha((int) (alpha * 80.0F)).getRGB();
            Render3DUtil.drawTexture(matrixStack.peek(), HuihuiClient.id("icons/firefly.png"),
               -s * 2.0F, -s * 2.0F, s * 4.0F, s * 4.0F, new Vector4i(cg, cg, cg, cg), false);
         }
         matrixStack.pop();
      }
   }

   private final class Particle {
      double x;
      double y;
      double z;
      double xSpeed;
      double ySpeed;
      double zSpeed;
      float rotation;
      ColorRGBA color;
      Animation animate;
      Timer time;

      Particle() {
         double px = mc.player.getX();
         double py = mc.player.getEyeY();
         double pz = mc.player.getZ();
         double radius = 2.0 + Math.random() * 2.5;
         double theta = Math.random() * Math.PI * 2.0;
         double phi = Math.acos(2.0 * Math.random() - 1.0);
         this.x = px + radius * Math.sin(phi) * Math.cos(theta);
         this.y = py + radius * Math.cos(phi);
         this.z = pz + radius * Math.sin(phi) * Math.sin(theta);
         this.xSpeed = (Math.random() - 0.5) * 0.04;
         this.ySpeed = (Math.random() - 0.5) * 0.04;
         this.zSpeed = (Math.random() - 0.5) * 0.04;
         this.rotation = (float) (Math.random() * Math.PI * 2.0);
         this.color = randomColor.isEnabled()
            ? ColorRGBA.fromHSB((float) Math.random(), 1.0F, 1.0F)
            : new ColorRGBA(255, 210, 80, 255);
         this.animate = new Animation(300L, Easing.LINEAR);
         this.animate.setValue(0.0F);
         this.animate.animateTo(255.0F);
         this.time = new Timer();
         this.time.reset();
      }

      void update(float partialTicks) {
         x += xSpeed * partialTicks;
         y += ySpeed * partialTicks;
         z += zSpeed * partialTicks;
         rotation += 0.02F * partialTicks;

         if (mc.player != null) {
            double dx = mc.player.getX() - x;
            double dy = mc.player.getEyeY() - y;
            double dz = mc.player.getZ() - z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 5.0) {
               x += dx * 0.02 * partialTicks;
               y += dy * 0.02 * partialTicks;
               z += dz * 0.02 * partialTicks;
            } else if (dist < 1.5) {
               x -= dx * 0.02 * partialTicks;
               y -= dy * 0.02 * partialTicks;
               z -= dz * 0.02 * partialTicks;
            }
         }

         animate.update();

         long life = (long) (lifeTime.getCurrent() * 1000.0F);
         if (time.getMillis() >= life) {
            animate.animateTo(0.0F);
         }
      }

      boolean shouldRemove() {
         long life = (long) (lifeTime.getCurrent() * 1000.0F);
         return time.getMillis() >= life + 400L && animate.getValue() <= 1.0F;
      }
   }
}