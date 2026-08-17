package tech.huihui.utility.mixin.client.render.gui.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.impl.render.Menu;
import tech.huihui.client.screens.mainmenu.MainMenuScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

   @Inject(method = {"init"}, at = {@At("HEAD")}, cancellable = true)
   private void onInit(CallbackInfo ci) {
      boolean custom = HuihuiClient.getInstance().getModuleManager().getModule("Menu") == Menu.INSTANCE && Menu.INSTANCE.getModeIndex() == Menu.CUSTOM_MODE;
      if (custom && !(MinecraftClient.getInstance().currentScreen instanceof MainMenuScreen)) {
         MinecraftClient.getInstance().setScreen(new MainMenuScreen());
         ci.cancel();
      }
   }
}