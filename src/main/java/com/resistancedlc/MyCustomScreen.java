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

    // ===== POTION EFFECTS HUD =====
    public static boolean showPotionEffects = false;
    public static int potionEffectsX = 10, potionEffectsY = 170;
    public static boolean potionEffectsRussian = false;
    public static boolean potionEffectsIcons = true;

    // ===== EQUIPMENT HUD =====
    public static boolean showEquipmentHud = false;
    public static int equipmentHudX = 4, equipmentHudY = -44;
    public static boolean equipmentHudRussian = false;
    public static boolean equipmentShowDurability = true;

    // ===== LOW FIRE / LOW SHIELD =====
    public static boolean lowFireEnabled = false;
    public static float lowFireOffset = 0.3f;
    public static boolean lowShieldEnabled = false;
    public static float lowShieldOffset = 0.3f;
    public static boolean lowFireShieldRussian = false;

    // ===== ZOOM =====
    public static boolean zoomEnabled = true;
    public static float zoomFactor = 4.0f;
    public static float zoomSmoothness = 0.25f;
    public static boolean zoomRussian = false;
    public static float currentZoom = 1.0f;

    // ===== TAPEMOUSE =====
    public static boolean tapeMouseEnabled = false;
    public static int tapeMouseTarget = 0;
    public static float tapeMouseDelay = 1.0f;
    public static boolean tapeMouseRussian = false;
    public static boolean tapeMouseRequireTarget = true;
    public static boolean tapeMouseRequireFullAttack = false;

    // ===== AUTOSWAP =====
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

    // ===== FASTEXP =====
    public static boolean fastExpEnabled = false;
    public static boolean fastExpRussian = false;

    // ===== COMBO COUNTER =====
    public static boolean comboEnabled = false;
    public static int comboX = 10, comboY = 185;
    public static int comboColor = 0xFFFFFF00;
    public static int comboResetTime = 3;
    public static int comboFontSize = 1;
    public static boolean comboRussian = false;

    public static int currentCombo = 0;
    public static long lastComboTime = 0;

    // ===== EFFECT WARNINGS =====
    public static boolean effectWarningsEnabled = false;
    public static int effectWarningsX = 300, effectWarningsY = 200;
    public static int effectWarningsColor = 0xFFFF0000;
    public static int effectWarningsThreshold = 10;
    public static int effectWarningsAlpha = 255;
    public static boolean effectWarningsShowName = true;
    public static boolean effectWarningsShowIcon = true;
    public static boolean effectWarningsRussian = false;

    // ===== CROSSHAIR =====
    public static boolean crosshairEnabled = false;
    public static int crosshairColor = 0xFFFFFFFF;
    public static int crosshairSize = 10;
    public static int crosshairThickness = 2;
    public static int crosshairGap = 3;
    public static int crosshairAlpha = 255;
    public static boolean crosshairRussian = false;

    // ===== AUTOSPRINT =====
    public static boolean autoSprintEnabled = false;
    public static boolean autoSprintRussian = false;

    // ===== SHIFTTAP =====
    public static boolean shiftTapEnabled = false;
    public static boolean shiftTapRussian = false;
    public static long shiftTapReleaseTime = 0;
    public static boolean shiftTapActive = false;

    // ===== CUSTOM HIT SOUNDS =====
    public static boolean customHitSoundsEnabled = false;
    public static float customHitSoundVolume = 1.0f;
    public static float customHitSoundPitch = 1.0f;
    public static boolean customHitSoundsRussian = false;
    public static int customHitSoundPreset = 1;

    // ===== РУССКИЙ ЯЗЫК ДЛЯ HUD =====
    public static boolean fpsRussian = false;
    public static boolean pingRussian = false;
    public static boolean tpsRussian = false;
    public static boolean bpsRussian = false;
    public static boolean directionRussian = false;

    // ===== FOV / ASPECT RATIO =====
    public static boolean aspectRatioEnabled = false;
    public static float aspectRatio = 1.0f;
    public static boolean aspectRatioRussian = false;

    // ===== ИСТОРИЯ ПОИСКА =====
    public static String searchHistoryRaw = "";
    private static final int SEARCH_HISTORY_MAX = 8;

    // ===== ПРОЧЕЕ =====
    public static boolean showPet = true;
    public static int hudAlpha = 255;
    public static boolean isBindingKey = false;
    public static int bindingTarget = 0;

    public static double lastPlayerX = 0, lastPlayerY = 0, lastPlayerZ = 0;
    public static double currentBps = 0;

    // ===== ПАГИНАЦИЯ =====
    private static final int MAX_PAGE = 16;
    private int currentPage = 0;

    private EditBox hexField;
    private EditBox searchField;

    private List<String[]> searchResults = new ArrayList<>();
    private int searchResultX = -1;
    private int searchResultY = -1;
    private int searchResultWidth = 0;
    private int searchResultHeight = 0;

    // ===== ГЕОМЕТРИЯ ПАНЕЛИ =====
    private int panelX;
    private int panelY;
    private final int panelWidth = 400;
    private final int panelHeight = 420;

    // ⚠️ ФИКС: флаг — события Screen'а уже зарегистрированы
    private boolean eventsRegistered = false;

    private static final String[][] SEARCH_INDEX = {
            { "hud", "Показывать HUD", "0" },
            { "иконка", "Иконка мода", "1" },
            { "лого", "Иконка мода", "1" },
            { "logo", "Иконка мода", "1" },
            { "фон", "Фон HUD", "0" },
            { "цвет", "Цвет HUD", "0" },
            { "цвет фона", "Цвет фона HUD", "0" },
            { "высота", "Высота фона HUD", "1" },
            { "координаты", "Положение координат", "0" },
            { "биом", "Положение биома", "0" },
            { "время", "Положение времени", "0" },
            { "gui", "Цвет GUI", "0" },
            { "интерфейс", "Цвет GUI", "0" },
            { "текст", "Цвет текста GUI", "0" },
            { "text", "Цвет текста GUI", "0" },
            { "прозрачность", "Прозрачность текста HUD", "1" },
            { "видимость", "Видимость элементов", "1" },
            { "fps", "FPS / КВС", "3" },
            { "ping", "Ping / Пинг", "3" },
            { "tps", "TPS / ТВС", "3" },
            { "bps", "BPS / БВС", "3" },
            { "направление", "Направление", "3" },
            { "удар", "Счётчик ударов", "3" },
            { "hit", "Счётчик ударов", "3" },
            { "клавиша", "Привязка клавиши", "2" },
            { "tape", "TapeMouse", "4" },
            { "наведение", "TapeMouse: бить только при наведении", "4" },
            { "заряд", "TapeMouse: бить при заряженной атаке", "4" },
            { "fov", "FOV (Угол обзора)", "5" },
            { "звук", "Custom Hit Sounds", "6" },
            { "sound", "Custom Hit Sounds", "6" },
            { "эффект", "Эффекты зелий", "7" },
            { "зелье", "Эффекты зелий", "7" },
            { "potion", "Эффекты зелий", "7" },
            { "иконки", "Иконки эффектов", "7" },
            { "icons", "Иконки эффектов", "7" },
            { "экипировка", "Экипировка", "8" },
            { "броня", "Экипировка", "8" },
            { "прочность", "Прочность брони", "8" },
            { "equipment", "Экипировка", "8" },
            { "armor", "Экипировка", "8" },
            { "огонь", "Низкий огонь", "9" },
            { "fire", "Низкий огонь", "9" },
            { "щит", "Низкий щит", "9" },
            { "shield", "Низкий щит", "9" },
            { "зум", "Zoom (Приближение)", "10" },
            { "zoom", "Zoom (Приближение)", "10" },
            { "приближение", "Zoom (Приближение)", "10" },
            { "автосвап", "Автосвап", "11" },
            { "autoswap", "Автосвап", "11" },
            { "свап", "Автосвап", "11" },
            { "fast", "FastExp", "12" },
            { "фаст", "FastExp", "12" },
            { "опыт", "FastExp", "12" },
            { "exp", "FastExp", "12" },
            { "sprint", "AutoSprint", "13" },
            { "бег", "AutoSprint", "13" },
            { "shifttap", "ShiftTap", "13" },
            { "шифт", "ShiftTap", "13" },
            { "комбо", "Счётчик комбо", "14" },
            { "combo", "Счётчик комбо", "14" },
            { "combo counter", "Счётчик комбо", "14" },
            { "эффект предупреждение", "Effect Warnings", "15" },
            { "warnings", "Effect Warnings", "15" },
            { "предупреждение", "Effect Warnings", "15" },
            { "прицел", "Кастомный прицел", "16" },
            { "crosshair", "Кастомный прицел", "16" },
            { "точка", "Кастомный прицел", "16" }
    };
    // ⚠️ ФИКС: конструктор НЕ регистрирует события
    public MyCustomScreen() {
        super(Component.literal("Resistance DLC — Настройки"));
    }

    /**
     * Регистрируется РОВНО ОДИН РАЗ на экземпляр Screen'а.
     * Вызывается из init() (см. ниже), а НЕ из конструктора —
     * Fabric Screen API требует, чтобы Screen уже был инициализирован.
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
            }
            isBindingKey = false;
            bindingTarget = 0;
            this.rebuildWidgets();
            return false;
        });

        ScreenMouseEvents.allowMouseClick(this).register((screen, mouseEvent) -> {
            double mouseX = mouseEvent.x();
            double mouseY = mouseEvent.y();

            if (searchResults != null && !searchResults.isEmpty()
                    && searchResultX >= 0 && searchResultY >= 0) {
                for (int i = 0; i < searchResults.size(); i++) {
                    int y = searchResultY + i * searchResultHeight;
                    if (mouseX >= searchResultX && mouseX <= searchResultX + searchResultWidth
                            && mouseY >= y && mouseY <= y + searchResultHeight) {
                        addSearchHistory(this.searchField.getValue());
                        int page = Integer.parseInt(searchResults.get(i)[2]);
                        currentPage = page;
                        this.searchField.setValue("");
                        this.searchResults = new ArrayList<>();
                        this.rebuildWidgets();
                        return false;
                    }
                }
            }

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

        // ⚠️ ФИКС: регистрация событий ТОЛЬКО здесь, ТОЛЬКО один раз.
        // При rebuildWidgets() init() вызывается снова, но флаг true — пропускаем.
        if (!eventsRegistered) {
            eventsRegistered = true;
            registerScreenEvents();
        }

        int centerX = this.panelX + panelWidth / 2;
        int panelY = this.panelY;

        this.searchField = new EditBox(this.font, panelX - 180, panelY + 50, 160, 18,
                Component.literal("Поиск..."));
        this.searchField.setMaxLength(30);
        this.searchField.setResponder(text -> {});
        this.addRenderableWidget(this.searchField);

        Button prevPageBtn = Button.builder(Component.literal("←"), (btn) -> {
            if (currentPage > 0) { currentPage--; this.rebuildWidgets(); }
        }).bounds(panelX + 10, panelY + 385, 20, 20).build();
        this.addRenderableWidget(prevPageBtn);

        Button nextPageBtn = Button.builder(Component.literal("→"), (btn) -> {
            if (currentPage < MAX_PAGE) { currentPage++; this.rebuildWidgets(); }
        }).bounds(panelX + panelWidth - 30, panelY + 385, 20, 20).build();
        this.addRenderableWidget(nextPageBtn);

        Button closeButton = Button.builder(Component.literal("Закрыть"), (btn) -> this.onClose())
                .bounds(panelX + panelWidth - 90, panelY + 360, 70, 18).build();
        this.addRenderableWidget(closeButton);

        switch (currentPage) {
            case 0 -> initPage0(centerX, panelY);
            case 1 -> initPage1(centerX, panelY);
            case 2 -> initPage2(centerX, panelY);
            case 3 -> initPage3(centerX, panelY);
            case 4 -> initPage4(centerX, panelY);
            case 5 -> initPage5(centerX, panelY);
            case 6 -> initPage6(centerX, panelY);
            case 7 -> initPage7(centerX, panelY);
            case 8 -> initPage8(centerX, panelY);
            case 9 -> initPage9(centerX, panelY);
            case 10 -> initPage10(centerX, panelY);
            case 11 -> initPage11(centerX, panelY);
            case 12 -> initPage12(centerX, panelY);
            case 13 -> initPage13(centerX, panelY);
            case 14 -> initPage14(centerX, panelY);
            case 15 -> initPage15(centerX, panelY);
            case 16 -> initPage16(centerX, panelY);
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

    private void initPage0(int centerX, int panelY) {
        Checkbox hudCheckbox = Checkbox.builder(Component.literal("Показывать HUD"), this.font)
                .pos(centerX - 100, panelY + 45).selected(showHud)
                .onValueChange((c, v) -> { showHud = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudCheckbox);

        Checkbox hudBgCheckbox = Checkbox.builder(Component.literal("Фон HUD"), this.font)
                .pos(centerX + 30, panelY + 45).selected(hudBackgroundEnabled)
                .onValueChange((c, v) -> { hudBackgroundEnabled = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudBgCheckbox);

        // ===== ЦВЕТ HUD =====
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

        // ===== ЦВЕТ ФОНА HUD =====
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

        // ===== ЦВЕТ GUI =====
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

        // ===== ЦВЕТ ТЕКСТА GUI =====
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

        // ===== ПОЛОЖЕНИЕ ЭЛЕМЕНТОВ =====
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

        // ===== КНОПКА СБРОСА =====
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

        // ===== СЛАЙДЕРЫ =====
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

        // ===== HITS (счётчик ударов до смерти) =====
        Checkbox hitCheckbox = Checkbox.builder(
                        Component.literal(hitCounterRussian ? "Удары до смерти" : "Hits to kill"), this.font)
                .pos(centerX - 100, panelY + 380).selected(showHitCounter)
                .onValueChange((c, v) -> { showHitCounter = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hitCheckbox);
        Button hitTranslate = Button.builder(Component.literal("RU"), (b) -> { hitCounterRussian = !hitCounterRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 380, 25, 20).build();
        this.addRenderableWidget(hitTranslate);
    }
    private void initPage4(int centerX, int panelY) {
        Checkbox tapeMouseCheckbox = Checkbox.builder(
                        Component.literal(tapeMouseRussian ? "Включить TapeMouse" : "Enable TapeMouse"), this.font)
                .pos(centerX - 100, panelY + 60)
                .selected(tapeMouseEnabled)
                .onValueChange((c, v) -> { tapeMouseEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(tapeMouseCheckbox);

        Button tapeMouseTranslate = Button.builder(Component.literal("RU"), (b) -> {
            tapeMouseRussian = !tapeMouseRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 60, 25, 20).build();
        this.addRenderableWidget(tapeMouseTranslate);

        Button targetBtn = Button.builder(
                Component.literal(getTargetName(tapeMouseTarget, tapeMouseRussian)),
                (b) -> {
                    tapeMouseTarget = (tapeMouseTarget + 1) % 3;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 90, 200, 20).build();
        this.addRenderableWidget(targetBtn);

        Checkbox requireTargetCheckbox = Checkbox.builder(
                        Component.literal(tapeMouseRussian ? "Бить только при наведении" : "Only when aiming at target"), this.font)
                .pos(centerX - 100, panelY + 120)
                .selected(tapeMouseRequireTarget)
                .onValueChange((c, v) -> { tapeMouseRequireTarget = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(requireTargetCheckbox);

        Checkbox requireFullAttackCheckbox = Checkbox.builder(
                        Component.literal(tapeMouseRussian ? "Бить только при заряженной атаке" : "Only on full attack charge"), this.font)
                .pos(centerX - 100, panelY + 143)
                .selected(tapeMouseRequireFullAttack)
                .onValueChange((c, v) -> { tapeMouseRequireFullAttack = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(requireFullAttackCheckbox);

        String tmKeyName = KeyBindings.tapeMouseKey != null
                ? KeyBindings.tapeMouseKey.getTranslatedKeyMessage().getString() : "R";

        Button tapeMouseKeyBindButton = Button.builder(
                        Component.literal(isBindingKey && bindingTarget == 2 ? "Нажмите клавишу..." : "Клавиша TapeMouse: " + tmKeyName),
                        (b) -> { isBindingKey = true; bindingTarget = 2; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 175, 200, 20).build();
        this.addRenderableWidget(tapeMouseKeyBindButton);

        AbstractSliderButton delaySlider = new AbstractSliderButton(
                centerX - 100, panelY + 215, 200, 20,
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

        for (int i = 1; i <= 7; i++) {
            final int preset = i;
            Button soundBtn = Button.builder(Component.literal(String.valueOf(i)),
                            (b) -> {
                                customHitSoundPreset = preset;
                                ConfigManager.save();
                                this.rebuildWidgets();
                            })
                    .bounds(centerX - 100 + (i - 1) * 30, panelY + 110, 25, 20)
                    .build();
            this.addRenderableWidget(soundBtn);
        }

        AbstractSliderButton volumeSlider = new AbstractSliderButton(
                centerX - 100, panelY + 150, 200, 20,
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
                centerX - 100, panelY + 190, 200, 20,
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
    }

    private void initPage13(int centerX, int panelY) {
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

        Checkbox shiftTapCheckbox = Checkbox.builder(
                        Component.literal(shiftTapRussian ? "Включить ShiftTap" : "Enable ShiftTap"), this.font)
                .pos(centerX - 100, panelY + 125)
                .selected(shiftTapEnabled)
                .onValueChange((c, v) -> { shiftTapEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(shiftTapCheckbox);

        Button shiftTranslate = Button.builder(Component.literal("RU"), (b) -> {
            shiftTapRussian = !shiftTapRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 125, 25, 20).build();
        this.addRenderableWidget(shiftTranslate);
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

        makePosEditor(centerX, panelY, 125,
                () -> comboX, () -> comboY,
                (x, y) -> { comboX = x; comboY = y; },
                10, 185);

        AbstractSliderButton resetSlider = new AbstractSliderButton(
                centerX - 100, panelY + 165, 200, 20,
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
        ).bounds(centerX - 100, panelY + 200, 200, 20).build();
        this.addRenderableWidget(sizeBtn);

        // ===== ЦВЕТ =====
        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 240, 80, 18,
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
        }).bounds(centerX - 45, panelY + 240, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorYellow = Button.builder(Component.literal("Жёлт"), (b) -> { colorField.setValue("#FFFF00"); comboColor = 0xFFFFFF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 240, 45, 18).build();
        this.addRenderableWidget(colorYellow);
        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); comboColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 55, panelY + 240, 45, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorGreen = Button.builder(Component.literal("Зел"), (b) -> { colorField.setValue("#00FF00"); comboColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 105, panelY + 240, 45, 18).build();
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

        AbstractSliderButton thresholdSlider = new AbstractSliderButton(
                centerX - 100, panelY + 110, 200, 20,
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

        makePosEditor(centerX, panelY, 150,
                () -> effectWarningsX, () -> effectWarningsY,
                (x, y) -> { effectWarningsX = x; effectWarningsY = y; },
                300, 200);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 190, 200, 20,
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

        // ===== ЦВЕТ =====
        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 230, 80, 18,
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
        }).bounds(centerX - 45, panelY + 230, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); effectWarningsColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 230, 45, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorYellow = Button.builder(Component.literal("Жёлт"), (b) -> { colorField.setValue("#FFFF00"); effectWarningsColor = 0xFFFFFF00; ConfigManager.save(); })
                .bounds(centerX + 55, panelY + 230, 45, 18).build();
        this.addRenderableWidget(colorYellow);
        Button colorWhite = Button.builder(Component.literal("Бел"), (b) -> { colorField.setValue("#FFFFFF"); effectWarningsColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 105, panelY + 230, 45, 18).build();
        this.addRenderableWidget(colorWhite);

        Checkbox showNameCheckbox = Checkbox.builder(
                        Component.literal(effectWarningsRussian ? "Показывать название" : "Show name"), this.font)
                .pos(centerX - 100, panelY + 265)
                .selected(effectWarningsShowName)
                .onValueChange((c, v) -> { effectWarningsShowName = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(showNameCheckbox);

        Checkbox showIconCheckbox = Checkbox.builder(
                        Component.literal(effectWarningsRussian ? "Показывать иконку" : "Show icon"), this.font)
                .pos(centerX + 20, panelY + 265)
                .selected(effectWarningsShowIcon)
                .onValueChange((c, v) -> { effectWarningsShowIcon = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(showIconCheckbox);
    }

    private void initPage16(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(crosshairRussian ? "Включить кастомный прицел" : "Enable custom crosshair"), this.font)
                .pos(centerX - 100, panelY + 75)
                .selected(crosshairEnabled)
                .onValueChange((c, v) -> { crosshairEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            crosshairRussian = !crosshairRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 75, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        AbstractSliderButton sizeSlider = new AbstractSliderButton(
                centerX - 100, panelY + 110, 200, 20,
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
                centerX - 100, panelY + 140, 200, 20,
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
                centerX - 100, panelY + 170, 200, 20,
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
                centerX - 100, panelY + 200, 200, 20,
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

        // ===== ЦВЕТ =====
        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 240, 80, 18,
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
        }).bounds(centerX - 45, panelY + 240, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorGreen = Button.builder(Component.literal("Зел"), (b) -> { colorField.setValue("#00FF00"); crosshairColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 240, 40, 18).build();
        this.addRenderableWidget(colorGreen);
        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); crosshairColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 240, 40, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorBlue = Button.builder(Component.literal("Син"), (b) -> { colorField.setValue("#0000FF"); crosshairColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 240, 40, 18).build();
        this.addRenderableWidget(colorBlue);
        Button colorWhite = Button.builder(Component.literal("Бел"), (b) -> { colorField.setValue("#FFFFFF"); crosshairColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 240, 40, 18).build();
        this.addRenderableWidget(colorWhite);
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Панель
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, guiColor);
        graphics.fill(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, guiColor);
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelHeight, guiColor);
        graphics.fill(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, guiColor);

        graphics.drawString(this.font, "§lПоиск:",
                panelX - 180, panelY + 35, guiTextColor);

        drawSearchResults(graphics);

        super.render(graphics, mouseX, mouseY, delta);

        // Пет-иконка — сидит на верхнем правом углу поля поиска.
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

        String title;
        switch (currentPage) {
            case 0 -> title = "Resistance DLC — Настройки";
            case 1 -> title = "Resistance DLC — Основные настройки HUD";
            case 2 -> title = "Resistance DLC — Привязка";
            case 3 -> title = "Resistance DLC — Доп. настройки";
            case 4 -> title = "Resistance DLC — TapeMouse";
            case 5 -> title = "Resistance DLC — FOV";
            case 6 -> title = "Resistance DLC — Custom Hit Sounds";
            case 7 -> title = "Resistance DLC — Potion Effects";
            case 8 -> title = "Resistance DLC — Equipment";
            case 9 -> title = "Resistance DLC — Low Fire / Low Shield";
            case 10 -> title = "Resistance DLC — Zoom";
            case 11 -> title = "Resistance DLC — Автосвап";
            case 12 -> title = "Resistance DLC — FastExp";
            case 13 -> title = "Resistance DLC — AutoSprint / ShiftTap";
            case 14 -> title = "Resistance DLC — Combo Counter";
            case 15 -> title = "Resistance DLC — Effect Warnings";
            default -> title = "Resistance DLC — Crosshair";
        }
        graphics.drawCenteredString(this.font, "§l" + title,
                panelX + panelWidth / 2, panelY + 15, guiColor);

        graphics.drawCenteredString(this.font, "§7Страница " + (currentPage + 1) + " / " + (MAX_PAGE + 1),
                panelX + panelWidth / 2, panelY + 405, 0xFFFFFFFF);

        if (currentPage == 0) {
            graphics.drawString(this.font, "§l▸ Цвет HUD",
                    panelX + 20, panelY + 62, guiTextColor);
            graphics.fill(panelX + 20, panelY + 72, panelX + panelWidth - 20, panelY + 73, guiColor);

            graphics.drawString(this.font, "§l▸ Цвет фона HUD",
                    panelX + 20, panelY + 110, guiTextColor);
            graphics.fill(panelX + 20, panelY + 120, panelX + panelWidth - 20, panelY + 121, guiColor);

            graphics.drawString(this.font, "§l▸ Цвет GUI",
                    panelX + 20, panelY + 158, guiTextColor);
            graphics.fill(panelX + 20, panelY + 168, panelX + panelWidth - 20, panelY + 169, guiColor);

            graphics.drawString(this.font, "§l▸ Цвет текста GUI",
                    panelX + 20, panelY + 206, guiTextColor);
            graphics.fill(panelX + 20, panelY + 216, panelX + panelWidth - 20, panelY + 217, guiColor);

            graphics.drawString(this.font, "§l▸ Положение элементов (X, Y)",
                    panelX + 20, panelY + 248, guiTextColor);
            graphics.fill(panelX + 20, panelY + 258, panelX + panelWidth - 20, panelY + 259, guiColor);

        } else if (currentPage == 1) {
            graphics.drawString(this.font, "§l▸ Элементы HUD",
                    panelX + 20, panelY + 40, guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, guiColor);

            graphics.drawString(this.font, "§l▸ Настройки",
                    panelX + 20, panelY + 162, guiTextColor);
            graphics.fill(panelX + 20, panelY + 174, panelX + panelWidth - 20, panelY + 175, guiColor);

        } else if (currentPage == 2) {
            graphics.drawString(this.font, "§l▸ Привязка клавиш",
                    panelX + 20, panelY + 40, guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, guiColor);

            graphics.drawString(this.font, "§7Нажмите «Клавиша: ...», затем любую клавишу",
                    panelX + 20, panelY + 80, 0xFFAAAAAA);

        } else if (currentPage == 3) {
            graphics.drawString(this.font, "§l▸ Дополнительные элементы",
                    panelX + 20, panelY + 25, guiTextColor);
            graphics.fill(panelX + 20, panelY + 37, panelX + panelWidth - 20, panelY + 38, guiColor);

        } else if (currentPage == 4) {
            graphics.drawString(this.font, "§l▸ TapeMouse (Автокликер)",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Автоматически наносит удары",
                    panelX + 20, panelY + 50, 0xFFAAAAAA);

        } else if (currentPage == 5) {
            graphics.drawString(this.font, "§l▸ FOV (Угол обзора)",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Изменяет угол обзора",
                    panelX + 20, panelY + 58, 0xFFAAAAAA);

        } else if (currentPage == 6) {
            graphics.drawString(this.font, "§l▸ Custom Hit Sounds",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawCenteredString(this.font,
                    customHitSoundsRussian ? "§lВыберите звук:" : "§lChoose a sound:",
                    panelX + panelWidth / 2, panelY + 90, guiTextColor);

            for (int i = 1; i <= 7; i++) {
                if (customHitSoundPreset == i) continue;
                int btnX = panelX + panelWidth / 2 - 100 + (i - 1) * 30;
                int btnY = panelY + 110;
                graphics.fill(btnX, btnY, btnX + 25, btnY + 20, 0x90000000);
            }

            graphics.drawCenteredString(this.font,
                    (customHitSoundsRussian ? "Текущий: " : "Current: ") + customHitSoundPreset,
                    panelX + panelWidth / 2, panelY + 135, 0xFFFFFF00);

        } else if (currentPage == 7) {
            graphics.drawString(this.font, "§l▸ Potion Effects HUD",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Показывает активные эффекты и время",
                    panelX + 20, panelY + 58, 0xFFAAAAAA);

        } else if (currentPage == 8) {
            graphics.drawString(this.font, "§l▸ Equipment HUD",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Броня, оружие, стрелы (смещение от хотбара)",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

        } else if (currentPage == 9) {
            graphics.drawString(this.font, "§l▸ Low Fire / Low Shield",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Опускает огонь и щит на экране",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

        } else if (currentPage == 10) {
            graphics.drawString(this.font, "§l▸ Zoom",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Приближение при зажатии клавиши",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

        } else if (currentPage == 11) {
            graphics.drawString(this.font, "§l▸ Автосвап",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Свап offhand ↔ инвентарь (с открытием)",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

            String modeText = switch (autoSwapMode) {
                case 0 -> "Текущий режим: Шар ↔ Шар";
                case 1 -> "Текущий режим: Тотем ↔ Тотем";
                case 2 -> "Текущий режим: Шар ↔ Тотем";
                default -> "Текущий режим: Тотем ↔ Шар";
            };
            graphics.drawCenteredString(this.font, "§e" + modeText,
                    panelX + panelWidth / 2, panelY + 172, 0xFFFFFF00);

        } else if (currentPage == 12) {
            graphics.drawString(this.font, "§l▸ FastExp",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Быстрое использование бутылочек опыта",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

        } else if (currentPage == 13) {
            graphics.drawString(this.font, "§l▸ AutoSprint / ShiftTap",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Авто-бег и авто-отпускание Shift",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

        } else if (currentPage == 14) {
            graphics.drawString(this.font, "§l▸ Combo Counter",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Счётчик ударов подряд",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

        } else if (currentPage == 15) {
            graphics.drawString(this.font, "§l▸ Effect Warnings",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Предупреждение о скором конце эффекта",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);

        } else if (currentPage == 16) {
            graphics.drawString(this.font, "§l▸ Crosshair",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Настройки прицела",
                    panelX + 20, panelY + 60, 0xFFAAAAAA);
        }
    }

    private void drawSearchResults(GuiGraphics graphics) {
        if (this.searchField == null) return;
        String query = this.searchField.getValue().toLowerCase().trim();

        int resultY = panelY + 75;
        int resultWidth = 160;
        int resultHeight = 14;
        int resultX = panelX - 180;

        if (query.isEmpty()) {
            this.searchResults = new ArrayList<>();
            this.searchResultX = -1;
            this.searchResultY = -1;

            List<String> history = getSearchHistory();
            if (history.isEmpty() || !this.searchField.isFocused()) return;

            int maxShow = Math.min(history.size(), 5);

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
            if (entry[0].contains(query)) {
                boolean duplicate = false;
                for (String[] m : matches) {
                    if (m[1].equals(entry[1])) { duplicate = true; break; }
                }
                if (!duplicate) matches.add(entry);
                if (matches.size() >= 5) break;
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
            String text = match[1] + " §7(стр. " + (Integer.parseInt(match[2]) + 1) + ")";
            graphics.drawString(this.font, text, resultX + 5, y + 3, 0xFFFFFFFF, false);
        }

        this.searchResults = matches;
        this.searchResultX = resultX;
        this.searchResultY = resultY;
        this.searchResultWidth = resultWidth;
        this.searchResultHeight = resultHeight;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}