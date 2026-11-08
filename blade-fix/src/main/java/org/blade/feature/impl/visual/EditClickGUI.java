package org.blade.feature.impl.visual;

import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;

public final class EditClickGUI extends Feature {
    public static final String STYLE_CARDS = "Cards";
    public static final String STYLE_DROPDOWN = "Dropdown";
    public static final String LAYOUT_COLUMNS = "Columns";
    public static final String LAYOUT_TABS = "Tabs";

    public static final EditClickGUI INSTANCE = new EditClickGUI();

    public final ModeSetting style = register(new ModeSetting(
            "GUI Style",
            STYLE_CARDS,
            STYLE_CARDS,
            STYLE_DROPDOWN
    ).configKey("menu.clickgui.style"));
    public final ModeSetting layout = register(new ModeSetting(
            "Layout",
            LAYOUT_COLUMNS,
            LAYOUT_COLUMNS,
            LAYOUT_TABS
    ).configKey("menu.clickgui.layout")
            .visibleWhen(() -> isDropdown()));
    public final NumberSetting panelWidth = register(new NumberSetting(
            "Panel Width",
            1024.0D,
            800.0D,
            1200.0D,
            10.0D,
            "px"
    ).configKey("menu.clickgui.panelWidth").visibleWhen(() -> isDropdown()));
    public final NumberSetting panelHeight = register(new NumberSetting(
            "Panel Height",
            800.0D,
            400.0D,
            1200.0D,
            10.0D,
            "px"
    ).configKey("menu.clickgui.panelHeight").visibleWhen(() -> isDropdown()));
    public final NumberSetting panelGap = register(new NumberSetting(
            "Panel Gap",
            5.0D,
            0.0D,
            50.0D,
            1.0D,
            "px"
    ).configKey("menu.clickgui.panelGap").visibleWhen(() -> isDropdown()));
    public final NumberSetting panelOpacity = register(new NumberSetting(
            "Panel Opacity",
            180.0D,
            40.0D,
            255.0D,
            1.0D,
            ""
    ).configKey("menu.clickgui.panelOpacity").visibleWhen(() -> isDropdown()));
    public final NumberSetting panelRadius = register(new NumberSetting(
            "Panel Radius",
            7.0D,
            0.0D,
            12.0D,
            1.0D,
            "px"
    ).configKey("menu.clickgui.panelRadius").visibleWhen(() -> isDropdown()));
    public final BooleanSetting blur = register(new BooleanSetting(
            "Panel Blur",
            false
    ).configKey("menu.clickgui.blur").visibleWhen(() -> isDropdown()));
    public final BooleanSetting smoothScroll = register(new BooleanSetting(
            "Smooth Scroll",
            true
    ).configKey("menu.clickgui.smoothScroll").visibleWhen(() -> isDropdown()));
public final NumberSetting scrollSpeed = register(new NumberSetting(
            "Scroll Speed",
            40.0D,
            1.0D,
            100.0D,
            1.0D,
            ""
    ).configKey("menu.clickgui.scrollSpeed").visibleWhen(() -> isDropdown()));
    public final NumberSetting scrollElasticity = register(new NumberSetting(
            "Scroll Elasticity",
            0.0D,
            0.0D,
            100.0D,
            1.0D,
            ""
    ).configKey("menu.clickgui.scrollElasticity").visibleWhen(() -> isDropdown()));
    public final BooleanSetting showSearch = register(new BooleanSetting(
            "Show Search",
            true
    ).configKey("menu.clickgui.showSearch").visibleWhen(() -> isDropdown()));

    private EditClickGUI() {
        super("ClickGUI Editor", "Dropdown module browser with live search", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public boolean isDropdown() {
        return this.style.is(STYLE_DROPDOWN);
    }

    public boolean isColumns() {
        return this.layout.is(LAYOUT_COLUMNS);
    }

    public boolean isTabs() {
        return this.layout.is(LAYOUT_TABS);
    }
}