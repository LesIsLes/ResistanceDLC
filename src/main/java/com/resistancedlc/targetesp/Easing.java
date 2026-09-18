package com.resistancedlc.targetesp;

/**
 * Easing — функция плавности (cubic bezier).
 * Портировано с anomalith.
 */
public interface Easing {

    Easing FIGMA_EASE_IN_OUT = Easing.generate(0.42, 0.0, 0.58, 1.0);

    static Easing generate(final double x1, final double y1, final double x2, final double y2) {
        return new Easing() {
            @Override
            public float ease(float t, float b, float c, float d) {
                if (d <= 0.0f || t <= 0.0f) return b;
                if (t >= d) return b + c;

                float progress = t / d;
                float tBez = solveTBez((float) x1, (float) x2, progress);
                float y = bezierY(tBez, (float) y1, (float) y2);
                return b + c * y;
            }

            private float solveTBez(float x12, float x22, float progress) {
                float t = progress;
                for (int i = 0; i < 8; i++) {
                    float x = bezierX(t, x12, x22);
                    float dx = bezierDX(t, x12, x22);
                    if (Math.abs(x - progress) < 1.0E-5f || Math.abs(dx) < 1.0E-6f) break;
                    t -= (x - progress) / dx;
                    t = Math.max(0.0f, Math.min(1.0f, t));
                }
                return t;
            }

            private float bezierX(float t, float x12, float x22) {
                return 3.0f * (1.0f - t) * (1.0f - t) * t * x12
                        + 3.0f * (1.0f - t) * t * t * x22
                        + t * t * t;
            }

            private float bezierDX(float t, float x12, float x22) {
                return 3.0f * ((1.0f - t) * (1.0f - 3.0f * t) * x12
                        + (2.0f * t - 3.0f * t * t) * x22)
                        + 3.0f * t * t;
            }

            private float bezierY(float t, float y12, float y22) {
                return 3.0f * (1.0f - t) * (1.0f - t) * t * y12
                        + 3.0f * (1.0f - t) * t * t * y22
                        + t * t * t;
            }
        };
    }

    float ease(float t, float b, float c, float d);
}