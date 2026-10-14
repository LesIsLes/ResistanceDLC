package com.resistancedlc.particles;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class HitParticlesManager {

    private static final List<HitParticle> PARTICLES = new ArrayList<>();
    private static final int MAX_PARTICLES = 300;

    private HitParticlesManager() {}

    public static List<HitParticle> getParticles() {
        return PARTICLES;
    }

    public static void spawn(Entity target) {
        if (!ModConfig.hitParticlesEnabled) return;
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RandomSource random = mc.level.getRandom();
        Vec3 center = target.position().add(0, target.getBbHeight() / 2.0, 0);

        int count = ModConfig.hitParticlesCount;
        float speedMul = ModConfig.hitParticlesSpeed;
        float sizeMul = ModConfig.hitParticlesSize;
        int color = ModConfig.hitParticlesColor;
        HitParticle.Shape shape = switch (ModConfig.hitParticlesType) {
            case "crystal" -> HitParticle.Shape.CRYSTAL;
            case "star" -> HitParticle.Shape.STAR;
            case "snowflake" -> HitParticle.Shape.SNOWFLAKE;
            case "pyramid" -> HitParticle.Shape.PYRAMID;
            default -> HitParticle.Shape.STAR;
        };

        long now = System.currentTimeMillis();
        long lifetime = 800 + random.nextInt(400); // 0.8-1.2 сек

        for (int i = 0; i < count; i++) {
            // Случайное направление в полусфере вверх
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI / 2; // 0..90° от вертикали
            double vx = Math.cos(theta) * Math.sin(phi) * 2.5 * speedMul;
            double vy = Math.cos(phi) * 2.5 * speedMul + 1.0;
            double vz = Math.sin(theta) * Math.sin(phi) * 2.5 * speedMul;

            // Небольшое случайное смещение начальной позиции
            Vec3 spawn = center.add(
                    (random.nextDouble() - 0.5) * 0.3,
                    (random.nextDouble() - 0.5) * 0.3,
                    (random.nextDouble() - 0.5) * 0.3
            );

            float size = 0.08f * sizeMul;
            float rotationSpeed = 2.0f + random.nextFloat() * 3.0f;

            PARTICLES.add(new HitParticle(
                    spawn, new Vec3(vx, vy, vz),
                    now, lifetime, shape, color, size, rotationSpeed
            ));
        }

        // Ограничиваем количество
        while (PARTICLES.size() > MAX_PARTICLES) {
            PARTICLES.remove(0);
        }
    }

    public static void tick() {
        if (PARTICLES.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<HitParticle> it = PARTICLES.iterator();
        while (it.hasNext()) {
            HitParticle p = it.next();
            if (p.isDead(now)) {
                it.remove();
            }
        }
    }

    public static void reset() {
        PARTICLES.clear();
    }
}