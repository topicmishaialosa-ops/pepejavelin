package tech.huihui.base.comand.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.base.comand.impl.args.PlayerArgumentType;
import tech.huihui.utility.game.other.MessageUtil;

public class TpCommand extends CommandAbstract {
   public TpCommand() {
      super("tp");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(arg("name", PlayerArgumentType.create()).executes((context) -> {
         String name = (String)context.getArgument("name", String.class);
         PlayerEntity target = this.findPlayer(name);
         if (target == null) {
            MessageUtil.displayInfo("Игрок " + name + " не найден");
            return 0;
         }

         double targetX = target.getX();
         double targetY = target.getY();
         double targetZ = target.getZ();

         for(int i = 0; i < 3; ++i) {
            mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
         }

         mc.player.networkHandler.sendPacket(new PositionAndOnGround(targetX, targetY, targetZ, false, mc.player.horizontalCollision));
         mc.player.setPosition(targetX, targetY, targetZ);
         MessageUtil.displayInfo("Телепортация к " + name + " выполнена");
         return 1;
      }));
   }

   private PlayerEntity findPlayer(String name) {
      if (mc.world == null) {
         return null;
      }

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player != null && player.getGameProfile().getName().equalsIgnoreCase(name)) {
            return player;
         }
      }

      return null;
   }
}