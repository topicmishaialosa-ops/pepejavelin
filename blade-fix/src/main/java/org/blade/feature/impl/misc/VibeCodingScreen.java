package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ArrayListDeque;
import org.blade.menu.ui.controls.MenuClipboard;
import org.blade.utils.ColorUtil;
import org.blade.utils.ai.ZaiAi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VibeCodingScreen extends Screen {
    private static final int MAX_INPUT = 500;
    private static final int MAX_RETRIES = 4;

    private static final List<VibeSession> SESSIONS = new ArrayList<>();
    private static int activeSessionIdx = -1;

    protected final VibeCoding module = VibeCoding.INSTANCE;
    protected boolean style2;

    protected String input = "";
    protected int cursor = 0;
    protected boolean focused = true;
    protected boolean waiting = false;
    protected float openAnim = 0.0F;

    protected boolean showingApiKeyInput = false;
    protected String apiKeyInput = "";
    protected int apiKeyCursor = 0;

    protected boolean showingColorPicker = false;
    protected float pickerHue;
    protected float pickerSat;
    protected float pickerVal;
    protected boolean draggingSV = false;
    protected boolean draggingHue = false;

    protected boolean showingModelSelector = false;
    protected List<ZaiAi.ModelInfo> availableModels = new ArrayList<>();
    protected boolean loadingModels = false;
    protected float modelScroll = 0.0F;

    protected int COL_BG;
    protected int COL_PANEL;
    protected int COL_SIDEBAR;
    protected int COL_SURFACE;
    protected int COL_BORDER;
    protected int COL_TEXT;
    protected int COL_TEXT_DIM;
    protected int COL_TEXT_DARK;

    protected VibeCodingScreen() {
        this(false);
    }

    protected VibeCodingScreen(boolean style2) {
        super(Component.literal("VibeCoding"));
        this.style2 = style2;
        this.initPalette();
        if (VibeCodingScreen.SESSIONS.isEmpty()) {
            VibeSession s = new VibeSession("Сессия 1");
            s.addMessage(false,
                    "Привет! Я сгенерирую скрипт для Blade Client.\n"
                            + "Выбери провайдера и введи API-ключ, затем напиши запрос.\n"
                            + "Код сохраню в файл, а здесь напишу что сделано.\n"
                            + "Кнопка «Stacktrace» отправит последнюю ошибку нейросети для исправления.");
            VibeCodingScreen.SESSIONS.add(s);
            VibeCodingScreen.activeSessionIdx = 0;
        }
        int c = this.module.accent.getValue();
        float[] hsv = ColorUtil.hsv(c);
        this.pickerHue = hsv[0];
        this.pickerSat = hsv[1];
        this.pickerVal = hsv[2];
        this.module.autoDetectModels();
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof VibeCodingScreen) {
            return;
        }
        mc.gui.setScreen(new VibeCodingScreen());
    }

    private void initPalette() {
        if (this.style2) {
            this.COL_BG = ColorUtil.rgba(8, 8, 14, 200);
            this.COL_PANEL = ColorUtil.rgb(16, 16, 24);
            this.COL_SIDEBAR = ColorUtil.rgb(12, 12, 18);
            this.COL_SURFACE = ColorUtil.rgb(22, 22, 32);
            this.COL_BORDER = ColorUtil.rgb(45, 45, 60);
            this.COL_TEXT = ColorUtil.rgb(230, 230, 245);
            this.COL_TEXT_DIM = ColorUtil.rgb(140, 140, 165);
            this.COL_TEXT_DARK = ColorUtil.rgb(90, 90, 115);
        } else {
            this.COL_BG = ColorUtil.rgba(13, 13, 20, 200);
            this.COL_PANEL = ColorUtil.rgb(18, 18, 28);
            this.COL_SIDEBAR = ColorUtil.rgb(15, 15, 22);
            this.COL_SURFACE = ColorUtil.rgb(24, 24, 36);
            this.COL_BORDER = ColorUtil.rgb(40, 40, 56);
            this.COL_TEXT = ColorUtil.rgb(220, 220, 235);
            this.COL_TEXT_DIM = ColorUtil.rgb(130, 130, 155);
            this.COL_TEXT_DARK = ColorUtil.rgb(90, 90, 110);
        }
    }

    protected VibeSession activeSession() {
        if (VibeCodingScreen.activeSessionIdx < 0 || VibeCodingScreen.activeSessionIdx >= VibeCodingScreen.SESSIONS.size()) {
            VibeCodingScreen.activeSessionIdx = 0;
        }
        return VibeCodingScreen.SESSIONS.get(VibeCodingScreen.activeSessionIdx);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected int pad() {
        return this.style2 ? 12 : 16;
    }

    protected Layout layout() {
        boolean s2 = this.style2;
        int pw = s2 ? Math.min(900, this.width - 32) : Math.min(1050, this.width - 40);
        int ph = s2 ? Math.min(620, this.height - 32) : Math.min(700, this.height - 40);
        int px = (this.width - pw) / 2;
        int py = (this.height - ph) / 2;

        int sidebarW = s2 ? 200 : 240;
        int headerH = s2 ? 48 : 44;
        int inputH = s2 ? 42 : 44;
        int pad = this.pad();
        int inputAreaH = inputH + pad * 2;

        int chatX = px + sidebarW;
        int chatW = pw - sidebarW;
        int chatY = py;
        int chatH = ph;

        int msgTop = chatY + headerH + pad;
        int msgBottom = chatY + chatH - inputAreaH - pad;
        int maxTextW = chatW - pad * 4;

        int inputX = chatX + pad;
        int inputY = chatY + chatH - inputAreaH + (s2 ? pad / 2 : 0);
        int inputW = chatW - pad * 2;

        int sendW = s2 ? 90 : 100;
        int sendH = s2 ? 34 : inputH;
        int sendX = inputX + inputW - sendW;
        int sendY = inputY + (inputH - sendH) / 2;
        int actualInputW = inputW - sendW - (s2 ? 12 : 10);

        int closeW = s2 ? 26 : 28;
        int closeH = s2 ? 26 : 28;
        int closeX = px + pw - closeW - (s2 ? 14 : 10);
        int closeY = py + (headerH - closeH) / 2;

        int sidebarHeaderH = s2 ? 44 : 50;
        int newSessionW = sidebarW - 24;
        int newSessionH = s2 ? 30 : 32;
        int newSessionX = px + 12;
        int newSessionY = py + sidebarHeaderH + (s2 ? 10 : 8);

        return new Layout(px, py, pw, ph, sidebarW, headerH, chatX, chatW, chatY, chatH,
                msgTop, msgBottom, maxTextW, inputX, inputY, actualInputW, inputH,
                sendX, sendY, sendW, sendH, closeX, closeY, closeW, closeH,
                sidebarHeaderH, newSessionX, newSessionY, newSessionW, newSessionH);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        this.openAnim += (1.0F - this.openAnim) * (this.style2 ? 0.15F : 0.18F);
        if (this.openAnim > (this.style2 ? 0.995F : 0.997F)) {
            this.openAnim = this.style2 ? 1.0F : 1.0F;
        }
        float a = this.openAnim;
        Layout L = this.layout();
        int accent = this.module.accent.getValue();

        gfx.fill(0, 0, this.width, this.height, ColorUtil.multiplyAlpha(this.COL_BG, a));
        gfx.fill(L.px, L.py, L.px + L.pw, L.py + L.ph, ColorUtil.multiplyAlpha(this.COL_PANEL, a));
        this.border(gfx, L.px, L.py, L.pw, L.ph, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));

        gfx.fill(L.px, L.py, L.px + L.sidebarW, L.py + L.ph, ColorUtil.multiplyAlpha(this.COL_SIDEBAR, a));
        gfx.fill(L.px, L.py + L.sidebarHeaderH - 1, L.px + L.sidebarW, L.py + L.sidebarHeaderH,
                ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 12), a));

        gfx.text(this.font, "VibeCoding", L.px + 14, L.py + (L.sidebarHeaderH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        boolean newHover = L.hitNewSession(mouseX, mouseY);
        gfx.fill(L.newSessionX, L.newSessionY, L.newSessionX + L.newSessionW, L.newSessionY + L.newSessionH,
                newHover ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(8.0F * a)));
        this.border(gfx, L.newSessionX, L.newSessionY, L.newSessionW, L.newSessionH, 1,
                ColorUtil.multiplyAlpha(this.COL_BORDER, a));
        gfx.text(this.font, "+  Новая сессия", L.newSessionX + 12, L.newSessionY + (L.newSessionH - 9) / 2,
                ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 200), a));

        this.renderSessionList(gfx, L, accent, a, mouseX, mouseY);
        this.renderProviderList(gfx, L, accent, a, mouseX, mouseY);
        this.renderSidebarButtons(gfx, L, accent, a, mouseX, mouseY);
        this.renderMessages(gfx, L, accent, a);

        gfx.fill(L.chatX, L.py + L.chatH - L.inputH - this.pad() * 2, L.chatX + L.chatW, L.py + L.chatH - L.inputH - this.pad() * 2 + 1,
                ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 8), a));

        gfx.fill(L.inputX, L.inputY, L.inputX + L.inputW, L.inputY + L.inputH,
                this.focused ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(6.0F * a)));
        int inputBorder = this.focused ? ColorUtil.withAlpha(accent, Math.round(180.0F * a)) : ColorUtil.multiplyAlpha(this.COL_BORDER, a);
        this.border(gfx, L.inputX, L.inputY, L.inputW, L.inputH, 1, inputBorder);

        String shown = this.input;
        int start = 0;
        while (start < shown.length() && this.font.width(shown.substring(start)) > L.inputW - 20) {
            start++;
        }
        String visible = shown.substring(start);
        int iy = L.inputY + (L.inputH - 9) / 2;
        if (visible.isEmpty()) {
            gfx.text(this.font, "Напиши запрос...", L.inputX + 12, iy, ColorUtil.multiplyAlpha(this.COL_TEXT_DARK, a));
        } else {
            gfx.text(this.font, visible, L.inputX + 12, iy, ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 245), a));
        }
        if (this.focused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int ci = Math.max(start, Math.min(this.cursor, shown.length()));
            int cx = L.inputX + 12 + this.font.width(shown.substring(start, ci));
            gfx.fill(cx, L.inputY + 8, cx + 1, L.inputY + L.inputH - 8, ColorUtil.multiplyAlpha(ColorUtil.WHITE, a));
        }

        boolean sendHover = L.hitSend(mouseX, mouseY) && !this.waiting;
        int sendColor = sendHover ? ColorUtil.withAlpha(ColorUtil.multiplyRgb(accent, 1.15F), Math.round(255.0F * a)) : ColorUtil.withAlpha(accent, Math.round(230.0F * a));
        gfx.fill(L.sendX, L.sendY, L.sendX + L.sendW, L.sendY + L.sendH, sendColor);
        String sendLabel = this.waiting ? "..." : "Отправить";
        gfx.centeredText(this.font, sendLabel, L.sendX + L.sendW / 2, L.sendY + (L.sendH - 9) / 2, ColorUtil.WHITE);

        boolean closeHover = L.hitClose(mouseX, mouseY);
        gfx.fill(L.closeX, L.closeY, L.closeX + L.closeW, L.closeY + L.closeH,
                closeHover ? ColorUtil.rgba(255, 80, 80, Math.round(40.0F * a)) : ColorUtil.rgba(255, 255, 255, Math.round(8.0F * a)));
        gfx.centeredText(this.font, "x", L.closeX + L.closeW / 2, L.closeY + (L.closeH - 8) / 2,
                closeHover ? ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 120, 120, 255), a) : ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));

        if (this.showingColorPicker) {
            this.dragColorPicker(mouseX, mouseY);
            this.renderColorPicker(gfx, accent, a, mouseX, mouseY);
        }
        if (this.showingApiKeyInput) {
            this.renderApiKeyInput(gfx, accent, a, mouseX, mouseY);
        }
        if (this.showingModelSelector) {
            this.renderModelSelector(gfx, accent, a, mouseX, mouseY);
        }
    }

    private void renderSessionList(GuiGraphicsExtractor gfx, Layout L, int accent, float a, int mx, int my) {
        int y = L.newSessionY + L.newSessionH + 8;
        int itemH = 34;
        int itemW = L.sidebarW - 24;
        int x = L.px + 12;

        for (int i = 0; i < VibeCodingScreen.SESSIONS.size(); i++) {
            VibeSession s = VibeCodingScreen.SESSIONS.get(i);
            boolean isActive = i == VibeCodingScreen.activeSessionIdx;
            boolean hover = this.isInside(mx, my, x, y, itemW, itemH);

            if (isActive) {
                gfx.fill(x, y, x + itemW, y + itemH, ColorUtil.withAlpha(accent, Math.round(25.0F * a)));
                this.border(gfx, x, y, itemW, itemH, 1, ColorUtil.withAlpha(accent, Math.round(60.0F * a)));
            } else if (hover) {
                gfx.fill(x, y, x + itemW, y + itemH, ColorUtil.rgba(255, 255, 255, Math.round(8.0F * a)));
            }

            String label = s.getShortLabel();
            gfx.text(this.font, label, x + 10, y + 8, isActive ? ColorUtil.withAlpha(accent, Math.round(255.0F * a)) : ColorUtil.multiplyAlpha(this.COL_TEXT, a));

            String meta = s.history.size() + " msgs";
            gfx.text(this.font, meta, x + 10, y + 17, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));

            y += itemH + 3;
        }
    }

    private void renderProviderList(GuiGraphicsExtractor gfx, Layout L, int accent, float a, int mx, int my) {
        int x = L.px + 12;
        int itemW = L.sidebarW - 24;
        int y = L.newSessionY + L.newSessionH + 8 + (VibeCodingScreen.SESSIONS.size() * 37) + 14;

        gfx.text(this.font, "ПРОВАЙДЕРЫ", x + 2, y, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));
        y += 12;

        ZaiAi.Provider active = this.module.getActiveProvider();
        String activeKey = this.module.getActiveApiKey();
        boolean hasKey = activeKey != null && !activeKey.isBlank();

        for (ZaiAi.Provider prov : ZaiAi.Provider.values()) {
            boolean isActive = prov == active;
            boolean hover = this.isInside(mx, my, x, y, itemW, 28);

            if (isActive) {
                gfx.fill(x, y, x + itemW, y + 28, ColorUtil.withAlpha(accent, Math.round(25.0F * a)));
                this.border(gfx, x, y, itemW, 28, 1, ColorUtil.withAlpha(accent, Math.round(50.0F * a)));
            } else if (hover) {
                gfx.fill(x, y, x + itemW, y + 28, ColorUtil.rgba(255, 255, 255, Math.round(8.0F * a)));
            }

            gfx.text(this.font, prov.getDisplayName(), x + 10, y + 6,
                    isActive ? ColorUtil.withAlpha(accent, Math.round(255.0F * a)) : ColorUtil.multiplyAlpha(this.COL_TEXT, a));

            if (isActive) {
                String status = hasKey ? "connected" : "no key";
                gfx.text(this.font, status, x + 10, y + 17,
                        hasKey ? ColorUtil.multiplyAlpha(ColorUtil.rgb(100, 220, 140), Math.max(0.1F, a)) : ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 140, 100), Math.max(0.1F, a)));
            }

            y += 30;
        }

        y += 6;
        gfx.text(this.font, "МОДЕЛЬ", x + 2, y, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));
        y += 12;

        String currentModel = this.module.getActiveModel();
        boolean modelHover = this.isInside(mx, my, x, y, itemW, 28);

        gfx.fill(x, y, x + itemW, y + 28, modelHover ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
        this.border(gfx, x, y, itemW, 28, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));

        String displayModel = currentModel.length() > 22 ? currentModel.substring(0, 22) + "..." : currentModel;
        gfx.text(this.font, displayModel, x + 10, y + 6, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        gfx.text(this.font, "\u25bc", x + itemW - 13, y + 6, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));

        gfx.text(this.font, this.loadingModels ? "загрузка..." : "нажми для выбора", x + 10, y + 17, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));
    }

    private void renderSidebarButtons(GuiGraphicsExtractor gfx, Layout L, int accent, float a, int mx, int my) {
        int x = L.px + 12;
        int itemW = L.sidebarW - 24;
        int y = L.py + L.ph - 140;

        int btnH = 30;
        int gap = 5;

        boolean keyHover = this.isInside(mx, my, x, y, itemW, btnH);
        gfx.fill(x, y, x + itemW, y + btnH, keyHover ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(6.0F * a)));
        this.border(gfx, x, y, itemW, btnH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));
        gfx.text(this.font, "API Key", x + 12, y + (btnH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        y += btnH + gap;
        boolean traceHover = this.isInside(mx, my, x, y, itemW, btnH) && !this.waiting;
        gfx.fill(x, y, x + itemW, y + btnH, traceHover ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(6.0F * a)));
        this.border(gfx, x, y, itemW, btnH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));
        gfx.text(this.font, "Stacktrace", x + 12, y + (btnH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, Math.max(0.05F, this.waiting ? 80.0F / 255.0F * a : a)));

        y += btnH + gap;
        boolean clearHover = this.isInside(mx, my, x, y, itemW, btnH);
        gfx.fill(x, y, x + itemW, y + btnH, clearHover ? ColorUtil.rgba(255, 80, 80, Math.round(25.0F * a)) : ColorUtil.rgba(255, 255, 255, Math.round(6.0F * a)));
        this.border(gfx, x, y, itemW, btnH, 1, ColorUtil.rgba(255, 100, 100, Math.round(80.0F * a)));
        gfx.text(this.font, "Очистить", x + 12, y + (btnH - 9) / 2,
                clearHover ? ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 140, 140), a) : ColorUtil.multiplyAlpha(ColorUtil.rgb(255, 130, 130), a));

        y += btnH + gap;
        boolean colorHover = this.isInside(mx, my, x, y, itemW, btnH);
        gfx.fill(x, y, x + itemW, y + btnH, colorHover ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(6.0F * a)));
        this.border(gfx, x, y, itemW, btnH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));
        gfx.fill(x + 12, y + 8, x + 26, y + 22, ColorUtil.withAlpha(accent, Math.round(255.0F * a)));
        gfx.text(this.font, "Цвет темы", x + 32, y + (btnH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));
    }

    private void renderColorPicker(GuiGraphicsExtractor gfx, int accent, float a, int mx, int my) {
        int w = 260;
        int h = 220;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        gfx.fill(0, 0, this.width, this.height, ColorUtil.rgba(0, 0, 0, Math.round(150.0F * a)));
        gfx.fill(x, y, x + w, y + h, ColorUtil.multiplyAlpha(ColorUtil.rgb(22, 22, 32), a));
        this.border(gfx, x, y, w, h, 1, ColorUtil.withAlpha(accent, Math.round(180.0F * a)));

        gfx.text(this.font, "Выбери цвет темы", x + 16, y + 14, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        int svX = x + 16;
        int svY = y + 36;
        int svW = 180;
        int svH = 140;

        for (int py = 0; py < svH; py++) {
            for (int px = 0; px < svW; px++) {
                float s = px / (float) svW;
                float v = 1.0F - py / (float) svH;
                gfx.fill(svX + px, svY + py, svX + px + 1, svY + py + 1,
                        ColorUtil.fromHsv(this.pickerHue, s, v, Math.round(255.0F * a)));
            }
        }
        this.border(gfx, svX, svY, svW, svH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));

        int cursorSX = svX + Math.round(this.pickerSat * svW);
        int cursorSY = svY + Math.round((1.0F - this.pickerVal) * svH);
        gfx.fill(cursorSX - 4, cursorSY - 4, cursorSX + 4, cursorSY + 4, ColorUtil.WHITE);
        this.border(gfx, cursorSX - 5, cursorSY - 5, 10, 10, 1, ColorUtil.rgba(0, 0, 0, Math.round(180.0F * a)));

        int hueX = svX + svW + 10;
        int hueW = 16;

        for (int hy = 0; hy < svH; hy++) {
            float hue = hy / (float) svH;
            gfx.fill(hueX, svY + hy, hueX + hueW, svY + hy + 1,
                    ColorUtil.fromHsv(hue, 1.0F, 1.0F, Math.round(255.0F * a)));
        }
        this.border(gfx, hueX, svY, hueW, svH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));

        int cursorHY = svY + Math.round(this.pickerHue * svH);
        gfx.fill(hueX - 2, cursorHY - 1, hueX + hueW + 4, cursorHY + 2, ColorUtil.WHITE);

        gfx.fill(hueX + hueW + 12, svY, hueX + hueW + 42, svY + 28, ColorUtil.fromHsv(this.pickerHue, this.pickerSat, this.pickerVal, 255));
        this.border(gfx, hueX + hueW + 12, svY, 30, 28, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));

        int applyY = svY + svH + 12;
        int applyW = 100;
        int applyH = 28;
        int applyX = x + w - applyW - 16;
        boolean applyHover = this.isInside(mx, my, applyX, applyY, applyW, applyH);
        int applyColor = applyHover ? ColorUtil.withAlpha(ColorUtil.multiplyRgb(accent, 1.12F), Math.round(255.0F * a)) : ColorUtil.withAlpha(accent, Math.round(220.0F * a));
        gfx.fill(applyX, applyY, applyX + applyW, applyY + applyH, applyColor);
        gfx.centeredText(this.font, "Применить", applyX + applyW / 2, applyY + (applyH - 9) / 2, ColorUtil.WHITE);

        int cancelW = 80;
        int cancelX = applyX - cancelW - 8;
        boolean cancelHover = this.isInside(mx, my, cancelX, applyY, cancelW, applyH);
        gfx.fill(cancelX, applyY, cancelX + cancelW, applyY + applyH,
                cancelHover ? ColorUtil.rgba(255, 80, 80, Math.round(30.0F * a)) : ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
        gfx.centeredText(this.font, "Отмена", cancelX + cancelW / 2, applyY + (applyH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));
    }

    private void renderApiKeyInput(GuiGraphicsExtractor gfx, int accent, float a, int mx, int my) {
        int w = 420;
        int h = 160;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        gfx.fill(0, 0, this.width, this.height, ColorUtil.rgba(0, 0, 0, Math.round(150.0F * a)));
        gfx.fill(x, y, x + w, y + h, ColorUtil.multiplyAlpha(ColorUtil.rgb(22, 22, 32), a));
        this.border(gfx, x, y, w, h, 1, ColorUtil.withAlpha(accent, Math.round(180.0F * a)));

        ZaiAi.Provider prov = this.module.getActiveProvider();
        gfx.text(this.font, "API Key — " + prov.getDisplayName(), x + 18, y + 16, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        int fieldX = x + 18;
        int fieldY = y + 40;
        int fieldW = w - 36;
        int fieldH = 32;
        gfx.fill(fieldX, fieldY, fieldX + fieldW, fieldY + fieldH, ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
        this.border(gfx, fieldX, fieldY, fieldW, fieldH, 1, ColorUtil.withAlpha(accent, Math.round(160.0F * a)));

        String display = this.apiKeyInput.isEmpty() ? "" : "*".repeat(Math.min(this.apiKeyInput.length(), 50));
        String shown = display;
        int start = 0;
        while (start < shown.length() && this.font.width(shown.substring(start)) > fieldW - 20) {
            start++;
        }
        if (display.isEmpty()) {
            gfx.text(this.font, "Вставь API-ключ...", fieldX + 10, fieldY + (fieldH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT_DARK, a));
        } else {
            gfx.text(this.font, shown.substring(start), fieldX + 10, fieldY + (fieldH - 9) / 2, ColorUtil.multiplyAlpha(ColorUtil.WHITE, a));
        }
        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            int cx = fieldX + 10 + this.font.width(shown.substring(start, Math.min(start + this.apiKeyCursor, shown.length())));
            gfx.fill(cx, fieldY + 8, cx + 1, fieldY + fieldH - 8, ColorUtil.multiplyAlpha(ColorUtil.WHITE, a));
        }

        int btnY = fieldY + fieldH + 12;
        int btnH = 28;
        int saveW = 100;
        int pasteW = 90;
        int cancelW = 80;
        int totalBtns = saveW + pasteW + cancelW + 16;
        int btnStartX = x + w - totalBtns - 18;

        int saveX = btnStartX;
        boolean saveHover = this.isInside(mx, my, saveX, btnY, saveW, btnH);
        int saveColor = saveHover ? ColorUtil.withAlpha(ColorUtil.multiplyRgb(accent, 1.12F), Math.round(255.0F * a)) : ColorUtil.withAlpha(accent, Math.round(220.0F * a));
        gfx.fill(saveX, btnY, saveX + saveW, btnY + btnH, saveColor);
        gfx.centeredText(this.font, "Сохранить", saveX + saveW / 2, btnY + (btnH - 9) / 2, ColorUtil.WHITE);

        int pasteX = saveX + saveW + 8;
        boolean pasteHover = this.isInside(mx, my, pasteX, btnY, pasteW, btnH);
        gfx.fill(pasteX, btnY, pasteX + pasteW, btnY + btnH, pasteHover ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
        this.border(gfx, pasteX, btnY, pasteW, btnH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));
        gfx.centeredText(this.font, "Вставить", pasteX + pasteW / 2, btnY + (btnH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        int cancelX = pasteX + pasteW + 8;
        boolean cancelHover = this.isInside(mx, my, cancelX, btnY, cancelW, btnH);
        gfx.fill(cancelX, btnY, cancelX + cancelW, btnY + btnH,
                cancelHover ? ColorUtil.rgba(255, 80, 80, Math.round(30.0F * a)) : ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
        gfx.centeredText(this.font, "Отмена", cancelX + cancelW / 2, btnY + (btnH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));
    }

    private void renderModelSelector(GuiGraphicsExtractor gfx, int accent, float a, int mx, int my) {
        int w = 420;
        int h = 380;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;

        gfx.fill(0, 0, this.width, this.height, ColorUtil.rgba(0, 0, 0, Math.round(150.0F * a)));
        gfx.fill(x, y, x + w, y + h, ColorUtil.multiplyAlpha(ColorUtil.rgb(22, 22, 32), a));
        this.border(gfx, x, y, w, h, 1, ColorUtil.withAlpha(accent, Math.round(180.0F * a)));

        ZaiAi.Provider prov = this.module.getActiveProvider();
        gfx.text(this.font, "Модели — " + prov.getDisplayName(), x + 18, y + 16, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        int listX = x + 18;
        int listY = y + 40;
        int listW = w - 36;
        int listH = h - 100;

        gfx.fill(listX, listY, listX + listW, listY + listH, ColorUtil.rgba(255, 255, 255, Math.round(6.0F * a)));
        this.border(gfx, listX, listY, listW, listH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));

        if (this.loadingModels) {
            gfx.centeredText(this.font, "Загрузка моделей...", listX + listW / 2, listY + listH / 2, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));
        } else if (this.availableModels.isEmpty()) {
            gfx.centeredText(this.font, "Нет моделей. Введи API-ключ.", listX + listW / 2, listY + listH / 2, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));
        } else {
            int itemH = 32;
            float totalH = this.availableModels.size() * itemH;
            float maxScroll = Math.max(0, totalH - listH);
            this.modelScroll = Math.max(0, Math.min(this.modelScroll, maxScroll));

            gfx.enableScissor(listX, listY, listX + listW, listY + listH);

            String currentModel = this.module.getActiveModel();
            float iy = listY - this.modelScroll;
            for (ZaiAi.ModelInfo model : this.availableModels) {
                if (iy + itemH >= listY && iy <= listY + listH) {
                    int rowY = Math.round(iy);
                    boolean isCurrent = model.id().equals(currentModel);
                    boolean hover = this.isInside(mx, my, listX, rowY, listW, itemH);

                    if (isCurrent) {
                        gfx.fill(listX + 2, rowY, listX + listW - 2, rowY + itemH, ColorUtil.withAlpha(accent, Math.round(25.0F * a)));
                    } else if (hover) {
                        gfx.fill(listX + 2, rowY, listX + listW - 2, rowY + itemH, ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
                    }

                    String displayName = model.name().length() > 40 ? model.name().substring(0, 40) + "..." : model.name();
                    gfx.text(this.font, displayName, listX + 10, rowY + 6,
                            isCurrent ? ColorUtil.withAlpha(accent, Math.round(255.0F * a)) : ColorUtil.multiplyAlpha(this.COL_TEXT, a));
                    gfx.text(this.font, model.id(), listX + 10, rowY + 16, ColorUtil.multiplyAlpha(this.COL_TEXT_DIM, a));
                }
                iy += itemH;
            }

            gfx.disableScissor();
        }

        int btnY = y + h - 44;
        int btnH = 28;
        int refreshW = 110;
        int closeW = 80;
        int refreshX = x + 18;
        boolean refreshHover = this.isInside(mx, my, refreshX, btnY, refreshW, btnH);
        gfx.fill(refreshX, btnY, refreshX + refreshW, btnY + btnH, refreshHover ? ColorUtil.multiplyAlpha(this.COL_SURFACE, a) : ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
        this.border(gfx, refreshX, btnY, refreshW, btnH, 1, ColorUtil.multiplyAlpha(this.COL_BORDER, a));
        gfx.centeredText(this.font, "Обновить", refreshX + refreshW / 2, btnY + (btnH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));

        int closeX = x + w - closeW - 18;
        boolean closeHover = this.isInside(mx, my, closeX, btnY, closeW, btnH);
        gfx.fill(closeX, btnY, closeX + closeW, btnY + btnH,
                closeHover ? ColorUtil.rgba(255, 80, 80, Math.round(30.0F * a)) : ColorUtil.rgba(255, 255, 255, Math.round(10.0F * a)));
        gfx.centeredText(this.font, "Закрыть", closeX + closeW / 2, btnY + (btnH - 9) / 2, ColorUtil.multiplyAlpha(this.COL_TEXT, a));
    }

    private void renderMessages(GuiGraphicsExtractor gfx, Layout L, int accent, float a) {
        VibeSession session = this.activeSession();
        int lineH = 12;
        List<Bubble> bubbles = new ArrayList<>();
        for (VibeSession.Msg m : session.history) {
            List<String> lines = this.wrap(m.text(), L.maxTextW);
            int bh = lines.size() * lineH + 20;
            int maxW = 0;
            for (String line : lines) {
                maxW = Math.max(maxW, this.font.width(line));
            }
            int bw = Math.min(L.maxTextW + 30, Math.max(80, maxW + 28));
            bubbles.add(new Bubble(m.user(), bw, bh, lines));
        }

        int avail = L.msgBottom - L.msgTop;
        int start = 0;
        float used = 0.0F;
        for (int i = bubbles.size() - 1; i >= 0; i--) {
            used += bubbles.get(i).h + 10.0F;
            if (used > avail) {
                start = i + 1;
                break;
            }
        }

        float y = L.msgBottom;
        for (int i = bubbles.size() - 1; i >= start; i--) {
            Bubble b = bubbles.get(i);
            y -= b.h + 10.0F;
            int bx = (int) (b.user() ? (L.chatX + L.chatW - this.pad() - b.w) : (L.chatX + this.pad()));

            int fill = b.user() ? ColorUtil.withAlpha(accent, Math.round(25.0F * a)) : ColorUtil.withAlpha(this.COL_SURFACE, Math.round(200.0F * a));
            int borderC = b.user() ? ColorUtil.withAlpha(accent, Math.round(80.0F * a)) : ColorUtil.withAlpha(this.COL_BORDER, Math.round(100.0F * a));

            gfx.fill(bx, (int) y, bx + b.w, (int) (y + b.h), fill);
            this.border(gfx, bx, (int) y, b.w, b.h, 1, borderC);

            String label = b.user ? "you" : this.module.getActiveProvider().getDisplayName();
            int labelColor = b.user ? ColorUtil.withAlpha(accent, Math.round(200.0F * a)) : ColorUtil.multiplyAlpha(ColorUtil.rgb(140, 180, 255), a);
            gfx.text(this.font, label, bx + 10, (int) y + 6, labelColor);

            float ty = y + 18;
            for (String line : b.lines) {
                gfx.text(this.font, line, bx + 10, (int) ty, ColorUtil.multiplyAlpha(this.COL_TEXT, a));
                ty += lineH;
            }
        }

        if (this.waiting) {
            int bx = L.chatX + this.pad();
            int by = L.msgBottom - 26;
            gfx.fill(bx, by, bx + 120, by + 22, ColorUtil.withAlpha(this.COL_SURFACE, Math.round(200.0F * a)));
            gfx.text(this.font, "нейросеть печатает...", bx + 12, by + 6, ColorUtil.multiplyAlpha(ColorUtil.rgb(140, 180, 255), a));
        }
    }

    private List<String> wrap(String text, int maxW) {
        List<String> out = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            if (paragraph.isEmpty()) {
                out.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String test = line.length() == 0 ? word : line + " " + word;
                if (this.font.width(test) > maxW && line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(test);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean flag) {
        if (event.button() != 0) {
            return super.mouseClicked(event, flag);
        }
        int mx = (int) event.x();
        int my = (int) event.y();
        Layout L = this.layout();

        if (this.showingColorPicker) {
            int w = 260;
            int h = 220;
            int x = (this.width - w) / 2;
            int y = (this.height - h) / 2;
            int svX = x + 16;
            int svY = y + 36;
            int svW = 180;
            int svH = 140;
            int hueX = svX + svW + 10;
            int applyY = svY + svH + 12;
            int applyW = 100;
            int applyH = 28;
            int applyX = x + w - applyW - 16;
            int cancelW = 80;
            int cancelX = applyX - cancelW - 8;

            if (this.isInside(mx, my, svX, svY, svW, svH)) {
                this.pickerSat = (float) Math.max(0, Math.min(1, ((float) mx - svX) / svW));
                this.pickerVal = (float) Math.max(0, Math.min(1, 1.0F - ((float) my - svY) / svH));
                this.draggingSV = true;
                return true;
            }
            if (this.isInside(mx, my, hueX, svY, 16, svH)) {
                this.pickerHue = (float) Math.max(0, Math.min(1, ((float) my - svY) / svH));
                this.draggingHue = true;
                return true;
            }
            if (this.isInside(mx, my, applyX, applyY, applyW, applyH)) {
                this.module.accent.setValue(ColorUtil.fromHsv(this.pickerHue, this.pickerSat, this.pickerVal, 255));
                this.showingColorPicker = false;
                return true;
            }
            if (this.isInside(mx, my, cancelX, applyY, cancelW, applyH)) {
                this.showingColorPicker = false;
                return true;
            }
            return true;
        }

        if (this.showingApiKeyInput) {
            int w = 420;
            int h = 160;
            int x = (this.width - w) / 2;
            int y = (this.height - h) / 2;
            int fieldX = x + 18;
            int fieldY = y + 40;
            int fieldW = w - 36;
            int fieldH = 32;
            int btnY = fieldY + fieldH + 12;
            int btnH = 28;
            int saveW = 100;
            int pasteW = 90;
            int cancelW = 80;
            int btnStartX = x + w - (saveW + pasteW + cancelW + 16) - 18;
            int saveX = btnStartX;
            int pasteX = saveX + saveW + 8;
            int cancelX = pasteX + pasteW + 8;

            if (this.isInside(mx, my, saveX, btnY, saveW, btnH)) {
                this.saveApiKey();
                return true;
            }
            if (this.isInside(mx, my, pasteX, btnY, pasteW, btnH)) {
                String clip = MenuClipboard.get();
                if (clip != null && !clip.isEmpty()) {
                    this.apiKeyInput = clip.trim();
                    this.apiKeyCursor = this.apiKeyInput.length();
                }
                return true;
            }
            if (this.isInside(mx, my, cancelX, btnY, cancelW, btnH)) {
                this.showingApiKeyInput = false;
                return true;
            }
            if (this.isInside(mx, my, fieldX, fieldY, fieldW, fieldH)) {
                return true;
            }
            this.showingApiKeyInput = false;
            return true;
        }

        if (this.showingModelSelector) {
            int w = 420;
            int h = 380;
            int x = (this.width - w) / 2;
            int y = (this.height - h) / 2;
            int listX = x + 18;
            int listY = y + 40;
            int listW = w - 36;
            int listH = h - 100;
            int btnY = y + h - 44;
            int btnH = 28;
            int refreshX = x + 18;
            int closeX = x + w - 80 - 18;

            if (this.isInside(mx, my, refreshX, btnY, 110, btnH)) {
                this.loadModels();
                return true;
            }
            if (this.isInside(mx, my, closeX, btnY, 80, btnH)) {
                this.showingModelSelector = false;
                return true;
            }
            if (this.isInside(mx, my, listX, listY, listW, listH) && !this.loadingModels) {
                int itemH = 32;
                float iy = listY - this.modelScroll;
                for (ZaiAi.ModelInfo model : this.availableModels) {
                    if (my >= iy && my <= iy + itemH) {
                        this.module.setActiveModel(model.id());
                        this.showingModelSelector = false;
                        return true;
                    }
                    iy += itemH;
                }
            }
            this.showingModelSelector = false;
            return true;
        }

        if (L.hitClose(mx, my)) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        if (L.hitSend(mx, my) && !this.waiting) {
            this.send();
            return true;
        }
        if (L.hitNewSession(mx, my)) {
            VibeSession s = new VibeSession("Сессия " + (VibeCodingScreen.SESSIONS.size() + 1));
            s.addMessage(false, "Новая сессия начата. Напиши запрос.");
            VibeCodingScreen.SESSIONS.add(s);
            VibeCodingScreen.activeSessionIdx = VibeCodingScreen.SESSIONS.size() - 1;
            return true;
        }

        if (L.hitSidebar(mx, my)) {
            int itemH = 34;
            int itemW = L.sidebarW - 24;
            int x = L.px + 12;
            int y = L.newSessionY + L.newSessionH + 8;

            for (int i = 0; i < VibeCodingScreen.SESSIONS.size(); i++) {
                if (this.isInside(mx, my, x, y, itemW, itemH)) {
                    VibeCodingScreen.activeSessionIdx = i;
                    return true;
                }
                y += itemH + 3;
            }

            int provY = L.newSessionY + L.newSessionH + 8 + (VibeCodingScreen.SESSIONS.size() * 37) + 14 + 12;
            for (ZaiAi.Provider prov : ZaiAi.Provider.values()) {
                if (this.isInside(mx, my, x, provY, itemW, 28)) {
                    if (!this.module.provider.getValue().equals(prov.getDisplayName())) {
                        this.module.provider.setValue(prov.getDisplayName());
                        this.module.autoDetectModels();
                    }
                    return true;
                }
                provY += 30;
            }

            int modelBtnY = provY + 6 + 12 + 28;
            if (this.isInside(mx, my, x, modelBtnY, itemW, 28)) {
                this.showingModelSelector = true;
                this.loadModels();
                return true;
            }

            int btnY2 = L.py + L.ph - 140;
            int btnH2 = 30;
            int gap = 5;
            if (this.isInside(mx, my, x, btnY2, itemW, btnH2)) {
                this.showingApiKeyInput = true;
                this.apiKeyInput = this.module.getActiveApiKey() != null ? this.module.getActiveApiKey() : "";
                this.apiKeyCursor = this.apiKeyInput.length();
                return true;
            }
            btnY2 += btnH2 + gap;
            if (this.isInside(mx, my, x, btnY2, itemW, btnH2) && !this.waiting) {
                this.sendTrace();
                return true;
            }
            btnY2 += btnH2 + gap;
            if (this.isInside(mx, my, x, btnY2, itemW, btnH2)) {
                this.activeSession().history.clear();
                return true;
            }
            btnY2 += btnH2 + gap;
            if (this.isInside(mx, my, x, btnY2, itemW, btnH2)) {
                int c = this.module.accent.getValue();
                float[] hsv = ColorUtil.hsv(c);
                this.pickerHue = hsv[0];
                this.pickerSat = hsv[1];
                this.pickerVal = hsv[2];
                this.showingColorPicker = true;
                return true;
            }
            return true;
        }

        if (L.inChat(mx, my)) {
            this.focused = L.hitInput(mx, my);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.draggingSV = false;
            this.draggingHue = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.showingModelSelector) {
            this.modelScroll -= (float) (verticalAmount * 20.0F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void dragColorPicker(int mouseX, int mouseY) {
        if (!this.showingColorPicker) {
            return;
        }
        int x = (this.width - 260) / 2;
        int y = (this.height - 220) / 2;
        int svX = x + 16;
        int svY = y + 36;
        int svW = 180;
        int svH = 140;
        int hueX = svX + svW + 10;
        if (this.draggingSV) {
            this.pickerSat = (float) Math.max(0, Math.min(1, ((float) mouseX - svX) / svW));
            this.pickerVal = (float) Math.max(0, Math.min(1, 1.0F - ((float) mouseY - svY) / svH));
        }
        if (this.draggingHue) {
            this.pickerHue = (float) Math.max(0, Math.min(1, ((float) mouseY - svY) / svH));
        }
    }

    private void saveApiKey() {
        ZaiAi.Provider prov = this.module.getActiveProvider();
        switch (prov) {
            case ZAI -> this.module.apiKeyZai.setValue(this.apiKeyInput);
            case GOOGLE -> this.module.apiKeyGoogle.setValue(this.apiKeyInput);
            case OPENROUTER -> this.module.apiKeyOpenRouter.setValue(this.apiKeyInput);
            case XAI -> this.module.apiKeyXai.setValue(this.apiKeyInput);
            case KIMI -> this.module.apiKeyKimi.setValue(this.apiKeyInput);
        }
        this.showingApiKeyInput = false;
        VibeSession session = this.activeSession();
        session.addMessage(false, "API-ключ " + prov.getDisplayName() + " сохранён.");
        this.module.autoDetectModels();
    }

    private void loadModels() {
        ZaiAi.Provider prov = this.module.getActiveProvider();
        String key = this.module.getActiveApiKey();
        if (key == null || key.isBlank()) {
            this.activeSession().addMessage(false, "Сначала введи API-ключ для " + prov.getDisplayName() + ".");
            return;
        }
        this.loadingModels = true;
        this.availableModels.clear();
        this.availableModels.addAll(this.module.getCachedModels());
        this.module.autoDetectModels();
        ZaiAi.fetchModels(prov, key,
                (models) -> Minecraft.getInstance().execute(() -> {
                    this.availableModels = models;
                    this.loadingModels = false;
                }),
                (err) -> Minecraft.getInstance().execute(() -> {
                    if (this.availableModels.isEmpty()) {
                        this.activeSession().addMessage(false, "Ошибка загрузки моделей: " + err);
                    }
                    this.loadingModels = false;
                }));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int chr = event.codepoint();
        if (this.showingApiKeyInput) {
            if (chr < 32 || this.apiKeyInput.length() >= 200) {
                return false;
            }
            this.apiKeyInput = this.apiKeyInput.substring(0, this.apiKeyCursor) + event.codepointAsString() + this.apiKeyInput.substring(this.apiKeyCursor);
            this.apiKeyCursor++;
            return true;
        }
        if (!this.focused || this.waiting) {
            return false;
        }
        if (chr < 32 || this.input.length() >= MAX_INPUT) {
            return false;
        }
        this.input = this.input.substring(0, this.cursor) + event.codepointAsString() + this.input.substring(this.cursor);
        this.cursor++;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.showingApiKeyInput) {
            if (event.key() == 256) {
                this.showingApiKeyInput = false;
                return true;
            }
            if (event.key() == 257 || event.key() == 335) {
                this.saveApiKey();
                return true;
            }
            if (event.key() == 259) {
                if (this.apiKeyCursor > 0) {
                    this.apiKeyInput = this.apiKeyInput.substring(0, this.apiKeyCursor - 1) + this.apiKeyInput.substring(this.apiKeyCursor);
                    this.apiKeyCursor--;
                }
                return true;
            }
            if (event.hasControlDown() && event.key() == 86) {
                String clip = MenuClipboard.get();
                if (clip != null && !clip.isEmpty()) {
                    this.apiKeyInput = this.apiKeyInput.substring(0, this.apiKeyCursor) + clip + this.apiKeyInput.substring(this.apiKeyCursor);
                    this.apiKeyCursor += clip.length();
                }
                return true;
            }
            return false;
        }

        if (this.focused && (event.key() == 257 || event.key() == 335)) {
            this.send();
            return true;
        }
        if (event.key() == 256) {
            if (this.focused) {
                this.focused = false;
            } else {
                Minecraft.getInstance().gui.setScreen(null);
            }
            return true;
        }
        if (!this.focused) {
            return false;
        }
        switch (event.key()) {
            case 259:
                if (this.cursor > 0) {
                    this.input = this.input.substring(0, this.cursor - 1) + this.input.substring(this.cursor);
                    this.cursor--;
                }
                return true;
            case 261:
                if (this.cursor < this.input.length()) {
                    this.input = this.input.substring(0, this.cursor) + this.input.substring(this.cursor + 1);
                }
                return true;
            case 263:
                if (this.cursor > 0) {
                    this.cursor--;
                }
                return true;
            case 262:
                if (this.cursor < this.input.length()) {
                    this.cursor++;
                }
                return true;
            case 268:
                this.cursor = 0;
                return true;
            case 269:
                this.cursor = this.input.length();
                return true;
            default:
                return false;
        }
    }

    private void send() {
        String prompt = this.input.trim();
        if (prompt.isEmpty() || this.waiting) {
            return;
        }
        ZaiAi.Provider prov = this.module.getActiveProvider();
        String key = this.module.getActiveApiKey();
        if (key == null || key.isBlank()) {
            this.activeSession().addMessage(false, "Сначала введи API-ключ для " + prov.getDisplayName() + ".");
            return;
        }
        VibeSession session = this.activeSession();
        session.addMessage(true, prompt);
        session.lastPrompt = prompt;
        session.retries = 0;
        this.input = "";
        this.cursor = 0;
        this.doRequest(session);
    }

    private void doRequest(VibeSession session) {
        this.waiting = true;
        List<VibeSession.Msg> historyForContext = new ArrayList<>();
        for (VibeSession.Msg msg : session.history) {
            if (!session.history.isEmpty() && msg != session.history.get(session.history.size() - 1)) {
                historyForContext.add(msg);
            }
        }
        this.module.requestScript(session.lastPrompt, historyForContext,
                (text, saved) -> {
                    this.waiting = false;
                    session.addMessage(false, text);
                },
                (err) -> this.onError(session, err));
    }

    private void onError(VibeSession session, String err) {
        session.retries++;
        if (session.retries <= MAX_RETRIES) {
            session.addMessage(false, "запрос не прошёл — повтор через 3 сек...");
            new Thread(() -> {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException ignored) {
                }
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().gui.screen() == VibeCodingScreen.this && session.retries <= MAX_RETRIES) {
                        this.doRequest(session);
                    }
                });
            }, "Blade-VibeCoding-Retry").start();
        } else {
            this.waiting = false;
            session.addMessage(false, "Ошибка: " + err);
        }
    }

    private static final String[] ERR_MARKERS = {
            "ошибка загрузки", "ошибка компиляции", "annotation processing", "note:",
            "luaerror", "stack traceback", "jsonsyntaxexception", "jsonparseexception",
            "expected", "exception", "caused by", "error:"
    };

    private String captureLastError() {
        try {
            ArrayListDeque<String> hist = Minecraft.getInstance().gui.hud.getChat().getRecentChat();
            if (hist == null || hist.isEmpty()) {
                return null;
            }
            List<String> all = new ArrayList<>(hist);
            int n = all.size();
            int from = Math.max(0, n - 400);
            List<String> strs = all.subList(from, n);
            int idx = -1;
            for (int i = strs.size() - 1; i >= 0; i--) {
                String low = strs.get(i).toLowerCase(Locale.ROOT);
                for (String m : ERR_MARKERS) {
                    if (low.contains(m)) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0) {
                    break;
                }
            }
            if (idx < 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(strs.get(idx));
            int taken = 1;
            for (int i = idx + 1; i < strs.size() && taken < 40; i++) {
                String s = strs.get(i);
                if (this.isContinuation(s)) {
                    sb.append('\n').append(s);
                    taken++;
                } else {
                    break;
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isContinuation(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return false;
        }
        String low = t.toLowerCase(Locale.ROOT);
        if (s.startsWith(" ") || s.startsWith("\t")) {
            return true;
        }
        return (low.contains("at ") && (low.contains(".java:") || low.contains(".lua:")))
                || low.contains("caused by") || low.contains("luaerror")
                || low.contains("stack traceback") || low.contains("attempt to")
                || low.contains(".java:") || low.contains(".lua:");
    }

    private void sendTrace() {
        if (this.waiting) {
            return;
        }
        String err = this.captureLastError();
        String lang = "?";
        VibeSession session = this.activeSession();
        if (err == null || err.isBlank()) {
            session.addMessage(false, "Ошибок не найдено. Загрузи проблемный скрипт, затем нажми «Stacktrace».");
            return;
        }
        session.addMessage(true, "стактрейс -> ИИ");
        session.lastPrompt = "Исправь ошибку в моём скрипте (" + lang + "). Вот сообщение об ошибке:\n\n"
                + err + "\n\nВерни ТОЛЬКО исправленный код в блоке ``` с указанием языка и имени файла "
                + "(комментарий '-- name:' для lua/json или '// name:' для java) в первой строке, как в документации. "
                + "Не добавляй пояснений вне блока кода.";
        session.retries = 0;
        this.doRequest(session);
    }

    private void border(GuiGraphicsExtractor gfx, int x, int y, int w, int h, int thickness, int color) {
        gfx.fill(x, y, x + w, y + thickness, color);
        gfx.fill(x, y, x + thickness, y + h, color);
        gfx.fill(x + w - thickness, y, x + w, y + h, color);
        gfx.fill(x, y + h - thickness, x + w, y + h, color);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Bubble(boolean user, int w, int h, List<String> lines) {
    }

    protected static final class Layout {
        final int px, py, pw, ph, sidebarW, headerH, chatX, chatW, chatY, chatH;
        final int msgTop, msgBottom, maxTextW;
        final int inputX, inputY, inputW, inputH;
        final int sendX, sendY, sendW, sendH;
        final int closeX, closeY, closeW, closeH;
        final int sidebarHeaderH, newSessionX, newSessionY, newSessionW, newSessionH;

        Layout(int px, int py, int pw, int ph, int sidebarW, int headerH, int chatX, int chatW, int chatY, int chatH,
               int msgTop, int msgBottom, int maxTextW, int inputX, int inputY, int inputW, int inputH,
               int sendX, int sendY, int sendW, int sendH, int closeX, int closeY, int closeW, int closeH,
               int sidebarHeaderH, int newSessionX, int newSessionY, int newSessionW, int newSessionH) {
            this.px = px;
            this.py = py;
            this.pw = pw;
            this.ph = ph;
            this.sidebarW = sidebarW;
            this.headerH = headerH;
            this.chatX = chatX;
            this.chatW = chatW;
            this.chatY = chatY;
            this.chatH = chatH;
            this.msgTop = msgTop;
            this.msgBottom = msgBottom;
            this.maxTextW = maxTextW;
            this.inputX = inputX;
            this.inputY = inputY;
            this.inputW = inputW;
            this.inputH = inputH;
            this.sendX = sendX;
            this.sendY = sendY;
            this.sendW = sendW;
            this.sendH = sendH;
            this.closeX = closeX;
            this.closeY = closeY;
            this.closeW = closeW;
            this.closeH = closeH;
            this.sidebarHeaderH = sidebarHeaderH;
            this.newSessionX = newSessionX;
            this.newSessionY = newSessionY;
            this.newSessionW = newSessionW;
            this.newSessionH = newSessionH;
        }

        boolean hitClose(int mx, int my) {
            return mx >= this.closeX && mx <= this.closeX + this.closeW && my >= this.closeY && my <= this.closeY + this.closeH;
        }

        boolean hitSend(int mx, int my) {
            return mx >= this.sendX && mx <= this.sendX + this.sendW && my >= this.sendY && my <= this.sendY + this.sendH;
        }

        boolean hitInput(int mx, int my) {
            return mx >= this.inputX && mx <= this.inputX + this.inputW && my >= this.inputY && my <= this.inputY + this.inputH;
        }

        boolean hitNewSession(int mx, int my) {
            return mx >= this.newSessionX && mx <= this.newSessionX + this.newSessionW
                    && my >= this.newSessionY && my <= this.newSessionY + this.newSessionH;
        }

        boolean hitSidebar(int mx, int my) {
            return mx >= this.px && mx <= this.px + this.sidebarW && my >= this.py && my <= this.py + this.ph;
        }

        boolean inChat(int mx, int my) {
            return mx >= this.chatX && mx <= this.chatX + this.chatW && my >= this.chatY && my <= this.chatY + this.chatH;
        }
    }
}