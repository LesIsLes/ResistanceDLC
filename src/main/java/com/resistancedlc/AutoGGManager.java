package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AutoGGManager — авто-сообщение в чат после убийства игрока.
 *
 * Триггеры:
 *   1. Миксин AutoGGMixin на ClientboundPlayerCombatKillPacket (надёжно).
 *   2. Fallback: парсинг чата (для серверов, где пакет не приходит,
 *      но в чат пишется "X was slain by Y" / "Y killed X").
 *
 * Отправка сообщения — через очередь (безопасно из Netty-потока).
 */
public class AutoGGManager {

    /** Очередь сообщений для отправки из рендер-потока. */
    private static final ConcurrentLinkedQueue<Long> PENDING_KILLS = new ConcurrentLinkedQueue<>();

    /**
     * Вызывается при подтверждённом убийстве игрока.
     * Запускает таймер задержки.
     */
    public static void onPlayerKilled() {
        if (!ModConfig.autoGgEnabled) return;
        if (!ModConfig.autoGgOnlyPlayers) return;
        PENDING_KILLS.offer(System.currentTimeMillis());
    }

    /**
     * Вызывается при убийстве моба/любой сущности (если autoGgOnlyPlayers == false).
     */
    public static void onEntityKilled() {
        if (!ModConfig.autoGgEnabled) return;
        if (ModConfig.autoGgOnlyPlayers) return;
        PENDING_KILLS.offer(System.currentTimeMillis());
    }

    /**
     * Тик — проверяет очередь и отправляет сообщения через нужную задержку.
     */
    public static void tick() {
        if (PENDING_KILLS.isEmpty()) return;
        if (!ModConfig.autoGgEnabled) {
            PENDING_KILLS.clear();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            PENDING_KILLS.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long delayMs = (long)(ModConfig.autoGgDelay * 1000L);

        Long killTime;
        while ((killTime = PENDING_KILLS.peek()) != null) {
            if (now - killTime < delayMs) break;
            PENDING_KILLS.poll();

            String template = ModConfig.autoGgTemplate;
            if (template == null || template.isEmpty()) template = "GG";

            String message;
            if (template.contains("%s")) {
                String victimName = ModConfig.autoGgLastVictim;
                if (victimName == null || victimName.isEmpty()) victimName = "?";
                message = template.replace("%s", victimName);
            } else {
                message = template;
            }

            try {
                client.getConnection().sendChat(message);
            } catch (Exception e) {
                ResistanceDLC.LOGGER.error("AutoGG failed to send: " + e.getMessage());
            }
        }
    }

    /**
     * Fallback: парсинг чата. Ловим сообщения вида:
     *   - "X was slain by Y"
     *   - "X was killed by Y"
     *   - "Y killed X"
     *   - "Y убил X"
     *   - "X убит Y"
     * Если Y == наш ник → триггерим.
     */
    public static void onChatMessage(String message) {
        if (!ModConfig.autoGgEnabled) return;
        if (message == null || message.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String myName = client.player.getName().getString();
        String lower = message.toLowerCase();
        String myNameLower = myName.toLowerCase();

        if (!lower.contains(myNameLower)) return;

        boolean isKill =
                lower.contains("was slain by " + myNameLower)
                        || lower.contains("was killed by " + myNameLower)
                        || lower.contains("was shot by " + myNameLower)
                        || lower.contains("was blown up by " + myNameLower)
                        || lower.contains("was fireballed by " + myNameLower)
                        || lower.contains(myNameLower + " killed ")
                        || lower.contains(myNameLower + " slain ")
                        || lower.contains(myNameLower + " убил ")
                        || lower.contains(myNameLower + " застрелил ")
                        || lower.contains("убит " + myNameLower)
                        || lower.contains("убита " + myNameLower)
                        || lower.contains("сражён " + myNameLower)
                        || lower.contains("сражена " + myNameLower);

        if (!isKill) return;

        String victim = extractVictim(message, myName);
        if (victim != null) {
            ModConfig.autoGgLastVictim = victim;
        }

        if (ModConfig.autoGgOnlyPlayers) {
            PENDING_KILLS.offer(System.currentTimeMillis());
        }
    }

    /**
     * Извлекает имя жертвы из сообщения.
     */
    private static String extractVictim(String message, String attackerName) {
        String lower = message.toLowerCase();
        String attackerLower = attackerName.toLowerCase();

        String[] markers = {
                " was slain by ", " was killed by ", " was shot by ",
                " was blown up by ", " was fireballed by ",
                " убит ", " убита ", " сражён ", " сражена "
        };

        for (String marker : markers) {
            int idx = lower.indexOf(marker);
            if (idx > 0) {
                String before = message.substring(0, idx).trim();
                String[] parts = before.split("\\s+");
                if (parts.length > 0) {
                    String candidate = parts[parts.length - 1];
                    candidate = candidate.replaceAll("§.", "").trim();
                    if (!candidate.isEmpty() && !candidate.equalsIgnoreCase(attackerName)) {
                        return candidate;
                    }
                }
            }
        }

        String[] killMarkers = { " killed ", " slain ", " убил ", " застрелил " };
        for (String marker : killMarkers) {
            int idx = lower.indexOf(attackerLower + marker);
            if (idx >= 0) {
                int start = idx + attackerLower.length() + marker.length();
                if (start < message.length()) {
                    String after = message.substring(start).trim();
                    String[] parts = after.split("\\s+");
                    if (parts.length > 0) {
                        String candidate = parts[0].replaceAll("§.", "").trim();
                        if (!candidate.isEmpty()) return candidate;
                    }
                }
            }
        }

        return null;
    }

    public static void reset() {
        PENDING_KILLS.clear();
    }
}