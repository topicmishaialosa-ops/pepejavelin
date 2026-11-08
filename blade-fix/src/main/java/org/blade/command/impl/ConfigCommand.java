package org.blade.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.util.Util;
import org.blade.command.ClientCommand;
import org.blade.feature.FeatureManager;
import org.blade.utils.ConfigIO;
import org.blade.utils.text.ChatUtil;

import java.io.File;
import java.util.List;

public final class ConfigCommand extends ClientCommand {
    public ConfigCommand() {
        super("config", "Manages named configs: save/load/remove/list", ":gear:");
    }

    @Override
    public List<String> aliases() {
        return List.of("cfg");
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        builder.executes(context -> {
            ChatUtil.usage("config save <name>  •  load <name>  •  remove <name>  •  list");
            return 1;
        });
        builder.then(named("save", this::executeSave));
        builder.then(named("load", this::executeLoad));
        builder.then(named("remove", this::executeRemove));
        builder.then(LiteralArgumentBuilder.literal("list").executes(context -> {
            List<String> names = FeatureManager.INSTANCE.configNames();
            if (names.isEmpty()) {
                ChatUtil.info("No configs saved");
            } else {
                ChatUtil.header("Saved configs  •  " + names.size());
                names.forEach(name -> ChatUtil.entry(":floppy_disk:", name, null));
            }
            return 1;
        }));
        builder.then(LiteralArgumentBuilder.literal("dir").executes(context -> {
            openConfigDir();
            return 1;
        }));
    }

    private void openConfigDir() {
        File dir = ConfigIO.configDir().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            Util.OS os = Util.getPlatform();
            String[] cmd;
            if (os == Util.OS.WINDOWS) {
                cmd = new String[]{"explorer.exe", dir.getAbsolutePath()};
            } else if (os == Util.OS.OSX) {
                cmd = new String[]{"open", dir.getAbsolutePath()};
            } else {
                cmd = new String[]{"xdg-open", dir.getAbsolutePath()};
            }
            new ProcessBuilder(cmd).start();
            ChatUtil.success("Opened config directory  •  " + dir.getAbsolutePath());
        } catch (Exception e) {
            ChatUtil.error("Failed to open config directory  •  " + e.getMessage());
        }
    }

    private LiteralArgumentBuilder<Object> named(String literal, java.util.function.Function<String, Integer> action) {
        return LiteralArgumentBuilder.<Object>literal(literal)
                .then(RequiredArgumentBuilder.<Object, String>argument("name", StringArgumentType.word())
                        .executes(context -> action.apply(name(context))));
    }

    private int executeSave(String name) {
        if (FeatureManager.INSTANCE.saveConfigAs(name)) {
            ChatUtil.success("Saved config  •  " + name);
            return 1;
        }
        ChatUtil.error("Invalid config name  •  Use letters, digits, - or _  •  " + name);
        return 0;
    }

    private int executeLoad(String name) {
        if (FeatureManager.INSTANCE.loadConfig(name)) {
            ChatUtil.success("Loaded config  •  " + name);
            return 1;
        }
        ChatUtil.error("Config not found  •  " + name);
        return 0;
    }

    private int executeRemove(String name) {
        if (FeatureManager.INSTANCE.deleteConfig(name)) {
            ChatUtil.success("Removed config  •  " + name);
            return 1;
        }
        ChatUtil.error("Config not found  •  " + name);
        return 0;
    }

    private String name(CommandContext<Object> context) {
        return StringArgumentType.getString(context, "name");
    }
}
