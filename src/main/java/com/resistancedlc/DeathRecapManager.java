package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * DeathRecapManager — кольцевой буфер последних 5 смертей (LIFO).
 * + снимок брони каждый тик (нужен, потому что сервер выбрасывает броню до DeathScreen).
 */
public class DeathRecapManager {

    public static final int MAX_ENTRIES = 5;

    public static class ArmorSnapshot {
        public final String head, chest, legs, feet, offhand;
        public ArmorSnapshot(String head, String chest, String legs,
                             String feet, String offhand) {
            this.head = head;
            this.chest = chest;
            this.legs = legs;
            this.feet = feet;
            this.offhand = offhand;
        }
    }

    private static ArmorSnapshot lastArmorSnapshot = null;

    public static void updateArmorSnapshot(ArmorSnapshot s) { lastArmorSnapshot = s; }
    public static ArmorSnapshot getLastArmorSnapshot() { return lastArmorSnapshot; }
    public static void clearArmorSnapshot() { lastArmorSnapshot = null; }

    public static List<DeathRecapEntry> getEntries() {
        List<DeathRecapEntry> list = new ArrayList<>();
        if (ModConfig.deathRecapRaw == null || ModConfig.deathRecapRaw.isEmpty()) return list;
        for (String s : ModConfig.deathRecapRaw.split("#")) {
            if (s.isEmpty()) continue;
            DeathRecapEntry e = DeathRecapEntry.deserialize(s);
            if (e != null) list.add(e);
        }
        return list;
    }

    public static void setEntries(List<DeathRecapEntry> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("#");
            sb.append(list.get(i).serialize());
        }
        ModConfig.deathRecapRaw = sb.toString();
    }

    public static void addDeath(DeathRecapEntry e) {
        if (e == null) return;
        List<DeathRecapEntry> list = getEntries();
        list.add(0, e);
        while (list.size() > MAX_ENTRIES) list.remove(list.size() - 1);
        setEntries(list);
        ConfigManager.save();
    }

    public static void clearAll() {
        ModConfig.deathRecapRaw = "";
        ConfigManager.save();
    }

    public static boolean isEmpty() { return getEntries().isEmpty(); }
}