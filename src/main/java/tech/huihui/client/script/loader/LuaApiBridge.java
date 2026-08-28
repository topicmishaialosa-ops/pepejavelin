package tech.huihui.client.script.loader;

import org.luaj.vm2.LuaValue;
import tech.huihui.client.script.api.HudRenderContext;
import tech.huihui.client.script.api.RenderContext2D;
import tech.huihui.client.script.api.RenderContext3D;
import tech.huihui.client.script.api.ScriptAPI;
import tech.huihui.client.script.api.ScriptHudModuleBuilder;
import tech.huihui.client.script.api.ScriptModuleBuilder;

import static org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce;

/**
 * Мост, предоставляющий Lua-скриптам методы Java-API. Lua-функции передаются как LuaValue.
 */
public class LuaApiBridge {
    private final ScriptAPI api;

    public LuaApiBridge(ScriptAPI api) {
        this.api = api;
    }

    public void onTick(LuaValue fn) {
        api.onTick(() -> fn.call());
    }

    public void onRender2D(LuaValue fn) {
        api.onRender2D(ctx -> fn.call(coerce(ctx)));
    }

    public void onRender3D(LuaValue fn) {
        api.onRender3D(ctx -> fn.call(coerce(ctx)));
    }

    public void onKey(LuaValue fn) {
        api.onKey(k -> fn.call(LuaValue.valueOf(k)));
    }

    public void onChatSend(LuaValue fn) {
        api.onChatSend(s -> fn.call(LuaValue.valueOf(s)));
    }

    public void onChatReceive(LuaValue fn) {
        api.onChatReceive(s -> fn.call(LuaValue.valueOf(s)));
    }

    public void onEnable(LuaValue fn) {
        api.onEnable(() -> fn.call());
    }

    public void onDisable(LuaValue fn) {
        api.onDisable(() -> fn.call());
    }

    public void jump() {
        api.jump();
    }

    public void print(String s) {
        api.print(s);
    }

    public void clickSlot(int slot) {
        api.clickSlot(slot);
    }

    public void clickSlot(int slot, int button) {
        api.clickSlot(slot, button);
    }

    public void clickSlot(int slot, int button, String action) {
        api.clickSlot(slot, button, action);
    }

    public void dropSlot(int slot) {
        api.dropSlot(slot);
    }

    public void quickMoveSlot(int slot) {
        api.quickMoveSlot(slot);
    }

    public void selectHotbar(int index) {
        api.selectHotbar(index);
    }

    public void clickSlotInventory(int slot) {
        api.clickSlotInventory(slot);
    }

    public void clickSlotInventory(int slot, int button) {
        api.clickSlotInventory(slot, button);
    }

    public void clickSlotInventory(int slot, int button, String action) {
        api.clickSlotInventory(slot, button, action);
    }

    public void chat(String s) {
        api.chat(s);
    }

    public void sendCommand(String s) {
        api.sendCommand(s);
    }

    public void enableModule(String n) {
        api.enableModule(n);
    }

    public void disableModule(String n) {
        api.disableModule(n);
    }

    public void toggleModule(String n) {
        api.toggleModule(n);
    }

    public void registerModule(String name, String category, int key, LuaValue tbl) {
        ScriptModuleBuilder b = api.module(name).category(category).key(key);
        if (tbl.istable()) {
            LuaValue oe = tbl.get("onEnable");
            if (oe.isfunction()) {
                final LuaValue f = oe;
                b.onEnable(() -> f.call());
            }
            LuaValue od = tbl.get("onDisable");
            if (od.isfunction()) {
                final LuaValue f = od;
                b.onDisable(() -> f.call());
            }
            LuaValue ot = tbl.get("onTick");
            if (ot.isfunction()) {
                final LuaValue f = ot;
                b.onTick(() -> f.call());
            }
        }
        b.register();
    }

    public void registerHud(String name, String category, int key, double x, double y, double w, double h, LuaValue tbl) {
        ScriptHudModuleBuilder b = api.hud(name).category(category).key(key).pos(x, y).size(w, h);
        if (tbl.istable()) {
            LuaValue or = tbl.get("onRender");
            if (or.isfunction()) {
                final LuaValue f = or;
                b.onRender(ctx -> f.call(coerce(ctx)));
            }
        }
        b.register();
    }
}
