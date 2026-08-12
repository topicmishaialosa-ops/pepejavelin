package tech.huihui.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "UseAnimation",
   category = Category.RENDER,
   description = "Кастомная анимация поедания и питья"
)
public final class UseAnimation extends Module {
   private static final String[] MODES = new String[]{"Без тряски", "Vanilla", "Ускоренная", "Слайд", "Поп", "Волна", "Спин", "Наклон", "Кастом"};
   public static final UseAnimation INSTANCE = new UseAnimation();
   public final ModeSetting mode = new ModeSetting("Режим", MODES);
   public final NumberSetting speed = new NumberSetting("Скорость анимации", 1.0F, 0.1F, 5.0F, 0.05F);
   public final NumberSetting delay = new NumberSetting("Задержка", 0.0F, 0.0F, 40.0F, 1.0F);
   public final NumberSetting power = new NumberSetting("Сила", 1.0F, 0.1F, 3.0F, 0.05F);
   public final NumberSetting offsetX = new NumberSetting("Сдвиг X", 0.0F, -2.0F, 2.0F, 0.05F, this::isTransformMode);
   public final NumberSetting offsetY = new NumberSetting("Сдвиг Y", 0.0F, -2.0F, 2.0F, 0.05F, this::isTransformMode);
   public final NumberSetting offsetZ = new NumberSetting("Сдвиг Z", 0.0F, -2.0F, 2.0F, 0.05F, this::isTransformMode);
   public final NumberSetting angleX = new NumberSetting("Угол X", 0.0F, -180.0F, 180.0F, 1.0F, this::isTransformMode);
   public final NumberSetting angleY = new NumberSetting("Угол Y", 0.0F, -180.0F, 180.0F, 1.0F, this::isTransformMode);
   public final NumberSetting angleZ = new NumberSetting("Угол Z", 0.0F, -180.0F, 180.0F, 1.0F, this::isTransformMode);
   public final NumberSetting itemScale = new NumberSetting("Масштаб", 1.0F, 0.1F, 3.0F, 0.05F, this::isTransformMode);
   public final ModeSetting curve = new ModeSetting("Кривая", this::isTransformMode, new String[]{"Линейная", "Плавная"});
   public final BooleanSetting mirror = new BooleanSetting("Зеркалить", false);
   public final BooleanSetting onlyFood = new BooleanSetting("Только еда и питьё", true);
   public final BooleanSetting wobble = new BooleanSetting("Покачивание", true, this::isFastMode);
   public final NumberSetting wobbleStrength = new NumberSetting("Сила покачивания", 0.1F, 0.0F, 0.5F, 0.01F, this::isFastMode);

   private UseAnimation() {
   }

   private boolean isTransformMode() {
      return this.mode.is("Наклон") || this.mode.is("Кастом");
   }

   private boolean isFastMode() {
      return this.mode.is("Ускоренная") || this.mode.is("Кастом");
   }

   public boolean shouldApply(ItemStack stack, PlayerEntity player) {
      if (!this.onlyFood.isEnabled()) {
         return true;
      }
      UseAction action = stack.getUseAction();
      return action == UseAction.EAT || action == UseAction.DRINK;
   }

   public void apply(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, PlayerEntity player) {
      float maxUse = (float)stack.getMaxUseTime(player);
      if (maxUse <= 0.0F) {
         return;
      }
      float timeLeft = (float)player.getItemUseTimeLeft() - tickDelta + 1.0F;
      float delayTicks = this.delay.getCurrent();
      if (delayTicks > 0.0F && timeLeft > maxUse - delayTicks) {
         return;
      }
      float speedFactor = Math.max(this.speed.getCurrent(), 0.01F);
      float progress = MathHelper.clamp(timeLeft / speedFactor / maxUse, 0.0F, 1.0F);
      float eat = 1.0F - progress;
      float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
      if (this.mirror.isEnabled()) {
         side = -side;
      }
      float pow = this.power.getCurrent();
      String mode = this.mode.get();
      switch (mode) {
         case "Без тряски":
            this.renderVanilla(matrices, progress, side);
            break;
         case "Ускоренная":
            this.renderFast(matrices, progress, timeLeft, side, pow, true);
            break;
         case "Слайд":
            this.renderSlide(matrices, eat, side, pow);
            break;
         case "Поп":
            this.renderPop(matrices, eat, pow);
            break;
         case "Волна":
            this.renderWave(matrices, eat, side, pow);
            break;
         case "Спин":
            this.renderSpin(matrices, eat, pow);
            break;
         case "Наклон":
            this.renderTilt(matrices, eat, pow);
            break;
         case "Кастом":
            this.renderCustom(matrices, eat, progress, timeLeft, side);
            break;
         default:
            break;
      }
   }

   private void renderVanilla(MatrixStack matrices, float progress, float side) {
      float h = 1.0F - (float)Math.pow(progress, 27.0);
      matrices.translate(h * 0.6F * side, h * -0.5F, 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * h * 90.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * h * 30.0F));
   }

   private void renderFast(MatrixStack matrices, float progress, float timeLeft, float side, float pow, boolean withWobble) {
      if (withWobble && this.wobble.isEnabled() && progress < 0.8F) {
         float wobble = MathHelper.abs(MathHelper.cos(timeLeft / 4.0F * (float)Math.PI) * this.wobbleStrength.getCurrent());
         matrices.translate(0.0F, wobble, 0.0F);
      }
      float f = 1.0F - (float)Math.pow(progress, 27.0);
      matrices.translate(f * 0.6F * side, f * -0.5F, 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * f * 90.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f * 10.0F * pow));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * f * 30.0F * pow));
   }

   private void renderSlide(MatrixStack matrices, float eat, float side, float pow) {
      float e = this.ease(eat);
      matrices.translate(side * (0.35F - e * 0.7F), -(e * 0.5F), -e * 0.25F);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(e * 20.0F * pow));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * e * 15.0F * pow));
   }

   private void renderPop(MatrixStack matrices, float eat, float pow) {
      float e = this.ease(eat);
      float bounce = MathHelper.abs(MathHelper.sin(e * (float)Math.PI));
      matrices.translate(0.0F, -bounce * 0.25F * pow, -bounce * 0.1F * pow);
      float scale = 1.0F + bounce * 0.15F * pow;
      matrices.scale(scale, scale, scale);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-bounce * 25.0F * pow));
   }

   private void renderWave(MatrixStack matrices, float eat, float side, float pow) {
      float e = this.ease(eat);
      float wave = MathHelper.sin(e * (float)Math.PI * 3.0F);
      matrices.translate(side * wave * 0.12F * pow, wave * 0.06F * pow, 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * wave * 14.0F * pow));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * wave * 8.0F * pow));
   }

   private void renderSpin(MatrixStack matrices, float eat, float pow) {
      float e = this.ease(eat);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(e * 360.0F * pow));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(e * 60.0F * pow));
   }

   private void renderTilt(MatrixStack matrices, float eat, float pow) {
      float e = this.ease(eat);
      matrices.translate(this.offsetX.getCurrent() * e, this.offsetY.getCurrent() * e, this.offsetZ.getCurrent() * e);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.angleX.getCurrent() * e * pow));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.angleY.getCurrent() * e * pow));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.angleZ.getCurrent() * e * pow));
   }

   private void renderCustom(MatrixStack matrices, float eat, float progress, float timeLeft, float side) {
      if (this.wobble.isEnabled() && progress < 0.8F) {
         float wobble = MathHelper.abs(MathHelper.cos(timeLeft / 4.0F * (float)Math.PI) * this.wobbleStrength.getCurrent());
         matrices.translate(0.0F, wobble, 0.0F);
      }
      float e = this.ease(eat);
      matrices.translate(this.offsetX.getCurrent() * e, this.offsetY.getCurrent() * e, this.offsetZ.getCurrent() * e);
      float scale = this.itemScale.getCurrent();
      matrices.scale(scale, scale, scale);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(this.angleX.getCurrent() * e));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.angleY.getCurrent() * e));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.angleZ.getCurrent() * e));
   }

   private float ease(float t) {
      if (this.curve.is("Плавная")) {
         return t * t * (3.0F - 2.0F * t);
      }
      return t;
   }
}
