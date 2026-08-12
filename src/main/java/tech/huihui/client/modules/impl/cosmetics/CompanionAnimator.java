package tech.huihui.client.modules.impl.cosmetics;

import tech.huihui.utility.render.model.ProceduralModel;

@FunctionalInterface
public interface CompanionAnimator {
   void animate(ProceduralModel model, CompanionInstance instance, float time, float limb, float walk);
}
