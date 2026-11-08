package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.blade.context.RenderContext;
import org.blade.menu.ui.controls.MenuClipboard;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Textures;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.TextAlign;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InstanceBotScreen extends Screen {

    private static final int BG = ColorUtil.rgba(10, 11, 16, 217);
    private static final int WIN = ColorUtil.rgb(16, 17, 22);
    private static final int SIDE = ColorUtil.rgb(13, 14, 19);
    private static final int CARD = ColorUtil.rgb(23, 24, 31);
    private static final int CARD_HOVER = ColorUtil.rgb(29, 30, 40);
    private static final int CARD_SEL = ColorUtil.rgb(30, 32, 44);
    private static final int CONTROL = ColorUtil.rgb(34, 36, 47);
    private static final int CONTROL_HOVER = ColorUtil.rgb(44, 47, 61);
    private static final int PRIMARY = ColorUtil.rgb(58, 61, 76);
    private static final int PRIMARY_HOVER = ColorUtil.rgb(70, 74, 92);
    private static final int FIELD = ColorUtil.rgb(20, 21, 28);
    private static final int CHIP = ColorUtil.rgb(9, 10, 13);
    private static final int TEXT = Theme.Colors.TEXT;
    private static final int DIM = Theme.Colors.SECONDARY;
    private static final int FAINT = ColorUtil.rgb(110, 112, 124);
    private static final int GREEN = InstanceBotManager.COLOR_OK;
    private static final int AMBER = InstanceBotManager.COLOR_AMBER;
    private static final int RED = InstanceBotManager.COLOR_ERROR;
    private static final int GRAY = InstanceBotManager.COLOR_GRAY;
    private static final int DANGER_BG = ColorUtil.rgba(237, 106, 95, 36);
    private static final int DANGER_BG_HOVER = ColorUtil.rgba(237, 106, 95, 56);

    private static final Identifier[] SIDE_ICONS = {
            Textures.Icons.SWORDS,
            Textures.Icons.CHEVRONS_LEFT_RIGHT,
            Textures.Icons.EYE,
            Textures.Icons.PIN,
            Textures.Icons.OPTION,
            Textures.Icons.GIFT,
            Textures.Icons.GAMEPAD
    };

    private final InstanceBotManager manager = InstanceBotManager.INSTANCE;

    private String search = "";
    private boolean searchFocus;
    private String message = "";
    private boolean messageFocus;
    private boolean adding;
    private String addName = "";
    private String addServer = "";
    private int addField;
    private InstanceBotManager.Bot selected;
    private float listScroll;
    private float logScroll;
    private InstanceBotManager.Bot logBot;
    private int logCount;
    private boolean showInventory = true;
    private float openAnim;
    private float mouseXf;
    private float mouseYf;

    public InstanceBotScreen() {
        super(Component.literal("InstanceBot"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof InstanceBotScreen) {
            return;
        }
        mc.gui.setScreen(new InstanceBotScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class L {
        float u;
        float wx;
        float wy;
        float ww;
        float wh;
        float sideW;
        float headerH;
        float contentLeft;
        float contentRight;
        float leftX;
        float leftW;
        float rightX;
        float rightW;
        float headerRowY;
        float headerRowH;
        float listTop;
        float listBottom;
        float searchX;
        float searchY;
        float searchW;
        float searchH;
        float chipX;
        float chipY;
        float chipW;
        float chipH;
        float mainBtnX;
        float mainBtnY;
        float mainBtnW;
        float mainBtnH;
        float hostX;
        float hostW;
        float addX;
        float addW;
        float headBtnY;
        float headBtnH;
        float delY;
        float delW;
        float delH;
        float nameY;
        float subY;
        float btnY;
        float btnH;
        float btnW;
        float btnGap;
        float msgY;
        float msgH;
        float sendW;
        float logTop;
        float logBottom;
        float statsY;
        float invY;
        float invW;
        float invH;
        float gridY;
        float toastY;
        float toastH;
    }

    private L layout() {
        L l = new L();
        float min = Math.min(this.width, this.height);
        float u = Math.min(this.width / 640.0F, this.height / 360.0F);
        if (u <= 0.0F) {
            u = min / 360.0F;
        }
        l.u = u;
        l.ww = 330.0F * u;
        l.wh = 203.0F * u;
        l.wx = (this.width - l.ww) * 0.5F;
        l.wy = (this.height - l.wh) * 0.5F;
        l.sideW = 27.0F * u;
        l.headerH = 25.0F * u;
        l.contentLeft = l.wx + l.sideW + 10.0F * u;
        l.contentRight = l.wx + l.ww - 9.0F * u;
        float contentW = l.contentRight - l.contentLeft;
        float gap = 9.0F * u;
        l.leftW = (contentW - gap) * 0.31F;
        l.leftX = l.contentLeft;
        l.rightX = l.leftX + l.leftW + gap;
        l.rightW = l.contentRight - l.rightX;

        l.headerRowY = l.wy + l.headerH + 9.0F * u;
        l.headerRowH = 9.0F * u;
        l.listTop = l.headerRowY + l.headerRowH + 5.0F * u;
        l.listBottom = l.wy + l.wh - 8.0F * u;

        l.searchH = 12.0F * u;
        l.searchW = 95.0F * u;
        l.searchX = l.wx + l.ww - 8.0F * u - l.searchW;
        l.searchY = l.wy + (l.headerH - l.searchH) * 0.5F;
        l.chipH = 12.0F * u;
        l.chipW = 38.0F * u;
        l.chipX = l.searchX - 5.0F * u - l.chipW;
        l.chipY = l.searchY;

        l.mainBtnW = 54.0F * u;
        l.mainBtnH = 12.0F * u;
        l.mainBtnX = l.chipX - 5.0F * u - l.mainBtnW;
        l.mainBtnY = l.searchY;

        l.headBtnH = 9.0F * u;
        l.headBtnY = l.headerRowY;
        l.hostW = 26.0F * u;
        l.addW = 44.0F * u;
        l.addX = l.leftX + l.leftW - l.addW;
        l.hostX = l.addX - 4.0F * u - l.hostW;

        l.nameY = l.wy + l.headerH + 11.0F * u;
        l.subY = l.nameY + 8.5F * u;
        l.delH = 9.0F * u;
        l.delW = 27.0F * u;
        l.delY = l.nameY - 1.0F * u;

        l.btnY = l.wy + l.headerH + 27.0F * u;
        l.btnH = 11.0F * u;
        l.btnGap = 3.0F * u;
        l.btnW = (l.rightW - 3.0F * l.btnGap) / 4.0F;

        l.msgY = l.wy + l.headerH + 42.0F * u;
        l.msgH = 10.0F * u;
        l.sendW = 30.0F * u;

        l.logTop = l.msgY + l.msgH + 6.0F * u;
        l.logBottom = l.wy + l.wh - 8.0F * u;

        l.statsY = l.wy + l.headerH + 62.0F * u;
        l.invY = l.wy + l.headerH + 82.0F * u;
        l.invH = 9.0F * u;
        l.invW = 36.0F * u;
        l.gridY = l.wy + l.headerH + 93.0F * u;

        l.toastH = 9.0F * u;
        l.toastY = l.wy + l.wh - 8.0F * u - l.toastH;
        return l;
    }

    private float cardHeight(L l) {
        return 21.0F * l.u;
    }

    private float cardGap(L l) {
        return 3.0F * l.u;
    }

    private float[] cardRect(L l, int index) {
        float h = cardHeight(l);
        return new float[]{
                l.leftX,
                l.listTop + index * (h + cardGap(l)) - listScroll,
                l.leftW,
                h
        };
    }

    private float actionX(L l, int index) {
        return l.rightX + index * (l.btnW + l.btnGap);
    }

    private float[] addCardRect(L l) {
        float w = 150.0F * l.u;
        float h = 78.0F * l.u;
        return new float[]{
                l.wx + (l.ww - w) * 0.5F,
                l.wy + (l.wh - h) * 0.5F,
                w,
                h
        };
    }

    private float[] addFieldRect(L l, int index) {
        float[] card = addCardRect(l);
        float y = index == 0 ? card[1] + 28.0F * l.u : card[1] + 48.0F * l.u;
        return new float[]{card[0] + 10.0F * l.u, y, card[2] - 20.0F * l.u, 11.0F * l.u};
    }

    private static boolean inside(float[] rect, float x, float y) {
        return x >= rect[0] && x <= rect[0] + rect[2] && y >= rect[1] && y <= rect[1] + rect[3];
    }

    private static boolean inside(float x, float y, float w, float h, float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    private int accent() {
        return Theme.getAccent();
    }

    private int ac(int color) {
        return this.openAnim >= 0.999F ? color : ColorUtil.multiplyAlpha(color, this.openAnim);
    }

    private int hover(int color) {
        return ColorUtil.lerp(color, ColorUtil.WHITE, 0.08F);
    }

    private void r(float x, float y, float w, float h, int color, float radius) {
        if (w <= 0.0F || h <= 0.0F) {
            return;
        }
        Render2DUtil.rect(x, y, w, h).color(ac(color)).radius(radius).draw();
    }

    private void rb(float x, float y, float w, float h, int color, float radius, float thickness) {
        if (w <= 0.0F || h <= 0.0F) {
            return;
        }
        Render2DUtil.rect(x, y, w, h)
                .color(ColorUtil.TRANSPARENT)
                .radius(radius)
                .border(thickness, ac(color))
                .draw();
    }

    private void t(float x, float y, float size, String text, int color, UiFontStyle style) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Render2DUtil.text(x, y, size, text).color(ac(color)).style(style).draw();
    }

    private void tCenter(float cx, float y, float size, String text, int color, UiFontStyle style) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Render2DUtil.text(cx, y, size, text)
                .color(ac(color))
                .style(style)
                .align(TextAlign.CENTER)
                .draw();
    }

    private void tRight(float rx, float y, float size, String text, int color, UiFontStyle style) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Render2DUtil.text(rx, y, size, text)
                .color(ac(color))
                .style(style)
                .align(TextAlign.RIGHT)
                .draw();
    }

    private void icon(float x, float y, float size, Identifier id, int color) {
        Render2DUtil.texture(x, y, size, size, id).color(ac(color)).draw();
    }

    private float tw(String text, float size, UiFontStyle style) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        return UiFonts.getFontForText(text, style.weight())
                .measureWidth(text, size, size * style.letterSpacingEm());
    }

    private String trunc(String text, float size, UiFontStyle style, float maxWidth) {
        if (text == null || text.isEmpty() || tw(text, size, style) <= maxWidth) {
            return text == null ? "" : text;
        }
        String ellipsis = "...";
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) / 2;
            if (tw(text.substring(0, mid) + ellipsis, size, style) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low) + ellipsis;
    }

    private float cy(String sample, float centerY, float size, UiFontStyle style) {
        String value = sample == null || sample.isEmpty() ? " " : sample;
        return UiFonts.getFontForText(value, style.weight()).centeredTextY(centerY, size);
    }

    private void button(float x, float y, float w, float h, String label, int bg, int fg, float radius, boolean hovered) {
        r(x, y, w, h, hovered ? hover(bg) : bg, radius);
        float size = 5.2F * layoutU();
        tCenter(x + w * 0.5F, cy(label, y + h * 0.5F, size, UiFontStyle.MEDIUM), size, label, fg, UiFontStyle.MEDIUM);
    }

    private float uNow;

    private float layoutU() {
        return this.uNow;
    }

    private void field(L l, float x, float y, float w, float h, String text, String placeholder,
                       boolean focused, boolean caret) {
        r(x, y, w, h, FIELD, 4.0F * l.u);
        rb(x, y, w, h,
                focused ? ColorUtil.withAlpha(accent(), 170) : ColorUtil.rgba(255, 255, 255, 10),
                4.0F * l.u, 1.0F);
        float size = 5.0F * l.u;
        float pad = 6.0F * l.u;
        if (text.isEmpty()) {
            t(x + pad, cy(placeholder, y + h * 0.5F, size, UiFontStyle.REGULAR), size, placeholder, FAINT, UiFontStyle.REGULAR);
            return;
        }
        String visible = trunc(text, size, UiFontStyle.MEDIUM, w - pad * 2.0F - 2.0F * l.u);
        t(x + pad, cy(visible, y + h * 0.5F, size, UiFontStyle.MEDIUM), size, visible, TEXT, UiFontStyle.MEDIUM);
        if (caret && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float caretX = x + pad + tw(visible, size, UiFontStyle.MEDIUM) + 1.0F;
            r(caretX, y + 2.5F * l.u, 1.0F, h - 5.0F * l.u, TEXT, 0.0F);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        this.openAnim += (1.0F - this.openAnim) * 0.22F;
        if (this.openAnim > 0.995F) {
            this.openAnim = 1.0F;
        }
        gfx.fill(0, 0, this.width, this.height, ac(BG));
        Minecraft mc = Minecraft.getInstance();
        RenderContext.enter2D(mc.gui, gfx, null);
        try {
            Render2DUtil.beginFrame();
            this.mouseXf = mouseX;
            this.mouseYf = mouseY;
            L l = layout();
            this.uNow = l.u;
            List<InstanceBotManager.Bot> bots = this.manager.bots();
            for (InstanceBotManager.Bot bot : bots) {
                this.manager.refresh(bot);
            }
            ensureSelection(bots);
            clampScrolls(l, bots);
            renderWindow(l, bots);
            if (this.adding) {
                renderAddDialog(l);
            }
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }

    private void ensureSelection(List<InstanceBotManager.Bot> bots) {
        if (this.selected != null && bots.contains(this.selected)) {
            return;
        }
        List<InstanceBotManager.Bot> visible = visible(bots);
        this.selected = visible.isEmpty() ? null : visible.get(0);
        this.logBot = null;
    }

    private List<InstanceBotManager.Bot> visible(List<InstanceBotManager.Bot> bots) {
        String query = InstanceBotManager.lower(this.search.trim());
        if (query.isEmpty()) {
            return new ArrayList<>(bots);
        }
        List<InstanceBotManager.Bot> result = new ArrayList<>();
        for (InstanceBotManager.Bot bot : bots) {
            if (InstanceBotManager.lower(bot.name).contains(query)
                    || InstanceBotManager.lower(bot.server).contains(query)) {
                result.add(bot);
            }
        }
        return result;
    }

    private void clampScrolls(L l, List<InstanceBotManager.Bot> bots) {
        float cardH = cardHeight(l);
        float contentH = visible(bots).size() * (cardH + cardGap(l)) - cardGap(l);
        float viewH = l.listBottom - l.listTop;
        float maxList = Math.max(0.0F, contentH - viewH);
        if (this.listScroll > maxList) {
            this.listScroll = maxList;
        }
        if (this.listScroll < 0.0F) {
            this.listScroll = 0.0F;
        }
    }

    private void renderWindow(L l, List<InstanceBotManager.Bot> bots) {
        float radius = 10.0F * l.u;
        Render2DUtil.rect(l.wx, l.wy, l.ww, l.wh)
                .color(ac(WIN))
                .radius(radius)
                .shadow(ac(ColorUtil.rgba(0, 0, 0, 120)), 20.0F * l.u)
                .draw();
        rb(l.wx, l.wy, l.ww, l.wh, ColorUtil.rgba(255, 255, 255, 9), radius, 1.0F);

        renderSidebar(l);
        renderHeader(l);

        r(l.wx + l.sideW, l.wy + l.headerH - 1.0F, l.ww - l.sideW, 1.0F,
                ColorUtil.rgba(255, 255, 255, 8), 0.0F);

        renderList(l, bots);
        renderDetail(l, bots);
        renderToast(l);
    }

    private void renderSidebar(L l) {
        r(l.wx, l.wy, l.sideW, l.wh, SIDE, 10.0F * l.u);
        r(l.wx + l.sideW * 0.5F, l.wy, l.sideW * 0.5F + 1.0F, l.wh, SIDE, 0.0F);

        float logoSize = 12.0F * l.u;
        icon(l.wx + (l.sideW - logoSize) * 0.5F, l.wy + 6.0F * l.u, logoSize, Textures.Logos.BOLT, accent());

        for (int i = 0; i < SIDE_ICONS.length; i++) {
            float cy = l.wy + 46.0F * l.u + i * 20.0F * l.u;
            float cx = l.wx + l.sideW * 0.5F;
            boolean active = i == SIDE_ICONS.length - 1;
            boolean hovered = inside(cx - 8.0F * l.u, cy - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u,
                    this.mouseXf, this.mouseYf);
            if (active) {
                r(cx - 8.0F * l.u, cy - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u,
                        ColorUtil.withAlpha(accent(), 46), 5.0F * l.u);
                rb(cx - 8.0F * l.u, cy - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u,
                        ColorUtil.withAlpha(accent(), 110), 5.0F * l.u, 1.0F);
            } else if (hovered) {
                r(cx - 8.0F * l.u, cy - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u,
                        ColorUtil.rgba(255, 255, 255, 8), 5.0F * l.u);
            }
            float iconSize = 7.5F * l.u;
            int color = active ? accent() : hovered ? TEXT : FAINT;
            icon(cx - iconSize * 0.5F, cy - iconSize * 0.5F, iconSize, SIDE_ICONS[i], color);
        }

        float bottomY = l.wy + l.wh - 18.0F * l.u;
        float bottomCx = l.wx + l.sideW * 0.5F;
        boolean bottomHover = inside(bottomCx - 8.0F * l.u, bottomY - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u,
                this.mouseXf, this.mouseYf);
        if (bottomHover) {
            r(bottomCx - 8.0F * l.u, bottomY - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u,
                    ColorUtil.rgba(255, 255, 255, 8), 8.0F * l.u);
        }
        rb(bottomCx - 7.5F * l.u, bottomY - 7.5F * l.u, 15.0F * l.u, 15.0F * l.u,
                ColorUtil.rgba(255, 255, 255, 16), 7.5F * l.u, 1.0F);
        icon(bottomCx - 3.5F * l.u, bottomY - 3.5F * l.u, 7.0F * l.u,
                Textures.Icons.USER_ROUND, bottomHover ? TEXT : FAINT);
    }

    private void renderHeader(L l) {
        float cyBase = l.wy + l.headerH * 0.5F;
        String crumb = "Blade  ›";
        float crumbSize = 4.9F * l.u;
        float crumbX = l.wx + l.sideW + 9.0F * l.u;
        t(crumbX, cy(crumb, cyBase, crumbSize, UiFontStyle.REGULAR), crumbSize, crumb, FAINT, UiFontStyle.REGULAR);
        float titleSize = 5.9F * l.u;
        float titleX = crumbX + tw(crumb, crumbSize, UiFontStyle.REGULAR) + 5.0F * l.u;
        t(titleX, cy("Bots", cyBase, titleSize, UiFontStyle.SEMIBOLD), titleSize, "Bots", TEXT, UiFontStyle.SEMIBOLD);

        boolean mainHover = inside(l.mainBtnX, l.mainBtnY, l.mainBtnW, l.mainBtnH, this.mouseXf, this.mouseYf);
        button(l.mainBtnX, l.mainBtnY, l.mainBtnW, l.mainBtnH, "К основному",
                mainHover ? CONTROL_HOVER : CONTROL, TEXT, 6.0F * l.u, false);

        boolean chipHover = inside(l.chipX, l.chipY, l.chipW, l.chipH, this.mouseXf, this.mouseYf);
        r(l.chipX, l.chipY, l.chipW, l.chipH, chipHover ? hover(CHIP) : CHIP, 6.0F * l.u);
        rb(l.chipX, l.chipY, l.chipW, l.chipH, ColorUtil.rgba(255, 255, 255, 14), 6.0F * l.u, 1.0F);
        String chipText = "RSHIFT  ×";
        float chipSize = 4.8F * l.u;
        tCenter(l.chipX + l.chipW * 0.5F, cy(chipText, l.chipY + l.chipH * 0.5F, chipSize, UiFontStyle.MEDIUM),
                chipSize, chipText, TEXT, UiFontStyle.MEDIUM);

        boolean searchHover = inside(l.searchX, l.searchY, l.searchW, l.searchH, this.mouseXf, this.mouseYf);
        r(l.searchX, l.searchY, l.searchW, l.searchH, FIELD, 6.0F * l.u);
        rb(l.searchX, l.searchY, l.searchW, l.searchH,
                this.searchFocus
                        ? ColorUtil.withAlpha(accent(), 170)
                        : searchHover ? ColorUtil.rgba(255, 255, 255, 18) : ColorUtil.rgba(255, 255, 255, 10),
                6.0F * l.u, 1.0F);
        float iconSize = 5.5F * l.u;
        icon(l.searchX + 5.0F * l.u, l.searchY + (l.searchH - iconSize) * 0.5F, iconSize,
                Textures.Header.SEARCH, this.searchFocus ? accent() : FAINT);
        float fieldSize = 4.9F * l.u;
        float textX = l.searchX + 5.0F * l.u + iconSize + 4.0F * l.u;
        float maxW = l.searchX + l.searchW - 5.0F * l.u - textX;
        if (this.search.isEmpty() && !this.searchFocus) {
            t(textX, cy("Поиск ботов...", l.searchY + l.searchH * 0.5F, fieldSize, UiFontStyle.REGULAR),
                    fieldSize, "Поиск ботов...", FAINT, UiFontStyle.REGULAR);
        } else {
            String visible = trunc(this.search, fieldSize, UiFontStyle.REGULAR, maxW);
            t(textX, cy(visible, l.searchY + l.searchH * 0.5F, fieldSize, UiFontStyle.REGULAR),
                    fieldSize, visible, TEXT, UiFontStyle.REGULAR);
            if (this.searchFocus && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                float caretX = textX + tw(visible, fieldSize, UiFontStyle.REGULAR) + 1.0F;
                r(caretX, l.searchY + 2.5F * l.u, 1.0F, l.searchH - 5.0F * l.u, TEXT, 0.0F);
            }
        }
    }

    private void renderList(L l, List<InstanceBotManager.Bot> bots) {
        float sectionSize = 6.0F * l.u;
        String section = "Боты:" + visible(bots).size();
        t(l.leftX, cy(section, l.headerRowY + l.headerRowH * 0.5F, sectionSize, UiFontStyle.SEMIBOLD),
                sectionSize, section, TEXT, UiFontStyle.SEMIBOLD);

        boolean hostHover = inside(l.hostX, l.headBtnY, l.hostW, l.headBtnH, this.mouseXf, this.mouseYf);
        button(l.hostX, l.headBtnY, l.hostW, l.headBtnH, "Host", CONTROL, TEXT, 4.5F * l.u, hostHover);
        boolean addHover = inside(l.addX, l.headBtnY, l.addW, l.headBtnH, this.mouseXf, this.mouseYf);
        button(l.addX, l.headBtnY, l.addW, l.headBtnH, "+ Добавить", CONTROL, TEXT, 4.5F * l.u, addHover);

        float viewH = l.listBottom - l.listTop;
        Render2DUtil.pushScissor(l.leftX, l.listTop, l.leftW, viewH);
        try {
            List<InstanceBotManager.Bot> visible = visible(bots);
            if (visible.isEmpty()) {
                float size = 4.9F * l.u;
                String hint = this.search.isEmpty() ? "Нет ботов — нажми «+ Добавить»" : "Ничего не найдено";
                tCenter(l.leftX + l.leftW * 0.5F,
                        cy(hint, l.listTop + viewH * 0.5F, size, UiFontStyle.REGULAR),
                        size, hint, FAINT, UiFontStyle.REGULAR);
            }
            for (int i = 0; i < visible.size(); i++) {
                renderCard(l, visible.get(i), cardRect(l, i), this.selected == visible.get(i));
            }
        } finally {
            Render2DUtil.popScissor();
        }
    }

    private void renderCard(L l, InstanceBotManager.Bot bot, float[] rect, boolean selected) {
        float x = rect[0];
        float y = rect[1];
        float w = rect[2];
        float h = rect[3];
        boolean hovered = inside(x, y, w, h, this.mouseXf, this.mouseYf);
        int bg = selected ? CARD_SEL : hovered ? CARD_HOVER : CARD;
        r(x, y, w, h, bg, 6.0F * l.u);
        if (selected) {
            rb(x, y, w, h, ColorUtil.lerp(ColorUtil.rgba(255, 255, 255, 16), accent(), 0.45F), 6.0F * l.u, 1.0F);
        } else {
            rb(x, y, w, h, ColorUtil.rgba(255, 255, 255, 7), 6.0F * l.u, 1.0F);
        }

        int statusColor = InstanceBotManager.statusColor(bot.status);
        float dot = 3.5F * l.u;
        r(x + 6.0F * l.u, y + 5.5F * l.u, dot, dot, statusColor, dot);

        float nameSize = 5.5F * l.u;
        float textX = x + 6.0F * l.u + dot + 4.0F * l.u;
        float textW = w - (textX - x) - 5.0F * l.u;
        t(textX, cy(bot.name, y + 6.5F * l.u, nameSize, UiFontStyle.SEMIBOLD),
                nameSize, trunc(bot.name, nameSize, UiFontStyle.SEMIBOLD, textW), TEXT, UiFontStyle.SEMIBOLD);
        float subSize = 4.5F * l.u;
        t(textX, cy(bot.server, y + 12.5F * l.u, subSize, UiFontStyle.REGULAR),
                subSize, trunc(bot.server, subSize, UiFontStyle.REGULAR, textW), DIM, UiFontStyle.REGULAR);
        t(textX, cy(bot.statusText, y + 17.5F * l.u, subSize, UiFontStyle.REGULAR),
                subSize, trunc(bot.statusText, subSize, UiFontStyle.REGULAR, textW), statusColor, UiFontStyle.REGULAR);
    }

    private void renderDetail(L l, List<InstanceBotManager.Bot> bots) {
        if (this.selected == null) {
            float size = 5.2F * l.u;
            String hint = "Выбери бота";
            float cy = l.wy + l.headerH + 40.0F * l.u;
            tCenter(l.rightX + l.rightW * 0.5F, cy(hint, cy, size, UiFontStyle.REGULAR), size, hint, FAINT, UiFontStyle.REGULAR);
            return;
        }
        InstanceBotManager.Bot bot = this.selected;
        int statusColor = InstanceBotManager.statusColor(bot.status);

        float dot = 4.0F * l.u;
        r(l.rightX, l.nameY + 1.0F * l.u, dot, dot, statusColor, dot);
        float nameSize = 6.4F * l.u;
        float nameX = l.rightX + dot + 4.5F * l.u;
        float nameMax = l.rightW - (nameX - l.rightX) - l.delW - 6.0F * l.u;
        t(nameX, cy(bot.name, l.nameY + 4.5F * l.u, nameSize, UiFontStyle.SEMIBOLD),
                nameSize, trunc(bot.name, nameSize, UiFontStyle.SEMIBOLD, nameMax), TEXT, UiFontStyle.SEMIBOLD);

        boolean delHover = inside(delX(l), l.delY, l.delW, l.delH, this.mouseXf, this.mouseYf);
        r(delX(l), l.delY, l.delW, l.delH, delHover ? DANGER_BG_HOVER : DANGER_BG, 4.5F * l.u);
        float delSize = 4.9F * l.u;
        tCenter(delX(l) + l.delW * 0.5F,
                cy("Удалить", l.delY + l.delH * 0.5F, delSize, UiFontStyle.MEDIUM),
                delSize, "Удалить", RED, UiFontStyle.MEDIUM);

        float subSize = 4.8F * l.u;
        float subCy = l.subY + 4.0F * l.u;
        String serverPart = bot.server + "  ·  ";
        t(nameX, cy(serverPart, subCy, subSize, UiFontStyle.REGULAR), subSize, serverPart, DIM, UiFontStyle.REGULAR);
        float statusX = nameX + tw(serverPart, subSize, UiFontStyle.REGULAR);
        t(statusX, cy(bot.statusText, subCy, subSize, UiFontStyle.REGULAR), subSize,
                trunc(bot.statusText, subSize, UiFontStyle.REGULAR, l.contentRight - statusX), statusColor, UiFontStyle.REGULAR);

        Minecraft mc = Minecraft.getInstance();
        boolean inGame = mc.level != null && mc.player != null && this.manager.isActive(bot);

        String[] labels = {"Управлять", "Модули", "Реконнект", "Отключить"};
        for (int i = 0; i < labels.length; i++) {
            float x = actionX(l, i);
            boolean hovered = inside(x, l.btnY, l.btnW, l.btnH, this.mouseXf, this.mouseYf);
            int bg;
            int fg;
            if (i == 0) {
                bg = inGame ? CONTROL : PRIMARY;
                fg = inGame ? DIM : TEXT;
            } else if (i == 3) {
                bg = DANGER_BG;
                fg = RED;
            } else {
                bg = CONTROL;
                fg = TEXT;
            }
            if (i == 0 && inGame && hovered) {
                bg = CONTROL_HOVER;
            } else if (i == 3) {
                bg = hovered ? DANGER_BG_HOVER : DANGER_BG;
            } else {
                bg = hovered ? (i == 0 ? PRIMARY_HOVER : CONTROL_HOVER) : bg;
            }
            button(x, l.btnY, l.btnW, l.btnH, labels[i], bg, fg, 4.5F * l.u, false);
        }

        float msgW = l.rightW - l.sendW - 4.0F * l.u;
        boolean msgHover = inside(l.rightX, l.msgY, msgW, l.msgH, this.mouseXf, this.mouseYf);
        r(l.rightX, l.msgY, msgW, l.msgH, FIELD, 4.5F * l.u);
        rb(l.rightX, l.msgY, msgW, l.msgH,
                this.messageFocus
                        ? ColorUtil.withAlpha(accent(), 170)
                        : msgHover ? ColorUtil.rgba(255, 255, 255, 18) : ColorUtil.rgba(255, 255, 255, 10),
                4.5F * l.u, 1.0F);
        float msgSize = 4.9F * l.u;
        float msgPad = 6.0F * l.u;
        if (this.message.isEmpty() && !this.messageFocus) {
            t(l.rightX + msgPad, cy("Сообщение или команда", l.msgY + l.msgH * 0.5F, msgSize, UiFontStyle.REGULAR),
                    msgSize, "Сообщение или команда", FAINT, UiFontStyle.REGULAR);
        } else {
            String visible = trunc(this.message, msgSize, UiFontStyle.MEDIUM, msgW - msgPad * 2.0F - 2.0F * l.u);
            t(l.rightX + msgPad, cy(visible, l.msgY + l.msgH * 0.5F, msgSize, UiFontStyle.MEDIUM),
                    msgSize, visible, TEXT, UiFontStyle.MEDIUM);
            if (this.messageFocus && (System.currentTimeMillis() / 500L) % 2L == 0L) {
                float caretX = l.rightX + msgPad + tw(visible, msgSize, UiFontStyle.MEDIUM) + 1.0F;
                r(caretX, l.msgY + 2.5F * l.u, 1.0F, l.msgH - 5.0F * l.u, TEXT, 0.0F);
            }
        }

        float sendX = l.rightX + msgW + 4.0F * l.u;
        boolean sendHover = inside(sendX, l.msgY, l.sendW, l.msgH, this.mouseXf, this.mouseYf);
        button(sendX, l.msgY, l.sendW, l.msgH, "Отправить",
                sendHover ? PRIMARY_HOVER : PRIMARY, TEXT, 4.5F * l.u, false);

        if (inGame) {
            renderOnline(l, mc);
        } else {
            renderLog(l, bot);
        }
    }

    private float delX(L l) {
        return l.contentRight - l.delW;
    }

    private void renderOnline(L l, Minecraft mc) {
        var player = mc.player;
        float statSize = 4.8F * l.u;
        float statCy = l.statsY + 4.0F * l.u;
        String hp = String.format(java.util.Locale.ROOT, "HP %.1f / %.1f", player.getHealth(), player.getMaxHealth());
        String food = "Еда " + player.getFoodData().getFoodLevel();
        String xp = "XP " + player.experienceLevel;
        var pos = player.blockPosition();
        String xyz = "XYZ " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        float[] frac = {0.02F, 0.36F, 0.60F, 0.80F};
        String[] values = {hp, food, xp, xyz};
        int[] colors = {GREEN, AMBER, DIM, DIM};
        for (int i = 0; i < values.length; i++) {
            float x = l.rightX + l.rightW * frac[i];
            t(x, cy(values[i], statCy, statSize, UiFontStyle.MEDIUM), statSize, values[i], colors[i], UiFontStyle.MEDIUM);
        }

        boolean invHover = inside(l.rightX + (l.rightW - l.invW) * 0.5F, l.invY, l.invW, l.invH,
                this.mouseXf, this.mouseYf);
        button(l.rightX + (l.rightW - l.invW) * 0.5F, l.invY, l.invW, l.invH, "Инвентарь",
                invHover ? CONTROL_HOVER : CONTROL, TEXT, 4.5F * l.u, false);

        if (!this.showInventory) {
            return;
        }

        float cell = 12.0F * l.u;
        float slot = 11.0F * l.u;
        float gridW = cell * 9.0F;
        float gx = l.rightX + (l.rightW - gridW) * 0.5F;
        float itemScale = (slot - 2.0F * l.u) / 16.0F;
        GuiGraphicsExtractor gfx = RenderContext.currentGuiGraphicsExtractor();

        for (int row = 0; row < 4; row++) {
            boolean hotbar = row == 3;
            float gy = l.gridY + row * cell + (hotbar ? 1.5F * l.u : 0.0F);
            for (int col = 0; col < 9; col++) {
                int index = hotbar ? col : 9 + row * 9 + col;
                float sx = gx + col * cell + (cell - slot) * 0.5F;
                float sy = gy + (cell - slot) * 0.5F;
                r(sx, sy, slot, slot, FIELD, 2.0F * l.u);
                rb(sx, sy, slot, slot, ColorUtil.rgba(255, 255, 255, 7), 2.0F * l.u, 1.0F);
                ItemStack stack = player.getInventory().getItem(index);
                if (stack.isEmpty() || gfx == null) {
                    continue;
                }
                var pose = gfx.pose();
                pose.pushMatrix();
                try {
                    pose.translate(sx + 1.0F * l.u, sy + 1.0F * l.u);
                    pose.scale(itemScale, itemScale);
                    gfx.item(stack, 0, 0);
                } finally {
                    pose.popMatrix();
                }
            }
        }
    }

    private void renderLog(L l, InstanceBotManager.Bot bot) {
        float lineH = 5.6F * l.u;
        float size = 4.5F * l.u;
        float viewH = l.logBottom - l.logTop;
        if (viewH <= 0.0F) {
            return;
        }
        int count = bot.log.size();
        float contentH = count * lineH;
        float maxScroll = Math.max(0.0F, contentH - viewH);
        if (this.logBot != bot || this.logCount != count) {
            this.logBot = bot;
            this.logCount = count;
            this.logScroll = maxScroll;
        }
        if (this.logScroll > maxScroll) {
            this.logScroll = maxScroll;
        }
        if (this.logScroll < 0.0F) {
            this.logScroll = 0.0F;
        }

        if (count == 0) {
            return;
        }
        Render2DUtil.pushScissor(l.rightX, l.logTop, l.rightW, viewH);
        try {
            for (int i = 0; i < count; i++) {
                InstanceBotManager.LogLine line = bot.log.get(i);
                float y = l.logTop + i * lineH - this.logScroll;
                if (y + lineH < l.logTop || y > l.logBottom) {
                    continue;
                }
                String visible = trunc(line.text, size, UiFontStyle.REGULAR, l.rightW - 2.0F * l.u);
                t(l.rightX, cy(visible, y + lineH * 0.5F, size, UiFontStyle.REGULAR),
                        size, visible, line.color, UiFontStyle.REGULAR);
            }
        } finally {
            Render2DUtil.popScissor();
        }
    }

    private void renderToast(L l) {
        if (!this.manager.hasToast()) {
            return;
        }
        float size = 4.9F * l.u;
        float padding = 8.0F * l.u;
        float w = tw(this.manager.toastText, size, UiFontStyle.MEDIUM) + padding * 2.0F;
        float x = l.rightX + (l.rightW - w) * 0.5F;
        r(x, l.toastY, w, l.toastH, this.manager.toastColor, l.toastH * 0.5F);
        tCenter(x + w * 0.5F,
                cy(this.manager.toastText, l.toastY + l.toastH * 0.5F, size, UiFontStyle.MEDIUM),
                size, this.manager.toastText, ColorUtil.rgb(12, 14, 18), UiFontStyle.MEDIUM);
    }

    private void renderAddDialog(L l) {
        r(l.wx, l.wy, l.ww, l.wh, ColorUtil.rgba(0, 0, 0, 140), 10.0F * l.u);
        float[] card = addCardRect(l);
        Render2DUtil.rect(card[0], card[1], card[2], card[3])
                .color(ac(ColorUtil.rgb(24, 25, 33)))
                .radius(8.0F * l.u)
                .shadow(ac(ColorUtil.rgba(0, 0, 0, 120)), 16.0F * l.u)
                .draw();
        rb(card[0], card[1], card[2], card[3], ColorUtil.rgba(255, 255, 255, 12), 8.0F * l.u, 1.0F);

        float titleSize = 6.0F * l.u;
        t(card[0] + 10.0F * l.u,
                cy("Новый бот", card[1] + 10.0F * l.u, titleSize, UiFontStyle.SEMIBOLD),
                titleSize, "Новый бот", TEXT, UiFontStyle.SEMIBOLD);

        float labelSize = 4.5F * l.u;
        float[] nameField = addFieldRect(l, 0);
        t(nameField[0], cy("Имя", nameField[1] - 4.0F * l.u, labelSize, UiFontStyle.REGULAR),
                labelSize, "Имя", DIM, UiFontStyle.REGULAR);
        field(l, nameField[0], nameField[1], nameField[2], nameField[3],
                this.addName, "bot_1234", this.adding && this.addField == 0, this.addField == 0);

        float[] serverField = addFieldRect(l, 1);
        t(serverField[0], cy("Сервер", serverField[1] - 4.0F * l.u, labelSize, UiFontStyle.REGULAR),
                labelSize, "Сервер", DIM, UiFontStyle.REGULAR);
        field(l, serverField[0], serverField[1], serverField[2], serverField[3],
                this.addServer, "funtime.su", this.adding && this.addField == 1, this.addField == 1);

        float btnY = card[1] + card[3] - 8.0F * l.u - 10.0F * l.u;
        float btnH = 10.0F * l.u;
        float cancelW = 36.0F * l.u;
        float submitW = 46.0F * l.u;
        float cancelX = card[0] + 10.0F * l.u;
        float submitX = card[0] + card[2] - 10.0F * l.u - submitW;
        boolean cancelHover = inside(cancelX, btnY, cancelW, btnH, this.mouseXf, this.mouseYf);
        button(cancelX, btnY, cancelW, btnH, "Отмена", CONTROL, TEXT, 4.5F * l.u, cancelHover);
        boolean submitHover = inside(submitX, btnY, submitW, btnH, this.mouseXf, this.mouseYf);
        button(submitX, btnY, submitW, btnH, "Добавить", submitHover ? hover(accent()) : accent(),
                ColorUtil.rgb(14, 15, 20), 4.5F * l.u, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
        if (event.button() != 0) {
            return super.mouseClicked(event, flag);
        }
        float x = (float) event.x();
        float y = (float) event.y();
        this.mouseXf = x;
        this.mouseYf = y;
        L l = layout();

        if (this.adding) {
            handleClickAdd(l, x, y);
            return true;
        }

        float chipHit = 6.0F * l.u;
        if (inside(l.chipX - chipHit, l.chipY - chipHit, l.chipW + chipHit * 2.0F, l.chipH + chipHit * 2.0F, x, y)) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        if (inside(l.mainBtnX, l.mainBtnY, l.mainBtnW, l.mainBtnH, x, y)) {
            this.manager.backToMain();
            return true;
        }
        if (inside(l.searchX, l.searchY, l.searchW, l.searchH, x, y)) {
            this.searchFocus = true;
            this.messageFocus = false;
            return true;
        }

        float sideCx = l.wx + l.sideW * 0.5F;
        for (int i = 0; i < SIDE_ICONS.length; i++) {
            float cy = l.wy + 46.0F * l.u + i * 20.0F * l.u;
            if (inside(sideCx - 8.0F * l.u, cy - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u, x, y)) {
                this.manager.showToast("Раздел недоступен", GRAY);
                return true;
            }
        }
        float bottomY = l.wy + l.wh - 18.0F * l.u;
        if (inside(sideCx - 8.0F * l.u, bottomY - 8.0F * l.u, 16.0F * l.u, 16.0F * l.u, x, y)) {
            this.manager.showToast("Профиль недоступен", GRAY);
            return true;
        }

        this.searchFocus = false;
        this.messageFocus = false;

        if (inside(l.hostX, l.headBtnY, l.hostW, l.headBtnH, x, y)) {
            this.selected = this.manager.host();
            this.listScroll = 0.0F;
            this.logBot = null;
            return true;
        }
        if (inside(l.addX, l.headBtnY, l.addW, l.headBtnH, x, y)) {
            this.adding = true;
            this.addName = "";
            this.addServer = this.manager.defaultServer();
            this.addField = 0;
            return true;
        }

        if (this.selected != null) {
            if (inside(delX(l), l.delY, l.delW, l.delH, x, y)) {
                this.manager.remove(this.selected);
                this.selected = null;
                this.logBot = null;
                this.manager.showToast("Бот удалён", GRAY);
                return true;
            }
            String[] actions = {"control", "modules", "reconnect", "disconnect"};
            for (int i = 0; i < actions.length; i++) {
                float bx = actionX(l, i);
                if (inside(bx, l.btnY, l.btnW, l.btnH, x, y)) {
                    handleAction(actions[i]);
                    return true;
                }
            }
            float msgW = l.rightW - l.sendW - 4.0F * l.u;
            if (inside(l.rightX, l.msgY, msgW, l.msgH, x, y)) {
                this.messageFocus = true;
                this.searchFocus = false;
                return true;
            }
            float sendX = l.rightX + msgW + 4.0F * l.u;
            if (inside(sendX, l.msgY, l.sendW, l.msgH, x, y)) {
                sendMessage();
                return true;
            }
            float invX = l.rightX + (l.rightW - l.invW) * 0.5F;
            boolean inGame = Minecraft.getInstance().level != null
                    && Minecraft.getInstance().player != null
                    && this.manager.isActive(this.selected);
            if (inGame && inside(invX, l.invY, l.invW, l.invH, x, y)) {
                this.showInventory = !this.showInventory;
                return true;
            }
        }

        if (x >= l.leftX && x <= l.leftX + l.leftW && y >= l.listTop && y <= l.listBottom) {
            List<InstanceBotManager.Bot> visible = visible(this.manager.bots());
            for (int i = 0; i < visible.size(); i++) {
                if (inside(cardRect(l, i), x, y)) {
                    this.selected = visible.get(i);
                    this.logBot = null;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, flag);
    }

    private void handleClickAdd(L l, float x, float y) {
        float[] nameField = addFieldRect(l, 0);
        if (inside(nameField, x, y)) {
            this.addField = 0;
            return;
        }
        float[] serverField = addFieldRect(l, 1);
        if (inside(serverField, x, y)) {
            this.addField = 1;
            return;
        }
        float[] card = addCardRect(l);
        float btnY = card[1] + card[3] - 8.0F * l.u - 10.0F * l.u;
        float btnH = 10.0F * l.u;
        float cancelW = 36.0F * l.u;
        float submitW = 46.0F * l.u;
        float cancelX = card[0] + 10.0F * l.u;
        float submitX = card[0] + card[2] - 10.0F * l.u - submitW;
        if (inside(cancelX, btnY, cancelW, btnH, x, y)) {
            this.adding = false;
            return;
        }
        if (inside(submitX, btnY, submitW, btnH, x, y)) {
            submitAdd();
        }
    }

    private void handleAction(String action) {
        InstanceBotManager.Bot bot = this.selected;
        if (bot == null) {
            return;
        }
        switch (action) {
            case "control" -> this.manager.control(bot);
            case "modules" -> this.manager.showToast("Модули: недоступно", GRAY);
            case "reconnect" -> this.manager.reconnect(bot);
            case "disconnect" -> this.manager.disconnectBot(bot);
            default -> {
            }
        }
    }

    private void sendMessage() {
        if (this.selected == null) {
            return;
        }
        String text = this.message.trim();
        if (text.isEmpty()) {
            return;
        }
        if (this.manager.sendMessage(this.selected, text)) {
            this.message = "";
            this.messageFocus = false;
        }
    }

    private void submitAdd() {
        String name = this.addName.trim();
        String server = this.addServer.trim();
        if (!name.matches("^[a-zA-Z0-9_]{3,16}$")) {
            this.manager.showToast("Имя: 3-16 символов (a-z, A-Z, 0-9, _)", RED);
            return;
        }
        if (server.isEmpty()) {
            this.manager.showToast("Укажи сервер", RED);
            return;
        }
        if (this.manager.findByName(name) != null) {
            this.manager.showToast("Бот с таким именем уже есть", RED);
            return;
        }
        this.selected = this.manager.add(name, server);
        this.adding = false;
        this.listScroll = 0.0F;
        this.logBot = null;
        this.manager.showToast("Бот добавлен: " + name, GREEN);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (this.adding) {
            if (key == 256) {
                this.adding = false;
                return true;
            }
            if (key == 257 || key == 335) {
                submitAdd();
                return true;
            }
            if (key == 258) {
                this.addField = this.addField == 0 ? 1 : 0;
                return true;
            }
            if (key == 259) {
                if (this.addField == 0) {
                    this.addName = dropLast(this.addName);
                } else {
                    this.addServer = dropLast(this.addServer);
                }
                return true;
            }
            if (event.hasControlDown() && key == 86) {
                pasteIntoAdd();
                return true;
            }
            return true;
        }
        if (this.searchFocus) {
            if (key == 256 || key == 257) {
                this.searchFocus = false;
                return true;
            }
            if (key == 259) {
                this.search = dropLast(this.search);
                return true;
            }
            if (key == 261 && !this.search.isEmpty()) {
                this.search = this.search.substring(0, this.search.length() - 1);
                return true;
            }
            if (event.hasControlDown() && key == 86) {
                String clip = MenuClipboard.get();
                if (clip != null && !clip.isEmpty() && this.search.length() < 32) {
                    this.search = (this.search + clip).substring(0, Math.min(32, (this.search + clip).length()));
                }
                return true;
            }
            return true;
        }
        if (this.messageFocus) {
            if (key == 256) {
                this.messageFocus = false;
                return true;
            }
            if (key == 257 || key == 335) {
                sendMessage();
                return true;
            }
            if (key == 259) {
                this.message = dropLast(this.message);
                return true;
            }
            if (key == 261 && !this.message.isEmpty()) {
                this.message = this.message.substring(0, this.message.length() - 1);
                return true;
            }
            if (event.hasControlDown() && key == 86) {
                String clip = MenuClipboard.get();
                if (clip != null && !clip.isEmpty() && this.message.length() < 160) {
                    this.message = (this.message + clip).substring(0, Math.min(160, (this.message + clip).length()));
                }
                return true;
            }
            return true;
        }
        if (key == 256) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        return super.keyPressed(event);
    }

    private static String dropLast(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, value.length() - 1);
    }

    private void pasteIntoAdd() {
        String clip = MenuClipboard.get();
        if (clip == null || clip.isEmpty()) {
            return;
        }
        if (this.addField == 0) {
            this.addName = (this.addName + clip).substring(0, Math.min(16, (this.addName + clip).length()));
        } else {
            this.addServer = (this.addServer + clip).substring(0, Math.min(64, (this.addServer + clip).length()));
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codepoint = event.codepoint();
        if (codepoint < 32) {
            return false;
        }
        String value = event.codepointAsString();
        if (this.adding) {
            if (this.addField == 0) {
                if (this.addName.length() < 16) {
                    this.addName += value;
                }
            } else if (this.addServer.length() < 64) {
                this.addServer += value;
            }
            return true;
        }
        if (this.searchFocus) {
            if (this.search.length() < 32) {
                this.search += value;
            }
            return true;
        }
        if (this.messageFocus) {
            if (this.message.length() < 160) {
                this.message += value;
            }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.adding || verticalAmount == 0.0D) {
            return true;
        }
        L l = layout();
        float x = (float) mouseX;
        float y = (float) mouseY;
        float direction = (float) verticalAmount;

        if (this.selected != null && this.manager.isActive(this.selected)
                && Minecraft.getInstance().level != null) {
            return true;
        }
        if (x >= l.rightX && x <= l.contentRight && y >= l.logTop && y <= l.logBottom) {
            InstanceBotManager.Bot bot = this.selected;
            if (bot != null) {
                float lineH = 5.6F * l.u;
                float viewH = l.logBottom - l.logTop;
                float contentH = bot.log.size() * lineH;
                float maxScroll = Math.max(0.0F, contentH - viewH);
                this.logScroll = clamp(this.logScroll - direction * lineH * 2.0F, 0.0F, maxScroll);
            }
            return true;
        }
        if (x >= l.leftX && x <= l.leftX + l.leftW && y >= l.listTop && y <= l.listBottom) {
            float cardH = cardHeight(l);
            int count = visible(this.manager.bots()).size();
            float contentH = count * (cardH + cardGap(l)) - cardGap(l);
            float maxScroll = Math.max(0.0F, contentH - (l.listBottom - l.listTop));
            this.listScroll = clamp(this.listScroll - direction * cardH * 1.5F, 0.0F, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void removed() {
        super.removed();
        InstanceBotFeature feature = InstanceBotFeature.INSTANCE;
        if (feature == null) {
            return;
        }
        if (this.manager.consumeNavigating()) {
            feature.markPendingReopen();
        } else if (feature.isEnabled()) {
            feature.setEnabled(false);
        }
    }
}
