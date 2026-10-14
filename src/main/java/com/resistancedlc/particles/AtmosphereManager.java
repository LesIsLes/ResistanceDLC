package com.resistancedlc.particles;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AtmosphereManager {

    public static final class Drop {
        public double x, y, z;
        public double vx, vy, vz;
        public long spawnTime;
        public long lifetimeMs;
        public float rotation;
        public float rotationSpeed;

        public Drop(double x, double y, double z,
                    double vx, double vy, double vz,
                    long spawnTime, long lifetimeMs,
                    float rotation, float rotationSpeed) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.spawnTime = spawnTime;
            this.lifetimeMs = lifetimeMs;
            this.rotation = rotation;
            this.rotationSpeed = rotationSpeed;
        }
    }

    private static final List<Drop> DROPS = new ArrayList<>();
    private static final int MAX_DROPS = 800;

    private AtmosphereManager() {}

    public static List<Drop> getDrops() {
        return DROPS;
    }

    public static void tick() {
        if (!ModConfig.atmosphereEnabled) {
            if (!DROPS.isEmpty()) DROPS.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            DROPS.clear();
            return;
        }

        RandomSource random = level.getRandom();
        Vec3 playerPos = mc.player.position();
        float range = ModConfig.atmosphereRange;
        int targetCount = ModConfig.atmosphereCount;

        // Спавн
        int toSpawn = targetCount - DROPS.size();
        if (toSpawn > 0) {
            // Спавним не всё сразу, а порциями — так плавнее
            int spawnThisTick = Math.min(toSpawn, 5);
            for (int i = 0; i < spawnThisTick; i++) {
                spawnDrop(playerPos, range, level, random);
            }
        }

        // Обновление
        long now = System.currentTimeMillis();
        long tickDt = 50L; // 20 TPS

        Iterator<Drop> it = DROPS.iterator();
        while (it.hasNext()) {
            Drop d = it.next();
            // Физика (тик = 50мс)
            double dt = tickDt / 1000.0;
            d.x += d.vx * dt * 20.0;
            d.y += d.vy * dt * 20.0;
            d.z += d.vz * dt * 20.0;

            d.rotation += d.rotationSpeed * (float) dt;

            // Удаление: ушли за пределы радиуса или истекло время
            double dx = d.x - playerPos.x;
            double dz = d.z - playerPos.z;
            double dy = d.y - playerPos.y;
            double dist2 = dx * dx + dz * dz;

            if (dist2 > range * range * 1.5
                    || dy < -range * 1.5
                    || dy > range * 2.0
                    || now - d.spawnTime > d.lifetimeMs) {
                it.remove();
            }
        }

        // Ограничение
        while (DROPS.size() > MAX_DROPS) {
            DROPS.remove(0);
        }
    }

    private static void spawnDrop(Vec3 playerPos, float range,
                                  ClientLevel level, RandomSource random) {
        // Случайная позиция в кубе вокруг игрока
        double x = playerPos.x + (random.nextDouble() - 0.5) * range * 2.0;
        double z = playerPos.z + (random.nextDouble() - 0.5) * range * 2.0;
        double y = playerPos.y + range * 0.8 + random.nextDouble() * range * 0.4;

        // Проверка "под открытым небом"
        if (ModConfig.atmosphereOnlyOutside) {
            var pos = net.minecraft.core.BlockPos.containing(x, y, z);
            if (!level.canSeeSky(pos)) return;
        }

        // Скорость: падение вниз + лёгкий дрейф
        float fallMul = ModConfig.atmosphereFallSpeed;
        double vx = (random.nextDouble() - 0.5) * 0.4;
        double vy = -(0.15 + random.nextDouble() * 0.1) * fallMul;
        double vz = (random.nextDouble() - 0.5) * 0.4;

        long lifetime = 4000 + random.nextInt(3000);
        float rotation = random.nextFloat() * (float)(Math.PI * 2);
        float rotationSpeed = (random.nextFloat() - 0.5f) * 2.0f;

        DROPS.add(new Drop(x, y, z, vx, vy, vz,
                System.currentTimeMillis(), lifetime,
                rotation, rotationSpeed));
    }

    public static void reset() {
        DROPS.clear();
    }
}