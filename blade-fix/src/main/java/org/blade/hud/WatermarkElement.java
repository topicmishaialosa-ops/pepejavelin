package org.blade.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import org.blade.feature.impl.misc.RenamePasterClientFeature;
import org.blade.feature.impl.visual.HudFeature;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.PlayerHead;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;
import org.blade.utils.render.Textures;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class WatermarkElement extends HudElement {

    private static final int LOGO_BACKGROUND = ColorUtil.rgb(37, 37, 39);
    private static final float MINI_SCALE_DIVISOR = 1.175F;
    private static final float LOGO_ASPECT = 19.3711F / 20.8607F;
    private static final float ICON_SIZE = 16.0F;
    private static final float ICON_TEXT_GAP = 8.0F;
    private static final float DIVIDER_GAP = 12.0F;
    private static final float TEXT_SIZE = 12.0F;

    private static final float PADDING = 5.0F;
    private static final float SEGMENT_SPACING = 2.0F;
    private static final float HTML_SPACING = 4.0F;
    private static final float SEGMENT_HEIGHT = 17.0F;
    private static final float SEGMENT_FONT_SIZE = 7.5F;
    private static final int SEG1_COLOR = ColorUtil.withAlpha(ColorUtil.rgb(47, 57, 56), 100);
    private static final int SEG1_BOTTOM_LEFT = ColorUtil.withAlpha(ColorUtil.rgb(47, 54, 57), 100);
    private static final int SEG2_TOP_LEFT = ColorUtil.withAlpha(ColorUtil.rgb(47, 57, 56), 100);
    private static final int SEG2_MID = ColorUtil.withAlpha(ColorUtil.rgb(17, 17, 17), 100);
    private static final int SEG2_BOTTOM_LEFT = ColorUtil.withAlpha(ColorUtil.rgb(47, 54, 57), 100);
    private static final int SEGMENT_ACCENT = ColorUtil.rgb(155, 200, 214);
    private static final int SEGMENT_TEXT = ColorUtil.rgb(255, 255, 255);

    private static final DateTimeFormatter BAR_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final float BAR_HEIGHT = 21.0F;
    private static final float BAR_RADIUS = 7.0F;
    private static final float BAR_PADDING = 9.0F;
    private static final float BAR_FONT_SIZE = 9.0F;
    private static final float BAR_ICON_SIZE = 9.5F;
    private static final float BAR_LOGO_SIZE = 14.5F;
    private static final float BAR_ICON_GAP = 4.5F;
    private static final float BAR_GAP = 8.0F;
    private static final float BAR_DIVIDER_GAP = 8.0F;
    private static final float BAR_UNIT_GAP = 3.0F;
    private static final int BAR_BG = ColorUtil.rgb(26, 26, 28);

    private record Style(
            float height,
            float radius,
            float padding,
            float logoSize,
            float logoIconSize,
            float headSize,
            float contentGap,
            float dividerHeight
    ) {
    }

    private static final Style MINI = new Style(32.0F, 8.0F, 8.0F, 32.0F, 16.0F, 18.0F, 8.0F, 14.0F);

    private record Section(Identifier icon, float iconSize, String text, float textWidth) {
    }

    private final List<Section> sections = new ArrayList<>(4);
    private final Style style = MINI;

    private String clientName;
    private String username;
    private String fpsText;
    private String msText;
    private float clientNameWidth;
    private float usernameWidth;
    private float fpsNumWidth;
    private float fpsLabelWidth;
    private float msNumWidth;
    private float msLabelWidth;

    private float miniUnit = Float.NaN;
    private String miniClientName;
    private String miniServer;
    private int miniFps = Integer.MIN_VALUE;
    private long miniPing = Long.MIN_VALUE;

    private float segmentUnit = Float.NaN;
    private int segmentFps = Integer.MIN_VALUE;
    private long segmentPing = Long.MIN_VALUE;

    private float barUnit = Float.NaN;
    private int barFps = Integer.MIN_VALUE;
    private long barPing = Long.MIN_VALUE;
    private String barClient;
    private String barName;
    private String barTime;
    private float barClientWidth;
    private float barNameWidth;
    private float barFpsNumWidth;
    private float barFpsUnitWidth;
    private float barPingNumWidth;
    private float barPingUnitWidth;
    private float barTimeWidth;

    public WatermarkElement() {
        super("watermark", "Watermark");
    }

    @Override
    protected boolean preservePositionOnContentResize() {
        return true;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        if (HudFeature.barWatermarkEnabled()) {
            layoutBar(mc, unit);
        } else if (HudFeature.newWatermarkEnabled()) {
            layoutSegment(mc, unit);
        } else {
            layoutMini(mc, unit);
        }
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        if (HudFeature.barWatermarkEnabled()) {
            drawBar(mc, unit);
        } else if (HudFeature.newWatermarkEnabled()) {
            drawSegment(mc, unit);
        } else {
            drawMini(mc, unit);
        }
    }

    private void layoutMini(Minecraft mc, float unit) {
        unit = scaledUnit(unit);
        String client = RenamePasterClientFeature.getClientName();
        String server = serverAddress(mc);
        int fps = mc.getFps();
        long ping = latency(mc);
        if (unit == this.miniUnit && fps == this.miniFps && ping == this.miniPing
                && client.equals(this.miniClientName) && server.equals(this.miniServer)
                && !this.sections.isEmpty()) {
            this.height = this.style.height() * unit;
            return;
        }
        this.miniUnit = unit;
        this.miniClientName = client;
        this.miniServer = server;
        this.miniFps = fps;
        this.miniPing = ping;

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();

        this.sections.clear();
        this.sections.add(new Section(null, this.style.headSize(), client,
                font.measureWidth(client, textSize, letterSpacing)));
        String serverText = server;
        this.sections.add(new Section(Textures.Icons.GAMEPAD, ICON_SIZE, serverText,
                font.measureWidth(serverText, textSize, letterSpacing)));
        String fpsSegment = Integer.toString(fps) + " FPS";
        this.sections.add(new Section(Textures.Icons.HARD_DRIVE, ICON_SIZE, fpsSegment,
                font.measureWidth(fpsSegment, textSize, letterSpacing)));
        String pingSegment = Long.toString(ping) + "ms";
        this.sections.add(new Section(Textures.Icons.CHEVRONS_LEFT_RIGHT_ELLIPSIS, ICON_SIZE, pingSegment,
                font.measureWidth(pingSegment, textSize, letterSpacing)));

        float width = (this.style.logoSize() + this.style.contentGap()) * unit;
        for (int i = 0; i < this.sections.size(); i++) {
            if (i > 0) {
                width += (DIVIDER_GAP * 2.0F + 1.0F) * unit;
            }
            Section section = this.sections.get(i);
            width += (section.iconSize() + ICON_TEXT_GAP) * unit;
            width += section.textWidth();
        }
        this.width = width + this.style.padding() * unit;
        this.height = this.style.height() * unit;
    }

    private void drawMini(Minecraft mc, float unit) {
        unit = scaledUnit(unit);
        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        drawLogo(unit, alpha);

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float centerY = this.y + this.height / 2.0F;
        float cursor = this.x + (this.style.logoSize() + this.style.contentGap()) * unit;

        for (int i = 0; i < this.sections.size(); i++) {
            if (i > 0) {
                cursor = divider(cursor, centerY, this.style.dividerHeight(), DIVIDER_GAP, unit, alpha);
            }

            Section section = this.sections.get(i);
            float iconSize = section.iconSize() * unit;
            float iconY = centerY - iconSize / 2.0F;
            if (section.icon() == null) {
                drawPlayerHead(mc, cursor, iconY, iconSize, alpha);
            } else {
                Render2DUtil.texture(cursor, iconY, iconSize, iconSize, section.icon())
                        .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_GHOST, alpha))
                        .draw();
            }
            cursor += iconSize + ICON_TEXT_GAP * unit;

            Render2DUtil.text(cursor, font.centeredTextY(centerY, textSize), textSize, section.text())
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.PRIMARY_BRIGHT, alpha))
                    .draw();
            cursor += section.textWidth();
        }
    }

    private void drawLogo(float unit, float alpha) {
        float size = this.style.logoSize() * unit;
        Render2DUtil.rect(this.x, this.y, size, size)
                .color(ColorUtil.multiplyAlpha(LOGO_BACKGROUND, alpha))
                .radius(this.style.radius() * unit)
                .draw();
        float iconHeight = this.style.logoIconSize() * unit;
        float iconWidth = iconHeight * LOGO_ASPECT;
        float iconX = this.x + (size - iconWidth) / 2.0F;
        float iconY = this.y + (size - iconHeight) / 2.0F;
        Render2DUtil.texture(iconX, iconY, iconWidth, iconHeight, Textures.Logos.BOLT)
                .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha))
                .draw();
    }

    private void drawPlayerHead(Minecraft mc, float x, float y, float size, float alpha) {
        if (mc.player == null) {
            return;
        }
        PlayerHead.draw(x, y, size, mc.player.getSkin().body().texturePath(),
                size * 0.2F, ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha));
    }

    private void layoutSegment(Minecraft mc, float unit) {
        float scale = unit / 1.175F;
        int fps = mc.getFps();
        long ping = latency(mc);
        String client = RenamePasterClientFeature.getClientName();
        String name = mc.getUser() != null && mc.getUser().getName() != null
                ? mc.getUser().getName()
                : "Player";
        if (scale == this.segmentUnit && fps == this.segmentFps && ping == this.segmentPing
                && client.equals(this.clientName) && name.equals(this.username)) {
            this.width = segmentWidth(scale);
            this.height = SEGMENT_HEIGHT * scale;
            return;
        }
        this.segmentUnit = scale;
        this.segmentFps = fps;
        this.segmentPing = ping;
        this.clientName = client;
        this.username = name;
        this.fpsText = Integer.toString(fps);
        this.msText = Long.toString(ping);

        float textSize = SEGMENT_FONT_SIZE * scale;
        MsdfFont font = UiFonts.sfProDisplay();
        float spacing = textSize * UiFontStyle.SEMIBOLD.letterSpacingEm();

        this.clientNameWidth = font.measureWidth(this.clientName, textSize, spacing);
        this.usernameWidth = font.measureWidth(this.username, textSize, spacing);
        this.fpsNumWidth = font.measureWidth(this.fpsText, textSize, spacing);
        this.fpsLabelWidth = font.measureWidth("FPS", textSize, spacing);
        this.msNumWidth = font.measureWidth(this.msText, textSize, spacing);
        this.msLabelWidth = font.measureWidth("MS", textSize, spacing);

        this.width = segmentWidth(scale);
        this.height = SEGMENT_HEIGHT * scale;
    }

    private float segmentWidth(float scale) {
        float blumeWidth = this.clientNameWidth + PADDING * 2.0F * scale;
        float contentWidth = this.usernameWidth + HTML_SPACING * scale
                + this.fpsNumWidth + this.fpsLabelWidth + HTML_SPACING * scale
                + this.msNumWidth + this.msLabelWidth;
        float mainBlockWidth = contentWidth + PADDING * 2.0F * scale;
        return blumeWidth + SEGMENT_SPACING * scale + mainBlockWidth;
    }

    private void drawSegment(Minecraft mc, float unit) {
        float alpha = appearAlpha();
        if (alpha <= 0.0F) {
            return;
        }

        float scale = unit / 1.175F;
        float x = this.x;
        float y = this.y;
        float height = this.height;
        float textSize = SEGMENT_FONT_SIZE * scale;
        MsdfFont font = UiFonts.sfProDisplay();

        float blumeWidth = this.clientNameWidth + PADDING * 2.0F * scale;
        float contentWidth = this.usernameWidth + HTML_SPACING * scale
                + this.fpsNumWidth + this.fpsLabelWidth + HTML_SPACING * scale
                + this.msNumWidth + this.msLabelWidth;
        float mainBlockWidth = contentWidth + PADDING * 2.0F * scale;

        Render2DUtil.rect(x, y, blumeWidth, height)
                .color(SEG1_COLOR, SEG1_COLOR, SEG1_COLOR, SEG1_BOTTOM_LEFT)
                .radius(5.0F * scale, 2.0F * scale, 2.0F * scale, 5.0F * scale)
                .draw();

        float centerY = y + height / 2.0F;
        Render2DUtil.text(x + PADDING * scale, font.centeredTextY(centerY, textSize), textSize, this.clientName)
                .font(UiFonts.sfProDisplay())
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.multiplyAlpha(SEGMENT_ACCENT, alpha))
                .draw();
        x += blumeWidth + SEGMENT_SPACING * scale;

        Render2DUtil.rect(x, y, mainBlockWidth, height)
                .color(SEG2_TOP_LEFT, SEG2_MID, SEG2_MID, SEG2_BOTTOM_LEFT)
                .radius(2.0F * scale, 5.0F * scale, 5.0F * scale, 2.0F * scale)
                .draw();

        float innerX = x + PADDING * scale;
        innerX = drawSegmentPart(font, innerX, centerY, textSize, alpha, this.username, SEGMENT_TEXT,
                this.usernameWidth, HTML_SPACING * scale);
        innerX = drawSegmentPart(font, innerX, centerY, textSize, alpha, this.fpsText, SEGMENT_TEXT,
                this.fpsNumWidth, 0.0F);
        innerX = drawSegmentPart(font, innerX, centerY, textSize, alpha, "FPS", SEGMENT_ACCENT,
                this.fpsLabelWidth, HTML_SPACING * scale);
        innerX = drawSegmentPart(font, innerX, centerY, textSize, alpha, this.msText, SEGMENT_TEXT,
                this.msNumWidth, 0.0F);
        drawSegmentPart(font, innerX, centerY, textSize, alpha, "MS", SEGMENT_ACCENT, this.msLabelWidth, 0.0F);
    }

    private float drawSegmentPart(MsdfFont font, float x, float centerY, float textSize, float alpha,
                                  String text, int color, float textWidth, float advance) {
        Render2DUtil.text(x, font.centeredTextY(centerY, textSize), textSize, text)
                .font(UiFonts.sfProDisplay())
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.multiplyAlpha(color, alpha))
                .draw();
        return x + textWidth + advance;
    }

    private void layoutBar(Minecraft mc, float unit) {
        float scale = unit / MINI_SCALE_DIVISOR;
        int fps = mc.getFps();
        long ping = latency(mc);
        String client = RenamePasterClientFeature.getClientName();
        String name = mc.getUser() != null && mc.getUser().getName() != null
                ? mc.getUser().getName()
                : "Player";
        String time = LocalTime.now().format(BAR_TIME_FORMAT);
        if (scale == this.barUnit && fps == this.barFps && ping == this.barPing
                && client.equals(this.barClient) && name.equals(this.barName)
                && time.equals(this.barTime)) {
            this.width = barWidth(scale);
            this.height = BAR_HEIGHT * scale;
            return;
        }
        this.barUnit = scale;
        this.barFps = fps;
        this.barPing = ping;
        this.barClient = client;
        this.barName = name;
        this.barTime = time;

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = BAR_FONT_SIZE * scale;
        float spacing = textSize * UiFontStyle.SEMIBOLD.letterSpacingEm();

        this.barClientWidth = font.measureWidth(client, textSize, spacing);
        this.barNameWidth = font.measureWidth(name, textSize, spacing);
        this.barFpsNumWidth = font.measureWidth(Integer.toString(fps), textSize, spacing);
        this.barFpsUnitWidth = font.measureWidth("fps", textSize, spacing);
        this.barPingNumWidth = font.measureWidth(Long.toString(ping), textSize, spacing);
        this.barPingUnitWidth = font.measureWidth("ms", textSize, spacing);
        this.barTimeWidth = font.measureWidth(time, textSize, spacing);

        this.width = barWidth(scale);
        this.height = BAR_HEIGHT * scale;
    }

    private float barWidth(float scale) {
        float icon = BAR_ICON_SIZE * scale;
        float gap = BAR_ICON_GAP * scale;
        float w = BAR_PADDING * scale;
        w += BAR_LOGO_SIZE * scale + gap + this.barClientWidth;
        w += BAR_DIVIDER_GAP * scale + scale + BAR_DIVIDER_GAP * scale;
        w += icon + gap + this.barNameWidth;
        w += BAR_GAP * scale;
        w += this.barFpsNumWidth + BAR_UNIT_GAP * scale + this.barFpsUnitWidth;
        w += BAR_GAP * scale;
        w += icon + gap + this.barPingNumWidth + BAR_UNIT_GAP * scale + this.barPingUnitWidth;
        w += BAR_GAP * scale;
        w += icon + gap + this.barTimeWidth;
        w += BAR_PADDING * scale;
        return w;
    }

    private void drawBar(Minecraft mc, float unit) {
        float alpha = appearAlpha();
        if (alpha <= 0.0F) {
            return;
        }

        float scale = unit / MINI_SCALE_DIVISOR;
        float x = this.x;
        float y = this.y;
        float centerY = y + this.height / 2.0F;
        float textSize = BAR_FONT_SIZE * scale;
        float iconSize = BAR_ICON_SIZE * scale;

        Render2DUtil.rect(x, y, this.width, this.height)
                .color(ColorUtil.multiplyAlpha(BAR_BG, alpha))
                .radius(BAR_RADIUS * scale)
                .draw();

        float cursor = x + BAR_PADDING * scale;

        float logo = BAR_LOGO_SIZE * scale;
        Render2DUtil.rect(cursor, centerY - logo / 2.0F, logo, logo)
                .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                .radius(logo / 2.0F)
                .draw();
        float boltHeight = logo * 0.62F;
        float boltWidth = boltHeight * LOGO_ASPECT;
        Render2DUtil.texture(cursor + (logo - boltWidth) / 2.0F, centerY - boltHeight / 2.0F, boltWidth, boltHeight, Textures.Logos.BOLT)
                .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha))
                .draw();
        cursor += logo + BAR_ICON_GAP * scale;

        cursor = barText(cursor, centerY, textSize, this.barClient, this.barClientWidth, Theme.Colors.PRIMARY_BRIGHT, alpha);
        cursor = divider(cursor, centerY, 11.0F, BAR_DIVIDER_GAP, scale, alpha);

        Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.USER_ROUND)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha))
                .draw();
        cursor += iconSize + BAR_ICON_GAP * scale;
        cursor = barText(cursor, centerY, textSize, this.barName, this.barNameWidth, Theme.Colors.PRIMARY_BRIGHT, alpha);

        cursor += BAR_GAP * scale;
        cursor = barText(cursor, centerY, textSize, Integer.toString(this.barFps), this.barFpsNumWidth, Theme.Colors.PRIMARY_BRIGHT, alpha);
        cursor += BAR_UNIT_GAP * scale;
        cursor = barText(cursor, centerY, textSize, "fps", this.barFpsUnitWidth, Theme.Colors.SECONDARY, alpha);

        cursor += BAR_GAP * scale;
        drawSignalIcon(cursor, centerY, iconSize, ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha));
        cursor += signalIconWidth(iconSize) + BAR_ICON_GAP * scale;
        cursor = barText(cursor, centerY, textSize, Long.toString(this.barPing), this.barPingNumWidth, Theme.Colors.PRIMARY_BRIGHT, alpha);
        cursor += BAR_UNIT_GAP * scale;
        cursor = barText(cursor, centerY, textSize, "ms", this.barPingUnitWidth, Theme.Colors.SECONDARY, alpha);

        cursor += BAR_GAP * scale;
        drawClockIcon(cursor, centerY, iconSize, ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha));
        cursor += iconSize + BAR_ICON_GAP * scale;
        barText(cursor, centerY, textSize, this.barTime, this.barTimeWidth, Theme.Colors.PRIMARY_BRIGHT, alpha);
    }

    private float barText(float x, float centerY, float textSize, String text, float textWidth, int color, float alpha) {
        Render2DUtil.text(x, UiFonts.sfProDisplay().centeredTextY(centerY, textSize), textSize, text)
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.multiplyAlpha(color, alpha))
                .draw();
        return x + textWidth;
    }

    private static float signalIconWidth(float size) {
        return signalBarWidth(size) * 4.0F + signalBarGap(size) * 3.0F;
    }

    private static float signalBarWidth(float size) {
        return Math.max(1.0F, size * 0.16F);
    }

    private static float signalBarGap(float size) {
        return Math.max(0.5F, size * 0.09F);
    }

    private void drawSignalIcon(float x, float centerY, float size, int color) {
        float barW = signalBarWidth(size);
        float gap = signalBarGap(size);
        float bottom = centerY + size / 2.0F;
        float cursor = x;
        for (int i = 0; i < 4; i++) {
            float barHeight = size * (0.35F + 0.2167F * i);
            Render2DUtil.rect(cursor, bottom - barHeight, barW, barHeight)
                    .color(color)
                    .radius(Math.min(barW, barHeight) * 0.4F)
                    .draw();
            cursor += barW + gap;
        }
    }

    private void drawClockIcon(float x, float centerY, float size, int color) {
        float half = size / 2.0F;
        float thickness = Math.max(0.75F, size * 0.11F);
        Render2DUtil.rect(x, centerY - half, size, size)
                .color(ColorUtil.TRANSPARENT)
                .radius(half)
                .border(thickness, color)
                .draw();
        float cx = x + half;
        Render2DUtil.rect(cx - thickness * 0.5F, centerY - size * 0.27F, thickness, size * 0.27F + thickness * 0.5F)
                .color(color)
                .radius(thickness * 0.4F)
                .draw();
        Render2DUtil.rect(cx, centerY - thickness * 0.5F, size * 0.26F, thickness)
                .color(color)
                .radius(thickness * 0.4F)
                .draw();
    }

    private static String serverAddress(Minecraft mc) {
        return mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Singleplayer";
    }

    private static long latency(Minecraft mc) {
        if (mc.getCurrentServer() == null) {
            return 0L;
        }
        if (mc.getConnection() == null || mc.player == null) {
            return mc.getCurrentServer().ping;
        }
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info != null ? info.getLatency() : mc.getCurrentServer().ping;
    }

    private float scaledUnit(float unit) {
        return unit / MINI_SCALE_DIVISOR;
    }
}