package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "Fly",
   category = Category.MOVEMENT,
   description = "Ванильный полёт без необходимости креатива"
)
public final class Fly extends Module {
   public static final Fly INSTANCE = new Fly();
   public final NumberSetting speed = new NumberSetting("Скорость", 0.05F, 0.01F, 1.0F, 0.01F);

   private Fly() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.applyFlight(true);
   }

   @Override
   public void onDisable() {
      this.applyFlight(false);
      super.onDisable();
   }

   @EventTarget
   public void onUpdate(EventTick event) {
      if (mc.player == null) {
         return;
      }
      this.applyFlight(true);
   }

   private void applyFlight(boolean flying) {
      if (mc.player == null) {
         return;
      }
      mc.player.getAbilities().allowFlying = flying;
      mc.player.getAbilities().flying = flying;
      mc.player.getAbilities().setFlySpeed(flying ? this.speed.getCurrent() : 0.05F);
   }
}
