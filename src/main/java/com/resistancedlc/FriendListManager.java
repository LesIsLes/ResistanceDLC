package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * FriendListManager — список друзей (по нику).
 *
 * Хранится как строка в ModConfig (friendsRaw), разделитель — "|".
 * Используется для:
 *   - Подсветки ников друзей в чате (FriendListMixin)
 *   - Подсветки ников друзей в табе (через HUD, не миксин)
 *   - Опционально: отдельный цвет в TargetESP (позже)
 */
public class FriendListManager {

    /** Возвращает список друзей. */
    public static List<String> getFriends() {
        List<String> list = new ArrayList<>();
        if (ModConfig.friendsRaw == null || ModConfig.friendsRaw.isEmpty()) return list;
        for (String s : ModConfig.friendsRaw.split("\\|")) {
            String t = s.trim();
            if (!t.isEmpty()) list.add(t);
        }
        return list;
    }

    public static void setFriends(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(list.get(i).replace("|", "").trim());
        }
        ModConfig.friendsRaw = sb.toString();
    }

    /**
     * @return true если добавлено (не было раньше)
     */
    public static boolean addFriend(String name) {
        if (name == null) return false;
        name = name.trim();
        if (name.isEmpty()) return false;

        List<String> list = getFriends();
        for (String f : list) {
            if (f.equalsIgnoreCase(name)) return false;
        }
        list.add(name);
        setFriends(list);
        ConfigManager.save();
        return true;
    }

    /**
     * @return true если удалено
     */
    public static boolean removeFriend(String name) {
        if (name == null) return false;
        name = name.trim();
        if (name.isEmpty()) return false;

        List<String> list = getFriends();
        boolean removed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).equalsIgnoreCase(name)) {
                list.remove(i);
                removed = true;
            }
        }
        if (removed) {
            setFriends(list);
            ConfigManager.save();
        }
        return removed;
    }

    public static void clearFriends() {
        ModConfig.friendsRaw = "";
        ConfigManager.save();
    }

    /**
     * Проверяет, является ли ник другом (регистронезависимо, точное совпадение).
     */
    public static boolean isFriend(String name) {
        if (name == null || name.isEmpty()) return false;
        if (!ModConfig.friendListEnabled) return false;
        for (String f : getFriends()) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /**
     * Проверяет, содержит ли текст имя друга (для чата).
     * Нужно, чтобы подсветить строку "FriendName: привет".
     */
    public static boolean textContainsFriend(String text) {
        if (text == null || text.isEmpty()) return false;
        if (!ModConfig.friendListEnabled) return false;
        String lower = text.toLowerCase();
        for (String f : getFriends()) {
            if (lower.contains(f.toLowerCase())) return true;
        }
        return false;
    }
}