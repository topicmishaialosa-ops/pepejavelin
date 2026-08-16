package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.LinkedHashSet;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.autobuy.item.ItemBuy;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.utility.game.other.MessageUtil;

public class AhSearchCommand extends CommandAbstract {
   public AhSearchCommand() {
      super("ahsearch");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      SuggestionProvider<CommandSource> itemNames = (context, suggestionsBuilder) -> {
         LinkedHashSet<String> names = new LinkedHashSet<>();
         for (ItemBuy item : HuihuiClient.getInstance().getAutoBuyManager().getFuntime()) {
            names.add(item.getDisplayName());
            if (item.getSearchName() != null && !item.getSearchName().equals(item.getDisplayName())) {
               names.add(item.getSearchName());
            }
         }
         for (ItemBuy item : HuihuiClient.getInstance().getAutoBuyManager().getVanilla()) {
            names.add(item.getDisplayName());
            if (item.getSearchName() != null && !item.getSearchName().equals(item.getDisplayName())) {
               names.add(item.getSearchName());
            }
         }
         for (String name : names) {
            suggestionsBuilder.suggest(name);
         }
         return suggestionsBuilder.buildFuture();
      };
      builder.executes((context) -> {
         if (mc.player == null || mc.getNetworkHandler() == null) {
            MessageUtil.displayInfo("[AH] Сначала зайди в мир");
            return 0;
         }
         String hand = this.handItemName();
         if (hand == null) {
            MessageUtil.displayInfo("[AH] В руке нет предмета — укажи название: .ahsearch <предмет>");
            return 0;
         }
         mc.getNetworkHandler().sendChatCommand("ah search " + hand);
         MessageUtil.displayInfo("[AH] Поиск: " + hand);
         return 1;
      });
      builder.then(arg("предмет", StringArgumentType.greedyString()).suggests(itemNames).executes((context) -> {
         String name = context.getArgument("предмет", String.class);
         if (mc.player == null || mc.getNetworkHandler() == null) {
            MessageUtil.displayInfo("[AH] Сначала зайди в мир");
            return 0;
         }
         mc.getNetworkHandler().sendChatCommand("ah search " + name);
         MessageUtil.displayInfo("[AH] Поиск: " + name);
         return 1;
      }));
   }

   private String handItemName() {
      if (mc.player == null || mc.player.getMainHandStack() == null || mc.player.getMainHandStack().isEmpty()) {
         return null;
      }
      String name = mc.player.getMainHandStack().getName().getString().replaceAll("§.", "").trim();
      return name.isEmpty() ? null : name;
   }
}