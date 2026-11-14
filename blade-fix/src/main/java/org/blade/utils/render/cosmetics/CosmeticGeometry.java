package org.blade.utils.render.cosmetics;

import net.minecraft.world.phys.Vec3;
import org.blade.utils.render.world.WorldMeshRenderer;

import java.util.List;

/** Small, shaded solid parts shared by the catalogue previews and the world models. */
final class CosmeticGeometry {
    private final List<WorldMeshRenderer.PlaneRect> faces;
    private final Vec3 origin;
    private final double cos;
    private final double sin;
    private final double scale;

    CosmeticGeometry(List<WorldMeshRenderer.PlaneRect> faces, Vec3 origin, double yaw, double scale) {
        this.faces = faces;
        this.origin = origin;
        this.cos = Math.cos(yaw);
        this.sin = Math.sin(yaw);
        this.scale = scale;
    }

    void box(double x, double y, double z, double width, double height, double depth, int color) {
        solid(new Vec3(x, y, z), new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1),
                width * 0.5, height * 0.5, depth * 0.5, color);
    }

    void beam(Vec3 start, Vec3 end, double width, int color) {
        Vec3 up = end.subtract(start).normalize();
        Vec3 right = up.cross(Math.abs(up.y) > 0.9 ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0)).normalize();
        solid(start.add(end).scale(0.5), right, up, right.cross(up).normalize(),
                width * 0.5, start.distanceTo(end) * 0.5, width * 0.5, color);
    }

    private void solid(Vec3 center, Vec3 right, Vec3 up, Vec3 forward,
                       double w, double h, double d, int color) {
        face(center.add(up.scale(h)), right, forward, w, d, shade(color, 1.12));
        face(center.subtract(up.scale(h)), right, forward, w, d, shade(color, 0.55));
        face(center.add(right.scale(w)), up, forward, h, d, shade(color, 0.76));
        face(center.subtract(right.scale(w)), up, forward, h, d, shade(color, 0.66));
        face(center.add(forward.scale(d)), right, up, w, h, color | 0xFF000000);
        face(center.subtract(forward.scale(d)), right, up, w, h, shade(color, 0.82));
    }

    private void face(Vec3 center, Vec3 a, Vec3 b, double halfA, double halfB, int color) {
        faces.add(new WorldMeshRenderer.PlaneRect(point(center), direction(a), direction(b),
                halfA * scale, halfB * scale, color));
    }

    private Vec3 point(Vec3 p) {
        return origin.add((p.x * cos - p.z * sin) * scale, p.y * scale,
                (p.x * sin + p.z * cos) * scale);
    }

    private Vec3 direction(Vec3 p) {
        return new Vec3(p.x * cos - p.z * sin, p.y, p.x * sin + p.z * cos);
    }

    static int shade(int color, double factor) {
        int r = Math.min(255, (int) (((color >>> 16) & 255) * factor));
        int g = Math.min(255, (int) (((color >>> 8) & 255) * factor));
        int b = Math.min(255, (int) ((color & 255) * factor));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
