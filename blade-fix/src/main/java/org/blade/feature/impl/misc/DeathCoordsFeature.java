package org.blade.feature.impl.misc;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.utils.text.ChatUtil;

public final class DeathCoordsFeature extends Feature {
    private boolean reported;

    public DeathCoordsFeature() {
        super("DeathCoords", "Shows your coordinates on death", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        LocalPlayer player = event.getClient().player;
        if (player == null) {
            reported = false;
            return;
        }

        if (!player.isDeadOrDying()) {
            reported = false;
            return;
        }
        if (reported) {
            return;
        }

        BlockPos pos = player.getOnPos();
        ChatUtil.print("Death at x: " + pos.getX() + " y: " + pos.getY() + " z: " + pos.getZ());
        reported = true;
    }
}
