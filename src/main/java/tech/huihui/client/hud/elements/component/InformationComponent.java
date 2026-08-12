package tech.huihui.client.hud.elements.component;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.util.Locale;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.events.impl.other.EventWindowResize;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.hud.elements.draggable.DraggableHudElement;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class InformationComponent extends DraggableHudElement {
   private final Animation yAnimation;

   public InformationComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.yAnimation = new Animation(200L, Easing.CUBIC_OUT);
      EventManager.register(this);
   }

   @EventTarget
   private void onWindowResized(EventWindowResize e) {
      if (mc.currentScreen instanceof ChatScreen) {
         this.yAnimation.setValue((float)(mc.getWindow().getScaledHeight() - 15));
         this.yAnimation.setStartValue((float)(mc.getWindow().getScaledHeight() - 15));
      } else {
         this.yAnimation.setStartValue((float)mc.getWindow().getScaledHeight());
         this.yAnimation.setValue((float)mc.getWindow().getScaledHeight());
      }

   }

   @Native
   public void render(CustomDrawContext ctx) {
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      if (mc.currentScreen instanceof ChatScreen) {
         this.yAnimation.update((float)(mc.getWindow().getScaledHeight() - 15));
      } else {
         this.yAnimation.update((float)mc.getWindow().getScaledHeight());
      }

      int px = (int)Math.floor(mc.player.getX());
      int py = (int)Math.floor(mc.player.getY());
      int pz = (int)Math.floor(mc.player.getZ());
      double speed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
      String coordsText = px + " " + py + " " + pz;
      long scaledSpeed = Math.round(speed * 20.0D * 100.0D);
      String speedText = scaledSpeed / 100L + "." + (scaledSpeed % 100L < 10L ? "0" : "") + scaledSpeed % 100L;
      float coordsWidth = Fonts.REGULAR.getWidth(coordsText, 7.75F);
      float speedWidth = Fonts.REGULAR.getWidth(speedText, 7.75F);
      DrawUtil.drawBlur(ctx.getMatrices(), 4.0F, this.yAnimation.getValue() - 17.0F, coordsWidth + speedWidth + 49.5F, 14.0F, 11.0F, BorderRadius.all(2.0F), new ColorRGBA(80, 80, 80, 255));
      ctx.drawText(Fonts.ICONS2.getFont(7.5F), "\uf57d", 7.75F, this.yAnimation.getValue() - 12.5F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.5F), coordsText, 18.0F, this.yAnimation.getValue() - 12.5F, ColorRGBA.WHITE);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), 18.5F + coordsWidth + 3.0F, this.yAnimation.getValue() - 11.0F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      ctx.drawText(Fonts.ICONS2.getFont(7.5F), "\uf70c", 24.0F + coordsWidth + 3.5F, this.yAnimation.getValue() - 12.5F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.5F), speedText + " Б/С", 24.0F + coordsWidth + 12.0F, this.yAnimation.getValue() - 12.5F, ColorRGBA.WHITE);
   }
}
