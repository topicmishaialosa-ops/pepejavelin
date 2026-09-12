package org.blade.utils.autobuy;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantVanilla extends Enchant {
    public EnchantVanilla(String name, String checked, int minLevel) {
        super(name, checked, minLevel);
    }

    public EnchantVanilla(String id, int minLevel) {
        this(id, id, minLevel);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        if (this.minLevel <= 0) {
            return true;
        }
        ItemEnchantments enchants = stack.getEnchantments();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            String id = entry.getKey().unwrapKey()
                    .map(key -> key.identifier().toString())
                    .orElse("");
            if (id.contains(this.checked)) {
                return entry.getIntValue() >= this.minLevel;
            }
        }
        return false;
    }
}