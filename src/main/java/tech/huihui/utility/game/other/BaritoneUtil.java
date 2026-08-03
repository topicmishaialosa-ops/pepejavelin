package tech.huihui.utility.game.other;

public final class BaritoneUtil {
   private static boolean checked;
   private static boolean present;

   private BaritoneUtil() {
   }

   public static boolean isPresent() {
      if (!checked) {
         checked = true;

         try {
            Class.forName("baritone.api.BaritoneAPI");
            present = true;
         } catch (Throwable ignored) {
            present = false;
         }
      }

      return present;
   }
}
