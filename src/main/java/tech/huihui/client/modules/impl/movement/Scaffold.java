package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Iterator;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "Scaffold",
   category = Category.MOVEMENT,
   description = "Ставит блоки под игроком (обход под ваниллу)"
)
public final class Scaffold extends Module {
   public static final Scaffold INSTANCE = new Scaffold();
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Вперёд", "Башня"});
   private final ModeSetting method = new ModeSetting("Способ", new String[]{"Ванилла", "Пакетный"});
   private final BooleanSetting autoSwap = new BooleanSetting("Автосвап", "Искать блок в инвентаре", true);
   private final NumberSetting delay = new NumberSetting("Задержка (мс)", 0.0F, 0.0F, 500.0F, 10.0F, "Задержка между постановками");
   private final Timer timer = new Timer();
   private int sequence;
   private int lastSyncedSlot = -1;

   @EventTarget
   private void onUpdate(EventUpdate ignored) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (!this.timer.finished((long)this.delay.getCurrent())) {
         return;
      }

      BlockPos target = this.findTarget();
      if (target == null) {
         return;
      }

      BlockPos support = this.findSupport(target);
      if (support == null) {
         return;
      }

      if (!this.switchToBlock()) {
         return;
      }

      Direction side = Direction.fromVector(target.subtract(support), Direction.UP);
      Vec3d hitVec = Vec3d.ofCenter(support).add((double)side.getOffsetX() * 0.5D, (double)side.getOffsetY() * 0.5D, (double)side.getOffsetZ() * 0.5D);
      BlockHitResult hitResult = new BlockHitResult(hitVec, side, support, false);

      this.rotate(hitVec);

      if (this.method.is("Пакетный")) {
         this.syncSelectedSlot();
         mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, ++this.sequence));
         mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
      } else {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
      }

      this.timer.reset();
   }

   private BlockPos findTarget() {
      BlockPos feet = mc.player.getBlockPos();
      if (this.mode.is("Башня")) {
         if (this.isPlaceable(feet)) {
            return feet;
         }

         BlockPos below = feet.down();
         return this.isPlaceable(below) ? below : null;
      }

      Direction facing = mc.player.getHorizontalFacing();
      BlockPos forward = feet.offset(facing);
      if (this.isPlaceable(forward)) {
         return forward;
      }

      BlockPos forwardBelow = forward.down();
      return this.isPlaceable(forwardBelow) ? forwardBelow : null;
   }

   private BlockPos findSupport(BlockPos target) {
      if (this.isSolid(target.down())) {
         return target.down();
      }

      Iterator<Direction> var2 = Direction.Type.HORIZONTAL.iterator();

      Direction direction;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         direction = (Direction)var2.next();
      } while(!this.isSolid(target.offset(direction)));

      return target.offset(direction);
   }

   private boolean switchToBlock() {
      ItemStack mainHand = mc.player.getMainHandStack();
      if (mainHand.getItem() instanceof BlockItem) {
         return true;
      }

      int hotbarSlot = PlayerInventoryUtil.getHotbarSlotId((slot) -> {
         return mc.player.getInventory().getStack(slot).getItem() instanceof BlockItem;
      });
      if (hotbarSlot != -1) {
         mc.player.getInventory().selectedSlot = hotbarSlot;
         return true;
      }

      if (!this.autoSwap.isEnabled() || mc.player.currentScreenHandler.syncId != 0) {
         return false;
      }

      Slot blockSlot = PlayerInventoryUtil.getSlot((slot) -> {
         return slot.getStack().getItem() instanceof BlockItem && slot.inventory == mc.player.getInventory();
      });
      if (blockSlot == null) {
         return false;
      }

      PlayerInventoryUtil.swapHand(blockSlot, mc.player.getInventory().selectedSlot);
      return true;
   }

   private void rotate(Vec3d hitVec) {
      double deltaX = hitVec.x - mc.player.getX();
      double deltaY = hitVec.y - mc.player.getEyeY();
      double deltaZ = hitVec.z - mc.player.getZ();
      double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
      float yaw = (float)(Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0D);
      float pitch = (float)(-Math.toDegrees(Math.atan2(deltaY, distance)));
      mc.player.setYaw(yaw);
      mc.player.setPitch(pitch);
      mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
   }

   private void syncSelectedSlot() {
      int slot = mc.player.getInventory().selectedSlot;
      if (slot != this.lastSyncedSlot) {
         this.lastSyncedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private boolean isPlaceable(BlockPos pos) {
      if (mc.world == null) {
         return false;
      }

      return mc.world.getBlockState(pos).isReplaceable() || mc.world.getBlockState(pos).isAir();
   }

   private boolean isSolid(BlockPos pos) {
      if (mc.world == null) {
         return false;
      }

      return !mc.world.getBlockState(pos).isAir() && !mc.world.getBlockState(pos).isReplaceable();
   }
}
