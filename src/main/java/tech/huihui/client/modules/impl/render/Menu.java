package tech.huihui.client.modules.impl.render;

import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.accessor.MenuAccessor;

@ModuleAnnotation(
   name = "Menu",
   category = Category.RENDER,
   description = "Меню чита"
)
public final class Menu extends Module implements MenuAccessor {
   public static final Menu INSTANCE;

   private static final String[] MODES = new String[]{"Ваниль", "Кастом"};
   private static final int VANILLA = 0;
   private static final int CUSTOM = 1;

   private final ModeSetting mode = new ModeSetting("Режим меню", MODES);

   static {
      INSTANCE = new Menu();
   }

   private Menu() {
      this.setKeyCode(344);
   }

   @Override
   public void onEnable() {
      if (mc.world == null) {
         this.setEnabled(false);
         return;
      }
      if (this.mode.is(MODES[CUSTOM])) {
         mc.setScreen(HuihuiClient.getInstance().getMenuScreen());
         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (this.mode.is(MODES[CUSTOM]) && mc.currentScreen == HuihuiClient.getInstance().getMenuScreen()) {
         mc.currentScreen.close();
      }
      super.onDisable();
   }

   public int getModeIndex() {
      return this.mode.is(MODES[CUSTOM]) ? CUSTOM : VANILLA;
   }

   @Override
   public String[] getModes() {
      return MODES;
   }

   @Override
   public ModeSetting getMode() {
      return this.mode;
   }
}
