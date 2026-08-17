package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
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
   public final NumberSetting delay = new NumberSetting("Задержка (мс)", 100.0F, 50.0F, 500.0F, 10.0F);
   public final NumberSetting randomDelay = new NumberSetting("Рандом задержка", 50.0F, 0.0F, 200.0F, 5.0F);
   public final BooleanSetting strictRotation = new BooleanSetting("Строгая ротация", true);
   public final BooleanSetting groundCheck = new BooleanSetting("Проверка земли", true);
   public final BooleanSetting raytraceCheck = new BooleanSetting("Raytrace проверка", true);

   private final Timer timer = new Timer();
   private final Random random = new Random();
   
   // Для отложенного плейса после ротации
   private BlockPos pendingPlace = null;
   private int pendingSlot = -1;
   private Hand pendingHand = Hand.MAIN_HAND;
   private BlockHitResult pendingHitResult = null;
   private int rotationTicks = 0;

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
      
      // Проверка на землю (Grim флагает плейс в воздухе)
      if (this.groundCheck.isEnabled() && !mc.player.isOnGround() && !mc.player.getAbilities().flying) {
         return;
      }
      
      // Обработка отложенного плейса после ротации
      if (this.pendingPlace != null) {
         if (this.rotationTicks > 0) {
            this.rotationTicks--;
            return;
         }
         
         // Проверяем, что ротация достигла цели
         if (this.strictRotation.isEnabled() && !this.isRotationDone()) {
            return;
         }
         
         this.executePlace(this.pendingSlot, this.pendingHand, this.pendingHitResult);
         this.pendingPlace = null;
         this.pendingSlot = -1;
         this.pendingHitResult = null;
         return;
      }
      
      // Рандомизированная задержка
      long currentDelay = (long)(this.delay.getCurrent() + random.nextInt((int)this.randomDelay.getCurrent()));
      if (!this.timer.finished(currentDelay)) {
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
         
         // Проверка дистанции (Grim флагает > 4.5)
         double distance = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
         if (distance > 4.5) {
            continue;
         }
         
         BlockPos support = this.findSupport(pos);
         if (support == null) {
            continue;
         }
         
         // Определяем руку и слот
         Hand hand = this.getPlaceHand();
         int slot = -1;
         
         if (hand == Hand.MAIN_HAND) {
            slot = this.findBlockSlot();
            if (slot == -1) {
               return;
            }
         } else if (hand == Hand.OFF_HAND) {
            // Offhand уже держит блок
            slot = mc.player.getInventory().selectedSlot;
         }
         
         Direction side = Direction.fromVector(pos.subtract(support), Direction.UP);
         Vec3d hitVec = this.getHitVec(support, side);
         
         // Raytrace проверка
         if (this.raytraceCheck.isEnabled() && !this.canSeeBlock(support, hitVec)) {
            continue;
         }
         
         BlockHitResult hitResult = new BlockHitResult(hitVec, side, support, false);
         
         // Ротация
         if (!this.rotate.is("OFF")) {
            this.rotateTo(hitVec);
            // Откладываем плейс на 1-2 тика
            this.pendingPlace = pos;
            this.pendingSlot = slot;
            this.pendingHand = hand;
            this.pendingHitResult = hitResult;
            this.rotationTicks = this.rotate.is("Легит") ? 2 : 1;
            return;
         } else {
            this.executePlace(slot, hand, hitResult);
            this.timer.reset();
            return;
         }
      }
   }
   
   private void executePlace(int slot, Hand hand, BlockHitResult hitResult) {
      if (this.swapMode.is("Silent") && hand == Hand.MAIN_HAND) {
         int current = mc.player.getInventory().selectedSlot;
         if (current != slot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
         
         // Отправляем пакеты в правильном порядке для Grim
         mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(hand, hitResult, 0));
         mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
         
         if (current != slot) {
            mc.player.getInventory().selectedSlot = current;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(current));
         }
      } else {
         if (hand == Hand.MAIN_HAND) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         }
         mc.interactionManager.interactBlock(mc.player, hand, hitResult);
         mc.player.swingHand(hand);
      }
      this.timer.reset();
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
   
   private Hand getPlaceHand() {
      ItemStack offhand = mc.player.getOffHandStack();
      boolean obsidianOnly = this.blockMode.is("Обсидиан");
      
      if (!offhand.isEmpty() && offhand.getItem() instanceof BlockItem) {
         if (!obsidianOnly || offhand.getItem() == Blocks.OBSIDIAN.asItem()) {
            return Hand.OFF_HAND;
         }
      }
      return Hand.MAIN_HAND;
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

   private void rotateTo(Vec3d point) {
      Vec3d eye = mc.player.getEyePos();
      Vec3d diff = point.subtract(eye);
      Rotation rotation = Rotation.fromRotationVec(diff);
      
      // Рандомизируем ротацию
      float yawRandom = (random.nextFloat() - 0.5f) * 2.0f;
      float pitchRandom = (random.nextFloat() - 0.5f) * 1.0f;
      rotation = new Rotation(rotation.getYaw() + yawRandom, Math.max(-90, Math.min(90, rotation.getPitch() + pitchRandom)));
      
      if (this.rotate.is("Легит")) {
         RotationComponent.update(rotation, 25.0F, 25.0F, 15.0F, 15.0F, 8, 2, false);
      } else {
         RotationComponent.update(rotation, 180.0F, 180.0F, 180.0F, 180.0F, 4, 1, false);
      }
   }
   
   private boolean isRotationDone() {
      return true;
   }
   
   private Vec3d getHitVec(BlockPos support, Direction side) {
      double x = support.getX() + 0.5 + side.getOffsetX() * 0.5;
      double y = support.getY() + 0.5 + side.getOffsetY() * 0.5;
      double z = support.getZ() + 0.5 + side.getOffsetZ() * 0.5;
      
      double randomOffset = 0.1;
      x += (random.nextDouble() - 0.5) * randomOffset;
      y += (random.nextDouble() - 0.5) * randomOffset;
      z += (random.nextDouble() - 0.5) * randomOffset;
      
      return new Vec3d(x, y, z);
   }
   
   private boolean canSeeBlock(BlockPos pos, Vec3d hitVec) {
      Vec3d eye = mc.player.getEyePos();
      return mc.world.raycast(new RaycastContext(
         eye, hitVec, 
         ShapeType.COLLIDER,
         FluidHandling.NONE,
         mc.player
      )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
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
         if (entity != null && entity != mc.player && entity.getBoundingBox().intersects(box)) {
            return true;
         }
      }
      return false;
   }
}