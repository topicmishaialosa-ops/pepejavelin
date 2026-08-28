package tech.huihui.client.script.api;

import tech.huihui.client.modules.api.Category;
import tech.huihui.client.script.ScriptHudModule;
import tech.huihui.client.script.api.ScriptAPI;

import java.util.function.Consumer;

/**
 * Построитель HUD-модуля для скриптов. Пример:
 * <pre>
 *   api.hud("FPS HUD")
 *      .pos(10, 10).size(80, 20)
 *      .category("RENDER").key(-1)
 *      .onRender(ctx -> ctx.text("X:" + ..., ctx.getX(), ctx.getY(), 0xffffff))
 *      .register();
 * </pre>
 */
public class ScriptHudModuleBuilder {
    private final ScriptAPI api;
    private final String name;
    private Category category = Category.RENDER;
    private int key = -1;
    private double x = 10, y = 10, w = 100, h = 100;
    private Consumer<HudRenderContext> renderCb;

    ScriptHudModuleBuilder(ScriptAPI api, String name) {
        this.api = api;
        this.name = name;
    }

    public ScriptHudModuleBuilder category(String c) {
        this.category = parse(c);
        return this;
    }

    public ScriptHudModuleBuilder key(int k) {
        this.key = k;
        return this;
    }

    public ScriptHudModuleBuilder pos(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public ScriptHudModuleBuilder size(double w, double h) {
        this.w = w;
        this.h = h;
        return this;
    }

    public ScriptHudModuleBuilder onRender(Consumer<HudRenderContext> cb) {
        this.renderCb = cb;
        return this;
    }

    public ScriptHudModule register() {
        ScriptHudModule m = new ScriptHudModule(api, name, category, key);
        m.setPos(x, y);
        m.setSize(w, h);
        m.setRender(renderCb);
        api.ownedModules.add(m);
        ScriptAPI.getModuleManager().registerScriptModule(m);
        return m;
    }

    private static Category parse(String s) {
        if (s != null) {
            for (Category c : Category.values()) {
                if (c.name().equalsIgnoreCase(s)) return c;
            }
        }
        return Category.RENDER;
    }
}
