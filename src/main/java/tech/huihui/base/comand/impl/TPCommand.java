package tech.huihui.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.utility.game.other.MessageUtil;

public class TPCommand extends CommandAbstract {
   public TPCommand() {
      super("tp");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      SuggestionProvider<CommandSource> playerNames = (context, suggestionsBuilder) -> {
         for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            suggestionsBuilder.suggest(player.getGameProfile().getName());
         }
         return suggestionsBuilder.buildFuture();
      };
      builder.then(arg("ник", StringArgumentType.word()).suggests(playerNames).executes((context) -> {
         String targetName = context.getArgument("ник", String.class);
         AbstractClientPlayerEntity target = this.findPlayer(targetName);
         if (target == null) {
            MessageUtil.displayInfo("Игрок " + targetName + " не найден");
            return 0;
         }

         double distance = mc.player.distanceTo(target);
         double targetX = target.getX();
         double targetZ = target.getZ();
         double targetY = this.findSolidBlockY(target);

         if (targetY == Double.MIN_VALUE) {
            MessageUtil.displayInfo("Не удалось найти твердый блок рядом с игроком " + targetName);
            return 0;
         }

         int packetsCount = Math.max((int) (distance / 1000), 3);
         for (int i = 0; i < packetsCount; i++) {
            mc.player.networkHandler.sendPacket(new OnGroundOnly(mc.player.isOnGround(), mc.player.horizontalCollision));
         }

         mc.player.networkHandler.sendPacket(new PositionAndOnGround(targetX, targetY, targetZ, false, mc.player.horizontalCollision));
         mc.player.setPosition(targetX, targetY, targetZ);
         MessageUtil.displayInfo("Телепортация к " + targetName + " выполнена. Координаты: " + (int)targetX + " " + (int)targetY + " " + (int)targetZ);
         return 1;
      }));
   }

   private double findSolidBlockY(AbstractClientPlayerEntity target) {
      BlockPos targetPos = target.getBlockPos();

      for (int y = targetPos.getY() - 1; y >= mc.world.getBottomY(); y--) {
         if (this.isSolid(new BlockPos(targetPos.getX(), y, targetPos.getZ()))) {
            return y + 0.25;
         }
      }

      for (int y = targetPos.getY() + 1; y < mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, targetPos.getX(), targetPos.getZ()); y++) {
         if (this.isSolid(new BlockPos(targetPos.getX(), y, targetPos.getZ()))) {
            return y + 0.25;
         }
      }

      return Double.MIN_VALUE;
   }

   private boolean isSolid(BlockPos pos) {
      if (mc.world == null) {
         return false;
      }

      return !mc.world.getBlockState(pos).isAir() && !mc.world.getBlockState(pos).isReplaceable();
   }

   private AbstractClientPlayerEntity findPlayer(String name) {
      if (mc.world == null) {
         return null;
      }

      for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
         if (player != null && player.getGameProfile().getName().equalsIgnoreCase(name)) {
            return player;
         }
      }
      return null;
   }
}
