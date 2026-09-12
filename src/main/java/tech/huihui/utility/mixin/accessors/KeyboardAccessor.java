package tech.huihui.utility.mixin.accessors;

import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Keyboard.class})
public interface KeyboardAccessor {
   @Invoker("onChar")
   void invokeOnChar(long window, int codepoint, int modifiers);
}