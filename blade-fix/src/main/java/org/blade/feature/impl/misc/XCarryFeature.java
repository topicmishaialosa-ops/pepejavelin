package org.blade.feature.impl.misc;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.blade.event.EventTarget;
import org.blade.event.events.packet.PacketSendEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class XCarryFeature extends Feature {
    private static final int PLAYER_INVENTORY_CONTAINER_ID = 0;

    public XCarryFeature() {
        super("XCarry", "Store items in the crafting grid", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPhase() != PacketSendEvent.Phase.PRE) {
            return;
        }
        if (event.getPacket() instanceof ServerboundContainerClosePacket packet
                && packet.getContainerId() == PLAYER_INVENTORY_CONTAINER_ID) {
            event.cancel();
        }
    }
}
