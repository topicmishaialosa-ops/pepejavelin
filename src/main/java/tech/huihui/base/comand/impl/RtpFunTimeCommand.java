package tech.huihui.base.comand.impl;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.slot.SlotActionType;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.comand.api.CommandAbstract;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.utility.game.other.MessageUtil;
import tech.huihui.utility.game.player.PlayerInventoryUtil;
import tech.huihui.utility.math.Timer;

public class RtpFunTimeCommand extends CommandAbstract {
   private static final long MENU_DELAY_MS = 150L;
   private static final long RESEND_DELAY_MS = 2000L;

   private final Timer menuTimer = new Timer();
   private boolean active;
   private boolean guiSeen;

   public RtpFunTimeCommand() {
      super("rtpfuntime");
      EventManager.register(this);
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         if (mc.player == null || mc.getNetworkHandler() == null) {
            MessageUtil.displayInfo("[RTP] Сначала зайди в мир");
            return 0;
         }
         this.active = true;
         this.guiSeen = false;
         this.menuTimer.reset();
         mc.getNetworkHandler().sendChatCommand("rtp");
         MessageUtil.displayInfo("[RTP] Запущено: /rtp отправлен");
         return 1;
      });
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (!this.active || mc.player == null || mc.getNetworkHandler() == null) {
         return;
      }

      if (mc.currentScreen instanceof GenericContainerScreen) {
         GenericContainerScreen screen = (GenericContainerScreen) mc.currentScreen;
         if (!this.menuTimer.finished(MENU_DELAY_MS)) {
            return;
         }
         PlayerInventoryUtil.clickSlot(screen.getScreenHandler().syncId, 1, 0, SlotActionType.PICKUP, false);
         this.menuTimer.reset();
         this.guiSeen = true;
         this.active = false;
         MessageUtil.displayInfo("[RTP] Клик по слоту 1");
         return;
      }

      if (this.guiSeen) {
         return;
      }

      if (this.menuTimer.finished(RESEND_DELAY_MS)) {
         mc.getNetworkHandler().sendChatCommand("rtp");
         this.menuTimer.reset();
      }
   }
}
