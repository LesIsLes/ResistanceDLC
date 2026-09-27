package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * AutoGGManager — авто-сообщение в чат после убийства игрока.
 *
 * Триггер: StatsTrackerManager.onChatMessage() → +1 kill → вызывает onPlayerKilled().
 * Отправка сообщения — через очередь (безопасно из Netty-потока).
 */
public class AutoGGManager {

    /** Очередь сообщений для отправки из рендер-потока. */
    private static final ConcurrentLinkedQueue<Long> PENDING_KILLS = new ConcurrentLinkedQueue<>();

    /**
     * Вызывается при подтверждённом убийстве игрока (из StatsTrackerManager).
     * Запускает таймер задержки.
     */
    public static void onPlayerKilled() {
        if (!ModConfig.autoGgEnabled) return;
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

    public static void reset() {
        PENDING_KILLS.clear();
    }
}