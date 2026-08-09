package tech.huihui.client.modules.impl.render;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.model.SkeletonEntityModel;
import net.minecraft.client.render.entity.model.ZombieEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.render.model.ProceduralModel;

@ModuleAnnotation(
   name = "CustomModel",
   category = Category.RENDER,
   description = "Кастомные модели и анимации игроков"
)
public final class CustomModel extends Module {
   public static final CustomModel INSTANCE = new CustomModel();

   private static final Identifier ZOMBIE_TEXTURE = Identifier.ofVanilla("textures/entity/zombie/zombie.png");
   private static final Identifier SKELETON_TEXTURE = Identifier.ofVanilla("textures/entity/skeleton/skeleton.png");
   private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

   public final ModeSetting model = new ModeSetting("Модель", new String[]{
      "Тунг Тунг Сахура", "Белирину", "Капучино", "Зомби", "Скелет", "Crazy Rabbit",
      "Куб", "Кубик", "Шар", "Слизь", "Гигант", "Мини", "Робот", "Прыгун", "Волчок"
   });
   public final BooleanSetting friendsOnly = new BooleanSetting("На друзьях", false);
   public final BooleanSetting self = new BooleanSetting("На себе", true);
   public final BooleanSetting figuraIntegration = new BooleanSetting("Интеграция с Figura", false);

   private ZombieEntityModel zombieModel;
   private SkeletonEntityModel skeletonModel;
   private LoadedEntityModels cachedModels;
   private ProceduralModel sahurModel;
   private ProceduralModel rabbitModelProc;

   private static final int ARGB_WHITE = 0xFFFFFFFF;
   private static final int ARGB_WOOD = 0xFF9C6B42;
   private static final int ARGB_WOOD_DARK = 0xFF6E4A2E;
   private static final int ARGB_WOOD_LIGHT = 0xFFB08050;
   private static final int ARGB_BLACK = 0xFF000000;
   private static final int ARGB_RED = 0xFFFF3333;
   private static final int ARGB_PINK = 0xFFF0A0A0;

   private CustomModel() {
      this.figuraIntegration.setOnToggle(this::handleFiguraToggle);
   }

   private void handleFiguraToggle() {
      if (!this.figuraIntegration.isEnabled()) {
         return;
      }
      if (isFiguraLoaded()) {
         return;
      }
      this.figuraIntegration.setEnabled(false);
      this.warnFiguraMissing();
   }

   public static boolean isFiguraLoaded() {
      return FabricLoader.getInstance().isModLoaded("figura");
   }

   private void warnFiguraMissing() {
      if (mc.player == null) {
         return;
      }
      String url = "https://github.com/FiguraMC/Figura/releases/download/0.1.6-rc.3/figura-0.1.6-rc.3+1.21.4-fabric-mc.jar";
      net.minecraft.text.MutableText message = net.minecraft.text.Text.literal("У вас нет мода Figura! Скачать можно тут: ");
      net.minecraft.text.MutableText link = net.minecraft.text.Text.literal("Скачать Figura");
      link.setStyle(net.minecraft.text.Style.EMPTY.withColor(net.minecraft.util.Formatting.AQUA).withUnderline(true)
         .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.OPEN_URL, url)));
      mc.player.sendMessage(message.append(link), false);
   }

   public boolean render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, PlayerEntityRenderState state) {
      if (!this.isEnabled() || matrices == null || vertexConsumers == null || state == null) {
         return false;
      }
      if (!this.shouldApply(state.name)) {
         return false;
      }
      switch (this.model.get()) {
      case "Тунг Тунг Сахура":
         this.ensureProceduralModels();
         this.renderSahur(matrices, vertexConsumers, light, overlay, state);
         return true;
      case "Зомби":
         this.ensureModels();
         this.renderBiped(matrices, vertexConsumers, light, overlay, state, true);
         return true;
      case "Скелет":
         this.ensureModels();
         this.renderBiped(matrices, vertexConsumers, light, overlay, state, false);
         return true;
      case "Crazy Rabbit":
         this.ensureProceduralModels();
         this.renderRabbitProc(matrices, vertexConsumers, light, overlay, state);
         return true;
      default:
         this.applyTransform(matrices, state);
         return false;
      }
   }

   private void applyTransform(MatrixStack matrices, PlayerEntityRenderState state) {
      float t = this.time();
      switch (this.model.get()) {
      case "Белирину":
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 40.0F % 360.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(6.0F));
         break;
      case "Капучино":
         float floatY = MathHelper.sin(t * 0.8F) * 0.18F;
         matrices.translate(0.0F, floatY, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(t * 0.4F) * 6.0F));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin(t * 0.2F) * 10.0F));
         break;
      case "Куб":
         matrices.scale(1.15F, 0.7F, 1.15F);
         matrices.translate(0.0F, 0.3F, 0.0F);
         break;
      case "Кубик":
         matrices.scale(0.6F, 0.45F, 0.6F);
         matrices.translate(0.0F, 0.5F, 0.0F);
         break;
      case "Шар":
         matrices.scale(1.4F, 0.7F, 1.4F);
         matrices.translate(0.0F, 0.25F, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 20.0F % 360.0F));
         break;
      case "Слизь":
         float squish = MathHelper.sin(t * 3.0F) * 0.08F;
         matrices.scale(1.0F + squish, 1.0F - squish * 1.4F, 1.0F + squish);
         break;
      case "Гигант":
         matrices.scale(1.6F, 1.6F, 1.6F);
         break;
      case "Мини":
         matrices.scale(0.5F, 0.5F, 0.5F);
         break;
      case "Робот":
         float robotBob = MathHelper.sin(t * 2.0F) * 0.05F;
         matrices.translate(0.0F, robotBob, 0.0F);
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(8.0F));
         break;
      case "Прыгун":
         float hop = Math.abs(MathHelper.sin(t * 2.0F));
         matrices.translate(0.0F, hop * 0.35F, 0.0F);
         matrices.scale(1.0F + hop * 0.1F, 1.0F - hop * 0.2F, 1.0F + hop * 0.1F);
         break;
      case "Волчок":
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 120.0F % 360.0F));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(15.0F));
         matrices.scale(0.8F, 1.2F, 0.8F);
         break;
      default:
         break;
      }
   }

   private void renderBiped(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, PlayerEntityRenderState state, boolean zombie) {
      BipedEntityModel<?> biped = zombie ? this.zombieModel : this.skeletonModel;
      this.animateBiped(biped, state, zombie);
      Identifier texture = zombie ? ZOMBIE_TEXTURE : SKELETON_TEXTURE;
      VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
      matrices.push();
      biped.render(matrices, buffer, light, overlay, 0xFFFFFFFF);
      matrices.pop();
   }

   private void animateBiped(BipedEntityModel<?> biped, PlayerEntityRenderState state, boolean zombie) {
      ModelPart head = biped.head;
      ModelPart body = biped.body;
      ModelPart rightArm = biped.rightArm;
      ModelPart leftArm = biped.leftArm;
      ModelPart rightLeg = biped.rightLeg;
      ModelPart leftLeg = biped.leftLeg;

      head.pitch = state.pitch * DEG_TO_RAD;
      head.yaw = state.yawDegrees * DEG_TO_RAD;
      head.roll = 0.0F;
      body.pitch = 0.0F;
      body.yaw = 0.0F;
      body.roll = 0.0F;

      float limb = state.limbFrequency;
      float amp = MathHelper.clamp(state.limbAmplitudeMultiplier, 0.0F, 1.0F);
      float walk = amp * 1.4F;

      leftLeg.pitch = MathHelper.cos(limb * 0.6662F + MathHelper.PI) * walk;
      rightLeg.pitch = MathHelper.cos(limb * 0.6662F) * walk;
      leftLeg.yaw = 0.0F;
      rightLeg.yaw = 0.0F;
      leftLeg.roll = 0.0F;
      rightLeg.roll = 0.0F;

      leftArm.pitch = MathHelper.cos(limb * 0.6662F) * walk;
      rightArm.pitch = MathHelper.cos(limb * 0.6662F + MathHelper.PI) * walk;
      leftArm.yaw = 0.0F;
      rightArm.yaw = 0.0F;
      leftArm.roll = 0.0F;
      rightArm.roll = 0.0F;

      float armsForward = zombie ? 1.3F : 0.8F;
      leftArm.pitch += armsForward;
      rightArm.pitch += armsForward;

      float swing = state.handSwingProgress;
      if (swing > 0.001F) {
         float root = MathHelper.sqrt(swing);
         float sin = MathHelper.sin(root * MathHelper.PI);
         float cos = MathHelper.cos(root * MathHelper.PI);
         rightArm.pitch = -2.4F * sin + 0.4F;
         rightArm.yaw = -0.5F * sin;
         rightArm.roll = 0.0F;
         leftArm.pitch = 0.6F * cos - 0.5F;
         leftArm.yaw = 0.0F;
         leftArm.roll = 0.0F;
      }

      if (biped.hat != null) {
         biped.hat.copyTransform(head);
      }
   }

   private void renderSahur(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, PlayerEntityRenderState state) {
      float t = this.time();
      float bob = MathHelper.sin(t * 1.2F) * 0.1F;
      float sway = MathHelper.sin(t * 0.7F) * 10.0F;

      ProceduralModel.Node body = this.sahurModel.node("body");
      ProceduralModel.Node head = this.sahurModel.node("head");
      ProceduralModel.Node leftArm = this.sahurModel.node("leftArm");
      ProceduralModel.Node rightArm = this.sahurModel.node("rightArm");
      ProceduralModel.Node leftLeg = this.sahurModel.node("leftLeg");
      ProceduralModel.Node rightLeg = this.sahurModel.node("rightLeg");
      ProceduralModel.Node stick = this.sahurModel.node("stick");

      body.setPitch(MathHelper.sin(t * 0.7F) * 0.05F);
      body.setYaw(sway * DEG_TO_RAD);
      head.setPitch(state.pitch * DEG_TO_RAD + MathHelper.sin(t * 0.7F) * 0.08F);
      head.setYaw(state.yawDegrees * DEG_TO_RAD - sway * DEG_TO_RAD);

      float armSwing = MathHelper.sin(t * 2.2F) * 0.6F;
      leftArm.setPitch(armSwing);
      rightArm.setPitch(-armSwing);
      stick.setPitch(-armSwing + 0.4F);

      float hop = Math.abs(MathHelper.sin(t * 1.6F));
      leftLeg.setPitch(hop * 0.4F);
      rightLeg.setPitch(-hop * 0.4F);

      matrices.push();
      matrices.translate(0.0F, bob, 0.0F);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sway * 0.5F));
      this.sahurModel.render(matrices, vertexConsumers, light, overlay);
      matrices.pop();
   }

   private void renderRabbitProc(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, PlayerEntityRenderState state) {
      float t = this.time();
      float hop = Math.abs(MathHelper.sin(t * 2.5F));
      float squish = MathHelper.sin(t * 3.0F) * 0.08F;

      ProceduralModel.Node head = this.rabbitModelProc.node("head");
      ProceduralModel.Node leftEar = this.rabbitModelProc.node("leftEar");
      ProceduralModel.Node rightEar = this.rabbitModelProc.node("rightEar");
      ProceduralModel.Node body = this.rabbitModelProc.node("body");
      ProceduralModel.Node tail = this.rabbitModelProc.node("tail");

      head.setPitch(state.pitch * DEG_TO_RAD + hop * 0.3F);
      head.setYaw(state.yawDegrees * DEG_TO_RAD);
      leftEar.setRoll(MathHelper.sin(t * 2.0F) * 0.1F);
      rightEar.setRoll(-MathHelper.sin(t * 2.0F) * 0.1F);
      body.setPitch(hop * 0.2F);
      tail.setPitch(hop * 0.5F);

      matrices.push();
      matrices.translate(0.0F, hop * 0.6F, 0.0F);
      matrices.scale(2.4F + squish, 2.4F - squish, 2.4F + squish);
      this.rabbitModelProc.render(matrices, vertexConsumers, light, overlay);
      matrices.pop();
   }

   private void ensureProceduralModels() {
      if (this.sahurModel == null) {
         this.sahurModel = buildSahurModel();
      }
      if (this.rabbitModelProc == null) {
         this.rabbitModelProc = buildRabbitModel();
      }
   }

   private static ProceduralModel buildSahurModel() {
      return ProceduralModel.builder()
         .part("body", 0.0F, 0.0F, 0.0F, -4.0F, 0.0F, -2.8F, 8.0F, 12.0F, 5.6F, ARGB_WOOD)
         .part("bodyBelt", 0.0F, 0.0F, 0.0F, -4.4F, 5.0F, -3.2F, 8.8F, 1.6F, 6.4F, ARGB_WOOD_DARK)
         .part("head", 0.0F, 0.0F, 0.0F, -3.5F, -8.0F, -4.0F, 7.0F, 8.0F, 8.0F, ARGB_WOOD_LIGHT)
         .part("headTop", 0.0F, 0.0F, 0.0F, -2.2F, -9.5F, -2.4F, 4.4F, 1.5F, 4.8F, ARGB_WOOD_DARK)
         .part("eyeLeft", -2.0F, -4.0F, -4.0F, -0.8F, -0.8F, -0.1F, 1.6F, 1.6F, 0.2F, ARGB_WHITE)
         .part("eyeRight", 2.0F, -4.0F, -4.0F, -0.8F, -0.8F, -0.1F, 1.6F, 1.6F, 0.2F, ARGB_WHITE)
         .part("pupilLeft", -2.0F, -4.0F, -3.9F, -0.45F, -0.45F, -0.3F, 0.9F, 0.9F, 0.6F, ARGB_BLACK)
         .part("pupilRight", 2.0F, -4.0F, -3.9F, -0.45F, -0.45F, -0.3F, 0.9F, 0.9F, 0.6F, ARGB_BLACK)
         .part("mouth", 0.0F, -1.2F, -4.0F, -1.8F, -0.3F, -0.1F, 3.6F, 0.6F, 0.2F, ARGB_WOOD_DARK)
         .part("leftArm", 6.0F, 2.0F, 0.0F, -1.4F, -1.0F, -1.4F, 2.8F, 10.0F, 2.8F, ARGB_WOOD)
         .part("rightArm", -6.0F, 2.0F, 0.0F, -1.4F, -1.0F, -1.4F, 2.8F, 10.0F, 2.8F, ARGB_WOOD)
         .part("leftLeg", 3.0F, 12.0F, 0.0F, -1.6F, 0.0F, -1.6F, 3.2F, 12.0F, 3.2F, ARGB_WOOD_DARK)
         .part("rightLeg", -3.0F, 12.0F, 0.0F, -1.6F, 0.0F, -1.6F, 3.2F, 12.0F, 3.2F, ARGB_WOOD_DARK)
         .part("stick", -6.5F, 10.0F, 2.0F, -0.4F, -4.0F, -0.4F, 0.8F, 10.0F, 0.8F, ARGB_WOOD_DARK)
         .build();
   }

   private static ProceduralModel buildRabbitModel() {
      return ProceduralModel.builder()
         .part("body", 0.0F, 19.0F, 8.0F, -3.0F, -1.5F, -9.0F, 6.0F, 5.0F, 10.0F, ARGB_WHITE)
         .part("chest", 0.0F, 18.0F, 4.0F, -2.8F, -2.0F, -3.0F, 5.6F, 5.0F, 6.0F, ARGB_WHITE)
         .part("head", 0.0F, 16.0F, -1.0F, -2.5F, -4.0F, -5.0F, 5.0F, 5.0F, 5.0F, ARGB_WHITE)
         .part("leftEar", 0.0F, 16.0F, -1.0F, 0.5F, -9.0F, -1.0F, 2.0F, 7.0F, 1.0F, ARGB_WHITE)
         .part("rightEar", 0.0F, 16.0F, -1.0F, -2.5F, -9.0F, -1.0F, 2.0F, 7.0F, 1.0F, ARGB_WHITE)
         .part("earLeftInner", 0.0F, 16.0F, -1.0F, 0.7F, -8.0F, -0.4F, 0.8F, 5.0F, 0.4F, ARGB_PINK)
         .part("earRightInner", 0.0F, 16.0F, -1.0F, -1.5F, -8.0F, -0.4F, 0.8F, 5.0F, 0.4F, ARGB_PINK)
         .part("eyeLeft", -1.4F, 14.0F, -5.5F, -0.55F, -0.55F, -0.1F, 1.1F, 1.1F, 0.2F, ARGB_RED)
         .part("eyeRight", 1.4F, 14.0F, -5.5F, -0.55F, -0.55F, -0.1F, 1.1F, 1.1F, 0.2F, ARGB_RED)
         .part("tooth", 0.0F, 12.5F, -5.5F, -0.7F, -0.8F, -0.1F, 1.4F, 1.6F, 0.2F, ARGB_WHITE)
         .part("leftFrontLeg", 3.0F, 17.0F, -1.0F, -1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, ARGB_WHITE)
         .part("rightFrontLeg", -3.0F, 17.0F, -1.0F, -1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, ARGB_WHITE)
         .part("leftHaunch", 3.0F, 17.5F, 3.7F, -1.0F, 0.0F, 0.0F, 2.0F, 4.0F, 5.0F, ARGB_WHITE)
         .part("rightHaunch", -3.0F, 17.5F, 3.7F, -1.0F, 0.0F, 0.0F, 2.0F, 4.0F, 5.0F, ARGB_WHITE)
         .part("tail", 0.0F, 20.0F, 7.0F, -1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 2.0F, ARGB_WHITE)
         .build();
   }

   private void ensureModels() {
      LoadedEntityModels models = mc.getLoadedEntityModels();
      if (models == this.cachedModels && this.zombieModel != null) {
         return;
      }
      this.cachedModels = models;
      this.zombieModel = new ZombieEntityModel(models.getModelPart(EntityModelLayers.ZOMBIE));
      this.skeletonModel = new SkeletonEntityModel(models.getModelPart(EntityModelLayers.SKELETON));
   }

   private boolean shouldApply(String name) {
      if (name == null || name.isEmpty()) {
         return false;
      }
      boolean isSelf = mc.player != null && name.equalsIgnoreCase(mc.player.getGameProfile().getName());
      if (isSelf) {
         return this.self.isEnabled();
      }
      if (this.friendsOnly.isEnabled() && !HuihuiClient.getInstance().getFriendManager().isFriend(name)) {
         return false;
      }
      return true;
   }

   private float time() {
      if (mc.world == null) {
         return 0.0F;
      }
      return (float) mc.world.getTime() + mc.getRenderTickCounter().getTickDelta(true);
   }
}
