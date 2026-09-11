package com.resistancedlc;

import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
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
    public static boolean hudBackgroundEnabled = false;
    public static int hudBackgroundAlpha = 128;
    public static int hudBackgroundColor = 0xFF000000;
    public static int hudBackgroundHeight = 10;
    public static int guiColor = 0xFF00FF00;
    public static int guiTextColor = 0xFFFFFFFF;

    public static int coordsX = 10, coordsY = 10;
    public static int biomeX = 10, biomeY = 25;
    public static int timeX = 10, timeY = 40;

    public static boolean showCoords = true;
    public static boolean showBiome = true;
    public static boolean showTime = true;

    public static boolean showFps = false;
    public static boolean showPing = false;
    public static boolean showTps = false;
    public static boolean showBps = false;
    public static boolean showDirection = false;

    public static int fpsX = 10, fpsY = 55;
    public static int pingX = 10, pingY = 70;
    public static int tpsX = 10, tpsY = 85;
    public static int bpsX = 10, bpsY = 100;
    public static int directionX = 10, directionY = 115;

    public static boolean showHitCounter = false;
    public static int hitCounterX = 10, hitCounterY = 130;
    public static boolean hitCounterRussian = false;

    // ===== POTION EFFECTS HUD =====
    public static boolean showPotionEffects = false;
    public static int potionEffectsX = 10, potionEffectsY = 145;
    public static boolean potionEffectsRussian = false;

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

    // ===== TAPEMOUSE =====
    public static boolean tapeMouseEnabled = false;
    public static int tapeMouseTarget = 0;
    public static float tapeMouseDelay = 1.0f;
    public static boolean tapeMouseRussian = false;

    // ===== FOV / ASPECT RATIO =====
    public static boolean aspectRatioEnabled = false;
    public static float aspectRatio = 1.0f;
    public static boolean aspectRatioRussian = false;

    // ===== ПРОЧЕЕ =====
    public static boolean showPet = true;
    public static int hudAlpha = 255;
    public static boolean isBindingKey = false;

    public static double lastPlayerX = 0, lastPlayerY = 0, lastPlayerZ = 0;
    public static double currentBps = 0;

    private int currentPage = 0;
    private EditBox hexField;

    private EditBox searchField;
    private List<String[]> searchResults = new ArrayList<>();
    private int searchResultX, searchResultY, searchResultWidth, searchResultHeight;

    private static final String[][] SEARCH_INDEX = {
            { "hud", "Показывать HUD", "0" },
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
            { "fov", "FOV (Угол обзора)", "5" },
            { "звук", "Custom Hit Sounds", "6" },
            { "sound", "Custom Hit Sounds", "6" },
            { "эффект", "Эффекты зелий", "7" },
            { "зелье", "Эффекты зелий", "7" },
            { "potion", "Эффекты зелий", "7" }
    };

    public MyCustomScreen() {
        super(Component.literal("Resistance DLC — Настройки"));
    }
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int panelWidth = 400;
        int panelHeight = 420;
        int panelX = (this.width - panelWidth) / 2 + 80;
        int panelY = (this.height - panelHeight) / 2;

        ScreenKeyboardEvents.allowKeyPress(this).register((screen, keyEvent) -> {
            if (this.searchField != null && this.searchField.isFocused()) return true;
            if (this.hexField != null && this.hexField.isFocused()) return true;

            if (!isBindingKey) return true;
            int keyCode = keyEvent.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isBindingKey = false;
                this.rebuildWidgets();
                return false;
            }
            KeyBindings.setKey(keyCode);
            isBindingKey = false;
            this.rebuildWidgets();
            return false;
        });

        ScreenMouseEvents.allowMouseClick(this).register((screen, mouseEvent) -> {
            double mouseX = mouseEvent.x();
            double mouseY = mouseEvent.y();

            if (searchResults != null && !searchResults.isEmpty()) {
                for (int i = 0; i < searchResults.size(); i++) {
                    int y = searchResultY + i * searchResultHeight;
                    if (mouseX >= searchResultX && mouseX <= searchResultX + searchResultWidth
                            && mouseY >= y && mouseY <= y + searchResultHeight) {
                        int page = Integer.parseInt(searchResults.get(i)[2]);
                        currentPage = page;
                        this.searchField.setValue("");
                        this.searchResults = new ArrayList<>();
                        this.rebuildWidgets();
                        return false;
                    }
                }
            }
            return true;
        });

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
            if (currentPage < 7) { currentPage++; this.rebuildWidgets(); }
        }).bounds(panelX + panelWidth - 30, panelY + 385, 20, 20).build();
        this.addRenderableWidget(nextPageBtn);

        Button closeButton = Button.builder(Component.literal("Закрыть"), (btn) -> this.onClose())
                .bounds(panelX + panelWidth / 2 - 60, panelY + 360, 120, 20).build();
        this.addRenderableWidget(closeButton);

        if (currentPage == 0) { initPage0(panelX + panelWidth / 2, panelY); }
        else if (currentPage == 1) { initPage1(panelX + panelWidth / 2, panelY); }
        else if (currentPage == 2) { initPage2(panelX + panelWidth / 2, panelY); }
        else if (currentPage == 3) { initPage3(panelX + panelWidth / 2, panelY); }
        else if (currentPage == 4) { initPage4(panelX + panelWidth / 2, panelY); }
        else if (currentPage == 5) { initPage5(panelX + panelWidth / 2, panelY); }
        else if (currentPage == 6) { initPage6(panelX + panelWidth / 2, panelY); }
        else if (currentPage == 7) { initPage7(panelX + panelWidth / 2, panelY); }
    }

    private void initPage0(int centerX, int panelY) {
        // ===== ЧЕКБОКСЫ HUD (в одну строку) =====
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
        EditBox hudBgColorField = new EditBox(this.font, centerX - 130, panelY + 118, 80, 18,
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
        }).bounds(centerX - 45, panelY + 118, 40, 18).build();
        this.addRenderableWidget(applyHudBgColorBtn);

        Button hudBgGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { hudBgColorField.setValue("#00FF00"); hudBackgroundColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 118, 40, 18).build();
        this.addRenderableWidget(hudBgGreenBtn);
        Button hudBgRedBtn = Button.builder(Component.literal("Крас"), (b) -> { hudBgColorField.setValue("#FF0000"); hudBackgroundColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 118, 40, 18).build();
        this.addRenderableWidget(hudBgRedBtn);
        Button hudBgBlueBtn = Button.builder(Component.literal("Син"), (b) -> { hudBgColorField.setValue("#0000FF"); hudBackgroundColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 118, 40, 18).build();
        this.addRenderableWidget(hudBgBlueBtn);
        Button hudBgBlackBtn = Button.builder(Component.literal("Чёрн"), (b) -> { hudBgColorField.setValue("#000000"); hudBackgroundColor = 0xFF000000; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 118, 40, 18).build();
        this.addRenderableWidget(hudBgBlackBtn);

        // ===== ЦВЕТ GUI =====
        EditBox guiColorField = new EditBox(this.font, centerX - 130, panelY + 158, 80, 18,
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
        }).bounds(centerX - 45, panelY + 158, 40, 18).build();
        this.addRenderableWidget(applyGuiColorBtn);

        Button guiGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiColorField.setValue("#00FF00"); guiColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 158, 40, 18).build();
        this.addRenderableWidget(guiGreenBtn);
        Button guiRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiColorField.setValue("#FF0000"); guiColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 158, 40, 18).build();
        this.addRenderableWidget(guiRedBtn);
        Button guiBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiColorField.setValue("#0000FF"); guiColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 158, 40, 18).build();
        this.addRenderableWidget(guiBlueBtn);

        // ===== ЦВЕТ ТЕКСТА GUI =====
        EditBox guiTextColorField = new EditBox(this.font, centerX - 130, panelY + 198, 80, 18,
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
        }).bounds(centerX - 45, panelY + 198, 40, 18).build();
        this.addRenderableWidget(applyGuiTextColorBtn);

        Button textGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiTextColorField.setValue("#00FF00"); guiTextColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 198, 40, 18).build();
        this.addRenderableWidget(textGreenBtn);
        Button textRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiTextColorField.setValue("#FF0000"); guiTextColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 198, 40, 18).build();
        this.addRenderableWidget(textRedBtn);
        Button textBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiTextColorField.setValue("#0000FF"); guiTextColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 198, 40, 18).build();
        this.addRenderableWidget(textBlueBtn);

        // ===== ПОЛОЖЕНИЕ ЭЛЕМЕНТОВ =====
        int upY = panelY + 250, midY = panelY + 273, downY = panelY + 296, size = 18;

        Button coordsUp = Button.builder(Component.literal("↑"), (b) -> { coordsY -= 5; ConfigManager.save(); }).bounds(centerX - 107, upY, size, size).build();
        this.addRenderableWidget(coordsUp);
        Button coordsLeft = Button.builder(Component.literal("←"), (b) -> { coordsX -= 5; ConfigManager.save(); }).bounds(centerX - 130, midY, size, size).build();
        this.addRenderableWidget(coordsLeft);
        Button coordsRight = Button.builder(Component.literal("→"), (b) -> { coordsX += 5; ConfigManager.save(); }).bounds(centerX - 84, midY, size, size).build();
        this.addRenderableWidget(coordsRight);
        Button coordsDown = Button.builder(Component.literal("↓"), (b) -> { coordsY += 5; ConfigManager.save(); }).bounds(centerX - 107, downY, size, size).build();
        this.addRenderableWidget(coordsDown);

        Button biomeUp = Button.builder(Component.literal("↑"), (b) -> { biomeY -= 5; ConfigManager.save(); }).bounds(centerX - 17, upY, size, size).build();
        this.addRenderableWidget(biomeUp);
        Button biomeLeft = Button.builder(Component.literal("←"), (b) -> { biomeX -= 5; ConfigManager.save(); }).bounds(centerX - 40, midY, size, size).build();
        this.addRenderableWidget(biomeLeft);
        Button biomeRight = Button.builder(Component.literal("→"), (b) -> { biomeX += 5; ConfigManager.save(); }).bounds(centerX + 6, midY, size, size).build();
        this.addRenderableWidget(biomeRight);
        Button biomeDown = Button.builder(Component.literal("↓"), (b) -> { biomeY += 5; ConfigManager.save(); }).bounds(centerX - 17, downY, size, size).build();
        this.addRenderableWidget(biomeDown);

        Button timeUp = Button.builder(Component.literal("↑"), (b) -> { timeY -= 5; ConfigManager.save(); }).bounds(centerX + 73, upY, size, size).build();
        this.addRenderableWidget(timeUp);
        Button timeLeft = Button.builder(Component.literal("←"), (b) -> { timeX -= 5; ConfigManager.save(); }).bounds(centerX + 50, midY, size, size).build();
        this.addRenderableWidget(timeLeft);
        Button timeRight = Button.builder(Component.literal("→"), (b) -> { timeX += 5; ConfigManager.save(); }).bounds(centerX + 96, midY, size, size).build();
        this.addRenderableWidget(timeRight);
        Button timeDown = Button.builder(Component.literal("↓"), (b) -> { timeY += 5; ConfigManager.save(); }).bounds(centerX + 73, downY, size, size).build();
        this.addRenderableWidget(timeDown);

        Button resetBtn = Button.builder(Component.literal("Сбросить позиции"), (b) -> {
            coordsX = 10; coordsY = 10; biomeX = 10; biomeY = 25; timeX = 10; timeY = 40;
            fpsX = 10; fpsY = 55; pingX = 10; pingY = 70; tpsX = 10; tpsY = 85;
            bpsX = 10; bpsY = 100; directionX = 10; directionY = 115;
            hitCounterX = 10; hitCounterY = 130;
            potionEffectsX = 10; potionEffectsY = 145;
            ConfigManager.save();
        }).bounds(centerX - 100, panelY + 330, 200, 20).build();
        this.addRenderableWidget(resetBtn);
    }
    private void initPage1(int centerX, int panelY) {
        Checkbox coordsCheckbox = Checkbox.builder(Component.literal("Показывать координаты"), this.font)
                .pos(centerX - 100, panelY + 70).selected(showCoords)
                .onValueChange((c, v) -> { showCoords = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(coordsCheckbox);

        Checkbox biomeCheckbox = Checkbox.builder(Component.literal("Показывать биом"), this.font)
                .pos(centerX - 100, panelY + 100).selected(showBiome)
                .onValueChange((c, v) -> { showBiome = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(biomeCheckbox);

        Checkbox timeCheckbox = Checkbox.builder(Component.literal("Показывать время"), this.font)
                .pos(centerX - 100, panelY + 130).selected(showTime)
                .onValueChange((c, v) -> { showTime = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(timeCheckbox);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 170, 200, 20,
                Component.literal("Прозрачность текста HUD: " + hudAlpha), hudAlpha / 255.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Прозрачность текста HUD: " + hudAlpha)); }
            @Override protected void applyValue() { hudAlpha = (int)(this.value * 255); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(alphaSlider);

        AbstractSliderButton bgAlphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 200, 200, 20,
                Component.literal("Прозрачность фона HUD: " + hudBackgroundAlpha), hudBackgroundAlpha / 255.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Прозрачность фона HUD: " + hudBackgroundAlpha)); }
            @Override protected void applyValue() { hudBackgroundAlpha = (int)(this.value * 255); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(bgAlphaSlider);

        AbstractSliderButton bgHeightSlider = new AbstractSliderButton(
                centerX - 100, panelY + 230, 200, 20,
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
                        Component.literal(isBindingKey ? "Нажмите клавишу..." : "Клавиша: " + keyName),
                        (b) -> { isBindingKey = true; b.setMessage(Component.literal("Нажмите клавишу...")); })
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
        int size = 20;
        int leftCol = centerX - 150;
        int rightCol = centerX + 50;

        Checkbox fpsCheckbox = Checkbox.builder(Component.literal(fpsRussian ? "КВС" : "FPS"), this.font)
                .pos(leftCol, panelY + 60).selected(showFps)
                .onValueChange((c, v) -> { showFps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(fpsCheckbox);
        Button fpsTranslate = Button.builder(Component.literal("RU"), (b) -> { fpsRussian = !fpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(leftCol + 85, panelY + 60, 25, 20).build();
        this.addRenderableWidget(fpsTranslate);
        Button fpsUp = Button.builder(Component.literal("↑"), (b) -> { fpsY -= 5; ConfigManager.save(); }).bounds(leftCol + 40, panelY + 85, size, size).build();
        this.addRenderableWidget(fpsUp);
        Button fpsLeft = Button.builder(Component.literal("←"), (b) -> { fpsX -= 5; ConfigManager.save(); }).bounds(leftCol, panelY + 110, size, size).build();
        this.addRenderableWidget(fpsLeft);
        Button fpsRight = Button.builder(Component.literal("→"), (b) -> { fpsX += 5; ConfigManager.save(); }).bounds(leftCol + 80, panelY + 110, size, size).build();
        this.addRenderableWidget(fpsRight);
        Button fpsDown = Button.builder(Component.literal("↓"), (b) -> { fpsY += 5; ConfigManager.save(); }).bounds(leftCol + 40, panelY + 135, size, size).build();
        this.addRenderableWidget(fpsDown);

        Checkbox tpsCheckbox = Checkbox.builder(Component.literal(tpsRussian ? "ТВС" : "TPS"), this.font)
                .pos(leftCol, panelY + 170).selected(showTps)
                .onValueChange((c, v) -> { showTps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(tpsCheckbox);
        Button tpsTranslate = Button.builder(Component.literal("RU"), (b) -> { tpsRussian = !tpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(leftCol + 85, panelY + 170, 25, 20).build();
        this.addRenderableWidget(tpsTranslate);
        Button tpsUp = Button.builder(Component.literal("↑"), (b) -> { tpsY -= 5; ConfigManager.save(); }).bounds(leftCol + 40, panelY + 195, size, size).build();
        this.addRenderableWidget(tpsUp);
        Button tpsLeft = Button.builder(Component.literal("←"), (b) -> { tpsX -= 5; ConfigManager.save(); }).bounds(leftCol, panelY + 220, size, size).build();
        this.addRenderableWidget(tpsLeft);
        Button tpsRight = Button.builder(Component.literal("→"), (b) -> { tpsX += 5; ConfigManager.save(); }).bounds(leftCol + 80, panelY + 220, size, size).build();
        this.addRenderableWidget(tpsRight);
        Button tpsDown = Button.builder(Component.literal("↓"), (b) -> { tpsY += 5; ConfigManager.save(); }).bounds(leftCol + 40, panelY + 245, size, size).build();
        this.addRenderableWidget(tpsDown);

        Checkbox dirCheckbox = Checkbox.builder(Component.literal(directionRussian ? "Направление" : "Direction"), this.font)
                .pos(leftCol, panelY + 280).selected(showDirection)
                .onValueChange((c, v) -> { showDirection = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(dirCheckbox);
        Button dirTranslate = Button.builder(Component.literal("RU"), (b) -> { directionRussian = !directionRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(leftCol + 125, panelY + 280, 25, 20).build();
        this.addRenderableWidget(dirTranslate);

        Checkbox pingCheckbox = Checkbox.builder(Component.literal(pingRussian ? "Пинг" : "Ping"), this.font)
                .pos(rightCol, panelY + 60).selected(showPing)
                .onValueChange((c, v) -> { showPing = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(pingCheckbox);
        Button pingTranslate = Button.builder(Component.literal("RU"), (b) -> { pingRussian = !pingRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(rightCol + 85, panelY + 60, 25, 20).build();
        this.addRenderableWidget(pingTranslate);
        Button pingUp = Button.builder(Component.literal("↑"), (b) -> { pingY -= 5; ConfigManager.save(); }).bounds(rightCol + 40, panelY + 85, size, size).build();
        this.addRenderableWidget(pingUp);
        Button pingLeft = Button.builder(Component.literal("←"), (b) -> { pingX -= 5; ConfigManager.save(); }).bounds(rightCol, panelY + 110, size, size).build();
        this.addRenderableWidget(pingLeft);
        Button pingRight = Button.builder(Component.literal("→"), (b) -> { pingX += 5; ConfigManager.save(); }).bounds(rightCol + 80, panelY + 110, size, size).build();
        this.addRenderableWidget(pingRight);
        Button pingDown = Button.builder(Component.literal("↓"), (b) -> { pingY += 5; ConfigManager.save(); }).bounds(rightCol + 40, panelY + 135, size, size).build();
        this.addRenderableWidget(pingDown);

        Checkbox bpsCheckbox = Checkbox.builder(Component.literal(bpsRussian ? "БВС" : "BPS"), this.font)
                .pos(rightCol, panelY + 170).selected(showBps)
                .onValueChange((c, v) -> { showBps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(bpsCheckbox);
        Button bpsTranslate = Button.builder(Component.literal("RU"), (b) -> { bpsRussian = !bpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(rightCol + 85, panelY + 170, 25, 20).build();
        this.addRenderableWidget(bpsTranslate);
        Button bpsUp = Button.builder(Component.literal("↑"), (b) -> { bpsY -= 5; ConfigManager.save(); }).bounds(rightCol + 40, panelY + 195, size, size).build();
        this.addRenderableWidget(bpsUp);
        Button bpsLeft = Button.builder(Component.literal("←"), (b) -> { bpsX -= 5; ConfigManager.save(); }).bounds(rightCol, panelY + 220, size, size).build();
        this.addRenderableWidget(bpsLeft);
        Button bpsRight = Button.builder(Component.literal("→"), (b) -> { bpsX += 5; ConfigManager.save(); }).bounds(rightCol + 80, panelY + 220, size, size).build();
        this.addRenderableWidget(bpsRight);
        Button bpsDown = Button.builder(Component.literal("↓"), (b) -> { bpsY += 5; ConfigManager.save(); }).bounds(rightCol + 40, panelY + 245, size, size).build();
        this.addRenderableWidget(bpsDown);

        Checkbox hitCounterCheckbox = Checkbox.builder(Component.literal(hitCounterRussian ? "Удары" : "Hits"), this.font)
                .pos(rightCol, panelY + 280).selected(showHitCounter)
                .onValueChange((c, v) -> { showHitCounter = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hitCounterCheckbox);
        Button hitCounterTranslate = Button.builder(Component.literal("RU"), (b) -> { hitCounterRussian = !hitCounterRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(rightCol + 85, panelY + 280, 25, 20).build();
        this.addRenderableWidget(hitCounterTranslate);
    }

    private void initPage4(int centerX, int panelY) {
        Checkbox tapeMouseCheckbox = Checkbox.builder(
                        Component.literal(tapeMouseRussian ? "Включить TapeMouse" : "Enable TapeMouse"), this.font)
                .pos(centerX - 100, panelY + 70)
                .selected(tapeMouseEnabled)
                .onValueChange((c, v) -> { tapeMouseEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(tapeMouseCheckbox);

        Button tapeMouseTranslate = Button.builder(Component.literal("RU"), (b) -> {
            tapeMouseRussian = !tapeMouseRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 70, 25, 20).build();
        this.addRenderableWidget(tapeMouseTranslate);

        Button targetBtn = Button.builder(
                Component.literal(getTargetName(tapeMouseTarget, tapeMouseRussian)),
                (b) -> {
                    tapeMouseTarget = (tapeMouseTarget + 1) % 3;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 110, 200, 20).build();
        this.addRenderableWidget(targetBtn);

        AbstractSliderButton delaySlider = new AbstractSliderButton(
                centerX - 100, panelY + 150, 200, 20,
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

    // СТРАНИЦА 8 — POTION EFFECTS HUD
    private void initPage7(int centerX, int panelY) {
        Checkbox potionCheckbox = Checkbox.builder(
                        Component.literal(potionEffectsRussian ? "Показывать эффекты" : "Show Effects"), this.font)
                .pos(centerX - 100, panelY + 75)
                .selected(showPotionEffects)
                .onValueChange((c, v) -> { showPotionEffects = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(potionCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            potionEffectsRussian = !potionEffectsRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 75, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        int size = 20;
        int upY   = panelY + 125;
        int midY  = panelY + 150;
        int downY = panelY + 175;

        Button upBtn    = Button.builder(Component.literal("↑"), (b) -> { potionEffectsY -= 5; ConfigManager.save(); }).bounds(centerX - 10, upY,   size, size).build();
        Button leftBtn  = Button.builder(Component.literal("←"), (b) -> { potionEffectsX -= 5; ConfigManager.save(); }).bounds(centerX - 35, midY, size, size).build();
        Button rightBtn = Button.builder(Component.literal("→"), (b) -> { potionEffectsX += 5; ConfigManager.save(); }).bounds(centerX + 15, midY, size, size).build();
        Button downBtn  = Button.builder(Component.literal("↓"), (b) -> { potionEffectsY += 5; ConfigManager.save(); }).bounds(centerX - 10, downY, size, size).build();

        this.addRenderableWidget(upBtn);
        this.addRenderableWidget(leftBtn);
        this.addRenderableWidget(rightBtn);
        this.addRenderableWidget(downBtn);
    }

    private String getTargetName(int target, boolean russian) {
        if (russian) {
            switch (target) {
                case 0: return "Цель: Все";
                case 1: return "Цель: Только мобы";
                case 2: return "Цель: Только игроки";
            }
        } else {
            switch (target) {
                case 0: return "Target: All";
                case 1: return "Target: Mobs only";
                case 2: return "Target: Players only";
            }
        }
        return "Target: All";
    }

    private String getDelayText() {
        return String.format("Задержка: %.1f сек", tapeMouseDelay);
    }

    private String getAspectRatioText() {
        return String.format("Соотношение: %.2f", aspectRatio);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int panelWidth = 400;
        int panelHeight = 420;
        int panelX = (this.width - panelWidth) / 2 + 80;
        int panelY = (this.height - panelHeight) / 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, guiColor);
        graphics.fill(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, guiColor);
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelHeight, guiColor);
        graphics.fill(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, guiColor);

        graphics.drawString(this.font, "§lПоиск:",
                panelX - 180, panelY + 35, guiTextColor);

        drawSearchResults(graphics, panelX, panelY, panelWidth);

        super.render(graphics, mouseX, mouseY, delta);

        // === ПИТОМЕЦ ===
        if (showPet) {
            int petSize = 48;
            int petX = panelX - 55;
            int petY = panelY + 8;
            Identifier petId = Identifier.fromNamespaceAndPath(
                    "resistancedlc", "textures/gui/pet/pet_idle.png"
            );

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
        if (currentPage == 0) {
            title = "Resistance DLC — Настройки";
        } else if (currentPage == 1) {
            title = "Resistance DLC — Основные настройки HUD";
        } else if (currentPage == 2) {
            title = "Resistance DLC — Привязка";
        } else if (currentPage == 3) {
            title = "Resistance DLC — Доп. настройки";
        } else if (currentPage == 4) {
            title = "Resistance DLC — TapeMouse";
        } else if (currentPage == 5) {
            title = "Resistance DLC — FOV";
        } else if (currentPage == 6) {
            title = "Resistance DLC — Custom Hit Sounds";
        } else {
            title = "Resistance DLC — Potion Effects";
        }
        graphics.drawCenteredString(this.font, "§l" + title,
                panelX + panelWidth / 2, panelY + 15, guiColor);

        graphics.drawCenteredString(this.font, "§7Страница " + (currentPage + 1) + " / 8",
                panelX + panelWidth / 2, panelY + 405, 0xFFFFFFFF);

        if (currentPage == 0) {
            graphics.drawString(this.font, "§l▸ Цвет HUD",
                    panelX + 20, panelY + 62, guiTextColor);
            graphics.fill(panelX + 20, panelY + 72, panelX + panelWidth - 20, panelY + 73, guiColor);

            graphics.drawString(this.font, "§l▸ Цвет фона HUD",
                    panelX + 20, panelY + 102, guiTextColor);
            graphics.fill(panelX + 20, panelY + 112, panelX + panelWidth - 20, panelY + 113, guiColor);

            graphics.drawString(this.font, "§l▸ Цвет GUI",
                    panelX + 20, panelY + 142, guiTextColor);
            graphics.fill(panelX + 20, panelY + 152, panelX + panelWidth - 20, panelY + 153, guiColor);

            graphics.drawString(this.font, "§l▸ Цвет текста GUI",
                    panelX + 20, panelY + 182, guiTextColor);
            graphics.fill(panelX + 20, panelY + 192, panelX + panelWidth - 20, panelY + 193, guiColor);

            graphics.drawString(this.font, "§l▸ Положение элементов",
                    panelX + 20, panelY + 232, guiTextColor);
            graphics.fill(panelX + 20, panelY + 242, panelX + panelWidth - 20, panelY + 243, guiColor);

            int labelY = panelY + 247;
            graphics.drawCenteredString(this.font, "§lКоординаты", panelX + panelWidth / 2 - 107, labelY, guiTextColor);
            graphics.drawCenteredString(this.font, "§lБиом", panelX + panelWidth / 2 - 17, labelY, guiTextColor);
            graphics.drawCenteredString(this.font, "§lВремя", panelX + panelWidth / 2 + 72, labelY, guiTextColor);

        } else if (currentPage == 1) {
            graphics.drawString(this.font, "§l▸ Элементы HUD",
                    panelX + 20, panelY + 40, guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, guiColor);

            graphics.drawString(this.font, "§l▸ Настройки",
                    panelX + 20, panelY + 155, guiTextColor);
            graphics.fill(panelX + 20, panelY + 167, panelX + panelWidth - 20, panelY + 168, guiColor);

        } else if (currentPage == 2) {
            graphics.drawString(this.font, "§l▸ Привязка клавиш",
                    panelX + 20, panelY + 40, guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, guiColor);

            graphics.drawString(this.font, "§7Нажмите «Клавиша: ...», затем любую клавишу",
                    panelX + 20, panelY + 80, 0xFFAAAAAA);

        } else if (currentPage == 3) {
            graphics.drawString(this.font, "§l▸ Дополнительные элементы",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

        } else if (currentPage == 4) {
            graphics.drawString(this.font, "§l▸ TapeMouse (Автокликер)",
                    panelX + 20, panelY + 35, guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, guiColor);

            graphics.drawString(this.font, "§7Автоматически наносит удары",
                    panelX + 20, panelY + 58, 0xFFAAAAAA);

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
        }
    }

    private void drawSearchResults(GuiGraphics graphics, int panelX, int panelY, int panelWidth) {
        if (this.searchField == null) return;
        String query = this.searchField.getValue().toLowerCase().trim();
        if (query.isEmpty()) {
            this.searchResults = new ArrayList<>();
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
            return;
        }

        int resultY = panelY + 75;
        int resultWidth = 160;
        int resultHeight = 14;
        int resultX = panelX - 180;

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