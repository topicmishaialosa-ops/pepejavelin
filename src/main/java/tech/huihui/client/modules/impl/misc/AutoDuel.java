package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.client.modules.api.setting.impl.StringSetting;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "AutoDuel",
   category = Category.MISC,
   description = "Автоматически вызывает игроков на дуэль"
)
public final class AutoDuel extends Module {
   private static final String TITLE_KIT_SELECT = "Выбор набора";
   private static final String TITLE_DUEL_SETUP = "Настройка поединка";
   private static final long MENU_DELAY_MS = 150L;

   private final ModeSetting priority = new ModeSetting("Приоритет", "Random", "По алфавиту");
   private final ModeSetting kit = new ModeSetting("Кит", "Щит", "Шипы 3", "Лук", "Тотем", "НоуДебаф", "Шары", "Классик", "Читерский рай", "Незер");
   private final BooleanSetting betToggle = new BooleanSetting("Ставка", false);
   private final StringSetting betAmount = new StringSetting("Сумма ставки", "");
   private final NumberSetting delay = new NumberSetting("Задержка команд (мс)", 500.0F, 200.0F, 9000.0F, 50.0F);

   public static final AutoDuel INSTANCE = new AutoDuel();

   private final List<String> sentPlayers = new ArrayList<>();
   private final Timer actionTimer = new Timer();
   private final Timer menuTimer = new Timer();
   private final SecureRandom random = new SecureRandom();
   private boolean wasInMenu;
   private Vec3d lastTickPos;

   private AutoDuel() {
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.sentPlayers.clear();
      this.wasInMenu = false;
      this.lastTickPos = null;
      this.actionTimer.reset();
      this.menuTimer.reset();
   }

   @EventTarget
   @Native
   private void onUpdate(EventUpdate e) {
      if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null || mc.interactionManager == null) {
         this.lastTickPos = null;
         return;
      }

      Vec3d currentPos = mc.player.getPos();
      if (this.lastTickPos != null && currentPos.distanceTo(this.lastTickPos) > 10.0D) {
         this.setToggled(false);
         return;
      }
      this.lastTickPos = currentPos;

      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.handleMenu((GenericContainerScreen) mc.currentScreen);
         return;
      }

      if (this.wasInMenu) {
         this.wasInMenu = false;
         this.menuTimer.reset();
         return;
      }

      if (!this.actionTimer.finished((long) this.delay.getCurrent())) {
         return;
      }

      String target = this.pickTarget();
      if (target == null) {
         return;
      }

      String command = "duel " + target;
      if (this.betToggle.isEnabled() && !this.betAmount.getValue().isEmpty()) {
         command = command + " " + this.betAmount.getValue();
      }

      mc.getNetworkHandler().sendChatCommand(command);
      this.sentPlayers.add(target);
      this.actionTimer.reset();
   }

   private void handleMenu(GenericContainerScreen screen) {
      this.wasInMenu = true;
      if (!this.menuTimer.finished(MENU_DELAY_MS)) {
         return;
      }

      String title = screen.getTitle().getString();
      if (title.contains(TITLE_KIT_SELECT)) {
         int slotIndex = this.findItemSlotByName(screen, this.kit.get());
         if (slotIndex != -1) {
            this.click(screen, slotIndex);
            this.menuTimer.reset();
         }
      } else if (title.contains(TITLE_DUEL_SETUP)) {
         this.click(screen, 0);
         this.menuTimer.reset();
      }
   }

   private String pickTarget() {
      List<String> playerNames = new ArrayList<>();
      for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
         String name = entry.getProfile().getName();
         if (!name.equals(mc.player.getName().getString()) && !this.sentPlayers.contains(name)) {
            playerNames.add(name);
         }
      }

      if (playerNames.isEmpty()) {
         return null;
      }

      if (this.priority.is("Random")) {
         Collections.shuffle(playerNames, this.random);
      } else {
         Collections.sort(playerNames);
      }

      return playerNames.getFirst();
   }

   private void click(GenericContainerScreen screen, int slot) {
      PlayerInventoryUtil.clickSlot(screen.getScreenHandler().syncId, slot, 0, SlotActionType.PICKUP, false);
   }

   private int findItemSlotByName(GenericContainerScreen screen, String itemName) {
      String lower = itemName.toLowerCase(Locale.ROOT);
      int containerSlots = screen.getScreenHandler().getRows() * 9;
      for (int i = 0; i < containerSlots; i++) {
         Slot slot = screen.getScreenHandler().slots.get(i);
         if (slot.hasStack() && slot.getStack().getName().getString().toLowerCase(Locale.ROOT).contains(lower)) {
            return i;
         }
      }
      return -1;
   }
}
