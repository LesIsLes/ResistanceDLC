package com.resistancedlc.environment;

import com.resistancedlc.config.ModConfig;

public final class CloudControlManager {
    private CloudControlManager() {}

    public static boolean isEnabled() {
        return ModConfig.cloudControlEnabled;
    }

    public static int modifyColor(int originalColor) {
        if (!isEnabled()) return originalColor;
        int rgb = ModConfig.cloudColor & 0x00FFFFFF;
        int alpha = ModConfig.cloudAlpha & 0xFF;
        return (alpha << 24) | rgb;
    }

    public static float modifyHeight(float originalHeight) {
        if (!isEnabled()) return originalHeight;
        return ModConfig.cloudHeight;
    }

    public static long modifyGameTime(long originalTime) {
        if (!isEnabled()) return originalTime;
        return (long)(originalTime * ModConfig.cloudSpeed);
    }

    public static void reset() {
        // Ничего не делаем — значения в конфиге
    }
}