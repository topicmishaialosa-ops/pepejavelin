package org.blade.utils.autobuy;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EnchantItemBuy extends ItemBuy {
    protected final List<Enchant> enchants = new ArrayList<>();

    public EnchantItemBuy(ItemStack itemStack, String displayName, String searchName, Category maxSumBuy) {
        super(itemStack, displayName, searchName, maxSumBuy);
    }

    public EnchantItemBuy(ItemStack itemStack, String searchName, Category maxSumBuy) {
        super(itemStack, searchName, maxSumBuy);
    }

    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }
        for (Enchant enchant : this.enchants) {
            if (!enchant.isEnchanted(stack)) {
                return false;
            }
        }
        return true;
    }

    public void addEnchant(Enchant enchant) {
        this.enchants.add(enchant);
    }

    public List<Enchant> getEnchants() {
        return this.enchants;
    }
}