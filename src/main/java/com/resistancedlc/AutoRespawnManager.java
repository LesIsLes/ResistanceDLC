package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;

/**
 * AutoRespawnManager — авто-respawn через отправку пакета PERFORM_RESPAWN.
 *
 * ВАЖНО (1.21.11):
 *   mc.setScreen(null) НЕ респавнит! Он только закрывает экран,
 *   и DeathScreen открывается заново через тик — получается бесконечный цикл.
 *
 * Правильный путь: отправить ServerboundClientCommandPacket(PERFORM_RESPAWN)
 * и закрыть экран через mc.setScreen(null).
 */
public class AutoRespawnManager {

    private static long deathScreenShownAt = 0L;
    private static boolean pending = false;

    public static void tick() {
        if (!ModConfig.autoRespawnEnabled) {
            if (pending) reset();
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        boolean onDeathScreen = mc.screen instanceof DeathScreen;

        if (!onDeathScreen) {
            if (pending) reset();
            return;
        }

        if (mc.player == null || mc.getConnection() == null) {
            reset();
            return;
        }

        if (!pending) {
            pending = true;
            deathScreenShownAt = System.currentTimeMillis();
            ResistanceDLC.LOGGER.info("[AutoRespawn] DeathScreen detected, delay="
                    + ModConfig.autoRespawnDelay + "s");
        }

        long delayMs = (long) (ModConfig.autoRespawnDelay * 1000L);
        long elapsed = System.currentTimeMillis() - deathScreenShownAt;

        if (elapsed >= delayMs) {
            ResistanceDLC.LOGGER.info("[AutoRespawn] Sending PERFORM_RESPAWN packet");

            try {
                mc.getConnection().send(
                        new ServerboundClientCommandPacket(
                                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN
                        )
                );
            } catch (Exception e) {
                ResistanceDLC.LOGGER.error("[AutoRespawn] Failed to send packet: " + e.getMessage());
            }

            // Закрываем экран смерти (ванильный клиент после respawn сам закроет,
            // но на всякий случай)
            mc.setScreen(null);
            reset();
        }
    }

    public static void reset() {
        pending = false;
        deathScreenShownAt = 0L;
    }
}