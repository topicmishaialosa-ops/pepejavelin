package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.input.EventMouse;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.FormattedTextProcessor;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.LegacyTextHelper;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.Gradient;
import tech.huihui.utility.render.display.base.GuiUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "Scoreboard",
   category = Category.RENDER,
   description = "Кастомный скорборд с поддержкой цветов"
)
public final class ScoreboardHud extends Module {
   public static final ScoreboardHud INSTANCE = new ScoreboardHud();
   private static final float TITLE_SIZE = 8.8F;
   private static final float LINE_SIZE = 7.8F;
   private static final float PAD_X = 10.0F;
   private static final float PAD_Y = 6.0F;
   private static final float LINE_GAP = 5.0F;
   private static final float VALUE_GAP = 16.0F;

   public final NumberSetting scale = new NumberSetting("Масштаб", 1.0F, 0.5F, 2.0F, 0.05F);
   public final NumberSetting x = new NumberSetting("Позиция X", 4.0F, 0.0F, 1920.0F, 1.0F);
   public final NumberSetting y = new NumberSetting("Позиция Y", 4.0F, 0.0F, 1080.0F, 1.0F);
   public final BooleanSetting showBackground = new BooleanSetting("Фон", true);
   public final BooleanSetting showShadow = new BooleanSetting("Тень", true);
   public final BooleanSetting showAccentLine = new BooleanSetting("Линия акцента", true);

   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;

   private ScoreboardHud() {
   }

   @EventTarget
   @Native
   public void onRender(EventHudRender event) {
      if (mc.world == null || mc.player == null || mc.options.hudHidden) {
         return;
      }
      if (this.dragging) {
         this.updatePosition();
      }
      this.renderScoreboard(event.getContext());
   }

   public void renderScoreboard(CustomDrawContext ctx) {
      ScoreboardObjective objective = this.getObjective();
      if (objective == null) {
         return;
      }

      Layout layout = this.buildLayout(objective);
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      Theme theme = this.theme();
      float scale = this.scale.getCurrent();

      if (this.showShadow.isEnabled()) {
         DrawUtil.drawShadow(ctx.getMatrices(), x - 5.0F, y - 5.0F, layout.width + 10.0F, layout.height + 10.0F, 15.0F, BorderRadius.all(6.0F), ColorRGBA.BLACK.withAlpha(63.75F));
      }

      if (this.showBackground.isEnabled()) {
         ctx.drawRoundedRect(x, y, layout.width, layout.height, BorderRadius.all(6.0F), (new ColorRGBA(0, 0, 0)).withAlpha(150.0F));
         ctx.drawRoundedBorder(x, y, layout.width, layout.height, 1.0F, BorderRadius.all(6.0F), theme.getSecondColor().darker(0.5F).withAlpha(150.0F));
      }

      if (this.showAccentLine.isEnabled()) {
         float lineH = Math.max(1.0F, 1.4F * scale);
         ColorRGBA accent = theme.getColor().withAlpha(170.0F);
         ColorRGBA secondary = theme.getSecondColor().withAlpha(50.0F);
         ctx.drawRoundedRect(x + layout.padX * 0.6F, y + layout.headerHeight - lineH - layout.padY * 0.3F,
            layout.width - layout.padX * 1.2F, lineH, BorderRadius.all(lineH / 2.0F),
            Gradient.of(accent, accent, secondary, secondary));
      }

      float titleX = x + PAD_X;
      for (Segment segment : layout.title.segments) {
         ctx.drawText(Fonts.MEDIUM.getFont(TITLE_SIZE * scale), segment.text, titleX, y + PAD_Y, ColorRGBA.fromInt(segment.color));
         titleX += Fonts.MEDIUM.getFont(TITLE_SIZE * scale).width(segment.text);
      }

      float lineY = y + layout.headerHeight;
      Font lineFont = Fonts.REGULAR.getFont(LINE_SIZE * scale);
      for (EntryLine line : layout.lines) {
         float rowY = lineY + (layout.lineHeight - lineFont.height()) / 2.0F;
         float valueWidth = line.value.width;

         float nameX = x + PAD_X;
         for (Segment segment : line.name.segments) {
            ctx.drawText(lineFont, segment.text, nameX, rowY, ColorRGBA.fromInt(segment.color));
            nameX += lineFont.width(segment.text);
         }

         float valueX = x + layout.width - PAD_X - valueWidth;
         for (Segment segment : line.value.segments) {
            ctx.drawText(lineFont, segment.text, valueX, rowY, ColorRGBA.fromInt(segment.color));
            valueX += lineFont.width(segment.text);
         }

         lineY += layout.lineHeight;
      }
   }

   private Layout buildLayout(ScoreboardObjective objective) {
      int defaultTextColor = theme().getGrayLight().getRGB() & 0xFFFFFF;
      float scale = this.scale.getCurrent();
      Font titleFont = Fonts.MEDIUM.getFont(TITLE_SIZE * scale);
      Font lineFont = Fonts.REGULAR.getFont(LINE_SIZE * scale);

      Text titleSource = objective.getDisplayName();
      Segments title = this.buildSegments(titleSource, titleFont, defaultTextColor);
      float maxWidth = title.width;

      List<EntryLine> lines = objective.getScoreboard().getScoreboardEntries(objective).stream()
         .filter(entry -> entry != null && !entry.hidden())
         .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String::compareToIgnoreCase))
         .limit(25)
         .map(entry -> {
            Segments name = this.buildSegments(this.resolveNameText(objective.getScoreboard(), entry), lineFont, defaultTextColor);
            Segments value = this.buildSegments(this.resolveValueText(objective, entry), lineFont, defaultTextColor);
            return new EntryLine(name, value);
         })
         .toList();

      for (EntryLine line : lines) {
         float rowWidth = line.name.width + line.value.width + VALUE_GAP;
         maxWidth = Math.max(maxWidth, rowWidth);
      }

      float headerHeight = titleFont.height() + PAD_Y * 2.0F;
      float lineHeight = lineFont.height() + LINE_GAP;
      float totalHeight = headerHeight + lines.size() * lineHeight + PAD_Y;
      float totalWidth = maxWidth + PAD_X * 2.0F + 12.0F;

      return new Layout(title, lines, headerHeight, lineHeight, totalWidth, totalHeight, scale, PAD_X, PAD_Y);
   }

   private Segments buildSegments(Text text, Font font, int defaultColor) {
      String raw = LegacyTextHelper.extractRaw(text);
      boolean hasLegacy = LegacyTextHelper.containsLegacyCodes(raw);
      List<Segment> segments = hasLegacy
         ? LegacyTextHelper.parseSegments(raw, defaultColor).stream()
            .map(segment -> new Segment(segment.text(), segment.color())).toList()
         : FormattedTextProcessor.processText(text, defaultColor).stream()
            .map(segment -> new Segment(segment.text(), segment.color())).toList();

      float width = 0.0F;
      List<Segment> sanitized = new ArrayList<>();
      for (Segment segment : segments) {
         String cleaned = segment.text;
         if (!cleaned.isEmpty()) {
            sanitized.add(new Segment(cleaned, segment.color));
            width += font.width(cleaned);
         }
      }

      return new Segments(sanitized, width);
   }

   private ScoreboardObjective getObjective() {
      if (mc.world == null) {
         return null;
      }
      return mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
   }

   private Text resolveNameText(net.minecraft.scoreboard.Scoreboard scoreboard, ScoreboardEntry entry) {
      Text display = entry.display();
      if (display != null) {
         return display;
      }

      String owner = entry.owner();
      if (owner == null) {
         return Text.empty();
      }

      Team team = scoreboard.getScoreHolderTeam(owner);
      Text base = Text.literal(owner);
      return team != null ? Team.decorateName(team, base) : base;
   }

   private Text resolveValueText(ScoreboardObjective objective, ScoreboardEntry entry) {
      Text formatted = entry.formatted(objective.getNumberFormatOr(StyledNumberFormat.EMPTY));
      if (formatted != null) {
         return formatted;
      }

      return Text.literal(Integer.toString(entry.value()));
   }

   private Theme theme() {
      return HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
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
      ScoreboardObjective objective = this.getObjective();
      if (objective == null) {
         return new float[]{60.0F, 20.0F};
      }
      Layout layout = this.buildLayout(objective);
      return new float[]{layout.width, layout.height};
   }

   public void updatePosition() {
      if (!this.dragging) {
         return;
      }
      Vector2f mousePos = GuiUtil.getMouse(2.0D);
      float[] size = this.currentSize();
      float scaledWidth = (float)mc.getWindow().getScaledWidth();
      float scaledHeight = (float)mc.getWindow().getScaledHeight();
      this.x.setCurrent(MathHelper.clamp(mousePos.getX() - this.dragOffsetX, 0.0F, Math.max(scaledWidth - size[0], 0.0F)));
      this.y.setCurrent(MathHelper.clamp(mousePos.getY() - this.dragOffsetY, 0.0F, Math.max(scaledHeight - size[1], 0.0F)));
   }

   private record Segment(String text, int color) {
   }

   private record Segments(List<Segment> segments, float width) {
   }

   private record EntryLine(Segments name, Segments value) {
   }

   private record Layout(Segments title, List<EntryLine> lines, float headerHeight, float lineHeight, float width, float height, float scale, float padX, float padY) {
   }
}
