package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/**
 * WeatherChangeManager — визуальное изменение погоды + спавн снежинок.
 *
 * Client-side only. Не влияет на сервер и «Тягун» — Riptide
 * проверяет серверную погоду, а не нашу.
 */
public final class WeatherChangeManager {

    private static final int SNOW_SPAWN_INTERVAL = 2;      // каждые 2 тика
    private static final int SNOW_PARTICLES_PER_SPAWN = 7; // средняя плотность

    private static int tickCounter = 0;

    private WeatherChangeManager() {}

    // ===================== GETTERS ДЛЯ МИКСИНА =====================

    /**
     * Возвращает fake rainLevel (0..1) или -1, если override не нужен.
     */
    public static float getFakeRainLevel() {
        if (!ModConfig.weatherChangeEnabled) return -1.0f;
        return switch (ModConfig.weatherChangeMode) {
            case 0 -> 0.0f;   // CLEAR
            case 1 -> 1.0f;   // RAIN
            case 2 -> 1.0f;   // THUNDER
            case 3 -> 0.0f;   // SNOW — дождь отключён, снежинки спавним сами
            case 4 -> 0.0f;   // OVERCAST — дождя нет
            default -> -1.0f;
        };
    }

    /**
     * Возвращает fake thunderLevel (0..1) или -1, если override не нужен.
     */
    public static float getFakeThunderLevel() {
        if (!ModConfig.weatherChangeEnabled) return -1.0f;
        return switch (ModConfig.weatherChangeMode) {
            case 0 -> 0.0f;   // CLEAR
            case 1 -> 0.0f;   // RAIN
            case 2 -> 1.0f;   // THUNDER
            case 3 -> 0.0f;   // SNOW
            case 4 -> 0.8f;   // OVERCAST — тёмные тучи без молний
            default -> -1.0f;
        };
    }

    // ===================== TICK =====================

    public static void tick() {
        if (!ModConfig.weatherChangeEnabled) return;
        if (ModConfig.weatherChangeMode != 3) return; // только SNOW

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return; // не спавним при открытых экранах

        tickCounter++;
        if (tickCounter < SNOW_SPAWN_INTERVAL) return;
        tickCounter = 0;

        spawnSnowflakes(mc);
    }

    private static void spawnSnowflakes(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;

        RandomSource random = level.random;
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();

        // Радиус: 15×15 по горизонтали, высота 8 над игроком
        double radiusXZ = 7.5;

        // Минимум 1.5 блока над глазами (чтобы не спавнить в камере)
        double minY = py + 1.5;
        double maxY = py + 8.0;

        for (int i = 0; i < SNOW_PARTICLES_PER_SPAWN; i++) {
            double x = px + (random.nextDouble() * 2.0 - 1.0) * radiusXZ;
            double y = minY + random.nextDouble() * (maxY - minY);
            double z = pz + (random.nextDouble() * 2.0 - 1.0) * radiusXZ;

            // Проверка: не спавнить в блоках (грубая)
            if (level.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
                level.addParticle(
                        ParticleTypes.SNOWFLAKE,
                        x, y, z,
                        0.0, -0.05, 0.0
                );
            }
        }
    }

    public static void reset() {
        tickCounter = 0;
    }

    public static String getModeName(int mode) {
        return switch (mode) {
            case 0 -> "CLEAR";
            case 1 -> "RAIN";
            case 2 -> "THUNDER";
            case 3 -> "SNOW";
            case 4 -> "OVERCAST";
            default -> "?";
        };
    }
}