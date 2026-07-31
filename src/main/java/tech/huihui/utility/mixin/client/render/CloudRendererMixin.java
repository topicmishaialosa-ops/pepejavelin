package tech.huihui.utility.mixin.client.render;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.CloudRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.Optimization;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
   @Inject(
      method = {"renderClouds(ILnet/minecraft/client/option/CloudRenderMode;FLorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/util/math/Vec3d;F)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void skipClouds(int ticks, CloudRenderMode mode, float tickDelta, Matrix4f matrix4f, Matrix4f matrix4f2, Vec3d pos, float f, CallbackInfo ci) {
      if (Optimization.INSTANCE.isCloudsEnabled()) {
         ci.cancel();
      }
   }
}
