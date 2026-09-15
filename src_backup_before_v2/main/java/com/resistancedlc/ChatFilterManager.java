package com.resistancedlc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Логика ChatFilter — фильтрация сообщений по стоп-словам.
 * Регистронезависимый, частичное совпадение.
 */
public class ChatFilterManager {

    /**
     * Проверяет, содержит ли сообщение одно из стоп-слов.
     * @param message текст сообщения
     * @return true, если сообщение нужно скрыть
     */
    public static boolean shouldHide(String message) {
        if (!MyCustomScreen.chatFilterEnabled) return false;
        if (message == null || message.isEmpty()) return false;

        List<String> words = getWords();
        if (words.isEmpty()) return false;

        String lowerMessage = message.toLowerCase();

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (lowerMessage.contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Возвращает список стоп-слов.
     */
    public static List<String> getWords() {
        List<String> list = new ArrayList<>();
        if (MyCustomScreen.chatFilterWordsRaw == null
                || MyCustomScreen.chatFilterWordsRaw.isEmpty()) return list;

        for (String s : MyCustomScreen.chatFilterWordsRaw.split("\\|")) {
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    /**
     * Устанавливает список стоп-слов.
     */
    public static void setWords(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(list.get(i).replace("|", ""));
        }
        MyCustomScreen.chatFilterWordsRaw = sb.toString();
    }

    /**
     * Добавляет стоп-слово.
     * @return true, если добавлено
     */
    public static boolean addWord(String word) {
        if (word == null) return false;
        word = word.trim().toLowerCase();
        if (word.isEmpty()) return false;

        List<String> list = getWords();
        if (list.contains(word)) return false;

        list.add(word);
        setWords(list);
        ConfigManager.save();
        return true;
    }

    /**
     * Удаляет стоп-слово.
     * @return true, если удалено
     */
    public static boolean removeWord(String word) {
        if (word == null) return false;
        word = word.trim().toLowerCase();
        if (word.isEmpty()) return false;

        List<String> list = getWords();
        boolean removed = list.remove(word);
        if (removed) {
            setWords(list);
            ConfigManager.save();
        }
        return removed;
    }

    /**
     * Очищает все стоп-слова.
     */
    public static void clearWords() {
        MyCustomScreen.chatFilterWordsRaw = "";
        ConfigManager.save();
    }

    /**
     * Отправляет системное сообщение в чат.
     */
    public static void sendMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}