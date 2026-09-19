package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;

/**
 * MacroManager — отправка команд/сообщений по бинду.
 * 5 слотов, каждая с собственной клавишей.
 *
 * Если текст начинается с "/" — отправляется как команда.
 * Иначе — как обычное сообщение в чат.
 */
public class MacroManager {

    public static void executeMacro(int index) {
        if (index < 0 || index >= 5) return;

        String text = getMacroCommand(index);
        if (text == null || text.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) return;

        // Если начинается с "/" — команда, иначе — сообщение в чат
        if (text.startsWith("/")) {
            // Убираем слэш, sendCommand сам его добавит
            client.getConnection().sendCommand(text.substring(1));
        } else {
            // Обычное сообщение в чат
            client.getConnection().sendChat(text);
        }
    }

    public static String getMacroCommand(int index) {
        return switch (index) {
            case 0 -> ModConfig.macro1;
            case 1 -> ModConfig.macro2;
            case 2 -> ModConfig.macro3;
            case 3 -> ModConfig.macro4;
            case 4 -> ModConfig.macro5;
            default -> null;
        };
    }

    public static void setMacroCommand(int index, String command) {
        switch (index) {
            case 0 -> ModConfig.macro1 = command;
            case 1 -> ModConfig.macro2 = command;
            case 2 -> ModConfig.macro3 = command;
            case 3 -> ModConfig.macro4 = command;
            case 4 -> ModConfig.macro5 = command;
        }
        ConfigManager.save();
    }
}