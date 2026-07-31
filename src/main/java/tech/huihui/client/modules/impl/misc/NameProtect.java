package tech.huihui.client.modules.impl.misc;

import java.util.Collection;
import java.util.Iterator;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
@ModuleAnnotation(
   name = "NameProtect",
   category = Category.MISC,
   description = "Защищает имена игроков"
)
public final class NameProtect extends Module {
   public static final NameProtect INSTANCE = new NameProtect();
   private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);
   private final StringSetting customName = new StringSetting("Ник", "JAVELIN");
   private NameProtect() {
   }
   @Native
   public static String getCustomName() {
      Module module = INSTANCE;
      return module != null && module.isEnabled() ? INSTANCE.customName.getValue() : mc.player.getNameForScoreboard();
   }
   @Native
   public static String getCustomName(String originalName) {
      Module module = INSTANCE;
      if (module != null && module.isEnabled() && mc.player != null) {
         String me = mc.player.getNameForScoreboard();
         String replacement = INSTANCE.customName.getValue();
         if (originalName.contains(me)) {
            return originalName.replace(me, replacement);
         } else {
            if (module instanceof NameProtect) {
               NameProtect nameProtect = (NameProtect)module;
               if (nameProtect.hideFriends.isEnabled()) {
                  Collection<String> friends = HuihuiClient.getInstance().getFriendManager().getItems();
                  Iterator var5 = friends.iterator();
                  while(var5.hasNext()) {
                     String friend = (String)var5.next();
                     if (originalName.contains(friend)) {
                        return originalName.replace(friend, replacement);
                     }
                  }
               }
            }
            return originalName;
         }
      } else {
         return originalName;
      }
   }
}
