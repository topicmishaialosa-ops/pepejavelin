package tech.huihui.client.modules.impl.render;

import net.minecraft.client.gui.screen.Screen;
import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.screens.dropdowngui.Csgui1Screen;
import tech.huihui.client.screens.dropdowngui.Dropdown3Screen;

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
         if (EditClickGUI.INSTANCE.isDropdown3()) {
            screen = Dropdown3Screen.getInstance();
         } else if (EditClickGUI.INSTANCE.isCsgui1()) {
            screen = Csgui1Screen.getInstance();
         } else {
            HuihuiClient.getInstance().getClickGuiScreen().resetSearch();
            screen = HuihuiClient.getInstance().getClickGuiScreen();
         }
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
