package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class Dropdown8Screen extends AbstractDropdownScreen {
   private static final DropdownDesign DESIGN = DropdownDesign.builder()
         .layout(DropdownDesign.Layout.TABS)
         .screen(new ColorRGBA(10, 8, 24), new ColorRGBA(8, 14, 28))
         .panel(new ColorRGBA(20, 16, 42), new ColorRGBA(14, 26, 48))
         .border(new ColorRGBA(130, 100, 255, 80))
         .text(new ColorRGBA(228, 222, 250), new ColorRGBA(150, 145, 185), new ColorRGBA(255, 255, 255))
         .accent(new ColorRGBA(160, 120, 255))
         .accent2(new ColorRGBA(80, 220, 255))
         .module(new ColorRGBA(160, 120, 255, 40), new ColorRGBA(80, 220, 255))
         .toggle(new ColorRGBA(160, 120, 255, 220), new ColorRGBA(255, 255, 255, 26))
         .cardShadow(new ColorRGBA(60, 20, 140, 60))
         .radius(8.0F)
         .rowRadius(8.0F)
         .borderWidth(1.0F)
         .gradientPanels(true)
         .shadowPanels(true)
         .fonts(Fonts.ICONS, Fonts.BOLD, Fonts.REGULAR)
         .build();

   public Dropdown8Screen() {
      super();
   }

   public static Dropdown8Screen getInstance() {
      return Holder.INSTANCE;
   }

   @Override
   protected DropdownDesign design() {
      return DESIGN;
   }

   private static final class Holder {
      private static final Dropdown8Screen INSTANCE = new Dropdown8Screen();
   }
}