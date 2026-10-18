package com.resistancedlc.headwear;

import java.util.ArrayList;
import java.util.List;

/**
 * Три пресета шапок. Геометрия строится один раз (lazy, cached).
 * Все модели в локальном пространстве:
 *   - Y = 0 → уровень "полей" (сидит на верхушке головы)
 *   - Y > 0 → вверх
 *   - Передняя сторона модели: +Z
 */
public enum HeadwearPreset {
    TOP_HAT("top_hat"),
    CHINESE_HAT("chinese_hat"),
    JESTER_CAP("jester_cap");

    public final String id;
    private List<HeadwearQuad> cachedQuads;

    HeadwearPreset(String id) {
        this.id = id;
    }

    public static HeadwearPreset byId(String id) {
        if (id != null) {
            for (HeadwearPreset p : values()) {
                if (p.id.equals(id)) return p;
            }
        }
        return TOP_HAT;
    }

    public List<HeadwearQuad> getQuads() {
        if (cachedQuads == null) {
            cachedQuads = build();
        }
        return cachedQuads;
    }

    private List<HeadwearQuad> build() {
        return switch (this) {
            case TOP_HAT -> buildTopHat();
            case CHINESE_HAT -> buildChineseHat();
            case JESTER_CAP -> buildJesterCap();
        };
    }

    // ============================================================
    // TOP HAT — цилиндр с полями и красной лентой
    // ============================================================
    private static List<HeadwearQuad> buildTopHat() {
        List<HeadwearQuad> q = new ArrayList<>();

        float bodyR   = 0.20f;
        float brimR   = 0.32f;
        float bodyH   = 0.35f;
        float brimY   = 0.0f;
        int   segs    = 16;

        int bodyColor = 0xFF2B2B2B;
        int brimColor = 0xFF1A1A1A;
        int bandColor = 0xFF8B0000;

        float bodyBaseY = 0.05f;

        // Боковая поверхность цилиндра
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float x0 = (float) Math.cos(a0) * bodyR;
            float z0 = (float) Math.sin(a0) * bodyR;
            float x1 = (float) Math.cos(a1) * bodyR;
            float z1 = (float) Math.sin(a1) * bodyR;
            q.add(new HeadwearQuad(
                    x0, bodyBaseY, z0,
                    x1, bodyBaseY, z1,
                    x1, bodyBaseY + bodyH, z1,
                    x0, bodyBaseY + bodyH, z0,
                    bodyColor));
        }

        // Верхний диск (треугольники как вырожденные квады)
        float topY = bodyBaseY + bodyH;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float x0 = (float) Math.cos(a0) * bodyR;
            float z0 = (float) Math.sin(a0) * bodyR;
            float x1 = (float) Math.cos(a1) * bodyR;
            float z1 = (float) Math.sin(a1) * bodyR;
            q.add(new HeadwearQuad(
                    0, topY, 0,
                    x1, topY, z1,
                    x0, topY, z0,
                    x0, topY, z0,
                    bodyColor));
        }

        // Поля (кольцо)
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float ox0 = (float) Math.cos(a0) * brimR;
            float oz0 = (float) Math.sin(a0) * brimR;
            float ox1 = (float) Math.cos(a1) * brimR;
            float oz1 = (float) Math.sin(a1) * brimR;
            float ix0 = (float) Math.cos(a0) * bodyR;
            float iz0 = (float) Math.sin(a0) * bodyR;
            float ix1 = (float) Math.cos(a1) * bodyR;
            float iz1 = (float) Math.sin(a1) * bodyR;
            q.add(new HeadwearQuad(
                    ix0, brimY, iz0,
                    ix1, brimY, iz1,
                    ox1, brimY, oz1,
                    ox0, brimY, oz0,
                    brimColor));
        }

        // Красная лента чуть больше радиуса
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float r = bodyR + 0.005f;
            float x0 = (float) Math.cos(a0) * r;
            float z0 = (float) Math.sin(a0) * r;
            float x1 = (float) Math.cos(a1) * r;
            float z1 = (float) Math.sin(a1) * r;
            q.add(new HeadwearQuad(
                    x0, bodyBaseY, z0,
                    x1, bodyBaseY, z1,
                    x1, bodyBaseY + 0.06f, z1,
                    x0, bodyBaseY + 0.06f, z0,
                    bandColor));
        }

        return q;
    }

    // ============================================================
    // CHINESE HAT — конус с широкими полями и красным наконечником
    // ============================================================
    private static List<HeadwearQuad> buildChineseHat() {
        List<HeadwearQuad> q = new ArrayList<>();

        float brimR      = 0.42f;
        float coneH      = 0.28f;
        float brimY      = 0.0f;
        int   segs       = 24;
        int   tipSegs    = 8;
        float tipH       = 0.035f;
        float tipR       = 0.025f;

        int coneColor = 0xFFE8C87A;
        int brimColor = 0xFFCFA862;
        int tipColor  = 0xFFFF3300;

        // Конус от полей к верхушке
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float x0 = (float) Math.cos(a0) * brimR;
            float z0 = (float) Math.sin(a0) * brimR;
            float x1 = (float) Math.cos(a1) * brimR;
            float z1 = (float) Math.sin(a1) * brimR;
            q.add(new HeadwearQuad(
                    x0, brimY, z0,
                    x1, brimY, z1,
                    0, coneH, 0,
                    0, coneH, 0,
                    coneColor));
        }

        // Поля (вид снизу)
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float x0 = (float) Math.cos(a0) * brimR;
            float z0 = (float) Math.sin(a0) * brimR;
            float x1 = (float) Math.cos(a1) * brimR;
            float z1 = (float) Math.sin(a1) * brimR;
            q.add(new HeadwearQuad(
                    0, brimY, 0,
                    x1, brimY, z1,
                    x0, brimY, z0,
                    x0, brimY, z0,
                    brimColor));
        }

        // Красный наконечник
        for (int i = 0; i < tipSegs; i++) {
            float a0 = (float) (2 * Math.PI * i / tipSegs);
            float a1 = (float) (2 * Math.PI * (i + 1) / tipSegs);
            float x0 = (float) Math.cos(a0) * tipR;
            float z0 = (float) Math.sin(a0) * tipR;
            float x1 = (float) Math.cos(a1) * tipR;
            float z1 = (float) Math.sin(a1) * tipR;
            q.add(new HeadwearQuad(
                    x0, coneH, z0,
                    x1, coneH, z1,
                    0, coneH + tipH, 0,
                    0, coneH + tipH, 0,
                    tipColor));
        }

        return q;
    }

    // ============================================================
    // JESTER CAP — обычный конус с шариком наверху
    // ============================================================
    private static List<HeadwearQuad> buildJesterCap() {
        List<HeadwearQuad> q = new ArrayList<>();

        int   segs      = 24;
        float baseR     = 0.22f;
        float height    = 0.45f;
        int   coneColor = 0xFF8B0000; // Темно-красный
        int   ballColor = 0xFFEEEEEE; // Белый шарик

        // Конус
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float x0 = (float) Math.cos(a0) * baseR;
            float z0 = (float) Math.sin(a0) * baseR;
            float x1 = (float) Math.cos(a1) * baseR;
            float z1 = (float) Math.sin(a1) * baseR;

            // Основание конуса (сидит на голове)
            q.add(new HeadwearQuad(
                    x0, 0.0f, z0,
                    x1, 0.0f, z1,
                    0, height, 0,
                    0, height, 0,
                    coneColor));
        }

        // Донышко (чтобы снизу не было пустоты)
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (2 * Math.PI * i / segs);
            float a1 = (float) (2 * Math.PI * (i + 1) / segs);
            float x0 = (float) Math.cos(a0) * baseR;
            float z0 = (float) Math.sin(a0) * baseR;
            float x1 = (float) Math.cos(a1) * baseR;
            float z1 = (float) Math.sin(a1) * baseR;
            q.add(new HeadwearQuad(
                    0, 0.0f, 0,
                    x1, 0.0f, z1,
                    x0, 0.0f, z0,
                    x0, 0.0f, z0,
                    coneColor));
        }

        // Шарик на макушке (в виде куба из 6 квадов)
        float br = 0.06f;
        float bx = 0, by = height, bz = 0;

        // +Z
        q.add(new HeadwearQuad(bx-br, by-br, bz+br, bx+br, by-br, bz+br, bx+br, by+br, bz+br, bx-br, by+br, bz+br, ballColor));
        // -Z
        q.add(new HeadwearQuad(bx+br, by-br, bz-br, bx-br, by-br, bz-br, bx-br, by+br, bz-br, bx+br, by+br, bz-br, ballColor));
        // +Y
        q.add(new HeadwearQuad(bx-br, by+br, bz-br, bx+br, by+br, bz-br, bx+br, by+br, bz+br, bx-br, by+br, bz+br, ballColor));
        // -Y
        q.add(new HeadwearQuad(bx-br, by-br, bz+br, bx+br, by-br, bz+br, bx+br, by-br, bz-br, bx-br, by-br, bz-br, ballColor));
        // -X
        q.add(new HeadwearQuad(bx-br, by-br, bz-br, bx-br, by+br, bz-br, bx-br, by+br, bz+br, bx-br, by-br, bz+br, ballColor));
        // +X
        q.add(new HeadwearQuad(bx+br, by-br, bz+br, bx+br, by+br, bz+br, bx+br, by+br, bz-br, bx+br, by-br, bz-br, ballColor));

        return q;
    }
}