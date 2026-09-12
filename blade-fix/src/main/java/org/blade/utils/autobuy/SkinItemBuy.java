package org.blade.utils.autobuy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.UUID;

public class SkinItemBuy extends ItemBuy {
    private final String skin;

    public SkinItemBuy(String name, Category category, String skin) {
        this(skin, name, name, category);
    }

    public SkinItemBuy(String skin, String displayName, String searchName, Category maxSumBuy) {
        super(new ItemStack(Items.PLAYER_HEAD), displayName, searchName, maxSumBuy);
        this.skin = skin;
        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.properties().put("textures", new Property("textures", skin));
        this.itemStack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
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
        if (!nbt.contains("SkullOwner")) {
            return false;
        }
        Tag skullOwner = nbt.get("SkullOwner");
        return skullOwner != null && skullOwner.toString().contains(this.skin);
    }

    public String getSkin() {
        return this.skin;
    }
}