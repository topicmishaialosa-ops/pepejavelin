package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.border.WorldBorder;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.impl.render.XRay;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.other.BaritoneUtil;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.math.Timer;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "AncientFarmer",
   category = Category.MISC,
   description = "Автоматически фармит древние обломки в режиме полёта или ходьбы"
)
public final class AncientFarmer extends Module {
   public static final AncientFarmer INSTANCE = new AncientFarmer();

   private final ModeSetting searchMode = new ModeSetting("Режим поиска территории", "Поиск сверху", "Поиск сверху", "Поиск снизу");
   private final BooleanSetting useFly = new BooleanSetting("Использовать полёт", "Не требовать режим полёта, работать в ходьбе", true);

   private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "AncientFarmer-Search");
      thread.setDaemon(true);
      return thread;
   });
   private final Timer mineTimer = new Timer();
   private final Timer gotoTimer = new Timer();
   private Phase phase = Phase.SEARCH;
   private BlockPos target;
   private Box territory;
   private boolean baritoneMissing;

   private AncientFarmer() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.baritoneMissing = !BaritoneUtil.isPresent();
      if (this.baritoneMissing) {
         MessageUtil.displayInfo("Скачайте Baritone, чтобы использовать AncientFarmer");
         return;
      }
      this.stopBaritone();
      this.target = null;
      this.territory = null;
      this.phase = Phase.SEARCH;
      this.mineTimer.reset();
      this.gotoTimer.reset();
   }

   @Override
   public void onDisable() {
      this.stopBaritone();
      this.target = null;
      this.territory = null;
      super.onDisable();
   }

   @EventTarget
   private void onTick(EventTick event) {
      if (this.baritoneMissing) {
         this.setToggled(false);
         return;
      }
      if (mc.player == null || mc.world == null) {
         return;
      }
      Requirement missing = Arrays.stream(Requirement.values())
            .filter(requirement -> !requirement.isMet())
            .findFirst()
            .orElse(null);
      boolean work = missing == Requirement.TNT && this.phase != Phase.SEARCH;
      if (missing != null && !work) {
         MessageUtil.displayInfo("Для работы модуля " + missing.getDescription() + "!");
         this.setToggled(false);
         return;
      }
      if (!XRay.INSTANCE.isEnabled()) {
         XRay.INSTANCE.setToggled(true);
         return;
      }
      int foodSlot = this.findFoodSlot();
      if (mc.player.getHungerManager().getFoodLevel() <= 17 && foodSlot != -1 && !mc.player.isUsingItem()) {
         this.switchToSlot(foodSlot);
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }
      int potionSlot = this.findFireResistancePotionSlot();
      if (potionSlot != -1 && !mc.player.isUsingItem()
            && (!mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)
                  || mc.player.getStatusEffect(StatusEffects.FIRE_RESISTANCE).getDuration() <= 100)) {
         this.switchToSlot(potionSlot);
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }
      if (mc.player.isUsingItem()) {
         return;
      }
      boolean near = this.territory != null
            && mc.player.getBlockPos().isWithinDistance(BlockPos.ofFloored(this.territory.getCenter()), 2.5);
      boolean primed = !mc.world.getEntitiesByType(net.minecraft.entity.EntityType.TNT,
            mc.player.getBoundingBox().expand(8.0D), tnt -> true).isEmpty();
      switch (this.phase) {
         case SEARCH:
            this.phaseSearch(near);
            break;
         case TNT:
            this.phaseTnt(primed);
            break;
         case RETREAT:
            this.phaseRetreat();
            break;
         case MINE:
            this.phaseMine();
            break;
      }
   }

   private void phaseSearch(boolean near) {
      if (!XRay.INSTANCE.getOres().isEmpty()) {
         MessageUtil.displayInfo("Вскапываем обломки найденные по пути");
         this.phase = Phase.MINE;
      } else if (this.territory == null) {
         if (mc.player.age > 20) {
            MessageUtil.displayInfo("Переходим к поиску новой территории.");
            this.executor.execute(this::findTerritory);
         }
      } else if (near) {
         this.stopBaritone();
         this.phase = Phase.TNT;
      } else if (!BaritoneUtil.isPathing() && this.gotoTimer.finished(3000L)) {
         this.sendGoto(BlockPos.ofFloored(this.territory.getCenter()));
      }
   }

   private void phaseTnt(boolean primed) {
      double reach = mc.player.getBlockInteractionRange();
      BlockPos tnt = BlockPos.streamOutwards(mc.player.getBlockPos(), (int) reach, (int) reach, (int) reach)
            .filter(pos -> mc.world.getBlockState(pos).isOf(Blocks.TNT))
            .map(BlockPos::toImmutable)
            .findFirst()
            .orElse(null);
      if (primed) {
         this.phase = Phase.RETREAT;
         return;
      }
      if (tnt != null) {
         this.target = tnt;
         int flint = this.findHotbarSlot(stack -> stack.isOf(Items.FLINT_AND_STEEL));
         if (flint != -1) {
            Vec3d eye = mc.player.getEyePos();
            Vec3d center = Vec3d.ofCenter(tnt);
            Vec3d aim = Arrays.stream(Direction.values())
                  .map(side -> center.add((double) side.getOffsetX() * 0.45D, (double) side.getOffsetY() * 0.45D, (double) side.getOffsetZ() * 0.45D))
                  .filter(point -> eye.distanceTo(point) <= reach)
                  .filter(point -> {
                     BlockHitResult trace = mc.world.raycast(new RaycastContext(eye, point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
                     return trace.getType() == HitResult.Type.BLOCK && trace.getBlockPos().equals(tnt);
                  })
                  .min((a, b) -> Double.compare(eye.squaredDistanceTo(a), eye.squaredDistanceTo(b)))
                  .orElse(null);
            if (aim != null) {
               Rotation rotation = Rotation.lookingAt(aim, eye);
               RotationComponent.update(rotation, 180.0F, 180.0F, 360.0F, 360.0F, 0, 1, false);
               if (this.currentAngleTo(rotation) < 1.0D && mc.player.age % 5 == 0) {
                  this.switchToSlot(flint);
                  if (mc.crosshairTarget instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(tnt)) {
                     mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                  }
               }
            }
         }
         return;
      }
      Vec3d eye = mc.player.getEyePos();
      BlockPos placeTarget = BlockPos.streamOutwards(mc.player.getBlockPos(), (int) reach, (int) reach, (int) reach)
            .filter(pos -> mc.world.getBlockState(pos).isReplaceable())
            .filter(pos -> mc.world.getBlockState(pos.down()).isSolidBlock(mc.world, pos.down()))
            .filter(pos -> !mc.player.getBoundingBox().stretch(mc.player.getVelocity()).expand(0.1D).intersects(new Box(pos)))
            .filter(pos -> {
               Vec3d hitVec = new Vec3d((double) pos.getX() + 0.5D, pos.getY(), (double) pos.getZ() + 0.5D);
               if (eye.distanceTo(hitVec) > reach || eye.subtract(hitVec).normalize().y <= 0.0D) {
                  return false;
               }
               BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, hitVec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
               return hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(pos.down());
            })
            .map(BlockPos::toImmutable)
            .min((a, b) -> Double.compare(eye.squaredDistanceTo(a.toCenterPos()), eye.squaredDistanceTo(b.toCenterPos())))
            .orElse(null);
      this.target = placeTarget;
      int tntSlot = this.findHotbarSlot(stack -> stack.isOf(Items.TNT));
      if (placeTarget != null && tntSlot != -1) {
         BlockPos support = placeTarget.down();
         Vec3d aim = new Vec3d((double) support.getX() + 0.5D, support.getY() + 1, (double) support.getZ() + 0.5D);
         Rotation rotation = Rotation.lookingAt(aim, eye);
         RotationComponent.update(rotation, 180.0F, 180.0F, 360.0F, 360.0F, 0, 1, false);
         if (this.currentAngleTo(rotation) < 1.0D && mc.player.age % 5 == 0) {
            this.switchToSlot(tntSlot);
            if (mc.crosshairTarget instanceof BlockHitResult hit
                  && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(support)
                  && hit.getSide() == Direction.UP) {
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            }
         }
      } else if (!BaritoneUtil.isPathing() && this.gotoTimer.finished(3000L)) {
         BlockPos feet = mc.player.getBlockPos();
         BlockPos stand = BlockPos.streamOutwards(feet, 16, 8, 16)
               .filter(pos -> !pos.equals(feet))
               .filter(pos -> mc.world.getBlockState(pos.down()).isSolidBlock(mc.world, pos.down()))
               .filter(pos -> mc.world.getBlockState(pos).isReplaceable() && mc.world.getBlockState(pos.up()).isReplaceable())
               .filter(pos -> mc.world.getBlockState(pos).getFluidState().isEmpty() && mc.world.getBlockState(pos.up()).getFluidState().isEmpty())
               .map(BlockPos::toImmutable)
               .min((a, b) -> Double.compare(feet.getSquaredDistance(a), feet.getSquaredDistance(b)))
               .orElse(null);
         if (stand != null) {
            this.sendGoto(stand);
         } else {
            this.phase = Phase.SEARCH;
         }
      }
   }

   private void phaseRetreat() {
      List<TntEntity> burning = mc.world.getEntitiesByType(net.minecraft.entity.EntityType.TNT,
            mc.player.getBoundingBox().expand(32.0D), tnt -> true);
      if (burning.isEmpty()) {
         MessageUtil.displayInfo("Ожидаем обломки, и начинаем вскапывать");
         this.stopBaritone();
         this.territory = null;
         this.phase = Phase.MINE;
         this.mineTimer.reset();
      } else if (!BaritoneUtil.isPathing() && this.gotoTimer.finished(3000L)) {
         Vec3d player = mc.player.getPos();
         Vec3d away = burning.stream()
               .map(tnt -> tnt.getPos())
               .reduce(Vec3d.ZERO, Vec3d::add)
               .multiply(1.0D / (double) burning.size());
         Vec3d dir = player.subtract(away).normalize();
         this.sendGoto(BlockPos.ofFloored(player.x + dir.x * 24.0D, player.y + dir.y * 8.0D, player.z + dir.z * 24.0D));
      }
   }

   private void phaseMine() {
      if (!this.mineTimer.finished(1000L)) {
         return;
      }
      if (!XRay.INSTANCE.getOres().isEmpty()) {
         if (!BaritoneUtil.isPathing()) {
            mc.player.networkHandler.sendChatMessage("#mine ancient_debris");
            this.gotoTimer.reset();
         }
      } else if (!BaritoneUtil.isPathing()) {
         this.phase = Phase.SEARCH;
      }
   }

   private void findTerritory() {
      if (this.territory != null || mc.player == null || mc.world == null) {
         return;
      }
      int reach = (int) ((Math.sqrt(mc.world.getChunkManager().getLoadedChunkCount()) - 1.0D) / 2.0D) * 16;
      BlockPos feet = mc.player.getBlockPos();
      WorldBorder border = mc.world.getWorldBorder();
      boolean top = this.searchMode.getValue().getName().equals("Поиск сверху");
      int bottom = top ? 90 : 20;
      int topY = top ? mc.world.getTopYInclusive() - 1 : 60;
      Box best = null;
      double bestScore = -1.0D;
      double bestFill = 0.0D;
      BlockPos.Mutable pos = new BlockPos.Mutable();
      for (int x = feet.getX() - reach; x <= feet.getX() + reach; x += 8) {
         for (int z = feet.getZ() - reach; z <= feet.getZ() + reach; z += 8) {
            if (mc.world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)
                  && border.contains((double) x - 20.0D, (double) z - 20.0D)
                  && border.contains((double) x + 20.0D, (double) z + 20.0D)) {
               for (int y = bottom; y <= topY; y += 8) {
                  int solid = 0;
                  int total = 0;
                  boolean badBiome = false;
                  for (int dx = -20; dx <= 20 && !badBiome; dx += 4) {
                     for (int dy = -20; dy <= 20 && !badBiome; dy += 4) {
                        for (int dz = -20; dz <= 20; dz += 4) {
                           pos.set(x + dx, y + dy, z + dz);
                           if (mc.world.getBiome(pos).matchesKey(BiomeKeys.BASALT_DELTAS)
                                 || mc.world.getBiome(pos).matchesKey(BiomeKeys.WARPED_FOREST)) {
                              badBiome = true;
                              break;
                           }
                           total++;
                           if (!mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos).getFluidState().isEmpty()) {
                              solid++;
                           }
                        }
                     }
                  }
                  if (!badBiome && total > 0) {
                     double score = ((double) solid / (double) total) - feet.getSquaredDistance(x, y, z) * 1.0E-9D;
                     if (score > bestScore) {
                        BlockPos anchor = BlockPos.streamOutwards(new BlockPos(x, y, z), 20, 20, 20)
                              .filter(candidate -> !mc.world.getBlockState(candidate).isAir()
                                    && mc.world.getBlockState(candidate).getFluidState().isEmpty())
                              .map(BlockPos::toImmutable)
                              .findFirst()
                              .orElse(null);
                        if (anchor != null) {
                           bestScore = score;
                           bestFill = (double) solid / (double) total;
                           best = new Box(anchor.getX() - 20, anchor.getY() - 20, anchor.getZ() - 20,
                                 anchor.getX() + 20, anchor.getY() + 20, anchor.getZ() + 20);
                        }
                     }
                  }
               }
            }
         }
      }
      if (best != null) {
         long percent = Math.round(bestFill * 100.0D);
         MessageUtil.displayInfo("Успешность: " + percent + "%, до неё " + percent + " блоков");
      }
      this.territory = best;
   }

   @EventTarget
   private void onRender3D(EventRender3D event) {
      if (this.target != null) {
         Render3DUtil.drawBox(new Box(this.target), 0x46E55A46, 1.0F, true, false, false);
      }
   }

   private void sendGoto(BlockPos pos) {
      mc.player.networkHandler.sendChatMessage("#goto " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
      this.gotoTimer.reset();
   }

   private void stopBaritone() {
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatMessage("#stop");
      }
   }

   private void switchToSlot(int slot) {
      if (slot < 0 || slot > 8) {
         return;
      }
      if (mc.player.getInventory().selectedSlot != slot) {
         mc.player.getInventory().selectedSlot = slot;
         mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(slot));
      }
   }

   private int findHotbarSlot(Predicate<ItemStack> predicate) {
      return IntStream.range(0, 9)
            .filter(slot -> predicate.test(mc.player.getInventory().getStack(slot)))
            .findFirst()
            .orElse(-1);
   }

   private int findFoodSlot() {
      return this.findHotbarSlot(stack -> stack.contains(DataComponentTypes.FOOD));
   }

   private int findFireResistancePotionSlot() {
      return this.findHotbarSlot(stack -> StreamSupport.stream(
            stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
                  .getEffects().spliterator(), false).anyMatch(this::isFireResistance));
   }

   private boolean isFireResistance(StatusEffectInstance effect) {
      return effect.getEffectType() == StatusEffects.FIRE_RESISTANCE;
   }

   private double currentAngleTo(Rotation targetRotation) {
      return new Rotation(mc.player.getYaw(), mc.player.getPitch()).angleTo(targetRotation);
   }

   private enum Phase {
      SEARCH,
      TNT,
      RETREAT,
      MINE
   }

   private enum Requirement {
      FLY("необходимо включить режим полёта (/fly)", () -> mc.player.getAbilities().allowFlying),
      NETHER("необходимо находиться в Незере", () -> mc.world.getRegistryKey() == World.NETHER),
      FOOD("в хотбаре должна быть еда", stack -> stack.contains(DataComponentTypes.FOOD)),
      TNT("в хотбаре должен быть динамит", stack -> stack.isOf(Items.TNT)),
      FLINT_AND_STEEL("в хотбаре должно быть огниво", stack -> stack.isOf(Items.FLINT_AND_STEEL)),
      PICKAXE("в хотбаре должна быть кирка с прочностью больше 5%", stack -> stack.getItem() instanceof PickaxeItem
            && (double) (stack.getMaxDamage() - stack.getDamage()) > (double) stack.getMaxDamage() * 0.05D),
      FIRE_RESISTANCE("в хотбаре должна быть огнестойкость", stack -> StreamSupport.stream(
            stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT)
                  .getEffects().spliterator(), false).anyMatch(effect -> effect.getEffectType() == StatusEffects.FIRE_RESISTANCE));

      private final String description;
      private final BooleanSupplier requirement;

      Requirement(String description, BooleanSupplier requirement) {
         this.description = description;
         this.requirement = requirement;
      }

      Requirement(String description, Predicate<ItemStack> predicate) {
         this(description, () -> IntStream.range(0, 9)
               .anyMatch(slot -> predicate.test(mc.player.getInventory().getStack(slot))));
      }

      boolean isMet() {
         if (this == FLY && !AncientFarmer.INSTANCE.useFly.isEnabled()) {
            return true;
         }
         return this.requirement.getAsBoolean();
      }

      String getDescription() {
         return this.description;
      }
   }
}
