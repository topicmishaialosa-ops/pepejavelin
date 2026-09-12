package org.blade.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.blade.event.EventTarget;
import org.blade.event.events.render.Render2DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Render3DUtil;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SupplyHologramsFeature extends Feature {
    private static final float MAIN_TEXT_SIZE = 13.0F;
    private static final float SUB_TEXT_SIZE = 11.0F;
    private static final float LINE_HEIGHT = 16.0F;
    private static final float PANEL_PADDING = 4.0F;
    private static final float PANEL_RADIUS = 4.0F;
    private static final float BOX_RADIUS = 2.0F;
    private static final float BOX_BORDER = 1.5F;

    private static final int PANEL_BG = ColorUtil.rgba(0, 0, 0, 145);
    private static final int TEXT_SHADOW = ColorUtil.rgba(0, 0, 0, 150);
    private static final int BOX_FILL_ALPHA = 35;
    private static final int BORDER_ALPHA = 170;
    private static final int WHITE = 0xFFFFFFFF;

    private static final String[] KEYWORDS = {"сундук", "припас", "провизия", "ящик"};
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,3}):(\\d{2})(?::(\\d{2}))?");
    private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d{1,4})\\s*(?:час|[чh])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d{1,4})\\s*(?:мин|[мm])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d{1,4})\\s*(?:сек|[сc])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern UNIT_PATTERN = Pattern.compile("\\d{1,4}\\s*(?:час|[чh]|мин|[мm]|сек|[сc])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public final ModeSetting mode = register(new ModeSetting("Режим", "Текст и рамка", "Текст и рамка", "Текст", "Рамка"));
    public final NumberSetting distance = register(new NumberSetting("Дистанция", 64.0D, 8.0D, 256.0D, 8.0D, ""));
    public final BooleanSetting showLines = register(new BooleanSetting("Показывать строки", true));
    public final ColorSetting color = register(new ColorSetting("Цвет", 0xFF00FF8C));

    private final Map<Entity, String> lastTexts = new HashMap<>();
    private final Map<Entity, Long[]> countdowns = new HashMap<>();

    public SupplyHologramsFeature() {
        super("SupplyHolograms", "Highlights supply chest holograms", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Set<Entity> processed = new HashSet<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (processed.contains(entity)) {
                continue;
            }
            if (!isSupplyEntity(entity) || !withinRange(entity, mc)) {
                continue;
            }

            List<Entity> group = new ArrayList<>();
            collectGroup(entity, group, processed, mc);
            if (!group.isEmpty()) {
                renderGroup(group, tickDelta, mc);
            }
        }
    }

    private String getText(Entity entity) {
        if (entity == null) {
            return null;
        }

        String text = null;
        if (entity instanceof Display.TextDisplay textDisplay) {
            Display.TextDisplay.TextRenderState state = textDisplay.textRenderState();
            if (state != null) {
                Component component = state.text();
                text = component != null ? component.getString() : null;
            }
        }
        if (text == null && entity.getCustomName() != null) {
            text = entity.getCustomName().getString();
        }
        if (text == null) {
            return null;
        }

        String stripped = text.replaceAll("§.", "");
        return stripped == null || stripped.isEmpty() ? null : stripped.trim();
    }

    private boolean isSupplyEntity(Entity entity) {
        String text = getText(entity);
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String keyword : KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean withinRange(Entity entity, Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        double max = this.distance.getValue().doubleValue();
        return entity.distanceToSqr(mc.player) <= max * max;
    }

    private void collectGroup(Entity anchor, List<Entity> group, Set<Entity> processed, Minecraft mc) {
        group.add(anchor);
        processed.add(anchor);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == anchor || processed.contains(entity)) {
                continue;
            }
            if (getText(entity) == null) {
                continue;
            }
            if (Math.abs(entity.getX() - anchor.getX()) <= 1.5D
                    && Math.abs(entity.getZ() - anchor.getZ()) <= 1.5D
                    && entity.getY() - anchor.getY() >= -5.0D
                    && entity.getY() - anchor.getY() <= 2.0D) {
                group.add(entity);
                processed.add(entity);
            }
        }
    }

    private void renderGroup(List<Entity> group, float tickDelta, Minecraft mc) {
        Entity top = group.get(0);
        for (int i = 1; i < group.size(); i++) {
            if (group.get(i).getY() > top.getY()) {
                top = group.get(i);
            }
        }

        if (this.mode.is("Рамка") || this.mode.is("Текст и рамка")) {
            renderBox(top, tickDelta, mc);
        }
        if (this.mode.is("Текст") || this.mode.is("Текст и рамка")) {
            renderText(top, group, tickDelta, mc);
        }
    }

    private void renderBox(Entity entity, float tickDelta, Minecraft mc) {
        Vec3 renderPosition = Render3DUtil.interpolatedPosition(entity, tickDelta);
        double halfWidth = entity.getBbWidth() / 2.0D;
        Render3DUtil.ScreenBounds bounds = Render3DUtil.projectBoxBounds(
                mc,
                renderPosition.x - halfWidth, renderPosition.y, renderPosition.z - halfWidth,
                renderPosition.x + halfWidth, renderPosition.y + entity.getBbHeight(), renderPosition.z + halfWidth
        );
        if (bounds == null) {
            return;
        }

        float x = Math.round(bounds.minX()) - 1.0F;
        float y = Math.round(bounds.minY()) - 1.0F;
        float width = Math.round(bounds.maxX()) - x + 1.0F;
        float height = Math.round(bounds.maxY()) - y + 1.0F;
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }

        int highlight = this.color.getValue();
        Render2DUtil.rect(x, y, width, height)
                .color(ColorUtil.withAlpha(highlight, BOX_FILL_ALPHA))
                .radius(BOX_RADIUS)
                .draw();
        Render2DUtil.rect(x, y, width, height)
                .color(0x00000000)
                .radius(BOX_RADIUS)
                .border(BOX_BORDER, highlight)
                .draw();
    }

    private void renderText(Entity top, List<Entity> group, float tickDelta, Minecraft mc) {
        Vec3 renderPosition = Render3DUtil.interpolatedPosition(top, tickDelta)
                .add(0.0D, top.getBbHeight() + 0.35D, 0.0D);
        Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, renderPosition);
        if (anchor == null) {
            return;
        }

        List<Entity> entities = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (Entity entity : group) {
            String text = getText(entity);
            if (text == null) {
                continue;
            }
            for (String part : text.split("\\n")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    entities.add(entity);
                    lines.add(trimmed);
                }
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        MsdfFont font = UiFonts.sfProDisplay();
        int highlight = this.color.getValue();
        float textWidth = 0.0F;
        for (int i = 0; i < lines.size(); i++) {
            boolean main = isMainLine(lines.get(i));
            float size = main ? MAIN_TEXT_SIZE : SUB_TEXT_SIZE;
            UiFontStyle style = main ? UiFontStyle.MEDIUM : UiFontStyle.REGULAR;
            float letterSpacing = size * style.letterSpacingEm();
            String display = formatLine(lines.get(i), entities.get(i));
            textWidth = Math.max(textWidth, font.measureWidth(display, size, letterSpacing));
        }

        float totalHeight = lines.size() * LINE_HEIGHT + 2.0F;
        float boxX = anchor.x() - textWidth / 2.0F - PANEL_PADDING;
        float boxY = anchor.y() - 2.0F;

        Render2DUtil.rect(boxX, boxY, textWidth + PANEL_PADDING * 2.0F, totalHeight)
                .color(PANEL_BG)
                .radius(PANEL_RADIUS)
                .draw();
        Render2DUtil.rect(boxX, boxY, textWidth + PANEL_PADDING * 2.0F, totalHeight)
                .color(0x00000000)
                .radius(PANEL_RADIUS)
                .border(1.0F, ColorUtil.withAlpha(highlight, BORDER_ALPHA))
                .draw();

        float lineY = boxY + 1.0F;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String display = formatLine(line, entities.get(i));
            boolean main = isMainLine(line);
            float size = main ? MAIN_TEXT_SIZE : SUB_TEXT_SIZE;
            UiFontStyle style = main ? UiFontStyle.MEDIUM : UiFontStyle.REGULAR;
            float letterSpacing = size * style.letterSpacingEm();
            float lineWidth = font.measureWidth(display, size, letterSpacing);
            float lineX = anchor.x() - lineWidth / 2.0F;
            int lineColor = main ? highlight : WHITE;

            Render2DUtil.text(lineX + 1.0F, lineY + 1.0F, size, display)
                    .style(style)
                    .color(TEXT_SHADOW)
                    .draw();
            Render2DUtil.text(lineX, lineY, size, display)
                    .style(style)
                    .color(lineColor)
                    .draw();
            lineY += LINE_HEIGHT;
        }
    }

    private boolean isMainLine(String line) {
        String lower = line.toLowerCase();
        for (String keyword : KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String formatLine(String line, Entity entity) {
        int totalSeconds = -1;
        String replaceSpan = null;
        int[] unit = parseCountdown(line);
        if (unit != null) {
            totalSeconds = unit[0];
            replaceSpan = line.substring(unit[1], unit[2]);
        } else {
            Matcher matcher = TIME_PATTERN.matcher(line);
            if (!matcher.find()) {
                return line;
            }
            if (matcher.group(3) != null) {
                totalSeconds = Integer.parseInt(matcher.group(1)) * 3600
                        + Integer.parseInt(matcher.group(2)) * 60
                        + Integer.parseInt(matcher.group(3));
            } else {
                totalSeconds = Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
            }
            replaceSpan = matcher.group(0);
        }

        String last = this.lastTexts.get(entity);
        if (last == null || !last.equals(line)) {
            this.lastTexts.put(entity, line);
            this.countdowns.put(entity, new Long[]{System.currentTimeMillis(), (long) totalSeconds});
        }
        Long[] stored = this.countdowns.get(entity);
        long capture = stored != null && stored[0] != null ? stored[0] : System.currentTimeMillis();
        long base = stored != null && stored[1] != null ? stored[1] : (long) totalSeconds;
        long remaining = base - (System.currentTimeMillis() - capture) / 1000L;
        if (remaining < 0L) {
            remaining = 0L;
        }
        return line.replace(replaceSpan, formatTime(remaining));
    }

    private int[] parseCountdown(String line) {
        int total = 0;
        boolean found = false;
        Matcher hours = HOURS_PATTERN.matcher(line);
        while (hours.find()) {
            total += Integer.parseInt(hours.group(1)) * 3600;
            found = true;
        }
        Matcher minutes = MINUTES_PATTERN.matcher(line);
        while (minutes.find()) {
            total += Integer.parseInt(minutes.group(1)) * 60;
            found = true;
        }
        Matcher seconds = SECONDS_PATTERN.matcher(line);
        while (seconds.find()) {
            total += Integer.parseInt(seconds.group(1));
            found = true;
        }
        if (!found) {
            return null;
        }

        Matcher broad = UNIT_PATTERN.matcher(line);
        int start = -1;
        int end = -1;
        while (broad.find()) {
            if (start < 0) {
                start = broad.start();
            }
            end = broad.end();
        }
        return new int[]{total, start, end};
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        return hours > 0L
                ? String.format("%d:%02d:%02d", hours, minutes, secs)
                : String.format("%02d:%02d", minutes, secs);
    }
}