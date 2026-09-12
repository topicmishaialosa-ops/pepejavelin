package org.blade.feature.impl.misc;

import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.TextSetting;

public final class NameProtectFeature extends Feature {
    public final TextSetting name = register(new TextSetting("Name", "BladeUser", 32));

    public NameProtectFeature() {
        super("NameProtect", "Protects your nickname in client UI", FeatureCategory.MISC, BindSetting.UNBOUND);
    }
}
