package org.blade.feature.impl.visual;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.blade.context.RenderContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.render.Render3DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BlockMapSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.screens.block.BlockPickerScreen;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Render3DUtil;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;
import org.blade.utils.render.world.WorldMeshRenderer;
import org.blade.utils.render.world.WorldMeshRenderer.WorldMesh;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BlockESPFeature extends Feature {
    public static final BlockESPFeature INSTANCE = new BlockESPFeature();

    private static final int MAX_SECTIONS_PER_TICK = 32;
    private static final long REBUILD_INTERVAL = 1000L;
    private static final int SPAWNER_COLOR = 0xFFFF8C00;
    private static final double FILL_INSET = 0.002D;
    private static final String FALLBACK_SPAWNER_NAME = "Спавнер";

    private final NumberSetting radius = new NumberSetting("Радиус", 32.0D, 4.0D, 200.0D, 1.0D, " блоков");
    private final BlockMapSetting blocks = new BlockMapSetting("Выбранные блоки");
    private final ButtonSetting openPicker = new ButtonSetting("Выбрать блоки", "Открыть выбор", BlockPickerScreen::open);
    private final BooleanSetting spawners = new BooleanSetting("Спавнеры", true);
    private final NumberSetting labelRadius = new NumberSetting("Скругление", 5.0D, 0.0D, 14.0D, 0.5D, "")
            .visibleWhen(() -> this.spawners.getValue());
    private final ColorSetting labelAccent = new ColorSetting("Акцент", SPAWNER_COLOR)
            .visibleWhen(() -> this.spawners.getValue());
    private final ColorSetting labelBg = new ColorSetting("Цвет фона", 0x000000)
            .visibleWhen(() -> this.spawners.getValue());
    private final ColorSetting labelTextColor = new ColorSetting("Цвет текста", 0xFFFFFFFF)
            .visibleWhen(() -> this.spawners.getValue());
    private final ModeSetting labelFont = new ModeSetting("Шрифт", "Средний", "Обычный", "Средний", "Полужирный")
            .visibleWhen(() -> this.spawners.getValue());
    private final NumberSetting labelSize = new NumberSetting("Размер текста", 10.0D, 6.0D, 20.0D, 0.5D, "")
            .visibleWhen(() -> this.spawners.getValue());
    private final NumberSetting lineWidth = new NumberSetting("Толщина линии", 1.5D, 0.5D, 5.0D, 0.5D, "px");
    private final BooleanSetting fill = new BooleanSetting("Заливка", true);

    private final Long2ObjectOpenHashMap<FoundBlock> found = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<FoundBlock> pending = new Long2ObjectOpenHashMap<>();
    private final ArrayDeque<SectionPos> scanQueue = new ArrayDeque<>();
    private final Map<net.minecraft.world.level.block.Block, Integer> colorCache = new HashMap<>();
    private final BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
    private boolean scanDone = true;
    private boolean dirty = true;
    private long nextScanAt;

    public BlockESPFeature() {
        super("BlockESP", "Подсвечивает выбранные блоки в мире", FeatureCategory.VISUAL, BindSetting.UNBOUND);
        register(radius);
        register(blocks);
        register(openPicker);
        register(spawners);
        register(labelRadius);
        register(labelAccent);
        register(labelBg);
        register(labelTextColor);
        register(labelFont);
        register(labelSize);
        register(lineWidth);
        register(fill);
    }

    @Override
    protected void onEnable() {
        this.dirty = true;
    }

    @EventTarget
    private void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (this.blocks.isEmpty() && !this.spawners.getValue()) {
            this.found.clear();
            this.pending.clear();
            this.scanQueue.clear();
            this.colorCache.clear();
            this.scanDone = true;
            return;
        }
        this.tickScan(mc);
    }

    @EventTarget
    private void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || this.found.isEmpty()) {
            return;
        }
        float widthScale = (float) (this.lineWidth.getValue() / 1.5D);
        List<WorldMeshRenderer.Line> lines = new ArrayList<>();
        List<WorldMeshRenderer.PlaneRect> quads = new ArrayList<>();
        for (Long2ObjectMap.Entry<FoundBlock> entry : this.found.long2ObjectEntrySet()) {
            this.addBox(lines, quads, entry.getValue().box(), entry.getValue().color(), widthScale);
        }
        WorldMeshRenderer.render(new WorldMesh(lines, List.of(), quads));
        this.renderSpawnerLabels(mc, event.getDeltaTracker());
    }

    private void renderSpawnerLabels(Minecraft mc, DeltaTracker deltaTracker) {
        if (!this.spawners.getValue()) {
            return;
        }
        boolean hasLabel = false;
        for (FoundBlock block : this.found.values()) {
            String mob = block.spawnerMob();
            if (mob != null && !mob.isEmpty()) {
                hasLabel = true;
                break;
            }
        }
        if (!hasLabel) {
            return;
        }
        GuiRenderState guiRenderState = mc.gameRenderer.gameRenderState().guiRenderState;
        GuiGraphicsExtractor extractor = new GuiGraphicsExtractor(
                mc,
                guiRenderState,
                (int) mc.mouseHandler.getScaledXPos(mc.getWindow()),
                (int) mc.mouseHandler.getScaledYPos(mc.getWindow()));
        RenderContext.enter2D(null, extractor, deltaTracker);
        try {
            Render2DUtil.beginFrame();
            this.drawSpawnerLabels(mc);
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }

    private UiFontStyle labelFontStyle() {
        if (this.labelFont.is("Обычный")) {
            return UiFontStyle.REGULAR;
        }
        if (this.labelFont.is("Полужирный")) {
            return UiFontStyle.SEMIBOLD;
        }
        return UiFontStyle.MEDIUM;
    }

    private void drawSpawnerLabels(Minecraft mc) {
        float unit = 1.1F / mc.getWindow().getGuiScale();
        float textSize = this.labelSize.getValue().floatValue() * unit;
        float padX = 6.0F * unit;
        float pillHeight = Math.max(14.0F * unit, textSize * 1.4F);
        float pillRadius = this.labelRadius.getValue().floatValue() * unit;
        UiFontStyle style = this.labelFontStyle();
        float letterSpacing = textSize * style.letterSpacingEm();
        int accentColor = ColorUtil.withAlpha(this.labelAccent.getValue(), 170);
        int bgColor = ColorUtil.withAlpha(this.labelBg.getValue(), 145);
        int textColor = this.labelTextColor.getValue();
        MsdfFont font = UiFonts.sfProDisplay();
        for (FoundBlock block : this.found.values()) {
            String mob = block.spawnerMob();
            if (mob == null || mob.isEmpty()) {
                continue;
            }
            AABB box = block.box();
            Vec3 anchorPosition = new Vec3(
                    (box.minX + box.maxX) / 2.0D,
                    box.maxY + 0.35D,
                    (box.minZ + box.maxZ) / 2.0D);
            Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, anchorPosition);
            if (anchor == null) {
                continue;
            }
            float textWidth = font.measureWidth(mob, textSize, letterSpacing);
            float width = textWidth + padX * 2.0F;
            float x = anchor.x() - width / 2.0F;
            float y = anchor.y() - pillHeight / 2.0F;
            Render2DUtil.rect(x, y, width, pillHeight)
                    .color(bgColor)
                    .radius(pillRadius)
                    .draw();
            Render2DUtil.rect(x, y, width, pillHeight)
                    .color(0x00000000)
                    .radius(pillRadius)
                    .border(Math.max(1.0F, unit), accentColor)
                    .draw();
            Render2DUtil.text(x + padX, font.centeredTextY(anchor.y(), textSize), textSize, mob)
                    .style(style)
                    .color(textColor)
                    .draw();
        }
    }

    private void addBox(List<WorldMeshRenderer.Line> lines, List<WorldMeshRenderer.PlaneRect> quads, AABB box, int color, float widthScale) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;
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
        lines.add(new WorldMeshRenderer.Line(c1, c2, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(c2, c3, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(c3, c4, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(c4, c1, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(t1, t2, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(t2, t3, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(t3, t4, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(t4, t1, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(c1, t1, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(c2, t2, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(c3, t3, lineColor, lineColor, widthScale));
        lines.add(new WorldMeshRenderer.Line(c4, t4, lineColor, lineColor, widthScale));
        if (this.fill.getValue()) {
            double hx = (x2 - x1) / 2.0D;
            double hz = (z2 - z1) / 2.0D;
            quads.add(new WorldMeshRenderer.PlaneRect(
                    new Vec3((x1 + x2) / 2.0D, y2 - FILL_INSET, (z1 + z2) / 2.0D),
                    new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D),
                    hx, hz, fillColor
            ));
            quads.add(new WorldMeshRenderer.PlaneRect(
                    new Vec3((x1 + x2) / 2.0D, y1 + FILL_INSET, (z1 + z2) / 2.0D),
                    new Vec3(1.0D, 0.0D, 0.0D), new Vec3(0.0D, 0.0D, 1.0D),
                    hx, hz, fillColor
            ));
        }
    }

    private void tickScan(Minecraft mc) {
        if (this.dirty) {
            this.rebuildScan(mc);
            return;
        }
        long now = System.currentTimeMillis();
        if (this.scanDone) {
            if (now >= this.nextScanAt) {
                this.rebuildScan(mc);
            }
            return;
        }
        int processed = 0;
        while (!this.scanQueue.isEmpty() && processed < MAX_SECTIONS_PER_TICK) {
            this.scanSection(mc, this.scanQueue.poll());
            processed++;
        }
        if (this.scanQueue.isEmpty()) {
            this.finishScan();
        }
    }

    private void finishScan() {
        this.found.clear();
        this.found.putAll(this.pending);
        this.pending.clear();
        this.scanDone = true;
        this.nextScanAt = System.currentTimeMillis() + REBUILD_INTERVAL;
    }

    private void rebuildScan(Minecraft mc) {
        this.pending.clear();
        this.scanQueue.clear();
        this.colorCache.clear();
        this.dirty = false;
        if (mc.level == null || mc.player == null) {
            this.scanDone = true;
            return;
        }
        for (Map.Entry<String, Integer> entry : this.blocks.getBlocks().entrySet()) {
            net.minecraft.world.level.block.Block block = BlockMapSetting.blockFromId(entry.getKey());
            if (block != null && block != Blocks.AIR) {
                this.colorCache.put(block, entry.getValue());
            }
        }
        if (this.colorCache.isEmpty() && !this.spawners.getValue()) {
            this.found.clear();
            this.scanDone = true;
            return;
        }
        int half = (int) Math.ceil(this.radius.getValue() / 16.0D);
        int playerChunkX = mc.player.chunkPosition().x();
        int playerChunkZ = mc.player.chunkPosition().z();
        int minSectionY = mc.level.getMinSectionY();
        int maxSectionY = mc.level.getMaxSectionY();

        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                if (!mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    this.scanQueue.add(SectionPos.of(chunkX, sectionY, chunkZ));
                }
            }
        }
        this.scanDone = this.scanQueue.isEmpty();
        if (this.scanDone) {
            this.nextScanAt = System.currentTimeMillis() + REBUILD_INTERVAL;
        }
    }

    private void scanSection(Minecraft mc, SectionPos sectionPos) {
        LevelChunk chunk = mc.level.getChunk(sectionPos.x(), sectionPos.z());
        if (chunk == null) {
            return;
        }
        int index = -1;
        if (sectionPos.y() >= mc.level.getMinSectionY() && sectionPos.y() <= mc.level.getMaxSectionY()) {
            index = sectionPos.y() - mc.level.getMinSectionY();
        }
        if (index < 0 || index >= chunk.getSections().length) {
            return;
        }
        LevelChunkSection section = chunk.getSection(index);
        if (section == null || section.hasOnlyAir()) {
            return;
        }
        boolean scanSpawners = this.spawners.getValue();
        int baseX = sectionPos.minBlockX();
        int baseY = sectionPos.minBlockY();
        int baseZ = sectionPos.minBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockState state = section.getBlockState(x, y, z);
                    if (state.isAir()) {
                        continue;
                    }
                    boolean isSpawner = scanSpawners && state.is(Blocks.SPAWNER);
                    Integer color = this.colorCache.get(state.getBlock());
                    if (color == null && !isSpawner) {
                        continue;
                    }
                    int blockX = baseX + x;
                    int blockY = baseY + y;
                    int blockZ = baseZ + z;
                    this.scanPos.set(blockX, blockY, blockZ);
                    VoxelShape shape = state.getCollisionShape(mc.level, this.scanPos);
                    if (shape.isEmpty()) {
                        shape = state.getShape(mc.level, this.scanPos);
                    }
                    AABB box = shape.isEmpty() ? new AABB(this.scanPos) : shape.bounds().move(this.scanPos);
                    String spawnerMob = isSpawner
                            ? spawnerMobName(mc, new BlockPos(blockX, blockY, blockZ))
                            : null;
                    this.pending.put(this.scanPos.asLong(),
                            new FoundBlock(box, isSpawner ? SPAWNER_COLOR : color, spawnerMob));
                }
            }
        }
    }

    private static String spawnerMobName(Minecraft mc, BlockPos pos) {
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (blockEntity instanceof SpawnerBlockEntity spawner) {
            Entity display = spawner.getSpawner().getOrCreateDisplayEntity(mc.level, pos);
            if (display != null) {
                return display.getType().getDescription().getString();
            }
        }
        return FALLBACK_SPAWNER_NAME;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public BlockMapSetting getBlocks() {
        return this.blocks;
    }

    private record FoundBlock(AABB box, int color, String spawnerMob) {
    }
}