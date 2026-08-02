package tech.huihui.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.InventoryAnimator;

@Mixin(HandledScreen.class)
public abstract class InventoryAnimatorMixin {
   @Shadow
   protected int x;
   @Shadow
   protected int y;
   @Shadow
   protected int backgroundWidth;
   @Shadow
   protected int backgroundHeight;

   @Inject(
      method = "render",
      at = @At("HEAD")
   )
   private void huihuiPushPanel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!InventoryAnimator.INSTANCE.isEnabled()) {
         return;
      }
      InventoryAnimator.INSTANCE.onSlotDraw((Object)this, System.currentTimeMillis());
      if (!InventoryAnimator.INSTANCE.isPanelEnabled()) {
         return;
      }
      context.getMatrices().push();
      float p = InventoryAnimator.INSTANCE.getPanelProgress(System.currentTimeMillis());
      if (p >= 1.0F) {
         return;
      }
      float eased = 1.0F - (1.0F - p) * (1.0F - p);
      MatrixStack ms = context.getMatrices();
      float cx = (float)this.x + (float)this.backgroundWidth * 0.5F;
      float cy = (float)this.y + (float)this.backgroundHeight * 0.5F;
      if (InventoryAnimator.INSTANCE.isSlide()) {
         ms.translate(0.0F, -(1.0F - eased) * (float)this.backgroundHeight * 0.25F, 0.0F);
      }
      if (InventoryAnimator.INSTANCE.isScale()) {
         float s = 0.7F + 0.3F * eased;
         ms.translate(cx, cy, 0.0F);
         ms.scale(s, s, 1.0F);
         ms.translate(-cx, -cy, 0.0F);
      }
   }

   @Inject(
      method = "render",
      at = @At("RETURN")
   )
   private void huihuiPopPanel(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (!InventoryAnimator.INSTANCE.isEnabled() || !InventoryAnimator.INSTANCE.isPanelEnabled()) {
         return;
      }
      context.getMatrices().pop();
   }

   @Inject(
      method = "drawSlot",
      at = @At("HEAD")
   )
   private void huihuiPushSlot(DrawContext context, Slot slot, CallbackInfo ci) {
      if (!InventoryAnimator.INSTANCE.isEnabled() || !InventoryAnimator.INSTANCE.isSlotsEnabled()) {
         return;
      }
      context.getMatrices().push();
      float p = InventoryAnimator.INSTANCE.getSlotProgress(slot.id, System.currentTimeMillis());
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
      if (!InventoryAnimator.INSTANCE.isEnabled() || !InventoryAnimator.INSTANCE.isSlotsEnabled()) {
         return;
      }
      context.getMatrices().pop();
   }
}
