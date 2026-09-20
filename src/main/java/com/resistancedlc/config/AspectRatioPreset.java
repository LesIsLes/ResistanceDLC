package com.resistancedlc.config;

public enum AspectRatioPreset {
    R16_9(1.0f, "16:9"),
    R5_4(1.25f, "5:4"),
    R4_3(1.333f, "4:3");

    private final float ratio;
    private final String displayName;

    AspectRatioPreset(float ratio, String displayName) {
        this.ratio = ratio;
        this.displayName = displayName;
    }

    public float getRatio() {
        return ratio;
    }

    public String getDisplayName() {
        return displayName;
    }
}