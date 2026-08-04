package tech.huihui.client.screens.dropdowngui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.setting.Setting;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.modules.impl.render.ClickGUI;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public final class Dropdown2Screen extends Screen {
    private static final int COL_W = 200;
    private static final int COL_GAP = 12;
    private static final int HDR_H = 26;
    private static final int MOD_H = 28;
    private static final int PAD = 10;
    private static final int ROW_H = 18;

    private final Map<Category, List<String>> expanded = new HashMap<>();
    private final Map<Category, Float> scroll = new HashMap<>();
    private boolean closing;
    private float scale = 1.0F;
    private NumberSetting draggingSlider;
    private boolean bindCapturing;
    private KeySetting bindSetting;

    public Dropdown2Screen() {
        super(Text.literal("ClickGUI"));
    }

    public static Dropdown2Screen getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final Dropdown2Screen INSTANCE = new Dropdown2Screen();
    }

    @Override
    protected void init() {
        this.closing = false;
        this.expanded.clear();
        this.draggingSlider = null;
        this.bindCapturing = false;
        this.bindSetting = null;
        Category[] cats = Category.values();
        float totalWidth = (float) cats.length * (float) (COL_W + COL_GAP) - (float) COL_GAP;
        float sx = ((float) this.width - 30.0F) / totalWidth;
        this.scale = MathHelper.clamp(Math.min(1.0F, sx), 0.35F, 1.0F);
    }

    private ColorRGBA themeColor() {
        Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
        return theme != null ? theme.getColor() : new ColorRGBA(138, 110, 255);
    }

    private ColorRGBA themeArgb(int alpha) {
        ColorRGBA c = this.themeColor();
        return new ColorRGBA(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private float totalLayoutWidth() {
        return (float) Category.values().length * (float) (COL_W + COL_GAP) - (float) COL_GAP;
    }

    private float toLocalX(double mouseX) {
        return (float) (mouseX - (double) ((float) this.width / 2.0F)) / this.scale + this.totalLayoutWidth() / 2.0F;
    }

    private float toLocalY(double mouseY) {
        return (float) (mouseY - 22.0D) / this.scale;
    }

    private List<Module> modulesOf(Category cat) {
        List<Module> mods = new ArrayList<>();
        for (Module module : HuihuiClient.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == cat) {
                mods.add(module);
            }
        }
        return mods;
    }

    private boolean isOpen(Category cat, String name) {
        return this.expanded.containsKey(cat) && this.expanded.get(cat).contains(name);
    }

    private int settingsRows(Module mod) {
        int rows = 0;
        for (Setting s : mod.getSettings()) {
            if (s.isVisible()) {
                rows++;
            }
        }
        return rows;
    }

    private int moduleTotalH(Module mod) {
        int totalH = MOD_H;
        if (this.isOpen(mod.getCategory(), mod.getName())) {
            int rows = this.settingsRows(mod);
            totalH += rows > 0 ? 4 + rows * ROW_H : 0;
        }
        return totalH;
    }

    private float contentHeight(List<Module> mods) {
        float h = 0.0F;
        for (Module mod : mods) {
            h += (float) this.moduleTotalH(mod) + 4.0F;
        }
        return h;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.drawBackground(context);
        float totalLayoutWidth = this.totalLayoutWidth();

        context.getMatrices().push();
        context.getMatrices().translate((float) this.width / 2.0F, 22.0F, 0.0F);
        context.getMatrices().scale(this.scale, this.scale, 1.0F);
        context.getMatrices().translate(-totalLayoutWidth / 2.0F, 0.0F, 0.0F);

        CustomDrawContext draw = CustomDrawContext.of(context);
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            float x = (float) i * (float) (COL_W + COL_GAP);
            this.renderColumn(draw, cats[i], x, 0.0F);
        }
        context.getMatrices().pop();
    }

    private void drawBackground(DrawContext context) {
        CustomDrawContext draw = CustomDrawContext.of(context);
        draw.drawRect(0.0F, 0.0F, (float) this.width, (float) this.height, new ColorRGBA(7, 9, 13));
        for (int r = 0; r < 3; r++) {
            int a = (int) (18.0D * (1.0D - (double) r * 0.33D));
            if (a < 1) {
                continue;
            }
            draw.drawRect(0.0F, 0.0F, (float) this.width, (float) this.height, new ColorRGBA(56, 66, 98, a));
        }
    }

    private void renderColumn(CustomDrawContext draw, Category cat, float x, float y) {
        this.renderHeader(draw, cat, x, y);
        List<Module> mods = this.modulesOf(cat);
        if (mods.isEmpty()) {
            return;
        }
        float content = this.contentHeight(mods);
        float maxScroll = Math.max(0.0F, content - ((float) this.height / this.scale - (float) HDR_H - 14.0F));
        float currentScroll = this.scroll.getOrDefault(cat, 0.0F);
        if (currentScroll > maxScroll) {
            currentScroll = maxScroll;
            this.scroll.put(cat, currentScroll);
        }

        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y + (float) HDR_H, (float) COL_W, (float) this.height / this.scale - (float) HDR_H, BorderRadius.ZERO, new ColorRGBA(10, 12, 17));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y + (float) HDR_H, (float) COL_W, (float) this.height / this.scale - (float) HDR_H, 1.0F, BorderRadius.ZERO, new ColorRGBA(255, 255, 255, 10));

        float curY = y + (float) HDR_H - currentScroll;
        for (Module mod : mods) {
            int totalH = this.moduleTotalH(mod);
            if (curY + (float) totalH < y + (float) HDR_H - 20.0F) {
                curY += (float) totalH + 4.0F;
                continue;
            }
            if (curY > y + (float) this.height / this.scale) {
                break;
            }
            this.renderModule(draw, mod, x, curY, (float) COL_W);
            curY += (float) totalH + 4.0F;
        }
    }

    private void renderHeader(CustomDrawContext draw, Category cat, float x, float y) {
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y, (float) COL_W, (float) HDR_H, BorderRadius.all(4.0F), new ColorRGBA(13, 15, 20));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y, (float) COL_W, (float) HDR_H, 1.0F, BorderRadius.all(4.0F), new ColorRGBA(255, 255, 255, 14));
        draw.drawText(Fonts.SEMIBOLD.getFont(10.0F), cat.getName(), x + (float) PAD, y + 8.0F, new ColorRGBA(238, 241, 247));
    }

    private void renderModule(CustomDrawContext draw, Module mod, float x, float y, float w) {
        boolean open = this.isOpen(mod.getCategory(), mod.getName());
        DrawUtil.drawRoundedRect(draw.getMatrices(), x, y + 1.0F, w, 27.0F, BorderRadius.ZERO, new ColorRGBA(10, 12, 17));
        DrawUtil.drawRoundedBorder(draw.getMatrices(), x, y + 1.0F, w, 27.0F, 1.0F, BorderRadius.ZERO, new ColorRGBA(255, 255, 255, 14));
        draw.drawText(Fonts.REGULAR.getFont(9.0F), mod.getName(), x + (float) PAD, y + 9.0F, new ColorRGBA(239, 242, 248));

        float gx = x + w - (float) PAD - 13.0F;
        float gy = y + 8.0F;
        ColorRGBA gearColor = open ? this.themeArgb(255) : new ColorRGBA(207, 211, 220, 235);
        draw.drawText(Fonts.BOLD.getFont(9.0F), "\u2699", gx + 1.0F, gy + 1.0F, gearColor);

        if (mod.isEnabled()) {
            float ex = x + w - (float) PAD - 13.0F - 8.0F - 5.0F;
            float ey = y + 9.0F;
            DrawUtil.drawRoundedRect(draw.getMatrices(), ex, ey, 4.0F, 10.0F, BorderRadius.all(2.0F), this.themeArgb(255));
        }

        if (open) {
            float cy = y + (float) MOD_H + 1.0F;
            List<Setting> settings = mod.getSettings();
            int ry = 0;
            for (Setting s : settings) {
                if (!s.isVisible()) {
                    continue;
                }
                float sy = cy + 4.0F + (float) ry * (float) ROW_H;
                this.renderSetting(draw, s, x, sy, w);
                ry++;
            }
            DrawUtil.drawRoundedRect(draw.getMatrices(), x, cy, w, 4.0F + (float) ry * (float) ROW_H, BorderRadius.ZERO, new ColorRGBA(10, 12, 17));
        }
    }

    private void renderSetting(CustomDrawContext draw, Setting setting, float x, float y, float w) {
        float lx = x + (float) PAD;
        float rx = x + w - (float) PAD;
        String name = setting.getName();

        if (setting instanceof BooleanSetting bs) {
            draw.drawText(Fonts.REGULAR.getFont(8.0F), name, lx, y + 3.0F, new ColorRGBA(216, 220, 228));
            boolean val = bs.isEnabled();
            float tx = rx - 12.0F;
            float ty = y + 2.0F;
            ColorRGBA tColor = val ? this.themeArgb(255) : new ColorRGBA(255, 255, 255, 18);
            DrawUtil.drawRoundedRect(draw.getMatrices(), tx + 1.0F, ty + 1.0F, 11.0F, 11.0F, BorderRadius.all(3.0F), tColor);
            if (val) {
                draw.drawText(Fonts.BOLD.getFont(7.0F), "\u2713", tx + 2.0F, ty, new ColorRGBA(255, 255, 255));
            }
        } else if (setting instanceof NumberSetting ns) {
            float val = ns.getCurrent();
            float min = ns.getMin();
            float max = ns.getMax();
            draw.drawText(Fonts.REGULAR.getFont(8.0F), name, lx, y + 3.0F, new ColorRGBA(216, 220, 228));
            String valStr = String.format("%.1f", val);
            draw.drawText(Fonts.REGULAR.getFont(8.0F), valStr, rx - Fonts.REGULAR.getWidth(valStr, 8.0F), y + 3.0F, new ColorRGBA(241, 243, 248));
            float sx = lx;
            float sy = y + 14.0F;
            float sw = w - 24.0F;
            float pct = (max - min) <= 0.0F ? 0.0F : (val - min) / (max - min);
            DrawUtil.drawRoundedRect(draw.getMatrices(), sx, sy, sw, 2.0F, BorderRadius.all(999.0F), new ColorRGBA(36, 39, 49));
            if (pct > 0.01F) {
                DrawUtil.drawRoundedRect(draw.getMatrices(), sx, sy, sw * pct, 2.0F, BorderRadius.all(999.0F), this.themeArgb(255));
            }
            float kx = sx + sw * pct;
            float ky = sy + 1.0F;
            DrawUtil.drawRoundedRect(draw.getMatrices(), kx - 3.0F, ky - 3.0F, 6.0F, 6.0F, BorderRadius.all(999.0F), new ColorRGBA(246, 247, 255));
        } else if (setting instanceof ModeSetting ms) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), lx, y + 2.0F, w - 24.0F, 14.0F, BorderRadius.all(3.0F), new ColorRGBA(23, 25, 31));
            String val = ms.get();
            draw.drawText(Fonts.REGULAR.getFont(8.0F), val, lx + 6.0F, y + 5.0F, new ColorRGBA(243, 245, 250));
        } else if (setting instanceof KeySetting ks) {
            String keyName = ks.getKeyCode() > 0 ? ks.getNameKey() : "n/a";
            if (this.bindCapturing && this.bindSetting == ks) {
                keyName = "...";
            }
            draw.drawText(Fonts.REGULAR.getFont(8.0F), name, lx, y + 3.0F, new ColorRGBA(216, 220, 228));
            float kx = rx - Math.max(26.0F, Fonts.REGULAR.getWidth(keyName, 7.0F) + 10.0F);
            float ky = y + 1.0F;
            DrawUtil.drawRoundedRect(draw.getMatrices(), kx, ky, rx - kx, 15.0F, BorderRadius.all(3.0F), this.themeArgb(255));
            draw.drawText(Fonts.BOLD.getFont(7.0F), keyName, kx + 4.0F, ky + 4.0F, new ColorRGBA(255, 255, 255));
        } else if (setting instanceof MultiBooleanSetting mbs) {
            String display = String.join(", ", mbs.getSelectedNames());
            if (display.isEmpty()) {
                display = "None";
            }
            draw.drawText(Fonts.REGULAR.getFont(8.0F), name, lx, y + 3.0F, new ColorRGBA(216, 220, 228));
            draw.drawText(Fonts.REGULAR.getFont(7.0F), display, rx - Fonts.REGULAR.getWidth(display, 7.0F), y + 3.0F, new ColorRGBA(135, 139, 150));
        } else if (setting instanceof ButtonSetting) {
            DrawUtil.drawRoundedRect(draw.getMatrices(), lx, y + 2.0F, w - 24.0F, 14.0F, BorderRadius.all(3.0F), this.themeArgb(255));
            draw.drawText(Fonts.BOLD.getFont(8.0F), name, lx + (w - 24.0F) / 2.0F - Fonts.REGULAR.getWidth(name, 8.0F) / 2.0F, y + 4.0F, new ColorRGBA(255, 255, 255));
        } else if (setting instanceof ColorSetting cs) {
            draw.drawText(Fonts.REGULAR.getFont(8.0F), name, lx, y + 3.0F, new ColorRGBA(216, 220, 228));
            DrawUtil.drawRoundedRect(draw.getMatrices(), rx - 18.0F, y + 2.0F, 18.0F, 14.0F, BorderRadius.all(3.0F), cs.getColor());
        } else if (setting instanceof StringSetting ss) {
            draw.drawText(Fonts.REGULAR.getFont(8.0F), name, lx, y + 3.0F, new ColorRGBA(216, 220, 228));
            draw.drawText(Fonts.REGULAR.getFont(7.0F), ss.getValue(), rx - Fonts.REGULAR.getWidth(ss.getValue(), 7.0F), y + 3.0F, new ColorRGBA(135, 139, 150));
        } else {
            draw.drawText(Fonts.REGULAR.getFont(8.0F), name, lx, y + 3.0F, new ColorRGBA(216, 220, 228));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.closing) {
            return true;
        }
        if (this.bindCapturing && this.bindSetting != null) {
            if (keyCode == 256 || keyCode == 261) {
                this.bindSetting.setKeyCode(-1);
            } else {
                this.bindSetting.setKeyCode(keyCode);
            }
            this.bindCapturing = false;
            this.bindSetting = null;
            return true;
        }
        if (keyCode == 256) {
            this.closeScreen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void closeScreen() {
        this.closing = true;
        this.bindCapturing = false;
        this.bindSetting = null;
        ClickGUI.INSTANCE.setEnabled(false);
        this.client.setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.closing) {
            return true;
        }
        if (this.draggingSlider != null) {
            return true;
        }
        float mx = this.toLocalX(mouseX);
        float my = this.toLocalY(mouseY);

        Category[] cats = Category.values();
        for (int ci = 0; ci < cats.length; ci++) {
            float cx = (float) ci * (float) (COL_W + COL_GAP);
            if (mx < cx || mx > cx + (float) COL_W) {
                continue;
            }
            Category cat = cats[ci];
            float currentScroll = this.scroll.getOrDefault(cat, 0.0F);
            float my2 = my - (float) HDR_H + currentScroll;
            List<Module> mods = this.modulesOf(cat);
            for (int mi = 0; mi < mods.size(); mi++) {
                Module mod = mods.get(mi);
                int totalH = this.moduleTotalH(mod);
                if (my2 >= 0.0F && my2 < (float) totalH) {
                    this.handleClick(mod, mx - cx, my2, button);
                    return true;
                }
                my2 -= (float) totalH + 4.0F;
            }
        }
        return true;
    }

    private void handleClick(Module mod, float lx, float ly, int button) {
        boolean open = this.isOpen(mod.getCategory(), mod.getName());
        List<String> ex = this.expanded.computeIfAbsent(mod.getCategory(), (k) -> new ArrayList<>());
        float gx = (float) COL_W - (float) PAD - 13.0F;
        if (lx >= gx && lx <= gx + 13.0F && ly >= 6.0F && ly <= 21.0F) {
            String n = mod.getName();
            if (open) {
                ex.remove(n);
            } else {
                ex.add(n);
            }
            return;
        }
        if (ly < (float) MOD_H) {
            if (button == 0) {
                mod.toggle();
            }
            return;
        }
        float sy = ly - (float) MOD_H - 4.0F;
        int si = (int) (sy / (float) ROW_H);
        List<Setting> settings = mod.getSettings();
        int visibleIndex = 0;
        for (Setting s : settings) {
            if (!s.isVisible()) {
                continue;
            }
            if (visibleIndex == si) {
                this.handleSettingClick(s, lx, button);
                return;
            }
            visibleIndex++;
        }
    }

    private void handleSettingClick(Setting setting, float lx, int button) {
        if (setting instanceof BooleanSetting bs) {
            bs.setEnabled(!bs.isEnabled());
        } else if (setting instanceof ModeSetting ms) {
            List<ModeSetting.Value> modes = ms.getValues();
            if (!modes.isEmpty()) {
                int idx = -1;
                for (int i = 0; i < modes.size(); i++) {
                    if (modes.get(i).getName().equals(ms.get())) {
                        idx = i;
                        break;
                    }
                }
                ms.set(modes.get((idx + 1) % modes.size()).getName());
            }
        } else if (setting instanceof NumberSetting ns) {
            this.draggingSlider = ns;
            this.updateSlider(ns, lx);
        } else if (setting instanceof KeySetting ks) {
            this.bindCapturing = true;
            this.bindSetting = ks;
        } else if (setting instanceof ButtonSetting btn) {
            btn.toggle();
        } else if (setting instanceof MultiBooleanSetting mbs) {
            List<MultiBooleanSetting.Value> values = mbs.getBooleanSettings();
            if (!values.isEmpty()) {
                MultiBooleanSetting.Value target = null;
                for (MultiBooleanSetting.Value value : values) {
                    if (value.isEnabled()) {
                        target = value;
                        break;
                    }
                }
                if (target == null) {
                    values.get(0).setEnabled(true);
                } else {
                    target.setEnabled(false);
                    int next = (values.indexOf(target) + 1) % values.size();
                    values.get(next).setEnabled(true);
                }
            }
        }
    }

    private void updateSlider(NumberSetting ns, float lx) {
        float min = ns.getMin();
        float max = ns.getMax();
        float sw = (float) COL_W - 24.0F;
        float pct = MathHelper.clamp((lx - (float) PAD) / sw, 0.0F, 1.0F);
        float val = min + (max - min) * pct;
        ns.setCurrent(val);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingSlider != null) {
            float mx = this.toLocalX(mouseX);
            float cx = (float) Math.floor(mx / (float) (COL_W + COL_GAP)) * (float) (COL_W + COL_GAP);
            this.updateSlider(this.draggingSlider, mx - cx);
            return true;
        }
        return this.closing || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.draggingSlider = null;
        return !this.closing;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.closing) {
            return true;
        }
        float mx = this.toLocalX(mouseX);
        Category[] cats = Category.values();
        for (int ci = 0; ci < cats.length; ci++) {
            float cx = (float) ci * (float) (COL_W + COL_GAP);
            if (mx >= cx && mx <= cx + (float) COL_W) {
                Category cat = cats[ci];
                float currentScroll = this.scroll.getOrDefault(cat, 0.0F);
                float next = currentScroll - (float) verticalAmount * 20.0F;
                List<Module> mods = this.modulesOf(cat);
                float content = this.contentHeight(mods);
                float maxScroll = Math.max(0.0F, content - ((float) this.height / this.scale - (float) HDR_H - 14.0F));
                this.scroll.put(cat, MathHelper.clamp(next, 0.0F, maxScroll));
                return true;
            }
        }
        return true;
    }

    @Override
    public void removed() {
        this.closing = true;
        this.draggingSlider = null;
    }
}
