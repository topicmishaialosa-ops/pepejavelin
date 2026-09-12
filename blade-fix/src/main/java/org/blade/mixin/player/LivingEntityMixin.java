package org.blade.mixin.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.blade.context.MinecraftContext;
import org.blade.event.EventManager;
import org.blade.event.Events;
import org.blade.event.events.game.PlayerJumpEvent;
import org.blade.feature.impl.visual.RemovalsFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void onPlayerJump(CallbackInfo ci) {
        LocalPlayer player = MinecraftContext.mc.player;
        if (player != null && (Object) this == player && EventManager.hasListeners(PlayerJumpEvent.class)) {
            EventManager.call(Events.PLAYER_JUMP.set(player, player.position()));
        }
    }

    @Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
    private void onGetEffectBlendFactor(Holder<MobEffect> effect, float partialTick, CallbackInfoReturnable<Float> cir) {
        if ((Object) this == MinecraftContext.mc.player && RemovalsFeature.shouldRemoveBadEffectsVisuals()) {
            cir.setReturnValue(0.0F);
        }
    }
}
