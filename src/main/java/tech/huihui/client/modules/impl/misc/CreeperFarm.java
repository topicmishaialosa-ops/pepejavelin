package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Random;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "CreeperFarm",
   category = Category.MISC,
   description = "Фарм-платформа: патрулирует, бежит к криперам, бьёт мечом, отбегает от взрывов"
)
public final class CreeperFarm extends Module implements IMinecraft {
   public static final CreeperFarm INSTANCE = new CreeperFarm();

   private final NumberSetting distance = new NumberSetting("Дистанция поиска", 20.0F, 5.0F, 50.0F, 1.0F);
   private final NumberSetting attackRange = new NumberSetting("Дистанция атаки", 3.2F, 1.0F, 6.0F, 0.1F);
   private final BooleanSetting autoAttack = new BooleanSetting("Бить мечом", true);
   private final BooleanSetting autoSword = new BooleanSetting("Брать меч", true);
   private final BooleanSetting critAttack = new BooleanSetting("Крит-удары", false);
   private final BooleanSetting runFromBlast = new BooleanSetting("Отбегать от взрыва", true);
   private final NumberSetting blastRadius = new NumberSetting("Радиус взрыва", 5.0F, 2.0F, 10.0F, 0.5F);
   private final BooleanSetting sprint = new BooleanSetting("Бег", true);
   private final BooleanSetting wander = new BooleanSetting("Патруль без цели", true);
   private final NumberSetting wanderRadius = new NumberSetting("Радиус патруля", 25.0F, 5.0F, 50.0F, 1.0F);

   private final Random random = new Random();
   private final Timer attackTimer = new Timer();
   private final Timer wanderTimer = new Timer();
   private CreeperEntity target;
   private CreeperEntity exploding;
   private float wanderYaw;
   private double centerX;
   private double centerZ;

   private CreeperFarm() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.target = null;
      this.exploding = null;
      this.attackTimer.reset();
      this.wanderTimer.reset();
      this.wanderYaw = mc.player == null ? 0.0F : mc.player.getYaw();
      if (mc.player != null) {
         this.centerX = mc.player.getX();
         this.centerZ = mc.player.getZ();
      }
   }

   @Override
   public void onDisable() {
      this.setKey(mc.options.forwardKey, false);
      this.setKey(mc.options.jumpKey, false);
      this.setKey(mc.options.sprintKey, false);
      super.onDisable();
   }

   @EventTarget
   private void onTick(EventTick e) {
      if (mc.world == null || mc.player == null) {
         this.releaseKeys();
         return;
      }

      this.updateCreeper();
      this.ensureSword();

      if (this.exploding != null && this.runFromBlast.isEnabled()) {
         this.flee();
         return;
      }

      if (this.target == null) {
         this.wander();
         return;
      }

      this.face(this.target);
      double meleeRange = this.attackRange.getCurrent();
      double distSq = mc.player.squaredDistanceTo(this.target);

      if (distSq > meleeRange * meleeRange) {
         this.setKey(mc.options.forwardKey, true);
         this.setSprinting(true);
         if (mc.player.horizontalCollision && mc.player.isOnGround()) {
            this.setKey(mc.options.jumpKey, true);
         } else {
            this.setKey(mc.options.jumpKey, false);
         }
      } else {
         this.setKey(mc.options.forwardKey, false);
         this.setKey(mc.options.sprintKey, false);
         this.attack(this.target);
      }
   }

   private void updateCreeper() {
      double searchSq = this.distance.getCurrent() * this.distance.getCurrent();
      double blastSq = this.blastRadius.getCurrent() * this.blastRadius.getCurrent();
      CreeperEntity nearest = null;
      CreeperEntity blast = null;
      double nearestSq = Double.MAX_VALUE;

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof CreeperEntity creeper)) {
            continue;
         }
         if (!creeper.isAlive()) {
            continue;
         }
         double d = mc.player.squaredDistanceTo(creeper);
         boolean fusing = creeper.isIgnited() || creeper.getFuseSpeed() > 0;
         if (fusing && d < blastSq) {
            if (blast == null || d < mc.player.squaredDistanceTo(blast)) {
               blast = creeper;
            }
         }
         if (d < searchSq && d < nearestSq) {
            nearestSq = d;
            nearest = creeper;
         }
      }
      this.exploding = blast;
      this.target = nearest;
   }

   private void ensureSword() {
      if (!this.autoSword.isEnabled()) {
         return;
      }
      if (this.isSword(mc.player.getMainHandStack().getItem())) {
         return;
      }
      for (int i = 0; i < 9; i++) {
         Item item = mc.player.getInventory().getStack(i).getItem();
         if (this.isSword(item)) {
            mc.player.getInventory().selectedSlot = i;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
            return;
         }
      }
   }

   private void face(Entity entity) {
      double dx = entity.getX() - mc.player.getX();
      double dz = entity.getZ() - mc.player.getZ();
      float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      yaw = MathHelper.wrapDegrees(yaw);
      mc.player.setYaw(yaw);
      mc.player.setHeadYaw(yaw);
      mc.player.setBodyYaw(yaw);
   }

   private void attack(LivingEntity entity) {
      if (!this.autoAttack.isEnabled()) {
         return;
      }
      if (mc.player.getAttackCooldownProgress(0.5F) < 0.9F) {
         return;
      }
      if (this.critAttack.isEnabled() && mc.player.isOnGround()) {
         this.setKey(mc.options.jumpKey, true);
         return;
      }
      this.setKey(mc.options.jumpKey, false);
      mc.interactionManager.attackEntity(mc.player, entity);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.attackTimer.reset();
   }

   private boolean isSword(Item item) {
      return item == Items.WOODEN_SWORD
            || item == Items.STONE_SWORD
            || item == Items.IRON_SWORD
            || item == Items.GOLDEN_SWORD
            || item == Items.DIAMOND_SWORD
            || item == Items.NETHERITE_SWORD;
   }

   private void flee() {
      if (this.exploding == null) {
         return;
      }
      double dx = mc.player.getX() - this.exploding.getX();
      double dz = mc.player.getZ() - this.exploding.getZ();
      double len = Math.sqrt(dx * dx + dz * dz);
      if (len < 1.0E-4) {
         dx = 1.0;
         dz = 0.0;
      } else {
         dx /= len;
         dz /= len;
      }
      float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      yaw = MathHelper.wrapDegrees(yaw);
      mc.player.setYaw(yaw);
      mc.player.setHeadYaw(yaw);
      mc.player.setBodyYaw(yaw);
      this.setKey(mc.options.forwardKey, true);
      this.setSprinting(true);
      if (mc.player.horizontalCollision && mc.player.isOnGround()) {
         this.setKey(mc.options.jumpKey, true);
      } else {
         this.setKey(mc.options.jumpKey, false);
      }
   }

   private void wander() {
      if (!this.wander.isEnabled()) {
         this.releaseKeys();
         return;
      }
      double radius = this.wanderRadius.getCurrent();
      double distFromCenter = Math.sqrt((mc.player.getX() - this.centerX) * (mc.player.getX() - this.centerX) + (mc.player.getZ() - this.centerZ) * (mc.player.getZ() - this.centerZ));
      if (this.wanderTimer.finished(3000L) || this.wanderTimer.getElapsedTime() == 0L || distFromCenter > radius) {
         this.wanderTimer.reset();
         float angle = this.random.nextFloat() * 6.2831855F;
         double targetX = this.centerX + this.random.nextDouble() * radius * 0.5D;
         double targetZ = this.centerZ + this.random.nextDouble() * radius * 0.5D;
         if (this.random.nextBoolean()) {
            targetX = this.centerX - targetX + this.centerX;
         }
         if (this.random.nextBoolean()) {
            targetZ = this.centerZ - targetZ + this.centerZ;
         }
         double ddx = targetX - mc.player.getX();
         double ddz = targetZ - mc.player.getZ();
         this.wanderYaw = (float) (Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0);
         this.wanderYaw = MathHelper.wrapDegrees(this.wanderYaw);
      }
      if (mc.player.horizontalCollision && mc.player.isOnGround()) {
         this.wanderTimer.reset();
         this.wanderYaw = this.random.nextFloat() * 360.0F;
      }
      mc.player.setYaw(this.wanderYaw);
      mc.player.setHeadYaw(this.wanderYaw);
      mc.player.setBodyYaw(this.wanderYaw);
      this.setKey(mc.options.forwardKey, true);
      this.setSprinting(true);
      if (mc.player.horizontalCollision && mc.player.isOnGround()) {
         this.setKey(mc.options.jumpKey, true);
      } else {
         this.setKey(mc.options.jumpKey, false);
      }
   }

   private void setSprinting(boolean sprinting) {
      if (!this.sprint.isEnabled()) {
         this.setKey(mc.options.sprintKey, false);
         return;
      }
      this.setKey(mc.options.sprintKey, sprinting);
      if (sprinting) {
         mc.player.setSprinting(mc.player.isWalking() && mc.player.canSprint() && !mc.player.horizontalCollision && mc.player.isOnGround());
      }
   }

   private void releaseKeys() {
      this.setKey(mc.options.forwardKey, false);
      this.setKey(mc.options.jumpKey, false);
      this.setKey(mc.options.sprintKey, false);
   }

   private void setKey(KeyBinding key, boolean pressed) {
      key.setPressed(pressed);
   }
}