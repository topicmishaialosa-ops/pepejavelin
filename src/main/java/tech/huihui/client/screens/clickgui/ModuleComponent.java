package tech.huihui.client.screens.clickgui;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.setting.Setting;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.screens.clickgui.settings.BindComponent;
import tech.huihui.client.screens.clickgui.settings.BooleanComponent;
import tech.huihui.client.screens.clickgui.settings.ButtonComponent;
import tech.huihui.client.screens.clickgui.settings.ColorComponent;
import tech.huihui.client.screens.clickgui.settings.ModeComponent;
import tech.huihui.client.screens.clickgui.settings.MultiBoxComponent;
import tech.huihui.client.screens.clickgui.settings.SliderComponent;
import tech.huihui.client.screens.clickgui.settings.StringComponent;
import tech.huihui.utility.render.display.Keyboard;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.base.color.ColorUtil;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class ModuleComponent extends Component {
   private final ClickGuiScreen screen;
   private final Module module;
   private final List<Component> components = new ArrayList();
   private final Animation expandAnim = new Animation(200L, Easing.CIRC_OUT);
   private boolean open;
   private boolean bind;

   public ModuleComponent(ClickGuiScreen screen, Module module) {
      this.screen = screen;
      this.module = module;

      for (Setting setting : module.getSettings()) {
         if (setting instanceof BooleanSetting bool) {
            this.components.add(new BooleanComponent(bool));
         } else if (setting instanceof NumberSetting number) {
            this.components.add(new SliderComponent(number));
         } else if (setting instanceof KeySetting key) {
            this.components.add(new BindComponent(key));
         } else if (setting instanceof ModeSetting mode) {
            this.components.add(new ModeComponent(mode));
         } else if (setting instanceof MultiBooleanSetting mode) {
            this.components.add(new MultiBoxComponent(mode));
         } else if (setting instanceof ColorSetting color) {
            this.components.add(new ColorComponent(color));
         } else if (setting instanceof ButtonSetting button) {
            this.components.add(new ButtonComponent(button));
         } else if (setting instanceof StringSetting stringSetting) {
            this.components.add(new StringComponent(stringSetting));
         }
      }
   }

   public float getCurrentHeight() {
      return ClickGuiScreen.MODULE_HEIGHT + this.getSettingsHeight() * this.expandAnim.getValue();
   }

   private float getSettingsHeight() {
      float height = 0.0F;
      for (Component component : this.components) {
         if (component.isVisible()) {
            height += component.getHeight();
         }
      }
      return height;
   }

   private boolean hasVisibleSettings() {
      for (Component component : this.components) {
         if (component.isVisible()) {
            return true;
         }
      }
      return false;
   }

   @Override
   public void render(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      this.expandAnim.update(this.open ? 1.0F : 0.0F);
      this.module.getAnimation().update(this.module.isEnabled());
      float enabled = this.module.getAnimation().getValue();
      float expanded = this.expandAnim.getValue();
      float settingsHeight = this.getSettingsHeight();
      float settingsH = settingsHeight * expanded;
      float rowHeight = ClickGuiScreen.MODULE_HEIGHT + settingsH;

      boolean hovered = this.isHovered(mouseX, mouseY, rowHeight);
      if (hovered) {
         this.screen.setHoveredModule(this.module);
      }

      float radius = 5.0F;
      boolean showContainer = expanded > 0.001F && settingsHeight > 0.0F;

      ColorRGBA headerTop = this.module.isEnabled() ? (new ColorRGBA(45, 46, 53)).withAlpha((hovered ? 85.0F : 65.0F) * alpha) : (new ColorRGBA(153, 153, 153)).withAlpha((hovered ? 24.0F : 15.0F) * alpha);
      ColorRGBA headerBottom = this.module.isEnabled() ? (new ColorRGBA(25, 26, 31)).withAlpha(0.0F) : (new ColorRGBA(153, 153, 153)).withAlpha(0.0F);
      BorderRadius headerRadius = showContainer ? BorderRadius.top(radius, radius) : BorderRadius.all(radius);
      DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 0.7F, this.y + 1.0F, this.width - 2.0F, ClickGuiScreen.MODULE_HEIGHT - 2.0F, headerRadius, headerTop, headerBottom, headerBottom, headerTop);

      if (this.module.isEnabled()) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 0.7F, this.y + 3.5F, 2.0F, ClickGuiScreen.MODULE_HEIGHT - 7.0F, BorderRadius.all(1.0F), theme.getColor().withAlpha(255.0F * alpha));
      }

      if (showContainer) {
         float containerAlpha = Math.min(1.0F, expanded * 1.3F);
         float cX = this.x + 5.5F;
         float cY = this.y + ClickGuiScreen.MODULE_HEIGHT - 0.5F;
         float cW = this.width - 11.0F;
         float cH = settingsH - 1.0F;
         BorderRadius containerRadius = BorderRadius.bottom(radius, radius);
         DrawUtil.drawRoundedRect(draw.getMatrices(), cX, cY, cW, cH, containerRadius, (new ColorRGBA(9, 9, 11)).withAlpha((int)(containerAlpha * 200.0F * alpha)));
         DrawUtil.drawRoundedBorder(draw.getMatrices(), cX, cY, cW, cH, 1.0F, containerRadius, (new ColorRGBA(30, 30, 34)).withAlpha((int)(containerAlpha * 120.0F * alpha)));
         DrawUtil.drawRoundedRect(draw.getMatrices(), cX + 8.0F, cY + 0.5F, cW - 16.0F, 1.0F, BorderRadius.all(0.5F), theme.getColor().withAlpha((int)(containerAlpha * 70.0F * alpha)));
      }

      ColorRGBA textColor = this.module.isEnabled() ? (new ColorRGBA(222, 222, 222)).withAlpha(255.0F * alpha) : (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha);
      if (hovered) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), this.x + 0.7F, this.y + 1.0F, this.width - 2.0F, ClickGuiScreen.MODULE_HEIGHT - 2.0F, headerRadius, (new ColorRGBA(255, 255, 255)).withAlpha((int)(5.0F * alpha)));
      }
      draw.drawText(Fonts.REGULAR.getFont(6.5F), this.module.getName(), this.x + 7.0F, this.y + 6.5F, textColor);

      if (this.bind) {
         String bindText = this.module.getKeyCode() == -1 ? "Bind..." : Keyboard.getKeyName(this.module.getKeyCode());
         draw.drawText(Fonts.REGULAR.getFont(5.5F), bindText, this.x + this.width / 2.0F - Fonts.REGULAR.getWidth(bindText, 5.5F) / 2.0F, this.y + 7.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      } else if (this.module.getKeyCode() != -1) {
         String bindText = Keyboard.getKeyName(this.module.getKeyCode());
         draw.drawText(Fonts.REGULAR.getFont(5.5F), bindText, this.x + this.width - 4.0F - Fonts.REGULAR.getWidth(bindText, 5.5F), this.y + 7.0F, (new ColorRGBA(111, 111, 111)).withAlpha(255.0F * alpha));
      } else if (this.hasVisibleSettings()) {
         ColorRGBA iconColor = ColorUtil.interpolate((new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha), Math.max(enabled, expanded));
         draw.drawText(Fonts.ICONS.getFont(8.0F), "S", this.x + this.width - 14.0F, this.y + 5.5F, iconColor);
      }

      if (settingsH > 0.0F) {
         this.renderSettings(draw, theme, mouseX, mouseY, alpha);
      }
   }

   private void renderSettings(CustomDrawContext draw, Theme theme, float mouseX, float mouseY, float alpha) {
      float cX = this.x + 5.5F;
      float cY = this.y + ClickGuiScreen.MODULE_HEIGHT - 0.5F;
      float cW = this.width - 11.0F;
      float cH = this.getSettingsHeight() * this.expandAnim.getValue() - 1.0F;
      this.screen.scissor(draw, cX, cY, cW, cH);
      float settingY = this.y + ClickGuiScreen.MODULE_HEIGHT;
      float settingX = this.x + 6.5F;
      float settingW = this.width - 13.0F;
      for (Component component : this.components) {
         Setting setting = this.getSetting(component);
         setting.getAnimationAlpha().update(setting.isVisible());
         float componentAlpha = alpha * setting.getAnimationAlpha().getValue();
         if (componentAlpha <= 0.01F) {
            continue;
         }

         float componentHeight = component.getHeight();
         component.setX(settingX);
         component.setY(settingY);
         component.setWidth(settingW);
         if (component.isHovered(mouseX, mouseY)) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), settingX, settingY, settingW, componentHeight, BorderRadius.all(4.0F), (new ColorRGBA(255, 255, 255)).withAlpha((int)(6.0F * alpha)));
         }

         component.render(draw, theme, mouseX, mouseY, componentAlpha);
         settingY += componentHeight;
      }
      draw.disableScissor();
   }

   private Setting getSetting(Component component) {
      if (component instanceof BooleanComponent bool) {
         return bool.getSetting();
      }
      if (component instanceof SliderComponent slider) {
         return slider.getSetting();
      }
      if (component instanceof BindComponent bindComponent) {
         return bindComponent.getSetting();
      }
      if (component instanceof ModeComponent mode) {
         return mode.getSetting();
      }
      if (component instanceof MultiBoxComponent multiBox) {
         return multiBox.getSetting();
      }
      if (component instanceof ColorComponent color) {
         return color.getSetting();
      }
      if (component instanceof StringComponent stringComponent) {
         return stringComponent.getSetting();
      }
      return ((ButtonComponent) component).getSetting();
   }

    @Override
    public boolean mouseClick(float mouseX, float mouseY, int button) {
       if (this.bind) {
          this.module.setKeyCode(button);
          this.bind = false;
          return true;
       }
       if (this.isHovered(mouseX, mouseY, ClickGuiScreen.MODULE_HEIGHT)) {
         if (button == 0) {
            this.module.toggle();
            return true;
         }
         if (button == 1) {
            if (this.hasVisibleSettings()) {
               this.open = !this.open;
            }
            return true;
         }
         if (button == 2) {
            this.bind = !this.bind;
            return true;
         }
      }
      if (this.open) {
         for (Component component : this.components) {
            if (component.isVisible() && component.mouseClick(mouseX, mouseY, button)) {
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public void mouseRelease(float mouseX, float mouseY, int button) {
      for (Component component : this.components) {
         if (component.isVisible()) {
            component.mouseRelease(mouseX, mouseY, button);
         }
      }
   }

   @Override
   public boolean keyPressed(int key, int scanCode, int modifiers) {
      for (Component component : this.components) {
         if (component.isVisible() && component.keyPressed(key, scanCode, modifiers)) {
            return true;
         }
      }
      if (this.bind) {
         if (key == 261) {
            this.module.setKeyCode(-1);
         } else {
            this.module.setKeyCode(key);
         }
         this.bind = false;
         return true;
      }
      return false;
   }

   @Override
   public boolean charTyped(char codePoint, int modifiers) {
      for (Component component : this.components) {
         if (component.isVisible() && component.charTyped(codePoint, modifiers)) {
            return true;
         }
      }
      return false;
   }
}
