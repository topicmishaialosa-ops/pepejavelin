package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.screen.slot.Slot;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.base.events.impl.render.EventHudRender;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.utility.render.animation.AnimationUtil;

import java.util.HashMap;
import java.util.Map;

@ModuleAnnotation(
   name = "Animations",
   category = Category.RENDER,
   description = "Анимирует выбранные элементы игры"
)
public final class Animations extends Module {
   public static final Animations INSTANCE = new Animations();
   private final MultiBooleanSetting animate = new MultiBooleanSetting("Выберите что анимировать", MultiBooleanSetting.Value.of("TAB", true), MultiBooleanSetting.Value.of("Открытие инвентаря", true), MultiBooleanSetting.Value.of("Смена перспективы", true), MultiBooleanSetting.Value.of("Поднятие хотбара", true), MultiBooleanSetting.Value.of("Слот хотбара", true), MultiBooleanSetting.Value.of("Появление сообщений", true), MultiBooleanSetting.Value.of("Предметы", true));
   private final AnimationUtil tabAnimation = new AnimationUtil();
   private final AnimationUtil hotbarAnimation = new AnimationUtil();
   private final AnimationUtil inventoryAnimation = new AnimationUtil();
   private final AnimationUtil perspectiveAnimation = new AnimationUtil();
   private final Map<Slot, AnimationUtil> slotAnimations = new HashMap();
   private float selectedSlot = -1.0F;

   private Animations() {
   }

   public MultiBooleanSetting getAnimate() {
      return this.animate;
   }

   public AnimationUtil getTabAnimation() {
      return this.tabAnimation;
   }

   public AnimationUtil getHotbarAnimation() {
      return this.hotbarAnimation;
   }

   public AnimationUtil getInventoryAnimation() {
      return this.inventoryAnimation;
   }

   public AnimationUtil getPerspectiveAnimation() {
      return this.perspectiveAnimation;
   }

   public float getSelectedSlot() {
      return this.selectedSlot;
   }

   public AnimationUtil getSlotAnimation(Slot slot) {
      return this.slotAnimations.computeIfAbsent(slot, (s) -> {
         return new AnimationUtil();
      });
   }

   public void onEnable() {
      super.onEnable();
      this.selectedSlot = -1.0F;
      this.slotAnimations.clear();
   }

   @EventTarget
   private void onRender(EventHudRender event) {
      this.tabAnimation.update(0.0F, 1.0F, 0.5F, event.getTickDelta());
      this.hotbarAnimation.update(0.0F, 1.0F, 0.45F, event.getTickDelta());
      this.inventoryAnimation.update(0.0F, 1.0F, 0.4F, event.getTickDelta());
      this.perspectiveAnimation.update(0.0F, 1.0F, 0.35F, event.getTickDelta());
      if (mc.player != null) {
         if (this.selectedSlot < 0.0F) {
            this.selectedSlot = (float)mc.player.getInventory().selectedSlot;
         }

         float target = (float)mc.player.getInventory().selectedSlot;
         float diff = target - this.selectedSlot;
         this.selectedSlot += Math.copySign(Math.min(Math.abs(diff), 1.25F), diff);
      }

   }

   @EventTarget
   private void onTick(EventTick event) {
      this.tabAnimation.update(mc.options.playerListKey.isPressed());
      this.inventoryAnimation.update(mc.currentScreen instanceof InventoryScreen);
      this.perspectiveAnimation.update(mc.options.getPerspective() != Perspective.FIRST_PERSON);
      this.hotbarAnimation.update(mc.currentScreen instanceof ChatScreen);
   }
}
