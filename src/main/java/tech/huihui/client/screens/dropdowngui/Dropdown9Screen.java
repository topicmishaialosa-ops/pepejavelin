package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown9Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.TABS)
         .screen(new ColorRGBA(5, 9, 5), new ColorRGBA(5, 9, 5))
         .panel(new ColorRGBA(8, 14, 8), new ColorRGBA(8, 14, 8))
         .border(new ColorRGBA(0, 255, 90, 110))
         .text(new ColorRGBA(170, 235, 180), new ColorRGBA(70, 140, 95), new ColorRGBA(210, 255, 220))
         .accent(new ColorRGBA(0, 255, 110))
         .accent2(new ColorRGBA(0, 255, 110))
         .module(new ColorRGBA(0, 255, 110, 32), new ColorRGBA(0, 255, 110))
         .toggle(new ColorRGBA(0, 255, 110, 210), new ColorRGBA(255, 255, 255, 20))
         .cardShadow(new ColorRGBA(0, 255, 60, 40))
         .radius(2.0F)
         .rowRadius(2.0F)
         .borderWidth(1.25F)
         .fonts(Fonts.ICONS, Fonts.REGULAR, Fonts.REGULAR)
         .build();

   public Dropdown9Screen() {
      super();
   }

   public static Dropdown9Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown9Screen INSTANCE = new Dropdown9Screen();
   }
}