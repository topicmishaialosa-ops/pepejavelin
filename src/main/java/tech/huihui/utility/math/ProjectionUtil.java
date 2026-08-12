package tech.huihui.utility.math;

import lombok.Generated;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import tech.huihui.utility.game.player.rotation.Rotation;
import tech.huihui.utility.game.player.rotation.RotationUtil;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.level.Render3DUtil;

public final class ProjectionUtil implements IMinecraft {
   private static final int[] VIEWPORT = new int[4];
   private static final Vector4f TRANSFORMED = new Vector4f();
   private static final Vector3f TARGET = new Vector3f();
   private static final Matrix4f PROJ = new Matrix4f();
   private static final Vec3d[] CORNER_ARRAY = new Vec3d[8];
   private static int viewportWidth = -1;
   private static int viewportHeight = -1;

   @NotNull
   public static Vec3d worldSpaceToScreenSpace(Vec3d pos) {
      if (viewportWidth != mc.getWindow().getFramebufferWidth() || viewportHeight != mc.getWindow().getFramebufferHeight()) {
         viewportWidth = mc.getWindow().getFramebufferWidth();
         viewportHeight = mc.getWindow().getFramebufferHeight();
         GL11.glGetIntegerv(2978, VIEWPORT);
      }
      Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
      TRANSFORMED.set((float)(pos.x - cameraPos.x), (float)(pos.y - cameraPos.y), (float)(pos.z - cameraPos.z), 1.0F).mul(Render3DUtil.getLastWorldSpaceMatrix());
      PROJ.set(Render3DUtil.getLastProjMat());
      PROJ.project(TRANSFORMED.x(), TRANSFORMED.y(), TRANSFORMED.z(), VIEWPORT, TARGET);
      return new Vec3d((double)TARGET.x / mc.getWindow().getScaleFactor(), (double)((float)mc.getWindow().getHeight() - TARGET.y) / mc.getWindow().getScaleFactor(), (double)TARGET.z);
   }

   public static boolean canSee(Vec3d vec3d) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      Rotation angle = RotationUtil.calculateAngle(vec3d);
      return Math.abs(MathHelper.wrapDegrees(angle.getYaw() - camera.getYaw())) < 90.0F && Math.abs(MathHelper.wrapDegrees(angle.getPitch() - camera.getPitch())) < 60.0F || canSee(new Box(BlockPos.ofFloored(vec3d)));
   }

   public static boolean canSee(Box box) {
      Frustum frustum = mc.worldRenderer.frustum;
      return box != null && frustum != null && frustum.isVisible(box);
   }

   public static boolean canSee(Vector4d vec) {
      return vec == null || vec.x < 0.0D && vec.z < 1.0D || vec.y < 0.0D && vec.w < 1.0D;
   }

   public static double centerX(Vector4d vec) {
      return vec.x + (vec.z - vec.x) / 2.0D;
   }

   @NotNull
   public static Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
      fillVec3ds(ent, pos, CORNER_ARRAY);
      return CORNER_ARRAY;
   }

   private static void fillVec3ds(Entity ent, Vec3d pos, Vec3d[] out) {
      Box axisAlignedBB2 = ent.getBoundingBox();
      double minX = axisAlignedBB2.minX - ent.getX() + pos.x - 0.10000000149011612D;
      double minY = axisAlignedBB2.minY - ent.getY() + pos.y - 0.10000000149011612D;
      double minZ = axisAlignedBB2.minZ - ent.getZ() + pos.z - 0.10000000149011612D;
      double maxX = axisAlignedBB2.maxX - ent.getX() + pos.x + 0.10000000149011612D;
      double maxY = axisAlignedBB2.maxY - ent.getY() + pos.y + 0.10000000149011612D;
      double maxZ = axisAlignedBB2.maxZ - ent.getZ() + pos.z + 0.10000000149011612D;
      out[0] = new Vec3d(minX, minY, minZ);
      out[1] = new Vec3d(minX, maxY, minZ);
      out[2] = new Vec3d(maxX, minY, minZ);
      out[3] = new Vec3d(maxX, maxY, minZ);
      out[4] = new Vec3d(minX, minY, maxZ);
      out[5] = new Vec3d(minX, maxY, maxZ);
      out[6] = new Vec3d(maxX, minY, maxZ);
      out[7] = new Vec3d(maxX, maxY, maxZ);
   }

   public static Vector4d getVector4D(Entity ent) {
      Vector4d position = null;
      fillVec3ds(ent, MathUtil.interpolate(ent), CORNER_ARRAY);
      Vec3d[] var2 = CORNER_ARRAY;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Vec3d vector = var2[var4];
         vector = worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
         if (vector.z > 0.0D && vector.z < 1.0D) {
            if (position == null) {
               position = new Vector4d(vector.x, vector.y, vector.z, 0.0D);
            }

            position.x = Math.min(vector.x, position.x);
            position.y = Math.min(vector.y, position.y);
            position.z = Math.max(vector.x, position.z);
            position.w = Math.max(vector.y, position.w);
         }
      }

      return position;
   }

   @Generated
   private ProjectionUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
