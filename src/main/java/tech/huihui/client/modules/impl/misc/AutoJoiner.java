package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.other.EventGameUpdate;
import tech.huihui.base.events.impl.server.EventChatReceive;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "AutoJoiner",
   category = Category.MISC,
   description = "Автоподключение к серверам + перезаход после кика по подозрению в читов"
)
public final class AutoJoiner extends Module {
   public static final AutoJoiner INSTANCE = new AutoJoiner();

   private final ModeSetting mode = new ModeSetting("Режим", "ReallyWorld", "SpookyTime", "WellMine");
   private final NumberSetting rwGriefNumber = new NumberSetting("Номер грифа (RW)", 1.0F, 1.0F, 60.0F, 1.0F);
   private final NumberSetting wmGriefNumber = new NumberSetting("Номер грифа (WellMine)", 1.0F, 1.0F, 3.0F, 1.0F);
   private final NumberSetting delay = new NumberSetting("Задержка клика (мс)", 200.0F, 50.0F, 1000.0F, 10.0F);
   private final BooleanSetting autoRejoin = new BooleanSetting("Перезаходить при кике", true);

   private static final Pattern GRIEF_PATTERN = Pattern.compile("(?i)гриф\\s*#?\\s*(\\d+)");
   private static final Pattern HASH_PATTERN = Pattern.compile("#(\\d+)");
   private static final int MAX_PAGES = 5;
   private static final long RECONNECT_DELAY_MS = 3000L;
   private static final long KICK_DEBOUNCE_MS = 5000L;

   private final Timer timer = new Timer();
   private boolean rejoining;
   private boolean isConnecting;
   private long lastClickTime;
   private int currentPage;
   private boolean needNextPage;
   private int lastMenuSyncId;
   private long menuOpenTime;
   private int autoGrief = -1;
   private int knownServer = -1;
   private String serverAddress;
   private int reconnectAttempts;
   private long kickTime;
   private long lastReconnectTime;
   private long resumeMinerTime;
   private boolean minerWasEnabled;
   private boolean kickPending;
   private int pendingGrief = -1;
   private String pendingAddress;
   private long lastKnownUpdate;

   private AutoJoiner() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.rejoining = false;
      this.isConnecting = false;
      this.autoGrief = -1;
      this.knownServer = -1;
      this.currentPage = 0;
      this.needNextPage = false;
      this.lastMenuSyncId = -1;
      this.menuOpenTime = 0L;
      this.reconnectAttempts = 0;
      this.minerWasEnabled = false;
      this.kickPending = false;
      this.pendingGrief = -1;
      this.pendingAddress = null;
      this.lastClickTime = 0L;
      this.timer.reset();
      MessageUtil.displayInfo("AutoJoiner включён — слежу за киками");
   }

   @Override
   public void onDisable() {
      if (this.minerWasEnabled && !ZarabotokReallyWorld.INSTANCE.isEnabled()) {
         ZarabotokReallyWorld.INSTANCE.setToggled(true);
      }
      this.minerWasEnabled = false;
      this.rejoining = false;
      super.onDisable();
   }

   @EventTarget
   private void onChat(EventChatReceive event) {
      if (!this.autoRejoin.isEnabled()) {
         return;
      }
      String lower = event.getMessage().getString().toLowerCase(Locale.ROOT);
      if (!this.isKickMessage(lower)) {
         return;
      }
      if (System.currentTimeMillis() - this.kickTime < KICK_DEBOUNCE_MS) {
         return;
      }
      int grief = this.readServerNumber();
      if (grief == -1) {
         grief = this.knownServer;
      }
      if (grief == -1) {
         grief = (int) this.rwGriefNumber.getCurrent();
      }
      this.pendingGrief = grief;
      this.pendingAddress = this.currentAddress();
      this.kickTime = System.currentTimeMillis();
      this.kickPending = true;
   }

   @EventTarget
   @Native
   private void onGameUpdate(EventGameUpdate event) {
      if (this.kickPending) {
         this.kickPending = false;
         this.autoGrief = this.pendingGrief;
         this.serverAddress = this.pendingAddress;
         this.lastReconnectTime = 0L;
         this.reconnectAttempts = 0;
         this.rejoining = true;
         this.isConnecting = false;
         this.currentPage = 0;
         this.needNextPage = false;
         this.lastMenuSyncId = -1;
         this.menuOpenTime = 0L;
         this.minerWasEnabled = ZarabotokReallyWorld.INSTANCE.isEnabled();
         if (this.minerWasEnabled) {
            ZarabotokReallyWorld.INSTANCE.setToggled(false);
         }
         MessageUtil.displayInfo("Кик по подозрению в читов — возвращаюсь на гриф #" + this.autoGrief);
      }

      this.updateKnownServer();

      if (!this.rejoining) {
         if (this.minerWasEnabled && System.currentTimeMillis() >= this.resumeMinerTime) {
            if (!ZarabotokReallyWorld.INSTANCE.isEnabled()) {
               ZarabotokReallyWorld.INSTANCE.setToggled(true);
               MessageUtil.displayInfo("ZarabotokReallyWorld продолжает фарм");
            }
            this.minerWasEnabled = false;
         }
         return;
      }

      if (mc.world == null || mc.getNetworkHandler() == null) {
         if (this.reconnectAttempts >= 3) {
            MessageUtil.displayInfo("Не удалось переподключиться за 3 попытки");
            this.finishRejoin(false);
         } else if (this.serverAddress != null
               && System.currentTimeMillis() - this.lastReconnectTime > RECONNECT_DELAY_MS) {
            this.reconnect();
         }
         return;
      }

      if (mc.player == null) {
         return;
      }

      if (this.mode.is("SpookyTime")) {
         this.handleSpookyTime();
      } else if (this.mode.is("ReallyWorld")) {
         this.handleReallyWorld();
      } else if (this.mode.is("WellMine")) {
         this.handleTickWellMine();
      }
   }

   private boolean isKickMessage(String lower) {
      return lower.contains("подозрению в читов")
            || lower.contains("подозрению в читах")
            || lower.contains("подозрению в использовании читов")
            || lower.contains("по подозрению в читах")
            || (lower.contains("вы кикнут") && lower.contains("чит"));
   }

   private void updateKnownServer() {
      if (mc.world == null || mc.player == null) {
         return;
      }
      if (System.currentTimeMillis() - this.lastKnownUpdate < 500L) {
         return;
      }
      this.lastKnownUpdate = System.currentTimeMillis();
      int server = this.readServerNumber();
      if (server != -1) {
         this.knownServer = server;
      }
   }

   private int readServerNumber() {
      if (mc.world == null) {
         return -1;
      }
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objective == null) {
         return -1;
      }
      int parsed = this.parseServer(objective.getDisplayName().getString());
      if (parsed != -1) {
         return parsed;
      }
      for (ScoreboardEntry entry : scoreboard.getScoreboardEntries(objective)) {
         String text = Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()).getString();
         parsed = this.parseServer(text);
         if (parsed != -1) {
            return parsed;
         }
      }
      return -1;
   }

   private int parseServer(String text) {
      Matcher matcher = GRIEF_PATTERN.matcher(text);
      if (matcher.find()) {
         return Integer.parseInt(matcher.group(1));
      }
      matcher = HASH_PATTERN.matcher(text);
      if (matcher.find()) {
         return Integer.parseInt(matcher.group(1));
      }
      return -1;
   }

   private String currentAddress() {
      if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
         return mc.getNetworkHandler().getServerInfo().address;
      }
      if (mc.getCurrentServerEntry() != null) {
         return mc.getCurrentServerEntry().address;
      }
      return null;
   }

   private void reconnect() {
      this.reconnectAttempts++;
      this.lastReconnectTime = System.currentTimeMillis();
      if (this.serverAddress == null || this.serverAddress.isEmpty()) {
         this.finishRejoin(false);
         return;
      }
      try {
         ServerInfo info = new ServerInfo("ReallyWorld", this.serverAddress, ServerInfo.ServerType.OTHER);
         ServerAddress address = ServerAddress.parse(this.serverAddress);
         ConnectScreen.connect(mc.currentScreen, mc, address, info, false, new CookieStorage(Map.of()));
         MessageUtil.displayInfo("Переподключение к " + this.serverAddress + " (" + this.reconnectAttempts + "/3)");
      } catch (Exception exception) {
         MessageUtil.displayInfo("Ошибка переподключения: " + exception.getMessage());
         this.finishRejoin(false);
      }
   }

   private void finishRejoin(boolean success) {
      this.rejoining = false;
      this.isConnecting = false;
      this.autoGrief = -1;
      this.resumeMinerTime = System.currentTimeMillis() + (success ? 3000L : 0L);
      MessageUtil.displayInfo(success ? "Гриф #" + this.pendingGrief + " — AutoJoiner следит за киками" : "Не удалось вернуться на гриф #" + this.pendingGrief);
   }

   // ---- WellMine ----

   private void handleTickWellMine() {
      if (!this.timer.finished((long) this.delay.getCurrent())) {
         return;
      }
      ScreenHandler handler = this.openMenuHandler();
      if (handler != null) {
         this.handleMenuWellMine(handler);
      } else if (mc.currentScreen == null) {
         this.openCompass();
      }
      this.timer.reset();
   }

   private void handleMenuWellMine(ScreenHandler handler) {
      int targetGrief = (int) this.wmGriefNumber.getCurrent();
      for (Slot slot : handler.slots) {
         ItemStack stack = slot.getStack();
         if (stack.isEmpty() || stack.getName() == null) {
            continue;
         }
         if (stack.getName().getString().contains(String.valueOf(targetGrief))) {
            PlayerInventoryUtil.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, false);
            this.timer.reset();
            return;
         }
      }
   }

   // ---- SpookyTime ----

   private void handleSpookyTime() {
      if (System.currentTimeMillis() - this.lastClickTime < 30L) {
         return;
      }
      if (mc.player.getInventory().selectedSlot != 4) {
         mc.player.getInventory().selectedSlot = 4;
         this.sendSelectedSlot(4);
         this.lastClickTime = System.currentTimeMillis();
         return;
      }
      ItemStack compass = mc.player.getInventory().getStack(4);
      if (compass.getItem() == Items.COMPASS && mc.currentScreen == null) {
         if (mc.interactionManager != null) {
            mc.interactionManager.interactItem(mc.player, mc.player.getActiveHand());
            this.lastClickTime = System.currentTimeMillis();
         }
         return;
      }
      ScreenHandler handler = this.openMenuHandler();
      if (handler == null) {
         return;
      }
      for (Slot slot : handler.slots) {
         ItemStack stack = slot.getStack();
         if (stack.isEmpty() || stack.getName() == null) {
            continue;
         }
         String name = stack.getName().getString();
         if (name.contains("Дуэли")) {
            PlayerInventoryUtil.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, false);
            this.isConnecting = true;
            this.lastClickTime = System.currentTimeMillis();
            if (mc.currentScreen != null) {
               mc.player.closeHandledScreen();
            }
            this.finishRejoin(true);
            return;
         }
      }
   }

   // ---- ReallyWorld ----

   private void handleReallyWorld() {
      if (System.currentTimeMillis() - this.lastClickTime < 50L) {
         return;
      }

      ScreenHandler handler = this.openMenuHandler();
      if (handler == null) {
         if (mc.currentScreen == null) {
            this.menuOpenTime = 0L;
            this.openCompass();
            this.lastClickTime = System.currentTimeMillis();
         } else {
            if (this.menuOpenTime == 0L) {
               this.menuOpenTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - this.menuOpenTime > 3000L) {
               mc.player.closeHandledScreen();
               this.menuOpenTime = 0L;
               this.openCompass();
               this.lastClickTime = System.currentTimeMillis();
            }
         }
         return;
      }
      this.menuOpenTime = 0L;

      if (handler.syncId != this.lastMenuSyncId) {
         this.lastMenuSyncId = handler.syncId;
         this.currentPage = 0;
         this.needNextPage = false;
      }

      int target = this.autoGrief != -1 ? this.autoGrief : (int) this.rwGriefNumber.getCurrent();
      Pattern griefName = Pattern.compile("(?i)гриф\\s*#?\\s*" + target + "(?![0-9])");
      boolean foundGrief = false;
      boolean foundGriefSurvival = false;

      if (this.needNextPage) {
         Slot lastSlot = handler.slots.get(handler.slots.size() - 1);
         ItemStack lastStack = lastSlot.getStack();
         if (!lastStack.isEmpty()) {
            String lastName = lastStack.getName() != null ? lastStack.getName().getString() : "";
            if (lastStack.getItem() == Items.ARROW || lastName.contains("Следующая страница")) {
               PlayerInventoryUtil.clickSlot(handler.syncId, lastSlot.id, 0, SlotActionType.PICKUP, false);
               this.currentPage++;
               this.needNextPage = false;
               this.lastClickTime = System.currentTimeMillis();
            }
         }
         return;
      }

      for (Slot slot : handler.slots) {
         ItemStack stack = slot.getStack();
         if (stack.isEmpty() || stack.getName() == null) {
            continue;
         }
         String name = stack.getName().getString();
         String lowerName = name.toLowerCase(Locale.ROOT);

         if (stack.getItem() == Items.ENCHANTING_TABLE) {
            PlayerInventoryUtil.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, false);
            this.lastClickTime = System.currentTimeMillis();
            return;
         }

         if (lowerName.contains("гриферское выживание")) {
            PlayerInventoryUtil.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, false);
            foundGriefSurvival = true;
            this.lastClickTime = System.currentTimeMillis();
            return;
         }

         if (lowerName.contains("следующая страница") && this.needNextPage) {
            PlayerInventoryUtil.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, false);
            this.currentPage++;
            this.needNextPage = false;
            this.lastClickTime = System.currentTimeMillis();
            return;
         }

         if (griefName.matcher(lowerName).find()) {
            PlayerInventoryUtil.clickSlot(handler.syncId, slot.id, 0, SlotActionType.PICKUP, false);
            foundGrief = true;
            this.isConnecting = true;
            this.lastClickTime = System.currentTimeMillis();
            if (mc.currentScreen != null) {
               mc.player.closeHandledScreen();
            }
            this.finishRejoin(true);
            return;
         }
      }

      if (!foundGrief && !foundGriefSurvival && this.currentPage < MAX_PAGES) {
         this.needNextPage = true;
      }
   }

   // ---- helpers ----

   private ScreenHandler openMenuHandler() {
      if (mc.currentScreen == null || mc.player == null) {
         return null;
      }
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (handler == null || handler == mc.player.playerScreenHandler) {
         return null;
      }
      return handler;
   }

   private boolean openCompass() {
      int slot = this.findCompassSlot();
      if (slot == -1) {
         return false;
      }
      if (mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
         this.sendSelectedSlot(slot);
      }
      if (mc.currentScreen == null && mc.interactionManager != null) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }
      return true;
   }

   private int findCompassSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.COMPASS || stack.getItem() == Items.RECOVERY_COMPASS) {
            return i;
         }
      }
      return -1;
   }

   private void sendSelectedSlot(int slot) {
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }
}
