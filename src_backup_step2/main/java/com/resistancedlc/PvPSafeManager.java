package com.resistancedlc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/**
 * Логика PvPSafe — блокировка выхода из игры и опасных команд
 * при активном PvP-таймере (30 сек с последнего удара).
 */
public class PvPSafeManager {

    /** Регулярка для /an<любое число до 9999>. */
    private static final Pattern AN_PATTERN = Pattern.compile("^/?an\\d*$");

    /** Время последнего удара по нам (мс). */
    private static long lastHitTime = 0;

    /**
     * Записать удар по игроку (из миксина на Player.hurt).
     */
    public static void recordHit() {
        lastHitTime = System.currentTimeMillis();
    }

    /**
     * Сколько секунд осталось до конца боя.
     * Возвращает 0, если бой не активен.
     */
    public static int getRemainingSeconds() {
        if (!MyCustomScreen.pvpSafeEnabled) return 0;
        if (lastHitTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - lastHitTime;
        long total = (long) MyCustomScreen.pvpSafeTimer * 1000L;
        if (elapsed >= total) return 0;
        return (int) ((total - elapsed) / 1000L) + 1;
    }

    /** Активен ли сейчас PvP-бой. */
    public static boolean isInCombat() {
        return getRemainingSeconds() > 0;
    }

    /** Сбросить таймер (при выходе из мира). */
    public static void reset() {
        lastHitTime = 0;
    }

    /**
     * Проверить, является ли команда опасной.
     * @param rawCommand команда БЕЗ ведущего слэша, например "an123" или "hub"
     */
    public static boolean isDangerousCommand(String rawCommand) {
        if (rawCommand == null) return false;
        String cmd = rawCommand.trim().toLowerCase();

        // /hub
        if (cmd.equals("hub")) return true;
        // /logout
        if (cmd.equals("logout")) return true;
        // /limbo
        if (cmd.equals("limbo")) return true;
        // /suicide
        if (cmd.equals("suicide")) return true;

        // /an<число> — /an, /an1, /an9999
        if (AN_PATTERN.matcher(cmd).matches()) return true;

        return false;
    }

    /**
     * Сообщение игроку о блокировке команды.
     */
    public static void sendBlockedMessage(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        int remaining = getRemainingSeconds();
        client.player.displayClientMessage(
                Component.literal("§c[PvPSafe] §7Команда §f/" + command
                        + " §7заблокирована! §cБой: " + remaining + " сек"),
                true
        );
    }

    /**
     * Сообщение о блокировке выхода.
     */
    public static void sendQuitBlockedMessage() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        int remaining = getRemainingSeconds();
        client.player.displayClientMessage(
                Component.literal("§c[PvPSafe] §7Выход заблокирован! §cБой: "
                        + remaining + " сек"),
                true
        );
    }
}