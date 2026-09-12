package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.other.EventGameUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.screens.bots.InstanceBotScreen;

@ModuleAnnotation(
   name = "InstanceBot",
   category = Category.MISC,
   description = "Окно управления ботами"
)
public final class InstanceBot extends Module {
   public static final InstanceBot INSTANCE = new InstanceBot();

   private boolean deferred;

   private InstanceBot() {
   }

   @Override
   public void onEnable() {
      if (mc.getWindow() != null) {
         mc.setScreen(new InstanceBotScreen());
      } else {
         this.deferred = true;
      }
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.deferred = false;
      super.onDisable();
   }

   @EventTarget
   public void onUpdate(EventGameUpdate event) {
      if (this.deferred && mc.getWindow() != null) {
         this.deferred = false;
         mc.setScreen(new InstanceBotScreen());
      }
   }
}