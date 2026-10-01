package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SmartChatCoordParser — превращает координаты в кликабельные компоненты.
 *
 * Координаты: "100 64 200", "[100, 64, 200]", "(100, 64, 200)",
 *             "100, 64, 200", "x:100 y:64 z:200"
 *
 * Клик:
 *   - по координатам → CopyToClipboard ("x y z")
 *   - по "[wp]"     → SuggestCommand ("/wp add x y z")
 *
 * Формат вывода НЕ меняем — оставляем оригинал как есть.
 */
public final class SmartChatCoordParser {

    private static final Pattern[] PATTERNS = {
            // [100, 64, 200]
            Pattern.compile("\\[\\s*(-?\\d{1,7})\\s*,\\s*(-?\\d{1,7})\\s*,\\s*(-?\\d{1,7})\\s*\\]"),
            // (100, 64, 200)
            Pattern.compile("\\(\\s*(-?\\d{1,7})\\s*,\\s*(-?\\d{1,7})\\s*,\\s*(-?\\d{1,7})\\s*\\)"),
            // x:100 y:64 z:200
            Pattern.compile("x:\\s*(-?\\d{1,7})\\s+y:\\s*(-?\\d{1,7})\\s+z:\\s*(-?\\d{1,7})",
                    Pattern.CASE_INSENSITIVE),
            // 100, 64, 200
            Pattern.compile("(-?\\d{1,7})\\s*,\\s*(-?\\d{1,7})\\s*,\\s*(-?\\d{1,7})"),
            // 100 64 200 (должен быть последним — самый жадный)
            Pattern.compile("\\b(-?\\d{1,7})\\s+(-?\\d{1,7})\\s+(-?\\d{1,7})\\b"),
    };

    private SmartChatCoordParser() {}

    /**
     * Парсит Component: заменяет все найденные координаты на кликабельные.
     * НЕ меняет оригинальный текст.
     */
    public static Component parse(Component input) {
        if (input == null) return null;
        if (!ModConfig.smartChatEnabled) return input;
        if (!ModConfig.smartChatCoordClickEnabled) return input;

        String plain = input.getString();
        if (plain.isEmpty()) return input;

        // Находим все совпадения (с их позициями)
        List<CoordHit> hits = findAllCoords(plain);
        if (hits.isEmpty()) return input;

        // Строим новый Component: текст между координатами + кликабельные блоки
        MutableComponent result = Component.empty();
        int cursor = 0;

        for (CoordHit hit : hits) {
            // Текст до координат
            if (hit.start > cursor) {
                result.append(Component.literal(plain.substring(cursor, hit.start)));
            }

            // Сама координата — кликабельная (копирование в буфер)
            String coordText = plain.substring(hit.start, hit.end);
            String copyValue = hit.x + " " + hit.y + " " + hit.z;
            MutableComponent coordComponent = Component.literal(coordText)
                    .withStyle(Style.EMPTY
                            .withColor(0x55FFFF)
                            .withClickEvent(new ClickEvent.CopyToClipboard(copyValue))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Component.literal(LocalizationManager.get(
                                            "gui.resistancedlc.smartchat.coord_hint")))));
            result.append(coordComponent);

            // [wp] — вставка команды
            MutableComponent wpComponent = Component.literal(" §7[")
                    .append(Component.literal("wp")
                            .withStyle(Style.EMPTY
                                    .withColor(0xFFFF55)
                                    .withClickEvent(new ClickEvent.SuggestCommand(
                                            "/wp add " + hit.x + " " + hit.y + " " + hit.z))
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            Component.literal(LocalizationManager.get(
                                                    "gui.resistancedlc.smartchat.wp_hint"))))))
                    .append(Component.literal("]"));
            result.append(wpComponent);

            cursor = hit.end;
        }

        // Хвост
        if (cursor < plain.length()) {
            result.append(Component.literal(plain.substring(cursor)));
        }

        return result;
    }

    /**
     * Находит все координаты в тексте.
     * При пересечении — приоритет у более раннего паттерна.
     */
    private static List<CoordHit> findAllCoords(String text) {
        List<CoordHit> hits = new ArrayList<>();
        boolean[] used = new boolean[text.length()];

        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                int start = m.start();
                int end = m.end();
                if (isUsed(used, start, end)) continue;

                try {
                    int x = Integer.parseInt(m.group(1));
                    int y = Integer.parseInt(m.group(2));
                    int z = Integer.parseInt(m.group(3));
                    hits.add(new CoordHit(start, end, x, y, z));
                    markUsed(used, start, end);
                } catch (NumberFormatException ignored) {}
            }
        }

        hits.sort((a, b) -> Integer.compare(a.start, b.start));
        return hits;
    }

    private static boolean isUsed(boolean[] used, int start, int end) {
        for (int i = start; i < end && i < used.length; i++) {
            if (used[i]) return true;
        }
        return false;
    }

    private static void markUsed(boolean[] used, int start, int end) {
        for (int i = start; i < end && i < used.length; i++) {
            used[i] = true;
        }
    }

    private record CoordHit(int start, int end, int x, int y, int z) {}
}