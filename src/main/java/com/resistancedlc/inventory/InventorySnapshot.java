package com.resistancedlc.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

/**
 * InventorySnapshot — «скриншот» одного инвентаря.
 *
 * Хранит 41 слот (4 брони + оффхенд + 9 хотбар + 27 основной) как ItemStack.
 * Сериализация в JSON — через NBT-строки (SNBT).
 *
 * Слоты:
 *   0..3   — броня (HEAD, CHEST, LEGS, FEET)
 *   4      — оффхенд
 *   5..13  — хотбар (0..8)
 *   14..40 — основной инвентарь (9..35)
 */
public class InventorySnapshot {

    public static final int ARMOR_COUNT   = 4;
    public static final int OFFHAND_COUNT = 1;
    public static final int HOTBAR_COUNT  = 9;
    public static final int MAIN_COUNT    = 27;
    public static final int TOTAL_SLOTS   = ARMOR_COUNT + OFFHAND_COUNT
            + HOTBAR_COUNT + MAIN_COUNT;   // 41

    // ===== Индексы для удобства =====
    public static final int IDX_HEAD     = 0;
    public static final int IDX_CHEST    = 1;
    public static final int IDX_LEGS     = 2;
    public static final int IDX_FEET     = 3;
    public static final int IDX_OFFHAND  = 4;
    public static final int IDX_HOTBAR_0 = 5;   // 5..13
    public static final int IDX_MAIN_9   = 14;  // 14..40

    private String name;
    private long timestamp;
    private final ItemStack[] slots;

    public InventorySnapshot() {
        this.name = "Inventory";
        this.timestamp = System.currentTimeMillis();
        this.slots = new ItemStack[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            this.slots[i] = ItemStack.EMPTY;
        }
    }

    public InventorySnapshot(String name, long timestamp) {
        this();
        this.name = name;
        this.timestamp = timestamp;
    }

    // ===== Getters / Setters =====

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long ts) { this.timestamp = ts; }

    public ItemStack getSlot(int index) {
        if (index < 0 || index >= TOTAL_SLOTS) return ItemStack.EMPTY;
        return slots[index];
    }

    public void setSlot(int index, ItemStack stack) {
        if (index < 0 || index >= TOTAL_SLOTS) return;
        slots[index] = (stack == null) ? ItemStack.EMPTY : stack;
    }

    public boolean isEmpty() {
        for (ItemStack s : slots) {
            if (s != null && !s.isEmpty()) return false;
        }
        return true;
    }

    public ItemStack[] getAllSlots() {
        return slots;
    }

    // ===== NBT-сериализация ItemStack =====

    /**
     * ItemStack → строка SNBT.
     * Возвращает "" для пустого стека.
     */
    public static String stackToNbt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        try {
            var result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
            if (result.result().isPresent()) {
                Tag tag = result.result().get();
                return tag.toString();
            }
        } catch (Exception e) {
            // fallback — тихо игнорируем
        }
        return "";
    }

    /**
     * Строка SNBT → ItemStack.
     * Возвращает ItemStack.EMPTY для пустой строки или ошибки.
     */
    public static ItemStack nbtToStack(String nbt) {
        if (nbt == null || nbt.isEmpty()) return ItemStack.EMPTY;
        try {
            CompoundTag tag = TagParser.parseCompoundFully(nbt);
            var result = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag);
            if (result.result().isPresent()) {
                return result.result().get();
            }
        } catch (Exception e) {
            // fallback — тихо игнорируем
        }
        return ItemStack.EMPTY;
    }
}