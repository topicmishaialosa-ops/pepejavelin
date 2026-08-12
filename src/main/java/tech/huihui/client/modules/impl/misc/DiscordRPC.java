package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonObject;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import tech.huihui.base.events.impl.other.EventGameUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.utility.game.other.DiscordIpc;
import tech.huihui.utility.game.other.MessageUtil;

@ModuleAnnotation(
   name = "DiscordRPC",
   description = "Rich Presence: показывает статус игры в Discord",
   category = Category.MISC
)
public final class DiscordRPC extends Module {
   public static final DiscordRPC INSTANCE = new DiscordRPC();
   private static final String CLIENT_NAME = "HuihuiClient";
   private static final String CLIENT_VERSION = "v0.3.5";

   private final StringSetting appId = new StringSetting("Application ID", "1536070842310594580");
   private final BooleanSetting showInMenu = new BooleanSetting("В главном меню", true);
   private final BooleanSetting showServer = new BooleanSetting("Сервер", true);
   private final BooleanSetting showPing = new BooleanSetting("Пинг", true);
   private final BooleanSetting showFps = new BooleanSetting("FPS", true);
   private final NumberSetting updateInterval = new NumberSetting("Интервал обновления, с", 3.0F, 1.0F, 10.0F, 1.0F);

   private String currentAppId;
   private boolean appIdWarned;
   private long startTimestamp;
   private World lastWorld;
   private long lastUpdateAt;
   private String lastDetails = "\u0000";
   private String lastState = "\u0000";
   private String lastLargeKey = "\u0000";
   private String lastLargeText = "\u0000";
   private String lastSmallKey = "\u0000";
   private String lastSmallText = "\u0000";
   private long lastStartTimestamp = -1L;

   private DiscordRPC() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.initRpc();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      DiscordIpc.stop();
      this.currentAppId = null;
      this.lastWorld = null;
      this.lastUpdateAt = 0L;
      this.lastDetails = "\u0000";
      this.lastState = "\u0000";
      this.lastLargeKey = "\u0000";
      this.lastLargeText = "\u0000";
      this.lastSmallKey = "\u0000";
      this.lastSmallText = "\u0000";
      this.lastStartTimestamp = -1L;
   }

   private void initRpc() {
      String id = this.appId.getValue();
      if (id == null || id.isBlank()) {
         if (!this.appIdWarned) {
            this.appIdWarned = true;
            MessageUtil.displayInfo("DiscordRPC: укажи Application ID в настройках модуля (discord.com/developers)");
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
   private void onGameUpdate(EventGameUpdate e) {
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

      if (mc.world == null) {
         if (this.lastWorld != null) {
            this.lastWorld = null;
         }
      } else if (this.lastWorld != mc.world) {
         this.lastWorld = mc.world;
         this.startTimestamp = System.currentTimeMillis() / 1000L;
      }

      this.pushPresence(false);
   }

   private void pushPresence(boolean force) {
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
      if (mc.world == null) {
         if (!this.showInMenu.isEnabled()) {
            if (force) {
               DiscordIpc.clear();
            }
            return;
         }

         details = "В главном меню";
         state = CLIENT_NAME + " " + CLIENT_VERSION + " · Minecraft 1.21.4";
         largeKey = "huihui";
         largeText = CLIENT_NAME + " " + CLIENT_VERSION;
         smallKey = "";
         smallText = "";
         timestamp = 0L;
      } else {
         ServerInfo server = mc.getCurrentServerEntry();
         if (server != null && this.showServer.isEnabled() && server.address != null && !server.address.isBlank()) {
            details = "Играет на " + server.address;
         } else if (server != null) {
            details = "Играет на сервере";
         } else {
            details = "Одиночная игра";
         }

         StringBuilder stateBuilder = new StringBuilder();
         String dimensionName = this.getDimensionName();
         stateBuilder.append(dimensionName);
         if (this.showPing.isEnabled()) {
            int ping = this.getPing();
            if (ping >= 0) {
               stateBuilder.append(" · ").append(ping).append(" мс");
            }
         }
         if (this.showFps.isEnabled()) {
            stateBuilder.append(" · ").append(mc.getCurrentFps()).append(" FPS");
         }

         state = stateBuilder.toString();
         largeKey = "huihui";
         largeText = CLIENT_NAME + " " + CLIENT_VERSION;
         smallKey = this.getDimensionKey();
         smallText = dimensionName;
         timestamp = this.startTimestamp;
      }

      if (!force && details.equals(this.lastDetails) && state.equals(this.lastState) && largeKey.equals(this.lastLargeKey) && largeText.equals(this.lastLargeText) && smallKey.equals(this.lastSmallKey) && smallText.equals(this.lastSmallText) && timestamp == this.lastStartTimestamp && now - this.lastUpdateAt < (long)(this.updateInterval.getCurrent() * 1000.0F)) {
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
      if (mc.world == null) {
         return "Неизвестно";
      }

      RegistryKey<World> key = mc.world.getRegistryKey();
      if (key == World.OVERWORLD) {
         return "Верхний мир";
      } else if (key == World.NETHER) {
         return "Нижний мир";
      } else if (key == World.END) {
         return "Край";
      } else {
         return key.getValue().toString();
      }
   }

   private String getDimensionKey() {
      if (mc.world == null) {
         return "other";
      }

      RegistryKey<World> key = mc.world.getRegistryKey();
      if (key == World.OVERWORLD) {
         return "overworld";
      } else if (key == World.NETHER) {
         return "nether";
      } else if (key == World.END) {
         return "end";
      } else {
         return "other";
      }
   }

   private int getPing() {
      if (mc.player == null || mc.getNetworkHandler() == null) {
         return -1;
      }

      try {
         var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         return entry == null ? -1 : entry.getLatency();
      } catch (Throwable throwable) {
         return -1;
      }
   }
}
