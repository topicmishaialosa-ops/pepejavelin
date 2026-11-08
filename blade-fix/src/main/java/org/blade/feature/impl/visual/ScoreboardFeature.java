package org.blade.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.blade.context.MinecraftContext;
import org.blade.context.RenderContext;
import org.blade.event.EventTarget;
import org.blade.event.events.input.MouseInputEvent;
import org.blade.event.events.render.FinalGuiRenderEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ScoreboardFeature extends Feature {
    private static final float TITLE_SIZE = 8.8F;
    private static final float LINE_SIZE = 7.8F;
    private static final float PAD_X = 10.0F;
    private static final float PAD_Y = 6.0F;
    private static final float LINE_GAP = 5.0F;
    private static final float VALUE_GAP = 16.0F;
    private static final int MAX_LINES = 25;

    private static final int[] LEGACY_COLORS = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
            0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
            0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };

    public final NumberSetting scale = register(new NumberSetting("Масштаб", 1.0, 0.5, 2.0, 0.05, ""));
    public final NumberSetting posX = register(new NumberSetting("Позиция X", 4.0, 0.0, 1920.0, 1.0, ""));
    public final NumberSetting posY = register(new NumberSetting("Позиция Y", 4.0, 0.0, 1080.0, 1.0, ""));
    public final BooleanSetting showBackground = register(new BooleanSetting("Фон", true));
    public final BooleanSetting showShadow = register(new BooleanSetting("Тень", true));
    public final BooleanSetting showAccentLine = register(new BooleanSetting("Линия акцента", true));

    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;

    public ScoreboardFeature() {
        super("Scoreboard", "Кастомный скорборд с поддержкой цветов", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onFinalGuiRender(FinalGuiRenderEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.player == null || mc.level == null) {
            return;
        }
        Screen screen = mc.gui.screen();
        if (screen != null && !(screen instanceof ChatScreen)) {
            return;
        }
        RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
        try {
            Render2DUtil.beginFrame();
            if (dragging) {
                updatePosition(mc);
            }
            renderScoreboard(mc);
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        Minecraft mc = MinecraftContext.mc;
        if (mc == null || mc.player == null) {
            return;
        }
        Screen screen = mc.gui.screen();
        if (!(screen instanceof ChatScreen)) {
            dragging = false;
            return;
        }
        float mouseX = mouseX(mc);
        float mouseY = mouseY(mc);
        float[] size = currentSize(mc);
        float sx = this.posX.getValue().floatValue();
        float sy = this.posY.getValue().floatValue();
        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mouseX >= sx && mouseX <= sx + size[0] && mouseY >= sy && mouseY <= sy + size[1]) {
                dragging = true;
                dragOffsetX = mouseX - sx;
                dragOffsetY = mouseY - sy;
                event.cancel();
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            dragging = false;
        }
    }

    private void renderScoreboard(Minecraft mc) {
        if (mc.level == null) return;
        Objective objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return;

        float scale = this.scale.getValue().floatValue();
        Layout layout = buildLayout(objective, scale);

        float baseX = this.posX.getValue().floatValue();
        float baseY = this.posY.getValue().floatValue();

        if (showShadow.getValue()) {
            Render2DUtil.rect(baseX - 5.0F, baseY - 5.0F, layout.width + 10.0F, layout.height + 10.0F)
                    .radius(6.0F)
                    .shadow(ColorUtil.rgba(0, 0, 0, 64), 15.0F)
                    .draw();
        }

        if (showBackground.getValue()) {
            int border = ColorUtil.withAlpha(ColorUtil.multiplyRgb(Theme.Colors.SECONDARY, 0.5F), 150);
            Render2DUtil.rect(baseX, baseY, layout.width, layout.height)
                    .radius(6.0F)
                    .color(ColorUtil.rgba(0, 0, 0, 150))
                    .border(1.0F, border)
                    .draw();
        }

        if (showAccentLine.getValue()) {
            float lineH = Math.max(1.0F, 1.4F * scale);
            int accent = ColorUtil.withAlpha(Theme.getAccent(), 170);
            int secondary = ColorUtil.withAlpha(Theme.Colors.SECONDARY, 50);
            Render2DUtil.rect(baseX + layout.padX * 0.6F,
                            baseY + layout.headerHeight - lineH - layout.padY * 0.3F,
                            layout.width - layout.padX * 1.2F, lineH)
                    .radius(lineH / 2.0F)
                    .color(accent, accent, secondary, secondary)
                    .draw();
        }

        float titleX = baseX + PAD_X;
        for (Segment segment : layout.title.segments) {
            Render2DUtil.text(titleX, baseY + PAD_Y, TITLE_SIZE * scale, segment.text)
                    .color(segment.color)
                    .weight(UiFontStyle.MEDIUM.weight())
                    .draw();
            titleX += layout.titleFont.measureWidth(segment.text, TITLE_SIZE * scale, 0.0F);
        }

        float lineY = baseY + layout.headerHeight;
        for (EntryLine line : layout.lines) {
            float rowY = lineY + (layout.lineHeight - layout.lineFont.textHeight(LINE_SIZE * scale)) / 2.0F;

            float nameX = baseX + PAD_X;
            for (Segment segment : line.name.segments) {
                Render2DUtil.text(nameX, rowY, LINE_SIZE * scale, segment.text)
                        .color(segment.color)
                        .draw();
                nameX += layout.lineFont.measureWidth(segment.text, LINE_SIZE * scale, 0.0F);
            }

            float valueX = baseX + layout.width - PAD_X - line.value.width;
            for (Segment segment : line.value.segments) {
                Render2DUtil.text(valueX, rowY, LINE_SIZE * scale, segment.text)
                        .color(segment.color)
                        .draw();
                valueX += layout.lineFont.measureWidth(segment.text, LINE_SIZE * scale, 0.0F);
            }

            lineY += layout.lineHeight;
        }
    }

    private Layout buildLayout(Objective objective, float scale) {
        int defaultTextColor = Theme.Colors.SECONDARY & 0xFFFFFF;
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.NO_STYLE);

        MsdfFont titleFont = UiFonts.getFontForText(objective.getDisplayName().getString(), UiFontStyle.MEDIUM.weight());
        Segments title = buildSegments(objective.getDisplayName(), defaultTextColor, TITLE_SIZE * scale, titleFont);
        float maxWidth = title.width;

        MsdfFont lineFont = UiFonts.getFontForText("Aa", UiFontStyle.REGULAR.weight());
        float lineHeight = lineFont.textHeight(LINE_SIZE * scale) + LINE_GAP;

        List<EntryLine> lines = scoreboard.listPlayerScores(objective).stream()
                .filter(e -> !e.isHidden())
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
                        .thenComparing(PlayerScoreEntry::owner, String::compareToIgnoreCase))
                .limit(MAX_LINES)
                .map(entry -> {
                    Component nameSource = resolveNameText(scoreboard, entry);
                    Segments name = buildSegments(nameSource, defaultTextColor, LINE_SIZE * scale, lineFont);
                    Component valueSource = entry.formatValue(numberFormat);
                    Segments value = buildSegments(valueSource, defaultTextColor, LINE_SIZE * scale, lineFont);
                    return new EntryLine(name, value);
                })
                .toList();

        for (EntryLine line : lines) {
            float rowWidth = line.name.width + line.value.width + VALUE_GAP;
            maxWidth = Math.max(maxWidth, rowWidth);
        }

        float headerHeight = titleFont.textHeight(TITLE_SIZE * scale) + PAD_Y * 2.0F;
        float totalHeight = headerHeight + lines.size() * lineHeight + PAD_Y;
        float totalWidth = maxWidth + PAD_X * 2.0F + 12.0F;

        return new Layout(title, titleFont, lines, lineFont, headerHeight, lineHeight, totalWidth, totalHeight, scale, PAD_X, PAD_Y);
    }

    private Segments buildSegments(Component component, int defaultColor, float size, MsdfFont font) {
        List<Segment> raw = new ArrayList<>();
        component.visit((Style style, String part) -> {
            int color = colorFromStyle(style, defaultColor);
            collectLegacy(part, color, defaultColor, raw);
            return Optional.empty();
        }, Style.EMPTY);

        List<Segment> merged = new ArrayList<>();
        for (Segment s : raw) {
            if (s.text.isEmpty()) continue;
            if (!merged.isEmpty() && merged.get(merged.size() - 1).color == s.color) {
                Segment prev = merged.get(merged.size() - 1);
                merged.set(merged.size() - 1, new Segment(prev.text + s.text, s.color));
            } else {
                merged.add(s);
            }
        }

        float width = 0.0F;
        for (Segment s : merged) {
            width += font.measureWidth(s.text, size, 0.0F);
        }
        return new Segments(merged, width);
    }

    private Component resolveNameText(Scoreboard scoreboard, PlayerScoreEntry entry) {
        Component display = entry.display();
        if (display != null) return display;
        String owner = entry.owner();
        if (owner == null) return Component.empty();
        PlayerTeam team = scoreboard.getPlayersTeam(owner);
        Component base = Component.literal(owner);
        return team != null ? PlayerTeam.formatNameForTeam(team, base) : base;
    }

    private void collectLegacy(String text, int color, int baseColor, List<Segment> out) {
        int idx = text.indexOf('\u00A7');
        if (idx < 0) {
            if (!text.isEmpty()) out.add(new Segment(text, color));
            return;
        }
        int start = 0;
        int currentColor = color;
        for (int i = idx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                if (i > start) {
                    out.add(new Segment(text.substring(start, i), currentColor));
                }
                char code = text.charAt(++i);
                if (code == 'x' && i + 6 < text.length()) {
                    currentColor = parseHexColor(text, i);
                    i += 6;
                } else if (code == 'r') {
                    currentColor = baseColor;
                } else {
                    int legacyIdx = legacyIndex(code);
                    if (legacyIdx >= 0) currentColor = LEGACY_COLORS[legacyIdx];
                }
                start = i + 1;
            }
        }
        if (start < text.length()) {
            out.add(new Segment(text.substring(start), currentColor));
        }
    }

    private static int parseHexColor(String text, int pos) {
        if (pos + 6 >= text.length()) return 0xFFFFFFFF;
        StringBuilder hex = new StringBuilder();
        for (int j = pos + 1; j <= pos + 6; j++) {
            char ch = text.charAt(j);
            if (ch == '\u00A7' && j + 1 <= pos + 6) {
                j++;
                hex.append(text.charAt(j));
            } else {
                hex.append(ch);
            }
        }
        try {
            return 0xFF000000 | Integer.parseInt(hex.toString(), 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    private static int legacyIndex(char code) {
        if (code >= '0' && code <= '9') return code - '0';
        if (code >= 'a' && code <= 'f') return code - 'a' + 10;
        if (code >= 'A' && code <= 'F') return code - 'A' + 10;
        return -1;
    }

    private static int colorFromStyle(Style style, int defaultColor) {
        TextColor textColor = style.getColor();
        return textColor != null ? (0xFF000000 | textColor.getValue()) : defaultColor;
    }

    private static float mouseX(Minecraft mc) {
        return (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
    }

    private static float mouseY(Minecraft mc) {
        return (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
    }

    private float[] currentSize(Minecraft mc) {
        if (mc.level == null) return new float[]{60.0F, 20.0F};
        Objective objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return new float[]{60.0F, 20.0F};
        Layout layout = buildLayout(objective, this.scale.getValue().floatValue());
        return new float[]{layout.width, layout.height};
    }

    private void updatePosition(Minecraft mc) {
        if (!dragging) return;
        float mx = mouseX(mc);
        float my = mouseY(mc);
        float[] size = currentSize(mc);
        float scaledWidth = (float) mc.getWindow().getGuiScaledWidth();
        float scaledHeight = (float) mc.getWindow().getGuiScaledHeight();
        this.posX.setValue((double) Math.min(Math.max(0.0F, mx - dragOffsetX), Math.max(scaledWidth - size[0], 0.0F)));
        this.posY.setValue((double) Math.min(Math.max(0.0F, my - dragOffsetY), Math.max(scaledHeight - size[1], 0.0F)));
    }

    private record Segment(String text, int color) {}

    private record Segments(List<Segment> segments, float width) {}

    private record EntryLine(Segments name, Segments value) {}

    private record Layout(Segments title, MsdfFont titleFont, List<EntryLine> lines, MsdfFont lineFont,
                          float headerHeight, float lineHeight, float width, float height,
                          float scale, float padX, float padY) {}
}
