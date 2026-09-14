package com.resistancedlc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Трекер последнего удара по игроку.
 * Нужен, потому что ванильный checkTotemDeathProtection не всегда знает атакующего.
 *
 * Запись: HitSoundMixin (Player.attack).
 * Чтение: TotemLogMixin (LivingEntity.checkTotemDeathProtection).
 */
public class TotemTracker {

    /** Информация о последнем ударе. */
    public static class HitInfo {
        public final String attackerName;
        public final long timestamp;

        public HitInfo(String attackerName, long timestamp) {
            this.attackerName = attackerName;
            this.timestamp = timestamp;
        }
    }

    /** UUID жертвы -> последний удар. */
    private static final Map<UUID, HitInfo> LAST_HITS = new HashMap<>();

    /** Записывает удар: victimUUID <- attackerName. */
    public static void record(UUID victimUUID, String attackerName) {
        if (victimUUID == null || attackerName == null) return;
        LAST_HITS.put(victimUUID, new HitInfo(attackerName, System.currentTimeMillis()));
    }

    /**
     * Возвращает имя атакующего, если удар был недавно (в пределах maxAgeMs).
     * Иначе null.
     */
    public static String getLastAttacker(UUID victimUUID, long maxAgeMs) {
        if (victimUUID == null) return null;
        HitInfo info = LAST_HITS.get(victimUUID);
        if (info == null) return null;
        long age = System.currentTimeMillis() - info.timestamp;
        if (age > maxAgeMs) return null;
        return info.attackerName;
    }

    /** Очистка (например, при выходе из мира). */
    public static void clear() {
        LAST_HITS.clear();
    }
}