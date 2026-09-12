package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import tech.huihui.base.bots.InstanceBotManager;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.utility.game.other.MessageUtil;

public class BotFocusScreenCommand extends CommandAbstract {
   public BotFocusScreenCommand() {
      super("botfocusscreen");
   }

   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(literal("off").executes(ctx -> {
         InstanceBotManager.INSTANCE.releaseFocusWithMessage();
         return 1;
      }));
      builder.then(arg("name", StringArgumentType.greedyString()).executes(ctx -> {
         String name = ctx.getArgument("name", String.class);
         InstanceBotManager.INSTANCE.focusBot(name);
         return 1;
      }));
      builder.executes(ctx -> {
         MessageUtil.displayInfo("Использование: .botfocusscreen <ник> | .botfocusscreen off");
         return 1;
      });
   }
}