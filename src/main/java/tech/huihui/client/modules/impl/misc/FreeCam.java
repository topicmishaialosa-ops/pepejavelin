package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Objects;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.other.EventGameUpdate;
import tech.huihui.base.events.impl.player.EventLook;
import tech.huihui.base.events.impl.player.EventMove;
import tech.huihui.base.events.impl.player.EventMoveInput;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.utility.game.other.BaritoneUtil;
import tech.huihui.utility.game.player.MovingUtil;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "FreeCam",
   description = "Обзор местности за фейк игрока",
   category = Category.MISC
)
public final class FreeCam extends Module {
   public static final FreeCam INSTANCE = new FreeCam();
   public Vec3d pos;
   public Vec3d prevPos;
   private boolean cameraMode;
   private Vec3d camPos;
   private Vec3d camVel = Vec3d.ZERO;
   private float camYaw;
   private float camPitch;
   private float lookYawDelta;
   private float lookPitchDelta;
   private long lastFrameNanos;

   private FreeCam() {
   }

   public void onEnable() {
      super.onEnable();
      if (mc.player != null) {
         this.pos = mc.player.getPos();
      }

      this.cameraMode = BaritoneUtil.isPathing();
      if (this.cameraMode) {
         this.initCamera();
      }
   }

   public void onDisable() {
      super.onDisable();
      if (mc.player != null && !this.cameraMode) {
         mc.player.setPosition(this.pos);
      }

      this.cameraMode = false;
   }

   private void initCamera() {
      if (mc.player == null) {
         return;
      }

      this.camPos = mc.player.getPos();
      this.camVel = Vec3d.ZERO;
      this.camYaw = mc.player.getYaw();
      this.camPitch = mc.player.getPitch();
      this.lookYawDelta = 0.0F;
      this.lookPitchDelta = 0.0F;
      this.lastFrameNanos = System.nanoTime();
   }

   public boolean isCameraMode() {
      return this.cameraMode && this.camPos != null;
   }

   public Vec3d getCamPos() {
      return this.camPos;
   }

   public float getCamYaw() {
      return this.camYaw;
   }

   public float getCamPitch() {
      return this.camPitch;
   }

   public void tickCamera() {
      if (mc.player == null) {
         return;
      }

      long now = System.nanoTime();
      float dt = (float)Math.min((double)(now - this.lastFrameNanos) / 1.0E9D, 0.1D);
      this.lastFrameNanos = now;
      this.camYaw += this.lookYawDelta;
      this.camPitch = MathHelper.clamp(this.camPitch + this.lookPitchDelta, -90.0F, 90.0F);
      this.lookYawDelta = 0.0F;
      this.lookPitchDelta = 0.0F;
      float forward = (mc.options.forwardKey.isPressed() ? 1.0F : 0.0F) - (mc.options.backKey.isPressed() ? 1.0F : 0.0F);
      float strafe = (mc.options.rightKey.isPressed() ? 1.0F : 0.0F) - (mc.options.leftKey.isPressed() ? 1.0F : 0.0F);
      float up = (mc.options.jumpKey.isPressed() ? 1.0F : 0.0F) - (mc.options.sneakKey.isPressed() ? 1.0F : 0.0F);
      double yawRad = Math.toRadians((double)this.camYaw);
      double dirX = -Math.sin(yawRad) * (double)forward + Math.cos(yawRad) * (double)strafe;
      double dirZ = Math.cos(yawRad) * (double)forward + Math.sin(yawRad) * (double)strafe;
      double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
      if (len > 1.0E-4D) {
         dirX /= len;
         dirZ /= len;
      }

      float speed = mc.options.sprintKey.isPressed() ? 25.0F : 10.0F;
      double targetX = dirX * (double)speed;
      double targetZ = dirZ * (double)speed;
      double targetY = (double)up * (double)speed;
      float factor = Math.min(1.0F, 8.0F * dt);
      double vx = this.camVel.x + (targetX - this.camVel.x) * (double)factor;
      double vy = this.camVel.y + (targetY - this.camVel.y) * (double)factor;
      double vz = this.camVel.z + (targetZ - this.camVel.z) * (double)factor;
      this.camVel = new Vec3d(vx, vy, vz);
      this.camPos = this.camPos.add(this.camVel.x * (double)dt, this.camVel.y * (double)dt, this.camVel.z * (double)dt);
   }

   @EventTarget
   private void onGameUpdate(EventGameUpdate e) {
      boolean baritoneBusy = BaritoneUtil.isPathing();
      if (this.cameraMode != baritoneBusy) {
         this.cameraMode = baritoneBusy;
         if (baritoneBusy) {
            this.initCamera();
         }
      }
   }

   @EventTarget
   private void onLook(EventLook e) {
      if (this.isCameraMode()) {
         this.lookYawDelta += (float)e.getYaw();
         this.lookPitchDelta += (float)e.getPitch();
         e.cancel();
      }
   }

   @EventTarget
   @Native
   public void onPacket(EventPacket e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof PlayerMoveC2SPacket) {
         if (!this.isCameraMode()) {
            e.cancel();
         }
      } else if (packet instanceof PlayerRespawnS2CPacket) {
         this.toggle();
      } else if (packet instanceof GameJoinS2CPacket) {
         this.toggle();
      }
   }

   @EventTarget
   public void onWorldRender(EventRender3D e) {
      if (this.pos != null) {
         Vec3d boxPos = this.isCameraMode() ? mc.player.getPos() : this.pos;
         Render3DUtil.drawBox(new Box(boxPos.x - (double)(mc.player.getWidth() / 2.0F), boxPos.y, boxPos.z - (double)(mc.player.getWidth() / 2.0F), boxPos.x + (double)(mc.player.getWidth() / 2.0F), boxPos.y + (double)mc.player.getHeight(), boxPos.z + (double)(mc.player.getWidth() / 2.0F)), JAVELIN.getThemeManager().getClientColor(90).getRGB(), 1.0F);
      }
   }

   @EventTarget
   public void onMove(EventMove e) {
      if (this.isCameraMode()) {
         return;
      }

      mc.player.noClip = true;
      double[] motion = MovingUtil.calculateDirection(1.0119999647140503D);
      e.setMovePos(new Vec3d(motion[0], mc.options.sneakKey.isPressed() ? -1.0D : (mc.options.jumpKey.isPressed() ? 1.0D : 0.0D), motion[1]));
   }

   @EventTarget
   private void onMoveInput(EventMoveInput e) {
      if (mc.player != null) {
         if (mc.player.getPose() == EntityPose.CROUCHING || mc.player.getPose() == EntityPose.SWIMMING) {
            e.setStrafe(e.getStrafe() * 5.0F);
         }

      }
   }
}
