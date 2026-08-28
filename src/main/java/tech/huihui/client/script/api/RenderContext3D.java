package tech.huihui.client.script.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * 3D-рендер контекст, передаётся в колбэки onRender3D.
 */
public class RenderContext3D {
    private final MatrixStack matrix;
    private final float partial;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public RenderContext3D(MatrixStack matrix, float partial) {
        this.matrix = matrix;
        this.partial = partial;
    }

    public MatrixStack matrix() {
        return matrix;
    }

    public float partialTicks() {
        return partial;
    }

    public MinecraftClient mc() {
        return mc;
    }

    private Vec3d camera() {
        return mc.gameRenderer.getCamera().getPos();
    }

    public void line3D(Vec3d a, Vec3d b, int color) {
        Vec3d cam = camera();
        MatrixStack ms = matrix;
        ms.push();
        Matrix4f pos = ms.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        bb.vertex(pos, (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z)).color(color);
        bb.vertex(pos, (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z)).color(color);
        BufferRenderer.drawWithGlobalProgram(bb.end());
        ms.pop();
    }

    public void box(Vec3d min, Vec3d max, int color) {
        Vec3d cam = camera();
        MatrixStack ms = matrix;
        ms.push();
        Matrix4f pos = ms.peek().getPositionMatrix();
        BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        double x1 = min.x - cam.x, y1 = min.y - cam.y, z1 = min.z - cam.z;
        double x2 = max.x - cam.x, y2 = max.y - cam.y, z2 = max.z - cam.z;
        edge(bb, pos, x1, y1, z1, x2, y1, z1, color);
        edge(bb, pos, x2, y1, z1, x2, y2, z1, color);
        edge(bb, pos, x2, y2, z1, x1, y2, z1, color);
        edge(bb, pos, x1, y2, z1, x1, y1, z1, color);
        edge(bb, pos, x1, y1, z2, x2, y1, z2, color);
        edge(bb, pos, x2, y1, z2, x2, y2, z2, color);
        edge(bb, pos, x2, y2, z2, x1, y2, z2, color);
        edge(bb, pos, x1, y2, z2, x1, y1, z2, color);
        edge(bb, pos, x1, y1, z1, x1, y1, z2, color);
        edge(bb, pos, x2, y1, z1, x2, y1, z2, color);
        edge(bb, pos, x1, y2, z1, x1, y2, z2, color);
        edge(bb, pos, x2, y2, z1, x2, y2, z2, color);
        BufferRenderer.drawWithGlobalProgram(bb.end());
        ms.pop();
    }

    private static void edge(BufferBuilder bb, Matrix4f pos, double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        bb.vertex(pos, (float) x1, (float) y1, (float) z1).color(color);
        bb.vertex(pos, (float) x2, (float) y2, (float) z2).color(color);
    }
}
