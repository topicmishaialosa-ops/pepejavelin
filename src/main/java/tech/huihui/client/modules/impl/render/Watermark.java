package tech.huihui.client.modules.impl.render;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.input.EventMouse;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.client.modules.impl.misc.RenamePasterClient;
import tech.huihui.client.screens.watermark.WatermarkLogo;
import tech.huihui.client.screens.watermark.WatermarkLogoEditorScreen;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.GuiUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(name = "Watermark", category = Category.RENDER, description = "Ватермарка с 10 стилями")
public final class Watermark extends Module {

   private static final String[] STYLES = new String[]{
      "Классика", "Минимал", "Градиент", "Неон", "Акцент-бар",
      "Профиль", "Стек", "Часы", "Терминал", "Бейдж"
   };
   private static final String ICON_NICK = "\uf007";
   private static final String ICON_PING = "\uf1eb";
   private static final String ICON_FPS = "\uf624";
   private static final String ICON_TPS = "\uf68f";
   private static final String ICON_SERVER = "\uf0ac";
   private static final String ICON_TIME = "\uf017";
   private static final String ICON_DATE = "\uf073";

   private static final ColorRGBA TEXT = new ColorRGBA(255, 255, 255, 255);
   private static final ColorRGBA DIM = new ColorRGBA(150, 150, 158, 255);
   private static final ColorRGBA BLACK = new ColorRGBA(0, 0, 0, 140);

   public final ModeSetting style = new ModeSetting("Стиль", STYLES);
   public final NumberSetting x = new NumberSetting("Позиция X", 4.0F, 0.0F, 1920.0F, 1.0F);
   public final NumberSetting y = new NumberSetting("Позиция Y", 4.0F, 0.0F, 1080.0F, 1.0F);
   public final ColorSetting accent = new ColorSetting("Акцент", new ColorRGBA(88, 166, 255, 255));
   public final BooleanSetting showName = new BooleanSetting("Название клиента", true);
   public final BooleanSetting showNick = new BooleanSetting("Ник", true);
   public final BooleanSetting showPing = new BooleanSetting("Пинг", true);
   public final BooleanSetting showFps = new BooleanSetting("ФПС", true);
   public final BooleanSetting showTps = new BooleanSetting("ТПС", true);
   public final BooleanSetting showTime = new BooleanSetting("Время", true);
   public final BooleanSetting showServer = new BooleanSetting("Сервер", true);
   public final ButtonSetting openLogoEditor = new ButtonSetting("Открыть редактор логотипа", WatermarkLogoEditorScreen::openEditor);
   private final StringSetting logoData = new StringSetting("Данные логотипа", "");

   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private float[] lastSize = new float[]{110.0F, 16.0F};

   private Watermark() {
      this.logoData.setVisible(() -> false);
   }

   public String getLogoData() {
      return this.logoData.getValue();
   }

   public void setLogoData(String data) {
      this.logoData.setValue(data);
   }

   private void renderLogo(CustomDrawContext ctx, float x, float y, ColorRGBA color) {
      WatermarkLogo logo = WatermarkLogo.deserialize(this.logoData.getValue());
      if (logo == null || logo.isEmpty()) {
         ctx.drawText(Fonts.ICONS.getFont(6.5F), "B", x, y, color);
         return;
      }
      logo.renderFit(ctx.getMatrices(), x - 0.5F, y - 2.5F, 11.0F, 11.0F, color);
   }

   public static final Watermark INSTANCE = new Watermark();

   @EventTarget
   @Native
   public void onRender(EventHudRender event) {
      if (mc.world == null || mc.player == null || mc.options.hudHidden) {
         return;
      }
      if (this.dragging) {
         Vector2f mousePos = GuiUtil.getMouse(2.0D);
         float[] size = this.currentSize();
         float scaledWidth = (float)mc.getWindow().getScaledWidth() / 2.0F;
         float scaledHeight = (float)mc.getWindow().getScaledHeight() / 2.0F;
         this.x.setCurrent(MathHelper.clamp(mousePos.getX() - this.dragOffsetX, 0.0F, Math.max(scaledWidth - size[0], 0.0F)));
         this.y.setCurrent(MathHelper.clamp(mousePos.getY() - this.dragOffsetY, 0.0F, Math.max(scaledHeight - size[1], 0.0F)));
      }
      this.renderWatermark(event.getContext(), this.x.getCurrent(), this.y.getCurrent());
   }

   @EventTarget
   @Native
   public void onMouse(EventMouse event) {
      if (!(mc.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         return;
      }
      Vector2f mousePos = GuiUtil.getMouse(2.0D);
      float mouseX = mousePos.getX();
      float mouseY = mousePos.getY();
      float[] size = this.currentSize();
      if (event.getAction() == 1 && event.getButton() == 0) {
         if (mouseX >= this.x.getCurrent() && mouseX <= this.x.getCurrent() + size[0] && mouseY >= this.y.getCurrent() && mouseY <= this.y.getCurrent() + size[1]) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.x.getCurrent();
            this.dragOffsetY = mouseY - this.y.getCurrent();
         }
      } else if (event.getAction() == 0) {
         this.dragging = false;
      }
   }

   private float[] currentSize() {
      return this.lastSize;
   }

   private List<Cell> cells() {
      List<Cell> cells = new ArrayList<>();
      PlayerListEntry list = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
      if (this.showName.isEnabled()) {
         cells.add(new Cell("", RenamePasterClient.getClientName()));
      }
      if (this.showNick.isEnabled()) {
         cells.add(new Cell(ICON_NICK, NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard()));
      }
      if (this.showPing.isEnabled()) {
         cells.add(new Cell(ICON_PING, list != null ? list.getLatency() + "ms" : "0ms"));
      }
      if (this.showFps.isEnabled()) {
         cells.add(new Cell(ICON_FPS, mc.getCurrentFps() + "fps"));
      }
      if (this.showTps.isEnabled()) {
         cells.add(new Cell(ICON_TPS, Math.round(HuihuiClient.getInstance().getServerHandler().getTPS() * 10.0F) / 10.0F + "tps"));
      }
      if (this.showServer.isEnabled()) {
         cells.add(new Cell(ICON_SERVER, mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null ? mc.getCurrentServerEntry().address : "Неизвестно"));
      }
      if (this.showTime.isEnabled()) {
         cells.add(new Cell(ICON_TIME, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
      }
      if (cells.isEmpty()) {
         cells.add(new Cell("", RenamePasterClient.getClientName()));
      }
      return cells;
   }

   private int styleIndex() {
      for (int i = 0; i < STYLES.length; i++) {
         if (this.style.is(STYLES[i])) {
            return i;
         }
      }
      return 0;
   }

   private ColorRGBA accent() {
      return this.accent.getColor();
   }

   private void renderWatermark(CustomDrawContext ctx, float x, float y) {
      switch (this.styleIndex()) {
         case 1 -> this.renderMinimal(ctx, x, y);
         case 2 -> this.renderGradient(ctx, x, y);
         case 3 -> this.renderNeon(ctx, x, y);
         case 4 -> this.renderAccentBar(ctx, x, y);
         case 5 -> this.renderProfile(ctx, x, y);
         case 6 -> this.renderStack(ctx, x, y);
         case 7 -> this.renderClock(ctx, x, y);
         case 8 -> this.renderTerminal(ctx, x, y);
         case 9 -> this.renderBadge(ctx, x, y);
         default -> this.renderClassic(ctx, x, y);
      }
      if (this.dragging) {
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), x - 1.0F, y - 1.0F, this.lastSize[0] + 2.0F, this.lastSize[1] + 2.0F, 1.0F, BorderRadius.all(5.0F), this.accent());
      }
   }

   private void renderClassic(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      float width = 12.0F + 14.0F + Math.max(this.cellsWidth(cells, 7.25F), 34.0F);
      float height = 16.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), BLACK);
      float cursor = x + 6.0F;
      this.renderLogo(ctx, cursor + 0.5F, y + 4.75F, this.accent());
      cursor += 14.0F;
      this.renderCells(ctx, cells, cursor, y, this.accent());
      this.lastSize = new float[]{width, height};
   }

   private void renderMinimal(CustomDrawContext ctx, float x, float y) {
      List<String> parts = new ArrayList<>();
      for (Cell cell : this.cells()) {
         parts.add(cell.label());
      }
      String text = String.join(" · ", parts);
      float width = 8.0F + Fonts.MEDIUM.getWidth(text, 7.25F);
      float height = 13.0F;
      ctx.drawText(Fonts.MEDIUM.getFont(7.25F), text, x + 5.0F, y + 3.5F, new ColorRGBA(0, 0, 0, 130));
      ctx.drawText(Fonts.MEDIUM.getFont(7.25F), text, x + 4.0F, y + 2.5F, new ColorRGBA(235, 235, 235, 255));
      this.lastSize = new float[]{width, height};
   }

   private void renderGradient(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      float width = 16.0F + 14.0F + Math.max(this.cellsWidth(cells, 7.25F), 40.0F);
      float height = 20.0F;
      ColorRGBA a1 = this.accent().withAlpha(120);
      ColorRGBA a2 = this.accent().brighter(0.3F).withAlpha(100);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(6.0F), a1, new ColorRGBA(22, 22, 30, 215), new ColorRGBA(22, 22, 30, 215), a2);
      float cursor = x + 8.0F;
      this.renderLogo(ctx, cursor + 0.5F, y + 6.0F, this.accent().withAlpha(255));
      cursor += 14.0F;
      this.renderCells(ctx, cells, cursor, y + 1.0F, this.accent().withAlpha(255));
      this.lastSize = new float[]{width, height};
   }

   private void renderNeon(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      float width = 14.0F + 14.0F + Math.max(this.cellsWidth(cells, 7.25F), 36.0F);
      float height = 20.0F;
      ColorRGBA accent = this.accent();
      DrawUtil.drawShadow(ctx.getMatrices(), x - 4.0F, y - 4.0F, width + 8.0F, height + 8.0F, 9.0F, BorderRadius.all(10.0F), accent.withAlpha(75));
      DrawUtil.drawShadow(ctx.getMatrices(), x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, 5.0F, BorderRadius.all(8.0F), accent.withAlpha(100));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(8.0F), new ColorRGBA(6, 6, 12, 160));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(8.0F), accent.withAlpha(200));
      float cursor = x + 7.0F;
      this.renderLogo(ctx, cursor + 0.5F, y + 6.0F, accent);
      cursor += 14.0F;
      for (Cell cell : cells) {
         if (!cell.icon().isEmpty()) {
            ctx.drawText(Fonts.ICONS2.getFont(6.0F), cell.icon(), cursor + 1.5F, y + 6.5F, accent.withAlpha(230));
            cursor += Fonts.ICONS2.getWidth(cell.icon(), 6.0F) + 3.0F;
         }
         ctx.drawText(Fonts.REGULAR.getFont(7.25F), cell.label(), cursor, y + 5.5F, accent.brighter(0.25F).withAlpha(255));
         cursor += Fonts.REGULAR.getWidth(cell.label(), 7.25F) + 14.0F;
      }
      this.lastSize = new float[]{width, height};
   }

   private void renderAccentBar(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      float width = 12.0F + 14.0F + Math.max(this.cellsWidth(cells, 7.25F), 34.0F);
      float height = 19.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(10, 10, 14, 180));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 1.0F, y + 1.0F, width - 2.0F, 2.5F, BorderRadius.all(1.25F), this.accent());
      float cursor = x + 6.0F;
      this.renderLogo(ctx, cursor + 0.5F, y + 6.0F, this.accent());
      cursor += 14.0F;
      this.renderCells(ctx, cells, cursor, y + 1.0F, this.accent());
      this.lastSize = new float[]{width, height};
   }

   private void renderProfile(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      String headline = cells.get(0).label();
      List<String> sub = new ArrayList<>();
      for (int i = 1; i < cells.size(); i++) {
         sub.add(cells.get(i).label());
      }
      if (sub.isEmpty()) {
         sub.add("online");
      }
      String subText = String.join(" · ", sub);
      float width = Math.min(Math.max(38.0F + Fonts.SEMIBOLD.getWidth(headline, 7.0F), 38.0F + Fonts.REGULAR.getWidth(subText, 5.5F)) + 8.0F, 190.0F);
      float height = 34.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(6.0F), new ColorRGBA(12, 12, 16, 190));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(6.0F), this.accent().withAlpha(120));
      DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), this.skinTexture(), x + 4.0F, y + 4.0F, 26.0F, BorderRadius.all(5.0F), ColorRGBA.WHITE);
      ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), headline, x + 38.0F, y + 6.0F, TEXT);
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), subText, x + 38.0F, y + 17.5F, DIM);
      this.lastSize = new float[]{width, height};
   }

   private void renderStack(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      float maxW = 0.0F;
      for (Cell cell : cells) {
         maxW = Math.max(maxW, Fonts.ICONS2.getWidth(cell.icon(), 6.0F) + 4.0F + Fonts.REGULAR.getWidth(cell.label(), 6.5F));
      }
      float width = 16.0F + Math.max(maxW, 60.0F);
      float height = 10.0F + cells.size() * 11.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 160));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 1.0F, y + 3.0F, 3.0F, height - 6.0F, BorderRadius.all(1.5F), this.accent());
      float lineY = y + 6.0F;
      for (Cell cell : cells) {
         float cursor = x + 9.0F;
         if (!cell.icon().isEmpty()) {
            ctx.drawText(Fonts.ICONS2.getFont(6.0F), cell.icon(), cursor, lineY + 0.5F, this.accent());
            cursor += Fonts.ICONS2.getWidth(cell.icon(), 6.0F) + 4.0F;
         }
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), cell.label(), cursor, lineY, TEXT);
         lineY += 11.0F;
      }
      this.lastSize = new float[]{width, height};
   }

   private void renderClock(CustomDrawContext ctx, float x, float y) {
      String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
      String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
      Cell fps = this.cellOrNull(3);
      Cell ping = this.cellOrNull(2);
      float timeW = Fonts.COMFORTA_REGULAR.getWidth(time, 26.0F);
      float width = Math.max(timeW + 20.0F, 118.0F);
      float height = 56.0F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(8.0F), new ColorRGBA(10, 10, 14, 200));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(8.0F), this.accent().withAlpha(140));
      ctx.drawText(Fonts.COMFORTA_REGULAR.getFont(26.0F), time, x + 10.0F, y + 4.0F, TEXT);
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), date, x + 12.0F, y + 34.0F, DIM);
      float rightX = x + width - 8.0F;
      float ry = y + 7.0F;
      if (fps != null) {
         ctx.drawText(Fonts.ICONS2.getFont(6.0F), fps.icon(), rightX - 26.0F, ry + 1.0F, this.accent());
         ctx.drawText(Fonts.REGULAR.getFont(6.0F), fps.label(), rightX - Fonts.REGULAR.getWidth(fps.label(), 6.0F), ry, TEXT);
         ry += 14.0F;
      }
      if (ping != null) {
         ctx.drawText(Fonts.ICONS2.getFont(6.0F), ping.icon(), rightX - 26.0F, ry + 1.0F, this.accent());
         ctx.drawText(Fonts.REGULAR.getFont(6.0F), ping.label(), rightX - Fonts.REGULAR.getWidth(ping.label(), 6.0F), ry, TEXT);
      }
      this.lastSize = new float[]{width, height};
   }

   private void renderTerminal(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      List<String> lines = new ArrayList<>();
      for (int i = 0; i < cells.size(); i++) {
         String prompt = i == 0 ? RenamePasterClient.getClientNameLower() + ":~$ " : "$ ";
         lines.add(prompt + cells.get(i).label());
      }
      float maxW = 0.0F;
      for (String line : lines) {
         maxW = Math.max(maxW, Fonts.REGULAR.getWidth(line, 7.0F));
      }
      boolean blink = System.currentTimeMillis() % 1000L < 500L;
      float width = 14.0F + maxW + (blink ? 8.0F : 0.0F);
      float height = 10.0F + lines.size() * 12.0F;
      ColorRGBA green = new ColorRGBA(80, 255, 130, 255);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(4, 8, 4, 200));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(4.0F), green.withAlpha(150));
      float lineY = y + 5.0F;
      for (int i = 0; i < lines.size(); i++) {
         String line = lines.get(i);
         int promptEnd = i == 0 ? (RenamePasterClient.getClientNameLower() + ":~$ ").length() : "$ ".length();
         ctx.drawText(Fonts.REGULAR.getFont(7.0F), line.substring(0, Math.min(promptEnd, line.length())), x + 7.0F, lineY, green);
         ctx.drawText(Fonts.REGULAR.getFont(7.0F), line.substring(Math.min(promptEnd, line.length())), x + 7.0F + Fonts.REGULAR.getWidth(line.substring(0, Math.min(promptEnd, line.length())), 7.0F), lineY, new ColorRGBA(190, 255, 205, 255));
         lineY += 12.0F;
      }
      if (blink) {
         DrawUtil.drawRect(ctx.getMatrices(), x + 7.0F + maxW + 2.0F, y + 5.0F + (lines.size() - 1) * 12.0F, 5.0F, 9.0F, green);
      }
      this.lastSize = new float[]{width, height};
   }

   private void renderBadge(CustomDrawContext ctx, float x, float y) {
      List<Cell> cells = this.cells();
      String name = cells.get(0).label();
      String values = "";
      for (int i = 1; i < Math.min(cells.size(), 4); i++) {
         if (!values.isEmpty()) {
            values += "  ·  ";
         }
         values += cells.get(i).label();
      }
      float textW = Fonts.SEMIBOLD.getWidth(name, 7.25F);
      float valuesW = Fonts.REGULAR.getWidth(values, 6.5F);
      float width = 12.0F + 14.0F + textW + (values.isEmpty() ? 0.0F : 14.0F + valuesW);
      float height = 18.0F;
      ColorRGBA accent = this.accent();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(height / 2.0F), accent.withAlpha(28));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(height / 2.0F), accent.withAlpha(190));
      float cursor = x + 6.0F;
      this.renderLogo(ctx, cursor + 0.5F, y + 5.0F, accent);
      cursor += 14.0F;
      ctx.drawText(Fonts.SEMIBOLD.getFont(7.25F), name, cursor, y + 4.5F, TEXT);
      cursor += textW;
      if (!values.isEmpty()) {
         cursor += 14.0F;
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), values, cursor, y + 5.5F, DIM);
      }
      this.lastSize = new float[]{width, height};
   }

   private Cell cellOrNull(int cellType) {
      List<Cell> cells = this.cells();
      int index = cellType;
      return index >= 0 && index < cells.size() ? cells.get(index) : null;
   }

   private float cellsWidth(List<Cell> cells, float fontSize) {
      float w = 0.0F;
      for (Cell cell : cells) {
         if (!cell.icon().isEmpty()) {
            w += Fonts.ICONS2.getWidth(cell.icon(), 6.0F) + 3.0F;
         }
         w += Fonts.REGULAR.getWidth(cell.label(), fontSize) + 14.0F;
      }
      return w;
   }

   private void renderCells(CustomDrawContext ctx, List<Cell> cells, float cursor, float y, ColorRGBA accent) {
      for (Cell cell : cells) {
         if (!cell.icon().isEmpty()) {
            ctx.drawText(Fonts.ICONS2.getFont(6.0F), cell.icon(), cursor + 1.5F, y + 5.75F, accent.withAlpha(230));
            cursor += Fonts.ICONS2.getWidth(cell.icon(), 6.0F) + 3.0F;
         }
         ctx.drawText(Fonts.REGULAR.getFont(7.25F), cell.label(), cursor, y + 4.25F, TEXT);
         cursor += Fonts.REGULAR.getWidth(cell.label(), 7.25F) + 14.0F;
      }
   }

   private Identifier skinTexture() {
      if (mc.getNetworkHandler() != null && mc.player != null) {
         try {
            String name = NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard();
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
               if (entry.getProfile().getName().equals(name)) {
                  return entry.getSkinTextures().texture();
               }
            }
         } catch (Exception e) {
         }
      }
      return DefaultSkinHelper.getSteve().texture();
   }

   private record Cell(String icon, String label) {
   }
}