package tech.huihui.client.modules.api.accessor;

import tech.huihui.client.modules.api.setting.impl.ModeSetting;

public interface MenuAccessor {
   String[] getModes();

   ModeSetting getMode();
}
