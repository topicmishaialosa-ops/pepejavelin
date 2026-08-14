package tech.huihui.client.screens.dropdowngui;

import tech.huihui.base.font.Fonts;
import tech.huihui.base.font.MsdfFont;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

public final class DropdownDesign {
   public enum Layout {
      COLUMNS,
      TABS,
      CARDS
   }

   public final Layout layout;
   public final ColorRGBA screenTop;
   public final ColorRGBA screenBottom;
   public final ColorRGBA panelBg1;
   public final ColorRGBA panelBg2;
   public final ColorRGBA border;
   public final ColorRGBA textMain;
   public final ColorRGBA textDim;
   public final ColorRGBA textBright;
   public final ColorRGBA accent;
   public final ColorRGBA accent2;
   public final ColorRGBA moduleOn;
   public final ColorRGBA moduleBar;
   public final ColorRGBA toggleOn;
   public final ColorRGBA toggleOff;
   public final ColorRGBA cardShadow;
   public final float radius;
   public final float rowRadius;
   public final float borderWidth;
   public final float headerHeight;
   public final float rowHeight;
   public final float settingGap;
   public final float contentX;
   public final boolean gradientPanels;
   public final boolean shadowPanels;
   public final boolean rainbow;
   public final float baseHue;
   public final MsdfFont headerFont;
   public final MsdfFont moduleFont;
   public final MsdfFont textFont;

   private DropdownDesign(Builder b) {
      this.layout = b.layout;
      this.screenTop = b.screenTop;
      this.screenBottom = b.screenBottom;
      this.panelBg1 = b.panelBg1;
      this.panelBg2 = b.panelBg2;
      this.border = b.border;
      this.textMain = b.textMain;
      this.textDim = b.textDim;
      this.textBright = b.textBright;
      this.accent = b.accent;
      this.accent2 = b.accent2;
      this.moduleOn = b.moduleOn;
      this.moduleBar = b.moduleBar;
      this.toggleOn = b.toggleOn;
      this.toggleOff = b.toggleOff;
      this.cardShadow = b.cardShadow;
      this.radius = b.radius;
      this.rowRadius = b.rowRadius;
      this.borderWidth = b.borderWidth;
      this.headerHeight = b.headerHeight;
      this.rowHeight = b.rowHeight;
      this.settingGap = b.settingGap;
      this.contentX = b.contentX;
      this.gradientPanels = b.gradientPanels;
      this.shadowPanels = b.shadowPanels;
      this.rainbow = b.rainbow;
      this.baseHue = b.baseHue;
      this.headerFont = b.headerFont;
      this.moduleFont = b.moduleFont;
      this.textFont = b.textFont;
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {
      private Layout layout = Layout.COLUMNS;
      private ColorRGBA screenTop = new ColorRGBA(5, 7, 12);
      private ColorRGBA screenBottom = new ColorRGBA(5, 7, 12);
      private ColorRGBA panelBg1 = new ColorRGBA(15, 15, 15);
      private ColorRGBA panelBg2 = new ColorRGBA(15, 15, 15);
      private ColorRGBA border = new ColorRGBA(21, 21, 21);
      private ColorRGBA textMain = new ColorRGBA(194, 202, 211);
      private ColorRGBA textDim = new ColorRGBA(150, 170, 166);
      private ColorRGBA textBright = new ColorRGBA(255, 255, 255);
      private ColorRGBA accent = new ColorRGBA(74, 238, 151);
      private ColorRGBA accent2 = new ColorRGBA(74, 238, 151);
      private ColorRGBA moduleOn = new ColorRGBA(55, 214, 133, 42);
      private ColorRGBA moduleBar = new ColorRGBA(74, 238, 151);
      private ColorRGBA toggleOn = new ColorRGBA(52, 205, 132, 210);
      private ColorRGBA toggleOff = new ColorRGBA(255, 255, 255, 22);
      private ColorRGBA cardShadow = new ColorRGBA(0, 0, 0, 90);
      private float radius = 7.0F;
      private float rowRadius = 7.0F;
      private float borderWidth = 1.0F;
      private float headerHeight = 38.0F;
      private float rowHeight = 24.0F;
      private float settingGap = 5.0F;
      private float contentX = 17.0F;
      private boolean gradientPanels;
      private boolean shadowPanels;
      private boolean rainbow;
      private float baseHue;
      private MsdfFont headerFont = Fonts.ICONS;
      private MsdfFont moduleFont = Fonts.BOLD;
      private MsdfFont textFont = Fonts.REGULAR;

      private Builder() {
      }

      public Builder layout(Layout value) {
         this.layout = value;
         return this;
      }

      public Builder screen(ColorRGBA top, ColorRGBA bottom) {
         this.screenTop = top;
         this.screenBottom = bottom;
         return this;
      }

      public Builder panel(ColorRGBA bg1, ColorRGBA bg2) {
         this.panelBg1 = bg1;
         this.panelBg2 = bg2;
         return this;
      }

      public Builder border(ColorRGBA value) {
         this.border = value;
         return this;
      }

      public Builder text(ColorRGBA main, ColorRGBA dim, ColorRGBA bright) {
         this.textMain = main;
         this.textDim = dim;
         this.textBright = bright;
         return this;
      }

      public Builder accent(ColorRGBA value) {
         this.accent = value;
         return this;
      }

      public Builder accent2(ColorRGBA value) {
         this.accent2 = value;
         return this;
      }

      public Builder module(ColorRGBA on, ColorRGBA bar) {
         this.moduleOn = on;
         this.moduleBar = bar;
         return this;
      }

      public Builder toggle(ColorRGBA on, ColorRGBA off) {
         this.toggleOn = on;
         this.toggleOff = off;
         return this;
      }

      public Builder cardShadow(ColorRGBA value) {
         this.cardShadow = value;
         return this;
      }

      public Builder radius(float value) {
         this.radius = value;
         return this;
      }

      public Builder rowRadius(float value) {
         this.rowRadius = value;
         return this;
      }

      public Builder borderWidth(float value) {
         this.borderWidth = value;
         return this;
      }

      public Builder headerHeight(float value) {
         this.headerHeight = value;
         return this;
      }

      public Builder rowHeight(float value) {
         this.rowHeight = value;
         return this;
      }

      public Builder settingGap(float value) {
         this.settingGap = value;
         return this;
      }

      public Builder contentX(float value) {
         this.contentX = value;
         return this;
      }

      public Builder gradientPanels(boolean value) {
         this.gradientPanels = value;
         return this;
      }

      public Builder shadowPanels(boolean value) {
         this.shadowPanels = value;
         return this;
      }

      public Builder rainbow(float baseHue) {
         this.rainbow = true;
         this.baseHue = baseHue;
         return this;
      }

      public Builder fonts(MsdfFont header, MsdfFont module, MsdfFont text) {
         this.headerFont = header;
         this.moduleFont = module;
         this.textFont = text;
         return this;
      }

      public DropdownDesign build() {
         return new DropdownDesign(this);
      }
   }
}