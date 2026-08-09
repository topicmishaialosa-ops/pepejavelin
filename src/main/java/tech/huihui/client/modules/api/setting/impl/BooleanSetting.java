package tech.huihui.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import java.util.function.Supplier;
import lombok.Generated;
import tech.huihui.base.animations.base.Animation;
import tech.huihui.base.animations.base.Easing;
import tech.huihui.client.modules.api.setting.Setting;

public class BooleanSetting extends Setting {
   private boolean enabled;
   private final String description;
   private final Animation animation;
   private Runnable onToggle;

   public BooleanSetting(String name, boolean state) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.description = "";
   }

   public BooleanSetting(String name, String description, boolean state) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.description = description;
   }

   public BooleanSetting(String name, String description, boolean state, Supplier<Boolean> supplier) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.setVisible(supplier);
      this.description = description;
   }

   public BooleanSetting(String name, boolean state, Supplier<Boolean> visible) {
      super(name);
      this.animation = new Animation(250L, Easing.CUBIC_OUT);
      this.enabled = state;
      this.setVisible(visible);
      this.description = "";
   }

   public static BooleanSetting of(String name, boolean state) {
      return new BooleanSetting(name, state);
   }

   public static BooleanSetting of(String name) {
      return new BooleanSetting(name, true);
   }

   public void toggle() {
      this.enabled = !this.enabled;
      this.fireOnToggle();
   }

   public void safe(JsonObject propertiesObject) {
      propertiesObject.addProperty(String.valueOf(this.name), this.isEnabled());
   }

   public void load(JsonObject propertiesObject) {
      this.setEnabled(propertiesObject.get(String.valueOf(this.name)).getAsBoolean());
   }

   @Generated
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      this.fireOnToggle();
   }

   public void setOnToggle(Runnable onToggle) {
      this.onToggle = onToggle;
   }

   private void fireOnToggle() {
      if (this.onToggle != null) {
         this.onToggle.run();
      }
   }

   @Generated
   public String getDescription() {
      return this.description;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }
}
