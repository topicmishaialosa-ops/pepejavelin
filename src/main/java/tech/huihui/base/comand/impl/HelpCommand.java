package tech.huihui.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.utility.game.other.MessageUtil;

public class HelpCommand extends CommandAbstract {
   public HelpCommand() {
      super("help");
   }

   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         MessageUtil.displayInfo("Команды: farm, stop");
         return 1;
      });
   }
}
