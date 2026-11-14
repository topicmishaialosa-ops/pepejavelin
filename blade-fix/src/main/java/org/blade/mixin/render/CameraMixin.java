package org.blade.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.blade.context.MinecraftContext;
import org.blade.context.RotationContext;
import org.blade.feature.impl.visual.AspectRatioFeature;
import org.blade.feature.impl.visual.RemovalsFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @WrapOperation(
            method = "setupPerspective",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V"
            )
    )
    private void applyAspectRatio(Projection projection, float near, float far, float fov,
                                  float width, float height, Operation<Void> original) {
        float adjustedWidth = width;
        if (height > 0.0F) {
            float aspectRatio = width / height;
            float adjustedRatio = AspectRatioFeature.resolveRatio(aspectRatio);
            if (adjustedRatio != aspectRatio) {
                adjustedWidth = height * adjustedRatio;
            }
        }
        original.call(projection, near, far, fov, adjustedWidth, height);
    }

    @ModifyArg(
            method = "createProjectionMatrixForCulling",
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"),
            index = 1
    )
    private float applyCullingAspectRatio(float aspectRatio) {
        return AspectRatioFeature.resolveRatio(aspectRatio);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(CameraRenderState cameraState, float cameraEntityPartialTick, CallbackInfo ci) {
        if (RemovalsFeature.shouldRemoveBadEffectsVisuals()) {
            cameraState.entityRenderState.doesMobEffectBlockSky = false;
        }
    }

    @Inject(method = "alignWithEntity", at = @At("HEAD"))
    private void onAlignWithEntity(float partialTick, CallbackInfo ci) {
        Camera camera = (Camera) (Object) this;
        Entity entity = camera.entity();
        if (entity == null) {
            return;
        }

        RotationContext.applyRenderInterpolation();
        RotationContext.syncFreeLook(entity.getViewYRot(partialTick), entity.getViewXRot(partialTick));
    }


}
