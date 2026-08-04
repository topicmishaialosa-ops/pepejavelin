package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.impl.misc.PilotSettings;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.math.Timer;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "BaseFinder",
   category = Category.MISC,
   description = "Летает на элитрах по карте и записывает найденных игроков"
)
public final class BaseFinder extends Module {
   public static final BaseFinder INSTANCE = new BaseFinder();

    private static final int MIN_COORD = -2500;
    private static final int MAX_COORD = 2500;
    private static final int TARGET_HEIGHT = 120;
    private static final long CLIMB_FIREWORK_INTERVAL = 1000L;

   private final List<PlayerRecord> foundPlayers = new ArrayList<>();
   private final Random random = new Random();
   private final Timer fireworkTimer = new Timer();
   private final Timer climbTimer = new Timer();

   private boolean scanning;
   private boolean elytraEquipped;
   private boolean waitingForElytra;
   private boolean reachedTargetHeight;
   private BlockPos currentTarget;
   private boolean startedFlight;

   private static final ColorRGBA HISTORY_BG = new ColorRGBA(0, 0, 0, 120);
   private static final ColorRGBA HISTORY_BORDER = new ColorRGBA(255, 255, 255, 40);
   private static final ColorRGBA HISTORY_TITLE = new ColorRGBA(88, 166, 255, 255);
   private static final ColorRGBA HISTORY_NAME = new ColorRGBA(255, 255, 255, 255);
   private static final ColorRGBA HISTORY_COORD = new ColorRGBA(153, 153, 153, 255);

   private static final class PlayerRecord {
      final String name;
      final BlockPos position;
      final Date foundTime;

      PlayerRecord(String name, BlockPos position) {
         this.name = name;
         this.position = position;
         this.foundTime = new Date();
      }
   }

   private BaseFinder() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (this.scanning) {
         return;
      }
      this.foundPlayers.clear();
      this.scanning = true;
      this.elytraEquipped = false;
      this.waitingForElytra = false;
      this.reachedTargetHeight = false;
      this.currentTarget = null;
      this.startedFlight = false;
      this.fireworkTimer.reset();
      this.climbTimer.reset();
      MessageUtil.displayInfo("[BaseFinder] Сканирование начато. Проверка элитры...");
   }

   @Override
   public void onDisable() {
      this.scanning = false;
      this.saveResultsToFile();
      MessageUtil.displayInfo("[BaseFinder] Сканирование остановлено.");
      super.onDisable();
   }

   @EventTarget
   private void onTick(EventTick event) {
      if (!this.scanning || mc.player == null || mc.world == null) {
         return;
      }

      if (!this.elytraEquipped) {
         if (!this.hasElytraEquipped()) {
            if (!this.waitingForElytra) {
               this.equipElytra();
               this.waitingForElytra = true;
            }
            return;
         }
         this.elytraEquipped = true;
         MessageUtil.displayInfo("[BaseFinder] Элитра надета, готовимся к взлету...");
         return;
      }

      if (!mc.player.isGliding()) {
         if (mc.player.isOnGround()) {
            mc.player.jump();
            return;
         }
         if (mc.player.getVelocity().y < 0.0D) {
            mc.options.jumpKey.setPressed(true);
            return;
         }
         return;
      }

      if (!this.reachedTargetHeight) {
         this.climbToTargetHeight();
         return;
      }

      this.checkForPlayers();
      this.maintainAltitude();
      this.avoidCollisions();

      if (this.currentTarget == null || this.isAtTarget()) {
         this.setRandomTarget();
      }

      this.flyToTarget();
   }

   @EventTarget
   private void onRender(EventHudRender event) {
      if (!this.scanning || mc.world == null || mc.player == null) {
         return;
      }
      if (this.foundPlayers.isEmpty()) {
         return;
      }
      CustomDrawContext ctx = event.getContext();
      float x = 4.0F;
      float y = 4.0F;
      float lineHeight = 9.0F;
      float pad = 4.0F;
      float textW = 0.0F;
      for (PlayerRecord record : this.foundPlayers) {
         float recordW = Fonts.REGULAR.getWidth(record.name, 6.5F) + 6.0F + Fonts.REGULAR.getWidth("X: " + record.position.getX() + " Y: " + record.position.getY() + " Z: " + record.position.getZ(), 5.5F);
         textW = Math.max(textW, recordW);
      }
      float titleW = Fonts.REGULAR.getWidth("Найденные игроки: " + this.foundPlayers.size(), 6.5F);
      float width = Math.max(textW, titleW) + pad * 2.0F;
      float height = pad * 2.0F + lineHeight + (float) this.foundPlayers.size() * lineHeight;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(4.0F), HISTORY_BG);
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(4.0F), HISTORY_BORDER);
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), "Найденные игроки: " + this.foundPlayers.size(), x + pad, y + 3.0F, HISTORY_TITLE);
      float cursor = y + pad + lineHeight + 1.0F;
      for (PlayerRecord record : this.foundPlayers) {
         ctx.drawText(Fonts.REGULAR.getFont(6.5F), record.name, x + pad, cursor, HISTORY_NAME);
         ctx.drawText(Fonts.REGULAR.getFont(5.5F), "X: " + record.position.getX() + " Y: " + record.position.getY() + " Z: " + record.position.getZ(), x + pad + Fonts.REGULAR.getWidth(record.name, 6.5F) + 6.0F, cursor + 1.0F, HISTORY_COORD);
         cursor += lineHeight;
      }
   }

   private boolean hasElytraEquipped() {
      ItemStack chestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      return chestplate.getItem() == Items.ELYTRA && chestplate.getDamage() < chestplate.getMaxDamage() - 10;
   }

   private void equipElytra() {
      ItemStack currentChestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      boolean hasChestplate = !currentChestplate.isEmpty() && currentChestplate.getItem() != Items.ELYTRA;

      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.ELYTRA && stack.getDamage() < stack.getMaxDamage() - 10) {
            if (hasChestplate) {
               int emptySlot = -1;
               for (int j = 0; j < mc.player.getInventory().size(); j++) {
                  if (mc.player.getInventory().getStack(j).isEmpty()) {
                     emptySlot = j;
                     break;
                  }
               }
               if (emptySlot != -1) {
                  mc.interactionManager.clickSlot(0, 6, emptySlot < 9 ? emptySlot + 36 : emptySlot, SlotActionType.SWAP, mc.player);
               }
            }
            mc.interactionManager.clickSlot(0, i < 9 ? i + 36 : i, 6, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket(0));
            MessageUtil.displayInfo("[BaseFinder] Элитра надета автоматически");
            return;
         }
      }
      MessageUtil.displayInfo("[BaseFinder] В инвентаре нет пригодной элитры!");
      this.setToggled(false);
   }

   private void climbToTargetHeight() {
      if (mc.player.getY() < (double) TARGET_HEIGHT) {
         mc.player.setPitch(-45.0F);
         if (this.climbTimer.finished(CLIMB_FIREWORK_INTERVAL)) {
            this.useFirework();
            this.climbTimer.reset();
         }
      } else {
         this.reachedTargetHeight = true;
         mc.player.setPitch(0.0F);
         MessageUtil.displayInfo("[BaseFinder] Достигнута нужная высота 120 блоков");
      }
   }

   private void maintainAltitude() {
      if (!mc.player.isGliding()) {
         return;
      }
      if (mc.player.getY() < (double) (TARGET_HEIGHT - 5)) {
         mc.player.setPitch(-15.0F);
      } else if (mc.player.getY() > (double) (TARGET_HEIGHT + 5)) {
         mc.player.setPitch(5.0F);
      } else {
         mc.player.setPitch(0.0F);
      }
   }

   private void avoidCollisions() {
      if (!mc.player.isGliding()) {
         return;
      }
      Vec3d lookVec = mc.player.getRotationVector();
      BlockPos checkPos = BlockPos.ofFloored(mc.player.getX() + lookVec.x * 15.0D, mc.player.getY() + lookVec.y * 15.0D, mc.player.getZ() + lookVec.z * 15.0D);
      if (!mc.world.getBlockState(checkPos).isAir()) {
         mc.player.setYaw(mc.player.getYaw() + 45.0F);
         MessageUtil.displayInfo("[BaseFinder] Обход препятствия");
      }
   }

   private boolean isAtTarget() {
      if (mc.player == null || this.currentTarget == null) {
         return true;
      }
      return this.currentTarget.isWithinDistance(mc.player.getBlockPos(), 15.0D);
   }

   private void setRandomTarget() {
      int x = this.random.nextInt(MAX_COORD - MIN_COORD) + MIN_COORD;
      int z = this.random.nextInt(MAX_COORD - MIN_COORD) + MIN_COORD;
      this.currentTarget = new BlockPos(x, TARGET_HEIGHT, z);
      MessageUtil.displayInfo("[BaseFinder] Летим к: X: " + x + " Z: " + z);
   }

   private void flyToTarget() {
      if (this.currentTarget == null || !mc.player.isGliding()) {
         return;
      }
      Vec3d targetVec = Vec3d.ofCenter(this.currentTarget);
      Vec3d direction = targetVec.subtract(mc.player.getPos()).normalize();
      float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
      mc.player.setYaw(yaw);

      if (this.fireworkTimer.finished(this.cruiseFireworkIntervalMs())) {
         this.useFirework();
         this.fireworkTimer.reset();
      }
   }

   private long cruiseFireworkIntervalMs() {
      float perSecond = Math.max(PilotSettings.INSTANCE.getFireworksPerSecond(), 0.02F);
      return (long) Math.max((double) (1000.0F / perSecond), 1000.0D);
   }

   private void useFirework() {
      if (PlayerInventoryUtil.find(Items.FIREWORK_ROCKET, 0, 8) == -1) {
         MessageUtil.displayInfo("[BaseFinder] Нет фейерверков в горячей панели!");
         return;
      }
      PlayerInventoryUtil.swapAndUseLegit(Items.FIREWORK_ROCKET);
   }

   private void checkForPlayers() {
      if (mc.world == null) {
         return;
      }
      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player) {
            continue;
         }
         boolean exists = this.foundPlayers.stream().anyMatch(record -> record.name.equals(player.getName().getString()));
         if (!exists) {
            PlayerRecord record = new PlayerRecord(player.getName().getString(), player.getBlockPos());
            this.foundPlayers.add(record);
            MessageUtil.displayInfo("[BaseFinder] Найден игрок: " + record.name + " в координатах X: " + record.position.getX() + " Y: " + record.position.getY() + " Z: " + record.position.getZ());
         }
      }
   }

   private void saveResultsToFile() {
      if (this.foundPlayers.isEmpty()) {
         return;
      }
      Path desktop = getDesktopPath();
      String filename = "BaseFinder_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + ".txt";
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(desktop.resolve(filename).toFile()))) {
         writer.write("Найденные игроки - " + new Date() + "\n\n");
         for (PlayerRecord record : this.foundPlayers) {
            writer.write(String.format("Игрок: %-16s | Координаты: X: %-6d Y: %-4d Z: %-6d | Время: %s\n", record.name, record.position.getX(), record.position.getY(), record.position.getZ(), new SimpleDateFormat("HH:mm:ss").format(record.foundTime)));
         }
         MessageUtil.displayInfo("[BaseFinder] Результаты сохранены в файл: " + desktop.resolve(filename));
      } catch (IOException e) {
         MessageUtil.displayInfo("[BaseFinder] Ошибка сохранения: " + e.getMessage());
      }
   }

   private static Path getDesktopPath() {
      String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
      String home = System.getProperty("user.home", ".");
      List<Path> candidates = new ArrayList<>();
      if (os.contains("win")) {
         String userProfile = System.getenv("USERPROFILE");
         if (userProfile != null && !userProfile.isEmpty()) {
            candidates.add(Path.of(userProfile, "Desktop"));
         }
      } else if (os.contains("mac")) {
         candidates.add(Path.of(home, "Desktop"));
      } else {
         try {
            Path userDirs = Path.of(home, ".config", "user-dirs.dirs");
            if (Files.exists(userDirs)) {
               for (String line : Files.readAllLines(userDirs)) {
                  if (line.startsWith("XDG_DESKTOP_DIR=")) {
                     String value = line.substring(line.indexOf('=') + 1).replace("\"", "").trim();
                     value = value.replace("$HOME", home);
                     if (value.startsWith("~/")) {
                        value = home + value.substring(1);
                     }
                     if (!value.isEmpty()) {
                        candidates.add(Path.of(value));
                     }
                     break;
                  }
               }
            }
         } catch (IOException ignored) {
         }
         candidates.add(Path.of(home, "Desktop"));
      }
      for (Path candidate : candidates) {
         if (Files.isDirectory(candidate)) {
            return candidate;
         }
      }
      return candidates.isEmpty() ? Path.of(home) : candidates.get(candidates.size() - 1);
   }
}
