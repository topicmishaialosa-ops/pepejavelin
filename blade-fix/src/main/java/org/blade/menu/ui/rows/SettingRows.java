package org.blade.menu.ui.rows;

import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.InputBindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.MultiSelectSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.setting.Setting;
import org.blade.feature.setting.TextSetting;

public final class SettingRows {
    private SettingRows() {
    }

    public static SettingRow create(String featureName, Setting<?> setting) {
        SettingRow row = switch (setting) {
            case BooleanSetting booleanSetting -> new ToggleRow(booleanSetting);
            case ButtonSetting buttonSetting -> new ButtonRow(buttonSetting);
            case NumberSetting numberSetting -> new SliderRow(numberSetting);
            case ModeSetting modeSetting -> new DropdownRow(modeSetting);
            case MultiSelectSetting multiSelectSetting -> new DropdownRow(multiSelectSetting);
            case ColorSetting colorSetting -> new ColorRow(colorSetting);
            case TextSetting textSetting -> new TextRow(textSetting);
            case InputBindSetting inputBindSetting -> new BindChipRow(inputBindSetting);
            default -> null;
        };
        return row == null ? null : row.context(featureName);
    }
}
