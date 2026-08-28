package tech.huihui.client.script;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.render.EventRender2D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.script.api.HudRenderContext;
import tech.huihui.client.script.api.ScriptAPI;

import java.util.function.Consumer;

/**
 * HUD-модуль, создаваемый скриптами через {@code api.hud(...)}.
 * Отрисовывается только пока модуль включён (toggle), что делает его
 * полноценным переключаемым HUD-элементом.
 */
@ModuleAnnotation(name = "scripthud", category = Category.RENDER, description = "script hud module")
public class ScriptHudModule extends Module {
    private Category scriptCategory;
    private Consumer<HudRenderContext> renderCb;
    private double x = 10, y = 10, width = 100, height = 100;

    public ScriptHudModule(ScriptAPI api, String name, Category category, int key) {
        setName(name);
        this.scriptCategory = category;
        setKeyCode(key);
        setEnabled(false);
    }

    @Override
    public Category getCategory() {
        return scriptCategory;
    }

    public void setRender(Consumer<HudRenderContext> cb) { this.renderCb = cb; }

    public void setPos(double x, double y) { this.x = x; this.y = y; }

    public void setSize(double w, double h) { this.width = w; this.height = h; }

    public double getX() { return x; }

    public double getY() { return y; }

    public double getWidth() { return width; }

    public double getHeight() { return height; }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        if (renderCb != null) {
            renderCb.accept(new HudRenderContext(e.getContext(), x, y, width, height));
        }
    }
}
