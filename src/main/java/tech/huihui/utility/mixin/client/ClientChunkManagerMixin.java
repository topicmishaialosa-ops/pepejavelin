package tech.huihui.utility.mixin.client;

import net.minecraft.client.world.ClientChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.ChunkAnimator;

@Mixin(ClientChunkManager.class)
public abstract class ClientChunkManagerMixin {
   @Inject(
      method = "onSectionStatusChanged(IIIZ)V",
      at = @At("HEAD")
   )
   private void huihuiOnSectionStatusChanged(int sectionX, int sectionY, int sectionZ, boolean empty, CallbackInfo ci) {
      ChunkAnimator.INSTANCE.onSectionChanged(sectionX, sectionY, sectionZ, empty);
   }
}