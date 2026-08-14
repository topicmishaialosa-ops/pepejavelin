package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown11Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.CARDS)
         .screen(new ColorRGBA(14, 4, 6), new ColorRGBA(20, 6, 8))
         .panel(new ColorRGBA(20, 8, 11), new ColorRGBA(20, 8, 11))
         .border(new ColorRGBA(255, 80, 90, 80))
         .text(new ColorRGBA(240, 210, 215), new ColorRGBA(165, 125, 130), new ColorRGBA(255, 235, 235))
         .accent(new ColorRGBA(255, 70, 90))
         .accent2(new ColorRGBA(255, 190, 70))
         .module(new ColorRGBA(255, 70, 90, 42), new ColorRGBA(255, 190, 70))
         .toggle(new ColorRGBA(255, 70, 90, 220), new ColorRGBA(255, 255, 255, 24))
         .cardShadow(new ColorRGBA(100, 0, 15, 75))
         .radius(6.0F)
         .rowRadius(6.0F)
         .borderWidth(1.0F)
         .rowHeight(30.0F)
         .contentX(12.0F)
         .shadowPanels(true)
         .fonts(Fonts.ICONS, Fonts.BOLD, Fonts.REGULAR)
         .build();

   public Dropdown11Screen() {
      super();
   }

   public static Dropdown11Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown11Screen INSTANCE = new Dropdown11Screen();
   }
}