package tech.huihui.base.autobuy.item;

import lombok.Generated;
import net.minecraft.item.ItemStack;
import tech.huihui.utility.interfaces.IClient;

public class ItemBuy implements IClient {
   protected ItemStack itemStack;
   protected final String displayName;
   protected final String searchName;
   protected final ItemBuy.Category category;
   protected int price;
   protected boolean enabled;

   public ItemBuy(ItemStack itemStack, String searchName, ItemBuy.Category category) {
      this.itemStack = itemStack;
      this.displayName = searchName;
      this.searchName = searchName;
      this.category = category;
   }

   public ItemBuy(ItemStack itemStack, String displayName, String searchName, ItemBuy.Category category) {
      this.itemStack = itemStack;
      this.displayName = displayName;
      this.searchName = searchName;
      this.category = category;
   }

   public boolean isBuy(ItemStack stack) {
      return stack != null && stack.getItem() == this.itemStack.getItem();
   }

   @Generated
   public ItemStack getItemStack() {
      return this.itemStack;
   }

   @Generated
   public String getDisplayName() {
      return this.displayName;
   }

   @Generated
   public String getSearchName() {
      return this.searchName;
   }

   @Generated
   public ItemBuy.Category getCategory() {
      return this.category;
   }

   @Generated
   public int getPrice() {
      return this.price;
   }

   @Generated
   public void setPrice(int price) {
      this.price = price;
   }

   @Generated
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public static enum Category {
      FUNTIME,
      HOLLYWORLD,
      ANY;

   
      private static ItemBuy.Category[] $values() {
         return new ItemBuy.Category[]{FUNTIME, HOLLYWORLD, ANY};
      }
   }
}
