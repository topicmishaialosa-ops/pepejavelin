package org.blade.feature.impl.misc;

import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class ScoreboardHealthFeature extends Feature {
    public ScoreboardHealthFeature() {
        super("ScoreboardHealth", "Фиксит хп цели если оно фейк", FeatureCategory.MISC, BindSetting.UNBOUND);
    }
}
