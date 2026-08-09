package tech.huihui.utility.mixin.client.render;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.base.events.impl.entity.EventEntityColor;
import tech.huihui.client.modules.impl.render.CustomModel;
import tech.huihui.utility.interfaces.IMinecraft;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> implements IMinecraft {
   @Shadow
   @Nullable
   protected abstract RenderLayer method_24302(LivingEntityRenderState var1, boolean var2, boolean var3, boolean var4);

   private static final ThreadLocal<VertexConsumerProvider> RENDER_VERTEX_CONSUMERS = new ThreadLocal();
   private static final ThreadLocal<LivingEntityRenderState> RENDER_STATE = new ThreadLocal();

   @Inject(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At("HEAD")
   )
   private void captureRenderContext(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      RENDER_VERTEX_CONSUMERS.set(vertexConsumers);
      RENDER_STATE.set(state);
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"
)
   )
   private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
      if (!translucent && state.width == 0.6F) {
         EventEntityColor event = new EventEntityColor(-1);
         EventManager.call(event);
         if (event.isCancelled()) {
            translucent = true;
         }
      }

      return this.method_24302(state, showBody, translucent, showOutline);
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
)
   )
   private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l) {
      VertexConsumerProvider vertexConsumers = RENDER_VERTEX_CONSUMERS.get();
      LivingEntityRenderState renderState = RENDER_STATE.get();
      EventEntityColor event = new EventEntityColor(l);
      if (renderState instanceof PlayerEntityRenderState && vertexConsumers != null) {
         if (CustomModel.INSTANCE.render(matrixStack, vertexConsumers, i, j, (PlayerEntityRenderState) renderState)) {
            return;
         }
      } else {
         if (renderState != null && renderState.invisibleToPlayer) {
            EventManager.call(event);
         }
      }

      instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
   }
}
