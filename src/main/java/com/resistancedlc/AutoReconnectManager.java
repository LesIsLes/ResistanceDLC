package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Логика AutoReconnect — переподключение к серверу после дисконнекта.
 */
public class AutoReconnectManager {

    /** Сохранённая ServerData (устанавливается при JOIN). */
    private static ServerData lastServerData = null;

    /** Время старта таймера (мс), 0 = не активно. */
    private static long reconnectStartTime = 0;

    /**
     * Вызывается при JOIN — сохраняем ServerData.
     */
    public static void onJoin() {
        Minecraft client = Minecraft.getInstance();
        if (client.getCurrentServer() != null) {
            lastServerData = client.getCurrentServer();
        }
    }

    /**
     * Вызывается при DISCONNECT — если включено, запускаем таймер.
     */
    public static void onDisconnect() {
        if (!ModConfig.autoReconnectEnabled) return;
        if (lastServerData == null) return;
        // Только на серверы (не в одиночку)
        if (Minecraft.getInstance().hasSingleplayerServer()) return;

        reconnectStartTime = System.currentTimeMillis();
    }

    /**
     * Вызывается каждый тик — проверяет, пора ли переподключаться.
     */
    public static void tick() {
        if (reconnectStartTime == 0) return;
        if (!ModConfig.autoReconnectEnabled) {
            reconnectStartTime = 0;
            return;
        }

        long elapsed = System.currentTimeMillis() - reconnectStartTime;
        long delayMs = (long) ModConfig.autoReconnectDelay * 1000L;

        if (elapsed >= delayMs) {
            performReconnect();
        }
    }

    /**
     * Выполняет переподключение.
     */
    private static void performReconnect() {
        reconnectStartTime = 0;
        if (lastServerData == null) return;

        Minecraft client = Minecraft.getInstance();
        ServerAddress address = ServerAddress.parseString(lastServerData.ip);

        try {
            ConnectScreen.startConnecting(
                    null,
                    client,
                    address,
                    lastServerData,
                    false,
                    null
            );
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("AutoReconnect failed: " + e.getMessage());
        }
    }

    /**
     * Отменяет реконнект.
     */
    public static void cancel() {
        reconnectStartTime = 0;
    }

    /**
     * Активен ли сейчас реконнект.
     */
    public static boolean isReconnecting() {
        return reconnectStartTime != 0;
    }

    /**
     * Сколько секунд осталось до реконнекта.
     */
    public static int getRemainingSeconds() {
        if (reconnectStartTime == 0) return 0;
        long elapsed = System.currentTimeMillis() - reconnectStartTime;
        long delayMs = (long) ModConfig.autoReconnectDelay * 1000L;
        if (elapsed >= delayMs) return 0;
        return (int) ((delayMs - elapsed) / 1000L) + 1;
    }

    /**
     * Сброс (при выходе из мира).
     */
    public static void reset() {
        lastServerData = null;
        reconnectStartTime = 0;
    }
}