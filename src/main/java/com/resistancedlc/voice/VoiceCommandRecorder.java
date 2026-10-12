package com.resistancedlc.voice;

import com.resistancedlc.ResistanceDLC;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * VoiceCommandRecorder — запись с микрофона (16 kHz, mono, 16-bit PCM).
 *
 * Фичи:
 *   - Отдельный поток (не лагает Minecraft).
 *   - Gain-фикс: MASTER_GAIN / VOLUME на максимум.
 *   - Нормализация после записи (пик → 24000).
 *   - Флаг recording выставляется ДО старта потока (thread-safe).
 */
public class VoiceCommandRecorder {

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            MfccExtractor.SAMPLE_RATE,
            16, 1, 2,
            MfccExtractor.SAMPLE_RATE, false);

    private static final int TARGET_PEAK = 24000;

    private static volatile boolean recording = false;
    private static volatile Thread recordThread = null;
    private static volatile TargetDataLine currentLine = null;

    private static final ByteArrayOutputStream capturedBytes = new ByteArrayOutputStream();

    public static boolean isRecording() {
        return recording;
    }

    public static float getCurrentAmplitude() {
        if (!recording) return 0f;
        byte[] snapshot;
        synchronized (capturedBytes) {
            snapshot = capturedBytes.toByteArray();
        }
        if (snapshot.length < 100) return 0f;

        int from = Math.max(0, snapshot.length - 3200);
        long sum = 0;
        int count = 0;
        for (int i = from; i + 1 < snapshot.length; i += 2) {
            int lo = snapshot[i] & 0xFF;
            int hi = snapshot[i + 1];
            short s = (short) ((hi << 8) | lo);
            sum += (long) s * s;
            count++;
        }
        if (count == 0) return 0f;
        double rms = Math.sqrt((double) sum / count);
        return (float) Math.min(1.0, rms / 8000.0);
    }

    public static float[] getVisualizerBars(int bars) {
        float[] result = new float[bars];
        if (!recording) return result;

        byte[] snapshot;
        synchronized (capturedBytes) {
            snapshot = capturedBytes.toByteArray();
        }
        int totalSamples = snapshot.length / 2;
        if (totalSamples < bars) return result;

        int chunk = Math.min(totalSamples, 3200);
        int startSample = totalSamples - chunk;
        int barsSize = chunk / bars;
        if (barsSize <= 0) return result;

        for (int b = 0; b < bars; b++) {
            long sum = 0;
            int count = 0;
            for (int i = 0; i < barsSize; i++) {
                int idx = (startSample + b * barsSize + i) * 2;
                if (idx + 1 >= snapshot.length) break;
                int lo = snapshot[idx] & 0xFF;
                int hi = snapshot[idx + 1];
                short s = (short) ((hi << 8) | lo);
                sum += (long) s * s;
                count++;
            }
            double rms = count > 0 ? Math.sqrt((double) sum / count) : 0;
            result[b] = (float) Math.min(1.0, rms / 8000.0);
        }
        return result;
    }

    public static void start() {
        if (recording) return;

        // ✅ Флаг — ДО старта потока (thread-safe)
        recording = true;
        synchronized (capturedBytes) {
            capturedBytes.reset();
        }

        recordThread = new Thread(() -> {
            TargetDataLine line = null;
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
                if (!AudioSystem.isLineSupported(info)) {
                    recording = false;
                    ResistanceDLC.LOGGER.error("[Voice] Microphone not supported");
                    return;
                }

                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(FORMAT);

                // ⚠️ MASTER_GAIN на максимум — ЛОМАЕТ RMS-калибровку.
                // Ставим +6 dB (компромисс), либо дефолт если контрол не поддерживается.
                try {
                    if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl gainCtl = (FloatControl) line.getControl(
                                FloatControl.Type.MASTER_GAIN);
                        // Ограничиваем: не выше +6 dB от дефолта
                        float defGain = 0f;
                        float maxGain = gainCtl.getMaximum();
                        float target = Math.min(6f, maxGain);
                        if (target < gainCtl.getMinimum()) target = gainCtl.getMinimum();
                        gainCtl.setValue(target);
                        ResistanceDLC.LOGGER.info("[Voice] Recorder gain set to "
                                + String.format("%.1f", target) + " dB");
                    }
                } catch (Throwable ignored) {}

                line.start();
                currentLine = line;

                byte[] buf = new byte[1024];
                while (recording) {
                    int read = line.read(buf, 0, buf.length);
                    if (read > 0) {
                        synchronized (capturedBytes) {
                            capturedBytes.write(buf, 0, read);
                        }
                    }
                }
            } catch (Throwable t) {
                ResistanceDLC.LOGGER.error("[Voice] Record error: " + t.getMessage());
                recording = false;
            } finally {
                if (line != null) {
                    try { line.stop(); line.close(); } catch (Throwable ignored) {}
                }
                currentLine = null;
            }
        }, "VoiceCommandRecorder");

        recordThread.setDaemon(true);
        recordThread.start();
    }

    public static short[] stop() {
        if (!recording) return null;
        recording = false;

        if (recordThread != null) {
            try { recordThread.join(1000); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            recordThread = null;
        }

        byte[] bytes;
        synchronized (capturedBytes) {
            bytes = capturedBytes.toByteArray();
            capturedBytes.reset();
        }

        short[] samples = new short[bytes.length / 2];
        for (int i = 0; i < samples.length; i++) {
            int lo = bytes[i * 2] & 0xFF;
            int hi = bytes[i * 2 + 1];
            samples[i] = (short) ((hi << 8) | lo);
        }

        samples = normalize(samples, TARGET_PEAK);
        return samples;
    }

    public static short[] normalize(short[] samples, int targetPeak) {
        if (samples == null || samples.length == 0) return samples;
        int peak = 0;
        for (short s : samples) {
            int abs = Math.abs(s);
            if (abs > peak) peak = abs;
        }
        if (peak == 0 || peak >= targetPeak) return samples;
        float gain = (float) targetPeak / peak;
        short[] out = new short[samples.length];
        for (int i = 0; i < samples.length; i++) {
            int v = Math.round(samples[i] * gain);
            if (v > 32767) v = 32767;
            if (v < -32768) v = -32768;
            out[i] = (short) v;
        }
        return out;
    }

    public static boolean saveWav(short[] samples, File outFile) {
        if (samples == null || samples.length == 0) return false;
        try {
            byte[] bytes = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) {
                bytes[i * 2] = (byte) (samples[i] & 0xFF);
                bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xFF);
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 AudioInputStream ais = new AudioInputStream(bais, FORMAT, samples.length)) {
                if (outFile.getParentFile() != null) outFile.getParentFile().mkdirs();
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outFile);
            }
            return true;
        } catch (Throwable t) {
            ResistanceDLC.LOGGER.error("[Voice] Save WAV error: " + t.getMessage());
            return false;
        }
    }
}