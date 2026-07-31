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
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import tech.huihui.base.events.impl.render.EventRender2D;
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
   name = "SupplyESP",
   category = Category.RENDER,
   description = "Подсвечивает голограммы сундуков с припасами"
)
public final class SupplyESP extends Module {
   public static final SupplyESP INSTANCE = new SupplyESP();
   private final ModeSetting mode = new ModeSetting("Режим", new String[]{"Текст и рамка", "Текст", "Рамка"});
   private final NumberSetting distance = new NumberSetting("Дистанция", 64.0F, 8.0F, 256.0F, 8.0F);
   private final BooleanSetting showLines = new BooleanSetting("Показывать строки", true);
   private final ColorSetting color = new ColorSetting("Цвет", new ColorRGBA(0, 255, 140, 255));
   private final Map<Entity, String> lastTexts = new HashMap();
   private final Map<Entity, Long[]> countdowns = new HashMap();
   private static final String MAIN_PHRASE = "сундук с припасами";
   private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?");

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
         if (!(entity instanceof ArmorStandEntity) || processed.contains(entity)) {
            continue;
         }

         ArmorStandEntity stand = (ArmorStandEntity)entity;
         if (stand.getCustomName() == null || !isSupplyStand(stand) || !withinRange(stand)) {
            continue;
         }

         List<ArmorStandEntity> group = new ArrayList();
         collectGroup(stand, group, processed);
         if (!group.isEmpty()) {
            renderGroup(group, tickDelta, e);
         }
      }
   }

   private boolean isSupplyStand(ArmorStandEntity stand) {
      String text = Formatting.strip(stand.getCustomName().getString());
      return text != null && text.toLowerCase().contains(MAIN_PHRASE);
   }

   private boolean withinRange(ArmorStandEntity stand) {
      return stand.squaredDistanceTo(mc.player) <= (double)(this.distance.getCurrent() * this.distance.getCurrent());
   }

   private void collectGroup(ArmorStandEntity anchor, List<ArmorStandEntity> group, Set<Entity> processed) {
      group.add(anchor);
      processed.add(anchor);
      Iterator var4 = mc.world.getEntities().iterator();

      while(var4.hasNext()) {
         Entity entity = (Entity)var4.next();
         if (!(entity instanceof ArmorStandEntity) || entity == anchor || processed.contains(entity)) {
            continue;
         }

         ArmorStandEntity stand = (ArmorStandEntity)entity;
         if (stand.getCustomName() == null) {
            continue;
         }

         if (Math.abs(stand.getX() - anchor.getX()) <= 1.5D && Math.abs(stand.getZ() - anchor.getZ()) <= 1.5D && stand.getY() - anchor.getY() >= -4.0D && stand.getY() - anchor.getY() <= 1.0D) {
            group.add(stand);
            processed.add(stand);
         }
      }

   }

   private void renderGroup(List<ArmorStandEntity> group, float tickDelta, EventRender2D e) {
      ArmorStandEntity top = group.get(0);

      for(int i = 1; i < group.size(); ++i) {
         if (((ArmorStandEntity)group.get(i)).getY() > top.getY()) {
            top = (ArmorStandEntity)group.get(i);
         }
      }

      if (this.mode.is("Рамка") || this.mode.is("Текст и рамка")) {
         this.renderBox((ArmorStandEntity)group.get(0), e);
      }

      if (this.mode.is("Текст") || this.mode.is("Текст и рамка")) {
         this.renderText(top, group, tickDelta, e);
      }

   }

   private void renderBox(ArmorStandEntity stand, EventRender2D e) {
      if (ProjectionUtil.canSee(stand.getBoundingBox().getCenter())) {
         Vector4d vec = ProjectionUtil.getVector4D(stand);
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

   }

   private void renderText(ArmorStandEntity top, List<ArmorStandEntity> group, float tickDelta, EventRender2D e) {
      if (ProjectionUtil.canSee(top.getBoundingBox().getCenter())) {
         double x = MathHelper.lerp((double)tickDelta, top.lastRenderX, top.getX());
         double y = MathHelper.lerp((double)tickDelta, top.lastRenderY, top.getY()) + (double)top.getHeight() + 0.35D;
         double z = MathHelper.lerp((double)tickDelta, top.lastRenderZ, top.getZ());
         Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
         if (!(pos.z <= 0.0D) && !(pos.z >= 1.0D)) {
            List<ArmorStandEntity> stands = new ArrayList();
            List<String> lines = new ArrayList();

            for(int i = 0; i < group.size(); ++i) {
               ArmorStandEntity stand = (ArmorStandEntity)group.get(i);
               if (stand.getCustomName() != null) {
                  String text = Formatting.strip(stand.getCustomName().getString());
                  if (text != null && !text.isEmpty()) {
                     stands.add(stand);
                     lines.add(text);
                  }
               }
            }

            if (!lines.isEmpty()) {
               ColorRGBA highlight = this.color.getColor(255.0F);
               float textWidth = 0.0F;

               for(int i = 0; i < lines.size(); ++i) {
                  String display = this.formatLine((String)lines.get(i), (ArmorStandEntity)stands.get(i));
                  textWidth = Math.max(textWidth, Fonts.REGULAR.getWidth(display, 6.0F));
               }

               float lineHeight = 10.0F;
               float totalHeight = (float)lines.size() * lineHeight + 2.0F;
               float bx = (float)pos.x - textWidth / 2.0F - 3.0F;
               float by = (float)pos.y - 2.0F;
               DrawUtil.drawRoundedRect(e.getContext().getMatrices(), bx, by, textWidth + 6.0F, totalHeight, BorderRadius.all(3.0F), new ColorRGBA(0, 0, 0, 123));
               DrawUtil.drawRoundedBorder(e.getContext().getMatrices(), bx, by, textWidth + 6.0F, totalHeight, 1.0F, BorderRadius.all(3.0F), highlight.withAlpha(160));
               float lineY = by + 1.0F;

               for(int i = 0; i < lines.size(); ++i) {
                  String line = (String)lines.get(i);
                  String display = this.formatLine(line, (ArmorStandEntity)stands.get(i));
                  float lineWidth = Fonts.REGULAR.getWidth(display, 6.0F);
                  ColorRGBA lineColor = line.toLowerCase().contains(MAIN_PHRASE) ? highlight : ColorRGBA.WHITE;
                  e.getContext().drawText(Fonts.REGULAR.getFont(6.0F), display, (float)pos.x - lineWidth / 2.0F, lineY, lineColor);
                  lineY += lineHeight;
               }
            }
         }
      }

   }

   private String formatLine(String line, ArmorStandEntity stand) {
      if (!this.showLines.isEnabled()) {
         return line;
      } else {
         Matcher matcher = TIME_PATTERN.matcher(line);
         if (matcher.find()) {
            int hours = Integer.parseInt(matcher.group(1));
            int minutes = Integer.parseInt(matcher.group(2));
            int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            int totalSeconds = hours * 3600 + minutes * 60 + seconds;
            String last = (String)this.lastTexts.get(stand);
            if (last == null || !last.equals(line)) {
               this.lastTexts.put(stand, line);
               this.countdowns.put(stand, new Long[]{System.currentTimeMillis(), (long)totalSeconds});
            }

            Long[] stored = (Long[])this.countdowns.get(stand);
            long capture = stored != null && stored[0] != null ? stored[0] : System.currentTimeMillis();
            long base = stored != null && stored[1] != null ? stored[1] : (long)totalSeconds;
            long remaining = base - (System.currentTimeMillis() - capture) / 1000L;
            if (remaining < 0L) {
               remaining = 0L;
            }

            String formatted = this.formatTime(remaining);
            return line.replace(matcher.group(0), formatted);
         } else {
            return line;
         }
      }
   }

   private String formatTime(long seconds) {
      long h = seconds / 3600L;
      long m = seconds % 3600L / 60L;
      long s = seconds % 60L;
      return h > 0L ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
   }
}
