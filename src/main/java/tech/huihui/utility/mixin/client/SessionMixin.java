package tech.huihui.utility.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tech.huihui.client.accounts.Account;
import tech.huihui.client.accounts.AccountManager;

@Mixin(Session.class)
public class SessionMixin {

   @ModifyReturnValue(method = {"getUsername"}, at = {@At("RETURN")})
   private String getUsername(String original) {
      Account selected = AccountManager.INSTANCE.getSelected();
      return selected != null ? selected.getName() : original;
   }
}