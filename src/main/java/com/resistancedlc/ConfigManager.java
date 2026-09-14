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

    private static final long SAVE_DEBOUNCE_MS = 500;
    private static boolean savePending = false;
    private static long lastSaveRequest = 0;

    public static void save() {
        savePending = true;
        lastSaveRequest = System.currentTimeMillis();
    }

    public static void saveNow() {
        savePending = false;
        saveAs("default");
    }

    public static void tick() {
        if (!savePending) return;
        if (System.currentTimeMillis() - lastSaveRequest < SAVE_DEBOUNCE_MS) return;
        savePending = false;
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
            json.addProperty("showModLogo", MyCustomScreen.showModLogo);
            json.addProperty("modLogoX", MyCustomScreen.modLogoX);
            json.addProperty("modLogoY", MyCustomScreen.modLogoY);
            json.addProperty("modLogoRussian", MyCustomScreen.modLogoRussian);
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
            json.addProperty("tapeMouseButton", MyCustomScreen.tapeMouseButton);
            json.addProperty("tapeMouseHoldRight", MyCustomScreen.tapeMouseHoldRight);

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

            // === COMBO COUNTER ===
            json.addProperty("comboEnabled", MyCustomScreen.comboEnabled);
            json.addProperty("comboX", MyCustomScreen.comboX);
            json.addProperty("comboY", MyCustomScreen.comboY);
            json.addProperty("comboColor", MyCustomScreen.comboColor);
            json.addProperty("comboResetTime", MyCustomScreen.comboResetTime);
            json.addProperty("comboFontSize", MyCustomScreen.comboFontSize);
            json.addProperty("comboRussian", MyCustomScreen.comboRussian);

            // === EFFECT WARNINGS ===
            json.addProperty("effectWarningsEnabled", MyCustomScreen.effectWarningsEnabled);
            json.addProperty("effectWarningsX", MyCustomScreen.effectWarningsX);
            json.addProperty("effectWarningsY", MyCustomScreen.effectWarningsY);
            json.addProperty("effectWarningsColor", MyCustomScreen.effectWarningsColor);
            json.addProperty("effectWarningsThreshold", MyCustomScreen.effectWarningsThreshold);
            json.addProperty("effectWarningsAlpha", MyCustomScreen.effectWarningsAlpha);
            json.addProperty("effectWarningsShowName", MyCustomScreen.effectWarningsShowName);
            json.addProperty("effectWarningsShowIcon", MyCustomScreen.effectWarningsShowIcon);
            json.addProperty("effectWarningsRussian", MyCustomScreen.effectWarningsRussian);

            // === CROSSHAIR ===
            json.addProperty("crosshairEnabled", MyCustomScreen.crosshairEnabled);
            json.addProperty("crosshairColor", MyCustomScreen.crosshairColor);
            json.addProperty("crosshairSize", MyCustomScreen.crosshairSize);
            json.addProperty("crosshairThickness", MyCustomScreen.crosshairThickness);
            json.addProperty("crosshairGap", MyCustomScreen.crosshairGap);
            json.addProperty("crosshairAlpha", MyCustomScreen.crosshairAlpha);
            json.addProperty("crosshairRussian", MyCustomScreen.crosshairRussian);
            json.addProperty("crosshairShape", MyCustomScreen.crosshairShape);

            // === WAYPOINTS ===
            json.addProperty("waypointsRaw", MyCustomScreen.waypointsRaw);
            json.addProperty("waypointsMax", MyCustomScreen.waypointsMax);
            json.addProperty("waypointsRussian", MyCustomScreen.waypointsRussian);
            json.addProperty("waypointsEnabled", MyCustomScreen.waypointsEnabled);

            // === TOTEM LOG ===
            json.addProperty("totemLogEnabled", MyCustomScreen.totemLogEnabled);
            json.addProperty("totemLogRadius", MyCustomScreen.totemLogRadius);
            json.addProperty("totemLogSound", MyCustomScreen.totemLogSound);
            json.addProperty("totemLogRussian", MyCustomScreen.totemLogRussian);

            // === CAMERA ===
            json.addProperty("noHurtCamEnabled", MyCustomScreen.noHurtCamEnabled);
            json.addProperty("noBobbingEnabled", MyCustomScreen.noBobbingEnabled);
            json.addProperty("cameraRussian", MyCustomScreen.cameraRussian);

            // === SHULKER PEEK ===
            json.addProperty("shulkerPeekEnabled", MyCustomScreen.shulkerPeekEnabled);
            json.addProperty("shulkerPeekRequireShift", MyCustomScreen.shulkerPeekRequireShift);
            json.addProperty("shulkerPeekShowTitle", MyCustomScreen.shulkerPeekShowTitle);
            json.addProperty("shulkerPeekShowCounts", MyCustomScreen.shulkerPeekShowCounts);
            json.addProperty("shulkerPeekRussian", MyCustomScreen.shulkerPeekRussian);

            // === PVP SAFE ===
            json.addProperty("pvpSafeEnabled", MyCustomScreen.pvpSafeEnabled);
            json.addProperty("pvpSafeTimer", MyCustomScreen.pvpSafeTimer);
            json.addProperty("pvpSafeBlockQuit", MyCustomScreen.pvpSafeBlockQuit);
            json.addProperty("pvpSafeBlockCommands", MyCustomScreen.pvpSafeBlockCommands);
            json.addProperty("pvpSafeShowHud", MyCustomScreen.pvpSafeShowHud);
            json.addProperty("pvpSafeHudX", MyCustomScreen.pvpSafeHudX);
            json.addProperty("pvpSafeHudY", MyCustomScreen.pvpSafeHudY);
            json.addProperty("pvpSafeHudColor", MyCustomScreen.pvpSafeHudColor);
            json.addProperty("pvpSafeRussian", MyCustomScreen.pvpSafeRussian);

            // === PICKUP LOGGER ===
            json.addProperty("pickupLogEnabled", MyCustomScreen.pickupLogEnabled);
            json.addProperty("pickupLogMode", MyCustomScreen.pickupLogMode);
            json.addProperty("pickupLogWeapon", MyCustomScreen.pickupLogWeapon);
            json.addProperty("pickupLogArmor", MyCustomScreen.pickupLogArmor);
            json.addProperty("pickupLogPotions", MyCustomScreen.pickupLogPotions);
            json.addProperty("pickupLogTotems", MyCustomScreen.pickupLogTotems);
            json.addProperty("pickupLogHeads", MyCustomScreen.pickupLogHeads);
            json.addProperty("pickupLogSpawners", MyCustomScreen.pickupLogSpawners);
            json.addProperty("pickupLogStructureBlocks", MyCustomScreen.pickupLogStructureBlocks);
            json.addProperty("pickupLogRussian", MyCustomScreen.pickupLogRussian);

            Path file = CONFIG_DIR.resolve(name + ".json");
            synchronized (GSON) {
                Files.writeString(file, GSON.toJson(json));
            }
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
            MyCustomScreen.showHud = getBool(json, "showHud", MyCustomScreen.showHud);
            MyCustomScreen.hudColor = getInt(json, "hudColor", MyCustomScreen.hudColor);
            MyCustomScreen.showModLogo = getBool(json, "showModLogo", MyCustomScreen.showModLogo);
            MyCustomScreen.modLogoX = getInt(json, "modLogoX", MyCustomScreen.modLogoX);
            MyCustomScreen.modLogoY = getInt(json, "modLogoY", MyCustomScreen.modLogoY);
            MyCustomScreen.modLogoRussian = getBool(json, "modLogoRussian", MyCustomScreen.modLogoRussian);
            MyCustomScreen.hudBackgroundEnabled = getBool(json, "hudBackgroundEnabled", MyCustomScreen.hudBackgroundEnabled);
            MyCustomScreen.hudBackgroundAlpha = getInt(json, "hudBackgroundAlpha", MyCustomScreen.hudBackgroundAlpha);
            MyCustomScreen.hudBackgroundColor = getInt(json, "hudBackgroundColor", MyCustomScreen.hudBackgroundColor);
            MyCustomScreen.hudBackgroundHeight = getInt(json, "hudBackgroundHeight", MyCustomScreen.hudBackgroundHeight);
            MyCustomScreen.guiColor = getInt(json, "guiColor", MyCustomScreen.guiColor);
            MyCustomScreen.guiTextColor = getInt(json, "guiTextColor", MyCustomScreen.guiTextColor);
            MyCustomScreen.hudAlpha = getInt(json, "hudAlpha", MyCustomScreen.hudAlpha);
            MyCustomScreen.showPet = getBool(json, "showPet", MyCustomScreen.showPet);
            MyCustomScreen.searchHistoryRaw = getString(json, "searchHistoryRaw", MyCustomScreen.searchHistoryRaw);

            // === ВИДИМОСТЬ ЭЛЕМЕНТОВ ===
            MyCustomScreen.showCoords = getBool(json, "showCoords", MyCustomScreen.showCoords);
            MyCustomScreen.showBiome = getBool(json, "showBiome", MyCustomScreen.showBiome);
            MyCustomScreen.showTime = getBool(json, "showTime", MyCustomScreen.showTime);
            MyCustomScreen.showFps = getBool(json, "showFps", MyCustomScreen.showFps);
            MyCustomScreen.showPing = getBool(json, "showPing", MyCustomScreen.showPing);
            MyCustomScreen.showTps = getBool(json, "showTps", MyCustomScreen.showTps);
            MyCustomScreen.showBps = getBool(json, "showBps", MyCustomScreen.showBps);
            MyCustomScreen.showDirection = getBool(json, "showDirection", MyCustomScreen.showDirection);
            MyCustomScreen.showHitCounter = getBool(json, "showHitCounter", MyCustomScreen.showHitCounter);
            MyCustomScreen.showPotionEffects = getBool(json, "showPotionEffects", MyCustomScreen.showPotionEffects);
            MyCustomScreen.potionEffectsIcons = getBool(json, "potionEffectsIcons", MyCustomScreen.potionEffectsIcons);
            MyCustomScreen.showEquipmentHud = getBool(json, "showEquipmentHud", MyCustomScreen.showEquipmentHud);
            MyCustomScreen.equipmentShowDurability = getBool(json, "equipmentShowDurability", MyCustomScreen.equipmentShowDurability);
            MyCustomScreen.lowFireEnabled = getBool(json, "lowFireEnabled", MyCustomScreen.lowFireEnabled);
            MyCustomScreen.lowShieldEnabled = getBool(json, "lowShieldEnabled", MyCustomScreen.lowShieldEnabled);

            // === ПОЗИЦИИ ===
            MyCustomScreen.coordsX = getInt(json, "coordsX", MyCustomScreen.coordsX);
            MyCustomScreen.coordsY = getInt(json, "coordsY", MyCustomScreen.coordsY);
            MyCustomScreen.biomeX = getInt(json, "biomeX", MyCustomScreen.biomeX);
            MyCustomScreen.biomeY = getInt(json, "biomeY", MyCustomScreen.biomeY);
            MyCustomScreen.timeX = getInt(json, "timeX", MyCustomScreen.timeX);
            MyCustomScreen.timeY = getInt(json, "timeY", MyCustomScreen.timeY);
            MyCustomScreen.fpsX = getInt(json, "fpsX", MyCustomScreen.fpsX);
            MyCustomScreen.fpsY = getInt(json, "fpsY", MyCustomScreen.fpsY);
            MyCustomScreen.pingX = getInt(json, "pingX", MyCustomScreen.pingX);
            MyCustomScreen.pingY = getInt(json, "pingY", MyCustomScreen.pingY);
            MyCustomScreen.tpsX = getInt(json, "tpsX", MyCustomScreen.tpsX);
            MyCustomScreen.tpsY = getInt(json, "tpsY", MyCustomScreen.tpsY);
            MyCustomScreen.bpsX = getInt(json, "bpsX", MyCustomScreen.bpsX);
            MyCustomScreen.bpsY = getInt(json, "bpsY", MyCustomScreen.bpsY);
            MyCustomScreen.directionX = getInt(json, "directionX", MyCustomScreen.directionX);
            MyCustomScreen.directionY = getInt(json, "directionY", MyCustomScreen.directionY);
            MyCustomScreen.hitCounterX = getInt(json, "hitCounterX", MyCustomScreen.hitCounterX);
            MyCustomScreen.hitCounterY = getInt(json, "hitCounterY", MyCustomScreen.hitCounterY);
            MyCustomScreen.potionEffectsX = getInt(json, "potionEffectsX", MyCustomScreen.potionEffectsX);
            MyCustomScreen.potionEffectsY = getInt(json, "potionEffectsY", MyCustomScreen.potionEffectsY);
            MyCustomScreen.equipmentHudX = getInt(json, "equipmentHudX", MyCustomScreen.equipmentHudX);
            MyCustomScreen.equipmentHudY = getInt(json, "equipmentHudY", MyCustomScreen.equipmentHudY);

            // === ПЕРЕВОД ===
            MyCustomScreen.fpsRussian = getBool(json, "fpsRussian", MyCustomScreen.fpsRussian);
            MyCustomScreen.pingRussian = getBool(json, "pingRussian", MyCustomScreen.pingRussian);
            MyCustomScreen.tpsRussian = getBool(json, "tpsRussian", MyCustomScreen.tpsRussian);
            MyCustomScreen.bpsRussian = getBool(json, "bpsRussian", MyCustomScreen.bpsRussian);
            MyCustomScreen.directionRussian = getBool(json, "directionRussian", MyCustomScreen.directionRussian);
            MyCustomScreen.hitCounterRussian = getBool(json, "hitCounterRussian", MyCustomScreen.hitCounterRussian);
            MyCustomScreen.potionEffectsRussian = getBool(json, "potionEffectsRussian", MyCustomScreen.potionEffectsRussian);
            MyCustomScreen.equipmentHudRussian = getBool(json, "equipmentHudRussian", MyCustomScreen.equipmentHudRussian);
            MyCustomScreen.lowFireShieldRussian = getBool(json, "lowFireShieldRussian", MyCustomScreen.lowFireShieldRussian);
            MyCustomScreen.zoomRussian = getBool(json, "zoomRussian", MyCustomScreen.zoomRussian);
            MyCustomScreen.autoSwapRussian = getBool(json, "autoSwapRussian", MyCustomScreen.autoSwapRussian);
            MyCustomScreen.fastExpRussian = getBool(json, "fastExpRussian", MyCustomScreen.fastExpRussian);
            MyCustomScreen.autoSprintRussian = getBool(json, "autoSprintRussian", MyCustomScreen.autoSprintRussian);
            MyCustomScreen.shiftTapRussian = getBool(json, "shiftTapRussian", MyCustomScreen.shiftTapRussian);

            // === TAPEMOUSE ===
            MyCustomScreen.tapeMouseEnabled = getBool(json, "tapeMouseEnabled", MyCustomScreen.tapeMouseEnabled);
            MyCustomScreen.tapeMouseTarget = getInt(json, "tapeMouseTarget", MyCustomScreen.tapeMouseTarget);
            MyCustomScreen.tapeMouseDelay = getFloat(json, "tapeMouseDelay", MyCustomScreen.tapeMouseDelay);
            MyCustomScreen.tapeMouseRussian = getBool(json, "tapeMouseRussian", MyCustomScreen.tapeMouseRussian);
            MyCustomScreen.tapeMouseRequireTarget = getBool(json, "tapeMouseRequireTarget", MyCustomScreen.tapeMouseRequireTarget);
            MyCustomScreen.tapeMouseRequireFullAttack = getBool(json, "tapeMouseRequireFullAttack", MyCustomScreen.tapeMouseRequireFullAttack);
            MyCustomScreen.tapeMouseButton = getInt(json, "tapeMouseButton", MyCustomScreen.tapeMouseButton);
            MyCustomScreen.tapeMouseHoldRight = getBool(json, "tapeMouseHoldRight", MyCustomScreen.tapeMouseHoldRight);

            // === AUTOSWAP ===
            MyCustomScreen.autoSwapEnabled = getBool(json, "autoSwapEnabled", MyCustomScreen.autoSwapEnabled);
            MyCustomScreen.autoSwapMode = getInt(json, "autoSwapMode", MyCustomScreen.autoSwapMode);
            MyCustomScreen.autoSwapOpenDelay = getInt(json, "autoSwapOpenDelay", MyCustomScreen.autoSwapOpenDelay);
            MyCustomScreen.autoSwapCooldown = getInt(json, "autoSwapCooldown", MyCustomScreen.autoSwapCooldown);

            // === FASTEXP ===
            MyCustomScreen.fastExpEnabled = getBool(json, "fastExpEnabled", MyCustomScreen.fastExpEnabled);

            // === AUTOSPRINT / SHIFTTAP ===
            MyCustomScreen.autoSprintEnabled = getBool(json, "autoSprintEnabled", MyCustomScreen.autoSprintEnabled);
            MyCustomScreen.shiftTapEnabled = getBool(json, "shiftTapEnabled", MyCustomScreen.shiftTapEnabled);

            // === ZOOM ===
            MyCustomScreen.zoomEnabled = getBool(json, "zoomEnabled", MyCustomScreen.zoomEnabled);
            MyCustomScreen.zoomFactor = getFloat(json, "zoomFactor", MyCustomScreen.zoomFactor);
            MyCustomScreen.zoomSmoothness = getFloat(json, "zoomSmoothness", MyCustomScreen.zoomSmoothness);

            // === ASPECT RATIO ===
            MyCustomScreen.aspectRatioEnabled = getBool(json, "aspectRatioEnabled", MyCustomScreen.aspectRatioEnabled);
            MyCustomScreen.aspectRatio = getFloat(json, "aspectRatio", MyCustomScreen.aspectRatio);
            MyCustomScreen.aspectRatioRussian = getBool(json, "aspectRatioRussian", MyCustomScreen.aspectRatioRussian);

            // === CUSTOM HIT SOUNDS ===
            MyCustomScreen.customHitSoundsEnabled = getBool(json, "customHitSoundsEnabled", MyCustomScreen.customHitSoundsEnabled);
            MyCustomScreen.customHitSoundVolume = getFloat(json, "customHitSoundVolume", MyCustomScreen.customHitSoundVolume);
            MyCustomScreen.customHitSoundPitch = getFloat(json, "customHitSoundPitch", MyCustomScreen.customHitSoundPitch);
            MyCustomScreen.customHitSoundsRussian = getBool(json, "customHitSoundsRussian", MyCustomScreen.customHitSoundsRussian);
            MyCustomScreen.customHitSoundPreset = getInt(json, "customHitSoundPreset", MyCustomScreen.customHitSoundPreset);

            // === LOW FIRE / LOW SHIELD ===
            MyCustomScreen.lowFireOffset = getFloat(json, "lowFireOffset", MyCustomScreen.lowFireOffset);
            MyCustomScreen.lowShieldOffset = getFloat(json, "lowShieldOffset", MyCustomScreen.lowShieldOffset);

            // === COMBO COUNTER ===
            MyCustomScreen.comboEnabled = getBool(json, "comboEnabled", MyCustomScreen.comboEnabled);
            MyCustomScreen.comboX = getInt(json, "comboX", MyCustomScreen.comboX);
            MyCustomScreen.comboY = getInt(json, "comboY", MyCustomScreen.comboY);
            MyCustomScreen.comboColor = getInt(json, "comboColor", MyCustomScreen.comboColor);
            MyCustomScreen.comboResetTime = getInt(json, "comboResetTime", MyCustomScreen.comboResetTime);
            MyCustomScreen.comboFontSize = getInt(json, "comboFontSize", MyCustomScreen.comboFontSize);
            MyCustomScreen.comboRussian = getBool(json, "comboRussian", MyCustomScreen.comboRussian);

            // === EFFECT WARNINGS ===
            MyCustomScreen.effectWarningsEnabled = getBool(json, "effectWarningsEnabled", MyCustomScreen.effectWarningsEnabled);
            MyCustomScreen.effectWarningsX = getInt(json, "effectWarningsX", MyCustomScreen.effectWarningsX);
            MyCustomScreen.effectWarningsY = getInt(json, "effectWarningsY", MyCustomScreen.effectWarningsY);
            MyCustomScreen.effectWarningsColor = getInt(json, "effectWarningsColor", MyCustomScreen.effectWarningsColor);
            MyCustomScreen.effectWarningsThreshold = getInt(json, "effectWarningsThreshold", MyCustomScreen.effectWarningsThreshold);
            MyCustomScreen.effectWarningsAlpha = getInt(json, "effectWarningsAlpha", MyCustomScreen.effectWarningsAlpha);
            MyCustomScreen.effectWarningsShowName = getBool(json, "effectWarningsShowName", MyCustomScreen.effectWarningsShowName);
            MyCustomScreen.effectWarningsShowIcon = getBool(json, "effectWarningsShowIcon", MyCustomScreen.effectWarningsShowIcon);
            MyCustomScreen.effectWarningsRussian = getBool(json, "effectWarningsRussian", MyCustomScreen.effectWarningsRussian);

            // === CROSSHAIR ===
            MyCustomScreen.crosshairEnabled = getBool(json, "crosshairEnabled", MyCustomScreen.crosshairEnabled);
            MyCustomScreen.crosshairColor = getInt(json, "crosshairColor", MyCustomScreen.crosshairColor);
            MyCustomScreen.crosshairSize = getInt(json, "crosshairSize", MyCustomScreen.crosshairSize);
            MyCustomScreen.crosshairThickness = getInt(json, "crosshairThickness", MyCustomScreen.crosshairThickness);
            MyCustomScreen.crosshairGap = getInt(json, "crosshairGap", MyCustomScreen.crosshairGap);
            MyCustomScreen.crosshairAlpha = getInt(json, "crosshairAlpha", MyCustomScreen.crosshairAlpha);
            MyCustomScreen.crosshairRussian = getBool(json, "crosshairRussian", MyCustomScreen.crosshairRussian);
            MyCustomScreen.crosshairShape = getInt(json, "crosshairShape", MyCustomScreen.crosshairShape);

            // === WAYPOINTS ===
            MyCustomScreen.waypointsRaw = getString(json, "waypointsRaw", MyCustomScreen.waypointsRaw);
            MyCustomScreen.waypointsMax = getInt(json, "waypointsMax", MyCustomScreen.waypointsMax);
            MyCustomScreen.waypointsRussian = getBool(json, "waypointsRussian", MyCustomScreen.waypointsRussian);
            MyCustomScreen.waypointsEnabled = getBool(json, "waypointsEnabled", MyCustomScreen.waypointsEnabled);

            // === TOTEM LOG ===
            MyCustomScreen.totemLogEnabled = getBool(json, "totemLogEnabled", MyCustomScreen.totemLogEnabled);
            MyCustomScreen.totemLogRadius = getInt(json, "totemLogRadius", MyCustomScreen.totemLogRadius);
            MyCustomScreen.totemLogSound = getBool(json, "totemLogSound", MyCustomScreen.totemLogSound);
            MyCustomScreen.totemLogRussian = getBool(json, "totemLogRussian", MyCustomScreen.totemLogRussian);

            // === CAMERA ===
            MyCustomScreen.noHurtCamEnabled = getBool(json, "noHurtCamEnabled", MyCustomScreen.noHurtCamEnabled);
            MyCustomScreen.noBobbingEnabled = getBool(json, "noBobbingEnabled", MyCustomScreen.noBobbingEnabled);
            MyCustomScreen.cameraRussian = getBool(json, "cameraRussian", MyCustomScreen.cameraRussian);

            // === SHULKER PEEK ===
            MyCustomScreen.shulkerPeekEnabled = getBool(json, "shulkerPeekEnabled", MyCustomScreen.shulkerPeekEnabled);
            MyCustomScreen.shulkerPeekRequireShift = getBool(json, "shulkerPeekRequireShift", MyCustomScreen.shulkerPeekRequireShift);
            MyCustomScreen.shulkerPeekShowTitle = getBool(json, "shulkerPeekShowTitle", MyCustomScreen.shulkerPeekShowTitle);
            MyCustomScreen.shulkerPeekShowCounts = getBool(json, "shulkerPeekShowCounts", MyCustomScreen.shulkerPeekShowCounts);
            MyCustomScreen.shulkerPeekRussian = getBool(json, "shulkerPeekRussian", MyCustomScreen.shulkerPeekRussian);

            // === PVP SAFE ===
            MyCustomScreen.pvpSafeEnabled = getBool(json, "pvpSafeEnabled", MyCustomScreen.pvpSafeEnabled);
            MyCustomScreen.pvpSafeTimer = getInt(json, "pvpSafeTimer", MyCustomScreen.pvpSafeTimer);
            MyCustomScreen.pvpSafeBlockQuit = getBool(json, "pvpSafeBlockQuit", MyCustomScreen.pvpSafeBlockQuit);
            MyCustomScreen.pvpSafeBlockCommands = getBool(json, "pvpSafeBlockCommands", MyCustomScreen.pvpSafeBlockCommands);
            MyCustomScreen.pvpSafeShowHud = getBool(json, "pvpSafeShowHud", MyCustomScreen.pvpSafeShowHud);
            MyCustomScreen.pvpSafeHudX = getInt(json, "pvpSafeHudX", MyCustomScreen.pvpSafeHudX);
            MyCustomScreen.pvpSafeHudY = getInt(json, "pvpSafeHudY", MyCustomScreen.pvpSafeHudY);
            MyCustomScreen.pvpSafeHudColor = getInt(json, "pvpSafeHudColor", MyCustomScreen.pvpSafeHudColor);
            MyCustomScreen.pvpSafeRussian = getBool(json, "pvpSafeRussian", MyCustomScreen.pvpSafeRussian);

            // === PICKUP LOGGER ===
            MyCustomScreen.pickupLogEnabled = getBool(json, "pickupLogEnabled", MyCustomScreen.pickupLogEnabled);
            MyCustomScreen.pickupLogMode = getInt(json, "pickupLogMode", MyCustomScreen.pickupLogMode);
            MyCustomScreen.pickupLogWeapon = getBool(json, "pickupLogWeapon", MyCustomScreen.pickupLogWeapon);
            MyCustomScreen.pickupLogArmor = getBool(json, "pickupLogArmor", MyCustomScreen.pickupLogArmor);
            MyCustomScreen.pickupLogPotions = getBool(json, "pickupLogPotions", MyCustomScreen.pickupLogPotions);
            MyCustomScreen.pickupLogTotems = getBool(json, "pickupLogTotems", MyCustomScreen.pickupLogTotems);
            MyCustomScreen.pickupLogHeads = getBool(json, "pickupLogHeads", MyCustomScreen.pickupLogHeads);
            MyCustomScreen.pickupLogSpawners = getBool(json, "pickupLogSpawners", MyCustomScreen.pickupLogSpawners);
            MyCustomScreen.pickupLogStructureBlocks = getBool(json, "pickupLogStructureBlocks", MyCustomScreen.pickupLogStructureBlocks);
            MyCustomScreen.pickupLogRussian = getBool(json, "pickupLogRussian", MyCustomScreen.pickupLogRussian);

            return true;

        } catch (Exception e) {
            ResistanceDLC.LOGGER.error("Не удалось загрузить конфиг " + name + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean getBool(JsonObject json, String key, boolean def) {
        return json.has(key) ? json.get(key).getAsBoolean() : def;
    }

    private static int getInt(JsonObject json, String key, int def) {
        return json.has(key) ? json.get(key).getAsInt() : def;
    }

    private static float getFloat(JsonObject json, String key, float def) {
        return json.has(key) ? json.get(key).getAsFloat() : def;
    }

    private static String getString(JsonObject json, String key, String def) {
        return json.has(key) ? json.get(key).getAsString() : def;
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

            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("explorer.exe", path);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", path);
            } else {
                pb = new ProcessBuilder("xdg-open", path);
            }
            pb.start();
        } catch (IOException e) {
            ResistanceDLC.LOGGER.error("Не удалось открыть папку конфигов: " + e.getMessage());
        }
    }

    public static String getConfigDirPath() {
        return CONFIG_DIR.toAbsolutePath().toString();
    }
}