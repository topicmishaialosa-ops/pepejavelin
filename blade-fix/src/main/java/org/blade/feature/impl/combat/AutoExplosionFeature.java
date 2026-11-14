package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.blade.context.RotationContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.render.Render3DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.utils.render.world.WorldMeshRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class AutoExplosionFeature extends Feature {
    public static final AutoExplosionFeature INSTANCE = new AutoExplosionFeature();

    private static final float PLACE_REACH = 4.5F;
    private static final long MIN_REACTION_MS = 200L;

    public final NumberSetting range = register(new NumberSetting(
            "Дистанция", 5.0D, 1.0D, 8.0D, 0.5D, "блоков"
    ));
    public final NumberSetting explodeDelay = register(new NumberSetting(
            "Задержка взрыва", 10.0D, 1.0D, 40.0D, 1.0D, " тиков"
    ));
    public final NumberSetting reactionTime = register(new NumberSetting(
            "Время реакции", 60.0D, 10.0D, 200.0D, 5.0D, " мс"
    ));
    public final NumberSetting breakRetries = register(new NumberSetting(
            "Попытки взрыва", 5.0D, 1.0D, 20.0D, 1.0D, ""
    ));
    public final BooleanSetting render = register(new BooleanSetting(
            "Визуализация", true
    ));

    private final Map<BlockPos, Block> snapshot = new HashMap<>();
    private final Map<BlockPos, Long> candidates = new HashMap<>();
    private final Map<BlockPos, Integer> explodeCountdown = new HashMap<>();
    private final Map<BlockPos, Integer> retries = new HashMap<>();

    public AutoExplosionFeature() {
        super("AutoExplosion", "Если игрок поставил обсидиан — ставит кристалл из хотбара и взрывает его", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        snapshot.clear();
        candidates.clear();
        explodeCountdown.clear();
        retries.clear();
        scan();
    }

    @Override
    public void onDisable() {
        snapshot.clear();
        candidates.clear();
        explodeCountdown.clear();
        retries.clear();
        RotationContext.clear();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = event.getClient();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        scan();
        processCandidates(player);
        processExplosions(player);
    }

    private void scan() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        int radius = Math.max(1, (int) Math.ceil(range.getValue()));
        BlockPos center = player.getBlockPosBelowThatAffectsMyMovement();
        Map<BlockPos, Block> current = new HashMap<>();
        long now = System.currentTimeMillis();

        for (int x = center.getX() - radius; x <= center.getX() + radius; ++x) {
            for (int y = center.getY() - 2; y <= center.getY() + 2; ++y) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; ++z) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.level.getBlockState(pos).getBlock();
                    current.put(pos, block);
                    if (block != Blocks.OBSIDIAN) {
                        continue;
                    }

                    Block previous = snapshot.get(pos);
                    if (previous == null || previous == Blocks.OBSIDIAN) {
                        continue;
                    }
                    if (!mc.level.getBlockState(pos.above()).isAir()) {
                        continue;
                    }
                    if (explodeCountdown.containsKey(pos) || candidates.containsKey(pos)) {
                        continue;
                    }
                    candidates.put(pos.immutable(), now);
                }
            }
        }

        snapshot.clear();
        snapshot.putAll(current);
    }

    private void processCandidates(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long maxAge = Math.max(MIN_REACTION_MS, (long) (double) reactionTime.getValue());
        Vec3 eye = player.getEyePosition();

        for (BlockPos pos : new HashSet<>(candidates.keySet())) {
            if (!mc.level.getBlockState(pos).is(Blocks.OBSIDIAN)
                    || !mc.level.getBlockState(pos.above()).isAir()) {
                candidates.remove(pos);
                continue;
            }

            if (now - candidates.get(pos) > maxAge) {
                candidates.remove(pos);
                continue;
            }

            if (eye.distanceTo(Vec3.atCenterOf(pos.above())) > PLACE_REACH) {
                continue;
            }

            if (placeCrystalAt(player, pos)) {
                candidates.remove(pos);
                explodeCountdown.put(pos.immutable(), Math.max(1, (int) (double) explodeDelay.getValue()));
            }
        }
    }

    private void processExplosions(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameMode == null) {
            return;
        }

        for (BlockPos pos : new HashSet<>(explodeCountdown.keySet())) {
            int ticks = explodeCountdown.get(pos) - 1;
            if (ticks > 0) {
                explodeCountdown.put(pos, ticks);
                continue;
            }

            EndCrystal crystal = findCrystal(pos);
            if (crystal != null) {
                attackCrystal(player, crystal);
                explodeCountdown.remove(pos);
                retries.remove(pos);
                continue;
            }

            int attempts = retries.getOrDefault(pos, 0);
            if (attempts >= (int) (double) breakRetries.getValue()) {
                explodeCountdown.remove(pos);
                retries.remove(pos);
                continue;
            }
            retries.put(pos, attempts + 1);

            if (placeCrystalAt(player, pos)) {
                explodeCountdown.put(pos, Math.max(2, (int) (double) explodeDelay.getValue() / 2));
            } else {
                explodeCountdown.put(pos, 2);
            }
        }
    }

    private boolean placeCrystalAt(LocalPlayer player, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.gameMode == null) {
            return false;
        }

        Vec3 placePos = new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        if (player.getEyePosition().distanceTo(placePos) > PLACE_REACH) {
            return false;
        }

        InteractionHand hand;
        int switchTo = -1;
        int previousSlot = player.getInventory().getSelectedSlot();

        if (player.getMainHandItem().is(Items.END_CRYSTAL)) {
            hand = InteractionHand.MAIN_HAND;
        } else if (player.getOffhandItem().is(Items.END_CRYSTAL)) {
            hand = InteractionHand.OFF_HAND;
        } else {
            switchTo = findHotbar(Items.END_CRYSTAL, player);
            if (switchTo == -1) {
                return false;
            }
            hand = InteractionHand.MAIN_HAND;
            player.getInventory().setSelectedSlot(switchTo);
            player.connection.send(new ServerboundSetCarriedItemPacket(switchTo));
        }

        rotateTo(player, placePos);
        BlockHitResult hit = new BlockHitResult(placePos, Direction.UP, pos, false);
        InteractionResult result = mc.gameMode.useItemOn(player, hand, hit);

        if (switchTo != -1) {
            player.getInventory().setSelectedSlot(previousSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(previousSlot));
        }

        return result.consumesAction();
    }

    private void attackCrystal(LocalPlayer player, EndCrystal crystal) {
        Vec3 center = crystal.getBoundingBox().getCenter();
        rotateTo(player, center);
        player.connection.send(new ServerboundAttackPacket(crystal.getId()));
        player.swing(InteractionHand.MAIN_HAND);
    }

    private void rotateTo(LocalPlayer player, Vec3 target) {
        Vec3 diff = target.subtract(player.getEyePosition());
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        RotationContext.setRotation(yaw, pitch);
    }

    private EndCrystal findCrystal(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }

        Vec3 center = Vec3.atCenterOf(pos.above());
        AABB box = new AABB(center.add(-2.0D, -2.0D, -2.0D), center.add(2.0D, 2.0D, 2.0D));
        EndCrystal best = null;
        double bestDistance = Double.MAX_VALUE;

        for (EndCrystal crystal : mc.level.getEntities(
                EntityTypeTest.forClass(EndCrystal.class),
                box,
                e -> e.isAlive() && !e.isRemoved())) {
            double distance = crystal.getBoundingBox().getCenter().distanceToSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = crystal;
            }
        }
        return best;
    }

    @EventTarget
    public void onRenderWorld(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!render.getValue() || player == null || mc.level == null) {
            return;
        }

        int cyan = 0xFF00E5FF;
        int fill = 0x5500E5FF;
        long now = System.currentTimeMillis();
        long maxAge = Math.max(MIN_REACTION_MS, (long) (double) reactionTime.getValue());

        List<WorldMeshRenderer.Line> lines = new ArrayList<>();
        List<WorldMeshRenderer.PlaneRect> quads = new ArrayList<>();

        for (Map.Entry<BlockPos, Long> entry : candidates.entrySet()) {
            if (now - entry.getValue() > maxAge) {
                continue;
            }
            addBox(lines, quads, entry.getKey(), cyan, fill);
        }
        for (BlockPos pos : explodeCountdown.keySet()) {
            addBox(lines, quads, pos, cyan, fill);
        }

        WorldMeshRenderer.render(new WorldMeshRenderer.WorldMesh(lines, List.of(), quads));
    }

    private void addBox(List<WorldMeshRenderer.Line> lines, List<WorldMeshRenderer.PlaneRect> quads,
                        BlockPos pos, int color, int fill) {
        Vec3 c1 = new Vec3(pos.getX(), pos.getY() + 1.0D, pos.getZ());
        Vec3 c2 = new Vec3(pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ());
        Vec3 c3 = new Vec3(pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
        Vec3 c4 = new Vec3(pos.getX(), pos.getY() + 1.0D, pos.getZ() + 1.0D);
        Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        quads.add(new WorldMeshRenderer.PlaneRect(
                center, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), 0.5D, 0.5D, fill
        ));
        lines.add(new WorldMeshRenderer.Line(c1, c2, color));
        lines.add(new WorldMeshRenderer.Line(c2, c3, color));
        lines.add(new WorldMeshRenderer.Line(c3, c4, color));
        lines.add(new WorldMeshRenderer.Line(c4, c1, color));
    }

    private int findHotbar(Item item, LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                return i;
            }
        }
        return -1;
    }
}
