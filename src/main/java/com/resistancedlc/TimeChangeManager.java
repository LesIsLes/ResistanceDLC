package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

/**
 * TimeChangeManager — визуальное изменение времени суток (26.1).
 *
 * Client-side only. Не влияет на сервер и других игроков.
 *
 * Миксин TimeChangeMixin подменяет ClientClockManager.getTotalTicks().
 */
public final class TimeChangeManager {

    private TimeChangeManager() {}

    /**
     * Возвращает fake-время суток в тиках (0..24000) или -1,
     * если override не нужен.
     */
    public static long getFakeDayTime() {
        if (!ModConfig.timeChangeEnabled) return -1L;
        return ModConfig.timeChangeValue;
    }

    /**
     * Форматирует тики в строку "HH:MM".
     */
    public static String formatTime(long dayTime) {
        long ticks = ((dayTime % 24000L) + 24000L) % 24000L;
        long hours = ((ticks / 1000L) + 6L) % 24L;
        long minutes = (ticks % 1000L) * 60L / 1000L;
        return String.format("%02d:%02d", hours, minutes);
    }
}