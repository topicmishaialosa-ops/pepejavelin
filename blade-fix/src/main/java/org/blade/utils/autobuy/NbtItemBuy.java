package org.blade.utils.autobuy;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.Map;

public class NbtItemBuy extends ItemBuy {
    private static final String UUID_PATTERN = ",?UUID:\\[I;[-0-9]+,[-0-9]+,[-0-9]+,[-0-9]+]";

    private final Map<String, String> nbtKeyValue;

    public NbtItemBuy(ItemStack itemStack, String displayName, String searchName, Category maxSumBuy) {
        super(itemStack, displayName, searchName, maxSumBuy);
        this.nbtKeyValue = new HashMap<>();
    }

    public NbtItemBuy(ItemStack itemStack, String searchName, Category maxSumBuy, Map<String, String> nbtKeyValue) {
        super(itemStack, searchName, maxSumBuy);
        this.nbtKeyValue = nbtKeyValue;
    }

    public NbtItemBuy(ItemStack itemStack, String searchName, Category maxSumBuy, String key, String value) {
        super(itemStack, searchName, maxSumBuy);
        this.nbtKeyValue = new HashMap<>();
        this.nbtKeyValue.put(key, value);
    }

    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        CompoundTag nbt = customData.copyTag();
        for (Map.Entry<String, String> entry : this.nbtKeyValue.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!nbt.contains(key)) {
                return false;
            }
            Tag tag = nbt.get(key);
            if (tag == null) {
                return false;
            }
            String raw = tag.toString().replaceAll(UUID_PATTERN, "");
            if (!raw.contains(value.replaceAll(UUID_PATTERN, ""))) {
                return false;
            }
        }
        return true;
    }
}