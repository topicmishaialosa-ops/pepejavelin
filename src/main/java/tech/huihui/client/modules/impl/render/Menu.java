package tech.huihui.client.modules.impl.render;

import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "Menu",
   category = Category.RENDER,
   description = "Меню чита"
)
public final class Menu extends Module {
   public static final Menu INSTANCE = new Menu();

   private Menu() {
      this.setKeyCode(344);
   }

   public void onEnable() {
      if (mc.world == null) {
         this.setEnabled(false);
      } else {
         HuihuiClient.getInstance().getMenuScreen().needToClose = false;
         if (mc.currentScreen != HuihuiClient.getInstance().getMenuScreen()) {
            mc.setScreen(HuihuiClient.getInstance().getMenuScreen());
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
