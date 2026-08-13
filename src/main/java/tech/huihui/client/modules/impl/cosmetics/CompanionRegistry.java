package tech.huihui.client.modules.impl.cosmetics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompanionRegistry {
   private static final Map<String, Companion> BY_ID = new HashMap<>();

   public static final List<Companion> ALL = List.of(
      new Companion("chase", "Чейз", 0xFF1F6FE0, 1.9F, false, CompanionModels::puppyChase, CompanionModels.quadFloppyEars()),
      new Companion("marshall", "Маршалл", 0xFFE0362C, 1.9F, false, CompanionModels::puppyMarshall, CompanionModels.quadFloppyEars()),
      new Companion("skye", "Скай", 0xFFF2A3C0, 1.9F, false, CompanionModels::puppySkye, CompanionModels.quadFloppyEars()),
      new Companion("rubble", "Крепыш", 0xFF8C7A66, 1.9F, false, CompanionModels::puppyRubble, CompanionModels.quadFloppyEars()),
      new Companion("zuma", "Зума", 0xFFF28A2E, 1.9F, false, CompanionModels::puppyZuma, CompanionModels.quadFloppyEars()),
      new Companion("rocky", "Рокки", 0xFF5FA84F, 1.9F, false, CompanionModels::puppyRocky, CompanionModels.quadFloppyEars()),
      new Companion("kesha", "Кеша", 0xFFF5F2EC, 1.5F, false, CompanionModels::bearKesha, CompanionModels.bearAnim()),
      new Companion("tuchka", "Тучка", 0xFF8A5A33, 1.5F, false, CompanionModels::bearTuchka, CompanionModels.bearAnim()),
      new Companion("sonya", "Соня", 0xFFF0A8C0, 1.5F, false, CompanionModels::bearSonya, CompanionModels.bearAnim()),
      new Companion("pig", "Свинья", 0xFFF0A8A0, 0.42F, false, CompanionModels::minecraftPig, CompanionModels.quadNoEars()),
      new Companion("wolf", "Собака", 0xFF9A9A9A, 0.42F, false, CompanionModels::minecraftWolf, CompanionModels.quadPointyEars()),
      new Companion("dachshund", "Такса", 0xFFA9713F, 0.46F, false, CompanionModels::minecraftDachshund, CompanionModels.quadFloppyEars()),
      new Companion("cat", "Кошка", 0xFFE8A050, 0.38F, false, CompanionModels::minecraftCat, CompanionModels.catAnim()),
      new Companion("chicken", "Курица", 0xFFF2F0EA, 0.46F, false, CompanionModels::minecraftChicken, CompanionModels.chickenAnim()),
      new Companion("cow", "Корова", 0xFFF2F0EA, 0.44F, false, CompanionModels::minecraftCow, CompanionModels.quadNoEars()),
      new Companion("rabbit", "Кролик", 0xFFE8E4DC, 0.36F, true, CompanionModels::minecraftRabbit, CompanionModels.hopAnim()),
      new Companion("panda", "Панда", 0xFFF5F2EC, 0.42F, false, CompanionModels::minecraftPanda, CompanionModels.quadPointyEars()),
      new Companion("sheep", "Овца", 0xFFE8E4D8, 0.42F, false, CompanionModels::minecraftSheep, CompanionModels.quadNoEars()),
      new Companion("fox", "Лиса", 0xFFE07A2E, 0.38F, false, CompanionModels::minecraftFox, CompanionModels.quadPointyEars()),
      new Companion("patrick", "Патрик", 0xFFF492C0, 1.15F, false, CompanionModels::patrickStar, CompanionModels.patrickWobble())
   );

   static {
      for (Companion companion : ALL) {
         BY_ID.put(companion.id, companion);
      }
   }

   private CompanionRegistry() {
   }

   public static Companion byId(String id) {
      return BY_ID.get(id);
   }
}
