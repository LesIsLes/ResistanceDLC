package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * ArmorAlertManager — предупреждение когда прочность брони падает ниже порога.
 *
 * Проверяем 4 слота брони: HEAD, CHEST, LEGS, FEET.
 * Если любой слот имеет прочность < threshold% → alert.
 *
 * Звук — с cooldown, чтобы не спамить.
 */
public class ArmorAlertManager {

    private static long lastSoundTime = 0L;
    private static final long SOUND_COOLDOWN_MS = 3000L;
    private static boolean alertActive = false;
    private static int lowestPercent = 100;

    public static void tick() {
        if (!ModConfig.armorAlertEnabled) {
            alertActive = false;
            lowestPercent = 100;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            alertActive = false;
            lowestPercent = 100;
            return;
        }

        EquipmentSlot[] slots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        int minPercent = 100;
        boolean anyDamaged = false;

        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            if (!stack.isDamageableItem()) continue;

            int maxDamage = stack.getMaxDamage();
            int damage = stack.getDamageValue();
            if (maxDamage <= 0) continue;

            float percent = 1.0f - (float) damage / maxDamage;
            int pct = (int) (percent * 100.0f);
            if (pct < minPercent) minPercent = pct;
            anyDamaged = true;
        }

        if (!anyDamaged) {
            alertActive = false;
            lowestPercent = 100;
            return;
        }

        lowestPercent = minPercent;
        alertActive = minPercent <= ModConfig.armorAlertThreshold;

        if (alertActive && ModConfig.armorAlertSound) {
            long now = System.currentTimeMillis();
            if (now - lastSoundTime >= SOUND_COOLDOWN_MS) {
                lastSoundTime = now;
                try {
                    mc.level.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_BASS.value(),
                            SoundSource.PLAYERS,
                            ModConfig.armorAlertSoundVolume,
                            ModConfig.armorAlertSoundPitch,
                            false
                    );
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isAlertActive() {
        return alertActive;
    }

    public static int getLowestPercent() {
        return lowestPercent;
    }

    public static void reset() {
        alertActive = false;
        lowestPercent = 100;
        lastSoundTime = 0L;
    }
}