package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BlockMapSetting;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.screens.block.BlockPickerScreen;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "BlockESP",
   category = Category.RENDER,
   description = "Подсвечивает выбранные блоки"
)
public final class BlockESP extends Module {
   private static final int MAX_SECTIONS_PER_FRAME = 48;
   private static final long REBUILD_INTERVAL = 1000L;

   public static final BlockESP INSTANCE = new BlockESP();
   private final BooleanSetting radiusEnabled = new BooleanSetting("Радиус", true);
   private final NumberSetting range = new NumberSetting("Радиус", 16.0F, 4.0F, 200.0F, 1.0F, this.radiusEnabled::isEnabled);
   private final BlockMapSetting blocks = new BlockMapSetting("Выбранные блоки");
   private final ButtonSetting openPicker = new ButtonSetting("Выбрать блок", BlockPickerScreen::open);
   private final NumberSetting lineWidth = new NumberSetting("Толщина линии", 1.5F, 0.5F, 5.0F, 0.5F);
   private final BooleanSetting fill = new BooleanSetting("Заливка", true);

   private final Map<BlockPos, Integer> found = new HashMap();
   private final ArrayDeque<ChunkSectionPos> scanQueue = new ArrayDeque();
   private boolean scanDone = true;
   private boolean dirty = true;
   private long nextScanAt;

   public BlockMapSetting getBlocks() {
      return this.blocks;
   }

   public void markDirty() {
      this.dirty = true;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.dirty = true;
   }

   @EventTarget
   private void onRenderWorld(EventRender3D event) {
      if (mc.world == null || mc.player == null) {
         return;
      }
      if (this.blocks.isEmpty()) {
         this.found.clear();
         this.scanQueue.clear();
         this.scanDone = true;
         return;
      }
      this.tickScan();
      if (this.found.isEmpty()) {
         return;
      }
      float width = this.lineWidth.getCurrent();
      boolean doFill = this.fill.isEnabled();
      for (Map.Entry<BlockPos, Integer> entry : this.found.entrySet()) {
         BlockPos pos = entry.getKey();
         BlockState state = mc.world.getBlockState(pos);
         VoxelShape shape = state.getCollisionShape(mc.world, pos);
         if (shape.isEmpty()) {
            shape = state.getOutlineShape(mc.world, pos);
         }
         Box box = shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos);
         Render3DUtil.drawBox(box, entry.getValue(), width, true, doFill, false);
      }
   }

   private void tickScan() {
      if (this.dirty) {
         this.rebuildScan();
         return;
      }
      long now = System.currentTimeMillis();
      if (this.scanDone) {
         if (now >= this.nextScanAt) {
            this.rebuildScan();
            this.nextScanAt = now + REBUILD_INTERVAL;
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
         this.nextScanAt = System.currentTimeMillis() + REBUILD_INTERVAL;
      }
   }

   private void rebuildScan() {
      this.found.clear();
      this.scanQueue.clear();
      this.dirty = false;
      if (mc.world == null || mc.player == null) {
         this.scanDone = true;
         return;
      }
      boolean useRadius = this.radiusEnabled.isEnabled();
      int half = useRadius ? (int) Math.ceil((double) this.range.getCurrent() / 16.0D) : mc.options.getViewDistance().getValue();
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
               if (state.isAir()) {
                  continue;
               }
               Integer color = this.blocks.getColor(BlockMapSetting.getId(state.getBlock()));
               if (color != null) {
                  this.found.put(new BlockPos(baseX + x, baseY + y, baseZ + z), color);
               }
            }
         }
      }
   }
}
