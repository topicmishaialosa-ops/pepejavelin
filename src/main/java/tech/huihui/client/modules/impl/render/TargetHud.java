package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
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
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.impl.combat.Aura;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.client.modules.impl.misc.ScoreboardHealth;
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
   private final ModeSetting type = new ModeSetting("Тип", "Крупный", "Маленький", "Проценты", "Большая полоса", "Вертикальный", "Метал", "Мини", "Градиент", "Боссбар", "Минимал");
   private final NumberSetting x = new NumberSetting("X", 4.0F, 0.0F, 1920.0F, 1.0F);
   private final NumberSetting y = new NumberSetting("Y", 4.0F, 0.0F, 1080.0F, 1.0F);
   private final BooleanSetting hover = new BooleanSetting("Наведение", true);
   private final Animation visibleAnimation = new Animation(220L, Easing.CIRC_OUT);
   private final Animation healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
   private final Animation outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
   private final Animation gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
   private final Animation toggleAnimationMetanoise = new Animation(1850L, Easing.CIRC_OUT);
   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private LivingEntity target;

   private TargetHud() {
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
      boolean inChat = mc.currentScreen instanceof ChatScreen;
      LivingEntity current = inChat ? mc.player : this.getTarget();
      this.setTarget(current);
      if (this.visibleAnimation.getValue() == 0.0F || this.target == null) {
         if (inChat && this.visibleAnimation.getValue() == 0.0F) {
            this.renderPlaceholder(event.getContext());
         }
         return;
      }
      CustomDrawContext ctx = event.getContext();
      float alpha = this.visibleAnimation.getValue();
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

   private float[] currentSize() {
      if (this.type.is("Крупный")) {
         return new float[]{132.0F, 48.0F};
      } else if (this.type.is("Маленький")) {
         return new float[]{84.0F, 20.0F};
      } else if (this.type.is("Проценты")) {
         return new float[]{112.0F, 42.0F};
      } else if (this.type.is("Большая полоса")) {
         return new float[]{124.0F, 34.0F};
      } else if (this.type.is("Вертикальный")) {
         return new float[]{74.0F, 58.0F};
      } else if (this.type.is("Метал")) {
         return new float[]{86.0F, 30.0F};
      } else if (this.type.is("Мини")) {
         return new float[]{64.0F, 16.0F};
      } else if (this.type.is("Градиент")) {
         return new float[]{112.0F, 38.0F};
      } else if (this.type.is("Боссбар")) {
         return new float[]{180.0F, 34.0F};
      } else {
         return new float[]{110.0F, 22.0F};
      }
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
      return String.format("%.1f", this.hp()).replace(",", ".");
   }

   private String percentText() {
      float percent = this.hp() / this.target.getMaxHealth() * 100.0F;
      return String.format("%.0f", percent) + "%";
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

   private ColorRGBA[] hpColors(Theme theme, float alpha) {
      float percent = this.hp() / this.target.getMaxHealth();
      if (percent <= 0.25F) {
         return new ColorRGBA[]{(new ColorRGBA(255, 60, 60)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 60, 60)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 30, 30)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 30, 30)).withAlpha(255.0F * alpha)};
      } else if (percent <= 0.5F) {
         return new ColorRGBA[]{(new ColorRGBA(255, 200, 40)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 200, 40)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 160, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 160, 20)).withAlpha(255.0F * alpha)};
      } else {
         return new ColorRGBA[]{theme.getSecondColor().withAlpha(255.0F * alpha), theme.getSecondColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha)};
      }
   }

   private void drawBackground(CustomDrawContext ctx, float x, float y, float width, float height, float radius, float alpha) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(radius), (new ColorRGBA(0, 0, 0)).withAlpha(120.0F * alpha));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(radius), this.theme().getSecondColor().darker(0.5F).withAlpha(255.0F * alpha));
   }

   private void drawBar(CustomDrawContext ctx, float x, float y, float width, float height, float radius, float alpha) {
      Theme theme = this.theme();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(radius), (new ColorRGBA(0, 0, 0)).withAlpha(90.0F * alpha));
      float outdatedW = MathHelper.clamp(width * this.outdatedHealthAnimation.getValue(), 0.0F, width);
      ColorRGBA[] outdated = new ColorRGBA[]{theme.getSecondColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getSecondColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getColor().darker(0.35F).withAlpha(255.0F * alpha)};
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, outdatedW, height, BorderRadius.all(radius), outdated[0], outdated[1], outdated[2], outdated[3]);
      if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
         float mainW = MathHelper.clamp(width * this.healthAnimation.getValue(), 0.0F, width);
         ColorRGBA[] main = this.hpColors(theme, alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, mainW, height, BorderRadius.all(radius), main[0], main[1], main[2], main[3]);
      }
      float absW = MathHelper.clamp(width * this.gappleAnimation.getValue(), 0.0F, width);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, absW, height, BorderRadius.all(radius), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
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
      DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), this.skinTexture(), x, y, size, BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(255.0F * alpha));
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
      float height = 48.0F;
      this.drawBackground(ctx, x, y, width, height, 5.0F, alpha);
      this.drawHead(ctx, x + 5.0F, y + 5.0F, 32.0F, alpha);
      MsdfRenderer.renderText(Fonts.ROUND_BOLD, this.name(), 8.5F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 43.0F, y + 6.0F, 0.0F, true, 0.7F, 1.0F, 82.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), "HP: " + this.hpText(), x + 43.0F, y + 18.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), this.percentText(), x + 43.0F + Fonts.REGULAR.getWidth("HP: " + this.hpText(), 6.5F) + 3.0F, y + 18.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 43.0F, y + 28.0F, 84.0F, 4.0F, 2.0F, alpha);
      this.drawArmor(ctx, x + 4.0F, y + 37.0F, 10.0F, alpha);
   }

   private void renderSmall(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 84.0F;
      float height = 20.0F;
      this.drawBackground(ctx, x, y, width, height, 4.0F, alpha);
      this.drawHead(ctx, x + 3.0F, y + 3.0F, 14.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 6.5F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 20.0F, y + 3.0F, 0.0F, true, 0.7F, 1.0F, 58.0F);
      ColorRGBA percentColor = this.hpColors(this.theme(), 1.0F)[0];
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), this.percentText(), x + 20.0F, y + 10.5F, percentColor.withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 20.0F, y + 17.0F, 60.0F, 2.0F, 1.0F, alpha);
   }

   private void renderPercent(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 112.0F;
      float height = 42.0F;
      this.drawBackground(ctx, x, y, width, height, 5.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 56.0F - Fonts.REGULAR.getWidth(this.name(), 7.0F) / 2.0F, y + 4.0F, 0.0F, true, 0.7F, 1.0F, 106.0F);
      ColorRGBA[] colors = this.hpColors(this.theme(), alpha);
      MsdfRenderer.renderText(Fonts.ROUND_BOLD, this.percentText(), 15.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 56.0F - Fonts.ROUND_BOLD.getWidth(this.percentText(), 15.0F) / 2.0F, y + 10.0F, 0.0F, true, 0.7F, 1.0F, 106.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), "HP: " + this.hpText(), x + 56.0F - Fonts.REGULAR.getWidth("HP: " + this.hpText(), 6.0F) / 2.0F, y + 31.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 6.0F, y + 36.0F, width - 12.0F, 4.0F, 2.0F, alpha);
   }

   private void renderBigBar(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 124.0F;
      float height = 34.0F;
      this.drawBackground(ctx, x, y, width, height, 5.0F, alpha);
      MsdfRenderer.renderText(Fonts.SEMIBOLD, this.name(), 7.5F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 6.0F, y + 3.0F, 0.0F, true, 0.7F, 1.0F, 112.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), "HP: " + this.hpText() + " (" + this.percentText() + ")", x + 6.0F, y + 11.5F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 6.0F, y + 20.0F, width - 12.0F, 10.0F, 2.0F, alpha);
      float percent = this.healthAnimation.getValue();
      MsdfRenderer.renderText(Fonts.BOLD, this.percentText(), 6.5F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 6.0F + (width - 12.0F) / 2.0F - Fonts.BOLD.getWidth(this.percentText(), 6.5F) / 2.0F, y + 21.8F, 0.0F);
   }

   private void renderVertical(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 74.0F;
      float height = 58.0F;
      this.drawBackground(ctx, x, y, width, height, 5.0F, alpha);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 4.0F, 8.0F, 50.0F, BorderRadius.all(2.0F), (new ColorRGBA(0, 0, 0)).withAlpha(90.0F * alpha));
      float outdatedH = MathHelper.clamp(50.0F * this.outdatedHealthAnimation.getValue(), 0.0F, 50.0F);
      Theme theme = this.theme();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 54.0F - outdatedH, 8.0F, outdatedH, BorderRadius.all(2.0F), theme.getSecondColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getSecondColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getColor().darker(0.35F).withAlpha(255.0F * alpha), theme.getColor().darker(0.35F).withAlpha(255.0F * alpha));
      if (this.gappleAnimation.getValue() < this.healthAnimation.getValue()) {
         float mainH = MathHelper.clamp(50.0F * this.healthAnimation.getValue(), 0.0F, 50.0F);
         ColorRGBA[] main = this.hpColors(theme, alpha);
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 54.0F - mainH, 8.0F, mainH, BorderRadius.all(2.0F), main[0], main[1], main[2], main[3]);
      }
      float absH = MathHelper.clamp(50.0F * this.gappleAnimation.getValue(), 0.0F, 50.0F);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 54.0F - absH, 8.0F, absH, BorderRadius.all(2.0F), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
      this.drawHead(ctx, x + 17.0F, y + 4.0F, 22.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 17.0F, y + 29.0F, 0.0F, true, 0.7F, 1.0F, 52.0F);
      ColorRGBA[] colors = this.hpColors(theme, alpha);
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), this.percentText(), x + 17.0F, y + 39.0F, colors[0]);
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), "HP " + this.hpText(), x + 17.0F, y + 48.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
   }

   private void renderMetanoise(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 86.0F;
      float height = 30.0F;
      Theme theme = this.theme();
      StencilUtil.push();
      DrawUtil.drawMetanoise(ctx.getMatrices(), x, y, width, height, this.toggleAnimationMetanoise.getValue(), 3.0F, (new ColorRGBA(0, 0, 0)).withAlpha(140.0F * alpha), theme.getColor().withAlpha(255.0F * alpha));
      StencilUtil.read(1);
      DrawUtil.drawBlur(ctx.getMatrices(), x, y, width, height, 11.0F, BorderRadius.all(3.0F), new ColorRGBA(255, 255, 255, 255.0F * alpha));
      DrawUtil.drawMetanoise(ctx.getMatrices(), x, y, width, height, this.toggleAnimationMetanoise.getValue(), 3.0F, (new ColorRGBA(0, 0, 0)).withAlpha(140.0F * alpha), theme.getColor().withAlpha(255.0F * alpha));
      this.drawHead(ctx, x + 4.0F, y + 4.0F, 22.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.25F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 29.0F, y + 5.5F, 0.0F, true, 0.7F, 1.0F, 56.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.5F), "HP: " + this.hpText() + " (" + this.percentText() + ")", x + 29.75F, y + 14.25F, ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      this.drawBar(ctx, x + 29.0F, y + 22.0F, width - 33.0F, 3.25F, 0.25F, alpha);
      StencilUtil.pop();
      this.drawArmor(ctx, x + 3.0F, y - 12.0F, 10.0F, alpha);
   }

   private void renderMini(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 64.0F;
      float height = 16.0F;
      this.drawBackground(ctx, x, y, width, height, 3.0F, alpha);
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 6.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 4.0F, y + 1.5F, 0.0F, true, 0.7F, 1.0F, 38.0F);
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), this.percentText(), x + 44.0F, y + 2.0F, this.hpColors(this.theme(), alpha)[0]);
      this.drawBar(ctx, x + 4.0F, y + 12.0F, 56.0F, 2.0F, 1.0F, alpha);
   }

   private void renderGradient(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 112.0F;
      float height = 38.0F;
      Theme theme = this.theme();
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, width, height, BorderRadius.all(5.0F), theme.getColor().withAlpha(40.0F * alpha), theme.getColor().withAlpha(10.0F * alpha), theme.getSecondColor().withAlpha(10.0F * alpha), theme.getSecondColor().withAlpha(40.0F * alpha));
      DrawUtil.drawRoundedBorder(ctx.getMatrices(), x, y, width, height, 1.0F, BorderRadius.all(5.0F), theme.getSecondColor().withAlpha(255.0F * alpha));
      this.drawHead(ctx, x + 6.0F, y + 7.0F, 24.0F, alpha);
      MsdfRenderer.renderText(Fonts.SEMIBOLD, this.name(), 7.5F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 36.0F, y + 6.0F, 0.0F, true, 0.7F, 1.0F, 70.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), "HP: " + this.hpText() + " (" + this.percentText() + ")", x + 36.0F, y + 16.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 36.0F, y + 27.0F, 70.0F, 5.0F, BorderRadius.all(2.5F), (new ColorRGBA(0, 0, 0)).withAlpha(90.0F * alpha));
      float mainW = MathHelper.clamp(70.0F * this.healthAnimation.getValue(), 0.0F, 70.0F);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 36.0F, y + 27.0F, mainW, 5.0F, BorderRadius.all(2.5F), theme.getSecondColor().withAlpha(255.0F * alpha), theme.getSecondColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha), theme.getColor().withAlpha(255.0F * alpha));
      float absW = MathHelper.clamp(70.0F * this.gappleAnimation.getValue(), 0.0F, 70.0F);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 36.0F, y + 27.0F, absW, 5.0F, BorderRadius.all(2.5F), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
   }

   private void renderBossbar(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 180.0F;
      float height = 34.0F;
      this.drawBackground(ctx, x, y, width, height, 3.0F, alpha);
      MsdfRenderer.renderText(Fonts.SEMIBOLD, this.name(), 7.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 6.0F, y + 3.0F, 0.0F, true, 0.7F, 1.0F, 168.0F);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 13.0F, width - 8.0F, 8.0F, BorderRadius.all(2.0F), (new ColorRGBA(0, 0, 0)).withAlpha(120.0F * alpha));
      float mainW = MathHelper.clamp((width - 8.0F) * this.healthAnimation.getValue(), 0.0F, width - 8.0F);
      ColorRGBA[] main = this.hpColors(this.theme(), alpha);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 13.0F, mainW, 8.0F, BorderRadius.all(2.0F), main[0], main[1], main[2], main[3]);
      float absW = MathHelper.clamp((width - 8.0F) * this.gappleAnimation.getValue(), 0.0F, width - 8.0F);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + 4.0F, y + 13.0F, absW, 8.0F, BorderRadius.all(2.0F), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 209, 0)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha), (new ColorRGBA(255, 246, 20)).withAlpha(255.0F * alpha));
      MsdfRenderer.renderText(Fonts.BOLD, this.percentText(), 6.5F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x + 90.0F - Fonts.BOLD.getWidth(this.percentText(), 6.5F) / 2.0F, y + 14.4F, 0.0F);
      ctx.drawText(Fonts.REGULAR.getFont(5.5F), "HP: " + this.hpText() + " / " + String.format("%.0f", this.target.getMaxHealth()), x + 6.0F, y + 25.0F, (new ColorRGBA(153, 153, 153)).withAlpha(255.0F * alpha));
   }

   private void renderMinimal(CustomDrawContext ctx, float alpha) {
      this.updateAnimations();
      float x = this.x.getCurrent();
      float y = this.y.getCurrent();
      float width = 110.0F;
      float height = 22.0F;
      Theme theme = this.theme();
      MsdfRenderer.renderText(Fonts.REGULAR, this.name(), 7.0F, ColorRGBA.WHITE.withAlpha(255.0F * alpha).getRGB(), ctx.getMatrices().peek().getPositionMatrix(), x, y, 0.0F, true, 0.7F, 1.0F, 110.0F);
      ctx.drawText(Fonts.REGULAR.getFont(6.0F), " | " + this.percentText() + " (" + this.hpText() + " HP)", x + 5.0F + Math.min(Fonts.REGULAR.getWidth(this.name(), 7.0F), 110.0F), y + 2.0F, this.hpColors(theme, alpha)[0]);
      float mainW = MathHelper.clamp(width * this.healthAnimation.getValue(), 0.0F, width);
      ColorRGBA[] main = this.hpColors(theme, alpha);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y + 17.0F, mainW, 1.5F, BorderRadius.all(0.75F), main[0], main[1], main[2], main[3]);
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + mainW, y + 17.0F, width - mainW, 1.5F, BorderRadius.all(0.75F), (new ColorRGBA(255, 255, 255)).withAlpha(40.0F * alpha), (new ColorRGBA(255, 255, 255)).withAlpha(40.0F * alpha), (new ColorRGBA(255, 255, 255)).withAlpha(40.0F * alpha), (new ColorRGBA(255, 255, 255)).withAlpha(40.0F * alpha));
   }
}
