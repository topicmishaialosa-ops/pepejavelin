package org.blade.feature.impl.misc;

import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.TextSetting;

public final class RenamePasterClientFeature extends Feature {
    private static final String DEFAULT_NAME = "Blade";
    private static RenamePasterClientFeature INSTANCE;

    private final TextSetting clientName = register(new TextSetting("Название клиента", DEFAULT_NAME));

    public RenamePasterClientFeature() {
        super("RenamePasterClient", "Замена отображаемого названия клиента на своё", FeatureCategory.MISC, BindSetting.UNBOUND);
        INSTANCE = this;
    }

    public static String getClientName() {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return DEFAULT_NAME;
        String name = INSTANCE.clientName.getValue();
        return name != null && !name.isBlank() ? name.trim() : DEFAULT_NAME;
    }

    public static String getClientNameLower() {
        return getClientName().toLowerCase();
    }
}