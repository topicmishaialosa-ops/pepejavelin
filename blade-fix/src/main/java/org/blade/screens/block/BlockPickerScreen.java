package org.blade.screens.block;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.joml.Matrix3x2fStack;
import org.blade.feature.impl.visual.BlockESPFeature;
import org.blade.feature.setting.BlockMapSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BlockPickerScreen extends Screen {
    private static final float MARGIN = 12.0F;
    private static final float SEARCH_H = 22.0F;
    private static final float PANEL_W = 250.0F;
    private static final float CELL_MAX = 62.0F;
    private static final float SWATCH = 24.0F;
    private static final float SWATCH_GAP = 5.0F;
    private static final int PALETTE_COLS = 6;
    private static final float BTN_H = 18.0F;

    private static final int[] PALETTE = new int[]{
            0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00, 0xFFA8E10C, 0xFF00E5A0, 0xFF00B8D9,
            0xFF4DA6FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF2D92, 0xFFFF9FC7, 0xFF00FF7F,
            0xFFFFD700, 0xFF964B00, 0xFF8E8E93, 0xFFFFFFFF, 0xFF111111, 0xFF00FFFF
    };

    private final List<Block> blocks = new ArrayList<>();
    private final BlockESPFeature feature;
    private final BlockMapSetting setting;

    private Block editing;
    private String searchBuffer = "";
    private boolean searchFocused;
    private float listScroll;
    private int draggingChannel = -1;

    public BlockPickerScreen() {
        super(Component.literal("Выбор блоков"));
        this.feature = BlockESPFeature.INSTANCE;
        this.setting = this.feature.getBlocks();
        for (Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK.stream().toList()) {
            if (block != null) {
                this.blocks.add(block);
            }
        }
        this.blocks.sort(Comparator.comparing(block -> block.getName().getString()));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof BlockPickerScreen) {
            return;
        }
        mc.gui.setScreen(new BlockPickerScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float textWidth(String text) {
        return this.font.width(text);
    }

    private int accent(int alpha) {
        return ColorUtil.withAlpha(Theme.getAccent(), alpha);
    }

    private float gridX() {
        return MARGIN;
    }

    private float gridY() {
        return MARGIN + 24.0F + SEARCH_H + 8.0F;
    }

    private float gridWidth() {
        return this.width - MARGIN * 2.0F - PANEL_W - 10.0F;
    }

    private float gridHeight() {
        return this.height - this.gridY() - MARGIN;
    }

    private float panelX() {
        return this.width - MARGIN - PANEL_W;
    }

    private int columns() {
        return Math.max(1, (int) (this.gridWidth() / CELL_MAX));
    }

    private float cellSize() {
        return this.gridWidth() / (float) this.columns();
    }

    private int paletteRows() {
        return (int) Math.ceil((double) PALETTE.length / (double) PALETTE_COLS);
    }

    private float paletteTop() {
        return this.gridY() + 78.0F;
    }

    private float sliderBase() {
        return this.paletteTop() + (float) this.paletteRows() * (SWATCH + SWATCH_GAP) + 10.0F;
    }

    private float previewTop() {
        return this.sliderBase() + 3.0F * 26.0F + 8.0F;
    }

    private float buttonsTop() {
        return this.previewTop() + 24.0F;
    }

    private List<Block> filtered() {
        String query = this.searchBuffer.trim().toLowerCase();
        if (query.isEmpty()) {
            return this.blocks;
        }
        List<Block> result = new ArrayList<>();
        for (Block block : this.blocks) {
            String name = block.getName().getString().toLowerCase();
            String id = BlockMapSetting.getId(block).toLowerCase();
            if (name.contains(query) || id.contains(query)) {
                result.add(block);
            }
        }
        return result;
    }

    private void clampScroll(int count) {
        int rows = (int) Math.ceil((double) count / (double) this.columns());
        float max = Math.max(0.0F, (float) rows * this.cellSize() - this.gridHeight());
        this.listScroll = Math.max(0.0F, Math.min(this.listScroll, max));
    }

    private void selectBlock(Block block) {
        String id = BlockMapSetting.getId(block);
        if (!this.setting.contains(id)) {
            this.setting.set(id, ColorUtil.withAlpha(Theme.getAccent(), 255));
            this.feature.markDirty();
        }
        this.editing = block;
    }

    private void updateSlider(int mouseX, int channel) {
        if (this.editing == null) {
            return;
        }
        float x = this.panelX() + 12.0F;
        float w = PANEL_W - 24.0F;
        float percent = Math.max(0.0F, Math.min(1.0F, (float) (mouseX - x) / w));
        int value = Math.round(percent * 255.0F);
        int color = this.setting.getColor(BlockMapSetting.getId(this.editing));
        int red = ColorUtil.red(color);
        int green = ColorUtil.green(color);
        int blue = ColorUtil.blue(color);
        if (channel == 0) {
            red = value;
        } else if (channel == 1) {
            green = value;
        } else {
            blue = value;
        }
        this.setting.set(BlockMapSetting.getId(this.editing), ColorUtil.withAlpha(ColorUtil.rgb(red, green, blue), 255));
        this.feature.markDirty();
    }

    private boolean isInside(float x, float y, float px, float py, float pw, float ph) {
        return x >= px && x <= px + pw && y >= py && y <= py + ph;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0x82000000);
        this.renderTitle(gfx);
        this.renderExit(gfx, mouseX, mouseY);
        this.renderSearch(gfx, mouseX, mouseY);
        this.renderGrid(gfx, mouseX, mouseY);
        this.renderPanel(gfx, mouseX, mouseY);
    }

    private void renderTitle(GuiGraphicsExtractor gfx) {
        gfx.text(this.font, Component.literal("Выбор блоков"), (int) MARGIN, (int) MARGIN, 0xFFE0E2E8);
        gfx.text(this.font, Component.literal("ЛКМ — добавить/выбрать, ПКМ — убрать"), (int) MARGIN, (int) MARGIN + 12, 0xFF969AA4);
    }

    private void renderExit(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        float x = this.width - MARGIN - 46.0F;
        boolean hovered = isInside(mouseX, mouseY, x, MARGIN, 46.0F, 18.0F);
        gfx.fill((int) x, (int) MARGIN, (int) (x + 46.0F), (int) (MARGIN + 18.0F), hovered ? accent(120) : 0xC814161C);
        gfx.centeredText(this.font, Component.literal("Выход"), (int) (x + 23.0F), (int) MARGIN + 5, 0xFFE6E8EE);
    }

    private void renderSearch(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        float x = this.gridX();
        float y = this.gridY() - SEARCH_H - 8.0F;
        float w = this.gridWidth();
        boolean hovered = isInside(mouseX, mouseY, x, y, w, SEARCH_H);
        gfx.fill((int) x, (int) y, (int) (x + w), (int) (y + SEARCH_H), 0xEB12141A);
        int border = this.searchFocused ? accent(180) : hovered ? accent(90) : 0xFF323640;
        gfx.fill((int) x, (int) y, (int) (x + w), (int) (y + 1), border);
        gfx.fill((int) x, (int) (y + SEARCH_H - 1), (int) (x + w), (int) (y + SEARCH_H), border);
        gfx.fill((int) x, (int) y, (int) (x + 1), (int) (y + SEARCH_H), border);
        gfx.fill((int) (x + w - 1), (int) y, (int) (x + w), (int) (y + SEARCH_H), border);
        if (this.searchBuffer.isEmpty() && !this.searchFocused) {
            gfx.text(this.font, Component.literal("Поиск блока..."), (int) x + 8, (int) y + 7, 0xFF787C86);
        } else {
            String text = this.searchBuffer + (this.searchFocused && System.currentTimeMillis() % 900L < 450L ? "_" : "");
            gfx.text(this.font, Component.literal(text), (int) x + 8, (int) y + 7, 0xFFE6E8EE);
        }
    }

    private void renderGrid(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        float x = this.gridX();
        float y = this.gridY();
        float w = this.gridWidth();
        float h = this.gridHeight();
        gfx.fill((int) x, (int) y, (int) (x + w), (int) (y + h), 0xDC0F1116);
        gfx.fill((int) x, (int) y, (int) (x + w), (int) (y + 1), accent(50));
        gfx.fill((int) x, (int) (y + h - 1), (int) (x + w), (int) (y + h), accent(50));
        gfx.fill((int) x, (int) y, (int) (x + 1), (int) (y + h), accent(50));
        gfx.fill((int) (x + w - 1), (int) y, (int) (x + w), (int) (y + h), accent(50));

        List<Block> filtered = this.filtered();
        this.clampScroll(filtered.size());
        int columns = this.columns();
        float cell = this.cellSize();

        gfx.enableScissor((int) (x + 2), (int) (y + 2), (int) (x + w - 2), (int) (y + h - 2));
        for (int i = 0; i < filtered.size(); i++) {
            Block block = filtered.get(i);
            float cellX = x + (float) (i % columns) * cell;
            float cellY = y - this.listScroll + (float) (i / columns) * cell;
            if (cellY + cell < y || cellY > y + h) {
                continue;
            }
            this.renderCell(gfx, block, cellX, cellY, cell, mouseX, mouseY, x, w);
        }
        gfx.disableScissor();

        int rows = (int) Math.ceil((double) filtered.size() / (double) columns);
        float contentH = (float) rows * cell;
        float maxScroll = Math.max(0.0F, contentH - h);
        if (maxScroll > 0.0F) {
            float thumbH = Math.max(18.0F, h * h / contentH);
            float thumbY = y + 4.0F + (h - thumbH - 8.0F) * (this.listScroll / maxScroll);
            gfx.fill((int) (x + w - 3), (int) (y + 4), (int) (x + w - 1), (int) (y + h - 4), 0x12FFFFFF);
            gfx.fill((int) (x + w - 3), (int) thumbY, (int) (x + w - 1), (int) (thumbY + thumbH), accent(200));
        }
    }

    private void renderCell(GuiGraphicsExtractor gfx, Block block, float x, float y, float cell, float mouseX, float mouseY, float gridX, float gridW) {
        boolean hovered = isInside(mouseX, mouseY, x, y, cell, cell);
        String id = BlockMapSetting.getId(block);
        boolean selected = this.setting.contains(id);
        Integer color = selected ? this.setting.getColor(id) : null;

        int base = 22;
        int hov = hovered ? 8 : 0;
        gfx.fill((int) (x + 2), (int) (y + 2), (int) (x + cell - 2), (int) (y + cell - 2), ColorUtil.rgba(base + hov, base + hov, base + 4 + hov, 255));
        gfx.fill((int) (x + 2), (int) (y + 2), (int) (x + cell - 2), (int) (y + 3), selected ? ColorUtil.withAlpha(color, 255) : hovered ? 0x5AFFFFFF : 0xFF2D3038);
        gfx.fill((int) (x + 2), (int) (y + cell - 3), (int) (x + cell - 2), (int) (y + cell - 2), selected ? ColorUtil.withAlpha(color, 255) : hovered ? 0x5AFFFFFF : 0xFF2D3038);
        gfx.fill((int) (x + 2), (int) y + 2, (int) (x + 3), (int) (y + cell - 2), selected ? ColorUtil.withAlpha(color, 255) : hovered ? 0x5AFFFFFF : 0xFF2D3038);
        gfx.fill((int) (x + cell - 3), (int) (y + 2), (int) (x + cell - 2), (int) (y + cell - 2), selected ? ColorUtil.withAlpha(color, 255) : hovered ? 0x5AFFFFFF : 0xFF2D3038);

        float iconSize = Math.min(32.0F, cell - 16.0F);
        float iconX = x + cell / 2.0F - iconSize / 2.0F;
        float iconY = y + 4.0F;
        ItemStack stack = block.asItem() == null ? null : new ItemStack(block.asItem());
        if (stack != null && !stack.isEmpty()) {
            Matrix3x2fStack pose = gfx.pose();
            pose.pushMatrix();
            pose.translate(iconX, iconY);
            pose.scale(iconSize / 16.0F, iconSize / 16.0F);
            gfx.item(stack, 0, 0);
            pose.popMatrix();
        } else {
            gfx.fill((int) iconX, (int) iconY, (int) (iconX + iconSize), (int) (iconY + iconSize), 0xFF464A54);
        }

        String name = block.getName().getString();
        float maxW = cell - 10.0F;
        while (this.textWidth(name) > maxW && name.length() > 1) {
            name = name.substring(0, name.length() - 1);
        }
        gfx.text(this.font, Component.literal(name), (int) (x + cell / 2.0F - this.textWidth(name) / 2.0F), (int) (y + cell - 10), 0xFFDEE0E6);
    }

    private void renderPanel(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        float px = this.panelX();
        float py = this.gridY();
        float ph = this.gridHeight();
        gfx.fill((int) px, (int) py, (int) (px + PANEL_W), (int) (py + ph), 0xE10F1116);
        gfx.fill((int) px, (int) py, (int) (px + PANEL_W), (int) (py + 1), accent(70));
        gfx.fill((int) px, (int) (py + ph - 1), (int) (px + PANEL_W), (int) (py + ph), accent(70));
        gfx.fill((int) px, (int) py, (int) (px + 1), (int) (py + ph), accent(70));
        gfx.fill((int) (px + PANEL_W - 1), (int) py, (int) (px + PANEL_W), (int) (py + ph), accent(70));

        gfx.text(this.font, Component.literal("Настройка блока"), (int) px + 12, (int) py + 10, 0xFFE6E8EE);
        if (this.editing == null) {
            gfx.text(this.font, Component.literal("Кликни по блоку слева,"), (int) px + 12, (int) py + 36, 0xFF969AA4);
            gfx.text(this.font, Component.literal("чтобы настроить его цвет"), (int) px + 12, (int) py + 46, 0xFF969AA4);
            return;
        }

        String id = BlockMapSetting.getId(this.editing);
        int color = this.setting.getColor(id);
        String name = this.editing.getName().getString();
        float maxNameW = PANEL_W - 24.0F;
        while (this.textWidth(name) > maxNameW && name.length() > 1) {
            name = name.substring(0, name.length() - 1);
        }
        gfx.text(this.font, Component.literal(name), (int) px + 12, (int) py + 30, 0xFFF0F2F8);

        gfx.fill((int) px + 12, (int) py + 46, (int) px + 58, (int) py + 68, ColorUtil.withAlpha(color, 255));
        gfx.fill((int) px + 12, (int) py + 46, (int) px + 58, (int) py + 47, 0x46FFFFFF);
        gfx.fill((int) px + 57, (int) py + 46, (int) px + 58, (int) py + 68, 0x46FFFFFF);
        gfx.text(this.font, Component.literal("#" + String.format("%02X%02X%02X", ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color))), (int) px + 66, (int) py + 54, 0xFFDEE0E6);

        float pTop = this.paletteTop();
        gfx.text(this.font, Component.literal("Палитра"), (int) px + 12, (int) (pTop - 12), 0xFFAAAEB8);
        for (int i = 0; i < PALETTE.length; i++) {
            float sx = px + 12.0F + (float) (i % PALETTE_COLS) * (SWATCH + SWATCH_GAP);
            float sy = pTop + (float) (i / PALETTE_COLS) * (SWATCH + SWATCH_GAP);
            boolean hovered = isInside(mouseX, mouseY, sx, sy, SWATCH, SWATCH);
            boolean active = (color & 0xFFFFFF) == (PALETTE[i] & 0xFFFFFF);
            gfx.fill((int) sx, (int) sy, (int) (sx + SWATCH), (int) (sy + SWATCH), PALETTE[i]);
            gfx.fill((int) sx, (int) sy, (int) (sx + SWATCH), (int) (sy + 1), active ? 0xFFFFFFFF : hovered ? 0x78FFFFFF : 0x8C000000);
            gfx.fill((int) sx, (int) (sy + SWATCH - 1), (int) (sx + SWATCH), (int) (sy + SWATCH), active ? 0xFFFFFFFF : hovered ? 0x78FFFFFF : 0x8C000000);
            gfx.fill((int) sx, (int) sy, (int) (sx + 1), (int) (sy + SWATCH), active ? 0xFFFFFFFF : hovered ? 0x78FFFFFF : 0x8C000000);
            gfx.fill((int) (sx + SWATCH - 1), (int) sy, (int) (sx + SWATCH), (int) (sy + SWATCH), active ? 0xFFFFFFFF : hovered ? 0x78FFFFFF : 0x8C000000);
        }

        float sBase = this.sliderBase();
        gfx.text(this.font, Component.literal("RGB"), (int) px + 12, (int) (sBase - 12), 0xFFAAAEB8);
        float sliderW = PANEL_W - 24.0F;
        for (int channel = 0; channel < 3; channel++) {
            this.renderSlider(gfx, mouseX, mouseY, px + 12.0F, sBase + (float) channel * 26.0F, channel == 0 ? "R" : channel == 1 ? "G" : "B", channel, color, sliderW);
        }

        float pvTop = this.previewTop();
        gfx.text(this.font, Component.literal("Предпросмотр"), (int) px + 12, (int) (pvTop - 12), 0xFFAAAEB8);
        gfx.fill((int) px + 12, (int) pvTop, (int) (px + PANEL_W - 12), (int) (pvTop + 18), ColorUtil.withAlpha(color, 255));

        float btTop = this.buttonsTop();
        float btnW = (PANEL_W - 24.0F - 10.0F) / 2.0F;
        this.renderButton(gfx, mouseX, mouseY, px + 12.0F, btTop, btnW, "Убрать");
        this.renderButton(gfx, mouseX, mouseY, px + 12.0F + btnW + 10.0F, btTop, btnW, "Готово");
    }

    private void renderSlider(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float x, float y, String label, int channel, int color, float w) {
        int value = channel == 0 ? ColorUtil.red(color) : channel == 1 ? ColorUtil.green(color) : ColorUtil.blue(color);
        gfx.text(this.font, Component.literal(label), (int) x, (int) y, 0xFFC8CCD4);
        gfx.text(this.font, Component.literal(String.valueOf(value)), (int) (x + w - this.textWidth(String.valueOf(value))), (int) y, 0xFF969AA4);

        float trackY = y + 10.0F;
        gfx.fill((int) x, (int) trackY, (int) (x + w), (int) (trackY + 4), 0xFF2A2C34);
        gfx.fill((int) x, (int) trackY, (int) (x + w * (float) value / 255.0F), (int) (trackY + 4), accent(255));
        float knob = w * (float) value / 255.0F;
        boolean hovered = isInside(mouseX, mouseY, x, trackY - 4.0F, w, 12.0F);
        gfx.fill((int) (x + knob - 3), (int) (trackY - 3), (int) (x + knob + 3), (int) (trackY + 7), (this.draggingChannel == channel || hovered) ? accent(255) : accent(180));
    }

    private void renderButton(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float x, float y, float w, String label) {
        boolean hovered = isInside(mouseX, mouseY, x, y, w, BTN_H);
        gfx.fill((int) x, (int) y, (int) (x + w), (int) (y + BTN_H), hovered ? accent(120) : 0xFF282C36);
        gfx.centeredText(this.font, Component.literal(label), (int) (x + w / 2.0F), (int) y + 5, 0xFFE6E8EE);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
        float mx = (float) event.x();
        float my = (float) event.y();
        if (event.button() == 0) {
            float exitX = this.width - MARGIN - 46.0F;
            if (isInside(mx, my, exitX, MARGIN, 46.0F, 18.0F)) {
                Minecraft.getInstance().gui.setScreen(null);
                return true;
            }

            float sx = this.gridX();
            float sy = this.gridY() - SEARCH_H - 8.0F;
            if (isInside(mx, my, sx, sy, this.gridWidth(), SEARCH_H)) {
                this.searchFocused = true;
                return true;
            }
            this.searchFocused = false;

            float gx = this.gridX();
            float gy = this.gridY();
            if (isInside(mx, my, gx, gy, this.gridWidth(), this.gridHeight())) {
                List<Block> filtered = this.filtered();
                int columns = this.columns();
                float cell = this.cellSize();
                int col = (int) ((mx - gx) / cell);
                int row = (int) ((my - (gy - this.listScroll)) / cell);
                int index = col + row * columns;
                if (col >= 0 && row >= 0 && index >= 0 && index < filtered.size()) {
                    this.selectBlock(filtered.get(index));
                }
                return true;
            }

            if (this.editing != null) {
                float px = this.panelX();
                float pTop = this.paletteTop();
                for (int i = 0; i < PALETTE.length; i++) {
                    float cxp = px + 12.0F + (float) (i % PALETTE_COLS) * (SWATCH + SWATCH_GAP);
                    float cyp = pTop + (float) (i / PALETTE_COLS) * (SWATCH + SWATCH_GAP);
                    if (isInside(mx, my, cxp, cyp, SWATCH, SWATCH)) {
                        this.setting.set(BlockMapSetting.getId(this.editing), PALETTE[i]);
                        this.feature.markDirty();
                        return true;
                    }
                }
                float sBase = this.sliderBase();
                float sliderW = PANEL_W - 24.0F;
                for (int channel = 0; channel < 3; channel++) {
                    float syp = sBase + (float) channel * 26.0F + 6.0F;
                    if (isInside(mx, my, px + 12.0F, syp, sliderW, 12.0F)) {
                        this.draggingChannel = channel;
                        this.updateSlider((int) mx, channel);
                        return true;
                    }
                }
                float pvTop = this.previewTop();
                float btTop = this.buttonsTop();
                float btnW = (PANEL_W - 24.0F - 10.0F) / 2.0F;
                if (isInside(mx, my, px + 12.0F, btTop, btnW, BTN_H)) {
                    this.setting.remove(BlockMapSetting.getId(this.editing));
                    this.feature.markDirty();
                    this.editing = null;
                    return true;
                }
                if (isInside(mx, my, px + 12.0F + btnW + 10.0F, btTop, btnW, BTN_H)) {
                    Minecraft.getInstance().gui.setScreen(null);
                    return true;
                }
            }
            return true;
        }

        if (event.button() == 1) {
            float gx = this.gridX();
            float gy = this.gridY();
            if (isInside(mx, my, gx, gy, this.gridWidth(), this.gridHeight())) {
                List<Block> filtered = this.filtered();
                int columns = this.columns();
                float cell = this.cellSize();
                int col = (int) ((mx - gx) / cell);
                int row = (int) ((my - (gy - this.listScroll)) / cell);
                int index = col + row * columns;
                if (col >= 0 && row >= 0 && index >= 0 && index < filtered.size()) {
                    Block block = filtered.get(index);
                    String id = BlockMapSetting.getId(block);
                    if (this.setting.contains(id)) {
                        this.setting.remove(id);
                        this.feature.markDirty();
                        if (this.editing == block) {
                            this.editing = null;
                        }
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, flag);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.draggingChannel = -1;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isInside((float) mouseX, (float) mouseY, this.gridX(), this.gridY(), this.gridWidth(), this.gridHeight())) {
            this.listScroll -= (float) verticalAmount * 34.0F;
            this.clampScroll(this.filtered().size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchFocused) {
            if (event.key() == 259) {
                if (!this.searchBuffer.isEmpty()) {
                    this.searchBuffer = this.searchBuffer.substring(0, this.searchBuffer.length() - 1);
                }
            } else if (event.key() == 256) {
                this.searchFocused = false;
            }
            return true;
        }
        if (event.key() == 256) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchFocused && event.isAllowedChatCharacter()) {
            if (this.searchBuffer.length() < 32) {
                this.searchBuffer = this.searchBuffer + event.codepointAsString();
            }
            return true;
        }
        return super.charTyped(event);
    }
}