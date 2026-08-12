package tech.huihui.client.modules.impl.cosmetics;

import java.util.function.Supplier;
import tech.huihui.utility.render.model.ProceduralModel;

public final class Companion {
   public final String id;
   public final String displayName;
   public final int accentColor;
   public final float scale;
   public final boolean hops;
   private final Supplier<ProceduralModel> modelFactory;
   private final CompanionAnimator animator;
   private ProceduralModel cachedModel;

   public Companion(String id, String displayName, int accentColor, float scale, boolean hops, Supplier<ProceduralModel> modelFactory, CompanionAnimator animator) {
      this.id = id;
      this.displayName = displayName;
      this.accentColor = accentColor;
      this.scale = scale;
      this.hops = hops;
      this.modelFactory = modelFactory;
      this.animator = animator;
   }

   public ProceduralModel model() {
      if (this.cachedModel == null) {
         this.cachedModel = this.modelFactory.get();
      }
      return this.cachedModel;
   }

   public void animate(ProceduralModel model, CompanionInstance instance, float time, float limb, float walk) {
      if (this.animator != null) {
         this.animator.animate(model, instance, time, limb, walk);
      }
   }
}
