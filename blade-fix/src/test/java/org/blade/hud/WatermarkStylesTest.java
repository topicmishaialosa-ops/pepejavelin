package org.blade.hud;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatermarkStylesTest {
    @Test
    void exposesSeventyUniquePresetsAlongsideLegacyStyles() {
        assertEquals(73, WatermarkStyles.OPTIONS.length);
        assertFalse(WatermarkStyles.isCustom("New"));
        Set<String> names = new HashSet<>();
        int custom = 0;
        for (String option : WatermarkStyles.OPTIONS) {
            if (!WatermarkStyles.isCustom(option)) continue;
            custom++;
            assertTrue(names.add(option), option);
            WatermarkStyles.Preset preset = WatermarkStyles.byName(option);
            assertNotNull(preset);
            assertNotNull(preset.family());
            assertNotNull(preset.surface());
            assertFalse(preset.name().isBlank());
            assertTrue(preset.scale() > 0.0F);
            assertTrue((preset.accent() >>> 24) > 0);
        }
        assertEquals(70, custom);
    }
}
