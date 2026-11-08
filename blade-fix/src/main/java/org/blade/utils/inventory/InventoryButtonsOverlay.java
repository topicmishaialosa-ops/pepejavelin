package org.blade.utils.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import org.blade.context.MinecraftContext;
import org.blade.context.RenderContext;
import org.blade.event.EventTarget;
import org.blade.event.events.game.GameTickEvent;
import org.blade.event.events.lifecycle.WorldLeaveEvent;
import org.blade.event.events.render.FinalGuiRenderEvent;
import org.blade.event.events.screen.ScreenCloseEvent;
import org.blade.event.events.screen.ScreenMouseButtonEvent;
import org.blade.mixin.accessor.AbstractContainerScreenAccessor;
import org.blade.menu.core.MenuOverlay;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.ScaleUtil;
import org.blade.utils.render.gui.TextAlign;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public final class InventoryButtonsOverlay implements MinecraftContext {
    public static final InventoryButtonsOverlay INSTANCE = new InventoryButtonsOverlay();

    private static final String LABEL_DROP_ALL = "Выкинуть всё";
    private static final String LABEL_SORT = "Сортировать";
    private static final String LABEL_DROP_XP = "Выкинуть опыт";
    private static final String LABEL_STOP = "Остановить";

    private static final float BUTTON_HEIGHT = 36.0F;
    private static final float BUTTON_TEXT_SIZE = 13.0F;
    private static final float BUTTON_PADDING_X = 12.0F;
    private static final float BUTTON_RADIUS = 9.0F;
    private static final float BUTTON_GAP = 8.0F;

    private static final int BUTTON_BG = ColorUtil.rgba(10, 12, 18, 232);
    private static final int BUTTON_BG_HOVER = ColorUtil.rgba(26, 30, 40, 243);
    private static final int BUTTON_BORDER = ColorUtil.rgba(170, 182, 204, 115);
    private static final int BUTTON_BORDER_HOVER = ColorUtil.rgba(216, 223, 238, 195);
    private static final int BUTTON_TEXT = 0xFFFFFFFF;

    private static final int SORT_START = InventoryMenu.INV_SLOT_START;
    private static final int SORT_END = InventoryMenu.INV_SLOT_END;
    private static final int XP_START = InventoryMenu.INV_SLOT_START;
    private static final int XP_END = InventoryMenu.USE_ROW_SLOT_END;
    private static final int SORT_MAX_TICKS = 300;

    private boolean sortRunning;
    private InventoryScreen sortScreen;
    private AbstractContainerMenu sortMenu;
    private ItemStack[] sortTarget;
    private int sortTicks;

    private boolean xpRunning;
    private InventoryScreen xpScreen;
    private AbstractContainerMenu xpMenu;
    private final Deque<Integer> xpSlots = new ArrayDeque<>();

    private InventoryButtonsOverlay() {
    }

    public static boolean isBusy() {
        return INSTANCE.sortRunning || INSTANCE.xpRunning;
    }

    @EventTarget
    public void onFinalGuiRender(FinalGuiRenderEvent event) {
        if (!(event.getClient().gui.screen() instanceof InventoryScreen screen) || mc.player == null
                || MenuOverlay.isOpen()) {
            return;
        }
        RenderContext.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
        try {
            Render2DUtil.beginFrame();
            drawButtons(screen, event.getMouseX(), event.getMouseY());
            Render2DUtil.flush();
        } finally {
            RenderContext.exit2D();
        }
    }

    @EventTarget
    public void onScreenMouseButton(ScreenMouseButtonEvent event) {
        if (event.getAction() != ScreenMouseButtonEvent.Action.CLICK
                || event.getMouseButtonEvent().button() != 0
                || MenuOverlay.isOpen()
                || !(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        double mouseX = event.getMouseButtonEvent().x();
        double mouseY = event.getMouseButtonEvent().y();
        for (ButtonBox button : buildButtons(screen)) {
            if (inside(mouseX, mouseY, button)) {
                button.action().run();
                event.cancel();
                return;
            }
        }
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft client = event.getClient();
        if (client == null) {
            return;
        }
        tickSort(client);
        tickXpDrop(client);
    }

    @EventTarget
    public void onScreenClose(ScreenCloseEvent event) {
        if (event.getScreen() == this.sortScreen) {
            finishSort();
        }
        if (event.getScreen() == this.xpScreen) {
            finishXpDrop();
        }
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {
        finishSort();
        finishXpDrop();
    }

    private void drawButtons(InventoryScreen screen, int mouseX, int mouseY) {
        float unit = ScaleUtil.toGuiPixels(1.0F, mc.getWindow().getGuiScale());
        float textSize = BUTTON_TEXT_SIZE * unit;
        float radius = BUTTON_RADIUS * unit;
        MsdfFont font = UiFonts.sfProDisplay();
        for (ButtonBox button : buildButtons(screen)) {
            boolean hovered = inside(mouseX, mouseY, button);
            Render2DUtil.rect(button.x(), button.y(), button.width(), button.height())
                    .color(hovered ? BUTTON_BG_HOVER : BUTTON_BG)
                    .radius(radius)
                    .border(Math.max(0.5F, unit), hovered ? BUTTON_BORDER_HOVER : BUTTON_BORDER)
                    .draw();
            Render2DUtil.text(button.x() + button.width() / 2.0F,
                            font.centeredTextY(button.y() + button.height() / 2.0F, textSize),
                            textSize, button.label())
                    .style(UiFontStyle.MEDIUM)
                    .align(TextAlign.CENTER)
                    .color(BUTTON_TEXT)
                    .draw();
        }
    }

    private List<ButtonBox> buildButtons(InventoryScreen screen) {
        Minecraft client = mc;
        float unit = ScaleUtil.toGuiPixels(1.0F, client.getWindow().getGuiScale());
        float height = BUTTON_HEIGHT * unit;
        float textSize = BUTTON_TEXT_SIZE * unit;
        float paddingX = BUTTON_PADDING_X * unit;
        float gap = BUTTON_GAP * unit;
        MsdfFont font = UiFonts.sfProDisplay();
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();

        String dropAllLabel = DropAllInventoryController.isRunning() ? LABEL_STOP : LABEL_DROP_ALL;
        String sortLabel = this.sortRunning ? LABEL_STOP : LABEL_SORT;
        String xpLabel = this.xpRunning ? LABEL_STOP : LABEL_DROP_XP;

        float dropAllWidth = font.measureWidth(dropAllLabel, textSize, letterSpacing) + paddingX * 2.0F;
        float sortWidth = font.measureWidth(sortLabel, textSize, letterSpacing) + paddingX * 2.0F;
        float xpWidth = font.measureWidth(xpLabel, textSize, letterSpacing) + paddingX * 2.0F;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        float left = accessor.getLeftPos();
        float top = accessor.getTopPos();
        float windowWidth = accessor.getImageWidth();
        float guiWidth = client.getWindow().getGuiScaledWidth();

        float dropAllX = left + (windowWidth - dropAllWidth) / 2.0F;
        float dropAllY = Math.max(4.0F * unit, top - height - 5.0F * unit);

        float rightX = left + windowWidth + BUTTON_GAP * unit;
        float rowY = top + 16.0F * unit;
        float rightMargin = 4.0F * unit;
        float secondX;
        float secondY;
        if (rightX + sortWidth + gap + xpWidth <= guiWidth - rightMargin) {
            secondX = rightX + sortWidth + gap;
            secondY = rowY;
        } else {
            secondX = rightX;
            secondY = rowY + height + gap;
        }

        List<ButtonBox> buttons = new ArrayList<>(3);
        buttons.add(new ButtonBox(dropAllLabel, dropAllX, dropAllY, dropAllWidth, height,
                () -> DropAllInventoryController.toggle(screen)));
        buttons.add(new ButtonBox(sortLabel, rightX, rowY, sortWidth, height,
                () -> toggleSort(screen)));
        buttons.add(new ButtonBox(xpLabel, secondX, secondY, xpWidth, height,
                () -> toggleXpDrop(screen)));
        return buttons;
    }

    private void toggleSort(InventoryScreen screen) {
        if (this.sortRunning) {
            finishSort();
            return;
        }
        Minecraft client = mc;
        if (DropAllInventoryController.blocksInventoryOperations()
                || client.player == null || screen == null
                || client.player.containerMenu != screen.getMenu()
                || InventoryUtil.hasCarriedItem()) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        ItemStack[] target = new ItemStack[SORT_END - SORT_START];
        for (int i = 0; i < target.length; i++) {
            ItemStack stack = menu.getSlot(SORT_START + i).getItem();
            target[i] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
        Arrays.sort(target, InventoryButtonsOverlay::compareStacks);
        this.sortScreen = screen;
        this.sortMenu = menu;
        this.sortTarget = target;
        this.sortTicks = 0;
        this.sortRunning = true;
    }

    private void tickSort(Minecraft client) {
        if (!this.sortRunning) {
            return;
        }
        if (client.player == null || client.gui.screen() != this.sortScreen
                || client.player.containerMenu != this.sortMenu
                || InventoryUtil.hasCarriedItem()) {
            finishSort();
            return;
        }
        if (InventorySwap.isBusy()) {
            return;
        }
        if (++this.sortTicks > SORT_MAX_TICKS) {
            finishSort();
            return;
        }
        for (int slotId = SORT_START; slotId < SORT_END; slotId++) {
            ItemStack current = this.sortMenu.getSlot(slotId).getItem();
            ItemStack want = this.sortTarget[slotId - SORT_START];
            if (sameStack(current, want)) {
                continue;
            }
            int partner = -1;
            for (int candidate = slotId + 1; candidate < SORT_END; candidate++) {
                if (sameStack(this.sortMenu.getSlot(candidate).getItem(), want)) {
                    partner = candidate;
                    break;
                }
            }
            if (partner < 0) {
                finishSort();
                return;
            }
            InventoryUtil.leftClickSlot(slotId);
            InventoryUtil.leftClickSlot(partner);
            InventoryUtil.leftClickSlot(slotId);
            return;
        }
        finishSort();
    }

    private void finishSort() {
        this.sortRunning = false;
        this.sortScreen = null;
        this.sortMenu = null;
        this.sortTarget = null;
        this.sortTicks = 0;
    }

    private void toggleXpDrop(InventoryScreen screen) {
        if (this.xpRunning) {
            finishXpDrop();
            return;
        }
        Minecraft client = mc;
        if (DropAllInventoryController.blocksInventoryOperations()
                || client.player == null || screen == null
                || client.player.containerMenu != screen.getMenu()
                || InventoryUtil.hasCarriedItem()) {
            return;
        }
        AbstractContainerMenu menu = screen.getMenu();
        Deque<Integer> slots = new ArrayDeque<>();
        for (int slotId = XP_START; slotId <= XP_END; slotId++) {
            if (menu.isValidSlotIndex(slotId)
                    && menu.getSlot(slotId).getItem().getItem() instanceof ExperienceBottleItem) {
                slots.addLast(slotId);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        this.xpScreen = screen;
        this.xpMenu = menu;
        this.xpSlots.clear();
        this.xpSlots.addAll(slots);
        this.xpRunning = true;
    }

    private void tickXpDrop(Minecraft client) {
        if (!this.xpRunning) {
            return;
        }
        if (client.player == null || client.gui.screen() != this.xpScreen
                || client.player.containerMenu != this.xpMenu
                || InventoryUtil.hasCarriedItem()) {
            finishXpDrop();
            return;
        }
        if (InventorySwap.isBusy()) {
            return;
        }
        while (!this.xpSlots.isEmpty()) {
            int slotId = this.xpSlots.removeFirst();
            if (!this.xpMenu.isValidSlotIndex(slotId)) {
                continue;
            }
            ItemStack stack = this.xpMenu.getSlot(slotId).getItem();
            if (stack.isEmpty() || !(stack.getItem() instanceof ExperienceBottleItem)) {
                continue;
            }
            InventoryUtil.dropStack(slotId);
            return;
        }
        finishXpDrop();
    }

    private void finishXpDrop() {
        this.xpRunning = false;
        this.xpSlots.clear();
        this.xpScreen = null;
        this.xpMenu = null;
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }
        return ItemStack.isSameItemSameComponents(first, second);
    }

    private static int compareStacks(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return Boolean.compare(first.isEmpty(), second.isEmpty());
        }
        int firstGroup = group(first);
        int secondGroup = group(second);
        if (firstGroup != secondGroup) {
            return Integer.compare(firstGroup, secondGroup);
        }
        String firstId = BuiltInRegistries.ITEM.getKey(first.getItem()).toString();
        String secondId = BuiltInRegistries.ITEM.getKey(second.getItem()).toString();
        int byId = firstId.compareTo(secondId);
        if (byId != 0) {
            return byId;
        }
        return Integer.compare(second.getCount(), first.getCount());
    }

    private static int group(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem) {
            return 0;
        }
        if (stack.has(DataComponents.EQUIPPABLE) || stack.has(DataComponents.MAX_DAMAGE)) {
            return 1;
        }
        if (stack.has(DataComponents.FOOD)) {
            return 2;
        }
        return 3;
    }

    private static boolean inside(double mouseX, double mouseY, ButtonBox button) {
        return mouseX >= button.x() && mouseX <= button.x() + button.width()
                && mouseY >= button.y() && mouseY <= button.y() + button.height();
    }

    private record ButtonBox(String label, float x, float y, float width, float height, Runnable action) {
    }
}
