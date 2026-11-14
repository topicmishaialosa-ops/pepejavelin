package org.blade.command.impl;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.blade.command.ClientCommand;
import org.blade.context.MinecraftContext;
import org.blade.utils.text.ChatUtil;

import java.util.List;

public final class VclipCommand extends ClientCommand implements MinecraftContext {
    public VclipCommand() {
        super("vclip", "Teleports you vertically through blocks", ":arrow_up_down:");
    }

    @Override
    public List<String> aliases() {
        return List.of("vc");
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        builder.executes(context -> {
            ChatUtil.usage("vclip <distance>  •  up  •  down");
            return 1;
        });
        builder.then(LiteralArgumentBuilder.literal("up").executes(context -> scan(1)));
        builder.then(LiteralArgumentBuilder.literal("down").executes(context -> scan(-1)));
        builder.then(RequiredArgumentBuilder.<Object, Double>argument("distance", DoubleArgumentType.doubleArg())
                .executes(context -> clip(DoubleArgumentType.getDouble(context, "distance"))));
    }

    private int clip(double distance) {
        LocalPlayer player = player();
        if (player == null) {
            ChatUtil.error("Not in game");
            return 0;
        }
        if (distance == 0.0 || Double.isNaN(distance) || Double.isInfinite(distance)) {
            ChatUtil.error("Distance must be a non-zero number");
            return 0;
        }
        teleport(player, player.getY() + distance, distance);
        return 1;
    }

    private int scan(int direction) {
        LocalPlayer player = player();
        if (player == null || level() == null) {
            ChatUtil.error("Not in game");
            return 0;
        }
        BlockPos feet = player.blockPosition();
        int fromY = feet.getY();
        int y = fromY + direction;
        int limit = direction > 0 ? level().getMaxY() - 2 : level().getMinY() + 1;
        while (direction > 0 ? y <= limit : y >= limit) {
            BlockPos pos = new BlockPos(feet.getX(), y, feet.getZ());
            if (!level().hasChunkAt(pos)) {
                break;
            }
            if (isStandable(pos)) {
                teleport(player, y, y - fromY);
                return 1;
            }
            y += direction;
        }
        ChatUtil.error(direction > 0 ? "No open space above" : "No open space below");
        return 0;
    }

    private boolean isStandable(BlockPos feetPos) {
        return level().getBlockState(feetPos).isAir()
                && level().getBlockState(feetPos.above()).isAir()
                && !level().getBlockState(feetPos.below()).isAir();
    }

    private void teleport(LocalPlayer player, double y, double delta) {
        double fromY = player.getY();
        double x = player.getX();
        double z = player.getZ();

        player.setDeltaMovement(Vec3.ZERO);
        player.setPos(x, y, z);
        player.resetFallDistance();
        player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, player.horizontalCollision));

        ChatUtil.info(String.format("VClip %+.0f  •  Y %.1f → %.1f", delta, fromY, y));
    }
}
