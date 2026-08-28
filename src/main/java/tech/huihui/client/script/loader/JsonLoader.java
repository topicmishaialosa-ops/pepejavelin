package tech.huihui.client.script.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tech.huihui.client.script.LoadedScript;
import tech.huihui.client.script.api.HudRenderContext;
import tech.huihui.client.script.api.ScriptAPI;
import tech.huihui.utility.game.other.MessageUtil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * Загрузчик JSON-скриптов (декларативно): описывает модули и HUD-виджеты.
 */
public class JsonLoader {

    public static LoadedScript load(String name, File file, ScriptAPI api) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()));
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        if (root.has("modules")) {
            for (JsonElement e : root.getAsJsonArray("modules")) {
                JsonObject m = e.getAsJsonObject();
                String mName = m.get("name").getAsString();
                String cat = m.has("category") ? m.get("category").getAsString() : "MISC";
                int key = m.has("key") ? m.get("key").getAsInt() : -1;
                var b = api.module(mName).category(cat).key(key);
                if (m.has("onEnable")) b.onEnable(makeAction(api, m.get("onEnable").getAsString()));
                if (m.has("onDisable")) b.onDisable(makeAction(api, m.get("onDisable").getAsString()));
                if (m.has("onTick")) b.onTick(makeAction(api, m.get("onTick").getAsString()));
                b.register();
            }
        }

        if (root.has("hud")) {
            int idx = 0;
            for (JsonElement e : root.getAsJsonArray("hud")) {
                JsonObject h = e.getAsJsonObject();
                String hName = h.has("name") ? h.get("name").getAsString() : ("JSONHUD" + (idx++));
                String type = h.get("type").getAsString();
                double x = h.has("x") ? h.get("x").getAsDouble() : 10;
                double y = h.has("y") ? h.get("y").getAsDouble() : 10;
                double w = h.has("width") ? h.get("width").getAsDouble() : 120;
                double hh = h.has("height") ? h.get("height").getAsDouble() : 20;
                int color = h.has("color") ? h.get("color").getAsInt() : -1;
                final JsonObject fh = h;
                api.hud(hName)
                        .pos(x, y).size(w, hh)
                        .category(h.has("category") ? h.get("category").getAsString() : "RENDER")
                        .key(h.has("key") ? h.get("key").getAsInt() : -1)
                        .onRender(ctx -> drawHud(ctx, type, color, fh))
                        .register();
            }
        }

        return new LoadedScript(name, "json", file, api, null, null);
    }

    private static Runnable makeAction(ScriptAPI api, String action) {
        return () -> {
            if (action.startsWith("chat ")) api.chat(action.substring(5));
            else if (action.startsWith("command ")) api.sendCommand(action.substring(8));
            else if (action.startsWith("enable ")) api.enableModule(action.substring(7).trim());
            else if (action.startsWith("disable ")) api.disableModule(action.substring(8).trim());
        };
    }

    private static void drawHud(HudRenderContext ctx, String type, int color, JsonObject h) {
        double x = ctx.getX();
        double y = ctx.getY();
        if ("text".equals(type)) {
            String t = h.get("text").getAsString();
            ctx.text(replacePlaceholders(t), x, y, color);
        } else if ("coords".equals(type)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                String t = "X: " + (int) mc.player.getX() + " Y: " + (int) mc.player.getY() + " Z: " + (int) mc.player.getZ();
                ctx.text(t, x, y, color);
            }
        }
    }

    private static String replacePlaceholders(String t) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            t = t.replace("{x}", Integer.toString((int) mc.player.getX()))
                    .replace("{y}", Integer.toString((int) mc.player.getY()))
                    .replace("{z}", Integer.toString((int) mc.player.getZ()));
        }
        t = t.replace("{fps}", Integer.toString(mc.getCurrentFps()));
        t = t.replace("{user}", mc.getSession().getUsername());
        if (mc.player != null) {
            PlayerListEntry entry = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
            int ping = entry != null ? entry.getLatency() : 0;
            t = t.replace("{ping}", Integer.toString(ping));
        }
        return t;
    }
}
