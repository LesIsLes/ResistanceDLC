package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PingIndicatorManager — трекер пинга игроков для tab-list.
 *
 * Кольцевой буфер последних 5 значений пинга для сглаживания.
 * Обновляется раз в секунду (20 тиков).
 */
public final class PingIndicatorManager {

    private static final int MAX_SAMPLES = 5;

    private static final Map<UUID, Deque<Integer>> SAMPLES = new HashMap<>();
    private static int tickCounter = 0;

    private PingIndicatorManager() {}

    /** Раз в 20 тиков обновляет буферы. */
    public static void tick() {
        if (!ModConfig.pingIndicatorEnabled) {
            SAMPLES.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().id();
            int latency = info.getLatency();

            Deque<Integer> deque = SAMPLES.computeIfAbsent(id, k -> new ArrayDeque<>());
            deque.addLast(latency);
            while (deque.size() > MAX_SAMPLES) deque.pollFirst();
        }

        // Удаляем ушедших игроков
        SAMPLES.keySet().removeIf(uuid ->
                mc.getConnection().getPlayerInfo(uuid) == null);
    }

    /** Средний пинг по буферу. Возвращает -1, если данных нет. */
    public static int getAverageLatency(UUID id) {
        Deque<Integer> deque = SAMPLES.get(id);
        if (deque == null || deque.isEmpty()) return -1;
        int sum = 0;
        for (int v : deque) sum += v;
        return sum / deque.size();
    }

    /**
     * Виден ли игрок в tab-list (не под Invisibility, не в другом мире).
     * Возвращает null если игрока нет в мире (нет пинга).
     */
    public static Boolean isPlayerVisible(PlayerInfo info) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        Player player = mc.level.getPlayerByUUID(info.getProfile().id());
        if (player == null) return null;   // нет в мире (в другом измерении)

        // Невидимость — пинг не показываем
        if (player.hasEffect(MobEffects.INVISIBILITY)) return false;

        return true;
    }

    public static void reset() {
        SAMPLES.clear();
        tickCounter = 0;
    }

    /** Цвет по пингу (фиксированные пороги). */
    public static int getPingColor(int ping) {
        if (ping < 0) return 0xFF808080;        // unknown — серый
        if (ping < 80) return 0xFF00FF00;       // зелёный
        if (ping < 150) return 0xFFFFFF00;      // жёлтый
        if (ping < 300) return 0xFFFF8800;      // оранжевый
        return 0xFFFF0000;                      // красный
    }
}