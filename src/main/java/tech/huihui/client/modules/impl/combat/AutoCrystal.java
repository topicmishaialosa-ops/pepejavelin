package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionImpl;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.component.RotationComponent;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.render.display.Keyboard;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "AutoCrystal",
   category = Category.COMBAT,
   description = "Автокристалл с расчётом урона"
)
public class AutoCrystal extends Module {

   public static final AutoCrystal INSTANCE = new AutoCrystal();

   public final ModeSetting page = new ModeSetting("Page", "Main", "Place", "Break", "Damages", "Pause", "Switch", "FailSafe", "Render", "Info", "IDPredict");

   public final BooleanSetting await = new BooleanSetting("Await", true, () -> this.page.is("Main"));
   public final ModeSetting timing = new ModeSetting("Timing", () -> this.page.is("Main"), "NORMAL", "SEQUENTIAL");
   public final ModeSetting sequential = new ModeSetting("Sequential", () -> this.page.is("Main"), "Off", "Strict", "Strong");
   public final ModeSetting rotate = new ModeSetting("Rotate", () -> this.page.is("Main"), "Off", "Instant", "Smooth", "Stepped");
   public final BooleanSetting yawStep = new BooleanSetting("YawStep", false, () -> !this.rotate.is("Off") && this.page.is("Main"));
   public final NumberSetting yawAngle = new NumberSetting("YawAngle", 180.0F, 1.0F, 180.0F, 1.0F, () -> !this.rotate.is("Off") && this.yawStep.isEnabled() && this.page.is("Main"));
   public final ModeSetting targetLogic = new ModeSetting("TargetLogic", () -> this.page.is("Main"), "Distance", "Health");
   public final NumberSetting targetRange = new NumberSetting("TargetRange", 10.0F, 1.0F, 15.0F, 0.1F, () -> this.page.is("Main"));

   public final ModeSetting interact = new ModeSetting("Interact", () -> this.page.is("Place"), "Default", "Strict");
   public final BooleanSetting strictCenter = new BooleanSetting("CCStrict", true, () -> this.page.is("Place") && this.interact.is("Strict"));
   public final BooleanSetting rayTraceBypass = new BooleanSetting("RayTraceBypass", false, () -> this.page.is("Place"));
   public final NumberSetting placeDelay = new NumberSetting("PlaceDelay", 0.0F, 0.0F, 20.0F, 1.0F, () -> this.page.is("Place"));
   public final NumberSetting placeRange = new NumberSetting("PlaceRange", 5.0F, 1.0F, 6.0F, 0.1F, () -> this.page.is("Place"));
   public final NumberSetting placeWallRange = new NumberSetting("PlaceWallRange", 3.5F, 0.0F, 6.0F, 0.1F, () -> this.page.is("Place"));

   public final BooleanSetting inhibit = new BooleanSetting("Inhibit", true, () -> this.page.is("Break"));
   public final NumberSetting breakDelay = new NumberSetting("BreakDelay", 0.0F, 0.0F, 20.0F, 1.0F, () -> this.page.is("Break"));
   public final NumberSetting explodeRange = new NumberSetting("BreakRange", 5.0F, 1.0F, 6.0F, 0.1F, () -> this.page.is("Break"));
   public final NumberSetting explodeWallRange = new NumberSetting("BreakWallRange", 3.5F, 0.0F, 6.0F, 0.1F, () -> this.page.is("Break"));

   public final BooleanSetting mining = new BooleanSetting("Mining", true, () -> this.page.is("Pause"));
   public final BooleanSetting eating = new BooleanSetting("Eating", true, () -> this.page.is("Pause"));
   public final BooleanSetting surround = new BooleanSetting("Surround", true, () -> this.page.is("Pause"));
   public final BooleanSetting inventoryPause = new BooleanSetting("Inventory", false, () -> this.page.is("Pause"));
   public final NumberSetting pauseHP = new NumberSetting("HP", 8.0F, 2.0F, 10.0F, 0.1F, () -> this.page.is("Pause"));

   public final NumberSetting minDamage = new NumberSetting("MinDamage", 6.0F, 2.0F, 20.0F, 0.1F, () -> this.page.is("Damages"));
   public final NumberSetting maxSelfDamage = new NumberSetting("MaxSelfDamage", 10.0F, 2.0F, 20.0F, 0.1F, () -> this.page.is("Damages"));
   public final BooleanSetting efficiency = new BooleanSetting("Efficiency", false, () -> this.page.is("Damages"));
   public final NumberSetting efficiencyFactor = new NumberSetting("EfficiencyFactor", 1.0F, 0.1F, 5.0F, 0.1F, () -> this.page.is("Damages") && this.efficiency.isEnabled());
   public final BooleanSetting protectFriends = new BooleanSetting("ProtectFriends", true, () -> this.page.is("Damages"));
   public final BooleanSetting overrideSelfDamage = new BooleanSetting("OverrideSelfDamage", true, () -> this.page.is("Damages"));
   public final BooleanSetting sacrificeTotem = new BooleanSetting("SacrificeTotem", true, () -> this.page.is("Damages") && this.overrideSelfDamage.isEnabled());
   public final BooleanSetting armorBreaker = new BooleanSetting("ArmorBreaker", true, () -> this.page.is("Damages"));
   public final NumberSetting armorScale = new NumberSetting("Armor %", 5.0F, 0.0F, 40.0F, 0.1F, () -> this.page.is("Damages") && this.armorBreaker.isEnabled());
   public final NumberSetting facePlaceHp = new NumberSetting("FacePlaceHp", 5.0F, 0.0F, 20.0F, 0.1F, () -> this.page.is("Damages"));
   public final KeySetting facePlaceButton = new KeySetting("FacePlaceBtn", -1, () -> this.page.is("Damages"));
   public final BooleanSetting ignoreTerrain = new BooleanSetting("IgnoreTerrain", true, () -> this.page.is("Damages"));

   public final ModeSetting autoSwitch = new ModeSetting("Switch", () -> this.page.is("Switch"), "NONE", "NORMAL", "SILENT", "INVENTORY");
   public final ModeSetting antiWeakness = new ModeSetting("AntiWeakness", () -> this.page.is("Switch"), "NONE", "NORMAL", "SILENT", "INVENTORY");

   public final BooleanSetting placeFailsafe = new BooleanSetting("PlaceFailsafe", true, () -> this.page.is("FailSafe"));
   public final BooleanSetting breakFailsafe = new BooleanSetting("BreakFailsafe", true, () -> this.page.is("FailSafe"));
   public final NumberSetting attempts = new NumberSetting("MaxAttempts", 5.0F, 1.0F, 30.0F, 1.0F, () -> this.page.is("FailSafe"));

   public final BooleanSetting idPredict = new BooleanSetting("IDPredict", false, () -> this.page.is("IDPredict"));
   public final NumberSetting idAttacks = new NumberSetting("IDAttacks", 3.0F, 1.0F, 10.0F, 1.0F, () -> this.page.is("IDPredict"));

   public final ModeSetting swingMode = new ModeSetting("Swing", () -> this.page.is("Render"), "Both", "Place", "Break", "ServerSide");
   public final MultiBooleanSetting renderSettings = MultiBooleanSetting.create("Render", List.of("Block", "Rect", "Satin outline", "Animate"));
   public final NumberSetting renderHoldMs = new NumberSetting("RenderHoldMs", 500.0F, 100.0F, 1000.0F, 10.0F, () -> this.page.is("Render"));

   private final CrystalTracker crystalTracker = new CrystalTracker();
   private final Map<BlockPos, Long> renderPositions = new ConcurrentHashMap<>();

   private PlayerEntity target;
   private PlaceData currentData;
   private BlockHitResult bestPosition;
   private EndCrystalEntity bestCrystal;
   private EndCrystalEntity secondaryCrystal;
   private RotationVec rotationVec;
   private State currentState = State.NoTarget;

   private float renderDamage;
   private float renderSelfDamage;

   private int rotationTicks;

   private boolean rotated;
   private boolean rotating;
   private boolean facePlacing;
   private boolean placedOnSpawn;
   private boolean initialized;

   private long currentId;
   private int lastTargetId = -1;

   private int placeTicks;
   private int breakTicks;
   private int calcTicks;
   private int placeSyncTicks;

   private AutoCrystal() {
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.reset();
   }

   @EventTarget
   @Native
   public void onPacket(EventPacket event) {
      if (!event.isReceive() || mc.player == null || mc.world == null) {
         return;
      }

      if (event.getPacket() instanceof ExperienceOrbSpawnS2CPacket spawn) {
         this.processSpawnPacket(spawn.getEntityId());
      } else if (event.getPacket() instanceof EntitySpawnS2CPacket spawn) {
         this.processSpawnPacket(spawn.getEntityId());
         this.confirmAwaitingBySpawn(spawn.getEntityId(), this.resolveEntitySpawnPos(spawn));
      } else if (event.getPacket() instanceof ExplosionS2CPacket explosion) {
         Vec3d explosionPos = this.resolveExplosionPos(explosion);
         for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof EndCrystalEntity crystal)) {
               continue;
            }
            if (crystal.squaredDistanceTo(explosionPos) <= 144.0 && !this.crystalTracker.isDead(crystal.getId())) {
               this.crystalTracker.setDead(crystal.getId(), System.currentTimeMillis());
            }
         }
      }
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }

      if (!this.initialized) {
         this.reset();
         this.initialized = true;
      }

      this.tickTimers();
      this.crystalTracker.update();
      this.cleanupRenderPositions();
      this.processAwaitingSpawnConfirm();

      this.target = this.findTarget(this.targetRange.getCurrent(), this.targetLogic.get());
      if (this.target != null && (!this.target.isAlive() || this.target.getHealth() <= 0.0F)) {
         this.target = null;
         this.currentState = State.NoTarget;
         this.lastTargetId = -1;
         this.clearCombatContext();
         return;
      }

      if (this.target == null) {
         this.currentState = State.NoTarget;
         this.lastTargetId = -1;
         this.clearCombatContext();
         return;
      }

      if (this.passedTicks(this.placeTicks, 20)) {
         this.renderDamage = 0.0F;
      }

      this.calcRotations();

      boolean targetChanged = this.lastTargetId != this.target.getId();
      if (targetChanged) {
         this.lastTargetId = this.target.getId();
         this.calcTicks = Integer.MAX_VALUE;
      }

      int pingGate = Math.max(1, Math.round(this.getPing() / 25f));
      if (targetChanged || this.bestPosition == null || !this.await.isEnabled() || this.passedTicks(this.placeTicks, 20) || this.passedTicks(this.calcTicks, pingGate)) {
         this.calcPosition(this.placeRange.getCurrent(), mc.player.getPos());
      }

      this.getCrystalToExplode();

      if (this.timing.is("NORMAL") || this.timing.is("SEQUENTIAL")) {
         this.doAction();
      }
   }

   @EventTarget
   @Native
   public void onRender3D(EventRender3D event) {
      this.cleanupRenderPositions();

      long now = System.currentTimeMillis();
      float holdMs = this.renderHoldMs.getCurrent();

      for (Map.Entry<BlockPos, Long> entry : new ArrayList<>(this.renderPositions.entrySet())) {
         BlockPos pos = entry.getKey();
         long elapsed = now - entry.getValue();

         if (elapsed > (long)holdMs) {
            continue;
         }

         float fade = 1.0F - MathHelper.clamp((float)elapsed / holdMs, 0.0F, 1.0F);
         if (!this.render("Animate")) {
            fade = 1.0F;
         }

         Box box = new Box(pos);
         if (this.render("Block")) {
            Render3DUtil.drawBox(box, this.withAlpha(0x3A98FF, 56.0F * fade), 1.15F, true, false, false);
         }
         if (this.render("Rect")) {
            Render3DUtil.drawBox(box, this.withAlpha(0x3A98FF, 160.0F * fade), 1.95F, false, true, false);
         }
      }
   }

   private int withAlpha(int color, float alpha) {
      int a = (int)(255.0F * MathHelper.clamp(alpha, 0.0F, 1.0F));
      return a << 24 | color & 0x00FFFFFF;
   }

   public void reset() {
      this.facePlacing = false;
      this.rotated = false;
      this.rotating = false;
      this.renderDamage = 0.0F;
      this.renderSelfDamage = 0.0F;
      this.currentId = 0L;
      this.lastTargetId = -1;

      this.placeTicks = 0;
      this.breakTicks = 0;
      this.calcTicks = 0;
      this.placeSyncTicks = 0;
      this.rotationTicks = 0;

      this.renderPositions.clear();
      this.crystalTracker.reset();
      this.bestCrystal = null;
      this.bestPosition = null;
      this.secondaryCrystal = null;
      this.currentData = null;
      this.target = null;
      this.rotationVec = null;
      this.currentState = State.NoTarget;
      this.placedOnSpawn = false;
      this.initialized = false;
   }

   private void tickTimers() {
      this.placeTicks++;
      this.breakTicks++;
      this.calcTicks++;
      this.placeSyncTicks++;
   }

   private void doAction() {
      if (this.target == null) {
         return;
      }

      if (this.sequential.is("Off")) {
         if (this.bestCrystal != null && this.passedTicks(this.breakTicks, (int)this.breakDelay.getCurrent())) {
            this.attackCrystal(this.bestCrystal);
         } else if (this.bestPosition != null && this.passedTicks(this.placeTicks, (int)this.placeDelay.getCurrent()) && !this.placedOnSpawn) {
            this.placeCrystal(this.bestPosition, false, false);
         }
      } else {
         if (this.bestCrystal != null && this.passedTicks(this.breakTicks, (int)this.breakDelay.getCurrent())) {
            this.attackCrystal(this.bestCrystal);
         }
         if (this.bestPosition != null && this.passedTicks(this.placeTicks, (int)this.placeDelay.getCurrent()) && !this.placedOnSpawn) {
            this.placeCrystal(this.bestPosition, false, false);
         }
      }
      this.placedOnSpawn = false;
   }

   private void clearCombatContext() {
      this.bestCrystal = null;
      this.bestPosition = null;
      this.secondaryCrystal = null;
      this.currentData = null;
      this.rotationVec = null;
      this.facePlacing = false;
   }

   private void processAwaitingSpawnConfirm() {
      if (mc.player == null || mc.world == null) {
         return;
      }

      Map<BlockPos, CrystalTracker.Attempt> awaiting = this.crystalTracker.getAwaitingPositions();
      if (awaiting.isEmpty()) {
         return;
      }

      for (Map.Entry<BlockPos, CrystalTracker.Attempt> entry : new ArrayList<>(awaiting.entrySet())) {
         BlockPos bp = entry.getKey();

         Box searchBox = new Box(bp.up()).expand(0.9, 0.8, 0.9);
         List<EndCrystalEntity> nearby = mc.world.getEntitiesByClass(
               EndCrystalEntity.class,
               searchBox,
               entity -> entity != null && entity.isAlive() && entity.squaredDistanceTo(bp.toCenterPos()) < 0.3
         );
         if (nearby.isEmpty()) {
            continue;
         }

         EndCrystalEntity crystal = nearby.get(0);
         this.crystalTracker.confirmSpawn(bp);

         if (this.passedTicks(this.breakTicks, (int)this.breakDelay.getCurrent())) {
            this.handleSpawn(crystal);
         }
      }
   }

   private void handleSpawn(EndCrystalEntity crystal) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (!this.canAttackCrystal(crystal)) {
         return;
      }

      this.attackCrystal(crystal);

      if (this.sequential.is("Strong") && this.passedTicks(this.placeTicks, (int)this.placeDelay.getCurrent())) {
         this.calcPosition(this.placeRange.getCurrent(), mc.player.getPos());
         if (this.bestPosition != null) {
            this.placeCrystal(this.bestPosition, false, true);
         }
      }
   }

   private void calcRotations() {
      if (this.rotate.is("Off") || this.shouldPause() || mc.player == null) {
         this.rotated = true;
         this.rotating = false;
         return;
      }

      Vec3d vec = null;
      if (this.rotationVec != null) {
         vec = this.rotationVec.hitVec() == null ? this.rotationVec.vec() : this.rotationVec.hitVec().getPos();
      } else if (this.bestPosition != null) {
         vec = this.bestPosition.getPos();
      } else if (this.bestCrystal != null) {
         vec = this.bestCrystal.getPos();
      }

      if (vec == null) {
         this.rotated = true;
         this.rotating = false;
         return;
      }

      Rotation desired = Rotation.lookingAt(vec, mc.player.getEyePos());
      Rotation limited = this.limitYaw(desired);
      this.applyRotation(vec, limited);
      this.rotated = this.isRotationAligned(desired);
      this.rotating = true;

      if (this.rotationVec != null && this.rotationTicks-- < 0) {
         this.rotationVec = null;
      }
   }

   private Rotation limitYaw(Rotation desired) {
      if (!this.yawStep.isEnabled()) {
         return desired;
      }

      float baseYaw = mc.player.getYaw();
      float deltaYaw = MathHelper.wrapDegrees(desired.getYaw() - baseYaw);
      float limitedDelta = MathHelper.clamp(deltaYaw, -this.yawAngle.getCurrent(), this.yawAngle.getCurrent());
      return new Rotation(baseYaw + limitedDelta, desired.getPitch());
   }

   private boolean isRotationAligned(Rotation targetRotation) {
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();

      float deltaYaw = Math.abs(MathHelper.wrapDegrees(targetRotation.getYaw() - yaw));
      float deltaPitch = Math.abs(MathHelper.wrapDegrees(targetRotation.getPitch() - pitch));
      return deltaYaw <= 8.0f && deltaPitch <= 8.0f;
   }

   private void applyRotation(Vec3d vec, Rotation desired) {
      if (this.rotate.is("Off")) {
         return;
      }

      float yawSpeed;
      int timeout;
      if (this.rotate.is("Instant")) {
         yawSpeed = 180.0F;
         timeout = 1;
      } else if (this.rotate.is("Stepped")) {
         yawSpeed = 60.0F;
         timeout = 2;
      } else {
         yawSpeed = 20.0F;
         timeout = 4;
      }

      RotationComponent.update(desired, yawSpeed, yawSpeed, yawSpeed, yawSpeed, 6, timeout, false);
   }

   private void attackCrystal(EndCrystalEntity crystal) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || crystal == null) {
         return;
      }

      if ((this.crystalTracker.isDead(crystal.getId()) && this.inhibit.isEnabled()) || this.shouldPause() || this.target == null) {
         return;
      }

      StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
      StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);

      int prevSlot = -1;
      HotbarSearch antiWeaknessHotbar = this.findAntiWeaknessHotbar();
      int antiWeaknessInv = this.findAntiWeaknessInventory();

      if (!this.antiWeakness.is("NONE")
            && weakness != null
            && (strength == null || strength.getAmplifier() < weakness.getAmplifier())) {
         prevSlot = this.switchTo(antiWeaknessHotbar, antiWeaknessInv, this.antiWeakness);
      }

      if (!this.rotate.is("Off")) {
         Vec3d center = crystal.getBoundingBox().getCenter();
         this.applyRotation(center, Rotation.lookingAt(center, mc.player.getEyePos()));
      }

      this.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
      this.swingHand(false, true);

      this.breakTicks = 0;
      this.crystalTracker.onAttack(crystal, this.breakFailsafe.isEnabled(), (int)this.attempts.getCurrent());
      this.rotationTicks = 10;

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof EndCrystalEntity exCrystal
               && exCrystal.squaredDistanceTo(crystal.getX(), crystal.getY(), crystal.getZ()) <= 144.0
               && !this.crystalTracker.isDead(exCrystal.getId())) {
            this.crystalTracker.setDead(exCrystal.getId(), System.currentTimeMillis());
         }
      }

      if (prevSlot != -1) {
         if (this.antiWeakness.is("SILENT")) {
            mc.player.getInventory().selectedSlot = prevSlot;
            this.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
         } else if (this.antiWeakness.is("INVENTORY")) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, prevSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
         }
      }
   }

   private boolean canAttackCrystal(EndCrystalEntity crystal) {
      if (crystal == null || this.target == null || mc.player == null || mc.world == null) {
         return false;
      }

      double distanceSq = mc.player.getEyePos().squaredDistanceTo(crystal.getBoundingBox().getCenter());
      double maxRangeSq = this.canSee(crystal.getPos()) ? this.square(this.explodeRange.getCurrent()) : this.square(this.explodeWallRange.getCurrent());
      if (distanceSq > maxRangeSq) {
         return false;
      }

      if (!crystal.isAlive()) {
         return false;
      }

      float damage = this.getCrystalDamage(crystal.getPos(), this.target);
      float selfDamage = this.getCrystalDamage(crystal.getPos(), mc.player);
      boolean overrideDamage = this.shouldOverrideMaxSelfDmg(damage, selfDamage);

      if (this.protectFriends.isEnabled()) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player || !HuihuiClient.getInstance().getFriendManager().isFriend(player.getName().getString())) {
               continue;
            }
            float friendDamage = this.getCrystalDamage(crystal.getPos(), player);
            if (friendDamage > selfDamage) {
               selfDamage = friendDamage;
            }
         }
      }

      return !(selfDamage > this.maxSelfDamage.getCurrent()) || overrideDamage;
   }

   private int switchTo(HotbarSearch hotbar, int invSlot, ModeSetting switchMode) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return -1;
      }

      int prevSlot = mc.player.getInventory().selectedSlot;
      if (switchMode.is("INVENTORY")) {
         if (invSlot != -1) {
            prevSlot = invSlot;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, invSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
            this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
         }
      } else if (switchMode.is("NORMAL")) {
         if (hotbar.found()) {
            mc.player.getInventory().selectedSlot = hotbar.slot();
            this.sendPacket(new UpdateSelectedSlotC2SPacket(hotbar.slot()));
         }
      } else if (switchMode.is("SILENT")) {
         if (hotbar.found()) {
            mc.player.getInventory().selectedSlot = hotbar.slot();
            this.sendPacket(new UpdateSelectedSlotC2SPacket(hotbar.slot()));
         }
      }

      return prevSlot;
   }

   private void placeCrystal(BlockHitResult bhr, boolean packetRotate, boolean onSpawn) {
      if (this.shouldPause() || mc.player == null || bhr == null) {
         return;
      }

      int prevSlot = -1;

      HotbarSearch crystalResult = this.findInHotbar(Items.END_CRYSTAL);
      int crystalResultInv = this.findInInventory(Items.END_CRYSTAL);

      boolean offhand = mc.player.getOffHandStack().getItem() instanceof EndCrystalItem;
      boolean holdingCrystal = mc.player.getMainHandStack().getItem() instanceof EndCrystalItem || offhand;

      if (!this.rotate.is("Off")) {
         this.rotationVec = new RotationVec(bhr.getPos(), bhr, true);
         this.applyRotation(bhr.getPos(), Rotation.lookingAt(bhr.getPos(), mc.player.getEyePos()));
         if (!packetRotate && !this.rotated) {
            return;
         }
      }

      if (this.isPositionBlockedByEntity(bhr.getBlockPos(), false)) {
         return;
      }

      if (!this.autoSwitch.is("NONE") && !holdingCrystal) {
         prevSlot = this.switchTo(crystalResult, crystalResultInv, this.autoSwitch);
      }

      if (!(mc.player.getMainHandStack().getItem() instanceof EndCrystalItem || offhand || this.autoSwitch.is("SILENT"))) {
         return;
      }

      Hand hand = offhand ? Hand.OFF_HAND : Hand.MAIN_HAND;
      boolean accepted = mc.interactionManager.interactBlock(mc.player, hand, bhr).isAccepted();
      if (!accepted) {
         return;
      }

      this.swingHand(offhand, false);

      if (this.passedTicks(this.breakTicks, (int)this.breakDelay.getCurrent()) && this.idPredict.isEnabled()) {
         this.predictAttack();
      }

      this.placeTicks = 0;
      this.rotationTicks = 10;

      this.crystalTracker.addAwaitingPos(bhr.getBlockPos(), this.placeFailsafe.isEnabled());
      this.renderPositions.put(bhr.getBlockPos().toImmutable(), System.currentTimeMillis());
      this.postPlaceSwitch(prevSlot);

      if (onSpawn) {
         this.placedOnSpawn = true;
         this.placeSyncTicks = 0;
      }
   }

   private void postPlaceSwitch(int slot) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return;
      }

      if (this.autoSwitch.is("SILENT") && slot != -1) {
         mc.player.getInventory().selectedSlot = slot;
         this.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
      }

      if (this.autoSwitch.is("INVENTORY") && slot != -1) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
         this.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
      }
   }

   private void calcPosition(float range, Vec3d center) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      this.calcTicks = 0;

      if (this.target == null) {
         this.bestPosition = null;
         this.currentData = null;
         return;
      }

      List<PlaceData> list = this.getPossibleBlocks(this.target, center, range).stream()
            .filter(data -> this.isSafe(data.damage(), data.selfDamage(), data.overrideDamage()))
            .toList();

      this.bestPosition = list.isEmpty() ? null : this.filterPositions(list);
   }

   private List<PlaceData> getPossibleBlocks(PlayerEntity target, Vec3d center, float range) {
      List<PlaceData> blocks = new ArrayList<>();
      BlockPos playerPos = BlockPos.ofFloored(center);
      Vec3d predictedPlayerPos = this.predictedPlayerPos();
      int r = MathHelper.ceil(range);
      double scanRangeSq = this.square(range + 1.0F);

      for (int x = playerPos.getX() - r; x <= playerPos.getX() + r; x++) {
         for (int y = playerPos.getY() - r; y <= playerPos.getY() + r; y++) {
            for (int z = playerPos.getZ() - r; z <= playerPos.getZ() + r; z++) {
               BlockPos bp = new BlockPos(x, y, z);
               if (bp.toCenterPos().squaredDistanceTo(center) > scanRangeSq) {
                  continue;
               }

               PlaceData data = this.getPlaceData(bp, target, predictedPlayerPos);
               if (data != null) {
                  blocks.add(data);
               }
            }
         }
      }

      return blocks;
   }

   private List<CrystalData> getPossibleCrystals(PlayerEntity target) {
      List<CrystalData> crystals = new ArrayList<>();
      if (mc.player == null || mc.world == null) {
         return crystals;
      }

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof EndCrystalEntity crystal)) {
            continue;
         }

         if (this.crystalTracker.isBlocked(crystal.getId())) {
            continue;
         }

         if (this.crystalTracker.isDead(crystal.getId()) && this.inhibit.isEnabled()) {
            continue;
         }

         double maxRangeSq = this.canSee(crystal.getPos()) ? this.square(this.explodeRange.getCurrent()) : this.square(this.explodeWallRange.getCurrent());
         if (mc.player.getEyePos().squaredDistanceTo(crystal.getBoundingBox().getCenter()) > maxRangeSq) {
            continue;
         }

         if (!crystal.isAlive()) {
            continue;
         }

         float damage = this.getCrystalDamage(entity.getPos(), this.target);
         float selfDamage = this.getCrystalDamage(entity.getPos(), mc.player);
         boolean overrideDamage = this.shouldOverrideMaxSelfDmg(damage, selfDamage);

         if (this.protectFriends.isEnabled()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
               if (player == null || player == mc.player || !HuihuiClient.getInstance().getFriendManager().isFriend(player.getName().getString())) {
                  continue;
               }
               float friendDamage = this.getCrystalDamage(entity.getPos(), player);
               if (friendDamage > selfDamage) {
                  selfDamage = friendDamage;
               }
            }
         }

         if (damage < 1.5f) {
            continue;
         }
         if (selfDamage > this.maxSelfDamage.getCurrent() && !overrideDamage) {
            continue;
         }

         crystals.add(new CrystalData(crystal, damage, selfDamage, overrideDamage));
      }

      return crystals;
   }

   private void getCrystalToExplode() {
      if (this.target == null) {
         this.bestCrystal = null;
         return;
      }

      if (this.secondaryCrystal != null) {
         this.bestCrystal = this.canAttackCrystal(this.secondaryCrystal) ? this.secondaryCrystal : null;
         this.secondaryCrystal = null;
         return;
      }

      List<CrystalData> list = this.getPossibleCrystals(this.target).stream()
            .filter(data -> this.isSafe(data.damage(), data.selfDamage(), data.overrideDamage()))
            .toList();
      this.bestCrystal = list.isEmpty() ? null : this.filterCrystals(list);
   }

   private boolean isSafe(float damage, float selfDamage, boolean overrideDamage) {
      if (mc.player == null || mc.world == null) {
         return false;
      }

      if (overrideDamage) {
         return true;
      }

      if (selfDamage + 0.5f > mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
         return false;
      }

      if (this.efficiency.isEnabled()) {
         return damage / Math.max(0.1f, selfDamage) >= this.efficiencyFactor.getCurrent();
      }

      return true;
   }

   private BlockHitResult filterPositions(List<PlaceData> clearedList) {
      PlaceData bestData = null;
      float bestVal = 0.0f;

      for (PlaceData data : clearedList) {
         if (!(this.shouldOverrideMinDmg(data.damage()) || data.damage() > this.minDamage.getCurrent())) {
            continue;
         }

         if (bestData != null
               && data.overrideDamage()
               && this.target.getAbsorptionAmount() + this.target.getHealth() < bestData.damage()
               && bestData.selfDamage() < data.selfDamage()) {
            continue;
         }

         boolean shouldStopOverride = bestData != null
               && bestData.overrideDamage()
               && data.damage() > this.target.getHealth() + this.target.getAbsorptionAmount()
               && data.selfDamage() < bestData.selfDamage();

         float safetyComparatorDelta = shouldStopOverride ? 10.0f : 1.0f;

         if (bestData != null
               && Math.abs(bestData.damage() - data.damage()) < safetyComparatorDelta
               && Math.abs(bestData.selfDamage() - data.selfDamage()) > 1.0f) {
            if (bestData.selfDamage() >= data.selfDamage()) {
               bestData = data;
               bestVal = data.damage();
            }
         } else if (bestVal < data.damage()) {
            bestData = data;
            bestVal = data.damage();
         }
      }

      if (bestData == null) {
         return null;
      }

      this.facePlacing = bestData.damage() < this.minDamage.getCurrent();
      this.renderDamage = bestData.damage();
      this.renderSelfDamage = bestData.selfDamage();
      this.currentData = bestData;
      return bestData.bhr();
   }

   private EndCrystalEntity filterCrystals(List<CrystalData> clearedList) {
      CrystalData bestData = null;
      float bestVal = 0.0f;

      for (CrystalData data : clearedList) {
         if (!(this.shouldOverrideMinDmg(data.damage()) || data.damage() > this.minDamage.getCurrent())) {
            continue;
         }

         if (bestData != null
               && data.overrideDamage()
               && this.target.getAbsorptionAmount() + this.target.getHealth() < bestData.damage()
               && bestData.selfDamage() < data.selfDamage()) {
            continue;
         }

         boolean shouldStopOverride = bestData != null
               && bestData.overrideDamage()
               && data.damage() > this.target.getHealth() + this.target.getAbsorptionAmount()
               && data.selfDamage() < bestData.selfDamage();

         float safetyComparatorDelta = shouldStopOverride ? 10.0f : 1.0f;

         if (bestData != null
               && Math.abs(bestData.damage() - data.damage()) < safetyComparatorDelta
               && Math.abs(bestData.selfDamage() - data.selfDamage()) > 1.0f) {
            if (bestData.selfDamage() >= data.selfDamage()) {
               bestData = data;
               bestVal = data.damage();
            }
         } else if (bestVal < data.damage()) {
            bestData = data;
            bestVal = data.damage();
         }
      }

      if (bestData == null) {
         return null;
      }

      this.renderDamage = bestData.damage();
      this.renderSelfDamage = bestData.selfDamage();
      return bestData.crystal();
   }

   private PlaceData getPlaceData(BlockPos bp, PlayerEntity target, Vec3d predictedPlayerPos) {
      if (mc.player == null || mc.world == null) {
         return null;
      }

      if (this.crystalTracker.isPositionBlocked(bp, this.placeFailsafe.isEnabled(), (int)this.attempts.getCurrent())) {
         return null;
      }

      if (!this.predictCrystalSpawn(bp, predictedPlayerPos)) {
         return null;
      }

      if (target != null && target.getPos().squaredDistanceTo(bp.toCenterPos().add(0.0, 0.5, 0.0)) > 144.0) {
         return null;
      }

      Block base = mc.world.getBlockState(bp).getBlock();
      if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) {
         return null;
      }

      boolean freeSpace = mc.world.getBlockState(bp.up()).isAir();
      if (!freeSpace) {
         return null;
      }

      if (this.isPositionBlockedByEntity(bp, true)) {
         return null;
      }

      Vec3d crystalVec = new Vec3d(bp.getX() + 0.5, bp.getY() + 1.0, bp.getZ() + 0.5);
      BlockHitResult interactResult = this.getInteractResult(bp, crystalVec);
      if (interactResult == null) {
         return null;
      }

      float damage = target == null ? 10.0f : this.getCrystalDamage(crystalVec, target);
      if (damage < 1.5f) {
         return null;
      }

      float selfDamage = this.getCrystalDamage(crystalVec, mc.player);
      boolean overrideDamage = this.shouldOverrideMaxSelfDmg(damage, selfDamage);

      if (this.protectFriends.isEnabled()) {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player || !HuihuiClient.getInstance().getFriendManager().isFriend(player.getName().getString())) {
               continue;
            }
            float friendDamage = this.getCrystalDamage(crystalVec, player);
            if (friendDamage > selfDamage) {
               selfDamage = friendDamage;
            }
         }
      }

      if (selfDamage > this.maxSelfDamage.getCurrent() && !overrideDamage) {
         return null;
      }

      return new PlaceData(interactResult, damage, selfDamage, overrideDamage);
   }

   private boolean predictCrystalSpawn(BlockPos bp, Vec3d predictedPlayerPos) {
      Vec3d predictedPos = bp.toCenterPos().add(0.0, 1.5, 0.0);
      Vec3d predictedEyes = predictedPlayerPos.add(0.0, mc.player.getEyeHeight(mc.player.getPose()), 0.0);
      double distanceSq = predictedEyes.squaredDistanceTo(predictedPos);

      if (this.canSee(predictedPos)) {
         return distanceSq <= this.square(this.explodeRange.getCurrent());
      }

      return distanceSq <= this.square(this.explodeWallRange.getCurrent());
   }

   private BlockHitResult getInteractResult(BlockPos bp, Vec3d crystalVec) {
      return this.interact.is("Strict") ? this.getStrictInteract(bp) : this.getDefaultInteract(crystalVec, bp);
   }

   private BlockHitResult getDefaultInteract(Vec3d crystalVector, BlockPos bp) {
      if (mc.player == null || mc.world == null) {
         return null;
      }

      if (mc.player.getEyePos().squaredDistanceTo(crystalVector) > this.square(this.placeRange.getCurrent())) {
         return null;
      }

      HitResult wallCheck = mc.world.raycast(new RaycastContext(
            mc.player.getEyePos(),
            crystalVector,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
      ));

      if (wallCheck instanceof BlockHitResult blockHit
            && blockHit.getType() == HitResult.Type.BLOCK
            && !blockHit.getBlockPos().equals(bp)
            && mc.player.getEyePos().squaredDistanceTo(crystalVector) > this.square(this.placeWallRange.getCurrent())) {
         return null;
      }

      Direction side = mc.world.isInBuildLimit(bp.up()) ? Direction.UP : Direction.DOWN;
      return new BlockHitResult(crystalVector, side, bp, false);
   }

   private BlockHitResult getStrictInteract(BlockPos bp) {
      if (mc.player == null || mc.world == null) {
         return null;
      }

      float bestDistance = Float.MAX_VALUE;
      Direction bestDirection = null;
      Vec3d bestVec = null;

      float upPoint = this.strictCenter.isEnabled() ? (float)bp.toCenterPos().getY() : bp.up().getY();
      if (mc.player.getEyePos().getY() > upPoint) {
         bestDirection = Direction.UP;
      } else if (mc.player.getEyePos().getY() < bp.getY() && mc.world.getBlockState(bp.down()).isAir()) {
         bestDirection = Direction.DOWN;
      }

      List<Direction> directions = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
      if (bestDirection != null) {
         directions.add(bestDirection);
      }

      for (Direction dir : directions) {
         Vec3d vec = bp.toCenterPos().add(Vec3d.of(dir.getVector()).multiply(0.5));
         if (!mc.world.getBlockState(bp.offset(dir)).isReplaceable()) {
            continue;
         }

         double distanceSq = mc.player.getEyePos().squaredDistanceTo(vec);
         double maxDistanceSq = this.canSee(vec) ? this.square(this.placeRange.getCurrent()) : this.square(this.placeWallRange.getCurrent());
         if (distanceSq > maxDistanceSq) {
            continue;
         }

         if (distanceSq < bestDistance) {
            bestDistance = (float)distanceSq;
            bestDirection = dir;
            bestVec = vec;
         }
      }

      if (bestDirection == null || bestVec == null) {
         return null;
      }

      return new BlockHitResult(bestVec, bestDirection, bp, false);
   }

   private boolean isPositionBlockedByEntity(BlockPos base, boolean calcPhase) {
      if (mc.player == null || mc.world == null) {
         return false;
      }

      Box box = new Box(base.up()).expand(0.0, 1.0, 0.0);
      for (Entity entity : mc.world.getEntities()) {
         if (entity == null || !entity.isAlive() || !entity.getBoundingBox().intersects(box)) {
            continue;
         }

         if (entity instanceof ExperienceOrbEntity) {
            continue;
         }

         if (entity instanceof EndCrystalEntity crystal) {
            if (this.crystalTracker.isDead(crystal.getId())) {
               continue;
            }

            if (this.crystalTracker.isBlocked(crystal.getId())) {
               return true;
            }

            if (calcPhase) {
               if (this.canAttackCrystal(crystal)) {
                  continue;
               }
            } else if (crystal.getPos().squaredDistanceTo(box.getCenter()) > 0.3) {
               this.secondaryCrystal = crystal;
            }
         }

         return true;
      }
      return false;
   }

   private boolean shouldOverrideMaxSelfDmg(float damage, float selfDamage) {
      if (!this.overrideSelfDamage.isEnabled() || this.target == null || mc.player == null || mc.world == null) {
         return false;
      }

      boolean targetSafe = this.target.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) || this.target.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
      boolean playerSafe = mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) || mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);

      float targetHp = this.target.getHealth() + this.target.getAbsorptionAmount();
      float playerHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

      boolean canPop = damage > targetHp && targetSafe;
      boolean canKill = damage > targetHp && !targetSafe;
      boolean canPopSelf = selfDamage > playerHp && playerSafe;
      boolean canKillSelf = selfDamage > playerHp && !playerSafe;

      if (canPopSelf && canKill && this.sacrificeTotem.isEnabled()) {
         return true;
      }

      return selfDamage > this.maxSelfDamage.getCurrent() && (canPop || canKill) && !canKillSelf && !canPopSelf;
   }

   private boolean shouldOverrideMinDmg(float damage) {
      if (this.target == null) {
         return false;
      }

      if (this.facePlaceButton.getKeyCode() != -1 && Keyboard.isKeyDown(this.facePlaceButton.getKeyCode())) {
         return true;
      }

      if ((this.target.getHealth() + this.target.getAbsorptionAmount()) - damage < 0.0f) {
         return true;
      }

      if (this.armorBreaker.isEnabled()) {
         for (ItemStack armor : this.target.getArmorItems()) {
            if (armor == null || armor.isEmpty() || armor.getItem() == Items.AIR || !armor.isDamageable()) {
               continue;
            }

            float durabilityPercent = ((armor.getMaxDamage() - armor.getDamage()) / (float)armor.getMaxDamage()) * 100.0f;
            if (durabilityPercent < this.armorScale.getCurrent()) {
               return true;
            }
         }
      }

      return this.target.getHealth() + this.target.getAbsorptionAmount() <= this.facePlaceHp.getCurrent();
   }

   private void swingHand(boolean offHand, boolean attack) {
      Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
      if (this.swingMode.is("Both")) {
         mc.player.swingHand(hand);
      } else if (this.swingMode.is("Break")) {
         if (attack) {
            mc.player.swingHand(Hand.MAIN_HAND);
         } else {
            this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      } else if (this.swingMode.is("Place")) {
         if (!attack) {
            mc.player.swingHand(hand);
         } else {
            this.sendPacket(new HandSwingC2SPacket(hand));
         }
      } else if (this.swingMode.is("ServerSide")) {
         this.sendPacket(new HandSwingC2SPacket(hand));
      } else {
         mc.player.swingHand(hand);
      }
   }

   private void predictAttack() {
      int attacks = (int)this.idAttacks.getCurrent();
      for (int i = 1; i <= attacks; i++) {
         int id = (int)(this.currentId + i);
         Entity entity = mc.world.getEntityById(id);
         if (entity == null || entity instanceof EndCrystalEntity) {
            PlayerInteractEntityC2SPacket attackPacket = PlayerInteractEntityC2SPacket.attack(mc.player, mc.player.isSneaking());
            this.changeId(attackPacket, id);
            this.sendPacket(attackPacket);
            this.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
         }
      }
   }

   private static void changeId(PlayerInteractEntityC2SPacket packet, int id) {
      try {
         Field field;
         try {
            field = PlayerInteractEntityC2SPacket.class.getDeclaredField("entityId");
         } catch (NoSuchFieldException ex) {
            field = PlayerInteractEntityC2SPacket.class.getDeclaredField("field_12870");
         }
         field.setAccessible(true);
         field.setInt(packet, id);
      } catch (Exception ignored) {
      }
   }

   private void processSpawnPacket(int id) {
      if (id > this.currentId) {
         this.currentId = id;
      }
   }

   private void confirmAwaitingBySpawn(int entityId, Vec3d spawnPos) {
      if (spawnPos == null) {
         return;
      }

      Map<BlockPos, CrystalTracker.Attempt> awaiting = this.crystalTracker.getAwaitingPositions();
      if (awaiting.isEmpty()) {
         return;
      }

      for (Map.Entry<BlockPos, CrystalTracker.Attempt> entry : new ArrayList<>(awaiting.entrySet())) {
         BlockPos bp = entry.getKey();
         if (spawnPos.squaredDistanceTo(bp.toCenterPos()) > 0.36) {
            continue;
         }

         this.crystalTracker.confirmSpawn(bp);

         if (this.passedTicks(this.breakTicks, (int)this.breakDelay.getCurrent())) {
            Entity entity = mc.world == null ? null : mc.world.getEntityById(entityId);
            if (entity instanceof EndCrystalEntity crystal) {
               this.handleSpawn(crystal);
            } else {
               this.attackSpawnById(entityId);
            }
         }
         break;
      }
   }

   private void attackSpawnById(int entityId) {
      if (mc.player == null || mc.world == null || this.target == null || this.shouldPause()) {
         return;
      }

      PlayerInteractEntityC2SPacket attackPacket = PlayerInteractEntityC2SPacket.attack(mc.player, mc.player.isSneaking());
      this.changeId(attackPacket, entityId);
      this.sendPacket(attackPacket);
      this.swingHand(false, true);
      this.crystalTracker.setDead(entityId, System.currentTimeMillis());
      this.breakTicks = 0;
      this.rotationTicks = 10;
   }

   private Vec3d resolveEntitySpawnPos(EntitySpawnS2CPacket packet) {
      Double x = this.tryReadCoordinate(packet, "getX", "x");
      Double y = this.tryReadCoordinate(packet, "getY", "y");
      Double z = this.tryReadCoordinate(packet, "getZ", "z");
      if (x != null && y != null && z != null) {
         return new Vec3d(x, y, z);
      }

      try {
         var method = packet.getClass().getMethod("getPos");
         Object value = method.invoke(packet);
         if (value instanceof Vec3d vec) {
            return vec;
         }
      } catch (ReflectiveOperationException ignored) {
      }

      return null;
   }

   private Vec3d resolveExplosionPos(ExplosionS2CPacket packet) {
      Vec3d center = this.tryReadCenter(packet);
      if (center != null) {
         return center;
      }

      Double x = this.tryReadCoordinate(packet, "getX", "x");
      Double y = this.tryReadCoordinate(packet, "getY", "y");
      Double z = this.tryReadCoordinate(packet, "getZ", "z");
      if (x != null && y != null && z != null) {
         return new Vec3d(x, y, z);
      }

      if (mc.player != null) {
         return mc.player.getPos();
      }

      return Vec3d.ZERO;
   }

   private Vec3d tryReadCenter(ExplosionS2CPacket packet) {
      try {
         var method = packet.getClass().getMethod("center");
         Object value = method.invoke(packet);
         if (value instanceof Vec3d vec) {
            return vec;
         }
      } catch (ReflectiveOperationException ignored) {
      }

      try {
         var method = packet.getClass().getMethod("getCenter");
         Object value = method.invoke(packet);
         if (value instanceof Vec3d vec) {
            return vec;
         }
      } catch (ReflectiveOperationException ignored) {
      }

      return null;
   }

   private Double tryReadCoordinate(Object packet, String methodName, String fieldName) {
      try {
         var method = packet.getClass().getMethod(methodName);
         Object value = method.invoke(packet);
         if (value instanceof Number number) {
            return number.doubleValue();
         }
      } catch (ReflectiveOperationException ignored) {
      }

      try {
         var field = packet.getClass().getDeclaredField(fieldName);
         field.setAccessible(true);
         Object value = field.get(packet);
         if (value instanceof Number number) {
            return number.doubleValue();
         }
      } catch (ReflectiveOperationException ignored) {
      }

      return null;
   }

   private int getPredictTicks() {
      return 0;
   }

   private int getSelfPredictTicks() {
      return (int)Math.ceil((this.getPing() * 1.5f) / 50.0f);
   }

   private Vec3d predictedPlayerPos() {
      Vec3d pos = mc.player.getPos();
      Vec3d vel = mc.player.getVelocity();
      int ticks = this.getSelfPredictTicks();
      return pos.add(vel.x * (double)ticks, 0.0D, vel.z * (double)ticks);
   }

   private float getPing() {
      if (mc.getNetworkHandler() == null || mc.player == null) {
         return 0f;
      }

      PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
      return entry == null ? 0f : entry.getLatency();
   }

   private boolean shouldPause() {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) {
         return true;
      }

      boolean offhand = mc.player.getOffHandStack().getItem() instanceof EndCrystalItem;
      boolean mainHand = mc.player.getMainHandStack().getItem() instanceof EndCrystalItem;

      if (mc.interactionManager.isBreakingBlock() && !offhand && this.mining.isEnabled()) {
         this.currentState = State.Mining;
         return true;
      }

      if (this.autoSwitch.is("NONE") && !offhand && !mainHand) {
         this.currentState = State.NoCrystalls;
         return true;
      }

      if ((this.autoSwitch.is("SILENT") || this.autoSwitch.is("NORMAL")) && !this.findInHotbar(Items.END_CRYSTAL).found() && !offhand) {
         this.currentState = State.NoCrystalls;
         return true;
      }

      if (this.autoSwitch.is("INVENTORY") && this.findAny(Items.END_CRYSTAL) == -1 && !offhand) {
         this.currentState = State.NoCrystalls;
         return true;
      }

      if (mc.player.isUsingItem() && this.eating.isEnabled()) {
         this.currentState = State.Eating;
         return true;
      }

      if (this.rotationMarkedDirty()) {
         this.currentState = State.ExternalPause;
         return true;
      }

      if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < this.pauseHP.getCurrent()) {
         this.currentState = State.LowHP;
         return true;
      }

      this.currentState = State.Active;
      return false;
   }

   private boolean rotationMarkedDirty() {
      if (this.surround.isEnabled()) {
         if (Surround.INSTANCE.isEnabled()) {
            return true;
         }
      }

      if (this.inventoryPause.isEnabled() && mc.currentScreen != null) {
         return true;
      }

      return false;
   }

   private PlayerEntity findTarget(float range, String logic) {
      if (mc.player == null || mc.world == null) {
         return null;
      }

      PlayerEntity best = null;
      double bestValue = Double.MAX_VALUE;
      boolean byHealth = "Health".equalsIgnoreCase(logic);

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == null || player == mc.player || !player.isAlive() || player.isSpectator()) {
            continue;
         }

         if (HuihuiClient.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            continue;
         }

         double dist = mc.player.squaredDistanceTo(player);
         if (dist > (double)range * range) {
            continue;
         }

         double value = byHealth ? player.getHealth() + player.getAbsorptionAmount() : dist;
         if (value < bestValue) {
            best = player;
            bestValue = value;
         }
      }

      return best;
   }

   private HotbarSearch findInHotbar(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().main.get(i).getItem() == item) {
            return new HotbarSearch(true, i);
         }
      }
      return new HotbarSearch(false, -1);
   }

   private int findInInventory(Item item) {
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().main.get(i).getItem() == item) {
            return i;
         }
      }
      return -1;
   }

   private int findAny(Item item) {
      HotbarSearch hotbar = this.findInHotbar(item);
      if (hotbar.found()) {
         return hotbar.slot();
      }
      return this.findInInventory(item);
   }

   private HotbarSearch findAntiWeaknessHotbar() {
      if (mc.player == null) {
         return new HotbarSearch(false, -1);
      }

      for (int i = 0; i < 9; i++) {
         Item item = mc.player.getInventory().main.get(i).getItem();
         if (item instanceof SwordItem || item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem) {
            return new HotbarSearch(true, i);
         }
      }

      return new HotbarSearch(false, -1);
   }

   private int findAntiWeaknessInventory() {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 9; i < 36; i++) {
         Item item = mc.player.getInventory().main.get(i).getItem();
         if (item instanceof SwordItem || item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem) {
            return i;
         }
      }

      return -1;
   }

   private void sendPacket(Packet<?> packet) {
      if (mc.getNetworkHandler() != null && packet != null) {
         mc.getNetworkHandler().sendPacket(packet);
      }
   }

   private boolean passedTicks(int counter, int delay) {
      return counter >= Math.max(0, delay);
   }

   private void cleanupRenderPositions() {
      long now = System.currentTimeMillis();
      long holdMs = (long)this.renderHoldMs.getCurrent();
      this.renderPositions.entrySet().removeIf(entry -> now - entry.getValue() > holdMs);
   }

   private double square(double value) {
      return value * value;
   }

   private boolean render(String key) {
      return this.renderSettings.isEnable(key);
   }

   private float getCrystalDamage(Vec3d crystalPos, PlayerEntity player) {
      if (mc.world == null || player == null || !player.isAlive()) {
         return 0.0F;
      }
      double distExposure = player.squaredDistanceTo(crystalPos) / 144.0D;
      if (distExposure > 1.0D) {
         return 0.0F;
      }
      float exposure = ExplosionImpl.calculateReceivedDamage(crystalPos, player);
      double finalExposure = (1.0D - distExposure) * (double)exposure;
      float damage = (float)Math.floor((finalExposure * finalExposure + finalExposure) / 2.0D * 7.0D * 12.0D + 1.0D);
      DamageSource source = Explosion.createDamageSource(mc.world, null);
      damage = DamageUtil.getDamageLeft(player, damage, source, (float)player.getArmor(), (float)player.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS));
      return Math.max(damage, 0.0F);
   }

   private boolean canSee(Vec3d point) {
      BlockHitResult hit = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
      return hit == null || hit.getType() != HitResult.Type.BLOCK;
   }

   public record PlaceData(BlockHitResult bhr, float damage, float selfDamage, boolean overrideDamage) {
   }

   private record CrystalData(EndCrystalEntity crystal, float damage, float selfDamage, boolean overrideDamage) {
   }

   private record RotationVec(Vec3d vec, BlockHitResult hitVec, boolean place) {
   }

   private record HotbarSearch(boolean found, int slot) {
   }

   private enum State {
      Active, Eating, LowHP, NoTarget, NoCrystalls, ExternalPause, Mining
   }

   private static final class CrystalTracker {
      private final Map<Integer, Long> deadCrystals = new ConcurrentHashMap<>();
      private final Map<Integer, Integer> attackAttempts = new ConcurrentHashMap<>();
      private final Map<Integer, Long> blockedCrystals = new ConcurrentHashMap<>();
      private final Map<BlockPos, Attempt> awaitingPositions = new ConcurrentHashMap<>();

      void reset() {
         this.deadCrystals.clear();
         this.attackAttempts.clear();
         this.blockedCrystals.clear();
         this.awaitingPositions.clear();
      }

      void update() {
         long now = System.currentTimeMillis();
         this.deadCrystals.entrySet().removeIf(entry -> now - entry.getValue() > 1800L);
         this.blockedCrystals.entrySet().removeIf(entry -> now - entry.getValue() > 1800L);
         this.awaitingPositions.entrySet().removeIf(entry -> now - entry.getValue().time() > 2500L);
      }

      void onAttack(EndCrystalEntity crystal, boolean breakFailsafeEnabled, int maxAttempts) {
         if (crystal == null) {
            return;
         }

         int id = crystal.getId();
         this.setDead(id, System.currentTimeMillis());

         if (breakFailsafeEnabled) {
            int attempts = this.attackAttempts.getOrDefault(id, 0) + 1;
            this.attackAttempts.put(id, attempts);
            if (attempts >= Math.max(1, maxAttempts)) {
               this.blockedCrystals.put(id, System.currentTimeMillis());
            }
         }
      }

      boolean isDead(int id) {
         return this.deadCrystals.containsKey(id);
      }

      void setDead(int id, long time) {
         this.deadCrystals.put(id, time);
      }

      boolean isBlocked(int id) {
         return this.blockedCrystals.containsKey(id);
      }

      void addAwaitingPos(BlockPos pos, boolean placeFailsafeEnabled) {
         Attempt previous = this.awaitingPositions.get(pos);
         int nextAttempt = previous == null ? 1 : previous.attempts() + 1;
         this.awaitingPositions.put(pos.toImmutable(), new Attempt(System.currentTimeMillis(), placeFailsafeEnabled ? nextAttempt : 1));
      }

      boolean isPositionBlocked(BlockPos pos, boolean placeFailsafeEnabled, int maxAttempts) {
         if (!placeFailsafeEnabled) {
            return false;
         }

         Attempt attempt = this.awaitingPositions.get(pos);
         return attempt != null && attempt.attempts() >= Math.max(1, maxAttempts);
      }

      void confirmSpawn(BlockPos pos) {
         this.awaitingPositions.remove(pos);
      }

      Map<BlockPos, Attempt> getAwaitingPositions() {
         return this.awaitingPositions;
      }

      record Attempt(long time, int attempts) {
      }
   }
}
