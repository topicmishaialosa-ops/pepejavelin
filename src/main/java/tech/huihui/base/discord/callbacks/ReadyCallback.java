package tech.huihui.base.discord.callbacks;

import com.sun.jna.Callback;
import tech.huihui.base.discord.utils.DiscordUser;

public interface ReadyCallback extends Callback {
   void apply(DiscordUser var1);
}
