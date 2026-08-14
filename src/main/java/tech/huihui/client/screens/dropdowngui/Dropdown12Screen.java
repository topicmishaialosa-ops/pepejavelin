package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown12Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.COLUMNS)
         .screen(new ColorRGBA(242, 246, 252), new ColorRGBA(242, 246, 252))
         .panel(new ColorRGBA(255, 255, 255), new ColorRGBA(255, 255, 255))
         .border(new ColorRGBA(188, 200, 216, 150))
         .text(new ColorRGBA(45, 55, 70), new ColorRGBA(110, 122, 138), new ColorRGBA(15, 20, 30))
         .accent(new ColorRGBA(50, 120, 255))
         .accent2(new ColorRGBA(50, 120, 255))
         .module(new ColorRGBA(50, 120, 255, 46), new ColorRGBA(50, 120, 255))
         .toggle(new ColorRGBA(50, 120, 255, 230), new ColorRGBA(215, 222, 232, 130))
         .cardShadow(new ColorRGBA(100, 120, 170, 45))
         .radius(8.0F)
         .rowRadius(8.0F)
         .borderWidth(1.0F)
         .shadowPanels(true)
         .fonts(Fonts.COMFORTA_REGULAR, Fonts.BOLD, Fonts.REGULAR)
         .build();

   public Dropdown12Screen() {
      super();
   }

   public static Dropdown12Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown12Screen INSTANCE = new Dropdown12Screen();
   }
}