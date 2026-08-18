package tech.huihui.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.screens.swing.SwingEditorScreen;

@ModuleAnnotation(
   name = "SwingAnimation",
   category = Category.RENDER,
   description = "Кастомные анимации удара"
)
public final class SwingAnimation extends Module {
   public static final SwingAnimation INSTANCE = new SwingAnimation();
   public static float previewProgress = -1.0F;
   public ModeSetting animationMode = new ModeSetting("Режим", new String[]{"HMI Взмахи", "HMI Вперед", "HMI Обычная", "HMI Копье", "HMI Инструмент", "HMI Блок", "HMI Лопата", "Smooth", "Self", "Self2", "Down", "Forward", "Touch", "Pander", "Curt", "BlockHit", "Spin", "Backhand", "Overhead", "Stab", "Slash", "Reverse", "Flick", "Whip", "Rush", "Upswing", "Hammer", "Tornado", "Rotor", "Bounce", "Jab", "Cross", "Power", "Ghost", "Shake", "Blink", "ReverseSpin", "Swing360", "Flip", "Boomerang", "Cannon", "Pierce", "Wave", "Claw", "Wide", "Aim", "Precise", "Drop", "Vertical", "Custom"});
   public NumberSetting swingPower = new NumberSetting("Сила", 5.0F, 1.0F, 10.0F, 1.0F);
   public NumberSetting speed = new NumberSetting("Скорость", 7.0F, 0.0F, 20.0F, 1.0F);
   public NumberSetting angle = new NumberSetting("Угол", 0.0F, 0.0F, 360.0F, 1.0F, () -> {
      return this.animationMode.is("Self") || this.animationMode.is("Self2") || this.animationMode.is("Aim") || this.animationMode.is("Precise");
   });
   public NumberSetting rightX = new NumberSetting("Правая-X", 0.0F, -2.0F, 2.0F, 0.05F);
   public NumberSetting rightY = new NumberSetting("Правая-Y", 0.0F, -2.0F, 2.0F, 0.05F);
   public NumberSetting rightZ = new NumberSetting("Правая-Z", 0.0F, -2.0F, 2.0F, 0.05F);
   public NumberSetting leftX = new NumberSetting("Левая-X", 0.0F, -2.0F, 2.0F, 0.05F);
   public NumberSetting leftY = new NumberSetting("Левая-Y", 0.0F, -2.0F, 2.0F, 0.05F);
   public NumberSetting leftZ = new NumberSetting("Левая-Z", 0.0F, -2.0F, 2.0F, 0.05F);
   public NumberSetting customStartX = new NumberSetting("Старт X", 0.0F, -180.0F, 180.0F, 1.0F, this::isCustomMode);
   public NumberSetting customStartY = new NumberSetting("Старт Y", 0.0F, -180.0F, 180.0F, 1.0F, this::isCustomMode);
   public NumberSetting customStartZ = new NumberSetting("Старт Z", 0.0F, -180.0F, 180.0F, 1.0F, this::isCustomMode);
   public NumberSetting customEndX = new NumberSetting("Конец X", -90.0F, -180.0F, 180.0F, 1.0F, this::isCustomMode);
   public NumberSetting customEndY = new NumberSetting("Конец Y", 0.0F, -180.0F, 180.0F, 1.0F, this::isCustomMode);
   public NumberSetting customEndZ = new NumberSetting("Конец Z", 0.0F, -180.0F, 180.0F, 1.0F, this::isCustomMode);
   public NumberSetting customPosX = new NumberSetting("Позиция X", 0.0F, -3.0F, 3.0F, 0.01F, this::isCustomMode);
   public NumberSetting customPosY = new NumberSetting("Позиция Y", 0.0F, -3.0F, 3.0F, 0.01F, this::isCustomMode);
   public NumberSetting customPosZ = new NumberSetting("Позиция Z", 0.0F, -3.0F, 3.0F, 0.01F, this::isCustomMode);
   public NumberSetting customScale = new NumberSetting("Масштаб", 1.0F, 0.1F, 3.0F, 0.01F, this::isCustomMode);
public ModeSetting customCurve = new ModeSetting("Кривая", this::isCustomMode, new String[]{"Линейная", "Плавная"});
    public ButtonSetting openEditor = new ButtonSetting("Открыть редактор анимации", SwingEditorScreen::openEditor);
    public BooleanSetting showHands = new BooleanSetting("Показать руки", false);

   private SwingAnimation() {
   }

   private boolean isCustomMode() {
      return this.animationMode.is("Custom");
   }

   public boolean isPreviewing() {
      return SwingAnimation.previewProgress >= 0.0F;
   }

   public float getPreviewProgress() {
      return SwingAnimation.previewProgress;
   }

   public void setPreviewProgress(float progress) {
      SwingAnimation.previewProgress = MathHelper.clamp(progress, 0.0F, 1.0F);
   }

   public void stopPreview() {
      SwingAnimation.previewProgress = -1.0F;
   }

   public static boolean isEditorOpen() {
      return mc.currentScreen instanceof SwingEditorScreen;
   }

public void applyEditorPosition(MatrixStack matrices, Arm arm) {
       if (mc.player == null || arm != mc.player.getMainArm()) {
          return;
       }
        matrices.translate(-0.56F, 0.52F, 0.25F);
     }

   private float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

public void renderSwordAnimation(MatrixStack matrices, float swingProgress, float equipProgress, Arm arm) {
        if (this.isPreviewing()) {
            swingProgress = this.getPreviewProgress();
        } else {
            float speedFactor = Math.max(this.speed.getCurrent(), 0.1F) / 7.0F;
            swingProgress = MathHelper.clamp(swingProgress * speedFactor, 0.0F, 1.0F);
        }
        if (arm == Arm.RIGHT) {
            matrices.translate(this.rightX.getCurrent(), this.rightY.getCurrent(), this.rightZ.getCurrent());
        } else {
            matrices.translate(this.leftX.getCurrent(), this.leftY.getCurrent(), this.leftZ.getCurrent());
        }
        float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966D * 2.0D);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * 3.1415927F);
        float power = this.swingPower.getCurrent();
        float ang = this.angle.getCurrent();
        String mode = this.animationMode.get();
        float f;
        float g;
        float sinExtra;
        switch(mode) {
      case "HMI Взмахи": {
         float swingRot = hmiSwingRot(swingProgress);
         float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.8F * swingRot, 0.3F * swingRot, -0.5F * swing);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * swing));
         break;
      }
      case "HMI Вперед": {
         float swingRot = hmiSwingRot(swingProgress);
         float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.12F * swingRot, 0.04F * swingRot, -0.95F * swing);
         matrices.translate(0.02F * swing, 0.1F * swing, -0.1F * swingRot);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(8.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-14.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-18.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(32.0F * swing));
         break;
      }
      case "HMI Обычная": {
         float swingRot = hmiSwingRot(swingProgress);
         float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.1F * swingRot, 0.1F * swingRot, -0.1F * swing);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * swing));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing));
         break;
      }
      case "HMI Копье": {
         float swingRot = hmiSwingRot(swingProgress);
         float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.0F, 0.0F, 0.45F * swingRot);
         matrices.translate(-0.25F * swing, -0.35F * swingRot, -0.6F * swing);
         matrices.translate(0.0F, 0.1F * swing, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swingRot));
         break;
      }
      case "HMI Инструмент": {
         float swingRot = hmiSwingRot(swingProgress);
         float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.1F * swingRot, 0.1F * swingRot, -0.5F * swing);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * swing));
         break;
      }
      case "HMI Блок": {
         float swingRot = hmiSwingRot(swingProgress);
         float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.1F * swingRot, 0.1F * swingRot, -0.2F * swing);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * swing));
         break;
      }
      case "HMI Лопата": {
         float swingRot = hmiSwingRot(swingProgress);
         float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.translate(0.0F, 0.15F * swingRot, -0.25F * swingRot);
         matrices.translate(0.0F, 0.0F, -0.2F * swing);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
         break;
      }
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
          matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F + g * -100.0F * power / 5.0F));
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * power / 5.0F));
          break;
      case "Pander":
         matrices.translate(0.56F, -0.52F, -0.72F);
         matrices.scale(0.8F, 0.8F, 0.8F);
         f = 1.0F - MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), mc.gameRenderer.firstPersonRenderer.prevEquipProgressMainHand, mc.gameRenderer.firstPersonRenderer.equipProgressMainHand);
         matrices.translate(0.3D - (double)(anim * 0.15F), (double)(0.2F - f * 0.12F), (double)(-0.15F - anim * 0.13F));
          matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F - 10.0F * anim * power / 5.0F));
          matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-16.0F - 8.0F * anim * power / 5.0F));
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83.0F - 26.0F * anim * power / 5.0F));
         break;
       case "BlockHit":
          matrices.translate(0.56F, -0.52F, -0.72F);
          f = MathHelper.sin((float)((double)(swingProgress * swingProgress) * 3.141592653589793D));
          matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F));
          g = MathHelper.sin((float)((double)MathHelper.sqrt(swingProgress) * 3.141592653589793D));
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * -20.0F * power / 5.0F));
          matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(g * -20.0F * power / 5.0F));
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F * power / 5.0F));
          matrices.translate(0.4F, 0.2F, 0.2F);
          matrices.translate(-0.5F, 0.08F, 0.0F);
          matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * power / 5.0F));
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
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ang - sin2 * 40.0F * power / 5.0F));
          break;
       case "Precise":
          matrices.translate(0.56F, -0.52F, -0.72F);
          matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
          matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F));
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-ang * 0.5F - sin2 * 90.0F * power / 5.0F));
          break;
      case "Drop":
         matrices.translate(0.56F, -0.52F + anim * 0.4F * power / 5.0F, -0.72F);
         break;
       case "Vertical":
          matrices.translate(0.56F, -0.52F, -0.72F);
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 360.0F));
          break;
       case "Custom": {
          matrices.translate(0.56F + this.customPosX.getCurrent(), -0.52F + this.customPosY.getCurrent(), -0.72F + this.customPosZ.getCurrent());
          float customScale = this.customScale.getCurrent();
          matrices.scale(customScale, customScale, customScale);
          float t = this.customCurve.is("Плавная")
                ? swingProgress * swingProgress * (3.0F - 2.0F * swingProgress)
                : swingProgress;
          matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.lerp(this.customStartX.getCurrent(), this.customEndX.getCurrent(), t)));
          matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.lerp(this.customStartY.getCurrent(), this.customEndY.getCurrent(), t)));
          matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.lerp(this.customStartZ.getCurrent(), this.customEndZ.getCurrent(), t)));
          break;
       }
       }

   }

   public void renderArmAnimation(MatrixStack matrices, float swingProgress, Arm arm) {
      if (this.isPreviewing()) {
         swingProgress = this.getPreviewProgress();
      } else {
         float speedFactor = Math.max(this.speed.getCurrent(), 0.1F) / 7.0F;
         swingProgress = MathHelper.clamp(swingProgress * speedFactor, 0.0F, 1.0F);
      }
      if (arm == Arm.RIGHT) {
         matrices.translate(this.rightX.getCurrent(), this.rightY.getCurrent(), this.rightZ.getCurrent());
      } else {
         matrices.translate(this.leftX.getCurrent(), this.leftY.getCurrent(), this.leftZ.getCurrent());
      }
      String mode = this.animationMode.get();
      float swingRot = hmiSwingRot(swingProgress);
      float swing = hmiEase(MathHelper.sin(swingProgress * 3.1415927F));
      switch(mode) {
      case "HMI Взмахи":
         matrices.translate(0.8F * swingRot, 0.3F * swingRot, -0.5F * swing);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * swing));
         break;
      case "HMI Вперед":
         matrices.translate(0.12F * swingRot, 0.04F * swingRot, -0.95F * swing);
         matrices.translate(0.02F * swing, 0.1F * swing, -0.1F * swingRot);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(8.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-14.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-18.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(32.0F * swing));
         break;
      case "HMI Обычная":
         matrices.translate(0.1F * swingRot, 0.1F * swingRot, -0.1F * swing);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * swing));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing));
         break;
      case "HMI Копье":
         matrices.translate(0.0F, 0.0F, 0.45F * swingRot);
         matrices.translate(-0.25F * swing, -0.35F * swingRot, -0.6F * swing);
         matrices.translate(0.0F, 0.1F * swing, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swingRot));
         break;
      case "HMI Инструмент":
         matrices.translate(0.1F * swingRot, 0.1F * swingRot, -0.5F * swing);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F * swing));
         break;
      case "HMI Блок":
         matrices.translate(0.1F * swingRot, 0.1F * swingRot, -0.2F * swing);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * swing));
         break;
      case "HMI Лопата":
         matrices.translate(0.0F, 0.15F * swingRot, -0.25F * swingRot);
         matrices.translate(0.0F, 0.0F, -0.2F * swing);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
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

   private static float hmiSwingRot(float swingProgress) {
      if (swingProgress < 0.6F) {
         return MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F);
      }
      return MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
   }

   private static float hmiEase(float value) {
      float c1 = 1.70158F;
      float c2 = c1 * 1.525F;
      if (value < 0.5F) {
         float doubled = 2.0F * value;
         return doubled * doubled * ((c2 + 1.0F) * doubled - c2) * 0.5F;
      }
      float shifted = 2.0F * value - 2.0F;
      return (shifted * shifted * ((c2 + 1.0F) * shifted + c2) + 2.0F) * 0.5F;
   }
}
