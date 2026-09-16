package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Логика CoolDowns — отслеживание предметов на кулдауне.
 *
 * Как работает:
 *   1. Каждый тик проверяем инвентарь игрока.
 *   2. Для каждого предмета смотрим cooldownPercent через ItemCooldowns.
 *   3. Если percent > 0 — предмет "на кулдауне", добавляем в список.
 *   4. HUD-рендер показывает список.
 *
 * Миксин CooldownsMixin нужен, чтобы отлавливать момент старта кулдауна
 * (ванильный ItemCooldowns.tick() вызывается каждый тик).
 */
public class CooldownsManager {

    /** Один элемент списка кулдаунов. */
    public static class CooldownEntry {
        public final ItemStack stack;
        public final int slotIndex;
        public final float percent;        // 0.0 - 1.0
        public final int remainingTicks;   // сколько тиков осталось
        public final float remainingSeconds;

        public CooldownEntry(ItemStack stack, int slotIndex,
                             float percent, int remainingTicks) {
            this.stack = stack;
            this.slotIndex = slotIndex;
            this.percent = percent;
            this.remainingTicks = remainingTicks;
            this.remainingSeconds = remainingTicks / 20.0f;
        }
    }

    /**
     * Собирает список активных кулдаунов.
     * Вызывается из HUD-рендера (каждый кадр).
     */
    public static List<CooldownEntry> getActiveCooldowns() {
        List<CooldownEntry> result = new ArrayList<>();
        if (!ModConfig.cooldownsEnabled) return result;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return result;

        var cooldowns = client.player.getCooldowns();

        // 1) Хотбар (слоты 0-8) — всегда проверяем
        for (int i = 0; i < 9; i++) {
            addIfOnCooldown(result, client, cooldowns,
                    client.player.getInventory().getItem(i), i);
        }

        // 2) Остальной инвентарь — если не ограничено хотбаром
        if (!ModConfig.cooldownsShowOnlyHotbar) {
            for (int i = 9; i < 36; i++) {
                addIfOnCooldown(result, client, cooldowns,
                        client.player.getInventory().getItem(i), i);
            }
            // Offhand
            addIfOnCooldown(result, client, cooldowns,
                    client.player.getOffhandItem(), 40);
        }

        // Сортируем по остатку времени (ближайшие — вверх)
        result.sort((a, b) -> Float.compare(a.remainingSeconds, b.remainingSeconds));

        // Ограничиваем количество
        int max = Math.max(1, Math.min(10, ModConfig.cooldownsMaxItems));
        if (result.size() > max) {
            result = result.subList(0, max);
        }

        return result;
    }

    private static void addIfOnCooldown(List<CooldownEntry> list,
                                        Minecraft client,
                                        net.minecraft.world.item.ItemCooldowns cooldowns,
                                        ItemStack stack, int slotIndex) {
        if (stack == null || stack.isEmpty()) return;

        float percent = cooldowns.getCooldownPercent(stack, 0.0f);
        if (percent <= 0.0f) return;

        // Вычисляем остаток тиков из процента
        int totalTicks = getTotalCooldownTicks(stack);
        int remainingTicks = Math.round(percent * totalTicks);

        list.add(new CooldownEntry(stack, slotIndex, percent, remainingTicks));
    }

    /**
     * Пытаемся получить полную длительность кулдауна предмета (в тиках).
     * Ванильный API не даёт напрямую, поэтому используем эвристику.
     */
    private static int getTotalCooldownTicks(ItemStack stack) {
        // Известные кулдауны (в тиках = секунды * 20)
        String itemId = stack.getItem().toString();

        // Тотем — 0 (нет кулдауна в ванили, но пусть будет)
        // Жемчуг Эндера — 20 тиков (1 сек)
        if (itemId.contains("ender_pearl")) return 20;
        // Хорус — 20 тиков
        if (itemId.contains("chorus_fruit")) return 20;
        // Щит — 100 тиков (5 сек)
        if (itemId.contains("shield")) return 100;
        // Зелья — 0 (нет кулдауна)
        // Трезубец (Riptide) — 10 тиков
        if (itemId.contains("trident")) return 10;

        // По умолчанию — 20 тиков (1 сек), просто для красоты таймера
        return 20;
    }

    /**
     * Форматирует остаток в строку вида "1.2s" или "3s".
     */
    public static String formatTime(float seconds) {
        if (seconds >= 10.0f) return String.format("%.0fs", seconds);
        if (seconds >= 1.0f) return String.format("%.1fs", seconds);
        return String.format("%.2fs", seconds);
    }
}