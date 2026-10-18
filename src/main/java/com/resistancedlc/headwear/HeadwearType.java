package com.resistancedlc.headwear;

import net.minecraft.network.chat.Component;

/**
 * HeadwearType — типы шляп.
 * Рендер — процедурный (см. HeadwearRenderer).
 */
public enum HeadwearType {
    NONE("none", "gui.resistancedlc.headwear.type.none"),
    CHINESE_HAT("chinese_hat", "gui.resistancedlc.headwear.type.chinese_hat"),
    CAP("cap", "gui.resistancedlc.headwear.type.cap"),
    TOP_HAT("top_hat", "gui.resistancedlc.headwear.type.top_hat");

    private final String id;
    private final String translationKey;

    HeadwearType(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return this.id;
    }

    public Component displayName() {
        return Component.translatable(this.translationKey);
    }

    public HeadwearType next() {
        HeadwearType[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static HeadwearType fromId(String id) {
        if (id == null) return CHINESE_HAT;
        for (HeadwearType t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return CHINESE_HAT;
    }
}