package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.base.events.impl.server.EventChatReceive;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.other.BaritoneUtil;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "ZarabotokReallyWorld",
   category = Category.MISC,
   description = "Авто-фарм ресурсов и продажа на маркете (ReallyWorld)"
)
public final class ZarabotokReallyWorld extends Module {
   public static final ZarabotokReallyWorld INSTANCE = new ZarabotokReallyWorld();

   private final ModeSetting resource = new ModeSetting("Ресурс", "Паутина", "Древние обломки");
   private final ModeSetting priceMode = new ModeSetting("Расчёт цены", "Минимальная", "Средняя");
   private final NumberSetting stackTarget = new NumberSetting("Стопка (шт)", 64.0F, 1.0F, 2304.0F, 1.0F);
   private final NumberSetting marginPercent = new NumberSetting("Отнять % от цены", 10.0F, 0.0F, 50.0F, 1.0F);
   private final BooleanSetting sellToggle = new BooleanSetting("Продавать на маркете", true);
   private final BooleanSetting depositToggle = new BooleanSetting("Складывать в сундуки", false);
   private final NumberSetting depositStacks = new NumberSetting("Стопки для склада", 4.0F, 1.0F, 36.0F, 1.0F);
   private final NumberSetting chestRadius = new NumberSetting("Радиус сундуков (блоки)", 15.0F, 5.0F, 50.0F, 1.0F);
   private final NumberSetting rtpCooldownSeconds = new NumberSetting("Кулдаун /rtp (сек)", 30.0F, 5.0F, 300.0F, 5.0F);
   private final BooleanSetting rtpOnDamage = new BooleanSetting("Телепорт при уроне", false);
   private final BooleanSetting detectHostiles = new BooleanSetting("Детект враждебных мобов", false);

   private enum Stage {
      SHEARS,
      NETHER_WALK,
      RTP,
      WAIT_TP,
      MINE,
      MARKET_OPEN,
      MARKET_SELECT,
      MARKET_READ,
      SELL,
      CLOSE_MARKET,
      DEPOSIT_HOME,
      DEPOSIT_FIND,
      DEPOSIT_WALK,
      DEPOSIT_OPEN,
      DEPOSIT_PUT,
      DEPOSIT_DROP
   }

   private record Profile(String block, Item item, String query) {
   }

   private Stage stage = Stage.SHEARS;
   private final Timer timer = new Timer();
   private boolean cmdSent;
   private boolean baritoneStarted;
   private boolean walkStarted;
   private int sellPrice;
   private BlockPos currentChest;
   private final Set<BlockPos> triedChests = new HashSet<>();
   private final Set<BlockPos> protectedSpots = new HashSet<>();
   private boolean rtpCooldown;
   private final Timer rtpCooldownTimer = new Timer();
   private int rtpAttempts;
   private Vec3d rtpStartPos;
   private int shearsStep;
   private final Timer shearsTimer = new Timer();
   private int craftSubStep;
   private int craftSlotA;
   private int craftSlotB;
   private boolean craftNeedSecond;   // надо ли брать железо из второго слота (countA == 1)
   private float lastHealth;
   private final Timer damageTpTimer = new Timer();
   private final Timer hostileTpTimer = new Timer();
   private Vec3d walkTarget;
   private int collectedCount;

   private ZarabotokReallyWorld() {
   }

   private Profile profile() {
      String key = this.resource.get();
      if ("Древние обломки".equals(key)) {
         return new Profile("ancient_debris", Items.ANCIENT_DEBRIS, "Древние обломки");
      }
      return new Profile("cobweb", Items.COBWEB, "Паутина");
   }

   private boolean isAncientDebris() {
      return "Древние обломки".equals(this.resource.get());
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.stage = this.isAncientDebris() ? Stage.NETHER_WALK : Stage.SHEARS;
      this.cmdSent = false;
      this.baritoneStarted = false;
      this.walkStarted = false;
      this.sellPrice = 0;
      this.currentChest = null;
      this.triedChests.clear();
      this.rtpCooldown = false;
      this.rtpAttempts = 0;
      this.rtpStartPos = null;
      this.shearsStep = 0;
      this.walkTarget = null;
      this.collectedCount = 0;
      this.timer.reset();
      this.lastHealth = -1.0F;
      this.damageTpTimer.reset();
      this.hostileTpTimer.reset();
   }

   @Override
   public void onDisable() {
      if (this.baritoneStarted && mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatMessage("#stop");
      }
      this.baritoneStarted = false;
      super.onDisable();
   }

   @EventTarget
   private void onUpdate(EventUpdate e) {
      if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
         return;
      }
      this.checkDamageTeleport();
      this.checkHostileMobs();
      switch (this.stage) {
         case SHEARS -> this.stageShears();
         case NETHER_WALK -> this.stageNetherWalk();
         case RTP -> this.stageRtp();
         case WAIT_TP -> this.stageWaitTp();
         case MINE -> this.stageMine();
         case MARKET_OPEN -> this.stageMarketOpen();
         case MARKET_SELECT -> this.stageMarketSelect();
         case MARKET_READ -> this.stageMarketRead();
         case SELL -> this.stageSell();
         case CLOSE_MARKET -> this.stageCloseMarket();
         case DEPOSIT_HOME -> this.stageDepositHome();
         case DEPOSIT_FIND -> this.stageDepositFind();
         case DEPOSIT_WALK -> this.stageDepositWalk();
         case DEPOSIT_OPEN -> this.stageDepositOpen();
         case DEPOSIT_PUT -> this.stageDepositPut();
         case DEPOSIT_DROP -> this.stageDepositDrop();
      }
   }

   @EventTarget
   private void onChat(EventChatReceive event) {
      String lower = event.getMessage().getString().toLowerCase(Locale.ROOT);
      if (this.stage == Stage.RTP && this.cmdSent && this.isRtpCooldown(lower)) {
         this.rtpCooldown = true;
         this.rtpCooldownTimer.reset();
      }
      if (this.stage == Stage.MINE && this.baritoneStarted && this.isProtectedArea(lower)) {
         this.baritoneStarted = false;
         this.protectedSpots.add(mc.player.getBlockPos());
         this.rtpAttempts = 0;
         this.cmdSent = false;
         this.send("#stop");
         MessageUtil.displayInfo("Регион защищён — телепортируюсь (/rtp)");
         this.stage = this.isAncientDebris() ? Stage.NETHER_WALK : Stage.RTP;
      }
   }

   private boolean isProtectedArea(String lower) {
      return lower.contains("не можете сломать")
            || lower.contains("не можете ломать")
            || lower.contains("нельзя сломать")
            || lower.contains("нельзя ломать")
            || lower.contains("cannot break")
            || lower.contains("защищен")
            || lower.contains("защищён")
            || lower.contains("приват");
   }

   private boolean isNearProtectedSpot() {
      for (BlockPos spot : this.protectedSpots) {
         if (mc.player.squaredDistanceTo(Vec3d.ofCenter(spot)) < 400.0) {
            return true;
         }
      }
      return false;
   }

   private void checkDamageTeleport() {
      float health = mc.player.getHealth();
      boolean tookDamage = this.lastHealth >= 0.0F && health < this.lastHealth - 0.5F;
      this.lastHealth = health;
      if (this.rtpOnDamage.isEnabled() && tookDamage && this.damageTpTimer.finished(10000L)) {
         this.damageTpTimer.reset();
         this.startEmergencyTeleport("Получен урон — телепортируюсь (/rtp)");
      }
   }

   private void checkHostileMobs() {
      if (!this.detectHostiles.isEnabled() || mc.world == null || mc.player == null) {
         return;
      }
      if (!this.hostileTpTimer.finished(10000L)) {
         return;
      }
      Box box = mc.player.getBoundingBox().expand(10.0);
      List<Entity> monsters = mc.world.getOtherEntities(mc.player, box, (entity) -> entity instanceof Monster);
      if (!monsters.isEmpty()) {
         this.hostileTpTimer.reset();
         this.startEmergencyTeleport("Враждебный моб рядом — телепортируюсь (/rtp)");
      }
   }

   private void startEmergencyTeleport(String message) {
      if (this.stage == Stage.RTP || this.stage == Stage.WAIT_TP) {
         return;
      }
      if (this.baritoneStarted) {
         this.baritoneStarted = false;
         this.send("#stop");
      }
      this.rtpCooldown = false;
      this.rtpAttempts = 0;
      this.cmdSent = false;
      this.timer.reset();
      this.walkTarget = null;
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }
      MessageUtil.displayInfo(message);
      this.stage = this.isAncientDebris() ? Stage.NETHER_WALK : Stage.RTP;
   }

   private void stageShears() {
      switch (this.shearsStep) {
         case 0:
            if (this.hasShearsInHotbar()) {
               this.shearsStep = 2;
            } else if (this.hasShearsInInventory()) {
               this.moveShearsToHotbar();
               this.shearsStep = 2;
            } else if (this.ironCount() >= 2) {
               this.shearsStep = 3;
               this.shearsTimer.reset();
            } else {
               MessageUtil.displayInfo("Нет ножниц и железа в инвентаре — модуль остановлен");
               this.setToggled(false);
            }
            break;

         case 2:
            this.removeSwordFromHotbar();
            this.shearsStep = 99;
            break;

         case 3:
            if (this.craftSubStep == 0) {
               if (!this.startShearsCraft()) {
                  MessageUtil.displayInfo("Не нашёл железо для крафта — модуль остановлен");
                  this.setToggled(false);
                  break;
               }
               this.shearsTimer.reset();
            }
            if (this.shearsTimer.finished(250L)) {
               this.shearsTimer.reset();
               if (this.advanceShearsCraft()) {
                  this.shearsStep = 4;
                  this.shearsTimer.reset();
               }
            }
            break;

         case 4:
            if (this.shearsTimer.finished(500L)) {
               if (!this.isShearsInCraftOutput()) {
                  MessageUtil.displayInfo("Крафт ножниц не удался — чищу сетку и пробую заново");
                  this.cleanCraftGrid();
                  this.shearsStep = 3;
                  this.shearsTimer.reset();
               } else {
                  this.takeShearsFromCraft();
                  this.shearsStep = 5;
                  this.shearsTimer.reset();
               }
            }
            break;

         case 5:
            if (this.shearsTimer.finished(500L)) {
               this.moveShearsToHotbar();
               this.removeSwordFromHotbar();
               this.cleanCraftGrid();
               this.shearsStep = 99;
            }
            break;

         case 99:
      this.shearsStep = 0;
      this.craftSubStep = 0;
      this.craftSlotA = -1;
      this.craftSlotB = -1;
      this.craftNeedSecond = false;
            this.stage = this.isAncientDebris() ? Stage.NETHER_WALK : Stage.RTP;
            break;
      }
   }

   private void stageNetherWalk() {
      if (!this.baritoneStarted) {
         if (!BaritoneUtil.isPresent()) {
            MessageUtil.displayInfo("Установите Baritone для работы ZarabotokReallyWorld");
            this.setToggled(false);
            return;
         }
         if (this.walkTarget == null) {
            float yaw = mc.player.getYaw();
            double rad = Math.toRadians(yaw);
            double dx = -Math.sin(rad) * 300.0;
            double dz = Math.cos(rad) * 300.0;
            this.walkTarget = mc.player.getPos().add(dx, 0.0, dz);
         }
         this.baritoneStarted = true;
         this.walkStarted = false;
         this.timer.reset();
         this.send("#goto " + (int) this.walkTarget.getX() + " " + (int) this.walkTarget.getY() + " " + (int) this.walkTarget.getZ());
         return;
      }
      if (!this.walkStarted) {
         this.walkStarted = true;
         this.timer.reset();
      }
      if (mc.player.squaredDistanceTo(this.walkTarget) < 25.0) {
         this.baritoneStarted = false;
         this.walkStarted = false;
         this.walkTarget = null;
         this.stage = Stage.MINE;
         MessageUtil.displayInfo("Дошёл до точки — начинаю копать обломки");
      } else if (this.timer.finished(180000L)) {
         this.baritoneStarted = false;
         this.walkStarted = false;
         this.walkTarget = null;
         this.stage = Stage.MINE;
         MessageUtil.displayInfo("Не дошёл за 3 минуты — копаю с текущей точки");
      }
   }

   private boolean hasShearsInHotbar() {
      for (int h = 0; h < 9; h++) {
         if (mc.player.getInventory().main.get(h).getItem() == Items.SHEARS) {
            return true;
         }
      }
      return false;
   }

   private boolean hasShearsInInventory() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().main.get(i).getItem() == Items.SHEARS) {
            return true;
         }
      }
      return false;
   }

   private int ironCount() {
      int count = 0;
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().main.get(i);
         if (stack.getItem() == Items.IRON_INGOT) {
            count += stack.getCount();
         }
      }
      return count;
   }

   private int findIronSlot() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().main.get(i).getItem() == Items.IRON_INGOT) {
            return i < 9 ? 36 + i : i;
         }
      }
      return -1;
   }

   private int findIronSlotExcept(int except) {
      for (int i = 0; i < 36; i++) {
         int id = i < 9 ? 36 + i : i;
         if (id == except) {
            continue;
         }
         if (mc.player.getInventory().main.get(i).getItem() == Items.IRON_INGOT) {
            return id;
         }
      }
      return -1;
   }

   private boolean startShearsCraft() {
      this.craftSlotA = this.findIronSlot();
      if (this.craftSlotA == -1) {
         return false;
      }
      int countA = mc.player.currentScreenHandler.getSlot(this.craftSlotA).getStack().getCount();
      this.craftNeedSecond = countA < 2;
      this.craftSlotB = -1;
      if (this.craftNeedSecond) {
         this.craftSlotB = this.findIronSlotExcept(this.craftSlotA);
         if (this.craftSlotB == -1) {
            return false;
         }
      }
      this.craftSubStep = 1;
      return true;
   }

   private boolean advanceShearsCraft() {
      switch (this.craftSubStep) {
         case 1:
            PlayerInventoryUtil.clickSlot(0, this.craftSlotA, 0, SlotActionType.PICKUP, false);
            break;
         case 2:
            PlayerInventoryUtil.clickSlot(0, 2, 1, SlotActionType.PICKUP, false);
            break;
         case 3:
            if (this.craftNeedSecond) {
               PlayerInventoryUtil.clickSlot(0, this.craftSlotB, 0, SlotActionType.PICKUP, false);
            } else {
               PlayerInventoryUtil.clickSlot(0, 3, 1, SlotActionType.PICKUP, false);
            }
            break;
         case 4:
            if (this.craftNeedSecond) {
               PlayerInventoryUtil.clickSlot(0, 3, 1, SlotActionType.PICKUP, false);
            } else {
               PlayerInventoryUtil.clickSlot(0, this.craftSlotA, 0, SlotActionType.PICKUP, false);
            }
            break;
         case 5:
            if (this.craftNeedSecond) {
               PlayerInventoryUtil.clickSlot(0, this.craftSlotB, 0, SlotActionType.PICKUP, false);
               this.craftSubStep = 6;
            } else {
               this.craftSubStep = 0;
               return true;
            }
            break;
         case 6:
            this.craftSubStep = 0;
            return true;
         default:
            this.craftSubStep = 0;
            return true;
      }
      this.craftSubStep++;
      return false;
   }

   private void takeShearsFromCraft() {
      PlayerInventoryUtil.clickSlot(0, 0, 0, SlotActionType.QUICK_MOVE, false);
   }

   private boolean isShearsInCraftOutput() {
      return mc.player.currentScreenHandler.getSlot(0).getStack().getItem() == Items.SHEARS;
   }

   private void moveShearsToHotbar() {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().main.get(i).getItem() == Items.SHEARS) {
            int target = -1;
            for (int h = 0; h < 9; h++) {
               if (mc.player.getInventory().main.get(h).isEmpty()) {
                  target = h;
                  break;
               }
            }
            if (target == -1) {
               for (int h = 0; h < 9; h++) {
                  if (this.isSword(mc.player.getInventory().main.get(h).getItem())) {
                     target = h;
                     break;
                  }
               }
            }
            if (target == -1) {
               target = 0;
            }
            PlayerInventoryUtil.clickSlot(0, i, target, SlotActionType.SWAP, false);
            return;
         }
      }
   }

   private void removeSwordFromHotbar() {
      for (int h = 0; h < 9; h++) {
         if (this.isSword(mc.player.getInventory().main.get(h).getItem())) {
            int free = this.findFreeMainInventorySlot();
            if (free != -1) {
               PlayerInventoryUtil.clickSlot(0, free, h, SlotActionType.SWAP, false);
            }
            return;
         }
      }
   }

   private void cleanCraftGrid() {
      for (int slot = 1; slot <= 4; slot++) {
         if (mc.player.currentScreenHandler.getSlot(slot).getStack().isEmpty()) {
            continue;
         }
         int free = this.findFreeMainInventorySlot();
         if (free != -1) {
            PlayerInventoryUtil.clickSlot(0, slot, 0, SlotActionType.PICKUP, false);
            PlayerInventoryUtil.clickSlot(0, free, 0, SlotActionType.PICKUP, false);
         }
      }
   }

   private int findFreeMainInventorySlot() {
      for (int j = 9; j < 36; j++) {
         if (mc.player.getInventory().main.get(j).isEmpty()) {
            return j;
         }
      }
      return -1;
   }

   private boolean isSword(Item item) {
      return item == Items.WOODEN_SWORD
            || item == Items.STONE_SWORD
            || item == Items.IRON_SWORD
            || item == Items.GOLDEN_SWORD
            || item == Items.DIAMOND_SWORD
            || item == Items.NETHERITE_SWORD;
   }

   private void stageRtp() {
      if (this.rtpCooldown) {
         if (this.rtpCooldownTimer.finished((long) this.rtpCooldownSeconds.getCurrent() * 1000L)) {
            this.rtpCooldown = false;
            this.rtpAttempts = 0;
            this.cmdSent = false;
            MessageUtil.displayInfo("Кулдаун /rtp прошёл — пробую снова");
         }
         return;
      }
      if (!this.cmdSent) {
         this.cmdSent = true;
         this.timer.reset();
         this.send("/rtp");
         return;
      }
      if (mc.currentScreen instanceof GenericContainerScreen screen) {
         int slot = this.findItemSlot(screen, Items.ENDER_PEARL);
         if (slot != -1) {
            this.click(screen, slot);
            this.cmdSent = false;
            this.rtpStartPos = mc.player.getPos();
            this.timer.reset();
            this.stage = Stage.WAIT_TP;
         }
      } else if (this.timer.finished(7000L)) {
         this.rtpAttempts++;
         this.cmdSent = false;
         if (this.rtpAttempts >= 3) {
            MessageUtil.displayInfo("Не удалось вызвать /rtp — продолжаю без него");
            this.stage = Stage.MINE;
         }
      }
   }

   private void stageWaitTp() {
      if (this.rtpStartPos != null && mc.player.getPos().distanceTo(this.rtpStartPos) > 10.0) {
         if (this.isNearProtectedSpot()) {
            this.rtpAttempts++;
            this.cmdSent = false;
            if (this.rtpAttempts >= 3) {
               MessageUtil.displayInfo("Везде защищённые регионы — копаю без телепорта");
               this.stage = Stage.MINE;
            } else {
               MessageUtil.displayInfo("Снова защищённый регион — пробую /rtp ещё раз");
               this.stage = Stage.RTP;
            }
         } else {
            this.stage = Stage.MINE;
         }
         return;
      }
      if (this.timer.finished(5000L)) {
         this.rtpAttempts++;
         this.cmdSent = false;
         if (this.rtpAttempts >= 3) {
            MessageUtil.displayInfo("Телепорт не выполнился — продолжаю без него");
            this.stage = Stage.MINE;
         } else {
            MessageUtil.displayInfo("Телепорт не выполнился — повторяю /rtp");
            this.stage = Stage.RTP;
         }
      }
   }

   private boolean isRtpCooldown(String lower) {
      return lower.contains("cooldown")
            || lower.contains("кулдаун")
            || lower.contains("куплдаун")
            || lower.contains("подожд")
            || lower.contains("погод")
            || lower.contains("позже");
   }

   private void stageMine() {
      Profile profile = this.profile();
      if (!this.baritoneStarted) {
         if (!BaritoneUtil.isPresent()) {
            MessageUtil.displayInfo("Установите Baritone для работы ZarabotokReallyWorld");
            this.setToggled(false);
            return;
         }
         this.baritoneStarted = true;
         this.send("#mine " + profile.block);
         return;
      }
      int count = PlayerInventoryUtil.getInventoryCount(profile.item);
      boolean depositOnly = this.isAncientDebris();
      if (!depositOnly && this.sellToggle.isEnabled()) {
         if (count < (int) this.stackTarget.getCurrent()) {
            return;
         }
         this.baritoneStarted = false;
         this.send("#stop");
         this.stage = Stage.MARKET_OPEN;
      } else if (depositOnly || this.depositToggle.isEnabled()) {
         if (count < (int) this.depositStacks.getCurrent() * 64) {
            return;
         }
         this.baritoneStarted = false;
         this.send("#stop");
         this.collectedCount = count;
         this.triedChests.clear();
         this.currentChest = null;
         this.walkStarted = false;
         this.stage = Stage.DEPOSIT_HOME;
      }
   }

   private void stageMarketOpen() {
      if (!this.cmdSent) {
         this.cmdSent = true;
         this.timer.reset();
         this.send("/market search " + this.profile().query);
         return;
      }
      if (this.timer.finished(6000L)) {
         this.cmdSent = false;
         this.stage = Stage.MINE;
         return;
      }
      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.cmdSent = false;
         this.timer.reset();
         this.stage = Stage.MARKET_SELECT;
      }
   }

   private void stageMarketSelect() {
      if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
         if (this.timer.finished(4000L)) {
            this.stage = Stage.MINE;
         }
         return;
      }
      int slot = this.findItemSlot(screen, this.profile().item);
      if (slot == -1) {
         MessageUtil.displayInfo("На маркете нет " + this.profile().query + " — продолжаю копать");
         this.stage = Stage.CLOSE_MARKET;
         return;
      }
      this.click(screen, slot);
      this.timer.reset();
      this.stage = Stage.MARKET_READ;
   }

   private void stageMarketRead() {
      if (this.timer.finished(5000L)) {
         this.stage = Stage.CLOSE_MARKET;
         return;
      }
      if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
         return;
      }
      List<Integer> prices = this.collectPrices(screen);
      if (prices.isEmpty()) {
         return;
      }
      int chosen = this.priceMode.is("Средняя") ? this.average(prices) : this.minimum(prices);
      this.sellPrice = (int) (chosen * (100.0F - this.marginPercent.getCurrent()) / 100.0F);
      if (this.sellPrice < 1) {
         this.sellPrice = 1;
      }
      this.cmdSent = false;
      this.stage = Stage.SELL;
   }

   private void stageSell() {
      if (!this.cmdSent) {
         this.cmdSent = true;
         this.timer.reset();
         MessageUtil.displayInfo("Продаю " + this.profile().query + " за " + this.sellPrice);
         this.send("/market sell " + this.sellPrice);
         return;
      }
      if (this.timer.finished(2000L)) {
         this.cmdSent = false;
         this.stage = Stage.CLOSE_MARKET;
      }
   }

   private void stageCloseMarket() {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
         this.timer.reset();
         return;
      }
      if (this.timer.finished(500L)) {
         this.stage = Stage.MINE;
      }
   }

   private void stageDepositHome() {
      if (!this.cmdSent) {
         this.cmdSent = true;
         this.timer.reset();
         this.send("/home");
         return;
      }
      if (this.timer.finished(6000L)) {
         this.cmdSent = false;
         this.stage = Stage.DEPOSIT_FIND;
      }
   }

   private void stageDepositFind() {
      this.currentChest = this.findNearestChest();
      if (this.currentChest == null) {
         MessageUtil.displayInfo("Нет сундуков в радиусе " + (int) this.chestRadius.getCurrent() + " — выбрасываю ресурс");
         this.stage = Stage.DEPOSIT_DROP;
         return;
      }
      this.walkStarted = false;
      this.stage = Stage.DEPOSIT_WALK;
   }

   private void stageDepositWalk() {
      if (!this.walkStarted) {
         this.walkStarted = true;
         this.timer.reset();
         this.send("#goto " + this.currentChest.getX() + " " + this.currentChest.getY() + " " + this.currentChest.getZ());
         return;
      }
      if (mc.player.squaredDistanceTo(Vec3d.ofCenter(this.currentChest)) < 9.0) {
         this.walkStarted = false;
         this.stage = Stage.DEPOSIT_OPEN;
      } else if (this.timer.finished(30000L)) {
         this.walkStarted = false;
         this.triedChests.add(this.currentChest);
         this.stage = Stage.DEPOSIT_FIND;
      }
   }

   private void stageDepositOpen() {
      if (!this.cmdSent) {
         this.cmdSent = true;
         this.timer.reset();
         this.openChest(this.currentChest);
         return;
      }
      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.cmdSent = false;
         this.stage = Stage.DEPOSIT_PUT;
      } else if (this.timer.finished(4000L)) {
         this.cmdSent = false;
         this.triedChests.add(this.currentChest);
         this.stage = Stage.DEPOSIT_FIND;
      }
   }

   private void stageDepositPut() {
      if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
         return;
      }
      if (!this.cmdSent) {
         this.cmdSent = true;
         this.timer.reset();
         this.moveCobwebToChest(screen);
         return;
      }
      if (!this.timer.finished(1000L)) {
         return;
      }
      this.cmdSent = false;
      if (PlayerInventoryUtil.getInventoryCount(this.profile().item) == 0) {
         if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }
         if (this.isAncientDebris()) {
            MessageUtil.displayInfo("Собрано " + this.collectedCount + " шт — складываю и останавливаюсь");
            this.setToggled(false);
         } else {
            MessageUtil.displayInfo("Собрано паутины: " + this.collectedCount + " шт — сложена в сундук, телепортируюсь (/rtp)");
            this.stage = Stage.RTP;
         }
      } else {
         this.triedChests.add(this.currentChest);
         if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }
         this.stage = Stage.DEPOSIT_FIND;
      }
   }

   private void stageDepositDrop() {
      if (!this.cmdSent) {
         this.cmdSent = true;
         this.timer.reset();
         this.dropCobweb();
         return;
      }
      if (this.timer.finished(500L)) {
         this.cmdSent = false;
         if (this.isAncientDebris()) {
            MessageUtil.displayInfo("Собрано " + this.collectedCount + " шт — нет сундука, останавливаюсь");
            this.setToggled(false);
         } else {
            MessageUtil.displayInfo("Собрано паутины: " + this.collectedCount + " шт — нет сундука, выбросил и телепортируюсь (/rtp)");
            this.stage = Stage.RTP;
         }
      }
   }

   private BlockPos findNearestChest() {
      BlockPos playerPos = mc.player.getBlockPos();
      int radius = (int) this.chestRadius.getCurrent();
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dy = -4; dy <= 4; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
               BlockPos pos = playerPos.add(dx, dy, dz);
               if (this.triedChests.contains(pos)) {
                  continue;
               }
               Block block = mc.world.getBlockState(pos).getBlock();
               if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL) {
                  double dist = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                  if (dist < bestDist) {
                     bestDist = dist;
                     best = pos;
                  }
               }
            }
         }
      }
      return best;
   }

   private void openChest(BlockPos pos) {
      BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
   }

   private void moveCobwebToChest(GenericContainerScreen screen) {
      int containerSlots = screen.getScreenHandler().getRows() * 9;
      for (int i = containerSlots; i < screen.getScreenHandler().slots.size(); i++) {
         Slot slot = screen.getScreenHandler().slots.get(i);
         if (slot.hasStack() && slot.getStack().getItem() == this.profile().item) {
            PlayerInventoryUtil.clickSlot(screen.getScreenHandler().syncId, i, 0, SlotActionType.QUICK_MOVE, false);
         }
      }
   }

   private void dropCobweb() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == this.profile().item) {
            int slotId = i < 9 ? 36 + i : i;
            mc.interactionManager.clickSlot(0, slotId, 1, SlotActionType.THROW, mc.player);
         }
      }
   }

   private List<Integer> collectPrices(GenericContainerScreen screen) {
      List<Integer> prices = new ArrayList<>();
      String query = this.profile().query.toLowerCase(Locale.ROOT);
      int containerSlots = screen.getScreenHandler().getRows() * 9;
      for (int i = 0; i < containerSlots; i++) {
         Slot slot = screen.getScreenHandler().slots.get(i);
         if (!slot.hasStack()) {
            continue;
         }
         ItemStack stack = slot.getStack();
         String name = this.stripColor(stack.getName().getString());
         if (!name.toLowerCase(Locale.ROOT).contains(query)) {
            continue;
         }
         String digits = name.replaceAll("[^0-9]", "");
         if (digits.isEmpty()) {
            continue;
         }
         prices.add(Integer.parseInt(digits));
      }
      return prices;
   }

   private String stripColor(String text) {
      return text.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
   }

   private int minimum(List<Integer> values) {
      int min = Integer.MAX_VALUE;
      for (int value : values) {
         if (value < min) {
            min = value;
         }
      }
      return min;
   }

   private int average(List<Integer> values) {
      long sum = 0L;
      for (int value : values) {
         sum += value;
      }
      return (int) (sum / values.size());
   }

   private int findItemSlot(GenericContainerScreen screen, Item item) {
      int containerSlots = screen.getScreenHandler().getRows() * 9;
      for (int i = 0; i < containerSlots; i++) {
         Slot slot = screen.getScreenHandler().slots.get(i);
         if (slot.hasStack() && slot.getStack().getItem() == item) {
            return i;
         }
      }
      return -1;
   }

   private void click(GenericContainerScreen screen, int slot) {
      PlayerInventoryUtil.clickSlot(screen.getScreenHandler().syncId, slot, 0, SlotActionType.PICKUP, false);
   }

   private void send(String command) {
      if (command.startsWith("/")) {
         mc.getNetworkHandler().sendChatCommand(command.substring(1));
      } else {
         mc.getNetworkHandler().sendChatMessage(command);
      }
   }
}
