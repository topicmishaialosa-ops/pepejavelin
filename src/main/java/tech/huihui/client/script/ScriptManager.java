package tech.huihui.client.script;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import tech.huihui.client.script.api.ScriptAPI;
import tech.huihui.client.script.loader.JavaLoader;
import tech.huihui.client.script.loader.JsonLoader;
import tech.huihui.client.script.loader.LuaLoader;
import tech.huihui.utility.game.other.MessageUtil;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Управляет загрузкой/выгрузкой скриптов разных языков и командой .script
 */
public class ScriptManager {
    private static final Map<String, LoadedScript> scripts = new LinkedHashMap<>();
    private static File scriptsDir;
    private static boolean inited = false;

    public static void init() {
        if (inited) return;
        inited = true;
        scriptsDir = new File(System.getProperty("user.home"), ".minecraft/Huihui/scripts");
        if (!scriptsDir.exists()) scriptsDir.mkdirs();
        EventManager.register(new ScriptEventBridge());
    }

    public static File getScriptsDir() {
        if (scriptsDir == null) init();
        return scriptsDir;
    }

    public static void load(String lang, String name) {
        if (scripts.containsKey(name)) {
            MessageUtil.displayInfo("§cСкрипт §f" + name + "§c уже загружен");
            return;
        }
        File file = resolveFile(lang, name);
        if (file == null || !file.exists()) {
            MessageUtil.displayInfo("§cСкрипт не найден: §f" + name + "." + lang);
            MessageUtil.displayInfo("§7Кинь файл в: §f" + new File(getScriptsDir(), lang).getAbsolutePath());
            return;
        }
        ScriptAPI api = new ScriptAPI(name);
        try {
            LoadedScript ls;
            switch (lang) {
                case "java":
                    ls = JavaLoader.load(name, file, api);
                    break;
                case "lua":
                    ls = LuaLoader.load(name, file, api);
                    break;
                case "json":
                    ls = JsonLoader.load(name, file, api);
                    break;
                default:
                    api.dispose();
                    MessageUtil.displayInfo("§cНеизвестный язык: §f" + lang);
                    return;
            }
            scripts.put(name, ls);
            for (Runnable r : api.getEnableCallbacks()) r.run();
            MessageUtil.displayInfo("§aСкрипт §f" + name + "§a загружен (" + lang + ")");
        } catch (Exception e) {
            api.dispose();
            MessageUtil.displayInfo("§cОшибка загрузки §f" + name + "§c: §f" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void unload(String name) {
        LoadedScript ls = scripts.remove(name);
        if (ls == null) {
            MessageUtil.displayInfo("§cСкрипт не найден: §f" + name);
            return;
        }
        try {
            ls.unload();
        } catch (Exception e) {
            e.printStackTrace();
        }
        MessageUtil.displayInfo("§aСкрипт выгружен: §f" + name);
    }

    public static void reload(String lang, String name) {
        unload(name);
        load(lang, name);
    }

    public static void list(String lang) {
        StringBuilder sb = new StringBuilder("§bСкрипты (" + lang + "): ");
        boolean any = false;
        for (LoadedScript ls : scripts.values()) {
            if (ls.lang.equals(lang)) {
                sb.append("§f").append(ls.name).append(" ");
                any = true;
            }
        }
        if (!any) sb.append("§7нет");
        MessageUtil.displayInfo(sb.toString());
    }

    public static void listAll() {
        if (scripts.isEmpty()) {
            MessageUtil.displayInfo("§bСкрипты: §7нет загруженных");
            return;
        }
        StringBuilder sb = new StringBuilder("§bСкрипты: ");
        for (LoadedScript ls : scripts.values()) {
            sb.append("§f").append(ls.name).append("§7(").append(ls.lang).append(") ");
        }
        MessageUtil.displayInfo(sb.toString());
    }

    private static File resolveFile(String lang, String name) {
        // 1) подпапка языка: scripts/java/HelloWorld.java
        File inLang = new File(new File(getScriptsDir(), lang), name + "." + lang);
        if (inLang.exists()) return inLang;
        // 2) корень: scripts/HelloWorld.java
        File inRoot = new File(getScriptsDir(), name + "." + lang);
        if (inRoot.exists()) return inRoot;
        // 3) папка examples: scripts/examples/HelloWorld.java
        File inExamples = new File(new File(getScriptsDir(), "examples"), name + "." + lang);
        if (inExamples.exists()) return inExamples;
        // не найдено — возвращаем вариант в подпапке языка (для сообщения об ошибке)
        return inLang;
    }
}
