package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoTPAcceptManager — авто-принятие /tpa от друзей или всех.
 *
 * Парсит чат на 8 фраз (4 рус + 4 англ), извлекает ник,
 * через 0.5 сек отправляет /tpaccept <nick>.
 *
 * Задержка — через очередь (потокобезопасно).
 */
public class AutoTPAcceptManager {

    // Русские фразы
    private static final Pattern RU_PATTERN_1 = Pattern.compile(
            "^(?:Игрок\\s+)?(.+?)\\s+хочет телепортироваться к вам\\.?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RU_PATTERN_2 = Pattern.compile(
            "^(?:Игрок\\s+)?(.+?)\\s+отправил вам запрос на телепортацию\\.?$",
            Pattern.CASE_INSENSITIVE);

    // Английские фразы
    private static final Pattern EN_PATTERN_1 = Pattern.compile(
            "^(?:Player\\s+)?(.+?)\\s+wants to teleport to you\\.?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EN_PATTERN_2 = Pattern.compile(
            "^(?:Player\\s+)?(.+?)\\s+sent you a teleport request\\.?$",
            Pattern.CASE_INSENSITIVE);

    /** Очередь отложенных tpaccept. */
    private static final java.util.concurrent.ConcurrentLinkedQueue<PendingAccept> PENDING
            = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private static class PendingAccept {
        final String nick;
        final long time;

        PendingAccept(String nick, long time) {
            this.nick = nick;
            this.time = time;
        }
    }

    /**
     * Вызывается из ClientReceiveMessageEvents.
     */
    public static void onChatMessage(String raw) {
        if (!ModConfig.autoTpAcceptEnabled) return;
        if (raw == null || raw.isEmpty()) return;

        String cleaned = stripFormatting(raw).trim();
        if (cleaned.isEmpty()) return;

        String nick = extractNick(cleaned);
        if (nick == null || nick.isEmpty()) return;

        // Проверка "только от друзей"
        if (ModConfig.autoTpAcceptOnlyFriends && !FriendListManager.isFriend(nick)) {
            ResistanceDLC.LOGGER.info("[AutoTPAccept] Ignored (not friend): " + nick);
            return;
        }

        ResistanceDLC.LOGGER.info("[AutoTPAccept] Queued accept for: " + nick);
        PENDING.offer(new PendingAccept(nick, System.currentTimeMillis()));
    }

    /**
     * Тик — обрабатывает очередь через delay.
     */
    public static void tick() {
        if (!ModConfig.autoTpAcceptEnabled) {
            PENDING.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            PENDING.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long delayMs = (long) (ModConfig.autoTpAcceptDelay * 1000L);

        PendingAccept head;
        while ((head = PENDING.peek()) != null) {
            if (now - head.time < delayMs) break;
            PENDING.poll();

            try {
                mc.getConnection().sendCommand("tpaccept " + head.nick);
                ResistanceDLC.LOGGER.info("[AutoTPAccept] Sent /tpaccept " + head.nick);
            } catch (Exception e) {
                ResistanceDLC.LOGGER.error("[AutoTPAccept] Send failed: " + e.getMessage());
            }
        }
    }

    private static String extractNick(String text) {
        Pattern[] patterns = {RU_PATTERN_1, RU_PATTERN_2, EN_PATTERN_1, EN_PATTERN_2};
        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            if (m.matches()) {
                String nick = m.group(1).trim();
                // Ник не должен содержать пробелы (после trim)
                if (nick.contains(" ")) return null;
                return nick;
            }
        }
        return null;
    }

    private static String stripFormatting(String text) {
        // Убираем §x и §xxxxxx
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;  // пропускаем цветовой код
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public static void reset() {
        PENDING.clear();
    }
}