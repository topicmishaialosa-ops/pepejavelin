package org.blade.feature.impl.visual.targethud;

import net.minecraft.client.Minecraft;
import org.blade.context.MinecraftContext;
import org.blade.context.RenderContext;
import org.blade.event.EventManager;
import org.blade.event.EventTarget;
import org.blade.event.events.input.CharacterInputEvent;
import org.blade.event.events.input.KeyboardInputEvent;
import org.blade.event.events.input.MouseInputEvent;
import org.blade.event.events.input.ScrollInputEvent;
import org.blade.event.events.render.FinalGuiRenderEvent;
import org.blade.feature.setting.BooleanSetting;
import org.blade.menu.core.MenuOverlay;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.setting.TextSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.math.MathUtil;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.TextAlign;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class TargetHudEditorOverlay {
    private static final TargetHudEditorOverlay INSTANCE = new TargetHudEditorOverlay();

    private static final int[] TEXT = {233, 236, 242};
    private static final int COLOR_TEXT = 0xFFE9ECF2;
    private static final int COLOR_SUBTEXT = 0xFF969BA5;
    private static final int COLOR_DIM = 0xFF686D78;
    private static final int COLOR_PANEL = 0xFF3A3B43;
    private static final int COLOR_CARD = 0xFF312F39;
    private static final int COLOR_ROW_HOVER = 0xFF3F4048;
    private static final int COLOR_INPUT = 0xFF24252C;
    private static final int COLOR_DANGER = 0xFFFF6060;
    private static final int COLOR_ACCENT = 0xFF3D7BFF;

    private static final float MARGIN = 12.0F;
    private static final float HEADER_H = 44.0F;
    private static final float TOP_Y = 50.0F;
    private static final float RAIL_W = 132.0F;
    private static final float GAP = 8.0F;
    private static final float BOTTOM_BAR_H = 46.0F;
    private static final float TAB_H = 30.0F;
    private static final float SETTINGS_W = 302.0F;
    private static final float ROW_H = 34.0F;

    private static final float PICKER_W = 260.0F;
    private static final float PICKER_H = 340.0F;
    private static final float PICKER_GRID = 168.0F;
    private static final float PICKER_GRID_X = 14.0F;
    private static final float PICKER_GRID_Y = 46.0F;
    private static final float PICKER_HUE_X = 190.0F;
    private static final float PICKER_HUE_W = 12.0F;
    private static final float PICKER_SLIDER_X = 14.0F;
    private static final float PICKER_SLIDER_W = PICKER_W - 28.0F;
    private static final float PICKER_SLIDER_Y = 224.0F;
    private static final float PICKER_SLIDER_H = 14.0F;
    private static final float PICKER_SLIDER_STEP = 17.0F;
    private static final float PICKER_HEX_Y = 284.0F;

    private static final String[] VIEWS = {"Слева сверху", "Сверху", "Справа сверху", "Слева", "Центр", "Справа"};
    private static final String[] TABS = {"Вид", "Полоска", "Голова", "Цвет", "Стиль", "Разное", "Пресеты"};
    private static final int TAB_PRESETS = TABS.length - 1;

    private static final String[] TYPES = TargetHudFeature.TYPES;
    private static final String[] DISPLAY_MODES = {"Проценты и HP", "Проценты", "HP"};
    private static final String[] MODES = {TargetHudFeature.MODE_BLADE, TargetHudFeature.MODE_HUIHUI};

    private TargetHudFeature feature;
    private boolean open;

    private int activeView = 4;
    private int activeTab = 0;

    private String focusedField;

    private int pickerColor = -1;
    private float pickerHue;
    private float pickerSat;
    private float pickerBright;

    private boolean managerOpen;
    private int presetsScroll;

    private String presetName = "Таргетхуд";
    private TargetHudPreset appliedPreset;

    private int dragSliderRow = -1;
    private String dragSliderKey = "";
    private boolean dragging;

    private boolean previewDrag;
    private double previewDragOffX;
    private double previewDragOffY;

    private float settingsScroll;

    private String pickerHex = "";
    private boolean pickerDragGrid;
    private boolean pickerDragHue;
    private int pickerDragSlider = -1;

    private String presetError = "";

    private static final int ROW_TYPE_TOGGLE = 0;
    private static final int ROW_TYPE_MODE = 1;
    private static final int ROW_TYPE_NUMBER = 2;
    private static final int ROW_TYPE_COLOR = 3;
    private static final int ROW_TYPE_TEXT = 4;
    private static final int ROW_TYPE_MODE_OWN = 5;

    private static final class Row {
        final String label;
        final int type;
        Object target;
        String textValue;
        float x;
        float y;
        float w;
        float h;
        float controlX;
        float controlW;
        Runnable onButton;

        Row(String label, int type) {
            this.label = label;
            this.type = type;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    private TargetHudEditorOverlay() {
    }

    public static void open(TargetHudFeature feature) {
        INSTANCE.feature = feature;
        INSTANCE.appliedPreset = null;
        INSTANCE.presetName = "Таргетхуд";
        INSTANCE.previewDrag = false;
        INSTANCE.dragging = false;
        INSTANCE.dragSliderRow = -1;
        INSTANCE.dragSliderKey = "";
        if (!INSTANCE.open) {
            INSTANCE.open = true;
            EventManager.subscribe(INSTANCE);
        }

        Minecraft mc = MinecraftContext.mc;
        if (mc != null) {
            if (MenuOverlay.blocksInput()) {
                MenuOverlay.close(mc);
            }
            if (mc.mouseHandler.isMouseGrabbed()) {
                mc.mouseHandler.releaseMouse();
            }
            INSTANCE.snapIntoView(mc);
        }
    }

    public static void close() {
        if (INSTANCE.open) {
            INSTANCE.open = false;
            EventManager.unsubscribe(INSTANCE);
        }
        Minecraft mc = MinecraftContext.mc;
        if (mc != null && mc.level != null && mc.player != null && mc.gui.screen() == null
                && !mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.grabMouse();
        }
    }

    public static boolean isOpen() {
        return INSTANCE.open;
    }

    @EventTarget(priority = 100)
    public void onFinalGuiRender(FinalGuiRenderEvent event) {
        Minecraft mc = event.getClient();
        RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
        try {
            float width = mc.getWindow().getGuiScaledWidth();
            float height = mc.getWindow().getGuiScaledHeight();
            Render2DUtil.beginFrame();
            render(mc, width, height);
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }

    private void render(Minecraft mc, float width, float height) {
        updateDragSlider(mc);
        updatePreviewDrag(mc);
        updatePickerDrag(mc);

        Render2DUtil.rect(0.0F, 0.0F, width, height)
                .color(ColorUtil.rgba(10, 10, 12, 200))
                .draw();

        float railTop = TOP_Y;
        float settingsBottom = height - BOTTOM_BAR_H - MARGIN;

        float panelX = width - SETTINGS_W - MARGIN;
        float panelY = railTop;
        float panelH = settingsBottom - railTop;

        renderPreviewArea(mc, width, height, settingsBottom - MARGIN);
        renderRail(mc, railTop, settingsBottom);
        renderSettings(mc, width, panelX, panelY, SETTINGS_W, panelH);
        renderBottomBar(mc, width, height);

        if (this.managerOpen) {
            renderPresetManager(mc, width, height);
        }
        if (this.pickerColor >= 0) {
            renderColorPicker(mc, width, height);
        }
    }

    private void renderPreviewArea(Minecraft mc, float width, float height, float settingsBottom) {
        float px = this.feature.x.getValue().floatValue();
        float py = this.feature.y.getValue().floatValue();
        if (mc.player != null) {
            this.feature.renderPreview(mc, mc.player, px, py);
        }

        float cell = 32.0F;
        Render2DUtil.rect(0.0F, 0.0F, width, settingsBottom * 0.03F + 1.0F)
                .color(ColorUtil.rgba(255, 255, 255, 8))
                .draw();
        Render2DUtil.rect(0.0F, settingsBottom * 0.9F, width, settingsBottom * 0.1F - 1.0F)
                .color(ColorUtil.rgba(255, 255, 255, 8))
                .draw();
        for (float gx = 0.0F; gx < width; gx += cell) {
            if (gx > 0.0F) {
                Render2DUtil.rect(gx, 0.0F, 1.0F, settingsBottom).color(ColorUtil.rgba(255, 255, 255, 4)).draw();
            }
        }
        for (float gy = 0.0F; gy < settingsBottom; gy += cell) {
            if (gy > 0.0F) {
                Render2DUtil.rect(0.0F, gy, width, 1.0F).color(ColorUtil.rgba(255, 255, 255, 4)).draw();
            }
        }
    }

    private void renderRail(Minecraft mc, float top, float bottom) {
        float x = MARGIN;
        float y = top;
        float w = RAIL_W;
        float h = bottom - top;
        Render2DUtil.rect(x, y, w, h)
                .color(ColorUtil.multiplyAlpha(COLOR_CARD, 1.0F))
                .radius(8.0F)
                .draw();

        float labelY = y + 10.0F;
        Render2DUtil.text(x + w / 2.0F, labelY, 8.0F, "Позиция")
                .weight(600)
                .color(COLOR_SUBTEXT)
                .align(TextAlign.CENTER)
                .draw();

        float rowH = 27.0F;
        float py = labelY + 16.0F;
        for (int i = 0; i < VIEWS.length; i++) {
            boolean sel = i == this.activeView;
            float ry = py + i * (rowH + 4.0F);
            if (ry + rowH > y + h - 4.0F) {
                break;
            }
            if (sel) {
                Render2DUtil.rect(x + 5.0F, ry, w - 10.0F, rowH)
                        .color(ColorUtil.rgba(61, 123, 255, 70))
                        .radius(5.0F)
                        .draw();
            }
            Render2DUtil.text(x + w / 2.0F, ry + 8.0F, 6.5F, VIEWS[i])
                    .weight(sel ? 600 : 400)
                    .color(sel ? COLOR_TEXT : COLOR_DIM)
                    .align(TextAlign.CENTER)
                    .draw();
        }

        float modeY = y + h - 40.0F;
        Render2DUtil.text(x + w / 2.0F, modeY, 8.0F, "Режим")
                .weight(600)
                .color(COLOR_SUBTEXT)
                .align(TextAlign.CENTER)
                .draw();
        String modeLabel = this.feature.mode.getValue();
        for (int i = 0; i < 2; i++) {
            boolean sel = MODES[i].equals(modeLabel);
            float bx = x + 8.0F + i * ((w - 16.0F) / 2.0F);
            Render2DUtil.rect(bx, modeY + 15.0F, (w - 16.0F) / 2.0F - 2.0F, 22.0F)
                    .color(sel ? ColorUtil.rgba(61, 123, 255, 90) : ColorUtil.rgba(255, 255, 255, 10))
                    .radius(5.0F)
                    .draw();
            Render2DUtil.text(bx + (w - 16.0F) / 4.0F, modeY + 22.0F, 6.0F, MODES[i])
                    .weight(600)
                    .color(sel ? COLOR_TEXT : COLOR_SUBTEXT)
                    .align(TextAlign.CENTER)
                    .draw();
        }
    }

    private void renderSettings(Minecraft mc, float width, float x, float y, float w, float h) {
        Render2DUtil.rect(x, y, w, h)
                .color(ColorUtil.multiplyAlpha(COLOR_PANEL, 1.0F))
                .radius(10.0F)
                .draw();

        float tabX = x;
        float tabW = w / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            boolean sel = i == this.activeTab;
            Render2DUtil.rect(tabX, y, tabW, TAB_H)
                    .color(sel ? ColorUtil.rgba(255, 255, 255, 14) : ColorUtil.TRANSPARENT)
                    .radius(0.0F)
                    .draw();
            Render2DUtil.text(tabX + tabW / 2.0F, y + 9.0F, 7.0F, TABS[i])
                    .weight(sel ? 600 : 400)
                    .color(sel ? COLOR_TEXT : COLOR_SUBTEXT)
                    .align(TextAlign.CENTER)
                    .draw();
            if (sel) {
                float underlineW = Math.min(24.0F, tabW * 0.5F);
                Render2DUtil.rect(tabX + tabW / 2.0F - underlineW / 2.0F, y + TAB_H - 2.0F, underlineW, 2.0F)
                        .color(COLOR_ACCENT)
                        .radius(1.0F)
                        .draw();
            }
            tabX += tabW;
        }

        if (this.activeTab == TAB_PRESETS) {
            renderPresetsPanel(mc, x, y, w, h);
            return;
        }

        this.rows.clear();
        buildRows(x, y + TAB_H);
        float contentTop = y + TAB_H + 6.0F;
        float contentH = h - TAB_H - 12.0F;
        float effectiveH = Math.max(1.0F, this.rows.size() * (ROW_H + 4.0F) - 4.0F);
        float maxScroll = Math.max(0.0F, effectiveH - contentH);
        this.settingsScroll = MathUtil.clamp(this.settingsScroll, 0.0F, maxScroll);

        float ry = contentTop - this.settingsScroll;
        for (Row row : this.rows) {
            row.y = ry;
            row.h = ROW_H;
            if (ry + ROW_H >= contentTop - 2.0F && ry <= contentTop + contentH + 2.0F) {
                renderRow(mc, row, x, y + TAB_H);
            }
            ry += ROW_H + 4.0F;
        }
        if (maxScroll > 1.0F) {
            float trackX = x + w - 7.0F;
            Render2DUtil.rect(trackX, contentTop, 2.0F, contentH)
                    .color(ColorUtil.rgba(255, 255, 255, 14))
                    .radius(1.0F)
                    .draw();
            float thumbH = Math.max(18.0F, contentH * (contentH / effectiveH));
            float thumbY = contentTop + (contentH - thumbH) * (this.settingsScroll / maxScroll);
            Render2DUtil.rect(trackX, thumbY, 2.0F, thumbH)
                    .color(COLOR_ACCENT)
                    .radius(1.0F)
                    .draw();
        }
        if (this.rows.isEmpty()) {
            float cx = x + w / 2.0F;
            Render2DUtil.text(cx, y + TAB_H + 20.0F, 8.0F, "Нет настроек в этой вкладке")
                    .weight(400)
                    .color(COLOR_DIM)
                    .align(TextAlign.CENTER)
                    .draw();
        }
    }

    private void renderPresetsPanel(Minecraft mc, float x, float y, float w, float h) {
        float contentTop = y + TAB_H + 6.0F;
        float contentH = h - TAB_H - 12.0F;
        float rowH = 44.0F;
        float gap = 6.0F;
        int visible = Math.max(1, (int) (contentH / (rowH + gap)));

        List<TargetHudPreset> templates = TargetHudPresetManager.getTemplates();
        List<TargetHudPreset> presets = TargetHudPresetManager.getPresets();
        int total = templates.size() + (presets.isEmpty() ? 0 : 1 + presets.size());

        this.presetsScroll = MathUtil.clamp(this.presetsScroll, 0, Math.max(0, total - visible));

        float contentW = w - 16.0F;
        float cardX = x + 8.0F;
        float top = contentTop;
        float slotStep = rowH + gap;
        int mineHeaderAt = templates.size();
        int minBound = Math.min(total, this.presetsScroll + visible);
        for (int slot = this.presetsScroll; slot < minBound; slot++) {
            float ry = top + (slot - this.presetsScroll) * slotStep;
            if (ry > contentTop + contentH) {
                break;
            }

            if (slot < templates.size()) {
                TargetHudPreset preset = templates.get(slot);
                boolean applied = isAppliedPreset(preset);
                Render2DUtil.rect(cardX, ry, contentW, rowH)
                        .color(applied ? ColorUtil.rgba(61, 123, 255, 55) : ColorUtil.multiplyAlpha(COLOR_CARD, 0.55F))
                        .radius(8.0F)
                        .border(applied ? 1.5F : 1.0F, applied ? ColorUtil.rgba(61, 123, 255, 160) : ColorUtil.rgba(61, 123, 255, 60))
                        .draw();

                int swatch = preset.customColors ? preset.barColor : ColorUtil.rgba(56, 152, 255, 255);
                Render2DUtil.rect(x + 18.0F, ry + 10.0F, 24.0F, 24.0F)
                        .color(swatch)
                        .radius(5.0F)
                        .border(1.0F, ColorUtil.rgba(255, 255, 255, 30))
                        .draw();

                Render2DUtil.text(x + 50.0F, ry + 6.0F, 7.5F, preset.name == null ? "(без имени)" : preset.name)
                        .weight(600)
                        .color(COLOR_ACCENT)
                        .draw();
                Render2DUtil.text(x + 50.0F, ry + 25.0F, 5.5F,
                                (preset.type == null ? "" : preset.type) + " · " + ((int) preset.x) + ";" + ((int) preset.y))
                        .weight(400)
                        .color(COLOR_DIM)
                        .draw();

                if (applied) {
                    Render2DUtil.text(x + contentW - 158.0F, ry + 12.0F, 6.5F, "✓ Активный")
                            .weight(600)
                            .color(ColorUtil.rgba(90, 200, 120, 255))
                            .align(TextAlign.RIGHT)
                            .draw();
                } else {
                    renderButton(x + contentW - 76.0F, ry + 10.0F, 66.0F, 24.0F, "Загрузить",
                            ColorUtil.rgba(36, 80, 190, 210), COLOR_TEXT, 5.5F);
                }
                renderButton(x + contentW - 148.0F, ry + 10.0F, 66.0F, 24.0F, "В мои",
                        ColorUtil.rgba(56, 120, 70, 210), COLOR_TEXT, 5.5F);
                continue;
            }

            if (mineHeaderAt > 0 && slot == mineHeaderAt) {
                Render2DUtil.text(cardX + 2.0F, ry + 14.0F, 6.0F, "МОИ ТАРГЕТХУДЫ")
                        .weight(600)
                        .color(COLOR_SUBTEXT)
                        .draw();
                Render2DUtil.text(cardX + 2.0F + 7.0F * 13.0F + 8.0F, ry + 14.0F, 6.0F, String.valueOf(presets.size()))
                        .weight(400)
                        .color(COLOR_DIM)
                        .draw();
                continue;
            }

            TargetHudPreset preset = presets.get(slot - templates.size() - mineHeaderAt);
            boolean applied = isAppliedPreset(preset);
            Render2DUtil.rect(cardX, ry, contentW, rowH)
                    .color(applied ? ColorUtil.rgba(61, 123, 255, 55) : ColorUtil.multiplyAlpha(COLOR_CARD, 0.55F))
                    .radius(8.0F)
                    .border(applied ? 1.5F : 1.0F, applied ? ColorUtil.rgba(61, 123, 255, 160) : ColorUtil.rgba(255, 255, 255, 14))
                    .draw();

            int swatch = preset.customColors ? preset.barColor : ColorUtil.rgba(56, 152, 255, 255);
            Render2DUtil.rect(x + 18.0F, ry + 10.0F, 24.0F, 24.0F)
                    .color(swatch)
                    .radius(5.0F)
                    .border(1.0F, ColorUtil.rgba(255, 255, 255, 30))
                    .draw();

            Render2DUtil.text(x + 50.0F, ry + 6.0F, 7.5F, preset.name == null ? "(без имени)" : preset.name)
                    .weight(600)
                    .color(COLOR_TEXT)
                    .draw();
            Render2DUtil.text(x + 50.0F, ry + 25.0F, 5.5F,
                            (preset.type == null ? "" : preset.type) + " · " + ((int) preset.x) + ";" + ((int) preset.y))
                    .weight(400)
                    .color(COLOR_DIM)
                    .draw();

            if (applied) {
                Render2DUtil.text(x + contentW - 158.0F, ry + 12.0F, 6.5F, "✓ Активный")
                        .weight(600)
                        .color(ColorUtil.rgba(90, 200, 120, 255))
                        .align(TextAlign.RIGHT)
                        .draw();
            } else {
                renderButton(x + contentW - 76.0F, ry + 10.0F, 66.0F, 24.0F, "Загрузить",
                        ColorUtil.rgba(36, 80, 190, 210), COLOR_TEXT, 5.5F);
            }
            renderButton(x + contentW - 148.0F, ry + 10.0F, 66.0F, 24.0F, "Удалить",
                    ColorUtil.rgba(120, 40, 40, 220), COLOR_TEXT, 5.5F);
        }

        if (total > visible) {
            float trackX = x + w - 7.0F;
            Render2DUtil.rect(trackX, contentTop, 2.0F, contentH)
                    .color(ColorUtil.rgba(255, 255, 255, 14))
                    .radius(1.0F)
                    .draw();
            float thumbH = Math.max(18.0F, contentH * ((float) visible / total));
            float thumbY = contentTop + (contentH - thumbH) * ((float) this.presetsScroll / Math.max(1, total - visible));
            Render2DUtil.rect(trackX, thumbY, 2.0F, thumbH)
                    .color(COLOR_ACCENT)
                    .radius(1.0F)
                    .draw();
        }
    }

    private void buildRows(float panelX, float panelTop) {
        float controlX = 0.0F;
        if (this.activeTab == TAB_PRESETS) {
            return;
        }
        switch (this.activeTab) {
            case 0 -> {
                Row type = new Row("Тип", ROW_TYPE_MODE_OWN);
                type.textValue = this.feature.type.getValue();
                addRow(panelX, type, controlX);

                Row posX = new Row("Позиция X", ROW_TYPE_NUMBER);
                posX.target = this.feature.x;
                addRow(panelX, posX, controlX);

                Row posY = new Row("Позиция Y", ROW_TYPE_NUMBER);
                posY.target = this.feature.y;
                addRow(panelX, posY, controlX);

                Row hover = new Row("Наведение", ROW_TYPE_TOGGLE);
                hover.target = this.feature.hover;
                addRow(panelX, hover, controlX);
            }
            case 1 -> {
                Row barThickness = new Row("Толщина полоски", ROW_TYPE_NUMBER);
                barThickness.target = this.feature.barThickness;
                addRow(panelX, barThickness, controlX);

                Row barRadius = new Row("Скругление полоски", ROW_TYPE_NUMBER);
                barRadius.target = this.feature.barRadius;
                addRow(panelX, barRadius, controlX);

                Row showSecond = new Row("Золотая полоска", ROW_TYPE_TOGGLE);
                showSecond.target = this.feature.showSecondBar;
                addRow(panelX, showSecond, controlX);

                if (this.feature.showSecondBar.getValue()) {
                    Row match = new Row("Полоски одного размера", ROW_TYPE_TOGGLE);
                    match.target = this.feature.matchBarThickness;
                    addRow(panelX, match, controlX);

                    if (!this.feature.matchBarThickness.getValue()) {
                        Row secondThickness = new Row("Толщина золотой полоски", ROW_TYPE_NUMBER);
                        secondThickness.target = this.feature.secondBarThickness;
                        addRow(panelX, secondThickness, controlX);
                    }
                }
            }
            case 2 -> {
                Row headSize = new Row("Размер головы", ROW_TYPE_NUMBER);
                headSize.target = this.feature.headSize;
                addRow(panelX, headSize, controlX);

                Row yaw = new Row("Поворот головы", ROW_TYPE_NUMBER);
                yaw.target = this.feature.headYaw;
                addRow(panelX, yaw, controlX);

                Row pitch = new Row("Наклон головы", ROW_TYPE_NUMBER);
                pitch.target = this.feature.headPitch;
                addRow(panelX, pitch, controlX);

                Row autoRotate = new Row("Автоповорот головы", ROW_TYPE_TOGGLE);
                autoRotate.target = this.feature.headAutoRotate;
                addRow(panelX, autoRotate, controlX);

                Row eyes = new Row("Глаза на голове", ROW_TYPE_TOGGLE);
                eyes.target = this.feature.showEyes;
                addRow(panelX, eyes, controlX);

                if (this.feature.showEyes.getValue()) {
                    Row eyeSize = new Row("Размер глаз", ROW_TYPE_NUMBER);
                    eyeSize.target = this.feature.eyeSize;
                    addRow(panelX, eyeSize, controlX);
                }
            }
            case 3 -> {
                Row custom = new Row("Свои цвета", ROW_TYPE_TOGGLE);
                custom.target = this.feature.customColors;
                addRow(panelX, custom, controlX);

                addColorRow(panelX, "Цвет полоски", this.feature.barColor);
                addColorRow(panelX, "Цвет золотой полоски", this.feature.barColorSecond);
                addColorRow(panelX, "Цвет фона", this.feature.bgColor);
                addColorRow(panelX, "Цвет рамки", this.feature.borderColor);
                addColorRow(panelX, "Цвет текста", this.feature.textColor);
            }
            case 4 -> {
                Row display = new Row("Отображение", ROW_TYPE_MODE);
                display.textValue = this.feature.displayMode.getValue();
                addRow(panelX, display, controlX);

                Row radius = new Row("Скругление", ROW_TYPE_NUMBER);
                radius.target = this.feature.radius;
                addRow(panelX, radius, controlX);

                Row border = new Row("Толщина рамки", ROW_TYPE_NUMBER);
                border.target = this.feature.borderThickness;
                addRow(panelX, border, controlX);

                Row bgAlpha = new Row("Прозрачность фона", ROW_TYPE_NUMBER);
                bgAlpha.target = this.feature.backgroundAlpha;
                addRow(panelX, bgAlpha, controlX);

                Row anim = new Row("Скорость анимации", ROW_TYPE_NUMBER);
                anim.target = this.feature.animationSpeed;
                addRow(panelX, anim, controlX);

                Row armor = new Row("Показывать броню", ROW_TYPE_TOGGLE);
                armor.target = this.feature.showArmor;
                addRow(panelX, armor, controlX);

                Row ping = new Row("Показывать пинг", ROW_TYPE_TOGGLE);
                ping.target = this.feature.showPing;
                addRow(panelX, ping, controlX);
            }
            default -> {
                Row hover = new Row("При наведении", ROW_TYPE_TOGGLE);
                hover.target = this.feature.hover;
                addRow(panelX, hover, controlX);

                Row bgImage = new Row("Картинка фона", ROW_TYPE_TEXT);
                bgImage.textValue = this.feature.bgImage.getValue();
                bgImage.target = this.feature.bgImage;
                addRow(panelX, bgImage, controlX);

                Row headImage = new Row("Картинка головы", ROW_TYPE_TEXT);
                headImage.textValue = this.feature.headImage.getValue();
                headImage.target = this.feature.headImage;
                addRow(panelX, headImage, controlX);
            }
        }
    }

    private void addRow(float panelX, Row row, float controlX) {
        row.x = panelX;
        row.w = SETTINGS_W - 16.0F;
        row.controlX = panelX + SETTINGS_W - 24.0F;
        row.controlW = 110.0F;
        this.rows.add(row);
    }

    private void addColorRow(float panelX, String label, ColorSetting setting) {
        Row row = new Row(label, ROW_TYPE_COLOR);
        row.target = setting;
        addRow(panelX, row, 0.0F);
    }

    private void renderRow(Minecraft mc, Row row, float panelX, float panelTop) {
        if (row.y < 0.0F) {
            return;
        }
        float hoveredLeft = panelX + 8.0F;
        Render2DUtil.text(row.x + 14.0F, row.y + 10.0F, 7.0F, row.label)
                .weight(400)
                .color(COLOR_TEXT)
                .draw();

        float cx = row.controlX;
        switch (row.type) {
            case ROW_TYPE_TOGGLE -> {
                BooleanSetting setting = (BooleanSetting) row.target;
                renderToggle(cx - 14.0F, row.y + 8.0F, 18.0F, setting.getValue());
            }
            case ROW_TYPE_MODE -> {
                renderCycle(cx - 22.0F, row.y + 7.0F, row.textValue);
            }
            case ROW_TYPE_MODE_OWN -> {
                renderCycle(cx - 22.0F, row.y + 7.0F, row.textValue);
            }
            case ROW_TYPE_NUMBER -> {
                NumberSetting setting = (NumberSetting) row.target;
                renderSlider(cx - 110.0F, row.y + 11.0F, 110.0F, setting);
            }
            case ROW_TYPE_COLOR -> {
                ColorSetting setting = (ColorSetting) row.target;
                boolean enabled = this.feature.customColors.getValue();
                int color = enabled ? setting.getValue() : ColorUtil.multiplyAlpha(setting.getValue(), 0.18F);
                Render2DUtil.rect(cx - 34.0F, row.y + 7.0F, 24.0F, 18.0F)
                        .color(color)
                        .radius(4.0F)
                        .border(1.0F, enabled ? ColorUtil.rgba(255, 255, 255, 40) : ColorUtil.rgba(255, 255, 255, 12))
                        .draw();
            }
            case ROW_TYPE_TEXT -> {
                TextSetting setting = (TextSetting) row.target;
                renderTextField(cx - row.controlW - 8.0F, row.y + 6.0F, row.controlW + 16.0F,
                        setting.getValue(), focusKey(setting));
            }
        }
    }

    private String focusKey(Object setting) {
        if (setting == this.feature.bgImage) {
            return "bgImage";
        }
        if (setting == this.feature.headImage) {
            return "headImage";
        }
        return "text";
    }

    private void renderToggle(float x, float y, float size, boolean value) {
        Render2DUtil.rect(x, y, size, size)
                .color(value ? ColorUtil.rgba(61, 123, 255, 200) : ColorUtil.rgba(255, 255, 255, 30))
                .radius(4.0F)
                .draw();
        if (value) {
            Render2DUtil.rect(x + 5.0F, y + 4.0F, size - 10.0F, 3.0F)
                    .color(COLOR_TEXT)
                    .radius(1.5F)
                    .draw();
        }
    }

    private void renderCycle(float x, float y, String label) {
        float w = 80.0F;
        Render2DUtil.rect(x, y, w, 20.0F)
                .color(ColorUtil.rgba(255, 255, 255, 12))
                .radius(5.0F)
                .draw();
        Render2DUtil.text(x + w / 2.0F, y + 5.0F, 6.5F, label)
                .weight(500)
                .color(COLOR_TEXT)
                .align(TextAlign.CENTER)
                .draw();
        Render2DUtil.text(x + 6.0F, y + 5.0F, 6.5F, "◀")
                .weight(500)
                .color(COLOR_SUBTEXT)
                .align(TextAlign.CENTER)
                .draw();
        Render2DUtil.text(x + w - 6.0F, y + 5.0F, 6.5F, "▶")
                .weight(500)
                .color(COLOR_SUBTEXT)
                .align(TextAlign.CENTER)
                .draw();
    }

    private void renderSlider(float x, float y, float w, NumberSetting setting) {
        float progress = (float) setting.getProgress();
        float trackH = 4.0F;
        Render2DUtil.rect(x, y + 4.0F, w, trackH)
                .color(ColorUtil.rgba(255, 255, 255, 30))
                .radius(2.0F)
                .draw();
        if (progress > 0.0F) {
            Render2DUtil.rect(x, y + 4.0F, w * MathUtil.clamp01(progress), trackH)
                    .color(COLOR_ACCENT)
                    .radius(2.0F)
                    .draw();
        }
        float thumbX = x + w * MathUtil.clamp01(progress);
        Render2DUtil.rect(thumbX - 4.0F, y, 8.0F, 12.0F)
                .color(COLOR_TEXT)
                .radius(4.0F)
                .draw();
        Render2DUtil.text(x + w + 10.0F, y + 1.0F, 6.0F, formatNumber(setting))
                .weight(500)
                .color(COLOR_SUBTEXT)
                .draw();
    }

    private String formatNumber(NumberSetting setting) {
        double value = setting.getValue();
        return value == Math.floor(value) ? String.valueOf((int) value)
                : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void renderTextField(float x, float y, float w, String value) {
        Render2DUtil.rect(x, y, w, 20.0F)
                .color(ColorUtil.multiplyAlpha(COLOR_INPUT, 1.0F))
                .radius(5.0F)
                .border(1.0F, ColorUtil.rgba(255, 255, 255, 20))
                .draw();
        String label = value == null || value.isEmpty() ? "..." : value;
        Render2DUtil.text(x + 6.0F, y + 5.0F, 6.0F, label)
                .weight(400)
                .color(COLOR_SUBTEXT)
                .align(TextAlign.LEFT)
                .draw();
    }

    private void renderTextField(float x, float y, float w, String value, String focus) {
        renderTextField(x, y, w, value);
    }

    private void renderBottomBar(Minecraft mc, float width, float height) {
        float y = height - BOTTOM_BAR_H - MARGIN;
        float x = MARGIN;
        float w = width - MARGIN * 2.0F;
        Render2DUtil.rect(x, y, w, BOTTOM_BAR_H)
                .color(ColorUtil.multiplyAlpha(COLOR_CARD, 1.0F))
                .radius(8.0F)
                .draw();

        float nameW = 180.0F;
        Render2DUtil.rect(x + 10.0F, y + 12.0F, nameW, 24.0F)
                .color(ColorUtil.multiplyAlpha(COLOR_INPUT, 1.0F))
                .radius(5.0F)
                .border(1.0F, "preset".equals(this.focusedField)
                        ? ColorUtil.rgba(61, 123, 255, 200)
                        : ColorUtil.rgba(255, 255, 255, 20))
                .draw();
        Render2DUtil.text(x + 16.0F, y + 18.0F, 7.0F, this.presetName)
                .weight(400)
                .color(COLOR_TEXT)
                .draw();
        Render2DUtil.text(x + 16.0F + 180.0F + 8.0F, y + 12.0F, 5.5F, "Название пресета")
                .weight(400)
                .color(COLOR_DIM)
                .draw();

        float bx = x + w - 96.0F;
        renderButton(bx, y + 12.0F, 86.0F, 24.0F, "Закрыть", COLOR_CARD, COLOR_TEXT, 6.5F);
        bx -= 110.0F;
        renderButton(bx, y + 12.0F, 100.0F, 24.0F, "Менеджер", COLOR_CARD, COLOR_TEXT, 6.5F);
        bx -= 116.0F;
        renderButton(bx, y + 12.0F, 106.0F, 24.0F, "Сохранить", ColorUtil.rgba(36, 80, 190, 210), COLOR_TEXT, 6.5F);

        if (!this.presetError.isEmpty()) {
            Render2DUtil.text(x + 16.0F + nameW + 14.0F, y + 26.0F, 5.5F, this.presetError)
                    .weight(400)
                    .color(COLOR_DANGER)
                    .draw();
        }
    }

    private void renderButton(float x, float y, float w, float h, String label, int bg, int fg, float size) {
        Render2DUtil.rect(x, y, w, h)
                .color(bg)
                .radius(5.0F)
                .border(1.0F, ColorUtil.rgba(255, 255, 255, 20))
                .draw();
        Render2DUtil.text(x + w / 2.0F, y + h / 2.0F - size * 0.5F, size, label)
                .weight(600)
                .color(fg)
                .align(TextAlign.CENTER)
                .draw();
    }

    private void renderPresetManager(Minecraft mc, float width, float height) {
        float pw = 320.0F;
        float ph = 300.0F;
        float px = width / 2.0F - pw / 2.0F;
        float py = height / 2.0F - ph / 2.0F;
        Render2DUtil.rect(px - 6.0F, py - 6.0F, pw + 12.0F, ph + 12.0F)
                .color(ColorUtil.rgba(0, 0, 0, 150))
                .radius(12.0F)
                .draw();

        List<TargetHudPreset> presets = TargetHudPresetManager.getPresets();
        float rowH = 34.0F;
        float top = py + 40.0F;

        if (presets.isEmpty()) {
            Render2DUtil.text(px + pw / 2.0F, py + 90.0F, 7.0F, "Пресетов пока нет")
                    .weight(400)
                    .color(COLOR_SUBTEXT)
                    .align(TextAlign.CENTER)
                    .draw();
            Render2DUtil.text(px + pw / 2.0F, py + 108.0F, 5.5F, "Настройте таргетхуд и нажмите «Сохранить» внизу")
                    .weight(400)
                    .color(COLOR_DIM)
                    .align(TextAlign.CENTER)
                    .draw();
        }

        for (int i = this.presetsScroll; i < Math.min(presets.size(), this.presetsScroll + 6); i++) {
            TargetHudPreset preset = presets.get(i);
            boolean applied = isAppliedPreset(preset);
            float ry = top + (i - this.presetsScroll) * (rowH + 4.0F);
            Render2DUtil.rect(px, ry, pw, rowH)
                    .color(applied ? ColorUtil.rgba(61, 123, 255, 55) : ColorUtil.multiplyAlpha(COLOR_ROW_HOVER, 1.0F))
                    .radius(6.0F)
                    .border(1.0F, applied ? ColorUtil.rgba(61, 123, 255, 160) : ColorUtil.rgba(255, 255, 255, 0))
                    .draw();
            int swatch = preset.customColors ? preset.barColor : ColorUtil.rgba(56, 152, 255, 255);
            Render2DUtil.rect(px + 10.0F, ry + 7.0F, 20.0F, 20.0F)
                    .color(swatch)
                    .radius(4.0F)
                    .draw();
            Render2DUtil.text(px + 38.0F, ry + 9.0F, 7.0F, preset.name == null ? "(без имени)" : preset.name)
                    .weight(500)
                    .color(COLOR_TEXT)
                    .draw();
            Render2DUtil.text(px + 38.0F, ry + 20.0F, 5.5F,
                            (preset.type == null ? "" : preset.type) + " · " + (int) preset.x + ";" + (int) preset.y)
                    .weight(400)
                    .color(COLOR_DIM)
                    .draw();

            float fx = px + pw - 150.0F;
            if (!applied) {
                renderButton(fx, ry + 5.0F, 66.0F, 24.0F, "Загрузить", ColorUtil.rgba(36, 80, 190, 210), COLOR_TEXT, 5.5F);
            }
            renderButton(fx + 74.0F, ry + 5.0F, 66.0F, 24.0F, "Удалить", ColorUtil.rgba(120, 40, 40, 220), COLOR_TEXT, 5.5F);
        }

        if (presets.size() > 6) {
            renderButton(px + pw - 60.0F, py + 12.0F, 52.0F, 20.0F, "▼", COLOR_CARD, COLOR_TEXT, 6.0F);
            this.presetsScroll = MathUtil.clamp(this.presetsScroll, 0, presets.size() - 6);
        }

        Render2DUtil.text(px + pw / 2.0F, py + 14.0F, 9.0F, "Пресеты")
                .weight(600)
                .color(COLOR_TEXT)
                .align(TextAlign.CENTER)
                .draw();
        renderButton(px + pw - 40.0F, py + 8.0F, 30.0F, 22.0F, "✕", COLOR_CARD, COLOR_TEXT, 6.0F);
    }

    private void renderColorPicker(Minecraft mc, float width, float height) {
        float px = width / 2.0F - PICKER_W / 2.0F;
        float py = height / 2.0F - PICKER_H / 2.0F;
        Render2DUtil.rect(px - 6.0F, py - 6.0F, PICKER_W + 12.0F, PICKER_H + 12.0F)
                .color(ColorUtil.rgba(0, 0, 0, 150))
                .radius(12.0F)
                .draw();
        Render2DUtil.rect(px, py, PICKER_W, PICKER_H)
                .color(ColorUtil.multiplyAlpha(COLOR_PANEL, 1.0F))
                .radius(10.0F)
                .draw();

        float gridX = px + PICKER_GRID_X;
        float gridY = py + PICKER_GRID_Y;
        int cells = 12;
        float cell = PICKER_GRID / cells;
        int[] colors = new int[cells * cells];
        for (int row = 0; row < cells; row++) {
            for (int col = 0; col < cells; col++) {
                float s = col / (cells - 1.0F);
                float b = 1.0F - row / (cells - 1.0F);
                colors[row * cells + col] = ColorUtil.fromHsv(this.pickerHue, s, b, 255);
            }
        }
        int selCol = (int) MathUtil.clamp(Math.round(this.pickerSat * (cells - 1)), 0L, (long) (cells - 1));
        int selRow = (int) MathUtil.clamp(Math.round((1.0F - this.pickerBright) * (cells - 1)), 0L, (long) (cells - 1));
        Render2DUtil.colorGrid(gridX, gridY, cell, cells, colors)
                .radius(2.0F)
                .selected(selRow * cells + selCol, 2.0F)
                .draw();

        float hueX = px + PICKER_HUE_X;
        int hueCells = 14;
        float hueCell = PICKER_GRID / hueCells;
        int[] hueColors = new int[hueCells];
        for (int i = 0; i < hueCells; i++) {
            hueColors[i] = ColorUtil.fromHsv(i / (hueCells - 1.0F), 1.0F, 1.0F, 255);
        }
        Render2DUtil.colorGrid(hueX, gridY, hueCell, 1, hueColors)
                .radius(2.0F)
                .draw();
        int selHue = (int) MathUtil.clamp(Math.round(this.pickerHue * (hueCells - 1)), 0L, (long) (hueCells - 1));
        Render2DUtil.rect(hueX - 3.0F, gridY + selHue * hueCell - 3.0F, PICKER_HUE_W + 6.0F, 6.0F)
                .color(COLOR_TEXT)
                .radius(3.0F)
                .draw();

        float sliderX = px + PICKER_SLIDER_X;
        float sliderY = py + PICKER_SLIDER_Y;
        renderPickerSlider(sliderX, sliderY, PICKER_SLIDER_W, "H", this.pickerHue, 0);
        renderPickerSlider(sliderX, sliderY + PICKER_SLIDER_STEP, PICKER_SLIDER_W, "S", this.pickerSat, 1);
        renderPickerSlider(sliderX, sliderY + PICKER_SLIDER_STEP * 2.0F, PICKER_SLIDER_W, "B", this.pickerBright, 2);

        renderHexField(sliderX, py + PICKER_HEX_Y, 120.0F, this.pickerHex);
        int current = pickerSetting().getValue();
        Render2DUtil.rect(sliderX + 130.0F, py + PICKER_HEX_Y, PICKER_SLIDER_W - 130.0F, 20.0F)
                .color(current)
                .radius(4.0F)
                .border(1.0F, ColorUtil.rgba(255, 255, 255, 40))
                .draw();

        String name = pickerColorName();
        Render2DUtil.text(px + PICKER_W / 2.0F, py + 12.0F, 8.0F, name)
                .weight(600)
                .color(COLOR_TEXT)
                .align(TextAlign.CENTER)
                .draw();
        renderButton(px + PICKER_W - 44.0F, py + 8.0F, 30.0F, 22.0F, "✕", COLOR_CARD, COLOR_TEXT, 6.0F);

        renderButton(px + PICKER_SLIDER_X, py + PICKER_H - 36.0F, 92.0F, 24.0F, "Сброс", COLOR_CARD, COLOR_TEXT, 5.5F);
        renderButton(px + PICKER_W - PICKER_SLIDER_X - 92.0F, py + PICKER_H - 36.0F, 92.0F, 24.0F, "Готово", ColorUtil.rgba(36, 80, 190, 210), COLOR_TEXT, 5.5F);
    }

    private void renderPickerSlider(float x, float y, float w, String label, float progress, int axis) {
        int columns = Math.max(1, (int) (w / 14.0F));
        float cell = w / columns;
        int[] colors = new int[columns];
        for (int i = 0; i < columns; i++) {
            float t = i / (columns - 1.0F);
            colors[i] = switch (axis) {
                case 0 -> ColorUtil.fromHsv(t, 1.0F, 1.0F, 255);
                case 1 -> ColorUtil.fromHsv(this.pickerHue, t, this.pickerBright, 255);
                default -> ColorUtil.fromHsv(this.pickerHue, this.pickerSat, t, 255);
            };
        }
        Render2DUtil.colorGrid(x, y, cell, columns, colors)
                .radius(2.0F)
                .draw();
        Render2DUtil.text(x + 4.0F, y + cell / 2.0F - 3.0F, 6.0F, label)
                .weight(600)
                .color(COLOR_DIM)
                .draw();
        float knobX = x + MathUtil.clamp(progress, 0.0F, 1.0F) * (w - cell) + cell / 2.0F;
        Render2DUtil.rect(knobX - 2.0F, y - 2.0F, 4.0F, cell + 4.0F)
                .color(COLOR_TEXT)
                .radius(2.0F)
                .draw();
    }

    private void renderHexField(float x, float y, float w, String value) {
        Render2DUtil.rect(x, y, w, 20.0F)
                .color(COLOR_INPUT)
                .radius(4.0F)
                .border(1.0F, "pickerHex".equals(this.focusedField) ? ColorUtil.rgba(61, 123, 255, 160) : ColorUtil.rgba(255, 255, 255, 24))
                .draw();
        Render2DUtil.text(x + 8.0F, y + 6.0F, 6.5F, "#")
                .weight(600)
                .color(COLOR_SUBTEXT)
                .draw();
        Render2DUtil.text(x + 16.0F, y + 6.0F, 6.5F, value)
                .weight(500)
                .color(COLOR_TEXT)
                .draw();
        Render2DUtil.text(x + w - 22.0F, y + 6.0F, 6.5F, "HEX")
                .weight(600)
                .color(COLOR_DIM)
                .align(TextAlign.RIGHT)
                .draw();
    }

    private ColorSetting pickerSetting() {
        return switch (this.pickerColor) {
            case 1 -> this.feature.barColorSecond;
            case 2 -> this.feature.bgColor;
            case 3 -> this.feature.borderColor;
            case 4 -> this.feature.textColor;
            default -> this.feature.barColor;
        };
    }

    private String pickerColorName() {
        return switch (this.pickerColor) {
            case 1 -> "Второй цвет полоски";
            case 2 -> "Цвет фона";
            case 3 -> "Цвет рамки";
            case 4 -> "Цвет текста";
            default -> "Цвет полоски";
        };
    }

    private void initPicker(int index) {
        this.pickerColor = index;
        float[] hsb = ColorUtil.hsv(pickerSetting().getValue());
        this.pickerHue = hsb[0];
        this.pickerSat = hsb[1];
        this.pickerBright = hsb[2];
        this.pickerDragGrid = false;
        this.pickerDragHue = false;
        this.pickerDragSlider = -1;
        this.pickerHex = String.format("%06X", pickerSetting().getValue() & 0xFFFFFF);
    }

    private void applyPickerColor() {
        pickerSetting().setValue(ColorUtil.fromHsv(this.pickerHue, this.pickerSat, this.pickerBright, 255));
        this.pickerHex = String.format("%06X", pickerSetting().getValue() & 0xFFFFFF);
    }

    @EventTarget(priority = 1_500)
    public void onMouseInput(MouseInputEvent event) {
        if (!this.open) {
            return;
        }
        Minecraft mc = MinecraftContext.mc;
        if (mc == null) {
            return;
        }
        float mouseX = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float mouseY = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();

        if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (this.dragging || this.dragSliderRow >= 0 || this.pickerColor >= 0 || this.previewDrag) {
                this.dragging = false;
                this.dragSliderRow = -1;
                this.dragSliderKey = "";
                this.previewDrag = false;
                this.pickerDragGrid = false;
                this.pickerDragHue = false;
                this.pickerDragSlider = -1;
                event.cancel();
                return;
            }
            event.cancel();
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            event.cancel();
            return;
        }

        if (handleColorPickerClick(mouseX, mouseY, width, height)) {
            event.cancel();
            return;
        }
        if (this.managerOpen) {
            if (handleManagerClick(mouseX, mouseY, width, height)) {
                event.cancel();
                return;
            }
        }
        if (handleBottomBarClick(mouseX, mouseY, width, height)) {
            event.cancel();
            return;
        }
        if (handleRailClick(mouseX, mouseY, width, height)) {
            event.cancel();
            return;
        }
        if (handleSettingsClick(mouseX, mouseY, width, height)) {
            event.cancel();
            return;
        }
        if (tryStartPreviewDrag(mouseX, mouseY, width, height)) {
            event.cancel();
            return;
        }
        this.focusedField = null;
        event.cancel();
    }

    private boolean handleColorPickerClick(float mx, float my, float width, float height) {
        if (this.pickerColor < 0) {
            return false;
        }
        float px = width / 2.0F - PICKER_W / 2.0F;
        float py = height / 2.0F - PICKER_H / 2.0F;

        float gridX = px + PICKER_GRID_X;
        float gridY = py + PICKER_GRID_Y;
        if (inside(mx, my, gridX, gridY, PICKER_GRID, PICKER_GRID)) {
            applyPickerGrid(mx, my, gridX, gridY);
            this.pickerDragGrid = true;
            return true;
        }
        float hueX = px + PICKER_HUE_X;
        if (inside(mx, my, hueX, gridY, PICKER_HUE_W, PICKER_GRID)) {
            this.pickerHue = MathUtil.clamp01((my - gridY) / PICKER_GRID);
            applyPickerColor();
            this.pickerDragHue = true;
            return true;
        }
        float sliderX = px + PICKER_SLIDER_X;
        float sliderY = py + PICKER_SLIDER_Y;
        for (int i = 0; i < 3; i++) {
            float sy = sliderY + i * PICKER_SLIDER_STEP;
            if (inside(mx, my, sliderX, sy, PICKER_SLIDER_W, PICKER_SLIDER_H)) {
                applyPickerSlider(mx, sliderX, PICKER_SLIDER_W, i);
                this.pickerDragSlider = i;
                return true;
            }
        }
        if (inside(mx, my, sliderX, py + PICKER_HEX_Y, 120.0F, 20.0F)) {
            this.focusedField = "pickerHex";
            return true;
        }
        if (inside(mx, my, px + PICKER_W - 44.0F, py + 8.0F, 30.0F, 22.0F)) {
            this.pickerColor = -1;
            this.focusedField = null;
            return true;
        }
        if (inside(mx, my, px + PICKER_SLIDER_X, py + PICKER_H - 36.0F, 92.0F, 24.0F)) {
            pickerSetting().reset();
            initPicker(this.pickerColor);
            return true;
        }
        if (inside(mx, my, px + PICKER_W - PICKER_SLIDER_X - 92.0F, py + PICKER_H - 36.0F, 92.0F, 24.0F)) {
            this.pickerColor = -1;
            this.focusedField = null;
            return true;
        }
        return false;
    }

    private void applyPickerGrid(float mx, float my, float gridX, float gridY) {
        this.pickerSat = MathUtil.clamp01((mx - gridX) / PICKER_GRID);
        this.pickerBright = 1.0F - MathUtil.clamp01((my - gridY) / PICKER_GRID);
        applyPickerColor();
    }

    private void applyPickerSlider(float mx, float sliderX, float sliderW, int axis) {
        float t = MathUtil.clamp01((mx - sliderX) / sliderW);
        switch (axis) {
            case 0 -> this.pickerHue = t;
            case 1 -> this.pickerSat = t;
            default -> this.pickerBright = t;
        }
        applyPickerColor();
    }

    private void updatePickerDrag(Minecraft mc) {
        if (!this.open || this.pickerColor < 0) {
            return;
        }
        if (!this.pickerDragGrid && !this.pickerDragHue && this.pickerDragSlider < 0) {
            return;
        }
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        float mx = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float my = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
        float px = width / 2.0F - PICKER_W / 2.0F;
        float py = height / 2.0F - PICKER_H / 2.0F;

        if (this.pickerDragGrid) {
            applyPickerGrid(mx, my, px + PICKER_GRID_X, py + PICKER_GRID_Y);
        }
        if (this.pickerDragHue) {
            this.pickerHue = MathUtil.clamp01((my - (py + PICKER_GRID_Y)) / PICKER_GRID);
            applyPickerColor();
        }
        if (this.pickerDragSlider >= 0) {
            applyPickerSlider(mx, px + PICKER_SLIDER_X, PICKER_SLIDER_W, this.pickerDragSlider);
        }
    }

    @EventTarget(priority = 1_500)
    public void onScrollInput(ScrollInputEvent event) {
        if (!this.open) {
            return;
        }
        if (this.pickerColor >= 0) {
            event.cancel();
            return;
        }
        if (this.managerOpen) {
            this.presetsScroll -= (int) event.getVertical();
            event.cancel();
            return;
        }
        if (this.activeTab == TAB_PRESETS) {
            this.presetsScroll -= (int) event.getVertical();
            event.cancel();
            return;
        }
        this.settingsScroll -= (float) event.getVertical() * 12.0F;
        event.cancel();
    }

    private boolean handleManagerClick(float mx, float my, float width, float height) {
        float pw = 320.0F;
        float ph = 300.0F;
        float px = width / 2.0F - pw / 2.0F;
        float py = height / 2.0F - ph / 2.0F;
        if (inside(mx, my, px + pw - 40.0F, py + 8.0F, 30.0F, 22.0F)) {
            this.managerOpen = false;
            return true;
        }
        List<TargetHudPreset> presets = TargetHudPresetManager.getPresets();
        float rowH = 34.0F;
        float top = py + 40.0F;
        for (int i = this.presetsScroll; i < Math.min(presets.size(), this.presetsScroll + 6); i++) {
            float ry = top + (i - this.presetsScroll) * (rowH + 4.0F);
            if (inside(mx, my, px + pw - 150.0F, ry + 5.0F, 66.0F, 24.0F)) {
                applyPreset(presets.get(i));
                snapIntoView(Minecraft.getInstance());
                this.managerOpen = false;
                return true;
            }
            if (inside(mx, my, px + pw - 76.0F, ry + 5.0F, 66.0F, 24.0F)) {
                TargetHudPresetManager.deletePreset(presets.get(i).name);
                if (isAppliedPreset(presets.get(i))) {
                    this.appliedPreset = null;
                    this.presetName = "Таргетхуд";
                }
                return true;
            }
        }
        return false;
    }

    private void applyPreset(TargetHudPreset preset) {
        this.feature.type.setValue(isValidMode(this.feature.type, preset.type) ? preset.type : TYPES[0]);
        this.feature.mode.setValue(TargetHudFeature.MODE_HUIHUI);
        this.feature.x.setValue(preset.x);
        this.feature.y.setValue(preset.y);
        this.feature.barThickness.setValue(preset.barThickness);
        this.feature.barRadius.setValue(preset.barRadius);
        this.feature.showSecondBar.setValue(preset.showSecondBar);
        this.feature.matchBarThickness.setValue(preset.matchBarThickness);
        this.feature.secondBarThickness.setValue(preset.secondBarThickness);
        this.feature.headSize.setValue(preset.headSize);
        this.feature.headYaw.setValue(preset.headYaw);
        this.feature.headPitch.setValue(preset.headPitch);
        this.feature.headAutoRotate.setValue(preset.headAutoRotate);
        this.feature.displayMode.setValue(isValidMode(this.feature.displayMode, preset.displayMode) ? preset.displayMode : DISPLAY_MODES[0]);
        this.feature.customColors.setValue(preset.customColors);
        this.feature.barColor.setValue(preset.barColor);
        this.feature.barColorSecond.setValue(preset.barColorSecond);
        this.feature.bgColor.setValue(preset.bgColor);
        this.feature.borderColor.setValue(preset.borderColor);
        this.feature.textColor.setValue(preset.textColor);
        this.feature.radius.setValue(preset.radius);
        this.feature.borderThickness.setValue(preset.borderThickness);
        this.feature.backgroundAlpha.setValue(preset.backgroundAlpha);
        this.feature.animationSpeed.setValue(preset.animationSpeed);
        this.feature.showArmor.setValue(preset.showArmor);
        this.feature.showPing.setValue(preset.showPing);
        this.feature.showEyes.setValue(preset.showEyes);
        this.feature.eyeSize.setValue(preset.eyeSize);
        this.feature.eyeColor.setValue(preset.eyeColor);
        this.feature.pupilColor.setValue(preset.pupilColor);
        this.feature.bgImage.setValue(preset.bgImage == null ? "" : preset.bgImage);
        this.feature.headImage.setValue(preset.headImage == null ? "" : preset.headImage);
        this.appliedPreset = preset;
        this.presetName = preset.name;
    }

    private boolean handleBottomBarClick(float mx, float my, float width, float height) {
        float y = height - BOTTOM_BAR_H - MARGIN;
        float x = MARGIN;
        float w = width - MARGIN * 2.0F;

        if (inside(mx, my, x + 10.0F, y + 12.0F, 180.0F, 24.0F)) {
            this.focusedField = "preset";
            return true;
        }
        this.focusedField = null;

        float bx = x + w - 96.0F;
        if (inside(mx, my, bx, y + 12.0F, 86.0F, 24.0F)) {
            close();
            return true;
        }
        bx -= 110.0F;
        if (inside(mx, my, bx, y + 12.0F, 100.0F, 24.0F)) {
            this.managerOpen = !this.managerOpen;
            this.presetsScroll = 0;
            return true;
        }
        bx -= 116.0F;
        if (inside(mx, my, bx, y + 12.0F, 106.0F, 24.0F)) {
            savePreset();
            return true;
        }
        return false;
    }

    private void savePreset() {
        String name = this.presetName.trim();
        if (name.isEmpty()) {
            this.presetError = "Введите название";
            return;
        }
        TargetHudPreset preset = new TargetHudPreset();
        preset.name = name;
        preset.type = this.feature.type.getValue();
        preset.x = this.feature.x.getValue();
        preset.y = this.feature.y.getValue();
        preset.barThickness = this.feature.barThickness.getValue();
        preset.barRadius = this.feature.barRadius.getValue();
        preset.showSecondBar = this.feature.showSecondBar.getValue();
        preset.matchBarThickness = this.feature.matchBarThickness.getValue();
        preset.secondBarThickness = this.feature.secondBarThickness.getValue();
        preset.headSize = this.feature.headSize.getValue();
        preset.headYaw = this.feature.headYaw.getValue();
        preset.headPitch = this.feature.headPitch.getValue();
        preset.headAutoRotate = this.feature.headAutoRotate.getValue();
        preset.displayMode = this.feature.displayMode.getValue();
        preset.customColors = this.feature.customColors.getValue();
        preset.barColor = this.feature.barColor.getValue();
        preset.barColorSecond = this.feature.barColorSecond.getValue();
        preset.bgColor = this.feature.bgColor.getValue();
        preset.borderColor = this.feature.borderColor.getValue();
        preset.textColor = this.feature.textColor.getValue();
        preset.radius = this.feature.radius.getValue();
        preset.borderThickness = this.feature.borderThickness.getValue();
        preset.backgroundAlpha = this.feature.backgroundAlpha.getValue();
        preset.animationSpeed = this.feature.animationSpeed.getValue();
        preset.showArmor = this.feature.showArmor.getValue();
        preset.showPing = this.feature.showPing.getValue();
        preset.showEyes = this.feature.showEyes.getValue();
        preset.eyeSize = this.feature.eyeSize.getValue();
        preset.eyeColor = this.feature.eyeColor.getValue();
        preset.pupilColor = this.feature.pupilColor.getValue();
        preset.bgImage = this.feature.bgImage.getValue();
        preset.headImage = this.feature.headImage.getValue();
        TargetHudPresetManager.savePreset(preset);
        this.appliedPreset = preset;
        this.presetError = "";
    }

    private boolean handleRailClick(float mx, float my, float width, float height) {
        float x = MARGIN;
        float y = TOP_Y;
        float w = RAIL_W;
        float h = height - BOTTOM_BAR_H - MARGIN - TOP_Y;
        if (!inside(mx, my, x, y, w, h)) {
            return false;
        }
        float rowH = 27.0F;
        float py = y + 10.0F + 16.0F;
        for (int i = 0; i < VIEWS.length; i++) {
            float ry = py + i * (rowH + 4.0F);
            if (inside(mx, my, x + 5.0F, ry, w - 10.0F, rowH)) {
                this.activeView = i;
                float[] pos = viewPosition(i, width, height, this.feature.currentWidth(), this.feature.currentHeight(), height - BOTTOM_BAR_H - MARGIN);
                this.feature.x.setValue((double) pos[0]);
                this.feature.y.setValue((double) pos[1]);
                return true;
            }
        }
        float modeY = y + h - 40.0F;
        for (int i = 0; i < 2; i++) {
            float bx = x + 8.0F + i * ((w - 16.0F) / 2.0F);
            if (inside(mx, my, bx, modeY + 15.0F, (w - 16.0F) / 2.0F - 2.0F, 22.0F)) {
                this.feature.mode.setValue(MODES[i]);
                return true;
            }
        }
        return false;
    }

    private boolean handlePresetsClick(float mx, float my, float panelX, float panelY, float panelH) {
        float contentTop = panelY + TAB_H + 6.0F;
        float contentH = panelH - TAB_H - 12.0F;
        float rowH = 44.0F;
        float gap = 6.0F;
        int visible = Math.max(1, (int) (contentH / (rowH + gap)));
        float contentW = SETTINGS_W - 16.0F;
        float cardX = panelX + 8.0F;

        if (my < contentTop || my > contentTop + contentH) {
            return false;
        }
        List<TargetHudPreset> templates = TargetHudPresetManager.getTemplates();
        List<TargetHudPreset> presets = TargetHudPresetManager.getPresets();
        int total = templates.size() + (presets.isEmpty() ? 0 : 1 + presets.size());
        if (total == 0) {
            return false;
        }
        int mineHeaderAt = templates.size();
        this.presetsScroll = MathUtil.clamp(this.presetsScroll, 0, Math.max(0, total - visible));
        int slot = this.presetsScroll + (int) ((my - contentTop) / (rowH + gap));
        if (slot < 0 || slot >= total) {
            return false;
        }

        if (slot < templates.size()) {
            TargetHudPreset template = templates.get(slot);
            if (inside(mx, my, cardX + contentW - 148.0F, contentTop + (slot - this.presetsScroll) * (rowH + gap) + 10.0F, 66.0F, 24.0F)) {
                copyTemplateToMine(template);
                return true;
            }
            if (inside(mx, my, cardX + contentW - 76.0F, contentTop + (slot - this.presetsScroll) * (rowH + gap) + 10.0F, 66.0F, 24.0F)) {
                applyPreset(template);
                snapIntoView(Minecraft.getInstance());
                return true;
            }
            if (inside(mx, my, cardX, contentTop + (slot - this.presetsScroll) * (rowH + gap), contentW, rowH)) {
                applyPreset(template);
                snapIntoView(Minecraft.getInstance());
                return true;
            }
            return false;
        }

        int mineSlot = slot - mineHeaderAt;
        if (mineSlot <= 0) {
            return false;
        }
        TargetHudPreset preset = presets.get(mineSlot - 1);
        if (inside(mx, my, cardX + contentW - 148.0F, contentTop + (slot - this.presetsScroll) * (rowH + gap) + 10.0F, 66.0F, 24.0F)) {
            TargetHudPresetManager.deletePreset(preset.name);
            if (isAppliedPreset(preset)) {
                this.appliedPreset = null;
                this.presetName = "Таргетхуд";
            }
            return true;
        }
        if (inside(mx, my, cardX + contentW - 76.0F, contentTop + (slot - this.presetsScroll) * (rowH + gap) + 10.0F, 66.0F, 24.0F)) {
            applyPreset(preset);
            snapIntoView(Minecraft.getInstance());
            return true;
        }
        if (inside(mx, my, cardX, contentTop + (slot - this.presetsScroll) * (rowH + gap), contentW, rowH)) {
            applyPreset(preset);
            snapIntoView(Minecraft.getInstance());
            return true;
        }
        return false;
    }

    private void copyTemplateToMine(TargetHudPreset template) {
        TargetHudPreset copy = new TargetHudPreset();
        copy.name = template.name;
        copy.type = template.type;
        copy.x = template.x;
        copy.y = template.y;
        copy.barThickness = template.barThickness;
        copy.barRadius = template.barRadius;
        copy.showSecondBar = template.showSecondBar;
        copy.matchBarThickness = template.matchBarThickness;
        copy.secondBarThickness = template.secondBarThickness;
        copy.headSize = template.headSize;
        copy.headYaw = template.headYaw;
        copy.headPitch = template.headPitch;
        copy.headAutoRotate = template.headAutoRotate;
        copy.displayMode = template.displayMode;
        copy.customColors = template.customColors;
        copy.barColor = template.barColor;
        copy.barColorSecond = template.barColorSecond;
        copy.bgColor = template.bgColor;
        copy.borderColor = template.borderColor;
        copy.textColor = template.textColor;
        copy.radius = template.radius;
        copy.borderThickness = template.borderThickness;
        copy.backgroundAlpha = template.backgroundAlpha;
        copy.animationSpeed = template.animationSpeed;
        copy.showArmor = template.showArmor;
        copy.showPing = template.showPing;
        copy.showEyes = template.showEyes;
        copy.eyeSize = template.eyeSize;
        copy.eyeColor = template.eyeColor;
        copy.pupilColor = template.pupilColor;
        copy.bgImage = template.bgImage;
        copy.headImage = template.headImage;

        String base = template.name == null ? "Таргетхуд" : template.name;
        String name = base;
        for (int suffix = 1; TargetHudPresetManager.findByName(name) != null; suffix++) {
            name = base + " " + suffix;
        }
        copy.name = name;

        TargetHudPresetManager.savePreset(copy);
        applyPreset(copy);
        snapIntoView(Minecraft.getInstance());
    }

    private boolean isAppliedPreset(TargetHudPreset preset) {
        return this.appliedPreset != null && this.appliedPreset.name.equalsIgnoreCase(preset.name);
    }

    private boolean isValidMode(ModeSetting setting, String value) {
        return value != null && setting.containsMode(value);
    }

    private boolean handleSettingsClick(float mx, float my, float width, float height) {
        float panelX = width - SETTINGS_W - MARGIN;
        float panelY = TOP_Y;
        float panelH = height - BOTTOM_BAR_H - MARGIN - TOP_Y;
        if (!inside(mx, my, panelX, panelY, SETTINGS_W, panelH)) {
            return false;
        }

        float tabW = SETTINGS_W / TABS.length;
        if (my < panelY + TAB_H) {
            for (int i = 0; i < TABS.length; i++) {
                if (inside(mx, my, panelX + i * tabW, panelY, tabW, TAB_H)) {
                    this.activeTab = i;
                    this.dragSliderRow = -1;
                    return true;
                }
            }
        }

        if (this.activeTab == TAB_PRESETS) {
            return handlePresetsClick(mx, my, panelX, panelY, panelH);
        }

        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            if (row.y < 0.0F || !inside(mx, my, panelX, row.y, SETTINGS_W - 4.0F, row.h)) {
                continue;
            }
            float cx = row.controlX;
            switch (row.type) {
                case ROW_TYPE_TOGGLE -> {
                    BooleanSetting setting = (BooleanSetting) row.target;
                    setting.setValue(!setting.getValue());
                    return true;
                }
                case ROW_TYPE_MODE -> {
                    cycleMode(this.feature.displayMode, DISPLAY_MODES);
                    return true;
                }
                case ROW_TYPE_MODE_OWN -> {
                    cycleMode(this.feature.type, TYPES);
                    return true;
                }
                case ROW_TYPE_NUMBER -> {
                    this.dragging = true;
                    this.dragSliderRow = i;
                    this.dragSliderKey = ((NumberSetting) row.target).getName();
                    updateSliderFromMouse((NumberSetting) row.target, mx);
                    return true;
                }
                case ROW_TYPE_COLOR -> {
                    ColorSetting setting = (ColorSetting) row.target;
                    int index = indexOfColor(setting);
                    this.feature.customColors.setValue(true);
                    initPicker(index);
                    return true;
                }
                case ROW_TYPE_TEXT -> {
                    this.focusedField = focusKey(row.target);
                    return true;
                }
            }
        }
        return false;
    }

    private int indexOfColor(ColorSetting setting) {
        if (setting == this.feature.barColor) {
            return 0;
        }
        if (setting == this.feature.barColorSecond) {
            return 1;
        }
        if (setting == this.feature.bgColor) {
            return 2;
        }
        if (setting == this.feature.borderColor) {
            return 3;
        }
        return 4;
    }

    private void cycleMode(ModeSetting setting, String[] values) {
        String current = setting.getValue();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                setting.setValue(values[(i + 1) % values.length]);
                return;
            }
        }
        setting.setValue(values[0]);
    }

    private void updateSliderFromMouse(NumberSetting setting, float mouseX) {
        if (this.rows.isEmpty()) {
            return;
        }
        if (this.dragSliderRow < 0 || this.dragSliderRow >= this.rows.size()) {
            return;
        }
        Row row = this.rows.get(this.dragSliderRow);
        float trackX = row.controlX - 110.0F;
        float progress = MathUtil.clamp01((mouseX - trackX) / 110.0F);
        setNumberFromProgress(setting, progress);
    }

    private void setNumberFromProgress(NumberSetting setting, float progress) {
        double min = setting.getMin();
        double max = setting.getMax();
        double step = setting.getStep();
        double raw = min + (max - min) * progress;
        double snapped = Math.round(raw / step) * step;
        snapped = Math.max(min, Math.min(max, snapped));
        setting.setValue(snapped);
    }

    @EventTarget(priority = 1_500)
    public void onCharacterInput(CharacterInputEvent event) {
        if (!this.open) {
            return;
        }
        String focus = this.focusedField;
        if (focus == null) {
            return;
        }
        String current = currentFieldValue(focus);
        if (current == null) {
            return;
        }
        char c = (char) event.getCodePoint();
        if (Character.isWhitespace(c)) {
            event.cancel();
            return;
        }
        boolean hexField = "pickerHex".equals(focus);
        if (hexField && Character.digit(c, 16) < 0) {
            event.cancel();
            return;
        }
        int maxLen = hexField ? 6 : 256;
        String next = current + (hexField ? Character.toUpperCase(c) : c);
        next = next.length() > maxLen ? current : next;
        setFieldValue(focus, next);
        event.cancel();
    }

    private String currentFieldValue(String key) {
        if ("preset".equals(key)) {
            return this.presetName;
        }
        if ("pickerHex".equals(key)) {
            return this.pickerHex;
        }
        if ("bgImage".equals(key)) {
            return this.feature.bgImage.getValue();
        }
        if ("headImage".equals(key)) {
            return this.feature.headImage.getValue();
        }
        return null;
    }

    private void setFieldValue(String key, String value) {
        if ("preset".equals(key)) {
            this.presetName = value;
        } else if ("pickerHex".equals(key)) {
            this.pickerHex = value;
            if (value.length() == 6) {
                try {
                    int rgb = Integer.parseInt(value, 16);
                    int color = 0xFF000000 | rgb;
                    pickerSetting().setValue(color);
                    float[] hsb = ColorUtil.hsv(color);
                    this.pickerHue = hsb[0];
                    this.pickerSat = hsb[1];
                    this.pickerBright = hsb[2];
                } catch (NumberFormatException ignored) {
                }
            }
        } else if ("bgImage".equals(key)) {
            this.feature.bgImage.setValue(value);
        } else if ("headImage".equals(key)) {
            this.feature.headImage.setValue(value);
        }
    }

    @EventTarget(priority = 1_500)
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (!this.open) {
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) {
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE) {
            this.pickerColor = -1;
            if (this.managerOpen) {
                this.managerOpen = false;
            } else {
                close();
            }
            this.focusedField = null;
            event.cancel();
            return;
        }
        if (this.focusedField != null) {
            if (event.getKey() == GLFW.GLFW_KEY_BACKSPACE) {
                String current = currentFieldValue(this.focusedField);
                if (current != null && !current.isEmpty()) {
                    setFieldValue(this.focusedField, current.substring(0, current.length() - 1));
                }
                event.cancel();
                return;
            }
            if (event.getKey() == GLFW.GLFW_KEY_ENTER) {
                this.focusedField = null;
                event.cancel();
                return;
            }
        }
        event.cancel();
    }

    private void updateDragSlider(Minecraft mc) {
        if (!this.open || !this.dragging || this.dragSliderRow < 0) {
            return;
        }
        if (this.dragSliderRow >= this.rows.size()) {
            return;
        }
        Row row = this.rows.get(this.dragSliderRow);
        if (row.type != ROW_TYPE_NUMBER || !(row.target instanceof NumberSetting setting)) {
            return;
        }
        float mouseX = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        updateSliderFromMouse(setting, mouseX);
    }

    private boolean tryStartPreviewDrag(float mx, float my, float width, float height) {
        float w = this.feature.currentWidth();
        float h = this.feature.currentHeight();
        if (w <= 0.0F || h <= 0.0F) {
            return false;
        }
        float px = this.feature.x.getValue().floatValue();
        float py = this.feature.y.getValue().floatValue();
        if (!inside(mx, my, px, py, w, h)) {
            return false;
        }
        this.previewDrag = true;
        this.previewDragOffX = this.feature.x.getValue() - mx;
        this.previewDragOffY = this.feature.y.getValue() - my;
        return true;
    }

    private void updatePreviewDrag(Minecraft mc) {
        if (!this.open || !this.previewDrag) {
            return;
        }
        float w = this.feature.currentWidth();
        float h = this.feature.currentHeight();
        if (w <= 0.0F || h <= 0.0F) {
            return;
        }
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        float mx = (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
        float my = (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
        float nx = MathUtil.clamp((float) (mx + this.previewDragOffX), 0.0F, Math.max(0.0F, width - w));
        float ny = MathUtil.clamp((float) (my + this.previewDragOffY), 0.0F, Math.max(0.0F, height - h));
        this.feature.x.setValue((double) nx);
        this.feature.y.setValue((double) ny);
    }

    private void snapIntoView(Minecraft mc) {
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        float w = this.feature.currentWidth();
        float h = this.feature.currentHeight();
        if (w <= 0.0F || h <= 0.0F) {
            return;
        }
        float settingsBottom = height - BOTTOM_BAR_H - MARGIN;
        float panelX = width - SETTINGS_W - MARGIN;
        float px = this.feature.x.getValue().floatValue();
        float py = this.feature.y.getValue().floatValue();
        boolean offScreen = px < -w / 2.0F || px > width || py < -h / 2.0F || py > height;
        boolean overlapsPanel = px + w > panelX + 4.0F && py + h > TOP_Y;
        if (offScreen || overlapsPanel) {
            float[] pos = viewPosition(4, width, height, w, h, settingsBottom);
            this.feature.x.setValue((double) pos[0]);
            this.feature.y.setValue((double) pos[1]);
        }
    }

    private static float[] viewPosition(int view, float width, float height, float w, float h, float settingsBottom) {
        float px;
        float py;
        switch (view) {
            case 0 -> {
                px = MARGIN;
                py = settingsBottom * 0.03F;
            }
            case 1 -> {
                px = width / 2.0F - w / 2.0F;
                py = settingsBottom * 0.03F;
            }
            case 2 -> {
                px = width - SETTINGS_W - MARGIN * 2.0F - w;
                py = settingsBottom * 0.03F;
            }
            case 3 -> {
                px = MARGIN;
                py = (settingsBottom * 0.9F) / 2.0F - h / 2.0F;
            }
            case 4 -> {
                px = width / 2.0F - w / 2.0F - SETTINGS_W / 2.0F;
                py = (settingsBottom * 0.9F) / 2.0F - h / 2.0F;
            }
            default -> {
                px = width - SETTINGS_W - MARGIN * 2.0F - w;
                py = (settingsBottom * 0.9F) / 2.0F - h / 2.0F;
            }
        }
        px = MathUtil.clamp(px, 0.0F, Math.max(0.0F, width - w));
        py = MathUtil.clamp(py, 0.0F, Math.max(0.0F, height - h));
        return new float[]{px, py};
    }

    private static boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }
}