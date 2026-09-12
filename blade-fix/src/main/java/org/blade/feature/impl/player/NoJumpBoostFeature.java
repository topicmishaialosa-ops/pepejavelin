package org.blade.feature.impl.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class NoJumpBoostFeature extends Feature {
    public NoJumpBoostFeature() {
        super("NoJumpBoost", "Removes jump boost effect", FeatureCategory.PLAYER, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        LocalPlayer player = event.getClient().player;
        if (player != null && player.hasEffect(MobEffects.JUMP_BOOST)) {
            player.removeEffect(MobEffects.JUMP_BOOST);
        }
    }
}
