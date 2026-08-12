package tech.huihui.utility.game.other;

import java.lang.reflect.Method;

public final class BaritoneUtil {
   private static boolean checked;
   private static boolean present;
   private static Method getProviderMethod;
   private static Method getPrimaryBaritoneMethod;
   private static Method getPathingBehaviorMethod;
   private static Method isPathingMethod;

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

   public static boolean isPathing() {
      if (!isPresent()) {
         return false;
      }

      try {
         if (getProviderMethod == null) {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            getProviderMethod = api.getMethod("getProvider");
            Class<?> provider = Class.forName("baritone.api.IBaritoneProvider");
            getPrimaryBaritoneMethod = provider.getMethod("getPrimaryBaritone");
            Class<?> iBaritone = Class.forName("baritone.api.IBaritone");
            getPathingBehaviorMethod = iBaritone.getMethod("getPathingBehavior");
            Class<?> pathingBehavior = Class.forName("baritone.api.behavior.IPathingBehavior");
            isPathingMethod = pathingBehavior.getMethod("isPathing");
         }

         Object baritone = getPrimaryBaritoneMethod.invoke(getProviderMethod.invoke(null));
         if (baritone == null) {
            return false;
         }

         return (Boolean)isPathingMethod.invoke(getPathingBehaviorMethod.invoke(baritone));
      } catch (Throwable throwable) {
         return false;
      }
   }
}
