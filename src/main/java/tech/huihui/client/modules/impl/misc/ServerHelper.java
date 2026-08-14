package tech.huihui.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.events.impl.input.EventKey;
import tech.huihui.base.events.impl.other.EventTickMovement;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.KeySetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.impl.movement.AutoSprint;
import tech.huihui.utility.game.player.PlayerInventoryUtil;

@ModuleAnnotation(
   name = "ServerHelper",
   category = Category.MISC,
   description = "Хелперы для серверов"
)
public final class ServerHelper extends Module {
   public static final ServerHelper INSTANCE = new ServerHelper();
   private final ModeSetting server = new ModeSetting("Сервер", new String[]{"ReallyWorld", "LonyGrief", "Funtime"});
   private final BooleanSetting bindMode = new BooleanSetting("Использование по бинду", true, () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting antiFly = new KeySetting("Клавиша юза анти-полета", () -> {
      return this.server.is("ReallyWorld");
   });
   private final KeySetting desorientKey = new KeySetting("Кнопка дезориентации", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting shulkerKey = new KeySetting("Кнопка шалкера", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting freezeKey = new KeySetting("Кнопка заморозки", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting trapKey = new KeySetting("Кнопка трапки", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting flameKey = new KeySetting("Кнопка огненного смерча", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting blatantKey = new KeySetting("Кнопка явной пыли", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting bowKey = new KeySetting("Кнопка арбалета", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting plastKey = new KeySetting("Кнопка пласта", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting bojkaKey = new KeySetting("Кнопка божьей ауры", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting xlopyshkaKey = new KeySetting("Кнопка хлопушки", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting svatvodaKey = new KeySetting("Кнопка святой воды", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting gnevkaKey = new KeySetting("Кнопка зелья гнева", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting paladinKey = new KeySetting("Кнопка зелья паладина", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting assasinKey = new KeySetting("Кнопка зелья ассасина", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting radiaciaKey = new KeySetting("Кнопка зелья радиации", () -> {
      return this.server.is("Funtime");
   });
   private final KeySetting snotvornoeKey = new KeySetting("Кнопка снотворного", () -> {
      return this.server.is("Funtime");
   });
   private final List<Ability> abilities = new ArrayList<>();
   private boolean useAntiFly;
   private long delay;

   private ServerHelper() {
      this.abilities.add(Ability.of("Дезориентация", "дезориентаци", Items.ENDER_EYE, this.desorientKey));
      this.abilities.add(Ability.of("Шалкер", "ящик", Items.SHULKER_BOX, this.shulkerKey));
      this.abilities.add(Ability.of("Заморозка", "снежок заморозк", Items.SNOWBALL, this.freezeKey));
      this.abilities.add(Ability.of("Трапка", "трапк", Items.NETHERITE_SCRAP, this.trapKey));
      this.abilities.add(Ability.of("Огненный смерч", "огненный смерч", Items.FIRE_CHARGE, this.flameKey));
      this.abilities.add(Ability.of("Явная пыль", "явная пыль", Items.SUGAR, this.blatantKey));
      this.abilities.add(Ability.of("Арбалет", "арбалет", Items.CROSSBOW, this.bowKey));
      this.abilities.add(Ability.of("Пласт", "пласт", Items.DRIED_KELP, this.plastKey));
      this.abilities.add(Ability.of("Божья аура", "божья аура", Items.PHANTOM_MEMBRANE, this.bojkaKey));
      this.abilities.add(Ability.of("Хлопушка", "хлопушк", Items.SPLASH_POTION, this.xlopyshkaKey));
      this.abilities.add(Ability.of("Святая вода", "святая вода", Items.SPLASH_POTION, this.svatvodaKey));
      this.abilities.add(Ability.of("Зелье гнева", "зелье гнева", Items.SPLASH_POTION, this.gnevkaKey));
      this.abilities.add(Ability.of("Зелье паладина", "палладин", Items.SPLASH_POTION, this.paladinKey));
      this.abilities.add(Ability.of("Зелье ассасина", "зелье ассасин", Items.SPLASH_POTION, this.assasinKey));
      this.abilities.add(Ability.of("Зелье радиации", "зелье радиаци", Items.SPLASH_POTION, this.radiaciaKey));
      this.abilities.add(Ability.of("Снотворное", "снотворн", Items.SPLASH_POTION, this.snotvornoeKey));
   }

   @EventTarget
   private void onKey(EventKey e) {
      if (mc.currentScreen != null) {
         return;
      }
      if (e.getAction() != 1) {
         return;
      }
      if (this.server.is("ReallyWorld") && e.getKeyCode() == this.antiFly.getKeyCode()) {
         this.useAntiFly = true;
      }
      if (this.server.is("Funtime") && this.bindMode.isEnabled()) {
         for (Ability ability : this.abilities) {
            if (e.getKeyCode() == ability.getKey()) {
               ability.queue();
            }
         }
      }
   }

   @EventTarget
   @Native
   private void onTick(EventTickMovement e) {
      if (this.server.is("Funtime") && this.bindMode.isEnabled() && mc.player != null) {
         this.processFuntime();
      }
      if (this.useAntiFly) {
         this.useAntiFly = false;
         this.useAntiFly();
      }
   }

   private void processFuntime() {
      if (System.currentTimeMillis() - this.delay < 200L) {
         return;
      }
      if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler)) {
         return;
      }
      for (Ability ability : this.abilities) {
         if (!ability.consume()) {
            continue;
         }
         if (mc.player.getItemCooldownManager().isCoolingDown(new ItemStack(ability.getItem()))) {
            continue;
         }
         if (!this.useItem(ability.getKeyword())) {
            this.notify("§c[!] " + ability.getName() + " не найдена!");
            continue;
         }
         this.notify("§a[+] Использована " + ability.getName() + "!");
         this.delay = System.currentTimeMillis();
      }
   }

   private boolean useItem(String keyword) {
      int hand = mc.player.getInventory().selectedSlot;
      int hotbar = this.findSlot(keyword, 0, 8);
      int inv = this.findSlot(keyword, 9, 35);
      int use = hotbar != -1 ? hotbar : inv;
      if (use == -1) {
         return false;
      }
      if (hotbar == hand) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         return true;
      }
      mc.interactionManager.clickSlot(0, use, hand, SlotActionType.SWAP, mc.player);
      mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      mc.interactionManager.clickSlot(0, use, hand, SlotActionType.SWAP, mc.player);
      return true;
   }

   private int findSlot(String keyword, int from, int to) {
      for (int i = to; i >= from; --i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) {
            continue;
         }
         String name = Formatting.strip(stack.getName().getString());
         if (name != null && name.toLowerCase(Locale.ROOT).contains(keyword)) {
            return i;
         }
      }
      return -1;
   }

   public List<AbilityInfo> getHudData(float tickDelta) {
      List<AbilityInfo> infos = new ArrayList<>();
      if (!this.server.is("Funtime") || mc.player == null) {
         return infos;
      }
      for (Ability ability : this.abilities) {
         int key = ability.getKey();
         if (key == -1) {
            ability.getWatch().reset();
            continue;
         }
         double ratio = (double) mc.player.getItemCooldownManager().getCooldownProgress(new ItemStack(ability.getItem()), tickDelta);
         OptionalDouble cooldown = ability.getWatch().update(ratio);
         Double seconds = cooldown.isPresent() ? cooldown.getAsDouble() : null;
         infos.add(new AbilityInfo(ability.getName(), ability.getItem(), key, seconds));
      }
      return infos;
   }

   private void notify(String message) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal(message), false);
      }
   }

   private void useAntiFly() {
      int slot = PlayerInventoryUtil.find(Items.FIREWORK_STAR, 9, 45);
      int slotHotbar = PlayerInventoryUtil.find(Items.FIREWORK_STAR, 0, 8);
      if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_STAR) {
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.PRESS_SHIFT_KEY));
         mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
      } else {
         boolean wasSprinting;
         if (slotHotbar != -1) {
            wasSprinting = false;
            if (mc.player.isSprinting()) {
               mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
               mc.player.setSprinting(false);
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
               if (!AutoSprint.INSTANCE.isEnabled()) {
                  mc.options.sprintKey.setPressed(false);
               }

               wasSprinting = true;
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, slotHotbar, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.PRESS_SHIFT_KEY));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, slotHotbar, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            if (wasSprinting) {
               mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
            }
         }

         if (slotHotbar == -1 && slot != -1) {
            wasSprinting = false;
            if (mc.player.isSprinting()) {
               mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
               mc.player.setSprinting(false);
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
               if (!AutoSprint.INSTANCE.isEnabled()) {
                  mc.options.sprintKey.setPressed(false);
               }

               wasSprinting = true;
            }

            mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.PRESS_SHIFT_KEY));
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.RELEASE_SHIFT_KEY));
            mc.interactionManager.clickSlot(0, slot, 40, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            if (wasSprinting) {
               mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
            }
         }
      }
   }

   public static final class AbilityInfo {
      private final String name;
      private final Item item;
      private final int keyCode;
      private final Double cooldownSeconds;

      public AbilityInfo(String name, Item item, int keyCode, Double cooldownSeconds) {
         this.name = name;
         this.item = item;
         this.keyCode = keyCode;
         this.cooldownSeconds = cooldownSeconds;
      }

      public String getName() {
         return this.name;
      }

      public Item getItem() {
         return this.item;
      }

      public int getKeyCode() {
         return this.keyCode;
      }

      public boolean hasCooldown() {
         return this.cooldownSeconds != null && this.cooldownSeconds > 0.0D;
      }

      public double getCooldownSeconds() {
         return this.cooldownSeconds != null ? this.cooldownSeconds : 0.0D;
      }
   }

   private static final class Ability {
      private final String name;
      private final String keyword;
      private final Item item;
      private final KeySetting key;
      private final CooldownWatch watch = new CooldownWatch();
      private boolean queued;

      private Ability(String name, String keyword, Item item, KeySetting key) {
         this.name = name;
         this.keyword = keyword;
         this.item = item;
         this.key = key;
      }

      private static Ability of(String name, String keyword, Item item, KeySetting key) {
         return new Ability(name, keyword, item, key);
      }

      public String getName() {
         return this.name;
      }

      public String getKeyword() {
         return this.keyword;
      }

      public Item getItem() {
         return this.item;
      }

      public int getKey() {
         return this.key.getKeyCode();
      }

      public CooldownWatch getWatch() {
         return this.watch;
      }

      public void queue() {
         this.queued = true;
      }

      public boolean consume() {
         if (!this.queued) {
            return false;
         }
         this.queued = false;
         return true;
      }
   }

   private static final class CooldownWatch {
      private boolean active;
      private long startTimeMs;
      private Double totalDurationMs;

      public OptionalDouble update(double ratio) {
         long now = System.currentTimeMillis();
         if (ratio <= 0.0D) {
            this.reset();
            return OptionalDouble.empty();
         }
         if (!this.active) {
            this.active = true;
            this.startTimeMs = now;
            this.totalDurationMs = null;
            return OptionalDouble.empty();
         }
         if (this.totalDurationMs == null && ratio < 0.98D) {
            double elapsed = (double) (now - this.startTimeMs);
            double denominator = 1.0D - ratio;
            if (denominator > 0.0D && elapsed > 0.0D) {
               this.totalDurationMs = elapsed / denominator;
            }
         }
         if (this.totalDurationMs == null) {
            return OptionalDouble.empty();
         }
         double elapsed = (double) (now - this.startTimeMs);
         double remaining = Math.max(0.0D, this.totalDurationMs - elapsed);
         if (remaining <= 0.0D) {
            this.reset();
            return OptionalDouble.empty();
         }
         return OptionalDouble.of(remaining / 1000.0D);
      }

      public void reset() {
         this.active = false;
         this.totalDurationMs = null;
      }
   }
}