package org.blade.feature.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AntiBotFeature extends Feature {
    public static final AntiBotFeature INSTANCE = new AntiBotFeature();

    private final List<Player> bots = new ArrayList<>();
    private long lastScan = 0L;

    public AntiBotFeature() {
        super("AntiBot", "Отмечает игроков-ботов по надетой кожаной/железной броне без зачарований", FeatureCategory.COMBAT, BindSetting.UNBOUND);
    }

    public void onEnable() {
        bots.clear();
        lastScan = 0L;
    }

    public void onDisable() {
        bots.clear();
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = event.getClient();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastScan >= 10000L) {
            lastScan = now;
            bots.clear();
        }

        Iterator<? extends Player> iterator = mc.level.players().iterator();
        while (iterator.hasNext()) {
            Player other = iterator.next();
            if (other != null && other != player && armorCheck(other) && !bots.contains(other)) {
                bots.add(other);
            }
        }
    }

    public boolean isBot(Player player) {
        return bots.contains(player);
    }

    private boolean armorCheck(Player entity) {
        return (getArmor(entity, EquipmentSlot.HEAD).getItem() == Items.LEATHER_HELMET
                && isNotColored(entity, EquipmentSlot.HEAD)
                && !getArmor(entity, EquipmentSlot.HEAD).isEnchanted())
                || (getArmor(entity, EquipmentSlot.CHEST).getItem() == Items.LEATHER_CHESTPLATE
                && isNotColored(entity, EquipmentSlot.CHEST)
                && !getArmor(entity, EquipmentSlot.CHEST).isEnchanted())
                || (getArmor(entity, EquipmentSlot.LEGS).getItem() == Items.LEATHER_LEGGINGS
                && isNotColored(entity, EquipmentSlot.LEGS)
                && !getArmor(entity, EquipmentSlot.LEGS).isEnchanted())
                || (getArmor(entity, EquipmentSlot.FEET).getItem() == Items.LEATHER_BOOTS
                && isNotColored(entity, EquipmentSlot.FEET)
                && !getArmor(entity, EquipmentSlot.FEET).isEnchanted())
                || (getArmor(entity, EquipmentSlot.CHEST).getItem() == Items.IRON_CHESTPLATE
                && !getArmor(entity, EquipmentSlot.CHEST).isEnchanted())
                || (getArmor(entity, EquipmentSlot.LEGS).getItem() == Items.IRON_LEGGINGS
                && !getArmor(entity, EquipmentSlot.LEGS).isEnchanted());
    }

    private ItemStack getArmor(Player entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot);
    }

    private boolean isNotColored(Player entity, EquipmentSlot slot) {
        return !getArmor(entity, slot).has(DataComponents.DYED_COLOR);
    }
}