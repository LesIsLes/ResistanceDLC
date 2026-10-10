package com.resistancedlc.lyrics;

import com.resistancedlc.ResistanceDLC;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LyricsManager — парсинг LRC-файлов и синхронизация с позицией трека.
 *
 * Формат LRC:
 *   [mm:ss.xx] Текст
 *   [mm:ss.xxx] Текст
 *   [offset:+/-ms]
 *
 * Файлы: config/resistancedlc/lyrics/<track_name>.lrc
 * где <track_name> — имя .ogg без расширения.
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

    /** Кэш: trackName -> lines (или null, если файла нет / не удалось распарсить). */
    private static final Map<String, List<LrcLine>> cache = new HashMap<>();

    /** Кэш негативных результатов: trackName -> true (уже пытались, файла нет). */
    private static final Map<String, Boolean> noLyricsCache = new HashMap<>();

    /** Имя трека, для которого уже отправляли уведомление «No lyrics». */
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
        lastNotifiedTrack = null;
    }

    /**
     * Загрузить LRC для трека. Возвращает список строк или null, если файла нет.
     */
    public static List<LrcLine> loadFor(String trackName) {
        if (trackName == null || trackName.isEmpty()) return null;

        if (cache.containsKey(trackName)) {
            return cache.get(trackName);
        }
        if (noLyricsCache.containsKey(trackName)) {
            return null;
        }

        Path lrcPath = LYRICS_DIR.resolve(trackName + ".lrc");
        if (!Files.exists(lrcPath)) {
            noLyricsCache.put(trackName, true);
            return null;
        }

        try {
            List<LrcLine> lines = parseLrc(lrcPath);
            if (lines.isEmpty()) {
                noLyricsCache.put(trackName, true);
                return null;
            }
            cache.put(trackName, lines);
            ResistanceDLC.LOGGER.info("[Lyrics] Loaded " + lines.size()
                    + " lines for '" + trackName + "'");
            return lines;
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[Lyrics] Parse failed for '" + trackName
                    + "': " + e.getMessage());
            noLyricsCache.put(trackName, true);
            return null;
        }
    }

    /**
     * Парсит LRC-файл. Поддерживает несколько тегов времени на строку.
     */
    private static List<LrcLine> parseLrc(Path path) throws IOException {
        List<LrcLine> lines = new ArrayList<>();
        List<String> rawLines = Files.readAllLines(path, StandardCharsets.UTF_8);

        long offsetMs = 0;

        // Первый проход — ищем offset
        for (String raw : rawLines) {
            Matcher m = OFFSET_TAG.matcher(raw);
            if (m.find()) {
                try {
                    offsetMs = Long.parseLong(m.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Второй проход — парсим строки
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
                        // Нормализуем .xx, .xxx → ms
                        if (msStr.length() == 1) ms = Long.parseLong(msStr) * 100;
                        else if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                        else ms = Long.parseLong(msStr);
                    }
                    long total = min * 60_000 + sec * 1_000 + ms;
                    timestamps.add(total + offsetMs);
                } catch (NumberFormatException ignored) {
                }
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
     * Индекс текущей строки для позиции trackPositionMs.
     * Возвращает -1, если позиция до первой строки.
     */
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

    /**
     * Текущая строка (или null, если до первой).
     */
    public static String getCurrentLine(List<LrcLine> lines, long trackPositionMs) {
        int idx = getCurrentIndex(lines, trackPositionMs);
        if (idx < 0 || idx >= lines.size()) return null;
        return lines.get(idx).text;
    }

    /**
     * Следующая строка (или null, если текущая — последняя).
     */
    public static String getNextLine(List<LrcLine> lines, long trackPositionMs) {
        int idx = getCurrentIndex(lines, trackPositionMs);
        int next = idx + 1;
        if (next < 0 || next >= lines.size()) return null;
        return lines.get(next).text;
    }

    /**
     * Показать ли уведомление «No lyrics» для этого трека.
     * Если трек тот же, что и в прошлый раз — не показывать повторно.
     * Возвращает true, если уведомление нужно показать.
     */
    public static boolean shouldNotifyNoLyrics(String trackName) {
        if (trackName == null) return false;
        if (trackName.equals(lastNotifiedTrack)) return false;
        lastNotifiedTrack = trackName;
        return true;
    }

    public static Path getLyricsDir() {
        return LYRICS_DIR;
    }

    public static String getLyricsDirPath() {
        return LYRICS_DIR.toAbsolutePath().toString();
    }
}