package org.blade.feature.impl.visual.targethud;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.blade.menu.core.MenuConfigStore;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class TargetHudPresetManager {
    private static final String CONFIG_KEY = "targethudPresets";
    private static final Gson GSON = new Gson();
    private static final List<TargetHudPreset> CACHE = new ArrayList<>();
    private static boolean loaded;

    private TargetHudPresetManager() {
    }

    public static List<TargetHudPreset> getPresets() {
        load();
        return new ArrayList<>(CACHE);
    }

    public static TargetHudPreset findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (TargetHudPreset preset : getPresets()) {
            if (preset.name.equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return null;
    }

    public static void savePreset(TargetHudPreset preset) {
        load();
        CACHE.removeIf(existing -> existing.name.equalsIgnoreCase(preset.name));
        CACHE.add(preset);
        persist();
    }

    public static boolean deletePreset(String name) {
        load();
        boolean removed = CACHE.removeIf(existing -> existing.name.equalsIgnoreCase(name));
        if (removed) {
            persist();
        }
        return removed;
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        CACHE.clear();
        JsonArray array = MenuConfigStore.getArray(CONFIG_KEY);
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                try {
                    TargetHudPreset preset = GSON.fromJson(element, TargetHudPreset.class);
                    if (preset != null) {
                        CACHE.add(preset);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        if (CACHE.isEmpty()) {
            importFromHuihui();
        }
    }

    private static void importFromHuihui() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return;
            }
            File file = new File(new File(mc.gameDirectory, "Huihui"), "targethud_presets.json");
            if (!file.isFile()) {
                return;
            }
            JsonArray array = JsonParser.parseString(Files.readString(file.toPath())).getAsJsonArray();
            boolean imported = false;
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                try {
                    TargetHudPreset preset = GSON.fromJson(element, TargetHudPreset.class);
                    if (preset == null || preset.name == null || preset.name.isBlank()) {
                        continue;
                    }
                    boolean exists = CACHE.stream()
                            .anyMatch(existing -> existing.name.equalsIgnoreCase(preset.name));
                    if (!exists) {
                        CACHE.add(preset);
                        imported = true;
                    }
                } catch (Exception ignored) {
                }
            }
            if (imported) {
                persist();
            }
        } catch (Exception ignored) {
        }
    }

    private static TargetHudPreset template(String name, String type, int bar, int barSecond, int bg, int border,
                                            int text, boolean custom, double radius, double borderThickness,
                                            double backgroundAlpha, double headSize, double x, double y,
                                            boolean showSecondBar, boolean matchBarThickness, double secondBarThickness) {
        TargetHudPreset preset = new TargetHudPreset();
        preset.name = name;
        preset.type = type;
        preset.x = x;
        preset.y = y;
        preset.barThickness = 1.0;
        preset.barRadius = 2.0;
        preset.showSecondBar = showSecondBar;
        preset.matchBarThickness = matchBarThickness;
        preset.secondBarThickness = secondBarThickness;
        preset.headSize = headSize;
        preset.headYaw = 0.0;
        preset.headPitch = 0.0;
        preset.headAutoRotate = false;
        preset.displayMode = "Проценты и HP";
        preset.customColors = custom;
        preset.barColor = bar;
        preset.barColorSecond = barSecond;
        preset.bgColor = bg;
        preset.borderColor = border;
        preset.textColor = text;
        preset.radius = radius;
        preset.borderThickness = borderThickness;
        preset.backgroundAlpha = backgroundAlpha;
        preset.animationSpeed = 1.0;
        preset.showArmor = true;
        preset.showPing = true;
        preset.showEyes = true;
        preset.eyeSize = 1.0;
        preset.eyeColor = 0xFFFFFFFF;
        preset.pupilColor = 0xFF0F0F0F;
        preset.bgImage = "";
        preset.headImage = "";
        return preset;
    }

    public static List<TargetHudPreset> getTemplates() {
        List<TargetHudPreset> list = new ArrayList<>();
        list.add(template("Классика", "Крупный", 0xFF3898FF, 0xFF646473, 0xFF000000, 0xFF646473, 0xFFFFFFFF,
                false, 5.0, 1.0, 120.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Неон", "Большая полоса", 0xFF00CFFF, 0xFF0077B6, 0xFF002233, 0xFF00CFFF, 0xFFFFFFFF,
                true, 6.0, 1.5, 170.0, 1.3, 4.0, 4.0, true, true, 0.8));
        list.add(template("Золото", "Крупный", 0xFFFFB300, 0xFFFF8F00, 0xFF120A00, 0xFFFFB300, 0xFFFFE082,
                true, 8.0, 1.5, 170.0, 1.0, 4.0, 4.0, true, false, 0.6));
        list.add(template("Метал", "Метал", 0xFF9EA7B0, 0xFF5C636B, 0xFF101318, 0xFF5C636B, 0xFFFFFFFF,
                true, 3.0, 1.0, 200.0, 1.1, 4.0, 4.0, true, true, 1.0));
        list.add(template("Проценты", "Проценты", 0xFF3DDC84, 0xFF2BB673, 0xFF0B1F17, 0xFF3DDC84, 0xFFC8FFE0,
                true, 5.0, 1.0, 130.0, 1.1, 4.0, 4.0, false, true, 1.0));
        list.add(template("Градиент", "Градиент", 0xFF8E2DE2, 0xFF4A00E0, 0xFF13002E, 0xFF8E2DE2, 0xFFE3D5FF,
                true, 8.0, 1.0, 140.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Кружок", "Кружок", 0xFFFF3D3D, 0xFFFF8A5C, 0xFF2B0000, 0xFFFF3D3D, 0xFFFFFFFF,
                true, 8.0, 0.0, 120.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Вертикальный", "Вертикальный", 0xFF00E676, 0xFF00A85A, 0xFF00220F, 0xFF00A85A, 0xFFB9F6CA,
                true, 5.0, 1.0, 140.0, 1.2, 4.0, 4.0, false, true, 1.0));
        list.add(template("Минимал", "Минимал", 0xFFEAEAEA, 0xFF9C9CA6, 0xFF0C0C0E, 0xFF3C3C42, 0xFFFFFFFF,
                true, 3.0, 0.5, 90.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Лаванда", "Мини", 0xFFB39DFB, 0xFF7E57C2, 0xFF120C24, 0xFF7E57C2, 0xFFFFFFFF,
                true, 5.0, 1.0, 150.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Малютка", "Маленький", 0xFFFF6B9D, 0xFFC44569, 0xFF1A0A12, 0xFFFF6B9D, 0xFFFFD6E8,
                true, 4.0, 1.0, 150.0, 0.8, 4.0, 4.0, false, true, 1.0));
        list.add(template("Корона", "Боссбар", 0xFFFFD700, 0xFFB8860B, 0xFF0D0D0F, 0xFFFFD700, 0xFFFFF8DC,
                true, 2.0, 1.0, 160.0, 1.0, 4.0, 4.0, true, true, 0.7));
        list.add(template("Матрица", "Проценты", 0xFF00FF41, 0xFF00B832, 0xFF001204, 0xFF00FF41, 0xFFB9FFC9,
                true, 4.0, 1.0, 130.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Арктика", "Метал", 0xFF7FDBFF, 0xFF39A0ED, 0xFF0A1622, 0xFF39A0ED, 0xFFFFFFFF,
                true, 3.0, 1.0, 190.0, 1.1, 4.0, 4.0, true, true, 1.0));
        list.add(template("Огонь", "Градиент", 0xFFFF5722, 0xFFFF9800, 0xFF1A0A00, 0xFFFF5722, 0xFFFFE0B2,
                true, 6.0, 1.0, 150.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Кислота", "Вертикальный", 0xFFC6FF00, 0xFF8BC34A, 0xFF0C1400, 0xFFC6FF00, 0xFFF4FFB0,
                true, 5.0, 1.0, 140.0, 1.2, 4.0, 4.0, false, true, 1.0));
        list.add(template("Ночь", "Крупный", 0xFF9C27B0, 0xFF6A1B9A, 0xFF0C0614, 0xFF6A1B9A, 0xFFE1BEE7,
                true, 7.0, 1.5, 170.0, 1.0, 4.0, 4.0, true, false, 0.6));
        list.add(template("Кровь", "Большая полоса", 0xFFF44336, 0xFFB71C1C, 0xFF170000, 0xFFF44336, 0xFFFFCDD2,
                true, 6.0, 1.5, 180.0, 1.3, 4.0, 4.0, true, true, 0.9));
        list.add(template("Пузырь", "Кружок", 0xFF00E5FF, 0xFF00B8D4, 0xFF001418, 0xFF00E5FF, 0xFFFFFFFF,
                true, 8.0, 0.0, 130.0, 1.0, 4.0, 4.0, false, true, 1.0));
        list.add(template("Уголёк", "Мини", 0xFFFF7043, 0xFFD84315, 0xFF140800, 0xFFD84315, 0xFFFFCCBC,
                true, 4.0, 1.0, 150.0, 1.0, 4.0, 4.0, false, true, 1.0));
        return list;
    }

    private static void persist() {
        MenuConfigStore.save(root -> {
            JsonArray array = new JsonArray();
            for (TargetHudPreset preset : CACHE) {
                JsonObject object = GSON.toJsonTree(preset).getAsJsonObject();
                array.add(object);
            }
            root.add(CONFIG_KEY, array);
        });
    }
}