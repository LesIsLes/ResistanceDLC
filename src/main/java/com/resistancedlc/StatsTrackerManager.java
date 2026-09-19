package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import java.util.regex.Pattern;

/**
 * StatsTrackerManager — счётчик K/D (убийства/смерти).
 */
public class StatsTrackerManager {

    // KILL паттерны (мы убили кого-то) — 7 штук
    private static final Pattern[] KILL_PATTERNS = {
            Pattern.compile("^You killed\\s+(.+?)\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s+was slain by you\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s+was killed by you\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Ты убил\\s+(.+?)\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Вы убили\\s+(.+?)\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s+был убит тобой\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s+была убита тобой\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s+убит тобой\\.?$", Pattern.CASE_INSENSITIVE),
    };

    // DEATH паттерны (мы умерли) — 12 штук
    private static final Pattern[] DEATH_PATTERNS = {
            Pattern.compile("^You died\\.?.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s+killed you\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(.+?)\\s+slain you\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^You were slain by\\s+(.+?)\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^You were killed by\\s+(.+?)\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\S+\\s+был убит\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\S+\\s+убит\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\S+\\s+погиб\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\S+\\s+погибла\\.?$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Ты погиб\\.?.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Ты умер\\.?.*$", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^Вы умерли\\.?.*$", Pattern.CASE_INSENSITIVE),
    };

    public static void onChatMessage(String raw) {
        if (!ModConfig.statsTrackerEnabled) return;
        if (raw == null || raw.isEmpty()) return;

        String cleaned = stripFormatting(raw).trim();
        if (cleaned.isEmpty()) return;

        // DEATH первым (более специфично)
        for (int i = 0; i < DEATH_PATTERNS.length; i++) {
            if (DEATH_PATTERNS[i].matcher(cleaned).matches()) {
                ModConfig.statsDeaths++;
                ConfigManager.save();
                return;
            }
        }

        // KILL
        for (int i = 0; i < KILL_PATTERNS.length; i++) {
            if (KILL_PATTERNS[i].matcher(cleaned).matches()) {
                ModConfig.statsKills++;
                ConfigManager.save();
                ResistanceDLC.LOGGER.info("[StatsTracker] +1 kill (total=" + ModConfig.statsKills + ")");
                return;
            }
        }
    }

    private static String stripFormatting(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static float getKdRatio() {
        if (ModConfig.statsDeaths <= 0) return ModConfig.statsKills;
        return (float) ModConfig.statsKills / ModConfig.statsDeaths;
    }

    public static String formatKd() {
        float kd = getKdRatio();
        return String.format("%.2f", kd);
    }

    public static void resetStats() {
        ModConfig.statsKills = 0;
        ModConfig.statsDeaths = 0;
        ConfigManager.save();
    }
}