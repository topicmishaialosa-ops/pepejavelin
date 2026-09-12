package org.blade.feature.impl.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponentType;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.packet.PacketReceiveEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.feature.setting.TextSetting;
import org.blade.utils.autobuy.AutoBuyUtil;
import org.blade.utils.ai.ZaiAi;
import org.blade.utils.inventory.InventoryUtil;
import org.blade.utils.text.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BpAutoFeature extends Feature {
    public static final BpAutoFeature INSTANCE = new BpAutoFeature();
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern PROGRESS_WORDS_RU = Pattern.compile("(\\d+)\\s+из\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRESS_PREFIX = Pattern.compile("прогресс[:\\s]*(\\d+)\\s*/\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRESS_SIMPLE = Pattern.compile("(\\d+)/(\\d+)");
    private static final Pattern PROGRESS_FUZZY = Pattern.compile("(\\d+)\\D*/\\D*(\\d+)");
    private static final String[] MENU_WORDS = new String[]{"задания", "назад", "закрыть", "стрелка", "еженедельн", "ежедневн", "информац", "выйти"};
    private static final int INVENTORY_SIZE = 36;

    private final NumberSetting clickDelay = register(new NumberSetting("Задержка кликов", 15.0D, 5.0D, 60.0D, 1.0D, " т."));
    private final NumberSetting timeout = register(new NumberSetting("Таймаут", 200.0D, 40.0D, 400.0D, 10.0D, " т."));
    private final BooleanSetting closeGui = register(new BooleanSetting("Закрывать GUI", true));
    private final BooleanSetting chatOutput = register(new BooleanSetting("Вывод в чат", true));
    private final BooleanSetting aiAssist = register(new BooleanSetting("ИИ-помощник (GLM-4.7-Flash)", false));
    private final TextSetting aiApiKey = register(new TextSetting("API-ключ Z.AI", "").secret());
    private final BooleanSetting ahTrade = register(new BooleanSetting("Торговля на /ah", true));
    private final NumberSetting buyThreshold = register(new NumberSetting("Порог баланса (покупка)", 500000.0D, 100000.0D, 10000000.0D, 10000.0D, " $"));
    public final List<BpTask> tasks = new ArrayList<>();

    private enum State {
        IDLE, WAIT_BP_OPEN, CLICK_TASKS, WAIT_TASKS_MENU, CLICK_WEEKLY, WAIT_WEEKLY, DONE
    }

    private State state = State.IDLE;
    private int tickDelay;
    private int stateTicks;
    private long balance = -1L;
    private boolean balanceRequested;
    private boolean balanceChecked;
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "BpAuto-AI");
        thread.setDaemon(true);
        return thread;
    });

    private BpAutoFeature() {
        super("BpAuto", "Читает задания из /bp (еженедельные) и выводит их", FeatureCategory.MISC, BindSetting.UNBOUND);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.tasks.clear();
        this.state = State.WAIT_BP_OPEN;
        this.tickDelay = 20;
        this.stateTicks = 0;
        this.balance = -1L;
        this.balanceRequested = false;
        this.balanceChecked = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("bp");
            this.requestBalance();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.state = State.IDLE;
    }

    private void requestBalance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null || this.balanceRequested) {
            return;
        }
        this.balanceRequested = true;
        mc.player.connection.sendCommand("bal");
    }

    @EventTarget
    public void onPacket(PacketReceiveEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!this.aiAssist.getValue() || !this.balanceRequested || this.balanceChecked || mc.player == null) {
            return;
        }
        if (!(event.getPacket() instanceof ClientboundSystemChatPacket messagePacket)) {
            return;
        }
        String text = messagePacket.content().getString();
        String lower = text.toLowerCase();
        if (!lower.contains("баланс") && !lower.contains("balance") && !lower.contains("монет")
                && !lower.contains("coins") && !lower.contains("$") && !lower.contains("кошельк")) {
            return;
        }
        long value = this.parseBalance(text);
        if (value < 0) {
            return;
        }
        this.balance = value;
        this.balanceChecked = true;
    }

    private long parseBalance(String text) {
        String clean = text.replaceAll("[^\\d ]", " ").trim();
        Matcher matcher = Pattern.compile("\\d{1,3}(?:[ ]\\d{3})+|\\d+").matcher(clean);
        if (!matcher.find()) {
            return -1L;
        }
        try {
            return Long.parseLong(matcher.group().replaceAll(" ", ""));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (this.state == State.IDLE || this.state == State.DONE || mc.player == null) {
            return;
        }

        if (++this.stateTicks > this.timeout.getValue().intValue()) {
            ChatUtil.error("BpAuto: таймаут, прерываю");
            this.setEnabled(false);
            return;
        }

        if (this.tickDelay-- > 0) {
            return;
        }

        if (mc.gui.screen() == null) {
            this.tickDelay = 5;
            return;
        }

        if (!(mc.gui.screen() instanceof ContainerScreen screen)) {
            this.tickDelay = 5;
            return;
        }

        AbstractContainerMenu handler = screen.getMenu();
        switch (this.state) {
            case WAIT_BP_OPEN -> {
                this.clickSlot(handler);
                this.state = State.WAIT_TASKS_MENU;
                this.tickDelay = this.clickDelay.getValue().intValue();
            }

            case WAIT_TASKS_MENU -> {
                int slot = this.findSlotByName(handler, "Еженедельные задания");
                if (slot == -1) {
                    slot = this.findSlotByItem(handler);
                }
                if (slot == -1) {
                    slot = 22;
                }
                this.clickSlot(handler, slot);
                this.state = State.WAIT_WEEKLY;
                this.tickDelay = this.clickDelay.getValue().intValue();
            }

            case WAIT_WEEKLY -> {
                this.readTasks(handler);
                if (this.closeGui.getValue()) {
                    mc.execute(() -> Minecraft.getInstance().gui.setScreen(null));
                }
                this.state = State.DONE;
                this.setEnabled(false);
            }

            default -> this.state = State.IDLE;
        }
    }

    private void clickSlot(AbstractContainerMenu handler) {
        int slot = this.findSlotByName(handler, "ЗАДАНИЯ");
        if (slot == -1) {
            slot = 31;
        }
        this.clickSlot(handler, slot);
    }

    private void clickSlot(AbstractContainerMenu handler, int slotId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) {
            return;
        }
        mc.gameMode.handleContainerInput(handler.containerId, slotId, 0, ContainerInput.PICKUP, mc.player);
    }

    private void readTasks(AbstractContainerMenu handler) {
        this.tasks.clear();
        int slots = handler.slots.size();
        int limit = Math.min(slots, Math.max(slots - INVENTORY_SIZE, 9));
        for (int i = 0; i < limit; ++i) {
            ItemStack stack = handler.slots.get(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }

            String name = this.sanitize(stack.getHoverName().getString());
            if (name.isEmpty() || !this.isTaskName(name)) {
                continue;
            }

            BpTask task = new BpTask();
            task.name = name.replaceAll("\\[[mM]\\]|\\[[lL]\\]", "").trim();
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    String text = this.sanitize(line.getString());
                    if (text.isEmpty()) {
                        continue;
                    }

                    boolean progressFound = this.tryParseProgress(task, text);
                    if (!progressFound && text.contains("/")) {
                        this.tryParseFuzzyProgress(task, text);
                    }

                    String lower = text.toLowerCase();
                    if (lower.contains("наград") || lower.contains("опыт") || lower.contains("монет")
                            || lower.contains("кристалл") || lower.contains("токен") || lower.contains("бонус")) {
                        task.reward = text.replaceFirst("^[\\s•·\\-*◆⬥]+", "").trim();
                    }
                }
            }
            this.tasks.add(task);
        }

        if (this.chatOutput.getValue()) {
            this.printTasks();
        }

        this.askAi();
    }

    private void askAi() {
        Minecraft mc = Minecraft.getInstance();
        if (!this.aiAssist.getValue() || this.tasks.isEmpty()) {
            return;
        }
        String key = this.aiApiKey.getValue();
        if (key == null || key.isBlank()) {
            ChatUtil.error("BpAuto ИИ: вставьте API-ключ z.ai в настройках");
            return;
        }
        if (!this.balanceChecked) {
            if (this.balanceRequested) {
                this.aiExecutor.execute(() -> {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ignored) {
                    }
                    mc.execute(BpAutoFeature.this::askAi);
                });
            } else {
                this.requestBalance();
                this.aiExecutor.execute(() -> {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException ignored) {
                    }
                    mc.execute(BpAutoFeature.this::askAi);
                });
            }
            return;
        }
        this.aiExecutor.execute(() -> ZaiAi.ask(ZaiAi.Provider.ZAI, key, null, this.buildSystemPrompt(), this.buildTasksPrompt(), new ZaiAi.Callback() {
            @Override
            public void onResult(String reply) {
                mc.execute(() -> BpAutoFeature.this.executePlan(reply));
            }

            @Override
            public void onError(String message) {
                ChatUtil.error("BpAuto ИИ: " + message);
            }
        }));
    }

    private String buildSystemPrompt() {
        return "Ты — ИИ-планировщик заданий для Minecraft (анархия FunTime). "
                + "СПРАВОЧНИК СЕРВЕРА FunTime анархия: "
                + "/rtp — случайный телепорт в случайную точку мира; "
                + "/bp — открывает GUI с еженедельными заданиями; "
                + "/bal — баланс монет; "
                + "/ah — открыть аукцион (GUI, там слот для продажи и покупки); "
                + "/ah sell <цена> — ПРОДАЖА на аукционе: игрок держит в руке предмет (весь стак/количество в руке, цена указывается за всё что в руке), затем пишет /ah sell <цена> — так продаётся вся пачка; "
                + "продажу добытых ресурсов игрок делает через аукцион /ah sell (не через /sellall и не через /pawn). "
                + "ДОБЫЧА: Baritone-команды #mine <блок> (добыть блок), #goto x y z (идти к координатам), #stop (остановить). "
                + "При добыче дерева и руд ресурсы получаются в инвентарь, потом их нужно продать через /ah sell. "
                + "Задания из /bp могут требовать: добыть N штук блока (tree, ore), скрафтить предмет, убить моба или купить предмет. "
                + "Верни ТОЛЬКО JSON без пояснений и markdown в формате: {\"plan\": [{\"action\": \"...\", \"поле\": \"...\"}]}. "
                + "Доступные действия: "
                + "\"rtp\" — случайный телепорт через /rtp; "
                + "\"mine\" с полем \"block\" — добывать блок через Baritone (блок по-английски, например oak_log, iron_ore); "
                + "\"baritone\" с полем \"command\" — выполнить команду Baritone (например \"#mine diamond_ore\" или \"#goto 100 64 100\"); "
                + "\"goto\" с полями x, y, z — идти к координатам через Baritone; "
                + "\"sell\" с полями \"item\" и \"price\" — продать добытый ресурс: клиент возьмёт предмет в руку и отправит /ah sell <price>. item — название предмета на русском или английском из добытого, price — цена монет за всё что в руке; "
                + "\"autobuy\" с полем \"item\" — купить предмет через AutoBuy на аукционе; "
                + "\"ahsearch\" с полем \"item\" — открыть поиск предмета на аукционе командой .ahsearch (покажет лоты с этим предметом и цены); "
                + "\"message\" с полем \"text\" — сообщение игроку в чат. "
                + "Правила: НИКОГДА не предлагай идти в Энд и не ищи порталы в Энд. "
                + "НИКОГДА не строй порталы в Ад — только ищи уже существующие. "
                + "Для добычи дерева и руд всегда сначала выполни rtp, потом mine. "
                + "ВАЖНО ПРО БАЛАНС: тебе дают текущий баланс игрока и порог. "
                + "Если баланс меньше порога — режим ДОБЫЧИ: запрещено использовать autobuy. "
                + "Планируй добычу ресурсов (rtp + mine) и продажу добытого через /ah sell (шлейф: rtp → mine → sell). "
                + "Когда баланс достигнет порога — можно переходить к покупкам через autobuy. "
                + "AutoBuy работает только на FunTime; для задания с хорусом/хорусами обязательно оцени стоимость и выгоду покупки через autobuy: "
                + "если покупка невыгодна — переходи к следующему заданию и выдай его план; если ВСЕ задания невыгодны или невыполнимы — верни одно действие message и объясни. "
                + "Планируй только задания, которые реально выполнить. Текст message пиши на русском.";
    }

    private String buildTasksPrompt() {
        StringBuilder builder = new StringBuilder("Задания /bp:\n");
        for (BpTask task : this.tasks) {
            builder.append("- ").append(task.name);
            if (task.current >= 0 && task.target > 0) {
                builder.append(" [").append(task.current).append("/").append(task.target).append("]");
            }
            if (task.reward != null) {
                builder.append(" | награда: ").append(task.reward);
            }
            builder.append('\n');
        }
        builder.append("Баланс: ").append(this.balance >= 0 ? this.balance : AutoBuyUtil.getBalance()).append(" монет\n");
        builder.append("Порог для покупок: ").append(this.buyThreshold.getValue().longValue()).append(" монет\n");
        return builder.toString();
    }

    private void executePlan(String reply) {
        try {
            String json = this.extractJson(reply);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray plan = root.getAsJsonArray("plan");
            if (plan == null) {
                this.aiOutput(reply);
                return;
            }
            boolean anyExecuted = false;
            for (JsonElement element : plan) {
                if (this.executeAction(element.getAsJsonObject())) {
                    anyExecuted = true;
                }
            }
            if (!anyExecuted) {
                ChatUtil.info("BpAuto ИИ: все задания невыгодны или уже выполнены");
            }
        } catch (Exception e) {
            ChatUtil.error("BpAuto ИИ: не удалось разобрать план: " + e.getMessage());
            this.aiOutput(reply);
        }
    }

    private String extractJson(String reply) {
        String text = reply == null ? "" : reply.trim();
        if (text.isEmpty()) {
            return "";
        }
        text = text.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        int start = -1;
        char open = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                open = c;
                break;
            }
        }
        if (start == -1) {
            return text;
        }
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return text.substring(start);
    }

    private boolean executeAction(JsonObject action) {
        if (action == null || !action.has("action")) {
            return false;
        }
        String type = action.get("action").getAsString().toLowerCase();
        switch (type) {
            case "rtp":
                this.runClientCommand(".rtpfuntime");
                return true;
            case "mine": {
                String block = action.has("block") ? action.get("block").getAsString() : null;
                this.runClientCommand(".rtpfuntime");
                if (block != null && !block.isBlank()) {
                    this.sendBaritone("#mine " + block);
                }
                return true;
            }
            case "baritone": {
                String command = action.has("command") ? action.get("command").getAsString() : null;
                if (command != null && !command.isBlank()) {
                    this.sendBaritone(command);
                    return true;
                }
                return false;
            }
            case "goto": {
                String command = "#goto";
                if (action.has("x") && action.has("y") && action.has("z")) {
                    command = command + " " + action.get("x").getAsString() + " " + action.get("y").getAsString() + " " + action.get("z").getAsString();
                } else if (action.has("command")) {
                    command = action.get("command").getAsString();
                }
                this.sendBaritone(command);
                return true;
            }
            case "sell": {
                if (!this.ahTrade.getValue()) {
                    ChatUtil.info("BpAuto ИИ: торговля на /ah выключена — продажа пропущена");
                    return true;
                }
                String item = action.has("item") ? action.get("item").getAsString() : null;
                String price = action.has("price") ? action.get("price").getAsString() : null;
                this.sellOnAh(item, price);
                return true;
            }
            case "ahsearch": {
                if (!this.ahTrade.getValue()) {
                    ChatUtil.info("BpAuto ИИ: торговля на /ah выключена — поиск на аукционе пропущен");
                    return true;
                }
                String item = action.has("item") ? action.get("item").getAsString() : null;
                if (item == null || item.isBlank()) {
                    this.runClientCommand(".ahsearch");
                    return true;
                }
                this.runClientCommand(".ahsearch " + item);
                ChatUtil.info("BpAuto ИИ: ищу \"" + item + "\" на аукционе");
                return true;
            }
            case "autobuy": {
                if (!this.ahTrade.getValue()) {
                    ChatUtil.info("BpAuto ИИ: торговля на /ah выключена — покупка пропущена");
                    return true;
                }
                String item = action.has("item") ? action.get("item").getAsString() : null;
                if (!AutoBuyUtil.isFuntimeServer()) {
                    ChatUtil.info("BpAuto ИИ: AutoBuy работает только на FunTime");
                    return true;
                }
                long threshold = this.buyThreshold.getValue().longValue();
                if (this.balance >= 0 && this.balance < threshold) {
                    ChatUtil.info("BpAuto ИИ: баланс " + this.balance
                            + " меньше порога " + threshold + " — сначала добываем/продаём, покупаем позже");
                    return true;
                }
                AutoBuyFeature.INSTANCE.setEnabled(true);
                ChatUtil.info("BpAuto ИИ: включаю AutoBuy" + (item != null && !item.isBlank() ? " для " + item : ""));
                return true;
            }
            case "message": {
                String text = action.has("text") ? action.get("text").getAsString() : "";
                this.aiOutput(text);
                return true;
            }
            default:
                return false;
        }
    }

    private void runClientCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendChat(command);
        }
    }

    private void sendBaritone(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return;
        }
        String cmd = command.startsWith("#") ? command : "#" + command;
        mc.player.connection.sendChat(cmd);
    }

    private void sellOnAh(String item, String price) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        this.holdItemInHand(item);
        if (price != null && !price.isBlank()) {
            this.runClientCommand("/ah sell " + price);
            ChatUtil.info("BpAuto ИИ: продаю на /ah за " + price);
        } else {
            this.aiOutput("Возьми предмет в руку и напиши /ah sell <цена>, чтобы продать его на аукционе");
        }
    }

    private void holdItemInHand(String item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String lower = item == null ? "" : item.toLowerCase();
        Integer hotbar = null;
        Integer inventory = null;
        Inventory playerInventory = mc.player.getInventory();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = playerInventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getHoverName().getString().toLowerCase();
            boolean match = lower.isEmpty() || name.contains(lower);
            if (!match) {
                continue;
            }
            if (i < 9 && hotbar == null) {
                hotbar = i;
            } else if (i >= 9 && inventory == null) {
                inventory = i;
            }
        }
        if (hotbar != null) {
            playerInventory.setSelectedSlot(hotbar);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbar));
            return;
        }
        if (inventory != null) {
            InventoryUtil.swapWithHotbar(inventory, playerInventory.getSelectedSlot());
        }
    }

    private void aiOutput(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        String clean = text == null ? "" : text.replaceAll("```", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        mc.player.sendSystemMessage(Component.literal("§d[ИИ]§r " + clean));
    }

    private boolean tryParseProgress(BpTask task, String text) {
        Matcher matcher = BpAutoFeature.PROGRESS_PATTERN.matcher(text);
        if (matcher.find()) {
            return this.applyProgress(task, matcher.group(1), matcher.group(2));
        }

        matcher = BpAutoFeature.PROGRESS_WORDS_RU.matcher(text);
        if (matcher.find()) {
            return this.applyProgress(task, matcher.group(1), matcher.group(2));
        }

        matcher = BpAutoFeature.PROGRESS_PREFIX.matcher(text);
        if (matcher.find()) {
            return this.applyProgress(task, matcher.group(1), matcher.group(2));
        }

        matcher = BpAutoFeature.PROGRESS_SIMPLE.matcher(text);
        if (matcher.find()) {
            return this.applyProgress(task, matcher.group(1), matcher.group(2));
        }
        return false;
    }

    private boolean tryParseFuzzyProgress(BpTask task, String text) {
        Matcher matcher = BpAutoFeature.PROGRESS_FUZZY.matcher(text);
        if (matcher.find()) {
            return this.applyProgress(task, matcher.group(1), matcher.group(2));
        }
        return false;
    }

    private boolean applyProgress(BpTask task, String currentStr, String targetStr) {
        try {
            int current = Integer.parseInt(currentStr);
            int target = Integer.parseInt(targetStr);
            if (target > 0 && current >= 0 && current <= target * 10) {
                task.current = current;
                task.target = target;
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    private void printTasks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ChatUtil.info("BpAuto: заданий найдено: " + this.tasks.size());
        for (BpTask task : this.tasks) {
            StringBuilder line = new StringBuilder("§7• §f").append(task.name);
            if (task.target > 0) {
                double percent = task.target > 0 ? task.current * 100.0 / task.target : 0.0;
                String color = percent >= 100.0 ? "§a" : percent >= 50.0 ? "§e" : "§c";
                line.append(" ").append(color).append("[").append(task.current).append("/").append(task.target).append("]");
            } else {
                line.append(" §7[?]");
            }
            if (task.reward != null) {
                line.append(" §e| ").append(task.reward);
            }
            mc.player.sendSystemMessage(Component.literal(line.toString()));
        }
    }

    private boolean isTaskName(String name) {
        String lower = name.toLowerCase();
        for (String word : BpAutoFeature.MENU_WORDS) {
            if (lower.contains(word)) {
                return false;
            }
        }
        if (name.length() <= 8 && !lower.contains("[m]") && !lower.contains("[l]")) {
            return false;
        }
        return true;
    }

    private String sanitize(String text) {
        String result = text.replaceAll("(?i)funtime\\s*\\.\\s*su", "");
        return result.replaceAll("\\s+", " ").trim();
    }

    private int findSlotByName(AbstractContainerMenu handler, String keyword) {
        for (int i = 0; i < handler.slots.size(); ++i) {
            ItemStack stack = handler.slots.get(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getHoverName().getString().contains(keyword)) {
                return i;
            }
        }
        return -1;
    }

    private int findSlotByItem(AbstractContainerMenu handler) {
        for (int i = 0; i < handler.slots.size(); ++i) {
            ItemStack stack = handler.slots.get(i).getItem();
            if (stack.is(Items.CLOCK)) {
                return i;
            }
        }
        return -1;
    }

    public static final class BpTask {
        public String name = "";
        public int current = -1;
        public int target = -1;
        public String reward;
    }
}