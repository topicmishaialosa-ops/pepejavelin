package tech.huihui.base.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import lombok.Generated;
import tech.huihui.HuihuiClient;
import tech.huihui.base.theme.Theme;
import tech.huihui.base.theme.ThemeManager;
import tech.huihui.client.modules.api.Module;

public class Config {
   private final String name;
   private final File file;

   public Config(String name) {
      this.name = name;
      this.file = new File(ConfigManager.configDirectory, name + "." + "huihui");
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException var3) {
            var3.printStackTrace();
         }
      }

   }

   public JsonObject save() {
      try {
         JsonObject root = new JsonObject();
         JsonObject modulesObject = new JsonObject();
         Iterator var3 = HuihuiClient.getInstance().getModuleManager().getModules().iterator();

         while(var3.hasNext()) {
            Module module = (Module)var3.next();
            modulesObject.add(module.getName(), module.save());
         }

         root.add("Modules", modulesObject);
         ThemeManager themeManager = HuihuiClient.getInstance().getThemeManager();
         JsonObject themeObject = new JsonObject();
         themeObject.addProperty("selected", themeManager.getCurrentTheme().getName());
         JsonArray themesArray = new JsonArray();
         Iterator var4 = themeManager.getThemes().iterator();

         while(var4.hasNext()) {
            Theme t = (Theme)var4.next();
            JsonObject themeEntry = new JsonObject();
            themeEntry.addProperty("name", t.getName());
            themeEntry.addProperty("color1", t.getColor1());
            themeEntry.addProperty("color2", t.getColor2());
            themeEntry.addProperty("defaultColor1", t.getDefaultColor1());
            themeEntry.addProperty("defaultColor2", t.getDefaultColor2());
            themeEntry.addProperty("preset", t.isPreset());
            themesArray.add(themeEntry);
         }

         themeObject.add("themes", themesArray);
         root.add("Theme", themeObject);
         return root;
      } catch (Exception var5) {
         var5.printStackTrace();
         return null;
      }
   }

   public void load(JsonObject object) {
      JsonObject modulesObject;
      if (object.has("Theme")) {
         modulesObject = object.getAsJsonObject("Theme");
         ThemeManager themeManager = HuihuiClient.getInstance().getThemeManager();
         if (modulesObject.has("themes")) {
            try {
               JsonArray themesArray = modulesObject.getAsJsonArray("themes");
               if (themesArray.size() > 0) {
                  themeManager.getThemes().clear();
                  Iterator var5 = themesArray.iterator();

                  while(var5.hasNext()) {
                     JsonElement element = (JsonElement)var5.next();
                     JsonObject themeEntry = element.getAsJsonObject();
                     String name = themeEntry.get("name").getAsString();
                     int color1 = themeEntry.get("color1").getAsInt();
                     int color2 = themeEntry.get("color2").getAsInt();
                     int defaultColor1 = themeEntry.has("defaultColor1") ? themeEntry.get("defaultColor1").getAsInt() : color1;
                     int defaultColor2 = themeEntry.has("defaultColor2") ? themeEntry.get("defaultColor2").getAsInt() : color2;
                     boolean preset = themeEntry.has("preset") && themeEntry.get("preset").getAsBoolean();
                     Theme t = new Theme(name, color1, color2, false);
                     t.setDefaultColor1(defaultColor1);
                     t.setDefaultColor2(defaultColor2);
                     t.setPreset(preset);
                     themeManager.addTheme(t);
                  }
               }
            } catch (Exception var12) {
               var12.printStackTrace();
            }
         }
         if (modulesObject.has("selected")) {
            String selected = modulesObject.get("selected").getAsString();
            Iterator var4 = themeManager.getThemes().iterator();

            while(var4.hasNext()) {
               Theme t = (Theme)var4.next();
               if (t.getName().equalsIgnoreCase(selected)) {
                  themeManager.setCurrentTheme(t);
                  break;
               }
            }
         }
      }

      if (object.has("Modules")) {
         try {
            modulesObject = object.getAsJsonObject("Modules");
            Iterator var6 = HuihuiClient.getInstance().getModuleManager().getModules().iterator();

            while(var6.hasNext()) {
               Module module = (Module)var6.next();
               module.load(modulesObject.getAsJsonObject(module.getName()));
            }
         } catch (Exception var5) {
            var5.printStackTrace();
         }
      }

   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public File getFile() {
      return this.file;
   }
}
