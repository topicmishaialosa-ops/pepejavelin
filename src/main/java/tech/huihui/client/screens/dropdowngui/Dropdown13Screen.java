package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown13Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.TABS)
         .screen(new ColorRGBA(13, 10, 5), new ColorRGBA(13, 10, 5))
         .panel(new ColorRGBA(26, 21, 11), new ColorRGBA(14, 18, 22))
         .border(new ColorRGBA(255, 198, 90, 110))
         .text(new ColorRGBA(240, 230, 205), new ColorRGBA(170, 152, 118), new ColorRGBA(255, 246, 220))
         .accent(new ColorRGBA(255, 190, 80))
         .accent2(new ColorRGBA(255, 240, 180))
         .module(new ColorRGBA(255, 190, 80, 44), new ColorRGBA(255, 190, 80))
         .toggle(new ColorRGBA(255, 190, 80, 220), new ColorRGBA(255, 255, 255, 22))
         .cardShadow(new ColorRGBA(170, 120, 25, 55))
         .radius(6.0F)
         .rowRadius(6.0F)
         .borderWidth(1.0F)
         .gradientPanels(true)
         .shadowPanels(true)
         .fonts(Fonts.ICONS, Fonts.BOLD, Fonts.REGULAR)
         .build();

   public Dropdown13Screen() {
      super();
   }

   public static Dropdown13Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown13Screen INSTANCE = new Dropdown13Screen();
   }
}