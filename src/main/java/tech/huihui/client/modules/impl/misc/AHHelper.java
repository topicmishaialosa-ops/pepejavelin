package tech.huihui.client.modules.impl.misc;

import lombok.Generated;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ColorSetting;
import tech.huihui.utility.game.server.AutoBuyUtil;
import tech.huihui.utility.interfaces.IClient;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "AH Helper",
   category = Category.MISC,
   description = "помощник в поиске дешевых предметов"
)
public final class AHHelper extends Module implements IClient {
   public static final AHHelper INSTANCE = new AHHelper();
   private final ColorSetting cheapSlotColor = new ColorSetting("Цвет дешевого", new ColorRGBA(64, 255, 64, 140));
   private final ColorSetting goodSlotColor = new ColorSetting("Цвет выгодного", new ColorRGBA(255, 255, 64, 140));
   private final BooleanSetting perUnit = new BooleanSetting("Выгодный за 1 шт", true);
   private boolean auction;
   private Slot cheapSlot;
   private Slot goodSlot;

   private AHHelper() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.auction = false;
      this.cheapSlot = null;
      this.goodSlot = null;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.auction = false;
      this.cheapSlot = null;
      this.goodSlot = null;
   }

   @Native
   public void scan(ScreenHandler handler) {
      if (handler == null) {
         this.auction = false;
         this.cheapSlot = null;
         this.goodSlot = null;
         return;
      }
      if (!this.auction && handler.slots.size() == 90 && ((Slot) handler.slots.get(49)).getStack().getItem() == net.minecraft.item.Items.NETHER_STAR) {
         this.auction = true;
      }
      if (!this.auction) {
         this.cheapSlot = null;
         this.goodSlot = null;
         return;
      }
      int containerSlots = Math.min(44, handler.slots.size());
      int minSum = Integer.MAX_VALUE;
      int minPerUnit = Integer.MAX_VALUE;
      Slot cheap = null;
      Slot good = null;
      for (int i = 0; i < containerSlots; i++) {
         Slot slot = (Slot) handler.slots.get(i);
         if (slot.getStack().isEmpty()) {
            continue;
         }
         int sum = AutoBuyUtil.getPrice(slot.getStack());
         if (sum <= 0 || sum == Integer.MAX_VALUE) {
            continue;
         }
         int per = sum / Math.max(1, slot.getStack().getCount());
         if (sum < minSum) {
            minSum = sum;
            cheap = slot;
         }
         if (per < minPerUnit) {
            minPerUnit = per;
            good = slot;
         }
      }
      this.cheapSlot = cheap;
      this.goodSlot = good;
   }

   @Native
   public void renderCheat(DrawContext context, Slot slot) {
      this.renderHighlight(context, slot, this.cheapSlotColor.getIntColor());
   }

   @Native
   public void renderGood(DrawContext context, Slot slot) {
      this.renderHighlight(context, slot, this.goodSlotColor.getIntColor());
   }

   @Native
   private void renderHighlight(DrawContext context, Slot slot, int color) {
      context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
      context.fill(slot.x, slot.y, slot.x + 16, slot.y + 1, -16777216);
      context.fill(slot.x, slot.y, slot.x + 1, slot.y + 16, -16777216);
      context.fill(slot.x + 15, slot.y, slot.x + 16, slot.y + 16, -16777216);
      context.fill(slot.x, slot.y + 15, slot.x + 16, slot.y + 16, -16777216);
   }

   @Generated
   public Slot getCheapSlot() {
      return this.cheapSlot;
   }

   @Generated
   public Slot getGoodSlot() {
      return this.goodSlot;
   }

   @Generated
   public boolean isAuction() {
      return this.auction;
   }

   @Generated
   public boolean isPerUnit() {
      return this.perUnit.isEnabled();
   }

   @Generated
   public ColorSetting getCheapSlotColor() {
      return this.cheapSlotColor;
   }

   @Generated
   public ColorSetting getGoodSlotColor() {
      return this.goodSlotColor;
   }
}