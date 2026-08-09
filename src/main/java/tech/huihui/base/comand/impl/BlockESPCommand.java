package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.client.modules.api.setting.impl.BlockMapSetting;
import tech.huihui.client.modules.impl.render.BlockESP;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public class BlockESPCommand extends CommandAbstract {
   private static final Map<String, Integer> COLORS = new LinkedHashMap();

   static {
      COLORS.put("red", 0xFFFF3B30);
      COLORS.put("orange", 0xFFFF9500);
      COLORS.put("yellow", 0xFFFFD700);
      COLORS.put("lime", 0xFFA8E10C);
      COLORS.put("green", 0xFF00E532);
      COLORS.put("cyan", 0xFF00B8D9);
      COLORS.put("light_blue", 0xFF4DA6FF);
      COLORS.put("blue", 0xFF2D63FF);
      COLORS.put("purple", 0xFFBF5AF2);
      COLORS.put("magenta", 0xFFD100D1);
      COLORS.put("pink", 0xFFFF2D92);
      COLORS.put("brown", 0xFF964B00);
      COLORS.put("white", 0xFFFFFFFF);
      COLORS.put("gray", 0xFF8E8E93);
      COLORS.put("grey", 0xFF8E8E93);
      COLORS.put("dark_gray", 0xFF3A3A3C);
      COLORS.put("dark_grey", 0xFF3A3A3C);
      COLORS.put("black", 0xFF111111);
   }

   public BlockESPCommand() {
      super("blockesp");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("add").then(arg("блок", StringArgumentType.word()).suggests(this.blockSuggestions())
         .executes((context) -> this.addBlock(context, (String)null))
         .then(arg("цвет", StringArgumentType.word()).suggests(this.colorSuggestions())
            .executes((context) -> this.addBlock(context, context.getArgument("цвет", String.class))))));
      builder.then(literal("remove").then(arg("блок", StringArgumentType.word()).suggests(this.addedBlockSuggestions())
         .executes(this::removeBlock)));
      builder.then(literal("clear").executes(this::clearBlocks));
      builder.then(literal("list").executes(this::listBlocks));
      builder.executes((context) -> {
         MessageUtil.displayInfo(Formatting.GRAY + "Использование: " + Formatting.WHITE + ".blockesp add <блок> [цвет] "
            + Formatting.GRAY + "| " + Formatting.WHITE + ".blockesp remove <блок> "
            + Formatting.GRAY + "| " + Formatting.WHITE + ".blockesp clear "
            + Formatting.GRAY + "| " + Formatting.WHITE + ".blockesp list");
         return 1;
      });
   }

   private int addBlock(CommandContext<CommandSource> context, String colorArg) {
      String name = context.getArgument("блок", String.class);
      Block block = this.findBlock(name);
      if (block == null) {
         MessageUtil.displayError("Блок '" + name + "' не найден. Используй .blockesp add <блок> [цвет]");
         return 0;
      }
      String id = BlockMapSetting.getId(block);
      int color;
      if (colorArg != null) {
         Integer parsed = this.parseColor(colorArg);
         if (parsed == null) {
            MessageUtil.displayError("Неизвестный цвет '" + colorArg + "'. Доступные: red, orange, yellow, lime, green, cyan, light_blue, blue, purple, magenta, pink, brown, white, gray, dark_gray, black, а также #RRGGBB");
            return 0;
         }
         color = parsed;
      } else {
         ColorRGBA theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getColor();
         color = (theme != null ? theme : new ColorRGBA(96, 130, 255)).getRGB();
      }
      BlockESP.INSTANCE.getBlocks().set(id, color);
      BlockESP.INSTANCE.markDirty();
      MessageUtil.displayInfo(Formatting.GRAY + "Блок " + Formatting.WHITE + id + Formatting.GRAY + " добавлен в BlockESP (цвет #" + String.format(Locale.ROOT, "%06X", color & 0xFFFFFF) + ")");
      return 1;
   }

   private int removeBlock(CommandContext<CommandSource> context) {
      String name = context.getArgument("блок", String.class);
      Block block = this.findBlock(name);
      if (block == null) {
         MessageUtil.displayError("Блок '" + name + "' не найден");
         return 0;
      }
      String id = BlockMapSetting.getId(block);
      if (!BlockESP.INSTANCE.getBlocks().contains(id)) {
         MessageUtil.displayInfo(Formatting.GRAY + "Блок " + Formatting.WHITE + id + Formatting.GRAY + " не в списке BlockESP");
         return 1;
      }
      BlockESP.INSTANCE.getBlocks().remove(id);
      BlockESP.INSTANCE.markDirty();
      MessageUtil.displayInfo(Formatting.GRAY + "Блок " + Formatting.WHITE + id + Formatting.GRAY + " убран из BlockESP");
      return 1;
   }

   private int clearBlocks(CommandContext<CommandSource> context) {
      if (BlockESP.INSTANCE.getBlocks().isEmpty()) {
         MessageUtil.displayInfo(Formatting.GRAY + "BlockESP и так пуст");
         return 1;
      }
      int count = BlockESP.INSTANCE.getBlocks().getBlocks().size();
      BlockESP.INSTANCE.getBlocks().clear();
      BlockESP.INSTANCE.markDirty();
      MessageUtil.displayInfo(Formatting.GRAY + "Убрано блоков из BlockESP: " + Formatting.WHITE + count);
      return 1;
   }

   private int listBlocks(CommandContext<CommandSource> context) {
      Map<String, Integer> map = BlockESP.INSTANCE.getBlocks().getBlocks();
      if (map.isEmpty()) {
         MessageUtil.displayInfo(Formatting.GRAY + "BlockESP пуст. Добавь блоки через " + Formatting.WHITE + ".blockesp add <блок> [цвет]");
         return 1;
      }
      StringBuilder result = new StringBuilder(Formatting.GRAY + "Блоки в BlockESP:");
      boolean first = true;
      for (String id : map.keySet()) {
         result.append(first ? Formatting.GRAY + " " + Formatting.WHITE : Formatting.GRAY + ", " + Formatting.WHITE).append(id);
         first = false;
      }
      MessageUtil.displayInfo(result.toString());
      return 1;
   }

   private Block findBlock(String input) {
      String normalized = input.toLowerCase(Locale.ROOT);
      for (Block block : Registries.BLOCK) {
         String id = BlockMapSetting.getId(block);
         if (id.equals(normalized) || id.equals("minecraft:" + normalized)) {
            return block;
         }
      }
      for (Block block : Registries.BLOCK) {
         String id = BlockMapSetting.getId(block);
         if (id.contains(normalized)) {
            return block;
         }
      }
      return null;
   }

   private Integer parseColor(String input) {
      String s = input.toLowerCase(Locale.ROOT);
      if (COLORS.containsKey(s)) {
         return COLORS.get(s);
      }
      String hex = s.startsWith("#") ? s.substring(1) : s;
      if (hex.length() == 6) {
         try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
         } catch (NumberFormatException var5) {
            return null;
         }
      }
      if (hex.length() == 3) {
         try {
            int r = Integer.parseInt(hex.substring(0, 1), 16);
            int g = Integer.parseInt(hex.substring(1, 2), 16);
            int b = Integer.parseInt(hex.substring(2, 3), 16);
            return 0xFF000000 | (r << 20) | (r << 16) | (g << 12) | (g << 8) | (b << 4) | b;
         } catch (NumberFormatException var6) {
            return null;
         }
      }
      return null;
   }

   private SuggestionProvider<CommandSource> blockSuggestions() {
      return (context, builder) -> {
         String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
         for (Block block : Registries.BLOCK) {
            String id = BlockMapSetting.getId(block);
            if (id.contains(remaining)) {
               builder.suggest(id);
            }
         }
         return builder.buildFuture();
      };
   }

   private SuggestionProvider<CommandSource> addedBlockSuggestions() {
      return (context, builder) -> {
         String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
         for (String id : BlockESP.INSTANCE.getBlocks().getBlocks().keySet()) {
            if (id.toLowerCase(Locale.ROOT).contains(remaining)) {
               builder.suggest(id);
            }
         }
         return builder.buildFuture();
      };
   }

   private SuggestionProvider<CommandSource> colorSuggestions() {
      return (context, builder) -> {
         String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
         for (String name : COLORS.keySet()) {
            if (name.contains(remaining)) {
               builder.suggest(name);
            }
         }
         return builder.buildFuture();
      };
   }
}
