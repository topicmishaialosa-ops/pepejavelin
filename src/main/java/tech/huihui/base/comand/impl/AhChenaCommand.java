package tech.huihui.base.comand.impl;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.autobuy.item.ItemBuy;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.game.server.AutoBuyUtil;
import tech.huihui.utility.math.Timer;

public class AhChenaCommand extends CommandAbstract {
   private static final long TIMEOUT_MS = 6000L;

   private final Timer timer = new Timer();
   private boolean active;
   private String query;

   public AhChenaCommand() {
      super("ahchena");
      EventManager.register(this);
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      SuggestionProvider<CommandSource> itemNames = (context, suggestionsBuilder) -> {
         LinkedHashSet<String> names = new LinkedHashSet<>();
         for (ItemBuy item : HuihuiClient.getInstance().getAutoBuyManager().getFuntime()) {
            names.add(item.getDisplayName());
            if (item.getSearchName() != null && !item.getSearchName().equals(item.getDisplayName())) {
               names.add(item.getSearchName());
            }
         }
         for (ItemBuy item : HuihuiClient.getInstance().getAutoBuyManager().getVanilla()) {
            names.add(item.getDisplayName());
            if (item.getSearchName() != null && !item.getSearchName().equals(item.getDisplayName())) {
               names.add(item.getSearchName());
            }
         }
         for (String name : names) {
            suggestionsBuilder.suggest(name);
         }
         return suggestionsBuilder.buildFuture();
      };
      builder.executes((context) -> this.start(null));
      builder.then(arg("предмет", StringArgumentType.greedyString()).suggests(itemNames).executes((context) -> {
         return this.start(context.getArgument("предмет", String.class));
      }));
   }

   private int start(String item) {
      if (mc.player == null || mc.getNetworkHandler() == null) {
         MessageUtil.displayInfo("[AH] Сначала зайди в мир");
         return 0;
      }
      String name = item;
      if (name == null || name.isBlank()) {
         name = this.handItemName();
      }
      if (name == null || name.isBlank()) {
         MessageUtil.displayInfo("[AH] В руке нет предмета — укажи название: .ahchena <предмет>");
         return 0;
      }
      this.query = name;
      this.active = true;
      this.timer.reset();
      mc.getNetworkHandler().sendChatCommand("ah search " + name);
      MessageUtil.displayInfo("[AH] Проверяю цену: " + name);
      return 1;
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (!this.active) {
         return;
      }
      if (this.timer.finished(TIMEOUT_MS)) {
         this.active = false;
         MessageUtil.displayInfo("[AH] Таймаут: не удалось получить цену для \"" + this.query + "\"");
         return;
      }
      if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
         return;
      }
      String lowerQuery = this.query == null ? "" : this.query.toLowerCase(Locale.ROOT);
      int containerSlots = Math.min(screen.getScreenHandler().slots.size(), screen.getScreenHandler().getRows() * 9);
      int minPrice = Integer.MAX_VALUE;
      int count = 0;
      for (int i = 0; i < containerSlots; i++) {
         Slot slot = screen.getScreenHandler().slots.get(i);
         if (!slot.hasStack()) {
            continue;
         }
         ItemStack stack = slot.getStack();
         String name = stack.getName().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT);
         if (!lowerQuery.isEmpty() && !name.contains(lowerQuery)) {
            continue;
         }
         int price = AutoBuyUtil.getServerPrice(stack);
         if (price > 0 && price < minPrice) {
            minPrice = price;
         }
         count++;
      }
      if (count == 0) {
         this.active = false;
         mc.execute(() -> mc.setScreen(null));
         MessageUtil.displayInfo("[AH] Лотов с \"" + this.query + "\" не найдено на аукционе");
         return;
      }
      if (minPrice == Integer.MAX_VALUE) {
         this.active = false;
         mc.execute(() -> mc.setScreen(null));
         MessageUtil.displayInfo("[AH] Найдено " + count + " лотов \"" + this.query + "\", но цена не определена");
         return;
      }
      this.active = false;
      mc.execute(() -> mc.setScreen(null));
      MessageUtil.displayInfo("[AH] " + this.query + ": минимальная цена " + String.format(Locale.US, "%,d", minPrice) + " $, лотов: " + count);
   }

   private String handItemName() {
      if (mc.player == null || mc.player.getMainHandStack() == null || mc.player.getMainHandStack().isEmpty()) {
         return null;
      }
      String name = mc.player.getMainHandStack().getName().getString().replaceAll("§.", "").trim();
      return name.isEmpty() ? null : name;
   }
}