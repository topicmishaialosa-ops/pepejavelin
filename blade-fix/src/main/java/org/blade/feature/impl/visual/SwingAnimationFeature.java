package org.blade.feature.impl.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;

public final class SwingAnimationFeature extends Feature {
    public static final SwingAnimationFeature INSTANCE = new SwingAnimationFeature();
    public static float previewProgress = -1.0F;

    public final ModeSetting animationMode = register(new ModeSetting("Режим", "HMI Взмахи",
            "HMI Взмахи", "HMI Вперед", "HMI Обычная", "HMI Копье", "HMI Инструмент", "HMI Блок", "HMI Лопата",
            "Smooth", "Self", "Self2", "Down", "Forward", "Touch", "Pander", "Curt", "BlockHit", "Spin",
            "Backhand", "Overhead", "Stab", "Slash", "Reverse", "Flick", "Whip", "Rush", "Upswing", "Hammer",
            "Tornado", "Rotor", "Bounce", "Jab", "Cross", "Power", "Ghost", "Shake", "Blink", "ReverseSpin",
            "Swing360", "Flip", "Boomerang", "Cannon", "Pierce", "Wave", "Claw", "Wide", "Aim", "Precise",
            "Drop", "Vertical", "Custom"));
    public final NumberSetting swingPower = register(new NumberSetting("Сила", 5.0D, 1.0D, 10.0D, 1.0D, ""));
    public final NumberSetting speed = register(new NumberSetting("Скорость", 7.0D, 0.0D, 20.0D, 1.0D, ""));
    public final NumberSetting angle = register(new NumberSetting("Угол", 0.0D, 0.0D, 360.0D, 1.0D, "")
            .visibleWhen(() -> this.animationMode.is("Self") || this.animationMode.is("Self2")
                    || this.animationMode.is("Aim") || this.animationMode.is("Precise")));
    public final NumberSetting rightX = register(new NumberSetting("Правая-X", 0.0D, -2.0D, 2.0D, 0.05D, ""));
    public final NumberSetting rightY = register(new NumberSetting("Правая-Y", 0.0D, -2.0D, 2.0D, 0.05D, ""));
    public final NumberSetting rightZ = register(new NumberSetting("Правая-Z", 0.0D, -2.0D, 2.0D, 0.05D, ""));
    public final NumberSetting leftX = register(new NumberSetting("Левая-X", 0.0D, -2.0D, 2.0D, 0.05D, ""));
    public final NumberSetting leftY = register(new NumberSetting("Левая-Y", 0.0D, -2.0D, 2.0D, 0.05D, ""));
    public final NumberSetting leftZ = register(new NumberSetting("Левая-Z", 0.0D, -2.0D, 2.0D, 0.05D, ""));
    public final NumberSetting customStartX = register(new NumberSetting("Старт X", 0.0D, -180.0D, 180.0D, 1.0D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customStartY = register(new NumberSetting("Старт Y", 0.0D, -180.0D, 180.0D, 1.0D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customStartZ = register(new NumberSetting("Старт Z", 0.0D, -180.0D, 180.0D, 1.0D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customEndX = register(new NumberSetting("Конец X", -90.0D, -180.0D, 180.0D, 1.0D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customEndY = register(new NumberSetting("Конец Y", 0.0D, -180.0D, 180.0D, 1.0D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customEndZ = register(new NumberSetting("Конец Z", 0.0D, -180.0D, 180.0D, 1.0D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customPosX = register(new NumberSetting("Позиция X", 0.0D, -3.0D, 3.0D, 0.01D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customPosY = register(new NumberSetting("Позиция Y", 0.0D, -3.0D, 3.0D, 0.01D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customPosZ = register(new NumberSetting("Позиция Z", 0.0D, -3.0D, 3.0D, 0.01D, "").visibleWhen(this::isCustomMode));
    public final NumberSetting customScale = register(new NumberSetting("Масштаб", 1.0D, 0.1D, 3.0D, 0.01D, "").visibleWhen(this::isCustomMode));
    public final ModeSetting customCurve = register(new ModeSetting("Кривая", "Линейная", "Линейная", "Плавная"));
    public final ButtonSetting openEditorButton = register(new ButtonSetting("Открыть редактор анимации", "Открыть", SwingEditorScreen::openEditor));
    public final BooleanSetting showHands = register(new BooleanSetting("Показать руки", false));

    private SwingAnimationFeature() {
        super("SwingAnimation", "Кастомные анимации удара", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    private boolean isCustomMode() {
        return this.animationMode.is("Custom");
    }

    public boolean isPreviewing() {
        return SwingAnimationFeature.previewProgress >= 0.0F;
    }

    public float getPreviewProgress() {
        return SwingAnimationFeature.previewProgress;
    }

    public void setPreviewProgress(float progress) {
        SwingAnimationFeature.previewProgress = Mth.clamp(progress, 0.0F, 1.0F);
    }

    public void stopPreview() {
        SwingAnimationFeature.previewProgress = -1.0F;
    }

    public static boolean isEditorOpen() {
        return Minecraft.getInstance().gui.screen() instanceof SwingEditorScreen;
    }

    public void applyEditorPosition(PoseStack poseStack, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || arm != mc.player.getMainArm()) {
            return;
        }
        poseStack.translate(-0.56F, 0.52F, 0.25F);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public void renderSwordAnimation(PoseStack poseStack, float swingProgress, float equipProgress, HumanoidArm arm) {
        if (this.isPreviewing()) {
            swingProgress = this.getPreviewProgress();
        } else {
            float speedFactor = (float) Math.max(this.speed.getValue(), 0.1D) / 7.0F;
            swingProgress = Mth.clamp(swingProgress * speedFactor, 0.0F, 1.0F);
        }
        if (arm == HumanoidArm.RIGHT) {
            poseStack.translate(this.rightX.getValue().floatValue(), this.rightY.getValue().floatValue(), this.rightZ.getValue().floatValue());
        } else {
            poseStack.translate(this.leftX.getValue().floatValue(), this.leftY.getValue().floatValue(), this.leftZ.getValue().floatValue());
        }
        float anim = (float) Math.sin((double) swingProgress * 1.5707963267948966D * 2.0D);
        float sin2 = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
        float power = this.swingPower.getValue().floatValue();
        float ang = this.angle.getValue().floatValue();
        String mode = this.animationMode.getValue();
        float f;
        float g;
        float sinExtra;
        switch (mode) {
            case "HMI Взмахи": {
                float swingRot = hmiSwingRot(swingProgress);
                float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.8F * swingRot, 0.3F * swingRot, -0.5F * swing);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-70.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * swing));
                break;
            }
            case "HMI Вперед": {
                float swingRot = hmiSwingRot(swingProgress);
                float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.12F * swingRot, 0.04F * swingRot, -0.95F * swing);
                poseStack.translate(0.02F * swing, 0.1F * swing, -0.1F * swingRot);
                poseStack.mulPose(Axis.YP.rotationDegrees(8.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(32.0F * swing));
                break;
            }
            case "HMI Обычная": {
                float swingRot = hmiSwingRot(swingProgress);
                float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.1F * swingRot, 0.1F * swingRot, -0.1F * swing);
                poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * swing));
                poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * swing));
                break;
            }
            case "HMI Копье": {
                float swingRot = hmiSwingRot(swingProgress);
                float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.0F, 0.0F, 0.45F * swingRot);
                poseStack.translate(-0.25F * swing, -0.35F * swingRot, -0.6F * swing);
                poseStack.translate(0.0F, 0.1F * swing, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F * swingRot));
                break;
            }
            case "HMI Инструмент": {
                float swingRot = hmiSwingRot(swingProgress);
                float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.1F * swingRot, 0.1F * swingRot, -0.5F * swing);
                poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * swing));
                break;
            }
            case "HMI Блок": {
                float swingRot = hmiSwingRot(swingProgress);
                float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.1F * swingRot, 0.1F * swingRot, -0.2F * swing);
                poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(20.0F * swing));
                break;
            }
            case "HMI Лопата": {
                float swingRot = hmiSwingRot(swingProgress);
                float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.0F, 0.15F * swingRot, -0.25F * swingRot);
                poseStack.translate(0.0F, 0.0F, -0.2F * swing);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
                break;
            }
            case "Smooth":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                f = power * 10.0F;
                g = Mth.sin(swingProgress * swingProgress * 3.1415927F);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + g * (-f / 4.0F)));
                sinExtra = Mth.sin(Mth.sqrt(swingProgress * swingProgress) * 3.1415927F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(sinExtra * -(f / 4.0F)));
                poseStack.mulPose(Axis.XP.rotationDegrees(sinExtra * -f));
                poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
                break;
            case "Self2":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-30.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-ang - power * 10.0F * anim));
                break;
            case "Forward":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                f = 35.0F;
                poseStack.translate(0.0F, 0.0F, -0.3F * sin2);
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2 * f));
                break;
            case "Self":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-60.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-ang - power * 10.0F * anim));
                break;
            case "Down":
                poseStack.translate(0.56F, -0.52F - anim * power / 24.0F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-30.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                break;
            case "Touch":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.scale(1.0F, 1.0F, 1.0F + anim * power / 4.0F);
                poseStack.translate(0.0F, 0.0F, -0.265F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
                break;
            case "Curt":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                f = Mth.sqrt(swingProgress);
                g = Mth.sin(f * 3.1415927F);
                sinExtra = Mth.sin(swingProgress * 3.1415927F);
                poseStack.translate(0.4F - g * 0.2F, -0.2F + g * 0.3F, -0.5F - sinExtra * 0.2F);
                poseStack.mulPose(Axis.YP.rotationDegrees(91.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F + g * -100.0F * power / 5.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-60.0F * power / 5.0F));
                break;
            case "Pander":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.scale(0.8F, 0.8F, 0.8F);
                f = 1.0F - Mth.clamp(equipProgress, 0.0F, 1.0F);
                poseStack.translate(0.3F - anim * 0.15F, 0.2F - f * 0.12F, -0.15F - anim * 0.13F);
                poseStack.mulPose(Axis.YP.rotationDegrees(76.0F - 10.0F * anim * power / 5.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-16.0F - 8.0F * anim * power / 5.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-83.0F - 26.0F * anim * power / 5.0F));
                break;
            case "BlockHit":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                f = Mth.sin(swingProgress * swingProgress * 3.141592653589793D);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                g = Mth.sin(Mth.sqrt(swingProgress) * 3.141592653589793D);
                poseStack.mulPose(Axis.XP.rotationDegrees(f * -20.0F * power / 5.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(g * -20.0F * power / 5.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(g * -80.0F * power / 5.0F));
                poseStack.translate(0.4F, 0.2F, 0.2F);
                poseStack.translate(-0.5F, 0.08F, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(20.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F * power / 5.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(20.0F));
                break;
            case "Spin":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim * 360.0F * power / 5.0F));
                break;
            case "Backhand":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(anim * -160.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(25.0F));
                break;
            case "Overhead":
                poseStack.translate(0.56F, -0.52F + anim * 0.35F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-130.0F + anim * 80.0F));
                break;
            case "Stab":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.0F, 0.0F, -sin2 * 0.55F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                break;
            case "Slash":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2 * -90.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -45.0F));
                break;
            case "Reverse":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                f = power * 10.0F;
                g = Mth.sin(swingProgress * swingProgress * 3.1415927F);
                poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F + g * (f / 4.0F)));
                sinExtra = Mth.sin(Mth.sqrt(swingProgress) * 3.1415927F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(sinExtra * (f / 4.0F)));
                poseStack.mulPose(Axis.XP.rotationDegrees(sinExtra * f));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                break;
            case "Flick":
                poseStack.translate(0.56F, -0.52F, -0.72F + anim * 0.2F);
                poseStack.mulPose(Axis.XP.rotationDegrees(anim * -120.0F));
                break;
            case "Whip":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim * 130.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(anim * -60.0F));
                break;
            case "Rush":
                poseStack.translate(0.56F, -0.52F, -0.72F - sin2 * 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + sin2 * 25.0F));
                break;
            case "Upswing":
                poseStack.translate(0.56F, -0.52F - sin2 * 0.45F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -160.0F));
                break;
            case "Hammer":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-170.0F + anim * 90.0F));
                poseStack.scale(1.0F, 1.0F + anim * 0.15F, 1.0F);
                break;
            case "Tornado":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim * 360.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2 * -30.0F));
                break;
            case "Rotor":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(anim * 360.0F));
                break;
            case "Bounce":
                poseStack.translate(0.56F, -0.52F - sin2 * 0.25F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-30.0F + sin2 * -60.0F));
                break;
            case "Jab":
                poseStack.translate(0.56F, -0.52F, -0.72F - sin2 * 0.6F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F));
                break;
            case "Cross":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                g = Mth.sin(swingProgress * swingProgress * 3.1415927F);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + g * -power));
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -100.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
                break;
            case "Power":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.scale(1.0F + anim * 0.1F, 1.0F + anim * 0.1F, 1.0F + anim * 0.1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim * 70.0F * power / 5.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
                break;
            case "Ghost":
                poseStack.translate(0.56F, -0.52F - anim * 0.15F, -0.72F + anim * 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim * 180.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-60.0F));
                break;
            case "Shake":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * 40.0F * power / 5.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * -25.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2 * 20.0F));
                break;
            case "Blink":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                {
                    float blinkScale = sin2 > 0.5F ? 0.2F : 1.2F;
                    poseStack.scale(blinkScale, blinkScale, blinkScale);
                }
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                break;
            case "ReverseSpin":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.ZP.rotationDegrees(anim * -360.0F));
                break;
            case "Swing360":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim * 360.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-30.0F));
                break;
            case "Flip":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(anim * 360.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                break;
            case "Boomerang":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(anim * 270.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(anim * 50.0F));
                break;
            case "Cannon":
                poseStack.translate(0.56F, -0.52F, -0.72F - sin2 * 0.35F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - sin2 * 45.0F));
                break;
            case "Pierce":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.translate(0.0F, 0.0F, -sin2 * 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
                break;
            case "Wave":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(sin2 * 40.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * -90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2 * 25.0F));
                break;
            case "Claw":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(20.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-60.0F - anim * 40.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
                break;
            case "Wide":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                g = Mth.sin(swingProgress * swingProgress * 3.1415927F);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F - g * 30.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(sin2 * 55.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-50.0F));
                break;
            case "Aim":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-35.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-ang - sin2 * 40.0F * power / 5.0F));
                break;
            case "Precise":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-ang * 0.5F - sin2 * 90.0F * power / 5.0F));
                break;
            case "Drop":
                poseStack.translate(0.56F, -0.52F + anim * 0.4F * power / 5.0F, -0.72F);
                break;
            case "Vertical":
                poseStack.translate(0.56F, -0.52F, -0.72F);
                poseStack.mulPose(Axis.XP.rotationDegrees(sin2 * 360.0F));
                break;
            case "Custom": {
                poseStack.translate(0.56F + this.customPosX.getValue().floatValue(), -0.52F + this.customPosY.getValue().floatValue(), -0.72F + this.customPosZ.getValue().floatValue());
                float customScale = this.customScale.getValue().floatValue();
                poseStack.scale(customScale, customScale, customScale);
                float t = this.customCurve.is("Плавная")
                        ? swingProgress * swingProgress * (3.0F - 2.0F * swingProgress)
                        : swingProgress;
                poseStack.mulPose(Axis.XP.rotationDegrees(this.lerp(this.customStartX.getValue().floatValue(), this.customEndX.getValue().floatValue(), t)));
                poseStack.mulPose(Axis.YP.rotationDegrees(this.lerp(this.customStartY.getValue().floatValue(), this.customEndY.getValue().floatValue(), t)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(this.lerp(this.customStartZ.getValue().floatValue(), this.customEndZ.getValue().floatValue(), t)));
                break;
            }
            default:
                break;
        }
    }

    public void renderArmAnimation(PoseStack poseStack, float swingProgress, HumanoidArm arm) {
        if (this.isPreviewing()) {
            swingProgress = this.getPreviewProgress();
        } else {
            float speedFactor = (float) Math.max(this.speed.getValue(), 0.1D) / 7.0F;
            swingProgress = Mth.clamp(swingProgress * speedFactor, 0.0F, 1.0F);
        }
        if (arm == HumanoidArm.RIGHT) {
            poseStack.translate(this.rightX.getValue().floatValue(), this.rightY.getValue().floatValue(), this.rightZ.getValue().floatValue());
        } else {
            poseStack.translate(this.leftX.getValue().floatValue(), this.leftY.getValue().floatValue(), this.leftZ.getValue().floatValue());
        }
        String mode = this.animationMode.getValue();
        float swingRot = hmiSwingRot(swingProgress);
        float swing = hmiEase(Mth.sin(swingProgress * 3.1415927F));
        switch (mode) {
            case "HMI Взмахи":
                poseStack.translate(0.8F * swingRot, 0.3F * swingRot, -0.5F * swing);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-70.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * swing));
                break;
            case "HMI Вперед":
                poseStack.translate(0.12F * swingRot, 0.04F * swingRot, -0.95F * swing);
                poseStack.translate(0.02F * swing, 0.1F * swing, -0.1F * swingRot);
                poseStack.mulPose(Axis.YP.rotationDegrees(8.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(-14.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(32.0F * swing));
                break;
            case "HMI Обычная":
                poseStack.translate(0.1F * swingRot, 0.1F * swingRot, -0.1F * swing);
                poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * swing));
                poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * swing));
                break;
            case "HMI Копье":
                poseStack.translate(0.0F, 0.0F, 0.45F * swingRot);
                poseStack.translate(-0.25F * swing, -0.35F * swingRot, -0.6F * swing);
                poseStack.translate(0.0F, 0.1F * swing, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F * swingRot));
                break;
            case "HMI Инструмент":
                poseStack.translate(0.1F * swingRot, 0.1F * swingRot, -0.5F * swing);
                poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(40.0F * swing));
                break;
            case "HMI Блок":
                poseStack.translate(0.1F * swingRot, 0.1F * swingRot, -0.2F * swing);
                poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(20.0F * swing));
                break;
            case "HMI Лопата":
                poseStack.translate(0.0F, 0.15F * swingRot, -0.25F * swingRot);
                poseStack.translate(0.0F, 0.0F, -0.2F * swing);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F * swingRot));
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
                break;
            default:
                break;
        }
    }

    private static float hmiSwingRot(float swingProgress) {
        if (swingProgress < 0.6F) {
            return Mth.sin(Mth.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F);
        }
        return Mth.sin(Mth.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
    }

    private static float hmiEase(float value) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        if (value < 0.5F) {
            float doubled = 2.0F * value;
            return doubled * doubled * ((c2 + 1.0F) * doubled - c2) * 0.5F;
        }
        float shifted = 2.0F * value - 2.0F;
        return (shifted * shifted * ((c2 + 1.0F) * shifted + c2) + 2.0F) * 0.5F;
    }
}