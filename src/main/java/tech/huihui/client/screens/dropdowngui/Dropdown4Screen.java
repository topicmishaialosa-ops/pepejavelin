package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown4Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.COLUMNS)
         .screen(new ColorRGBA(6, 7, 12), new ColorRGBA(12, 10, 24))
         .panel(new ColorRGBA(13, 15, 26), new ColorRGBA(13, 15, 26))
         .border(new ColorRGBA(0, 229, 255, 95))
         .text(new ColorRGBA(215, 230, 240), new ColorRGBA(125, 145, 165), new ColorRGBA(255, 255, 255))
         .accent(new ColorRGBA(0, 229, 255))
         .accent2(new ColorRGBA(255, 0, 170))
         .module(new ColorRGBA(0, 229, 255, 32), new ColorRGBA(0, 229, 255))
         .toggle(new ColorRGBA(0, 229, 255, 220), new ColorRGBA(255, 255, 255, 26))
         .cardShadow(new ColorRGBA(0, 200, 255, 70))
         .radius(4.0F)
         .rowRadius(4.0F)
         .borderWidth(1.0F)
         .shadowPanels(true)
         .rainbow(190.0F)
         .fonts(Fonts.ICONS, Fonts.BOLD, Fonts.REGULAR)
         .build();

   public Dropdown4Screen() {
      super();
   }

   public static Dropdown4Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown4Screen INSTANCE = new Dropdown4Screen();
   }
}