package tech.huihui.client.hud.elements.component;

import java.util.Iterator;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.lang.Lang;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.hud.elements.draggable.DraggableHudElement;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.impl.render.Interface;
import tech.huihui.utility.render.display.Keyboard;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class KeybindsComponent extends DraggableHudElement {
   private final Animation widthAnimation;
   private final Animation xLine;
   private final Animation alpha;

   public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.xLine = new Animation(170L, Easing.SINE_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      float posX = this.getX();
      float posY = this.getY();
      float spacing = Interface.INSTANCE.hudSpacing.getCurrent();
      float defaultWidth = 53.0F;
      float height = 14.5F;
      boolean isFound = false;
      Iterator var7 = HuihuiClient.getInstance().getModuleManager().getModules().iterator();

      while(var7.hasNext()) {
         Module module = (Module)var7.next();
         if (module.isEnabled() && module.getKeyCode() != -1) {
            this.alpha.update(1.0F);
            isFound = true;
         }
      }

      if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
         this.alpha.update(0.0F);
      }

      if (mc.currentScreen instanceof ChatScreen) {
         this.alpha.update(1.0F);
      }

      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, this.widthAnimation.getValue(), 14.5F, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(80, 80, 80, 255.0F * this.alpha.getValue()));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + 15.0F, posY + 1.5F, 0.5F, 12.25F, BorderRadius.all(0.0F), new ColorRGBA(166, 166, 166, 255.0F * this.alpha.getValue()));
      ctx.drawText(Fonts.ICONS2.getFont(7.0F), "\uf11c", posX + 4.0F, posY + 5.0F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));
      String keybindsTitle = Lang.t(Interface.INSTANCE.lang.get(), "Бинды", "KeyBinds", "按键绑定");
      ctx.drawText(Fonts.REGULAR.getFont(7.0F), keybindsTitle, posX + 19.5F, posY + 4.75F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));
      posY += 14.5F;
      float bindWidth = 0.0F;
      Iterator var9 = HuihuiClient.getInstance().getModuleManager().getModules().iterator();

      Module module;
      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            float localBindWidth = Fonts.REGULAR.getWidth(Keyboard.getKeyName(module.getKeyCode()), 6.75F);
            if (localBindWidth > bindWidth) {
               bindWidth = localBindWidth;
            }
         }
      }

      this.xLine.update(bindWidth);
      var9 = HuihuiClient.getInstance().getModuleManager().getModules().iterator();

      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            height += spacing;
            String bind = Keyboard.getKeyName(module.getKeyCode());
            String moduleName = module.getName();
            float elementsWidth = Fonts.REGULAR.getWidth(moduleName, 6.75F) + Fonts.REGULAR.getWidth(bind, 6.75F) + 40.0F;
            DrawUtil.drawBlur(ctx.getMatrices(), posX, posY + module.getAnimation().getValue() * 3.0F - 3.0F, this.widthAnimation.getValue(), spacing, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(80, 80, 80, 255.0F * module.getAnimation().getValue() * this.alpha.getValue()));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + this.widthAnimation.getValue() - 6.5F - this.xLine.getValue(), posY + module.getAnimation().getValue() * 3.0F - 3.0F + 1.5F, 0.5F, 8.75F, BorderRadius.all(0.0F), new ColorRGBA(166, 166, 166, 255.0F * module.getAnimation().getValue() * this.alpha.getValue()));
            ctx.drawText(Fonts.ICONS.getFont(5.0F), module.getCategory().getIcon(), posX + 3.15F, posY + module.getAnimation().getValue() * 3.0F - 3.0F + 3.5F, theme.getColor().withAlpha(module.getAnimation().getValue() * 255.0F * this.alpha.getValue()));
            ctx.drawText(Fonts.REGULAR.getFont(6.5F), moduleName, posX + 10.5F, posY + module.getAnimation().getValue() * 3.0F - 3.0F + 3.25F, (new ColorRGBA(-1)).withAlpha(module.getAnimation().getValue() * 255.0F * this.alpha.getValue()));
            ctx.drawText(Fonts.REGULAR.getFont(6.5F), bind, posX + this.widthAnimation.getValue() - 3.0F - this.xLine.getValue() - Fonts.REGULAR.getWidth(bind, 6.75F) / 2.0F + this.xLine.getValue() / 2.0F, posY + module.getAnimation().getValue() * 3.0F - 3.0F + 3.25F, (new ColorRGBA(-1)).withAlpha(module.getAnimation().getValue() * 255.0F * this.alpha.getValue()));
            if (elementsWidth > defaultWidth) {
               defaultWidth = elementsWidth;
            }

            posY += spacing * module.getAnimation().getValue();
         }
      }

      this.widthAnimation.update(defaultWidth);
      this.width = this.widthAnimation.getValue();
      this.height = height;
   }
}
