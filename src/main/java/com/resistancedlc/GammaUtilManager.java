package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import java.lang.reflect.Field;

public class GammaUtilManager {

    private static Field valueField = null;
    private static double lastApplied = -1.0;
    private static boolean reflectionFailed = false;

    public static void tick() {
        if (reflectionFailed) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        double target = ModConfig.gammaUtilEnabled ? ModConfig.gammaValue : 1.0;
        if (Math.abs(target - lastApplied) < 0.001) return;

        try {
            OptionInstance<Double> gammaOption = mc.options.gamma();
            setGammaDirectly(gammaOption, target);
            lastApplied = target;
        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("[GammaUtil] ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            reflectionFailed = true;
        }
    }

    @SuppressWarnings("unchecked")
    private static void setGammaDirectly(OptionInstance<Double> option, double value) throws Exception {
        if (valueField == null) {
            // ✅ Ищем поле по имени "value" — это ТЕКУЩЕЕ значение OptionInstance
            // (НЕ initialValue — это дефолт!)
            try {
                valueField = OptionInstance.class.getDeclaredField("value");
                valueField.setAccessible(true);
                ResistanceDLC.LOGGER.info("[GammaUtil] Found field 'value' type="
                        + valueField.getType().getSimpleName());
            } catch (NoSuchFieldException e) {
                // Fallback — перебор с исключением initialValue
                for (Field f : OptionInstance.class.getDeclaredFields()) {
                    String name = f.getName().toLowerCase();
                    if (f.getType() == Object.class && !name.contains("initial")) {
                        valueField = f;
                        valueField.setAccessible(true);
                        ResistanceDLC.LOGGER.info("[GammaUtil] Fallback field: "
                                + f.getName() + " type=" + f.getType().getSimpleName());
                        break;
                    }
                }
            }
            if (valueField == null) {
                throw new NoSuchFieldException("Cannot find 'value' field in OptionInstance");
            }
        }

        valueField.set(option, value);
    }

    public static void reset() {
        lastApplied = -1.0;
        reflectionFailed = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            try {
                setGammaDirectly(mc.options.gamma(), 1.0);
            } catch (Exception ignored) {}
        }
    }
}