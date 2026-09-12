package org.blade.feature.impl.misc;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import org.blade.event.EventTarget;
import org.blade.event.events.screen.ScreenOpenEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;

public final class NoInteractFeature extends Feature {
    private final BooleanSetting onlyOnPvP = register(new BooleanSetting("Только в PvP", false));

    public NoInteractFeature() {
        super("NoInteract", "Не дает открыть контейнер", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onScreenOpen(ScreenOpenEvent event) {
        if (event.getScreen() instanceof AbstractContainerScreen) {
            if (!this.onlyOnPvP.getValue()) {
                event.cancel();
            }
        }
    }
}
