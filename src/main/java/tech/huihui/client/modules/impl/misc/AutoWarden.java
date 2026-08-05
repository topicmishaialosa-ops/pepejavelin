package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.base.events.impl.server.EventChatReceive;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.utility.math.Timer;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "AutoWarden",
   category = Category.MISC,
   description = "Автоматический фарм и депозит лута на Warden-сервере (Baritone)"
)
public final class AutoWarden extends Module {
   public static final AutoWarden INSTANCE = new AutoWarden();
   private static final String Clan = "clan home ";
   private static final int SUPPLY_SEARCH_RADIUS = 32;
   private static final int SUPPLY_SEARCH_Y_RANGE = 1;
   private static final int MIN_INVISIBILITY_POTIONS = 1;
   private static final int MIN_FOOD_ITEMS = 8;
   private static final long SUPPLY_RETRY_DELAY_MS = 15000L;
   private static final long SUPPLY_TIMEOUT_MS = 45000L;
   private static final long SCAN_FIND_TIMEOUT_MS = 750L;
   private static final long CHEST_OPEN_TIMEOUT_MS = 20000L;
   private static final long OPEN_MISSED_TIMEOUT_MS = 10000L;
   private static final long OPEN_CONFIRM_TIMEOUT_MS = 5000L;
   private static final long LOOT_EMPTY_CHECK_DELAY_MS = 500L;
   private static final long TIMED_CHEST_LOOT_GRACE_MS = 3000L;
   private static final long HOLOGRAM_TIMER_SAFETY_MS = 1000L;
   private static final long BARITONE_NO_PATH_RESET_MS = 4000L;
   private static final int LOOT_EMPTY_REQUIRED_CHECKS = 2;
   private static final long MIN_HUB_WAIT_MS = 10000L;
   private static final long HOME_TELEPORT_WAIT_MS = 6000L;
   private static final long ARENA_RETURN_LEAD_MS = 9000L;
   private static final long LAST_KNOWN_CHEST_FALLBACK_MS = 300000L;
   private static final double CHEST_OPEN_RANGE = 3.0;
   private static final double CHEST_OPEN_RANGE_SQ = 9.0;
   private static final double HOLOGRAM_READ_RADIUS_SQ = 4.0;
   private static final String[] DEFAULT_SUPPLY_SIGN_KEYWORDS = new String[]{"инвиз", "невид", "еда", "invis", "food"};
   private static final String[] LOOT_ITEMS = new String[]{"Totem", "Netherite Helmet", "Netherite Chestplate", "Netherite Leggings", "Netherite Boots", "Netherite Sword", "Netherite Pickaxe", "Enchanted Golden Apple", "Player Head", "ENDER_EYE", "Shulker Box", "Netherite Ingot", "Dragon Head", "Elytra", "Snowball", "Splash Potion", "Tripwire Hook", "Netherite Scrap", "Beacon", "Villager Spawn Egg", "DRAGON_HEAD", "NETHERITE_SCRAP", "Paper", "FIREWORK_ROCKET", "PHANTOM_MEMBRANE", "Diamond", "phantom_membrane", "TOTEM_OF_UNDYING", " Golden Apple", "Golden Carrot", "tnt"};
   private static final String[] STRICT_LOOT_ITEM_IDS = new String[]{"totem of undying", "netherite helmet", "netherite chestplate", "netherite leggings", "netherite boots", "netherite sword", "netherite pickaxe", "enchanted golden apple", "shulker box", "netherite ingot", "dragon head", "elytra", "snowball", "splash potion", "phantom_membrane", "tripwire hook", "netherite scrap", "beacon", "ender_eye", "villager spawn egg", "paper", "firework rocket", "phantom membrane", "diamond", "golden apple", "golden carrot", "tnt"};
   private static final Pattern TIME_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*[:\\uFF1A]\\s*(\\d{1,2})(?:\\s*[:\\uFF1A]\\s*(\\d{1,2}))?(?!\\d)");
   private static final Pattern SECONDS_PATTERN = Pattern.compile("(?<![:\\d])(\\d{1,3})\\s*(?:секунд(?:а|ы)?|сек\\.?|sec(?:\\.|ond)?s?|seconds?|с\\.?|s\\.?|c\\.?)");
   private static final Pattern MIN_SEC_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,3})\\s*(?:мин(?:\\.|ут(?:а|ы)?)?|м\\.?|min(?:\\.|ute)?s?|m\\.?)\\s*(?:(\\d{1,2})\\s*(?:секунд(?:а|ы)?|сек\\.?|sec(?:\\.|ond)?s?|seconds?|с\\.?|s\\.?|c\\.?))?");

   private final StringSetting warehouse = new StringSetting("Хом стежа", "home");
   private final StringSetting house = new StringSetting("Хом склада", "st");
   private final StringSetting loot = new StringSetting("Хом лута", "warden");
   private final BooleanSetting autoLoot = new BooleanSetting("Авто лут", true);
   private final BooleanSetting autoDeposit = new BooleanSetting("Авто депозит", true);
   private final BooleanSetting autoSupplies = new BooleanSetting("Авто припасы", true);
   private final StringSetting supplySign = new StringSetting("Название таблички", "инвиз,еда");
   private final NumberSetting openRetryDelay = new NumberSetting("Задержка открытия", 150.0f, 50.0f, 1500.0f, 50.0f);
   private final NumberSetting rejoinLead = new NumberSetting("Заходить за (мс)", 3000.0f, 500.0f, 15000.0f, 100.0f);
   private final StringSetting zaxdod = new StringSetting("Выходить меньше чем", "60");
   private final Timer stateTimer = new Timer();
   private final Timer foodUseTimer = new Timer();
   private final Timer invisibilityUseTimer = new Timer();
   private final Map<BlockPos, Long> ignoredChests = new ConcurrentHashMap<BlockPos, Long>();
   private State state = State.IDLE;
   private int targetAnarchy = -1;
   private BlockPos targetChest;
   private BlockPos lastKnownLootChest;
   private long lastKnownLootChestAt = -1L;
   private long targetOpenTime = -1L;
   private long targetFoundOpenTime = -1L;
   private long scannedChestOpenTime = -1L;
   private int scanIndex = 0;
   private int depositSlotIndex = 0;
   private boolean lootedCurrentChest = false;
   private boolean openedCurrentChest = false;
   private boolean aimedCurrentChest = false;
   private boolean checkingUntimedChest = false;
   private boolean openingTimedChestImmediately = false;
   private boolean pendingDeposit = false;
   private boolean warehouseHomeCommandSent = false;
   private boolean pendingClanStorageWithdraw = false;
   private boolean farmHomeCommandSent = false;
   private boolean supplyHomeCommandSent = false;
   private boolean returnCommandSent = false;
   private long lootContainerOpenedAt = -1L;
   private int emptyLootChecks = 0;
   private boolean pausedByTeleportBossBar = false;
   private long baritoneNoPathSince = -1L;
   private boolean pausedByHomeTeleport = false;
   private long homeTeleportPauseUntil = 0L;
   private State openTimeoutState = null;
   private long openTimeoutStartedAt = -1L;
   private BlockPos lastOpenAttemptChest;
   private long lastOpenAttemptAt = -1L;
   private BlockPos pendingOpenedChest;
   private int openAttemptSyncId = -1;
   private long openAttemptStartedAt = -1L;
   private int chestOpenRecoveries = 0;
   private BlockPos supplyChest;
   private State supplyReturnState = State.RUSH_JOIN;
   private long supplyStartedAt = -1L;
   private long supplyRetryAfter = 0L;
   private boolean aimedSupplyChest = false;
   private boolean initialSupplyPending = false;
   private boolean forceSupplyPending = false;
   private boolean supplyTookInvisibility = false;
   private boolean supplyTookFood = false;
   private boolean autoEatingFood = false;
   private int previousFoodSlot = -1;
   private int foodHotbarSlot = -1;
   private int foodUseDelayTicks = 0;
   private boolean autoDrinkingInvisibility = false;
   private int previousInvisibilitySlot = -1;
   private int invisibilityHotbarSlot = -1;
   private int invisibilityUseDelayTicks = 0;
   private Boolean previousBaritoneFreeLook;
   private Boolean previousBaritoneRightClickContainerOnArrival;

   private AutoWarden() {
      this.supplySign.setVisible(this.autoSupplies::isEnabled);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.state = State.IDLE;
      this.stateTimer.reset();
      this.initialSupplyPending = this.autoSupplies.isEnabled();
      this.supplyRetryAfter = 0L;
      this.resetAllState();
      this.configureBaritone();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.cleanupAll();
      WardenBaritone.cancelEverything(WardenBaritone.getBaritone());
      this.restoreBaritoneSettings();
   }

   @EventTarget
   private void onChat(EventChatReceive event) {
      Text text = event.getMessage();
      if (text == null) {
         return;
      }
      String message = this.cleanString(text.getString());
      if (!message.contains("помянем. вы погибли!")) {
         return;
      }
      WardenBaritone.cancelEverything(WardenBaritone.getBaritone());
      if (mc.player != null && mc.world != null) {
         mc.player.stopRiding();
      }
      this.cleanupAll();
      this.goHomeForSupplies();
   }

   @EventTarget
   private void onTick(EventTick event) {
      try {
         this.processTick();
      } catch (Throwable throwable) {
         this.handleTickError();
      }
   }

   private void processTick() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }
      Object baritone = WardenBaritone.getBaritone();
      if (this.isPlayerDead()) {
         this.stateTimer.reset();
         return;
      }
      if (this.isTeleportingToHub()) {
         this.pausedByTeleportBossBar = true;
         this.stopEatingAndDrinking();
         this.cancelPathing();
         this.resetMovementKeys();
         WardenBaritone.cancelEverything(baritone);
         this.disableAutoJump();
         if (mc.world != null) {
            mc.player.stopRiding();
         }
         this.stateTimer.reset();
         return;
      }
      if (this.pausedByTeleportBossBar) {
         this.pausedByTeleportBossBar = false;
         this.stateTimer.reset();
      }
      if (this.pausedByHomeTeleport) {
         if (System.currentTimeMillis() < this.homeTeleportPauseUntil) {
            return;
         }
         this.pausedByHomeTeleport = false;
         this.stateTimer.reset();
      }
      int supplyStatus = this.getSupplyStatus();
      this.handleBaritonePathingTimeout(baritone);
      this.handleAutoEatAndDrink();
      if (this.shouldHandleSupplies(supplyStatus)) {
         if (this.goToSupply(this.state == State.HUB_WAITING ? State.HUB_WAITING : State.SCAN_NEXT, supplyStatus, false)) {
            return;
         }
         WardenBaritone.cancelEverything(baritone);
         this.disableAutoJump();
         this.stateTimer.reset();
         return;
      }
      switch (this.state) {
         case IDLE: {
            if (this.initialSupplyPending || this.forceSupplyPending) {
               if (!this.autoSupplies.isEnabled()) {
                  this.initialSupplyPending = false;
                  this.forceSupplyPending = false;
               } else {
                  if (this.goToSupply(State.SCAN_NEXT, supplyStatus, this.forceSupplyPending)) {
                     return;
                  }
                  if (this.forceSupplyPending) {
                     this.stateTimer.reset();
                     return;
                  }
                  this.initialSupplyPending = false;
               }
            }
            this.scanIndex = 0;
            this.state = this.isInHub() ? State.HUB_WAITING : State.SCAN_NEXT;
            break;
         }
         case SCAN_NEXT:
         case SCAN_WAIT_JOIN: {
            this.stateTimer.reset();
            this.state = State.SCAN_FIND_HOLOGRAM;
            break;
         }
         case SCAN_FIND_HOLOGRAM: {
            boolean hasTimedChest;
            if (supplyStatus == -1) {
               this.state = this.isInHub() ? State.HUB_WAITING : State.IDLE;
               this.stateTimer.reset();
               return;
            }
            BlockPos nearestHolo = this.findNearestHologram();
            long scannedOpenTime = this.scannedChestOpenTime;
            BlockPos timedChest = this.findTimedChest();
            BlockPos nearestChest = this.findNearestChest();
            BlockPos bestChest = this.pickBestChest(nearestChest, this.pickBestChest(timedChest, nearestHolo));
            boolean bl = hasTimedChest = nearestHolo != null && nearestHolo.equals(bestChest);
            if (bestChest != null) {
               this.targetChest = bestChest;
               this.rememberChest(bestChest);
               this.targetFoundOpenTime = hasTimedChest ? scannedOpenTime : -1L;
               this.lootedCurrentChest = false;
               this.openedCurrentChest = false;
               this.aimedCurrentChest = false;
               this.checkingUntimedChest = !hasTimedChest;
               this.chestOpenRecoveries = 0;
               WardenBaritone.setGoalAndPath(baritone, this.targetChest);
               this.stateTimer.reset();
               this.state = State.SCAN_PATHING;
               break;
            }
            if (!this.stateTimer.finished(SCAN_FIND_TIMEOUT_MS)) {
               break;
            }
            BlockPos lastKnownChest = this.getLastKnownChest();
            if (lastKnownChest != null) {
               this.targetChest = lastKnownChest;
               this.targetFoundOpenTime = -1L;
               this.lootedCurrentChest = false;
               this.openedCurrentChest = false;
               this.aimedCurrentChest = false;
               this.checkingUntimedChest = true;
               this.chestOpenRecoveries = 0;
               WardenBaritone.setGoalAndPath(baritone, this.targetChest);
               this.state = State.SCAN_PATHING;
               this.stateTimer.reset();
               return;
            }
            ++this.scanIndex;
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            break;
         }
         case SCAN_PATHING: {
            if (supplyStatus == -1) {
               this.state = this.isInHub() ? State.HUB_WAITING : State.IDLE;
               this.stateTimer.reset();
               return;
            }
            if (this.targetChest == null) {
               this.targetChest = null;
               this.state = State.SCAN_FIND_HOLOGRAM;
               return;
            }
            double distance = mc.player.getPos().distanceTo(this.targetChest.toCenterPos());
            if (this.isNearChest(this.targetChest) || distance <= 4.0 && !WardenBaritone.isPathing(baritone)) {
               WardenBaritone.cancelEverything(baritone);
               this.stateTimer.reset();
               this.state = State.SCAN_READ_HOLOGRAM;
               break;
            }
            if (!WardenBaritone.isPathing(baritone) && this.stateTimer.finished(2000L)) {
               WardenBaritone.setGoalAndPath(baritone, this.targetChest);
               this.stateTimer.reset();
            }
            break;
         }
         case SCAN_READ_HOLOGRAM: {
            if (supplyStatus == -1) {
               this.state = this.isInHub() ? State.HUB_WAITING : State.IDLE;
               this.stateTimer.reset();
               return;
            }
            if (this.targetChest == null) {
               this.state = State.SCAN_FIND_HOLOGRAM;
               return;
            }
            long timeLeft = this.readChestTime(this.targetChest);
            long timedOpenTime = -1L;
            long timedOpenAt = -1L;
            if (this.targetFoundOpenTime > System.currentTimeMillis()) {
               timedOpenAt = Math.max(0L, (this.targetFoundOpenTime - System.currentTimeMillis() + 999L) / 1000L);
            }
            if (timeLeft >= 0L) {
               timedOpenTime = this.calculateOpenTime(timeLeft);
            }
            if (timedOpenAt >= 0L && (timeLeft < 0L || timedOpenAt < timeLeft)) {
               timeLeft = timedOpenAt;
               timedOpenTime = this.targetFoundOpenTime;
            }
            if (timeLeft < 0L && this.targetFoundOpenTime != -1L) {
               timeLeft = timedOpenAt;
               timedOpenTime = this.targetFoundOpenTime;
            }
            if (timeLeft >= 0L && timedOpenTime != -1L) {
               long timeUntilOpen = timedOpenTime - System.currentTimeMillis();
               this.targetAnarchy = supplyStatus;
               this.returnCommandSent = false;
               this.lootedCurrentChest = false;
               this.openedCurrentChest = false;
               this.aimedCurrentChest = false;
               if (timeUntilOpen > this.getMaxWaitTime()) {
                  this.targetOpenTime = -1L;
                  this.checkingUntimedChest = true;
                  this.openingTimedChestImmediately = false;
                  this.state = State.WAIT_OPEN;
                  this.stateTimer.reset();
                  return;
               }
               this.targetOpenTime = timedOpenTime;
               this.checkingUntimedChest = false;
               this.openingTimedChestImmediately = false;
               this.state = this.shouldSetArenaHome(timeUntilOpen) ? State.ARENA_SET_HOME : State.WAIT_OPEN;
               this.stateTimer.reset();
               break;
            }
            if (timeLeft < 0L) {
               this.targetOpenTime = -1L;
               this.checkingUntimedChest = true;
               this.lootedCurrentChest = false;
               this.openedCurrentChest = false;
               this.aimedCurrentChest = false;
               this.state = State.WAIT_OPEN;
               this.stateTimer.reset();
               break;
            }
            if (!this.stateTimer.finished(4000L)) {
               break;
            }
            this.ignoredChests.put(this.targetChest.toImmutable(), System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
            this.targetChest = null;
            this.checkingUntimedChest = false;
            this.state = State.SCAN_FIND_HOLOGRAM;
            this.stateTimer.reset();
            break;
         }
         case HUB_WAITING: {
            if (this.targetAnarchy == -1 || this.targetOpenTime == -1L) {
               this.state = State.SCAN_FIND_HOLOGRAM;
               this.stateTimer.reset();
               return;
            }
            long timeLeft = this.targetOpenTime - System.currentTimeMillis();
            if (timeLeft < -OPEN_MISSED_TIMEOUT_MS) {
               this.resetChestState();
               this.state = State.SCAN_NEXT;
               this.stateTimer.reset();
               return;
            }
            if (timeLeft > this.getRejoinLead()) {
               break;
            }
            this.state = State.RUSH_JOIN;
            this.stateTimer.reset();
            break;
         }
         case ARENA_SET_HOME: {
            if (this.targetChest == null || this.targetOpenTime == -1L) {
               this.resetChestState();
               this.state = State.SCAN_NEXT;
               this.stateTimer.reset();
               return;
            }
            if (!this.isNearChest(this.targetChest)) {
               this.state = State.RUSH_PATH;
               this.stateTimer.reset();
               return;
            }
            if (mc.world != null) {
               mc.player.stopRiding();
            }
            this.disableAutoJump();
            this.sendCommand(this.getWarehouseHome());
            this.state = State.ARENA_OPEN;
            this.stateTimer.reset();
            break;
         }
         case ARENA_OPEN: {
            if (this.targetOpenTime == -1L) {
               this.resetChestState();
               this.state = State.SCAN_NEXT;
               this.stateTimer.reset();
               return;
            }
            if (this.targetOpenTime - System.currentTimeMillis() <= ARENA_RETURN_LEAD_MS) {
               this.state = State.ARENA_RETURN;
               this.stateTimer.reset();
               return;
            }
            if (this.isInventoryOpen()) {
               if (!this.isChestOpen(this.targetChest)) {
                  break;
               }
               this.state = State.ARENA_WAIT_RETURN;
               this.stateTimer.reset();
               break;
            }
            if (!this.stateTimer.finished(800L)) {
               break;
            }
            this.sendCommand("darena");
            this.stateTimer.reset();
            break;
         }
         case ARENA_WAIT_RETURN: {
            long timeUntilOpen;
            if (this.targetOpenTime == -1L) {
               this.resetChestState();
               this.state = State.SCAN_NEXT;
               this.stateTimer.reset();
               return;
            }
            if (mc.world != null) {
               mc.player.stopRiding();
            }
            if ((timeUntilOpen = this.targetOpenTime - System.currentTimeMillis()) > ARENA_RETURN_LEAD_MS) {
               break;
            }
            this.state = State.ARENA_RETURN;
            this.stateTimer.reset();
            break;
         }
         case ARENA_RETURN: {
            if (mc.world != null) {
               mc.player.stopRiding();
            }
            this.sendCommand(this.getWarehouseHomeCommand());
            this.state = State.ARENA_RETURN_WAIT;
            this.stateTimer.reset();
            break;
         }
         case ARENA_RETURN_WAIT: {
            if (!this.stateTimer.finished(HOME_TELEPORT_WAIT_MS)) {
               break;
            }
            this.aimedCurrentChest = false;
            this.state = this.isNearChest(this.targetChest) ? State.WAIT_OPEN : State.RUSH_PATH;
            this.stateTimer.reset();
            break;
         }
         case RUSH_JOIN: {
            if (this.isNearChest(this.targetChest)) {
               this.aimedCurrentChest = false;
               this.state = State.WAIT_OPEN;
               this.stateTimer.reset();
               break;
            }
            if (this.targetChest != null) {
               WardenBaritone.setGoalAndPath(baritone, this.targetChest);
            }
            this.state = State.RUSH_PATH;
            break;
         }
         case RUSH_PATH: {
            if (this.targetChest == null) {
               this.targetChest = null;
               this.state = State.SCAN_FIND_HOLOGRAM;
               return;
            }
            if (this.isNearChest(this.targetChest)) {
               WardenBaritone.cancelEverything(baritone);
               this.aimedCurrentChest = false;
               this.state = State.WAIT_OPEN;
               this.stateTimer.reset();
               break;
            }
            if (!WardenBaritone.isPathing(baritone) && this.stateTimer.finished(2000L)) {
               WardenBaritone.setGoalAndPath(baritone, this.targetChest);
               this.stateTimer.reset();
            }
            break;
         }
         case WAIT_OPEN: {
            BlockPos currentChest;
            WardenBaritone.cancelEverything(baritone);
            if ((currentChest = this.getCurrentChest()) == null) {
               this.clearCurrentChest();
               this.resetOpenAttempts();
               this.state = State.RUSH_PATH;
               this.stateTimer.reset();
               return;
            }
            if (!this.isNearChest(currentChest)) {
               this.clearCurrentChest();
               this.resetOpenAttempts();
               this.aimedCurrentChest = false;
               this.state = State.RUSH_PATH;
               this.stateTimer.reset();
               return;
            }
            if (this.isChestOpen(currentChest)) {
               this.resetOpenAttempts();
               this.clearCurrentChest();
               this.rememberChest(currentChest);
               this.openedCurrentChest = true;
               this.aimedCurrentChest = false;
               this.chestOpenRecoveries = 0;
               this.lootContainerOpenedAt = System.currentTimeMillis();
               this.emptyLootChecks = 0;
               this.state = State.LOOTING;
               this.stateTimer.reset();
               return;
            }
            if (this.isInventoryOpen()) {
               this.clearCurrentChest();
               this.resetOpenAttempts();
               this.aimedCurrentChest = false;
               this.state = State.RUSH_PATH;
               this.stateTimer.reset();
               return;
            }
            long timeLeft = this.targetOpenTime > 0L ? this.targetOpenTime - System.currentTimeMillis() : 0L;
            if (this.targetOpenTime > 0L && timeLeft > 0L && !this.openingTimedChestImmediately) {
               this.clearCurrentChest();
               this.resetOpenAttempts();
               this.prepareChestOpen(currentChest);
               return;
            }
            if (this.targetOpenTime > 0L && timeLeft < -OPEN_MISSED_TIMEOUT_MS && !this.openingTimedChestImmediately) {
               this.clearCurrentChest();
               this.resetChestState();
               this.state = State.SCAN_NEXT;
               this.stateTimer.reset();
               return;
            }
            if (this.isTimedOut(State.WAIT_OPEN)) {
               this.retryChestOpen(State.WAIT_OPEN);
               return;
            }
            if (this.aimedCurrentChest && !this.isChestStillThere(currentChest)) {
               break;
            }
            if (!this.attemptChestOpen(currentChest, true)) {
               return;
            }
            this.aimedCurrentChest = true;
            this.stateTimer.reset();
            break;
         }
         case LOOTING: {
            if (this.isInventoryOpen()) {
               if (!this.stateTimer.finished(60L)) {
                  break;
               }
               this.lootChest();
               this.stateTimer.reset();
               break;
            }
            if (!this.lootedCurrentChest) {
               if (this.isTimedChestLootGrace()) {
                  this.openedCurrentChest = false;
                  this.aimedCurrentChest = false;
                  this.state = State.WAIT_OPEN;
                  this.stateTimer.reset();
                  return;
               }
               if (this.openedCurrentChest || this.checkingUntimedChest) {
                  if (this.targetChest != null) {
                     this.ignoredChests.put(this.targetChest.toImmutable(), System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
                  }
                  this.targetChest = null;
                  this.openedCurrentChest = false;
                  this.aimedCurrentChest = false;
                  this.checkingUntimedChest = false;
                  this.state = State.SCAN_FIND_HOLOGRAM;
                  this.stateTimer.reset();
                  return;
               }
               this.state = State.WAIT_OPEN;
               this.stateTimer.reset();
               return;
            }
            this.checkingUntimedChest = false;
            this.openedCurrentChest = false;
            this.aimedCurrentChest = false;
            if (this.shouldDepositLoot()) {
               return;
            }
            if (this.autoDeposit.isEnabled() && (this.pendingDeposit || this.hasLootToDeposit())) {
               this.stateTimer.reset();
               this.state = State.CLAN_STORAGE_OPEN;
               break;
            }
            this.pendingDeposit = false;
            this.resetChestState();
            this.scanIndex = 0;
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
            break;
         }
         case CLAN_STORAGE_OPEN: {
            if (!this.hasLootToDeposit()) {
               this.returnToScanning();
               return;
            }
            if (this.isInventoryOpen()) {
               this.depositSlotIndex = 0;
               this.state = State.CLAN_STORAGE_DEPOSIT;
               this.stateTimer.reset();
               break;
            }
            if (!this.stateTimer.finished(800L)) {
               break;
            }
            this.sendCommand("clan storage");
            this.stateTimer.reset();
            break;
         }
         case CLAN_STORAGE_DEPOSIT: {
            if (this.isInventoryOpen()) {
               if (!this.stateTimer.finished(120L)) {
                  break;
               }
               this.depositToClanStorage();
               this.stateTimer.reset();
               break;
            }
            if (this.hasLootToDeposit()) {
               this.pendingClanStorageWithdraw = true;
               this.warehouseHomeCommandSent = false;
               this.state = State.GO_WAREHOUSE;
               this.stateTimer.reset();
               break;
            }
            this.returnToScanning();
            break;
         }
         case SUPPLY_WAIT_JOIN: {
            String homeName = this.getHouseHome();
            if (homeName.isBlank()) {
               this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
               this.finishSupplies(false);
               return;
            }
            if (this.isSupplyTimedOut()) {
               this.supplyStartedAt = System.currentTimeMillis();
            }
            if (!this.supplyHomeCommandSent) {
               if (mc.world != null) {
                  mc.player.stopRiding();
               }
               this.sendCommand(homeName);
               this.supplyHomeCommandSent = true;
               this.stateTimer.reset();
               break;
            }
            if (!this.stateTimer.finished(HOME_TELEPORT_WAIT_MS)) {
               break;
            }
            this.state = State.SUPPLY_PATHING;
            this.stateTimer.reset();
            break;
         }
         case SUPPLY_PATHING: {
            if (this.getHouseHome().isBlank()) {
               this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
               this.finishSupplies(false);
               return;
            }
            if (this.isSupplyTimedOut()) {
               this.supplyStartedAt = System.currentTimeMillis();
               this.supplyChest = null;
            }
            if (this.supplyChest == null) {
               if (this.stateTimer.finished(500L)) {
                  this.supplyChest = this.findSupplyChest();
                  this.stateTimer.reset();
               }
               return;
            }
            if (this.isNearChest(this.supplyChest)) {
               WardenBaritone.cancelEverything(baritone);
               this.aimedSupplyChest = false;
               this.state = State.SUPPLY_OPEN;
               this.stateTimer.reset();
               break;
            }
            if (!WardenBaritone.isPathing(baritone) && this.stateTimer.finished(2000L)) {
               WardenBaritone.setGoalAndPath(baritone, this.supplyChest);
               this.stateTimer.reset();
            }
            break;
         }
         case SUPPLY_OPEN: {
            WardenBaritone.cancelEverything(baritone);
            if (this.isTimedOut(State.SUPPLY_OPEN)) {
               this.retryChestOpen(State.SUPPLY_OPEN);
               return;
            }
            BlockPos supplyChest = this.getSupplyChest();
            if (this.getHouseHome().isBlank() || supplyChest == null || this.isSupplyTimedOut()) {
               this.clearCurrentChest();
               this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
               this.finishSupplies(false);
               return;
            }
            if (!this.isNearChest(supplyChest)) {
               this.clearCurrentChest();
               this.resetOpenAttempts();
               this.aimedSupplyChest = false;
               this.state = State.SUPPLY_PATHING;
               this.stateTimer.reset();
               return;
            }
            if (this.isChestOpen(supplyChest)) {
               this.resetOpenAttempts();
               this.clearCurrentChest();
               this.aimedSupplyChest = false;
               this.state = State.SUPPLY_TAKE;
               this.stateTimer.reset();
               return;
            }
            if (this.isInventoryOpen()) {
               this.clearCurrentChest();
               this.resetOpenAttempts();
               this.stateTimer.reset();
               return;
            }
            if (this.aimedSupplyChest && !this.isChestStillThere(supplyChest)) {
               break;
            }
            if (!this.attemptChestOpen(supplyChest, false)) {
               return;
            }
            this.aimedSupplyChest = true;
            this.openedCurrentChest = true;
            this.stateTimer.reset();
            break;
         }
         case SUPPLY_TAKE: {
            if (this.isInventoryOpen()) {
               if (!this.stateTimer.finished(80L)) {
                  break;
               }
               this.takeSupplies();
               this.stateTimer.reset();
               break;
            }
            this.finishSupplies(false);
            break;
         }
         case GO_WAREHOUSE: {
            String homeName = this.getHouseHome();
            if (homeName.isBlank()) {
               this.state = State.IDLE;
               return;
            }
            if (!this.hasLootToDeposit()) {
               if (this.pendingDeposit && !this.stateTimer.finished(2000L)) {
                  return;
               }
               this.pendingDeposit = false;
               this.resetChestState();
               this.scanIndex = 0;
               this.state = State.SCAN_NEXT;
               this.stateTimer.reset();
               return;
            }
            this.ignoredChests.clear();
            this.targetChest = null;
            if (!this.warehouseHomeCommandSent) {
               if (mc.world != null) {
                  mc.player.stopRiding();
               }
               this.sendCommand(homeName);
               this.warehouseHomeCommandSent = true;
               this.stateTimer.reset();
               return;
            }
            if (!this.stateTimer.finished(HOME_TELEPORT_WAIT_MS)) {
               break;
            }
            this.state = this.pendingClanStorageWithdraw ? State.CLAN_STORAGE_WITHDRAW_OPEN : State.WAREHOUSE_OPEN;
            this.stateTimer.reset();
            break;
         }
         case WAREHOUSE_WAIT_JOIN: {
            this.state = State.GO_WAREHOUSE;
            this.stateTimer.reset();
            break;
         }
         case CLAN_STORAGE_WITHDRAW_OPEN: {
            if (this.isInventoryOpen()) {
               this.pendingClanStorageWithdraw = false;
               this.depositSlotIndex = 0;
               this.state = State.CLAN_STORAGE_WITHDRAW;
               this.stateTimer.reset();
               break;
            }
            if (!this.stateTimer.finished(800L)) {
               break;
            }
            this.sendCommand("clan storage");
            this.stateTimer.reset();
            break;
         }
         case CLAN_STORAGE_WITHDRAW: {
            if (this.isInventoryOpen()) {
               if (!this.stateTimer.finished(120L)) {
                  break;
               }
               this.withdrawFromClanStorage();
               this.stateTimer.reset();
               break;
            }
            this.state = State.WAREHOUSE_OPEN;
            this.stateTimer.reset();
            break;
         }
         case WAREHOUSE_FIND_CHEST:
         case WAREHOUSE_PATHING: {
            this.state = State.WAREHOUSE_OPEN;
            break;
         }
         case WAREHOUSE_OPEN: {
            if (baritone != null && (this.targetChest == null || this.isNearChest(this.targetChest))) {
               WardenBaritone.cancelEverything(baritone);
            }
            if (this.targetChest != null && this.isChestOpen(this.targetChest)) {
               if (!this.isChestCurrent(this.targetChest)) {
                  this.returnToScanning();
                  return;
               }
               this.resetOpenAttempts();
               this.depositSlotIndex = 0;
               this.state = State.DEPOSITING;
               this.stateTimer.reset();
               break;
            }
            if (this.isInventoryOpen()) {
               this.clearCurrentChest();
               this.resetOpenAttempts();
               this.stateTimer.reset();
               break;
            }
            if (!this.hasLootToDeposit()) {
               this.returnToScanning();
               break;
            }
            if (!this.stateTimer.finished(this.getOpenRetryDelay())) {
               break;
            }
            if (this.targetChest == null || !this.isChestCurrent(this.targetChest)) {
               this.targetChest = this.findWarehouseChest();
            }
            if (this.targetChest == null) {
               this.stateTimer.reset();
               return;
            }
            if (!this.isNearChest(this.targetChest)) {
               this.resetOpenAttempts();
               WardenBaritone.setGoalAndPath(baritone, this.targetChest);
               this.stateTimer.reset();
               return;
            }
            if (this.isTimedOut(State.WAREHOUSE_OPEN)) {
               this.retryChestOpen(State.WAREHOUSE_OPEN);
               return;
            }
            if (!this.attemptChestOpen(this.targetChest, false)) {
               break;
            }
            this.stateTimer.reset();
            break;
         }
         case DEPOSITING: {
            if (mc.currentScreen instanceof HandledScreen) {
               if (this.targetChest == null || !this.isChestCurrent(this.targetChest)) {
                  this.returnToScanning();
                  return;
               }
               if (!this.stateTimer.finished(150L)) {
                  break;
               }
               this.depositToChest();
               this.stateTimer.reset();
               break;
            }
            if (this.hasLootToDeposit()) {
               if (this.targetChest != null) {
                  this.ignoredChests.put(this.targetChest.toImmutable(), System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
               }
               this.targetChest = null;
               this.state = State.WAREHOUSE_FIND_CHEST;
               this.stateTimer.reset();
               break;
            }
            this.returnToScanning();
            break;
         }
         case RETURN_FARM_HOME: {
            if (!this.farmHomeCommandSent) {
               if (mc.world != null) {
                  mc.player.stopRiding();
               }
               this.sendCommand(this.getFarmHome());
               this.farmHomeCommandSent = true;
               this.stateTimer.reset();
               return;
            }
            if (!this.stateTimer.finished(HOME_TELEPORT_WAIT_MS)) {
               break;
            }
            this.farmHomeCommandSent = false;
            this.warehouseHomeCommandSent = false;
            this.pendingDeposit = false;
            this.ignoredChests.clear();
            this.resetChestState();
            this.scanIndex = 0;
            this.state = State.SCAN_NEXT;
            this.stateTimer.reset();
         }
      }
   }

   private void returnToScanning() {
      this.clearCurrentChest();
      this.resetOpenAttempts();
      if (this.targetChest != null) {
         this.ignoredChests.put(this.targetChest.toImmutable(), System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
      }
      this.targetChest = null;
      this.depositSlotIndex = 0;
      if (mc.player != null && mc.world != null) {
         this.closeScreenIfOpen();
      }
      this.state = State.WAREHOUSE_FIND_CHEST;
      this.stateTimer.reset();
   }

   private void handleTickError() {
      try {
         this.stopEatingAndDrinking();
         this.cancelPathing();
         this.resetMovementKeys();
         this.disableAutoJump();
         WardenBaritone.cancelEverything(WardenBaritone.getBaritone());
         if (mc.player != null && mc.world != null) {
            mc.player.stopRiding();
         }
         this.resetOpenAttempts();
         this.targetChest = null;
         this.supplyChest = null;
         this.openedCurrentChest = false;
         this.aimedCurrentChest = false;
         this.aimedSupplyChest = false;
         this.checkingUntimedChest = false;
         this.openingTimedChestImmediately = false;
         this.chestOpenRecoveries = 0;
         this.depositSlotIndex = 0;
         this.emptyLootChecks = 0;
         this.pendingClanStorageWithdraw = false;
         this.warehouseHomeCommandSent = false;
         this.farmHomeCommandSent = false;
         this.supplyHomeCommandSent = false;
         this.forceSupplyPending = false;
         this.state = this.initialSupplyPending ? State.IDLE : State.SCAN_NEXT;
         this.stateTimer.reset();
      } catch (Throwable throwable) {
         this.state = State.IDLE;
      }
   }

   private boolean isTimedOut(State state) {
      long currentTime = System.currentTimeMillis();
      if (this.openTimeoutState != state) {
         this.openTimeoutState = state;
         this.openTimeoutStartedAt = currentTime;
         return false;
      }
      if (state == State.WAIT_OPEN && this.openingTimedChestImmediately && this.targetOpenTime > 0L && currentTime <= this.targetOpenTime + CHEST_OPEN_TIMEOUT_MS) {
         return false;
      }
      return this.openTimeoutStartedAt > 0L && currentTime - this.openTimeoutStartedAt > CHEST_OPEN_TIMEOUT_MS;
   }

   private void resetOpenAttempts() {
      this.openTimeoutState = null;
      this.openTimeoutStartedAt = -1L;
      this.resetChestOpenState();
   }

   private void retryChestOpen(State state) {
      this.stopEatingAndDrinking();
      this.disableAutoJump();
      this.resetOpenAttempts();
      if (mc.player != null && mc.world != null) {
         mc.player.stopRiding();
      }
      if (state == State.SUPPLY_OPEN) {
         this.supplyChest = null;
         this.aimedSupplyChest = false;
         this.supplyStartedAt = System.currentTimeMillis();
         this.state = State.SUPPLY_PATHING;
      } else if (state == State.WAREHOUSE_OPEN) {
         if (this.targetChest != null) {
            this.ignoredChests.put(this.targetChest.toImmutable(), System.currentTimeMillis() + 60000L);
         }
         this.targetChest = null;
         this.state = State.WAREHOUSE_FIND_CHEST;
      } else {
         if (this.targetChest != null && this.chestOpenRecoveries < 4) {
            ++this.chestOpenRecoveries;
            this.openedCurrentChest = false;
            this.aimedCurrentChest = false;
            this.checkingUntimedChest = false;
            this.openingTimedChestImmediately = false;
            this.state = this.isNearChest(this.targetChest) ? State.WAIT_OPEN : State.RUSH_PATH;
            this.stateTimer.reset();
            return;
         }
         if (this.targetChest != null) {
            this.ignoredChests.put(this.targetChest.toImmutable(), System.currentTimeMillis() + 60000L);
         }
         this.targetChest = null;
         this.openedCurrentChest = false;
         this.aimedCurrentChest = false;
         this.checkingUntimedChest = false;
         this.openingTimedChestImmediately = false;
         this.chestOpenRecoveries = 0;
         this.state = State.SCAN_FIND_HOLOGRAM;
      }
      this.stateTimer.reset();
   }

   private void handleBaritonePathingTimeout(Object baritone) {
      boolean isPathing;
      if (baritone == null || !this.isBaritonePathingNeeded()) {
         this.baritoneNoPathSince = -1L;
         return;
      }
      boolean hasPath = WardenBaritone.inProgressPresent(baritone);
      boolean hasGoal = WardenBaritone.getGoal(baritone) != null;
      boolean noPath = hasGoal && !WardenBaritone.hasPath(baritone) && !WardenBaritone.isPathing(baritone);
      boolean bl = isPathing = hasPath || noPath;
      if (!isPathing) {
         this.baritoneNoPathSince = -1L;
         return;
      }
      long currentTime = System.currentTimeMillis();
      if (this.baritoneNoPathSince == -1L) {
         this.baritoneNoPathSince = currentTime;
         return;
      }
      if (currentTime - this.baritoneNoPathSince >= BARITONE_NO_PATH_RESET_MS) {
         this.resetBaritonePathing();
      }
   }

   private boolean isBaritonePathingNeeded() {
      return switch (this.state) {
         case SCAN_PATHING, RUSH_PATH, SUPPLY_PATHING, WAREHOUSE_PATHING -> true;
         case WAREHOUSE_OPEN -> this.targetChest != null && !this.isNearChest(this.targetChest);
         default -> false;
      };
   }

   private void resetBaritonePathing() {
      this.cancelPathing();
      this.resetOpenAttempts();
      this.baritoneNoPathSince = -1L;
      this.aimedCurrentChest = false;
      this.aimedSupplyChest = false;
      this.openedCurrentChest = false;
      switch (this.state) {
         case SUPPLY_PATHING: {
            this.supplyChest = null;
            this.state = State.SUPPLY_PATHING;
            break;
         }
         case WAREHOUSE_PATHING:
         case WAREHOUSE_OPEN: {
            this.targetChest = null;
            this.state = State.WAREHOUSE_FIND_CHEST;
            break;
         }
         default: {
            this.targetChest = null;
            this.targetFoundOpenTime = -1L;
            this.scannedChestOpenTime = -1L;
            this.state = State.SCAN_FIND_HOLOGRAM;
         }
      }
      this.stateTimer.reset();
   }

   private void configureBaritone() {
      try {
         if (this.previousBaritoneFreeLook == null) {
            this.previousBaritoneFreeLook = WardenBaritone.getFreeLook();
            this.previousBaritoneRightClickContainerOnArrival = WardenBaritone.getRightClickContainerOnArrival();
         }
         WardenBaritone.setFreeLook(false);
         WardenBaritone.setRightClickContainerOnArrival(false);
      } catch (Throwable throwable) {
      }
   }

   private void restoreBaritoneSettings() {
      try {
         if (this.previousBaritoneFreeLook != null) {
            WardenBaritone.setFreeLook(this.previousBaritoneFreeLook);
            WardenBaritone.setRightClickContainerOnArrival(this.previousBaritoneRightClickContainerOnArrival);
         }
      } catch (Throwable throwable) {
      }
      this.previousBaritoneFreeLook = null;
      this.previousBaritoneRightClickContainerOnArrival = null;
   }

   private BlockPos findNearestHologram() {
      this.scannedChestOpenTime = -1L;
      if (mc.player == null || mc.world == null) {
         return null;
      }
      long currentTime = System.currentTimeMillis();
      Vec3d playerPos = mc.player.getEyePos();
      List<HologramData> holograms = new ArrayList<HologramData>();
      Box searchBox = mc.player.getBoundingBox().expand(150.0);
      for (Entity entity : mc.world.getEntitiesByType(TypeFilter.instanceOf(Entity.class), searchBox, this::isValidHologram)) {
         Long ignoreUntil;
         String text = this.getHologramText(entity);
         long time = this.parseTime(text);
         if (time < 0L) {
            continue;
         }
         BlockPos entityPos = entity.getBlockPos();
         BlockPos chestPos = entityPos.toImmutable();
         if (mc.world.isChunkLoaded(entityPos.getX() >> 4, entityPos.getZ() >> 4)) {
            BlockPos chestBelow = null;
            for (int i = 1; i <= 3; ++i) {
               BlockPos checkPos = entityPos.down(i);
               if (mc.world.getBlockState(checkPos).getBlock() != Blocks.CHEST && mc.world.getBlockState(checkPos).getBlock() != Blocks.TRAPPED_CHEST) {
                  continue;
               }
               chestBelow = checkPos.toImmutable();
               break;
            }
            if (chestBelow == null) {
               continue;
            }
            chestPos = chestBelow;
         }
         if ((ignoreUntil = this.ignoredChests.get(chestPos)) != null && ignoreUntil > currentTime) {
            continue;
         }
         long openTime = currentTime + time * 1000L + HOLOGRAM_TIMER_SAFETY_MS;
         double distance = Math.sqrt(entity.squaredDistanceTo(playerPos));
         HologramData existing = null;
         for (HologramData data : holograms) {
            if (!data.pos.equals(chestPos)) {
               continue;
            }
            existing = data;
            break;
         }
         if (existing == null) {
            holograms.add(new HologramData(chestPos.toImmutable(), openTime, distance));
            continue;
         }
         existing.openTime = Math.max(existing.openTime, openTime);
         existing.distance = Math.min(existing.distance, distance);
      }
      HologramData best = null;
      for (HologramData data : holograms) {
         boolean isTimed = this.isTimedChest(data.openTime);
         boolean isBestTimed = best != null && this.isTimedChest(best.openTime);
         if (best == null) {
            best = data;
            continue;
         }
         if (isTimed && !isBestTimed) {
            best = data;
            continue;
         }
         if (isTimed && isBestTimed) {
            if (data.openTime >= best.openTime && (data.openTime != best.openTime || !(data.distance < best.distance))) {
               continue;
            }
            best = data;
            continue;
         }
         if (isBestTimed || !(data.distance < best.distance)) {
            continue;
         }
         best = data;
      }
      if (best == null) {
         return null;
      }
      this.scannedChestOpenTime = best.openTime;
      return best.pos;
   }

   private BlockPos findTimedChest() {
      if (mc.player == null || mc.world == null) {
         return null;
      }
      long currentTime = System.currentTimeMillis();
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos bestChest = null;
      double bestDistance = Double.MAX_VALUE;
      int horizontalRadius = 8;
      int verticalRadius = 48;
      double maxDistance = 22500.0;
      int chunkX = playerPos.getX() >> 4;
      int chunkZ = playerPos.getZ() >> 4;
      for (int dx = -horizontalRadius; dx <= horizontalRadius; ++dx) {
         for (int dz = -horizontalRadius; dz <= horizontalRadius; ++dz) {
            int cx = chunkX + dx;
            int cz = chunkZ + dz;
            if (!mc.world.getChunkManager().isChunkLoaded(cx, cz)) {
               continue;
            }
            WorldChunk chunk = mc.world.getChunk(cx, cz);
            if (chunk == null) {
               continue;
            }
            for (BlockPos pos : chunk.getBlockEntities().keySet()) {
               Long ignoreUntil;
               BlockPos chestPos = pos.toImmutable();
               if (Math.abs(chestPos.getY() - playerPos.getY()) > verticalRadius) {
                  continue;
               }
               if (!this.isValidChest(chestPos)) {
                  continue;
               }
               if ((ignoreUntil = this.ignoredChests.get(chestPos)) != null && ignoreUntil > currentTime) {
                  continue;
               }
               double distance = this.distance(chestPos, playerPos);
               if (distance > maxDistance) {
                  continue;
               }
               if (!(distance < bestDistance)) {
                  continue;
               }
               bestDistance = distance;
               bestChest = chestPos;
            }
         }
      }
      BlockPos nearestChest = this.findNearestChest(playerPos, currentTime);
      return this.pickBestChest(bestChest, nearestChest);
   }

   private BlockPos findNearestChest(BlockPos playerPos, long currentTime) {
      if (mc.player == null || mc.world == null || playerPos == null) {
         return null;
      }
      BlockPos bestChest = null;
      double bestDistance = Double.MAX_VALUE;
      int radius = 24;
      int verticalRadius = 16;
      for (BlockPos pos : BlockPos.iterateOutwards(playerPos, radius, verticalRadius, radius)) {
         Long ignoreUntil;
         BlockPos chestPos = pos.toImmutable();
         if (!this.isValidChest(chestPos)) {
            continue;
         }
         if ((ignoreUntil = this.ignoredChests.get(chestPos)) != null && ignoreUntil > currentTime) {
            continue;
         }
         double distance = this.distance(chestPos, playerPos);
         if (!(distance < bestDistance)) {
            continue;
         }
         bestDistance = distance;
         bestChest = chestPos;
      }
      return bestChest;
   }

   private BlockPos findNearestChest() {
      if (mc.player == null) {
         return null;
      }
      return this.findNearestChest(mc.player.getBlockPos(), System.currentTimeMillis());
   }

   private double distance(BlockPos pos1, BlockPos pos2) {
      double dx = pos1.getX() - pos2.getX();
      double dy = pos1.getY() - pos2.getY();
      double dz = pos1.getZ() - pos2.getZ();
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private BlockPos pickBestChest(BlockPos chest1, BlockPos chest2) {
      if (mc.player == null) {
         return chest1 != null ? chest1 : chest2;
      }
      if (chest1 == null) {
         return chest2;
      }
      if (chest2 == null) {
         return chest1;
      }
      BlockPos playerPos = mc.player.getBlockPos();
      return this.distance(chest1, playerPos) <= this.distance(chest2, playerPos) ? chest1 : chest2;
   }

   private void rememberChest(BlockPos pos) {
      if (pos == null) {
         return;
      }
      this.lastKnownLootChest = pos.toImmutable();
      this.lastKnownLootChestAt = System.currentTimeMillis();
   }

   private BlockPos getLastKnownChest() {
      if (this.lastKnownLootChest == null || this.lastKnownLootChestAt <= 0L) {
         return null;
      }
      if (System.currentTimeMillis() - this.lastKnownLootChestAt > LAST_KNOWN_CHEST_FALLBACK_MS) {
         return null;
      }
      return this.lastKnownLootChest;
   }

   private boolean attemptChestOpen(BlockPos pos, boolean isLootChest) {
      if (pos == null) {
         return false;
      }
      this.stopEatingAndDrinking();
      this.disableAutoJump();
      this.resetChestOpenState();
      if (!this.switchToEmptyHand(isLootChest)) {
         return false;
      }
      if (!this.isChestValidToOpen(pos)) {
         return false;
      }
      if (!this.isChestInRange(pos, isLootChest)) {
         return false;
      }
      long currentTime = System.currentTimeMillis();
      this.prepareChestOpen(pos, currentTime);
      this.setPendingChest(pos);
      this.lastOpenAttemptChest = pos.toImmutable();
      this.lastOpenAttemptAt = currentTime;
      return true;
   }

   private boolean switchToEmptyHand(boolean isLootChest) {
      if (mc.player == null || mc.interactionManager == null) {
         return false;
      }
      int currentSlot = mc.player.getInventory().selectedSlot;
      ItemStack heldItem = mc.player.getInventory().getStack(currentSlot);
      if (heldItem.isEmpty()) {
         return true;
      }
      if (isLootChest) {
         return this.switchToEmptyHandSlot(currentSlot);
      }
      int emptySlot = this.findEmptyHotbarSlot();
      if (emptySlot != -1) {
         this.switchToSlot(emptySlot);
         return false;
      }
      int freeInventorySlot = this.findFreeInventorySlot();
      if (freeInventorySlot == -1) {
         return true;
      }
      int hotbarSlot = currentSlot;
      this.clickSlot(hotbarSlot, 0, SlotActionType.PICKUP);
      this.clickSlot(freeInventorySlot, 0, SlotActionType.PICKUP);
      return false;
   }

   private boolean switchToEmptyHandSlot(int currentSlot) {
      if (mc.player == null || mc.interactionManager == null) {
         return false;
      }
      int emptySlot = this.findEmptyHotbarSlot();
      if (emptySlot == -1) {
         int freeSlot = this.findFreeInventorySlot();
         if (freeSlot != -1) {
            this.clickSlot(currentSlot, 0, SlotActionType.PICKUP);
            this.clickSlot(freeSlot, 0, SlotActionType.PICKUP);
         }
         return false;
      }
      this.clickSlot(currentSlot, 0, SlotActionType.PICKUP);
      this.clickSlot(emptySlot, 0, SlotActionType.PICKUP);
      return false;
   }

   private void clickSlot(int slotId, int button, SlotActionType actionType) {
      if (mc.player == null || mc.interactionManager == null) {
         return;
      }
      mc.interactionManager.clickSlot(this.currentScreenHandler().syncId, slotId, button, actionType, mc.player);
   }

   private ScreenHandler currentScreenHandler() {
      return mc.player != null ? mc.player.currentScreenHandler : null;
   }

   private boolean isChestValidToOpen(BlockPos pos) {
      return this.isValidChest(pos) && this.isNearChest(pos);
   }

   private boolean isChestInRange(BlockPos pos, boolean isLootChest) {
      if (!this.isNearChest(pos)) {
         return false;
      }
      if (!this.isChestStillThere(pos)) {
         return false;
      }
      long currentTime = System.currentTimeMillis();
      if (this.lastOpenAttemptChest != null && this.lastOpenAttemptChest.equals(pos)) {
         return currentTime - this.lastOpenAttemptAt >= this.getOpenRetryDelay();
      }
      return true;
   }

   private void resetChestOpenState() {
      this.lastOpenAttemptChest = null;
      this.lastOpenAttemptAt = -1L;
      this.pendingOpenedChest = null;
      this.openAttemptSyncId = -1;
      this.openAttemptStartedAt = -1L;
   }

   private void prepareChestOpen(BlockPos pos, long currentTime) {
      this.pendingOpenedChest = pos.toImmutable();
      ScreenHandler handler = this.currentScreenHandler();
      this.openAttemptSyncId = handler != null ? handler.syncId : -1;
      this.openAttemptStartedAt = currentTime;
   }

   private void prepareChestOpen(BlockPos pos) {
      this.prepareChestOpen(pos, System.currentTimeMillis());
   }

   private void setPendingChest(BlockPos pos) {
      if (mc.player == null || mc.interactionManager == null || pos == null) {
         return;
      }
      this.pointAtChest(pos);
      BlockHitResult hitResult = this.getHitResult(pos);
      mc.crosshairTarget = hitResult;
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private boolean pointAtChest(BlockPos pos) {
      if (mc.player == null || pos == null) {
         return false;
      }
      Vec3d targetVec = Vec3d.ofCenter(pos);
      double dx = targetVec.x - mc.player.getX();
      double dy = targetVec.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
      double dz = targetVec.z - mc.player.getZ();
      mc.player.setYaw((float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
      mc.player.setPitch((float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))));
      return true;
   }

   private BlockHitResult getHitResult(BlockPos pos) {
      return new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
   }

   private boolean isInventoryOpen() {
      return mc.currentScreen instanceof HandledScreen && mc.player != null;
   }

   private boolean isChestOpen(BlockPos pos) {
      if (!this.isInventoryOpen() || mc.player == null || pos == null || this.pendingOpenedChest == null) {
         return false;
      }
      if (!this.pendingOpenedChest.equals(pos)) {
         return false;
      }
      if (this.openAttemptStartedAt <= 0L || System.currentTimeMillis() - this.openAttemptStartedAt > OPEN_CONFIRM_TIMEOUT_MS) {
         return false;
      }
      return this.isNearChest(pos);
   }

   private void closeScreenIfOpen() {
      if (mc.currentScreen != null && !this.autoEatingFood && !this.autoDrinkingInvisibility) {
         mc.setScreen(null);
      }
   }

   private void clearCurrentChest() {
      this.closeScreenIfOpen();
   }

   private void cancelPathing() {
      this.closeScreenIfOpen();
   }

   private void stopEatingAndDrinking() {
      this.closeScreenIfOpen();
   }

   private void disableAutoJump() {
      if (mc.player == null) {
         return;
      }
      if (mc.options != null) {
         mc.options.getAutoJump().setValue(false);
      }
      if (mc.player.isSneaking()) {
         mc.player.setSneaking(false);
         mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
      }
   }

   private void resetMovementKeys() {
      this.closeScreenIfOpen();
   }

   private boolean isNearChest(BlockPos pos) {
      return mc.player != null && pos != null && mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= CHEST_OPEN_RANGE_SQ;
   }

   private long readChestTime(BlockPos pos) {
      if (mc.player == null || mc.world == null || pos == null) {
         return -1L;
      }
      Box searchBox = new Box(pos.getX() - 2.0, pos.getY(), pos.getZ() - 2.0, pos.getX() + 3.0, pos.getY() + 7.0, pos.getZ() + 3.0);
      Vec3d centerPos = pos.toCenterPos();
      double bestDistance = Double.MAX_VALUE;
      long bestTime = -1L;
      for (Entity entity : mc.world.getEntitiesByType(TypeFilter.instanceOf(Entity.class), searchBox, this::isValidHologram)) {
         double dx = entity.getX() - centerPos.x;
         double dz = entity.getZ() - centerPos.z;
         double horizontalDist = dx * dx + dz * dz;
         if (entity.getY() + 0.25 < pos.getY() || horizontalDist > HOLOGRAM_READ_RADIUS_SQ) {
            continue;
         }
         long time = this.parseTime(this.getHologramText(entity));
         if (time < 0L) {
            continue;
         }
         if (bestTime == -1L || horizontalDist + 0.25 < bestDistance) {
            bestDistance = horizontalDist;
            bestTime = time;
            continue;
         }
         if (!(Math.abs(horizontalDist - bestDistance) <= 0.25) || time <= bestTime) {
            continue;
         }
         bestTime = time;
      }
      return bestTime;
   }

   private boolean isValidHologram(Entity entity) {
      return entity instanceof ArmorStandEntity && ((ArmorStandEntity)entity).isMarker() || entity instanceof ItemFrameEntity;
   }

   private String getHologramText(Entity entity) {
      String text = "";
      if (entity instanceof ArmorStandEntity && ((ArmorStandEntity)entity).getName() != null) {
         text = ((ArmorStandEntity)entity).getName().getString();
      } else if (entity instanceof ItemFrameEntity && ((ItemFrameEntity)entity).getName() != null) {
         text = ((ItemFrameEntity)entity).getName().getString();
      }
      return this.cleanString(text);
   }

   private String cleanString(String text) {
      return text.replaceAll("§.", "").toLowerCase(Locale.ROOT).trim();
   }

   private long parseTime(String text) {
      if (text == null || text.isBlank()) {
         return -1L;
      }
      text = this.cleanString(text).replace('\u00a0', ' ').replace('\u202f', ' ').replace('\ufe55', ':').replace('\uff1a', ':').replace('\ua789', ':').replace('\u2236', ':');
      Matcher timeMatcher = TIME_PATTERN.matcher(text);
      if (timeMatcher.find()) {
         long hours = Long.parseLong(timeMatcher.group(1));
         long minutes = Long.parseLong(timeMatcher.group(2));
         if (timeMatcher.group(3) == null) {
            return hours * 60L + minutes;
         }
         return hours * 3600L + minutes * 60L + Long.parseLong(timeMatcher.group(3));
      }
      Matcher minSecMatcher = MIN_SEC_PATTERN.matcher(text);
      if (minSecMatcher.find()) {
         long totalSec = Long.parseLong(minSecMatcher.group(1)) * 60L;
         if (minSecMatcher.group(2) != null) {
            totalSec += Long.parseLong(minSecMatcher.group(2));
         }
         return totalSec;
      }
      if (text.contains(":")) {
         return -1L;
      }
      Matcher secMatcher = SECONDS_PATTERN.matcher(text);
      if (secMatcher.find()) {
         return Long.parseLong(secMatcher.group(1));
      }
      return -1L;
   }

   private void sendCommand(String command) {
      if (command == null || command.isBlank()) {
         return;
      }
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatCommand(command.trim());
      }
   }

   private void goHomeForSupplies() {
      this.clearCurrentChest();
      this.stopEatingAndDrinking();
      WardenBaritone.cancelEverything(WardenBaritone.getBaritone());
      this.disableAutoJump();
      if (mc.player != null && mc.world != null) {
         mc.player.stopRiding();
      }
      this.sendCommand("hub");
   }

   private void resetAllState() {
      this.clearCurrentChest();
      this.stopEatingAndDrinking();
      this.resetOpenAttempts();
      this.targetAnarchy = -1;
      this.targetChest = null;
      this.targetOpenTime = -1L;
      this.targetFoundOpenTime = -1L;
      this.scannedChestOpenTime = -1L;
      this.lootedCurrentChest = false;
      this.openedCurrentChest = false;
      this.aimedCurrentChest = false;
      this.checkingUntimedChest = false;
      this.openingTimedChestImmediately = false;
      this.chestOpenRecoveries = 0;
      this.lootContainerOpenedAt = -1L;
      this.emptyLootChecks = 0;
      this.returnCommandSent = false;
      this.warehouseHomeCommandSent = false;
      this.pendingClanStorageWithdraw = false;
      this.farmHomeCommandSent = false;
      this.supplyHomeCommandSent = false;
      this.supplyChest = null;
      this.supplyReturnState = State.RUSH_JOIN;
      this.supplyStartedAt = -1L;
      this.aimedSupplyChest = false;
      this.supplyTookInvisibility = false;
      this.supplyTookFood = false;
   }

   private void cleanupAll() {
      this.clearCurrentChest();
      this.stopEatingAndDrinking();
      this.resetOpenAttempts();
      this.resetAllState();
      this.ignoredChests.clear();
      this.depositSlotIndex = 0;
      this.scanIndex = 0;
      this.pendingDeposit = false;
      this.warehouseHomeCommandSent = false;
      this.pendingClanStorageWithdraw = false;
      this.farmHomeCommandSent = false;
      this.supplyHomeCommandSent = false;
      this.chestOpenRecoveries = 0;
      this.lootContainerOpenedAt = -1L;
      this.emptyLootChecks = 0;
      this.supplyRetryAfter = 0L;
      this.initialSupplyPending = this.autoSupplies.isEnabled();
      this.forceSupplyPending = false;
      this.state = State.IDLE;
      this.stateTimer.reset();
   }

   private int getSupplyStatus() {
      if (mc.player == null) {
         return -1;
      }
      if (!this.isInAnarchyWorld()) {
         return -1;
      }
      return this.needsSupplies() ? 1 : 0;
   }

   private boolean isInAnarchyWorld() {
      String worldPath = mc.player.getWorld().getRegistryKey().getValue().getPath().toLowerCase(Locale.ROOT);
      if (worldPath.equals("overworld") || worldPath.equals("nether") || worldPath.equals("the_end")) {
         return false;
      }
      return worldPath.contains("anarchy");
   }

   private boolean shouldHandleSupplies(int supplyStatus) {
      if (!this.autoSupplies.isEnabled() || supplyStatus <= 0) {
         return false;
      }
      return switch (this.state) {
         case ARENA_SET_HOME, ARENA_OPEN, ARENA_WAIT_RETURN, ARENA_RETURN, ARENA_RETURN_WAIT, SUPPLY_WAIT_JOIN, SUPPLY_PATHING, SUPPLY_OPEN, SUPPLY_TAKE, CLAN_STORAGE_OPEN, CLAN_STORAGE_DEPOSIT, CLAN_STORAGE_WITHDRAW_OPEN, CLAN_STORAGE_WITHDRAW, GO_WAREHOUSE, RETURN_FARM_HOME, WAREHOUSE_WAIT_JOIN, WAREHOUSE_FIND_CHEST, WAREHOUSE_PATHING, WAREHOUSE_OPEN, DEPOSITING -> false;
         default -> true;
      };
   }

   private boolean goToSupply(State returnState, int supplyStatus, boolean force) {
      BlockPos supplyChest;
      if (!this.autoSupplies.isEnabled() || !force && !this.needsSupplies() || mc.currentScreen != null) {
         return false;
      }
      if (this.getHouseHome().isBlank()) {
         return false;
      }
      long currentTime = System.currentTimeMillis();
      if (!force && currentTime < this.supplyRetryAfter) {
         return false;
      }
      this.supplyChest = supplyChest = this.findSupplyChest();
      this.supplyReturnState = returnState;
      this.supplyStartedAt = currentTime;
      this.aimedSupplyChest = false;
      this.supplyTookInvisibility = this.hasInvisibilityPotions();
      this.supplyTookFood = false;
      this.supplyHomeCommandSent = false;
      this.state = this.supplyChest == null ? State.SUPPLY_WAIT_JOIN : (this.isNearChest(this.supplyChest) ? State.SUPPLY_OPEN : State.SUPPLY_PATHING);
      this.stateTimer.reset();
      return true;
   }

   private void goToSuppliesDirect() {
      this.forceSupplyPending = this.autoSupplies.isEnabled();
      this.initialSupplyPending = this.autoSupplies.isEnabled();
      String homeName = this.getHouseHome();
      if (!this.autoSupplies.isEnabled() || homeName.isBlank()) {
         this.sendCommand(this.getFarmHome());
         return;
      }
      if (mc.player != null && mc.world != null) {
         mc.player.stopRiding();
      }
      this.supplyChest = null;
      this.supplyReturnState = State.RETURN_FARM_HOME;
      this.supplyStartedAt = System.currentTimeMillis();
      this.aimedSupplyChest = false;
      this.supplyTookInvisibility = this.hasInvisibilityPotions();
      this.supplyTookFood = false;
      this.sendCommand(homeName);
      this.supplyHomeCommandSent = true;
      this.state = State.SUPPLY_WAIT_JOIN;
      this.stateTimer.reset();
   }

   private boolean isSupplyRelated() {
      return switch (this.state) {
         case SUPPLY_WAIT_JOIN, SUPPLY_PATHING, SUPPLY_OPEN, SUPPLY_TAKE, RETURN_FARM_HOME -> true;
         default -> false;
      };
   }

   private boolean isInHub() {
      if (mc.player == null) {
         return false;
      }
      BlockPos pos = mc.player.getBlockPos();
      return pos.getX() == 0 && pos.getZ() == 0 && pos.getY() == 90;
   }

   private boolean goHomeOnTimeout() {
      if (!this.isInHub()) {
         return false;
      }
      long currentTime = System.currentTimeMillis();
      if (this.pausedByHomeTeleport && currentTime < this.homeTeleportPauseUntil) {
         return true;
      }
      String homeName = this.getHouseHome();
      if (homeName.isBlank()) {
         homeName = Clan + this.house.getValue();
      }
      this.sendCommand(homeName);
      this.stopEatingAndDrinking();
      WardenBaritone.cancelEverything(WardenBaritone.getBaritone());
      this.disableAutoJump();
      if (mc.world != null) {
         mc.player.stopRiding();
      }
      this.pausedByHomeTeleport = true;
      this.homeTeleportPauseUntil = currentTime + 5000L;
      this.stateTimer.reset();
      return true;
   }

   private boolean needsSupplies() {
      return this.getInvisibilityCount() < MIN_INVISIBILITY_POTIONS || this.getFoodCount() < MIN_FOOD_ITEMS;
   }

   private boolean isSupplyTimedOut() {
      return this.supplyStartedAt > 0L && System.currentTimeMillis() - this.supplyStartedAt > SUPPLY_TIMEOUT_MS;
   }

   private void finishSupplies(boolean success) {
      this.clearCurrentChest();
      this.resetOpenAttempts();
      if (mc.player != null && mc.world != null) {
         mc.player.stopRiding();
      }
      this.supplyChest = null;
      this.supplyStartedAt = -1L;
      this.aimedSupplyChest = false;
      this.supplyTookInvisibility = false;
      this.supplyTookFood = false;
      this.supplyHomeCommandSent = false;
      if (!success && this.initialSupplyPending) {
         if (this.forceSupplyPending) {
            this.state = State.IDLE;
            this.stateTimer.reset();
            return;
         }
         this.state = State.IDLE;
         this.stateTimer.reset();
         return;
      }
      if (success) {
         this.initialSupplyPending = false;
         this.forceSupplyPending = false;
         this.returnToLooting();
         return;
      }
      State returnState = this.supplyReturnState == null ? State.RUSH_JOIN : this.supplyReturnState;
      this.state = returnState;
      if (this.state == State.HUB_WAITING) {
         this.returnCommandSent = false;
         this.goHomeForSupplies();
      }
      this.stateTimer.reset();
   }

   private void takeSupplies() {
      ScreenHandler handler = this.currentScreenHandler();
      if (handler == null) {
         return;
      }
      int totalSlots = handler.slots.size();
      if (!this.supplyTookInvisibility) {
         int slot = this.findItemSlot(handler, totalSlots, this::isInvisibilityPotion);
         this.supplyTookInvisibility = true;
         if (slot != -1) {
            if (!this.takeItemFromChest(handler, slot, totalSlots)) {
               return;
            }
            return;
         }
      }
      if (!this.supplyTookFood) {
         int slot = this.findItemSlot(handler, totalSlots, this::isFood);
         this.supplyTookFood = true;
         if (slot != -1) {
            if (!this.takeItemFromChest(handler, slot)) {
               return;
            }
            return;
         }
      }
      this.closeScreenIfOpen();
      this.finishSupplies(true);
   }

   private boolean hasEmptyInventorySlot(ScreenHandler handler) {
      if (handler == null) {
         return false;
      }
      int totalSlots = handler.slots.size();
      for (int i = 0; i < totalSlots; ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (!stack.isEmpty() || !this.isShulkerBox(stack)) {
            continue;
         }
         this.clickSlot(handler.getSlot(i).id, 0, SlotActionType.PICKUP);
         return true;
      }
      return false;
   }

   private boolean isShulkerBox(ItemStack stack) {
      return Registries.ITEM.getId(stack.getItem()).getPath().endsWith("shulker_box");
   }

   private int findItemSlot(ScreenHandler handler, int totalSlots, Predicate<ItemStack> predicate) {
      for (int i = 0; i < totalSlots; ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.isEmpty() || !predicate.test(stack)) {
            continue;
         }
         return i;
      }
      return -1;
   }

   private boolean takeItemFromChest(ScreenHandler handler, int slot, int totalSlots) {
      ItemStack stack = handler.getSlot(slot).getStack();
      int emptySlot = this.findEmptySlot(handler, totalSlots, stack);
      if (emptySlot == -1) {
         this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
         this.closeScreenIfOpen();
         this.finishSupplies(false);
         return false;
      }
      this.clickSlot(handler.getSlot(slot).id, 0, SlotActionType.PICKUP);
      this.clickSlot(handler.getSlot(emptySlot).id, 1, SlotActionType.PICKUP);
      this.clickSlot(handler.getSlot(slot).id, 0, SlotActionType.PICKUP);
      return true;
   }

   private int findEmptySlot(ScreenHandler handler, int totalSlots, ItemStack stack) {
      for (int i = 0; i < totalSlots; ++i) {
         ItemStack currentStack = handler.getSlot(i).getStack();
         if (!currentStack.isEmpty() || !ItemStack.areItemsEqual(currentStack, stack) && currentStack.getCount() >= currentStack.getMaxCount()) {
            continue;
         }
         return i;
      }
      for (int i = 0; i < totalSlots; ++i) {
         if (!handler.getSlot(i).hasStack()) {
            continue;
         }
         return i;
      }
      return -1;
   }

   private boolean takeItemFromChest(ScreenHandler handler, int slot) {
      ItemStack stack = handler.getSlot(slot).getStack();
      if (!this.canTakeItem(stack)) {
         this.supplyRetryAfter = System.currentTimeMillis() + SUPPLY_RETRY_DELAY_MS;
         this.closeScreenIfOpen();
         this.finishSupplies(false);
         return false;
      }
      this.clickSlot(handler.getSlot(slot).id, 0, SlotActionType.QUICK_MOVE);
      return true;
   }

   private BlockPos findSupplyChest() {
      if (mc.player == null || mc.world == null) {
         return null;
      }
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos bestChest = null;
      double bestDistance = Double.MAX_VALUE;
      for (BlockPos pos : BlockPos.iterateOutwards(playerPos, SUPPLY_SEARCH_RADIUS, SUPPLY_SEARCH_Y_RANGE, SUPPLY_SEARCH_RADIUS)) {
         if (!this.isValidChest(pos) || !this.hasSupplySign(pos)) {
            continue;
         }
         double distance = this.distance(playerPos, pos);
         if (!(distance < bestDistance)) {
            continue;
         }
         bestDistance = distance;
         bestChest = pos.toImmutable();
      }
      return bestChest != null ? bestChest : this.getCurrentChest();
   }

   private BlockPos getCurrentChest() {
      HitResult hitResult;
      if (mc.world == null || !((hitResult = mc.crosshairTarget) instanceof BlockHitResult)) {
         return null;
      }
      BlockHitResult blockHit = (BlockHitResult)hitResult;
      BlockPos pos = blockHit.getBlockPos();
      if (!this.isValidChest(pos) || mc.player.getEyePos().distanceTo(pos.toCenterPos()) > 36.0) {
         return null;
      }
      return pos.toImmutable();
   }

   private boolean hasSupplySign(BlockPos pos) {
      if (mc.world == null) {
         return false;
      }
      for (BlockPos checkPos : BlockPos.iterateOutwards(pos, 1, 2, 1)) {
         BlockEntity blockEntity = mc.world.getBlockEntity(checkPos);
         if (!(blockEntity instanceof SignBlockEntity) || !this.isSupplySign((SignBlockEntity)blockEntity)) {
            continue;
         }
         return true;
      }
      return false;
   }

   private boolean isSupplySign(SignBlockEntity sign) {
      String text = this.cleanString(this.getSignText(sign.getFrontText()) + " " + this.getSignText(sign.getBackText()));
      for (String keyword : this.getSupplyKeywords()) {
         if (keyword.isBlank() || !text.contains(this.cleanString(keyword))) {
            continue;
         }
         return true;
      }
      return false;
   }

   private String getSignText(SignText signText) {
      StringBuilder builder = new StringBuilder();
      for (Text line : signText.getMessages(false)) {
         builder.append(' ').append(line.getString());
      }
      return builder.toString().replaceAll("§.", "");
   }

   private List<String> getSupplyKeywords() {
      List<String> keywords = new ArrayList<String>();
      String signText = this.supplySign.getValue();
      if (signText != null) {
         for (String part : signText.split("[,;\\s]+")) {
            String keyword = this.cleanString(part).trim();
            if (keyword.isBlank()) {
               continue;
            }
            keywords.add(keyword);
         }
      }
      if (keywords.isEmpty()) {
         keywords.addAll(List.of(DEFAULT_SUPPLY_SIGN_KEYWORDS));
      }
      return keywords;
   }

   private boolean isSupplyChestNear() {
      return this.supplyChest != null && this.isNearChest(this.supplyChest);
   }

   private BlockPos getSupplyChest() {
      BlockPos chest = this.supplyChest;
      if (chest == null || !this.isValidChest(chest)) {
         return this.findSupplyChest();
      }
      return chest;
   }

   private int getInvisibilityCount() {
      return this.countItems(this::isInvisibilityPotion);
   }

   private boolean hasInvisibilityPotions() {
      if (mc.player == null) {
         return false;
      }
      for (int i = 0; i < 9; ++i) {
         if (!this.isInvisibilityPotion(mc.player.getInventory().getStack(i))) {
            continue;
         }
         return true;
      }
      return false;
   }

   private int getFoodCount() {
      return this.countItems(this::isFood);
   }

   private int countItems(Predicate<ItemStack> predicate) {
      if (mc.player == null) {
         return 0;
      }
      int count = 0;
      for (int i = 0; i < mc.player.getInventory().size(); ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!predicate.test(stack)) {
            continue;
         }
         count += stack.getCount();
      }
      return count;
   }

   private boolean isInvisibilityPotion(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (stack.getItem() != Items.POTION) {
         return false;
      }
      PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
      if (contents != null) {
         for (StatusEffectInstance effect : contents.getEffects()) {
            if (!effect.getEffectType().equals(StatusEffects.INVISIBILITY)) {
               continue;
            }
            return true;
         }
      }
      String name = this.cleanString(stack.getName().getString());
      return name.contains("инвиз") || name.contains("невид") || name.contains("invis");
   }

   private boolean isFood(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.get(DataComponentTypes.FOOD) != null;
   }

   private boolean isLootItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (!this.autoLoot.isEnabled()) {
         return false;
      }
      return this.isValuableLoot(stack);
   }

   private boolean isSelfItem(ItemStack stack) {
      return this.isInvisibilityPotion(stack) || this.isFood(stack);
   }

   private boolean canTakeItem(ItemStack stack) {
      if (mc.player == null || stack == null || stack.isEmpty()) {
         return false;
      }
      for (int i = 0; i < 36; ++i) {
         ItemStack invStack = mc.player.getInventory().getStack(i);
         if (invStack.isEmpty()) {
            return true;
         }
         if (!ItemStack.areItemsEqual(invStack, stack) || invStack.getCount() >= invStack.getMaxCount()) {
            continue;
         }
         return true;
      }
      return false;
   }

   private void lootChest() {
      ScreenHandler handler = this.currentScreenHandler();
      if (handler == null) {
         return;
      }
      boolean looted = false;
      for (int i = 0; i < handler.slots.size(); ++i) {
         Slot slot = handler.getSlot(i);
         if (!slot.hasStack() || !this.isLootItem(slot.getStack())) {
            continue;
         }
         this.clickSlot(slot.id, 0, SlotActionType.QUICK_MOVE);
         looted = true;
      }
      if (looted) {
         this.lootedCurrentChest = true;
         this.emptyLootChecks = 0;
         this.pendingDeposit = true;
      }
      this.checkEmptyChest(handler);
   }

   private void checkEmptyChest(ScreenHandler handler) {
      if (this.isTimedChestLootGrace()) {
         this.emptyLootChecks = 0;
         return;
      }
      if (this.lootContainerOpenedAt > 0L && System.currentTimeMillis() - this.lootContainerOpenedAt < LOOT_EMPTY_CHECK_DELAY_MS) {
         return;
      }
      boolean hasItems = false;
      for (int i = 0; i < handler.slots.size(); ++i) {
         Slot slot = handler.getSlot(i);
         if (!slot.hasStack()) {
            continue;
         }
         hasItems = true;
         if (!this.isLootItem(slot.getStack())) {
            continue;
         }
         this.emptyLootChecks = 0;
         return;
      }
      if (hasItems) {
         this.closeScreenIfOpen();
         return;
      }
      ++this.emptyLootChecks;
      if (this.emptyLootChecks < LOOT_EMPTY_REQUIRED_CHECKS) {
         return;
      }
      this.closeScreenIfOpen();
   }

   private boolean isTimedChestLootGrace() {
      return this.openingTimedChestImmediately && this.targetOpenTime > 0L && System.currentTimeMillis() < this.targetOpenTime + TIMED_CHEST_LOOT_GRACE_MS;
   }

   private boolean isTimedChest(long openTime) {
      return openTime > System.currentTimeMillis();
   }

   private boolean isValuableLoot(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      String name = this.normalizeString(stack.getName().getString());
      String id = this.normalizeString(Registries.ITEM.getId(stack.getItem()).toString());
      for (String strictId : STRICT_LOOT_ITEM_IDS) {
         if (!id.equals(this.normalizeString(strictId))) {
            continue;
         }
         return true;
      }
      for (String lootItem : LOOT_ITEMS) {
         String normalizedLoot = this.normalizeString(lootItem);
         if (normalizedLoot.isEmpty() || this.isStrictMatch(normalizedLoot) || !name.contains(normalizedLoot)) {
            continue;
         }
         return true;
      }
      return false;
   }

   private boolean isStrictMatch(String name) {      for (String strictId : STRICT_LOOT_ITEM_IDS) {
         if (!name.equals(this.normalizeString(strictId))) {
            continue;
         }
         return true;
      }
      return false;
   }

   private String normalizeString(String text) {
      return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("minecraft:", "").replace('_', ' ').trim();
   }

   private void depositToClanStorage() {
      ScreenHandler handler = this.currentScreenHandler();
      if (handler == null) {
         return;
      }
      int startSlot = this.getStartSlot(handler);
      if (startSlot < 0) {
         this.closeScreenIfOpen();
         return;
      }
      for (int i = startSlot + this.depositSlotIndex; i < handler.slots.size(); ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.isEmpty() || !this.isDepositItem(stack)) {
            continue;
         }
         ItemStack original = stack.copy();
         this.clickSlot(handler.getSlot(i).id, 0, SlotActionType.QUICK_MOVE);
         ItemStack newStack = handler.getSlot(i).getStack();
         if (!newStack.isEmpty() && ItemStack.areItemsEqual(newStack, original) && newStack.getCount() >= original.getCount()) {
            this.startDepositNextSlot();
            return;
         }
         this.depositSlotIndex = i - startSlot + 1;
         return;
      }
      this.depositSlotIndex = 0;
      this.closeScreenIfOpen();
   }

   private void startDepositNextSlot() {
      this.depositSlotIndex = 0;
      this.pendingClanStorageWithdraw = true;
      this.warehouseHomeCommandSent = false;
      this.closeScreenIfOpen();
      this.state = State.GO_WAREHOUSE;
      this.stateTimer.reset();
   }

   private void withdrawFromClanStorage() {
      ScreenHandler handler = this.currentScreenHandler();
      if (handler == null) {
         return;
      }
      int totalSlots = handler.slots.size();
      for (int i = this.depositSlotIndex; i < totalSlots; ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.isEmpty() || !this.isDepositItem(stack)) {
            continue;
         }
         if (!this.canTakeItem(stack)) {
            this.pendingClanStorageWithdraw = true;
            this.depositSlotIndex = 0;
            this.closeScreenIfOpen();
            return;
         }
         this.clickSlot(handler.getSlot(i).id, 0, SlotActionType.QUICK_MOVE);
         this.depositSlotIndex = i + 1;
         return;
      }
      this.depositSlotIndex = 0;
      this.pendingClanStorageWithdraw = false;
      this.closeScreenIfOpen();
      this.state = State.WAREHOUSE_OPEN;
      this.stateTimer.reset();
   }

   private int getStartSlot(ScreenHandler handler) {
      int totalSlots = handler.slots.size() - 36;
      return totalSlots >= 0 ? totalSlots : -1;
   }

   private void returnToLooting() {
      this.pendingDeposit = false;
      this.pendingClanStorageWithdraw = false;
      this.warehouseHomeCommandSent = false;
      this.resetChestState();
      this.scanIndex = 0;
      this.state = State.SCAN_NEXT;
      this.stateTimer.reset();
   }

   private void depositToChest() {
      ScreenHandler handler = this.currentScreenHandler();
      if (handler == null) {
         return;
      }
      int totalSlots = handler.slots.size();
      int startSlot = totalSlots - 36;
      for (int i = this.depositSlotIndex; i < 36; ++i) {
         int slotIndex = startSlot + i;
         ItemStack stack = handler.getSlot(slotIndex).getStack();
         if (stack.isEmpty() || !this.isDepositItem(stack)) {
            continue;
         }
         this.clickSlot(handler.getSlot(slotIndex).id, 0, SlotActionType.QUICK_MOVE);
         this.depositSlotIndex = i + 1;
         return;
      }
      this.depositSlotIndex = 0;
      mc.player.stopRiding();
      if (this.hasLootToDeposit()) {
         if (this.targetChest != null) {
            this.ignoredChests.put(this.targetChest.toImmutable(), System.currentTimeMillis() + LAST_KNOWN_CHEST_FALLBACK_MS);
         }
         this.targetChest = null;
         this.state = State.WAREHOUSE_FIND_CHEST;
      } else if (this.pendingClanStorageWithdraw) {
         this.state = State.CLAN_STORAGE_WITHDRAW_OPEN;
      } else {
         this.pendingDeposit = false;
         this.returnToLooting();
      }
      this.stateTimer.reset();
   }

   private boolean isDepositItem(ItemStack stack) {
      return !this.isSelfItem(stack) && this.isValuableLoot(stack);
   }

   private boolean hasLootToDeposit() {
      if (mc.player == null) {
         return false;
      }
      for (int i = 0; i < mc.player.getInventory().size(); ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty() || !this.isDepositItem(stack)) {
            continue;
         }
         return true;
      }
      return false;
   }

   private boolean hasAnyItems() {
      if (mc.player == null) {
         return false;
      }
      for (int i = 0; i < mc.player.getInventory().size(); ++i) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            continue;
         }
         return true;
      }
      return false;
   }

   private void clearExpiredIgnoredChests() {
      long currentTime = System.currentTimeMillis();
      this.ignoredChests.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
   }

   private boolean isTimedChestAvailable() {
      if (this.targetAnarchy == -1 || this.targetOpenTime == -1L || this.targetChest == null) {
         return false;
      }
      return this.isValidTimedChest(this.targetOpenTime);
   }

   private boolean isValidTimedChest(long openTime) {
      if (openTime == -1L || this.shouldDepositLoot()) {
         return false;
      }
      long timeUntilOpen = openTime - System.currentTimeMillis();
      long minWait = Math.max(MIN_HUB_WAIT_MS, this.getRejoinLead() + 2000L);
      return timeUntilOpen <= this.getMaxWaitTime() && timeUntilOpen > minWait;
   }

   private long getOpenRetryDelay() {
      return Math.max(0, (long)this.openRetryDelay.getCurrent());
   }

   private long getRejoinLead() {
      return Math.max(500L, (long)this.rejoinLead.getCurrent());
   }

   private long getMaxWaitTime() {
      try {
         return Math.max(1L, Long.parseLong(this.zaxdod.getValue())) * 1000L;
      } catch (Exception exception) {
         return 60000L;
      }
   }

   private long calculateOpenTime(long timeLeft) {
      return System.currentTimeMillis() + Math.max(0L, timeLeft) * 1000L + HOLOGRAM_TIMER_SAFETY_MS;
   }

   private boolean shouldSetArenaHome(long timeUntilOpen) {
      return timeUntilOpen > ARENA_RETURN_LEAD_MS && timeUntilOpen <= this.getMaxWaitTime();
   }

   private void resetChestState() {
      this.clearCurrentChest();
      this.stopEatingAndDrinking();
      this.resetOpenAttempts();
      this.targetAnarchy = -1;
      this.targetChest = null;
      this.targetOpenTime = -1L;
      this.targetFoundOpenTime = -1L;
      this.scannedChestOpenTime = -1L;
      this.lootedCurrentChest = false;
      this.openedCurrentChest = false;
      this.aimedCurrentChest = false;
      this.checkingUntimedChest = false;
      this.openingTimedChestImmediately = false;
      this.chestOpenRecoveries = 0;
      this.lootContainerOpenedAt = -1L;
      this.emptyLootChecks = 0;
      this.returnCommandSent = false;
      this.warehouseHomeCommandSent = false;
      this.pendingClanStorageWithdraw = false;
      this.farmHomeCommandSent = false;
      this.supplyHomeCommandSent = false;
      this.supplyChest = null;
      this.supplyReturnState = State.RUSH_JOIN;
      this.supplyStartedAt = -1L;
      this.aimedSupplyChest = false;
      this.supplyTookInvisibility = false;
      this.supplyTookFood = false;
   }

   private void blacklistChest(long openTime) {
      if (this.targetChest != null) {
         long blacklistUntil = Math.max(System.currentTimeMillis() + 1000L, openTime - this.getMaxWaitTime());
         this.ignoredChests.put(this.targetChest.toImmutable(), blacklistUntil);
      }
      this.targetAnarchy = -1;
      this.targetChest = null;
      this.targetOpenTime = -1L;
      this.targetFoundOpenTime = -1L;
      this.scannedChestOpenTime = -1L;
      this.lootedCurrentChest = false;
      this.openedCurrentChest = false;
      this.aimedCurrentChest = false;
      this.checkingUntimedChest = false;
      this.openingTimedChestImmediately = false;
      this.returnCommandSent = false;
   }

   private boolean isValidChest(BlockPos pos) {
      if (mc.world == null || pos == null) {
         return false;
      }
      BlockState state = mc.world.getBlockState(pos);
      return state.getBlock() == Blocks.CHEST || state.getBlock() == Blocks.TRAPPED_CHEST || state.getBlock() == Blocks.BARREL || state.getBlock() == Blocks.SHULKER_BOX;
   }

   private boolean isChestStillThere(BlockPos pos) {
      return this.isValidChest(pos) && this.isNearChest(pos);
   }

   private boolean isChestCurrent(BlockPos pos) {
      return this.targetChest != null && this.targetChest.equals(pos);
   }

   private boolean isPlayerDead() {
      return mc.player == null || mc.player.isDead() || mc.player.getHealth() <= 0.0f;
   }

   private boolean isTeleportingToHub() {
      if (mc.inGameHud == null) {
         return false;
      }
      for (ClientBossBar bossBar : mc.inGameHud.getBossBarHud().bossBars.values()) {
         String text = bossBar.getName().getString().trim();
         if (!"Телепортация".equals(text)) {
            continue;
         }
         return true;
      }
      return false;
   }

   private boolean shouldDepositLoot() {
      return this.hasLootToDeposit() && mc.player != null && mc.world != null && mc.currentScreen != null && this.isPvPArena();
   }

   private boolean isPvPArena() {
      if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
         return false;
      }
      for (ClientBossBar bossBar : mc.inGameHud.getBossBarHud().bossBars.values()) {
         String text = this.cleanString(bossBar.getName().getString());
         if (!text.contains("pvp") && !text.contains("пвп")) {
            continue;
         }
         return true;
      }
      return false;
   }

   private String getHouseHome() {
      return this.getSettingString(this.house);
   }

   private String getFarmHome() {
      return this.getSettingString(this.loot);
   }

   private String getWarehouseHome() {
      return this.getSettingString(this.warehouse, "sethome ");
   }

   private String getWarehouseHomeCommand() {
      return this.getSettingString(this.warehouse, "home ");
   }

   private String getSettingString(StringSetting setting) {
      String value = setting.getValue();
      if (value.isBlank()) {
         return "";
      }
      if (value.regionMatches(true, 0, Clan, 0, Clan.length())) {
         return value;
      }
      return Clan + value;
   }

   private String getSettingString(StringSetting setting, String prefix) {
      String value = setting.getValue();
      if (value.isBlank()) {
         return "";
      }
      if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
         return value;
      }
      return prefix + value;
   }

   private String formatTime(long time) {
      if (time <= 0L) {
         return "READY";
      }
      long seconds = (time + 999L) / 1000L;
      long minutes = seconds / 60L;
      long remainingSeconds = seconds % 60L;
      return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds);
   }

   private BlockPos findWarehouseChest() {
      BlockPos currentChest = this.getCurrentChest();
      if (currentChest != null) {
         return currentChest;
      }
      if (mc.player == null || mc.world == null) {
         return null;
      }
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos bestChest = null;
      double bestDistance = Double.MAX_VALUE;
      long currentTime = System.currentTimeMillis();
      for (BlockPos pos : BlockPos.iterateOutwards(playerPos, SUPPLY_SEARCH_RADIUS, SUPPLY_SEARCH_Y_RANGE, SUPPLY_SEARCH_RADIUS)) {
         BlockPos chestPos = pos.toImmutable();
         Long ignoreUntil = this.ignoredChests.get(chestPos);
         if (ignoreUntil != null && ignoreUntil > currentTime) {
            continue;
         }
         if (!this.isValidChest(chestPos)) {
            continue;
         }
         double distance = this.distance(chestPos, playerPos);
         if (!(distance < bestDistance)) {
            continue;
         }
         bestDistance = distance;
         bestChest = chestPos;
      }
      return bestChest;
   }

   private void handleAutoEatAndDrink() {
      if (this.shouldUseInvisibility() && this.drinkInvisibilityPotion()) {
         return;
      }
      if (mc.player.getHungerManager().isNotFull() && this.eatFood()) {
         return;
      }
   }

   private boolean shouldUseInvisibility() {
      StatusEffectInstance invisibility = mc.player.getStatusEffect(StatusEffects.INVISIBILITY);
      return invisibility == null || !invisibility.isAmbient() && invisibility.getDuration() <= 200;
   }

   private boolean drinkInvisibilityPotion() {
      if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) {
         this.stopDrinkingInvisibility();
         return false;
      }
      if (!this.shouldUseInvisibility()) {
         this.stopDrinkingInvisibility();
         return false;
      }
      if (!this.autoDrinkingInvisibility && !this.findInvisibilityPotion()) {
         return false;
      }
      if (this.invisibilityHotbarSlot < 0 || this.invisibilityHotbarSlot > 8 || !this.isInvisibilityPotion(mc.player.getInventory().getStack(this.invisibilityHotbarSlot))) {
         this.stopDrinkingInvisibility();
         return false;
      }
      this.switchToSlot(this.invisibilityHotbarSlot);
      this.disableAutoJump();
      if (this.invisibilityUseDelayTicks > 0) {
         --this.invisibilityUseDelayTicks;
         return true;
      }
      if (!mc.player.isUsingItem()) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }
      if (this.invisibilityUseTimer.finished(7000L)) {
         this.stopDrinkingInvisibility();
         return false;
      }
      return true;
   }

   private boolean findInvisibilityPotion() {
      int slot = this.findHotbarSlot(this::isInvisibilityPotion);
      this.previousInvisibilitySlot = mc.player.getInventory().selectedSlot;
      if (slot != -1) {
         this.invisibilityHotbarSlot = slot;
         this.autoDrinkingInvisibility = true;
         this.invisibilityUseDelayTicks = 1;
         this.invisibilityUseTimer.reset();
         return true;
      }
      int inventorySlot = this.findItemSlotInPlayer(this::isInvisibilityPotion);
      if (inventorySlot == -1) {
         this.stopDrinkingInvisibility();
         return false;
      }
      this.invisibilityHotbarSlot = this.findEmptyHotbarSlot();
      if (this.invisibilityHotbarSlot == -1) {
         this.invisibilityHotbarSlot = this.previousInvisibilitySlot;
      }
      this.switchToSlot(this.invisibilityHotbarSlot);
      this.moveItemToHotbar(inventorySlot, this.invisibilityHotbarSlot);
      this.autoDrinkingInvisibility = true;
      this.invisibilityUseDelayTicks = 2;
      this.invisibilityUseTimer.reset();
      return true;
   }

   private boolean eatFood() {
      if (mc.player == null || mc.interactionManager == null || mc.currentScreen != null) {
         this.stopEatingFood();
         return false;
      }
      if (!mc.player.getHungerManager().isNotFull()) {
         this.stopEatingFood();
         return false;
      }
      if (!this.autoEatingFood && !this.findFood()) {
         return false;
      }
      if (this.foodHotbarSlot < 0 || this.foodHotbarSlot > 8 || !this.isFood(mc.player.getInventory().getStack(this.foodHotbarSlot))) {
         this.stopEatingFood();
         return false;
      }
      this.switchToSlot(this.foodHotbarSlot);
      this.disableAutoJump();
      if (this.foodUseDelayTicks > 0) {
         --this.foodUseDelayTicks;
         return true;
      }
      if (!mc.player.isUsingItem()) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }
      if (this.foodUseTimer.finished(7000L)) {
         this.stopEatingFood();
         return false;
      }
      return true;
   }

   private boolean findFood() {
      int slot = this.findHotbarSlot(this::isFood);
      this.previousFoodSlot = mc.player.getInventory().selectedSlot;
      if (slot != -1) {
         this.foodHotbarSlot = slot;
         this.autoEatingFood = true;
         this.foodUseDelayTicks = 1;
         this.foodUseTimer.reset();
         return true;
      }
      int inventorySlot = this.findItemSlotInPlayer(this::isFood);
      if (inventorySlot == -1) {
         this.stopEatingFood();
         return false;
      }
      this.foodHotbarSlot = this.findEmptyHotbarSlot();
      if (this.foodHotbarSlot == -1) {
         this.foodHotbarSlot = this.previousFoodSlot;
      }
      this.switchToSlot(this.foodHotbarSlot);
      this.moveItemToHotbar(inventorySlot, this.foodHotbarSlot);
      this.autoEatingFood = true;
      this.foodUseDelayTicks = 2;
      this.foodUseTimer.reset();
      return true;
   }

   private int findItemSlotInPlayer(Predicate<ItemStack> predicate) {
      if (mc.player == null) {
         return -1;
      }
      for (int i = 0; i < mc.player.getInventory().size(); ++i) {
         if (!predicate.test(mc.player.getInventory().getStack(i))) {
            continue;
         }
         return i;
      }
      return -1;
   }

   private void moveItemToHotbar(int inventorySlot, int hotbarSlot) {
      if (inventorySlot == hotbarSlot) {
         return;
      }
      this.clickSlot(inventorySlot, 0, SlotActionType.PICKUP);
      this.clickSlot(hotbarSlot, 0, SlotActionType.PICKUP);
   }

   private int findHotbarSlot(Predicate<ItemStack> predicate) {
      if (mc.player == null) {
         return -1;
      }
      for (int i = 0; i < 9; ++i) {
         if (!predicate.test(mc.player.getInventory().getStack(i))) {
            continue;
         }
         return i;
      }
      return -1;
   }

   private int findEmptyHotbarSlot() {
      return this.findHotbarSlot(ItemStack::isEmpty);
   }

   private int findFreeInventorySlot() {
      if (mc.player == null) {
         return -1;
      }
      for (int i = 9; i < 36; ++i) {
         if (!mc.player.getInventory().getStack(i).isEmpty()) {
            continue;
         }
         return i;
      }
      return -1;
   }

   private void switchToSlot(int slot) {
      if (mc.player == null || mc.player.networkHandler == null || slot < 0 || slot > 8) {
         return;
      }
      if (mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private void stopEatingFood() {
      if (!this.autoEatingFood) {
         this.resetFoodState();
         return;
      }
      this.closeScreenIfOpen();
      if (this.previousFoodSlot != -1) {
         this.switchToSlot(this.previousFoodSlot);
      }
      this.resetFoodState();
   }

   private void stopDrinkingInvisibility() {
      if (!this.autoDrinkingInvisibility) {
         this.resetInvisibilityState();
         return;
      }
      this.closeScreenIfOpen();
      if (this.previousInvisibilitySlot != -1) {
         this.switchToSlot(this.previousInvisibilitySlot);
      }
      this.resetInvisibilityState();
   }

   private void resetFoodState() {
      this.autoEatingFood = false;
      this.previousFoodSlot = -1;
      this.foodHotbarSlot = -1;
      this.foodUseDelayTicks = 0;
      this.foodUseTimer.reset();
   }

   private void resetInvisibilityState() {
      this.autoDrinkingInvisibility = false;
      this.previousInvisibilitySlot = -1;
      this.invisibilityHotbarSlot = -1;
      this.invisibilityUseDelayTicks = 0;
      this.invisibilityUseTimer.reset();
   }

   @EventTarget
   private void onHudRender(EventHudRender event) {
      if (mc.player == null) {
         return;
      }
      String status = switch (this.state) {
         case SUPPLY_WAIT_JOIN -> "Иду на склад за инвизом";
         case SUPPLY_PATHING -> this.supplyChest == null ? "Ищу сундук инвиза" : "Иду к сундуку инвиза";
         case SUPPLY_OPEN, SUPPLY_TAKE -> "Беру инвиз и еду";
         case CLAN_STORAGE_OPEN, CLAN_STORAGE_DEPOSIT -> "Складываю лут в clan storage";
         case GO_WAREHOUSE -> "Иду на склад";
         case CLAN_STORAGE_WITHDRAW_OPEN, CLAN_STORAGE_WITHDRAW -> "Забираю лут из clan storage";
         default -> this.targetAnarchy != -1 && this.targetOpenTime != -1L ? "Открытие через " + this.formatTime(this.targetOpenTime - System.currentTimeMillis()) : "Поиск сундука";
      };
      float width = Fonts.REGULAR.getWidth(status, 7.0F) + 24.0F;
      float x = mc.getWindow().getScaledWidth() / 2.0F - width / 2.0F;
      float y = 50.0F;
      event.getContext().drawRoundedRect(x, y, width, 18.0F, BorderRadius.all(6.0F), new ColorRGBA(0, 0, 0, 180));
      event.getContext().drawText(Fonts.REGULAR.getFont(7.0F), status, x + 12.0F, y + 5.0F, ColorRGBA.WHITE);
   }

   private static final class HologramData {
      final BlockPos pos;
      long openTime;
      double distance;

      HologramData(BlockPos pos, long openTime, double distance) {
         this.pos = pos;
         this.openTime = openTime;
         this.distance = distance;
      }
   }

   private static final class WardenBaritone {
      private static final boolean PRESENT;
      private static Method getProvider;
      private static Method getSettingsMethod;
      private static Method getPrimaryBaritone;
      private static Method getPathingBehavior;
      private static Method getCustomGoalProcess;
      private static Method getGoal;
      private static Method isPathing;
      private static Method hasPath;
      private static Method getInProgress;
      private static Method setGoalAndPath;
      private static Method cancelEverything;
      private static Method optionalIsPresent;
      private static Constructor<?> goalNearConstructor;
      private static Object settings;
      private static Field freeLookField;
      private static Field rightClickContainerOnArrivalField;
      private static Field settingValueField;

      static {
         boolean present = false;
         try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            getProvider = api.getMethod("getProvider");
            getSettingsMethod = api.getMethod("getSettings");
            Class<?> provider = Class.forName("baritone.api.IBaritoneProvider");
            getPrimaryBaritone = provider.getMethod("getPrimaryBaritone");
            Class<?> iBaritone = Class.forName("baritone.api.IBaritone");
            getPathingBehavior = iBaritone.getMethod("getPathingBehavior");
            getCustomGoalProcess = iBaritone.getMethod("getCustomGoalProcess");
            Class<?> pathingBehavior = Class.forName("baritone.api.behavior.IPathingBehavior");
            getGoal = pathingBehavior.getMethod("getGoal");
            isPathing = pathingBehavior.getMethod("isPathing");
            hasPath = pathingBehavior.getMethod("hasPath");
            getInProgress = pathingBehavior.getMethod("getInProgress");
            cancelEverything = pathingBehavior.getMethod("cancelEverything");
            Class<?> customGoalProcess = Class.forName("baritone.api.process.ICustomGoalProcess");
            setGoalAndPath = customGoalProcess.getMethod("setGoalAndPath", Class.forName("baritone.api.pathing.goals.Goal"));
            goalNearConstructor = Class.forName("baritone.api.pathing.goals.GoalNear").getConstructor(BlockPos.class, int.class);
            optionalIsPresent = Optional.class.getMethod("isPresent");
            present = true;
         } catch (Throwable throwable) {
         }
         PRESENT = present;
      }

      private static void ensureSettings() {
         if (settings != null || !PRESENT) {
            return;
         }
         try {
            settings = getSettingsMethod.invoke(null);
            freeLookField = settings.getClass().getField("freeLook");
            rightClickContainerOnArrivalField = settings.getClass().getField("rightClickContainerOnArrival");
            settingValueField = freeLookField.getType().getField("value");
         } catch (Throwable throwable) {
         }
      }

      static Object getBaritone() {
         if (!PRESENT) {
            return null;
         }
         try {
            return getPrimaryBaritone.invoke(getProvider.invoke(null));
         } catch (Throwable throwable) {
            return null;
         }
      }

      static void cancelEverything(Object baritone) {
         if (baritone == null) {
            return;
         }
         try {
            cancelEverything.invoke(getPathingBehavior.invoke(baritone));
         } catch (Throwable throwable) {
         }
      }

      static boolean isPathing(Object baritone) {
         if (baritone == null) {
            return false;
         }
         try {
            return (Boolean)isPathing.invoke(getPathingBehavior.invoke(baritone));
         } catch (Throwable throwable) {
            return false;
         }
      }

      static boolean hasPath(Object baritone) {
         if (baritone == null) {
            return false;
         }
         try {
            return (Boolean)hasPath.invoke(getPathingBehavior.invoke(baritone));
         } catch (Throwable throwable) {
            return false;
         }
      }

      static Object getGoal(Object baritone) {
         if (baritone == null) {
            return null;
         }
         try {
            return getGoal.invoke(getPathingBehavior.invoke(baritone));
         } catch (Throwable throwable) {
            return null;
         }
      }

      static boolean inProgressPresent(Object baritone) {
         if (baritone == null) {
            return false;
         }
         try {
            Object optional = getInProgress.invoke(getPathingBehavior.invoke(baritone));
            return (Boolean)optionalIsPresent.invoke(optional);
         } catch (Throwable throwable) {
            return false;
         }
      }

      static void setGoalAndPath(Object baritone, BlockPos pos) {
         if (baritone == null || pos == null) {
            return;
         }
         try {
            Object goal = goalNearConstructor.newInstance(pos.toImmutable(), 1);
            setGoalAndPath.invoke(getCustomGoalProcess.invoke(baritone), goal);
         } catch (Throwable throwable) {
         }
      }

      static Boolean getFreeLook() {
         ensureSettings();
         try {
            return (Boolean)settingValueField.get(freeLookField.get(settings));
         } catch (Throwable throwable) {
            return null;
         }
      }

      static Boolean getRightClickContainerOnArrival() {
         ensureSettings();
         try {
            return (Boolean)settingValueField.get(rightClickContainerOnArrivalField.get(settings));
         } catch (Throwable throwable) {
            return null;
         }
      }

      static void setFreeLook(boolean value) {
         ensureSettings();
         try {
            settingValueField.set(freeLookField.get(settings), value);
         } catch (Throwable throwable) {
         }
      }

      static void setRightClickContainerOnArrival(boolean value) {
         ensureSettings();
         try {
            settingValueField.set(rightClickContainerOnArrivalField.get(settings), value);
         } catch (Throwable throwable) {
         }
      }
   }

   private enum State {
      IDLE,
      SCAN_NEXT,
      SCAN_WAIT_JOIN,
      SCAN_FIND_HOLOGRAM,
      SCAN_PATHING,
      SCAN_READ_HOLOGRAM,
      HUB_WAITING,
      ARENA_SET_HOME,
      ARENA_OPEN,
      ARENA_WAIT_RETURN,
      ARENA_RETURN,
      ARENA_RETURN_WAIT,
      RUSH_JOIN,
      RUSH_PATH,
      WAIT_OPEN,
      LOOTING,
      SUPPLY_WAIT_JOIN,
      SUPPLY_PATHING,
      SUPPLY_OPEN,
      SUPPLY_TAKE,
      CLAN_STORAGE_OPEN,
      CLAN_STORAGE_DEPOSIT,
      CLAN_STORAGE_WITHDRAW_OPEN,
      CLAN_STORAGE_WITHDRAW,
      GO_WAREHOUSE,
      RETURN_FARM_HOME,
      WAREHOUSE_WAIT_JOIN,
      WAREHOUSE_FIND_CHEST,
      WAREHOUSE_PATHING,
      WAREHOUSE_OPEN,
      DEPOSITING
   }
}
