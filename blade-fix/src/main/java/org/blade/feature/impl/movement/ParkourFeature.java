package org.blade.feature.impl.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class ParkourFeature extends Feature {
    private static final double EDGE_PROBE_DEPTH = 0.001;

    public ParkourFeature() {
        super("Parkour", "Automatically jumps at block edges", FeatureCategory.MOVEMENT, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        LocalPlayer player = event.getClient().player;
        if (player == null || event.getClient().level == null) {
            return;
        }
        if (!player.onGround() || player.isShiftKeyDown() || player.getAbilities().flying) {
            return;
        }

        if (isAtEdge(event, player)) {
            player.jumpFromGround();
        }
    }

    private boolean isAtEdge(GameTickEvent event, LocalPlayer player) {
        AABB probe = player.getBoundingBox().move(0.0, -EDGE_PROBE_DEPTH, 0.0);
        return !event.getClient().level.getBlockCollisions(player, probe).iterator().hasNext();
    }
}
