package tech.huihui.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.utility.game.player.MovingUtil;

@ModuleAnnotation(
   name = "Jesus",
   category = Category.MOVEMENT,
   description = "Позволяет ходить по воде"
)
public class Jesus extends Module {
   public static final Jesus INSTANCE = new Jesus();
   private final ModeSetting mode = new ModeSetting("Режим", "Metahvh");
   private final float melonBallSpeed = 0.44F;

   @EventTarget
   @Native
   private void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
            ItemStack offHandItem = mc.player.getOffHandStack();
            String itemName = offHandItem.getName().getString();
            float appliedSpeed;

            if (itemName.contains("Ломтик Дыни") && speedEffect != null && speedEffect.getAmplifier() == 2) {
               appliedSpeed = 0.4283F * 1.15F;
            } else if (speedEffect != null) {
               if (speedEffect.getAmplifier() == 2) {
                  appliedSpeed = this.melonBallSpeed * 1.15F;
               } else if (speedEffect.getAmplifier() == 1) {
                  appliedSpeed = this.melonBallSpeed;
               } else {
                  appliedSpeed = this.melonBallSpeed * 0.68F;
               }
            } else {
               appliedSpeed = this.melonBallSpeed * 0.68F;
            }

            if (slowEffect != null) {
               appliedSpeed *= 0.85F;
            }

            if (this.mode.is("Metahvh")) {
               MovingUtil.setVelocity((double)appliedSpeed);
            }

            boolean isMoving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
            Vec3d velocity = mc.player.getVelocity();
            if (!isMoving) {
               mc.player.setVelocity(0.0D, velocity.y, 0.0D);
            }
            mc.player.setVelocity(mc.player.getVelocity().x, mc.options.jumpKey.isPressed() ? 0.019D : 0.003D, mc.player.getVelocity().z);
         }

      }
   }
}
