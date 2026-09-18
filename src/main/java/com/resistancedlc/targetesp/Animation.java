package com.resistancedlc.targetesp;

import java.util.Objects;

/**
 * Animation — плавная интерполяция значения.
 * Портировано с anomalith.
 */
public final class Animation {

    private final long duration;
    private float value;
    private final Easing easing;
    private long startTime;
    private float startValue;
    private float targetValue;

    public Animation(long duration, float initialValue, Easing easing) {
        if (duration <= 0L) {
            throw new IllegalArgumentException("Animation duration must be positive");
        }
        this.duration = duration;
        this.easing = Objects.requireNonNull(easing, "easing");
        this.value = initialValue;
        this.startValue = initialValue;
        this.targetValue = initialValue;
        this.startTime = nowMs();
    }

    public float update(boolean value) {
        return update(value ? 1.0f : 0.0f);
    }

    public float update(float newValue) {
        long currentTime = nowMs();
        sample(currentTime);
        if (Float.compare(newValue, this.targetValue) != 0) {
            this.startValue = this.value;
            this.targetValue = newValue;
            this.startTime = currentTime;
        }
        return this.value;
    }

    private void sample(long currentTime) {
        long elapsed = currentTime - this.startTime;
        if (elapsed >= this.duration) {
            this.value = this.targetValue;
            return;
        }
        float progress = (float) elapsed / (float) this.duration;
        float easedProgress = this.easing.ease(progress, 0.0f, 1.0f, 1.0f);
        this.value = this.startValue + (this.targetValue - this.startValue) * easedProgress;
    }

    public void reset(float newValue) {
        this.value = newValue;
        this.startValue = newValue;
        this.targetValue = newValue;
        this.startTime = nowMs();
    }

    private static long nowMs() {
        return System.nanoTime() / 1_000_000L;
    }
}