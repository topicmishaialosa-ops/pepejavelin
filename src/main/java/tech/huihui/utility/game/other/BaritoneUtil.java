package tech.huihui.utility.game.other;

import java.lang.reflect.Method;

public final class BaritoneUtil {
   private static boolean checked;
   private static boolean present;
   private static Method getProviderMethod;
   private static Method getPrimaryBaritoneMethod;
   private static Method getPathingBehaviorMethod;
   private static Method isPathingMethod;
   private static Method getMineProcessMethod;
   private static Method mineByNameMethod;
   private static Method cancelMethod;
   private static Method isActiveMethod;

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

   private static Object getMineProcess() {
      if (!isPresent()) {
         return null;
      }

      try {
         if (getMineProcessMethod == null) {
            Class<?> iBaritone = Class.forName("baritone.api.IBaritone");
            getMineProcessMethod = iBaritone.getMethod("getMineProcess");
         }

         Object baritone = getPrimaryBaritoneMethod.invoke(getProviderMethod.invoke(null));
         if (baritone == null) {
            return null;
         }

         return getMineProcessMethod.invoke(baritone);
      } catch (Throwable throwable) {
         return null;
      }
   }

   public static boolean isMining() {
      Object process = getMineProcess();
      if (process == null) {
         return false;
      }

      try {
         if (isActiveMethod == null) {
            isActiveMethod = Class.forName("baritone.api.process.IBaritoneProcess").getMethod("isActive");
         }

         return (Boolean)isActiveMethod.invoke(process);
      } catch (Throwable throwable) {
         return false;
      }
   }

   public static void startMining(int quantity, String... blockNames) {
      Object process = getMineProcess();
      if (process == null || blockNames.length == 0) {
         return;
      }

      try {
         if (mineByNameMethod == null) {
            mineByNameMethod = Class.forName("baritone.api.process.IMineProcess").getMethod("mineByName", Integer.TYPE, String[].class);
         }

         mineByNameMethod.invoke(process, quantity, (Object)blockNames);
      } catch (Throwable throwable) {
      }
   }

   public static void stopMining() {
      Object process = getMineProcess();
      if (process == null) {
         return;
      }

      try {
         if (cancelMethod == null) {
            cancelMethod = Class.forName("baritone.api.process.IMineProcess").getMethod("cancel");
         }

         cancelMethod.invoke(process);
      } catch (Throwable throwable) {
      }
   }
}
