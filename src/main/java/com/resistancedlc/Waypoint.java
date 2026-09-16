package com.resistancedlc;

/**
 * Waypoint — именованная точка на карте.
 * Вынесен из MyCustomScreen, чтобы использовать в AccordionScreen и WaypointManager.
 */
public record Waypoint(String name, double x, double y, double z) {

    public String serialize() {
        return name.replace(":", "_").replace("|", "_")
                + ":" + x + ":" + y + ":" + z;
    }

    public static Waypoint deserialize(String s) {
        try {
            String[] parts = s.split(":");
            if (parts.length != 4) return null;
            String name = parts[0];
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Waypoint(name, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    public String displayName() {
        return name + " (" + (int) x + ", " + (int) y + ", " + (int) z + ")";
    }

    public double distanceTo(double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}