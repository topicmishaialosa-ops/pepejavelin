package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown7Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.COLUMNS)
         .screen(new ColorRGBA(250, 246, 240), new ColorRGBA(244, 238, 250))
         .panel(new ColorRGBA(255, 253, 250), new ColorRGBA(255, 253, 250))
         .border(new ColorRGBA(215, 200, 235, 140))
         .text(new ColorRGBA(95, 85, 115), new ColorRGBA(150, 140, 175), new ColorRGBA(45, 40, 60))
         .accent(new ColorRGBA(255, 140, 170))
         .accent2(new ColorRGBA(170, 150, 255))
         .module(new ColorRGBA(255, 140, 170, 60), new ColorRGBA(255, 120, 160))
         .toggle(new ColorRGBA(255, 140, 170, 220), new ColorRGBA(230, 220, 240, 130))
         .cardShadow(new ColorRGBA(150, 120, 190, 45))
         .radius(12.0F)
         .rowRadius(12.0F)
         .borderWidth(1.0F)
         .shadowPanels(true)
         .fonts(Fonts.ROUND_BOLD, Fonts.ROUND_BOLD, Fonts.REGULAR)
         .build();

   public Dropdown7Screen() {
      super();
   }

   public static Dropdown7Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown7Screen INSTANCE = new Dropdown7Screen();
   }
}