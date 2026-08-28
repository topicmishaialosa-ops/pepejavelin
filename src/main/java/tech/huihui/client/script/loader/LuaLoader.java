package tech.huihui.client.script.loader;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import tech.huihui.client.script.LoadedScript;
import tech.huihui.client.script.api.ScriptAPI;
import tech.huihui.utility.game.other.MessageUtil;

import java.io.File;

/**
 * Загрузчик Lua-скриптов (через luaj). Глобальная таблица {@code api} доступна из скрипта.
 */
public class LuaLoader {

    public static LoadedScript load(String name, File file, ScriptAPI api) throws Exception {
        LuaApiBridge bridge = new LuaApiBridge(api);
        Globals globals = JsePlatform.standardGlobals();

        globals.set("print", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                MessageUtil.displayInfo(arg.tojstring());
                return LuaValue.NIL;
            }
        });
        globals.set("api", CoerceJavaToLua.coerce(bridge));

        LuaValue chunk = globals.get("loadfile").call(LuaValue.valueOf(file.getAbsolutePath()));
        if (!chunk.isfunction()) {
            throw new RuntimeException("Ошибка загрузки Lua: " + chunk.tojstring());
        }
        chunk.call();

        LuaValue onLoad = globals.get("onLoad");
        if (onLoad.isfunction()) onLoad.call();

        final Globals g = globals;
        return new LoadedScript(name, "lua", file, api, g, a -> {
            LuaValue un = g.get("onUnload");
            if (un.isfunction()) un.call();
        });
    }
}
