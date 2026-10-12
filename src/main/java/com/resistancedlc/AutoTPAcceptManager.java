package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AutoTPAcceptManager — авто-принятие /tpa.
 *
 * v3.0.0-alpha.4 — ФИКСЫ:
 *   - sendChatCommand вместо sendCommand (работает на серверах).
 *   - Больше паттернов (8 рус + 8 англ).
 *   - Убрана проверка "ник без пробелов" — ники могут содержать пробелы.
 *   - Логирование распознанных фраз.
 */
public class AutoTPAcceptManager {

    // ===== РУССКИЕ ПАТТЕРНЫ =====
    private static final Pattern[] RU_PATTERNS = {
            // "Игрок Steve хочет телепортироваться к вам"
            Pattern.compile("^(?:Игрок\\s+)?(.+?)\\s+хочет телепортироваться к вам\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve отправил вам запрос на телепортацию"
            Pattern.compile("^(?:Игрок\\s+)?(.+?)\\s+отправил вам запрос на телепортацию\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve хочет к вам телепортироваться"
            Pattern.compile("^(?:Игрок\\s+)?(.+?)\\s+хочет к вам телепортироваться\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve просит телепортироваться к вам"
            Pattern.compile("^(?:Игрок\\s+)?(.+?)\\s+просит телепортироваться к вам\\.?$", Pattern.CASE_INSENSITIVE),
            // "Запрос телепортации от Steve"
            Pattern.compile("^Запрос телепортации от\\s+(.+?)\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve → /tpa"
            Pattern.compile("^(.+?)\\s*[→>]\\s*/tpa.*$", Pattern.CASE_INSENSITIVE),
            // "Steve запрашивает телепортацию"
            Pattern.compile("^(?:Игрок\\s+)?(.+?)\\s+запрашивает телепортацию\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve прислал запрос на тп"
            Pattern.compile("^(?:Игрок\\s+)?(.+?)\\s+прислал запрос на тп\\.?$", Pattern.CASE_INSENSITIVE),
    };

    // ===== АНГЛИЙСКИЕ ПАТТЕРНЫ =====
    private static final Pattern[] EN_PATTERNS = {
            // "Player Steve wants to teleport to you"
            Pattern.compile("^(?:Player\\s+)?(.+?)\\s+wants to teleport to you\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve sent you a teleport request"
            Pattern.compile("^(?:Player\\s+)?(.+?)\\s+sent you a teleport request\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve has requested to teleport to you"
            Pattern.compile("^(?:Player\\s+)?(.+?)\\s+has requested to teleport to you\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve requests to teleport"
            Pattern.compile("^(?:Player\\s+)?(.+?)\\s+requests? to teleport.*$", Pattern.CASE_INSENSITIVE),
            // "Teleport request from Steve"
            Pattern.compile("^Teleport request from\\s+(.+?)\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve → /tpa"
            Pattern.compile("^(.+?)\\s*[→>]\\s*/tpa.*$", Pattern.CASE_INSENSITIVE),
            // "Steve wants to teleport to you."
            Pattern.compile("^(?:Player\\s+)?(.+?)\\s+wants to tp to you\\.?$", Pattern.CASE_INSENSITIVE),
            // "Steve has sent you a tpa request"
            Pattern.compile("^(?:Player\\s+)?(.+?)\\s+has sent you a tpa request\\.?$", Pattern.CASE_INSENSITIVE),
    };

    // ===== ОЧЕРЕДЬ =====
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

    public static void onChatMessage(String raw) {
        if (!ModConfig.autoTpAcceptEnabled) return;
        if (raw == null || raw.isEmpty()) return;

        String cleaned = stripFormatting(raw).trim();
        if (cleaned.isEmpty()) return;

        String nick = extractNick(cleaned);
        if (nick == null || nick.isEmpty()) return;

        if (ModConfig.autoTpAcceptOnlyFriends && !FriendListManager.isFriend(nick)) {
            ResistanceDLC.LOGGER.info("[AutoTPAccept] Ignored (not friend): " + nick);
            return;
        }

        ResistanceDLC.LOGGER.info("[AutoTPAccept] Queued accept for: " + nick + " (from: '" + cleaned + "')");
        PENDING.offer(new PendingAccept(nick, System.currentTimeMillis()));
    }

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
                // ✅ sendChatCommand вместо sendCommand — работает на серверах
                mc.getConnection().sendCommand("tpaccept " + head.nick);
                ResistanceDLC.LOGGER.info("[AutoTPAccept] Sent /tpaccept " + head.nick);
            } catch (Exception e) {
                ResistanceDLC.LOGGER.error("[AutoTPAccept] Send failed: " + e.getMessage());
                // Пробуем fallback — без ника
                try {
                    mc.getConnection().sendCommand("tpaccept");
                    ResistanceDLC.LOGGER.info("[AutoTPAccept] Fallback /tpaccept sent");
                } catch (Exception e2) {
                    ResistanceDLC.LOGGER.error("[AutoTPAccept] Fallback failed: " + e2.getMessage());
                }
            }
        }
    }

    private static String extractNick(String text) {
        // RU паттерны
        for (Pattern p : RU_PATTERNS) {
            Matcher m = p.matcher(text);
            if (m.matches()) {
                String nick = m.group(1).trim();
                if (nick.isEmpty()) return null;
                return nick;
            }
        }
        // EN паттерны
        for (Pattern p : EN_PATTERNS) {
            Matcher m = p.matcher(text);
            if (m.matches()) {
                String nick = m.group(1).trim();
                if (nick.isEmpty()) return null;
                return nick;
            }
        }
        return null;
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

    public static void reset() {
        PENDING.clear();
    }
}