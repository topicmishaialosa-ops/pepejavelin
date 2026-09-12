package org.blade.feature.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.blade.event.EventTarget;
import org.blade.event.events.input.KeyboardInputEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.InputBindSetting;
import org.blade.utils.FriendManager;
import org.blade.utils.text.ChatUtil;
import org.lwjgl.glfw.GLFW;

public final class ClickFriendFeature extends Feature {
    private final InputBindSetting friendBind = register(new InputBindSetting("Добавить друга", -1));

    public ClickFriendFeature() {
        super("ClickFriend", "Добавляет друга по бинду", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || !friendBind.matches(event.getKey())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!(mc.crosshairPickEntity instanceof Player player)) {
            return;
        }
        String name = player.getGameProfile().name();
        if (FriendManager.INSTANCE.isFriend(name)) {
            FriendManager.INSTANCE.remove(name);
            ChatUtil.info("Убран из друзей: " + name);
        } else {
            FriendManager.INSTANCE.add(name);
            ChatUtil.info("Добавлен в друзья: " + name);
        }
    }
}