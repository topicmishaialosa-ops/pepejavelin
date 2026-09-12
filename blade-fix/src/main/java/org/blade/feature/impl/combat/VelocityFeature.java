package org.blade.feature.impl.combat;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.blade.context.MinecraftContext;
import org.blade.event.EventTarget;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class VelocityFeature extends Feature implements MinecraftContext {
    public VelocityFeature() {
        super("Velocity", "Removes knockback", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPhase() != PacketReceiveEvent.Phase.PRE || player() == null) {
            return;
        }
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.id() == player().getId()) {
            event.cancel();
        }
    }
}
