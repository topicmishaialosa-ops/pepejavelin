package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.KeySetting;

@ModuleAnnotation(
   name = "ClickFriend",
   description = "Добавляет друга по бинду",
   category = Category.MISC
)
public final class ClickAction extends Module {
   private final KeySetting friendBind = new KeySetting("Добавить друга");
   public static final ClickAction INSTANCE = new ClickAction();

   @EventTarget
   @Native
   public void onKey(EventKey e) {
      if (e.isKeyDown(this.friendBind.getKeyCode())) {
         HitResult var4 = mc.crosshairTarget;
         if (var4 instanceof EntityHitResult) {
            EntityHitResult result = (EntityHitResult)var4;
            Entity var5 = result.getEntity();
            if (var5 instanceof PlayerEntity) {
               PlayerEntity player = (PlayerEntity)var5;
               if (HuihuiClient.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())) {
                  HuihuiClient.getInstance().getFriendManager().removeFriend(player.getGameProfile().getName());
               } else {
                  HuihuiClient.getInstance().getFriendManager().add(player.getGameProfile().getName());
               }
            }
         }
      }

   }
}
