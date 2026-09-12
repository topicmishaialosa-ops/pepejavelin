package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.mixin.accessor.LivingEntityAccessor;
import org.blade.mixin.accessor.MinecraftAccessor;
import org.blade.mixin.accessor.MultiPlayerGameModeAccessor;

public final class NoDelaysFeature extends Feature {
    private static final int DEFAULT_JUMP_DELAY = 10;
    private static final int DEFAULT_RIGHT_CLICK_DELAY = 4;
    private static final int DEFAULT_BLOCK_BREAK_DELAY = 5;

    public final BooleanSetting jump = register(new BooleanSetting("Jump", true));
    public final BooleanSetting rightClick = register(new BooleanSetting("Right Click", false));
    public final BooleanSetting experienceBottlesOnly = register(new BooleanSetting(
            "Experience Bottles Only", false
    ).visibleWhen(this.rightClick::getValue));
    public final BooleanSetting blockBreak = register(new BooleanSetting("Block Break", false));

    public NoDelaysFeature() {
        super("NoDelays", "Removes selected player action delays", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft client = event.getClient();
        if (client.player == null) {
            return;
        }

        if (jump.getValue()) {
            ((LivingEntityAccessor) client.player).setNoJumpDelay(0);
        }
        if (rightClick.getValue()
                && (!this.experienceBottlesOnly.getValue()
                || client.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)
                || client.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE))) {
            ((MinecraftAccessor) client).setRightClickDelay(0);
        }
        if (blockBreak.getValue() && client.gameMode != null) {
            ((MultiPlayerGameModeAccessor) client.gameMode).setDestroyDelay(0);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            ((LivingEntityAccessor) client.player).setNoJumpDelay(DEFAULT_JUMP_DELAY);
        }
        ((MinecraftAccessor) client).setRightClickDelay(DEFAULT_RIGHT_CLICK_DELAY);
        if (client.gameMode != null) {
            ((MultiPlayerGameModeAccessor) client.gameMode).setDestroyDelay(DEFAULT_BLOCK_BREAK_DELAY);
        }
    }
}
