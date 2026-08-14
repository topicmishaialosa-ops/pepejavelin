package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "ChestStealer",
   category = Category.MISC,
   description = "Автоматически забирает предметы из сундуков"
)
public final class ChestStealer extends Module {
   public static final ChestStealer INSTANCE = new ChestStealer();
   private final NumberSetting delay = new NumberSetting("Задержка", 2.0F, 1.0F, 10.0F, 1.0F);
   private final BooleanSetting onlyChest = new BooleanSetting("Только сундуки", true);
   private final BooleanSetting closeWhenDone = new BooleanSetting("Закрывать когда пусто", true);
   private static final int CLOSE_GRACE_TICKS = 40;
   private int tickDelay;
   private int nextSlot;
   private int emptyTicks;

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.currentScreen == null) {
         this.nextSlot = 0;
         this.tickDelay = 0;
         this.emptyTicks = 0;
         return;
      }
      if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
         this.emptyTicks = 0;
         return;
      }
      ScreenHandler handler = screen.getScreenHandler();
      if (handler == null) {
         return;
      }
      if (this.onlyChest.isEnabled() && !(mc.currentScreen instanceof GenericContainerScreen)) {
         return;
      }

      if (this.tickDelay-- > 0) {
         this.emptyTicks = 0;
         return;
      }
      this.tickDelay = (int) this.delay.getCurrent();

      int containerSlots = this.getContainerSlots(handler);
      boolean empty = true;
      for (int i = this.nextSlot; i < containerSlots; ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.isEmpty()) {
            continue;
         }
         empty = false;
         mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
         this.nextSlot = i + 1;
         this.emptyTicks = 0;
         return;
      }

      if (this.nextSlot >= containerSlots) {
         this.nextSlot = 0;
      }
      if (!empty) {
         this.emptyTicks = 0;
         return;
      }
      if (this.containerHasItems(containerSlots, handler)) {
         this.emptyTicks = 0;
         this.nextSlot = 0;
         return;
      }
      if (!this.closeWhenDone.isEnabled() || containerSlots <= 0) {
         return;
      }
      if (++this.emptyTicks < CLOSE_GRACE_TICKS) {
         return;
      }
      mc.execute(() -> mc.setScreen(null));
      this.nextSlot = 0;
      this.emptyTicks = 0;
   }

   private boolean containerHasItems(int containerSlots, ScreenHandler handler) {
      for (int i = 0; i < containerSlots; ++i) {
         if (!handler.getSlot(i).getStack().isEmpty()) {
            return true;
         }
      }
      return false;
   }

   private int getContainerSlots(ScreenHandler handler) {
      if (handler instanceof GenericContainerScreenHandler generic) {
         return generic.getRows() * 9;
      }
      return Math.max(0, handler.slots.size() - 36);
   }
}
