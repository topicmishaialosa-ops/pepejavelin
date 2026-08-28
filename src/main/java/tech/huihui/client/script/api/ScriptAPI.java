package tech.huihui.client.script.api;

import net.minecraft.client.MinecraftClient;
import tech.huihui.HuihuiClient;
import tech.huihui.base.modules.ModuleManager;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.setting.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Единый удобный API для скриптов (Java, Lua, JSON). Один экземпляр создаётся
 * на каждый загруженный скрипт и автоматически очищается при выгрузке.
 */
public class ScriptAPI {
    public static final CopyOnWriteArrayList<ScriptAPI> ALL = new CopyOnWriteArrayList<>();

    final public CopyOnWriteArrayList<Runnable> tick = new CopyOnWriteArrayList<>();
    final public CopyOnWriteArrayList<Consumer<RenderContext2D>> render2D = new CopyOnWriteArrayList<>();
    final public CopyOnWriteArrayList<Consumer<RenderContext3D>> render3D = new CopyOnWriteArrayList<>();
    final public CopyOnWriteArrayList<Consumer<Integer>> key = new CopyOnWriteArrayList<>();
    final public CopyOnWriteArrayList<Consumer<String>> chatSend = new CopyOnWriteArrayList<>();
    final public CopyOnWriteArrayList<Consumer<String>> chatReceive = new CopyOnWriteArrayList<>();
    final public CopyOnWriteArrayList<Runnable> enable = new CopyOnWriteArrayList<>();
    final public CopyOnWriteArrayList<Runnable> disable = new CopyOnWriteArrayList<>();

    final List<Module> ownedModules = new ArrayList<>();

    private final String name;

    public ScriptAPI(String name) {
        this.name = name;
        ALL.add(this);
    }

    public List<Runnable> getTickCallbacks() { return tick; }
    public List<Consumer<RenderContext2D>> getRender2DCallbacks() { return render2D; }
    public List<Consumer<RenderContext3D>> getRender3DCallbacks() { return render3D; }
    public List<Consumer<Integer>> getKeyCallbacks() { return key; }
    public List<Consumer<String>> getChatSendCallbacks() { return chatSend; }
    public List<Consumer<String>> getChatReceiveCallbacks() { return chatReceive; }
    public List<Runnable> getEnableCallbacks() { return enable; }
    public List<Runnable> getDisableCallbacks() { return disable; }

    public String getName() {
        return name;
    }

    /* ---------- регистрация колбэков ---------- */

    public void onTick(Runnable r) {
        if (r != null) tick.add(r);
    }

    public void onRender2D(Consumer<RenderContext2D> c) {
        if (c != null) render2D.add(c);
    }

    public void onRender3D(Consumer<RenderContext3D> c) {
        if (c != null) render3D.add(c);
    }

    public void onKey(Consumer<Integer> c) {
        if (c != null) key.add(c);
    }

    public void onChatSend(Consumer<String> c) {
        if (c != null) chatSend.add(c);
    }

    public void onChatReceive(Consumer<String> c) {
        if (c != null) chatReceive.add(c);
    }

    public void onEnable(Runnable r) {
        if (r != null) enable.add(r);
    }

    public void onDisable(Runnable r) {
        if (r != null) disable.add(r);
    }

    /* ---------- действия игрока / клиента ---------- */

    public MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public void print(String msg) {
        tech.huihui.utility.game.other.MessageUtil.displayInfo(msg);
    }

    /* ---------- взаимодействие со слотами инвентаря ---------- */

    /**
     * Клик по слоту текущего контейнера (или инвентаря игрока, если экран не открыт).
     * button: 0 — левый, 1 — правый.
     */
    public void clickSlot(int slot) {
        clickSlot(slot, 0, "pickup");
    }

    public void clickSlot(int slot, int button) {
        clickSlot(slot, button, "pickup");
    }

    /**
     * Клик по слоту с выбором действия:
     * "pickup" (по умолчанию), "quick_move" (shift), "swap" (button = слот хотбара),
     * "clone" (creative), "throw"/"drop" (выброс), "pickup_all".
     */
    public void clickSlot(int slot, int button, String action) {
        var mc = mc();
        if (mc.player == null || mc.interactionManager == null) return;
        var handler = mc.player.currentScreenHandler;
        if (handler == null) return;
        net.minecraft.screen.slot.SlotActionType type;
        switch (action.toLowerCase()) {
            case "quick_move":
            case "shift":
                type = net.minecraft.screen.slot.SlotActionType.QUICK_MOVE;
                break;
            case "swap":
                type = net.minecraft.screen.slot.SlotActionType.SWAP;
                break;
            case "clone":
                type = net.minecraft.screen.slot.SlotActionType.CLONE;
                break;
            case "throw":
            case "drop":
                type = net.minecraft.screen.slot.SlotActionType.THROW;
                break;
            case "pickup_all":
                type = net.minecraft.screen.slot.SlotActionType.PICKUP_ALL;
                break;
            default:
                type = net.minecraft.screen.slot.SlotActionType.PICKUP;
        }
        mc.interactionManager.clickSlot(handler.syncId, slot, button, type, mc.player);
    }

    /** Выбросить предмет из слота (пкм с drop-действием). */
    public void dropSlot(int slot) {
        clickSlot(slot, 1, "throw");
    }

    /** Быстро переместить предмет из слота (shift-клик). */
    public void quickMoveSlot(int slot) {
        clickSlot(slot, 0, "quick_move");
    }

    /** Выбрать слот хотбара (0..8), как будто нажали цифру на клавиатуре. */
    public void selectHotbar(int index) {
        var mc = mc();
        if (mc.player != null && index >= 0 && index < 9) {
            mc.player.getInventory().selectedSlot = index;
        }
    }

    /**
     * Клик по слоту собственного инвентаря игрока, даже если открыт другой экран
     * (сундук/печка и т.п.). В отличие от {@link #clickSlot}, бьёт всегда по
     * инвентарю игрока, а не по открытому контейнеру.
     * Нумерация слотов — как в Inventory: 0..8 — хотбар, 9..35 — основной
     * инвентарь, 36..39 — броня, 40 — отключённый слот (offhand, недоступен).
     */
    public void clickSlotInventory(int slot) {
        clickSlotInventory(slot, 0, "pickup");
    }

    public void clickSlotInventory(int slot, int button) {
        clickSlotInventory(slot, button, "pickup");
    }

    public void clickSlotInventory(int slot, int button, String action) {
        var mc = mc();
        if (mc.player == null || mc.interactionManager == null) return;
        var psh = mc.player.playerScreenHandler;
        if (psh == null) return;
        int pshSlot = toPlayerScreenHandlerSlot(slot);
        if (pshSlot < 0) return;
        net.minecraft.screen.slot.SlotActionType type;
        switch (action.toLowerCase()) {
            case "quick_move":
            case "shift":
                type = net.minecraft.screen.slot.SlotActionType.QUICK_MOVE;
                break;
            case "swap":
                type = net.minecraft.screen.slot.SlotActionType.SWAP;
                break;
            case "clone":
                type = net.minecraft.screen.slot.SlotActionType.CLONE;
                break;
            case "throw":
            case "drop":
                type = net.minecraft.screen.slot.SlotActionType.THROW;
                break;
            case "pickup_all":
                type = net.minecraft.screen.slot.SlotActionType.PICKUP_ALL;
                break;
            default:
                type = net.minecraft.screen.slot.SlotActionType.PICKUP;
        }
        mc.interactionManager.clickSlot(psh.syncId, pshSlot, button, type, mc.player);
    }

    /** Перевод индекса Inventory (0..35 хотбар+инвентарь, 36..39 броня) в слот PlayerScreenHandler. */
    private static int toPlayerScreenHandlerSlot(int inv) {
        if (inv >= 0 && inv <= 8) return inv + 36;   // хотбар 0..8  -> PSH 36..44
        if (inv >= 9 && inv <= 35) return inv;       // инвентарь 9..35 -> PSH 9..35
        if (inv >= 36 && inv <= 39) return inv - 31; // броня 36..39 -> PSH 5..8
        return -1;                                   // offhand/прочее недоступно тут
    }

    public void jump() {
        if (mc().player != null) mc().player.jump();
    }

    public void chat(String msg) {
        if (mc().player != null) mc().player.networkHandler.sendChatMessage(msg);
    }

    public void sendCommand(String cmd) {
        if (mc().player != null) mc().player.networkHandler.sendCommand(cmd);
    }

    public void enableModule(String name) {
        Module m = moduleManager().getModule(name);
        if (m != null && !m.isEnabled()) m.toggle();
    }

    public void disableModule(String name) {
        Module m = moduleManager().getModule(name);
        if (m != null && m.isEnabled()) m.toggle();
    }

    public void toggleModule(String name) {
        Module m = moduleManager().getModule(name);
        if (m != null) m.toggle();
    }

    public Module getModule(String name) {
        return moduleManager().getModule(name);
    }

    /* ---------- создание кастомных модулей ---------- */

    public ScriptModuleBuilder module(String name) {
        return new ScriptModuleBuilder(this, name);
    }

    /* ---------- создание HUD-модулей ---------- */

    public ScriptHudModuleBuilder hud(String name) {
        return new ScriptHudModuleBuilder(this, name);
    }

    /* ---------- внутреннее ---------- */

    private static ModuleManager moduleManager() {
        return HuihuiClient.getInstance().getModuleManager();
    }

    public static ModuleManager getModuleManager() {
        return moduleManager();
    }

    public void dispose() {
        tick.clear();
        render2D.clear();
        render3D.clear();
        key.clear();
        chatSend.clear();
        chatReceive.clear();
        enable.clear();
        disable.clear();
        for (Module m : ownedModules) {
            moduleManager().unregisterModule(m);
        }
        ownedModules.clear();
        ALL.remove(this);
    }
}
