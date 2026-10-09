package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

/**
 * TimeChangeManager — визуальное изменение времени суток.
 *
 * Client-side only: другие игроки видят настоящее время.
 * Плагин на сервер не нужен.
 *
 * Значение хранится в ModConfig.timeChangeValue (0..24000 тиков).
 * Миксин TimeChangeMixin подменяет ClientLevel.getDayTime() и getTimeOfDay().
 */
public final class TimeChangeManager {

    private TimeChangeManager() {}

    /**
     * Возвращает fake-время суток (0..24000).
     * Если фича выключена — возвращает -1 (сигнал миксину, что override не нужен).
     */
    public static long getFakeDayTime() {
        if (!ModConfig.timeChangeEnabled) return -1L;
        return ModConfig.timeChangeValue;
    }

    /**
     * Возвращает fake-время в виде float (0.0..1.0), где 0.0 = полночь, 0.5 = полдень.
     * Используется для getTimeOfDay(float).
     */
    public static float getFakeTimeOfDay(float partialTick) {
        if (!ModConfig.timeChangeEnabled) return -1.0f;
        // getTimeOfDay возвращает [0..1) — долю суток
        long dayTime = ModConfig.timeChangeValue;
        return (float) (dayTime % 24000L) / 24000.0f;
    }

    /**
     * Форматирует тики в строку "HH:MM".
     * В Minecraft: 0 = 06:00 (рассвет), 6000 = 12:00 (полдень),
     *              12000 = 18:00 (закат), 18000 = 00:00 (полночь).
     *
     * Формула: (dayTime / 1000 + 6) % 24 = часы.
     */
    public static String formatTime(long dayTime) {
        long ticks = ((dayTime % 24000L) + 24000L) % 24000L;
        long hours = ((ticks / 1000L) + 6L) % 24L;
        long minutes = (ticks % 1000L) * 60L / 1000L;
        return String.format("%02d:%02d", hours, minutes);
    }
}