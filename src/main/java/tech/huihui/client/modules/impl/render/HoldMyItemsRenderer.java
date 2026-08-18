package tech.huihui.client.modules.impl.render;

import java.util.Locale;
import net.minecraft.block.PaneBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class HoldMyItemsRenderer {
   public static final HoldMyItemsRenderer INSTANCE = new HoldMyItemsRenderer();
   private static final double MAX_DELTA = 0.05D;
   private static final double ANIMATION_SPEED = 30.0D;
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private double prevFrameTime = System.nanoTime() / 1.0E9D;
   private double deltaTime;
   private double previousRotation;
   private float swingAngleY;
   private float swingAngleX;
   private float swingVelocityY;
   private float swingVelocityX;
   private float swingVelocityZ;
   private float vertAngleY;
   private float vertVelocityYSlime;
   private float vertAngleYSlime;
   private float climbBlend;
   private float crawlCount;
   private float directionalCrawlCount;
   private float inWaterCounter;
   private boolean isAttacking;
   private boolean holdMyItemsLeft;
   private float prevSwingProgress;
   private boolean physicsUpdatedThisFrame;
   private float chestRightHandMotion;

   private HoldMyItemsRenderer() {
   }

   public void renderItemInFirstPerson(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Object cancelToken) {
      if (stack.getItem() instanceof FilledMapItem || stack.getItem() instanceof CrossbowItem) {
         return;
      }
      if (player.isUsingItem() && player.getActiveHand() == hand) {
         UseAction action = stack.getUseAction();
         if (action != UseAction.EAT && action != UseAction.DRINK && action != UseAction.BOW) {
            return;
         }
      }
      this.cancel(cancelToken);
      this.updateDeltaTime();
      this.updateChestRightHandMotion();
      boolean mainHand = hand == Hand.MAIN_HAND;
      Arm handside = mainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      if (swingProgress > 0.0F && this.prevSwingProgress == 0.0F) {
         this.holdMyItemsLeft = !this.holdMyItemsLeft;
      }
      this.prevSwingProgress = swingProgress;

      if (stack.isEmpty()) {
         matrices.push();
         this.applySwing(matrices, player, hand, stack, swingProgress);
         this.applyEnvironment(matrices, player, hand, handside, stack, swingProgress, tickDelta);
         this.applyBaseHandPose(matrices, handside, equipProgress, swingProgress);
         this.renderArmFirstPerson(matrices, vertexConsumers, light, 0.0F, 0.0F, handside);
         matrices.pop();
         return;
      }

      if (this.shouldUseBow(player, hand, stack)) {
         this.renderBow(player, tickDelta, hand, swingProgress, stack, equipProgress, matrices, vertexConsumers, light, handside);
         return;
      }
      if (this.shouldUseConsume(player, hand, stack)) {
         this.renderConsume(player, tickDelta, hand, swingProgress, stack, equipProgress, matrices, vertexConsumers, light, handside);
         return;
      }
      this.renderHoldMyItems(player, tickDelta, hand, swingProgress, stack, equipProgress, matrices, vertexConsumers, light, handside);
   }

   private void cancel(Object token) {
      if (token instanceof org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
         ci.cancel();
      }
   }

   private void updateDeltaTime() {
      double currentTime = System.nanoTime() / 1.0E9D;
      this.deltaTime = Math.min(MAX_DELTA, Math.max(0.0D, currentTime - this.prevFrameTime));
      this.prevFrameTime = currentTime;
      this.physicsUpdatedThisFrame = false;
   }

   private boolean shouldUseBow(AbstractClientPlayerEntity player, Hand hand, ItemStack stack) {
      return stack.getUseAction() == UseAction.BOW && player.isUsingItem() && player.getActiveHand() == hand;
   }

   private boolean shouldUseConsume(AbstractClientPlayerEntity player, Hand hand, ItemStack stack) {
      ItemStack active = !player.getStackInHand(player.getActiveHand()).isEmpty() && player.getActiveHand() == hand
            ? player.getStackInHand(player.getActiveHand()) : stack;
      UseAction action = active.getUseAction();
      return (action == UseAction.EAT || action == UseAction.DRINK) && player.isUsingItem() && player.getActiveHand() == hand;
   }

   private boolean isThrowable(ItemStack stack) {
      return stack.getItem() instanceof ExperienceBottleItem
            || stack.getItem() instanceof EggItem
            || stack.getItem() instanceof SnowballItem
            || stack.getItem() instanceof EnderPearlItem
            || stack.getItem() instanceof SplashPotionItem
            || stack.getItem() instanceof LingeringPotionItem
            || stack.getItem() == Items.ENDER_EYE;
   }

   private boolean isWeapon(ItemStack stack) {
      return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem;
   }

   private boolean isTool(ItemStack stack) {
      return stack.getItem() instanceof net.minecraft.item.AxeItem
            || stack.getItem() instanceof net.minecraft.item.PickaxeItem
            || stack.getItem() instanceof net.minecraft.item.ShovelItem
            || stack.getItem() instanceof net.minecraft.item.HoeItem
            || stack.getItem() instanceof TridentItem;
   }

   private boolean isShovel(ItemStack stack) {
      return stack.getItem() instanceof ShovelItem;
   }

   private boolean isLantern(ItemStack stack) {
      return stack.getItem() == Items.LANTERN || stack.getItem() == Items.SOUL_LANTERN;
   }

   private boolean isThinBlock(ItemStack stack) {
      if (!(stack.getItem() instanceof BlockItem)) {
         return false;
      }
      BlockItem blockItem = (BlockItem) stack.getItem();
      if (stack.getItem() == Items.STRING
            || stack.getItem() == Items.REDSTONE
            || stack.getItem() == Items.LEVER
            || stack.getItem() == Items.TRIPWIRE_HOOK) {
         return true;
      }
      net.minecraft.block.BlockState state = blockItem.getBlock().getDefaultState();
      return blockItem.getBlock() instanceof PaneBlock
            || state.isIn(BlockTags.RAILS)
            || state.isIn(BlockTags.CLIMBABLE)
            || state.isIn(BlockTags.DOORS);
   }

   private boolean isTorch(ItemStack stack) {
      String name = stack.getName().getString().toLowerCase(Locale.ROOT);
      return name.contains("torch") || name.contains("факел");
   }

   private boolean isSmallItem(ItemStack stack) {
      return !(stack.getItem() instanceof BlockItem)
            && !this.isTool(stack)
            && !this.isWeapon(stack)
            && !(stack.getItem() instanceof FishingRodItem)
            && !(stack.getItem() instanceof BucketItem)
            && stack.getUseAction() != UseAction.BOW
            && stack.getUseAction() != UseAction.SPEAR
            && stack.getUseAction() != UseAction.BLOCK;
   }

   private boolean isCrawling(AbstractClientPlayerEntity player) {
      return player.isCrawling();
   }

   private boolean isClimbing(AbstractClientPlayerEntity player) {
      return player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().y) > 0.0D;
   }

   private void renderArmFirstPerson(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm side) {
      boolean right = side != Arm.LEFT;
      float f = right ? 1.0F : -1.0F;
      float f1 = MathHelper.sqrt(swingProgress);
      float f2 = -0.3F * MathHelper.sin(f1 * (float) Math.PI);
      float f3 = 0.4F * MathHelper.sin(f1 * (float) Math.PI * 2.0F);
      float f4 = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
      matrices.translate(f * (f2 + 0.64F), f3 - 0.6F + equipProgress * -0.6F, f4 - 0.72F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * 45.0F));
      float f5 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
      float f6 = MathHelper.sin(f1 * (float) Math.PI);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * f6 * 70.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * f5 * -20.0F));
      AbstractClientPlayerEntity player = this.mc.player;
      matrices.translate(f * -1.0F, 3.6F, 3.5D);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 120.0F));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(200.0F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * -135.0F));
      matrices.translate(f * 5.6F, 0.0D, 0.0D);
      this.applyHandOffsets(matrices, side);
      PlayerEntityRenderer renderer = (PlayerEntityRenderer) this.mc.getEntityRenderDispatcher().getRenderer(player);
      if (right) {
         renderer.renderRightArm(matrices, vertexConsumers, light, player.getSkinTextures().texture(), false);
      } else {
         renderer.renderLeftArm(matrices, vertexConsumers, light, player.getSkinTextures().texture(), false);
      }
   }

   private void applyHandOffsets(MatrixStack matrices, Arm side) {
      if (side == Arm.RIGHT) {
         matrices.translate(SwingAnimation.INSTANCE.rightX.getCurrent(), SwingAnimation.INSTANCE.rightY.getCurrent(), SwingAnimation.INSTANCE.rightZ.getCurrent());
      } else {
         matrices.translate(SwingAnimation.INSTANCE.leftX.getCurrent(), SwingAnimation.INSTANCE.leftY.getCurrent(), SwingAnimation.INSTANCE.leftZ.getCurrent());
      }
   }

   private void renderItemSide(AbstractClientPlayerEntity player, ItemStack stack, boolean rightHand, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
      if (!stack.isEmpty()) {
         ModelTransformationMode mode = rightHand ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND;
         this.mc.getItemRenderer().renderItem(player, stack, mode, !rightHand, matrices, vertexConsumers, player.getWorld(), light, OverlayTexture.DEFAULT_UV, 0);
      }
   }

   private float ease(float value) {
      float c1 = 1.70158F;
      float c2 = c1 * 1.525F;
      if (value < 0.5F) {
         float doubled = 2.0F * value;
         return doubled * doubled * ((c2 + 1.0F) * doubled - c2) * 0.5F;
      }
      float shifted = 2.0F * value - 2.0F;
      return (shifted * shifted * ((c2 + 1.0F) * shifted + c2) + 2.0F) * 0.5F;
   }

   private float getSwingRot(float swingProgress) {
      if (swingProgress < 0.6F) {
         return MathHelper.sin(MathHelper.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F);
      }
      return MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
   }

   private void updateChestRightHandMotion() {
      float target = this.mc.currentScreen instanceof GenericContainerScreen ? 1.0F : 0.0F;
      this.chestRightHandMotion = MathHelper.lerp(0.18F, this.chestRightHandMotion, target);
   }

   private void applyChestRightHandMotion(MatrixStack matrices, Arm handside) {
      if (handside != Arm.RIGHT || this.chestRightHandMotion <= 0.001F) {
         return;
      }
      float progress = this.chestRightHandMotion;
      float time = (System.currentTimeMillis() % 1200L) / 1200.0F;
      float pulse = MathHelper.sin(time * (float) Math.PI * 2.0F) * progress;
      matrices.translate(0.04D * progress, -0.03D * progress + 0.01D * pulse, -0.12D * progress);
      matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(12.0F * progress));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(8.0F * progress + 2.5F * pulse));
      matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(4.0F * progress));
   }

   private void applyBaseHandPose(MatrixStack matrices, Arm handside, float equipProgress, float swingProgress) {
      int direction = handside == Arm.RIGHT ? 1 : -1;
      float swingSin = MathHelper.sin(swingProgress * (float) Math.PI);
      matrices.translate(direction, -equipProgress * 0.3D, 0.3D);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * direction));
      matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(40.0F * direction));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * (45.0F + swingSin * 0.0F)));
      matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(direction * 45.0F));
      matrices.scale(0.9F, 0.9F, 0.9F);
   }

   private void applyArmPrePose(MatrixStack matrices, ItemStack stack, Arm handside) {
      int direction = handside == Arm.RIGHT ? 1 : -1;
      if (this.isLantern(stack)) {
         matrices.translate(0.1D * direction, 0.0D, -0.1D);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
         return;
      }
      if (stack.getUseAction() == UseAction.BLOCK) {
         matrices.translate(0.0D, -0.2D, 0.0D);
      }
   }

   private void applyEnvironment(MatrixStack matrices, AbstractClientPlayerEntity player, Hand handIn, Arm handside, ItemStack stack, float swingProgress, float tickDelta) {
      float yaw = MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw());
      double radians = Math.toRadians(yaw);
      double forwardX = -Math.sin(radians);
      double forwardZ = Math.cos(radians);
      net.minecraft.util.math.Vec3d motion = player.getVelocity();
      double dotProduct = motion.x * forwardX + motion.z * forwardZ;
      double crossProduct = motion.x * forwardZ - motion.z * forwardX;
      float pitchFactor = player.getPitch() != 0.0F ? 90.0F / player.getPitch() / 10.0F : 1.0F;
      if (pitchFactor > 1.0F || pitchFactor < 0.0F) {
         pitchFactor = 1.0F;
      }
      boolean crawling = this.isCrawling(player);
      boolean climbing = this.isClimbing(player);
      boolean elytraFlying = player.isGliding();
      double tt = this.deltaTime * ANIMATION_SPEED;
      float handDirection = handIn == Hand.MAIN_HAND ? 1.0F : -1.0F;
      int armDirection = handside == Arm.RIGHT ? 1 : -1;

      if (elytraFlying) {
         if (!this.physicsUpdatedThisFrame) {
            this.climbBlend = 0.0F;
            this.inWaterCounter = 0.0F;
            this.vertAngleY *= (float) Math.pow(0.72D, tt);
            this.vertVelocityYSlime *= (float) Math.pow(0.72D, tt);
            this.vertAngleYSlime *= (float) Math.pow(0.72D, tt);
            this.physicsUpdatedThisFrame = true;
         }
         if (!stack.isEmpty() && stack.getUseAction() != UseAction.BLOCK) {
            matrices.translate(0.0D, -0.1D, 0.1D);
         }
         if (this.isLantern(stack)) {
            matrices.translate(0.0D, 0.1D, 0.0D);
         }
         return;
      }

      if (!this.physicsUpdatedThisFrame) {
         if (motion.length() >= 0.08D) {
            double clampedSpeed = Math.min(motion.length(), 0.22D);
            double clampedDot = MathHelper.clamp(dotProduct, -0.22D, 0.22D);
            double clampedCross = MathHelper.clamp(crossProduct, -0.22D, 0.22D);
            this.crawlCount = (float) (this.crawlCount + 0.1D * clampedSpeed * 2.0D * tt);
            this.directionalCrawlCount = (float) (this.directionalCrawlCount + 0.1D * clampedDot * 4.0D * tt);
            this.directionalCrawlCount = (float) (this.directionalCrawlCount + (clampedDot > 0.0D ? 0.1D * Math.abs(clampedCross) * 4.0D * tt : 0.1D * Math.abs(clampedCross) * -4.0D * tt));
         }
         float motionYNormalized = player.isOnGround() ? 0.0F : (float) MathHelper.clamp(motion.y, -0.42D, 0.42D);
         this.vertAngleY = (float) (this.vertAngleY + motionYNormalized * 0.015D * tt);
         this.vertAngleY = (float) (this.vertAngleY - 0.1D * this.vertAngleY * tt);
         this.vertAngleY = (float) (this.vertAngleY * Math.pow(0.88D, tt));
         this.vertVelocityYSlime = (float) (this.vertVelocityYSlime + motionYNormalized * 0.015D * tt);
         this.vertVelocityYSlime = (float) (this.vertVelocityYSlime - 0.1D * this.vertAngleYSlime * tt);
         this.vertVelocityYSlime = (float) (this.vertVelocityYSlime * Math.pow(0.88D, tt));
         this.vertAngleYSlime = (float) (this.vertAngleYSlime + this.vertVelocityYSlime * tt);
         if (player.isTouchingWater() && !player.isSwimming() && !player.isSubmergedIn(FluidTags.WATER)) {
            this.inWaterCounter = (float) (this.inWaterCounter + 0.1D * tt);
            if (this.inWaterCounter > 1.0F) {
               this.inWaterCounter = 1.0F;
            }
         } else {
            this.inWaterCounter = (float) (this.inWaterCounter * Math.pow(0.88D, tt));
         }
         this.physicsUpdatedThisFrame = true;
      }

      if ((crawling || climbing) && !player.isUsingItem() && swingProgress == 0.0F) {
         this.climbBlend = (float) (this.climbBlend + 0.1D * tt);
         if (this.climbBlend > 1.0F) {
            this.climbBlend = 1.0F;
         }
         if (!this.isLantern(stack)) {
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * this.climbBlend));
         }
      } else {
         this.climbBlend = (float) (this.climbBlend * Math.pow(0.88D, tt));
      }

      if (swingProgress == 0.0F) {
         float pitch = player.getPitch();
         matrices.translate(handDirection > 0.0F ? pitch / 650.0F * this.climbBlend * -1.0F : pitch / 650.0F * this.climbBlend, 0.0F, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * this.climbBlend));
      }

      if (!this.isLantern(stack)) {
         matrices.translate(0.0D, 0.0D, player.getPitch() / 120.0F * this.climbBlend);
      } else if (swingProgress == 0.0F) {
         matrices.translate(0.0D, 0.0D, player.getPitch() / 80.0F * this.climbBlend);
      }

      if (climbing && !this.isLantern(stack) && !player.isUsingItem()) {
         matrices.translate(0.0D, 0.1D, -0.2D);
      }

      matrices.translate(0.0D, 0.02D * this.inWaterCounter, 0.0D);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * handDirection * this.inWaterCounter));
      matrices.translate(0.0D, -this.vertAngleY, 0.0D);
      matrices.translate(0.0D, Math.sin(player.age * 0.1D) * 0.007D * armDirection, 0.0D);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin(player.age * 0.15F) * armDirection));

      if (!stack.isEmpty() || crawling || climbing || player.isSwimming()) {
         if (stack.getUseAction() != UseAction.BLOCK) {
            matrices.translate(0.0D, -0.1D, 0.1D);
         }
      }
      if (this.isLantern(stack)) {
         matrices.translate(0.0D, 0.1D, 0.0D);
         if (player.isSwimming()) {
            matrices.translate(0.0D, -0.1D, 0.1D);
         }
      }
      if (player.isSwimming() && swingProgress == 0.0F) {
         double distance = (player.age + tickDelta) * 0.2D;
         double handRotation = Math.sin(distance) * 1.5D;
         double smoothRotation = handRotation * 0.8D + this.previousRotation * 0.2D;
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (handIn == Hand.MAIN_HAND ? smoothRotation : -smoothRotation)));
         matrices.translate(0.0D, 0.0D, smoothRotation * 0.2D);
         this.previousRotation = smoothRotation;
      }
      if ((climbing || crawling) && !player.isUsingItem() && swingProgress == 0.0F) {
         float crawlProgress = MathHelper.sin(this.directionalCrawlCount * 4.0F);
         float upAndDown = MathHelper.cos(this.directionalCrawlCount * 4.0F);
         if (this.isLantern(stack)) {
            crawlProgress *= 0.14F;
            upAndDown *= 0.14F;
         }
         matrices.translate(0.2D * crawlProgress, 0.3D * crawlProgress * armDirection, -0.2D * crawlProgress * armDirection * pitchFactor);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * crawlProgress));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * upAndDown * armDirection, 0.0F, 20.0F)));
      }
   }

   private void applyLanternPose(MatrixStack matrices, AbstractClientPlayerEntity player, Arm handside, float swingProgress) {
      float dt = (float) (this.deltaTime * ANIMATION_SPEED);
      int direction = handside == Arm.RIGHT ? 1 : -1;
      float yawDelta = player.prevYaw - player.getYaw();
      float pitchDelta = player.prevPitch - player.getPitch();
      this.swingVelocityY += yawDelta * 0.015F * dt;
      this.swingVelocityY += swingProgress * 2.0F * dt;
      this.swingVelocityX += pitchDelta * 0.015F * dt;
      this.swingVelocityY -= 0.1F * this.swingAngleY * dt;
      this.swingVelocityX -= 0.1F * this.swingAngleX * dt;
      this.swingVelocityY = (float) (this.swingVelocityY * Math.pow(0.88D, dt));
      this.swingVelocityX = (float) (this.swingVelocityX * Math.pow(0.88D, dt));
      this.swingAngleY += this.swingVelocityY * dt;
      this.swingAngleX += this.swingVelocityX * dt;
      double currentSpeed = player.getVelocity().length();
      this.swingVelocityZ = (float) (this.swingVelocityZ + (direction > 0 ? ((currentSpeed * -15.0D) - this.swingVelocityZ) * 0.1D * dt : ((currentSpeed * 15.0D) - this.swingVelocityZ) * 0.1D * dt));
      if (currentSpeed > 0.09D && (player.isOnGround() || player.isSwimming() || this.isClimbing(player)) && this.mc.options.getBobView().getValue()) {
         this.swingVelocityY += (float) ((Math.random() < 0.5D ? -5.5D : 5.5D) * currentSpeed * dt);
      }
      matrices.translate(0.0D, 0.0D, -0.1D);
      matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(35.0F * direction + this.swingAngleY));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + this.swingAngleX));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction + this.swingVelocityZ));
      matrices.translate(0.3D * direction, -0.35D, 0.0D);
      matrices.translate(0.0D, 0.0D, 0.1D);
      matrices.scale(1.5F, 1.5F, 1.5F);
   }

   private void applySwing(MatrixStack matrices, AbstractClientPlayerEntity player, Hand handIn, ItemStack stack, float swingProgress) {
      boolean mainHand = handIn == Hand.MAIN_HAND;
      if (player.getMainArm() == Arm.LEFT) {
         mainHand = !mainHand;
      }
      float ll = mainHand ? 1.0F : -1.0F;
      float handDirection = handIn == Hand.MAIN_HAND ? 1.0F : -1.0F;
      float swingRot = this.getSwingRot(swingProgress);
      float swing = this.ease(MathHelper.sin(swingProgress * (float) Math.PI));
      SwingAnimation animation = SwingAnimation.INSTANCE;
      boolean forwardHandsAttack = animation.animationMode.is("HMI Вперед");
      boolean normalHandsAttack = animation.animationMode.is("HMI Обычная");

      if (animation.animationMode.is("HMI Копье")) {
         matrices.translate(0.0D, 0.0D, 0.45D * swingRot);
         matrices.translate(-0.25D * handDirection * swing, -0.35D * swingRot, -0.6D * swing);
         matrices.translate(0.0D, 0.1D * swing, 0.0D);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot * ll));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swingRot * ll));
         return;
      }
      if (animation.animationMode.is("HMI Инструмент")) {
         matrices.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.5D * swing);
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swingRot));
         matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(20.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
         return;
      }
      if (animation.animationMode.is("HMI Блок")) {
         matrices.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.2D * swing);
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(10.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
         return;
      }
      if (animation.animationMode.is("HMI Лопата")) {
         matrices.translate(0.0D, 0.15D * swingRot, -0.25D * swingRot);
         matrices.translate(0.0D, 0.0D, -0.2D * swing);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
         return;
      }

      if (stack.getItem() instanceof SwordItem && forwardHandsAttack) {
         matrices.translate(0.12D * ll * swingRot, 0.04D * swingRot, -0.95D * swing);
         matrices.translate(0.02D * ll * swing, 0.10D * swing, -0.10D * swingRot);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(8.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-14.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-18.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(32.0F * swing));
         return;
      }
      if (stack.getItem() instanceof SwordItem && normalHandsAttack) {
         this.applyGenericSwing(matrices, ll, swingRot, swing);
         return;
      }
      if ((this.holdMyItemsLeft || stack.getItem() instanceof AxeItem || stack.getUseAction() == UseAction.SPEAR || stack.getUseAction() == UseAction.BLOCK) && !this.isShovel(stack)) {
         if (this.isWeapon(stack)) {
            matrices.translate(0.8D * ll * swingRot, 0.3D * swingRot, -0.5D * swing);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swingRot));
            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(70.0F * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees((stack.getItem() instanceof SwordItem ? 40.0F : 30.0F) * swing));
            return;
         }
         if (stack.getUseAction() == UseAction.SPEAR) {
            matrices.translate(0.0D, 0.0D, 0.45D * swingRot);
            matrices.translate(-0.25D * handDirection * swing, -0.35D * swingRot, -0.6D * swing);
            matrices.translate(0.0D, 0.1D * swing, 0.0D);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot * ll));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swingRot * ll));
            return;
         }
         if (this.isTool(stack) && stack.getUseAction() != UseAction.BLOCK) {
            matrices.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.5D * swing);
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swingRot));
            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(20.0F * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
            return;
         }
         if (stack.getUseAction() != UseAction.BLOCK) {
            matrices.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.1D * swing);
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swingRot));
            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(10.0F * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * ll));
            return;
         }
         matrices.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.2D * swing);
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swingRot));
         matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(10.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
         return;
      }
      if (this.isShovel(stack)) {
         matrices.translate(0.0D, 0.15D * swingRot, -0.25D * swingRot);
         matrices.translate(0.0D, 0.0D, -0.2D * swing);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swingRot));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
         return;
      }
      if (stack.getItem() instanceof SwordItem) {
         matrices.translate(-0.55D * ll * swingRot, -0.8D * swingRot, -0.77D * swing);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swingRot));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(50.0F * swing));
         return;
      }
      if (this.isTool(stack)) {
         matrices.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.5D * swing);
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swingRot));
         matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(20.0F * swingRot * ll));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
         return;
      }
      this.applyGenericSwing(matrices, ll, swingRot, swing);
   }

   private void applyGenericSwing(MatrixStack matrices, float direction, float swingRot, float swing) {
      matrices.translate(0.1D * direction * swingRot, 0.1D * swingRot, -0.1D * swing);
      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swingRot));
      matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(10.0F * swingRot * direction));
      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * direction));
   }

   private void renderConsume(AbstractClientPlayerEntity player, float tickDelta, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm handside) {
      ItemStack activeStack = !player.getStackInHand(player.getActiveHand()).isEmpty() && player.getActiveHand() == hand
            ? player.getStackInHand(player.getActiveHand()) : stack;
      int direction = handside == Arm.RIGHT ? 1 : -1;
      float useTicks = (float) activeStack.getMaxUseTime(player) - ((float) player.getItemUseTime() - tickDelta + 1.0F);
      float progress = MathHelper.clamp(useTicks / 5.0F, 0.0F, 1.0F);
      float wobble = MathHelper.sin(useTicks / 2.0F * (float) Math.PI) * 0.1F;

      matrices.push();
      matrices.translate(direction, 0.1D, 0.3D);
      matrices.translate(0.2D * direction * progress, -0.7D * progress, -0.2D * progress);
      matrices.translate(0.0D, -0.2D * wobble, -0.2D * wobble);
      matrices.translate(0.0D, 0.1D * this.ease(MathHelper.sin(progress * (float) Math.PI)), 0.0D);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * direction));
      matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(40.0F * direction));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
      matrices.scale(0.9F, 0.9F, 0.9F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * progress * direction));

      this.renderArmFirstPerson(matrices, vertexConsumers, light, 0.0F, swingProgress, handside);
      this.applyItemPose(matrices, player, hand, handside, activeStack, swingProgress);
      this.renderItemSide(player, activeStack, handside == Arm.RIGHT, matrices, vertexConsumers, light);
      matrices.pop();
      this.isAttacking = this.mc.options.attackKey.isPressed();
   }

   private void renderBow(AbstractClientPlayerEntity player, float tickDelta, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm handside) {
      boolean rightHand = handside == Arm.RIGHT;
      int handDirection = rightHand ? 1 : -1;
      float useTicks = (float) stack.getMaxUseTime(player) - ((float) player.getItemUseTime() - tickDelta + 1.0F);
      float drawLinear = MathHelper.clamp(useTicks / 20.0F, 0.0F, 1.0F);
      float drawCurve = (drawLinear * drawLinear + drawLinear * 2.0F) / 3.0F;

      matrices.push();
      this.applyEnvironment(matrices, player, hand, handside, stack, swingProgress, tickDelta);
      matrices.push();
      matrices.translate(rightHand ? -0.1D : 0.1D, 0.0D, drawLinear * 0.15D);
      this.renderArmFirstPerson(matrices, vertexConsumers, light, equipProgress, swingProgress, handside);
      matrices.pop();
      matrices.push();
      matrices.translate(rightHand ? -0.5D : 0.5D, -0.45D, 0.1D);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(17.2F));
      if (rightHand) {
         matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(17.2F));
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(57.3F));
         this.renderArmFirstPerson(matrices, vertexConsumers, light, equipProgress, swingProgress, handside.getOpposite());
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(143.2F));
      } else {
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(17.2F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(57.3F));
         this.renderArmFirstPerson(matrices, vertexConsumers, light, equipProgress, swingProgress, handside.getOpposite());
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(143.2F));
      }
      matrices.translate(rightHand ? -0.65D : 0.65D, -0.35D, 0.27D);
      matrices.pop();

      matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
      matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(15.0F * handDirection));
      matrices.translate(0.8D * handDirection, -equipProgress * 0.3D, -0.1D);
      this.applyItemPose(matrices, player, hand, handside, stack, swingProgress);
      this.renderItemSide(player, stack, rightHand, matrices, vertexConsumers, light);
      matrices.pop();
      this.isAttacking = this.mc.options.attackKey.isPressed();
   }

   private void applyItemPose(MatrixStack matrices, AbstractClientPlayerEntity player, Hand hand, Arm handside, ItemStack stack, float swingProgress) {
      int direction = handside == Arm.RIGHT ? 1 : -1;
      boolean mainHand = hand == Hand.MAIN_HAND;
      if (player.getMainArm() == Arm.LEFT) {
         mainHand = !mainHand;
      }

      matrices.translate(-0.3D * direction, 0.65D, -0.1D);
      matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(65.0F * direction));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));

      if (stack.getItem() instanceof BlockItem && !(stack.getItem() instanceof BucketItem) && stack.getUseAction() != UseAction.EAT) {
         if (this.isTorch(stack)) {
            matrices.scale(1.5F, 1.5F, 1.5F);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25.0F * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction));
            matrices.translate(0.2D * direction, 0.2D, 0.05D);
            return;
         }
         if (this.isThinBlock(stack)) {
            matrices.translate(0.0D, 0.0D, -0.1D);
            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5.0F * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction));
            return;
         }
         if (this.isLantern(stack)) {
            this.applyLanternPose(matrices, player, handside, swingProgress);
            return;
         }
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25.0F * direction));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction));
         matrices.translate(0.2D * direction, 0.2D, 0.05D);
         return;
      }

      if (this.isSmallItem(stack) && !this.isWeapon(stack)) {
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5.0F * direction));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0F * direction));
         matrices.translate(0.0D, -0.05D, -0.1D);
         matrices.scale(0.7F, 0.7F, 0.7F);
         return;
      }

      if (stack.getUseAction() == UseAction.BLOCK) {
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160.0F * direction));
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(60.0F * direction));
         matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(70.0F));
         matrices.scale(0.75F, 0.75F, 0.75F);
         matrices.translate(0.15D * direction, mainHand ? 0.35D : 0.45D, mainHand ? -0.15D : -0.1D);
         matrices.translate(0.17D * direction, 0.0D, 0.3D);
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90.0F * direction));
         return;
      }

      if (stack.getUseAction() == UseAction.SPEAR) {
         matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75.0F * direction));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0F * direction));
         matrices.translate(-0.3D * direction, 0.0D, 0.0D);
         return;
      }

      matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75.0F * direction));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0F * direction));
      matrices.scale(1.2F, 1.2F, 1.2F);
   }

   private void renderHoldMyItems(AbstractClientPlayerEntity player, float tickDelta, Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm handside) {
      boolean rightHand = handside == Arm.RIGHT;
      matrices.push();
      if (stack.isEmpty()) {
         this.applySwing(matrices, player, hand, stack, swingProgress);
         this.applyEnvironment(matrices, player, hand, handside, stack, swingProgress, tickDelta);
         this.applyBaseHandPose(matrices, handside, equipProgress, swingProgress);
         this.renderArmFirstPerson(matrices, vertexConsumers, light, 0.0F, 0.0F, handside);
         matrices.pop();
         return;
      }
      this.applySwing(matrices, player, hand, stack, swingProgress);
      this.applyEnvironment(matrices, player, hand, handside, stack, swingProgress, tickDelta);
      this.applyArmPrePose(matrices, stack, handside);
      this.applyBaseHandPose(matrices, handside, equipProgress, swingProgress);
      this.renderArmFirstPerson(matrices, vertexConsumers, light, 0.0F, 0.0F, handside);
      this.applyItemPose(matrices, player, hand, handside, stack, swingProgress);
      this.renderItemSide(player, stack, rightHand, matrices, vertexConsumers, light);
      matrices.pop();
      this.isAttacking = this.mc.options.attackKey.isPressed();
   }
}
