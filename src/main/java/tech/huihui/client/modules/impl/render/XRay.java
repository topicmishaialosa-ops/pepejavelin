package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "XRay",
   category = Category.RENDER,
   description = "Сканирует чанки и ловит обновления блоков, подсвечивая древние обломки"
)
public final class XRay extends Module {
   private static final int MAX_SECTIONS_PER_FRAME = 48;

   public static final XRay INSTANCE = new XRay();
   private final NumberSetting range = new NumberSetting("Радиус", 24.0F, 4.0F, 64.0F, 1.0F);
   private final NumberSetting scanInterval = new NumberSetting("Интервал сканирования (мс)", 2000.0F, 500.0F, 10000.0F, 100.0F);
   private final CopyOnWriteArrayList<BlockPos> ores = new CopyOnWriteArrayList();
   private final Set<BlockPos> announced = ConcurrentHashMap.newKeySet();
   private final ConcurrentLinkedQueue<BlockPos> pendingAnnounce = new ConcurrentLinkedQueue();
   private final ArrayDeque<ChunkSectionPos> scanQueue = new ArrayDeque();
   private boolean scanDone = true;
   private long nextScanAt;

   private XRay() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.nextScanAt = 0L;
      if (mc.player != null) {
         MessageUtil.displayInfo("Найдено древних обломков: " + this.ores.size());
      }
   }

   private void found(BlockPos pos) {
      if (!this.ores.contains(pos)) {
         this.ores.add(pos);
      }
      if (this.announced.add(pos)) {
         this.pendingAnnounce.add(pos);
      }
   }

   @EventTarget
   private void onPacket(EventPacket event) {
      if (!event.isReceive()) {
         return;
      }
      if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket packet) {
         packet.visitUpdates((blockPos, blockState) -> {
            if (blockState.getBlock() == Blocks.ANCIENT_DEBRIS) {
               this.found(blockPos);
            }
         });
      }
   }

   @EventTarget
   private void onRender3D(EventRender3D event) {
      if (mc.world == null || mc.player == null) {
         return;
      }
      this.tickScan();
      BlockPos announcedPos;
      while ((announcedPos = this.pendingAnnounce.poll()) != null) {
         MessageUtil.displayInfo("Найдено древних обломков: " + this.ores.size());
      }
      this.ores.removeIf(pos -> {
         if (mc.world.getBlockState(pos).getBlock() != Blocks.ANCIENT_DEBRIS) {
            this.announced.remove(pos);
            return true;
         }
         return false;
      });
      this.ores.forEach(pos -> Render3DUtil.drawBox(new Box(pos), 0xFFC3A278, 1.0F, true, false, false));
   }

   private void tickScan() {
      if (this.scanDone) {
         long now = System.currentTimeMillis();
         if (now >= this.nextScanAt) {
            this.rebuildScan();
            this.nextScanAt = now + (long)this.scanInterval.getCurrent();
         }
         return;
      }
      int processed = 0;
      while (!this.scanQueue.isEmpty() && processed < MAX_SECTIONS_PER_FRAME) {
         this.scanSection(this.scanQueue.poll());
         processed++;
      }
      if (this.scanQueue.isEmpty()) {
         this.scanDone = true;
      }
   }

   private void rebuildScan() {
      this.scanQueue.clear();
      this.scanDone = false;
      if (mc.world == null || mc.player == null) {
         this.scanDone = true;
         return;
      }
      int half = (int)Math.ceil((double)this.range.getCurrent() / 16.0D);
      int playerChunkX = mc.player.getChunkPos().x;
      int playerChunkZ = mc.player.getChunkPos().z;
      int minSectionY = mc.world.getBottomY() >> 4;
      int maxSectionY = mc.world.getTopYInclusive() >> 4;
      for (int dx = -half; dx <= half; dx++) {
         for (int dz = -half; dz <= half; dz++) {
            int chunkX = playerChunkX + dx;
            int chunkZ = playerChunkZ + dz;
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
               continue;
            }
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
               this.scanQueue.add(ChunkSectionPos.from(chunkX, sectionY, chunkZ));
            }
         }
      }
      this.scanDone = this.scanQueue.isEmpty();
   }

   private void scanSection(ChunkSectionPos sectionPos) {
      Chunk chunk = mc.world.getChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), ChunkStatus.FULL, false);
      if (chunk == null) {
         return;
      }
      ChunkSection section = chunk.getSection(sectionPos.getSectionY() - (chunk.getBottomY() >> 4));
      if (section == null || section.isEmpty()) {
         return;
      }
      int baseX = sectionPos.getMinX();
      int baseY = sectionPos.getMinY();
      int baseZ = sectionPos.getMinZ();
      for (int x = 0; x < 16; x++) {
         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               BlockState state = section.getBlockState(x, y, z);
               if (state.getBlock() == Blocks.ANCIENT_DEBRIS) {
                  this.found(new BlockPos(baseX + x, baseY + y, baseZ + z));
               }
            }
         }
      }
   }
}
