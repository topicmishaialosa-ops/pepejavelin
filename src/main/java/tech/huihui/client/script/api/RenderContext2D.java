package tech.huihui.client.script.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

/**
 * Удобный 2D-рендер контекст, передаётся в колбэки onRender2D и HUD-модулей.
 * Обёртка над клиентским {@link CustomDrawContext}, поэтому доступен
 * «тот самый» кастомный рендер клиента (Blur, RoundedRect, шрифты, тема и т.д.).
 */
public class RenderContext2D {
    private final DrawContext ctx;
    private final CustomDrawContext custom;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public RenderContext2D(DrawContext ctx) {
        this.ctx = ctx;
        this.custom = (ctx instanceof CustomDrawContext) ? (CustomDrawContext) ctx : CustomDrawContext.of(ctx);
    }

    /** Исходный vanilla DrawContext */
    public DrawContext raw() {
        return ctx;
    }

    /** Клиентский CustomDrawContext — полный доступ к кастомному рендеру (DrawUtil, drawRoundedRect, drawText(Font,...) и т.д.) */
    public CustomDrawContext custom() {
        return custom;
    }

    /** MatrixStack текущего кадра */
    public MatrixStack matrices() {
        return ctx.getMatrices();
    }

    public MinecraftClient mc() {
        return mc;
    }

    /** Текущая тема клиента (цвета, шрифты) */
    public Theme theme() {
        return HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
    }

    /** Хелпер: int-цвет (0xAARRGGBB) -> ColorRGBA */
    public ColorRGBA color(int rgba) {
        return new ColorRGBA(rgba);
    }

    /* ---------- базовые примитивы (vanilla) ---------- */

    public void rect(double x, double y, double w, double h, int color) {
        ctx.fill((int) Math.round(x), (int) Math.round(y), (int) Math.round(x + w), (int) Math.round(y + h), color);
    }

    public void outlineRect(double x, double y, double w, double h, double thickness, int color) {
        rect(x, y, w, thickness, color);
        rect(x, y + h - thickness, w, thickness, color);
        rect(x, y, thickness, h, color);
        rect(x + w - thickness, y, thickness, h, color);
    }

    public void text(String t, double x, double y, int color) {
        ctx.drawText(mc.textRenderer, Text.literal(t), (int) Math.round(x), (int) Math.round(y), color, false);
    }

    public void text(String t, double x, double y, int color, boolean shadow) {
        ctx.drawText(mc.textRenderer, Text.literal(t), (int) Math.round(x), (int) Math.round(y), color, shadow);
    }

    public int textWidth(String t) {
        return mc.textRenderer.getWidth(t);
    }

    public int textHeight() {
        return mc.textRenderer.fontHeight;
    }

    public void line(double x1, double y1, double x2, double y2, double thickness, int color) {
        double ang = Math.atan2(y2 - y1, x2 - x1);
        double len = Math.hypot(x2 - x1, y2 - y1);
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x1, y1, 0.0D);
        ms.multiply(RotationAxis.POSITIVE_Z.rotation((float) ang));
        ctx.fill(0, (int) (-thickness / 2.0D), (int) Math.round(len), (int) Math.round(thickness), color);
        ms.pop();
    }

    public void circle(double x, double y, double r, int color) {
        int ir = (int) Math.round(r);
        for (int dy = -ir; dy <= ir; dy++) {
            int half = (int) Math.round(Math.sqrt((double) (ir * ir - dy * dy)));
            ctx.fill((int) Math.round(x - half), (int) Math.round(y + dy), half * 2, 1, color);
        }
    }

    /* ---------- кастомный рендер клиента (DrawUtil / CustomDrawContext) ---------- */

    /** Закруглённый прямоугольник через клиентский DrawUtil. */
    public void roundedRect(double x, double y, double w, double h, double radius, int color) {
        DrawUtil.drawRoundedRect(custom.getMatrices(), (float) x, (float) y, (float) w, (float) h,
                BorderRadius.all((float) radius), new ColorRGBA(color));
    }

    /** Закруглённая рамка через клиентский DrawUtil. */
    public void roundedOutline(double x, double y, double w, double h, double thickness, double radius, int color) {
        DrawUtil.drawRoundedBorder(custom.getMatrices(), (float) x, (float) y, (float) w, (float) h,
                (float) thickness, BorderRadius.all((float) radius), new ColorRGBA(color));
    }

    /** Закруглённый «squircle». */
    public void squircle(double x, double y, double w, double h, double squirt, double radius, int color) {
        DrawUtil.drawSquircle(custom.getMatrices(), (float) x, (float) y, (float) w, (float) h,
                (float) squirt, BorderRadius.all((float) radius), new ColorRGBA(color));
    }

    /** Размытие фона (blur) через клиентский DrawUtil. */
    public void blur(double x, double y, double w, double h, double radius, int color) {
        DrawUtil.drawBlur(custom.getMatrices(), (float) x, (float) y, (float) w, (float) h,
                (float) radius, BorderRadius.all((float) radius), new ColorRGBA(color));
    }

    /** Мягкая тень через клиентский DrawUtil. */
    public void shadow(double x, double y, double w, double h, double softness, double radius, int color) {
        DrawUtil.drawShadow(custom.getMatrices(), (float) x, (float) y, (float) w, (float) h,
                (float) softness, BorderRadius.all((float) radius), new ColorRGBA(color));
    }

    /** Текст клиентским шрифтом (Fonts.REGULAR) заданного размера. */
    public void text(String t, double x, double y, int color, float size) {
        custom.drawText(Fonts.REGULAR.getFont(size), t, (float) x, (float) y, new ColorRGBA(color));
    }

    /** Текст клиентским шрифтом с glow/тенью (Fonts.REGULAR). */
    public void textGlow(String t, double x, double y, int color, float size) {
        custom.drawText(Fonts.REGULAR.getFont(size), t, (float) x, (float) y, new ColorRGBA(color).withAlpha(60));
        custom.drawText(Fonts.REGULAR.getFont(size), t, (float) x, (float) y, new ColorRGBA(color));
    }
}
