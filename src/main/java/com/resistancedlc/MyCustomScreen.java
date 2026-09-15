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
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MyCustomScreen extends Screen {

    // ===== ПАГИНАЦИЯ + РАЗДЕЛЫ =====
    private static final String[] SECTION_NAMES_RU = {
            "HUD", "PVP", "PVE", "Visual", "Misc"
    };
    private static final String[] SECTION_NAMES_EN = {
            "HUD", "PVP", "PVE", "Visual", "Misc"
    };

    private static final int[] SECTION_PAGES = {
            7,  // 0 = HUD
            8,  // 1 = PVP
            1,  // 2 = PVE
            9,  // 3 = Visual (было 8 → 9: Particle Blocker)
            4   // 4 = Misc
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

    private boolean renameDialogOpen = false;
    private int renameDialogIndex = -1;
    private String renameDialogOldName = "";

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
            // ===================== HUD =====================
            // Стр 0: Элементы
            { "hud", "Показывать HUD", "0", "0" },
            { "худ", "Показывать HUD", "0", "0" },
            { "координаты", "Координаты", "0", "0" },
            { "coords", "Координаты", "0", "0" },
            { "xyz", "Координаты", "0", "0" },
            { "биом", "Биом", "0", "0" },
            { "biome", "Биом", "0", "0" },
            { "время", "Время", "0", "0" },
            { "time", "Время", "0", "0" },
            { "иконка", "Иконка мода", "0", "0" },
            { "лого", "Иконка мода", "0", "0" },
            { "logo", "Иконка мода", "0", "0" },
            { "modlogo", "Иконка мода", "0", "0" },
            { "прозрачность текста", "Прозрачность текста", "0", "0" },
            { "прозрачность фона", "Прозрачность фона", "0", "0" },
            { "высота фона", "Высота фона", "0", "0" },
            { "hudalpha", "Прозрачность текста", "0", "0" },

            // Стр 1: Эффекты зелий
            { "эффект", "Эффекты зелий", "1", "0" },
            { "зелье", "Эффекты зелий", "1", "0" },
            { "potion", "Эффекты зелий", "1", "0" },
            { "effect", "Эффекты зелий", "1", "0" },
            { "иконки эффектов", "Иконки эффектов", "1", "0" },
            { "potionicons", "Иконки эффектов", "1", "0" },

            // Стр 2: Экипировка
            { "экипировка", "Экипировка", "2", "0" },
            { "equipment", "Экипировка", "2", "0" },
            { "броня", "Экипировка", "2", "0" },
            { "armor", "Экипировка", "2", "0" },
            { "прочность", "Прочность брони", "2", "0" },
            { "durability", "Прочность брони", "2", "0" },

            // Стр 3: Effect Warnings
            { "warnings", "Effect Warnings", "3", "0" },
            { "предупреждение", "Effect Warnings", "3", "0" },
            { "эффекты заканчиваются", "Effect Warnings", "3", "0" },
            { "порог эффектов", "Порог Effect Warnings", "3", "0" },
            { "threshold", "Порог Effect Warnings", "3", "0" },
            { "прозрачность warnings", "Прозрачность warnings", "3", "0" },
            { "цвет warnings", "Цвет warnings", "3", "0" },
            { "show name", "Показывать название", "3", "0" },
            { "show icon", "Показывать иконку", "3", "0" },

            // Стр 4: Combo Counter
            { "комбо", "Combo Counter", "4", "0" },
            { "combo", "Combo Counter", "4", "0" },
            { "счётчик комбо", "Combo Counter", "4", "0" },
            { "время сброса комбо", "Время сброса комбо", "4", "0" },
            { "размер комбо", "Размер комбо", "4", "0" },
            { "цвет комбо", "Цвет комбо", "4", "0" },

            // Стр 5: Доп. элементы
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
            { "hits to kill", "Счётчик ударов", "5", "0" },
            { "удары до смерти", "Счётчик ударов", "5", "0" },

            // Стр 6: Оформление HUD
            { "цвет hud", "Цвет HUD", "6", "0" },
            { "hudcolor", "Цвет HUD", "6", "0" },
            { "фон hud", "Фон HUD", "6", "0" },
            { "hud background", "Фон HUD", "6", "0" },
            { "цвет фона", "Цвет фона HUD", "6", "0" },
            { "цвет gui", "Цвет GUI", "6", "0" },
            { "guicolor", "Цвет GUI", "6", "0" },
            { "цвет текста", "Цвет текста GUI", "6", "0" },
            { "текст gui", "Цвет текста GUI", "6", "0" },
            { "сбросить цвета", "Сбросить цвета", "6", "0" },

            // ===================== PVP =====================
            // Стр 0: Custom Hit Sounds
            { "звук", "Custom Hit Sounds", "0", "1" },
            { "sound", "Custom Hit Sounds", "0", "1" },
            { "хит саунд", "Custom Hit Sounds", "0", "1" },
            { "громкость удара", "Громкость удара", "0", "1" },
            { "тон удара", "Тон удара", "0", "1" },
            { "volume", "Громкость удара", "0", "1" },
            { "pitch", "Тон удара", "0", "1" },

            // Стр 1: AutoSwap
            { "автосвап", "Автосвап", "1", "1" },
            { "autoswap", "Автосвап", "1", "1" },
            { "свап", "Автосвап", "1", "1" },
            { "swap", "Автосвап", "1", "1" },
            { "задержка свапа", "Задержка свапа", "1", "1" },
            { "cooldown", "Cooldown свапа", "1", "1" },

            // Стр 2: FastExp
            { "fastexp", "FastExp", "2", "1" },
            { "фаст эксп", "FastExp", "2", "1" },
            { "опыт", "FastExp", "2", "1" },
            { "бутылочки опыта", "FastExp", "2", "1" },

            // Стр 3: ShiftTap
            { "shifttap", "ShiftTap", "3", "1" },
            { "шифт тап", "ShiftTap", "3", "1" },
            { "крит через шифт", "ShiftTap", "3", "1" },
            { "shift", "ShiftTap", "3", "1" },

            // Стр 4: AutoSprint
            { "sprint", "AutoSprint", "4", "1" },
            { "бег", "AutoSprint", "4", "1" },
            { "авто бег", "AutoSprint", "4", "1" },

            // Стр 5: Totem Log
            { "тотем", "Totem Log", "5", "1" },
            { "totem", "Totem Log", "5", "1" },
            { "тотем лог", "Totem Log", "5", "1" },
            { "лог тотемов", "Totem Log", "5", "1" },
            { "радиус тотемов", "Радиус тотемов", "5", "1" },
            { "звук тотема", "Звук тотема", "5", "1" },

            // Стр 6: PvPSafe
            { "pvp safe", "PvPSafe", "6", "1" },
            { "pvpsafe", "PvPSafe", "6", "1" },
            { "пвп сейф", "PvPSafe", "6", "1" },
            { "бой", "PvPSafe", "6", "1" },
            { "combat", "PvPSafe", "6", "1" },
            { "защита выхода", "Блокировка выхода", "6", "1" },
            { "таймер боя", "Таймер боя", "6", "1" },
            { "блокировка команд", "Блокировка команд", "6", "1" },

            // Стр 7: PickUpLogger
            { "pickup", "PickUpLogger", "7", "1" },
            { "подбор", "PickUpLogger", "7", "1" },
            { "pickup logger", "PickUpLogger", "7", "1" },
            { "лог подбора", "PickUpLogger", "7", "1" },
            { "режим pickup", "Режим PickUpLogger", "7", "1" },
            { "оружие pickup", "Оружие PickUp", "7", "1" },
            { "броня pickup", "Броня PickUp", "7", "1" },
            { "зелья pickup", "Зелья PickUp", "7", "1" },
            { "талисманы pickup", "Талисманы PickUp", "7", "1" },
            { "спавнеры pickup", "Спавнеры PickUp", "7", "1" },

            // ===================== PVE =====================
            // Стр 0: TapeMouse
            { "tape", "TapeMouse", "0", "2" },
            { "тейп", "TapeMouse", "0", "2" },
            { "tape mouse", "TapeMouse", "0", "2" },
            { "автокликер", "TapeMouse", "0", "2" },
            { "autoclicker", "TapeMouse", "0", "2" },
            { "цель tape", "Цель TapeMouse", "0", "2" },
            { "кнопка tape", "Кнопка TapeMouse", "0", "2" },
            { "задержка кликов", "Задержка TapeMouse", "0", "2" },
            { "зажать пкм", "Зажать ПКМ", "0", "2" },

            // ===================== VISUAL =====================
            // Стр 0: Crosshair
            { "прицел", "Кастомный прицел", "0", "3" },
            { "crosshair", "Кастомный прицел", "0", "3" },
            { "форма прицела", "Форма прицела", "0", "3" },
            { "размер прицела", "Размер прицела", "0", "3" },
            { "толщина прицела", "Толщина прицела", "0", "3" },
            { "зазор прицела", "Зазор прицела", "0", "3" },
            { "цвет прицела", "Цвет прицела", "0", "3" },
            { "прозрачность прицела", "Прозрачность прицела", "0", "3" },

            // Стр 1: FOV / AspectRatio
            { "fov", "FOV (Угол обзора)", "1", "3" },
            { "угол обзора", "FOV (Угол обзора)", "1", "3" },
            { "растяг", "Aspect Ratio", "1", "3" },
            { "aspect ratio", "Aspect Ratio", "1", "3" },
            { "растянуть экран", "Aspect Ratio", "1", "3" },

            // Стр 2: Low Fire/Shield
            { "огонь", "Низкий огонь", "2", "3" },
            { "low fire", "Низкий огонь", "2", "3" },
            { "щит", "Низкий щит", "2", "3" },
            { "low shield", "Низкий щит", "2", "3" },
            { "смещение огня", "Смещение огня", "2", "3" },
            { "смещение щита", "Смещение щита", "2", "3" },

            // Стр 3: Zoom
            { "зум", "Zoom (Приближение)", "3", "3" },
            { "zoom", "Zoom (Приближение)", "3", "3" },
            { "приближение", "Zoom (Приближение)", "3", "3" },
            { "сила зума", "Сила зума", "3", "3" },
            { "плавность зума", "Плавность зума", "3", "3" },
            { "клавиша зума", "Клавиша зума", "3", "3" },

            // Стр 4: Темы GUI
            { "тема", "Темы GUI", "4", "3" },
            { "theme", "Темы GUI", "4", "3" },
            { "vanilla тема", "Тема Vanilla", "4", "3" },
            { "dark тема", "Тема Dark", "4", "3" },
            { "neon тема", "Тема Neon", "4", "3" },
            { "candy тема", "Тема Candy", "4", "3" },
            { "blood тема", "Тема Blood", "4", "3" },

            // Стр 5: Waypoints
            { "метка", "Waypoints", "5", "3" },
            { "метки", "Waypoints", "5", "3" },
            { "waypoint", "Waypoints", "5", "3" },
            { "waypoints", "Waypoints", "5", "3" },
            { "точка на карте", "Waypoints", "5", "3" },

            // Стр 6: Camera
            { "hurt", "No Hurt Cam", "6", "3" },
            { "тряска", "No Hurt Cam", "6", "3" },
            { "тряска при уроне", "No Hurt Cam", "6", "3" },
            { "nohurtcam", "No Hurt Cam", "6", "3" },
            { "bobbing", "No Bobbing", "6", "3" },
            { "покачивание", "No Bobbing", "6", "3" },
            { "покачивание камеры", "No Bobbing", "6", "3" },
            { "камера", "Camera", "6", "3" },

            // Стр 7: ItemPhysics
            { "itemphysics", "ItemPhysics", "7", "3" },
            { "физика", "ItemPhysics", "7", "3" },
            { "физика предметов", "ItemPhysics", "7", "3" },
            { "предмет", "ItemPhysics", "7", "3" },
            { "лежат предметы", "ItemPhysics", "7", "3" },

            // Стр 8: Particle Blocker
            { "particle", "Particle Blocker", "8", "3" },
            { "частицы", "Particle Blocker", "8", "3" },
            { "particles", "Particle Blocker", "8", "3" },
            { "particle blocker", "Particle Blocker", "8", "3" },
            { "огонь частицы", "Огонь", "8", "3" },
            { "дым частицы", "Дым", "8", "3" },
            { "взрывы частицы", "Взрывы", "8", "3" },
            { "зелья частицы", "Зелья", "8", "3" },
            { "вода частицы", "Вода", "8", "3" },
            { "редстоун частицы", "Редстоун", "8", "3" },
            { "портал частицы", "Портал", "8", "3" },
            { "криты частицы", "Криты", "8", "3" },

            // ===================== MISC =====================
            // Стр 0: Привязка GUI
            { "клавиша", "Привязка клавиши GUI", "0", "4" },
            { "gui", "Привязка клавиши GUI", "0", "4" },
            { "open gui", "Привязка клавиши GUI", "0", "4" },
            { "открыть gui", "Привязка клавиши GUI", "0", "4" },

            // Стр 1: Конфигурации
            { "конфиг", "Конфигурации", "1", "4" },
            { "config", "Конфигурации", "1", "4" },
            { "сохранить конфиг", "Сохранить конфиг", "1", "4" },
            { "загрузить конфиг", "Загрузить конфиг", "1", "4" },

            // Стр 2: ChatFilter
            { "chatfilter", "ChatFilter", "2", "4" },
            { "фильтр чата", "ChatFilter", "2", "4" },
            { "стоп-слово", "ChatFilter", "2", "4" },
            { "spam", "ChatFilter", "2", "4" },
            { "спам", "ChatFilter", "2", "4" },

            // Стр 3: AutoReconnect
            { "reconnect", "AutoReconnect", "3", "4" },
            { "автореконнект", "AutoReconnect", "3", "4" },
            { "авто реконнект", "AutoReconnect", "3", "4" },
            { "переподключение", "AutoReconnect", "3", "4" },
            { "задержка реконнекта", "Задержка реконнекта", "3", "4" }
    };

    public MyCustomScreen() {
        super(Component.literal("Resistance DLC — Настройки"));
    }

    private void registerScreenEvents() {
        ScreenKeyboardEvents.allowKeyPress(this).register((screen, keyEvent) -> {
            if (this.searchField != null && this.searchField.isFocused()) return true;
            if (this.hexField != null && this.hexField.isFocused()) return true;

            if (!ModConfig.isBindingKey) return true;
            int keyCode = keyEvent.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                ModConfig.isBindingKey = false;
                ModConfig.bindingTarget = 0;
                this.rebuildWidgets();
                return false;
            }
            if (ModConfig.bindingTarget == 0) {
                KeyBindings.setKey(keyCode);
            } else if (ModConfig.bindingTarget == 1) {
                KeyBindings.setZoomKey(keyCode);
            } else if (ModConfig.bindingTarget == 2) {
                KeyBindings.setTapeMouseKey(keyCode);
            } else if (ModConfig.bindingTarget == 3) {
                KeyBindings.setAutoSwapKey(keyCode);
            } else if (ModConfig.bindingTarget == 4) {
                KeyBindings.setCustomHitSoundsKey(keyCode);
            } else if (ModConfig.bindingTarget == 5) {
                KeyBindings.setFastExpKey(keyCode);
            } else if (ModConfig.bindingTarget == 6) {
                KeyBindings.setShiftTapKey(keyCode);
            } else if (ModConfig.bindingTarget == 7) {
                KeyBindings.setComboKey(keyCode);
            } else if (ModConfig.bindingTarget == 8) {
                KeyBindings.setEffectWarningsKey(keyCode);
            } else if (ModConfig.bindingTarget == 9) {
                KeyBindings.setWaypointsKey(keyCode);
            } else if (ModConfig.bindingTarget == 10) {
                KeyBindings.setTotemLogKey(keyCode);
            } else if (ModConfig.bindingTarget == 11) {
                KeyBindings.setPickupLogKey(keyCode);
            }
            ModConfig.isBindingKey = false;
            ModConfig.bindingTarget = 0;
            this.rebuildWidgets();
            return false;
        });

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

        this.searchField = new EditBox(this.font, panelX - 180, panelY + 50, 160, 18,
                Component.literal("Поиск..."));
        this.searchField.setMaxLength(30);
        this.searchField.setResponder(text -> {});
        this.addRenderableWidget(this.searchField);

        int tabX = panelX - 180;
        int tabY = panelY + 185;
        int tabW = 160;
        int tabH = 22;
        int tabGap = 4;

        for (int i = 0; i < SECTION_COUNT; i++) {
            final int sectionIdx = i;
            String label = getSectionName(i, ModConfig.modLogoRussian);
            int pagesInSection = SECTION_PAGES[i];
            String text = label + "  §7(" + pagesInSection + ")";

            Button tabBtn = Button.builder(Component.literal(text), (b) -> {
                currentSection = sectionIdx;
                currentPage = 0;
                this.searchOtherSectionMsg = "";
                this.closeRenameDialogSilent();
                this.rebuildWidgets();
            }).bounds(tabX, tabY + i * (tabH + tabGap), tabW, tabH).build();
            this.addRenderableWidget(tabBtn);
        }

        Button prevPageBtn = Button.builder(Component.literal("←"), (btn) -> {
            navigatePrev();
            this.closeRenameDialogSilent();
            this.rebuildWidgets();
        }).bounds(panelX + 10, panelY + 385, 20, 20).build();
        prevPageBtn.active = currentPage > 0;
        this.addRenderableWidget(prevPageBtn);

        Button nextPageBtn = Button.builder(Component.literal("→"), (btn) -> {
            navigateNext();
            this.closeRenameDialogSilent();
            this.rebuildWidgets();
        }).bounds(panelX + panelWidth - 30, panelY + 385, 20, 20).build();
        nextPageBtn.active = currentPage < SECTION_PAGES[currentSection] - 1;
        this.addRenderableWidget(nextPageBtn);

        Button closeButton = Button.builder(Component.literal("Закрыть"), (btn) -> this.onClose())
                .bounds(panelX + panelWidth - 90, panelY + 360, 70, 18).build();
        this.addRenderableWidget(closeButton);

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
        if (currentPage > 0) currentPage--;
    }

    private void navigateNext() {
        if (currentPage < SECTION_PAGES[currentSection] - 1) currentPage++;
    }

    private void initCurrentPage(int centerX, int panelY) {
        switch (currentSection) {
            case 0 -> {
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
            case 1 -> {
                switch (currentPage) {
                    case 0 -> initPage6(centerX, panelY);
                    case 1 -> initPage11(centerX, panelY);
                    case 2 -> initPage12(centerX, panelY);
                    case 3 -> initPage13(centerX, panelY);
                    case 4 -> initPage3b(centerX, panelY);
                    case 5 -> initTotemLogPage(centerX, panelY);
                    case 6 -> initPvPSafePage(centerX, panelY);
                    case 7 -> initPickUpLoggerPage(centerX, panelY);
                }
            }
            case 2 -> {
                switch (currentPage) {
                    case 0 -> initPage4(centerX, panelY);
                }
            }
            case 3 -> {
                switch (currentPage) {
                    case 0 -> initPage16(centerX, panelY);
                    case 1 -> initPage5(centerX, panelY);
                    case 2 -> initPage9(centerX, panelY);
                    case 3 -> initPage10(centerX, panelY);
                    case 4 -> initThemesPage(centerX, panelY);
                    case 5 -> initWaypointsPage(centerX, panelY);
                    case 6 -> initCameraPage(centerX, panelY);
                    case 7 -> initItemPhysicsPage(centerX, panelY);
                    case 8 -> initParticleBlockerPage(centerX, panelY);
                }
            }
            case 4 -> {
                switch (currentPage) {
                    case 0 -> initPage2(centerX, panelY);
                    case 1 -> initConfigsPage(centerX, panelY);
                    case 2 -> initChatFilterPage(centerX, panelY);
                    case 3 -> initAutoReconnectPage(centerX, panelY);
                }
            }
        }
    }
    // =========================================================
    // AUTO RECONNECT (Misc, index 3)
    // =========================================================
    private void initAutoReconnectPage(int centerX, int panelY) {
        // ===== ВКЛ/ВЫКЛ =====
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.autoReconnectRussian ? "Включить AutoReconnect" : "Enable AutoReconnect"), this.font)
                .pos(centerX - 100, panelY + 70)
                .selected(ModConfig.autoReconnectEnabled)
                .onValueChange((c, v) -> { ModConfig.autoReconnectEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.autoReconnectRussian = !ModConfig.autoReconnectRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 70, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        // ===== СЛАЙДЕР ЗАДЕРЖКИ =====
        AbstractSliderButton delaySlider = new AbstractSliderButton(
                centerX - 100, panelY + 110, 200, 20,
                Component.literal(ModConfig.autoReconnectRussian
                        ? ("Задержка: " + ModConfig.autoReconnectDelay + " сек")
                        : ("Delay: " + ModConfig.autoReconnectDelay + " sec")),
                (ModConfig.autoReconnectDelay - 1) / 29.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.autoReconnectRussian
                        ? ("Задержка: " + ModConfig.autoReconnectDelay + " сек")
                        : ("Delay: " + ModConfig.autoReconnectDelay + " sec")));
            }
            @Override protected void applyValue() {
                ModConfig.autoReconnectDelay = 1 + (int)(this.value * 29);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(delaySlider);

        // ===== HUD-ИНДИКАТОР =====
        Checkbox hudCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.autoReconnectRussian ? "Показывать HUD-таймер" : "Show HUD timer"), this.font)
                .pos(centerX - 100, panelY + 150)
                .selected(ModConfig.autoReconnectShowHud)
                .onValueChange((c, v) -> { ModConfig.autoReconnectShowHud = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(hudCheckbox);
    }

    // =========================================================
    // TOTEM LOG (PVP, index 5)
    // =========================================================
    private void initTotemLogPage(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.totemLogRussian ? "Включить Totem Log" : "Enable Totem Log"), this.font)
                .pos(centerX - 100, panelY + 70)
                .selected(ModConfig.totemLogEnabled)
                .onValueChange((c, v) -> { ModConfig.totemLogEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.totemLogRussian = !ModConfig.totemLogRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 70, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String tlKeyName = KeyBindings.totemLogKey != null
                ? KeyBindings.totemLogKey.getTranslatedKeyMessage().getString() : "O";
        Button keyBindBtn = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 10
                                ? "Нажмите клавишу..."
                                : (ModConfig.totemLogRussian ? "Клавиша: " : "Key: ") + tlKeyName),
                        (b) -> {
                            ModConfig.isBindingKey = true;
                            ModConfig.bindingTarget = 10;
                            b.setMessage(Component.literal("Нажмите клавишу..."));
                        })
                .bounds(centerX - 100, panelY + 105, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        AbstractSliderButton radiusSlider = new AbstractSliderButton(
                centerX - 100, panelY + 140, 200, 20,
                Component.literal(ModConfig.totemLogRussian
                        ? ("Радиус: " + ModConfig.totemLogRadius + " блоков")
                        : ("Radius: " + ModConfig.totemLogRadius + " blocks")),
                (ModConfig.totemLogRadius - 5) / 15.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.totemLogRussian
                        ? ("Радиус: " + ModConfig.totemLogRadius + " блоков")
                        : ("Radius: " + ModConfig.totemLogRadius + " blocks")));
            }
            @Override protected void applyValue() {
                ModConfig.totemLogRadius = 5 + (int)(this.value * 15.0);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(radiusSlider);

        Checkbox soundCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.totemLogRussian ? "Звук-уведомление" : "Sound notification"), this.font)
                .pos(centerX - 100, panelY + 175)
                .selected(ModConfig.totemLogSound)
                .onValueChange((c, v) -> { ModConfig.totemLogSound = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(soundCheckbox);
    }

    // =========================================================
    // PVP SAFE (PVP, index 6)
    // =========================================================
    private void initPvPSafePage(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.pvpSafeRussian ? "Включить PvPSafe" : "Enable PvPSafe"), this.font)
                .pos(centerX - 100, panelY + 70)
                .selected(ModConfig.pvpSafeEnabled)
                .onValueChange((c, v) -> { ModConfig.pvpSafeEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.pvpSafeRussian = !ModConfig.pvpSafeRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 70, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        AbstractSliderButton timerSlider = new AbstractSliderButton(
                centerX - 100, panelY + 110, 200, 20,
                Component.literal(ModConfig.pvpSafeRussian
                        ? ("Таймер боя: " + ModConfig.pvpSafeTimer + " сек")
                        : ("Combat timer: " + ModConfig.pvpSafeTimer + " sec")),
                (ModConfig.pvpSafeTimer - 10) / 50.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.pvpSafeRussian
                        ? ("Таймер боя: " + ModConfig.pvpSafeTimer + " сек")
                        : ("Combat timer: " + ModConfig.pvpSafeTimer + " sec")));
            }
            @Override protected void applyValue() {
                ModConfig.pvpSafeTimer = 10 + (int)(this.value * 50);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(timerSlider);

        Checkbox quitCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.pvpSafeRussian ? "Блокировать выход (ESC)" : "Block quit (ESC)"), this.font)
                .pos(centerX - 100, panelY + 150)
                .selected(ModConfig.pvpSafeBlockQuit)
                .onValueChange((c, v) -> { ModConfig.pvpSafeBlockQuit = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(quitCheckbox);

        Checkbox cmdCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.pvpSafeRussian ? "Блокировать команды" : "Block commands"), this.font)
                .pos(centerX - 100, panelY + 180)
                .selected(ModConfig.pvpSafeBlockCommands)
                .onValueChange((c, v) -> { ModConfig.pvpSafeBlockCommands = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(cmdCheckbox);

        Checkbox hudCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.pvpSafeRussian ? "Показывать HUD-таймер" : "Show HUD timer"), this.font)
                .pos(centerX - 100, panelY + 215)
                .selected(ModConfig.pvpSafeShowHud)
                .onValueChange((c, v) -> { ModConfig.pvpSafeShowHud = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(hudCheckbox);

        makePosEditor(centerX, panelY, 255,
                () -> ModConfig.pvpSafeHudX, () -> ModConfig.pvpSafeHudY,
                (x, y) -> { ModConfig.pvpSafeHudX = x; ModConfig.pvpSafeHudY = y; },
                10, 240);
    }

    // =========================================================
    // PICKUP LOGGER (PVP, index 7)
    // =========================================================
    private void initPickUpLoggerPage(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.pickupLogRussian ? "Включить PickUpLogger" : "Enable PickUpLogger"), this.font)
                .pos(centerX - 100, panelY + 65)
                .selected(ModConfig.pickupLogEnabled)
                .onValueChange((c, v) -> { ModConfig.pickupLogEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.pickupLogRussian = !ModConfig.pickupLogRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 65, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String plKeyName = KeyBindings.pickupLogKey != null
                ? KeyBindings.pickupLogKey.getTranslatedKeyMessage().getString() : "P";
        Button keyBindBtn = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 11
                                ? "Нажмите клавишу..."
                                : (ModConfig.pickupLogRussian ? "Клавиша: " : "Key: ") + plKeyName),
                        (b) -> {
                            ModConfig.isBindingKey = true;
                            ModConfig.bindingTarget = 11;
                            b.setMessage(Component.literal("Нажмите клавишу..."));
                        })
                .bounds(centerX - 100, panelY + 100, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        String[] modesRu = {"Режим: Все предметы", "Режим: Только ценные", "Режим: По категориям"};
        String[] modesEn = {"Mode: All items", "Mode: Valuable only", "Mode: By categories"};
        Button modeBtn = Button.builder(
                Component.literal(ModConfig.pickupLogRussian ? modesRu[ModConfig.pickupLogMode] : modesEn[ModConfig.pickupLogMode]),
                (b) -> {
                    ModConfig.pickupLogMode = (ModConfig.pickupLogMode + 1) % 3;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 135, 200, 20).build();
        this.addRenderableWidget(modeBtn);

        if (ModConfig.pickupLogMode == 2) {
            Checkbox weaponCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.pickupLogRussian ? "Оружие" : "Weapon"), this.font)
                    .pos(centerX - 100, panelY + 175)
                    .selected(ModConfig.pickupLogWeapon)
                    .onValueChange((c, v) -> { ModConfig.pickupLogWeapon = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(weaponCheckbox);

            Checkbox armorCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.pickupLogRussian ? "Броня" : "Armor"), this.font)
                    .pos(centerX - 100, panelY + 200)
                    .selected(ModConfig.pickupLogArmor)
                    .onValueChange((c, v) -> { ModConfig.pickupLogArmor = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(armorCheckbox);

            Checkbox potionsCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.pickupLogRussian ? "Зелья" : "Potions"), this.font)
                    .pos(centerX - 100, panelY + 225)
                    .selected(ModConfig.pickupLogPotions)
                    .onValueChange((c, v) -> { ModConfig.pickupLogPotions = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(potionsCheckbox);

            Checkbox totemsCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.pickupLogRussian ? "Талисманы" : "Totems"), this.font)
                    .pos(centerX - 100, panelY + 250)
                    .selected(ModConfig.pickupLogTotems)
                    .onValueChange((c, v) -> { ModConfig.pickupLogTotems = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(totemsCheckbox);

            Checkbox headsCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.pickupLogRussian ? "Головы игроков" : "Player heads"), this.font)
                    .pos(centerX - 100, panelY + 275)
                    .selected(ModConfig.pickupLogHeads)
                    .onValueChange((c, v) -> { ModConfig.pickupLogHeads = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(headsCheckbox);

            Checkbox spawnersCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.pickupLogRussian ? "Спавнеры" : "Spawners"), this.font)
                    .pos(centerX - 100, panelY + 300)
                    .selected(ModConfig.pickupLogSpawners)
                    .onValueChange((c, v) -> { ModConfig.pickupLogSpawners = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(spawnersCheckbox);

            Checkbox structureCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.pickupLogRussian ? "Блоки пазл/конструктор" : "Jigsaw/Structure blocks"), this.font)
                    .pos(centerX - 100, panelY + 325)
                    .selected(ModConfig.pickupLogStructureBlocks)
                    .onValueChange((c, v) -> { ModConfig.pickupLogStructureBlocks = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(structureCheckbox);
        }
    }

    // =========================================================
    // CAMERA (Visual, index 6)
    // =========================================================
    private void initCameraPage(int centerX, int panelY) {
        Checkbox noHurtCamCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.cameraRussian ? "Отключить тряску при уроне" : "No Hurt Cam"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.noHurtCamEnabled)
                .onValueChange((c, v) -> { ModConfig.noHurtCamEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(noHurtCamCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.cameraRussian = !ModConfig.cameraRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        Checkbox noBobbingCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.cameraRussian ? "Отключить покачивание камеры" : "No Bobbing"), this.font)
                .pos(centerX - 100, panelY + 120)
                .selected(ModConfig.noBobbingEnabled)
                .onValueChange((c, v) -> { ModConfig.noBobbingEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(noBobbingCheckbox);
    }

    // =========================================================
    // ITEM PHYSICS (Visual, index 7)
    // =========================================================
    private void initItemPhysicsPage(int centerX, int panelY) {
        // ===== ВКЛ/ВЫКЛ =====
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.itemPhysicsRussian ? "Включить ItemPhysics" : "Enable ItemPhysics"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.itemPhysicsEnabled)
                .onValueChange((c, v) -> { ModConfig.itemPhysicsEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.itemPhysicsRussian = !ModConfig.itemPhysicsRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);
    }
    // =========================================================
    // PARTICLE BLOCKER (Visual, index 8)
    // =========================================================
    private void initParticleBlockerPage(int centerX, int panelY) {
        // ===== ВКЛ/ВЫКЛ ВСЕГО =====
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Включить Particle Blocker" : "Enable Particle Blocker"), this.font)
                .pos(centerX - 100, panelY + 75)
                .selected(ModConfig.particleBlockerEnabled)
                .onValueChange((c, v) -> { ModConfig.particleBlockerEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.particleBlockerRussian = !ModConfig.particleBlockerRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 75, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        // ===== КАТЕГОРИИ (2 столбца) =====
        int col1X = centerX - 100;
        int col2X = centerX + 10;
        int row1Y = panelY + 110;
        int rowStep = 22;

        // Столбец 1
        Checkbox fireCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Огонь" : "Fire"), this.font)
                .pos(col1X, row1Y)
                .selected(ModConfig.particleBlockerFire)
                .onValueChange((c, v) -> { ModConfig.particleBlockerFire = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(fireCheckbox);

        Checkbox smokeCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Дым" : "Smoke"), this.font)
                .pos(col1X, row1Y + rowStep)
                .selected(ModConfig.particleBlockerSmoke)
                .onValueChange((c, v) -> { ModConfig.particleBlockerSmoke = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(smokeCheckbox);

        Checkbox explosionCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Взрывы" : "Explosions"), this.font)
                .pos(col1X, row1Y + rowStep * 2)
                .selected(ModConfig.particleBlockerExplosion)
                .onValueChange((c, v) -> { ModConfig.particleBlockerExplosion = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(explosionCheckbox);

        Checkbox potionCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Зелья" : "Potions"), this.font)
                .pos(col1X, row1Y + rowStep * 3)
                .selected(ModConfig.particleBlockerPotions)
                .onValueChange((c, v) -> { ModConfig.particleBlockerPotions = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(potionCheckbox);

        // Столбец 2
        Checkbox waterCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Вода" : "Water"), this.font)
                .pos(col2X, row1Y)
                .selected(ModConfig.particleBlockerWater)
                .onValueChange((c, v) -> { ModConfig.particleBlockerWater = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(waterCheckbox);

        Checkbox redstoneCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Редстоун" : "Redstone"), this.font)
                .pos(col2X, row1Y + rowStep)
                .selected(ModConfig.particleBlockerRedstone)
                .onValueChange((c, v) -> { ModConfig.particleBlockerRedstone = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(redstoneCheckbox);

        Checkbox portalCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Портал" : "Portal"), this.font)
                .pos(col2X, row1Y + rowStep * 2)
                .selected(ModConfig.particleBlockerPortal)
                .onValueChange((c, v) -> { ModConfig.particleBlockerPortal = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(portalCheckbox);

        Checkbox critCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.particleBlockerRussian ? "Криты" : "Crits"), this.font)
                .pos(col2X, row1Y + rowStep * 3)
                .selected(ModConfig.particleBlockerCrit)
                .onValueChange((c, v) -> { ModConfig.particleBlockerCrit = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(critCheckbox);
    }
    // =========================================================
    // CHAT FILTER (Misc, index 2)
    // =========================================================
    private void initChatFilterPage(int centerX, int panelY) {
        // ===== ВКЛ/ВЫКЛ =====
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.chatFilterRussian ? "Включить ChatFilter" : "Enable ChatFilter"), this.font)
                .pos(centerX - 100, panelY + 65)
                .selected(ModConfig.chatFilterEnabled)
                .onValueChange((c, v) -> { ModConfig.chatFilterEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.chatFilterRussian = !ModConfig.chatFilterRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 65, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        // ===== ДОБАВЛЕНИЕ СЛОВА =====
        EditBox wordField = new EditBox(this.font, centerX - 100, panelY + 100, 160, 20,
                Component.literal(ModConfig.chatFilterRussian ? "Стоп-слово..." : "Stop word..."));
        wordField.setMaxLength(30);
        this.addRenderableWidget(wordField);

        Button addBtn = Button.builder(Component.literal("+"), (b) -> {
            String word = wordField.getValue().trim();
            if (!word.isEmpty()) {
                ChatFilterManager.addWord(word);
                wordField.setValue("");
                this.rebuildWidgets();
            }
        }).bounds(centerX + 65, panelY + 100, 35, 20).build();
        this.addRenderableWidget(addBtn);

        // ===== СПИСОК СТОП-СЛОВ (кнопки ×) =====
        List<String> words = ChatFilterManager.getWords();
        int listStartY = panelY + 140;
        int rowHeight = 20;
        int maxVisible = 9;

        for (int i = 0; i < Math.min(words.size(), maxVisible); i++) {
            final String word = words.get(i);
            int y = listStartY + i * rowHeight;

            Button delBtn = Button.builder(Component.literal("×"), (b) -> {
                ChatFilterManager.removeWord(word);
                this.rebuildWidgets();
            }).bounds(centerX + 70, y, 20, 18).build();
            this.addRenderableWidget(delBtn);
        }

        // ===== КНОПКА "ОЧИСТИТЬ ВСЁ" =====
        Button clearBtn = Button.builder(
                Component.literal(ModConfig.chatFilterRussian ? "Очистить всё" : "Clear all"), (b) -> {
                    ChatFilterManager.clearWords();
                    this.rebuildWidgets();
                }).bounds(centerX - 100, panelY + 335, 200, 20).build();
        this.addRenderableWidget(clearBtn);
    }

    private static void addSearchHistory(String query) {
        if (query == null) return;
        query = query.trim().toLowerCase();
        if (query.isEmpty()) return;

        List<String> history = new ArrayList<>();
        if (!ModConfig.searchHistoryRaw.isEmpty()) {
            for (String s : ModConfig.searchHistoryRaw.split("\\|")) {
                if (!s.isEmpty()) history.add(s);
            }
        }

        history.remove(query);
        history.add(0, query);

        while (history.size() > ModConfig.SEARCH_HISTORY_MAX) {
            history.remove(history.size() - 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) sb.append("|");
            sb.append(history.get(i));
        }
        ModConfig.searchHistoryRaw = sb.toString();

        ConfigManager.save();
    }

    private static List<String> getSearchHistory() {
        List<String> history = new ArrayList<>();
        if (ModConfig.searchHistoryRaw == null || ModConfig.searchHistoryRaw.isEmpty()) return history;
        for (String s : ModConfig.searchHistoryRaw.split("\\|")) {
            if (!s.isEmpty()) history.add(s);
        }
        return history;
    }

    public static List<Waypoint> getWaypoints() {
        List<Waypoint> list = new ArrayList<>();
        if (ModConfig.waypointsRaw == null || ModConfig.waypointsRaw.isEmpty()) return list;
        for (String s : ModConfig.waypointsRaw.split("\\|")) {
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
        ModConfig.waypointsRaw = sb.toString();
    }

    public static boolean addWaypoint(double x, double y, double z) {
        List<Waypoint> list = getWaypoints();
        if (list.size() >= ModConfig.waypointsMax) return false;
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
        ModConfig.waypointsRaw = "";
        ConfigManager.save();
    }

    public static boolean renameWaypoint(String oldName, String newName) {
        if (oldName == null || newName == null) return false;
        if (oldName.isEmpty() || newName.isEmpty()) return false;

        String safeNewName = newName.replace(":", "_").replace("|", "_");

        List<Waypoint> list = getWaypoints();
        boolean renamed = false;
        for (int i = 0; i < list.size(); i++) {
            Waypoint wp = list.get(i);
            if (wp.name().equals(oldName)) {
                list.set(i, new Waypoint(safeNewName, wp.x(), wp.y(), wp.z()));
                renamed = true;
            }
        }
        if (renamed) {
            setWaypoints(list);
            ConfigManager.save();
        }
        return renamed;
    }

    private void openRenameDialog(int index, String oldName) {
        this.renameDialogOpen = true;
        this.renameDialogIndex = index;
        this.renameDialogOldName = oldName;
        this.rebuildWidgets();
    }

    private void closeRenameDialog() {
        this.renameDialogOpen = false;
        this.renameDialogIndex = -1;
        this.renameDialogOldName = "";
        this.rebuildWidgets();
    }

    private void closeRenameDialogSilent() {
        this.renameDialogOpen = false;
        this.renameDialogIndex = -1;
        this.renameDialogOldName = "";
    }

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

    private void initAppearancePage(int centerX, int panelY) {
        Checkbox hudCheckbox = Checkbox.builder(Component.literal("Показывать HUD"), this.font)
                .pos(centerX - 100, panelY + 45).selected(ModConfig.showHud)
                .onValueChange((c, v) -> { ModConfig.showHud = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudCheckbox);

        Checkbox hudBgCheckbox = Checkbox.builder(Component.literal("Фон HUD"), this.font)
                .pos(centerX + 30, panelY + 45).selected(ModConfig.hudBackgroundEnabled)
                .onValueChange((c, v) -> { ModConfig.hudBackgroundEnabled = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hudBgCheckbox);

        this.hexField = new EditBox(this.font, centerX - 130, panelY + 78, 80, 18,
                Component.literal("#RRGGBB"));
        this.hexField.setMaxLength(7);
        this.hexField.setValue(String.format("#%06X", ModConfig.hudColor & 0xFFFFFF));
        this.addRenderableWidget(this.hexField);

        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = this.hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.hudColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 78, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button greenBtn = Button.builder(Component.literal("Зел"), (b) -> { this.hexField.setValue("#00FF00"); ModConfig.hudColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 78, 40, 18).build();
        this.addRenderableWidget(greenBtn);
        Button redBtn = Button.builder(Component.literal("Крас"), (b) -> { this.hexField.setValue("#FF0000"); ModConfig.hudColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 78, 40, 18).build();
        this.addRenderableWidget(redBtn);
        Button blueBtn = Button.builder(Component.literal("Син"), (b) -> { this.hexField.setValue("#0000FF"); ModConfig.hudColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 78, 40, 18).build();
        this.addRenderableWidget(blueBtn);
        Button whiteBtn = Button.builder(Component.literal("Бел"), (b) -> { this.hexField.setValue("#FFFFFF"); ModConfig.hudColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 78, 40, 18).build();
        this.addRenderableWidget(whiteBtn);

        EditBox hudBgColorField = new EditBox(this.font, centerX - 130, panelY + 126, 80, 18,
                Component.literal("#RRGGBB"));
        hudBgColorField.setMaxLength(7);
        hudBgColorField.setValue(String.format("#%06X", ModConfig.hudBackgroundColor & 0xFFFFFF));
        this.addRenderableWidget(hudBgColorField);

        Button applyHudBgColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = hudBgColorField.getValue().replace("#", "").trim();
            try {
                ModConfig.hudBackgroundColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 126, 40, 18).build();
        this.addRenderableWidget(applyHudBgColorBtn);

        Button hudBgGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { hudBgColorField.setValue("#00FF00"); ModConfig.hudBackgroundColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgGreenBtn);
        Button hudBgRedBtn = Button.builder(Component.literal("Крас"), (b) -> { hudBgColorField.setValue("#FF0000"); ModConfig.hudBackgroundColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgRedBtn);
        Button hudBgBlueBtn = Button.builder(Component.literal("Син"), (b) -> { hudBgColorField.setValue("#0000FF"); ModConfig.hudBackgroundColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgBlueBtn);
        Button hudBgBlackBtn = Button.builder(Component.literal("Чёрн"), (b) -> { hudBgColorField.setValue("#000000"); ModConfig.hudBackgroundColor = 0xFF000000; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 126, 40, 18).build();
        this.addRenderableWidget(hudBgBlackBtn);

        EditBox guiColorField = new EditBox(this.font, centerX - 130, panelY + 174, 80, 18,
                Component.literal("#RRGGBB"));
        guiColorField.setMaxLength(7);
        guiColorField.setValue(String.format("#%06X", ModConfig.guiColor & 0xFFFFFF));
        this.addRenderableWidget(guiColorField);

        Button applyGuiColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = guiColorField.getValue().replace("#", "").trim();
            try {
                ModConfig.guiColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 174, 40, 18).build();
        this.addRenderableWidget(applyGuiColorBtn);

        Button guiGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiColorField.setValue("#00FF00"); ModConfig.guiColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiGreenBtn);
        Button guiRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiColorField.setValue("#FF0000"); ModConfig.guiColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiRedBtn);
        Button guiBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiColorField.setValue("#0000FF"); ModConfig.guiColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 174, 40, 18).build();
        this.addRenderableWidget(guiBlueBtn);

        EditBox guiTextColorField = new EditBox(this.font, centerX - 130, panelY + 222, 80, 18,
                Component.literal("#RRGGBB"));
        guiTextColorField.setMaxLength(7);
        guiTextColorField.setValue(String.format("#%06X", ModConfig.guiTextColor & 0xFFFFFF));
        this.addRenderableWidget(guiTextColorField);

        Button applyGuiTextColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = guiTextColorField.getValue().replace("#", "").trim();
            try {
                ModConfig.guiTextColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 222, 40, 18).build();
        this.addRenderableWidget(applyGuiTextColorBtn);

        Button textGreenBtn = Button.builder(Component.literal("Зел"), (b) -> { guiTextColorField.setValue("#00FF00"); ModConfig.guiTextColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textGreenBtn);
        Button textRedBtn = Button.builder(Component.literal("Крас"), (b) -> { guiTextColorField.setValue("#FF0000"); ModConfig.guiTextColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textRedBtn);
        Button textBlueBtn = Button.builder(Component.literal("Син"), (b) -> { guiTextColorField.setValue("#0000FF"); ModConfig.guiTextColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 222, 40, 18).build();
        this.addRenderableWidget(textBlueBtn);

        Button resetColorsBtn = Button.builder(Component.literal("Сбросить цвета на Vanilla"), (b) -> {
            ModConfig.guiColor = 0xFF00FF00;
            ModConfig.guiTextColor = 0xFFFFFFFF;
            ModConfig.hudColor = 0xFF00FF00;
            ModConfig.hudBackgroundColor = 0xFF000000;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 270, 200, 20).build();
        this.addRenderableWidget(resetColorsBtn);
    }

    private void initPage0(int centerX, int panelY) {
    }

    private void initPage1(int centerX, int panelY) {
        Checkbox coordsCheckbox = Checkbox.builder(Component.literal("Показывать координаты"), this.font)
                .pos(centerX - 100, panelY + 60).selected(ModConfig.showCoords)
                .onValueChange((c, v) -> { ModConfig.showCoords = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(coordsCheckbox);

        Checkbox biomeCheckbox = Checkbox.builder(Component.literal("Показывать биом"), this.font)
                .pos(centerX - 100, panelY + 82).selected(ModConfig.showBiome)
                .onValueChange((c, v) -> { ModConfig.showBiome = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(biomeCheckbox);

        Checkbox timeCheckbox = Checkbox.builder(Component.literal("Показывать время"), this.font)
                .pos(centerX - 100, panelY + 104).selected(ModConfig.showTime)
                .onValueChange((c, v) -> { ModConfig.showTime = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(timeCheckbox);

        Checkbox modLogoCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать иконку мода" : "Show mod logo"), this.font)
                .pos(centerX - 100, panelY + 126).selected(ModConfig.showModLogo)
                .onValueChange((c, v) -> { ModConfig.showModLogo = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(modLogoCheckbox);

        Button modLogoTranslate = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.modLogoRussian = !ModConfig.modLogoRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 126, 25, 20).build();
        this.addRenderableWidget(modLogoTranslate);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 178, 200, 20,
                Component.literal("Прозрачность текста HUD: " + ModConfig.hudAlpha), ModConfig.hudAlpha / 255.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Прозрачность текста HUD: " + ModConfig.hudAlpha)); }
            @Override protected void applyValue() { ModConfig.hudAlpha = (int)(this.value * 255); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(alphaSlider);

        AbstractSliderButton bgAlphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 208, 200, 20,
                Component.literal("Прозрачность фона HUD: " + ModConfig.hudBackgroundAlpha), ModConfig.hudBackgroundAlpha / 255.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Прозрачность фона HUD: " + ModConfig.hudBackgroundAlpha)); }
            @Override protected void applyValue() { ModConfig.hudBackgroundAlpha = (int)(this.value * 255); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(bgAlphaSlider);

        AbstractSliderButton bgHeightSlider = new AbstractSliderButton(
                centerX - 100, panelY + 238, 200, 20,
                Component.literal("Высота фона HUD: " + ModConfig.hudBackgroundHeight),
                (ModConfig.hudBackgroundHeight - 6) / 24.0) {
            @Override protected void updateMessage() { this.setMessage(Component.literal("Высота фона HUD: " + ModConfig.hudBackgroundHeight)); }
            @Override protected void applyValue() { ModConfig.hudBackgroundHeight = 6 + (int)(this.value * 24); this.updateMessage(); ConfigManager.save(); }
        };
        this.addRenderableWidget(bgHeightSlider);
    }

    private void initPage2(int centerX, int panelY) {
        String keyName = KeyBindings.openGuiKey != null
                ? KeyBindings.openGuiKey.getTranslatedKeyMessage().getString() : "G";

        Button keyBindButton = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 0 ? "Нажмите клавишу..." : "Клавиша: " + keyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 0; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 100, 200, 20).build();
        this.addRenderableWidget(keyBindButton);

        Button resetKeyBtn = Button.builder(Component.literal("Сбросить на G"), (b) -> {
            KeyBindings.setKey(GLFW.GLFW_KEY_G);
            ModConfig.isBindingKey = false;
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 140, 200, 20).build();
        this.addRenderableWidget(resetKeyBtn);
    }

    private void initPage3(int centerX, int panelY) {
        Checkbox fpsCheckbox = Checkbox.builder(Component.literal(ModConfig.fpsRussian ? "КВС" : "FPS"), this.font)
                .pos(centerX - 100, panelY + 45).selected(ModConfig.showFps)
                .onValueChange((c, v) -> { ModConfig.showFps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(fpsCheckbox);
        Button fpsTranslate = Button.builder(Component.literal("RU"), (b) -> { ModConfig.fpsRussian = !ModConfig.fpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 45, 25, 20).build();
        this.addRenderableWidget(fpsTranslate);

        makePosEditor(centerX, panelY, 70,
                () -> ModConfig.fpsX, () -> ModConfig.fpsY,
                (x, y) -> { ModConfig.fpsX = x; ModConfig.fpsY = y; },
                10, 80);

        Checkbox pingCheckbox = Checkbox.builder(Component.literal(ModConfig.pingRussian ? "Пинг" : "Ping"), this.font)
                .pos(centerX - 100, panelY + 112).selected(ModConfig.showPing)
                .onValueChange((c, v) -> { ModConfig.showPing = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(pingCheckbox);
        Button pingTranslate = Button.builder(Component.literal("RU"), (b) -> { ModConfig.pingRussian = !ModConfig.pingRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 112, 25, 20).build();
        this.addRenderableWidget(pingTranslate);

        makePosEditor(centerX, panelY, 137,
                () -> ModConfig.pingX, () -> ModConfig.pingY,
                (x, y) -> { ModConfig.pingX = x; ModConfig.pingY = y; },
                10, 95);

        Checkbox tpsCheckbox = Checkbox.builder(Component.literal(ModConfig.tpsRussian ? "ТВС" : "TPS"), this.font)
                .pos(centerX - 100, panelY + 179).selected(ModConfig.showTps)
                .onValueChange((c, v) -> { ModConfig.showTps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(tpsCheckbox);
        Button tpsTranslate = Button.builder(Component.literal("RU"), (b) -> { ModConfig.tpsRussian = !ModConfig.tpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 179, 25, 20).build();
        this.addRenderableWidget(tpsTranslate);

        makePosEditor(centerX, panelY, 204,
                () -> ModConfig.tpsX, () -> ModConfig.tpsY,
                (x, y) -> { ModConfig.tpsX = x; ModConfig.tpsY = y; },
                10, 110);

        Checkbox bpsCheckbox = Checkbox.builder(Component.literal(ModConfig.bpsRussian ? "БВС" : "BPS"), this.font)
                .pos(centerX - 100, panelY + 246).selected(ModConfig.showBps)
                .onValueChange((c, v) -> { ModConfig.showBps = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(bpsCheckbox);
        Button bpsTranslate = Button.builder(Component.literal("RU"), (b) -> { ModConfig.bpsRussian = !ModConfig.bpsRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 246, 25, 20).build();
        this.addRenderableWidget(bpsTranslate);

        makePosEditor(centerX, panelY, 271,
                () -> ModConfig.bpsX, () -> ModConfig.bpsY,
                (x, y) -> { ModConfig.bpsX = x; ModConfig.bpsY = y; },
                10, 125);

        Checkbox dirCheckbox = Checkbox.builder(Component.literal(ModConfig.directionRussian ? "Направление" : "Direction"), this.font)
                .pos(centerX - 100, panelY + 313).selected(ModConfig.showDirection)
                .onValueChange((c, v) -> { ModConfig.showDirection = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(dirCheckbox);
        Button dirTranslate = Button.builder(Component.literal("RU"), (b) -> { ModConfig.directionRussian = !ModConfig.directionRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 313, 25, 20).build();
        this.addRenderableWidget(dirTranslate);

        makePosEditor(centerX, panelY, 338,
                () -> ModConfig.directionX, () -> ModConfig.directionY,
                (x, y) -> { ModConfig.directionX = x; ModConfig.directionY = y; },
                10, 140);

        Checkbox hitCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.hitCounterRussian ? "Удары до смерти" : "Hits to kill"), this.font)
                .pos(centerX - 100, panelY + 380).selected(ModConfig.showHitCounter)
                .onValueChange((c, v) -> { ModConfig.showHitCounter = v; ConfigManager.save(); }).build();
        this.addRenderableWidget(hitCheckbox);
        Button hitTranslate = Button.builder(Component.literal("RU"), (b) -> { ModConfig.hitCounterRussian = !ModConfig.hitCounterRussian; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 120, panelY + 380, 25, 20).build();
        this.addRenderableWidget(hitTranslate);
    }

    private void initPage3b(int centerX, int panelY) {
        Checkbox autoSprintCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.autoSprintRussian ? "Включить AutoSprint" : "Enable AutoSprint"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.autoSprintEnabled)
                .onValueChange((c, v) -> { ModConfig.autoSprintEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(autoSprintCheckbox);

        Button sprintTranslate = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.autoSprintRussian = !ModConfig.autoSprintRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(sprintTranslate);
    }

    private void initPage4(int centerX, int panelY) {
        Checkbox tapeMouseCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.tapeMouseRussian ? "Включить TapeMouse" : "Enable TapeMouse"), this.font)
                .pos(centerX - 100, panelY + 75)
                .selected(ModConfig.tapeMouseEnabled)
                .onValueChange((c, v) -> { ModConfig.tapeMouseEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(tapeMouseCheckbox);

        Button tapeMouseTranslate = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.tapeMouseRussian = !ModConfig.tapeMouseRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 145, panelY + 75, 25, 20).build();
        this.addRenderableWidget(tapeMouseTranslate);

        Button buttonModeBtn = Button.builder(
                Component.literal(getButtonName(ModConfig.tapeMouseButton, ModConfig.tapeMouseRussian)),
                (b) -> {
                    ModConfig.tapeMouseButton = (ModConfig.tapeMouseButton + 1) % 2;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 100, 200, 20).build();
        this.addRenderableWidget(buttonModeBtn);

        if (ModConfig.tapeMouseButton == 0) {
            Button targetBtn = Button.builder(
                    Component.literal(getTargetName(ModConfig.tapeMouseTarget, ModConfig.tapeMouseRussian)),
                    (b) -> {
                        ModConfig.tapeMouseTarget = (ModConfig.tapeMouseTarget + 1) % 3;
                        ConfigManager.save();
                        this.rebuildWidgets();
                    }
            ).bounds(centerX - 100, panelY + 130, 200, 20).build();
            this.addRenderableWidget(targetBtn);

            Checkbox requireTargetCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.tapeMouseRussian ? "Бить только при наведении" : "Only when aiming at target"), this.font)
                    .pos(centerX - 100, panelY + 158)
                    .selected(ModConfig.tapeMouseRequireTarget)
                    .onValueChange((c, v) -> { ModConfig.tapeMouseRequireTarget = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(requireTargetCheckbox);

            Checkbox requireFullAttackCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.tapeMouseRussian ? "Бить только при заряженной атаке" : "Only on full attack charge"), this.font)
                    .pos(centerX - 100, panelY + 181)
                    .selected(ModConfig.tapeMouseRequireFullAttack)
                    .onValueChange((c, v) -> { ModConfig.tapeMouseRequireFullAttack = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(requireFullAttackCheckbox);
        } else {
            Checkbox holdRightCheckbox = Checkbox.builder(
                            Component.literal(ModConfig.tapeMouseRussian ? "Зажать ПКМ" : "Hold RMB"), this.font)
                    .pos(centerX - 100, panelY + 160)
                    .selected(ModConfig.tapeMouseHoldRight)
                    .onValueChange((c, v) -> { ModConfig.tapeMouseHoldRight = v; ConfigManager.save(); })
                    .build();
            this.addRenderableWidget(holdRightCheckbox);
        }

        String tmKeyName = KeyBindings.tapeMouseKey != null
                ? KeyBindings.tapeMouseKey.getTranslatedKeyMessage().getString() : "R";

        Button tapeMouseKeyBindButton = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 2 ? "Нажмите клавишу..." : "Клавиша TapeMouse: " + tmKeyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 2; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 225, 200, 20).build();
        this.addRenderableWidget(tapeMouseKeyBindButton);

        AbstractSliderButton delaySlider = new AbstractSliderButton(
                centerX - 100, panelY + 265, 200, 20,
                Component.literal(getDelayText()),
                (ModConfig.tapeMouseDelay - 0.1f) / 4.9f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(getDelayText())); }
            @Override
            protected void applyValue() {
                ModConfig.tapeMouseDelay = 0.1f + (float) (this.value * 4.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(delaySlider);
    }

    private void initPage5(int centerX, int panelY) {
        Checkbox aspectCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.aspectRatioRussian ? "Включить растяг" : "Enable stretch"), this.font)
                .pos(centerX - 100, panelY + 70)
                .selected(ModConfig.aspectRatioEnabled)
                .onValueChange((c, v) -> { ModConfig.aspectRatioEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(aspectCheckbox);

        Button aspectTranslate = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.aspectRatioRussian = !ModConfig.aspectRatioRussian;
            ConfigManager.save();
            this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 70, 25, 20).build();
        this.addRenderableWidget(aspectTranslate);

        AbstractSliderButton ratioSlider = new AbstractSliderButton(
                centerX - 100, panelY + 110, 200, 20,
                Component.literal(getAspectRatioText()),
                (ModConfig.aspectRatio - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(getAspectRatioText())); }
            @Override
            protected void applyValue() {
                ModConfig.aspectRatio = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(ratioSlider);

        Button preset4x3 = Button.builder(Component.literal("4:3"), (b) -> { ModConfig.aspectRatio = 1.33f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 100, panelY + 150, 60, 20).build();
        this.addRenderableWidget(preset4x3);
        Button preset16x9 = Button.builder(Component.literal("16:9"), (b) -> { ModConfig.aspectRatio = 1.0f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 35, panelY + 150, 60, 20).build();
        this.addRenderableWidget(preset16x9);
        Button preset21x9 = Button.builder(Component.literal("21:9"), (b) -> { ModConfig.aspectRatio = 0.75f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 30, panelY + 150, 60, 20).build();
        this.addRenderableWidget(preset21x9);
        Button presetSquare = Button.builder(Component.literal("1:1"), (b) -> { ModConfig.aspectRatio = 1.78f; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX + 95, panelY + 150, 60, 20).build();
        this.addRenderableWidget(presetSquare);

        Button resetBtn = Button.builder(Component.literal(ModConfig.aspectRatioRussian ? "Сбросить" : "Reset"), (b) -> {
            ModConfig.aspectRatio = 1.0f; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 190, 200, 20).build();
        this.addRenderableWidget(resetBtn);
    }

    private void initPage6(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.customHitSoundsRussian ? "Включить кастомные звуки" : "Enable custom hit sounds"), this.font)
                .pos(centerX - 100, panelY + 60)
                .selected(ModConfig.customHitSoundsEnabled)
                .onValueChange((c, v) -> { ModConfig.customHitSoundsEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.customHitSoundsRussian = !ModConfig.customHitSoundsRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 60, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String chsKeyName = KeyBindings.customHitSoundsKey != null
                ? KeyBindings.customHitSoundsKey.getTranslatedKeyMessage().getString() : "J";
        Button keyBindBtn = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 4 ? "Нажмите клавишу..." : "Клавиша: " + chsKeyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 4; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 95, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        for (int i = 1; i <= 7; i++) {
            final int preset = i;
            Button soundBtn = Button.builder(Component.literal(String.valueOf(i)),
                            (b) -> {
                                ModConfig.customHitSoundPreset = preset;
                                ConfigManager.save();
                                this.rebuildWidgets();
                            })
                    .bounds(centerX - 100 + (i - 1) * 30, panelY + 130, 25, 20)
                    .build();
            this.addRenderableWidget(soundBtn);
        }

        AbstractSliderButton volumeSlider = new AbstractSliderButton(
                centerX - 100, panelY + 170, 200, 20,
                Component.literal(String.format("Громкость: %.1f", ModConfig.customHitSoundVolume)),
                (ModConfig.customHitSoundVolume - 0.1f) / 1.9f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(String.format("Громкость: %.1f", ModConfig.customHitSoundVolume))); }
            @Override
            protected void applyValue() {
                ModConfig.customHitSoundVolume = 0.1f + (float) (this.value * 1.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(volumeSlider);

        AbstractSliderButton pitchSlider = new AbstractSliderButton(
                centerX - 100, panelY + 210, 200, 20,
                Component.literal(String.format("Тон: %.1f", ModConfig.customHitSoundPitch)),
                (ModConfig.customHitSoundPitch - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() { this.setMessage(Component.literal(String.format("Тон: %.1f", ModConfig.customHitSoundPitch))); }
            @Override
            protected void applyValue() {
                ModConfig.customHitSoundPitch = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(pitchSlider);
    }

    private void initPage7(int centerX, int panelY) {
        Checkbox potionCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.potionEffectsRussian ? "Показывать эффекты" : "Show Effects"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.showPotionEffects)
                .onValueChange((c, v) -> { ModConfig.showPotionEffects = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(potionCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.potionEffectsRussian = !ModConfig.potionEffectsRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        Checkbox iconsCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.potionEffectsRussian ? "Показывать иконки" : "Show icons"), this.font)
                .pos(centerX - 100, panelY + 110)
                .selected(ModConfig.potionEffectsIcons)
                .onValueChange((c, v) -> { ModConfig.potionEffectsIcons = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(iconsCheckbox);

        makePosEditor(centerX, panelY, 150,
                () -> ModConfig.potionEffectsX, () -> ModConfig.potionEffectsY,
                (x, y) -> { ModConfig.potionEffectsX = x; ModConfig.potionEffectsY = y; },
                10, 170);
    }

    private void initPage8(int centerX, int panelY) {
        Checkbox equipCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.equipmentHudRussian ? "Показывать экипировку" : "Show equipment"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.showEquipmentHud)
                .onValueChange((c, v) -> { ModConfig.showEquipmentHud = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(equipCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.equipmentHudRussian = !ModConfig.equipmentHudRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        Checkbox durabilityCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.equipmentHudRussian ? "Показывать прочность" : "Show durability"), this.font)
                .pos(centerX - 100, panelY + 110)
                .selected(ModConfig.equipmentShowDurability)
                .onValueChange((c, v) -> { ModConfig.equipmentShowDurability = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(durabilityCheckbox);

        makePosEditor(centerX, panelY, 155,
                () -> ModConfig.equipmentHudX, () -> ModConfig.equipmentHudY,
                (x, y) -> { ModConfig.equipmentHudX = x; ModConfig.equipmentHudY = y; },
                4, -44);
    }

    private void initPage9(int centerX, int panelY) {
        Checkbox fireCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.lowFireShieldRussian ? "Низкий огонь" : "Low Fire"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.lowFireEnabled)
                .onValueChange((c, v) -> { ModConfig.lowFireEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(fireCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.lowFireShieldRussian = !ModConfig.lowFireShieldRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        AbstractSliderButton fireSlider = new AbstractSliderButton(
                centerX - 100, panelY + 115, 200, 20,
                Component.literal(String.format(ModConfig.lowFireShieldRussian ? "Смещение огня: %.2f" : "Fire offset: %.2f", ModConfig.lowFireOffset)),
                ModConfig.lowFireOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(ModConfig.lowFireShieldRussian ? "Смещение огня: %.2f" : "Fire offset: %.2f", ModConfig.lowFireOffset)));
            }
            @Override protected void applyValue() {
                ModConfig.lowFireOffset = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(fireSlider);

        Checkbox shieldCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.lowFireShieldRussian ? "Низкий щит" : "Low Shield"), this.font)
                .pos(centerX - 100, panelY + 160)
                .selected(ModConfig.lowShieldEnabled)
                .onValueChange((c, v) -> { ModConfig.lowShieldEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(shieldCheckbox);

        AbstractSliderButton shieldSlider = new AbstractSliderButton(
                centerX - 100, panelY + 195, 200, 20,
                Component.literal(String.format(ModConfig.lowFireShieldRussian ? "Смещение щита: %.2f" : "Shield offset: %.2f", ModConfig.lowShieldOffset)),
                ModConfig.lowShieldOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(ModConfig.lowFireShieldRussian ? "Смещение щита: %.2f" : "Shield offset: %.2f", ModConfig.lowShieldOffset)));
            }
            @Override protected void applyValue() {
                ModConfig.lowShieldOffset = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(shieldSlider);
    }

    private void initPage10(int centerX, int panelY) {
        Checkbox zoomCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.zoomRussian ? "Включить Zoom" : "Enable Zoom"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.zoomEnabled)
                .onValueChange((c, v) -> { ModConfig.zoomEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(zoomCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.zoomRussian = !ModConfig.zoomRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String keyName = KeyBindings.zoomKey != null
                ? KeyBindings.zoomKey.getTranslatedKeyMessage().getString() : "C";

        Button zoomKeyBindButton = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 1 ? "Нажмите клавишу..." : "Клавиша зума: " + keyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 1; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(zoomKeyBindButton);

        AbstractSliderButton factorSlider = new AbstractSliderButton(
                centerX - 100, panelY + 160, 200, 20,
                Component.literal(String.format(ModConfig.zoomRussian ? "Сила зума: %.1fx" : "Zoom factor: %.1fx", ModConfig.zoomFactor)),
                (ModConfig.zoomFactor - 1.5f) / 8.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(ModConfig.zoomRussian ? "Сила зума: %.1fx" : "Zoom factor: %.1fx", ModConfig.zoomFactor)));
            }
            @Override protected void applyValue() {
                ModConfig.zoomFactor = 1.5f + (float) (this.value * 8.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(factorSlider);

        AbstractSliderButton smoothSlider = new AbstractSliderButton(
                centerX - 100, panelY + 200, 200, 20,
                Component.literal(String.format(ModConfig.zoomRussian ? "Плавность: %.2f" : "Smoothness: %.2f", ModConfig.zoomSmoothness)),
                (ModConfig.zoomSmoothness - 0.05f) / 0.95f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(ModConfig.zoomRussian ? "Плавность: %.2f" : "Smoothness: %.2f", ModConfig.zoomSmoothness)));
            }
            @Override protected void applyValue() {
                ModConfig.zoomSmoothness = 0.05f + (float) (this.value * 0.95f);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(smoothSlider);
    }

    private void initPage11(int centerX, int panelY) {
        Checkbox swapCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.autoSwapRussian ? "Включить Автосвап" : "Enable AutoSwap"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.autoSwapEnabled)
                .onValueChange((c, v) -> { ModConfig.autoSwapEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(swapCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.autoSwapRussian = !ModConfig.autoSwapRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        int modeW = 95;
        Button mode0 = Button.builder(Component.literal("Шар ↔ Шар"),
                        (b) -> { ModConfig.autoSwapMode = 0; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 100, panelY + 120, modeW, 20).build();
        this.addRenderableWidget(mode0);
        Button mode1 = Button.builder(Component.literal("Тотем ↔ Тотем"),
                        (b) -> { ModConfig.autoSwapMode = 1; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 5, panelY + 120, modeW, 20).build();
        this.addRenderableWidget(mode1);
        Button mode2 = Button.builder(Component.literal("Шар ↔ Тотем"),
                        (b) -> { ModConfig.autoSwapMode = 2; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 100, panelY + 145, modeW, 20).build();
        this.addRenderableWidget(mode2);
        Button mode3 = Button.builder(Component.literal("Тотем ↔ Шар"),
                        (b) -> { ModConfig.autoSwapMode = 3; ConfigManager.save(); this.rebuildWidgets(); })
                .bounds(centerX - 5, panelY + 145, modeW, 20).build();
        this.addRenderableWidget(mode3);

        String swapKeyName = KeyBindings.autoSwapKey != null
                ? KeyBindings.autoSwapKey.getTranslatedKeyMessage().getString() : "H";

        Button swapKeyBindButton = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 3 ? "Нажмите клавишу..." : "Клавиша Автосвапа: " + swapKeyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 3; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 185, 200, 20).build();
        this.addRenderableWidget(swapKeyBindButton);

        AbstractSliderButton openDelaySlider = new AbstractSliderButton(
                centerX - 100, panelY + 220, 200, 20,
                Component.literal(String.format(ModConfig.autoSwapRussian ? "Задержка открытия: %d мс" : "Open delay: %d ms", ModConfig.autoSwapOpenDelay)),
                (ModConfig.autoSwapOpenDelay - 50) / 450.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(ModConfig.autoSwapRussian ? "Задержка открытия: %d мс" : "Open delay: %d ms", ModConfig.autoSwapOpenDelay)));
            }
            @Override protected void applyValue() {
                ModConfig.autoSwapOpenDelay = 50 + (int)(this.value * 450);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(openDelaySlider);

        AbstractSliderButton cooldownSlider = new AbstractSliderButton(
                centerX - 100, panelY + 250, 200, 20,
                Component.literal(String.format(ModConfig.autoSwapRussian ? "Cooldown: %d мс" : "Cooldown: %d ms", ModConfig.autoSwapCooldown)),
                (ModConfig.autoSwapCooldown - 100) / 1900.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(ModConfig.autoSwapRussian ? "Cooldown: %d мс" : "Cooldown: %d ms", ModConfig.autoSwapCooldown)));
            }
            @Override protected void applyValue() {
                ModConfig.autoSwapCooldown = 100 + (int)(this.value * 1900);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(cooldownSlider);
    }

    private void initPage12(int centerX, int panelY) {
        Checkbox fastExpCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.fastExpRussian ? "Включить FastExp" : "Enable FastExp"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.fastExpEnabled)
                .onValueChange((c, v) -> { ModConfig.fastExpEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(fastExpCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.fastExpRussian = !ModConfig.fastExpRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String feKeyName = KeyBindings.fastExpKey != null
                ? KeyBindings.fastExpKey.getTranslatedKeyMessage().getString() : "K";
        Button keyBindBtn = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 5 ? "Нажмите клавишу..." : "Клавиша: " + feKeyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 5; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);
    }

    private void initPage13(int centerX, int panelY) {
        Checkbox shiftTapCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.shiftTapRussian ? "Включить ShiftTap" : "Enable ShiftTap"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.shiftTapEnabled)
                .onValueChange((c, v) -> { ModConfig.shiftTapEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(shiftTapCheckbox);

        Button shiftTranslate = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.shiftTapRussian = !ModConfig.shiftTapRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(shiftTranslate);

        String stKeyName = KeyBindings.shiftTapKey != null
                ? KeyBindings.shiftTapKey.getTranslatedKeyMessage().getString() : "L";
        Button keyBindBtn = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 6 ? "Нажмите клавишу..." : "Клавиша: " + stKeyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 6; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);
    }

    private void initPage14(int centerX, int panelY) {
        Checkbox comboCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.comboRussian ? "Включить Combo Counter" : "Enable Combo Counter"), this.font)
                .pos(centerX - 100, panelY + 80)
                .selected(ModConfig.comboEnabled)
                .onValueChange((c, v) -> { ModConfig.comboEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(comboCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.comboRussian = !ModConfig.comboRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 80, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String cbKeyName = KeyBindings.comboKey != null
                ? KeyBindings.comboKey.getTranslatedKeyMessage().getString() : "M";
        Button keyBindBtn = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 7 ? "Нажмите клавишу..." : "Клавиша: " + cbKeyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 7; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 120, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        makePosEditor(centerX, panelY, 160,
                () -> ModConfig.comboX, () -> ModConfig.comboY,
                (x, y) -> { ModConfig.comboX = x; ModConfig.comboY = y; },
                10, 185);

        AbstractSliderButton resetSlider = new AbstractSliderButton(
                centerX - 100, panelY + 200, 200, 20,
                Component.literal(ModConfig.comboRussian ? ("Время сброса: " + ModConfig.comboResetTime + " сек") : ("Reset time: " + ModConfig.comboResetTime + " sec")),
                (ModConfig.comboResetTime - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.comboRussian ? ("Время сброса: " + ModConfig.comboResetTime + " сек") : ("Reset time: " + ModConfig.comboResetTime + " sec")));
            }
            @Override protected void applyValue() {
                ModConfig.comboResetTime = 1 + (int)(this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(resetSlider);

        String sizeText = ModConfig.comboRussian ? "Размер: " : "Size: ";
        String[] sizesRu = {"Малый", "Средний", "Крупный"};
        String[] sizesEn = {"Small", "Medium", "Large"};
        Button sizeBtn = Button.builder(
                Component.literal(sizeText + (ModConfig.comboRussian ? sizesRu[ModConfig.comboFontSize] : sizesEn[ModConfig.comboFontSize])),
                (b) -> {
                    ModConfig.comboFontSize = (ModConfig.comboFontSize + 1) % 3;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 235, 200, 20).build();
        this.addRenderableWidget(sizeBtn);

        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 275, 80, 18,
                Component.literal("#RRGGBB"));
        colorField.setMaxLength(7);
        colorField.setValue(String.format("#%06X", ModConfig.comboColor & 0xFFFFFF));
        this.addRenderableWidget(colorField);

        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = colorField.getValue().replace("#", "").trim();
            try {
                ModConfig.comboColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 275, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorYellow = Button.builder(Component.literal("Жёлт"), (b) -> { colorField.setValue("#FFFF00"); ModConfig.comboColor = 0xFFFFFF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 275, 45, 18).build();
        this.addRenderableWidget(colorYellow);
        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); ModConfig.comboColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 55, panelY + 275, 45, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorGreen = Button.builder(Component.literal("Зел"), (b) -> { colorField.setValue("#00FF00"); ModConfig.comboColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 105, panelY + 275, 45, 18).build();
        this.addRenderableWidget(colorGreen);
    }

    private void initPage15(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.effectWarningsRussian ? "Включить Effect Warnings" : "Enable Effect Warnings"), this.font)
                .pos(centerX - 100, panelY + 75)
                .selected(ModConfig.effectWarningsEnabled)
                .onValueChange((c, v) -> { ModConfig.effectWarningsEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.effectWarningsRussian = !ModConfig.effectWarningsRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 75, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        String ewKeyName = KeyBindings.effectWarningsKey != null
                ? KeyBindings.effectWarningsKey.getTranslatedKeyMessage().getString() : "N";
        Button keyBindBtn = Button.builder(
                        Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 8 ? "Нажмите клавишу..." : "Клавиша: " + ewKeyName),
                        (b) -> { ModConfig.isBindingKey = true; ModConfig.bindingTarget = 8; b.setMessage(Component.literal("Нажмите клавишу...")); })
                .bounds(centerX - 100, panelY + 110, 200, 20).build();
        this.addRenderableWidget(keyBindBtn);

        AbstractSliderButton thresholdSlider = new AbstractSliderButton(
                centerX - 100, panelY + 145, 200, 20,
                Component.literal(ModConfig.effectWarningsRussian ? ("Порог: " + ModConfig.effectWarningsThreshold + " сек") : ("Threshold: " + ModConfig.effectWarningsThreshold + " sec")),
                (ModConfig.effectWarningsThreshold - 3) / 12.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.effectWarningsRussian ? ("Порог: " + ModConfig.effectWarningsThreshold + " сек") : ("Threshold: " + ModConfig.effectWarningsThreshold + " sec")));
            }
            @Override protected void applyValue() {
                ModConfig.effectWarningsThreshold = 3 + (int)(this.value * 12);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(thresholdSlider);

        makePosEditor(centerX, panelY, 185,
                () -> ModConfig.effectWarningsX, () -> ModConfig.effectWarningsY,
                (x, y) -> { ModConfig.effectWarningsX = x; ModConfig.effectWarningsY = y; },
                300, 200);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 225, 200, 20,
                Component.literal(ModConfig.effectWarningsRussian ? ("Прозрачность: " + ModConfig.effectWarningsAlpha) : ("Alpha: " + ModConfig.effectWarningsAlpha)),
                ModConfig.effectWarningsAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.effectWarningsRussian ? ("Прозрачность: " + ModConfig.effectWarningsAlpha) : ("Alpha: " + ModConfig.effectWarningsAlpha)));
            }
            @Override protected void applyValue() {
                ModConfig.effectWarningsAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(alphaSlider);

        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 265, 80, 18,
                Component.literal("#RRGGBB"));
        colorField.setMaxLength(7);
        colorField.setValue(String.format("#%06X", ModConfig.effectWarningsColor & 0xFFFFFF));
        this.addRenderableWidget(colorField);

        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = colorField.getValue().replace("#", "").trim();
            try {
                ModConfig.effectWarningsColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 265, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); ModConfig.effectWarningsColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 265, 45, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorYellow = Button.builder(Component.literal("Жёлт"), (b) -> { colorField.setValue("#FFFF00"); ModConfig.effectWarningsColor = 0xFFFFFF00; ConfigManager.save(); })
                .bounds(centerX + 55, panelY + 265, 45, 18).build();
        this.addRenderableWidget(colorYellow);
        Button colorWhite = Button.builder(Component.literal("Бел"), (b) -> { colorField.setValue("#FFFFFF"); ModConfig.effectWarningsColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 105, panelY + 265, 45, 18).build();
        this.addRenderableWidget(colorWhite);

        Checkbox showNameCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.effectWarningsRussian ? "Показывать название" : "Show name"), this.font)
                .pos(centerX - 100, panelY + 300)
                .selected(ModConfig.effectWarningsShowName)
                .onValueChange((c, v) -> { ModConfig.effectWarningsShowName = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(showNameCheckbox);

        Checkbox showIconCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.effectWarningsRussian ? "Показывать иконку" : "Show icon"), this.font)
                .pos(centerX + 20, panelY + 300)
                .selected(ModConfig.effectWarningsShowIcon)
                .onValueChange((c, v) -> { ModConfig.effectWarningsShowIcon = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(showIconCheckbox);
    }

    private void initPage16(int centerX, int panelY) {
        Checkbox enableCheckbox = Checkbox.builder(
                        Component.literal(ModConfig.crosshairRussian ? "Включить кастомный прицел" : "Enable custom crosshair"), this.font)
                .pos(centerX - 100, panelY + 45)
                .selected(ModConfig.crosshairEnabled)
                .onValueChange((c, v) -> { ModConfig.crosshairEnabled = v; ConfigManager.save(); })
                .build();
        this.addRenderableWidget(enableCheckbox);

        Button translateBtn = Button.builder(Component.literal("RU"), (b) -> {
            ModConfig.crosshairRussian = !ModConfig.crosshairRussian; ConfigManager.save(); this.rebuildWidgets();
        }).bounds(centerX + 120, panelY + 45, 25, 20).build();
        this.addRenderableWidget(translateBtn);

        Button shapeBtn = Button.builder(
                Component.literal(getCrosshairShapeName(ModConfig.crosshairShape, ModConfig.crosshairRussian)),
                (b) -> {
                    ModConfig.crosshairShape = (ModConfig.crosshairShape + 1) % 5;
                    ConfigManager.save();
                    this.rebuildWidgets();
                }
        ).bounds(centerX - 100, panelY + 80, 200, 20).build();
        this.addRenderableWidget(shapeBtn);

        AbstractSliderButton sizeSlider = new AbstractSliderButton(
                centerX - 100, panelY + 115, 200, 20,
                Component.literal(ModConfig.crosshairRussian ? ("Размер: " + ModConfig.crosshairSize + " px") : ("Size: " + ModConfig.crosshairSize + " px")),
                (ModConfig.crosshairSize - 4) / 16.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.crosshairRussian ? ("Размер: " + ModConfig.crosshairSize + " px") : ("Size: " + ModConfig.crosshairSize + " px")));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairSize = 4 + (int)(this.value * 16);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(sizeSlider);

        AbstractSliderButton thicknessSlider = new AbstractSliderButton(
                centerX - 100, panelY + 145, 200, 20,
                Component.literal(ModConfig.crosshairRussian ? ("Толщина: " + ModConfig.crosshairThickness + " px") : ("Thickness: " + ModConfig.crosshairThickness + " px")),
                (ModConfig.crosshairThickness - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.crosshairRussian ? ("Толщина: " + ModConfig.crosshairThickness + " px") : ("Thickness: " + ModConfig.crosshairThickness + " px")));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairThickness = 1 + (int)(this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(thicknessSlider);

        AbstractSliderButton gapSlider = new AbstractSliderButton(
                centerX - 100, panelY + 175, 200, 20,
                Component.literal(ModConfig.crosshairRussian ? ("Зазор: " + ModConfig.crosshairGap + " px") : ("Gap: " + ModConfig.crosshairGap + " px")),
                ModConfig.crosshairGap / 10.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.crosshairRussian ? ("Зазор: " + ModConfig.crosshairGap + " px") : ("Gap: " + ModConfig.crosshairGap + " px")));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairGap = (int)(this.value * 10);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(gapSlider);

        AbstractSliderButton alphaSlider = new AbstractSliderButton(
                centerX - 100, panelY + 205, 200, 20,
                Component.literal(ModConfig.crosshairRussian ? ("Прозрачность: " + ModConfig.crosshairAlpha) : ("Alpha: " + ModConfig.crosshairAlpha)),
                ModConfig.crosshairAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(ModConfig.crosshairRussian ? ("Прозрачность: " + ModConfig.crosshairAlpha) : ("Alpha: " + ModConfig.crosshairAlpha)));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        };
        this.addRenderableWidget(alphaSlider);

        EditBox colorField = new EditBox(this.font, centerX - 130, panelY + 245, 80, 18,
                Component.literal("#RRGGBB"));
        colorField.setMaxLength(7);
        colorField.setValue(String.format("#%06X", ModConfig.crosshairColor & 0xFFFFFF));
        this.addRenderableWidget(colorField);
        Button applyColorBtn = Button.builder(Component.literal("ОК"), (btn) -> {
            String hex = colorField.getValue().replace("#", "").trim();
            try {
                ModConfig.crosshairColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
        }).bounds(centerX - 45, panelY + 245, 40, 18).build();
        this.addRenderableWidget(applyColorBtn);

        Button colorGreen = Button.builder(Component.literal("Зел"), (b) -> { colorField.setValue("#00FF00"); ModConfig.crosshairColor = 0xFF00FF00; ConfigManager.save(); })
                .bounds(centerX + 5, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorGreen);
        Button colorRed = Button.builder(Component.literal("Крас"), (b) -> { colorField.setValue("#FF0000"); ModConfig.crosshairColor = 0xFFFF0000; ConfigManager.save(); })
                .bounds(centerX + 50, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorRed);
        Button colorBlue = Button.builder(Component.literal("Син"), (b) -> { colorField.setValue("#0000FF"); ModConfig.crosshairColor = 0xFF0000FF; ConfigManager.save(); })
                .bounds(centerX + 95, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorBlue);
        Button colorWhite = Button.builder(Component.literal("Бел"), (b) -> { colorField.setValue("#FFFFFF"); ModConfig.crosshairColor = 0xFFFFFFFF; ConfigManager.save(); })
                .bounds(centerX + 140, panelY + 245, 40, 18).build();
        this.addRenderableWidget(colorWhite);
    }

    private void initThemesPage(int centerX, int panelY) {
        int[][] themes = {
                {0xFF00FF00, 0xFFFFFFFF, 0xFF00FF00},
                {0xFF808080, 0xFFDDDDDD, 0xFF808080},
                {0xFF00FFFF, 0xFF00FF00, 0xFF00FFFF},
                {0xFFFF69B4, 0xFFFFFFFF, 0xFFFF69B4},
                {0xFFFF0000, 0xFFFFFFFF, 0xFFFF0000},
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
                ModConfig.guiColor = themes[idx][0];
                ModConfig.guiTextColor = themes[idx][1];
                ModConfig.hudColor = themes[idx][2];
                ConfigManager.save();
                this.rebuildWidgets();
            }).bounds(x, y, cardW, cardH).build();
            this.addRenderableWidget(themeBtn);
        }

        Button resetBtn = Button.builder(Component.literal("Сбросить на Vanilla"), (b) -> {
            ModConfig.guiColor = 0xFF00FF00;
            ModConfig.guiTextColor = 0xFFFFFFFF;
            ModConfig.hudColor = 0xFF00FF00;
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

    private void initWaypointsPage(int centerX, int panelY) {
        int defaultX = 0;
        int defaultY = 64;
        int defaultZ = 0;
        if (Minecraft.getInstance().player != null) {
            defaultX = (int) Minecraft.getInstance().player.getX();
            defaultY = (int) Minecraft.getInstance().player.getY();
            defaultZ = (int) Minecraft.getInstance().player.getZ();
        }

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
                                    + ModConfig.waypointsMax + ")"), true);
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

        Button clearBtn = Button.builder(Component.literal("Очистить все метки"), (b) -> {
            MyCustomScreen.clearWaypoints();
            this.rebuildWidgets();
        }).bounds(centerX - 100, panelY + 105, 200, 20).build();
        this.addRenderableWidget(clearBtn);

        List<MyCustomScreen.Waypoint> waypoints = MyCustomScreen.getWaypoints();

        int listStartY = panelY + 155;
        int rowHeight = 22;

        for (int i = 0; i < waypoints.size(); i++) {
            final int index = i;
            final MyCustomScreen.Waypoint wp = waypoints.get(i);
            int y = listStartY + i * rowHeight;

            Button renameBtn = Button.builder(Component.literal("✎"), (b) -> {
                openRenameDialog(index, wp.name());
            }).bounds(centerX + 55, y, 20, 18).build();
            this.addRenderableWidget(renameBtn);

            Button delBtn = Button.builder(Component.literal("×"), (b) -> {
                MyCustomScreen.removeWaypoint(index);
                this.rebuildWidgets();
            }).bounds(centerX + 80, y, 20, 18).build();
            this.addRenderableWidget(delBtn);
        }

        if (renameDialogOpen) {
            EditBox nameField = new EditBox(this.font, centerX - 100, panelY + 320, 200, 18,
                    Component.literal("Имя"));
            nameField.setMaxLength(24);
            nameField.setValue(renameDialogOldName);
            this.addRenderableWidget(nameField);
            this.setFocused(nameField);

            Button okBtn = Button.builder(Component.literal("ОК"), (b) -> {
                String newName = nameField.getValue().trim();
                if (newName.isEmpty()) {
                    closeRenameDialog();
                    return;
                }
                List<MyCustomScreen.Waypoint> list = MyCustomScreen.getWaypoints();
                if (renameDialogIndex >= 0 && renameDialogIndex < list.size()) {
                    MyCustomScreen.Waypoint old = list.get(renameDialogIndex);
                    MyCustomScreen.renameWaypoint(old.name(), newName);
                }
                closeRenameDialog();
            }).bounds(centerX - 100, panelY + 342, 95, 18).build();
            this.addRenderableWidget(okBtn);

            Button cancelBtn = Button.builder(Component.literal("Отмена"), (b) -> {
                closeRenameDialog();
            }).bounds(centerX + 5, panelY + 342, 95, 18).build();
            this.addRenderableWidget(cancelBtn);
        }
    }

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
            if (ModConfig.guiColor == themes[i][0]
                    && ModConfig.guiTextColor == themes[i][1]
                    && ModConfig.hudColor == themes[i][2]) {
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
        return String.format("Задержка: %.1f сек", ModConfig.tapeMouseDelay);
    }

    private String getAspectRatioText() {
        return String.format("Соотношение: %.2f", ModConfig.aspectRatio);
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
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 2, ModConfig.guiColor);
        graphics.fill(panelX, panelY + panelHeight - 2, panelX + panelWidth, panelY + panelHeight, ModConfig.guiColor);
        graphics.fill(panelX, panelY, panelX + 2, panelY + panelHeight, ModConfig.guiColor);
        graphics.fill(panelX + panelWidth - 2, panelY, panelX + panelWidth, panelY + panelHeight, ModConfig.guiColor);

        graphics.drawString(this.font, "§lПоиск:",
                panelX - 180, panelY + 35, ModConfig.guiTextColor);

        drawSearchResults(graphics);

        int tabX = panelX - 180;
        int tabY = panelY + 185;
        int tabW = 160;
        int tabH = 22;
        int tabGap = 4;

        for (int i = 0; i < SECTION_COUNT; i++) {
            int ty = tabY + i * (tabH + tabGap);
            if (i == currentSection) {
                graphics.fill(tabX, ty, tabX + tabW, ty + tabH, (ModConfig.guiColor & 0x00FFFFFF) | 0x40000000);
                graphics.fill(tabX, ty, tabX + tabW, ty + 1, ModConfig.guiColor);
                graphics.fill(tabX, ty + tabH - 1, tabX + tabW, ty + tabH, ModConfig.guiColor);
                graphics.fill(tabX, ty, tabX + 1, ty + tabH, ModConfig.guiColor);
                graphics.fill(tabX + tabW - 1, ty, tabX + tabW, ty + tabH, ModConfig.guiColor);
            } else {
                graphics.fill(tabX, ty, tabX + tabW, ty + tabH, 0x60000000);
            }
        }

        super.render(graphics, mouseX, mouseY, delta);

        if (currentSection == 3 && currentPage == 5) {
            graphics.drawString(this.font, "§l▸ Координаты новой метки",
                    panelX + 20, panelY + 35, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 47, panelX + panelWidth - 20, panelY + 48, ModConfig.guiColor);

            graphics.drawString(this.font, "§l▸ Существующие метки",
                    panelX + 20, panelY + 130, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 142, panelX + panelWidth - 20, panelY + 143, ModConfig.guiColor);

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

            if (renameDialogOpen) {
                graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xC0000000);
                int dialogX = panelX + 40;
                int dialogY = panelY + 275;
                int dialogW = panelWidth - 80;
                int dialogH = 100;
                graphics.fill(dialogX, dialogY, dialogX + dialogW, dialogY + dialogH, 0xE0000000);
                graphics.fill(dialogX, dialogY, dialogX + dialogW, dialogY + 1, ModConfig.guiColor);
                graphics.fill(dialogX, dialogY + dialogH - 1, dialogX + dialogW, dialogY + dialogH, ModConfig.guiColor);
                graphics.fill(dialogX, dialogY, dialogX + 1, dialogY + dialogH, ModConfig.guiColor);
                graphics.fill(dialogX + dialogW - 1, dialogY, dialogX + dialogW, dialogY + dialogH, ModConfig.guiColor);

                graphics.drawCenteredString(this.font, "§lПереименовать метку",
                        panelX + panelWidth / 2, panelY + 285, ModConfig.guiColor);

                graphics.drawCenteredString(this.font,
                        "§7Старое имя: §e" + renameDialogOldName,
                        panelX + panelWidth / 2, panelY + 303, 0xFFFFFFFF);
            }
        }

        if (currentSection == 3 && currentPage == 6) {
            graphics.drawString(this.font, "§l▸ Настройки камеры",
                    panelX + 20, panelY + 40, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, ModConfig.guiColor);

            graphics.drawString(this.font, "§7No Hurt Cam §f— убирает тряску при получении урона",
                    panelX + 20, panelY + 175, 0xFFAAAAAA);
            graphics.drawString(this.font, "§7No Bobbing §f— убирает покачивание камеры при ходьбе",
                    panelX + 20, panelY + 189, 0xFFAAAAAA);
            graphics.drawString(this.font, "§7Обе фичи безопасны и разрешены на PvP-серверах",
                    panelX + 20, panelY + 203, 0xFF888888);
        }

        if (currentSection == 3 && currentPage == 7) {
            graphics.drawString(this.font, "§l▸ ItemPhysics",
                    panelX + 20, panelY + 40, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, ModConfig.guiColor);

            graphics.drawString(this.font, "§7Предметы на земле лежат плашмя,",
                    panelX + 20, panelY + 175, 0xFFAAAAAA);
            graphics.drawString(this.font, "§7а не крутятся и не парят.",
                    panelX + 20, panelY + 189, 0xFFAAAAAA);
            graphics.drawString(this.font, "§7Чисто визуально — сервер не видит.",
                    panelX + 20, panelY + 203, 0xFF888888);
        }

        if (currentSection == 1 && currentPage == 5) {
            int hintY = panelY + 230;
            graphics.drawString(this.font,
                    "§7Формат: §c[PvP] §f<атакующий> §7снёс тотем §f<жертва>",
                    panelX + 20, hintY, 0xFFFFFFFF);
            graphics.drawString(this.font,
                    "§7Радиус проверяется от §fтебя §7до §fжертвы",
                    panelX + 20, hintY + 14, 0xFFFFFFFF);
            graphics.drawString(this.font,
                    "§7Звук: §fBLOCK_NOTE_BLOCK_PLING §7(клиентский)",
                    panelX + 20, hintY + 28, 0xFFFFFFFF);
        }

        if (currentSection == 1 && currentPage == 6) {
            graphics.drawString(this.font, "§l▸ PvPSafe",
                    panelX + 20, panelY + 40, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, ModConfig.guiColor);

            graphics.drawString(this.font, "§7Блокирует выход и опасные команды во время боя:",
                    panelX + 20, panelY + 310, 0xFFAAAAAA);
            graphics.drawString(this.font, "§f/hub §7· §f/an<число> §7· §f/logout §7· §f/limbo §7· §f/suicide",
                    panelX + 20, panelY + 324, 0xFFFFFFFF);
            graphics.drawString(this.font, "§c⚠ Не работает со спец-предметами (явная пыль и т.п.)",
                    panelX + 20, panelY + 342, 0xFFFF5555);
        }

        if (currentSection == 1 && currentPage == 7) {
            graphics.drawString(this.font, "§l▸ PickUpLogger",
                    panelX + 20, panelY + 40, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, ModConfig.guiColor);

            if (ModConfig.pickupLogMode != 2) {
                graphics.drawString(this.font, "§7Логирует подобранные предметы в чат:",
                        panelX + 20, panelY + 195, 0xFFAAAAAA);
                graphics.drawString(this.font, "§a[PickUp] §f+1 §eАлмазный меч",
                        panelX + 20, panelY + 209, 0xFFFFFFFF);
            }
        }

        // ===== CHAT FILTER: заголовок + список =====
        if (currentSection == 4 && currentPage == 2) {
            graphics.drawString(this.font, "§l▸ ChatFilter",
                    panelX + 20, panelY + 40, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, ModConfig.guiColor);

            List<String> words = ChatFilterManager.getWords();
            if (words.isEmpty()) {
                graphics.drawString(this.font, "§7Список пуст. Добавь стоп-слово выше.",
                        panelX + 30, panelY + 140, 0xFFAAAAAA);
            } else {
                int listStartY = panelY + 140;
                int rowHeight = 20;
                int maxVisible = 9;

                for (int i = 0; i < Math.min(words.size(), maxVisible); i++) {
                    String word = words.get(i);
                    int y = listStartY + i * rowHeight;

                    graphics.drawString(this.font, "§e" + word,
                            panelX + 30, y + 5, 0xFFFFFFFF);
                }

                if (words.size() > maxVisible) {
                    graphics.drawString(this.font,
                            "§7... и ещё §e" + (words.size() - maxVisible),
                            panelX + 30, listStartY + maxVisible * rowHeight + 5, 0xFF888888);
                }
            }
        }

        // ===== AUTO RECONNECT: заголовок + подсказки =====
        if (currentSection == 4 && currentPage == 3) {
            graphics.drawString(this.font, "§l▸ AutoReconnect",
                    panelX + 20, panelY + 40, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 52, panelX + panelWidth - 20, panelY + 53, ModConfig.guiColor);

            graphics.drawString(this.font, "§7Автоматически переподключается к серверу",
                    panelX + 20, panelY + 195, 0xFFAAAAAA);
            graphics.drawString(this.font, "§7после кика или потери соединения.",
                    panelX + 20, panelY + 209, 0xFFAAAAAA);
            graphics.drawString(this.font, "§7Отмена: §fESC",
                    panelX + 20, panelY + 230, 0xFFFFFFFF);
            graphics.drawString(this.font, "§c⚠ Не работает в одиночной игре",
                    panelX + 20, panelY + 250, 0xFFFF5555);
        }
        // ===== PARTICLE BLOCKER: заголовок + подсказки =====
        if (currentSection == 3 && currentPage == 8) {
            graphics.drawString(this.font, "§l▸ Particle Blocker",
                    panelX + 20, panelY + 25, ModConfig.guiTextColor);
            graphics.fill(panelX + 20, panelY + 37, panelX + panelWidth - 20, panelY + 38, ModConfig.guiColor);

            graphics.drawString(this.font, "§7Отключает частицы по категориям:",
                    panelX + 20, panelY + 220, 0xFFAAAAAA);
            graphics.drawString(this.font, "§fогонь · дым · взрывы · зелья",
                    panelX + 20, panelY + 234, 0xFFFFFFFF);
            graphics.drawString(this.font, "§fвода · редстоун · портал · криты",
                    panelX + 20, panelY + 248, 0xFFFFFFFF);
            graphics.drawString(this.font, "§7Чисто визуально — сервер не видит.",
                    panelX + 20, panelY + 270, 0xFF888888);
        }

        if (searchOtherSectionMsg != null && !searchOtherSectionMsg.isEmpty()) {
            int msgW = this.font.width(searchOtherSectionMsg) + 16;
            int msgH = 18;
            int msgX = panelX + (panelWidth - msgW) / 2;
            int msgY = panelY + panelHeight - 55;

            graphics.fill(msgX, msgY, msgX + msgW, msgY + msgH, 0xE0000000);
            graphics.fill(msgX, msgY, msgX + msgW, msgY + 1, ModConfig.guiColor);
            graphics.fill(msgX, msgY + msgH - 1, msgX + msgW, msgY + msgH, ModConfig.guiColor);
            graphics.fill(msgX, msgY, msgX + 1, msgY + msgH, ModConfig.guiColor);
            graphics.fill(msgX + msgW - 1, msgY, msgX + msgW, msgY + msgH, ModConfig.guiColor);

            graphics.drawCenteredString(this.font, searchOtherSectionMsg,
                    msgX + msgW / 2, msgY + 5, 0xFFFFFFFF);
        }

        if (ModConfig.showPet) {
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

        String sectionName = getSectionName(currentSection, ModConfig.modLogoRussian);
        int pagesInSection = SECTION_PAGES[currentSection];
        String title = "Resistance DLC — " + sectionName + " · Стр. " + (currentPage + 1) + " / " + pagesInSection;
        graphics.drawCenteredString(this.font, "§l" + title,
                panelX + panelWidth / 2, panelY + 15, ModConfig.guiColor);

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
            String secName = getSectionName(matchSection, ModConfig.modLogoRussian);
            String text = match[1] + " §7· " + secName + " (стр. " + (matchPage + 1) + ")";
            graphics.drawString(this.font, text, resultX + 5, y + 3, 0xFFFFFFFF, false);
        }

        this.searchResults = matches;
        this.searchResultX = resultX;
        this.searchResultY = resultY;
        this.searchResultWidth = resultWidth;
        this.searchResultHeight = resultHeight;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (renameDialogOpen) {
            return super.mouseClicked(event, isDoubleClick);
        }

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
                        String secName = getSectionName(targetSection, ModConfig.modLogoRussian);
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