package com.resistancedlc.voice;

import com.resistancedlc.ResistanceDLC;
import net.fabricmc.loader.api.FabricLoader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VoiceCommandManager — управление голосовыми командами.
 *
 *   - Папка: config/resistancedlc/voice/
 *   - Файлы: <action_id>.wav (16kHz, mono, 16-bit PCM)
 *   - Кэш MFCC: Map<Action, float[][]>
 */
public class VoiceCommandManager {

    private static final Path VOICE_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("resistancedlc").resolve("voice");

    /** Загруженные команды. */
    private static final List<VoiceCommandEntry> commands = new ArrayList<>();

    /** Кэш MFCC: action → float[frames][13]. */
    private static final Map<VoiceCommandAction, float[][]> mfccCache = new HashMap<>();

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        ensureDirExists();
        loadAll();
    }

    private static void ensureDirExists() {
        try {
            if (!Files.exists(VOICE_DIR)) {
                Files.createDirectories(VOICE_DIR);
            }
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("[Voice] Cannot create voice dir: " + e.getMessage());
        }
    }

    /**
     * Загружает все .wav для известных actions.
     * Если файл отсутствует — команда не добавляется, но action остаётся доступным для записи.
     */
    public static void loadAll() {
        commands.clear();
        mfccCache.clear();

        for (VoiceCommandAction action : VoiceCommandAction.values()) {
            Path wav = VOICE_DIR.resolve(action.getId() + ".wav");
            VoiceCommandEntry entry = new VoiceCommandEntry(action, wav);
            commands.add(entry);

            if (Files.exists(wav)) {
                float[][] mfcc = loadMfccFromWav(wav);
                if (mfcc != null) {
                    entry.setMfccCache(mfcc);
                    mfccCache.put(action, mfcc);
                } else {
                    ResistanceDLC.LOGGER.warn("[Voice] Failed to compute MFCC for " + action.getId());
                }
            }
        }
    }

    /**
     * Загрузка MFCC для одного action (после записи).
     */
    public static void reload(VoiceCommandAction action) {
        Path wav = VOICE_DIR.resolve(action.getId() + ".wav");
        if (!Files.exists(wav)) return;

        float[][] mfcc = loadMfccFromWav(wav);
        if (mfcc == null) {
            ResistanceDLC.LOGGER.warn("[Voice] Failed to reload MFCC for " + action.getId());
            return;
        }

        mfccCache.put(action, mfcc);
        for (VoiceCommandEntry e : commands) {
            if (e.action == action) {
                e.setMfccCache(mfcc);
                break;
            }
        }
        ResistanceDLC.LOGGER.info("[Voice] Reloaded " + action.getId()
                + " (" + mfcc.length + " frames)");
    }

    /**
     * Считывает WAV и вычисляет MFCC.
     */
    private static float[][] loadMfccFromWav(Path wav) {
        try {
            short[] pcm = readWavPcm(wav.toFile());
            if (pcm == null || pcm.length == 0) return null;
            return MfccExtractor.extract(pcm);
        } catch (Throwable t) {
            ResistanceDLC.LOGGER.error("[Voice] MFCC error: " + t.getMessage());
            return null;
        }
    }

    /**
     * Читает WAV-файл и возвращает PCM samples (16-bit signed, mono).
     * Если формат неверный — пытается сконвертировать.
     */
    public static short[] readWavPcm(java.io.File file) {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
            AudioFormat srcFormat = in.getFormat();
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    MfccExtractor.SAMPLE_RATE,
                    16,
                    1,
                    2,
                    MfccExtractor.SAMPLE_RATE,
                    false
            );

            AudioInputStream pcmStream;
            if (!srcFormat.matches(targetFormat)) {
                if (!AudioSystem.isConversionSupported(targetFormat, srcFormat)) {
                    ResistanceDLC.LOGGER.warn("[Voice] Unsupported format: " + srcFormat);
                    return null;
                }
                pcmStream = AudioSystem.getAudioInputStream(targetFormat, in);
            } else {
                pcmStream = in;
            }

            byte[] bytes = pcmStream.readAllBytes();
            pcmStream.close();

            short[] samples = new short[bytes.length / 2];
            for (int i = 0; i < samples.length; i++) {
                int lo = bytes[i * 2] & 0xFF;
                int hi = bytes[i * 2 + 1];
                samples[i] = (short) ((hi << 8) | lo);
            }

            return samples;
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[Voice] WAV read error: " + e.getMessage());
            return null;
        }
    }

    // ===================== Геттеры =====================

    public static List<VoiceCommandEntry> getCommands() {
        return commands;
    }

    public static Path getVoiceDir() {
        return VOICE_DIR;
    }

    public static String getVoiceDirPath() {
        return VOICE_DIR.toAbsolutePath().toString();
    }

    public static boolean hasCommand(VoiceCommandAction action) {
        return mfccCache.containsKey(action);
    }

    public static float[][] getMfcc(VoiceCommandAction action) {
        return mfccCache.get(action);
    }

    public static Map<VoiceCommandAction, float[][]> getAllMfcc() {
        return mfccCache;
    }

    public static void reset() {
        initialized = false;
        commands.clear();
        mfccCache.clear();
    }
}