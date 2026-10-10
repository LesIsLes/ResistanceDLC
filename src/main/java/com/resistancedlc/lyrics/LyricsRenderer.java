package com.resistancedlc.lyrics;

import com.resistancedlc.MusicPlayerManager;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LyricsRenderer {

    private static final long FADE_IN_MS = 300L;
    private static final long FADE_OUT_MS = 500L;

    private static String lastCurrentText = null;
    private static long currentFadeStart = 0;
    private static Vec3 currentAnchorWorld = null;

    private static String lastNextText = null;
    private static long nextFadeStart = 0;
    private static Vec3 nextAnchorWorld = null;

    public static void tick() {
        if (!ModConfig.lyricsEnabled) return;
        if (!ModConfig.musicPlayerEnabled) return;
        if (!MusicPlayerManager.isPlaying() && !MusicPlayerManager.isPaused()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String trackName = MusicPlayerManager.getCurrentTrackName();
        if (trackName == null) return;

        List<LyricsManager.LrcLine> lines = LyricsManager.loadFor(trackName);
        if (lines == null || lines.isEmpty()) return;

        long posMs = MusicPlayerManager.getCurrentPositionMs();
        int curIdx = LyricsManager.getCurrentIndex(lines, posMs);
        if (curIdx < 0) return;

        String currentText = lines.get(curIdx).text;

        String nextText = null;
        if (ModConfig.lyricsShowNext && curIdx + 1 < lines.size()) {
            nextText = lines.get(curIdx + 1).text;
        }

        // Current — anchor фиксируется при смене строки
        if (!currentText.equals(lastCurrentText)) {
            lastCurrentText = currentText;
            currentFadeStart = System.currentTimeMillis();
            currentAnchorWorld = computeWorldAnchor(mc, false);
        }

        // Next — anchor фиксируется при смене строки
        if (nextText != null && !nextText.equals(lastNextText)) {
            lastNextText = nextText;
            nextFadeStart = System.currentTimeMillis();
            nextAnchorWorld = computeWorldAnchor(mc, true);
        } else if (nextText == null) {
            lastNextText = null;
            nextAnchorWorld = null;
        }
    }

    public static String getCurrentText() { return lastCurrentText; }
    public static String getNextText() { return lastNextText; }
    public static Vec3 getCurrentAnchorWorld() { return currentAnchorWorld; }
    public static Vec3 getNextAnchorWorld() { return nextAnchorWorld; }

    public static float getCurrentAlpha() {
        if (lastCurrentText == null) return 0f;
        long posMs = MusicPlayerManager.getCurrentPositionMs();
        String trackName = MusicPlayerManager.getCurrentTrackName();
        if (trackName == null) return 0f;
        List<LyricsManager.LrcLine> lines = LyricsManager.loadFor(trackName);
        if (lines == null) return 0f;
        int curIdx = LyricsManager.getCurrentIndex(lines, posMs);
        if (curIdx < 0) return 0f;
        long lineEnd = (curIdx + 1 < lines.size())
                ? lines.get(curIdx + 1).timestampMs
                : Long.MAX_VALUE;
        return computeAlpha(posMs, lineEnd, currentFadeStart);
    }

    public static float getNextAlpha() {
        if (lastNextText == null) return 0f;
        long elapsed = System.currentTimeMillis() - nextFadeStart;
        if (elapsed >= FADE_IN_MS) return 1.0f;
        return (float) elapsed / FADE_IN_MS;
    }

    private static Vec3 computeWorldAnchor(Minecraft mc, boolean isNext) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 forward = mc.player.getLookAngle().normalize();
        Vec3 worldUp = new Vec3(0, 1, 0);

        float dist = ModConfig.lyricsDistance;
        float upOffset = isNext ? -0.20f : 0.35f;

        Vec3 dir = forward.scale(dist).add(worldUp.scale(upOffset));
        return eye.add(dir);
    }

    private static float computeAlpha(long posMs, long lineEnd, long fadeStartWallClock) {
        long wallElapsed = System.currentTimeMillis() - fadeStartWallClock;
        float fadeIn = wallElapsed < FADE_IN_MS
                ? (float) wallElapsed / FADE_IN_MS
                : 1.0f;

        float fadeOut = 1.0f;
        if (lineEnd != Long.MAX_VALUE) {
            long untilEnd = lineEnd - posMs;
            if (untilEnd < FADE_OUT_MS) {
                if (untilEnd < 0) return 0f;
                fadeOut = (float) untilEnd / FADE_OUT_MS;
            }
        }
        return Math.min(fadeIn, fadeOut);
    }

    public static void reset() {
        lastCurrentText = null;
        lastNextText = null;
        currentFadeStart = 0;
        nextFadeStart = 0;
        currentAnchorWorld = null;
        nextAnchorWorld = null;
    }
}