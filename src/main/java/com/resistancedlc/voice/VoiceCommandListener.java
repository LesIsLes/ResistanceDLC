package com.resistancedlc.voice;

import com.resistancedlc.AccordionScreen;
import com.resistancedlc.ConfigManager;
import com.resistancedlc.LocalizationManager;
import com.resistancedlc.ResistanceDLC;
import com.resistancedlc.config.ModConfig;
import com.resistancedlc.hud.HudEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.TargetDataLine;
import java.util.Map;

/**
 * VoiceCommandListener — финальный патч v3.0.0-alpha.4:
 *
 *  1. Listener работает ДАЖЕ при открытом GUI (кроме чата) — чтобы CLOSE_GUI работал.
 *  2. MASTER_GAIN на максимум (симметрия с Recorder'ом), лог один раз.
 *  3. VAD с ГИСТЕРЕЗИСОМ: старт речи — 50% порога, продолжение — 100%.
 *  4. PRE-ROLL: храним 5 последних кадров, при детекте речи — выливаем их в буфер.
 *  5. Логирование среднего RMS и пика по фразе.
 *  6. Отсечка тихих фраз (rms_avg < 30).
 *  7. Валидация калибровки: ambient < 3 или > 2000 → невалидна.
 */
public class VoiceCommandListener {

    private static final AudioFormat FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            MfccExtractor.SAMPLE_RATE,
            16, 1, 2,
            MfccExtractor.SAMPLE_RATE, false);

    private static final float BASE_VAD_THRESHOLD = 500f;
    private static final long SILENCE_END_MS = 400;
    private static final int MIN_SAMPLES = 6000;
    private static final int MAX_SAMPLES = 32000;    // 2 сек
    private static final long TRIGGER_COOLDOWN_MS = 2000;
    private static final long RESTART_COOLDOWN_MS = 500;

    // Гистерезис VAD
    private static final float START_RATIO = 0.5f;
    private static final float CONTINUE_RATIO = 1.0f;

    // Pre-roll: 5 кадров по ~100 мс = 500 мс
    private static final int PRE_ROLL_FRAMES = 5;

    private static final int CALIBRATION_FRAMES = 100;
    private static final float CALIBRATION_MULTIPLIER = 3.0f;
    private static final float MIN_CALIBRATED_THRESHOLD = 50f;
    private static final float MAX_CALIBRATED_THRESHOLD = 3000f;
    private static final float MIN_VALID_AMBIENT = 3f;
    private static final float MAX_VALID_AMBIENT = 2000f;

    // Отсечка тихих фраз
    private static final float MIN_VOICE_RMS = 30f;

    private static boolean calibrating = false;
    private static int calibrationCounter = 0;
    private static double calibrationSum = 0;

    private static Thread listenThread = null;
    private static volatile boolean running = false;
    private static volatile TargetDataLine line = null;

    private static boolean speaking = false;
    private static long lastVoiceTime = 0;
    private static final short[] buffer = new short[MAX_SAMPLES];
    private static int bufferPos = 0;

    // Pre-roll кольцевой буфер
    private static final short[][] preRoll = new short[PRE_ROLL_FRAMES][1600];
    private static final int[] preRollLen = new int[PRE_ROLL_FRAMES];
    private static int preRollIdx = 0;

    private static long lastTriggerTime = 0;
    private static long lastStopTime = 0;
    private static boolean startedOnce = false;
    private static boolean gainLoggedOnce = false;

    // ===== API =====

    public static void start() {
        if (VoiceCommandRecorder.isRecording()) return;
        if (running) return;

        long now = System.currentTimeMillis();
        if (now - lastStopTime < RESTART_COOLDOWN_MS) return;

        running = true;

        if (!startedOnce) {
            startedOnce = true;
            ResistanceDLC.LOGGER.info("[Voice] Listener STARTED. Threshold="
                    + ModConfig.voiceCommandThreshold
                    + ", commands=" + VoiceCommandManager.getAllMfcc().size()
                    + ", calibrated=" + String.format("%.0f", ModConfig.voiceVadCalibratedThreshold)
                    + ", sensitivity=" + String.format("%.2f", ModConfig.voiceVadSensitivity));
        }

        listenThread = new Thread(VoiceCommandListener::listenLoop, "VoiceCommandListener");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    public static void stop() {
        if (!running && line == null && listenThread == null) return;
        running = false;
        lastStopTime = System.currentTimeMillis();

        if (line != null) {
            try { line.stop(); line.close(); } catch (Throwable ignored) {}
            line = null;
        }
        if (listenThread != null) {
            try { listenThread.join(1000); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            listenThread = null;
        }
    }

    public static void startCalibration() {
        if (calibrating) return;
        calibrating = true;
        calibrationCounter = 0;
        calibrationSum = 0;
        ResistanceDLC.LOGGER.info("[Voice] Calibration started (10 sec, silence please)");
    }

    public static boolean isCalibrating() {
        return calibrating;
    }

    public static int getCalibrationProgress() {
        return Math.min(100, calibrationCounter * 100 / CALIBRATION_FRAMES);
    }

    // ===== Внутренняя логика =====

    private static void listenLoop() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                ResistanceDLC.LOGGER.warn("[Voice] Microphone not supported");
                running = false;
                return;
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(FORMAT);

            // ✅ MASTER_GAIN — лог только один раз
            try {
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainCtl = (FloatControl) line.getControl(
                            FloatControl.Type.MASTER_GAIN);
                    float maxGain = gainCtl.getMaximum();
                    gainCtl.setValue(maxGain);
                    if (!gainLoggedOnce) {
                        gainLoggedOnce = true;
                        ResistanceDLC.LOGGER.info("[Voice] Listener gain set to MAX: "
                                + String.format("%.1f", maxGain) + " dB");
                    }
                } else {
                    if (!gainLoggedOnce) {
                        gainLoggedOnce = true;
                        ResistanceDLC.LOGGER.info("[Voice] Listener MASTER_GAIN not supported, using default gain");
                    }
                }
            } catch (Throwable t) {
                if (!gainLoggedOnce) {
                    gainLoggedOnce = true;
                    ResistanceDLC.LOGGER.info("[Voice] Listener gain failed: " + t.getMessage());
                }
            }

            line.start();

            if (ModConfig.voiceVadCalibratedThreshold <= 0f) {
                startCalibration();
            }

            byte[] buf = new byte[2048];
            short[] frame = new short[1600];

            while (running) {
                Minecraft mc = Minecraft.getInstance();

                // ✅ НЕ слушаем только если нет игрока или открыт ЧАТ
                //    (при открытом GUI — слушаем, чтобы CLOSE_GUI работал)
                if (mc.player == null || mc.gui.screen() instanceof ChatScreen) {
                    Thread.sleep(50);
                    continue;
                }

                int read = line.read(buf, 0, buf.length);
                if (read <= 0) continue;

                int samples = Math.min(read / 2, frame.length);
                for (int i = 0; i < samples; i++) {
                    int lo = buf[i * 2] & 0xFF;
                    int hi = buf[i * 2 + 1];
                    frame[i] = (short) ((hi << 8) | lo);
                }

                processFrame(frame, samples);
            }
        } catch (Throwable t) {
            ResistanceDLC.LOGGER.error("[Voice] Listener error: " + t.getMessage());
        } finally {
            running = false;
        }
    }

    private static void processFrame(short[] frame, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (long) frame[i] * frame[i];
        }
        float rms = (float) Math.sqrt((double) sum / Math.max(1, length));

        // ===== Калибровка =====
        if (calibrating) {
            calibrationSum += rms;
            calibrationCounter++;
            if (calibrationCounter >= CALIBRATION_FRAMES) {
                double avg = calibrationSum / calibrationCounter;
                float threshold;
                if (avg < MIN_VALID_AMBIENT || avg > MAX_VALID_AMBIENT) {
                    threshold = BASE_VAD_THRESHOLD;
                    ResistanceDLC.LOGGER.warn("[Voice] Calibration FAILED: ambient="
                            + String.format("%.1f", avg)
                            + " out of range [" + MIN_VALID_AMBIENT + ".." + MAX_VALID_AMBIENT
                            + "]. Using default threshold=" + threshold);
                } else {
                    threshold = (float) Math.max(MIN_CALIBRATED_THRESHOLD,
                            Math.min(MAX_CALIBRATED_THRESHOLD, avg * CALIBRATION_MULTIPLIER));
                    ResistanceDLC.LOGGER.info("[Voice] Calibration done: ambient="
                            + String.format("%.1f", avg)
                            + " * " + CALIBRATION_MULTIPLIER
                            + " = threshold=" + String.format("%.0f", threshold));
                }
                ModConfig.voiceVadCalibratedThreshold = threshold;
                ConfigManager.save();
                calibrating = false;
            }
            return;
        }

        // ===== VAD с ГИСТЕРЕЗИСОМ =====
        float calibrated = ModConfig.voiceVadCalibratedThreshold > 0
                ? ModConfig.voiceVadCalibratedThreshold
                : BASE_VAD_THRESHOLD;
        float sensitivity = Math.max(0.3f, Math.min(3.0f, ModConfig.voiceVadSensitivity));
        float dynamicThreshold = calibrated / sensitivity;

        float startThreshold = dynamicThreshold * START_RATIO;
        float continueThreshold = dynamicThreshold * CONTINUE_RATIO;

        long now = System.currentTimeMillis();

        // Всегда пишем кадр в pre-roll
        int idx = preRollIdx % PRE_ROLL_FRAMES;
        System.arraycopy(frame, 0, preRoll[idx], 0, length);
        preRollLen[idx] = length;
        preRollIdx++;

        if (!speaking) {
            if (rms > startThreshold) {
                // ✅ PRE-ROLL: выливаем накопленные кадры в буфер
                speaking = true;
                bufferPos = 0;
                for (int p = 0; p < PRE_ROLL_FRAMES; p++) {
                    int preIdx = (preRollIdx - PRE_ROLL_FRAMES + p) % PRE_ROLL_FRAMES;
                    if (preIdx < 0) preIdx += PRE_ROLL_FRAMES;
                    int len = preRollLen[preIdx];
                    if (len <= 0) continue;
                    int toCopy = Math.min(len, MAX_SAMPLES - bufferPos);
                    if (toCopy <= 0) break;
                    System.arraycopy(preRoll[preIdx], 0, buffer, bufferPos, toCopy);
                    bufferPos += toCopy;
                }
                lastVoiceTime = now;
            } else {
                return;
            }
        } else {
            if (rms > continueThreshold) {
                lastVoiceTime = now;
            }
            int toCopy = Math.min(length, MAX_SAMPLES - bufferPos);
            if (toCopy > 0) {
                System.arraycopy(frame, 0, buffer, bufferPos, toCopy);
                bufferPos += toCopy;
            }

            if (bufferPos >= MAX_SAMPLES) {
                ResistanceDLC.LOGGER.info("[Voice] Buffer full (" + bufferPos
                        + " samples), forcing recognize");
                tryRecognize();
                return;
            }

            if (now - lastVoiceTime > SILENCE_END_MS) {
                tryRecognize();
            }
        }
    }

    private static void tryRecognize() {
        int savedBufferPos = bufferPos;

        if (bufferPos < MIN_SAMPLES) {
            if (bufferPos > 0) {
                ResistanceDLC.LOGGER.info("[Voice] Skip: too short ("
                        + bufferPos + " < " + MIN_SAMPLES + " samples)");
            }
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < TRIGGER_COOLDOWN_MS) {
            reset();
            return;
        }

        short[] samples = new short[bufferPos];
        System.arraycopy(buffer, 0, samples, 0, bufferPos);
        reset();

        // ✅ Средний RMS и пик по ВСЕЙ фразе
        long sqSum = 0;
        int peak = 0;
        for (short s : samples) {
            sqSum += (long) s * s;
            int abs = Math.abs(s);
            if (abs > peak) peak = abs;
        }
        float avgRms = (float) Math.sqrt((double) sqSum / samples.length);

        // ✅ Отсечка тихих фраз
        if (avgRms < MIN_VOICE_RMS) {
            ResistanceDLC.LOGGER.info("[Voice] Skip: too quiet (rms_avg="
                    + String.format("%.1f", avgRms)
                    + " peak=" + peak + " < " + MIN_VOICE_RMS + ")");
            return;
        }

        float[][] mfcc = MfccExtractor.extract(samples);
        if (mfcc == null || mfcc.length == 0) {
            ResistanceDLC.LOGGER.info("[Voice] MFCC extraction failed");
            return;
        }

        VoiceCommandAction best = null;
        float bestSim = 0f;

        for (Map.Entry<VoiceCommandAction, float[][]> e
                : VoiceCommandManager.getAllMfcc().entrySet()) {
            float sim = DtwComparator.compare(mfcc, e.getValue());
            if (sim > bestSim) {
                bestSim = sim;
                best = e.getKey();
            }
        }

        ResistanceDLC.LOGGER.info("[Voice] Best: "
                + (best == null ? "null" : best.name())
                + " sim=" + String.format("%.3f", bestSim)
                + " threshold=" + String.format("%.2f", ModConfig.voiceCommandThreshold)
                + " frames=" + mfcc.length
                + " samples=" + savedBufferPos
                + " rms_avg=" + String.format("%.1f", avgRms)
                + " peak=" + peak
                + (savedBufferPos >= MAX_SAMPLES ? " [TRIMMED]" : ""));

        if (best == null || bestSim < ModConfig.voiceCommandThreshold) {
            return;
        }

        lastTriggerTime = now;
        final VoiceCommandAction action = best;
        final float sim = bestSim;
        Minecraft.getInstance().execute(() -> applyAction(action, sim));
    }

    private static void reset() {
        bufferPos = 0;
        speaking = false;
        lastVoiceTime = 0;
    }

    private static void applyAction(VoiceCommandAction action, float sim) {
        Minecraft mc = Minecraft.getInstance();
        ResistanceDLC.LOGGER.info("[Voice] Triggered " + action.name()
                + " (sim=" + String.format("%.2f", sim) + ")");

        switch (action) {
            case OPEN_GUI -> {
                if (!(mc.gui.screen() instanceof AccordionScreen)) {
                    mc.gui.setScreen(new AccordionScreen());
                }
            }
            case HUD_EDITOR -> {
                if (!(mc.gui.screen() instanceof HudEditScreen)) {
                    mc.gui.setScreen(new HudEditScreen());
                }
            }
            case CHANGE_LANG -> {
                ModConfig.modLogoRussian = !ModConfig.modLogoRussian;
                ConfigManager.save();
                LocalizationManager.reload();

                // ✅ Если открыт AccordionScreen — пересоздаём,
                //    чтобы все строки обновились сразу.
                if (mc.gui.screen() instanceof AccordionScreen oldAccordion) {
                    oldAccordion.saveExpanded();
                    mc.gui.setScreen(new AccordionScreen());
                    ResistanceDLC.LOGGER.info("[Voice] Rebuilt AccordionScreen after language change");
                }
            }
            case CLOSE_GUI -> {
                if (mc.gui.screen() != null) {
                    ResistanceDLC.LOGGER.info("[Voice] Closing screen: "
                            + mc.gui.screen().getClass().getSimpleName());
                    mc.gui.setScreen(null);
                } else {
                    ResistanceDLC.LOGGER.info("[Voice] CLOSE_GUI: no screen to close");
                }
            }
        }
    }

    public static void resetState() {
        startedOnce = false;
        gainLoggedOnce = false;
        reset();
    }
}