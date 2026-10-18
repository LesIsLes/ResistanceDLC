package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import com.resistancedlc.inventory.InventorySnapshot;
import com.resistancedlc.inventory.InventorySnapshotManager;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.resistancedlc.config.BossBarStyle;
import com.resistancedlc.config.AspectRatioPreset;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AccordionScreen — GUI мода с аккордеон-меню.
 *
 * v3.0.0-alpha.4 — полировка:
 *   - Главные чекбоксы "Включить X" удалены (включение через ПКМ по item).
 *   - Панели с единственным чекбоксом → contentHeight = 0 (не раскрываются).
 *   - Остальные панели сохранили подопции (слайдеры, кнопки, подчинённые чекбоксы).
 */
public class AccordionScreen extends Screen {

    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 460;
    private static final int COLUMN_COLLAPSED = 60;
    private static final int COLUMN_EXPANDED = 220;
    private static final int HEADER_HEIGHT = 30;

    private static final int OVERLAY_W = 460;
    private static final int OVERLAY_H = 380;

    // ===== ПЕРЕХОД ИЗ HUD MUSIC =====
    public static String pendingJumpToSection = null;
    public static String pendingJumpToItem = null;

    private int panelX;
    private int panelY;

    private int activeSectionIndex = 0;
    private float columnWidth = COLUMN_COLLAPSED;
    private boolean hoverColumn = false;
    private float lastColumnWidthForWidgets = COLUMN_COLLAPSED;

    private final Map<String, List<AbstractWidget>> panelWidgets = new HashMap<>();

    private int contentScroll = 0;
    private int maxContentScroll = 0;

    private int activeExtraHudSetting = -1;
    private int activeMainHudSetting = -1;
    private int activeEnchantSetting = -1;
    private int activeDeathRecapIndex = -1;
    private static final Map<String, Boolean> savedExpanded = new HashMap<>();
    private static int savedActiveSectionIndex = 0;

    // ===== INVENTORY MANAGER =====
    private static final int MAX_INVENTORY_SLOTS = 5;
    private int activeInventoryDetail = -1;

    private EditBox searchField;
    private final List<AccordionItem> filteredItems = new ArrayList<>();
    private static String savedSearchText = "";
    private boolean restoringSearch = false;

    private boolean globalSearchOpen = false;
    private EditBox globalSearchField;
    private final List<String[]> globalSearchResults = new ArrayList<>();
    private int globalSearchHovered = -1;
    private int globalSearchListX, globalSearchListY, globalSearchListW, globalSearchRowH;
    private final List<AbstractWidget> globalSearchWidgets = new ArrayList<>();

    private String highlightedItemId = null;
    private long highlightStartTime = 0;
    private static final long HIGHLIGHT_DURATION = 2000;

    private float globalSearchFadeProgress = 0.0f;
    private static final float GLOBAL_SEARCH_FADE_SPEED = 4.0f;
    private boolean globalSearchClosing = false;

    private float sectionFadeProgress = 1.0f;
    private static final float SECTION_FADE_SPEED = 3.0f;
    private float sectionSlideOffset = 0.0f;
    private static final float SECTION_SLIDE_DISTANCE = 15.0f;

    // ===================== МОДЕЛЬ =====================
    public static class AccordionItem {
        public final String id;
        public final String title;
        public final String description;
        public final Supplier<Boolean> statusGetter;
        public final Runnable toggler;
        public boolean expanded = false;
        public int contentHeight = 46;
        public float expandProgress = 0.0f;

        public AccordionItem(String id, String title, String description,
                             Supplier<Boolean> statusGetter,
                             Runnable toggler) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.statusGetter = statusGetter;
            this.toggler = toggler;
        }
    }

    public static class Section {
        public final String id;
        public final String name;
        public final String description;
        public final ItemStack icon;
        public final List<AccordionItem> items = new ArrayList<>();

        public Section(String id, String name, String description, ItemStack icon) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }
    }

    private final List<Section> sections = new ArrayList<>();

    public AccordionScreen() {
        super(Component.literal(LocalizationManager.get("gui.resistancedlc.title")));
    }

    // ===================== INIT =====================
    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        panelWidgets.clear();
        sections.clear();
        initSections();

        // Восстанавливаем активную секцию
        if (savedActiveSectionIndex >= 0 && savedActiveSectionIndex < sections.size()) {
            activeSectionIndex = savedActiveSectionIndex;
        }

        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                it.expandProgress = it.expanded ? 1.0f : 0.0f;
            }
        }

        Button closeBtn = Button.builder(
                        Component.literal("×"),
                        (b) -> this.onClose())
                .bounds(panelX + PANEL_WIDTH - 42, panelY + 6, 30, 18).build();
        this.addRenderableWidget(closeBtn);

        Button langBtn = Button.builder(
                        Component.literal(ModConfig.modLogoRussian ? "EN" : "RU"),
                        (b) -> {
                            ModConfig.modLogoRussian = !ModConfig.modLogoRussian;
                            ConfigManager.save();
                            LocalizationManager.reload();
                            Minecraft.getInstance().execute(this::rebuildWidgetsPreservingState);
                        })
                .bounds(panelX + PANEL_WIDTH - 78, panelY + 6, 30, 18).build();
        this.addRenderableWidget(langBtn);

        int themeBtnSize = 18;
        int themeBtnGap = 4;
        int themeBtnY = panelY + 6;
        int themeBtnRight = panelX + PANEL_WIDTH - 92;
        int themeBtnStartX = themeBtnRight - themeBtnSize
                - 4 * (themeBtnSize + themeBtnGap);

        for (int i = 0; i < 5; i++) {
            int bx = themeBtnStartX + i * (themeBtnSize + themeBtnGap);
            Button themeBtn = Button.builder(Component.literal(""), (b) -> {
                    })
                    .bounds(bx, themeBtnY, themeBtnSize, themeBtnSize)
                    .build();
            themeBtn.active = false;
            this.addRenderableWidget(themeBtn);
        }

        int searchX = panelX + PANEL_WIDTH - 12 - 145;
        int searchY = panelY + HEADER_HEIGHT + 10 + 6;
        this.searchField = new EditBox(this.font, searchX, searchY, 140, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.search.hint")));
        this.searchField.setMaxLength(30);

        this.searchField.setResponder(text -> {
            if (restoringSearch) return;
            contentScroll = 0;
            updateFilteredItems();
            Minecraft.getInstance().execute(this::rebuildAllPanelWidgets);
        });

        if (!savedSearchText.isEmpty()) {
            restoringSearch = true;
            this.searchField.setValue(savedSearchText);
            restoringSearch = false;
        }
        this.addRenderableWidget(this.searchField);

        restoreExpanded();
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                it.expandProgress = it.expanded ? 1.0f : 0.0f;
            }
        }
        updateFilteredItems();
        Minecraft.getInstance().execute(this::rebuildAllPanelWidgets);

        if (globalSearchOpen) {
            buildGlobalSearchWidgets();
        }

        if (pendingJumpToSection != null) {
            String targetSection = pendingJumpToSection;
            String targetItem = pendingJumpToItem;
            pendingJumpToSection = null;
            pendingJumpToItem = null;

            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).id.equals(targetSection)) {
                    activeSectionIndex = i;
                    sectionFadeProgress = 0.0f;
                    sectionSlideOffset = SECTION_SLIDE_DISTANCE;
                    contentScroll = 0;
                    break;
                }
            }

            if (targetItem != null) {
                Section active = sections.get(activeSectionIndex);
                AccordionItem toExpand = null;
                for (AccordionItem it : active.items) {
                    if (it.id.equals(targetItem)) {
                        toExpand = it;
                        break;
                    }
                }
                if (toExpand != null) {
                    closeAllExcept(toExpand);
                    toExpand.expanded = true;
                    toExpand.expandProgress = 1.0f;
                    updateFilteredItems();
                    Minecraft.getInstance().execute(this::rebuildAllPanelWidgets);
                    triggerHighlight(targetItem);
                }
            }
        }
    }

    // ===================== СОХРАНЕНИЕ =====================
    public void saveExpanded() {
        savedExpanded.clear();
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                savedExpanded.put(it.id, it.expanded);
            }
        }
        if (searchField != null) {
            savedSearchText = searchField.getValue();
        }
        savedActiveSectionIndex = activeSectionIndex;
    }
    /** Публичный доступ для Mixin: пересобрать виджеты после смены бинда. */
    public void refreshAfterKeybind() {
        saveExpanded();
        this.rebuildWidgets();
    }
    private void restoreExpanded() {
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                Boolean saved = savedExpanded.get(it.id);
                if (saved != null) it.expanded = saved;
            }
        }
    }

    private void rebuildWidgetsPreservingState() {
        saveExpanded();
        this.rebuildWidgets();
    }

    // ===================== АНИМАЦИЯ =====================
    private void tickAnimations(float delta) {
        float safeDelta = Math.min(delta, 0.05f);
        if (safeDelta < 0.001f) safeDelta = 0.016f;
        float speed = 4.0f * safeDelta;

        for (Section s : sections) {
            for (AccordionItem item : s.items) {
                float target = item.expanded ? 1.0f : 0.0f;
                if (Math.abs(item.expandProgress - target) < 0.001f) {
                    item.expandProgress = target;
                    continue;
                }
                if (item.expandProgress < target) {
                    item.expandProgress = Math.min(target, item.expandProgress + speed);
                } else {
                    item.expandProgress = Math.max(target, item.expandProgress - speed);
                }
            }
        }

        if (globalSearchOpen) {
            if (globalSearchFadeProgress < 1.0f) {
                globalSearchFadeProgress = Math.min(1.0f,
                        globalSearchFadeProgress + safeDelta * GLOBAL_SEARCH_FADE_SPEED);
            }
        } else if (globalSearchClosing) {
            globalSearchFadeProgress -= safeDelta * GLOBAL_SEARCH_FADE_SPEED;
            if (globalSearchFadeProgress <= 0.0f) {
                globalSearchFadeProgress = 0.0f;
                globalSearchClosing = false;
                globalSearchResults.clear();
                globalSearchHovered = -1;
                clearGlobalSearchWidgets();
                if (searchField != null) {
                    searchField.visible = true;
                    searchField.active = true;
                }
            }
        }

        if (sectionFadeProgress < 1.0f) {
            sectionFadeProgress = Math.min(1.0f,
                    sectionFadeProgress + safeDelta * SECTION_FADE_SPEED);
            sectionSlideOffset = (1.0f - sectionFadeProgress) * SECTION_SLIDE_DISTANCE;
        } else {
            sectionSlideOffset = 0.0f;
        }
    }

    private void syncWidgetPositionsToColumnWidth() {
        if (Math.abs(lastColumnWidthForWidgets - columnWidth) < 0.5f) return;
        if (globalSearchOpen) return;

        if (panelWidgets.isEmpty()) {
            lastColumnWidthForWidgets = columnWidth;
            return;
        }

        int deltaX = (int) (columnWidth - lastColumnWidthForWidgets);
        if (deltaX == 0) {
            lastColumnWidthForWidgets = columnWidth;
            return;
        }

        for (List<AbstractWidget> widgets : panelWidgets.values()) {
            for (AbstractWidget w : widgets) {
                w.setX(w.getX() + deltaX);
            }
        }
        lastColumnWidthForWidgets = columnWidth;
    }

    private float easeInOutQuad(float t) {
        if (t < 0.5f) return 2.0f * t * t;
        float u = 1.0f - t;
        return 1.0f - 2.0f * u * u;
    }

    private int getAnimatedHeight(AccordionItem item) {
        float eased = easeInOutQuad(item.expandProgress);
        return (int) (item.contentHeight * eased);
    }

    private void updateContentMetrics() {
        int itemHeight = 26;
        int gap = 4;
        int totalHeight = 0;
        for (AccordionItem item : filteredItems) {
            totalHeight += itemHeight + gap;
            if (item.expanded) {
                totalHeight += item.contentHeight + gap;
            }
        }
        int visibleHeight = getListBottom() - getListTop();
        maxContentScroll = Math.max(0, totalHeight - visibleHeight);
        if (contentScroll > maxContentScroll) contentScroll = maxContentScroll;
        if (contentScroll < 0) contentScroll = 0;
    }

    // ===================== ПОИСК =====================
    private boolean matchesSearch(AccordionItem item) {
        if (searchField == null) return true;
        String query = searchField.getValue().toLowerCase().trim();
        if (query.isEmpty()) return true;
        return item.title.toLowerCase().contains(query)
                || item.description.toLowerCase().contains(query);
    }

    private void updateFilteredItems() {
        filteredItems.clear();
        if (sections.isEmpty()) return;
        Section active = sections.get(activeSectionIndex);
        for (AccordionItem item : active.items) {
            if (matchesSearch(item)) {
                filteredItems.add(item);
            }
        }
    }

    // ===================== ПОДСВЕТКА =====================
    private void triggerHighlight(String itemId) {
        this.highlightedItemId = itemId;
        this.highlightStartTime = System.currentTimeMillis();
    }

    private boolean isHighlightActive(AccordionItem item) {
        if (highlightedItemId == null) return false;
        if (!highlightedItemId.equals(item.id)) return false;
        long elapsed = System.currentTimeMillis() - highlightStartTime;
        return elapsed < HIGHLIGHT_DURATION;
    }

    private float getHighlightProgress(AccordionItem item) {
        if (!isHighlightActive(item)) return 0f;
        long elapsed = System.currentTimeMillis() - highlightStartTime;
        float t = (float) elapsed / HIGHLIGHT_DURATION;
        return Math.max(0f, Math.min(1f, t));
    }

    // ===================== ГЛОБАЛЬНЫЙ ПОИСК =====================
    private void openGlobalSearch() {
        globalSearchOpen = true;
        globalSearchClosing = false;
        globalSearchFadeProgress = 0.0f;
        globalSearchResults.clear();
        globalSearchHovered = -1;

        for (List<AbstractWidget> widgets : panelWidgets.values()) {
            for (AbstractWidget w : widgets) {
                w.visible = false;
                w.active = false;
            }
        }
        if (searchField != null) {
            searchField.visible = false;
            searchField.active = false;
        }

        buildGlobalSearchWidgets();
    }

    private void closeGlobalSearch() {
        globalSearchClosing = true;
        globalSearchOpen = false;
        globalSearchHovered = -1;

        for (AbstractWidget w : globalSearchWidgets) {
            w.visible = false;
            w.active = false;
        }
    }

    private void buildGlobalSearchWidgets() {
        clearGlobalSearchWidgets();

        int overlayX = panelX + (PANEL_WIDTH - OVERLAY_W) / 2;
        int overlayY = panelY + (PANEL_HEIGHT - OVERLAY_H) / 2;

        EditBox field = new EditBox(this.font, overlayX + 15, overlayY + 40,
                OVERLAY_W - 60, 22, Component.literal(
                LocalizationManager.get("gui.resistancedlc.search.global.hint")));
        field.setMaxLength(40);
        field.setResponder(this::performGlobalSearch);
        this.globalSearchField = field;
        this.addRenderableWidget(field);
        this.setFocused(field);
        globalSearchWidgets.add(field);

        Button close = Button.builder(Component.literal("×"),
                        (b) -> closeGlobalSearch())
                .bounds(overlayX + OVERLAY_W - 40, overlayY + 40, 25, 22).build();
        this.addRenderableWidget(close);
        globalSearchWidgets.add(close);
    }

    private void clearGlobalSearchWidgets() {
        for (AbstractWidget w : globalSearchWidgets) {
            this.removeWidget(w);
        }
        globalSearchWidgets.clear();
        this.globalSearchField = null;
    }

    private void performGlobalSearch(String query) {
        globalSearchResults.clear();
        if (query == null || query.trim().isEmpty()) return;

        String q = query.toLowerCase().trim();
        for (Section section : sections) {
            for (AccordionItem item : section.items) {
                if (item.title.toLowerCase().contains(q)
                        || item.description.toLowerCase().contains(q)) {
                    globalSearchResults.add(new String[]{
                            section.id, item.id, item.title, item.description
                    });
                    if (globalSearchResults.size() >= 12) return;
                }
            }
        }
    }

    private void closeAllExcept(AccordionItem keep) {
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it == keep) continue;
                if (it.expanded) {
                    it.expanded = false;
                    clearPanelWidgets(it);
                }
            }
        }
    }

    private void jumpToFunction(String sectionId, String itemId) {
        int sectionIdx = -1;
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).id.equals(sectionId)) {
                sectionIdx = i;
                break;
            }
        }
        if (sectionIdx < 0) return;

        globalSearchOpen = false;
        globalSearchClosing = false;
        globalSearchFadeProgress = 0.0f;
        globalSearchResults.clear();
        globalSearchHovered = -1;
        clearGlobalSearchWidgets();

        if (searchField != null) {
            searchField.visible = true;
            searchField.active = true;
        }

        activeSectionIndex = sectionIdx;
        sectionFadeProgress = 0.0f;
        sectionSlideOffset = SECTION_SLIDE_DISTANCE;

        if (searchField != null) {
            restoringSearch = true;
            searchField.setValue("");
            restoringSearch = false;
        }

        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                it.expanded = false;
                it.expandProgress = 0.0f;
            }
        }
        clearAllPanelWidgets();

        Section active = sections.get(activeSectionIndex);
        AccordionItem target = null;
        for (AccordionItem it : active.items) {
            if (it.id.equals(itemId)) {
                target = it;
                break;
            }
        }
        if (target == null) return;
        target.expanded = true;
        target.expandProgress = 1.0f;

        triggerHighlight(itemId);

        updateFilteredItems();

        int targetOffset = 0;
        int itemH = 26, gap = 4;
        for (AccordionItem it : active.items) {
            if (it == target) break;
            targetOffset += itemH + gap;
            if (it.expanded) targetOffset += it.contentHeight + gap;
        }

        int totalH = 0;
        for (AccordionItem it : active.items) {
            totalH += itemH + gap;
            if (it.expanded) totalH += it.contentHeight + gap;
        }
        int visibleH = getListBottom() - getListTop();
        int maxScroll = Math.max(0, totalH - visibleH);

        contentScroll = Math.min(targetOffset, maxScroll);
        if (contentScroll < 0) contentScroll = 0;

        Minecraft.getInstance().execute(this::rebuildAllPanelWidgets);
    }

    // ===================== РАЗДЕЛЫ =====================
    private void initSections() {
        sections.clear();

        // HUD
        Section hud = new Section("hud",
                LocalizationManager.get("gui.resistancedlc.section.hud"),
                LocalizationManager.get("gui.resistancedlc.section.hud.desc"),
                new ItemStack(Items.COMPASS));

        // ===== HUD EDITOR =====
        AccordionItem hudEditorItem = new AccordionItem(
                "hud_editor",
                LocalizationManager.get("gui.resistancedlc.item.hud_editor.title"),
                LocalizationManager.get("gui.resistancedlc.item.hud_editor.desc"),
                () -> true,
                () -> {
                    Minecraft.getInstance().gui.setScreen(
                            new com.resistancedlc.hud.HudEditScreen());
                }
        );
        hudEditorItem.contentHeight = 0;
        hud.items.add(hudEditorItem);

        AccordionItem nhcItem = new AccordionItem(
                "no_hurt_cam",
                LocalizationManager.get("gui.resistancedlc.item.no_hurt_cam.title"),
                LocalizationManager.get("gui.resistancedlc.item.no_hurt_cam.desc"),
                () -> ModConfig.noHurtCamEnabled,
                () -> {
                    ModConfig.noHurtCamEnabled = !ModConfig.noHurtCamEnabled;
                    ConfigManager.save();
                }
        );
        nhcItem.contentHeight = 0;
        hud.items.add(nhcItem);

        AccordionItem nbItem = new AccordionItem(
                "no_bobbing",
                LocalizationManager.get("gui.resistancedlc.item.no_bobbing.title"),
                LocalizationManager.get("gui.resistancedlc.item.no_bobbing.desc"),
                () -> ModConfig.noBobbingEnabled,
                () -> {
                    ModConfig.noBobbingEnabled = !ModConfig.noBobbingEnabled;
                    ConfigManager.save();
                }
        );
        nbItem.contentHeight = 0;
        hud.items.add(nbItem);

        // ===== Mod Logo & Name =====
        AccordionItem logoItem = new AccordionItem(
                "mod_logo",
                LocalizationManager.get("gui.resistancedlc.item.mod_logo.title"),
                LocalizationManager.get("gui.resistancedlc.item.mod_logo.desc"),
                () -> ModConfig.showModLogo,
                () -> {
                    ModConfig.showModLogo = !ModConfig.showModLogo;
                    ConfigManager.save();
                }
        );
        logoItem.contentHeight = 46;
        hud.items.add(logoItem);

        AccordionItem cdItem = new AccordionItem(
                "cooldowns",
                LocalizationManager.get("gui.resistancedlc.item.cooldowns.title"),
                LocalizationManager.get("gui.resistancedlc.item.cooldowns.desc"),
                () -> ModConfig.cooldownsEnabled,
                () -> {
                    ModConfig.cooldownsEnabled = !ModConfig.cooldownsEnabled;
                    ConfigManager.save();
                }
        );
        cdItem.contentHeight = 270;
        hud.items.add(cdItem);

        AccordionItem comboItem = new AccordionItem(
                "combo",
                LocalizationManager.get("gui.resistancedlc.item.combo.title"),
                LocalizationManager.get("gui.resistancedlc.item.combo.desc"),
                () -> ModConfig.comboEnabled,
                () -> {
                    ModConfig.comboEnabled = !ModConfig.comboEnabled;
                    ConfigManager.save();
                }
        );
        comboItem.contentHeight = 130;
        hud.items.add(comboItem);

        AccordionItem peItem = new AccordionItem(
                "potion_effects",
                LocalizationManager.get("gui.resistancedlc.item.potion_effects.title"),
                LocalizationManager.get("gui.resistancedlc.item.potion_effects.desc"),
                () -> ModConfig.showPotionEffects,
                () -> {
                    ModConfig.showPotionEffects = !ModConfig.showPotionEffects;
                    ConfigManager.save();
                }
        );
        peItem.contentHeight = 74;
        hud.items.add(peItem);

        AccordionItem eqItem = new AccordionItem(
                "equipment_hud",
                LocalizationManager.get("gui.resistancedlc.item.equipment_hud.title"),
                LocalizationManager.get("gui.resistancedlc.item.equipment_hud.desc"),
                () -> ModConfig.showEquipmentHud,
                () -> {
                    ModConfig.showEquipmentHud = !ModConfig.showEquipmentHud;
                    ConfigManager.save();
                }
        );
        eqItem.contentHeight = 74;
        hud.items.add(eqItem);

        AccordionItem ewItem = new AccordionItem(
                "effect_warnings",
                LocalizationManager.get("gui.resistancedlc.item.effect_warnings.title"),
                LocalizationManager.get("gui.resistancedlc.item.effect_warnings.desc"),
                () -> ModConfig.effectWarningsEnabled,
                () -> {
                    ModConfig.effectWarningsEnabled = !ModConfig.effectWarningsEnabled;
                    ConfigManager.save();
                }
        );
        ewItem.contentHeight = 130;
        hud.items.add(ewItem);

        AccordionItem ehItem = new AccordionItem(
                "extra_hud",
                LocalizationManager.get("gui.resistancedlc.item.extra_hud.title"),
                LocalizationManager.get("gui.resistancedlc.item.extra_hud.desc"),
                () -> ModConfig.showFps || ModConfig.showPing || ModConfig.showTps
                        || ModConfig.showBps || ModConfig.showDirection || ModConfig.showHitCounter,
                () -> {
                    boolean any = ModConfig.showFps || ModConfig.showPing || ModConfig.showTps
                            || ModConfig.showBps || ModConfig.showDirection || ModConfig.showHitCounter;
                    ModConfig.showFps = !any;
                    ModConfig.showPing = !any;
                    ModConfig.showTps = !any;
                    ModConfig.showBps = !any;
                    ModConfig.showDirection = !any;
                    ModConfig.showHitCounter = !any;
                    ConfigManager.save();
                }
        );
        ehItem.contentHeight = 130;
        hud.items.add(ehItem);

        AccordionItem pingItem = new AccordionItem(
                "ping_indicator",
                LocalizationManager.get("gui.resistancedlc.item.ping_indicator.title"),
                LocalizationManager.get("gui.resistancedlc.item.ping_indicator.desc"),
                () -> ModConfig.pingIndicatorEnabled,
                () -> {
                    ModConfig.pingIndicatorEnabled = !ModConfig.pingIndicatorEnabled;
                    ConfigManager.save();
                }
        );
        pingItem.contentHeight = 46;
        hud.items.add(pingItem);

        AccordionItem lhaItem = new AccordionItem(
                "low_hp_alert",
                LocalizationManager.get("gui.resistancedlc.item.low_hp_alert.title"),
                LocalizationManager.get("gui.resistancedlc.item.low_hp_alert.desc"),
                () -> ModConfig.lowHpAlertEnabled,
                () -> {
                    ModConfig.lowHpAlertEnabled = !ModConfig.lowHpAlertEnabled;
                    ConfigManager.save();
                }
        );
        lhaItem.contentHeight = 192;
        hud.items.add(lhaItem);

        AccordionItem aaItem = new AccordionItem(
                "armor_alert",
                LocalizationManager.get("gui.resistancedlc.item.armor_alert.title"),
                LocalizationManager.get("gui.resistancedlc.item.armor_alert.desc"),
                () -> ModConfig.armorAlertEnabled,
                () -> {
                    ModConfig.armorAlertEnabled = !ModConfig.armorAlertEnabled;
                    ConfigManager.save();
                }
        );
        aaItem.contentHeight = 192;
        hud.items.add(aaItem);
        // ===== ОСНОВНЫЕ НАСТРОЙКИ HUD =====
        AccordionItem mainHudItem = new AccordionItem(
                "main_hud_settings",
                LocalizationManager.get("gui.resistancedlc.item.main_hud_settings.title"),
                LocalizationManager.get("gui.resistancedlc.item.main_hud_settings.desc"),
                () -> ModConfig.showCoords || ModConfig.showBiome || ModConfig.showTime,
                () -> {
                    boolean any = ModConfig.showCoords || ModConfig.showBiome || ModConfig.showTime;
                    ModConfig.showCoords = !any;
                    ModConfig.showBiome = !any;
                    ModConfig.showTime = !any;
                    ConfigManager.save();
                }
        );
        mainHudItem.contentHeight = 130;
        hud.items.add(mainHudItem);
        sections.add(hud);

        // PVP
        Section pvp = new Section("pvp",
                LocalizationManager.get("gui.resistancedlc.section.pvp"),
                LocalizationManager.get("gui.resistancedlc.section.pvp.desc"),
                new ItemStack(Items.DIAMOND_SWORD));

        AccordionItem chsItem = new AccordionItem(
                "custom_hit_sounds",
                LocalizationManager.get("gui.resistancedlc.item.custom_hit_sounds.title"),
                LocalizationManager.get("gui.resistancedlc.item.custom_hit_sounds.desc"),
                () -> ModConfig.customHitSoundsEnabled,
                () -> {
                    ModConfig.customHitSoundsEnabled = !ModConfig.customHitSoundsEnabled;
                    ConfigManager.save();
                }
        );
        chsItem.contentHeight = 130;
        pvp.items.add(chsItem);

        AccordionItem totemItem = new AccordionItem(
                "totem_log",
                LocalizationManager.get("gui.resistancedlc.item.totem_log.title"),
                LocalizationManager.get("gui.resistancedlc.item.totem_log.desc"),
                () -> ModConfig.totemLogEnabled,
                () -> {
                    ModConfig.totemLogEnabled = !ModConfig.totemLogEnabled;
                    ConfigManager.save();
                }
        );
        totemItem.contentHeight = 102;
        pvp.items.add(totemItem);

        AccordionItem asItem = new AccordionItem(
                "auto_swap",
                LocalizationManager.get("gui.resistancedlc.item.auto_swap.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_swap.desc"),
                () -> ModConfig.autoSwapEnabled,
                () -> {
                    ModConfig.autoSwapEnabled = !ModConfig.autoSwapEnabled;
                    ConfigManager.save();
                }
        );
        asItem.contentHeight = 158;
        pvp.items.add(asItem);

        AccordionItem feItem = new AccordionItem(
                "fast_exp",
                LocalizationManager.get("gui.resistancedlc.item.fast_exp.title"),
                LocalizationManager.get("gui.resistancedlc.item.fast_exp.desc"),
                () -> ModConfig.fastExpEnabled,
                () -> {
                    ModConfig.fastExpEnabled = !ModConfig.fastExpEnabled;
                    ConfigManager.save();
                }
        );
        feItem.contentHeight = 0;
        pvp.items.add(feItem);

        AccordionItem stItem = new AccordionItem(
                "shift_tap",
                LocalizationManager.get("gui.resistancedlc.item.shift_tap.title"),
                LocalizationManager.get("gui.resistancedlc.item.shift_tap.desc"),
                () -> ModConfig.shiftTapEnabled,
                () -> {
                    ModConfig.shiftTapEnabled = !ModConfig.shiftTapEnabled;
                    ConfigManager.save();
                }
        );
        stItem.contentHeight = 0;
        pvp.items.add(stItem);

        AccordionItem aspItem = new AccordionItem(
                "auto_sprint",
                LocalizationManager.get("gui.resistancedlc.item.auto_sprint.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_sprint.desc"),
                () -> ModConfig.autoSprintEnabled,
                () -> {
                    ModConfig.autoSprintEnabled = !ModConfig.autoSprintEnabled;
                    ConfigManager.save();
                }
        );
        aspItem.contentHeight = 0;
        pvp.items.add(aspItem);

        AccordionItem psItem = new AccordionItem(
                "pvp_safe",
                LocalizationManager.get("gui.resistancedlc.item.pvp_safe.title"),
                LocalizationManager.get("gui.resistancedlc.item.pvp_safe.desc"),
                () -> ModConfig.pvpSafeEnabled,
                () -> {
                    ModConfig.pvpSafeEnabled = !ModConfig.pvpSafeEnabled;
                    ConfigManager.save();
                }
        );
        psItem.contentHeight = 158;
        pvp.items.add(psItem);

        AccordionItem plItem = new AccordionItem(
                "pickup_logger",
                LocalizationManager.get("gui.resistancedlc.item.pickup_logger.title"),
                LocalizationManager.get("gui.resistancedlc.item.pickup_logger.desc"),
                () -> ModConfig.pickupLogEnabled,
                () -> {
                    ModConfig.pickupLogEnabled = !ModConfig.pickupLogEnabled;
                    ConfigManager.save();
                }
        );
        plItem.contentHeight = 158;
        pvp.items.add(plItem);

        AccordionItem aggItem = new AccordionItem(
                "auto_gg",
                LocalizationManager.get("gui.resistancedlc.item.auto_gg.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_gg.desc"),
                () -> ModConfig.autoGgEnabled,
                () -> {
                    ModConfig.autoGgEnabled = !ModConfig.autoGgEnabled;
                    ConfigManager.save();
                }
        );
        aggItem.contentHeight = 130;
        pvp.items.add(aggItem);

        AccordionItem eggItem = new AccordionItem(
                "killaura_egg",
                LocalizationManager.get("gui.resistancedlc.item.killaura_egg.title"),
                LocalizationManager.get("gui.resistancedlc.item.killaura_egg.desc"),
                () -> ModConfig.killAuraEggEnabled,
                () -> {
                    ModConfig.killAuraEggEnabled = !ModConfig.killAuraEggEnabled;
                    ConfigManager.save();
                }
        );
        eggItem.contentHeight = 74;
        pvp.items.add(eggItem);

        AccordionItem atpItem = new AccordionItem(
                "auto_tp_accept",
                LocalizationManager.get("gui.resistancedlc.item.auto_tp_accept.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_tp_accept.desc"),
                () -> ModConfig.autoTpAcceptEnabled,
                () -> {
                    ModConfig.autoTpAcceptEnabled = !ModConfig.autoTpAcceptEnabled;
                    ConfigManager.save();
                }
        );
        atpItem.contentHeight = 74;
        pvp.items.add(atpItem);

        AccordionItem stItem2 = new AccordionItem(
                "stats_tracker",
                LocalizationManager.get("gui.resistancedlc.item.stats_tracker.title"),
                LocalizationManager.get("gui.resistancedlc.item.stats_tracker.desc"),
                () -> ModConfig.statsTrackerEnabled,
                () -> {
                    ModConfig.statsTrackerEnabled = !ModConfig.statsTrackerEnabled;
                    ConfigManager.save();
                }
        );
        stItem2.contentHeight = 160;
        pvp.items.add(stItem2);

        AccordionItem ksItem = new AccordionItem(
                "kill_streak",
                LocalizationManager.get("gui.resistancedlc.item.kill_streak.title"),
                LocalizationManager.get("gui.resistancedlc.item.kill_streak.desc"),
                () -> ModConfig.killStreakEnabled,
                () -> {
                    ModConfig.killStreakEnabled = !ModConfig.killStreakEnabled;
                    ConfigManager.save();
                }
        );
        ksItem.contentHeight = 186;
        pvp.items.add(ksItem);

        AccordionItem targetEspItem = new AccordionItem(
                "target_esp",
                LocalizationManager.get("gui.resistancedlc.item.target_esp.title"),
                LocalizationManager.get("gui.resistancedlc.item.target_esp.desc"),
                () -> ModConfig.targetEspEnabled,
                () -> {
                    ModConfig.targetEspEnabled = !ModConfig.targetEspEnabled;
                    ConfigManager.save();
                }
        );
        targetEspItem.contentHeight = 252;
        pvp.items.add(targetEspItem);
        // ===== TARGET HUD =====
        AccordionItem targetHudItem = new AccordionItem(
                "target_hud",
                LocalizationManager.get("gui.resistancedlc.item.target_hud.title"),
                LocalizationManager.get("gui.resistancedlc.item.target_hud.desc"),
                () -> ModConfig.targetHudEnabled,
                () -> {
                    ModConfig.targetHudEnabled = !ModConfig.targetHudEnabled;
                    ConfigManager.save();
                }
        );
        targetHudItem.contentHeight = 210;
        pvp.items.add(targetHudItem);
        sections.add(pvp);

        // PVE
        Section pve = new Section("pve",
                LocalizationManager.get("gui.resistancedlc.section.pve"),
                LocalizationManager.get("gui.resistancedlc.section.pve.desc"),
                new ItemStack(Items.CARROT));

        AccordionItem tmItem = new AccordionItem(
                "tape_mouse",
                LocalizationManager.get("gui.resistancedlc.item.tape_mouse.title"),
                LocalizationManager.get("gui.resistancedlc.item.tape_mouse.desc"),
                () -> ModConfig.tapeMouseEnabled,
                () -> {
                    ModConfig.tapeMouseEnabled = !ModConfig.tapeMouseEnabled;
                    ConfigManager.save();
                }
        );
        tmItem.contentHeight = 186;
        pve.items.add(tmItem);

        AccordionItem isItem = new AccordionItem(
                "item_scroller",
                LocalizationManager.get("gui.resistancedlc.item.item_scroller.title"),
                LocalizationManager.get("gui.resistancedlc.item.item_scroller.desc"),
                () -> ModConfig.itemScrollerEnabled,
                () -> {
                    ModConfig.itemScrollerEnabled = !ModConfig.itemScrollerEnabled;
                    ConfigManager.save();
                }
        );
        isItem.contentHeight = 102;
        pve.items.add(isItem);

        AccordionItem atItem = new AccordionItem(
                "auto_tool",
                LocalizationManager.get("gui.resistancedlc.item.auto_tool.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_tool.desc"),
                () -> ModConfig.autoToolEnabled,
                () -> {
                    ModConfig.autoToolEnabled = !ModConfig.autoToolEnabled;
                    ConfigManager.save();
                }
        );
        atItem.contentHeight = 46;
        pve.items.add(atItem);

        AccordionItem arsItem = new AccordionItem(
                "auto_respawn",
                LocalizationManager.get("gui.resistancedlc.item.auto_respawn.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_respawn.desc"),
                () -> ModConfig.autoRespawnEnabled,
                () -> {
                    ModConfig.autoRespawnEnabled = !ModConfig.autoRespawnEnabled;
                    ConfigManager.save();
                }
        );
        arsItem.contentHeight = 46;
        pve.items.add(arsItem);

        sections.add(pve);

        // VISUAL
        Section visual = new Section("visual",
                LocalizationManager.get("gui.resistancedlc.section.visual"),
                LocalizationManager.get("gui.resistancedlc.section.visual.desc"),
                new ItemStack(Items.ENDER_EYE));

        AccordionItem zoomItem = new AccordionItem(
                "zoom",
                LocalizationManager.get("gui.resistancedlc.item.zoom.title"),
                LocalizationManager.get("gui.resistancedlc.item.zoom.desc"),
                () -> ModConfig.zoomEnabled,
                () -> {
                    ModConfig.zoomEnabled = !ModConfig.zoomEnabled;
                    ConfigManager.save();
                }
        );
        zoomItem.contentHeight = 102;
        visual.items.add(zoomItem);

        AccordionItem chItem = new AccordionItem(
                "crosshair",
                LocalizationManager.get("gui.resistancedlc.item.crosshair.title"),
                LocalizationManager.get("gui.resistancedlc.item.crosshair.desc"),
                () -> ModConfig.crosshairEnabled,
                () -> {
                    ModConfig.crosshairEnabled = !ModConfig.crosshairEnabled;
                    ConfigManager.save();
                }
        );
        chItem.contentHeight = 214;
        visual.items.add(chItem);
        AccordionItem atmItem = new AccordionItem(
                "atmosphere",
                LocalizationManager.get("gui.resistancedlc.item.atmosphere.title"),
                LocalizationManager.get("gui.resistancedlc.item.atmosphere.desc"),
                () -> ModConfig.atmosphereEnabled,
                () -> {
                    ModConfig.atmosphereEnabled = !ModConfig.atmosphereEnabled;
                    ConfigManager.save();
                }
        );

        atmItem.contentHeight = 270;
        visual.items.add(atmItem);

        AccordionItem hbItem = new AccordionItem(
                "custom_hitbox",
                LocalizationManager.get("gui.resistancedlc.item.custom_hitbox.title"),
                LocalizationManager.get("gui.resistancedlc.item.custom_hitbox.desc"),
                () -> ModConfig.customHitboxEnabled,
                () -> {
                    ModConfig.customHitboxEnabled = !ModConfig.customHitboxEnabled;
                    ConfigManager.save();
                }
        );
        hbItem.contentHeight = 46;
        visual.items.add(hbItem);

        AccordionItem ipItem = new AccordionItem(
                "item_physics",
                LocalizationManager.get("gui.resistancedlc.item.item_physics.title"),
                LocalizationManager.get("gui.resistancedlc.item.item_physics.desc"),
                () -> ModConfig.itemPhysicsEnabled,
                () -> {
                    ModConfig.itemPhysicsEnabled = !ModConfig.itemPhysicsEnabled;
                    ConfigManager.save();
                }
        );
        ipItem.contentHeight = 0;
        visual.items.add(ipItem);

        AccordionItem lfsItem = new AccordionItem(
                "low_fire_shield",
                LocalizationManager.get("gui.resistancedlc.item.low_fire_shield.title"),
                LocalizationManager.get("gui.resistancedlc.item.low_fire_shield.desc"),
                () -> ModConfig.lowFireEnabled || ModConfig.lowShieldEnabled,
                () -> {
                    boolean any = ModConfig.lowFireEnabled || ModConfig.lowShieldEnabled;
                    ModConfig.lowFireEnabled = !any;
                    ModConfig.lowShieldEnabled = !any;
                    ConfigManager.save();
                }
        );
        lfsItem.contentHeight = 74;
        visual.items.add(lfsItem);

        AccordionItem pbItem = new AccordionItem(
                "particle_blocker",
                LocalizationManager.get("gui.resistancedlc.item.particle_blocker.title"),
                LocalizationManager.get("gui.resistancedlc.item.particle_blocker.desc"),
                () -> ModConfig.particleBlockerEnabled,
                () -> {
                    ModConfig.particleBlockerEnabled = !ModConfig.particleBlockerEnabled;
                    ConfigManager.save();
                }
        );
        pbItem.contentHeight = 102;
        visual.items.add(pbItem);
        AccordionItem fogItem = new AccordionItem(
                "custom_fog",
                LocalizationManager.get("gui.resistancedlc.item.custom_fog.title"),
                LocalizationManager.get("gui.resistancedlc.item.custom_fog.desc"),
                () -> ModConfig.customFogEnabled,
                () -> {
                    ModConfig.customFogEnabled = !ModConfig.customFogEnabled;
                    ConfigManager.save();
                }
        );
        fogItem.contentHeight = 250;
        visual.items.add(fogItem);
        AccordionItem wpItem = new AccordionItem(
                "waypoints",
                LocalizationManager.get("gui.resistancedlc.item.waypoints.title"),
                LocalizationManager.get("gui.resistancedlc.item.waypoints.desc"),
                () -> ModConfig.waypointsEnabled,
                () -> {
                    ModConfig.waypointsEnabled = !ModConfig.waypointsEnabled;
                    ConfigManager.save();
                }
        );
        wpItem.contentHeight = calcWaypointsHeight() - 28;
        visual.items.add(wpItem);

        AccordionItem srItem = new AccordionItem(
                "strike_range",
                LocalizationManager.get("gui.resistancedlc.item.strike_range.title"),
                LocalizationManager.get("gui.resistancedlc.item.strike_range.desc"),
                () -> ModConfig.strikeRangeEnabled,
                () -> {
                    ModConfig.strikeRangeEnabled = !ModConfig.strikeRangeEnabled;
                    ConfigManager.save();
                }
        );
        AccordionItem clearLiqItem = new AccordionItem(
                "clear_liquids",
                LocalizationManager.get("gui.resistancedlc.item.clear_liquids.title"),
                LocalizationManager.get("gui.resistancedlc.item.clear_liquids.desc"),
                () -> ModConfig.clearLiquidsEnabled,
                () -> {
                    ModConfig.clearLiquidsEnabled = !ModConfig.clearLiquidsEnabled;
                    ConfigManager.save();
                }
        );
        clearLiqItem.contentHeight = 74;
        visual.items.add(clearLiqItem);
        AccordionItem headwearItem = new AccordionItem(
                "headwear",
                LocalizationManager.get("gui.resistancedlc.item.headwear.title"),
                LocalizationManager.get("gui.resistancedlc.item.headwear.desc"),
                () -> ModConfig.headwearEnabled,
                () -> {
                    ModConfig.headwearEnabled = !ModConfig.headwearEnabled;
                    ConfigManager.save();
                }
        );
        headwearItem.contentHeight = 260;
        visual.items.add(headwearItem);
        AccordionItem chmItem = new AccordionItem(
                "crosshair_heatmap",
                LocalizationManager.get("gui.resistancedlc.item.crosshair_heatmap.title"),
                LocalizationManager.get("gui.resistancedlc.item.crosshair_heatmap.desc"),
                () -> ModConfig.crosshairHeatmapEnabled,
                () -> {
                    ModConfig.crosshairHeatmapEnabled = !ModConfig.crosshairHeatmapEnabled;
                    ConfigManager.save();
                }
        );
        chmItem.contentHeight = 392;
        visual.items.add(chmItem);

        AccordionItem gammaItem = new AccordionItem(
                "gamma_util",
                LocalizationManager.get("gui.resistancedlc.item.gamma_util.title"),
                LocalizationManager.get("gui.resistancedlc.item.gamma_util.desc"),
                () -> ModConfig.gammaUtilEnabled,
                () -> {
                    ModConfig.gammaUtilEnabled = !ModConfig.gammaUtilEnabled;
                    ConfigManager.save();
                }
        );
        gammaItem.contentHeight = 46;
        visual.items.add(gammaItem);
        srItem.contentHeight = 186;
        visual.items.add(srItem);

        AccordionItem predItem = new AccordionItem(
                "predictions",
                LocalizationManager.get("gui.resistancedlc.item.predictions.title"),
                LocalizationManager.get("gui.resistancedlc.item.predictions.desc"),
                () -> ModConfig.predictionsEnabled,
                () -> {
                    ModConfig.predictionsEnabled = !ModConfig.predictionsEnabled;
                    ConfigManager.save();
                }
        );
        predItem.contentHeight = 290;
        visual.items.add(predItem);

        AccordionItem ehItem2 = new AccordionItem(
                "enchant_highlight",
                LocalizationManager.get("gui.resistancedlc.item.enchant_highlight.title"),
                LocalizationManager.get("gui.resistancedlc.item.enchant_highlight.desc"),
                () -> ModConfig.enchantHighlightEnabled,
                () -> {
                    ModConfig.enchantHighlightEnabled = !ModConfig.enchantHighlightEnabled;
                    ConfigManager.save();
                }
        );
        ehItem2.contentHeight = calcEnchantHighlightHeight();
        visual.items.add(ehItem2);

        AccordionItem fovItem = new AccordionItem(
                "fov",
                LocalizationManager.get("gui.resistancedlc.item.fov.title"),
                LocalizationManager.get("gui.resistancedlc.item.fov.desc"),
                () -> ModConfig.fovEnabled,
                () -> {
                    ModConfig.fovEnabled = !ModConfig.fovEnabled;
                    ConfigManager.save();
                }
        );
        fovItem.contentHeight = 74;
        visual.items.add(fovItem);

        AccordionItem arItem = new AccordionItem(
                "aspect_ratio",
                LocalizationManager.get("gui.resistancedlc.item.aspect_ratio.title"),
                LocalizationManager.get("gui.resistancedlc.item.aspect_ratio.desc"),
                () -> ModConfig.aspectRatioEnabled,
                () -> {
                    ModConfig.aspectRatioEnabled = !ModConfig.aspectRatioEnabled;
                    ConfigManager.save();
                }
        );
        arItem.contentHeight = 130;
        visual.items.add(arItem);

        AccordionItem freelookItem = new AccordionItem(
                "freelook",
                LocalizationManager.get("gui.resistancedlc.item.freelook.title"),
                LocalizationManager.get("gui.resistancedlc.item.freelook.desc"),
                () -> ModConfig.freelookEnabled,
                () -> {
                    ModConfig.freelookEnabled = !ModConfig.freelookEnabled;
                    ConfigManager.save();
                }
        );
        freelookItem.contentHeight = 122;
        visual.items.add(freelookItem);

        AccordionItem tcItem = new AccordionItem(
                "time_change",
                LocalizationManager.get("gui.resistancedlc.item.time_change.title"),
                LocalizationManager.get("gui.resistancedlc.item.time_change.desc"),
                () -> ModConfig.timeChangeEnabled,
                () -> {
                    ModConfig.timeChangeEnabled = !ModConfig.timeChangeEnabled;
                    ConfigManager.save();
                }
        );
        tcItem.contentHeight = 112;
        visual.items.add(tcItem);

        AccordionItem wcItem = new AccordionItem(
                "weather_change",
                LocalizationManager.get("gui.resistancedlc.item.weather_change.title"),
                LocalizationManager.get("gui.resistancedlc.item.weather_change.desc"),
                () -> ModConfig.weatherChangeEnabled,
                () -> {
                    ModConfig.weatherChangeEnabled = !ModConfig.weatherChangeEnabled;
                    ConfigManager.save();
                }
        );
        wcItem.contentHeight = 90;
        visual.items.add(wcItem);

        AccordionItem bbbItem = new AccordionItem(
                "better_bossbar",
                LocalizationManager.get("gui.resistancedlc.item.better_bossbar.title"),
                LocalizationManager.get("gui.resistancedlc.item.better_bossbar.desc"),
                () -> ModConfig.betterBossBarEnabled,
                () -> {
                    ModConfig.betterBossBarEnabled = !ModConfig.betterBossBarEnabled;
                    ConfigManager.save();
                }
        );
        bbbItem.contentHeight = 180;
        visual.items.add(bbbItem);

        AccordionItem jcItem = new AccordionItem(
                "jump_circles",
                LocalizationManager.get("gui.resistancedlc.item.jump_circles.title"),
                LocalizationManager.get("gui.resistancedlc.item.jump_circles.desc"),
                () -> ModConfig.jumpCirclesEnabled,
                () -> {
                    ModConfig.jumpCirclesEnabled = !ModConfig.jumpCirclesEnabled;
                    ConfigManager.save();
                }
        );
        jcItem.contentHeight = 220;
        visual.items.add(jcItem);
// ===== CLOUD CONTROL =====
        AccordionItem cloudItem = new AccordionItem(
                "cloud_control",
                LocalizationManager.get("gui.resistancedlc.item.cloud_control.title"),
                LocalizationManager.get("gui.resistancedlc.item.cloud_control.desc"),
                () -> ModConfig.cloudControlEnabled,
                () -> {
                    ModConfig.cloudControlEnabled = !ModConfig.cloudControlEnabled;
                    ConfigManager.save();
                }
        );
        cloudItem.contentHeight = 186;
        visual.items.add(cloudItem);

// ===== TRAILS =====
        AccordionItem trailsItem = new AccordionItem(
                "trails",
                LocalizationManager.get("gui.resistancedlc.item.trails.title"),
                LocalizationManager.get("gui.resistancedlc.item.trails.desc"),
                () -> ModConfig.trailsEnabled,
                () -> {
                    ModConfig.trailsEnabled = !ModConfig.trailsEnabled;
                    ConfigManager.save();
                }
        );
        trailsItem.contentHeight = 248;
        visual.items.add(trailsItem);

// ===== HIT PARTICLES =====
        AccordionItem hitPartItem = new AccordionItem(
                "hit_particles",
                LocalizationManager.get("gui.resistancedlc.item.hit_particles.title"),
                LocalizationManager.get("gui.resistancedlc.item.hit_particles.desc"),
                () -> ModConfig.hitParticlesEnabled,
                () -> {
                    ModConfig.hitParticlesEnabled = !ModConfig.hitParticlesEnabled;
                    ConfigManager.save();
                }
        );
        hitPartItem.contentHeight = 220;
        visual.items.add(hitPartItem);
        sections.add(visual);

        // MISC
        Section misc = new Section("misc",
                LocalizationManager.get("gui.resistancedlc.section.misc"),
                LocalizationManager.get("gui.resistancedlc.section.misc.desc"),
                new ItemStack(Items.REDSTONE));

        AccordionItem cfItem = new AccordionItem(
                "chat_filter",
                LocalizationManager.get("gui.resistancedlc.item.chat_filter.title"),
                LocalizationManager.get("gui.resistancedlc.item.chat_filter.desc"),
                () -> ModConfig.chatFilterEnabled,
                () -> {
                    ModConfig.chatFilterEnabled = !ModConfig.chatFilterEnabled;
                    ConfigManager.save();
                }
        );
        cfItem.contentHeight = calcChatFilterHeight();
        misc.items.add(cfItem);

        AccordionItem scItem = new AccordionItem(
                "smart_chat",
                LocalizationManager.get("gui.resistancedlc.item.smart_chat.title"),
                LocalizationManager.get("gui.resistancedlc.item.smart_chat.desc"),
                () -> ModConfig.smartChatEnabled,
                () -> {
                    ModConfig.smartChatEnabled = !ModConfig.smartChatEnabled;
                    ConfigManager.save();
                }
        );
        scItem.contentHeight = 92;
        misc.items.add(scItem);

        AccordionItem arcItem = new AccordionItem(
                "auto_reconnect",
                LocalizationManager.get("gui.resistancedlc.item.auto_reconnect.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_reconnect.desc"),
                () -> ModConfig.autoReconnectEnabled,
                () -> {
                    ModConfig.autoReconnectEnabled = !ModConfig.autoReconnectEnabled;
                    ConfigManager.save();
                }
        );
        arcItem.contentHeight = 74;
        misc.items.add(arcItem);

        AccordionItem dcItem = new AccordionItem(
                "death_coords",
                LocalizationManager.get("gui.resistancedlc.item.death_coords.title"),
                LocalizationManager.get("gui.resistancedlc.item.death_coords.desc"),
                () -> ModConfig.deathCoordsEnabled,
                () -> {
                    ModConfig.deathCoordsEnabled = !ModConfig.deathCoordsEnabled;
                    ConfigManager.save();
                }
        );
        dcItem.contentHeight = 102;
        misc.items.add(dcItem);

        AccordionItem themeItem = new AccordionItem(
                "gui_theme",
                LocalizationManager.get("gui.resistancedlc.item.gui_theme.title"),
                LocalizationManager.get("gui.resistancedlc.item.gui_theme.desc"),
                () -> true,
                () -> {
                }
        );
        themeItem.contentHeight = 220;
        misc.items.add(themeItem);

        AccordionItem cmmItem = new AccordionItem(
                "custom_main_menu",
                LocalizationManager.get("gui.resistancedlc.item.custom_main_menu.title"),
                LocalizationManager.get("gui.resistancedlc.item.custom_main_menu.desc"),
                () -> ModConfig.customMainMenuEnabled,
                () -> {
                    ModConfig.customMainMenuEnabled = !ModConfig.customMainMenuEnabled;
                    ConfigManager.save();
                }
        );
        cmmItem.contentHeight = 74;
        misc.items.add(cmmItem);

        AccordionItem macroItem = new AccordionItem(
                "macros",
                LocalizationManager.get("gui.resistancedlc.item.macros.title"),
                LocalizationManager.get("gui.resistancedlc.item.macros.desc"),
                () -> ModConfig.macrosEnabled,
                () -> {
                    ModConfig.macrosEnabled = !ModConfig.macrosEnabled;
                    ConfigManager.save();
                }
        );
        macroItem.contentHeight = 200;
        misc.items.add(macroItem);

        AccordionItem flItem = new AccordionItem(
                "friend_list",
                LocalizationManager.get("gui.resistancedlc.item.friend_list.title"),
                LocalizationManager.get("gui.resistancedlc.item.friend_list.desc"),
                () -> ModConfig.friendListEnabled,
                () -> {
                    ModConfig.friendListEnabled = !ModConfig.friendListEnabled;
                    ConfigManager.save();
                }
        );
        flItem.contentHeight = calcFriendListHeight();
        misc.items.add(flItem);

        AccordionItem drItem = new AccordionItem(
                "death_recap",
                LocalizationManager.get("gui.resistancedlc.item.death_recap.title"),
                LocalizationManager.get("gui.resistancedlc.item.death_recap.desc"),
                () -> true,
                () -> {
                }
        );
        drItem.contentHeight = calcDeathRecapHeight();
        misc.items.add(drItem);

        AccordionItem cfgItem = new AccordionItem(
                "config_manager",
                LocalizationManager.get("gui.resistancedlc.item.config_manager.title"),
                LocalizationManager.get("gui.resistancedlc.item.config_manager.desc"),
                () -> true,
                () -> {
                }
        );
        cfgItem.contentHeight = calcConfigManagerHeight();
        misc.items.add(cfgItem);
        AccordionItem guiKeyItem = new AccordionItem(
                "gui_key",
                LocalizationManager.get("gui.resistancedlc.item.gui_key.title"),
                LocalizationManager.get("gui.resistancedlc.item.gui_key.desc"),
                () -> true,
                () -> {
                }
        );
        guiKeyItem.contentHeight = 46;
        misc.items.add(guiKeyItem);
        AccordionItem voiceItem = new AccordionItem(
                "voice_commands",
                LocalizationManager.get("gui.resistancedlc.item.voice_commands.title"),
                LocalizationManager.get("gui.resistancedlc.item.voice_commands.desc"),
                () -> ModConfig.voiceCommandsEnabled,
                () -> {
                    ModConfig.voiceCommandsEnabled = !ModConfig.voiceCommandsEnabled;
                    ConfigManager.save();
                }
        );
        voiceItem.contentHeight = 292;
        misc.items.add(voiceItem);

        AccordionItem invItem = new AccordionItem(
                "inventory_manager",
                LocalizationManager.get("gui.resistancedlc.item.inventory_manager.title"),
                LocalizationManager.get("gui.resistancedlc.item.inventory_manager.desc"),
                () -> true,
                () -> {
                }
        );
        invItem.contentHeight = calcInventoryManagerHeight();
        misc.items.add(invItem);

        sections.add(misc);

        // MUSIC
        Section music = new Section("music",
                LocalizationManager.get("gui.resistancedlc.section.music"),
                LocalizationManager.get("gui.resistancedlc.section.music.desc"),
                new ItemStack(Items.MUSIC_DISC_CAT));

        AccordionItem mpItem = new AccordionItem(
                "music_player",
                LocalizationManager.get("gui.resistancedlc.item.music_player.title"),
                LocalizationManager.get("gui.resistancedlc.item.music_player.desc"),
                () -> ModConfig.musicPlayerEnabled,
                () -> {
                    ModConfig.musicPlayerEnabled = !ModConfig.musicPlayerEnabled;
                    if (!ModConfig.musicPlayerEnabled) {
                        MusicPlayerManager.stop();
                    }
                    ConfigManager.save();
                }
        );
        mpItem.contentHeight = calcMusicPlayerHeight() - 28;
        music.items.add(mpItem);

        AccordionItem lyricsItem = new AccordionItem(
                "lyrics",
                LocalizationManager.get("gui.resistancedlc.item.lyrics.title"),
                LocalizationManager.get("gui.resistancedlc.item.lyrics.desc"),
                () -> ModConfig.lyricsEnabled,
                () -> {
                    ModConfig.lyricsEnabled = !ModConfig.lyricsEnabled;
                    ConfigManager.save();
                }
        );
        lyricsItem.contentHeight = 500;
        music.items.add(lyricsItem);

        sections.add(music);
    }

    // ===================== ГРАНИЦЫ =====================
    private int getContentLeft() {
        return panelX + (int) columnWidth + 10;
    }

    private int getContentTop() {
        return panelY + HEADER_HEIGHT + 10;
    }

    private int getContentRight() {
        return panelX + PANEL_WIDTH - 12;
    }

    private int getContentBottom() {
        return panelY + PANEL_HEIGHT - 12;
    }

    private int getListTop() {
        return getContentTop() + 42;
    }

    private int getListBottom() {
        return getContentBottom() - 4;
    }

    private int[] getPanelBoundsFixed(AccordionItem target) {
        int contentLeft = getContentLeft();
        int contentTop = getContentTop();
        int contentRight = getContentRight();

        int itemY = contentTop + 42 - contentScroll;
        int itemHeight = 26;
        int gap = 4;

        updateFilteredItems();

        for (AccordionItem item : filteredItems) {
            if (item == target) {
                if (!item.expanded) return null;
                int panelTop = itemY + itemHeight + gap;
                int panelLeft = contentLeft + 15;
                int panelRight = contentRight - 15;
                return new int[]{panelLeft, panelTop, panelRight};
            }
            itemY += itemHeight + gap;
            if (item.expanded) {
                itemY += item.contentHeight + gap;
            }
        }
        return null;
    }

    // ===================== ВИДЖЕТЫ =====================
    private void clearAllPanelWidgets() {
        for (List<AbstractWidget> widgets : panelWidgets.values()) {
            for (AbstractWidget w : widgets) {
                this.removeWidget(w);
            }
        }
        panelWidgets.clear();
        lastColumnWidthForWidgets = columnWidth;
    }

    private void clearPanelWidgets(AccordionItem item) {
        List<AbstractWidget> widgets = panelWidgets.remove(item.id);
        if (widgets != null) {
            for (AbstractWidget w : widgets) {
                this.removeWidget(w);
            }
        }
    }

    private void rebuildAllPanelWidgets() {
        clearAllPanelWidgets();
        if (globalSearchOpen) return;
        updateFilteredItems();

        List<AccordionItem> snapshot = new ArrayList<>(filteredItems);

        // ПРОХОД 1: измеряем — строим виджеты, чтобы авто-высота обновила contentHeight
        for (AccordionItem item : snapshot) {
            if (item.expanded) {
                buildPanelWidgets(item);
            }
        }
        updateContentMetrics();

        // ПРОХОД 2: строим заново с правильными Y (contentHeight теперь верный)
        clearAllPanelWidgets();
        for (AccordionItem item : snapshot) {
            if (item.expanded) {
                buildPanelWidgets(item);
            }
        }
        updateContentMetrics();
        updateWidgetsVisibility();
    }

    private void updateWidgetsVisibility() {
        if (globalSearchOpen) return;
        int listTop = getListTop();
        int listBottom = getListBottom();

        for (Map.Entry<String, List<AbstractWidget>> entry : panelWidgets.entrySet()) {
            String itemId = entry.getKey();
            AccordionItem owner = null;
            for (Section s : sections) {
                for (AccordionItem it : s.items) {
                    if (it.id.equals(itemId)) {
                        owner = it;
                        break;
                    }
                }
                if (owner != null) break;
            }

            boolean panelVisible = owner == null || owner.expandProgress > 0.6f;

            for (AbstractWidget w : entry.getValue()) {
                int wTop = w.getY();
                int wBottom = w.getY() + w.getHeight();
                boolean inView = wBottom > listTop && wTop < listBottom;
                boolean visible = panelVisible && inView;
                w.visible = visible;
                w.active = visible;
            }
        }
    }

    private void buildPanelWidgets(AccordionItem item) {
        int[] bounds = getPanelBoundsFixed(item);
        if (bounds == null) return;

        int left = bounds[0];
        int top = bounds[1];
        int right = bounds[2];

        int innerX = left + 15;
        int innerY = top + 10;
        int innerRight = right - 15;

        List<AbstractWidget> widgets = new ArrayList<>();

        switch (item.id) {
            case "hud_editor" -> { }
            case "no_hurt_cam" -> { }
            case "no_bobbing" -> { }
            case "fast_exp" -> { }
            case "shift_tap" -> { }
            case "auto_sprint" -> { }
            case "item_physics" -> { }
            case "lyrics" -> buildLyricsPanel(widgets, innerX, innerY, innerRight);
            case "weather_change" -> buildWeatherChangePanel(widgets, innerX, innerY, innerRight);
            case "mod_logo" -> buildModLogoPanel(widgets, innerX, innerY, innerRight);
            case "freelook" -> buildFreelookPanel(widgets, innerX, innerY, innerRight);
            case "clear_liquids" -> buildClearLiquidsPanel(widgets, innerX, innerY, innerRight);
            case "headwear" -> buildHeadwearPanel(widgets, innerX, innerY, innerRight);
            case "cooldowns" -> buildCoolDownsPanel(widgets, innerX, innerY, innerRight);
            case "time_change" -> buildTimeChangePanel(widgets, innerX, innerY, innerRight);
            case "combo" -> buildComboPanel(widgets, innerX, innerY, innerRight);
            case "death_recap" -> buildDeathRecapPanel(widgets, innerX, innerY, innerRight);
            case "potion_effects" -> buildPotionEffectsPanel(widgets, innerX, innerY, innerRight);
            case "equipment_hud" -> buildEquipmentHudPanel(widgets, innerX, innerY, innerRight);
            case "smart_chat" -> buildSmartChatPanel(widgets, innerX, innerY, innerRight);
            case "effect_warnings" -> buildEffectWarningsPanel(widgets, innerX, innerY, innerRight);
            case "extra_hud" -> buildExtraHudPanel(widgets, innerX, innerY, innerRight);
            case "main_hud_settings" -> buildMainHudSettingsPanel(widgets, innerX, innerY, innerRight);
            case "ping_indicator" -> buildPingIndicatorPanel(widgets, innerX, innerY, innerRight);
            case "custom_hit_sounds" -> buildCustomHitSoundsPanel(widgets, innerX, innerY, innerRight);
            case "totem_log" -> buildTotemLogPanel(widgets, innerX, innerY, innerRight);
            case "cloud_control" -> buildCloudControlPanel(widgets, innerX, innerY, innerRight);
            case "trails" -> buildTrailsPanel(widgets, innerX, innerY, innerRight);
            case "target_hud" -> buildTargetHudPanel(widgets, innerX, innerY, innerRight);
            case "atmosphere" -> buildAtmospherePanel(widgets, innerX, innerY, innerRight);
            case "hit_particles" -> buildHitParticlesPanel(widgets, innerX, innerY, innerRight);
            case "auto_swap" -> buildAutoSwapPanel(widgets, innerX, innerY, innerRight);
            case "pvp_safe" -> buildPvPSafePanel(widgets, innerX, innerY, innerRight);
            case "pickup_logger" -> buildPickUpLoggerPanel(widgets, innerX, innerY, innerRight);
            case "tape_mouse" -> buildTapeMousePanel(widgets, innerX, innerY, innerRight);
            case "item_scroller" -> buildItemScrollerPanel(widgets, innerX, innerY, innerRight);
            case "zoom" -> buildZoomPanel(widgets, innerX, innerY, innerRight);
            case "custom_fog" -> buildCustomFogPanel(widgets, innerX, innerY, innerRight);
            case "crosshair" -> buildCrosshairPanel(widgets, innerX, innerY, innerRight);
            case "custom_hitbox" -> buildCustomHitboxPanel(widgets, innerX, innerY, innerRight);
            case "aspect_ratio" -> buildAspectRatioPanel(widgets, innerX, innerY, innerRight);
            case "fov" -> buildFovPanel(widgets, innerX, innerY, innerRight);
            case "better_bossbar" -> buildBetterBossBarPanel(widgets, innerX, innerY, innerRight);
            case "low_fire_shield" -> buildLowFireShieldPanel(widgets, innerX, innerY, innerRight);
            case "particle_blocker" -> buildParticleBlockerPanel(widgets, innerX, innerY, innerRight);
            case "waypoints" -> buildWaypointsPanel(widgets, innerX, innerY, innerRight);
            case "chat_filter" -> buildChatFilterPanel(widgets, innerX, innerY, innerRight);
            case "auto_reconnect" -> buildAutoReconnectPanel(widgets, innerX, innerY, innerRight);
            case "death_coords" -> buildDeathCoordsPanel(widgets, innerX, innerY, innerRight);
            case "gui_theme" -> buildGuiThemePanel(widgets, innerX, innerY, innerRight);
            case "config_manager" -> buildConfigManagerPanel(widgets, innerX, innerY, innerRight);
            case "voice_commands" -> buildVoiceCommandsPanel(widgets, innerX, innerY, innerRight);
            case "inventory_manager" -> buildInventoryManagerPanel(widgets, innerX, innerY, innerRight);
            case "gui_key" -> buildGuiKeyPanel(widgets, innerX, innerY, innerRight);
            case "auto_gg" -> buildAutoGGPanel(widgets, innerX, innerY, innerRight);
            case "strike_range" -> buildStrikeRangePanel(widgets, innerX, innerY, innerRight);
            case "crosshair_heatmap" -> buildCrosshairHeatmapPanel(widgets, innerX, innerY, innerRight);
            case "killaura_egg" -> buildKillAuraPanel(widgets, innerX, innerY, innerRight);
            case "target_esp" -> buildTargetEspPanel(widgets, innerX, innerY, innerRight);
            case "music_player" -> buildMusicPlayerPanel(widgets, innerX, innerY, innerRight);
            case "gamma_util" -> buildGammaUtilPanel(widgets, innerX, innerY, innerRight);
            case "auto_tool" -> buildAutoToolPanel(widgets, innerX, innerY, innerRight);
            case "macros" -> buildMacrosPanel(widgets, innerX, innerY, innerRight);
            case "predictions" -> buildPredictionsPanel(widgets, innerX, innerY, innerRight);
            case "low_hp_alert" -> buildLowHpAlertPanel(widgets, innerX, innerY, innerRight);
            case "auto_respawn" -> buildAutoRespawnPanel(widgets, innerX, innerY, innerRight);
            case "armor_alert" -> buildArmorAlertPanel(widgets, innerX, innerY, innerRight);
            case "friend_list" -> buildFriendListPanel(widgets, innerX, innerY, innerRight);
            case "auto_tp_accept" -> buildAutoTpAcceptPanel(widgets, innerX, innerY, innerRight);
            case "stats_tracker" -> buildStatsTrackerPanel(widgets, innerX, innerY, innerRight);
            case "kill_streak" -> buildKillStreakPanel(widgets, innerX, innerY, innerRight);
            case "enchant_highlight" -> buildEnchantHighlightPanel(widgets, innerX, innerY, innerRight);
            case "jump_circles" -> buildJumpCirclesPanel(widgets, innerX, innerY, innerRight);
            case "custom_main_menu" -> buildCustomMainMenuPanel(widgets, innerX, innerY, innerRight);
            default -> { }
        }
        for (AbstractWidget w : widgets) {
            this.addRenderableWidget(w);
        }
        panelWidgets.put(item.id, widgets);
        lastColumnWidthForWidgets = columnWidth;

        // Авто-высота: считаем максимальный Y + padding
        if (!widgets.isEmpty()) {
            int maxBottom = innerY;
            for (AbstractWidget w : widgets) {
                int bottom = w.getY() + w.getHeight();
                if (bottom > maxBottom) maxBottom = bottom;
            }
            item.contentHeight = maxBottom - innerY + 12;
        }
    }

    // ===================== ПАНЕЛИ =====================
    private void buildModLogoPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        addPosEditorRow(widgets, x, y, right,
                () -> ModConfig.modLogoX, () -> ModConfig.modLogoY,
                (nx, ny) -> {
                    ModConfig.modLogoX = nx;
                    ModConfig.modLogoY = ny;
                },
                10, 5);
    }

    private int addPosEditorRow(List<AbstractWidget> widgets,
                                int x, int y, int right,
                                Supplier<Integer> getX, Supplier<Integer> getY,
                                java.util.function.BiConsumer<Integer, Integer> setXY,
                                int defX, int defY) {
        int w = right - x;
        int rowH = 20;
        int gap = 6;

        EditBox posField = new EditBox(this.font, x, y, w - 50, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_hint")));
        posField.setMaxLength(20);
        posField.setValue(getX.get() + ", " + getY.get());
        widgets.add(posField);

        Button applyBtn = Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            try {
                String[] parts = posField.getValue().split(",");
                if (parts.length == 2) {
                    int nx = Integer.parseInt(parts[0].trim());
                    int ny = Integer.parseInt(parts[1].trim());
                    setXY.accept(nx, ny);
                    ConfigManager.save();
                }
            } catch (Exception ignored) {
            }
        }).bounds(x + w - 45, y, 20, 18).build();
        applyBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal(LocalizationManager.get("gui.resistancedlc.tooltip.apply"))));
        widgets.add(applyBtn);

        Button resetBtn = Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_reset")), (b) -> {
            setXY.accept(defX, defY);
            posField.setValue(defX + ", " + defY);
            ConfigManager.save();
        }).bounds(x + w - 22, y, 22, 18).build();
        resetBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal(LocalizationManager.get("gui.resistancedlc.tooltip.reset"))));
        widgets.add(resetBtn);

        return y + rowH + gap;
    }

    // ===================== HUD =====================
    private void buildPotionEffectsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show_icons")), this.font)
                .pos(x, curY).selected(ModConfig.potionEffectsIcons)
                .onValueChange((c, v) -> {
                    ModConfig.potionEffectsIcons = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.potionEffectsX, () -> ModConfig.potionEffectsY,
                (nx, ny) -> {
                    ModConfig.potionEffectsX = nx;
                    ModConfig.potionEffectsY = ny;
                },
                10, 170);
    }

    private void buildEquipmentHudPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.equipment_durability")), this.font)
                .pos(x, curY).selected(ModConfig.equipmentShowDurability)
                .onValueChange((c, v) -> {
                    ModConfig.equipmentShowDurability = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.equipmentHudX, () -> ModConfig.equipmentHudY,
                (nx, ny) -> {
                    ModConfig.equipmentHudX = nx;
                    ModConfig.equipmentHudY = ny;
                },
                4, -44);
    }

    private void buildEffectWarningsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show")), this.font)
                .pos(x, curY).selected(ModConfig.effectWarningsShowName)
                .onValueChange((c, v) -> {
                    ModConfig.effectWarningsShowName = v;
                    ConfigManager.save();
                })
                .build());

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show_icons")), this.font)
                .pos(x + w / 2, curY).selected(ModConfig.effectWarningsShowIcon)
                .onValueChange((c, v) -> {
                    ModConfig.effectWarningsShowIcon = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.threshold", ModConfig.effectWarningsThreshold)),
                (ModConfig.effectWarningsThreshold - 3) / 12.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.threshold",
                        ModConfig.effectWarningsThreshold)));
            }

            @Override
            protected void applyValue() {
                ModConfig.effectWarningsThreshold = 3 + (int) (this.value * 12);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.effectWarningsX, () -> ModConfig.effectWarningsY,
                (nx, ny) -> {
                    ModConfig.effectWarningsX = nx;
                    ModConfig.effectWarningsY = ny;
                },
                300, 200);
    }

    private void buildExtraHudPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 4;
        int cbW = (w - 6) / 3;
        int gearSize = 16;

        String[] names = {
                LocalizationManager.get("gui.resistancedlc.hud.fps"),
                LocalizationManager.get("gui.resistancedlc.hud.ping"),
                LocalizationManager.get("gui.resistancedlc.hud.tps"),
                LocalizationManager.get("gui.resistancedlc.hud.bps"),
                LocalizationManager.get("gui.resistancedlc.hud.direction"),
                LocalizationManager.get("gui.resistancedlc.hud.hits")
        };
        Supplier<Boolean>[] getters = new Supplier[]{
                (Supplier<Boolean>) () -> ModConfig.showFps,
                (Supplier<Boolean>) () -> ModConfig.showPing,
                (Supplier<Boolean>) () -> ModConfig.showTps,
                (Supplier<Boolean>) () -> ModConfig.showBps,
                (Supplier<Boolean>) () -> ModConfig.showDirection,
                (Supplier<Boolean>) () -> ModConfig.showHitCounter
        };
        java.util.function.Consumer<Boolean>[] setters = new java.util.function.Consumer[]{
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showFps = v,
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showPing = v,
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showTps = v,
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showBps = v,
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showDirection = v,
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showHitCounter = v
        };

        for (int i = 0; i < 6; i++) {
            final int idx = i;
            int col = i % 3;
            int row = i / 3;
            int cbX = x + col * (cbW + 3);
            int cbY = curY + row * rowH;

            widgets.add(Checkbox.builder(
                            Component.literal(names[i]), this.font)
                    .pos(cbX, cbY)
                    .selected(getters[i].get())
                    .onValueChange((c, v) -> {
                        setters[idx].accept(v);
                        ConfigManager.save();
                    })
                    .build());

            final int gearIdx = i;
            int gearX = cbX + cbW - gearSize - 2;
            int gearY = cbY + 1;

            widgets.add(Button.builder(
                    Component.literal("§6" + LocalizationManager.get("gui.resistancedlc.panel.gear")),
                    (b) -> {
                        activeExtraHudSetting = (activeExtraHudSetting == gearIdx) ? -1 : gearIdx;
                        rebuildExtraHudPanel();
                    }
            ).bounds(gearX, gearY, gearSize, gearSize).build());
        }
        curY += rowH * 2 + rowGap;

        if (activeExtraHudSetting >= 0) {
            final int idx = activeExtraHudSetting;
            String settingName = names[idx];

            Button labelBtn = Button.builder(
                    Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.setting", settingName)),
                    (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            labelBtn.active = false;
            widgets.add(labelBtn);
            curY += 18 + 4;

            final int finalIdx = idx;
            Supplier<Integer> getX = () -> {
                switch (finalIdx) {
                    case 0: return ModConfig.fpsX;
                    case 1: return ModConfig.pingX;
                    case 2: return ModConfig.tpsX;
                    case 3: return ModConfig.bpsX;
                    case 4: return ModConfig.directionX;
                    default: return ModConfig.hitCounterX;
                }
            };
            Supplier<Integer> getY = () -> {
                switch (finalIdx) {
                    case 0: return ModConfig.fpsY;
                    case 1: return ModConfig.pingY;
                    case 2: return ModConfig.tpsY;
                    case 3: return ModConfig.bpsY;
                    case 4: return ModConfig.directionY;
                    default: return ModConfig.hitCounterY;
                }
            };
            java.util.function.BiConsumer<Integer, Integer> setXY = (nx, ny) -> {
                switch (finalIdx) {
                    case 0 -> { ModConfig.fpsX = nx; ModConfig.fpsY = ny; }
                    case 1 -> { ModConfig.pingX = nx; ModConfig.pingY = ny; }
                    case 2 -> { ModConfig.tpsX = nx; ModConfig.tpsY = ny; }
                    case 3 -> { ModConfig.bpsX = nx; ModConfig.bpsY = ny; }
                    case 4 -> { ModConfig.directionX = nx; ModConfig.directionY = ny; }
                    default -> { ModConfig.hitCounterX = nx; ModConfig.hitCounterY = ny; }
                }
            };
            int[] defaults = {10, 80, 10, 95, 10, 110, 10, 125, 10, 140, 10, 155};
            int defX = defaults[finalIdx * 2];
            int defY = defaults[finalIdx * 2 + 1];

            addPosEditorRow(widgets, x, curY, right, getX, getY, setXY, defX, defY);
        }
    }

    private void rebuildExtraHudPanel() {
        rebuildPanelTwoPass("extra_hud");
    }
    private void buildMainHudSettingsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 4;
        int cbW = (w - 6) / 3;
        int gearSize = 16;

        String[] names = {
                LocalizationManager.get("gui.resistancedlc.panel.hud.coords"),
                LocalizationManager.get("gui.resistancedlc.panel.hud.biome"),
                LocalizationManager.get("gui.resistancedlc.panel.hud.time")
        };
        Supplier<Boolean>[] getters = new Supplier[]{
                (Supplier<Boolean>) () -> ModConfig.showCoords,
                (Supplier<Boolean>) () -> ModConfig.showBiome,
                (Supplier<Boolean>) () -> ModConfig.showTime
        };
        java.util.function.Consumer<Boolean>[] setters = new java.util.function.Consumer[]{
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showCoords = v,
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showBiome = v,
                (java.util.function.Consumer<Boolean>) v -> ModConfig.showTime = v
        };

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int col = i % 3;
            int row = i / 3;
            int cbX = x + col * (cbW + 3);
            int cbY = curY + row * rowH;

            widgets.add(Checkbox.builder(
                            Component.literal(names[i]), this.font)
                    .pos(cbX, cbY)
                    .selected(getters[i].get())
                    .onValueChange((c, v) -> {
                        setters[idx].accept(v);
                        ConfigManager.save();
                    })
                    .build());

            final int gearIdx = i;
            int gearX = cbX + cbW - gearSize - 2;
            int gearY = cbY + 1;

            widgets.add(Button.builder(
                    Component.literal("§6" + LocalizationManager.get("gui.resistancedlc.panel.gear")),
                    (b) -> {
                        activeMainHudSetting = (activeMainHudSetting == gearIdx) ? -1 : gearIdx;
                        rebuildMainHudSettingsPanel();
                    }
            ).bounds(gearX, gearY, gearSize, gearSize).build());
        }
        curY += rowH + rowGap;

        if (activeMainHudSetting >= 0) {
            final int idx = activeMainHudSetting;
            String settingName = names[idx];

            Button labelBtn = Button.builder(
                    Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.setting", settingName)),
                    (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            labelBtn.active = false;
            widgets.add(labelBtn);
            curY += 18 + 4;

            final int finalIdx = idx;
            Supplier<Integer> getX = () -> {
                switch (finalIdx) {
                    case 0: return ModConfig.coordsX;
                    case 1: return ModConfig.biomeX;
                    default: return ModConfig.timeX;
                }
            };
            Supplier<Integer> getY = () -> {
                switch (finalIdx) {
                    case 0: return ModConfig.coordsY;
                    case 1: return ModConfig.biomeY;
                    default: return ModConfig.timeY;
                }
            };
            java.util.function.BiConsumer<Integer, Integer> setXY = (nx, ny) -> {
                switch (finalIdx) {
                    case 0 -> { ModConfig.coordsX = nx; ModConfig.coordsY = ny; }
                    case 1 -> { ModConfig.biomeX = nx; ModConfig.biomeY = ny; }
                    default -> { ModConfig.timeX = nx; ModConfig.timeY = ny; }
                }
            };
            int[] defaults = {10, 35, 10, 50, 10, 65};
            int defX = defaults[finalIdx * 2];
            int defY = defaults[finalIdx * 2 + 1];

            addPosEditorRow(widgets, x, curY, right, getX, getY, setXY, defX, defY);
        }
    }

    private void rebuildMainHudSettingsPanel() {
        rebuildPanelTwoPass("main_hud_settings");
    }
    private void buildPingIndicatorPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.ping_indicator.show_ms")),
                        this.font)
                .pos(x, curY).selected(ModConfig.pingIndicatorShowMs)
                .onValueChange((c, v) -> {
                    ModConfig.pingIndicatorShowMs = v;
                    ConfigManager.save();
                })
                .build());
    }

    private AccordionItem findItemById(String id) {
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals(id)) return it;
            }
        }
        return null;
    }

    private void rebuildPanelTwoPass(String id) {
        AccordionItem item = findItemById(id);
        if (item == null || !item.expanded) return;

        clearPanelWidgets(item);
        buildPanelWidgets(item);
        updateContentMetrics();

        clearPanelWidgets(item);
        buildPanelWidgets(item);
        updateContentMetrics();
        updateWidgetsVisibility();
    }

    // ===================== PVP =====================
    private void buildCustomHitSoundsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 6;

        String keyName = KeyBindings.customHitSoundsKey != null
                ? KeyBindings.customHitSoundsKey.getTranslatedKeyMessage().getString() : "J";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 4
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", keyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 4;
                    b.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        int btnW = (w - 18) / 7;
        for (int i = 1; i <= 7; i++) {
            final int preset = i;
            Button soundBtn = Button.builder(Component.literal(String.valueOf(i)),
                            (b) -> {
                                ModConfig.customHitSoundPreset = preset;
                                ConfigManager.save();
                                rebuildCustomHitSoundsPresets();
                            })
                    .bounds(x + (i - 1) * (btnW + 3), curY, btnW, 20).build();
            soundBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(LocalizationManager.get("gui.resistancedlc.tooltip.sound_preset", preset))));
            widgets.add(soundBtn);
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.volume",
                        ModConfig.customHitSoundVolume)),
                (ModConfig.customHitSoundVolume - 0.1f) / 1.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.volume",
                        ModConfig.customHitSoundVolume)));
            }

            @Override
            protected void applyValue() {
                ModConfig.customHitSoundVolume = 0.1f + (float) (this.value * 1.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pitch", ModConfig.customHitSoundPitch)),
                (ModConfig.customHitSoundPitch - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pitch",
                        ModConfig.customHitSoundPitch)));
            }

            @Override
            protected void applyValue() {
                ModConfig.customHitSoundPitch = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void rebuildCustomHitSoundsPresets() {
        rebuildPanelTwoPass("custom_hit_sounds");
    }

    private void buildPvPSafePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 6;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.combat_timer", ModConfig.pvpSafeTimer)),
                (ModConfig.pvpSafeTimer - 10) / 50.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.combat_timer",
                        ModConfig.pvpSafeTimer)));
            }

            @Override
            protected void applyValue() {
                ModConfig.pvpSafeTimer = 10 + (int) (this.value * 50);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show")), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeBlockQuit)
                .onValueChange((c, v) -> {
                    ModConfig.pvpSafeBlockQuit = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show_slot")), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeBlockCommands)
                .onValueChange((c, v) -> {
                    ModConfig.pvpSafeBlockCommands = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_reconnect_hud")), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeShowHud)
                .onValueChange((c, v) -> {
                    ModConfig.pvpSafeShowHud = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        Button warnBtn = Button.builder(
                Component.literal("§c⚠ " + LocalizationManager.get("gui.resistancedlc.panel.pvp_safe_warning")),
                (b) -> {
                }
        ).bounds(x, curY, w, 18).build();
        warnBtn.active = false;
        widgets.add(warnBtn);
        curY += 18 + 4;

        Button warnBtn2 = Button.builder(
                Component.literal("§7" + LocalizationManager.get("gui.resistancedlc.panel.pvp_safe_warning2")),
                (b) -> {
                }
        ).bounds(x, curY, w, 18).build();
        warnBtn2.active = false;
        widgets.add(warnBtn2);
    }

    private void buildAutoGGPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_gg_only_players")), this.font)
                .pos(x, curY).selected(ModConfig.autoGgOnlyPlayers)
                .onValueChange((c, v) -> {
                    ModConfig.autoGgOnlyPlayers = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        EditBox templateField = new EditBox(this.font, x, curY, w, 18,
                Component.literal("GG %s"));
        templateField.setMaxLength(80);
        templateField.setValue(ModConfig.autoGgTemplate);
        templateField.setResponder(text -> {
            ModConfig.autoGgTemplate = text;
            ConfigManager.save();
        });
        widgets.add(templateField);
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_gg_delay", ModConfig.autoGgDelay)),
                ModConfig.autoGgDelay / 5.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_gg_delay",
                        ModConfig.autoGgDelay)));
            }

            @Override
            protected void applyValue() {
                ModConfig.autoGgDelay = (float) (this.value * 5.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void buildTargetEspPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        // ===== Режим =====
        String[] modes = {
                LocalizationManager.get("gui.resistancedlc.panel.target_esp.mode_crystals"),
                LocalizationManager.get("gui.resistancedlc.panel.target_esp.mode_cubes"),
                LocalizationManager.get("gui.resistancedlc.panel.target_esp.mode_ring"),
                LocalizationManager.get("gui.resistancedlc.panel.target_esp.mode_ghosts"),
        };
        String[] modeIds = {"crystals", "cubes", "ring", "ghosts"};
        int foundIdx = 0;
        for (int i = 0; i < modeIds.length; i++) {
            if (modeIds[i].equals(ModConfig.targetEspVariant)) {
                foundIdx = i;
                break;
            }
        }
        final int currentIdx = foundIdx;
        widgets.add(Button.builder(
                        Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.target_esp.mode") + ": §f" + modes[currentIdx]),
                        (b) -> {
                            int next = (currentIdx + 1) % modeIds.length;
                            ModConfig.targetEspVariant = modeIds[next];
                            ConfigManager.save();
                            rebuildTargetEspPanel();
                        })
                .bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        // ===== Цвет =====
        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.targetEspColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.targetEspColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.targetEspColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
        curY += rowH + rowGap;

        // ===== Размер =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.size",
                        ModConfig.targetEspSize)),
                (ModConfig.targetEspSize - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get(
                        "gui.resistancedlc.panel.target_esp.size",
                        ModConfig.targetEspSize)));
            }

            @Override
            protected void applyValue() {
                ModConfig.targetEspSize = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Вращение =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.rotation",
                        ModConfig.targetEspRotationSpeed)),
                ModConfig.targetEspRotationSpeed / 3.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get(
                        "gui.resistancedlc.panel.target_esp.rotation",
                        ModConfig.targetEspRotationSpeed)));
            }

            @Override
            protected void applyValue() {
                ModConfig.targetEspRotationSpeed = (float) (this.value * 3.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Пульсация =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.pulse",
                        ModConfig.targetEspPulse)),
                ModConfig.targetEspPulse / 0.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get(
                        "gui.resistancedlc.panel.target_esp.pulse",
                        ModConfig.targetEspPulse)));
            }

            @Override
            protected void applyValue() {
                ModConfig.targetEspPulse = (float) (this.value * 0.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Прозрачность =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.alpha", ModConfig.targetEspAlpha)),
                ModConfig.targetEspAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.alpha",
                        ModConfig.targetEspAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.targetEspAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Hide hitboxes =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.hide_hitboxes")), this.font)
                .pos(x, curY).selected(ModConfig.targetEspHideHitboxes)
                .onValueChange((c, v) -> {
                    ModConfig.targetEspHideHitboxes = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        // ===== Hurt =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_esp.hurt")), this.font)
                .pos(x, curY).selected(ModConfig.targetEspHurt)
                .onValueChange((c, v) -> {
                    ModConfig.targetEspHurt = v;
                    ConfigManager.save();
                })
                .build());
    }

    private void rebuildTargetEspPanel() {
        rebuildPanelTwoPass("target_esp");
    }

    // ===================== PVE =====================
    private void buildTapeMousePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Button.builder(
                Component.literal(ModConfig.tapeMouseButton == 0
                        ? LocalizationManager.get("gui.resistancedlc.panel.button", "LMB (attack)")
                        : LocalizationManager.get("gui.resistancedlc.panel.button", "RMB (use)")),
                (b) -> {
                    ModConfig.tapeMouseButton = (ModConfig.tapeMouseButton + 1) % 2;
                    ConfigManager.save();
                    rebuildTapeMousePanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        if (ModConfig.tapeMouseButton == 0) {
            String[] targets = {
                    LocalizationManager.get("gui.resistancedlc.panel.target", "All"),
                    LocalizationManager.get("gui.resistancedlc.panel.target", "Mobs only"),
                    LocalizationManager.get("gui.resistancedlc.panel.target", "Players only")
            };
            int tgt = Math.max(0, Math.min(2, ModConfig.tapeMouseTarget));
            widgets.add(Button.builder(
                    Component.literal(targets[tgt]),
                    (b) -> {
                        ModConfig.tapeMouseTarget = (ModConfig.tapeMouseTarget + 1) % 3;
                        ConfigManager.save();
                        rebuildTapeMousePanel();
                    }
            ).bounds(x, curY, w, 20).build());
            curY += rowH + rowGap;
        } else {
            widgets.add(Checkbox.builder(
                            Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hold_rmb")), this.font)
                    .pos(x, curY).selected(ModConfig.tapeMouseHoldRight)
                    .onValueChange((c, v) -> {
                        ModConfig.tapeMouseHoldRight = v;
                        ConfigManager.save();
                    })
                    .build());
            curY += rowH + rowGap;
        }

        String tmKeyName = KeyBindings.tapeMouseKey != null
                ? KeyBindings.tapeMouseKey.getTranslatedKeyMessage().getString() : "R";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 2
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", tmKeyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 2;
                    b.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay", ModConfig.tapeMouseDelay)),
                (ModConfig.tapeMouseDelay - 0.1f) / 4.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay",
                        ModConfig.tapeMouseDelay)));
            }

            @Override
            protected void applyValue() {
                ModConfig.tapeMouseDelay = 0.1f + (float) (this.value * 4.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        if (ModConfig.tapeMouseButton == 0) {
            widgets.add(Checkbox.builder(
                            Component.literal(LocalizationManager.get("gui.resistancedlc.panel.only_aiming")), this.font)
                    .pos(x, curY).selected(ModConfig.tapeMouseRequireTarget)
                    .onValueChange((c, v) -> {
                        ModConfig.tapeMouseRequireTarget = v;
                        ConfigManager.save();
                    })
                    .build());
            curY += rowH;

            widgets.add(Checkbox.builder(
                            Component.literal(LocalizationManager.get("gui.resistancedlc.panel.only_full_charge")), this.font)
                    .pos(x, curY).selected(ModConfig.tapeMouseRequireFullAttack)
                    .onValueChange((c, v) -> {
                        ModConfig.tapeMouseRequireFullAttack = v;
                        ConfigManager.save();
                    })
                    .build());
        }
    }

    private void rebuildTapeMousePanel() {
        rebuildPanelTwoPass("tape_mouse");
    }

    private void buildItemScrollerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay_ms", ModConfig.itemScrollerDelay)),
                (ModConfig.itemScrollerDelay - 100) / 400.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay_ms",
                        ModConfig.itemScrollerDelay)));
            }

            @Override
            protected void applyValue() {
                ModConfig.itemScrollerDelay = 100 + (int) (this.value * 400);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.shift_stack")), this.font)
                .pos(x, curY).selected(ModConfig.itemScrollerShiftStack)
                .onValueChange((c, v) -> {
                    ModConfig.itemScrollerShiftStack = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.ctrl_all")), this.font)
                .pos(x, curY).selected(ModConfig.itemScrollerCtrlAll)
                .onValueChange((c, v) -> {
                    ModConfig.itemScrollerCtrlAll = v;
                    ConfigManager.save();
                })
                .build());
    }

    private void buildAutoToolPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_tool.switch_back")), this.font)
                .pos(x, curY).selected(ModConfig.autoToolSwitchBack)
                .onValueChange((c, v) -> {
                    ModConfig.autoToolSwitchBack = v;
                    ConfigManager.save();
                })
                .build());
    }

    private void buildMacrosPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        for (int i = 0; i < 5; i++) {
            final int idx = i;

            EditBox field = new EditBox(this.font, x, curY, w - 30, 18,
                    Component.literal(LocalizationManager.get("gui.resistancedlc.panel.macros.hint")));
            field.setMaxLength(100);
            field.setValue(MacroManager.getMacroCommand(i));
            field.setResponder(text -> MacroManager.setMacroCommand(idx, text));
            widgets.add(field);

            String keyName = switch (idx) {
                case 0 ->
                        KeyBindings.macro1Key != null ? KeyBindings.macro1Key.getTranslatedKeyMessage().getString() : "F1";
                case 1 ->
                        KeyBindings.macro2Key != null ? KeyBindings.macro2Key.getTranslatedKeyMessage().getString() : "F2";
                case 2 ->
                        KeyBindings.macro3Key != null ? KeyBindings.macro3Key.getTranslatedKeyMessage().getString() : "F3";
                case 3 ->
                        KeyBindings.macro4Key != null ? KeyBindings.macro4Key.getTranslatedKeyMessage().getString() : "F4";
                default ->
                        KeyBindings.macro5Key != null ? KeyBindings.macro5Key.getTranslatedKeyMessage().getString() : "F5";
            };
            int bindTarget = 12 + idx;
            widgets.add(Button.builder(Component.literal(keyName), (b) -> {
                ModConfig.isBindingKey = true;
                ModConfig.bindingTarget = bindTarget;
            }).bounds(x + w - 25, curY, 25, 18).build());

            curY += rowH + rowGap;
        }
    }

    private void buildPredictionsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.predictionsColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            try {
                ModConfig.predictionsColor = 0xFF000000 | Integer.parseInt(hexField.getValue().replace("#", ""), 16);
                ConfigManager.save();
            } catch (Exception ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.predictionsColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.thickness",
                        ModConfig.predictionsThickness)),
                (ModConfig.predictionsThickness - 1.0f) / 4.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.thickness",
                        ModConfig.predictionsThickness)));
            }

            @Override
            protected void applyValue() {
                ModConfig.predictionsThickness = 1.0f + (float) (this.value * 4.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.alpha", ModConfig.predictionsAlpha)),
                ModConfig.predictionsAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.alpha", ModConfig.predictionsAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.predictionsAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.steps", ModConfig.predictionsSteps)),
                (ModConfig.predictionsSteps - 20) / 180.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.steps", ModConfig.predictionsSteps)));
            }

            @Override
            protected void applyValue() {
                ModConfig.predictionsSteps = 20 + (int) (this.value * 180);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.show_impact")),
                        this.font)
                .pos(x, curY).selected(ModConfig.predictionsShowImpact)
                .onValueChange((c, v) -> {
                    ModConfig.predictionsShowImpact = v;
                    ConfigManager.save();
                    Minecraft.getInstance().execute(this::rebuildPredictionsPanel);
                })
                .build());
        curY += rowH + rowGap;

        if (ModConfig.predictionsShowImpact) {
            String styleName = ModConfig.predictionsImpactStyle == 0
                    ? LocalizationManager.get("gui.resistancedlc.panel.predictions.impact_style_cross")
                    : LocalizationManager.get("gui.resistancedlc.panel.predictions.impact_style_dot");
            widgets.add(Button.builder(
                    Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.impact_style", styleName)),
                    (b) -> {
                        ModConfig.predictionsImpactStyle = (ModConfig.predictionsImpactStyle + 1) % 2;
                        ConfigManager.save();
                        Minecraft.getInstance().execute(this::rebuildPredictionsPanel);
                    }
            ).bounds(x, curY, w, 20).build());
            curY += rowH + rowGap;

            EditBox impactHex = new EditBox(this.font, x, curY, 80, 18,
                    Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
            impactHex.setMaxLength(7);
            impactHex.setValue(String.format("#%06X", ModConfig.predictionsImpactColor & 0xFFFFFF));
            widgets.add(impactHex);

            widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
                try {
                    ModConfig.predictionsImpactColor = 0xFF000000 | Integer.parseInt(
                            impactHex.getValue().replace("#", "").trim(), 16);
                    ConfigManager.save();
                } catch (Exception ignored) {
                }
            }).bounds(x + 85, curY, 35, 18).build());

            int ipResetX = x + 125;
            int ipResetW = (right - ipResetX - 9) / 4;
            String[] ipn = {"R", "G", "B", "W"};
            int[] ipc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
            for (int i = 0; i < 4; i++) {
                final int color = ipc[i];
                final String hex = String.format("#%06X", color & 0xFFFFFF);
                widgets.add(Button.builder(Component.literal(ipn[i]), (b) -> {
                    impactHex.setValue(hex);
                    ModConfig.predictionsImpactColor = color;
                    ConfigManager.save();
                }).bounds(ipResetX + i * (ipResetW + 3), curY, ipResetW, 18).build());
            }
            curY += rowH + rowGap;

            widgets.add(new AbstractSliderButton(x, curY, w, 20,
                    Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.impact_size",
                            ModConfig.predictionsImpactSize)),
                    (ModConfig.predictionsImpactSize - 0.1f) / 0.4f
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.predictions.impact_size",
                            ModConfig.predictionsImpactSize)));
                }

                @Override
                protected void applyValue() {
                    ModConfig.predictionsImpactSize = 0.1f + (float) (this.value * 0.4f);
                    this.updateMessage();
                    ConfigManager.save();
                }
            });
            curY += rowH + rowGap;
        }
    }

    private void rebuildPredictionsPanel() {
        rebuildPanelTwoPass("predictions");
    }

    // ===================== LOW HP ALERT =====================
    private void buildLowHpAlertPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_hp_alert.threshold",
                        ModConfig.lowHpAlertThreshold)),
                (ModConfig.lowHpAlertThreshold - 1.0f) / 19.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_hp_alert.threshold",
                        ModConfig.lowHpAlertThreshold)));
            }

            @Override
            protected void applyValue() {
                ModConfig.lowHpAlertThreshold = 1.0f + (float) (this.value * 19.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_hp_alert.alpha",
                        ModConfig.lowHpAlertAlpha)),
                ModConfig.lowHpAlertAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_hp_alert.alpha",
                        ModConfig.lowHpAlertAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.lowHpAlertAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_hp_alert.sound")), this.font)
                .pos(x, curY).selected(ModConfig.lowHpAlertSound)
                .onValueChange((c, v) -> {
                    ModConfig.lowHpAlertSound = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_hp_alert.blink")), this.font)
                .pos(x, curY).selected(ModConfig.lowHpAlertBlink)
                .onValueChange((c, v) -> {
                    ModConfig.lowHpAlertBlink = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.lowHpAlertColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            try {
                ModConfig.lowHpAlertColor = 0xFF000000 | Integer.parseInt(
                        hexField.getValue().replace("#", "").trim(), 16);
                ConfigManager.save();
            } catch (Exception ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.lowHpAlertColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
    }

    // ===================== AUTO RESPAWN =====================
    private void buildAutoRespawnPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_respawn.delay",
                        ModConfig.autoRespawnDelay)),
                (ModConfig.autoRespawnDelay - 0.5f) / 4.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_respawn.delay",
                        ModConfig.autoRespawnDelay)));
            }

            @Override
            protected void applyValue() {
                ModConfig.autoRespawnDelay = 0.5f + (float) (this.value * 4.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    // ===================== ARMOR ALERT =====================
    private void buildArmorAlertPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.armor_alert.threshold",
                        ModConfig.armorAlertThreshold)),
                (ModConfig.armorAlertThreshold - 5) / 45.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.armor_alert.threshold",
                        ModConfig.armorAlertThreshold)));
            }

            @Override
            protected void applyValue() {
                ModConfig.armorAlertThreshold = 5 + (int) (this.value * 45);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.armor_alert.alpha",
                        ModConfig.armorAlertAlpha)),
                ModConfig.armorAlertAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.armor_alert.alpha",
                        ModConfig.armorAlertAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.armorAlertAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.armor_alert.sound")), this.font)
                .pos(x, curY).selected(ModConfig.armorAlertSound)
                .onValueChange((c, v) -> {
                    ModConfig.armorAlertSound = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.armorAlertColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            try {
                ModConfig.armorAlertColor = 0xFF000000 | Integer.parseInt(
                        hexField.getValue().replace("#", "").trim(), 16);
                ConfigManager.save();
            } catch (Exception ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.armorAlertColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
    }

    // ===================== FRIEND LIST =====================
    private void buildFriendListPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.friend_list.show_hud")), this.font)
                .pos(x, curY).selected(ModConfig.friendListShowHud)
                .onValueChange((c, v) -> {
                    ModConfig.friendListShowHud = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.friend_list.hud_alpha",
                        ModConfig.friendListHudAlpha)),
                ModConfig.friendListHudAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.friend_list.hud_alpha",
                        ModConfig.friendListHudAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.friendListHudAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.friendListHudX, () -> ModConfig.friendListHudY,
                (nx, ny) -> {
                    ModConfig.friendListHudX = nx;
                    ModConfig.friendListHudY = ny;
                },
                -1, 10);

        EditBox nameField = new EditBox(this.font, x, curY, w - 25, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.friend_list.hint")));
        nameField.setMaxLength(20);
        widgets.add(nameField);

        widgets.add(Button.builder(Component.literal("+"), (b) -> {
            String name = nameField.getValue().trim();
            if (!name.isEmpty()) {
                if (FriendListManager.addFriend(name)) {
                    nameField.setValue("");
                    contentScroll = 0;
                    rebuildFriendListPanel();
                }
            }
        }).bounds(x + w - 22, curY, 22, 20).build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.friend_list.chat")), this.font)
                .pos(x, curY).selected(ModConfig.friendListHighlightChat)
                .onValueChange((c, v) -> {
                    ModConfig.friendListHighlightChat = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.friend_list.tab")), this.font)
                .pos(x, curY).selected(ModConfig.friendListHighlightTab)
                .onValueChange((c, v) -> {
                    ModConfig.friendListHighlightTab = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.friend_list.clear")),
                (b) -> {
                    FriendListManager.clearFriends();
                    contentScroll = 0;
                    rebuildFriendListPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        List<String> friends = FriendListManager.getFriends();
        if (!friends.isEmpty()) {
            Button headerBtn = Button.builder(
                    Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.friend_list.existing")
                            + " (§f" + friends.size() + "§e):"),
                    (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            headerBtn.active = false;
            widgets.add(headerBtn);
            curY += 18 + 4;

            int maxShow = Math.min(friends.size(), 10);
            for (int i = 0; i < maxShow; i++) {
                final String friend = friends.get(i);
                Button label = Button.builder(
                        Component.literal("§e" + friend), (b) -> {
                        }
                ).bounds(x, curY, w - 25, 18).build();
                label.active = false;
                widgets.add(label);

                widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.remove")), (b) -> {
                    FriendListManager.removeFriend(friend);
                    contentScroll = 0;
                    rebuildFriendListPanel();
                }).bounds(x + w - 22, curY, 22, 18).build());

                curY += 20;
            }

            if (friends.size() > 10) {
                Button moreBtn = Button.builder(
                        Component.literal("§7... ещё §e" + (friends.size() - 10)), (b) -> {
                        }
                ).bounds(x, curY, w, 18).build();
                moreBtn.active = false;
                widgets.add(moreBtn);
            }
        }
    }

    private void rebuildFriendListPanel() {
        rebuildPanelTwoPass("friend_list");
    }

    private int calcFriendListHeight() {
        int base = 18 + 186;
        int count = FriendListManager.getFriends().size();
        if (count > 0) {
            base += 22;
            base += 18 + 4;
            base += count * 20;
        }
        return base;
    }

    private int calcEnchantHighlightHeight() {
        int base = 18 + 2 * 28;
        int count = EnchantmentHighlightManager.getRules().size();
        if (count > 0) {
            base += 22;
            base += Math.min(count, 10) * 20;
            if (count > 10) base += 22;
        }
        if (activeEnchantSetting >= 0) {
            base += 4 * 28;
        }
        return base;
    }

    private int calcDeathRecapHeight() {
        int base = 26 + 26;
        int count = DeathRecapManager.getEntries().size();

        if (count == 0) {
            return base + 30;
        }

        base += count * 22;

        if (activeDeathRecapIndex >= 0 && activeDeathRecapIndex < count) {
            base += 220;
        }
        return base;
    }

    // ===================== AUTO TP ACCEPT =====================
    private void buildAutoTpAcceptPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String modeText = ModConfig.autoTpAcceptOnlyFriends
                ? LocalizationManager.get("gui.resistancedlc.panel.auto_tp_accept.only_friends")
                : LocalizationManager.get("gui.resistancedlc.panel.auto_tp_accept.from_all");
        widgets.add(Button.builder(
                Component.literal(modeText),
                (b) -> {
                    ModConfig.autoTpAcceptOnlyFriends = !ModConfig.autoTpAcceptOnlyFriends;
                    ConfigManager.save();
                    rebuildAutoTpAcceptPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_tp_accept.delay",
                        ModConfig.autoTpAcceptDelay)),
                (ModConfig.autoTpAcceptDelay - 0.1f) / 2.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_tp_accept.delay",
                        ModConfig.autoTpAcceptDelay)));
            }

            @Override
            protected void applyValue() {
                ModConfig.autoTpAcceptDelay = 0.1f + (float) (this.value * 2.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void rebuildAutoTpAcceptPanel() {
        rebuildPanelTwoPass("auto_tp_accept");
    }

    // ===================== STATS TRACKER =====================
    private void buildStatsTrackerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        Button kdBtn = Button.builder(
                Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.stats_tracker.kd")
                        + " §f" + StatsTrackerManager.formatKd()),
                (b) -> {
                }
        ).bounds(x, curY, w, 20).build();
        kdBtn.active = false;
        widgets.add(kdBtn);
        curY += rowH + rowGap;

        Button killsBtn = Button.builder(
                Component.literal("§a" + LocalizationManager.get("gui.resistancedlc.panel.stats_tracker.kills")
                        + " §f" + ModConfig.statsKills),
                (b) -> {
                }
        ).bounds(x, curY, w, 20).build();
        killsBtn.active = false;
        widgets.add(killsBtn);
        curY += rowH + rowGap;

        Button deathsBtn = Button.builder(
                Component.literal("§c" + LocalizationManager.get("gui.resistancedlc.panel.stats_tracker.deaths")
                        + " §f" + ModConfig.statsDeaths),
                (b) -> {
                }
        ).bounds(x, curY, w, 20).build();
        deathsBtn.active = false;
        widgets.add(deathsBtn);
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.stats_tracker.reset")),
                (b) -> {
                    StatsTrackerManager.resetStats();
                    rebuildStatsTrackerPanel();
                }
        ).bounds(x, curY, w, 20).build());
    }

    private void rebuildStatsTrackerPanel() {
        rebuildPanelTwoPass("stats_tracker");
    }

    // ===================== KILL STREAK =====================
    private void buildKillStreakPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.kill_streak.volume",
                        String.format("%.2f", ModConfig.killStreakSoundVolume))),
                ModConfig.killStreakSoundVolume
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.kill_streak.volume",
                        String.format("%.2f", ModConfig.killStreakSoundVolume))));
            }

            @Override
            protected void applyValue() {
                ModConfig.killStreakSoundVolume = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.kill_streak.alpha",
                        ModConfig.killStreakAlpha)),
                ModConfig.killStreakAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.kill_streak.alpha",
                        ModConfig.killStreakAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.killStreakAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        String[] sizes = {
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Small"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Medium"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Large")
        };
        int fs = Math.max(0, Math.min(2, ModConfig.killStreakFontSize));
        widgets.add(Button.builder(
                Component.literal(sizes[fs]),
                (b) -> {
                    ModConfig.killStreakFontSize = (ModConfig.killStreakFontSize + 1) % 3;
                    ConfigManager.save();
                    rebuildKillStreakPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.kill_streak.show_timer")), this.font)
                .pos(x, curY).selected(ModConfig.killStreakShowTimer)
                .onValueChange((c, v) -> {
                    ModConfig.killStreakShowTimer = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.killStreakHudX, () -> ModConfig.killStreakHudY,
                (nx, ny) -> {
                    ModConfig.killStreakHudX = nx;
                    ModConfig.killStreakHudY = ny;
                },
                -1, 10);

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.killStreakColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            try {
                ModConfig.killStreakColor = 0xFF000000 | Integer.parseInt(
                        hexField.getValue().replace("#", "").trim(), 16);
                ConfigManager.save();
            } catch (Exception ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.killStreakColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
    }

    private void rebuildKillStreakPanel() {
        rebuildPanelTwoPass("kill_streak");
    }

    // ===================== ENCHANTMENT HIGHLIGHT =====================
    private void buildEnchantHighlightPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        EditBox keyField = new EditBox(this.font, x, curY, w - 25, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enchant_highlight.hint")));
        keyField.setMaxLength(30);
        widgets.add(keyField);

        widgets.add(Button.builder(Component.literal("+"), (b) -> {
            String key = keyField.getValue().trim();
            if (!key.isEmpty()) {
                if (EnchantmentHighlightManager.addRule(key, 0xFFFFAA00, true)) {
                    keyField.setValue("");
                    activeEnchantSetting = -1;
                    contentScroll = 0;
                    rebuildEnchantHighlightPanel();
                }
            }
        }).bounds(x + w - 22, curY, 22, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enchant_highlight.clear")),
                (b) -> {
                    EnchantmentHighlightManager.clearRules();
                    activeEnchantSetting = -1;
                    contentScroll = 0;
                    rebuildEnchantHighlightPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        List<EnchantmentHighlightManager.Rule> rules = EnchantmentHighlightManager.getRules();
        if (rules.isEmpty()) return;

        Button headerBtn = Button.builder(
                Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.enchant_highlight.list")
                        + " (§f" + rules.size() + "§e):"),
                (b) -> {
                }
        ).bounds(x, curY, w, 18).build();
        headerBtn.active = false;
        widgets.add(headerBtn);
        curY += 18 + 4;

        int maxShow = Math.min(rules.size(), 10);
        for (int i = 0; i < maxShow; i++) {
            final int idx = i;
            final EnchantmentHighlightManager.Rule rule = rules.get(i);

            Checkbox cb = Checkbox.builder(Component.literal("§e" + rule.key), this.font)
                    .pos(x, curY)
                    .selected(true)
                    .onValueChange((c, v) -> {
                        if (!v) {
                            EnchantmentHighlightManager.removeRule(rule.key);
                            contentScroll = 0;
                            rebuildEnchantHighlightPanel();
                        }
                    })
                    .build();
            widgets.add(cb);

            widgets.add(Button.builder(Component.literal("§6" + LocalizationManager.get("gui.resistancedlc.panel.gear")), (b) -> {
                activeEnchantSetting = (activeEnchantSetting == idx) ? -1 : idx;
                rebuildEnchantHighlightPanel();
            }).bounds(x + w - 22, curY, 22, 18).build());

            curY += 20;
        }

        if (rules.size() > 10) {
            Button moreBtn = Button.builder(
                    Component.literal("§7... ещё §e" + (rules.size() - 10)), (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            moreBtn.active = false;
            widgets.add(moreBtn);
            curY += 18 + 4;
        }

        if (activeEnchantSetting >= 0 && activeEnchantSetting < rules.size()) {
            EnchantmentHighlightManager.Rule active = rules.get(activeEnchantSetting);

            Button titleBtn = Button.builder(
                    Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.enchant_highlight.setting")
                            + " §f" + active.key),
                    (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            titleBtn.active = false;
            widgets.add(titleBtn);
            curY += 18 + 4;

            EditBox keyEdit = new EditBox(this.font, x, curY, w, 18,
                    Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enchant_highlight.key")));
            keyEdit.setMaxLength(30);
            keyEdit.setValue(active.key);
            widgets.add(keyEdit);
            curY += 18 + 4;

            Checkbox boldCb = Checkbox.builder(
                            Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enchant_highlight.bold")), this.font)
                    .pos(x, curY).selected(active.bold)
                    .onValueChange((c, v) -> {
                    })
                    .build();
            widgets.add(boldCb);
            curY += 18 + 4;

            widgets.add(Button.builder(
                    Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")),
                    (b) -> {
                        String newKey = keyEdit.getValue().trim();
                        if (!newKey.isEmpty()) {
                            EnchantmentHighlightManager.removeRule(active.key);
                            EnchantmentHighlightManager.addRule(newKey, active.color, boldCb.selected());
                            activeEnchantSetting = -1;
                            contentScroll = 0;
                            rebuildEnchantHighlightPanel();
                        }
                    }
            ).bounds(x, curY, w, 18).build());
            curY += 18 + 4;

            int presetX = x;
            int presetW = (w - 9) / 4;
            String[] pn = {"R", "G", "B", "W"};
            int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
            for (int i = 0; i < 4; i++) {
                final int color = pc[i];
                widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                    EnchantmentHighlightManager.removeRule(active.key);
                    EnchantmentHighlightManager.addRule(active.key, color, boldCb.selected());
                    rebuildEnchantHighlightPanel();
                }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
            }
        }
    }

    private void rebuildEnchantHighlightPanel() {
        AccordionItem item = findItemById("enchant_highlight");
        if (item == null || !item.expanded) return;

        clearPanelWidgets(item);
        item.contentHeight = calcEnchantHighlightHeight();
        contentScroll = 0;
        buildPanelWidgets(item);
        updateWidgetsVisibility();
    }

    // ===================== VISUAL =====================
    private void buildZoomPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal("Zoom: " + String.format(java.util.Locale.ROOT, "%.1f", ModConfig.zoomFactor) + "x"),
                (ModConfig.zoomFactor - 1.5f) / 8.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal("Zoom: " + String.format(java.util.Locale.ROOT, "%.1f", ModConfig.zoomFactor) + "x"));
            }

            @Override
            protected void applyValue() {
                ModConfig.zoomFactor = 1.5f + (float) (this.value * 8.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.smoothness", ModConfig.zoomSmoothness)),
                (ModConfig.zoomSmoothness - 0.05f) / 0.95f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.smoothness",
                        ModConfig.zoomSmoothness)));
            }

            @Override
            protected void applyValue() {
                ModConfig.zoomSmoothness = 0.05f + (float) (this.value * 0.95f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        String keyName = KeyBindings.zoomKey != null
                ? KeyBindings.zoomKey.getTranslatedKeyMessage().getString() : "C";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 1
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", keyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 1;
                    b.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, curY, w, 20).build());
    }

    private void buildCrosshairPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String[] shapes = {
                LocalizationManager.get("gui.resistancedlc.panel.crosshair_shape", "Cross"),
                LocalizationManager.get("gui.resistancedlc.panel.crosshair_shape", "Dot"),
                LocalizationManager.get("gui.resistancedlc.panel.crosshair_shape", "Circle"),
                LocalizationManager.get("gui.resistancedlc.panel.crosshair_shape",
                        LocalizationManager.get("gui.resistancedlc.panel.crosshair_arrow")),
                LocalizationManager.get("gui.resistancedlc.panel.crosshair_shape", "Cross + Dot")
        };
        int shape = Math.max(0, Math.min(4, ModConfig.crosshairShape));
        widgets.add(Button.builder(
                Component.literal(shapes[shape]),
                (b) -> {
                    ModConfig.crosshairShape = (ModConfig.crosshairShape + 1) % 5;
                    ConfigManager.save();
                    rebuildCrosshairPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.size", ModConfig.crosshairSize)),
                (ModConfig.crosshairSize - 4) / 16.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.size",
                        ModConfig.crosshairSize)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairSize = 4 + (int) (this.value * 16);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.thickness", ModConfig.crosshairThickness)),
                (ModConfig.crosshairThickness - 1) / 4.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.thickness",
                        ModConfig.crosshairThickness)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairThickness = 1 + (int) (this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.gap", ModConfig.crosshairGap)),
                ModConfig.crosshairGap / 10.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.gap",
                        ModConfig.crosshairGap)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairGap = (int) (this.value * 10);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.crosshairAlpha)),
                ModConfig.crosshairAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.crosshairAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.crosshairColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.crosshairColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.crosshairColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
    }

    private void rebuildCrosshairPanel() {
        rebuildPanelTwoPass("crosshair");
    }

    private void buildCustomHitboxPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.customHitboxColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.customHitboxColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.customHitboxColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.customHitboxAlpha)),
                ModConfig.customHitboxAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.customHitboxAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.customHitboxAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void buildLowFireShieldPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_fire_offset", ModConfig.lowFireOffset)),
                ModConfig.lowFireOffset
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_fire_offset",
                        ModConfig.lowFireOffset)));
            }

            @Override
            protected void applyValue() {
                ModConfig.lowFireOffset = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_shield_offset", ModConfig.lowShieldOffset)),
                ModConfig.lowShieldOffset
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_shield_offset",
                        ModConfig.lowShieldOffset)));
            }

            @Override
            protected void applyValue() {
                ModConfig.lowShieldOffset = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void buildParticleBlockerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 4;
        int curY = y;
        int cbW = (w - 6) / 3;

        String[] cats = {"Fire", "Smoke", "Explosions", "Potions", "Water", "Redstone", "Portal", "Crits"};
        boolean[] vals = {
                ModConfig.particleBlockerFire, ModConfig.particleBlockerSmoke,
                ModConfig.particleBlockerExplosion, ModConfig.particleBlockerPotions,
                ModConfig.particleBlockerWater, ModConfig.particleBlockerRedstone,
                ModConfig.particleBlockerPortal, ModConfig.particleBlockerCrit
        };
        for (int i = 0; i < cats.length; i++) {
            final int idx = i;
            int col = i % 3;
            int row = i / 3;
            widgets.add(Checkbox.builder(
                            Component.literal(cats[i]), this.font)
                    .pos(x + col * (cbW + 3), curY + row * rowH)
                    .selected(vals[i])
                    .onValueChange((c, v) -> {
                        switch (idx) {
                            case 0 -> ModConfig.particleBlockerFire = v;
                            case 1 -> ModConfig.particleBlockerSmoke = v;
                            case 2 -> ModConfig.particleBlockerExplosion = v;
                            case 3 -> ModConfig.particleBlockerPotions = v;
                            case 4 -> ModConfig.particleBlockerWater = v;
                            case 5 -> ModConfig.particleBlockerRedstone = v;
                            case 6 -> ModConfig.particleBlockerPortal = v;
                            case 7 -> ModConfig.particleBlockerCrit = v;
                        }
                        ConfigManager.save();
                    })
                    .build());
        }
    }

    // ===================== WAYPOINTS =====================
    private int calcWaypointsHeight() {
        int base = 18 + 4 * 28;
        int waypointsCount = WaypointManager.getWaypoints().size();
        if (waypointsCount > 0) {
            base += 22;
            base += waypointsCount * 20;
        }
        return base;
    }

    private void rebuildWaypointsPanel() {
        AccordionItem wp = findItemById("waypoints");
        if (wp == null || !wp.expanded) return;

        clearPanelWidgets(wp);
        wp.contentHeight = calcWaypointsHeight() - 28;
        contentScroll = 0;
        buildPanelWidgets(wp);
        updateWidgetsVisibility();
    }

    private void buildWaypointsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.waypoints_max", ModConfig.waypointsMax)),
                (ModConfig.waypointsMax - 1) / 19.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.waypoints_max",
                        ModConfig.waypointsMax)));
            }

            @Override
            protected void applyValue() {
                ModConfig.waypointsMax = 1 + (int) (this.value * 19);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.waypoints_add")),
                (b) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) return;
                    double px = mc.player.getX();
                    double py = mc.player.getY();
                    double pz = mc.player.getZ();
                    boolean added = WaypointManager.addWaypoint(px, py, pz);
                    if (added) {
                        int idx = WaypointManager.getWaypoints().size();
                        mc.player.sendSystemMessage(Component.literal(String.format(
                                "§a[Waypoints] Добавлена метка §6WP%d§a: §e%d, %d, %d",
                                idx, (int) px, (int) py, (int) pz)));
                        contentScroll = 0;
                        rebuildWaypointsPanel();
                    } else {
                        mc.player.sendSystemMessage(Component.literal(
                                "§c[Waypoints] Достигнут лимит (" + ModConfig.waypointsMax + ")"));
                    }
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.waypoints_clear")),
                (b) -> {
                    WaypointManager.clearWaypoints();
                    contentScroll = 0;
                    rebuildWaypointsPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        List<Waypoint> list = WaypointManager.getWaypoints();
        if (!list.isEmpty()) {
            Button headerBtn = Button.builder(
                    Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.existing")
                            + " (§f" + list.size() + "§e):"),
                    (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            headerBtn.active = false;
            widgets.add(headerBtn);
            curY += 18 + 4;

            for (int i = 0; i < list.size(); i++) {
                final int index = i;
                final Waypoint wp = list.get(i);
                int rowY = curY;

                Button label = Button.builder(
                        Component.literal("§e" + wp.name() + " §7(" + (int) wp.x() + ", " + (int) wp.y() + ", " + (int) wp.z() + ")"),
                        (b) -> {
                        }
                ).bounds(x, rowY, w - 50, 18).build();
                label.active = false;
                widgets.add(label);

                widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.rename")), (b) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal(
                                "§7Переименование: используйте команду §e/wp rename <старое> <новое>"));
                    }
                }).bounds(x + w - 48, rowY, 22, 18).build());

                widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.remove")), (b) -> {
                    WaypointManager.removeWaypoint(index);
                    contentScroll = 0;
                    rebuildWaypointsPanel();
                }).bounds(x + w - 24, rowY, 22, 18).build());

                curY += 20;
            }
        }
    }

    // ===================== MISC =====================
    private void buildChatFilterPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        EditBox wordField = new EditBox(this.font, x, curY, w - 30, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.chat_filter_placeholder")));
        wordField.setMaxLength(30);
        widgets.add(wordField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.add")), (b) -> {
            String word = wordField.getValue().trim();
            if (!word.isEmpty()) {
                ChatFilterManager.addWord(word);
                wordField.setValue("");
                rebuildChatFilterPanel();
            }
        }).bounds(x + w - 25, curY, 25, 20).build());
        curY += rowH + rowGap;

        java.util.List<String> words = ChatFilterManager.getWords();
        int maxShow = 3;
        int rowListH = 18;

        for (int i = 0; i < Math.min(words.size(), maxShow); i++) {
            final String word = words.get(i);
            Button wordLabel = Button.builder(Component.literal("§e" + word), (b) -> {
                    })
                    .bounds(x, curY, w - 25, rowListH).build();
            wordLabel.active = false;
            widgets.add(wordLabel);

            widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.remove")), (b) -> {
                ChatFilterManager.removeWord(word);
                rebuildChatFilterPanel();
            }).bounds(x + w - 22, curY, 22, rowListH).build());
            curY += rowListH + 2;
        }

        if (words.size() > maxShow) {
            Button moreBtn = Button.builder(
                            Component.literal("§7... ещё §e" + (words.size() - maxShow)), (b) -> {
                            })
                    .bounds(x, curY, w, rowListH).build();
            moreBtn.active = false;
            widgets.add(moreBtn);
            curY += rowListH + 2;
        }

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.chat_filter_clear")),
                (b) -> {
                    ChatFilterManager.clearWords();
                    rebuildChatFilterPanel();
                }
        ).bounds(x, curY, w, 20).build());
    }

    private void rebuildChatFilterPanel() {
        AccordionItem cf = findItemById("chat_filter");
        if (cf == null || !cf.expanded) return;

        clearPanelWidgets(cf);
        cf.contentHeight = calcChatFilterHeight();
        buildPanelWidgets(cf);
        updateContentMetrics();
        updateWidgetsVisibility();
    }

    private void buildAutoReconnectPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_reconnect_delay",
                        ModConfig.autoReconnectDelay)),
                (ModConfig.autoReconnectDelay - 1) / 29.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_reconnect_delay",
                        ModConfig.autoReconnectDelay)));
            }

            @Override
            protected void applyValue() {
                ModConfig.autoReconnectDelay = 1 + (int) (this.value * 29);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_reconnect_hud")), this.font)
                .pos(x, curY).selected(ModConfig.autoReconnectShowHud)
                .onValueChange((c, v) -> {
                    ModConfig.autoReconnectShowHud = v;
                    ConfigManager.save();
                })
                .build());
    }

    private void buildDeathCoordsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.show_last_death")),
                (b) -> DeathCoordsManager.showLastDeath()
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.clear_last_death")),
                (b) -> DeathCoordsManager.clearLastDeath()
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        String info;
        if (ModConfig.lastDeathTime > 0) {
            info = "§7" + LocalizationManager.get("gui.resistancedlc.panel.last_death",
                    ModConfig.lastDeathX, ModConfig.lastDeathY, ModConfig.lastDeathZ);
        } else {
            info = "§8" + LocalizationManager.get("gui.resistancedlc.panel.no_death");
        }
        Button infoBtn = Button.builder(Component.literal(info), (b) -> {
                })
                .bounds(x, curY, w, 20).build();
        infoBtn.active = false;
        widgets.add(infoBtn);
    }

    private void buildSmartChatPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.smart_chat.grouping")), this.font)
                .pos(x, curY).selected(ModConfig.smartChatGroupingEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.smartChatGroupingEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.smart_chat.coord_click")), this.font)
                .pos(x, curY).selected(ModConfig.smartChatCoordClickEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.smartChatCoordClickEnabled = v;
                    ConfigManager.save();
                })
                .build());
    }

    // ===================== GUI THEME =====================
    private void buildGuiThemePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        for (int i = 0; i < THEMES.length; i++) {
            final int idx = i;
            final int[] theme = THEMES[i];
            final String name = THEME_NAMES[i];

            boolean isActive = (ModConfig.guiColor == theme[0]
                    && ModConfig.guiTextColor == theme[1]
                    && ModConfig.hudColor == theme[2]);

            String prefix = isActive ? "§l✔ " : "  ";
            String suffix = isActive ? " §7" + LocalizationManager.get("gui.resistancedlc.panel.theme_active") : "";

            Button themeBtn = Button.builder(
                            Component.literal(prefix + name + suffix),
                            (b) -> {
                                ModConfig.guiColor = theme[0];
                                ModConfig.guiTextColor = theme[1];
                                ModConfig.hudColor = theme[2];
                                ThemeManager.setThemeIndex(idx);
                                ConfigManager.save();
                                rebuildGuiThemePanel();
                            })
                    .bounds(x, curY, w, 20).build();
            widgets.add(themeBtn);

            curY += rowH + rowGap;
        }

        curY += 4;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.theme_anim_enable")),
                        this.font)
                .pos(x, curY).selected(ModConfig.themeAnimationEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.themeAnimationEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.theme_anim_speed",
                        ModConfig.themeAnimationSpeed)),
                (ModConfig.themeAnimationSpeed - 0.1f) / 2.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.theme_anim_speed",
                        ModConfig.themeAnimationSpeed)));
            }

            @Override
            protected void applyValue() {
                ModConfig.themeAnimationSpeed = 0.1f + (float) (this.value * 2.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void rebuildGuiThemePanel() {
        AccordionItem theme = findItemById("gui_theme");
        if (theme == null || !theme.expanded) return;

        clearPanelWidgets(theme);
        buildPanelWidgets(theme);
        updateContentMetrics();
        updateWidgetsVisibility();
    }

    // ===================== CONFIG MANAGER =====================
    private int calcConfigManagerHeight() {
        int base = 18 + 4 * 28;
        int configsCount = ConfigManager.listConfigs().size();
        if (configsCount > 0) {
            base += 22;
            int shown = Math.min(configsCount, 20);
            base += shown * 20;
            if (configsCount > 20) {
                base += 22;
            }
        }
        return base;
    }

    // ===================== INVENTORY MANAGER =====================
    private int calcInventoryManagerHeight() {
        int base = 18 + 6;
        base += MAX_INVENTORY_SLOTS * 26;
        base += 22;
        if (activeInventoryDetail >= 0) {
            base += 340;
        }
        return base;
    }

    private void buildInventoryManagerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 4;
        int curY = y;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.inventory.open_folder")),
                (b) -> {
                    InventorySnapshotManager.openFolder();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal(
                                        "§a" + LocalizationManager.get(
                                                "gui.resistancedlc.inventory.folder_opened",
                                                InventorySnapshotManager.getDir().toAbsolutePath().toString())));
                    }
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        List<InventorySnapshot> all = InventorySnapshotManager.listAll();

        for (int i = 0; i < MAX_INVENTORY_SLOTS; i++) {
            final int slotIndex = i + 1;
            InventorySnapshot snap = (i < all.size()) ? all.get(i) : null;

            int rowY = curY;

            String labelText;
            if (snap == null) {
                labelText = "§7" + LocalizationManager.get(
                        "gui.resistancedlc.inventory.slot_empty", slotIndex);
            } else {
                String timeStr = formatDeathTime(snap.getTimestamp());
                labelText = "§e# " + slotIndex + " §f" + snap.getName()
                        + " §7· " + timeStr;
                if (labelText.length() > 55) labelText = labelText.substring(0, 53) + "…";
            }

            final int finalSlotIndex = slotIndex;
            widgets.add(Button.builder(
                    Component.literal(labelText),
                    (b) -> Minecraft.getInstance().gui.setScreen(
                            new com.resistancedlc.inventory.InventorySaveScreen(
                                    finalSlotIndex, this))
            ).bounds(x, rowY, w - 25, 20).build());

            boolean isOpen = (activeInventoryDetail == i);
            String arrow = isOpen ? "▲" : "▼";
            final int finalI = i;
            widgets.add(Button.builder(Component.literal(arrow), (b) -> {
                if (activeInventoryDetail == finalI) {
                    activeInventoryDetail = -1;
                } else {
                    activeInventoryDetail = finalI;
                }
                Minecraft.getInstance().execute(this::rebuildInventoryManagerPanel);
            }).bounds(x + w - 22, rowY, 22, 20).build());

            curY += rowH + rowGap;

            if (isOpen && snap != null) {
                curY = buildInventoryDetailsView(widgets, x, curY, w, slotIndex, snap);
                curY += 6;
            } else if (isOpen) {
                Button emptyBtn = Button.builder(
                        Component.literal("§8" + LocalizationManager.get(
                                "gui.resistancedlc.inventory.detail_empty")),
                        (b) -> {
                        }
                ).bounds(x, curY, w, 18).build();
                emptyBtn.active = false;
                widgets.add(emptyBtn);
                curY += 18 + 6;
            }
        }
    }

    private int buildInventoryDetailsView(List<AbstractWidget> widgets,
                                          int x, int y, int w,
                                          int slotIndex, InventorySnapshot snap) {
        int curY = y;

        int steveBoxH = 90;
        widgets.add(new ArmorPreviewWidget(x, curY, steveBoxH, snap));

        int iconX = x + steveBoxH + 10;
        int iconY = curY + 4;
        addEquipmentIconRow(widgets, iconX, iconY, snap.getSlot(InventorySnapshot.IDX_HEAD), "Helmet");
        addEquipmentIconRow(widgets, iconX, iconY + 20, snap.getSlot(InventorySnapshot.IDX_CHEST), "Chestplate");
        addEquipmentIconRow(widgets, iconX, iconY + 40, snap.getSlot(InventorySnapshot.IDX_LEGS), "Leggings");
        addEquipmentIconRow(widgets, iconX, iconY + 60, snap.getSlot(InventorySnapshot.IDX_FEET), "Boots");
        addEquipmentIconRow(widgets, iconX, iconY + 80, snap.getSlot(InventorySnapshot.IDX_OFFHAND), "Offhand");

        curY += steveBoxH + 6;

        widgets.add(new SlotLabelWidget(x, curY, Component.literal("§7Hotbar:"), 200));
        curY += 12;
        curY = addSlotRow(widgets, x, curY, 9, InventorySnapshot.IDX_HOTBAR_0, snap);

        widgets.add(new SlotLabelWidget(x, curY, Component.literal("§7Inventory:"), 200));
        curY += 12;
        for (int row = 0; row < 3; row++) {
            curY = addSlotRow(widgets, x, curY, 9,
                    InventorySnapshot.IDX_MAIN_9 + row * 9, snap);
        }

        int btnW = (w - 8) / 3;
        int btnY = curY + 4;

        final int finalSlotIndex = slotIndex;

        widgets.add(Button.builder(
                Component.literal("§a" + LocalizationManager.get(
                        "gui.resistancedlc.inventory.btn.save")),
                (b) -> Minecraft.getInstance().gui.setScreen(
                        new com.resistancedlc.inventory.InventorySaveScreen(
                                finalSlotIndex, this))
        ).bounds(x, btnY, btnW, 20).build());

        widgets.add(Button.builder(
                Component.literal("§6" + LocalizationManager.get(
                        "gui.resistancedlc.inventory.btn.rename")),
                (b) -> Minecraft.getInstance().gui.setScreen(
                        new com.resistancedlc.inventory.InventoryRenameScreen(
                                finalSlotIndex, this))
        ).bounds(x + btnW + 4, btnY, btnW, 20).build());

        widgets.add(Button.builder(
                Component.literal("§c" + LocalizationManager.get(
                        "gui.resistancedlc.inventory.btn.delete")),
                (b) -> {
                    InventorySnapshotManager.delete(finalSlotIndex);
                    activeInventoryDetail = -1;
                    Minecraft.getInstance().execute(this::rebuildInventoryManagerPanel);
                }
        ).bounds(x + (btnW + 4) * 2, btnY, btnW, 20).build());

        return btnY + 20;
    }

    private int addSlotRow(List<AbstractWidget> widgets, int x, int y, int count,
                           int startIdx, InventorySnapshot snap) {
        for (int i = 0; i < count; i++) {
            int sx = x + i * 22;
            widgets.add(new InventorySlotWidget(sx, y, snap.getSlot(startIdx + i)));
        }
        return y + 22;
    }

    private void addEquipmentIconRow(List<AbstractWidget> widgets, int x, int y,
                                     ItemStack stack, String label) {
        widgets.add(new InventorySlotWidget(x, y, stack));
        widgets.add(new SlotLabelWidget(x + 22, y + 5,
                Component.literal("§7" + label), 150));
    }

    private void rebuildInventoryManagerPanel() {
        AccordionItem item = findItemById("inventory_manager");
        if (item == null || !item.expanded) return;

        clearPanelWidgets(item);
        item.contentHeight = calcInventoryManagerHeight();
        contentScroll = 0;
        buildPanelWidgets(item);
        updateWidgetsVisibility();
    }

    private void buildConfigManagerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.config_open_folder")),
                (b) -> {
                    ConfigManager.openFolder();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal(
                                "§a" + LocalizationManager.get("gui.resistancedlc.message.folder_opened",
                                        ConfigManager.getConfigDirPath())));
                    }
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.config_save")),
                (b) -> {
                    ConfigManager.saveNow();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal(
                                "§a" + LocalizationManager.get("gui.resistancedlc.message.config_saved", "default")));
                    }
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.config_load")),
                (b) -> {
                    if (ConfigManager.loadFrom("default")) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal(
                                    "§a" + LocalizationManager.get("gui.resistancedlc.message.config_loaded", "default")));
                        }
                        contentScroll = 0;
                        rebuildConfigManagerPanel();
                    } else {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal(
                                    "§c" + LocalizationManager.get("gui.resistancedlc.message.config_not_found", "default")));
                        }
                    }
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        EditBox nameField = new EditBox(this.font, x, curY, w - 90, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.config_name")));
        nameField.setMaxLength(30);
        widgets.add(nameField);

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.config_save_as")),
                (b) -> {
                    String name = nameField.getValue().trim();
                    if (name.isEmpty()) name = "default";
                    if (ConfigManager.saveAs(name)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal(
                                    "§a" + LocalizationManager.get("gui.resistancedlc.message.config_saved", name)));
                        }
                        contentScroll = 0;
                        rebuildConfigManagerPanel();
                    }
                }
        ).bounds(x + w - 85, curY, 85, 20).build());
        curY += rowH + rowGap;

        List<String> configs = ConfigManager.listConfigs();
        if (!configs.isEmpty()) {
            Button headerBtn = Button.builder(
                    Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.config_list")
                            + " (§f" + configs.size() + "§e):"),
                    (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            headerBtn.active = false;
            widgets.add(headerBtn);
            curY += 18 + 4;

            int maxShow = Math.min(configs.size(), 20);
            for (int i = 0; i < maxShow; i++) {
                final String cfgName = configs.get(i);
                int rowY = curY;

                Button label = Button.builder(
                        Component.literal("§e" + cfgName), (b) -> {
                        }
                ).bounds(x, rowY, w - 25, 18).build();
                label.active = false;
                widgets.add(label);

                widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.remove")), (b) -> {
                    if (ConfigManager.remove(cfgName)) {
                        contentScroll = 0;
                        rebuildConfigManagerPanel();
                    }
                }).bounds(x + w - 22, rowY, 22, 18).build());

                curY += 20;
            }
        }
    }

    private void rebuildConfigManagerPanel() {
        AccordionItem cfg = findItemById("config_manager");
        if (cfg == null || !cfg.expanded) return;

        clearPanelWidgets(cfg);
        cfg.contentHeight = calcConfigManagerHeight();
        contentScroll = 0;
        buildPanelWidgets(cfg);
        updateWidgetsVisibility();
    }

    private int calcChatFilterHeight() {
        int base = 18;
        base += 28;
        int words = ChatFilterManager.getWords().size();
        if (words > 0) {
            int shown = Math.min(words, 3);
            base += shown * 20;
            if (words > 3) base += 20;
        }
        return base + 8;
    }

    // ===================== COOLDOWNS =====================
    private void buildCoolDownsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.cooldownsX, () -> ModConfig.cooldownsY,
                (nx, ny) -> {
                    ModConfig.cooldownsX = nx;
                    ModConfig.cooldownsY = ny;
                },
                10, 200);

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.max_items", ModConfig.cooldownsMaxItems)),
                (ModConfig.cooldownsMaxItems - 1) / 9.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.max_items",
                        ModConfig.cooldownsMaxItems)));
            }

            @Override
            protected void applyValue() {
                ModConfig.cooldownsMaxItems = 1 + (int) (this.value * 9);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.cooldownsAlpha)),
                ModConfig.cooldownsAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.cooldownsAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.cooldownsAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.icon_darkening", ModConfig.cooldownsIconDarkening)),
                ModConfig.cooldownsIconDarkening / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.icon_darkening",
                        ModConfig.cooldownsIconDarkening)));
            }

            @Override
            protected void applyValue() {
                ModConfig.cooldownsIconDarkening = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        int cbW = (w - 6) / 3;
        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.icon")), this.font)
                .pos(x, curY).selected(ModConfig.cooldownsShowIcon)
                .onValueChange((c, v) -> {
                    ModConfig.cooldownsShowIcon = v;
                    ConfigManager.save();
                }).build());
        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.name")), this.font)
                .pos(x + cbW + 3, curY).selected(ModConfig.cooldownsShowName)
                .onValueChange((c, v) -> {
                    ModConfig.cooldownsShowName = v;
                    ConfigManager.save();
                }).build());
        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.timer")), this.font)
                .pos(x + (cbW + 3) * 2, curY).selected(ModConfig.cooldownsShowTime)
                .onValueChange((c, v) -> {
                    ModConfig.cooldownsShowTime = v;
                    ConfigManager.save();
                }).build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hotbar_only")), this.font)
                .pos(x, curY).selected(ModConfig.cooldownsShowOnlyHotbar)
                .onValueChange((c, v) -> {
                    ModConfig.cooldownsShowOnlyHotbar = v;
                    ConfigManager.save();
                }).build());
        curY += rowH + rowGap;

        String[] sizes = {
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Small"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Medium"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Large")
        };
        int fs = Math.max(0, Math.min(2, ModConfig.cooldownsFontSize));
        widgets.add(Button.builder(
                Component.literal(sizes[fs]),
                (b) -> {
                    ModConfig.cooldownsFontSize = (ModConfig.cooldownsFontSize + 1) % 3;
                    ConfigManager.save();
                    rebuildCoolDownsPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.reset")),
                (b) -> {
                    ModConfig.cooldownsX = 10;
                    ModConfig.cooldownsY = 200;
                    ConfigManager.save();
                    rebuildCoolDownsPanel();
                }
        ).bounds(x, curY, w, 20).build());
    }

    private void rebuildCoolDownsPanel() {
        rebuildPanelTwoPass("cooldowns");
    }

    private void buildComboPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.comboX, () -> ModConfig.comboY,
                (nx, ny) -> {
                    ModConfig.comboX = nx;
                    ModConfig.comboY = ny;
                },
                10, 185);

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.timer", ModConfig.comboResetTime)),
                (ModConfig.comboResetTime - 1) / 4.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.timer",
                        ModConfig.comboResetTime)));
            }

            @Override
            protected void applyValue() {
                ModConfig.comboResetTime = 1 + (int) (this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        String[] sizes = {
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Small"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Medium"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Large")
        };
        int fs = Math.max(0, Math.min(2, ModConfig.comboFontSize));
        widgets.add(Button.builder(
                Component.literal(sizes[fs]),
                (b) -> {
                    ModConfig.comboFontSize = (ModConfig.comboFontSize + 1) % 3;
                    ConfigManager.save();
                    rebuildComboPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.comboColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.comboColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 6) / 3;
        String[] pn = {"Y", "R", "G"};
        int[] pc = {0xFFFFFF00, 0xFFFF0000, 0xFF00FF00};
        for (int i = 0; i < 3; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.comboColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
    }

    private void rebuildComboPanel() {
        rebuildPanelTwoPass("combo");
    }

    // ===================== TOTEM LOG =====================
    private void buildTotemLogPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String keyName = KeyBindings.totemLogKey != null
                ? KeyBindings.totemLogKey.getTranslatedKeyMessage().getString() : "O";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 10
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", keyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 10;
                    b.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.radius", ModConfig.totemLogRadius)),
                (ModConfig.totemLogRadius - 5) / 15.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.radius",
                        ModConfig.totemLogRadius)));
            }

            @Override
            protected void applyValue() {
                ModConfig.totemLogRadius = 5 + (int) (this.value * 15.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.sound_notify")), this.font)
                .pos(x, curY).selected(ModConfig.totemLogSound)
                .onValueChange((c, v) -> {
                    ModConfig.totemLogSound = v;
                    ConfigManager.save();
                })
                .build());
    }

    // ===================== AUTOSWAP =====================
    private void buildAutoSwapPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String[] modes = {
                LocalizationManager.get("gui.resistancedlc.panel.mode_head_head"),
                LocalizationManager.get("gui.resistancedlc.panel.mode_totem_totem"),
                LocalizationManager.get("gui.resistancedlc.panel.mode_head_totem"),
                LocalizationManager.get("gui.resistancedlc.panel.mode_totem_head")
        };
        int halfW = (w - 3) / 2;
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            int col = i % 2;
            int row = i / 2;
            widgets.add(Button.builder(
                    Component.literal(modes[i]),
                    (b) -> {
                        ModConfig.autoSwapMode = idx;
                        ConfigManager.save();
                        rebuildAutoSwapPanel();
                    }
            ).bounds(x + col * (halfW + 3), curY + row * (rowH - 2), halfW, 20).build());
        }
        curY += rowH * 2 + rowGap - 2;

        String keyName = KeyBindings.autoSwapKey != null
                ? KeyBindings.autoSwapKey.getTranslatedKeyMessage().getString() : "H";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 3
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", keyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 3;
                    b.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay_ms", ModConfig.autoSwapOpenDelay)),
                (ModConfig.autoSwapOpenDelay - 50) / 450.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay_ms",
                        ModConfig.autoSwapOpenDelay)));
            }

            @Override
            protected void applyValue() {
                ModConfig.autoSwapOpenDelay = 50 + (int) (this.value * 450);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal("Cooldown: " + ModConfig.autoSwapCooldown + " ms"),
                (ModConfig.autoSwapCooldown - 100) / 1900.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal("Cooldown: " + ModConfig.autoSwapCooldown + " ms"));
            }

            @Override
            protected void applyValue() {
                ModConfig.autoSwapCooldown = 100 + (int) (this.value * 1900);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void rebuildAutoSwapPanel() {
        rebuildPanelTwoPass("auto_swap");
    }

    // ===================== PICKUP LOGGER =====================
    private void buildPickUpLoggerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String keyName = KeyBindings.pickupLogKey != null
                ? KeyBindings.pickupLogKey.getTranslatedKeyMessage().getString() : "P";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 11
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", keyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 11;
                    b.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        String[] modes = {
                LocalizationManager.get("gui.resistancedlc.panel.mode", "All items"),
                LocalizationManager.get("gui.resistancedlc.panel.mode", "Valuable only"),
                LocalizationManager.get("gui.resistancedlc.panel.mode", "By categories")
        };
        int mode = Math.max(0, Math.min(2, ModConfig.pickupLogMode));
        widgets.add(Button.builder(
                Component.literal(modes[mode]),
                (b) -> {
                    ModConfig.pickupLogMode = (ModConfig.pickupLogMode + 1) % 3;
                    ConfigManager.save();
                    rebuildPickUpLoggerPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        if (ModConfig.pickupLogMode == 2) {
            int cbW = (w - 6) / 3;
            String[] cats = {"Weapon", "Armor", "Potions", "Totems", "Heads", "Spawners", "Structure blocks"};
            boolean[] vals = {
                    ModConfig.pickupLogWeapon, ModConfig.pickupLogArmor,
                    ModConfig.pickupLogPotions, ModConfig.pickupLogTotems,
                    ModConfig.pickupLogHeads, ModConfig.pickupLogSpawners,
                    ModConfig.pickupLogStructureBlocks
            };
            for (int i = 0; i < cats.length; i++) {
                final int idx = i;
                int col = i % 3;
                int row = i / 3;
                widgets.add(Checkbox.builder(
                                Component.literal(cats[i]), this.font)
                        .pos(x + col * (cbW + 3), curY + row * rowH)
                        .selected(vals[i])
                        .onValueChange((c, v) -> {
                            switch (idx) {
                                case 0 -> ModConfig.pickupLogWeapon = v;
                                case 1 -> ModConfig.pickupLogArmor = v;
                                case 2 -> ModConfig.pickupLogPotions = v;
                                case 3 -> ModConfig.pickupLogTotems = v;
                                case 4 -> ModConfig.pickupLogHeads = v;
                                case 5 -> ModConfig.pickupLogSpawners = v;
                                case 6 -> ModConfig.pickupLogStructureBlocks = v;
                            }
                            ConfigManager.save();
                        })
                        .build());
            }
        }
    }

    private void rebuildPickUpLoggerPanel() {
        rebuildPanelTwoPass("pickup_logger");
    }

    // ===================== STRIKE RANGE =====================
    private void buildStrikeRangePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.strikeRangeX, () -> ModConfig.strikeRangeY,
                (nx, ny) -> {
                    ModConfig.strikeRangeX = nx;
                    ModConfig.strikeRangeY = ny;
                },
                10, 300);

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.strikeRangeAlpha)),
                ModConfig.strikeRangeAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.strikeRangeAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.strikeRangeAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.strike_range_show_time", ModConfig.strikeRangeShowTime)),
                ModConfig.strikeRangeShowTime / 5000.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.strike_range_show_time",
                        ModConfig.strikeRangeShowTime)));
            }

            @Override
            protected void applyValue() {
                ModConfig.strikeRangeShowTime = 200 + (int) (this.value * 4800);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        String[] sizes = {
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Small"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Medium"),
                LocalizationManager.get("gui.resistancedlc.panel.font_size", "Large")
        };
        int fs = Math.max(0, Math.min(2, ModConfig.strikeRangeFontSize));
        widgets.add(Button.builder(
                Component.literal(sizes[fs]),
                (b) -> {
                    ModConfig.strikeRangeFontSize = (ModConfig.strikeRangeFontSize + 1) % 3;
                    ConfigManager.save();
                    rebuildStrikeRangePanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.strike_range_show_blocks")), this.font)
                .pos(x, curY).selected(ModConfig.strikeRangeShowBlocks)
                .onValueChange((c, v) -> {
                    ModConfig.strikeRangeShowBlocks = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.strike_range_show_target")), this.font)
                .pos(x, curY).selected(ModConfig.strikeRangeShowTarget)
                .onValueChange((c, v) -> {
                    ModConfig.strikeRangeShowTarget = v;
                    ConfigManager.save();
                })
                .build());
    }

    private void rebuildStrikeRangePanel() {
        rebuildPanelTwoPass("strike_range");
    }

    private void buildGammaUtilPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.gamma_util.value",
                        ModConfig.gammaValue)),
                (ModConfig.gammaValue - 1.0f) / 99.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.gamma_util.value",
                        ModConfig.gammaValue)));
            }

            @Override
            protected void applyValue() {
                ModConfig.gammaValue = 1.0f + (float) (this.value * 99.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    // ===================== MUSIC PLAYER =====================
    private int calcMusicPlayerHeight() {
        int base = 18 + 10 * 28;
        int trackCount = MusicPlayerManager.getPlaylist().size();
        if (trackCount > 0) {
            base += 22;
            int shown = Math.min(trackCount, 5);
            base += shown * 20;
            if (trackCount > 5) base += 22;
        }
        return base;
    }

    private void buildMusicPlayerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        int btnW = (w - 12) / 5;
        widgets.add(Button.builder(
                        Component.literal("▶"),
                        (b) -> MusicPlayerManager.play())
                .bounds(x, curY, btnW, 20).build());
        widgets.add(Button.builder(
                        Component.literal("⏸"),
                        (b) -> MusicPlayerManager.pause())
                .bounds(x + btnW + 3, curY, btnW, 20).build());
        widgets.add(Button.builder(
                        Component.literal("⏮"),
                        (b) -> MusicPlayerManager.prev())
                .bounds(x + (btnW + 3) * 2, curY, btnW, 20).build());
        widgets.add(Button.builder(
                        Component.literal("⏭"),
                        (b) -> MusicPlayerManager.next())
                .bounds(x + (btnW + 3) * 3, curY, btnW, 20).build());
        widgets.add(Button.builder(
                        Component.literal("⏹"),
                        (b) -> MusicPlayerManager.stop())
                .bounds(x + (btnW + 3) * 4, curY, btnW, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_volume",
                        (int) (ModConfig.musicVolume * 100))),
                ModConfig.musicVolume
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_volume",
                        (int) (ModConfig.musicVolume * 100))));
            }

            @Override
            protected void applyValue() {
                MusicPlayerManager.setVolume((float) this.value);
                this.updateMessage();
            }
        });
        curY += rowH + rowGap;

        String[] repeatNames = {
                LocalizationManager.get("gui.resistancedlc.panel.music_repeat_off"),
                LocalizationManager.get("gui.resistancedlc.panel.music_repeat_one"),
                LocalizationManager.get("gui.resistancedlc.panel.music_repeat_all")
        };
        int rMode = Math.max(0, Math.min(2, ModConfig.musicRepeat));
        widgets.add(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_repeat", repeatNames[rMode])),
                        (b) -> {
                            MusicPlayerManager.cycleRepeat();
                            rebuildMusicPlayerPanel();
                        })
                .bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_shuffle")),
                        this.font)
                .pos(x, curY).selected(ModConfig.musicShuffle)
                .onValueChange((c, v) -> MusicPlayerManager.toggleShuffle())
                .build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_show_hud")),
                        this.font)
                .pos(x, curY).selected(ModConfig.musicShowHud)
                .onValueChange((c, v) -> {
                    ModConfig.musicShowHud = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_autoplay")),
                        this.font)
                .pos(x, curY).selected(ModConfig.musicAutoPlay)
                .onValueChange((c, v) -> {
                    ModConfig.musicAutoPlay = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        int halfW = (w - 3) / 2;
        widgets.add(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_open_folder")),
                        (b) -> MusicPlayerManager.openMusicFolder())
                .bounds(x, curY, halfW, 20).build());
        widgets.add(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_rescan")),
                        (b) -> {
                            MusicPlayerManager.rescan();
                            rebuildMusicPlayerPanel();
                        })
                .bounds(x + halfW + 3, curY, halfW, 20).build());
        curY += rowH + rowGap;

        MusicTrack current = MusicPlayerManager.getCurrentTrack();
        String infoText;
        if (current != null) {
            infoText = "§a♪ §f" + current.displayFull();
            if (infoText.length() > 60) infoText = infoText.substring(0, 58) + "…";
        } else {
            infoText = "§7" + LocalizationManager.get("gui.resistancedlc.panel.music_nothing_playing");
        }
        Button infoBtn = Button.builder(Component.literal(infoText), (b) -> {
                })
                .bounds(x, curY, w, 18).build();
        infoBtn.active = false;
        widgets.add(infoBtn);
        curY += 18 + rowGap;

        List<MusicTrack> tracks = MusicPlayerManager.getPlaylist();
        if (!tracks.isEmpty()) {
            Button headerBtn = Button.builder(
                            Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.music_tracks")
                                    + " (§f" + tracks.size() + "§e):"),
                            (b) -> {
                            })
                    .bounds(x, curY, w, 18).build();
            headerBtn.active = false;
            widgets.add(headerBtn);
            curY += 18 + 4;

            int maxShow = Math.min(tracks.size(), 5);
            for (int i = 0; i < maxShow; i++) {
                final int idx = i;
                MusicTrack t = tracks.get(i);
                String label = "§7" + (i + 1) + ". §f" + t.displayFull();
                if (label.length() > 55) label = label.substring(0, 53) + "…";

                boolean isCurrent = (i == MusicPlayerManager.getCurrentIndex());
                if (isCurrent) label = "§a▶ " + label;

                widgets.add(Button.builder(
                                Component.literal(label),
                                (b) -> MusicPlayerManager.playTrackAt(idx))
                        .bounds(x, curY, w, 18).build());
                curY += 20;
            }

            if (tracks.size() > 5) {
                Button moreBtn = Button.builder(
                        Component.literal("§7... §e" + (tracks.size() - 5) + " §7ещё"),
                        (b) -> {
                        }).bounds(x, curY, w, 18).build();
                moreBtn.active = false;
                widgets.add(moreBtn);
            }
        } else {
            Button emptyBtn = Button.builder(
                    Component.literal("§7" + LocalizationManager.get("gui.resistancedlc.panel.music_empty")),
                    (b) -> {
                    }).bounds(x, curY, w, 18).build();
            emptyBtn.active = false;
            widgets.add(emptyBtn);
        }
    }

    private void rebuildMusicPlayerPanel() {
        AccordionItem mp = findItemById("music_player");
        if (mp == null || !mp.expanded) return;

        clearPanelWidgets(mp);
        mp.contentHeight = calcMusicPlayerHeight() - 28;
        contentScroll = 0;
        buildPanelWidgets(mp);
        updateWidgetsVisibility();
    }

    // ===================== VOICE COMMANDS =====================
    private void buildVoiceCommandsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        Button warn = Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.experimental")),
                        (b) -> {})
                .bounds(x, curY, w, 18).build();
        warn.active = false;
        widgets.add(warn);
        curY += 18 + rowGap;

        String calText = ModConfig.voiceVadCalibratedThreshold > 0
                ? String.format(LocalizationManager.get("gui.resistancedlc.panel.voice.calibrated"),
                ModConfig.voiceVadCalibratedThreshold)
                : LocalizationManager.get("gui.resistancedlc.panel.voice.not_calibrated");
        Button calBtn = Button.builder(Component.literal("§7" + calText), (b) -> {})
                .bounds(x, curY, w, 18).build();
        calBtn.active = false;
        widgets.add(calBtn);
        curY += 18 + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.sensitivity",
                        ModConfig.voiceVadSensitivity)),
                (ModConfig.voiceVadSensitivity - 0.3f) / 2.7f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.sensitivity",
                        ModConfig.voiceVadSensitivity)));
            }
            @Override
            protected void applyValue() {
                ModConfig.voiceVadSensitivity = 0.3f + (float) (this.value * 2.7f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.match_threshold",
                        ModConfig.voiceCommandThreshold)),
                (ModConfig.voiceCommandThreshold - 0.20f) / 0.75f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.match_threshold",
                        ModConfig.voiceCommandThreshold)));
            }
            @Override
            protected void applyValue() {
                ModConfig.voiceCommandThreshold = 0.20f + (float) (this.value * 0.75f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.recalibrate")),
                (b) -> {
                    com.resistancedlc.voice.VoiceCommandListener.startCalibration();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal(
                                "§e[Voice] Calibration started. Stay silent for 10 sec..."));
                    }
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.manage")),
                (b) -> Minecraft.getInstance().gui.setScreen(
                        new com.resistancedlc.voice.gui.VoiceCommandSelectScreen(
                                Minecraft.getInstance().gui.screen()))
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.open_folder")),
                (b) -> {
                    try {
                        String os = System.getProperty("os.name").toLowerCase();
                        String path = com.resistancedlc.voice.VoiceCommandManager
                                .getVoiceDir().toAbsolutePath().toString();
                        ProcessBuilder pb;
                        if (os.contains("win")) pb = new ProcessBuilder("explorer.exe", path);
                        else if (os.contains("mac")) pb = new ProcessBuilder("open", path);
                        else pb = new ProcessBuilder("xdg-open", path);
                        pb.start();
                    } catch (Exception ignored) {}
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.voice.rescan")),
                (b) -> {
                    com.resistancedlc.voice.VoiceCommandManager.loadAll();
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.sendSystemMessage(Component.literal(
                                "§a[Voice] Rescan done. Commands: "
                                        + com.resistancedlc.voice.VoiceCommandManager
                                        .getAllMfcc().size()));
                    }
                }
        ).bounds(x, curY, w, 20).build());
    }

    private void rebuildVoiceCommandsPanel() {
        rebuildPanelTwoPass("voice_commands");
    }

    // ===================== LYRICS =====================
    private void buildLyricsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 24, rowGap = 6;
        int cbH = 26;
        int curY = y;

        // ===== Distance =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.distance",
                        ModConfig.lyricsDistance)),
                (ModConfig.lyricsDistance - 1.0f) / 14.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.distance",
                        ModConfig.lyricsDistance)));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsDistance = 1.0f + (float) (this.value * 14.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Font size =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.font_size",
                        ModConfig.lyricsFontSize)),
                (ModConfig.lyricsFontSize - 0.8f) / 1.2f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.font_size",
                        ModConfig.lyricsFontSize)));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsFontSize = 0.8f + (float) (this.value * 1.2f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Line spacing =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.line_spacing",
                        (float) ModConfig.lyricsLineSpacing)),
                (ModConfig.lyricsLineSpacing - 0.2f) / 1.3f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.line_spacing",
                        (float) ModConfig.lyricsLineSpacing)));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsLineSpacing = 0.2f + (float) (this.value * 1.3f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Pulse strength =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.pulse_strength",
                        (float) ModConfig.lyricsPulseStrength)),
                ModConfig.lyricsPulseStrength / 0.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.pulse_strength",
                        (float) ModConfig.lyricsPulseStrength)));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsPulseStrength = (float) (this.value * 0.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Outline thickness =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.outline_thickness",
                        ModConfig.lyricsOutlineThickness)),
                (ModConfig.lyricsOutlineThickness - 1) / 2.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.outline_thickness",
                        ModConfig.lyricsOutlineThickness)));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsOutlineThickness = 1 + (int) (this.value * 2);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Gradient strength =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.gradient_strength",
                        (float) ModConfig.lyricsGradientStrength)),
                ModConfig.lyricsGradientStrength
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.gradient_strength",
                        (float) ModConfig.lyricsGradientStrength)));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsGradientStrength = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Checkbox: Pulse =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.pulse_enable")),
                        this.font)
                .pos(x, curY).selected(ModConfig.lyricsPulseEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.lyricsPulseEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += cbH;

        // ===== Checkbox: LRCLIB =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.lrclib_enable")),
                        this.font)
                .pos(x, curY).selected(ModConfig.lyricsLrclibEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.lyricsLrclibEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += cbH;

        // ===== Checkbox: Outline =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.outline_enable")),
                        this.font)
                .pos(x, curY).selected(ModConfig.lyricsOutlineEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.lyricsOutlineEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += cbH;

        // ===== Checkbox: Holographic =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.holographic_enable")),
                        this.font)
                .pos(x, curY).selected(ModConfig.lyricsHolographic)
                .onValueChange((c, v) -> {
                    ModConfig.lyricsHolographic = v;
                    ConfigManager.save();
                })
                .build());
        curY += cbH;

        // ===== Checkbox: Entrance =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.entrance_enable")),
                        this.font)
                .pos(x, curY).selected(ModConfig.lyricsEntranceEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.lyricsEntranceEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += cbH + rowGap;

        // ===== Angle =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.offset_x",
                        String.format("%.2f", ModConfig.lyricsOffsetX))),
                (ModConfig.lyricsOffsetX + 1.0f) / 2.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.offset_x",
                        String.format("%.2f", ModConfig.lyricsOffsetX))));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsOffsetX = (float) (this.value * 2.0 - 1.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Alpha =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.alpha", ModConfig.lyricsAlpha)),
                ModConfig.lyricsAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.alpha",
                        ModConfig.lyricsAlpha)));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Checkbox: Show next =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.show_next")),
                        this.font)
                .pos(x, curY).selected(ModConfig.lyricsShowNext)
                .onValueChange((c, v) -> {
                    ModConfig.lyricsShowNext = v;
                    ConfigManager.save();
                })
                .build());
        curY += cbH + rowGap;
        // ===== Checkbox: Shake =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.shake_enable")),
                        this.font)
                .pos(x, curY).selected(ModConfig.lyricsShakeEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.lyricsShakeEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += cbH;

        // ===== Shake speed =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.shake_speed",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.lyricsShakeSpeed))),
                ModConfig.lyricsShakeSpeed / 3.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.shake_speed",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.lyricsShakeSpeed))));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsShakeSpeed = (float) (this.value * 3.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Shake radius =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.shake_radius",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.lyricsShakeRadius))),
                ModConfig.lyricsShakeRadius / 3.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.lyrics.shake_radius",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.lyricsShakeRadius))));
            }
            @Override
            protected void applyValue() {
                ModConfig.lyricsShakeRadius = (float) (this.value * 3.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;
        // ===== Colors =====
        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.lyrics.current_color"),
                () -> ModConfig.lyricsCurrentColor,
                v -> ModConfig.lyricsCurrentColor = v);
        curY += rowH + rowGap;

        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.lyrics.next_color"),
                () -> ModConfig.lyricsNextColor,
                v -> ModConfig.lyricsNextColor = v);
    }

    private void addColorRow(List<AbstractWidget> widgets, int x, int y, int right,
                             String label, Supplier<Integer> getter,
                             java.util.function.IntConsumer setter) {
        int w = right - x;
        Button lbl = Button.builder(Component.literal("§7" + label), (b) -> {
                })
                .bounds(x, y, w, 14).build();
        lbl.active = false;
        widgets.add(lbl);

        EditBox hex = new EditBox(this.font, x, y + 16, 80, 18,
                Component.literal("#RRGGBB"));
        hex.setMaxLength(7);
        hex.setValue(String.format("#%06X", getter.get() & 0xFFFFFF));
        widgets.add(hex);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            try {
                int c = 0xFF000000 | Integer.parseInt(hex.getValue().replace("#", "").trim(), 16);
                setter.accept(c);
                ConfigManager.save();
            } catch (Exception ignored) {
            }
        }).bounds(x + 85, y + 16, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hx = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hex.setValue(hx);
                setter.accept(color);
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), y + 16, presetW, 18).build());
        }
    }

    // ===================== EASTER EGG (KILLAURA) =====================
    private void buildKillAuraPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.killaura_button")),
                        (b) -> EasterEggManager.onKillAuraClick())
                .bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        String defaultHint = LocalizationManager.get("gui.resistancedlc.panel.killaura_hint");
        Button hintBtn = Button.builder(
                Component.literal("§7" + defaultHint),
                (b) -> {
                }
        ).bounds(x, curY, w, 18).build();
        hintBtn.active = false;
        widgets.add(hintBtn);

        EasterEggManager.registerHintButton(hintBtn);
    }

    // ===================== CROSSHAIR HEATMAP =====================
    private void buildCrosshairHeatmapPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.track_mobs")),
                        this.font)
                .pos(x, curY).selected(ModConfig.crosshairHeatmapTrackMobs)
                .onValueChange((c, v) -> {
                    ModConfig.crosshairHeatmapTrackMobs = v;
                    ConfigManager.save();
                })
                .build());

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.track_players")),
                        this.font)
                .pos(x + w / 2, curY).selected(ModConfig.crosshairHeatmapTrackPlayers)
                .onValueChange((c, v) -> {
                    ModConfig.crosshairHeatmapTrackPlayers = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.crosshairHeatmapColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.crosshairHeatmapColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.crosshairHeatmapColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.crosshairHeatmapAlpha)),
                ModConfig.crosshairHeatmapAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.crosshairHeatmapAlpha)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairHeatmapAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.size",
                        ModConfig.crosshairHeatmapMarkSize)),
                (ModConfig.crosshairHeatmapMarkSize - 1) / 7.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.size",
                        ModConfig.crosshairHeatmapMarkSize)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairHeatmapMarkSize = 1 + (int) (this.value * 7);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.lifetime",
                        ModConfig.crosshairHeatmapMarkLifetime)),
                (ModConfig.crosshairHeatmapMarkLifetime - 500) / 9500.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.lifetime",
                        ModConfig.crosshairHeatmapMarkLifetime)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairHeatmapMarkLifetime = 500 + (int) (this.value * 9500);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.max_marks",
                        ModConfig.crosshairHeatmapMaxMarks)),
                (ModConfig.crosshairHeatmapMaxMarks - 10) / 490.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.max_marks",
                        ModConfig.crosshairHeatmapMaxMarks)));
            }

            @Override
            protected void applyValue() {
                ModConfig.crosshairHeatmapMaxMarks = 10 + (int) (this.value * 490);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        double hitChance = CrosshairHeatmapManager.getHitChance();
        double missChance = CrosshairHeatmapManager.getMissChance();
        int total = ModConfig.crosshairHeatmapTotalHits + ModConfig.crosshairHeatmapTotalMisses;

        Button hitBtn = Button.builder(
                Component.literal("§a" + String.format(
                        LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.stats_hit"),
                        hitChance)),
                (b) -> {
                }
        ).bounds(x, curY, w, 20).build();
        hitBtn.active = false;
        widgets.add(hitBtn);
        curY += rowH;

        Button missBtn = Button.builder(
                Component.literal("§c" + String.format(
                        LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.stats_miss"),
                        missChance)),
                (b) -> {
                }
        ).bounds(x, curY, w, 20).build();
        missBtn.active = false;
        widgets.add(missBtn);
        curY += rowH;

        Button wrBtn = Button.builder(
                Component.literal("§e" + String.format(
                        LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.stats_winrate"),
                        hitChance)),
                (b) -> {
                }
        ).bounds(x, curY, w, 20).build();
        wrBtn.active = false;
        widgets.add(wrBtn);
        curY += rowH;

        Button totalBtn = Button.builder(
                Component.literal("§7" + LocalizationManager.get(
                        "gui.resistancedlc.panel.crosshair_heatmap.stats_total", total)),
                (b) -> {
                }
        ).bounds(x, curY, w, 20).build();
        totalBtn.active = false;
        widgets.add(totalBtn);
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.crosshair_heatmap.reset_stats")),
                (b) -> {
                    CrosshairHeatmapManager.resetStats();
                    rebuildCrosshairHeatmapPanel();
                }
        ).bounds(x, curY, w, 20).build());
    }

    private void rebuildCrosshairHeatmapPanel() {
        rebuildPanelTwoPass("crosshair_heatmap");
    }

    // ===================== DEATH RECAP =====================
    private void buildDeathRecapPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 18, rowGap = 4;
        int curY = y;

        List<DeathRecapEntry> entries = DeathRecapManager.getEntries();

        Button headerBtn = Button.builder(
                Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.death_recap.list")
                        + " (§f" + entries.size() + "§e/§f" + DeathRecapManager.MAX_ENTRIES + "§e):"),
                (b) -> {
                }
        ).bounds(x, curY, w, 18).build();
        headerBtn.active = false;
        widgets.add(headerBtn);
        curY += 18 + 4;

        if (entries.isEmpty()) {
            Button emptyBtn = Button.builder(
                    Component.literal("§7" + LocalizationManager.get("gui.resistancedlc.panel.death_recap.empty")),
                    (b) -> {
                    }
            ).bounds(x, curY, w, 18).build();
            emptyBtn.active = false;
            widgets.add(emptyBtn);
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            final int idx = i;
            DeathRecapEntry e = entries.get(i);

            String timeStr = formatDeathTime(e.timestamp());
            String label = "§c☠ §f" + LocalizationManager.get(
                    "gui.resistancedlc.panel.death_recap.summary",
                    e.x(), e.y(), e.z(), timeStr);
            if (label.length() > 55) label = label.substring(0, 53) + "…";

            Button row = Button.builder(Component.literal(label), (b) -> {
                    })
                    .bounds(x, curY, w - 25, 18).build();
            row.active = false;
            widgets.add(row);

            boolean isOpen = (activeDeathRecapIndex == idx);
            String arrow = isOpen ? "▲" : "▼";
            widgets.add(Button.builder(Component.literal(arrow), (b) -> {
                if (activeDeathRecapIndex == idx) {
                    activeDeathRecapIndex = -1;
                } else {
                    activeDeathRecapIndex = idx;
                }
                Minecraft.getInstance().execute(this::rebuildDeathRecapPanel);
            }).bounds(x + w - 22, curY, 22, 18).build());

            curY += rowH + 2;

            if (isOpen) {
                curY = buildDeathRecapDetails(widgets, x, curY, w, e);
                curY += rowGap;
            }
        }

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.death_recap.clear")),
                (b) -> {
                    DeathRecapManager.clearAll();
                    activeDeathRecapIndex = -1;
                    Minecraft.getInstance().execute(this::rebuildDeathRecapPanel);
                }
        ).bounds(x, curY, w, 20).build());
    }

    private int buildDeathRecapDetails(List<AbstractWidget> widgets, int x, int y, int w,
                                       DeathRecapEntry e) {
        int curY = y;

        widgets.add(labelRow(LocalizationManager.get("gui.resistancedlc.panel.death_recap.reason"),
                e.reason(), x, curY, w));
        curY += 18;

        String killer = (e.killer() == null || e.killer().isEmpty())
                ? LocalizationManager.get("gui.resistancedlc.panel.death_recap.unknown")
                : e.killer();
        widgets.add(labelRow(LocalizationManager.get("gui.resistancedlc.panel.death_recap.killer"),
                killer, x, curY, w));
        curY += 18;

        widgets.add(labelRow(LocalizationManager.get("gui.resistancedlc.panel.death_recap.coords"),
                e.x() + ", " + e.y() + ", " + e.z(), x, curY, w));
        curY += 18;

        widgets.add(labelRow(LocalizationManager.get("gui.resistancedlc.panel.death_recap.dimension"),
                e.dimension(), x, curY, w));
        curY += 18;

        widgets.add(labelRow(LocalizationManager.get("gui.resistancedlc.panel.death_recap.time"),
                formatDeathTime(e.timestamp()), x, curY, w));
        curY += 18 + 4;

        Button armorHeader = Button.builder(
                Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.death_recap.armor") + ":"),
                (b) -> {
                }
        ).bounds(x, curY, w, 16).build();
        armorHeader.active = false;
        widgets.add(armorHeader);
        curY += 16 + 2;

        curY = addArmorSlot(widgets, x, curY, w,
                LocalizationManager.get("gui.resistancedlc.panel.death_recap.armor_head"),
                e.armorHead());
        curY = addArmorSlot(widgets, x, curY, w,
                LocalizationManager.get("gui.resistancedlc.panel.death_recap.armor_chest"),
                e.armorChest());
        curY = addArmorSlot(widgets, x, curY, w,
                LocalizationManager.get("gui.resistancedlc.panel.death_recap.armor_legs"),
                e.armorLegs());
        curY = addArmorSlot(widgets, x, curY, w,
                LocalizationManager.get("gui.resistancedlc.panel.death_recap.armor_feet"),
                e.armorFeet());
        curY = addArmorSlot(widgets, x, curY, w,
                LocalizationManager.get("gui.resistancedlc.panel.death_recap.armor_offhand"),
                e.armorOffhand());

        return curY;
    }

    private AbstractWidget labelRow(String key, String value, int x, int y, int w) {
        String text = "§e" + key + ": §f" + (value == null ? "" : value);
        Button btn = Button.builder(Component.literal(text), (b) -> {
                })
                .bounds(x, y, w, 16).build();
        btn.active = false;
        return btn;
    }

    private int addArmorSlot(List<AbstractWidget> widgets, int x, int y, int w,
                             String slotName, String slotData) {
        int labelW = 80;
        int rowH = 20;

        Button rowBg = Button.builder(Component.empty(), (b) -> {
                })
                .bounds(x, y, w, rowH - 2).build();
        rowBg.active = false;
        widgets.add(rowBg);

        widgets.add(new ArmorTextWidget(x + 4, y + 5,
                Component.literal("§7" + slotName + ":")));

        if (slotData == null || slotData.isEmpty()) {
            widgets.add(new ArmorTextWidget(x + labelW + 20, y + 5,
                    Component.literal("§8" + LocalizationManager.get(
                            "gui.resistancedlc.panel.death_recap.armor_empty"))));
            return y + rowH;
        }

        String itemId;
        String customName = "";
        int sep = slotData.indexOf('|');
        if (sep >= 0) {
            itemId = slotData.substring(0, sep);
            customName = slotData.substring(sep + 1);
        } else {
            itemId = slotData;
        }

        ItemStack stack = itemIdToStack(itemId);
        if (!stack.isEmpty()) {
            widgets.add(new ArmorIconWidget(x + labelW, y + 2, stack));
        }

        String name;
        boolean isCustom = false;
        if (customName != null && !customName.isEmpty()) {
            name = customName;
            isCustom = true;
        } else if (!stack.isEmpty()) {
            name = stack.getHoverName().getString();
        } else {
            name = itemId;
        }

        if (name.length() > 40) name = name.substring(0, 38) + "…";

        String prefix = isCustom ? "§b" : "§f";
        widgets.add(new ArmorTextWidget(x + labelW + 22, y + 5,
                Component.literal(prefix + name)));

        return y + rowH;
    }

    private ItemStack itemIdToStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) return ItemStack.EMPTY;
        try {
            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(itemId);
            if (id == null) return ItemStack.EMPTY;

            net.minecraft.world.item.Item item =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);

            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item);
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private String formatDeathTime(long ts) {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.ROOT);
        return sdf.format(new java.util.Date(ts));
    }

    private void rebuildDeathRecapPanel() {
        AccordionItem item = findItemById("death_recap");
        if (item == null || !item.expanded) return;

        clearPanelWidgets(item);
        item.contentHeight = calcDeathRecapHeight();
        contentScroll = 0;
        buildPanelWidgets(item);
        updateWidgetsVisibility();
    }

    // ===================== FREELOOK =====================
    private void buildFreelookPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String keyName = KeyBindings.freelookKey != null
                ? KeyBindings.freelookKey.getTranslatedKeyMessage().getString() : "LCTRL";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 17
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", keyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 17;
                    b.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.freelook.sensitivity",
                        ModConfig.freelookSensitivity)),
                (ModConfig.freelookSensitivity - 0.1f) / 2.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.freelook.sensitivity",
                        ModConfig.freelookSensitivity)));
            }

            @Override
            protected void applyValue() {
                ModConfig.freelookSensitivity = 0.1f + (float) (this.value * 2.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.freelook.distance",
                        ModConfig.freelookDistance)),
                (ModConfig.freelookDistance - 2.0f) / 6.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.freelook.distance",
                        ModConfig.freelookDistance)));
            }

            @Override
            protected void applyValue() {
                ModConfig.freelookDistance = 2.0f + (float) (this.value * 6.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        Button warnBtn = Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.freelook.warning")),
                (b) -> {
                }
        ).bounds(x, curY, w, 18).build();
        warnBtn.active = false;
        widgets.add(warnBtn);
    }

    // ===================== TIME CHANGE =====================
    private void buildTimeChangePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.time_change.value",
                        (int) ModConfig.timeChangeValue,
                        TimeChangeManager.formatTime(ModConfig.timeChangeValue))),
                (ModConfig.timeChangeValue - 0L) / 24000.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.time_change.value",
                        (int) ModConfig.timeChangeValue,
                        TimeChangeManager.formatTime(ModConfig.timeChangeValue))));
            }

            @Override
            protected void applyValue() {
                ModConfig.timeChangeValue = (long) (this.value * 24000.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        String[][] presets = {
                {"Sunrise",  "23000"},
                {"Day",      "1000"},
                {"Noon",     "6000"},
                {"Sunset",   "12000"},
                {"Night",    "13000"},
                {"Midnight", "18000"}
        };

        int btnW = (w - 5 * 3) / 6;
        if (btnW < 30) btnW = 30;

        for (int i = 0; i < presets.length; i++) {
            final String presetKey = presets[i][0];
            final long presetValue = Long.parseLong(presets[i][1]);

            String label = LocalizationManager.get(
                    "gui.resistancedlc.panel.time_change.preset_" + presetKey.toLowerCase());

            Button presetBtn = Button.builder(
                    Component.literal(label),
                    (b) -> {
                        ModConfig.timeChangeValue = presetValue;
                        ConfigManager.save();
                        Minecraft.getInstance().execute(this::rebuildTimeChangePanel);
                    }
            ).bounds(x + i * (btnW + 3), curY, btnW, 20).build();

            presetBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(TimeChangeManager.formatTime(presetValue))));

            widgets.add(presetBtn);
        }
    }

    private void rebuildTimeChangePanel() {
        rebuildPanelTwoPass("time_change");
    }

    // ===================== WEATHER CHANGE =====================
    private void buildWeatherChangePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String[] modeKeys = {
                "gui.resistancedlc.panel.weather_change.mode_clear",
                "gui.resistancedlc.panel.weather_change.mode_rain",
                "gui.resistancedlc.panel.weather_change.mode_thunder",
                "gui.resistancedlc.panel.weather_change.mode_snow",
                "gui.resistancedlc.panel.weather_change.mode_overcast"
        };

        int btnW = (w - 2 * 3) / 3;
        for (int i = 0; i < 3; i++) {
            final int mode = i;
            boolean active = (ModConfig.weatherChangeMode == mode);
            String label = (active ? "§a✓ " : "") + LocalizationManager.get(modeKeys[i]);
            widgets.add(Button.builder(Component.literal(label), (b) -> {
                ModConfig.weatherChangeMode = mode;
                ConfigManager.save();
                rebuildWeatherChangePanel();
            }).bounds(x + i * (btnW + 3), curY, btnW, 20).build());
        }
        curY += rowH + rowGap;

        int btnW2 = (w - 3) / 2;
        for (int i = 3; i < 5; i++) {
            final int mode = i;
            boolean active = (ModConfig.weatherChangeMode == mode);
            String label = (active ? "§a✓ " : "") + LocalizationManager.get(modeKeys[i]);
            widgets.add(Button.builder(Component.literal(label), (b) -> {
                ModConfig.weatherChangeMode = mode;
                ConfigManager.save();
                rebuildWeatherChangePanel();
            }).bounds(x + (i - 3) * (btnW2 + 3), curY, btnW2, 20).build());
        }
        curY += rowH + rowGap;

        Button noteBtn = Button.builder(
                Component.literal("§7" + LocalizationManager.get("gui.resistancedlc.panel.weather_change.note")),
                (b) -> {
                }
        ).bounds(x, curY, w, 18).build();
        noteBtn.active = false;
        widgets.add(noteBtn);
    }

    private void rebuildWeatherChangePanel() {
        rebuildPanelTwoPass("weather_change");
    }

    // ===================== JUMP CIRCLES =====================
    private void buildJumpCirclesPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String[] styles = {"circle", "hexagon", "star"};
        String[] styleNames = {
                LocalizationManager.get("gui.resistancedlc.panel.jump_circles.style_circle"),
                LocalizationManager.get("gui.resistancedlc.panel.jump_circles.style_hexagon"),
                LocalizationManager.get("gui.resistancedlc.panel.jump_circles.style_star")
        };
        int styleIdx = 0;
        for (int i = 0; i < styles.length; i++) {
            if (styles[i].equals(ModConfig.jumpCirclesStyle)) {
                styleIdx = i;
                break;
            }
        }
        final int currentStyleIdx = styleIdx;
        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.style")
                        + ": " + styleNames[currentStyleIdx]),
                (b) -> {
                    int next = (currentStyleIdx + 1) % styles.length;
                    ModConfig.jumpCirclesStyle = styles[next];
                    ConfigManager.save();
                    rebuildJumpCirclesPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.jumpCirclesColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                int alpha = ModConfig.jumpCirclesColor & 0xFF000000;
                ModConfig.jumpCirclesColor = alpha | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {
            }
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                int alpha = ModConfig.jumpCirclesColor & 0xFF000000;
                ModConfig.jumpCirclesColor = alpha | (color & 0x00FFFFFF);
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.alpha")
                        + ModConfig.jumpCirclesAlpha),
                ModConfig.jumpCirclesAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.alpha")
                        + ModConfig.jumpCirclesAlpha));
            }

            @Override
            protected void applyValue() {
                ModConfig.jumpCirclesAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.line_width")
                        + String.format(java.util.Locale.ROOT, "%.1f", ModConfig.jumpCirclesLineWidth)),
                (ModConfig.jumpCirclesLineWidth - 1.0f) / 4.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.line_width")
                        + String.format(java.util.Locale.ROOT, "%.1f", ModConfig.jumpCirclesLineWidth)));
            }

            @Override
            protected void applyValue() {
                ModConfig.jumpCirclesLineWidth = 1.0f + (float) (this.value * 4.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.scale")
                        + String.format(java.util.Locale.ROOT, "%.2f", ModConfig.jumpCirclesScale)),
                (ModConfig.jumpCirclesScale - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.scale")
                        + String.format(java.util.Locale.ROOT, "%.2f", ModConfig.jumpCirclesScale)));
            }

            @Override
            protected void applyValue() {
                ModConfig.jumpCirclesScale = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.live_time")
                        + ModConfig.jumpCirclesLiveTime + "s"),
                (ModConfig.jumpCirclesLiveTime - 1) / 4.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.live_time")
                        + ModConfig.jumpCirclesLiveTime + "s"));
            }

            @Override
            protected void applyValue() {
                ModConfig.jumpCirclesLiveTime = 1 + (int) (this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.spin_speed")
                        + String.format(java.util.Locale.ROOT, "%.2f", ModConfig.jumpCirclesSpinSpeed)),
                (ModConfig.jumpCirclesSpinSpeed + 3.0f) / 6.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.spin_speed")
                        + String.format(java.util.Locale.ROOT, "%.2f", ModConfig.jumpCirclesSpinSpeed)));
            }

            @Override
            protected void applyValue() {
                ModConfig.jumpCirclesSpinSpeed = -3.0f + (float) (this.value * 6.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.jump_circles.fade_out")), this.font)
                .pos(x, curY).selected(ModConfig.jumpCirclesFadeOut)
                .onValueChange((c, v) -> {
                    ModConfig.jumpCirclesFadeOut = v;
                    ConfigManager.save();
                })
                .build());
    }

    private void rebuildJumpCirclesPanel() {
        rebuildPanelTwoPass("jump_circles");
    }

    // ===================== CUSTOM MAIN MENU =====================
    private void buildCustomMainMenuPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_main_menu.enable_menu")),
                        this.font)
                .pos(x, curY).selected(ModConfig.customMainMenuEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.customMainMenuEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_main_menu.enable_loading")),
                        this.font)
                .pos(x, curY).selected(ModConfig.customLoadingScreenEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.customLoadingScreenEnabled = v;
                    ConfigManager.save();
                })
                .build());
    }

    // ===================== FOV =====================
    private void buildFovPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(buildFovLabel()),
                (ModConfig.fovMultiplier - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(buildFovLabel()));
            }

            @Override
            protected void applyValue() {
                ModConfig.fovMultiplier = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        int btnW = (w - 12) / 4;
        widgets.add(Button.builder(Component.literal("0.5x"), (b) -> {
            ModConfig.fovMultiplier = 0.5f;
            ConfigManager.save();
            rebuildFovPanel();
        }).bounds(x, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal("1.0x"), (b) -> {
            ModConfig.fovMultiplier = 1.0f;
            ConfigManager.save();
            rebuildFovPanel();
        }).bounds(x + btnW + 4, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal("1.5x"), (b) -> {
            ModConfig.fovMultiplier = 1.5f;
            ConfigManager.save();
            rebuildFovPanel();
        }).bounds(x + (btnW + 4) * 2, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal("2.0x"), (b) -> {
            ModConfig.fovMultiplier = 2.0f;
            ConfigManager.save();
            rebuildFovPanel();
        }).bounds(x + (btnW + 4) * 3, curY, btnW, 20).build());
    }

    private static String buildFovLabel() {
        String value = String.format(java.util.Locale.ROOT, "%.1f", ModConfig.fovMultiplier);
        return LocalizationManager.get("gui.resistancedlc.panel.fov_prefix") + " " + value + "x";
    }

    private void rebuildFovPanel() {
        rebuildPanelTwoPass("fov");
    }

    // ===================== ASPECT RATIO =====================
    private void buildAspectRatioPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.aspect_use_preset")), this.font)
                .pos(x, curY).selected(ModConfig.aspectRatioUsePreset)
                .onValueChange((c, v) -> {
                    ModConfig.aspectRatioUsePreset = v;
                    ConfigManager.save();
                    rebuildAspectRatioPanel();
                })
                .build());
        curY += rowH + rowGap;

        if (ModConfig.aspectRatioUsePreset) {
            int btnW = (w - 8) / 3;
            String[] names = {"16:9", "5:4", "4:3"};
            AspectRatioPreset[] presets = {
                    AspectRatioPreset.R16_9,
                    AspectRatioPreset.R5_4,
                    AspectRatioPreset.R4_3
            };
            for (int i = 0; i < 3; i++) {
                final AspectRatioPreset preset = presets[i];
                boolean active = (ModConfig.aspectRatioPreset == preset);
                String label = (active ? "§a✓ " : "") + names[i];
                widgets.add(Button.builder(Component.literal(label), (b) -> {
                    ModConfig.aspectRatioPreset = preset;
                    ConfigManager.save();
                    rebuildAspectRatioPanel();
                }).bounds(x + i * (btnW + 4), curY, btnW, 20).build());
            }
        } else {
            widgets.add(new AbstractSliderButton(x, curY, w, 20,
                    Component.literal(LocalizationManager.get("gui.resistancedlc.panel.aspect_factor",
                            String.format("%.2f", ModConfig.aspectRatioFactor))),
                    (ModConfig.aspectRatioFactor - 0.5f) / 1.5f
            ) {
                @Override
                protected void updateMessage() {
                    this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.aspect_factor",
                            String.format("%.2f", ModConfig.aspectRatioFactor))));
                }

                @Override
                protected void applyValue() {
                    ModConfig.aspectRatioFactor = 0.5f + (float) (this.value * 1.5f);
                    this.updateMessage();
                    ConfigManager.save();
                }
            });
        }
    }

    private void rebuildAspectRatioPanel() {
        rebuildPanelTwoPass("aspect_ratio");
    }

    // ===================== BETTER BOSSBAR =====================
    private void buildBetterBossBarPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String styleText = ModConfig.betterBossBarStyle == BossBarStyle.CLASSIC
                ? LocalizationManager.get("gui.resistancedlc.panel.bossbar_classic")
                : LocalizationManager.get("gui.resistancedlc.panel.bossbar_minimal");
        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.bossbar_style") + ": " + styleText),
                (b) -> {
                    ModConfig.betterBossBarStyle = (ModConfig.betterBossBarStyle == BossBarStyle.CLASSIC)
                            ? BossBarStyle.MINIMAL
                            : BossBarStyle.CLASSIC;
                    ConfigManager.save();
                    rebuildBetterBossBarPanel();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        int[] colors = {
                ModConfig.betterBossBarBgColor,
                ModConfig.betterBossBarEmptyColor,
                ModConfig.betterBossBarFillColor1,
                ModConfig.betterBossBarFillColor2
        };
        int halfW = (w - 4) / 2;
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            int col = i % 2;
            int row = i / 2;
            int cx = x + col * (halfW + 4);
            int cy = curY + row * (rowH - 2);

            EditBox hex = new EditBox(this.font, cx, cy, halfW - 40, 18,
                    Component.literal("#RRGGBB"));
            hex.setMaxLength(7);
            hex.setValue(String.format("#%06X", colors[i] & 0xFFFFFF));
            widgets.add(hex);

            widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")), (b) -> {
                try {
                    int newColor = 0xFF000000 | Integer.parseInt(hex.getValue().replace("#", "").trim(), 16);
                    switch (idx) {
                        case 0 -> ModConfig.betterBossBarBgColor = newColor;
                        case 1 -> ModConfig.betterBossBarEmptyColor = newColor;
                        case 2 -> ModConfig.betterBossBarFillColor1 = newColor;
                        case 3 -> ModConfig.betterBossBarFillColor2 = newColor;
                    }
                    ConfigManager.save();
                } catch (NumberFormatException ignored) {
                }
            }).bounds(cx + halfW - 38, cy, 38, 18).build());
        }
    }

    private void rebuildBetterBossBarPanel() {
        rebuildPanelTwoPass("better_bossbar");
    }

    // ===================== РЕНДЕР =====================
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        tickAnimations(delta);
        ThemeManager.updateThemeIndexFromConfig();

        hoverColumn = isMouseOverColumn(mouseX, mouseY);

        boolean anyExpanded = false;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.expanded) {
                    anyExpanded = true;
                    break;
                }
            }
            if (anyExpanded) break;
        }

        if (!anyExpanded) {
            float targetWidth = hoverColumn ? COLUMN_EXPANDED : COLUMN_COLLAPSED;
            columnWidth += (targetWidth - columnWidth) * 0.25f;
        }

        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        graphics.fill(panelX + 4, panelY + 4,
                panelX + PANEL_WIDTH + 4, panelY + PANEL_HEIGHT + 4, 0x40000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xC0000000);

        drawPanelBorders(graphics);
        graphics.text(this.font, "§l" + LocalizationManager.get("gui.resistancedlc.title"),
                panelX + 15, panelY + 11, ThemeManager.getCurrentGuiColor());

        if (globalSearchOpen || globalSearchClosing) {
            drawGlobalSearchBackground(graphics);
            if (searchField != null) {
                searchField.visible = false;
                searchField.active = false;
            }
        } else {
            drawColumn(graphics, mouseX, mouseY);
            drawContent(graphics, mouseX, mouseY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        updateWidgetsVisibility();

        drawThemeButtons(graphics, mouseX, mouseY);

        if (globalSearchOpen || globalSearchClosing) {
            drawGlobalSearchForeground(graphics, mouseX, mouseY);
        }
    }

    private static final int[][] THEMES = {
            {0xFF00FF00, 0xFFFFFFFF, 0xFF00FF00},
            {0xFF606060, 0xFFDDDDDD, 0xFF606060},
            {0xFFFFB6C1, 0xFFFFFFFF, 0xFFFFB6C1},
            {0xFF00E5FF, 0xFFFFFFFF, 0xFF00E5FF},
            {0xFFFF2020, 0xFFFFFFFF, 0xFFFF2020},
    };

    private final int[][] themeButtonRects = new int[5][3];

    private static final String[] THEME_NAMES = {
            "Vanilla", "Dark", "Candy", "Neon", "Blood"
    };

    private void drawThemeButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int themeBtnSize = 18;
        int themeBtnGap = 4;
        int themeBtnY = panelY + 6;
        int themeBtnRight = panelX + PANEL_WIDTH - 92;
        int themeBtnStartX = themeBtnRight - themeBtnSize
                - 4 * (themeBtnSize + themeBtnGap);

        int hoveredIndex = -1;

        for (int i = 0; i < THEMES.length; i++) {
            int bx = themeBtnStartX + i * (themeBtnSize + themeBtnGap);
            themeButtonRects[i][0] = bx;
            themeButtonRects[i][1] = themeBtnY;
            themeButtonRects[i][2] = themeBtnSize;

            int color = THEMES[i][0];

            boolean isHovered = mouseX >= bx && mouseX <= bx + themeBtnSize
                    && mouseY >= themeBtnY && mouseY <= themeBtnY + themeBtnSize;

            if (isHovered) {
                hoveredIndex = i;
            }

            graphics.fill(bx, themeBtnY, bx + themeBtnSize, themeBtnY + themeBtnSize,
                    0xFF000000);
            graphics.fill(bx + 1, themeBtnY + 1,
                    bx + themeBtnSize - 1, themeBtnY + themeBtnSize - 1, color);

            if (isHovered) {
                graphics.fill(bx + 1, themeBtnY + 1,
                        bx + themeBtnSize - 1, themeBtnY + themeBtnSize - 1, 0x40FFFFFF);
            }

            boolean isActive = (ModConfig.guiColor == THEMES[i][0]
                    && ModConfig.guiTextColor == THEMES[i][1]
                    && ModConfig.hudColor == THEMES[i][2]);
            int borderColor;
            if (isActive) borderColor = 0xFFFFFFFF;
            else if (isHovered) borderColor = 0xFFA0A0FF;
            else borderColor = 0xFF505050;

            graphics.fill(bx, themeBtnY, bx + themeBtnSize, themeBtnY + 1, borderColor);
            graphics.fill(bx, themeBtnY + themeBtnSize - 1,
                    bx + themeBtnSize, themeBtnY + themeBtnSize, borderColor);
            graphics.fill(bx, themeBtnY, bx + 1, themeBtnY + themeBtnSize, borderColor);
            graphics.fill(bx + themeBtnSize - 1, themeBtnY,
                    bx + themeBtnSize, themeBtnY + themeBtnSize, borderColor);
        }

        if (hoveredIndex >= 0) {
            drawTooltip(graphics, THEME_NAMES[hoveredIndex], mouseX, mouseY);
        }
    }

    private void drawTooltip(GuiGraphicsExtractor graphics, String text, int mouseX, int mouseY) {
        int w = this.font.width(text) + 8;
        int h = 14;
        int x = mouseX + 10;
        int y = mouseY + 8;

        if (x + w > this.width - 4) x = mouseX - w - 10;
        if (y + h > this.height - 4) y = mouseY - h - 10;
        if (x < 4) x = 4;
        if (y < 4) y = 4;

        int guiCol = ThemeManager.getCurrentGuiColor();
        graphics.fill(x, y, x + w, y + h, 0xF0100010);
        graphics.fill(x, y, x + w, y + 1, guiCol);
        graphics.fill(x, y + h - 1, x + w, y + h, guiCol);
        graphics.fill(x, y, x + 1, y + h, guiCol);
        graphics.fill(x + w - 1, y, x + w, y + h, guiCol);

        graphics.text(this.font, text, x + 4, y + 3, 0xFFFFFFFF, false);
    }

    private void drawPanelBorders(GuiGraphicsExtractor graphics) {
        ThemeManager.drawAnimatedBorder(
                graphics,
                panelX, panelY,
                panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT,
                2
        );
    }

    private void drawColumn(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int colLeft = panelX + 2;
        int colTop = panelY + HEADER_HEIGHT;
        int colBottom = panelY + PANEL_HEIGHT - 2;
        int colRight = panelX + (int) columnWidth;

        graphics.fill(colLeft, colTop, colRight, colBottom, 0xA0000000);
        graphics.fill(colRight - 1, colTop, colRight, colBottom, 0xFF303030);

        int iconSize = (columnWidth > 140) ? 32 : 24;
        int iconX = colLeft + 18;
        int startY = colTop + 12;
        int yStep = (columnWidth > 140) ? 58 : 48;

        for (int i = 0; i < sections.size(); i++) {
            Section section = sections.get(i);
            int iconY = startY + i * yStep;
            boolean isActive = (i == activeSectionIndex);

            int rowLeft = colLeft + 4;
            int rowRight = colRight - 6;
            int rowTop = iconY - 6;
            int rowBottom = iconY + iconSize + 6;

            boolean isHover = mouseX >= rowLeft && mouseX <= rowRight
                    && mouseY >= rowTop && mouseY <= rowBottom;

            graphics.fill(rowLeft, rowTop, rowRight, rowBottom, 0x50000000);
            graphics.fill(rowLeft, rowTop, rowRight, rowTop + 1, 0xFF404040);
            graphics.fill(rowLeft, rowBottom - 1, rowRight, rowBottom, 0xFF404040);
            graphics.fill(rowLeft, rowTop, rowLeft + 1, rowBottom, 0xFF404040);
            graphics.fill(rowRight - 1, rowTop, rowRight, rowBottom, 0xFF404040);

            int guiCol = ThemeManager.getCurrentGuiColor();
            if (isActive) {
                int bg = (guiCol & 0x00FFFFFF) | 0x60000000;
                graphics.fill(rowLeft, rowTop, rowRight, rowBottom, bg);
                graphics.fill(rowLeft, rowTop, rowLeft + 4, rowBottom, guiCol);
                graphics.fill(rowLeft, rowTop, rowRight, rowTop + 1, guiCol);
                graphics.fill(rowLeft, rowBottom - 1, rowRight, rowBottom, guiCol);
            } else if (isHover) {
                graphics.fill(rowLeft, rowTop, rowRight, rowBottom, 0x40FFFFFF);
                graphics.fill(rowLeft, rowTop, rowRight, rowTop + 1, 0x80FFFFFF);
                graphics.fill(rowLeft, rowBottom - 1, rowRight, rowBottom, 0x80FFFFFF);
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) iconX, (float) iconY);
            float scale = (iconSize == 32) ? 2.0f : 1.5f;
            graphics.pose().scale(scale, scale);
            graphics.item(section.icon, 0, 0);
            graphics.pose().popMatrix();

            if (columnWidth > 100) {
                int textX = iconX + iconSize + 10;
                int nameY = iconY + (iconSize - 8) / 2 - 6;
                int nameColor = isActive ? ThemeManager.getCurrentGuiColor() : 0xFFEEEEEE;
                graphics.text(this.font, "§l" + section.name, textX, nameY, nameColor, true);
                if (columnWidth > 140) {
                    graphics.text(this.font, "§7" + section.description,
                            textX, nameY + 12, 0xFFAAAAAA, false);
                }
            }
        }

        int searchRowHeight = iconSize + 12;
        int separatorY = colBottom - searchRowHeight - 10;
        graphics.fill(colLeft + 8, separatorY, colRight - 8, separatorY + 1, 0xFF505050);

        int searchIconX = colLeft + 18;
        int searchIconY = separatorY + 10;
        int searchRowLeft = colLeft + 4;
        int searchRowRight = colRight - 6;
        int searchRowTop = searchIconY - 6;
        int searchRowBottom = searchIconY + iconSize + 6;

        boolean searchHover = mouseX >= searchRowLeft && mouseX <= searchRowRight
                && mouseY >= searchRowTop && mouseY <= searchRowBottom;

        graphics.fill(searchRowLeft, searchRowTop, searchRowRight, searchRowBottom, 0x50000000);
        graphics.fill(searchRowLeft, searchRowTop, searchRowRight, searchRowTop + 1, 0xFF404040);
        graphics.fill(searchRowLeft, searchRowBottom - 1, searchRowRight, searchRowBottom, 0xFF404040);
        graphics.fill(searchRowLeft, searchRowTop, searchRowLeft + 1, searchRowBottom, 0xFF404040);
        graphics.fill(searchRowRight - 1, searchRowTop, searchRowRight, searchRowBottom, 0xFF404040);

        if (searchHover) {
            graphics.fill(searchRowLeft, searchRowTop, searchRowRight, searchRowBottom, 0x40FFFFFF);
            graphics.fill(searchRowLeft, searchRowTop, searchRowRight, searchRowTop + 1, 0x80FFFFFF);
            graphics.fill(searchRowLeft, searchRowBottom - 1, searchRowRight, searchRowBottom, 0x80FFFFFF);
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) searchIconX, (float) searchIconY);
        float searchScale = (iconSize == 32) ? 2.0f : 1.5f;
        graphics.pose().scale(searchScale, searchScale);
        graphics.item(new ItemStack(Items.SPYGLASS), 0, 0);
        graphics.pose().popMatrix();

        if (columnWidth > 100) {
            int searchTextX = searchIconX + iconSize + 10;
            int searchNameY = searchIconY + (iconSize - 8) / 2 - 6;
            graphics.text(this.font, "§l" + LocalizationManager.get("gui.resistancedlc.search"),
                    searchTextX, searchNameY, 0xFFEEEEEE, true);
            if (columnWidth > 140) {
                graphics.text(this.font, "§7" + LocalizationManager.get("gui.resistancedlc.search.all"),
                        searchTextX, searchNameY + 12, 0xFFAAAAAA, false);
            }
        }
    }

    private void drawContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int contentLeft = getContentLeft();
        int contentTop = getContentTop();
        int contentRight = getContentRight();
        int contentBottom = getContentBottom();

        ThemeManager.drawAnimatedBorder(
                graphics,
                contentLeft, contentTop,
                contentRight, contentBottom,
                1
        );

        Section active = sections.get(activeSectionIndex);
        graphics.text(this.font, "§l▸ " + active.name,
                contentLeft + 15, contentTop + 8, ThemeManager.getCurrentGuiColor(), true);
        graphics.text(this.font, "§7" + active.description,
                contentLeft + 15, contentTop + 22, 0xFFAAAAAA, false);

        graphics.fill(contentLeft + 15, contentTop + 36,
                contentRight - 15, contentTop + 37, 0x40FFFFFF);

        updateFilteredItems();

        int listTop = getListTop();
        int listBottom = getListBottom();
        int listLeft = contentLeft + 5;
        int listRight = contentRight - 5;

        boolean searchActive = searchField != null && !searchField.getValue().trim().isEmpty();
        if (searchActive && filteredItems.isEmpty()) {
            String noResults = "§7" + LocalizationManager.get("gui.resistancedlc.search.nothing");
            graphics.text(this.font, noResults,
                    listLeft + 15, listTop + 10, 0xFFAAAAAA, false);
            maxContentScroll = 0;
            contentScroll = 0;
            return;
        }

        updateContentMetrics();
        int itemHeight = 26;
        int gap = 4;
        int totalHeight = 0;
        for (AccordionItem item : filteredItems) {
            totalHeight += itemHeight + gap;
            if (item.expanded) totalHeight += item.contentHeight + gap;
        }
        int visibleHeight = listBottom - listTop;

        int itemY = listTop - contentScroll + (int) sectionSlideOffset;
        int drawIndex = 0;
        for (AccordionItem item : filteredItems) {
            int itemTop = itemY;
            int itemBottom = itemY + itemHeight;

            if (drawIndex > 0 && itemTop > listTop && itemTop < listBottom) {
                graphics.fill(listLeft + 10, itemTop - 1,
                        listRight - 10, itemTop, 0x30FFFFFF);
            }

            if (itemBottom > listTop && itemTop < listBottom) {
                boolean isHover = mouseX >= listLeft && mouseX <= listRight
                        && mouseY >= itemTop && mouseY <= itemBottom
                        && mouseY >= listTop && mouseY <= listBottom;

                drawAccordionItemClipped(graphics, item, listLeft, itemTop, listRight, itemHeight,
                        isHover, listTop, listBottom);
            }

            itemY += itemHeight + gap;

            int animH = getAnimatedHeight(item);
            if (animH > 0) {
                int panelTop = itemY;
                int panelBottom = itemY + animH;

                if (panelBottom > listTop && panelTop < listBottom) {
                    drawItemPanelClipped(graphics, item, listLeft - 10, panelTop,
                            listRight + 10, listTop, listBottom, animH);
                }

                itemY += animH + gap;
            }
            drawIndex++;
        }

        if (maxContentScroll > 0) {
            int barX = contentRight - 4;
            int barTop = listTop;
            int barBottom = listBottom;
            int barHeight = barBottom - barTop;

            graphics.fill(barX, barTop, barX + 3, barBottom, 0x40000000);

            float ratio = (float) visibleHeight / totalHeight;
            int thumbHeight = Math.max(15, (int) (barHeight * ratio));
            int thumbY = barTop + (int) ((float) contentScroll / maxContentScroll * (barHeight - thumbHeight));

            graphics.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, ThemeManager.getCurrentGuiColor());
        }
    }

    private void drawAccordionItemClipped(GuiGraphicsExtractor graphics, AccordionItem item,
                                          int left, int itemY, int right, int itemHeight,
                                          boolean isHover, int clipTop, int clipBottom) {
        int top = itemY;
        int bottom = itemY + itemHeight;

        if (top >= clipTop && bottom <= clipBottom) {
            drawAccordionItem(graphics, item, left - 5, itemY, right + 5, itemHeight, isHover);
            return;
        }

        int visibleTop = Math.max(top, clipTop);
        int visibleBottom = Math.min(bottom, clipBottom);

        int alpha = (int) (0xFF * sectionFadeProgress);
        int bgColor = (alpha << 24) | 0x000000;
        int borderColor = (alpha << 24) | 0x404040;
        int textColor = (alpha << 24) | 0xFFFFFF;
        int descColor = (alpha << 24) | 0xAAAAAA;

        graphics.fill(left, visibleTop, right, visibleBottom, bgColor);
        graphics.fill(left, visibleTop, right, visibleTop + 1, borderColor);
        graphics.fill(left, visibleBottom - 1, right, visibleBottom, borderColor);
        graphics.fill(left, visibleTop, left + 1, visibleBottom, borderColor);
        graphics.fill(right - 1, visibleTop, right, visibleBottom, borderColor);

        if (isHover) graphics.fill(left, visibleTop, right, visibleBottom, 0x30FFFFFF);
        if (item.expanded) graphics.fill(left, visibleTop, left + 3, visibleBottom, ThemeManager.getCurrentGuiColor());

        if (top + 8 >= clipTop && top + 8 <= clipBottom) {
            String arrow = item.expanded ? "▼" : "▶";
            graphics.text(this.font, arrow, left + 8, top + 8, ThemeManager.getCurrentGuiColor(), true);
        }
        if (top + 3 >= clipTop && top + 3 <= clipBottom) {
            graphics.text(this.font, "§l" + item.title, left + 22, top + 3, textColor, true);
        }
        if (top + 14 >= clipTop && top + 14 <= clipBottom) {
            graphics.text(this.font, "§7" + item.description, left + 22, top + 14, descColor, false);
        }
        if (top + 6 >= clipTop && top + 6 <= clipBottom) {
            boolean status = item.statusGetter.get();
            String statusText = status ? "§a[ON]" : "§7[OFF]";
            int statusWidth = this.font.width(statusText);
            graphics.text(this.font, statusText, right - statusWidth - 10, top + 6, textColor, true);
        }
    }

    private void drawItemPanelClipped(GuiGraphicsExtractor graphics, AccordionItem item,
                                      int left, int panelY, int right,
                                      int clipTop, int clipBottom, int panelHeight) {
        int top = panelY;
        int bottom = panelY + panelHeight;

        int visibleTop = Math.max(top, clipTop);
        int visibleBottom = Math.min(bottom, clipBottom);

        if (visibleTop >= visibleBottom) return;

        graphics.fill(left, visibleTop, right, visibleBottom, 0x30000000);
        if (visibleTop == top) {
            graphics.fill(left, top, right, top + 1, 0xFF505050);
        }
        if (visibleBottom == bottom) {
            graphics.fill(left, bottom - 1, right, bottom, 0xFF505050);
        }
        graphics.fill(left, visibleTop, left + 1, visibleBottom, 0xFF505050);
        graphics.fill(right - 1, visibleTop, right, visibleBottom, 0xFF505050);
    }

    private void drawAccordionItem(GuiGraphicsExtractor graphics, AccordionItem item,
                                   int contentLeft, int itemY, int contentRight, int itemHeight,
                                   boolean isHover) {
        int left = contentLeft + 5;
        int right = contentRight - 5;
        int top = itemY;
        int bottom = itemY + itemHeight;

        int alpha = (int) (0xFF * sectionFadeProgress);
        int bgColor = (alpha << 24) | 0x000000;
        int borderColor = (alpha << 24) | 0x404040;
        int textColor = (alpha << 24) | 0xFFFFFF;
        int descColor = (alpha << 24) | 0xAAAAAA;

        graphics.fill(left, top, right, bottom, bgColor);
        graphics.fill(left, top, right, top + 1, borderColor);
        graphics.fill(left, bottom - 1, right, bottom, borderColor);
        graphics.fill(left, top, left + 1, bottom, borderColor);
        graphics.fill(right - 1, top, right, bottom, borderColor);

        if (isHover) {
            graphics.fill(left, top, right, bottom, 0x40FFFFFF);
            graphics.fill(left, top, right, top + 1, 0xB0FFFFFF);
            graphics.fill(left, bottom - 1, right, bottom, 0xB0FFFFFF);
        }

        if (item.expanded) {
            graphics.fill(left, top, left + 3, bottom, ThemeManager.getCurrentGuiColor());
        } else if (isHover) {
            graphics.fill(left, top, left + 2, bottom, ThemeManager.getCurrentGuiColor());
        }

        String arrow = item.expanded ? "▼" : "▶";
        graphics.text(this.font, arrow, left + 8, top + 8, ThemeManager.getCurrentGuiColor(), true);
        graphics.text(this.font, "§l" + item.title, left + 22, top + 3, textColor, true);
        graphics.text(this.font, "§7" + item.description, left + 22, top + 14, descColor, false);

        boolean showStatus = !"hud_editor".equals(item.id)
                && !"death_recap".equals(item.id)
                && !"gui_key".equals(item.id)
                && !"config_manager".equals(item.id)
                && !"inventory_manager".equals(item.id);
        int statusWidth = 0;
        if (showStatus) {
            boolean status = item.statusGetter.get();
            String statusText = status ? "§a[ON]" : "§7[OFF]";
            statusWidth = this.font.width(statusText);
            graphics.text(this.font, statusText, right - statusWidth - 10, top + 6, textColor, true);
        }

        if (isHover && !item.expanded) {
            int arrowX = right - statusWidth - 26;
            graphics.text(this.font, "§7→", arrowX, top + 10, descColor, false);
        }

        if (isHighlightActive(item)) {
            float progress = getHighlightProgress(item);
            float pulse = 0.7f + 0.3f * (float) Math.sin(progress * Math.PI * 6);
            float fade = 1.0f - progress * 0.5f;
            int hiAlpha = (int) (0xFF * pulse * fade);

            int r = 0xFF;
            int g = 0xFF;
            int b = 0x80;
            int highlightColor = (hiAlpha << 24) | (r << 16) | (g << 8) | b;

            int thickness = 3;
            graphics.fill(left - thickness, top - thickness, right + thickness, top, highlightColor);
            graphics.fill(left - thickness, bottom, right + thickness, bottom + thickness, highlightColor);
            graphics.fill(left - thickness, top, left, bottom, highlightColor);
            graphics.fill(right, top, right + thickness, bottom, highlightColor);

            int innerColor = (Math.min(255, hiAlpha + 40) << 24) | 0xFFFFFF;
            graphics.fill(left, top, right, top + 1, innerColor);
            graphics.fill(left, bottom - 1, right, bottom, innerColor);
        }
    }

    // ===================== OVERLAY =====================
    private void drawGlobalSearchBackground(GuiGraphicsExtractor graphics) {
        int overlayX = panelX + (PANEL_WIDTH - OVERLAY_W) / 2;
        int overlayY = panelY + (PANEL_HEIGHT - OVERLAY_H) / 2;

        int alpha = (int) (0xFF * globalSearchFadeProgress);
        int bgAlpha = (int) (0xB0 * globalSearchFadeProgress);
        int panelAlpha = (int) (0xF0 * globalSearchFadeProgress);

        graphics.fill(0, 0, this.width, this.height, (bgAlpha << 24));
        graphics.fill(overlayX, overlayY, overlayX + OVERLAY_W, overlayY + OVERLAY_H,
                (panelAlpha << 24) | 0x000000);

        int borderColor = (alpha << 24) | (ThemeManager.getCurrentGuiColor() & 0x00FFFFFF);
        graphics.fill(overlayX, overlayY, overlayX + OVERLAY_W, overlayY + 2, borderColor);
        graphics.fill(overlayX, overlayY + OVERLAY_H - 2, overlayX + OVERLAY_W, overlayY + OVERLAY_H, borderColor);
        graphics.fill(overlayX, overlayY, overlayX + 2, overlayY + OVERLAY_H, borderColor);
        graphics.fill(overlayX + OVERLAY_W - 2, overlayY, overlayX + OVERLAY_W, overlayY + OVERLAY_H, borderColor);

        graphics.text(this.font,
                "§l🔍 " + LocalizationManager.get("gui.resistancedlc.search.global"),
                overlayX + 15, overlayY + 15, borderColor, true);
    }

    private void drawGlobalSearchForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int overlayX = panelX + (PANEL_WIDTH - OVERLAY_W) / 2;
        int overlayY = panelY + (PANEL_HEIGHT - OVERLAY_H) / 2;

        int alpha = (int) (0xFF * globalSearchFadeProgress);

        graphics.text(this.font,
                "§7" + LocalizationManager.get("gui.resistancedlc.search.found", globalSearchResults.size()),
                overlayX + 15, overlayY + 70, (alpha << 24) | 0xAAAAAA, false);

        globalSearchListX = overlayX + 15;
        globalSearchListY = overlayY + 90;
        globalSearchListW = OVERLAY_W - 30;
        globalSearchRowH = 22;

        globalSearchHovered = -1;

        String query = globalSearchField != null ? globalSearchField.getValue().trim() : "";

        if (query.isEmpty()) {
            graphics.text(this.font,
                    "§7" + LocalizationManager.get("gui.resistancedlc.search.start"),
                    globalSearchListX + 5, globalSearchListY + 10, (alpha << 24) | 0x888888, false);
        } else if (globalSearchResults.isEmpty()) {
            graphics.text(this.font,
                    "§7" + LocalizationManager.get("gui.resistancedlc.search.nothing"),
                    globalSearchListX + 5, globalSearchListY + 10, (alpha << 24) | 0x888888, false);
        } else {
            for (int i = 0; i < globalSearchResults.size(); i++) {
                String[] r = globalSearchResults.get(i);
                int rowY = globalSearchListY + i * globalSearchRowH;
                boolean hovered = mouseX >= globalSearchListX && mouseX <= globalSearchListX + globalSearchListW
                        && mouseY >= rowY && mouseY < rowY + globalSearchRowH;

                if (hovered) {
                    globalSearchHovered = i;
                    graphics.fill(globalSearchListX, rowY, globalSearchListX + globalSearchListW,
                            rowY + globalSearchRowH, (int) (0x40 * globalSearchFadeProgress) << 24);
                    graphics.fill(globalSearchListX, rowY, globalSearchListX + 3,
                            rowY + globalSearchRowH, (alpha << 24) | (ThemeManager.getCurrentGuiColor() & 0x00FFFFFF));
                }

                String sectionName = r[0];
                for (Section s : sections) {
                    if (s.id.equals(r[0])) {
                        sectionName = s.name;
                        break;
                    }
                }

                String text = "§f" + r[2] + " §7· §e" + sectionName;
                graphics.text(this.font, text, globalSearchListX + 10, rowY + 6, (alpha << 24) | 0xFFFFFF, false);

                String desc = r[3];
                if (desc.length() > 40) desc = desc.substring(0, 40) + "...";
                graphics.text(this.font, "§8" + desc,
                        globalSearchListX + 10 + this.font.width(text) + 8, rowY + 6, (alpha << 24) | 0x888888, false);
            }
        }

        graphics.text(this.font,
                "§7" + LocalizationManager.get("gui.resistancedlc.search.click"),
                overlayX + 15, overlayY + OVERLAY_H - 18, (alpha << 24) | 0xAAAAAA, false);
    }

    // ===================== СКРОЛЛ =====================
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        if (globalSearchOpen) return true;

        int contentLeft = getContentLeft();
        int contentTop = getContentTop();
        int contentRight = getContentRight();
        int contentBottom = getContentBottom();

        boolean overContent = mouseX >= contentLeft && mouseX <= contentRight
                && mouseY >= contentTop && mouseY <= contentBottom;

        if (overContent && maxContentScroll > 0) {
            int oldScroll = contentScroll;
            contentScroll -= (int) (verticalAmount * 20);
            if (contentScroll < 0) contentScroll = 0;
            if (contentScroll > maxContentScroll) contentScroll = maxContentScroll;

            int delta = oldScroll - contentScroll;
            if (delta != 0) {
                for (List<AbstractWidget> widgets : panelWidgets.values()) {
                    for (AbstractWidget w : widgets) {
                        w.setY(w.getY() + delta);
                    }
                }
                updateWidgetsVisibility();
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ===================== КЛИКИ =====================
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        for (int i = 0; i < themeButtonRects.length; i++) {
            int[] r = themeButtonRects[i];
            if (r[2] > 0
                    && mouseX >= r[0] && mouseX <= r[0] + r[2]
                    && mouseY >= r[1] && mouseY <= r[1] + r[2]) {
                ModConfig.guiColor = THEMES[i][0];
                ModConfig.guiTextColor = THEMES[i][1];
                ModConfig.hudColor = THEMES[i][2];
                ThemeManager.setThemeIndex(i);
                ConfigManager.save();
                return true;
            }
        }

        if (globalSearchOpen) {
            if (globalSearchHovered >= 0 && globalSearchHovered < globalSearchResults.size()) {
                String[] r = globalSearchResults.get(globalSearchHovered);
                jumpToFunction(r[0], r[1]);
                return true;
            }

            for (AbstractWidget w : globalSearchWidgets) {
                if (!w.visible || !w.active) continue;
                if (w.isMouseOver(mouseX, mouseY)) {
                    w.mouseClicked(event, isDoubleClick);
                    return true;
                }
            }

            int overlayX = panelX + (PANEL_WIDTH - OVERLAY_W) / 2;
            int overlayY = panelY + (PANEL_HEIGHT - OVERLAY_H) / 2;
            boolean inOverlay = mouseX >= overlayX && mouseX <= overlayX + OVERLAY_W
                    && mouseY >= overlayY && mouseY <= overlayY + OVERLAY_H;
            if (!inOverlay) {
                closeGlobalSearch();
            }
            return true;
        }

        int colLeft = panelX + 2;
        int colTop = panelY + HEADER_HEIGHT;
        int colRight = panelX + (int) columnWidth;
        int colBottom = panelY + PANEL_HEIGHT - 2;

        boolean inColumn = mouseX >= colLeft && mouseX <= colRight
                && mouseY >= colTop && mouseY <= colBottom;

        if (inColumn) {
            int iconSize = (columnWidth > 140) ? 32 : 24;
            int startY = colTop + 12;
            int yStep = (columnWidth > 140) ? 58 : 48;

            for (int i = 0; i < sections.size(); i++) {
                int iconY = startY + i * yStep;
                int rowLeft = colLeft + 4;
                int rowRight = colRight - 6;
                int rowTop = iconY - 6;
                int rowBottom = iconY + iconSize + 6;

                if (mouseX >= rowLeft && mouseX <= rowRight
                        && mouseY >= rowTop && mouseY <= rowBottom) {
                    clearAllPanelWidgets();
                    for (Section s : sections) {
                        for (AccordionItem it : s.items) {
                            it.expanded = false;
                            it.expandProgress = 0.0f;
                        }
                    }
                    activeSectionIndex = i;
                    contentScroll = 0;
                    sectionFadeProgress = 0.0f;
                    sectionSlideOffset = SECTION_SLIDE_DISTANCE;
                    if (searchField != null) {
                        restoringSearch = true;
                        searchField.setValue("");
                        restoringSearch = false;
                    }
                    updateFilteredItems();
                    return true;
                }
            }

            int searchRowHeight = iconSize + 12;
            int separatorY = colBottom - searchRowHeight - 10;
            int searchIconY = separatorY + 10;
            int searchRowLeft = colLeft + 4;
            int searchRowRight = colRight - 6;
            int searchRowTop = searchIconY - 6;
            int searchRowBottom = searchIconY + iconSize + 6;

            if (mouseX >= searchRowLeft && mouseX <= searchRowRight
                    && mouseY >= searchRowTop && mouseY <= searchRowBottom) {
                openGlobalSearch();
                return true;
            }
            return true;
        }

        if (handleContentClick(mouseX, mouseY, event.button())) {
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean handleContentClick(double mouseX, double mouseY, int button) {
        int contentLeft = getContentLeft();
        int contentTop = getContentTop();
        int contentRight = getContentRight();
        int contentBottom = getContentBottom();

        if (mouseX < contentLeft || mouseX > contentRight) return false;
        if (mouseY < contentTop || mouseY > contentBottom) return false;

        updateFilteredItems();

        int checkY = contentTop + 42 - contentScroll;
        int itemHeight = 26;
        int gap = 4;

        for (AccordionItem item : filteredItems) {
            checkY += itemHeight + gap;
            int animH = getAnimatedHeight(item);
            if (animH > 0) {
                int panelTop = checkY;
                int panelBottom = checkY + animH;
                if (mouseX >= contentLeft + 15 && mouseX <= contentRight - 15
                        && mouseY >= panelTop && mouseY <= panelBottom) {
                    return false;
                }
                checkY += animH + gap;
            }
        }

        int itemY = contentTop + 42 - contentScroll;

        for (AccordionItem item : filteredItems) {
            int top = itemY;
            int bottom = itemY + itemHeight;

            if (mouseX >= contentLeft + 5 && mouseX <= contentRight - 5
                    && mouseY >= top && mouseY <= bottom) {

                if (bottom < contentTop + 42 || top > contentBottom) {
                    itemY += itemHeight + gap;
                    int animH = getAnimatedHeight(item);
                    if (animH > 0) itemY += animH + gap;
                    continue;
                }

                if ("hud_editor".equals(item.id)) {
                    item.toggler.run();
                    return true;
                }

                if (button == 1) {
                    item.toggler.run();
                } else {
                    boolean wasExpanded = item.expanded;
                    int savedScroll = contentScroll;

                    closeAllExcept(null);

                    if (!wasExpanded) {
                        item.expanded = true;
                    }

                    // ПРОХОД 1: измеряем
                    if (!wasExpanded) {
                        buildPanelWidgets(item);
                    }
                    updateContentMetrics();

                    // ПРОХОД 2: пересоздаём с правильными Y
                    clearPanelWidgets(item);
                    buildPanelWidgets(item);
                    updateContentMetrics();

                    contentScroll = Math.min(savedScroll, maxContentScroll);
                    if (contentScroll < 0) contentScroll = 0;
                    updateWidgetsVisibility();
                }
                return true;
            }

            itemY += itemHeight + gap;
            int animH = getAnimatedHeight(item);
            if (animH > 0) {
                itemY += animH + gap;
            }
        }

        return false;
    }

    // ===================== КЛАВИШИ =====================
    @Override
    public boolean keyPressed(KeyEvent event) {
        // Перехват бинда на уровне GUI (страховка)
        if (ModConfig.isBindingKey) {
            int key = event.key();
            int target = ModConfig.bindingTarget;

            ModConfig.isBindingKey = false;
            ModConfig.bindingTarget = 0;

            if (key != GLFW.GLFW_KEY_ESCAPE) {
                switch (target) {
                    case 0 -> KeyBindings.setKey(key);
                    case 1 -> KeyBindings.setZoomKey(key);
                    case 2 -> KeyBindings.setTapeMouseKey(key);
                    case 3 -> KeyBindings.setAutoSwapKey(key);
                    case 4 -> KeyBindings.setCustomHitSoundsKey(key);
                    case 5 -> KeyBindings.setFastExpKey(key);
                    case 6 -> KeyBindings.setShiftTapKey(key);
                    case 7 -> KeyBindings.setComboKey(key);
                    case 8 -> KeyBindings.setEffectWarningsKey(key);
                    case 9 -> KeyBindings.setWaypointsKey(key);
                    case 10 -> KeyBindings.setTotemLogKey(key);
                    case 11 -> KeyBindings.setPickupLogKey(key);
                    case 12 -> KeyBindings.setMacro1Key(key);
                    case 13 -> KeyBindings.setMacro2Key(key);
                    case 14 -> KeyBindings.setMacro3Key(key);
                    case 15 -> KeyBindings.setMacro4Key(key);
                    case 16 -> KeyBindings.setMacro5Key(key);
                    case 17 -> KeyBindings.setFreelookKey(key);
                }
            }

            Minecraft.getInstance().execute(this::refreshAfterKeybind);
            return true;
        }

        if (globalSearchOpen) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                closeGlobalSearch();
                return true;
            }
            return super.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    private boolean isMouseOverColumn(double mouseX, double mouseY) {
        int colLeft = panelX + 2;
        int colTop = panelY + HEADER_HEIGHT;
        int colRight = panelX + Math.max(COLUMN_COLLAPSED, (int) columnWidth) + 20;
        int colBottom = panelY + PANEL_HEIGHT - 2;
        return mouseX >= colLeft && mouseX <= colRight
                && mouseY >= colTop && mouseY <= colBottom;
    }
    // ===================== GUI KEY =====================
    private void buildGuiKeyPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        String keyName = KeyBindings.openGuiKey != null
                ? KeyBindings.openGuiKey.getTranslatedKeyMessage().getString() : "G";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 0
                        ? LocalizationManager.get("gui.resistancedlc.panel.press_key")
                        : LocalizationManager.get("gui.resistancedlc.panel.key", keyName)),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 0;
                    b.setMessage(Component.literal(
                            LocalizationManager.get("gui.resistancedlc.panel.press_key")));
                }
        ).bounds(x, y, w, 20).build());
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ===================== ВИДЖЕТЫ-КЛАССЫ =====================
    private static class ArmorIconWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final ItemStack stack;

        public ArmorIconWidget(int x, int y, ItemStack stack) {
            super(x, y, 16, 16, Component.empty());
            this.stack = stack;
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            if (!stack.isEmpty()) {
                graphics.item(stack, getX(), getY());
            }
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private static class ArmorTextWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final Component text;

        public ArmorTextWidget(int x, int y, Component text) {
            super(x, y, 200, 9, Component.empty());
            this.text = text;
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.text(
                    net.minecraft.client.Minecraft.getInstance().font,
                    text, getX(), getY(), 0xFFFFFFFF, false);
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private static class InventorySlotWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final ItemStack stack;

        public InventorySlotWidget(int x, int y, ItemStack stack) {
            super(x, y, 20, 20, Component.empty());
            this.stack = (stack == null) ? ItemStack.EMPTY : stack;
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int bx = getX(), by = getY();
            graphics.fill(bx, by, bx + 20, by + 20, 0xFF1A1A1A);
            graphics.fill(bx, by, bx + 20, by + 1, 0xFF555555);
            graphics.fill(bx, by + 19, bx + 20, by + 20, 0xFF555555);
            graphics.fill(bx, by, bx + 1, by + 20, 0xFF555555);
            graphics.fill(bx + 19, by, bx + 20, by + 20, 0xFF555555);

            if (!stack.isEmpty()) {
                graphics.item(stack, bx + 2, by + 2);
                graphics.itemDecorations(
                        net.minecraft.client.Minecraft.getInstance().font,
                        stack, bx + 2, by + 2);
            }
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private static class SlotLabelWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final Component text;

        public SlotLabelWidget(int x, int y, Component text, int width) {
            super(x, y, width, 9, Component.empty());
            this.text = text;
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            graphics.text(
                    net.minecraft.client.Minecraft.getInstance().font,
                    text, getX(), getY(), 0xFFFFFFFF, false);
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }

    private static class ArmorPreviewWidget extends net.minecraft.client.gui.components.AbstractWidget {
        private final InventorySnapshot snap;

        public ArmorPreviewWidget(int x, int y, int size, InventorySnapshot snap) {
            super(x, y, size, size, Component.empty());
            this.snap = snap;
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack origHead = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy();
            ItemStack origChest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).copy();
            ItemStack origLegs = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).copy();
            ItemStack origFeet = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).copy();
            ItemStack origOffhand = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).copy();

            try {
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                        snap.getSlot(InventorySnapshot.IDX_HEAD).copy());
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                        snap.getSlot(InventorySnapshot.IDX_CHEST).copy());
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                        snap.getSlot(InventorySnapshot.IDX_LEGS).copy());
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                        snap.getSlot(InventorySnapshot.IDX_FEET).copy());
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
                        snap.getSlot(InventorySnapshot.IDX_OFFHAND).copy());

                int x1 = getX();
                int y1 = getY();
                int x2 = getX() + getWidth();
                int y2 = getY() + getHeight();

                net.minecraft.client.gui.screens.inventory.InventoryScreen
                        .extractEntityInInventoryFollowsMouse(
                                graphics, x1, y1, x2, y2,
                                30,
                                0.0625F,
                                (float) mouseX, (float) mouseY,
                                mc.player
                        );
            } finally {
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, origHead);
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, origChest);
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, origLegs);
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, origFeet);
                mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, origOffhand);
            }
        }

        @Override
        protected void updateWidgetNarration(
                net.minecraft.client.gui.narration.NarrationElementOutput narration) {
        }
    }
    // ===================== CLOUD CONTROL =====================
    private void buildCloudControlPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hex")));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.cloudColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.apply")),
                (b) -> {
                    try {
                        ModConfig.cloudColor = 0xFF000000 | Integer.parseInt(
                                hexField.getValue().replace("#", "").trim(), 16);
                        ConfigManager.save();
                    } catch (Exception ignored) {}
                }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.cloudColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.cloudAlpha)),
                ModConfig.cloudAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.cloudAlpha)));
            }
            @Override
            protected void applyValue() {
                ModConfig.cloudAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.cloud.height", ModConfig.cloudHeight)),
                (ModConfig.cloudHeight - 64.0f) / 192.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.cloud.height", ModConfig.cloudHeight)));
            }
            @Override
            protected void applyValue() {
                ModConfig.cloudHeight = 64.0f + (float) (this.value * 192.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.cloud.speed", ModConfig.cloudSpeed)),
                (ModConfig.cloudSpeed - 0.1f) / 4.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.cloud.speed", ModConfig.cloudSpeed)));
            }
            @Override
            protected void applyValue() {
                ModConfig.cloudSpeed = 0.1f + (float) (this.value * 4.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    // ===================== TRAILS =====================
    private void buildTrailsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.trails.color1"),
                () -> ModConfig.trailsColor1,
                v -> ModConfig.trailsColor1 = v);
        curY += 34 + rowGap;

        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.trails.color2"),
                () -> ModConfig.trailsColor2,
                v -> ModConfig.trailsColor2 = v);
        curY += 34 + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.trails.gradient")), this.font)
                .pos(x, curY).selected(ModConfig.trailsGradient)
                .onValueChange((c, v) -> {
                    ModConfig.trailsGradient = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.trails.length", ModConfig.trailsLength)),
                (ModConfig.trailsLength - 0.5f) / 9.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.trails.length", ModConfig.trailsLength)));
            }
            @Override
            protected void applyValue() {
                ModConfig.trailsLength = 0.5f + (float) (this.value * 9.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.trails.width", ModConfig.trailsWidth)),
                (ModConfig.trailsWidth - 0.05f) / 1.95f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.trails.width", ModConfig.trailsWidth)));
            }
            @Override
            protected void applyValue() {
                ModConfig.trailsWidth = 0.05f + (float) (this.value * 1.95f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.trails.alpha", ModConfig.trailsAlpha)),
                ModConfig.trailsAlpha
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.trails.alpha", ModConfig.trailsAlpha)));
            }
            @Override
            protected void applyValue() {
                ModConfig.trailsAlpha = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    // ===================== HIT PARTICLES =====================
    private void buildHitParticlesPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String typeText = switch (ModConfig.hitParticlesType) {
            case "crystal" -> LocalizationManager.get("gui.resistancedlc.panel.hitparticles.type_crystal");
            case "snowflake" -> LocalizationManager.get("gui.resistancedlc.panel.hitparticles.type_snowflake");
            case "pyramid" -> LocalizationManager.get("gui.resistancedlc.panel.hitparticles.type_pyramid");
            default -> LocalizationManager.get("gui.resistancedlc.panel.hitparticles.type_star");
        };
        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hitparticles.type") + ": " + typeText),
                (b) -> {
                    ModConfig.hitParticlesType = switch (ModConfig.hitParticlesType) {
                        case "star" -> "crystal";
                        case "crystal" -> "snowflake";
                        case "snowflake" -> "pyramid";
                        default -> "star";
                    };
                    ConfigManager.save();
                    rebuildPanelTwoPass("hit_particles");
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.hitparticles.color"),
                () -> ModConfig.hitParticlesColor,
                v -> ModConfig.hitParticlesColor = v);
        curY += 34 + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hitparticles.count", ModConfig.hitParticlesCount)),
                (ModConfig.hitParticlesCount - 1) / 31.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hitparticles.count", ModConfig.hitParticlesCount)));
            }
            @Override
            protected void applyValue() {
                ModConfig.hitParticlesCount = 1 + (int) (this.value * 31);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hitparticles.size", ModConfig.hitParticlesSize)),
                (ModConfig.hitParticlesSize - 0.3f) / 2.7f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hitparticles.size", ModConfig.hitParticlesSize)));
            }
            @Override
            protected void applyValue() {
                ModConfig.hitParticlesSize = 0.3f + (float) (this.value * 2.7f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hitparticles.speed", ModConfig.hitParticlesSpeed)),
                (ModConfig.hitParticlesSpeed - 0.2f) / 2.8f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hitparticles.speed", ModConfig.hitParticlesSpeed)));
            }
            @Override
            protected void applyValue() {
                ModConfig.hitParticlesSpeed = 0.2f + (float) (this.value * 2.8f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }
    // ===================== CLEAR LIQUIDS =====================
    private void buildClearLiquidsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.clear_liquids.water")),
                        this.font)
                .pos(x, curY).selected(ModConfig.clearWaterEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.clearWaterEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.clear_liquids.lava")),
                        this.font)
                .pos(x, curY).selected(ModConfig.clearLavaEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.clearLavaEnabled = v;
                    ConfigManager.save();
                })
                .build());
    }
    // ===================== ATMOSPHERE =====================
    private void buildAtmospherePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        // Тип
        String typeText = "snow".equals(ModConfig.atmosphereType)
                ? LocalizationManager.get("gui.resistancedlc.panel.atmosphere.type_snow")
                : LocalizationManager.get("gui.resistancedlc.panel.atmosphere.type_star");
        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.type") + ": " + typeText),
                (b) -> {
                    ModConfig.atmosphereType = "snow".equals(ModConfig.atmosphereType) ? "star" : "snow";
                    ConfigManager.save();
                    rebuildPanelTwoPass("atmosphere");
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        // Цвет
        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.atmosphere.color"),
                () -> ModConfig.atmosphereColor,
                v -> ModConfig.atmosphereColor = v);
        curY += 34 + rowGap;

        // Альфа
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.atmosphereAlpha)),
                ModConfig.atmosphereAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.atmosphereAlpha)));
            }
            @Override
            protected void applyValue() {
                ModConfig.atmosphereAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Радиус
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.range", ModConfig.atmosphereRange)),
                (ModConfig.atmosphereRange - 4.0f) / 28.0f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.range", ModConfig.atmosphereRange)));
            }
            @Override
            protected void applyValue() {
                ModConfig.atmosphereRange = 4.0f + (float) (this.value * 28.0f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Количество
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.count", ModConfig.atmosphereCount)),
                (ModConfig.atmosphereCount - 10) / 490.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.count", ModConfig.atmosphereCount)));
            }
            @Override
            protected void applyValue() {
                ModConfig.atmosphereCount = 10 + (int) (this.value * 490);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Скорость падения
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.fall_speed", ModConfig.atmosphereFallSpeed)),
                (ModConfig.atmosphereFallSpeed - 0.1f) / 2.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.fall_speed", ModConfig.atmosphereFallSpeed)));
            }
            @Override
            protected void applyValue() {
                ModConfig.atmosphereFallSpeed = 0.1f + (float) (this.value * 2.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Размер
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.size", ModConfig.atmosphereSize)),
                (ModConfig.atmosphereSize - 0.3f) / 2.7f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.size", ModConfig.atmosphereSize)));
            }
            @Override
            protected void applyValue() {
                ModConfig.atmosphereSize = 0.3f + (float) (this.value * 2.7f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Вращение
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.rotate")), this.font)
                .pos(x, curY).selected(ModConfig.atmosphereRotate)
                .onValueChange((c, v) -> {
                    ModConfig.atmosphereRotate = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        // Только на улице
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.atmosphere.outside")), this.font)
                .pos(x, curY).selected(ModConfig.atmosphereOnlyOutside)
                .onValueChange((c, v) -> {
                    ModConfig.atmosphereOnlyOutside = v;
                    ConfigManager.save();
                })
                .build());
    }
    // ===================== CUSTOM FOG =====================
    private void buildCustomFogPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        // Режим
        String modeText;
        switch (ModConfig.customFogMode) {
            case 1 -> modeText = LocalizationManager.get("gui.resistancedlc.panel.custom_fog.mode_atmospheric");
            case 2 -> modeText = LocalizationManager.get("gui.resistancedlc.panel.custom_fog.mode_liquid");
            default -> modeText = LocalizationManager.get("gui.resistancedlc.panel.custom_fog.mode_always");
        }
        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_fog.mode") + ": " + modeText),
                (b) -> {
                    ModConfig.customFogMode = (ModConfig.customFogMode + 1) % 3;
                    ConfigManager.save();
                    rebuildPanelTwoPass("custom_fog");
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        // Цвет
        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.custom_fog.color"),
                () -> ModConfig.customFogColor,
                v -> ModConfig.customFogColor = v);
        curY += 34 + rowGap;

        // Start
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_fog.start", ModConfig.customFogStart)),
                ModConfig.customFogStart
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_fog.start", ModConfig.customFogStart)));
            }
            @Override
            protected void applyValue() {
                ModConfig.customFogStart = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // End
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_fog.end", ModConfig.customFogEnd)),
                ModConfig.customFogEnd
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_fog.end", ModConfig.customFogEnd)));
            }
            @Override
            protected void applyValue() {
                ModConfig.customFogEnd = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Sky
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_fog.affect_sky")), this.font)
                .pos(x, curY).selected(ModConfig.customFogAffectSky)
                .onValueChange((c, v) -> {
                    ModConfig.customFogAffectSky = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        // Clouds
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.custom_fog.affect_clouds")), this.font)
                .pos(x, curY).selected(ModConfig.customFogAffectClouds)
                .onValueChange((c, v) -> {
                    ModConfig.customFogAffectClouds = v;
                    ConfigManager.save();
                })
                .build());
    }
    // ===================== TARGET HUD =====================
    private void buildTargetHudPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        // Прозрачность фона
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_hud.bg_alpha", ModConfig.targetHudBgAlpha)),
                ModConfig.targetHudBgAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_hud.bg_alpha", ModConfig.targetHudBgAlpha)));
            }
            @Override
            protected void applyValue() {
                ModConfig.targetHudBgAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Градиент — цвет 1
        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.target_hud.color1"),
                () -> ModConfig.targetHudColor1,
                v -> ModConfig.targetHudColor1 = v);
        curY += 34 + rowGap;

        // Градиент — цвет 2
        addColorRow(widgets, x, curY, right,
                LocalizationManager.get("gui.resistancedlc.panel.target_hud.color2"),
                () -> ModConfig.targetHudColor2,
                v -> ModConfig.targetHudColor2 = v);
        curY += 34 + rowGap;

        // Скорость градиента
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_hud.gradient_speed", ModConfig.targetHudGradientSpeed)),
                (ModConfig.targetHudGradientSpeed - 0.1f) / 4.9f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_hud.gradient_speed", ModConfig.targetHudGradientSpeed)));
            }
            @Override
            protected void applyValue() {
                ModConfig.targetHudGradientSpeed = 0.1f + (float) (this.value * 4.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // Показывать скин
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_hud.show_skin")), this.font)
                .pos(x, curY).selected(ModConfig.targetHudShowSkin)
                .onValueChange((c, v) -> {
                    ModConfig.targetHudShowSkin = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        // Показывать имя
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_hud.show_name")), this.font)
                .pos(x, curY).selected(ModConfig.targetHudShowName)
                .onValueChange((c, v) -> {
                    ModConfig.targetHudShowName = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        // Показывать HP
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.target_hud.show_hp")), this.font)
                .pos(x, curY).selected(ModConfig.targetHudShowHp)
                .onValueChange((c, v) -> {
                    ModConfig.targetHudShowHp = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH + rowGap;

        // Позиция
        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.targetHudX, () -> ModConfig.targetHudY,
                (nx, ny) -> {
                    ModConfig.targetHudX = nx;
                    ModConfig.targetHudY = ny;
                },
                -1, -1);
    }
    // ===================== HEADWEAR =====================
    private void buildHeadwearPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        // ===== Выбор типа шляпы =====
        com.resistancedlc.headwear.HeadwearType[] types = {
                com.resistancedlc.headwear.HeadwearType.CHINESE_HAT,
                com.resistancedlc.headwear.HeadwearType.CAP,
                com.resistancedlc.headwear.HeadwearType.TOP_HAT
        };
        com.resistancedlc.headwear.HeadwearType current =
                com.resistancedlc.headwear.HeadwearType.fromId(ModConfig.headwearType);

        int btnW = (w - 6) / 3;
        for (int i = 0; i < types.length; i++) {
            final com.resistancedlc.headwear.HeadwearType t = types[i];
            boolean active = (t == current);
            String label = (active ? "§a✓ " : "") + t.displayName().getString();
            widgets.add(Button.builder(Component.literal(label), (b) -> {
                ModConfig.headwearType = t.id();
                ConfigManager.save();
                rebuildPanelTwoPass("headwear");
            }).bounds(x + i * (btnW + 3), curY, btnW, 20).build());
        }
        curY += rowH + rowGap;

        // ===== Цвет =====
        EditBox hexField = new EditBox(this.font, x, curY, 80, 18,
                Component.literal("#RRGGBB"));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.headwearColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal("OK"), (b) -> {
            try {
                ModConfig.headwearColor = 0xFF000000 | Integer.parseInt(
                        hexField.getValue().replace("#", "").trim(), 16);
                ConfigManager.save();
            } catch (Exception ignored) {}
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        if (presetW < 15) presetW = 15;
        String[] pn = {"R", "G", "B", "W"};
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hx = String.format("#%06X", color & 0xFFFFFF);
            widgets.add(Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hx);
                ModConfig.headwearColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build());
        }
        curY += rowH + rowGap;

        // ===== Alpha =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.headwearAlpha)),
                ModConfig.headwearAlpha / 255.0
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get(
                        "gui.resistancedlc.panel.alpha", ModConfig.headwearAlpha)));
            }
            @Override
            protected void applyValue() {
                ModConfig.headwearAlpha = (int) (this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Scale =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.headwear.scale",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.headwearScale))),
                (ModConfig.headwearScale - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get(
                        "gui.resistancedlc.panel.headwear.scale",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.headwearScale))));
            }
            @Override
            protected void applyValue() {
                ModConfig.headwearScale = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Offset Y =====
        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.headwear.offset_y",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.headwearOffsetY))),
                (ModConfig.headwearOffsetY + 0.5f)
        ) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get(
                        "gui.resistancedlc.panel.headwear.offset_y",
                        String.format(java.util.Locale.ROOT, "%.2f", ModConfig.headwearOffsetY))));
            }
            @Override
            protected void applyValue() {
                ModConfig.headwearOffsetY = (float) (this.value * 1.0 - 0.5);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        // ===== Self enabled =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.headwear.show_self")),
                        this.font)
                .pos(x, curY).selected(ModConfig.headwearSelfEnabled)
                .onValueChange((c, v) -> {
                    ModConfig.headwearSelfEnabled = v;
                    ConfigManager.save();
                })
                .build());
        curY += rowH;

        // ===== Friends =====
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.headwear.show_friends")),
                        this.font)
                .pos(x, curY).selected(ModConfig.headwearShowFriends)
                .onValueChange((c, v) -> {
                    ModConfig.headwearShowFriends = v;
                    ConfigManager.save();
                })
                .build());
    }
}