package com.resistancedlc.environment;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TrailsManager {

    public static final class TrailPoint {
        public final double x, y, z;
        public final long time;

        public TrailPoint(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }

    private static final Deque<TrailPoint> POINTS = new ArrayDeque<>();
    private static final int MAX_POINTS = 200;

    public static void tick() {
        if (!ModConfig.trailsEnabled) {
            POINTS.clear();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            POINTS.clear();
            return;
        }

        // Не добавляем точки, если игрок не двигается — иначе trail "стоит" на месте
        double x = mc.player.getX();
        double y = mc.player.getY() + 0.1; // чуть выше ног
        double z = mc.player.getZ();

        if (!POINTS.isEmpty()) {
            TrailPoint last = POINTS.peekLast();
            double dx = x - last.x;
            double dy = y - last.y;
            double dz = z - last.z;
            double dist2 = dx * dx + dy * dy + dz * dz;
            if (dist2 < 0.01) return; // менее 0.1 блока — не добавляем
        }

        POINTS.addLast(new TrailPoint(x, y, z, System.currentTimeMillis()));

        // Ограничиваем длину trail'а
        long lifetimeMs = (long)(ModConfig.trailsLength * 1000.0f);
        long cutoff = System.currentTimeMillis() - lifetimeMs;

        while (!POINTS.isEmpty() && POINTS.peekFirst().time < cutoff) {
            POINTS.removeFirst();
        }
        while (POINTS.size() > MAX_POINTS) {
            POINTS.removeFirst();
        }
    }

    public static Deque<TrailPoint> getPoints() {
        return POINTS;
    }

    public static void reset() {
        POINTS.clear();
    }
}