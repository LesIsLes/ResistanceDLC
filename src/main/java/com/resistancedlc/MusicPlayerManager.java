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
import java.util.List;
import java.util.Map;
import java.util.Random;
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

    /** Кэш длительностей треков (в секундах). */
    private static final Map<Path, Integer> durationCache = new HashMap<>();

    // ===================== ИНИЦИАЛИЗАЦИЯ =====================

    /**
     * Загрузить плейлист (при первом обращении или принудительно).
     */
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

    /**
     * Сканирует папку music/ и строит плейлист.
     * Формат: только .ogg, без рекурсии, сортировка по имени файла.
     */
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

        // Чистим кэш длительностей
        durationCache.clear();
    }

    /**
     * Полная очистка при выходе из мира.
     */
    public static void reset() {
        stop();
        currentIndex = -1;
        playlist.clear();
        durationCache.clear();
        initialized = false;
    }

    // ===================== УПРАВЛЕНИЕ =====================

    /**
     * Воспроизведение. Если currentIndex == -1, начинаем с 0.
     * Если на паузе — возобновляем.
     */
    public static void play() {
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

    /**
     * Пауза.
     */
    public static void pause() {
        playback.pause();
    }

    /**
     * Переключить пауза/плей.
     */
    public static void togglePause() {
        if (playback.isRunning()) {
            playback.pause();
        } else {
            play();
        }
    }

    /**
     * Стоп.
     */
    public static void stop() {
        playback.stop();
    }

    /**
     * Следующий трек.
     */
    public static void next() {
        if (playlist.isEmpty()) return;
        if (ModConfig.musicShuffle) {
            currentIndex = getRandomIndex();
        } else {
            currentIndex = (currentIndex + 1) % playlist.size();
        }
        playIndex(currentIndex);
    }

    /**
     * Предыдущий трек.
     */
    public static void prev() {
        if (playlist.isEmpty()) return;
        if (ModConfig.musicShuffle) {
            currentIndex = getRandomIndex();
        } else {
            currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        }
        playIndex(currentIndex);
    }

    /**
     * Публичный метод для прямого перехода к треку по индексу.
     * Используется в GUI при клике на трек в списке.
     */
    public static void playTrackAt(int index) {
        if (playlist.isEmpty()) return;
        if (index < 0 || index >= playlist.size()) return;
        playIndex(index);
    }

    /**
     * Проигрывание конкретного индекса (с авто-пропуском битых).
     */
    private static void playIndex(int index) {
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

    /**
     * Пропуск битого файла — перейти к следующему, но не зацикливаться.
     */
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

    /**
     * Callback — трек закончился естественным образом.
     */
    private static void onTrackFinished() {
        if (playlist.isEmpty()) return;

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

    /**
     * Установка громкости (0.0 - 1.0).
     */
    public static void setVolume(float v) {
        ModConfig.musicVolume = Math.max(0.0f, Math.min(1.0f, v));
        applyVolume();
        ConfigManager.save();
    }

    private static void applyVolume() {
        playback.setVolume(ModConfig.musicVolume);
    }

    /**
     * Циклическая смена repeat: off → one → all → off.
     */
    public static void cycleRepeat() {
        int mode = (ModConfig.musicRepeat + 1) % 3;
        ModConfig.musicRepeat = mode;
        ConfigManager.save();
    }

    /**
     * Переключить shuffle.
     */
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
     * Текущая позиция в секундах.
     */
    public static int getPositionSeconds() {
        MusicTrack t = getCurrentTrack();
        if (t == null) return 0;
        int durationSec = getDurationSeconds();
        return (int) (durationSec * playback.getProgress());
    }

    /**
     * Длительность текущего трека в секундах.
     * Кэшируется по пути.
     */
    public static int getDurationSeconds() {
        MusicTrack t = getCurrentTrack();
        if (t == null) return 0;

        Integer cached = durationCache.get(t.path());
        if (cached != null) return cached;

        try (javax.sound.sampled.AudioInputStream ais =
                     javax.sound.sampled.AudioSystem.getAudioInputStream(t.path().toFile())) {
            javax.sound.sampled.AudioFormat fmt = ais.getFormat();
            long frames = ais.getFrameLength();
            if (frames > 0 && fmt.getFrameRate() > 0) {
                int sec = (int) (frames / fmt.getFrameRate());
                durationCache.put(t.path(), sec);
                return sec;
            }
        } catch (Exception ignored) {}

        return 0;
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

    /**
     * Форматирование секунд в MM:SS.
     */
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
}