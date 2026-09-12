package org.blade.feature.impl.misc;

import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.blade.context.MinecraftContext;
import org.blade.event.EventTarget;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.combat.AuraFeature;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.pve.PvpStateTracker;
import org.blade.utils.FriendManager;

import java.util.List;
import java.util.Locale;

public final class AutoTpaAcceptFeature extends Feature implements MinecraftContext {
    private static final List<String> REQUEST_MARKERS = List.of(
            "has requested teleport",
            "телепортироваться"
    );
    private static final long ACCEPT_COOLDOWN_MILLIS = 1000L;

    public final BooleanSetting friendsOnly = register(new BooleanSetting(
            "Friends Only", false
    ));
    public final BooleanSetting avoidCombat = register(new BooleanSetting(
            "Avoid Combat", true
    ));

    private long lastAcceptAt;

    public AutoTpaAcceptFeature() {
        super("AutoTpaAccept", "Automatically accepts teleport requests", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPhase() != PacketReceiveEvent.Phase.PRE || player() == null) {
            return;
        }
        if (!(event.getPacket() instanceof ClientboundSystemChatPacket packet)) {
            return;
        }

        String text = packet.content().getString();
        String normalized = text.toLowerCase(Locale.ROOT);
        if (REQUEST_MARKERS.stream().noneMatch(normalized::contains)) {
            return;
        }
        if (this.avoidCombat.getValue() && isInCombat()) {
            return;
        }
        if (this.friendsOnly.getValue()
                && FriendManager.INSTANCE.getFriends().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .noneMatch(normalized::contains)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAcceptAt < ACCEPT_COOLDOWN_MILLIS) {
            return;
        }
        lastAcceptAt = now;
        player().connection.sendCommand("tpaccept");
    }

    private boolean isInCombat() {
        if (PvpStateTracker.INSTANCE.isActive() || player().hurtTime > 0) {
            return true;
        }
        AuraFeature aura = FeatureManager.INSTANCE.getFeature(AuraFeature.class);
        return aura != null && aura.getCurrentTarget() != null;
    }
}
