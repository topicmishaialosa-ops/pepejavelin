package org.blade.utils.render.cosmetics;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.blade.feature.impl.visual.CosmeticsFeature.Cosmetic;
import org.blade.utils.render.world.WorldMeshRenderer;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public final class CosmeticWeapons {
    private static final Map<String, List<Part>> MODELS = loadModels();

    private CosmeticWeapons() {
    }

    public static String modelName(Cosmetic cosmetic) {
        return switch (cosmetic.name()) {
            case "Кровавый" -> "blood";
            case "Ледяной" -> "frost";
            case "Золотой" -> "gold";
            case "Незеритовый палаш" -> "netherite";
            case "Катана Бездны" -> "katana_void";
            case "Катана Сакура" -> "katana_sakura";
            case "Ледяная катана" -> "katana_frost";
            default -> "netherite";
        };
    }

    public static Identifier itemModel(Cosmetic cosmetic) {
        return Identifier.parse("blade:cosmetics/" + modelName(cosmetic));
    }

    public static void append(Cosmetic cosmetic, Vec3 origin, double yaw, double scale,
                              List<WorldMeshRenderer.PlaneRect> faces) {
        CosmeticGeometry g = new CosmeticGeometry(faces, origin, yaw, scale / 16.0);
        for (Part p : MODELS.getOrDefault(modelName(cosmetic), List.of())) {
            g.box(p.x - 8, p.y, p.z - 8, p.w, p.h, p.d, p.color);
        }
    }

    private static Map<String, List<Part>> loadModels() {
        Map<String, List<Part>> result = new HashMap<>();
        try (var stream = CosmeticWeapons.class.getResourceAsStream("/assets/blade/cosmetics/weapons.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing cosmetic weapon geometry");
            }
            var root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                List<Part> parts = new ArrayList<>();
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    var object = element.getAsJsonObject();
                    var center = object.getAsJsonArray("center");
                    var size = object.getAsJsonArray("size");
                    parts.add(new Part(center.get(0).getAsDouble(), center.get(1).getAsDouble(), center.get(2).getAsDouble(),
                            size.get(0).getAsDouble(), size.get(1).getAsDouble(), size.get(2).getAsDouble(),
                            object.get("color").getAsInt() | 0xFF000000));
                }
                result.put(entry.getKey(), List.copyOf(parts));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot read cosmetic weapon geometry", exception);
        }
        return Map.copyOf(result);
    }

    private record Part(double x, double y, double z, double w, double h, double d, int color) {
    }
}
