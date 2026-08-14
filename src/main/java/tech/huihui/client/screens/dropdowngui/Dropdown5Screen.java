package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown5Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.COLUMNS)
         .screen(new ColorRGBA(12, 14, 20), new ColorRGBA(18, 16, 26))
         .panel(new ColorRGBA(255, 255, 255, 40), new ColorRGBA(255, 255, 255, 40))
         .border(new ColorRGBA(255, 255, 255, 70))
         .text(new ColorRGBA(235, 240, 248), new ColorRGBA(160, 170, 185), new ColorRGBA(255, 255, 255))
         .accent(new ColorRGBA(120, 180, 255))
         .accent2(new ColorRGBA(255, 255, 255))
         .module(new ColorRGBA(120, 180, 255, 46), new ColorRGBA(150, 200, 255))
         .toggle(new ColorRGBA(120, 180, 255, 220), new ColorRGBA(255, 255, 255, 34))
         .cardShadow(new ColorRGBA(190, 215, 255, 48))
         .radius(10.0F)
         .rowRadius(10.0F)
         .borderWidth(1.0F)
         .shadowPanels(true)
         .fonts(Fonts.ICONS, Fonts.BOLD, Fonts.REGULAR)
         .build();

   public Dropdown5Screen() {
      super();
   }

   public static Dropdown5Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown5Screen INSTANCE = new Dropdown5Screen();
   }
}