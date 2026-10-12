package com.resistancedlc.voice;

/**
 * DtwComparator — Dynamic Time Warping для MFCC.
 *
 * v3.0.0-alpha.4 — ПАТЧ:
 *   - MIN_LENGTH_RATIO снижен с 0.30 до 0.20 (мягче к коротким фразам).
 *   - Добавлен флаг DIAGNOSTIC_LOGS для отладки lengthRatio.
 */
public class DtwComparator {

    private static final float DISTANCE_SCALE = 20.0f;
    private static final float MIN_LENGTH_RATIO = 0.20f;    // было 0.30f
    private static final boolean DIAGNOSTIC_LOGS = false;   // true для отладки

    public static float compare(float[][] a, float[][] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) return 0f;

        int n = a.length;
        int m = b.length;
        int dim = Math.min(a[0].length, b[0].length);

        float lengthRatio = (float) Math.min(n, m) / Math.max(n, m);

        if (DIAGNOSTIC_LOGS) {
            System.out.println("[DTW] n=" + n + " m=" + m
                    + " ratio=" + String.format("%.3f", lengthRatio)
                    + " minRatio=" + MIN_LENGTH_RATIO);
        }

        if (lengthRatio < MIN_LENGTH_RATIO) return 0f;

        int band = Math.max(10, Math.abs(n - m) * 2);
        float INF = Float.MAX_VALUE / 2;

        float[][] dtw = new float[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) dtw[i][j] = INF;
        }
        dtw[0][0] = 0;

        for (int i = 1; i <= n; i++) {
            int jStart = Math.max(1, i - band);
            int jEnd = Math.min(m, i + band);
            for (int j = jStart; j <= jEnd; j++) {
                float cost = euclidean(a[i - 1], b[j - 1], dim);
                float minPrev = Math.min(
                        Math.min(dtw[i - 1][j], dtw[i][j - 1]),
                        dtw[i - 1][j - 1]);
                dtw[i][j] = cost + minPrev;
            }
        }

        float dist = dtw[n][m];
        float pathLen = Math.max(n, m);
        float avgDist = dist / pathLen;

        float similarity = 1.0f / (1.0f + avgDist / DISTANCE_SCALE);
        similarity *= (0.5f + 0.5f * lengthRatio);

        return Math.max(0f, Math.min(1f, similarity));
    }

    private static float euclidean(float[] a, float[] b, int dim) {
        float sum = 0;
        for (int i = 0; i < dim; i++) {
            float d = a[i] - b[i];
            sum += d * d;
        }
        return (float) Math.sqrt(sum);
    }
}