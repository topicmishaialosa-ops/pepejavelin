package tech.huihui.client.modules.impl.render;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.Vector2f;
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
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.GuiUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(name = "Watermark", category = Category.RENDER, description = "Настраиваемая ватермарка с 20 пресетами")
public final class Watermark extends Module {

   private static final String[] STYLES = new String[]{
      "Классика", "Минимал", "Полоса", "Блок", "Неон", "Чистый", "Двойная рамка", "Скруглённый",
      "Градиент", "Акцент слева", "Иконка справа", "Мини-панель", "Стрелка", "Профиль", "Тень",
      "Панель", "Время", "Сервер", "Компакт", "Молния"
   };
   private static final int[] BOX_STYLE = new int[]{2, 0, 1, 1, 2, 0, 6, 3, 4, 5, 2, 1, 5, 3, 7, 4, 1, 3, 1, 2};
   private static final int[] ICON_POS = new int[]{1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 2, 1, 1, 1, 0, 1, 0, 1};
   private static final int[] ACCENT = new int[]{-1, -1, -1, -1, 0xFF54B23C, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0xFFFF9F0A};

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

   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;

   private Watermark() {
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
      List<String[]> cells = this.buildCells();
      int iconPos = ICON_POS[this.currentStyleIndex()];
      float contentW = 0.0F;
      for (String[] cell : cells) {
         contentW += Fonts.ICONS2.getWidth(cell[0], 6.0F) + 2.0F + Fonts.REGULAR.getWidth(cell[1], 7.25F) + 14.0F;
      }
      float logoW = iconPos == 0 ? 0.0F : 11.0F;
      return new float[]{6.0F * 2.0F + logoW + Math.max(contentW, 34.0F), 16.0F};
   }

   private List<String[]> buildCells() {
      List<String[]> cells = new ArrayList<>();
      PlayerListEntry list = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
      if (this.showName.isEnabled()) {
         cells.add(new String[]{"", "Huihui Client"});
      }
      if (this.showNick.isEnabled()) {
         cells.add(new String[]{"\uf007", NameProtect.INSTANCE.isEnabled() ? NameProtect.getCustomName() : mc.player.getNameForScoreboard()});
      }
      if (this.showPing.isEnabled()) {
         cells.add(new String[]{"\uf1eb", list != null ? list.getLatency() + "ms" : "0ms"});
      }
      if (this.showFps.isEnabled()) {
         cells.add(new String[]{"\uf624", mc.getCurrentFps() + "fps"});
      }
      if (this.showTps.isEnabled()) {
         cells.add(new String[]{"\uf68f", String.format("%.1f", HuihuiClient.getInstance().getServerHandler().getTPS()).replace(",", ".") + "tps"});
      }
      if (this.showServer.isEnabled()) {
         cells.add(new String[]{"\uf0ac", mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null ? mc.getCurrentServerEntry().address : "Неизвестно"});
      }
      if (this.showTime.isEnabled()) {
         cells.add(new String[]{"\uf017", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))});
      }
      if (cells.isEmpty()) {
         cells.add(new String[]{"", "Huihui Client"});
      }
      return cells;
   }

   private int currentStyleIndex() {
      for (int i = 0; i < STYLES.length; i++) {
         if (this.style.is(STYLES[i])) {
            return i;
         }
      }
      return 0;
   }

   private void renderWatermark(CustomDrawContext ctx, float x, float y) {
      List<String[]> cells = this.buildCells();
      int index = this.currentStyleIndex();
      int iconPos = ICON_POS[index];
      int boxStyle = BOX_STYLE[index];
      ColorRGBA accent = ACCENT[index] != -1 ? new ColorRGBA((ACCENT[index] >> 16) & 255, (ACCENT[index] >> 8) & 255, ACCENT[index] & 255, 255) : this.accent.getColor();
      float pad = 6.0F;
      float height = 16.0F;
      float logoW = iconPos == 0 ? 0.0F : 11.0F;
      float contentW = 0.0F;
      for (String[] cell : cells) {
         contentW += Fonts.ICONS2.getWidth(cell[0], 6.0F) + 2.0F + Fonts.REGULAR.getWidth(cell[1], 7.25F) + 14.0F;
      }
      float width = pad * 2.0F + logoW + Math.max(contentW, 34.0F);
      this.drawBox(ctx, x, y, width, height, boxStyle, accent);
      float cursor = x + pad;
      if (iconPos == 1) {
         ctx.drawText(Fonts.ICONS.getFont(6.5F), "B", cursor + 0.5F, y + 4.75F, accent);
         cursor += 11.0F;
      }
      for (String[] cell : cells) {
         if (!cell[0].isEmpty()) {
            ctx.drawText(Fonts.ICONS2.getFont(6.0F), cell[0], cursor + 1.5F, y + 5.75F, accent);
            cursor += Fonts.ICONS2.getWidth(cell[0], 6.0F) + 3.0F;
         }
         ctx.drawText(Fonts.REGULAR.getFont(7.25F), cell[1], cursor, y + 4.25F, new ColorRGBA(255, 255, 255, 255));
         cursor += Fonts.REGULAR.getWidth(cell[1], 7.25F) + 14.0F;
      }
      if (iconPos == 2) {
         ctx.drawText(Fonts.ICONS.getFont(6.5F), "B", cursor + 0.5F, y + 4.75F, accent);
      }
   }

   private void drawBox(CustomDrawContext ctx, float x, float y, float width, float height, int boxStyle, ColorRGBA accent) {
      switch (boxStyle) {
         case 1:
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 140));
            break;
         case 2:
            DrawUtil.drawBlur(ctx.getMatrices(), x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 5.0F, BorderRadius.all(4.0F), accent.withAlpha(90));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 140));
            break;
         case 3:
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 140));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(4.0F), accent);
            break;
         case 4:
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(20, 20, 28, 200), accent.withAlpha(80), accent.withAlpha(80), new ColorRGBA(20, 20, 28, 200));
            break;
         case 5:
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 140));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 1.0F, y + 2.0F, 3.0F, height - 4.0F, BorderRadius.all(1.5F), accent);
            break;
         case 6:
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 140));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 1.0F, BorderRadius.all(5.0F), accent.withAlpha(120));
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(4.0F), accent);
            break;
         case 7:
            DrawUtil.drawShadow(ctx.getMatrices(), x - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, 6.0F, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 160));
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 160));
            break;
         default:
            break;
      }
      if (this.dragging) {
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 1.0F, BorderRadius.all(5.0F), accent);
      }
   }
}
