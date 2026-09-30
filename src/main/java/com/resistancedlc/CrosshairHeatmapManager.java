package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * CrosshairHeatmapManager — кольцевой буфер последних ударов (hit/miss).
 *
 * Итерация 4: метки хранят МИРОВЫЕ координаты (Vec3), а не экранные.
 * Проекция в экран — на этапе рендера (CrosshairHeatmapHud).
 */
public class CrosshairHeatmapManager {

    /** Одна метка. */
    public static class Mark {
        public final Vec3 worldPos;       // мировая точка
        public final long timestamp;      // ms
        public final boolean hit;         // true — попадание, false — промах

        public Mark(Vec3 worldPos, long timestamp, boolean hit) {
            this.worldPos = worldPos;
            this.timestamp = timestamp;
            this.hit = hit;
        }
    }

    private static final Deque<Mark> MARKS = new ArrayDeque<>();

    /**
     * Записать попадание.
     * @param worldPos мировая точка попадания
     */
    public static void recordHit(Vec3 worldPos) {
        if (!ModConfig.crosshairHeatmapEnabled) return;
        if (worldPos == null) return;

        long now = System.currentTimeMillis();
        MARKS.addLast(new Mark(worldPos, now, true));
        ModConfig.crosshairHeatmapTotalHits++;
        trim(now);
    }

    /**
     * Записать промах.
     * @param worldPos мировая точка промаха
     */
    public static void recordMiss(Vec3 worldPos) {
        if (!ModConfig.crosshairHeatmapEnabled) return;
        if (worldPos == null) return;

        long now = System.currentTimeMillis();
        MARKS.addLast(new Mark(worldPos, now, false));
        ModConfig.crosshairHeatmapTotalMisses++;
        trim(now);
    }

    /** Удаляет устаревшие и превышающие лимит записи. */
    private static void trim(long now) {
        long lifetime = ModConfig.crosshairHeatmapMarkLifetime;

        while (!MARKS.isEmpty()) {
            Mark first = MARKS.peekFirst();
            if (first != null && now - first.timestamp > lifetime) {
                MARKS.pollFirst();
            } else {
                break;
            }
        }

        int max = Math.max(10, ModConfig.crosshairHeatmapMaxMarks);
        while (MARKS.size() > max) {
            MARKS.pollFirst();
        }
    }

    /** Возвращает список актуальных меток (только не истёкшие по времени). */
    public static List<Mark> getActiveMarks() {
        long now = System.currentTimeMillis();
        long lifetime = ModConfig.crosshairHeatmapMarkLifetime;

        List<Mark> result = new ArrayList<>();
        for (Mark m : MARKS) {
            if (now - m.timestamp <= lifetime) {
                result.add(m);
            }
        }
        return result;
    }

    /** Сброс статистики + очистка меток. */
    public static void resetStats() {
        MARKS.clear();
        ModConfig.crosshairHeatmapTotalHits = 0;
        ModConfig.crosshairHeatmapTotalMisses = 0;
        ConfigManager.save();
    }

    /** Полная очистка (при выходе из мира). */
    public static void reset() {
        MARKS.clear();
    }

    public static double getHitChance() {
        int total = ModConfig.crosshairHeatmapTotalHits + ModConfig.crosshairHeatmapTotalMisses;
        if (total <= 0) return 0.0;
        return 100.0 * ModConfig.crosshairHeatmapTotalHits / total;
    }

    public static double getMissChance() {
        int total = ModConfig.crosshairHeatmapTotalHits + ModConfig.crosshairHeatmapTotalMisses;
        if (total <= 0) return 0.0;
        return 100.0 * ModConfig.crosshairHeatmapTotalMisses / total;
    }
}