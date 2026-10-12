package com.resistancedlc.voice;

/**
 * VoiceCommandAction — список поддерживаемых действий голосовых команд.
 *
 * Чтобы добавить новое действие:
 *   1) Добавь значение в enum.
 *   2) Добавь локализацию в en_us.json / ru_ru.json (ключ: gui.resistancedlc.voice.action.<name>).
 *   3) Обработай в VoiceCommandListener.applyAction(...).
 */
public enum VoiceCommandAction {
    OPEN_GUI("open_gui"),
    HUD_EDITOR("hud_editor"),
    CHANGE_LANG("change_lang"),
    CLOSE_GUI("close_gui");

    private final String id;

    VoiceCommandAction(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static VoiceCommandAction fromId(String id) {
        for (VoiceCommandAction a : values()) {
            if (a.id.equals(id)) return a;
        }
        return null;
    }
}