package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "Surround",
   category = Category.COMBAT,
   description = "Ставит блоки вокруг игрока и восстанавливает их при разрушении"
)
public class Surround extends Module {
   private static final String[] BLOCK_MODES = new String[]{"Обсидиан", "Любой блок"};
   private static final String[] ROTATION_MODES = new String[]{"OFF", "Легит", "Snap"};
   private static final String[] SWAP_MODES = new String[]{"Silent", "Видимый"};
   public static final Surround INSTANCE = new Surround();

   public final ModeSetting blockMode = new ModeSetting("Блок", BLOCK_MODES);
   public final BooleanSetting corners = new BooleanSetting("Углы", true);
   public final BooleanSetting top = new BooleanSetting("Верх", false);
   public final ModeSetting rotate = new ModeSetting("Ротация", ROTATION_MODES);
   public final ModeSetting swapMode = new ModeSetting("Свап", SWAP_MODES);
   public final NumberSetting delay = new NumberSetting("Задержка (мс)", 60.0F, 0.0F, 500.0F, 10.0F);

   private final Timer timer = new Timer();

   private Surround() {
   }

   @EventTarget
   @Native
   private void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }
      if (mc.currentScreen != null) {
         return;
      }
      if (!this.timer.finished((long)this.delay.getCurrent())) {
         return;
      }
      List<BlockPos> positions = this.getSurroundPositions(mc.player.getBlockPos());
      for (BlockPos pos : positions) {
         if (this.isSolid(pos)) {
            continue;
         }
         if (this.isBlocked(pos)) {
            continue;
         }
         BlockPos support = this.findSupport(pos);
         if (support == null) {
            continue;
         }
         int slot = this.findBlockSlot();
         if (slot == -1) {
            return;
         }
         Direction side = Direction.fromVector(pos.subtract(support), Direction.UP);
         Vec3d hitVec = Vec3d.ofCenter(support).add((double)side.getOffsetX() * 0.5D, (double)side.getOffsetY() * 0.5D, (double)side.getOffsetZ() * 0.5D);
         BlockHitResult hitResult = new BlockHitResult(hitVec, side, support, false);
         this.rotateTo(hitVec);
         this.place(slot, hitResult);
         this.timer.reset();
         return;
      }
   }

   private List<BlockPos> getSurroundPositions(BlockPos center) {
      List<BlockPos> list = new ArrayList<>();
      int[][] sides = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
      for (int[] offset : sides) {
         list.add(center.add(offset[0], offset[1], offset[2]));
      }
      if (this.corners.isEnabled()) {
         int[][] cornerOffsets = new int[][]{{1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}};
         for (int[] offset : cornerOffsets) {
            list.add(center.add(offset[0], offset[1], offset[2]));
         }
      }
      if (this.top.isEnabled()) {
         for (int[] offset : sides) {
            list.add(center.add(offset[0], offset[1] + 1, offset[2]));
         }
      }
      return list;
   }

   private int findBlockSlot() {
      boolean obsidianOnly = this.blockMode.is("Обсидиан");
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            continue;
         }
         if (obsidianOnly && stack.getItem() != Blocks.OBSIDIAN.asItem()) {
            continue;
         }
         return i;
      }
      return -1;
   }

   private void place(int slot, BlockHitResult hitResult) {
      if (this.swapMode.is("Silent")) {
         int current = mc.player.getInventory().selectedSlot;
         if (current != slot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
         mc.player.swingHand(Hand.MAIN_HAND);
         if (current != slot) {
            mc.player.getInventory().selectedSlot = current;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(current));
         }
      } else {
         mc.player.getInventory().selectedSlot = slot;
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   private void rotateTo(Vec3d point) {
      if (this.rotate.is("OFF")) {
         return;
      }
      Vec3d eye = mc.player.getEyePos();
      Vec3d diff = point.subtract(eye);
      Rotation rotation = Rotation.fromRotationVec(diff);
      if (this.rotate.is("Легит")) {
         RotationComponent.update(rotation, 20.0F, 20.0F, 10.0F, 10.0F, 6, 1, false);
      } else {
         RotationComponent.update(rotation, 360.0F, 360.0F, 360.0F, 360.0F, 6, 1, false);
      }
   }

   private boolean isSolid(BlockPos pos) {
      if (mc.world == null) {
         return false;
      }
      return !mc.world.getBlockState(pos).isAir() && !mc.world.getBlockState(pos).isReplaceable();
   }

   private BlockPos findSupport(BlockPos target) {
      if (this.isSolid(target.down())) {
         return target.down();
      }
      for (Direction direction : Direction.Type.HORIZONTAL) {
         if (this.isSolid(target.offset(direction))) {
            return target.offset(direction);
         }
      }
      return null;
   }

   private boolean isBlocked(BlockPos pos) {
      Box box = new Box(pos);
      for (Entity entity : mc.world.getEntities()) {
         if (entity != null && entity.getBoundingBox().intersects(box)) {
            return true;
         }
      }
      return false;
   }
}
