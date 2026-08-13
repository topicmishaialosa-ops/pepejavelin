package tech.huihui.client.modules.impl.misc;

import lombok.Generated;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.StringSetting;

@ModuleAnnotation(
   name = "RenamePasterClient",
   category = Category.MISC,
   description = "Замена отображаемого названия клиента на своё"
)
public final class RenamePasterClient extends Module {
   public static final RenamePasterClient INSTANCE = new RenamePasterClient();
   private static final String DEFAULT_NAME = "Huihui Client";
   private final StringSetting clientName = new StringSetting("Название клиента", DEFAULT_NAME);

   private RenamePasterClient() {
   }

   public static String getClientName() {
      if (!INSTANCE.isEnabled()) {
         return DEFAULT_NAME;
      }
      String custom = INSTANCE.clientName.getValue();
      return custom != null && !custom.isBlank() ? custom.trim() : DEFAULT_NAME;
   }

   public static String getClientNameLower() {
      return getClientName().toLowerCase();
   }

   @Generated
   public StringSetting getClientNameSetting() {
      return this.clientName;
   }
}