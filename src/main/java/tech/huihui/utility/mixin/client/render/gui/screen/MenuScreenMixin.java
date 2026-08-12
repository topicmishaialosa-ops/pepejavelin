package tech.huihui.utility.mixin.client.render.gui.screen;

import com.darkmagician6.eventapi.EventManager;
import tech.huihui.HuihuiClient;
import tech.huihui.base.modules.ModuleManager;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.accessor.MenuAccessor;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
abstract class MenuScreenMixin {

   private static final float TOGGLE_X = 6.0F;
   private static final float TOGGLE_Y = 20.0F;
   private static final float TOGGLE_WIDTH = 200.0F;
   private static final float TOGGLE_HEIGHT = 32.0F;
   private static final float TOGGLE_GAP = 4.0F;

   private List<ToggleButton> buttons = new ArrayList<>();

   @Inject(method = {"init"}, at = {@At("HEAD")}, require = 0)
   private void onInit(CallbackInfo ci) {
      this.buttons.clear();
      Module menuModule = HuihuiClient.getInstance().getModuleManager().getModule("Menu");
      if (menuModule instanceof MenuAccessor) {
         String[] modes = ((MenuAccessor) menuModule).getModes();
         for (int i = 0; i < modes.length; i++) {
            this.buttons.add(new ToggleButton(i, 0.0F));
         }
      }
   }

   @Inject(method = {"mouseClicked"}, at = {@At("HEAD")}, require = 0)
   private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfo ci) {
      float startY = TOGGLE_Y;
      float spacing = TOGGLE_HEIGHT + TOGGLE_GAP;
      float toggleX = TOGGLE_X;
      float toggleW = TOGGLE_WIDTH;
      Module menuModule = HuihuiClient.getInstance().getModuleManager().getModule("Menu");
      if (menuModule instanceof MenuAccessor) {
         String[] modes = ((MenuAccessor) menuModule).getModes();
         ModeSetting mode = ((MenuAccessor) menuModule).getMode();
         String currentMode = mode.getValue().getName();

         for (int i = 0; i < this.buttons.size(); i++) {
            float y = startY + (float) i * spacing;
            if (mouseX >= toggleX && mouseX <= toggleX + toggleW && mouseY >= y && mouseY <= y + TOGGLE_HEIGHT) {
               String clickedMode = modes[i];
               if (!currentMode.equals(clickedMode)) {
                  mode.set(clickedMode);
                  EventManager.call(new tech.huihui.base.events.impl.input.EventKey(0, 0));
               }
               ci.cancel();
               return;
            }
         }
      }
   }

   private static class ToggleButton {
      int index;
      float hover;

      ToggleButton(int index, float hover) {
         this.index = index;
         this.hover = hover;
      }
   }
}
