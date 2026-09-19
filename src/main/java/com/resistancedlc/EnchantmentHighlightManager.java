package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * EnchantmentHighlightManager — подсветка текста чар в тултипе.
 *
 * Хранит список правил: ключ для матчинга + цвет + bold.
 * Подсветка — через ItemTooltipCallback (в ResistanceDLCClient).
 *
 * Формат хранения в ModConfig.enchantHighlightRaw:
 *   key1~color1~bold1|key2~color2~bold2|...
 *   Пример: "острот~0xFFFF0000~true|защит~0xFF00FF00~false"
 */
public class EnchantmentHighlightManager {

    public static class Rule {
        public String key;          // подстрока для матчинга (регистронезависимо)
        public int color;           // ARGB или RGB (в формате 0xRRGGBB)
        public boolean bold;

        public Rule(String key, int color, boolean bold) {
            this.key = key;
            this.color = color;
            this.bold = bold;
        }
    }

    public static List<Rule> getRules() {
        List<Rule> list = new ArrayList<>();
        if (ModConfig.enchantHighlightRaw == null || ModConfig.enchantHighlightRaw.isEmpty()) {
            return list;
        }
        for (String s : ModConfig.enchantHighlightRaw.split("\\|")) {
            if (s.isEmpty()) continue;
            String[] parts = s.split("~");
            if (parts.length < 3) continue;
            try {
                String key = parts[0];
                int color = (int) Long.parseLong(parts[1], 16);
                boolean bold = Boolean.parseBoolean(parts[2]);
                if (!key.isEmpty()) list.add(new Rule(key, color, bold));
            } catch (Exception ignored) {}
        }
        return list;
    }

    public static void setRules(List<Rule> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Rule r = list.get(i);
            if (i > 0) sb.append("|");
            sb.append(r.key.replace("~", "_").replace("|", "_"))
                    .append("~")
                    .append(String.format("%06X", r.color & 0xFFFFFF))
                    .append("~")
                    .append(r.bold);
        }
        ModConfig.enchantHighlightRaw = sb.toString();
    }

    public static boolean addRule(String key, int color, boolean bold) {
        if (key == null) return false;
        key = key.trim();
        if (key.isEmpty()) return false;

        List<Rule> list = getRules();
        for (Rule r : list) {
            if (r.key.equalsIgnoreCase(key)) return false;
        }
        list.add(new Rule(key, color, bold));
        setRules(list);
        ConfigManager.save();
        return true;
    }

    public static boolean removeRule(String key) {
        if (key == null) return false;
        List<Rule> list = getRules();
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).key.equalsIgnoreCase(key)) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed) {
            setRules(list);
            ConfigManager.save();
        }
        return removed;
    }

    public static void clearRules() {
        ModConfig.enchantHighlightRaw = "";
        ConfigManager.save();
    }

    /**
     * Найти правило для строки (имя чара из тултипа).
     * Матчинг по подстроке (регистронезависимо).
     */
    public static Rule findMatch(String line) {
        if (line == null || line.isEmpty()) return null;
        if (!ModConfig.enchantHighlightEnabled) return null;

        String lower = line.toLowerCase();
        for (Rule r : getRules()) {
            if (lower.contains(r.key.toLowerCase())) {
                return r;
            }
        }
        return null;
    }
}