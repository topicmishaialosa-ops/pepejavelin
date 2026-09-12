package org.blade.feature.impl.misc;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.setting.TextSetting;
import org.blade.utils.DiscordIpc;
import org.blade.utils.text.ChatUtil;

public final class DiscordRpcFeature extends Feature {
    private static final String CLIENT_VERSION = "v0.3.5";

    public final TextSetting appId = register(new TextSetting("Application ID", "1536070842310594580"));
    public final BooleanSetting showInMenu = register(new BooleanSetting("В главном меню", true));
    public final BooleanSetting showServer = register(new BooleanSetting("Сервер", true));
    public final BooleanSetting showPing = register(new BooleanSetting("Пинг", true));
    public final BooleanSetting showFps = register(new BooleanSetting("FPS", true));
    public final NumberSetting updateInterval = register(new NumberSetting("Интервал обновления, с", 3.0, 1.0, 10.0, 1.0, ""));

    private String currentAppId;
    private boolean appIdWarned;
    private long startTimestamp;
    private Level lastWorld;
    private long lastUpdateAt;
    private String lastDetails = "" + '\u0000';
    private String lastState = "" + '\u0000';
    private String lastLargeKey = "" + '\u0000';
    private String lastLargeText = "" + '\u0000';
    private String lastSmallKey = "" + '\u0000';
    private String lastSmallText = "" + '\u0000';
    private long lastStartTimestamp = -1L;

    public DiscordRpcFeature() {
        super("DiscordRPC", "Rich Presence: показывает статус игры в Discord", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        this.initRpc();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        DiscordIpc.stop();
        this.currentAppId = null;
        this.lastWorld = null;
        this.lastUpdateAt = 0L;
        this.lastDetails = "" + '\u0000';
        this.lastState = "" + '\u0000';
        this.lastLargeKey = "" + '\u0000';
        this.lastLargeText = "" + '\u0000';
        this.lastSmallKey = "" + '\u0000';
        this.lastSmallText = "" + '\u0000';
        this.lastStartTimestamp = -1L;
    }

    private void initRpc() {
        String id = this.appId.getValue();
        if (id == null || id.isBlank()) {
            if (!this.appIdWarned) {
                this.appIdWarned = true;
                ChatUtil.info("DiscordRPC: укажи Application ID в настройках модуля (discord.com/developers)");
            }
            return;
        }

        boolean ok = DiscordIpc.start(id);
        this.currentAppId = id;
        if (!ok) {
            return;
        }

        this.lastUpdateAt = 0L;
        this.lastWorld = null;
        this.pushPresence(true);
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        String id = this.appId.getValue();
        if (id == null || id.isBlank()) {
            return;
        }
        if (!id.equals(this.currentAppId)) {
            DiscordIpc.stop();
            this.currentAppId = null;
            this.initRpc();
            return;
        }
        if (this.currentAppId == null) {
            this.initRpc();
            return;
        }

        if (mc.level == null) {
            if (this.lastWorld != null) {
                this.lastWorld = null;
            }
        } else if (this.lastWorld != mc.level) {
            this.lastWorld = mc.level;
            this.startTimestamp = System.currentTimeMillis() / 1000L;
        }

        this.pushPresence(false);
    }

    private void pushPresence(boolean force) {
        Minecraft mc = Minecraft.getInstance();
        if (this.currentAppId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        String details;
        String state;
        String largeKey;
        String largeText;
        String smallKey;
        String smallText;
        long timestamp;
        if (mc.level == null) {
            if (!this.showInMenu.getValue()) {
                if (force) {
                    DiscordIpc.clear();
                }
                return;
            }

            details = "В главном меню";
            state = RenamePasterClientFeature.getClientName() + " " + CLIENT_VERSION + " · Minecraft 1.21.4";
            largeKey = "huihui";
            largeText = RenamePasterClientFeature.getClientName() + " " + CLIENT_VERSION;
            smallKey = "";
            smallText = "";
            timestamp = 0L;
        } else {
            ServerData server = mc.getCurrentServer();
            if (server != null && this.showServer.getValue() && server.ip != null && !server.ip.isBlank()) {
                details = "Играет на " + server.ip;
            } else if (server != null) {
                details = "Играет на сервере";
            } else {
                details = "Одиночная игра";
            }

            StringBuilder stateBuilder = new StringBuilder();
            String dimensionName = this.getDimensionName();
            stateBuilder.append(dimensionName);
            if (this.showPing.getValue()) {
                int ping = this.getPing();
                if (ping >= 0) {
                    stateBuilder.append(" · ").append(ping).append(" мс");
                }
            }
            if (this.showFps.getValue()) {
                stateBuilder.append(" · ").append(mc.getFps()).append(" FPS");
            }

            state = stateBuilder.toString();
            largeKey = "huihui";
            largeText = RenamePasterClientFeature.getClientName() + " " + CLIENT_VERSION;
            smallKey = this.getDimensionKey();
            smallText = dimensionName;
            timestamp = this.startTimestamp;
        }

        if (!force
                && details.equals(this.lastDetails)
                && state.equals(this.lastState)
                && largeKey.equals(this.lastLargeKey)
                && largeText.equals(this.lastLargeText)
                && smallKey.equals(this.lastSmallKey)
                && smallText.equals(this.lastSmallText)
                && timestamp == this.lastStartTimestamp
                && now - this.lastUpdateAt < (long) (this.updateInterval.getValue().doubleValue() * 1000.0F)) {
            return;
        }

        this.lastDetails = details;
        this.lastState = state;
        this.lastLargeKey = largeKey;
        this.lastLargeText = largeText;
        this.lastSmallKey = smallKey;
        this.lastSmallText = smallText;
        this.lastStartTimestamp = timestamp;
        this.lastUpdateAt = now;

        JsonObject activity = new JsonObject();
        activity.addProperty("state", state);
        activity.addProperty("details", details);
        JsonObject timestamps = new JsonObject();
        if (timestamp > 0L) {
            timestamps.addProperty("start", timestamp);
        }
        activity.add("timestamps", timestamps);
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", largeKey);
        if (!largeText.isEmpty()) {
            assets.addProperty("large_text", largeText);
        }
        if (!smallKey.isEmpty()) {
            assets.addProperty("small_image", smallKey);
        }
        if (!smallText.isEmpty()) {
            assets.addProperty("small_text", smallText);
        }
        activity.add("assets", assets);
        DiscordIpc.update(activity);
    }

    private String getDimensionName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "Неизвестно";
        }

        ResourceKey<Level> key = mc.level.dimension();
        if (key == Level.OVERWORLD) {
            return "Верхний мир";
        } else if (key == Level.NETHER) {
            return "Нижний мир";
        } else if (key == Level.END) {
            return "Край";
        } else {
            return key.identifier().toString();
        }
    }

    private String getDimensionKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "other";
        }

        ResourceKey<Level> key = mc.level.dimension();
        if (key == Level.OVERWORLD) {
            return "overworld";
        } else if (key == Level.NETHER) {
            return "nether";
        } else if (key == Level.END) {
            return "end";
        } else {
            return "other";
        }
    }

    private int getPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return -1;
        }

        try {
            for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                if (entry.getProfile().id().equals(mc.player.getUUID())) {
                    return entry.getLatency();
                }
            }
            return -1;
        } catch (Throwable throwable) {
            return -1;
        }
    }
}