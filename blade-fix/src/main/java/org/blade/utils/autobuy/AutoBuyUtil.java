package org.blade.utils.autobuy;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoBuyUtil {
    private static final Pattern patternFuntime = Pattern.compile("\\$\\s*([0-9][\\d,]*)");
    private static final Pattern patternHollyWorld = Pattern.compile("Цена:(?:.*?\\{\"text\":\"([\\d ]+)\")");
    private static final ConcurrentHashMap<Long, String> nbtCache = new ConcurrentHashMap<>();

    private AutoBuyUtil() {
    }

    public static int getPrice(String nbt) {
        Matcher matcher = patternFuntime.matcher(nbt);
        if (matcher.find()) {
            return parseNumber(matcher.group(1));
        }
        matcher = patternHollyWorld.matcher(nbt);
        if (matcher.find()) {
            return parseNumber(matcher.group(1));
        }
        return Integer.MAX_VALUE;
    }

    public static int getPrice(ItemStack stack) {
        return getPrice(getNBT(stack));
    }

    public static int getServerPrice(ItemStack stack) {
        return getPrice(stack) / Math.max(1, stack.getCount());
    }

    private static int parseNumber(String value) {
        String clean = value.replaceAll(" ", "").replace(",", "").replaceAll("[^0-9]", "");
        if (clean.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public static long getBalance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return -1L;
        }
        PlayerTeam team = mc.player.getTeam();
        if (team == null) {
            return -1L;
        }
        String prefix = stripFormatting(team.getPlayerPrefix().getString());
        if (!prefix.contains("монет")) {
            return -1L;
        }
        Matcher matcher = Pattern.compile("[\\d ]+").matcher(prefix);
        if (matcher.find()) {
            String amount = matcher.group().replaceAll(" ", "").trim();
            if (!amount.isEmpty()) {
                try {
                    return Long.parseLong(amount);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1L;
    }

    public static String stripFormatting(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7') {
                i++;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String getServer() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getCurrentServer() != null && mc.getCurrentServer().ip != null
                ? mc.getCurrentServer().ip
                : "";
    }

    public static boolean isFuntimeServer() {
        return getServer().toLowerCase(Locale.ROOT).contains("funtime");
    }

    public static boolean isHollyworldServer() {
        String server = getServer().toLowerCase(Locale.ROOT);
        return server.contains("hollyworld") || server.contains("holyworld");
    }

    public static String getNBT(ItemStack stack) {
        return getTag(stack).toString();
    }

    public static String getTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "{}";
        }
        long key = stack.getComponentsPatch().hashCode() ^ (stack.getItem().hashCode() * 31L);
        String cached = nbtCache.get(key);
        if (cached != null) {
            return cached;
        }
        String result;
        try {
            Minecraft mc = Minecraft.getInstance();
            DataComponentPatch patch = stack.getComponentsPatch();
            Tag encoded = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, patch).result()
                    .orElse(NbtOps.INSTANCE.empty());
            result = encoded.toString();
        } catch (Exception e) {
            result = "{}";
        }
        if (result.length() <= 128) {
            nbtCache.put(key, result);
        }
        return result;
    }

    public static void clearCache() {
        nbtCache.clear();
    }
}