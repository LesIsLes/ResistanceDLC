package com.resistancedlc.lyrics;

import com.resistancedlc.ResistanceDLC;
import com.resistancedlc.MusicPlayerManager;
import com.resistancedlc.MusicTrack;
import com.resistancedlc.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LyricsManager — парсинг LRC-файлов и синхронизация с позицией трека.
 *
 * Источники (в порядке приоритета):
 *   1. Локальный .lrc файл в config/resistancedlc/lyrics/<track>.lrc
 *   2. LRCLIB API (асинхронно, кэшируется)
 *
 * Формат LRC:
 *   [mm:ss.xx] Текст
 *   [mm:ss.xxx] Текст
 *   [offset:+/-ms]
 */
public class LyricsManager {

    private static final Path LYRICS_DIR =
            FabricLoader.getInstance().getConfigDir().resolve("resistancedlc").resolve("lyrics");

    private static final Pattern TIME_TAG = Pattern.compile(
            "\\[(\\d+):(\\d{2})(?:\\.(\\d{1,3}))?\\]");
    private static final Pattern OFFSET_TAG = Pattern.compile(
            "\\[offset:\\s*([+-]?\\d+)\\s*\\]", Pattern.CASE_INSENSITIVE);

    public static class LrcLine {
        public final long timestampMs;
        public final String text;

        public LrcLine(long timestampMs, String text) {
            this.timestampMs = timestampMs;
            this.text = text;
        }
    }

    /** Кэш: trackName -> lines (или null, если не найдено). */
    private static final Map<String, List<LrcLine>> cache = new HashMap<>();

    /** Негативный кэш: trackName -> true (уже искали, не нашли). */
    private static final Map<String, Boolean> noLyricsCache = new HashMap<>();

    /** Треки, для которых уже запущен запрос к LRCLIB (чтобы не дублировать). */
    private static final Map<String, CompletableFuture<String>> pendingFetches = new HashMap<>();

    private static String lastNotifiedTrack = null;

    public static void init() {
        try {
            if (!Files.exists(LYRICS_DIR)) {
                Files.createDirectories(LYRICS_DIR);
            }
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("[Lyrics] Cannot create lyrics dir: " + e.getMessage());
        }
    }

    public static void reset() {
        cache.clear();
        noLyricsCache.clear();
        pendingFetches.clear();
        lastNotifiedTrack = null;
    }

    /**
     * Загрузить LRC для трека.
     * 1. Проверяет кэш.
     * 2. Пробует локальный .lrc.
     * 3. Если нет — ждёт, пока duration распарсится, и запрашивает LRCLIB.
     * 4. Если запрос ещё не завершён — возвращает null (следующий вызов вернёт результат).
     */
    public static List<LrcLine> loadFor(String trackName) {
        if (trackName == null || trackName.isEmpty()) return null;

        // 1. Кэш
        if (cache.containsKey(trackName)) {
            return cache.get(trackName);
        }
        if (noLyricsCache.containsKey(trackName)) {
            return null;
        }

        // 2. Локальный файл
        Path lrcPath = LYRICS_DIR.resolve(trackName + ".lrc");
        if (Files.exists(lrcPath)) {
            try {
                List<LrcLine> lines = parseLrc(lrcPath);
                if (!lines.isEmpty()) {
                    cache.put(trackName, lines);
                    ResistanceDLC.LOGGER.info("[Lyrics] Loaded " + lines.size()
                            + " lines from local .lrc for '" + trackName + "'");
                    return lines;
                }
            } catch (Exception e) {
                ResistanceDLC.LOGGER.error("[Lyrics] Local parse failed for '"
                        + trackName + "': " + e.getMessage());
            }
        }

        // 3. LRCLIB
        if (!ModConfig.lyricsLrclibEnabled) {
            noLyricsCache.put(trackName, true);
            return null;
        }

        // Уже запущен запрос?
        if (pendingFetches.containsKey(trackName)) {
            CompletableFuture<String> future = pendingFetches.get(trackName);
            if (future.isDone()) {
                pendingFetches.remove(trackName);
                String lrc = future.join();
                if (lrc != null) {
                    List<LrcLine> lines = parseLrcString(lrc);
                    if (!lines.isEmpty()) {
                        cache.put(trackName, lines);
                        ResistanceDLC.LOGGER.info("[Lyrics] Loaded " + lines.size()
                                + " lines from LRCLIB for '" + trackName + "'");
                        return lines;
                    }
                }
                noLyricsCache.put(trackName, true);
                return null;
            }
            // Запрос ещё идёт — вернём null
            return null;
        }

        // Собираем artist + duration + ЧИСТОЕ имя трека (без артиста)
        String artist = "";
        String cleanTrackName = trackName;
        int duration = 0;
        MusicTrack track = findTrackByName(trackName);
        if (track != null) {
            artist = track.displayArtist();
            cleanTrackName = track.displayName();
            duration = MusicPlayerManager.getDurationSeconds();
        }

        // Если duration ещё не готов — ждём следующий тик (OGG парсится 100-300 мс)
        if (duration <= 0) {
            ResistanceDLC.LOGGER.info("[Lyrics] Waiting for duration to be parsed for '"
                    + trackName + "'...");
            return null;
        }

        // Запускаем запрос
        ResistanceDLC.LOGGER.info("[Lyrics] Fetching from LRCLIB: '"
                + cleanTrackName + "' by '" + artist + "' duration=" + duration);
        CompletableFuture<String> future =
                LyricsFetcher.fetchAsync(cleanTrackName, artist, duration);
        pendingFetches.put(trackName, future);

        return null;
    }

    /**
     * Парсит LRC из строки (не из файла).
     */
    private static List<LrcLine> parseLrcString(String lrc) {
        List<LrcLine> lines = new ArrayList<>();
        if (lrc == null || lrc.isBlank()) return lines;

        String[] rawLines = lrc.split("\\r?\\n");
        long offsetMs = 0;

        // Первый проход — offset
        for (String raw : rawLines) {
            Matcher m = OFFSET_TAG.matcher(raw);
            if (m.find()) {
                try {
                    offsetMs = Long.parseLong(m.group(1));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Второй проход — строки
        for (String raw : rawLines) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) continue;

            Matcher m = TIME_TAG.matcher(trimmed);
            List<Long> timestamps = new ArrayList<>();
            int lastEnd = 0;

            while (m.find()) {
                try {
                    long min = Long.parseLong(m.group(1));
                    long sec = Long.parseLong(m.group(2));
                    long ms = 0;
                    if (m.group(3) != null) {
                        String msStr = m.group(3);
                        if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                        else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                        else ms = Long.parseLong(msStr);
                    }
                    long total = min * 60_000 + sec * 1_000 + ms;
                    timestamps.add(total + offsetMs);
                } catch (NumberFormatException ignored) {}
                lastEnd = m.end();
            }

            if (timestamps.isEmpty()) continue;

            String text = trimmed.substring(lastEnd).trim();
            if (text.isEmpty()) continue;

            for (long ts : timestamps) {
                lines.add(new LrcLine(ts, text));
            }
        }

        lines.sort((a, b) -> Long.compare(a.timestampMs, b.timestampMs));
        return lines;
    }

    /**
     * Ищет MusicTrack по имени (без расширения).
     */
    private static MusicTrack findTrackByName(String trackName) {
        for (MusicTrack t : MusicPlayerManager.getPlaylist()) {
            String fileName = t.path().getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String nameNoExt = dot > 0 ? fileName.substring(0, dot) : fileName;
            if (nameNoExt.equals(trackName)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Парсит LRC из файла.
     */
    private static List<LrcLine> parseLrc(Path path) throws IOException {
        List<String> rawLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (String l : rawLines) {
            sb.append(l).append('\n');
        }
        return parseLrcString(sb.toString());
    }

    public static int getCurrentIndex(List<LrcLine> lines, long trackPositionMs) {
        if (lines == null || lines.isEmpty()) return -1;
        int idx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).timestampMs <= trackPositionMs) {
                idx = i;
            } else {
                break;
            }
        }
        return idx;
    }

    public static String getCurrentLine(List<LrcLine> lines, long trackPositionMs) {
        int idx = getCurrentIndex(lines, trackPositionMs);
        if (idx < 0 || idx >= lines.size()) return null;
        return lines.get(idx).text;
    }

    public static String getNextLine(List<LrcLine> lines, long trackPositionMs) {
        int idx = getCurrentIndex(lines, trackPositionMs);
        int next = idx + 1;
        if (next < 0 || next >= lines.size()) return null;
        return lines.get(next).text;
    }

    public static boolean shouldNotifyNoLyrics(String trackName) {
        if (trackName == null) return false;
        if (trackName.equals(lastNotifiedTrack)) return false;
        lastNotifiedTrack = trackName;
        return true;
    }

    /**
     * Проверяет, идёт ли сейчас запрос к LRCLIB для этого трека.
     * Нужно, чтобы НЕ показывать «нет текста», пока fuzzy-search не отработал.
     */
    public static boolean isFetchPending(String trackName) {
        if (trackName == null) return false;
        CompletableFuture<String> future = pendingFetches.get(trackName);
        return future != null && !future.isDone();
    }

    public static Path getLyricsDir() {
        return LYRICS_DIR;
    }

    public static String getLyricsDirPath() {
        return LYRICS_DIR.toAbsolutePath().toString();
    }
}