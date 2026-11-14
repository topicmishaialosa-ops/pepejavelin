package org.blade.feature.impl.visual;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ButtonSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CosmeticsFeature extends Feature {
    public static final String NONE = "Нет";

    public enum Category {
        HATS("Шляпы"),
        GLASSES("Очки"),
        DECORATIONS("Украшения"),
        WINGS("Крылья"),
        BODY("Тело"),
        SHOES("Обувь"),
        PETS("Питомцы"),
        SWORDS("Текстуры меча");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return this.displayName;
        }
    }

    public record Cosmetic(Category category, String name, int primary, int secondary, String capeFile) {
    }

    private static final List<Cosmetic> COSMETICS = List.of(
            new Cosmetic(Category.HATS, "Нимб", 0xFFF2C94C, 0x66F2C94C, null),
            new Cosmetic(Category.HATS, "Корона", 0xFFFFD700, 0x88FFD700, null),
            new Cosmetic(Category.HATS, "Рога", 0xFFE05A5A, 0x66E05A5A, null),

            new Cosmetic(Category.GLASSES, "Тёмные", 0xFF1A1A1A, 0xAA1A1A1A, null),
            new Cosmetic(Category.GLASSES, "Неон", 0xFF35E6FF, 0x8835E6FF, null),

            new Cosmetic(Category.DECORATIONS, "Шарф", 0xFFE05A5A, 0x88E05A5A, null),
            new Cosmetic(Category.DECORATIONS, "Цепь", 0xFFD4AF37, 0x88D4AF37, null),
            new Cosmetic(Category.DECORATIONS, "Плащ", 0xFF7A5CFF, 0xAA7A5CFF, "glass.png"),
            new Cosmetic(Category.DECORATIONS, "Плащ ASCII", 0xFFFFFFFF, 0xAAFFFFFF, "ascii.png"),

            new Cosmetic(Category.WINGS, "Ангел", 0xFFFFFFFF, 0x88FFFFFF, null),
            new Cosmetic(Category.WINGS, "Демон", 0xFF8B1E3F, 0x888B1E3F, null),
            new Cosmetic(Category.WINGS, "Кибер", 0xFF35E6FF, 0x8835E6FF, null),

            new Cosmetic(Category.BODY, "Аура", 0xFF8A5CFF, 0x668A5CFF, null),
            new Cosmetic(Category.BODY, "Наплечники", 0xFFC0C6D4, 0x88C0C6D4, null),

            new Cosmetic(Category.SHOES, "Неон", 0xFF35E6FF, 0x8835E6FF, null),
            new Cosmetic(Category.SHOES, "Золото", 0xFFFFD700, 0x88FFD700, null),

            new Cosmetic(Category.PETS, "Кристалл", 0xFF8A5CFF, 0x888A5CFF, null),
            new Cosmetic(Category.PETS, "Сфера", 0xFF35E6FF, 0x8835E6FF, null),
            new Cosmetic(Category.PETS, "Киберпёс", 0xFF303D55, 0xFF59E9F5, null),
            new Cosmetic(Category.PETS, "Лунный волк", 0xFFCBD6ED, 0xFF8A9FFF, null),
            new Cosmetic(Category.PETS, "Дракон Бездны", 0xFF30243E, 0xFFD579FF, null),
            new Cosmetic(Category.PETS, "Ледяной дракон", 0xFF9ABDD5, 0xFF72F0FF, null),
            new Cosmetic(Category.PETS, "Неоновый ара", 0xFF4052A0, 0xFF56EDD3, null),
            new Cosmetic(Category.PETS, "Феникс", 0xFFCF483E, 0xFFFFBD55, null),
            new Cosmetic(Category.PETS, "Лисёнок", 0xFFC77540, 0xFFFFE4B6, null),
            new Cosmetic(Category.PETS, "Кот-астронавт", 0xFF69758F, 0xFFA1D9F4, null),

            new Cosmetic(Category.SWORDS, "Кровавый", 0xFFFF3355, 0x88FF3355, null),
            new Cosmetic(Category.SWORDS, "Ледяной", 0xFF66D9FF, 0x8866D9FF, null),
            new Cosmetic(Category.SWORDS, "Золотой", 0xFFFFC94C, 0x88FFC94C, null),
            new Cosmetic(Category.SWORDS, "Незеритовый палаш", 0xFF6B586D, 0xFF71E9F5, null),
            new Cosmetic(Category.SWORDS, "Катана Бездны", 0xFF7F668F, 0xFFD63C68, null),
            new Cosmetic(Category.SWORDS, "Катана Сакура", 0xFFDB96BA, 0xFFEC719E, null),
            new Cosmetic(Category.SWORDS, "Ледяная катана", 0xFFACCBD9, 0xFF71E9F5, null)
    );

    public final BooleanSetting otherPlayers = register(new BooleanSetting("Другие игроки", false));
    public final BooleanSetting firstPerson = register(new BooleanSetting("Показывать от 1 лица", true));
    public final NumberSetting scale = register(new NumberSetting("Масштаб", 1.0, 0.5, 2.0, 0.05, "x"));
    public final BooleanSetting swordNetherite = register(new BooleanSetting("Незеритовый меч", true));
    public final BooleanSetting swordDiamond = register(new BooleanSetting("Алмазный меч", true));
    public final BooleanSetting swordIron = register(new BooleanSetting("Железный меч", true));
    public final BooleanSetting swordGold = register(new BooleanSetting("Золотой меч", true));
    public final BooleanSetting swordStone = register(new BooleanSetting("Каменный меч", true));
    public final BooleanSetting swordWood = register(new BooleanSetting("Деревянный меч", true));
    public final BooleanSetting swordOther = register(new BooleanSetting("Другие мечи", true));
    public final ButtonSetting openMenu = register(new ButtonSetting("Меню косметики", "Открыть", CosmeticsScreen::open));

    public final List<SwordTarget> swordTargets = List.of(
            new SwordTarget("Незерит", swordNetherite),
            new SwordTarget("Алмаз", swordDiamond),
            new SwordTarget("Железо", swordIron),
            new SwordTarget("Золото", swordGold),
            new SwordTarget("Камень", swordStone),
            new SwordTarget("Дерево", swordWood),
            new SwordTarget("Другие", swordOther)
    );

    public record SwordTarget(String label, BooleanSetting setting) {
    }

    private final Map<Category, ModeSetting> selections = new EnumMap<>(Category.class);

    public CosmeticsFeature() {
        super("Cosmetics", "Косметические украшения на игрока", FeatureCategory.VISUAL, BindSetting.UNBOUND);
        for (Category category : Category.values()) {
            List<String> modes = new ArrayList<>();
            modes.add(NONE);
            for (Cosmetic cosmetic : COSMETICS) {
                if (cosmetic.category() == category) {
                    modes.add(cosmetic.name());
                }
            }
            ModeSetting setting = register(new ModeSetting(category.displayName(), NONE, modes.toArray(String[]::new)));
            setting.visibleWhen(() -> false);
            this.selections.put(category, setting);
        }
    }

    public static CosmeticsFeature getEnabled() {
        return FeatureManager.INSTANCE.getEnabled(CosmeticsFeature.class);
    }

    public ModeSetting selection(Category category) {
        return this.selections.get(category);
    }

    public List<Cosmetic> cosmetics(Category category) {
        List<Cosmetic> result = new ArrayList<>();
        for (Cosmetic cosmetic : COSMETICS) {
            if (cosmetic.category() == category) {
                result.add(cosmetic);
            }
        }
        return result;
    }

    public Cosmetic selected(Category category) {
        ModeSetting setting = this.selections.get(category);
        if (setting == null || NONE.equals(setting.getValue())) {
            return null;
        }
        String name = setting.getValue();
        for (Cosmetic cosmetic : COSMETICS) {
            if (cosmetic.category() == category && cosmetic.name().equals(name)) {
                return cosmetic;
            }
        }
        return null;
    }

    public int equippedCount() {
        int count = 0;
        for (ModeSetting setting : this.selections.values()) {
            if (!NONE.equals(setting.getValue())) {
                count++;
            }
        }
        return count;
    }

    public void resetAll() {
        for (ModeSetting setting : this.selections.values()) {
            setting.setValue(NONE);
        }
    }

    public String capeFile() {
        Cosmetic cape = selected(Category.DECORATIONS);
        return cape != null ? cape.capeFile() : null;
    }

    public float scaleValue() {
        return this.scale.getValue().floatValue();
    }

    public boolean renderOthers() {
        return this.otherPlayers.getValue();
    }

    public boolean showFirstPerson() {
        return this.firstPerson.getValue();
    }

    public boolean appliesToSword(ItemStack stack) {
        if (stack.is(Items.NETHERITE_SWORD)) return this.swordNetherite.getValue();
        if (stack.is(Items.DIAMOND_SWORD)) return this.swordDiamond.getValue();
        if (stack.is(Items.IRON_SWORD)) return this.swordIron.getValue();
        if (stack.is(Items.GOLDEN_SWORD)) return this.swordGold.getValue();
        if (stack.is(Items.STONE_SWORD)) return this.swordStone.getValue();
        if (stack.is(Items.WOODEN_SWORD)) return this.swordWood.getValue();
        return stack.is(ItemTags.SWORDS) && this.swordOther.getValue();
    }
}
