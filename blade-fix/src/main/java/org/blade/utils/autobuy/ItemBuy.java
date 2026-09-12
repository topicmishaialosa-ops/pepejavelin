package org.blade.utils.autobuy;

import net.minecraft.world.item.ItemStack;

public class ItemBuy {
    protected ItemStack itemStack;
    protected final String displayName;
    protected final String searchName;
    protected final Category category;
    protected int price;
    protected boolean enabled;

    public ItemBuy(ItemStack itemStack, String searchName, Category category) {
        this.itemStack = itemStack;
        this.displayName = searchName;
        this.searchName = searchName;
        this.category = category;
    }

    public ItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        this.itemStack = itemStack;
        this.displayName = displayName;
        this.searchName = searchName;
        this.category = category;
    }

    public boolean isBuy(ItemStack stack) {
        return stack != null && stack.getItem() == this.itemStack.getItem();
    }

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getSearchName() {
        return this.searchName;
    }

    public Category getCategory() {
        return this.category;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public enum Category {
        FUNTIME,
        HOLLYWORLD,
        ANY
    }
}