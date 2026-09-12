package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class AutoExplosionFeature extends Feature {
    public static final AutoExplosionFeature INSTANCE = new AutoExplosionFeature();

    private static final float PLACE_REACH = 4.5F;

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
    private BlockPos lastScanPos;
    private int scanCooldown;

    public AutoExplosionFeature() {
        super("AutoExplosion", "Если игрок поставил обсидиан — ставит кристалл из хотбара и взрывает его", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    public void onEnable() {
        snapshot.clear();
        candidates.clear();
        explodeCountdown.clear();
        retries.clear();
        lastScanPos = null;
        scanCooldown = 0;
        scan();
    }

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

        if (shouldScan(player)) {
            scan();
        }

        processCandidates(player);
        processExplosions(player);
    }

    private boolean shouldScan(LocalPlayer player) {
        BlockPos current = player.getBlockPosBelowThatAffectsMyMovement();
        if (lastScanPos == null || !lastScanPos.equals(current)) {
            lastScanPos = current;
            scanCooldown = 0;
            return true;
        }

        if (scanCooldown-- <= 0) {
            scanCooldown = 10;
            return true;
        }

        return false;
    }

    private void scan() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        int radius = Math.max(1, (int) Math.ceil(range.getValue()));
        BlockPos playerPos = player.getBlockPosBelowThatAffectsMyMovement();
        Map<BlockPos, Block> current = new HashMap<>();
        for (int x = playerPos.getX() - radius; x <= playerPos.getX() + radius; ++x) {
            for (int y = playerPos.getY() - 2; y <= playerPos.getY() + 2; ++y) {
                for (int z = playerPos.getZ() - radius; z <= playerPos.getZ() + radius; ++z) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.level.getBlockState(pos).getBlock();
                    current.put(pos, block);
                    if (block == Blocks.OBSIDIAN && snapshot.get(pos) != Blocks.OBSIDIAN) {
                        candidates.put(pos.immutable(), System.currentTimeMillis());
                    }
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
        Vec3 eye = player.getEyePosition();
        double rangeDbl = range.getValue();
        float maxDist = (float) rangeDbl;
        long maxAge = (long) (double) reactionTime.getValue();

        for (BlockPos pos : new HashSet<>(candidates.keySet())) {
            if (explodeCountdown.containsKey(pos) || retries.containsKey(pos)) {
                candidates.remove(pos);
                continue;
            }

            if (now - candidates.get(pos) > maxAge) {
                candidates.remove(pos);
                continue;
            }

            double distance = eye.distanceTo(Vec3.atCenterOf(pos));
            if (distance > Math.max(maxDist, PLACE_REACH)) {
                candidates.remove(pos);
                continue;
            }

            BlockPos above = pos.above();
            if (!mc.level.getBlockState(above).isAir()) {
                candidates.remove(pos);
                continue;
            }

            int crystalSlot = findHotbar(Items.END_CRYSTAL, player);
            if (crystalSlot == -1) {
                continue;
            }

            Vec3 placePos = new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
            if (eye.distanceTo(placePos) > PLACE_REACH) {
                continue;
            }

            placeCrystal(player, pos, above, placePos, crystalSlot);
        }
    }

    private void placeCrystal(LocalPlayer player, BlockPos pos, BlockPos above, Vec3 placePos, int crystalSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 diff = placePos.subtract(eye);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));
        RotationContext.setRotation(yaw, pitch);

        int prevSlot = player.getInventory().getSelectedSlot();
        if (crystalSlot != prevSlot) {
            player.getInventory().setSelectedSlot(crystalSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(crystalSlot));
        }

        BlockHitResult hit = new BlockHitResult(placePos, Direction.UP, pos, false);
        boolean accepted = mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit).consumesAction();
        if (prevSlot != crystalSlot && player.getInventory().getSelectedSlot() == crystalSlot) {
            player.getInventory().setSelectedSlot(prevSlot);
            player.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
        }

            if (accepted) {
                explodeCountdown.put(pos.immutable(), (int) (double) explodeDelay.getValue());
                candidates.remove(pos);
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

            explodeCountdown.remove(pos);
            EndCrystal crystal = findCrystal(pos);
            if (crystal == null) {
                int attempts = retries.getOrDefault(pos, 0) + 1;
                if (attempts < (int) (double) breakRetries.getValue()) {
                    retries.put(pos, attempts);
                    explodeCountdown.put(pos, 1);
                } else {
                    retries.remove(pos);
                }
                continue;
            }

            mc.gameMode.attack(player, crystal);
            player.swing(InteractionHand.MAIN_HAND);
            retries.remove(pos);
        }
    }

    private EndCrystal findCrystal(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        AABB box = new AABB(pos.above()).inflate(0.5D);
        for (EndCrystal crystal : mc.level.getEntities(
                EntityTypeTest.forClass(EndCrystal.class),
                box,
                e -> e.isAlive() && !e.isRemoved())) {
            return crystal;
        }
        return null;
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
        long maxAge = (long) (double) reactionTime.getValue();

        java.util.List<WorldMeshRenderer.Line> lines = new java.util.ArrayList<>();
        java.util.List<WorldMeshRenderer.PlaneRect> quads = new java.util.ArrayList<>();

        for (Map.Entry<BlockPos, Long> entry : candidates.entrySet()) {
            if (now - entry.getValue() > maxAge) {
                continue;
            }

            BlockPos pos = entry.getKey();
            Vec3 c1 = new Vec3(pos.getX() + 0.0D, pos.getY() + 1.0D, pos.getZ() + 0.0D);
            Vec3 c2 = new Vec3(pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 0.0D);
            Vec3 c3 = new Vec3(pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
            Vec3 c4 = new Vec3(pos.getX() + 0.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
            Vec3 center = new Vec3(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
            quads.add(new WorldMeshRenderer.PlaneRect(
                    center, new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D), 0.5D, 0.5D, fill
            ));
            lines.add(new WorldMeshRenderer.Line(c1, c2, cyan));
            lines.add(new WorldMeshRenderer.Line(c2, c3, cyan));
            lines.add(new WorldMeshRenderer.Line(c3, c4, cyan));
            lines.add(new WorldMeshRenderer.Line(c4, c1, cyan));
        }

        WorldMeshRenderer.render(new WorldMeshRenderer.WorldMesh(lines, java.util.List.of(), quads));
    }

    private int findHotbar(net.minecraft.world.item.Item item, LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}