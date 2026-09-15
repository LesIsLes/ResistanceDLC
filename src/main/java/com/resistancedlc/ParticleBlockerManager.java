package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

/**
 * Логика Particle Blocker — отключение частиц по категориям.
 */
public class ParticleBlockerManager {

    /**
     * Проверяет, нужно ли заблокировать частицу по её ID.
     * @param particleId строка вида "flame", "smoke", "explosion" и т.д.
     * @return true, если частицу нужно скрыть
     */
    public static boolean shouldBlock(String particleId) {
        if (!ModConfig.particleBlockerEnabled) return false;
        if (particleId == null || particleId.isEmpty()) return false;

        String id = particleId.toLowerCase();

        // === ОГОНЬ ===
        if (ModConfig.particleBlockerFire) {
            if (id.contains("flame") || id.contains("fire")) return true;
        }

        // === ДЫМ ===
        if (ModConfig.particleBlockerSmoke) {
            if (id.contains("smoke")) return true;
        }

        // === ВЗРЫВЫ ===
        if (ModConfig.particleBlockerExplosion) {
            if (id.contains("explosion") || id.contains("poof")) return true;
        }

        // === ЗЕЛЬЯ ===
        if (ModConfig.particleBlockerPotions) {
            if (id.contains("spell") || id.contains("witch")
                    || id.contains("effect") || id.contains("potion")) return true;
        }

        // === ВОДА ===
        if (ModConfig.particleBlockerWater) {
            if (id.contains("splash") || id.contains("bubble")
                    || id.contains("water") || id.contains("drip")) return true;
        }

        // === РЕДСТОУН ===
        if (ModConfig.particleBlockerRedstone) {
            if (id.contains("redstone") || id.contains("dust")) return true;
        }

        // === ПОРТАЛ ===
        if (ModConfig.particleBlockerPortal) {
            if (id.contains("portal")) return true;
        }

        // === КРИТЫ ===
        if (ModConfig.particleBlockerCrit) {
            if (id.contains("crit") || id.contains("enchanted_hit")) return true;
        }

        return false;
    }
}