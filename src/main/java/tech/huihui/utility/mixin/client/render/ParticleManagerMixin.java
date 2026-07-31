package tech.huihui.utility.mixin.client.render;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.Optimization;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
   @Inject(
      method = {"renderParticles(Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/VertexConsumerProvider$Immediate;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void skipParticles(Camera camera, float tickDelta, VertexConsumerProvider.Immediate vertexConsumers, CallbackInfo ci) {
      if (Optimization.INSTANCE.isParticlesEnabled()) {
         ci.cancel();
      }
   }
}
