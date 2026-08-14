package tech.huihui.client.hud.elements.component;

import java.util.List;
import java.util.Locale;
import net.minecraft.item.ItemStack;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.lang.Lang;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.hud.elements.draggable.DraggableHudElement;
import tech.huihui.client.modules.impl.misc.ServerHelper;
import tech.huihui.client.modules.impl.render.Interface;
import tech.huihui.utility.render.display.Keyboard;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class HelperBindsComponent extends DraggableHudElement {
   private final Animation alpha = new Animation(200L, Easing.CUBIC_OUT);
   private final Animation widthAnimation = new Animation(200L, Easing.CUBIC_OUT);

   public HelperBindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
   }

   public void render(CustomDrawContext ctx) {
      float posX = this.getX();
      float posY = this.getY();
      float spacing = Interface.INSTANCE.hudSpacing.getCurrent();
      float headerHeight = 14.5F;
      ServerHelper helper = ServerHelper.INSTANCE;
      List<ServerHelper.AbilityInfo> data = helper.isEnabled() ? helper.getHudData(mc.getRenderTickCounter().getTickDelta(false)) : List.of();
      this.alpha.update(data.isEmpty() ? 0.0F : 1.0F);
      float a = this.alpha.getValue();
      if (a <= 0.01F) {
         this.width = 0.0F;
         this.height = 0.0F;
         return;
      }

      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      float nameWidth = 0.0F;
      float keyWidth = 0.0F;
      float cdWidth = 0.0F;
      for (ServerHelper.AbilityInfo info : data) {
         nameWidth = Math.max(nameWidth, Fonts.REGULAR.getWidth(info.getName(), 6.5F));
         keyWidth = Math.max(keyWidth, Fonts.REGULAR.getWidth(Keyboard.getKeyName(info.getKeyCode()), 6.5F));
         if (info.hasCooldown()) {
            cdWidth = Math.max(cdWidth, Fonts.REGULAR.getWidth(String.format(Locale.US, "%.1fs", info.getCooldownSeconds()), 6.0F));
         }
      }
      float targetWidth = Math.max(88.0F, nameWidth + keyWidth + cdWidth + 42.0F);
      float targetHeight = headerHeight + (float) data.size() * spacing;
      this.widthAnimation.update(targetWidth);
      float w = this.widthAnimation.getValue();
      float h = targetHeight;
      this.width = w;
      this.height = h;

      DrawUtil.drawBlur(ctx.getMatrices(), posX, posY, w, h, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(80, 80, 80, 255.0F * a));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + 15.0F, posY + 1.5F, 0.5F, 12.25F, BorderRadius.all(0.0F), new ColorRGBA(166, 166, 166, 255.0F * a));
      ctx.drawText(Fonts.ICONS2.getFont(7.0F), "\uf11c", posX + 4.0F, posY + 5.0F, theme.getColor().withAlpha(255.0F * a));
      String title = Lang.t(Interface.INSTANCE.lang.get(), "Бинды хелперов", "Helper Binds", "辅助绑定");
      ctx.drawText(Fonts.REGULAR.getFont(7.0F), title, posX + 19.5F, posY + 4.75F, (new ColorRGBA(-1)).withAlpha(255.0F * a));

      float rowY = posY + headerHeight;
      for (ServerHelper.AbilityInfo info : data) {
         ctx.pushMatrix();
         ctx.getMatrices().translate((double) (posX + 2.5F), (double) (rowY + 0.5F), 0.0D);
         ctx.getMatrices().scale(0.75F, 0.75F, 1.0F);
         ctx.drawItem(new ItemStack(info.getItem()), 0, 0);
         ctx.popMatrix();
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), info.getName(), posX + 15.0F, rowY + 3.0F, (new ColorRGBA(-1)).withAlpha(255.0F * a));
         String keyName = Keyboard.getKeyName(info.getKeyCode());
         float kx = posX + w - 3.0F - Fonts.REGULAR.getWidth(keyName, 6.5F);
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), keyName, kx, rowY + 3.0F, theme.getColor().withAlpha(255.0F * a));
         if (info.hasCooldown()) {
            String cd = String.format(Locale.US, "%.1fs", info.getCooldownSeconds());
            ctx.drawText(Fonts.REGULAR.getFont(6.0F), cd, kx - 8.0F - Fonts.REGULAR.getWidth(cd, 6.0F), rowY + 3.5F, (new ColorRGBA(166, 166, 166)).withAlpha(255.0F * a));
         }
         rowY += spacing;
      }
   }
}