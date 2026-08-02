package tech.huihui.base.theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.base.color.ColorUtil;

public class ThemeManager {
   private Theme currentTheme;
   private final List<Theme> themes = new ArrayList();
   private final Theme defaultTheme = new Theme("Cyber Blue", (new Color(0, 200, 255, 255)).getRGB(), (new Color(10, 10, 20, 255)).getRGB());

   public ThemeManager() {
      this.initThemes();
   }

   @Native
   private void initThemes() {
      if (this.currentTheme == null) {
         this.currentTheme = this.defaultTheme;
      }

      this.themes.addAll(List.of(new Theme[]{this.defaultTheme, new Theme("Violet Void", (new Color(180, 0, 255, 255)).getRGB(), (new Color(30, 0, 40, 255)).getRGB()), new Theme("Sunset", (new Color(255, 94, 0, 255)).getRGB(), (new Color(40, 20, 0, 255)).getRGB()), new Theme("Neon Green", (new Color(57, 255, 20, 255)).getRGB(), (new Color(10, 20, 10, 255)).getRGB()), new Theme("Abyss Blue", (new Color(0, 102, 204, 255)).getRGB(), (new Color(10, 10, 30, 255)).getRGB()), new Theme("Cotton Candy", (new Color(255, 182, 193, 255)).getRGB(), (new Color(240, 240, 255, 255)).getRGB()), new Theme("Sky Breeze", (new Color(135, 206, 250, 255)).getRGB(), (new Color(220, 240, 255, 255)).getRGB()), new Theme("Obsidian Glow", (new Color(200, 200, 255, 255)).getRGB(), (new Color(10, 10, 15, 230)).getRGB()), new Theme("Quantum Shift", (new Color(100, 255, 230, 255)).getRGB(), (new Color(0, 20, 25, 220)).getRGB()), new Theme("White-Black", (new Color(255, 255, 255, 255)).getRGB(), (new Color(0, 0, 0, 255)).getRGB()), new Theme("Peach Cream", (new Color(255, 218, 185, 255)).getRGB(), (new Color(255, 245, 235, 255)).getRGB()), new Theme("Mint Ice", (new Color(189, 255, 201, 255)).getRGB(), (new Color(230, 255, 240, 255)).getRGB()), new Theme("Lavender Fog", (new Color(200, 160, 255, 255)).getRGB(), (new Color(240, 230, 255, 255)).getRGB()), new Theme("Emerald", (new Color(80, 200, 120, 255)).getRGB(), (new Color(10, 30, 20, 255)).getRGB())}));
      this.themes.addAll(List.of(new Theme[]{
         new Theme("Redstone", 0xFFFF3B30, 0xFF1A1A1A),
         new Theme("Orange Pop", 0xFFFF9500, 0xFF2B1A00),
         new Theme("Amber", 0xFFFFCC00, 0xFF332900),
         new Theme("Lime", 0xFFA8E10C, 0xFF223300),
         new Theme("Spring", 0xFF00E5A0, 0xFF00221A),
         new Theme("Ocean", 0xFF00B8D9, 0xFF002233),
         new Theme("Sky Blue", 0xFF4DA6FF, 0xFF0A1A2E),
         new Theme("Indigo", 0xFF5E5CE6, 0xFF12102E),
         new Theme("Purple", 0xFFBF5AF2, 0xFF22102E),
         new Theme("Magenta", 0xFFFF2D92, 0xFF2E0A1C),
         new Theme("Pink", 0xFFFF9FC7, 0xFF2E121C),
         new Theme("Crimson", 0xFFFF4D5E, 0xFF2E0A0F),
         new Theme("Gold", 0xFFFFD700, 0xFF33270A),
         new Theme("Copper", 0xFFC77B4B, 0xFF2E1A0F),
         new Theme("Bronze", 0xFFCD7F32, 0xFF2B1A0A),
         new Theme("Silver", 0xFFC0C0C0, 0xFF22222E),
         new Theme("Graphite", 0xFF8E8E93, 0xFF141418),
         new Theme("Charcoal", 0xFF6E6E7E, 0xFF101014),
         new Theme("Ice", 0xFFB8E6FF, 0xFF0A1622),
         new Theme("Snow", 0xFFF0F8FF, 0xFF1A222E),
         new Theme("Fire", 0xFFFF4D00, 0xFF2E0F00),
         new Theme("Lava", 0xFFFF6A00, 0xFF331400),
         new Theme("Ember", 0xFFFF8C42, 0xFF2E1800),
         new Theme("Toxic", 0xFF9EFF00, 0xFF1A2E00),
         new Theme("Radiation", 0xFF52FF00, 0xFF0F2E00),
         new Theme("Nuclear", 0xFFB5FF1A, 0xFF1F3300),
         new Theme("Cyberpunk", 0xFF00FFF7, 0xFF1A0033),
         new Theme("Neon Pink", 0xFFFF00E5, 0xFF330022),
         new Theme("Ultraviolet", 0xFF7A00FF, 0xFF14002E),
         new Theme("Vaporwave", 0xFFFF71CE, 0xFF2E0A55),
         new Theme("Sunrise", 0xFFFFB347, 0xFF3A1E0A),
         new Theme("Sunset Orange", 0xFFFF5E3A, 0xFF3A1000),
         new Theme("Twilight", 0xFF9B59B6, 0xFF1A0A2E),
         new Theme("Midnight", 0xFF2C3E50, 0xFF080D14),
         new Theme("Night", 0xFF1E2A78, 0xFF05070F),
         new Theme("Starlight", 0xFFD6E4FF, 0xFF0A1226),
         new Theme("Galaxy", 0xFF9B6DFF, 0xFF0A0618),
         new Theme("Nebula", 0xFFFF5FD7, 0xFF180A24),
         new Theme("Plasma", 0xFF7DF9FF, 0xFF0A1026),
         new Theme("Photon", 0xFFFFF14A, 0xFF262000),
         new Theme("Laser", 0xFFFF4444, 0xFF260808),
         new Theme("Matrix", 0xFF00FF41, 0xFF001A08),
         new Theme("Terminal", 0xFF33FF33, 0xFF001100),
         new Theme("Hacker", 0xFF00C853, 0xFF04140A),
         new Theme("Siren", 0xFFFF0040, 0xFF2E000F),
         new Theme("Alert", 0xFFFF6600, 0xFF331100),
         new Theme("Signal", 0xFF00E5FF, 0xFF002633),
         new Theme("Sapphire", 0xFF1A6DFF, 0xFF060D2E),
         new Theme("Azure", 0xFF0090FF, 0xFF001A33),
         new Theme("Cyan", 0xFF00D8FF, 0xFF00222E),
         new Theme("Teal", 0xFF00A8A8, 0xFF002222),
         new Theme("Mint", 0xFF00FFB3, 0xFF002E22),
         new Theme("Emerald Dark", 0xFF00C46A, 0xFF002E14),
         new Theme("Forest", 0xFF2E8B57, 0xFF07140C),
         new Theme("Cactus", 0xFF7CB342, 0xFF14220A),
         new Theme("Olive", 0xFF9ACD32, 0xFF1E2E0A),
         new Theme("Banana", 0xFFFFE135, 0xFF332B00),
         new Theme("Honey", 0xFFFFB300, 0xFF331F00),
         new Theme("Maple", 0xFFFF7F50, 0xFF33170A),
         new Theme("Coral", 0xFFFF6F61, 0xFF33100A),
         new Theme("Rose", 0xFFFF4D6D, 0xFF330A14),
         new Theme("Berry", 0xFFB0116B, 0xFF26061A),
         new Theme("Grape", 0xFF8E44AD, 0xFF1F0A2B),
         new Theme("Orchid", 0xFFDA70D6, 0xFF2B0E2B),
         new Theme("Lilac", 0xFFC8A2C8, 0xFF221422),
         new Theme("Plum", 0xFF8E4585, 0xFF220A20),
         new Theme("Chocolate", 0xFF7B3F00, 0xFF241400),
         new Theme("Caramel", 0xFFD29B63, 0xFF2E1A0C),
         new Theme("Cream", 0xFFFFFDD0, 0xFF2E2A1E),
         new Theme("Mocha", 0xFF96786A, 0xFF221610),
         new Theme("Slate", 0xFF708090, 0xFF12161C),
         new Theme("Steel", 0xFFB0BEC5, 0xFF14181C),
         new Theme("Iron", 0xFFD8D8D8, 0xFF1C1C22),
         new Theme("Titanium", 0xFFB8B8C4, 0xFF14141C),
         new Theme("Cobalt", 0xFF005A9C, 0xFF071A2E),
         new Theme("Zinc", 0xFF9EA7B3, 0xFF14171C),
         new Theme("Lead", 0xFF3B3B45, 0xFF0A0A0E),
         new Theme("Mercury", 0xFFE6E6E6, 0xFF1E1E24),
         new Theme("Amethyst", 0xFF9966CC, 0xFF1A0F2B),
         new Theme("Quartz", 0xFFF5F0E8, 0xFF2E2822)
      }));
   }

   public void addTheme(Theme theme) {
      this.themes.add(theme);
   }

   public boolean removeTheme(Theme theme) {
      if (theme.isPreset()) {
         return false;
      }
      if (this.currentTheme == theme) {
         this.currentTheme = this.defaultTheme;
      }
      return this.themes.remove(theme);
   }

   public ColorRGBA getClientColor(int index) {
      return this.currentTheme == null ? new ColorRGBA(255, 255, 255, 255) : ColorUtil.gradient(3, index, this.currentTheme.getColor(), this.currentTheme.getSecondColor());
   }

   @Generated
   public Theme getCurrentTheme() {
      return this.currentTheme;
   }

   @Generated
   public List<Theme> getThemes() {
      return this.themes;
   }

   @Generated
   public Theme getDefaultTheme() {
      return this.defaultTheme;
   }

   @Generated
   public void setCurrentTheme(Theme currentTheme) {
      this.currentTheme = currentTheme;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ThemeManager)) {
         return false;
      } else {
         ThemeManager other = (ThemeManager)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            label47: {
               Object this$currentTheme = this.getCurrentTheme();
               Object other$currentTheme = other.getCurrentTheme();
               if (this$currentTheme == null) {
                  if (other$currentTheme == null) {
                     break label47;
                  }
               } else if (this$currentTheme.equals(other$currentTheme)) {
                  break label47;
               }

               return false;
            }

            Object this$themes = this.getThemes();
            Object other$themes = other.getThemes();
            if (this$themes == null) {
               if (other$themes != null) {
                  return false;
               }
            } else if (!this$themes.equals(other$themes)) {
               return false;
            }

            Object this$defaultTheme = this.getDefaultTheme();
            Object other$defaultTheme = other.getDefaultTheme();
            if (this$defaultTheme == null) {
               if (other$defaultTheme != null) {
                  return false;
               }
            } else if (!this$defaultTheme.equals(other$defaultTheme)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof ThemeManager;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1;
      Object $currentTheme = this.getCurrentTheme();
      result = result * 59 + ($currentTheme == null ? 43 : $currentTheme.hashCode());
      Object $themes = this.getThemes();
      result = result * 59 + ($themes == null ? 43 : $themes.hashCode());
      Object $defaultTheme = this.getDefaultTheme();
      result = result * 59 + ($defaultTheme == null ? 43 : $defaultTheme.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getCurrentTheme());
      return "ThemeManager(currentTheme=" + var10000 + ", themes=" + String.valueOf(this.getThemes()) + ", defaultTheme=" + String.valueOf(this.getDefaultTheme()) + ")";
   }
}
