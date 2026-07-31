package tech.huihui.client.modules.impl.misc;

import tech.huihui.HuihuiClient;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
   name = "NoInteract",
   category = Category.MISC,
   description = "Не дает открыть контейнера"
)
public final class NoInteract extends Module {
   private final BooleanSetting onlyOnPvP = new BooleanSetting("Только в PvP", false);
   public static final NoInteract INSTANCE = new NoInteract();

   public boolean needToWork() {
      return !this.onlyOnPvP.isEnabled() || HuihuiClient.getInstance().getServerHandler().isPvp();
   }
}
