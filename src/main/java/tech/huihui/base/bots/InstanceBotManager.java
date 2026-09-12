package tech.huihui.base.bots;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.util.math.Vec3d;
import tech.huihui.HuihuiClient;
import tech.huihui.base.bots.net.BotConnection;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.base.events.impl.input.EventMouse;
import tech.huihui.base.events.impl.other.EventGameUpdate;
import tech.huihui.client.modules.impl.misc.FreeCam;
import tech.huihui.client.screens.bots.InstanceBotScreen;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.interfaces.IMinecraft;

public final class InstanceBotManager implements IMinecraft {
   public static final InstanceBotManager INSTANCE = new InstanceBotManager();

   private final List<InstanceBot> bots = new ArrayList<>();
   private final Map<String, BotConnection> connections = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();
   private final Map<String, BotConnection.State> lastStates = new ConcurrentHashMap<>();
   private String defaultServer = "";
   private volatile String focusedName;
   private volatile boolean chatOpen;

   private InstanceBotManager() {
   }

   public void init() {
      EventManager.register(this);
      this.load();
      Runtime.getRuntime().addShutdownHook(new Thread(this::disconnectAll, "huihui-bots-shutdown"));
   }

   public List<InstanceBot> getBots() {
      return this.bots;
   }

   public String getDefaultServer() {
      return this.defaultServer;
   }

   public void setDefaultServer(String server) {
      this.defaultServer = server == null ? "" : server.trim();
      this.save();
   }

   public InstanceBot findByName(String findName) {
      for (InstanceBot bot : this.bots) {
         if (bot.getName().equalsIgnoreCase(findName)) {
            return bot;
         }
      }
      return null;
   }

   public void add(String name, String server) {
      String trimmed = name == null ? "" : name.trim();
      if (trimmed.isEmpty() || this.findByName(trimmed) != null) {
         return;
      }
      String serverTrim = server == null ? "" : server.trim();
      if (serverTrim.isEmpty()) {
         serverTrim = this.defaultServer;
      }
      this.bots.add(new InstanceBot(trimmed, serverTrim));
      this.save();
   }

   public void remove(InstanceBot bot) {
      if (bot == null) {
         return;
      }
      if (bot.getName().equalsIgnoreCase(this.focusedName)) {
         this.releaseFocus();
      }
      this.bots.remove(bot);
      this.disconnectBot(bot.getName());
      this.save();
   }

   public void save() {
      try {
         File parent = new File(HuihuiClient.DIRECTORY, "bots");
         if (parent != null) {
            parent.mkdirs();
         }
         JsonArray array = new JsonArray();
         for (InstanceBot bot : this.bots) {
            JsonObject object = new JsonObject();
            object.addProperty("name", bot.getName());
            object.addProperty("server", bot.getServer());
            array.add(object);
         }
         JsonObject root = new JsonObject();
         root.addProperty("server", this.defaultServer);
         root.add("bots", array);
         try (FileWriter writer = new FileWriter(new File(parent, "instancebots.json"))) {
            writer.write(root.toString());
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }
   }

   private void load() {
      this.bots.clear();
      File file = new File(new File(HuihuiClient.DIRECTORY, "bots"), "instancebots.json");
      if (!file.exists()) {
         return;
      }
      try {
         String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
         JsonObject root = JsonParser.parseString(json).getAsJsonObject();
         if (root.has("server")) {
            this.defaultServer = root.get("server").getAsString();
         }
         if (root.has("bots")) {
            for (JsonElement element : root.getAsJsonArray("bots")) {
               JsonObject object = element.getAsJsonObject();
               if (object.has("name")) {
                  String server = object.has("server") ? object.get("server").getAsString() : this.defaultServer;
                  this.bots.add(new InstanceBot(object.get("name").getAsString(), server));
               }
            }
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }
   }

   public void launch(InstanceBot bot) {
      if (bot == null) {
         return;
      }
      if (bot.getServer().isEmpty()) {
         MessageUtil.displayWarning("У бота " + bot.getName() + " не указан сервер");
         return;
      }
      BotConnection existing = this.connections.get(bot.getName());
      if (existing != null && (existing.getState() == BotConnection.State.CONNECTING
         || existing.getState() == BotConnection.State.CONFIG
         || existing.getState() == BotConnection.State.PLAY)) {
         MessageUtil.displayWarning("Бот " + bot.getName() + " уже подключается");
         return;
      }
      this.connections.remove(bot.getName());
      this.lastStates.remove(bot.getName());
      BotConnection connection = new BotConnection(bot, null, null);
      this.connections.put(bot.getName(), connection);
      connection.start();
   }

   private void disconnectBot(String name) {
      BotConnection connection = this.connections.remove(name);
      if (connection != null) {
         connection.dispose();
      }
      this.lastStates.remove(name);
   }

   private void disconnectAll() {
      for (BotConnection connection : this.connections.values()) {
         connection.dispose();
      }
      this.connections.clear();
   }

   public BotConnection bot(String name) {
      return name == null ? null : this.connections.get(name);
   }

   public String statusText(InstanceBot bot) {
      if (bot == null) {
         return "Оффлайн";
      }
      BotConnection connection = this.connections.get(bot.getName());
      if (connection == null) {
         return "Оффлайн";
      }
      switch (connection.getState()) {
         case PLAY:
            return "В сети";
         case CONNECTING:
         case CONFIG:
            return "Подключение...";
         case ERROR:
            return "Ошибка";
         default:
            return "Отключён";
      }
   }

   public boolean isConnected(String name) {
      BotConnection connection = this.connections.get(name);
      return connection != null && connection.getState() == BotConnection.State.PLAY;
   }

   public String getFocusedName() {
      return this.focusedName;
   }

   public boolean isFocused() {
      return this.focusedName != null;
   }

   public void focusBot(String name) {
      InstanceBot bot = this.findByName(name);
      if (bot == null) {
         MessageUtil.displayWarning("Бот " + name + " не найден");
         return;
      }
      BotConnection connection = this.connections.get(name);
      if (connection == null || connection.getState() != BotConnection.State.PLAY) {
         MessageUtil.displayWarning("Бот " + name + " ещё не подключён");
         return;
      }
      if (this.focusedName != null && !this.focusedName.equalsIgnoreCase(name)) {
         this.releaseFocus();
      }
      mc.setScreen(new InstanceBotScreen());
      this.focusedName = name;
      this.chatOpen = false;
      this.lastCursorX = Double.NaN;
      this.lastCursorY = Double.NaN;
      FreeCam.INSTANCE.beginFollow(new Vec3d(connection.getX(), connection.getY(), connection.getZ()),
         connection.getYaw(), connection.getPitch());
      MessageUtil.displayInfo("Фокус включён на бота " + name + ". Камера следует за ботом.");
   }

   public InstanceBot getFocusedBot() {
      return this.focusedName == null ? null : this.findByName(this.focusedName);
   }

   public void releaseFocus() {
      this.chatOpen = false;
      if (this.focusedName != null) {
         FreeCam.INSTANCE.endFollow();
      }
      this.focusedName = null;
   }

   public void releaseFocusWithMessage() {
      if (this.focusedName != null) {
         MessageUtil.displayInfo("Фокус снят с бота " + this.focusedName);
      }
      this.releaseFocus();
   }

   public boolean isChatOpen() {
      return this.chatOpen;
   }

   public void setChatOpen(boolean open) {
      this.chatOpen = open;
      this.lastCursorX = Double.NaN;
      this.lastCursorY = Double.NaN;
   }

   private double lastCursorX = Double.NaN;
   private double lastCursorY = Double.NaN;

   public void addRawCursorDelta(double cursorX, double cursorY) {
      if (this.focusedName == null || this.chatOpen) {
         this.lastCursorX = Double.NaN;
         this.lastCursorY = Double.NaN;
         return;
      }
      if (!(mc.currentScreen instanceof InstanceBotScreen)) {
         this.lastCursorX = Double.NaN;
         this.lastCursorY = Double.NaN;
         return;
      }
      BotConnection connection = this.focused();
      if (connection == null || connection.getState() != BotConnection.State.PLAY) {
         return;
      }
      if (Double.isNaN(this.lastCursorX) || Double.isNaN(this.lastCursorY)) {
         this.lastCursorX = cursorX;
         this.lastCursorY = cursorY;
         return;
      }
      double dx = cursorX - this.lastCursorX;
      double dy = cursorY - this.lastCursorY;
      this.lastCursorX = cursorX;
      this.lastCursorY = cursorY;
      if (Math.abs(dx) > 60.0D || Math.abs(dy) > 60.0D) {
         this.lastCursorX = Double.NaN;
         this.lastCursorY = Double.NaN;
         return;
      }
      if (dx == 0.0D && dy == 0.0D) {
         return;
      }
      double sensitivity = (Double) mc.options.getMouseSensitivity().getValue() * 0.6D + 0.2D;
      double scaled = sensitivity * sensitivity * sensitivity * 8.0D;
      connection.addLookDelta((float) (-dx * 0.15D * scaled), (float) (dy * 0.15D * scaled));
   }

   @EventTarget
   public void onGameUpdate(EventGameUpdate event) {
      this.drainMessages();
      if (this.focusedName != null) {
         BotConnection connection = this.focused();
         if (connection == null) {
            this.releaseFocusWithMessage();
            return;
         }
         if (connection.getState() != BotConnection.State.PLAY) {
            MessageUtil.displayWarning("Бот " + this.focusedName + " отключился");
            this.releaseFocus();
            return;
         }
         FreeCam.INSTANCE.updateFollow(new Vec3d(connection.getX(), connection.getY(), connection.getZ()),
            connection.getYaw(), connection.getPitch());
      }
      for (InstanceBot bot : this.bots) {
         BotConnection connection = this.connections.get(bot.getName());
         if (connection == null) {
            continue;
         }
         BotConnection.State prev = this.lastStates.get(bot.getName());
         BotConnection.State current = connection.getState();
         if (prev == current) {
            continue;
         }
         this.lastStates.put(bot.getName(), current);
         if (current == BotConnection.State.PLAY) {
            this.messages.add("§aБот " + bot.getName() + " зашёл на сервер");
         } else if (current == BotConnection.State.ERROR) {
            String err = connection.getError();
            this.messages.add("§cБот " + bot.getName() + ": " + (err == null ? "" : err));
         } else if (current == BotConnection.State.DISCONNECTED) {
            String err = connection.getError();
            this.messages.add("§7Бот " + bot.getName() + " отключился" + (err == null ? "" : ": " + err));
         }
      }
   }

   private BotConnection focused() {
      return this.focusedName == null ? null : this.connections.get(this.focusedName);
   }

   private void drainMessages() {
      String message;
      while ((message = this.messages.poll()) != null) {
         MessageUtil.displayInfo(message);
      }
   }

   @EventTarget
   public void onKey(EventKey event) {
      if (this.focusedName == null || this.chatOpen) {
         return;
      }
      if (!(mc.currentScreen instanceof InstanceBotScreen)) {
         return;
      }
      BotConnection connection = this.focused();
      if (connection == null || connection.getState() != BotConnection.State.PLAY) {
         return;
      }
      connection.setKey(event.getKeyCode(), event.getAction() == 1);
   }

   @EventTarget
   public void onMouse(EventMouse event) {
      if (this.focusedName == null || this.chatOpen) {
         return;
      }
      if (!(mc.currentScreen instanceof InstanceBotScreen)) {
         return;
      }
      if (event.getAction() != 1) {
         return;
      }
      BotConnection connection = this.focused();
      if (connection == null || connection.getState() != BotConnection.State.PLAY) {
         return;
      }
      try {
         if (event.getButton() == 0) {
            connection.swingArm();
         } else if (event.getButton() == 1) {
            connection.useItem();
         }
      } catch (Exception e) {
      }
   }
}