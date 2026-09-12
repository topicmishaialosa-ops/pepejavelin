package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.utils.text.ChatUtil;

public final class InstanceBotFeature extends Feature {
    private boolean deferred;

    public InstanceBotFeature() {
        super("InstanceBot", "Окно управления ботами", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) {
            this.deferred = true;
        } else {
            ChatUtil.info("InstanceBot: экран не портирован (требуется InstanceBotScreen)");
        }
    }

    @Override
    public void onDisable() {
        this.deferred = false;
        super.onDisable();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        if (this.deferred && Minecraft.getInstance().gui.screen() == null) {
            this.deferred = false;
            ChatUtil.info("InstanceBot: экран не портирован (требуется InstanceBotScreen)");
        }
    }
}
