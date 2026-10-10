package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/**
 * MusicPlayerManager — центральная логика MusicPlayer.
 *
 * Отвечает за:
 *   - Сканирование папки music/ и построение плейлиста
 *   - Воспроизведение через OggPlayback
 *   - Play/Pause/Next/Prev/Stop
 *   - Repeat (off / one / all), Shuffle
 *   - Громкость
 *   - Авто-переход к следующему треку
 *   - Авто-пропуск битых файлов
 *   - Длительность трека (полное чтение PCM-потока, кэш)
 *
 * ВАЖНО: этот класс не рисует HUD и не обрабатывает клики — этим занимаются
 * MusicPlayerHud и MusicPlayerHudMixin соответственно.
 */
public class MusicPlayerManager {

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ONE = 1;
    public static final int REPEAT_ALL = 2;

    private static final Path MUSIC_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("resistancedlc").resolve("music");

    private static final List<MusicTrack> playlist = new ArrayList<>();
    private static int currentIndex = -1;

    private static OggPlayback playback = new OggPlayback();
    private static final Random RANDOM = new Random();

    private static boolean initialized = false;

    /** Кэш длительностей треков (в секундах). -1 = не удалось распарсить. */
    private static final Map<Path, Integer> durationCache = new HashMap<>();

    /** Треки, для которых уже пытались парсить длительность (чтобы не повторять). */
    private static final Set<Path> durationParsed = new HashSet<>();

    // ===================== ИНИЦИАЛИЗАЦИЯ =====================

    public static void init() {
        ensureDirExists();
        rescan();
        initialized = true;
    }

    private static void ensureDirExists() {
        try {
            if (!Files.exists(MUSIC_DIR)) {
                Files.createDirectories(MUSIC_DIR);
            }
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("MusicPlayer: cannot create music dir: " + e.getMessage());
        }
    }

    public static void rescan() {
        playlist.clear();
        ensureDirExists();

        if (!Files.exists(MUSIC_DIR)) {
            currentIndex = -1;
            return;
        }

        try (Stream<Path> stream = Files.list(MUSIC_DIR)) {
            stream.filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ogg"))
                    .sorted()
                    .forEach(p -> playlist.add(MusicTrack.fromPath(p)));
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("MusicPlayer: scan failed: " + e.getMessage());
        }

        if (currentIndex >= playlist.size()) {
            currentIndex = -1;
        }

        // НЕ чистим durationCache и durationParsed при rescan — файлы те же.
        // Если нужно — очисти вручную через clearDurationCache().
    }

    public static void reset() {
        stop();
        currentIndex = -1;
        playlist.clear();
        durationCache.clear();
        durationParsed.clear();
        initialized = false;
    }

    public static void clearDurationCache() {
        durationCache.clear();
        durationParsed.clear();
    }

    // ===================== ТИК =====================

    public static void tick() {
        if (!ModConfig.musicPlayerEnabled && playback.isRunning()) {
            playback.stop();
        }
    }

    // ===================== УПРАВЛЕНИЕ =====================

    public static void play() {
        if (!ModConfig.musicPlayerEnabled) return;

        if (!initialized) init();
        if (playlist.isEmpty()) {
            sendChatMessage("§c[Music] Плейлист пуст. Добавь .ogg в папку.");
            return;
        }

        if (currentIndex < 0) {
            currentIndex = 0;
        }

        if (playback.isPaused()) {
            if (playback.resume()) {
                applyVolume();
                return;
            }
        }

        playIndex(currentIndex);
    }

    public static void pause() {
        playback.pause();
    }

    public static void togglePause() {
        if (playback.isRunning()) {
            playback.pause();
        } else {
            play();
        }
    }

    public static void stop() {
        playback.stop();
    }

    public static void next() {
        if (playlist.isEmpty()) return;
        if (ModConfig.musicShuffle) {
            currentIndex = getRandomIndex();
        } else {
            currentIndex = (currentIndex + 1) % playlist.size();
        }
        playIndex(currentIndex);
    }

    public static void prev() {
        if (playlist.isEmpty()) return;
        if (ModConfig.musicShuffle) {
            currentIndex = getRandomIndex();
        } else {
            currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        }
        playIndex(currentIndex);
    }

    public static void playTrackAt(int index) {
        if (playlist.isEmpty()) return;
        if (index < 0 || index >= playlist.size()) return;
        playIndex(index);
    }

    private static void playIndex(int index) {
        com.resistancedlc.lyrics.LyricsRenderer.reset();
        if (playlist.isEmpty()) return;
        if (index < 0 || index >= playlist.size()) return;

        currentIndex = index;
        MusicTrack track = playlist.get(index);
        File file = track.path().toFile();

        if (!file.exists()) {
            ResistanceDLC.LOGGER.warn("MusicPlayer: file missing: " + file);
            skipBroken();
            return;
        }

        playback.stop();
        playback = new OggPlayback();
        applyVolume();

        boolean ok = playback.play(file, MusicPlayerManager::onTrackFinished);
        if (!ok) {
            ResistanceDLC.LOGGER.warn("MusicPlayer: cannot play " + file.getName() + ", skipping");
            skipBroken();
        }
    }

    private static void skipBroken() {
        if (playlist.isEmpty()) return;
        int attempts = 0;
        int originalIndex = currentIndex;
        while (attempts < playlist.size()) {
            currentIndex = (currentIndex + 1) % playlist.size();
            if (currentIndex == originalIndex) break;
            attempts++;

            File f = playlist.get(currentIndex).path().toFile();
            if (!f.exists()) continue;

            playback = new OggPlayback();
            applyVolume();
            if (playback.play(f, MusicPlayerManager::onTrackFinished)) {
                return;
            }
        }
        currentIndex = -1;
        sendChatMessage("§c[Music] Не удалось воспроизвести ни один трек.");
    }

    private static void onTrackFinished() {
        if (playlist.isEmpty()) return;
        if (!ModConfig.musicPlayerEnabled) return;

        int mode = ModConfig.musicRepeat;

        if (mode == REPEAT_ONE) {
            playIndex(currentIndex);
            return;
        }

        boolean lastTrack = (currentIndex == playlist.size() - 1);

        if (lastTrack && mode == REPEAT_OFF) {
            return;
        }

        next();
    }

    // ===================== НАСТРОЙКИ =====================

    public static void setVolume(float v) {
        ModConfig.musicVolume = Math.max(0.0f, Math.min(1.0f, v));
        applyVolume();
        ConfigManager.save();
    }

    private static void applyVolume() {
        playback.setVolume(ModConfig.musicVolume);
    }

    public static void cycleRepeat() {
        int mode = (ModConfig.musicRepeat + 1) % 3;
        ModConfig.musicRepeat = mode;
        ConfigManager.save();
    }

    public static void toggleShuffle() {
        ModConfig.musicShuffle = !ModConfig.musicShuffle;
        ConfigManager.save();
    }

    // ===================== ГЕТТЕРЫ =====================

    public static List<MusicTrack> getPlaylist() {
        if (!initialized) init();
        return Collections.unmodifiableList(playlist);
    }

    public static int getCurrentIndex() {
        return currentIndex;
    }

    public static MusicTrack getCurrentTrack() {
        if (currentIndex < 0 || currentIndex >= playlist.size()) return null;
        return playlist.get(currentIndex);
    }

    public static boolean isPlaying() {
        return playback.isRunning();
    }

    public static boolean isPaused() {
        return playback.isPaused();
    }

    public static boolean hasTrack() {
        return currentIndex >= 0 && currentIndex < playlist.size();
    }

    public static float getProgress() {
        return playback.getProgress();
    }

    /**
     * Текущая позиция в секундах (из positionBytes в OggPlayback).
     */
    public static int getPositionSeconds() {
        return playback.getPositionSeconds();
    }

    /**
     * Длительность текущего трека в секундах.
     *
     * OGG (Vorbis) не хранит длину в заголовке как количество фреймов —
     * getFrameLength() возвращает -1. Поэтому читаем PCM-поток ДО КОНЦА
     * и считаем: totalBytes / frameSize / frameRate.
     *
     * ВАЖНО:
     *   - Результат кэшируется, в т.ч. НЕГАТИВНЫЙ (-1), чтобы не парсить повторно.
     *   - Не вызывается в горячем цикле — только при смене трека / в HUD.
     */
    public static int getDurationSeconds() {
        MusicTrack t = getCurrentTrack();
        if (t == null) return 0;

        Path path = t.path();

        // Уже парсили? Возвращаем кэш (в т.ч. -1).
        if (durationParsed.contains(path)) {
            Integer cached = durationCache.get(path);
            return (cached == null || cached < 0) ? 0 : cached;
        }

        // Помечаем ДО парсинга — чтобы рекурсивные вызовы не зациклились.
        durationParsed.add(path);

        int result = parseDuration(path);
        durationCache.put(path, result);

        if (result > 0) {
            ResistanceDLC.LOGGER.info("[MusicPlayer] Duration for "
                    + path.getFileName() + " = " + result + " sec");
        } else {
            ResistanceDLC.LOGGER.warn("[MusicPlayer] Cannot determine duration for "
                    + path.getFileName() + " (will not retry)");
        }

        return result < 0 ? 0 : result;
    }

    /**
     * Читает PCM-поток OGG до конца и считает длительность.
     * @return секунды (>0) или -1 при ошибке.
     */
    private static int parseDuration(Path path) {
        javax.sound.sampled.AudioInputStream raw = null;
        try {
            Class<?> readerClass = Class.forName(
                    "com.github.trilarion.sound.vorbis.sampled.spi.VorbisAudioFileReader"
            );
            Object reader = readerClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method getStreamMethod = readerClass.getMethod(
                    "getAudioInputStream", java.io.File.class
            );
            raw = (javax.sound.sampled.AudioInputStream) getStreamMethod.invoke(
                    reader, path.toFile());

            javax.sound.sampled.AudioFormat fmt = raw.getFormat();
            float frameRate = fmt.getFrameRate();
            int frameSize = fmt.getFrameSize();
            if (frameRate <= 0 || frameSize <= 0) {
                ResistanceDLC.LOGGER.warn("[MusicPlayer] invalid format: frameRate="
                        + frameRate + ", frameSize=" + frameSize);
                return -1;
            }

            byte[] buf = new byte[8192];
            long totalBytes = 0;
            int read;
            while ((read = raw.read(buf)) > 0) {
                totalBytes += read;
            }

            if (totalBytes <= 0) {
                ResistanceDLC.LOGGER.warn("[MusicPlayer] 0 bytes read");
                return -1;
            }

            long frames = totalBytes / frameSize;
            int sec = (int) (frames / frameRate);
            ResistanceDLC.LOGGER.info("[MusicPlayer] Duration for " + path.getFileName()
                    + " = " + sec + " sec");
            return sec;
        } catch (Throwable e) {
            ResistanceDLC.LOGGER.error("[MusicPlayer] parseDuration failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return -1;
        } finally {
            try { if (raw != null) raw.close(); } catch (Exception ignored) {}
        }
    }

    // ===================== ВСПОМОГАТЕЛЬНОЕ =====================

    private static int getRandomIndex() {
        if (playlist.size() <= 1) return 0;
        int idx;
        int guard = 0;
        do {
            idx = RANDOM.nextInt(playlist.size());
            guard++;
        } while (idx == currentIndex && guard < 10);
        return idx;
    }

    private static void sendChatMessage(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), false);
        }
    }

    public static String formatTime(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    public static void openMusicFolder() {
        ensureDirExists();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String path = MUSIC_DIR.toAbsolutePath().toString();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer.exe", path);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", path);
            } else {
                pb = new ProcessBuilder("xdg-open", path);
            }
            pb.start();
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("MusicPlayer: cannot open folder: " + e.getMessage());
        }
    }

    public static String getMusicDirPath() {
        return MUSIC_DIR.toAbsolutePath().toString();
    }
    /**
     * Имя текущего трека без расширения — для поиска .lrc.
     */
    public static String getCurrentTrackName() {
        MusicTrack t = getCurrentTrack();
        if (t == null) return null;
        String fileName = t.path().getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * Текущая позиция трека в миллисекундах.
     * OggPlayback отдаёт секунды — конвертим.
     */
    public static long getCurrentPositionMs() {
        return (long) playback.getPositionSeconds() * 1000L;
    }
}