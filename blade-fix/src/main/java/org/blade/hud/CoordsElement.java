package org.blade.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.blade.feature.impl.visual.HudFeature;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;
import org.blade.utils.render.Textures;

public final class CoordsElement extends HudElement {

    private static final float ICON_SIZE = 16.0F;
    private static final float TEXT_SIZE = 10.0F;

    private static final int NAMETAGS_BACKGROUND = 0x80000000;
    private static final float NAMETAGS_RADIUS = 3.0F;
    private static final float NAMETAGS_BLUR = 6.0F;
    private static final float NAMETAGS_PAD_X = 4.0F;
    private static final float NAMETAGS_PAD_Y = 1.5F;

    private record Style(
            float height,
            float radius,
            float padding,
            boolean showIcon,
            float labelValueGap,
            float dividerGap,
            float dividerHeight
    ) {
    }

    private static final Style MINI = new Style(28.0F, 8.0F, 8.0F, false, 4.0F, 8.0F, 12.0F);

    private static final String[] AXES = {"X", "Y", "Z"};

    private final String[] values = new String[3];
    private final float[] valueWidths = new float[3];
    private final float[] axisWidths = new float[3];
    private float measuredUnit = Float.NaN;
    private boolean measuredNametags;
    private String nametagsText = "";
    private final Style style = MINI;

    public CoordsElement() {
        super("coords", "Coordinates");
    }

    @Override
    protected float defaultY(float unit) {
        return 72.0F * unit;
    }

    @Override
    protected void layout(Minecraft mc, float unit) {
        String nextX = coordinate(mc.player.getX());
        String nextY = coordinate(mc.player.getY());
        String nextZ = coordinate(mc.player.getZ());
        boolean nametagsStyle = HudFeature.newCoordsEnabled();
        boolean styleChanged = nametagsStyle != this.measuredNametags;
        boolean unitChanged = unit != this.measuredUnit;
        boolean valuesChanged = styleChanged || unitChanged
                || !nextX.equals(this.values[0])
                || !nextY.equals(this.values[1])
                || !nextZ.equals(this.values[2]);
        if (!valuesChanged) {
            return;
        }
        this.values[0] = nextX;
        this.values[1] = nextY;
        this.values[2] = nextZ;
        this.measuredNametags = nametagsStyle;

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();

        if (nametagsStyle) {
            if (unitChanged) {
                this.measuredUnit = unit;
            }
            this.nametagsText = "x " + nextX + " y " + nextY + " z " + nextZ;
            this.width = font.measureWidth(this.nametagsText, textSize, letterSpacing)
                    + NAMETAGS_PAD_X * 2.0F * unit;
            this.height = font.textHeight(textSize) + NAMETAGS_PAD_Y * 2.0F * unit;
            return;
        }

        if (unitChanged) {
            this.measuredUnit = unit;
            for (int i = 0; i < AXES.length; i++) {
                this.axisWidths[i] = font.measureWidth(AXES[i], textSize, letterSpacing);
            }
        }
        for (int i = 0; i < AXES.length; i++) {
            this.valueWidths[i] = font.measureWidth(this.values[i], textSize, letterSpacing);
        }

        float width = this.style.padding() * unit;
        if (this.style.showIcon()) {
            width += ICON_SIZE * unit;
        }
        for (int i = 0; i < AXES.length; i++) {
            if (hasDivider(i)) {
                width += (this.style.dividerGap() * 2.0F + 1.0F) * unit;
            }
            width += this.axisWidths[i];
            width += this.style.labelValueGap() * unit;
            width += this.valueWidths[i];
        }
        this.width = width + this.style.padding() * unit;
        this.height = this.style.height() * unit;
    }

    @Override
    protected void draw(Minecraft mc, float unit) {
        float alpha = appearAlpha();
        if (HudFeature.newCoordsEnabled()) {
            drawNametags(unit, alpha);
            return;
        }

        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .glass(alpha, 0.5F * unit, 8.0F * unit)
                .radius(this.style.radius() * unit)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float centerY = this.y + this.height / 2.0F;
        float textY = font.centeredTextY(centerY, textSize);

        float cursor = this.x + this.style.padding() * unit;
        if (this.style.showIcon()) {
            float iconSize = ICON_SIZE * unit;
            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.MOVE_3D)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON_GHOST, alpha))
                    .draw();
            cursor += iconSize;
        }

        for (int i = 0; i < AXES.length; i++) {
            if (hasDivider(i)) {
                cursor = divider(cursor, centerY, this.style.dividerHeight(), this.style.dividerGap(), unit, alpha);
            }

            Render2DUtil.text(cursor, textY, textSize, AXES[i])
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.ICON, alpha))
                    .draw();
            cursor += this.axisWidths[i] + this.style.labelValueGap() * unit;

            Render2DUtil.text(cursor, textY, textSize, this.values[i])
                    .style(UiFontStyle.MEDIUM)
                    .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TEXT, alpha))
                    .draw();
            cursor += this.valueWidths[i];
        }
    }

    private void drawNametags(float unit, float alpha) {
        Render2DUtil.rect(this.x, this.y, this.width, this.height)
                .color(ColorUtil.multiplyAlpha(NAMETAGS_BACKGROUND, alpha))
                .radius(NAMETAGS_RADIUS * unit)
                .blur(NAMETAGS_BLUR * unit, alpha)
                .draw();

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float textY = font.centeredTextY(this.y + this.height / 2.0F, textSize);
        Render2DUtil.text(this.x + NAMETAGS_PAD_X * unit, textY, textSize, this.nametagsText)
                .style(UiFontStyle.MEDIUM)
                .color(ColorUtil.multiplyAlpha(Theme.Colors.TEXT_TEXT, alpha))
                .draw();
    }

    private boolean hasDivider(int index) {
        return index > 0 || this.style.showIcon();
    }

    private static String coordinate(double value) {
        return Integer.toString((int) Math.floor(value));
    }
}
