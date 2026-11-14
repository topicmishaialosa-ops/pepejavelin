package org.blade.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.blade.feature.impl.misc.RenamePasterClientFeature;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Textures;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.TextAlign;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WatermarkStyles {
    public enum Family {
        LINE, STACK, GRID, CARD, SPLIT, BADGE, BIG, TERMINAL,
        FRAME, SIDEBAR, TICKER, TICKET, HERO, DUO
    }

    public enum Surface {
        SOLID, GLASS, OUTLINE, GRADIENT, STRIPE, GLOW, DOUBLE, EDGE,
        SCAN, PILL, DOTS, HALF, CORNER, BARE
    }

    public record Preset(String name, Family family, Surface surface,
                         int accent, int text, boolean upper, float scale) {
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM");

    private static final int[] ACCENTS = {
            0xFF35E6FF, 0xFFFF4FD8, 0xFFA3FF5A, 0xFFFFC94C, 0xFFFF5C5C, 0xFF8A5CFF,
            0xFF5C8DFF, 0xFFF4F4F4, 0xFF56EDD3, 0xFFFF8A3D, 0xFFFF9AD5, 0xFFC7F464
    };
    private static final int TEXT_BRIGHT = 0xFFF4F4F7;
    private static final int TEXT_SOFT = 0xFFD7DBE4;

    private static final List<Preset> PRESETS = List.of(
            preset("Неон", Family.LINE, Surface.STRIPE, 0, TEXT_BRIGHT, false, 1.00F),
            preset("Стекло", Family.LINE, Surface.GLASS, 1, TEXT_BRIGHT, true, 1.00F),
            preset("Тонкая линия", Family.LINE, Surface.OUTLINE, 2, TEXT_SOFT, false, 0.92F),
            preset("Градиент", Family.LINE, Surface.GRADIENT, 3, TEXT_BRIGHT, false, 1.06F),
            preset("Полоса", Family.LINE, Surface.EDGE, 4, TEXT_BRIGHT, true, 0.98F),

            preset("Стопка", Family.STACK, Surface.SOLID, 5, TEXT_BRIGHT, false, 1.00F),
            preset("Тёмная стопка", Family.STACK, Surface.GLASS, 6, TEXT_BRIGHT, true, 1.02F),
            preset("Матрица", Family.STACK, Surface.SCAN, 7, TEXT_BRIGHT, true, 0.94F),
            preset("Список", Family.STACK, Surface.BARE, 8, TEXT_SOFT, false, 0.96F),
            preset("Две зоны", Family.STACK, Surface.HALF, 9, TEXT_BRIGHT, false, 1.04F),

            preset("Сетка", Family.GRID, Surface.SOLID, 10, TEXT_BRIGHT, false, 1.00F),
            preset("Плитки", Family.GRID, Surface.GRADIENT, 11, TEXT_BRIGHT, true, 1.02F),
            preset("Кубики", Family.GRID, Surface.DOTS, 0, TEXT_SOFT, false, 0.94F),
            preset("Статистика", Family.GRID, Surface.OUTLINE, 2, TEXT_BRIGHT, false, 0.98F),
            preset("Дэшборд", Family.GRID, Surface.GLOW, 4, TEXT_BRIGHT, true, 1.05F),

            preset("Карточка", Family.CARD, Surface.SOLID, 5, TEXT_BRIGHT, false, 1.00F),
            preset("Стеклянная карточка", Family.CARD, Surface.GLASS, 1, TEXT_BRIGHT, false, 1.00F),
            preset("Паспорт", Family.CARD, Surface.DOUBLE, 2, TEXT_BRIGHT, true, 1.02F),
            preset("Пропуск", Family.CARD, Surface.EDGE, 3, TEXT_BRIGHT, false, 0.98F),
            preset("Тёмный пропуск", Family.CARD, Surface.GRADIENT, 6, TEXT_SOFT, true, 1.04F),

            preset("Сплит", Family.SPLIT, Surface.STRIPE, 0, TEXT_BRIGHT, false, 1.00F),
            preset("Дуэт", Family.SPLIT, Surface.SOLID, 11, TEXT_BRIGHT, true, 1.00F),
            preset("Раздел", Family.SPLIT, Surface.DOUBLE, 2, TEXT_SOFT, false, 0.96F),
            preset("Блок", Family.SPLIT, Surface.HALF, 9, TEXT_BRIGHT, false, 1.04F),
            preset("Контраст", Family.SPLIT, Surface.OUTLINE, 7, TEXT_BRIGHT, true, 1.00F),

            preset("Значок", Family.BADGE, Surface.PILL, 0, TEXT_BRIGHT, false, 1.00F),
            preset("Пилюля", Family.BADGE, Surface.PILL, 5, TEXT_BRIGHT, true, 0.94F),
            preset("Бейдж", Family.BADGE, Surface.SOLID, 2, TEXT_BRIGHT, false, 1.00F),
            preset("Мини-бейдж", Family.BADGE, Surface.OUTLINE, 1, TEXT_SOFT, false, 0.90F),
            preset("Капсула", Family.BADGE, Surface.GRADIENT, 8, TEXT_BRIGHT, true, 1.06F),

            preset("Большой FPS", Family.BIG, Surface.SOLID, 0, TEXT_BRIGHT, false, 1.00F),
            preset("Гигант", Family.BIG, Surface.BARE, 7, TEXT_BRIGHT, true, 1.02F),
            preset("Акцент", Family.BIG, Surface.GRADIENT, 1, TEXT_BRIGHT, false, 1.00F),
            preset("Счётчик", Family.BIG, Surface.OUTLINE, 2, TEXT_SOFT, false, 0.96F),
            preset("Фокус", Family.BIG, Surface.GLOW, 9, TEXT_BRIGHT, true, 1.04F),

            preset("Консоль", Family.TERMINAL, Surface.SOLID, 2, TEXT_BRIGHT, true, 1.00F),
            preset("Хакер", Family.TERMINAL, Surface.SCAN, 2, TEXT_BRIGHT, true, 0.98F),
            preset("Шелл", Family.TERMINAL, Surface.BARE, 8, TEXT_BRIGHT, true, 0.96F),
            preset("Лог", Family.TERMINAL, Surface.EDGE, 10, TEXT_SOFT, true, 1.02F),
            preset("Код", Family.TERMINAL, Surface.OUTLINE, 3, TEXT_BRIGHT, true, 1.00F),

            preset("Рамка", Family.FRAME, Surface.DOUBLE, 7, TEXT_BRIGHT, false, 1.00F),
            preset("Уголки", Family.FRAME, Surface.CORNER, 0, TEXT_BRIGHT, true, 1.02F),
            preset("Квадрат", Family.FRAME, Surface.OUTLINE, 5, TEXT_SOFT, false, 0.98F),
            preset("Панель", Family.FRAME, Surface.SOLID, 6, TEXT_BRIGHT, false, 1.00F),
            preset("Неоновая рамка", Family.FRAME, Surface.GLOW, 1, TEXT_BRIGHT, true, 1.05F),

            preset("Боковая панель", Family.SIDEBAR, Surface.EDGE, 2, TEXT_BRIGHT, false, 1.00F),
            preset("Вертикаль", Family.SIDEBAR, Surface.STRIPE, 0, TEXT_BRIGHT, true, 0.96F),
            preset("Колонка", Family.SIDEBAR, Surface.SOLID, 7, TEXT_SOFT, false, 1.02F),
            preset("Ремень", Family.SIDEBAR, Surface.HALF, 9, TEXT_BRIGHT, false, 1.00F),
            preset("Слайдер", Family.SIDEBAR, Surface.GRADIENT, 5, TEXT_BRIGHT, true, 1.04F),

            preset("Бегущая строка", Family.TICKER, Surface.SOLID, 0, TEXT_BRIGHT, false, 1.00F),
            preset("Тикер", Family.TICKER, Surface.BARE, 7, TEXT_SOFT, false, 0.94F),
            preset("Дорожка", Family.TICKER, Surface.STRIPE, 11, TEXT_BRIGHT, true, 1.00F),
            preset("Лента", Family.TICKER, Surface.GRADIENT, 2, TEXT_BRIGHT, false, 1.03F),
            preset("Пунктир", Family.TICKER, Surface.DOTS, 8, TEXT_BRIGHT, true, 0.98F),

            preset("Билет", Family.TICKET, Surface.SOLID, 3, TEXT_BRIGHT, false, 1.00F),
            preset("Талон", Family.TICKET, Surface.DOUBLE, 5, TEXT_BRIGHT, true, 1.02F),
            preset("Купон", Family.TICKET, Surface.EDGE, 0, TEXT_SOFT, false, 0.98F),
            preset("Ваучер", Family.TICKET, Surface.GRADIENT, 9, TEXT_BRIGHT, false, 1.04F),
            preset("Пропуск 2", Family.TICKET, Surface.GLASS, 6, TEXT_BRIGHT, true, 1.00F),

            preset("Герой", Family.HERO, Surface.SOLID, 7, TEXT_BRIGHT, false, 1.00F),
            preset("Профиль", Family.HERO, Surface.GRADIENT, 0, TEXT_BRIGHT, false, 1.02F),
            preset("Аватар", Family.HERO, Surface.GLASS, 1, TEXT_BRIGHT, true, 1.00F),
            preset("Визитка", Family.HERO, Surface.DOUBLE, 5, TEXT_SOFT, false, 0.98F),
            preset("Постер", Family.HERO, Surface.STRIPE, 2, TEXT_BRIGHT, true, 1.05F),

            preset("Два блока", Family.DUO, Surface.SOLID, 0, TEXT_BRIGHT, false, 1.00F),
            preset("Две пилюли", Family.DUO, Surface.PILL, 1, TEXT_BRIGHT, true, 0.98F),
            preset("Пара", Family.DUO, Surface.OUTLINE, 7, TEXT_SOFT, false, 0.94F),
            preset("Двойной", Family.DUO, Surface.GRADIENT, 8, TEXT_BRIGHT, false, 1.02F),
            preset("Дуо-неон", Family.DUO, Surface.GLOW, 9, TEXT_BRIGHT, true, 1.04F)
    );

    public static final String[] OPTIONS;
    private static final Map<String, Preset> BY_NAME = new LinkedHashMap<>();

    static {
        List<String> options = new ArrayList<>(PRESETS.size() + 3);
        options.add("New");
        options.add("Old");
        options.add("Bar");
        for (Preset preset : PRESETS) {
            options.add(preset.name());
            BY_NAME.put(preset.name(), preset);
        }
        OPTIONS = options.toArray(String[]::new);
    }

    private WatermarkStyles() {
    }

    private static Preset preset(String name, Family family, Surface surface,
                                 int accentIndex, int text, boolean upper, float scale) {
        return new Preset(name, family, surface, ACCENTS[accentIndex % ACCENTS.length], text, upper, scale);
    }

    public static Preset byName(String name) {
        return BY_NAME.get(name);
    }

    public static boolean isCustom(String name) {
        return BY_NAME.containsKey(name);
    }

    public static Scene build(Minecraft mc, float unit, Preset preset) {
        return new Builder(mc, unit, preset).build();
    }

    public sealed interface Prim permits Rect, Text, Tex {
    }

    public record Rect(float x, float y, float w, float h, int c1, int c2, int c3, int c4,
                       float radius, float border, int borderColor, float shadow, int shadowColor,
                       float glass, float blur) implements Prim {
    }

    public record Text(float x, float y, float size, String text, int color, UiFontStyle style,
                       TextAlign align, MsdfFont font, float outline, int outlineColor) implements Prim {
    }

    public record Tex(float x, float y, float w, float h, Identifier id, int color) implements Prim {
    }

    public static final class Scene {
        private final List<Prim> prims = new ArrayList<>();
        public float width;
        public float height;

        public void draw(float ox, float oy, float alpha) {
            if (alpha <= 0.001F) return;
            for (Prim prim : this.prims) {
                if (prim instanceof Rect r) {
                    var rect = Render2DUtil.rect(ox + r.x, oy + r.y, r.w, r.h);
                    if (r.glass > 0.0F) {
                        rect.glass(alpha * r.glass, r.border, r.blur);
                    } else {
                        rect.color(multiply(r.c1, alpha), multiply(r.c2, alpha),
                                multiply(r.c3, alpha), multiply(r.c4, alpha));
                        if (r.border > 0.0F) rect.border(r.border, multiply(r.borderColor, alpha));
                        if (r.shadow > 0.0F) rect.shadow(multiply(r.shadowColor, alpha), r.shadow);
                    }
                    if (r.radius > 0.0F) rect.radius(r.radius);
                    rect.draw();
                } else if (prim instanceof Text t) {
                    var text = Render2DUtil.text(ox + t.x, oy + t.y, t.size, t.text)
                            .font(t.font).style(t.style).color(multiply(t.color, alpha)).align(t.align);
                    if (t.outline > 0.0F) text.outline(multiply(t.outlineColor, alpha), t.outline);
                    text.draw();
                } else if (prim instanceof Tex x) {
                    Render2DUtil.texture(ox + x.x, oy + x.y, x.w, x.h, x.id)
                            .color(multiply(x.color, alpha)).draw();
                }
            }
        }

        private static int multiply(int color, float alpha) {
            return ColorUtil.multiplyAlpha(color, alpha);
        }
    }

    private static final class Builder {
        private final Minecraft mc;
        private final Preset preset;
        private final Scene scene = new Scene();
        private final MsdfFont font = UiFonts.sfProDisplay();
        private final float s;
        private final int accent;
        private final int text;
        private final int dim;
        private final int faint;
        private final int bg = 0xE8141821;

        private final String client;
        private final String name;
        private final String server;
        private final String fps;
        private final String ping;
        private final String time;
        private final String date;
        private final String coords;
        private final String dimension;
        private final String memory;

        private Builder(Minecraft mc, float unit, Preset preset) {
            this.mc = mc;
            this.preset = preset;
            this.s = Math.max(0.35F, unit * preset.scale());
            this.accent = preset.accent();
            this.text = preset.text();
            this.dim = ColorUtil.withAlpha(this.text, 155);
            this.faint = ColorUtil.withAlpha(this.text, 95);

            String rawClient = RenamePasterClientFeature.getClientName();
            this.client = label(rawClient == null || rawClient.isBlank() ? "BLADE" : rawClient);
            String userName = mc.getUser() != null ? mc.getUser().getName() : null;
            this.name = label(userName == null || userName.isBlank() ? "PLAYER" : userName);
            this.server = label(shortServer(mc));
            this.fps = Integer.toString(mc.getFps());
            this.ping = Long.toString(latency(mc));
            this.time = LocalTime.now().format(TIME);
            this.date = LocalDate.now().format(DATE);
            this.coords = mc.player == null ? "x 0 y 0 z 0"
                    : "x " + Math.round(mc.player.getX()) + " y " + Math.round(mc.player.getY())
                    + " z " + Math.round(mc.player.getZ());
            this.dimension = label(dimension(mc));
            this.memory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                    / (1024L * 1024L)) + " MB";
        }

        private Scene build() {
            switch (this.preset.family()) {
                case LINE -> line();
                case STACK -> stack();
                case GRID -> grid();
                case CARD -> card();
                case SPLIT -> split();
                case BADGE -> badge();
                case BIG -> big();
                case TERMINAL -> terminal();
                case FRAME -> frame();
                case SIDEBAR -> sidebar();
                case TICKER -> ticker();
                case TICKET -> ticket();
                case HERO -> hero();
                case DUO -> duo();
            }
            return this.scene;
        }

        private String label(String value) {
            String safe = value == null ? "" : value;
            return this.preset.upper() ? safe.toUpperCase(Locale.ROOT) : safe;
        }

        private float width(String value, float size, UiFontStyle style) {
            return this.font.measureWidth(value, size, size * style.letterSpacingEm());
        }

        private float height(float size) {
            return this.font.textHeight(size);
        }

        private void rect(float x, float y, float w, float h, int color, float radius) {
            this.scene.prims.add(new Rect(x, y, w, h, color, color, color, color,
                    radius, 0.0F, 0, 0.0F, 0, 0.0F, 0.0F));
        }

        private void rect(float x, float y, float w, float h, int color, float radius,
                          float border, int borderColor) {
            this.scene.prims.add(new Rect(x, y, w, h, color, color, color, color,
                    radius, border, borderColor, 0.0F, 0, 0.0F, 0.0F));
        }

        private void rect(float x, float y, float w, float h, int color, float radius,
                          float border, int borderColor, float shadow, int shadowColor) {
            this.scene.prims.add(new Rect(x, y, w, h, color, color, color, color,
                    radius, border, borderColor, shadow, shadowColor, 0.0F, 0.0F));
        }

        private void outline(float x, float y, float w, float h, float radius, float border, int color) {
            this.scene.prims.add(new Rect(x, y, w, h, 0, 0, 0, 0,
                    radius, border, color, 0.0F, 0, 0.0F, 0.0F));
        }

        private void gradient(float x, float y, float w, float h, float radius,
                              int c1, int c2, int c3, int c4) {
            this.scene.prims.add(new Rect(x, y, w, h, c1, c2, c3, c4,
                    radius, 0.0F, 0, 0.0F, 0, 0.0F, 0.0F));
        }

        private void glass(float x, float y, float w, float h, float amount, float border,
                           float blur, float radius) {
            this.scene.prims.add(new Rect(x, y, w, h, 0, 0, 0, 0,
                    radius, Math.max(0.5F, border), 0, 0.0F, 0, amount, blur));
        }

        private void text(float x, float y, float size, String value, int color, UiFontStyle style) {
            text(x, y, size, value, color, style, TextAlign.LEFT, 0.0F, 0);
        }

        private void text(float x, float y, float size, String value, int color, UiFontStyle style,
                          TextAlign align) {
            text(x, y, size, value, color, style, align, 0.0F, 0);
        }

        private void text(float x, float y, float size, String value, int color, UiFontStyle style,
                          TextAlign align, float outline, int outlineColor) {
            if (value == null || value.isEmpty()) return;
            this.scene.prims.add(new Text(x, y, size, value, color, style, align,
                    this.font, outline, outlineColor));
        }

        private void icon(float x, float y, float w, float h, Identifier id, int color) {
            this.scene.prims.add(new Tex(x, y, w, h, id, color));
        }

        private void dot(float cx, float cy, float radius, int color) {
            rect(cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, color, radius);
        }

        private void surface(float x, float y, float w, float h, float radius) {
            int tint = ColorUtil.lerp(this.bg, this.accent, 0.16F);
            int tint2 = ColorUtil.lerp(this.bg, this.accent, 0.30F);
            switch (this.preset.surface()) {
                case BARE -> {
                }
                case SOLID -> rect(x, y, w, h, this.bg, radius);
                case GLASS -> glass(x, y, w, h, 0.72F, 0.8F * this.s, 10.0F * this.s, radius);
                case OUTLINE -> outline(x, y, w, h, radius, 1.1F * this.s, ColorUtil.withAlpha(this.accent, 170));
                case GRADIENT -> gradient(x, y, w, h, radius, tint2, this.bg, this.bg, tint);
                case STRIPE -> {
                    rect(x, y, w, h, this.bg, radius);
                    rect(x, y, 3.0F * this.s, h, this.accent, Math.min(radius, 1.5F * this.s));
                }
                case GLOW -> rect(x, y, w, h, this.bg, radius, 1.2F * this.s,
                        ColorUtil.withAlpha(this.accent, 180), 10.0F * this.s,
                        ColorUtil.withAlpha(this.accent, 70));
                case DOUBLE -> {
                    rect(x, y, w, h, this.bg, radius, 1.4F * this.s, ColorUtil.withAlpha(this.accent, 170));
                    outline(x + 3.0F * this.s, y + 3.0F * this.s, w - 6.0F * this.s,
                            h - 6.0F * this.s, Math.max(0.0F, radius - 2.0F * this.s),
                            0.7F * this.s, ColorUtil.withAlpha(this.text, 42));
                }
                case EDGE -> {
                    rect(x, y, w, h, this.bg, radius);
                    rect(x, y, 3.5F * this.s, h, this.accent, Math.min(radius, 1.75F * this.s));
                }
                case SCAN -> {
                    rect(x, y, w, h, this.bg, radius);
                    for (float lineY = y + 4.0F * this.s; lineY < y + h - 1.0F; lineY += 4.0F * this.s) {
                        rect(x + 2.0F * this.s, lineY, w - 4.0F * this.s, 0.55F * this.s,
                                ColorUtil.withAlpha(this.accent, 28), 0.0F);
                    }
                }
                case PILL -> {
                    float pill = h / 2.0F;
                    rect(x, y, w, h, this.bg, pill, 1.0F * this.s,
                            ColorUtil.withAlpha(this.accent, 120));
                }
                case DOTS -> {
                    rect(x, y, w, h, this.bg, radius);
                    int dotColor = ColorUtil.withAlpha(this.accent, 42);
                    for (float yy = y + 6.0F * this.s; yy < y + h - 3.0F * this.s; yy += 7.0F * this.s) {
                        for (float xx = x + 7.0F * this.s; xx < x + w - 3.0F * this.s; xx += 7.0F * this.s) {
                            rect(xx, yy, 1.4F * this.s, 1.4F * this.s, dotColor, 0.7F * this.s);
                        }
                    }
                }
                case HALF -> {
                    rect(x, y, w, h, this.bg, radius);
                    gradient(x, y, w, h * 0.5F, radius, tint2, tint2,
                            ColorUtil.withAlpha(tint, 0), ColorUtil.withAlpha(tint, 0));
                    rect(x, y + h * 0.5F, w, 0.8F * this.s, ColorUtil.withAlpha(this.accent, 90), 0.0F);
                }
                case CORNER -> {
                    float len = 9.0F * this.s;
                    float th = 1.5F * this.s;
                    int c = ColorUtil.withAlpha(this.accent, 210);
                    rect(x, y, len, th, c, 0.0F);
                    rect(x, y, th, len, c, 0.0F);
                    rect(x + w - len, y, len, th, c, 0.0F);
                    rect(x + w - th, y, th, len, c, 0.0F);
                    rect(x, y + h - th, len, th, c, 0.0F);
                    rect(x, y + h - len, th, len, c, 0.0F);
                    rect(x + w - len, y + h - th, len, th, c, 0.0F);
                    rect(x + w - th, y + h - len, th, len, c, 0.0F);
                }
                default -> rect(x, y, w, h, this.bg, radius);
            }
        }

        private void line() {
            float fs = 8.0F * this.s;
            float pad = 7.0F * this.s;
            float gap = 4.0F * this.s;
            float dotR = 1.6F * this.s;
            List<String> parts = new ArrayList<>(6);
            List<Float> widths = new ArrayList<>(6);
            addPart(parts, widths, this.client, fs, UiFontStyle.SEMIBOLD);
            addPart(parts, widths, this.name, fs, UiFontStyle.REGULAR);
            addPart(parts, widths, this.fps + " FPS", fs, UiFontStyle.SEMIBOLD);
            addPart(parts, widths, this.ping + " MS", fs, UiFontStyle.REGULAR);
            if (this.preset.surface() != Surface.BARE) {
                addPart(parts, widths, this.server, fs, UiFontStyle.REGULAR);
                addPart(parts, widths, this.time, fs, UiFontStyle.REGULAR);
            }

            float content = 12.0F * this.s + gap;
            for (int i = 0; i < widths.size(); i++) {
                content += widths.get(i);
                if (i + 1 < widths.size()) content += gap + dotR * 2.0F + gap;
            }
            float w = content + pad * 2.0F;
            float h = height(fs) + pad * 2.0F;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 7.0F * this.s);

            float cy = h / 2.0F;
            float x = pad;
            icon(x, cy - 6.0F * this.s, 12.0F * this.s, 12.0F * this.s,
                    Textures.Logos.BOLT, this.accent);
            x += 12.0F * this.s + gap;
            for (int i = 0; i < parts.size(); i++) {
                UiFontStyle style = i == 0 ? UiFontStyle.SEMIBOLD
                        : i == 2 ? UiFontStyle.SEMIBOLD : UiFontStyle.REGULAR;
                int color = i == 0 ? this.accent : i < 2 ? this.text : this.dim;
                text(x, cy - height(fs) / 2.0F, fs, parts.get(i), color, style);
                x += widths.get(i);
                if (i + 1 < parts.size()) {
                    dot(x + gap + dotR, cy, dotR, ColorUtil.withAlpha(this.accent, 150));
                    x += gap + dotR * 2.0F + gap;
                }
            }
        }

        private void addPart(List<String> parts, List<Float> widths, String value,
                             float size, UiFontStyle style) {
            parts.add(value);
            widths.add(width(value, size, style));
        }

        private void stack() {
            float fs = 8.0F * this.s;
            float small = 5.2F * this.s;
            float pad = 7.0F * this.s;
            float lineH = height(fs) + 4.0F * this.s;
            String[] lines = {this.client, this.name, this.fps + " FPS  /  " + this.ping + " MS",
                    this.server + "  /  " + this.time};
            float max = 0.0F;
            for (String line : lines) max = Math.max(max, width(line, fs, UiFontStyle.MEDIUM));
            float w = max + pad * 2.0F + 6.0F * this.s;
            float h = pad * 2.0F + lineH * lines.length;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 7.0F * this.s);
            rect(0, pad, 2.5F * this.s, h - pad * 2.0F, this.accent, 1.25F * this.s);
            float y = pad;
            for (int i = 0; i < lines.length; i++) {
                int color = i == 0 ? this.accent : i == 3 ? this.faint : this.text;
                UiFontStyle style = i == 0 ? UiFontStyle.SEMIBOLD : UiFontStyle.MEDIUM;
                text(pad + 5.0F * this.s, y, i == 3 ? small : fs, lines[i], color, style);
                y += lineH;
            }
        }

        private void grid() {
            float labelSize = 4.8F * this.s;
            float valueSize = 8.5F * this.s;
            float pad = 7.0F * this.s;
            float gap = 5.0F * this.s;
            String[] labels = {"FPS", "PING", "TIME", "MEM"};
            String[] values = {this.fps, this.ping + "ms", this.time, this.memory};
            float cellW = 33.0F * this.s;
            for (int i = 0; i < values.length; i++) {
                cellW = Math.max(cellW, width(values[i], valueSize, UiFontStyle.SEMIBOLD) + 10.0F * this.s);
            }
            float cellH = height(labelSize) + height(valueSize) + 8.0F * this.s;
            float w = pad * 2.0F + cellW * 2.0F + gap;
            float h = pad * 2.0F + cellH * 2.0F + gap;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 8.0F * this.s);
            for (int i = 0; i < 4; i++) {
                float x = pad + (i % 2) * (cellW + gap);
                float y = pad + (i / 2) * (cellH + gap);
                rect(x, y, cellW, cellH, ColorUtil.withAlpha(this.accent, 18), 5.0F * this.s);
                rect(x, y, cellW, 1.2F * this.s, ColorUtil.withAlpha(this.accent, 160), 0.0F);
                text(x + 5.0F * this.s, y + 4.0F * this.s, labelSize, labels[i], this.faint, UiFontStyle.MEDIUM);
                text(x + 5.0F * this.s, y + 4.0F * this.s + height(labelSize) + 1.0F * this.s,
                        valueSize, values[i], this.text, UiFontStyle.SEMIBOLD);
            }
        }

        private void card() {
            float pad = 7.0F * this.s;
            float fs = 7.0F * this.s;
            float small = 5.0F * this.s;
            float w = 166.0F * this.s;
            float h = 48.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 8.0F * this.s);
            float logo = 15.0F * this.s;
            rect(pad, pad, logo, logo, ColorUtil.withAlpha(this.accent, 55), 4.0F * this.s,
                    0.8F * this.s, ColorUtil.withAlpha(this.accent, 160));
            icon(pad + 2.5F * this.s, pad + 2.5F * this.s, logo - 5.0F * this.s, logo - 5.0F * this.s,
                    Textures.Logos.BOLT, this.accent);
            text(pad + logo + 5.0F * this.s, pad + 1.0F * this.s, fs, this.client, this.text, UiFontStyle.SEMIBOLD);
            text(w - pad, pad + 1.0F * this.s, small, this.time, this.dim,
                    UiFontStyle.REGULAR, TextAlign.RIGHT);
            rect(pad, 26.0F * this.s, w - pad * 2.0F, 0.6F * this.s, ColorUtil.withAlpha(this.text, 26), 0.0F);
            text(pad, 31.0F * this.s, fs, this.name, this.text, UiFontStyle.MEDIUM);
            text(w - pad, 31.0F * this.s, small, this.fps + " FPS   " + this.ping + " MS",
                    this.accent, UiFontStyle.SEMIBOLD, TextAlign.RIGHT);
            text(pad, 40.0F * this.s, small, this.server + "  " + this.date, this.faint, UiFontStyle.REGULAR);
        }

        private void split() {
            float fs = 8.0F * this.s;
            float small = 5.2F * this.s;
            float pad = 7.0F * this.s;
            float leftW = 44.0F * this.s;
            float rightW = 118.0F * this.s;
            float w = leftW + rightW;
            float h = 40.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 8.0F * this.s);
            gradient(0, 0, leftW, h, 8.0F * this.s, this.accent,
                    ColorUtil.lerp(this.accent, 0xFF000000, 0.25F),
                    ColorUtil.lerp(this.accent, 0xFF000000, 0.35F), this.accent);
            rect(leftW, 0, rightW, h, 0xE611151D, 0.0F);
            icon(leftW / 2.0F - 8.0F * this.s, pad, 16.0F * this.s, 16.0F * this.s,
                    Textures.Logos.BOLT, 0xFF0C0F16);
            text(leftW / 2.0F, h - pad - height(small), small, this.client, 0xFF0C0F16,
                    UiFontStyle.SEMIBOLD, TextAlign.CENTER);
            float x = leftW + pad;
            text(x, pad, fs, this.name, this.text, UiFontStyle.SEMIBOLD);
            text(x, pad + height(fs) + 4.0F * this.s, small,
                    this.fps + " FPS  /  " + this.ping + " MS", this.accent, UiFontStyle.MEDIUM);
            text(x, h - pad - height(small), small, this.time + "  " + this.server,
                    this.faint, UiFontStyle.REGULAR);
        }

        private void badge() {
            float fs = 7.5F * this.s;
            float pad = 7.0F * this.s;
            String right = this.fps + " FPS";
            float w = pad * 2.0F + 12.0F * this.s + 5.0F * this.s
                    + width(this.client, fs, UiFontStyle.SEMIBOLD) + 5.0F * this.s
                    + width(right, fs, UiFontStyle.MEDIUM) + 12.0F * this.s;
            float h = 22.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, h / 2.0F);
            float cy = h / 2.0F;
            icon(pad, cy - 6.0F * this.s, 12.0F * this.s, 12.0F * this.s,
                    Textures.Logos.BOLT, this.accent);
            float x = pad + 12.0F * this.s + 5.0F * this.s;
            text(x, cy - height(fs) / 2.0F, fs, this.client, this.text, UiFontStyle.SEMIBOLD);
            x += width(this.client, fs, UiFontStyle.SEMIBOLD) + 5.0F * this.s;
            text(x, cy - height(fs) / 2.0F, fs, right, this.accent, UiFontStyle.MEDIUM);
            x += width(right, fs, UiFontStyle.MEDIUM) + 5.0F * this.s;
            dot(x, cy, 3.0F * this.s, ColorUtil.withAlpha(this.accent, 190));
        }

        private void big() {
            float bigSize = 23.0F * this.s;
            float fs = 7.0F * this.s;
            float small = 4.8F * this.s;
            float pad = 8.0F * this.s;
            String fpsText = this.fps;
            float bigW = width(fpsText, bigSize, UiFontStyle.SEMIBOLD);
            float rightW = Math.max(width(this.client, fs, UiFontStyle.SEMIBOLD),
                    Math.max(width(this.name, fs, UiFontStyle.MEDIUM),
                            width(this.ping + " MS / " + this.server, small, UiFontStyle.REGULAR)));
            float w = pad * 2.0F + bigW + 8.0F * this.s + rightW;
            float h = 58.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 8.0F * this.s);
            text(pad, 10.0F * this.s, small, "FPS", this.faint, UiFontStyle.MEDIUM);
            text(pad, h - pad - height(bigSize), bigSize, fpsText, this.accent, UiFontStyle.SEMIBOLD);
            float x = pad + bigW + 8.0F * this.s;
            text(x, pad + 4.0F * this.s, fs, this.client, this.text, UiFontStyle.SEMIBOLD);
            text(x, pad + 4.0F * this.s + height(fs) + 3.0F * this.s, fs, this.name,
                    this.dim, UiFontStyle.MEDIUM);
            text(x, h - pad - height(small), small,
                    this.ping + " MS / " + this.server, this.faint, UiFontStyle.REGULAR);
        }

        private void terminal() {
            float fs = 6.5F * this.s;
            float pad = 7.0F * this.s;
            float lineH = height(fs) + 3.0F * this.s;
            String[] lines = {"> " + this.client, "> " + this.server,
                    "> " + this.fps + " FPS / " + this.ping + " MS",
                    "> " + this.coords};
            float max = 0.0F;
            for (String line : lines) max = Math.max(max, width(line, fs, UiFontStyle.MEDIUM));
            float w = max + pad * 2.0F;
            float h = pad * 2.0F + lineH * lines.length;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 5.0F * this.s);
            float y = pad;
            for (int i = 0; i < lines.length; i++) {
                text(pad, y, fs, lines[i], i == 0 ? this.accent : this.text, UiFontStyle.MEDIUM);
                y += lineH;
            }
        }

        private void frame() {
            float fs = 7.0F * this.s;
            float small = 5.0F * this.s;
            float pad = 8.0F * this.s;
            float w = 168.0F * this.s;
            float h = 54.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 3.0F * this.s);
            text(pad, pad, fs, this.client, this.accent, UiFontStyle.SEMIBOLD);
            this.scene.prims.add(new Text(w - pad, pad, fs, this.fps + " FPS",
                    this.text, UiFontStyle.SEMIBOLD, TextAlign.RIGHT, this.font, 0.0F, 0));
            text(pad, h - pad - height(small), small, this.name, this.dim, UiFontStyle.MEDIUM);
            this.scene.prims.add(new Text(w - pad, h - pad - height(small), small,
                    this.ping + " MS / " + this.time, this.faint, UiFontStyle.REGULAR,
                    TextAlign.RIGHT, this.font, 0.0F, 0));
            this.scene.prims.add(new Text(w / 2.0F, h / 2.0F - height(fs) / 2.0F, fs,
                    this.server, this.text, UiFontStyle.MEDIUM, TextAlign.CENTER,
                    this.font, 0.0F, 0));
        }

        private void sidebar() {
            float fs = 7.5F * this.s;
            float small = 5.0F * this.s;
            float pad = 7.0F * this.s;
            float bar = 5.0F * this.s;
            float right = Math.max(width(this.client, fs, UiFontStyle.SEMIBOLD),
                    Math.max(width(this.name, fs, UiFontStyle.MEDIUM),
                            width(this.fps + " FPS   " + this.ping + " MS", small, UiFontStyle.MEDIUM)));
            float w = bar + pad + right + pad;
            float h = 48.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 7.0F * this.s);
            rect(0, 0, bar, h, this.accent, 2.5F * this.s);
            float x = bar + pad;
            text(x, pad, fs, this.client, this.text, UiFontStyle.SEMIBOLD);
            text(x, pad + height(fs) + 5.0F * this.s, small, this.name, this.dim, UiFontStyle.MEDIUM);
            text(x, h - pad - height(small), small, this.fps + " FPS   " + this.ping + " MS",
                    this.accent, UiFontStyle.MEDIUM);
        }

        private void ticker() {
            float fs = 7.0F * this.s;
            float pad = 7.0F * this.s;
            float gap = 5.0F * this.s;
            String[] parts = {this.client, this.name, this.fps + " FPS",
                    this.ping + " MS", this.time, this.server};
            float content = 0.0F;
            for (int i = 0; i < parts.length; i++) {
                content += width(parts[i], fs, UiFontStyle.MEDIUM);
                if (i + 1 < parts.length) content += gap * 2.0F + 2.0F * this.s;
            }
            float w = content + pad * 2.0F;
            float h = height(fs) + pad * 1.6F;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 5.0F * this.s);
            float cy = h / 2.0F;
            float x = pad;
            for (int i = 0; i < parts.length; i++) {
                int color = i == 0 ? this.accent : i < 2 ? this.text : this.dim;
                text(x, cy - height(fs) / 2.0F, fs, parts[i], color, UiFontStyle.MEDIUM);
                x += width(parts[i], fs, UiFontStyle.MEDIUM);
                if (i + 1 < parts.length) {
                    dot(x + gap + this.s, cy, 1.3F * this.s, ColorUtil.withAlpha(this.accent, 170));
                    x += gap * 2.0F + 2.0F * this.s;
                }
            }
        }

        private void ticket() {
            float fs = 7.0F * this.s;
            float small = 4.8F * this.s;
            float pad = 8.0F * this.s;
            float stub = 42.0F * this.s;
            float w = 174.0F * this.s;
            float h = 52.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 8.0F * this.s);
            float splitX = w - stub;
            for (float y = pad; y < h - pad; y += 5.0F * this.s) {
                rect(splitX, y, 0.8F * this.s, 2.8F * this.s, ColorUtil.withAlpha(this.text, 55), 0.0F);
            }
            dot(0, h / 2.0F, 4.0F * this.s, 0xFF0B0E14);
            dot(w, h / 2.0F, 4.0F * this.s, 0xFF0B0E14);
            text(pad, pad, fs, this.client, this.accent, UiFontStyle.SEMIBOLD);
            text(pad, pad + height(fs) + 3.0F * this.s, fs, this.name, this.text, UiFontStyle.MEDIUM);
            text(pad, h - pad - height(small), small,
                    this.server + "  " + this.coords, this.faint, UiFontStyle.REGULAR);
            float x = splitX + 7.0F * this.s;
            text(x, pad, small, "FPS", this.faint, UiFontStyle.MEDIUM);
            text(x, pad + height(small) + 1.0F * this.s, 8.0F * this.s, this.fps,
                    this.text, UiFontStyle.SEMIBOLD);
            text(x, h - pad - height(small), small, this.ping + " MS", this.accent, UiFontStyle.MEDIUM);
        }

        private void hero() {
            float fs = 6.0F * this.s;
            float big = 14.0F * this.s;
            float small = 5.2F * this.s;
            float pad = 8.0F * this.s;
            float w = 170.0F * this.s;
            float h = 66.0F * this.s;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, w, h, 10.0F * this.s);
            float cx = w / 2.0F;
            this.scene.prims.add(new Text(cx, pad, fs, this.client,
                    this.accent, UiFontStyle.SEMIBOLD, TextAlign.CENTER, this.font, 0.0F, 0));
            icon(cx - 7.0F * this.s, pad + height(fs) + 4.0F * this.s,
                    14.0F * this.s, 14.0F * this.s, Textures.Logos.BOLT, this.accent);
            this.scene.prims.add(new Text(cx, pad + height(fs) + 20.0F * this.s, big,
                    this.name, this.text, UiFontStyle.SEMIBOLD, TextAlign.CENTER, this.font, 0.0F, 0));
            this.scene.prims.add(new Text(cx, h - pad - height(small), small,
                    this.fps + " FPS  /  " + this.ping + " MS  /  " + this.time,
                    this.dim, UiFontStyle.MEDIUM, TextAlign.CENTER, this.font, 0.0F, 0));
        }

        private void duo() {
            float fs = 7.0F * this.s;
            float pad = 7.0F * this.s;
            float h = 22.0F * this.s;
            String left = this.client + "  " + this.name;
            String right = this.fps + " FPS  /  " + this.ping + " MS";
            float leftW = pad * 2.0F + 12.0F * this.s + 4.0F * this.s
                    + width(left, fs, UiFontStyle.MEDIUM);
            float rightW = pad * 2.0F + width(right, fs, UiFontStyle.MEDIUM);
            float gap = 4.0F * this.s;
            float w = leftW + gap + rightW;
            this.scene.width = w;
            this.scene.height = h;
            surface(0, 0, leftW, h, h / 2.0F);
            surface(leftW + gap, 0, rightW, h, h / 2.0F);
            float cy = h / 2.0F;
            icon(pad, cy - 6.0F * this.s, 12.0F * this.s, 12.0F * this.s,
                    Textures.Logos.BOLT, this.accent);
            text(pad + 12.0F * this.s + 4.0F * this.s, cy - height(fs) / 2.0F, fs, left,
                    this.text, UiFontStyle.MEDIUM);
            text(leftW + gap + pad, cy - height(fs) / 2.0F, fs, right,
                    this.accent, UiFontStyle.MEDIUM);
        }

        private static String shortServer(Minecraft mc) {
            if (mc.getCurrentServer() == null) return "SINGLEPLAYER";
            String ip = mc.getCurrentServer().ip;
            return ip == null || ip.isBlank() ? "SINGLEPLAYER" : ip;
        }

        private static String dimension(Minecraft mc) {
            if (mc.level == null) return "OVERWORLD";
            if (mc.level.dimension() == Level.OVERWORLD) return "OVERWORLD";
            if (mc.level.dimension() == Level.NETHER) return "NETHER";
            if (mc.level.dimension() == Level.END) return "END";
            return mc.level.dimension().identifier().getPath();
        }

        private static long latency(Minecraft mc) {
            if (mc.getCurrentServer() == null) return 0L;
            if (mc.getConnection() == null || mc.player == null) return mc.getCurrentServer().ping;
            var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return info != null ? info.getLatency() : mc.getCurrentServer().ping;
        }
    }
}
