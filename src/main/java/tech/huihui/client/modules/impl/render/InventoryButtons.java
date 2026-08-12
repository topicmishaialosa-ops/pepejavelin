package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.huihui.base.events.impl.input.EventMouse;
import tech.huihui.base.events.impl.render.EventHandledScreen;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "InventoryButtons",
   category = Category.RENDER,
   description = "Custom buttons for inventory screens like Expensive"
)
public final class InventoryButtons extends Module {
   public InventoryButtons() {}

   private static final byte BUTTON_SPACE = 20;
   private static final byte BUTTON_WIDTH = 100;
   private static final byte BUTTON_HEIGHT = 20;

   private static final Item[] TRASH_ITEMS = new Item[] {
      net.minecraft.item.Items.COBBLESTONE,
      net.minecraft.item.Items.DIRT,
      net.minecraft.item.Items.GRAVEL,
      net.minecraft.item.Items.SAND,
      net.minecraft.item.Items.GRANITE,
      net.minecraft.item.Items.DIORITE,
      net.minecraft.item.Items.ANDESITE,
      net.minecraft.item.Items.ROTTEN_FLESH,
      net.minecraft.item.Items.SPIDER_EYE,
      net.minecraft.item.Items.POISONOUS_POTATO,
      net.minecraft.item.Items.STICK,
      net.minecraft.item.Items.WHEAT_SEEDS,
      net.minecraft.item.Items.PUMPKIN_SEEDS,
      net.minecraft.item.Items.MELON_SEEDS,
      net.minecraft.item.Items.BEETROOT_SEEDS,
      net.minecraft.item.Items.DEAD_BUSH,
      net.minecraft.item.Items.KELP,
      net.minecraft.item.Items.SEAGRASS,
      net.minecraft.item.Items.BONE,
      net.minecraft.item.Items.STRING,
      net.minecraft.item.Items.GUNPOWDER,
      net.minecraft.item.Items.SLIME_BALL,
      net.minecraft.item.Items.EGG,
      net.minecraft.item.Items.FEATHER,
      net.minecraft.item.Items.LEATHER,
      net.minecraft.item.Items.RABBIT_HIDE,
      net.minecraft.item.Items.CLAY_BALL,
      net.minecraft.item.Items.BRICK,
      net.minecraft.item.Items.NETHER_BRICK,
      net.minecraft.item.Items.FLINT,
      net.minecraft.item.Items.SNOWBALL,
      net.minecraft.item.Items.SUGAR_CANE,
      net.minecraft.item.Items.PAPER,
      net.minecraft.item.Items.BOOK,
      net.minecraft.item.Items.INK_SAC,
      net.minecraft.item.Items.COCOA_BEANS,
      net.minecraft.item.Items.LILY_PAD,
      net.minecraft.item.Items.VINE,
      net.minecraft.item.Items.TALL_GRASS,
      net.minecraft.item.Items.FERN,
      net.minecraft.item.Items.LARGE_FERN,
      net.minecraft.item.Items.SUNFLOWER,
      net.minecraft.item.Items.LILAC,
      net.minecraft.item.Items.ROSE_BUSH,
      net.minecraft.item.Items.PEONY,
      net.minecraft.item.Items.POPPY,
      net.minecraft.item.Items.BLUE_ORCHID,
      net.minecraft.item.Items.ALLIUM,
      net.minecraft.item.Items.AZURE_BLUET,
      net.minecraft.item.Items.RED_TULIP,
      net.minecraft.item.Items.ORANGE_TULIP,
      net.minecraft.item.Items.WHITE_TULIP,
      net.minecraft.item.Items.PINK_TULIP,
      net.minecraft.item.Items.OXEYE_DAISY,
      net.minecraft.item.Items.CORNFLOWER,
      net.minecraft.item.Items.WITHER_ROSE,
      net.minecraft.item.Items.LILY_OF_THE_VALLEY,
      net.minecraft.item.Items.BROWN_MUSHROOM,
      net.minecraft.item.Items.RED_MUSHROOM,
      net.minecraft.item.Items.CACTUS,
      net.minecraft.item.Items.SUGAR,
      net.minecraft.item.Items.WHEAT,
      net.minecraft.item.Items.CARROT,
      net.minecraft.item.Items.POTATO,
      net.minecraft.item.Items.BEETROOT,
      net.minecraft.item.Items.PUMPKIN,
      net.minecraft.item.Items.MELON_SLICE,
      net.minecraft.item.Items.APPLE,
      net.minecraft.item.Items.BREAD,
      net.minecraft.item.Items.COOKIE,
      net.minecraft.item.Items.PUMPKIN_PIE,
      net.minecraft.item.Items.MUSHROOM_STEW,
      net.minecraft.item.Items.BEETROOT_SOUP,
      net.minecraft.item.Items.RABBIT_STEW,
      net.minecraft.item.Items.BAKED_POTATO,
      net.minecraft.item.Items.COOKED_CHICKEN,
      net.minecraft.item.Items.COOKED_BEEF,
      net.minecraft.item.Items.COOKED_PORKCHOP,
      net.minecraft.item.Items.COOKED_MUTTON,
      net.minecraft.item.Items.COOKED_RABBIT,
      net.minecraft.item.Items.COOKED_COD,
      net.minecraft.item.Items.COOKED_SALMON,
      net.minecraft.item.Items.DRIED_KELP
   };

   private float oldMouseX;
   private float oldMouseY;
   private boolean buttonClicked;

   @EventTarget
   public void onEventHandledScreen(EventHandledScreen event) {
      if (!this.isEnabled() || !(mc.currentScreen instanceof InventoryScreen)) {
         return;
      }

      DrawContext context = event.getDrawContext();
      CustomDrawContext customContext = CustomDrawContext.of(context);

      Slot hovered = event.getSlotHover();
      if (hovered != null && hovered.getStack() != null) {
         int mouseX = (int) mc.mouse.getX();
         int mouseY = (int) mc.mouse.getY();

         int screenWidth = mc.getWindow().getScaledWidth();
         int buttonY = (int)(mc.getWindow().getScaledHeight() * 0.75F - BUTTON_HEIGHT - 5F);

         customContext.drawRoundedRect(screenWidth - BUTTON_WIDTH - 10, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, BorderRadius.all(3.0F), ColorRGBA.BLUE.withAlpha(180));
         customContext.drawRoundedRect(screenWidth - BUTTON_WIDTH - 10, buttonY + BUTTON_HEIGHT + BUTTON_SPACE, BUTTON_WIDTH, BUTTON_HEIGHT, BorderRadius.all(3.0F), ColorRGBA.BLUE.withAlpha(180));
      }
   }

   @EventTarget
   public void onEventMouse(EventMouse event) {
      if (!this.isEnabled() || !(mc.currentScreen instanceof InventoryScreen)) {
         return;
      }

      if (event.getButton() == 0 && event.getAction() == 1) {
         InventoryScreen playerScreen = (InventoryScreen) mc.currentScreen;
         
         if (buttonClicked) {
            buttonClicked = false;
            return;
         }

         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         int buttonY = (int)(screenHeight * 0.75F - BUTTON_HEIGHT - 5F);
         int buttonX = screenWidth - BUTTON_WIDTH - 10;

         int mouseY = (int) mc.mouse.getY();
         int clickIndex = -1;

         int currentY = buttonY;
         for (int i = 0; i < 2; i++) {
            if (mouseY >= currentY && mouseY <= currentY + BUTTON_HEIGHT) {
               clickIndex = i;
               break;
            }
            currentY += BUTTON_HEIGHT + BUTTON_SPACE;
         }

         if (clickIndex != -1 && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT * 2 + BUTTON_SPACE) {
            buttonClicked = true;
            ItemStack cursorStack = mc.player.currentScreenHandler.getCursorStack();
            
            ScreenHandler handler = mc.player.currentScreenHandler;
            
            for (int i = 0; i < handler.slots.size() && mc.currentScreen == mc.currentScreen; ++i) {
               if (clickIndex == 0) {
                  mc.interactionManager.clickSlot(0, i, 0, SlotActionType.PICKUP, mc.player);
               } else {
                  ItemStack itemStack = handler.slots.get(i).getStack();
                  if (!itemStack.isEmpty() && isTrashItem(itemStack.getItem())) {
                     mc.interactionManager.clickSlot(0, i, 0, SlotActionType.PICKUP, mc.player);
                  }
               }
            }
            
            if (!cursorStack.isEmpty()) {
               mc.interactionManager.clickSlot(0, -999, 0, SlotActionType.PICKUP, mc.player);
            }
         }
      }
   }

   private boolean isTrashItem(Item item) {
      for (Item trashItem : TRASH_ITEMS) {
         if (trashItem == item) {
            return true;
         }
      }
      return false;
   }
}