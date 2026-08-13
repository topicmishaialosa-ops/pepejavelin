package tech.huihui.client.screens.dropdowngui;

import net.minecraft.util.math.Vec2f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.base.theme.ThemeManager;
import tech.huihui.client.screens.theme.ThemeEditorScreen;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class ThemePicker {
    public static final float BOX_WIDTH = 150.0F;
    public static final float BOX_HEIGHT = 18.0F;
    private static final float ROW_HEIGHT = 16.0F;
    private static final float EDIT_BTN_SIZE = 18.0F;
    private static final float EDIT_GAP = 4.0F;

    @FunctionalInterface
    public interface Scissor {
        void enable(float x, float y, float width, float height);

        default void disable() {
        }
    }

    private final Animation themeAnim = new Animation(180L, Easing.CUBIC_OUT);
    private boolean themeOpen;

    public void reset() {
        this.themeOpen = false;
    }

    public float width() {
        return EDIT_BTN_SIZE + EDIT_GAP + BOX_WIDTH;
    }

    private ThemeManager themeManager() {
        return HuihuiClient.getInstance().getThemeManager();
    }

    private Theme themeAt(float mouseX, float mouseY, float x, float y) {
        float listY = y + BOX_HEIGHT + 4.0F;
        if (mouseX < x || mouseX > x + BOX_WIDTH || mouseY < listY) {
            return null;
        }
        int index = (int) ((mouseY - listY) / ROW_HEIGHT);
        if (index < 0 || index >= this.themeManager().getThemes().size()) {
            return null;
        }
        return this.themeManager().getThemes().get(index);
    }

    private void selectTheme(Theme theme) {
        ThemeManager themeManager = this.themeManager();
        if (themeManager.getCurrentTheme() != theme) {
            theme.getAnimation().setValue(0.0F);
            theme.startAnimation(themeManager.getCurrentTheme().getColor1(), themeManager.getCurrentTheme().getColor2());
            themeManager.setCurrentTheme(theme);
        }
        this.themeOpen = false;
    }

    public boolean mouseClicked(float mouseX, float mouseY, float x, float y) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, BOX_WIDTH, BOX_HEIGHT)) {
            this.themeOpen = !this.themeOpen;
            return true;
        }
        float ebx = x - EDIT_BTN_SIZE - EDIT_GAP;
        if (MathUtil.isHovered(mouseX, mouseY, ebx, y, EDIT_BTN_SIZE, EDIT_BTN_SIZE)) {
            this.themeOpen = false;
            ThemeEditorScreen.openEditor();
            return true;
        }
        if (this.themeOpen) {
            Theme theme = this.themeAt(mouseX, mouseY, x, y);
            if (theme != null) {
                this.selectTheme(theme);
                return true;
            }
            this.themeOpen = false;
            if (MathUtil.isHovered(mouseX, mouseY, x, y + BOX_HEIGHT + 4.0F, BOX_WIDTH, (float) this.themeManager().getThemes().size() * ROW_HEIGHT)) {
                return true;
            }
        }
        return false;
    }

    public void render(CustomDrawContext draw, Theme theme, ColorRGBA panelBg, ColorRGBA accent, ColorRGBA text, float alpha, float x, float y, float mouseX, float mouseY, Scissor scissor) {
        this.themeAnim.update(this.themeOpen);
        Theme current = this.themeManager().getCurrentTheme();

        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, BOX_WIDTH, BOX_HEIGHT);
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, BOX_WIDTH, BOX_HEIGHT, BorderRadius.all(6.0F), panelBg.withAlpha((int) (200.0F * alpha)));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, BOX_WIDTH, BOX_HEIGHT, 1.0F, BorderRadius.all(6.0F), accent.withAlpha(hovered ? 130.0F : 90.0F).withAlpha(255.0F * alpha));

        float ebx = x - EDIT_BTN_SIZE - EDIT_GAP;
        float eby = y;
        boolean editHovered = MathUtil.isHovered(mouseX, mouseY, ebx, eby, EDIT_BTN_SIZE, EDIT_BTN_SIZE);
        DrawUtil.drawRoundedRect(draw.getMatrices(), ebx, eby, EDIT_BTN_SIZE, EDIT_BTN_SIZE, BorderRadius.all(6.0F), editHovered ? accent.withAlpha(120.0F * alpha) : panelBg.withAlpha((int) (200.0F * alpha)));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), ebx, eby, EDIT_BTN_SIZE, EDIT_BTN_SIZE, 1.0F, BorderRadius.all(6.0F), accent.withAlpha(editHovered ? 200.0F : 90.0F).withAlpha(255.0F * alpha));
        ColorRGBA pencil = (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha);
        DrawUtil.drawLine(draw.getMatrices(), new Vec2f(ebx + 11.5F, eby + 4.5F), new Vec2f(ebx + 5.0F, eby + 11.0F), pencil);
        DrawUtil.drawLine(draw.getMatrices(), new Vec2f(ebx + 5.0F, eby + 11.0F), new Vec2f(ebx + 7.5F, eby + 13.5F), pencil);
        DrawUtil.drawLine(draw.getMatrices(), new Vec2f(ebx + 12.5F, eby + 5.5F), new Vec2f(ebx + 9.5F, eby + 8.5F), pencil);

        DrawUtil.drawRoundedRect(draw.getMatrices(), x + 5.0F, y + 5.0F, 8.0F, 8.0F, BorderRadius.all(2.0F), current.getColor().withAlpha(255.0F * alpha), current.getColor().withAlpha(255.0F * alpha), current.getSecondColor().withAlpha(255.0F * alpha), current.getSecondColor().withAlpha(255.0F * alpha));
        draw.drawText(Fonts.REGULAR.getFont(5.5F), current.getName(), x + 17.0F, y + 6.0F, text.withAlpha(255.0F * alpha));

        float cxp = x + BOX_WIDTH - 13.0F;
        float cyp = y + BOX_HEIGHT / 2.0F;
        ColorRGBA chevron = (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha);
        DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp - 2.0F), new Vec2f(cxp, cyp + 1.0F), chevron);
        DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp + 1.0F), new Vec2f(cxp + 3.0F, cyp - 2.0F), chevron);

        if (this.themeOpen) {
            float listY = y + BOX_HEIGHT + 4.0F;
            float listH = (float) this.themeManager().getThemes().size() * ROW_HEIGHT * this.themeAnim.getValue();
            DrawUtil.drawRoundedRect(draw.getMatrices(), x, listY, BOX_WIDTH, Math.max(listH, 2.0F), BorderRadius.all(6.0F), panelBg.withAlpha((int) (225.0F * alpha)));
            DrawUtil.drawRoundedBorder(draw.getMatrices(), x, listY, BOX_WIDTH, Math.max(listH, 2.0F), 1.0F, BorderRadius.all(6.0F), accent.withAlpha(70.0F * alpha));
            scissor.enable(x, listY, BOX_WIDTH, listH);
            int row = 0;
            for (Theme candidate : this.themeManager().getThemes()) {
                float ry = listY + (float) row * ROW_HEIGHT;
                boolean selected = candidate == current;
                boolean rowHovered = MathUtil.isHovered(mouseX, mouseY, x, ry, BOX_WIDTH, ROW_HEIGHT);
                if (selected) {
                    DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, ry, BOX_WIDTH - 4.0F, ROW_HEIGHT, BorderRadius.all(4.0F), accent.withAlpha(60.0F * alpha));
                } else if (rowHovered) {
                    DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, ry, BOX_WIDTH - 4.0F, ROW_HEIGHT, BorderRadius.all(4.0F), (new ColorRGBA(60, 60, 60)).withAlpha(120.0F * alpha));
                }
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + 5.0F, ry + 4.0F, 8.0F, 8.0F, BorderRadius.all(2.0F), candidate.getColor(), candidate.getColor(), candidate.getSecondColor(), candidate.getSecondColor());
                draw.drawText(Fonts.REGULAR.getFont(5.0F), candidate.getName(), x + 17.0F, ry + 5.0F, (new ColorRGBA(selected ? 255 : 200, selected ? 255 : 200, selected ? 255 : 200)).withAlpha(255.0F * alpha));
                row++;
            }
            scissor.disable();
        }
    }
}
