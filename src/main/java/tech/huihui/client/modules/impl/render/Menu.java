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
   public static final int CUSTOM_MODE = 1;

   public static final int LAYOUT_CENTER = 0;
   public static final int LAYOUT_LEFT = 1;
   public static final int LAYOUT_RIGHT = 2;
   public static final int LAYOUT_BOTTOM = 3;
   public static final int LAYOUT_TOP = 4;
   public static final int LAYOUT_CORNERS = 5;
   public static final int LAYOUT_DIAGONAL = 6;
   public static final int LAYOUT_SCATTER = 7;
   public static final int LAYOUT_RANDOM = 8;
   public static final String[] LAYOUTS = new String[]{
      "Центр-сплит", "Левая колонка", "Правая колонка", "Нижний ряд", "Верхний ряд", "Углы", "Диагональ", "Вразброс", "Случайный"
   };

   public static final int STYLE_DARK = 0;
   public static final int STYLE_OUTLINE = 1;
   public static final int STYLE_GRADIENT = 2;
   public static final int STYLE_LIGHT = 3;
   public static final int STYLE_NEON = 4;
   public static final String[] STYLES = new String[]{"Тёмный", "Контур", "Градиент", "Светлый", "Неон"};

   private final ModeSetting mode = new ModeSetting("Режим меню", MODES);
   private final ModeSetting layout = new ModeSetting("Расположение меню", LAYOUTS);
   private final ModeSetting style = new ModeSetting("Стиль кнопок", STYLES);

   static {
      INSTANCE = new Menu();
   }

   private Menu() {
      this.setKeyCode(344);
      this.mode.set(MODES[CUSTOM_MODE]);
   }

   @Override
   public void onEnable() {
      if (mc.world == null) {
         this.setEnabled(false);
         return;
      }
      if (this.mode.is(MODES[CUSTOM_MODE])) {
         mc.setScreen(HuihuiClient.getInstance().getMenuScreen());
         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (this.mode.is(MODES[CUSTOM_MODE]) && mc.currentScreen == HuihuiClient.getInstance().getMenuScreen()) {
         mc.currentScreen.close();
      }
      super.onDisable();
   }

   public int getModeIndex() {
      return this.mode.is(MODES[CUSTOM_MODE]) ? CUSTOM_MODE : VANILLA;
   }

   public int getLayoutIndex() {
      return Math.max(0, this.layout.getValues().indexOf(this.layout.getValue()));
   }

   public void setLayoutIndex(int index) {
      if (index >= 0 && index < LAYOUTS.length) {
         this.layout.set(LAYOUTS[index]);
      }
   }

   public void cycleLayout(int step) {
      this.setLayoutIndex((this.getLayoutIndex() + step + LAYOUTS.length) % LAYOUTS.length);
   }

   public int getStyleIndex() {
      return Math.max(0, this.style.getValues().indexOf(this.style.getValue()));
   }

   public void setStyleIndex(int index) {
      if (index >= 0 && index < STYLES.length) {
         this.style.set(STYLES[index]);
      }
   }

   public void cycleStyle(int step) {
      this.setStyleIndex((this.getStyleIndex() + step + STYLES.length) % STYLES.length);
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