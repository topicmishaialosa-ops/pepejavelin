package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.player.rotation.Rotation;

@ModuleAnnotation(
   name = "AutoCrystal",
   category = Category.COMBAT,
   description = "Автоматически ставит и взрывает кристаллы"
)
public class AutoCrystal extends Module {
   public static final AutoCrystal INSTANCE = new AutoCrystal();
   private static final List<Item> VALUABLE_ITEMS = List.of(
      Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
      Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
      Items.NETHERITE_SWORD, Items.DIAMOND_SWORD,
      Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE,
      Items.NETHERITE_SHOVEL, Items.DIAMOND_SHOVEL,
      Items.NETHERITE_AXE, Items.DIAMOND_AXE,
      Items.TOTEM_OF_UNDYING, Items.END_CRYSTAL,
      Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE,
      Items.ENDER_PEARL, Items.TRIDENT, Items.CROSSBOW, Items.ELYTRA
   );
   private final MultiBooleanSetting doNotExplode = new MultiBooleanSetting(
      "Не взрывать",
      MultiBooleanSetting.Value.of("Себя", true),
      MultiBooleanSetting.Value.of("Друзей", false),
      MultiBooleanSetting.Value.of("Предметы", true)
   );
   private final NumberSetting checkRadius = new NumberSetting("Радиус проверки", 6.0F, 1.0F, 20.0F, 0.5F);
   private final BooleanSetting modeOption = new BooleanSetting("Ставить обсидиан и кристалл", false);
   private final KeySetting placeCrystalButton = new KeySetting("Ставить кристалл", -1);
   private final KeySetting obsidianButton = new KeySetting("Обсидиан", -1);
   private final KeySetting crystalButton = new KeySetting("Кристалл", -1);
   private final ModeSetting crystalSourceMode = new ModeSetting(
      "Источник кристалла",
      () -> !this.modeOption.isEnabled(),
      "В руку (хотбар)"
   );
   private final double aimPitchRange = 0.5D;
   private final double attackAimPitchRange = 0.5D;
   private final double crystalBBoxGrow = 0.35D;
   private final long crystalPlaceCooldown = 100L;
   private long lastCrystalPlaceTime;
   private int crystalSlot = -1;
   private int savedHotbarSlot = -1;
   private boolean needRestoreSlot;
   private boolean justPlaced;
   private BlockPos targetBlockPos;
   private Box crystalBoundingBox;

   @EventTarget
   @Native
   private void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }
      if (mc.currentScreen != null) {
         return;
      }
      this.trackCrystalTarget();
      this.tickObsidianFlow();
   }

   @EventTarget
   @Native
   private void onKey(EventKey e) {
      if (this.modeOption.isEnabled()) {
         if (e.isKeyDown(this.crystalButton.getKeyCode())) {
            this.tryAttackCrystal();
            return;
         }
         if (e.isKeyDown(this.obsidianButton.getKeyCode())) {
            this.placeObsidianBlock();
         }
      } else {
         if (this.placeCrystalButton.getKeyCode() != -1 && e.isKeyDown(this.placeCrystalButton.getKeyCode())) {
            this.tryAttackCrystal();
         }
      }
   }

   private void trackCrystalTarget() {
      Item mainHand = mc.player.getMainHandStack().getItem();
      Item offHand = mc.player.getOffHandStack().getItem();
      if (mainHand != Items.END_CRYSTAL && offHand != Items.END_CRYSTAL) {
         return;
      }
      if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
         return;
      }
      if (hit.getType() != HitResult.Type.BLOCK) {
         return;
      }
      BlockPos pos = hit.getBlockPos();
      Block block = mc.world.getBlockState(pos).getBlock();
      if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
         return;
      }
      if (!mc.world.getBlockState(pos.up()).isAir()) {
         return;
      }
      if (!mc.world.getBlockState(pos.up().up()).isAir()) {
         return;
      }
      this.updateCrystalBoundingBox(pos);
   }

   private void tickObsidianFlow() {
      if (this.needRestoreSlot) {
         this.needRestoreSlot = false;
         mc.player.getInventory().selectedSlot = this.savedHotbarSlot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.savedHotbarSlot));
      } else if (this.targetBlockPos != null) {
         if (mc.world.getBlockState(this.targetBlockPos).isAir()) {
            this.targetBlockPos = null;
         } else if (this.justPlaced) {
            this.justPlaced = false;
         } else if (!this.canPlaceCrystalAt(this.targetBlockPos)) {
            this.targetBlockPos = null;
            return;
         } else {
            this.placeCrystalOnObsidian();
         }
      }
      this.attackCrystal();
   }

   private void placeCrystalOnObsidian() {
      Vec3d eye = mc.player.getEyePos();
      Vec3d aimVec = this.clampVecToAABB(eye, new Box(this.targetBlockPos));
      Vec3d diff = aimVec.subtract(eye);
      RotationComponent.update(Rotation.fromRotationVec(diff), 180.0F, 180.0F, 180.0F, 180.0F, 6, 1, false);
      this.savedHotbarSlot = mc.player.getInventory().selectedSlot;
      mc.player.getInventory().selectedSlot = this.crystalSlot;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.crystalSlot));
      Vec3d inverse = diff.negate();
      Direction facing = Direction.getFacing(inverse.x, inverse.y, inverse.z);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(aimVec, facing, this.targetBlockPos, false));
      mc.player.swingHand(Hand.MAIN_HAND);
      this.updateCrystalBoundingBox(this.targetBlockPos);
      this.needRestoreSlot = true;
      this.targetBlockPos = null;
   }

   private void attackCrystal() {
      if (this.crystalBoundingBox == null) {
         return;
      }
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity && this.crystalBoundingBox.contains(entity.getPos())) {
            if (!this.shouldTargetEntity(entity)) {
               this.crystalBoundingBox = null;
               return;
            }
            if (!entity.getBoundingBox().contains(mc.player.getEyePos())) {
               Vec3d aimVec = this.getAimVecToEntity(entity);
               RotationComponent.update(Rotation.fromRotationVec(aimVec), 180.0F, 180.0F, 180.0F, 180.0F, 6, 1, false);
            }
            mc.interactionManager.attackEntity(mc.player, entity);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.crystalBoundingBox = null;
            return;
         }
      }
   }

   private void tryAttackCrystal() {
      long now = System.currentTimeMillis();
      if (now - this.lastCrystalPlaceTime < this.crystalPlaceCooldown) {
         return;
      }
      this.lastCrystalPlaceTime = now;
      if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
         return;
      }
      if (hit.getType() != HitResult.Type.BLOCK) {
         return;
      }
      BlockPos pos = hit.getBlockPos();
      Block block = mc.world.getBlockState(pos).getBlock();
      if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
         return;
      }
      BlockPos up = pos.up();
      if (!mc.world.getBlockState(up).isAir()) {
         return;
      }
      if (!mc.world.getBlockState(up.up()).isAir()) {
         return;
      }
      if (mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(Items.END_CRYSTAL))) {
         return;
      }
      int slot = this.findItem(Items.END_CRYSTAL);
      if (slot == -1) {
         return;
      }
      if (this.modeOption.isEnabled()) {
         this.switchSlotAndRun(slot, () -> this.placeBlockAt(pos, Direction.UP, Hand.MAIN_HAND));
      } else if (this.crystalSourceMode.is("В руку (хотбар)")) {
         if (slot < 9) {
            int current = mc.player.getInventory().selectedSlot;
            if (current == slot) {
               this.placeBlockAt(pos, Direction.UP, Hand.MAIN_HAND);
            } else {
               mc.player.getInventory().selectedSlot = slot;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
               this.placeBlockAt(pos, Direction.UP, Hand.MAIN_HAND);
               mc.player.getInventory().selectedSlot = current;
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(current));
            }
         } else {
            this.switchSlotAndRun(slot, () -> this.placeBlockAt(pos, Direction.UP, Hand.MAIN_HAND));
         }
      } else {
         this.switchSlotAndRun(slot, () -> this.placeBlockAt(pos, Direction.UP, Hand.MAIN_HAND));
      }
      this.updateCrystalBoundingBox(pos);
   }

   private void placeObsidianBlock() {
      if (!(mc.crosshairTarget instanceof BlockHitResult hit)) {
         return;
      }
      if (hit.getType() != HitResult.Type.BLOCK) {
         return;
      }
      BlockPos pos = hit.getBlockPos();
      Direction face = hit.getSide();
      BlockPos offsetPos = pos.offset(face);
      if (!mc.world.getBlockState(offsetPos).isReplaceable()) {
         return;
      }
      int slot = this.findItem(Items.OBSIDIAN);
      if (slot == -1) {
         return;
      }
      this.switchSlotAndRun(slot, () -> {
         this.placeBlockAt(offsetPos, face, Hand.MAIN_HAND);
         int crystalSlot = this.findItem(Items.END_CRYSTAL);
         if (crystalSlot != -1 && crystalSlot < 9) {
            this.targetBlockPos = offsetPos;
            this.crystalSlot = crystalSlot;
            this.justPlaced = true;
         }
      });
   }

   private void placeBlockAt(BlockPos pos, Direction face, Hand hand) {
      Vec3d vec = new Vec3d(pos.getX() + this.aimPitchRange, pos.getY() + 1.0D, pos.getZ() + this.attackAimPitchRange);
      mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(vec, face, pos, false));
      mc.player.swingHand(hand);
   }

   private void switchSlotAndRun(int slot, Runnable runnable) {
      int current = mc.player.getInventory().selectedSlot;
      if (slot < 9) {
         mc.player.getInventory().selectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         runnable.run();
         mc.player.getInventory().selectedSlot = current;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(current));
      } else {
         int windowId = mc.player.currentScreenHandler.syncId;
         mc.interactionManager.clickSlot(windowId, slot, current, SlotActionType.SWAP, mc.player);
         runnable.run();
         mc.interactionManager.clickSlot(windowId, slot, current, SlotActionType.SWAP, mc.player);
      }
   }

   private void updateCrystalBoundingBox(BlockPos pos) {
      if (pos == null || !this.canPlaceCrystalAt(pos)) {
         this.crystalBoundingBox = null;
         return;
      }
      this.crystalBoundingBox = new Box(pos.up()).expand(this.crystalBBoxGrow);
   }

   private boolean canPlaceCrystalAt(BlockPos pos) {
      if (pos == null) {
         return false;
      }
      if (this.doNotExplode.isEnable("Себя") && mc.player.getY() > (double)pos.getY()) {
         return false;
      }
      if (this.doNotExplode.isEnable("Друзей") && this.isFriendNearBlock(pos)) {
         return false;
      }
      if (this.doNotExplode.isEnable("Предметы") && this.itemsNear(pos)) {
         return false;
      }
      return true;
   }

   private boolean isFriendNearBlock(BlockPos pos) {
      if (mc.world == null || mc.player == null) {
         return false;
      }
      Vec3d vec = new Vec3d((double)pos.getX() + 0.5D, (double)pos.getY() + 1.0D, (double)pos.getZ() + 0.5D);
      double radius = this.checkRadius.getCurrent();
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player) {
            continue;
         }
         if (HuihuiClient.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            if (player.getPos().squaredDistanceTo(vec) <= radius * radius) {
               return true;
            }
         }
      }
      return false;
   }

   private boolean itemsNear(BlockPos pos) {
      if (mc.world == null) {
         return false;
      }
      double radius = this.checkRadius.getCurrent();
      Box box = new Box((double)pos.getX() - radius, (double)pos.getY() - radius, (double)pos.getZ() - radius, (double)pos.getX() + radius + 1.0D, (double)pos.getY() + radius + 2.0D, (double)pos.getZ() + radius + 1.0D);
      for (Entity entity : mc.world.getOtherEntities(null, box)) {
         if (entity instanceof ItemEntity) {
            Item item = ((ItemEntity)entity).getStack().getItem();
            if (VALUABLE_ITEMS.contains(item)) {
               return true;
            }
         }
      }
      return false;
   }

   private boolean shouldTargetEntity(Entity entity) {
      if (entity == null) {
         return false;
      }
      BlockPos pos = BlockPos.ofFloored(entity.getPos());
      if (this.doNotExplode.isEnable("Друзей") && this.isFriendNearBlock(pos)) {
         return false;
      }
      if (this.doNotExplode.isEnable("Предметы") && this.itemsNear(pos)) {
         return false;
      }
      return true;
   }

   private Vec3d getAimVecToEntity(Entity entity) {
      Vec3d eye = mc.player.getEyePos();
      return this.clampVecToAABB(eye, entity.getBoundingBox()).subtract(eye);
   }

   private Vec3d clampVecToAABB(Vec3d vec, Box box) {
      return new Vec3d(MathHelper.clamp(vec.x, box.minX, box.maxX), MathHelper.clamp(vec.y, box.minY, box.maxY), MathHelper.clamp(vec.z, box.minZ, box.maxZ));
   }

   private int findItem(Item item) {
      for (int i = 35; i >= 0; --i) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            return i;
         }
      }
      return -1;
   }
}
