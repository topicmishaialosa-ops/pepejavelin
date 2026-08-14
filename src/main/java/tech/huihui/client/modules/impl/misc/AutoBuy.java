package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import tech.huihui.HuihuiClient;
import tech.huihui.base.autobuy.AutoBuyManager;
import tech.huihui.base.autobuy.item.ItemBuy;
import tech.huihui.base.config.AutoBuyConfig;
import tech.huihui.base.events.impl.other.EventTickMovement;
import tech.huihui.base.events.impl.render.EventHandledScreen;
import tech.huihui.base.events.impl.server.EventPacket;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ButtonSetting;
import tech.huihui.utility.game.server.AutoBuyUtil;
import tech.huihui.utility.mixin.accessors.HandledScreenAccessor;

@ModuleAnnotation(name = "AutoBuy", category = Category.MISC, description = "Автоматически скупает выбранные предметы по заданной цене")
public final class AutoBuy extends Module {
   public static final AutoBuy INSTANCE = new AutoBuy();
   private final BooleanSetting reissueSetting = new BooleanSetting("Авто-перевыставление вещей", false);
   private final ButtonSetting editorButton = new ButtonSetting("Открыть редактор", () -> {
      mc.setScreen(new AutoBuyEditorScreen(0));
   });
   private final List<ItemStack> reissueItems = new ArrayList<>();
   private final List<ItemBuy> entries = new ArrayList<>();
   private long lastReset = System.currentTimeMillis();
   private int quietTicks;
   private int reissueTicks;
   private ItemStack lastBought;
   private boolean needOpen;
   private boolean ahWaiting;
   private int lastSyncId = -1;

   private AutoBuy() {
      AutoBuyManager manager = HuihuiClient.getInstance().getAutoBuyManager();
      List<ItemBuy> items = new ArrayList<>();
      if (AutoBuyUtil.isFuntimeServer()) {
         items.addAll(manager.getFuntime());
         items.addAll(manager.getVanilla());
      } else if (AutoBuyUtil.isHollyworldServer()) {
         items.addAll(manager.getHollyworld());
         items.addAll(manager.getVanilla());
      } else {
         items.addAll(manager.getVanilla());
      }

      this.entries.addAll(items);
      AutoBuyConfig.loadAutoBuy(this.entries);
   }

   private boolean elapsed(long delay) {
      return System.currentTimeMillis() - this.lastReset >= delay;
   }

   private void resetCounter() {
      this.lastReset = System.currentTimeMillis();
   }

   @EventTarget
   public void onTick(EventTickMovement event) {
      if (mc.player.age >= 220 && this.ahWaiting && !(mc.currentScreen instanceof GenericContainerScreen) && this.needOpen && mc.player.age % 20 == 0) {
         mc.player.networkHandler.sendChatCommand("ah");
         this.ahWaiting = false;
      }

      this.quietTicks++;
      this.reissueTicks++;
      if (!(mc.currentScreen instanceof GenericContainerScreen screen) || !this.needOpen) {
         return;
      }

      ScreenHandler handler = screen.getScreenHandler();
      String title = screen.getTitle().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT).trim();
      boolean buy = mc.player.age % 2 == 0;
      boolean reissue = this.reissueSetting.isEnabled() && this.elapsed(10000L);
      if (title.contains("аукцион")) {
         boolean found = false;
         for (Slot slot : handler.slots.subList(0, Math.min(45, handler.slots.size()))) {
            ItemStack stack = slot.getStack();
            ContainerComponent shulker = stack.get(DataComponentTypes.CONTAINER);
            ItemBuy find = this.entries.stream().filter(item -> {
               if (!item.isEnabled()) {
                  return false;
               } else if (item.isBuy(stack)) {
                  return this.isBuyable(stack);
               } else if (shulker != null) {
                  return shulker.stream().anyMatch(inner -> item.isBuy(inner)) && this.isBuyable(stack);
               }

               return false;
            }).findFirst().orElse(null);
            if (find != null && buy) {
               found = true;
               this.lastBought = stack.copy();
               this.click(handler, slot.id, SlotActionType.QUICK_MOVE);
               break;
            }
         }

         if (!found && !reissue && this.lastSyncId == handler.syncId) {
            this.click(handler, 49, Math.random() * 100.0 < 25.0 ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP);
            this.lastSyncId = -1;
         }
      } else if ((title.contains("подтверждение покупки") || title.contains("подозрительная цена!")) && buy) {
         this.click(handler, 1, SlotActionType.QUICK_MOVE);
      }

      if (this.reissueSetting.isEnabled() && reissue && mc.currentScreen instanceof HandledScreen<?> handledScreen) {
         if (title.matches(".*а.*у.*к.*ц.*и.*о.*н.*")) {
            if (mc.player.age % 10 == 0) {
               this.click(handledScreen.getScreenHandler(), 46, SlotActionType.PICKUP);
               this.reissueTicks = 0;
            }
         } else if (title.matches(".*х.*р.*а.*н.*и.*л.*и.*щ.*е.*")) {
            if (this.reissueTicks % 20 == 10) {
               this.click(handledScreen.getScreenHandler(), 52, SlotActionType.PICKUP);
            } else if (this.reissueTicks % 20 == 0 && this.reissueTicks > 0) {
               this.click(handledScreen.getScreenHandler(), 46, SlotActionType.PICKUP);
               this.resetCounter();
            }
         }

         this.quietTicks = 0;
      }
   }

   private boolean isBuyable(ItemStack stack) {
      long balance = AutoBuyUtil.getBalance();
      int price = AutoBuyUtil.getServerPrice(stack);
      return balance > 0L && price > 0 && (long) price * (long) Math.max(stack.getCount(), 1) <= balance;
   }

   @EventTarget
   public void onPacket(EventPacket event) {
      if (!event.isReceive() || !this.needOpen) {
         return;
      }

      if (event.getPacket() instanceof InventoryS2CPacket inventoryPacket) {
         if (inventoryPacket.getContents().size() == 90 && this.quietTicks >= 7) {
            int anarchy = (int) (Math.random() * 100.0 <= 50.0 ? Math.random() * 26.0 + 205.0 : Math.random() * 20.0 + 305.0);
            mc.player.networkHandler.sendChatCommand("an" + anarchy);
            this.chat("§7Обнаружили замедление аукциона, переходим на " + anarchy + " анархию");
            this.quietTicks = 0;
            this.ahWaiting = true;
         }
      } else if (event.getPacket() instanceof GameMessageS2CPacket messagePacket) {
         if (this.lastBought != null && messagePacket.content().getString().contains("Вы успешно купили")) {
            if (this.reissueItems.isEmpty() || !ItemStack.areEqual(this.reissueItems.getFirst(), this.lastBought)) {
               this.chat("§aУспешно куплен предмет §c" + this.lastBought.getName().getString()
                  + (this.lastBought.getCount() > 1 ? " ×" + this.lastBought.getCount() : "")
                  + " §7за §c" + String.format(Locale.US, "%,d", AutoBuyUtil.getServerPrice(this.lastBought)) + " §7$");
               this.reissueItems.addFirst(this.lastBought);
            }

            this.lastBought = null;
         }
      } else if (event.getPacket() instanceof OpenScreenS2CPacket openScreenPacket) {
         if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            this.quietTicks = 0;
         }

         this.lastSyncId = openScreenPacket.getSyncId();
      } else if (event.getPacket() instanceof PlaySoundS2CPacket soundPacket) {
         if (soundPacket.getSound().value().id().getPath().equals("block.note_block.basedrum")) {
            this.lastSyncId = mc.player.currentScreenHandler.syncId;
            event.setCancelled(true);
         }
      }
   }

   @EventTarget
   public void onHandledScreen(EventHandledScreen event) {
      if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
         return;
      }

      String title = screen.getTitle().getString().replaceAll("§.", "").toLowerCase(Locale.ROOT).trim();
      if (!title.contains("аукцион")) {
         return;
      }

      HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
      DrawContext context = event.getDrawContext();
      int count = event.getBackgroundHeight() / 18;
      int x = accessor.getX() - 22;
      int y = accessor.getY() + 3;
      int bottom = y + count * 18;
      int[][] edges = new int[][]{
         {x - 2, y, x + 20, bottom, -3750202},
         {x, y - 2, x + 18, bottom + 2, -3750202},
         {x - 1, y - 1, x + 19, y, -3750202},
         {x - 1, bottom, x + 19, bottom + 1, -3750202},
         {x, y - 2, x + 18, y - 1, -1},
         {x - 1, y - 1, x, y, -1},
         {x - 2, y, x - 1, bottom, -1},
         {x, bottom + 1, x + 18, bottom + 2, -11184811},
         {x + 18, bottom, x + 19, bottom + 1, -11184811},
         {x + 19, y, x + 20, bottom, -11184811}
      };

      for (int[] edge : edges) {
         context.fill(edge[0], edge[1], edge[2], edge[3], edge[4]);
      }

      for (int i = 0; i < count; i++) {
         int slotY = y + i * 18;
         context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("container/slot"), x, slotY, 18, 18);
         if (i >= this.reissueItems.size()) {
            continue;
         }

         ItemStack stack = this.reissueItems.get(i);
         context.drawItem(stack, x + 1, slotY + 1);
         if (isInside(event.getMouseX(), event.getMouseY(), x + 1, slotY + 1, 16.0f, 16.0f)) {
            context.fillGradient(RenderLayer.getGuiOverlay(), x + 1, slotY + 1, x + 17, slotY + 17, -2130706433, -2130706433, 0);
            context.drawItemTooltip(mc.textRenderer, stack, event.getMouseX(), event.getMouseY());
         }
      }
   }

   private boolean isInside(int mouseX, int mouseY, int x, int y, float width, float height) {
      return (float) mouseX >= (float) x && (float) mouseX <= (float) x + width && (float) mouseY >= (float) y && (float) mouseY <= (float) y + height;
   }

   private void click(ScreenHandler handler, int slot, SlotActionType action) {
      mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
         handler.syncId, handler.getRevision(), slot, 0, action, handler.getCursorStack().copy(), Int2ObjectMaps.emptyMap()));
      this.quietTicks = 0;
   }

   private void chat(String message) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(message), false);
      }
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.needOpen = true;
   }

   public List<ItemBuy> getEntries() {
      return this.entries;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.needOpen = false;
      this.lastBought = null;
      this.reissueItems.clear();
      AutoBuyConfig.saveAutoBuy(this.entries);
   }
}
