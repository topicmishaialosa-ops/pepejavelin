package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "XRay",
   category = Category.RENDER,
   description = "Поиск обломков после взрыва ТНТ"
)
public final class XRay extends Module {
   public static final XRay INSTANCE = new XRay();

   private final Set<BlockPos> set = ConcurrentHashMap.newKeySet();
   private final Set<BlockPos> set2 = ConcurrentHashMap.newKeySet();
   private final List<DelayBlock> list = new ArrayList<>();
   private final int[] nums = new int[]{4, 10, 20, 40};
   private long lng = 0L;

   private XRay() {
   }

   private static class DelayBlock {
      public BlockPos pos;
      public int ticks;

      public DelayBlock(BlockPos pos, int ticks) {
         this.pos = pos;
         this.ticks = ticks;
      }
   }

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      Iterator<DelayBlock> iterator = this.list.iterator();
      while (iterator.hasNext()) {
         DelayBlock delayBlock = iterator.next();
         delayBlock.ticks--;
         if (delayBlock.ticks > 0) {
            continue;
         }
         this.checkBlocks(delayBlock.pos, 28);
         iterator.remove();
      }

      if (System.currentTimeMillis() - this.lng > 50L) {
         for (BlockPos blockPos : this.set) {
            if (this.set2.contains(blockPos)) {
               continue;
            }
            this.set2.add(blockPos);
            this.startStopDestroy(blockPos);
            this.lng = System.currentTimeMillis();
            break;
         }
      }
   }

   private void startStopDestroy(BlockPos blockPos) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
         mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
      }
   }

   private void handleBlockUpdate(BlockPos blockPos, Block block) {
      BlockPos blockPos2 = blockPos.toImmutable();
      if (block == Blocks.ANCIENT_DEBRIS) {
         if (this.isValidDebris(blockPos2) && this.set.add(blockPos2)) {
            MessageUtil.displayInfo("§6[AncientXray] Обломок найден §e" + blockPos2.toShortString());
         }
      } else {
         this.set.remove(blockPos2);
         this.set2.remove(blockPos2);
      }
   }

   private void checkBlocks(BlockPos blockPos, int n) {
      if (mc.world == null) {
         return;
      }
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      for (int i = -n; i <= n; ++i) {
         for (int j = -n; j <= n; ++j) {
            for (int k = -n; k <= n; ++k) {
               mutable.set(blockPos.getX() + i, blockPos.getY() + j, blockPos.getZ() + k);
               if (!this.isValidDebris(mutable)) {
                  continue;
               }
               BlockPos blockPos2 = mutable.toImmutable();
               if (!this.set.add(blockPos2)) {
                  continue;
               }
               MessageUtil.displayInfo("§6[AncientXray] Обнаружен обломок: §e" + blockPos2.toShortString());
            }
         }
      }
   }

   @EventTarget
   public void onRender3D(EventRender3D event) {
      if (mc.world == null || mc.player == null || this.set.isEmpty()) {
         return;
      }

      Iterator<BlockPos> iterator = this.set.iterator();
      while (iterator.hasNext()) {
         BlockPos blockPos = iterator.next();
         if (!mc.world.getBlockState(blockPos).isOf(Blocks.ANCIENT_DEBRIS)) {
            iterator.remove();
            continue;
         }
         Render3DUtil.drawBox(new Box(blockPos), 0x8000FF00, 1.0F, true, false, false);
      }
   }

   @EventTarget
   public void onPacketReceive(EventPacket event) {
      if (mc.world == null || !event.isReceive()) {
         return;
      }
      var packet = event.getPacket();
      if (packet instanceof ExplosionS2CPacket explosionS2CPacket) {
         BlockPos pos = BlockPos.ofFloored(explosionS2CPacket.center());
         for (int n : this.nums) {
            if (pos != null) {
               this.list.add(new DelayBlock(pos.toImmutable(), n));
            }
         }
      } else if (packet instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
         this.handleBlockUpdate(blockUpdateS2CPacket.getPos(), blockUpdateS2CPacket.getState().getBlock());
      } else if (packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
         chunkDeltaUpdateS2CPacket.visitUpdates((blockPos, blockState) -> this.handleBlockUpdate(blockPos, blockState.getBlock()));
      }
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.set.clear();
      this.set2.clear();
      this.list.clear();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.set.clear();
      this.set2.clear();
      this.list.clear();
      this.lng = 0L;
   }

   public java.util.List<BlockPos> getOres() {
      return new ArrayList<>(this.set);
   }

   private boolean checkDebrisNearby(BlockPos blockPos) {
      int n = 0;
      for (int i = -3; i <= 2; ++i) {
         for (int j = -2; j <= 2; ++j) {
            for (int k = -2; k <= 3; ++k) {
               if (mc.world.getBlockState(blockPos.add(i, j, k)).getBlock() != Blocks.ANCIENT_DEBRIS) {
                  continue;
               }
               if (++n <= 6) {
                  continue;
               }
               return true;
            }
         }
      }
      return false;
   }

   private boolean checkAirNearby(BlockPos blockPos) {
      int n = 0;
      for (Direction direction : Direction.values()) {
         Block block = mc.world.getBlockState(blockPos.offset(direction)).getBlock();
         if (block != Blocks.AIR && block != Blocks.LAVA && block != Blocks.CAVE_AIR) {
            continue;
         }
         if (++n < 2) {
            continue;
         }
         return true;
      }
      return false;
   }

   private boolean checkQuartzOrGold(BlockPos blockPos) {
      int n = 0;
      for (int i = -1; i <= 1; ++i) {
         for (int j = -1; j <= 1; ++j) {
            for (int k = -1; k <= 1; ++k) {
               Block block = mc.world.getBlockState(blockPos.add(i, j, k)).getBlock();
               if (block != Blocks.NETHER_QUARTZ_ORE && block != Blocks.NETHER_GOLD_ORE) {
                  continue;
               }
               if (++n < 4) {
                  continue;
               }
               return true;
            }
         }
      }
      return false;
   }

   private boolean checkAirLavaNearbyBig(BlockPos blockPos) {
      int n = 0;
      for (int i = -1; i <= 1; ++i) {
         for (int j = -1; j <= 1; ++j) {
            for (int k = -1; k <= 1; ++k) {
               Block block = mc.world.getBlockState(blockPos.add(i, j, k)).getBlock();
               if (block != Blocks.AIR && block != Blocks.LAVA && block != Blocks.CAVE_AIR) {
                  continue;
               }
               if (++n < 4) {
                  continue;
               }
               return true;
            }
         }
      }
      return n >= 4;
   }

   private boolean isValidDebris(BlockPos blockPos) {
      if (mc.world == null) {
         return false;
      }
      Block block = mc.world.getBlockState(blockPos).getBlock();
      if (block != Blocks.ANCIENT_DEBRIS) {
         return false;
      }
      if (!this.checkAirNearby(blockPos)) {
         return false;
      }
      if (this.checkQuartzOrGold(blockPos)) {
         return false;
      }
      if (!this.checkAirLavaNearbyBig(blockPos)) {
         return false;
      }
      if (this.checkDebrisNearby(blockPos)) {
         return false;
      }
      return true;
   }
}