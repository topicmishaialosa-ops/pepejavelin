package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.level.Render3DUtil;

@ModuleAnnotation(
   name = "ChunkAnimator",
   category = Category.RENDER,
   description = "Чанки плавно появляются"
)
public final class ChunkAnimator extends Module implements IMinecraft {
   public static final ChunkAnimator INSTANCE = new ChunkAnimator();
   private static final long LIVE_MS = 900L;
   private final NumberSetting duration = new NumberSetting("Длительность (мс)", 800.0F, 100.0F, 3000.0F, 50.0F);
   private final NumberSetting lift = new NumberSetting("Подъём (блоков)", 30.0F, 2.0F, 200.0F, 1.0F);
   private final Map<ChunkSectionPos, Long> spawnTimes = new ConcurrentHashMap<>();
   private final ColorRGBA ACCENT = new ColorRGBA(255, 255, 255);

   private ChunkAnimator() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.spawnTimes.clear();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.spawnTimes.clear();
   }

   public void onSectionChanged(int sectionX, int sectionY, int sectionZ, boolean empty) {
      if (!this.isEnabled() || mc.world == null) {
         return;
      }
      if (empty) {
         this.spawnTimes.remove(ChunkSectionPos.from(sectionX, sectionY, sectionZ));
         return;
      }
      ChunkSectionPos pos = ChunkSectionPos.from(sectionX, sectionY, sectionZ);
      long now = System.currentTimeMillis();
      this.spawnTimes.put(pos, now);
   }

   @EventTarget
   private void onRenderWorld(EventRender3D event) {
      if (!this.isEnabled() || mc.world == null || mc.player == null || this.spawnTimes.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      float lifetime = this.duration.getCurrent() + LIVE_MS;

      Iterator<Map.Entry<ChunkSectionPos, Long>> it = this.spawnTimes.entrySet().iterator();
      while (it.hasNext()) {
         Map.Entry<ChunkSectionPos, Long> entry = it.next();
         ChunkSectionPos pos = entry.getKey();
         long start = entry.getValue();
         float age = (float)(now - start);
         if (age >= lifetime) {
            it.remove();
            continue;
         }

         float progress = Math.min(1.0F, age / lifetime);
         float p = easeOutCubic(progress);
         float liftBlocks = this.lift.getCurrent() * (1.0F - p);

         int minX = pos.getMinX();
         int minY = pos.getMinY();
         int minZ = pos.getMinZ();
         double x1 = (double)minX;
         double y1 = (double)minY - (double)liftBlocks;
         double z1 = (double)minZ;
         double x2 = (double)(minX + 16);
         double y2 = (double)(minY + 16);
         double z2 = (double)(minZ + 16);

         int alpha = (int)(255.0F * (1.0F - progress) * 0.35F);
         if (alpha > 0) {
            int color = ACCENT.withAlpha(alpha).getRGB();
            Render3DUtil.drawBox(new Box(x1, y1, z1, x2, y2, z2), color, 1.5F, true, true, false);
         }
      }
   }

   private static float easeOutCubic(float p) {
      float t = 1.0F - p;
      return 1.0F - t * t * t;
   }

   public int getLiftBlocks() {
      return (int)this.lift.getCurrent();
   }
}