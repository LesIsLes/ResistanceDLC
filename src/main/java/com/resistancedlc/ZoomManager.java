package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;

/**
 * ZoomManager — плавный zoom, независимый от FPS.
 *
 * Обновляется из FOVMixin.getFov() каждый КАДР (60-240 раз/сек),
 * а не из tick (20 раз/сек). Это даёт одинаковую визуальную плавность
 * при любом FPS и любом tick rate.
 *
 * zoomSmoothness (0.05..1.0):
 *   0.05 = очень плавно (~3 сек)
 *   0.15 = плавно (~1.5 сек) — дефолт
 *   0.50 = быстро (~0.5 сек)
 *   1.00 = мгновенно
 */
public final class ZoomManager {

    private ZoomManager() {}

    /** Текущее сглаженное значение (1.0 = без зума, <1.0 = приближено). */
    private static float current = 1.0f;

    /** Целевое значение. */
    private static float target = 1.0f;

    /** Время последнего обновления (нано). */
    private static long lastUpdateNanos = 0L;

    /** Порог «достигнуто». */
    private static final float EPSILON = 0.0005f;

    /** Максимальный dt, чтобы не было рывков после лага. */
    private static final float MAX_DT = 0.1f;

    /**
     * Вызывается из FOVMixin.getFov() на каждом кадре.
     * @param tickDelta — partialTick от GameRenderer (не обязателен, но оставлен для API).
     */
    public static void updatePerFrame(float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            // В меню/чате — плавно возвращаем к 1.0
            target = 1.0f;
        } else {
            boolean keyDown = KeyBindings.zoomKey != null && KeyBindings.zoomKey.isDown();
            if (ModConfig.zoomEnabled && keyDown) {
                target = 1.0f / Math.max(1.0f, ModConfig.zoomFactor);
            } else {
                target = 1.0f;
            }
        }

        // Delta time
        long now = System.nanoTime();
        if (lastUpdateNanos == 0L) {
            lastUpdateNanos = now;
            current = target;
            return;
        }
        float dt = (now - lastUpdateNanos) / 1_000_000_000.0f;
        lastUpdateNanos = now;

        if (dt > MAX_DT) dt = MAX_DT;
        if (dt < 0.0f) dt = 0.0f;

        // Сглаживание
        float s = Math.max(0.05f, Math.min(1.0f, ModConfig.zoomSmoothness));

        if (s >= 0.999f) {
            current = target;
            return;
        }

        // k = 0.5..12 — эмпирически подобрано.
        //   s=0.05 → k≈0.5  → ~9 сек до 99%
        //   s=0.15 → k≈2.7  → ~1.7 сек
        //   s=0.50 → k≈6.8  → ~0.7 сек
        //   s=1.00 → мгновенно
        float normalized = (s - 0.05f) / 0.95f;  // 0..1
        float k = 0.5f + normalized * 11.5f;

        float speed = 1.0f - (float) Math.exp(-k * dt);
        current += (target - current) * speed;

        if (Math.abs(target - current) < EPSILON) {
            current = target;
        }
    }

    /** Текущее сглаженное значение. */
    public static float getCurrentZoom() {
        return current;
    }

    /** Мгновенный сброс (дисконнект, смена мира). */
    public static void reset() {
        current = 1.0f;
        target = 1.0f;
        lastUpdateNanos = 0L;
    }
}