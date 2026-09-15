package com.resistancedlc;

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
        if (!MyCustomScreen.particleBlockerEnabled) return false;
        if (particleId == null || particleId.isEmpty()) return false;

        String id = particleId.toLowerCase();

        // === ОГОНЬ ===
        if (MyCustomScreen.particleBlockerFire) {
            if (id.contains("flame") || id.contains("fire")) return true;
        }

        // === ДЫМ ===
        if (MyCustomScreen.particleBlockerSmoke) {
            if (id.contains("smoke")) return true;
        }

        // === ВЗРЫВЫ ===
        if (MyCustomScreen.particleBlockerExplosion) {
            if (id.contains("explosion") || id.contains("poof")) return true;
        }

        // === ЗЕЛЬЯ ===
        if (MyCustomScreen.particleBlockerPotions) {
            if (id.contains("spell") || id.contains("witch")
                    || id.contains("effect") || id.contains("potion")) return true;
        }

        // === ВОДА ===
        if (MyCustomScreen.particleBlockerWater) {
            if (id.contains("splash") || id.contains("bubble")
                    || id.contains("water") || id.contains("drip")) return true;
        }

        // === РЕДСТОУН ===
        if (MyCustomScreen.particleBlockerRedstone) {
            if (id.contains("redstone") || id.contains("dust")) return true;
        }

        // === ПОРТАЛ ===
        if (MyCustomScreen.particleBlockerPortal) {
            if (id.contains("portal")) return true;
        }

        // === КРИТЫ ===
        if (MyCustomScreen.particleBlockerCrit) {
            if (id.contains("crit") || id.contains("enchanted_hit")) return true;
        }

        return false;
    }
}