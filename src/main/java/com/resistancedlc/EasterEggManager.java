package com.resistancedlc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.Random;

/**
 * EasterEggManager — логика пасхалки "KillAura".
 *
 * Логика:
 *   1. hint-кнопка по умолчанию показывает "Проверка совести…"
 *   2. При клике на "Установить модуль KillAura":
 *      - играется звук denied.ogg
 *      - hint-кнопка меняется на "Ты серьёзно думал, что мы читеры? 😐" (красный)
 *      - в чат летит рандомное сообщение
 *   3. Через 500 мс hint-кнопка возвращается к "Проверка совести…"
 */
public class EasterEggManager {

    private static final Random RANDOM = new Random();

    private static long redFlashStartTime = 0;
    private static final long RED_FLASH_DURATION = 500L;

    /** Ссылка на hint-кнопку. */
    private static Button hintButton = null;

    private static final String[] CHAT_MESSAGE_KEYS = {
            "gui.resistancedlc.egg.killaura.msg1",
            "gui.resistancedlc.egg.killaura.msg2",
            "gui.resistancedlc.egg.killaura.msg3"
    };

    /**
     * Клик по кнопке "Установить модуль KillAura".
     */
    public static void onKillAuraClick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        redFlashStartTime = System.currentTimeMillis();

        // Меняем текст hint на "Ты серьёзно думал, что мы читеры? 😐" (красный)
        if (hintButton != null) {
            String deniedText = LocalizationManager.get("gui.resistancedlc.panel.killaura_denied");
            hintButton.setMessage(Component.literal("§c" + deniedText));
        }

        playDeniedSound(mc);

        String msgKey = CHAT_MESSAGE_KEYS[RANDOM.nextInt(CHAT_MESSAGE_KEYS.length)];
        mc.player.displayClientMessage(Component.literal(LocalizationManager.get(msgKey)), false);
    }

    /**
     * Тик — возвращает текст hint через 500 мс.
     */
    public static void tick() {
        if (redFlashStartTime == 0) return;
        long elapsed = System.currentTimeMillis() - redFlashStartTime;
        if (elapsed >= RED_FLASH_DURATION) {
            redFlashStartTime = 0;
            if (hintButton != null) {
                String defaultText = LocalizationManager.get("gui.resistancedlc.panel.killaura_hint");
                hintButton.setMessage(Component.literal("§7" + defaultText));
            }
        }
    }

    private static void playDeniedSound(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        try {
            SoundEvent deniedSound = SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath("resistancedlc", "denied")
            );
            mc.level.playLocalSound(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    deniedSound, SoundSource.MASTER, 1.0f, 1.0f, false
            );
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("EasterEgg: cannot play denied.ogg: " + e.getMessage());
        }
    }

    /**
     * Регистрирует hint-кнопку (вызывается из AccordionScreen.buildKillAuraPanel).
     */
    public static void registerHintButton(Button btn) {
        hintButton = btn;
        String defaultText = LocalizationManager.get("gui.resistancedlc.panel.killaura_hint");
        hintButton.setMessage(Component.literal("§7" + defaultText));
    }

    /**
     * Сбрасывает ссылку при закрытии панели.
     */
    public static void unregisterHintButton() {
        hintButton = null;
    }

    public static void reset() {
        redFlashStartTime = 0;
        hintButton = null;
    }
}