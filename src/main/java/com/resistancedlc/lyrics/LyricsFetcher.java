package com.resistancedlc.lyrics;

import com.resistancedlc.ResistanceDLC;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LyricsFetcher — асинхронная загрузка текстов песен из LRCLIB API.
 *
 * LRCLIB — бесплатная публичная база синхронизированных текстов.
 * API: https://lrclib.net/docs
 *
 * Логика:
 *   1. Точный запрос /api/get с track_name + artist_name + duration (±2 сек).
 *   2. Если 404 → fuzzy-поиск /api/search (первый результат).
 *
 * User-Agent обязателен (иначе бан).
 */
public final class LyricsFetcher {

    private static final String API_BASE = "https://lrclib.net/api";
    private static final String USER_AGENT = "ResistanceDLC/3.0.0 (Minecraft Fabric Mod)";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Извлечение "syncedLyrics":"..." из JSON
    private static final Pattern SYNCED_PATTERN = Pattern.compile(
            "\"syncedLyrics\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private LyricsFetcher() {}

    /**
     * Асинхронно загружает LRC-текст с LRCLIB.
     *
     * @param trackName   название трека (без артиста)
     * @param artistName  имя артиста
     * @param durationSec длительность в секундах (для точного матчинга, может быть 0)
     * @return CompletableFuture с LRC-строкой, или null если не найдено
     */
    public static CompletableFuture<String> fetchAsync(String trackName,
                                                       String artistName,
                                                       int durationSec) {
        if (trackName == null || trackName.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        String artist = (artistName == null || artistName.isBlank()) ? "" : artistName;

        StringBuilder url = new StringBuilder(API_BASE)
                .append("/get")
                .append("?track_name=").append(urlEncode(trackName))
                .append("&artist_name=").append(urlEncode(artist));

        // LRCLIB требует duration для точного матчинга (±2 сек).
        // Если duration ещё не распарсен (0) — НЕ передаём его.
        if (durationSec > 0) {
            url.append("&duration=").append(durationSec);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> {
                        int status = response.statusCode();
                        if (status == 200) {
                            String lrc = extractSyncedLyrics(response.body());
                            if (lrc != null && !lrc.isBlank()) {
                                ResistanceDLC.LOGGER.info("[LRCLIB] Found synced lyrics for '"
                                        + trackName + "' by '" + artist + "'");
                                return CompletableFuture.completedFuture(lrc);
                            }
                            ResistanceDLC.LOGGER.info("[LRCLIB] No synced lyrics for '"
                                    + trackName + "' (instrumental or plain only)");
                            return CompletableFuture.completedFuture(null);
                        } else if (status == 404) {
                            // Exact не нашёл — пробуем fuzzy
                            ResistanceDLC.LOGGER.info("[LRCLIB] Exact miss for '"
                                    + trackName + "' by '" + artist + "', trying fuzzy search...");
                            return searchFuzzy(trackName, artist);
                        } else if (status == 429) {
                            ResistanceDLC.LOGGER.warn("[LRCLIB] Rate limited (429). Retry-After: "
                                    + response.headers().firstValue("Retry-After").orElse("?"));
                            return CompletableFuture.completedFuture(null);
                        } else {
                            ResistanceDLC.LOGGER.warn("[LRCLIB] HTTP " + status
                                    + " for '" + trackName + "'");
                            return CompletableFuture.completedFuture(null);
                        }
                    })
                    .exceptionally(ex -> {
                        ResistanceDLC.LOGGER.warn("[LRCLIB] Request failed for '"
                                + trackName + "': " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[LRCLIB] Cannot build request: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Fuzzy-поиск через /api/search. Берёт первый результат с syncedLyrics.
     */
    private static CompletableFuture<String> searchFuzzy(String trackName, String artistName) {
        StringBuilder url = new StringBuilder(API_BASE)
                .append("/search")
                .append("?track_name=").append(urlEncode(trackName));

        if (artistName != null && !artistName.isBlank()) {
            url.append("&artist_name=").append(urlEncode(artistName));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            ResistanceDLC.LOGGER.info("[LRCLIB] Fuzzy search HTTP "
                                    + response.statusCode());
                            return null;
                        }
                        String body = response.body();
                        // Ответ — массив JSON. Ищем ПЕРВЫЙ syncedLyrics.
                        Matcher m = SYNCED_PATTERN.matcher(body);
                        if (m.find()) {
                            String lrc = unescapeJson(m.group(1));
                            if (lrc != null && !lrc.isBlank()) {
                                ResistanceDLC.LOGGER.info("[LRCLIB] Fuzzy search found lyrics for '"
                                        + trackName + "'");
                                return lrc;
                            }
                        }
                        ResistanceDLC.LOGGER.info("[LRCLIB] Fuzzy search — no synced lyrics");
                        return null;
                    })
                    .exceptionally(ex -> {
                        ResistanceDLC.LOGGER.warn("[LRCLIB] Fuzzy search failed: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[LRCLIB] Cannot build fuzzy request: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Извлекает поле "syncedLyrics" из JSON-ответа и разэкранирует.
     */
    private static String extractSyncedLyrics(String json) {
        Matcher m = SYNCED_PATTERN.matcher(json);
        if (!m.find()) return null;
        return unescapeJson(m.group(1));
    }

    /**
     * Разэкранирование JSON-строки: \n, \t, \r, \", \\, \/.
     */
    private static String unescapeJson(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                switch (next) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    default -> sb.append(next);
                }
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}