package com.resistancedlc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("resistancedlc");

    public static void save() {
        saveAs("default");
    }

    public static boolean saveAs(String name) {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            JsonObject json = new JsonObject();

            // === ОСНОВНЫЕ НАСТРОЙКИ ===
            json.addProperty("showHud", MyCustomScreen.showHud);
            json.addProperty("hudColor", MyCustomScreen.hudColor);
            json.addProperty("hudBackgroundEnabled", MyCustomScreen.hudBackgroundEnabled);
            json.addProperty("hudBackgroundAlpha", MyCustomScreen.hudBackgroundAlpha);
            json.addProperty("hudBackgroundColor", MyCustomScreen.hudBackgroundColor);
            json.addProperty("hudBackgroundHeight", MyCustomScreen.hudBackgroundHeight);
            json.addProperty("guiColor", MyCustomScreen.guiColor);
            json.addProperty("guiTextColor", MyCustomScreen.guiTextColor);
            json.addProperty("hudAlpha", MyCustomScreen.hudAlpha);
            json.addProperty("showPet", MyCustomScreen.showPet);
            json.addProperty("searchHistoryRaw", MyCustomScreen.searchHistoryRaw);

            // === ВИДИМОСТЬ ЭЛЕМЕНТОВ ===
            json.addProperty("showCoords", MyCustomScreen.showCoords);
            json.addProperty("showBiome", MyCustomScreen.showBiome);
            json.addProperty("showTime", MyCustomScreen.showTime);
            json.addProperty("showFps", MyCustomScreen.showFps);
            json.addProperty("showPing", MyCustomScreen.showPing);
            json.addProperty("showTps", MyCustomScreen.showTps);
            json.addProperty("showBps", MyCustomScreen.showBps);
            json.addProperty("showDirection", MyCustomScreen.showDirection);
            json.addProperty("showHitCounter", MyCustomScreen.showHitCounter);
            json.addProperty("showPotionEffects", MyCustomScreen.showPotionEffects);
            json.addProperty("potionEffectsIcons", MyCustomScreen.potionEffectsIcons);
            json.addProperty("showEquipmentHud", MyCustomScreen.showEquipmentHud);
            json.addProperty("equipmentShowDurability", MyCustomScreen.equipmentShowDurability);
            json.addProperty("lowFireEnabled", MyCustomScreen.lowFireEnabled);
            json.addProperty("lowShieldEnabled", MyCustomScreen.lowShieldEnabled);

            // === ПОЗИЦИИ ===
            json.addProperty("coordsX", MyCustomScreen.coordsX);
            json.addProperty("coordsY", MyCustomScreen.coordsY);
            json.addProperty("biomeX", MyCustomScreen.biomeX);
            json.addProperty("biomeY", MyCustomScreen.biomeY);
            json.addProperty("timeX", MyCustomScreen.timeX);
            json.addProperty("timeY", MyCustomScreen.timeY);
            json.addProperty("fpsX", MyCustomScreen.fpsX);
            json.addProperty("fpsY", MyCustomScreen.fpsY);
            json.addProperty("pingX", MyCustomScreen.pingX);
            json.addProperty("pingY", MyCustomScreen.pingY);
            json.addProperty("tpsX", MyCustomScreen.tpsX);
            json.addProperty("tpsY", MyCustomScreen.tpsY);
            json.addProperty("bpsX", MyCustomScreen.bpsX);
            json.addProperty("bpsY", MyCustomScreen.bpsY);
            json.addProperty("directionX", MyCustomScreen.directionX);
            json.addProperty("directionY", MyCustomScreen.directionY);
            json.addProperty("hitCounterX", MyCustomScreen.hitCounterX);
            json.addProperty("hitCounterY", MyCustomScreen.hitCounterY);
            json.addProperty("potionEffectsX", MyCustomScreen.potionEffectsX);
            json.addProperty("potionEffectsY", MyCustomScreen.potionEffectsY);
            json.addProperty("equipmentHudX", MyCustomScreen.equipmentHudX);
            json.addProperty("equipmentHudY", MyCustomScreen.equipmentHudY);

            // === ПЕРЕВОД ===
            json.addProperty("fpsRussian", MyCustomScreen.fpsRussian);
            json.addProperty("pingRussian", MyCustomScreen.pingRussian);
            json.addProperty("tpsRussian", MyCustomScreen.tpsRussian);
            json.addProperty("bpsRussian", MyCustomScreen.bpsRussian);
            json.addProperty("directionRussian", MyCustomScreen.directionRussian);
            json.addProperty("hitCounterRussian", MyCustomScreen.hitCounterRussian);
            json.addProperty("potionEffectsRussian", MyCustomScreen.potionEffectsRussian);
            json.addProperty("equipmentHudRussian", MyCustomScreen.equipmentHudRussian);
            json.addProperty("lowFireShieldRussian", MyCustomScreen.lowFireShieldRussian);
            json.addProperty("zoomRussian", MyCustomScreen.zoomRussian);
            json.addProperty("autoSwapRussian", MyCustomScreen.autoSwapRussian);
            json.addProperty("fastExpRussian", MyCustomScreen.fastExpRussian);
            json.addProperty("autoSprintRussian", MyCustomScreen.autoSprintRussian);
            json.addProperty("shiftTapRussian", MyCustomScreen.shiftTapRussian);

            // === TAPEMOUSE ===
            json.addProperty("tapeMouseEnabled", MyCustomScreen.tapeMouseEnabled);
            json.addProperty("tapeMouseTarget", MyCustomScreen.tapeMouseTarget);
            json.addProperty("tapeMouseDelay", MyCustomScreen.tapeMouseDelay);
            json.addProperty("tapeMouseRussian", MyCustomScreen.tapeMouseRussian);
            json.addProperty("tapeMouseRequireTarget", MyCustomScreen.tapeMouseRequireTarget);
            json.addProperty("tapeMouseRequireFullAttack", MyCustomScreen.tapeMouseRequireFullAttack);

            // === AUTOSWAP ===
            json.addProperty("autoSwapEnabled", MyCustomScreen.autoSwapEnabled);
            json.addProperty("autoSwapMode", MyCustomScreen.autoSwapMode);
            json.addProperty("autoSwapOpenDelay", MyCustomScreen.autoSwapOpenDelay);
            json.addProperty("autoSwapCooldown", MyCustomScreen.autoSwapCooldown);

            // === FASTEXP ===
            json.addProperty("fastExpEnabled", MyCustomScreen.fastExpEnabled);

            // === AUTOSPRINT / SHIFTTAP ===
            json.addProperty("autoSprintEnabled", MyCustomScreen.autoSprintEnabled);
            json.addProperty("shiftTapEnabled", MyCustomScreen.shiftTapEnabled);

            // === ZOOM ===
            json.addProperty("zoomEnabled", MyCustomScreen.zoomEnabled);
            json.addProperty("zoomFactor", MyCustomScreen.zoomFactor);
            json.addProperty("zoomSmoothness", MyCustomScreen.zoomSmoothness);

            // === ASPECT RATIO ===
            json.addProperty("aspectRatioEnabled", MyCustomScreen.aspectRatioEnabled);
            json.addProperty("aspectRatio", MyCustomScreen.aspectRatio);
            json.addProperty("aspectRatioRussian", MyCustomScreen.aspectRatioRussian);

            // === CUSTOM HIT SOUNDS ===
            json.addProperty("customHitSoundsEnabled", MyCustomScreen.customHitSoundsEnabled);
            json.addProperty("customHitSoundVolume", MyCustomScreen.customHitSoundVolume);
            json.addProperty("customHitSoundPitch", MyCustomScreen.customHitSoundPitch);
            json.addProperty("customHitSoundsRussian", MyCustomScreen.customHitSoundsRussian);
            json.addProperty("customHitSoundPreset", MyCustomScreen.customHitSoundPreset);

            // === LOW FIRE / LOW SHIELD ===
            json.addProperty("lowFireOffset", MyCustomScreen.lowFireOffset);
            json.addProperty("lowShieldOffset", MyCustomScreen.lowShieldOffset);

            Path file = CONFIG_DIR.resolve(name + ".json");
            Files.writeString(file, GSON.toJson(json));
            return true;

        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("Не удалось сохранить конфиг " + name + ": " + e.getMessage());
            return false;
        }
    }
    public static void load() {
        loadFrom("default");
    }

    public static boolean loadFrom(String name) {
        Path file = CONFIG_DIR.resolve(name + ".json");

        if (!Files.exists(file)) {
            return false;
        }

        try {
            String content = Files.readString(file);
            JsonObject json = GSON.fromJson(content, JsonObject.class);

            if (json == null) return false;

            // === ОСНОВНЫЕ НАСТРОЙКИ ===
            if (json.has("showHud")) MyCustomScreen.showHud = json.get("showHud").getAsBoolean();
            if (json.has("hudColor")) MyCustomScreen.hudColor = json.get("hudColor").getAsInt();
            if (json.has("hudBackgroundEnabled")) MyCustomScreen.hudBackgroundEnabled = json.get("hudBackgroundEnabled").getAsBoolean();
            if (json.has("hudBackgroundAlpha")) MyCustomScreen.hudBackgroundAlpha = json.get("hudBackgroundAlpha").getAsInt();
            if (json.has("hudBackgroundColor")) MyCustomScreen.hudBackgroundColor = json.get("hudBackgroundColor").getAsInt();
            if (json.has("hudBackgroundHeight")) MyCustomScreen.hudBackgroundHeight = json.get("hudBackgroundHeight").getAsInt();
            if (json.has("guiColor")) MyCustomScreen.guiColor = json.get("guiColor").getAsInt();
            if (json.has("guiTextColor")) MyCustomScreen.guiTextColor = json.get("guiTextColor").getAsInt();
            if (json.has("hudAlpha")) MyCustomScreen.hudAlpha = json.get("hudAlpha").getAsInt();
            if (json.has("showPet")) MyCustomScreen.showPet = json.get("showPet").getAsBoolean();
            if (json.has("searchHistoryRaw")) MyCustomScreen.searchHistoryRaw = json.get("searchHistoryRaw").getAsString();

            // === ВИДИМОСТЬ ЭЛЕМЕНТОВ ===
            if (json.has("showCoords")) MyCustomScreen.showCoords = json.get("showCoords").getAsBoolean();
            if (json.has("showBiome")) MyCustomScreen.showBiome = json.get("showBiome").getAsBoolean();
            if (json.has("showTime")) MyCustomScreen.showTime = json.get("showTime").getAsBoolean();
            if (json.has("showFps")) MyCustomScreen.showFps = json.get("showFps").getAsBoolean();
            if (json.has("showPing")) MyCustomScreen.showPing = json.get("showPing").getAsBoolean();
            if (json.has("showTps")) MyCustomScreen.showTps = json.get("showTps").getAsBoolean();
            if (json.has("showBps")) MyCustomScreen.showBps = json.get("showBps").getAsBoolean();
            if (json.has("showDirection")) MyCustomScreen.showDirection = json.get("showDirection").getAsBoolean();
            if (json.has("showHitCounter")) MyCustomScreen.showHitCounter = json.get("showHitCounter").getAsBoolean();
            if (json.has("showPotionEffects")) MyCustomScreen.showPotionEffects = json.get("showPotionEffects").getAsBoolean();
            if (json.has("potionEffectsIcons")) MyCustomScreen.potionEffectsIcons = json.get("potionEffectsIcons").getAsBoolean();
            if (json.has("showEquipmentHud")) MyCustomScreen.showEquipmentHud = json.get("showEquipmentHud").getAsBoolean();
            if (json.has("equipmentShowDurability")) MyCustomScreen.equipmentShowDurability = json.get("equipmentShowDurability").getAsBoolean();
            if (json.has("lowFireEnabled")) MyCustomScreen.lowFireEnabled = json.get("lowFireEnabled").getAsBoolean();
            if (json.has("lowShieldEnabled")) MyCustomScreen.lowShieldEnabled = json.get("lowShieldEnabled").getAsBoolean();

            // === ПОЗИЦИИ ===
            if (json.has("coordsX")) MyCustomScreen.coordsX = json.get("coordsX").getAsInt();
            if (json.has("coordsY")) MyCustomScreen.coordsY = json.get("coordsY").getAsInt();
            if (json.has("biomeX")) MyCustomScreen.biomeX = json.get("biomeX").getAsInt();
            if (json.has("biomeY")) MyCustomScreen.biomeY = json.get("biomeY").getAsInt();
            if (json.has("timeX")) MyCustomScreen.timeX = json.get("timeX").getAsInt();
            if (json.has("timeY")) MyCustomScreen.timeY = json.get("timeY").getAsInt();
            if (json.has("fpsX")) MyCustomScreen.fpsX = json.get("fpsX").getAsInt();
            if (json.has("fpsY")) MyCustomScreen.fpsY = json.get("fpsY").getAsInt();
            if (json.has("pingX")) MyCustomScreen.pingX = json.get("pingX").getAsInt();
            if (json.has("pingY")) MyCustomScreen.pingY = json.get("pingY").getAsInt();
            if (json.has("tpsX")) MyCustomScreen.tpsX = json.get("tpsX").getAsInt();
            if (json.has("tpsY")) MyCustomScreen.tpsY = json.get("tpsY").getAsInt();
            if (json.has("bpsX")) MyCustomScreen.bpsX = json.get("bpsX").getAsInt();
            if (json.has("bpsY")) MyCustomScreen.bpsY = json.get("bpsY").getAsInt();
            if (json.has("directionX")) MyCustomScreen.directionX = json.get("directionX").getAsInt();
            if (json.has("directionY")) MyCustomScreen.directionY = json.get("directionY").getAsInt();
            if (json.has("hitCounterX")) MyCustomScreen.hitCounterX = json.get("hitCounterX").getAsInt();
            if (json.has("hitCounterY")) MyCustomScreen.hitCounterY = json.get("hitCounterY").getAsInt();
            if (json.has("potionEffectsX")) MyCustomScreen.potionEffectsX = json.get("potionEffectsX").getAsInt();
            if (json.has("potionEffectsY")) MyCustomScreen.potionEffectsY = json.get("potionEffectsY").getAsInt();
            if (json.has("equipmentHudX")) MyCustomScreen.equipmentHudX = json.get("equipmentHudX").getAsInt();
            if (json.has("equipmentHudY")) MyCustomScreen.equipmentHudY = json.get("equipmentHudY").getAsInt();

            // === ПЕРЕВОД ===
            if (json.has("fpsRussian")) MyCustomScreen.fpsRussian = json.get("fpsRussian").getAsBoolean();
            if (json.has("pingRussian")) MyCustomScreen.pingRussian = json.get("pingRussian").getAsBoolean();
            if (json.has("tpsRussian")) MyCustomScreen.tpsRussian = json.get("tpsRussian").getAsBoolean();
            if (json.has("bpsRussian")) MyCustomScreen.bpsRussian = json.get("bpsRussian").getAsBoolean();
            if (json.has("directionRussian")) MyCustomScreen.directionRussian = json.get("directionRussian").getAsBoolean();
            if (json.has("hitCounterRussian")) MyCustomScreen.hitCounterRussian = json.get("hitCounterRussian").getAsBoolean();
            if (json.has("potionEffectsRussian")) MyCustomScreen.potionEffectsRussian = json.get("potionEffectsRussian").getAsBoolean();
            if (json.has("equipmentHudRussian")) MyCustomScreen.equipmentHudRussian = json.get("equipmentHudRussian").getAsBoolean();
            if (json.has("lowFireShieldRussian")) MyCustomScreen.lowFireShieldRussian = json.get("lowFireShieldRussian").getAsBoolean();
            if (json.has("zoomRussian")) MyCustomScreen.zoomRussian = json.get("zoomRussian").getAsBoolean();
            if (json.has("autoSwapRussian")) MyCustomScreen.autoSwapRussian = json.get("autoSwapRussian").getAsBoolean();
            if (json.has("fastExpRussian")) MyCustomScreen.fastExpRussian = json.get("fastExpRussian").getAsBoolean();
            if (json.has("autoSprintRussian")) MyCustomScreen.autoSprintRussian = json.get("autoSprintRussian").getAsBoolean();
            if (json.has("shiftTapRussian")) MyCustomScreen.shiftTapRussian = json.get("shiftTapRussian").getAsBoolean();

            // === TAPEMOUSE ===
            if (json.has("tapeMouseEnabled")) MyCustomScreen.tapeMouseEnabled = json.get("tapeMouseEnabled").getAsBoolean();
            if (json.has("tapeMouseTarget")) MyCustomScreen.tapeMouseTarget = json.get("tapeMouseTarget").getAsInt();
            if (json.has("tapeMouseDelay")) MyCustomScreen.tapeMouseDelay = json.get("tapeMouseDelay").getAsFloat();
            if (json.has("tapeMouseRussian")) MyCustomScreen.tapeMouseRussian = json.get("tapeMouseRussian").getAsBoolean();
            if (json.has("tapeMouseRequireTarget")) MyCustomScreen.tapeMouseRequireTarget = json.get("tapeMouseRequireTarget").getAsBoolean();
            if (json.has("tapeMouseRequireFullAttack")) MyCustomScreen.tapeMouseRequireFullAttack = json.get("tapeMouseRequireFullAttack").getAsBoolean();

            // === AUTOSWAP ===
            if (json.has("autoSwapEnabled")) MyCustomScreen.autoSwapEnabled = json.get("autoSwapEnabled").getAsBoolean();
            if (json.has("autoSwapMode")) MyCustomScreen.autoSwapMode = json.get("autoSwapMode").getAsInt();
            if (json.has("autoSwapOpenDelay")) MyCustomScreen.autoSwapOpenDelay = json.get("autoSwapOpenDelay").getAsInt();
            if (json.has("autoSwapCooldown")) MyCustomScreen.autoSwapCooldown = json.get("autoSwapCooldown").getAsInt();

            // === FASTEXP ===
            if (json.has("fastExpEnabled")) MyCustomScreen.fastExpEnabled = json.get("fastExpEnabled").getAsBoolean();

            // === AUTOSPRINT / SHIFTTAP ===
            if (json.has("autoSprintEnabled")) MyCustomScreen.autoSprintEnabled = json.get("autoSprintEnabled").getAsBoolean();
            if (json.has("shiftTapEnabled")) MyCustomScreen.shiftTapEnabled = json.get("shiftTapEnabled").getAsBoolean();

            // === ZOOM ===
            if (json.has("zoomEnabled")) MyCustomScreen.zoomEnabled = json.get("zoomEnabled").getAsBoolean();
            if (json.has("zoomFactor")) MyCustomScreen.zoomFactor = json.get("zoomFactor").getAsFloat();
            if (json.has("zoomSmoothness")) MyCustomScreen.zoomSmoothness = json.get("zoomSmoothness").getAsFloat();

            // === ASPECT RATIO ===
            if (json.has("aspectRatioEnabled")) MyCustomScreen.aspectRatioEnabled = json.get("aspectRatioEnabled").getAsBoolean();
            if (json.has("aspectRatio")) MyCustomScreen.aspectRatio = json.get("aspectRatio").getAsFloat();
            if (json.has("aspectRatioRussian")) MyCustomScreen.aspectRatioRussian = json.get("aspectRatioRussian").getAsBoolean();

            // === CUSTOM HIT SOUNDS ===
            if (json.has("customHitSoundsEnabled")) MyCustomScreen.customHitSoundsEnabled = json.get("customHitSoundsEnabled").getAsBoolean();
            if (json.has("customHitSoundVolume")) MyCustomScreen.customHitSoundVolume = json.get("customHitSoundVolume").getAsFloat();
            if (json.has("customHitSoundPitch")) MyCustomScreen.customHitSoundPitch = json.get("customHitSoundPitch").getAsFloat();
            if (json.has("customHitSoundsRussian")) MyCustomScreen.customHitSoundsRussian = json.get("customHitSoundsRussian").getAsBoolean();
            if (json.has("customHitSoundPreset")) MyCustomScreen.customHitSoundPreset = json.get("customHitSoundPreset").getAsInt();

            // === LOW FIRE / LOW SHIELD ===
            if (json.has("lowFireOffset")) MyCustomScreen.lowFireOffset = json.get("lowFireOffset").getAsFloat();
            if (json.has("lowShieldOffset")) MyCustomScreen.lowShieldOffset = json.get("lowShieldOffset").getAsFloat();

            return true;

        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("Не удалось загрузить конфиг " + name + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean remove(String name) {
        Path file = CONFIG_DIR.resolve(name + ".json");
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("Не удалось удалить конфиг " + name + ": " + e.getMessage());
            return false;
        }
    }

    public static List<String> listConfigs() {
        List<String> names = new ArrayList<>();
        if (!Files.exists(CONFIG_DIR)) return names;

        try (Stream<Path> stream = Files.list(CONFIG_DIR)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        names.add(fileName.substring(0, fileName.length() - 5));
                    });
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("Не удалось прочитать список конфигов: " + e.getMessage());
        }
        return names;
    }

    public static void openFolder() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            String os = System.getProperty("os.name").toLowerCase();
            String path = CONFIG_DIR.toAbsolutePath().toString();

            if (os.contains("win")) {
                Runtime.getRuntime().exec("explorer.exe \"" + path + "\"");
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open \"" + path + "\"");
            } else {
                Runtime.getRuntime().exec("xdg-open \"" + path + "\"");
            }
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("Не удалось открыть папку конфигов: " + e.getMessage());
        }
    }

    public static String getConfigDirPath() {
        return CONFIG_DIR.toAbsolutePath().toString();
    }
}