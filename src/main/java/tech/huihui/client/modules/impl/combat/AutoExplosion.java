package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "AutoExplosion",
   category = Category.COMBAT,
   description = "Если игрок поставил обсидиан — ставит кристалл из хотбара и взрывает его"
)
public final class AutoExplosion extends Module {
   public static final AutoExplosion INSTANCE = new AutoExplosion();
   private static final float PLACE_REACH = 4.5F;
   private final NumberSetting range = new NumberSetting("Дистанция", 5.0F, 1.0F, 8.0F, 0.5F);
   private final NumberSetting explodeDelay = new NumberSetting("Задержка взрыва", 10.0F, 1.0F, 40.0F, 1.0F);
   private final NumberSetting reactionTime = new NumberSetting("Время реакции", 60.0F, 10.0F, 200.0F, 5.0F);
   private final NumberSetting breakRetries = new NumberSetting("Попытки взрыва", 5.0F, 1.0F, 20.0F, 1.0F);
   private final BooleanSetting render = new BooleanSetting("Визуализация", true);
   private final Map<BlockPos, Block> snapshot = new HashMap<>();
   private final Map<BlockPos, Long> candidates = new HashMap<>();
   private final Map<BlockPos, Integer> explodeCountdown = new HashMap<>();
   private final Map<BlockPos, Integer> retries = new HashMap<>();
   private BlockPos lastScanPos;
   private int scanCooldown;

   @Override
   public void onEnable() {
      super.onEnable();
      this.snapshot.clear();
      this.candidates.clear();
      this.explodeCountdown.clear();
      this.retries.clear();
      this.lastScanPos = null;
      this.scanCooldown = 0;
      this.scan();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.snapshot.clear();
      this.candidates.clear();
      this.explodeCountdown.clear();
      this.retries.clear();
   }

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }

      if (this.shouldScan()) {
         this.scan();
      }

      this.processCandidates();
      this.processExplosions();
   }

   private boolean shouldScan() {
      BlockPos current = mc.player.getBlockPos();
      if (this.lastScanPos == null || !this.lastScanPos.equals(current)) {
         this.lastScanPos = current;
         this.scanCooldown = 0;
         return true;
      }

      if (this.scanCooldown-- <= 0) {
         this.scanCooldown = 10;
         return true;
      }

      return false;
   }

   private void scan() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      int radius = Math.max(1, (int) Math.ceil(this.range.getCurrent()));
      BlockPos player = mc.player.getBlockPos();
      Map<BlockPos, Block> current = new HashMap<>();
      for (int x = player.getX() - radius; x <= player.getX() + radius; ++x) {
         for (int y = player.getY() - 2; y <= player.getY() + 2; ++y) {
            for (int z = player.getZ() - radius; z <= player.getZ() + radius; ++z) {
               BlockPos pos = new BlockPos(x, y, z);
               Block block = mc.world.getBlockState(pos).getBlock();
               current.put(pos, block);
               if (block == Blocks.OBSIDIAN && this.snapshot.get(pos) != Blocks.OBSIDIAN) {
                  this.candidates.put(pos.toImmutable(), mc.world.getTime());
               }
            }
         }
      }

      this.snapshot.clear();
      this.snapshot.putAll(current);
   }

   private void processCandidates() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      long now = mc.world.getTime();
      Vec3d eye = mc.player.getEyePos();
      float maxDist = this.range.getCurrent();
      long maxAge = (long) this.reactionTime.getCurrent();
      for (BlockPos pos : new HashSet<>(this.candidates.keySet())) {
         if (this.explodeCountdown.containsKey(pos) || this.retries.containsKey(pos)) {
            this.candidates.remove(pos);
            continue;
         }

         if (now - this.candidates.get(pos) > maxAge) {
            this.candidates.remove(pos);
            continue;
         }

         double distance = eye.distanceTo(Vec3d.ofCenter(pos));
         if (distance > (double) Math.max(maxDist, AutoExplosion.PLACE_REACH)) {
            this.candidates.remove(pos);
            continue;
         }

         BlockPos above = pos.up();
         if (!mc.world.getBlockState(above).isAir()) {
            this.candidates.remove(pos);
            continue;
         }

         int crystalSlot = PlayerInventoryUtil.find(Items.END_CRYSTAL, 0, 8);
         if (crystalSlot == -1) {
            continue;
         }

         Vec3d placePos = new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
         if (eye.distanceTo(placePos) > (double) AutoExplosion.PLACE_REACH) {
            continue;
         }

         this.placeCrystal(pos, above, placePos, crystalSlot);
      }
   }

   private void placeCrystal(BlockPos pos, BlockPos above, Vec3d placePos, int crystalSlot) {
      RotationComponent.update(Rotation.lookingAt(placePos, mc.player.getEyePos()), 360.0F, 360.0F, 360.0F, 0, 2);
      int prevSlot = mc.player.getInventory().selectedSlot;
      if (crystalSlot != prevSlot) {
         mc.player.getInventory().selectedSlot = crystalSlot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(crystalSlot));
      }

      BlockHitResult hit = new BlockHitResult(placePos, Direction.UP, pos, false);
      boolean accepted = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit).isAccepted();
      if (prevSlot != crystalSlot && mc.player.getInventory().selectedSlot == crystalSlot) {
         mc.player.getInventory().selectedSlot = prevSlot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
      }

      if (accepted) {
         this.explodeCountdown.put(pos.toImmutable(), (int) this.explodeDelay.getCurrent());
         this.candidates.remove(pos);
      }
   }

   private void processExplosions() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }

      for (BlockPos pos : new HashSet<>(this.explodeCountdown.keySet())) {
         int ticks = this.explodeCountdown.get(pos) - 1;
         if (ticks > 0) {
            this.explodeCountdown.put(pos, ticks);
            continue;
         }

         this.explodeCountdown.remove(pos);
         EndCrystalEntity crystal = this.findCrystal(pos);
         if (crystal == null) {
            int attempts = this.retries.getOrDefault(pos, 0) + 1;
            if (attempts < (int) this.breakRetries.getCurrent()) {
               this.retries.put(pos, attempts);
               this.explodeCountdown.put(pos, 1);
            } else {
               this.retries.remove(pos);
            }
            continue;
         }

         mc.interactionManager.attackEntity(mc.player, crystal);
         mc.player.swingHand(Hand.MAIN_HAND);
         this.retries.remove(pos);
      }
   }

   private EndCrystalEntity findCrystal(BlockPos pos) {
      Box box = new Box(pos.up()).expand(0.5D);
      return mc.world.getEntitiesByClass(EndCrystalEntity.class, box, (crystal) -> crystal.isAlive() && !crystal.isRemoved()).stream().findFirst().orElse(null);
   }

   @EventTarget
   private void onRenderWorld(EventRender3D event) {
      if (!this.render.isEnabled() || mc.player == null || mc.world == null) {
         return;
      }

      int cyan = 0xFF00E5FF;
      int fill = 0x5500E5FF;
      long now = mc.world.getTime();
      long maxAge = (long) this.reactionTime.getCurrent();
      for (Map.Entry<BlockPos, Long> entry : this.candidates.entrySet()) {
         if (now - entry.getValue() > maxAge) {
            continue;
         }

         BlockPos pos = entry.getKey();
         Vec3d p = new Vec3d(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
         Vec3d c1 = new Vec3d(p.x - 0.5D, p.y, p.z - 0.5D);
         Vec3d c2 = new Vec3d(p.x + 0.5D, p.y, p.z - 0.5D);
         Vec3d c3 = new Vec3d(p.x + 0.5D, p.y, p.z + 0.5D);
         Vec3d c4 = new Vec3d(p.x - 0.5D, p.y, p.z + 0.5D);
         Render3DUtil.drawQuad(c1, c2, c3, c4, fill, false);
         Render3DUtil.drawLine(c1.x, p.y, c1.z, c2.x, p.y, c2.z, cyan, 1.5F, false);
         Render3DUtil.drawLine(c2.x, p.y, c2.z, c3.x, p.y, c3.z, cyan, 1.5F, false);
         Render3DUtil.drawLine(c3.x, p.y, c3.z, c4.x, p.y, c4.z, cyan, 1.5F, false);
         Render3DUtil.drawLine(c4.x, p.y, c4.z, c1.x, p.y, c1.z, cyan, 1.5F, false);
      }
   }
}
