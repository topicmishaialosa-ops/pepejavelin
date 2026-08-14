package tech.huihui.client.screens.dropdowngui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.font.MsdfFont;
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
import tech.huihui.utility.render.display.Keyboard;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.Gradient;
import tech.huihui.utility.render.display.base.ToggleSwitch;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public abstract class AbstractDropdownScreen extends Screen {
    protected static final float TAB_BAR_H = 30.0F;
    protected static final float PICKER_H = 54.0F;

    private static float transformScale = 1.0F;
    private static int screenWidth;
    private static int screenHeight;

    protected final List<Category> categories = List.of(Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.RENDER, Category.COSMETICS, Category.MISC);
    protected final EnumMap<Category, Float> scroll = new EnumMap<>(Category.class);
    protected final Map<Category, Float> displayScroll = new HashMap<>();
    protected final Map<Category, Float> displayVelocity = new HashMap<>();
    protected final Map<Module, Boolean> expanded = new HashMap<>();
    protected final Map<Module, Float> hover = new HashMap<>();
    protected final Map<Module, Float> expandProgress = new HashMap<>();
    protected final Map<Setting, Float> toggleProgress = new HashMap<>();
    protected final Map<Setting, Float> sliderProgress = new HashMap<>();
    protected final Map<ColorSetting, float[]> hsb = new HashMap<>();

    protected Category selectedCategory = Category.COMBAT;
    private boolean closing;
    private NumberSetting draggingSlider;
    private ColorSetting draggingPicker;
    private boolean draggingHue;
    private StringSetting editingString;
    private boolean bindCapturing;
    private KeySetting bindSetting;
    private Module bindingModule;
    private String search = "";
    private boolean searchFocused;
    private float scale = 1.0F;
    private final ThemePicker themePicker = new ThemePicker();

    protected AbstractDropdownScreen() {
        super(Text.literal("Dropdown ClickGUI"));
        for (Category cat : this.categories) {
            this.scroll.put(cat, 0.0F);
        }
    }

    protected abstract DropdownDesign design();

    @Override
    protected void init() {
        this.closing = false;
        this.draggingSlider = null;
        this.draggingPicker = null;
        this.bindCapturing = false;
        this.bindSetting = null;
        this.bindingModule = null;
        this.editingString = null;
        this.themePicker.reset();
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

    private MsdfFont uiFont() {
        return this.design().textFont;
    }

    private float textWidth(String text, float size) {
        return this.uiFont().getWidth(text, size);
    }

    private float panelWidth() {
        return EditClickGUI.INSTANCE.getWidth().getCurrent();
    }

    private float panelHeight() {
        return EditClickGUI.INSTANCE.getHeight().getCurrent();
    }

    private float panelGap() {
        return EditClickGUI.INSTANCE.getGap().getCurrent();
    }

    private float panelRadius() {
        return EditClickGUI.INSTANCE.getRadius().getCurrent();
    }

    private int panelOpacity() {
        return (int) EditClickGUI.INSTANCE.getOpacity().getCurrent();
    }

    private float contentWidth() {
        return this.panelWidthAt(0) - 14.0F - 2.0F * this.design().contentX;
    }

    private ColorRGBA panelBg() {
        return this.design().panelBg1.withAlpha(this.panelOpacity());
    }

    private boolean isTabs() {
        return this.design().layout == DropdownDesign.Layout.TABS;
    }

    private boolean isCards() {
        return this.design().layout == DropdownDesign.Layout.CARDS;
    }

    private int panelCount() {
        return this.isTabs() ? 1 : this.categories.size();
    }

    private float tabsWidth() {
        float max = Math.max(240.0F, (float) screenWidth - 24.0F);
        return MathHelper.clamp(this.panelWidth() * 1.5F, 240.0F, max);
    }

    private float panelWidthAt(int index) {
        return this.isTabs() ? this.tabsWidth() : this.panelWidth();
    }

    private float headerHeight() {
        return this.isTabs() ? TAB_BAR_H + 8.0F : this.design().headerHeight;
    }

    private float totalWidth() {
        return this.isTabs() ? this.tabsWidth() : (float) this.categories.size() * (this.panelWidth() + this.panelGap()) - this.panelGap();
    }

    private float startX() {
        return ((float) screenWidth - this.totalWidth()) / 2.0F;
    }

    private float startY() {
        return Math.max(24.0F, ((float) screenHeight - this.panelHeight()) / 2.0F);
    }

    private float panelX(int index) {
        return this.startX() + (float) index * (this.panelWidthAt(index) + this.panelGap());
    }

    private List<Category> panelCategories(int index) {
        return this.isTabs() ? List.of(this.selectedCategory) : List.of(this.categories.get(index));
    }

    private int accentIndex(Category cat) {
        return this.categories.indexOf(cat);
    }

    private ColorRGBA accent(int index, int alpha) {
        DropdownDesign d = this.design();
        ColorRGBA base = d.rainbow ? ColorRGBA.fromHSB((d.baseHue + (float) (index * 55 % 360)) / 360.0F, 0.55F, 1.0F) : d.accent;
        return base.withAlpha(alpha);
    }

    private float themeX() {
        return this.startX() + (this.totalWidth() - this.themePicker.width()) / 2.0F;
    }

    private float themeY() {
        return this.startY() - 34.0F - 8.0F - ThemePicker.BOX_HEIGHT;
    }

    private void renderTheme(CustomDrawContext draw, float mouseX, float mouseY) {
        Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
        this.themePicker.render(draw, theme, this.panelBg(), this.design().accent2, this.design().textMain, 1.0F, this.themeX(), this.themeY(), mouseX, mouseY, new ThemePicker.Scissor() {
            @Override
            public void enable(float x, float y, float width, float height) {
                AbstractDropdownScreen.this.scissor(draw, x, y, width, height);
            }

            @Override
            public void disable() {
                draw.disableScissor();
            }
        });
    }

    private void updateLayout() {
        screenWidth = this.width;
        screenHeight = this.height;
        float rowWidth = this.totalWidth();
        float height = this.panelHeight();
        float fitScale = Math.min((float) (screenWidth - 16) / rowWidth, (float) (screenHeight - 40) / (height + 40.0F));
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
            if (module.getCategory() == cat) {
                mods.add(module);
            }
        }
        return mods;
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

    private float settingHeight(Setting setting) {
        float h;
        if (setting instanceof BooleanSetting || setting instanceof KeySetting || setting instanceof StringSetting || setting instanceof ButtonSetting) {
            h = 17.0F;
        } else if (setting instanceof NumberSetting) {
            h = 24.0F;
        } else if (setting instanceof ModeSetting mode) {
            h = 15.0F + this.choiceRowHeight(this.modeNames(mode)) + this.design().settingGap;
        } else if (setting instanceof MultiBooleanSetting multi) {
            h = 15.0F + this.choiceRowHeight(this.multiNames(multi)) + this.design().settingGap;
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
                result += this.settingHeight(setting) + this.design().settingGap;
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

    private float choiceRowHeight(List<String> choices) {
        return (float) this.buildChoiceRows(choices, this.contentWidth()).size() * 17.0F - 3.0F;
    }

    private float choiceWidth(String choice) {
        return Math.max(24.0F, this.textWidth(choice, 7.0F) + 10.0F);
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

        float startX = this.startX();
        float startY = this.startY();
        float lx = this.inverseX(mouseX);
        float ly = this.inverseY(mouseY);
        this.renderSearch(draw, startX, startY - 34.0F, lx, ly);

        for (int i = 0; i < this.panelCount(); i++) {
            for (Category cat : this.panelCategories(i)) {
                this.renderPanel(draw, cat, i, this.panelX(i), startY, lx, ly);
            }
        }
        this.renderTheme(draw, lx, ly);
        context.getMatrices().pop();
    }

    private void drawBackground(CustomDrawContext draw) {
        DropdownDesign d = this.design();
        DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, (float) this.width, (float) this.height, BorderRadius.ZERO,
                d.screenTop, d.screenTop, d.screenBottom, d.screenBottom);
    }

    private void renderSearch(CustomDrawContext draw, float x, float y, float mouseX, float mouseY) {
        DropdownDesign d = this.design();
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, this.totalWidth(), 24.0F, BorderRadius.all(7.0F), this.panelBg());
        boolean active = this.searchFocused || !this.search.isEmpty();
        if (active) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, this.totalWidth(), 24.0F, 1.0F, BorderRadius.all(7.0F), d.accent2.withAlpha(120));
        }
        if (this.search.isEmpty() && !this.searchFocused) {
            draw.drawText(Fonts.LUPA.getFont(6.0F), "\uf002", x + 8.0F, y + 8.0F, d.textDim);
            draw.drawText(this.uiFont().getFont(9.0F), "Поиск функций", x + 24.0F, y + 8.0F, d.textMain);
        } else {
            String text = this.search + (this.searchFocused && System.currentTimeMillis() % 900L < 450L ? "|" : "");
            draw.drawText(this.uiFont().getFont(9.0F), text, x + 10.0F, y + 8.0F, d.textBright);
        }
    }

    private void renderPanel(CustomDrawContext draw, Category cat, int index, float x, float y, float mouseX, float mouseY) {
        DropdownDesign d = this.design();
        ColorRGBA accent = this.accent(this.accentIndex(cat), 255);
        float width = this.panelWidthAt(index);
        float height = this.panelHeight();
        float radius = this.panelRadius();
        int opacity = this.panelOpacity();
        ColorRGBA bg1 = d.panelBg1.withAlpha(opacity);
        ColorRGBA bg2 = d.panelBg2.withAlpha(opacity);
        if (EditClickGUI.INSTANCE.getBlur().isEnabled()) {
            DrawUtil.drawBlur(draw.getMatrices(), x, y, width, height, 11.0F, BorderRadius.all(radius), bg1);
        }
        if (d.shadowPanels) {
            DrawUtil.drawShadow(draw.getMatrices(), x - 3.0F, y - 3.0F, width + 6.0F, height + 6.0F, radius + 8.0F, BorderRadius.all(radius + 8.0F), d.cardShadow);
        }
        if (d.gradientPanels) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, height, BorderRadius.all(radius), Gradient.of(bg1, bg1, bg2, bg2));
        } else {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, height, BorderRadius.all(radius), bg1);
        }
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, width, height, d.borderWidth, BorderRadius.all(radius), d.border);

        if (this.isTabs()) {
            this.renderTabBar(draw, cat, x, y, width, mouseX, mouseY, accent);
        } else {
            draw.drawText(d.headerFont.getFont(11.0F), cat.getIcon(), x + 14.0F, y + 14.0F, accent);
            draw.drawText(d.moduleFont.getFont(9.0F), cat.getName(), x + 39.0F, y + 15.0F, d.textBright);
        }

        List<Module> mods = this.modulesOf(cat);
        float maxScroll = this.calculateMaxScroll(mods);
        float targetScroll = MathHelper.clamp(this.scroll.getOrDefault(cat, 0.0F), 0.0F, maxScroll);
        this.scroll.put(cat, targetScroll);
        float display = this.displayScroll.getOrDefault(cat, targetScroll);
        if (EditClickGUI.INSTANCE.isSmoothScroll()) {
            display = this.updateScroll(display, targetScroll, cat, false);
        } else {
            display = targetScroll;
        }
        this.displayScroll.put(cat, display);
        float offset = display;

        this.scissor(draw, x + 1.0F, y + this.headerHeight(), width - 2.0F, height - this.headerHeight() - 3.0F);
        float currentY = y + this.headerHeight() + 7.0F - offset;
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
            float totalHeight = this.design().rowHeight + settingsHeight;
            if (currentY + totalHeight >= y + this.headerHeight() && currentY <= y + height) {
                this.renderModule(draw, module, x + 7.0F, currentY, width - 14.0F, totalHeight, mouseX, mouseY, accent);
            }
            currentY += totalHeight + 4.0F;
        }
        draw.disableScissor();

        if (maxScroll > 0.0F) {
            float trackHeight = height - this.headerHeight() - 14.0F;
            float thumbHeight = Math.max(22.0F, trackHeight * trackHeight / (trackHeight + maxScroll));
            float thumbY = y + this.headerHeight() + 7.0F + (trackHeight - thumbHeight) * (offset / maxScroll);
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 5.0F, y + this.headerHeight() + 7.0F, 2.0F, trackHeight, BorderRadius.all(1.0F), new ColorRGBA(255, 255, 255, 22));
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 5.0F, thumbY, 2.0F, thumbHeight, BorderRadius.all(1.0F), accent.withAlpha(185));
        }
    }

    private void renderTabBar(CustomDrawContext draw, Category selected, float x, float y, float width, float mouseX, float mouseY, ColorRGBA accent) {
        DropdownDesign d = this.design();
        float tabW = width / (float) this.categories.size();
        for (int i = 0; i < this.categories.size(); i++) {
            Category cat = this.categories.get(i);
            float tx = x + (float) i * tabW;
            boolean active = cat == selected;
            boolean hovered = mouseX >= tx + 2.0F && mouseX <= tx + tabW - 2.0F && mouseY >= y + 2.0F && mouseY <= y + TAB_BAR_H - 4.0F;
            if (active) {
                DrawUtil.drawRoundedRect(draw.getMatrices(), tx + 2.0F, y + 2.0F, tabW - 4.0F, TAB_BAR_H - 6.0F, BorderRadius.all(6.0F), d.accent2.withAlpha(52));
                DrawUtil.drawRoundedBorder(draw.getMatrices(), tx + 2.0F, y + 2.0F, tabW - 4.0F, TAB_BAR_H - 6.0F, 1.0F, BorderRadius.all(6.0F), accent.withAlpha(160));
            } else if (hovered) {
                DrawUtil.drawRoundedRect(draw.getMatrices(), tx + 2.0F, y + 2.0F, tabW - 4.0F, TAB_BAR_H - 6.0F, BorderRadius.all(6.0F), new ColorRGBA(255, 255, 255, 9));
            }
            String icon = cat.getIcon();
            String name = cat.getName();
            float iconW = 10.0F;
            float total = iconW + 4.0F + this.textWidth(name, 7.5F);
            float start = tx + (tabW - total) / 2.0F;
            draw.drawText(d.headerFont.getFont(10.0F), icon, start, y + 9.0F, active ? accent : d.textDim);
            draw.drawText(d.moduleFont.getFont(7.5F), name, start + iconW + 4.0F, y + 10.0F, active ? d.textBright : d.textDim);
        }
    }

    private float calculateMaxScroll(List<Module> mods) {
        float total = 0.0F;
        for (Module module : mods) {
            if (!this.visible(module)) {
                continue;
            }
            total += this.design().rowHeight + 4.0F;
            if (this.isOpen(module) && this.hasVisibleSettings(module)) {
                total += this.visibleSettingsHeight(module);
            }
        }
        return Math.max(0.0F, total - (this.panelHeight() - this.headerHeight() - 10.0F));
    }

    private float scrollSmoothing() {
        float speed = EditClickGUI.INSTANCE.getScrollSpeed();
        float dt = Math.max(1.0F, this.getTickDelta());
        float k = 1.0F - (float) Math.pow(1.0D - Math.min(1.0D, (double) speed / 60.0D), (double) dt);
        return Math.max(0.001F, Math.min(1.0F, k));
    }

    private float updateScroll(float display, float target, Category cat, boolean edge) {
        float elasticity = EditClickGUI.INSTANCE.getScrollElasticity();
        if (elasticity <= 0.001F) {
            float k = this.scrollSmoothing();
            float next = display + (target - display) * k;
            if (Math.abs(target - next) < 0.05F) {
                next = target;
            }
            return next;
        }
        float dt = Math.max(1.0F, this.getTickDelta());
        this.displayScroll.put(cat, display);
        float vel = this.displayVelocity.getOrDefault(cat, 0.0F);
        float stiffness = 0.05F + elasticity / 100.0F * 0.35F;
        float damping = Math.max(0.55F, 0.92F - elasticity / 100.0F * 0.30F);
        vel += (target - display) * stiffness * dt;
        vel *= (float) Math.pow((double) damping, (double) dt);
        float next = display + vel * dt;
        this.displayVelocity.put(cat, vel);
        return next;
    }

    private float getTickDelta() {
        return MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
    }

    private void renderModule(CustomDrawContext draw, Module module, float x, float y, float width, float totalHeight, float mouseX, float mouseY, ColorRGBA accent) {
        if (this.isCards()) {
            this.renderModuleCard(draw, module, x, y, width, totalHeight, mouseX, mouseY, accent);
        } else {
            this.renderModuleSlim(draw, module, x, y, width, totalHeight, mouseX, mouseY, accent);
        }
    }

    private void renderModuleSlim(CustomDrawContext draw, Module module, float x, float y, float width, float totalHeight, float mouseX, float mouseY, ColorRGBA accent) {
        DropdownDesign d = this.design();
        float rowH = d.rowHeight;
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + rowH;
        float hp = this.hover.getOrDefault(module, 0.0F);
        hp += ((isHovered ? 1.0F : 0.0F) - hp) * 0.16F;
        this.hover.put(module, hp);
        ColorRGBA bg = module.isEnabled() ? d.moduleOn : new ColorRGBA(255, 255, 255, (int) (7.0F + hp * 9.0F));
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, rowH, BorderRadius.all(d.rowRadius), bg);
        if (module.isEnabled()) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, y + 7.0F, 2.0F, rowH - 14.0F, BorderRadius.all(1.0F), d.moduleBar);
        }
        draw.drawText(d.moduleFont.getFont(8.0F), module.getName(), x + 10.0F, y + 8.0F, module.isEnabled() ? d.textBright : d.textMain);

        if (this.bindingModule == module) {
            draw.drawText(this.uiFont().getFont(8.0F), "...", x + width - 34.0F, y + 8.0F, accent);
        } else if (module.getKeyCode() != -1) {
            draw.drawText(this.uiFont().getFont(8.0F), Keyboard.getKeyName(module.getKeyCode()), x + width - 34.0F, y + 8.0F, d.textDim);
        }

        if (this.hasVisibleSettings(module)) {
            float cxp = x + width - 12.0F;
            float cyp = y + rowH / 2.0F - 1.0F;
            ColorRGBA chevron = d.textDim;
            if (this.isOpen(module)) {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp + 1.5F), new Vec2f(cxp, cyp - 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp - 1.5F), new Vec2f(cxp + 3.0F, cyp + 1.5F), chevron);
            } else {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp - 1.5F), new Vec2f(cxp, cyp + 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp + 1.5F), new Vec2f(cxp + 3.0F, cyp - 1.5F), chevron);
            }
        }

        if (totalHeight > rowH) {
            this.scissor(draw, x, y + rowH, width, totalHeight - rowH);
            this.renderSettings(draw, module, x + d.contentX, y + rowH, width - 2.0F * d.contentX, mouseX, mouseY, accent);
            draw.disableScissor();
        }
    }

    private void renderModuleCard(CustomDrawContext draw, Module module, float x, float y, float width, float totalHeight, float mouseX, float mouseY, ColorRGBA accent) {
        DropdownDesign d = this.design();
        float rowH = d.rowHeight;
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + rowH;
        float hp = this.hover.getOrDefault(module, 0.0F);
        hp += ((isHovered ? 1.0F : 0.0F) - hp) * 0.14F;
        this.hover.put(module, hp);
        ColorRGBA bg = module.isEnabled() ? d.moduleOn.mix(new ColorRGBA(0, 0, 0, 0), 0.25F) : new ColorRGBA(255, 255, 255, (int) (4.0F + hp * 10.0F));
        DrawUtil.drawShadow(draw.getMatrices(), x, y, width, rowH, 14.0F, BorderRadius.all(d.rowRadius + 3.0F), d.cardShadow.withAlpha((int) (90.0F * (0.4F + hp * 0.6F))));
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, rowH, BorderRadius.all(d.rowRadius), bg);
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, width, rowH, d.borderWidth, BorderRadius.all(d.rowRadius), module.isEnabled() ? accent.withAlpha(130) : d.border.withAlpha(70));
        if (module.isEnabled()) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, y + 7.0F, 2.5F, rowH - 14.0F, BorderRadius.all(1.0F), d.moduleBar);
        }
        draw.drawText(d.moduleFont.getFont(9.0F), module.getName(), x + 12.0F, y + rowH / 2.0F - 5.0F, module.isEnabled() ? d.textBright : d.textMain);

        if (this.bindingModule == module) {
            draw.drawText(this.uiFont().getFont(8.0F), "...", x + width - 36.0F, y + rowH / 2.0F - 4.0F, accent);
        } else if (module.getKeyCode() != -1) {
            draw.drawText(this.uiFont().getFont(8.0F), Keyboard.getKeyName(module.getKeyCode()), x + width - 36.0F, y + rowH / 2.0F - 4.0F, d.textDim);
        }

        if (this.hasVisibleSettings(module)) {
            float cxp = x + width - 13.0F;
            float cyp = y + rowH / 2.0F - 1.0F;
            ColorRGBA chevron = d.textDim;
            if (this.isOpen(module)) {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp + 1.5F), new Vec2f(cxp, cyp - 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp - 1.5F), new Vec2f(cxp + 3.0F, cyp + 1.5F), chevron);
            } else {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp - 1.5F), new Vec2f(cxp, cyp + 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp + 1.5F), new Vec2f(cxp + 3.0F, cyp - 1.5F), chevron);
            }
        }

        if (totalHeight > rowH) {
            this.scissor(draw, x, y + rowH, width, totalHeight - rowH);
            this.renderSettings(draw, module, x + d.contentX, y + rowH, width - 2.0F * d.contentX, mouseX, mouseY, accent);
            draw.disableScissor();
        }
    }

    private void renderSettings(CustomDrawContext draw, Module module, float x, float y, float width, float mouseX, float mouseY, ColorRGBA accent) {
        DropdownDesign d = this.design();
        float currentY = y + 3.0F;
        for (Setting setting : module.getSettings()) {
            if (!setting.isVisible()) {
                continue;
            }
            if (setting instanceof BooleanSetting bool) {
                draw.drawText(this.uiFont().getFont(8.0F), bool.getName(), x, currentY + 2.0F, d.textMain);
                float toggle = this.toggleProgress.getOrDefault(setting, bool.isEnabled() ? 1.0F : 0.0F);
                toggle += ((bool.isEnabled() ? 1.0F : 0.0F) - toggle) * 0.22F;
                if (Math.abs((bool.isEnabled() ? 1.0F : 0.0F) - toggle) < 0.002F) {
                    toggle = bool.isEnabled() ? 1.0F : 0.0F;
                }
                this.toggleProgress.put(setting, toggle);
                float sw = 26.0F;
                float sh = 13.0F;
                ToggleSwitch.render(draw, x + width - sw - 6.0F, currentY + 1.0F, sw, sh, toggle, d.toggleOn, d.toggleOff);
                currentY += 17.0F + d.settingGap;
            } else if (setting instanceof KeySetting key) {
                draw.drawText(this.uiFont().getFont(8.0F), key.getName(), x, currentY + 2.0F, d.textMain);
                String keyName = key.getKeyCode() != -1 ? key.getNameKey() : "Нету";
                if (this.bindCapturing && this.bindSetting == key) {
                    keyName = "...";
                }
                float bx = x + width - Math.max(30.0F, this.textWidth(keyName, 8.0F) + 12.0F);
                DrawUtil.drawRoundedRect(draw.getMatrices(), bx, currentY + 1.0F, x + width - bx, 11.0F, BorderRadius.all(3.0F), accent.withAlpha(this.bindCapturing && this.bindSetting == key ? 180 : 110));
                draw.drawText(this.uiFont().getFont(8.0F), keyName, bx + 6.0F, currentY + 2.5F, d.textBright);
                currentY += 17.0F + d.settingGap;
            } else if (setting instanceof NumberSetting slider) {
                draw.drawText(this.uiFont().getFont(8.0F), slider.getName(), x, currentY, d.textMain);
                String value = String.format(Locale.US, "%.1f", slider.getCurrent());
                draw.drawText(this.uiFont().getFont(8.0F), value, x + width - this.textWidth(value, 8.0F), currentY, d.textDim);
                float trackY = currentY + 14.0F;
                float pct = (slider.getMax() - slider.getMin()) <= 0.0F ? 0.0F : (slider.getCurrent() - slider.getMin()) / (slider.getMax() - slider.getMin());
                float smooth = this.sliderProgress.getOrDefault(slider, pct);
                float k = EditClickGUI.INSTANCE.getSliderSmoothness();
                float kk = this.draggingSlider == slider ? Math.max(k, 0.45F) : k;
                smooth += (pct - smooth) * kk;
                if (Math.abs(pct - smooth) < 0.001F) {
                    smooth = pct;
                }
                this.sliderProgress.put(slider, smooth);
                DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, width, 3.0F, BorderRadius.all(2.0F), new ColorRGBA(255, 255, 255, 18));
                DrawUtil.drawRoundedRect(draw.getMatrices(), x, trackY, Math.max(3.0F, width * smooth), 3.0F, BorderRadius.all(2.0F), accent);
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width * smooth - 4.0F, trackY - 2.5F, 8.0F, 8.0F, BorderRadius.all(999.0F), d.textBright);
                currentY += 24.0F + d.settingGap;
            } else if (setting instanceof ModeSetting mode) {
                draw.drawText(this.uiFont().getFont(8.0F), mode.getName() + ": " + mode.get(), x, currentY, d.textMain);
                currentY += 15.0F;
                currentY = this.renderChoiceRow(draw, this.modeNames(mode), mode.get(), null, x, currentY, width, accent);
                currentY += d.settingGap;
            } else if (setting instanceof MultiBooleanSetting multi) {
                List<String> selected = multi.getSelectedNames();
                draw.drawText(this.uiFont().getFont(8.0F), multi.getName() + ": " + String.join(", ", selected), x, currentY, d.textMain);
                currentY += 15.0F;
                currentY = this.renderChoiceRow(draw, this.multiNames(multi), null, multi, x, currentY, width, accent);
                currentY += d.settingGap;
            } else if (setting instanceof ColorSetting color) {
                draw.drawText(this.uiFont().getFont(8.0F), color.getName(), x, currentY + 2.0F, d.textMain);
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 22.0F, currentY + 2.0F, 18.0F, 11.0F, BorderRadius.all(3.5F), color.getColor());
                if (this.colorPickerOpen(color)) {
                    this.renderColorPicker(draw, color, x, currentY + 17.0F, width, mouseX, mouseY);
                }
                currentY += this.settingHeight(color) + d.settingGap;
            } else if (setting instanceof StringSetting stringSetting) {
                draw.drawText(this.uiFont().getFont(8.0F), stringSetting.getName(), x, currentY + 2.0F, d.textMain);
                boolean editing = this.editingString == stringSetting;
                float tx = x + width - Math.max(30.0F, this.textWidth(stringSetting.getValue(), 8.0F) + 12.0F);
                DrawUtil.drawRoundedRect(draw.getMatrices(), tx, currentY + 1.0F, x + width - tx, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, editing ? 30 : 14));
                if (editing) {
                    DrawUtil.drawRoundedBorder(draw.getMatrices(), tx, currentY + 1.0F, x + width - tx, 11.0F, 1.0F, BorderRadius.all(3.0F), d.accent2.withAlpha(140));
                }
                String shown = stringSetting.getValue() + (editing && System.currentTimeMillis() % 900L < 450L ? "|" : "");
                draw.drawText(this.uiFont().getFont(8.0F), shown, tx + 5.0F, currentY + 2.5F, d.textBright);
                currentY += 17.0F + d.settingGap;
            } else if (setting instanceof ButtonSetting button) {
                DrawUtil.drawRoundedRect(draw.getMatrices(), x, currentY + 1.0F, width, 12.0F, BorderRadius.all(3.0F), accent.withAlpha(110));
                draw.drawText(this.uiFont().getFont(8.0F), button.getName(), x + width / 2.0F - this.textWidth(button.getName(), 8.0F) / 2.0F, currentY + 3.0F, d.textBright);
                currentY += 17.0F + d.settingGap;
            } else {
                currentY += 17.0F + d.settingGap;
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
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, pickerW, PICKER_H, BorderRadius.all(6.0F), ColorRGBA.WHITE, ColorRGBA.BLACK, ColorRGBA.BLACK, hue);

        float knobX = x + 4.0F + hsb[1] * (pickerW - 8.0F);
        float knobY = y + 4.0F + (1.0F - hsb[2]) * (PICKER_H - 8.0F);
        DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 4.0F, knobY - 4.0F, 8.0F, 8.0F, BorderRadius.all(999.0F), ColorRGBA.BLACK);
        DrawUtil.drawRoundedRect(draw.getMatrices(), knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F, BorderRadius.all(999.0F), ColorRGBA.WHITE);

        float sliderX = x + pickerW + 5.0F;
        DrawUtil.drawHueBar(draw.getMatrices(), sliderX, y, 4.0F, PICKER_H, 255.0F);
        float hueKnobY = y + hsb[0] * PICKER_H;
        DrawUtil.drawRoundedRect(draw.getMatrices(), sliderX - 2.5F, hueKnobY - 4.0F, 9.0F, 8.0F, BorderRadius.all(4.0F), ColorRGBA.BLACK);
        DrawUtil.drawRoundedRect(draw.getMatrices(), sliderX - 1.5F, hueKnobY - 3.0F, 7.0F, 6.0F, BorderRadius.all(3.0F), ColorRGBA.WHITE);
    }

    private void applyHsb(ColorSetting setting, float[] hsb) {
        setting.setColor(ColorRGBA.fromHSB(hsb[0], hsb[1], hsb[2]));
    }

    private float renderChoiceRow(CustomDrawContext draw, List<String> choices, String selected, MultiBooleanSetting multi, float x, float y, float width, ColorRGBA accent) {
        float rowY = y;
        float lineHeight = 14.0F;
        for (List<String> row : this.buildChoiceRows(choices, width)) {
            float currentX = x;
            for (String choice : row) {
                float choiceWidth = this.choiceWidth(choice);
                boolean active = multi != null ? multi.isEnable(choice) : choice.equals(selected);
                DrawUtil.drawRoundedRect(draw.getMatrices(), currentX, rowY, choiceWidth, lineHeight, BorderRadius.all(5.0F), active ? accent.withAlpha(190) : new ColorRGBA(255, 255, 255, 18));
                draw.drawText(this.uiFont().getFont(7.0F), choice, currentX + choiceWidth / 2.0F - this.textWidth(choice, 7.0F) / 2.0F, rowY + 3.0F, active ? ColorRGBA.WHITE : new ColorRGBA(190, 198, 204));
                currentX += choiceWidth + 3.0F;
            }
            rowY += lineHeight + 3.0F;
        }
        return rowY - 3.0F;
    }

    private boolean visible(Module module) {
        if (this.search.isEmpty()) {
            return true;
        }
        String query = this.search.toLowerCase(Locale.ROOT);
        return module.getName().toLowerCase(Locale.ROOT).contains(query);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.closing) {
            return true;
        }
        if (this.bindCapturing && this.bindSetting != null && button >= 0 && button <= 7) {
            this.bindSetting.setKeyCode(button);
            this.bindCapturing = false;
            this.bindSetting = null;
            return true;
        }
        if (this.bindingModule != null && button >= 0 && button <= 7) {
            this.bindingModule.setKeyCode(button);
            this.bindingModule = null;
            return true;
        }
        float mx = this.inverseX(mouseX);
        float my = this.inverseY(mouseY);
        float startX = this.startX();
        float startY = this.startY();
        if (this.themePicker.mouseClicked(mx, my, this.themeX(), this.themeY())) {
            this.searchFocused = false;
            this.editingString = null;
            this.bindCapturing = false;
            return true;
        }
        if (my >= startY - 34.0F && my <= startY - 10.0F && mx >= startX && mx <= startX + this.totalWidth()) {
            this.searchFocused = true;
            this.editingString = null;
            this.bindCapturing = false;
            return true;
        }
        for (int i = 0; i < this.panelCount(); i++) {
            float x = this.panelX(i);
            float w = this.panelWidthAt(i);
            float minY = this.isTabs() ? startY : startY + this.headerHeight();
            if (mx < x || mx > x + w || my < minY || my > startY + this.panelHeight()) {
                continue;
            }
            if (this.isTabs() && my < startY + TAB_BAR_H && button == 0) {
                int tabIndex = MathHelper.clamp((int) ((mx - x) / (w / (float) this.categories.size())), 0, this.categories.size() - 1);
                this.selectedCategory = this.categories.get(tabIndex);
                this.searchFocused = false;
                this.editingString = null;
                this.bindCapturing = false;
                return true;
            }
            for (Category cat : this.panelCategories(i)) {
                if (button == 0) {
                    if (this.handleSettingClick(cat, mx, my, x, startY)) {
                        return true;
                    }
                    Module module = this.getModuleAt(cat, mx, my, x, startY);
                    if (module != null) {
                        module.toggle();
                        return true;
                    }
                } else if (button == 1) {
                    Module module = this.getModuleAt(cat, mx, my, x, startY);
                    if (module != null && this.hasVisibleSettings(module)) {
                        boolean value = !this.isOpen(module);
                        this.expanded.put(module, value);
                        return true;
                    }
                } else if (button == 2) {
                    Module module = this.getModuleAt(cat, mx, my, x, startY);
                    if (module != null) {
                        this.bindingModule = this.bindingModule == module ? null : module;
                        this.bindCapturing = false;
                        this.bindSetting = null;
                        return true;
                    }
                }
            }
        }
        if (button == 0) {
            this.searchFocused = false;
            this.editingString = null;
            this.bindCapturing = false;
            this.bindingModule = null;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Module getModuleAt(Category cat, float mouseX, float mouseY, float x, float y) {
        float currentY = y + this.headerHeight() + 7.0F - this.displayScroll.getOrDefault(cat, this.scroll.getOrDefault(cat, 0.0F));
        for (Module module : this.modulesOf(cat)) {
            if (!this.visible(module)) {
                continue;
            }
            boolean open = this.isOpen(module) && this.hasVisibleSettings(module);
            float progress = this.expandProgress.getOrDefault(module, open ? 1.0F : 0.0F);
            float total = this.design().rowHeight + this.visibleSettingsHeight(module) * progress;
            if (mouseX >= x + 7.0F && mouseX <= x + this.panelWidthAt(0) - 7.0F && mouseY >= currentY && mouseY <= currentY + this.design().rowHeight) {
                return module;
            }
            currentY += total + 4.0F;
        }
        return null;
    }

    private boolean handleSettingClick(Category cat, float mouseX, float mouseY, float panelX, float panelY) {
        DropdownDesign d = this.design();
        float currentY = panelY + this.headerHeight() + 7.0F - this.displayScroll.getOrDefault(cat, this.scroll.getOrDefault(cat, 0.0F));
        for (Module module : this.modulesOf(cat)) {
            if (!this.visible(module)) {
                continue;
            }
            boolean open = this.isOpen(module) && this.hasVisibleSettings(module);
            float progress = this.expandProgress.getOrDefault(module, open ? 1.0F : 0.0F);
            float settingsHeight = this.visibleSettingsHeight(module) * progress;
            float rowH = this.design().rowHeight;
            if (open && settingsHeight > 0.0F && mouseY >= currentY + rowH && mouseY <= currentY + rowH + settingsHeight) {
                float settingY = currentY + rowH + 3.0F;
                float contentX = panelX + 7.0F + d.contentX;
                float contentWidth = this.contentWidth();
                for (Setting setting : module.getSettings()) {
                    if (!setting.isVisible()) {
                        continue;
                    }
                    float height = this.settingHeight(setting);
                    if (setting instanceof BooleanSetting bool) {
                        float switchX = contentX + contentWidth - 32.0F;
                        float switchY = settingY + 1.0F;
                        if (mouseX >= switchX && mouseX <= switchX + 26.0F && mouseY >= switchY && mouseY <= switchY + 13.0F) {
                            bool.setEnabled(!bool.isEnabled());
                            return true;
                        }
                        settingY += height + d.settingGap;
                        continue;
                    }
                    if (setting instanceof KeySetting key) {
                        float bx = contentX + contentWidth - Math.max(30.0F, this.textWidth(key.getKeyCode() != -1 ? key.getNameKey() : "Нету", 8.0F) + 12.0F);
                        if (mouseY >= settingY + 1.0F && mouseY <= settingY + 12.0F && mouseX >= bx && mouseX <= contentX + contentWidth) {
                            this.bindCapturing = true;
                            this.bindSetting = key;
                            this.editingString = null;
                            return true;
                        }
                    }
                    if (mouseY >= settingY && mouseY <= settingY + height) {
                        if (setting instanceof NumberSetting slider) {
                            if (mouseY >= settingY + 10.0F) {
                                this.draggingSlider = slider;
                                this.updateSlider(slider, mouseX, contentX, contentWidth);
                                return true;
                            }
                        }
                        if (setting instanceof ModeSetting mode) {
                            if (mouseY > settingY + 15.0F) {
                                String choice = this.choiceAt(mouseX, mouseY, contentX, settingY + 15.0F, contentWidth, this.modeNames(mode));
                                if (choice != null) {
                                    mode.set(choice);
                                }
                            }
                            return true;
                        }
                        if (setting instanceof MultiBooleanSetting multi) {
                            if (mouseY > settingY + 15.0F) {
                                String choice = this.choiceAt(mouseX, mouseY, contentX, settingY + 15.0F, contentWidth, this.multiNames(multi));
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
                            float swatchX = contentX + contentWidth - 22.0F;
                            if (mouseX >= swatchX && mouseX <= swatchX + 18.0F && mouseY >= settingY + 2.0F && mouseY <= settingY + 13.0F) {
                                if (this.hsb.containsKey(color)) {
                                    this.hsb.remove(color);
                                } else {
                                    this.hsbOf(color);
                                }
                                return true;
                            }
                            if (this.colorPickerOpen(color)) {
                                float pickerW = contentWidth * 0.76F;
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
                    settingY += height + d.settingGap;
                }
            }
            currentY += rowH + 4.0F + settingsHeight;
        }
        return false;
    }

    private String choiceAt(float mouseX, float mouseY, float x, float y, float width, List<String> choices) {
        float rowY = y;
        for (List<String> row : this.buildChoiceRows(choices, width)) {
            float currentX = x;
            for (String choice : row) {
                float itemWidth = this.choiceWidth(choice);
                if (mouseX >= currentX && mouseX <= currentX + itemWidth && mouseY >= rowY && mouseY <= rowY + 14.0F) {
                    return choice;
                }
                currentX += itemWidth + 3.0F;
            }
            rowY += 17.0F;
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
            for (int i = 0; i < this.panelCount(); i++) {
                float x = this.panelX(i);
                if (mx >= x && mx <= x + this.panelWidthAt(i)) {
                    this.updateSlider(this.draggingSlider, mx, x + 7.0F + this.design().contentX, this.contentWidth());
                    break;
                }
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
        float startY = this.startY();
        for (int i = 0; i < this.panelCount(); i++) {
            float x = this.panelX(i);
            if (mx >= x && mx <= x + this.panelWidthAt(i) && my >= startY && my <= startY + this.panelHeight()) {
                for (Category cat : this.panelCategories(i)) {
                    float max = this.calculateMaxScroll(this.modulesOf(cat));
                    float next = this.scroll.getOrDefault(cat, 0.0F) - (float) verticalAmount * 18.0F;
                    this.scroll.put(cat, MathHelper.clamp(next, 0.0F, max));
                }
                return true;
            }
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
        if (this.bindingModule != null) {
            if (keyCode == 261) {
                this.bindingModule.setKeyCode(-1);
            } else if (keyCode != 256) {
                this.bindingModule.setKeyCode(keyCode);
            }
            this.bindingModule = null;
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
        this.bindSetting = null;
        this.bindingModule = null;
        this.editingString = null;
    }
}