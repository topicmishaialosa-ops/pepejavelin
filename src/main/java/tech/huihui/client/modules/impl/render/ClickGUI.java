package tech.huihui.client.modules.impl.render;

import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;

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
         HuihuiClient.getInstance().getClickGuiScreen().resetSearch();
         if (mc.currentScreen != HuihuiClient.getInstance().getClickGuiScreen()) {
            mc.setScreen(HuihuiClient.getInstance().getClickGuiScreen());
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
