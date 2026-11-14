package org.blade.feature.impl.visual;

import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;

public final class AspectRatioFeature extends Feature {
    private static final String CUSTOM = "Своё";

    public final ModeSetting mode = register(new ModeSetting(
            "Соотношение сторон", "4:3", "Авто", "4:3", "5:4", "16:9", "21:9", CUSTOM
    ));
    public final NumberSetting ratio = register(new NumberSetting(
            "Своё соотношение", 1.33, 0.5, 3.5, 0.01, ""
    ).visibleWhen(() -> this.mode.is(CUSTOM)));

    public AspectRatioFeature() {
        super("AspectRatio", "Changes the aspect ratio of the 3D world", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public static float resolveRatio(float original) {
        AspectRatioFeature feature = FeatureManager.INSTANCE.getEnabled(AspectRatioFeature.class);
        if (feature == null) {
            return original;
        }
        return switch (feature.mode.getValue()) {
            case "4:3" -> 4.0F / 3.0F;
            case "5:4" -> 5.0F / 4.0F;
            case "16:9" -> 16.0F / 9.0F;
            case "21:9" -> 21.0F / 9.0F;
            case CUSTOM -> feature.ratio.getValue().floatValue();
            default -> original;
        };
    }
}
