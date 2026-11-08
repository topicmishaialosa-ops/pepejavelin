package org.blade.feature.impl.visual.targethud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.blade.context.MinecraftContext;
import org.blade.context.RenderContext;
import org.blade.event.EventTarget;
import org.blade.event.events.input.MouseInputEvent;
import org.blade.event.events.render.FinalGuiRenderEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.combat.AuraFeature;
import org.blade.feature.impl.combat.TriggerBotFeature;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.setting.TextSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.math.Animation;
import org.blade.utils.math.MathUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.PlayerHead;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.TextAlign;
import org.blade.utils.render.gui.UiFonts;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class TargetHudFeature extends Feature {
    public static final String MODE_BLADE = "Blade";
    public static final String MODE_HUIHUI = "Huihui";

    public static final String TYPE_LARGE = "Крупный";
    public static final String TYPE_SMALL = "Маленький";
    public static final String TYPE_PERCENT = "Проценты";
    public static final String TYPE_BIG_BAR = "Большая полоса";
    public static final String TYPE_VERTICAL = "Вертикальный";
    public static final String TYPE_METANOISE = "Метал";
    public static final String TYPE_MINI = "Мини";
    public static final String TYPE_GRADIENT = "Градиент";
    public static final String TYPE_BOSS_BAR = "Боссбар";
    public static final String TYPE_MINIMAL = "Минимал";
    public static final String TYPE_CIRCLE = "Кружок";

    public static final String[] TYPES = {
            TYPE_LARGE, TYPE_SMALL, TYPE_PERCENT, TYPE_BIG_BAR, TYPE_VERTICAL,
            TYPE_METANOISE, TYPE_MINI, TYPE_GRADIENT, TYPE_BOSS_BAR, TYPE_MINIMAL, TYPE_CIRCLE
    };

    private static final EquipmentSlot[] ARMOR_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public final ModeSetting mode = register(new ModeSetting("Режим", MODE_HUIHUI, MODE_BLADE, MODE_HUIHUI));
    public final ModeSetting type = register(new ModeSetting("Тип", TYPE_LARGE, TYPES)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final NumberSetting x = register(new NumberSetting("X", 4.0, 0.0, 3840.0, 1.0, ""));
    public final NumberSetting y = register(new NumberSetting("Y", 4.0, 0.0, 2160.0, 1.0, ""));
    public final NumberSetting hudScale = register(new NumberSetting("Величина", 1.0, 0.5, 2.0, 0.05, "x"));
    public final BooleanSetting hover = register(new BooleanSetting("Наведение", true));
    public final NumberSetting barThickness = register(new NumberSetting("Толщина полоски", 1.0, 0.5, 3.0, 0.05, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final NumberSetting barRadius = register(new NumberSetting("Скругление полоски", 2.0, 0.0, 6.0, 0.1, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final BooleanSetting showSecondBar = register(new BooleanSetting("Золотая полоска", false)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final BooleanSetting matchBarThickness = register(new BooleanSetting("Полоски одного размера", true)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && showSecondBar.getValue()));
    public final NumberSetting secondBarThickness = register(new NumberSetting("Толщина золотой полоски", 1.0, 0.5, 3.0, 0.05, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && showSecondBar.getValue() && !matchBarThickness.getValue()));
    public final NumberSetting headSize = register(new NumberSetting("Размер иконки головы", 1.0, 0.5, 2.0, 0.05, ""));
    public final NumberSetting headYaw = register(new NumberSetting("Поворот головы", 0.0, -180.0, 180.0, 1.0, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final NumberSetting headPitch = register(new NumberSetting("Наклон головы", 0.0, -90.0, 90.0, 1.0, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final BooleanSetting headAutoRotate = register(new BooleanSetting("Автоповорот головы", false)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final ModeSetting displayMode = register(new ModeSetting("Отображение", "Проценты и HP", "Проценты и HP", "Проценты", "HP")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final BooleanSetting customColors = register(new BooleanSetting("Свои цвета", false)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final ColorSetting barColor = register(new ColorSetting("Цвет полоски", 0xFF3898FF)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && customColors.getValue()));
    public final ColorSetting barColorSecond = register(new ColorSetting("Второй цвет полоски", 0xFF646473)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && customColors.getValue()));
    public final ColorSetting bgColor = register(new ColorSetting("Цвет фона", 0xFF000000)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && customColors.getValue()));
    public final ColorSetting borderColor = register(new ColorSetting("Цвет рамки", 0xFF646473)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && customColors.getValue()));
    public final ColorSetting textColor = register(new ColorSetting("Цвет текста", 0xFFFFFFFF)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && customColors.getValue()));
    public final NumberSetting radius = register(new NumberSetting("Скругление", 5.0, 1.0, 10.0, 0.5, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final NumberSetting borderThickness = register(new NumberSetting("Толщина рамки", 1.0, 0.5, 3.0, 0.1, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final NumberSetting backgroundAlpha = register(new NumberSetting("Прозрачность фона", 120.0, 0.0, 255.0, 5.0, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final NumberSetting animationSpeed = register(new NumberSetting("Скорость анимации", 1.0, 0.1, 3.0, 0.1, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final BooleanSetting animatedHpToggle = register(new BooleanSetting("Анимация полоски", true));
    public final BooleanSetting showArmor = register(new BooleanSetting("Показывать броню", true));
    public final BooleanSetting showPing = register(new BooleanSetting("Показывать пинг", false)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final BooleanSetting showEyes = register(new BooleanSetting("Глаза на голове", true)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final NumberSetting eyeSize = register(new NumberSetting("Размер глаз", 1.0, 0.5, 2.0, 0.05, "")
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && showEyes.getValue()));
    public final ColorSetting eyeColor = register(new ColorSetting("Цвет глаз", 0xFFFFFFFF)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && showEyes.getValue()));
    public final ColorSetting pupilColor = register(new ColorSetting("Цвет зрачка", 0xFF0F0F0F)
            .visibleWhen(() -> mode.is(MODE_HUIHUI) && showEyes.getValue()));
    public final TextSetting bgImage = register(new TextSetting("Картинка фона", "", 256)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final TextSetting headImage = register(new TextSetting("Картинка головы", "", 256)
            .visibleWhen(() -> mode.is(MODE_HUIHUI)));
    public final ButtonSetting openEditor = register(new ButtonSetting(
            "Редактор таргетхуда", "Открыть редактор", () -> TargetHudEditorOverlay.open(TargetHudFeature.this)
    ).nonPersistent());

    private LivingEntity target;
    private LivingEntity previous;
    private LivingEntity lastMathEntity;
    private final Animation attackAnimation = new Animation(600L, Animation.Easing.EASE_OUT_QUAD);
    private final Animation toggleMetanoise = new Animation(300L, Animation.Easing.EASE_OUT_QUAD);
    private float animatedHp;
    private float outdatedHealth;
    private boolean previewMode;
    private long blinkElapsed;
    private long lastFrameNanos;
    private boolean animationTarget;

    private float hudWidth = -1.0F;
    private float hudHeight = -1.0F;
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;

    private String cachedName;
    private String cachedHealthText;
    private String cachedDisplayText;
    private String cachedPercentText;
    private String cachedSecondary;
    private float cachedHealthValue = Float.NaN;
    private int cachedPercentValue = -1;

    private static final class LoadedImage {
        final Identifier id;
        final int width;
        final int height;

        LoadedImage(Identifier id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }

    private static final Map<String, LoadedImage> imageCache = new HashMap<>();

    public TargetHudFeature() {
        super("TargetHud", "Отображает информацию о цели", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onFinalGuiRender(FinalGuiRenderEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.player == null || TargetHudEditorOverlay.isOpen()) {
            return;
        }
        net.minecraft.client.gui.screens.Screen screen = mc.gui.screen();
        if (screen != null && !(screen instanceof ChatScreen) && !(screen instanceof InventoryScreen)) {
            return;
        }
        if (this.dragging && screen == null) {
            this.dragging = false;
        }
        RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
        try {
            Render2DUtil.beginFrame();
            if (this.dragging) {
                updateDragPosition(mc);
            }
            LivingEntity resolved = resolveTarget(mc);
            boolean present = resolved != null;
            if (present || this.previous != null) {
                LivingEntity draw = present ? resolved : this.previous;
                render(mc, draw, present, totalAlpha(mc));
                this.hudWidth = currentWidth();
                this.hudHeight = currentHeight();
            } else {
                updateAnimations(false);
                updateDisplayMath(null);
                this.hudWidth = -1.0F;
                this.hudHeight = -1.0F;
            }
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        Minecraft mc = MinecraftContext.mc;
        if (mc == null || mc.player == null
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || TargetHudEditorOverlay.isOpen()) {
            return;
        }
        boolean guiOpen = mc.gui.screen() instanceof ChatScreen || mc.gui.screen() instanceof InventoryScreen;
        if (!guiOpen) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (this.dragging) {
                this.dragging = false;
                this.dragOffsetX = 0.0F;
                this.dragOffsetY = 0.0F;
                event.cancel();
            }
            return;
        }
        if (event.getAction() != GLFW.GLFW_PRESS || this.dragging
                || this.hudWidth <= 0.0F || this.hudHeight <= 0.0F) {
            return;
        }
        float mouseX = mouseX(mc);
        float mouseY = mouseY(mc);
        if (!inside(mouseX, mouseY,
                this.x.getValue().floatValue() + this.dragOffsetX,
                this.y.getValue().floatValue() + this.dragOffsetY,
                this.hudWidth, this.hudHeight)) {
            return;
        }
        this.dragging = true;
        this.dragOffsetX = mouseX - this.x.getValue().floatValue();
        this.dragOffsetY = mouseY - this.y.getValue().floatValue();
        event.cancel();
    }

    public void renderPreview(Minecraft mc, LivingEntity entity, float px, float py) {
        GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
        if (extractor == null) {
            return;
        }
        Matrix3x2fStack pose = extractor.pose();
        pose.pushMatrix();
        pose.translate(px - this.x.getValue().floatValue(), py - this.y.getValue().floatValue());
        this.previewMode = true;
        try {
            render(mc, entity, true, 1.0F);
        } finally {
            this.previewMode = false;
            pose.popMatrix();
        }
    }

    private void render(Minecraft mc, LivingEntity entity, boolean present, float alphaMultiplier) {
        updateAnimations(present);
        updateDisplayMath(present ? entity : null);
        updateCachedStrings(entity);
        float alpha = (present ? 1.0F : this.attackAnimation.getValue()) * alphaMultiplier;
        if (alpha <= 0.01F) {
            return;
        }
        Palette pal = palette(animFraction(entity), alpha);
        float scale = this.hudScale.getValue().floatValue();
        GuiGraphicsExtractor scaleExtractor = RenderContext.currentGuiGraphicsExtractor();
        Matrix3x2fStack scalePose = scaleExtractor == null ? null : scaleExtractor.pose();
        boolean scaled = scalePose != null && Math.abs(scale - 1.0F) > 0.001F;
        if (scaled) {
            scalePose.pushMatrix();
            scalePose.translate(x0(), y0());
            scalePose.scale(scale, scale);
            scalePose.translate(-x0(), -y0());
        }
        try {
            if (this.mode.is(MODE_BLADE)) {
                renderBlade(mc, entity, alpha, pal);
                return;
            }
            switch (this.type.getValue()) {
                case TYPE_SMALL -> renderSmall(mc, entity, alpha, pal);
                case TYPE_PERCENT -> renderPercent(mc, entity, alpha, pal);
                case TYPE_BIG_BAR -> renderBigBar(mc, entity, alpha, pal);
                case TYPE_VERTICAL -> renderVertical(mc, entity, alpha, pal);
                case TYPE_METANOISE -> renderMetanoise(mc, entity, alpha, pal);
                case TYPE_MINI -> renderMini(mc, entity, alpha, pal);
                case TYPE_GRADIENT -> renderGradient(mc, entity, alpha, pal);
                case TYPE_BOSS_BAR -> renderBossBar(mc, entity, alpha, pal);
                case TYPE_MINIMAL -> renderMinimal(mc, entity, alpha, pal);
                case TYPE_CIRCLE -> renderCircle(mc, entity, alpha, pal);
                default -> renderLarge(mc, entity, alpha, pal);
            }
            float belowY = y0() + rawHeight() + 2.0F;
            if (this.showArmor.getValue()) {
                drawArmor(entity, x0() + 2.0F, belowY, alpha);
                belowY += 12.0F;
            }
            if (this.showPing.getValue()) {
                int ping = pingOf(mc, entity);
                if (ping >= 0) {
                    Render2DUtil.text(x0() + rawWidth() - 2.0F, belowY, 5.0F, ping + " ms")
                            .weight(400)
                            .color(pal.textSecond)
                            .align(TextAlign.RIGHT)
                            .draw();
                }
            }
        } finally {
            if (scaled) {
                scalePose.popMatrix();
            }
        }
    }

    private float x0() {
        return this.x.getValue().floatValue();
    }

    private float y0() {
        return this.y.getValue().floatValue();
    }

    private static int pingOf(Minecraft mc, LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayer player) || mc.getConnection() == null) {
            return -1;
        }
        net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    private void updateCachedStrings(LivingEntity entity) {
        if (entity == null || entity != this.target) {
            return;
        }
        String currentName = entity.getName().getString();
        float health = animValue(entity);
        int percent = (int) (animFraction(entity) * 100.0F);
        if (currentName.equals(this.cachedName)
                && this.cachedDisplayText != null
                && this.cachedPercentValue == percent
                && this.cachedHealthValue == health) {
            return;
        }
        this.cachedName = currentName;
        this.cachedHealthValue = health;
        this.cachedPercentValue = percent;
        this.cachedHealthText = Integer.toString((int) Math.ceil(health));

        String mode = this.displayMode.getValue();
        if ("Проценты и HP".equals(mode)) {
            this.cachedDisplayText = percent + "% " + (int) Math.ceil(health);
            this.cachedSecondary = "HP " + (int) Math.ceil(animValue(entity)) + "/"
                    + (int) Math.ceil(maxHealth(entity));
        } else if ("Проценты".equals(mode)) {
            this.cachedDisplayText = percent + "%";
            this.cachedSecondary = this.cachedDisplayText;
        } else {
            this.cachedDisplayText = String.valueOf((int) Math.ceil(health));
            this.cachedSecondary = this.cachedDisplayText;
        }

        this.cachedPercentText = percent + "%";
    }

    private void renderBlade(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = this.x.getValue().floatValue();
        float y = this.y.getValue().floatValue();
        float size = 22.0F * this.headSize.getValue().floatValue();
        float width = 154.0F + (size - 22.0F);
        float height = 46.0F;

        if (this.customColors.getValue()) {
            Render2DUtil.rect(x, y, width, height)
                    .color(pal.bg)
                    .radius(8.0F)
                    .draw();
        } else {
            Render2DUtil.rect(x, y, width, height)
                    .glass(alpha, 0.5F, 8.0F)
                    .radius(8.0F)
                    .draw();
        }

        float headSize = 24.0F * this.headSize.getValue().floatValue();
        float headX = x + 8.0F;
        float headY = y + 8.0F;
        drawHead(entity, headX, headY, headSize, alpha);

        float infoX = headX + headSize + 6.0F;
        String healthText = this.cachedHealthText != null && entity == this.lastMathEntity
                ? this.cachedHealthText
                : Integer.toString((int) Math.ceil(animValue(entity)));
        float healthWidth = measure(healthText, 400, 10.0F);
        String name = ellipsize(entityName(entity), 400, 10.0F,
                x + width - 8.0F - infoX - healthWidth - 6.0F);

        float textTop = headY + headSize / 2.0F - 7.0F;
        Render2DUtil.text(infoX, textTop, 10.0F, name)
                .weight(500)
                .color(pal.text)
                .draw();
        Render2DUtil.text(x + width - 8.0F, textTop, 10.0F, healthText)
                .weight(500)
                .color(pal.text)
                .align(TextAlign.RIGHT)
                .draw();

        float barY = y + height - 16.0F;
        float barWidth = x + width - 8.0F - infoX;
        float fraction = animFraction(entity);
        Render2DUtil.rect(infoX, barY, barWidth, 4.0F)
                .color(pal.track)
                .radius(2.0F)
                .draw();
        float fillWidth = barWidth * fraction;
        if (fillWidth >= 2.0F) {
            Render2DUtil.rect(infoX, barY, fillWidth, 4.0F)
                    .color(this.customColors.getValue() ? pal.bar : ColorUtil.multiplyAlpha(Theme.getAccent(), alpha))
                    .radius(2.0F)
                    .draw();
        }

        if (this.showArmor.getValue()) {
            drawArmor(entity, infoX, Math.min(headY + headSize + 2.0F, y + height - 12.0F), alpha);
        }
    }

    private static float totalAlpha(Minecraft mc) {
        return 1.0F;
    }

    private void updateAnimations(boolean present) {
        if (present != this.animationTarget) {
            this.animationTarget = present;
            this.attackAnimation.animate(this.attackAnimation.getValue(), present ? 1.0F : 0.0F,
                    500L, Animation.Easing.EASE_OUT_QUAD);
        }
    }

    private void renderLarge(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 28.0F * this.headSize.getValue().floatValue();
        float nameSize = Math.max(5.0F, size * 0.3F);
        float bigSize = nameSize + 4.0F;
        float barH = barHeight(4.0F);
        float width = Math.max(52.0F, size * 1.5F + 12.0F);

        float headY = y + 3.0F;
        float bigTop = headY + size + 3.0F;
        float nameTop = bigTop + bigSize + 1.0F;
        float barY = nameTop + nameSize + 4.0F;
        float height = barY - y + barH + 4.0F;

        float corner = this.radius.getValue().floatValue() * 0.6F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + width / 2.0F - size / 2.0F, headY, size, alpha);

        String big = displayText(entity);
        float bigWidth = measure(big, 600, bigSize);
        if (bigWidth <= width * 0.92F) {
            Render2DUtil.text(x + width / 2.0F, bigTop, bigSize, big)
                    .weight(600)
                    .color(pal.text)
                    .align(TextAlign.CENTER)
                    .draw();
        }

        String name = entityName(entity);
        float avail = width - 8.0F;
        if (measure(name, 400, nameSize) > avail) {
            name = ellipsize(name, 400, nameSize, avail);
        }
        Render2DUtil.text(x + width / 2.0F, nameTop, nameSize, name)
                .weight(400)
                .color(ColorUtil.multiplyAlpha(pal.text, 0.85F))
                .align(TextAlign.CENTER)
                .draw();

        drawBars(entity, x + 4.0F, barY, width - 8.0F, barH, pal);
    }

    private void renderSmall(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 18.0F * this.headSize.getValue().floatValue();
        float nameSize = 6.5F;
        float barH = barHeight(3.5F);
        float width = Math.max(48.0F, size + 16.0F);

        float headY = y + 3.0F;
        float barY = headY + size + 4.0F;
        float nameTop = barY + barH + 2.0F;
        float height = nameTop - y + nameSize + 3.0F;

        float corner = this.radius.getValue().floatValue() * 0.5F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 3.0F, headY, size, alpha);
        drawBars(entity, x + 3.0F, barY, width - 6.0F, barH, pal);

        String name = ellipsize(entityName(entity), 400, nameSize, width - 6.0F);
        Render2DUtil.text(x + width / 2.0F, nameTop, nameSize, name)
                .weight(400)
                .color(pal.text)
                .align(TextAlign.CENTER)
                .draw();
    }

    private void renderPercent(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 22.0F * this.headSize.getValue().floatValue();
        float width = Math.max(56.0F, size + 4.0F);
        float textSize = 5.5F;
        float barH = barHeight(4.0F);

        float headY = y + 2.0F;
        float percentTop = headY + size + 4.0F;
        float barY = percentTop + textSize + 2.0F + 4.0F;
        float nameTop = barY + barH + 3.0F;
        float nameSize = textSize - 0.5F;
        float height = nameTop - y + nameSize + 3.0F;

        float corner = this.radius.getValue().floatValue() * 0.55F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 2.0F, headY, size, alpha);

        Render2DUtil.text(x + width / 2.0F, percentTop, textSize + 2.0F, displayPercent(entity))
                .weight(600)
                .color(pal.text)
                .align(TextAlign.CENTER)
                .draw();
        drawBars(entity, x + 2.0F, barY, width - 4.0F, barH, pal);
        Render2DUtil.text(x + width / 2.0F, nameTop, nameSize,
                        ellipsize(entityName(entity), 400, nameSize, width - 4.0F))
                .weight(400)
                .color(pal.text)
                .align(TextAlign.CENTER)
                .draw();
    }

    private void renderBigBar(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 10.0F * this.headSize.getValue().floatValue();
        float nameSize = 6.0F;
        String name = entityName(entity);
        float width = Math.max(82.0F, size + measure(name, 400, nameSize + 4.0F) + 34.0F);
        float barH = barHeight(9.0F);
        float barY = Math.max(16.0F, 3.0F + size + 3.0F);
        float height = barY + barH + 3.0F;

        float corner = this.radius.getValue().floatValue() * 0.5F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 3.0F, y + 3.0F, size, alpha);
        float infoX = x + 3.0F + size + 5.0F;
        float rightEdge = x + width - 3.0F;
        float displayW = measure(displayText(entity), 400, nameSize + 2.0F);
        float nameMax = rightEdge - infoX - displayW - 6.0F;
        Render2DUtil.text(infoX, y + 3.0F, nameSize + 4.0F, ellipsize(name, 400, nameSize + 4.0F, nameMax))
                .weight(400)
                .color(pal.text)
                .draw();
        Render2DUtil.text(rightEdge, y + 4.0F, nameSize + 2.0F, displayText(entity))
                .weight(400)
                .color(pal.text)
                .align(TextAlign.RIGHT)
                .draw();

        drawBars(entity, infoX, barY, rightEdge - 1.0F - infoX, barH, pal);
    }

    private void renderVertical(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 20.0F * this.headSize.getValue().floatValue();
        float width = 36.0F;

        float nameSize = 4.5F;
        float textSize = 5.0F;
        float headY = y + 4.0F;
        float nameTop = headY + size + 6.0F;
        float percentTop = nameTop + nameSize + 3.0F;
        float height = percentTop - y + textSize + 4.0F;

        float corner = this.radius.getValue().floatValue() * 0.55F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 4.0F, headY, size, alpha);

        float totalH = size - 2.0F;
        float barW = 4.0F;
        float barX = x + width - 7.0F;
        float hpFrac = animFraction(entity);
        Render2DUtil.rect(barX, headY, barW, totalH)
                .color(pal.track)
                .radius(2.0F)
                .draw();
        float fillH = totalH * hpFrac;
        if (fillH > 0.0F) {
            Render2DUtil.rect(barX, headY + totalH - fillH, barW, fillH)
                    .color(pal.bar)
                    .radius(2.0F)
                    .draw();
        }

        String name = ellipsize(entityName(entity), 400, nameSize, width - 4.0F);
        Render2DUtil.text(x + width / 2.0F, nameTop, nameSize, name)
                .weight(400)
                .color(pal.text)
                .align(TextAlign.CENTER)
                .draw();
        Render2DUtil.text(x + width / 2.0F, percentTop, textSize, displayPercent(entity))
                .weight(600)
                .color(pal.bar)
                .align(TextAlign.CENTER)
                .draw();
    }

    private void renderMini(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = this.x.getValue().floatValue();
        float y = this.y.getValue().floatValue();
        float size = 16.0F * this.headSize.getValue().floatValue();
        float width = Math.max(40.0F, size + 22.0F);
        float height = 32.0F;

        float corner = this.radius.getValue().floatValue() * 0.4F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 2.0F, y + 2.0F, size, alpha);

        float nameSize = 5.5F;
        float infoX = x + 2.0F + size + 3.0F;
        float infoW = width - infoX + x - 2.0F;
        String name = ellipsize(entityName(entity), 400, nameSize, infoW);
        Render2DUtil.text(infoX, y + 5.0F, nameSize, name)
                .weight(400)
                .color(pal.text)
                .draw();

        float hpFrac = animFraction(entity);
        Render2DUtil.rect(infoX, y + 13.0F, infoW, 2.0F)
                .color(pal.track)
                .radius(1.0F)
                .draw();
        float fill = infoW * hpFrac;
        if (fill > 0.0F) {
            Render2DUtil.rect(infoX, y + 13.0F, fill, 2.0F)
                    .color(pal.barSecond)
                    .radius(1.0F)
                    .draw();
        }

        String second = secondaryDisplay(entity);
        Render2DUtil.text(infoX, y + 20.0F, 5.0F, second)
                .weight(400)
                .color(pal.textSecond)
                .draw();
    }

    private void renderGradient(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 24.0F * this.headSize.getValue().floatValue();
        float width = Math.max(58.0F, size + 10.0F);
        float nameSize = 5.5F;
        float barH = barHeight(5.0F);

        float headY = y + 4.0F;
        float barTop = headY + size + 6.0F;
        float nameTop = barTop + barH + 4.0F;
        float height = nameTop - y + nameSize + 4.0F;

        float corner = this.radius.getValue().floatValue() * 0.55F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 4.0F, headY, size, alpha);

        Render2DUtil.rect(x + 6.0F, barTop, width - 12.0F, barH)
                .color(pal.bar, pal.barSecond, pal.bar, pal.barSecond)
                .radius(barH / 2.0F)
                .draw();

        String name = ellipsize(entityName(entity), 400, nameSize, width - 8.0F);
        Render2DUtil.text(x + width / 2.0F, nameTop, nameSize, name)
                .weight(400)
                .color(pal.text)
                .align(TextAlign.CENTER)
                .draw();
    }

    private void renderMinimal(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = this.x.getValue().floatValue();
        float y = this.y.getValue().floatValue();
        float size = 14.0F * this.headSize.getValue().floatValue();
        float nameSize = 5.0F;
        String name = entityName(entity);
        float width = Math.max(52.0F, size + measure(name, 400, nameSize + 1.0F) + 26.0F);
        float height = 32.0F;

        float corner = this.radius.getValue().floatValue() * 0.45F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 3.0F, y + 3.0F, size, alpha);

        float infoX = x + 3.0F + size + 5.0F;
        float infoW = width - infoX + x - 3.0F;
        Render2DUtil.text(infoX, y + 4.0F, nameSize + 1.0F,
                        ellipsize(name, 400, nameSize + 1.0F, infoW))
                .weight(400)
                .color(pal.text)
                .draw();
        String second = displayText(entity);
        Render2DUtil.text(infoX, y + 13.0F, 5.0F, second)
                .weight(600)
                .color(pal.textSecond)
                .draw();

        float hpFrac = animFraction(entity);
        Render2DUtil.rect(infoX, y + 19.0F, infoW, 3.0F)
                .color(pal.track)
                .radius(1.0F)
                .draw();
        float fill = infoW * hpFrac;
        if (fill > 0.0F) {
            Render2DUtil.rect(infoX, y + 19.0F, fill, 3.0F)
                    .color(pal.bar)
                    .radius(1.0F)
                    .draw();
        }
    }

    private void renderBossBar(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 30.0F * this.headSize.getValue().floatValue();
        float nameSize = 7.0F;
        float textSize = nameSize + 4.0F;
        String name = entityName(entity);
        String hpText = displayText(entity);
        float hpW = measure(hpText, 600, textSize);
        float nameW = measure(name, 600, textSize);
        float width = Math.max(110.0F, size + nameW + hpW + 34.0F);
        float barH = barHeight(9.0F);
        float barY = y + 5.0F + nameSize + 4.0F + 4.0F;
        float height = Math.max(size + 10.0F, barY - y + barH + 4.0F);

        float corner = this.radius.getValue().floatValue() * 0.5F;
        drawBackground(mc, x, y, width, height, corner, pal.bg, pal.border, borderThickness.getValue().floatValue(), alpha);

        drawHead(entity, x + 4.0F, y + 4.0F, size, alpha);

        float infoX = x + 4.0F + size + 5.0F;
        float nameMax = x + width - 4.0F - hpW - 8.0F - infoX;
        Render2DUtil.text(infoX, y + 5.0F, textSize, ellipsize(name, 600, textSize, Math.max(10.0F, nameMax)))
                .weight(600)
                .color(pal.text)
                .draw();
        Render2DUtil.text(x + width - 4.0F, y + 5.0F, textSize, hpText)
                .weight(600)
                .color(pal.text)
                .align(TextAlign.RIGHT)
                .draw();
        drawBars(entity, infoX, barY, width - 4.0F - size - 9.0F, barH, pal);
    }

    private void renderMetanoise(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 28.0F * this.headSize.getValue().floatValue();
        float width = size + 20.0F;
        float nameSize = 5.5F;
        float barH = barHeight(4.0F);

        float percentTop = y + 2.0F + size + 4.0F;
        float barY = percentTop + nameSize + 2.0F + 4.0F;
        float height = barY - y + barH + 4.0F;

        int baseC = this.customColors.getValue()
                ? pal.bg
                : ColorUtil.multiplyAlpha(ColorUtil.rgba(12, 12, 14, this.backgroundAlpha.getValue().intValue()), alpha);
        Render2DUtil.rect(x, y, width, height)
                .color(baseC)
                .radius(3.0F)
                .draw();
        Render2DUtil.rect(x, y, width, height)
                .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha * 0.5F))
                .radius(3.0F)
                .blur(11.0F, alpha * 0.4F)
                .draw();
        Render2DUtil.rect(x, y, width, height)
                .color(baseC)
                .radius(3.0F)
                .draw();
        drawBgImage(x, y, width, height, 3.0F, alpha);

        float sheen = this.toggleMetanoise.getValue();
        float sheenX = x + (width - 36.0F) * sheen;
        Render2DUtil.rect(sheenX, y + 2.0F, 36.0F, height - 4.0F)
                .color(ColorUtil.multiplyAlpha(pal.bar, 0.4F),
                        ColorUtil.TRANSPARENT, ColorUtil.TRANSPARENT, ColorUtil.TRANSPARENT)
                .radius(3.0F)
                .draw();

        drawHead(entity, x + 2.0F, y + 2.0F, size, alpha);

        Render2DUtil.text(x + width / 2.0F, percentTop, nameSize + 2.0F, displayPercent(entity))
                .weight(600)
                .color(pal.bar)
                .align(TextAlign.CENTER)
                .draw();

        drawBars(entity, x + 5.0F, barY, width - 10.0F, barH, pal);
    }

    private void renderCircle(Minecraft mc, LivingEntity entity, float alpha, Palette pal) {
        float x = x0();
        float y = y0();
        float size = 20.0F * this.headSize.getValue().floatValue();
        float ringOuter = size / 2.0F + 5.0F;
        float cx = x + ringOuter;
        float cy = y + ringOuter;
        float ringThickness = 4.0F;
        float ringBottom = y + ringOuter * 2.0F;

        float hpFrac = animFraction(entity);
        drawRing(cx, cy, ringOuter - ringThickness / 2.0F, ringThickness, 0.0F, 1.0F,
                ColorUtil.multiplyAlpha(ColorUtil.rgba(255, 255, 255, 26), alpha));
        if (hpFrac > 0.0F) {
            drawRing(cx, cy, ringOuter - ringThickness / 2.0F, ringThickness, 0.0F, hpFrac, pal.bar);
        }
        if (this.showSecondBar.getValue()) {
            float start = absorptionStart(hpFrac, entity);
            if (hpFrac > start) {
                drawRing(cx, cy, ringOuter - ringThickness / 2.0F, ringThickness * 0.7F, start, hpFrac, pal.barSecond);
            }
        }

        float headSize = Math.min(size, ringOuter * 2.0F - 2.0F);
        drawHead(entity, cx - headSize / 2.0F, cy - headSize / 2.0F, headSize, alpha);

        float textSize = 4.0F;
        float percentTop = ringBottom + 2.0F;
        float nameTop = percentTop + textSize + 4.0F;
        float nameSize = 4.0F;

        Render2DUtil.text(cx, percentTop, textSize + 1.0F, displayPercent(entity))
                .weight(600)
                .color(pal.text)
                .align(TextAlign.CENTER)
                .draw();
        Render2DUtil.text(cx, nameTop, nameSize,
                        ellipsize(entityName(entity), 400, nameSize, ringOuter * 2.0F + 8.0F))
                .weight(400)
                .color(ColorUtil.multiplyAlpha(pal.text, 0.8F))
                .align(TextAlign.CENTER)
                .draw();
    }

    private void drawRing(float cx, float cy, float radius, float thickness, float from, float to, int color) {
        int count = 64;
        int start = (int) (from * count);
        int end = (int) Math.ceil(to * count);
        float dot = thickness * 0.92F;
        for (int i = start; i < end; i++) {
            float angle = (float) ((i + 0.5F) / count * Math.PI * 2.0D - Math.PI / 2.0D);
            float dx = cx + (float) Math.cos(angle) * radius;
            float dy = cy + (float) Math.sin(angle) * radius;
            Render2DUtil.rect(dx - dot / 2.0F, dy - dot / 2.0F, dot, dot)
                    .color(color)
                    .radius(dot / 2.0F)
                    .draw();
        }
    }

    private void drawBars(LivingEntity entity, float x, float y, float w, float h, Palette pal) {
        float hpFrac = animFraction(entity);

        Render2DUtil.rect(x, y, w, h)
                .color(pal.track)
                .radius(this.barRadius.getValue().floatValue())
                .draw();

        float outdated = Mth.clamp(this.outdatedHealth / maxHealth(entity), 0.0F, 1.0F);
        if (outdated > hpFrac) {
            Render2DUtil.rect(x + hpFrac * w, y, (outdated - hpFrac) * w, h)
                    .color(pal.outdated)
                    .radius(this.barRadius.getValue().floatValue())
                    .draw();
        }

        if (hpFrac > 0.0F) {
            Render2DUtil.rect(x, y, hpFrac * w, h)
                    .color(pal.bar)
                    .radius(this.barRadius.getValue().floatValue())
                    .draw();
        }

        if (this.showSecondBar.getValue()) {
            float start = absorptionStart(hpFrac, entity);
            if (hpFrac > start) {
                boolean match = this.matchBarThickness.getValue();
                float goldH = match ? h : barHeight(4.0F) * this.secondBarThickness.getValue().floatValue();
                float goldY = match ? y : y + h - goldH - 2.0F;
                Render2DUtil.rect(x + start * w, goldY, (hpFrac - start) * w, goldH)
                        .color(pal.barSecond)
                        .radius(this.barRadius.getValue().floatValue())
                        .draw();
            }
        }
    }

    private float barHeight(float base) {
        return base * this.barThickness.getValue().floatValue();
    }

    private void drawBackground(Minecraft mc, float x, float y, float w, float h, float cornerRadius, int bg, int border, float borderWidth, float alpha) {
        if (bg != ColorUtil.TRANSPARENT) {
            Render2DUtil.rect(x, y, w, h)
                    .color(bg)
                    .radius(cornerRadius)
                    .draw();
        }
        drawBgImage(x, y, w, h, cornerRadius, alpha);
        if (borderWidth > 0.0F && border != ColorUtil.TRANSPARENT) {
            Render2DUtil.rect(x, y, w, h)
                    .color(ColorUtil.TRANSPARENT)
                    .border(borderWidth, border)
                    .radius(cornerRadius)
                    .draw();
        }
    }

    private void drawBgImage(float x, float y, float w, float h, float radius, float alpha) {
        String bgPath = this.bgImage.getValue();
        if (bgPath == null || bgPath.isBlank()) {
            return;
        }
        LoadedImage image = loadImage(bgPath);
        if (image == null) {
            return;
        }
        Render2DUtil.texture(x, y, w, h, image.id)
                .managed()
                .uv(0.0F, 0.0F, 1.0F, 1.0F)
                .radius(radius)
                .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha))
                .draw();
    }
    private float currentHeadYaw() {
        if (this.headAutoRotate.getValue()) {
            return (float) ((System.currentTimeMillis() / 8L) % 360L);
        }
        return this.headYaw.getValue().floatValue();
    }

    private void drawHead(LivingEntity entity, float x, float y, float size, float alpha) {
        float yaw = currentHeadYaw();
        float pitch = this.headPitch.getValue().floatValue();
        String custom = this.headImage.getValue();
        if (custom != null && !custom.isBlank()) {
            LoadedImage image = loadImage(custom);
            if (image != null) {
                float scaleY = PlayerHead.pitchScale(pitch);
                float drawH = size * scaleY;
                Render2DUtil.texture(x, y + (size - drawH) / 2.0F, size, drawH, image.id)
                        .managed()
                        .uv(0.0F, 0.0F, 1.0F, 1.0F)
                        .radius(size * 0.2F)
                        .color(ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha))
                        .draw();
            }
            return;
        }
        if (entity instanceof AbstractClientPlayer player) {
            PlayerHead.draw(x, y, size, player.getSkin().body().texturePath(), size * 0.2F,
                    ColorUtil.multiplyAlpha(ColorUtil.WHITE, alpha), yaw, pitch);
            if (this.showEyes.getValue()) {
                drawEyes(x, y, size, alpha, yaw, pitch);
            }
        } else {
            float scaleY = PlayerHead.pitchScale(pitch);
            float drawH = size * scaleY;
            Render2DUtil.rect(x, y + (size - drawH) / 2.0F, size, drawH)
                    .color(ColorUtil.multiplyAlpha(ColorUtil.rgba(60, 60, 70, 160), alpha))
                    .radius(size * 0.2F)
                    .draw();
        }
    }

    private void drawEyes(float x, float y, float size, float alpha, float yaw, float pitch) {
        float facing = PlayerHead.facing(yaw);
        if (facing <= 0.15F) {
            return;
        }
        float eyeAlpha = alpha * Math.min(1.0F, (facing - 0.15F) / 0.5F);
        if (eyeAlpha <= 0.02F) {
            return;
        }

        long cycle = this.blinkElapsed % 4200L;
        float open = cycle > 3800L
                ? Math.max(0.1F, 1.0F - (cycle - 3800L) / 400.0F)
                : 1.0F;

        float scaleY = PlayerHead.pitchScale(pitch);
        float eyeScale = this.eyeSize.getValue().floatValue();
        float eyeW = size * 0.25F * eyeScale * Math.max(0.35F, facing);
        float eH = size * 0.25F * eyeScale;
        float eyeH = eH * open * scaleY;

        float headTop = y + (size - size * scaleY) / 2.0F;
        float eyeY = headTop + size * scaleY * 0.5F - eyeH / 2.0F;
        float centerX = x + size * 0.5F;
        float eye1 = centerX + (x + size * 0.375F - centerX) * facing;
        float eye2 = centerX + (x + size * 0.75F - centerX) * facing;

        int eyeC = ColorUtil.multiplyAlpha(this.eyeColor.getValue(), eyeAlpha);
        int pupilC = ColorUtil.multiplyAlpha(this.pupilColor.getValue(), eyeAlpha);

        drawSingleEye(eye1 - eyeW / 2.0F, eyeY, eyeW, eyeH, eyeC, pupilC, eyeAlpha);
        drawSingleEye(eye2 - eyeW / 2.0F, eyeY, eyeW, eyeH, eyeC, pupilC, eyeAlpha);
    }

    private void drawSingleEye(float ex, float ey, float eyeW, float eyeH, int eyeC, int pupilC, float alpha) {
        Render2DUtil.rect(ex, ey, eyeW, eyeH)
                .color(eyeC)
                .radius(eyeH / 2.0F)
                .draw();
        float pupilW = eyeW * 0.5F;
        float pupilH = eyeH * 0.6F;
        float px = ex + (eyeW - pupilW) / 2.0F;
        float py = ey + (eyeH - pupilH) / 2.0F;
        Render2DUtil.rect(px, py, pupilW, pupilH)
                .color(pupilC)
                .radius(pupilH / 4.0F)
                .draw();
        float hiW = pupilW * 0.4F;
        float hiH = pupilH * 0.35F;
        Render2DUtil.rect(px + pupilW - hiW - pupilW * 0.06F, py + pupilH * 0.1F, hiW, hiH)
                .color(ColorUtil.rgba(255, 255, 255, (int) (200 * alpha)))
                .radius(hiH / 3.0F)
                .draw();
    }

    private void drawArmor(LivingEntity entity, float x, float y, float alpha) {
        ItemStack[] stacks = new ItemStack[4];
        int count = 0;
        for (EquipmentSlot slot : ARMOR_ORDER) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stacks[count++] = stack;
            }
        }
        if (count == 0) {
            return;
        }
        GuiGraphicsExtractor extractor = RenderContext.currentGuiGraphicsExtractor();
        if (extractor == null) {
            return;
        }
        Render2DUtil.flush();
        Matrix3x2fStack pose = extractor.pose();
        float icon = 10.0F;
        float gap = 2.0F;
        float cursor = x;
        for (int i = 0; i < count; i++) {
            pose.pushMatrix();
            pose.translate(cursor, y);
            pose.scale(icon / 16.0F);
            extractor.item(stacks[i], 0, 0);
            pose.popMatrix();
            cursor += icon + gap;
        }
        Render2DUtil.flush();
    }

    private record Palette(
            int bg,
            int border,
            int text,
            int textSecond,
            int track,
            int outdated,
            int bar,
            int barSecond
    ) {
    }

    private Palette palette(float progress, float alpha) {
        boolean custom = this.customColors.getValue();

        int bgBase = custom ? this.bgColor.getValue() : 0xFF000000;
        float colorAlpha = ColorUtil.alpha(bgBase) / 255.0F;
        int bg = ColorUtil.rgba(ColorUtil.red(bgBase), ColorUtil.green(bgBase), ColorUtil.blue(bgBase),
                (int) (MathUtil.clamp(this.backgroundAlpha.getValue() * colorAlpha, 0.0, 255.0) * alpha));

        int border = ColorUtil.multiplyAlpha(
                custom ? this.borderColor.getValue() : ColorUtil.rgba(100, 100, 115, 255), alpha);

        int text = ColorUtil.multiplyAlpha(
                custom ? this.textColor.getValue() : ColorUtil.WHITE, alpha);

        int textSecond = custom
                ? ColorUtil.multiplyAlpha(ColorUtil.multiplyAlpha(this.textColor.getValue(), 0.75F), alpha)
                : ColorUtil.multiplyAlpha(ColorUtil.rgba(203, 203, 210, 255), alpha);

        int track;
        if (custom) {
            int tb = this.bgColor.getValue();
            track = ColorUtil.rgba(ColorUtil.red(tb), ColorUtil.green(tb), ColorUtil.blue(tb),
                    Math.round(200.0F * alpha));
        } else {
            track = ColorUtil.rgba(0, 0, 0, Math.round(90.0F * alpha));
        }

        int outdated = custom
                ? ColorUtil.multiplyAlpha(ColorUtil.multiplyRgb(this.barColor.getValue(), 0.65F), alpha)
                : ColorUtil.multiplyAlpha(ColorUtil.multiplyRgb(hpColor(progress), 0.55F), alpha);

        int bar = custom
                ? ColorUtil.multiplyAlpha(this.barColor.getValue(), alpha)
                : ColorUtil.multiplyAlpha(hpColor(progress), alpha);

        int barSecond = custom
                ? ColorUtil.multiplyAlpha(this.barColorSecond.getValue(), alpha)
                : ColorUtil.multiplyAlpha(hpColor2(progress), alpha);

        return new Palette(bg, border, text, textSecond, track, outdated, bar, barSecond);
    }

    private int hpColor(float progress) {
        if (progress >= 0.8F) {
            return ColorUtil.rgba(0, 200, 83, 255);
        }
        if (progress >= 0.55F) {
            return ColorUtil.lerp(ColorUtil.rgba(0, 200, 83, 255), ColorUtil.rgba(255, 220, 0, 255),
                    (0.8F - progress) / 0.25F);
        }
        if (progress >= 0.35F) {
            return ColorUtil.lerp(ColorUtil.rgba(255, 220, 0, 255), ColorUtil.rgba(255, 140, 0, 255),
                    (0.55F - progress) / 0.2F);
        }
        if (progress >= 0.15F) {
            return ColorUtil.lerp(ColorUtil.rgba(255, 140, 0, 255), ColorUtil.rgba(255, 60, 0, 255),
                    (0.35F - progress) / 0.2F);
        }
        return ColorUtil.rgba(255, 60, 0, 255);
    }

    private int hpColor2(float progress) {
        if (progress >= 0.65F) {
            return ColorUtil.lerp(ColorUtil.rgba(0, 200, 83, 255), ColorUtil.rgba(255, 220, 0, 255),
                    (0.85F - progress) / 0.2F);
        }
        if (progress >= 0.35F) {
            return ColorUtil.lerp(ColorUtil.rgba(255, 220, 0, 255), ColorUtil.rgba(255, 140, 0, 255),
                    (0.65F - progress) / 0.3F);
        }
        return ColorUtil.lerp(ColorUtil.rgba(255, 140, 0, 255), ColorUtil.rgba(255, 60, 0, 255),
                (0.35F - progress) / 0.35F);
    }

    private float animValue(LivingEntity entity) {
        if (entity == null) {
            return 0.0F;
        }
        if (entity != this.target || !this.animatedHpToggle.getValue()) {
            return totalHealth(entity);
        }
        return this.animatedHp;
    }

    private float animFraction(LivingEntity entity) {
        if (entity == null) {
            return 0.0F;
        }
        return Mth.clamp(animValue(entity) / maxHealth(entity), 0.0F, 1.0F);
    }

    private float totalHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    private float maxHealth(LivingEntity entity) {
        return Math.max(1.0F, entity.getMaxHealth() + entity.getAbsorptionAmount());
    }

    private float displayAbsorption(LivingEntity entity) {
        float absorption = entity.getAbsorptionAmount();
        if (absorption > 0.0F) {
            return absorption;
        }
        if (this.previewMode && this.showSecondBar.getValue()) {
            return Math.max(1.0F, entity.getMaxHealth() * 0.2F);
        }
        return 0.0F;
    }

    private float absorptionStart(float hpFrac, LivingEntity entity) {
        float absorption = displayAbsorption(entity);
        if (absorption <= 0.0F) {
            return hpFrac;
        }
        float denom = Math.max(1.0F, entity.getMaxHealth() + absorption);
        float absFrac = Mth.clamp(absorption / denom, 0.0F, 1.0F);
        return Math.max(0.0F, hpFrac - absFrac);
    }

    private String displayText(LivingEntity entity) {
        if (entity == this.lastMathEntity && this.cachedDisplayText != null) {
            return this.cachedDisplayText;
        }
        String mode = this.displayMode.getValue();
        if ("Проценты и HP".equals(mode)) {
            return (int) (animFraction(entity) * 100.0F) + "% " + (int) Math.ceil(animValue(entity));
        }
        if ("Проценты".equals(mode)) {
            return (int) (animFraction(entity) * 100.0F) + "%";
        }
        return String.valueOf((int) Math.ceil(animValue(entity)));
    }

    private String displayPercent(LivingEntity entity) {
        if (entity == this.lastMathEntity && this.cachedPercentText != null) {
            return this.cachedPercentText;
        }
        return (int) (animFraction(entity) * 100.0F) + "%";
    }

    private String entityName(LivingEntity entity) {
        if (entity == this.lastMathEntity && this.cachedName != null) {
            return this.cachedName;
        }
        return entity.getName().getString();
    }

    private String secondaryDisplay(LivingEntity entity) {
        if (entity == this.lastMathEntity && this.cachedSecondary != null) {
            return this.cachedSecondary;
        }
        String mode = this.displayMode.getValue();
        if ("Проценты и HP".equals(mode)) {
            return "HP " + (int) Math.ceil(animValue(entity)) + "/" + (int) Math.ceil(maxHealth(entity));
        }
        return displayText(entity);
    }

    private void updateDisplayMath(LivingEntity entity) {
        long now = System.nanoTime();
        float delta = this.lastFrameNanos == 0L ? 0.016F
                : Mth.clamp((now - this.lastFrameNanos) / 1.0E9F, 0.0F, 0.1F);
        this.lastFrameNanos = now;

        float max = 20.0F;
        if (entity != null) {
            max = maxHealth(entity);
        } else if (this.lastMathEntity != null) {
            max = maxHealth(this.lastMathEntity);
        }
        float targetHp = entity != null ? totalHealth(entity) : 0.0F;
        if (entity != null && entity != this.lastMathEntity) {
            this.lastMathEntity = entity;
            this.animatedHp = targetHp;
            this.outdatedHealth = targetHp;
            this.cachedName = null;
            this.cachedHealthText = null;
            this.cachedDisplayText = null;
            this.cachedPercentText = null;
            this.cachedSecondary = null;
        } else if (entity != null) {
            if (!this.animatedHpToggle.getValue()) {
                this.animatedHp = targetHp;
            } else {
                float step = Mth.clamp(delta * 8.0F * this.animationSpeed.getValue().floatValue(), 0.0F, 1.0F);
                this.animatedHp += (targetHp - this.animatedHp) * step;
            }
            this.outdatedHealth = Math.max(this.animatedHp, targetHp);
        } else {
            float step = Mth.clamp(delta * 8.0F, 0.0F, 1.0F);
            this.animatedHp += (0.0F - this.animatedHp) * step;
            this.outdatedHealth = this.animatedHp;
        }

        this.blinkElapsed += (long) (delta * 1000.0F);
        this.toggleMetanoise.animate(this.toggleMetanoise.getValue(), entity != null ? 1.0F : 0.0F,
                300L, Animation.Easing.EASE_OUT_QUAD);
    }

    public float currentWidth() {
        return rawWidth() * this.hudScale.getValue().floatValue();
    }

    public float currentHeight() {
        return rawHeight() * this.hudScale.getValue().floatValue();
    }

    private float rawWidth() {
        if (this.mode.is(MODE_BLADE)) {
            return 154.0F + (22.0F * this.headSize.getValue().floatValue() - 22.0F);
        }
        float size = this.headSize.getValue().floatValue();
        return switch (this.type.getValue()) {
            case TYPE_SMALL -> Math.max(48.0F, 18.0F * size + 16.0F);
            case TYPE_PERCENT -> Math.max(56.0F, 22.0F * size + 4.0F);
            case TYPE_BIG_BAR -> Math.max(82.0F, 10.0F * size + 40.0F);
            case TYPE_VERTICAL -> 36.0F;
            case TYPE_METANOISE -> 28.0F * size + 20.0F;
            case TYPE_MINI -> Math.max(48.0F, 16.0F * size + 22.0F);
            case TYPE_GRADIENT -> Math.max(58.0F, 24.0F * size + 10.0F);
            case TYPE_BOSS_BAR -> Math.max(110.0F, 30.0F * size + 30.0F);
            case TYPE_MINIMAL -> Math.max(52.0F, 14.0F * size + 26.0F);
            case TYPE_CIRCLE -> 20.0F * size + 10.0F;
            default -> Math.max(52.0F, 28.0F * size * 1.5F + 12.0F);
        };
    }

    private float rawHeight() {
        float size = this.headSize.getValue().floatValue();
        float thickness = this.barThickness.getValue().floatValue();
        if (this.mode.is(MODE_BLADE)) {
            return 46.0F;
        }
        return switch (this.type.getValue()) {
            case TYPE_SMALL -> 18.0F * size + 18.5F + 3.5F * thickness;
            case TYPE_PERCENT -> 22.0F * size + 24.5F + 4.0F * thickness;
            case TYPE_BIG_BAR -> Math.max(16.0F, 6.0F + 10.0F * size) + 9.0F * thickness + 3.0F;
            case TYPE_VERTICAL -> 20.0F * size + 26.5F;
            case TYPE_METANOISE -> 28.0F * size + 21.5F + 4.0F * thickness;
            case TYPE_MINI -> 32.0F;
            case TYPE_GRADIENT -> 24.0F * size + 23.5F + 5.0F * thickness;
            case TYPE_BOSS_BAR -> Math.max(30.0F * size + 10.0F, 24.0F + 9.0F * thickness);
            case TYPE_MINIMAL -> 32.0F;
            case TYPE_CIRCLE -> 20.0F * size + 28.0F;
            default -> {
                float base = 28.0F * size;
                float nameSize = Math.max(5.0F, base * 0.3F);
                yield base + 19.0F + 2.0F * nameSize + 4.0F * thickness;
            }
        };
    }

    public boolean isDragsWhileHovering() {
        return this.hover.getValue();
    }

    private float measure(String text, int weight, float size) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        return UiFonts.getFontForText(text, weight).measureWidth(text, size, 0.0F);
    }

    private String ellipsize(String text, int weight, float size, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 1.0F) {
            return "";
        }
        if (measure(text, weight, size) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        String truncated = text;
        while (truncated.length() > 1 && measure(truncated + ellipsis, weight, size) > maxWidth) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated + ellipsis;
    }

    private static LoadedImage loadImage(String path) {
        LoadedImage cached = imageCache.get(path);
        if (cached != null) {
            return cached.width > 0 ? cached : null;
        }
        try {
            Path file = Path.of(path);
            if (!Files.exists(file)) {
                imageCache.put(path, new LoadedImage(Identifier.parse("missingno"), 0, 0));
                return null;
            }
            NativeImage image = NativeImage.read(Files.newInputStream(file));
            Identifier id = Identifier.parse("blade:targethud_custom_" + Integer.toHexString(path.hashCode()));
            Minecraft.getInstance().getTextureManager().register(
                    id, new net.minecraft.client.renderer.texture.DynamicTexture(id::toString, image));
            LoadedImage loaded = new LoadedImage(id, image.getWidth(), image.getHeight());
            imageCache.put(path, loaded);
            return loaded;
        } catch (Exception e) {
            imageCache.put(path, new LoadedImage(Identifier.parse("missingno"), 0, 0));
            return null;
        }
    }

    private LivingEntity resolveTarget(Minecraft mc) {
        LivingEntity combat = null;
        AuraFeature aura = AuraFeature.getMarkerFeature();
        if (aura != null) {
            combat = aura.getCurrentTarget();
        }
        if (combat == null) {
            TriggerBotFeature triggerBot = TriggerBotFeature.getEnabled();
            if (triggerBot != null) {
                combat = triggerBot.getCurrentTarget();
            }
        }
        LivingEntity current = this.mode.is(MODE_BLADE) ? combat : null;
        boolean inChat = mc.gui.screen() instanceof ChatScreen || mc.gui.screen() instanceof InventoryScreen;
        if (inChat) {
            current = mc.player;
        }
        if (current == null && this.hover.getValue()
                && mc.hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living
                && living.isAlive()) {
            current = living;
        }
        if (current == null) {
            current = combat;
        }
        if (current != null && !current.isAlive()) {
            return null;
        }
        if (current != this.target) {
            this.previous = this.target;
        }
        this.target = current;
        return current;
    }

    private static boolean inside(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    private void updateDragPosition(Minecraft mc) {
        float w = currentWidth();
        float h = currentHeight();
        if (w <= 0.0F || h <= 0.0F) {
            return;
        }
        float scaledWidth = mc.getWindow().getGuiScaledWidth();
        float scaledHeight = mc.getWindow().getGuiScaledHeight();
        float mx = mouseX(mc);
        float my = mouseY(mc);
        float nx = MathUtil.clamp(mx - this.dragOffsetX, 0.0F, Math.max(0.0F, scaledWidth - w));
        float ny = MathUtil.clamp(my - this.dragOffsetY, 0.0F, Math.max(0.0F, scaledHeight - h));
        this.x.setValue((double) nx);
        this.y.setValue((double) ny);
    }

    private static float mouseX(Minecraft mc) {
        return (float) mc.mouseHandler.getScaledXPos(mc.getWindow());
    }

    private static float mouseY(Minecraft mc) {
        return (float) mc.mouseHandler.getScaledYPos(mc.getWindow());
    }

    private static double clampPos(double value, double max) {
        return Math.max(0.0, Math.min(max, value));
    }
}