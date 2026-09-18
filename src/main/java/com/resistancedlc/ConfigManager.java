package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

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
            json.addProperty("showHud", ModConfig.showHud);
            json.addProperty("hudColor", ModConfig.hudColor);
            json.addProperty("showModLogo", ModConfig.showModLogo);
            json.addProperty("modLogoX", ModConfig.modLogoX);
            json.addProperty("modLogoY", ModConfig.modLogoY);
            json.addProperty("modLogoRussian", ModConfig.modLogoRussian);
            json.addProperty("hudBackgroundEnabled", ModConfig.hudBackgroundEnabled);
            json.addProperty("hudBackgroundAlpha", ModConfig.hudBackgroundAlpha);
            json.addProperty("hudBackgroundColor", ModConfig.hudBackgroundColor);
            json.addProperty("hudBackgroundHeight", ModConfig.hudBackgroundHeight);
            json.addProperty("guiColor", ModConfig.guiColor);
            json.addProperty("guiTextColor", ModConfig.guiTextColor);
            json.addProperty("hudAlpha", ModConfig.hudAlpha);
            json.addProperty("showPet", ModConfig.showPet);
            json.addProperty("searchHistoryRaw", ModConfig.searchHistoryRaw);

            // === ВИДИМОСТЬ ЭЛЕМЕНТОВ ===
            json.addProperty("showCoords", ModConfig.showCoords);
            json.addProperty("showBiome", ModConfig.showBiome);
            json.addProperty("showTime", ModConfig.showTime);
            json.addProperty("showFps", ModConfig.showFps);
            json.addProperty("showPing", ModConfig.showPing);
            json.addProperty("showTps", ModConfig.showTps);
            json.addProperty("showBps", ModConfig.showBps);
            json.addProperty("showDirection", ModConfig.showDirection);
            json.addProperty("showHitCounter", ModConfig.showHitCounter);
            json.addProperty("showPotionEffects", ModConfig.showPotionEffects);
            json.addProperty("potionEffectsIcons", ModConfig.potionEffectsIcons);
            json.addProperty("showEquipmentHud", ModConfig.showEquipmentHud);
            json.addProperty("equipmentShowDurability", ModConfig.equipmentShowDurability);
            json.addProperty("lowFireEnabled", ModConfig.lowFireEnabled);
            json.addProperty("lowShieldEnabled", ModConfig.lowShieldEnabled);

            // === ПОЗИЦИИ ===
            json.addProperty("coordsX", ModConfig.coordsX);
            json.addProperty("coordsY", ModConfig.coordsY);
            json.addProperty("biomeX", ModConfig.biomeX);
            json.addProperty("biomeY", ModConfig.biomeY);
            json.addProperty("timeX", ModConfig.timeX);
            json.addProperty("timeY", ModConfig.timeY);
            json.addProperty("fpsX", ModConfig.fpsX);
            json.addProperty("fpsY", ModConfig.fpsY);
            json.addProperty("pingX", ModConfig.pingX);
            json.addProperty("pingY", ModConfig.pingY);
            json.addProperty("tpsX", ModConfig.tpsX);
            json.addProperty("tpsY", ModConfig.tpsY);
            json.addProperty("bpsX", ModConfig.bpsX);
            json.addProperty("bpsY", ModConfig.bpsY);
            json.addProperty("directionX", ModConfig.directionX);
            json.addProperty("directionY", ModConfig.directionY);
            json.addProperty("hitCounterX", ModConfig.hitCounterX);
            json.addProperty("hitCounterY", ModConfig.hitCounterY);
            json.addProperty("potionEffectsX", ModConfig.potionEffectsX);
            json.addProperty("potionEffectsY", ModConfig.potionEffectsY);
            json.addProperty("equipmentHudX", ModConfig.equipmentHudX);
            json.addProperty("equipmentHudY", ModConfig.equipmentHudY);

            // === ПЕРЕВОД ===
            json.addProperty("fpsRussian", ModConfig.fpsRussian);
            json.addProperty("pingRussian", ModConfig.pingRussian);
            json.addProperty("tpsRussian", ModConfig.tpsRussian);
            json.addProperty("bpsRussian", ModConfig.bpsRussian);
            json.addProperty("directionRussian", ModConfig.directionRussian);
            json.addProperty("hitCounterRussian", ModConfig.hitCounterRussian);
            json.addProperty("potionEffectsRussian", ModConfig.potionEffectsRussian);
            json.addProperty("equipmentHudRussian", ModConfig.equipmentHudRussian);
            json.addProperty("lowFireShieldRussian", ModConfig.lowFireShieldRussian);
            json.addProperty("zoomRussian", ModConfig.zoomRussian);
            json.addProperty("autoSwapRussian", ModConfig.autoSwapRussian);
            json.addProperty("fastExpRussian", ModConfig.fastExpRussian);
            json.addProperty("autoSprintRussian", ModConfig.autoSprintRussian);
            json.addProperty("shiftTapRussian", ModConfig.shiftTapRussian);

            // === TAPEMOUSE ===
            json.addProperty("tapeMouseEnabled", ModConfig.tapeMouseEnabled);
            json.addProperty("tapeMouseTarget", ModConfig.tapeMouseTarget);
            json.addProperty("tapeMouseDelay", ModConfig.tapeMouseDelay);
            json.addProperty("tapeMouseRussian", ModConfig.tapeMouseRussian);
            json.addProperty("tapeMouseRequireTarget", ModConfig.tapeMouseRequireTarget);
            json.addProperty("tapeMouseRequireFullAttack", ModConfig.tapeMouseRequireFullAttack);
            json.addProperty("tapeMouseButton", ModConfig.tapeMouseButton);
            json.addProperty("tapeMouseHoldRight", ModConfig.tapeMouseHoldRight);

            // === AUTOSWAP ===
            json.addProperty("autoSwapEnabled", ModConfig.autoSwapEnabled);
            json.addProperty("autoSwapMode", ModConfig.autoSwapMode);
            json.addProperty("autoSwapOpenDelay", ModConfig.autoSwapOpenDelay);
            json.addProperty("autoSwapCooldown", ModConfig.autoSwapCooldown);

            // === FASTEXP ===
            json.addProperty("fastExpEnabled", ModConfig.fastExpEnabled);

            // === AUTOSPRINT / SHIFTTAP ===
            json.addProperty("autoSprintEnabled", ModConfig.autoSprintEnabled);
            json.addProperty("shiftTapEnabled", ModConfig.shiftTapEnabled);

            // === ZOOM ===
            json.addProperty("zoomEnabled", ModConfig.zoomEnabled);
            json.addProperty("zoomFactor", ModConfig.zoomFactor);
            json.addProperty("zoomSmoothness", ModConfig.zoomSmoothness);

            // === ASPECT RATIO ===
            json.addProperty("aspectRatioEnabled", ModConfig.aspectRatioEnabled);
            json.addProperty("aspectRatio", ModConfig.aspectRatio);
            json.addProperty("aspectRatioRussian", ModConfig.aspectRatioRussian);

            // === CUSTOM HIT SOUNDS ===
            json.addProperty("customHitSoundsEnabled", ModConfig.customHitSoundsEnabled);
            json.addProperty("customHitSoundVolume", ModConfig.customHitSoundVolume);
            json.addProperty("customHitSoundPitch", ModConfig.customHitSoundPitch);
            json.addProperty("customHitSoundsRussian", ModConfig.customHitSoundsRussian);
            json.addProperty("customHitSoundPreset", ModConfig.customHitSoundPreset);

            // === LOW FIRE / LOW SHIELD ===
            json.addProperty("lowFireOffset", ModConfig.lowFireOffset);
            json.addProperty("lowShieldOffset", ModConfig.lowShieldOffset);

            // === COMBO COUNTER ===
            json.addProperty("comboEnabled", ModConfig.comboEnabled);
            json.addProperty("comboX", ModConfig.comboX);
            json.addProperty("comboY", ModConfig.comboY);
            json.addProperty("comboColor", ModConfig.comboColor);
            json.addProperty("comboResetTime", ModConfig.comboResetTime);
            json.addProperty("comboFontSize", ModConfig.comboFontSize);
            json.addProperty("comboRussian", ModConfig.comboRussian);

            // === EFFECT WARNINGS ===
            json.addProperty("effectWarningsEnabled", ModConfig.effectWarningsEnabled);
            json.addProperty("effectWarningsX", ModConfig.effectWarningsX);
            json.addProperty("effectWarningsY", ModConfig.effectWarningsY);
            json.addProperty("effectWarningsColor", ModConfig.effectWarningsColor);
            json.addProperty("effectWarningsThreshold", ModConfig.effectWarningsThreshold);
            json.addProperty("effectWarningsAlpha", ModConfig.effectWarningsAlpha);
            json.addProperty("effectWarningsShowName", ModConfig.effectWarningsShowName);
            json.addProperty("effectWarningsShowIcon", ModConfig.effectWarningsShowIcon);
            json.addProperty("effectWarningsRussian", ModConfig.effectWarningsRussian);

            // === CROSSHAIR ===
            json.addProperty("crosshairEnabled", ModConfig.crosshairEnabled);
            json.addProperty("crosshairColor", ModConfig.crosshairColor);
            json.addProperty("crosshairSize", ModConfig.crosshairSize);
            json.addProperty("crosshairThickness", ModConfig.crosshairThickness);
            json.addProperty("crosshairGap", ModConfig.crosshairGap);
            json.addProperty("crosshairAlpha", ModConfig.crosshairAlpha);
            json.addProperty("crosshairRussian", ModConfig.crosshairRussian);
            json.addProperty("crosshairShape", ModConfig.crosshairShape);

            // === WAYPOINTS ===
            json.addProperty("waypointsRaw", ModConfig.waypointsRaw);
            json.addProperty("waypointsMax", ModConfig.waypointsMax);
            json.addProperty("waypointsRussian", ModConfig.waypointsRussian);
            json.addProperty("waypointsEnabled", ModConfig.waypointsEnabled);

            // === TOTEM LOG ===
            json.addProperty("totemLogEnabled", ModConfig.totemLogEnabled);
            json.addProperty("totemLogRadius", ModConfig.totemLogRadius);
            json.addProperty("totemLogSound", ModConfig.totemLogSound);
            json.addProperty("totemLogRussian", ModConfig.totemLogRussian);

            // === CAMERA ===
            json.addProperty("noHurtCamEnabled", ModConfig.noHurtCamEnabled);
            json.addProperty("noBobbingEnabled", ModConfig.noBobbingEnabled);
            json.addProperty("cameraRussian", ModConfig.cameraRussian);

            // === ITEM PHYSICS ===
            json.addProperty("itemPhysicsEnabled", ModConfig.itemPhysicsEnabled);
            json.addProperty("itemPhysicsRussian", ModConfig.itemPhysicsRussian);

            // === PARTICLE BLOCKER ===
            json.addProperty("particleBlockerEnabled", ModConfig.particleBlockerEnabled);
            json.addProperty("particleBlockerFire", ModConfig.particleBlockerFire);
            json.addProperty("particleBlockerSmoke", ModConfig.particleBlockerSmoke);
            json.addProperty("particleBlockerExplosion", ModConfig.particleBlockerExplosion);
            json.addProperty("particleBlockerPotions", ModConfig.particleBlockerPotions);
            json.addProperty("particleBlockerWater", ModConfig.particleBlockerWater);
            json.addProperty("particleBlockerRedstone", ModConfig.particleBlockerRedstone);
            json.addProperty("particleBlockerPortal", ModConfig.particleBlockerPortal);
            json.addProperty("particleBlockerCrit", ModConfig.particleBlockerCrit);
            json.addProperty("particleBlockerRussian", ModConfig.particleBlockerRussian);

            // === PVP SAFE ===
            json.addProperty("pvpSafeEnabled", ModConfig.pvpSafeEnabled);
            json.addProperty("pvpSafeTimer", ModConfig.pvpSafeTimer);
            json.addProperty("pvpSafeBlockQuit", ModConfig.pvpSafeBlockQuit);
            json.addProperty("pvpSafeBlockCommands", ModConfig.pvpSafeBlockCommands);
            json.addProperty("pvpSafeShowHud", ModConfig.pvpSafeShowHud);
            json.addProperty("pvpSafeHudX", ModConfig.pvpSafeHudX);
            json.addProperty("pvpSafeHudY", ModConfig.pvpSafeHudY);
            json.addProperty("pvpSafeHudColor", ModConfig.pvpSafeHudColor);
            json.addProperty("pvpSafeRussian", ModConfig.pvpSafeRussian);

            // === PICKUP LOGGER ===
            json.addProperty("pickupLogEnabled", ModConfig.pickupLogEnabled);
            json.addProperty("pickupLogMode", ModConfig.pickupLogMode);
            json.addProperty("pickupLogWeapon", ModConfig.pickupLogWeapon);
            json.addProperty("pickupLogArmor", ModConfig.pickupLogArmor);
            json.addProperty("pickupLogPotions", ModConfig.pickupLogPotions);
            json.addProperty("pickupLogTotems", ModConfig.pickupLogTotems);
            json.addProperty("pickupLogHeads", ModConfig.pickupLogHeads);
            json.addProperty("pickupLogSpawners", ModConfig.pickupLogSpawners);
            json.addProperty("pickupLogStructureBlocks", ModConfig.pickupLogStructureBlocks);
            json.addProperty("pickupLogRussian", ModConfig.pickupLogRussian);

            // === AUTO GG ===
            json.addProperty("autoGgEnabled", ModConfig.autoGgEnabled);
            json.addProperty("autoGgTemplate", ModConfig.autoGgTemplate);
            json.addProperty("autoGgDelay", ModConfig.autoGgDelay);
            json.addProperty("autoGgOnlyPlayers", ModConfig.autoGgOnlyPlayers);

            // === MUSIC PLAYER ===
            json.addProperty("musicPlayerEnabled", ModConfig.musicPlayerEnabled);
            json.addProperty("musicVolume", ModConfig.musicVolume);
            json.addProperty("musicRepeat", ModConfig.musicRepeat);
            json.addProperty("musicShuffle", ModConfig.musicShuffle);
            json.addProperty("musicHudX", ModConfig.musicHudX);
            json.addProperty("musicHudY", ModConfig.musicHudY);
            json.addProperty("musicHudAlpha", ModConfig.musicHudAlpha);
            json.addProperty("musicShowHud", ModConfig.musicShowHud);
            json.addProperty("musicLastIndex", ModConfig.musicLastIndex);
            json.addProperty("musicAutoPlay", ModConfig.musicAutoPlay);
            json.addProperty("musicRussian", ModConfig.musicRussian);

            // === CHAT FILTER ===
            json.addProperty("chatFilterEnabled", ModConfig.chatFilterEnabled);
            json.addProperty("chatFilterWordsRaw", ModConfig.chatFilterWordsRaw);
            json.addProperty("chatFilterRussian", ModConfig.chatFilterRussian);

            // === AUTO RECONNECT ===
            json.addProperty("autoReconnectEnabled", ModConfig.autoReconnectEnabled);
            json.addProperty("autoReconnectDelay", ModConfig.autoReconnectDelay);
            json.addProperty("autoReconnectShowHud", ModConfig.autoReconnectShowHud);
            json.addProperty("autoReconnectRussian", ModConfig.autoReconnectRussian);

            // === DEATH COORDS ===
            json.addProperty("deathCoordsEnabled", ModConfig.deathCoordsEnabled);
            json.addProperty("deathCoordsRussian", ModConfig.deathCoordsRussian);
            json.addProperty("lastDeathX", ModConfig.lastDeathX);
            json.addProperty("lastDeathY", ModConfig.lastDeathY);
            json.addProperty("lastDeathZ", ModConfig.lastDeathZ);
            json.addProperty("lastDeathDimension", ModConfig.lastDeathDimension);
            json.addProperty("lastDeathTime", ModConfig.lastDeathTime);

            // === STRIKE RANGE ===
            json.addProperty("strikeRangeEnabled", ModConfig.strikeRangeEnabled);
            json.addProperty("strikeRangeX", ModConfig.strikeRangeX);
            json.addProperty("strikeRangeY", ModConfig.strikeRangeY);
            json.addProperty("strikeRangeColor", ModConfig.strikeRangeColor);
            json.addProperty("strikeRangeAlpha", ModConfig.strikeRangeAlpha);
            json.addProperty("strikeRangeFontSize", ModConfig.strikeRangeFontSize);
            json.addProperty("strikeRangeShowTime", ModConfig.strikeRangeShowTime);
            json.addProperty("strikeRangeShowBlocks", ModConfig.strikeRangeShowBlocks);
            json.addProperty("strikeRangeShowTarget", ModConfig.strikeRangeShowTarget);

            // === CUSTOM HITBOX ===
            json.addProperty("customHitboxEnabled", ModConfig.customHitboxEnabled);
            json.addProperty("customHitboxRussian", ModConfig.customHitboxRussian);
            json.addProperty("customHitboxColor", ModConfig.customHitboxColor);
            json.addProperty("customHitboxAlpha", ModConfig.customHitboxAlpha);

            // === ITEM SCROLLER ===
            json.addProperty("itemScrollerEnabled", ModConfig.itemScrollerEnabled);
            json.addProperty("itemScrollerRussian", ModConfig.itemScrollerRussian);
            json.addProperty("itemScrollerDelay", ModConfig.itemScrollerDelay);
            json.addProperty("itemScrollerShiftStack", ModConfig.itemScrollerShiftStack);
            json.addProperty("itemScrollerCtrlAll", ModConfig.itemScrollerCtrlAll);

            // === COOLDOWNS ===
            json.addProperty("cooldownsEnabled", ModConfig.cooldownsEnabled);
            json.addProperty("cooldownsRussian", ModConfig.cooldownsRussian);
            json.addProperty("cooldownsX", ModConfig.cooldownsX);
            json.addProperty("cooldownsY", ModConfig.cooldownsY);
            json.addProperty("cooldownsColor", ModConfig.cooldownsColor);
            json.addProperty("cooldownsAlpha", ModConfig.cooldownsAlpha);
            json.addProperty("cooldownsShowIcon", ModConfig.cooldownsShowIcon);
            json.addProperty("cooldownsShowName", ModConfig.cooldownsShowName);
            json.addProperty("cooldownsShowTime", ModConfig.cooldownsShowTime);
            json.addProperty("cooldownsShowOnlyHotbar", ModConfig.cooldownsShowOnlyHotbar);
            json.addProperty("cooldownsMaxItems", ModConfig.cooldownsMaxItems);
            json.addProperty("cooldownsFontSize", ModConfig.cooldownsFontSize);
            json.addProperty("cooldownsIconDarkening", ModConfig.cooldownsIconDarkening);

            // === GAMMA UTIL ===
            json.addProperty("gammaUtilEnabled", ModConfig.gammaUtilEnabled);
            json.addProperty("gammaValue", ModConfig.gammaValue);

            // === TARGET ESP ===
            json.addProperty("targetEspEnabled", ModConfig.targetEspEnabled);
            json.addProperty("targetEspVariant", ModConfig.targetEspVariant);
            json.addProperty("targetEspColor", ModConfig.targetEspColor);
            json.addProperty("targetEspAlpha", ModConfig.targetEspAlpha);
            json.addProperty("targetEspSize", ModConfig.targetEspSize);
            json.addProperty("targetEspRotationSpeed", ModConfig.targetEspRotationSpeed);
            json.addProperty("targetEspPulse", ModConfig.targetEspPulse);
            json.addProperty("targetEspHurt", ModConfig.targetEspHurt);
            json.addProperty("targetEspHideHitboxes", ModConfig.targetEspHideHitboxes);
            json.addProperty("targetEspRussian", ModConfig.targetEspRussian);

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
            ModConfig.showHud = getBool(json, "showHud", ModConfig.showHud);
            ModConfig.hudColor = getInt(json, "hudColor", ModConfig.hudColor);
            ModConfig.showModLogo = getBool(json, "showModLogo", ModConfig.showModLogo);
            ModConfig.modLogoX = getInt(json, "modLogoX", ModConfig.modLogoX);
            ModConfig.modLogoY = getInt(json, "modLogoY", ModConfig.modLogoY);
            ModConfig.modLogoRussian = getBool(json, "modLogoRussian", ModConfig.modLogoRussian);
            ModConfig.hudBackgroundEnabled = getBool(json, "hudBackgroundEnabled", ModConfig.hudBackgroundEnabled);
            ModConfig.hudBackgroundAlpha = getInt(json, "hudBackgroundAlpha", ModConfig.hudBackgroundAlpha);
            ModConfig.hudBackgroundColor = getInt(json, "hudBackgroundColor", ModConfig.hudBackgroundColor);
            ModConfig.hudBackgroundHeight = getInt(json, "hudBackgroundHeight", ModConfig.hudBackgroundHeight);
            ModConfig.guiColor = getInt(json, "guiColor", ModConfig.guiColor);
            ModConfig.guiTextColor = getInt(json, "guiTextColor", ModConfig.guiTextColor);
            ModConfig.hudAlpha = getInt(json, "hudAlpha", ModConfig.hudAlpha);
            ModConfig.showPet = getBool(json, "showPet", ModConfig.showPet);
            ModConfig.searchHistoryRaw = getString(json, "searchHistoryRaw", ModConfig.searchHistoryRaw);

            // === ВИДИМОСТЬ ЭЛЕМЕНТОВ ===
            ModConfig.showCoords = getBool(json, "showCoords", ModConfig.showCoords);
            ModConfig.showBiome = getBool(json, "showBiome", ModConfig.showBiome);
            ModConfig.showTime = getBool(json, "showTime", ModConfig.showTime);
            ModConfig.showFps = getBool(json, "showFps", ModConfig.showFps);
            ModConfig.showPing = getBool(json, "showPing", ModConfig.showPing);
            ModConfig.showTps = getBool(json, "showTps", ModConfig.showTps);
            ModConfig.showBps = getBool(json, "showBps", ModConfig.showBps);
            ModConfig.showDirection = getBool(json, "showDirection", ModConfig.showDirection);
            ModConfig.showHitCounter = getBool(json, "showHitCounter", ModConfig.showHitCounter);
            ModConfig.showPotionEffects = getBool(json, "showPotionEffects", ModConfig.showPotionEffects);
            ModConfig.potionEffectsIcons = getBool(json, "potionEffectsIcons", ModConfig.potionEffectsIcons);
            ModConfig.showEquipmentHud = getBool(json, "showEquipmentHud", ModConfig.showEquipmentHud);
            ModConfig.equipmentShowDurability = getBool(json, "equipmentShowDurability", ModConfig.equipmentShowDurability);
            ModConfig.lowFireEnabled = getBool(json, "lowFireEnabled", ModConfig.lowFireEnabled);
            ModConfig.lowShieldEnabled = getBool(json, "lowShieldEnabled", ModConfig.lowShieldEnabled);

            // === ПОЗИЦИИ ===
            ModConfig.coordsX = getInt(json, "coordsX", ModConfig.coordsX);
            ModConfig.coordsY = getInt(json, "coordsY", ModConfig.coordsY);
            ModConfig.biomeX = getInt(json, "biomeX", ModConfig.biomeX);
            ModConfig.biomeY = getInt(json, "biomeY", ModConfig.biomeY);
            ModConfig.timeX = getInt(json, "timeX", ModConfig.timeX);
            ModConfig.timeY = getInt(json, "timeY", ModConfig.timeY);
            ModConfig.fpsX = getInt(json, "fpsX", ModConfig.fpsX);
            ModConfig.fpsY = getInt(json, "fpsY", ModConfig.fpsY);
            ModConfig.pingX = getInt(json, "pingX", ModConfig.pingX);
            ModConfig.pingY = getInt(json, "pingY", ModConfig.pingY);
            ModConfig.tpsX = getInt(json, "tpsX", ModConfig.tpsX);
            ModConfig.tpsY = getInt(json, "tpsY", ModConfig.tpsY);
            ModConfig.bpsX = getInt(json, "bpsX", ModConfig.bpsX);
            ModConfig.bpsY = getInt(json, "bpsY", ModConfig.bpsY);
            ModConfig.directionX = getInt(json, "directionX", ModConfig.directionX);
            ModConfig.directionY = getInt(json, "directionY", ModConfig.directionY);
            ModConfig.hitCounterX = getInt(json, "hitCounterX", ModConfig.hitCounterX);
            ModConfig.hitCounterY = getInt(json, "hitCounterY", ModConfig.hitCounterY);
            ModConfig.potionEffectsX = getInt(json, "potionEffectsX", ModConfig.potionEffectsX);
            ModConfig.potionEffectsY = getInt(json, "potionEffectsY", ModConfig.potionEffectsY);
            ModConfig.equipmentHudX = getInt(json, "equipmentHudX", ModConfig.equipmentHudX);
            ModConfig.equipmentHudY = getInt(json, "equipmentHudY", ModConfig.equipmentHudY);

            // === ПЕРЕВОД ===
            ModConfig.fpsRussian = getBool(json, "fpsRussian", ModConfig.fpsRussian);
            ModConfig.pingRussian = getBool(json, "pingRussian", ModConfig.pingRussian);
            ModConfig.tpsRussian = getBool(json, "tpsRussian", ModConfig.tpsRussian);
            ModConfig.bpsRussian = getBool(json, "bpsRussian", ModConfig.bpsRussian);
            ModConfig.directionRussian = getBool(json, "directionRussian", ModConfig.directionRussian);
            ModConfig.hitCounterRussian = getBool(json, "hitCounterRussian", ModConfig.hitCounterRussian);
            ModConfig.potionEffectsRussian = getBool(json, "potionEffectsRussian", ModConfig.potionEffectsRussian);
            ModConfig.equipmentHudRussian = getBool(json, "equipmentHudRussian", ModConfig.equipmentHudRussian);
            ModConfig.lowFireShieldRussian = getBool(json, "lowFireShieldRussian", ModConfig.lowFireShieldRussian);
            ModConfig.zoomRussian = getBool(json, "zoomRussian", ModConfig.zoomRussian);
            ModConfig.autoSwapRussian = getBool(json, "autoSwapRussian", ModConfig.autoSwapRussian);
            ModConfig.fastExpRussian = getBool(json, "fastExpRussian", ModConfig.fastExpRussian);
            ModConfig.autoSprintRussian = getBool(json, "autoSprintRussian", ModConfig.autoSprintRussian);
            ModConfig.shiftTapRussian = getBool(json, "shiftTapRussian", ModConfig.shiftTapRussian);

            // === TAPEMOUSE ===
            ModConfig.tapeMouseEnabled = getBool(json, "tapeMouseEnabled", ModConfig.tapeMouseEnabled);
            ModConfig.tapeMouseTarget = getInt(json, "tapeMouseTarget", ModConfig.tapeMouseTarget);
            ModConfig.tapeMouseDelay = getFloat(json, "tapeMouseDelay", ModConfig.tapeMouseDelay);
            ModConfig.tapeMouseRussian = getBool(json, "tapeMouseRussian", ModConfig.tapeMouseRussian);
            ModConfig.tapeMouseRequireTarget = getBool(json, "tapeMouseRequireTarget", ModConfig.tapeMouseRequireTarget);
            ModConfig.tapeMouseRequireFullAttack = getBool(json, "tapeMouseRequireFullAttack", ModConfig.tapeMouseRequireFullAttack);
            ModConfig.tapeMouseButton = getInt(json, "tapeMouseButton", ModConfig.tapeMouseButton);
            ModConfig.tapeMouseHoldRight = getBool(json, "tapeMouseHoldRight", ModConfig.tapeMouseHoldRight);

            // === AUTOSWAP ===
            ModConfig.autoSwapEnabled = getBool(json, "autoSwapEnabled", ModConfig.autoSwapEnabled);
            ModConfig.autoSwapMode = getInt(json, "autoSwapMode", ModConfig.autoSwapMode);
            ModConfig.autoSwapOpenDelay = getInt(json, "autoSwapOpenDelay", ModConfig.autoSwapOpenDelay);
            ModConfig.autoSwapCooldown = getInt(json, "autoSwapCooldown", ModConfig.autoSwapCooldown);

            // === FASTEXP ===
            ModConfig.fastExpEnabled = getBool(json, "fastExpEnabled", ModConfig.fastExpEnabled);

            // === AUTOSPRINT / SHIFTTAP ===
            ModConfig.autoSprintEnabled = getBool(json, "autoSprintEnabled", ModConfig.autoSprintEnabled);
            ModConfig.shiftTapEnabled = getBool(json, "shiftTapEnabled", ModConfig.shiftTapEnabled);

            // === ZOOM ===
            ModConfig.zoomEnabled = getBool(json, "zoomEnabled", ModConfig.zoomEnabled);
            ModConfig.zoomFactor = getFloat(json, "zoomFactor", ModConfig.zoomFactor);
            ModConfig.zoomSmoothness = getFloat(json, "zoomSmoothness", ModConfig.zoomSmoothness);

            // === ASPECT RATIO ===
            ModConfig.aspectRatioEnabled = getBool(json, "aspectRatioEnabled", ModConfig.aspectRatioEnabled);
            ModConfig.aspectRatio = getFloat(json, "aspectRatio", ModConfig.aspectRatio);
            ModConfig.aspectRatioRussian = getBool(json, "aspectRatioRussian", ModConfig.aspectRatioRussian);

            // === CUSTOM HIT SOUNDS ===
            ModConfig.customHitSoundsEnabled = getBool(json, "customHitSoundsEnabled", ModConfig.customHitSoundsEnabled);
            ModConfig.customHitSoundVolume = getFloat(json, "customHitSoundVolume", ModConfig.customHitSoundVolume);
            ModConfig.customHitSoundPitch = getFloat(json, "customHitSoundPitch", ModConfig.customHitSoundPitch);
            ModConfig.customHitSoundsRussian = getBool(json, "customHitSoundsRussian", ModConfig.customHitSoundsRussian);
            ModConfig.customHitSoundPreset = getInt(json, "customHitSoundPreset", ModConfig.customHitSoundPreset);

            // === LOW FIRE / LOW SHIELD ===
            ModConfig.lowFireOffset = getFloat(json, "lowFireOffset", ModConfig.lowFireOffset);
            ModConfig.lowShieldOffset = getFloat(json, "lowShieldOffset", ModConfig.lowShieldOffset);

            // === COMBO COUNTER ===
            ModConfig.comboEnabled = getBool(json, "comboEnabled", ModConfig.comboEnabled);
            ModConfig.comboX = getInt(json, "comboX", ModConfig.comboX);
            ModConfig.comboY = getInt(json, "comboY", ModConfig.comboY);
            ModConfig.comboColor = getInt(json, "comboColor", ModConfig.comboColor);
            ModConfig.comboResetTime = getInt(json, "comboResetTime", ModConfig.comboResetTime);
            ModConfig.comboFontSize = getInt(json, "comboFontSize", ModConfig.comboFontSize);
            ModConfig.comboRussian = getBool(json, "comboRussian", ModConfig.comboRussian);

            // === EFFECT WARNINGS ===
            ModConfig.effectWarningsEnabled = getBool(json, "effectWarningsEnabled", ModConfig.effectWarningsEnabled);
            ModConfig.effectWarningsX = getInt(json, "effectWarningsX", ModConfig.effectWarningsX);
            ModConfig.effectWarningsY = getInt(json, "effectWarningsY", ModConfig.effectWarningsY);
            ModConfig.effectWarningsColor = getInt(json, "effectWarningsColor", ModConfig.effectWarningsColor);
            ModConfig.effectWarningsThreshold = getInt(json, "effectWarningsThreshold", ModConfig.effectWarningsThreshold);
            ModConfig.effectWarningsAlpha = getInt(json, "effectWarningsAlpha", ModConfig.effectWarningsAlpha);
            ModConfig.effectWarningsShowName = getBool(json, "effectWarningsShowName", ModConfig.effectWarningsShowName);
            ModConfig.effectWarningsShowIcon = getBool(json, "effectWarningsShowIcon", ModConfig.effectWarningsShowIcon);
            ModConfig.effectWarningsRussian = getBool(json, "effectWarningsRussian", ModConfig.effectWarningsRussian);

            // === CROSSHAIR ===
            ModConfig.crosshairEnabled = getBool(json, "crosshairEnabled", ModConfig.crosshairEnabled);
            ModConfig.crosshairColor = getInt(json, "crosshairColor", ModConfig.crosshairColor);
            ModConfig.crosshairSize = getInt(json, "crosshairSize", ModConfig.crosshairSize);
            ModConfig.crosshairThickness = getInt(json, "crosshairThickness", ModConfig.crosshairThickness);
            ModConfig.crosshairGap = getInt(json, "crosshairGap", ModConfig.crosshairGap);
            ModConfig.crosshairAlpha = getInt(json, "crosshairAlpha", ModConfig.crosshairAlpha);
            ModConfig.crosshairRussian = getBool(json, "crosshairRussian", ModConfig.crosshairRussian);
            ModConfig.crosshairShape = getInt(json, "crosshairShape", ModConfig.crosshairShape);

            // === WAYPOINTS ===
            ModConfig.waypointsRaw = getString(json, "waypointsRaw", ModConfig.waypointsRaw);
            ModConfig.waypointsMax = getInt(json, "waypointsMax", ModConfig.waypointsMax);
            ModConfig.waypointsRussian = getBool(json, "waypointsRussian", ModConfig.waypointsRussian);
            ModConfig.waypointsEnabled = getBool(json, "waypointsEnabled", ModConfig.waypointsEnabled);

            // === TOTEM LOG ===
            ModConfig.totemLogEnabled = getBool(json, "totemLogEnabled", ModConfig.totemLogEnabled);
            ModConfig.totemLogRadius = getInt(json, "totemLogRadius", ModConfig.totemLogRadius);
            ModConfig.totemLogSound = getBool(json, "totemLogSound", ModConfig.totemLogSound);
            ModConfig.totemLogRussian = getBool(json, "totemLogRussian", ModConfig.totemLogRussian);

            // === CAMERA ===
            ModConfig.noHurtCamEnabled = getBool(json, "noHurtCamEnabled", ModConfig.noHurtCamEnabled);
            ModConfig.noBobbingEnabled = getBool(json, "noBobbingEnabled", ModConfig.noBobbingEnabled);
            ModConfig.cameraRussian = getBool(json, "cameraRussian", ModConfig.cameraRussian);

            // === ITEM PHYSICS ===
            ModConfig.itemPhysicsEnabled = getBool(json, "itemPhysicsEnabled", ModConfig.itemPhysicsEnabled);
            ModConfig.itemPhysicsRussian = getBool(json, "itemPhysicsRussian", ModConfig.itemPhysicsRussian);

            // === PARTICLE BLOCKER ===
            ModConfig.particleBlockerEnabled = getBool(json, "particleBlockerEnabled", ModConfig.particleBlockerEnabled);
            ModConfig.particleBlockerFire = getBool(json, "particleBlockerFire", ModConfig.particleBlockerFire);
            ModConfig.particleBlockerSmoke = getBool(json, "particleBlockerSmoke", ModConfig.particleBlockerSmoke);
            ModConfig.particleBlockerExplosion = getBool(json, "particleBlockerExplosion", ModConfig.particleBlockerExplosion);
            ModConfig.particleBlockerPotions = getBool(json, "particleBlockerPotions", ModConfig.particleBlockerPotions);
            ModConfig.particleBlockerWater = getBool(json, "particleBlockerWater", ModConfig.particleBlockerWater);
            ModConfig.particleBlockerRedstone = getBool(json, "particleBlockerRedstone", ModConfig.particleBlockerRedstone);
            ModConfig.particleBlockerPortal = getBool(json, "particleBlockerPortal", ModConfig.particleBlockerPortal);
            ModConfig.particleBlockerCrit = getBool(json, "particleBlockerCrit", ModConfig.particleBlockerCrit);
            ModConfig.particleBlockerRussian = getBool(json, "particleBlockerRussian", ModConfig.particleBlockerRussian);

            // === PVP SAFE ===
            ModConfig.pvpSafeEnabled = getBool(json, "pvpSafeEnabled", ModConfig.pvpSafeEnabled);
            ModConfig.pvpSafeTimer = getInt(json, "pvpSafeTimer", ModConfig.pvpSafeTimer);
            ModConfig.pvpSafeBlockQuit = getBool(json, "pvpSafeBlockQuit", ModConfig.pvpSafeBlockQuit);
            ModConfig.pvpSafeBlockCommands = getBool(json, "pvpSafeBlockCommands", ModConfig.pvpSafeBlockCommands);
            ModConfig.pvpSafeShowHud = getBool(json, "pvpSafeShowHud", ModConfig.pvpSafeShowHud);
            ModConfig.pvpSafeHudX = getInt(json, "pvpSafeHudX", ModConfig.pvpSafeHudX);
            ModConfig.pvpSafeHudY = getInt(json, "pvpSafeHudY", ModConfig.pvpSafeHudY);
            ModConfig.pvpSafeHudColor = getInt(json, "pvpSafeHudColor", ModConfig.pvpSafeHudColor);
            ModConfig.pvpSafeRussian = getBool(json, "pvpSafeRussian", ModConfig.pvpSafeRussian);

            // === PICKUP LOGGER ===
            ModConfig.pickupLogEnabled = getBool(json, "pickupLogEnabled", ModConfig.pickupLogEnabled);
            ModConfig.pickupLogMode = getInt(json, "pickupLogMode", ModConfig.pickupLogMode);
            ModConfig.pickupLogWeapon = getBool(json, "pickupLogWeapon", ModConfig.pickupLogWeapon);
            ModConfig.pickupLogArmor = getBool(json, "pickupLogArmor", ModConfig.pickupLogArmor);
            ModConfig.pickupLogPotions = getBool(json, "pickupLogPotions", ModConfig.pickupLogPotions);
            ModConfig.pickupLogTotems = getBool(json, "pickupLogTotems", ModConfig.pickupLogTotems);
            ModConfig.pickupLogHeads = getBool(json, "pickupLogHeads", ModConfig.pickupLogHeads);
            ModConfig.pickupLogSpawners = getBool(json, "pickupLogSpawners", ModConfig.pickupLogSpawners);
            ModConfig.pickupLogStructureBlocks = getBool(json, "pickupLogStructureBlocks", ModConfig.pickupLogStructureBlocks);
            ModConfig.pickupLogRussian = getBool(json, "pickupLogRussian", ModConfig.pickupLogRussian);

            // === AUTO GG ===
            ModConfig.autoGgEnabled = getBool(json, "autoGgEnabled", ModConfig.autoGgEnabled);
            ModConfig.autoGgTemplate = getString(json, "autoGgTemplate", ModConfig.autoGgTemplate);
            ModConfig.autoGgDelay = getFloat(json, "autoGgDelay", ModConfig.autoGgDelay);
            ModConfig.autoGgOnlyPlayers = getBool(json, "autoGgOnlyPlayers", ModConfig.autoGgOnlyPlayers);

            // === MUSIC PLAYER ===
            ModConfig.musicPlayerEnabled = getBool(json, "musicPlayerEnabled", ModConfig.musicPlayerEnabled);
            ModConfig.musicVolume = getFloat(json, "musicVolume", ModConfig.musicVolume);
            ModConfig.musicRepeat = getInt(json, "musicRepeat", ModConfig.musicRepeat);
            ModConfig.musicShuffle = getBool(json, "musicShuffle", ModConfig.musicShuffle);
            ModConfig.musicHudX = getInt(json, "musicHudX", ModConfig.musicHudX);
            ModConfig.musicHudY = getInt(json, "musicHudY", ModConfig.musicHudY);
            ModConfig.musicHudAlpha = getInt(json, "musicHudAlpha", ModConfig.musicHudAlpha);
            ModConfig.musicShowHud = getBool(json, "musicShowHud", ModConfig.musicShowHud);
            ModConfig.musicLastIndex = getInt(json, "musicLastIndex", ModConfig.musicLastIndex);
            ModConfig.musicAutoPlay = getBool(json, "musicAutoPlay", ModConfig.musicAutoPlay);
            ModConfig.musicRussian = getBool(json, "musicRussian", ModConfig.musicRussian);

            // === CHAT FILTER ===
            ModConfig.chatFilterEnabled = getBool(json, "chatFilterEnabled", ModConfig.chatFilterEnabled);
            ModConfig.chatFilterWordsRaw = getString(json, "chatFilterWordsRaw", ModConfig.chatFilterWordsRaw);
            ModConfig.chatFilterRussian = getBool(json, "chatFilterRussian", ModConfig.chatFilterRussian);

            // === AUTO RECONNECT ===
            ModConfig.autoReconnectEnabled = getBool(json, "autoReconnectEnabled", ModConfig.autoReconnectEnabled);
            ModConfig.autoReconnectDelay = getInt(json, "autoReconnectDelay", ModConfig.autoReconnectDelay);
            ModConfig.autoReconnectShowHud = getBool(json, "autoReconnectShowHud", ModConfig.autoReconnectShowHud);
            ModConfig.autoReconnectRussian = getBool(json, "autoReconnectRussian", ModConfig.autoReconnectRussian);

            // === DEATH COORDS ===
            ModConfig.deathCoordsEnabled = getBool(json, "deathCoordsEnabled", ModConfig.deathCoordsEnabled);
            ModConfig.deathCoordsRussian = getBool(json, "deathCoordsRussian", ModConfig.deathCoordsRussian);
            ModConfig.lastDeathX = getInt(json, "lastDeathX", ModConfig.lastDeathX);
            ModConfig.lastDeathY = getInt(json, "lastDeathY", ModConfig.lastDeathY);
            ModConfig.lastDeathZ = getInt(json, "lastDeathZ", ModConfig.lastDeathZ);
            ModConfig.lastDeathDimension = getString(json, "lastDeathDimension", ModConfig.lastDeathDimension);
            ModConfig.lastDeathTime = getLong(json, "lastDeathTime", ModConfig.lastDeathTime);

            // === STRIKE RANGE ===
            ModConfig.strikeRangeEnabled = getBool(json, "strikeRangeEnabled", ModConfig.strikeRangeEnabled);
            ModConfig.strikeRangeX = getInt(json, "strikeRangeX", ModConfig.strikeRangeX);
            ModConfig.strikeRangeY = getInt(json, "strikeRangeY", ModConfig.strikeRangeY);
            ModConfig.strikeRangeColor = getInt(json, "strikeRangeColor", ModConfig.strikeRangeColor);
            ModConfig.strikeRangeAlpha = getInt(json, "strikeRangeAlpha", ModConfig.strikeRangeAlpha);
            ModConfig.strikeRangeFontSize = getInt(json, "strikeRangeFontSize", ModConfig.strikeRangeFontSize);
            ModConfig.strikeRangeShowTime = getInt(json, "strikeRangeShowTime", ModConfig.strikeRangeShowTime);
            ModConfig.strikeRangeShowBlocks = getBool(json, "strikeRangeShowBlocks", ModConfig.strikeRangeShowBlocks);
            ModConfig.strikeRangeShowTarget = getBool(json, "strikeRangeShowTarget", ModConfig.strikeRangeShowTarget);

            // === CUSTOM HITBOX ===
            ModConfig.customHitboxEnabled = getBool(json, "customHitboxEnabled", ModConfig.customHitboxEnabled);
            ModConfig.customHitboxRussian = getBool(json, "customHitboxRussian", ModConfig.customHitboxRussian);
            ModConfig.customHitboxColor = getInt(json, "customHitboxColor", ModConfig.customHitboxColor);
            ModConfig.customHitboxAlpha = getInt(json, "customHitboxAlpha", ModConfig.customHitboxAlpha);

            // === ITEM SCROLLER ===
            ModConfig.itemScrollerEnabled = getBool(json, "itemScrollerEnabled", ModConfig.itemScrollerEnabled);
            ModConfig.itemScrollerRussian = getBool(json, "itemScrollerRussian", ModConfig.itemScrollerRussian);
            ModConfig.itemScrollerDelay = getInt(json, "itemScrollerDelay", ModConfig.itemScrollerDelay);
            ModConfig.itemScrollerShiftStack = getBool(json, "itemScrollerShiftStack", ModConfig.itemScrollerShiftStack);
            ModConfig.itemScrollerCtrlAll = getBool(json, "itemScrollerCtrlAll", ModConfig.itemScrollerCtrlAll);

            // === COOLDOWNS ===
            ModConfig.cooldownsEnabled = getBool(json, "cooldownsEnabled", ModConfig.cooldownsEnabled);
            ModConfig.cooldownsRussian = getBool(json, "cooldownsRussian", ModConfig.cooldownsRussian);
            ModConfig.cooldownsX = getInt(json, "cooldownsX", ModConfig.cooldownsX);
            ModConfig.cooldownsY = getInt(json, "cooldownsY", ModConfig.cooldownsY);
            ModConfig.cooldownsColor = getInt(json, "cooldownsColor", ModConfig.cooldownsColor);
            ModConfig.cooldownsAlpha = getInt(json, "cooldownsAlpha", ModConfig.cooldownsAlpha);
            ModConfig.cooldownsShowIcon = getBool(json, "cooldownsShowIcon", ModConfig.cooldownsShowIcon);
            ModConfig.cooldownsShowName = getBool(json, "cooldownsShowName", ModConfig.cooldownsShowName);
            ModConfig.cooldownsShowTime = getBool(json, "cooldownsShowTime", ModConfig.cooldownsShowTime);
            ModConfig.cooldownsShowOnlyHotbar = getBool(json, "cooldownsShowOnlyHotbar", ModConfig.cooldownsShowOnlyHotbar);
            ModConfig.cooldownsMaxItems = getInt(json, "cooldownsMaxItems", ModConfig.cooldownsMaxItems);
            ModConfig.cooldownsFontSize = getInt(json, "cooldownsFontSize", ModConfig.cooldownsFontSize);
            ModConfig.cooldownsIconDarkening = getInt(json, "cooldownsIconDarkening", ModConfig.cooldownsIconDarkening);

            // === GAMMA UTIL ===
            ModConfig.gammaUtilEnabled = getBool(json, "gammaUtilEnabled", ModConfig.gammaUtilEnabled);
            ModConfig.gammaValue = getFloat(json, "gammaValue", ModConfig.gammaValue);

            // === TARGET ESP ===
            ModConfig.targetEspEnabled = getBool(json, "targetEspEnabled", ModConfig.targetEspEnabled);
            ModConfig.targetEspVariant = getString(json, "targetEspVariant", ModConfig.targetEspVariant);
            ModConfig.targetEspColor = getInt(json, "targetEspColor", ModConfig.targetEspColor);
            ModConfig.targetEspAlpha = getInt(json, "targetEspAlpha", ModConfig.targetEspAlpha);
            ModConfig.targetEspSize = getFloat(json, "targetEspSize", ModConfig.targetEspSize);
            ModConfig.targetEspRotationSpeed = getFloat(json, "targetEspRotationSpeed", ModConfig.targetEspRotationSpeed);
            ModConfig.targetEspPulse = getFloat(json, "targetEspPulse", ModConfig.targetEspPulse);
            ModConfig.targetEspHurt = getBool(json, "targetEspHurt", ModConfig.targetEspHurt);
            ModConfig.targetEspHideHitboxes = getBool(json, "targetEspHideHitboxes", ModConfig.targetEspHideHitboxes);
            ModConfig.targetEspRussian = getBool(json, "targetEspRussian", ModConfig.targetEspRussian);

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
    private static long getLong(JsonObject json, String key, long def) {
        return json.has(key) ? json.get(key).getAsLong() : def;
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