package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.base.events.impl.server.EventChatReceive;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.utility.game.other.BaritoneUtil;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.math.Timer;
import tech.huihui.utility.render.level.Render3DUtil;
import net.minecraft.text.Text;

@ModuleAnnotation(
      name = "KeyFinder",
      category = Category.MISC,
      description = "Ищет сундуки и вагонетки с ключами и показывает их в мире"
)
public final class KeyFinder extends Module {
   public static final KeyFinder INSTANCE = new KeyFinder();

    private static final int SEARCH_RADIUS_XZ = 24;
    private static final int SEARCH_DEPTH_DOWN = 70;
    private static final int SEARCH_HEIGHT_UP = 70;
    private static final int SPAWNER_RADIUS = 4;
    private static final int GLOBAL_SEARCH_RADIUS = 200;
    private static final long SCAN_DELAY = 1000L;
    private static final long BARITONE_DELAY = 1500L;
    private static final long REGION_COOLDOWN = 60000L; // 1 минута

    private final BooleanSetting walkWithBaritone = new BooleanSetting(
          "Идти к цели через Baritone",
          "Автоматически отправлять Baritone к ближайшей найденной цели",
          false
    );

    private final BooleanSetting activeSearch = new BooleanSetting(
          "Активный поиск по миру",
          "Перемещаться к целям и сканировать регион в радиусе 200 блоков",
          false
    );

    private final List<KeyTarget> targets = new CopyOnWriteArrayList<>();
    private final Set<BlockPos> visitedContainers = new HashSet<>();
    private final Timer scanTimer = new Timer();
    private final Timer baritoneTimer = new Timer();
    private final Timer regionCooldownTimer = new Timer();
    private final Timer rtpTimer = new Timer();
    private boolean regionCooldown = false;
    private int rtpAttempts = 0;
    private int totalTargetsFound = 0;
    private int lastScanCount = 0;

    private BlockPos currentBaritoneTarget;
    private boolean baritoneMissing;

   private KeyFinder() {
   }

    @Override
    public void onEnable() {
       super.onEnable();
       this.targets.clear();
       this.visitedContainers.clear();
       this.currentBaritoneTarget = null;
       this.baritoneMissing = false;
       this.scanTimer.reset();
       this.baritoneTimer.reset();
       this.regionCooldown = false;
       this.rtpAttempts = 0;
       this.totalTargetsFound = 0;
       this.lastScanCount = 0;
       this.regionCooldownTimer.reset();
       this.rtpTimer.reset();

       if (this.walkWithBaritone.isEnabled() && !BaritoneUtil.isPresent()) {
          this.baritoneMissing = true;
          MessageUtil.displayInfo("KeyFinder: Baritone не найден, автоматический маршрут недоступен");
       }
    }

    @Override
    public void onDisable() {
       this.stopBaritone();
       if (!this.targets.isEmpty()) {
          MessageUtil.displayInfo(String.format("END KeyFinder выключен: осталось %d целей", this.targets.size()));
       }
       if (this.activeSearch.isEnabled()) {
          MessageUtil.displayInfo("🛑 Активный поиск отключён");
       }
       this.targets.clear();
       this.currentBaritoneTarget = null;
       super.onDisable();
    }

    @EventTarget
    private void onTick(EventTick event) {
       if (mc.world == null || mc.player == null) {
          return;
       }

       if (this.activeSearch.isEnabled()) {
          if (this.scanTimer.finished(SCAN_DELAY)) {
             this.scanWorld();
             this.scanTimer.reset();
          }

          if (this.regionCooldown && this.regionCooldownTimer.finished(REGION_COOLDOWN)) {
             this.regionCooldown = false;
             this.rtpAttempts = 0;
             MessageUtil.displayInfo("Кулдаун /rtp прошёл — могу искать дальше");
          }

          if (this.regionCooldown && this.activeSearch.isEnabled()) {
             this.rtpTimer.reset();
             this.activeSearch.setEnabled(false);
             MessageUtil.displayInfo("OFF Обнаружен защищённый регион — отключаю поиск и делаю /rtp");
             mc.player.networkHandler.sendChatMessage("/rtp");
             this.regionCooldown = true;
             return;
          }
       }

       if (this.walkWithBaritone.isEnabled() && !this.baritoneMissing) {
          this.followNearestTarget();
       }
    }

    @EventTarget
    private void onChatReceive(EventChatReceive event) {
       Text message = event.getMessage();
       String lower = message.getString().toLowerCase();

       if (this.activeSearch.isEnabled() && this.regionCooldown) {
          if (lower.contains("не можете сломать")
                || lower.contains("нельзя сломать")
                || lower.contains("cannot break")
                || lower.contains("защищен")
                || lower.contains("защищён")
                || lower.contains("приват")
                || lower.contains("обломки не могут быть сломаны")
                || lower.contains("this structure is protected")) {
             if (this.rtpAttempts < 3) {
                this.rtpAttempts++;
                MessageUtil.displayInfo("WARN Защищённый регион — делаю /rtp");
                mc.player.networkHandler.sendChatMessage("/rtp");
                this.regionCooldown = true;
                this.regionCooldownTimer.reset();
             } else {
                MessageUtil.displayInfo("ERR Регион защищён, отключаю активный поиск");
                this.activeSearch.setEnabled(false);
                this.regionCooldown = false;
             }
          }
       }

       if (this.rtpTimer.finished(10000L)) {
          if (lower.contains("телепортировался") || lower.contains("you have been teleported")) {
             this.rtpAttempts = 0;
             this.regionCooldown = false;
             this.rtpTimer.reset();
             MessageUtil.displayInfo("OK Телепорт выполнен, возобновляю поиск");
          }
       }
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
       if (mc.world == null || this.targets.isEmpty()) {
          return;
       }

       for (KeyTarget target : this.targets) {
          int color = switch (target.status) {
             case HAS_KEY -> 0xFF35D07F;
             case UNLOOTED -> 0xFFFFC857;
             case LOOTED -> 0xFF888888;
          };
          Render3DUtil.drawBox(new Box(target.position), color, 1.5F, true, false, false);
       }

        if (this.regionCooldown && this.activeSearch.isEnabled()) {
           MessageUtil.displayInfo("[!] Защищённый регион - отключаю поиск и делаю /rtp");
        }
     }

     @EventTarget
     private void onHudRender(EventHudRender event) {
        if (this.activeSearch.isEnabled() && !this.regionCooldown && mc.player != null) {
           for (KeyTarget target : this.targets) {
              String status = switch (target.status) {
                 case HAS_KEY -> "[KEY] С ключом";
                 case UNLOOTED -> "[?] Без ключа";
                 case LOOTED -> "[OK] Залутано";
              };
              String coords = String.format("(%d, %d, %d)", target.position.getX(), target.position.getY(), target.position.getZ());
              double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(target.position));
              
              MessageUtil.displayInfo(String.format("[TARGET] %s: %s - %s (%.1f блоков)", status, coords, target.position.toShortString(), distance));
           }
        }
     }

    private void scanWorld() {
      BlockPos playerPos = mc.player.getBlockPos();

      List<KeyTarget> found = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();

       for (Entity entity : mc.world.getEntities()) {
          if (!(entity instanceof ChestMinecartEntity minecart)) {
             continue;
          }
          double px = mc.player.getX();
          double py = mc.player.getY();
          double pz = mc.player.getZ();
          double mx = minecart.getX();
          double my = minecart.getY();
          double mz = minecart.getZ();
          if (Math.abs(mx - px) > SEARCH_RADIUS_XZ
                || Math.abs(mz - pz) > SEARCH_RADIUS_XZ
                || py - my > SEARCH_DEPTH_DOWN
                || my - py > SEARCH_HEIGHT_UP) {
             continue;
          }

          BlockPos position = minecart.getBlockPos().toImmutable();
          if (seen.add(position)) {
             LootState state = this.hasKey(minecart) ? LootState.HAS_KEY : LootState.UNLOOTED;
             KeyTarget target = new KeyTarget(position, state);
             found.add(target);
             if (state == LootState.HAS_KEY) {
                MessageUtil.displayInfo(String.format("[KEY] Найден ключ в вагонетке в (%d, %d, %d)!", 
                      position.getX(), position.getY(), position.getZ()));
             }
          }
       }

       for (BlockPos spawner : this.findSpawners(playerPos)) {
          for (BlockPos chest : this.findNearbyContainers(spawner)) {
             if (seen.add(chest)) {
                BlockEntity blockEntity = mc.world.getBlockEntity(chest);
                LootState state = this.hasKey(blockEntity) ? LootState.HAS_KEY : LootState.UNLOOTED;
                KeyTarget target = new KeyTarget(chest, state);
                found.add(target);
                if (state == LootState.HAS_KEY) {
                   MessageUtil.displayInfo(String.format("[KEY] Найден ключ в сундуке рядом со спавнером в (%d, %d, %d)!", 
                         chest.getX(), chest.getY(), chest.getZ()));
                }
             }
          }
       }

       this.targets.clear();
       this.targets.addAll(found);
       
        if (!found.isEmpty()) {
           MessageUtil.displayInfo(String.format("[SCAN] KeyFinder: Найдено %d цели(й) в радиусе %d по сторонам и %d вверх/вниз", found.size(), SEARCH_RADIUS_XZ, SEARCH_DEPTH_DOWN));
        }
    }

   private List<BlockPos> findSpawners(BlockPos center) {
      List<BlockPos> result = new ArrayList<>();
      for (int dx = -SEARCH_RADIUS_XZ; dx <= SEARCH_RADIUS_XZ; dx++) {
         for (int dy = -SEARCH_DEPTH_DOWN; dy <= SEARCH_HEIGHT_UP; dy++) {
            for (int dz = -SEARCH_RADIUS_XZ; dz <= SEARCH_RADIUS_XZ; dz++) {
               BlockPos position = center.add(dx, dy, dz);
               if (mc.world.getBlockState(position).isOf(Blocks.SPAWNER)) {
                  result.add(position.toImmutable());
               }
            }
         }
      }
      return result;
   }

   private List<BlockPos> findNearbyContainers(BlockPos spawner) {
      List<BlockPos> result = new ArrayList<>();
      for (BlockPos position : BlockPos.iterateOutwards(spawner, SPAWNER_RADIUS, SPAWNER_RADIUS, SPAWNER_RADIUS)) {
         if (mc.world.getBlockEntity(position) instanceof LootableContainerBlockEntity) {
            result.add(position.toImmutable());
         }
      }
      return result;
   }

   private boolean hasKey(BlockEntity blockEntity) {
      if (!(blockEntity instanceof LootableContainerBlockEntity inventory)) {
         return false;
      }
      for (int slot = 0; slot < inventory.size(); slot++) {
         if (this.isKey(inventory.getStack(slot))) {
            return true;
         }
      }
      return false;
   }

   private boolean hasKey(ChestMinecartEntity inventory) {
      for (int slot = 0; slot < inventory.size(); slot++) {
         if (this.isKey(inventory.getStack(slot))) {
            return true;
         }
      }
      return false;
   }

   private boolean isKey(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() == Items.TRIPWIRE_HOOK;
   }

   private void followNearestTarget() {
      if (mc.player == null || this.targets.isEmpty()) {
         return;
      }

      if (!BaritoneUtil.isPresent()) {
         this.baritoneMissing = true;
         return;
      }

      KeyTarget nearest = null;
      double nearestDistance = Double.MAX_VALUE;
      for (KeyTarget target : this.targets) {
         if (target.status == LootState.LOOTED) {
            continue;
         }
         double distance = mc.player.getPos().squaredDistanceTo(target.position.toCenterPos());
         if (distance < nearestDistance) {
            nearestDistance = distance;
            nearest = target;
         }
      }

      if (nearest == null || nearest.position.equals(this.currentBaritoneTarget)
            || !this.baritoneTimer.finished(BARITONE_DELAY)) {
         return;
      }

       this.currentBaritoneTarget = nearest.position;
       this.baritoneTimer.reset();
       String targetType = nearest.status == LootState.HAS_KEY ? "KEY с ключом" : "CHT без ключа";
       String coords = String.format("X:%d Y:%d Z:%d", nearest.position.getX(), nearest.position.getY(), nearest.position.getZ());
       MessageUtil.displayInfo(String.format("🧭 KeyFinder: Иду к цели через Baritone: %s в %s", targetType, coords));
       mc.player.networkHandler.sendChatMessage("#goto "
             + nearest.position.getX() + " "
             + nearest.position.getY() + " "
             + nearest.position.getZ());
    }

   private void stopBaritone() {
      if (mc.player != null && mc.player.networkHandler != null && this.currentBaritoneTarget != null) {
         mc.player.networkHandler.sendChatMessage("#stop");
      }
   }

   private enum LootState {
      UNLOOTED,
      HAS_KEY,
      LOOTED
   }

   private static final class KeyTarget {
      private final BlockPos position;
      private final LootState status;

      private KeyTarget(BlockPos position, LootState status) {
         this.position = position;
         this.status = status;
      }
   }
}
