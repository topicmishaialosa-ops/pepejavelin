package org.blade;

import net.fabricmc.api.ModInitializer;
import org.blade.command.CommandManager;
import org.blade.event.EventManager;
import org.blade.event.EventTarget;
import org.blade.event.events.lifecycle.ClientStartEvent;
import org.blade.event.events.lifecycle.WorldLeaveEvent;
import org.blade.feature.FeatureManager;
import org.blade.utils.FriendManager;
import org.blade.hud.NotificationsElement;
import org.blade.menu.MenuKeyHandler;
import org.blade.menu.MenuOverlayRenderHandler;
import org.blade.menu.i18n.UiLanguage;
import org.blade.pve.PveAutomationCoordinator;
import org.blade.pve.PvpStateTracker;
import org.blade.utils.AccountSwitcher;
import org.blade.utils.combat.ServerSprintTracker;
import org.blade.utils.combat.ServerTickSync;
import org.blade.utils.ScreenNbtParser;
import org.blade.utils.inventory.DropAllInventoryController;
import org.blade.utils.inventory.InventorySwap;
import org.blade.utils.render.EntityEspStateCache;
import org.blade.utils.render.Textures;
import org.blade.utils.render.gui.GuiTexture;
import org.blade.utils.render.world.WorldEffects;

public class Blade implements ModInitializer {
    private final MenuKeyHandler menuKeyHandler = new MenuKeyHandler();
    private final MenuOverlayRenderHandler menuOverlayRenderHandler = new MenuOverlayRenderHandler();

    @Override
    public void onInitialize() {
        UiLanguage.loadSaved();
        FriendManager.INSTANCE.initialize();
        FeatureManager.INSTANCE.initialize();
        CommandManager.INSTANCE.initialize();
        WorldEffects.bootstrap();
        EventManager.subscribe(FeatureManager.INSTANCE);
        EventManager.subscribe(CommandManager.INSTANCE);
        EventManager.subscribe(FriendManager.INSTANCE);
        EventManager.subscribe(new NotificationsElement.ToggleListener());
        EventManager.subscribe(menuKeyHandler);
        EventManager.subscribe(menuOverlayRenderHandler);
        EventManager.subscribe(InventorySwap.INSTANCE);
        EventManager.subscribe(DropAllInventoryController.INSTANCE);
        EventManager.subscribe(ServerTickSync.INSTANCE);
        EventManager.subscribe(ServerSprintTracker.INSTANCE);
        EventManager.subscribe(PveAutomationCoordinator.INSTANCE);
        EventManager.subscribe(PvpStateTracker.INSTANCE);
        EventManager.subscribe(ScreenNbtParser.INSTANCE);
        EventManager.subscribe(this);
    }

    @EventTarget
    public void onClientStart(ClientStartEvent event) {
        AccountSwitcher.applyStartupAccount();

        GuiTexture.prewarm(Textures.all());
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {

        EntityEspStateCache.clear();
    }
}
