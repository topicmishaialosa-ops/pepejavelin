package tech.huihui.client.screens.targethud;

import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import tech.huihui.base.filemanager.api.ManagerFileAbstract;

public class TargetHudPresetManager extends ManagerFileAbstract<TargetHudPreset> {
   public TargetHudPresetManager() {
      super("targethud_presets.json", "", (new TypeToken<List<TargetHudPreset>>() {
      }).getType(), ArrayList::new);
   }

   public TargetHudPreset findByName(String name) {
      for (TargetHudPreset preset : this.getItems()) {
         if (preset.getName().equals(name)) {
            return preset;
         }
      }
      return null;
   }

   public void savePreset(TargetHudPreset preset) {
      TargetHudPreset existing = this.findByName(preset.getName());
      if (existing != null) {
         this.getItems().remove(existing);
      }
      this.getItems().add(preset);
      this.save();
   }

   public boolean deletePreset(String name) {
      TargetHudPreset existing = this.findByName(name);
      if (existing != null) {
         this.getItems().remove(existing);
         this.save();
         return true;
      }
      return false;
   }

   public List<TargetHudPreset> getPresets() {
      return new ArrayList<>(this.getItems());
   }
}
