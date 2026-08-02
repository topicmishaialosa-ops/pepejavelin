package tech.huihui.utility.mixin.client.render;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tech.huihui.client.modules.impl.render.ChunkAnimator;
import tech.huihui.utility.mixin.accessors.BuiltChunkAccessor;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererChunkAnimatorMixin {
   @Redirect(
      method = "renderLayer(Lnet/minecraft/client/render/RenderLayer;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;getOrigin()Lnet/minecraft/util/math/BlockPos;"
      )
   )
   private BlockPos huihuiAnimateChunk(ChunkBuilder.BuiltChunk chunk) {
      BlockPos real = ((BuiltChunkAccessor)(Object)chunk).invokeGetOrigin();
      float p = ChunkAnimator.INSTANCE.getProgress(chunk);
      if (p >= 1.0F) {
         return real;
      }
      float eased = 1.0F - (1.0F - p) * (1.0F - p);
      int off = (int)((float)ChunkAnimator.INSTANCE.getLiftBlocks() * (1.0F - eased));
      return real.down(off);
   }
}
