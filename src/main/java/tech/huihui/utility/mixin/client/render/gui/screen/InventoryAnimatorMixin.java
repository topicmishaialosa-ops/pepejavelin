package tech.huihui.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.InventoryAnimator;

@Mixin(HandledScreen.class)
public abstract class InventoryAnimatorMixin {
   @Inject(
      method = "drawSlot",
      at = @At("HEAD")
   )
   private void huihuiPushSlot(DrawContext context, Slot slot, CallbackInfo ci) {
      if (!InventoryAnimator.INSTANCE.isEnabled()) {
         return;
      }
      long now = System.currentTimeMillis();
      InventoryAnimator.INSTANCE.onSlotDraw((Object)this, now);
      context.getMatrices().push();
      float p = InventoryAnimator.INSTANCE.getProgress(slot.id, now);
      if (p >= 1.0F) {
         return;
      }
      float eased = 1.0F - (1.0F - p) * (1.0F - p);
      MatrixStack ms = context.getMatrices();
      if (InventoryAnimator.INSTANCE.isSlide()) {
         ms.translate(slot.x, slot.y + (1.0F - eased) * 14.0F, 0.0F);
      }
      if (InventoryAnimator.INSTANCE.isScale()) {
         float s = 0.6F + 0.4F * eased;
         ms.translate(slot.x, slot.y, 0.0F);
         ms.scale(s, s, 1.0F);
         ms.translate(-slot.x, -slot.y, 0.0F);
      }
   }

   @Inject(
      method = "drawSlot",
      at = @At("RETURN")
   )
   private void huihuiPopSlot(DrawContext context, Slot slot, CallbackInfo ci) {
      if (!InventoryAnimator.INSTANCE.isEnabled()) {
         return;
      }
      context.getMatrices().pop();
   }
}
