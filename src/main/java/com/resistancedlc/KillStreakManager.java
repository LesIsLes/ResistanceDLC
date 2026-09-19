package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * KillStreakManager — счётчик подряд идущих убийств.
 *
 * Логика:
 *   1. AttackEntityCallback → запоминаем entity ID (только ОДИН раз)
 *   2. tick() → проверяем что entity мертва → +1 kill
 *   3. Уже засчитанные entity ID → в processedKills (не считаем дважды)
 */
public class KillStreakManager {

    private static final int[] LEVELS = {1, 2, 3, 4, 5, 8, 10};

    private static int currentStreak = 0;
    private static long lastKillTime = 0L;

    private static final long WINDOW_MS = 30_000L;
    private static final long KILL_CONFIRM_MS = 3_000L;

    /** Entity, которые мы ударили (ID → время удара). */
    private static final Map<Integer, Long> PENDING_KILLS = new HashMap<>();

    /** Entity, которые уже засчитаны как убитые — чтобы не считать дважды. */
    private static final Set<Integer> PROCESSED_KILLS = new HashSet<>();

    public static void onAttack(Entity target) {
        if (!ModConfig.killStreakEnabled) return;
        if (!(target instanceof LivingEntity)) return;

        int id = target.getId();

        // Уже обработали — игнорируем
        if (PROCESSED_KILLS.contains(id)) return;

        // Уже в pending — не обновляем время (первый удар важнее)
        if (PENDING_KILLS.containsKey(id)) return;

        PENDING_KILLS.put(id, System.currentTimeMillis());
    }

    public static void tick() {
        if (!ModConfig.killStreakEnabled) {
            currentStreak = 0;
            PENDING_KILLS.clear();
            PROCESSED_KILLS.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            PENDING_KILLS.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Long>> it = PENDING_KILLS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Long> entry = it.next();
            int entityId = entry.getKey();
            long attackTime = entry.getValue();
            long elapsed = now - attackTime;

            Entity entity = mc.level.getEntity(entityId);

            // Entity пропала с клиента (сервер удалил) — возможно, умерла
            if (entity == null) {
                it.remove();
                // Проверяем: не засчитывали уже?
                if (!PROCESSED_KILLS.contains(entityId)) {
                    PROCESSED_KILLS.add(entityId);
                    registerKill();
                }
                continue;
            }

            // Entity мертва или удалена — считаем kill
            if (!entity.isAlive() || entity.isRemoved()) {
                it.remove();
                if (!PROCESSED_KILLS.contains(entityId)) {
                    PROCESSED_KILLS.add(entityId);
                    registerKill();
                }
                continue;
            }

            // Прошло > 3 сек, а entity ещё жива — забываем удар (не убили)
            if (elapsed > KILL_CONFIRM_MS) {
                it.remove();
            }
        }

        // Чистим processedKills от старых ID (чтобы Set не разрастался)
        // Раз в 30 сек можно чистить те, что старше 30 сек
        // Проще — просто чистим весь Set когда окно streak сброшено
        if (currentStreak == 0 && !PROCESSED_KILLS.isEmpty()) {
            PROCESSED_KILLS.clear();
        }

        if (currentStreak > 0 && now - lastKillTime > WINDOW_MS) {
            currentStreak = 0;
            ResistanceDLC.LOGGER.info("[KillStreak] Window expired, streak reset");
        }
    }

    private static void registerKill() {
        long now = System.currentTimeMillis();

        if (now - lastKillTime > WINDOW_MS) {
            currentStreak = 0;
        }

        currentStreak++;
        lastKillTime = now;

        ResistanceDLC.LOGGER.info("[KillStreak] +1 kill, streak=" + currentStreak);

        if (isLevel(currentStreak)) {
            playStreakSound(currentStreak);
        }
    }

    private static boolean isLevel(int streak) {
        for (int lvl : LEVELS) {
            if (lvl == streak) return true;
        }
        return false;
    }

    private static void playStreakSound(int streak) {
        Path soundsDir = FabricLoader.getInstance().getConfigDir()
                .resolve("resistancedlc").resolve("sounds");
        File ogg = soundsDir.resolve("killstreak_" + streak + ".ogg").toFile();
        if (!ogg.exists()) {
            ResistanceDLC.LOGGER.warn("[KillStreak] Sound not found: " + ogg.getAbsolutePath());
            return;
        }

        try {
            OggPlayback pb = new OggPlayback();
            pb.setVolume(ModConfig.killStreakSoundVolume);
            pb.play(ogg, null);
            ResistanceDLC.LOGGER.info("[KillStreak] Playing sound: " + ogg.getName());
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[KillStreak] Failed to play: " + e.getMessage());
        }
    }

    public static int getCurrentStreak() {
        return currentStreak;
    }

    public static boolean isActive() {
        return currentStreak > 0
                && System.currentTimeMillis() - lastKillTime <= WINDOW_MS;
    }

    public static int getRemainingSeconds() {
        if (!isActive()) return 0;
        long elapsed = System.currentTimeMillis() - lastKillTime;
        return (int) ((WINDOW_MS - elapsed) / 1000L) + 1;
    }

    public static void reset() {
        currentStreak = 0;
        lastKillTime = 0L;
        PENDING_KILLS.clear();
        PROCESSED_KILLS.clear();
    }

    public static int[] getLevels() {
        return LEVELS.clone();
    }
}