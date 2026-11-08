package org.blade.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.event.events.packet.PacketSendEvent;

public final class PacketEventManager {
    private PacketEventManager() {
    }

    /**
     * События пакетов рассылаются только для главного соединения игрока.
     * Headless-сессии ботов (InstanceBotSession) в события не попадают.
     */
    public static boolean isPrimaryConnection(Connection connection) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && listener.getConnection() == connection;
    }

    public static boolean hasSendListeners() {
        return EventManager.hasListeners(PacketSendEvent.class);
    }

    public static boolean hasReceiveListeners() {
        return EventManager.hasListeners(PacketReceiveEvent.class);
    }

    public static PacketSendEvent callSendPre(Connection connection, Packet<?> packet) {
        return EventManager.call(new PacketSendEvent(connection, packet, PacketSendEvent.Phase.PRE));
    }

    public static void callSendPost(Connection connection, Packet<?> packet) {
        EventManager.call(new PacketSendEvent(connection, packet, PacketSendEvent.Phase.POST));
    }

    public static boolean callReceivePre(Connection connection, Packet<?> packet) {
        return EventManager.call(new PacketReceiveEvent(connection, packet, PacketReceiveEvent.Phase.PRE)).isCancelled();
    }

    public static void callReceivePost(Connection connection, Packet<?> packet) {
        EventManager.call(new PacketReceiveEvent(connection, packet, PacketReceiveEvent.Phase.POST));
    }
}
