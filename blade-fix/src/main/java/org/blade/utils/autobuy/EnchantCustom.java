package org.blade.utils.autobuy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class EnchantCustom extends Enchant {
    public EnchantCustom(String name, String checked, int minLevel) {
        super(name, checked, minLevel);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains("custom-enchantments") && nbt.getListOrEmpty("custom-enchantments").size() > 0) {
                ListTag customEnchants = nbt.getList("custom-enchantments").orElse(null);
                if (customEnchants != null) {
                    for (int i = 0; i < customEnchants.size(); i++) {
                        CompoundTag ench = customEnchants.getCompound(i).orElse(null);
                        if (ench == null) {
                            continue;
                        }
                        String type = ench.getString("type").orElse("");
                        int level = ench.getInt("level").orElse(0);
                        if (type.equals(this.checked)) {
                            return level >= this.minLevel;
                        }
                    }
                }
            }
        }
        return false;
    }
}