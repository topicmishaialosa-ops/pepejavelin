package org.blade.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.blade.context.MinecraftContext;
import org.blade.event.EventTarget;
import org.blade.event.events.input.MouseInputEvent;
import org.blade.event.events.render.Render2DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.menu.core.MenuConfigStore;
import org.blade.menu.i18n.MenuText;
import org.blade.utils.ColorUtil;
import org.blade.utils.math.MathUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.Textures;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.TextAlign;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KeybindsFeature extends Feature {

    public static final String MODE_BLADE = "Blade";
    public static final String MODE_HUIHUI = "Huihui";
    public static final String MODE_NEXIS = "Nexis";
    public static final String MODE_SK3D = "Sk3d";
    public static final String MODE_MONOTON = "Monoton";

    private static final String POSITION_X_KEY = "feature.keybinds.x";
    private static final String POSITION_Y_KEY = "feature.keybinds.y";
    private static final float DEFAULT_X = 20.0F;
    private static final float DEFAULT_Y = 120.0F;
    private static final float BASE_TEXT_SIZE = 11.0F;
    private static final float BLADE_MAX_NAME_WIDTH = 64.0F;

    private static final class Row {
        private final String name;
        private final String bind;
        private final boolean enabled;
        private final String bracketed;
        private final String monoton;
        private final String text;

        private float measuredSize = Float.NaN;
        private float nameWidth;
        private float bindWidth;
        private float bracketedWidth;
        private float monotonWidth;
        private float textWidth;
        private String truncatedName;
        private float truncatedNameWidth;

        private Row(String name, String bind, boolean enabled) {
            this.name = name;
            this.bind = bind;
            this.enabled = enabled;
            this.bracketed = bracketed(bind);
            this.monoton = monotonBind(bind);
            this.text = name.toLowerCase(Locale.ROOT) + " [" + bind + "]";
        }

        private void measure(MsdfFont font, float textSize) {
            if (this.measuredSize == textSize) {
                return;
            }
            this.measuredSize = textSize;
            float spacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
            this.nameWidth = font.measureWidth(this.name, textSize, spacing);
            this.bindWidth = font.measureWidth(this.bind, textSize, spacing);
            this.bracketedWidth = font.measureWidth(this.bracketed, textSize, spacing);
            this.monotonWidth = font.measureWidth(this.monoton, textSize, spacing);
            this.textWidth = font.measureWidth(this.text, textSize, spacing);
            this.truncatedName = truncate(font, this.name, textSize, BLADE_MAX_NAME_WIDTH);
            this.truncatedNameWidth = font.measureWidth(this.truncatedName, textSize, spacing);
        }
    }

    public final ModeSetting mode = register(new ModeSetting(
            "Режим",
            MODE_HUIHUI,
            MODE_BLADE,
            MODE_HUIHUI,
            MODE_NEXIS,
            MODE_SK3D,
            MODE_MONOTON
    ));
    public final NumberSetting textSize = register(new NumberSetting(
            "Размер текста",
            BASE_TEXT_SIZE,
            8.0,
            16.0,
            0.5,
            ""
    ));
    public final NumberSetting transparency = register(new NumberSetting(
            "Прозрачность",
            0.6,
            0.0,
            1.0,
            0.05,
            ""
    ).visibleWhen(() -> !mode.is(MODE_BLADE)));

    private boolean dragging;
    private boolean positionInitialized;
    private float dragOffsetX;
    private float dragOffsetY;
    private float x = DEFAULT_X;
    private float y = DEFAULT_Y;
    private float width;
    private float height;
    private long rowsSignature = Long.MIN_VALUE;
    private List<Row> cachedRows = List.of();

    public KeybindsFeature() {
        super("Keybinds", "Shows the binds of enabled modules on your HUD.", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.player == null) {
            return;
        }

        float textSize = this.textSize.getValue().floatValue();
        ensurePositionLoaded();
        if (this.dragging) {
            this.x = mouseX(mc) - this.dragOffsetX;
            this.y = mouseY(mc) - this.dragOffsetY;
        }
        clampPosition(mc);

        List<Row> rows = collectRows(mc);
        if (rows.isEmpty() && !this.dragging) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }
        if (rows.isEmpty()) {
            rows = List.of(new Row("keybinds", "-", true));
        }

        if (mode.is(MODE_BLADE)) {
            renderBlade(rows, textSize);
        } else if (mode.is(MODE_HUIHUI)) {
            renderHuihui(rows, textSize);
        } else if (mode.is(MODE_NEXIS)) {
            renderNexis(rows, textSize);
        } else if (mode.is(MODE_MONOTON)) {
            renderMonoton(rows, textSize);
        } else {
            renderSk3d(rows, textSize);
        }
    }

    private void renderBlade(List<Row> rows, float textSize) {
        MsdfFont font = UiFonts.sfProDisplay();
        float rowHeight = textSize + 9.0F;
        float paddingX = 8.0F;
        float paddingY = 6.0F;
        float rowGap = 4.0F;
        float radius = 8.0F;
        float chipPaddingX = 8.0F;
        float chipMinWidth = 22.0F;

        float contentWidth = 112.0F;
        for (Row row : rows) {
            row.measure(font, textSize);
            float chipW = Math.max(chipMinWidth, row.bindWidth + chipPaddingX * 2.0F);
            float rowWidth = row.truncatedNameWidth + rowGap + chipW;
            contentWidth = Math.max(contentWidth, rowWidth);
        }

        float posX = this.x;
        float posY = this.y;
        this.width = contentWidth + paddingX * 2.0F;
        this.height = paddingY * 2.0F
                + rows.size() * rowHeight
                + Math.max(0.0F, rows.size() - 1) * rowGap;

        Render2DUtil.rect(posX, posY, this.width, this.height)
                .glass(1.0F, 0.5F, 8.0F)
                .radius(radius)
                .draw();

        float contentX = posX + paddingX;
        float contentRight = posX + this.width - paddingX;
        float cursorY = posY + paddingY;
        for (Row row : rows) {
            float rowCenterY = cursorY + rowHeight / 2.0F;
            Render2DUtil.text(contentX, font.centeredTextY(rowCenterY, textSize), textSize, row.truncatedName)
                    .style(UiFontStyle.MEDIUM)
                    .color(Theme.Colors.TEXT_TEXT)
                    .draw();

            float chipW = Math.max(chipMinWidth, row.bindWidth + chipPaddingX * 2.0F);
            float chipX = contentRight - chipW;
            Render2DUtil.rect(chipX, cursorY, chipW, rowHeight)
                    .color(row.enabled ? Theme.getAccent() : Theme.Colors.OUTLINES_MEDIUM)
                    .radius(4.0F)
                    .draw();
            Render2DUtil.text(chipX + chipW / 2.0F, font.centeredTextY(rowCenterY, textSize), textSize, row.bind)
                    .style(UiFontStyle.MEDIUM)
                    .color(row.enabled ? ColorUtil.WHITE : Theme.Colors.ICON)
                    .align(TextAlign.CENTER)
                    .draw();
            cursorY += rowHeight + rowGap;
        }
    }

    private void renderHuihui(List<Row> rows, float textSize) {
        MsdfFont font = UiFonts.sfProDisplay();
        float rowHeight = textSize + 5.0F;
        float paddingX = 10.0F;
        float paddingY = 8.0F;
        float radius = 8.0F;
        float headerSize = textSize + 1.0F;
        float headerGap = 6.0F;
        String heading = baseLabel();
        float headingSpacing = headerSize * UiFontStyle.MEDIUM.letterSpacingEm();

        float contentWidth = font.measureWidth(heading, headerSize, headingSpacing);
        for (Row row : rows) {
            row.measure(font, textSize);
            contentWidth = Math.max(contentWidth, row.textWidth);
        }

        float posX = this.x;
        float posY = this.y;
        this.width = contentWidth + paddingX * 2.0F;
        this.height = paddingY * 2.0F + headerSize + headerGap + rows.size() * rowHeight;

        float alpha = this.transparency.getValue().floatValue();
        Render2DUtil.rect(posX, posY, this.width, this.height)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.PANEL, alpha))
                .radius(radius)
                .border(1.0F, ColorUtil.multiplyAlpha(Theme.Colors.OUTLINES_MEDIUM, alpha))
                .draw();

        float textX = posX + paddingX;
        float cursorY = posY + paddingY;
        Render2DUtil.text(textX, cursorY, headerSize, heading)
                .style(UiFontStyle.MEDIUM)
                .color(Theme.getAccent())
                .draw();
        cursorY += headerSize + headerGap;

        for (Row row : rows) {
            Render2DUtil.text(textX, cursorY, textSize, row.text)
                    .style(UiFontStyle.MEDIUM)
                    .color(Theme.Colors.TEXT_TEXT)
                    .draw();
            cursorY += rowHeight;
        }
    }

    private void renderNexis(List<Row> rows, float textSize) {
        MsdfFont font = UiFonts.sfProDisplay();
        float scale = textSize / BASE_TEXT_SIZE;
        float leftPartWidth = 44.0F * scale;
        float itemHeight = 30.75F * scale;
        float rowSpacing = 2.5F * scale;
        float padding = 4.0F * scale;
        float paddingX = 9.4F * scale;
        float rounding = Math.max(3.0F, 11.0F * scale);
        float iconSize = 22.0F * scale;

        float targetWidth = 0.0F;
        for (Row row : rows) {
            row.measure(font, textSize);
            float rowWidth = row.nameWidth + row.bracketedWidth
                    + paddingX * 2.0F
                    + 34.0F * scale
                    + leftPartWidth;
            targetWidth = Math.max(targetWidth, rowWidth);
        }
        if (rows.isEmpty()) {
            targetWidth = Math.max(targetWidth, paddingX * 2.0F + leftPartWidth + 40.0F * scale);
        }

        float posX = this.x;
        float posY = this.y;
        this.width = Math.max(targetWidth, 1.0F);
        this.height = padding * 2.0F
                + rows.size() * itemHeight
                + Math.max(0.0F, rows.size() - 1) * rowSpacing;

        float alpha = this.transparency.getValue().floatValue();
        int bg = ColorUtil.rgba(0, 0, 0, (int) (165 * alpha));
        int leftBg = ColorUtil.rgba(0, 0, 0, (int) (105 * alpha));

        Render2DUtil.rect(posX, posY, this.width, this.height)
                .color(bg)
                .radius(rounding)
                .draw();
        Render2DUtil.rect(posX, posY, leftPartWidth, this.height)
                .color(leftBg)
                .radius(rounding, 0.0F, 0.0F, rounding)
                .draw();

        float lineX = posX + leftPartWidth - 0.5F;
        float lineHeight = this.height - 6.0F * scale;
        Render2DUtil.rect(lineX, posY + 3.0F * scale, 1.2F, lineHeight)
                .color(ColorUtil.multiplyAlpha(0xFFFFFFFF, alpha * 0.8F))
                .draw();

        Render2DUtil.texture(posX + leftPartWidth / 2.0F - iconSize / 2.0F,
                        posY + this.height / 2.0F - iconSize / 2.0F,
                        iconSize,
                        iconSize,
                        Textures.Icons.KEYBOARD)
                .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                .draw();

        float cursorY = posY + padding;
        float textX = posX + leftPartWidth + paddingX;
        for (Row row : rows) {
            row.measure(font, textSize);
            float rowCenterY = cursorY + itemHeight / 2.0F;
            Render2DUtil.text(textX, font.centeredTextY(rowCenterY, textSize), textSize, row.name)
                    .style(UiFontStyle.MEDIUM)
                    .color(Theme.Colors.TEXT_TEXT)
                    .draw();
            Render2DUtil.text(posX + this.width - paddingX - row.bracketedWidth,
                            font.centeredTextY(rowCenterY, textSize), textSize, row.bracketed)
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TEXT, 0.8F))
                    .draw();
            cursorY += itemHeight + rowSpacing;
        }

        Render2DUtil.rect(posX, posY, this.width, this.height)
                .color(0x00000000)
                .radius(rounding)
                .border(1.2F, ColorUtil.multiplyAlpha(0xFFFFFFFF, alpha * 0.2F))
                .draw();
    }

    private void renderSk3d(List<Row> rows, float textSize) {
        MsdfFont font = UiFonts.sfProDisplay();
        float scale = textSize / BASE_TEXT_SIZE;
        float titleHeight = 24.0F * scale;
        float playerHeight = 16.0F * scale;
        float rowGap = 2.0F * scale;
        float titleGap = 2.0F * scale;
        float inner = 4.5F * scale;
        float barXOffset = 2.8F * scale;
        float barWidth = 1.3F * scale;
        float barHeight = 7.0F * scale;
        float spaceAfterBar = 3.5F * scale;
        float gapBetweenPanels = 2.5F * scale;
        float panelRadius = 7.0F * scale;
        float innerRadius = 3.5F * scale;
        float titleSize = textSize + 1.0F;
        float titleSpacing = titleSize * UiFontStyle.MEDIUM.letterSpacingEm();

        float contentHeight = rows.size() * playerHeight + Math.max(0.0F, rows.size() - 1) * rowGap;
        float totalHeight = titleHeight + titleGap + contentHeight;

        String heading = baseLabel();
        float mainWidth = font.measureWidth(heading, titleSize, titleSpacing);
        float statusWidth = 0.0F;
        for (Row row : rows) {
            row.measure(font, textSize);
            mainWidth = Math.max(mainWidth, row.nameWidth);
            statusWidth = Math.max(statusWidth, row.bracketedWidth + 8.0F * scale);
        }

        float mainPanelWidth = Math.max(60.0F * scale,
                mainWidth + barXOffset + barWidth + spaceAfterBar + 6.0F * scale);
        float statusPanelWidth = Math.max(18.0F * scale, statusWidth);
        float panelWidth = inner * 2.0F + mainPanelWidth + gapBetweenPanels + statusPanelWidth;

        float posX = this.x;
        float posY = this.y;
        this.width = panelWidth;
        this.height = totalHeight;
        float alpha = this.transparency.getValue().floatValue();

        Render2DUtil.rect(posX, posY, panelWidth, totalHeight)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.PANEL, alpha))
                .radius(panelRadius)
                .draw();

        float titlePanelX = posX + inner;
        float titlePanelY = posY + inner;
        float titlePanelWidth = panelWidth - inner * 2.0F;
        float titlePanelHeight = titleHeight - inner * 2.0F;
        Render2DUtil.rect(titlePanelX, titlePanelY, titlePanelWidth, titlePanelHeight)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.CARD, alpha))
                .radius(innerRadius)
                .draw();

        float titleY = titlePanelY + (titlePanelHeight - titleSize) / 2.0F;
        Render2DUtil.text(posX + 10.0F, titleY, titleSize, heading)
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TITLE, alpha))
                .draw();

        float iconSize = titleSize + 2.0F;
        Render2DUtil.texture(posX + panelWidth - iconSize - 8.0F * scale, titleY + (titleSize - iconSize) / 2.0F,
                        iconSize, iconSize, Textures.Icons.KEYBOARD)
                .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                .draw();

        float contentY = posY + titleHeight + titleGap;
        Render2DUtil.rect(posX + inner, contentY, mainPanelWidth, contentHeight)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.CONTROL, alpha))
                .radius(innerRadius)
                .draw();
        float statusX = posX + inner + mainPanelWidth + gapBetweenPanels;
        Render2DUtil.rect(statusX, contentY, statusPanelWidth, contentHeight)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.CONTROL, alpha))
                .radius(innerRadius)
                .draw();

        int accent = Theme.getAccent();
        float cursorY = contentY;
        for (Row row : rows) {
            float rowCenterY = cursorY + playerHeight / 2.0F;
            float barX = posX + inner + barXOffset;
            Render2DUtil.rect(barX, rowCenterY - barHeight / 2.0F, barWidth, barHeight)
                    .color(accent)
                    .radius(0.9F)
                    .draw();

            float nameX = barX + barWidth + spaceAfterBar;
            Render2DUtil.text(nameX, font.centeredTextY(rowCenterY, textSize), textSize, row.name)
                    .style(UiFontStyle.MEDIUM)
                    .color(Theme.Colors.TEXT_TEXT)
                    .draw();

            Render2DUtil.text(statusX + (statusPanelWidth - row.bracketedWidth) / 2.0F,
                            font.centeredTextY(rowCenterY, textSize), textSize, row.bracketed)
                    .style(UiFontStyle.MEDIUM)
                    .color(accent)
                    .draw();

            cursorY += playerHeight + rowGap;
        }
    }

    private void renderMonoton(List<Row> rows, float textSize) {
        MsdfFont font = UiFonts.sfProDisplay();
        float scale = textSize / BASE_TEXT_SIZE;
        float headerHeight = 15.0F * scale;
        float padding = 5.0F * scale;
        float lineHeight = font.textHeight(textSize);
        float offset = lineHeight + 4.0F * scale;
        float headerSize = textSize + 1.0F;
        String heading = "Keybinds";

        float maxCombinedWidth = 0.0F;
        for (Row row : rows) {
            row.measure(font, textSize);
            float combinedWidth = row.nameWidth + row.monotonWidth + padding;
            maxCombinedWidth = Math.max(maxCombinedWidth, combinedWidth);
        }
        float panelWidth = Math.max(70.0F * scale, maxCombinedWidth + 7.9F * padding) + 1.0F;
        float bodyHeight = 4.0F * scale + rows.size() * offset;

        float posX = this.x;
        float posY = this.y;
        this.width = panelWidth;
        this.height = 2.5F * scale + headerHeight + bodyHeight;

        float alpha = this.transparency.getValue().floatValue();
        int headerColor = ColorUtil.multiplyAlpha(Theme.Colors.CARD, alpha);
        int bodyColor = ColorUtil.multiplyAlpha(Theme.Colors.PANEL, alpha * 0.65F);
        int textColor = Theme.Colors.TEXT_TEXT;
        int dotColor = ColorUtil.multiplyAlpha(Theme.Colors.ICON, 0.9F);
        int accent = Theme.getAccent();

        float headerTop = posY + 2.5F;
        float headerRadius = 4.0F * scale;
        Render2DUtil.rect(posX, headerTop, panelWidth, headerHeight)
                .color(headerColor)
                .radius(headerRadius, headerRadius, 0.0F, 0.0F)
                .draw();
        Render2DUtil.rect(posX, headerTop + headerHeight - 0.5F, panelWidth, bodyHeight + 0.5F)
                .color(bodyColor)
                .radius(0.0F, 0.0F, headerRadius, headerRadius)
                .draw();

        float iconSize = headerSize + 2.0F;
        Render2DUtil.texture(posX + 5.0F * scale,
                        headerTop + (headerHeight - iconSize) / 2.0F, iconSize, iconSize,
                        Textures.Icons.KEYBOARD)
                .color(accent)
                .draw();

        float headingX = posX + 5.0F * scale + iconSize + 3.0F * scale;
        Render2DUtil.text(headingX, headerTop + (headerHeight - headerSize) / 2.0F, headerSize, heading)
                .style(UiFontStyle.MEDIUM)
                .color(Theme.Colors.TEXT_TITLE)
                .draw();

        float dotsY = headerTop + (headerHeight - 2.0F * scale) / 2.0F;
        for (int i = 0; i < 3; i++) {
            Render2DUtil.rect(posX + panelWidth - 18.0F * scale + i * 4.0F * scale, dotsY,
                            2.0F * scale, 2.0F * scale)
                    .color(dotColor)
                    .radius(1.5F)
                    .draw();
        }

        float rowX = posX + padding - 2.0F * scale;
        float rowsTop = headerTop + headerHeight;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            float rowCenterY = rowsTop + 3.0F * scale + offset * i + offset / 2.0F;
            Render2DUtil.text(rowX, font.centeredTextY(rowCenterY, textSize), textSize, row.name)
                    .style(UiFontStyle.MEDIUM)
                    .color(textColor)
                    .draw();
            Render2DUtil.text(posX + panelWidth - padding - row.monotonWidth - 2.0F * scale,
                            font.centeredTextY(rowCenterY, textSize), textSize, row.monoton)
                    .style(UiFontStyle.MEDIUM)
                    .color(textColor)
                    .draw();
        }
    }

    private List<Row> collectRows(Minecraft mc) {
        boolean blade = mode.is(MODE_BLADE);
        boolean showcase = showcase(mc);
        long signature = 1125899906842597L;
        signature = signature * 31L + (blade ? 1 : 0);
        signature = signature * 31L + (showcase ? 1 : 0);
        for (Feature feature : FeatureManager.INSTANCE.getFeatures()) {
            signature = signature * 31L + feature.getName().hashCode();
            signature = signature * 31L + (feature.isEnabled() ? 1 : 0);
            List<Integer> bindCodes = feature.getBinds();
            signature = signature * 31L + bindCodes.size();
            for (int i = 0; i < bindCodes.size(); i++) {
                signature = signature * 31L + bindCodes.get(i);
                signature = signature * 31L + (feature.isBindVisibleAt(i) ? 1 : 0);
            }
        }
        if (signature == this.rowsSignature) {
            return this.cachedRows;
        }
        this.rowsSignature = signature;
        List<Row> rows = new ArrayList<>();
        for (Feature feature : FeatureManager.INSTANCE.getFeatures()) {
            if (!blade && !feature.isEnabled()) {
                continue;
            }
            StringBuilder binds = new StringBuilder();
            List<Integer> bindCodes = feature.getBinds();
            for (int i = 0; i < bindCodes.size(); i++) {
                int bindCode = bindCodes.get(i);
                if (bindCode != BindSetting.UNBOUND && feature.isBindVisibleAt(i)) {
                    if (binds.length() > 0) {
                        binds.append(" + ");
                    }
                    binds.append(BindSetting.describe(bindCode));
                }
            }
            if (binds.length() == 0) {
                continue;
            }
            rows.add(new Row(feature.getName(), binds.toString(), feature.isEnabled()));
        }
        if (rows.isEmpty() && showcase) {
            rows.add(new Row("Aura", "R", true));
            rows.add(new Row("Velocity", "V", false));
            rows.add(new Row("FullBright", "Z + X", false));
        }
        this.cachedRows = rows;
        return rows;
    }

    private static String bracketed(String bind) {
        return "[" + bind + "]";
    }

    private static String monotonBind(String bind) {
        String normalized = bind.toUpperCase(Locale.ROOT)
                .replace("MOUSE", "M")
                .replace("LEFT", "L")
                .replace("RIGHT", "R")
                .replace("CONTROL", "CTRL")
                .replace("_", "");
        return normalized.substring(0, Math.min(normalized.length(), 4));
    }

    private static String baseLabel() {
        return MenuText.option("Keybinds");
    }

    private static String truncate(MsdfFont font, String name, float textSize, float maxWidth) {
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        if (font.measureWidth(name, textSize, letterSpacing) <= maxWidth) {
            return name;
        }
        for (int length = name.length() - 1; length > 0; length--) {
            String truncated = name.substring(0, length) + "...";
            if (font.measureWidth(truncated, textSize, letterSpacing) <= maxWidth) {
                return truncated;
            }
        }
        return name.substring(0, 1) + "...";
    }

    @Override
    protected void onDisable() {
        if (this.dragging) {
            stopDrag();
        }
    }

    @EventTarget(priority = 100)
    public void onMouseInput(MouseInputEvent event) {
        Minecraft mc = MinecraftContext.mc;
        if (mc == null || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (this.dragging) {
                stopDrag();
                event.cancel();
            }
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS || this.width <= 0.0F || this.height <= 0.0F) {
            return;
        }
        if (!(mc.gui.screen() instanceof ChatScreen)) {
            return;
        }
        if (inside(mouseX(mc), mouseY(mc), this.x, this.y, this.width, this.height)) {
            this.dragging = true;
            this.dragOffsetX = mouseX(mc) - this.x;
            this.dragOffsetY = mouseY(mc) - this.y;
            event.cancel();
        }
    }

    private void ensurePositionLoaded() {
        if (this.positionInitialized) {
            return;
        }
        this.positionInitialized = true;
        this.x = MenuConfigStore.getFloat(POSITION_X_KEY, DEFAULT_X);
        this.y = MenuConfigStore.getFloat(POSITION_Y_KEY, DEFAULT_Y);
    }

    private void clampPosition(Minecraft mc) {
        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();
        this.x = MathUtil.clamp(this.x, 0.0F, Math.max(0.0F, screenWidth - this.width));
        this.y = MathUtil.clamp(this.y, 0.0F, Math.max(0.0F, screenHeight - this.height));
    }

    private void stopDrag() {
        this.dragging = false;
        MenuConfigStore.save(data -> {
            data.addProperty(POSITION_X_KEY, this.x);
            data.addProperty(POSITION_Y_KEY, this.y);
        });
    }

    private static boolean showcase(Minecraft mc) {
        return mc.gui.screen() instanceof ChatScreen || mc.gui.screen() instanceof InventoryScreen;
    }

    private static float mouseX(Minecraft mc) {
        return (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
    }

    private static float mouseY(Minecraft mc) {
        return (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
    }

    private static boolean inside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}