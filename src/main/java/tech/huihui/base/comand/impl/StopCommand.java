package tech.huihui.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.client.modules.impl.misc.Autofarm;
import tech.huihui.utility.game.other.MessageUtil;

public class StopCommand extends CommandAbstract {
   public StopCommand() {
      super("stop");
   }

   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         Autofarm.INSTANCE.setToggled(false);
         MessageUtil.displayInfo("Бот остановлен");
         return 1;
      });
   }
}
