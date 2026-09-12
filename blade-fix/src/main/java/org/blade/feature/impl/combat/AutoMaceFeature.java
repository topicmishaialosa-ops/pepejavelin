package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.blade.event.EventTarget;
import org.blade.event.events.packet.PacketSendEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

public final class AutoMaceFeature extends Feature {
    public static final AutoMaceFeature INSTANCE = new AutoMaceFeature();

    private boolean attacking = false;

    public AutoMaceFeature() {
        super("AutoMace", "Автоматически бьет булавой и возвращает предмет", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onPacketSend(PacketSendEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || event.getPhase() == PacketSendEvent.Phase.POST) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ServerboundInteractPacket)) {
            return;
        }

        if (attacking) {
            return;
        }

        int maceSlot = findItemInHotbar(Items.MACE);
        if (maceSlot == -1) {
            return;
        }

        int currentSlot = player.getInventory().getSelectedSlot();
        if (maceSlot == currentSlot) {
            return;
        }

        event.cancel();

        attacking = true;
        player.getInventory().setSelectedSlot(maceSlot);
        player.connection.send(new ServerboundSetCarriedItemPacket(maceSlot));
        player.connection.send(packet);
        player.getInventory().setSelectedSlot(currentSlot);
        player.connection.send(new ServerboundSetCarriedItemPacket(currentSlot));
        attacking = false;
    }

    private int findItemInHotbar(Item item) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return -1;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}