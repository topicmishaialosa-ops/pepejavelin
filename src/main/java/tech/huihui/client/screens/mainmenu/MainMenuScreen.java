package tech.huihui.client.screens.mainmenu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import tech.huihui.HuihuiClient;
import tech.huihui.base.font.Fonts;
import tech.huihui.base.theme.Theme;
import tech.huihui.client.modules.impl.misc.RenamePasterClient;
import tech.huihui.client.modules.impl.render.Menu;
import tech.huihui.utility.interfaces.IClient;
import tech.huihui.utility.math.MathUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.Gradient;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class MainMenuScreen extends Screen implements IClient {
   private static final float[] PARALLAX = new float[2];

   private final AnimationUtilState openState = new AnimationUtilState();
   private final MenuButton singleplayer;
   private final MenuButton multiplayer;
   private final MenuButton accounts;
   private final MenuButton settings;
   private final List<MenuButton> buttons = new ArrayList<>();
   private final List<EffectMarker.Marker> effects = new ArrayList<>();
   private float switchKnob;
   private float dragKnob = -1.0F;
   private float switchTrackX;
   private float switchTrackY;
   private int shuffleLayout = -1;

   public MainMenuScreen() {
      super(Text.empty());
      if (mc.currentScreen instanceof MainMenuScreen) {
         this.openState.set(1.0F);
      }
      if (Menu.INSTANCE.getLayoutIndex() == Menu.LAYOUT_RANDOM) {
         this.shuffleLayout = (int) (Math.random() * Menu.LAYOUT_RANDOM);
      }
      this.singleplayer = new MenuButton(88.0F, 38.0F, "Одиночный Режим", () -> mc.setScreen(new SelectWorldScreen(null)));
      this.multiplayer = new MenuButton(88.0F, 38.0F, "Сетевая Игра", () -> mc.setScreen(new MultiplayerScreen(null)));
      this.accounts = new MenuButton(181.0F, 30.0F, "Выбор аккаунта", () -> mc.setScreen(new AccountManagerScreen()));
      this.settings = new MenuButton(79.0F, 19.5F, "Настройки", () -> mc.setScreen(new OptionsScreen(null, mc.options)));
      this.buttons.add(this.singleplayer);
      this.buttons.add(this.multiplayer);
      this.buttons.add(this.accounts);
      this.buttons.add(this.settings);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void close() {
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      this.openState.expand(mc.currentScreen instanceof MainMenuScreen);
      this.openState.update(0.0F, 1.0F, 0.15F, delta);
      float fMin = Math.min(1.0F, this.openState.getValue() / 0.9F);
      float screenW = mw.getScaledWidth();
      float screenH = mw.getScaledHeight();

      renderParallaxBackground(context, screenW, screenH, mouseX, mouseY, 1.25F - (easeBack(fMin) * 0.2F));
      CustomDrawContext draw = CustomDrawContext.of(context);

      this.applyLayout(screenW, screenH, this.shuffleLayout >= 0 ? this.shuffleLayout : Menu.INSTANCE.getLayoutIndex());

      for (MenuButton button : this.buttons) {
         button.render(context, mouseX, mouseY, delta, fMin);
      }
      this.renderSwitch(draw, fMin, (float) mouseX);
      this.renderTitle(draw, screenW * 0.5F, ((screenH - this.singleplayer.getHeight()) * 0.5F) - 58.0F, fMin);
      this.renderLayoutPanel(draw, screenW, screenH, fMin, mouseX, mouseY);
      EffectMarker.renderAll(context.getMatrices(), delta, this.effects);
   }

   private boolean layoutPanelContains(float screenW, float screenH, double x, double y) {
      return MathUtil.isHovered(x, y, screenW - 178.0F, screenH - 70.0F, 168.0F, 60.0F);
   }

   private void renderLayoutPanel(CustomDrawContext draw, float screenW, float screenH, float open, int mx, int my) {
      MatrixStack matrices = draw.getMatrices();
      float px = screenW - 178.0F;
      float py = screenH - 70.0F;
      DrawUtil.drawRoundedRect(matrices, px, py, 168.0F, 60.0F, BorderRadius.all(8.0F), new ColorRGBA(11, 11, 13).withAlpha(165.0F * open));
      DrawUtil.drawRoundedBorder(matrices, px, py, 168.0F, 60.0F, 0.5F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255).withAlpha(14.0F * open));
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA accent = theme.getColor().withAlpha(255.0F * open);

      int layout = this.shuffleLayout >= 0 ? this.shuffleLayout : Menu.INSTANCE.getLayoutIndex();
      this.drawSwitcherRow(draw, px, py + 7.0F, open, layout, Menu.LAYOUTS.length, Menu.LAYOUTS[layout],
         this.shuffleLayout >= 0 ? "Случайно" : Menu.LAYOUTS[layout], mx, my);
      this.drawSwitcherRow(draw, px, py + 32.0F, open, Menu.INSTANCE.getStyleIndex(), Menu.STYLES.length, Menu.STYLES[Menu.INSTANCE.getStyleIndex()], null, mx, my);

      this.drawDots(matrices, px, py, 13.0F, layout, Menu.LAYOUTS.length, 7.0F, accent.withAlpha(open));
      this.drawDots(matrices, px, py, 38.0F, Menu.INSTANCE.getStyleIndex(), Menu.STYLES.length, 10.0F, accent.withAlpha(open));
   }

   private void drawSwitcherRow(CustomDrawContext draw, float px, float rowY, float open, int index, int count, String label, String subtitle, int mx, int my) {
      boolean hovL = MathUtil.isHovered(mx, my, px + 4.0F, rowY - 6.0F, 18.0F, 16.0F);
      boolean hovR = MathUtil.isHovered(mx, my, px + 146.0F, rowY - 6.0F, 18.0F, 16.0F);
      ColorRGBA arrowColor = new ColorRGBA(255, 255, 255, (int) (170.0F * open));
      this.drawArrow(draw.getMatrices(), px + 13.0F, rowY, arrowColor, hovL);
      this.drawArrow(draw.getMatrices(), px + 155.0F, rowY, arrowColor, hovR);
      String shown = subtitle != null ? subtitle + "  (" + (index + 1) + "/" + count + ")" : label + "  (" + (index + 1) + "/" + count + ")";
      float w = Fonts.REGULAR.getFont(4.0F).width(shown);
      draw.drawText(Fonts.REGULAR.getFont(4.0F), shown, px + ((168.0F - w) / 2.0F), rowY - 3.0F, new ColorRGBA(255, 255, 255, (int) (200.0F * open)));
   }

   private void drawArrow(MatrixStack matrices, float cx, float cy, ColorRGBA color, boolean reversed) {
      ColorRGBA c = reversed ? new ColorRGBA(255, 255, 255, 255) : color;
      float r = 4.0F;
      int dir = reversed ? -1 : 1;
      net.minecraft.util.math.Vec2f top = new net.minecraft.util.math.Vec2f(cx - (r * dir), cy - 4.5F);
      net.minecraft.util.math.Vec2f mid = new net.minecraft.util.math.Vec2f(cx + (r * dir), cy);
      net.minecraft.util.math.Vec2f bot = new net.minecraft.util.math.Vec2f(cx - (r * dir), cy + 4.5F);
      DrawUtil.drawLine(matrices, top, mid, c);
      DrawUtil.drawLine(matrices, bot, mid, c);
   }

   private void drawDots(MatrixStack matrices, float px, float py, float y, int active, int count, float spacing, ColorRGBA accent) {
      float size = 3.0F;
      float total = (count * spacing) - (spacing - size);
      float startX = px + ((168.0F - total) / 2.0F);
      for (int i = 0; i < count; i++) {
         ColorRGBA color = i == active ? accent : new ColorRGBA(255, 255, 255).withAlpha(35.0F * (accent.getAlpha() / 255.0F));
         DrawUtil.drawRoundedRect(matrices, startX + (i * spacing), py + y, size, size, BorderRadius.all(1.5F), color);
      }
   }

   private void applyLayout(float screenW, float screenH, int layout) {
      float gap = 6.0F;
      switch (layout) {
         case Menu.LAYOUT_LEFT: {
            float x = screenW * 0.08F;
            float y = screenH * 0.30F;
            this.singleplayer.setPosition(x, y);
            this.multiplayer.setPosition(x, y + 43.0F);
            this.accounts.setPosition(x, y + 83.0F);
            this.settings.setPosition(x, y + 113.5F);
            this.switchTrackX = x;
            this.switchTrackY = screenH * 0.85F;
            break;
         }
         case Menu.LAYOUT_RIGHT: {
            float x = screenW * 0.08F;
            float y = screenH * 0.30F;
            this.singleplayer.setPosition(screenW - x - 88.0F, y);
            this.multiplayer.setPosition(screenW - x - 88.0F, y + 43.0F);
            this.accounts.setPosition(screenW - x - 181.0F, y + 83.0F);
            this.settings.setPosition(screenW - x - 79.0F, y + 113.5F);
            this.switchTrackX = screenW - x - 79.0F;
            this.switchTrackY = screenH * 0.85F;
            break;
         }
         case Menu.LAYOUT_BOTTOM: {
            float rowY = screenH * 0.82F;
            float startX = (screenW - 539.0F) / 2.0F;
            this.singleplayer.setPosition(startX, rowY);
            this.multiplayer.setPosition(startX + 94.0F, rowY);
            this.accounts.setPosition(startX + 188.0F, rowY + 4.0F);
            this.settings.setPosition(startX + 375.0F, rowY + 9.25F);
            this.switchTrackX = startX + 460.0F;
            this.switchTrackY = rowY + 9.25F;
            break;
         }
         case Menu.LAYOUT_TOP: {
            float rowY = screenH * 0.08F;
            float startX = (screenW - 539.0F) / 2.0F;
            this.singleplayer.setPosition(startX, rowY);
            this.multiplayer.setPosition(startX + 94.0F, rowY);
            this.accounts.setPosition(startX + 188.0F, rowY + 4.0F);
            this.settings.setPosition(startX + 375.0F, rowY + 9.25F);
            this.switchTrackX = startX + 460.0F;
            this.switchTrackY = rowY + 9.25F;
            break;
         }
         case Menu.LAYOUT_CORNERS: {
            float x = screenW * 0.07F;
            this.singleplayer.setPosition(x, screenH * 0.18F);
            this.multiplayer.setPosition(screenW - x - 88.0F, screenH * 0.18F);
            this.accounts.setPosition(x, screenH * 0.74F);
            this.settings.setPosition(screenW - x - 79.0F, screenH * 0.74F);
            this.switchTrackX = (screenW - 79.0F) / 2.0F;
            this.switchTrackY = screenH * 0.90F;
            break;
         }
         case Menu.LAYOUT_DIAGONAL: {
            this.singleplayer.setPosition(screenW * 0.14F, screenH * 0.12F);
            this.multiplayer.setPosition(screenW * 0.38F, screenH * 0.34F);
            this.accounts.setPosition(screenW * 0.58F, screenH * 0.56F);
            this.settings.setPosition(screenW * 0.74F, screenH * 0.78F);
            this.switchTrackX = (screenW - 79.0F) / 2.0F;
            this.switchTrackY = screenH * 0.92F;
            break;
         }
         case Menu.LAYOUT_SCATTER: {
            this.singleplayer.setPosition(screenW * 0.07F, screenH * 0.30F);
            this.multiplayer.setPosition(screenW * 0.90F - 88.0F, screenH * 0.22F);
            this.accounts.setPosition(screenW * 0.26F, screenH * 0.66F);
            this.settings.setPosition(screenW * 0.68F, screenH * 0.72F);
            this.switchTrackX = screenW * 0.68F;
            this.switchTrackY = screenH * 0.88F;
            break;
         }
         default: {
            float mainY = (screenH - 38.0F) / 2.0F;
            this.singleplayer.setPosition((screenW - 181.0F) / 2.0F, mainY);
            this.multiplayer.setPosition((screenW - 181.0F) / 2.0F + 88.0F + 5.0F, mainY);
            this.accounts.setPosition((screenW - 181.0F) / 2.0F, mainY + 43.0F);
            this.switchTrackX = (screenW - 79.0F) / 2.0F;
            this.switchTrackY = screenH * 0.85F;
            this.settings.setPosition((screenW - 79.0F) / 2.0F, this.switchTrackY - 24.5F);
            break;
         }
      }
   }

   private void renderTitle(CustomDrawContext draw, float centerX, float titleY, float open) {
      String clientName = RenamePasterClient.getClientName();
      float titleWidth = Fonts.ROUND_BOLD.getFont(12.0F).width(clientName);
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA primary = theme.getColor().withAlpha(255.0F * open);
      ColorRGBA white = new ColorRGBA(255, 255, 255).withAlpha(255.0F * open);
      MatrixStack matrices = draw.getMatrices();
      float scale = 0.85F + (0.15F * easeBack(open));
      matrices.push();
      matrices.translate(centerX, titleY + 8.0F, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-centerX, -titleY - 8.0F, 0.0F);
      draw.drawText(Fonts.ROUND_BOLD.getFont(12.0F), clientName, centerX - (titleWidth / 2.0F), titleY + 1.5F, Gradient.of(primary, white, white, primary));
      draw.drawText(Fonts.ROUND_BOLD.getFont(7.0F), "1.21.4", centerX - (Fonts.ROUND_BOLD.getFont(7.0F).width("1.21.4") / 2.0F), titleY + 21.5F, new ColorRGBA(255, 255, 255, (int) (160.0F * open)));
      matrices.pop();
   }

   static void renderParallaxBackground(DrawContext context, float width, float height, int mouseX, int mouseY, float scale) {
      float marginX = width * 0.025F;
      float marginY = height * 0.025F;
      PARALLAX[0] += ((MathHelper.clamp((((mouseX / width) - 0.5F) * 2.0F) * marginX, -marginX * 0.9F, marginX * 0.9F) - PARALLAX[0]) * 0.03F);
      PARALLAX[1] += ((MathHelper.clamp((((mouseY / height) - 0.5F) * 2.0F) * marginY, -marginY * 0.9F, marginY * 0.9F) - PARALLAX[1]) * 0.03F);
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(width / 2.0F, height / 2.0F, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-width / 2.0F, -height / 2.0F, 0.0F);
      Theme theme = HuihuiClient.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();
      ColorRGBA second = theme.getSecondColor();
      DrawUtil.drawRoundedRect(matrices, -marginX * 4.0F, -marginY * 4.0F, width + (marginX * 8.0F), height + (marginY * 8.0F), BorderRadius.ZERO,
         new ColorRGBA(11, 11, 15), new ColorRGBA(9, 9, 13), new ColorRGBA(6, 6, 10), new ColorRGBA(14, 14, 20));
      float driftX = MathHelper.sin(System.currentTimeMillis() / 2400.0F) * 26.0F + PARALLAX[0];
      float driftY = MathHelper.cos(System.currentTimeMillis() / 3200.0F) * 20.0F + PARALLAX[1];
      DrawUtil.drawGlow(matrices, width * 0.10F - 150.0F + driftX, height * 0.18F - 120.0F + driftY, 440.0F, 280.0F, 90);
      DrawUtil.drawGlow(matrices, width * 0.90F - 130.0F - driftX, height * 0.82F - 40.0F - driftY, 380.0F, 220.0F, 70);
      if (themeColor.getRed() + themeColor.getGreen() + themeColor.getBlue() > 180) {
         DrawUtil.drawGlow(matrices, width * 0.5F - 90.0F + driftX, height * 0.5F - 60.0F + driftY, 280.0F, 160.0F, 40);
      }
      matrices.pop();
   }

   private void renderSwitch(CustomDrawContext draw, float open, float mouseX) {
      float target = this.dragKnob >= 0.0F ? MathHelper.clamp((((mouseX - this.dragKnob) - this.switchTrackX) - 1.75F) / 59.5F, 0.0F, 1.0F) : 0.0F;
      this.switchKnob += (target - this.switchKnob) * 0.25F;
      float scale = 0.85F + (0.15F * easeBack(open));
      MatrixStack matrices = draw.getMatrices();
      matrices.push();
      matrices.translate(this.switchTrackX + 39.5F, this.switchTrackY + 9.75F, 0.0F);
      matrices.scale(scale, scale, 1.0F);
      matrices.translate(-this.switchTrackX - 39.5F, -this.switchTrackY - 9.75F, 0.0F);
      float knobX = this.switchTrackX + 1.75F + (this.switchKnob * 59.5F);
      float knobY = this.switchTrackY + 1.75F;
      float centerX = knobX + 8.0F;
      float centerY = knobY + 8.0F;
      DrawUtil.drawRoundedRect(matrices, this.switchTrackX, this.switchTrackY, 79.0F, 19.5F, BorderRadius.all(8.0F), new ColorRGBA(11, 11, 13).withAlpha(195.0F * open));
      DrawUtil.drawRoundedBorder(matrices, this.switchTrackX, this.switchTrackY, 79.0F, 19.5F, 0.5F, BorderRadius.all(8.0F), new ColorRGBA(255, 255, 255).withAlpha(15.0F * open));
      float labelX = this.switchTrackX + 9.0F;
      float labelWidth = Fonts.REGULAR.getFont(6.0F).width("Выйти из игры");
      String clipped = labelWidth > ((knobX - 3.0F) - this.switchTrackX) - 9.0F ? clipText("Выйти из игры", ((knobX - 3.0F) - this.switchTrackX) - 9.0F) : "Выйти из игры";
      draw.drawText(Fonts.REGULAR.getFont(6.0F), clipped, labelX, this.switchTrackY + ((19.5F - Fonts.REGULAR.getFont(6.0F).height()) / 2.0F), new ColorRGBA(220, 80, 80, (int) (this.switchKnob * 255.0F * open)));
      ColorRGBA knob = ColorRGBA.lerp(new ColorRGBA(255, 255, 255, 13), new ColorRGBA(220, 80, 80, 40), this.switchKnob);
      DrawUtil.drawRoundedRect(matrices, knobX, knobY, 16.0F, 16.0F, BorderRadius.all(7.0F), knob.withAlpha(255.0F * open));
      matrices.push();
      matrices.translate(centerX, centerY, 0.0F);
      matrices.multiply(new Quaternionf().rotateZ((float) Math.toRadians(-90.0F + (180.0F * this.switchKnob))));
      matrices.translate(-centerX, -centerY, 0.0F);
      ColorRGBA iconColor = ColorRGBA.lerp(new ColorRGBA(255, 255, 255), new ColorRGBA(220, 80, 80), this.switchKnob);
      float iconW = Fonts.ICONS.getFont(10.5F).width("4");
      draw.drawText(Fonts.ICONS.getFont(10.5F), "4", (centerX - iconW / 2.0F) + 1.0F, centerY - (Fonts.ICONS.getFont(10.5F).height() / 2.0F), iconColor.withAlpha(255.0F * open));
      matrices.pop();
      matrices.pop();
   }

   private String clipText(String text, float maxWidth) {
      StringBuilder builder = new StringBuilder();
      for (char c : text.toCharArray()) {
         if (Fonts.REGULAR.getFont(6.0F).width(builder.toString() + c) > maxWidth) {
            break;
         }
         builder.append(c);
      }
      return builder.toString();
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_F1) {
         this.shuffleLayout = -1;
         Menu.INSTANCE.cycleLayout(-1);
         return true;
      }
      if (keyCode == GLFW.GLFW_KEY_F2) {
         this.shuffleLayout = -1;
         Menu.INSTANCE.cycleLayout(1);
         return true;
      }
      if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
         this.shuffleLayout = -1;
         Menu.INSTANCE.setLayoutIndex(keyCode - GLFW.GLFW_KEY_1);
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float screenW = mw.getScaledWidth();
      float screenH = mw.getScaledHeight();
      if (this.layoutPanelContains(screenW, screenH, mouseX, mouseY)) {
         float px = screenW - 178.0F;
         float py = screenH - 70.0F;
         if (MathUtil.isHovered(mouseX, mouseY, px + 4.0F, py + 1.0F, 18.0F, 16.0F)) {
            this.shuffleLayout = -1;
            Menu.INSTANCE.cycleLayout(-1);
            return true;
         }
         if (MathUtil.isHovered(mouseX, mouseY, px + 146.0F, py + 1.0F, 18.0F, 16.0F)) {
            this.shuffleLayout = -1;
            Menu.INSTANCE.cycleLayout(1);
            return true;
         }
         if (MathUtil.isHovered(mouseX, mouseY, px + 4.0F, py + 26.0F, 18.0F, 16.0F)) {
            Menu.INSTANCE.cycleStyle(-1);
            return true;
         }
         if (MathUtil.isHovered(mouseX, mouseY, px + 146.0F, py + 26.0F, 18.0F, 16.0F)) {
            Menu.INSTANCE.cycleStyle(1);
            return true;
         }
      }
      EffectMarker.spawn(this.effects, (float) mouseX, (float) mouseY);
      float dragX = (float) mouseX;
      float dragY = (float) mouseY;
      if (MathUtil.isHovered(dragX, dragY, this.switchTrackX + 1.75F, this.switchTrackY + 1.75F, 16.0F, 16.0F)) {
         this.dragKnob = dragX - this.switchTrackX;
         return true;
      }
      for (MenuButton menuButton : this.buttons) {
         if (menuButton.isHovered(dragX, dragY)) {
            menuButton.click();
            return true;
         }
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (this.dragKnob >= 0.0F) {
         return true;
      }
      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.dragKnob < 0.0F) {
         return super.mouseReleased(mouseX, mouseY, button);
      }
      float progress = MathHelper.clamp(((((float) mouseX - this.dragKnob) - this.switchTrackX) - 1.75F) / 59.5F, 0.0F, 1.0F);
      this.dragKnob = -1.0F;
      if (progress < 0.95F) {
         return true;
      }
      mc.scheduleStop();
      return true;
   }

   private static float easeBack(float value) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + (c3 * (value - 1.0F) * (value - 1.0F) * (value - 1.0F)) + (c1 * (value - 1.0F) * (value - 1.0F));
   }
}