package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.network.chat.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SmartChatManager — группировка повторяющихся сообщений в чате.
 *
 * Логика:
 *   1. На каждое входящее сообщение вызывается processMessage(plainText).
 *   2. Если это повтор (за 60 сек, от того же отправителя, с тем же текстом) —
 *      возвращает true → ChatComponent.addMessage отменяется.
 *   3. Если новое — сохраняется в map, возвращает false.
 *   4. При рендере ChatComponent рисует бейдж "×N" через миксин на content().
 */
public final class SmartChatManager {

    private static final long GROUP_WINDOW_MS = 60_000L;

    /** sender + "|" + text → GroupedEntry */
    private static final Map<String, GroupedEntry> ENTRIES = new HashMap<>();

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm:ss", Locale.ROOT);

    private SmartChatManager() {}

    public static class GroupedEntry {
        public final String sender;
        public final String text;
        public final List<Long> timestamps = new ArrayList<>();
        public int count = 1;

        public GroupedEntry(String sender, String text, long ts) {
            this.sender = sender;
            this.text = text;
            this.timestamps.add(ts);
        }
    }

    /**
     * Обработка нового сообщения.
     * @return true — повтор (нужно отменить добавление),
     *         false — новое (пропустить как обычно).
     */
    public static boolean processMessage(String plainText) {
        if (!ModConfig.smartChatEnabled) return false;
        if (!ModConfig.smartChatGroupingEnabled) return false;
        if (plainText == null || plainText.isEmpty()) return false;

        long now = System.currentTimeMillis();

        // Чистим старше 60 сек
        ENTRIES.entrySet().removeIf(e ->
                now - e.getValue().timestamps.get(0) > GROUP_WINDOW_MS);

        String sender = extractSender(plainText);
        String key = sender + "|" + plainText;

        GroupedEntry existing = ENTRIES.get(key);
        if (existing != null) {
            existing.count++;
            existing.timestamps.add(now);
            return true;
        }

        ENTRIES.put(key, new GroupedEntry(sender, plainText, now));
        return false;
    }

    /** Возвращает количество повторов для текста (минимум 1). */
    public static int getCount(String plainText) {
        if (plainText == null) return 1;
        String sender = extractSender(plainText);
        GroupedEntry e = ENTRIES.get(sender + "|" + plainText);
        return (e == null) ? 1 : e.count;
    }

    /**
     * Tooltip: "Повторено N раз: 12:34:56, 12:34:57, ..."
     * Возвращает null, если повторов не было.
     */
    public static Component getTooltip(String plainText) {
        if (plainText == null) return null;
        String sender = extractSender(plainText);
        GroupedEntry e = ENTRIES.get(sender + "|" + plainText);
        if (e == null || e.count < 2) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(LocalizationManager.get("gui.resistancedlc.smartchat.tooltip", e.count));
        sb.append(": ");

        int max = Math.min(e.timestamps.size(), 10);
        for (int i = 0; i < max; i++) {
            if (i > 0) sb.append(", ");
            sb.append(TIME_FMT.format(new Date(e.timestamps.get(i))));
        }
        if (e.timestamps.size() > 10) sb.append(", …");

        return Component.literal(sb.toString());
    }

    /**
     * Извлекает отправителя из plain text.
     * Поддерживает форматы:
     *   "P1: message"     → "P1"
     *   "[P1] message"    → "P1"
     *   "<P1> message"    → "P1"
     *   "P1 » message"    → "P1"
     * Если не находит — возвращает "".
     */
    private static String extractSender(String plain) {
        if (plain == null || plain.isEmpty()) return "";

        // [P1] msg
        Matcher m = Pattern.compile("^\\[([^\\]]+)\\]").matcher(plain);
        if (m.find()) return m.group(1);

        // <P1> msg
        m = Pattern.compile("^<([^>]+)>").matcher(plain);
        if (m.find()) return m.group(1);

        // P1: msg (только если P1 — одно слово без пробелов)
        m = Pattern.compile("^([\\wа-яА-Я_\\-]+):\\s").matcher(plain);
        if (m.find()) return m.group(1);

        // P1 » msg
        m = Pattern.compile("^([\\wа-яА-Я_\\-]+)\\s*»").matcher(plain);
        if (m.find()) return m.group(1);

        return "";
    }

    public static void clear() {
        ENTRIES.clear();
    }
}