package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "AutoMace",
   category = Category.COMBAT,
   description = "Автоматически бьет булавой и возвращает предмет"
)
public class AutoMace extends Module {
   public static final AutoMace INSTANCE = new AutoMace();
   private boolean isAttacking = false;

   private AutoMace() {
   }

   @EventTarget
   @Native
   private void onPacket(EventPacket event) {
      if (mc.player == null || mc.world == null || !event.isSent()) {
         return;
      }

      Packet<?> packet = event.getPacket();
      if (!(packet instanceof PlayerInteractEntityC2SPacket)) {
         return;
      }

      if (this.isAttacking) {
         return;
      }

      int maceSlot = this.findItemInHotbar(Items.MACE);
      if (maceSlot == -1) {
         return;
      }

      int currentSlot = mc.player.getInventory().selectedSlot;
      if (maceSlot == currentSlot) {
         return;
      }

      event.cancel();

      this.isAttacking = true;
      mc.player.getInventory().selectedSlot = maceSlot;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
      mc.getNetworkHandler().sendPacket(packet);
      mc.player.getInventory().selectedSlot = currentSlot;
      mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
      this.isAttacking = false;
   }

   private int findItemInHotbar(net.minecraft.item.Item item) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == item) {
            return i;
         }
      }
      return -1;
   }
}