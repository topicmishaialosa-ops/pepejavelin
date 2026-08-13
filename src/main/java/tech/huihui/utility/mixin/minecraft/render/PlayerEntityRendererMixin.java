package tech.huihui.utility.mixin.minecraft.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.EntityESP;
import tech.huihui.client.modules.impl.render.GlowHands;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@Mixin({PlayerEntityRenderer.class})
public abstract class PlayerEntityRendererMixin {
   @Inject(
      method = {"renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void render(PlayerEntityRenderState playerEntityRenderState, Text text, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
      if (EntityESP.INSTANCE.isEnabled()) {
         ci.cancel();
      }

   }

   @Redirect(
      method = {"renderArm"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/model/ModelPart;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V"
),
      require = 0
   )
   private void glowHandRender(ModelPart part, MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, MatrixStack matrices2, VertexConsumerProvider vertexConsumers, int light2, Identifier skin, ModelPart part2, boolean sleeveVisible) {
      GlowHands glowHands = GlowHands.INSTANCE;
      if (!glowHands.isEnabled()) {
         part.render(matrices, vertexConsumer, light, overlay);
         return;
      }

      if (glowHands.isTransparencyMode()) {
         int alpha = (int)glowHands.getTransparency();
         VertexConsumer baseBuffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(skin));
         part.render(matrices, baseBuffer, light, overlay, new ColorRGBA(255, 255, 255, alpha).getRGB());
      }

      if (glowHands.isGlowMode() && glowHands.getGlowIntensity() > 0.0F) {
         ColorRGBA color = glowHands.getCurrentColor();
         int glowAlpha = (int)glowHands.getGlowIntensity();
         float size = glowHands.getGlowSize();
         if (size > 1.001F) {
            matrices.push();
            matrices.scale(size, size, size);
         }

         part.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(skin)), light, overlay, new ColorRGBA(color.getRed(), color.getGreen(), color.getBlue(), glowAlpha).getRGB());
         if (size > 1.001F) {
            matrices.pop();
         }
      }
   }
}
