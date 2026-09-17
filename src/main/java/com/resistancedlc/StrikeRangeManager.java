package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

/**
 * StrikeRangeManager — хранит последнюю дистанцию удара.
 * Рендер — в ResistanceDLCClient.renderHud().
 */
public class StrikeRangeManager {

    private static double lastDistance = 0.0;
    private static long lastHitTime = 0;

    /**
     * Вызывается при ударе по сущности.
     */
    public static void onHit(double distance) {
        lastDistance = distance;
        lastHitTime = System.currentTimeMillis();
    }

    /**
     * Активна ли сейчас подсветка (в пределах showTime).
     */
    public static boolean isActive() {
        if (lastHitTime == 0) return false;
        return System.currentTimeMillis() - lastHitTime < ModConfig.strikeRangeShowTime;
    }

    public static double getDistance() {
        return lastDistance;
    }

    /**
     * Прогресс 0..1 — для fade-out.
     */
    public static float getFadeProgress() {
        if (lastHitTime == 0) return 0.0f;
        long elapsed = System.currentTimeMillis() - lastHitTime;
        long total = ModConfig.strikeRangeShowTime;
        if (elapsed >= total) return 0.0f;
        return 1.0f - (float) elapsed / total;
    }

    public static void reset() {
        lastDistance = 0.0;
        lastHitTime = 0;
    }
}