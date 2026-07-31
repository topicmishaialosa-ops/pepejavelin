package tech.huihui.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.base.comand.impl.args.FriendArgumentType;
import tech.huihui.base.comand.impl.args.PlayerArgumentType;
import tech.huihui.utility.game.other.MessageUtil;

public class StaffCommand extends CommandAbstract {
   public StaffCommand() {
      super("friend");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("add").then(arg("player", PlayerArgumentType.create()).executes((context) -> {
         String name = (String)context.getArgument("player", String.class);
         if (HuihuiClient.getInstance().getStaffManager().getItems().contains(name)) {
            MessageUtil.displayMessage(MessageUtil.LogLevel.WARN, "Уже добавлен " + name);
            return 1;
         } else {
            HuihuiClient.getInstance().getStaffManager().add(name);
            MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, "Добавили " + name);
            return 1;
         }
      })));
      builder.then(literal("remove").then(arg("player", FriendArgumentType.create()).executes((context) -> {
         String nickname = (String)context.getArgument("player", String.class);
         HuihuiClient.getInstance().getStaffManager().remove(nickname);
         MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, nickname + " удален из стаффа");
         return 1;
      })));
      builder.then(literal("list").executes((commandContext) -> {
         MessageUtil.displayMessage(MessageUtil.LogLevel.INFO, HuihuiClient.getInstance().getStaffManager().getItems().toString());
         return 1;
      }));
   }
}
