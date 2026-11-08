package org.blade.hud;

import net.minecraft.client.Minecraft;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.pve.AutoMineFeature;
import org.blade.feature.impl.pve.PveManagerFeature;
import org.blade.menu.i18n.MenuText;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Textures;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.List;

public final class PveStatusElement extends HudElement {
    private static final float PADDING = 8.0F;
    private static final float ICON_SIZE = 14.0F;
    private static final float TITLE_SIZE = 11.0F;
    private static final float ROW_SIZE = 9.0F;
    private static final float ROW_HEIGHT = 13.0F;
    private static final float MIN_WIDTH = 168.0F;

    private record Row(String label, String value, boolean accent) {
    }

    private final List<Row> rows = new ArrayList<>();
    private long rowsSignature = Long.MIN_VALUE;
    private long layoutSignature = Long.MIN_VALUE;
    private float cachedContentWidth;

    public PveStatusElement() {
        super("pve_status", "PvE State");
    }

    @Override
    protected float defaultY(float unit) {
        return 170.0F * unit;
    }

    @Override
    protected boolean preservePositionOnContentResize() {
        return true;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        rebuildRows();
        long signature = this.rowsSignature * 31L + Float.floatToIntBits(unit);
        if (signature != this.layoutSignature) {
            MsdfFont font = UiFonts.sfProDisplay();
            float rowSize = ROW_SIZE * unit;
            float spacing = rowSize * UiFontStyle.MEDIUM.letterSpacingEm();
            float contentWidth = MIN_WIDTH * unit;
            for (Row row : this.rows) {
                float measured = font.measureWidth(
                        row.label() + "  " + row.value(),
                        rowSize,
                        spacing
                );
                contentWidth = Math.max(contentWidth, measured + PADDING * 2.0F * unit);
            }
            this.cachedContentWidth = contentWidth;
            this.layoutSignature = signature;
        }
        this.width = this.cachedContentWidth;
        this.height = (PADDING * 2.0F + 18.0F
                + this.rows.size() * ROW_HEIGHT) * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        float alpha = appearAlpha();
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(8.0F * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float iconSize = ICON_SIZE * unit;
        float titleSize = TITLE_SIZE * unit;
        float headerCenterY = this.y + (PADDING + 7.0F) * unit;
        Render2DUtil.texture(
                        this.x + PADDING * unit,
                        headerCenterY - iconSize / 2.0F,
                        iconSize,
                        iconSize,
                        Textures.Icons.OPTION
                )
                .color(ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                .draw();
        Render2DUtil.text(
                        this.x + (PADDING + ICON_SIZE + 6.0F) * unit,
                        font.centeredTextY(headerCenterY, titleSize),
                        titleSize,
                        MenuText.ui("PvE State")
                )
                .style(UiFontStyle.SEMIBOLD)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TITLE, alpha))
                .draw();

        float cursorY = this.y + (PADDING + 18.0F) * unit;
        float rowSize = ROW_SIZE * unit;
        for (Row row : this.rows) {
            float centerY = cursorY + ROW_HEIGHT * unit / 2.0F;
            Render2DUtil.text(
                            this.x + PADDING * unit,
                            font.centeredTextY(centerY, rowSize),
                            rowSize,
                            row.label()
                    )
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(
                            row.accent() ? Theme.getAccent() : Theme.Colors.TEXT_TEXT,
                            alpha
                    ))
                    .draw();
            float valueWidth = font.measureWidth(
                    row.value(),
                    rowSize,
                    rowSize * UiFontStyle.MEDIUM.letterSpacingEm()
            );
            Render2DUtil.text(
                            this.x + this.width - PADDING * unit - valueWidth,
                            font.centeredTextY(centerY, rowSize),
                            rowSize,
                            row.value()
                    )
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.SECONDARY_DARK, alpha))
                    .draw();
            cursorY += ROW_HEIGHT * unit;
        }
    }

    private void rebuildRows() {
        AutoMineFeature autoMine =
                FeatureManager.INSTANCE.getFeature(AutoMineFeature.class);
        AutoMineFeature.Status status = autoMine == null ? null : autoMine.currentStatus();

        long signature = 1125899906842597L;
        if (status == null) {
            signature = signature * 31L + 1L;
        } else {
            signature = signature * 31L + (status.active() ? 1 : 0);
            signature = signature * 31L + hashCode(status.phase());
            signature = signature * 31L + status.minedOres();
            signature = signature * 31L + status.knownTargets();
            signature = signature * 31L + status.elapsedMillis() / 1_000L;
            signature = signature * 31L + hashCode(status.profile());
            signature = signature * 31L + hashCode(status.toolProfile());
        }
        for (Feature feature : FeatureManager.INSTANCE.getFeatures(FeatureCategory.PVE)) {
            if (feature != autoMine
                    && feature != PveManagerFeature.INSTANCE
                    && feature.isEnabled()) {
                signature = signature * 31L + feature.getName().hashCode();
            }
        }
        if (signature == this.rowsSignature) {
            return;
        }
        this.rowsSignature = signature;

        this.rows.clear();
        if (status == null) {
            this.rows.add(new Row("AutoMine", MenuText.ui("In development"), false));
        } else {
            this.rows.add(new Row(
                    "AutoMine",
                    status.active()
                            ? displayPhase(status.phase())
                            : MenuText.ui("Disabled"),
                    status.active()
            ));
            this.rows.add(new Row(
                    MenuText.ui("Mined ores"),
                    Integer.toString(status.minedOres()),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Known targets"),
                    Integer.toString(status.knownTargets()),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Elapsed"),
                    formatDuration(status.elapsedMillis()),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Mine profile"),
                    status.profile(),
                    false
            ));
            this.rows.add(new Row(
                    MenuText.ui("Tool profile"),
                    status.toolProfile(),
                    false
            ));
        }

        for (Feature feature :
                FeatureManager.INSTANCE.getFeatures(FeatureCategory.PVE)) {
            if (feature == autoMine
                    || feature == PveManagerFeature.INSTANCE
                    || !feature.isEnabled()) {
                continue;
            }
            this.rows.add(new Row(
                    feature.getName(),
                    MenuText.ui("In development"),
                    false
            ));
        }
    }

    private static int hashCode(String value) {
        return value == null ? 0 : value.hashCode();
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1_000L);
        long hours = totalSeconds / 3_600L;
        long minutes = totalSeconds % 3_600L / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return pad2(hours) + ":" + pad2(minutes) + ":" + pad2(seconds);
        }
        return pad2(minutes) + ":" + pad2(seconds);
    }

    private static String pad2(long value) {
        return value < 10L ? "0" + value : Long.toString(value);
    }

    private static String displayPhase(String phase) {
        if (phase != null && phase.startsWith("Paused:")) {
            String reason = phase.substring("Paused:".length()).trim();
            return MenuText.ui("Paused") + ": " + MenuText.ui(reason);
        }
        return MenuText.ui(phase);
    }
}
