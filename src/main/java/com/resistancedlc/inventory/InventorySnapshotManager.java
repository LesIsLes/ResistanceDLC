package com.resistancedlc.inventory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.resistancedlc.ResistanceDLC;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * InventorySnapshotManager — save/load/list/delete/rename для снапшотов инвентаря.
 *
 * Хранение: config/resistancedlc/inventories/inventory_N.json (N = 1..5).
 */
public final class InventorySnapshotManager {

    public static final int MAX_SLOTS = 5;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path DIR = FabricLoader.getInstance()
            .getConfigDir().resolve("resistancedlc").resolve("inventories");

    private InventorySnapshotManager() {}

    public static Path getDir() {
        return DIR;
    }

    public static void ensureDir() {
        try {
            if (!Files.exists(DIR)) {
                Files.createDirectories(DIR);
            }
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("[Inventory] Cannot create dir: " + e.getMessage());
        }
    }

    private static Path fileFor(int index) {
        return DIR.resolve("inventory_" + index + ".json");
    }

    /**
     * Возвращает список снапшотов по индексам 1..MAX_SLOTS.
     * null — если слот пустой.
     */
    public static List<InventorySnapshot> listAll() {
        ensureDir();
        List<InventorySnapshot> list = new ArrayList<>();
        for (int i = 1; i <= MAX_SLOTS; i++) {
            list.add(load(i));
        }
        return list;
    }

    /**
     * Загружает снапшот по индексу. null — если файла нет или битый.
     */
    public static InventorySnapshot load(int index) {
        if (index < 1 || index > MAX_SLOTS) return null;
        Path file = fileFor(index);
        if (!Files.exists(file)) return null;

        try {
            String content = Files.readString(file);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            if (json == null) return null;

            InventorySnapshot snap = new InventorySnapshot();
            if (json.has("name"))      snap.setName(json.get("name").getAsString());
            if (json.has("timestamp")) snap.setTimestamp(json.get("timestamp").getAsLong());

            if (json.has("slots")) {
                JsonArray arr = json.getAsJsonArray("slots");
                for (int i = 0; i < Math.min(arr.size(), InventorySnapshot.TOTAL_SLOTS); i++) {
                    String nbt = arr.get(i).getAsString();
                    snap.setSlot(i, InventorySnapshot.nbtToStack(nbt));
                }
            }
            return snap;
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[Inventory] Load failed #" + index + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Сохраняет снапшот по индексу.
     */
    public static boolean save(int index, InventorySnapshot snap) {
        if (index < 1 || index > MAX_SLOTS) return false;
        if (snap == null) return false;

        ensureDir();
        try {
            JsonObject json = new JsonObject();
            json.addProperty("name", snap.getName());
            json.addProperty("timestamp", snap.getTimestamp());

            JsonArray arr = new JsonArray();
            for (int i = 0; i < InventorySnapshot.TOTAL_SLOTS; i++) {
                arr.add(InventorySnapshot.stackToNbt(snap.getSlot(i)));
            }
            json.add("slots", arr);

            Files.writeString(fileFor(index), GSON.toJson(json));
            return true;
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("[Inventory] Save failed #" + index + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean delete(int index) {
        if (index < 1 || index > MAX_SLOTS) return false;
        try {
            return Files.deleteIfExists(fileFor(index));
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("[Inventory] Delete failed #" + index + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean rename(int index, String newName) {
        InventorySnapshot snap = load(index);
        if (snap == null) return false;
        if (newName == null || newName.trim().isEmpty()) return false;
        snap.setName(newName.trim());
        return save(index, snap);
    }

    /**
     * Создаёт снапшот из текущего игрока.
     * Крафт НЕ сохраняется — только броня, оффхенд, хотбар, основной инвентарь.
     */
    public static InventorySnapshot captureFromPlayer(Player player) {
        InventorySnapshot snap = new InventorySnapshot();
        if (player == null) return snap;

        // Броня (4 слота)
        snap.setSlot(InventorySnapshot.IDX_HEAD,  player.getItemBySlot(EquipmentSlot.HEAD).copy());
        snap.setSlot(InventorySnapshot.IDX_CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        snap.setSlot(InventorySnapshot.IDX_LEGS,  player.getItemBySlot(EquipmentSlot.LEGS).copy());
        snap.setSlot(InventorySnapshot.IDX_FEET,  player.getItemBySlot(EquipmentSlot.FEET).copy());

        // Оффхенд
        snap.setSlot(InventorySnapshot.IDX_OFFHAND, player.getItemBySlot(EquipmentSlot.OFFHAND).copy());

        // Хотбар 0..8 → слоты 5..13
        for (int i = 0; i < 9; i++) {
            snap.setSlot(InventorySnapshot.IDX_HOTBAR_0 + i,
                    player.getInventory().getItem(i).copy());
        }

        // Основной инвентарь 9..35 → слоты 14..40
        for (int i = 9; i < 36; i++) {
            snap.setSlot(InventorySnapshot.IDX_MAIN_9 + (i - 9),
                    player.getInventory().getItem(i).copy());
        }

        snap.setTimestamp(System.currentTimeMillis());
        return snap;
    }

    /**
     * Авто-имя для пустого слота: "Inventory N".
     */
    public static String defaultNameFor(int index) {
        return "Inventory " + index;
    }

    public static void openFolder() {
        ensureDir();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String path = DIR.toAbsolutePath().toString();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer.exe", path);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", path);
            } else {
                pb = new ProcessBuilder("xdg-open", path);
            }
            pb.start();
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("[Inventory] Cannot open folder: " + e.getMessage());
        }
    }
}