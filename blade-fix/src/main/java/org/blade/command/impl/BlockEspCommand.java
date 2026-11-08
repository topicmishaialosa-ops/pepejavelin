package org.blade.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.blade.command.ClientCommand;
import org.blade.context.MinecraftContext;
import org.blade.feature.impl.visual.BlockESPFeature;
import org.blade.feature.setting.BlockMapSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.text.ChatUtil;

public final class BlockEspCommand extends ClientCommand implements MinecraftContext {
    public BlockEspCommand() {
        super("blockesp", "Add/remove blocks to/from BlockESP", ":package:");
    }

    @Override
    public void build(LiteralArgumentBuilder<Object> builder) {
        builder.executes(context -> usage());
        builder.then(LiteralArgumentBuilder.literal("add")
                .executes(context -> addLookedAt())
                .then(RequiredArgumentBuilder.<Object, String>argument("block", StringArgumentType.word())
                        .executes(context -> addByName(StringArgumentType.getString(context, "block")))));
        builder.then(LiteralArgumentBuilder.literal("remove")
                .executes(context -> removeLookedAt())
                .then(RequiredArgumentBuilder.<Object, String>argument("block", StringArgumentType.word())
                        .executes(context -> removeByName(StringArgumentType.getString(context, "block")))));
        builder.then(LiteralArgumentBuilder.literal("clear").executes(context -> clear()));
    }

    private int usage() {
        ChatUtil.usage(".blockesp add [block]  •  remove [block]  •  clear");
        return 1;
    }

    private int addLookedAt() {
        Block block = lookedAtBlock();
        if (block == null) {
            ChatUtil.error("No block in reach  •  try .blockesp add <block>");
            return 0;
        }
        addBlock(block);
        return 1;
    }

    private int removeLookedAt() {
        Block block = lookedAtBlock();
        if (block == null) {
            ChatUtil.error("No block in reach  •  try .blockesp remove <block>");
            return 0;
        }
        removeBlock(block);
        return 1;
    }

    private int addByName(String input) {
        Block block = BlockMapSetting.blockFromId(resolveId(input));
        if (block == null || block == Blocks.AIR) {
            ChatUtil.error("Unknown block  •  " + input);
            return 0;
        }
        addBlock(block);
        return 1;
    }

    private int removeByName(String input) {
        Block block = BlockMapSetting.blockFromId(resolveId(input));
        if (block == null || block == Blocks.AIR) {
            ChatUtil.error("Unknown block  •  " + input);
            return 0;
        }
        removeBlock(block);
        return 1;
    }

    private static Block lookedAtBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        HitResult hit = mc.player.pick(5.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockPos pos = blockHit.getBlockPos();
        return mc.level.getBlockState(pos).getBlock();
    }

    private void addBlock(Block block) {
        BlockMapSetting setting = BlockESPFeature.INSTANCE.getBlocks();
        String id = BlockMapSetting.getId(block);
        if (setting.contains(id)) {
            ChatUtil.info(blockName(block) + " already in BlockESP");
            return;
        }
        setting.set(id, ColorUtil.withAlpha(ColorUtil.rgb(0, 255, 128), 120));
        BlockESPFeature.INSTANCE.markDirty();
        ChatUtil.success(blockName(block) + " added to BlockESP");
    }

    private void removeBlock(Block block) {
        BlockMapSetting setting = BlockESPFeature.INSTANCE.getBlocks();
        String id = BlockMapSetting.getId(block);
        if (!setting.contains(id)) {
            ChatUtil.info(blockName(block) + " not in BlockESP");
            return;
        }
        setting.remove(id);
        BlockESPFeature.INSTANCE.markDirty();
        ChatUtil.success(blockName(block) + " removed from BlockESP");
    }

    private int clear() {
        BlockMapSetting setting = BlockESPFeature.INSTANCE.getBlocks();
        if (setting.isEmpty()) {
            ChatUtil.error("BlockESP is already empty");
            return 0;
        }
        int count = setting.getBlocks().size();
        for (String id : setting.getBlocks().keySet()) {
            setting.remove(id);
        }
        BlockESPFeature.INSTANCE.markDirty();
        ChatUtil.success("Cleared " + count + " blocks from BlockESP");
        return 1;
    }

    private static String resolveId(String input) {
        return input.contains(":") ? input : "minecraft:" + input;
    }

    private static String blockName(Block block) {
        String id = BlockMapSetting.getId(block);
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }
}