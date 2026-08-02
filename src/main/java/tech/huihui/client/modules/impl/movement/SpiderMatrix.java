package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
   name = "SpiderMatrix",
   category = Category.MOVEMENT,
   description = "Лазает по стенам или поднимается на воде"
)
public final class SpiderMatrix extends Module {
   public static final SpiderMatrix INSTANCE = new SpiderMatrix();
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Стены", "Вода"});
   private final ModeSetting method = new ModeSetting("Способ", this::isWaterMode, new String[]{"Легитный", "Пакетный"});
   private final NumberSetting speed = new NumberSetting("Скорость", 0.42F, 0.1F, 1.0F, 0.02F, "Скорость подъёма");
   private final BooleanSetting forwardOnly = new BooleanSetting("Только вперёд", true);
   private final BooleanSetting noSwap = new BooleanSetting("No Swap", "Не возвращать слот обратно", false);
   private final BooleanSetting matrixBypass = new BooleanSetting("Matrix Bypass", "Не ставить скорость без прыжка", false);

   @EventTarget
   private void onUpdate(EventUpdate ignored) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (this.mode.is("Вода")) {
         this.waterClimb();
      } else {
         this.wallClimb();
      }
   }

   private boolean isWaterMode() {
      return this.mode.is("Вода");
   }

   private void wallClimb() {
      if (mc.player.isGliding() || !mc.player.horizontalCollision) {
         return;
      }

      if (this.forwardOnly.isEnabled() && mc.player.input.movementForward <= 0.0F) {
         return;
      }

      Vec3d velocity = mc.player.getVelocity();
      mc.player.setVelocity(velocity.x, this.speed.getCurrent(), velocity.z);
   }

   private void waterClimb() {
      if (mc.player.isGliding() || !this.hasWaterBucket()) {
         return;
      }

      mc.player.setPitch(90.0F);

      if (this.method.is("Пакетный")) {
         PlayerInventoryUtil.swapAndUseHvH(Items.WATER_BUCKET);
         this.hidePlacedWater();
         this.rise();
      } else {
         this.legitClimb();
      }
   }

   private void legitClimb() {
      int waterSlot = PlayerInventoryUtil.find(Items.WATER_BUCKET, 0, 8);
      int previousSlot = mc.player.getInventory().selectedSlot;
      boolean inHand = mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET;

      if (waterSlot != -1 && !inHand) {
         mc.player.getInventory().selectedSlot = waterSlot;
      }

      if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET) {
         mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }

      if (mc.player.isTouchingWater()) {
         Vec3d velocity = mc.player.getVelocity();
         double target = mc.options.jumpKey.isPressed() ? 0.3D
               : (this.matrixBypass.isEnabled() ? velocity.y : 0.4D);
         mc.player.setVelocity(velocity.x, Math.max(velocity.y, target), velocity.z);
      }

      if (!this.noSwap.isEnabled() && waterSlot != -1 && mc.player.getInventory().selectedSlot != previousSlot) {
         mc.player.getInventory().selectedSlot = previousSlot;
      }
   }

   private void rise() {
      Vec3d velocity = mc.player.getVelocity();
      mc.player.setVelocity(velocity.x, Math.max(velocity.y, this.speed.getCurrent()), velocity.z);
   }

   private void hidePlacedWater() {
      if (mc.world == null) {
         return;
      }

      BlockPos feet = mc.player.getBlockPos();
      if (mc.world.getBlockState(feet).isOf(Blocks.WATER)) {
         mc.world.setBlockState(feet, Blocks.AIR.getDefaultState());
      }

      BlockPos below = feet.down();
      if (mc.world.getBlockState(below).isOf(Blocks.WATER)) {
         mc.world.setBlockState(below, Blocks.AIR.getDefaultState());
      }
   }

   private boolean hasWaterBucket() {
      return mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET
            || mc.player.getOffHandStack().getItem() == Items.WATER_BUCKET
            || PlayerInventoryUtil.find(Items.WATER_BUCKET, 0, 45) != -1;
   }
}
