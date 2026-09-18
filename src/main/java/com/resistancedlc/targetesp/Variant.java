package com.resistancedlc.targetesp;

import java.util.Arrays;
import net.minecraft.network.chat.Component;

/**
 * Variant — режимы TargetESP.
 * Портировано с TargetESP by anomalith (Yarn → Mojang mappings).
 */
public enum Variant {
    OFF("off", "gui.resistancedlc.targetesp.variant.off"),
    GHOSTS_2("ghosts_2", "gui.resistancedlc.targetesp.variant.ghosts_2"),
    CUBES("cubes", "gui.resistancedlc.targetesp.variant.cubes"),
    RING("ring", "gui.resistancedlc.targetesp.variant.ring"),
    CRYSTALS("crystals", "gui.resistancedlc.targetesp.variant.crystals");

    private static final Variant[] VALUES = Variant.values();
    private final String id;
    private final String translationKey;

    Variant(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return this.id;
    }

    public Component displayName() {
        return Component.translatable(this.translationKey);
    }

    public Variant next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public static Variant fromId(String id) {
        if (id == null) return OFF;
        return Arrays.stream(VALUES)
                .filter(v -> v.id.equalsIgnoreCase(id))
                .findFirst()
                .orElse(OFF);
    }
}