package org.blade.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.blade.context.MinecraftContext;
import org.blade.event.EventTarget;
import org.blade.event.events.input.PlayerInputEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class SafeWalkFeature extends Feature implements MinecraftContext {
    public SafeWalkFeature() {
        super("SafeWalk", "Prevents walking off block edges", FeatureCategory.MOVEMENT, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        LocalPlayer player = player();
        if (player == null || level() == null || !player.onGround()) {
            return;
        }

        BlockPos below = player.blockPosition().below();
        if (level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
            event.setShift(true);
        }
    }
}
