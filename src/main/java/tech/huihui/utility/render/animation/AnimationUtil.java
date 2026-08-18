package tech.huihui.utility.render.animation;

import net.minecraft.util.math.MathHelper;

public class AnimationUtil {
   private float currentValue;
   private float previousValue;
   private float animationSpeed;
   private float animationValue;
   private float fromValue = 0.0F;
   private float toValue = 1.0F;
   private long lastUpdateTime = System.currentTimeMillis();

   public void setCurrentValue(float value) {
      this.currentValue = value;
   }

   public void setPreviousValue(float prevValue) {
      this.previousValue = prevValue;
   }

   public float getCurrentValue() {
      return this.currentValue;
   }

   public float getPreviousValue() {
      return this.previousValue;
   }

   public float getAnimationValue() {
      return this.animationValue;
   }

   public void setAnimationValue(float animationValue) {
      this.animationValue = animationValue;
   }

   public void update(boolean expanding) {
      this.previousValue = this.currentValue;
      float direction = expanding ? 1.0F : -1.0F;
      this.currentValue = MathHelper.clamp(this.currentValue + direction * this.animationSpeed * 20.0F * this.delta(), this.fromValue, this.toValue);
   }

   public void update(float fromValue, float toValue, float animationSpeed, float partialTicks) {
      this.animationSpeed = animationSpeed;
      this.fromValue = fromValue;
      this.toValue = toValue;
      this.animationValue = this.previousValue + (this.currentValue - this.previousValue) * partialTicks;
   }

   public void addToValue(float amount) {
      this.toValue += amount;
   }

   public void setValue(float value) {
      this.toValue = value;
      this.currentValue = value;
   }

   public float move(float min, float max, float speed) {
      this.toValue = MathHelper.clamp(this.toValue, min, max);
      float delta = Math.max(0.0F, (float)(System.currentTimeMillis() - this.lastUpdateTime) / 1000.0F);
      this.currentValue = this.currentValue + (this.toValue - this.currentValue) * (1.0F - (float)Math.exp((double)(-speed * delta)));
      this.lastUpdateTime = System.currentTimeMillis();
      return this.currentValue;
   }

   public float moveTo(float target, float speed) {
      return this.move(target, target, speed);
   }

   private float delta() {
      long now = System.currentTimeMillis();
      float delta = (float)(now - this.lastUpdateTime) / 1000.0F;
      this.lastUpdateTime = now;
      return delta;
   }
}
