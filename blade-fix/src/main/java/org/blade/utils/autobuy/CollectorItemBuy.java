package org.blade.utils.autobuy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CollectorItemBuy extends ItemBuy {
    private final ItemBuy matcher;
    private final List<String> loreLines = new ArrayList<>();
    private final List<PotionRequirement> potionRequirements = new ArrayList<>();
    private int count;
    private boolean active;
    private boolean scan;

    public CollectorItemBuy(ItemStack itemStack, String searchName, Category category, int count, boolean active, boolean scan) {
        this(itemStack, searchName, searchName, category, count, active, scan);
    }

    public CollectorItemBuy(ItemStack itemStack, String displayName, String searchName, Category category, int count, boolean active, boolean scan) {
        super(itemStack, displayName, searchName, category);
        this.matcher = null;
        this.count = count;
        this.active = active;
        this.scan = scan;
    }

    public CollectorItemBuy(ItemBuy matcher, String displayName, String searchName, int count, boolean active, boolean scan) {
        super(matcher.getItemStack(), displayName, searchName, matcher.getCategory());
        this.matcher = matcher;
        this.count = count;
        this.active = active;
        this.scan = scan;
    }

    public CollectorItemBuy lore(String... lines) {
        for (String line : lines) {
            if (line != null && !line.isEmpty()) {
                this.loreLines.add(line);
            }
        }
        return this;
    }

    public CollectorItemBuy potion(PotionRequirement requirement) {
        this.potionRequirements.add(requirement);
        return this;
    }

    @Override
    public boolean isBuy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (this.matcher != null ? !this.matcher.isBuy(stack) : !super.isBuy(stack)) {
            return false;
        }
        if (!this.loreLines.isEmpty() && !this.matchesLore(stack)) {
            return false;
        }
        return this.potionRequirements.isEmpty() || this.matchesPotions(stack);
    }

    private boolean matchesLore(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        String tooltip = stack.getTooltipLines(Item.TooltipContext.EMPTY, mc.player, TooltipFlag.NORMAL)
                .stream()
                .skip(1L)
                .map(line -> normalize(line.getString()))
                .reduce((left, right) -> left + " " + right)
                .orElse("");
        for (String line : this.loreLines) {
            String needle = normalize(line);
            if (!tooltip.contains(needle)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesPotions(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        Iterable<MobEffectInstance> effects = contents.getAllEffects();
        for (PotionRequirement requirement : this.potionRequirements) {
            boolean found = false;
            for (MobEffectInstance effect : effects) {
                if (effect.getEffect().equals(requirement.effect())
                        && effect.getAmplifier() + 1 == requirement.level()
                        && effect.getDuration() >= requirement.duration()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String text) {
        return text.replaceAll("§.", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    public boolean isPotion() {
        return this.getItemStack().getItem() == Items.POTION || this.getItemStack().getItem() == Items.SPLASH_POTION;
    }

    public boolean canStackInInventory() {
        return this.getItemStack().getMaxStackSize() > 1
                || this.getItemStack().getItem() == Items.TOTEM_OF_UNDYING
                || this.getItemStack().getItem() == Items.SPLASH_POTION
                || this.getItemStack().getItem() == Items.POTION;
    }

    public int getMaxCount() {
        return this.getItemStack().getItem() == Items.POTION || this.getItemStack().getItem() == Items.SPLASH_POTION
                ? 16
                : this.getItemStack().getMaxStackSize();
    }

    public ItemBuy getMatcher() {
        return this.matcher;
    }

    public List<String> getLoreLines() {
        return this.loreLines;
    }

    public List<PotionRequirement> getPotionRequirements() {
        return this.potionRequirements;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isScan() {
        return this.scan;
    }

    public void setScan(boolean scan) {
        this.scan = scan;
    }

    public record PotionRequirement(Holder<MobEffect> effect, int level, int duration) {
    }
}