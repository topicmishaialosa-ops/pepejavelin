package tech.huihui.client.script;

import tech.huihui.client.script.api.ScriptAPI;

import java.io.File;
import java.util.function.Consumer;

/**
 * Описание загруженного скрипта (для выгрузки/перезагрузки).
 */
public class LoadedScript {
    final String name;
    final String lang;
    final File file;
    final ScriptAPI api;
    final Object handle;
    final Consumer<ScriptAPI> unload;

    public LoadedScript(String name, String lang, File file, ScriptAPI api, Object handle, Consumer<ScriptAPI> unload) {
        this.name = name;
        this.lang = lang;
        this.file = file;
        this.api = api;
        this.handle = handle;
        this.unload = unload;
    }

    public void unload() {
        if (unload != null) unload.accept(api);
        api.dispose();
    }
}
