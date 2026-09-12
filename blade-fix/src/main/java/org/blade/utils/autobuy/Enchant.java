package org.blade.utils.autobuy;

import net.minecraft.world.item.ItemStack;

public abstract class Enchant {
    protected final String name;
    protected final String checked;
    protected int minLevel;

    protected Enchant(String name, String checked, int minLevel) {
        this.name = name;
        this.checked = checked;
        this.minLevel = minLevel;
    }

    public abstract boolean isEnchanted(ItemStack stack);

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [checked=" + this.checked + ", level=" + this.minLevel + "]";
    }

    public String getName() {
        return this.name;
    }

    public String getChecked() {
        return this.checked;
    }

    public int getMinLevel() {
        return this.minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }
}