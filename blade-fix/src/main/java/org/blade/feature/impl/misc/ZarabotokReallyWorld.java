package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.pve.navigation.BaritoneNavigator;
import org.blade.utils.inventory.InventoryUtil;
import org.blade.utils.math.BladeTimer;
import org.blade.utils.text.ChatUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ZarabotokReallyWorld extends Feature {
    public static final ZarabotokReallyWorld INSTANCE = new ZarabotokReallyWorld();

    private final ModeSetting resource = register(new ModeSetting("Ресурс", "Паутина", "Паутина", "Древние обломки"));
    private final ModeSetting priceMode = register(new ModeSetting("Расчёт цены", "Минимальная", "Минимальная", "Средняя"));
    private final NumberSetting stackTarget = register(new NumberSetting("Стопка (шт)", 64.0D, 1.0D, 2304.0D, 1.0D, ""));
    private final NumberSetting marginPercent = register(new NumberSetting("Отнять % от цены", 10.0D, 0.0D, 50.0D, 1.0D, ""));
    private final BooleanSetting sellToggle = register(new BooleanSetting("Продавать на маркете", true));
    private final BooleanSetting depositToggle = register(new BooleanSetting("Складывать в сундуки", false));
    private final NumberSetting depositStacks = register(new NumberSetting("Стопки для склада", 4.0D, 1.0D, 36.0D, 1.0D, ""));
    private final NumberSetting chestRadius = register(new NumberSetting("Радиус сундуков (блоки)", 15.0D, 5.0D, 50.0D, 1.0D, ""));
    private final NumberSetting rtpCooldownSeconds = register(new NumberSetting("Кулдаун /rtpdefault (сек)", 30.0D, 5.0D, 300.0D, 5.0D, ""));
    private final BooleanSetting rtpOnDamage = register(new BooleanSetting("Телепорт при уроне", false));
    private final BooleanSetting detectHostiles = register(new BooleanSetting("Детект враждебных мобов", false));
    private final NumberSetting mineRetrySeconds = register(new NumberSetting("Повтор #mine при ошибке (сек)", 5.0D, 1.0D, 60.0D, 1.0D, ""));

    private static final Pattern COOLDOWN_SECONDS = Pattern.compile("(\\d+)\\s*(?:сек|sec)");
    private static final Pattern COOLDOWN_MINUTES = Pattern.compile("(\\d+)\\s*(?:мин|min)");
    private static final Pattern COOLDOWN_AFTER_WAIT = Pattern.compile("подожд\\w*\\s+(\\d+)");

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
    private final BladeTimer timer = new BladeTimer();
    private boolean cmdSent;
    private boolean baritoneStarted;
    private boolean walkStarted;
    private int sellPrice;
    private BlockPos currentChest;
    private final Set<BlockPos> triedChests = new HashSet<>();
    private final Set<BlockPos> protectedSpots = new HashSet<>();
    private boolean rtpCooldown;
    private final BladeTimer rtpCooldownTimer = new BladeTimer();
    private int rtpAttempts;
    private Vec3 rtpStartPos;
    private int shearsStep;
    private final BladeTimer shearsTimer = new BladeTimer();
    private int craftSubStep;
    private int craftSlotA;
    private int craftSlotB;
    private boolean craftNeedSecond;
    private float lastHealth;
    private final BladeTimer damageTpTimer = new BladeTimer();
    private final BladeTimer hostileTpTimer = new BladeTimer();
    private Vec3 walkTarget;
    private int collectedCount;
    private volatile boolean pendingProtectedArea;
    private volatile boolean pendingRtpCooldown;
    private volatile long pendingRtpCooldownSec = -1L;
    private volatile boolean pendingMineRetry;
    private final BladeTimer mineRetryTimer = new BladeTimer();
    private int mineRetryCount;
    private long rtpCooldownMs;

    private ZarabotokReallyWorld() {
        super("ZarabotokReallyWorld", "Авто-фарм ресурсов и продажа на маркете (ReallyWorld)", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    private Profile profile() {
        String key = this.resource.getValue();
        if ("Древние обломки".equals(key)) {
            return new Profile("ancient_debris", Items.ANCIENT_DEBRIS, "Древние обломки");
        }
        return new Profile("cobweb", Items.COBWEB, "Паутина");
    }

    private boolean isAncientDebris() {
        return "Древние обломки".equals(this.resource.getValue());
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
        this.pendingProtectedArea = false;
        this.pendingRtpCooldown = false;
        this.pendingRtpCooldownSec = -1L;
        this.pendingMineRetry = false;
        this.mineRetryCount = 0;
        this.mineRetryTimer.reset();
        this.rtpCooldownMs = 0L;
        this.timer.reset();
        this.lastHealth = -1.0F;
        this.damageTpTimer.reset();
        this.hostileTpTimer.reset();
    }

    @Override
    public void onDisable() {
        this.stopBaritone();
        this.pendingProtectedArea = false;
        this.pendingRtpCooldown = false;
        this.pendingRtpCooldownSec = -1L;
        this.pendingMineRetry = false;
        super.onDisable();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.connection == null) {
            return;
        }
        this.applyPendingChatTriggers();
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
    public void onPacket(PacketReceiveEvent event) {
        String text = incomingText(event.getPacket());
        if (text == null) {
            return;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (this.isRtpCooldown(lower)) {
            this.pendingRtpCooldown = true;
            this.pendingRtpCooldownSec = parseCooldownSeconds(lower);
        }
        if (this.isMineNoPath(lower)) {
            this.pendingMineRetry = true;
        }
        if (this.isProtectedArea(lower)) {
            this.pendingProtectedArea = true;
        }
    }

    private static String incomingText(Object packet) {
        if (packet instanceof ClientboundSystemChatPacket systemChat) {
            return systemChat.content().getString();
        }
        if (packet instanceof ClientboundSetActionBarTextPacket actionBar) {
            return actionBar.text().getString();
        }
        if (packet instanceof ClientboundSetTitleTextPacket title) {
            return title.text().getString();
        }
        if (packet instanceof ClientboundSetSubtitleTextPacket subtitle) {
            return subtitle.text().getString();
        }
        return null;
    }

    private void applyPendingChatTriggers() {
        Minecraft mc = Minecraft.getInstance();
        if (this.pendingRtpCooldown) {
            this.pendingRtpCooldown = false;
            long parsed = this.pendingRtpCooldownSec;
            this.pendingRtpCooldownSec = -1L;
            if (this.stage == Stage.RTP || this.stage == Stage.WAIT_TP) {
                long seconds = parsed > 0L ? parsed : this.rtpCooldownSeconds.getValue().longValue();
                this.rtpCooldownMs = seconds * 1000L;
                this.rtpCooldown = true;
                this.rtpCooldownTimer.reset();
                this.cmdSent = false;
                this.stage = Stage.RTP;
                ChatUtil.info("Задержка /rtpdefault: ждём " + seconds + " сек");
            }
        }
        if (this.pendingMineRetry) {
            if (this.stage != Stage.MINE || !this.baritoneStarted) {
                this.pendingMineRetry = false;
            } else if (this.mineRetryTimer.finished(this.mineRetrySeconds.getValue().longValue() * 1000L)) {
                this.mineRetryTimer.reset();
                this.pendingMineRetry = false;
                if (this.mineRetryCount == 0) {
                    this.mineRetryCount = 1;
                    ChatUtil.info("Baritone не нашёл путь — повторяю #mine");
                    this.send("#mine " + this.profile().block);
                } else {
                    ChatUtil.info("Путь не найден повторно — телепортируюсь (/rtpdefault)");
                    this.mineRetryCount = 0;
                    this.stopBaritone();
                    this.pendingProtectedArea = false;
                    this.rtpAttempts = 0;
                    this.cmdSent = false;
                    this.stage = this.isAncientDebris() ? Stage.NETHER_WALK : Stage.RTP;
                }
            }
        }
        if (!this.pendingProtectedArea) {
            return;
        }
        this.pendingProtectedArea = false;
        if (this.stage != Stage.MINE) {
            return;
        }
        this.stopBaritone();
        this.protectedSpots.add(mc.player.blockPosition());
        this.rtpAttempts = 0;
        this.cmdSent = false;
        ChatUtil.info("Регион защищён — телепортируюсь (/rtpdefault)");
        this.stage = this.isAncientDebris() ? Stage.NETHER_WALK : Stage.RTP;
    }

    private boolean isProtectedArea(String lower) {
        return lower.contains("sorry, but you cant break")
                || lower.contains("sorry, but you can't break")
                || lower.contains("you cant break that block")
                || lower.contains("you can't break that block")
                || lower.contains("не можете сломать")
                || lower.contains("не можете ломать")
                || lower.contains("нельзя сломать")
                || lower.contains("нельзя ломать")
                || lower.contains("could not break this")
                || lower.contains("cannot break")
                || lower.contains("cannot get")
                || lower.contains("cannot use")
                || lower.contains("cannot")
                || lower.contains("защищен")
                || lower.contains("защищён")
                || lower.contains("приват")
                || lower.contains("but you can")
                || lower.contains("but you cant")
                || lower.contains("but you can't")
                || lower.contains("but you cannot")
                || lower.contains("zone protected")
                || lower.contains("this zone");
    }

    private boolean isNearProtectedSpot() {
        Minecraft mc = Minecraft.getInstance();
        for (BlockPos spot : this.protectedSpots) {
            if (mc.player.distanceToSqr(Vec3.atCenterOf(spot)) < 400.0) {
                return true;
            }
        }
        return false;
    }

    private void checkDamageTeleport() {
        Minecraft mc = Minecraft.getInstance();
        float health = mc.player.getHealth();
        boolean tookDamage = this.lastHealth >= 0.0F && health < this.lastHealth - 0.5F;
        this.lastHealth = health;
        if (this.rtpOnDamage.getValue() && tookDamage && this.damageTpTimer.finished(10000L)) {
            this.damageTpTimer.reset();
            this.startEmergencyTeleport("Получен урон — телепортируюсь (/rtpdefault)");
        }
    }

    private void checkHostileMobs() {
        Minecraft mc = Minecraft.getInstance();
        if (!this.detectHostiles.getValue() || mc.level == null || mc.player == null) {
            return;
        }
        if (!this.hostileTpTimer.finished(10000L)) {
            return;
        }
        AABB box = mc.player.getBoundingBox().inflate(10.0);
        List<Entity> monsters = mc.level.getEntities(mc.player, box, (entity) -> entity instanceof Monster);
        if (!monsters.isEmpty()) {
            this.hostileTpTimer.reset();
            this.startEmergencyTeleport("Враждебный моб рядом — телепортируюсь (/rtpdefault)");
        }
    }

    private void startEmergencyTeleport(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (this.stage == Stage.RTP || this.stage == Stage.WAIT_TP) {
            return;
        }
        if (this.baritoneStarted) {
            this.stopBaritone();
        }
        this.rtpCooldown = false;
        this.rtpAttempts = 0;
        this.cmdSent = false;
        this.timer.reset();
        this.walkTarget = null;
        if (mc.gui.screen() != null) {
            mc.player.closeContainer();
        }
        ChatUtil.info(message);
        this.stage = this.isAncientDebris() ? Stage.NETHER_WALK : Stage.RTP;
    }

    private void stageShears() {
        Minecraft mc = Minecraft.getInstance();
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
                    ChatUtil.info("Нет ножниц и железа в инвентаре — модуль остановлен");
                    this.setEnabled(false);
                }
                break;

            case 2:
                this.removeSwordFromHotbar();
                this.shearsStep = 99;
                break;

            case 3:
                if (this.craftSubStep == 0) {
                    if (!this.startShearsCraft()) {
                        ChatUtil.info("Не нашёл железо для крафта — модуль остановлен");
                        this.setEnabled(false);
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
                        ChatUtil.info("Крафт ножниц не удался — чищу сетку и пробую заново");
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
        Minecraft mc = Minecraft.getInstance();
        if (!this.baritoneStarted) {
            if (!this.isBaritonePresent()) {
                ChatUtil.info("Установите Baritone для работы ZarabotokReallyWorld");
                this.setEnabled(false);
                return;
            }
            if (this.walkTarget == null) {
                float yaw = mc.player.getYRot();
                double rad = Math.toRadians(yaw);
                double dx = -Math.sin(rad) * 300.0;
                double dz = Math.cos(rad) * 300.0;
                this.walkTarget = mc.player.position().add(dx, 0.0, dz);
            }
            this.baritoneStarted = true;
            this.walkStarted = false;
            this.timer.reset();
            this.send("#goto " + (int) this.walkTarget.x + " " + (int) this.walkTarget.y + " " + (int) this.walkTarget.z);
            return;
        }
        if (!this.walkStarted) {
            this.walkStarted = true;
            this.timer.reset();
        }
        if (mc.player.distanceToSqr(this.walkTarget) < 25.0) {
            this.stopBaritone();
            this.walkStarted = false;
            this.walkTarget = null;
            this.stage = Stage.MINE;
            ChatUtil.info("Дошёл до точки — начинаю копать обломки");
        } else if (this.timer.finished(180000L)) {
            this.stopBaritone();
            this.walkStarted = false;
            this.walkTarget = null;
            this.stage = Stage.MINE;
            ChatUtil.info("Не дошёл за 3 минуты — копаю с текущей точки");
        }
    }

    private boolean hasShearsInHotbar() {
        Minecraft mc = Minecraft.getInstance();
        for (int h = 0; h < 9; h++) {
            if (mc.player.getInventory().getItem(h).is(Items.SHEARS)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasShearsInInventory() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.SHEARS)) {
                return true;
            }
        }
        return false;
    }

    private int ironCount() {
        Minecraft mc = Minecraft.getInstance();
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.IRON_INGOT)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int findIronSlot() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.IRON_INGOT)) {
                return i < 9 ? 36 + i : i;
            }
        }
        return -1;
    }

    private int findIronSlotExcept(int except) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 36; i++) {
            int id = i < 9 ? 36 + i : i;
            if (id == except) {
                continue;
            }
            if (mc.player.getInventory().getItem(i).is(Items.IRON_INGOT)) {
                return id;
            }
        }
        return -1;
    }

    private boolean startShearsCraft() {
        Minecraft mc = Minecraft.getInstance();
        this.craftSlotA = this.findIronSlot();
        if (this.craftSlotA == -1) {
            return false;
        }
        int countA = mc.player.inventoryMenu.getSlot(this.craftSlotA).getItem().getCount();
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
                this.clickInvSlot(0, this.craftSlotA, 0, ContainerInput.PICKUP);
                break;
            case 2:
                this.clickInvSlot(0, 2, 1, ContainerInput.PICKUP);
                break;
            case 3:
                if (this.craftNeedSecond) {
                    this.clickInvSlot(0, this.craftSlotB, 0, ContainerInput.PICKUP);
                } else {
                    this.clickInvSlot(0, 3, 1, ContainerInput.PICKUP);
                }
                break;
            case 4:
                if (this.craftNeedSecond) {
                    this.clickInvSlot(0, 3, 1, ContainerInput.PICKUP);
                } else {
                    this.clickInvSlot(0, this.craftSlotA, 0, ContainerInput.PICKUP);
                }
                break;
            case 5:
                if (this.craftNeedSecond) {
                    this.clickInvSlot(0, this.craftSlotB, 0, ContainerInput.PICKUP);
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
        this.clickInvSlot(0, 0, 0, ContainerInput.QUICK_MOVE);
    }

    private boolean isShearsInCraftOutput() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player.inventoryMenu.getSlot(0).getItem().is(Items.SHEARS);
    }

    private void moveShearsToHotbar() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.SHEARS)) {
                int target = -1;
                for (int h = 0; h < 9; h++) {
                    if (mc.player.getInventory().getItem(h).isEmpty()) {
                        target = h;
                        break;
                    }
                }
                if (target == -1) {
                    for (int h = 0; h < 9; h++) {
                        if (this.isSword(mc.player.getInventory().getItem(h).getItem())) {
                            target = h;
                            break;
                        }
                    }
                }
                if (target == -1) {
                    target = 0;
                }
                this.clickInvSlot(0, i, target, ContainerInput.SWAP);
                return;
            }
        }
    }

    private void removeSwordFromHotbar() {
        Minecraft mc = Minecraft.getInstance();
        for (int h = 0; h < 9; h++) {
            if (this.isSword(mc.player.getInventory().getItem(h).getItem())) {
                int free = this.findFreeMainInventorySlot();
                if (free != -1) {
                    this.clickInvSlot(0, free, h, ContainerInput.SWAP);
                }
                return;
            }
        }
    }

    private void cleanCraftGrid() {
        Minecraft mc = Minecraft.getInstance();
        for (int slot = 1; slot <= 4; slot++) {
            if (mc.player.inventoryMenu.getSlot(slot).getItem().isEmpty()) {
                continue;
            }
            int free = this.findFreeMainInventorySlot();
            if (free != -1) {
                this.clickInvSlot(0, slot, 0, ContainerInput.PICKUP);
                this.clickInvSlot(0, free, 0, ContainerInput.PICKUP);
            }
        }
    }

    private int findFreeMainInventorySlot() {
        Minecraft mc = Minecraft.getInstance();
        for (int j = 9; j < 36; j++) {
            if (mc.player.getInventory().getItem(j).isEmpty()) {
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
        Minecraft mc = Minecraft.getInstance();
        if (this.rtpCooldown) {
            long delayMs = this.rtpCooldownMs > 0L
                    ? this.rtpCooldownMs
                    : this.rtpCooldownSeconds.getValue().longValue() * 1000L;
            if (this.rtpCooldownTimer.finished(delayMs)) {
                this.rtpCooldown = false;
                this.rtpAttempts = 0;
                this.cmdSent = false;
                ChatUtil.info("Кулдаун /rtpdefault прошёл — пробую снова");
            }
            return;
        }
        if (!this.cmdSent) {
            this.cmdSent = true;
            this.timer.reset();
            this.rtpStartPos = mc.player.position();
            this.send("/rtpdefault");
            this.stage = Stage.WAIT_TP;
        }
    }

    private void stageWaitTp() {
        Minecraft mc = Minecraft.getInstance();
        if (this.rtpStartPos != null && mc.player.position().distanceTo(this.rtpStartPos) > 10.0) {
            if (this.isNearProtectedSpot()) {
                this.rtpAttempts++;
                this.cmdSent = false;
                if (this.rtpAttempts >= 3) {
                    ChatUtil.info("Везде защищённые регионы — копаю без телепорта");
                    this.stage = Stage.MINE;
                } else {
                    ChatUtil.info("Снова защищённый регион — пробую /rtpdefault ещё раз");
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
                ChatUtil.info("Телепорт не выполнился — продолжаю без него");
                this.stage = Stage.MINE;
            } else {
                ChatUtil.info("Телепорт не выполнился — повторяю /rtpdefault");
                this.stage = Stage.RTP;
            }
        }
    }

    private boolean isRtpCooldown(String lower) {
        return lower.contains("cooldown")
                || lower.contains("кулдаун")
                || lower.contains("куплдаун")
                || lower.contains("задержк")
                || lower.contains("подожд")
                || lower.contains("погод")
                || lower.contains("позже");
    }

    private boolean isMineNoPath(String lower) {
        return lower.contains("unable to find any path")
                || lower.contains("не удалось найти путь")
                || lower.contains("не найден путь");
    }

    private static long parseCooldownSeconds(String lower) {
        Matcher matcher = COOLDOWN_SECONDS.matcher(lower);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        matcher = COOLDOWN_MINUTES.matcher(lower);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1)) * 60L;
        }
        matcher = COOLDOWN_AFTER_WAIT.matcher(lower);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return -1L;
    }

    private void stageMine() {
        Minecraft mc = Minecraft.getInstance();
        Profile profile = this.profile();
        if (!this.baritoneStarted) {
            if (!this.isBaritonePresent()) {
                ChatUtil.info("Установите Baritone для работы ZarabotokReallyWorld");
                this.setEnabled(false);
                return;
            }
            this.baritoneStarted = true;
            this.mineRetryCount = 0;
            this.mineRetryTimer.reset();
            this.send("#mine " + profile.block);
            return;
        }
        int count = this.countItem(mc.player, profile.item);
        boolean depositOnly = this.isAncientDebris();
        if (!depositOnly && this.sellToggle.getValue()) {
            if (count < this.stackTarget.getValue().intValue()) {
                return;
            }
            this.stopBaritone();
            this.stage = Stage.MARKET_OPEN;
        } else if (depositOnly || this.depositToggle.getValue()) {
            if (count < this.depositStacks.getValue().intValue() * 64) {
                return;
            }
            this.stopBaritone();
            this.collectedCount = count;
            this.triedChests.clear();
            this.currentChest = null;
            this.walkStarted = false;
            this.stage = Stage.DEPOSIT_HOME;
        }
    }

    private void stageMarketOpen() {
        Minecraft mc = Minecraft.getInstance();
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
        if (mc.gui.screen() instanceof ContainerScreen) {
            this.cmdSent = false;
            this.timer.reset();
            this.stage = Stage.MARKET_SELECT;
        }
    }

    private void stageMarketSelect() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof ContainerScreen screen)) {
            if (this.timer.finished(4000L)) {
                this.stage = Stage.MINE;
            }
            return;
        }
        int slot = this.findItemSlot(screen, this.profile().item);
        if (slot == -1) {
            ChatUtil.info("На маркете нет " + this.profile().query + " — продолжаю копать");
            this.stage = Stage.CLOSE_MARKET;
            return;
        }
        this.click(screen, slot);
        this.timer.reset();
        this.stage = Stage.MARKET_READ;
    }

    private void stageMarketRead() {
        Minecraft mc = Minecraft.getInstance();
        if (this.timer.finished(5000L)) {
            this.stage = Stage.CLOSE_MARKET;
            return;
        }
        if (!(mc.gui.screen() instanceof ContainerScreen screen)) {
            return;
        }
        List<Integer> prices = this.collectPrices(screen);
        if (prices.isEmpty()) {
            return;
        }
        int chosen = this.priceMode.is("Средняя") ? this.average(prices) : this.minimum(prices);
        this.sellPrice = (int) (chosen * (100.0F - this.marginPercent.getValue().floatValue()) / 100.0F);
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
            ChatUtil.info("Продаю " + this.profile().query + " за " + this.sellPrice);
            this.send("/market sell " + this.sellPrice);
            return;
        }
        if (this.timer.finished(2000L)) {
            this.cmdSent = false;
            this.stage = Stage.CLOSE_MARKET;
        }
    }

    private void stageCloseMarket() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) {
            mc.player.closeContainer();
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
            ChatUtil.info("Нет сундуков в радиусе " + (int) this.chestRadius.getValue().intValue() + " — выбрасываю ресурс");
            this.stage = Stage.DEPOSIT_DROP;
            return;
        }
        this.walkStarted = false;
        this.stage = Stage.DEPOSIT_WALK;
    }

    private void stageDepositWalk() {
        Minecraft mc = Minecraft.getInstance();
        if (!this.walkStarted) {
            this.walkStarted = true;
            this.timer.reset();
            this.send("#goto " + this.currentChest.getX() + " " + this.currentChest.getY() + " " + this.currentChest.getZ());
            return;
        }
        if (mc.player.distanceToSqr(Vec3.atCenterOf(this.currentChest)) < 9.0) {
            this.walkStarted = false;
            this.stage = Stage.DEPOSIT_OPEN;
        } else if (this.timer.finished(30000L)) {
            this.walkStarted = false;
            this.triedChests.add(this.currentChest);
            this.stage = Stage.DEPOSIT_FIND;
        }
    }

    private void stageDepositOpen() {
        Minecraft mc = Minecraft.getInstance();
        if (!this.cmdSent) {
            this.cmdSent = true;
            this.timer.reset();
            this.openChest(this.currentChest);
            return;
        }
        if (mc.gui.screen() instanceof ContainerScreen) {
            this.cmdSent = false;
            this.stage = Stage.DEPOSIT_PUT;
        } else if (this.timer.finished(4000L)) {
            this.cmdSent = false;
            this.triedChests.add(this.currentChest);
            this.stage = Stage.DEPOSIT_FIND;
        }
    }

    private void stageDepositPut() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof ContainerScreen screen)) {
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
        if (this.countItem(mc.player, this.profile().item) == 0) {
            if (mc.gui.screen() != null) {
                mc.player.closeContainer();
            }
            if (this.isAncientDebris()) {
                ChatUtil.info("Собрано " + this.collectedCount + " шт — складываю и останавливаюсь");
                this.setEnabled(false);
            } else {
                ChatUtil.info("Собрано паутины: " + this.collectedCount + " шт — сложена в сундук, телепортируюсь (/rtpdefault)");
                this.stage = Stage.RTP;
            }
        } else {
            this.triedChests.add(this.currentChest);
            if (mc.gui.screen() != null) {
                mc.player.closeContainer();
            }
            this.stage = Stage.DEPOSIT_FIND;
        }
    }

    private void stageDepositDrop() {
        Minecraft mc = Minecraft.getInstance();
        if (!this.cmdSent) {
            this.cmdSent = true;
            this.timer.reset();
            this.dropCobweb();
            return;
        }
        if (this.timer.finished(500L)) {
            this.cmdSent = false;
            if (this.isAncientDebris()) {
                ChatUtil.info("Собрано " + this.collectedCount + " шт — нет сундука, останавливаюсь");
                this.setEnabled(false);
            } else {
                ChatUtil.info("Собрано паутины: " + this.collectedCount + " шт — нет сундука, выбросил и телепортируюсь (/rtpdefault)");
                this.stage = Stage.RTP;
            }
        }
    }

    private BlockPos findNearestChest() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos playerPos = mc.player.blockPosition();
        int radius = this.chestRadius.getValue().intValue();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (this.triedChests.contains(pos)) {
                        continue;
                    }
                    Block block = mc.level.getBlockState(pos).getBlock();
                    if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL) {
                        double dist = mc.player.distanceToSqr(Vec3.atCenterOf(pos));
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
        Minecraft mc = Minecraft.getInstance();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    }

    private void moveCobwebToChest(ContainerScreen screen) {
        AbstractContainerMenu menu = screen.getMenu();
        int containerSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = containerSlots; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem() && slot.getItem().is(this.profile().item)) {
                InventoryUtil.quickMoveSlot(i);
            }
        }
    }

    private void dropCobweb() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(this.profile().item)) {
                int slotId = i < 9 ? 36 + i : i;
                InventoryUtil.dropPlayerStack(mc.player, slotId);
            }
        }
    }

    private List<Integer> collectPrices(ContainerScreen screen) {
        List<Integer> prices = new ArrayList<>();
        String query = this.profile().query.toLowerCase(Locale.ROOT);
        AbstractContainerMenu menu = screen.getMenu();
        int containerSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            String name = this.stripColor(stack.getHoverName().getString());
            if (!name.toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            String digits = name.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                continue;
            }
            try {
                prices.add(Integer.parseInt(digits));
            } catch (NumberFormatException ignored) {
            }
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

    private int findItemSlot(ContainerScreen screen, Item item) {
        AbstractContainerMenu menu = screen.getMenu();
        int containerSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem() && slot.getItem().is(item)) {
                return i;
            }
        }
        return -1;
    }

    private void click(ContainerScreen screen, int slot) {
        InventoryUtil.leftClickSlot(slot);
    }

    private void clickInvSlot(int syncId, int slotId, int button, ContainerInput input) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }
        mc.gameMode.handleContainerInput(syncId, slotId, button, input, mc.player);
    }

    private int countItem(LocalPlayer player, Item item) {
        int count = 0;
        for (int slot = InventoryMenu.INV_SLOT_START;
             slot < InventoryMenu.USE_ROW_SLOT_END;
             slot++) {
            ItemStack stack = player.inventoryMenu.getSlot(slot).getItem();
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private void stopBaritone() {
        if (!this.baritoneStarted) {
            return;
        }
        this.baritoneStarted = false;
        try {
            BaritoneNavigator.INSTANCE.cancel();
        } catch (LinkageError | RuntimeException ignored) {
            if (this.isBaritonePresent()) {
                this.send("#stop");
            }
        }
    }

    private void send(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        if (command.startsWith("/")) {
            mc.player.connection.sendCommand(command.substring(1));
        } else {
            mc.player.connection.sendChat(command);
        }
    }

    private boolean isBaritonePresent() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}