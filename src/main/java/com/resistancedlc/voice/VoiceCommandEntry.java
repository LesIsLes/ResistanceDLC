package com.resistancedlc.voice;

import java.nio.file.Path;
import java.util.List;

/**
 * VoiceCommandEntry — одна голосовая команда.
 * Содержит действие, путь к .wav и кэш MFCC (float[][]).
 */
public class VoiceCommandEntry {

    public final VoiceCommandAction action;
    public final Path wavPath;

    /** MFCC-матрица: [frames][13 coefficients]. null, если не загружено. */
    private float[][] mfccCache = null;

    public VoiceCommandEntry(VoiceCommandAction action, Path wavPath) {
        this.action = action;
        this.wavPath = wavPath;
    }

    public float[][] getMfccCache() {
        return mfccCache;
    }

    public void setMfccCache(float[][] mfcc) {
        this.mfccCache = mfcc;
    }

    public boolean isReady() {
        return mfccCache != null && mfccCache.length > 0;
    }
}