package org.blade.feature.impl.misc;

import io.netty.channel.ChannelFuture;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientRegistryLayer;
import net.minecraft.client.multiplayer.RegistryDataCollector;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundResetChatPacket;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.configuration.ServerboundAcceptCodeOfConductPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Отдельная (headless) сессия бота: полный login -> configuration -> play рукопожатие
 * без привязки к главному клиенту. Бот стоит на сервере, отвечает на keep-alive/teleport
 * и живёт, пока сессию явно не закроют.
 */
public final class InstanceBotSession {

    public enum State {
        CONNECTING,
        ONLINE,
        CLOSED
    }

    public interface Callbacks {
        void onOnline();

        void onClosed(String reason, boolean intentional, boolean wasOnline);

        void onLog(String text, int color);
    }

    private static final long CONNECT_TIMEOUT_MS = 30_000L;

    private static final ScheduledExecutorService TICKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "InstanceBotSession-Tick");
        thread.setDaemon(true);
        return thread;
    });

    private static final ExecutorService CONNECTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "InstanceBotSession-Connect");
        thread.setDaemon(true);
        return thread;
    });

    public final String name;
    public final String server;
    public volatile State state = State.CONNECTING;
    public final long startedAt = System.currentTimeMillis();

    private final Callbacks callbacks;
    private final Map<net.minecraft.resources.Identifier, byte[]> cookies = new ConcurrentHashMap<>();
    private final RegistryDataCollector registryData = new RegistryDataCollector();
    private volatile FeatureFlagSet enabledFeatures = FeatureFlags.DEFAULT_FLAGS;
    private volatile RegistryAccess.Frozen baseRegistries;

    private volatile Connection connection;
    private volatile boolean done;
    private volatile boolean intentional;
    private volatile String failReason;
    private volatile boolean accepting = true;
    private ScheduledFuture<?> ticker;

    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    private volatile boolean falling;
    private volatile double fallGroundY;
    private volatile double fallVelocityY;
    private volatile long fallStartedAt;

    InstanceBotSession(String name, String server, Callbacks callbacks) {
        this.name = name;
        this.server = server;
        this.callbacks = callbacks;
    }

    public void start() {
        CONNECTOR.execute(this::connectLoop);
    }

    public boolean isOnline() {
        return this.state == State.ONLINE;
    }

    public void close(String reason) {
        closeInternal(reason, true);
    }

    public boolean sendChat(String text) {
        Connection conn = this.connection;
        if (conn == null || this.done || this.state != State.ONLINE) {
            return false;
        }
        try {
            if (text.startsWith("/")) {
                conn.send(new ServerboundChatCommandPacket(text.substring(1)));
            } else {
                conn.send(new ServerboundChatPacket(
                        text,
                        Instant.now(),
                        0L,
                        null,
                        new LastSeenMessages.Update(0, new BitSet(), LastSeenMessages.Update.IGNORE_CHECKSUM)));
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private void connectLoop() {
        try {
            callbacks.onLog("resolving " + server + " ...", InstanceBotManager.COLOR_AMBER);
            ServerAddress address = ServerAddress.parseString(this.server);
            Optional<ResolvedServerAddress> resolved = ServerNameResolver.DEFAULT.resolveAddress(address);
            if (resolved.isEmpty()) {
                throw new IOException("address not resolved: " + address.getHost());
            }
            ResolvedServerAddress target = resolved.get();
            String hostName = target.getHostName();
            int port = target.getPort();
            InetSocketAddress socketAddress = target.asInetSocketAddress();
            callbacks.onLog("resolved -> " + hostName + ":" + port + ", connecting ...", InstanceBotManager.COLOR_TEXT_DIM);

            Minecraft mc = Minecraft.getInstance();
            Connection conn = new Connection(PacketFlow.CLIENTBOUND);
            ChannelFuture future = Connection.connect(
                    socketAddress,
                    EventLoopGroupHolder.remote(mc.options.useNativeTransport()),
                    conn);
            future.syncUninterruptibly();
            if (!future.isSuccess()) {
                Throwable cause = future.cause();
                throw new IOException(cause != null ? cause.getMessage() : "connect failed", cause);
            }
            this.connection = conn;
            startTicking();
            callbacks.onLog("handshake sent, waiting for login ...", InstanceBotManager.COLOR_TEXT_DIM);
            conn.initiateServerboundPlayConnection(hostName, port, new LoginListener());
            conn.send(new ServerboundHelloPacket(this.name, UUIDUtil.createOfflinePlayerUUID(this.name)));

            long deadline = this.startedAt + CONNECT_TIMEOUT_MS;
            while (!this.done && this.state == State.CONNECTING && System.currentTimeMillis() < deadline) {
                Thread.sleep(50L);
            }
            if (!this.done && this.state == State.CONNECTING) {
                closeInternal("timeout: server did not respond", false);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            closeInternal("interrupted", false);
        } catch (Throwable throwable) {
            String detail = throwable.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = throwable.getClass().getSimpleName();
            }
            closeInternal(detail, false);
        }
    }

    private synchronized void closeInternal(String reason, boolean intentionalFlag) {
        if (this.done) {
            return;
        }
        boolean wasOnline = this.state == State.ONLINE;
        this.done = true;
        this.intentional = intentionalFlag;
        this.failReason = reason;
        this.state = State.CLOSED;
        this.accepting = false;
        if (this.ticker != null) {
            this.ticker.cancel(false);
            this.ticker = null;
        }
        Connection conn = this.connection;
        if (conn != null) {
            try {
                conn.disconnect(Component.literal(reason));
            } catch (Throwable ignored) {
            }
        }
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> this.callbacks.onClosed(reason, intentionalFlag, wasOnline));
    }

    private void failPacket(String reason) {
        if (this.done) {
            return;
        }
        this.failReason = reason;
        closeInternal(reason, false);
    }

    private synchronized void markOnline() {
        if (this.done || this.state != State.CONNECTING) {
            return;
        }
        this.state = State.ONLINE;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (!this.done) {
                this.callbacks.onOnline();
            }
        });
    }

    private void onListenerDisconnect(DisconnectionDetails details) {
        if (this.done) {
            return;
        }
        String reason = this.failReason;
        if (reason == null && details != null && details.reason() != null) {
            reason = details.reason().getString();
        }
        if (reason == null) {
            reason = "connection closed";
        }
        closeInternal(reason, this.intentional);
    }

    private void send(Packet<?> packet) {
        Connection conn = this.connection;
        if (conn != null && !this.done) {
            conn.send(packet);
        }
    }

    private void startTicking() {
        if (this.ticker != null) {
            return;
        }
        this.ticker = TICKER.scheduleAtFixedRate(() -> {
            Connection conn = this.connection;
            if (conn != null && !this.done) {
                try {
                    conn.tick();
                } catch (Throwable ignored) {
                }
                try {
                    tickFall();
                } catch (Throwable ignored) {
                }
            }
        }, 50L, 50L, TimeUnit.MILLISECONDS);
    }

    private void handleMovePlayer(ClientboundPlayerPositionPacket packet) {
        PositionMoveRotation current = new PositionMoveRotation(
                new Vec3(this.x, this.y, this.z), new Vec3(0.0D, 0.0D, 0.0D), this.yaw, this.pitch);
        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(current, packet.change(), packet.relatives());
        Vec3 position = absolute.position();
        double oldY = this.y;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.yaw = absolute.yRot();
        this.pitch = absolute.xRot();
        send(new ServerboundAcceptTeleportationPacket(packet.id()));
        send(new ServerboundMovePlayerPacket.PosRot(this.x, this.y, this.z, this.yaw, this.pitch, false, false));
        if (this.done || this.state != State.ONLINE || !fallEnabled()) {
            return;
        }
        if (position.y > oldY + 1.0D) {
            if (!this.falling) {
                this.falling = true;
                this.fallGroundY = oldY;
                this.fallVelocityY = 0.0D;
                this.fallStartedAt = System.currentTimeMillis();
            }
        } else if (this.falling && oldY - position.y > 1.0D) {
            this.falling = false;
        }
    }

    private static boolean fallEnabled() {
        InstanceBotFeature feature = InstanceBotFeature.INSTANCE;
        return feature != null && feature.fallSimulation.getValue();
    }

    private void tickFall() {
        if (!this.falling || this.done || this.state != State.ONLINE) {
            return;
        }
        if (!fallEnabled()) {
            this.falling = false;
            return;
        }
        if (System.currentTimeMillis() - this.fallStartedAt > 30_000L) {
            this.falling = false;
            send(new ServerboundMovePlayerPacket.PosRot(this.x, this.y, this.z, this.yaw, this.pitch, true, false));
            return;
        }
        this.fallVelocityY = (this.fallVelocityY - 0.08D) * 0.98D;
        this.y += this.fallVelocityY;
        if (this.y <= this.fallGroundY) {
            this.y = this.fallGroundY;
            this.fallVelocityY = 0.0D;
            this.falling = false;
            send(new ServerboundMovePlayerPacket.PosRot(this.x, this.y, this.z, this.yaw, this.pitch, true, false));
        } else {
            send(new ServerboundMovePlayerPacket.PosRot(this.x, this.y, this.z, this.yaw, this.pitch, false, false));
        }
    }

    private final class LoginListener implements ClientLoginPacketListener {

        @Override
        public void handleHello(ClientboundHelloPacket packet) {
            failPacket("server requires online-mode (encryption)");
        }

        @Override
        public void handleLoginFinished(ClientboundLoginFinishedPacket packet) {
            InstanceBotSession.this.baseRegistries = ClientRegistryLayer.createRegistryAccess().compositeAccess();
            ConfigListener configListener = new ConfigListener();
            Connection conn = InstanceBotSession.this.connection;
            conn.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, configListener);
            conn.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
            conn.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
            conn.send(new ServerboundCustomPayloadPacket(
                    new BrandPayload(ClientBrandRetriever.getClientModName())));
            conn.send(new ServerboundClientInformationPacket(ClientInformation.createDefault()));
        }

        @Override
        public void handleDisconnect(ClientboundLoginDisconnectPacket packet) {
            failPacket(packet.reason() != null ? packet.reason().getString() : "login refused");
        }

        @Override
        public void handleCompression(ClientboundLoginCompressionPacket packet) {
            Connection conn = InstanceBotSession.this.connection;
            if (conn != null && !conn.isMemoryConnection()) {
                conn.setupCompression(packet.getCompressionThreshold(), false);
            }
        }

        @Override
        public void handleCustomQuery(ClientboundCustomQueryPacket packet) {
            send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), null));
        }

        @Override
        public void handleRequestCookie(ClientboundCookieRequestPacket packet) {
            send(new ServerboundCookieResponsePacket(packet.key(), cookies.get(packet.key())));
        }

        @Override
        public void onDisconnect(DisconnectionDetails details) {
            onListenerDisconnect(details);
        }

        @Override
        public boolean isAcceptingMessages() {
            return !InstanceBotSession.this.done;
        }
    }

    private final class ConfigListener implements ClientConfigurationPacketListener {

        @Override
        public void handleSelectKnownPacks(ClientboundSelectKnownPacks packet) {
            send(new ServerboundSelectKnownPacks(List.of()));
        }

        @Override
        public void handleRegistryData(ClientboundRegistryDataPacket packet) {
            registryData.appendContents(packet.registry(), packet.entries());
        }

        @Override
        public void handleUpdateTags(ClientboundUpdateTagsPacket packet) {
            registryData.appendTags(packet.getTags());
        }

        @Override
        public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket packet) {
            InstanceBotSession.this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(packet.features());
        }

        @Override
        public void handleConfigurationFinished(ClientboundFinishConfigurationPacket packet) {
            RegistryAccess.Frozen base = InstanceBotSession.this.baseRegistries;
            if (base == null) {
                base = ClientRegistryLayer.createRegistryAccess().compositeAccess();
            }
            RegistryAccess.Frozen registries;
            try {
                registries = registryData.collectGameRegistries(ResourceProvider.EMPTY, base, false);
            } catch (Throwable throwable) {
                callbacks.onLog("registry collect failed: " + throwable, InstanceBotManager.COLOR_ERROR);
                registries = base;
            }
            Connection conn = InstanceBotSession.this.connection;
            ProtocolInfo<ClientGamePacketListener> playInfo =
                    GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(registries));
            conn.setupInboundProtocol(playInfo, createPlayListener());
            conn.send(ServerboundFinishConfigurationPacket.INSTANCE);
            conn.setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(
                    RegistryFriendlyByteBuf.decorator(registries),
                    (GameProtocols.Context) () -> false));
        }

        @Override
        public void handleCodeOfConduct(ClientboundCodeOfConductPacket packet) {
            send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
        }

        @Override
        public void handleResetChat(ClientboundResetChatPacket packet) {
        }

        @Override
        public void handleKeepAlive(ClientboundKeepAlivePacket packet) {
            send(new ServerboundKeepAlivePacket(packet.getId()));
        }

        @Override
        public void handlePing(ClientboundPingPacket packet) {
            send(new ServerboundPongPacket(packet.getId()));
        }

        @Override
        public void handleCustomPayload(ClientboundCustomPayloadPacket packet) {
        }

        @Override
        public void handleDisconnect(ClientboundDisconnectPacket packet) {
            failPacket(packet.reason() != null ? packet.reason().getString() : "disconnected");
        }

        @Override
        public void handleResourcePackPush(ClientboundResourcePackPushPacket packet) {
            send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
        }

        @Override
        public void handleResourcePackPop(ClientboundResourcePackPopPacket packet) {
        }

        @Override
        public void handleStoreCookie(ClientboundStoreCookiePacket packet) {
            cookies.put(packet.key(), packet.payload());
        }

        @Override
        public void handleTransfer(ClientboundTransferPacket packet) {
        }

        @Override
        public void handleCustomReportDetails(ClientboundCustomReportDetailsPacket packet) {
        }

        @Override
        public void handleServerLinks(ClientboundServerLinksPacket packet) {
        }

        @Override
        public void handleClearDialog(ClientboundClearDialogPacket packet) {
        }

        @Override
        public void handleShowDialog(ClientboundShowDialogPacket packet) {
        }

        @Override
        public void handleRequestCookie(ClientboundCookieRequestPacket packet) {
            send(new ServerboundCookieResponsePacket(packet.key(), cookies.get(packet.key())));
        }

        @Override
        public void onDisconnect(DisconnectionDetails details) {
            onListenerDisconnect(details);
        }

        @Override
        public boolean isAcceptingMessages() {
            return !InstanceBotSession.this.done;
        }
    }

    private ClientGamePacketListener createPlayListener() {
        return (ClientGamePacketListener) Proxy.newProxyInstance(
                InstanceBotSession.class.getClassLoader(),
                new Class<?>[]{ClientGamePacketListener.class},
                InstanceBotSession.this::invokePlay);
    }

    private Object invokePlay(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        switch (methodName) {
            case "handleKeepAlive" -> send(new ServerboundKeepAlivePacket(
                    ((ClientboundKeepAlivePacket) args[0]).getId()));
            case "handlePing" -> send(new ServerboundPongPacket(
                    ((ClientboundPingPacket) args[0]).getId()));
            case "handleDisconnect" -> {
                ClientboundDisconnectPacket packet = (ClientboundDisconnectPacket) args[0];
                failPacket(packet.reason() != null ? packet.reason().getString() : "disconnected");
            }
            case "handleMovePlayer" -> handleMovePlayer((ClientboundPlayerPositionPacket) args[0]);
            case "handleLogin" -> markOnline();
            case "handleSystemChat" -> {
                ClientboundSystemChatPacket packet = (ClientboundSystemChatPacket) args[0];
                if (packet.content() != null) {
                    String text = packet.content().getString();
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        InstanceBotManager.Bot bot = findBot();
                        if (bot != null) {
                            bot.log(">> " + text, InstanceBotManager.COLOR_TEXT);
                        }
                    });
                }
            }
            case "handleResourcePackPush" -> {
                ClientboundResourcePackPushPacket packet = (ClientboundResourcePackPushPacket) args[0];
                send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DECLINED));
            }
            case "handleStoreCookie" -> {
                ClientboundStoreCookiePacket packet = (ClientboundStoreCookiePacket) args[0];
                cookies.put(packet.key(), packet.payload());
            }
            case "handleRequestCookie" -> {
                ClientboundCookieRequestPacket packet = (ClientboundCookieRequestPacket) args[0];
                send(new ServerboundCookieResponsePacket(packet.key(), cookies.get(packet.key())));
            }
            case "protocol" -> {
                return net.minecraft.network.ConnectionProtocol.PLAY;
            }
            case "flow" -> {
                return PacketFlow.CLIENTBOUND;
            }
            case "isAcceptingMessages" -> {
                return !InstanceBotSession.this.done;
            }
            case "shouldHandleMessage" -> {
                return Boolean.TRUE;
            }
            case "onDisconnect" -> onListenerDisconnect((DisconnectionDetails) args[0]);
            case "createDisconnectionInfo" -> {
                return new DisconnectionDetails(
                        Component.literal(InstanceBotSession.this.failReason != null
                                ? InstanceBotSession.this.failReason
                                : "connection closed"));
            }
            case "equals" -> {
                return proxy == args[0];
            }
            case "hashCode" -> {
                return System.identityHashCode(proxy);
            }
            case "toString" -> {
                return "InstanceBotSession[" + InstanceBotSession.this.name + "]";
            }
            default -> {
            }
        }
        return null;
    }

    private InstanceBotManager.Bot findBot() {
        return InstanceBotManager.INSTANCE.findByName(InstanceBotSession.this.name);
    }
}
