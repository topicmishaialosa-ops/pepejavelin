package org.blade.utils.render.gui;

import net.minecraft.resources.Identifier;

public final class PlayerHead {
    private PlayerHead() {
    }

    public static void draw(float x, float y, float size, Identifier skin, float radius, int tint) {
        draw(x, y, size, skin, radius, tint, 0.0F, 0.0F);
    }

    public static void draw(float x, float y, float size, Identifier skin, float radius, int tint,
                            float yawDeg, float pitchDeg) {
        float offset = normalize360(yawDeg) / 360.0F * 32.0F;
        float scaleY = Math.max(0.35F, (float) Math.cos(Math.toRadians(pitchDeg)));
        float drawH = size * scaleY;
        float drawY = y + (size - drawH) / 2.0F;
        drawLayer(skin, x, drawY, size, drawH, radius, tint, 0.0F, 32.0F, 8.0F, offset);
        drawLayer(skin, x, drawY, size, drawH, radius, tint, 32.0F, 64.0F, 40.0F, offset);
    }

    public static float pitchScale(float pitchDeg) {
        return Math.max(0.35F, (float) Math.cos(Math.toRadians(pitchDeg)));
    }

    public static float facing(float yawDeg) {
        return (float) Math.cos(Math.toRadians(normalize180(yawDeg)));
    }

    private static void drawLayer(Identifier skin, float x, float y, float w, float h, float radius,
                                  int tint, float ringStart, float ringEnd, float front, float offset) {
        float len = ringEnd - ringStart;
        float u0 = ringStart + mod(front - ringStart + offset, len);
        float u1 = u0 + 8.0F;
        if (u1 <= ringEnd + 0.001F) {
            drawTex(skin, x, y, w, h, u0, u1, radius, tint);
            return;
        }
        float frac = (ringEnd - u0) / 8.0F;
        float splitW = w * frac;
        drawTex(skin, x, y, splitW, h, u0, ringEnd, 0.0F, tint);
        drawTex(skin, x + splitW, y, w - splitW, h, ringStart, u1 - len, 0.0F, tint);
    }

    private static void drawTex(Identifier skin, float x, float y, float w, float h,
                                float u0, float u1, float radius, int tint) {
        Render2DUtil.texture(x, y, w, h, skin)
                .managed()
                .uv(u0 / 64.0F, 8.0F / 64.0F, u1 / 64.0F, 16.0F / 64.0F)
                .radius(radius)
                .color(tint)
                .draw();
    }

    private static float mod(float v, float len) {
        return ((v % len) + len) % len;
    }

    private static float normalize360(float deg) {
        return mod(deg, 360.0F);
    }

    private static float normalize180(float deg) {
        float v = normalize360(deg);
        return v > 180.0F ? v - 360.0F : v;
    }
}
