package tech.huihui.utility.render.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public final class ProceduralModel {
   public static final Identifier WHITE_TEXTURE = Identifier.ofVanilla("textures/misc/white.png");

   private final ModelPart root;
   private final Map<String, Node> nodes = new HashMap<>();

   private ProceduralModel(List<PartData> parts, int textureWidth, int textureHeight) {
      ModelData modelData = new ModelData();
      ModelPartData rootData = modelData.getRoot();
      for (PartData part : parts) {
         ModelPartBuilder builder = ModelPartBuilder.create().uv(0, 0)
            .cuboid(part.x, part.y, part.z, part.w, part.h, part.d);
         rootData.addChild(part.name, builder, ModelTransform.pivot(part.pivotX, part.pivotY, part.pivotZ));
      }
      this.root = TexturedModelData.of(modelData, textureWidth, textureHeight).createModel();
      for (PartData part : parts) {
         this.nodes.put(part.name, new Node(part.name, this.root.getChild(part.name), part.argb));
      }
   }

   public Node node(String name) {
      return this.nodes.get(name);
   }

   public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
      VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE));
      for (Node node : this.nodes.values()) {
         node.render(matrices, buffer, light, overlay);
      }
   }

   public static final class Node {
      public final String name;
      private final ModelPart part;
      private final int argb;

      public Node(String name, ModelPart part, int argb) {
         this.name = name;
         this.part = part;
         this.argb = argb;
      }

      public void render(MatrixStack matrices, VertexConsumer buffer, int light, int overlay) {
         this.part.render(matrices, buffer, light, overlay, this.argb);
      }

      public void setPitch(float pitch) {
         this.part.pitch = pitch;
      }

      public void setYaw(float yaw) {
         this.part.yaw = yaw;
      }

      public void setRoll(float roll) {
         this.part.roll = roll;
      }
   }

   private static final class PartData {
      final String name;
      final float pivotX;
      final float pivotY;
      final float pivotZ;
      final float x;
      final float y;
      final float z;
      final float w;
      final float h;
      final float d;
      final int argb;

      PartData(String name, float pivotX, float pivotY, float pivotZ, float x, float y, float z, float w, float h, float d, int argb) {
         this.name = name;
         this.pivotX = pivotX;
         this.pivotY = pivotY;
         this.pivotZ = pivotZ;
         this.x = x;
         this.y = y;
         this.z = z;
         this.w = w;
         this.h = h;
         this.d = d;
         this.argb = argb;
      }
   }

   public static Builder builder() {
      return new Builder();
   }

   public static final class Builder {
      private final List<PartData> parts = new ArrayList<>();

      public Builder part(String name, float pivotX, float pivotY, float pivotZ, float x, float y, float z, float w, float h, float d, int argb) {
         this.parts.add(new PartData(name, pivotX, pivotY, pivotZ, x, y, z, w, h, d, argb));
         return this;
      }

      public ProceduralModel build() {
         return new ProceduralModel(this.parts, 64, 64);
      }
   }
}