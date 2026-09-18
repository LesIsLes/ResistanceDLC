package com.resistancedlc.config;

/**
 * ModConfig — единое хранилище состояния мода.
 * Сюда переехали все public static поля из MyCustomScreen.
 *
 * ВАЖНО: поля пока остаются public static для совместимости с существующим кодом.
 * Позже (Фаза 1, шаг 5+) переведём на Feature-архитектуру.
 */
public class ModConfig {

    // ===================== HUD: ОСНОВНОЕ =====================
    public static boolean showHud = true;
    public static int hudColor = 0xFF00FF00;
    public static boolean showModLogo = false;
    public static int modLogoX = 10, modLogoY = 5;
    public static boolean modLogoRussian = false;
    public static boolean hudBackgroundEnabled = false;
    public static int hudBackgroundAlpha = 128;
    public static int hudBackgroundColor = 0xFF000000;
    public static int hudBackgroundHeight = 10;
    public static int guiColor = 0xFF00FF00;
    public static int guiTextColor = 0xFFFFFFFF;
    public static int hudAlpha = 255;
    public static boolean showPet = true;

    // ===================== HUD: ЭЛЕМЕНТЫ =====================
    public static int coordsX = 10, coordsY = 35;
    public static int biomeX = 10, biomeY = 50;
    public static int timeX = 10, timeY = 65;

    public static boolean showCoords = true;
    public static boolean showBiome = true;
    public static boolean showTime = true;

    public static boolean showFps = false;
    public static boolean showPing = false;
    public static boolean showTps = false;
    public static boolean showBps = false;
    public static boolean showDirection = false;

    public static int fpsX = 10, fpsY = 80;
    public static int pingX = 10, pingY = 95;
    public static int tpsX = 10, tpsY = 110;
    public static int bpsX = 10, bpsY = 125;
    public static int directionX = 10, directionY = 140;

    public static boolean showHitCounter = false;
    public static int hitCounterX = 10, hitCounterY = 155;
    public static boolean hitCounterRussian = false;

    public static boolean showPotionEffects = false;
    public static int potionEffectsX = 10, potionEffectsY = 170;
    public static boolean potionEffectsRussian = false;
    public static boolean potionEffectsIcons = true;

    public static boolean showEquipmentHud = false;
    public static int equipmentHudX = 4, equipmentHudY = -44;
    public static boolean equipmentHudRussian = false;
    public static boolean equipmentShowDurability = true;

    public static boolean comboEnabled = false;
    public static int comboX = 10, comboY = 185;
    public static int comboColor = 0xFFFFFF00;
    public static int comboResetTime = 3;
    public static int comboFontSize = 1;
    public static boolean comboRussian = false;
    public static int currentCombo = 0;
    public static long lastComboTime = 0;

    public static boolean effectWarningsEnabled = false;
    public static int effectWarningsX = 300, effectWarningsY = 200;
    public static int effectWarningsColor = 0xFFFF0000;
    public static int effectWarningsThreshold = 10;
    public static int effectWarningsAlpha = 255;
    public static boolean effectWarningsShowName = true;
    public static boolean effectWarningsShowIcon = true;
    public static boolean effectWarningsRussian = false;

    // ===================== HUD: ЛОКАЛИЗАЦИЯ =====================
    public static boolean fpsRussian = false;
    public static boolean pingRussian = false;
    public static boolean tpsRussian = false;
    public static boolean bpsRussian = false;
    public static boolean directionRussian = false;

    // ===================== PVP =====================
    public static boolean customHitSoundsEnabled = false;
    public static float customHitSoundVolume = 1.0f;
    public static float customHitSoundPitch = 1.0f;
    public static boolean customHitSoundsRussian = false;
    public static int customHitSoundPreset = 1;

    public static boolean autoSwapEnabled = false;
    public static int autoSwapMode = 2;
    public static int autoSwapOpenDelay = 200;
    public static int autoSwapCooldown = 500;
    public static boolean autoSwapRussian = false;
    public static boolean autoSwapInProgress = false;
    public static int autoSwapStage = 0;
    public static long autoSwapNextActionTime = 0;
    public static long autoSwapLastTime = 0;
    public static int autoSwapSlotToSwap = -1;

    public static boolean fastExpEnabled = false;
    public static boolean fastExpRussian = false;

    public static boolean autoSprintEnabled = false;
    public static boolean autoSprintRussian = false;

    public static boolean shiftTapEnabled = false;
    public static boolean shiftTapRussian = false;
    public static long shiftTapReleaseTime = 0;
    public static boolean shiftTapActive = false;

    public static boolean totemLogEnabled = false;
    public static int totemLogRadius = 15;
    public static boolean totemLogSound = true;
    public static boolean totemLogRussian = false;

    public static boolean pvpSafeEnabled = false;
    public static int pvpSafeTimer = 30;
    public static boolean pvpSafeBlockQuit = true;
    public static boolean pvpSafeBlockCommands = true;
    public static boolean pvpSafeShowHud = true;
    public static int pvpSafeHudX = 10;
    public static int pvpSafeHudY = 240;
    public static int pvpSafeHudColor = 0xFFFF0000;
    public static boolean pvpSafeRussian = false;

    public static boolean pickupLogEnabled = false;
    public static int pickupLogMode = 1;
    public static boolean pickupLogWeapon = true;
    public static boolean pickupLogArmor = true;
    public static boolean pickupLogPotions = true;
    public static boolean pickupLogTotems = true;
    public static boolean pickupLogHeads = true;
    public static boolean pickupLogSpawners = true;
    public static boolean pickupLogStructureBlocks = true;
    public static boolean pickupLogRussian = false;

    // ===================== AUTO GG =====================
    public static boolean autoGgEnabled = false;
    public static String autoGgTemplate = "GG %s";
    public static float autoGgDelay = 0.5f;
    public static boolean autoGgOnlyPlayers = true;
    public static String autoGgLastVictim = "";

    // ===================== PVE =====================
    public static boolean tapeMouseEnabled = false;
    public static int tapeMouseTarget = 0;
    public static float tapeMouseDelay = 1.0f;
    public static boolean tapeMouseRussian = false;
    public static boolean tapeMouseRequireTarget = true;
    public static boolean tapeMouseRequireFullAttack = false;
    public static int tapeMouseButton = 0;
    public static boolean tapeMouseHoldRight = false;

    // ===================== VISUAL =====================
    public static boolean crosshairEnabled = false;
    public static int crosshairColor = 0xFFFFFFFF;
    public static int crosshairSize = 10;
    public static int crosshairThickness = 2;
    public static int crosshairGap = 3;
    public static int crosshairAlpha = 255;
    public static boolean crosshairRussian = false;
    public static int crosshairShape = 0;

    public static boolean aspectRatioEnabled = false;
    public static float aspectRatio = 1.0f;
    public static boolean aspectRatioRussian = false;

    public static boolean lowFireEnabled = false;
    public static float lowFireOffset = 0.3f;
    public static boolean lowShieldEnabled = false;
    public static float lowShieldOffset = 0.3f;
    public static boolean lowFireShieldRussian = false;

    public static boolean zoomEnabled = true;
    public static float zoomFactor = 4.0f;
    public static float zoomSmoothness = 0.25f;
    public static boolean zoomRussian = false;
    public static float currentZoom = 1.0f;

    public static String waypointsRaw = "";
    public static int waypointsMax = 5;
    public static boolean waypointsRussian = false;
    public static boolean waypointsEnabled = true;

    public static boolean noHurtCamEnabled = false;
    public static boolean noBobbingEnabled = false;
    public static boolean cameraRussian = false;

    public static boolean itemPhysicsEnabled = false;
    public static boolean itemPhysicsRussian = false;

    public static boolean particleBlockerEnabled = false;
    public static boolean particleBlockerFire = false;
    public static boolean particleBlockerSmoke = false;
    public static boolean particleBlockerExplosion = false;
    public static boolean particleBlockerPotions = false;
    public static boolean particleBlockerWater = false;
    public static boolean particleBlockerRedstone = false;
    public static boolean particleBlockerPortal = false;
    public static boolean particleBlockerCrit = false;
    public static boolean particleBlockerRussian = false;

    // ===================== STRIKE RANGE =====================
    public static boolean strikeRangeEnabled = false;
    public static int strikeRangeX = 10;
    public static int strikeRangeY = 300;
    public static int strikeRangeColor = 0xFFFFFF00;
    public static int strikeRangeAlpha = 255;
    public static int strikeRangeFontSize = 1;
    public static int strikeRangeShowTime = 1500;
    public static boolean strikeRangeShowBlocks = true;
    public static boolean strikeRangeShowTarget = true;
    public static String strikeRangeLastTarget = "";

    // ===================== MUSIC PLAYER =====================
    public static boolean musicPlayerEnabled = false;
    public static float musicVolume = 0.5f;
    public static int musicRepeat = 0;                // 0=off, 1=one, 2=all
    public static boolean musicShuffle = false;
    public static int musicHudX = -1;                 // -1 = auto (правый верхний)
    public static int musicHudY = 10;
    public static int musicHudAlpha = 255;
    public static boolean musicShowHud = true;
    public static int musicLastIndex = -1;            // для восстановления при входе в мир
    public static boolean musicAutoPlay = false;
    public static boolean musicRussian = false;

    // ===================== EASTER EGG (KILLAURA) =====================
    public static boolean killAuraEggEnabled = true;

    // ===================== MISC =====================
    public static boolean chatFilterEnabled = false;
    public static String chatFilterWordsRaw = "";
    public static boolean chatFilterRussian = false;

    public static boolean autoReconnectEnabled = false;
    public static int autoReconnectDelay = 5;
    public static boolean autoReconnectShowHud = true;
    public static boolean autoReconnectRussian = false;

    // ===================== DEATH COORDS =====================
    public static boolean deathCoordsEnabled = false;
    public static boolean deathCoordsRussian = false;
    public static int lastDeathX = 0;
    public static int lastDeathY = 0;
    public static int lastDeathZ = 0;
    public static String lastDeathDimension = "";
    public static long lastDeathTime = 0;

    // ===================== ПОИСК + WAYPOINTS =====================
    public static String searchHistoryRaw = "";
    public static final int SEARCH_HISTORY_MAX = 8;

    // ===================== СЛУЖЕБНОЕ =====================
    public static boolean isBindingKey = false;
    public static int bindingTarget = 0;

    public static double lastPlayerX = 0, lastPlayerY = 0, lastPlayerZ = 0;
    public static double currentBps = 0;

    // ===================== CUSTOM HITBOX =====================
    public static boolean customHitboxEnabled = false;
    public static boolean customHitboxRussian = false;
    public static int customHitboxColor = 0xFFFFFFFF;
    public static int customHitboxAlpha = 180;

    // ===================== ITEM SCROLLER =====================
    public static boolean itemScrollerEnabled = false;
    public static boolean itemScrollerRussian = false;
    public static int itemScrollerDelay = 200;
    public static boolean itemScrollerShiftStack = true;
    public static boolean itemScrollerCtrlAll = true;

    // ===================== COOLDOWNS =====================
    public static boolean cooldownsEnabled = false;
    public static boolean cooldownsRussian = false;
    public static int cooldownsX = 10;
    public static int cooldownsY = 200;
    public static int cooldownsColor = 0xFFFFFFFF;
    public static int cooldownsAlpha = 255;
    public static boolean cooldownsShowIcon = true;
    public static boolean cooldownsShowName = true;
    public static boolean cooldownsShowTime = true;
    public static boolean cooldownsShowOnlyHotbar = false;
    public static int cooldownsMaxItems = 5;
    public static int cooldownsFontSize = 1;
    public static int cooldownsIconDarkening = 180;

    // ===================== GAMMA UTIL =====================
    public static boolean gammaUtilEnabled = false;
    public static float gammaValue = 1.0f;

    // ===================== TARGET ESP =====================
    public static boolean targetEspEnabled = false;
    public static String targetEspVariant = "crystals";
    public static int targetEspColor = 0xFFAA00FF;
    public static int targetEspAlpha = 220;           // 0..255 — эмулируется размером
    public static float targetEspSize = 1.0f;         // 0.5..2.0
    public static float targetEspRotationSpeed = 1.0f; // 0.0..3.0
    public static float targetEspPulse = 0.15f;       // 0.0..0.5
    public static boolean targetEspHurt = true;
    public static boolean targetEspHideHitboxes = true;
    public static boolean targetEspRussian = false;
}