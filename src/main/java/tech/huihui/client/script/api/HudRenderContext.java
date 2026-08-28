package tech.huihui.client.script.api;

import net.minecraft.client.gui.DrawContext;

/**
 * Контекст рендера для HUD-модуля скрипта. Помимо стандартных 2D-методов
 * предоставляет координаты и размеры самого HUD-элемента (верхний левый угол).
 */
public class HudRenderContext extends RenderContext2D {
    private final double hx, hy, hw, hh;

    public HudRenderContext(DrawContext ctx, double x, double y, double w, double h) {
        super(ctx);
        this.hx = x;
        this.hy = y;
        this.hw = w;
        this.hh = h;
    }

    /** Координата X верхнего левого угла HUD-элемента */
    public double getX() { return hx; }

    /** Координата Y верхнего левого угла HUD-элемента */
    public double getY() { return hy; }

    /** Ширина HUD-элемента */
    public double getWidth() { return hw; }

    /** Высота HUD-элемента */
    public double getHeight() { return hh; }
}
