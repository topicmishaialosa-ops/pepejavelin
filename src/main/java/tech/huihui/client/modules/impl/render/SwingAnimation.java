package tech.huihui.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "SwingAnimation",
   category = Category.RENDER,
   description = "Кастомные анимации удара"
)
public final class SwingAnimation extends Module {
   public static final SwingAnimation INSTANCE = new SwingAnimation();
   public ModeSetting animationMode = new ModeSetting("Режим", new String[]{"Smooth", "Self", "Self2", "Down", "Forward", "Touch", "Pander", "Curt", "BlockHit", "Spin", "Backhand", "Overhead", "Stab", "Slash", "Reverse", "Flick", "Whip", "Rush", "Upswing", "Hammer", "Tornado", "Rotor", "Bounce", "Jab", "Cross", "Power", "Ghost", "Shake", "Blink", "ReverseSpin", "Swing360", "Flip", "Boomerang", "Cannon", "Pierce", "Wave", "Claw", "Wide", "Aim", "Precise", "Drop", "Vertical"});
   public NumberSetting swingPower = new NumberSetting("Сила", 5.0F, 1.0F, 10.0F, 1.0F, () -> {
      return !this.animationMode.is("BlockHit") && !this.animationMode.is("Pander") && !this.animationMode.is("Curt") && !this.animationMode.is("Aim") && !this.animationMode.is("Precise");
   });
   public NumberSetting speed = new NumberSetting("Скорость", 7.0F, 0.0F, 10.0F, 1.0F);
   public NumberSetting angle = new NumberSetting("Угол", 0.0F, 0.0F, 360.0F, 1.0F, () -> {
      return this.animationMode.is("Self") || this.animationMode.is("Self2") || this.animationMode.is("Aim") || this.animationMode.is("Precise");
   });

   private SwingAnimation() {
   }

   public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
      float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966D * 2.0D);
      float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
      float power = this.swingPower.getCurrent();
      float ang = this.angle.getCurrent();
      String mode = this.animationMode.get();
      float f;
      float g;
      float sinExtra;
      switch(mode) {
      case "Smooth":
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = power * 10.0F;
         g = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + g * (-f / 4.0F)));
         sinExtra = MathHelper.sin(MathHelper.sqrt(swingProgress * swingProgress) * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sinExtra * -(f / 4.0F)));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinExtra * -f));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
         break;
      case "Self2":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ang - power * 10.0F * anim));
         break;
      case "Forward":
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = 35.0F;
         matrices.translate(0.0D, 0.0D, -0.3D * (double)sin2);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -f));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * f));
         break;
      case "Self":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ang - power * 10.0F * anim));
         break;
      case "Down":
         matrices.translate(0.56F, -0.52F - anim * power / 24.0F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
         break;
      case "Touch":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.scale(1.0F, 1.0F, 1.0F + anim * power / 4.0F);
         matrices.translate(0.0F, 0.0F, -0.265F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
         break;
      case "Curt":
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = MathHelper.sqrt(swingProgress);
         g = MathHelper.sin(f * 3.1415927F);
         sinExtra = MathHelper.sin(swingProgress * 3.1415927F);
         matrices.translate(0.4F - g * 0.2F, -0.2F + g * 0.3F, -0.5F - sinExtra * 0.2F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(91.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F + g * -100.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
         break;
      case "Pander":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.scale(0.8F, 0.8F, 0.8F);
         f = 1.0F - MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), mc.gameRenderer.firstPersonRenderer.prevEquipProgressMainHand, mc.gameRenderer.firstPersonRenderer.equipProgressMainHand);
         matrices.translate(0.3D - (double)(anim * 0.15F), (double)(0.2F - f * 0.12F), (double)(-0.15F - anim * 0.13F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F - 10.0F * anim));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-16.0F - 8.0F * anim));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0F - 26.0F * anim));
         break;
      case "BlockHit":
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = MathHelper.sin((float)((double)(swingProgress * swingProgress) * 3.141592653589793D));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
         g = MathHelper.sin((float)((double)MathHelper.sqrt(swingProgress) * 3.141592653589793D));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -20.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g * -20.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
         matrices.translate(0.4F, 0.2F, 0.2F);
         matrices.translate(-0.5F, 0.08F, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
         break;
      case "Spin":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim * 360.0F * power / 5.0F));
         break;
      case "Backhand":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -160.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(25.0F));
         break;
      case "Overhead":
         matrices.translate(0.56F, -0.52F + anim * 0.35F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-130.0F + anim * 80.0F));
         break;
      case "Stab":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.0F, 0.0F, -sin2 * 0.55F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
         break;
      case "Slash":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -90.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F));
         break;
      case "Reverse":
         matrices.translate(0.56F, -0.52F, -0.72F);
         f = power * 10.0F;
         g = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F + g * (f / 4.0F)));
         sinExtra = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sinExtra * (f / 4.0F)));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinExtra * f));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
         break;
      case "Flick":
         matrices.translate(0.56F, -0.52F, -0.72F + anim * 0.2F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -120.0F));
         break;
      case "Whip":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim * 130.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * -60.0F));
         break;
      case "Rush":
         matrices.translate(0.56F, -0.52F, -0.72F - sin2 * 0.5F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + sin2 * 25.0F));
         break;
      case "Upswing":
         matrices.translate(0.56F, -0.52F - sin2 * 0.45F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -160.0F));
         break;
      case "Hammer":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-170.0F + anim * 90.0F));
         matrices.scale(1.0F, 1.0F + anim * 0.15F, 1.0F);
         break;
      case "Tornado":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim * 360.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -30.0F));
         break;
      case "Rotor":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * 360.0F));
         break;
      case "Bounce":
         matrices.translate(0.56F, -0.52F - sin2 * 0.25F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F + sin2 * -60.0F));
         break;
      case "Jab":
         matrices.translate(0.56F, -0.52F, -0.72F - sin2 * 0.6F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F));
         break;
      case "Cross":
         matrices.translate(0.56F, -0.52F, -0.72F);
         g = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F + g * -power));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -100.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F));
         break;
      case "Power":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.scale(1.0F + anim * 0.1F, 1.0F + anim * 0.1F, 1.0F + anim * 0.1F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim * 70.0F * power / 5.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         break;
      case "Ghost":
         matrices.translate(0.56F, -0.52F - anim * 0.15F, -0.72F + anim * 0.25F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim * 180.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
         break;
      case "Shake":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 40.0F * power / 5.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -25.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * 20.0F));
         break;
      case "Blink": {
         matrices.translate(0.56F, -0.52F, -0.72F);
         float blinkScale = sin2 > 0.5F ? 0.2F : 1.2F;
         matrices.scale(blinkScale, blinkScale, blinkScale);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         break;
      }
      case "ReverseSpin":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * -360.0F));
         break;
      case "Swing360":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim * 360.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-30.0F));
         break;
      case "Flip":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * 360.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         break;
      case "Boomerang":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(anim * 270.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * 50.0F));
         break;
      case "Cannon":
         matrices.translate(0.56F, -0.52F, -0.72F - sin2 * 0.35F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F - sin2 * 45.0F));
         break;
      case "Pierce":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.0F, 0.0F, -sin2 * 0.5F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
         break;
      case "Wave":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 40.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * 25.0F));
         break;
      case "Claw":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-60.0F - anim * 40.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
         break;
      case "Wide":
         matrices.translate(0.56F, -0.52F, -0.72F);
         g = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F - g * 30.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * 55.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-50.0F));
         break;
      case "Aim":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-35.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ang - sin2 * 40.0F));
         break;
      case "Precise":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ang * 0.5F - sin2 * 90.0F));
         break;
      case "Drop":
         matrices.translate(0.56F, -0.52F + anim * 0.4F * power / 5.0F, -0.72F);
         break;
      case "Vertical":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 360.0F));
         break;
      }

   }

   private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      matrices.translate((float)i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      float f = MathHelper.sin(swingProgress * swingProgress * 3.1415927F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * (45.0F + f * -20.0F)));
      float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * g * -20.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * -45.0F));
   }
}
