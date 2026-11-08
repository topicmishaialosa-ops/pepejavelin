package org.blade.menu.pages;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blade.feature.FeatureManager;
import org.blade.menu.core.MenuPage;
import org.blade.menu.i18n.MenuText;
import org.blade.menu.pages.configs.ConfigCard;
import org.blade.menu.ui.CardGrid;
import org.blade.menu.ui.controls.IconButton;
import org.blade.menu.ui.controls.InputComponent;
import org.blade.menu.ui.popups.ModalDialog;
import org.blade.menu.ui.PageComponent;
import org.blade.utils.render.Textures;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

import java.util.ArrayList;
import java.util.List;

public final class ConfigPage extends PageComponent {

    private static final int CONTROLS_RIGHT = 1024 - 32;
    private static final int PLUS_BOX = 24;
    private static final int PLUS_X = CONTROLS_RIGHT - PLUS_BOX;
    private static final float PLUS_Y = 104 + (36 - PLUS_BOX) / 2.0F;

    private static final int LIST_TOP = 188;
    private static final int ROW_STEP = ConfigCard.HEIGHT + 16;

    private final List<ConfigCard> cards = new ArrayList<>();
    private final CardGrid grid = new CardGrid(LIST_TOP, ConfigCard.WIDTH, ROW_STEP, 624);

    private final InputComponent nameInput = new InputComponent(
            () -> this.pendingName,
            value -> this.pendingName = value)
            .placeholder("Config name")
            .filter(value -> value.length() <= 32 && value.matches("[A-Za-z0-9_-]*"));

    private final ModalDialog saveModal = new ModalDialog(
            Textures.Icons.HARD_DRIVE,
            "Save Config",
            new String[]{"Enter a name for the preset."},
            this.nameInput,
            this::confirmSave,
            null);

    private final IconButton addButton = new IconButton(Textures.Icons.CIRCLE_PLUS, 16, this::openModal);

    private MenuPage displayedPage = MenuPage.NONE;
    private List<String> snapshot = List.of();
    private String selectedConfig = "";
    private String pendingName = "";

    @Override
    protected void onLayout() {
        this.displayedPage = this.state.displayPage();
        if (this.displayedPage != MenuPage.CONFIGURATIONS && this.saveModal.isOpen()) {
            this.saveModal.close();
        }

        syncCards();
        this.grid.update(this.cards.size());

        for (int index = 0; index < this.cards.size(); index++) {
            ConfigCard card = this.cards.get(index);
            card.setSelected(card.name().equals(this.selectedConfig));
            card.place(this, this.grid.x(index), this.grid.y(index),
                    this.mouseX, this.mouseY, this.progress);
        }

        this.addButton.place(this, PLUS_X, PLUS_Y, PLUS_BOX, this.mouseX, this.mouseY)
                .alpha(this.progress);
        this.saveModal.place(this, this.mouseX, this.mouseY, this.progress);
    }

    @Override
    public boolean handleClick(int mouseX, int mouseY) {
        if (!contentContains(mouseX, mouseY)) {
            return false;
        }
        if (this.saveModal.isOpen()) {
            return this.saveModal.handleClick(mouseX, mouseY);
        }
        if (this.addButton.handleClick(mouseX, mouseY)) {
            return true;
        }

        for (ConfigCard card : this.cards) {
            if (card.isDeleteAt(mouseX, mouseY)) {
                FeatureManager.INSTANCE.deleteConfig(card.name());
                if (this.selectedConfig.equals(card.name())) {
                    this.selectedConfig = "";
                }
                syncCards();
                return true;
            }
            if (card.handleClick(mouseX, mouseY)) {
                if (FeatureManager.INSTANCE.loadConfig(card.name())) {
                    this.selectedConfig = card.name();
                }
                return true;
            }
        }
        return true;
    }

    public boolean handleKey(int key) {
        if (this.displayedPage != MenuPage.CONFIGURATIONS) {
            return false;
        }
        return this.saveModal.handleKey(key);
    }

    public boolean handleCharacter(int codePoint) {
        return this.displayedPage == MenuPage.CONFIGURATIONS
                && this.saveModal.handleCharacter(codePoint);
    }

    public void handleScroll(int mouseX, int mouseY, double vertical) {
        if (this.saveModal.isOpen() || !contains(mouseX, mouseY) || mouseY < sy(168)) {
            return;
        }
        this.grid.scroll(vertical, this.cards.size());
    }

    @Override
    public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
        if (this.displayedPage != MenuPage.CONFIGURATIONS || this.progress <= 0.001F) {
            return;
        }

        pageHeader("Configurations", "Load, save, and manage your client presets.");

        this.addButton.render(minecraft, guiGraphicsExtractor);

        Render2DUtil.pushScissor(x(), sy(168), width(), height() - px(168));
        if (this.cards.isEmpty()) {
            renderEmptyState();
        } else {
            for (ConfigCard card : this.cards) {
                card.render(minecraft, guiGraphicsExtractor);
            }
        }
        Render2DUtil.popScissor();

        this.saveModal.render(minecraft, guiGraphicsExtractor);
    }

    private void renderEmptyState() {
        float centerY = (188 + 768) / 2.0F;
        texture(500, centerY - 58, 24, Textures.Icons.HARD_DRIVE,
                Theme.Colors.ICON_MUTED, this.progress);
        float titleHeight = UiFonts.sfProDisplay().textHeight(16);
        textCentered(512, centerY - 20, 16, MenuText.ui("No saved configs"),
                Theme.Colors.PRIMARY, this.progress, UiFontStyle.MEDIUM);
        textCentered(512, centerY - 20 + titleHeight + 8, 12,
                MenuText.ui("Save your current settings with + or use .cfg save <name>"),
                Theme.Colors.SECONDARY, this.progress, UiFontStyle.REGULAR);
    }

    private void openModal() {
        this.pendingName = "";
        this.saveModal.open();
    }

    private void confirmSave() {
        String name = this.pendingName.trim();
        if (name.isEmpty() || !name.matches("[A-Za-z0-9_-]{1,32}")) {
            return;
        }
        FeatureManager.INSTANCE.saveConfigAs(name);
        this.selectedConfig = name;
        this.saveModal.close();
        syncCards();
    }

    private void syncCards() {
        List<String> current = FeatureManager.INSTANCE.configNames();
        if (current.equals(this.snapshot)) {
            return;
        }
        this.snapshot = current;
        this.cards.clear();
        for (String name : current) {
            this.cards.add(new ConfigCard(name));
        }
    }
}
