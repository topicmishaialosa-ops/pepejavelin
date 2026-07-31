package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import java.util.Locale;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "AutoKit",
   category = Category.MISC,
   description = "Берет киты, складывает выбранные предметы в /ec и чистит инвентарь."
)
public final class AutoKit extends Module {
   private static final String[] KIT_COMMANDS = new String[]{
      "/kit free",
      "/kit hero",
      "/kit prince",
      "/kit killer",
      "/kit krushitel",
      "/kit kraken",
      "/kit alpha",
      "/kit rabbit",
      "/kit sponsor",
      "/kit pumpkin"
   };
   private static final String[] KIT_NAMES = new String[]{
      "free", "hero", "prince", "killer", "krushitel", "kraken", "alpha", "rabbit", "sponsor", "pumpkin"
   };
   private final MultiBooleanSetting kitsMode = MultiBooleanSetting.create("Киты", List.of(KIT_NAMES));
   private final MultiBooleanSetting itemMode = MultiBooleanSetting.create("Предметы в /ec", List.of("Зелье Викинга", "Чарка", "Ангел Хранитель"));
   private final NumberSetting delayMs = new NumberSetting("Задержка (мс)", 700.0F, 250.0F, 3000.0F, 50.0F);
   private final Timer timer = new Timer();
   private int kitIndex;
   private Stage stage = Stage.KITS;
   private int ecReopenAttempts;
   private boolean firstTick;

   public static final AutoKit INSTANCE = new AutoKit();

   private enum Stage {
      KITS,
      OPEN_EC,
      MOVE_ITEMS_TO_EC,
      CI_1,
      CI_2,
      DONE
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.kitIndex = 0;
      this.stage = Stage.KITS;
      this.ecReopenAttempts = 0;
      this.firstTick = true;
      this.timer.reset();
   }

   @EventTarget
   private void onUpdate(EventUpdate e) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (this.firstTick) {
         this.firstTick = false;
         this.timer.setMillis(System.currentTimeMillis() - (long)this.delayMs.getCurrent());
      }

      if (!this.timer.finished((long)this.delayMs.getCurrent())) {
         return;
      }
      this.timer.reset();

      switch (this.stage) {
         case KITS:
            String nextKitCommand = this.getNextSelectedKitCommand();
            if (nextKitCommand != null) {
               mc.getNetworkHandler().sendChatMessage(nextKitCommand);
               this.stage = Stage.OPEN_EC;
               this.ecReopenAttempts = 0;
            } else {
               this.stage = Stage.DONE;
            }
            break;

         case OPEN_EC:
            mc.getNetworkHandler().sendChatMessage("/ec");
            this.stage = Stage.MOVE_ITEMS_TO_EC;
            break;

         case MOVE_ITEMS_TO_EC:
            if (mc.player.currentScreenHandler.syncId == 0) {
               if (this.ecReopenAttempts++ < 3) {
                  mc.getNetworkHandler().sendChatMessage("/ec");
               } else {
                  this.stage = Stage.CI_1;
               }
               break;
            }

            this.moveSelectedItemsToEc();
            PlayerInventoryUtil.closeScreen(true);
            this.stage = Stage.CI_1;
            break;

         case CI_1:
            mc.getNetworkHandler().sendChatMessage("/ci");
            this.stage = Stage.CI_2;
            break;

         case CI_2:
            mc.getNetworkHandler().sendChatMessage("/ci");
            this.stage = Stage.KITS;
            break;

         case DONE:
            this.toggle();
            break;
      }
   }

   private void moveSelectedItemsToEc() {
      for (Slot slot : mc.player.currentScreenHandler.slots) {
         if (slot == null || slot.inventory != mc.player.getInventory() || !slot.hasStack()) {
            continue;
         }
         ItemStack stack = slot.getStack();
         if (stack.isEmpty()) {
            continue;
         }

         String name = stack.getName().getString().toLowerCase(Locale.ROOT);
         if (!this.shouldMoveByMode(stack, name)) {
            continue;
         }

         PlayerInventoryUtil.clickSlot(slot.id, 0, SlotActionType.QUICK_MOVE, false);
      }
   }

   private boolean shouldMoveByMode(ItemStack stack, String lowerName) {
      boolean isViking = lowerName.contains("зелье викинга");
      boolean isChar = stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE;
      boolean isTotem = lowerName.contains("ангел хранитель");

      boolean pickViking = this.itemMode.isEnable("Зелье Викинга");
      boolean pickChar = this.itemMode.isEnable("Чарка");
      boolean pickTotem = this.itemMode.isEnable("Ангел Хранитель");

      if (pickViking && isViking) {
         return true;
      }
      if (pickChar && isChar) {
         return true;
      }
      return pickTotem && isTotem;
   }

   private String getNextSelectedKitCommand() {
      while (this.kitIndex < KIT_COMMANDS.length) {
         int idx = this.kitIndex++;
         if (this.kitsMode.isEnable(KIT_NAMES[idx])) {
            return KIT_COMMANDS[idx];
         }
      }
      return null;
   }
}
