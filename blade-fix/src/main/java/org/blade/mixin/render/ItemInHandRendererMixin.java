package org.blade.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.blade.context.RotationContext;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.visual.SwingAnimationFeature;
import org.blade.feature.impl.visual.ViewModelFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)
    )
    private void applyViewModelOffset(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equippedProgress,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            CallbackInfo ci
    ) {
        ViewModelFeature viewModel = FeatureManager.INSTANCE.getEnabled(ViewModelFeature.class);
        if (viewModel == null || stack.isEmpty()
                || player.isUsingItem() && player.getUsedItemHand() == hand) {
            return;
        }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        poseStack.translate(viewModel.offsetX(arm), viewModel.offsetY(arm), viewModel.offsetZ(arm));
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void customSwingArm(float swingProgress, PoseStack poseStack, int direction, HumanoidArm arm, CallbackInfo ci) {
        SwingAnimationFeature swing = FeatureManager.INSTANCE.getEnabled(SwingAnimationFeature.class);
        if (swing == null || arm != HumanoidArm.RIGHT) {
            return;
        }
        swing.renderSwordAnimation(poseStack, swingProgress, 0.0F, arm);
        ci.cancel();
    }

    @Redirect(
            method = "submitHandsWithItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot(F)F")
    )
    private float useFreeLookPitch(LocalPlayer player, float partialTick) {
        if (RotationContext.isActive()) {
            org.blade.feature.impl.combat.AuraFeature aura = org.blade.feature.FeatureManager.INSTANCE.getEnabled(org.blade.feature.impl.combat.AuraFeature.class);
            if (aura != null && aura.clientLook.getValue()) {
                return player.getXRot(partialTick);
            }
            return RotationContext.getFreePitch();
        }

        return player.getXRot(partialTick);
    }

    @Redirect(
            method = "submitHandsWithItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F")
    )
    private float useCameraPitch(LocalPlayer player, float partialTick) {
        return this.minecraft.gameRenderer.mainCamera().xRot();
    }

    @Redirect(
            method = "submitHandsWithItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F")
    )
    private float useCameraYaw(LocalPlayer player, float partialTick) {
        return this.minecraft.gameRenderer.mainCamera().yRot();
    }
}
