package tech.huihui.utility.render;

import net.fabricmc.loader.api.FabricLoader;
import tech.huihui.client.modules.impl.misc.RenamePasterClient;

public final class VulkanCompatibility {
   private static final boolean VULKAN_MOD_LOADED;

   public static boolean isVulkanModLoaded() {
      return VULKAN_MOD_LOADED;
   }

   static {
      boolean loaded = false;

      try {
         loaded = FabricLoader.getInstance().isModLoaded("vulkanmod");
      } catch (Throwable var2) {
      }

      VULKAN_MOD_LOADED = loaded;
      if (loaded) {
         System.out.println("[" + RenamePasterClient.getClientName() + "] VulkanMod detected. Custom GLSL shaders and raw OpenGL calls are disabled.");
      }
   }

   private VulkanCompatibility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}