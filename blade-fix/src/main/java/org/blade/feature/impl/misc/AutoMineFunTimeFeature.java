package org.blade.feature.impl.misc;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.blade.context.MinecraftContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.render.Render2DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.MultiSelectSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;
import org.blade.utils.text.ChatUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoMineFunTimeFeature extends Feature implements MinecraftContext {
    private static final String ORE_COAL = "Уголь";
    private static final String ORE_IRON = "Железо";
    private static final String ORE_REDSTONE = "Редстоун";
    private static final String ORE_GOLD = "Золото";
    private static final String ORE_EMERALD = "Изумруды";
    private static final String ORE_DIAMOND = "Алмазы";
    private static final String ORE_NETHERITE = "Незерит";
    private static final String ORE_LAPIS = "Лазурит";
    private static final String ORE_NETHER_GOLD = "Адская золотая руда";

    private static final BlockPos ZONE_MIN = new BlockPos(-86, 72, -5);
    private static final BlockPos ZONE_MAX = new BlockPos(-66, 81, 15);
    private static final long CHECK_INTERVAL = 1000L;
    private static final long RESTART_COOLDOWN = 5000L;
    private static final int ORE_RADIUS = 8;
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

    public final MultiSelectSetting ores = register(new MultiSelectSetting(
            "Руды",
            Set.of(ORE_COAL, ORE_IRON, ORE_REDSTONE, ORE_GOLD, ORE_EMERALD, ORE_DIAMOND, ORE_NETHERITE, ORE_LAPIS, ORE_NETHER_GOLD),
            ORE_COAL, ORE_IRON, ORE_REDSTONE, ORE_GOLD, ORE_EMERALD, ORE_DIAMOND, ORE_NETHERITE, ORE_LAPIS, ORE_NETHER_GOLD
    ));
    public final BooleanSetting autoRestart = register(new BooleanSetting("Авто-перезапуск", true));
    public final BooleanSetting showPanel = register(new BooleanSetting("Панель", true));
    public final BooleanSetting noBreakOutside = register(new BooleanSetting("Не ломать вне зоны", true));
    public final BooleanSetting noPlace = register(new BooleanSetting("Не ставить блоки", true));

    private boolean mining;
    private boolean waiting;
    private long lastCheckTime;
    private long lastMineAt;
    private String cachedTimer = "";

    public AutoMineFunTimeFeature() {
        super("AutoMineFunTime", "Автоматический копатель через Baritone в зоне шахты", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        this.mining = false;
        this.waiting = false;
        this.lastCheckTime = 0L;
        this.lastMineAt = 0L;
        this.cachedTimer = "";
        this.applyBaritoneRestrictions();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        this.stopMining();
        this.restoreBaritoneSettings();
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        if (!inGame() || player().connection == null) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastCheckTime < CHECK_INTERVAL) {
            return;
        }
        this.lastCheckTime = currentTime;

        if (this.ores.getValue().isEmpty()) {
            this.stopMining();
            this.waiting = false;
            this.cachedTimer = "";
            return;
        }

        if (!this.isInMineZone(player().blockPosition())) {
            this.stopMining();
            this.waiting = false;
            this.cachedTimer = "";
            return;
        }

        if (!this.hasOresInZone()) {
            this.stopMining();
            this.waiting = true;
            this.cachedTimer = this.findMineTimer();
            return;
        }

        this.waiting = false;
        this.cachedTimer = "";
        if (!this.mining) {
            this.startMining();
        } else if (this.autoRestart.getValue() && currentTime - this.lastMineAt > RESTART_COOLDOWN) {
            this.startMining();
        }
    }

    @EventTarget
    private void onRender2D(Render2DEvent event) {
        if (!this.showPanel.getValue() || !inGame()) {
            return;
        }

        String status;
        String timerText;
        if (!this.isInMineZone(player().blockPosition())) {
            status = "Вне зоны шахты";
            timerText = "Обновление: —";
        } else if (this.waiting) {
            status = "Жду обновления шахты...";
            timerText = this.cachedTimer.isEmpty() ? "Обновление: —" : "Обновление через: " + this.cachedTimer;
        } else {
            status = "Копаю...";
            timerText = "Обновление: —";
        }

        float titleWidth = measureWidth("AutoMine", 7.0F);
        float infoWidthA = measureWidth(status, 6.0F);
        float infoWidthB = measureWidth(timerText, 6.0F);
        float textWidth = Math.max(titleWidth, Math.max(infoWidthA, infoWidthB));

        int paddingX = 8;
        int paddingY = 6;
        int iconWidth = 18;
        int gap = 6;
        float panelWidth = iconWidth + gap + textWidth + 16.0F;
        float panelHeight = 16.0F + 2.0F * 11.0F + (float) paddingY * 2.0F - 6.0F;

        float centerX = event.getGuiGraphicsExtractor().guiWidth() / 2.0F;
        float topY = event.getGuiGraphicsExtractor().guiHeight() / 7.5F + 22.0F;
        float panelX = centerX - panelWidth / 2.0F;
        float panelY = topY;

        int accent = 0xFF6082FF;
        Render2DUtil.rect(panelX, panelY, panelWidth, panelHeight)
                .color(0x8C000000)
                .radius(6.0F)
                .draw();
        Render2DUtil.rect(panelX, panelY, 3.0F, panelHeight)
                .color(accent)
                .radius(6.0F)
                .draw();

        float textX = panelX + iconWidth + gap + 8.0F;
        float lineY = panelY + paddingY + 1.0F;
        Render2DUtil.text(textX, lineY, 7.0F, "AutoMine")
                .style(UiFontStyle.MEDIUM)
                .color(accent)
                .draw();
        lineY += 11.0F;
        Render2DUtil.text(textX, lineY, 6.0F, status)
                .style(UiFontStyle.MEDIUM)
                .color(0xFFC6C6C6)
                .draw();
        lineY += 11.0F;
        Render2DUtil.text(textX, lineY, 6.0F, timerText)
                .style(UiFontStyle.MEDIUM)
                .color(0xFFC6C6C6)
                .draw();
    }

    private boolean isInMineZone(BlockPos pos) {
        return pos.getX() >= Math.min(ZONE_MIN.getX(), ZONE_MAX.getX())
                && pos.getX() <= Math.max(ZONE_MIN.getX(), ZONE_MAX.getX())
                && pos.getY() >= Math.min(ZONE_MIN.getY(), ZONE_MAX.getY())
                && pos.getY() <= Math.max(ZONE_MIN.getY(), ZONE_MAX.getY())
                && pos.getZ() >= Math.min(ZONE_MIN.getZ(), ZONE_MAX.getZ())
                && pos.getZ() <= Math.max(ZONE_MIN.getZ(), ZONE_MAX.getZ());
    }

    private boolean hasOresInZone() {
        BlockPos playerPos = player().blockPosition();
        int minY = Math.min(ZONE_MIN.getY(), ZONE_MAX.getY());
        int maxY = Math.max(ZONE_MIN.getY(), ZONE_MAX.getY());
        for (int dx = -ORE_RADIUS; dx <= ORE_RADIUS; dx++) {
            for (int y = minY; y <= maxY; y++) {
                for (int dz = -ORE_RADIUS; dz <= ORE_RADIUS; dz++) {
                    BlockPos checkPos = new BlockPos(playerPos.getX() + dx, y, playerPos.getZ() + dz);
                    if (!this.isInMineZone(checkPos)) {
                        continue;
                    }
                    if (this.isOre(level().getBlockState(checkPos).getBlock())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOre(Block block) {
        return this.getOreName(block) != null;
    }

    private String getOreName(Block block) {
        for (String oreName : this.ores.getValue()) {
            for (Block oreBlock : this.getOreBlocks(oreName)) {
                if (oreBlock == block) {
                    return oreName;
                }
            }
        }
        return null;
    }

    private List<Block> getOreBlocks(String oreName) {
        List<Block> blocks = new ArrayList<>();
        switch (oreName) {
            case ORE_COAL -> {
                blocks.add(Blocks.COAL_ORE);
                blocks.add(Blocks.DEEPSLATE_COAL_ORE);
            }
            case ORE_IRON -> {
                blocks.add(Blocks.IRON_ORE);
                blocks.add(Blocks.DEEPSLATE_IRON_ORE);
            }
            case ORE_REDSTONE -> {
                blocks.add(Blocks.REDSTONE_ORE);
                blocks.add(Blocks.DEEPSLATE_REDSTONE_ORE);
            }
            case ORE_GOLD -> {
                blocks.add(Blocks.GOLD_ORE);
                blocks.add(Blocks.DEEPSLATE_GOLD_ORE);
            }
            case ORE_EMERALD -> {
                blocks.add(Blocks.EMERALD_ORE);
                blocks.add(Blocks.DEEPSLATE_EMERALD_ORE);
            }
            case ORE_DIAMOND -> {
                blocks.add(Blocks.DIAMOND_ORE);
                blocks.add(Blocks.DEEPSLATE_DIAMOND_ORE);
            }
            case ORE_NETHERITE -> blocks.add(Blocks.ANCIENT_DEBRIS);
            case ORE_LAPIS -> {
                blocks.add(Blocks.LAPIS_ORE);
                blocks.add(Blocks.DEEPSLATE_LAPIS_ORE);
            }
            case ORE_NETHER_GOLD -> blocks.add(Blocks.NETHER_GOLD_ORE);
            default -> {
            }
        }
        return blocks;
    }

    private void startMining() {
        if (player().connection == null || this.ores.getValue().isEmpty()) {
            return;
        }
        List<String> blockNames = new ArrayList<>();
        for (String oreName : this.ores.getValue()) {
            for (Block block : this.getOreBlocks(oreName)) {
                blockNames.add(BuiltInRegistries.BLOCK.getKey(block).getPath());
            }
        }
        this.mining = true;
        this.lastMineAt = System.currentTimeMillis();
        StringBuilder command = new StringBuilder("#mine");
        for (String blockName : blockNames) {
            command.append(' ').append(blockName);
        }
        player().connection.sendChat(command.toString());
    }

    private void stopMining() {
        if (!this.mining || player().connection == null) {
            return;
        }
        this.mining = false;
        player().connection.sendChat("#stop");
    }

    private void applyBaritoneRestrictions() {
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI", true, classLoader);
            Object settings = apiClass.getMethod("getSettings").invoke(null);
            Class<?> settingsClass = settings.getClass();
            if (this.noPlace.getValue()) {
                this.setSettingValue(settingsClass, settings, "allowPlace", false);
            }
            if (this.noBreakOutside.getValue()) {
                int zoneMinY = Math.min(ZONE_MIN.getY(), ZONE_MAX.getY());
                int zoneMaxY = Math.max(ZONE_MIN.getY(), ZONE_MAX.getY());
                this.setSettingValue(settingsClass, settings, "minYLevelWhileMining", zoneMinY);
                this.setSettingValue(settingsClass, settings, "maxYLevelWhileMining", zoneMaxY);
            }
        } catch (Throwable throwable) {
            ChatUtil.error("AutoMineFunTime: не удалось применить ограничения Baritone");
        }
    }

    private void setSettingValue(Class<?> settingsClass, Object settings, String fieldName, Object value)
            throws ReflectiveOperationException {
        Object setting = settingsClass.getField(fieldName).get(settings);
        setting.getClass().getField("value").set(setting, value);
    }

    private void restoreBaritoneSettings() {
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI", true, classLoader);
            Object settings = apiClass.getMethod("getSettings").invoke(null);
            Class<?> settingsClass = settings.getClass();
            this.setSettingValue(settingsClass, settings, "allowPlace", true);
            this.setSettingValue(settingsClass, settings, "minYLevelWhileMining", -64);
            this.setSettingValue(settingsClass, settings, "maxYLevelWhileMining", 320);
        } catch (Throwable throwable) {
            // ignore
        }
    }

    private String findMineTimer() {
        if (!inGame()) {
            return "";
        }
        List<ArmorStand> armorStands = new ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : level().getEntities(
                (net.minecraft.world.entity.Entity) null,
                new net.minecraft.world.phys.AABB(player().blockPosition()).inflate(64.0D),
                e -> e instanceof ArmorStand && e.getCustomName() != null)) {
            armorStands.add((ArmorStand) entity);
        }
        armorStands.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player().position())));

        for (ArmorStand entity : armorStands) {
            String name = stripColor(entity.getCustomName().getString()).trim();
            if (!name.contains("Обновление через:")) {
                continue;
            }
            ArmorStand closestTimer = null;
            double bestDistance = Double.MAX_VALUE;
            for (ArmorStand candidate : armorStands) {
                String candidateName = stripColor(candidate.getCustomName().getString()).trim();
                if (TIME_PATTERN.matcher(candidateName).matches()) {
                    double dy = Math.abs(candidate.getY() - entity.getY());
                    if (dy < bestDistance) {
                        bestDistance = dy;
                        closestTimer = candidate;
                    }
                }
            }
            if (closestTimer != null) {
                Matcher matcher = TIME_PATTERN.matcher(stripColor(closestTimer.getCustomName().getString()).trim());
                if (matcher.matches()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    return String.format("%02d:%02d", minutes, seconds);
                }
            }
            break;
        }
        return "";
    }

    private static String stripColor(String text) {
        return ChatFormatting.stripFormatting(text);
    }

    private static float measureWidth(String text, float size) {
        return UiFonts.getFontForText(text, UiFontStyle.MEDIUM.weight()).measureWidth(text, size);
    }
}