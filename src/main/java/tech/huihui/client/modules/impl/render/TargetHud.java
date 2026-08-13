package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.base.events.impl.input.EventMouse;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.font.MsdfRenderer;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.client.modules.impl.combat.Aura;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.client.modules.impl.misc.ScoreboardHealth;
import tech.huihui.client.screens.targethud.TargetHudEditScreen;
import tech.huihui.client.screens.targethud.TargetHudPreset;
import tech.huihui.utility.game.player.PlayerIntersectionUtil;
import tech.huihui.utility.mixin.accessors.DrawContextAccessor;
import tech.huihui.utility.render.display.StencilUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.GuiUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "TargetHud",
   category = Category.RENDER,
   description = "Отображает информацию о цели"
)
public final class TargetHud extends Module {
   public static final TargetHud INSTANCE = new TargetHud();
   public final ModeSetting type = new ModeSetting("Тип", "Крупный", "Маленький", "Проценты", "Большая полоса", "Вертикальный", "Метал", "Мини", "Градиент", "Боссбар", "Минимал", "Кружок");
   public final NumberSetting x = new NumberSetting("X", 4.0F, 0.0F, 1920.0F, 1.0F);
   public final NumberSetting y = new NumberSetting("Y", 4.0F, 0.0F, 1080.0F, 1.0F);
   private final BooleanSetting hover = new BooleanSetting("Наведение", true);
    public final NumberSetting barThickness = new NumberSetting("Толщина полоски", 1.0F, 0.5F, 3.0F, 0.05F);
    public final NumberSetting barRadius = new NumberSetting("Скругление полоски", 2.0F, 0.0F, 6.0F, 0.1F);
   public final BooleanSetting showSecondBar = new BooleanSetting("Золотая полоска", false);
   public final BooleanSetting matchBarThickness = new BooleanSetting("Полоски одного размера", true);
   public final NumberSetting secondBarThickness = new NumberSetting("Толщина золотой полоски", 1.0F, 0.5F, 3.0F, 0.05F);
   public final NumberSetting headSize = new NumberSetting("Размер иконки головы", 1.0F, 0.5F, 2.0F, 0.05F);
   public final NumberSetting headYaw = new NumberSetting("Поворот головы", 0.0F, -180.0F, 180.0F, 1.0F);
   public final NumberSetting headPitch = new NumberSetting("Наклон головы", 0.0F, -90.0F, 90.0F, 1.0F);
   public final BooleanSetting headAutoRotate = new BooleanSetting("Автоповорот головы", false);
   public final ModeSetting displayMode = new ModeSetting("Отображение", "Проценты и HP", "Проценты", "HP");
   public final BooleanSetting customColors = new BooleanSetting("Свои цвета", false);
   public final ColorSetting barColor = new ColorSetting("Цвет полоски", new ColorRGBA(58, 152, 255, 255));
   public final ColorSetting bgColor = new ColorSetting("Цвет фона", new ColorRGBA(0, 0, 0, 160));
   public final ColorSetting borderColor = new ColorSetting("Цвет рамки", new ColorRGBA(100, 100, 115, 255));
   public final ColorSetting textColor = new ColorSetting("Цвет текста", ColorRGBA.WHITE);
   public final ColorSetting barColorSecond = new ColorSetting("Второй цвет полоски", new ColorRGBA(100, 100, 115, 255));
   public final NumberSetting radius = new NumberSetting("Скругление", 5.0F, 1.0F, 10.0F, 0.5F);
   public final NumberSetting borderThickness = new NumberSetting("Толщина рамки", 1.0F, 0.5F, 3.0F, 0.1F);
   public final NumberSetting backgroundAlpha = new NumberSetting("Прозрачность фона", 120.0F, 0.0F, 255.0F, 5.0F);
   public final NumberSetting animationSpeed = new NumberSetting("Скорость анимации", 1.0F, 0.1F, 3.0F, 0.1F);
    public final BooleanSetting showArmor = new BooleanSetting("Показывать броню", true);
    public final BooleanSetting showPing = new BooleanSetting("Показывать пинг", true);
    public final BooleanSetting showEyes = new BooleanSetting("Глаза на голове", true);
    public final NumberSetting eyeSize = new NumberSetting("Размер глаз", 1.0F, 0.5F, 2.0F, 0.05F);
    public final ColorSetting eyeColor = new ColorSetting("Цвет глаз", new ColorRGBA(255, 255, 255, 255));
    public final ColorSetting pupilColor = new ColorSetting("Цвет зрачка", new ColorRGBA(15, 15, 15, 255));
    public final StringSetting bgImage = new StringSetting("Картинка фона", "");
    public final StringSetting headImage = new StringSetting("Картинка головы", "");
   private final ButtonSetting openEditor = new ButtonSetting("открыть редактор таргетхуда", TargetHudEditScreen::openEditor);
   private final Animation visibleAnimation = new Animation(220L, Easing.CIRC_OUT);
   private final Animation healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
   private final Animation outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
   private final Animation gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
   private final Animation toggleAnimationMetanoise = new Animation(1850L, Easing.CIRC_OUT);
    private boolean dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private LivingEntity target;
    private final Map<String, LoadedImage> imageCache = new HashMap();
    private final Set<String> failedImages = new HashSet();
   private static final float[][] CUBE_CORNERS = new float[][]{{-1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F, 1.0F}, {-1.0F, 1.0F, -1.0F}, {-1.0F, 1.0F, 1.0F}, {1.0F, -1.0F, -1.0F}, {1.0F, -1.0F, 1.0F}, {1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, 1.0F}};
   private static final HeadFace[] HEAD_FACES = new HeadFace[]{
      new HeadFace(new float[][]{{-1.0F, 1.0F, 1.0F}, {1.0F, 1.0F, 1.0F}, {1.0F, -1.0F, 1.0F}, {-1.0F, -1.0F, 1.0F}}, 0.125F, 0.125F, 0.25F, 0.25F, 1.0F),
      new HeadFace(new float[][]{{-1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, -1.0F}, {1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F, -1.0F}}, 0.375F, 0.125F, 0.5F, 0.25F, 1.0F),
      new HeadFace(new float[][]{{-1.0F, 1.0F, 1.0F}, {-1.0F, 1.0F, -1.0F}, {-1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F, 1.0F}}, 0.25F, 0.125F, 0.375F, 0.25F, 1.0F),
      new HeadFace(new float[][]{{1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, 1.0F}, {1.0F, -1.0F, 1.0F}, {1.0F, -1.0F, -1.0F}}, 0.0F, 0.125F, 0.125F, 0.25F, 1.0F),
      new HeadFace(new float[][]{{-1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, 1.0F}, {-1.0F, 1.0F, 1.0F}}, 0.125F, 0.0F, 0.25F, 0.125F, 1.0F),
      new HeadFace(new float[][]{{-1.0F, -1.0F, 1.0F}, {1.0F, -1.0F, 1.0F}, {1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F, -1.0F}}, 0.25F, 0.0F, 0.375F, 0.125F, 1.0F)
   };
   private static final HeadFace[] HAT_FACES = new HeadFace[]{
      new HeadFace(new float[][]{{-1.0F, 1.0F, 1.0F}, {1.0F, 1.0F, 1.0F}, {1.0F, -1.0F, 1.0F}, {-1.0F, -1.0F, 1.0F}}, 0.625F, 0.125F, 0.75F, 0.25F, 1.125F),
      new HeadFace(new float[][]{{-1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, -1.0F}, {1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F, -1.0F}}, 0.875F, 0.125F, 1.0F, 0.25F, 1.125F),
      new HeadFace(new float[][]{{-1.0F, 1.0F, 1.0F}, {-1.0F, 1.0F, -1.0F}, {-1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F, 1.0F}}, 0.75F, 0.125F, 0.875F, 0.25F, 1.125F),
      new HeadFace(new float[][]{{1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, 1.0F}, {1.0F, -1.0F, 1.0F}, {1.0F, -1.0F, -1.0F}}, 0.5F, 0.125F, 0.625F, 0.25F, 1.125F),
      new HeadFace(new float[][]{{-1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, -1.0F}, {1.0F, 1.0F, 1.0F}, {-1.0F, 1.0F, 1.0F}}, 0.625F, 0.0F, 0.75F, 0.125F, 1.125F),
      new HeadFace(new float[][]{{-1.0F, -1.0F, 1.0F}, {1.0F, -1.0F, 1.0F}, {1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F, -1.0F}}, 0.75F, 0.0F, 0.875F, 0.125F, 1.125F)
   };

   private TargetHud() {
   }

   @EventTarget
   @Native
   public void onRender(EventHudRender event) {
      if (mc.world == null || mc.player == null || mc.options.hudHidden) {
         return;
      }
      if (mc.currentScreen instanceof TargetHudEditScreen) {
         return;
      }
      if (this.dragging) {
         Vector2f mousePos = GuiUtil.getMouse(2.0D);
         float[] size = this.currentSize();
         float scaledWidth = (float)mc.getWindow().getScaledWidth();
         float scaledHeight = (float)mc.getWindow().getScaledHeight();
         this.x.setCurrent(MathHelper.clamp(mousePos.getX() - this.dragOffsetX, 0.0F, Math.max(scaledWidth - size[0], 0.0F)));
         this.y.setCurrent(MathHelper.clamp(mousePos.getY() - this.dragOffsetY, 0.0F, Math.max(scaledHeight - size[1], 0.0F)));
      }
      boolean inChat = mc.currentScreen instanceof ChatScreen;
      LivingEntity current = inChat ? mc.player : this.getTarget();
      this.setTarget(current);
      this.applyAnimationSpeed();
      if (this.visibleAnimation.getValue() == 0.0F || this.target == null) {
         if (inChat && this.visibleAnimation.getValue() == 0.0F) {
            this.renderPlaceholder(event.getContext());
         }
         return;
      }
      CustomDrawContext ctx = event.getContext();
      float alpha = this.visibleAnimation.getValue();
      this.renderCurrentType(ctx, alpha);
   }

   public void renderPreview(CustomDrawContext ctx) {
      if (mc.player == null) {
         return;
      }
      this.target = mc.player;
      this.visibleAnimation.setValue(1.0F);
      this.toggleAnimationMetanoise.setValue(1.0F);
      this.applyAnimationSpeed();
      this.updateAnimations();
      this.renderCurrentType(ctx, 1.0F);
   }

   private void renderCurrentType(CustomDrawContext ctx, float alpha) {
      if (this.type.is("Крупный")) {
         this.renderLarge(ctx, alpha);
      } else if (this.type.is("Маленький")) {
         this.renderSmall(ctx, alpha);
      } else if (this.type.is("Проценты")) {
         this.renderPercent(ctx, alpha);
      } else if (this.type.is("Большая полоса")) {
         this.renderBigBar(ctx, alpha);
      } else if (this.type.is("Вертикальный")) {
         this.renderVertical(ctx, alpha);
      } else if (this.type.is("Метал")) {
         this.renderMetanoise(ctx, alpha);
      } else if (this.type.is("Мини")) {
         this.renderMini(ctx, alpha);
      } else if (this.type.is("Градиент")) {
         this.renderGradient(ctx, alpha);
       } else if (this.type.is("Боссбар")) {
          this.renderBossbar(ctx, alpha);
       } else if (this.type.is("Кружок")) {
          this.renderCircle(ctx, alpha);
       } else {
          this.renderMinimal(ctx, alpha);
       }
   }

   @EventTarget
   @Native
   public void onMouse(EventMouse event) {
      if (!(mc.currentScreen instanceof ChatScreen)) {
         this.dragging = false;
         return;
      }
      Vector2f mousePos = GuiUtil.getMouse(2.0D);
      float mouseX = mousePos.getX();
      float mouseY = mousePos.getY();
      float[] size = this.currentSize();
      if (event.getAction() == 1 && event.getButton() == 0) {
         if (mouseX >= this.x.getCurrent() && mouseX <= this.x.getCurrent() + size[0] && mouseY >= this.y.getCurrent() && mouseY <= this.y.getCurrent() + size[1]) {
            this.dragging = true;
            this.dragOffsetX = mouseX - this.x.getCurrent();
            this.dragOffsetY = mouseY - this.y.getCurrent();
         }
      } else if (event.getAction() == 0) {
         this.dragging = false;
      }
   }

   private void renderPlaceholder(CustomDrawContext ctx) {
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float[] size = this.currentSize();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, size[0], size[1], BorderRadius.all(4.0F), (new ColorRGBA(0, 0, 0)).withAlpha(110.0F));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, size[0], size[1], 1.0F, BorderRadius.all(4.0F), this.theme().getSecondColor().withAlpha(200.0F));
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), "TargetHud", x + 4.0F, y + size[1] / 2.0F - 3.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F));
      if (this.dragging) {
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), x - 1.0F, y - 1.0F, size[0] + 2.0F, size[1] + 2.0F, 1.0F, BorderRadius.all(5.0F), this.theme().getColor().withAlpha(255.0F));
      }
   }

   public void applyPreset(TargetHudPreset preset) {
      this.type.set(preset.getType());
      this.x.setCurrent(preset.getX());
      this.y.setCurrent(preset.getY());
      this.barThickness.setCurrent(preset.getBarThickness());
       this.barRadius.setCurrent(preset.getBarRadius());
      this.showSecondBar.setEnabled(preset.isShowSecondBar());
      this.matchBarThickness.setEnabled(preset.isMatchBarThickness());
      this.secondBarThickness.setCurrent(preset.getSecondBarThickness());
      this.headSize.setCurrent(preset.getHeadSize());
      this.headYaw.setCurrent(preset.getHeadYaw());
      this.headPitch.setCurrent(preset.getHeadPitch());
      this.headAutoRotate.setEnabled(preset.isHeadAutoRotate());
      this.displayMode.set(preset.getDisplayMode());
      this.customColors.setEnabled(preset.isCustomColors());
      this.barColor.setColor(preset.getBarColor());
      this.bgColor.setColor(preset.getBgColor());
      this.borderColor.setColor(preset.getBorderColor());
      this.textColor.setColor(preset.getTextColor());
      this.barColorSecond.setColor(preset.getBarColorSecond());
      this.radius.setCurrent(preset.getRadius());
      this.borderThickness.setCurrent(preset.getBorderThickness());
      this.backgroundAlpha.setCurrent(preset.getBackgroundAlpha());
      this.animationSpeed.setCurrent(preset.getAnimationSpeed());
      this.showArmor.setEnabled(preset.isShowArmor());
      this.showPing.setEnabled(preset.isShowPing());
      this.showEyes.setEnabled(preset.isShowEyes());
      this.eyeSize.setCurrent(preset.getEyeSize());
      this.eyeColor.setColor(preset.getEyeColor());
      this.pupilColor.setColor(preset.getPupilColor());
      this.bgImage.setValue(preset.getBgImage());
      this.headImage.setValue(preset.getHeadImage());
      this.clearImageCache();
   }

   public TargetHudPreset toPreset(String name) {
      TargetHudPreset preset = new TargetHudPreset();
      preset.setName(name);
      preset.setType(this.type.get());
      preset.setX(this.x.getCurrent());
      preset.setY(this.y.getCurrent());
      preset.setBarThickness(this.barThickness.getCurrent());
       preset.setBarRadius(this.barRadius.getCurrent());
      preset.setShowSecondBar(this.showSecondBar.isEnabled());
      preset.setMatchBarThickness(this.matchBarThickness.isEnabled());
      preset.setSecondBarThickness(this.secondBarThickness.getCurrent());
      preset.setHeadSize(this.headSize.getCurrent());
      preset.setHeadYaw(this.headYaw.getCurrent());
      preset.setHeadPitch(this.headPitch.getCurrent());
      preset.setHeadAutoRotate(this.headAutoRotate.isEnabled());
      preset.setDisplayMode(this.displayMode.get());
      preset.setCustomColors(this.customColors.isEnabled());
      preset.setBarColor(this.barColor.getIntColor());
      preset.setBgColor(this.bgColor.getIntColor());
      preset.setBorderColor(this.borderColor.getIntColor());
      preset.setTextColor(this.textColor.getIntColor());
      preset.setBarColorSecond(this.barColorSecond.getIntColor());
      preset.setRadius(this.radius.getCurrent());
      preset.setBorderThickness(this.borderThickness.getCurrent());
      preset.setBackgroundAlpha(this.backgroundAlpha.getCurrent());
      preset.setAnimationSpeed(this.animationSpeed.getCurrent());
      preset.setShowArmor(this.showArmor.isEnabled());
      preset.setShowPing(this.showPing.isEnabled());
      preset.setShowEyes(this.showEyes.isEnabled());
      preset.setEyeSize(this.eyeSize.getCurrent());
      preset.setEyeColor(this.eyeColor.getIntColor());
      preset.setPupilColor(this.pupilColor.getIntColor());
      preset.setBgImage(this.bgImage.getValue());
      preset.setHeadImage(this.headImage.getValue());
      return preset;
   }

   private String cachedSizeType = "";
   private float[] cachedSize = new float[]{110.0F, 22.0F};

   public float[] currentSize() {
      String type = this.type.get();
      if (!this.cachedSizeType.equals(type)) {
         this.cachedSizeType = type;
         if (type.equals("Крупный")) {
            this.cachedSize = new float[]{132.0F, 48.0F + this.secondBarExtra(4.0F)};
         } else if (type.equals("Маленький")) {
            this.cachedSize = new float[]{84.0F, 20.0F + this.secondBarExtra(2.0F)};
         } else if (type.equals("Проценты")) {
            this.cachedSize = new float[]{112.0F, 42.0F + this.secondBarExtra(4.0F)};
         } else if (type.equals("Большая полоса")) {
            this.cachedSize = new float[]{124.0F, 34.0F + this.secondBarExtra(10.0F)};
         } else if (type.equals("Вертикальный")) {
            this.cachedSize = new float[]{74.0F, 58.0F};
         } else if (type.equals("Метал")) {
            this.cachedSize = new float[]{86.0F, 30.0F + this.secondBarExtra(3.25F)};
         } else if (type.equals("Мини")) {
            this.cachedSize = new float[]{64.0F, 16.0F + this.secondBarExtra(2.0F)};
         } else if (type.equals("Градиент")) {
            this.cachedSize = new float[]{112.0F, 38.0F + this.secondBarExtra(5.0F)};
         } else if (type.equals("Боссбар")) {
            this.cachedSize = new float[]{180.0F, 34.0F + this.secondBarExtra(8.0F)};
         } else if (type.equals("Кружок")) {
            this.cachedSize = new float[]{132.0F, 48.0F + this.secondBarExtra(5.0F)};
         } else {
            this.cachedSize = new float[]{110.0F, 22.0F};
         }
      }
      return this.cachedSize;
   }

   private void setTarget(LivingEntity target) {      if (target == null) {
         this.visibleAnimation.update(0.0F);
         this.toggleAnimationMetanoise.update(0.0F);
         this.toggleAnimationMetanoise.setDuration(2200L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         if (this.toggleAnimationMetanoise.getValue() == 0.0F) {
            this.target = null;
         }
      } else {
         this.target = target;
         this.toggleAnimationMetanoise.update(1.0F);
         this.toggleAnimationMetanoise.setDuration(1300L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         this.visibleAnimation.update(1.0F);
      }
   }

   private LivingEntity getTarget() {
      LivingEntity auraTarget = Aura.INSTANCE.getTarget();
      if (auraTarget != null && auraTarget.isAlive()) {
         return auraTarget;
      }
      if (this.hover.isEnabled() && mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
         return living;
      }
      return null;
   }

   private Theme theme() {
      return HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
   }

   private String name() {
      return this.target == mc.player ? NameProtect.getCustomName() : this.target.getNameForScoreboard();
   }

   private float hp() {
      return ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(this.target) : this.target.getHealth();
   }

   private String hpText() {
      float hp = this.hp();
      int tenth = Math.round(hp * 10.0F);
      return tenth / 10 + "." + Math.abs(tenth % 10);
   }

   private String percentText() {
      float percent = this.hp() / this.target.getMaxHealth() * 100.0F;
      return Math.round(percent) + "%";
   }

   private String bigText() {
      if (this.displayMode.is("HP")) {
         return this.hpText();
      }
      return this.percentText();
   }

   private String secondaryText() {
      if (this.displayMode.is("Проценты")) {
         return "HP: " + this.hpText();
      }
      if (this.displayMode.is("HP")) {
         return this.percentText();
      }
      return "HP: " + this.hpText();
   }

   private String infoText() {
      String info;
      if (this.displayMode.is("Проценты")) {
         info = this.percentText();
      } else if (this.displayMode.is("HP")) {
         info = "HP: " + this.hpText();
      } else {
         info = "HP: " + this.hpText() + " (" + this.percentText() + ")";
      }
      if (this.showPing.isEnabled()) {
         String ping = this.ping();
         if (ping != null) {
            info = info + " · " + ping;
         }
      }
      return info;
   }

   private String ping() {
      if (this.target instanceof PlayerEntity player) {
         PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         if (entry != null) {
            return "Пинг: " + entry.getLatency() + "мс";
         }
      }
      return null;
   }

   private boolean customColorsEnabled() {
      return this.customColors.isEnabled();
   }

   private ColorRGBA textColor(float alpha) {
      return this.customColorsEnabled() ? this.textColor.getColor().withAlpha(255.0F * alpha) : ColorRGBA.WHITE.withAlpha(255.0F * alpha);
   }

   private ColorRGBA bgColor(float alpha) {
      float factor = this.backgroundAlpha.getCurrent() / 255.0F;
      float a = 255.0F * alpha * factor;
      return this.customColorsEnabled() ? this.bgColor.getColor().withAlpha(a) : (new ColorRGBA(0, 0, 0)).withAlpha(a);
   }

   private ColorRGBA borderColor(float alpha) {
      return this.customColorsEnabled() ? this.borderColor.getColor().withAlpha(255.0F * alpha) : this.theme().getSecondColor().darker(0.5F).withAlpha(255.0F * alpha);
   }

   private ColorRGBA trackColor(float alpha) {
      return this.customColorsEnabled() ? this.bgColor.getColor().withAlpha(200.0F * alpha) : (new ColorRGBA(0, 0, 0)).withAlpha(90.0F * alpha);
   }

   private ColorRGBA barColor(float alpha) {
      return this.customColorsEnabled() ? this.barColor.getColor().withAlpha(255.0F * alpha) : this.theme().getColor().withAlpha(255.0F * alpha);
   }

   private ColorRGBA barColorSecond(float alpha) {
      return this.customColorsEnabled() ? this.barColor.getColor().withAlpha(255.0F * alpha) : this.theme().getSecondColor().withAlpha(255.0F * alpha);
   }

   private ColorRGBA[] barColors(float alpha) {
      if (this.customColorsEnabled()) {
         ColorRGBA a = this.barColor.getColor().withAlpha(255.0F * alpha);
         ColorRGBA b = this.barColorSecond.getColor().withAlpha(255.0F * alpha);
         return new ColorRGBA[]{a, b, a, b};
      }
      Theme theme = this.theme();
      return new ColorRGBA[]{theme.getSecondColor().withAlpha(255.0F * alpha), theme.getSecondColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha)};
   }

   private ColorRGBA[] outdatedColors(float alpha) {
      if (this.customColorsEnabled()) {
         ColorRGBA darker = this.barColor.getColor().darker(0.35F).withAlpha(255.0F * alpha);
         return new ColorRGBA[]{darker, darker, darker, darker};
      }
      Theme theme = this.theme();
      return new ColorRGBA[]{theme.getSecondColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getSecondColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getColor().darker(0.35F).withAlpha(255.0F * alpha)};
   }

   private void updateAnimations() {
      float hp = this.hp();
      float maxHp = this.target.getMaxHealth();
      this.healthAnimation.update(hp / maxHp);
      if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
         this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
         this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
      } else {
         this.outdatedHealthAnimation.update(hp / maxHp);
      }
      this.gappleAnimation.update(this.target.getAbsorptionAmount() / maxHp);
   }

   private void applyAnimationSpeed() {
      float speed = this.animationSpeed.getCurrent();
      this.visibleAnimation.setDuration((long)(220.0F / speed));
      this.healthAnimation.setDuration((long)(250.0F / speed));
      this.outdatedHealthAnimation.setDuration((long)(650.0F / speed));
      this.gappleAnimation.setDuration((long)(250.0F / speed));
      this.toggleAnimationMetanoise.setDuration((long)(1300.0F / speed));
   }

   private ColorRGBA[] hpColors(Theme theme, float alpha) {
      if (this.customColorsEnabled()) {
         ColorRGBA c = this.barColor.getColor().withAlpha(255.0F * alpha);
         return new ColorRGBA[]{c, c, c, c};
      }
      float percent = this.hp() / this.target.getMaxHealth();
      if (percent <= 0.25F) {
         return new ColorRGBA[]{(new ColorRGBA(255, 60, 60)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 60, 60)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 30, 30)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 30, 30)).withAlpha(255.0F * alpha)};
      } else if (percent <= 0.5F) {
         return new ColorRGBA[]{(new ColorRGBA(255, 200, 40)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 200, 40)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 160, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 160, 20)).withAlpha(255.0F * alpha)};
      } else {
         return new ColorRGBA[]{theme.getSecondColor().withAlpha(255.0F * alpha), theme.getSecondColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha)};
      }
   }

   private void drawBackground(CustomDrawContext ctx, float x, float y, float width, float height, float alpha) {
      float radius = this.radius.getCurrent();
      LoadedImage bg = this.loadImage(this.bgImage.getValue());
      if (bg != null) {
         this.drawImageCover(ctx, bg, x, y, width, height, radius, alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(radius), this.bgColor(alpha).withAlpha(255.0F * alpha * 0.4F));
      } else {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(radius), this.bgColor(alpha));
      }
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, this.borderThickness.getCurrent(), BorderRadius.all(radius), this.borderColor(alpha));
   }

   private void drawBar(CustomDrawContext ctx, float x, float y, float width, float height, float radius, float alpha) {
      float barHeight = height * this.barThickness.getCurrent();
      if (this.showSecondBar.isEnabled()) {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, barHeight, BorderRadius.all(radius), this.trackColor(alpha));
         float outdatedW = MathHelper.clamp(width * this.outdatedHealthAnimation.getValue(), 0.0F, width);
         ColorRGBA[] outdated = this.outdatedColors(alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, outdatedW, barHeight, BorderRadius.all(radius), outdated[0], outdated[1], outdated[2], outdated[3]);
         float mainW = MathHelper.clamp(width * this.healthAnimation.getValue(), 0.0F, width);
         ColorRGBA[] main = this.hpColors(this.theme(), alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, mainW, barHeight, BorderRadius.all(radius), main[0], main[1], main[2], main[3]);
         float goldH = this.goldBarHeight(height);
         float goldY = y + barHeight + 2.0F;
         this.drawGoldTrack(ctx, x, goldY, width, goldH, radius, alpha);
         this.drawGoldFill(ctx, x, goldY, width, goldH, radius, alpha);
         return;
      }
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, barHeight, BorderRadius.all(radius), this.trackColor(alpha));
      float outdatedW = MathHelper.clamp(width * this.outdatedHealthAnimation.getValue(), 0.0F, width);
      ColorRGBA[] outdated = this.outdatedColors(alpha);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, outdatedW, barHeight, BorderRadius.all(radius), outdated[0], outdated[1], outdated[2], outdated[3]);
      if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
         float mainW = MathHelper.clamp(width * this.healthAnimation.getValue(), 0.0F, width);
         ColorRGBA[] main = this.hpColors(this.theme(), alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, mainW, barHeight, BorderRadius.all(radius), main[0], main[1], main[2], main[3]);
      }
      float absW = MathHelper.clamp(width * this.gappleAnimation.getValue(), 0.0F, width);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, absW, barHeight, BorderRadius.all(radius), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
   }

   private float goldBarHeight(float height) {
      float thickness = this.matchBarThickness.isEnabled() ? this.barThickness.getCurrent() : this.secondBarThickness.getCurrent();
      return height * thickness;
   }

   private void drawGoldTrack(CustomDrawContext ctx, float x, float y, float width, float height, float radius, float alpha) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(radius), (new ColorRGBA(84, 64, 10)).withAlpha(170.0F * alpha));
   }

   private void drawGoldFill(CustomDrawContext ctx, float x, float y, float width, float height, float radius, float alpha) {
      float absW = MathHelper.clamp(width * this.gappleAnimation.getValue(), 0.0F, width);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, absW, height, BorderRadius.all(radius), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
   }

   private float secondBarExtra(float heightParam) {
      return this.showSecondBar.isEnabled() ? this.goldBarHeight(heightParam) + 2.0F : 0.0F;
   }

   private Identifier skinTexture() {
      if (this.target instanceof PlayerEntity player) {
         try {
            Iterator var3 = mc.getNetworkHandler().getPlayerList().iterator();
            while(var3.hasNext()) {
               PlayerListEntry entry = (PlayerListEntry)var3.next();
               if (entry.getProfile().getName().equals(player.getNameForScoreboard())) {
                  return entry.getSkinTextures().texture();
               }
            }
         } catch (Exception var5) {
         }
      }
      return DefaultSkinHelper.getSteve().texture();
   }

   private void drawHead(CustomDrawContext ctx, float x, float y, float size, float alpha) {
      float scaled = size * this.headSize.getCurrent();
      float offset = (size - scaled) / 2.0F;
      float yaw = this.headYaw.getCurrent();
      float pitch = this.headPitch.getCurrent();
      if (this.headAutoRotate.isEnabled()) {
         yaw += (float)(System.currentTimeMillis() % 4000L) / 4000.0F * 360.0F;
      }
      boolean flat = yaw == 0.0F && pitch == 0.0F;
      LoadedImage customHead = this.loadImage(this.headImage.getValue());
      if (customHead != null && flat) {
         this.drawImageCover(ctx, customHead, x + offset, y + offset, scaled, scaled, 3.0F, alpha);
      } else if (flat) {
         DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), this.skinTexture(), x + offset, y + offset, scaled, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      } else {
         this.drawHead3D(ctx, x + offset, y + offset, scaled, alpha, yaw, pitch);
      }
      if (this.showEyes.isEnabled() && flat) {
         this.drawEyes(ctx, x + offset, y + offset, scaled, alpha);
      }
   }

   private void drawEyes(CustomDrawContext ctx, float x, float y, float size, float alpha) {
      float s = size * this.eyeSize.getCurrent();
      float eyeW = s * 0.22F;
      float eyeH = s * 0.24F;
      long t = System.currentTimeMillis() % 4200L;
      float blink = 1.0F;
      if (t > 3800L) {
         blink = Math.max(0.1F, 1.0F - (float)(t - 3800L) / 400.0F);
      }
      float eH = eyeH * blink;
      float eyeY = y + size * 0.24F + (eyeH - eH) / 2.0F;
      ColorRGBA white = this.eyeColor.getColor().withAlpha(255.0F * alpha);
      ColorRGBA pupil = this.pupilColor.getColor().withAlpha(255.0F * alpha);
      this.drawSingleEye(ctx, x + size * 0.16F, eyeY, eyeW, eH, white, pupil, alpha);
      this.drawSingleEye(ctx, x + size * 0.62F, eyeY, eyeW, eH, white, pupil, alpha);
   }

   private void drawSingleEye(CustomDrawContext ctx, float x, float y, float w, float h, ColorRGBA white, ColorRGBA pupil, float alpha) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, BorderRadius.all(h / 2.0F), white);
      float pupilW = w * 0.5F;
      float pupilH = h * 0.6F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + w / 2.0F - pupilW / 2.0F, y + h / 2.0F - pupilH / 2.0F, pupilW, pupilH, BorderRadius.all(pupilH / 2.0F), pupil);
      float highlight = pupilW * 0.4F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + w / 2.0F + pupilW * 0.15F, y + h / 2.0F - pupilH * 0.35F, highlight, highlight, BorderRadius.all(highlight / 2.0F), ColorRGBA.WHITE.withAlpha(200.0F * alpha));
   }

   private LoadedImage loadImage(String path) {
      if (path == null || path.isEmpty()) {
         return null;
      }
      LoadedImage cached = this.imageCache.get(path);
      if (cached != null) {
         return cached;
      }
      try {
         NativeImage image = NativeImage.read(Files.newInputStream(Path.of(path)));
         int width = image.getWidth();
         int height = image.getHeight();
         NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
         Identifier id = Identifier.of("huihui", "targethud_" + Integer.toHexString(path.hashCode()));
         mc.getTextureManager().registerTexture(id, texture);
         texture.upload();
         LoadedImage loaded = new LoadedImage(id, width, height);
         this.imageCache.put(path, loaded);
         this.failedImages.remove(path);
         return loaded;
      } catch (Exception var9) {
         if (this.failedImages.add(path)) {
            var9.printStackTrace();
         }
         return null;
      }
   }

   public void clearImageCache() {
      this.imageCache.clear();
      this.failedImages.clear();
   }

   public boolean hasImageError(String path) {
      return path != null && !path.isEmpty() && this.failedImages.contains(path);
   }

   private void drawImageCover(CustomDrawContext ctx, LoadedImage image, float x, float y, float width, float height, float radius, float alpha) {
      float imageAspect = (float)image.width / (float)image.height;
      float boxAspect = width / height;
      float u1 = 0.0F;
      float u2 = 1.0F;
      float v1 = 0.0F;
      float v2 = 1.0F;
      if (imageAspect > boxAspect) {
         float crop = 1.0F - boxAspect / imageAspect;
         u1 = crop / 2.0F;
         u2 = 1.0F - crop / 2.0F;
      } else {
         float crop = 1.0F - imageAspect / boxAspect;
         v1 = crop / 2.0F;
         v2 = 1.0F - crop / 2.0F;
      }
      DrawUtil.drawRoundedTextureWithUV(ctx.getMatrices(), image.id, x, y, width, height, BorderRadius.all(radius), ColorRGBA.WHITE.withAlpha(255.0F * alpha), u1, v1, u2, v2);
   }

   private void drawHead3D(CustomDrawContext ctx, float x, float y, float size, float alpha, float yawDeg, float pitchDeg) {
      float rad = 0.017453292F;
      float cosY = MathHelper.cos(yawDeg * rad);
      float sinY = MathHelper.sin(yawDeg * rad);
      float cosX = MathHelper.cos(pitchDeg * rad);
      float sinX = MathHelper.sin(pitchDeg * rad);
      float centerX = x + size / 2.0F;
      float centerY = y + size / 2.0F;
      float maxDist = 0.0F;
      for(int i = 0; i < CUBE_CORNERS.length; ++i) {
         float[] corner = CUBE_CORNERS[i];
         float[] p = this.projectHead(corner[0] * 1.125F, corner[1] * 1.125F, corner[2] * 1.125F, cosY, sinY, cosX, sinX, 1.0F);
         maxDist = Math.max(maxDist, Math.max(Math.abs(p[0]), Math.abs(p[1])));
      }
      float scale = maxDist <= 0.0F ? size / 2.0F : (size / 2.0F - 1.0F) / maxDist;
      List<HeadFace> faces = new ArrayList();
      for(int i = 0; i < HEAD_FACES.length; ++i) {
         faces.add(HEAD_FACES[i]);
      }
      for(int i = 0; i < HAT_FACES.length; ++i) {
         faces.add(HAT_FACES[i]);
      }
      faces.sort((a, b) -> {
         return Float.compare(this.faceZ(a, cosY, sinY, cosX, sinX), this.faceZ(b, cosY, sinY, cosX, sinX));
      });
      ColorRGBA color = ColorRGBA.WHITE.withAlpha(255.0F * alpha);
      for(int i = 0; i < faces.size(); ++i) {
         HeadFace face = (HeadFace)faces.get(i);
         float[][] pts = new float[4][2];
         for(int j = 0; j < 4; ++j) {
            float[] p = this.projectHead(face.corners[j][0] * face.scale, face.corners[j][1] * face.scale, face.corners[j][2] * face.scale, cosY, sinY, cosX, sinX, scale);
            pts[j][0] = centerX + p[0];
            pts[j][1] = centerY + p[1];
         }
         DrawUtil.drawTexturedQuad(ctx.getMatrices(), this.skinTexture(), pts[0][0], pts[0][1], pts[1][0], pts[1][1], pts[2][0], pts[2][1], pts[3][0], pts[3][1], face.u1, face.v1, face.u2, face.v2, color);
      }
   }

   private float faceZ(HeadFace face, float cosY, float sinY, float cosX, float sinX) {
      float z = 0.0F;
      for(int i = 0; i < 4; ++i) {
         float mx = face.corners[i][0] * face.scale;
         float my = face.corners[i][1] * face.scale;
         float mz = face.corners[i][2] * face.scale;
         float x1 = mx * cosY + mz * sinY;
         float z1 = -mx * sinY + mz * cosY;
         z += my * sinX + z1 * cosX;
      }
      return z / 4.0F;
   }

   private float[] projectHead(float mx, float my, float mz, float cosY, float sinY, float cosX, float sinX, float scale) {
      float x1 = mx * cosY + mz * sinY;
      float z1 = -mx * sinY + mz * cosY;
      float y2 = my * cosX - z1 * sinX;
      float z2 = my * sinX + z1 * cosX;
      float persp = 4.0F / (4.0F - z2);
      return new float[]{x1 * persp * scale, y2 * persp * scale};
   }

   private void drawArmor(CustomDrawContext ctx, float posX, float posY, float boxSize, float alpha) {
      if (this.target instanceof PlayerEntity player) {
         List<ItemStack> armor = player.getInventory().armor;
         ItemStack[] items = new ItemStack[]{player.getMainHandStack(), player.getOffHandStack(), armor.get(3), armor.get(2), armor.get(1), armor.get(0)};
         float iconX = posX;
         for(ItemStack stack : items) {
            if (!stack.isEmpty()) {
               ctx.getMatrices().push();
               ctx.getMatrices().translate((double)iconX + (double)((boxSize - 9.6F) / 2.0F), (double)posY + (double)((boxSize - 9.6F) / 2.0F), 0.0D);
               ctx.getMatrices().scale(0.6F * alpha, 0.6F * alpha, 0.6F * alpha);
               ctx.drawItem(stack, 0, 0);
               ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
               ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
               ctx.getMatrices().pop();
               iconX += boxSize;
            }
         }
      }
   }

   private void renderLarge(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 132.0F;
      float height = 48.0F + this.secondBarExtra(4.0F);
      this.drawBackground(ctx, x, y, width, height, alpha);
      this.drawHead(ctx, x + 5.0F, y + 5.0F, 32.0F, alpha);
      MsdfRenderer.renderText(Fonts.ROUND_BOLD, this.name(), 8.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 43.0F, y + 6.0F, 0.0F, true, 0.7F, 1.0F, 82.0F);
      String big = this.bigText();
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), big, x + 43.0F, y + 18.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), this.secondaryText(), x + 43.0F + Fonts.REGULAR.getWidth(big, 6.5F) + 3.0F, y + 18.0F, this.textColor(alpha));
      this.drawBar(ctx, x + 43.0F, y + 28.0F, 84.0F, 4.0F, this.barRadius.getCurrent(), alpha);
      if (this.showArmor.isEnabled()) {
         this.drawArmor(ctx, x + 4.0F, y + 37.0F + this.secondBarExtra(4.0F), 10.0F, alpha);
      }
   }

   private void renderSmall(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 84.0F;
      float height = 20.0F + this.secondBarExtra(2.0F);
      this.drawBackground(ctx, x, y, width, height, alpha);
      this.drawHead(ctx, x + 3.0F, y + 3.0F, 14.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 6.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 20.0F, y + 3.0F, 0.0F, true, 0.7F, 1.0F, 58.0F);
      ColorRGBA percentColor = this.hpColors(this.theme(), 1.0F)[0];
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), this.bigText(), x + 20.0F, y + 10.5F, percentColor.withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 20.0F, y + 17.0F, 60.0F, 2.0F, this.barRadius.getCurrent(), alpha);
   }

   private void renderPercent(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 112.0F;
      float height = 42.0F + this.secondBarExtra(4.0F);
      this.drawBackground(ctx, x, y, width, height, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.0F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 56.0F - Fonts.REGULAR.getWidth(this.name(), 7.0F) / 2.0F, y + 4.0F, 0.0F, true, 0.7F, 1.0F, 106.0F);
      ColorRGBA[] colors = this.hpColors(this.theme(), alpha);
      String big = this.bigText();
      MsdfRenderer.renderText(Fonts.ROUND_BOLD, big, 15.0F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 56.0F - Fonts.ROUND_BOLD.getWidth(big, 15.0F) / 2.0F, y + 10.0F, 0.0F, true, 0.7F, 1.0F, 106.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), this.secondaryText(), x + 56.0F - Fonts.REGULAR.getWidth(this.secondaryText(), 6.0F) / 2.0F, y + 31.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 6.0F, y + 36.0F, width - 12.0F, 4.0F, this.barRadius.getCurrent(), alpha);
   }

   private void renderBigBar(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 124.0F;
      float height = 34.0F + this.secondBarExtra(10.0F);
      this.drawBackground(ctx, x, y, width, height, alpha);
      MsdfRenderer.renderText(Fonts.SEMIBOLD, this.name(), 7.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 6.0F, y + 3.0F, 0.0F, true, 0.7F, 1.0F, 112.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), this.infoText(), x + 6.0F, y + 11.5F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 6.0F, y + 20.0F, width - 12.0F, 10.0F, this.barRadius.getCurrent(), alpha);
      String big = this.bigText();
      MsdfRenderer.renderText(Fonts.BOLD, big, 6.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 6.0F + (width - 12.0F) / 2.0F - Fonts.BOLD.getWidth(big, 6.5F) / 2.0F, y + 21.8F, 0.0F);
   }

   private void renderVertical(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 74.0F;
      float height = 58.0F;
      this.drawBackground(ctx, x, y, width, height, alpha);
      float barHeight = 50.0F * this.barThickness.getCurrent();
      Theme theme = this.theme();
      float headX = x + 17.0F;
      if (this.showSecondBar.isEnabled()) {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F, 8.0F, barHeight, BorderRadius.all(this.barRadius.getCurrent()), this.trackColor(alpha));
         float outdatedH = MathHelper.clamp(barHeight * this.outdatedHealthAnimation.getValue(), 0.0F, barHeight);
         ColorRGBA[] outdated = this.outdatedColors(alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F + barHeight - outdatedH, 8.0F, outdatedH, BorderRadius.all(this.barRadius.getCurrent()), outdated[0], outdated[1], outdated[2], outdated[3]);
         float mainH = MathHelper.clamp(barHeight * this.healthAnimation.getValue(), 0.0F, barHeight);
         ColorRGBA[] main = this.hpColors(theme, alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F + barHeight - mainH, 8.0F, mainH, BorderRadius.all(this.barRadius.getCurrent()), main[0], main[1], main[2], main[3]);
         float goldH = this.goldBarHeight(50.0F);
         this.drawGoldTrack(ctx, x + 14.0F, y + 4.0F, 8.0F, goldH, this.barRadius.getCurrent(), alpha);
         float absH = MathHelper.clamp(goldH * this.gappleAnimation.getValue(), 0.0F, goldH);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 14.0F, y + 4.0F + goldH - absH, 8.0F, absH, BorderRadius.all(this.barRadius.getCurrent()), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
         headX = x + 27.0F;
      } else {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F, 8.0F, barHeight, BorderRadius.all(this.barRadius.getCurrent()), this.trackColor(alpha));
         float outdatedH = MathHelper.clamp(barHeight * this.outdatedHealthAnimation.getValue(), 0.0F, barHeight);
         ColorRGBA[] outdated = this.outdatedColors(alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F + barHeight - outdatedH, 8.0F, outdatedH, BorderRadius.all(this.barRadius.getCurrent()), outdated[0], outdated[1], outdated[2], outdated[3]);
         if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
            float mainH = MathHelper.clamp(barHeight * this.healthAnimation.getValue(), 0.0F, barHeight);
            ColorRGBA[] main = this.hpColors(theme, alpha);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F + barHeight - mainH, 8.0F, mainH, BorderRadius.all(this.barRadius.getCurrent()), main[0], main[1], main[2], main[3]);
         }
         float absH = MathHelper.clamp(barHeight * this.gappleAnimation.getValue(), 0.0F, barHeight);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F + barHeight - absH, 8.0F, absH, BorderRadius.all(this.barRadius.getCurrent()), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
      }
      this.drawHead(ctx, headX, y + 4.0F, 22.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.0F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), headX, y + 29.0F, 0.0F, true, 0.7F, 1.0F, 52.0F);
      ColorRGBA[] colors = this.hpColors(theme, alpha);
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), this.bigText(), headX, y + 39.0F, colors[0]);
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), this.secondaryText(), headX, y + 48.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
   }

   private void renderMetanoise(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 86.0F;
      float height = 30.0F + this.secondBarExtra(3.25F);
      ColorRGBA metanoiseBg = this.customColorsEnabled() ? this.bgColor.getColor().withAlpha(140.0F * alpha) : (new ColorRGBA(0, 0, 0)).withAlpha(140.0F * alpha);
      ColorRGBA metanoiseTint = this.customColorsEnabled() ? this.barColor.getColor().withAlpha(255.0F * alpha) : this.theme().getColor().withAlpha(255.0F * alpha);
      StencilUtil.push();
      DrawUtil.drawMetanoise(ctx.getMatrices(), x, y, width, height, this.toggleAnimationMetanoise.getValue(), 3.0F, metanoiseBg, metanoiseTint);
      StencilUtil.read(1);
      DrawUtil.drawBlur(ctx.getMatrices(), x, y, width, height, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, 255.0F * alpha));
      DrawUtil.drawMetanoise(ctx.getMatrices(), x, y, width, height, this.toggleAnimationMetanoise.getValue(), 3.0F, metanoiseBg, metanoiseTint);
      this.drawHead(ctx, x + 4.0F, y + 4.0F, 22.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.25F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 29.0F, y + 5.5F, 0.0F, true, 0.7F, 1.0F, 56.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), this.infoText(), x + 29.75F, y + 14.25F, this.textColor(alpha));
      this.drawBar(ctx, x + 29.0F, y + 22.0F, width - 33.0F, 3.25F, this.barRadius.getCurrent(), alpha);
      StencilUtil.pop();
      if (this.showArmor.isEnabled()) {
         this.drawArmor(ctx, x + 3.0F, y - 12.0F, 10.0F, alpha);
      }
   }

   private void renderMini(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 64.0F;
      float height = 16.0F + this.secondBarExtra(2.0F);
      this.drawBackground(ctx, x, y, width, height, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 6.0F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 4.0F, y + 1.5F, 0.0F, true, 0.7F, 1.0F, 38.0F);
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), this.bigText(), x + 44.0F, y + 2.0F, this.hpColors(this.theme(), alpha)[0]);
      this.drawBar(ctx, x + 4.0F, y + 12.0F, 56.0F, 2.0F, this.barRadius.getCurrent(), alpha);
   }

   private void renderGradient(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 112.0F;
      float height = 38.0F + this.secondBarExtra(5.0F);
      LoadedImage bg = this.loadImage(this.bgImage.getValue());
      if (bg != null) {
         this.drawImageCover(ctx, bg, x, y, width, height, this.radius.getCurrent(), alpha);
      }
      if (this.customColorsEnabled()) {
         ColorRGBA a = this.barColor.getColor();
         ColorRGBA b = this.barColorSecond.getColor();
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(this.radius.getCurrent()), a.withAlpha(40.0F * alpha), b.withAlpha(10.0F * alpha), b.withAlpha(10.0F * alpha), a.withAlpha(40.0F * alpha));
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, this.borderThickness.getCurrent(), BorderRadius.all(this.radius.getCurrent()), this.borderColor(alpha));
      } else {
         Theme theme = this.theme();
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(this.radius.getCurrent()), theme.getColor().withAlpha(40.0F * alpha), theme.getColor().withAlpha(10.0F * alpha), theme.getSecondColor().withAlpha(10.0F * alpha), theme.getSecondColor().withAlpha(40.0F * alpha));
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, this.borderThickness.getCurrent(), BorderRadius.all(this.radius.getCurrent()), theme.getSecondColor().withAlpha(255.0F * alpha));
      }
      this.drawHead(ctx, x + 6.0F, y + 7.0F, 24.0F, alpha);
      MsdfRenderer.renderText(Fonts.SEMIBOLD, this.name(), 7.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 36.0F, y + 6.0F, 0.0F, true, 0.7F, 1.0F, 70.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), this.infoText(), x + 36.0F, y + 16.0F, this.textColor(alpha));
      float barH = 5.0F * this.barThickness.getCurrent();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 36.0F, y + 27.0F, 70.0F, barH, BorderRadius.all(this.barRadius.getCurrent()), this.trackColor(alpha));
      float mainW = MathHelper.clamp(70.0F * this.healthAnimation.getValue(), 0.0F, 70.0F);
      ColorRGBA[] mainBar = this.barColors(alpha);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 36.0F, y + 27.0F, mainW, barH, BorderRadius.all(this.barRadius.getCurrent()), mainBar[0], mainBar[1], mainBar[2], mainBar[3]);
      if (this.showSecondBar.isEnabled()) {
         float goldH = this.goldBarHeight(5.0F);
         float goldY = y + 27.0F + barH + 2.0F;
         this.drawGoldTrack(ctx, x + 36.0F, goldY, 70.0F, goldH, this.barRadius.getCurrent(), alpha);
         this.drawGoldFill(ctx, x + 36.0F, goldY, 70.0F, goldH, this.barRadius.getCurrent(), alpha);
      } else {
         float absW = MathHelper.clamp(70.0F * this.gappleAnimation.getValue(), 0.0F, 70.0F);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 36.0F, y + 27.0F, absW, barH, BorderRadius.all(this.barRadius.getCurrent()), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
      }
   }

   private void renderBossbar(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 180.0F;
      float height = 34.0F + this.secondBarExtra(8.0F);
      this.drawBackground(ctx, x, y, width, height, alpha);
      MsdfRenderer.renderText(Fonts.SEMIBOLD, this.name(), 7.0F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 6.0F, y + 3.0F, 0.0F, true, 0.7F, 1.0F, 168.0F);
      float barH = 8.0F * this.barThickness.getCurrent();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 13.0F, width - 8.0F, barH, BorderRadius.all(this.barRadius.getCurrent()), this.trackColor(alpha));
      float mainW = MathHelper.clamp((width - 8.0F) * this.healthAnimation.getValue(), 0.0F, width - 8.0F);
      ColorRGBA[] main = this.hpColors(this.theme(), alpha);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 13.0F, mainW, barH, BorderRadius.all(this.barRadius.getCurrent()), main[0], main[1], main[2], main[3]);
      if (this.showSecondBar.isEnabled()) {
         float goldH = this.goldBarHeight(8.0F);
         float goldY = y + 13.0F + barH + 2.0F;
         this.drawGoldTrack(ctx, x + 4.0F, goldY, width - 8.0F, goldH, this.barRadius.getCurrent(), alpha);
         this.drawGoldFill(ctx, x + 4.0F, goldY, width - 8.0F, goldH, this.barRadius.getCurrent(), alpha);
      } else {
         float absW = MathHelper.clamp((width - 8.0F) * this.gappleAnimation.getValue(), 0.0F, width - 8.0F);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 13.0F, absW, barH, BorderRadius.all(this.barRadius.getCurrent()), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
      }
      String big = this.bigText();
      MsdfRenderer.renderText(Fonts.BOLD, big, 6.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 90.0F - Fonts.BOLD.getWidth(big, 6.5F) / 2.0F, y + 14.4F, 0.0F);
      String hpInfo = this.displayMode.is("Проценты") ? this.percentText() : "HP: " + this.hpText() + " / " + Math.round(this.target.getMaxHealth());
      float infoY = y + 25.0F;
      if (this.showSecondBar.isEnabled()) {
         infoY = y + 13.0F + barH + 2.0F + this.goldBarHeight(8.0F) + 2.0F;
      }
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), hpInfo, x + 6.0F, infoY, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
   }

   private void renderMinimal(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 110.0F;
      float height = 22.0F;
      Theme theme = this.theme();
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.0F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x, y, 0.0F, true, 0.7F, 1.0F, 110.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), " | " + this.infoText(), x + 5.0F + Math.min(Fonts.REGULAR.getWidth(this.name(), 7.0F), 110.0F), y + 2.0F, this.hpColors(theme, alpha)[0]);
      float mainW = MathHelper.clamp(width * this.healthAnimation.getValue(), 0.0F, width);
      ColorRGBA[] main = this.hpColors(theme, alpha);
      float barH = 1.5F * this.barThickness.getCurrent();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y + 17.0F, mainW, barH, BorderRadius.all(this.barRadius.getCurrent()), main[0], main[1], main[2], main[3]);
      ColorRGBA emptyBar = this.customColorsEnabled() ? this.bgColor.getColor().withAlpha(120.0F * alpha) : (new ColorRGBA(255, 255, 255)).withAlpha(40.0F * alpha);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + mainW, y + 17.0F, width - mainW, barH, BorderRadius.all(this.barRadius.getCurrent()), emptyBar, emptyBar, emptyBar, emptyBar);
      if (this.showSecondBar.isEnabled()) {
         float goldH = this.goldBarHeight(1.5F);
         float goldY = y + 17.0F + barH + 2.0F;
         this.drawGoldTrack(ctx, x, goldY, width, goldH, this.barRadius.getCurrent(), alpha);
         this.drawGoldFill(ctx, x, goldY, width, goldH, this.barRadius.getCurrent(), alpha);
      }
   }

   private void renderCircle(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 132.0F;
      float height = 48.0F + this.secondBarExtra(5.0F);
      this.drawBackground(ctx, x, y, width, height, alpha);
      this.drawHead(ctx, x + 5.0F, y + 5.0F, 32.0F, alpha);
      MsdfRenderer.renderText(Fonts.ROUND_BOLD, this.name(), 8.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 43.0F, y + 6.0F, 0.0F, true, 0.7F, 1.0F, 82.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), this.secondaryText(), x + 43.0F, y + 18.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      float centerX = x + 96.0F;
      float centerY = y + 26.0F;
      float outerRadius = 15.0F;
      float thickness = 4.5F * this.barThickness.getCurrent();
      this.drawRing(ctx, centerX, centerY, outerRadius, thickness, -0.25F, 0.75F, this.trackColor(alpha));
      float outdated = MathHelper.clamp(this.outdatedHealthAnimation.getValue(), 0.0F, 1.0F);
      if (outdated > 0.0F) {
         ColorRGBA[] outdatedColors = this.outdatedColors(alpha);
         this.drawRing(ctx, centerX, centerY, outerRadius, thickness, -0.25F, -0.25F + outdated, outdatedColors[0]);
      }
      float hp = MathHelper.clamp(this.healthAnimation.getValue(), 0.0F, 1.0F);
      ColorRGBA[] mainColors = this.hpColors(this.theme(), alpha);
      this.drawRing(ctx, centerX, centerY, outerRadius, thickness, -0.25F, -0.25F + hp, mainColors[0]);
      if (this.showSecondBar.isEnabled()) {
         float goldT = this.goldBarHeight(4.5F);
         float goldRadius = outerRadius + thickness + 2.5F;
         this.drawRing(ctx, centerX, centerY, goldRadius, goldT, -0.25F, 0.75F, (new ColorRGBA(84, 64, 10)).withAlpha(170.0F * alpha));
         float abs = MathHelper.clamp(this.gappleAnimation.getValue(), 0.0F, 1.0F);
         if (abs > 0.0F) {
            this.drawRing(ctx, centerX, centerY, goldRadius, goldT, -0.25F, -0.25F + abs, (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha));
         }
      } else {
         float abs = MathHelper.clamp(this.gappleAnimation.getValue(), 0.0F, 1.0F);
         if (abs > 0.0F) {
            this.drawRing(ctx, centerX, centerY, outerRadius, thickness, -0.25F, -0.25F + abs, (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha));
         }
      }
      String big = this.bigText();
      MsdfRenderer.renderText(Fonts.ROUND_BOLD, big, 8.5F, this.textColor(alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), centerX - Fonts.ROUND_BOLD.getWidth(big, 8.5F) / 2.0F, centerY - 3.5F, 0.0F, true, 0.7F, 1.0F, 24.0F);
      if (this.showArmor.isEnabled()) {
         this.drawArmor(ctx, x + 4.0F, y + 37.0F, 10.0F, alpha);
      }
   }

   private void drawRing(CustomDrawContext ctx, float centerX, float centerY, float outerRadius, float thickness, float from, float to, ColorRGBA color) {
      if (to <= from) {
         return;
      }
      float innerRadius = outerRadius - thickness;
      float start = from * 2.0F * (float)Math.PI;
      float end = to * 2.0F * (float)Math.PI;
      int arcSegments = Math.max(2, (int)(32.0F * (to - from)));
      MatrixStack matrices = ctx.getMatrices();
      matrices.push();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();
      for (int i = 0; i < arcSegments; i++) {
         float a1 = start + (end - start) * (float)i / (float)arcSegments;
         float a2 = start + (end - start) * (float)(i + 1) / (float)arcSegments;
         float c1 = MathHelper.cos(a1);
         float s1 = MathHelper.sin(a1);
         float c2 = MathHelper.cos(a2);
         float s2 = MathHelper.sin(a2);
         builder.vertex(matrix, centerX + innerRadius * c1, centerY + innerRadius * s1, 0.0F).color(color.getRGB());
         builder.vertex(matrix, centerX + outerRadius * c1, centerY + outerRadius * s1, 0.0F).color(color.getRGB());
         builder.vertex(matrix, centerX + outerRadius * c2, centerY + outerRadius * s2, 0.0F).color(color.getRGB());
         builder.vertex(matrix, centerX + innerRadius * c2, centerY + innerRadius * s2, 0.0F).color(color.getRGB());
      }
      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.disableBlend();
      matrices.pop();
   }

   private static final class HeadFace {
      private final float[][] corners;
      private final float u1;
      private final float v1;
      private final float u2;
      private final float v2;
      private final float scale;

      private HeadFace(float[][] corners, float u1, float v1, float u2, float v2, float scale) {
         this.corners = corners;
         this.u1 = u1;
         this.v1 = v1;
         this.u2 = u2;
         this.v2 = v2;
         this.scale = scale;
      }
   }

   private static final class LoadedImage {
      private final Identifier id;
      private final int width;
      private final int height;

      private LoadedImage(Identifier id, int width, int height) {
         this.id = id;
         this.width = width;
         this.height = height;
      }
   }
}
