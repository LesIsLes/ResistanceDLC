package com.resistancedlc.hud;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TargetHudManager {
    private static LivingEntity lastTarget = null;
    private static long lastHitTime = 0;
    private static final long SHOW_TIME_MS = 3000;

    public static void onHit(Entity target) {
        if (target instanceof LivingEntity living) {
            lastTarget = living;
            lastHitTime = System.currentTimeMillis();
        }
    }

    public static LivingEntity getTarget() {
        // Если время вышло — сбрасываем
        if (System.currentTimeMillis() - lastHitTime > SHOW_TIME_MS) {
            lastTarget = null;
        }
        return lastTarget;
    }

    public static void reset() {
        lastTarget = null;
        lastHitTime = 0;
    }
}