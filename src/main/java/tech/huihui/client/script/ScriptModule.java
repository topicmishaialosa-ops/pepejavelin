package tech.huihui.client.script;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Динамический модуль, создаваемый скриптами через API.
 */
@ModuleAnnotation(name = "script", category = Category.MISC, description = "script module")
public class ScriptModule extends Module {
    private Category scriptCategory;
    private Runnable enableCb;
    private Runnable disableCb;
    private Runnable tickCb;
    private final List<Setting> extra = new ArrayList<>();

    public ScriptModule(String name, Category category, int key) {
        setName(name);
        this.scriptCategory = category;
        setKeyCode(key);
        setEnabled(false);
    }

    @Override
    public Category getCategory() {
        return scriptCategory;
    }

    public void setCallbacks(Runnable enable, Runnable disable, Runnable tick) {
        this.enableCb = enable;
        this.disableCb = disable;
        this.tickCb = tick;
    }

    public void addSetting(Setting s) {
        extra.add(s);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (enableCb != null) enableCb.run();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (disableCb != null) disableCb.run();
    }

    @EventTarget
    public void onTick(EventTick e) {
        if (tickCb != null) tickCb.run();
    }

    @Override
    public List<Setting> getSettings() {
        List<Setting> all = super.getSettings();
        List<Setting> merged = new ArrayList<>(all);
        merged.addAll(extra);
        return merged;
    }
}
