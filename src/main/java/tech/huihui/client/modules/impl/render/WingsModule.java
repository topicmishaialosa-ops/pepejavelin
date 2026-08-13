package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.combat.AntiBot;
import tech.huihui.utility.render.display.base.color.ColorUtil;

@ModuleAnnotation(
   name = "Wings",
   category = Category.COSMETICS,
   description = "Косметические крылья"
)
public final class WingsModule extends Module {
   public static final WingsModule INSTANCE = new WingsModule();
   private final Map<UUID, WingAnimationState> animationStates = new HashMap<>();

   private static final int[] RIB_BONE = {2, 4, 7, 9, 11};

   private static final WingPoint[] WING_SHAPE = WingBuilder.create()
           .add(0.08f, 0.10f, 0.88f).add(0.28f, 0.34f, 0.78f).add(0.56f, 0.82f, 0.62f)
           .add(0.86f, 0.30f, 0.52f).add(1.14f, 0.46f, 0.40f).add(1.24f, 0.04f, 0.30f)
           .add(1.02f, -0.18f, 0.28f).add(1.18f, -0.64f, 0.22f).add(0.86f, -0.46f, 0.20f)
           .add(0.80f, -0.98f, 0.14f).add(0.54f, -0.74f, 0.16f).add(0.30f, -1.16f, 0.12f)
           .add(0.10f, -0.54f, 0.18f).build();

   private final BooleanSetting otherPlayers = new BooleanSetting("Other players", false);
   private final NumberSetting wingSize = new NumberSetting("Size", 1.0F, 0.8F, 1.5F, 0.05F);
   private final NumberSetting opacity = new NumberSetting("Opacity", 0.4F, 0.1F, 1.0F, 0.01F);

   @EventTarget
   public void onWorld(EventRender3D event) {
      if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;

      MatrixStack matrixStack = event.getMatrix();
      float deltaTracker = event.getPartialTicks();

      Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();

      matrixStack.push();
      matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      if (!mc.options.getPerspective().isFirstPerson() && mc.player.isAlive() && !mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
         renderWings(matrixStack, mc.player, deltaTracker);
      }

      if (otherPlayers.isEnabled()) {
         for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity targetPlayer) || targetPlayer == mc.player) continue;
            if (AntiBot.INSTANCE.isBot(targetPlayer)) continue;
            if (!targetPlayer.isAlive() || targetPlayer.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA))
               continue;

            renderWings(matrixStack, targetPlayer, deltaTracker);
         }
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();

      RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);

      RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

      matrixStack.pop();
   }

   void renderWings(MatrixStack matrixStack, PlayerEntity targetPlayer, float deltaTracker) {
      WingAnimationState state = animationStates.computeIfAbsent(targetPlayer.getUuid(), uuid -> new WingAnimationState());

      double interpolatedX = MathHelper.lerp(deltaTracker, targetPlayer.prevX, targetPlayer.getX());
      double interpolatedY = MathHelper.lerp(deltaTracker, targetPlayer.prevY, targetPlayer.getY());
      double interpolatedZ = MathHelper.lerp(deltaTracker, targetPlayer.prevZ, targetPlayer.getZ());

      float currentBodyYaw = yaw(targetPlayer, deltaTracker, state);

      float move = MathHelper.clamp(targetPlayer.limbAnimator.getSpeed(deltaTracker), 0f, 1f);

      float targetWaterTransition = targetPlayer.isTouchingWater() ? 1f : 0f;

      state.waterAnim += (targetWaterTransition - state.waterAnim) * 0.08f;

      float motionX = (float) (targetPlayer.getX() - targetPlayer.prevX);
      float motionZ = (float) (targetPlayer.getZ() - targetPlayer.prevZ);

      float bodyYawRad = (float) Math.toRadians(currentBodyYaw);

      float targetForward = (float) (-(motionX * Math.sin(bodyYawRad)) + (motionZ * Math.cos(bodyYawRad)));

      targetForward = MathHelper.clamp(targetForward * 22f, -1f, 1f);

      state.forwardAnim += (targetForward - state.forwardAnim) * 0.08f;

      WingPose flightPose = pose(targetPlayer, deltaTracker);

      float waterPitch = 25f;
      float waterScale = 0.9f;
      float waterOpen = 0.85f;

      flightPose.pitchRotation += (waterPitch - flightPose.pitchRotation) * state.waterAnim;
      flightPose.scaleFactor += (waterScale - flightPose.scaleFactor) * state.waterAnim;
      flightPose.opennessMultiplier += (waterOpen - flightPose.opennessMultiplier) * state.waterAnim;

      float targetFlapStrength = flightPose.flapStrength + (move * 6f);

      state.flapAnim += (targetFlapStrength - state.flapAnim) * 0.08f;

      float flapAngle = (float) Math.sin((targetPlayer.age + deltaTracker) * flightPose.flapFrequency) * state.flapAnim;

      float spread = (8 + flapAngle + move * flightPose.motionSpreadBonus * 1.8f) * flightPose.opennessMultiplier;

      float motionSpread = state.forwardAnim * 16f;

      float dynamicSpread = spread + motionSpread;

      float fWingScale = wingSize.getCurrent() * flightPose.scaleFactor;

      int highlightColor = ColorUtil.fade(0);
      int glowShade = interpolateColor(highlightColor, rgb(255, 255, 255), 0.28f);
      int coreShade = interpolateColor(highlightColor, rgb(255, 255, 255), 0.55f);

      matrixStack.push();

      matrixStack.translate(interpolatedX, interpolatedY + targetPlayer.getHeight() * 0.75f, interpolatedZ);

      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-currentBodyYaw));

      matrixStack.translate(0f, 0f, -0.23f);

      matrixStack.scale(fWingScale, fWingScale, fWingScale);

      if (flightPose.pitchRotation != 0f) {
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(flightPose.pitchRotation));
      }

      if (flightPose.rollRotation != 0f) {
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(flightPose.rollRotation));
      }

      renderWing(matrixStack, -1f, dynamicSpread, highlightColor, glowShade, coreShade, highlightColor, flightPose, state);

      renderWing(matrixStack, 1f, dynamicSpread, highlightColor, glowShade, coreShade, highlightColor, flightPose, state);

      matrixStack.pop();
   }

   void renderWing(MatrixStack matrixStack, float direction, float wingOpenness, int primaryColor, int glowColor, int coreColor, int outlineColor, WingPose wingPose, WingAnimationState state) {
      matrixStack.push();

      matrixStack.translate(direction * wingPose.sideOffset, wingPose.sideVerticalOffset, wingPose.sideDepthOffset);

      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * wingOpenness));

      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * wingPose.sideRollAngle));

      float dynamicPitch = wingPose.sidePitchAngle + (state.forwardAnim * 10f);

      int opacityValue = 220;

      matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(dynamicPitch));

      RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

      drawWing(matrixStack, direction, setAlpha(glowColor, (int) (opacityValue * 0.25f)));

      drawWing(matrixStack, direction, setAlpha(coreColor, (int) (opacityValue * 0.30f)));

      RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

      drawWing(matrixStack, direction, setAlpha(primaryColor, opacityValue));

      RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);

      drawLines(matrixStack, direction, setAlpha(outlineColor, (int) (opacityValue * 0.6f)), setAlpha(glowColor, (int) (opacityValue * 0.20f)));

      matrixStack.pop();
   }

   void drawWing(MatrixStack matrixStack, float direction, int surfaceColor) {
      org.joml.Matrix4f transformMatrix = matrixStack.peek().getPositionMatrix();

      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

      float red = ColorUtil.red(surfaceColor);
      float green = ColorUtil.green(surfaceColor);
      float blue = ColorUtil.blue(surfaceColor);

      float baseAlphaValue = (ColorUtil.alpha(surfaceColor) / 255f) * opacity.getCurrent();

      float centerX = 0f;
      float centerY = 0f;

      for (WingPoint point : WING_SHAPE) {
         centerX += point.x;
         centerY += point.y;
      }

      centerX /= WING_SHAPE.length;
      centerY /= WING_SHAPE.length;

      bufferBuilder.vertex(transformMatrix, direction * centerX, centerY, 0f).color(red / 255f, green / 255f, blue / 255f, baseAlphaValue);

      float maxDistanceY = 0f;

      for (WingPoint point : WING_SHAPE) {
         maxDistanceY = Math.max(maxDistanceY, Math.abs(point.y - centerY));
      }

      for (WingPoint point : WING_SHAPE) {
         float vertexX = direction * point.x;
         float vertexY = point.y;

         float distanceFromCenter = Math.abs(point.y - centerY);

         float normalizedDistance = distanceFromCenter / maxDistanceY;

         float fadeFactor = normalizedDistance * normalizedDistance * (3f - 2f * normalizedDistance);

         float vertexAlpha = baseAlphaValue * (1f - fadeFactor);

         bufferBuilder.vertex(transformMatrix, vertexX, vertexY, 0f).color(red / 255f, green / 255f, blue / 255f, vertexAlpha);
      }

      WingPoint firstPoint = WING_SHAPE[0];

      float firstPointDistance = Math.abs(firstPoint.y - centerY);

      float firstPointNormalized = firstPointDistance / maxDistanceY;

      float firstPointFade = firstPointNormalized * firstPointNormalized * (3f - 2f * firstPointNormalized);

      float firstPointAlpha = baseAlphaValue * (1f - firstPointFade);

      bufferBuilder.vertex(transformMatrix, direction * firstPoint.x, firstPoint.y, 0f).color(red / 255f, green / 255f, blue / 255f, firstPointAlpha);

      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
   }

   void drawLines(MatrixStack matrixStack, float direction, int outlineColor, int ribColor) {
      org.joml.Matrix4f transformMatrix = matrixStack.peek().getPositionMatrix();

      RenderSystem.lineWidth(1.35f);

      BufferBuilder outlineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

      for (WingPoint point : WING_SHAPE) {
         outlineBuffer.vertex(transformMatrix, direction * point.x, point.y, 0f).color(ColorUtil.red(outlineColor) / 255f, ColorUtil.green(outlineColor) / 255f, ColorUtil.blue(outlineColor) / 255f, ColorUtil.alpha(outlineColor) / 255f);
      }

      outlineBuffer.vertex(transformMatrix, direction * WING_SHAPE[0].x, WING_SHAPE[0].y, 0f).color(ColorUtil.red(outlineColor) / 255f, ColorUtil.green(outlineColor) / 255f, ColorUtil.blue(outlineColor) / 255f, ColorUtil.alpha(outlineColor) / 255f);

      BufferRenderer.drawWithGlobalProgram(outlineBuffer.end());

      RenderSystem.lineWidth(0.9f);

      BufferBuilder ribsBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

      for (int ribIndex : RIB_BONE) {
         WingPoint ribPoint = WING_SHAPE[ribIndex];

         ribsBuffer.vertex(transformMatrix, 0f, 0f, 0f).color(ColorUtil.red(ribColor) / 255f, ColorUtil.green(ribColor) / 255f, ColorUtil.blue(ribColor) / 255f, Math.max(8, (int) (ColorUtil.alpha(ribColor) * 0.75f)) / 255f);

         ribsBuffer.vertex(transformMatrix, direction * ribPoint.x * 0.96f, ribPoint.y * 0.96f, 0f).color(ColorUtil.red(ribColor) / 255f, ColorUtil.green(ribColor) / 255f, ColorUtil.blue(ribColor) / 255f, (int) (ColorUtil.alpha(ribColor) * ribPoint.alphaMul) / 255f);
      }

      BufferRenderer.drawWithGlobalProgram(ribsBuffer.end());
   }

   float yaw(PlayerEntity targetPlayer, float deltaTracker, WingAnimationState state) {

      float targetAngle = MathHelper.lerpAngleDegrees(deltaTracker, targetPlayer.prevBodyYaw, targetPlayer.bodyYaw);

      if (!state.yawInitialized || targetPlayer.age < 2) {
         state.smoothYaw = targetAngle;
         state.yawInitialized = true;
         return state.smoothYaw;
      }

      float angleDelta = MathHelper.wrapDegrees(targetAngle - state.smoothYaw);
      angleDelta = MathHelper.clamp(angleDelta, -14f, 14f);
      state.smoothYaw = state.smoothYaw + angleDelta;

      return state.smoothYaw;
   }

   WingPose pose(PlayerEntity targetPlayer, float deltaTracker) {
      float currentPitch = MathHelper.lerp(deltaTracker, targetPlayer.prevPitch, targetPlayer.getPitch());

      if (targetPlayer.isGliding()) {
         float glidingDuration = (float) targetPlayer.getGlidingTicks() + deltaTracker;

         float glideProgress = MathHelper.clamp(glidingDuration * glidingDuration / 100f, 0f, 1f);

         float pitchAngle = glideProgress * (-90f - currentPitch);

         return new WingPose(0.34f, 0.46f, 0f, 0f, pitchAngle, 0f, 0.76f, 0.92f, 0.10f, 0.58f, 0.05f, 0.06f, -5f, -2f, 0.13f);
      }

      if (targetPlayer.isSneaking()) {
         return new WingPose(0f, 0f, 0.96f, 0.10f, 18f, 0f, 1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
      }

      return new WingPose(0f, 0f, 1.38f, 0.10f, 0f, 0f, 1f, 1f, 0.18f, 4.5f, 0.06f, 0.02f, -11f, -4f, 0.12f);
   }

   @Override
   public void onDisable() {
      animationStates.clear();
      super.onDisable();
   }

   private static int rgb(int r, int g, int b) {
      return 255 << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
   }

   private static int setAlpha(int color, int alpha) {
      return MathHelper.clamp(alpha, 0, 255) << 24 | color & 0xFFFFFF;
   }

   private static int interpolateColor(int color1, int color2, float amount) {
      int r = (int) MathHelper.lerp(amount, ColorUtil.red(color1), ColorUtil.red(color2));
      int g = (int) MathHelper.lerp(amount, ColorUtil.green(color1), ColorUtil.green(color2));
      int b = (int) MathHelper.lerp(amount, ColorUtil.blue(color1), ColorUtil.blue(color2));
      int a = (int) MathHelper.lerp(amount, ColorUtil.alpha(color1), ColorUtil.alpha(color2));
      return a << 24 | r << 16 | g << 8 | b;
   }

   record WingPoint(float x, float y, float alphaMul) {
   }

   static class WingAnimationState {
      float smoothYaw;
      boolean yawInitialized;

      float forwardAnim;
      float flapAnim;
      float waterAnim;
   }

   static class WingPose {
      float preShiftY, preShiftZ;
      float anchorY, anchorZ;
      float pitchRotation, rollRotation;
      float opennessMultiplier, scaleFactor;
      float motionSpreadBonus, flapStrength;
      float sideOffset, sideVerticalOffset, sideDepthOffset;
      float sideRollAngle, sidePitchAngle, flapFrequency;

      WingPose(float preShiftY, float preShiftZ, float anchorY, float anchorZ, float pitchRotation, float rollRotation, float opennessMultiplier, float scaleFactor, float motionSpreadBonus, float flapStrength, float sideOffset, float sideDepthOffset, float sideRollAngle, float sidePitchAngle, float flapFrequency) {
         this(preShiftY, preShiftZ, anchorY, anchorZ, pitchRotation, rollRotation, opennessMultiplier, scaleFactor, motionSpreadBonus, flapStrength, sideOffset, 0f, sideDepthOffset, sideRollAngle, sidePitchAngle, flapFrequency);
      }

      WingPose(float preShiftY, float preShiftZ, float anchorY, float anchorZ, float pitchRotation, float rollRotation, float opennessMultiplier, float scaleFactor, float motionSpreadBonus, float flapStrength, float sideOffset, float sideVerticalOffset, float sideDepthOffset, float sideRollAngle, float sidePitchAngle, float flapFrequency) {

         this.preShiftY = preShiftY;
         this.preShiftZ = preShiftZ;
         this.anchorY = anchorY;
         this.anchorZ = anchorZ;
         this.pitchRotation = pitchRotation;
         this.rollRotation = rollRotation;
         this.opennessMultiplier = opennessMultiplier;
         this.scaleFactor = scaleFactor;
         this.motionSpreadBonus = motionSpreadBonus;
         this.flapStrength = flapStrength;
         this.sideOffset = sideOffset;
         this.sideVerticalOffset = sideVerticalOffset;
         this.sideDepthOffset = sideDepthOffset;
         this.sideRollAngle = sideRollAngle;
         this.sidePitchAngle = sidePitchAngle;
         this.flapFrequency = flapFrequency;
      }
   }

   static final class WingBuilder {
      final List<WingPoint> points = new ArrayList<>();

      public static WingBuilder create() {
         return new WingBuilder();
      }

      public WingBuilder add(float x, float y, float alpha) {
         points.add(new WingPoint(x, y, alpha));
         return this;
      }

      public WingPoint[] build() {
         return points.toArray(WingPoint[]::new);
      }
   }
}
