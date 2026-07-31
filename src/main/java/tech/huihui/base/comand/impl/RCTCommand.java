package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.base.notify.NotifyManager;
import tech.huihui.base.repository.RCTRepository;
import tech.huihui.utility.game.server.ServerHandler;
import tech.huihui.utility.interfaces.IClient;

public class RCTCommand extends CommandAbstract implements IClient {
   private final RCTRepository repository = HuihuiClient.getInstance().getRCTRepository();

   public RCTCommand() {
      super("rct");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         ServerHandler serverHandler = HuihuiClient.getInstance().getServerHandler();
         if (!serverHandler.isHolyWorld()) {
            NotifyManager.getInstance().addNotification("[RCT]", Text.literal(" Не работает на этом " + String.valueOf(Formatting.RED) + "сервере"));
            return 1;
         } else if (serverHandler.isPvp()) {
            NotifyManager.getInstance().addNotification("️[RCT]", Text.literal(" Вы находитесь в режиме " + String.valueOf(Formatting.RED) + "пвп"));
            return 1;
         } else {
            this.repository.reconnect(serverHandler.getAnarchy());
            return 1;
         }
      });
      builder.then(CommandAbstract.arg("anarchy", IntegerArgumentType.integer(1, 63)).executes((context) -> {
         ServerHandler serverHandler = HuihuiClient.getInstance().getServerHandler();
         if (!serverHandler.isHolyWorld()) {
            NotifyManager.getInstance().addNotification("[RCT]", Text.literal(" Не работает на этом " + String.valueOf(Formatting.RED) + "сервере"));
            return 1;
         } else if (serverHandler.isPvp()) {
            NotifyManager.getInstance().addNotification("[RCT]️", Text.literal(" Вы находитесь в режиме " + String.valueOf(Formatting.RED) + "пвп"));
            return 1;
         } else {
            int anarchy = (Integer)context.getArgument("anarchy", Integer.class);
            this.repository.reconnect(anarchy);
            return 1;
         }
      }));
   }
}
