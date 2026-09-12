package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.blade.context.RotationContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.event.events.render.Render3DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.InputBindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.MultiSelectSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.utils.FriendManager;
import org.blade.utils.render.world.WorldMeshRenderer;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoCrystalFeature extends Feature {
    public static final AutoCrystalFeature INSTANCE = new AutoCrystalFeature();

    public final ModeSetting page = register(new ModeSetting(
            "Страница", "Основное",
            "Основное", "Размещение", "Взрыв", "Урон",
            "Пауза", "Переключение", "Защита", "Рендер", "IDPredict"
    ));

    public final BooleanSetting await = register(new BooleanSetting("Ожидание хода", true))
            .visibleWhen(() -> page.is("Основное"));
    public final ModeSetting timing = register(new ModeSetting(
            "Тайминг", "NORMAL", "NORMAL", "SEQUENTIAL"
    )).visibleWhen(() -> page.is("Основное"));
    public final ModeSetting sequential = register(new ModeSetting(
            "Последовательно", "Off", "Off", "Strict", "Strong"
    )).visibleWhen(() -> page.is("Основное"));
    public final ModeSetting rotate = register(new ModeSetting(
            "Поворот", "Instant", "Off", "Instant", "Smooth", "Stepped"
    )).visibleWhen(() -> page.is("Основное"));
    public final BooleanSetting yawStep = register(new BooleanSetting("Ограничение поворота", false))
            .visibleWhen(() -> !rotate.is("Off") && page.is("Основное"));
    public final NumberSetting yawAngle = register(new NumberSetting(
            "Макс. угол", 180.0D, 1.0D, 180.0D, 1.0D, "°"
    )).visibleWhen(() -> !rotate.is("Off") && yawStep.getValue() && page.is("Основное"));
    public final ModeSetting targetLogic = register(new ModeSetting(
            "Выбор цели", "Distance", "Distance", "Health"
    )).visibleWhen(() -> page.is("Основное"));
    public final NumberSetting targetRange = register(new NumberSetting(
            "Дальность цели", 10.0D, 1.0D, 15.0D, 0.1D, " б"
    )).visibleWhen(() -> page.is("Основное"));

    public final ModeSetting interact = register(new ModeSetting(
            "Интеракт", "Default", "Default", "Strict"
    )).visibleWhen(() -> page.is("Размещение"));
    public final BooleanSetting strictCenter = register(new BooleanSetting("Строгий центр", true))
            .visibleWhen(() -> page.is("Размещение") && interact.is("Strict"));
    public final BooleanSetting rayTraceBypass = register(new BooleanSetting("Обход рейтрейса", false))
            .visibleWhen(() -> page.is("Размещение"));
    public final NumberSetting placeDelay = register(new NumberSetting(
            "Задержка размещения", 0.0D, 0.0D, 20.0D, 1.0D, " т"
    )).visibleWhen(() -> page.is("Размещение"));
    public final NumberSetting placeRange = register(new NumberSetting(
            "Дальность размещения", 5.0D, 1.0D, 6.0D, 0.1D, " б"
    )).visibleWhen(() -> page.is("Размещение"));
    public final NumberSetting placeWallRange = register(new NumberSetting(
            "Дальность сквозь стены", 3.5D, 0.0D, 6.0D, 0.1D, " б"
    )).visibleWhen(() -> page.is("Размещение"));

    public final BooleanSetting inhibit = register(new BooleanSetting("Запрет мёртвых", true))
            .visibleWhen(() -> page.is("Взрыв"));
    public final NumberSetting breakDelay = register(new NumberSetting(
            "Задержка взрыва", 0.0D, 0.0D, 20.0D, 1.0D, " т"
    )).visibleWhen(() -> page.is("Взрыв"));
    public final NumberSetting explodeRange = register(new NumberSetting(
            "Дальность взрыва", 5.0D, 1.0D, 6.0D, 0.1D, " б"
    )).visibleWhen(() -> page.is("Взрыв"));
    public final NumberSetting explodeWallRange = register(new NumberSetting(
            "Взрыв сквозь стены", 3.5D, 0.0D, 6.0D, 0.1D, " б"
    )).visibleWhen(() -> page.is("Взрыв"));

    public final BooleanSetting miningPause = register(new BooleanSetting("Пауза при копании", true))
            .visibleWhen(() -> page.is("Пауза"));
    public final BooleanSetting eatingPause = register(new BooleanSetting("Пауза при еде", true))
            .visibleWhen(() -> page.is("Пауза"));
    public final BooleanSetting surroundPause = register(new BooleanSetting("Пауза с Surround", true))
            .visibleWhen(() -> page.is("Пауза"));
    public final BooleanSetting inventoryPause = register(new BooleanSetting("Пауза в инвентаре", false))
            .visibleWhen(() -> page.is("Пауза"));
    public final NumberSetting pauseHP = register(new NumberSetting(
            "Пауза по HP", 8.0D, 2.0D, 10.0D, 0.1D, " ♥"
    )).visibleWhen(() -> page.is("Пауза"));

    public final NumberSetting minDamage = register(new NumberSetting(
            "Мин. урон", 6.0D, 2.0D, 20.0D, 0.1D, ""
    )).visibleWhen(() -> page.is("Урон"));
    public final NumberSetting maxSelfDamage = register(new NumberSetting(
            "Макс. урон себе", 10.0D, 2.0D, 20.0D, 0.1D, ""
    )).visibleWhen(() -> page.is("Урон"));
    public final BooleanSetting efficiency = register(new BooleanSetting("Эффективность", false))
            .visibleWhen(() -> page.is("Урон"));
    public final NumberSetting efficiencyFactor = register(new NumberSetting(
            "Коэффициент", 1.0D, 0.1D, 5.0D, 0.1D, ""
    )).visibleWhen(() -> page.is("Урон") && efficiency.getValue());
    public final BooleanSetting protectFriends = register(new BooleanSetting("Защита друзей", true))
            .visibleWhen(() -> page.is("Урон"));
    public final BooleanSetting overrideSelfDamage = register(new BooleanSetting("Игнор урона себе", true))
            .visibleWhen(() -> page.is("Урон"));
    public final BooleanSetting sacrificeTotem = register(new BooleanSetting("Жертва тотемом", true))
            .visibleWhen(() -> page.is("Урон") && overrideSelfDamage.getValue());
    public final BooleanSetting armorBreaker = register(new BooleanSetting("Ломание брони", true))
            .visibleWhen(() -> page.is("Урон"));
    public final NumberSetting armorScale = register(new NumberSetting(
            "Процент брони", 5.0D, 0.0D, 40.0D, 0.1D, " %"
    )).visibleWhen(() -> page.is("Урон") && armorBreaker.getValue());
    public final NumberSetting facePlaceHp = register(new NumberSetting(
            "FacePlace HP", 5.0D, 0.0D, 20.0D, 0.1D, " ♥"
    )).visibleWhen(() -> page.is("Урон"));
    public final InputBindSetting facePlaceButton = register(new InputBindSetting("FacePlace кнопка", InputBindSetting.UNBOUND))
            .visibleWhen(() -> page.is("Урон"));
    public final BooleanSetting ignoreTerrain = register(new BooleanSetting("Игнор местности", true))
            .visibleWhen(() -> page.is("Урон"));

    public final ModeSetting autoSwitch = register(new ModeSetting(
            "Переключение", "NONE", "NONE", "NORMAL", "SILENT", "INVENTORY"
    )).visibleWhen(() -> page.is("Переключение"));
    public final ModeSetting antiWeakness = register(new ModeSetting(
            "AntiWeakness", "NONE", "NONE", "NORMAL", "SILENT", "INVENTORY"
    )).visibleWhen(() -> page.is("Переключение"));

    public final BooleanSetting placeFailsafe = register(new BooleanSetting("PlaceFailsafe", true))
            .visibleWhen(() -> page.is("Защита"));
    public final BooleanSetting breakFailsafe = register(new BooleanSetting("BreakFailsafe", true))
            .visibleWhen(() -> page.is("Защита"));
    public final NumberSetting attempts = register(new NumberSetting(
            "Макс. попыток", 5.0D, 1.0D, 30.0D, 1.0D, ""
    )).visibleWhen(() -> page.is("Защита"));

    public final BooleanSetting idPredict = register(new BooleanSetting("IDPredict", false))
            .visibleWhen(() -> page.is("IDPredict"));
    public final NumberSetting idAttacks = register(new NumberSetting(
            "IDАтак", 3.0D, 1.0D, 10.0D, 1.0D, ""
    )).visibleWhen(() -> page.is("IDPredict"));

    public final ModeSetting swingMode = register(new ModeSetting(
            "Свинг", "Both", "Both", "Place", "Break", "ServerSide"
    )).visibleWhen(() -> page.is("Рендер"));
    public final MultiSelectSetting renderSettings = register(new MultiSelectSetting(
            "Рендер", List.of("Block"), "Block", "Rect", "Animate"
    )).visibleWhen(() -> page.is("Рендер"));
    public final NumberSetting renderHoldMs = register(new NumberSetting(
            "Время отображения", 500.0D, 100.0D, 1000.0D, 10.0D, " мс"
    )).visibleWhen(() -> page.is("Рендер"));

    private static final double[][] EXPOSURE_OFFSETS = {
            {-0.25D, -0.25D}, {0.25D, -0.25D}, {-0.25D, 0.25D}, {0.25D, 0.25D},
            {-0.15D, -0.15D}, {0.15D, -0.15D}, {-0.15D, 0.15D}, {0.15D, 0.15D}
    };

    private final CrystalTracker crystalTracker = new CrystalTracker();
    private final Map<BlockPos, Long> renderPositions = new ConcurrentHashMap<>();

    private Player target;
    private PlaceData currentData;
    private BlockHitResult bestPosition;
    private EndCrystal bestCrystal;
    private EndCrystal secondaryCrystal;
    private Vec3 rotationVec;
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

    private AutoCrystalFeature() {
        super("AutoCrystal", "Автокристалл с расчётом урона", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
        RotationContext.clear();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        if (!initialized) {
            reset();
            initialized = true;
        }

        tickTimers();
        crystalTracker.update();
        cleanupRenderPositions();
        processAwaitingSpawnConfirm();

        WeakReference<Player> weakTarget = this.target == null ? null : new WeakReference<>(this.target);
        target = null;
        if (weakTarget != null && weakTarget.get() != null) {
            target = weakTarget.get();
        }
        if (target == null) {
        }
        this.target = findTarget((float) targetRange.getValue().doubleValue(), targetLogic.getValue());
        Player resolved = this.target;
        if (resolved != null && (!resolved.isAlive() || resolved.getHealth() <= 0.0F)) {
            this.target = null;
            this.currentState = State.NoTarget;
            this.lastTargetId = -1;
            clearCombatContext();
            return;
        }

        if (resolved == null) {
            this.currentState = State.NoTarget;
            this.lastTargetId = -1;
            clearCombatContext();
            return;
        }

        if (passedTicks(placeTicks, 20)) {
            renderDamage = 0.0F;
        }

        calcRotations();

        boolean targetChanged = lastTargetId != resolved.getId();
        if (targetChanged) {
            lastTargetId = resolved.getId();
            calcTicks = Integer.MAX_VALUE;
        }

        int pingGate = Math.max(1, Math.round(getPing() / 25.0F));
        if (targetChanged || bestPosition == null || !await.getValue()
                || passedTicks(placeTicks, 20) || passedTicks(calcTicks, pingGate)) {
            calcPosition((float) placeRange.getValue().doubleValue(), player.position());
        }

        getCrystalToExplode();

        if (timing.is("NORMAL") || timing.is("SEQUENTIAL")) {
            doAction();
        }
    }

    @EventTarget
    public void onPacket(PacketReceiveEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.getPacket() instanceof ClientboundAddEntityPacket add) {
            processSpawnPacket(add.getId());
            if (add.getType() == EntityTypes.END_CRYSTAL) {
                confirmAwaitingBySpawn(add.getId(), new Vec3(add.getX(), add.getY(), add.getZ()));
            }
        } else if (event.getPacket() instanceof ClientboundExplodePacket explosion) {
            Vec3 explosionPos = explosion.center();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof EndCrystal crystal
                        && crystal.distanceToSqr(explosionPos) <= 144.0
                        && !crystalTracker.isDead(crystal.getId())) {
                    crystalTracker.setDead(crystal.getId(), System.currentTimeMillis());
                }
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        cleanupRenderPositions();

        long now = System.currentTimeMillis();
        float holdMs = (float) renderHoldMs.getValue().doubleValue();

        int cyan = 0xFF3A98FF;
        int fill = 0x553A98FF;
        List<WorldMeshRenderer.Line> lines = new ArrayList<>();
        List<WorldMeshRenderer.PlaneRect> quads = new ArrayList<>();

        for (Map.Entry<BlockPos, Long> entry : new ArrayList<>(renderPositions.entrySet())) {
            BlockPos pos = entry.getKey();
            long elapsed = now - entry.getValue();
            if (elapsed > holdMs) {
                continue;
            }

            float fade = renderSettings.isSelected("Animate")
                    ? 1.0F - Mth.clamp(elapsed / holdMs, 0.0F, 1.0F)
                    : 1.0F;

            AABB box = new AABB(pos);
            if (renderSettings.isSelected("Block")) {
                double m = box.minX + 0.001D;
                drawBox(lines, quads, m, box.minY - 0.001D, box.minZ + 0.001D, box.maxX - 0.001D, box.maxY + 0.001D, box.maxZ - 0.001D, fill, (int) (120.0F * fade));
            }
            if (renderSettings.isSelected("Rect")) {
                drawBox(lines, quads, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, fill, (int) (200.0F * fade));
            }
        }

        WorldMeshRenderer.render(new WorldMeshRenderer.WorldMesh(lines, List.of(), quads));
    }

    private void drawBox(List<WorldMeshRenderer.Line> lines, List<WorldMeshRenderer.PlaneRect> quads,
                         double x1, double y1, double z1, double x2, double y2, double z2,
                         int fill, int alpha) {
        Vec3 c1 = new Vec3(x1, y1, z1);
        Vec3 c2 = new Vec3(x2, y1, z1);
        Vec3 c3 = new Vec3(x2, y1, z2);
        Vec3 c4 = new Vec3(x1, y1, z2);
        Vec3 t1 = new Vec3(x1, y2, z1);
        Vec3 t2 = new Vec3(x2, y2, z1);
        Vec3 t3 = new Vec3(x2, y2, z2);
        Vec3 t4 = new Vec3(x1, y2, z2);
        int color = withAlpha(cyan(), alpha);
        lines.add(new WorldMeshRenderer.Line(c1, c2, color));
        lines.add(new WorldMeshRenderer.Line(c2, c3, color));
        lines.add(new WorldMeshRenderer.Line(c3, c4, color));
        lines.add(new WorldMeshRenderer.Line(c4, c1, color));
        lines.add(new WorldMeshRenderer.Line(t1, t2, color));
        lines.add(new WorldMeshRenderer.Line(t2, t3, color));
        lines.add(new WorldMeshRenderer.Line(t3, t4, color));
        lines.add(new WorldMeshRenderer.Line(t4, t1, color));
        lines.add(new WorldMeshRenderer.Line(c1, t1, color));
        lines.add(new WorldMeshRenderer.Line(c2, t2, color));
        lines.add(new WorldMeshRenderer.Line(c3, t3, color));
        lines.add(new WorldMeshRenderer.Line(c4, t4, color));
        quads.add(new WorldMeshRenderer.PlaneRect(
                new Vec3((x1 + x2) / 2.0D, y2, (z1 + z2) / 2.0D),
                new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D),
                (x2 - x1) / 2.0D, (z2 - z1) / 2.0D, withAlpha(fill, alpha)
        ));
    }

    private int cyan() {
        return 0x3A98FF;
    }

    private int withAlpha(int argb, int alpha) {
        int a = (int) (255.0F * Mth.clamp(alpha / 255.0F, 0.0F, 1.0F));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public void reset() {
        facePlacing = false;
        rotated = false;
        rotating = false;
        renderDamage = 0.0F;
        renderSelfDamage = 0.0F;
        currentId = 0L;
        lastTargetId = -1;

        placeTicks = 0;
        breakTicks = 0;
        calcTicks = 0;
        placeSyncTicks = 0;
        rotationTicks = 0;

        renderPositions.clear();
        crystalTracker.reset();
        bestCrystal = null;
        bestPosition = null;
        secondaryCrystal = null;
        currentData = null;
        target = null;
        rotationVec = null;
        currentState = State.NoTarget;
        placedOnSpawn = false;
        initialized = false;
        RotationContext.clear();
    }

    private void tickTimers() {
        placeTicks++;
        breakTicks++;
        calcTicks++;
        placeSyncTicks++;
    }

    private void doAction() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (target == null) {
            return;
        }

        if (sequential.is("Off")) {
            if (bestCrystal != null && passedTicks(breakTicks, (int) breakDelay.getValue().doubleValue())) {
                attackCrystal(bestCrystal);
            } else if (bestPosition != null
                    && passedTicks(placeTicks, (int) placeDelay.getValue().doubleValue())
                    && !placedOnSpawn) {
                placeCrystal(bestPosition, false, false);
            }
        } else {
            if (bestCrystal != null && passedTicks(breakTicks, (int) breakDelay.getValue().doubleValue())) {
                attackCrystal(bestCrystal);
            }
            if (bestPosition != null
                    && passedTicks(placeTicks, (int) placeDelay.getValue().doubleValue())
                    && !placedOnSpawn) {
                placeCrystal(bestPosition, false, false);
            }
        }
        placedOnSpawn = false;
    }

    private void clearCombatContext() {
        bestCrystal = null;
        bestPosition = null;
        secondaryCrystal = null;
        currentData = null;
        rotationVec = null;
        facePlacing = false;
    }

    private void processAwaitingSpawnConfirm() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        Map<BlockPos, CrystalTracker.Attempt> awaiting = crystalTracker.getAwaitingPositions();
        if (awaiting.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockPos, CrystalTracker.Attempt> entry : new ArrayList<>(awaiting.entrySet())) {
            BlockPos bp = entry.getKey();
            AABB searchBox = new AABB(bp.above()).inflate(0.9D, 0.8D, 0.9D);
            List<EndCrystal> nearby = new ArrayList<>();
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof EndCrystal crystal) {
                    if (crystal.isAlive() && !crystal.isRemoved()
                            && crystal.getBoundingBox().intersects(searchBox)
                            && crystal.distanceToSqr(Vec3.atCenterOf(bp)) < 0.3D) {
                        nearby.add(crystal);
                    }
                }
            }
            if (nearby.isEmpty()) {
                continue;
            }

            crystalTracker.confirmSpawn(bp);
            if (passedTicks(breakTicks, (int) breakDelay.getValue().doubleValue())) {
                handleSpawn(nearby.get(0));
            }
        }
    }

    private void handleSpawn(EndCrystal crystal) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!canAttackCrystal(crystal)) {
            return;
        }

        attackCrystal(crystal);

        if (sequential.is("Strong") && passedTicks(placeTicks, (int) placeDelay.getValue().doubleValue())) {
            calcPosition((float) placeRange.getValue().doubleValue(), mc.player.position());
            if (bestPosition != null) {
                placeCrystal(bestPosition, false, true);
            }
        }
    }

    private void calcRotations() {
        Minecraft mc = Minecraft.getInstance();
        if (rotate.is("Off") || shouldPause() || mc.player == null) {
            rotated = true;
            rotating = false;
            return;
        }

        Vec3 vec = null;
        if (rotationVec != null) {
            vec = rotationVec;
        } else if (bestPosition != null) {
            vec = bestPosition.getLocation();
        } else if (bestCrystal != null) {
            vec = bestCrystal.getBoundingBox().getCenter();
        }

        if (vec == null) {
            rotated = true;
            rotating = false;
            return;
        }

        rotated = tryRotate(vec);
        rotating = true;

        if (rotationVec != null && --rotationTicks < 0) {
            rotationVec = null;
        }
    }

    private Rotation targetRotation(Vec3 vec) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 diff = vec.subtract(eye);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        return new Rotation(yaw, pitch);
    }

    private Rotation limitYaw(Rotation desired) {
        Minecraft mc = Minecraft.getInstance();
        if (!yawStep.getValue()) {
            return desired;
        }
        float baseYaw = mc.player.getYRot();
        float deltaYaw = Mth.wrapDegrees(desired.yaw - baseYaw);
        float limitedDelta = Mth.clamp(deltaYaw, (float) -yawAngle.getValue(), (float) yawAngle.getValue().doubleValue());
        return new Rotation(baseYaw + limitedDelta, desired.pitch);
    }

    private boolean tryRotate(Vec3 vec) {
        Minecraft mc = Minecraft.getInstance();
        Rotation desired = limitYaw(targetRotation(vec));

        if (rotate.is("Instant")) {
            RotationContext.setRotation(desired.yaw, desired.pitch);
        } else {
            float yawSpeed = rotate.is("Stepped") ? 60.0F : 20.0F;
            float currentYaw = RotationContext.getServerYaw();
            float currentPitch = RotationContext.getServerPitch();
            float deltaYaw = Mth.clamp(Mth.wrapDegrees(desired.yaw - currentYaw), -yawSpeed, yawSpeed);
            float deltaPitch = Mth.clamp(desired.pitch - currentPitch, -yawSpeed, yawSpeed);
            RotationContext.apply(deltaYaw, deltaPitch);
        }

        float serverYaw = RotationContext.getServerYaw();
        float serverPitch = RotationContext.getServerPitch();
        float deltaYaw = Math.abs(Mth.wrapDegrees(desired.yaw - serverYaw));
        float deltaPitch = Math.abs(Mth.wrapDegrees(desired.pitch - serverPitch));
        return deltaYaw <= 8.0F && deltaPitch <= 8.0F;
    }

    private void attackCrystal(EndCrystal crystal) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null || crystal == null) {
            return;
        }

        if ((crystalTracker.isDead(crystal.getId()) && inhibit.getValue())
                || shouldPause() || target == null) {
            return;
        }

        MobEffectInstance weakness = player.getEffect(MobEffects.WEAKNESS);
        MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);

        int prevSlot = -1;
        HotbarSearch antiWeaknessHotbar = findAntiWeaknessHotbar();
        int antiWeaknessInv = findAntiWeaknessInventory();

        if (!antiWeakness.is("NONE")
                && weakness != null
                && (strength == null || strength.getAmplifier() < weakness.getAmplifier())) {
            prevSlot = switchTo(antiWeaknessHotbar, antiWeaknessInv, antiWeakness);
        }

        if (!rotate.is("Off")) {
            Vec3 center = crystal.getBoundingBox().getCenter();
            tryRotate(center);
        }

        player.connection.send(new ServerboundAttackPacket(crystal.getId()));
        swingHand(false, true);

        breakTicks = 0;
        crystalTracker.onAttack(crystal, breakFailsafe.getValue(), (int) attempts.getValue().doubleValue());
        rotationTicks = 10;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal exCrystal
                    && exCrystal.distanceToSqr(crystal.getX(), crystal.getY(), crystal.getZ()) <= 144.0
                    && !crystalTracker.isDead(exCrystal.getId())) {
                crystalTracker.setDead(exCrystal.getId(), System.currentTimeMillis());
            }
        }

        if (prevSlot != -1) {
            if (antiWeakness.is("SILENT")) {
                player.getInventory().setSelectedSlot(prevSlot);
                player.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
            } else if (antiWeakness.is("INVENTORY")) {
                swapMenu(prevSlot, player.getInventory().getSelectedSlot());
            }
        }
    }

    private boolean canAttackCrystal(EndCrystal crystal) {
        Minecraft mc = Minecraft.getInstance();
        if (crystal == null || target == null || mc.player == null || mc.level == null) {
            return false;
        }

        double distanceSq = mc.player.getEyePosition().distanceToSqr(crystal.getBoundingBox().getCenter());
        double maxRangeSq = canSee(crystal.position())
                ? square((float) explodeRange.getValue().doubleValue())
                : square((float) explodeWallRange.getValue().doubleValue());
        if (distanceSq > maxRangeSq) {
            return false;
        }

        if (!crystal.isAlive()) {
            return false;
        }

        float damage = getCrystalDamage(crystal.position(), target);
        float selfDamage = protectionAdjustedSelfDamage(crystal.position(), damage);

        return !(selfDamage > (float) maxSelfDamage.getValue().doubleValue()) || shouldOverrideMaxSelfDmg(damage, selfDamage);
    }

    private int switchTo(HotbarSearch hotbar, int invSlot, ModeSetting switchMode) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            return -1;
        }

        int prevSlot = player.getInventory().getSelectedSlot();
        if (switchMode.is("INVENTORY")) {
            if (invSlot != -1) {
                prevSlot = invSlot;
                swapMenu(invSlot, player.getInventory().getSelectedSlot());
            }
        } else if (switchMode.is("NORMAL") || switchMode.is("SILENT")) {
            if (hotbar.found()) {
                player.getInventory().setSelectedSlot(hotbar.slot());
                player.connection.send(new ServerboundSetCarriedItemPacket(hotbar.slot()));
            }
        }

        return prevSlot;
    }

    private void swapMenu(int menuSlot, int hotbarSlot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            return;
        }
        mc.gameMode.handleContainerInput(
                player.inventoryMenu.containerId,
                menuSlot,
                hotbarSlot,
                ContainerInput.SWAP,
                player
        );
    }

    private void placeCrystal(BlockHitResult bhr, boolean packetRotate, boolean onSpawn) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (shouldPause() || player == null || bhr == null) {
            return;
        }

        int prevSlot = -1;

        HotbarSearch crystalResult = findInHotbar(Items.END_CRYSTAL);
        int crystalResultInv = findInInventory(Items.END_CRYSTAL);

        boolean offhand = player.getOffhandItem().getItem() instanceof net.minecraft.world.item.EndCrystalItem;
        boolean holdingCrystal = player.getMainHandItem().getItem() instanceof net.minecraft.world.item.EndCrystalItem || offhand;

        if (!rotate.is("Off")) {
            rotationVec = bhr.getLocation();
            rotationTicks = 10;
            tryRotate(bhr.getLocation());
            if (!packetRotate && !rotated) {
                return;
            }
        }

        if (isPositionBlockedByEntity(bhr.getBlockPos(), false)) {
            return;
        }

        if (!autoSwitch.is("NONE") && !holdingCrystal) {
            prevSlot = switchTo(crystalResult, crystalResultInv, autoSwitch);
        }

        holdingCrystal = player.getMainHandItem().getItem() instanceof net.minecraft.world.item.EndCrystalItem
                || player.getOffhandItem().getItem() instanceof net.minecraft.world.item.EndCrystalItem
                || autoSwitch.is("SILENT");
        if (!holdingCrystal) {
            return;
        }

        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean accepted = mc.gameMode.useItemOn(player, hand, bhr).consumesAction();
        if (!accepted) {
            return;
        }

        swingHand(offhand, false);

        if (passedTicks(breakTicks, (int) breakDelay.getValue().doubleValue()) && idPredict.getValue()) {
            predictAttack();
        }

        placeTicks = 0;
        rotationTicks = 10;

        crystalTracker.addAwaitingPos(bhr.getBlockPos(), placeFailsafe.getValue());
        renderPositions.put(bhr.getBlockPos().immutable(), System.currentTimeMillis());
        postPlaceSwitch(prevSlot);

        if (onSpawn) {
            placedOnSpawn = true;
            placeSyncTicks = 0;
        }
    }

    private void postPlaceSwitch(int slot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        if (autoSwitch.is("SILENT") && slot != -1) {
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }

        if (autoSwitch.is("INVENTORY") && slot != -1) {
            swapMenu(slot, player.getInventory().getSelectedSlot());
        }
    }

    private void calcPosition(float range, Vec3 center) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        calcTicks = 0;

        if (target == null) {
            bestPosition = null;
            currentData = null;
            return;
        }

        List<PlaceData> list = getPossibleBlocks(target, center, range).stream()
                .filter(data -> isSafe(data.damage, data.selfDamage, data.overrideDamage))
                .toList();

        bestPosition = list.isEmpty() ? null : filterPositions(list);
    }

    private List<PlaceData> getPossibleBlocks(Player target, Vec3 center, float range) {
        Minecraft mc = Minecraft.getInstance();
        List<PlaceData> blocks = new ArrayList<>();
        BlockPos playerPos = BlockPos.containing(center);
        Vec3 predictedPlayerPos = predictedPlayerPos();
        int r = Mth.ceil(range);
        double scanRangeSq = square(range + 1.0F);

        for (int x = playerPos.getX() - r; x <= playerPos.getX() + r; x++) {
            for (int y = playerPos.getY() - r; y <= playerPos.getY() + r; y++) {
                for (int z = playerPos.getZ() - r; z <= playerPos.getZ() + r; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (Vec3.atCenterOf(bp).distanceToSqr(center) > scanRangeSq) {
                        continue;
                    }

                    PlaceData data = getPlaceData(bp, target, predictedPlayerPos);
                    if (data != null) {
                        blocks.add(data);
                    }
                }
            }
        }

        return blocks;
    }

    private List<CrystalData> getPossibleCrystals(Player target) {
        Minecraft mc = Minecraft.getInstance();
        List<CrystalData> crystals = new ArrayList<>();
        if (mc.player == null || mc.level == null) {
            return crystals;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) {
                continue;
            }

            if (crystalTracker.isBlocked(crystal.getId())) {
                continue;
            }

            if (crystalTracker.isDead(crystal.getId()) && inhibit.getValue()) {
                continue;
            }

            double maxRangeSq = canSee(crystal.position())
                    ? square((float) explodeRange.getValue().doubleValue())
                    : square((float) explodeWallRange.getValue().doubleValue());
            if (mc.player.getEyePosition().distanceToSqr(crystal.getBoundingBox().getCenter()) > maxRangeSq) {
                continue;
            }

            if (!crystal.isAlive()) {
                continue;
            }

            float damage = getCrystalDamage(crystal.position(), target);
            float selfDamage = protectionAdjustedSelfDamage(crystal.position(), damage);

            if (damage < 1.5F) {
                continue;
            }
            if (selfDamage > (float) maxSelfDamage.getValue().doubleValue() && !shouldOverrideMaxSelfDmg(damage, selfDamage)) {
                continue;
            }

            crystals.add(new CrystalData(crystal, damage, selfDamage, shouldOverrideMaxSelfDmg(damage, selfDamage)));
        }

        return crystals;
    }

    private void getCrystalToExplode() {
        Minecraft mc = Minecraft.getInstance();
        if (target == null) {
            bestCrystal = null;
            return;
        }

        if (secondaryCrystal != null) {
            bestCrystal = canAttackCrystal(secondaryCrystal) ? secondaryCrystal : null;
            secondaryCrystal = null;
            return;
        }

        List<CrystalData> list = getPossibleCrystals(target).stream()
                .filter(data -> isSafe(data.damage, data.selfDamage, data.overrideDamage))
                .toList();
        bestCrystal = list.isEmpty() ? null : filterCrystals(list);
    }

    private boolean isSafe(float damage, float selfDamage, boolean overrideDamage) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        if (overrideDamage) {
            return true;
        }

        if (selfDamage + 0.5F > mc.player.getHealth() + mc.player.getAbsorptionAmount()) {
            return false;
        }

        if (efficiency.getValue()) {
            return damage / Math.max(0.1F, selfDamage) >= (float) efficiencyFactor.getValue().doubleValue();
        }

        return true;
    }

    private BlockHitResult filterPositions(List<PlaceData> clearedList) {
        Minecraft mc = Minecraft.getInstance();
        PlaceData bestData = null;
        float bestVal = 0.0F;

        for (PlaceData data : clearedList) {
            if (!(shouldOverrideMinDmg(data.damage) || data.damage > (float) minDamage.getValue().doubleValue())) {
                continue;
            }

            if (bestData != null
                    && data.overrideDamage
                    && target.getAbsorptionAmount() + target.getHealth() < bestData.damage
                    && bestData.selfDamage < data.selfDamage) {
                continue;
            }

            boolean shouldStopOverride = bestData != null
                    && bestData.overrideDamage
                    && data.damage > target.getHealth() + target.getAbsorptionAmount()
                    && data.selfDamage < bestData.selfDamage;

            float safetyComparatorDelta = shouldStopOverride ? 10.0F : 1.0F;

            if (bestData != null
                    && Math.abs(bestData.damage - data.damage) < safetyComparatorDelta
                    && Math.abs(bestData.selfDamage - data.selfDamage) > 1.0F) {
                if (bestData.selfDamage >= data.selfDamage) {
                    bestData = data;
                    bestVal = data.damage;
                }
            } else if (bestVal < data.damage) {
                bestData = data;
                bestVal = data.damage;
            }
        }

        if (bestData == null) {
            return null;
        }

        facePlacing = bestData.damage < (float) minDamage.getValue().doubleValue();
        renderDamage = bestData.damage;
        renderSelfDamage = bestData.selfDamage;
        currentData = bestData;
        return bestData.bhr;
    }

    private EndCrystal filterCrystals(List<CrystalData> clearedList) {
        CrystalData bestData = null;
        float bestVal = 0.0F;

        for (CrystalData data : clearedList) {
            if (!(shouldOverrideMinDmg(data.damage) || data.damage > (float) minDamage.getValue().doubleValue())) {
                continue;
            }

            if (bestData != null
                    && data.overrideDamage
                    && target.getAbsorptionAmount() + target.getHealth() < bestData.damage
                    && bestData.selfDamage < data.selfDamage) {
                continue;
            }

            boolean shouldStopOverride = bestData != null
                    && bestData.overrideDamage
                    && data.damage > target.getHealth() + target.getAbsorptionAmount()
                    && data.selfDamage < bestData.selfDamage;

            float safetyComparatorDelta = shouldStopOverride ? 10.0F : 1.0F;

            if (bestData != null
                    && Math.abs(bestData.damage - data.damage) < safetyComparatorDelta
                    && Math.abs(bestData.selfDamage - data.selfDamage) > 1.0F) {
                if (bestData.selfDamage >= data.selfDamage) {
                    bestData = data;
                    bestVal = data.damage;
                }
            } else if (bestVal < data.damage) {
                bestData = data;
                bestVal = data.damage;
            }
        }

        if (bestData == null) {
            return null;
        }

        renderDamage = bestData.damage;
        renderSelfDamage = bestData.selfDamage;
        return bestData.crystal;
    }

    private PlaceData getPlaceData(BlockPos bp, Player target, Vec3 predictedPlayerPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }

        if (crystalTracker.isPositionBlocked(bp, placeFailsafe.getValue(), (int) attempts.getValue().doubleValue())) {
            return null;
        }

        if (!predictCrystalSpawn(bp, predictedPlayerPos)) {
            return null;
        }

        if (target != null
                && target.position().distanceToSqr(Vec3.atCenterOf(bp).add(0.0D, 0.5D, 0.0D)) > 144.0D) {
            return null;
        }

        net.minecraft.world.level.block.Block base = mc.level.getBlockState(bp).getBlock();
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK) {
            return null;
        }

        if (!mc.level.getBlockState(bp.above()).isAir()) {
            return null;
        }

        if (isPositionBlockedByEntity(bp, true)) {
            return null;
        }

        Vec3 crystalVec = Vec3.atCenterOf(bp.above());
        BlockHitResult interactResult = getInteractResult(bp, crystalVec);
        if (interactResult == null) {
            return null;
        }

        float damage = target == null ? 10.0F : getCrystalDamage(crystalVec, target);
        if (damage < 1.5F) {
            return null;
        }

        float selfDamage = protectionAdjustedSelfDamage(crystalVec, damage);

        if (selfDamage > (float) maxSelfDamage.getValue().doubleValue()
                && !shouldOverrideMaxSelfDmg(damage, selfDamage)) {
            return null;
        }

        return new PlaceData(interactResult, damage, selfDamage, shouldOverrideMaxSelfDmg(damage, selfDamage));
    }

    private float protectionAdjustedSelfDamage(Vec3 crystalPos, float damage) {
        Minecraft mc = Minecraft.getInstance();
        float selfDamage = getCrystalDamage(crystalPos, mc.player);

        if (protectFriends.getValue()) {
            for (AbstractClientPlayer player : mc.level.players()) {
                if (player == null || player == mc.player
                        || !FriendManager.INSTANCE.isFriend(player.getGameProfile().name())) {
                    continue;
                }
                float friendDamage = getCrystalDamage(crystalPos, player);
                if (friendDamage > selfDamage) {
                    selfDamage = friendDamage;
                }
            }
        }

        return selfDamage;
    }

    private boolean predictCrystalSpawn(BlockPos bp, Vec3 predictedPlayerPos) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 predictedPos = Vec3.atCenterOf(bp).add(0.0D, 1.5D, 0.0D);
        Vec3 predictedEyes = predictedPlayerPos.add(mc.player.getEyePosition().subtract(mc.player.position()));
        double distanceSq = predictedEyes.distanceToSqr(predictedPos);

        if (canSee(predictedPos)) {
            return distanceSq <= square((float) explodeRange.getValue().doubleValue());
        }
        return distanceSq <= square((float) explodeWallRange.getValue().doubleValue());
    }

    private BlockHitResult getInteractResult(BlockPos bp, Vec3 crystalVec) {
        return interact.is("Strict") ? getStrictInteract(bp) : getDefaultInteract(crystalVec, bp);
    }

    private BlockHitResult getDefaultInteract(Vec3 crystalVector, BlockPos bp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }

        if (mc.player.getEyePosition().distanceToSqr(crystalVector) > square((float) placeRange.getValue().doubleValue())) {
            return null;
        }

        HitResult wallCheck = mc.level.clip(new ClipContext(
                mc.player.getEyePosition(),
                crystalVector,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (wallCheck instanceof BlockHitResult blockHit
                && blockHit.getType() == HitResult.Type.BLOCK
                && !blockHit.getBlockPos().equals(bp)
                && mc.player.getEyePosition().distanceToSqr(crystalVector) > square((float) placeWallRange.getValue().doubleValue())) {
            return null;
        }

        Direction side = mc.level.isInWorldBounds(bp.above()) ? Direction.UP : Direction.DOWN;
        return new BlockHitResult(crystalVector, side, bp, false);
    }

    private BlockHitResult getStrictInteract(BlockPos bp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }

        float bestDistance = Float.MAX_VALUE;
        Direction bestDirection = null;
        Vec3 bestVec = null;

        float upPoint = strictCenter.getValue()
                ? (float) Vec3.atCenterOf(bp).y
                : bp.above().getY();
        if (mc.player.getEyePosition().y > upPoint) {
            bestDirection = Direction.UP;
        } else if (mc.player.getEyePosition().y < bp.getY()
                && mc.level.getBlockState(bp.below()).isAir()) {
            bestDirection = Direction.DOWN;
        }

        List<Direction> directions = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        if (bestDirection != null) {
            directions.add(bestDirection);
        }

        for (Direction dir : directions) {
            Vec3 vec = Vec3.atCenterOf(bp.offset(dir.getStepX(), dir.getStepY(), dir.getStepZ()));
            if (!mc.level.getBlockState(bp.offset(dir.getStepX(), dir.getStepY(), dir.getStepZ())).canBeReplaced()) {
                continue;
            }

            double distanceSq = mc.player.getEyePosition().distanceToSqr(vec);
            double maxDistanceSq = canSee(vec)
                    ? square((float) placeRange.getValue().doubleValue())
                    : square((float) placeWallRange.getValue().doubleValue());
            if (distanceSq > maxDistanceSq) {
                continue;
            }

            if (distanceSq < bestDistance) {
                bestDistance = (float) distanceSq;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }

        AABB box = new AABB(base.above()).inflate(0.0D, 1.0D, 0.0D);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || !entity.isAlive() || !entity.getBoundingBox().intersects(box)) {
                continue;
            }

            if (entity instanceof ExperienceOrb) {
                continue;
            }

            if (entity instanceof EndCrystal crystal) {
                if (crystalTracker.isDead(crystal.getId())) {
                    continue;
                }

                if (crystalTracker.isBlocked(crystal.getId())) {
                    return true;
                }

                if (calcPhase) {
                    if (canAttackCrystal(crystal)) {
                        continue;
                    }
                } else if (crystal.position().distanceToSqr(box.getCenter()) > 0.3D) {
                    secondaryCrystal = crystal;
                }
            }

            return true;
        }
        return false;
    }

    private boolean shouldOverrideMaxSelfDmg(float damage, float selfDamage) {
        Minecraft mc = Minecraft.getInstance();
        if (!overrideSelfDamage.getValue() || target == null || mc.player == null || mc.level == null) {
            return false;
        }

        boolean targetSafe = target.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING
                || target.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING;
        boolean playerSafe = mc.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING
                || mc.player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING;

        float targetHp = target.getHealth() + target.getAbsorptionAmount();
        float playerHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        boolean canPop = damage > targetHp && targetSafe;
        boolean canKill = damage > targetHp && !targetSafe;
        boolean canPopSelf = selfDamage > playerHp && playerSafe;
        boolean canKillSelf = selfDamage > playerHp && !playerSafe;

        if (canPopSelf && canKill && sacrificeTotem.getValue()) {
            return true;
        }

        return selfDamage > (float) maxSelfDamage.getValue().doubleValue() && (canPop || canKill) && !canKillSelf && !canPopSelf;
    }

    private boolean shouldOverrideMinDmg(float damage) {
        Minecraft mc = Minecraft.getInstance();
        if (target == null) {
            return false;
        }

        if (facePlaceButton.getValue() != InputBindSetting.UNBOUND) {
            long handle = mc.getWindow() == null ? -1L : mc.getWindow().handle();
            if (handle != -1L && mc.getWindow() != null
                    && GLFW.glfwGetKey(handle, facePlaceButton.getValue()) == GLFW.GLFW_PRESS) {
                return true;
            }
        }

        if ((target.getHealth() + target.getAbsorptionAmount()) - damage < 0.0F) {
            return true;
        }

        if (armorBreaker.getValue()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{
                    EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                    EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                ItemStack armor = target.getItemBySlot(slot);
                if (armor == null || armor.isEmpty() || !armor.isDamageableItem()) {
                    continue;
                }
                float durabilityPercent = ((armor.getMaxDamage() - armor.getDamageValue()) / (float) armor.getMaxDamage()) * 100.0F;
                if (durabilityPercent < (float) armorScale.getValue().doubleValue()) {
                    return true;
                }
            }
        }

        return target.getHealth() + target.getAbsorptionAmount() <= (float) facePlaceHp.getValue().doubleValue();
    }

    private void swingHand(boolean offHand, boolean attack) {
        Minecraft mc = Minecraft.getInstance();
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (swingMode.is("Both")) {
            player_swing(hand);
        } else if (swingMode.is("Break")) {
            if (attack) {
                player_swing(InteractionHand.MAIN_HAND);
            } else {
                player_connection_send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        } else if (swingMode.is("Place")) {
            if (!attack) {
                player_swing(hand);
            } else {
                player_connection_send(new ServerboundSwingPacket(hand));
            }
        } else if (swingMode.is("ServerSide")) {
            player_connection_send(new ServerboundSwingPacket(hand));
        } else {
            player_swing(hand);
        }
    }

    private void player_swing(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.swing(hand);
        }
    }

    private void player_connection_send(Object packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.send((net.minecraft.network.protocol.Packet<?>) packet);
        }
    }

    private void predictAttack() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        int attacks = (int) idAttacks.getValue().doubleValue();
        for (int i = 1; i <= attacks; i++) {
            int id = (int) (currentId + i);
            Entity entity = mc.level.getEntity(id);
            if (entity == null || entity instanceof EndCrystal) {
                player.connection.send(new ServerboundAttackPacket(id));
                player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
    }

    private void processSpawnPacket(int id) {
        if (id > currentId) {
            currentId = id;
        }
    }

    private void confirmAwaitingBySpawn(int entityId, Vec3 spawnPos) {
        Minecraft mc = Minecraft.getInstance();
        if (spawnPos == null) {
            return;
        }

        Map<BlockPos, CrystalTracker.Attempt> awaiting = crystalTracker.getAwaitingPositions();
        if (awaiting.isEmpty()) {
            return;
        }

        for (Map.Entry<BlockPos, CrystalTracker.Attempt> entry : new ArrayList<>(awaiting.entrySet())) {
            BlockPos bp = entry.getKey();
            if (spawnPos.distanceToSqr(Vec3.atCenterOf(bp)) > 0.36D) {
                continue;
            }

            crystalTracker.confirmSpawn(bp);

            if (passedTicks(breakTicks, (int) breakDelay.getValue().doubleValue())) {
                Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
                if (entity instanceof EndCrystal crystal) {
                    handleSpawn(crystal);
                } else {
                    attackSpawnById(entityId);
                }
            }
            break;
        }
    }

    private void attackSpawnById(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || target == null || shouldPause()) {
            return;
        }

        player.connection.send(new ServerboundAttackPacket(entityId));
        swingHand(false, true);
        crystalTracker.setDead(entityId, System.currentTimeMillis());
        breakTicks = 0;
        rotationTicks = 10;
    }

    private int getSelfPredictTicks() {
        return (int) Math.ceil((getPing() * 1.5F) / 50.0F);
    }

    private Vec3 predictedPlayerPos() {
        Minecraft mc = Minecraft.getInstance();
        Vec3 pos = mc.player.position();
        Vec3 vel = mc.player.getDeltaMovement();
        int ticks = getSelfPredictTicks();
        return pos.add(vel.x * ticks, 0.0D, vel.z * ticks);
    }

    private float getPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) {
            return 0.0F;
        }
        var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return entry == null ? 0.0F : entry.getLatency();
    }

    private boolean shouldPause() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            return true;
        }

        boolean offhand = player.getOffhandItem().getItem() instanceof net.minecraft.world.item.EndCrystalItem;
        boolean mainHand = player.getMainHandItem().getItem() instanceof net.minecraft.world.item.EndCrystalItem;

        if (mc.gameMode.isDestroying() && !offhand && miningPause.getValue()) {
            currentState = State.Mining;
            return true;
        }

        if (autoSwitch.is("NONE") && !offhand && !mainHand) {
            currentState = State.NoCrystalls;
            return true;
        }

        if ((autoSwitch.is("SILENT") || autoSwitch.is("NORMAL")) && !findInHotbar(Items.END_CRYSTAL).found && !offhand) {
            currentState = State.NoCrystalls;
            return true;
        }

        if (autoSwitch.is("INVENTORY") && findAny(Items.END_CRYSTAL) == -1 && !offhand) {
            currentState = State.NoCrystalls;
            return true;
        }

        if (player.isUsingItem() && eatingPause.getValue()) {
            currentState = State.Eating;
            return true;
        }

        if (rotationMarkedDirty()) {
            currentState = State.ExternalPause;
            return true;
        }

        if (player.getHealth() + player.getAbsorptionAmount() < (float) pauseHP.getValue().doubleValue()) {
            currentState = State.LowHP;
            return true;
        }

        currentState = State.Active;
        return false;
    }

    private boolean rotationMarkedDirty() {
        Minecraft mc = Minecraft.getInstance();
        if (surroundPause.getValue() && SurroundFeature.INSTANCE.isEnabled()) {
            return true;
        }
        return inventoryPause.getValue() && mc.gui.screen() != null;
    }

    private Player findTarget(float range, String logic) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }

        Player best = null;
        double bestValue = Double.MAX_VALUE;
        boolean byHealth = "Health".equalsIgnoreCase(logic);

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == null || player == mc.player || !player.isAlive() || player.isSpectator()) {
                continue;
            }

            if (FriendManager.INSTANCE.isFriend(player.getGameProfile().name())) {
                continue;
            }

            double dist = mc.player.distanceToSqr(player);
            if (dist > (double) range * range) {
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return new HotbarSearch(false, -1);
        }
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == item) {
                return new HotbarSearch(true, i);
            }
        }
        return new HotbarSearch(false, -1);
    }

    private int findInInventory(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return -1;
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findAny(Item item) {
        HotbarSearch hotbar = findInHotbar(item);
        if (hotbar.found) {
            return hotbar.slot;
        }
        return findInInventory(item);
    }

    private HotbarSearch findAntiWeaknessHotbar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return new HotbarSearch(false, -1);
        }
        for (int i = 0; i < 9; i++) {
            if (isMeleeTool(mc.player.getInventory().getItem(i).getItem())) {
                return new HotbarSearch(true, i);
            }
        }
        return new HotbarSearch(false, -1);
    }

    private int findAntiWeaknessInventory() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return -1;
        }
        for (int i = 9; i < 36; i++) {
            if (isMeleeTool(mc.player.getInventory().getItem(i).getItem())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isMeleeTool(Item item) {
        return item instanceof AxeItem
                || item instanceof ShovelItem
                || item instanceof TridentItem
                || item instanceof HoeItem;
    }

    private void sendPacket(net.minecraft.network.protocol.Packet<?> packet) {
        player_connection_send(packet);
    }

    private boolean passedTicks(int counter, int delay) {
        return counter >= Math.max(0, delay);
    }

    private void cleanupRenderPositions() {
        long now = System.currentTimeMillis();
        long holdMs = (long) (double) renderHoldMs.getValue();
        renderPositions.entrySet().removeIf(entry -> now - entry.getValue() > holdMs);
    }

    private double square(double value) {
        return value * value;
    }

    private float getCrystalDamage(Vec3 crystalPos, LivingEntity player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || player == null || !player.isAlive()) {
            return 0.0F;
        }
        double distSq = player.distanceToSqr(crystalPos);
        if (distSq > 144.0D) {
            return 0.0F;
        }
        double distFactor = 1.0D - distSq / 144.0D;
        float exposure = calculateExposure(player, crystalPos);
        double finalExposure = distFactor * exposure;
        float damage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0D * 7.0D * 12.0D + 1.0D);
        DamageSource source = Explosion.getDefaultDamageSource(mc.level, null);
        float armor = (float) player.getArmorValue();
        float toughness = (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float reduced = CombatRules.getDamageAfterAbsorb(player, damage, source, armor, toughness);
        return Math.max(reduced, 0.0F);
    }

    private float calculateExposure(LivingEntity entity, Vec3 explosionPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 1.0F;
        }
        Vec3 center = entity.getBoundingBox().getCenter();
        float exposure = 0.0F;
        for (double[] offset : EXPOSURE_OFFSETS) {
            Vec3 start = new Vec3(center.x + offset[0], center.y, center.z + offset[1]);
            BlockHitResult hit = mc.level.clip(new ClipContext(
                    start, explosionPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity
            ));
            if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
                exposure += 0.125F;
            }
        }
        return exposure;
    }

    private boolean canSee(Vec3 point) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        BlockHitResult hit = mc.level.clip(new ClipContext(
                mc.player.getEyePosition(), point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player
        ));
        return hit == null || hit.getType() != HitResult.Type.BLOCK;
    }

    private record PlaceData(BlockHitResult bhr, float damage, float selfDamage, boolean overrideDamage) {
    }

    private record CrystalData(EndCrystal crystal, float damage, float selfDamage, boolean overrideDamage) {
    }

    private record HotbarSearch(boolean found, int slot) {
    }

    private record Rotation(float yaw, float pitch) {
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

        void onAttack(EndCrystal crystal, boolean breakFailsafeEnabled, int maxAttempts) {
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
            this.awaitingPositions.put(pos.immutable(), new Attempt(System.currentTimeMillis(), placeFailsafeEnabled ? nextAttempt : 1));
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