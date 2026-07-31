package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import tech.huihui.base.events.impl.render.EventHandledScreen;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "ShulkerPreview",
   category = Category.RENDER,
   description = "Показывает содержимое шалкера при наведении"
)
public final class ShulkerPreview extends Module {
   public static final ShulkerPreview INSTANCE = new ShulkerPreview();

   @EventTarget
   private void onHandledScreen(EventHandledScreen event) {
      Slot slot = event.getSlotHover();
      if (slot == null || !slot.hasStack()) {
         return;
      }

      ItemStack stack = slot.getStack();
      if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
         return;
      }

      ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
      if (container == null) {
         return;
      }

      List<ItemStack> items = container.stream().limit(27L).toList();
      if (items.stream().allMatch(ItemStack::isEmpty)) {
         return;
      }

      DrawContext context = event.getDrawContext();
      int cell = 18;
      int padding = 4;
      int previewWidth = 9 * cell + padding * 2;
      int previewHeight = 3 * cell + padding * 2;

      int x = event.getMouseX() + 8;
      int y = event.getMouseY() - previewHeight / 2;
      int screenWidth = mc.getWindow().getScaledWidth();
      int screenHeight = mc.getWindow().getScaledHeight();
      x = Math.max(2, Math.min(x, screenWidth - previewWidth - 2));
      y = Math.max(2, Math.min(y, screenHeight - previewHeight - 2));

      context.fill(x, y, x + previewWidth, y + previewHeight, 0xCC0B0B0B);
      context.drawBorder(x, y, previewWidth, previewHeight, 0xFF3C3C3C);

      for (int i = 0; i < items.size(); ++i) {
         ItemStack item = items.get(i);
         if (item.isEmpty()) {
            continue;
         }

         int col = i % 9;
         int row = i / 9;
         context.drawItem(item, x + padding + col * cell, y + padding + row * cell);
      }
   }
}
