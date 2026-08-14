package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import tech.huihui.base.autobuy.item.CollectorItemBuy;
import tech.huihui.base.autobuy.item.EnchantItemBuy;
import tech.huihui.base.autobuy.item.ItemBuy;
import tech.huihui.base.autobuy.enchantes.Enchant;
import tech.huihui.base.config.AutoBuyConfig;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.base.events.impl.other.EventTickMovement;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.utility.game.server.AutoBuyUtil;

@ModuleAnnotation(name = "Collector", category = Category.MISC, description = "Автоматически собирает нужный инвентарь на FunTime")
public final class Collector extends Module {
   public static final Collector INSTANCE = new Collector();
   private static final Pattern PAGE_PATTERN = Pattern.compile("(\\d+)/(\\d+)");
   private final List<CollectorItemBuy> entries = new ArrayList<>();
   private final List<Offer> offers = new ArrayList<>();
   private final Counter searchCounter = new Counter();
   private final Counter anarchyCounter = new Counter();
   private final ButtonSetting editorButton = new ButtonSetting("Открыть редактор", () -> {
      mc.setScreen(new AutoBuyEditorScreen(1));
   });
   private final ButtonSetting startButton = new ButtonSetting("Запустить работу", () -> {
      if (!this.isEnabled()) {
         this.setToggled(true);
      }

      this.startWork();
   });
   private CollectorItemBuy target;
   private Offer offer;
   private int tickCounter;

   private Collector() {
      this.entries.addAll(this.buildDefaultEntries());
      AutoBuyConfig.loadCollect(this.entries);
   }

   private List<CollectorItemBuy> buildDefaultEntries() {
      List<CollectorItemBuy> list = new ArrayList<>();
      list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_SWORD), "Незеритовый меч",
         new EnchantVanillaLocal("minecraft:sharpness", 7),
         new EnchantVanillaLocal("minecraft:fire_aspect", 2)), "Незеритовый меч", "Незеритовый меч", 1, true, true)
         .lore("Яд 3", "Вампиризм 2", "Окисление 2", "Опытный 3", "Детекция 3"));
      list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.MACE), "Булава",
         new EnchantVanillaLocal("minecraft:sharpness", 7),
         new EnchantVanillaLocal("minecraft:breach", 3),
         new EnchantVanillaLocal("minecraft:density", 5)), "Булава", "Булава", 1, true, true));
      list.add(new CollectorItemBuy(new ItemStack(Items.TRIDENT), "Трезубец", "Трезубец", ItemBuy.Category.FUNTIME, 1, true, true)
         .lore("Ступор 3", "Притяжение 2", "Скаут 3", "Возвращение", "Подрывник"));
      list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_HELMET), "Незеритовый шлем",
         new EnchantVanillaLocal("minecraft:protection", 5),
         new EnchantVanillaLocal("minecraft:unbreaking", 5),
         new EnchantVanillaLocal("minecraft:respiration", 3),
         new EnchantVanillaLocal("minecraft:mending", 1)), "Незеритовый шлем", "Незеритовый шлем", 1, true, true));
      list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_CHESTPLATE), "Незеритовый нагрудник",
         new EnchantVanillaLocal("minecraft:protection", 5),
         new EnchantVanillaLocal("minecraft:unbreaking", 5),
         new EnchantVanillaLocal("minecraft:mending", 1)), "Незеритовый нагрудник", "Незеритовый нагрудник", 1, true, true));
      list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_LEGGINGS), "Незеритовые поножи",
         new EnchantVanillaLocal("minecraft:protection", 5),
         new EnchantVanillaLocal("minecraft:unbreaking", 5),
         new EnchantVanillaLocal("minecraft:mending", 1)), "Незеритовые поножи", "Незеритовые поножи", 1, true, true));
      list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_BOOTS), "Незеритовые ботинки",
         new EnchantVanillaLocal("minecraft:protection", 5),
         new EnchantVanillaLocal("minecraft:unbreaking", 5),
         new EnchantVanillaLocal("minecraft:depth_strider", 3),
         new EnchantVanillaLocal("minecraft:mending", 1)), "Незеритовые ботинки", "Незеритовые ботинки", 1, true, true));
      list.add(new CollectorItemBuy(new ItemStack(Items.NETHERITE_SCRAP), "Трапка", "Трапка", ItemBuy.Category.FUNTIME, 8, true, false)
         .lore("Каст: Нерушимая клетка"));
      list.add(new CollectorItemBuy(new ItemStack(Items.SUGAR), "Явная пыль", "Явная пыль", ItemBuy.Category.FUNTIME, 12, true, false)
         .lore("Каст: Световая вспышка"));
      list.add(new CollectorItemBuy(new ItemStack(Items.PHANTOM_MEMBRANE), "Божья аура", "Божья аура", ItemBuy.Category.FUNTIME, 4, true, false)
         .lore("Каст: Божественная аура"));
      list.add(new CollectorItemBuy(new ItemStack(Items.ENDER_EYE), "Дезориентация", "Дезориентация", ItemBuy.Category.FUNTIME, 16, true, false)
         .lore("Каст: Звуковая волна"));
      list.add(new CollectorItemBuy(new ItemStack(Items.WIND_CHARGE), "Заряд ветра", "Заряд ветра", ItemBuy.Category.FUNTIME, 32, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.DRIED_KELP), "Пласт", "Пласт", ItemBuy.Category.FUNTIME, 16, true, false)
         .lore("Каст: Нерушимая стена"));
      list.add(new CollectorItemBuy(new ItemStack(Items.SNOWBALL), "Снежок заморозка", "Снежок заморозка", ItemBuy.Category.FUNTIME, 4, true, false)
         .lore("Каст: Ледяная сфера"));
      list.add(new CollectorItemBuy(new ItemStack(Items.ENDER_PEARL), "Перка", "Перка", ItemBuy.Category.FUNTIME, 16, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.TOTEM_OF_UNDYING), "Тотем бессмертия", "Тотем бессмертия", ItemBuy.Category.FUNTIME, 1, true, false));
      list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.CROSSBOW), "Арбалет",
         new EnchantVanillaLocal("minecraft:quick_charge", 3),
         new EnchantVanillaLocal("minecraft:mending", 1),
         new EnchantVanillaLocal("minecraft:multishot", 1)), "Арбалет", "Арбалет", 1, true, true));
      list.add(new CollectorItemBuy(new ItemStack(Items.GOLDEN_APPLE), "Золотое яблоко", "Золотое яблоко", ItemBuy.Category.FUNTIME, 16, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), "Зачарованное золотое яб", "Зачарованное золотое яб", ItemBuy.Category.FUNTIME, 8, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.GOLDEN_CARROT), "Золотая морковь", "Золотая морковь", ItemBuy.Category.FUNTIME, 64, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.CHORUS_FRUIT), "Хорус", "Хорус", ItemBuy.Category.FUNTIME, 64, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.ELYTRA), "Элитры", "Элитры", ItemBuy.Category.FUNTIME, 1, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.FIREWORK_ROCKET), "Фейерверк", "Фейерверк", ItemBuy.Category.FUNTIME, 64, true, false));
      list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Хлопушка", "Хлопушка", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SLOWNESS, 10, 100))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SPEED, 5, 600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.BLINDNESS, 10, 100))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.GLOWING, 1, 3600)));
      list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Святая вода", "Святая вода", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.REGENERATION, 2, 900))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.INVISIBILITY, 2, 12000))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.INSTANT_HEALTH, 2, 0)));
      list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Гнева", "Зелье Гнева", ItemBuy.Category.FUNTIME, 1, true, true)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.STRENGTH, 5, 600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SLOWNESS, 4, 600)));
      list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Палладина", "Зелье Палладина", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.RESISTANCE, 1, 12000))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.FIRE_RESISTANCE, 1, 12000))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.HEALTH_BOOST, 3, 1200))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.INVISIBILITY, 1, 18000)));
      list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Ассасина", "Зелье Ассасина", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.STRENGTH, 4, 1200))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SPEED, 3, 6000))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.HASTE, 1, 1200))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.INSTANT_DAMAGE, 2, 0)));
      list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Радиации", "Зелье Радиации", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.POISON, 2, 600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.WITHER, 2, 600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SLOWNESS, 3, 600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.HUNGER, 5, 600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.GLOWING, 1, 600)));
      list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Снотворное", "Снотворное", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.WEAKNESS, 2, 1800))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.MINING_FATIGUE, 2, 600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.WITHER, 3, 1800))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.BLINDNESS, 1, 600)));
      list.add(new CollectorItemBuy(new ItemStack(Items.POTION), "Зелье", "Зелье", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.STRENGTH, 3, 3600))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SPEED, 3, 3600)));
      list.add(new CollectorItemBuy(new ItemStack(Items.POTION), "Зелье регенерации", "Зелье регенерации", ItemBuy.Category.FUNTIME, 1, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.INSTANT_HEALTH, 2, 0))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.REGENERATION, 1, 900)));
      list.add(new CollectorItemBuy(new ItemStack(Items.TIPPED_ARROW), "Кровавая стрела", "Кровавая стрела", ItemBuy.Category.FUNTIME, 32, true, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.WEAKNESS, 3, 60))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.BLINDNESS, 1, 40))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.MINING_FATIGUE, 1, 40))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.NAUSEA, 1, 100)));
      list.add(new CollectorItemBuy(new ItemStack(Items.TIPPED_ARROW), "Стрела обледенения", "Стрела обледенения", ItemBuy.Category.FUNTIME, 64, false, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SLOWNESS, 10, 100))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.MINING_FATIGUE, 3, 40)));
      list.add(new CollectorItemBuy(new ItemStack(Items.TIPPED_ARROW), "Мучительная стрела", "Мучительная стрела", ItemBuy.Category.FUNTIME, 64, false, false)
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.SLOWNESS, 3, 100))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.WITHER, 3, 100))
         .potion(new CollectorItemBuy.PotionRequirement(StatusEffects.POISON, 3, 100)));
      return list;
   }

   private EnchantItemBuy enchantItem(ItemStack stack, String displayName, Enchant... enchants) {
      EnchantItemBuy item = new EnchantItemBuy(stack, displayName, ItemBuy.Category.FUNTIME);
      for (Enchant enchant : enchants) {
         item.addEnchant(enchant);
      }

      return item;
   }

   private void startWork() {
      int start = this.target == null ? 0 : this.entries.indexOf(this.target) + 1;
      this.offer = null;
      this.offers.clear();
      this.target = null;
      for (int i = start; i < this.entries.size(); i++) {
         CollectorItemBuy entry = this.entries.get(i);
         if (entry.isActive() && this.haveCount(entry) < this.targetLower(entry)) {
            this.target = entry;
            break;
         }
      }

      if (this.target != null) {
         this.chat("§aПереходим к сбору предмета: §c" + this.target.getDisplayName());
      } else {
         this.chat("§aВсе предметы собраны, работа завершена");
      }
   }

   @EventTarget
   public void onTick(EventTickMovement event) {
      if (this.target == null) {
         return;
      }

      if (mc.player.getInventory().getEmptySlot() == -1) {
         this.chat("§cАвтоматическое отключение: нет свободных слотов в инвентаре, освободите место");
         this.setToggled(false);
         return;
      }

      this.tickCounter++;
      if (mc.currentScreen instanceof GenericContainerScreen screen) {
         this.handleContainer(screen);
      } else if (mc.player.age >= 200 && this.searchCounter.elapsed(1000L, 300L)) {
         mc.player.networkHandler.sendChatCommand("ah search " + this.target.getSearchName());
         this.searchCounter.reset();
         this.tickCounter = 0;
      }
   }

   private void handleContainer(GenericContainerScreen screen) {
      String title = screen.getTitle().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT).trim();
      List<Slot> slots = screen.getScreenHandler().slots;
      if (title.contains(this.target.getSearchName().toLowerCase(Locale.ROOT))) {
         if (this.searchCounter.elapsed(300L, 80L) && this.haveCount(this.target) >= this.targetUpper(this.target)) {
            this.chat("§aПредмет §c" + this.target.getDisplayName() + " §aприобретен, перехожу к следующему");
            this.startWork();
            this.close();
            return;
         }

         if (this.anarchyCounter.elapsed(1000L, 150L)) {
            if (this.searchCounter.elapsed(this.target.isScan() ? 400L : 550L, 120L)) {
               if (this.target.isScan()) {
                  this.handleScanMode(screen, title, slots);
               } else {
                  this.handleBuyMode(screen, title, slots);
               }

               this.searchCounter.reset();
            }

            return;
         }

         return;
      }

      if (title.contains("подтверждение покупки") || title.contains("подозрительная цена!")) {
         if (this.anarchyCounter.elapsed(200L, 90L)) {
            this.clickButton(slots, "[Кyпить]");
            this.anarchyCounter.reset();
         }

         return;
      }

      this.close();
   }

   private void handleScanMode(GenericContainerScreen screen, String title, List<Slot> slots) {
      Matcher matcher = PAGE_PATTERN.matcher(title);
      if (!matcher.find()) {
         return;
      }

      int currentPage = Integer.parseInt(matcher.group(1));
      int lastScanPage = Math.min(4, Integer.parseInt(matcher.group(2)));
      if (this.offer == null) {
         if (this.offers.stream().noneMatch(offer -> offer.page == currentPage)) {
            for (Slot slot : slots) {
               if (this.isBuy(slot.getStack())) {
                  this.offers.add(new Offer(currentPage, slot.id, AutoBuyUtil.getServerPrice(slot.getStack())));
               }
            }
         }

         if (currentPage < lastScanPage) {
            this.clickButton(slots, "следующая страница");
         } else {
            this.offer = this.offers.stream().min(Comparator.comparingInt(offer -> offer.price)).orElse(null);
            if (this.offer == null) {
               this.close();
            }
         }
      } else if (currentPage == this.offer.page) {
         Slot offerSlot = slots.stream()
            .filter(slot -> this.isBuy(slot.getStack()) && AutoBuyUtil.getServerPrice(slot.getStack()) == this.offer.price)
            .findFirst()
            .orElse(slots.stream().filter(slot -> this.isBuy(slot.getStack()))
               .min(Comparator.comparingInt(slot -> AutoBuyUtil.getServerPrice(slot.getStack())))
               .orElse(null));
         if (offerSlot != null) {
            this.clickSlot(screen, offerSlot.id);
            this.searchCounter.reset();
         } else {
            this.chat("§cОффер пропал, пересканирую");
            this.offer = null;
            this.offers.clear();
            this.close();
         }
      } else {
         this.clickButton(slots, currentPage < this.offer.page ? "следующая страница" : "предыдущая страница");
      }
   }

   private void handleBuyMode(GenericContainerScreen screen, String title, List<Slot> slots) {
      List<Slot> buyable = slots.stream().filter(slot -> this.isBuy(slot.getStack())).toList();
      int minPrice = buyable.stream().mapToInt(slot -> AutoBuyUtil.getServerPrice(slot.getStack())).min().orElse(0);
      List<Slot> affordable = buyable.stream()
         .filter(slot -> AutoBuyUtil.getServerPrice(slot.getStack()) <= Math.round((float) (minPrice * 2)))
         .filter(slot -> slot.getStack().getCount() <= this.targetUpper(this.target) - this.haveCount(this.target))
         .toList();
      Slot cheapest = affordable.stream()
         .filter(slot -> slot.getStack().getCount() >= this.targetLower(this.target) - this.haveCount(this.target))
         .min(Comparator.comparingInt(slot -> AutoBuyUtil.getServerPrice(slot.getStack())))
         .orElse(affordable.stream().min(Comparator.comparingInt(slot -> AutoBuyUtil.getServerPrice(slot.getStack()))).orElse(null));
      if (cheapest != null) {
         this.clickSlot(screen, cheapest.id);
         this.searchCounter.reset();
      } else {
         Matcher matcher = PAGE_PATTERN.matcher(title);
         if (matcher.find()) {
            int currentPage = Integer.parseInt(matcher.group(1));
            int totalPages = Integer.parseInt(matcher.group(2));
            if (totalPages == 1) {
               this.chat("§cПропускаем предмет §e" + this.target.getDisplayName() + " §c, ибо нету подходящего");
               this.startWork();
               this.close();
               return;
            }

            this.clickButton(slots, currentPage == 1 ? "следующая страница" : "предыдущая страница");
         }
      }
   }

   private boolean isBuy(ItemStack stack) {
      if (this.target == null || stack.isEmpty() || !this.target.isBuy(stack)) {
         return false;
      }

      ItemStack inventory = null;
      for (int i = 0; i < 40; i++) {
         ItemStack stackInSlot = mc.player.getInventory().getStack(i);
         if (!stackInSlot.isEmpty() && this.target.isBuy(stackInSlot)) {
            inventory = stackInSlot;
            break;
         }
      }

      if (inventory != null && !inventory.isEmpty()
         && ((this.target.getItemStack().getItem() instanceof PotionItem)
            && !java.util.Objects.equals(stack.get(DataComponentTypes.POTION_CONTENTS), inventory.get(DataComponentTypes.POTION_CONTENTS))
            || !stack.getName().getString().trim().equalsIgnoreCase(inventory.getName().getString().trim()))) {
         return false;
      }

      if (stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC).stream()
         .anyMatch(line -> line.getString().contains("➥ Нажмите, чтобы забрать"))) {
         return false;
      }

      if (AutoBuyUtil.getServerPrice(stack) <= 0) {
         return false;
      }

      if (this.target.getItemStack().getItem() == Items.TOTEM_OF_UNDYING && stack.hasGlint()) {
         return false;
      }

      if (this.target.getItemStack().getItem() == Items.ELYTRA && stack.get(DataComponentTypes.DAMAGE) != null) {
         return ((float) (432 - stack.get(DataComponentTypes.DAMAGE))) / 432.0f >= 0.5f;
      }

      if (!(this.target.getItemStack().getItem() instanceof ArmorItem)
         || stack.get(DataComponentTypes.DAMAGE) == null
         || stack.getMaxDamage() <= 0) {
         return true;
      }

      return this.hasMendingCondition() || ((float) (stack.getMaxDamage() - stack.get(DataComponentTypes.DAMAGE))) / (float) stack.getMaxDamage() >= 0.7f;
   }

   private boolean hasMendingCondition() {
      if (!(this.target.getMatcher() instanceof EnchantItemBuy enchantItem)) {
         return false;
      }

      for (Enchant enchant : enchantItem.getEnchants()) {
         if (enchant.getChecked().equals("minecraft:mending")) {
            return true;
         }
      }

      return false;
   }

   private int haveCount(CollectorItemBuy info) {
      int count = 0;
      for (int i = 0; i < 40; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && info.isBuy(stack)) {
            count += stack.getCount();
         }
      }

      return count;
   }

   private int targetLower(CollectorItemBuy info) {
      return Math.max(1, info.getCount());
   }

   private int targetUpper(CollectorItemBuy info) {
      return Math.max(1, info.getCount() + Math.round(info.getCount() * 0.2f));
   }

   private void clickButton(List<Slot> slots, String name) {
      slots.stream()
         .filter(slot -> slot.getStack().getName().getString().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
         .findFirst()
         .ifPresent(slot -> mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player));
   }

   private void clickSlot(GenericContainerScreen screen, int slotId) {
      mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
   }

   private void close() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
         mc.player.closeScreen();
      }
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (this.target == null || !event.isReceive()) {
         return;
      }

      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String message = packet.content().getString();
         if (message.toLowerCase(Locale.ROOT).contains("[✘] ошибка! этот товар уже купили!")) {
            this.offer = null;
            this.offers.clear();
         } else if (message.contains("[✘] Ошибка! У Вас не хватает Монет!")) {
            this.chat("§cАвтоматическое отключение из-за нехватки баланса на аккаунте");
            this.setToggled(false);
         } else if (message.contains("Данная команда недоступна в режиме AFK")) {
            this.chat("§cКоманда недоступна в режиме AFK");
         }
      } else if (event.getPacket() instanceof OpenScreenS2CPacket && !(mc.currentScreen instanceof GenericContainerScreen)) {
         if (this.tickCounter >= 8) {
            int anarchy = (int) (Math.random() * 100.0 <= 50.0 ? Math.random() * 26.0 + 205.0 : Math.random() * 20.0 + 305.0);
            mc.player.networkHandler.sendChatCommand("an" + anarchy);
            this.chat("§7Обнаружили замедление аукциона, переходим на " + anarchy + " анархию");
         }

         this.anarchyCounter.reset();
      }
   }

   @EventTarget
   public void onKey(EventKey event) {
      if (this.target == null || event.getKeyCode() != 256 || event.getAction() != 1) {
         return;
      }

      this.target = null;
      this.chat("§aРабота модуля была принудительно завершена");
   }

   private void chat(String message) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(message), false);
      }
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.chat("§aМодуль ищет самые дешевые лоты среди тех которые есть, имейте это ввиду, и будьте осторожны!");
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.target = null;
      this.offer = null;
      this.offers.clear();
      AutoBuyConfig.saveCollect(this.entries);
   }

   public List<CollectorItemBuy> getEntries() {
      return this.entries;
   }

   public static final class EnchantVanillaLocal extends Enchant {
      public EnchantVanillaLocal(String checked, int minLevel) {
         super(checked, checked, minLevel);
      }

      @Override
      public boolean isEnchanted(ItemStack stack) {
         for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<net.minecraft.enchantment.Enchantment>> entry : stack.getEnchantments().getEnchantmentEntries()) {
            if (entry.getKey().getKey().isPresent() && entry.getKey().getKey().get().getValue().toString().equals(this.getChecked()) && entry.getIntValue() >= this.getMinLevel()) {
               return true;
            }
         }

         return false;
      }
   }

   public record Offer(int page, int slot, int price) {
   }

   public static final class Counter {
      private long lastReset = System.currentTimeMillis();
      private int counter = -1;

      public boolean elapsed(long delay, long jitter) {
         this.counter++;
         return System.currentTimeMillis() - this.lastReset >= delay + (long) (this.counter % (jitter + 1));
      }

      public void reset() {
         this.lastReset = System.currentTimeMillis();
      }
   }
}
