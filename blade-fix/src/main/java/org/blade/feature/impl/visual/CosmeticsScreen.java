package org.blade.feature.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.blade.context.RenderContext;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.visual.CosmeticsFeature.Category;
import org.blade.feature.impl.visual.CosmeticsFeature.Cosmetic;
import org.blade.feature.impl.visual.CosmeticsFeature.SwordTarget;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.cosmetics.CosmeticPets;
import org.blade.utils.render.cosmetics.CosmeticsPreview;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.TextAlign;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.util.List;

public final class CosmeticsScreen extends Screen {
    private static final int TEXT = 0xFFF1F1F7;
    private static final int DIM = 0xFF999DAD;
    private static final int FAINT = 0xFF656B7E;
    private final Screen parent;
    private Category category = Category.HATS;
    private Cosmetic focused;
    private float scroll;
    private float openAnim;
    private float mouseX, mouseY;
    private float previewYaw = -25;
    private boolean draggingPreview;
    private float lastDragX;

    public CosmeticsScreen(Screen parent) {
        super(Component.literal("Косметика"));
        this.parent = parent;
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof CosmeticsScreen)) {
            mc.gui.setScreen(new CosmeticsScreen(mc.gui.screen()));
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static CosmeticsFeature feature() {
        return FeatureManager.INSTANCE.getFeature(CosmeticsFeature.class);
    }

    private record Layout(float u, float x, float y, float w, float h, float gridX, float gridY,
                          float gridW, float gridH, float panelX, float panelW, float cardW) {
        float cardH() { return 94 * u; }
        float rowH() { return 102 * u; }
        float panelY() { return y + 46 * u; }
        float panelH() { return h - 58 * u; }
        ScreenRectangle clip() {
            return new ScreenRectangle(Math.round(gridX), Math.round(gridY), Math.max(1, Math.round(gridW)),
                    Math.max(1, Math.round(gridH)));
        }
    }

    private Layout layout() {
        float u = Math.min(width / 640.0F, height / 360.0F);
        float w = 580 * u, h = 320 * u;
        float x = (width - w) * 0.5F, y = (height - h) * 0.5F;
        float gridW = 288 * u;
        return new Layout(u, x, y, w, h, x + 116 * u, y + 65 * u, gridW, h - 85 * u,
                x + w - 164 * u, 152 * u, (gridW - 8 * u) * 0.5F);
    }

    private int ac(int color) {
        return ColorUtil.multiplyAlpha(color, openAnim);
    }

    private void rect(float x, float y, float w, float h, int color, float radius) {
        Render2DUtil.rect(x, y, w, h).color(ac(color)).radius(radius).draw();
    }

    private void border(float x, float y, float w, float h, int color, float radius) {
        Render2DUtil.rect(x, y, w, h).color(0).radius(radius).border(1, ac(color)).draw();
    }

    private void text(float x, float y, float size, String text, int color, boolean bold) {
        Render2DUtil.text(x, y, size, text).color(ac(color))
                .style(bold ? UiFontStyle.SEMIBOLD : UiFontStyle.REGULAR).draw();
    }

    private void center(float x, float y, float size, String text, int color) {
        Render2DUtil.text(x, y, size, text).color(ac(color)).style(UiFontStyle.MEDIUM)
                .align(TextAlign.CENTER).draw();
    }

    private static boolean inside(float x, float y, float w, float h, double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float partialTick) {
        openAnim += (1 - openAnim) * 0.24F;
        if (openAnim > 0.995F) openAnim = 1;
        mouseX = mx;
        mouseY = my;
        gfx.fill(0, 0, width, height, ac(0xD9090B12));
        Minecraft mc = Minecraft.getInstance();
        RenderContext.enter2D(mc.gui, gfx, null);
        try {
            Render2DUtil.beginFrame();
            Layout l = layout();
            float u = l.u;
            Render2DUtil.rect(l.x, l.y, l.w, l.h).color(ac(0xFF11141D)).radius(10 * u)
                    .shadow(ac(0x99000000), 22 * u).draw();
            border(l.x, l.y, l.w, l.h, 0x22A0ADCF, 10 * u);
            rect(l.x + 104 * u, l.y + 35 * u, l.w - 104 * u, u, 0x12FFFFFF, 0);
            sidebar(l);
            header(l);
            grid(gfx, l);
            panel(gfx, l);
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }

    private void sidebar(Layout l) {
        float u = l.u;
        rect(l.x + 8 * u, l.y + 8 * u, 88 * u, 26 * u, 0xFF1C2130, 6 * u);
        text(l.x + 18 * u, l.y + 13 * u, 6.4F * u, "BLADE", Theme.getAccent(), true);
        text(l.x + 18 * u, l.y + 23 * u, 3.7F * u, "COSMETIC STUDIO", DIM, false);
        int i = 0;
        CosmeticsFeature f = feature();
        for (Category c : Category.values()) {
            float y = l.y + (47 + i++ * 28) * u;
            boolean active = category == c;
            boolean hover = inside(l.x + 8 * u, y, 88 * u, 24 * u, mouseX, mouseY);
            if (active || hover) {
                rect(l.x + 8 * u, y, 88 * u, 24 * u,
                        active ? ColorUtil.withAlpha(Theme.getAccent(), 35) : 0x0CFFFFFF, 5 * u);
            }
            if (active) rect(l.x + 8 * u, y + 6 * u, 2 * u, 12 * u, Theme.getAccent(), u);
            text(l.x + 18 * u, y + 9 * u, 5.2F * u,
                    c == Category.SWORDS ? "Мечи и катаны" : c.displayName(), active ? TEXT : DIM, active);
            if (f != null && f.selected(c) != null) {
                rect(l.x + 87 * u, y + 10 * u, 3 * u, 3 * u, Theme.getAccent(), 2 * u);
            }
        }
        text(l.x + 17 * u, l.y + l.h - 25 * u, 4 * u, "Твоя коллекция", FAINT, false);
        text(l.x + 17 * u, l.y + l.h - 15 * u, 3.7F * u, "Создай свой образ", DIM, false);
    }

    private void header(Layout l) {
        float u = l.u;
        text(l.gridX, l.y + 14 * u, 8 * u, "Косметика", TEXT, true);
        CosmeticsFeature f = feature();
        text(l.gridX + 100 * u, l.y + 18 * u, 4.5F * u,
                (f == null ? 0 : f.equippedCount()) + " надето", DIM, false);
        float closeX = l.x + l.w - 27 * u;
        if (inside(closeX, l.y + 8 * u, 19 * u, 19 * u, mouseX, mouseY)) {
            rect(closeX, l.y + 8 * u, 19 * u, 19 * u, 0x17FFFFFF, 5 * u);
        }
        center(closeX + 9.5F * u, l.y + 14 * u, 6 * u, "x", DIM);
        text(l.gridX, l.y + 47 * u, 5.6F * u,
                category == Category.SWORDS ? "Мечи и катаны" : category.displayName(), TEXT, true);
        if (f != null) {
            String count = f.cosmetics(category).size() + " вариантов";
            text(l.gridX + l.gridW - 57 * u, l.y + 49 * u, 4 * u, count, FAINT, false);
        }
    }

    private float maxScroll(Layout l) {
        CosmeticsFeature f = feature();
        int count = f == null ? 0 : f.cosmetics(category).size() + 1;
        return Math.max(0, ((count + 1) / 2) * l.rowH() - 8 * l.u - l.gridH);
    }

    private void grid(GuiGraphicsExtractor gfx, Layout l) {
        CosmeticsFeature f = feature();
        if (f == null) return;
        scroll = Math.max(0, Math.min(scroll, maxScroll(l)));
        List<Cosmetic> items = f.cosmetics(category);
        Render2DUtil.pushScissor(l.gridX, l.gridY, l.gridW, l.gridH);
        try {
            for (int i = 0; i <= items.size(); i++) {
                float x = l.gridX + (i % 2) * (l.cardW + 8 * l.u);
                float y = l.gridY + (i / 2) * l.rowH() - scroll;
                if (y + l.cardH() <= l.gridY || y >= l.gridY + l.gridH) continue;
                Cosmetic item = i == 0 ? null : items.get(i - 1);
                boolean selected = item == null ? f.selected(category) == null : item.equals(f.selected(category));
                boolean hover = inside(x, y, l.cardW, l.cardH(), mouseX, mouseY)
                        && inside(l.gridX, l.gridY, l.gridW, l.gridH, mouseX, mouseY);
                if (hover && item != null && !draggingPreview) focused = item;
                card(gfx, l, x, y, item, selected, hover);
            }
            Render2DUtil.flush();
        } finally {
            Render2DUtil.popScissor();
        }
        if (maxScroll(l) > 0) {
            float track = l.gridH;
            float thumb = Math.max(22 * l.u, track * track / (track + maxScroll(l)));
            rect(l.gridX + l.gridW + 3 * l.u, l.gridY, 2 * l.u, track, 0x10FFFFFF, l.u);
            rect(l.gridX + l.gridW + 3 * l.u, l.gridY + (track - thumb) * scroll / maxScroll(l),
                    2 * l.u, thumb, ColorUtil.withAlpha(Theme.getAccent(), 150), l.u);
            text(l.gridX, l.y + l.h - 12 * l.u, 3.7F * l.u, "Колесо мыши - листать", FAINT, false);
        }
    }

    private void card(GuiGraphicsExtractor gfx, Layout l, float x, float y, Cosmetic item,
                      boolean selected, boolean hover) {
        float u = l.u, w = l.cardW, h = l.cardH();
        int accent = item == null ? Theme.getAccent() : item.secondary() | 0xFF000000;
        rect(x, y, w, h, selected ? 0xFF252B3D : hover ? 0xFF242A38 : 0xFF1B202C, 7 * u);
        border(x, y, w, h, selected ? ColorUtil.withAlpha(accent, 160) : 0x12FFFFFF, 7 * u);
        rect(x + 5 * u, y + 5 * u, w - 10 * u, 62 * u, ColorUtil.withAlpha(accent, 12), 5 * u);
        // A subtle plinth keeps very thin accessories legible against the tile.
        rect(x + w * 0.25F, y + 58 * u, w * 0.5F, 4 * u, 0x18000000, 2 * u);
        if (item != null) {
            CosmeticsPreview.drawItem(gfx, item, x + 9 * u, y + 7 * u, w - 18 * u, 57 * u,
                    -25, CosmeticPets.onShoulder(item), openAnim, l.clip());
        } else {
            center(x + w * 0.5F, y + 24 * u, 15 * u, "-", FAINT);
            center(x + w * 0.5F, y + 48 * u, 3.8F * u, "БЕЗ КОСМЕТИКИ", FAINT);
        }
        String name = item == null ? "Снять" : item.name();
        text(x + 9 * u, y + 73 * u, 5.2F * u, fitText(name, 5.2F * u, w - 18 * u), TEXT, true);
        text(x + 9 * u, y + 84 * u, 3.6F * u,
                selected ? item == null ? "СНЯТО" : "НАДЕТО" : item == null ? "Очистить категорию" : badge(item),
                selected ? accent : DIM, false);
        if (selected) rect(x + w - 12 * u, y + 9 * u, 4 * u, 4 * u, accent, 2 * u);
    }

    private Cosmetic focus() {
        if (focused != null && focused.category() == category) return focused;
        CosmeticsFeature f = feature();
        if (f == null) return null;
        if (f.selected(category) != null) return f.selected(category);
        List<Cosmetic> items = f.cosmetics(category);
        return items.isEmpty() ? null : items.getFirst();
    }

    private static String badge(Cosmetic item) {
        if (CosmeticPets.onShoulder(item)) return "НА ПЛЕЧЕ";
        return switch (item.category()) {
            case SWORDS -> "CUSTOM MODEL";
            case PETS -> "CUSTOM PET";
            default -> "ПРЕДПРОСМОТР";
        };
    }

    private float previewHeight(Layout l, Cosmetic item) {
        boolean swords = item != null && item.category() == Category.SWORDS;
        return l.h - (swords ? 150 : 128) * l.u;
    }

    private void swordChips(Layout l, int accent) {
        CosmeticsFeature f = feature();
        if (f == null) return;
        float u = l.u, gap = 3 * u, chipW = (l.panelW - 18 * u - gap * 3) / 4f, chipH = 14 * u;
        text(l.panelX + 12 * u, l.panelY() + 155 * u, 3.4F * u, "ПРИМЕНЯТЬ К", FAINT, false);
        for (int i = 0; i < f.swordTargets.size(); i++) {
            SwordTarget target = f.swordTargets.get(i);
            float x = l.panelX + 9 * u + (i % 4) * (chipW + gap);
            float y = l.panelY() + 164 * u + (i / 4) * (chipH + gap);
            boolean on = target.setting().getValue();
            boolean hover = inside(x, y, chipW, chipH, mouseX, mouseY);
            rect(x, y, chipW, chipH, on ? ColorUtil.withAlpha(accent, hover ? 65 : 45)
                    : hover ? 0x18FFFFFF : 0x0CFFFFFF, 4 * u);
            border(x, y, chipW, chipH, on ? ColorUtil.withAlpha(accent, 140) : 0x18FFFFFF, 4 * u);
            center(x + chipW * 0.5F, y + 4.5F * u, 3.3F * u, target.label(), on ? TEXT : FAINT);
        }
    }

    private boolean clickSwordTarget(Layout l, Cosmetic item, double mx, double my) {
        if (item == null || item.category() != Category.SWORDS) return false;
        CosmeticsFeature f = feature();
        if (f == null) return false;
        float u = l.u, gap = 3 * u, chipW = (l.panelW - 18 * u - gap * 3) / 4f, chipH = 14 * u;
        for (int i = 0; i < f.swordTargets.size(); i++) {
            float x = l.panelX + 9 * u + (i % 4) * (chipW + gap);
            float y = l.panelY() + 164 * u + (i / 4) * (chipH + gap);
            if (inside(x, y, chipW, chipH, mx, my)) {
                SwordTarget target = f.swordTargets.get(i);
                target.setting().setValue(!target.setting().getValue());
                return true;
            }
        }
        return false;
    }

    private void panel(GuiGraphicsExtractor gfx, Layout l) {
        float u = l.u, x = l.panelX, y = l.panelY(), w = l.panelW, h = l.panelH();
        Cosmetic item = focus();
        int accent = item == null ? Theme.getAccent() : item.secondary() | 0xFF000000;
        rect(x, y, w, h, 0xFF181E2A, 8 * u);
        border(x, y, w, h, 0x20A2B5D9, 8 * u);
        text(x + 12 * u, y + 11 * u, 3.8F * u, item == null ? "КОЛЛЕКЦИЯ" : badge(item), accent, true);
        if (item == null) return;
        text(x + 12 * u, y + 23 * u, 6 * u, fitText(item.name(), 6 * u, w - 24 * u), TEXT, true);
        float previewH = previewHeight(l, item);
        rect(x + 9 * u, y + 39 * u, w - 18 * u, previewH, ColorUtil.withAlpha(accent, 10), 6 * u);
        ScreenRectangle clip = new ScreenRectangle(Math.round(x + 9 * u), Math.round(y + 39 * u),
                Math.max(1, Math.round(w - 18 * u)), Math.max(1, Math.round(previewH)));
        CosmeticsPreview.drawItem(gfx, item, x + 12 * u, y + 42 * u, w - 24 * u, previewH - 6 * u,
                previewYaw, CosmeticPets.onShoulder(item), openAnim, clip);
        if (item.category() == Category.SWORDS) {
            swordChips(l, accent);
            center(x + w * 0.5F, y + h - 64 * u, 3.4F * u, "ЛКМ - вращать", FAINT);
        } else {
            center(x + w * 0.5F, y + h - 81 * u, 3.8F * u, "ЛКМ - вращать", FAINT);
            String line1 = switch (item.category()) {
                case PETS -> CosmeticPets.onShoulder(item) ? "Спутник на твоём плече" : "Твой маленький спутник";
                default -> "Косметика для твоего образа";
            };
            center(x + w * 0.5F, y + h - 65 * u, 4 * u, line1, DIM);
            center(x + w * 0.5F, y + h - 55 * u, 3.6F * u, "Выбери карточку, чтобы надеть", FAINT);
        }
        boolean equipped = item.equals(feature().selected(category));
        boolean hover = inside(x + 12 * u, y + h - 40 * u, w - 24 * u, 21 * u, mouseX, mouseY);
        rect(x + 12 * u, y + h - 40 * u, w - 24 * u, 21 * u,
                ColorUtil.withAlpha(accent, hover ? 75 : 45), 5 * u);
        border(x + 12 * u, y + h - 40 * u, w - 24 * u, 21 * u, ColorUtil.withAlpha(accent, 120), 5 * u);
        center(x + w * 0.5F, y + h - 33 * u, 5 * u, equipped ? "Снять" : "Надеть", TEXT);
        center(x + w * 0.5F, y + h - 10 * u, 3.8F * u, "Сбросить всё", DIM);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
        if (event.button() != 0) return super.mouseClicked(event, flag);
        Layout l = layout();
        float u = l.u;
        double x = event.x(), y = event.y();
        if (inside(l.x + l.w - 27 * u, l.y + 8 * u, 19 * u, 19 * u, x, y)) {
            onClose();
            return true;
        }
        int index = 0;
        for (Category c : Category.values()) {
            if (inside(l.x + 8 * u, l.y + (47 + index * 28) * u, 88 * u, 24 * u, x, y)) {
                category = c;
                focused = null;
                scroll = 0;
                previewYaw = -25;
                return true;
            }
            index++;
        }
        CosmeticsFeature f = feature();
        if (f == null) return true;
        if (inside(l.gridX, l.gridY, l.gridW, l.gridH, x, y)) {
            List<Cosmetic> items = f.cosmetics(category);
            for (int i = 0; i <= items.size(); i++) {
                float cardX = l.gridX + (i % 2) * (l.cardW + 8 * u);
                float cardY = l.gridY + (i / 2) * l.rowH() - scroll;
                if (inside(cardX, cardY, l.cardW, l.cardH(), x, y)) {
                    focused = i == 0 ? null : items.get(i - 1);
                    select(f, focused);
                    return true;
                }
            }
        }
        Cosmetic focusItem = focus();
        if (clickSwordTarget(l, focusItem, x, y)) {
            return true;
        }
        if (inside(l.panelX + 9 * u, l.panelY() + 39 * u, l.panelW - 18 * u,
                previewHeight(l, focusItem), x, y)) {
            draggingPreview = true;
            lastDragX = (float) x;
            return true;
        }
        if (inside(l.panelX + 12 * u, l.panelY() + l.panelH() - 40 * u,
                l.panelW - 24 * u, 21 * u, x, y)) {
            select(f, focus());
            return true;
        }
        if (inside(l.panelX, l.panelY() + l.panelH() - 16 * u, l.panelW, 16 * u, x, y)) {
            f.resetAll();
            return true;
        }
        return super.mouseClicked(event, flag);
    }

    private void select(CosmeticsFeature f, Cosmetic item) {
        if (item == null || item.equals(f.selected(category))) {
            f.selection(category).setValue(CosmeticsFeature.NONE);
        } else {
            f.selection(category).setValue(item.name());
            if (!f.isEnabled()) f.setEnabled(true);
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        Layout l = layout();
        if (inside(l.gridX, l.gridY, l.gridW, l.gridH, x, y)) {
            scroll = (float) Math.max(0, Math.min(maxScroll(l), scroll - dy * 34 * l.u));
            return true;
        }
        return super.mouseScrolled(x, y, dx, dy);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingPreview && event.button() == 0) {
            previewYaw = (previewYaw + ((float) event.x() - lastDragX) * 0.8F) % 360;
            lastDragX = (float) event.x();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingPreview && event.button() == 0) {
            draggingPreview = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private String fitText(String value, float size, float width) {
        var font = UiFonts.getFontForText(value, UiFontStyle.SEMIBOLD.weight());
        if (font.measureWidth(value, size, 0) <= width) return value;
        int length = value.length();
        while (length > 0 && font.measureWidth(value.substring(0, length) + "...", size, 0) > width) length--;
        return value.substring(0, length) + "...";
    }
}
