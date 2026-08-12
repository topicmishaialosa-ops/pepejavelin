package tech.huihui.client.hud.elements.component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.network.PlayerListEntry;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.font.MsdfFont;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.hud.elements.draggable.DraggableHudElement;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class WatermarkComponent extends DraggableHudElement {
   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

   public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      float x = this.getX();
      float y = this.getY();
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      String name = "Huihui Client";
      String fps = mc.getCurrentFps() + "fps";
      PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      String playerName = NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard();
      String latency = list != null ? list.getLatency() + "ms" : "0ms";
      float nameWidth = Fonts.REGULAR.getWidth(playerName, 7.75F);
      float latencyWidth = Fonts.REGULAR.getWidth(latency, 7.25F);
      float fpsWidth = Fonts.REGULAR.getWidth(fps, 7.25F);
      float width = 92.5F + nameWidth + latencyWidth + fpsWidth;
      DrawUtil.drawBlur(ctx.getMatrices(), x - 0.5F, y - 1.5F, width, 14.25F, 5.0F, BorderRadius.all(3.0F), new ColorRGBA(80, 80, 80, 255));
      ctx.drawText(Fonts.ICONS.getFont(6.5F), "B", x + 3.5F, y + 3.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), name, x + 12.75F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x += 34.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf007", x + 10.0F, y + 3.75F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), playerName, x + 18.0F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x += nameWidth + 14.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 6.0F, y + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf1eb", x + 12.0F, y + 3.65F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), latency, x + 21.5F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x += latencyWidth + 21.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.5F, y + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf624", x + 11.0F, y + 3.95F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), fps, x + 19.0F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      float x2 = this.getX();
      float y2 = this.getY() + 15.0F;
      String time = LocalTime.now().format(TIME_FORMAT);
      String tpsText = formatTps(HuihuiClient.getInstance().getServerHandler().getTPS());
      float serverWidth = Fonts.REGULAR.getWidth(mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null ? mc.getCurrentServerEntry().address : "Неизвестно", 7.25F);
      float timeWidth = Fonts.REGULAR.getWidth(time, 7.25F);
      float tpsWidth = Fonts.REGULAR.getWidth(tpsText + "tps", 7.25F);
      float width2 = 54.0F + serverWidth + timeWidth + tpsWidth;
      DrawUtil.drawBlur(ctx.getMatrices(), x2 - 0.5F, y2 - 1.5F, width2, 14.25F, 5.0F, BorderRadius.all(3.0F), new ColorRGBA(80, 80, 80, 255));
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf0ac", x2 + 3.0F, y2 + 4.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null ? mc.getCurrentServerEntry().address : "Неизвестно", x2 + 11.5F, y2 + 3.5F, new ColorRGBA(255, 255, 255, 255));
      x2 += serverWidth + 13.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x2 + 4.0F, y2 + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf017", x2 + 10.0F, y2 + 3.85F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), time, x2 + 18.5F, y2 + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x2 += timeWidth + 17.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x2 + 5.0F, y2 + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf68f", x2 + 11.5F, y2 + 3.5F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), tpsText + "tps", x2 + 19.0F, y2 + 3.25F, new ColorRGBA(255, 255, 255, 255));
      this.width = width;
      this.height = 29.0F;
   }

   private static String formatTps(double tps) {
      return String.valueOf(Math.round(tps * 10.0D) / 10.0D);
   }
}
