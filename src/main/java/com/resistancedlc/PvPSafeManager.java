package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/**
 * Логика PvPSafe — блокировка выхода из игры и опасных команд
 * при активном PvP-таймере.
 */
public class PvPSafeManager {

    private static final Pattern AN_PATTERN = Pattern.compile("^/?an\\d*$");

    private static long lastHitTime = 0;

    // ===================== ТАЙМЕР =====================

    public static void recordHit() {
        lastHitTime = System.currentTimeMillis();
    }

    public static int getRemainingSeconds() {
        if (!ModConfig.pvpSafeEnabled) return 0;
        if (lastHitTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - lastHitTime;
        long total = (long) ModConfig.pvpSafeTimer * 1000L;
        if (elapsed >= total) return 0;
        return (int) ((total - elapsed) / 1000L) + 1;
    }

    public static boolean isInCombat() {
        return getRemainingSeconds() > 0;
    }

    public static void reset() {
        lastHitTime = 0;
    }

    // ===================== ПАРСИНГ ЧАТА =====================

    public static boolean onChatMessage(String message) {
        if (message == null || message.isEmpty()) return false;
        if (!ModConfig.pvpSafeEnabled) return false;

        String lower = message.toLowerCase();

        if (lower.contains("no longer in combat")
                || lower.contains("вышли из боя")
                || lower.contains("combat tag expired")
                || lower.contains("бой окончен")
                || lower.contains("боевой режим окончен")
                || lower.contains("combat ended")) {
            reset();
            return true;
        }

        if (lower.contains("in combat")
                || lower.contains("в бою")
                || lower.contains("боевой")
                || lower.contains("combat tag")
                || lower.contains("боевой режим")
                || lower.contains("combat started")) {
            recordHit();
            return true;
        }

        return false;
    }

    // ===================== ОПАСНЫЕ КОМАНДЫ =====================

    public static boolean isDangerousCommand(String rawCommand) {
        if (rawCommand == null) return false;
        String cmd = rawCommand.trim().toLowerCase();

        if (cmd.equals("hub")) return true;
        if (cmd.equals("logout")) return true;
        if (cmd.equals("limbo")) return true;
        if (cmd.equals("suicide")) return true;

        if (AN_PATTERN.matcher(cmd).matches()) return true;

        return false;
    }

    public static void sendBlockedMessage(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        int remaining = getRemainingSeconds();
        client.player.displayClientMessage(
                Component.literal("§c" + LocalizationManager.get("gui.resistancedlc.pvp.blocked_command",
                        command, remaining)),
                true
        );
    }

    public static void sendQuitBlockedMessage() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        int remaining = getRemainingSeconds();
        client.player.displayClientMessage(
                Component.literal("§c" + LocalizationManager.get("gui.resistancedlc.pvp.blocked_quit",
                        remaining)),
                true
        );
    }
}