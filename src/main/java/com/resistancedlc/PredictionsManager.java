package com.resistancedlc;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PredictionsManager {

    /**
     * Результат симуляции одной траектории.
     * points      — точки траектории (в мировых координатах).
     * impactPoint — точка попадания в блок, либо null (улетела в небо/за steps).
     * hitBlock    — true, если попала в блок.
     */
    public static class Trajectory {
        public final List<Vec3> points;
        public final Vec3 impactPoint;
        public final boolean hitBlock;

        public Trajectory(List<Vec3> points, Vec3 impactPoint, boolean hitBlock) {
            this.points = points;
            this.impactPoint = impactPoint;
            this.hitBlock = hitBlock;
        }
    }

    public static class ProjectileSettings {
        public final float gravity;
        public final float drag;
        public final float baseSpeed;

        public ProjectileSettings(float gravity, float drag, float baseSpeed) {
            this.gravity = gravity;
            this.drag = drag;
            this.baseSpeed = baseSpeed;
        }
    }

    public static ProjectileSettings getSettingsFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        Item item = stack.getItem();
        if (item instanceof BowItem) return new ProjectileSettings(0.05f, 0.99f, 3.0f);
        if (item instanceof CrossbowItem) return new ProjectileSettings(0.05f, 0.99f, 3.15f);
        if (item == Items.SNOWBALL) return new ProjectileSettings(0.03f, 0.99f, 1.5f);
        if (item == Items.ENDER_PEARL) return new ProjectileSettings(0.03f, 0.99f, 1.5f);
        if (item == Items.EGG) return new ProjectileSettings(0.03f, 0.99f, 1.5f);
        if (item == Items.SPLASH_POTION) return new ProjectileSettings(0.05f, 0.99f, 0.5f);
        if (item == Items.LINGERING_POTION) return new ProjectileSettings(0.05f, 0.99f, 0.5f);
        if (item == Items.EXPERIENCE_BOTTLE) return new ProjectileSettings(0.07f, 0.99f, 0.7f);
        if (item == Items.TRIDENT) return new ProjectileSettings(0.05f, 0.99f, 2.5f);
        return null;
    }

    public static boolean isAiming(Player player) {
        if (player == null) return false;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        // Лук — только если натягивает
        if (main.getItem() instanceof BowItem || off.getItem() instanceof BowItem) {
            if (player.isUsingItem()) return true;
        }

        // Арбалет — если заряжен (проверяем оба)
        if (main.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(main)) return true;
        if (off.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(off)) return true;

        // Перка/снежок/зелья — просто в руке
        if (getSettingsFor(main) != null) return true;
        if (getSettingsFor(off) != null) return true;

        return false;
    }

    public static List<Trajectory> simulateAllTrajectories(Player player, int steps) {
        List<Trajectory> result = new ArrayList<>();

        ItemStack stack = player.getMainHandItem();
        if (getSettingsFor(stack) == null) stack = player.getOffhandItem();
        if (getSettingsFor(stack) == null) return result;

        ProjectileSettings settings = getSettingsFor(stack);

        // ✅ ФИКС (Вариант A): старт из КАМЕРЫ, а не из глаз тела.
        // Камера учитывает bob-view (бег), hurt-shake (тряска), F5, freelook.
        // Направление берём от ТЕЛА (player.getLookAngle()) — снаряд летит
        // туда, куда смотрит тело, даже если камера отдельно.
        Minecraft mc = Minecraft.getInstance();
        Camera cam = mc.gameRenderer.getMainCamera();

        Vec3 pos = cam.position();
        Vec3 look = player.getLookAngle();

        // ✅ Мультишот — проверяем в main и в off
        boolean isMultishot = false;
        if (stack.getItem() instanceof CrossbowItem) {
            var enchHolder = player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.MULTISHOT);
            isMultishot = EnchantmentHelper.getItemEnchantmentLevel(enchHolder, stack) > 0;
        }

        if (isMultishot) {
            result.add(simulateOne(player, pos, look, settings, steps, 0));
            result.add(simulateOne(player, pos, look, settings, steps, -10));
            result.add(simulateOne(player, pos, look, settings, steps, +10));
        } else {
            result.add(simulateOne(player, pos, look, settings, steps, 0));
        }

        return result;
    }

    /**
     * Симуляция одной траектории.
     * Рейкаст по блокам каждые 2 шага.
     */
    private static Trajectory simulateOne(Player player, Vec3 start, Vec3 look,
                                          ProjectileSettings s, int steps,
                                          double angleDeg) {
        List<Vec3> points = new ArrayList<>();

        Vec3 rotatedLook = look;
        if (angleDeg != 0) {
            double rad = Math.toRadians(angleDeg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double x = look.x * cos - look.z * sin;
            double z = look.x * sin + look.z * cos;
            rotatedLook = new Vec3(x, look.y, z);
        }

        Vec3 pos = start;
        Vec3 velocity = rotatedLook.scale(s.baseSpeed);

        Vec3 lastChecked = pos;   // точка последнего рейкаста (каждые 2 шага)
        points.add(pos);

        for (int i = 0; i < steps; i++) {
            Vec3 prev = pos;
            pos = pos.add(velocity);
            velocity = velocity.scale(s.drag);
            velocity = velocity.add(0, -s.gravity, 0);

            points.add(pos);

            // ✅ Рейкаст каждые 2 шага
            if (i % 2 == 1) {
                BlockHitResult hit = raycastBlock(player, lastChecked, pos);
                if (hit != null) {
                    // Попадание в блок
                    Vec3 impact = hit.getLocation();
                    points.set(points.size() - 1, impact);   // заменяем последнюю точку на impact
                    return new Trajectory(points, impact, true);
                }
                lastChecked = pos;
            }
        }

        return new Trajectory(points, null, false);
    }

    /**
     * Рейкаст по блокам между двумя точками.
     * Возвращает null, если попадания нет.
     */
    private static BlockHitResult raycastBlock(Player player, Vec3 from, Vec3 to) {
        if (player.level() == null) return null;
        try {
            ClipContext ctx = new ClipContext(
                    from, to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            );
            BlockHitResult hit = player.level().clip(ctx);
            if (hit.getType() != HitResult.Type.MISS) return hit;
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void reset() {}
}