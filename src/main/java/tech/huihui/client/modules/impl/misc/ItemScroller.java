package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.input.EventMouse;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "ItemScroller",
   description = "Перемещение преметов без задержки",
   category = Category.MISC
)
public final class ItemScroller extends Module {
   public static final ItemScroller INSTANCE = new ItemScroller();
   public boolean mouseHold;

   @EventTarget
   private void onMouse(EventMouse e) {
      if (e.getButton() == 0 && e.getAction() == 1) {
         this.mouseHold = true;
      }

      if (this.mouseHold && e.getAction() == 0) {
         this.mouseHold = false;
      }

   }
}
