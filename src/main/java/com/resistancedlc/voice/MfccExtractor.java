package com.resistancedlc.voice;

/**
 * MfccExtractor — вычисление MFCC + DELTA.
 *
 * v3.0.0-alpha.4 — DELTA-КОЭФФИЦИЕНТЫ:
 *   - 13 статических MFCC + 13 delta = 26 признаков.
 *   - Delta = производная по времени, ловит динамику речи.
 *   - Формула HTK: d[t] = sum_{n=1..N} n*(c[t+n]-c[t-n]) / (2*sum n^2)
 *   - Значительно повышает точность DTW (sim 0.35 → 0.60+).
 *
 * ⚠️ После применения — ПЕРЕЗАПИСАТЬ все эталоны (.wav)!
 *    Старые (13D) несовместимы с новыми (26D).
 */
public class MfccExtractor {

    public static final int SAMPLE_RATE = 16000;
    public static final int FRAME_SIZE = 400;
    public static final int FRAME_STEP = 160;
    public static final int FFT_SIZE = 512;
    public static final int MEL_FILTERS = 26;
    public static final int NUM_CEPS = 13;
    public static final int NUM_CEPS_WITH_DELTA = 26;

    /**
     * Извлекает MFCC + DELTA из массива PCM.
     *
     * @param pcmSamples массив samples [-32768..32767]
     * @return float[frames][26] (13 MFCC + 13 delta), или null
     */
    public static float[][] extract(short[] pcmSamples) {
        if (pcmSamples == null || pcmSamples.length < FRAME_SIZE) {
            return null;
        }

        // 1) Normalize + pre-emphasis
        float[] signal = new float[pcmSamples.length];
        float preEmphasis = 0.97f;
        signal[0] = pcmSamples[0] / 32768.0f;
        for (int i = 1; i < pcmSamples.length; i++) {
            float s = pcmSamples[i] / 32768.0f;
            signal[i] = s - preEmphasis * (pcmSamples[i - 1] / 32768.0f);
        }

        // 2) Разбиваем на фреймы
        int numFrames = 1 + (signal.length - FRAME_SIZE) / FRAME_STEP;
        if (numFrames <= 0) return null;

        // 3) Окно Хэмминга
        float[] hamming = new float[FRAME_SIZE];
        for (int i = 0; i < FRAME_SIZE; i++) {
            hamming[i] = 0.54f - 0.46f * (float) Math.cos(2.0 * Math.PI * i / (FRAME_SIZE - 1));
        }

        float[][] melFilters = buildMelFilterbank();
        float[][] dctMatrix = buildDctMatrix();

        // 4) Обработка кадров
        float[][] mfcc = new float[numFrames][NUM_CEPS];
        float[] real = new float[FFT_SIZE];
        float[] imag = new float[FFT_SIZE];

        for (int f = 0; f < numFrames; f++) {
            int start = f * FRAME_STEP;

            java.util.Arrays.fill(real, 0);
            java.util.Arrays.fill(imag, 0);
            for (int i = 0; i < FRAME_SIZE; i++) {
                real[i] = signal[start + i] * hamming[i];
            }

            fft(real, imag);

            float[] powerSpec = new float[FFT_SIZE / 2 + 1];
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                powerSpec[i] = real[i] * real[i] + imag[i] * imag[i];
            }

            float[] melEnergies = new float[MEL_FILTERS];
            for (int m = 0; m < MEL_FILTERS; m++) {
                float sum = 0;
                for (int k = 0; k <= FFT_SIZE / 2; k++) {
                    sum += melFilters[m][k] * powerSpec[k];
                }
                melEnergies[m] = sum > 0 ? (float) Math.log(sum) : -50f;
            }

            for (int c = 0; c < NUM_CEPS; c++) {
                float sum = 0;
                for (int m = 0; m < MEL_FILTERS; m++) {
                    sum += dctMatrix[c][m] * melEnergies[m];
                }
                mfcc[f][c] = (c == 0) ? 0f : sum;
            }
        }

        // 5) CMN
        for (int c = 0; c < NUM_CEPS; c++) {
            float mean = 0;
            for (int f = 0; f < numFrames; f++) mean += mfcc[f][c];
            mean /= numFrames;
            for (int f = 0; f < numFrames; f++) mfcc[f][c] -= mean;
        }

        // 6) DELTA
        return computeDeltas(mfcc, numFrames);
    }

    /**
     * Delta-коэффициенты (HTK).
     */
    private static float[][] computeDeltas(float[][] mfcc, int numFrames) {
        float[][] result = new float[numFrames][NUM_CEPS_WITH_DELTA];
        final int N = 2;

        for (int f = 0; f < numFrames; f++) {
            System.arraycopy(mfcc[f], 0, result[f], 0, NUM_CEPS);

            for (int c = 0; c < NUM_CEPS; c++) {
                float sum = 0;
                float denom = 0;
                for (int n = 1; n <= N; n++) {
                    int fPlus = Math.min(numFrames - 1, f + n);
                    int fMinus = Math.max(0, f - n);
                    sum += n * (mfcc[fPlus][c] - mfcc[fMinus][c]);
                    denom += 2 * n * n;
                }
                result[f][NUM_CEPS + c] = denom > 0 ? sum / denom : 0f;
            }
        }
        return result;
    }

    // ===================== FFT =====================
    private static void fft(float[] real, float[] imag) {
        int n = real.length;
        if (n != (n & -n)) {
            throw new IllegalArgumentException("FFT size must be power of 2");
        }

        int j = 0;
        for (int i = 0; i < n - 1; i++) {
            if (i < j) {
                float tr = real[i]; real[i] = real[j]; real[j] = tr;
                float ti = imag[i]; imag[i] = imag[j]; imag[j] = ti;
            }
            int m = n >> 1;
            while (m >= 1 && j >= m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }

        for (int size = 2; size <= n; size <<= 1) {
            int halfSize = size >> 1;
            float tabReal = (float) Math.cos(-2.0 * Math.PI / size);
            float tabImag = (float) Math.sin(-2.0 * Math.PI / size);
            for (int i = 0; i < n; i += size) {
                float wReal = 1f, wImag = 0f;
                for (int k = 0; k < halfSize; k++) {
                    int idx1 = i + k;
                    int idx2 = i + k + halfSize;
                    float xReal = real[idx2] * wReal - imag[idx2] * wImag;
                    float xImag = real[idx2] * wImag + imag[idx2] * wReal;
                    real[idx2] = real[idx1] - xReal;
                    imag[idx2] = imag[idx1] - xImag;
                    real[idx1] += xReal;
                    imag[idx1] += xImag;
                    float newWReal = wReal * tabReal - wImag * tabImag;
                    float newWImag = wReal * tabImag + wImag * tabReal;
                    wReal = newWReal;
                    wImag = newWImag;
                }
            }
        }
    }

    // ===================== Mel filterbank =====================
    private static float[][] buildMelFilterbank() {
        float lowFreqMel = hzToMel(20);
        float highFreqMel = hzToMel(SAMPLE_RATE / 2.0f);
        float[] melPoints = new float[MEL_FILTERS + 2];
        for (int i = 0; i < melPoints.length; i++) {
            melPoints[i] = lowFreqMel + (highFreqMel - lowFreqMel) * i / (MEL_FILTERS + 1);
        }

        int[] binPoints = new int[melPoints.length];
        for (int i = 0; i < melPoints.length; i++) {
            float hz = melToHz(melPoints[i]);
            binPoints[i] = (int) Math.floor((FFT_SIZE + 1) * hz / SAMPLE_RATE);
        }

        float[][] filters = new float[MEL_FILTERS][FFT_SIZE / 2 + 1];
        for (int m = 1; m <= MEL_FILTERS; m++) {
            int left = binPoints[m - 1];
            int center = binPoints[m];
            int right = binPoints[m + 1];
            for (int k = left; k < center; k++) {
                if (k >= 0 && k < filters[m - 1].length && center > left) {
                    filters[m - 1][k] = (float) (k - left) / (center - left);
                }
            }
            for (int k = center; k < right; k++) {
                if (k >= 0 && k < filters[m - 1].length && right > center) {
                    filters[m - 1][k] = (float) (right - k) / (right - center);
                }
            }
        }
        return filters;
    }

    private static float hzToMel(float hz) {
        return (float) (2595.0 * Math.log10(1.0 + hz / 700.0));
    }

    private static float melToHz(float mel) {
        return (float) (700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0));
    }

    // ===================== DCT =====================
    private static float[][] buildDctMatrix() {
        float[][] dct = new float[NUM_CEPS][MEL_FILTERS];
        for (int c = 0; c < NUM_CEPS; c++) {
            for (int m = 0; m < MEL_FILTERS; m++) {
                dct[c][m] = (float) Math.cos(Math.PI * c * (m + 0.5) / MEL_FILTERS);
            }
        }
        return dct;
    }
}