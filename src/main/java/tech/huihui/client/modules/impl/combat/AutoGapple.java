package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.player.PlayerIntersectionUtil;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "AutoGapple",
   category = Category.COMBAT,
   description = "Ест золотые яблоки из руки при низком здоровье"
)
public final class AutoGapple extends Module {
   public static final AutoGapple INSTANCE = new AutoGapple();
   private final NumberSetting health = new NumberSetting("Здоровье (ХП)", 10.0F, 0.0F, 20.0F, 0.5F);
   private final ModeSetting appleType = new ModeSetting("Тип яблока", "Золотое", "Зачарованное", "Оба");
   private final NumberSetting delay = new NumberSetting("Задержка (мс)", 250.0F, 50.0F, 2000.0F, 25.0F);
   private final Timer retryTimer = new Timer();

   private AutoGapple() {
   }

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.currentScreen != null) {
         return;
      }
      if (mc.player.getHealth() > this.health.getCurrent()) {
         return;
      }
      if (mc.player.isUsingItem()) {
         return;
      }
      ItemStack mainStack = mc.player.getMainHandStack();
      ItemStack offStack = mc.player.getOffHandStack();
      Hand hand = null;
      if (this.isApple(mainStack)) {
         hand = Hand.MAIN_HAND;
      } else if (this.isApple(offStack)) {
         hand = Hand.OFF_HAND;
      }
      if (hand == null) {
         return;
      }
      ItemStack stack = hand == Hand.MAIN_HAND ? mainStack : offStack;
      if (mc.player.getItemCooldownManager().isCoolingDown(stack)) {
         return;
      }
      if (!this.retryTimer.finished((long) this.delay.getCurrent())) {
         return;
      }
      PlayerIntersectionUtil.useItem(hand);
      this.retryTimer.reset();
   }

   private boolean isApple(ItemStack stack) {
      if (this.appleType.is("Зачарованное")) {
         return stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
      }
      if (this.appleType.is("Оба")) {
         return stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE);
      }
      return stack.isOf(Items.GOLDEN_APPLE);
   }
}
