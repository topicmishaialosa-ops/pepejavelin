package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.utils.text.ChatUtil;

public final class AutofarmFeature extends Feature {
    private boolean farmStarted;
    private boolean baritoneMissing;

    public AutofarmFeature() {
        super("Autofarm", "Запускает сбор урожая через Baritone (#farm)", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Minecraft mc = Minecraft.getInstance();
        if (!isBaritonePresent()) {
            ChatUtil.info("Скачайте Baritone, чтобы использовать Autofarm");
            this.baritoneMissing = true;
            return;
        }
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendChat("#farm");
            this.farmStarted = true;
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (this.farmStarted && mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendChat("#stop");
        }
        this.farmStarted = false;
        super.onDisable();
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        if (this.baritoneMissing) {
            this.baritoneMissing = false;
            this.setEnabled(false);
        }
    }

    private boolean isBaritonePresent() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}