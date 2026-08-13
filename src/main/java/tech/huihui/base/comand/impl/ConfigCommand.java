package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.command.CommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.utility.game.other.MessageUtil;

public class ConfigCommand extends CommandAbstract {
   public ConfigCommand() {
      super("cfg");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("save").then(arg("name", StringArgumentType.word()).executes((context) -> {
         String name = (String)context.getArgument("name", String.class);
         boolean success = HuihuiClient.getInstance().getConfigManager().saveConfig(name);
         if (success) {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Конфигурация сохранена");
         } else {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Ошибка при сохранении конфигурации");
         }

         return 1;
      })));
      builder.then(literal("load").then(arg("name", StringArgumentType.word()).executes((context) -> {
         String name = (String)context.getArgument("name", String.class);
         boolean success = HuihuiClient.getInstance().getConfigManager().loadConfig(name);
         if (success) {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Конфигурация загружена");
         } else {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Ошибка при загрузке конфигурации");
         }

         return 1;
      })));
      builder.then(literal("delete").then(arg("name", StringArgumentType.word()).executes((context) -> {
         String name = (String)context.getArgument("name", String.class);
         boolean success = HuihuiClient.getInstance().getConfigManager().deleteConfig(name);
         if (success) {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Конфигурация удалена");
         } else {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Ошибка при удалении конфигурации");
         }

         return 1;
      })));
      builder.then(literal("list").executes((context) -> {
         List<String> configs = HuihuiClient.getInstance().getConfigManager().configNames();
         if (configs.isEmpty()) {
            MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Конфигураций пока нет");
            return 1;
         }

         MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Список конфигураций:");
         for(String name : configs) {
            Text loadButton = Text.literal(" [Загрузить]").setStyle(Style.EMPTY.withColor(Formatting.GREEN).withBold(true)
               .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ".cfg load " + name))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажми, чтобы загрузить " + name))));
            Text deleteButton = Text.literal(" [Удалить]").setStyle(Style.EMPTY.withColor(Formatting.RED).withBold(true)
               .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ".cfg delete " + name))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Нажми, чтобы удалить " + name))));
            Text line = Text.literal(name).setStyle(Style.EMPTY.withColor(Formatting.WHITE));
            mc.player.sendMessage(line.copy().append(loadButton).append(deleteButton), false);
         }

         return 1;
      }));
builder.then(literal("dir").executes((context) -> {
          try {
             File dir = new File("huihui/configs/");
             if (!dir.exists()) {
                MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Ты нахуя папку удалил фрик");
                dir.mkdirs();
             } else {
                MessageUtil.displayInfo(String.valueOf(Formatting.GRAY) + "Открываю папку с конфигами...");
             }

             try {
                Desktop.getDesktop().open(dir);
             } catch (Exception var3) {
                try {
                   String os = System.getProperty("os.name").toLowerCase();
                   if (os.contains("win")) {
                      Runtime.getRuntime().exec(new String[]{"explorer", dir.getAbsolutePath()});
                   } else if (os.contains("mac")) {
                      Runtime.getRuntime().exec(new String[]{"open", dir.getAbsolutePath()});
                   } else {
                      Runtime.getRuntime().exec(new String[]{"xdg-open", dir.getAbsolutePath()});
                   }
                } catch (IOException var2) {
                   String var10000 = String.valueOf(Formatting.GRAY);
                   MessageUtil.displayInfo(var10000 + "Ошибка при открытии папки: " + String.valueOf(Formatting.WHITE) + var2.getMessage());
                }
             }
          } catch (Exception var5) {
             String var10001 = String.valueOf(Formatting.GRAY);
             MessageUtil.displayInfo(var10001 + "Ошибка при открытии папки: " + String.valueOf(Formatting.WHITE) + var5.getMessage());
          }

          return 1;
       }));
   }
}
