package com.resistancedlc;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KillTracker — отслеживает, кого мы недавно ударили.
 *
 * Запись: AttackEntityCallback (при ударе по entity).
 * Чтение: ClientPacketListenerMixin (при смерти entity — event ID 3).
 *
 * TTL — 5 секунд. Если entity умирает в течение этого времени
 * после нашего удара — считаем, что килл наш.
 *
 * ⚠ Ограничение: нельзя отличить "мы добили" от "он сам умер через 2 сек".
 *   Но это лучшее, что можно сделать на клиенте без информации от сервера.
 */
public class KillTracker {

    /** Время жизни записи об ударе (мс). */
    private static final long TTL_MS = 5000L;

    /** UUID entity → timestamp последнего удара. */
    private static final Map<UUID, Long> RECENT_HITS = new ConcurrentHashMap<>();

    /** Записать удар по entity. */
    public static void recordHit(UUID entityUuid) {
        if (entityUuid == null) return;
        RECENT_HITS.put(entityUuid, System.currentTimeMillis());
    }

    /**
     * Проверить, был ли недавний удар по entity.
     * Если да — вернуть true и удалить запись (одноразовое потребление).
     */
    public static boolean consumeHit(UUID entityUuid) {
        if (entityUuid == null) return false;
        Long ts = RECENT_HITS.remove(entityUuid);
        if (ts == null) return false;
        return (System.currentTimeMillis() - ts) <= TTL_MS;
    }

    /** Очистка (при выходе из мира). */
    public static void clear() {
        RECENT_HITS.clear();
    }
}