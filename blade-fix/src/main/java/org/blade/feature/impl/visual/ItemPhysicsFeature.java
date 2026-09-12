package org.blade.feature.impl.visual;

import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class ItemPhysicsFeature extends Feature {
    public ItemPhysicsFeature() {
        super("ItemPhysics", "Dropped items lie on the ground", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }
}
