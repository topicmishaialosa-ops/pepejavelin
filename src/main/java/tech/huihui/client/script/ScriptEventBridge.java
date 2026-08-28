package tech.huihui.client.script;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import tech.huihui.base.events.impl.input.EventChatSend;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.base.events.impl.server.EventChatReceive;
import tech.huihui.base.events.impl.other.EventTick;
import tech.huihui.base.events.impl.render.EventRender2D;
import tech.huihui.base.events.impl.render.EventRender3D;
import tech.huihui.client.script.api.RenderContext2D;
import tech.huihui.client.script.api.RenderContext3D;
import tech.huihui.client.script.api.ScriptAPI;

/**
 * Единая точка приёма событий клиента, перенаправляющая их в колбэки всех скриптов.
 */
public class ScriptEventBridge {

    @EventTarget
    public void onTick(EventTick e) {
        for (ScriptAPI api : ScriptAPI.ALL) {
            for (Runnable r : api.getTickCallbacks()) r.run();
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D e) {
        RenderContext2D ctx = new RenderContext2D(e.getContext());
        for (ScriptAPI api : ScriptAPI.ALL) {
            for (java.util.function.Consumer<RenderContext2D> c : api.getRender2DCallbacks()) c.accept(ctx);
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D e) {
        RenderContext3D ctx = new RenderContext3D(e.getMatrix(), e.getPartialTicks());
        for (ScriptAPI api : ScriptAPI.ALL) {
            for (java.util.function.Consumer<RenderContext3D> c : api.getRender3DCallbacks()) c.accept(ctx);
        }
    }

    @EventTarget
    public void onKey(EventKey e) {
        for (ScriptAPI api : ScriptAPI.ALL) {
            for (java.util.function.Consumer<Integer> c : api.getKeyCallbacks()) c.accept(e.getKeyCode());
        }
    }

    @EventTarget
    public void onChatSend(EventChatSend e) {
        for (ScriptAPI api : ScriptAPI.ALL) {
            for (java.util.function.Consumer<String> c : api.getChatSendCallbacks()) c.accept(e.getMessage());
        }
    }

    @EventTarget
    public void onChatReceive(EventChatReceive e) {
        String msg = e.getMessage().getString();
        for (ScriptAPI api : ScriptAPI.ALL) {
            for (java.util.function.Consumer<String> c : api.getChatReceiveCallbacks()) c.accept(msg);
        }
    }
}
