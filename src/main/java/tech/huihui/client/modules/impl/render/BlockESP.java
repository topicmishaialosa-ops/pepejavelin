package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.shape.VoxelShape;
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
   private static final int MINECART_COLOR = 0xFF9E9E9E;
   private static final int CHEST_MINECART_COLOR = 0xFFD9A800;

   public static final BlockESP INSTANCE = new BlockESP();
   private final BooleanSetting radiusEnabled = new BooleanSetting("Радиус", true);
   private final NumberSetting range = new NumberSetting("Радиус", 16.0F, 4.0F, 200.0F, 1.0F, this.radiusEnabled::isEnabled);
   private final BlockMapSetting blocks = new BlockMapSetting("Выбранные блоки");
   private final ButtonSetting openPicker = new ButtonSetting("Выбрать блок", BlockPickerScreen::open);
   private final NumberSetting lineWidth = new NumberSetting("Толщина линии", 1.5F, 0.5F, 5.0F, 0.5F);
   private final BooleanSetting fill = new BooleanSetting("Заливка", true);
   private final BooleanSetting minecarts = new BooleanSetting("Вагонетки", false);
   private final BooleanSetting chestMinecarts = new BooleanSetting("Вагонетки с сундуком", false);

   private final Long2ObjectOpenHashMap<FoundBlock> found = new Long2ObjectOpenHashMap();
   private final ArrayDeque<ChunkSectionPos> scanQueue = new ArrayDeque();
   private final Map<Block, Integer> colorCache = new HashMap();
   private final BlockPos.Mutable scanPos = new BlockPos.Mutable();
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
      this.renderMinecarts();
      if (this.blocks.isEmpty()) {
         this.found.clear();
         this.scanQueue.clear();
         this.colorCache.clear();
         this.scanDone = true;
         return;
      }
      this.tickScan();
      if (this.found.isEmpty()) {
         return;
      }
      float width = this.lineWidth.getCurrent();
      boolean doFill = this.fill.isEnabled();
      for (Long2ObjectMap.Entry<FoundBlock> entry : this.found.long2ObjectEntrySet()) {
         FoundBlock foundBlock = entry.getValue();
         Render3DUtil.drawBox(foundBlock.box, foundBlock.color, width, true, doFill, false);
      }
   }

   private void renderMinecarts() {
      boolean showMinecarts = this.minecarts.isEnabled();
      boolean showChestMinecarts = this.chestMinecarts.isEnabled();
      if (!showMinecarts && !showChestMinecarts) {
         return;
      }
      float width = this.lineWidth.getCurrent();
      boolean doFill = this.fill.isEnabled();
      double radiusSq = this.radiusEnabled.isEnabled() ? (double)this.range.getCurrent() * (double)this.range.getCurrent() : Double.MAX_VALUE;
      for (Entity entity : mc.world.getEntities()) {
         if (entity.isRemoved()) {
            continue;
         }
         int color;
         if (entity instanceof ChestMinecartEntity) {
            if (!showChestMinecarts) {
               continue;
            }
            color = CHEST_MINECART_COLOR;
         } else if (entity instanceof MinecartEntity) {
            if (!showMinecarts) {
               continue;
            }
            color = MINECART_COLOR;
         } else {
            continue;
         }
         if (entity.squaredDistanceTo(mc.player) > radiusSq) {
            continue;
         }
         Render3DUtil.drawBox(entity.getBoundingBox(), color, width, true, doFill, false);
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
      this.colorCache.clear();
      this.dirty = false;
      if (mc.world == null || mc.player == null) {
         this.scanDone = true;
         return;
      }
      for (Map.Entry<String, Integer> entry : this.blocks.getBlocks().entrySet()) {
         Identifier id = Identifier.tryParse(entry.getKey());
         if (id == null) {
            continue;
         }
         Block block = Registries.BLOCK.get(id);
         if (block != null && block != Blocks.AIR) {
            this.colorCache.put(block, entry.getValue());
         }
      }
      if (this.colorCache.isEmpty()) {
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
               Integer color = this.colorCache.get(state.getBlock());
               if (color == null) {
                  continue;
               }
               this.scanPos.set(baseX + x, baseY + y, baseZ + z);
               VoxelShape shape = state.getCollisionShape(mc.world, this.scanPos);
               if (shape.isEmpty()) {
                  shape = state.getOutlineShape(mc.world, this.scanPos);
               }
               Box box = shape.isEmpty() ? new Box(this.scanPos) : shape.getBoundingBox().offset(this.scanPos);
               this.found.put(this.scanPos.asLong(), new FoundBlock(box, color));
            }
         }
      }
   }

   private record FoundBlock(Box box, int color) {
   }
}