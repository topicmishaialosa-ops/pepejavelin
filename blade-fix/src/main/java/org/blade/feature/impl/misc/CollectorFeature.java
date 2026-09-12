package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.input.KeyboardInputEvent;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.utils.autobuy.AutoBuyConfig;
import org.blade.utils.autobuy.AutoBuyUtil;
import org.blade.utils.autobuy.CollectorItemBuy;
import org.blade.utils.autobuy.EnchantItemBuy;
import org.blade.utils.autobuy.Enchant;
import org.blade.utils.autobuy.EnchantVanilla;
import org.blade.utils.autobuy.ItemBuy;
import org.blade.utils.inventory.InventoryUtil;
import org.blade.utils.text.ChatUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CollectorFeature extends Feature {
    public static final CollectorFeature INSTANCE = new CollectorFeature();
    private static final Pattern PAGE_PATTERN = Pattern.compile("(\\d+)/(\\d+)");

    private final ButtonSetting editorButton = register(new ButtonSetting("Открыть редактор", "Открыть", null));
    private final ButtonSetting startButton = register(new ButtonSetting("Запустить работу", "Старт", null));
    private final List<CollectorItemBuy> entries = new ArrayList<>();
    private final List<Offer> offers = new ArrayList<>();
    private final Counter searchCounter = new Counter();
    private final Counter anarchyCounter = new Counter();
    private CollectorItemBuy target;
    private Offer offer;
    private int tickCounter;

    private CollectorFeature() {
        super("Collector", "Автоматически собирает нужный инвентарь на FunTime", FeatureCategory.MISC, BindSetting.UNBOUND);
        this.editorButton.setChangeListener(() -> Minecraft.getInstance().gui.setScreen(new AutoBuyEditorScreen(1)));
        this.startButton.setChangeListener(() -> {
            if (!this.isEnabled()) {
                this.setEnabled(true);
            }
            this.startWork();
        });
    }

    private void ensureEntries() {
        if (this.entries.isEmpty()) {
            this.entries.addAll(this.buildDefaultEntries());
            AutoBuyConfig.loadCollect(this.entries);
        }
    }

    private List<CollectorItemBuy> buildDefaultEntries() {
        List<CollectorItemBuy> list = new ArrayList<>();
        list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_SWORD), "Незеритовый меч",
                new EnchantVanilla("minecraft:sharpness", 7),
                new EnchantVanilla("minecraft:fire_aspect", 2)), "Незеритовый меч", "Незеритовый меч", 1, true, true)
                .lore("Яд 3", "Вампиризм 2", "Окисление 2", "Опытный 3", "Детекция 3"));
        list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.MACE), "Булава",
                new EnchantVanilla("minecraft:sharpness", 7),
                new EnchantVanilla("minecraft:breach", 3),
                new EnchantVanilla("minecraft:density", 5)), "Булава", "Булава", 1, true, true));
        list.add(new CollectorItemBuy(new ItemStack(Items.TRIDENT), "Трезубец", "Трезубец", ItemBuy.Category.FUNTIME, 1, true, true)
                .lore("Ступор 3", "Притяжение 2", "Скаут 3", "Возвращение", "Подрывник"));
        list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_HELMET), "Незеритовый шлем",
                new EnchantVanilla("minecraft:protection", 5),
                new EnchantVanilla("minecraft:unbreaking", 5),
                new EnchantVanilla("minecraft:respiration", 3),
                new EnchantVanilla("minecraft:mending", 1)), "Незеритовый шлем", "Незеритовый шлем", 1, true, true));
        list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_CHESTPLATE), "Незеритовый нагрудник",
                new EnchantVanilla("minecraft:protection", 5),
                new EnchantVanilla("minecraft:unbreaking", 5),
                new EnchantVanilla("minecraft:mending", 1)), "Незеритовый нагрудник", "Незеритовый нагрудник", 1, true, true));
        list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_LEGGINGS), "Незеритовые поножи",
                new EnchantVanilla("minecraft:protection", 5),
                new EnchantVanilla("minecraft:unbreaking", 5),
                new EnchantVanilla("minecraft:mending", 1)), "Незеритовые поножи", "Незеритовые поножи", 1, true, true));
        list.add(new CollectorItemBuy(this.enchantItem(new ItemStack(Items.NETHERITE_BOOTS), "Незеритовые ботинки",
                new EnchantVanilla("minecraft:protection", 5),
                new EnchantVanilla("minecraft:unbreaking", 5),
                new EnchantVanilla("minecraft:depth_strider", 3),
                new EnchantVanilla("minecraft:mending", 1)), "Незеритовые ботинки", "Незеритовые ботинки", 1, true, true));
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
                new EnchantVanilla("minecraft:quick_charge", 3),
                new EnchantVanilla("minecraft:mending", 1),
                new EnchantVanilla("minecraft:multishot", 1)), "Арбалет", "Арбалет", 1, true, true));
        list.add(new CollectorItemBuy(new ItemStack(Items.GOLDEN_APPLE), "Золотое яблоко", "Золотое яблоко", ItemBuy.Category.FUNTIME, 16, true, false));
        list.add(new CollectorItemBuy(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), "Зачарованное золотое яб", "Зачарованное золотое яб", ItemBuy.Category.FUNTIME, 8, true, false));
        list.add(new CollectorItemBuy(new ItemStack(Items.GOLDEN_CARROT), "Золотая морковь", "Золотая морковь", ItemBuy.Category.FUNTIME, 64, true, false));
        list.add(new CollectorItemBuy(new ItemStack(Items.CHORUS_FRUIT), "Хорус", "Хорус", ItemBuy.Category.FUNTIME, 64, true, false));
        list.add(new CollectorItemBuy(new ItemStack(Items.ELYTRA), "Элитры", "Элитры", ItemBuy.Category.FUNTIME, 1, true, false));
        list.add(new CollectorItemBuy(new ItemStack(Items.FIREWORK_ROCKET), "Фейерверк", "Фейерверк", ItemBuy.Category.FUNTIME, 64, true, false));
        list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Хлопушка", "Хлопушка", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SLOWNESS, 10, 100))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SPEED, 5, 600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.BLINDNESS, 10, 100))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.GLOWING, 1, 3600)));
        list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Святая вода", "Святая вода", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.REGENERATION, 2, 900))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.INVISIBILITY, 2, 12000))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.INSTANT_HEALTH, 2, 0)));
        list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Гнева", "Зелье Гнева", ItemBuy.Category.FUNTIME, 1, true, true)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.STRENGTH, 5, 600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SLOWNESS, 4, 600)));
        list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Палладина", "Зелье Палладина", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.RESISTANCE, 1, 12000))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.FIRE_RESISTANCE, 1, 12000))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.HEALTH_BOOST, 3, 1200))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.INVISIBILITY, 1, 18000)));
        list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Ассасина", "Зелье Ассасина", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.STRENGTH, 4, 1200))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SPEED, 3, 6000))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.HASTE, 1, 1200))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.INSTANT_DAMAGE, 2, 0)));
        list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Зелье Радиации", "Зелье Радиации", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.POISON, 2, 600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.WITHER, 2, 600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SLOWNESS, 3, 600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.HUNGER, 5, 600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.GLOWING, 1, 600)));
        list.add(new CollectorItemBuy(new ItemStack(Items.SPLASH_POTION), "Снотворное", "Снотворное", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.WEAKNESS, 2, 1800))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.MINING_FATIGUE, 2, 600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.WITHER, 3, 1800))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.BLINDNESS, 1, 600)));
        list.add(new CollectorItemBuy(new ItemStack(Items.POTION), "Зелье", "Зелье", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.STRENGTH, 3, 3600))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SPEED, 3, 3600)));
        list.add(new CollectorItemBuy(new ItemStack(Items.POTION), "Зелье регенерации", "Зелье регенерации", ItemBuy.Category.FUNTIME, 1, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.INSTANT_HEALTH, 2, 0))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.REGENERATION, 1, 900)));
        list.add(new CollectorItemBuy(new ItemStack(Items.TIPPED_ARROW), "Кровавая стрела", "Кровавая стрела", ItemBuy.Category.FUNTIME, 32, true, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.WEAKNESS, 3, 60))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.BLINDNESS, 1, 40))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.MINING_FATIGUE, 1, 40))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.NAUSEA, 1, 100)));
        list.add(new CollectorItemBuy(new ItemStack(Items.TIPPED_ARROW), "Стрела обледенения", "Стрела обледенения", ItemBuy.Category.FUNTIME, 64, false, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SLOWNESS, 10, 100))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.MINING_FATIGUE, 3, 40)));
        list.add(new CollectorItemBuy(new ItemStack(Items.TIPPED_ARROW), "Мучительная стрела", "Мучительная стрела", ItemBuy.Category.FUNTIME, 64, false, false)
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.SLOWNESS, 3, 100))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.WITHER, 3, 100))
                .potion(new CollectorItemBuy.PotionRequirement(MobEffects.POISON, 3, 100)));
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
        this.ensureEntries();
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
            ChatUtil.info("Переходим к сбору предмета: " + this.target.getDisplayName());
        } else {
            ChatUtil.info("Все предметы собраны, работа завершена");
        }
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (this.target == null || mc.player == null) {
            return;
        }
        if (mc.player.getInventory().getFreeSlot() == -1) {
            ChatUtil.error("Автоматическое отключение: нет свободных слотов в инвентаре, освободите место");
            this.setEnabled(false);
            return;
        }
        this.tickCounter++;
        if (mc.gui.screen() instanceof ContainerScreen screen) {
            this.handleContainer(screen);
        } else if (mc.player.tickCount >= 200 && this.searchCounter.elapsed(1000L, 300L)) {
            mc.player.connection.sendCommand("ah search " + this.target.getSearchName());
            this.searchCounter.reset();
            this.tickCounter = 0;
        }
    }

    private void handleContainer(ContainerScreen screen) {
        String title = screen.getTitle().getString().replaceAll("\u00a7.", "").toLowerCase(Locale.ROOT).trim();
        List<net.minecraft.world.inventory.Slot> slots = screen.getMenu().slots;
        if (title.contains(this.target.getSearchName().toLowerCase(Locale.ROOT))) {
            if (this.searchCounter.elapsed(300L, 80L) && this.haveCount(this.target) >= this.targetUpper(this.target)) {
                ChatUtil.info("Предмет " + this.target.getDisplayName() + " приобретен, перехожу к следующему");
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

    private void handleScanMode(ContainerScreen screen, String title, List<net.minecraft.world.inventory.Slot> slots) {
        Matcher matcher = PAGE_PATTERN.matcher(title);
        if (!matcher.find()) {
            return;
        }
        int currentPage = Integer.parseInt(matcher.group(1));
        int lastScanPage = Math.min(4, Integer.parseInt(matcher.group(2)));
        if (this.offer == null) {
            if (this.offers.stream().noneMatch(offer -> offer.page == currentPage)) {
                for (net.minecraft.world.inventory.Slot slot : slots) {
                    if (this.isBuy(slot.getItem())) {
                        this.offers.add(new Offer(currentPage, slot.index, AutoBuyUtil.getServerPrice(slot.getItem())));
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
            net.minecraft.world.inventory.Slot offerSlot = slots.stream()
                    .filter(slot -> this.isBuy(slot.getItem()) && AutoBuyUtil.getServerPrice(slot.getItem()) == this.offer.price)
                    .findFirst()
                    .orElse(slots.stream().filter(slot -> this.isBuy(slot.getItem()))
                            .min(Comparator.comparingInt(slot -> AutoBuyUtil.getServerPrice(slot.getItem())))
                            .orElse(null));
            if (offerSlot != null) {
                this.clickSlot(offerSlot.index);
                this.searchCounter.reset();
            } else {
                ChatUtil.info("Оффер пропал, пересканирую");
                this.offer = null;
                this.offers.clear();
                this.close();
            }
        } else {
            this.clickButton(slots, currentPage < this.offer.page ? "следующая страница" : "предыдущая страница");
        }
    }

    private void handleBuyMode(ContainerScreen screen, String title, List<net.minecraft.world.inventory.Slot> slots) {
        List<net.minecraft.world.inventory.Slot> buyable = slots.stream().filter(slot -> this.isBuy(slot.getItem())).toList();
        int minPrice = buyable.stream().mapToInt(slot -> AutoBuyUtil.getServerPrice(slot.getItem())).min().orElse(0);
        List<net.minecraft.world.inventory.Slot> affordable = buyable.stream()
                .filter(slot -> AutoBuyUtil.getServerPrice(slot.getItem()) <= Math.round((float) (minPrice * 2)))
                .filter(slot -> slot.getItem().getCount() <= this.targetUpper(this.target) - this.haveCount(this.target))
                .toList();
        net.minecraft.world.inventory.Slot cheapest = affordable.stream()
                .filter(slot -> slot.getItem().getCount() >= this.targetLower(this.target) - this.haveCount(this.target))
                .min(Comparator.comparingInt(slot -> AutoBuyUtil.getServerPrice(slot.getItem())))
                .orElse(affordable.stream().min(Comparator.comparingInt(slot -> AutoBuyUtil.getServerPrice(slot.getItem()))).orElse(null));
        if (cheapest != null) {
            this.clickSlot(cheapest.index);
            this.searchCounter.reset();
        } else {
            Matcher matcher = PAGE_PATTERN.matcher(title);
            if (matcher.find()) {
                int currentPage = Integer.parseInt(matcher.group(1));
                int totalPages = Integer.parseInt(matcher.group(2));
                if (totalPages == 1) {
                    ChatUtil.info("Пропускаем предмет " + this.target.getDisplayName() + ", ибо нету подходящего");
                    this.startWork();
                    this.close();
                    return;
                }
                this.clickButton(slots, currentPage == 1 ? "следующая страница" : "предыдущая страница");
            }
        }
    }

    private boolean isBuy(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (this.target == null || stack.isEmpty() || !this.target.isBuy(stack)) {
            return false;
        }
        ItemStack inventory = null;
        for (int i = 0; i < 40; i++) {
            ItemStack stackInSlot = mc.player.getInventory().getItem(i);
            if (!stackInSlot.isEmpty() && this.target.isBuy(stackInSlot)) {
                inventory = stackInSlot;
                break;
            }
        }
        if (inventory != null && !inventory.isEmpty()
                && ((this.target.getItemStack().getItem() instanceof PotionItem)
                && !java.util.Objects.equals(stack.get(DataComponents.POTION_CONTENTS), inventory.get(DataComponents.POTION_CONTENTS))
                || !stack.getHoverName().getString().trim().equalsIgnoreCase(inventory.getHoverName().getString().trim()))) {
            return false;
        }
        if (stack.getTooltipLines(Item.TooltipContext.EMPTY, mc.player, TooltipFlag.NORMAL).stream()
                .anyMatch(line -> line.getString().contains("➥ Нажмите, чтобы забрать"))) {
            return false;
        }
        if (AutoBuyUtil.getServerPrice(stack) <= 0) {
            return false;
        }
        if (this.target.getItemStack().getItem() == Items.TOTEM_OF_UNDYING && stack.hasFoil()) {
            return false;
        }
        if (this.target.getItemStack().getItem() == Items.ELYTRA && stack.get(DataComponents.DAMAGE) != null) {
            int damage = stack.get(DataComponents.DAMAGE);
            return ((float) (432 - damage)) / 432.0f >= 0.5f;
        }
        if (!this.isArmorItem(stack)
                || stack.get(DataComponents.DAMAGE) == null
                || stack.getMaxDamage() <= 0) {
            return true;
        }
        return this.hasMendingCondition() || ((float) (stack.getMaxDamage() - stack.getDamageValue())) / (float) stack.getMaxDamage() >= 0.7f;
    }

    private boolean isArmorItem(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }
        return switch (equippable.slot()) {
            case HEAD, CHEST, LEGS, FEET, BODY -> true;
            default -> false;
        };
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
        Minecraft mc = Minecraft.getInstance();
        int count = 0;
        for (int i = 0; i < 40; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
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

    private void clickButton(List<net.minecraft.world.inventory.Slot> slots, String name) {
        slots.stream()
                .filter(slot -> slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .ifPresent(slot -> InventoryUtil.quickMoveSlot(slot.index));
    }

    private void clickSlot(int slotId) {
        InventoryUtil.quickMoveSlot(slotId);
    }

    private void close() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof ContainerScreen) {
            mc.player.closeContainer();
        }
    }

    @EventTarget
    public void onPacket(PacketReceiveEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (this.target == null || mc.player == null) {
            return;
        }
        Object packet = event.getPacket();
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket packetObj) {
            String message = packetObj.content().getString();
            if (message.toLowerCase(Locale.ROOT).contains("[✘] ошибка! этот товар уже купили!")) {
                this.offer = null;
                this.offers.clear();
            } else if (message.contains("[✘] Ошибка! У Вас не хватает Монет!")) {
                ChatUtil.error("Автоматическое отключение из-за нехватки баланса на аккаунте");
                this.setEnabled(false);
            } else if (message.contains("Данная команда недоступна в режиме AFK")) {
                ChatUtil.error("Команда недоступна в режиме AFK");
            }
        } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundOpenScreenPacket && !(mc.gui.screen() instanceof ContainerScreen)) {
            if (this.tickCounter >= 8) {
                int anarchy = (int) (Math.random() * 100.0 <= 50.0 ? Math.random() * 26.0 + 205.0 : Math.random() * 20.0 + 305.0);
                mc.player.connection.sendCommand("an" + anarchy);
                ChatUtil.info("Обнаружили замедление аукциона, переходим на " + anarchy + " анархию");
            }
            this.anarchyCounter.reset();
        }
    }

    @EventTarget
    public void onKey(KeyboardInputEvent event) {
        if (this.target == null) {
            return;
        }
        if (event.getKey() == 256 && event.getAction() == 1) {
            this.target = null;
            ChatUtil.info("Работа модуля была принудительно завершена");
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ChatUtil.info("Модуль ищет самые дешевые лоты среди тех которые есть, имейте это ввиду, и будьте осторожны!");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.target = null;
        this.offer = null;
        this.offers.clear();
        if (!this.entries.isEmpty()) {
            AutoBuyConfig.saveCollect(this.entries);
        }
    }

    public List<CollectorItemBuy> getEntries() {
        this.ensureEntries();
        return this.entries;
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