package tech.huihui.client.screens.block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.setting.impl.BlockMapSetting;
import tech.huihui.client.modules.impl.render.BlockESP;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class BlockPickerScreen extends Screen implements IMinecraft {
    private static final float MARGIN = 12.0F;
    private static final float SEARCH_H = 22.0F;
    private static final float PANEL_W = 250.0F;
    private static final float PANEL_GAP = 10.0F;
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

    private static float transformScale = 1.0F;
    private static int screenWidth;
    private static int screenHeight;

    private final List<Block> blocks = new ArrayList();
    private final BlockMapSetting setting;
    private final Map<Block, Float> cellHover = new HashMap<>();
    private Block editing;
    private String searchBuffer = "";
    private boolean searchFocused;
    private float listScroll;
    private int draggingChannel = -1;
    private float scale = 1.0F;

    public BlockPickerScreen() {
        super(Text.literal("Выбор блоков"));
        this.setting = BlockESP.INSTANCE.getBlocks();
        for (int i = 0; i < Registries.BLOCK.size(); i++) {
            Block block = Registries.BLOCK.get(i);
            if (block != null) {
                this.blocks.add(block);
            }
        }
        this.blocks.sort(Comparator.comparing((block) -> block.getName().getString()));
    }

    public static void open() {
        if (mc.currentScreen instanceof BlockPickerScreen) {
            return;
        }
        mc.setScreen(new BlockPickerScreen());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Font font(float size) {
        return Fonts.REGULAR.getFont(size);
    }

    private float textWidth(String text, float size) {
        return Fonts.REGULAR.getWidth(text, size);
    }

    private ColorRGBA accent() {
        Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
        return theme != null ? theme.getColor() : new ColorRGBA(96, 130, 255);
    }

    private float gridX() {
        return MARGIN;
    }

    private float gridY() {
        return MARGIN + 24.0F + SEARCH_H + 8.0F;
    }

    private float gridWidth() {
        return this.width - MARGIN * 2.0F - PANEL_W - PANEL_GAP;
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
        return this.panelX() + 12.0F + 74.0F;
    }

    private float sliderBase() {
        return this.panelX() + 12.0F + 92.0F + (float) this.paletteRows() * (SWATCH + SWATCH_GAP);
    }

    private float previewTop() {
        return this.sliderBase() + 3.0F * 26.0F + 6.0F;
    }

    private float buttonsTop() {
        return this.previewTop() + 24.0F;
    }

    private void updateLayout() {
        screenWidth = this.width;
        screenHeight = this.height;
        float fitScale = Math.min((float) (screenWidth - 20) / 820.0F, (float) (screenHeight - 20) / 480.0F);
        fitScale = MathHelper.clamp(fitScale, 0.45F, 1.0F);
        this.scale = fitScale;
        transformScale = this.scale;
    }

    private static float transformX(float x) {
        return (float) screenWidth / 2.0F + (x - (float) screenWidth / 2.0F) * transformScale;
    }

    private static float transformY(float y) {
        return (float) screenHeight / 2.0F + (y - (float) screenHeight / 2.0F) * transformScale;
    }

    private float inverseX(double x) {
        float s = Math.max(transformScale, 0.01F);
        return (float) screenWidth / 2.0F + (float) (x - (double) screenWidth / 2.0D) / s;
    }

    private float inverseY(double y) {
        float s = Math.max(transformScale, 0.01F);
        return (float) screenHeight / 2.0F + (float) (y - (double) screenHeight / 2.0D) / s;
    }

    private void scissor(CustomDrawContext draw, float x, float y, float width, float height) {
        draw.enableScissor((int) transformX(x), (int) transformY(y), (int) transformX(x + width), (int) transformY(y + height));
    }

    private List<Block> filtered() {
        String query = this.searchBuffer.trim().toLowerCase();
        if (query.isEmpty()) {
            return this.blocks;
        }
        List<Block> result = new ArrayList();
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
        this.listScroll = MathHelper.clamp(this.listScroll, 0.0F, max);
    }

    private void selectBlock(Block block) {
        String id = BlockMapSetting.getId(block);
        if (!this.setting.contains(id)) {
            int color = this.accent().getRGB();
            this.setting.set(id, color);
            BlockESP.INSTANCE.markDirty();
        }
        this.editing = block;
    }

    private void updateSlider(int mouseX, int channel) {
        if (this.editing == null) {
            return;
        }
        float x = this.panelX() + 12.0F;
        float w = PANEL_W - 24.0F;
        float percent = MathHelper.clamp((float) (mouseX - x) / w, 0.0F, 1.0F);
        int value = Math.round(percent * 255.0F);
        ColorRGBA color = new ColorRGBA(this.setting.getColor(BlockMapSetting.getId(this.editing)));
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        if (channel == 0) {
            red = value;
        } else if (channel == 1) {
            green = value;
        } else {
            blue = value;
        }
        this.setting.set(BlockMapSetting.getId(this.editing), new ColorRGBA(red, green, blue, 255).getRGB());
        BlockESP.INSTANCE.markDirty();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        super.render(context, mouseX, mouseY, tickDelta);
        this.updateLayout();
        if (this.draggingChannel >= 0) {
            this.updateSlider((int) this.inverseX(mouseX), this.draggingChannel);
        }
        CustomDrawContext draw = CustomDrawContext.of(context);
        ColorRGBA themeColor = this.accent();

        DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, this.width, this.height, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 130));

        context.getMatrices().push();
        context.getMatrices().translate((float) screenWidth / 2.0F, (float) screenHeight / 2.0F, 0.0F);
        context.getMatrices().scale(this.scale, this.scale, 1.0F);
        context.getMatrices().translate((float) (-screenWidth) / 2.0F, (float) (-screenHeight) / 2.0F, 0.0F);

        float lx = this.inverseX(mouseX);
        float ly = this.inverseY(mouseY);

        draw.drawText(Fonts.BOLD.getFont(10.0F), "Выбор блоков", MARGIN, MARGIN, new ColorRGBA(224, 226, 232));
        draw.drawText(font(5.0F), "ЛКМ — добавить/выбрать, ПКМ — убрать", MARGIN, MARGIN + 12.0F, new ColorRGBA(150, 154, 164));

        this.renderExit(draw, themeColor, lx, ly);
        this.renderSearch(draw, themeColor, lx, ly);
        this.renderGrid(draw, themeColor, lx, ly);
        this.renderPanel(draw, themeColor, lx, ly);

        context.getMatrices().pop();
    }

    private void renderExit(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY) {
        float x = this.width - MARGIN - 46.0F;
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, MARGIN, 46.0F, 18.0F);
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, MARGIN, 46.0F, 18.0F, BorderRadius.all(4.0F), hovered ? themeColor.withAlpha(120) : new ColorRGBA(20, 22, 28, 200));
        draw.drawText(font(6.0F), "Выход", x + 23.0F - this.textWidth("Выход", 6.0F) / 2.0F, MARGIN + 6.0F, new ColorRGBA(230, 232, 238));
    }

    private void renderSearch(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY) {
        float x = this.gridX();
        float y = this.gridY() - SEARCH_H - 8.0F;
        float w = this.gridWidth();
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, w, SEARCH_H);
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, SEARCH_H, BorderRadius.all(5.0F), new ColorRGBA(18, 20, 26, 235));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, SEARCH_H, 1.0F, BorderRadius.all(5.0F), this.searchFocused ? themeColor.withAlpha(180) : (hovered ? themeColor.withAlpha(90) : new ColorRGBA(50, 54, 64, 255)));
        draw.drawText(Fonts.LUPA.getFont(5.0F), "\uf002", x + 6.0F, y + 7.5F, new ColorRGBA(140, 144, 154));
        if (this.searchBuffer.isEmpty() && !this.searchFocused) {
            draw.drawText(font(6.0F), "Поиск блока...", x + 15.0F, y + 7.0F, new ColorRGBA(120, 124, 134));
        } else {
            String text = this.searchBuffer + (this.searchFocused && System.currentTimeMillis() % 900L < 450L ? "|" : "");
            draw.drawText(font(6.0F), text, x + 15.0F, y + 7.0F, new ColorRGBA(230, 232, 238));
        }
    }

    private void renderGrid(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY) {
        float x = this.gridX();
        float y = this.gridY();
        float w = this.gridWidth();
        float h = this.gridHeight();
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(6.0F), new ColorRGBA(15, 17, 22, 220));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(6.0F), themeColor.withAlpha(50));

        List<Block> filtered = this.filtered();
        this.clampScroll(filtered.size());
        int columns = this.columns();
        float cell = this.cellSize();

        this.scissor(draw, x + 2.0F, y + 2.0F, w - 4.0F, h - 4.0F);
        for (int i = 0; i < filtered.size(); i++) {
            Block block = filtered.get(i);
            float cellX = x + (float) (i % columns) * cell;
            float cellY = y - this.listScroll + (float) (i / columns) * cell;
            if (cellY + cell < y || cellY > y + h) {
                continue;
            }
            this.renderCell(draw, block, cellX, cellY, cell, mouseX, mouseY);
        }
        draw.disableScissor();

        int rows = (int) Math.ceil((double) filtered.size() / (double) columns);
        float contentH = (float) rows * cell;
        float maxScroll = Math.max(0.0F, contentH - h);
        if (maxScroll > 0.0F) {
            float thumbH = Math.max(18.0F, h * h / contentH);
            float thumbY = y + 4.0F + (h - thumbH - 8.0F) * (this.listScroll / maxScroll);
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - 3.0F, y + 4.0F, 2.0F, h - 8.0F, BorderRadius.all(1.0F), new ColorRGBA(255, 255, 255, 18));
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - 3.0F, thumbY, 2.0F, thumbH, BorderRadius.all(1.0F), themeColor.withAlpha(200));
        }
    }

    private void renderCell(CustomDrawContext draw, Block block, float x, float y, float cell, float mouseX, float mouseY) {
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + 2.0F, y + 2.0F, cell - 4.0F, cell - 4.0F);
        float hp = this.cellHover.getOrDefault(block, 0.0F);
        hp += ((hovered ? 1.0F : 0.0F) - hp) * 0.18F;
        this.cellHover.put(block, hp);

        String id = BlockMapSetting.getId(block);
        boolean selected = this.setting.contains(id);
        Integer color = selected ? this.setting.getColor(id) : null;

        int base = 22;
        DrawUtil.drawRoundedRect(draw.getMatrices(), x + 2.0F, y + 2.0F, cell - 4.0F, cell - 4.0F, BorderRadius.all(5.0F), new ColorRGBA(base + (int) (8.0F * hp), base + (int) (8.0F * hp), base + 4 + (int) (10.0F * hp)));
        ColorRGBA border = selected ? new ColorRGBA(color) : (hovered ? new ColorRGBA(255, 255, 255).withAlpha(90) : new ColorRGBA(45, 48, 56));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x + 2.0F, y + 2.0F, cell - 4.0F, cell - 4.0F, selected ? 2.0F : 1.0F, BorderRadius.all(5.0F), border);

        float iconSize = Math.min(26.0F, cell - 22.0F);
        ItemStack stack = new ItemStack(block.asItem());
        float iconX = x + cell / 2.0F - iconSize / 2.0F;
        float iconY = y + 6.0F;
        if (!stack.isEmpty()) {
            draw.getMatrices().push();
            draw.getMatrices().translate(iconX, iconY, 0.0F);
            draw.getMatrices().scale(iconSize / 16.0F, iconSize / 16.0F, 1.0F);
            draw.drawItem(stack, 0, 0);
            draw.getMatrices().pop();
        } else {
            DrawUtil.drawRoundedRect(draw.getMatrices(), iconX, iconY, iconSize, iconSize, BorderRadius.all(4.0F), new ColorRGBA(70, 74, 84));
        }

        String name = block.getName().getString();
        float maxW = cell - 10.0F;
        while (this.textWidth(name, 4.5F) > maxW && name.length() > 1) {
            name = name.substring(0, name.length() - 1);
        }
        draw.drawText(font(4.5F), name, x + cell / 2.0F - this.textWidth(name, 4.5F) / 2.0F, y + cell - 12.0F, new ColorRGBA(222, 224, 230));
    }

    private void renderPanel(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY) {
        float px = this.panelX();
        float py = this.gridY();
        float ph = this.gridHeight();
        DrawUtil.drawRoundedRect(draw.getMatrices(), px, py, PANEL_W, ph, BorderRadius.all(6.0F), new ColorRGBA(15, 17, 22, 225));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), px, py, PANEL_W, ph, 1.0F, BorderRadius.all(6.0F), themeColor.withAlpha(70));

        this.scissor(draw, px + 1.0F, py + 1.0F, PANEL_W - 2.0F, ph - 2.0F);
        draw.drawText(Fonts.BOLD.getFont(7.5F), "Настройка блока", px + 12.0F, py + 10.0F, new ColorRGBA(230, 232, 238));

        if (this.editing == null) {
            draw.drawText(font(5.0F), "Кликни по блоку слева,", px + 12.0F, py + 36.0F, new ColorRGBA(150, 154, 164));
            draw.drawText(font(5.0F), "чтобы настроить его цвет", px + 12.0F, py + 46.0F, new ColorRGBA(150, 154, 164));
            draw.disableScissor();
            return;
        }

        String id = BlockMapSetting.getId(this.editing);
        int color = this.setting.getColor(id);
        String name = this.editing.getName().getString();
        float maxNameW = PANEL_W - 24.0F;
        while (this.textWidth(name, 7.0F) > maxNameW && name.length() > 1) {
            name = name.substring(0, name.length() - 1);
        }
        draw.drawText(Fonts.MEDIUM.getFont(7.0F), name, px + 12.0F, py + 30.0F, new ColorRGBA(240, 242, 248));

        ColorRGBA current = new ColorRGBA(color);
        DrawUtil.drawRoundedRect(draw.getMatrices(), px + 12.0F, py + 46.0F, 46.0F, 22.0F, BorderRadius.all(5.0F), current);
        DrawUtil.drawRoundedBorder(draw.getMatrices(), px + 12.0F, py + 46.0F, 46.0F, 22.0F, 1.0F, BorderRadius.all(5.0F), new ColorRGBA(255, 255, 255, 70));
        draw.drawText(font(5.5F), "#" + String.format("%02X%02X%02X", current.getRed(), current.getGreen(), current.getBlue()), px + 66.0F, py + 54.0F, new ColorRGBA(222, 224, 230));

        float pTop = py + 78.0F;
        draw.drawText(font(5.0F), "Палитра", px + 12.0F, pTop - 12.0F, new ColorRGBA(170, 174, 184));
        for (int i = 0; i < PALETTE.length; i++) {
            float sx = px + 12.0F + (float) (i % PALETTE_COLS) * (SWATCH + SWATCH_GAP);
            float sy = pTop + (float) (i / PALETTE_COLS) * (SWATCH + SWATCH_GAP);
            boolean hovered = MathUtil.isHovered(mouseX, mouseY, sx, sy, SWATCH, SWATCH);
            boolean active = (color & 16777215) == (PALETTE[i] & 16777215);
            DrawUtil.drawRoundedRect(draw.getMatrices(), sx, sy, SWATCH, SWATCH, BorderRadius.all(4.0F), ColorRGBA.fromInt(PALETTE[i]));
            DrawUtil.drawRoundedBorder(draw.getMatrices(), sx, sy, SWATCH, SWATCH, active ? 2.0F : 1.0F, BorderRadius.all(4.0F), active ? new ColorRGBA(255, 255, 255) : (hovered ? new ColorRGBA(255, 255, 255, 120) : new ColorRGBA(0, 0, 0, 140)));
        }

        float sBase = pTop + (float) this.paletteRows() * (SWATCH + SWATCH_GAP) + 10.0F;
        draw.drawText(font(5.0F), "RGB", px + 12.0F, sBase - 12.0F, new ColorRGBA(170, 174, 184));
        float sliderW = PANEL_W - 24.0F;
        for (int channel = 0; channel < 3; channel++) {
            this.renderSlider(draw, themeColor, mouseX, mouseY, px + 12.0F, sBase + (float) channel * 26.0F, channel == 0 ? "R" : channel == 1 ? "G" : "B", channel, color, sliderW);
        }

        float pvTop = sBase + 3.0F * 26.0F + 8.0F;
        draw.drawText(font(5.0F), "Предпросмотр", px + 12.0F, pvTop - 12.0F, new ColorRGBA(170, 174, 184));
        ColorRGBA preview = new ColorRGBA(current.getRed(), current.getGreen(), current.getBlue());
        DrawUtil.drawRoundedRect(draw.getMatrices(), px + 12.0F, pvTop, PANEL_W - 24.0F, 18.0F, BorderRadius.all(4.0F), preview, preview, preview.mix(ColorRGBA.BLACK, 0.35F), preview.mix(ColorRGBA.BLACK, 0.35F));

        float btTop = pvTop + 24.0F;
        float btnW = (PANEL_W - 24.0F - 10.0F) / 2.0F;
        this.renderButton(draw, themeColor, mouseX, mouseY, px + 12.0F, btTop, btnW, "Убрать");
        this.renderButton(draw, themeColor, mouseX, mouseY, px + 12.0F + btnW + 10.0F, btTop, btnW, "Готово");

        draw.disableScissor();
    }

    private void renderSlider(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY, float x, float y, String label, int channel, int color, float w) {
        ColorRGBA base = new ColorRGBA(color);
        int red = base.getRed();
        int green = base.getGreen();
        int blue = base.getBlue();
        int value = channel == 0 ? red : channel == 1 ? green : blue;
        draw.drawText(font(5.0F), label, x, y, new ColorRGBA(200, 204, 212));
        draw.drawText(font(5.0F), String.valueOf(value), x + w - this.textWidth(String.valueOf(value), 5.0F), y, new ColorRGBA(150, 154, 164));

        float trackY = y + 10.0F;
        ColorRGBA c0 = new ColorRGBA(channel == 0 ? 0 : red, channel == 1 ? 0 : green, channel == 2 ? 0 : blue);
        ColorRGBA c1 = new ColorRGBA(channel == 0 ? 255 : red, channel == 1 ? 255 : green, channel == 2 ? 255 : blue);
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, w, 4.0F, BorderRadius.all(2.0F), c0, c0, c1, c1);
        float knob = w * (float) value / 255.0F;
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, trackY - 4.0F, w, 12.0F);
        DrawUtil.drawRoundedRect(draw.getMatrices(), x + knob - 3.0F, trackY - 3.0F, 6.0F, 10.0F, BorderRadius.all(3.0F), (this.draggingChannel == channel || hovered) ? themeColor.withAlpha(255) : themeColor.withAlpha(180));
    }

    private void renderButton(CustomDrawContext draw, ColorRGBA themeColor, float mouseX, float mouseY, float x, float y, float w, String label) {
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, w, BTN_H);
        ColorRGBA fill = hovered ? themeColor.withAlpha(120) : new ColorRGBA(40, 44, 54);
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, BTN_H, BorderRadius.all(4.0F), fill);
        draw.drawText(font(5.5F), label, x + w / 2.0F - this.textWidth(label, 5.5F) / 2.0F, y + 6.0F, new ColorRGBA(230, 232, 238));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float mx = this.inverseX(mouseX);
        float my = this.inverseY(mouseY);

        if (button == 0) {
            float exitX = this.width - MARGIN - 46.0F;
            if (MathUtil.isHovered(mx, my, exitX, MARGIN, 46.0F, 18.0F)) {
                this.close();
                return true;
            }

            float sx = this.gridX();
            float sy = this.gridY() - SEARCH_H - 8.0F;
            if (MathUtil.isHovered(mx, my, sx, sy, this.gridWidth(), SEARCH_H)) {
                this.searchFocused = true;
                return true;
            }
            this.searchFocused = false;

            float gx = this.gridX();
            float gy = this.gridY();
            if (MathUtil.isHovered(mx, my, gx, gy, this.gridWidth(), this.gridHeight())) {
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
                float pTop = this.gridY() + 78.0F;
                for (int i = 0; i < PALETTE.length; i++) {
                    float cxp = px + 12.0F + (float) (i % PALETTE_COLS) * (SWATCH + SWATCH_GAP);
                    float cyp = pTop + (float) (i / PALETTE_COLS) * (SWATCH + SWATCH_GAP);
                    if (MathUtil.isHovered(mx, my, cxp, cyp, SWATCH, SWATCH)) {
                        this.setting.set(BlockMapSetting.getId(this.editing), PALETTE[i]);
                        BlockESP.INSTANCE.markDirty();
                        return true;
                    }
                }
                float sBase = pTop + (float) this.paletteRows() * (SWATCH + SWATCH_GAP) + 10.0F;
                float sliderW = PANEL_W - 24.0F;
                for (int channel = 0; channel < 3; channel++) {
                    float syp = sBase + (float) channel * 26.0F + 6.0F;
                    if (MathUtil.isHovered(mx, my, px + 12.0F, syp, sliderW, 12.0F)) {
                        this.draggingChannel = channel;
                        this.updateSlider((int) mx, channel);
                        return true;
                    }
                }
                float pvTop = sBase + 3.0F * 26.0F + 8.0F;
                float btTop = pvTop + 24.0F;
                float btnW = (PANEL_W - 24.0F - 10.0F) / 2.0F;
                if (MathUtil.isHovered(mx, my, px + 12.0F, btTop, btnW, BTN_H)) {
                    this.setting.remove(BlockMapSetting.getId(this.editing));
                    BlockESP.INSTANCE.markDirty();
                    this.editing = null;
                    return true;
                }
                if (MathUtil.isHovered(mx, my, px + 12.0F + btnW + 10.0F, btTop, btnW, BTN_H)) {
                    this.close();
                    return true;
                }
            }
            return true;
        }

        if (button == 1) {
            float gx = this.gridX();
            float gy = this.gridY();
            if (MathUtil.isHovered(mx, my, gx, gy, this.gridWidth(), this.gridHeight())) {
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
                        BlockESP.INSTANCE.markDirty();
                        if (this.editing == block) {
                            this.editing = null;
                        }
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingChannel = -1;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float mx = this.inverseX(mouseX);
        float my = this.inverseY(mouseY);
        if (MathUtil.isHovered(mx, my, this.gridX(), this.gridY(), this.gridWidth(), this.gridHeight())) {
            this.listScroll -= (float) verticalAmount * 34.0F;
            this.clampScroll(this.filtered().size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.searchBuffer.isEmpty()) {
                    this.searchBuffer = this.searchBuffer.substring(0, this.searchBuffer.length() - 1);
                }
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchFocused = false;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchFocused) {
            if (this.searchBuffer.length() < 32) {
                this.searchBuffer = this.searchBuffer + chr;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        super.close();
    }
}
