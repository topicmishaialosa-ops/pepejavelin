package tech.huihui.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.client.modules.impl.misc.Autofarm;
import tech.huihui.utility.game.other.MessageUtil;

public class FarmCommand extends CommandAbstract {
   public FarmCommand() {
      super("farm");
   }

   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         Autofarm.INSTANCE.setToggled(true);
         MessageUtil.displayInfo("Запускаю сбор урожая (#farm)");
         return 1;
      });
   }
}
