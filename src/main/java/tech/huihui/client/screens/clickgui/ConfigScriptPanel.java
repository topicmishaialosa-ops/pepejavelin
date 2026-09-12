package tech.huihui.client.screens.clickgui;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.minecraft.util.math.Vec2f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.font.MsdfFont;
import tech.huihui.client.modules.impl.render.EditClickGUI;
import tech.huihui.client.script.ScriptManager;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@Getter
public class ConfigScriptPanel {
   public interface Host {
      int width();

      int height();

      void scissor(CustomDrawContext draw, float x, float y, float w, float h);

      ColorRGBA accent();

      ColorRGBA bg();

      ColorRGBA border();

      ColorRGBA textMain();

      ColorRGBA textDim();

      MsdfFont headerFont();

      MsdfFont textFont();
   }

   private static final float HEADER = 24.0F;
   private static final float TAB_H = 20.0F;
   private static final float ROW_H = 18.0F;
   private static final float VISIBLE = 8.0F;
   private static final float RADIUS = 5.0F;
   private static final float PADDING = 5.0F;

   private final Host host;
   private final Animation expandAnim = new Animation(200L, Easing.CIRC_OUT);
   private Mode mode = Mode.CONFIGS;
   private float scroll;
   private boolean open;
   private String loadedConfig = "current_config";
   private List<String> cachedConfigs = new ArrayList<>();
   private List<ScriptManager.ScriptFile> cachedScripts = new ArrayList<>();

   private enum Mode { CONFIGS, SCRIPTS }

   public ConfigScriptPanel(Host host) {
      this.host = host;
   }

   private float width() {
      return EditClickGUI.INSTANCE.getPanelWidth().getCurrent();
   }

   private float x() {
      return EditClickGUI.INSTANCE.getPanelPosition().is("Слева") ? 6.0F : (float) this.host.width() - width() - 6.0F;
   }

   private float totalH() {
      return HEADER + TAB_H + ROW_H * VISIBLE + PADDING * 2.0F;
   }

   private float contentY(float baseY) {
      return baseY + HEADER;
   }

   private ColorRGBA accent(float alpha) {
      return this.host.accent().withAlpha((int) (this.host.accent().getAlpha() * alpha));
   }

   private ColorRGBA text(ColorRGBA base, float alpha) {
      return base.withAlpha((int) (base.getAlpha() * alpha));
   }

   public void refreshCache() {
      cachedConfigs = new ArrayList<>(HuihuiClient.getInstance().getConfigManager().configNames());
      cachedScripts = ScriptManager.listScriptsOnDisk();
   }

   public void render(CustomDrawContext draw, float alpha, float mx, float my) {
      expandAnim.update(this.open);
      float expand = expandAnim.getValue();

      refreshCache();
      float h = HEADER + (totalH() - HEADER) * expand;
      float x = x();
      float y = (float) this.host.height() - 6.0F - h;
      float w = width();

      ColorRGBA bg = this.host.bg();
      DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(RADIUS), bg.withAlpha((int) (bg.getAlpha() * alpha)));
      ColorRGBA border = this.host.border();
      DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(RADIUS), border.withAlpha((int) (84.0F * alpha)));

      draw.drawText(Fonts.ICONS.getFont(8.0F), "S", x + 8.0F, y + 8.0F, accent(alpha));
      ColorRGBA title = text(this.host.textMain(), alpha);
      draw.drawText(this.host.headerFont().getFont(8.0F), "Конфиги / Скрипты", x + 21.0F, y + 8.0F, title);
      float cx = x + w - 14.0F;
      float cy = y + 12.0F;
      ColorRGBA chevronCol = text(this.host.textDim(), alpha);
      float off = open ? -2.0F : 1.0F;
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cx - 3.5F, cy + off + 1.0F), new Vec2f(cx, cy - off), chevronCol);
      DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cx, cy - off), new Vec2f(cx + 3.5F, cy + off + 1.0F), chevronCol);

      if (expand < 0.5F) return;

      float contentX = x + PADDING;
      float contentW = w - PADDING * 2.0F;
      float cY = contentY(y);

      float tabW = (contentW - 3.0F) / 2.0F;
      renderTab(draw, alpha, contentX, cY, tabW, TAB_H, "Конфиги", Mode.CONFIGS, mx, my);
      renderTab(draw, alpha, contentX + tabW + 3.0F, cY, tabW, TAB_H, "Скрипты", Mode.SCRIPTS, mx, my);

      float listY = cY + TAB_H + 3.0F;
      float listH = ROW_H * VISIBLE;
      float count = (mode == Mode.CONFIGS) ? configCount() : scriptCount();
      float contentH = count * ROW_H;
      float maxScroll = Math.max(0.0F, contentH - listH);
      if (scroll > maxScroll) scroll = maxScroll;
      if (scroll < 0.0F) scroll = 0.0F;

      this.host.scissor(draw, contentX, listY, contentW, listH);
      if (mode == Mode.CONFIGS) {
         renderConfigs(draw, alpha, contentX, listY, contentW, mx, my);
      } else {
         renderScripts(draw, alpha, contentX, listY, contentW, mx, my);
      }
      draw.disableScissor();

      if (maxScroll > 0.0F) {
         float sbH = Math.max(10.0F, listH * listH / contentH);
         float sbY = listY + (-scroll / maxScroll) * (listH - sbH);
         DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - PADDING - 2.5F, sbY, 3.0F, sbH, BorderRadius.all(1.5F), (new ColorRGBA(153, 153, 153)).withAlpha((int) (100.0F * expand * alpha)));
      }
   }

   private void renderTab(CustomDrawContext draw, float alpha, float x, float y, float w, float h, String text, Mode m, float mx, float my) {
      boolean active = mode == m;
      boolean hovered = MathUtil.isHovered(mx, my, x, y, w, h);
      if (active) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(3.0F), this.host.accent().withAlpha((int) (55.0F * alpha)));
      } else if (hovered) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(3.0F), (new ColorRGBA(255, 255, 255)).withAlpha((int) (5.0F * alpha)));
      }
      ColorRGBA col = active ? text(this.host.textMain(), alpha) : text(this.host.textDim(), alpha);
      draw.drawText(this.host.textFont().getFont(6.0F), text, x + 6.0F, y + 7.0F, col);
   }

   private int configCount() {
      return cachedConfigs.size();
   }

   private int scriptCount() {
      return cachedScripts.size();
   }

   private void renderConfigs(CustomDrawContext draw, float alpha, float x, float y, float w, float mx, float my) {
      List<String> names = cachedConfigs;
      float ry = y;
      if (names.isEmpty()) {
         draw.drawText(this.host.textFont().getFont(6.0F), "Нет конфигов", x + 7.0F, y + 6.5F, text(this.host.textDim(), alpha));
         return;
      }
      for (String name : names) {
         boolean loaded = name.equals(loadedConfig);
         boolean hovered = MathUtil.isHovered(mx, my, x, ry, w, ROW_H);
         drawRowBg(draw, alpha, x, ry, w, loaded, hovered);
         if (loaded) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, ry + 4.0F, 2.0F, ROW_H - 8.0F, BorderRadius.all(1.0F), accent(alpha));
         }
         ColorRGBA nameCol = loaded ? text(this.host.textMain(), alpha) : text(this.host.textDim(), alpha);
         draw.drawText(this.host.textFont().getFont(6.0F), name, x + 8.0F, ry + 6.0F, nameCol);
         ry += ROW_H;
      }
   }

   private void renderScripts(CustomDrawContext draw, float alpha, float x, float y, float w, float mx, float my) {
      List<ScriptManager.ScriptFile> files = cachedScripts;
      float ry = y;
      if (files.isEmpty()) {
         draw.drawText(this.host.textFont().getFont(6.0F), "Скриптов нет", x + 7.0F, y + 6.5F, text(this.host.textDim(), alpha));
         return;
      }
      for (ScriptManager.ScriptFile sf : files) {
         String baseName = sf.name.contains("/") ? sf.name.substring(sf.name.lastIndexOf('/') + 1) : sf.name;
         boolean loaded = ScriptManager.isLoaded(baseName);
         boolean hovered = MathUtil.isHovered(mx, my, x, ry, w, ROW_H);
         drawRowBg(draw, alpha, x, ry, w, loaded, hovered);
         if (loaded) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, ry + 4.0F, 2.0F, ROW_H - 8.0F, BorderRadius.all(1.0F), accent(alpha));
         }
         ColorRGBA nameCol = loaded ? text(this.host.textMain(), alpha) : text(this.host.textDim(), alpha);
         draw.drawText(this.host.textFont().getFont(6.0F), sf.name, x + 8.0F, ry + 6.0F, nameCol);
         ColorRGBA langCol = loaded ? (new ColorRGBA(111, 111, 111)).withAlpha((int) (255.0F * alpha)) : (new ColorRGBA(80, 80, 80)).withAlpha((int) (255.0F * alpha));
         draw.drawText(this.host.textFont().getFont(5.0F), sf.lang, x + w - 6.0F - this.host.textFont().getFont(5.0F).width(sf.lang), ry + 7.0F, langCol);
         ry += ROW_H;
      }
   }

   private void drawRowBg(CustomDrawContext draw, float alpha, float x, float ry, float w, boolean loaded, boolean hovered) {
      if (loaded) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, ry, w, ROW_H - 1.0F, BorderRadius.all(3.0F), this.host.accent().withAlpha((int) (35.0F * alpha)));
      } else if (hovered) {
         DrawUtil.drawRoundedRect(draw.getMatrices(), x, ry, w, ROW_H - 1.0F, BorderRadius.all(3.0F), (new ColorRGBA(255, 255, 255)).withAlpha((int) (5.0F * alpha)));
      }
   }

   public boolean mouseClicked(float mx, float my, int button) {
      float x = x();
      float baseY = (float) this.host.height() - 6.0F - (HEADER + (totalH() - HEADER) * expandAnim.getValue());
      float w = width();
      if (button == 0) {
         if (MathUtil.isHovered(mx, my, x, baseY, w, HEADER)) {
            this.open = !this.open;
            this.scroll = 0.0F;
            return true;
         }
      }
      if (!open) return false;
      float y = (float) this.host.height() - 6.0F - totalH();
      float contentX = x + PADDING;
      float contentW = w - PADDING * 2.0F;
      float cY = contentY(y);
      float tabW = (contentW - 3.0F) / 2.0F;
      if (MathUtil.isHovered(mx, my, contentX, cY, tabW, TAB_H)) {
         mode = Mode.CONFIGS;
         scroll = 0.0F;
         return true;
      }
      if (MathUtil.isHovered(mx, my, contentX + tabW + 3.0F, cY, tabW, TAB_H)) {
         mode = Mode.SCRIPTS;
         scroll = 0.0F;
         return true;
      }
      float listY = cY + TAB_H + 3.0F;
      float listH = ROW_H * VISIBLE;
      if (!MathUtil.isHovered(mx, my, contentX, listY, contentW, listH)) return false;
      int index = (int) ((my - (listY - scroll)) / ROW_H);
      if (mode == Mode.CONFIGS) {
         if (index >= 0 && index < cachedConfigs.size()) {
            String name = cachedConfigs.get(index);
            if (HuihuiClient.getInstance().getConfigManager().loadConfig(name)) {
               loadedConfig = name;
            }
         }
      } else {
         if (index >= 0 && index < cachedScripts.size()) {
            ScriptManager.ScriptFile sf = cachedScripts.get(index);
            String baseName = sf.name.contains("/") ? sf.name.substring(sf.name.lastIndexOf('/') + 1) : sf.name;
            if (ScriptManager.isLoaded(baseName)) {
               ScriptManager.unload(baseName);
            } else {
               ScriptManager.load(baseName, sf.lang);
            }
         }
      }
      refreshCache();
      return true;
   }

   public boolean mouseScrolled(float mouseX, float mouseY, double amount) {
      if (!open) return false;
      float x = x();
      float y = (float) this.host.height() - 6.0F - totalH();
      float contentX = x + PADDING;
      float contentW = width() - PADDING * 2.0F;
      float cY = contentY(y);
      float listY = cY + TAB_H + 3.0F;
      float listH = ROW_H * VISIBLE;
      if (!MathUtil.isHovered(mouseX, mouseY, contentX, listY, contentW, listH)) return false;
      scroll -= (float) amount * 15.0F;
      return true;
   }
}