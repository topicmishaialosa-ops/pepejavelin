package org.blade.utils.render.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.blade.feature.impl.visual.CosmeticsFeature;
import org.blade.feature.impl.visual.CosmeticsFeature.Category;
import org.blade.feature.impl.visual.CosmeticsFeature.Cosmetic;
import org.blade.utils.render.Render3DUtil;
import org.blade.utils.render.world.WorldMeshRenderer;

import java.util.ArrayList;
import java.util.List;

public final class CosmeticsRenderer {
    private static final double[] WING_ANGLES = {52.0, 32.0, 12.0, -8.0, -28.0};
    private static final double[] WING_LENGTHS = {1.30, 1.18, 1.02, 0.86, 0.68};
    private static final double[] WING_WIDTHS = {0.13, 0.12, 0.105, 0.09, 0.07};

    private CosmeticsRenderer() {
    }

    public static void render(CosmeticsFeature feature, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || feature.equippedCount() == 0) {
            return;
        }

        List<WorldMeshRenderer.Line> lines = new ArrayList<>();
        List<WorldMeshRenderer.Ring> rings = new ArrayList<>();
        List<WorldMeshRenderer.PlaneRect> rects = new ArrayList<>();

        boolean firstPerson = mc.options.getCameraType().isFirstPerson();
        if (!firstPerson || feature.showFirstPerson()) {
            appendPlayer(feature, mc.player, tickDelta, lines, rings, rects);
        }
        if (feature.renderOthers()) {
            for (Player player : mc.level.players()) {
                if (player == mc.player || player.isRemoved() || !player.isAlive() || player.isSpectator()) {
                    continue;
                }
                appendPlayer(feature, player, tickDelta, lines, rings, rects);
            }
        }

        WorldMeshRenderer.renderCosmetics(new WorldMeshRenderer.WorldMesh(lines, rings, rects));
    }

    private static void appendPlayer(CosmeticsFeature feature, Player player, float tickDelta,
                                     List<WorldMeshRenderer.Line> lines,
                                     List<WorldMeshRenderer.Ring> rings,
                                     List<WorldMeshRenderer.PlaneRect> rects) {
        Vec3 base = Render3DUtil.interpolatedPosition(player, tickDelta);
        float bodyYaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
        double yaw = Math.toRadians(bodyYaw);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        float headYaw = Mth.rotLerp(tickDelta, player.yHeadRotO, player.yHeadRot);
        double headRadians = Math.toRadians(headYaw);
        double headCos = Math.cos(headRadians);
        double headSin = Math.sin(headRadians);
        double height = player.getBbHeight();
        double scale = feature.scaleValue();
        double time = System.currentTimeMillis() / 1000.0;

        appendParts(feature, base, headCos, headSin, cos, sin, height, scale, time, lines, rings, rects);
    }

    private static void appendParts(CosmeticsFeature feature, Vec3 base,
                                    double headCos, double headSin, double cos, double sin,
                                    double height, double scale, double time,
                                    List<WorldMeshRenderer.Line> lines,
                                    List<WorldMeshRenderer.Ring> rings,
                                    List<WorldMeshRenderer.PlaneRect> rects) {
        Cosmetic hat = feature.selected(Category.HATS);
        if (hat != null) {
            appendHat(hat, base, headCos, headSin, height, scale, lines, rings);
        }

        Cosmetic glasses = feature.selected(Category.GLASSES);
        if (glasses != null) {
            appendGlasses(glasses, base, headCos, headSin, height, scale, lines, rects);
        }

        Cosmetic decoration = feature.selected(Category.DECORATIONS);
        if (decoration != null && decoration.capeFile() == null) {
            appendDecoration(decoration, base, cos, sin, height, scale, lines, rings, rects);
        }

        Cosmetic wings = feature.selected(Category.WINGS);
        if (wings != null) {
            appendWings(wings, base, cos, sin, height, scale, time, rects);
        }

        Cosmetic body = feature.selected(Category.BODY);
        if (body != null) {
            appendBody(body, base, cos, sin, height, scale, rings, rects);
        }

        Cosmetic shoes = feature.selected(Category.SHOES);
        if (shoes != null) {
            appendShoes(shoes, base, cos, sin, scale, rings, rects);
        }

        Cosmetic pet = feature.selected(Category.PETS);
        if (pet != null) {
            appendPet(pet, base, cos, sin, height, scale, time, rings, rects);
        }

        // Weapons are real held-item models, replaced by ItemModelResolverMixin.
    }

    private static void appendHat(Cosmetic hat, Vec3 base, double cos, double sin, double height, double scale,
                                  List<WorldMeshRenderer.Line> lines,
                                  List<WorldMeshRenderer.Ring> rings) {
        if (hat.name().equals("Нимб")) {
            Vec3 center = point(base, cos, sin, 0.0, height + 0.30 * scale, 0.0);
            rings.add(new WorldMeshRenderer.Ring(
                    center,
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 0, 1),
                    0.30 * scale,
                    0.035 * scale,
                    alpha(hat.primary(), 170),
                    32
            ));
            return;
        }

        if (hat.name().equals("Корона")) {
            Vec3 center = point(base, cos, sin, 0.0, height + 0.16 * scale, 0.0);
            rings.add(new WorldMeshRenderer.Ring(
                    center,
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 0, 1),
                    0.21 * scale,
                    0.03 * scale,
                    alpha(hat.primary(), 190),
                    24
            ));
            for (int i = 0; i < 5; i++) {
                double angle = Math.PI * 2.0 * i / 5.0;
                double x = Math.cos(angle) * 0.19 * scale;
                double z = Math.sin(angle) * 0.19 * scale;
                lines.add(new WorldMeshRenderer.Line(
                        point(base, cos, sin, x, height + 0.17 * scale, z),
                        point(base, cos, sin, x, height + 0.38 * scale, z),
                        alpha(hat.primary(), 200)
                ));
            }
            return;
        }

        for (int side = -1; side <= 1; side += 2) {
            Vec3 start = point(base, cos, sin, side * 0.13 * scale, height - 0.06, 0.0);
            Vec3 middle = point(base, cos, sin, side * 0.23 * scale, height + 0.16 * scale, 0.0);
            Vec3 end = point(base, cos, sin, side * 0.19 * scale, height + 0.34 * scale, 0.0);
            lines.add(new WorldMeshRenderer.Line(start, middle, alpha(hat.primary(), 210)));
            lines.add(new WorldMeshRenderer.Line(middle, end, alpha(hat.primary(), 160)));
        }
    }

    private static void appendGlasses(Cosmetic glasses, Vec3 base, double cos, double sin, double height, double scale,
                                      List<WorldMeshRenderer.Line> lines,
                                      List<WorldMeshRenderer.PlaneRect> rects) {
        double eyeY = height - 0.22;
        double front = 0.29 * scale;
        boolean neon = glasses.name().equals("Неон");

        rects.add(new WorldMeshRenderer.PlaneRect(
                point(base, cos, sin, 0.0, eyeY, front),
                direction(cos, sin, 1, 0, 0),
                direction(cos, sin, 0, 1, 0),
                0.18 * scale,
                0.045 * scale,
                alpha(glasses.primary(), neon ? 215 : 195)
        ));

        if (neon) {
            lines.add(new WorldMeshRenderer.Line(
                    point(base, cos, sin, -0.18 * scale, eyeY, front - 0.10 * scale),
                    point(base, cos, sin, -0.20 * scale, eyeY, -0.02),
                    alpha(glasses.primary(), 200)
            ));
            lines.add(new WorldMeshRenderer.Line(
                    point(base, cos, sin, 0.18 * scale, eyeY, front - 0.10 * scale),
                    point(base, cos, sin, 0.20 * scale, eyeY, -0.02),
                    alpha(glasses.primary(), 200)
            ));
        }
    }

    private static void appendDecoration(Cosmetic decoration, Vec3 base, double cos, double sin, double height, double scale,
                                         List<WorldMeshRenderer.Line> lines,
                                         List<WorldMeshRenderer.Ring> rings,
                                         List<WorldMeshRenderer.PlaneRect> rects) {
        if (decoration.name().equals("Шарф")) {
            Vec3 center = point(base, cos, sin, 0.0, height - 0.32, 0.0);
            rings.add(new WorldMeshRenderer.Ring(
                    center,
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 0, 1),
                    0.20 * scale,
                    0.055 * scale,
                    alpha(decoration.primary(), 205),
                    24
            ));
            lines.add(new WorldMeshRenderer.Line(
                    point(base, cos, sin, 0.12 * scale, height - 0.34, 0.18 * scale),
                    point(base, cos, sin, 0.16 * scale, height - 0.62, 0.20 * scale),
                    alpha(decoration.primary(), 200)
            ));
            return;
        }

        if (decoration.name().equals("Цепь")) {
            lines.add(new WorldMeshRenderer.Line(
                    point(base, cos, sin, 0.0, height - 0.30, 0.18 * scale),
                    point(base, cos, sin, 0.0, height - 0.72, 0.20 * scale),
                    alpha(decoration.primary(), 220)
            ));
            rects.add(new WorldMeshRenderer.PlaneRect(
                    point(base, cos, sin, 0.0, height - 0.74, 0.21 * scale),
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 1, 0),
                    0.04 * scale,
                    0.04 * scale,
                    alpha(decoration.primary(), 230)
            ));
        }
    }

    private static void appendWings(Cosmetic wings, Vec3 base, double cos, double sin, double height, double scale,
                                    double time, List<WorldMeshRenderer.PlaneRect> rects) {
        double flap = Math.sin(time * 1.9) * 4.0;
        double rootY = height - 0.38;

        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < WING_ANGLES.length; i++) {
                double angle = Math.toRadians(WING_ANGLES[i] + flap);
                double halfLength = WING_LENGTHS[i] * scale * 0.5;
                double halfWidth = WING_WIDTHS[i] * scale;
                double rootX = side * 0.10 * scale;
                double x = rootX + side * Math.cos(angle) * halfLength;
                double y = rootY + Math.sin(angle) * halfLength;
                double z = -0.17 * scale - 0.06 * scale * (1.0 - (double) i / WING_ANGLES.length);

                rects.add(new WorldMeshRenderer.PlaneRect(
                        point(base, cos, sin, x, y, z),
                        direction(cos, sin, side * Math.cos(angle), Math.sin(angle), 0.0),
                        direction(cos, sin, -side * Math.sin(angle), Math.cos(angle), 0.0),
                        halfLength,
                        halfWidth,
                        alpha(wings.primary(), 200 - i * 28)
                ));
            }
        }
    }

    private static void appendBody(Cosmetic body, Vec3 base, double cos, double sin, double height, double scale,
                                   List<WorldMeshRenderer.Ring> rings,
                                   List<WorldMeshRenderer.PlaneRect> rects) {
        if (body.name().equals("Аура")) {
            rings.add(new WorldMeshRenderer.Ring(
                    point(base, cos, sin, 0.0, 0.92 * scale, 0.0),
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 0, 1),
                    0.46 * scale,
                    0.045 * scale,
                    alpha(body.primary(), 170),
                    32
            ));
            rings.add(new WorldMeshRenderer.Ring(
                    point(base, cos, sin, 0.0, 1.32 * scale, 0.0),
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 0, 1),
                    0.34 * scale,
                    0.03 * scale,
                    alpha(body.secondary(), 120),
                    28
            ));
            return;
        }

        for (int side = -1; side <= 1; side += 2) {
            rects.add(new WorldMeshRenderer.PlaneRect(
                    point(base, cos, sin, side * 0.34 * scale, height - 0.40, 0.0),
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 1, 0),
                    0.16 * scale,
                    0.10 * scale,
                    alpha(body.primary(), 190)
            ));
        }
    }

    private static void appendShoes(Cosmetic shoes, Vec3 base, double cos, double sin, double scale,
                                    List<WorldMeshRenderer.Ring> rings,
                                    List<WorldMeshRenderer.PlaneRect> rects) {
        for (int side = -1; side <= 1; side += 2) {
            rects.add(new WorldMeshRenderer.PlaneRect(
                    point(base, cos, sin, side * 0.16 * scale, 0.07 * scale, 0.05 * scale),
                    direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 1, 0),
                    0.13 * scale,
                    0.055 * scale,
                    alpha(shoes.primary(), 210)
            ));
        }
        rings.add(new WorldMeshRenderer.Ring(
                point(base, cos, sin, 0.0, 0.05 * scale, 0.18 * scale),
                direction(cos, sin, 1, 0, 0),
                direction(cos, sin, 0, 0, 1),
                0.22 * scale,
                0.02 * scale,
                alpha(shoes.secondary(), 120),
                24
        ));
    }

    private static void appendPet(Cosmetic pet, Vec3 base, double cos, double sin, double height, double scale,
                                  double time, List<WorldMeshRenderer.Ring> rings,
                                  List<WorldMeshRenderer.PlaneRect> rects) {
        if (!pet.name().equals("Кристалл") && !pet.name().equals("Сфера")) {
            boolean shoulder = CosmeticPets.onShoulder(pet);
            boolean dragon = pet.name().contains("дракон") || pet.name().contains("Дракон");
            double bob = shoulder ? 0 : Math.sin(time * 2.2) * 0.045;
            Vec3 anchor = shoulder
                    ? point(base, cos, sin, -0.36, height - 0.40, 0.01)
                    : point(base, cos, sin, 0.88 * scale, dragon ? height * 0.48 + bob : 0.08 + bob, -0.34 * scale);
            CosmeticPets.append(pet, anchor, Math.atan2(sin, cos), scale * (shoulder ? 0.62 : 0.85), time, rects);
            return;
        }
        double orbit = time * 1.7;
        double x = Math.cos(orbit) * 0.85 * scale;
        double z = Math.sin(orbit) * 0.85 * scale;
        double y = height * 0.62 + Math.sin(time * 2.0) * 0.08;
        Vec3 center = point(base, cos, sin, x, y, z);

        if (pet.name().equals("Сфера")) {
            rings.add(new WorldMeshRenderer.Ring(center, direction(cos, sin, 1, 0, 0),
                    direction(cos, sin, 0, 1, 0), 0.16 * scale, 0.025 * scale, pet.primary(), 32));
            rings.add(new WorldMeshRenderer.Ring(center, direction(cos, sin, 0, 0, 1),
                    direction(cos, sin, 0, 1, 0), 0.16 * scale, 0.025 * scale, pet.primary(), 32));
            return;
        }
        CosmeticPets.append(pet, center, time * 0.6, scale, time, rects);
    }

    public static WorldMeshRenderer.WorldMesh buildItemMesh(Cosmetic cosmetic, double time, boolean shoulderContext) {
        List<WorldMeshRenderer.Line> lines = new ArrayList<>();
        List<WorldMeshRenderer.Ring> rings = new ArrayList<>();
        List<WorldMeshRenderer.PlaneRect> rects = new ArrayList<>();
        switch (cosmetic.category()) {
            case HATS -> appendHat(cosmetic, Vec3.ZERO, 1, 0, 1.8, 1, lines, rings);
            case GLASSES -> appendGlasses(cosmetic, Vec3.ZERO, 1, 0, 1.8, 1, lines, rects);
            case WINGS -> appendWings(cosmetic, Vec3.ZERO, 1, 0, 1.8, 1, time, rects);
            case BODY -> appendBody(cosmetic, Vec3.ZERO, 1, 0, 1.8, 1, rings, rects);
            case SHOES -> appendShoes(cosmetic, Vec3.ZERO, 1, 0, 1, rings, rects);
            case DECORATIONS -> {
                if (cosmetic.capeFile() == null) {
                    appendDecoration(cosmetic, Vec3.ZERO, 1, 0, 1.8, 1, lines, rings, rects);
                } else {
                    CosmeticGeometry g = new CosmeticGeometry(rects, Vec3.ZERO, 0, 1);
                    g.box(0, 0.6, 0, 0.63, 1.05, 0.035, cosmetic.primary());
                    for (int i = -2; i <= 2; i++) {
                        g.box(i * 0.10, 0.6, 0.025, 0.015, 0.92, 0.01, cosmetic.secondary());
                    }
                }
            }
            case PETS -> {
                if (cosmetic.name().equals("Сфера")) {
                    rings.add(new WorldMeshRenderer.Ring(Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                            0.16, 0.025, cosmetic.primary(), 32));
                    rings.add(new WorldMeshRenderer.Ring(Vec3.ZERO, new Vec3(0, 0, 1), new Vec3(0, 1, 0),
                            0.16, 0.025, cosmetic.primary(), 32));
                } else {
                    CosmeticPets.append(cosmetic, Vec3.ZERO, 0, CosmeticPets.onShoulder(cosmetic) ? 0.62 : 1, time, rects);
                    if (shoulderContext && CosmeticPets.onShoulder(cosmetic)) {
                        CosmeticGeometry g = new CosmeticGeometry(rects, Vec3.ZERO, 0, 1);
                        // A cropped upper arm, torso and neck make the shoulder placement explicit.
                        g.box(0, -0.20, 0, 0.24, 0.44, 0.31, 0xFF40506B);
                        g.box(0.28, -0.22, 0, 0.33, 0.48, 0.30, 0xFF35435B);
                        g.box(0.30, 0.075, 0, 0.18, 0.13, 0.19, 0xFFD6B396);
                        g.box(0, -0.12, 0.164, 0.245, 0.025, 0.012, 0xFF8395B9);
                    }
                }
            }
            case SWORDS -> CosmeticWeapons.append(cosmetic, Vec3.ZERO, 0, 1, rects);
        }
        return new WorldMeshRenderer.WorldMesh(lines, rings, rects);
    }

    private static Vec3 point(Vec3 base, double cos, double sin, double x, double y, double z) {
        return new Vec3(base.x + x * cos - z * sin, base.y + y, base.z + x * sin + z * cos);
    }

    private static Vec3 direction(double cos, double sin, double x, double y, double z) {
        return new Vec3(x * cos - z * sin, y, x * sin + z * cos);
    }

    private static int alpha(int color, int alpha) {
        return (color & 0xFFFFFF) | ((alpha & 0xFF) << 24);
    }
}
