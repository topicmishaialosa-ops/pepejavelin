package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.client.script.ScriptManager;
import tech.huihui.utility.game.other.MessageUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ScriptCommand extends CommandAbstract {
    private static final String[] LANGS = {"java", "lua", "json"};

    public ScriptCommand() {
        super("script");
    }

    private static Path scriptsRoot() {
        return Paths.get(System.getProperty("user.home"), ".minecraft", "Huihui", "scripts");
    }

    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        for (String lang : LANGS) {
            builder.then(literal(lang)
                    .then(literal("list").executes(ctx -> {
                        ScriptManager.list(lang);
                        return 1;
                    }))
                    .then(literal("load").then(arg("name", StringArgumentType.word()).executes(ctx -> {
                        ScriptManager.load(lang, ctx.getArgument("name", String.class));
                        return 1;
                    })))
                    .then(literal("unload").then(arg("name", StringArgumentType.word()).executes(ctx -> {
                        ScriptManager.unload(ctx.getArgument("name", String.class));
                        return 1;
                    })))
                    .then(literal("reload").then(arg("name", StringArgumentType.word()).executes(ctx -> {
                        String name = ctx.getArgument("name", String.class);
                        ScriptManager.unload(name);
                        ScriptManager.load(lang, name);
                        return 1;
                    })))
                    .then(literal("dir").executes(ctx -> {
                        Path dir = scriptsRoot().resolve(lang);
                        ensureDir(dir);
                        openFolder(dir.toFile());
                        return 1;
                    }))
            );
        }
        // .script list — показать все загруженные
        builder.then(literal("list").executes(ctx -> {
            ScriptManager.listAll();
            return 1;
        }));
        // .script dir — открыть корневую папку скриптов
        builder.then(literal("dir").executes(ctx -> {
            Path dir = scriptsRoot();
            ensureDir(dir);
            openFolder(dir.toFile());
            return 1;
        }));
    }

    private static void ensureDir(Path dir) {
        try {
            if (!Files.exists(dir)) Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
    }

    /**
     * Кроссплатформенное открытие папки:
     * Windows -> explorer.exe, macOS -> open, Linux -> xdg-open (с фоллбэками).
     * Результат дублируется в чат, чтобы было понятно, что происходит.
     */
    private static void openFolder(File folder) {
        if (folder == null || !folder.exists()) {
            MessageUtil.displayInfo("§cПапка не существует: §f" + (folder == null ? "null" : folder.getAbsolutePath()));
            return;
        }
        String os = System.getProperty("os.name").toLowerCase();
        String path = folder.getAbsolutePath();
        try {
            Process p;
            if (os.contains("win")) {
                p = Runtime.getRuntime().exec(new String[]{"cmd", "/c", "explorer.exe", path});
            } else if (os.contains("mac") || os.contains("darwin")) {
                p = Runtime.getRuntime().exec(new String[]{"open", path});
            } else {
                // Linux / другие UNIX: перебираем известные файловые менеджеры
                String[] candidates = {"xdg-open", "nautilus", "dolphin", "thunar", "pcmanfm", "konqueror"};
                Process started = null;
                for (String cmd : candidates) {
                    try {
                        started = Runtime.getRuntime().exec(new String[]{cmd, path});
                        break;
                    } catch (IOException ignored) {
                    }
                }
                if (started == null) {
                    MessageUtil.displayInfo("§cНе найден файловый менеджер (xdg-open/open). Путь: §f" + path);
                    return;
                }
                p = started;
            }
            // не блокируем поток и не ждём вечно
            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();
            MessageUtil.displayInfo("§aОткрываю папку: §f" + path);
        } catch (Exception e) {
            MessageUtil.displayInfo("§cНе удалось открыть папку: §f" + path + " §c(" + e.getMessage() + ")");
        }
    }
}
