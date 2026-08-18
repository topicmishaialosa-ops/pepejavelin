package tech.huihui.utility.mixin.client.render.gui.hud;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tech.huihui.client.modules.impl.render.Animations;
import tech.huihui.utility.interfaces.IMinecraft;

@Mixin({ChatHud.class})
public abstract class ChatHudMixin {
   @WrapOperation(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
)}
   )
   private int animationsMessage(DrawContext context, TextRenderer renderer, OrderedText text, int x, int y, int color, Operation<Integer> original, @Local ChatHudLine.Visible line, @Local(argsOnly = true, ordinal = 0) int currentTick) {
      Animations animations = Animations.INSTANCE;
      if (!animations.isEnabled() || !animations.getAnimate().isEnable("Появление сообщений")) {
         return (Integer)original.call(context, renderer, text, x, y, color);
      }

      double t = Math.max(0.0D, Math.min(1.0D, (double)((currentTick - line.addedTime()) + IMinecraft.mc.getRenderTickCounter().getTickDelta(false)) / 9.0D));
      float progress = t == 1.0D ? 1.0F : (float)(1.0D - Math.pow(2.0D, -10.0D * t));
      int alpha = (int)Math.round((double)((color >>> 24) & 255) * (double)progress);
      context.getMatrices().push();
      context.getMatrices().translate((float)(-(1.0D - (double)progress)) * 8.0F, 0.0F, 0.0F);
      int result = (Integer)original.call(context, renderer, text, x, y, (color & 16777215) | (alpha << 24));
      context.getMatrices().pop();
      return result;
   }
}