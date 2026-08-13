package tech.huihui.utility.mixin.client.render;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.huihui.client.modules.impl.render.Optimization;
import tech.huihui.utility.interfaces.IMinecraft;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin implements IMinecraft {
   @Inject(
      method = {"shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cullDistantEntities(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (Optimization.INSTANCE.isEnabled() && mc.player != null) {
         float max = Optimization.INSTANCE.getEntityDistance();
         if (entity.squaredDistanceTo(mc.player) > (double)(max * max)) {
            cir.setReturnValue(false);
         }
      }
   }

   @Shadow
   private static void renderShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, EntityRenderState state, float opacity, float tickDelta, WorldView world, float y) {
      throw new AssertionError();
   }

   @Redirect(
      method = {"render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState;FFLnet/minecraft/world/WorldView;F)V"
),
      require = 0
   )
   private void renderShadowHook(MatrixStack matrices, VertexConsumerProvider vertexConsumers, EntityRenderState state, float opacity, float tickDelta, WorldView world, float y, Entity entity, double x, double y2, double z, float yaw, MatrixStack matrices2, VertexConsumerProvider vertexConsumers2, int light, EntityRenderer entityRenderer) {
      if (!Optimization.INSTANCE.isShadowsEnabled()) {
         renderShadow(matrices, vertexConsumers, state, opacity, tickDelta, world, y);
      }
   }
}
