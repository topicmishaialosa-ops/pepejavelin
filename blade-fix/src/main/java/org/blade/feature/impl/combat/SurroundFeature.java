package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.blade.context.RotationContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SurroundFeature extends Feature {
    public static final SurroundFeature INSTANCE = new SurroundFeature();

    public final ModeSetting blockMode = register(new ModeSetting(
            "Блок", "Обсидиан", "Обсидиан", "Любой блок"
    ));
    public final BooleanSetting corners = register(new BooleanSetting("Углы", true));
    public final BooleanSetting top = register(new BooleanSetting("Верх", false));
    public final ModeSetting rotate = register(new ModeSetting(
            "Ротация", "OFF", "OFF", "Легит", "Snap"
    ));
    public final ModeSetting swapMode = register(new ModeSetting(
            "Свап", "Silent", "Silent", "Видимый"
    ));
    public final NumberSetting delay = register(new NumberSetting(
            "Задержка (мс)", 100.0D, 50.0D, 500.0D, 10.0D, " мс"
    ));
    public final NumberSetting randomDelay = register(new NumberSetting(
            "Рандом задержка", 50.0D, 0.0D, 200.0D, 5.0D, ""
    ));
    public final BooleanSetting strictRotation = register(new BooleanSetting("Строгая ротация", true));
    public final BooleanSetting groundCheck = register(new BooleanSetting("Проверка земли", true));
    public final BooleanSetting raytraceCheck = register(new BooleanSetting("Raytrace проверка", true));

    private final Random random = new Random();
    private long lastPlace = 0L;

    private BlockPos pendingPlace = null;
    private int pendingSlot = -1;
    private InteractionHand pendingHand = InteractionHand.MAIN_HAND;
    private BlockHitResult pendingHitResult = null;
    private int rotationTicks = 0;
    private BlockPos lastBase = null;

    private float targetYaw;
    private float targetPitch;

    private SurroundFeature() {
        super("Surround", "Ставит блоки вокруг игрока и восстанавливает их при разрушении", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    public void onDisable() {
        clearPending();
        RotationContext.clear();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = event.getClient();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null) {
            return;
        }

        if (groundCheck.getValue() && !player.onGround() && !player.getAbilities().flying) {
            return;
        }

        if (pendingPlace != null) {
            processPending(player);
            return;
        }

        long currentDelay = (long) (double) delay.getValue() + (long) random.nextInt((int) (double) randomDelay.getValue() + 1);
        if (System.currentTimeMillis() - lastPlace < currentDelay) {
            return;
        }

        BlockPos base = player.getBlockPosBelowThatAffectsMyMovement();
        List<BlockPos> positions = getSurroundPositions(base);
        for (BlockPos pos : positions) {
            if (isSolid(player, pos) || isBlocked(player, pos)) {
                continue;
            }

            double distance = player.getEyePosition().distanceTo(Vec3.atCenterOf(pos));
            if (distance > 4.5D) {
                continue;
            }

            BlockPos support = findSupport(player, pos);
            if (support == null) {
                continue;
            }

            InteractionHand hand = getPlaceHand(player);
            int slot;
            if (hand == InteractionHand.MAIN_HAND) {
                slot = findBlockSlot(player);
                if (slot == -1) {
                    return;
                }
            } else {
                slot = player.getInventory().getSelectedSlot();
            }

            Direction side = Direction.getNearest(pos.getX() - support.getX(), pos.getY() - support.getY(), pos.getZ() - support.getZ(), Direction.UP);
            if (side == null) {
                side = Direction.UP;
            }
            Vec3 hitVec = getHitVec(support, side);

            if (raytraceCheck.getValue() && !canSeeBlock(player, support, hitVec)) {
                continue;
            }

            BlockHitResult hitResult = new BlockHitResult(hitVec, side, support, false);

            if (!rotate.is("OFF")) {
                rotateTo(player, hitVec, rotate.is("Легит"));
                pendingPlace = pos;
                pendingSlot = slot;
                pendingHand = hand;
                pendingHitResult = hitResult;
                rotationTicks = rotate.is("Легит") ? 2 : 1;
                lastBase = base;
                return;
            }

            executePlace(player, slot, hand, hitResult);
            return;
        }
    }

    private void processPending(LocalPlayer player) {
        if (pendingPlace == null) {
            return;
        }

        BlockPos base = player.getBlockPosBelowThatAffectsMyMovement();
        if (lastBase != null && !base.equals(lastBase)) {
            clearPending();
            return;
        }

        if (rotationTicks > 0) {
            --rotationTicks;
            return;
        }

        if (strictRotation.getValue() && rotate.is("Легит") && !isRotationDone(player)) {
            return;
        }

        if (isSolid(player, pendingPlace)) {
            clearPending();
            return;
        }

        executePlace(player, pendingSlot, pendingHand, pendingHitResult);
        clearPending();
    }

    private void clearPending() {
        pendingPlace = null;
        pendingSlot = -1;
        pendingHitResult = null;
        rotationTicks = 0;
        lastBase = null;
    }

    private void executePlace(LocalPlayer player, int slot, InteractionHand hand, BlockHitResult hitResult) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) {
            return;
        }

        if (swapMode.is("Silent") && hand == InteractionHand.MAIN_HAND) {
            int current = player.getInventory().getSelectedSlot();
            if (current != slot) {
                player.getInventory().setSelectedSlot(slot);
                player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            }

            player.connection.send(new ServerboundUseItemOnPacket(hand, hitResult, 0));
            player.connection.send(new ServerboundSwingPacket(hand));

            if (current != slot) {
                player.getInventory().setSelectedSlot(current);
                player.connection.send(new ServerboundSetCarriedItemPacket(current));
            }
        } else {
            if (hand == InteractionHand.MAIN_HAND) {
                player.getInventory().setSelectedSlot(slot);
                player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            }
            mc.gameMode.useItemOn(player, hand, hitResult);
            player.swing(hand);
        }
        lastPlace = System.currentTimeMillis();
    }

    private List<BlockPos> getSurroundPositions(BlockPos center) {
        List<BlockPos> list = new ArrayList<>();
        int[][] sides = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] offset : sides) {
            list.add(center.offset(offset[0], offset[1], offset[2]));
        }
        if (corners.getValue()) {
            int[][] cornerOffsets = new int[][]{{1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}};
            for (int[] offset : cornerOffsets) {
                list.add(center.offset(offset[0], offset[1], offset[2]));
            }
        }
        if (top.getValue()) {
            for (int[] offset : sides) {
                list.add(center.offset(offset[0], offset[1] + 1, offset[2]));
            }
        }
        return list;
    }

    private InteractionHand getPlaceHand(LocalPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        boolean obsidianOnly = blockMode.is("Обсидиан");

        if (!offhand.isEmpty() && offhand.getItem() instanceof BlockItem) {
            if (!obsidianOnly || offhand.getItem() == Blocks.OBSIDIAN.asItem()) {
                return InteractionHand.OFF_HAND;
            }
        }
        return InteractionHand.MAIN_HAND;
    }

    private int findBlockSlot(LocalPlayer player) {
        boolean obsidianOnly = blockMode.is("Обсидиан");
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }
            if (obsidianOnly && stack.getItem() != Blocks.OBSIDIAN.asItem()) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private void rotateTo(LocalPlayer player, Vec3 point, boolean smooth) {
        Vec3 eye = player.getEyePosition();
        Vec3 diff = point.subtract(eye);
        double horizontal = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, horizontal));

        float yawRandom = (random.nextFloat() - 0.5F) * 2.0F;
        float pitchRandom = (random.nextFloat() - 0.5F) * 1.0F;
        yaw = yaw + yawRandom;
        pitch = Mth.clamp(pitch + pitchRandom, -90.0F, 90.0F);

        targetYaw = yaw;
        targetPitch = pitch;
        RotationContext.setRotation(yaw, pitch);
    }

    private boolean isRotationDone(LocalPlayer player) {
        float currentYaw = RotationContext.isActive() ? RotationContext.getServerYaw() : player.getYRot();
        float currentPitch = RotationContext.isActive() ? RotationContext.getServerPitch() : player.getXRot();
        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;
        return Math.abs(yawDelta) < 2.0F && Math.abs(pitchDelta) < 2.0F;
    }

    private Vec3 getHitVec(BlockPos support, Direction side) {
        double x = support.getX() + 0.5 + (double) side.getStepX() * 0.5;
        double y = support.getY() + 0.5 + (double) side.getStepY() * 0.5;
        double z = support.getZ() + 0.5 + (double) side.getStepZ() * 0.5;

        double randomOffset = 0.1;
        x += (random.nextDouble() - 0.5) * randomOffset;
        y += (random.nextDouble() - 0.5) * randomOffset;
        z += (random.nextDouble() - 0.5) * randomOffset;

        return new Vec3(x, y, z);
    }

    private boolean canSeeBlock(LocalPlayer player, BlockPos pos, Vec3 hitVec) {
        Vec3 eye = player.getEyePosition();
        return player.level().clip(new ClipContext(
                eye, hitVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player
        )).getType() == HitResult.Type.MISS;
    }

    private boolean isSolid(LocalPlayer player, BlockPos pos) {
        if (player.level() == null) {
            return false;
        }
        var state = player.level().getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    private BlockPos findSupport(LocalPlayer player, BlockPos target) {
        if (isSolid(player, target.below())) {
            return target.below();
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isSolid(player, target.relative(direction))) {
                return target.relative(direction);
            }
        }
        return null;
    }

    private boolean isBlocked(LocalPlayer player, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        AABB box = new AABB(pos);
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != null && entity != player && entity.getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
    }
}