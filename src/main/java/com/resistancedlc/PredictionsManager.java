package com.resistancedlc;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PredictionsManager {

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

    public static List<List<Vec3>> simulateAllTrajectories(Player player, int steps) {
        List<List<Vec3>> result = new ArrayList<>();

        ItemStack stack = player.getMainHandItem();
        if (getSettingsFor(stack) == null) stack = player.getOffhandItem();
        if (getSettingsFor(stack) == null) return result;

        ProjectileSettings settings = getSettingsFor(stack);

        // ✅ ФИКС: сдвигаем старт на 0.5 блока вперёд, чтобы
        // первый сегмент не проходил через near-plane камеры.
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.getEyePosition().add(look.scale(0.5));

        // ✅ ФИКС: проверяем мультишот и в main, и в off
        boolean isMultishot = false;
        if (stack.getItem() instanceof CrossbowItem) {
            var enchHolder = player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.MULTISHOT);
            isMultishot = EnchantmentHelper.getItemEnchantmentLevel(enchHolder, stack) > 0;
        }

        if (isMultishot) {
            // ✅ ФИКС: правильный разброс — центр, -10°, +10°
            result.add(simulateOne(pos, look, settings, steps, 0));
            result.add(simulateOne(pos, look, settings, steps, -10));
            result.add(simulateOne(pos, look, settings, steps, +10));
        } else {
            result.add(simulateOne(pos, look, settings, steps, 0));
        }

        return result;
    }

    private static List<Vec3> simulateOne(Vec3 start, Vec3 look, ProjectileSettings s,
                                          int steps, double angleDeg) {
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

        for (int i = 0; i < steps; i++) {
            points.add(pos);
            pos = pos.add(velocity);
            velocity = velocity.scale(s.drag);
            velocity = velocity.add(0, -s.gravity, 0);
        }

        return points;
    }

    public static void reset() {}
}