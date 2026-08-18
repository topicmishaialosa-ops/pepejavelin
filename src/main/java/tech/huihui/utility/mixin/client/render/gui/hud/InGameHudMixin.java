package tech.huihui.utility.mixin.client.render.gui.hud;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.base.events.impl.render.EventRender2D;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.impl.render.Animations;
import tech.huihui.client.modules.impl.render.Crosshair;
import tech.huihui.client.modules.impl.render.CustomHotbar;
import tech.huihui.client.modules.impl.render.Interface;
import tech.huihui.client.modules.impl.render.ScoreboardHud;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.display.base.CustomDrawContext;

@Mixin({InGameHud.class})
public abstract class InGameHudMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CustomDrawContext customDrawContext = new CustomDrawContext(IMinecraft.mc.getBufferBuilders().getEntityVertexConsumers());
      EventManager.call(new EventRender2D(customDrawContext, tickCounter.getTickDelta(false)));
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void animationsTab(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Animations animations = Animations.INSTANCE;
      if (animations.isEnabled() && animations.getAnimate().isEnable("TAB") && animations.getTabAnimation().getAnimationValue() > 0.0F && !IMinecraft.mc.options.playerListKey.isPressed()) {
         IMinecraft.mc.inGameHud.getPlayerListHud().render(context, IMinecraft.mc.getWindow().getScaledWidth(), IMinecraft.mc.world.getScoreboard(), IMinecraft.mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.LIST));
      }

   }

   @Inject(
      method = {"renderMainHud"},
      at = {@At("HEAD")}
   )
   private void animationsHotbarHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Animations animations = Animations.INSTANCE;
      if (!(Interface.INSTANCE.isEnabled() && Interface.INSTANCE.isEnableHotBar()) && animations.isEnabled() && animations.getAnimate().isEnable("Поднятие хотбара")) {
         context.getMatrices().push();
         context.getMatrices().translate(0.0F, -16.0F * animations.getHotbarAnimation().getAnimationValue(), 0.0F);
      }

   }

   @Inject(
      method = {"renderMainHud"},
      at = {@At("RETURN")}
   )
   private void animationsHotbarTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Animations animations = Animations.INSTANCE;
      if (!(Interface.INSTANCE.isEnabled() && Interface.INSTANCE.isEnableHotBar()) && animations.isEnabled() && animations.getAnimate().isEnable("Поднятие хотбара")) {
         context.getMatrices().pop();
      }

   }

   @Inject(
      method = {"renderExperienceLevel"},
      at = {@At("HEAD")}
   )
   private void animationsExpHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Animations animations = Animations.INSTANCE;
      if (!(Interface.INSTANCE.isEnabled() && Interface.INSTANCE.isEnableHotBar()) && animations.isEnabled() && animations.getAnimate().isEnable("Поднятие хотбара")) {
         context.getMatrices().push();
         context.getMatrices().translate(0.0F, -16.0F * animations.getHotbarAnimation().getAnimationValue(), 0.0F);
      }

   }

   @Inject(
      method = {"renderExperienceLevel"},
      at = {@At("RETURN")}
   )
   private void animationsExpTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Animations animations = Animations.INSTANCE;
      if (!(Interface.INSTANCE.isEnabled() && Interface.INSTANCE.isEnableHotBar()) && animations.isEnabled() && animations.getAnimate().isEnable("Поднятие хотбара")) {
         context.getMatrices().pop();
      }

   }

   @ModifyArg(
      method = {"renderHotbar"},
      index = 2,
      at = @At(
   value = "INVOKE",
   ordinal = 1,
   target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"
)
   )
   private int animationsSlot(int x) {
      Animations animations = Animations.INSTANCE;
      if (animations.isEnabled() && animations.getAnimate().isEnable("Слот хотбара") && IMinecraft.mc.player != null) {
         return Math.round((x - (IMinecraft.mc.player.getInventory().selectedSlot * 20)) + (animations.getSelectedSlot() * 20.0F));
      }

      return x;
   }

   @Inject(
      method = {"renderHotbar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void removeVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      try {
         CustomHotbar customHotbar = CustomHotbar.INSTANCE;
         if (customHotbar.isEnabled() && customHotbar.hideVanilla.isEnabled()) {
            ci.cancel();
         }
      } catch (Exception var5) {
      }

   }

   @Inject(
      method = {"renderStatusBars"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void removeVanillaStatusBars(DrawContext context, CallbackInfo ci) {
      try {
         CustomHotbar customHotbar = CustomHotbar.INSTANCE;
         if (customHotbar.isEnabled() && customHotbar.hideVanilla.isEnabled() && customHotbar.showStatus.isEnabled()) {
            ci.cancel();
         }
      } catch (Exception var4) {
      }

   }

   @Inject(
      method = {"renderCrosshair"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void removeVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      try {
         Module crosshairModule = Crosshair.INSTANCE;
         if (crosshairModule.isEnabled()) {
            ci.cancel();
         }
      } catch (Exception var5) {
      }

   }

   @Inject(
      method = {"renderMainHud"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"renderExperienceLevel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"renderPlayerList"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void inject(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Interface interfaceModule = Interface.INSTANCE;
      if (interfaceModule.isEnabled() && interfaceModule.isEnableTab()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"renderOverlayMessage"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void injectRenderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            ci.cancel();
         }
      }

   }

   @Inject(
      method = {"renderScoreboardSidebar*"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void injectRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      Interface interfaceModule = Interface.INSTANCE;
      if (interfaceModule.isEnabled() && interfaceModule.isEnableScoreBar()) {
         ci.cancel();
      }

      if (ScoreboardHud.INSTANCE.isEnabled()) {
         ci.cancel();
      }

   }

   @ModifyVariable(
      method = {"renderStatusBars"},
      at = @At("STORE"),
      ordinal = 3
   )
   private int modifyM(int original, DrawContext context) {
      if (IMinecraft.mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         Interface interfaceModule = Interface.INSTANCE;
         if (interfaceModule.isEnabled() && interfaceModule.isEnableHotBar()) {
            return context.getScaledWindowWidth() / 2 + 90 + 36;
         }
      }

      return original;
   }
}
