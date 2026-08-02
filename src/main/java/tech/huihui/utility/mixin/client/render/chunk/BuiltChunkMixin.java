package tech.huihui.utility.mixin.client.render.chunk;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.client.modules.impl.render.ChunkAnimator;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class BuiltChunkMixin {
   @Inject(
      method = "setSectionPos",
      at = @At("HEAD")
   )
   private void huihuiOnSetSectionPos(long sectionPos, CallbackInfo ci) {
      ChunkAnimator.INSTANCE.onSectionPosSet(sectionPos, (Object)this);
   }

   @Inject(
      method = "delete",
      at = @At("HEAD")
   )
   private void huihuiOnDelete(CallbackInfo ci) {
      ChunkAnimator.INSTANCE.onChunkDelete((Object)this);
   }
}
