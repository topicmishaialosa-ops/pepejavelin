package tech.huihui.utility.game.server;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import tech.huihui.base.autobuy.enchantes.Enchant;
import tech.huihui.base.autobuy.enchantes.custom.EnchantCustom;
import tech.huihui.base.autobuy.enchantes.minecraft.EnchantVanilla;
import tech.huihui.utility.mixin.accessors.ItemStackAccessor;

public final class AutoBuyUtil {
   private static Pattern patternFuntime = Pattern.compile("\\$\\s*([0-9][\\d,]*)");
   private static Pattern patternHollyWorld = Pattern.compile("Цена:(?:.*?\\{\"text\":\"([\\d ]+)\")");
   private static final Map<ItemStack, NbtCompound> nbtCompoundMap = new HashMap();
   public static List<String> testBypass = new ArrayList();

   public static int getPrice(String nbt) {
      Matcher matcher = patternFuntime.matcher(nbt);
      String amount;
      if (matcher.find()) {
         amount = matcher.group(1);
         String price = amount.replace(",", "");
         return Integer.parseInt(price);
      } else {
         matcher = patternHollyWorld.matcher(nbt);
         if (matcher.find()) {
            amount = matcher.group(1).replaceAll(" ", "");
            return Integer.parseInt(amount);
         } else {
            return Integer.MAX_VALUE;
         }
      }
   }

   public static boolean checkDon(ItemStack itemStack) {
      return itemStack.getCustomName().getString().contains("★");
   }

   public static int getPrice(ItemStack itemStack) {
      String nbt = getNBT(itemStack);
      return getPrice(nbt);
   }

   public static String getNBT(ItemStack itemStack) {
      return getTag(itemStack).toString();
   }

   public static String getKey(ItemStack itemStack) {
      System.out.println(getNBT(itemStack));
      NbtComponent customData = (NbtComponent)itemStack.get(DataComponentTypes.CUSTOM_DATA);
      if (customData != null) {
         System.out.println(customData.getNbt().getKeys());
         System.out.println(itemStack.getItem());
         if (customData.getNbt().contains("kringeItems")) {
            NbtElement customEnchants = customData.getNbt().get("kringeItems");
            MinecraftClient.getInstance().keyboard.setClipboard(customEnchants.toString());
            return customEnchants.toString();
         }
      }

      return "";
   }

   public static NbtCompound getTag(ItemStack stack) {
      MergedComponentMap components = ((ItemStackAccessor)(Object)stack).getComponents();
      ComponentChanges changes = components.getChanges();
      World world = MinecraftClient.getInstance().world;
      return world == null ? new NbtCompound() : (NbtCompound)nbtCompoundMap.computeIfAbsent(stack, (itemStack) -> {
         return (NbtCompound)ComponentChanges.CODEC.encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes).getOrThrow();
      });
   }

   public static String getTagFuntimeNotTempElements(ItemStack stack) {
      MergedComponentMap components = ((ItemStackAccessor)(Object)stack).getComponents();
      ComponentChanges changes = components.getChanges();
      World world = MinecraftClient.getInstance().world;
      return world == null ? "" : ((NbtCompound)nbtCompoundMap.computeIfAbsent(stack, (itemStack) -> {
         return (NbtCompound)ComponentChanges.CODEC.encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes).getOrThrow();
      })).toString().replaceAll(",?\\s*PublicBukkitValues:\\{[^}]*\\}", "").replaceAll("'\\{[^']*Истeкaeт:[^']*\\}',?", "").replaceAll(",?UUID:\\[I;[-0-9]+,[-0-9]+,[-0-9]+,[-0-9]+]", "").replaceAll("minecraft:[0-9a-f\\-]{36}", "minecraft:UUID");
   }

   public static ArrayList<Enchant> getEnchants(ItemStack stack) {
      ArrayList<Enchant> enchantsBuy = new ArrayList();
      NbtComponent customData = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      String type;
      if (customData != null && customData.getNbt().contains("Enchantments", 9)) {
         NbtList customEnchants = customData.getNbt().getList("Enchantments", 10);

         for(int i = 0; i < customEnchants.size(); ++i) {
            NbtCompound ench = customEnchants.getCompound(i);
            type = ench.getString("id");
            int level = ench.getInt("lvl");
            enchantsBuy.add(new EnchantCustom(type, type, level));
         }
      }

      ItemEnchantmentsComponent enchants = stack.getEnchantments();
      Iterator var9 = enchants.getEnchantmentEntries().iterator();

      while(var9.hasNext()) {
         Entry<RegistryEntry<Enchantment>> entry = (Entry)var9.next();
         type = ((RegistryKey)((RegistryEntry)entry.getKey()).getKey().get()).getValue().toString();
         enchantsBuy.add(new EnchantVanilla(type, type, entry.getIntValue()));
      }

      return enchantsBuy;
   }

   public static boolean isAuction(ScreenHandler handledScreen) {
      return handledScreen.slots.size() == 90 && handledScreen.getSlot(49).getStack().getItem() == Items.NETHER_STAR;
   }

   public static String getServer() {
      return MinecraftClient.getInstance().getCurrentServerEntry() == null
         ? ""
         : MinecraftClient.getInstance().getCurrentServerEntry().address;
   }

   public static boolean isFuntimeServer() {
      return getServer().toLowerCase().contains("funtime");
   }

   public static boolean isHollyworldServer() {
      String server = getServer().toLowerCase();
      return server.contains("hollyworld") || server.contains("holyworld");
   }

   public static int getServerPrice(ItemStack itemStack) {
      return getPrice(itemStack) / Math.max(1, itemStack.getCount());
   }

   public static long getBalance() {
      if (MinecraftClient.getInstance().player == null
         || MinecraftClient.getInstance().player.getScoreboardTeam() == null
         || MinecraftClient.getInstance().player.getScoreboardTeam().getPrefix() == null) {
         return -1L;
      }

      String prefix = stripFormatting(MinecraftClient.getInstance().player.getScoreboardTeam().getPrefix().getString());
      if (!prefix.contains("монет")) {
         return -1L;
      }

      Matcher matcher = Pattern.compile("[\\d ]+").matcher(prefix);
      if (matcher.find()) {
         String amount = matcher.group().replaceAll(" ", "").trim();
         if (!amount.isEmpty()) {
            try {
               return Long.parseLong(amount);
            } catch (NumberFormatException var5) {
               return -1L;
            }
         }
      }

      return -1L;
   }

   public static String stripFormatting(String text) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < text.length(); ++i) {
         char c = text.charAt(i);
         if (c == 167) {
            ++i;
         } else {
            builder.append(c);
         }
      }

      return builder.toString();
   }

   public static boolean isWaitBuy(ScreenHandler handledScreen) {
      return handledScreen.slots.size() == 63 && handledScreen.getSlot(0).getStack().getItem() == Items.LIME_STAINED_GLASS_PANE;
   }

   public static void test(int slotId) {
   }

   @Generated
   private AutoBuyUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
