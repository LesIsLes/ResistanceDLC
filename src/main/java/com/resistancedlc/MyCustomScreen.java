package com.resistancedlc;

import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MyCustomScreen extends Screen {

    // ===== HUD =====
    public static boolean showHud = false;
    public static int hudColor = 0xFF00FF00;

    // ===== ИКОНКА МОДА =====
    public static boolean showModLogo = false;
    public static int modLogoX = 10, modLogoY = 5;
    public static boolean modLogoRussian = false;

    public static boolean hudBackgroundEnabled = false;
    public static int hudBackgroundAlpha = 128;
    public static int hudBackgroundColor = 0xFF000000;
    public static int hudBackgroundHeight = 10;
    public static int guiColor = 0xFF00FF00;
    public static int guiTextColor = 0xFFFFFFFF;

    // ===== ПОЗИЦИИ HUD =====
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

    public static boolean tapeMouseEnabled = false;
    public static int tapeMouseTarget = 0;
    public static float tapeMouseDelay = 1.0f;
    public static boolean tapeMouseRussian = false;
    public static boolean tapeMouseRequireTarget = true;
    public static boolean tapeMouseRequireFullAttack = false;
    public static int tapeMouseButton = 0;
    public static boolean tapeMouseHoldRight = false;

    public static boolean autoSwapEnabled = false;
    public static int autoSwapMode = 2;
    public static int autoSwapOpenDelay = 150;
    public static int autoSwapCooldown = 500;
    public static boolean autoSwapRussian = false;

    public static boolean autoSwapInProgress = false;
    public static int autoSwapStage = 0;
    public static long autoSwapNextActionTime = 0;
    public static long autoSwapLastTime = 0;
    public static int autoSwapSlotToSwap = -1;

    public static boolean fastExpEnabled = false;
    public static boolean fastExpRussian = false;

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

    public static boolean crosshairEnabled = false;
    public static int crosshairColor = 0xFFFFFFFF;
    public static int crosshairSize = 10;
    public static int crosshairThickness = 2;
    public static int crosshairGap = 3;
    public static int crosshairAlpha = 255;
    public static boolean crosshairRussian = false;
    public static int crosshairShape = 0;

    // ===== WAYPOINTS =====
    public static String waypointsRaw = "";
    public static int waypointsMax = 5;
    public static boolean waypointsRussian = false;
    public static boolean waypointsEnabled = true;

    public static boolean autoSprintEnabled = false;
    public static boolean autoSprintRussian = false;

    public static boolean shiftTapEnabled = false;
    public static boolean shiftTapRussian = false;
    public static long shiftTapReleaseTime = 0;
    public static boolean shiftTapActive = false;

    public static boolean customHitSoundsEnabled = false;
    public static float customHitSoundVolume = 1.0f;
    public static float customHitSoundPitch = 1.0f;
    public static boolean customHitSoundsRussian = false;
    public static int customHitSoundPreset = 1;

    public static boolean fpsRussian = false;
    public static boolean pingRussian = false;
    public static boolean tpsRussian = false;
    public static boolean bpsRussian = false;
    public static boolean directionRussian = false;

    public static boolean aspectRatioEnabled = false;
    public static float aspectRatio = 1.0f;
    public static boolean aspectRatioRussian = false;

    public static String searchHistoryRaw = "";
    private static final int SEARCH_HISTORY_MAX = 8;

    public static boolean showPet = true;
    public static int hudAlpha = 255;
    public static boolean isBindingKey = false;
    public static int bindingTarget = 0;

    public static double lastPlayerX = 0, lastPlayerY = 0, lastPlayerZ = 0;
    public static double currentBps = 0;

    // ===== ПАГИНАЦИЯ + РАЗДЕЛЫ =====
    private static final String[] SECTION_NAMES_RU = {
            "HUD", "PVP", "PVE", "Visual", "Misc"
    };
    private static final String[] SECTION_NAMES_EN = {
            "HUD", "PVP", "PVE", "Visual", "Misc"
    };

    private static final int[] SECTION_PAGES = {
            7,  // 0 = HUD
            5,  // 1 = PVP
            1,  // 2 = PVE
            6,  // 3 = Visual (6 страниц — добавлена Waypoints)
            2   // 4 = Misc
    };
    private static final int SECTION_COUNT = SECTION_PAGES.length;

    private int currentSection = 0;
    private int currentPage = 0;

    private EditBox hexField;
    private EditBox searchField;

    private List<String[]> searchResults = new ArrayList<>();
    private int searchResultX = -1;
    private int searchResultY = -1;
    private int searchResultWidth = 0;
    private int searchResultHeight = 0;

    private String searchOtherSectionMsg = "";

    private int panelX;
    private int panelY;
    private final int panelWidth = 400;
    private final int panelHeight = 420;

    private boolean eventsRegistered = false;

    /**
     * Точка-метка в мире.
     * Формат хранения: "Name:x:y:z|Name2:x:y:z|..."
     */
    public static record Waypoint(String name, double x, double y, double z) {
        public String serialize() {
            return name.replace(":", "_").replace("|", "_")
                    + ":" + x + ":" + y + ":" + z;
        }

        public static Waypoint deserialize(String s) {
            try {
                String[] parts = s.split(":");
                if (parts.length != 4) return null;
                String name = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                return new Waypoint(name, x, y, z);
            } catch (Exception e) {
                return null;
            }
        }

        public String displayName() {
            return name + " (" + (int) x + ", " + (int) y + ", " + (int) z + ")";
        }

        public double distanceTo(double px, double py, double pz) {
            double dx = x - px;
            double dy = y - py;
            double dz = z - pz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    private static final String[][] SEARCH_INDEX = {
            // ===== HUD 0.0 — Элементы HUD =====
            { "hud", "Показывать HUD", "0", "0" },
            { "худ", "Показывать HUD", "0", "0" },
            { "иконка", "Иконка мода", "0", "0" },
            { "лого", "Иконка мода", "0", "0" },
            { "logo", "Иконка мода", "0", "0" },
            { "иконка мода", "Иконка мода", "0", "0" },
            { "координаты", "Координаты", "0", "0" },
            { "coords", "Координаты", "0", "0" },
            { "xyz", "Координаты", "0", "0" },
            { "биом", "Биом", "0", "0" },
            { "biome", "Биом", "0", "0" },
            { "время", "Время", "0", "0" },
            { "time", "Время", "0", "0" },
            { "прозрачность", "Прозрачность HUD", "0", "0" },
            { "alpha", "Прозрачность HUD", "0", "0" },

            // ===== HUD 0.1 — Эффекты зелий =====
            { "эффект", "Эффекты зелий", "1", "0" },
            { "зелье", "Эффекты зелий", "1", "0" },
            { "potion", "Эффекты зелий", "1", "0" },
            { "иконки", "Иконки эффектов", "1", "0" },
            { "иконки эффектов", "Иконки эффектов", "1", "0" },
            { "icons", "Иконки эффектов", "1", "0" },

            // ===== HUD 0.2 — Экипировка =====
            { "экипировка", "Экипировка", "2", "0" },
            { "броня", "Экипировка", "2", "0" },
            { "прочность", "Прочность брони", "2", "0" },
            { "equipment", "Экипировка", "2", "0" },
            { "armor", "Экипировка", "2", "0" },
            { "durability", "Прочность брони", "2", "0" },

            // ===== HUD 0.3 — Effect Warnings =====
            { "warnings", "Effect Warnings", "3", "0" },
            { "варнинги", "Effect Warnings", "3", "0" },
            { "предупреждение", "Effect Warnings", "3", "0" },
            { "эффект предупреждение", "Effect Warnings", "3", "0" },
            { "порог", "Effect Warnings: порог", "3", "0" },
            { "threshold", "Effect Warnings: порог", "3", "0" },

            // ===== HUD 0.4 — Combo Counter =====
            { "комбо", "Combo Counter", "4", "0" },
            { "combo", "Combo Counter", "4", "0" },
            { "combo counter", "Combo Counter", "4", "0" },
            { "счётчик", "Combo Counter", "4", "0" },
            { "счетчик", "Combo Counter", "4", "0" },

            // ===== HUD 0.5 — Доп. элементы =====
            { "fps", "FPS / КВС", "5", "0" },
            { "квс", "FPS / КВС", "5", "0" },
            { "ping", "Ping / Пинг", "5", "0" },
            { "пинг", "Ping / Пинг", "5", "0" },
            { "tps", "TPS / ТВС", "5", "0" },
            { "твс", "TPS / ТВС", "5", "0" },
            { "bps", "BPS / БВС", "5", "0" },
            { "бвс", "BPS / БВС", "5", "0" },
            { "направление", "Направление", "5", "0" },
            { "direction", "Направление", "5", "0" },
            { "удар", "Счётчик ударов", "5", "0" },
            { "hit", "Счётчик ударов", "5", "0" },
            { "хит", "Счётчик ударов", "5", "0" },

            // ===== HUD 0.6 — Оформление HUD =====
            { "цвет", "Цвет HUD / GUI", "6", "0" },
            { "color", "Цвет HUD / GUI", "6", "0" },
            { "фон", "Фон HUD", "6", "0" },
            { "background", "Фон HUD", "6", "0" },
            { "сбросить цвета", "Сброс цветов", "6", "0" },

            // ===== PVP 1.0 — Custom Hit Sounds =====
            { "звук", "Custom Hit Sounds", "0", "1" },
            { "sound", "Custom Hit Sounds", "0", "1" },
            { "hit sound", "Custom Hit Sounds", "0", "1" },
            { "hitsound", "Custom Hit Sounds", "0", "1" },
            { "громкость", "Hit Sound: громкость", "0", "1" },
            { "тон", "Hit Sound: тон", "0", "1" },
            { "пресет", "Hit Sound: пресет", "0", "1" },

            // ===== PVP 1.1 — AutoSwap =====
            { "автосвап", "Автосвап", "1", "1" },
            { "autoswap", "Автосвап", "1", "1" },
            { "swap", "Автосвап", "1", "1" },
            { "свап", "Автосвап", "1", "1" },
            { "тотем", "Автосвап", "1", "1" },
            { "totem", "Автосвап", "1", "1" },

            // ===== PVP 1.2 — FastExp =====
            { "fast", "FastExp", "2", "1" },
            { "fastexp", "FastExp", "2", "1" },
            { "фаст", "FastExp", "2", "1" },
            { "опыт", "FastExp", "2", "1" },
            { "exp", "FastExp", "2", "1" },

            // ===== PVP 1.3 — ShiftTap =====
            { "shifttap", "ShiftTap", "3", "1" },
            { "shift", "ShiftTap", "3", "1" },
            { "шифт", "ShiftTap", "3", "1" },
            { "крит", "ShiftTap", "3", "1" },

            // ===== PVP 1.4 — AutoSprint =====
            { "sprint", "AutoSprint", "4", "1" },
            { "autosprint", "AutoSprint", "4", "1" },
            { "бег", "AutoSprint", "4", "1" },
            { "спринт", "AutoSprint", "4", "1" },

            // ===== PVE 2.0 — TapeMouse =====
            { "tape", "TapeMouse", "0", "2" },
            { "tapemouse", "TapeMouse", "0", "2" },
            { "тейп", "TapeMouse", "0", "2" },
            { "автокликер", "TapeMouse", "0", "2" },
            { "кликер", "TapeMouse", "0", "2" },

            // ===== Visual 3.0 — Crosshair =====
            { "прицел", "Кастомный прицел", "0", "3" },
            { "crosshair", "Кастомный прицел", "0", "3" },
            { "крест", "Кастомный прицел", "0", "3" },

            // ===== Visual 3.1 — FOV / Aspect Ratio =====
            { "fov", "FOV (Угол обзора)", "1", "3" },
            { "фов", "FOV (Угол обзора)", "1", "3" },
            { "растяг", "Aspect Ratio", "1", "3" },
            { "aspect", "Aspect Ratio", "1", "3" },

            // ===== Visual 3.2 — Low Fire / Low Shield =====
            { "огонь", "Низкий огонь", "2", "3" },
            { "fire", "Низкий огонь", "2", "3" },
            { "щит", "Низкий щит", "2", "3" },
            { "shield", "Низкий щит", "2", "3" },

            // ===== Visual 3.3 — Zoom =====
            { "зум", "Zoom (Приближение)", "3", "3" },
            { "zoom", "Zoom (Приближение)", "3", "3" },
            { "приближение", "Zoom (Приближение)", "3", "3" },

            // ===== Visual 3.4 — Темы GUI =====
            { "тема", "Темы GUI", "4", "3" },
            { "theme", "Темы GUI", "4", "3" },
            { "vanilla", "Тема Vanilla", "4", "3" },
            { "dark", "Тема Dark", "4", "3" },
            { "neon", "Тема Neon", "4", "3" },
            { "candy", "Тема Candy", "4", "3" },
            { "blood", "Тема Blood", "4", "3" },

            // ===== Visual 3.5 — Waypoints =====
            { "метка", "Waypoints", "5", "3" },
            { "метки", "Waypoints", "5", "3" },
            { "waypoint", "Waypoints", "5", "3" },
            { "waypoints", "Waypoints", "5", "3" },
            { "точка", "Waypoints", "5", "3" },

            // ===== Misc 4.0 — Привязка клавиш =====
            { "клавиша", "Привязка клавиши GUI", "0", "4" },
            { "keybind", "Привязка клавиши GUI", "0", "4" },
            { "gui", "Привязка клавиши GUI", "0", "4" },

            // ===== Misc 4.1 — Конфигурации =====
            { "конфиг", "Конфигурации", "1", "4" },
            { "config", "Конфигурации", "1", "4" },
            { "сохранить", "Сохранить конфиг", "1", "4" },
            { "загрузить", "Загрузить конфиг", "1", "4" }
    };
    public MyCustomScreen() {
        super(Component.literal("Resistance DLC — Настройки"));
    }

    /**
     * Регистрируется РОВНО ОДИН РАЗ на экземпляр Screen'а.
     * Вызывается из init(), а НЕ из конструктора.
     */
    private void registerScreenEvents() {
        ScreenKeyboardEvents.allowKeyPress(this).register((screen, keyEvent) -> {
            if (this.searchField != null && this.searchField.isFocused()) return true;
            if (this.hexField != null && this.hexField.isFocused()) return true;

            if (!isBindingKey) return true;
            int keyCode = keyEvent.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isBindingKey = false;
                bindingTarget = 0;
                this.rebuildWidgets();
                return false;
            }
            if (bindingTarget == 0) {
                KeyBindings.setKey(keyCode);
            } else if (bindingTarget == 1) {
                KeyBindings.setZoomKey(keyCode);
            } else if (bindingTarget == 2) {
                KeyBindings.setTapeMouseKey(keyCode);
            } else if (bindingTarget == 3) {
                KeyBindings.setAutoSwapKey(keyCode);
            } else if (bindingTarget == 4) {
                KeyBindings.setCustomHitSoundsKey(keyCode);
            } else if (bindingTarget == 5) {
                KeyBindings.setFastExpKey(keyCode);
            } else if (bindingTarget == 6) {
                KeyBindings.setShiftTapKey(keyCode);
            } else if (bindingTarget == 7) {
                KeyBindings.setComboKey(keyCode);
            } else if (bindingTarget == 8) {
                KeyBindings.setEffectWarningsKey(keyCode);
            } else if (bindingTarget == 9) {
                KeyBindings.setWaypointsKey(keyCode);
            }
            isBindingKey = false;
            bindingTarget = 0;
            this.rebuildWidgets();
            return false;
        });

        // Клик по истории поиска (когда поле пустое и в фокусе).
        ScreenMouseEvents.allowMouseClick(this).register((screen, mouseEvent) -> {
            double mouseX = mouseEvent.x();
            double mouseY = mouseEvent.y();

            if (this.searchField != null && this.searchField.getValue().isEmpty()
                    && this.searchField.isFocused()) {
                List<String> history = getSearchHistory();
                if (!history.isEmpty()) {
                    int histY = panelY + 75;
                    int histH = 14;
                    for (int i = 0; i < Math.min(history.size(), 5); i++) {
                        int y = histY + i * histH;
                        if (mouseX >= panelX - 180 && mouseX <= panelX - 20
                                && mouseY >= y && mouseY <= y + histH) {
                            this.searchField.setValue(history.get(i));
                            return false;
                        }
                    }
                }
            }

            return true;
        });
    }

    @Override
    protected void init() {
        this.panelX = (this.width - panelWidth) / 2 + 80;
        this.panelY = (this.height - panelHeight) / 2;

        if (!eventsRegistered) {
            eventsRegistered = true;
            registerScreenEvents();
        }

        clampSectionPage();

        int centerX = this.panelX + panelWidth / 2;
        int panelY = this.panelY;

        // ===== ПОИСК =====
        this.searchField = new EditBox(this.font, panelX - 180, panelY + 50, 160, 18,
                Component.literal("Поиск..."));
        this.searchField.setMaxLength(30);
        this.searchField.setResponder(text -> {});
        this.addRenderableWidget(this.searchField);

        // ===== ТАБЫ РАЗДЕЛОВ =====
        int tabX = panelX - 180;
        int tabY = panelY + 185;
        int tabW = 160;
        int tabH = 22;
        int tabGap = 4;

        for (int i = 0; i < SECTION_COUNT; i++) {
            final int sectionIdx = i;
            String label = getSectionName(i, modLogoRussian);
            int pagesInSection = SECTION_PAGES[i];
            String text = label + "  §7(" + pagesInSection + ")";

            Button tabBtn = Button.builder(Component.literal(text), (b) -> {
                currentSection = sectionIdx;
                currentPage = 0;
                this.searchOtherSectionMsg = "";
                this.rebuildWidgets();
            }).bounds(tabX, tabY + i * (tabH + tabGap), tabW, tabH).build();
            this.addRenderableWidget(tabBtn);
        }

        // ===== КНОПКИ ← / → =====
        Button prevPageBtn = Button.builder(Component.literal("←"), (btn) -> {
            navigatePrev();
            this.rebuildWidgets();
        }).bounds(panelX + 10, panelY + 385, 20, 20).build();
        prevPageBtn.active = currentPage > 0;
        this.addRenderableWidget(prevPageBtn);

        Button nextPageBtn = Button.builder(Component.literal("→"), (btn) -> {
            navigateNext();
            this.rebuildWidgets();
        }).bounds(panelX + panelWidth - 30, panelY + 385, 20, 20).build();
        nextPageBtn.active = currentPage < SECTION_PAGES[currentSection] - 1;
        this.addRenderableWidget(nextPageBtn);

        // ===== ЗАКРЫТЬ =====
        Button closeButton = Button.builder(Component.literal("Закрыть"), (btn) -> this.onClose())
                .bounds(panelX + panelWidth - 90, panelY + 360, 70, 18).build();
        this.addRenderableWidget(closeButton);

        // ===== КОНТЕНТ ТЕКУЩЕЙ СТРАНИЦЫ =====
        initCurrentPage(centerX, panelY);
    }

    private void clampSectionPage() {
        if (currentSection < 0) currentSection = 0;
        if (currentSection >= SECTION_COUNT) currentSection = SECTION_COUNT - 1;
        int max = SECTION_PAGES[currentSection];
        if (currentPage < 0) currentPage = 0;
        if (currentPage >= max) currentPage = max - 1;
    }

    private void navigatePrev() {
        if (currentPage > 0) {
            currentPage--;
        }
    }

    private void navigateNext() {
        if (currentPage < SECTION_PAGES[currentSection] - 1) {
            currentPage++;
        }
    }

    /**
     * Диспетчер: вызывает нужный initPage по (currentSection, currentPage).
     *   0 HUD (7):    0 элементы, 1 зелья, 2 экипировка, 3 warnings, 4 combo, 5 доп, 6 оформление
     *   1 PVP (5):    0 hit sounds, 1 autoswap, 2 fastexp, 3 shifttap, 4 autosprint
     *   2 PVE (1):    0 tapemouse
     *   3 Visual (6): 0 crosshair, 1 fov, 2 low fire/shield, 3 zoom, 4 темы, 5 waypoints
     *   4 Misc (2):   0 привязка GUI, 1 конфиги
     */
    private void initCurrentPage(int centerX, int panelY) {
        switch (currentSection) {
            case 0 -> { // HUD — 7 страниц
                switch (currentPage) {
                    case 0 -> initPage1(centerX, panelY);
                    case 1 -> initPage7(centerX, panelY);
                    case 2 -> initPage8(centerX, panelY);
                    case 3 -> initPage15(centerX, panelY);
                    case 4 -> initPage14(centerX, panelY);
                    case 5 -> initPage3(centerX, panelY);
                    case 6 -> initAppearancePage(centerX, panelY);
                }
            }
            case 1 -> { // PVP
                switch (currentPage) {
                    case 0 -> initPage6(centerX, panelY);
                    case 1 -> initPage11(centerX, panelY);
                    case 2 -> initPage12(centerX, panelY);
                    case 3 -> initPage13(centerX, panelY);
                    case 4 -> initPage3b(centerX, panelY);
                }
            }
            case 2 -> { // PVE
                switch (currentPage) {
                    case 0 -> initPage4(centerX, panelY);
                }
            }
            case 3 -> { // Visual
                switch (currentPage) {
                    case 0 -> initPage16(centerX, panelY);
                    case 1 -> initPage5(centerX, panelY);
                    case 2 -> initPage9(centerX, panelY);
                    case 3 -> initPage10(centerX, panelY);
                    case 4 -> initThemesPage(centerX, panelY);
                    case 5 -> initWaypointsPage(centerX, panelY);
                }
            }
            case 4 -> { // Misc
                switch (currentPage) {
                    case 0 -> initPage2(centerX, panelY);
                    case 1 -> initConfigsPage(centerX, panelY);
                }
            }
        }
    }

    private static void addSearchHistory(String query) {
        if (query == null) return;
        query = query.trim().toLowerCase();
        if (query.isEmpty()) return;

        List<String> history = new ArrayList<>();
        if (!searchHistoryRaw.isEmpty()) {
            for (String s : searchHistoryRaw.split("\\|")) {
                if (!s.isEmpty()) history.add(s);
            }
        }

        history.remove(query);
        history.add(0, query);

        while (history.size() > SEARCH_HISTORY_MAX) {
            history.remove(history.size() - 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(history.get(i));
        }
        searchHistoryRaw = sb.toString();

        ConfigManager.save();
    }

    private static List<String> getSearchHistory() {
        List<String> history = new ArrayList<>();
        if (searchHistoryRaw == null || searchHistoryRaw.isEmpty()) return history;
        for (String s : searchHistoryRaw.split("\\|")) {
            if (!s.isEmpty()) history.add(s);
        }
        return history;
    }

    // =========================================================
    // WAYPOINTS — методы
    // =========================================================

    public static List<Waypoint> getWaypoints() {
        List<Waypoint> list = new ArrayList<>();
        if (waypointsRaw == null || waypointsRaw.isEmpty()) return list;
        for (String s : waypointsRaw.split("\\|")) {
            if (s.isEmpty()) continue;
            Waypoint wp = Waypoint.deserialize(s);
            if (wp != null) list.add(wp);
        }
        return list;
    }

    public static void setWaypoints(List<Waypoint> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(list.get(i).serialize());
        }
        waypointsRaw = sb.toString();
    }

    public static boolean addWaypoint(double x, double y, double z) {
        List<Waypoint> list = getWaypoints();
        if (list.size() >= waypointsMax) return false;
        String name = "WP" + (list.size() + 1);
        list.add(new Waypoint(name, x, y, z));
        setWaypoints(list);
        ConfigManager.save();
        return true;
    }

    public static boolean removeWaypoint(int index) {
        List<Waypoint> list = getWaypoints();
        if (index < 0 || index >= list.size()) return false;
        list.remove(index);
        setWaypoints(list);
        ConfigManager.save();
        return true;
    }

    public static void clearWaypoints() {
        waypointsRaw = "";
        ConfigManager.save();
    }
    // ===== ХЕЛПЕР: EditBox "X, Y" + ОК + Сброс + 4 стрелки =====
    private void makePosEditor(int centerX, int panelY, int rowY,
                               java.util.function.IntSupplier getX,
                               java.util.function.IntSupplier getY,
                               java.util.function.BiConsumer<Integer, Integer> setXY,
                               int defaultX, int defaultY) {
        EditBox posField = new EditBox(this.font, centerX - 100, panelY + rowY, 90, 18,
                Component.literal("X, Y"));
        posField.setMaxLength(20);
        posField.setValue(getX.getAsInt() + ", " + getY.getAsInt());
        this.addRenderableWidget(posField);

        Button okBtn = Button.builder(Component.literal("ОК"), (b) -> {
            try {
                String[] parts = posField.getValue().split(",");
                if (parts.length == 2) {
                    int nx = Integer.parseInt(parts[0].trim());
                    int ny = Integer.parseInt(parts[1].trim());
                    setXY.accept(nx, ny);
                    ConfigManager.save();
                }
            } catch (Exception ignored) {}
        }).bounds(centerX - 5, panelY + rowY, 35, 18).build();
        this.addRenderableWidget(okBtn);

        Button resetBtn = Button.builder(Component.literal("↺"), (b) -> {
            setXY.accept(defaultX, defaultY);
            posField.setValue(defaultX + ", " + defaultY);
            ConfigManager.save();
        }).bounds(centerX + 35, panelY + rowY, 20, 18).build();
        this.addRenderableWidget(resetBtn);

        int arrowSize = 14;
        int arrowY = panelY + rowY + 20;
        Button upBtn = Button.builder(Component.literal("↑"), (b) -> {
            int newY = getY.getAsInt() - 5;
            setXY.accept(getX.getAsInt(), newY);
            posField.setValue(getX.getAsInt() + ", " + newY);
            ConfigManager.save();
        }).bounds(centerX - 100, arrowY, arrowSize, arrowSize).build();
        this.addRenderableWidget(upBtn);

        Button downBtn = Button.builder(Component.literal("↓"), (b) -> {
            int newY = getY.getAsInt() + 5;
            setXY.accept(getX.getAsInt(), newY);
            posField.setValue(getX.getAsInt() + ", " + newY);
            ConfigManager.save();
        }).bounds(centerX - 100 + arrowSize + 2, arrowY, arrowSize, arrowSize).build();
        this.addRenderableWidget(downBtn);

        Button leftBtn = Button.builder(Component.literal("←"), (b) -> {
            int newX = getX.getAsInt() - 5;
            setXY.accept(newX, getY.getAsInt());
            posField.setValue(newX + ", " + getY.getAsInt());
            ConfigManager.save();
        }).bounds(centerX - 100 + (arrowSize + 2) * 2, arrowY, arrowSize, arrowSize).build();
        this.addRenderableWidget(leftBtn);

        Button rightBtn = Button.builder(Component.literal("→"), (b) -> {
            int newX = getX.getAsInt() + 5;
            setXY.accept(newX, getY.getAsInt());
            posField.setValue(newX + ", " + getY.getAsInt());
            ConfigManager.save();
        }).bounds(centerX - 100 + (arrowSize + 2) * 3, arrowY, arrowSize, arrowSize).build();
        this.addRenderableWidget(rightBtn);
    }

    /**
     * HUD 0.6 — Оформление HUD.
     */
    private void initAppearancePage(int centerX, int panelY) {
        Checkbox hudCheckbox = Checkbox.builder(Component.literal("Показывать HUD"), this.font)
                .pos(centerX - 100, panelY + 45).selected(showHud)
                .onValueChange((c, v) -> { showHud = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudCheckbox);

        Checkbox hudBgCheckbox = Checkbox.builder(Component.literal("Фон HUD"), this.font)
                .pos(centerX + 30, panelY + 45).selected(hudBackgroundEnabled)
                .onValueChange((c, v) -> { hudBackgroundEnabled = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudBgCheckbox);

        this.hexField = new EditBox(this.font, centerX - 130, panelY + 78, 80, 18,
                Component.literal("#RRGGBB"));
        this.hexField.setMaxLength(7);
        this.hexField.setValue(String.format("#%06X", hudColor & 0xFFFFFF));
        this.addRenderableWidget(this.hexField);

        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = this.hexField.getValue().replace("#", "").trim();
            try {
                hudColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 78, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button greenBtn = Button.builder(Component.literal("Зел"), (b) -> { this.hexField.setValue("#00FF00"); hudColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 78, 40, 18).build();
        this.addRenderableWidget(greenBtn);
        Button redBtn = Button.builder(Component.literal("Крас"), (b) -> { this.hexField.setValue("#FF0000"); hudColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 78, 40, 18).build();
        this.addRenderableWidget(redBtn);
        Button blueBtn = Button.builder(Component.literal("Син"), (b) -> { this.hexField.setValue("#0000FF"); hudColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 78, 40, 18).build();
        this.addRenderableWidget(blueBtn);
        Button whiteBtn = Button.builder(Component.literal("Бел"), (b) -> { this.hexField.setValue("#FFFFFF"); hudColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 78, 40, 18).build();
        this.addRenderableWidget(whiteBtn);

        EditBox hudBgColorField = new EditBox(this.font, centerX - 130, panelY + 126, 80, 18,
                Component.literal("#RRGGBB"));
        hudBgColorField.setMaxLength(7);
        hudBgColorField.setValue(String.format("#%06X", hudBackgroundColor & 0xFFFFFF));
        this.addRenderableWidget(hudBgColorField);

        Button applyHudBgColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = hudBgColorField.getValue().replace("#", "").trim();
            try {
                hudBackgroundColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 126, 40, 18).build();
        this.addRenderableWidget(applyHudBgColorBtn);

        Button hudBgGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { hudBgColorField.setValue("#00FF00"); hudBackgroundColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgGreenBtn);
        Button hudBgRedBtn = Button.builder(Component.literal("Крас"), (b) -> { hudBgColorField.setValue("#FF0000"); hudBackgroundColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgRedBtn);
        Button hudBgBlueBtn = Button.builder(Component.literal("Син"), (b) -> { hudBgColorField.setValue("#0000FF"); hudBackgroundColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgBlueBtn);
        Button hudBgBlackBtn = Button.builder(Component.literal("Чёрн"), (b) -> { hudBgColorField.setValue("#000000"); hudBackgroundColor = 0xFF000000; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgBlackBtn);

        EditBox guiColorField = new EditBox(this.font, centerX - 130, panelY + 174, 80, 18,
                Component.literal("#RRGGBB"));
        guiColorField.setMaxLength(7);
        guiColorField.setValue(String.format("#%06X", guiColor & 0xFFFFFF));
        this.addRenderableWidget(guiColorField);

        Button applyGuiColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = guiColorField.getValue().replace("#", "").trim();
            try {
                guiColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 174, 40, 18).build();
        this.addRenderableWidget(applyGuiColorBtn);

        Button guiGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiColorField.setValue("#00FF00"); guiColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiGreenBtn);
        Button guiRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiColorField.setValue("#FF0000"); guiColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiRedBtn);
        Button guiBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiColorField.setValue("#0000FF"); guiColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiBlueBtn);

        EditBox guiTextColorField = new EditBox(this.font, centerX - 130, panelY + 222, 80, 18,
                Component.literal("#RRGGBB"));
        guiTextColorField.setMaxLength(7);
        guiTextColorField.setValue(String.format("#%06X", guiTextColor & 0xFFFFFF));
        this.addRenderableWidget(guiTextColorField);

        Button applyGuiTextColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = guiTextColorField.getValue().replace("#", "").trim();
            try {
                guiTextColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 222, 40, 18).build();
        this.addRenderableWidget(applyGuiTextColorBtn);

        Button textGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiTextColorField.setValue("#00FF00"); guiTextColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textGreenBtn);
        Button textRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiTextColorField.setValue("#FF0000"); guiTextColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textRedBtn);
        Button textBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiTextColorField.setValue("#0000FF"); guiTextColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textBlueBtn);

        Button resetColorsBtn = Button.builder(Component.literal("Сбросить цвета на Vanilla"), (b) -> {
            guiColor = 0xFF00FF00;
            guiTextColor = 0xFFFFFFFF;
            hudColor = 0xFF00FF00;
            hudBackgroundColor = 0xFF000000;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 270, 200, 20).build();
        this.addRenderableWidget(resetColorsBtn);
    }

    private void initPage0(int centerX, int panelY) {
        Checkbox hudCheckbox = Checkbox.builder(Component.literal("Показывать HUD"), this.font)
                .pos(centerX - 100, panelY + 45).selected(showHud)
                .onValueChange((c, v) -> { showHud = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudCheckbox);

        Checkbox hudBgCheckbox = Checkbox.builder(Component.literal("Фон HUD"), this.font)
                .pos(centerX + 30, panelY + 45).selected(hudBackgroundEnabled)
                .onValueChange((c, v) -> { hudBackgroundEnabled = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudBgCheckbox);

        this.hexField = new EditBox(this.font, centerX - 130, panelY + 78, 80, 18,
                Component.literal("#RRGGBB"));
        this.hexField.setMaxLength(7);
        this.hexField.setValue(String.format("#%06X", hudColor & 0xFFFFFF));
        this.addRenderableWidget(this.hexField);

        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = this.hexField.getValue().replace("#", "").trim();
            try {
                hudColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 78, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button greenBtn = Button.builder(Component.literal("Зел"), (b) -> { this.hexField.setValue("#00FF00"); hudColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 78, 40, 18).build();
        this.addRenderableWidget(greenBtn);
        Button redBtn = Button.builder(Component.literal("Крас"), (b) -> { this.hexField.setValue("#FF0000"); hudColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 78, 40, 18).build();
        this.addRenderableWidget(redBtn);
        Button blueBtn = Button.builder(Component.literal("Син"), (b) -> { this.hexField.setValue("#0000FF"); hudColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 78, 40, 18).build();
        this.addRenderableWidget(blueBtn);
        Button whiteBtn = Button.builder(Component.literal("Бел"), (b) -> { this.hexField.setValue("#FFFFFF"); hudColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 78, 40, 18).build();
        this.addRenderableWidget(whiteBtn);

        EditBox hudBgColorField = new EditBox(this.font, centerX - 130, panelY + 126, 80, 18,
                Component.literal("#RRGGBB"));
        hudBgColorField.setMaxLength(7);
        hudBgColorField.setValue(String.format("#%06X", hudBackgroundColor & 0xFFFFFF));
        this.addRenderableWidget(hudBgColorField);

        Button applyHudBgColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = hudBgColorField.getValue().replace("#", "").trim();
            try {
                hudBackgroundColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 126, 40, 18).build();
        this.addRenderableWidget(applyHudBgColorBtn);

        Button hudBgGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { hudBgColorField.setValue("#00FF00"); hudBackgroundColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgGreenBtn);
        Button hudBgRedBtn = Button.builder(Component.literal("Крас"), (b) -> { hudBgColorField.setValue("#FF0000"); hudBackgroundColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgRedBtn);
        Button hudBgBlueBtn = Button.builder(Component.literal("Син"), (b) -> { hudBgColorField.setValue("#0000FF"); hudBackgroundColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgBlueBtn);
        Button hudBgBlackBtn = Button.builder(Component.literal("Чёрн"), (b) -> { hudBgColorField.setValue("#000000"); hudBackgroundColor = 0xFF000000; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgBlackBtn);

        EditBox guiColorField = new EditBox(this.font, centerX - 130, panelY + 174, 80, 18,
                Component.literal("#RRGGBB"));
        guiColorField.setMaxLength(7);
        guiColorField.setValue(String.format("#%06X", guiColor & 0xFFFFFF));
        this.addRenderableWidget(guiColorField);

        Button applyGuiColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = guiColorField.getValue().replace("#", "").trim();
            try {
                guiColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 174, 40, 18).build();
        this.addRenderableWidget(applyGuiColorBtn);

        Button guiGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiColorField.setValue("#00FF00"); guiColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiGreenBtn);
        Button guiRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiColorField.setValue("#FF0000"); guiColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiRedBtn);
        Button guiBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiColorField.setValue("#0000FF"); guiColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiBlueBtn);

        EditBox guiTextColorField = new EditBox(this.font, centerX - 130, panelY + 222, 80, 18,
                Component.literal("#RRGGBB"));
        guiTextColorField.setMaxLength(7);
        guiTextColorField.setValue(String.format("#%06X", guiTextColor & 0xFFFFFF));
        this.addRenderableWidget(guiTextColorField);

        Button applyGuiTextColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = guiTextColorField.getValue().replace("#", "").trim();
            try {
                guiTextColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 222, 40, 18).build();
        this.addRenderableWidget(applyGuiTextColorBtn);

        Button textGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiTextColorField.setValue("#00FF00"); guiTextColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textGreenBtn);
        Button textRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiTextColorField.setValue("#FF0000"); guiTextColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textRedBtn);
        Button textBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiTextColorField.setValue("#0000FF"); guiTextColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textBlueBtn);

        makePosEditor(centerX, panelY, 268,
                () -> coordsX, () -> coordsY,
                (x, y) -> { coordsX = x; coordsY = y; },
                10, 35);

        makePosEditor(centerX, panelY, 305,
                () -> biomeX, () -> biomeY,
                (x, y) -> { biomeX = x; biomeY = y; },
                10, 50);

        makePosEditor(centerX, panelY, 342,
                () -> timeX, () -> timeY,
                (x, y) -> { timeX = x; timeY = y; },
                10, 65);

        Button resetAllBtn = Button.builder(Component.literal("Сбросить всё"), (b) -> {
            coordsX = 10; coordsY = 35; biomeX = 10; biomeY = 50; timeX = 10; timeY = 65;
            fpsX = 10; fpsY = 80; pingX = 10; pingY = 95; tpsX = 10; tpsY = 110;
            bpsX = 10; bpsY = 125; directionX = 10; directionY = 140;
            hitCounterX = 10; hitCounterY = 155;
            potionEffectsX = 10; potionEffectsY = 170;
            equipmentHudX = 4; equipmentHudY = -44;
            modLogoX = 10; modLogoY = 5;
            comboX = 10; comboY = 185;
            effectWarningsX = 300; effectWarningsY = 200;
            crosshairSize = 10; crosshairThickness = 2; crosshairGap = 3; crosshairAlpha = 255;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 375, 200, 18).build();
        this.addRenderableWidget(resetAllBtn);
    }

    private void initPage1(int centerX, int panelY) {
        Checkbox coordsCheckbox = Checkbox.builder(Component.literal("Показывать координаты"), this.font)
                .pos(centerX - 100, panelY + 60).selected(showCoords)
                .onValueChange((c, v) -> { showCoords = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(coordsCheckbox);

        Checkbox biomeCheckbox = Checkbox.builder(Component.literal("Показывать биом"), this.font)
                .pos(centerX - 100, panelY + 82).selected(showBiome)
                .onValueChange((c, v) -> { showBiome = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(biomeCheckbox);

        Checkbox timeCheckbox = Checkbox.builder(Component.literal("Показывать время"), this.font)
                .pos(centerX - 100, panelY + 104).selected(showTime)
                .onValueChange((c, v) -> { showTime = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(timeCheckbox);

        Checkbox modLogoCheckbox = Checkbox.builder(
                        Component.literal(modLogoRussian ? "Показывать иконку мода" : "Show mod logo"), this.font)
                .pos(centerX - 100, panelY + 126).selected(showModLogo)
                .onValueChange((c, v) -> { showModLogo = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(modLogoCheckbox);

        Button modLogoTranslate = Button.builder(Component.literal("RU"), (b) -> {
            modLogoRussian = !modLogoRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 126, 25, 20).build();
        this.addRenderableWidget(modLogoTranslate);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 178, 200, 20,
                Component.literal("Прозрачность текста HUD: " + hudAlpha), hudAlpha / 255.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Прозрачность текста HUD: " + hudAlpha)); }
            @Override protected void applyValue() { hudAlpha = (int)(this.value * 255); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(alphaSlider);

        AbstractSliderButton bgAlphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 208, 200, 20,
                Component.literal("Прозрачность фона HUD: " + hudBackgroundAlpha), hudBackgroundAlpha / 255.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Прозрачность фона HUD: " + hudBackgroundAlpha)); }
            @Override protected void applyValue() { hudBackgroundAlpha = (int)(this.value * 255); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(bgAlphaSlider);

        AbstractSliderButton bgHeightSlider = new AbstractSliderButton(
                centerX - 100, panelY + 238, 200, 20,
                Component.literal("Высота фона HUD: " + hudBackgroundHeight),
                (hudBackgroundHeight - 6) / 24.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Высота фона HUD: " + hudBackgroundHeight)); }
            @Override protected void applyValue() { hudBackgroundHeight = 6 + (int)(this.value * 24); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(bgHeightSlider);
    }

    private void initPage2(int centerX, int panelY) {
        String keyName = KeyBindings.openGuiKey != null
                ? KeyBindings.openGuiKey.getTranslatedKeyMessage().getString() : "G";

        Button keyBindButton = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 0 ? "Нажмите клавишу..." : "Клавиша: " + keyName),
                        (b) -> { isBindingKey = true; bindingTarget = 0; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 100, 200, 20).build();
        this.addRenderableWidget(keyBindButton);

        Button resetKeyBtn = Button.builder(Component.literal("Сбросить на G"), (b) -> {
            KeyBindings.setKey(GLFW.GLFW_KEY_G);
            isBindingKey = false;
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 140, 200, 20).build();
        this.addRenderableWidget(resetKeyBtn);
    }

    private void initPage3(int centerX, int panelY) {
        // ===== FPS =====
        Checkbox fpsCheckbox = Checkbox.builder(Component.literal(fpsRussian ? "КВС" : "FPS"), this.font)
                .pos(centerX - 100, panelY + 45).selected(showFps)
                .onValueChange((c, v) -> { showFps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(fpsCheckbox);
        Button fpsTranslate = Button.builder(Component.literal("RU"), (b) -> { fpsRussian = !fpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 45, 25, 20).build();
        this.addRenderableWidget(fpsTranslate);

        makePosEditor(centerX, panelY, 70,
                () -> fpsX, () -> fpsY,
                (x, y) -> { fpsX = x; fpsY = y; },
                10, 80);

        // ===== PING =====
        Checkbox pingCheckbox = Checkbox.builder(Component.literal(pingRussian ? "Пинг" : "Ping"), this.font)
                .pos(centerX - 100, panelY + 112).selected(showPing)
                .onValueChange((c, v) -> { showPing = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(pingCheckbox);
        Button pingTranslate = Button.builder(Component.literal("RU"), (b) -> { pingRussian = !pingRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 112, 25, 20).build();
        this.addRenderableWidget(pingTranslate);

        makePosEditor(centerX, panelY, 137,
                () -> pingX, () -> pingY,
                (x, y) -> { pingX = x; pingY = y; },
                10, 95);

        // ===== TPS =====
        Checkbox tpsCheckbox = Checkbox.builder(Component.literal(tpsRussian ? "ТВС" : "TPS"), this.font)
                .pos(centerX - 100, panelY + 179).selected(showTps)
                .onValueChange((c, v) -> { showTps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(tpsCheckbox);
        Button tpsTranslate = Button.builder(Component.literal("RU"), (b) -> { tpsRussian = !tpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 179, 25, 20).build();
        this.addRenderableWidget(tpsTranslate);

        makePosEditor(centerX, panelY, 204,
                () -> tpsX, () -> tpsY,
                (x, y) -> { tpsX = x; tpsY = y; },
                10, 110);

        // ===== BPS =====
        Checkbox bpsCheckbox = Checkbox.builder(Component.literal(bpsRussian ? "БВС" : "BPS"), this.font)
                .pos(centerX - 100, panelY + 246).selected(showBps)
                .onValueChange((c, v) -> { showBps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(bpsCheckbox);
        Button bpsTranslate = Button.builder(Component.literal("RU"), (b) -> { bpsRussian = !bpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 246, 25, 20).build();
        this.addRenderableWidget(bpsTranslate);

        makePosEditor(centerX, panelY, 271,
                () -> bpsX, () -> bpsY,
                (x, y) -> { bpsX = x; bpsY = y; },
                10, 125);

        // ===== DIRECTION =====
        Checkbox dirCheckbox = Checkbox.builder(Component.literal(directionRussian ? "Направление" : "Direction"), this.font)
                .pos(centerX - 100, panelY + 313).selected(showDirection)
                .onValueChange((c, v) -> { showDirection = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(dirCheckbox);
        Button dirTranslate = Button.builder(Component.literal("RU"), (b) -> { directionRussian = !directionRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 313, 25, 20).build();
        this.addRenderableWidget(dirTranslate);

        makePosEditor(centerX, panelY, 338,
                () -> directionX, () -> directionY,
                (x, y) -> { directionX = x; directionY = y; },
                10, 140);

        // ===== HITS =====
        Checkbox hitCheckbox = Checkbox.builder(
                        Component.literal(hitCounterRussian ? "Удары до смерти" : "Hits to kill"), this.font)
                .pos(centerX - 100, panelY + 380).selected(showHitCounter)
                .onValueChange((c, v) -> { showHitCounter = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hitCheckbox);
        Button hitTranslate = Button.builder(Component.literal("RU"), (b) -> { hitCounterRussian = !hitCounterRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 380, 25, 20).build();
        this.addRenderableWidget(hitTranslate);
    }

    private void initPage3b(int centerX, int panelY) {
        Checkbox autoSprintCheckbox = Checkbox.builder(
                        Component.literal(autoSprintRussian ? "Включить AutoSprint" : "Enable AutoSprint"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(autoSprintEnabled)
                .onValueChange((c, v) -> { autoSprintEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(autoSprintCheckbox);

        Button sprintTranslate = Button.builder(Component.literal("RU"), (b) -> {
            autoSprintRussian = !autoSprintRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(sprintTranslate);
    }

    private void initPage4(int centerX, int panelY) {
        Checkbox tapeMouseCheckbox = Checkbox.builder(
                        Component.literal(tapeMouseRussian ? "Включить TapeMouse" : "Enable TapeMouse"), this.font)
                .pos(centerX - 100, panelY + 75)
                .selected(tapeMouseEnabled)
                .onValueChange((c, v) -> { tapeMouseEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(tapeMouseCheckbox);

        Button tapeMouseTranslate = Button.builder(Component.literal("RU"), (b) -> {
            tapeMouseRussian = !tapeMouseRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 145, panelY + 75, 25, 20).build();
        this.addRenderableWidget(tapeMouseTranslate);

        Button buttonModeBtn = Button.builder(
                Component.literal(getButtonName(tapeMouseButton, tapeMouseRussian)),
                (b) -> {
                    tapeMouseButton = (tapeMouseButton + 1) % 2;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 100, 200, 20).build();
        this.addRenderableWidget(buttonModeBtn);

        if (tapeMouseButton == 0) {
            Button targetBtn = Button.builder(
                    Component.literal(getTargetName(tapeMouseTarget, tapeMouseRussian)),
                    (b) -> {
                        tapeMouseTarget = (tapeMouseTarget + 1) % 3;
                        ConfigManager.save();
                        this.rebuildWidgets();
                    }
            ).bounds(centerX - 100, panelY + 130, 200, 20).build();
            this.addRenderableWidget(targetBtn);

            Checkbox requireTargetCheckbox = Checkbox.builder(
                            Component.literal(tapeMouseRussian ? "Бить только при наведении" : "Only when aiming at target"), this.font)
                    .pos(centerX - 100, panelY + 158)
                    .selected(tapeMouseRequireTarget)
                    .onValueChange((c, v) -> { tapeMouseRequireTarget = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(requireTargetCheckbox);

            Checkbox requireFullAttackCheckbox = Checkbox.builder(
                            Component.literal(tapeMouseRussian ? "Бить только при заряженной атаке" : "Only on full attack charge"), this.font)
                    .pos(centerX - 100, panelY + 181)
                    .selected(tapeMouseRequireFullAttack)
                    .onValueChange((c, v) -> { tapeMouseRequireFullAttack = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(requireFullAttackCheckbox);
        } else {
            Checkbox holdRightCheckbox = Checkbox.builder(
                            Component.literal(tapeMouseRussian ? "Зажать ПКМ" : "Hold RMB"), this.font)
                    .pos(centerX - 100, panelY + 160)
                    .selected(tapeMouseHoldRight)
                    .onValueChange((c, v) -> { tapeMouseHoldRight = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(holdRightCheckbox);
        }

        String tmKeyName = KeyBindings.tapeMouseKey != null
                ? KeyBindings.tapeMouseKey.getTranslatedKeyMessage().getString() : "R";

        Button tapeMouseKeyBindButton = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 2 ? "Нажмите клавишу..." : "Клавиша TapeMouse: " + tmKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 2; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 225, 200, 20).build();
        this.addRenderableWidget(tapeMouseKeyBindButton);

        AbstractSliderButton delaySlider = new AbstractSliderButton(
                centerX - 100, panelY + 265, 200, 20,
                Component.literal(getDelayText()),
                (tapeMouseDelay - 0.1f) / 4.9f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(getDelayText())); }
            @Override
            protected void applyValue() {
                tapeMouseDelay = 0.1f + (float) (this.value * 4.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(delaySlider);
    }

    private void initPage5(int centerX, int panelY) {
        Checkbox aspectCheckbox = Checkbox.builder(
                        Component.literal(aspectRatioRussian ? "Включить растяг" : "Enable stretch"), this.font)
                .pos(centerX - 100, panelY + 70)
                .selected(aspectRatioEnabled)
                .onValueChange((c, v) -> { aspectRatioEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(aspectCheckbox);

        Button aspectTranslate = Button.builder(Component.literal("RU"), (b) -> {
            aspectRatioRussian = !aspectRatioRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 70, 25, 20).build();
        this.addRenderableWidget(aspectTranslate);

        AbstractSliderButton ratioSlider = new AbstractSliderButton(
                centerX - 100, panelY + 110, 200, 20,
                Component.literal(getAspectRatioText()),
                (aspectRatio - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(getAspectRatioText())); }
            @Override
            protected void applyValue() {
                aspectRatio = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(ratioSlider);

        Button preset4x3 = Button.builder(Component.literal("4:3"), (b) -> { aspectRatio = 1.33f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 100, panelY + 150, 60, 20).build();
        this.addRenderableWidget(preset4x3);
        Button preset16x9 = Button.builder(Component.literal("16:9"), (b) -> { aspectRatio = 1.0f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 35, panelY + 150, 60, 20).build();
        this.addRenderableWidget(preset16x9);
        Button preset21x9 = Button.builder(Component.literal("21:9"), (b) -> { aspectRatio = 0.75f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 30, panelY + 150, 60, 20).build();
        this.addRenderableWidget(preset21x9);
        Button presetSquare = Button.builder(Component.literal("1:1"), (b) -> { aspectRatio = 1.78f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 95, panelY + 150, 60, 20).build();
        this.addRenderableWidget(presetSquare);

        Button resetBtn = Button.builder(Component.literal(aspectRatioRussian ? "Сбросить" : "Reset"), (b) -> {
            aspectRatio = 1.0f; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 190, 200, 20).build();
        this.addRenderableWidget(resetBtn);
    }

    private void initPage6(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(customHitSoundsRussian ? "Включить кастомные звуки" : "Enable custom hit sounds"), this.font)
                .pos(centerX - 100, panelY + 60)
                .selected(customHitSoundsEnabled)
                .onValueChange((c, v) -> { customHitSoundsEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            customHitSoundsRussian = !customHitSoundsRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 60, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String chsKeyName = KeyBindings.customHitSoundsKey != null
                ? KeyBindings.customHitSoundsKey.getTranslatedKeyMessage().getString() : "J";
        Button keyBindBtn = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 4 ? "Нажмите клавишу..." : "Клавиша: " + chsKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 4; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 95, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        for (int i = 1; i <= 7; i++) {
            final int preset = i;
            Button soundBtn = Button.builder(Component.literal(String.valueOf(i)),
                            (b) -> {
                                customHitSoundPreset = preset;
                                ConfigManager.save();
                                this.rebuildWidgets();
                            })
                    .bounds(centerX - 100 + (i - 1) * 30, panelY + 130, 25, 20)
                    .build();
            this.addRenderableWidget(soundBtn);
        }

        AbstractSliderButton volumeSlider = new AbstractSliderButton(
                centerX - 100, panelY + 170, 200, 20,
                Component.literal(String.format("Громкость: %.1f", customHitSoundVolume)),
                (customHitSoundVolume - 0.1f) / 1.9f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(String.format("Громкость: %.1f", customHitSoundVolume))); }
            @Override
            protected void applyValue() {
                customHitSoundVolume = 0.1f + (float) (this.value * 1.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(volumeSlider);

        AbstractSliderButton pitchSlider = new AbstractSliderButton(
                centerX - 100, panelY + 210, 200, 20,
                Component.literal(String.format("Тон: %.1f", customHitSoundPitch)),
                (customHitSoundPitch - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(String.format("Тон: %.1f", customHitSoundPitch))); }
            @Override
            protected void applyValue() {
                customHitSoundPitch = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(pitchSlider);
    }
    private void initPage7(int centerX, int panelY) {
        Checkbox potionCheckbox = Checkbox.builder(
                        Component.literal(potionEffectsRussian ? "Показывать эффекты" : "Show Effects"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(showPotionEffects)
                .onValueChange((c, v) -> { showPotionEffects = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(potionCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            potionEffectsRussian = !potionEffectsRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        Checkbox iconsCheckbox = Checkbox.builder(
                        Component.literal(potionEffectsRussian ? "Показывать иконки" : "Show icons"), this.font)
                .pos(centerX - 100, panelY + 110)
                .selected(potionEffectsIcons)
                .onValueChange((c, v) -> { potionEffectsIcons = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(iconsCheckbox);

        makePosEditor(centerX, panelY, 150,
                () -> potionEffectsX, () -> potionEffectsY,
                (x, y) -> { potionEffectsX = x; potionEffectsY = y; },
                10, 170);
    }

    private void initPage8(int centerX, int panelY) {
        Checkbox equipCheckbox = Checkbox.builder(
                        Component.literal(equipmentHudRussian ? "Показывать экипировку" : "Show equipment"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(showEquipmentHud)
                .onValueChange((c, v) -> { showEquipmentHud = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(equipCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            equipmentHudRussian = !equipmentHudRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        Checkbox durabilityCheckbox = Checkbox.builder(
                        Component.literal(equipmentHudRussian ? "Показывать прочность" : "Show durability"), this.font)
                .pos(centerX - 100, panelY + 110)
                .selected(equipmentShowDurability)
                .onValueChange((c, v) -> { equipmentShowDurability = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(durabilityCheckbox);

        makePosEditor(centerX, panelY, 155,
                () -> equipmentHudX, () -> equipmentHudY,
                (x, y) -> { equipmentHudX = x; equipmentHudY = y; },
                4, -44);
    }

    private void initPage9(int centerX, int panelY) {
        Checkbox fireCheckbox = Checkbox.builder(
                        Component.literal(lowFireShieldRussian ? "Низкий огонь" : "Low Fire"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(lowFireEnabled)
                .onValueChange((c, v) -> { lowFireEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(fireCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            lowFireShieldRussian = !lowFireShieldRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        AbstractSliderButton fireSlider = new AbstractSliderButton(
                centerX - 100, panelY + 115, 200, 20,
                Component.literal(String.format(lowFireShieldRussian ? "Смещение огня: %.2f" : "Fire offset: %.2f", lowFireOffset)),
                lowFireOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(lowFireShieldRussian ? "Смещение огня: %.2f" : "Fire offset: %.2f", lowFireOffset)));
            }
            @Override protected void applyValue() {
                lowFireOffset = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(fireSlider);

        Checkbox shieldCheckbox = Checkbox.builder(
                        Component.literal(lowFireShieldRussian ? "Низкий щит" : "Low Shield"), this.font)
                .pos(centerX - 100, panelY + 160)
                .selected(lowShieldEnabled)
                .onValueChange((c, v) -> { lowShieldEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(shieldCheckbox);

        AbstractSliderButton shieldSlider = new AbstractSliderButton(
                centerX - 100, panelY + 195, 200, 20,
                Component.literal(String.format(lowFireShieldRussian ? "Смещение щита: %.2f" : "Shield offset: %.2f", lowShieldOffset)),
                lowShieldOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(lowFireShieldRussian ? "Смещение щита: %.2f" : "Shield offset: %.2f", lowShieldOffset)));
            }
            @Override protected void applyValue() {
                lowShieldOffset = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(shieldSlider);
    }

    private void initPage10(int centerX, int panelY) {
        Checkbox zoomCheckbox = Checkbox.builder(
                        Component.literal(zoomRussian ? "Включить Zoom" : "Enable Zoom"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(zoomEnabled)
                .onValueChange((c, v) -> { zoomEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(zoomCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            zoomRussian = !zoomRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String keyName = KeyBindings.zoomKey != null
                ? KeyBindings.zoomKey.getTranslatedKeyMessage().getString() : "C";

        Button zoomKeyBindButton = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 1 ? "Нажмите клавишу..." : "Клавиша зума: " + keyName),
                        (b) -> { isBindingKey = true; bindingTarget = 1; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(zoomKeyBindButton);

        AbstractSliderButton factorSlider = new AbstractSliderButton(
                centerX - 100, panelY + 160, 200, 20,
                Component.literal(String.format(zoomRussian ? "Сила зума: %.1fx" : "Zoom factor: %.1fx", zoomFactor)),
                (zoomFactor - 1.5f) / 8.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(zoomRussian ? "Сила зума: %.1fx" : "Zoom factor: %.1fx", zoomFactor)));
            }
            @Override protected void applyValue() {
                zoomFactor = 1.5f + (float) (this.value * 8.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(factorSlider);

        AbstractSliderButton smoothSlider = new AbstractSliderButton(
                centerX - 100, panelY + 200, 200, 20,
                Component.literal(String.format(zoomRussian ? "Плавность: %.2f" : "Smoothness: %.2f", zoomSmoothness)),
                (zoomSmoothness - 0.05f) / 0.95f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(zoomRussian ? "Плавность: %.2f" : "Smoothness: %.2f", zoomSmoothness)));
            }
            @Override protected void applyValue() {
                zoomSmoothness = 0.05f + (float) (this.value * 0.95f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(smoothSlider);
    }

    private void initPage11(int centerX, int panelY) {
        Checkbox swapCheckbox = Checkbox.builder(
                        Component.literal(autoSwapRussian ? "Включить Автосвап" : "Enable AutoSwap"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(autoSwapEnabled)
                .onValueChange((c, v) -> { autoSwapEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(swapCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            autoSwapRussian = !autoSwapRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        int modeW = 95;
        Button mode0 = Button.builder(Component.literal("Шар ↔ Шар"),
                        (b) -> { autoSwapMode = 0; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 100, panelY + 120, modeW, 20).build();
        this.addRenderableWidget(mode0);
        Button mode1 = Button.builder(Component.literal("Тотем ↔ Тотем"),
                        (b) -> { autoSwapMode = 1; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 5, panelY + 120, modeW, 20).build();
        this.addRenderableWidget(mode1);
        Button mode2 = Button.builder(Component.literal("Шар ↔ Тотем"),
                        (b) -> { autoSwapMode = 2; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 100, panelY + 145, modeW, 20).build();
        this.addRenderableWidget(mode2);
        Button mode3 = Button.builder(Component.literal("Тотем ↔ Шар"),
                        (b) -> { autoSwapMode = 3; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 5, panelY + 145, modeW, 20).build();
        this.addRenderableWidget(mode3);

        String swapKeyName = KeyBindings.autoSwapKey != null
                ? KeyBindings.autoSwapKey.getTranslatedKeyMessage().getString() : "H";

        Button swapKeyBindButton = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 3 ? "Нажмите клавишу..." : "Клавиша Автосвапа: " + swapKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 3; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 185, 200, 20).build();
        this.addRenderableWidget(swapKeyBindButton);

        AbstractSliderButton openDelaySlider = new AbstractSliderButton(
                centerX - 100, panelY + 220, 200, 20,
                Component.literal(String.format(autoSwapRussian ? "Задержка открытия: %d мс" : "Open delay: %d ms", autoSwapOpenDelay)),
                (autoSwapOpenDelay - 50) / 450.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(autoSwapRussian ? "Задержка открытия: %d мс" : "Open delay: %d ms", autoSwapOpenDelay)));
            }
            @Override protected void applyValue() {
                autoSwapOpenDelay = 50 + (int)(this.value * 450);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(openDelaySlider);

        AbstractSliderButton cooldownSlider = new AbstractSliderButton(
                centerX - 100, panelY + 250, 200, 20,
                Component.literal(String.format(autoSwapRussian ? "Cooldown: %d мс" : "Cooldown: %d ms", autoSwapCooldown)),
                (autoSwapCooldown - 100) / 1900.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(autoSwapRussian ? "Cooldown: %d мс" : "Cooldown: %d ms", autoSwapCooldown)));
            }
            @Override protected void applyValue() {
                autoSwapCooldown = 100 + (int)(this.value * 1900);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(cooldownSlider);
    }

    private void initPage12(int centerX, int panelY) {
        Checkbox fastExpCheckbox = Checkbox.builder(
                        Component.literal(fastExpRussian ? "Включить FastExp" : "Enable FastExp"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(fastExpEnabled)
                .onValueChange((c, v) -> { fastExpEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(fastExpCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            fastExpRussian = !fastExpRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String feKeyName = KeyBindings.fastExpKey != null
                ? KeyBindings.fastExpKey.getTranslatedKeyMessage().getString() : "K";
        Button keyBindBtn = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 5 ? "Нажмите клавишу..." : "Клавиша: " + feKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 5; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);
    }

    private void initPage13(int centerX, int panelY) {
        Checkbox shiftTapCheckbox = Checkbox.builder(
                        Component.literal(shiftTapRussian ? "Включить ShiftTap" : "Enable ShiftTap"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(shiftTapEnabled)
                .onValueChange((c, v) -> { shiftTapEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(shiftTapCheckbox);

        Button shiftTranslate = Button.builder(Component.literal("RU"), (b) -> {
            shiftTapRussian = !shiftTapRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(shiftTranslate);

        String stKeyName = KeyBindings.shiftTapKey != null
                ? KeyBindings.shiftTapKey.getTranslatedKeyMessage().getString() : "L";
        Button keyBindBtn = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 6 ? "Нажмите клавишу..." : "Клавиша: " + stKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 6; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);
    }
    private void initPage14(int centerX, int panelY) {
        Checkbox comboCheckbox = Checkbox.builder(
                        Component.literal(comboRussian ? "Включить Combo Counter" : "Enable Combo Counter"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(comboEnabled)
                .onValueChange((c, v) -> { comboEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(comboCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            comboRussian = !comboRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String cbKeyName = KeyBindings.comboKey != null
                ? KeyBindings.comboKey.getTranslatedKeyMessage().getString() : "M";
        Button keyBindBtn = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 7 ? "Нажмите клавишу..." : "Клавиша: " + cbKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 7; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        makePosEditor(centerX, panelY, 160,
                () -> comboX, () -> comboY,
                (x, y) -> { comboX = x; comboY = y; },
                10, 185);

        AbstractSliderButton resetSlider = new AbstractSliderButton(
                centerX - 100, panelY + 200, 200, 20,
                Component.literal(comboRussian ? ("Время сброса: " + comboResetTime + " сек") : ("Reset time: " + comboResetTime + " sec")),
                (comboResetTime - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(comboRussian ? ("Время сброса: " + comboResetTime + " сек") : ("Reset time: " + comboResetTime + " sec")));
            }
            @Override protected void applyValue() {
                comboResetTime = 1 + (int)(this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(resetSlider);

        String sizeText = comboRussian ? "Размер: " : "Size: ";
        String[] sizesRu = {"Малый", "Средний", "Крупный"};
        String[] sizesEn = {"Small", "Medium", "Large"};
        Button sizeBtn = Button.builder(
                Component.literal(sizeText + (comboRussian ? sizesRu[comboFontSize] : sizesEn[comboFontSize])),
                (b) -> {
                    comboFontSize = (comboFontSize + 1) % 3;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 235, 200, 20).build();
        this.addRenderableWidget(sizeBtn);

        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 275, 80, 18,
                Component.literal("#RRGGBB"));
        colorField.setMaxLength(7);
        colorField.setValue(String.format("#%06X", comboColor & 0xFFFFFF));
        this.addRenderableWidget(colorField);

        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = colorField.getValue().replace("#", "").trim();
            try {
                comboColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 275, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorYellow = Button.builder(Component.literal("Жёлт"), (b) -> { colorField.setValue("#FFFF00"); comboColor = 0xFFFFFF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 275, 45, 18).build();
        this.addRenderableWidget(colorYellow);
        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); comboColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 55, panelY + 275, 45, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorGreen = Button.builder(Component.literal("Зел"), (b) -> { colorField.setValue("#00FF00"); comboColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 105, panelY + 275, 45, 18).build();
        this.addRenderableWidget(colorGreen);
    }

    private void initPage15(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(effectWarningsRussian ? "Включить Effect Warnings" : "Enable Effect Warnings"), this.font)
                .pos(centerX - 100, panelY + 75)
                .selected(effectWarningsEnabled)
                .onValueChange((c, v) -> { effectWarningsEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            effectWarningsRussian = !effectWarningsRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 75, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String ewKeyName = KeyBindings.effectWarningsKey != null
                ? KeyBindings.effectWarningsKey.getTranslatedKeyMessage().getString() : "N";
        Button keyBindBtn = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 8 ? "Нажмите клавишу..." : "Клавиша: " + ewKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 8; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 110, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        AbstractSliderButton thresholdSlider = new AbstractSliderButton(
                centerX - 100, panelY + 145, 200, 20,
                Component.literal(effectWarningsRussian ? ("Порог: " + effectWarningsThreshold + " сек") : ("Threshold: " + effectWarningsThreshold + " sec")),
                (effectWarningsThreshold - 3) / 12.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(effectWarningsRussian ? ("Порог: " + effectWarningsThreshold + " сек") : ("Threshold: " + effectWarningsThreshold + " sec")));
            }
            @Override protected void applyValue() {
                effectWarningsThreshold = 3 + (int)(this.value * 12);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(thresholdSlider);

        makePosEditor(centerX, panelY, 185,
                () -> effectWarningsX, () -> effectWarningsY,
                (x, y) -> { effectWarningsX = x; effectWarningsY = y; },
                300, 200);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 225, 200, 20,
                Component.literal(effectWarningsRussian ? ("Прозрачность: " + effectWarningsAlpha) : ("Alpha: " + effectWarningsAlpha)),
                effectWarningsAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(effectWarningsRussian ? ("Прозрачность: " + effectWarningsAlpha) : ("Alpha: " + effectWarningsAlpha)));
            }
            @Override protected void applyValue() {
                effectWarningsAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(alphaSlider);

        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 265, 80, 18,
                Component.literal("#RRGGBB"));
        colorField.setMaxLength(7);
        colorField.setValue(String.format("#%06X", effectWarningsColor & 0xFFFFFF));
        this.addRenderableWidget(colorField);

        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = colorField.getValue().replace("#", "").trim();
            try {
                effectWarningsColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 265, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); effectWarningsColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 265, 45, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorYellow = Button.builder(Component.literal("Жёлт"), (b) -> { colorField.setValue("#FFFF00"); effectWarningsColor = 0xFFFFFF00; ConfigManager.save(); })
                .bounds(centerX + 55, panelY + 265, 45, 18).build();
        this.addRenderableWidget(colorYellow);
        Button colorWhite = Button.builder(Component.literal("Бел"), (b) -> { colorField.setValue("#FFFFFF"); effectWarningsColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 105, panelY + 265, 45, 18).build();
        this.addRenderableWidget(colorWhite);

        Checkbox showNameCheckbox = Checkbox.builder(
                        Component.literal(effectWarningsRussian ? "Показывать название" : "Show name"), this.font)
                .pos(centerX - 100, panelY + 300)
                .selected(effectWarningsShowName)
                .onValueChange((c, v) -> { effectWarningsShowName = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(showNameCheckbox);

        Checkbox showIconCheckbox = Checkbox.builder(
                        Component.literal(effectWarningsRussian ? "Показывать иконку" : "Show icon"), this.font)
                .pos(centerX + 20, panelY + 300)
                .selected(effectWarningsShowIcon)
                .onValueChange((c, v) -> { effectWarningsShowIcon = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(showIconCheckbox);
    }

    private void initPage16(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(crosshairRussian ? "Включить кастомный прицел" : "Enable custom crosshair"), this.font)
                .pos(centerX - 100, panelY + 45)
                .selected(crosshairEnabled)
                .onValueChange((c, v) -> { crosshairEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            crosshairRussian = !crosshairRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 45, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        Button shapeBtn = Button.builder(
                Component.literal(getCrosshairShapeName(crosshairShape, crosshairRussian)),
                (b) -> {
                    crosshairShape = (crosshairShape + 1) % 5;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 80, 200, 20).build();
        this.addRenderableWidget(shapeBtn);

        AbstractSliderButton sizeSlider = new AbstractSliderButton(
                centerX - 100, panelY + 115, 200, 20,
                Component.literal(crosshairRussian ? ("Размер: " + crosshairSize + " px") : ("Size: " + crosshairSize + " px")),
                (crosshairSize - 4) / 16.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(crosshairRussian ? ("Размер: " + crosshairSize + " px") : ("Size: " + crosshairSize + " px")));
            }
            @Override protected void applyValue() {
                crosshairSize = 4 + (int)(this.value * 16);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(sizeSlider);

        AbstractSliderButton thicknessSlider = new AbstractSliderButton(
                centerX - 100, panelY + 145, 200, 20,
                Component.literal(crosshairRussian ? ("Толщина: " + crosshairThickness + " px") : ("Thickness: " + crosshairThickness + " px")),
                (crosshairThickness - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(crosshairRussian ? ("Толщина: " + crosshairThickness + " px") : ("Thickness: " + crosshairThickness + " px")));
            }
            @Override protected void applyValue() {
                crosshairThickness = 1 + (int)(this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(thicknessSlider);

        AbstractSliderButton gapSlider = new AbstractSliderButton(
                centerX - 100, panelY + 175, 200, 20,
                Component.literal(crosshairRussian ? ("Зазор: " + crosshairGap + " px") : ("Gap: " + crosshairGap + " px")),
                crosshairGap / 10.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(crosshairRussian ? ("Зазор: " + crosshairGap + " px") : ("Gap: " + crosshairGap + " px")));
            }
            @Override protected void applyValue() {
                crosshairGap = (int)(this.value * 10);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(gapSlider);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 205, 200, 20,
                Component.literal(crosshairRussian ? ("Прозрачность: " + crosshairAlpha) : ("Alpha: " + crosshairAlpha)),
                crosshairAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(crosshairRussian ? ("Прозрачность: " + crosshairAlpha) : ("Alpha: " + crosshairAlpha)));
            }
            @Override protected void applyValue() {
                crosshairAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(alphaSlider);

        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 245, 80, 18,
                Component.literal("#RRGGBB"));
        colorField.setMaxLength(7);
        colorField.setValue(String.format("#%06X", crosshairColor & 0xFFFFFF));
        this.addRenderableWidget(colorField);
        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = colorField.getValue().replace("#", "").trim();
            try {
                crosshairColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 245, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorGreen = Button.builder(Component.literal("Зел"), (b) -> { colorField.setValue("#00FF00"); crosshairColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorGreen);
        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); crosshairColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorBlue = Button.builder(Component.literal("Син"), (b) -> { colorField.setValue("#0000FF"); crosshairColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorBlue);
        Button colorWhite = Button.builder(Component.literal("Бел"), (b) -> { colorField.setValue("#FFFFFF"); crosshairColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorWhite);
    }

    /**
     * Visual 3.4 — Темы GUI.
     */
    private void initThemesPage(int centerX, int panelY) {
        int[][] themes = {
                {0xFF00FF00, 0xFFFFFFFF, 0xFF00FF00}, // 0 Vanilla
                {0xFF808080, 0xFFDDDDDD, 0xFF808080}, // 1 Dark
                {0xFF00FFFF, 0xFF00FF00, 0xFF00FFFF}, // 2 Neon
                {0xFFFF69B4, 0xFFFFFFFF, 0xFFFF69B4}, // 3 Candy
                {0xFFFF0000, 0xFFFFFFFF, 0xFFFF0000}, // 4 Blood
        };
        String[] names = {"Vanilla", "Dark", "Neon", "Candy", "Blood"};

        int cardW = 70;
        int cardH = 40;
        int gapX = 8;
        int gapY = 12;
        int cols = 3;
        int startX = centerX - (cols * cardW + (cols - 1) * gapX) / 2;
        int startY = panelY + 80;

        for (int i = 0; i < themes.length; i++) {
            final int idx = i;
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (cardW + gapX);
            int y = startY + row * (cardH + gapY + 12);

            Button themeBtn = Button.builder(Component.literal(names[i]), (b) -> {
                MyCustomScreen.guiColor = themes[idx][0];
                MyCustomScreen.guiTextColor = themes[idx][1];
                MyCustomScreen.hudColor = themes[idx][2];
                ConfigManager.save();
                this.rebuildWidgets();
            }).bounds(x, y, cardW, cardH).build();
            this.addRenderableWidget(themeBtn);
        }

        Button resetBtn = Button.builder(Component.literal("Сбросить на Vanilla"), (b) -> {
            MyCustomScreen.guiColor = 0xFF00FF00;
            MyCustomScreen.guiTextColor = 0xFFFFFFFF;
            MyCustomScreen.hudColor = 0xFF00FF00;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 220, 200, 20).build();
        this.addRenderableWidget(resetBtn);

        int cur = detectCurrentTheme(themes);
        String curText = "Текущая тема: " + names[cur];
        Button curBtn = Button.builder(Component.literal(curText), (b) -> {})
                .bounds(centerX - 100, panelY + 250, 200, 20).build();
        curBtn.active = false;
        this.addRenderableWidget(curBtn);
    }

    /**
     * Visual 3.5 — Waypoints.
     * Форма добавления (X, Y, Z) + список существующих меток.
     *
     * Координаты:
     *   +35  заголовок "Координаты новой метки"
     *   +50  поля X, Y, Z
     *   +75  кнопка "Добавить метку"
     *   +105 кнопка "Очистить все метки"
     *   +130 заголовок "Существующие метки"
     *   +155 список меток (по 22 px на строку)
     */
    private void initWaypointsPage(int centerX, int panelY) {
        // ===== ЗНАЧЕНИЯ ПО УМОЛЧАНИЮ (позиция игрока) =====
        int defaultX = 0;
        int defaultY = 64;
        int defaultZ = 0;
        if (Minecraft.getInstance().player != null) {
            defaultX = (int) Minecraft.getInstance().player.getX();
            defaultY = (int) Minecraft.getInstance().player.getY();
            defaultZ = (int) Minecraft.getInstance().player.getZ();
        }

        // ===== ПОЛЯ ВВОДА КООРДИНАТ =====
        EditBox xField = new EditBox(this.font, centerX - 100, panelY + 50, 55, 18,
                Component.literal("X"));
        xField.setMaxLength(8);
        xField.setValue(String.valueOf(defaultX));
        this.addRenderableWidget(xField);

        EditBox yField = new EditBox(this.font, centerX - 38, panelY + 50, 55, 18,
                Component.literal("Y"));
        yField.setMaxLength(8);
        yField.setValue(String.valueOf(defaultY));
        this.addRenderableWidget(yField);

        EditBox zField = new EditBox(this.font, centerX + 24, panelY + 50, 55, 18,
                Component.literal("Z"));
        zField.setMaxLength(8);
        zField.setValue(String.valueOf(defaultZ));
        this.addRenderableWidget(zField);

        // ===== КНОПКА "ДОБАВИТЬ МЕТКУ" =====
        Button addBtn = Button.builder(Component.literal("Добавить метку"), (b) -> {
            try {
                double x = Double.parseDouble(xField.getValue().trim());
                double y = Double.parseDouble(yField.getValue().trim());
                double z = Double.parseDouble(zField.getValue().trim());

                boolean added = MyCustomScreen.addWaypoint(x, y, z);
                if (added && Minecraft.getInstance().player != null) {
                    int newIndex = MyCustomScreen.getWaypoints().size();
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal(String.format(
                                    "§a[Waypoints] Добавлена метка §6WP%d§a: §e%d, %d, %d",
                                    newIndex, (int) x, (int) y, (int) z
                            )), true);
                } else if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("§c[Waypoints] Достигнут лимит ("
                                    + MyCustomScreen.waypointsMax + ")"), true);
                }
            } catch (NumberFormatException e) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("§c[Waypoints] Неверные координаты"), true);
                }
            }
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 75, 200, 20).build();
        this.addRenderableWidget(addBtn);

        // ===== КНОПКА "ОЧИСТИТЬ ВСЕ" =====
        Button clearBtn = Button.builder(Component.literal("Очистить все метки"), (b) -> {
            MyCustomScreen.clearWaypoints();
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 105, 200, 20).build();
        this.addRenderableWidget(clearBtn);

        // ===== СПИСОК СУЩЕСТВУЮЩИХ МЕТОК =====
        List<MyCustomScreen.Waypoint> waypoints = MyCustomScreen.getWaypoints();

        int listStartY = panelY + 155;
        int rowHeight = 22;

        for (int i = 0; i < waypoints.size(); i++) {
            final int index = i;
            int y = listStartY + i * rowHeight;

            // Кнопка "×" для удаления этой метки
            Button delBtn = Button.builder(Component.literal("×"), (b) -> {
                MyCustomScreen.removeWaypoint(index);
                this.rebuildWidgets();
            }).bounds(centerX + 80, y, 20, 18).build();
            this.addRenderableWidget(delBtn);
        }
    }

    /**
     * Misc 4.1 — Конфигурации (базовая версия).
     */
    private void initConfigsPage(int centerX, int panelY) {
        Button saveBtn = Button.builder(Component.literal("Сохранить как…"), (b) -> {
            ConfigManager.saveAs("default");
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("§aСохранено в default"), true);
            }
        }).bounds(centerX - 100, panelY + 80, 200, 20).build();
        this.addRenderableWidget(saveBtn);

        Button loadBtn = Button.builder(Component.literal("Загрузить default"), (b) -> {
            ConfigManager.loadFrom("default");
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 110, 200, 20).build();
        this.addRenderableWidget(loadBtn);

        Button openBtn = Button.builder(Component.literal("Открыть папку конфигов"), (b) -> {
            ConfigManager.openFolder();
        }).bounds(centerX - 100, panelY + 140, 200, 20).build();
        this.addRenderableWidget(openBtn);

        Button listBtn = Button.builder(Component.literal("Список конфигов"), (b) -> {
            List<String> list = ConfigManager.listConfigs();
            if (Minecraft.getInstance().player != null) {
                if (list.isEmpty()) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("§7Конфигов нет."), false);
                } else {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("§6Конфиги: §e" + String.join(", ", list)), false);
                }
            }
        }).bounds(centerX - 100, panelY + 170, 200, 20).build();
        this.addRenderableWidget(listBtn);

        Button closeBtn = Button.builder(Component.literal("Закрыть"), (b) -> this.onClose())
                .bounds(centerX - 100, panelY + 200, 200, 20).build();
        this.addRenderableWidget(closeBtn);
    }

    private int detectCurrentTheme(int[][] themes) {
        for (int i = 0; i < themes.length; i++) {
            if (MyCustomScreen.guiColor == themes[i][0]
                    && MyCustomScreen.guiTextColor == themes[i][1]
                    && MyCustomScreen.hudColor == themes[i][2]) {
                return i;
            }
        }
        return 0;
    }

    private String getCrosshairShapeName(int shape, boolean russian) {
        if (russian) {
            return switch (shape) {
                case 1 -> "Форма: Точка";
                case 2 -> "Форма: Круг";
                case 3 -> "Форма: Стрелки";
                case 4 -> "Форма: Крест + Точка";
                default -> "Форма: Крест";
            };
        } else {
            return switch (shape) {
                case 1 -> "Shape: Dot";
                case 2 -> "Shape: Circle";
                case 3 -> "Shape: Arrows";
                case 4 -> "Shape: Cross + Dot";
                default -> "Shape: Cross";
            };
        }
    }

    private String getTargetName(int target, boolean russian) {
        if (russian) {
            return switch (target) {
                case 1 -> "Цель: Только мобы";
                case 2 -> "Цель: Только игроки";
                default -> "Цель: Все";
            };
        } else {
            return switch (target) {
                case 1 -> "Target: Mobs only";
                case 2 -> "Target: Players only";
                default -> "Target: All";
            };
        }
    }

    private String getDelayText() {
        return String.format("Задержка: %.1f сек", tapeMouseDelay);
    }

    private String getAspectRatioText() {
        return String.format("Соотношение: %.2f", aspectRatio);
    }

    private String getButtonName(int btn, boolean russian) {
        if (russian) {
            return btn == 0 ? "Кнопка: ЛКМ (атака)" : "Кнопка: ПКМ (использование)";
        } else {
            return btn == 0 ? "Button: LMB (attack)" : "Button: RMB (use)";
        }
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Панель
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, guiColor);
        graphics.fill(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, guiColor);
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelHeight, guiColor);
        graphics.fill(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, guiColor);

        // Поиск
        graphics.drawString(this.font, "§lПоиск:",
                panelX - 180, panelY + 35, guiTextColor);

        drawSearchResults(graphics);

        // Табы — подсветка активного
        int tabX = panelX - 180;
        int tabY = panelY + 185;
        int tabW = 160;
        int tabH = 22;
        int tabGap = 4;

        for (int i = 0; i < SECTION_COUNT; i++) {
            int ty = tabY + i * (tabH + tabGap);
            if (i == currentSection) {
                graphics.fill(tabX, ty, tabX + tabW, ty + tabH, (guiColor & 0x00FFFFFF) | 0x40000000);
                graphics.fill(tabX, ty, tabX + tabW, ty + 1, guiColor);
                graphics.fill(tabX, ty + tabH - 1, tabX + tabW, ty + tabH, guiColor);
                graphics.fill(tabX, ty, tabX + 1, ty + tabH, guiColor);
                graphics.fill(tabX + tabW - 1, ty, tabX + tabW, ty + tabH, guiColor);
            } else {
                graphics.fill(tabX, ty, tabX + tabW, ty + tabH, 0x60000000);
            }
        }

        super.render(graphics, mouseX, mouseY, delta);

        // ===== WAYPOINTS: заголовки и список =====
        // Рисуем поверх кнопок (после super.render), только на Visual 3.5
        if (currentSection == 3 && currentPage == 5) {
            // Заголовок "Координаты новой метки" — на +35, под шапкой панели
            graphics.drawString(this.font, "§l▸ Координаты новой метки",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            // Заголовок "Существующие метки" — на +130, под кнопками
            graphics.drawString(this.font, "§l▸ Существующие метки",
                    panelX + 20, panelY + 130, guiTextColor);
            graphics.fill(panelX + 20, panelY + 142, panelX + panelWidth - 20, panelY + 143, guiColor);

            // Список меток — рисуем текст
            List<MyCustomScreen.Waypoint> waypoints = MyCustomScreen.getWaypoints();

            if (waypoints.isEmpty()) {
                graphics.drawString(this.font, "§7Меток нет. Нажмите B или введите координаты.",
                        panelX + 20, panelY + 158, 0xFFAAAAAA);
            } else {
                int listStartY = panelY + 155;
                int rowHeight = 22;

                for (int i = 0; i < waypoints.size(); i++) {
                    MyCustomScreen.Waypoint wp = waypoints.get(i);
                    int y = listStartY + i * rowHeight;

                    String text = String.format("§e%s §7(%d, %d, %d)",
                            wp.name(), (int) wp.x(), (int) wp.y(), (int) wp.z());
                    graphics.drawString(this.font, text,
                            panelX + 30, y + 5, 0xFFFFFFFF);
                }
            }
        }

        // ===== Плашка "результат в другом разделе" =====
        if (searchOtherSectionMsg != null && !searchOtherSectionMsg.isEmpty()) {
            int msgW = this.font.width(searchOtherSectionMsg) + 16;
            int msgH = 18;
            int msgX = panelX + (panelWidth - msgW) / 2;
            int msgY = panelY + panelHeight - 55;

            graphics.fill(msgX, msgY, msgX + msgW, msgY + msgH, 0xE0000000);
            graphics.fill(msgX, msgY, msgX + msgW, msgY + 1, guiColor);
            graphics.fill(msgX, msgY + msgH - 1, msgX + msgW, msgY + msgH, guiColor);
            graphics.fill(msgX, msgY, msgX + 1, msgY + msgH, guiColor);
            graphics.fill(msgX + msgW - 1, msgY, msgX + msgW, msgY + msgH, guiColor);

            graphics.drawCenteredString(this.font, searchOtherSectionMsg,
                    msgX + msgW / 2, msgY + 5, 0xFFFFFFFF);
        }

        // Пет-иконка
        if (showPet) {
            int petSize = 24;
            int petX = panelX - 45;
            int petY = panelY + 30;
            Identifier petId = Identifier.fromNamespaceAndPath(
                    "resistancedlc", "textures/gui/pet/pet_idle.png"
            );
            Minecraft.getInstance().getTextureManager().getTexture(petId);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    petId,
                    petX, petY,
                    0, 0,
                    petSize, petSize,
                    petSize, petSize
            );
        }

        // Заголовок: раздел + локальный счётчик
        String sectionName = getSectionName(currentSection, modLogoRussian);
        int pagesInSection = SECTION_PAGES[currentSection];
        String title = "Resistance DLC — " + sectionName + " · Стр. " + (currentPage + 1) + " / " + pagesInSection;
        graphics.drawCenteredString(this.font, "§l" + title,
                panelX + panelWidth / 2, panelY + 15, guiColor);

        // Счётчик внизу
        graphics.drawCenteredString(this.font,
                "§7" + sectionName + " · Стр. " + (currentPage + 1) + " / " + pagesInSection,
                panelX + panelWidth / 2, panelY + 405, 0xFFFFFFFF);
    }

    private void drawSearchResults(GuiGraphics graphics) {
        if (this.searchField == null) return;
        String query = this.searchField.getValue().toLowerCase().trim();

        int resultY = panelY + 75;
        int resultWidth = 160;
        int resultHeight = 11;
        int resultX = panelX - 180;

        if (query.isEmpty()) {
            this.searchResults = new ArrayList<>();
            this.searchResultX = -1;
            this.searchResultY = -1;
            this.searchOtherSectionMsg = "";

            List<String> history = getSearchHistory();
            if (history.isEmpty() || !this.searchField.isFocused()) return;

            int maxShow = Math.min(history.size(), 8);

            graphics.drawString(this.font, "§7История:",
                    resultX + 2, panelY + 62, 0xFFAAAAAA);

            graphics.fill(resultX - 5, panelY + 72, resultX + resultWidth + 5,
                    resultY + maxShow * resultHeight + 5, 0xC0000000);

            for (int i = 0; i < maxShow; i++) {
                int y = resultY + i * resultHeight;
                graphics.fill(resultX, y, resultX + resultWidth, y + resultHeight, 0xE0000000);
                String text = "§e" + history.get(i);
                graphics.drawString(this.font, text, resultX + 5, y + 3, 0xFFFFFFFF, false);
            }
            return;
        }

        List<String[]> matches = new ArrayList<>();
        for (String[] entry : SEARCH_INDEX) {
            if (entry[0].contains(query) || entry[1].toLowerCase().contains(query)) {
                boolean duplicate = false;
                for (String[] m : matches) {
                    if (m[1].equals(entry[1])) { duplicate = true; break; }
                }
                if (!duplicate) matches.add(entry);
                if (matches.size() >= 8) break;
            }
        }

        if (matches.isEmpty()) {
            this.searchResults = new ArrayList<>();
            this.searchResultX = -1;
            this.searchResultY = -1;
            return;
        }

        graphics.fill(resultX - 5, panelY + 45, resultX + resultWidth + 5,
                resultY + matches.size() * resultHeight + 5, 0xC0000000);

        for (int i = 0; i < matches.size(); i++) {
            String[] match = matches.get(i);
            int y = resultY + i * resultHeight;
            graphics.fill(resultX, y, resultX + resultWidth, y + resultHeight, 0xE0000000);

            int matchSection = Integer.parseInt(match[3]);
            int matchPage = Integer.parseInt(match[2]);
            String secName = getSectionName(matchSection, modLogoRussian);
            String text = match[1] + " §7· " + secName + " (стр. " + (matchPage + 1) + ")";
            graphics.drawString(this.font, text, resultX + 5, y + 3, 0xFFFFFFFF, false);
        }

        this.searchResults = matches;
        this.searchResultX = resultX;
        this.searchResultY = resultY;
        this.searchResultWidth = resultWidth;
        this.searchResultHeight = resultHeight;
    }

    /**
     * Клик по результатам поиска.
     * Если результат в текущем разделе — переходим на страницу.
     * Если результат в другом разделе — показываем сообщение, не переключаем.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (searchResults != null && !searchResults.isEmpty()
                && searchResultX >= 0 && searchResultY >= 0) {
            for (int i = 0; i < searchResults.size(); i++) {
                int y = searchResultY + i * searchResultHeight;
                if (mouseX >= searchResultX && mouseX <= searchResultX + searchResultWidth
                        && mouseY >= y && mouseY <= y + searchResultHeight) {
                    addSearchHistory(this.searchField.getValue());

                    int targetSection = Integer.parseInt(searchResults.get(i)[3]);
                    int targetPage = Integer.parseInt(searchResults.get(i)[2]);

                    if (targetSection == currentSection) {
                        currentPage = Math.min(targetPage, SECTION_PAGES[currentSection] - 1);
                        this.searchField.setValue("");
                        this.searchResults = new ArrayList<>();
                        this.searchOtherSectionMsg = "";
                        this.rebuildWidgets();
                    } else {
                        String secName = getSectionName(targetSection, modLogoRussian);
                        this.searchOtherSectionMsg = "§eРезультат в разделе §6" + secName
                                + "§e. Переключитесь туда вручную.";
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    private static String getSectionName(int idx, boolean russian) {
        String[] arr = russian ? SECTION_NAMES_RU : SECTION_NAMES_EN;
        if (idx < 0 || idx >= arr.length) return "?";
        return arr[idx];
    }

    private static int totalPages() {
        int sum = 0;
        for (int p : SECTION_PAGES) sum += p;
        return sum;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}