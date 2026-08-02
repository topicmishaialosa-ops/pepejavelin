package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.item.Items;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.render.display.Keyboard;

@ModuleAnnotation(
   name = "ClickPearl",
   category = Category.COMBAT,
   description = "Кидает перку по бинду"
)
public final class ClickPearl extends Module {
   public static final ClickPearl INSTANCE = new ClickPearl();
   private final ModeSetting mode = new ModeSetting("Мод", new String[]{"Хвх", "Легит"});
   private final KeySetting keyBind = new KeySetting("Бинд", Keyboard.MOUSE_3.keyCode);

   @EventTarget
   @Native
   private void onKey(EventKey e) {
      if (e.isKeyDown(this.keyBind.getKeyCode())) {
         if (this.mode.is("Хвх")) {
            PlayerInventoryUtil.swapAndUseHvH(Items.ENDER_PEARL);
         } else {
            PlayerInventoryUtil.swapAndUseLegit(Items.ENDER_PEARL);
         }
      }
   }
}
