package tech.huihui.client.screens.mainmenu;

import tech.huihui.utility.math.MathUtil;

public class AnimationUtilState {
   private float currentValue;
   private float previousValue;
   private float animationSpeed;
   private float animationValue;
   private float fromValue = 0.0F;
   private float toValue = 1.0F;
   private long lastUpdateTime = System.currentTimeMillis();

   public void set(float value) {
      this.currentValue = value;
   }

   public void setPrevious(float value) {
      this.previousValue = value;
   }

   public float getCurrentValue() {
      return this.currentValue;
   }

   public float getPreviousValue() {
      return this.previousValue;
   }

   public float getValue() {
      return this.animationValue;
   }

   public void setAnimationValue(float value) {
      this.animationValue = value;
   }

   public void expand(boolean expanding) {
      this.previousValue = this.currentValue;
      float direction = expanding ? 1.0F : -1.0F;
      float delta = (float) (System.currentTimeMillis() - this.lastUpdateTime) / 1000.0F;
      this.currentValue = clampToBounds(this.currentValue + (direction * this.animationSpeed * 20.0F * delta));
      this.lastUpdateTime = System.currentTimeMillis();
   }

   public void update(float fromValue, float toValue, float animationSpeed, float partialTicks) {
      this.animationSpeed = animationSpeed;
      this.fromValue = fromValue;
      this.toValue = toValue;
      this.animationValue = tech.huihui.utility.math.MathUtil.interpolate(previousValue, currentValue, partialTicks);
   }

   public void add(float amount) {
      this.toValue += amount;
   }

   public void setTarget(float value) {
      this.toValue = value;
      this.currentValue = value;
   }

   private float clampToBounds(float value) {
      return Math.max(this.fromValue, Math.min(this.toValue, value));
   }
}