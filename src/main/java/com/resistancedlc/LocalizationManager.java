package com.resistancedlc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.resistancedlc.config.ModConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * LocalizationManager — ручная система локализации.
 * Читает en_us.json и ru_ru.json из ресурсов мода.
 * Переключение языка — через ModConfig.modLogoRussian.
 *
 * Использование:
 *   LocalizationManager.get("gui.resistancedlc.item.no_hurt_cam.title")
 */
public class LocalizationManager {

    private static final Gson GSON = new Gson();
    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> RU = new HashMap<>();
    private static boolean loaded = false;

    private static final String LANG_PATH_EN = "/assets/resistancedlc/lang/en_us.json";
    private static final String LANG_PATH_RU = "/assets/resistancedlc/lang/ru_ru.json";

    /**
     * Загрузить оба языка в память (вызывается один раз).
     */
    public static void load() {
        if (loaded) return;
        loadLang(LANG_PATH_EN, EN);
        loadLang(LANG_PATH_RU, RU);
        loaded = true;
    }

    private static void loadLang(String path, Map<String, String> target) {
        try (InputStream is = LocalizationManager.class.getResourceAsStream(path)) {
            if (is == null) {
                ResistanceDLC.LOGGER.warn("Lang file not found: " + path);
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) return;
                for (String key : json.keySet()) {
                    target.put(key, json.get(key).getAsString());
                }
            }
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("Failed to load lang file " + path + ": " + e.getMessage());
        }
    }

    /**
     * Получить строку по ключу. Язык — по ModConfig.modLogoRussian.
     */
    public static String get(String key) {
        if (key == null || key.isEmpty()) return "";
        if (!loaded) load();

        Map<String, String> target = ModConfig.modLogoRussian ? RU : EN;
        String value = target.get(key);
        if (value == null) {
            // Fallback — на английский
            value = EN.get(key);
        }
        return value != null ? value : key;
    }

    /**
     * Получить строку с плейсхолдерами (%s, %d).
     */
    public static String get(String key, Object... args) {
        String template = get(key);
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    /**
     * Перезагрузить (после смены языка).
     */
    public static void reload() {
        loaded = false;
        load();
    }

    public static boolean isRussian() {
        return ModConfig.modLogoRussian;
    }
}