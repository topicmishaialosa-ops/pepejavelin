package org.blade.mixin.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.blade.feature.impl.visual.NameTagsFeature;
import org.blade.utils.text.NameProtectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void protectNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {

        if (entity instanceof Player && NameTagsFeature.shouldHideVanillaTag()) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(NameProtectUtil.protect(cir.getReturnValue()));
    }
}
