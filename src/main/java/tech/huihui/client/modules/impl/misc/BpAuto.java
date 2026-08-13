package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import tech.huihui.base.events.impl.player.EventUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.other.MessageUtil;

@ModuleAnnotation(
   name = "BpAuto",
   category = Category.MISC,
   description = "Читает задания из /bp (еженедельные) и выводит их"
)
public final class BpAuto extends Module {
   public static final BpAuto INSTANCE = new BpAuto();
   private static final Pattern PROGRESS_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
   private static final Pattern PROGRESS_WORDS_RU = Pattern.compile("(\\d+)\\s+из\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
   private static final Pattern PROGRESS_PREFIX = Pattern.compile("прогресс[:\\s]*(\\d+)\\s*/\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
   private static final Pattern PROGRESS_SIMPLE = Pattern.compile("(\\d+)/(\\d+)");
   private static final Pattern PROGRESS_FUZZY = Pattern.compile("(\\d+)\\D*/\\D*(\\d+)");
   private static final String[] MENU_WORDS = new String[]{"задания", "назад", "закрыть", "стрелка", "еженедельн", "ежедневн", "информац", "выйти"};
   private final NumberSetting clickDelay = new NumberSetting("Задержка кликов", 15.0F, 5.0F, 60.0F, 1.0F);
   private final NumberSetting timeout = new NumberSetting("Таймаут", 200.0F, 40.0F, 400.0F, 10.0F);
   private final BooleanSetting closeGui = new BooleanSetting("Закрывать GUI", true);
   private final BooleanSetting chatOutput = new BooleanSetting("Вывод в чат", true);
   public final List<BpTask> tasks = new ArrayList<>();

   private enum State {
      IDLE, WAIT_BP_OPEN, CLICK_TASKS, WAIT_TASKS_MENU, CLICK_WEEKLY, WAIT_WEEKLY, DONE
   }

   private State state = State.IDLE;
   private int tickDelay;
   private int stateTicks;

   @Override
   public void onEnable() {
      super.onEnable();
      this.tasks.clear();
      this.state = State.WAIT_BP_OPEN;
      this.tickDelay = 20;
      this.stateTicks = 0;
      if (mc.player != null) {
         mc.getNetworkHandler().sendChatMessage("/bp");
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.state = State.IDLE;
   }

   @EventTarget
   private void onUpdate(EventUpdate event) {
      if (this.state == State.IDLE || this.state == State.DONE || mc.player == null) {
         return;
      }

      if (++this.stateTicks > (int) this.timeout.getCurrent()) {
         MessageUtil.displayError("BpAuto: таймаут, прерываю");
         this.setToggled(false);
         return;
      }

      if (this.tickDelay-- > 0) {
         return;
      }

      if (mc.currentScreen == null) {
         this.tickDelay = 5;
         return;
      }

      if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
         this.tickDelay = 5;
         return;
      }

      ScreenHandler handler = screen.getScreenHandler();
      switch (this.state) {
         case WAIT_BP_OPEN -> {
            this.clickSlot(handler);
            this.state = State.WAIT_TASKS_MENU;
            this.tickDelay = (int) this.clickDelay.getCurrent();
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
            this.tickDelay = (int) this.clickDelay.getCurrent();
         }

         case WAIT_WEEKLY -> {
            this.readTasks(handler);
            if (this.closeGui.isEnabled()) {
               mc.execute(() -> mc.setScreen(null));
            }
            this.state = State.DONE;
            this.setToggled(false);
         }

         default -> this.state = State.IDLE;
      }
   }

   private void clickSlot(ScreenHandler handler) {
      int slot = this.findSlotByName(handler, "ЗАДАНИЯ");
      if (slot == -1) {
         slot = 31;
      }
      this.clickSlot(handler, slot);
   }

   private void clickSlot(ScreenHandler handler, int slotId) {
      if (mc.interactionManager == null || mc.player == null) {
         return;
      }
      mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
   }

   private void readTasks(ScreenHandler handler) {
      this.tasks.clear();
      int slots = handler.slots.size();
      int limit = Math.min(slots, Math.max(slots - 36, 9));
      for (int i = 0; i < limit; ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.isEmpty()) {
            continue;
         }

         String name = this.sanitize(stack.getName().getString());
         if (name.isEmpty() || !this.isTaskName(name)) {
            continue;
         }

         BpTask task = new BpTask();
         task.name = name.replaceAll("\\[[mM]\\]|\\[[lL]\\]", "").trim();
         LoreComponent lore = stack.get(DataComponentTypes.LORE);
         if (lore != null) {
            for (Text line : lore.lines()) {
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

      if (this.chatOutput.isEnabled()) {
         this.printTasks();
      }
   }

   private boolean tryParseProgress(BpTask task, String text) {
      Matcher matcher = BpAuto.PROGRESS_PATTERN.matcher(text);
      if (matcher.find()) {
         return this.applyProgress(task, matcher.group(1), matcher.group(2));
      }

      matcher = BpAuto.PROGRESS_WORDS_RU.matcher(text);
      if (matcher.find()) {
         return this.applyProgress(task, matcher.group(1), matcher.group(2));
      }

      matcher = BpAuto.PROGRESS_PREFIX.matcher(text);
      if (matcher.find()) {
         return this.applyProgress(task, matcher.group(1), matcher.group(2));
      }

      matcher = BpAuto.PROGRESS_SIMPLE.matcher(text);
      if (matcher.find()) {
         return this.applyProgress(task, matcher.group(1), matcher.group(2));
      }
      return false;
   }

   private boolean tryParseFuzzyProgress(BpTask task, String text) {
      Matcher matcher = BpAuto.PROGRESS_FUZZY.matcher(text);
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
      MessageUtil.displayInfo("BpAuto: заданий найдено: " + this.tasks.size());
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
         mc.player.sendMessage(Text.literal(line.toString()), false);
      }
   }

   private boolean isTaskName(String name) {
      String lower = name.toLowerCase();
      for (String word : BpAuto.MENU_WORDS) {
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

   private int findSlotByName(ScreenHandler handler, String keyword) {
      for (int i = 0; i < handler.slots.size(); ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.isEmpty()) {
            continue;
         }
         if (stack.getName().getString().contains(keyword)) {
            return i;
         }
      }
      return -1;
   }

   private int findSlotByItem(ScreenHandler handler) {
      for (int i = 0; i < handler.slots.size(); ++i) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.isOf(Items.CLOCK)) {
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