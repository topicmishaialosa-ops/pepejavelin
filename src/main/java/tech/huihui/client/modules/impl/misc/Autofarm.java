package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.utility.game.other.BaritoneUtil;
import tech.huihui.utility.game.other.MessageUtil;

@ModuleAnnotation(
   name = "Autofarm",
   category = Category.MISC,
   description = "Запускает сбор урожая через Baritone (#farm)"
)
public final class Autofarm extends Module {
   public static final Autofarm INSTANCE = new Autofarm();
   private boolean farmStarted;
   private boolean baritoneMissing;

   private Autofarm() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (!BaritoneUtil.isPresent()) {
         MessageUtil.displayInfo("Скачайте Baritone, чтобы использовать Autofarm");
         this.baritoneMissing = true;
         return;
      }
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatMessage("#farm");
         this.farmStarted = true;
      }
   }

   @Override
   public void onDisable() {
      if (this.farmStarted && mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.sendChatMessage("#stop");
      }
      this.farmStarted = false;
      super.onDisable();
   }

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (this.baritoneMissing) {
         this.baritoneMissing = false;
         this.setToggled(false);
      }
   }
}
