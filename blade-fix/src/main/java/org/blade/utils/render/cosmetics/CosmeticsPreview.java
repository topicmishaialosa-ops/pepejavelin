package org.blade.utils.render.cosmetics;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.blade.feature.impl.visual.CosmeticsFeature.Cosmetic;
import org.blade.mixin.accessor.GuiGraphicsExtractorAccessor;
import org.blade.utils.render.gui.Render2DUtil;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.blade.utils.render.world.WorldMeshRenderer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Orthographic views of the actual model geometry, independently fitted to every tile. */
public final class CosmeticsPreview {
    private static final RenderPipeline ICON_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
            .withLocation(Identifier.parse("blade:pipeline/cosmetics_icon")).withCull(false).build();
    private CosmeticsPreview() {
    }

    public static void drawItem(GuiGraphicsExtractor gfx, Cosmetic cosmetic, float x, float y, float width,
                                float height, float yaw, boolean shoulderContext, float alpha,
                                ScreenRectangle scissor) {
        if (cosmetic == null || width < 2 || height < 2) {
            return;
        }
        var mesh = CosmeticsRenderer.buildItemMesh(cosmetic, System.currentTimeMillis() / 1000.0, shoulderContext);
        List<Quad> quads = new ArrayList<>();
        for (var rect : mesh.planeRects()) {
            Vec3 a = rect.axis().normalize().scale(rect.halfLength());
            Vec3 b = rect.normal().normalize().scale(rect.halfWidth());
            quads.add(new Quad(rect.center().subtract(a).subtract(b), rect.center().subtract(a).add(b),
                    rect.center().add(a).add(b), rect.center().add(a).subtract(b), rect.color()));
        }
        for (var ring : mesh.rings()) {
            for (int i = 0; i < ring.segments(); i++) {
                double a = i * Math.PI * 2 / ring.segments();
                double b = (i + 1) * Math.PI * 2 / ring.segments();
                quads.add(new Quad(radial(ring, a, -1), radial(ring, a, 1),
                        radial(ring, b, 1), radial(ring, b, -1), ring.color()));
            }
        }
        for (var line : mesh.lines()) {
            Vec3 delta = line.end().subtract(line.start());
            if (delta.lengthSqr() < 1.0E-8) continue;
            Vec3 helper = Math.abs(delta.normalize().y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 side = delta.cross(helper).normalize().scale(0.012 * line.widthScale());
            quads.add(new Quad(line.start().subtract(side), line.start().add(side),
                    line.end().add(side), line.end().subtract(side), line.startColor()));
        }
        if (quads.isEmpty()) return;

        Quaternionf rotation = new Quaternionf();
        if (cosmetic.category() == org.blade.feature.impl.visual.CosmeticsFeature.Category.SWORDS) {
            rotation.rotateZ(-0.35F);
        }
        rotation.rotateX(-0.18F).rotateY((float) Math.toRadians(yaw));
        List<Quad> rotated = new ArrayList<>(quads.size());
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Quad q : quads) {
            Quad view = new Quad(rotate(q.a, rotation), rotate(q.b, rotation),
                    rotate(q.c, rotation), rotate(q.d, rotation), q.color);
            rotated.add(view);
            for (Vec3 p : List.of(view.a, view.b, view.c, view.d)) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
        }
        // Large Z is the front of the original models. Paint the distant faces first.
        rotated.sort(Comparator.comparingDouble(q -> q.a.z + q.b.z + q.c.z + q.d.z));
        float fit = (float) Math.min(width / Math.max(0.05, maxX - minX), height / Math.max(0.05, maxY - minY)) * 0.86F;
        float centerX = (float) ((minX + maxX) * 0.5), centerY = (float) ((minY + maxY) * 0.5);
        Render2DUtil.flush();
        ((GuiGraphicsExtractorAccessor) gfx).getGuiRenderState().addGuiElement(
                new IconState(gfx, x, y, width, height, rotated, centerX, centerY, fit, alpha, scissor));
    }

    private static Vec3 radial(WorldMeshRenderer.Ring ring, double angle, int side) {
        double radius = Math.max(0, ring.radius() + side * ring.halfWidth());
        return ring.center().add(ring.u().scale(Math.cos(angle) * radius)).add(ring.v().scale(Math.sin(angle) * radius));
    }

    private static Vec3 rotate(Vec3 p, Quaternionf rotation) {
        Vector3f v = rotation.transform(new Vector3f((float) p.x, (float) p.y, (float) p.z));
        return new Vec3(v.x, v.y, v.z);
    }

    private record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
    }

    private static final class IconState implements GuiElementRenderState {
        private final List<Quad> quads;
        private final float x, y, centerX, centerY, fit, alpha;
        private final Matrix3x2f pose;
        private final ScreenRectangle scissor, bounds;

        IconState(GuiGraphicsExtractor gfx, float x, float y, float w, float h, List<Quad> quads,
                  float centerX, float centerY, float fit, float alpha, ScreenRectangle scissor) {
            this.pose = new Matrix3x2f(gfx.pose());
            this.scissor = scissor;
            this.bounds = new ScreenRectangle(Math.round(x), Math.round(y), Math.max(1, Math.round(w)),
                    Math.max(1, Math.round(h))).transformMaxBounds(pose);
            this.quads = quads;
            this.x = x + w * 0.5F;
            this.y = y + h * 0.5F;
            this.centerX = centerX;
            this.centerY = centerY;
            this.fit = fit;
            this.alpha = alpha;
        }

        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (Quad q : quads) {
                int color = q.color & 0xFFFFFF | Math.round((q.color >>> 24) * alpha) << 24;
                vertex(consumer, q.a, color);
                vertex(consumer, q.b, color);
                vertex(consumer, q.c, color);
                vertex(consumer, q.d, color);
            }
        }

        private void vertex(VertexConsumer consumer, Vec3 p, int color) {
            float px = x + ((float) p.x - centerX) * fit;
            float py = y - ((float) p.y - centerY) * fit;
            consumer.addVertex(pose.m00() * px + pose.m10() * py + pose.m20(),
                    pose.m01() * px + pose.m11() * py + pose.m21(), 0).setColor(color);
        }

        @Override
        public ScreenRectangle bounds() {
            return bounds;
        }

        @Override
        public ScreenRectangle scissorArea() {
            return scissor;
        }

        @Override
        public RenderPipeline pipeline() {
            return ICON_PIPELINE;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
