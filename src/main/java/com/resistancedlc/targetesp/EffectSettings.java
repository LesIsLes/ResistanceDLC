package com.resistancedlc.targetesp;

/**
 * EffectSettings — настройки эффектов TargetESP.
 * Портировано с anomalith (Yarn → Mojang).
 *
 * Поля: size, speed, radius, waveHeight, amount, trailLength.
 * Убраны IMAGE/TRIANGLE/GHOSTS — оставлены только нужные варианты.
 */
public record EffectSettings(
        float size,
        float speed,
        float radius,
        float waveHeight,
        int amount,
        int trailLength
) {
    private static final Range STANDARD_SIZE = new Range(0.5, 2.0, 0.05);
    private static final Range GHOSTS_2_SIZE = new Range(0.05, 0.4, 0.01);
    private static final Range STANDARD_SPEED = new Range(0.2, 3.0, 0.05);

    public EffectSettings {
        if (!(Float.isFinite(size) && Float.isFinite(speed)
                && Float.isFinite(radius) && Float.isFinite(waveHeight))) {
            throw new IllegalArgumentException("Effect settings must contain finite numbers");
        }
        if (amount < 0 || trailLength < 0) {
            throw new IllegalArgumentException("Effect counts must not be negative");
        }
    }

    public static EffectSettings defaultsFor(Variant variant) {
        return switch (safeVariant(variant)) {
            case GHOSTS_2 -> new EffectSettings(0.2f, 2.0f, 0.75f, 0.55f, 3, 14);
            case CUBES -> new EffectSettings(1.0f, 1.0f, 0.7f, 0.0f, 40, 0);
            case RING -> new EffectSettings(1.0f, 1.0f, 0.5f, 0.0f, 0, 0);
            case CRYSTALS -> new EffectSettings(1.0f, 1.0f, 1.5f, 0.0f, 18, 0);
            case OFF -> new EffectSettings(1.0f, 1.0f, 0.0f, 0.0f, 0, 0);
        };
    }

    public static EffectSettings fromRaw(Variant variant,
                                         Float size, Float speed, Float radius, Float waveHeight,
                                         Integer amount, Integer trailLength) {
        EffectSettings defaults = defaultsFor(variant);
        EffectSettings candidate = new EffectSettings(
                finiteOrDefault(size, defaults.size),
                finiteOrDefault(speed, defaults.speed),
                finiteOrDefault(radius, defaults.radius),
                finiteOrDefault(waveHeight, defaults.waveHeight),
                amount == null || amount < 0 ? defaults.amount : amount,
                trailLength == null || trailLength < 0 ? defaults.trailLength : trailLength
        );
        return sanitize(variant, candidate);
    }

    public static EffectSettings sanitize(Variant variant, EffectSettings settings) {
        Variant safe = safeVariant(variant);
        EffectSettings defaults = defaultsFor(safe);
        if (settings == null) return defaults;
        return new EffectSettings(
                usesSize(safe) ? (float) sizeRange(safe).normalize(settings.size, defaults.size) : defaults.size,
                usesSpeed(safe) ? (float) speedRange(safe).normalize(settings.speed, defaults.speed) : defaults.speed,
                usesRadius(safe) ? (float) radiusRange(safe).normalize(settings.radius, defaults.radius) : defaults.radius,
                usesWaveHeight(safe) ? (float) waveHeightRange(safe).normalize(settings.waveHeight, defaults.waveHeight) : defaults.waveHeight,
                usesAmount(safe) ? (int) Math.round(amountRange(safe).normalize(settings.amount, defaults.amount)) : defaults.amount,
                usesTrailLength(safe) ? (int) Math.round(trailLengthRange(safe).normalize(settings.trailLength, defaults.trailLength)) : defaults.trailLength
        );
    }

    public EffectSettings withSize(float v) { return new EffectSettings(v, speed, radius, waveHeight, amount, trailLength); }
    public EffectSettings withSpeed(float v) { return new EffectSettings(size, v, radius, waveHeight, amount, trailLength); }
    public EffectSettings withRadius(float v) { return new EffectSettings(size, speed, v, waveHeight, amount, trailLength); }
    public EffectSettings withWaveHeight(float v) { return new EffectSettings(size, speed, radius, v, amount, trailLength); }
    public EffectSettings withAmount(int v) { return new EffectSettings(size, speed, radius, waveHeight, v, trailLength); }
    public EffectSettings withTrailLength(int v) { return new EffectSettings(size, speed, radius, waveHeight, amount, v); }

    public static boolean hasSettings(Variant variant) {
        return safeVariant(variant) != Variant.OFF;
    }

    public static boolean usesSize(Variant v) {
        return switch (safeVariant(v)) {
            case GHOSTS_2, CUBES, CRYSTALS -> true;
            default -> false;
        };
    }

    public static boolean usesSpeed(Variant v) {
        return safeVariant(v) != Variant.OFF;
    }

    public static boolean usesRadius(Variant v) {
        return switch (safeVariant(v)) {
            case GHOSTS_2, CUBES, RING, CRYSTALS -> true;
            default -> false;
        };
    }

    public static boolean usesWaveHeight(Variant v) {
        return safeVariant(v) == Variant.GHOSTS_2;
    }

    public static boolean usesAmount(Variant v) {
        return switch (safeVariant(v)) {
            case GHOSTS_2, CUBES, CRYSTALS -> true;
            default -> false;
        };
    }

    public static boolean usesTrailLength(Variant v) {
        return safeVariant(v) == Variant.GHOSTS_2;
    }

    public static Range sizeRange(Variant v) {
        return switch (safeVariant(v)) {
            case GHOSTS_2 -> GHOSTS_2_SIZE;
            case CUBES, CRYSTALS -> STANDARD_SIZE;
            default -> unsupported("size", v);
        };
    }

    public static Range speedRange(Variant v) {
        return switch (safeVariant(v)) {
            case GHOSTS_2 -> new Range(0.25, 4.0, 0.05);
            case CUBES -> new Range(0.5, 2.0, 0.05);
            case RING, CRYSTALS -> STANDARD_SPEED;
            default -> unsupported("speed", v);
        };
    }

    public static Range radiusRange(Variant v) {
        return switch (safeVariant(v)) {
            case GHOSTS_2, CUBES -> new Range(0.25, 1.5, 0.05);
            case RING -> new Range(0.25, 1.25, 0.05);
            case CRYSTALS -> new Range(0.5, 2.5, 0.05);
            default -> unsupported("radius", v);
        };
    }

    public static Range waveHeightRange(Variant v) {
        if (safeVariant(v) == Variant.GHOSTS_2) {
            return new Range(0.0, 1.0, 0.05);
        }
        return unsupported("wave height", v);
    }

    public static Range amountRange(Variant v) {
        return switch (safeVariant(v)) {
            case GHOSTS_2 -> new Range(1.0, 3.0, 1.0);
            case CUBES -> new Range(12.0, 40.0, 1.0);
            case CRYSTALS -> new Range(6.0, 32.0, 1.0);
            default -> unsupported("amount", v);
        };
    }

    public static Range trailLengthRange(Variant v) {
        if (safeVariant(v) == Variant.GHOSTS_2) {
            return new Range(4.0, 28.0, 1.0);
        }
        return unsupported("trail length", v);
    }

    private static float finiteOrDefault(Float value, float fallback) {
        return value != null && Float.isFinite(value) ? value : fallback;
    }

    private static Variant safeVariant(Variant variant) {
        return variant == null ? Variant.OFF : variant;
    }

    private static Range unsupported(String setting, Variant v) {
        throw new IllegalArgumentException(setting + " is not used by " + safeVariant(v));
    }

    public record Range(double min, double max, double step) {
        public Range {
            if (!Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(step)
                    || min > max || step <= 0.0) {
                throw new IllegalArgumentException("Invalid setting range");
            }
        }

        public double normalize(double value, double fallback) {
            double safeValue = Double.isFinite(value) ? value : fallback;
            double clamped = Math.clamp(safeValue, min, max);
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.clamp(snapped, min, max);
        }
    }
}