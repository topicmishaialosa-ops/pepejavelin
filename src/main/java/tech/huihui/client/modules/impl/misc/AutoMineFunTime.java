package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.base.events.impl.render.EventRender2D;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "AutoMineFunTime",
   category = Category.MISC,
   description = "Автоматический копатель через Baritone в зоне шахты"
)
public final class AutoMineFunTime extends Module {
   public static final AutoMineFunTime INSTANCE = new AutoMineFunTime();

   // ========== НАСТРОЙКИ ЗОНЫ ШАХТЫ ==========
   private final BlockPos zoneMin = new BlockPos(-86, 81, -5);
   private final BlockPos zoneMax = new BlockPos(-66, 72, 15);

   // ========== НАСТРОЙКИ МОДУЛЯ ==========
   private final MultiBooleanSetting ores = new MultiBooleanSetting(
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
   private final BooleanSetting autoRestart = new BooleanSetting("Авто-перезапуск", true);
   private final BooleanSetting showPanel = new BooleanSetting("Панель", true);
   private final BooleanSetting noBreakOutside = new BooleanSetting("Не ломать вне зоны", true);
   private final BooleanSetting noPlace = new BooleanSetting("Не ставить блоки", true);

   // ========== СОСТОЯНИЕ ==========
   private boolean mining;
   private boolean waiting;
   private long lastCheckTime;
   private String cachedTimer = "";
   private static final long CHECK_INTERVAL = 1000L;
   private static final long RESTART_COOLDOWN = 5000L;
   private static final int ORE_RADIUS = 8;

   private AutoMineFunTime() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.mining = false;
      this.waiting = false;
      this.lastCheckTime = 0L;
      this.cachedTimer = "";
      this.applyBaritoneRestrictions();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.stopMining();
      this.restoreBaritoneSettings();
   }

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (mc.world == null || mc.player == null || mc.player.networkHandler == null) {
         return;
      }
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastCheckTime < CHECK_INTERVAL) {
         return;
      }
      this.lastCheckTime = currentTime;

      if (this.ores.getSelectedNames().isEmpty()) {
         this.stopMining();
         this.waiting = false;
         this.cachedTimer = "";
         return;
      }

      if (!this.isInMineZone(mc.player.getBlockPos())) {
         this.stopMining();
         this.waiting = false;
         this.cachedTimer = "";
         return;
      }

      if (!this.hasOresInZone()) {
         this.stopMining();
         this.waiting = true;
         this.cachedTimer = this.findMineTimer();
         return;
      }

      this.waiting = false;
      this.cachedTimer = "";
      if (!this.mining) {
         this.startMining();
      } else if (this.autoRestart.isEnabled() && currentTime - this.lastMineAt > RESTART_COOLDOWN) {
         // Модуль уже в зоне и руды есть, но копание не идёт — перезапускаем
         this.startMining();
      }
   }

   private long lastMineAt;

   /**
    * Проверяет, находится ли позиция внутри зоны шахты
    */
   private boolean isInMineZone(BlockPos pos) {
      return pos.getX() >= Math.min(this.zoneMin.getX(), this.zoneMax.getX())
          && pos.getX() <= Math.max(this.zoneMin.getX(), this.zoneMax.getX())
          && pos.getY() >= Math.min(this.zoneMin.getY(), this.zoneMax.getY())
          && pos.getY() <= Math.max(this.zoneMin.getY(), this.zoneMax.getY())
          && pos.getZ() >= Math.min(this.zoneMin.getZ(), this.zoneMax.getZ())
          && pos.getZ() <= Math.max(this.zoneMin.getZ(), this.zoneMax.getZ());
   }

   /**
    * Проверяет, есть ли руды в зоне копания (радиус вокруг игрока, вся высота зоны)
    */
   private boolean hasOresInZone() {
      BlockPos playerPos = mc.player.getBlockPos();
      int minY = Math.min(this.zoneMin.getY(), this.zoneMax.getY());
      int maxY = Math.max(this.zoneMin.getY(), this.zoneMax.getY());
      for (int dx = -ORE_RADIUS; dx <= ORE_RADIUS; dx++) {
         for (int y = minY; y <= maxY; y++) {
            for (int dz = -ORE_RADIUS; dz <= ORE_RADIUS; dz++) {
               BlockPos checkPos = new BlockPos(playerPos.getX() + dx, y, playerPos.getZ() + dz);
               if (!this.isInMineZone(checkPos)) {
                  continue;
               }
               if (this.isOre(mc.world.getBlockState(checkPos).getBlock())) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   /**
    * Проверяет, является ли блок рудой (из выбранных)
    */
   private boolean isOre(Block block) {
      return this.getOreName(block) != null;
   }

   private String getOreName(Block block) {
      for (String oreName : this.ores.getSelectedNames()) {
         for (Block oreBlock : this.getOreBlocks(oreName)) {
            if (oreBlock == block) {
               return oreName;
            }
         }
      }
      return null;
   }

   private List<Block> getOreBlocks(String oreName) {
      List<Block> blocks = new ArrayList();
      switch (oreName) {
         case "Уголь":
            blocks.add(Blocks.COAL_ORE);
            blocks.add(Blocks.DEEPSLATE_COAL_ORE);
            break;
         case "Железо":
            blocks.add(Blocks.IRON_ORE);
            blocks.add(Blocks.DEEPSLATE_IRON_ORE);
            break;
         case "Редстоун":
            blocks.add(Blocks.REDSTONE_ORE);
            blocks.add(Blocks.DEEPSLATE_REDSTONE_ORE);
            break;
         case "Золото":
            blocks.add(Blocks.GOLD_ORE);
            blocks.add(Blocks.DEEPSLATE_GOLD_ORE);
            break;
         case "Изумруды":
            blocks.add(Blocks.EMERALD_ORE);
            blocks.add(Blocks.DEEPSLATE_EMERALD_ORE);
            break;
         case "Алмазы":
            blocks.add(Blocks.DIAMOND_ORE);
            blocks.add(Blocks.DEEPSLATE_DIAMOND_ORE);
            break;
         case "Незерит":
            blocks.add(Blocks.ANCIENT_DEBRIS);
            break;
         case "Лазурит":
            blocks.add(Blocks.LAPIS_ORE);
            blocks.add(Blocks.DEEPSLATE_LAPIS_ORE);
            break;
         case "Адская золотая руда":
            blocks.add(Blocks.NETHER_GOLD_ORE);
            break;
      }
      return blocks;
   }

   /**
    * Запускает Baritone на копание руд через чат-команду
    */
   private void startMining() {
      if (mc.player.networkHandler == null || this.ores.getSelectedNames().isEmpty()) {
         return;
      }
      List<String> blockNames = new ArrayList();
      for (String oreName : this.ores.getSelectedNames()) {
         for (Block block : this.getOreBlocks(oreName)) {
            blockNames.add(net.minecraft.registry.Registries.BLOCK.getId(block).getPath());
         }
      }
      this.mining = true;
      this.lastMineAt = System.currentTimeMillis();
      StringBuilder command = new StringBuilder("#mine");
      for (String blockName : blockNames) {
         command.append(' ').append(blockName);
      }
      mc.player.networkHandler.sendChatMessage(command.toString());
   }

   /**
    * Останавливает Baritone
    */
   private void stopMining() {
      if (!this.mining) {
         return;
      }
      this.mining = false;
      mc.player.networkHandler.sendChatMessage("#stop");
   }

   /**
    * Жёсткие ограничения Baritone: allowPlace=false (не ставить блоки нигде)
    * и ограничение ломания по высоте зоны (min/maxYLevelWhileMining).
    * Границы X/Z охраняются модулем: при выходе игрока за пределы — мгновенный #stop.
    */
   private void applyBaritoneRestrictions() {
      try {
         ClassLoader classLoader = this.getClass().getClassLoader();
         Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI", true, classLoader);
         Object settings = apiClass.getMethod("getSettings").invoke(null);
         Class<?> settingsClass = settings.getClass();
         if (this.noPlace.isEnabled()) {
            this.setSettingValue(settingsClass, settings, "allowPlace", false);
         }
         if (this.noBreakOutside.isEnabled()) {
            int zoneMinY = Math.min(this.zoneMin.getY(), this.zoneMax.getY());
            int zoneMaxY = Math.max(this.zoneMin.getY(), this.zoneMax.getY());
            this.setSettingValue(settingsClass, settings, "minYLevelWhileMining", zoneMinY);
            this.setSettingValue(settingsClass, settings, "maxYLevelWhileMining", zoneMaxY);
         }
      } catch (Throwable throwable) {
      }
   }

   private void setSettingValue(Class<?> settingsClass, Object settings, String fieldName, Object value)
      throws ReflectiveOperationException {
      Object setting = settingsClass.getField(fieldName).get(settings);
      setting.getClass().getField("value").set(setting, value);
   }

   private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

   /**
    * Читает таймер обновления шахты локально (без MineHelper): ищет арморстенд
    * "Обновление через:" и ближайший к нему по высоте с временем MM:SS.
    */
   private String findMineTimer() {
      if (mc.world == null || mc.player == null) {
         return "";
      }
      List<net.minecraft.entity.decoration.ArmorStandEntity> armorStands = new ArrayList();
      for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
         if (entity instanceof net.minecraft.entity.decoration.ArmorStandEntity && entity.hasCustomName()) {
            armorStands.add((net.minecraft.entity.decoration.ArmorStandEntity) entity);
         }
      }
      armorStands.sort(
         java.util.Comparator.comparingDouble(
            (e) -> e.squaredDistanceTo(mc.player)
         )
      );
      for (net.minecraft.entity.decoration.ArmorStandEntity entity : armorStands) {
         String name = net.minecraft.util.Formatting.strip(entity.getCustomName().getString()).trim();
         if (!name.contains("Обновление через:")) {
            continue;
         }
         net.minecraft.entity.decoration.ArmorStandEntity closestTimer = null;
         double bestDistance = Double.MAX_VALUE;
         for (net.minecraft.entity.decoration.ArmorStandEntity candidate : armorStands) {
            String candidateName = net.minecraft.util.Formatting.strip(
               candidate.getCustomName().getString()
            ).trim();
            if (TIME_PATTERN.matcher(candidateName).matches()) {
               double dy = Math.abs(candidate.getY() - entity.getY());
               if (dy < bestDistance) {
                  bestDistance = dy;
                  closestTimer = candidate;
               }
            }
         }
         if (closestTimer != null) {
            java.util.regex.Matcher matcher = TIME_PATTERN.matcher(
               net.minecraft.util.Formatting.strip(closestTimer.getCustomName().getString()).trim()
            );
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

   /**
    * Возвращает настройки Baritone к значениям по умолчанию
    */
   private void restoreBaritoneSettings() {
      try {
         ClassLoader classLoader = this.getClass().getClassLoader();
         Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI", true, classLoader);
         Object settings = apiClass.getMethod("getSettings").invoke(null);
         Class<?> settingsClass = settings.getClass();
         this.setSettingValue(settingsClass, settings, "allowPlace", true);
         this.setSettingValue(settingsClass, settings, "minYLevelWhileMining", -64);
         this.setSettingValue(settingsClass, settings, "maxYLevelWhileMining", 320);
      } catch (Throwable throwable) {
      }
   }

   @EventTarget
   private void onRender2D(EventRender2D event) {
      if (!this.showPanel.isEnabled() || mc.world == null || mc.player == null) {
         return;
      }

      String status;
      String timerText;
      if (!this.isInMineZone(mc.player.getBlockPos())) {
         status = "Вне зоны шахты";
         timerText = "Обновление: —";
      } else if (this.waiting) {
         status = "Жду обновления шахты...";
         timerText = this.cachedTimer.isEmpty() ? "Обновление: —" : "Обновление через: " + this.cachedTimer;
      } else {
         status = "Копаю...";
         timerText = "Обновление: —";
      }

      Font titleFont = Fonts.MEDIUM.getFont(7.0F);
      Font infoFont = Fonts.REGULAR.getFont(6.5F);
      float textWidth = titleFont.width("AutoMine");
      textWidth = Math.max(textWidth, infoFont.width(status));
      textWidth = Math.max(textWidth, infoFont.width(timerText));

      int paddingX = 8;
      int paddingY = 6;
      int iconWidth = 18;
      int gap = 6;
      float panelWidth = (float)(iconWidth + gap) + textWidth + 16.0F;
      float panelHeight = 16.0F + 2.0F * 11.0F + (float)paddingY * 2.0F - 6.0F;

      int centerX = mc.getWindow().getScaledWidth() / 2;
      int topY = (int)((float)mc.getWindow().getScaledHeight() / 7.5F) + 22;
      float panelX = (float)centerX - panelWidth / 2.0F;
      float panelY = (float)topY;

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
      float lineY = panelY + (float)paddingY + 1.0F;
      event.getContext().drawText(titleFont, "AutoMine", textX, lineY, accent);
      lineY += 11.0F;
      event.getContext().drawText(infoFont, status, textX, lineY, new ColorRGBA(198, 198, 198, 255));
      lineY += 11.0F;
      event.getContext().drawText(infoFont, timerText, textX, lineY, new ColorRGBA(198, 198, 198, 255));
   }
}