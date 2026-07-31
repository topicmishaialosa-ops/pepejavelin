package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import tech.huihui.base.events.impl.render.EventRender2D;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.math.ProjectionUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "AresMinePvPWarpChest",
   category = Category.RENDER,
   description = "Подсвечивает голограммы сундуков с припасами"
)
public final class AresMinePvPWarpChest extends Module {
   public static final AresMinePvPWarpChest INSTANCE = new AresMinePvPWarpChest();
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Текст и рамка", "Текст", "Рамка"});
   private final NumberSetting distance = new NumberSetting("Дистанция", 64.0F, 8.0F, 256.0F, 8.0F);
   private final BooleanSetting showLines = new BooleanSetting("Показывать строки", true);
   private final ColorSetting color = new ColorSetting("Цвет", new ColorRGBA(0, 255, 140, 255));
   private final Map<Entity, String> lastTexts = new HashMap();
   private final Map<Entity, Long[]> countdowns = new HashMap();
   private static final String[] KEYWORDS = new String[]{"сундук", "припас", "провизия", "ящик"};
   private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,3}):(\\d{2})(?::(\\d{2}))?");
   private static final Pattern HOURS_PATTERN = Pattern.compile("(\\d{1,4})\\s*(?:час|[чh])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
   private static final Pattern MINUTES_PATTERN = Pattern.compile("(\\d{1,4})\\s*(?:мин|[мm])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
   private static final Pattern SECONDS_PATTERN = Pattern.compile("(\\d{1,4})\\s*(?:сек|[сc])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
   private static final Pattern UNIT_PATTERN = Pattern.compile("\\d{1,4}\\s*(?:час|[чh]|мин|[мm]|сек|[сc])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

   @EventTarget
   private void onRender(EventRender2D e) {
      if (mc.world == null || mc.player == null) {
         return;
      }

      float tickDelta = e.getTickDelta();
      Set<Entity> processed = new HashSet();
      Iterator var4 = mc.world.getEntities().iterator();

      while(var4.hasNext()) {
         Entity entity = (Entity)var4.next();
         if (processed.contains(entity)) {
            continue;
         }

         if (!this.isSupplyEntity(entity) || !this.withinRange(entity)) {
            continue;
         }

         List<Entity> group = new ArrayList();
         this.collectGroup(entity, group, processed);
         if (!group.isEmpty()) {
            this.renderGroup(group, tickDelta, e);
         }
      }
   }

   private String getText(Entity entity) {
      if (entity == null) {
         return null;
      }
      String text = null;
      if (entity instanceof TextDisplayEntity) {
         Text display = ((TextDisplayEntity)entity).getText();
         text = display != null ? display.getString() : null;
      }
      if (text == null && entity.getCustomName() != null) {
         text = entity.getCustomName().getString();
      }
      if (text == null) {
         return null;
      }
      String stripped = Formatting.strip(text);
      return stripped == null || stripped.isEmpty() ? null : stripped.trim();
   }

   private boolean isSupplyEntity(Entity entity) {
      String text = this.getText(entity);
      if (text == null) {
         return false;
      }
      String lower = text.toLowerCase();
      for(int i = 0; i < KEYWORDS.length; ++i) {
         if (lower.contains(KEYWORDS[i])) {
            return true;
         }
      }
      return false;
   }

   private boolean withinRange(Entity entity) {
      return entity.squaredDistanceTo(mc.player) <= (double)(this.distance.getCurrent() * this.distance.getCurrent());
   }

   private void collectGroup(Entity anchor, List<Entity> group, Set<Entity> processed) {
      group.add(anchor);
      processed.add(anchor);
      Iterator var4 = mc.world.getEntities().iterator();

      while(var4.hasNext()) {
         Entity entity = (Entity)var4.next();
         if (entity == anchor || processed.contains(entity)) {
            continue;
         }

         if (this.getText(entity) == null) {
            continue;
         }

         if (Math.abs(entity.getX() - anchor.getX()) <= 1.5D && Math.abs(entity.getZ() - anchor.getZ()) <= 1.5D && entity.getY() - anchor.getY() >= -5.0D && entity.getY() - anchor.getY() <= 2.0D) {
            group.add(entity);
            processed.add(entity);
         }
      }

   }

   private void renderGroup(List<Entity> group, float tickDelta, EventRender2D e) {
      Entity top = (Entity)group.get(0);

      for(int i = 1; i < group.size(); ++i) {
         if (((Entity)group.get(i)).getY() > top.getY()) {
            top = (Entity)group.get(i);
         }
      }

      if (this.mode.is("Рамка") || this.mode.is("Текст и рамка")) {
         this.renderBox(top, e);
      }

      if (this.mode.is("Текст") || this.mode.is("Текст и рамка")) {
         this.renderText(top, group, tickDelta, e);
      }

   }

   private void renderBox(Entity entity, EventRender2D e) {
      Vector4d vec = ProjectionUtil.getVector4D(entity);
      if (vec != null) {
         float x = (float)vec.x;
         float y = (float)vec.y;
         float width = (float)(vec.z - vec.x);
         float height = (float)(vec.w - vec.y);
         ColorRGBA highlight = this.color.getColor(255.0F);
         DrawUtil.drawRoundedRect(e.getContext().getMatrices(), x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, BorderRadius.all(2.0F), highlight.withAlpha(35));
         DrawUtil.drawRoundedBorder(e.getContext().getMatrices(), x - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 1.5F, BorderRadius.all(2.0F), highlight);
      }

   }

   private void renderText(Entity top, List<Entity> group, float tickDelta, EventRender2D e) {
      double x = MathHelper.lerp((double)tickDelta, top.lastRenderX, top.getX());
      double y = MathHelper.lerp((double)tickDelta, top.lastRenderY, top.getY()) + (double)top.getHeight() + 0.35D;
      double z = MathHelper.lerp((double)tickDelta, top.lastRenderZ, top.getZ());
      Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
      if (pos.z <= 0.0D || pos.z >= 1.0D) {
         return;
      }

      List<Entity> entities = new ArrayList();
      List<String> lines = new ArrayList();

      for(int i = 0; i < group.size(); ++i) {
         Entity entity = (Entity)group.get(i);
         String text = this.getText(entity);
         if (text == null) {
            continue;
         }
         String[] split = text.split("\\n");
         for(int j = 0; j < split.length; ++j) {
            String part = split[j].trim();
            if (!part.isEmpty()) {
               entities.add(entity);
               lines.add(part);
            }
         }
      }

      if (lines.isEmpty()) {
         return;
      }

      ColorRGBA highlight = this.color.getColor(255.0F);
      ColorRGBA shadow = new ColorRGBA(0, 0, 0, 150);
      List<Font> fonts = new ArrayList();
      List<String> displays = new ArrayList();
      float textWidth = 0.0F;
      float lineHeight = 11.0F;
      for(int i = 0; i < lines.size(); ++i) {
         String line = (String)lines.get(i);
         String display = this.formatLine(line, (Entity)entities.get(i));
         Font font = this.isMainLine(line) ? Fonts.MEDIUM.getFont(7.0F) : Fonts.REGULAR.getFont(6.0F);
         displays.add(display);
         fonts.add(font);
         textWidth = Math.max(textWidth, font.width(display));
         lineHeight = Math.max(lineHeight, font.height() + 3.5F);
      }

      float totalHeight = (float)lines.size() * lineHeight + 2.0F;
      float bx = (float)pos.x - textWidth / 2.0F - 4.0F;
      float by = (float)pos.y - 2.0F;
      DrawUtil.drawRoundedRect(e.getContext().getMatrices(), bx, by, textWidth + 8.0F, totalHeight, BorderRadius.all(4.0F), new ColorRGBA(0, 0, 0, 145));
      DrawUtil.drawRoundedBorder(e.getContext().getMatrices(), bx, by, textWidth + 8.0F, totalHeight, 1.0F, BorderRadius.all(4.0F), highlight.withAlpha(170));
      float lineY = by + 1.0F;

      for(int i = 0; i < lines.size(); ++i) {
         String line = (String)lines.get(i);
         String display = (String)displays.get(i);
         Font font = (Font)fonts.get(i);
         float lineWidth = font.width(display);
         float lineX = (float)pos.x - lineWidth / 2.0F;
         ColorRGBA lineColor = this.isMainLine(line) ? highlight : ColorRGBA.WHITE;
         e.getContext().drawText(font, display, lineX + 0.8F, lineY + 0.8F, shadow);
         e.getContext().drawText(font, display, lineX, lineY, lineColor);
         lineY += lineHeight;
      }
   }

   private boolean isMainLine(String line) {
      String lower = line.toLowerCase();
      for(int i = 0; i < KEYWORDS.length; ++i) {
         if (lower.contains(KEYWORDS[i])) {
            return true;
         }
      }
      return false;
   }

   private String formatLine(String line, Entity entity) {
      int totalSeconds = -1;
      String replaceSpan = null;
      int[] unit = this.parseCountdown(line);
      if (unit != null) {
         totalSeconds = unit[0];
         replaceSpan = line.substring(unit[1], unit[2]);
      } else {
         Matcher matcher = TIME_PATTERN.matcher(line);
         if (!matcher.find()) {
            return line;
         }
         if (matcher.group(3) != null) {
            totalSeconds = Integer.parseInt(matcher.group(1)) * 3600 + Integer.parseInt(matcher.group(2)) * 60 + Integer.parseInt(matcher.group(3));
         } else {
            totalSeconds = Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
         }
         replaceSpan = matcher.group(0);
      }
      String last = (String)this.lastTexts.get(entity);
      if (last == null || !last.equals(line)) {
         this.lastTexts.put(entity, line);
         this.countdowns.put(entity, new Long[]{System.currentTimeMillis(), (long)totalSeconds});
      }
      Long[] stored = (Long[])this.countdowns.get(entity);
      long capture = stored != null && stored[0] != null ? stored[0] : System.currentTimeMillis();
      long base = stored != null && stored[1] != null ? stored[1] : (long)totalSeconds;
      long remaining = base - (System.currentTimeMillis() - capture) / 1000L;
      if (remaining < 0L) {
         remaining = 0L;
      }
      return line.replace(replaceSpan, this.formatTime(remaining));
   }

   private int[] parseCountdown(String line) {
      int total = 0;
      boolean found = false;
      Matcher hours = HOURS_PATTERN.matcher(line);
      while (hours.find()) {
         total += Integer.parseInt(hours.group(1)) * 3600;
         found = true;
      }
      Matcher minutes = MINUTES_PATTERN.matcher(line);
      while (minutes.find()) {
         total += Integer.parseInt(minutes.group(1)) * 60;
         found = true;
      }
      Matcher seconds = SECONDS_PATTERN.matcher(line);
      while (seconds.find()) {
         total += Integer.parseInt(seconds.group(1));
         found = true;
      }
      if (!found) {
         return null;
      }
      Matcher broad = UNIT_PATTERN.matcher(line);
      int start = -1;
      int end = -1;
      while (broad.find()) {
         if (start < 0) {
            start = broad.start();
         }
         end = broad.end();
      }
      return new int[]{total, start, end};
   }

   private String formatTime(long seconds) {
      long h = seconds / 3600L;
      long m = seconds % 3600L / 60L;
      long s = seconds % 60L;
      return h > 0L ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
   }
}
