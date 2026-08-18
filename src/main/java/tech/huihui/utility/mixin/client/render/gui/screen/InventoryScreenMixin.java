package tech.huihui.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.Animations;

@Mixin({InventoryScreen.class})
public abstract class InventoryScreenMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void animationsInventoryHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Animations animations = Animations.INSTANCE;
      if (animations.isEnabled() && animations.getAnimate().isEnable("Открытие инвентаря")) {
         float value = animations.getInventoryAnimation().getAnimationValue();
         context.getMatrices().push();
         context.getMatrices().translate((float)context.getScaledWindowWidth() / 2.0F, (float)context.getScaledWindowHeight() / 2.0F, 0.0F);
         context.getMatrices().scale(value, value, 1.0F);
         context.getMatrices().translate((float)(-context.getScaledWindowWidth()) / 2.0F, (float)(-context.getScaledWindowHeight()) / 2.0F, 0.0F);
      }

   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void animationsInventoryTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Animations animations = Animations.INSTANCE;
      if (animations.isEnabled() && animations.getAnimate().isEnable("Открытие инвентаря")) {
         context.getMatrices().pop();
      }

   }
}