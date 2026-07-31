package tech.huihui.utility.mixin.client.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.Optimization;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
   @Inject(
      method = {"addWeatherParticlesAndSound(Lnet/minecraft/client/render/Camera;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void skipWeather(Camera camera, CallbackInfo ci) {
      if (Optimization.INSTANCE.isWeatherEnabled()) {
         ci.cancel();
      }
   }
}
