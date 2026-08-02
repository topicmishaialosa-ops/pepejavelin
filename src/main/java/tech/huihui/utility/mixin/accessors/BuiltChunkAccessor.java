package tech.huihui.utility.mixin.accessors;

import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkBuilder.BuiltChunk.class)
public interface BuiltChunkAccessor {
   @Invoker("getOrigin")
   BlockPos invokeGetOrigin();
}
