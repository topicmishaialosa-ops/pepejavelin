package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown10Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.CARDS)
         .screen(new ColorRGBA(5, 11, 19), new ColorRGBA(7, 16, 26))
         .panel(new ColorRGBA(9, 18, 28), new ColorRGBA(9, 18, 28))
         .border(new ColorRGBA(70, 150, 220, 90))
         .text(new ColorRGBA(212, 230, 245), new ColorRGBA(130, 155, 180), new ColorRGBA(240, 248, 255))
         .accent(new ColorRGBA(70, 190, 255))
         .accent2(new ColorRGBA(70, 255, 200))
         .module(new ColorRGBA(70, 190, 255, 40), new ColorRGBA(70, 255, 200))
         .toggle(new ColorRGBA(70, 190, 255, 230), new ColorRGBA(255, 255, 255, 24))
         .cardShadow(new ColorRGBA(0, 60, 140, 70))
         .radius(10.0F)
         .rowRadius(10.0F)
         .borderWidth(1.0F)
         .rowHeight(30.0F)
         .contentX(12.0F)
         .shadowPanels(true)
         .fonts(Fonts.ICONS, Fonts.BOLD, Fonts.REGULAR)
         .build();

   public Dropdown10Screen() {
      super();
   }

   public static Dropdown10Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown10Screen INSTANCE = new Dropdown10Screen();
   }
}