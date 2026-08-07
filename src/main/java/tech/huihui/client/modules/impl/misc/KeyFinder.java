package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.utility.game.other.BaritoneUtil;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.math.Timer;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
      name = "KeyFinder",
      category = Category.MISC,
      description = "Ищет сундуки и вагонетки с ключами и показывает их в мире"
)
public final class KeyFinder extends Module {
   public static final KeyFinder INSTANCE = new KeyFinder();

   private static final int SEARCH_RADIUS = 24;
   private static final int SPAWNER_RADIUS = 4;
   private static final long SCAN_DELAY = 1000L;
   private static final long BARITONE_DELAY = 1500L;

   private final BooleanSetting walkWithBaritone = new BooleanSetting(
         "Идти к цели через Baritone",
         "Автоматически отправлять Baritone к ближайшей найденной цели",
         false
   );

   private final List<KeyTarget> targets = new CopyOnWriteArrayList<>();
   private final Set<BlockPos> visitedContainers = new HashSet<>();
   private final Timer scanTimer = new Timer();
   private final Timer baritoneTimer = new Timer();

   private BlockPos currentBaritoneTarget;
   private boolean baritoneMissing;

   private KeyFinder() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.targets.clear();
      this.visitedContainers.clear();
      this.currentBaritoneTarget = null;
      this.baritoneMissing = false;
      this.scanTimer.reset();
      this.baritoneTimer.reset();

      if (this.walkWithBaritone.isEnabled() && !BaritoneUtil.isPresent()) {
         this.baritoneMissing = true;
         MessageUtil.displayInfo("KeyFinder: Baritone не найден, автоматический маршрут недоступен");
      }
   }

    @Override
    public void onDisable() {
       this.stopBaritone();
       if (!this.targets.isEmpty()) {
          MessageUtil.displayInfo(String.format("🎯 KeyFinder выключен: осталось %d целей", this.targets.size()));
       }
       this.targets.clear();
       this.currentBaritoneTarget = null;
       super.onDisable();
    }

   @EventTarget
   private void onTick(EventTick event) {
      if (mc.world == null || mc.player == null) {
         return;
      }

      if (this.scanTimer.finished(SCAN_DELAY)) {
         this.scanWorld();
         this.scanTimer.reset();
      }

      if (this.walkWithBaritone.isEnabled() && !this.baritoneMissing) {
         this.followNearestTarget();
      }
   }

   @EventTarget
   private void onRender3D(EventRender3D event) {
      if (mc.world == null || this.targets.isEmpty()) {
         return;
      }

      for (KeyTarget target : this.targets) {
         int color = switch (target.status) {
            case HAS_KEY -> 0xFF35D07F;
            case UNLOOTED -> 0xFFFFC857;
            case LOOTED -> 0xFF888888;
         };
         Render3DUtil.drawBox(new Box(target.position), color, 1.5F, true, false, false);
      }
   }

   private void scanWorld() {
      BlockPos playerPos = mc.player.getBlockPos();
      int minChunkX = (playerPos.getX() - SEARCH_RADIUS) >> 4;
      int maxChunkX = (playerPos.getX() + SEARCH_RADIUS) >> 4;
      int minChunkZ = (playerPos.getZ() - SEARCH_RADIUS) >> 4;
      int maxChunkZ = (playerPos.getZ() + SEARCH_RADIUS) >> 4;

      List<KeyTarget> found = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();

      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
               continue;
            }

            WorldChunk chunk = mc.world.getChunk(chunkX, chunkZ);
            if (chunk == null) {
               continue;
            }

            for (BlockPos position : chunk.getBlockEntities().keySet()) {
               BlockEntity blockEntity = chunk.getBlockEntity(position);
               if (!(blockEntity instanceof LootableContainerBlockEntity)) {
                  continue;
               }
               if (position.getSquaredDistance(playerPos) > (double) SEARCH_RADIUS * SEARCH_RADIUS) {
                  continue;
               }

                BlockPos immutable = position.toImmutable();
                if (seen.add(immutable)) {
                   LootState state = this.hasKey(blockEntity) ? LootState.HAS_KEY : LootState.UNLOOTED;
                   KeyTarget target = new KeyTarget(immutable, state);
                   found.add(target);
                   if (state == LootState.HAS_KEY) {
                      MessageUtil.displayInfo(String.format("🔑 Ключ найден в сундуке в (%d, %d, %d)!", 
                            immutable.getX(), immutable.getY(), immutable.getZ()));
                   }
                }
             }
          }
       }

       for (Entity entity : mc.world.getEntities()) {
          if (!(entity instanceof ChestMinecartEntity minecart)) {
             continue;
          }
          if (minecart.squaredDistanceTo(mc.player) > (double) SEARCH_RADIUS * SEARCH_RADIUS) {
             continue;
          }

          BlockPos position = minecart.getBlockPos().toImmutable();
          if (seen.add(position)) {
             LootState state = this.hasKey(minecart) ? LootState.HAS_KEY : LootState.UNLOOTED;
             KeyTarget target = new KeyTarget(position, state);
             found.add(target);
             if (state == LootState.HAS_KEY) {
                MessageUtil.displayInfo(String.format("🔑 Ключ найден в вагонетке в (%d, %d, %d)!", 
                      position.getX(), position.getY(), position.getZ()));
             }
          }
       }

       for (BlockPos spawner : this.findSpawners(playerPos)) {
          for (BlockPos chest : this.findNearbyContainers(spawner)) {
             if (seen.add(chest)) {
                BlockEntity blockEntity = mc.world.getBlockEntity(chest);
                LootState state = this.hasKey(blockEntity) ? LootState.HAS_KEY : LootState.UNLOOTED;
                KeyTarget target = new KeyTarget(chest, state);
                found.add(target);
                if (state == LootState.HAS_KEY) {
                   MessageUtil.displayInfo(String.format("🔑 Ключ найден в сундуке рядом со спавнером в (%d, %d, %d)!", 
                         chest.getX(), chest.getY(), chest.getZ()));
                }
             }
          }
       }

       this.targets.clear();
       this.targets.addAll(found);
       
       if (!found.isEmpty()) {
          MessageUtil.displayInfo(String.format("🔍 KeyFinder: Найдено %d цели(й) в радиусе %d блоков", found.size(), SEARCH_RADIUS));
       }
    }

   private List<BlockPos> findSpawners(BlockPos center) {
      List<BlockPos> result = new ArrayList<>();
      for (BlockPos position : BlockPos.iterateOutwards(center, SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)) {
         if (mc.world.getBlockState(position).isOf(Blocks.SPAWNER)) {
            result.add(position.toImmutable());
         }
      }
      return result;
   }

   private List<BlockPos> findNearbyContainers(BlockPos spawner) {
      List<BlockPos> result = new ArrayList<>();
      for (BlockPos position : BlockPos.iterateOutwards(spawner, SPAWNER_RADIUS, SPAWNER_RADIUS, SPAWNER_RADIUS)) {
         if (mc.world.getBlockEntity(position) instanceof LootableContainerBlockEntity) {
            result.add(position.toImmutable());
         }
      }
      return result;
   }

   private boolean hasKey(BlockEntity blockEntity) {
      if (!(blockEntity instanceof LootableContainerBlockEntity inventory)) {
         return false;
      }
      for (int slot = 0; slot < inventory.size(); slot++) {
         if (this.isKey(inventory.getStack(slot))) {
            return true;
         }
      }
      return false;
   }

   private boolean hasKey(ChestMinecartEntity inventory) {
      for (int slot = 0; slot < inventory.size(); slot++) {
         if (this.isKey(inventory.getStack(slot))) {
            return true;
         }
      }
      return false;
   }

   private boolean isKey(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() == Items.TRIPWIRE_HOOK;
   }

   private void followNearestTarget() {
      if (mc.player == null || this.targets.isEmpty()) {
         return;
      }

      if (!BaritoneUtil.isPresent()) {
         this.baritoneMissing = true;
         return;
      }

      KeyTarget nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (KeyTarget target : this.targets) {
         if (target.status == LootState.LOOTED) {
            continue;
         }
         double distance = mc.player.getPos().squaredDistanceTo(target.position.toCenterPos());
         if (distance < nearestDistance) {
            nearestDistance = distance;
            nearest = target;
         }
      }

      if (nearest == null || nearest.position.equals(this.currentBaritoneTarget)
            || !this.baritoneTimer.finished(BARITONE_DELAY)) {
         return;
      }

       this.currentBaritoneTarget = nearest.position;
       this.baritoneTimer.reset();
       String targetType = nearest.status == LootState.HAS_KEY ? "🔑 с ключом" : "📦 без ключа";
       String coords = String.format("X:%d Y:%d Z:%d", nearest.position.getX(), nearest.position.getY(), nearest.position.getZ());
       MessageUtil.displayInfo(String.format("🧭 KeyFinder: Иду к цели через Baritone: %s в %s", targetType, coords));
       mc.player.networkHandler.sendChatMessage("#goto "
             + nearest.position.getX() + " "
             + nearest.position.getY() + " "
             + nearest.position.getZ());
    }

   private void stopBaritone() {
      if (mc.player != null && mc.player.networkHandler != null && this.currentBaritoneTarget != null) {
         mc.player.networkHandler.sendChatMessage("#stop");
      }
   }

   private enum LootState {
      UNLOOTED,
      HAS_KEY,
      LOOTED
   }

   private static final class KeyTarget {
      private final BlockPos position;
      private final LootState status;

      private KeyTarget(BlockPos position, LootState status) {
         this.position = position;
         this.status = status;
      }
   }
}
