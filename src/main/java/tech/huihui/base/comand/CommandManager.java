package tech.huihui.base.comand;

import com.mojang.brigadier.CommandDispatcher;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.base.comand.impl.ClipCommand;
import tech.huihui.base.comand.impl.ConfigCommand;
import tech.huihui.base.comand.impl.FarmCommand;
import tech.huihui.base.comand.impl.FriendCommand;
import tech.huihui.base.comand.impl.GPSCommand;
import tech.huihui.base.comand.impl.HelpCommand;
import tech.huihui.base.comand.impl.MacroCommand;
import tech.huihui.base.comand.impl.PilotCommand;
import tech.huihui.base.comand.impl.RCTCommand;
import tech.huihui.base.comand.impl.StopCommand;

public class CommandManager {
   private String prefix = ".";
   private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher();
   private final CommandSource source = new ClientCommandSource((ClientPlayNetworkHandler)null, MinecraftClient.getInstance());
   private final List<CommandAbstract> commands = new ArrayList();

   public CommandManager() {
      this.register();
   }

    @Native
    private void register() {
       this.registerCommand(new FriendCommand());
       this.registerCommand(new MacroCommand());
       this.registerCommand(new ClipCommand());
       this.registerCommand(new ConfigCommand());
       this.registerCommand(new RCTCommand());
       this.registerCommand(new GPSCommand());
       this.registerCommand(new FarmCommand());
       this.registerCommand(new StopCommand());
       this.registerCommand(new HelpCommand());
       this.registerCommand(new PilotCommand());
    }

   @Native
   public void registerCommand(CommandAbstract command) {
      if (command != null) {
         command.register(this.dispatcher);
         this.commands.add(command);
      }
   }

   @Generated
   public String getPrefix() {
      return this.prefix;
   }

   @Generated
   public CommandDispatcher<CommandSource> getDispatcher() {
      return this.dispatcher;
   }

   @Generated
   public CommandSource getSource() {
      return this.source;
   }

   @Generated
   public List<CommandAbstract> getCommands() {
      return this.commands;
   }
}
