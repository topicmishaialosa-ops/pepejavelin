package tech.huihui.base.modules;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.Generated;
import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.base.events.impl.other.EventGameUpdate;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.base.macro.Macro;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.setting.Setting;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting.Value;
import tech.huihui.client.modules.impl.combat.Aimbow;
import tech.huihui.client.modules.impl.combat.AntiBot;
import tech.huihui.client.modules.impl.combat.Aura;
import tech.huihui.client.modules.impl.combat.AutoCrystal;
import tech.huihui.client.modules.impl.combat.AutoGapple;
import tech.huihui.client.modules.impl.combat.AutoSwap;
import tech.huihui.client.modules.impl.combat.AutoTotem;
import tech.huihui.client.modules.impl.combat.ClickPearl;
import tech.huihui.client.modules.impl.combat.Surround;
import tech.huihui.client.modules.impl.combat.TargetPearl;
import tech.huihui.client.modules.impl.cosmetics.AnimalsModule;
import tech.huihui.client.modules.impl.misc.AHHelper;
import tech.huihui.client.modules.impl.misc.AutoAccept;
import tech.huihui.client.modules.impl.misc.AutoDuel;
import tech.huihui.client.modules.impl.misc.AutoWarden;
import tech.huihui.client.modules.impl.misc.BaseFinder;
import tech.huihui.client.modules.impl.misc.AutoJoiner;
import tech.huihui.client.modules.impl.misc.AutoKit;
import tech.huihui.client.modules.impl.misc.AutoRespawn;
import tech.huihui.client.modules.impl.misc.Autofarm;
import tech.huihui.client.modules.impl.misc.ClickAction;
import tech.huihui.client.modules.impl.misc.CreeperFarm;
import tech.huihui.client.modules.impl.misc.DiscordRPC;
import tech.huihui.client.modules.impl.misc.ElytraHelper;
import tech.huihui.client.modules.impl.misc.FreeCam;
import tech.huihui.client.modules.impl.misc.ItemScroller;
import tech.huihui.client.modules.impl.misc.KeyFinder;
import tech.huihui.client.modules.impl.misc.MineHelper;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.client.modules.impl.misc.NoInteract;
import tech.huihui.client.modules.impl.misc.PilotSettings;
import tech.huihui.client.modules.impl.misc.ScoreboardHealth;
import tech.huihui.client.modules.impl.misc.ServerHelper;
import tech.huihui.client.modules.impl.misc.ZarabotokReallyWorld;
import tech.huihui.client.modules.impl.movement.AirStuck;
import tech.huihui.client.modules.impl.movement.AntiElytraTarget;
import tech.huihui.client.modules.impl.movement.AutoSprint;
import tech.huihui.client.modules.impl.movement.ElytraAccelerate;
import tech.huihui.client.modules.impl.movement.ElytraBooster;
import tech.huihui.client.modules.impl.movement.ElytraMotion;
import tech.huihui.client.modules.impl.movement.ElytraRecast;
import tech.huihui.client.modules.impl.movement.Fly;
import tech.huihui.client.modules.impl.movement.GuiWalk;
import tech.huihui.client.modules.impl.movement.Jesus;
import tech.huihui.client.modules.impl.movement.NoSlow;
import tech.huihui.client.modules.impl.movement.NoWeb;
import tech.huihui.client.modules.impl.movement.Scaffold;
import tech.huihui.client.modules.impl.movement.SpiderMatrix;
import tech.huihui.client.modules.impl.movement.Speed;
import tech.huihui.client.modules.impl.movement.TargetStrafe;
import tech.huihui.client.modules.impl.player.AutoArmor;
import tech.huihui.client.modules.impl.player.AutoTool;
import tech.huihui.client.modules.impl.player.Blink;
import tech.huihui.client.modules.impl.player.FastBreak;
import tech.huihui.client.modules.impl.player.NoDelay;
import tech.huihui.client.modules.impl.player.NoPush;
import tech.huihui.client.modules.impl.render.AntiInvisible;
import tech.huihui.client.modules.impl.render.BlockESP;
import tech.huihui.client.modules.impl.render.ChunkAnimator;
import tech.huihui.client.modules.impl.render.Crosshair;
import tech.huihui.client.modules.impl.render.CustomFog;
import tech.huihui.client.modules.impl.render.CustomHotbar;
import tech.huihui.client.modules.impl.render.CustomModel;
import tech.huihui.client.modules.impl.render.ClickGUI;
import tech.huihui.client.modules.impl.render.EditClickGUI;
import tech.huihui.client.modules.impl.render.EntityESP;
import tech.huihui.client.modules.impl.render.FullBright;
import tech.huihui.client.modules.impl.render.GlowHands;
import tech.huihui.client.modules.impl.render.Interface;
import tech.huihui.client.modules.impl.render.Menu;
import tech.huihui.client.modules.impl.render.NoRender;
import tech.huihui.client.modules.impl.render.Optimization;
import tech.huihui.client.modules.impl.render.ShulkerPreview;
import tech.huihui.client.modules.impl.render.Predictions;
import tech.huihui.client.modules.impl.render.AresMinePvPWarpChest;
import tech.huihui.client.modules.impl.render.SwingAnimation;
import tech.huihui.client.modules.impl.render.UseAnimation;
import tech.huihui.client.modules.impl.render.TargetESP;
import tech.huihui.client.modules.impl.render.TargetHud;
import tech.huihui.client.modules.impl.render.ViewModel;
import tech.huihui.client.modules.impl.render.Watermark;
import tech.huihui.client.modules.impl.render.WingsModule;
import tech.huihui.client.modules.impl.render.WorldTime;
import tech.huihui.client.modules.impl.render.XRay;
import tech.huihui.client.modules.impl.render.ScoreboardHud;
import tech.huihui.client.screens.menu.MenuScreen;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.interfaces.IMinecraft;

public final class ModuleManager implements IMinecraft {
   private final List<Module> modules = new ArrayList<>();
   private boolean isBack;
   private boolean isRotated;
   private float acceleration;

   private long lastKeyPressTime = 0;
   private int lastKeyCode = -1;
   private static final long DEBOUNCE_THRESHOLD_MS = 200;

   public ModuleManager() {
      init();
      EventManager.register(this);
   }

   private void init() {
      registerCombat();
      registerMovement();
      registerRender();
      registerPlayer();
      registerMisc();
   }

   private void registerCombat() {
      registerModule(AntiBot.INSTANCE);
      registerModule(Aimbow.INSTANCE);
      registerModule(Aura.INSTANCE);
       registerModule(AutoCrystal.INSTANCE);
       registerModule(Surround.INSTANCE);
      registerModule(AutoSwap.INSTANCE);
      registerModule(AutoTotem.INSTANCE);
      registerModule(AutoGapple.INSTANCE);
      registerModule(ClickPearl.INSTANCE);
      registerModule(TargetPearl.INSTANCE);
   }

   private void registerMovement() {
      registerModule(AutoSprint.INSTANCE);
      registerModule(Fly.INSTANCE);
      registerModule(ElytraBooster.INSTANCE);
      registerModule(ElytraRecast.INSTANCE);
      registerModule(GuiWalk.INSTANCE);
      registerModule(NoSlow.INSTANCE);
      registerModule(Jesus.INSTANCE);
      registerModule(Speed.INSTANCE);
      registerModule(AirStuck.INSTANCE);
      registerModule(AntiElytraTarget.INSTANCE);
      registerModule(ElytraMotion.INSTANCE);
       registerModule(NoWeb.INSTANCE);
        registerModule(SpiderMatrix.INSTANCE);
        registerModule(Scaffold.INSTANCE);
        registerModule(TargetStrafe.INSTANCE);
     }

   private void registerRender() {
      registerModule(Interface.INSTANCE);
      registerModule(AntiInvisible.INSTANCE);
      registerModule(Menu.INSTANCE);
      registerModule(NoRender.INSTANCE);
      registerModule(Predictions.INSTANCE);
      registerModule(SwingAnimation.INSTANCE);
      registerModule(Crosshair.INSTANCE);
      registerModule(ViewModel.INSTANCE);
      registerModule(CustomFog.INSTANCE);
       registerModule(FullBright.INSTANCE);
       registerModule(CustomModel.INSTANCE);
       registerModule(GlowHands.INSTANCE);
      registerModule(Optimization.INSTANCE);
      registerModule(ShulkerPreview.INSTANCE);
      registerModule(WorldTime.INSTANCE);
      registerModule(ClickGUI.INSTANCE);
      registerModule(EditClickGUI.INSTANCE);
       registerModule(EntityESP.INSTANCE);
       registerModule(TargetESP.INSTANCE);
       registerModule(BlockESP.INSTANCE);
       registerModule(AresMinePvPWarpChest.INSTANCE);
       registerModule(TargetHud.INSTANCE);
       registerModule(Watermark.INSTANCE);
       registerModule(ChunkAnimator.INSTANCE);
       registerModule(XRay.INSTANCE);
       registerModule(ScoreboardHud.INSTANCE);
       registerModule(WingsModule.INSTANCE);
       registerModule(AnimalsModule.INSTANCE);
       registerModule(CustomHotbar.INSTANCE);
       registerModule(UseAnimation.INSTANCE);
    }

   private void registerPlayer() {
      registerModule(AutoTool.INSTANCE);
      registerModule(AutoArmor.INSTANCE);
      registerModule(Blink.INSTANCE);
      registerModule(NoDelay.INSTANCE);
      registerModule(FastBreak.INSTANCE);
      registerModule(NoPush.INSTANCE);
   }

   private void registerMisc() {
      registerModule(ServerHelper.INSTANCE);
       registerModule(ElytraHelper.INSTANCE);
       registerModule(PilotSettings.INSTANCE);
       registerModule(ItemScroller.INSTANCE);
       registerModule(ClickAction.INSTANCE);
       registerModule(FreeCam.INSTANCE);
       registerModule(DiscordRPC.INSTANCE);
      registerModule(AHHelper.INSTANCE);
      registerModule(NoInteract.INSTANCE);
      registerModule(AutoAccept.INSTANCE);
      registerModule(AutoDuel.INSTANCE);
      registerModule(AutoRespawn.INSTANCE);
      registerModule(Autofarm.INSTANCE);
      registerModule(AutoKit.INSTANCE);
      registerModule(CreeperFarm.INSTANCE);
      registerModule(NameProtect.INSTANCE);
      registerModule(ScoreboardHealth.INSTANCE);
      registerModule(ElytraAccelerate.INSTANCE);
      registerModule(ZarabotokReallyWorld.INSTANCE);
       registerModule(AutoJoiner.INSTANCE);
       registerModule(BaseFinder.INSTANCE);
        registerModule(MineHelper.INSTANCE);
        registerModule(KeyFinder.INSTANCE);
        registerModule(AutoWarden.INSTANCE);
     }

   private void registerModule(Module module) {
      modules.add(module);
   }

   public Module getModule(String name) {
      return modules.stream()
              .filter(module -> module.getName().equalsIgnoreCase(name))
              .findFirst()
              .orElse(null);
   }

   public Set<Module> getActiveModules() {
      Set<Module> active = new HashSet<>();
      for (Module module : modules) {
         if (module.isEnabled()) {
            active.add(module);
         }
      }
      return active;
   }

   @EventTarget
   public void onKey(EventKey event) {
      if (mc.currentScreen == null && event.getAction() == 1) {
         int keyCode = event.getKeyCode();
         long currentTime = System.currentTimeMillis();
         
         if (keyCode == lastKeyCode && (currentTime - lastKeyPressTime) < DEBOUNCE_THRESHOLD_MS) {
            return;
         }
         
         lastKeyCode = keyCode;
         lastKeyPressTime = currentTime;
         
         for (Module module : modules) {
            if (module.getKeyCode() == keyCode && module.getKeyCode() != -1) {
               module.toggle();
            }
         }

         for (Macro macro : HuihuiClient.getInstance().getMacroManager().getItems()) {
            if (keyCode == macro.getBind()) {
               mc.getNetworkHandler().sendChatMessage(macro.getText());
            }
         }
      }
   }

   @EventTarget
   public void onRender(EventHudRender e) {
      HuihuiClient.getInstance().getThemeManager().getCurrentTheme().getAnimation().update(1.0F);

      for (Module module : modules) {
         module.getAnimation().update(module.isEnabled());

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
               booleanSetting.getAnimation().update(booleanSetting.isEnabled());
            } else if (setting instanceof ModeSetting modeSetting) {
               for (ModeSetting.Value value : modeSetting.getValues()) {
                  value.getAnimation().update(value.isSelected());
               }
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
               for (MultiBooleanSetting.Value value : multiBooleanSetting.getBooleanSettings()) {
                  value.getAnimation().update(value.isEnabled());
               }
            }
         }
      }

      MenuScreen menuScreen = HuihuiClient.getInstance().getMenuScreen();
      if (menuScreen.needToClose) {
         if (menuScreen.savedRunnable != null) {
            menuScreen.savedRunnable.run();
         }

         if (menuScreen.openAnimationMetanoise.getValue() <= 0.27F) {
            menuScreen.savedRunnable = null;
            menuScreen.needToClose = false;
            menuScreen.openAnimationMetanoise.setValue(0.0F);
            menuScreen.openAnimationMetanoise.setStartValue(0.0F);
         }
      }
   }

   @EventTarget
   private void onPacket(EventPacket e) {
      if (e.getPacket() instanceof CloseScreenS2CPacket && mc.currentScreen instanceof MenuScreen) {
         e.cancel();
      }
   }

   @EventTarget
   private void onGameUpdate(EventGameUpdate e) {
      if (mc.player == null) return;

      if (!Aura.INSTANCE.isEnabled() || Aura.INSTANCE.getTarget() == null) {
         float cameraYaw = mc.gameRenderer.getCamera().getYaw();
         float cameraPitch = mc.gameRenderer.getCamera().getPitch();

         if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            Aura.INSTANCE.lastYaw = cameraYaw - 180.0F;
            Aura.INSTANCE.lastPitch = -cameraPitch;
         } else {
            Aura.INSTANCE.lastYaw = cameraYaw;
            Aura.INSTANCE.lastPitch = cameraPitch;
         }

         if (Aura.INSTANCE.rotationMode.is("Vanilla")) {
            return;
         }

         Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         float deltaYaw = MathHelper.wrapDegrees(cameraYaw - current.getYaw());
         float deltaPitch = cameraPitch - current.getPitch();

         if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            deltaYaw = MathHelper.wrapDegrees(cameraYaw - 180.0F - current.getYaw());
            deltaPitch = -cameraPitch - current.getPitch();
         }

         acceleration += 0.0024F;
         float smooth = MathHelper.clamp(acceleration, 0.0F, 1.0F);
         float newYaw = current.getYaw() + deltaYaw * smooth;
         float newPitch = current.getPitch() + deltaPitch * (smooth / 2.0F);

         Rotation smoothRot = new Rotation(newYaw, newPitch);
         RotationComponent.update(smoothRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 2, false);
      }
   }


   public List<Module> getModules() {
      return modules;
   }

   public boolean isBack() {
      return isBack;
   }

   public boolean isRotated() {
      return isRotated;
   }

   public float getAcceleration() {
      return acceleration;
   }

   public void setBack(boolean isBack) {
      this.isBack = isBack;
   }

   public void setRotated(boolean isRotated) {
      this.isRotated = isRotated;
   }

   public void setAcceleration(float acceleration) {
      this.acceleration = acceleration;
   }
}