package org.blade.menu.pages.configs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.blade.menu.i18n.MenuText;
import org.blade.menu.ui.Component;
import org.blade.menu.ui.MenuCard;
import org.blade.menu.ui.controls.IconButton;
import org.blade.utils.ColorUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.Textures;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;

public final class ConfigCard extends MenuCard {

    private static final int TEXT_X = PADDING_X + AVATAR_SIZE + 10;
    private static final int DELETE_BOX_X = WIDTH - PADDING_X - ACTION_BOX;

    private final String name;
    private boolean selected;
    private final IconButton deleteButton = new IconButton(Textures.Icons.DELETE_LEFT, ACTION_ICON, null)
            .hoverBackground(ColorUtil.withAlpha(Theme.Colors.SYSTEM_RED, 28))
            .tint(Theme.Colors.ICON, Theme.Colors.SYSTEM_RED);

    public ConfigCard(String name) {
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void place(Component owner, int x, int y, int mouseX, int mouseY, float alpha) {
        placeBounds(owner, x, y, mouseX, mouseY, alpha);

        this.deleteButton.place(owner, x + DELETE_BOX_X, actionRowY(), ACTION_BOX, mouseX, mouseY)
                .alpha(alpha);
    }

    public boolean isDeleteAt(int mouseX, int mouseY) {
        return this.deleteButton.contains(mouseX, mouseY);
    }

    @Override
    public void render(Minecraft minecraft, GuiGraphicsExtractor guiGraphicsExtractor) {
        if (this.selected) {
            rect(this.designX, this.designY, WIDTH, HEIGHT, Theme.getAccent(), RADIUS, this.alpha);
        } else {
            rect(this.designX, this.designY, WIDTH, HEIGHT,
                    cardHovered() ? Theme.Colors.CARD_HOVER : Theme.Colors.BACKGROUND_SURFACE_S,
                    RADIUS, this.alpha);
            outline(this.designX, this.designY, WIDTH, HEIGHT,
                    Theme.Colors.OUTLINES_SMALL, RADIUS, 0.5F, this.alpha);
        }

        texture(this.designX + PADDING_X, this.designY + AVATAR_Y, AVATAR_SIZE,
                Textures.Icons.HARD_DRIVE, ColorUtil.WHITE, this.alpha);

        int textColor = this.selected ? Theme.Colors.TEXT_TITLE : Theme.Colors.ICON;
        float maxTextWidth = DELETE_BOX_X - TEXT_X - 8;

        String shownName = UiFonts.sfProDisplay().ellipsize(
                this.name, 14, 14 * UiFontStyle.MEDIUM.letterSpacingEm(), maxTextWidth);
        String shownStatus = this.selected ? MenuText.ui("Active") : MenuText.ui("Click to load");
        float nameHeight = UiFonts.sfProDisplay().textHeight(14);
        float statusHeight = UiFonts.sfProDisplay().textHeight(11);
        float textTop = this.designY + (HEIGHT - nameHeight - 4 - statusHeight) / 2.0F;

        text(this.designX + TEXT_X, textTop, 14, shownName,
                textColor, this.alpha, UiFontStyle.MEDIUM);
        text(this.designX + TEXT_X, textTop + nameHeight + 4, 11, shownStatus,
                this.selected ? Theme.Colors.TEXT_TITLE : Theme.Colors.SECONDARY,
                this.alpha, UiFontStyle.REGULAR);

        this.deleteButton.render(minecraft, guiGraphicsExtractor);
    }
}
