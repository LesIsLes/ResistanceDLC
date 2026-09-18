package com.resistancedlc.targetesp;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * TargetManager — управление текущей целью TargetESP.
 * Портировано с anomalith.
 *
 * TTL = 6000 мс — цель держится 6 секунд после последнего удара.
 */
public final class TargetManager {

    public static final long TARGET_TTL_MS = 6000L;

    private LivingEntity currentTarget;
    private long attackedAtMs;

    public void setCurrentTarget(Entity target) {
        if (target instanceof LivingEntity living && isEligibleTarget(living)) {
            this.currentTarget = living;
            this.attackedAtMs = System.currentTimeMillis();
        } else {
            this.reset();
        }
    }

    public void reset() {
        this.currentTarget = null;
        this.attackedAtMs = 0L;
    }

    /**
     * Тик каждый кадр: проверяет, жива ли цель, и не истёк ли TTL.
     * @return true, если цель сброшена
     */
    public boolean tick(@Nullable Level currentLevel) {
        if (this.currentTarget == null) return false;

        if (!isValidInWorld(this.currentTarget, currentLevel)) {
            this.reset();
            return true;
        }
        if (System.currentTimeMillis() - this.attackedAtMs >= TARGET_TTL_MS) {
            this.reset();
        }
        return false;
    }

    public @Nullable LivingEntity getLivingTarget() {
        return this.currentTarget;
    }

    /**
     * Проверяет, подходит ли сущность как цель.
     */
    public static boolean isEligibleTarget(LivingEntity living) {
        if (!living.isAlive() || living.isSpectator()) return false;
        return true; // ← упростил — не проверяем экипировку
    }

    private static boolean isValidInWorld(LivingEntity living, @Nullable Level currentLevel) {
        if (currentLevel == null) return false;
        if (!living.isAlive()) return false;
        if (living.isRemoved()) return false;
        return true; // ← не сравниваем объекты Level
    }
}