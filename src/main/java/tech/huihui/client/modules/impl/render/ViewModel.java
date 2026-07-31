package tech.huihui.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.joml.Vector4f;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.screens.viewmodel.ViewModelEditScreen;

@ModuleAnnotation(
   name = "ViewModel",
   category = Category.RENDER,
   description = "Настройка позиции"
)
public final class ViewModel extends Module {
   public static final ViewModel INSTANCE = new ViewModel();
   public static float leftHandScreenX;
   public static float leftHandScreenY;
   public static float rightHandScreenX;
   public static float rightHandScreenY;
   public static float leftHandPxPerWorldX;
   public static float leftHandPxPerWorldY;
   public static float rightHandPxPerWorldX;
   public static float rightHandPxPerWorldY;
   public final NumberSetting leftX = new NumberSetting("Левая рука X", 0.0F, -10.0F, 10.0F, 0.01F);
   public final NumberSetting leftY = new NumberSetting("Левая рука Y", 0.0F, -10.0F, 10.0F, 0.01F);
   public final NumberSetting leftZ = new NumberSetting("Левая рука Z", 0.0F, -10.0F, 10.0F, 0.01F);
   public final NumberSetting leftScale = new NumberSetting("Левая рука размер", 1.0F, 0.05F, 3.0F, 0.01F);
   public final NumberSetting rightX = new NumberSetting("Правая рука X", 0.0F, -10.0F, 10.0F, 0.01F);
   public final NumberSetting rightY = new NumberSetting("Правая рука Y", 0.0F, -10.0F, 10.0F, 0.01F);
   public final NumberSetting rightZ = new NumberSetting("Правая рука Z", 0.0F, -10.0F, 10.0F, 0.01F);
   public final NumberSetting rightScale = new NumberSetting("Правая рука размер", 1.0F, 0.05F, 3.0F, 0.01F);
   public final ButtonSetting openEditor = new ButtonSetting("Открыть редактор", ViewModelEditScreen::openEditor);

   private ViewModel() {
   }

   public void applyHandScale(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.scale(this.rightScale.getCurrent(), this.rightScale.getCurrent(), this.rightScale.getCurrent());
         } else {
            matrices.scale(this.leftScale.getCurrent(), this.leftScale.getCurrent(), this.leftScale.getCurrent());
         }
      } else {
         matrices.scale(1.0F, 1.0F, 1.0F);
      }

   }

   public void applyHandPosition(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.translate(this.rightX.getCurrent(), this.rightY.getCurrent(), this.rightZ.getCurrent());
         } else {
            matrices.translate(this.leftX.getCurrent(), this.leftY.getCurrent(), this.leftZ.getCurrent());
         }
      } else {
         matrices.translate(0.0F, 0.0F, 0.0F);
      }

   }

   public static void captureHandScreenPosition(MatrixStack matrices, Arm arm) {
      Vector4f position = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
      matrices.peek().getPositionMatrix().transform(position);
      float w = position.w;
      if (w == 0.0F) {
         return;
      }
      float screenX = (position.x / w * 0.5F + 0.5F) * mw.getScaledWidth();
      float screenY = (0.5F - position.y / w * 0.5F) * mw.getScaledHeight();
      float pxX = mw.getScaledWidth() * 0.5F / w;
      float pxY = mw.getScaledHeight() * 0.5F / w;
      if (arm == Arm.RIGHT) {
         rightHandScreenX = screenX;
         rightHandScreenY = screenY;
         rightHandPxPerWorldX = pxX;
         rightHandPxPerWorldY = pxY;
      } else {
         leftHandScreenX = screenX;
         leftHandScreenY = screenY;
         leftHandPxPerWorldX = pxX;
         leftHandPxPerWorldY = pxY;
      }
   }
}
