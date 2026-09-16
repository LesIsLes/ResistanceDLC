package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * WaypointManager — логика Waypoints (get/set/add/remove/clear/rename).
 * Вынесено из MyCustomScreen, чтобы использовать в AccordionScreen и командах.
 */
public class WaypointManager {

    /** Возвращает список всех меток. */
    public static List<Waypoint> getWaypoints() {
        List<Waypoint> list = new ArrayList<>();
        if (ModConfig.waypointsRaw == null || ModConfig.waypointsRaw.isEmpty()) return list;
        for (String s : ModConfig.waypointsRaw.split("\\|")) {
            if (s.isEmpty()) continue;
            Waypoint wp = Waypoint.deserialize(s);
            if (wp != null) list.add(wp);
        }
        return list;
    }

    /** Устанавливает список меток (сериализует в ModConfig). */
    public static void setWaypoints(List<Waypoint> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(list.get(i).serialize());
        }
        ModConfig.waypointsRaw = sb.toString();
    }

    /**
     * Добавляет метку с автоименем WP<N>.
     * @return true, если добавлено (не превышен лимит)
     */
    public static boolean addWaypoint(double x, double y, double z) {
        List<Waypoint> list = getWaypoints();
        if (list.size() >= ModConfig.waypointsMax) return false;
        String name = "WP" + (list.size() + 1);
        list.add(new Waypoint(name, x, y, z));
        setWaypoints(list);
        ConfigManager.save();
        return true;
    }

    /** Удаляет метку по индексу. */
    public static boolean removeWaypoint(int index) {
        List<Waypoint> list = getWaypoints();
        if (index < 0 || index >= list.size()) return false;
        list.remove(index);
        setWaypoints(list);
        ConfigManager.save();
        return true;
    }

    /** Очищает все метки. */
    public static void clearWaypoints() {
        ModConfig.waypointsRaw = "";
        ConfigManager.save();
    }

    /** Переименовывает метку по старому имени. */
    public static boolean renameWaypoint(String oldName, String newName) {
        if (oldName == null || newName == null) return false;
        if (oldName.isEmpty() || newName.isEmpty()) return false;

        String safeNewName = newName.replace(":", "_").replace("|", "_");

        List<Waypoint> list = getWaypoints();
        boolean renamed = false;
        for (int i = 0; i < list.size(); i++) {
            Waypoint wp = list.get(i);
            if (wp.name().equals(oldName)) {
                list.set(i, new Waypoint(safeNewName, wp.x(), wp.y(), wp.z()));
                renamed = true;
            }
        }
        if (renamed) {
            setWaypoints(list);
            ConfigManager.save();
        }
        return renamed;
    }
}