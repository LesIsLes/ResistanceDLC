package com.resistancedlc;

/**
 * DeathRecapEntry — одна запись о смерти.
 *
 * Хранит: причину, убийцу, координаты, измерение, время, броню.
 * Сериализация — через "~~" (чтобы не конфликтовать с текстом причины/ника).
 */
public record DeathRecapEntry(
        String reason,
        String killer,
        int x,
        int y,
        int z,
        String dimension,
        long timestamp,
        String armorHead,
        String armorChest,
        String armorLegs,
        String armorFeet,
        String armorOffhand
) {

    /**
     * Сериализация в строку.
     * Формат: reason~~killer~~x~~y~~z~~dim~~timestamp~~armorHead~~armorChest~~armorLegs~~armorFeet~~armorOffhand
     */
    public String serialize() {
        return safe(reason) + "~~"
                + safe(killer) + "~~"
                + x + "~~"
                + y + "~~"
                + z + "~~"
                + safe(dimension) + "~~"
                + timestamp + "~~"
                + safe(armorHead) + "~~"
                + safe(armorChest) + "~~"
                + safe(armorLegs) + "~~"
                + safe(armorFeet) + "~~"
                + safe(armorOffhand);
    }

    /**
     * Десериализация из строки.
     * @return null если формат битый
     */
    public static DeathRecapEntry deserialize(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split("~~", -1);
        if (parts.length != 12) return null;
        try {
            String reason = parts[0];
            String killer = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int z = Integer.parseInt(parts[4]);
            String dim = parts[5];
            long ts = Long.parseLong(parts[6]);
            String head = parts[7];
            String chest = parts[8];
            String legs = parts[9];
            String feet = parts[10];
            String off = parts[11];
            return new DeathRecapEntry(reason, killer, x, y, z, dim, ts,
                    head, chest, legs, feet, off);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String safe(String v) {
        if (v == null) return "";
        // Защита только от "~~" — "|" нужен как разделитель itemId|customName
        return v.replace("~~", "_");
    }
}