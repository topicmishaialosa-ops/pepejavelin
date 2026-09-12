package org.blade.feature;

import org.blade.event.EventTarget;
import org.blade.event.events.input.KeyboardInputEvent;
import org.blade.event.events.input.MouseInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.blade.event.events.lifecycle.ShutdownEvent;
import org.blade.feature.impl.combat.AuraFeature;
import org.blade.feature.impl.combat.AutoClickerFeature;
import org.blade.feature.impl.combat.AutoTotemFeature;
import org.blade.feature.impl.combat.BowAimbotFeature;
import org.blade.feature.impl.combat.HitBoxesFeature;
import org.blade.feature.impl.combat.TriggerBotFeature;
import org.blade.feature.impl.combat.VelocityFeature;
import org.blade.feature.impl.combat.AntiBotFeature;
import org.blade.feature.impl.combat.AutoMaceFeature;
import org.blade.feature.impl.combat.AutoExplosionFeature;
import org.blade.feature.impl.combat.SurroundFeature;
import org.blade.feature.impl.combat.TargetPearlFeature;
import org.blade.feature.impl.combat.TpInflFeature;
import org.blade.feature.impl.misc.AuctionHelperFeature;
import org.blade.feature.impl.misc.AutoTpaAcceptFeature;
import org.blade.feature.impl.misc.ChestStealerFeature;
import org.blade.feature.impl.misc.DeathCoordsFeature;
import org.blade.feature.impl.misc.NameProtectFeature;
import org.blade.feature.impl.misc.NoDelaysFeature;
import org.blade.feature.impl.misc.ServerHelperFeature;
import org.blade.feature.impl.misc.XCarryFeature;
import org.blade.feature.impl.misc.ClickFriendFeature;
import org.blade.feature.impl.misc.ItemScrollerFeature;
import org.blade.feature.impl.misc.NoInteractFeature;
import org.blade.feature.impl.misc.ScoreboardHealthFeature;
import org.blade.feature.impl.misc.InstanceBotFeature;
import org.blade.feature.impl.misc.AutofarmFeature;
import org.blade.feature.impl.misc.AutoKitFeature;
import org.blade.feature.impl.misc.AutoDuelFeature;
import org.blade.feature.impl.misc.ElytraHelperFeature;
import org.blade.feature.impl.misc.RenamePasterClientFeature;
import org.blade.feature.impl.misc.FreeCamFeature;
import org.blade.feature.impl.misc.KeyFinderFeature;
import org.blade.feature.impl.misc.AutoMineFunTimeFeature;
import org.blade.feature.impl.misc.AutoBuyFeature;
import org.blade.feature.impl.misc.BpAutoFeature;
import org.blade.feature.impl.misc.CollectorFeature;
import org.blade.feature.impl.misc.DiscordRpcFeature;
import org.blade.feature.impl.misc.VibeCoding;
import org.blade.feature.impl.misc.ZarabotokReallyWorld;
import org.blade.feature.impl.movement.AutoJumpFeature;
import org.blade.feature.impl.movement.GrimCollideFeature;
import org.blade.feature.impl.movement.GrimElytraFeature;
import org.blade.feature.impl.movement.GrimFlyFeature;
import org.blade.feature.impl.movement.InventoryMoveFeature;
import org.blade.feature.impl.movement.SpeedFeature;
import org.blade.feature.impl.movement.NoPushFeature;
import org.blade.feature.impl.movement.ParkourFeature;
import org.blade.feature.impl.movement.SafeWalkFeature;
import org.blade.feature.impl.movement.SprintFeature;
import org.blade.feature.impl.movement.TimerFeature;
import org.blade.feature.impl.movement.WallClimbFeature;
import org.blade.feature.impl.movement.WaterSpeedFeature;
import org.blade.feature.impl.player.AutoRespawnFeature;
import org.blade.feature.impl.player.AutoSwapFeature;
import org.blade.feature.impl.player.AutoToolFeature;
import org.blade.feature.impl.player.ClickPearlFeature;
import org.blade.feature.impl.player.FullBrightFeature;
import org.blade.feature.impl.player.MultiActionFeature;
import org.blade.feature.impl.player.NoJumpBoostFeature;
import org.blade.feature.impl.pve.*;
import org.blade.feature.impl.pve.autowarden.AutoWardenFeature;
import org.blade.feature.impl.visual.ArrowsFeature;
import org.blade.feature.impl.visual.BlockOutlineFeature;
import org.blade.feature.impl.visual.ChamsFeature;
import org.blade.feature.impl.visual.CrosshairFeature;
import org.blade.feature.impl.visual.EditClickGUI;
import org.blade.feature.impl.visual.EntityEspFeature;
import org.blade.feature.impl.visual.HitParticlesFeature;
import org.blade.feature.impl.visual.HoldMyItemsFeature;
import org.blade.feature.impl.visual.HudFeature;
import org.blade.feature.impl.visual.ItemPhysicsFeature;
import org.blade.feature.impl.visual.JumpCirclesFeature;
import org.blade.feature.impl.visual.NameTagsFeature;
import org.blade.feature.impl.visual.PopChamsFeature;
import org.blade.feature.impl.visual.RemovalsFeature;
import org.blade.feature.impl.visual.ShaderHandsFeature;
import org.blade.feature.impl.visual.SupplyHologramsFeature;
import org.blade.feature.impl.visual.SwingAnimationFeature;
import org.blade.feature.impl.visual.targethud.TargetHudFeature;
import org.blade.feature.impl.visual.TrajectoriesFeature;
import org.blade.feature.impl.visual.ViewModelFeature;
import org.blade.feature.impl.visual.WorldTweaksFeature;
import org.blade.feature.impl.visual.WorldParticlesFeature;
import org.blade.feature.setting.BindSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class FeatureManager {
    public static final FeatureManager INSTANCE = new FeatureManager();

    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Map<Class<? extends Feature>, Feature> featuresByType = new ConcurrentHashMap<>();
    private final FeatureConfigStore configStore = new FeatureConfigStore();
    private final ScheduledExecutorService configWriter = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Blade Config Writer");
        thread.setDaemon(true);
        return thread;
    });
    private final Object saveLock = new Object();
    private final Map<Feature, Set<Integer>> activeHoldBinds = new IdentityHashMap<>();
    private ScheduledFuture<?> pendingSave;
    private boolean initialized;

    private FeatureManager() {
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        List.of(
                new SprintFeature(),
                new SpeedFeature(),
                new AutoJumpFeature(),
                new GrimFlyFeature(),
                new InventoryMoveFeature(),
                new NoPushFeature(),
                new SafeWalkFeature(),
                new WallClimbFeature(),
                new WaterSpeedFeature(),
                new ParkourFeature(),
                new TimerFeature(),
                new GrimCollideFeature(),
                new GrimElytraFeature(),
                new AuraFeature(),
                new TriggerBotFeature(),
                new AutoTotemFeature(),
                new AutoClickerFeature(),
                new HitBoxesFeature(),
                new VelocityFeature(),
                new BowAimbotFeature(),
                AntiBotFeature.INSTANCE,
                AutoMaceFeature.INSTANCE,
                AutoExplosionFeature.INSTANCE,
                SurroundFeature.INSTANCE,
                TargetPearlFeature.INSTANCE,
                TpInflFeature.INSTANCE,
                new HudFeature(),
                new TargetHudFeature(),
                new ArrowsFeature(),
                new EntityEspFeature(),
                new NameTagsFeature(),
                new SupplyHologramsFeature(),
                new HitParticlesFeature(),
                new JumpCirclesFeature(),
                new TrajectoriesFeature(),
                new WorldParticlesFeature(),
                new PopChamsFeature(),
                new RemovalsFeature(),
                new CrosshairFeature(),
                EditClickGUI.INSTANCE,
                SwingAnimationFeature.INSTANCE,
                new ViewModelFeature(),
                new ShaderHandsFeature(),
                new BlockOutlineFeature(),
                new ChamsFeature(),
                new HoldMyItemsFeature(),
                new ItemPhysicsFeature(),
                new WorldTweaksFeature(),
                new NameProtectFeature(),
                new NoDelaysFeature(),
                new DeathCoordsFeature(),
                new XCarryFeature(),
                new AuctionHelperFeature(),
                new ServerHelperFeature(),
                new AutoTpaAcceptFeature(),
                new ChestStealerFeature(),
                new ClickFriendFeature(),
                new ItemScrollerFeature(),
                new NoInteractFeature(),
                new ScoreboardHealthFeature(),
                new InstanceBotFeature(),
                new AutofarmFeature(),
                new AutoKitFeature(),
                new AutoDuelFeature(),
                new ElytraHelperFeature(),
                new FreeCamFeature(),
                new RenamePasterClientFeature(),
                new KeyFinderFeature(),
                new AutoMineFunTimeFeature(),
                AutoBuyFeature.INSTANCE,
                CollectorFeature.INSTANCE,
                BpAutoFeature.INSTANCE,
                new DiscordRpcFeature(),
                VibeCoding.INSTANCE,
                ZarabotokReallyWorld.INSTANCE,
                new AutoSwapFeature(),
                new AutoRespawnFeature(),
                new FullBrightFeature(),
                new NoJumpBoostFeature(),
                new AutoToolFeature(),
                new ClickPearlFeature(),
                new MultiActionFeature(),
                PveManagerFeature.INSTANCE,
                new AutoFishFeature(),
                new AutoArmorFeature(),
                new AutoPotionFeature(),
                new AutoUseFeature(),
                new AutoGappleFeature(),
                new AntiAfkFeature(),
                new AutoLeaveFeature(),
                new AutoAuthFeature(),
                new NukerFeature(),
                new MineHelperFeature(),
                new AutoTpLootFeature(),
                new AutoMineFeature(),
                new BaseFinderFeature(),
                new AppleFarmerFeature(),
                new CreeperFarmFeature(),
                new AutoCrafterFeature(),
                new AutoTradeFeature(),
                new AuctionRelistFeature(),
                new ClanInvestFeature(),
                new ClanUpgradeFeature(),
                new GriefJoinerFeature(),
                new SynchronizationFeature(),
                new AutoWardenFeature()
        ).forEach(this::register);

        configStore.load(this);
        initialized = true;

        Runtime.getRuntime().addShutdownHook(new Thread(this::flushPendingSave, "Blade Config Flush"));
    }

    public void save() {
        if (!initialized) {
            return;
        }
        var snapshot = configStore.snapshot(this);
        synchronized (saveLock) {
            if (pendingSave != null) {
                pendingSave.cancel(false);
            }
            pendingSave = configWriter.schedule(() -> configStore.save(snapshot), 2L, TimeUnit.SECONDS);
        }
    }

    private void flushPendingSave() {
        synchronized (saveLock) {
            if (pendingSave == null) {
                return;
            }
            pendingSave.cancel(false);
            pendingSave = null;
        }
        configStore.save(configStore.snapshot(this));
    }

    public boolean saveConfigAs(String name) {
        return initialized && configStore.saveNamed(name, configStore.snapshot(this));
    }

    public boolean loadConfig(String name) {
        if (!initialized || !configStore.loadNamed(this, name)) {
            return false;
        }
        save();
        return true;
    }

    public boolean deleteConfig(String name) {
        return configStore.deleteNamed(name);
    }

    public List<String> configNames() {
        return configStore.listNamed();
    }

    public void register(Feature feature) {
        String key = normalize(feature.getName());
        if (features.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate feature name: " + feature.getName());
        }

        feature.setStateListener(this::save);
        features.put(key, feature);
        featuresByType.put(feature.getClass(), feature);
    }

    public Collection<Feature> getFeatures() {
        return Collections.unmodifiableCollection(features.values());
    }

    public List<Feature> getFeatures(FeatureCategory category) {
        List<Feature> result = new ArrayList<>();
        for (Feature feature : features.values()) {
            if (feature.getCategory() == category) {
                result.add(feature);
            }
        }
        return result;
    }

    public Feature getFeature(String name) {
        return features.get(normalize(name));
    }

    public <T extends Feature> T getFeature(Class<T> type) {
        return type.cast(featuresByType.get(type));
    }

    public <T extends Feature> T getEnabled(Class<T> type) {
        T feature = getFeature(type);
        return feature != null && feature.isEnabled() ? feature : null;
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null && !(mc.gui.screen() instanceof AbstractContainerScreen)) {
            return;
        }
        dispatchBind(BindSetting.key(event.getKey()), event.getAction());
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null && !(mc.gui.screen() instanceof AbstractContainerScreen)) {
            return;
        }
        dispatchBind(BindSetting.mouse(event.getButton()), event.getAction());
    }

    @EventTarget
    public void onShutdown(ShutdownEvent event) {
        synchronized (saveLock) {
            if (pendingSave != null) {
                pendingSave.cancel(false);
                pendingSave = null;
            }
        }
        configStore.save(configStore.snapshot(this));
        configWriter.shutdown();
        initialized = false;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private void dispatchBind(int bindCode, int action) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) {
            return;
        }
        for (Feature feature : features.values()) {
            if (feature.supportsBinds()
                    && feature.isToggleable()
                    && feature.getBind().matchesCode(bindCode)) {
                handleBindInput(feature, bindCode, action);
            }
        }
    }

    private void handleBindInput(Feature feature, int bindCode, int action) {
        if (feature.getBindModeForCode(bindCode) == BindMode.HOLD) {
            Set<Integer> activeCodes = this.activeHoldBinds.computeIfAbsent(feature, ignored -> new LinkedHashSet<>());
            if (action == GLFW.GLFW_PRESS) {
                if (activeCodes.add(bindCode)) {
                    feature.setEnabled(true);
                }
                return;
            }

            activeCodes.remove(bindCode);
            if (activeCodes.isEmpty()) {
                this.activeHoldBinds.remove(feature);
                feature.setEnabled(false);
            }
            return;
        }

        if (action == GLFW.GLFW_PRESS) {
            feature.toggle();
        }
    }
}
