package tech.huihui.base.autobuy.item;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import tech.huihui.utility.interfaces.IClient;

public class CollectorItemBuy extends ItemBuy {
   private final ItemBuy matcher;
   private final List<String> loreLines = new ArrayList<>();
   private final List<PotionRequirement> potionRequirements = new ArrayList<>();
   private int count;
   private boolean active;
   private boolean scan;

   public CollectorItemBuy(ItemStack itemStack, String searchName, ItemBuy.Category category, int count, boolean active, boolean scan) {
      this(itemStack, searchName, searchName, category, count, active, scan);
   }

   public CollectorItemBuy(ItemStack itemStack, String displayName, String searchName, ItemBuy.Category category, int count, boolean active, boolean scan) {
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

      if (!this.potionRequirements.isEmpty() && !this.matchesPotions(stack)) {
         return false;
      }

      return true;
   }

   private boolean matchesLore(ItemStack stack) {
      String tooltip = stack.getTooltip(Item.TooltipContext.DEFAULT, IClient.mc.player, TooltipType.BASIC).stream()
         .skip(1L)
         .map(line -> normalize(line.getString()))
         .collect(java.util.stream.Collectors.joining(" "));
      for (String line : this.loreLines) {
         String needle = normalize(line);
         if (!tooltip.contains(needle)) {
            return false;
         }
      }

      return true;
   }

   private boolean matchesPotions(ItemStack stack) {
      PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
      if (contents == null) {
         return false;
      }

      Iterable<StatusEffectInstance> effects = contents.getEffects();
      for (PotionRequirement requirement : this.potionRequirements) {
         boolean found = false;
         for (StatusEffectInstance effect : effects) {
            if (effect.getEffectType().equals(requirement.effect()) && effect.getAmplifier() + 1 == requirement.level() && effect.getDuration() >= requirement.duration()) {
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
      return text.replaceAll("§.", "").toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
   }

   public boolean isPotion() {
      return this.getItemStack().getItem() == Items.POTION || this.getItemStack().getItem() == Items.SPLASH_POTION;
   }

   public boolean canStackInInventory() {
      return this.getItemStack().getMaxCount() > 1
         || this.getItemStack().getItem() == Items.TOTEM_OF_UNDYING
         || this.getItemStack().getItem() == Items.SPLASH_POTION
         || this.getItemStack().getItem() == Items.POTION;
   }

   public int getMaxCount() {
      return this.getItemStack().getItem() == Items.POTION || this.getItemStack().getItem() == Items.SPLASH_POTION
         ? 16
         : this.getItemStack().getMaxCount();
   }

   @Generated
   public ItemBuy getMatcher() {
      return this.matcher;
   }

   @Generated
   public List<String> getLoreLines() {
      return this.loreLines;
   }

   @Generated
   public List<PotionRequirement> getPotionRequirements() {
      return this.potionRequirements;
   }

   @Generated
   public int getCount() {
      return this.count;
   }

   @Generated
   public void setCount(int count) {
      this.count = count;
   }

   @Generated
   public boolean isActive() {
      return this.active;
   }

   @Generated
   public void setActive(boolean active) {
      this.active = active;
   }

   @Generated
   public boolean isScan() {
      return this.scan;
   }

   @Generated
   public void setScan(boolean scan) {
      this.scan = scan;
   }

   public record PotionRequirement(RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int level, int duration) {
   }
}
