package org.blade.menu.pages.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.visual.EditClickGUI;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.InputBindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.MultiSelectSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.setting.Setting;
import org.blade.feature.setting.TextSetting;
import org.blade.menu.core.MenuOverlayState;
import org.blade.menu.core.MenuPage;
import org.blade.menu.ui.Component;
import org.blade.utils.ColorUtil;
import org.blade.utils.math.MathUtil;
import org.blade.utils.render.Textures;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DropdownModulePage extends Component {
    private static final float SEARCH_Y = 62.0F;
    private static final float SEARCH_HEIGHT = 30.0F;
    private static final float SEARCH_RADIUS = 9.0F;
    private static final float PANEL_TOP = 102.0F;
    private static final float HEADER_HEIGHT = 32.0F;
    private static final float TAB_BAR_HEIGHT = 34.0F;
    private static final float ROW_HEIGHT = 24.0F;
    private static final float ROW_GAP = 4.0F;
    private static final float SETTING_GAP = 5.0F;
    private static final float CONTENT_X = 14.0F;
    private static final float PICKER_H = 50.0F;
    private static final float BOTTOM_MARGIN = 18.0F;
    private static final float CHIP_HEIGHT = 14.0F;
    private static final float CHIP_GAP = 3.0F;
    private static final int SEARCH_MAX_LEN = 40;

    private static final Identifier[] CATEGORY_ICONS = {
            Textures.Icons.SWORDS,
            Textures.Icons.PERSON_STANDING,
            Textures.Icons.EYE,
            Textures.Icons.USER_ROUND,
            Textures.Icons.BOXES,
            Textures.Icons.BRAIN
    };

    private record ModuleRow(Feature feature, float y, float rowHeight, float settingsHeight) {
    }

    private final StringBuilder searchQuery = new StringBuilder();
    private final Map<FeatureCategory, Float> scrollTarget = new EnumMap<>(FeatureCategory.class);
    private final Map<FeatureCategory, Float> displayScroll = new EnumMap<>(FeatureCategory.class);
    private final Map<Feature, Boolean> expanded = new IdentityHashMap<>();
    private final Map<Feature, Float> expandProgress = new IdentityHashMap<>();
    private final Map<Feature, Float> hoverProgress = new IdentityHashMap<>();
    private final Map<Setting<?>, Float> toggleProgress = new IdentityHashMap<>();
    private final Map<Setting<?>, Float> sliderProgress = new IdentityHashMap<>();
    private final Map<ColorSetting, float[]> pickers = new IdentityHashMap<>();

    private MenuOverlayState state;
    private MenuPage displayedPage = MenuPage.NONE;
    private int mouseX;
    private int mouseY;
    private float transitionProgress;
    private float frameDelta = 1.0F;
    private long lastFrameMs;
    private boolean searchFocused;
    private NumberSetting draggingSlider;
    private float dragSliderContentX;
    private float dragSliderWidth;
    private ColorSetting draggingPicker;
    private boolean draggingHue;
    private InputBindSetting capturingBind;
    private Feature bindingModule;
    private TextSetting editingText;
    private final StringBuilder textBuffer = new StringBuilder();
    private FeatureCategory selectedCategory = FeatureCategory.COMBAT;

    public void layout(Component frame, MenuOverlayState state, int mouseX, int mouseY) {
        attach(frame, frame.x(), frame.y(), frame.width(), frame.height());
        this.state = state;
        this.displayedPage = state.displayPage();
        this.transitionProgress = state.contentProgress();
        if (state.page() != MenuPage.SEARCH && !isSearchFocused()) {
            this.searchFocused = false;
        }
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public boolean handleMouseButton(int mouseX, int mouseY, int button) {
        float dx = designX(mouseX);
        float dy = designY(mouseY);

        if (this.capturingBind != null && button >= 0 && button <= 7) {
            this.capturingBind.setMouse(button);
            this.capturingBind = null;
            return true;
        }
        if (this.bindingModule != null && button >= 0 && button <= 7) {
            this.bindingModule.setMouseBind(button);
            this.bindingModule = null;
            return true;
        }

        if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (hoveringSearch(dx, dy)) {
                this.searchFocused = true;
                this.editingText = null;
                this.capturingBind = null;
                return true;
            }
        }

        if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT || button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            int panelIndex = panelAt(dx, dy);
            if (panelIndex >= 0) {
                FeatureCategory category = categoryOf(panelIndex);
                float panelX = panelX(panelIndex);
                float panelW = panelWidth();
                if (isTabs()) {
                    if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT && dy >= PANEL_TOP && dy <= PANEL_TOP + TAB_BAR_HEIGHT) {
                        int tab = MathUtil.clamp((int) ((dx - panelX) / (panelW / (float) categoryCount())), 0, categoryCount() - 1);
                        this.selectedCategory = categoryAt(tab);
                        this.scrollTarget.put(this.selectedCategory, 0.0F);
                        this.searchFocused = false;
                        this.editingText = null;
                        this.capturingBind = null;
                        return true;
                    }
                } else if (dy < PANEL_TOP + HEADER_HEIGHT) {
                    return true;
                }

                if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
                        && handleSettingClick(category, panelX, panelW, dx, dy)) {
                    return true;
                }

                Feature feature = featureAt(category, panelX, panelW, dx, dy);
                if (feature != null) {
                    if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        feature.toggle();
                    } else if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        if (hasVisibleSettings(feature)) {
                            this.expanded.put(feature, !isExpanded(feature));
                        }
                    } else if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        this.bindingModule = this.bindingModule == feature ? null : feature;
                    }
                    return true;
                }
            }
        }

        if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (this.editingText != null) {
                commitText();
            }
            this.searchFocused = false;
            this.capturingBind = null;
            if (this.displayedPage == MenuPage.SEARCH && this.state != null) {
                this.state.openPage(MenuPage.NONE);
            }
        }
        return false;
    }

    public void drag(int mouseX, int mouseY) {
        if (this.draggingSlider != null) {
            float dx = designX(mouseX);
            updateSliderValue(this.draggingSlider, dx, this.dragSliderContentX, this.dragSliderWidth);
        }
        if (this.draggingPicker != null) {
            updatePicker(this.draggingPicker, designX(mouseX), designY(mouseY));
        }
    }

    public void releasePointer() {
        this.draggingSlider = null;
        this.draggingPicker = null;
        this.draggingHue = false;
    }

    public boolean handleKey(int key) {
        if (this.displayedPage != MenuPage.NONE && this.displayedPage != MenuPage.SEARCH) {
            return false;
        }
        if (this.bindingModule != null) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                this.bindingModule.clearBind();
            } else if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                this.bindingModule.setKeyBind(key);
            }
            this.bindingModule = null;
            return true;
        }
        if (this.capturingBind != null) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
                this.capturingBind.clear();
            } else if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                this.capturingBind.setKey(key);
            }
            this.capturingBind = null;
            return true;
        }
        if (this.editingText != null) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                commitText();
            } else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !this.textBuffer.isEmpty()) {
                this.textBuffer.deleteCharAt(this.textBuffer.length() - 1);
            }
            return true;
        }
        if (this.displayedPage == MenuPage.NONE && isSearchFocused()
                && key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            clearSearch();
            return true;
        }
        return false;
    }

    public boolean handleCharacter(int codePoint) {
        if (this.editingText != null) {
            if (this.textBuffer.length() < this.editingText.getMaxLength() && !Character.isISOControl(codePoint)) {
                this.textBuffer.appendCodePoint(codePoint);
            }
            return true;
        }
        return false;
    }

    public boolean isCapturingBind() {
        return this.bindingModule != null || this.capturingBind != null;
    }

    public boolean isSearchOpen() {
        return this.displayedPage == MenuPage.SEARCH || !this.searchQuery.isEmpty() || this.searchFocused;
    }

    public boolean isSearchFocused() {
        return this.displayedPage == MenuPage.SEARCH || this.searchFocused;
    }

    public void focusSearch() {
        this.searchFocused = true;
        this.editingText = null;
        this.capturingBind = null;
    }

    public void clearSearch() {
        this.searchQuery.setLength(0);
        this.searchFocused = false;
    }

    public void appendCodePoint(int codePoint) {
        if (!isSearchFocused() || Character.isISOControl(codePoint)) {
            return;
        }
        if (this.searchQuery.codePointCount(0, this.searchQuery.length()) >= SEARCH_MAX_LEN) {
            return;
        }
        this.searchQuery.appendCodePoint(codePoint);
    }

    public void backspace() {
        if (!isSearchFocused() || this.searchQuery.isEmpty()) {
            return;
        }
        int lastCodePoint = this.searchQuery.codePointBefore(this.searchQuery.length());
        this.searchQuery.delete(this.searchQuery.length() - Character.charCount(lastCodePoint), this.searchQuery.length());
    }

    public void handleScroll(int mouseX, int mouseY, double vertical) {
        if (this.displayedPage != MenuPage.NONE && this.displayedPage != MenuPage.SEARCH) {
            return;
        }
        float dx = designX(mouseX);
        float dy = designY(mouseY);
        int panelIndex = panelAt(dx, dy);
        if (panelIndex < 0) {
            return;
        }
        FeatureCategory category = categoryOf(panelIndex);
        float panelW = panelWidth();
        float maxScroll = maxScroll(category, panelW);
        float target = clampScroll(category, vertical);
        this.scrollTarget.put(category, MathUtil.clamp(target, 0.0F, maxScroll));
    }

    private float clampScroll(FeatureCategory category, double vertical) {
        return this.scrollTarget.getOrDefault(category, 0.0F) - (float) vertical * 18.0F;
    }

    @Override
    public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
        if (this.displayedPage != MenuPage.NONE && this.displayedPage != MenuPage.SEARCH) {
            return;
        }
        updateFrameDelta();

        if (this.displayedPage == MenuPage.SEARCH && this.transitionProgress > 0.001F) {
            Render2DUtil.rect(x(), y() + px(Theme.Sizes.HEADER_HEIGHT), width(), height() - px(Theme.Sizes.HEADER_HEIGHT))
                    .color(ColorUtil.withAlpha(Theme.Colors.OVERLAY, Math.round(218.0F * this.transitionProgress)))
                    .radius(0.0F, 0.0F, px(12), px(12))
                    .blur(px(14.0F * this.transitionProgress))
                    .draw();
        }

        if (EditClickGUI.INSTANCE.showSearch.getValue()) {
            renderSearch(guiGraphicsExtractor);
        }
        for (int index = 0; index < panelCount(); index++) {
            renderPanel(guiGraphicsExtractor, index, categoryOf(index));
        }
    }

    private void renderSearch(GuiGraphicsExtractor extractor) {
        float x = startX();
        float y = SEARCH_Y;
        float w = totalWidth();
        boolean active = isSearchFocused() || !this.searchQuery.isEmpty();
        int opacity = panelOpacity();
        rect(x, y, w, SEARCH_HEIGHT, ColorUtil.withAlpha(Theme.Colors.CARD, opacity), SEARCH_RADIUS);
        if (active) {
            outline(x, y, w, SEARCH_HEIGHT, ColorUtil.withAlpha(Theme.getAccent(), 120), SEARCH_RADIUS, 0.5F, 1.0F);
        }

        int iconColor = ColorUtil.withAlpha(Theme.Colors.SECONDARY, Math.round(255.0F * panelOpacity() / 255.0F));
        float iconSize = 10.0F;
        float iconCenterX = x + 8.0F + iconSize / 2.0F;
        float iconCenterY = y + SEARCH_HEIGHT / 2.0F;
        rotatedSegment(extractor, iconCenterX - iconSize * 0.35F, iconCenterY + iconSize * 0.15F,
                iconCenterX + 4.0F, iconCenterY + 6.0F, 2.0F, iconColor);
        rotatedSegment(extractor, iconCenterX + 4.0F, iconCenterY + 6.0F,
                iconCenterX + iconSize * 0.45F, iconCenterY + iconSize * 0.85F, 2.0F, iconColor);
        rect(iconCenterX - iconSize * 0.5F, iconCenterY - iconSize * 0.5F, iconSize, iconSize, iconColor, iconSize / 2.0F);

        float textX = x + 8.0F + iconSize + 2.0F;
        if (this.searchQuery.isEmpty() && !active) {
            text(textX, centeredTextY(y + SEARCH_HEIGHT / 2.0F, 10.0F), 10.0F,
                    "Search modules...", Theme.Colors.SECONDARY_DARK, UiFontStyle.REGULAR);
        } else {
            String value = this.searchQuery.toString();
            if (active && System.currentTimeMillis() % 900L < 450L) {
                value += "|";
            }
            text(textX, centeredTextY(y + SEARCH_HEIGHT / 2.0F, 10.0F), 10.0F,
                    value, Theme.Colors.TEXT, UiFontStyle.REGULAR);
        }
    }

    private int panelOpacity() {
        return Math.round(EditClickGUI.INSTANCE.panelOpacity.getValue().floatValue() * progressFactor());
    }

    private float progressFactor() {
        return this.state == null ? 1.0F : this.state.openProgress();
    }

    private void renderPanel(GuiGraphicsExtractor extractor, int index, FeatureCategory category) {
        float x = panelX(index);
        float w = panelWidth();
        float h = panelHeight();
        float y = PANEL_TOP;
        int opacity = panelOpacity();
        float radius = panelRadius();

        if (EditClickGUI.INSTANCE.blur.getValue()) {
            rect(x, y, w, h, ColorUtil.withAlpha(Theme.Colors.CARD, opacity), radius);
            Render2DUtil.rect(sx(x), sy(y), px(w), px(h))
                    .color(ColorUtil.withAlpha(Theme.Colors.CARD, opacity))
                    .radius(px(radius))
                    .blur(px(8.0F))
                    .draw();
        } else {
            rect(x, y, w, h, ColorUtil.withAlpha(Theme.Colors.CARD, opacity), radius);
        }
        outline(x, y, w, h, Theme.Colors.OUTLINES_MEDIUM, radius, 0.5F, 1.0F);

        if (isTabs()) {
            renderTabBar(extractor, x, y, w, opacity);
        } else {
            renderPanelHeader(extractor, x, y, w, category, opacity);
        }

        float headerH = isTabs() ? TAB_BAR_HEIGHT : HEADER_HEIGHT;
        List<Feature> features = visibleFeatures(category);
        float maxScroll = maxScroll(category, w);
        float target = MathUtil.clamp(this.scrollTarget.getOrDefault(category, 0.0F), 0.0F, maxScroll);
        float offset = smoothScroll(category, target);

        Render2DUtil.pushScissor(sx(x + 0.5F), sy(y + headerH), px(w - 1.0F), px(h - headerH - 2.0F));
        float currentY = y + headerH + 6.0F - offset;
        for (Feature feature : features) {
            float settingsHeight = settingsHeight(feature, w);
            boolean open = isExpanded(feature) && hasVisibleSettings(feature);
            float current = this.expandProgress.getOrDefault(feature, 0.0F);
            float next = open ? 1.0F : 0.0F;
            current += (next - current) * (1.0F - (float) Math.exp(-0.22D * this.frameDelta));
            if (Math.abs(next - current) < 0.002F) {
                current = next;
            }
            this.expandProgress.put(feature, current);
            float displaySettings = settingsHeight * current;
            float total = ROW_HEIGHT + displaySettings + ROW_GAP;
            if (currentY + total >= y + headerH && currentY <= y + h) {
                renderModule(extractor, feature, x, currentY, w, displaySettings);
            }
            currentY += total;
        }
        renderScrollbar(x, y, headerH, w, h, maxScroll, offset);
        Render2DUtil.popScissor();
    }

    private void renderScrollbar(float x, float y, float headerH, float w, float h, float maxScroll, float offset) {
        if (maxScroll <= 0.0F) {
            return;
        }
        float trackY = y + headerH + 7.0F;
        float trackH = h - headerH - 14.0F;
        float thumbH = Math.max(22.0F, trackH * trackH / (trackH + maxScroll));
        float thumbY = trackY + (trackH - thumbH) * (offset / maxScroll);
        int accent = ColorUtil.withAlpha(Theme.getAccent(), 185);
        rect(x + w - 5.0F, trackY, 2.0F, trackH, ColorUtil.TRANSPARENT, 1.0F);
        rect(x + w - 5.0F, thumbY, 2.0F, thumbH, accent, 1.0F);
    }

    private void renderPanelHeader(GuiGraphicsExtractor extractor, float x, float y, float w, FeatureCategory category, int opacity) {
        int index = category.ordinal();
        int iconColor = ColorUtil.withAlpha(drawAccent(), opacity);
        Identifier icon = index >= 0 && index < CATEGORY_ICONS.length ? CATEGORY_ICONS[index] : CATEGORY_ICONS[0];
        texture(x + 12.0F, y + 8.0F, 15.0F, icon, iconColor);
        text(x + 34.0F, centeredTextY(y + HEADER_HEIGHT / 2.0F, 9.0F), 9.0F,
                category.getDisplayName(), Theme.Colors.TEXT, opacity / 255.0F, UiFontStyle.SEMIBOLD);
    }

    private void renderTabBar(GuiGraphicsExtractor extractor, float x, float y, float w, int opacity) {
        float tabW = w / categoryCount();
        for (int index = 0; index < categoryCount(); index++) {
            FeatureCategory category = categoryAt(index);
            float tx = x + index * tabW;
            boolean active = category == this.selectedCategory;
            boolean hovered = hovered(tx + 2.0F, y + 2.0F, tabW - 4.0F, TAB_BAR_HEIGHT - 4.0F);
            if (active) {
                rect(tx + 2.0F, y + 2.0F, tabW - 4.0F, TAB_BAR_HEIGHT - 4.0F,
                        ColorUtil.withAlpha(drawAccent(), 52), 7.0F, 1.0F);
                outline(tx + 2.0F, y + 2.0F, tabW - 4.0F, TAB_BAR_HEIGHT - 4.0F,
                        ColorUtil.withAlpha(drawAccent(), 160), 7.0F, 0.5F, 1.0F);
            } else if (hovered) {
                rect(tx + 2.0F, y + 2.0F, tabW - 4.0F, TAB_BAR_HEIGHT - 4.0F,
                        ColorUtil.withAlpha(ColorUtil.WHITE, 9), 7.0F, 1.0F);
            }
            int iconIndex = category.ordinal();
            Identifier icon = iconIndex >= 0 && iconIndex < CATEGORY_ICONS.length ? CATEGORY_ICONS[iconIndex] : CATEGORY_ICONS[0];
            int iconColor = ColorUtil.withAlpha(active ? Theme.Colors.TEXT_TITLE : Theme.Colors.ICON, opacity);
            float iconSize = 13.0F;
            texture(tx + tabW / 2.0F - 8.5F, y + (TAB_BAR_HEIGHT - iconSize) / 2.0F, iconSize, icon, iconColor);
            textCentered(tx + tabW / 2.0F, centeredTextY(y + TAB_BAR_HEIGHT / 2.0F, 7.0F), 7.0F,
                    category.getDisplayName(), active ? Theme.Colors.TEXT_TITLE : Theme.Colors.SECONDARY, opacity / 255.0F);
        }
    }

    private void renderModule(GuiGraphicsExtractor extractor, Feature feature, float x, float y, float w,
                              float settingsHeight) {
        float rowH = ROW_HEIGHT;
        boolean hovered = this.hovered(x + 3.0F, y, w - 6.0F, rowH);
        float current = this.hoverProgress.getOrDefault(feature, 0.0F);
        float target = hovered ? 1.0F : 0.0F;
        current += (target - current) * (1.0F - (float) Math.exp(-0.16D * this.frameDelta));
        if (Math.abs(target - current) < 0.002F) {
            current = target;
        }
        this.hoverProgress.put(feature, current);

        int bg;
        if (feature.isEnabled()) {
            bg = ColorUtil.lerp(Theme.Colors.BACKGROUND_SURFACE_S, drawAccent(), 0.16F + 0.08F * current);
        } else {
            bg = ColorUtil.lerp(Theme.Colors.BACKGROUND_SURFACE_S, this.hoverBg(), current);
        }
        rect(x + 3.0F, y, w - 6.0F, rowH, bg, 5.0F, 1.0F);
        if (feature.isEnabled()) {
            rect(x + 3.0F, y + 5.0F, 2.0F, rowH - 10.0F, ColorUtil.withAlpha(drawAccent(), 230), 1.0F, 1.0F);
        }

        text(x + 10.0F, centeredTextY(y + rowH / 2.0F, 8.0F), 8.0F, feature.getName(),
                Theme.Colors.TEXT, 1.0F, UiFontStyle.MEDIUM);

        boolean binding = this.bindingModule == feature;
        if (binding || !feature.getBinds().isEmpty()) {
            String bindLabel = binding ? "..." : describeBinds(feature);
            textRight(x + w - 10.0F, centeredTextY(y + rowH / 2.0F, 7.0F), 7.0F, bindLabel,
                    binding ? ColorUtil.withAlpha(drawAccent(), 220) : Theme.Colors.SECONDARY_DARK, 1.0F);
        }

        int chevronColor = this.expandProgress.getOrDefault(feature, 0.0F) > 0.5F
                ? ColorUtil.withAlpha(drawAccent(), 240)
                : ColorUtil.withAlpha(Theme.Colors.SECONDARY, 160);
        drawChevron(extractor, x + w - 12.0F, y + rowH / 2.0F, 4.0F, 1.5F,
                this.expandProgress.getOrDefault(feature, 0.0F) > 0.5F, chevronColor);

        if (settingsHeight > 0.0F) {
            Render2DUtil.pushScissor(sx(x), sy(y + rowH), px(w), px(settingsHeight));
            renderSettings(extractor, feature, x, y + rowH, w);
            Render2DUtil.popScissor();
        }
    }

    private void renderSettings(GuiGraphicsExtractor extractor, Feature feature, float x, float y, float w) {
        float contentX = x + 7.0F + CONTENT_X;
        float contentW = w - 14.0F - 2.0F * CONTENT_X;
        float currentY = y + 3.0F;
        for (Setting<?> setting : feature.getSettings()) {
            if (!setting.isVisible()) {
                continue;
            }
            float slotH = settingHeight(setting, contentW);
            renderSetting(extractor, setting, contentX, contentW, currentY, slotH);
            currentY += slotH;
        }
    }

    private void renderSetting(GuiGraphicsExtractor extractor, Setting<?> setting, float contentX, float contentW,
                               float y, float slotH) {
        if (setting instanceof BooleanSetting bool) {
            drawToggle(extractor, contentX, contentW, y, bool);
        } else if (setting instanceof NumberSetting number) {
            drawSlider(extractor, setting, contentX, contentW, y);
        } else if (setting instanceof ModeSetting mode) {
            text(contentX, centeredTextY(y + 8.0F, 7.5F), 7.5F,
                    setting.getName() + ": " + mode.getValue(), Theme.Colors.TEXT_TEXT, 1.0F, UiFontStyle.MEDIUM);
            drawChoices(extractor, contentX, contentW, y + 15.0F, mode.getModes(),
                    mode.getValue(), null);
        } else if (setting instanceof MultiSelectSetting multi) {
            text(contentX, centeredTextY(y + 8.0F, 7.5F), 7.5F,
                    setting.getName(), Theme.Colors.TEXT_TEXT, 1.0F, UiFontStyle.MEDIUM);
            drawChoices(extractor, contentX, contentW, y + 15.0F, multi.getOptions(),
                    null, multi);
        } else if (setting instanceof ColorSetting color) {
            boolean open = this.pickers.containsKey(color);
            text(contentX, centeredTextY(y + 8.5F, 7.5F), 7.5F,
                    setting.getName(), Theme.Colors.TEXT_TEXT, 1.0F, UiFontStyle.MEDIUM);
            rect(contentX + contentW - 22.0F, y + 2.0F, 18.0F, 11.0F, color.getValue(), 3.5F, 1.0F);
            outline(contentX + contentW - 22.0F, y + 2.0F, 18.0F, 11.0F,
                    Theme.Colors.OUTLINES_LARGE, 3.5F, 0.5F, 1.0F);
            if (open) {
                renderColorPicker(extractor, color, contentX, contentW, y + 17.0F);
            }
        } else if (setting instanceof InputBindSetting inputBind) {
            text(contentX, centeredTextY(y + 8.5F, 7.5F), 7.5F,
                    setting.getName(), Theme.Colors.TEXT_TEXT, 1.0F, UiFontStyle.MEDIUM);
            String label = this.capturingBind == inputBind ? "..." : inputBind.getDisplayValue();
            drawChip(contentX + contentW, y + 3.0F, label,
                    this.capturingBind == inputBind, 11.0F, 8.0F);
        } else if (setting instanceof TextSetting text) {
            text(contentX, centeredTextY(y + 8.5F, 7.5F), 7.5F,
                    setting.getName(), Theme.Colors.TEXT_TEXT, 1.0F, UiFontStyle.MEDIUM);
            boolean editing = this.editingText == text;
            String value = editing ? this.textBuffer.toString() : text.getValue();
            if (editing && System.currentTimeMillis() % 900L < 450L) {
                value += "|";
            }
            drawChip(contentX + contentW, y + 3.0F, value, editing, 11.0F, 8.0F);
        } else if (setting instanceof ButtonSetting button) {
            rect(contentX, y + 1.0F, contentW, 12.0F, ColorUtil.withAlpha(drawAccent(), 110), 3.0F, 1.0F);
            textCentered(contentX + contentW / 2.0F, centeredTextY(y + 7.0F, 7.5F), 7.5F,
                    button.getButtonLabel(), Theme.Colors.TEXT_TITLE, 1.0F, UiFontStyle.SEMIBOLD);
        } else {
            text(contentX, centeredTextY(y + 8.5F, 7.5F), 7.5F,
                    setting.getName(), Theme.Colors.TEXT, 1.0F, UiFontStyle.REGULAR);
        }
    }

    private void renderColorPicker(GuiGraphicsExtractor extractor, ColorSetting setting, float contentX, float contentW, float y) {
        float[] hsb = this.pickers.get(setting);
        if (hsb == null) {
            return;
        }
        float pickerW = contentW * 0.76F;
        int hue = ColorUtil.hue(hsb[0]);

        Render2DUtil.rect(sx(contentX), sy(y), px(pickerW), px(PICKER_H))
                .color(ColorUtil.WHITE, hue, ColorUtil.BLACK, ColorUtil.BLACK)
                .radius(px(6.0F))
                .draw();
        outline(contentX, y, pickerW, PICKER_H, Theme.Colors.OUTLINES_LARGE, 6.0F, 0.5F, 1.0F);

        float knobX = contentX + 4.0F + hsb[1] * (pickerW - 8.0F);
        float knobY = y + 4.0F + (1.0F - hsb[2]) * (PICKER_H - 8.0F);
        rect(knobX - 4.0F, knobY - 4.0F, 8.0F, 8.0F, ColorUtil.BLACK, 4.0F, 1.0F);
        rect(knobX - 3.0F, knobY - 3.0F, 6.0F, 6.0F, ColorUtil.WHITE, 3.0F, 1.0F);

        float barX = contentX + pickerW + 5.0F;
        for (int seg = 0; seg < 32; seg++) {
            float t = seg / 32.0F;
            Render2DUtil.rect(sx(barX), sy(y + t * PICKER_H), px(4.0F), px(PICKER_H / 32.0F + 0.5F))
                    .color(ColorUtil.hue(t))
                    .draw();
        }
        rect(barX, y, 4.0F, PICKER_H, ColorUtil.TRANSPARENT, 2.0F);
        outline(barX, y, 4.0F, PICKER_H, Theme.Colors.OUTLINES_LARGE, 2.0F, 0.5F, 1.0F);
        float barY = y + hsb[0] * PICKER_H;
        rect(barX - 2.0F, barY - 4.0F, 8.0F, 8.0F, ColorUtil.BLACK, 4.0F, 1.0F);
        rect(barX - 1.0F, barY - 3.0F, 6.0F, 6.0F, ColorUtil.WHITE, 3.0F, 1.0F);
    }

    private void drawToggle(GuiGraphicsExtractor extractor, float contentX, float contentW, float y, BooleanSetting setting) {
        text(contentX, centeredTextY(y + 8.5F, 7.5F), 7.5F,
                setting.getName(), Theme.Colors.TEXT_TEXT, 1.0F, UiFontStyle.MEDIUM);
        float tw = 26.0F;
        float th = 13.0F;
        float tx = contentX + contentW - tw;
        float ty = y + 1.0F;
        float current = this.toggleProgress.getOrDefault(setting, setting.getValue() ? 1.0F : 0.0F);
        float eased = current;
        float target = setting.getValue() ? 1.0F : 0.0F;
        current += (target - current) * (1.0F - (float) Math.exp(-0.22D * this.frameDelta));
        if (Math.abs(target - current) < 0.002F) {
            current = target;
        }
        this.toggleProgress.put(setting, current);
        eased = current;
        int trackColor = ColorUtil.lerp(settingOffColor(), ColorUtil.withAlpha(drawAccent(), 210), eased);
        rect(tx, ty, tw, th, trackColor, th / 2.0F, 1.0F);
        float knobX = tx + 1.5F + (tw - 13.0F) * eased;
        rect(knobX, ty + 2.0F, th - 4.0F, th - 4.0F, ColorUtil.WHITE, (th - 4.0F) / 2.0F, 1.0F);
    }

    private void drawSlider(GuiGraphicsExtractor extractor, Setting<?> setting, float contentX, float contentW, float y) {
        NumberSetting slider = (NumberSetting) setting;
        text(contentX, centeredTextY(y + 8.0F, 7.5F), 7.5F,
                slider.getName(), Theme.Colors.TEXT_TEXT, 1.0F, UiFontStyle.MEDIUM);
        String valueLabel = String.format(Locale.ROOT, "%.1f", slider.getValue().doubleValue());
        textRight(contentX + contentW, centeredTextY(y + 8.0F, 7.5F), 7.5F,
                valueLabel, Theme.Colors.SECONDARY, 1.0F, UiFontStyle.MEDIUM);

        float raw = (float) ((slider.getValue().doubleValue() - slider.getMin()) / (slider.getMax() - slider.getMin()));
        float current = this.sliderProgress.getOrDefault(slider, raw);
        boolean dragged = this.draggingSlider == slider;
        float k = dragged ? 0.45F : 0.18F;
        current += (raw - current) * (1.0F - (float) Math.exp(-k * this.frameDelta));
        if (Math.abs(raw - current) < 0.001F) {
            current = raw;
        }
        this.sliderProgress.put(slider, current);

        float trackY = y + 14.0F;
        rect(contentX, trackY, contentW, 3.0F, ColorUtil.withAlpha(ColorUtil.WHITE, 18), 1.5F, 1.0F);
        rect(contentX, trackY, contentW * current, 3.0F, ColorUtil.withAlpha(drawAccent(), 220), 1.5F, 1.0F);
        rect(contentX + contentW * current - 4.0F, trackY - 2.5F, 8.0F, 8.0F, Theme.Colors.TEXT_TITLE, 4.0F, 1.0F);
    }

    private void drawChoices(GuiGraphicsExtractor extractor, float contentX, float contentW, float y,
                             List<String> choices, String selected, MultiSelectSetting multi) {
        int lineHeight = (int) CHIP_HEIGHT;
        int gap = (int) CHIP_GAP;
        float rowWidth = 0.0F;
        int line = 0;
        for (String choice : choices) {
            float chipWidth = chipWidth(choice);
            if (rowWidth + chipWidth > contentW + 0.01F && rowWidth > 0.0F) {
                line++;
                rowWidth = 0.0F;
            }
            boolean active = multi != null
                    ? multi.isSelected(choice)
                    : selected != null && choice.equalsIgnoreCase(selected);
            int bg = active
                    ? ColorUtil.withAlpha(drawAccent(), 190)
                    : ColorUtil.withAlpha(ColorUtil.WHITE, 18);
            rect(contentX + rowWidth, y + line * (lineHeight + gap), chipWidth, lineHeight, bg, 5.0F, 1.0F);
            textCentered(contentX + rowWidth + chipWidth / 2.0F,
                    centeredTextY(y + line * (lineHeight + gap) + lineHeight / 2.0F, 7.0F),
                    7.0F, choice, active ? ColorUtil.WHITE : ColorUtil.rgb(190, 198, 204), 1.0F);
            rowWidth += chipWidth + gap;
        }
    }

    private float chipWidth(String choice) {
        return Math.max(24.0F, textWidth(choice, 7.0F) + 10.0F);
    }

    private void drawChip(float right, float y, String label, boolean active, float height, float fontSize) {
        float chipWidth = Math.max(30.0F, textWidth(label, fontSize) + 12.0F);
        float bx = right - chipWidth;
        int bg = active
                ? ColorUtil.withAlpha(drawAccent(), 180)
                : ColorUtil.withAlpha(drawAccent(), 110);
        rect(bx, y, chipWidth, height, bg, 3.0F, 1.0F);
        textCentered(bx + chipWidth / 2.0F, centeredTextY(y + height / 2.0F, fontSize), fontSize,
                label, ColorUtil.WHITE, 1.0F);
    }

    private boolean hovered(float x, float y, float w, float h) {
        return hit(this.mouseX, this.mouseY, x, y, w, h);
    }

    private boolean hoveringSearch(float dx, float dy) {
        return dy >= SEARCH_Y && dy <= SEARCH_Y + SEARCH_HEIGHT
                && dx >= startX() && dx <= startX() + totalWidth();
    }

    private boolean handleSettingClick(FeatureCategory category, float panelX, float panelW, float dx, float dy) {
        for (ModuleRow row : rows(category, panelW)) {
            if (!isExpanded(row.feature()) || !hasVisibleSettings(row.feature())) {
                continue;
            }
            if (dy <= row.y() + row.rowHeight() || dy > row.y() + row.rowHeight() + row.settingsHeight()) {
                continue;
            }
            float contentX = panelX + 7.0F + CONTENT_X;
            float contentW = panelW - 14.0F - 2.0F * CONTENT_X;
            float settingY = row.y() + row.rowHeight() + 3.0F;
            for (Setting<?> setting : row.feature().getSettings()) {
                if (!setting.isVisible()) {
                    continue;
                }
                float slotH = settingHeight(setting, contentW);
                if (dy >= settingY && dy < settingY + slotH) {
                    if (handleSettingAt(row.feature(), setting, contentX, contentW, settingY, dx, dy)) {
                        return true;
                    }
                    return true;
                }
                settingY += slotH;
            }
        }
        return false;
    }

    private boolean handleSettingAt(Feature feature, Setting<?> setting, float contentX, float contentW, float y,
                                    float dx, float dy) {
        if (setting instanceof BooleanSetting bool) {
            float tw = 26.0F;
            float tx = contentX + contentW - tw;
            if (dy >= y && dy <= y + 14.0F && dx >= tx && dx <= tx + tw) {
                bool.setValue(!bool.getValue());
                return true;
            }
            return false;
        }
        if (setting instanceof NumberSetting number) {
            if (dy >= y + 10.0F) {
                this.draggingSlider = number;
                this.dragSliderContentX = contentX;
                this.dragSliderWidth = contentW;
                updateSliderValue(number, dx, contentX, contentW);
                return true;
            }
            return false;
        }
        if (setting instanceof ModeSetting mode) {
            if (dy > y + 15.0F) {
                String choice = choiceAt(contentX, contentW, y + 15.0F, mode.getModes(), dx, dy);
                if (choice != null) {
                    mode.setValue(choice);
                    return true;
                }
            }
            return false;
        }
        if (setting instanceof MultiSelectSetting multi) {
            if (dy > y + 15.0F) {
                String choice = choiceAt(contentX, contentW, y + 15.0F, multi.getOptions(), dx, dy);
                if (choice != null) {
                    multi.toggle(choice);
                    return true;
                }
            }
            return false;
        }
        if (setting instanceof ColorSetting color) {
            float swatchX = contentX + contentW - 22.0F;
            if (dy >= y && dy <= y + 14.0F && dx >= swatchX && dx <= swatchX + 18.0F) {
                if (this.pickers.containsKey(color)) {
                    this.pickers.remove(color);
                } else {
                    float[] hsb = ColorUtil.hsv(color.getValue());
                    this.pickers.put(color, new float[]{hsb[0], hsb[1], hsb[2]});
                }
                return true;
            }
            if (this.pickers.containsKey(color)) {
                float pickerW = contentW * 0.76F;
                float py = y + 17.0F;
                if (dx >= contentX + pickerW + 5.0F && dx <= contentX + pickerW + 9.0F && dy >= py && dy <= py + PICKER_H) {
                    this.draggingPicker = color;
                    this.draggingHue = true;
                    updatePicker(color, dx, dy);
                    return true;
                }
                if (dx >= contentX && dx <= contentX + pickerW && dy >= py && dy <= py + PICKER_H) {
                    this.draggingPicker = color;
                    this.draggingHue = false;
                    updatePicker(color, dx, dy);
                    return true;
                }
            }
            return false;
        }
        if (setting instanceof InputBindSetting inputBind) {
            String label = this.capturingBind == inputBind ? "..." : inputBind.getDisplayValue();
            float chipWidth = Math.max(30.0F, textWidth(label, 8.0F) + 12.0F);
            float bx = contentX + contentW - chipWidth;
            if (dx >= bx && dx <= bx + chipWidth && dy >= y + 3.0F && dy <= y + 14.0F) {
                this.capturingBind = this.capturingBind == inputBind ? null : inputBind;
                this.editingText = null;
                return true;
            }
            return false;
        }
        if (setting instanceof TextSetting text) {
            String label = text.getValue();
            float chipWidth = Math.max(30.0F, textWidth(label, 8.0F) + 12.0F);
            float bx = contentX + contentW - chipWidth;
            if (dx >= bx && dx <= bx + chipWidth && dy >= y + 3.0F && dy <= y + 14.0F) {
                if (this.editingText != text) {
                    this.textBuffer.setLength(0);
                    this.textBuffer.append(text.getValue());
                }
                this.editingText = this.editingText == text ? null : text;
                this.capturingBind = null;
                return true;
            }
            return false;
        }
        if (setting instanceof ButtonSetting button) {
            if (dy >= y && dy <= y + 14.0F && dx >= contentX && dx <= contentX + contentW) {
                button.press();
                return true;
            }
            return false;
        }
        return false;
    }

    private String choiceAt(float contentX, float contentW, float y, List<String> choices, float dx, float dy) {
        int lineHeight = (int) CHIP_HEIGHT;
        int gap = (int) CHIP_GAP;
        float rowWidth = 0.0F;
        int line = 0;
        for (String choice : choices) {
            float chipWidth = chipWidth(choice);
            if (rowWidth + chipWidth > contentW + 0.01F && rowWidth > 0.0F) {
                line++;
                rowWidth = 0.0F;
            }
            if (dx >= contentX + rowWidth && dx <= contentX + rowWidth + chipWidth
                    && dy >= y + line * (lineHeight + gap) && dy <= y + line * (lineHeight + gap) + lineHeight) {
                return choice;
            }
            rowWidth += chipWidth + gap;
        }
        return null;
    }

    private void updateSliderValue(NumberSetting slider, float dx, float contentX, float contentW) {
        if (contentW <= 0.0F) {
            return;
        }
        float progress = MathUtil.clamp((dx - contentX) / contentW, 0.0F, 1.0F);
        double min = slider.getMin();
        double max = slider.getMax();
        double value = min + progress * (max - min);
        double step = slider.getStep();
        if (step > 0.0D) {
            value = min + Math.round((value - min) / step) * step;
        }
        slider.setValue(MathUtil.clamp(value, min, max));
    }

    private void updatePicker(ColorSetting setting, float dx, float dy) {
        float[] hsb = this.pickers.get(setting);
        if (hsb == null) {
            return;
        }
        float[] geometry = pickerGeometry(setting);
        if (geometry == null) {
            return;
        }
        float contentX = geometry[0];
        float contentW = geometry[1];
        float pickerY = geometry[2];
        float pickerW = contentW * 0.76F;
        if (this.draggingHue) {
            hsb[0] = MathUtil.clamp((dy - pickerY) / PICKER_H, 0.0F, 1.0F);
        } else {
            hsb[1] = MathUtil.clamp((dx - contentX - 4.0F) / (pickerW - 8.0F), 0.0F, 1.0F);
            hsb[2] = 1.0F - MathUtil.clamp((dy - pickerY - 4.0F) / (PICKER_H - 8.0F), 0.0F, 1.0F);
        }
        setting.setValue(ColorUtil.fromHsv(hsb[0], hsb[1], hsb[2], 255));
    }

    private float[] pickerGeometry(ColorSetting setting) {
        for (int index = 0; index < panelCount(); index++) {
            FeatureCategory category = categoryOf(index);
            float panelX = panelX(index);
            float panelW = panelWidth();
            for (ModuleRow row : rows(category, panelW)) {
                if (!isExpanded(row.feature())) {
                    continue;
                }
                float contentX = panelX + 7.0F + CONTENT_X;
                float contentW = panelW - 14.0F - 2.0F * CONTENT_X;
                float settingY = row.y() + row.rowHeight() + 3.0F;
                for (Setting<?> candidate : row.feature().getSettings()) {
                    if (candidate == setting) {
                        return new float[]{contentX, contentW, settingY + 17.0F};
                    }
                    float slotH = settingHeight(candidate, contentW);
                    if (candidate.isVisible()) {
                        settingY += slotH;
                    }
                }
            }
        }
        return null;
    }

    private void commitText() {
        if (this.editingText != null) {
            this.editingText.setValue(this.textBuffer.toString());
            this.editingText = null;
            this.textBuffer.setLength(0);
        }
    }

    private Feature featureAt(FeatureCategory category, float panelX, float panelW, float dx, float dy) {
        for (ModuleRow row : rows(category, panelW)) {
            if (dx >= panelX + 3.0F && dx <= panelX + panelW - 3.0F
                    && dy >= row.y() && dy < row.y() + row.rowHeight()) {
                return row.feature();
            }
        }
        return null;
    }

    private List<ModuleRow> rows(FeatureCategory category, float panelW) {
        List<ModuleRow> result = new ArrayList<>();
        float headerH = isTabs() ? TAB_BAR_HEIGHT : HEADER_HEIGHT;
        float offset = this.displayScroll.getOrDefault(category, 0.0F);
        float y = PANEL_TOP + headerH + 6.0F - offset;
        for (Feature feature : visibleFeatures(category)) {
            float settingsHeight = settingsHeight(feature, panelW)
                    * this.expandProgress.getOrDefault(feature, 0.0F);
            float rowH = ROW_HEIGHT + settingsHeight;
            result.add(new ModuleRow(feature, y, ROW_HEIGHT, settingsHeight));
            y += rowH + ROW_GAP;
        }
        return result;
    }

    private float maxScroll(FeatureCategory category, float panelW) {
        float headerH = isTabs() ? TAB_BAR_HEIGHT : HEADER_HEIGHT;
        float content = panelHeight() - headerH - 10.0F;
        float total = 0.0F;
        for (ModuleRow row : rows(category, panelW)) {
            total += row.rowHeight() + row.settingsHeight() + ROW_GAP;
        }
        return Math.max(0.0F, total - content);
    }

    private float smoothScroll(FeatureCategory category, float target) {
        float current = this.displayScroll.getOrDefault(category, 0.0F);
        if (!EditClickGUI.INSTANCE.smoothScroll.getValue()) {
            this.displayScroll.put(category, target);
            return target;
        }
        float speed = EditClickGUI.INSTANCE.scrollSpeed.getValue().floatValue();
        float k = 1.0F - (float) Math.pow(1.0F - Math.min(1.0F, speed / 60.0F), this.frameDelta);
        k = MathUtil.clamp(k, 0.001F, 1.0F);
        float next = current + (target - current) * k;
        if (Math.abs(target - next) < 0.05F) {
            next = target;
        }
        this.displayScroll.put(category, next);
        return next;
    }

    private float settingsHeight(Feature feature, float contentW) {
        float total = 3.0F;
        for (Setting<?> setting : feature.getSettings()) {
            if (!setting.isVisible()) {
                continue;
            }
            total += settingHeight(setting, contentW);
        }
        return total;
    }

    private float settingHeight(Setting<?> setting, float contentW) {
        if (setting instanceof NumberSetting) {
            return 24.0F;
        }
        if (setting instanceof ModeSetting mode) {
            return 16.0F + choiceRowsHeight(mode.getModes(), contentW) + SETTING_GAP;
        }
        if (setting instanceof MultiSelectSetting multi) {
            return 16.0F + choiceRowsHeight(multi.getOptions(), contentW) + SETTING_GAP;
        }
        if (setting instanceof ColorSetting color) {
            return 17.0F + (this.pickers.containsKey(color) ? PICKER_H + 6.0F : 0.0F);
        }
        return 17.0F;
    }

    private float choiceRowsHeight(List<String> choices, float contentW) {
        int rows = 1;
        float rowWidth = 0.0F;
        for (String choice : choices) {
            float chipWidth = chipWidth(choice);
            if (rowWidth + chipWidth > contentW + 0.01F && rowWidth > 0.0F) {
                rows++;
                rowWidth = 0.0F;
            }
            rowWidth += chipWidth + CHIP_GAP;
        }
        return rows * (CHIP_HEIGHT + CHIP_GAP);
    }

    private boolean hasVisibleSettings(Feature feature) {
        for (Setting<?> setting : feature.getSettings()) {
            if (setting.isVisible()) {
                return true;
            }
        }
        return false;
    }

    private boolean isExpanded(Feature feature) {
        return this.expanded.getOrDefault(feature, false);
    }

    private List<Feature> visibleFeatures(FeatureCategory category) {
        String query = this.searchQuery.toString().trim().toLowerCase(Locale.ROOT);
        List<Feature> features = FeatureManager.INSTANCE.getFeatures(category);
        if (query.isEmpty()) {
            return features;
        }
        List<Feature> filtered = new ArrayList<>();
        for (Feature feature : features) {
            if (feature.getName().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(feature);
            }
        }
        return filtered;
    }

    private int panelAt(float dx, float dy) {
        float headerH = isTabs() ? TAB_BAR_HEIGHT : HEADER_HEIGHT;
        for (int index = 0; index < panelCount(); index++) {
            float x = panelX(index);
            float w = panelWidth();
            if (dx < x || dx > x + w) {
                continue;
            }
            if (dy < PANEL_TOP + headerH || dy > PANEL_TOP + panelHeight()) {
                continue;
            }
            return index;
        }
        return -1;
    }

    private FeatureCategory categoryOf(int index) {
        return isTabs() ? this.selectedCategory : categoryAt(index);
    }

    private int panelCount() {
        return isTabs() ? 1 : categoryCount();
    }

    private int categoryCount() {
        return FeatureCategory.values().length;
    }

    private FeatureCategory categoryAt(int index) {
        FeatureCategory[] categories = FeatureCategory.values();
        return categories[Math.floorMod(index, categories.length)];
    }

    private boolean isTabs() {
        return EditClickGUI.INSTANCE.isTabs();
    }

    private float panelWidth() {
        float gap = panelGap();
        if (isTabs()) {
            float wide = EditClickGUI.INSTANCE.panelWidth.getValue().floatValue() * 1.5F;
            return MathUtil.clamp(wide, 240.0F, 1024.0F - 40.0F);
        }
        float requested = EditClickGUI.INSTANCE.panelWidth.getValue().floatValue();
        int count = categoryCount();
        float available = 1024.0F - 2.0F * 20.0F - (count - 1) * gap;
        float byCount = available / count;
        return Math.min(requested, Math.max(70.0F, byCount));
    }

    private float panelHeight() {
        float requested = EditClickGUI.INSTANCE.panelHeight.getValue().floatValue();
        return Math.min(requested, 640.0F - PANEL_TOP - BOTTOM_MARGIN);
    }

    private float panelGap() {
        return EditClickGUI.INSTANCE.panelGap.getValue().floatValue();
    }

    private float panelRadius() {
        return EditClickGUI.INSTANCE.panelRadius.getValue().floatValue();
    }

    private float totalWidth() {
        int count = panelCount();
        return count * panelWidth() + (count - 1) * panelGap();
    }

    private float startX() {
        return (1024.0F - totalWidth()) / 2.0F;
    }

    private float panelX(int index) {
        return startX() + index * (panelWidth() + panelGap());
    }

    private int drawAccent() {
        return Theme.getAccent();
    }

    private int hoverBg() {
        return Theme.Colors.CARD_HOVER;
    }

    private int settingOffColor() {
        return ColorUtil.withAlpha(ColorUtil.WHITE, 22);
    }

    private float textWidth(String text, float size) {
        return UiFonts.sfProDisplay().measureWidth(text, size, size * UiFontStyle.REGULAR.letterSpacingEm());
    }

    private String describeBinds(Feature feature) {
        List<Integer> binds = feature.getBinds();
        if (binds.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int bind : binds) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(BindSetting.describe(bind));
        }
        return builder.toString();
    }

    private void drawChevron(GuiGraphicsExtractor extractor, float cx, float cy, float size, float thickness,
                             boolean up, int color) {
        float k = size * 0.5F;
        if (up) {
            rotatedSegment(extractor, cx - size, cy + k, cx, cy - k, thickness, color);
            rotatedSegment(extractor, cx + size, cy + k, cx, cy - k, thickness, color);
        } else {
            rotatedSegment(extractor, cx - size, cy - k, cx, cy + k, thickness, color);
            rotatedSegment(extractor, cx + size, cy - k, cx, cy + k, thickness, color);
        }
    }

    private void rotatedSegment(GuiGraphicsExtractor extractor, float x1, float y1, float x2, float y2,
                                float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0F) {
            return;
        }
        var pose = extractor.pose();
        pose.pushMatrix();
        pose.translate((x1 + x2) / 2.0F, (y1 + y2) / 2.0F);
        pose.rotate((float) Math.atan2(dy, dx));
        Render2DUtil.rect(-length / 2.0F, -thickness / 2.0F, length, thickness)
                .color(color)
                .radius(thickness / 2.0F)
                .draw();
        pose.popMatrix();
    }

    private void updateFrameDelta() {
        long now = System.currentTimeMillis();
        if (this.lastFrameMs == 0L) {
            this.frameDelta = 1.0F;
        } else {
            this.frameDelta = MathUtil.clamp((now - this.lastFrameMs) / 16.667F, 0.5F, 8.0F);
        }
        this.lastFrameMs = now;
    }
}