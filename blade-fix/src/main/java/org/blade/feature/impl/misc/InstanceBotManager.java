package org.blade.feature.impl.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.blade.menu.core.MenuConfigStore;
import org.blade.utils.AccountSwitcher;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class InstanceBotManager {

    public static final InstanceBotManager INSTANCE = new InstanceBotManager();

    public static final int COLOR_OK = ColorUtil.rgb(76, 201, 120);
    public static final int COLOR_AMBER = ColorUtil.rgb(240, 180, 60);
    public static final int COLOR_GRAY = ColorUtil.rgb(120, 120, 128);
    public static final int COLOR_ERROR = Theme.Colors.SYSTEM_RED;
    public static final int COLOR_TEXT = Theme.Colors.TEXT;
    public static final int COLOR_TEXT_DIM = ColorUtil.rgb(154, 154, 162);

    private static final String CONFIG_KEY = "instanceBots";
    private static final long CONNECT_TIMEOUT_MS = 30_000L;

    public enum Status {
        OFFLINE,
        CONNECTING,
        ONLINE,
        ERROR
    }

    public static final class LogLine {
        public final String text;
        public final int color;

        LogLine(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    public static final class Bot {
        public final String name;
        public final String server;
        public Status status = Status.OFFLINE;
        public String statusText = "Disconnected by user";
        public final List<LogLine> log = new ArrayList<>();
        public boolean connecting;
        public long connectingSince;

        Bot(String name, String server) {
            this.name = name;
            this.server = server;
            this.log.add(new LogLine("Бот добавлен", COLOR_TEXT_DIM));
        }

        public void log(String text, int color) {
            this.log.add(new LogLine(text, color));
            if (this.log.size() > 100) {
                this.log.remove(0);
            }
        }
    }

    public String toastText;
    public int toastColor;
    public long toastUntil;

    private final List<Bot> bots = new ArrayList<>();
    private final Map<String, InstanceBotSession> sessions = new ConcurrentHashMap<>();
    private boolean loaded;
    private boolean navigating;
    private String lastLaunched;
    private String mainServer;

    private InstanceBotManager() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeAllSessions, "InstanceBot-Shutdown"));
    }

    public InstanceBotSession session(String name) {
        if (name == null) {
            return null;
        }
        return this.sessions.get(name.toLowerCase(Locale.ROOT));
    }

    public void closeSession(String name, String reason) {
        if (name == null) {
            return;
        }
        InstanceBotSession session = this.sessions.remove(name.toLowerCase(Locale.ROOT));
        if (session != null) {
            session.close(reason);
        }
    }

    public void closeAllSessions() {
        for (InstanceBotSession session : this.sessions.values()) {
            session.close("client shutting down");
        }
        this.sessions.clear();
    }

    public void connectHeadless(String name, String server) {
        connectHeadless(name, server, 0, 0L);
    }

    private void persistSession(String name, String server) {
        CompletableFuture.delayedExecutor(3500L, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> Minecraft.getInstance().execute(() -> {
                    InstanceBotSession current = session(name);
                    if (current != null && current.state != InstanceBotSession.State.CLOSED) {
                        return;
                    }
                    Bot bot = findByName(name);
                    if (bot != null) {
                        bot.log("rejoining standing session ...", COLOR_AMBER);
                    }
                    connectHeadless(name, server, 0, 0L);
                }));
    }

    private void connectHeadless(String name, String server, int attempt, long delayMs) {
        if (delayMs > 0L) {
            CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .execute(() -> Minecraft.getInstance().execute(() -> connectHeadless(name, server, attempt, 0L)));
            return;
        }
        closeSession(name, "reconnecting");
        InstanceBotSession[] holder = new InstanceBotSession[1];
        InstanceBotSession session = new InstanceBotSession(name, server, new InstanceBotSession.Callbacks() {
            @Override
            public void onOnline() {
                if (superseded()) {
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                Bot bot = findByName(name);
                if (bot == null) {
                    return;
                }
                if (mc.level != null && bot.name.equalsIgnoreCase(currentName())) {
                    return;
                }
                bot.connecting = false;
                bot.status = Status.ONLINE;
                bot.statusText = "On the server (AFK)";
                bot.log("joined the game (standing on server)", COLOR_OK);
                chatLog(bot, "joined the game (standing on server)", COLOR_OK);
            }

            @Override
            public void onClosed(String reason, boolean intentional, boolean wasOnline) {
                if (superseded()) {
                    return;
                }
                String key = name.toLowerCase(Locale.ROOT);
                boolean stillRegistered = sessions.get(key) == holder[0];
                Bot bot = findByName(name);
                if (stillRegistered && !intentional && !wasOnline && attempt < 1) {
                    if (bot != null) {
                        bot.log("join failed (" + reason + "), retrying ...", COLOR_AMBER);
                        chatLog(bot, "join failed (" + reason + "), retrying ...", COLOR_AMBER);
                    }
                    CompletableFuture.delayedExecutor(3000L, java.util.concurrent.TimeUnit.MILLISECONDS)
                            .execute(() -> Minecraft.getInstance().execute(() -> {
                                if (sessions.get(key) == holder[0]) {
                                    connectHeadless(name, server, attempt + 1, 0L);
                                }
                            }));
                    return;
                }
                sessions.remove(key, holder[0]);
                if (bot == null) {
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && bot.name.equalsIgnoreCase(currentName())) {
                    return;
                }
                bot.connecting = false;
                if (intentional) {
                    bot.status = Status.OFFLINE;
                    bot.statusText = "Disconnected by user";
                    bot.log("Disconnected by user", COLOR_TEXT_DIM);
                } else {
                    bot.status = Status.ERROR;
                    bot.statusText = reason;
                    bot.log("disconnected: " + reason, COLOR_ERROR);
                    chatLog(bot, "disconnected: " + reason, COLOR_ERROR);
                }
            }

            @Override
            public void onLog(String text, int color) {
                if (superseded()) {
                    return;
                }
                Bot bot = findByName(name);
                if (bot != null) {
                    bot.log(text, color);
                    chatLog(bot, text, color);
                }
            }

            private boolean superseded() {
                InstanceBotSession current = sessions.get(name.toLowerCase(Locale.ROOT));
                return current != null && current != holder[0];
            }
        });
        holder[0] = session;
        this.sessions.put(name.toLowerCase(Locale.ROOT), session);

        Bot bot = findByName(name);
        if (bot != null) {
            bot.connecting = true;
            bot.connectingSince = System.currentTimeMillis();
            bot.status = Status.CONNECTING;
            bot.statusText = "Configuring session ...";
            bot.log("standing session -> " + server, COLOR_AMBER);
            chatLog(bot, "standing session -> " + server, COLOR_AMBER);
        }
        session.start();
    }

    public List<Bot> bots() {
        ensureLoaded();
        return this.bots;
    }

    public Bot findByName(String name) {
        if (name == null) {
            return null;
        }
        for (Bot bot : bots()) {
            if (bot.name.equalsIgnoreCase(name)) {
                return bot;
            }
        }
        return null;
    }

    public Bot add(String name, String server) {
        Bot bot = new Bot(name, server);
        bots().add(bot);
        save();
        return bot;
    }

    public Bot host() {
        String name;
        do {
            name = "bot_" + (1000 + ThreadLocalRandom.current().nextInt(9000));
        } while (findByName(name) != null);
        Bot bot = add(name, defaultServer());
        showToast("Бот добавлен: " + name, COLOR_OK);
        return bot;
    }

    public void remove(Bot bot) {
        closeSession(bot.name, "bot removed");
        if (bots().remove(bot)) {
            save();
        }
    }

    public void showToast(String text, int color) {
        this.toastText = text;
        this.toastColor = color;
        this.toastUntil = System.currentTimeMillis() + 2600L;
    }

    public boolean hasToast() {
        return this.toastText != null && System.currentTimeMillis() < this.toastUntil;
    }

    public String currentName() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getUser() != null ? mc.getUser().getName() : "";
    }

    public boolean isActive(Bot bot) {
        return bot.name.equalsIgnoreCase(currentName());
    }

    public String defaultServer() {
        Minecraft mc = Minecraft.getInstance();
        ServerData server = mc.getCurrentServer();
        return server != null && server.ip != null && !server.ip.isBlank() ? server.ip : "funtime.su";
    }

    public void refresh(Bot bot) {
        Minecraft mc = Minecraft.getInstance();
        boolean inGame = mc.level != null && isActive(bot);
        InstanceBotSession session = session(bot.name);
        if (inGame) {
            if (session != null) {
                closeSession(bot.name, "session takeover");
            }
            bot.connecting = false;
            bot.status = Status.ONLINE;
            bot.statusText = "Joined the game as " + bot.name;
            return;
        }
        if (session != null) {
            switch (session.state) {
                case CONNECTING -> {
                    bot.connecting = true;
                    bot.status = Status.CONNECTING;
                    bot.statusText = "Configuring session ...";
                    return;
                }
                case ONLINE -> {
                    bot.connecting = false;
                    bot.status = Status.ONLINE;
                    bot.statusText = "On the server (AFK)";
                    return;
                }
                case CLOSED -> {
                }
            }
        }
        if (bot.connecting) {
            if (System.currentTimeMillis() - bot.connectingSince < CONNECT_TIMEOUT_MS) {
                bot.status = Status.CONNECTING;
                bot.statusText = "Configuring session ...";
                return;
            }
            bot.connecting = false;
            bot.status = Status.OFFLINE;
            bot.statusText = "Disconnected by user";
            return;
        }
        if (bot.status == Status.ONLINE || bot.status == Status.CONNECTING) {
            bot.status = Status.OFFLINE;
            bot.statusText = "Disconnected by user";
        }
    }

    public static int statusColor(Status status) {
        return switch (status) {
            case ONLINE -> COLOR_OK;
            case CONNECTING -> COLOR_AMBER;
            case ERROR -> COLOR_ERROR;
            case OFFLINE -> COLOR_GRAY;
        };
    }

    public void control(Bot bot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && isActive(bot)) {
            showToast("Ты уже управляешь этим ботом", COLOR_GRAY);
            return;
        }
        if (bot.connecting) {
            return;
        }
        closeSession(bot.name, "переключение управления");
        bot.connecting = true;
        bot.connectingSince = System.currentTimeMillis();
        bot.status = Status.CONNECTING;
        bot.statusText = "Configuring session ...";
        bot.log("switching to " + bot.name + " ...", COLOR_AMBER);
        chatLog(bot, "switching to " + bot.name + " ...", COLOR_AMBER);
        launch(bot);
    }

    public void reconnect(Bot bot) {
        if (bot.connecting) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        InstanceBotSession existing = session(bot.name);
        if (existing != null && existing.isOnline()) {
            showToast("Этот бот уже на сервере (уже переподключён)", COLOR_GRAY);
            return;
        }
        showToast("Подключение запущено", COLOR_OK);
        connectHeadless(bot.name, bot.server);
    }

    public void disconnectBot(Bot bot) {
        Minecraft mc = Minecraft.getInstance();
        bot.connecting = false;
        closeSession(bot.name, "Disconnected by user");
        if (mc.level != null && isActive(bot)) {
            mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
        }
        bot.status = Status.OFFLINE;
        bot.statusText = "Disconnected by user";
        bot.log("Disconnected by user", COLOR_TEXT_DIM);
    }

    public boolean sendMessage(Bot bot, String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null && isActive(bot)) {
            if (text.startsWith("/")) {
                mc.player.connection.sendCommand(text.substring(1));
            } else {
                mc.player.connection.sendChat(text);
            }
            bot.log("> " + text, COLOR_TEXT);
            return true;
        }
        InstanceBotSession session = session(bot.name);
        if (session != null && session.isOnline()) {
            if (session.sendChat(text)) {
                bot.log("> " + text, COLOR_TEXT);
                return true;
            }
            bot.log("не удалось отправить", COLOR_ERROR);
            return false;
        }
        bot.log("бот не на сервере — нажми «Реконнект» или «Управлять»", COLOR_ERROR);
        return false;
    }

    public void onJoined() {
        Bot bot = findByName(currentName());
        this.lastLaunched = null;
        if (bot == null) {
            return;
        }
        bot.connecting = false;
        bot.status = Status.ONLINE;
        bot.statusText = "Joined the game as " + bot.name;
        bot.log("joined the game as " + bot.name, COLOR_OK);
        chatLog(bot, "joined the game as " + bot.name, COLOR_OK);
    }

    public void markLaunchFailed() {
        Bot bot = findByName(this.lastLaunched);
        this.lastLaunched = null;
        if (bot == null) {
            return;
        }
        bot.connecting = false;
        bot.status = Status.ERROR;
        bot.statusText = "Connection failed";
        bot.log("connection failed: сервер недоступен", COLOR_ERROR);
        chatLog(bot, "connection failed: сервер недоступен", COLOR_ERROR);
        showToast("Не удалось подключиться", COLOR_ERROR);
    }

    public boolean consumeNavigating() {
        boolean was = this.navigating;
        this.navigating = false;
        return was;
    }

    public String mainAccountName() {
        String stored = MenuConfigStore.getString("selectedAccount", "");
        if (stored.isEmpty() || !stored.matches("^[a-zA-Z0-9_]{3,16}$")) {
            stored = AccountSwitcher.DEFAULT_NAME;
        }
        return stored;
    }

    public void backToMain() {
        Minecraft mc = Minecraft.getInstance();
        String main = mainAccountName();
        if (main.equalsIgnoreCase(currentName()) && mc.level != null) {
            showToast("Ты уже на основном аккаунте", COLOR_GRAY);
            return;
        }
        String server = this.mainServer;
        if (server == null || server.isBlank()) {
            ServerData current = mc.getCurrentServer();
            if (current != null && current.ip != null && !current.ip.isBlank()) {
                server = current.ip;
            } else if (mc.level != null) {
                showToast("Неизвестно, где основной аккаунт", COLOR_ERROR);
                return;
            } else {
                server = defaultServer();
            }
        }

        this.navigating = true;
        this.lastLaunched = null;

        String leaving = null;
        String leaveServer = null;
        if (mc.level != null) {
            leaving = currentName();
            Bot leaveBot = findByName(leaving);
            if (leaveBot != null) {
                leaveServer = leaveBot.server;
            }
        }

        closeSession(main, "returning to main account");
        AccountSwitcher.switchTo(main);
        if (mc.level != null) {
            mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
        }
        if (leaving != null && findByName(leaving) != null && !mc.isLocalServer()) {
            persistSession(leaving, leaveServer != null ? leaveServer : server);
        }

        ServerData data = new ServerData(main, server, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(
                new TitleScreen(),
                mc,
                ServerAddress.parseString(server),
                data,
                false,
                null
        );
        showToast("Возврат к " + main, COLOR_OK);
    }

    private void launch(Bot bot) {
        Minecraft mc = Minecraft.getInstance();
        this.navigating = true;
        this.lastLaunched = bot.name;

        String leaving = null;
        String leaveServer = null;
        if (mc.level != null) {
            leaving = currentName();
            ServerData current = mc.getCurrentServer();
            Bot leaveBot = findByName(leaving);
            leaveServer = leaveBot != null
                    ? leaveBot.server
                    : current != null && current.ip != null && !current.ip.isBlank()
                    ? current.ip
                    : bot.server;
            if (leaveBot == null && leaveServer != null && !leaveServer.isBlank()) {
                this.mainServer = leaveServer;
            }
        }

        AccountSwitcher.switchTo(bot.name);
        if (mc.level != null) {
            mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
        }
        if (leaving != null && !leaving.equalsIgnoreCase(bot.name) && !mc.isLocalServer()) {
            persistSession(leaving, leaveServer);
        }

        ServerData data = new ServerData(bot.name, bot.server, ServerData.Type.OTHER);
        ConnectScreen.startConnecting(
                new TitleScreen(),
                mc,
                ServerAddress.parseString(bot.server),
                data,
                false,
                null
        );
    }

    private void chatLog(Bot bot, String text, int color) {
        InstanceBotFeature feature = InstanceBotFeature.INSTANCE;
        if (feature == null || !feature.isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) {
            return;
        }
        MutableComponent line = Component.empty()
                .append(Component.literal("Blade >> [Bot] " + bot.name + " >> ")
                        .withStyle(Style.EMPTY.withColor(COLOR_TEXT_DIM & 0xFFFFFF)))
                .append(Component.literal(text)
                        .withStyle(Style.EMPTY.withColor(color & 0xFFFFFF)));
        mc.gui.hud.getChat().addClientSystemMessage(line);
    }

    private void ensureLoaded() {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        JsonArray array = MenuConfigStore.getArray(CONFIG_KEY);
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String name = object.has("name") ? object.get("name").getAsString() : "";
            String server = object.has("server") ? object.get("server").getAsString() : "";
            if (name.matches("^[a-zA-Z0-9_]{3,16}$") && !server.isBlank()) {
                this.bots.add(new Bot(name, server));
            }
        }
    }

    private void save() {
        MenuConfigStore.save(data -> {
            JsonArray array = new JsonArray();
            for (Bot bot : this.bots) {
                JsonObject object = new JsonObject();
                object.addProperty("name", bot.name);
                object.addProperty("server", bot.server);
                array.add(object);
            }
            data.add(CONFIG_KEY, array);
        });
    }

    public static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
