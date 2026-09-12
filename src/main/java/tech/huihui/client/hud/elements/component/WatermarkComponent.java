package tech.huihui.client.hud.elements.component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.network.PlayerListEntry;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.font.MsdfFont;
import tech.huihui.base.lang.Lang;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.hud.elements.draggable.DraggableHudElement;
import tech.huihui.client.hud.elements.style.HudStyle2;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.client.modules.impl.render.Interface;
import tech.huihui.client.modules.impl.misc.RenamePasterClient;
import tech.huihui.client.modules.impl.render.Watermark;
import tech.huihui.client.screens.watermark.WatermarkLogo;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class WatermarkComponent extends DraggableHudElement {
   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
   private final Map<String, CachedLogo> logoCache = new HashMap<>();

   private static final class CachedLogo {
      final String data;
      final WatermarkLogo logo;

      CachedLogo(String data, WatermarkLogo logo) {
         this.data = data;
         this.logo = logo;
      }
   }

   public WatermarkComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
   }

   @Native
   public void render(CustomDrawContext ctx) {
      if (Interface.INSTANCE.hudStyle.is("Стиль 2")) {
         this.renderStyle2(ctx);
         return;
      }
      Watermark module = Watermark.INSTANCE;
      float scale = module.logoScale.getCurrent();
      float x = this.getX();
      float y = this.getY();
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      String name = RenamePasterClient.getClientName();
      String fps = mc.getCurrentFps() + "fps";
      PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      String playerName = NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard();
      String latency = list != null ? list.getLatency() + "ms" : "0ms";
      float nameWidth = Fonts.REGULAR.getWidth(playerName, 7.75F);
      float latencyWidth = Fonts.REGULAR.getWidth(latency, 7.25F);
      float fpsWidth = Fonts.REGULAR.getWidth(fps, 7.25F);
      float width = 92.5F + nameWidth + latencyWidth + fpsWidth;
      DrawUtil.drawBlur(ctx.getMatrices(), x - 0.5F, y - 1.5F, width, 14.25F, 5.0F, BorderRadius.all(3.0F), new ColorRGBA(80, 80, 80, 255));
      this.renderCustomIcon(ctx, module, "logo", "B", x + 3.5F, y + 3.0F, 6.5F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), name, x + 12.75F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x += 34.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      this.renderCustomIcon(ctx, module, "nick", "\uf007", x + 10.0F, y + 3.75F, 6.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), playerName, x + 18.0F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x += nameWidth + 14.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 6.0F, y + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      this.renderCustomIcon(ctx, module, "ping", "\uf1eb", x + 12.0F, y + 3.65F, 6.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), latency, x + 21.5F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x += latencyWidth + 21.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.5F, y + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      this.renderCustomIcon(ctx, module, "fps", "\uf624", x + 11.0F, y + 3.95F, 6.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), fps, x + 19.0F, y + 3.25F, new ColorRGBA(255, 255, 255, 255));
      float x2 = this.getX();
      float y2 = this.getY() + 15.0F;
      String time = LocalTime.now().format(TIME_FORMAT);
      String tpsText = formatTps(HuihuiClient.getInstance().getServerHandler().getTPS());
      String serverName = mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null ? mc.getCurrentServerEntry().address : Lang.t(module.lang.get(), "Неизвестно", "Unknown", "未知");
      float serverWidth = Fonts.REGULAR.getWidth(serverName, 7.25F);
      float timeWidth = Fonts.REGULAR.getWidth(time, 7.25F);
      float tpsWidth = Fonts.REGULAR.getWidth(tpsText + "tps", 7.25F);
      float width2 = 54.0F + serverWidth + timeWidth + tpsWidth;
      DrawUtil.drawBlur(ctx.getMatrices(), x2 - 0.5F, y2 - 1.5F, width2, 14.25F, 5.0F, BorderRadius.all(3.0F), new ColorRGBA(80, 80, 80, 255));
      this.renderCustomIcon(ctx, module, "server", "\uf0ac", x2 + 3.0F, y2 + 4.0F, 6.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), serverName, x2 + 11.5F, y2 + 3.5F, new ColorRGBA(255, 255, 255, 255));
      x2 += serverWidth + 13.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x2 + 4.0F, y2 + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      this.renderCustomIcon(ctx, module, "time", "\uf017", x2 + 10.0F, y2 + 3.85F, 6.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), time, x2 + 18.5F, y2 + 3.25F, new ColorRGBA(255, 255, 255, 255));
      x2 += timeWidth + 17.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x2 + 5.0F, y2 + 5.25F, 2.0F, 2.0F, BorderRadius.all(0.5F), theme.getColor());
      this.renderCustomIcon(ctx, module, "tps", "\uf68f", x2 + 11.5F, y2 + 3.5F, 6.0F, theme.getColor());
      ctx.drawText(Fonts.REGULAR.getFont(7.25F), tpsText + "tps", x2 + 19.0F, y2 + 3.25F, new ColorRGBA(255, 255, 255, 255));
      this.width = width;
      this.height = 29.0F;
   }

   private void renderStyle2(CustomDrawContext ctx) {
      Watermark module = Watermark.INSTANCE;
      float x = this.getX();
      float y = this.getY();
      String name = RenamePasterClient.getClientName();
      String fps = mc.getCurrentFps() + "";
      PlayerListEntry list = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      String latency = list != null ? list.getLatency() + "" : "0";
      String serverName = mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null ? mc.getCurrentServerEntry().address : "Unknown";
      String time = LocalTime.now().format(TIME_FORMAT);
      String tpsText = formatTps(HuihuiClient.getInstance().getServerHandler().getTPS());
      double speed = Math.hypot(mc.player.getX() - mc.player.prevX, mc.player.getZ() - mc.player.prevZ);
      long bps = Math.round(speed * 20.0D);

      float cardW = 130.0F;
      float headerH = 18.0F;
      float rowH = 11.0F;
      float totalH = headerH + rowH * 3 + 6.0F;

      HudStyle2.drawCard(ctx, x, y, cardW, totalH, 255.0F);
      HudStyle2.drawHeader(ctx, "\uf0ac", name, x, y, cardW, 255.0F, HudStyle2.GREEN);
      float ry = y + headerH + 3.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 3.0F, ry, cardW - 6.0F, rowH, HudStyle2.RADIUS_SM,
         HudStyle2.CARD_BG.withAlpha(180));
      HudStyle2.drawKeyValue(ctx, "FPS: ", fps, x + 7.0F, ry + 2.0F, 255.0F);
      HudStyle2.drawKeyValue(ctx, "Ping: ", latency + "ms", x + 50.0F, ry + 2.0F, 255.0F);
      ry += rowH + 2.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 3.0F, ry, cardW - 6.0F, rowH, HudStyle2.RADIUS_SM,
         HudStyle2.CARD_BG.withAlpha(180));
      HudStyle2.drawKeyValue(ctx, "TPS: ", tpsText, x + 7.0F, ry + 2.0F, 255.0F);
      HudStyle2.drawKeyValue(ctx, "Time: ", time, x + 55.0F, ry + 2.0F, 255.0F);
      ry += rowH + 2.0F;

      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 3.0F, ry, cardW - 6.0F, rowH, HudStyle2.RADIUS_SM,
         HudStyle2.CARD_BG.withAlpha(180));
      HudStyle2.drawKeyValue(ctx, "BPS: ", bps + "", x + 7.0F, ry + 2.0F, 255.0F);
      HudStyle2.drawKeyValue(ctx, "Server: ", serverName, x + 50.0F, ry + 2.0F, 255.0F);

      this.width = cardW;
      this.height = totalH;
   }

   private void renderCustomIcon(CustomDrawContext ctx, Watermark module, String id, String fallback, float x, float y, float fontSize, ColorRGBA color) {
      String data = module.getIconLogoData(id);
      CachedLogo cached = this.logoCache.get(id);
      WatermarkLogo custom;
      if (cached != null && cached.data.equals(data)) {
         custom = cached.logo;
      } else {
         custom = WatermarkLogo.deserialize(data);
         this.logoCache.put(id, new CachedLogo(data, custom));
      }

      if (custom != null && !custom.isEmpty()) {
         float slot = 8.0F * module.logoScale.getCurrent();
         custom.renderFit(ctx.getMatrices(), x, y + 0.5F, slot, slot, color);
      } else {
         ctx.drawText(Fonts.ICONS2.getFont(fontSize), fallback, x, y, color);
      }
   }

   private static String formatTps(double tps) {
      return String.valueOf(Math.round(tps * 10.0D) / 10.0D);
   }
}
