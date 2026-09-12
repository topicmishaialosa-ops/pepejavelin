package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.event.events.render.Render3DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.math.BladeTimer;
import org.blade.utils.render.world.WorldMeshRenderer;
import org.blade.utils.text.ChatUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class KeyFinderFeature extends Feature {
    private static final int SEARCH_RADIUS_XZ = 24;
    private static final int SEARCH_DEPTH_DOWN = 70;
    private static final int SEARCH_HEIGHT_UP = 70;
    private static final int SPAWNER_RADIUS = 4;
    private static final long GLOBAL_SEARCH_RADIUS = 200L;
    private static final long SCAN_DELAY = 1000L;
    private static final long BARITONE_DELAY = 1500L;
    private static final long REGION_COOLDOWN = 60000L;

    public final BooleanSetting walkWithBaritone = register(new BooleanSetting(
            "Идти к цели через Baritone", false
    ));
    public final BooleanSetting activeSearch = register(new BooleanSetting(
            "Активный поиск по миру", false
    ));

    private final Set<BlockPos> visitedContainers = new HashSet<>();
    private final BladeTimer scanTimer = new BladeTimer();
    private final BladeTimer baritoneTimer = new BladeTimer();
    private final BladeTimer regionCooldownTimer = new BladeTimer();
    private final BladeTimer rtpTimer = new BladeTimer();
    private boolean regionCooldown;
    private int rtpAttempts;
    private int totalTargetsFound;
    private int lastScanCount;
    private BlockPos currentBaritoneTarget;
    private boolean baritoneMissing;
    private List<KeyTarget> targets = new ArrayList<>();

    public KeyFinderFeature() {
        super("KeyFinder", "Ищет сундуки и вагонетки с ключами и показывает их в мире", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        this.targets = new ArrayList<>();
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

        if (this.walkWithBaritone.getValue() && !isBaritonePresent()) {
            this.baritoneMissing = true;
            ChatUtil.info("KeyFinder: Baritone не найден, автоматический маршрут недоступен");
        }
    }

    @Override
    protected void onDisable() {
        this.stopBaritone();
        if (!this.targets.isEmpty()) {
            ChatUtil.info("KeyFinder выключен: осталось " + this.targets.size() + " целей");
        }
        if (this.activeSearch.getValue()) {
            ChatUtil.error("Активный поиск отключён");
        }
        this.targets = new ArrayList<>();
        this.currentBaritoneTarget = null;
        super.onDisable();
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (this.activeSearch.getValue()) {
            if (this.scanTimer.finished(SCAN_DELAY)) {
                this.scanWorld();
                this.scanTimer.reset();
            }

            if (this.regionCooldown && this.regionCooldownTimer.finished(REGION_COOLDOWN)) {
                this.regionCooldown = false;
                this.rtpAttempts = 0;
                ChatUtil.info("Кулдаун /rtp прошёл — могу искать дальше");
            }

            if (this.regionCooldown) {
                this.rtpTimer.reset();
                this.activeSearch.setValue(false);
                ChatUtil.info("Обнаружен защищённый регион — отключаю поиск и делаю /rtp");
                mc.player.connection.sendCommand("rtp");
                this.regionCooldown = true;
                return;
            }
        }

        if (this.walkWithBaritone.getValue() && !this.baritoneMissing) {
            this.followNearestTarget();
        }
    }

    @EventTarget
    private void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPhase() != PacketReceiveEvent.Phase.PRE) {
            return;
        }
        String message = incomingText(event.getPacket());
        if (message == null) {
            return;
        }
        String lower = message.toLowerCase(Locale.ROOT);

        if (this.activeSearch.getValue() && this.regionCooldown) {
            if (lower.contains("не можете сломать")
                    || lower.contains("нельзя сломать")
                    || lower.contains("cannot break")
                    || lower.contains("защищен")
                    || lower.contains("защищён")
                    || lower.contains("приват")
                    || lower.contains("обломки не могут быть сломаны")
                    || lower.contains("this structure is protected")) {
                Minecraft mc = Minecraft.getInstance();
                if (this.rtpAttempts < 3) {
                    this.rtpAttempts++;
                    ChatUtil.info("Предупреждение: защищённый регион — делаю /rtp");
                    if (mc.player != null) {
                        mc.player.connection.sendCommand("rtp");
                    }
                    this.regionCooldown = true;
                    this.regionCooldownTimer.reset();
                } else {
                    ChatUtil.error("Регион защищён, отключаю активный поиск");
                    this.activeSearch.setValue(false);
                    this.regionCooldown = false;
                }
            }
        }

        if (this.rtpTimer.finished(10000L)
                && (lower.contains("телепортировался") || lower.contains("you have been teleported"))) {
            this.rtpAttempts = 0;
            this.regionCooldown = false;
            this.rtpTimer.reset();
            ChatUtil.info("Телепорт выполнен, возобновляю поиск");
        }
    }

    @EventTarget
    private void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || this.targets.isEmpty()) {
            return;
        }

        List<WorldMeshRenderer.Line> lines = new ArrayList<>();
        List<WorldMeshRenderer.PlaneRect> quads = new ArrayList<>();
        for (KeyTarget target : this.targets) {
            int color = switch (target.status) {
                case HAS_KEY -> 0xFF35D07F;
                case UNLOOTED -> 0xFFFFC857;
                case LOOTED -> 0xFF888888;
            };
            drawBox(lines, quads, target.position, color);
        }
        WorldMeshRenderer.render(new WorldMeshRenderer.WorldMesh(lines, List.of(), quads));
    }

    private void drawBox(List<WorldMeshRenderer.Line> lines, List<WorldMeshRenderer.PlaneRect> quads,
                         BlockPos pos, int color) {
        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = pos.getX() + 1.0D;
        double y2 = pos.getY() + 1.0D;
        double z2 = pos.getZ() + 1.0D;
        int lineColor = ColorUtil.withAlpha(color, 255);
        int fillColor = ColorUtil.withAlpha(color, 40);
        Vec3 c1 = new Vec3(x1, y1, z1);
        Vec3 c2 = new Vec3(x2, y1, z1);
        Vec3 c3 = new Vec3(x2, y1, z2);
        Vec3 c4 = new Vec3(x1, y1, z2);
        Vec3 t1 = new Vec3(x1, y2, z1);
        Vec3 t2 = new Vec3(x2, y2, z1);
        Vec3 t3 = new Vec3(x2, y2, z2);
        Vec3 t4 = new Vec3(x1, y2, z2);
        lines.add(new WorldMeshRenderer.Line(c1, c2, lineColor));
        lines.add(new WorldMeshRenderer.Line(c2, c3, lineColor));
        lines.add(new WorldMeshRenderer.Line(c3, c4, lineColor));
        lines.add(new WorldMeshRenderer.Line(c4, c1, lineColor));
        lines.add(new WorldMeshRenderer.Line(t1, t2, lineColor));
        lines.add(new WorldMeshRenderer.Line(t2, t3, lineColor));
        lines.add(new WorldMeshRenderer.Line(t3, t4, lineColor));
        lines.add(new WorldMeshRenderer.Line(t4, t1, lineColor));
        lines.add(new WorldMeshRenderer.Line(c1, t1, lineColor));
        lines.add(new WorldMeshRenderer.Line(c2, t2, lineColor));
        lines.add(new WorldMeshRenderer.Line(c3, t3, lineColor));
        lines.add(new WorldMeshRenderer.Line(c4, t4, lineColor));
        quads.add(new WorldMeshRenderer.PlaneRect(
                new Vec3((x1 + x2) / 2.0D, y2, (z1 + z2) / 2.0D),
                new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D),
                (x2 - x1) / 2.0D, (z2 - z1) / 2.0D, fillColor
        ));
        quads.add(new WorldMeshRenderer.PlaneRect(
                new Vec3((x1 + x2) / 2.0D, y1, (z1 + z2) / 2.0D),
                new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D),
                (x2 - x1) / 2.0D, (z2 - z1) / 2.0D, fillColor
        ));
    }

    private void scanWorld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        BlockPos playerPos = mc.player.blockPosition();

        List<KeyTarget> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();

        AABB searchBox = new AABB(
                playerPos.getX() - SEARCH_RADIUS_XZ, playerPos.getY() - SEARCH_DEPTH_DOWN, playerPos.getZ() - SEARCH_RADIUS_XZ,
                playerPos.getX() + SEARCH_RADIUS_XZ + 1.0D, playerPos.getY() + SEARCH_HEIGHT_UP + 1.0D, playerPos.getZ() + SEARCH_RADIUS_XZ + 1.0D
        );
        for (Entity entity : mc.level.getEntities((Entity) null, searchBox, e -> e instanceof MinecartChest)) {
            if (!(entity instanceof MinecartChest minecart)) {
                continue;
            }
            BlockPos position = minecart.blockPosition().immutable();
            if (seen.add(position)) {
                LootState state = this.hasKey(minecart) ? LootState.HAS_KEY : LootState.UNLOOTED;
                found.add(new KeyTarget(position, state));
                if (state == LootState.HAS_KEY) {
                    ChatUtil.info("Найден ключ в вагонетке в ("
                            + position.getX() + ", " + position.getY() + ", " + position.getZ() + ")!");
                }
            }
        }

        for (BlockPos spawner : this.findSpawners(playerPos)) {
            for (BlockPos chest : this.findNearbyContainers(spawner)) {
                if (seen.add(chest)) {
                    BlockEntity blockEntity = mc.level.getBlockEntity(chest);
                    LootState state = this.hasKey(blockEntity) ? LootState.HAS_KEY : LootState.UNLOOTED;
                    found.add(new KeyTarget(chest, state));
                    if (state == LootState.HAS_KEY) {
                        ChatUtil.info("Найден ключ в сундуке рядом со спавнером в ("
                                + chest.getX() + ", " + chest.getY() + ", " + chest.getZ() + ")!");
                    }
                }
            }
        }

        this.targets = found;

        if (!found.isEmpty()) {
            ChatUtil.info("KeyFinder: Найдено " + found.size() + " цели(й) в радиусе "
                    + SEARCH_RADIUS_XZ + " по сторонам и " + SEARCH_DEPTH_DOWN + " вверх/вниз");
        }
    }

    private List<BlockPos> findSpawners(BlockPos center) {
        Minecraft mc = Minecraft.getInstance();
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos position : BlockPos.betweenClosed(
                center.getX() - SEARCH_RADIUS_XZ, center.getY() - SEARCH_DEPTH_DOWN, center.getZ() - SEARCH_RADIUS_XZ,
                center.getX() + SEARCH_RADIUS_XZ, center.getY() + SEARCH_HEIGHT_UP, center.getZ() + SEARCH_RADIUS_XZ)) {
            if (mc.level.getBlockState(position).is(Blocks.SPAWNER)) {
                result.add(position.immutable());
            }
        }
        return result;
    }

    private List<BlockPos> findNearbyContainers(BlockPos spawner) {
        Minecraft mc = Minecraft.getInstance();
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos position : BlockPos.withinManhattan(spawner, SPAWNER_RADIUS, SPAWNER_RADIUS, SPAWNER_RADIUS)) {
            if (mc.level.getBlockEntity(position) instanceof BaseContainerBlockEntity) {
                result.add(position.immutable());
            }
        }
        return result;
    }

    private boolean hasKey(BlockEntity blockEntity) {
        if (!(blockEntity instanceof BaseContainerBlockEntity inventory)) {
            return false;
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (this.isKey(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasKey(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (this.isKey(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private boolean isKey(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TRIPWIRE_HOOK);
    }

    private void followNearestTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || this.targets.isEmpty()) {
            return;
        }

        if (!isBaritonePresent()) {
            this.baritoneMissing = true;
            return;
        }

        KeyTarget nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Vec3 playerPos = mc.player.position();
        for (KeyTarget target : this.targets) {
            if (target.status == LootState.LOOTED) {
                continue;
            }
            Vec3 center = new Vec3(target.position.getX() + 0.5D, target.position.getY() + 0.5D, target.position.getZ() + 0.5D);
            double distance = playerPos.distanceToSqr(center);
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
        ChatUtil.info("KeyFinder: иду к цели через Baritone: " + targetType + " в ("
                + nearest.position.getX() + ", " + nearest.position.getY() + ", " + nearest.position.getZ() + ")");
        mc.player.connection.sendChat("#goto "
                + nearest.position.getX() + " "
                + nearest.position.getY() + " "
                + nearest.position.getZ());
    }

    private void stopBaritone() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null && this.currentBaritoneTarget != null) {
            mc.player.connection.sendChat("#stop");
        }
    }

    private boolean isBaritonePresent() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String incomingText(Packet<?> packet) {
        if (packet instanceof ClientboundSystemChatPacket systemChat) {
            return systemChat.content().getString();
        }
        if (packet instanceof ClientboundDisguisedChatPacket disguisedChat) {
            return disguisedChat.message().getString();
        }
        if (packet instanceof ClientboundPlayerChatPacket playerChat) {
            Component unsigned = playerChat.unsignedContent();
            return unsigned != null ? unsigned.getString() : playerChat.body().content();
        }
        return null;
    }

    private enum LootState {
        UNLOOTED,
        HAS_KEY,
        LOOTED
    }

    private record KeyTarget(BlockPos position, LootState status) {
    }
}