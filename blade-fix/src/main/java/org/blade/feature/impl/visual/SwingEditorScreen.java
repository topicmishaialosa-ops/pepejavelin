package org.blade.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.blade.feature.setting.NumberSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SwingEditorScreen extends Screen {
    private static final int EXIT_WIDTH = 50;
    private static final int EXIT_HEIGHT = 16;
    private static final int EXIT_OFFSET = 10;
    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_OFFSET = 10;
    private static final int PANEL_PADDING = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int SECTION_PADDING = 14;
    private static final int BAR_HEIGHT = 28;
    private static final int BAR_OFFSET = 20;
    private static final float[] SPEEDS = {0.5F, 1.0F, 2.0F};
    private static final String[] SPEED_LABELS = {"0.5x", "1x", "2x"};

    private final SwingAnimationFeature module;
    private final boolean wasEnabled;
    private final List<Row> rows = new ArrayList<>();
    private int draggingRow = -1;
    private boolean draggingTimeline;
    private boolean playing = true;
    private float speed = 1.0F;
    private float preview;
    private int direction = 1;
    private long lastFrame;

    public SwingEditorScreen() {
        super(Component.literal("Редактор анимации удара"));
        this.module = SwingAnimationFeature.INSTANCE;
        this.wasEnabled = this.module.isEnabled();
        if (!this.wasEnabled) {
            this.module.setEnabled(true);
        }
        this.module.animationMode.setValue("Custom");
        this.module.setPreviewProgress(0.0F);
        this.lastFrame = System.currentTimeMillis();
        this.buildRows();
    }

    public static void openEditor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof SwingEditorScreen) {
            return;
        }
        mc.gui.setScreen(new SwingEditorScreen());
    }

    private void buildRows() {
        this.rows.clear();
        this.addSection("Угол старта");
        this.addSlider("Старт X", this.module.customStartX);
        this.addSlider("Старт Y", this.module.customStartY);
        this.addSlider("Старт Z", this.module.customStartZ);
        this.addSection("Угол конца");
        this.addSlider("Конец X", this.module.customEndX);
        this.addSlider("Конец Y", this.module.customEndY);
        this.addSlider("Конец Z", this.module.customEndZ);
        this.addSection("Позиция");
        this.addSlider("Позиция X", this.module.customPosX);
        this.addSlider("Позиция Y", this.module.customPosY);
        this.addSlider("Позиция Z", this.module.customPosZ);
        this.addSection("Масштаб");
        this.addSlider("Масштаб", this.module.customScale);
    }

    private void addSection(String title) {
        this.rows.add(new Row(title));
    }

    private void addSlider(String label, NumberSetting setting) {
        this.rows.add(new Row(label, setting));
    }

    private int panelX() {
        return this.width - PANEL_WIDTH - PANEL_OFFSET;
    }

    private int panelY() {
        return 30;
    }

    private int panelBoxHeight() {
        return Math.max(300, this.height - this.panelY() - BAR_HEIGHT - BAR_OFFSET * 2);
    }

    private int barY() {
        return this.height - BAR_HEIGHT - BAR_OFFSET;
    }

    private int barTrackX() {
        return BAR_OFFSET + 46;
    }

    private int barTrackW() {
        return this.width - BAR_OFFSET * 2 - 46 - 190;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        this.updatePreview();
        if (this.draggingRow >= 0 && this.draggingRow < this.rows.size()) {
            Row row = this.rows.get(this.draggingRow);
            if (row.setting != null) {
                this.updateSliderValue(row.setting, mouseX, row);
            }
        }
        if (this.draggingTimeline) {
            this.setPreviewFromX(mouseX);
        }

        gfx.fill(0, 0, this.width, this.height, -1610612736);

        this.renderExit(gfx, mouseX, mouseY);
        this.renderHeader(gfx);
        this.renderPanel(gfx, mouseX, mouseY);
        this.renderPlaybackBar(gfx, mouseX, mouseY);
    }

    private void renderExit(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        int accent = this.accent(110);
        int exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
        int exitY = EXIT_OFFSET;
        boolean hovered = this.isInside(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT);
        gfx.fill(exitX, exitY, exitX + EXIT_WIDTH, exitY + EXIT_HEIGHT, hovered ? accent : 0xB40F0F0F);
        gfx.centeredText(this.font, Component.literal("Выход"), exitX + EXIT_WIDTH / 2, exitY + 4, 0xFFDEDEDE);
    }

    private void renderHeader(GuiGraphicsExtractor gfx) {
        gfx.text(this.font, Component.literal("Редактор анимации удара"), EXIT_OFFSET, EXIT_OFFSET, 0xFFDEDEDE);
        gfx.text(this.font, Component.literal("Крути полосу внизу, лови позу и жми «В старт»/«В конец» — так анимация собирается за секунды"), EXIT_OFFSET, EXIT_OFFSET + 12, 0xFF999999);
    }

    private void updatePreview() {
        if (!this.playing || this.draggingTimeline) {
            return;
        }
        long now = System.currentTimeMillis();
        float delta = (float) (now - this.lastFrame) / 1000.0F;
        this.lastFrame = now;
        if (delta <= 0.0F) {
            return;
        }
        this.preview += delta * this.speed * 0.4F * this.direction;
        if (this.preview >= 1.0F) {
            this.preview = 1.0F;
            this.direction = -1;
        }
        if (this.preview <= 0.0F) {
            this.preview = 0.0F;
            this.direction = 1;
        }
        this.module.setPreviewProgress(this.preview);
    }

    private float curveAt(float t) {
        return this.module.customCurve.is("Плавная") ? t * t * (3.0F - 2.0F * t) : t;
    }

    private float interp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    private float poseX(float t) {
        return this.interp(this.module.customStartX.getValue().floatValue(), this.module.customEndX.getValue().floatValue(), t);
    }

    private float poseY(float t) {
        return this.interp(this.module.customStartY.getValue().floatValue(), this.module.customEndY.getValue().floatValue(), t);
    }

    private float poseZ(float t) {
        return this.interp(this.module.customStartZ.getValue().floatValue(), this.module.customEndZ.getValue().floatValue(), t);
    }

    private void captureToStart() {
        float t = this.curveAt(this.preview);
        this.setRounded(this.module.customStartX, this.poseX(t));
        this.setRounded(this.module.customStartY, this.poseY(t));
        this.setRounded(this.module.customStartZ, this.poseZ(t));
    }

    private void captureToEnd() {
        float t = this.curveAt(this.preview);
        this.setRounded(this.module.customEndX, this.poseX(t));
        this.setRounded(this.module.customEndY, this.poseY(t));
        this.setRounded(this.module.customEndZ, this.poseZ(t));
    }

    private void setRounded(NumberSetting setting, float value) {
        setting.setValue(Mth.clamp((double) value, setting.getMin(), setting.getMax()));
    }

    private void renderPanel(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        int x = this.panelX();
        int y = this.panelY();
        int boxH = this.panelBoxHeight();
        int accent = this.accent(220);

        gfx.fill(x, y, x + PANEL_WIDTH, y + boxH, 0xD70F0F0F);
        gfx.fill(x, y, x + PANEL_WIDTH, y + 1, Theme.Colors.DOCK_BORDER);
        gfx.fill(x, y, x + 1, y + boxH, Theme.Colors.DOCK_BORDER);
        gfx.fill(x + PANEL_WIDTH - 1, y, x + PANEL_WIDTH, y + boxH, Theme.Colors.DOCK_BORDER);
        gfx.fill(x, y + boxH - 1, x + PANEL_WIDTH, y + boxH, Theme.Colors.DOCK_BORDER);
        gfx.text(this.font, Component.literal("Параметры удара"), x + PANEL_PADDING, y + 10, 0xFFDEDEDE);

        int rowY = y + 28;
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            if (row.setting == null) {
                gfx.text(this.font, Component.literal(row.label), x + PANEL_PADDING, rowY + 4, this.accent(230));
                rowY += SECTION_PADDING;
                continue;
            }
            int trackWidth = PANEL_WIDTH - PANEL_PADDING * 2;
            String value = this.formatValue(row.setting);
            gfx.text(this.font, Component.literal(row.label), x + PANEL_PADDING, rowY + 3, 0xFFC8C8C8);
            gfx.text(this.font, Component.literal(value), x + PANEL_PADDING + trackWidth - this.font.width(value), rowY + 3, 0xFF999999);

            int trackY = rowY + 11;
            int anim = this.sliderAnim(row, trackWidth);
            boolean hovered = this.isInside(mouseX, mouseY, x + PANEL_PADDING, trackY, trackWidth, 2);
            gfx.fill(x + PANEL_PADDING, trackY, x + PANEL_PADDING + trackWidth, trackY + 2, 0x78373737);
            gfx.fill(x + PANEL_PADDING, trackY, x + PANEL_PADDING + anim, trackY + 2, accent);
            int knobColor = this.draggingRow == i ? this.accent(255) : hovered ? this.accent(200) : this.accent(160);
            gfx.fill(x + PANEL_PADDING + anim - 4, trackY - 4, x + PANEL_PADDING + anim + 4, trackY + 6, knobColor);
            rowY += ROW_HEIGHT;
        }

        int curveY = rowY + 2;
        gfx.text(this.font, Component.literal("Кривая"), x + PANEL_PADDING, curveY, 0xFFC8C8C8);
        int buttonWidth = (PANEL_WIDTH - PANEL_PADDING * 2 - 6) / 2;
        this.renderCurveButton(gfx, mouseX, mouseY, x + PANEL_PADDING, curveY + 12, buttonWidth, "Линейная", this.module.customCurve.is("Линейная"));
        this.renderCurveButton(gfx, mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6, curveY + 12, buttonWidth, "Плавная", this.module.customCurve.is("Плавная"));

        int captureY = curveY + 36;
        gfx.text(this.font, Component.literal("Захватить позу"), x + PANEL_PADDING, captureY, 0xFFC8C8C8);
        this.renderCaptureButton(gfx, mouseX, mouseY, x + PANEL_PADDING, captureY + 12, buttonWidth, "В старт");
        this.renderCaptureButton(gfx, mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6, captureY + 12, buttonWidth, "В конец");

        int resetY = captureY + 36;
        boolean resetHovered = this.isInside(mouseX, mouseY, x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2, 16);
        gfx.fill(x + PANEL_PADDING, resetY, x + PANEL_PADDING + PANEL_WIDTH - PANEL_PADDING * 2, resetY + 16, resetHovered ? this.accent(110) : 0xFF282828);
        gfx.centeredText(this.font, Component.literal("Сбросить"), x + PANEL_WIDTH / 2, resetY + 5, 0xFFDEDEDE);
    }

    private void renderCurveButton(GuiGraphicsExtractor gfx, int mouseX, int mouseY, int x, int y, int width, String label, boolean selected) {
        boolean hovered = this.isInside(mouseX, mouseY, x, y, width, 14);
        int fill = selected ? (hovered ? this.accent(210) : this.accent(180)) : hovered ? 0xFF3A3A3A : 0xFF282828;
        gfx.fill(x, y, x + width, y + 14, fill);
        gfx.centeredText(this.font, Component.literal(label), x + width / 2, y + 4, 0xFFDEDEDE);
    }

    private void renderCaptureButton(GuiGraphicsExtractor gfx, int mouseX, int mouseY, int x, int y, int width, String label) {
        boolean hovered = this.isInside(mouseX, mouseY, x, y, width, 14);
        gfx.fill(x, y, x + width, y + 14, hovered ? this.accent(110) : 0xFF282828);
        gfx.centeredText(this.font, Component.literal(label), x + width / 2, y + 4, 0xFFDEDEDE);
    }

    private void renderPlaybackBar(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        int y = this.barY();
        int x = BAR_OFFSET;
        int w = this.width - BAR_OFFSET * 2;
        int accent = this.accent(220);

        gfx.fill(x, y, x + w, y + BAR_HEIGHT, 0xD70F0F0F);
        gfx.fill(x, y, x + w, y + 1, Theme.Colors.DOCK_BORDER);
        gfx.fill(x, y, x + 1, y + BAR_HEIGHT, Theme.Colors.DOCK_BORDER);
        gfx.fill(x + w - 1, y, x + w, y + BAR_HEIGHT, Theme.Colors.DOCK_BORDER);
        gfx.fill(x, y + BAR_HEIGHT - 1, x + w, y + BAR_HEIGHT, Theme.Colors.DOCK_BORDER);

        int btnX = x + 14;
        int btnY = y + (BAR_HEIGHT - 16) / 2;
        boolean btnHovered = this.isInside(mouseX, mouseY, btnX - 4, btnY - 4, 24, 24);
        gfx.fill(btnX - 4, btnY - 4, btnX + 20, btnY + 20, btnHovered ? accent : 0xFF282828);
        if (this.playing) {
            this.drawPauseIcon(gfx, btnX + 3, btnY + 3, this.accent(255));
        } else {
            this.drawPlayIcon(gfx, btnX + 4, btnY + 1, this.accent(255));
        }

        int trackX = this.barTrackX();
        int trackY = y + (BAR_HEIGHT - 4) / 2;
        int trackW = this.barTrackW();
        boolean trackHovered = this.isInside(mouseX, mouseY, trackX, trackY - 5, trackW, 14);
        gfx.fill(trackX, trackY, trackX + trackW, trackY + 4, 0x78373737);
        int fill = (int) (trackW * this.preview);
        gfx.fill(trackX, trackY, trackX + fill, trackY + 4, accent);
        int knobX = trackX + fill;
        gfx.fill(knobX - 3, trackY - 3, knobX + 3, trackY + 7, (this.draggingTimeline || trackHovered) ? this.accent(255) : this.accent(180));

        int percentTextX = trackX + trackW + 12;
        String percent = String.format(Locale.US, "%d%%", (int) Math.round(this.preview * 100.0D));
        gfx.text(this.font, Component.literal(percent), percentTextX, y + (BAR_HEIGHT - 6) / 2, 0xFFC8C8C8);

        int speedX = this.width - BAR_OFFSET - 138;
        for (int i = 0; i < SPEED_LABELS.length; i++) {
            int sX = speedX + i * 46;
            boolean hovered = this.isInside(mouseX, mouseY, sX, y + (BAR_HEIGHT - 20) / 2, 42, 20);
            boolean selected = this.speed == SPEEDS[i];
            gfx.fill(sX, y + (BAR_HEIGHT - 20) / 2, sX + 42, y + (BAR_HEIGHT - 20) / 2 + 20, selected ? (hovered ? this.accent(210) : this.accent(180)) : hovered ? 0xFF3A3A3A : 0xFF282828);
            gfx.centeredText(this.font, Component.literal(SPEED_LABELS[i]), sX + 21, y + (BAR_HEIGHT - 6) / 2, 0xFFDEDEDE);
        }
    }

    private void drawPlayIcon(GuiGraphicsExtractor gfx, int x, int y, int color) {
        int height = 14;
        for (int i = 0; i < 7; i++) {
            int rowY = y + i * 2;
            int rowWidth = 3 + i * 2;
            gfx.fill(x, rowY, x + rowWidth, rowY + 2, color);
        }
    }

    private void drawPauseIcon(GuiGraphicsExtractor gfx, int x, int y, int color) {
        gfx.fill(x, y, x + 3, y + 10, color);
        gfx.fill(x + 7, y, x + 10, y + 10, color);
    }

    private int sliderAnim(Row row, int trackWidth) {
        NumberSetting setting = row.setting;
        double range = setting.getMax() - setting.getMin();
        if (range <= 0.0) {
            return 0;
        }
        return (int) (trackWidth * (setting.getValue() - setting.getMin()) / range);
    }

    private void setPreviewFromX(float mouseX) {
        float trackX = this.barTrackX();
        float trackW = Math.max(1, this.barTrackW());
        float percent = Mth.clamp((mouseX - trackX) / trackW, 0.0F, 1.0F);
        this.preview = percent;
        this.module.setPreviewProgress(percent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
        if (event.button() != 0) {
            return super.mouseClicked(event, flag);
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int exitX = this.width - EXIT_OFFSET - EXIT_WIDTH;
        int exitY = EXIT_OFFSET;
        if (this.isInside(mouseX, mouseY, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT)) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        if (mouseX >= this.panelX() && mouseX <= this.panelX() + PANEL_WIDTH && mouseY >= this.panelY() && mouseY <= this.panelY() + this.panelBoxHeight()) {
            this.handlePanelClick(mouseX, mouseY);
            return true;
        }
        if (this.isInside(mouseX, mouseY, BAR_OFFSET, this.barY(), this.width - BAR_OFFSET * 2, BAR_HEIGHT)) {
            this.handleBarClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(event, flag);
    }

    private void handlePanelClick(int mouseX, int mouseY) {
        int x = this.panelX();
        int y = this.panelY();
        int rowY = y + 28;
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            if (row.setting == null) {
                rowY += SECTION_PADDING;
                continue;
            }
            int trackWidth = PANEL_WIDTH - PANEL_PADDING * 2;
            int trackY = rowY + 11;
            if (this.isInside(mouseX, mouseY, x + PANEL_PADDING, trackY, trackWidth, 2)) {
                this.draggingRow = i;
                this.updateSliderValue(row.setting, mouseX, row);
                return;
            }
            rowY += ROW_HEIGHT;
        }

        int curveY = rowY + 2;
        int buttonWidth = (PANEL_WIDTH - PANEL_PADDING * 2 - 6) / 2;
        if (this.isInside(mouseX, mouseY, x + PANEL_PADDING, curveY + 12, buttonWidth, 14)) {
            this.module.customCurve.setValue("Линейная");
            return;
        }
        if (this.isInside(mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6, curveY + 12, buttonWidth, 14)) {
            this.module.customCurve.setValue("Плавная");
            return;
        }

        int captureY = curveY + 36;
        if (this.isInside(mouseX, mouseY, x + PANEL_PADDING, captureY + 12, buttonWidth, 14)) {
            this.captureToStart();
            return;
        }
        if (this.isInside(mouseX, mouseY, x + PANEL_PADDING + buttonWidth + 6, captureY + 12, buttonWidth, 14)) {
            this.captureToEnd();
            return;
        }

        int resetY = captureY + 36;
        if (this.isInside(mouseX, mouseY, x + PANEL_PADDING, resetY, PANEL_WIDTH - PANEL_PADDING * 2, 16)) {
            this.resetAll();
        }
    }

    private void handleBarClick(int mouseX, int mouseY) {
        int x = BAR_OFFSET;
        int y = this.barY();

        int btnX = x + 14 - 4;
        int btnY = y + (BAR_HEIGHT - 16) / 2 - 4;
        if (this.isInside(mouseX, mouseY, btnX, btnY, 24, 24)) {
            this.togglePlay();
            return;
        }

        int trackX = this.barTrackX();
        int trackW = Math.max(1, this.barTrackW());
        if (this.isInside(mouseX, mouseY, trackX, this.trackYForHit(), trackW, 14)) {
            this.draggingTimeline = true;
            this.setPreviewFromX(mouseX);
            return;
        }

        int speedX = this.width - BAR_OFFSET - 138;
        for (int i = 0; i < SPEED_LABELS.length; i++) {
            int sX = speedX + i * 46;
            if (this.isInside(mouseX, mouseY, sX, y + (BAR_HEIGHT - 20) / 2, 42, 20)) {
                this.speed = SPEEDS[i];
                return;
            }
        }
    }

    private int trackYForHit() {
        return this.barY() + (BAR_HEIGHT - 4) / 2 - 5;
    }

    private void togglePlay() {
        this.playing = !this.playing;
        this.lastFrame = System.currentTimeMillis();
    }

    private void updateSliderValue(NumberSetting setting, float mouseX, Row row) {
        float x = this.panelX() + PANEL_PADDING;
        float trackWidth = PANEL_WIDTH - PANEL_PADDING * 2;
        float percent = Mth.clamp((mouseX - x) / trackWidth, 0.0F, 1.0F);
        double range = setting.getMax() - setting.getMin();
        setting.setValue(setting.getMin() + percent * range);
    }

    private void resetAll() {
        this.module.customStartX.setValue(0.0D);
        this.module.customStartY.setValue(0.0D);
        this.module.customStartZ.setValue(0.0D);
        this.module.customEndX.setValue(-90.0D);
        this.module.customEndY.setValue(0.0D);
        this.module.customEndZ.setValue(0.0D);
        this.module.customPosX.setValue(0.0D);
        this.module.customPosY.setValue(0.0D);
        this.module.customPosZ.setValue(0.0D);
        this.module.customScale.setValue(1.0D);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.draggingRow = -1;
            this.draggingTimeline = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 32) {
            this.togglePlay();
            return true;
        }
        if (event.key() == 263) {
            this.setPreviewFromX(this.barTrackX() + this.barTrackW() * (this.preview - 0.05F));
            return true;
        }
        if (event.key() == 262) {
            this.setPreviewFromX(this.barTrackX() + this.barTrackW() * (this.preview + 0.05F));
            return true;
        }
        if (event.key() == 268) {
            this.setPreviewFromX(this.barTrackX());
            return true;
        }
        if (event.key() == 269) {
            this.setPreviewFromX(this.barTrackX() + this.barTrackW());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.module.stopPreview();
        if (!this.wasEnabled) {
            this.module.setEnabled(false);
        }
        super.onClose();
    }

    private int accent(int alpha) {
        return ColorUtil.withAlpha(Theme.getAccent(), alpha);
    }

    private String formatValue(NumberSetting setting) {
        double step = setting.getStep();
        String stepStr = String.valueOf(step);
        int decimals = stepStr.contains(".") ? stepStr.split("\\.")[1].length() : 0;
        String format = "%." + decimals + "f";
        String formatted = String.format(Locale.US, format, setting.getValue());
        return formatted.replaceAll("\\.?0+$", "");
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Row {
        private final String label;
        private final NumberSetting setting;

        private Row(String label, NumberSetting setting) {
            this.label = label;
            this.setting = setting;
        }

        private Row(String label) {
            this.label = label;
            this.setting = null;
        }
    }
}