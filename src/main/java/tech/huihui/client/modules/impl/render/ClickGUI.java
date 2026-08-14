package tech.huihui.client.modules.impl.render;

import net.minecraft.client.gui.screen.Screen;
import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.screens.dropdowngui.Csgui1Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown10Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown11Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown12Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown13Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown3Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown4Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown5Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown6Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown7Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown8Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown9Screen;

@ModuleAnnotation(
   name = "ClickGUI",
   category = Category.RENDER,
   description = "Dropdown кликгуи"
)
public final class ClickGUI extends Module {
   public static final ClickGUI INSTANCE = new ClickGUI();

   private ClickGUI() {
      this.setKeyCode(93);
   }

   public void onEnable() {
      if (mc.world == null) {
         this.setEnabled(false);
      } else {
         Screen screen;
         String mode = EditClickGUI.INSTANCE.getMode().get();
         screen = switch (mode) {
            case "Dropdown3" -> Dropdown3Screen.getInstance();
            case "Dropdown4" -> Dropdown4Screen.getInstance();
            case "Dropdown5" -> Dropdown5Screen.getInstance();
            case "Dropdown6" -> Dropdown6Screen.getInstance();
            case "Dropdown7" -> Dropdown7Screen.getInstance();
            case "Dropdown8" -> Dropdown8Screen.getInstance();
            case "Dropdown9" -> Dropdown9Screen.getInstance();
            case "Dropdown10" -> Dropdown10Screen.getInstance();
            case "Dropdown11" -> Dropdown11Screen.getInstance();
            case "Dropdown12" -> Dropdown12Screen.getInstance();
            case "Dropdown13" -> Dropdown13Screen.getInstance();
            case "csgui1" -> Csgui1Screen.getInstance();
            default -> {
               HuihuiClient.getInstance().getClickGuiScreen().resetSearch();
               yield HuihuiClient.getInstance().getClickGuiScreen();
            }
         };
         if (mc.currentScreen != screen) {
            mc.setScreen(screen);
            super.onEnable();
         }
      }
   }

   public void onDisable() {
      super.onDisable();
   }

   public void setKeyCode(int keyCode) {
      if (keyCode != -1) {
         super.setKeyCode(keyCode);
      }
   }
}
