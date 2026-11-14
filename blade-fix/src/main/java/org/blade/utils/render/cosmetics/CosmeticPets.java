package org.blade.utils.render.cosmetics;

import net.minecraft.world.phys.Vec3;
import org.blade.feature.impl.visual.CosmeticsFeature.Cosmetic;
import org.blade.utils.render.world.WorldMeshRenderer;

import java.util.List;

/** Original miniature companions, built from solid articulated parts rather than mob entities. */
public final class CosmeticPets {
    private CosmeticPets() {
    }

    public static boolean onShoulder(Cosmetic pet) {
        return pet.name().equals("Неоновый ара") || pet.name().equals("Феникс");
    }

    public static void append(Cosmetic pet, Vec3 origin, double yaw, double scale, double time,
                              List<WorldMeshRenderer.PlaneRect> faces) {
        CosmeticGeometry g = new CosmeticGeometry(faces, origin, yaw, scale);
        switch (pet.name()) {
            case "Киберпёс", "Лунный волк", "Лисёнок", "Кот-астронавт" -> quadruped(g, pet, time);
            case "Дракон Бездны", "Ледяной дракон" -> dragon(g, pet, time);
            case "Неоновый ара", "Феникс" -> bird(g, pet, time);
            default -> crystal(g, pet, time);
        }
    }

    private static void quadruped(CosmeticGeometry g, Cosmetic pet, double time) {
        int fur = pet.primary();
        int trim = pet.secondary();
        boolean fox = pet.name().equals("Лисёнок");
        boolean cat = pet.name().equals("Кот-астронавт");
        g.box(0, 0.27, -0.04, 0.27, 0.25, 0.43, fur);
        g.box(0, 0.43, 0.21, 0.30, 0.27, 0.27, fur);
        g.box(0, 0.38, 0.37, cat ? 0.15 : 0.20, 0.10, cat ? 0.07 : 0.16, 0xFFDBD6CB);
        g.box(0, 0.41, cat ? 0.41 : 0.46, 0.065, 0.045, 0.025, 0xFF171B28);
        for (int side : new int[]{-1, 1}) {
            g.box(side * 0.10, 0.62, 0.19, 0.08, fox ? 0.22 : 0.15, 0.085, fur);
            g.box(side * 0.10, 0.63, 0.237, 0.044, 0.08, 0.012, trim);
            g.box(side * 0.085, 0.47, 0.352, 0.045, 0.042, 0.016, trim);
            g.box(side * 0.084, 0.478, 0.363, 0.014, 0.014, 0.008, 0xFFFFFFFF);
            for (int leg = -1; leg <= 1; leg += 2) {
                double swing = Math.sin(time * 5.0 + (side * leg > 0 ? 0 : Math.PI)) * 0.035;
                g.box(side * 0.10, 0.11, leg * 0.15 + swing, 0.085, 0.20, 0.095, fur);
                g.box(side * 0.10, 0.028, leg * 0.15 + swing + 0.018, 0.10, 0.055, 0.12, trim);
            }
        }
        double wag = Math.sin(time * 4) * 0.15;
        g.beam(new Vec3(0, 0.30, -0.24), new Vec3(wag, 0.45, -0.48), fox ? 0.14 : 0.075, fur);
        g.box(wag, 0.46, -0.49, fox ? 0.15 : 0.08, 0.11, 0.12, trim);
        g.box(0, 0.34, 0.115, 0.29, 0.055, 0.05, trim);
        if (pet.name().equals("Киберпёс")) {
            g.box(0, 0.405, -0.06, 0.16, 0.035, 0.24, 0xFF343E54);
            g.box(0, 0.43, -0.06, 0.045, 0.025, 0.18, trim);
            g.box(0.16, 0.47, 0.20, 0.035, 0.12, 0.16, 0xFF47556E);
        }
        if (cat) {
            g.box(0, 0.26, -0.25, 0.19, 0.23, 0.10, 0xFFD8E0F0);
            g.box(0, 0.51, 0.29, 0.34, 0.11, 0.15, 0xFFCDD8EB);
            g.box(0, 0.50, 0.375, 0.26, 0.07, 0.025, trim);
        }
    }

    private static void dragon(CosmeticGeometry g, Cosmetic pet, double time) {
        int scales = pet.primary();
        int glow = pet.secondary();
        double flap = Math.sin(time * 4) * 0.17;
        g.box(0, 0.29, -0.02, 0.24, 0.25, 0.48, scales);
        g.box(0, 0.27, 0.13, 0.16, 0.19, 0.09, CosmeticGeometry.shade(glow, 0.7));
        g.box(0, 0.47, 0.25, 0.28, 0.23, 0.28, scales);
        g.box(0, 0.42, 0.43, 0.22, 0.10, 0.16, scales);
        g.box(0, 0.385, 0.46, 0.18, 0.025, 0.13, glow);
        for (int side : new int[]{-1, 1}) {
            g.box(side * 0.105, 0.50, 0.395, 0.055, 0.038, 0.018, glow);
            g.beam(new Vec3(side * 0.09, 0.57, 0.17), new Vec3(side * 0.16, 0.76, 0.02), 0.055, 0xFFE4DCEB);
            g.box(side * 0.15, 0.12, 0.10, 0.09, 0.18, 0.14, scales);
            g.box(side * 0.15, 0.055, 0.15, 0.11, 0.05, 0.19, glow);
            for (int feather = 0; feather < 4; feather++) {
                double tipY = 0.60 + flap - feather * 0.11;
                Vec3 root = new Vec3(side * 0.10, 0.39, -0.04);
                Vec3 tip = new Vec3(side * (0.67 - feather * 0.07), tipY, -0.22 - feather * 0.065);
                g.beam(root, tip, 0.043, scales);
                g.beam(root.add(0, -0.035, 0), tip.add(0, -0.075, -0.015), 0.085,
                        CosmeticGeometry.shade(glow, 0.65 + feather * 0.08));
            }
        }
        Vec3 previous = new Vec3(0, 0.28, -0.22);
        for (int i = 0; i < 5; i++) {
            Vec3 next = new Vec3(Math.sin(time * 2 - i * 0.4) * i * 0.025, 0.25 + i * 0.018, -0.31 - i * 0.10);
            g.beam(previous, next, 0.10 - i * 0.015, scales);
            previous = next;
        }
        for (int i = 0; i < 4; i++) {
            g.box(0, 0.45 - i * 0.025, -0.06 - i * 0.12, 0.035, 0.09, 0.06, glow);
        }
    }

    private static void bird(CosmeticGeometry g, Cosmetic pet, double time) {
        int feathers = pet.primary();
        int bright = pet.secondary();
        g.box(0, 0.19, 0, 0.19, 0.27, 0.20, feathers);
        g.box(0, 0.29, 0.085, 0.14, 0.19, 0.08, bright);
        g.box(0, 0.41, 0.04, 0.22, 0.22, 0.21, feathers);
        g.box(0, 0.39, 0.18, 0.085, 0.10, 0.10, 0xFFFFC65A);
        g.box(0, 0.345, 0.21, 0.06, 0.055, 0.05, 0xFF3C2637);
        for (int side : new int[]{-1, 1}) {
            g.box(side * 0.09, 0.44, 0.151, 0.055, 0.055, 0.017, 0xFFF0F6FF);
            g.box(side * 0.09, 0.44, 0.163, 0.026, 0.033, 0.009, 0xFF101827);
            g.box(side * 0.06, 0.025, 0.06, 0.045, 0.035, 0.12, 0xFFD3AC77);
            double flutter = Math.sin(time * 3) * 0.025;
            g.beam(new Vec3(side * 0.10, 0.28, 0), new Vec3(side * (0.17 + flutter), 0.10, -0.08), 0.09, bright);
            g.beam(new Vec3(side * 0.04, 0.11, -0.07), new Vec3(side * 0.08, -0.15, -0.24), 0.055, bright);
        }
        for (int i = 0; i < 3; i++) {
            g.beam(new Vec3((i - 1) * 0.045, 0.51, 0.01),
                    new Vec3((i - 1) * 0.07, 0.62 + (i == 1 ? 0.05 : 0), -0.06), 0.038, bright);
        }
        if (pet.name().equals("Феникс")) {
            g.beam(new Vec3(0, 0.11, -0.1), new Vec3(0, -0.23, -0.34), 0.055, 0xFFFFE99B);
        }
    }

    private static void crystal(CosmeticGeometry g, Cosmetic pet, double time) {
        for (int i = 0; i < 3; i++) {
            g.box(0, 0.18 + i * 0.09, 0, 0.24 - Math.abs(i - 1) * 0.08, 0.09,
                    0.24 - Math.abs(i - 1) * 0.08, i == 1 ? pet.primary() : pet.secondary());
        }
        g.box(0, 0.18 + Math.sin(time * 2) * 0.04, 0, 0.09, 0.35, 0.09, pet.secondary());
    }
}
