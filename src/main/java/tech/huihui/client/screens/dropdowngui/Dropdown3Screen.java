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
import tech.huihui.utility.render.display.Keyboard;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.ToggleSwitch;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class Dropdown3Screen extends Screen {
    private static final float HEADER_HEIGHT = 38;
    private static final float ROW_HEIGHT = 24;
    private static final float SETTING_GAP = 5;
    private static final float CONTENT_X = 17;
    private static final float PICKER_H = 54;

    private static final ColorRGBA GREEN_BG = new ColorRGBA(55, 214, 133, 42);
    private static final ColorRGBA GREEN_BAR = new ColorRGBA(74, 238, 151);
    private static final ColorRGBA GREEN_SWITCH = new ColorRGBA(52, 205, 132, 210);
    private static final ColorRGBA TEXT_MAIN = new ColorRGBA(194, 202, 211);
    private static final ColorRGBA TEXT_DIM = new ColorRGBA(150, 170, 166);

    private static float transformScale = 1.0F;
    private static int screenWidth;
    private static int screenHeight;

    private final List<Category> categories = List.of(Category.COMBAT, Category.MOVEMENT, Category.PLAYER, Category.RENDER, Category.COSMETICS, Category.MISC);
    private final EnumMap<Category, Float> scroll = new EnumMap<>(Category.class);
    private final Map<Category, Float> displayScroll = new HashMap<>();
    private final Map<Category, Float> displayVelocity = new HashMap<>();
    private final Map<Module, Boolean> expanded = new HashMap<>();
    private final Map<Module, Float> hover = new HashMap<>();
    private final Map<Module, Float> expandProgress = new HashMap<>();
    private final Map<Setting, Float> toggleProgress = new HashMap<>();
    private final Map<Setting, Float> sliderProgress = new HashMap<>();
    private final Map<ColorSetting, float[]> hsb = new HashMap<>();

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

    public Dropdown3Screen() {
        super(Text.literal("Dark Client ClickGUI"));
        for (Category cat : this.categories) {
            this.scroll.put(cat, 0.0F);
        }
    }

    public static Dropdown3Screen getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Dropdown3Screen INSTANCE = new Dropdown3Screen();
    }

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

    private Font font(float size) {
        return Fonts.REGULAR.getFont(size);
    }

    private float textWidth(String text, float size) {
        return Fonts.REGULAR.getWidth(text, size);
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
        return this.panelWidth() - 34.0F;
    }

    private ColorRGBA panelBg() {
        return EditClickGUI.INSTANCE.getBgColor().getColor().withAlpha(this.panelOpacity());
    }

    private float totalWidth() {
        return (float) this.categories.size() * (this.panelWidth() + this.panelGap()) - this.panelGap();
    }

    private float startX() {
        return ((float) screenWidth - this.totalWidth()) / 2.0F;
    }

    private float startY() {
        return Math.max(24.0F, ((float) screenHeight - this.panelHeight()) / 2.0F);
    }

    private float panelX(int index) {
        return this.startX() + (float) index * (this.panelWidth() + this.panelGap());
    }

    private float themeX() {
        return this.startX() + (this.totalWidth() - this.themePicker.width()) / 2.0F;
    }

    private float themeY() {
        return this.startY() - 34.0F - 8.0F - ThemePicker.BOX_HEIGHT;
    }

    private void renderTheme(CustomDrawContext draw, float mouseX, float mouseY) {
        Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
        this.themePicker.render(draw, theme, this.panelBg(), GREEN_BAR, TEXT_MAIN, 1.0F, this.themeX(), this.themeY(), mouseX, mouseY, new ThemePicker.Scissor() {
            @Override
            public void enable(float x, float y, float width, float height) {
                Dropdown3Screen.this.scissor(draw, x, y, width, height);
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

    private ColorRGBA accent(int index, int alpha) {
        return ColorRGBA.fromHSB((float) (index * 55 % 360) / 360.0F, 0.55F, 1.0F).withAlpha(alpha);
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
            h = 15.0F + this.choiceRowHeight(this.modeNames(mode)) + SETTING_GAP;
        } else if (setting instanceof MultiBooleanSetting multi) {
            h = 15.0F + this.choiceRowHeight(this.multiNames(multi)) + SETTING_GAP;
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
                result += this.settingHeight(setting) + SETTING_GAP;
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

        for (int i = 0; i < this.categories.size(); i++) {
            this.renderPanel(draw, this.categories.get(i), i, this.panelX(i), startY, lx, ly);
        }
        this.renderTheme(draw, lx, ly);
        context.getMatrices().pop();
    }

    private void drawBackground(CustomDrawContext draw) {
        draw.drawRect(0.0F, 0.0F, (float) this.width, (float) this.height, new ColorRGBA(5, 7, 12, 218));
        DrawUtil.drawRoundedRect(draw.getMatrices(), 0.0F, 0.0F, (float) this.width, (float) this.height, BorderRadius.ZERO,
                new ColorRGBA(20, 45, 70, 70), new ColorRGBA(5, 7, 12, 18), new ColorRGBA(7, 11, 20, 18), new ColorRGBA(5, 7, 12, 235));
    }

    private void renderSearch(CustomDrawContext draw, float x, float y, float mouseX, float mouseY) {
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, this.totalWidth(), 24.0F, BorderRadius.all(7.0F), this.panelBg());
        boolean active = this.searchFocused || !this.search.isEmpty();
        if (active) {
            DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, this.totalWidth(), 24.0F, 1.0F, BorderRadius.all(7.0F), GREEN_BAR.withAlpha(120));
        }
        if (this.search.isEmpty() && !this.searchFocused) {
            draw.drawText(Fonts.LUPA.getFont(6.0F), "\uf002", x + 8.0F, y + 8.0F, TEXT_DIM);
            draw.drawText(this.font(9.0F), "Поиск функций", x + 24.0F, y + 8.0F, new ColorRGBA(190, 200, 205));
        } else {
            String text = this.search + (this.searchFocused && System.currentTimeMillis() % 900L < 450L ? "|" : "");
            draw.drawText(this.font(9.0F), text, x + 10.0F, y + 8.0F, new ColorRGBA(210, 216, 222));
        }
    }

    private void renderPanel(CustomDrawContext draw, Category cat, int index, float x, float y, float mouseX, float mouseY) {
        ColorRGBA accent = this.accent(index, 255);
        float width = this.panelWidth();
        float height = this.panelHeight();
        float radius = this.panelRadius();
        ColorRGBA bg = this.panelBg();
        if (EditClickGUI.INSTANCE.getBlur().isEnabled()) {
            DrawUtil.drawBlur(draw.getMatrices(), x, y, width, height, 11.0F, BorderRadius.all(radius), bg);
        }
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, height, BorderRadius.all(radius), bg);
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(radius), EditClickGUI.INSTANCE.getBorderColor().getColor().withAlpha(80));
        draw.drawText(Fonts.ICONS.getFont(11.0F), cat.getIcon(), x + 14.0F, y + 14.0F, accent);
        draw.drawText(Fonts.BOLD.getFont(9.0F), cat.getName(), x + 39.0F, y + 15.0F, ColorRGBA.WHITE);

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

        this.scissor(draw, x + 1.0F, y + HEADER_HEIGHT, width - 2.0F, height - HEADER_HEIGHT - 3.0F);
        float currentY = y + HEADER_HEIGHT + 7.0F - offset;
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
            float totalHeight = ROW_HEIGHT + settingsHeight;
            if (currentY + totalHeight >= y + HEADER_HEIGHT && currentY <= y + height) {
                this.renderModule(draw, module, x + 7.0F, currentY, width - 14.0F, totalHeight, mouseX, mouseY, accent);
            }
            currentY += totalHeight + 4.0F;
        }
        draw.disableScissor();

        if (maxScroll > 0.0F) {
            float trackHeight = height - HEADER_HEIGHT - 14.0F;
            float thumbHeight = Math.max(22.0F, trackHeight * trackHeight / (trackHeight + maxScroll));
            float thumbY = y + HEADER_HEIGHT + 7.0F + (trackHeight - thumbHeight) * (offset / maxScroll);
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 5.0F, y + HEADER_HEIGHT + 7.0F, 2.0F, trackHeight, BorderRadius.all(1.0F), new ColorRGBA(255, 255, 255, 22));
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 5.0F, thumbY, 2.0F, thumbHeight, BorderRadius.all(1.0F), new ColorRGBA(120, 255, 178, 185));
        }
    }

    private float calculateMaxScroll(List<Module> mods) {
        float total = 0.0F;
        for (Module module : mods) {
            if (!this.visible(module)) {
                continue;
            }
            total += ROW_HEIGHT + 4.0F;
            if (this.isOpen(module) && this.hasVisibleSettings(module)) {
                total += this.visibleSettingsHeight(module);
            }
        }
        return Math.max(0.0F, total - (this.panelHeight() - HEADER_HEIGHT - 10.0F));
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
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + ROW_HEIGHT;
        float hp = this.hover.getOrDefault(module, 0.0F);
        hp += ((isHovered ? 1.0F : 0.0F) - hp) * 0.16F;
        this.hover.put(module, hp);
        ColorRGBA bg = module.isEnabled() ? GREEN_BG : new ColorRGBA(255, 255, 255, (int) (7.0F + hp * 9.0F));
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, width, ROW_HEIGHT, BorderRadius.all(7.0F), bg);
        if (module.isEnabled()) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), x + 3.0F, y + 7.0F, 2.0F, ROW_HEIGHT - 14.0F, BorderRadius.all(1.0F), GREEN_BAR);
        }
        draw.drawText(Fonts.BOLD.getFont(8.0F), module.getName(), x + 10.0F, y + 8.0F, module.isEnabled() ? ColorRGBA.WHITE : new ColorRGBA(208, 214, 222));

        if (this.bindingModule == module) {
            draw.drawText(this.font(8.0F), "...", x + width - 34.0F, y + 8.0F, accent);
        } else if (module.getKeyCode() != -1) {
            draw.drawText(this.font(8.0F), Keyboard.getKeyName(module.getKeyCode()), x + width - 34.0F, y + 8.0F, new ColorRGBA(120, 130, 140));
        }

        if (this.hasVisibleSettings(module)) {
            float cxp = x + width - 12.0F;
            float cyp = y + ROW_HEIGHT / 2.0F - 1.0F;
            ColorRGBA chevron = new ColorRGBA(160, 170, 180);
            if (this.isOpen(module)) {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp + 1.5F), new Vec2f(cxp, cyp - 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp - 1.5F), new Vec2f(cxp + 3.0F, cyp + 1.5F), chevron);
            } else {
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp - 3.0F, cyp - 1.5F), new Vec2f(cxp, cyp + 1.5F), chevron);
                DrawUtil.drawLine(draw.getMatrices(), new Vec2f(cxp, cyp + 1.5F), new Vec2f(cxp + 3.0F, cyp - 1.5F), chevron);
            }
        }

        if (totalHeight > ROW_HEIGHT) {
            this.scissor(draw, x, y + ROW_HEIGHT, width, totalHeight - ROW_HEIGHT);
            this.renderSettings(draw, module, x + 10.0F, y + ROW_HEIGHT, width - 20.0F, mouseX, mouseY, accent);
            draw.disableScissor();
        }
    }

    private void renderSettings(CustomDrawContext draw, Module module, float x, float y, float width, float mouseX, float mouseY, ColorRGBA accent) {
        float currentY = y + 3.0F;
        for (Setting setting : module.getSettings()) {
            if (!setting.isVisible()) {
                continue;
            }
            if (setting instanceof BooleanSetting bool) {
                draw.drawText(this.font(8.0F), bool.getName(), x, currentY + 2.0F, TEXT_MAIN);
                float toggle = this.toggleProgress.getOrDefault(setting, bool.isEnabled() ? 1.0F : 0.0F);
                toggle += ((bool.isEnabled() ? 1.0F : 0.0F) - toggle) * 0.22F;
                if (Math.abs((bool.isEnabled() ? 1.0F : 0.0F) - toggle) < 0.002F) {
                    toggle = bool.isEnabled() ? 1.0F : 0.0F;
                }
                this.toggleProgress.put(setting, toggle);
                float sw = 26.0F;
                float sh = 13.0F;
                ToggleSwitch.render(draw, x + width - sw - 6.0F, currentY + 1.0F, sw, sh, toggle, accent, new ColorRGBA(255, 255, 255, 22));
                currentY += 17.0F + SETTING_GAP;
            } else if (setting instanceof KeySetting key) {
                draw.drawText(this.font(8.0F), key.getName(), x, currentY + 2.0F, TEXT_MAIN);
                String keyName = key.getKeyCode() != -1 ? key.getNameKey() : "Нету";
                if (this.bindCapturing && this.bindSetting == key) {
                    keyName = "...";
                }
                float bx = x + width - Math.max(30.0F, this.textWidth(keyName, 8.0F) + 12.0F);
                DrawUtil.drawRoundedRect(draw.getMatrices(), bx, currentY + 1.0F, x + width - bx, 11.0F, BorderRadius.all(3.0F), accent.withAlpha(this.bindCapturing && this.bindSetting == key ? 180 : 110));
                draw.drawText(this.font(8.0F), keyName, bx + 6.0F, currentY + 2.5F, ColorRGBA.WHITE);
                currentY += 17.0F + SETTING_GAP;
            } else if (setting instanceof NumberSetting slider) {
                draw.drawText(this.font(8.0F), slider.getName(), x, currentY, TEXT_MAIN);
                String value = String.format(Locale.US, "%.1f", slider.getCurrent());
                draw.drawText(this.font(8.0F), value, x + width - this.textWidth(value, 8.0F), currentY, TEXT_DIM);
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
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width * smooth - 4.0F, trackY - 2.5F, 8.0F, 8.0F, BorderRadius.all(999.0F), ColorRGBA.WHITE);
                currentY += 24.0F + SETTING_GAP;
            } else if (setting instanceof ModeSetting mode) {
                draw.drawText(this.font(8.0F), mode.getName() + ": " + mode.get(), x, currentY, TEXT_MAIN);
                currentY += 15.0F;
                currentY = this.renderChoiceRow(draw, this.modeNames(mode), mode.get(), null, x, currentY, width, accent);
                currentY += SETTING_GAP;
            } else if (setting instanceof MultiBooleanSetting multi) {
                List<String> selected = multi.getSelectedNames();
                draw.drawText(this.font(8.0F), multi.getName() + ": " + String.join(", ", selected), x, currentY, TEXT_MAIN);
                currentY += 15.0F;
                currentY = this.renderChoiceRow(draw, this.multiNames(multi), null, multi, x, currentY, width, accent);
                currentY += SETTING_GAP;
            } else if (setting instanceof ColorSetting color) {
                draw.drawText(this.font(8.0F), color.getName(), x, currentY + 2.0F, TEXT_MAIN);
                DrawUtil.drawRoundedRect(draw.getMatrices(), x + width - 22.0F, currentY + 2.0F, 18.0F, 11.0F, BorderRadius.all(3.5F), color.getColor());
                if (this.colorPickerOpen(color)) {
                    this.renderColorPicker(draw, color, x, currentY + 17.0F, width, mouseX, mouseY);
                }
                currentY += this.settingHeight(color) + SETTING_GAP;
            } else if (setting instanceof StringSetting stringSetting) {
                draw.drawText(this.font(8.0F), stringSetting.getName(), x, currentY + 2.0F, TEXT_MAIN);
                boolean editing = this.editingString == stringSetting;
                float tx = x + width - Math.max(30.0F, this.textWidth(stringSetting.getValue(), 8.0F) + 12.0F);
                DrawUtil.drawRoundedRect(draw.getMatrices(), tx, currentY + 1.0F, x + width - tx, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, editing ? 30 : 14));
                if (editing) {
                    DrawUtil.drawRoundedBorder(draw.getMatrices(), tx, currentY + 1.0F, x + width - tx, 11.0F, 1.0F, BorderRadius.all(3.0F), GREEN_BAR.withAlpha(140));
                }
                String shown = stringSetting.getValue() + (editing && System.currentTimeMillis() % 900L < 450L ? "|" : "");
                draw.drawText(this.font(8.0F), shown, tx + 5.0F, currentY + 2.5F, new ColorRGBA(222, 226, 232));
                currentY += 17.0F + SETTING_GAP;
            } else if (setting instanceof ButtonSetting button) {
                DrawUtil.drawRoundedRect(draw.getMatrices(), x, currentY + 1.0F, width, 12.0F, BorderRadius.all(3.0F), accent.withAlpha(110));
                draw.drawText(this.font(8.0F), button.getName(), x + width / 2.0F - this.textWidth(button.getName(), 8.0F) / 2.0F, currentY + 3.0F, ColorRGBA.WHITE);
                currentY += 17.0F + SETTING_GAP;
            } else {
                currentY += 17.0F + SETTING_GAP;
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
                draw.drawText(this.font(7.0F), choice, currentX + choiceWidth / 2.0F - this.textWidth(choice, 7.0F) / 2.0F, rowY + 3.0F, active ? ColorRGBA.WHITE : new ColorRGBA(190, 198, 204));
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
        for (int i = 0; i < this.categories.size(); i++) {
            float x = this.panelX(i);
            if (mx < x || mx > x + this.panelWidth() || my < startY + HEADER_HEIGHT || my > startY + this.panelHeight()) {
                continue;
            }
            if (button == 0) {
                if (this.handleSettingClick(this.categories.get(i), mx, my, x, startY)) {
                    return true;
                }
                Module module = this.getModuleAt(this.categories.get(i), mx, my, x, startY);
                if (module != null) {
                    module.toggle();
                    return true;
                }
            } else if (button == 1) {
                Module module = this.getModuleAt(this.categories.get(i), mx, my, x, startY);
                if (module != null && this.hasVisibleSettings(module)) {
                    boolean value = !this.isOpen(module);
                    this.expanded.put(module, value);
                    return true;
                }
            } else if (button == 2) {
                Module module = this.getModuleAt(this.categories.get(i), mx, my, x, startY);
                if (module != null) {
                    this.bindingModule = this.bindingModule == module ? null : module;
                    this.bindCapturing = false;
                    this.bindSetting = null;
                    return true;
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
        float currentY = y + HEADER_HEIGHT + 7.0F - this.displayScroll.getOrDefault(cat, this.scroll.getOrDefault(cat, 0.0F));
        for (Module module : this.modulesOf(cat)) {
            if (!this.visible(module)) {
                continue;
            }
            boolean open = this.isOpen(module) && this.hasVisibleSettings(module);
            float progress = this.expandProgress.getOrDefault(module, open ? 1.0F : 0.0F);
            float total = ROW_HEIGHT + this.visibleSettingsHeight(module) * progress;
            if (mouseX >= x + 7.0F && mouseX <= x + this.panelWidth() - 7.0F && mouseY >= currentY && mouseY <= currentY + ROW_HEIGHT) {
                return module;
            }
            currentY += total + 4.0F;
        }
        return null;
    }

    private boolean handleSettingClick(Category cat, float mouseX, float mouseY, float panelX, float panelY) {
        float currentY = panelY + HEADER_HEIGHT + 7.0F - this.displayScroll.getOrDefault(cat, this.scroll.getOrDefault(cat, 0.0F));
        for (Module module : this.modulesOf(cat)) {
            if (!this.visible(module)) {
                continue;
            }
            boolean open = this.isOpen(module) && this.hasVisibleSettings(module);
            float progress = this.expandProgress.getOrDefault(module, open ? 1.0F : 0.0F);
            float settingsHeight = this.visibleSettingsHeight(module) * progress;
            if (open && settingsHeight > 0.0F && mouseY >= currentY + ROW_HEIGHT && mouseY <= currentY + ROW_HEIGHT + settingsHeight) {
                float settingY = currentY + ROW_HEIGHT + 3.0F;
                float contentX = panelX + CONTENT_X;
                for (Setting setting : module.getSettings()) {
                    if (!setting.isVisible()) {
                        continue;
                    }
                    float height = this.settingHeight(setting);
                    if (setting instanceof BooleanSetting bool) {
                        float switchX = panelX + this.panelWidth() - 42.0F;
                        float switchY = settingY + 1.0F;
                        if (mouseX >= switchX && mouseX <= switchX + 26.0F && mouseY >= switchY && mouseY <= switchY + 13.0F) {
                            bool.setEnabled(!bool.isEnabled());
                            return true;
                        }
                        settingY += height + SETTING_GAP;
                        continue;
                    }
                    if (setting instanceof KeySetting key) {
                        float bx = contentX + this.contentWidth() - Math.max(30.0F, this.textWidth(key.getKeyCode() != -1 ? key.getNameKey() : "Нету", 8.0F) + 12.0F);
                        if (mouseY >= settingY + 1.0F && mouseY <= settingY + 12.0F && mouseX >= bx && mouseX <= contentX + this.contentWidth()) {
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
                                this.updateSlider(slider, mouseX, contentX, this.contentWidth());
                                return true;
                            }
                        }
                        if (setting instanceof ModeSetting mode) {
                            if (mouseY > settingY + 15.0F) {
                                String choice = this.choiceAt(mouseX, mouseY, contentX, settingY + 15.0F, this.contentWidth(), this.modeNames(mode));
                                if (choice != null) {
                                    mode.set(choice);
                                }
                            }
                            return true;
                        }
                        if (setting instanceof MultiBooleanSetting multi) {
                            if (mouseY > settingY + 15.0F) {
                                String choice = this.choiceAt(mouseX, mouseY, contentX, settingY + 15.0F, this.contentWidth(), this.multiNames(multi));
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
                            float swatchX = contentX + this.contentWidth() - 22.0F;
                            if (mouseX >= swatchX && mouseX <= swatchX + 18.0F && mouseY >= settingY + 2.0F && mouseY <= settingY + 13.0F) {
                                if (this.hsb.containsKey(color)) {
                                    this.hsb.remove(color);
                                } else {
                                    this.hsbOf(color);
                                }
                                return true;
                            }
                            if (this.colorPickerOpen(color)) {
                                float pickerW = this.contentWidth() * 0.76F;
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
                    settingY += height + SETTING_GAP;
                }
            }
            currentY += ROW_HEIGHT + 4.0F + settingsHeight;
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
            for (int i = 0; i < this.categories.size(); i++) {
                float x = this.panelX(i);
                if (mx >= x && mx <= x + this.panelWidth()) {
                    this.updateSlider(this.draggingSlider, mx, x + CONTENT_X, this.contentWidth());
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
        for (int i = 0; i < this.categories.size(); i++) {
            float x = this.panelX(i);
            if (mx >= x && mx <= x + this.panelWidth() && my >= startY && my <= startY + this.panelHeight()) {
                Category cat = this.categories.get(i);
                float max = this.calculateMaxScroll(this.modulesOf(cat));
                float next = this.scroll.getOrDefault(cat, 0.0F) - (float) verticalAmount * 18.0F;
                this.scroll.put(cat, MathHelper.clamp(next, 0.0F, max));
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
