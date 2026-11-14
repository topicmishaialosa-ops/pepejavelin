package org.blade.utils.render.cosmetics;

import com.google.gson.JsonParser;
import org.blade.feature.impl.visual.CosmeticsFeature;
import org.blade.feature.impl.visual.CosmeticsFeature.Category;
import org.blade.feature.impl.visual.CosmeticsFeature.Cosmetic;
import org.blade.utils.render.world.WorldMeshRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CosmeticsModelsTest {
    @Test
    void cataloguePreviewRendererInitializesWithoutInvalidPipelineIdentifiers() {
        assertDoesNotThrow(() -> Class.forName(CosmeticsPreview.class.getName()));
    }

    @Test
    void everyCatalogueEntryHasFiniteIsolatedGeometryWithoutChangingEquipment() {
        CosmeticsFeature feature = new CosmeticsFeature();
        feature.selection(Category.PETS).setValue("Киберпёс");
        for (Category category : Category.values()) {
            for (Cosmetic item : feature.cosmetics(category)) {
                var mesh = CosmeticsRenderer.buildItemMesh(item, 0.5, CosmeticPets.onShoulder(item));
                assertFalse(mesh.isEmpty(), item.name());
                for (var rect : mesh.planeRects()) {
                    assertTrue(Double.isFinite(rect.center().lengthSqr()), item.name());
                    assertTrue(rect.halfWidth() > 0 && rect.halfLength() > 0, item.name());
                    assertEquals(1, rect.axis().length(), 1.0E-6);
                    assertEquals(1, rect.normal().length(), 1.0E-6);
                    assertEquals(0, rect.axis().dot(rect.normal()), 1.0E-6);
                }
            }
        }
        assertEquals("Киберпёс", feature.selected(Category.PETS).name());
        assertEquals(1, feature.equippedCount());
    }

    @Test
    void weaponItemResourcesMatchPreviewPartsAndLegalMinecraftElementBounds() throws Exception {
        CosmeticsFeature feature = new CosmeticsFeature();
        for (Cosmetic item : feature.cosmetics(Category.SWORDS)) {
            String name = CosmeticWeapons.modelName(item);
            var model = resource("models/item/cosmetics/" + name + ".json");
            var elements = model.getAsJsonArray("elements");
            var mesh = CosmeticsRenderer.buildItemMesh(item, 0, false);
            assertEquals(elements.size() * 6, mesh.planeRects().size(), item.name());
            var definition = resource("items/cosmetics/" + name + ".json");
            assertEquals("blade:item/cosmetics/" + name,
                    definition.getAsJsonObject("model").get("model").getAsString());
            for (var element : elements) {
                var object = element.getAsJsonObject();
                var from = object.getAsJsonArray("from");
                var to = object.getAsJsonArray("to");
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(from.get(axis).getAsDouble() >= -16);
                    assertTrue(to.get(axis).getAsDouble() <= 32);
                    assertTrue(to.get(axis).getAsDouble() > from.get(axis).getAsDouble());
                }
            }
        }
        try (var stream = getClass().getResourceAsStream("/assets/blade/textures/item/cosmetics/forged_palette.png")) {
            assertNotNull(stream);
            var image = ImageIO.read(stream);
            assertEquals(64, image.getWidth());
            assertEquals(16, image.getHeight());
        }
    }

    @Test
    void customCompanionsHaveDistinctSolidAnimatedModelsAndShoulderPlacement() {
        CosmeticsFeature feature = new CosmeticsFeature();
        List<Integer> signatures = new ArrayList<>();
        for (Cosmetic item : feature.cosmetics(Category.PETS)) {
            if (item.name().equals("Сфера") || item.name().equals("Кристалл")) continue;
            var resting = CosmeticsRenderer.buildItemMesh(item, 0, false);
            var animated = CosmeticsRenderer.buildItemMesh(item, 1, false);
            assertTrue(resting.planeRects().size() >= 100, item.name());
            assertNotEquals(resting, animated, item.name());
            int signature = resting.planeRects().hashCode();
            assertFalse(signatures.contains(signature), item.name());
            signatures.add(signature);
            if (CosmeticPets.onShoulder(item)) {
                var shoulder = CosmeticsRenderer.buildItemMesh(item, 0, true);
                assertTrue(shoulder.planeRects().size() > resting.planeRects().size());
            }
        }
        assertEquals(8, signatures.size());
    }

    @Test
    void exportActualGeometryContactSheetForVisualReview() throws Exception {
        CosmeticsFeature feature = new CosmeticsFeature();
        List<Cosmetic> items = new ArrayList<>(feature.cosmetics(Category.PETS).subList(2, 10));
        items.addAll(feature.cosmetics(Category.SWORDS));
        int cellW = 280, cellH = 245;
        BufferedImage image = new BufferedImage(cellW * 4, cellH * 4, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x11141D));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (int index = 0; index < items.size(); index++) {
            Cosmetic item = items.get(index);
            int x = (index % 4) * cellW, y = (index / 4) * cellH;
            g.setColor(new Color(0x1B202C));
            g.fillRoundRect(x + 8, y + 8, cellW - 16, cellH - 16, 14, 14);
            var mesh = CosmeticsRenderer.buildItemMesh(item, 0.4, CosmeticPets.onShoulder(item));
            List<Face> faces = new ArrayList<>();
            double minX = 1E9, maxX = -1E9, minY = 1E9, maxY = -1E9;
            for (var rect : mesh.planeRects()) {
                var a = rect.axis().scale(rect.halfLength());
                var b = rect.normal().scale(rect.halfWidth());
                var c = rect.center();
                var points = List.of(c.subtract(a).subtract(b), c.subtract(a).add(b),
                        c.add(a).add(b), c.add(a).subtract(b));
                double[][] projected = new double[4][3];
                double depth = 0;
                for (int i = 0; i < 4; i++) {
                    var p = points.get(i);
                    double yaw = Math.toRadians(-25), pitch = -0.18;
                    double px = p.x * Math.cos(yaw) + p.z * Math.sin(yaw);
                    double pz = -p.x * Math.sin(yaw) + p.z * Math.cos(yaw);
                    double py = p.y * Math.cos(pitch) - pz * Math.sin(pitch);
                    pz = p.y * Math.sin(pitch) + pz * Math.cos(pitch);
                    if (item.category() == Category.SWORDS) {
                        double angledX = px * Math.cos(-0.35) - py * Math.sin(-0.35);
                        py = px * Math.sin(-0.35) + py * Math.cos(-0.35);
                        px = angledX;
                    }
                    projected[i] = new double[]{px, py, pz};
                    minX = Math.min(minX, px); maxX = Math.max(maxX, px);
                    minY = Math.min(minY, py); maxY = Math.max(maxY, py);
                    depth += pz;
                }
                faces.add(new Face(projected, depth, rect.color()));
            }
            faces.sort(Comparator.comparingDouble(Face::depth));
            double fit = Math.min(230 / (maxX - minX), 177 / (maxY - minY)) * 0.86;
            for (Face face : faces) {
                Path2D polygon = new Path2D.Double();
                for (int i = 0; i < 4; i++) {
                    double px = x + 140 + (face.points[i][0] - (minX + maxX) * 0.5) * fit;
                    double py = y + 105 - (face.points[i][1] - (minY + maxY) * 0.5) * fit;
                    if (i == 0) polygon.moveTo(px, py); else polygon.lineTo(px, py);
                }
                polygon.closePath();
                g.setColor(new Color(face.color, true));
                g.fill(polygon);
            }
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(0xF1F1F7));
            g.drawString(item.name(), x + 18, y + 216);
        }
        g.dispose();
        Path output = Path.of("build/reports/cosmetics/catalogue.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
        assertTrue(Files.size(output) > 1000);
    }

    private com.google.gson.JsonObject resource(String file) throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/blade/" + file)) {
            assertNotNull(stream, file);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private record Face(double[][] points, double depth, int color) {
    }
}
