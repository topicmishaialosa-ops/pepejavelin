package tech.huihui.client.screens.dropdowngui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.setting.Setting;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.modules.impl.render.ClickGUI;
import tech.huihui.client.modules.impl.render.EditClickGUI;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class Csgui1Screen extends Screen {
    private static final float RAIL_W = 54.0F;
    private static final float GAP = 8.0F;
    private static final float HEADER_H = 42.0F;
    private static final float ROW_H = 26.0F;
    private static final float ICON_H = 46.0F;
    private static final float PAD = 8.0F;
    private static final float SET_GAP = 4.0F;
    private static final float CHIP_H = 15.0F;
    private static final float PICKER_H = 48.0F;

    private static final ColorRGBA RAIL_BG = new ColorRGBA(16, 18, 24, 235);
    private static final ColorRGBA TEXT_MAIN = new ColorRGBA(224, 226, 232);
    private static final ColorRGBA TEXT_DIM = new ColorRGBA(146, 150, 160);
    private static final ColorRGBA ROW_BG = new ColorRGBA(255, 255, 255, 10);
    private static final ColorRGBA ROW_ACTIVE = new ColorRGBA(255, 255, 255, 16);

    private static float transformScale = 1.0F;
    private static int screenWidth;
    private static int screenHeight;

    private final List<Category> categories = List.of(Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.RENDER, Category.MISC);
    private final EnumMap<Category, Float> scroll = new EnumMap<>(Category.class);
    private final Map<Module, Boolean> expanded = new HashMap<>();
    private final Map<Module, Float> hover = new HashMap<>();
    private final Map<Module, Float> expandProgress = new HashMap<>();
    private final Map<Setting, Float> toggleProgress = new HashMap<>();
    private final Map<ColorSetting, float[]> hsb = new HashMap<>();

    private Category selected = Category.COMBAT;
    private boolean closing;
    private NumberSetting draggingSlider;
    private ColorSetting draggingPicker;
    private boolean draggingHue;
    private StringSetting editingString;
    private boolean bindCapturing;
    private KeySetting bindSetting;
    private String search = "";
    private boolean searchFocused;
    private float scale = 1.0F;

    public Csgui1Screen() {
        super(Text.literal("ClickGUI"));
        for (Category cat : this.categories) {
            this.scroll.put(cat, 0.0F);
        }
    }

    public static Csgui1Screen getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Csgui1Screen INSTANCE = new Csgui1Screen();
    }

    @Override
    protected void init() {
        this.closing = false;
        this.draggingSlider = null;
        this.draggingPicker = null;
        this.bindCapturing = false;
        this.bindSetting = null;
        this.editingString = null;
        this.search = "";
        this.searchFocused = false;
        super.init();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
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

    private float panelW() {
        return EditClickGUI.INSTANCE.getWidth().getCurrent();
    }

    private float panelH() {
        return EditClickGUI.INSTANCE.getHeight().getCurrent();
    }

    private int panelOpacity() {
        return (int) EditClickGUI.INSTANCE.getOpacity().getCurrent();
    }

    private float panelRadius() {
        return EditClickGUI.INSTANCE.getRadius().getCurrent();
    }

    private ColorRGBA panelBg() {
        return EditClickGUI.INSTANCE.getBgColor().getColor().withAlpha(this.panelOpacity());
    }

    private ColorRGBA accent() {
        Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA c = theme != null ? theme.getColor() : new ColorRGBA(96, 130, 255);
        return c;
    }

    private float totalW() {
        return RAIL_W + GAP + this.panelW();
    }

    private float startX() {
        return ((float) screenWidth - this.totalW()) / 2.0F;
    }

    private float startY() {
        return Math.max(24.0F, ((float) screenHeight - this.panelH()) / 2.0F);
    }

    private float railX() {
        return this.startX();
    }

    private float panelX() {
        return this.startX() + RAIL_W + GAP;
    }

    private void updateLayout() {
        screenWidth = this.width;
        screenHeight = this.height;
        float fitScale = Math.min((float) (screenWidth - 16) / this.totalW(), (float) (screenHeight - 40) / (this.panelH() + 40.0F));
        fitScale = MathHelper.clamp(fitScale, 0.5F, 1.0F);
        this.scale = MathHelper.clamp(fitScale * EditClickGUI.INSTANCE.getScale().getCurrent(), 0.35F, fitScale);
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

    private List<Module> modulesOf(Category cat) {
        List<Module> mods = new ArrayList<>();
        for (Module module : HuihuiClient.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == cat && this.visible(module)) {
                mods.add(module);
            }
        }
        return mods;
    }

    private boolean visible(Module module) {
        if (this.search.isEmpty()) {
            return true;
        }
        String query = this.search.toLowerCase(Locale.ROOT);
        return module.getName().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean isOpen(Module module) {
        return this.expanded.getOrDefault(module, false);
    }

    private boolean hasVisibleSettings(Module module) {
        for (Setting setting : module.getSettings()) {
            if (setting.isVisible()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.closing) {
            this.closeScreen();
            return;
        }
        this.updateLayout();
        CustomDrawContext draw = CustomDrawContext.of(context);
        this.drawBackground(draw);

        context.getMatrices().push();
        context.getMatrices().translate((float) screenWidth / 2.0F, (float) screenHeight / 2.0F, 0.0F);
        context.getMatrices().scale(this.scale, this.scale, 1.0F);
        context.getMatrices().translate((float) (-screenWidth) / 2.0F, (float) (-screenHeight) / 2.0F, 0.0F);

        float lx = this.inverseX(mouseX);
        float ly = this.inverseY(mouseY);
        this.renderRail(draw, lx, ly);
        this.renderPanel(draw, lx, ly);
        context.getMatrices().pop();
    }

    private void drawBackground(CustomDrawContext draw) {
        draw.drawRect(0.0F, 0.0F, (float) this.width, (float) this.height, new ColorRGBA(4, 5, 8, 220));
        DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, (float) this.width, (float) this.height, BorderRadius.ZERO,
                this.accent().withAlpha(50), new ColorRGBA(4, 5, 8, 20), new ColorRGBA(4, 5, 8, 20), new ColorRGBA(4, 5, 8, 230));
    }

    private void renderRail(CustomDrawContext draw, float mouseX, float mouseY) {
        float x = this.railX();
        float y = this.startY();
        float radius = this.panelRadius();
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, RAIL_W, this.panelH(), BorderRadius.all(radius), RAIL_BG);
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, RAIL_W, this.panelH(), 1.0F, BorderRadius.all(radius), this.accent().withAlpha(40));

        draw.drawText(Fonts.ICONS.getFont(10.0F), "G", x + RAIL_W / 2.0F - this.textWidth("G", 10.0F) / 2.0F, y + 10.0F, this.accent().withAlpha(230));

        float iconY = y + 32.0F;
        for (int i = 0; i < this.categories.size(); i++) {
            Category cat = this.categories.get(i);
            float cy = iconY + (float) i * ICON_H;
            boolean selected = cat == this.selected;
            boolean hovered = mouseX >= x + 7.0F && mouseX <= x + RAIL_W - 7.0F && mouseY >= cy && mouseY <= cy + ICON_H;
            if (selected || hovered) {
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + 7.0F, cy + 3.0F, RAIL_W - 14.0F, ICON_H - 6.0F, BorderRadius.all(10.0F), selected ? this.accent().withAlpha(60) : new ColorRGBA(255, 255, 255, 12));
            }
            ColorRGBA iconColor = selected ? this.accent().withAlpha(255) : (hovered ? TEXT_MAIN : TEXT_DIM);
            draw.drawText(Fonts.ICONS.getFont(13.0F), cat.getIcon(), x + RAIL_W / 2.0F - this.textWidth(cat.getIcon(), 13.0F) / 2.0F, cy + ICON_H / 2.0F - 8.0F, iconColor);
        }
    }

    private void renderPanel(CustomDrawContext draw, float mouseX, float mouseY) {
        float x = this.panelX();
        float y = this.startY();
        float w = this.panelW();
        float h = this.panelH();
        float radius = this.panelRadius();
        ColorRGBA bg = this.panelBg();
        if (EditClickGUI.INSTANCE.getBlur().isEnabled()) {
            DrawUtil.drawBlur(draw.getMatrices(), x, y, w, h, 11.0F, BorderRadius.all(radius), bg);
        }
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, w, h, BorderRadius.all(radius), bg);
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(radius), EditClickGUI.INSTANCE.getBorderColor().getColor().withAlpha(60));

        draw.drawText(Fonts.BOLD.getFont(11.0F), this.selected.getName(), x + PAD, y + 13.0F, TEXT_MAIN);
        draw.drawText(Fonts.ICONS.getFont(9.0F), this.selected.getIcon(), x + w - PAD - 14.0F, y + 14.0F, this.accent().withAlpha(200));

        this.renderSearch(draw, x + PAD, y + HEADER_H - 18.0F, w - PAD * 2.0F);

        this.renderModuleList(draw, x, y, w, h, mouseX, mouseY);
    }

    private void renderSearch(CustomDrawContext draw, float x, float y, float width) {
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, 16.0F, BorderRadius.all(5.0F), new ColorRGBA(255, 255, 255, 12));
        boolean active = this.searchFocused || !this.search.isEmpty();
        if (active) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, width, 16.0F, 1.0F, BorderRadius.all(5.0F), this.accent().withAlpha(120));
        }
        if (this.search.isEmpty() && !this.searchFocused) {
            draw.drawText(Fonts.LUPA.getFont(5.0F), "\uf002", x + 5.0F, y + 5.5F, TEXT_DIM);
            draw.drawText(this.font(7.5F), "Поиск", x + 14.0F, y + 5.0F, TEXT_DIM);
        } else {
            String text = this.search + (this.searchFocused && System.currentTimeMillis() % 900L < 450L ? "|" : "");
            draw.drawText(this.font(7.5F), text, x + 6.0F, y + 5.0F, TEXT_MAIN);
        }
    }

    private void renderModuleList(CustomDrawContext draw, float x, float y, float w, float h, float mouseX, float mouseY) {
        List<Module> mods = this.modulesOf(this.selected);
        float maxScroll = this.calculateMaxScroll(mods);
        float offset = MathHelper.clamp(this.scroll.getOrDefault(this.selected, 0.0F), 0.0F, maxScroll);
        this.scroll.put(this.selected, offset);

        this.scissor(draw, x + 1.0F, y + HEADER_H, w - 2.0F, h - HEADER_H - 2.0F);
        float currentY = y + HEADER_H + 4.0F - offset;
        for (Module module : mods) {
            if (!this.visible(module)) {
                continue;
            }
            boolean open = this.isOpen(module) && this.hasVisibleSettings(module);
            float target = open ? 1.0F : 0.0F;
            float progress = this.expandProgress.getOrDefault(module, target);
            progress += (target - progress) * 0.22F;
            if (Math.abs(target - progress) < 0.002F) {
                progress = target;
            }
            this.expandProgress.put(module, progress);
            float settingsHeight = this.visibleSettingsHeight(module) * progress;
            float totalHeight = ROW_H + settingsHeight;
            if (currentY + totalHeight >= y + HEADER_H && currentY <= y + h) {
                this.renderModule(draw, module, x + 5.0F, currentY, w - 10.0F, totalHeight, mouseX, mouseY);
            }
            currentY += totalHeight + 4.0F;
        }
        draw.disableScissor();

        if (maxScroll > 0.0F) {
            float trackHeight = h - HEADER_H - 12.0F;
            float thumbHeight = Math.max(18.0F, trackHeight * trackHeight / (trackHeight + maxScroll));
            float thumbY = y + HEADER_H + 4.0F + (trackHeight - thumbHeight) * (offset / maxScroll);
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - 4.0F, y + HEADER_H + 4.0F, 2.0F, trackHeight, BorderRadius.all(1.0F), new ColorRGBA(255, 255, 255, 20));
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + w - 4.0F, thumbY, 2.0F, thumbHeight, BorderRadius.all(1.0F), this.accent().withAlpha(200));
        }
    }

    private float calculateMaxScroll(List<Module> mods) {
        float total = 0.0F;
        for (Module module : mods) {
            total += ROW_H + 4.0F;
            if (this.isOpen(module) && this.hasVisibleSettings(module)) {
                total += this.visibleSettingsHeight(module);
            }
        }
        return Math.max(0.0F, total - (this.panelH() - HEADER_H - 8.0F));
    }

    private void renderModule(CustomDrawContext draw, Module module, float x, float y, float width, float totalHeight, float mouseX, float mouseY) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_H;
        float hp = this.hover.getOrDefault(module, 0.0F);
        hp += ((isHovered ? 1.0F : 0.0F) - hp) * 0.16F;
        this.hover.put(module, hp);

        ColorRGBA bg = module.isEnabled() ? new ColorRGBA(255, 255, 255, (int) (14.0F + hp * 10.0F)) : new ColorRGBA(255, 255, 255, (int) (5.0F + hp * 9.0F));
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, ROW_H, BorderRadius.all(8.0F), bg);
        if (module.isEnabled()) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, y + 7.0F, 3.0F, ROW_H - 14.0F, BorderRadius.all(2.0F), this.accent().withAlpha(255));
        }
        draw.drawText(Fonts.MEDIUM.getFont(8.5F), module.getName(), x + 12.0F, y + 8.0F, module.isEnabled() ? TEXT_MAIN : new ColorRGBA(176, 180, 190));

        float bx = x + width - 16.0F;
        float by = y + ROW_H / 2.0F - 1.0F;
        ColorRGBA chevron = this.accent().withAlpha(this.isOpen(module) ? 255 : 120);
        if (this.hasVisibleSettings(module)) {
            if (this.isOpen(module)) {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(bx - 2.5F, by + 1.0F), new Vec2f(bx, by - 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(bx, by - 1.5F), new Vec2f(bx + 2.5F, by + 1.0F), chevron);
            } else {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(bx - 2.5F, by - 1.0F), new Vec2f(bx, by + 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(bx, by + 1.5F), new Vec2f(bx + 2.5F, by - 1.0F), chevron);
            }
        }

        if (totalHeight > ROW_H) {
            this.scissor(draw, x, y + ROW_H, width, totalHeight - ROW_H);
            this.renderSettings(draw, module, x + 10.0F, y + ROW_H, width - 20.0F, mouseX, mouseY);
            draw.disableScissor();
        }
    }

    private float settingHeight(Setting setting) {
        float h;
        if (setting instanceof BooleanSetting || setting instanceof KeySetting || setting instanceof StringSetting || setting instanceof ButtonSetting) {
            h = 17.0F;
        } else if (setting instanceof NumberSetting) {
            h = 24.0F;
        } else if (setting instanceof ModeSetting mode) {
            h = 14.0F + this.choiceRowHeight(this.modeNames(mode), this.panelW() - 30.0F) + SET_GAP;
        } else if (setting instanceof MultiBooleanSetting multi) {
            h = 14.0F + this.choiceRowHeight(this.multiNames(multi), this.panelW() - 30.0F) + SET_GAP;
        } else if (setting instanceof ColorSetting color) {
            h = 17.0F;
            if (this.colorPickerOpen(color)) {
                h += PICKER_H + 6.0F;
            }
        } else {
            h = 17.0F;
        }
        return h;
    }

    private float visibleSettingsHeight(Module module) {
        float result = 3.0F;
        for (Setting setting : module.getSettings()) {
            if (setting.isVisible()) {
                result += this.settingHeight(setting) + SET_GAP;
            }
        }
        return result;
    }

    private List<String> modeNames(ModeSetting mode) {
        List<String> names = new ArrayList<>();
        for (ModeSetting.Value value : mode.getValues()) {
            names.add(value.getName());
        }
        return names;
    }

    private List<String> multiNames(MultiBooleanSetting multi) {
        List<String> names = new ArrayList<>();
        for (MultiBooleanSetting.Value value : multi.getBooleanSettings()) {
            names.add(value.getName());
        }
        return names;
    }

    private List<List<String>> buildChoiceRows(List<String> choices, float width) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        float rowWidth = 0.0F;
        for (String choice : choices) {
            float itemWidth = this.choiceWidth(choice);
            float required = row.isEmpty() ? itemWidth : itemWidth + 3.0F;
            if (!row.isEmpty() && rowWidth + required > width) {
                rows.add(row);
                row = new ArrayList<>();
                rowWidth = 0.0F;
                required = itemWidth;
            }
            row.add(choice);
            rowWidth += required;
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return rows;
    }

    private float choiceRowHeight(List<String> choices, float width) {
        return (float) this.buildChoiceRows(choices, width).size() * (CHIP_H + 3.0F) - 3.0F;
    }

    private float choiceWidth(String choice) {
        return Math.max(22.0F, this.textWidth(choice, 7.0F) + 10.0F);
    }

    private void renderSettings(CustomDrawContext draw, Module module, float x, float y, float width, float mouseX, float mouseY) {
        float currentY = y + 2.0F;
        for (Setting setting : module.getSettings()) {
            if (!setting.isVisible()) {
                continue;
            }
            if (setting instanceof BooleanSetting bool) {
                draw.drawText(this.font(7.5F), bool.getName(), x, currentY + 3.0F, TEXT_MAIN);
                float toggle = this.toggleProgress.getOrDefault(setting, bool.isEnabled() ? 1.0F : 0.0F);
                toggle += ((bool.isEnabled() ? 1.0F : 0.0F) - toggle) * 0.22F;
                if (Math.abs((bool.isEnabled() ? 1.0F : 0.0F) - toggle) < 0.002F) {
                    toggle = bool.isEnabled() ? 1.0F : 0.0F;
                }
                this.toggleProgress.put(setting, toggle);
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 26.0F, currentY + 1.0F, 24.0F, 13.0F, BorderRadius.all(7.0F), toggle > 0.01F ? this.accent().withAlpha(220) : new ColorRGBA(255, 255, 255, 22));
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 21.0F + toggle * 12.0F - 4.5F, currentY + 2.0F, 9.0F, 11.0F, BorderRadius.all(999.0F), ColorRGBA.WHITE);
                currentY += 17.0F + SET_GAP;
            } else if (setting instanceof KeySetting key) {
                draw.drawText(this.font(7.5F), key.getName(), x, currentY + 3.0F, TEXT_MAIN);
                String keyName = key.getKeyCode() > 0 ? key.getNameKey() : "Нету";
                if (this.bindCapturing && this.bindSetting == key) {
                    keyName = "...";
                }
                float bx = x + width - Math.max(34.0F, this.textWidth(keyName, 7.5F) + 14.0F);
                DrawUtil.drawRoundedRect(draw.getMatrices(), bx, currentY + 1.0F, x + width - bx, 13.0F, BorderRadius.all(4.0F), this.accent().withAlpha(this.bindCapturing && this.bindSetting == key ? 200 : 90));
                draw.drawText(this.font(7.5F), keyName, bx + 5.0F, currentY + 4.0F, ColorRGBA.WHITE);
                currentY += 17.0F + SET_GAP;
            } else if (setting instanceof NumberSetting slider) {
                draw.drawText(this.font(7.5F), slider.getName(), x, currentY, TEXT_MAIN);
                String value = String.format(Locale.US, "%.1f", slider.getCurrent());
                draw.drawText(this.font(7.5F), value, x + width - this.textWidth(value, 7.5F), currentY, this.accent().withAlpha(220));
                float trackY = currentY + 14.0F;
                float pct = (slider.getMax() - slider.getMin()) <= 0.0F ? 0.0F : (slider.getCurrent() - slider.getMin()) / (slider.getMax() - slider.getMin());
                DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, width, 3.0F, BorderRadius.all(2.0F), new ColorRGBA(255, 255, 255, 16));
                DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, Math.max(3.0F, width * pct), 3.0F, BorderRadius.all(2.0F), this.accent().withAlpha(255));
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width * pct - 4.5F, trackY - 3.0F, 9.0F, 9.0F, BorderRadius.all(999.0F), ColorRGBA.WHITE);
                currentY += 24.0F + SET_GAP;
            } else if (setting instanceof ModeSetting mode) {
                draw.drawText(this.font(7.5F), mode.getName() + ": " + mode.get(), x, currentY, TEXT_MAIN);
                currentY += 14.0F;
                currentY = this.renderChoiceRow(draw, this.modeNames(mode), mode.get(), null, x, currentY, width);
                currentY += SET_GAP;
            } else if (setting instanceof MultiBooleanSetting multi) {
                List<String> selected = multi.getSelectedNames();
                draw.drawText(this.font(7.5F), multi.getName() + ": " + String.join(", ", selected), x, currentY, TEXT_MAIN);
                currentY += 14.0F;
                currentY = this.renderChoiceRow(draw, this.multiNames(multi), null, multi, x, currentY, width);
                currentY += SET_GAP;
            } else if (setting instanceof ColorSetting color) {
                draw.drawText(this.font(7.5F), color.getName(), x, currentY + 3.0F, TEXT_MAIN);
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 20.0F, currentY + 2.0F, 18.0F, 12.0F, BorderRadius.all(3.5F), color.getColor());
                DrawUtil.drawRoundedBorder(draw.getMatrices(), x + width - 20.0F, currentY + 2.0F, 18.0F, 12.0F, 1.0F, BorderRadius.all(3.5F), new ColorRGBA(255, 255, 255, 30));
                if (this.colorPickerOpen(color)) {
                    this.renderColorPicker(draw, color, x, currentY + 17.0F, width, mouseX, mouseY);
                }
                currentY += this.settingHeight(color) + SET_GAP;
            } else if (setting instanceof StringSetting stringSetting) {
                draw.drawText(this.font(7.5F), stringSetting.getName(), x, currentY + 3.0F, TEXT_MAIN);
                boolean editing = this.editingString == stringSetting;
                float tx = x + width - Math.max(34.0F, this.textWidth(stringSetting.getValue(), 7.5F) + 14.0F);
                DrawUtil.drawRoundedRect(draw.getMatrices(), tx, currentY + 1.0F, x + width - tx, 13.0F, BorderRadius.all(4.0F), new ColorRGBA(255, 255, 255, editing ? 26 : 12));
                if (editing) {
                    DrawUtil.drawRoundedBorder(draw.getMatrices(), tx, currentY + 1.0F, x + width - tx, 13.0F, 1.0F, BorderRadius.all(4.0F), this.accent().withAlpha(160));
                }
                String shown = stringSetting.getValue() + (editing && System.currentTimeMillis() % 900L < 450L ? "|" : "");
                draw.drawText(this.font(7.5F), shown, tx + 5.0F, currentY + 4.0F, TEXT_MAIN);
                currentY += 17.0F + SET_GAP;
            } else if (setting instanceof ButtonSetting button) {
                DrawUtil.drawRoundedRect(draw.getMatrices(), x, currentY + 1.0F, width, 14.0F, BorderRadius.all(4.0F), this.accent().withAlpha(110));
                draw.drawText(this.font(7.5F), button.getName(), x + width / 2.0F - this.textWidth(button.getName(), 7.5F) / 2.0F, currentY + 4.0F, ColorRGBA.WHITE);
                currentY += 17.0F + SET_GAP;
            } else {
                currentY += 17.0F + SET_GAP;
            }
        }
    }

    private boolean colorPickerOpen(ColorSetting setting) {
        return this.hsb.containsKey(setting);
    }

    private float[] hsbOf(ColorSetting setting) {
        float[] values = this.hsb.get(setting);
        if (values == null) {
            values = new float[]{setting.getColor().getHue(), setting.getColor().getSaturation(), setting.getColor().getBrightness()};
            this.hsb.put(setting, values);
        }
        return values;
    }

    private void renderColorPicker(CustomDrawContext draw, ColorSetting setting, float x, float y, float width, float mouseX, float mouseY) {
        float[] hsb = this.hsbOf(setting);
        float pickerW = width * 0.76F;
        if (this.draggingPicker == setting) {
            if (this.draggingHue) {
                hsb[0] = MathHelper.clamp((mouseY - y) / PICKER_H, 0.0F, 1.0F);
            } else {
                hsb[1] = MathHelper.clamp((mouseX - x - 4.0F) / (pickerW - 8.0F), 0.0F, 1.0F);
                hsb[2] = 1.0F - MathHelper.clamp((mouseY - y - 4.0F) / (PICKER_H - 8.0F), 0.0F, 1.0F);
            }
            this.applyHsb(setting, hsb);
        }

        ColorRGBA hue = ColorRGBA.fromHSB(hsb[0], 1.0F, 1.0F);
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, pickerW, PICKER_H, BorderRadius.all(5.0F), ColorRGBA.WHITE, ColorRGBA.BLACK, ColorRGBA.BLACK, hue);

        float knobX = x + 4.0F + hsb[1] * (pickerW - 8.0F);
        float knobY = y + 4.0F + (1.0F - hsb[2]) * (PICKER_H - 8.0F);
        DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 4.0F, knobY - 4.0F, 8.0F, 8.0F, BorderRadius.all(999.0F), ColorRGBA.BLACK);
        DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F, BorderRadius.all(999.0F), ColorRGBA.WHITE);

        float sliderX = x + pickerW + 5.0F;
        for (int i = 0; i < (int) PICKER_H; i++) {
            float rowHue = (float) i / PICKER_H;
            DrawUtil.drawRect(draw.getMatrices(), sliderX, y + (float) i, 4.0F, 1.0F, ColorRGBA.fromHSB(rowHue, 1.0F, 1.0F));
        }
        float hueKnobY = y + hsb[0] * PICKER_H;
        DrawUtil.drawRoundedRect(draw.getMatrices(), sliderX - 2.5F, hueKnobY - 4.0F, 9.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK);
        DrawUtil.drawRoundedRect(draw.getMatrices(), sliderX - 1.5F, hueKnobY - 3.0F, 7.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE);
    }

    private void applyHsb(ColorSetting setting, float[] hsb) {
        setting.setColor(ColorRGBA.fromHSB(hsb[0], hsb[1], hsb[2]));
    }

    private float renderChoiceRow(CustomDrawContext draw, List<String> choices, String selected, MultiBooleanSetting multi, float x, float y, float width) {
        float rowY = y;
        for (List<String> row : this.buildChoiceRows(choices, width)) {
            float currentX = x;
            for (String choice : row) {
                float choiceWidth = this.choiceWidth(choice);
                boolean active = multi != null ? multi.isEnable(choice) : choice.equals(selected);
                DrawUtil.drawRoundedRect(draw.getMatrices(), currentX, rowY, choiceWidth, CHIP_H, BorderRadius.all(5.0F), active ? this.accent().withAlpha(180) : new ColorRGBA(255, 255, 255, 16));
                draw.drawText(this.font(7.0F), choice, currentX + choiceWidth / 2.0F - this.textWidth(choice, 7.0F) / 2.0F, rowY + 4.0F, active ? ColorRGBA.WHITE : new ColorRGBA(178, 182, 192));
                currentX += choiceWidth + 3.0F;
            }
            rowY += CHIP_H + 3.0F;
        }
        return rowY - 3.0F;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.closing) {
            return true;
        }
        float mx = this.inverseX(mouseX);
        float my = this.inverseY(mouseY);
        float startY = this.startY();

        if (mx >= this.railX() && mx <= this.railX() + RAIL_W && my >= startY && my <= startY + this.panelH()) {
            float iconY = startY + 32.0F;
            for (int i = 0; i < this.categories.size(); i++) {
                float cy = iconY + (float) i * ICON_H;
                if (my >= cy && my <= cy + ICON_H) {
                    if (this.selected != this.categories.get(i)) {
                        this.selected = this.categories.get(i);
                        this.scroll.put(this.selected, 0.0F);
                        this.search = "";
                    }
                    return true;
                }
            }
            return true;
        }

        float px = this.panelX();
        float pw = this.panelW();
        if (mx >= px && mx <= px + pw && my >= startY && my <= startY + this.panelH()) {
            if (my >= startY + HEADER_H - 18.0F && my <= startY + HEADER_H - 2.0F) {
                this.searchFocused = true;
                this.editingString = null;
                this.bindCapturing = false;
                return true;
            }
            if (my < startY + HEADER_H) {
                return true;
            }
            if (button == 0) {
                if (this.handleSettingClick(mx, my, px, startY)) {
                    return true;
                }
                Module module = this.getModuleAt(mx, my, px, startY);
                if (module != null) {
                    module.toggle();
                    return true;
                }
            } else if (button == 1) {
                Module module = this.getModuleAt(mx, my, px, startY);
                if (module != null && this.hasVisibleSettings(module)) {
                    boolean value = !this.isOpen(module);
                    this.expanded.put(module, value);
                    return true;
                }
            }
        }
        if (button == 0) {
            this.searchFocused = false;
            this.editingString = null;
            this.bindCapturing = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Module getModuleAt(float mouseX, float mouseY, float x, float y) {
        float currentY = y + HEADER_H + 4.0F - this.scroll.getOrDefault(this.selected, 0.0F);
        for (Module module : this.modulesOf(this.selected)) {
            boolean open = this.isOpen(module) && this.hasVisibleSettings(module);
            float progress = this.expandProgress.getOrDefault(module, open ? 1.0F : 0.0F);
            float total = ROW_H + this.visibleSettingsHeight(module) * progress;
            if (mouseX >= x + 5.0F && mouseX <= x + this.panelW() - 5.0F && mouseY >= currentY && mouseY <= currentY + ROW_H) {
                return module;
            }
            currentY += total + 4.0F;
        }
        return null;
    }

    private boolean handleSettingClick(float mouseX, float mouseY, float panelX, float panelY) {
        float currentY = panelY + HEADER_H + 4.0F - this.scroll.getOrDefault(this.selected, 0.0F);
        for (Module module : this.modulesOf(this.selected)) {
            boolean open = this.isOpen(module) && this.hasVisibleSettings(module);
            float progress = this.expandProgress.getOrDefault(module, open ? 1.0F : 0.0F);
            float settingsHeight = this.visibleSettingsHeight(module) * progress;
            if (open && settingsHeight > 0.0F && mouseY >= currentY + ROW_H && mouseY <= currentY + ROW_H + settingsHeight) {
                float settingY = currentY + ROW_H + 2.0F;
                float contentX = panelX + 15.0F;
                float contentW = this.panelW() - 30.0F;
                for (Setting setting : module.getSettings()) {
                    if (!setting.isVisible()) {
                        continue;
                    }
                    float height = this.settingHeight(setting);
                    if (setting instanceof BooleanSetting bool) {
                        float switchX = contentX + contentW - 26.0F;
                        float switchY = settingY + 1.0F;
                        if (mouseX >= switchX && mouseX <= switchX + 24.0F && mouseY >= switchY && mouseY <= switchY + 13.0F) {
                            bool.setEnabled(!bool.isEnabled());
                            return true;
                        }
                        settingY += height + SET_GAP;
                        continue;
                    }
                    if (setting instanceof KeySetting key) {
                        float bx = contentX + contentW - Math.max(34.0F, this.textWidth(key.getKeyCode() > 0 ? key.getNameKey() : "Нету", 7.5F) + 14.0F);
                        if (mouseY >= settingY + 1.0F && mouseY <= settingY + 14.0F && mouseX >= bx && mouseX <= contentX + contentW) {
                            this.bindCapturing = true;
                            this.bindSetting = key;
                            this.editingString = null;
                            return true;
                        }
                    }
                    if (mouseY >= settingY && mouseY <= settingY + height) {
                        if (setting instanceof NumberSetting slider) {
                            if (mouseY >= settingY + 11.0F) {
                                this.draggingSlider = slider;
                                this.updateSlider(slider, mouseX, contentX, contentW);
                                return true;
                            }
                        }
                        if (setting instanceof ModeSetting mode) {
                            if (mouseY > settingY + 14.0F) {
                                String choice = this.choiceAt(mouseX, mouseY, contentX, settingY + 14.0F, contentW, this.modeNames(mode));
                                if (choice != null) {
                                    mode.set(choice);
                                }
                            }
                            return true;
                        }
                        if (setting instanceof MultiBooleanSetting multi) {
                            if (mouseY > settingY + 14.0F) {
                                String choice = this.choiceAt(mouseX, mouseY, contentX, settingY + 14.0F, contentW, this.multiNames(multi));
                                if (choice != null) {
                                    MultiBooleanSetting.Value value = multi.getValueByName(choice);
                                    if (value != null) {
                                        value.toggle();
                                    }
                                }
                            }
                            return true;
                        }
                        if (setting instanceof ColorSetting color) {
                            float swatchX = contentX + contentW - 20.0F;
                            if (mouseX >= swatchX && mouseX <= swatchX + 18.0F && mouseY >= settingY + 2.0F && mouseY <= settingY + 14.0F) {
                                if (this.hsb.containsKey(color)) {
                                    this.hsb.remove(color);
                                } else {
                                    this.hsbOf(color);
                                }
                                return true;
                            }
                            if (this.colorPickerOpen(color)) {
                                float pickerW = contentW * 0.76F;
                                float sliderX = contentX + pickerW + 5.0F;
                                if (mouseX >= sliderX && mouseX <= sliderX + 4.0F && mouseY >= settingY + 17.0F && mouseY <= settingY + 17.0F + PICKER_H) {
                                    this.draggingPicker = color;
                                    this.draggingHue = true;
                                    return true;
                                }
                                if (mouseX >= contentX && mouseX <= contentX + pickerW && mouseY >= settingY + 17.0F && mouseY <= settingY + 17.0F + PICKER_H) {
                                    this.draggingPicker = color;
                                    this.draggingHue = false;
                                    return true;
                                }
                            }
                        }
                        if (setting instanceof ButtonSetting button) {
                            button.toggle();
                            return true;
                        }
                        if (setting instanceof StringSetting stringSetting) {
                            this.editingString = this.editingString == stringSetting ? null : stringSetting;
                            this.bindCapturing = false;
                            return true;
                        }
                    }
                    settingY += height + SET_GAP;
                }
            }
            currentY += ROW_H + 4.0F + settingsHeight;
        }
        return false;
    }

    private String choiceAt(float mouseX, float mouseY, float x, float y, float width, List<String> choices) {
        float rowY = y;
        for (List<String> row : this.buildChoiceRows(choices, width)) {
            float currentX = x;
            for (String choice : row) {
                float itemWidth = this.choiceWidth(choice);
                if (mouseX >= currentX && mouseX <= currentX + itemWidth && mouseY >= rowY && mouseY <= rowY + CHIP_H) {
                    return choice;
                }
                currentX += itemWidth + 3.0F;
            }
            rowY += CHIP_H + 3.0F;
        }
        return null;
    }

    private void updateSlider(NumberSetting slider, float mouseX, float x, float width) {
        float progress = MathHelper.clamp((mouseX - x) / width, 0.0F, 1.0F);
        float value = slider.getMin() + progress * (slider.getMax() - slider.getMin());
        float increment = Math.max(0.0001F, slider.getIncrement());
        float rounded = (float) Math.round(value / increment) * increment;
        slider.setCurrent(MathHelper.clamp(rounded, slider.getMin(), slider.getMax()));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingSlider != null && button == 0) {
            float mx = this.inverseX(mouseX);
            if (mx >= this.panelX() + 15.0F && mx <= this.panelX() + this.panelW() - 15.0F) {
                this.updateSlider(this.draggingSlider, mx, this.panelX() + 15.0F, this.panelW() - 30.0F);
            }
            return true;
        }
        if (this.draggingPicker != null && button == 0) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingSlider = null;
            this.draggingPicker = null;
            this.draggingHue = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.closing) {
            return true;
        }
        float mx = this.inverseX(mouseX);
        float my = this.inverseY(mouseY);
        float px = this.panelX();
        float startY = this.startY();
        if (mx >= px && mx <= px + this.panelW() && my >= startY && my <= startY + this.panelH()) {
            float max = this.calculateMaxScroll(this.modulesOf(this.selected));
            float next = this.scroll.getOrDefault(this.selected, 0.0F) - (float) verticalAmount * 18.0F;
            this.scroll.put(this.selected, MathHelper.clamp(next, 0.0F, max));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.closing) {
            return true;
        }
        if (this.bindCapturing && this.bindSetting != null) {
            if (keyCode == 256 || keyCode == 261) {
                this.bindSetting.setKeyCode(-1);
            } else {
                this.bindSetting.setKeyCode(keyCode);
            }
            this.bindCapturing = false;
            this.bindSetting = null;
            return true;
        }
        if (this.editingString != null) {
            if (keyCode == 259) {
                String current = this.editingString.getValue();
                if (!current.isEmpty()) {
                    this.editingString.setValue(current.substring(0, current.length() - 1));
                }
            } else if (keyCode == 257 || keyCode == 256) {
                this.editingString = null;
            }
            return true;
        }
        if (this.searchFocused) {
            if (keyCode == 259 && !this.search.isEmpty()) {
                this.search = this.search.substring(0, this.search.length() - 1);
            } else if (keyCode == 256 || keyCode == 257) {
                this.searchFocused = false;
            }
            return true;
        }
        if (keyCode == 256) {
            this.closeScreen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.closing) {
            return true;
        }
        if (this.editingString != null && chr >= 32 && chr != 127) {
            String current = this.editingString.getValue();
            if (current.length() < 24) {
                this.editingString.setValue(current + chr);
            }
            return true;
        }
        if (this.searchFocused && chr >= 32 && chr != 127) {
            this.search += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void closeScreen() {
        if (this.closing) {
            ClickGUI.INSTANCE.setEnabled(false);
            this.client.setScreen(null);
            return;
        }
        this.closing = true;
    }

    @Override
    public void removed() {
        this.closing = true;
        this.draggingSlider = null;
        this.draggingPicker = null;
        this.bindCapturing = false;
        this.editingString = null;
    }
}
