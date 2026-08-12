package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.input.EventMouse;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.base.font.Font;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.screens.hotbar.HotbarPickerScreen;
import tech.huihui.utility.mixin.accessors.DrawContextAccessor;
import tech.huihui.utility.render.display.StencilUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.GuiUtil;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "CustomHotbar",
   category = Category.RENDER,
   description = "Кастомный хотбар с 20 пресетами"
)
public final class CustomHotbar extends Module {
   private static final String[] STYLES = new String[]{
      "Классика", "Пиксель", "Минимал", "Панель", "Радуга", "Стекло", "Кружки", "Неон", "Металл",
      "Градиент", "Плитка", "Шары", "Стрелка", "Горизонт", "Арка", "Тетрис", "Сетка", "Босс", "Волна", "Плазма"
   };
   private static final String[] ARMOR_POS = new String[]{"Слева", "Снизу", "Справа"};
   private static final String[] STATUS_STYLES = new String[]{
      "Классика", "Полоска", "Пиксель", "Неон", "Металл", "Градиент", "Радуга", "Стекло", "Минимал", "Цифры"
   };
   private static final ColorRGBA GOLD = new ColorRGBA(255, 200, 40);
   private static final int[] PANEL_RADIUS = new int[]{4, 0, 0, 4, 4, 6, 0, 4, 2, 5, 2, 10, 4, 0, 4, 0, 0, 6, 4, 3};
   private static final int[] SLOT_RADIUS = new int[]{3, 0, 0, 2, 3, 5, 8, 2, 1, 3, 1, 10, 2, 0, 8, 0, 0, 4, 3, 2};
   private static final int[] OFFHAND_POS = new int[]{0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0};
   public static final CustomHotbar INSTANCE = new CustomHotbar();

   public final ModeSetting style = new ModeSetting("Стиль", STYLES);
   public final NumberSetting x = new NumberSetting("Позиция X", 4.0F, 0.0F, 1920.0F, 1.0F);
   public final NumberSetting y = new NumberSetting("Позиция Y", 4.0F, 0.0F, 1080.0F, 1.0F);
   public final NumberSetting scale = new NumberSetting("Масштаб", 1.0F, 0.5F, 2.0F, 0.05F);
   public final NumberSetting gap = new NumberSetting("Зазор", 2.0F, 0.0F, 10.0F, 0.5F);
   public final BooleanSetting hideVanilla = new BooleanSetting("Скрыть ванильный", true);
   public final BooleanSetting showNumbers = new BooleanSetting("Номера слотов", true);
   public final BooleanSetting showOffhand = new BooleanSetting("Оффхенд", true);
   public final BooleanSetting showCount = new BooleanSetting("Количество", true);
   public final BooleanSetting showDurability = new BooleanSetting("Прочность", true);
   public final BooleanSetting showCooldown = new BooleanSetting("Перезарядка", true);
   public final BooleanSetting showArmor = new BooleanSetting("Броня", true);
   public final ModeSetting armorPos = new ModeSetting("Позиция брони", this::isArmorVisible, ARMOR_POS);
   public final BooleanSetting showStatus = new BooleanSetting("Сердца и голод", true);
   public final ModeSetting statusStyle = new ModeSetting("Стиль статов", STATUS_STYLES);
   public final NumberSetting statusScale = new NumberSetting("Масштаб статов", 1.0F, 0.5F, 2.0F, 0.05F);
   public final NumberSetting statusOffsetX = new NumberSetting("Сдвиг статов X", 0.0F, -500.0F, 500.0F, 1.0F);
   public final NumberSetting statusOffsetY = new NumberSetting("Сдвиг статов Y", 0.0F, -500.0F, 500.0F, 1.0F);
   public final BooleanSetting showAbsorption = new BooleanSetting("Поглощение", true);
   public final ButtonSetting openPicker = new ButtonSetting("Пикер стилей", HotbarPickerScreen::open);
   public final ColorSetting bgColor = new ColorSetting("Цвет фона", new ColorRGBA(0, 0, 0, 160));
   public final ColorSetting borderColor = new ColorSetting("Цвет рамки", new ColorRGBA(100, 100, 115, 255));
   public final ColorSetting accentColor = new ColorSetting("Акцент", new ColorRGBA(58, 152, 255, 255));
   public final ColorSetting textColor = new ColorSetting("Цвет текста", ColorRGBA.WHITE);

   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;

   private CustomHotbar() {
   }

   private boolean isArmorVisible() {
      return this.showArmor.isEnabled();
   }

   public static String[] getStyles() {
      return STYLES;
   }

   public static String[] getStatusStyles() {
      return STATUS_STYLES;
   }

   public static int styleCount() {
      return STYLES.length;
   }

   public static int statusCount() {
      return STATUS_STYLES.length;
   }

   public static float panelRadiusOf(int index) {
      return (float)PANEL_RADIUS[index];
   }

   public static float slotRadiusOf(int index) {
      return (float)SLOT_RADIUS[index];
   }

   public static boolean offhandLeftOf(int index) {
      return OFFHAND_POS[index] == 0;
   }

   @EventTarget
   @Native
   public void onRender(EventHudRender event) {
      if (mc.world == null || mc.player == null || mc.options.hudHidden) {
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
      this.renderCurrent(event.getContext(), this.x.getCurrent(), this.y.getCurrent());
      if (this.showArmor.isEnabled()) {
         this.renderArmor(event.getContext());
      }
      this.renderStatus(event.getContext());
   }

   private void renderArmor(CustomDrawContext ctx) {
      float size = this.slotSize();
      float g = this.gapSize();
      String pos = this.armorPos.get();
      if (this.hideVanilla.isEnabled()) {
         float x = this.x.getCurrent();
         float y = this.y.getCurrent();
         float[] sizeArr = this.currentSize();
         switch (pos) {
            case "Справа" -> this.renderArmorColumn(ctx, x + sizeArr[0] + g, y);
            case "Снизу" -> this.renderArmorRow(ctx, x, y + sizeArr[1] + g);
            default -> this.renderArmorColumn(ctx, x - size - g, y);
         }
      } else {
         float vx = (float)mc.getWindow().getScaledWidth() / 2.0F - 91.0F;
         float vy = (float)mc.getWindow().getScaledHeight() - 22.0F;
         switch (pos) {
            case "Справа" -> this.renderArmorColumn(ctx, vx + 182.0F + g, vy);
            case "Снизу" -> this.renderArmorRow(ctx, vx, vy + 22.0F + g);
            default -> this.renderArmorColumn(ctx, vx - size - g, vy);
         }
      }
   }

   private void renderArmorColumn(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float g = this.gapSize();
      for (int i = 3; i >= 0; i--) {
         this.renderArmorSlot(ctx, mc.player.getInventory().getArmorStack(i), x, y + (float)(3 - i) * (size + g));
      }
   }

   private void renderArmorRow(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float g = this.gapSize();
      for (int i = 3; i >= 0; i--) {
         this.renderArmorSlot(ctx, mc.player.getInventory().getArmorStack(i), x + (float)(3 - i) * (size + g), y);
      }
   }

   private void renderArmorSlot(CustomDrawContext ctx, ItemStack stack, float x, float y) {
      float size = this.slotSize();
      BorderRadius radius = BorderRadius.all(this.slotRadius());
      if (stack.isEmpty()) {
         ctx.drawRoundedRect(x, y, size, size, radius, this.bg().withAlpha(70));
         return;
      }
      ctx.drawRoundedRect(x, y, size, size, radius, this.bg().withAlpha(200));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, size, size, 1.0F, radius, this.accent().withAlpha(180));
      this.drawItem(ctx, stack, x, y, size);
      if (this.showDurability.isEnabled() && stack.isDamageable() && stack.isDamaged()) {
         Font font = Fonts.MEDIUM.getFont(6.0F);
         String percent = (int)((double)(stack.getMaxDamage() - stack.getDamage()) * 100.0D / (double)stack.getMaxDamage()) + "%";
         ctx.drawText(font, percent, x + size - font.width(percent) - 1.0F, y + 1.0F, this.text().withAlpha(220));
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

   public float[] currentSize() {
      float slot = this.slotSize();
      float g = this.gapSize();
      float pad = this.padding();
      float w = 9.0F * slot + 8.0F * g + 2.0F * pad;
      float h = slot + 2.0F * pad;
      int index = this.currentStyleIndex();
      if (this.showOffhand.isEnabled() && OFFHAND_POS[index] == 0) {
         w += slot + g + pad;
      }
      if (OFFHAND_POS[index] == 1) {
         w += slot + g + pad;
      }
      if (index == 17) {
         h += 4.0F * this.scale.getCurrent();
      }
      return new float[]{w, h};
   }

   public int currentStyleIndex() {
      for (int i = 0; i < STYLES.length; i++) {
         if (this.style.is(STYLES[i])) {
            return i;
         }
      }
      return 0;
   }

   public int currentStatusStyleIndex() {
      for (int i = 0; i < STATUS_STYLES.length; i++) {
         if (this.statusStyle.is(STATUS_STYLES[i])) {
            return i;
         }
      }
      return 0;
   }

   private float slotSize() {
      return 22.0F * this.scale.getCurrent();
   }

   private float gapSize() {
      return this.gap.getCurrent() * this.scale.getCurrent();
   }

   private float padding() {
      return 3.0F * this.scale.getCurrent();
   }

   private float panelRadius() {
      return (float)PANEL_RADIUS[this.currentStyleIndex()] * this.scale.getCurrent();
   }

   private float slotRadius() {
      return (float)SLOT_RADIUS[this.currentStyleIndex()] * this.scale.getCurrent();
   }

   private ColorRGBA bg() {
      return this.bgColor.getColor();
   }

   private ColorRGBA border() {
      return this.borderColor.getColor();
   }

   private ColorRGBA accent() {
      return this.accentColor.getColor();
   }

   private ColorRGBA text() {
      return this.textColor.getColor();
   }

   private void renderCurrent(CustomDrawContext ctx, float x, float y) {
      switch (this.currentStyleIndex()) {
         case 0: this.renderClassic(ctx, x, y); break;
         case 1: this.renderPixel(ctx, x, y); break;
         case 2: this.renderMinimal(ctx, x, y); break;
         case 3: this.renderPanel(ctx, x, y); break;
         case 4: this.renderRainbow(ctx, x, y); break;
         case 5: this.renderGlass(ctx, x, y); break;
         case 6: this.renderCircles(ctx, x, y); break;
         case 7: this.renderNeon(ctx, x, y); break;
         case 8: this.renderMetal(ctx, x, y); break;
         case 9: this.renderGradient(ctx, x, y); break;
         case 10: this.renderTiles(ctx, x, y); break;
         case 11: this.renderBalls(ctx, x, y); break;
         case 12: this.renderArrow(ctx, x, y); break;
         case 13: this.renderHorizon(ctx, x, y); break;
         case 14: this.renderArc(ctx, x, y); break;
         case 15: this.renderTetris(ctx, x, y); break;
         case 16: this.renderGrid(ctx, x, y); break;
         case 17: this.renderBoss(ctx, x, y); break;
         case 18: this.renderWave(ctx, x, y); break;
         default: this.renderPlasma(ctx, x, y); break;
      }
   }

   private float panelWidth() {
      float slot = this.slotSize();
      return 9.0F * slot + 8.0F * this.gapSize() + 2.0F * this.padding();
   }

   private float panelHeight() {
      return this.slotSize() + 2.0F * this.padding();
   }

   private float slotX(float panelX, int index) {
      return panelX + this.padding() + (float)index * (this.slotSize() + this.gapSize());
   }

   private boolean offhandLeft() {
      return OFFHAND_POS[this.currentStyleIndex()] == 0;
   }

   private void renderPanelBase(CustomDrawContext ctx, float x, float y, float width, float height, ColorRGBA color) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(this.panelRadius()), color);
   }

   private void renderSlotBase(CustomDrawContext ctx, int index, float x, float y, ColorRGBA bg, ColorRGBA borderColor, boolean selected) {
      float size = this.slotSize();
      BorderRadius radius = BorderRadius.all(this.slotRadius());
      if (selected) {
         ctx.drawRoundedRect(x - 1.0F, y - 1.0F, size + 2.0F, size + 2.0F, BorderRadius.all(this.slotRadius() + 1.0F), this.accent());
      }
      ctx.drawRoundedRect(x, y, size, size, radius, bg);
      if (borderColor != null) {
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, size, size, selected ? 1.5F : 0.5F, radius, borderColor);
      }
      this.renderSlotContent(ctx, index, x, y);
   }

   private void renderSlotContent(CustomDrawContext ctx, int index, float x, float y) {
      float size = this.slotSize();
      ItemStack stack = (ItemStack)mc.player.getInventory().main.get(index);
      if (this.showNumbers.isEnabled() && stack.isEmpty() && index != mc.player.getInventory().selectedSlot) {
         Font font = Fonts.MEDIUM.getFont(6.0F);
         String number = String.valueOf(index + 1);
         ctx.drawText(font, number, x + 1.5F, y + 1.5F, this.text().withAlpha(220));
      }
      if (!stack.isEmpty()) {
         this.drawItem(ctx, stack, x, y, size);
         if (this.showCount.isEnabled() && stack.getCount() > 1) {
            Font font = Fonts.MEDIUM.getFont(6.0F);
            String count = "x" + stack.getCount();
            float countWidth = font.width(count);
            ctx.drawText(font, count, x + size - countWidth - 1.0F, y + size - font.height() - 2.0F, this.text());
         }
      }
   }

   private void drawItem(CustomDrawContext ctx, ItemStack stack, float x, float y, float size) {
      float itemSize = size * 0.7F;
      float scale = itemSize / 16.0F;
      float pad = (size - itemSize) / 2.0F;
      ctx.pushMatrix();
      ctx.getMatrices().translate((double)(x + pad), (double)(y + pad), 1.0D);
      ctx.getMatrices().scale(scale, scale, scale);
      ctx.drawItem(stack, 0, 0);
      if (this.showDurability.isEnabled()) {
         ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
      }
      if (this.showCooldown.isEnabled()) {
         ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
      }
      ctx.popMatrix();
   }

   private void renderOffhand(CustomDrawContext ctx, float panelX, float y, float panelWidth, boolean left) {
      if (!this.showOffhand.isEnabled()) {
         return;
      }
      ItemStack offHand = mc.player.getOffHandStack();
      float size = this.slotSize();
      float offX = left ? panelX - size - this.gapSize() : panelX + panelWidth + this.gapSize();
      ctx.drawRoundedRect(offX, y, size, size, BorderRadius.all(this.slotRadius()), this.bg().withAlpha(200));
      if (offHand.isEmpty()) {
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), offX, y, size, size, 0.5F, BorderRadius.all(this.slotRadius()), this.border().withAlpha(120));
      } else {
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), offX, y, size, size, 1.0F, BorderRadius.all(this.slotRadius()), this.accent().withAlpha(200));
         this.drawItem(ctx, offHand, offX, y, size);
         if (this.showCount.isEnabled() && offHand.getCount() > 1) {
            Font font = Fonts.MEDIUM.getFont(6.0F);
            String count = "x" + offHand.getCount();
            ctx.drawText(font, count, offX + size - font.width(count) - 1.0F, y + size - font.height() - 2.0F, this.text());
         }
      }
   }

   private void renderClassic(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      DrawUtil.drawBlur(ctx.getMatrices(), panelX - 1.0F, y - 1.0F, width + 2.0F, height + 2.0F, 8.0F, BorderRadius.all(this.panelRadius()), this.accent().withAlpha(60));
      this.renderPanelBase(ctx, panelX, y, width, height, this.bg());
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 0.5F, BorderRadius.all(this.panelRadius()), this.border());
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), selected ? this.accent().withAlpha(80) : ColorRGBA.TRANSPARENT, selected ? this.accent() : this.border().withAlpha(150), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderPixel(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      this.renderPanelBase(ctx, panelX, y, width, height, new ColorRGBA(10, 10, 10, 220));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 1.0F, BorderRadius.all(0.0F), new ColorRGBA(60, 60, 60, 255));
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         ColorRGBA slotBg = selected ? new ColorRGBA(0, 90, 0, 255) : new ColorRGBA(25, 25, 25, 255);
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), slotBg, selected ? ColorRGBA.GREEN : new ColorRGBA(70, 70, 70, 255), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderMinimal(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float pad = this.padding();
      float panelY = y + pad;
      for (int i = 0; i < 9; i++) {
         float slotX = x + pad + (float)i * (size + this.gapSize());
         boolean selected = i == mc.player.getInventory().selectedSlot;
         if (selected) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), slotX - 2.0F, panelY + size + 2.0F, size + 4.0F, 2.0F, BorderRadius.all(1.0F), this.accent());
         }
         this.renderSlotContent(ctx, i, slotX, panelY);
      }
      this.renderOffhand(ctx, x, panelY, this.panelWidth(), true);
   }

   private void renderPanel(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      this.renderPanelBase(ctx, panelX, y, width, height, this.bg().brighter(0.15F));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 1.0F, BorderRadius.all(this.panelRadius()), this.border());
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), new ColorRGBA(255, 255, 255, 25), selected ? this.accent() : this.border().withAlpha(200), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderRainbow(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      float time = (System.currentTimeMillis() % 4000L) / 4000.0F;
      ColorRGBA c1 = ColorRGBA.fromHSB(time, 0.8F, 0.35F);
      ColorRGBA c2 = ColorRGBA.fromHSB(time + 0.25F, 0.8F, 0.35F);
      ColorRGBA c3 = ColorRGBA.fromHSB(time + 0.5F, 0.8F, 0.35F);
      ColorRGBA c4 = ColorRGBA.fromHSB(time + 0.75F, 0.8F, 0.35F);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX, y, width, height, BorderRadius.all(this.panelRadius()), c1, c2, c3, c4);
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 1.0F, BorderRadius.all(this.panelRadius()), new ColorRGBA(255, 255, 255, 120));
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), new ColorRGBA(0, 0, 0, 110), selected ? ColorRGBA.WHITE : new ColorRGBA(255, 255, 255, 80), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderGlass(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      DrawUtil.drawBlur(ctx.getMatrices(), panelX - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, 12.0F, BorderRadius.all(this.panelRadius()), ColorRGBA.WHITE.withAlpha(120));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX, y, width, height, BorderRadius.all(this.panelRadius()), new ColorRGBA(255, 255, 255, 30));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 1.0F, BorderRadius.all(this.panelRadius()), ColorRGBA.WHITE.withAlpha(140));
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         ColorRGBA slotBg = selected ? new ColorRGBA(255, 255, 255, 60) : new ColorRGBA(255, 255, 255, 15);
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), slotBg, ColorRGBA.WHITE.withAlpha(selected ? 220 : 90), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderCircles(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float pad = this.padding();
      float panelY = y + pad;
      for (int i = 0; i < 9; i++) {
         float slotX = x + pad + (float)i * (size + this.gapSize());
         boolean selected = i == mc.player.getInventory().selectedSlot;
         BorderRadius round = BorderRadius.all(size / 2.0F);
         if (selected) {
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), slotX - 2.0F, panelY - 2.0F, size + 4.0F, size + 4.0F, 2.0F, BorderRadius.all(size / 2.0F + 2.0F), this.accent());
         }
         ctx.drawRoundedRect(slotX, panelY, size, size, round, selected ? this.accent().withAlpha(90) : this.bg().withAlpha(160));
         this.renderSlotContent(ctx, i, slotX, panelY);
      }
      this.renderOffhand(ctx, x, panelY, this.panelWidth(), true);
   }

   private void renderNeon(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      this.renderPanelBase(ctx, panelX, y, width, height, this.bg());
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         float slotX = this.slotX(panelX, i);
         float slotY = y + this.padding();
         if (selected) {
            DrawUtil.drawGlow(ctx.getMatrices(), slotX - 4.0F, slotY - 4.0F, this.slotSize() + 8.0F, this.slotSize() + 8.0F, 12);
         }
         this.renderSlotBase(ctx, i, slotX, slotY, selected ? this.accent().withAlpha(50) : ColorRGBA.TRANSPARENT, selected ? this.accent() : this.border().withAlpha(120), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderMetal(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      ColorRGBA top = new ColorRGBA(110, 112, 118, 255);
      ColorRGBA mid = new ColorRGBA(52, 54, 60, 255);
      ColorRGBA bottom = new ColorRGBA(24, 25, 30, 255);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX, y, width, height, BorderRadius.all(this.panelRadius()), top, mid, mid, bottom);
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 1.0F, BorderRadius.all(this.panelRadius()), new ColorRGBA(160, 165, 175, 255));
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         ColorRGBA slotBg = selected ? new ColorRGBA(30, 31, 36, 220) : new ColorRGBA(18, 19, 24, 180);
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), slotBg, selected ? new ColorRGBA(255, 209, 0, 255) : new ColorRGBA(90, 92, 100, 255), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderGradient(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      ColorRGBA a = this.accent();
      ColorRGBA b = this.border();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX, y, width, height, BorderRadius.all(this.panelRadius()), a.withAlpha(80), b.withAlpha(40), b.withAlpha(40), a.withAlpha(80));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 1.0F, BorderRadius.all(this.panelRadius()), this.border());
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), new ColorRGBA(0, 0, 0, 120), selected ? this.accent() : this.border().withAlpha(150), selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderTiles(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float pad = this.padding();
      float panelY = y + pad;
      for (int i = 0; i < 9; i++) {
         float slotX = x + pad + (float)i * (size + this.gapSize());
         boolean selected = i == mc.player.getInventory().selectedSlot;
         ColorRGBA bg = selected ? this.accent().withAlpha(120) : this.bg().withAlpha(170);
         ctx.drawRoundedRect(slotX, panelY, size, size, BorderRadius.all(this.slotRadius()), bg);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), slotX, panelY, size, size, selected ? 1.5F : 1.0F, BorderRadius.all(this.slotRadius()), selected ? this.accent() : this.border().withAlpha(180));
         this.renderSlotContent(ctx, i, slotX, panelY);
      }
      this.renderOffhand(ctx, x, panelY, this.panelWidth(), true);
   }

   private void renderBalls(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float pad = this.padding();
      float panelY = y + pad;
      for (int i = 0; i < 9; i++) {
         float slotX = x + pad + (float)i * (size + this.gapSize());
         boolean selected = i == mc.player.getInventory().selectedSlot;
         BorderRadius round = BorderRadius.all(size / 2.0F);
         DrawUtil.drawShadow(ctx.getMatrices(), slotX - 1.0F, panelY - 1.0F, size + 2.0F, size + 2.0F, 5.0F, round, new ColorRGBA(0, 0, 0, 140));
         ColorRGBA bg = selected ? this.accent().withAlpha(180) : this.bg().withAlpha(170);
         ctx.drawRoundedRect(slotX, panelY, size, size, round, bg);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), slotX, panelY, size, size, 1.0F, round, selected ? ColorRGBA.WHITE : this.border().withAlpha(140));
         this.renderSlotContent(ctx, i, slotX, panelY);
      }
      this.renderOffhand(ctx, x, panelY, this.panelWidth(), true);
   }

   private void renderArrow(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      this.renderPanelBase(ctx, panelX, y, width, height, this.bg());
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 0.8F, BorderRadius.all(this.panelRadius()), this.border());
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         float slotX = this.slotX(panelX, i);
         float slotY = y + this.padding();
         this.renderSlotBase(ctx, i, slotX, slotY, selected ? this.accent().withAlpha(60) : ColorRGBA.TRANSPARENT, null, selected);
         if (selected) {
            DrawUtil.drawRoundedRect(ctx.getMatrices(), slotX + this.slotSize() / 2.0F - 4.0F, slotY - 5.0F, 8.0F, 5.0F, BorderRadius.all(1.0F), this.accent());
         }
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderHorizon(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight() + 4.0F * this.scale.getCurrent();
      this.renderPanelBase(ctx, panelX, y, width, height, this.bg());
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 0.5F, BorderRadius.all(this.panelRadius()), this.border().withAlpha(160));
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), selected ? this.accent().withAlpha(50) : ColorRGBA.TRANSPARENT, this.border().withAlpha(100), selected);
      }
      float barY = y + height - 3.0F * this.scale.getCurrent();
      float selectedX = this.slotX(panelX, mc.player.getInventory().selectedSlot);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX + this.padding(), barY, width - 2.0F * this.padding(), 1.5F * this.scale.getCurrent(), BorderRadius.all(1.0F), this.border().withAlpha(120));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), selectedX, barY, this.slotSize(), 1.5F * this.scale.getCurrent(), BorderRadius.all(1.0F), this.accent());
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderArc(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float pad = this.padding();
      float amp = size * 0.35F;
      for (int i = 0; i < 9; i++) {
         float slotX = x + pad + (float)i * (size + this.gapSize());
         float offset = MathHelper.sin((float)i / 8.0F * (float)Math.PI) * amp;
         float slotY = y + pad + (amp - offset);
         boolean selected = i == mc.player.getInventory().selectedSlot;
         ctx.drawRoundedRect(slotX, slotY, size, size, BorderRadius.all(this.slotRadius()), selected ? this.accent().withAlpha(80) : this.bg().withAlpha(160));
         if (selected) {
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), slotX, slotY, size, size, 1.5F, BorderRadius.all(this.slotRadius()), this.accent());
         }
         this.renderSlotContent(ctx, i, slotX, slotY);
      }
      this.renderOffhand(ctx, x, y + pad, this.panelWidth(), true);
   }

   private void renderTetris(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float pad = this.padding();
      float panelY = y + pad;
      for (int i = 0; i < 9; i++) {
         float slotX = x + pad + (float)i * (size + this.gapSize());
         boolean selected = i == mc.player.getInventory().selectedSlot;
         ColorRGBA bg;
         if (selected) {
            bg = this.accent().withAlpha(150);
         } else {
            float hue = (float)i / 9.0F;
            bg = ColorRGBA.fromHSB(hue, 0.45F, 0.22F);
         }
         ctx.drawRoundedRect(slotX, panelY, size, size, BorderRadius.all(0.0F), bg);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), slotX, panelY, size, size, 1.0F, BorderRadius.all(0.0F), new ColorRGBA(0, 0, 0, 120));
         this.renderSlotContent(ctx, i, slotX, panelY);
      }
      this.renderOffhand(ctx, x, panelY, this.panelWidth(), true);
   }

   private void renderGrid(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      this.renderPanelBase(ctx, panelX, y, width, height, this.bg().withAlpha(120));
      for (int i = 1; i < 9; i++) {
         float lineX = this.slotX(panelX, i) - this.gapSize() / 2.0F;
         DrawUtil.drawRect(ctx.getMatrices(), lineX, y + this.padding(), 1.0F, this.slotSize(), this.border().withAlpha(100));
      }
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), ColorRGBA.TRANSPARENT, selected ? this.accent() : null, selected);
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderBoss(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight() + 4.0F * this.scale.getCurrent();
      DrawUtil.drawShadow(ctx.getMatrices(), panelX - 2.0F, y - 2.0F, width + 4.0F, height + 4.0F, 8.0F, BorderRadius.all(this.panelRadius()), new ColorRGBA(0, 0, 0, 160));
      this.renderPanelBase(ctx, panelX, y, width, height, this.bg());
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), panelX, y, width, height, 2.0F, BorderRadius.all(this.panelRadius()), this.accent().withAlpha(200));
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), selected ? this.accent().withAlpha(70) : new ColorRGBA(255, 255, 255, 15), selected ? this.accent() : this.border().withAlpha(160), selected);
      }
      ItemStack offHand = mc.player.getOffHandStack();
      if (!offHand.isEmpty() && this.showOffhand.isEnabled()) {
         float barY = y + height - 2.5F * this.scale.getCurrent();
         DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX + this.padding(), barY, width - 2.0F * this.padding(), 2.0F * this.scale.getCurrent(), BorderRadius.all(1.0F), new ColorRGBA(255, 255, 255, 30));
         DrawUtil.drawRoundedRect(ctx.getMatrices(), panelX + this.padding(), barY, (width - 2.0F * this.padding()) * 0.3F, 2.0F * this.scale.getCurrent(), BorderRadius.all(1.0F), this.accent());
      }
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderWave(CustomDrawContext ctx, float x, float y) {
      float size = this.slotSize();
      float pad = this.padding();
      float time = (System.currentTimeMillis() % 2000L) / 2000.0F * (float)Math.PI * 2.0F;
      for (int i = 0; i < 9; i++) {
         float slotX = x + pad + (float)i * (size + this.gapSize());
         float offset = MathHelper.sin(time + (float)i * 0.7F) * 3.0F;
         float slotY = y + pad + offset;
         boolean selected = i == mc.player.getInventory().selectedSlot;
         ctx.drawRoundedRect(slotX, slotY, size, size, BorderRadius.all(this.slotRadius()), selected ? this.accent().withAlpha(110) : this.bg().withAlpha(150));
         if (selected) {
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), slotX, slotY, size, size, 1.5F, BorderRadius.all(this.slotRadius()), this.accent());
         }
         this.renderSlotContent(ctx, i, slotX, slotY);
      }
      this.renderOffhand(ctx, x, y + pad, this.panelWidth(), true);
   }

   private void renderPlasma(CustomDrawContext ctx, float x, float y) {
      float panelX = x;
      if (this.offhandLeft() && this.showOffhand.isEnabled()) {
         panelX += this.slotSize() + this.gapSize();
      }
      float width = this.panelWidth();
      float height = this.panelHeight();
      float time = (System.currentTimeMillis() % 3000L) / 3000.0F;
      StencilUtil.push();
      DrawUtil.drawMetanoise(ctx.getMatrices(), panelX, y, width, height, time, 3.0F, this.bg(), this.accent());
      StencilUtil.read(1);
      DrawUtil.drawBlur(ctx.getMatrices(), panelX, y, width, height, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, 255));
      DrawUtil.drawMetanoise(ctx.getMatrices(), panelX, y, width, height, time, 3.0F, this.bg(), this.accent());
      for (int i = 0; i < 9; i++) {
         boolean selected = i == mc.player.getInventory().selectedSlot;
         this.renderSlotBase(ctx, i, this.slotX(panelX, i), y + this.padding(), selected ? this.accent().withAlpha(70) : new ColorRGBA(0, 0, 0, 60), selected ? this.accent() : null, selected);
      }
      StencilUtil.pop();
      this.renderOffhand(ctx, x, y + this.padding(), width, true);
   }

   private void renderStatus(CustomDrawContext ctx) {
      if (!this.showStatus.isEnabled()) {
         return;
      }
      switch (this.currentStatusStyleIndex()) {
         case 0: this.renderStatusClassic(ctx); break;
         case 1: this.renderStatusBar(ctx); break;
         case 2: this.renderStatusPixel(ctx); break;
         case 3: this.renderStatusNeon(ctx); break;
         case 4: this.renderStatusMetal(ctx); break;
         case 5: this.renderStatusGradient(ctx); break;
         case 6: this.renderStatusRainbow(ctx); break;
         case 7: this.renderStatusGlass(ctx); break;
         case 8: this.renderStatusMinimal(ctx); break;
         default: this.renderStatusDigits(ctx); break;
      }
   }

   private float statusCellSize() {
      return 13.0F * this.statusScale.getCurrent();
   }

   private float statusGapSize() {
      return 2.0F * this.statusScale.getCurrent();
   }

   private float statusPadding() {
      return 3.0F * this.statusScale.getCurrent();
   }

   private float statusPanelWidth() {
      return 10.0F * this.statusCellSize() + 9.0F * this.statusGapSize() + 2.0F * this.statusPadding();
   }

   private float statusPanelHeight() {
      int rows = 2 + (this.statusHasGold() ? 1 : 0);
      return (float)rows * this.statusCellSize() + (float)(rows - 1) * this.statusGapSize() + 2.0F * this.statusPadding();
   }

   private float statusOriginX() {
      float[] hotbarSize = this.currentSize();
      return this.x.getCurrent() + (hotbarSize[0] - this.statusPanelWidth()) / 2.0F + this.statusOffsetX.getCurrent();
   }

   private float statusOriginY() {
      return this.y.getCurrent() - this.statusPanelHeight() - 4.0F * this.scale.getCurrent() + this.statusOffsetY.getCurrent();
   }

   private float statusCellX(float panelX, int index) {
      return panelX + this.statusPadding() + (float)index * (this.statusCellSize() + this.statusGapSize());
   }

   private float statusTotalHealth() {
      return mc.player.getHealth();
   }

   private boolean statusHasGold() {
      return this.statusAbsorptionCells() > 0;
   }

   private int statusAbsorptionCells() {
      if (!this.showAbsorption.isEnabled()) {
         return 0;
      }
      return MathHelper.ceil(Math.max(mc.player.getAbsorptionAmount(), 0.0F) / 2.0F);
   }

   private float statusGoldRowY(float panelY) {
      return panelY + this.statusPadding();
   }

   private float statusHealthRowY(float panelY) {
      return panelY + this.statusPadding() + (this.statusHasGold() ? this.statusCellSize() + this.statusGapSize() : 0.0F);
   }

   private float statusFoodRowY(float panelY) {
      return this.statusHealthRowY(panelY) + this.statusCellSize() + this.statusGapSize();
   }

   private void renderStatusGoldRow(CustomDrawContext ctx, float panelX, float rowY, ColorRGBA empty, BorderRadius radius, ColorRGBA borderColor, boolean glow) {
      int cells = this.statusAbsorptionCells();
      for (int i = 0; i < cells; i++) {
         float cx = this.statusCellX(panelX, i);
         float cell = this.statusCellSize();
         ctx.drawRoundedRect(cx, rowY, cell, cell, radius, empty);
         if (borderColor != null) {
            DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx, rowY, cell, cell, 0.5F, radius, borderColor);
         }
         float fill = this.statusCellFill(mc.player.getAbsorptionAmount(), i);
         if (fill > 0.0F && glow) {
            DrawUtil.drawGlow(ctx.getMatrices(), cx - 3.0F, rowY - 3.0F, cell + 6.0F, cell + 6.0F, 10);
         }
         this.drawStatusHeartCell(ctx, cx, rowY, fill, GOLD);
      }
   }

   private float statusCellFill(float value, int index) {
      return MathHelper.clamp(value - (float)index * 2.0F, 0.0F, 2.0F);
   }

   private void drawStatusHeart(CustomDrawContext ctx, float x, float y, float size, ColorRGBA color) {
      float lobeW = size * 0.44F;
      float lobeH = size * 0.48F;
      float cx = x + size / 2.0F;
      float top = y + size * 0.04F;
      BorderRadius round = BorderRadius.all(lobeW * 0.5F);
      ctx.drawRoundedRect(cx - lobeW, top, lobeW, lobeH, round, color);
      ctx.drawRoundedRect(cx, top, lobeW, lobeH, round, color);
      ctx.pushMatrix();
      ctx.getMatrices().translate(cx, y + size * 0.42F, 0.0F);
      ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0F));
      float d = size * 0.5F;
      ctx.drawRoundedRect(-d / 2.0F, -d / 2.0F, d, d, BorderRadius.all(d * 0.22F), color);
      ctx.popMatrix();
   }

   private void drawStatusHeartCell(CustomDrawContext ctx, float x, float y, float fill, ColorRGBA color) {
      float cell = this.statusCellSize();
      if (fill <= 0.0F) {
         return;
      }
      if (fill >= 2.0F) {
         this.drawStatusHeart(ctx, x, y, cell, color);
      } else {
         float visible = cell * (fill / 2.0F);
         int sy = (int)(y + cell - visible);
         ctx.enableScissor((int)x, sy, (int)cell, (int)visible);
         this.drawStatusHeart(ctx, x, y, cell, color);
         ctx.disableScissor();
      }
   }

   private void drawStatusFoodCell(CustomDrawContext ctx, float x, float y, float fill, ColorRGBA color) {
      float cell = this.statusCellSize();
      if (fill <= 0.0F) {
         return;
      }
      ctx.drawRoundedRect(x, y, cell, cell * (fill / 2.0F), BorderRadius.all(cell * 0.22F), color);
   }

   private void renderStatusClassic(CustomDrawContext ctx) {
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float w = this.statusPanelWidth();
      float h = this.statusPanelHeight();
      this.renderPanelBase(ctx, x, y, w, h, this.bg());
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 0.5F, BorderRadius.all(this.panelRadius()), this.border());
      ColorRGBA heartColor = new ColorRGBA(225, 55, 65);
      ColorRGBA foodColor = new ColorRGBA(235, 185, 70);
      ColorRGBA empty = new ColorRGBA(0, 0, 0, 80);
      BorderRadius round = BorderRadius.all(this.statusCellSize() * 0.22F);
      this.renderStatusGoldRow(ctx, x, this.statusGoldRowY(y), empty, round, null, false);
      float rowY = this.statusHealthRowY(y);
      float fy = this.statusFoodRowY(y);
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = this.statusCellX(x, i);
         float fill = this.statusCellFill(total, i);
         ctx.drawRoundedRect(cx, rowY, this.statusCellSize(), this.statusCellSize(), round, empty);
         this.drawStatusHeartCell(ctx, cx, rowY, fill, heartColor);
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         ctx.drawRoundedRect(this.statusCellX(x, i), fy, this.statusCellSize(), this.statusCellSize(), round, empty);
         this.drawStatusFoodCell(ctx, this.statusCellX(x, i), fy, ff, foodColor);
      }
   }

   private void renderStatusBar(CustomDrawContext ctx) {
      float s = this.statusScale.getCurrent();
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float w = this.statusPanelWidth();
      float h = 7.0F * s;
      float gap = 6.0F * s;
      float pad = this.statusPadding();
      this.renderPanelBase(ctx, x, y, w, this.statusPanelHeight(), this.bg());
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, this.statusPanelHeight(), 0.5F, BorderRadius.all(this.panelRadius()), this.border());
      float maxHp = mc.player.getMaxHealth();
      float hpPct = MathHelper.clamp(mc.player.getHealth() / maxHp, 0.0F, 1.0F);
      float foodPct = MathHelper.clamp((float)mc.player.getHungerManager().getFoodLevel() / 20.0F, 0.0F, 1.0F);
      float sat = MathHelper.clamp(mc.player.getHungerManager().getSaturationLevel() / 20.0F, 0.0F, 1.0F);
      Font font = Fonts.MEDIUM.getFont(5.0F * s);
      String hpText = (int)(hpPct * 100.0F) + "%";
      String foodText = (int)(foodPct * 100.0F) + "%";
      String goldText = this.statusHasGold() ? "GA " + (int)((float)this.statusAbsorptionCells() * 10.0F) + "%" : "";
      float textW = Math.max(Math.max(font.width(hpText), font.width(foodText)), font.width(goldText)) + 4.0F * s;
      float barW = w - 2.0F * pad - textW;
      BorderRadius round = BorderRadius.all(h / 2.0F);
      float row1 = y + pad + (this.statusHasGold() ? h + gap : 0.0F);
      float row2 = y + pad + (this.statusHasGold() ? 2.0F * (h + gap) : h + gap);
      if (this.statusHasGold()) {
         float goldFill = (float)this.statusAbsorptionCells() / 10.0F;
         ctx.drawRoundedRect(x + pad, y + pad, barW, h, round, new ColorRGBA(0, 0, 0, 90));
         ctx.drawRoundedRect(x + pad, y + pad, barW * goldFill, h, round, GOLD);
         ctx.drawText(font, goldText, x + pad + barW + 2.0F * s, y + pad - 1.0F, GOLD);
      }
      ColorRGBA hpColor = ColorRGBA.lerp(new ColorRGBA(220, 60, 60), new ColorRGBA(70, 200, 90), hpPct);
      ctx.drawRoundedRect(x + pad, row1, barW, h, round, new ColorRGBA(0, 0, 0, 90));
      ctx.drawRoundedRect(x + pad, row1, barW * hpPct, h, round, hpColor);
      ctx.drawText(font, hpText, x + pad + barW + 2.0F * s, row1 - 1.0F, ColorRGBA.WHITE);
      ctx.drawRoundedRect(x + pad, row2, barW, h, round, new ColorRGBA(0, 0, 0, 90));
      ctx.drawRoundedRect(x + pad, row2, barW * foodPct, h, round, new ColorRGBA(235, 185, 70));
      if (sat > 0.0F) {
         ctx.drawRoundedRect(x + pad, row2, barW * sat, h * 0.35F, BorderRadius.all(1.0F), ColorRGBA.WHITE.withAlpha(160));
      }
      ctx.drawText(font, foodText, x + pad + barW + 2.0F * s, row2 - 1.0F, ColorRGBA.WHITE);
   }

   private void renderStatusPixel(CustomDrawContext ctx) {
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float cell = this.statusCellSize();
      float gap = this.statusGapSize();
      ColorRGBA heartColor = new ColorRGBA(215, 45, 45);
      ColorRGBA foodColor = new ColorRGBA(240, 180, 60);
      ColorRGBA empty = new ColorRGBA(28, 28, 28, 255);
      ColorRGBA borderColor = new ColorRGBA(0, 0, 0, 200);
      this.renderStatusGoldRow(ctx, x, this.statusGoldRowY(y), empty, BorderRadius.ZERO, borderColor, false);
      float rowY = this.statusHealthRowY(y);
      float fy = this.statusFoodRowY(y);
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = this.statusCellX(x, i);
         float fill = this.statusCellFill(total, i);
         ctx.drawRoundedRect(cx, rowY, cell, cell, BorderRadius.ZERO, empty);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx, rowY, cell, cell, 1.0F, BorderRadius.ZERO, borderColor);
         this.drawStatusHeartCell(ctx, cx, rowY, fill, heartColor);
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         ctx.drawRoundedRect(cx, fy, cell, cell, BorderRadius.ZERO, empty);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx, fy, cell, cell, 1.0F, BorderRadius.ZERO, borderColor);
         this.drawStatusFoodCell(ctx, cx, fy, ff, foodColor);
      }
   }

   private void renderStatusNeon(CustomDrawContext ctx) {
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float w = this.statusPanelWidth();
      float h = this.statusPanelHeight();
      this.renderPanelBase(ctx, x, y, w, h, this.bg());
      ColorRGBA heartColor = new ColorRGBA(0, 230, 255);
      ColorRGBA foodColor = new ColorRGBA(255, 90, 220);
      ColorRGBA empty = new ColorRGBA(0, 0, 0, 60);
      BorderRadius round = BorderRadius.all(this.statusCellSize() * 0.22F);
      this.renderStatusGoldRow(ctx, x, this.statusGoldRowY(y), empty, round, null, true);
      float rowY = this.statusHealthRowY(y);
      float fy = this.statusFoodRowY(y);
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = this.statusCellX(x, i);
         float cell = this.statusCellSize();
         float fill = this.statusCellFill(total, i);
         ctx.drawRoundedRect(cx, rowY, cell, cell, round, empty);
         if (fill > 0.0F) {
            DrawUtil.drawGlow(ctx.getMatrices(), cx - 3.0F, rowY - 3.0F, cell + 6.0F, cell + 6.0F, 10);
         }
         this.drawStatusHeartCell(ctx, cx, rowY, fill, heartColor);
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         ctx.drawRoundedRect(cx, fy, cell, cell, round, empty);
         this.drawStatusFoodCell(ctx, cx, fy, ff, foodColor);
      }
   }

   private void renderStatusMetal(CustomDrawContext ctx) {
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float w = this.statusPanelWidth();
      float h = this.statusPanelHeight();
      ColorRGBA top = new ColorRGBA(110, 112, 118, 255);
      ColorRGBA mid = new ColorRGBA(52, 54, 60, 255);
      ColorRGBA bottom = new ColorRGBA(24, 25, 30, 255);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, BorderRadius.all(this.panelRadius()), top, mid, mid, bottom);
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(this.panelRadius()), new ColorRGBA(160, 165, 175, 255));
      ColorRGBA heartColor = new ColorRGBA(230, 70, 60);
      ColorRGBA foodColor = new ColorRGBA(255, 200, 80);
      ColorRGBA empty = new ColorRGBA(18, 19, 24, 220);
      ColorRGBA cellBorder = new ColorRGBA(90, 92, 100, 255);
      BorderRadius round = BorderRadius.all(this.statusCellSize() * 0.18F);
      this.renderStatusGoldRow(ctx, x, this.statusGoldRowY(y), empty, round, cellBorder, false);
      float rowY = this.statusHealthRowY(y);
      float fy = this.statusFoodRowY(y);
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = this.statusCellX(x, i);
         float cell = this.statusCellSize();
         float fill = this.statusCellFill(total, i);
         ctx.drawRoundedRect(cx, rowY, cell, cell, round, empty);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx, rowY, cell, cell, 0.6F, round, cellBorder);
         this.drawStatusHeartCell(ctx, cx, rowY, fill, heartColor);
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         ctx.drawRoundedRect(cx, fy, cell, cell, round, empty);
         DrawUtil.drawRoundedBorder(ctx.getMatrices(), cx, fy, cell, cell, 0.6F, round, cellBorder);
         this.drawStatusFoodCell(ctx, cx, fy, ff, foodColor);
      }
   }

   private void renderStatusGradient(CustomDrawContext ctx) {
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float w = this.statusPanelWidth();
      float h = this.statusPanelHeight();
      ColorRGBA accent = this.accent();
      ColorRGBA borderColor = this.border();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, BorderRadius.all(this.panelRadius()), accent.withAlpha(60), borderColor.withAlpha(30), borderColor.withAlpha(30), accent.withAlpha(60));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(this.panelRadius()), borderColor);
      ColorRGBA empty = new ColorRGBA(0, 0, 0, 120);
      BorderRadius round = BorderRadius.all(this.statusCellSize() * 0.22F);
      this.renderStatusGoldRow(ctx, x, this.statusGoldRowY(y), empty, round, null, false);
      float rowY = this.statusHealthRowY(y);
      float fy = this.statusFoodRowY(y);
      float cell = this.statusCellSize();
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = this.statusCellX(x, i);
         float fill = this.statusCellFill(total, i);
         ctx.drawRoundedRect(cx, rowY, cell, cell, round, empty);
         if (fill > 0.0F) {
            float visible = cell * (fill / 2.0F);
            ctx.enableScissor((int)cx, (int)(rowY + cell - visible), (int)cell, (int)visible);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), cx, rowY, cell, cell, round, accent, borderColor, borderColor, accent);
            ctx.disableScissor();
         }
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         ctx.drawRoundedRect(cx, fy, cell, cell, round, empty);
         if (ff > 0.0F) {
            float visible = cell * (ff / 2.0F);
            ctx.enableScissor((int)cx, (int)(fy + cell - visible), (int)cell, (int)visible);
            DrawUtil.drawRoundedRect(ctx.getMatrices(), cx, fy, cell, cell, round, accent, borderColor, borderColor, accent);
            ctx.disableScissor();
         }
      }
   }

   private void renderStatusRainbow(CustomDrawContext ctx) {
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float w = this.statusPanelWidth();
      float h = this.statusPanelHeight();
      float time = (System.currentTimeMillis() % 4000L) / 4000.0F;
      this.renderPanelBase(ctx, x, y, w, h, this.bg());
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(this.panelRadius()), new ColorRGBA(255, 255, 255, 120));
      ColorRGBA empty = new ColorRGBA(0, 0, 0, 70);
      BorderRadius round = BorderRadius.all(this.statusCellSize() * 0.22F);
      this.renderStatusGoldRow(ctx, x, this.statusGoldRowY(y), empty, round, null, false);
      float rowY = this.statusHealthRowY(y);
      float fy = this.statusFoodRowY(y);
      float cell = this.statusCellSize();
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = this.statusCellX(x, i);
         float fill = this.statusCellFill(total, i);
         ColorRGBA heartColor = ColorRGBA.fromHSB(time + (float)i * 0.08F, 0.85F, 0.9F);
         ctx.drawRoundedRect(cx, rowY, cell, cell, round, empty);
         this.drawStatusHeartCell(ctx, cx, rowY, fill, heartColor);
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         ColorRGBA foodColor = ColorRGBA.fromHSB(time + (float)i * 0.08F + 0.5F, 0.85F, 0.9F);
         ctx.drawRoundedRect(cx, fy, cell, cell, round, empty);
         this.drawStatusFoodCell(ctx, cx, fy, ff, foodColor);
      }
   }

   private void renderStatusGlass(CustomDrawContext ctx) {
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float w = this.statusPanelWidth();
      float h = this.statusPanelHeight();
      DrawUtil.drawBlur(ctx.getMatrices(), x - 2.0F, y - 2.0F, w + 4.0F, h + 4.0F, 12.0F, BorderRadius.all(this.panelRadius()), ColorRGBA.WHITE.withAlpha(120));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, BorderRadius.all(this.panelRadius()), new ColorRGBA(255, 255, 255, 30));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, w, h, 1.0F, BorderRadius.all(this.panelRadius()), ColorRGBA.WHITE.withAlpha(140));
      ColorRGBA empty = new ColorRGBA(255, 255, 255, 15);
      ColorRGBA heartColor = new ColorRGBA(255, 255, 255, 190);
      ColorRGBA foodColor = new ColorRGBA(255, 255, 255, 160);
      BorderRadius round = BorderRadius.all(this.statusCellSize() * 0.22F);
      this.renderStatusGoldRow(ctx, x, this.statusGoldRowY(y), empty, round, null, false);
      float rowY = this.statusHealthRowY(y);
      float fy = this.statusFoodRowY(y);
      float cell = this.statusCellSize();
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = this.statusCellX(x, i);
         float fill = this.statusCellFill(total, i);
         ctx.drawRoundedRect(cx, rowY, cell, cell, round, empty);
         this.drawStatusHeartCell(ctx, cx, rowY, fill, heartColor);
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         ctx.drawRoundedRect(cx, fy, cell, cell, round, empty);
         this.drawStatusFoodCell(ctx, cx, fy, ff, foodColor);
      }
   }

   private void renderStatusMinimal(CustomDrawContext ctx) {
      float s = this.statusScale.getCurrent();
      float x = this.statusOriginX() + this.statusPadding();
      float y = this.statusOriginY();
      float segW = 6.0F * s;
      float segGap = 2.0F * s;
      float segH = 4.0F * s;
      if (this.statusHasGold()) {
         for (int i = 0; i < 10; i++) {
            float cx = x + (float)i * (segW + segGap);
            float gy = y + this.statusPadding();
            ctx.drawRoundedRect(cx, gy, segW, segH, BorderRadius.all(1.0F), new ColorRGBA(30, 32, 38, 220));
            float g = this.statusCellFill(mc.player.getAbsorptionAmount(), i) / 2.0F;
            if (g > 0.0F) {
               ctx.drawRoundedRect(cx, gy, segW * g, segH, BorderRadius.all(1.0F), GOLD);
            }
         }
      }
      float segY = y + this.statusPadding() + (this.statusHasGold() ? segH + 4.0F * s : 0.0F);
      float total = this.statusTotalHealth();
      for (int i = 0; i < 10; i++) {
         float cx = x + (float)i * (segW + segGap);
         float fill = this.statusCellFill(total, i) / 2.0F;
         ColorRGBA bg = new ColorRGBA(30, 32, 38, 220);
         ctx.drawRoundedRect(cx, segY, segW, segH, BorderRadius.all(1.0F), bg);
         if (fill > 0.0F) {
            ColorRGBA color = ColorRGBA.lerp(new ColorRGBA(220, 60, 60), new ColorRGBA(70, 200, 90), (float)i / 9.0F);
            ctx.drawRoundedRect(cx, segY, segW * fill, segH, BorderRadius.all(1.0F), color);
         }
         float fy = segY + segH + 5.0F * s;
         float dot = 4.0F * s;
         float ff = this.statusCellFill((float)mc.player.getHungerManager().getFoodLevel(), i);
         BorderRadius round = BorderRadius.all(dot * 0.5F);
         ctx.drawRoundedRect(cx, fy, dot, dot, round, new ColorRGBA(30, 32, 38, 220));
         if (ff >= 2.0F) {
            ctx.drawRoundedRect(cx, fy, dot, dot, round, new ColorRGBA(235, 185, 70));
         } else if (ff >= 1.0F) {
            ctx.drawRoundedRect(cx, fy, dot, dot / 2.0F, BorderRadius.all(dot * 0.25F), new ColorRGBA(235, 185, 70));
         }
      }
   }

   private void renderStatusDigits(CustomDrawContext ctx) {
      float s = this.statusScale.getCurrent();
      float x = this.statusOriginX();
      float y = this.statusOriginY();
      float rowH = 7.0F * s + 3.0F * s;
      Font big = Fonts.BOLD.getFont(7.0F * s);
      Font labelFont = Fonts.MEDIUM.getFont(5.0F * s);
      float maxHp = mc.player.getMaxHealth();
      float hpPct = MathHelper.clamp(mc.player.getHealth() / maxHp, 0.0F, 1.0F);
      float foodPct = MathHelper.clamp((float)mc.player.getHungerManager().getFoodLevel() / 20.0F, 0.0F, 1.0F);
      ColorRGBA hpColor = ColorRGBA.lerp(new ColorRGBA(220, 60, 60), new ColorRGBA(70, 200, 90), hpPct);
      String hpText = (int)(hpPct * 100.0F) + "%";
      String foodText = (int)(foodPct * 100.0F) + "%";
      String goldText = this.statusHasGold() ? "AB " + (int)((float)this.statusAbsorptionCells() * 10.0F) + "%" : "";
      ColorRGBA goldColor = GOLD;
      float textW = Math.max(Math.max(big.width(hpText), big.width(foodText)), big.width(goldText)) + 4.0F * s;
      float barW = this.statusPanelWidth() - 2.0F * this.statusPadding() - textW;
      float goldOffset = 0.0F;
      if (this.statusHasGold()) {
         float goldPct = MathHelper.clamp(mc.player.getAbsorptionAmount() / maxHp, 0.0F, 1.0F);
         ctx.drawText(labelFont, "AB", x, y, goldColor);
         ctx.drawText(big, goldText, x + barW + 2.0F * s, y - 1.5F * s, goldColor);
         ctx.drawRoundedRect(x, y + rowH, barW, 2.5F * s, BorderRadius.all(1.0F), new ColorRGBA(30, 32, 38, 200));
         ctx.drawRoundedRect(x, y + rowH, barW * goldPct, 2.5F * s, BorderRadius.all(1.0F), goldColor);
         goldOffset = rowH + 5.0F * s;
      }
      float hpRowY = y + goldOffset;
      float foodRowY = hpRowY + rowH + 5.0F * s;
      ctx.drawText(labelFont, "HP", x, hpRowY, hpColor);
      ctx.drawText(big, hpText, x + barW + 2.0F * s, hpRowY - 1.5F * s, hpColor);
      ctx.drawRoundedRect(x, hpRowY + rowH, barW, 2.5F * s, BorderRadius.all(1.0F), new ColorRGBA(30, 32, 38, 200));
      ctx.drawRoundedRect(x, hpRowY + rowH, barW * hpPct, 2.5F * s, BorderRadius.all(1.0F), hpColor);
      ctx.drawText(labelFont, "FO", x, foodRowY, new ColorRGBA(235, 185, 70));
      ctx.drawText(big, foodText, x + barW + 2.0F * s, foodRowY - 1.5F * s, new ColorRGBA(235, 185, 70));
      ctx.drawRoundedRect(x, foodRowY + rowH, barW, 2.5F * s, BorderRadius.all(1.0F), new ColorRGBA(30, 32, 38, 200));
      ctx.drawRoundedRect(x, foodRowY + rowH, barW * foodPct, 2.5F * s, BorderRadius.all(1.0F), new ColorRGBA(235, 185, 70));
   }
}
