package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * LowHPAlertManager — предупреждение при низком HP.
 *
 * Логика:
 *   - Каждый тик проверяем health игрока
 *   - Если health <= threshold → играем звук (с cooldown) + флаг "мигаем"
 *   - Флаг читается в HUD-рендере (текст по центру экрана)
 *
 * Звук: NOTE_BLOCK_PLING (клиентский, сервер не видит).
 */
public class LowHPAlertManager {

    private static long lastSoundTime = 0L;
    private static final long SOUND_COOLDOWN_MS = 2000L;
    private static boolean alertActive = false;

    /**
     * Тик — проверка HP.
     */
    public static void tick() {
        if (!ModConfig.lowHpAlertEnabled) {
            alertActive = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            alertActive = false;
            return;
        }

        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float threshold = ModConfig.lowHpAlertThreshold;

        alertActive = health > 0.0f && health <= threshold;

        if (alertActive && ModConfig.lowHpAlertSound) {
            long now = System.currentTimeMillis();
            if (now - lastSoundTime >= SOUND_COOLDOWN_MS) {
                lastSoundTime = now;
                try {
                    mc.level.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_PLING.value(),
                            SoundSource.PLAYERS,
                            ModConfig.lowHpAlertSoundVolume,
                            ModConfig.lowHpAlertSoundPitch,
                            false
                    );
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isAlertActive() {
        return alertActive;
    }

    public static void reset() {
        alertActive = false;
        lastSoundTime = 0L;
    }
}