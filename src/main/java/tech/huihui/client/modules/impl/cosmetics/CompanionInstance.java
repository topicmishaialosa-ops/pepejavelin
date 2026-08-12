package tech.huihui.client.modules.impl.cosmetics;

public final class CompanionInstance {
   public final Companion companion;
   public double x;
   public double y;
   public double z;
   public double prevX;
   public double prevY;
   public double prevZ;
   public float yaw;
   public int age;
   public float walkDist;
   public float walkAmount;
   public float animTime;
   public double velX;
   public double velZ;
   public double targetX;
   public double targetZ;
   public float thinkTimer;
   public float idleTimer;
   public boolean following;

   public CompanionInstance(Companion companion, double x, double y, double z) {
      this.companion = companion;
      this.x = x;
      this.y = y;
      this.z = z;
      this.prevX = x;
      this.prevY = y;
      this.prevZ = z;
      this.targetX = x;
      this.targetZ = z;
   }
}
