package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import tech.huihui.HuihuiClient;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.client.modules.api.Module;
import tech.huihui.utility.game.other.MessageUtil;

public class BindCommand extends CommandAbstract {
    public BindCommand() {
        super("bind");
    }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("list").executes(context -> {
            MessageUtil.displayInfo("Модули с привязками:");
            for (Module module : HuihuiClient.getInstance().getModuleManager().getModules()) {
                if (module.getKeyCode() != -1) {
                    MessageUtil.displayInfo(module.getName() + ": " + getKeyCodeName(module.getKeyCode()));
                }
            }
            return 1;
        })).then(literal("clear").executes(context -> {
            for (Module module : HuihuiClient.getInstance().getModuleManager().getModules()) {
                module.setKeyCode(-1);
            }
            MessageUtil.displayInfo("Все привязки клавиш удалены");
            return 1;
        })).then(arg("модуль", StringArgumentType.word()).suggests(this.moduleSuggestions()).executes(context -> {
            String moduleName = context.getArgument("модуль", String.class);
            Module module = HuihuiClient.getInstance().getModuleManager().getModule(moduleName);
            if (module == null) {
                MessageUtil.displayError("Модуль '" + moduleName + "' не найден");
                return 1;
            }
            MessageUtil.displayInfo("Привязка модуля: " + module.getName() + " (код: " + module.getKeyCode() + ")");
            return 1;
        })).then(arg("модуль", StringArgumentType.word()).suggests(this.moduleSuggestions()).then(arg("код", IntegerArgumentType.integer(0, 100)).executes(context -> {
            String moduleName = context.getArgument("модуль", String.class);
            Module module = HuihuiClient.getInstance().getModuleManager().getModule(moduleName);
            if (module == null) {
                MessageUtil.displayError("Модуль '" + moduleName + "' не найден");
                return 1;
            }
            int keyCode = context.getArgument("код", Integer.class);
            module.setKeyCode(keyCode);
            MessageUtil.displayInfo("Модуль '" + module.getName() + "' привязан к клавише: " + getKeyCodeName(keyCode));
            return 1;
        })));
    }

    private String getKeyCodeName(int keyCode) {
        switch (keyCode) {
            case 0: return "None";
            case 1: return "Mouse Left";
            case 2: return "Mouse Right";
            case 3: return "Mouse Middle";
            case 4: return "Backspace";
            case 5: return "Tab";
            case 6: return "Clear";
            case 7: return "Enter";
            case 8: return "Shift";
            case 9: return "Control";
            case 10: return "Alt";
            case 11: return "Pause";
            case 12: return "Caps Lock";
            case 13: return "Escape";
            case 14: return "Space";
            case 15: return "Page Up";
            case 16: return "Page Down";
            case 17: return "End";
            case 18: return "Home";
            case 19: return "Arrow Left";
            case 20: return "Arrow Up";
            case 21: return "Arrow Right";
            case 22: return "Arrow Down";
            case 23: return "Print Screen";
            case 24: return "Insert";
            case 25: return "Delete";
            case 26: return "Help";
            case 27: return "0";
            case 28: return "1";
            case 29: return "2";
            case 30: return "3";
            case 31: return "4";
            case 32: return "5";
            case 33: return "6";
            case 34: return "7";
            case 35: return "8";
            case 36: return "9";
            case 37: return "A";
            case 38: return "B";
            case 39: return "C";
            case 40: return "D";
            case 41: return "E";
            case 42: return "F";
            case 43: return "G";
            case 44: return "H";
            case 45: return "I";
            case 46: return "J";
            case 47: return "K";
            case 48: return "L";
            case 49: return "M";
            case 50: return "N";
            case 51: return "O";
            case 52: return "P";
            case 53: return "Q";
            case 54: return "R";
            case 55: return "S";
            case 56: return "T";
            case 57: return "U";
            case 58: return "V";
            case 59: return "W";
            case 60: return "X";
            case 61: return "Y";
            case 62: return "Z";
            default: return "Key " + keyCode;
        }
    }

    private SuggestionProvider<CommandSource> moduleSuggestions() {
        return (context, builder) -> {
            for (Module module : HuihuiClient.getInstance().getModuleManager().getModules()) {
                builder.suggest(module.getName());
            }
            return builder.buildFuture();
        };
    }
}