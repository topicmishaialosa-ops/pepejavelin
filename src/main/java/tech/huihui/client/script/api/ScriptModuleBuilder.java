package tech.huihui.client.script.api;

import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.setting.Setting;
import tech.huihui.client.script.ScriptModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Построитель кастомного модуля для скриптов.
 */
public class ScriptModuleBuilder {
    private final ScriptAPI api;
    private final String name;
    private Category category = Category.MISC;
    private int key = -1;
    private Runnable onEnableCb;
    private Runnable onDisableCb;
    private Runnable onTickCb;
    private final List<Setting> settings = new ArrayList<>();

    ScriptModuleBuilder(ScriptAPI api, String name) {
        this.api = api;
        this.name = name;
    }

    public ScriptModuleBuilder category(String c) {
        this.category = parse(c);
        return this;
    }

    public ScriptModuleBuilder key(int k) {
        this.key = k;
        return this;
    }

    public ScriptModuleBuilder onEnable(Runnable r) {
        this.onEnableCb = r;
        return this;
    }

    public ScriptModuleBuilder onDisable(Runnable r) {
        this.onDisableCb = r;
        return this;
    }

    public ScriptModuleBuilder onTick(Runnable r) {
        this.onTickCb = r;
        return this;
    }

    public ScriptModuleBuilder setting(Setting s) {
        this.settings.add(s);
        return this;
    }

    public ScriptModule register() {
        ScriptModule m = new ScriptModule(name, category, key);
        m.setCallbacks(onEnableCb, onDisableCb, onTickCb);
        for (Setting s : settings) m.addSetting(s);
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
        return Category.MISC;
    }
}
