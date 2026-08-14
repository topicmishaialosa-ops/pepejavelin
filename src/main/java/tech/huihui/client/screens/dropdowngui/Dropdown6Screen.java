package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown6Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.COLUMNS)
         .screen(new ColorRGBA(8, 8, 8), new ColorRGBA(8, 8, 8))
         .panel(new ColorRGBA(16, 16, 16), new ColorRGBA(16, 16, 16))
         .border(new ColorRGBA(255, 255, 255, 150))
         .text(new ColorRGBA(210, 210, 210), new ColorRGBA(128, 128, 128), new ColorRGBA(255, 255, 255))
         .accent(new ColorRGBA(255, 255, 255))
         .accent2(new ColorRGBA(255, 255, 255))
         .module(new ColorRGBA(255, 255, 255, 36), new ColorRGBA(255, 255, 255))
         .toggle(new ColorRGBA(255, 255, 255, 230), new ColorRGBA(255, 255, 255, 30))
         .cardShadow(new ColorRGBA(0, 0, 0, 120))
         .radius(2.0F)
         .rowRadius(2.0F)
         .borderWidth(1.5F)
         .fonts(Fonts.REGULAR, Fonts.REGULAR, Fonts.REGULAR)
         .build();

   public Dropdown6Screen() {
      super();
   }

   public static Dropdown6Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown6Screen INSTANCE = new Dropdown6Screen();
   }
}