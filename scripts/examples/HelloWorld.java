package script;

import tech.huihui.client.script.api.Script;
import tech.huihui.client.script.api.HudRenderContext;

public class HelloWorld extends Script {

    private int ticks = 0;

    @Override
    public void onLoad() {
        api.print("HelloWorld загружен!");

        // Обычный модуль
        api.module("HW Module")
            .category("MISC")
            .key(-1)
            .onEnable(() -> api.print("HW Module вкл"))
            .onDisable(() -> api.print("HW Module выкл"))
            .onTick(() -> { if (++ticks % 20 == 0) api.print("tick " + ticks); })
            .register();

        // HUD-модуль (рисуется только когда включён)
        api.hud("HW HUD")
            .pos(10, 70).size(120, 20)
            .category("RENDER").key(-1)
            .onRender((HudRenderContext ctx) ->
                ctx.text("Hello HUD!", ctx.getX(), ctx.getY(), 0xff00ff00))
            .register();
    }

    @Override
    public void onUnload() {
        api.print("HelloWorld выгружен!");
    }
}
