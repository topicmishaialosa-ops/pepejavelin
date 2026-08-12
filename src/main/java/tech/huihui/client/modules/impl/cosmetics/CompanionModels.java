package tech.huihui.client.modules.impl.cosmetics;

import net.minecraft.util.math.MathHelper;
import tech.huihui.utility.render.model.ProceduralModel;

public final class CompanionModels {
   private static final int ARGB_WHITE = 0xFFFFFFFF;
   private static final int ARGB_LIGHT_GREY = 0xFFB8B8B8;
   private static final int ARGB_GREY = 0xFF9A9A9A;
   private static final int ARGB_DARK_GREY = 0xFF6E6E6E;
   private static final int ARGB_NEAR_BLACK = 0xFF1A1A1A;
   private static final int ARGB_BLACK = 0xFF222222;
   private static final int ARGB_EYE_DARK = 0xFF1A1A1A;

   private CompanionModels() {
   }

   public static CompanionAnimator quadFloppyEars() {
      return quad(true, true, false);
   }

   public static CompanionAnimator quadPointyEars() {
      return quad(true, false, true);
   }

   public static CompanionAnimator quadNoEars() {
      return quad(true, false, false);
   }

   public static CompanionAnimator catAnim() {
      return (model, instance, time, limb, walk) -> {
         pitch(model, "legFL", MathHelper.cos(limb + MathHelper.PI) * 0.55F * walk);
         pitch(model, "legFR", MathHelper.cos(limb) * 0.55F * walk);
         pitch(model, "legBL", MathHelper.cos(limb) * 0.55F * walk);
         pitch(model, "legBR", MathHelper.cos(limb + MathHelper.PI) * 0.55F * walk);
         pitch(model, "head", MathHelper.abs(MathHelper.sin(limb * 2.0F)) * 0.05F * walk + MathHelper.sin(time * 1.4F) * 0.05F);
         yaw(model, "head", MathHelper.sin(time * 0.6F) * 0.22F);
         yaw(model, "tail", MathHelper.sin(time * 2.4F) * 0.5F + walk * 0.3F);
         roll(model, "tail", MathHelper.sin(time * 2.4F) * 0.25F);
         roll(model, "earL", MathHelper.sin(time * 1.7F) * 0.15F);
         roll(model, "earR", -MathHelper.sin(time * 1.7F) * 0.15F);
      };
   }

   public static CompanionAnimator bearAnim() {
      return (model, instance, time, limb, walk) -> {
         pitch(model, "legL", MathHelper.cos(limb + MathHelper.PI) * 0.55F * walk);
         pitch(model, "legR", MathHelper.cos(limb) * 0.55F * walk);
         pitch(model, "armL", MathHelper.cos(limb) * 0.55F * walk);
         pitch(model, "armR", MathHelper.cos(limb + MathHelper.PI) * 0.55F * walk);
         pitch(model, "body", walk * 0.06F + MathHelper.sin(time * 1.6F) * 0.02F);
         pitch(model, "head", MathHelper.sin(time * 1.3F) * 0.07F);
         yaw(model, "head", MathHelper.sin(time * 0.5F) * 0.25F);
         roll(model, "earL", MathHelper.sin(time * 2.0F) * 0.1F);
         roll(model, "earR", -MathHelper.sin(time * 2.0F) * 0.1F);
      };
   }

   public static CompanionAnimator hopAnim() {
      return (model, instance, time, limb, walk) -> {
         float hop = Math.abs(MathHelper.sin(time * 3.0F));
         pitch(model, "legFL", -hop * 0.6F * walk);
         pitch(model, "legFR", -hop * 0.6F * walk);
         pitch(model, "legBL", -hop * 0.7F * walk);
         pitch(model, "legBR", -hop * 0.7F * walk);
         pitch(model, "body", hop * 0.15F * walk);
         pitch(model, "head", hop * 0.25F * walk + MathHelper.sin(time * 1.2F) * 0.05F);
         roll(model, "earL", MathHelper.sin(time * 2.0F) * 0.12F);
         roll(model, "earR", -MathHelper.sin(time * 2.0F) * 0.12F);
         yaw(model, "tail", MathHelper.sin(time * 1.5F) * 0.25F);
      };
   }

   public static CompanionAnimator chickenAnim() {
      return (model, instance, time, limb, walk) -> {
         float flap = MathHelper.sin(time * 4.0F) * 0.45F;
         pitch(model, "wingL", flap + walk * 0.2F);
         pitch(model, "wingR", -flap - walk * 0.2F);
         pitch(model, "head", MathHelper.abs(MathHelper.sin(time * 2.0F)) * 0.2F + MathHelper.sin(time * 3.0F) * 0.1F);
         pitch(model, "legL", MathHelper.sin(time * 8.0F) * 0.25F * walk);
         pitch(model, "legR", MathHelper.sin(time * 8.0F + MathHelper.PI) * 0.25F * walk);
      };
   }

   private static CompanionAnimator quad(boolean tail, boolean floppyEars, boolean pointyEars) {
      return (model, instance, time, limb, walk) -> {
         pitch(model, "legFL", MathHelper.cos(limb + MathHelper.PI) * 0.55F * walk);
         pitch(model, "legFR", MathHelper.cos(limb) * 0.55F * walk);
         pitch(model, "legBL", MathHelper.cos(limb) * 0.55F * walk);
         pitch(model, "legBR", MathHelper.cos(limb + MathHelper.PI) * 0.55F * walk);
         yaw(model, "body", MathHelper.sin(limb) * 0.05F * walk);
         pitch(model, "head", MathHelper.abs(MathHelper.sin(limb * 2.0F)) * 0.06F * walk + MathHelper.sin(time * 1.4F) * 0.04F);
         yaw(model, "head", MathHelper.sin(time * 0.6F) * 0.18F);
         if (tail) {
            yaw(model, "tail", MathHelper.sin(time * 2.2F) * 0.45F + walk * 0.3F);
            pitch(model, "tail", walk * 0.3F);
         }
         if (floppyEars) {
            roll(model, "earL", -MathHelper.sin(time * 1.7F) * 0.25F);
            roll(model, "earR", MathHelper.sin(time * 1.7F) * 0.25F);
         }
         if (pointyEars) {
            roll(model, "earL", MathHelper.sin(time * 1.7F) * 0.12F);
            roll(model, "earR", -MathHelper.sin(time * 1.7F) * 0.12F);
         }
      };
   }

   public static ProceduralModel puppyChase() {
      return puppyHat(puppyBase(0xFF1F6FE0, 0xFFF5F8FF, 0xFF14479A), HAT_POLICE, 0xFF0D3D8F);
   }

   public static ProceduralModel puppyMarshall() {
      return puppyHat(puppyBase(0xFFE0362C, 0xFFF2D9C4, 0xFF9E2218), HAT_FIRE, 0xFFB02A18);
   }

   public static ProceduralModel puppySkye() {
      return puppyHat(puppyBase(0xFFF2A3C0, 0xFFFFFFFF, 0xFFD16F97), HAT_PILOT, 0xFFE57FB0);
   }

   public static ProceduralModel puppyRubble() {
      return puppy(puppyBase(0xFF8C7A66, 0xFFC9B8A6, 0xFF6B5C4C), 0xFF8C7A66, 0xFFC9B8A6, 0xFF6B5C4C);
   }

   public static ProceduralModel puppyZuma() {
      return puppy(puppyBase(0xFFF28A2E, 0xFFFFD9A8, 0xFFC76A1C), 0xFFF28A2E, 0xFFFFD9A8, 0xFFC76A1C);
   }

   public static ProceduralModel puppyRocky() {
      return puppy(puppyBase(0xFF5FA84F, 0xFFC8E8BC, 0xFF3F7A33), 0xFF5FA84F, 0xFFC8E8BC, 0xFF3F7A33);
   }

   public static ProceduralModel bearKesha() {
      return bear(0xFFF5F2EC, 0xFFFFFFFF, 0xFF2E6BD6);
   }

   public static ProceduralModel bearTuchka() {
      return bear(0xFF8A5A33, 0xFFD9B089, 0xFFD63131);
   }

   public static ProceduralModel bearSonya() {
      return bear(0xFFF0A8C0, 0xFFFFE0EC, 0xFFB57EDC);
   }

   public static ProceduralModel minecraftPig() {
      return pigModel();
   }

   public static ProceduralModel minecraftWolf() {
      return wolfModel();
   }

   public static ProceduralModel minecraftDachshund() {
      return dachshundModel();
   }

   public static ProceduralModel minecraftCat() {
      return catModel();
   }

   public static ProceduralModel minecraftChicken() {
      return chickenModel();
   }

   public static ProceduralModel minecraftCow() {
      return cowModel();
   }

   public static ProceduralModel minecraftRabbit() {
      return rabbitModel();
   }

   public static ProceduralModel minecraftPanda() {
      return pandaModel();
   }

   public static ProceduralModel minecraftSheep() {
      return sheepModel();
   }

   public static ProceduralModel minecraftFox() {
      return foxModel();
   }

   private static final int HAT_NONE = 0;
   private static final int HAT_POLICE = 1;
   private static final int HAT_FIRE = 2;
   private static final int HAT_PILOT = 3;

   private static ProceduralModel.Builder puppyBase(int main, int light, int dark) {
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 5F, 0F, -3F, 0F, -2.4F, 6F, 4.4F, 4.8F, main)
         .part("belly", 0F, 5F, 0F, -2.3F, 0.3F, -1.9F, 4.6F, 1.8F, 3.8F, light)
         .part("chest", 0F, 5.2F, -2.0F, -2.6F, -0.4F, -1.2F, 5.2F, 4.6F, 2.4F, main)
         .part("head", 0F, 10.6F, -3.4F, -2.6F, -1.8F, -2.2F, 5.2F, 4.4F, 4.4F, main)
         .part("snout", 0F, 8.6F, -5.2F, -1.4F, -0.8F, -1.0F, 2.8F, 2.2F, 2.0F, light)
         .part("nose", 0F, 8.9F, -5.2F, -0.7F, 0F, -0.8F, 1.4F, 0.9F, 0.8F, 0xFF2A2A2A)
         .part("eyeL", -1.7F, 10.7F, -5.5F, -0.5F, -0.5F, -0.2F, 1.0F, 1.0F, 0.4F, ARGB_EYE_DARK)
         .part("eyeR", 1.7F, 10.7F, -5.5F, -0.5F, -0.5F, -0.2F, 1.0F, 1.0F, 0.4F, ARGB_EYE_DARK)
         .part("earL", -1.9F, 12.4F, -3.6F, -0.7F, 0F, -0.9F, 1.4F, 3.0F, 1.8F, dark)
         .part("earR", 1.9F, 12.4F, -3.6F, -0.7F, 0F, -0.9F, 1.4F, 3.0F, 1.8F, dark)
         .part("legFL", -1.8F, 4.8F, -1.8F, -1.0F, -4.8F, -1.0F, 2.0F, 4.8F, 2.0F, main)
         .part("legFR", 1.8F, 4.8F, -1.8F, -1.0F, -4.8F, -1.0F, 2.0F, 4.8F, 2.0F, main)
         .part("legBL", -1.8F, 4.8F, 1.6F, -1.0F, -4.8F, -1.0F, 2.0F, 4.8F, 2.0F, main)
         .part("legBR", 1.8F, 4.8F, 1.6F, -1.0F, -4.8F, -1.0F, 2.0F, 4.8F, 2.0F, main)
         .part("tail", 0F, 6.4F, 2.2F, -0.8F, 0F, -0.8F, 1.6F, 1.6F, 3.0F, dark)
         .part("bandana", 0F, 7.9F, -3.4F, -2.0F, -0.6F, -0.5F, 4.0F, 1.5F, 1.4F, dark);
      eyeShine(b, 1.7F, 10.7F, -5.5F);
      toes(b, 0, -1.8F, -1.8F, light);
      toes(b, 1, 1.8F, -1.8F, light);
      toes(b, 2, -1.8F, 1.6F, light);
      toes(b, 3, 1.8F, 1.6F, light);
      return b;
   }

   private static ProceduralModel puppy(ProceduralModel.Builder base, int main, int light, int dark) {
      return puppyHat(base, HAT_NONE, main);
   }

   private static ProceduralModel puppyHat(ProceduralModel.Builder base, int hat, int hatColor) {
      if (hat == HAT_POLICE) {
         base.part("cap", 0F, 12.9F, -3.4F, -2.8F, 0F, -2.3F, 5.6F, 1.1F, 4.6F, hatColor)
             .part("capBrim", 0F, 12.6F, -5.5F, -2.8F, -0.35F, -0.8F, 5.6F, 0.7F, 1.6F, hatColor);
      } else if (hat == HAT_FIRE) {
         base.part("cap", 0F, 12.9F, -3.4F, -2.9F, 0F, -2.4F, 5.8F, 2.0F, 4.8F, hatColor)
             .part("capBrim", 0F, 12.6F, -3.4F, -3.1F, -0.4F, -2.6F, 6.2F, 0.8F, 5.2F, hatColor);
      } else if (hat == HAT_PILOT) {
         base.part("cap", 0F, 12.9F, -3.4F, -2.8F, 0F, -2.3F, 5.6F, 1.6F, 4.6F, hatColor)
             .part("goggle", 0F, 12.6F, -5.6F, -2.2F, -0.3F, -0.5F, 4.4F, 0.9F, 1.0F, 0xFF3A3A3A);
      }
      return base.build();
   }

   private static ProceduralModel bear(int main, int light, int accent) {
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 6.5F, 0F, -3F, 0F, -2.2F, 6F, 5.6F, 4.4F, main)
         .part("belly", 0F, 6.7F, -2.6F, -2.3F, 0.2F, -0.4F, 4.6F, 3.4F, 0.8F, light)
         .part("scarf", 0F, 10.6F, 0F, -2.7F, -0.8F, -2.4F, 5.4F, 1.3F, 4.8F, accent)
         .part("head", 0F, 13.2F, 0F, -3F, -2F, -2.8F, 6F, 5.6F, 5.6F, main)
         .part("earL", -2.5F, 15.8F, 0F, -1.3F, 0F, -1.1F, 2.6F, 2.2F, 2.2F, main)
         .part("earR", 2.5F, 15.8F, 0F, -1.3F, 0F, -1.1F, 2.6F, 2.2F, 2.2F, main)
         .part("earInnerL", -2.5F, 15.6F, 0F, -0.9F, 0F, -0.7F, 1.8F, 1.4F, 1.4F, light)
         .part("earInnerR", 2.5F, 15.6F, 0F, -0.9F, 0F, -0.7F, 1.8F, 1.4F, 1.4F, light)
         .part("muzzle", 0F, 12.2F, -2.8F, -1.7F, -0.9F, -1.0F, 3.4F, 2.7F, 2.0F, light)
         .part("nose", 0F, 12.7F, -3.8F, -0.8F, -0.35F, -0.5F, 1.6F, 1.1F, 1.0F, 0xFF2A2A2A)
         .part("eyeL", -1.7F, 13.9F, -2.8F, -0.5F, -0.5F, -0.15F, 1.0F, 1.0F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.7F, 13.9F, -2.8F, -0.5F, -0.5F, -0.15F, 1.0F, 1.0F, 0.3F, ARGB_EYE_DARK)
         .part("blushL", -2.4F, 12.9F, -2.5F, -0.35F, -0.1F, -0.15F, 0.9F, 0.6F, 0.4F, 0xFFF2A8B0)
         .part("blushR", 2.4F, 12.9F, -2.5F, -0.35F, -0.1F, -0.15F, 0.9F, 0.6F, 0.4F, 0xFFF2A8B0)
         .part("armL", -3.5F, 13.0F, 0F, -1.1F, -4.8F, -1.1F, 2.2F, 4.8F, 2.2F, main)
         .part("armR", 3.5F, 13.0F, 0F, -1.1F, -4.8F, -1.1F, 2.2F, 4.8F, 2.2F, main)
         .part("legL", -1.7F, 6.5F, 0F, -1.3F, -6.5F, -1.3F, 2.6F, 6.5F, 2.6F, main)
         .part("legR", 1.7F, 6.5F, 0F, -1.3F, -6.5F, -1.3F, 2.6F, 6.5F, 2.6F, main)
         .part("pawPadL", -1.7F, 0.6F, 0.9F, -0.7F, 0F, -0.3F, 1.4F, 0.9F, 0.8F, 0xFFD9A8B8)
         .part("pawPadR", 1.7F, 0.6F, 0.9F, -0.7F, 0F, -0.3F, 1.4F, 0.9F, 0.8F, 0xFFD9A8B8);
      eyeShine(b, 1.7F, 13.9F, -2.8F);
      return b.build();
   }

   private static ProceduralModel pigModel() {
      int main = 0xFFF0A8A0;
      int dark = 0xFFE08E84;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 6F, 0F, -3.2F, 0F, -2.4F, 6.4F, 4.8F, 4.8F, main)
         .part("head", 0F, 10.4F, -3.4F, -2.6F, -1.8F, -2.0F, 5.2F, 4.6F, 4.0F, main)
         .part("snout", 0F, 9.2F, -5.4F, -2.2F, -1.0F, -1.0F, 4.4F, 2.4F, 2.0F, dark)
         .part("nostrilL", -1.2F, 9.2F, -5.4F, -0.55F, -0.25F, -1.0F, 1.1F, 0.8F, 0.8F, 0xFFB06A5C)
         .part("nostrilR", 1.2F, 9.2F, -5.4F, -0.55F, -0.25F, -1.0F, 1.1F, 0.8F, 0.8F, 0xFFB06A5C)
         .part("eyeL", -1.8F, 10.9F, -5.2F, -0.45F, -0.45F, -0.15F, 0.9F, 0.9F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.8F, 10.9F, -5.2F, -0.45F, -0.45F, -0.15F, 0.9F, 0.9F, 0.3F, ARGB_EYE_DARK)
         .part("earL", -2.6F, 12.6F, -3.6F, -0.9F, 0F, -1.2F, 1.8F, 1.3F, 2.4F, dark)
         .part("earR", 2.6F, 12.6F, -3.6F, -0.9F, 0F, -1.2F, 1.8F, 1.3F, 2.4F, dark)
         .part("legFL", -1.9F, 3.4F, -1.7F, -1.0F, -3.4F, -1.0F, 2.0F, 3.4F, 2.0F, dark)
         .part("legFR", 1.9F, 3.4F, -1.7F, -1.0F, -3.4F, -1.0F, 2.0F, 3.4F, 2.0F, dark)
         .part("legBL", -1.9F, 3.4F, 1.7F, -1.0F, -3.4F, -1.0F, 2.0F, 3.4F, 2.0F, dark)
         .part("legBR", 1.9F, 3.4F, 1.7F, -1.0F, -3.4F, -1.0F, 2.0F, 3.4F, 2.0F, dark)
         .part("tail", 0F, 7.6F, 2.4F, -0.7F, -0.5F, 0F, 1.4F, 1.4F, 1.8F, main);
      eyeShine(b, 1.8F, 10.9F, -5.2F);
      toes(b, 0, -1.9F, -1.7F, dark);
      toes(b, 1, 1.9F, -1.7F, dark);
      toes(b, 2, -1.9F, 1.7F, dark);
      toes(b, 3, 1.9F, 1.7F, dark);
      return b.build();
   }

   private static ProceduralModel wolfModel() {
      int main = ARGB_GREY;
      int light = ARGB_LIGHT_GREY;
      int dark = ARGB_DARK_GREY;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 6.5F, 0F, -3F, 0F, -2.2F, 6F, 4.6F, 4.4F, main)
         .part("chest", 0F, 6.8F, -1.9F, -2.5F, -0.5F, -1.0F, 5F, 4.9F, 2F, light)
         .part("head", 0F, 11F, -3.4F, -2.4F, -1.8F, -2.0F, 4.8F, 4.6F, 4.0F, main)
         .part("snout", 0F, 9.8F, -5.2F, -1.3F, -0.8F, -1.0F, 2.6F, 2.2F, 2.0F, light)
         .part("nose", 0F, 10.1F, -5.2F, -0.6F, 0F, -0.7F, 1.2F, 0.8F, 0.7F, 0xFF1A1A1A)
         .part("eyeL", -1.6F, 11.5F, -5.3F, -0.45F, -0.45F, -0.15F, 0.9F, 0.9F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.6F, 11.5F, -5.3F, -0.45F, -0.45F, -0.15F, 0.9F, 0.9F, 0.3F, ARGB_EYE_DARK)
         .part("earL", -1.9F, 13F, -3.6F, -0.85F, 0F, -0.85F, 1.7F, 2.8F, 1.7F, dark)
         .part("earR", 1.9F, 13F, -3.6F, -0.85F, 0F, -0.85F, 1.7F, 2.8F, 1.7F, dark)
         .part("collar", 0F, 8.4F, -3.0F, -2.1F, -0.6F, -0.5F, 4.2F, 1.3F, 1.4F, 0xFFD63A2A)
         .part("collarTag", 0F, 7.8F, -3.6F, -0.45F, -0.35F, -0.2F, 0.9F, 1.0F, 0.5F, 0xFFF2C94C)
         .part("legFL", -1.7F, 5.0F, -1.7F, -0.95F, -5.0F, -0.95F, 1.9F, 5.0F, 1.9F, main)
         .part("legFR", 1.7F, 5.0F, -1.7F, -0.95F, -5.0F, -0.95F, 1.9F, 5.0F, 1.9F, main)
         .part("legBL", -1.7F, 5.0F, 1.6F, -0.95F, -5.0F, -0.95F, 1.9F, 5.0F, 1.9F, main)
         .part("legBR", 1.7F, 5.0F, 1.6F, -0.95F, -5.0F, -0.95F, 1.9F, 5.0F, 1.9F, main)
         .part("tail", 0F, 7.2F, 2.2F, -0.9F, 0F, -0.9F, 1.8F, 2.4F, 4.0F, dark);
      eyeShine(b, 1.6F, 11.5F, -5.3F);
      toes(b, 0, -1.7F, -1.7F, dark);
      toes(b, 1, 1.7F, -1.7F, dark);
      toes(b, 2, -1.7F, 1.6F, dark);
      toes(b, 3, 1.7F, 1.6F, dark);
      return b.build();
   }

   private static ProceduralModel dachshundModel() {
      int main = 0xFFA9713F;
      int dark = 0xFF7A4E26;
      int light = 0xFFC08B54;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 3.8F, 0F, -2.8F, 0F, -4.4F, 5.6F, 3.6F, 8.8F, main)
         .part("chest", 0F, 3.8F, -3.8F, -2.4F, 0F, -1.6F, 4.8F, 4.2F, 3.2F, light)
         .part("head", 0F, 8.6F, -5.6F, -2.2F, -1.6F, -1.8F, 4.4F, 4.2F, 3.6F, main)
         .part("snout", 0F, 7.6F, -7.2F, -1.1F, -0.8F, -1.0F, 2.2F, 2.4F, 2.6F, main)
         .part("nose", 0F, 7.9F, -7.2F, -0.55F, 0F, -0.7F, 1.1F, 0.75F, 0.7F, 0xFF1A1A1A)
         .part("eyeL", -1.5F, 9.0F, -7.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.5F, 9.0F, -7.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("earL", -2.2F, 9.2F, -5.2F, -0.9F, -0.5F, -1.2F, 1.8F, 2.8F, 2.4F, dark)
         .part("earR", 2.2F, 9.2F, -5.2F, -0.9F, -0.5F, -1.2F, 1.8F, 2.8F, 2.4F, dark)
         .part("legFL", -1.9F, 2.6F, -2.4F, -0.9F, -2.6F, -0.9F, 1.8F, 2.6F, 1.8F, main)
         .part("legFR", 1.9F, 2.6F, -2.4F, -0.9F, -2.6F, -0.9F, 1.8F, 2.6F, 1.8F, main)
         .part("legBL", -1.9F, 2.6F, 2.6F, -0.9F, -2.6F, -0.9F, 1.8F, 2.6F, 1.8F, main)
         .part("legBR", 1.9F, 2.6F, 2.6F, -0.9F, -2.6F, -0.9F, 1.8F, 2.6F, 1.8F, main)
         .part("tail", 0F, 5.2F, 4.2F, -0.5F, 0F, 0F, 1.0F, 1.0F, 2.6F, main);
      eyeShine(b, 1.5F, 9.0F, -7.3F);
      toes(b, 0, -1.9F, -2.4F, dark);
      toes(b, 1, 1.9F, -2.4F, dark);
      toes(b, 2, -1.9F, 2.6F, dark);
      toes(b, 3, 1.9F, 2.6F, dark);
      return b.build();
   }

   private static ProceduralModel catModel() {
      int main = 0xFFE8A050;
      int dark = 0xFFC07E2E;
      int light = 0xFFF5D8A8;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 5.8F, 0F, -2.6F, 0F, -1.8F, 5.2F, 3.8F, 3.6F, main)
         .part("chest", 0F, 5.8F, -1.6F, -2.0F, -0.2F, -0.8F, 4.0F, 4.0F, 1.6F, light)
         .part("head", 0F, 9.8F, -2.6F, -2.2F, -1.6F, -1.8F, 4.4F, 4.0F, 3.6F, main)
         .part("snout", 0F, 8.6F, -4.3F, -1.0F, -0.6F, -0.6F, 2.0F, 1.7F, 1.2F, light)
         .part("nose", 0F, 8.8F, -4.3F, -0.5F, 0F, -0.6F, 1.0F, 0.7F, 0.6F, 0xFFE06088)
         .part("eyeL", -1.5F, 10.1F, -4.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, 0xFF2E7D32)
         .part("eyeR", 1.5F, 10.1F, -4.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, 0xFF2E7D32)
         .part("earL", -1.8F, 12F, -2.8F, -0.8F, 0F, -0.7F, 1.6F, 2.4F, 1.4F, main)
         .part("earR", 1.8F, 12F, -2.8F, -0.8F, 0F, -0.7F, 1.6F, 2.4F, 1.4F, main)
         .part("earInnerL", -1.8F, 11.6F, -2.8F, -0.45F, 0F, -0.35F, 0.9F, 1.5F, 0.7F, 0xFFE8A8B8)
         .part("earInnerR", 1.8F, 11.6F, -2.8F, -0.45F, 0F, -0.35F, 0.9F, 1.5F, 0.7F, 0xFFE8A8B8)
         .part("legFL", -1.5F, 4.2F, -1.4F, -0.7F, -4.2F, -0.7F, 1.4F, 4.2F, 1.4F, main)
         .part("legFR", 1.5F, 4.2F, -1.4F, -0.7F, -4.2F, -0.7F, 1.4F, 4.2F, 1.4F, main)
         .part("legBL", -1.5F, 4.2F, 1.4F, -0.7F, -4.2F, -0.7F, 1.4F, 4.2F, 1.4F, main)
         .part("legBR", 1.5F, 4.2F, 1.4F, -0.7F, -4.2F, -0.7F, 1.4F, 4.2F, 1.4F, main)
         .part("tail", 0F, 6.8F, 1.8F, -0.5F, 0F, 0F, 1.0F, 1.0F, 4.6F, main)
         .part("tailTip", 0F, 8.8F, 1.8F, -0.5F, -1.4F, 3.4F, 1.0F, 1.6F, 1.2F, dark);
      eyeShine(b, 1.5F, 10.1F, -4.3F);
      toes(b, 0, -1.5F, -1.4F, light);
      toes(b, 1, 1.5F, -1.4F, light);
      toes(b, 2, -1.5F, 1.4F, light);
      toes(b, 3, 1.5F, 1.4F, light);
      return b.build();
   }

   private static ProceduralModel chickenModel() {
      int white = 0xFFF2F0EA;
      int yellow = 0xFFE8B838;
      int red = 0xFFD63A2A;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 4.4F, 0F, -2.6F, 0F, -2.2F, 5.2F, 4.4F, 4.4F, white)
         .part("wingL", -2.8F, 5.2F, 0F, -0.5F, -1.2F, -2.0F, 1.0F, 3.2F, 4.0F, white)
         .part("wingR", 2.8F, 5.2F, 0F, -0.5F, -1.2F, -2.0F, 1.0F, 3.2F, 4.0F, white)
         .part("tail", 0F, 5.6F, 2.0F, -2.2F, 0F, 0F, 4.4F, 3.4F, 2.0F, white)
         .part("head", 0F, 8.2F, -1.6F, -1.6F, -1.2F, -1.4F, 3.2F, 3.4F, 3.2F, white)
         .part("comb", 0F, 10.4F, -1.6F, -1.0F, 0F, -0.5F, 2.0F, 1.3F, 1.0F, red)
         .part("beak", 0F, 7.8F, -3.0F, -0.9F, -0.5F, -1.0F, 1.8F, 1.4F, 1.2F, yellow)
         .part("wattle", 0F, 7.0F, -3.0F, -0.5F, 0F, -0.4F, 1.0F, 1.0F, 0.8F, red)
         .part("eyeL", -1.0F, 8.8F, -2.9F, -0.35F, -0.35F, -0.15F, 0.7F, 0.7F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.0F, 8.8F, -2.9F, -0.35F, -0.35F, -0.15F, 0.7F, 0.7F, 0.3F, ARGB_EYE_DARK)
         .part("legL", -1.0F, 3.2F, 0F, -0.4F, -3.2F, -0.4F, 0.8F, 3.2F, 0.8F, yellow)
         .part("legR", 1.0F, 3.2F, 0F, -0.4F, -3.2F, -0.4F, 0.8F, 3.2F, 0.8F, yellow);
      eyeShine(b, 1.0F, 8.8F, -2.9F);
      toes(b, 0, -1.0F, 0.0F, yellow);
      toes(b, 1, 1.0F, 0.0F, yellow);
      return b.build();
   }

   private static ProceduralModel cowModel() {
      int white = 0xFFF2F0EA;
      int dark = 0xFF3A3A3A;
      int pink = 0xFFE8A8B0;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 7F, 0F, -3.4F, 0F, -2.6F, 6.8F, 5.6F, 5.2F, white)
         .part("spot1", 0F, 7F, 0F, -3.5F, 0.6F, -1.8F, 1.8F, 2.2F, 2.4F, dark)
         .part("spot2", 0F, 7F, 0F, 1.6F, 1.0F, -0.6F, 1.8F, 2.0F, 2.6F, dark)
         .part("spot3", 0F, 7F, 0F, -0.9F, 0.4F, 2.2F, 2.2F, 1.8F, 1.6F, dark)
         .part("head", 0F, 11.6F, -3.6F, -2.6F, -2.0F, -2.2F, 5.2F, 4.8F, 4.4F, white)
         .part("muzzle", 0F, 9.8F, -5.6F, -1.8F, -0.9F, -1.0F, 3.6F, 2.6F, 2.0F, pink)
         .part("noseL", -0.9F, 9.8F, -5.6F, -0.55F, -0.2F, -1.0F, 1.1F, 0.8F, 0.8F, 0xFFC8788A)
         .part("noseR", 0.9F, 9.8F, -5.6F, -0.55F, -0.2F, -1.0F, 1.1F, 0.8F, 0.8F, 0xFFC8788A)
         .part("eyeL", -1.8F, 12.1F, -5.5F, -0.45F, -0.45F, -0.15F, 0.9F, 0.9F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.8F, 12.1F, -5.5F, -0.45F, -0.45F, -0.15F, 0.9F, 0.9F, 0.3F, ARGB_EYE_DARK)
         .part("earL", -2.6F, 12.8F, -3.6F, -0.9F, 0F, -0.9F, 1.8F, 1.4F, 1.8F, white)
         .part("earR", 2.6F, 12.8F, -3.6F, -0.9F, 0F, -0.9F, 1.8F, 1.4F, 1.8F, white)
         .part("hornL", -1.9F, 13.8F, -3.8F, -0.5F, 0F, -0.5F, 1.0F, 1.6F, 1.0F, 0xFFD8D0C0)
         .part("hornR", 1.9F, 13.8F, -3.8F, -0.5F, 0F, -0.5F, 1.0F, 1.6F, 1.0F, 0xFFD8D0C0)
         .part("bell", 0F, 9.4F, -5.0F, -0.55F, -0.45F, -0.3F, 1.1F, 1.3F, 1.0F, 0xFFF2C94C)
         .part("legFL", -2.0F, 5.0F, -1.8F, -1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, white)
         .part("legFR", 2.0F, 5.0F, -1.8F, -1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, white)
         .part("legBL", -2.0F, 5.0F, 1.8F, -1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, white)
         .part("legBR", 2.0F, 5.0F, 1.8F, -1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F, white)
         .part("hoofFL", -2.0F, 0F, -1.8F, -1.05F, 0F, -1.05F, 2.1F, 1.0F, 2.1F, dark)
         .part("hoofFR", 2.0F, 0F, -1.8F, -1.05F, 0F, -1.05F, 2.1F, 1.0F, 2.1F, dark)
         .part("hoofBL", -2.0F, 0F, 1.8F, -1.05F, 0F, -1.05F, 2.1F, 1.0F, 2.1F, dark)
         .part("hoofBR", 2.0F, 0F, 1.8F, -1.05F, 0F, -1.05F, 2.1F, 1.0F, 2.1F, dark)
         .part("tail", 0F, 8.6F, 2.6F, -0.4F, 0F, 0F, 0.8F, 1.2F, 3.4F, dark)
         .part("tailTip", 0F, 9.6F, 2.6F, -0.7F, -0.6F, 2.6F, 1.4F, 1.6F, 1.2F, dark);
      eyeShine(b, 1.8F, 12.1F, -5.5F);
      return b.build();
   }

   private static ProceduralModel rabbitModel() {
      int white = 0xFFE8E4DC;
      int pink = 0xFFE8A8B8;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 4.6F, 0F, -2.6F, 0F, -1.8F, 5.2F, 4.2F, 3.6F, white)
         .part("head", 0F, 8.8F, -1.6F, -2.2F, -1.4F, -1.8F, 4.4F, 4.2F, 3.6F, white)
         .part("earL", -1.4F, 11.4F, -1.6F, -0.8F, 0F, -0.6F, 1.6F, 6.0F, 1.2F, white)
         .part("earR", 1.4F, 11.4F, -1.6F, -0.8F, 0F, -0.6F, 1.6F, 6.0F, 1.2F, white)
         .part("earInnerL", -1.4F, 11.2F, -1.6F, -0.45F, 0F, -0.25F, 0.9F, 4.8F, 0.5F, pink)
         .part("earInnerR", 1.4F, 11.2F, -1.6F, -0.45F, 0F, -0.25F, 0.9F, 4.8F, 0.5F, pink)
         .part("muzzle", 0F, 7.8F, -3.3F, -1.4F, -0.8F, -0.7F, 2.8F, 2.2F, 1.4F, white)
         .part("nose", 0F, 8.2F, -3.3F, -0.6F, 0F, -0.8F, 1.2F, 0.8F, 0.8F, pink)
         .part("toothL", -0.4F, 7.2F, -3.3F, -0.35F, 0F, -0.5F, 0.7F, 1.1F, 0.5F, 0xFFFFFFFF)
         .part("toothR", 0.4F, 7.2F, -3.3F, -0.35F, 0F, -0.5F, 0.7F, 1.1F, 0.5F, 0xFFFFFFFF)
         .part("eyeL", -1.4F, 9.3F, -3.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.4F, 9.3F, -3.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("legFL", -1.5F, 2.6F, -1.2F, -0.9F, -2.6F, -0.9F, 1.8F, 2.6F, 1.8F, white)
         .part("legFR", 1.5F, 2.6F, -1.2F, -0.9F, -2.6F, -0.9F, 1.8F, 2.6F, 1.8F, white)
         .part("legBL", -1.8F, 3.4F, 1.3F, -1.2F, -3.4F, -1.4F, 2.4F, 3.4F, 3.8F, white)
         .part("legBR", 1.8F, 3.4F, 1.3F, -1.2F, -3.4F, -1.4F, 2.4F, 3.4F, 3.8F, white)
         .part("tail", 0F, 5.8F, 1.8F, -1.0F, -1.0F, 0F, 2.0F, 2.0F, 1.4F, white);
      eyeShine(b, 1.4F, 9.3F, -3.3F);
      toes(b, 0, -1.5F, -1.2F, pink);
      toes(b, 1, 1.5F, -1.2F, pink);
      toes(b, 2, -1.8F, 1.3F, white);
      toes(b, 3, 1.8F, 1.3F, white);
      return b.build();
   }

   private static ProceduralModel pandaModel() {
      int white = 0xFFF5F2EC;
      int black = 0xFF222222;
      return ProceduralModel.builder()
         .part("body", 0F, 6F, 0F, -3.4F, 0F, -2.6F, 6.8F, 5.2F, 5.2F, white)
         .part("shoulder", 0F, 7.4F, 0F, -3.5F, 0.8F, -2.7F, 7.0F, 3.0F, 5.4F, black)
         .part("head", 0F, 11.2F, -3.4F, -2.8F, -2.0F, -2.4F, 5.6F, 5.2F, 4.8F, white)
         .part("eyePatchL", -1.7F, 11.8F, -5.6F, -1.4F, -1.1F, -0.3F, 2.8F, 2.6F, 0.6F, black)
         .part("eyePatchR", 1.7F, 11.8F, -5.6F, -1.4F, -1.1F, -0.3F, 2.8F, 2.6F, 0.6F, black)
         .part("eyeL", -1.7F, 11.8F, -5.6F, -0.4F, -0.4F, -0.5F, 0.8F, 0.8F, 0.5F, ARGB_WHITE)
         .part("eyeR", 1.7F, 11.8F, -5.6F, -0.4F, -0.4F, -0.5F, 0.8F, 0.8F, 0.5F, ARGB_WHITE)
         .part("pupilL", -1.7F, 11.8F, -5.6F, -0.18F, -0.1F, -0.6F, 0.4F, 0.4F, 0.3F, ARGB_NEAR_BLACK)
         .part("pupilR", 1.7F, 11.8F, -5.6F, -0.18F, -0.1F, -0.6F, 0.4F, 0.4F, 0.3F, ARGB_NEAR_BLACK)
         .part("nose", 0F, 10.4F, -5.8F, -1.0F, -0.4F, -0.5F, 2.0F, 1.1F, 1.0F, black)
         .part("earL", -2.5F, 14.4F, -3.6F, -1.3F, 0F, -1.1F, 2.6F, 2.6F, 2.2F, black)
         .part("earR", 2.5F, 14.4F, -3.6F, -1.3F, 0F, -1.1F, 2.6F, 2.6F, 2.2F, black)
         .part("legFL", -1.9F, 4.4F, -1.8F, -1.1F, -4.4F, -1.1F, 2.2F, 4.4F, 2.2F, black)
         .part("legFR", 1.9F, 4.4F, -1.8F, -1.1F, -4.4F, -1.1F, 2.2F, 4.4F, 2.2F, black)
         .part("legBL", -1.9F, 4.4F, 1.8F, -1.1F, -4.4F, -1.1F, 2.2F, 4.4F, 2.2F, black)
         .part("legBR", 1.9F, 4.4F, 1.8F, -1.1F, -4.4F, -1.1F, 2.2F, 4.4F, 2.2F, black)
         .part("tail", 0F, 7.0F, 2.6F, -0.9F, -0.9F, 0F, 1.8F, 1.8F, 1.2F, white)
         .build();
   }

   private static ProceduralModel sheepModel() {
      int wool = 0xFFE8E4D8;
      int dark = 0xFF7A7468;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 5F, 0F, -3.4F, 0F, -2.6F, 6.8F, 4.6F, 5.2F, wool)
         .part("bodyTop", 0F, 7.4F, 0F, -2.6F, 0F, -1.8F, 5.2F, 2.0F, 3.6F, wool)
         .part("head", 0F, 10F, -3.4F, -2.2F, -1.8F, -2.0F, 4.4F, 4.4F, 4.0F, dark)
         .part("earL", -2.3F, 10.8F, -3.6F, -0.8F, 0F, -0.8F, 1.6F, 1.2F, 1.6F, dark)
         .part("earR", 2.3F, 10.8F, -3.6F, -0.8F, 0F, -0.8F, 1.6F, 1.2F, 1.6F, dark)
         .part("nose", 0F, 9.2F, -5.4F, -0.7F, -0.3F, -0.6F, 1.4F, 1.0F, 1.2F, 0xFF5A5448)
         .part("eyeL", -1.6F, 10.6F, -5.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.6F, 10.6F, -5.3F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("legFL", -1.7F, 4.0F, -1.7F, -0.85F, -4.0F, -0.85F, 1.7F, 4.0F, 1.7F, dark)
         .part("legFR", 1.7F, 4.0F, -1.7F, -0.85F, -4.0F, -0.85F, 1.7F, 4.0F, 1.7F, dark)
         .part("legBL", -1.7F, 4.0F, 1.7F, -0.85F, -4.0F, -0.85F, 1.7F, 4.0F, 1.7F, dark)
         .part("legBR", 1.7F, 4.0F, 1.7F, -0.85F, -4.0F, -0.85F, 1.7F, 4.0F, 1.7F, dark)
         .part("tail", 0F, 6.4F, 2.6F, -1.1F, -1.1F, 0F, 2.2F, 2.2F, 1.2F, wool);
      eyeShine(b, 1.6F, 10.6F, -5.3F);
      toes(b, 0, -1.7F, -1.7F, dark);
      toes(b, 1, 1.7F, -1.7F, dark);
      toes(b, 2, -1.7F, 1.7F, dark);
      toes(b, 3, 1.7F, 1.7F, dark);
      return b.build();
   }

   private static ProceduralModel foxModel() {
      int main = 0xFFE07A2E;
      int light = 0xFFF5E4C8;
      int dark = 0xFFB05A1E;
      int tip = 0xFFF8F4EC;
      ProceduralModel.Builder b = ProceduralModel.builder()
         .part("body", 0F, 6F, 0F, -2.8F, 0F, -2.0F, 5.6F, 4.4F, 4.0F, main)
         .part("chest", 0F, 6.2F, -1.8F, -2.0F, -0.3F, -1.0F, 4.0F, 4.4F, 2.0F, light)
         .part("head", 0F, 10.4F, -3.0F, -2.3F, -1.7F, -2.0F, 4.6F, 4.4F, 4.0F, main)
         .part("snout", 0F, 9.4F, -4.8F, -1.2F, -0.7F, -0.9F, 2.4F, 2.1F, 1.8F, light)
         .part("chin", 0F, 8.3F, -4.4F, -1.0F, -0.4F, -0.5F, 2.0F, 1.5F, 1.2F, tip)
         .part("nose", 0F, 9.7F, -4.8F, -0.5F, 0F, -0.6F, 1.0F, 0.7F, 0.6F, 0xFF1A1A1A)
         .part("eyeL", -1.6F, 10.9F, -4.9F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("eyeR", 1.6F, 10.9F, -4.9F, -0.4F, -0.4F, -0.15F, 0.8F, 0.8F, 0.3F, ARGB_EYE_DARK)
         .part("earL", -1.8F, 12.6F, -3.2F, -0.85F, 0F, -0.8F, 1.7F, 3.0F, 1.6F, dark)
         .part("earR", 1.8F, 12.6F, -3.2F, -0.85F, 0F, -0.8F, 1.7F, 3.0F, 1.6F, dark)
         .part("earInnerL", -1.8F, 12.2F, -3.2F, -0.45F, 0F, -0.4F, 0.9F, 2.0F, 0.8F, 0xFFE8B89A)
         .part("earInnerR", 1.8F, 12.2F, -3.2F, -0.45F, 0F, -0.4F, 0.9F, 2.0F, 0.8F, 0xFFE8B89A)
         .part("legFL", -1.6F, 4.6F, -1.5F, -0.85F, -4.6F, -0.85F, 1.7F, 4.6F, 1.7F, dark)
         .part("legFR", 1.6F, 4.6F, -1.5F, -0.85F, -4.6F, -0.85F, 1.7F, 4.6F, 1.7F, dark)
         .part("legBL", -1.6F, 4.6F, 1.5F, -0.85F, -4.6F, -0.85F, 1.7F, 4.6F, 1.7F, dark)
         .part("legBR", 1.6F, 4.6F, 1.5F, -0.85F, -4.6F, -0.85F, 1.7F, 4.6F, 1.7F, dark)
         .part("tail", 0F, 6.8F, 2.0F, -1.4F, -1.2F, 0F, 2.8F, 3.8F, 4.8F, main)
         .part("tailTip", 0F, 7.6F, 2.0F, -1.1F, -0.6F, 3.6F, 2.2F, 2.4F, 1.4F, tip);
      eyeShine(b, 1.6F, 10.9F, -4.9F);
      toes(b, 0, -1.6F, -1.5F, dark);
      toes(b, 1, 1.6F, -1.5F, dark);
      toes(b, 2, -1.6F, 1.5F, dark);
      toes(b, 3, 1.6F, 1.5F, dark);
      return b.build();
   }

   private static void eyeShine(ProceduralModel.Builder b, float ex, float ey, float ez) {
      b.part("shineL", -ex, ey, ez, -0.26F, 0.26F, -0.12F, 0.42F, 0.42F, 0.2F, 0xFFFFFFFF)
       .part("shineR", ex, ey, ez, -0.26F, 0.26F, -0.12F, 0.42F, 0.42F, 0.2F, 0xFFFFFFFF);
   }

   private static void toes(ProceduralModel.Builder b, int leg, float x, float z, int color) {
      b.part("toe" + leg + "a", x - 0.6F, 0.05F, z, 0F, -0.02F, -0.5F, 0.6F, 0.5F, 1.0F, color)
       .part("toe" + leg + "b", x, 0.05F, z, 0F, -0.02F, -0.5F, 0.6F, 0.5F, 1.0F, color)
       .part("toe" + leg + "c", x + 0.6F, 0.05F, z, 0F, -0.02F, -0.5F, 0.6F, 0.5F, 1.0F, color);
   }

   private static void pitch(ProceduralModel model, String node, float value) {
      ProceduralModel.Node n = model.node(node);
      if (n != null) {
         n.setPitch(value);
      }
   }

   private static void yaw(ProceduralModel model, String node, float value) {
      ProceduralModel.Node n = model.node(node);
      if (n != null) {
         n.setYaw(value);
      }
   }

   private static void roll(ProceduralModel model, String node, float value) {
      ProceduralModel.Node n = model.node(node);
      if (n != null) {
         n.setRoll(value);
      }
   }
}
