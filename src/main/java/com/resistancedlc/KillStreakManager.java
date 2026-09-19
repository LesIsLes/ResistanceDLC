package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * KillStreakManager — счётчик подряд идущих убийств.
 *
 * Логика:
 *   1. AttackEntityCallback → запоминаем entity ID (один раз)
 *   2. tick() → проверяем что entity мертва → +1 kill
 *   3. Уже засчитанные entity ID → в processedKills (не считаем дважды)
 *   4. На уровнях LEVELS → проигрываем killstreak_N.ogg
 *
 * Приоритет звуков:
 *   1) config/resistancedlc/sounds/killstreak_N.ogg (юзерские)
 *   2) /assets/resistancedlc/sounds/killstreak_N.ogg (встроенные в jar)
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

    // ===================== CALLBACK =====================

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

    // ===================== TICK =====================

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

        // Чистим processedKills когда streak сброшен
        if (currentStreak == 0 && !PROCESSED_KILLS.isEmpty()) {
            PROCESSED_KILLS.clear();
        }

        // Проверяем окно streak
        if (currentStreak > 0 && now - lastKillTime > WINDOW_MS) {
            currentStreak = 0;
        }
    }

    // ===================== KILL =====================

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

    // ===================== SOUND =====================

    /**
     * Играет звук для уровня.
     *   1) Сначала config/resistancedlc/sounds/killstreak_N.ogg (юзерский — приоритет)
     *   2) Потом /assets/resistancedlc/sounds/killstreak_N.ogg (встроенный в jar)
     */
    private static void playStreakSound(int streak) {
        // ===== 1. Юзерский звук из config/ =====
        Path soundsDir = FabricLoader.getInstance().getConfigDir()
                .resolve("resistancedlc").resolve("sounds");
        File userOgg = soundsDir.resolve("killstreak_" + streak + ".ogg").toFile();

        if (userOgg.exists()) {
            try {
                OggPlayback pb = new OggPlayback();
                pb.setVolume(ModConfig.killStreakSoundVolume);
                pb.play(userOgg, null);
                ResistanceDLC.LOGGER.info("[KillStreak] Playing user sound: "
                        + userOgg.getName());
                return;
            } catch (Exception e) {
                ResistanceDLC.LOGGER.error("[KillStreak] User sound failed: "
                        + e.getMessage());
            }
        }

        // ===== 2. Встроенный звук из resources =====
        String resourcePath = "/assets/resistancedlc/sounds/killstreak_" + streak + ".ogg";
        InputStream resourceStream = KillStreakManager.class.getResourceAsStream(resourcePath);

        if (resourceStream == null) {
            ResistanceDLC.LOGGER.warn("[KillStreak] No sound found for streak=" + streak);
            return;
        }

        try {
            // Копируем ресурс во временный файл — OggPlayback работает с File
            Path tempFile = Files.createTempFile("killstreak_" + streak + "_", ".ogg");
            Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            resourceStream.close();

            OggPlayback pb = new OggPlayback();
            pb.setVolume(ModConfig.killStreakSoundVolume);
            pb.play(tempFile.toFile(), null);

            ResistanceDLC.LOGGER.info("[KillStreak] Playing built-in sound: killstreak_"
                    + streak + ".ogg");

            // Удаляем temp-файл через 5 секунд (после проигрывания)
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {}
            }, "KillStreak-TempCleanup").start();

        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[KillStreak] Built-in sound failed: "
                    + e.getMessage());
        }
    }

    // ===================== GETTERS =====================

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