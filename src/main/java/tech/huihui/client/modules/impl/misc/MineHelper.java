package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import tech.huihui.base.events.impl.render.EventRender2D;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "MineHelper",
   category = Category.MISC,
   description = "Таджек + рынок"
)
public final class MineHelper extends Module {
   public static final MineHelper INSTANCE = new MineHelper();
   private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

   private final MultiBooleanSetting oresSettings = new MultiBooleanSetting(
      "Руды",
      MultiBooleanSetting.Value.of("Уголь", true),
      MultiBooleanSetting.Value.of("Железо", true),
      MultiBooleanSetting.Value.of("Редстоун", true),
      MultiBooleanSetting.Value.of("Золото", true),
      MultiBooleanSetting.Value.of("Изумруды", true),
      MultiBooleanSetting.Value.of("Алмазы", true),
      MultiBooleanSetting.Value.of("Незерит", true),
      MultiBooleanSetting.Value.of("Лазурит", true),
      MultiBooleanSetting.Value.of("Адская золотая руда", true)
   );
   private final BooleanSetting showMine = new BooleanSetting("Показывать шахту", true);
   private final BooleanSetting showOres = new BooleanSetting("Показывать руды", true);
   private final MultiBooleanSetting renderSettings;
   private final ModeSetting highlightType = new ModeSetting(
      "Тип выделения",
      () -> this.showOres.isEnabled(),
      "По боксу",
      "По границам"
   );
   private final NumberSetting borderScale = new NumberSetting(
      "Масштаб границ",
      0.05F,
      0.01F,
      0.1F,
      0.005F,
      () -> this.showOres.isEnabled()
   );
   private final NumberSetting searchRadius = new NumberSetting(
      "Радиус поиска",
      24.0F,
      8.0F,
      48.0F,
      1.0F,
      () -> this.showOres.isEnabled()
   );
   private final NumberSetting boxesPerFrame = new NumberSetting(
      "Лимит боксов/кадр",
      400.0F,
      50.0F,
      1500.0F,
      10.0F,
      () -> this.showOres.isEnabled()
   );
   private final NumberSetting cacheUpdateFrequency = new NumberSetting(
      "Частота обновления кэша (мс)",
      1000.0F,
      500.0F,
      5000.0F,
      100.0F,
      () -> this.showOres.isEnabled()
   );

   private final Map<BlockPos, OreInfo> oreCache = new HashMap();
   private BlockPos lastPlayerPos;
   private long lastUpdateTime;

   public MineHelper() {
      this.renderSettings = new MultiBooleanSetting(
         "Рендеринг",
         MultiBooleanSetting.Value.of("Заливка", true),
         MultiBooleanSetting.Value.of("Обводка", true),
         MultiBooleanSetting.Value.of("Штрихи", false)
      );
      this.renderSettings.setVisible(() -> this.showOres.isEnabled());
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.oreCache.clear();
      this.lastPlayerPos = null;
      this.lastUpdateTime = 0L;
   }

   @EventTarget
   private void onRender2D(EventRender2D event) {
      if (!this.showMine.isEnabled() || mc.world == null || mc.player == null) {
         return;
      }

      List<ArmorStandEntity> armorStands = this.getArmorStands();
      String mineName = this.findMineName(armorStands);
      String timerValue = this.findTimerValue(armorStands);

      int screenWidth = mc.getWindow().getScaledWidth();
      int screenHeight = mc.getWindow().getScaledHeight();

      String title = "Авто Шахта";
      String info = mineName.isEmpty() && timerValue.isEmpty()
         ? "—"
         : mineName + (timerValue.isEmpty() ? "" : " " + timerValue);

      Font titleFont = Fonts.MEDIUM.getFont(7.0F);
      Font infoFont = Fonts.REGULAR.getFont(6.5F);
      float titleWidth = titleFont.width(title);
      float infoWidth = infoFont.width(info);
      float textWidth = Math.max(titleWidth, infoWidth);

      int paddingX = 8;
      int paddingY = 6;
      int iconWidth = 18;
      int gap = 6;
      float panelWidth = (float)(iconWidth + gap) + textWidth + 4.0F;
      float panelHeight = 16.0F + (float)paddingY * 2.0F;

      int centerX = screenWidth / 2;
      int topY = (int)((float)screenHeight / 7.5F);
      float panelX = (float)centerX - panelWidth / 2.0F;
      float panelY = (float)topY - 18.0F;

      ColorRGBA accent = new ColorRGBA(96, 130, 255);
      DrawUtil.drawRoundedRect(
         event.getContext().getMatrices(),
         panelX,
         panelY,
         panelWidth,
         panelHeight,
         BorderRadius.all(6.0F),
         new ColorRGBA(0, 0, 0, 140)
      );
      DrawUtil.drawRoundedRect(
         event.getContext().getMatrices(),
         panelX,
         panelY,
         3.0F,
         panelHeight,
         BorderRadius.all(6.0F),
         accent
      );

      float textX = panelX + (float)iconWidth + (float)gap + 8.0F;
      float titleY = panelY + (float)paddingY + 1.0F;
      float infoY = titleY + 11.0F;
      event.getContext().drawText(titleFont, title, textX + 0.8F, titleY + 0.8F, new ColorRGBA(0, 0, 0, 180));
      event.getContext().drawText(titleFont, title, textX, titleY, accent);
      event.getContext().drawText(infoFont, info, textX + 0.8F, infoY + 0.8F, new ColorRGBA(0, 0, 0, 180));
      event.getContext().drawText(infoFont, info, textX, infoY, new ColorRGBA(198, 198, 198, 255));
   }

   @EventTarget
   private void onRender3D(EventRender3D event) {
      if (!this.showOres.isEnabled() || mc.world == null || mc.player == null) {
         return;
      }

      boolean showOutline = this.renderSettings.isEnable("Обводка");
      boolean showDecorations = this.renderSettings.isEnable("Штрихи");
      boolean showFill = this.renderSettings.isEnable("Заливка");
      if (!showOutline && !showDecorations && !showFill) {
         return;
      }

      boolean renderByBorders = this.highlightType.is("По границам");
      float expandAmount = this.borderScale.getCurrent() / 2.0F;

      BlockPos playerPos = mc.player.getBlockPos();
      this.updateOreCache(playerPos);

      int rendered = 0;
      int maxBoxes = (int) this.boxesPerFrame.getCurrent();
      int radius = (int) this.searchRadius.getCurrent();

      for (Map.Entry<BlockPos, OreInfo> entry : this.oreCache.entrySet()) {
         if (rendered >= maxBoxes) {
            break;
         }
         BlockPos pos = entry.getKey();
         OreInfo oreInfo = entry.getValue();

         double dx = pos.getX() - playerPos.getX();
         double dy = pos.getY() - playerPos.getY();
         double dz = pos.getZ() - playerPos.getZ();
         double distanceSq = dx * dx + dy * dy + dz * dz;
         if (distanceSq > (double)(radius * radius) || !this.isOreEnabled(oreInfo.name)) {
            continue;
         }

         BlockState state = mc.world.getBlockState(pos);
         VoxelShape shape = state.getOutlineShape(mc.world, pos);
         if (shape == null || shape.isEmpty()) {
            continue;
         }

         ColorRGBA baseColor = ColorRGBA.lerp(ColorRGBA.fromInt(oreInfo.color), ColorRGBA.WHITE, 0.06F);
         int fillColor = baseColor.withAlpha(19).getRGB();
         int lineColor = baseColor.withAlpha(128).getRGB();
         int decorationColor = baseColor.withAlpha(89).getRGB();
         float width = 1.0F;

         if (renderByBorders) {
            Vec3d offset = Vec3d.of(pos);
            shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
               Render3DUtil.drawLine(
                  offset.add(minX, minY, minZ),
                  offset.add(maxX, maxY, maxZ),
                  lineColor,
                  width,
                  false
               );
            });
            if (showFill) {
               for (Box box : shape.getBoundingBoxes()) {
                  Render3DUtil.drawBox(box.offset(pos), fillColor, width, false, true, false);
               }
            }
         } else {
            for (Box box : shape.getBoundingBoxes()) {
               Render3DUtil.drawBox(box.offset(pos), lineColor, width, showOutline, showFill, false);
               if (showDecorations) {
                  Render3DUtil.drawLine(
                     new Vec3d(box.minX, box.minY, box.minZ),
                     new Vec3d(box.maxX, box.maxY, box.maxZ),
                     decorationColor,
                     width,
                     false
                  );
                  Render3DUtil.drawLine(
                     new Vec3d(box.maxX, box.minY, box.minZ),
                     new Vec3d(box.minX, box.maxY, box.maxZ),
                     decorationColor,
                     width,
                     false
                  );
               }
            }
         }
         rendered++;
      }
   }

   public String getTimerValue() {
      if (mc.world == null || mc.player == null) {
         return "";
      }
      return this.findTimerValue(this.getArmorStands());
   }

   public int getTimerSeconds() {
      String value = this.getTimerValue();
      if (value.isEmpty()) {
         return -1;
      }
      String[] parts = value.split(":");
      try {
         return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
      } catch (NumberFormatException var6) {
         return -1;
      }
   }

   private List<ArmorStandEntity> getArmorStands() {
      List<ArmorStandEntity> armorStands = new ArrayList();
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof ArmorStandEntity && entity.hasCustomName()) {
            armorStands.add((ArmorStandEntity) entity);
         }
      }
      armorStands.sort(Comparator.comparingDouble((e) -> e.squaredDistanceTo(mc.player)));
      return armorStands;
   }

   private String findMineName(List<ArmorStandEntity> armorStands) {
      for (ArmorStandEntity entity : armorStands) {
         String rawName = entity.getCustomName().getString().trim();
         if (rawName.contains("Следующая:")) {
            return Formatting.strip(rawName).replace("Следующая:", "").trim();
         }
      }
      return "";
   }

   private String findTimerValue(List<ArmorStandEntity> armorStands) {
      for (ArmorStandEntity entity : armorStands) {
         String name = Formatting.strip(entity.getCustomName().getString()).trim();
         if (!name.contains("Обновление через:")) {
            continue;
         }
         ArmorStandEntity closestTimer = null;
         double bestDistance = Double.MAX_VALUE;
         for (ArmorStandEntity candidate : armorStands) {
            String candidateName = Formatting.strip(candidate.getCustomName().getString()).trim();
            Matcher matcher = TIME_PATTERN.matcher(candidateName);
            if (matcher.matches()) {
               double dy = Math.abs(candidate.getY() - entity.getY());
               if (dy < bestDistance) {
                  bestDistance = dy;
                  closestTimer = candidate;
               }
            }
         }
         if (closestTimer != null) {
            Matcher matcher = TIME_PATTERN.matcher(Formatting.strip(closestTimer.getCustomName().getString()).trim());
            if (matcher.matches()) {
               int minutes = Integer.parseInt(matcher.group(1));
               int seconds = Integer.parseInt(matcher.group(2));
               return String.format("%02d:%02d", minutes, seconds);
            }
         }
         break;
      }
      return "";
   }

   private void updateOreCache(BlockPos playerPos) {
      long currentTime = System.currentTimeMillis();
      boolean shouldUpdate = this.lastPlayerPos == null
         || this.getSquaredDistance(this.lastPlayerPos, playerPos) > 16.0D
         || (currentTime - this.lastUpdateTime) > this.cacheUpdateFrequency.getCurrent();
      if (!shouldUpdate) {
         return;
      }

      this.oreCache.clear();
      this.lastPlayerPos = playerPos;
      this.lastUpdateTime = currentTime;

      int radius = (int) this.searchRadius.getCurrent() + 8;
      int maxOres = (int) this.boxesPerFrame.getCurrent() * 2;
      int found = 0;

      for (int dx = -radius; dx <= radius && found < maxOres; dx++) {
         for (int dy = -radius; dy <= radius && found < maxOres; dy++) {
            for (int dz = -radius; dz <= radius && found < maxOres; dz++) {
               BlockPos pos = playerPos.add(dx, dy, dz);
               double dx2 = pos.getX() - playerPos.getX();
               double dy2 = pos.getY() - playerPos.getY();
               double dz2 = pos.getZ() - playerPos.getZ();
               if (dx2 * dx2 + dy2 * dy2 + dz2 * dz2 > (double)(radius * radius)) {
                  continue;
               }
               Block block = mc.world.getBlockState(pos).getBlock();
               OreInfo oreInfo = this.getOreInfo(block);
               if (oreInfo != null) {
                  this.oreCache.put(pos, oreInfo);
                  found++;
               }
            }
         }
      }
   }

   private double getSquaredDistance(BlockPos first, BlockPos second) {
      double dx = first.getX() - second.getX();
      double dy = first.getY() - second.getY();
      double dz = first.getZ() - second.getZ();
      return dx * dx + dy * dy + dz * dz;
   }

   private boolean isOreEnabled(String oreName) {
      MultiBooleanSetting.Value setting = this.oresSettings.getValueByName(oreName);
      return setting != null && setting.isEnabled();
   }

   private OreInfo getOreInfo(Block block) {
      if (block == Blocks.COAL_ORE) {
         return new OreInfo("Уголь", 2829099);
      } else if (block == Blocks.IRON_ORE) {
         return new OreInfo("Железо", 12689801);
      } else if (block == Blocks.REDSTONE_ORE) {
         return new OreInfo("Редстоун", 13644852);
      } else if (block == Blocks.GOLD_ORE) {
         return new OreInfo("Золото", 14730049);
      } else if (block == Blocks.EMERALD_ORE) {
         return new OreInfo("Изумруды", 4116590);
      } else if (block == Blocks.DIAMOND_ORE) {
         return new OreInfo("Алмазы", 4118758);
      } else if (block == Blocks.ANCIENT_DEBRIS) {
         return new OreInfo("Незерит", 7027246);
      } else if (block == Blocks.LAPIS_ORE) {
         return new OreInfo("Лазурит", 3102678);
      } else if (block == Blocks.NETHER_GOLD_ORE) {
         return new OreInfo("Адская золотая руда", 14730049);
      }
      return null;
   }

   private static final class OreInfo {
      final String name;
      final int color;

      OreInfo(String name, int color) {
         this.name = name;
         this.color = color;
      }
   }
}
