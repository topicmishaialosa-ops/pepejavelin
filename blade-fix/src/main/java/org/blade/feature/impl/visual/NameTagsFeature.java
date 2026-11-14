package org.blade.feature.impl.visual;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.blade.event.EventTarget;
import org.blade.event.events.render.Render2DEvent;
import org.blade.feature.Feature;
import org.blade.feature.FeatureCategory;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.combat.AntiBotFeature;
import org.blade.feature.setting.BindSetting;
import org.blade.feature.setting.BooleanSetting;
import org.blade.feature.setting.ColorSetting;
import org.blade.feature.setting.ModeSetting;
import org.blade.feature.setting.NumberSetting;
import org.blade.utils.ColorUtil;
import org.blade.utils.FriendManager;
import org.blade.utils.render.Render3DUtil;
import org.blade.utils.render.Theme;
import org.blade.utils.render.gui.MsdfFont;
import org.blade.utils.render.gui.Render2DUtil;
import org.blade.utils.render.gui.UiFontStyle;
import org.blade.utils.render.gui.UiFonts;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.blade.utils.render.Textures;

public final class NameTagsFeature extends Feature {
    private static final int DIVIDER_COLOR = ColorUtil.rgba(255, 255, 255, 20);

    private static final float SCALE = 1.1F;
    private static final float PILL_HEIGHT = 40.0F;
    private static final float RADIUS = 16.0F;
    private static final float PADDING = 10.0F;
    private static final float GAP = 8.0F;
    private static final float HEAD_SIZE = 20.0F;
    private static final float ICON_SIZE = 16.0F;
    private static final float ITEM_SIZE = 16.0F;
    private static final float DIVIDER_HEIGHT = 12.0F;
    private static final float TEXT_SIZE = 12.0F;
    private static final float HEAD_OFFSET = 0.25F;

    private static final EquipmentSlot[] EQUIPMENT_ORDER = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private record Privilege(String name, int color) {
    }

    private record CachedPrivilege(PlayerTeam team, Privilege privilege) {
    }

    private static final Map<UUID, CachedPrivilege> PRIVILEGE_CACHE = new HashMap<>();
    private static final Map<String, String> NAME_CACHE = new HashMap<>();

    private final List<ItemStack> equipmentScratch = new ArrayList<>(EQUIPMENT_ORDER.length);

    public final ModeSetting style = register(new ModeSetting("Style", "nametags", "nametags", "nametags2"));
    public final NumberSetting scale = register(new NumberSetting("Scale", 1.0, 0.95, 1.2, 0.05, "x"));
    public final BooleanSetting privilege = register(new BooleanSetting("Privilege", true)
            .visibleWhen(() -> this.style.is("nametags")));
    public final BooleanSetting health = register(new BooleanSetting("Health", true));
    public final BooleanSetting items = register(new BooleanSetting("Items", true));
    public final NumberSetting distance = register(new NumberSetting("Distance", 64, 16, 128, 8, " blocks"));

    public final BooleanSetting head = register(new BooleanSetting("Head", true)
            .visibleWhen(() -> this.style.is("nametags2")));
    public final BooleanSetting friendTag = register(new BooleanSetting("Friend tag", true)
            .visibleWhen(() -> this.style.is("nametags2")));
    public final BooleanSetting botTag = register(new BooleanSetting("Bot tag", true)
            .visibleWhen(() -> this.style.is("nametags2")));
    public final NumberSetting textSize = register(new NumberSetting("Text size", 6.0, 3.0, 100.0, 0.5, "")
            .visibleWhen(() -> this.style.is("nametags2")));
    public final ColorSetting nameColor = register(new ColorSetting("Name color", 0xFFFFFFFF)
            .visibleWhen(() -> this.style.is("nametags2")));
    public final ColorSetting hpColor = register(new ColorSetting("HP color", 0xFFFFFFFF)
            .visibleWhen(() -> this.style.is("nametags2")));
    public final ColorSetting bgColor = register(new ColorSetting("Background color", 0xFF000000)
            .visibleWhen(() -> this.style.is("nametags2")));
    public final ColorSetting friendTagColor = register(new ColorSetting("Friend tag color", 0xFF00FF00)
            .visibleWhen(() -> this.style.is("nametags2")));

    public NameTagsFeature() {
        super("NameTags", "Draws styled nametags above players", FeatureCategory.VISUAL, BindSetting.UNBOUND);
    }

    public static boolean shouldHideVanillaTag() {
        return FeatureManager.INSTANCE.getEnabled(NameTagsFeature.class) != null;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float unit = (float) (SCALE * this.scale.getValue() / mc.getWindow().getGuiScale());
        double maxDistanceSqr = this.distance.getValue() * this.distance.getValue();

        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
                continue;
            }
            if (player.isRemoved() || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            if (mc.player.distanceToSqr(player) > maxDistanceSqr) {
                continue;
            }
            if (this.style.is("nametags2")) {
                drawTag2(event, player, tickDelta, unit);
            } else {
                drawTag(event, player, tickDelta, unit);
            }
        }
    }

    private void drawTag(Render2DEvent event, AbstractClientPlayer player, float tickDelta, float unit) {
        Minecraft mc = event.getClient();
        Vec3 position = Render3DUtil.interpolatedPosition(player, tickDelta)
                .add(0.0D, player.getBbHeight() + HEAD_OFFSET, 0.0D);
        Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, position);
        if (anchor == null) {
            return;
        }

        MsdfFont font = UiFonts.sfProDisplay();
        float textSize = TEXT_SIZE * unit;
        float letterSpacing = textSize * UiFontStyle.MEDIUM.letterSpacingEm();
        float headSize = HEAD_SIZE * unit;
        float iconSize = ICON_SIZE * unit;
        float itemSize = ITEM_SIZE * unit;
        float gap = GAP * unit;
        float dividerWidth = Math.max(0.5F, 0.5F * unit);

        String name = safeName(player.getGameProfile().name());
        Privilege donat = this.privilege.getValue() ? resolvePrivilege(player) : null;
        String healthText = this.health.getValue()
                ? (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount()) + "HP"
                : null;
        List<ItemStack> equipment = this.items.getValue() ? equipment(player) : List.of();

        float width = PADDING * unit + headSize + gap + font.measureWidth(name, textSize, letterSpacing);
        if (donat != null) {
            width += gap + dividerWidth + gap + iconSize + gap
                    + font.measureWidth(donat.name(), textSize, letterSpacing);
        }
        if (healthText != null) {
            width += gap + dividerWidth + gap + iconSize + gap
                    + font.measureWidth(healthText, textSize, letterSpacing);
        }
        if (!equipment.isEmpty()) {
            width += gap + dividerWidth + gap
                    + equipment.size() * itemSize + (equipment.size() - 1) * gap;
        }
        width += PADDING * unit;

        float pillHeight = PILL_HEIGHT * unit;
        float pillX = anchor.x() - width / 2.0F;
        float pillY = anchor.y() - pillHeight;
        float centerY = pillY + pillHeight / 2.0F;
        float textY = font.centeredTextY(centerY, textSize);

        Render2DUtil.rect(pillX, pillY, width, pillHeight)
                .color(Theme.Colors.BACKGROUND_PRIMARY_50)
                .radius(RADIUS * unit)
                .border(Math.max(0.5F, 0.5F * unit), Theme.Colors.OUTLINES_MEDIUM)
                .blur(8.0F * unit)
                .draw();

        float cursor = pillX + PADDING * unit;

        drawHead(player, cursor, centerY - headSize / 2.0F, headSize);
        cursor += headSize + gap;

        Render2DUtil.text(cursor, textY, textSize, name)
                .style(UiFontStyle.MEDIUM)
                .color(Theme.Colors.TEXT_TITLE)
                .draw();
        cursor += font.measureWidth(name, textSize, letterSpacing);

        if (donat != null) {
            cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);

            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.SPARKLES)
                    .color(Theme.Colors.ICON_GHOST)
                    .draw();
            cursor += iconSize + gap;

            Render2DUtil.text(cursor, textY, textSize, donat.name())
                    .style(UiFontStyle.MEDIUM)
                    .color(donat.color())
                    .draw();
            cursor += font.measureWidth(donat.name(), textSize, letterSpacing);
        }

        if (healthText != null) {
            cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);

            Render2DUtil.texture(cursor, centerY - iconSize / 2.0F, iconSize, iconSize, Textures.Icons.SCAN_HEART)
                    .color(Theme.Colors.ICON_GHOST)
                    .draw();
            cursor += iconSize + gap;

            Render2DUtil.text(cursor, textY, textSize, healthText)
                    .style(UiFontStyle.MEDIUM)
                    .color(Theme.Colors.TEXT_TITLE)
                    .draw();
            cursor += font.measureWidth(healthText, textSize, letterSpacing);
        }

        if (!equipment.isEmpty()) {
            cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);

            Render2DUtil.flush();

            Matrix3x2fStack pose = event.getGuiGraphicsExtractor().pose();
            float guiScale = (float) mc.getWindow().getGuiScale();
            float scale = itemSize / 16.0F;
            float itemY = Math.round((centerY - itemSize / 2.0F) * guiScale) / guiScale;
            for (ItemStack stack : equipment) {
                float itemX = Math.round(cursor * guiScale) / guiScale;
                pose.pushMatrix();
                pose.translate(itemX, itemY);
                pose.scale(scale);
                event.getGuiGraphicsExtractor().item(stack, 0, 0);
                pose.popMatrix();
                cursor += itemSize + gap;
            }
        }
    }

    private void drawTag2(Render2DEvent event, AbstractClientPlayer player, float tickDelta, float unit) {
        Minecraft mc = event.getClient();
        Vec3 position = Render3DUtil.interpolatedPosition(player, tickDelta)
                .add(0.0D, player.getBbHeight() + HEAD_OFFSET, 0.0D);
        Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, position);
        if (anchor == null) {
            return;
        }

        MsdfFont font = UiFonts.sfProDisplay();
        float size = this.textSize.getValue().floatValue() * unit;
        float letterSpacing = size * UiFontStyle.MEDIUM.letterSpacingEm();
        float textHeight = font.textHeight(size);
        float padX = 4.0F * unit;
        float padY = 1.5F * unit;
        float gap = 2.0F * unit;
        float radius = 3.0F * unit;
        int background = 0x80000000 | (this.bgColor.getValue() & 0xFFFFFF);

        String name = player.getGameProfile().name();
        boolean friend = this.friendTag.getValue() && FriendManager.INSTANCE.isFriend(name);
        boolean bot = this.botTag.getValue()
                && AntiBotFeature.INSTANCE.isEnabled()
                && AntiBotFeature.INSTANCE.isBot(player);
        name = safeName(name);
        String friendText = friend ? "F " : "";
        String botText = bot ? "[BOT] " : "";
        String hpText = this.health.getValue()
                ? " [" + (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount()) + "]"
                : "";

        float headSize = this.head.getValue() ? textHeight : 0.0F;
        float friendWidth = friendText.isEmpty() ? 0.0F : font.measureWidth(friendText, size, letterSpacing);
        float botWidth = botText.isEmpty() ? 0.0F : font.measureWidth(botText, size, letterSpacing);
        float nameWidth = font.measureWidth(name, size, letterSpacing);
        float hpWidth = hpText.isEmpty() ? 0.0F : font.measureWidth(hpText, size, letterSpacing);

        float textWidth = (headSize > 0.0F ? headSize + gap : 0.0F)
                + friendWidth + botWidth + nameWidth + hpWidth;
        float tagWidth = textWidth + padX * 2.0F;
        float tagHeight = textHeight + padY * 2.0F;

        List<ItemStack> equipment = this.items.getValue() ? equipment(player) : List.of();
        float itemSize = 10.0F * unit;
        float itemGap = 1.5F * unit;
        float armorWidth = 0.0F;
        float armorHeight = 0.0F;
        if (!equipment.isEmpty()) {
            armorWidth = equipment.size() * itemSize + (equipment.size() - 1) * itemGap + padX * 2.0F;
            armorHeight = itemSize + padY * 2.0F;
        }

        float centerX = anchor.x();
        float yCursor = anchor.y() - 2.0F * unit;

        if (!equipment.isEmpty()) {
            float armorX = centerX - armorWidth / 2.0F;
            float armorY = yCursor - armorHeight;
            Render2DUtil.rect(armorX, armorY, armorWidth, armorHeight)
                    .color(background)
                    .radius(radius)
                    .blur(6.0F * unit)
                    .draw();

            Render2DUtil.flush();

            Matrix3x2fStack pose = event.getGuiGraphicsExtractor().pose();
            float guiScale = (float) mc.getWindow().getGuiScale();
            float scale = itemSize / 16.0F;
            float itemY = Math.round((armorY + (armorHeight - itemSize) / 2.0F) * guiScale) / guiScale;
            float cursor = centerX - (armorWidth - padX * 2.0F) / 2.0F;
            for (ItemStack stack : equipment) {
                float itemX = Math.round(cursor * guiScale) / guiScale;
                pose.pushMatrix();
                pose.translate(itemX, itemY);
                pose.scale(scale);
                event.getGuiGraphicsExtractor().item(stack, 0, 0);
                pose.popMatrix();
                cursor += itemSize + itemGap;
            }

            yCursor = armorY - 1.0F * unit;
        }

        float tagX = centerX - tagWidth / 2.0F;
        float tagY = yCursor - tagHeight;
        Render2DUtil.rect(tagX, tagY, tagWidth, tagHeight)
                .color(background)
                .radius(radius)
                .blur(6.0F * unit)
                .draw();

        float centerY = tagY + tagHeight / 2.0F;
        float textY = font.centeredTextY(centerY, size);
        float cursor = tagX + padX;

        if (headSize > 0.0F) {
            drawHead(player, cursor, centerY - headSize / 2.0F, headSize);
            cursor += headSize + gap;
        }
        if (!friendText.isEmpty()) {
            Render2DUtil.text(cursor, textY, size, friendText)
                    .style(UiFontStyle.MEDIUM)
                    .color(this.friendTagColor.getValue())
                    .draw();
            cursor += friendWidth;
        }
        if (!botText.isEmpty()) {
            Render2DUtil.text(cursor, textY, size, botText)
                    .style(UiFontStyle.MEDIUM)
                    .color(0xFFFF5555)
                    .draw();
            cursor += botWidth;
        }
        Render2DUtil.text(cursor, textY, size, name)
                .style(UiFontStyle.MEDIUM)
                .color(this.nameColor.getValue())
                .draw();
        cursor += nameWidth;
        if (!hpText.isEmpty()) {
            Render2DUtil.text(cursor, textY, size, hpText)
                    .style(UiFontStyle.MEDIUM)
                    .color(this.hpColor.getValue())
                    .draw();
        }
    }

    private static void drawHead(AbstractClientPlayer player, float x, float y, float size) {
        Identifier skin = player.getSkin().body().texturePath();
        float radius = size * 0.2F;
        Render2DUtil.texture(x, y, size, size, skin)
                .managed()
                .uv(8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F)
                .radius(radius)
                .draw();
        Render2DUtil.texture(x, y, size, size, skin)
                .managed()
                .uv(40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F)
                .radius(radius)
                .draw();
    }

    private float drawDivider(float cursor, float centerY, float dividerWidth, float gap, float unit) {
        cursor += gap;
        Render2DUtil.rect(cursor, centerY - DIVIDER_HEIGHT * unit / 2.0F, dividerWidth, DIVIDER_HEIGHT * unit)
                .color(DIVIDER_COLOR)
                .draw();
        return cursor + dividerWidth + gap;
    }

    private List<ItemStack> equipment(AbstractClientPlayer player) {
        List<ItemStack> stacks = this.equipmentScratch;
        stacks.clear();
        for (EquipmentSlot slot : EQUIPMENT_ORDER) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static Privilege resolvePrivilege(AbstractClientPlayer player) {
        PlayerTeam team = player.getTeam();
        UUID id = player.getUUID();
        CachedPrivilege cached = PRIVILEGE_CACHE.get(id);
        if (cached != null && cached.team() == team) {
            return cached.privilege();
        }
        Privilege privilege = computePrivilege(team);
        PRIVILEGE_CACHE.put(id, new CachedPrivilege(team, privilege));
        return privilege;
    }

    private static Privilege computePrivilege(PlayerTeam team) {
        if (team == null) {
            return null;
        }
        Component prefix = team.getPlayerPrefix();
        if (prefix == null) {
            return null;
        }

        String raw = prefix.getString();
        String name = cleanName(raw);
        if (name.isEmpty()) {
            return null;
        }

        Integer color = extractColor(prefix);
        if (color == null) {
            color = legacyColor(raw);
        }
        int rgb = color != null
                ? ColorUtil.rgba(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
                : Theme.getAccent();
        return new Privilege(name, rgb);
    }

    private static String safeName(String name) {
        String cached = NAME_CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        MsdfFont font = UiFonts.sfProDisplay();
        String result;
        if (font.canRender(name)) {
            result = name;
        } else {
            StringBuilder builder = new StringBuilder(name.length());
            for (int i = 0; i < name.length(); ) {
                int codePoint = name.codePointAt(i);
                i += Character.charCount(codePoint);
                if (Character.isWhitespace(codePoint)) {
                    if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ' ') {
                        builder.append(' ');
                    }
                    continue;
                }
                if (font.hasGlyph(codePoint)) {
                    builder.appendCodePoint(codePoint);
                }
            }
            String trimmed = builder.toString().trim();
            result = trimmed.isEmpty() ? name : trimmed;
        }
        if (NAME_CACHE.size() > 1024) {
            NAME_CACHE.clear();
        }
        NAME_CACHE.put(name, result);
        return result;
    }

    private static String cleanName(String text) {
        StringBuilder clean = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '§') {
                i++;
                continue;
            }
            if (!isBasicLetterOrDigit(character) && character != ' ') {
                continue;
            }
            if (character == ' ' && (clean.isEmpty() || clean.charAt(clean.length() - 1) == ' ')) {
                continue;
            }
            clean.append(character);
        }
        int end = clean.length();
        while (end > 0 && clean.charAt(end - 1) == ' ') {
            end--;
        }
        clean.setLength(end);
        return clean.toString();
    }

    private static boolean isBasicLetterOrDigit(char character) {
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9')
                || (character >= 'а' && character <= 'я')
                || (character >= 'А' && character <= 'Я')
                || character == 'ё' || character == 'Ё';
    }

    private static Integer legacyColor(String text) {
        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != '§') {
                continue;
            }
            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting == null) {
                continue;
            }
            TextColor color = TextColor.fromLegacyFormat(formatting);
            if (color != null) {
                return color.getValue();
            }
        }
        return null;
    }

    private static Integer extractColor(Component component) {
        TextColor color = component.getStyle().getColor();
        if (color != null) {
            return color.getValue();
        }
        for (Component sibling : component.getSiblings()) {
            Integer siblingColor = extractColor(sibling);
            if (siblingColor != null) {
                return siblingColor;
            }
        }
        return null;
    }
}
