package tech.huihui.client.modules.impl.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "ChunkAnimator",
   category = Category.RENDER,
   description = "Чанки плавно появляются"
)
public final class ChunkAnimator extends Module {
   public static final ChunkAnimator INSTANCE = new ChunkAnimator();
   private final NumberSetting duration = new NumberSetting("Длительность (мс)", 800.0F, 100.0F, 3000.0F, 50.0F);
   private final NumberSetting lift = new NumberSetting("Подъём (блоков)", 30.0F, 2.0F, 200.0F, 1.0F);
   private final Map<Object, Long> spawnTimes = new ConcurrentHashMap<>();

   private ChunkAnimator() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.spawnTimes.clear();
   }

   public void onSectionPosSet(long sectionPos, Object chunk) {
      if (!this.isEnabled()) {
         return;
      }
      if (sectionPos == 0L) {
         this.spawnTimes.remove(chunk);
         return;
      }
      this.spawnTimes.put(chunk, System.currentTimeMillis());
   }

   public void onChunkDelete(Object chunk) {
      this.spawnTimes.remove(chunk);
   }

   public float getProgress(Object chunk) {
      Long start = this.spawnTimes.get(chunk);
      if (start == null) {
         return 1.0F;
      }
      float p = (float)(System.currentTimeMillis() - start) / this.duration.getCurrent();
      if (p >= 1.0F) {
         this.spawnTimes.remove(chunk);
         return 1.0F;
      }
      return p;
   }

   public int getLiftBlocks() {
      return (int)this.lift.getCurrent();
   }
}
