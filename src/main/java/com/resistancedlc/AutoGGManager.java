package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AutoGGManager — авто-сообщение в чат после убийства ИГРОКА (PvP).
 *
 * Триггеры:
 *   1. StatsTrackerManager.onChatMessage() — если сервер пишет килл в чат.
 *   2. ClientPacketListenerMixin — event 3 (ENTITY_DEATH) + KillTracker.
 *
 * Дедупликация по victim: если тот же victim в течение 3 сек — пропускаем.
 */
public class AutoGGManager {

    private static final ConcurrentLinkedQueue<Long> PENDING_KILLS = new ConcurrentLinkedQueue<>();

    private static String lastVictim = "";
    private static long lastVictimTime = 0;
    private static final long DEDUP_VICTIM_MS = 3000L;

    public static void onPlayerKilled() {
        if (!ModConfig.autoGgEnabled) return;

        String victim = ModConfig.autoGgLastVictim;
        long now = System.currentTimeMillis();

        if (victim != null && !victim.isEmpty()
                && victim.equals(lastVictim)
                && (now - lastVictimTime) < DEDUP_VICTIM_MS) {
            return; // дубликат — пропускаем молча
        }

        lastVictim = victim != null ? victim : "";
        lastVictimTime = now;

        PENDING_KILLS.offer(now);
    }

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
        long delayMs = (long) (ModConfig.autoGgDelay * 1000L);

        Long killTime;
        while ((killTime = PENDING_KILLS.peek()) != null) {
            if (now - killTime < delayMs) break;
            PENDING_KILLS.poll();

            String template = ModConfig.autoGgTemplate;
            if (template == null || template.isEmpty()) template = "GG";

            String message;
            if (template.contains("%s")) {
                String victimName = ModConfig.autoGgLastVictim;
                if (victimName == null || victimName.isEmpty()) {
                    message = template.replace("%s", "")
                            .replaceAll("\\s+", " ")
                            .trim();
                } else {
                    message = template.replace("%s", victimName);
                }
            } else {
                message = template;
            }

            try {
                client.getConnection().sendChat(message);
                ModConfig.autoGgLastVictim = "";
            } catch (Exception e) {
                ResistanceDLC.LOGGER.error("[AutoGG] Failed to send: " + e.getMessage());
            }
        }
    }

    public static void reset() {
        PENDING_KILLS.clear();
        lastVictim = "";
        lastVictimTime = 0;
    }
}