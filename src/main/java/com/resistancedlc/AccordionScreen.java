package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AccordionScreen — GUI мода с аккордеон-меню.
 * Одна панель раскрыта за раз, плавные анимации, глобальный поиск,
 * подсветка найденного, fade-in overlay, fade-in разделов.
 *
 * ВСЕ строки локализованы через LocalizationManager.
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
    private boolean keybindListenerRegistered = false;

    private int contentScroll = 0;
    private int maxContentScroll = 0;

    private int activeExtraHudSetting = -1;

    private final Map<String, Boolean> savedExpanded = new HashMap<>();

    private EditBox searchField;
    private final List<AccordionItem> filteredItems = new ArrayList<>();
    private String savedSearchText = "";
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
                            rebuildWidgetsPreservingState();
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
            Button themeBtn = Button.builder(Component.literal(""), (b) -> {})
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
            rebuildAllPanelWidgets();
        });

        if (!savedSearchText.isEmpty()) {
            restoringSearch = true;
            this.searchField.setValue(savedSearchText);
            restoringSearch = false;
        }
        this.addRenderableWidget(this.searchField);

        if (!keybindListenerRegistered) {
            keybindListenerRegistered = true;
            ScreenKeyboardEvents.allowKeyPress(this).register((screen, keyEvent) -> {
                if (!ModConfig.isBindingKey) return true;
                int keyCode = keyEvent.key();

                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    ModConfig.isBindingKey = false;
                    ModConfig.bindingTarget = 0;
                    rebuildWidgetsPreservingState();
                    return false;
                }

                switch (ModConfig.bindingTarget) {
                    case 0 -> KeyBindings.setKey(keyCode);
                    case 1 -> KeyBindings.setZoomKey(keyCode);
                    case 2 -> KeyBindings.setTapeMouseKey(keyCode);
                    case 3 -> KeyBindings.setAutoSwapKey(keyCode);
                    case 4 -> KeyBindings.setCustomHitSoundsKey(keyCode);
                    case 5 -> KeyBindings.setFastExpKey(keyCode);
                    case 6 -> KeyBindings.setShiftTapKey(keyCode);
                    case 7 -> KeyBindings.setComboKey(keyCode);
                    case 8 -> KeyBindings.setEffectWarningsKey(keyCode);
                    case 9 -> KeyBindings.setWaypointsKey(keyCode);
                    case 10 -> KeyBindings.setTotemLogKey(keyCode);
                    case 11 -> KeyBindings.setPickupLogKey(keyCode);
                }
                ModConfig.isBindingKey = false;
                ModConfig.bindingTarget = 0;
                rebuildWidgetsPreservingState();
                return false;
            });
        }

        restoreExpanded();
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                it.expandProgress = it.expanded ? 1.0f : 0.0f;
            }
        }
        updateFilteredItems();
        rebuildAllPanelWidgets();

        if (globalSearchOpen) {
            buildGlobalSearchWidgets();
        }

        // ===== ОБРАБОТКА ПЕРЕХОДА ИЗ HUD =====
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
                    rebuildAllPanelWidgets();
                    triggerHighlight(targetItem);
                }
            }
        }
    }

    // ===================== СОХРАНЕНИЕ =====================
    private void saveExpanded() {
        savedExpanded.clear();
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                savedExpanded.put(it.id, it.expanded);
            }
        }
        if (searchField != null) {
            savedSearchText = searchField.getValue();
        }
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
                rebuildAllPanelWidgets();
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

        int deltaX = (int)(columnWidth - lastColumnWidthForWidgets);
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
        return (int)(item.contentHeight * eased);
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

        rebuildAllPanelWidgets();
    }

    // ===================== РАЗДЕЛЫ =====================
    private void initSections() {
        sections.clear();

        // HUD
        Section hud = new Section("hud",
                LocalizationManager.get("gui.resistancedlc.section.hud"),
                LocalizationManager.get("gui.resistancedlc.section.hud.desc"),
                new ItemStack(Items.COMPASS));

        AccordionItem nhcItem = new AccordionItem(
                "no_hurt_cam",
                LocalizationManager.get("gui.resistancedlc.item.no_hurt_cam.title"),
                LocalizationManager.get("gui.resistancedlc.item.no_hurt_cam.desc"),
                () -> ModConfig.noHurtCamEnabled,
                () -> { ModConfig.noHurtCamEnabled = !ModConfig.noHurtCamEnabled; ConfigManager.save(); }
        );
        nhcItem.contentHeight = 46;
        hud.items.add(nhcItem);

        AccordionItem nbItem = new AccordionItem(
                "no_bobbing",
                LocalizationManager.get("gui.resistancedlc.item.no_bobbing.title"),
                LocalizationManager.get("gui.resistancedlc.item.no_bobbing.desc"),
                () -> ModConfig.noBobbingEnabled,
                () -> { ModConfig.noBobbingEnabled = !ModConfig.noBobbingEnabled; ConfigManager.save(); }
        );
        nbItem.contentHeight = 46;
        hud.items.add(nbItem);

        AccordionItem cdItem = new AccordionItem(
                "cooldowns",
                LocalizationManager.get("gui.resistancedlc.item.cooldowns.title"),
                LocalizationManager.get("gui.resistancedlc.item.cooldowns.desc"),
                () -> ModConfig.cooldownsEnabled,
                () -> { ModConfig.cooldownsEnabled = !ModConfig.cooldownsEnabled; ConfigManager.save(); }
        );
        cdItem.contentHeight = 270;
        hud.items.add(cdItem);

        AccordionItem comboItem = new AccordionItem(
                "combo",
                LocalizationManager.get("gui.resistancedlc.item.combo.title"),
                LocalizationManager.get("gui.resistancedlc.item.combo.desc"),
                () -> ModConfig.comboEnabled,
                () -> { ModConfig.comboEnabled = !ModConfig.comboEnabled; ConfigManager.save(); }
        );
        comboItem.contentHeight = 130;
        hud.items.add(comboItem);

        AccordionItem peItem = new AccordionItem(
                "potion_effects",
                LocalizationManager.get("gui.resistancedlc.item.potion_effects.title"),
                LocalizationManager.get("gui.resistancedlc.item.potion_effects.desc"),
                () -> ModConfig.showPotionEffects,
                () -> { ModConfig.showPotionEffects = !ModConfig.showPotionEffects; ConfigManager.save(); }
        );
        peItem.contentHeight = 102;
        hud.items.add(peItem);

        AccordionItem eqItem = new AccordionItem(
                "equipment_hud",
                LocalizationManager.get("gui.resistancedlc.item.equipment_hud.title"),
                LocalizationManager.get("gui.resistancedlc.item.equipment_hud.desc"),
                () -> ModConfig.showEquipmentHud,
                () -> { ModConfig.showEquipmentHud = !ModConfig.showEquipmentHud; ConfigManager.save(); }
        );
        eqItem.contentHeight = 102;
        hud.items.add(eqItem);

        AccordionItem ewItem = new AccordionItem(
                "effect_warnings",
                LocalizationManager.get("gui.resistancedlc.item.effect_warnings.title"),
                LocalizationManager.get("gui.resistancedlc.item.effect_warnings.desc"),
                () -> ModConfig.effectWarningsEnabled,
                () -> { ModConfig.effectWarningsEnabled = !ModConfig.effectWarningsEnabled; ConfigManager.save(); }
        );
        ewItem.contentHeight = 158;
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
                () -> { ModConfig.customHitSoundsEnabled = !ModConfig.customHitSoundsEnabled; ConfigManager.save(); }
        );
        chsItem.contentHeight = 158;
        pvp.items.add(chsItem);

        AccordionItem totemItem = new AccordionItem(
                "totem_log",
                LocalizationManager.get("gui.resistancedlc.item.totem_log.title"),
                LocalizationManager.get("gui.resistancedlc.item.totem_log.desc"),
                () -> ModConfig.totemLogEnabled,
                () -> { ModConfig.totemLogEnabled = !ModConfig.totemLogEnabled; ConfigManager.save(); }
        );
        totemItem.contentHeight = 102;
        pvp.items.add(totemItem);

        AccordionItem asItem = new AccordionItem(
                "auto_swap",
                LocalizationManager.get("gui.resistancedlc.item.auto_swap.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_swap.desc"),
                () -> ModConfig.autoSwapEnabled,
                () -> { ModConfig.autoSwapEnabled = !ModConfig.autoSwapEnabled; ConfigManager.save(); }
        );
        asItem.contentHeight = 158;
        pvp.items.add(asItem);

        AccordionItem feItem = new AccordionItem(
                "fast_exp",
                LocalizationManager.get("gui.resistancedlc.item.fast_exp.title"),
                LocalizationManager.get("gui.resistancedlc.item.fast_exp.desc"),
                () -> ModConfig.fastExpEnabled,
                () -> { ModConfig.fastExpEnabled = !ModConfig.fastExpEnabled; ConfigManager.save(); }
        );
        feItem.contentHeight = 46;
        pvp.items.add(feItem);

        AccordionItem stItem = new AccordionItem(
                "shift_tap",
                LocalizationManager.get("gui.resistancedlc.item.shift_tap.title"),
                LocalizationManager.get("gui.resistancedlc.item.shift_tap.desc"),
                () -> ModConfig.shiftTapEnabled,
                () -> { ModConfig.shiftTapEnabled = !ModConfig.shiftTapEnabled; ConfigManager.save(); }
        );
        stItem.contentHeight = 46;
        pvp.items.add(stItem);

        AccordionItem aspItem = new AccordionItem(
                "auto_sprint",
                LocalizationManager.get("gui.resistancedlc.item.auto_sprint.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_sprint.desc"),
                () -> ModConfig.autoSprintEnabled,
                () -> { ModConfig.autoSprintEnabled = !ModConfig.autoSprintEnabled; ConfigManager.save(); }
        );
        aspItem.contentHeight = 46;
        pvp.items.add(aspItem);

        AccordionItem psItem = new AccordionItem(
                "pvp_safe",
                LocalizationManager.get("gui.resistancedlc.item.pvp_safe.title"),
                LocalizationManager.get("gui.resistancedlc.item.pvp_safe.desc"),
                () -> ModConfig.pvpSafeEnabled,
                () -> { ModConfig.pvpSafeEnabled = !ModConfig.pvpSafeEnabled; ConfigManager.save(); }
        );
        psItem.contentHeight = 186;
        pvp.items.add(psItem);

        AccordionItem plItem = new AccordionItem(
                "pickup_logger",
                LocalizationManager.get("gui.resistancedlc.item.pickup_logger.title"),
                LocalizationManager.get("gui.resistancedlc.item.pickup_logger.desc"),
                () -> ModConfig.pickupLogEnabled,
                () -> { ModConfig.pickupLogEnabled = !ModConfig.pickupLogEnabled; ConfigManager.save(); }
        );
        plItem.contentHeight = 158;
        pvp.items.add(plItem);

        AccordionItem aggItem = new AccordionItem(
                "auto_gg",
                LocalizationManager.get("gui.resistancedlc.item.auto_gg.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_gg.desc"),
                () -> ModConfig.autoGgEnabled,
                () -> { ModConfig.autoGgEnabled = !ModConfig.autoGgEnabled; ConfigManager.save(); }
        );
        aggItem.contentHeight = 158;
        pvp.items.add(aggItem);

        AccordionItem eggItem = new AccordionItem(
                "killaura_egg",
                LocalizationManager.get("gui.resistancedlc.item.killaura_egg.title"),
                LocalizationManager.get("gui.resistancedlc.item.killaura_egg.desc"),
                () -> ModConfig.killAuraEggEnabled,
                () -> { ModConfig.killAuraEggEnabled = !ModConfig.killAuraEggEnabled; ConfigManager.save(); }
        );
        eggItem.contentHeight = 74;
        pvp.items.add(eggItem);

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
                () -> { ModConfig.tapeMouseEnabled = !ModConfig.tapeMouseEnabled; ConfigManager.save(); }
        );
        tmItem.contentHeight = 186;
        pve.items.add(tmItem);

        AccordionItem isItem = new AccordionItem(
                "item_scroller",
                LocalizationManager.get("gui.resistancedlc.item.item_scroller.title"),
                LocalizationManager.get("gui.resistancedlc.item.item_scroller.desc"),
                () -> ModConfig.itemScrollerEnabled,
                () -> { ModConfig.itemScrollerEnabled = !ModConfig.itemScrollerEnabled; ConfigManager.save(); }
        );
        isItem.contentHeight = 102;
        pve.items.add(isItem);

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
                () -> { ModConfig.zoomEnabled = !ModConfig.zoomEnabled; ConfigManager.save(); }
        );
        zoomItem.contentHeight = 102;
        visual.items.add(zoomItem);

        AccordionItem chItem = new AccordionItem(
                "crosshair",
                LocalizationManager.get("gui.resistancedlc.item.crosshair.title"),
                LocalizationManager.get("gui.resistancedlc.item.crosshair.desc"),
                () -> ModConfig.crosshairEnabled,
                () -> { ModConfig.crosshairEnabled = !ModConfig.crosshairEnabled; ConfigManager.save(); }
        );
        chItem.contentHeight = 214;
        visual.items.add(chItem);

        AccordionItem hbItem = new AccordionItem(
                "custom_hitbox",
                LocalizationManager.get("gui.resistancedlc.item.custom_hitbox.title"),
                LocalizationManager.get("gui.resistancedlc.item.custom_hitbox.desc"),
                () -> ModConfig.customHitboxEnabled,
                () -> { ModConfig.customHitboxEnabled = !ModConfig.customHitboxEnabled; ConfigManager.save(); }
        );
        hbItem.contentHeight = 74;
        visual.items.add(hbItem);

        AccordionItem ipItem = new AccordionItem(
                "item_physics",
                LocalizationManager.get("gui.resistancedlc.item.item_physics.title"),
                LocalizationManager.get("gui.resistancedlc.item.item_physics.desc"),
                () -> ModConfig.itemPhysicsEnabled,
                () -> { ModConfig.itemPhysicsEnabled = !ModConfig.itemPhysicsEnabled; ConfigManager.save(); }
        );
        ipItem.contentHeight = 46;
        visual.items.add(ipItem);

        AccordionItem arItem = new AccordionItem(
                "aspect_ratio",
                LocalizationManager.get("gui.resistancedlc.item.aspect_ratio.title"),
                LocalizationManager.get("gui.resistancedlc.item.aspect_ratio.desc"),
                () -> ModConfig.aspectRatioEnabled,
                () -> { ModConfig.aspectRatioEnabled = !ModConfig.aspectRatioEnabled; ConfigManager.save(); }
        );
        arItem.contentHeight = 102;
        visual.items.add(arItem);

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
        lfsItem.contentHeight = 130;
        visual.items.add(lfsItem);

        AccordionItem pbItem = new AccordionItem(
                "particle_blocker",
                LocalizationManager.get("gui.resistancedlc.item.particle_blocker.title"),
                LocalizationManager.get("gui.resistancedlc.item.particle_blocker.desc"),
                () -> ModConfig.particleBlockerEnabled,
                () -> { ModConfig.particleBlockerEnabled = !ModConfig.particleBlockerEnabled; ConfigManager.save(); }
        );
        pbItem.contentHeight = 130;
        visual.items.add(pbItem);

        AccordionItem wpItem = new AccordionItem(
                "waypoints",
                LocalizationManager.get("gui.resistancedlc.item.waypoints.title"),
                LocalizationManager.get("gui.resistancedlc.item.waypoints.desc"),
                () -> ModConfig.waypointsEnabled,
                () -> { ModConfig.waypointsEnabled = !ModConfig.waypointsEnabled; ConfigManager.save(); }
        );
        wpItem.contentHeight = calcWaypointsHeight();
        visual.items.add(wpItem);

        AccordionItem srItem = new AccordionItem(
                "strike_range",
                LocalizationManager.get("gui.resistancedlc.item.strike_range.title"),
                LocalizationManager.get("gui.resistancedlc.item.strike_range.desc"),
                () -> ModConfig.strikeRangeEnabled,
                () -> { ModConfig.strikeRangeEnabled = !ModConfig.strikeRangeEnabled; ConfigManager.save(); }
        );
        srItem.contentHeight = 214;
        visual.items.add(srItem);

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
                () -> { ModConfig.chatFilterEnabled = !ModConfig.chatFilterEnabled; ConfigManager.save(); }
        );
        cfItem.contentHeight = 186;
        misc.items.add(cfItem);

        AccordionItem arcItem = new AccordionItem(
                "auto_reconnect",
                LocalizationManager.get("gui.resistancedlc.item.auto_reconnect.title"),
                LocalizationManager.get("gui.resistancedlc.item.auto_reconnect.desc"),
                () -> ModConfig.autoReconnectEnabled,
                () -> { ModConfig.autoReconnectEnabled = !ModConfig.autoReconnectEnabled; ConfigManager.save(); }
        );
        arcItem.contentHeight = 74;
        misc.items.add(arcItem);

        AccordionItem dcItem = new AccordionItem(
                "death_coords",
                LocalizationManager.get("gui.resistancedlc.item.death_coords.title"),
                LocalizationManager.get("gui.resistancedlc.item.death_coords.desc"),
                () -> ModConfig.deathCoordsEnabled,
                () -> { ModConfig.deathCoordsEnabled = !ModConfig.deathCoordsEnabled; ConfigManager.save(); }
        );
        dcItem.contentHeight = 102;
        misc.items.add(dcItem);

        AccordionItem themeItem = new AccordionItem(
                "gui_theme",
                LocalizationManager.get("gui.resistancedlc.item.gui_theme.title"),
                LocalizationManager.get("gui.resistancedlc.item.gui_theme.desc"),
                () -> true,
                () -> {}
        );
        themeItem.contentHeight = 158;
        misc.items.add(themeItem);

        AccordionItem cfgItem = new AccordionItem(
                "config_manager",
                LocalizationManager.get("gui.resistancedlc.item.config_manager.title"),
                LocalizationManager.get("gui.resistancedlc.item.config_manager.desc"),
                () -> true,
                () -> {}
        );
        cfgItem.contentHeight = calcConfigManagerHeight();
        misc.items.add(cfgItem);

        sections.add(misc);

        // ===================== MUSIC =====================
        Section music = new Section("music",
                LocalizationManager.get("gui.resistancedlc.section.music"),
                LocalizationManager.get("gui.resistancedlc.section.music.desc"),
                new ItemStack(Items.MUSIC_DISC_CAT));

        AccordionItem mpItem = new AccordionItem(
                "music_player",
                LocalizationManager.get("gui.resistancedlc.item.music_player.title"),
                LocalizationManager.get("gui.resistancedlc.item.music_player.desc"),
                () -> ModConfig.musicPlayerEnabled,
                () -> { ModConfig.musicPlayerEnabled = !ModConfig.musicPlayerEnabled; ConfigManager.save(); }
        );
        mpItem.contentHeight = calcMusicPlayerHeight();
        music.items.add(mpItem);

        sections.add(music);
    }

    // ===================== ГРАНИЦЫ =====================
    private int getContentLeft() { return panelX + (int) columnWidth + 10; }
    private int getContentTop() { return panelY + HEADER_HEIGHT + 10; }
    private int getContentRight() { return panelX + PANEL_WIDTH - 12; }
    private int getContentBottom() { return panelY + PANEL_HEIGHT - 12; }
    private int getListTop() { return getContentTop() + 42; }
    private int getListBottom() { return getContentBottom() - 4; }

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
        for (AccordionItem item : filteredItems) {
            if (item.expanded) {
                buildPanelWidgets(item);
            }
        }
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
            case "no_hurt_cam" -> buildNoHurtCamPanel(widgets, innerX, innerY);
            case "no_bobbing" -> buildNoBobbingPanel(widgets, innerX, innerY);
            case "cooldowns" -> buildCoolDownsPanel(widgets, innerX, innerY, innerRight);
            case "combo" -> buildComboPanel(widgets, innerX, innerY, innerRight);
            case "potion_effects" -> buildPotionEffectsPanel(widgets, innerX, innerY, innerRight);
            case "equipment_hud" -> buildEquipmentHudPanel(widgets, innerX, innerY, innerRight);
            case "effect_warnings" -> buildEffectWarningsPanel(widgets, innerX, innerY, innerRight);
            case "extra_hud" -> buildExtraHudPanel(widgets, innerX, innerY, innerRight);
            case "custom_hit_sounds" -> buildCustomHitSoundsPanel(widgets, innerX, innerY, innerRight);
            case "totem_log" -> buildTotemLogPanel(widgets, innerX, innerY, innerRight);
            case "auto_swap" -> buildAutoSwapPanel(widgets, innerX, innerY, innerRight);
            case "fast_exp" -> buildFastExpPanel(widgets, innerX, innerY);
            case "shift_tap" -> buildShiftTapPanel(widgets, innerX, innerY);
            case "auto_sprint" -> buildAutoSprintPanel(widgets, innerX, innerY);
            case "pvp_safe" -> buildPvPSafePanel(widgets, innerX, innerY, innerRight);
            case "pickup_logger" -> buildPickUpLoggerPanel(widgets, innerX, innerY, innerRight);
            case "tape_mouse" -> buildTapeMousePanel(widgets, innerX, innerY, innerRight);
            case "item_scroller" -> buildItemScrollerPanel(widgets, innerX, innerY, innerRight);
            case "zoom" -> buildZoomPanel(widgets, innerX, innerY, innerRight);
            case "crosshair" -> buildCrosshairPanel(widgets, innerX, innerY, innerRight);
            case "custom_hitbox" -> buildCustomHitboxPanel(widgets, innerX, innerY, innerRight);
            case "item_physics" -> buildItemPhysicsPanel(widgets, innerX, innerY);
            case "aspect_ratio" -> buildAspectRatioPanel(widgets, innerX, innerY, innerRight);
            case "low_fire_shield" -> buildLowFireShieldPanel(widgets, innerX, innerY, innerRight);
            case "particle_blocker" -> buildParticleBlockerPanel(widgets, innerX, innerY, innerRight);
            case "waypoints" -> buildWaypointsPanel(widgets, innerX, innerY, innerRight);
            case "chat_filter" -> buildChatFilterPanel(widgets, innerX, innerY, innerRight);
            case "auto_reconnect" -> buildAutoReconnectPanel(widgets, innerX, innerY, innerRight);
            case "death_coords" -> buildDeathCoordsPanel(widgets, innerX, innerY, innerRight);
            case "gui_theme" -> buildGuiThemePanel(widgets, innerX, innerY, innerRight);
            case "config_manager" -> buildConfigManagerPanel(widgets, innerX, innerY, innerRight);
            case "auto_gg" -> buildAutoGGPanel(widgets, innerX, innerY, innerRight);
            case "strike_range" -> buildStrikeRangePanel(widgets, innerX, innerY, innerRight);
            case "killaura_egg" -> buildKillAuraPanel(widgets, innerX, innerY, innerRight);
            case "music_player" -> buildMusicPlayerPanel(widgets, innerX, innerY, innerRight);
            default -> { }
        }
        for (AbstractWidget w : widgets) {
            this.addRenderableWidget(w);
        }
        panelWidgets.put(item.id, widgets);
        lastColumnWidthForWidgets = columnWidth;
    }

    // ===================== ПАНЕЛИ =====================
    private void buildNoHurtCamPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "No Hurt Cam")),
                        this.font)
                .pos(x, y).selected(ModConfig.noHurtCamEnabled)
                .onValueChange((c, v) -> { ModConfig.noHurtCamEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildNoBobbingPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "No Bobbing")),
                        this.font)
                .pos(x, y).selected(ModConfig.noBobbingEnabled)
                .onValueChange((c, v) -> { ModConfig.noBobbingEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildItemPhysicsPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "ItemPhysics")),
                        this.font)
                .pos(x, y).selected(ModConfig.itemPhysicsEnabled)
                .onValueChange((c, v) -> { ModConfig.itemPhysicsEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildAutoSprintPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "AutoSprint")),
                        this.font)
                .pos(x, y).selected(ModConfig.autoSprintEnabled)
                .onValueChange((c, v) -> { ModConfig.autoSprintEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildFastExpPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "FastExp")),
                        this.font)
                .pos(x, y).selected(ModConfig.fastExpEnabled)
                .onValueChange((c, v) -> { ModConfig.fastExpEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildShiftTapPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "ShiftTap")),
                        this.font)
                .pos(x, y).selected(ModConfig.shiftTapEnabled)
                .onValueChange((c, v) -> { ModConfig.shiftTapEnabled = v; ConfigManager.save(); })
                .build());
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
            } catch (Exception ignored) {}
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
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show_effects")), this.font)
                .pos(x, curY).selected(ModConfig.showPotionEffects)
                .onValueChange((c, v) -> { ModConfig.showPotionEffects = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show_icons")), this.font)
                .pos(x, curY).selected(ModConfig.potionEffectsIcons)
                .onValueChange((c, v) -> { ModConfig.potionEffectsIcons = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.potionEffectsX, () -> ModConfig.potionEffectsY,
                (nx, ny) -> { ModConfig.potionEffectsX = nx; ModConfig.potionEffectsY = ny; },
                10, 170);
    }

    private void buildEquipmentHudPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.equipment_show")), this.font)
                .pos(x, curY).selected(ModConfig.showEquipmentHud)
                .onValueChange((c, v) -> { ModConfig.showEquipmentHud = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.equipment_durability")), this.font)
                .pos(x, curY).selected(ModConfig.equipmentShowDurability)
                .onValueChange((c, v) -> { ModConfig.equipmentShowDurability = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.equipmentHudX, () -> ModConfig.equipmentHudY,
                (nx, ny) -> { ModConfig.equipmentHudX = nx; ModConfig.equipmentHudY = ny; },
                4, -44);
    }

    private void buildEffectWarningsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "Effect Warnings")), this.font)
                .pos(x, curY).selected(ModConfig.effectWarningsEnabled)
                .onValueChange((c, v) -> { ModConfig.effectWarningsEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show")), this.font)
                .pos(x, curY).selected(ModConfig.effectWarningsShowName)
                .onValueChange((c, v) -> { ModConfig.effectWarningsShowName = v; ConfigManager.save(); })
                .build());

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show_icons")), this.font)
                .pos(x + w / 2, curY).selected(ModConfig.effectWarningsShowIcon)
                .onValueChange((c, v) -> { ModConfig.effectWarningsShowIcon = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.threshold", ModConfig.effectWarningsThreshold)),
                (ModConfig.effectWarningsThreshold - 3) / 12.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.threshold",
                        ModConfig.effectWarningsThreshold)));
            }
            @Override protected void applyValue() {
                ModConfig.effectWarningsThreshold = 3 + (int)(this.value * 12);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.effectWarningsX, () -> ModConfig.effectWarningsY,
                (nx, ny) -> { ModConfig.effectWarningsX = nx; ModConfig.effectWarningsY = ny; },
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
                    (b) -> {}
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
        AccordionItem extra = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("extra_hud")) { extra = it; break; }
            }
            if (extra != null) break;
        }
        if (extra == null || !extra.expanded) return;

        clearPanelWidgets(extra);
        buildPanelWidgets(extra);
        updateWidgetsVisibility();
    }

    // ===================== PVP =====================
    private void buildCustomHitSoundsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 6;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enable")), this.font)
                .pos(x, curY).selected(ModConfig.customHitSoundsEnabled)
                .onValueChange((c, v) -> { ModConfig.customHitSoundsEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

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
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.volume", ModConfig.customHitSoundVolume)),
                (ModConfig.customHitSoundVolume - 0.1f) / 1.9f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.volume",
                        ModConfig.customHitSoundVolume)));
            }
            @Override protected void applyValue() {
                ModConfig.customHitSoundVolume = 0.1f + (float)(this.value * 1.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pitch", ModConfig.customHitSoundPitch)),
                (ModConfig.customHitSoundPitch - 0.5f) / 1.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pitch",
                        ModConfig.customHitSoundPitch)));
            }
            @Override protected void applyValue() {
                ModConfig.customHitSoundPitch = 0.5f + (float)(this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void rebuildCustomHitSoundsPresets() {
        AccordionItem chs = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("custom_hit_sounds")) { chs = it; break; }
            }
            if (chs != null) break;
        }
        if (chs == null || !chs.expanded) return;

        clearPanelWidgets(chs);
        buildPanelWidgets(chs);
        updateWidgetsVisibility();
    }

    private void buildPvPSafePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 6;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "PvPSafe")), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeEnabled)
                .onValueChange((c, v) -> { ModConfig.pvpSafeEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.combat_timer", ModConfig.pvpSafeTimer)),
                (ModConfig.pvpSafeTimer - 10) / 50.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.combat_timer",
                        ModConfig.pvpSafeTimer)));
            }
            @Override protected void applyValue() {
                ModConfig.pvpSafeTimer = 10 + (int)(this.value * 50);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show")), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeBlockQuit)
                .onValueChange((c, v) -> { ModConfig.pvpSafeBlockQuit = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.pos_show_slot")), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeBlockCommands)
                .onValueChange((c, v) -> { ModConfig.pvpSafeBlockCommands = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_reconnect_hud")), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeShowHud)
                .onValueChange((c, v) -> { ModConfig.pvpSafeShowHud = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        // === ПРЕДУПРЕЖДЕНИЕ О ПЕРКАХ/СПЕЦ-ПРЕДМЕТАХ ===
        Button warnBtn = Button.builder(
                Component.literal("§c⚠ " + LocalizationManager.get("gui.resistancedlc.panel.pvp_safe_warning")),
                (b) -> {}
        ).bounds(x, curY, w, 18).build();
        warnBtn.active = false;
        widgets.add(warnBtn);
        curY += 18 + 4;

        Button warnBtn2 = Button.builder(
                Component.literal("§7" + LocalizationManager.get("gui.resistancedlc.panel.pvp_safe_warning2")),
                (b) -> {}
        ).bounds(x, curY, w, 18).build();
        warnBtn2.active = false;
        widgets.add(warnBtn2);
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
                    .onValueChange((c, v) -> { ModConfig.tapeMouseHoldRight = v; ConfigManager.save(); })
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
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay",
                        ModConfig.tapeMouseDelay)));
            }
            @Override protected void applyValue() {
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
                    .onValueChange((c, v) -> { ModConfig.tapeMouseRequireTarget = v; ConfigManager.save(); })
                    .build());
            curY += rowH;

            widgets.add(Checkbox.builder(
                            Component.literal(LocalizationManager.get("gui.resistancedlc.panel.only_full_charge")), this.font)
                    .pos(x, curY).selected(ModConfig.tapeMouseRequireFullAttack)
                    .onValueChange((c, v) -> { ModConfig.tapeMouseRequireFullAttack = v; ConfigManager.save(); })
                    .build());
        }
    }

    private void rebuildTapeMousePanel() {
        AccordionItem tm = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("tape_mouse")) { tm = it; break; }
            }
            if (tm != null) break;
        }
        if (tm == null || !tm.expanded) return;

        clearPanelWidgets(tm);
        buildPanelWidgets(tm);
        updateWidgetsVisibility();
    }

    private void buildItemScrollerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay_ms", ModConfig.itemScrollerDelay)),
                (ModConfig.itemScrollerDelay - 100) / 400.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay_ms",
                        ModConfig.itemScrollerDelay)));
            }
            @Override protected void applyValue() {
                ModConfig.itemScrollerDelay = 100 + (int)(this.value * 400);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.shift_stack")), this.font)
                .pos(x, curY).selected(ModConfig.itemScrollerShiftStack)
                .onValueChange((c, v) -> { ModConfig.itemScrollerShiftStack = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.ctrl_all")), this.font)
                .pos(x, curY).selected(ModConfig.itemScrollerCtrlAll)
                .onValueChange((c, v) -> { ModConfig.itemScrollerCtrlAll = v; ConfigManager.save(); })
                .build());
    }

    // ===================== VISUAL =====================
    private void buildZoomPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.zoom", ModConfig.zoomFactor)),
                (ModConfig.zoomFactor - 1.5f) / 8.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.zoom",
                        ModConfig.zoomFactor)));
            }
            @Override protected void applyValue() {
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
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.smoothness",
                        ModConfig.zoomSmoothness)));
            }
            @Override protected void applyValue() {
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
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.size",
                        ModConfig.crosshairSize)));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairSize = 4 + (int)(this.value * 16);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.thickness", ModConfig.crosshairThickness)),
                (ModConfig.crosshairThickness - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.thickness",
                        ModConfig.crosshairThickness)));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairThickness = 1 + (int)(this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.gap", ModConfig.crosshairGap)),
                ModConfig.crosshairGap / 10.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.gap",
                        ModConfig.crosshairGap)));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairGap = (int)(this.value * 10);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.crosshairAlpha)),
                ModConfig.crosshairAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.crosshairAlpha)));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairAlpha = (int)(this.value * 255);
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
            } catch (NumberFormatException ignored) {}
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        String[] pnTip = {
                LocalizationManager.get("gui.resistancedlc.tooltip.color_r"),
                LocalizationManager.get("gui.resistancedlc.tooltip.color_g"),
                LocalizationManager.get("gui.resistancedlc.tooltip.color_b"),
                LocalizationManager.get("gui.resistancedlc.tooltip.color_w")
        };
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            final String tip = pnTip[i];
            Button presetBtn = Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.crosshairColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build();
            presetBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(tip)));
            widgets.add(presetBtn);
        }
    }

    private void rebuildCrosshairPanel() {
        AccordionItem ch = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("crosshair")) { ch = it; break; }
            }
            if (ch != null) break;
        }
        if (ch == null || !ch.expanded) return;

        clearPanelWidgets(ch);
        buildPanelWidgets(ch);
        updateWidgetsVisibility();
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
            } catch (NumberFormatException ignored) {}
        }).bounds(x + 85, curY, 35, 18).build());

        int presetX = x + 125;
        int presetW = (right - presetX - 9) / 4;
        String[] pn = {"R", "G", "B", "W"};
        String[] pnTip = {
                LocalizationManager.get("gui.resistancedlc.tooltip.color_r"),
                LocalizationManager.get("gui.resistancedlc.tooltip.color_g"),
                LocalizationManager.get("gui.resistancedlc.tooltip.color_b"),
                LocalizationManager.get("gui.resistancedlc.tooltip.color_w")
        };
        int[] pc = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        for (int i = 0; i < 4; i++) {
            final int color = pc[i];
            final String hex = String.format("#%06X", color & 0xFFFFFF);
            final String tip = pnTip[i];
            Button presetBtn = Button.builder(Component.literal(pn[i]), (b) -> {
                hexField.setValue(hex);
                ModConfig.crosshairColor = color;
                ConfigManager.save();
            }).bounds(presetX + i * (presetW + 3), curY, presetW, 18).build();
            presetBtn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal(tip)));
            widgets.add(presetBtn);
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.customHitboxAlpha)),
                ModConfig.customHitboxAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.customHitboxAlpha)));
            }
            @Override protected void applyValue() {
                ModConfig.customHitboxAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void buildAspectRatioPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enable_stretch")), this.font)
                .pos(x, curY).selected(ModConfig.aspectRatioEnabled)
                .onValueChange((c, v) -> { ModConfig.aspectRatioEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.aspect_ratio", ModConfig.aspectRatio)),
                (ModConfig.aspectRatio - 0.5f) / 1.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.aspect_ratio",
                        ModConfig.aspectRatio)));
            }
            @Override protected void applyValue() {
                ModConfig.aspectRatio = 0.5f + (float) (this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        int btnW = (w - 9) / 4;
        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.ratio_4_3")), (b) -> {
            ModConfig.aspectRatio = 1.33f; ConfigManager.save(); rebuildAspectRatioPanel();
        }).bounds(x, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.ratio_16_9")), (b) -> {
            ModConfig.aspectRatio = 1.0f; ConfigManager.save(); rebuildAspectRatioPanel();
        }).bounds(x + btnW + 3, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.ratio_21_9")), (b) -> {
            ModConfig.aspectRatio = 0.75f; ConfigManager.save(); rebuildAspectRatioPanel();
        }).bounds(x + (btnW + 3) * 2, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.ratio_1_1")), (b) -> {
            ModConfig.aspectRatio = 1.78f; ConfigManager.save(); rebuildAspectRatioPanel();
        }).bounds(x + (btnW + 3) * 3, curY, btnW, 20).build());
    }

    private void rebuildAspectRatioPanel() {
        AccordionItem ar = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("aspect_ratio")) { ar = it; break; }
            }
            if (ar != null) break;
        }
        if (ar == null || !ar.expanded) return;

        clearPanelWidgets(ar);
        buildPanelWidgets(ar);
        updateWidgetsVisibility();
    }

    private void buildLowFireShieldPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_fire")), this.font)
                .pos(x, curY).selected(ModConfig.lowFireEnabled)
                .onValueChange((c, v) -> { ModConfig.lowFireEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_fire_offset", ModConfig.lowFireOffset)),
                ModConfig.lowFireOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_fire_offset",
                        ModConfig.lowFireOffset)));
            }
            @Override protected void applyValue() {
                ModConfig.lowFireOffset = (float) this.value;
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_shield")), this.font)
                .pos(x, curY).selected(ModConfig.lowShieldEnabled)
                .onValueChange((c, v) -> { ModConfig.lowShieldEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_shield_offset", ModConfig.lowShieldOffset)),
                ModConfig.lowShieldOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.low_shield_offset",
                        ModConfig.lowShieldOffset)));
            }
            @Override protected void applyValue() {
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

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.particle_show")), this.font)
                .pos(x, curY).selected(ModConfig.particleBlockerEnabled)
                .onValueChange((c, v) -> { ModConfig.particleBlockerEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

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
        // 4 строки: enable, max, add, clear
        int base = 18 + 4 * 28;
        int waypointsCount = WaypointManager.getWaypoints().size();
        if (waypointsCount > 0) {
            // header + waypoints * row
            base += 22;
            base += waypointsCount * 20;
        }
        return base;
    }

    private void rebuildWaypointsPanel() {
        AccordionItem wp = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("waypoints")) { wp = it; break; }
            }
            if (wp != null) break;
        }
        if (wp == null || !wp.expanded) return;

        clearPanelWidgets(wp);
        wp.contentHeight = calcWaypointsHeight();
        contentScroll = 0;
        buildPanelWidgets(wp);
        updateWidgetsVisibility();
    }

    private void buildWaypointsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "Waypoints")), this.font)
                .pos(x, curY).selected(ModConfig.waypointsEnabled)
                .onValueChange((c, v) -> { ModConfig.waypointsEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.waypoints_max", ModConfig.waypointsMax)),
                (ModConfig.waypointsMax - 1) / 19.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.waypoints_max",
                        ModConfig.waypointsMax)));
            }
            @Override protected void applyValue() {
                ModConfig.waypointsMax = 1 + (int)(this.value * 19);
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
                        mc.player.displayClientMessage(Component.literal(String.format(
                                "§a[Waypoints] Добавлена метка §6WP%d§a: §e%d, %d, %d",
                                idx, (int) px, (int) py, (int) pz)), true);
                        contentScroll = 0;
                        rebuildWaypointsPanel();
                    } else {
                        mc.player.displayClientMessage(Component.literal(
                                "§c[Waypoints] Достигнут лимит (" + ModConfig.waypointsMax + ")"), true);
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
                    (b) -> {}
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
                        (b) -> {}
                ).bounds(x, rowY, w - 50, 18).build();
                label.active = false;
                widgets.add(label);

                widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.rename")), (b) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        mc.player.displayClientMessage(Component.literal(
                                "§7Переименование: используйте команду §e/wp rename <старое> <новое>"), false);
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
            Button wordLabel = Button.builder(Component.literal("§e" + word), (b) -> {})
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
                            Component.literal("§7... ещё §e" + (words.size() - maxShow)), (b) -> {})
                    .bounds(x, curY, w, rowListH).build();
            moreBtn.active = false;
            widgets.add(moreBtn);
            curY += rowListH + 2;
        }

        widgets.add(Button.builder(
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.chat_filter_clear")),
                (b) -> { ChatFilterManager.clearWords(); rebuildChatFilterPanel(); }
        ).bounds(x, curY, w, 20).build());
    }

    private void rebuildChatFilterPanel() {
        AccordionItem cf = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("chat_filter")) { cf = it; break; }
            }
            if (cf != null) break;
        }
        if (cf == null || !cf.expanded) return;

        clearPanelWidgets(cf);
        buildPanelWidgets(cf);
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
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_reconnect_delay",
                        ModConfig.autoReconnectDelay)));
            }
            @Override protected void applyValue() {
                ModConfig.autoReconnectDelay = 1 + (int)(this.value * 29);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_reconnect_hud")), this.font)
                .pos(x, curY).selected(ModConfig.autoReconnectShowHud)
                .onValueChange((c, v) -> { ModConfig.autoReconnectShowHud = v; ConfigManager.save(); })
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
        Button infoBtn = Button.builder(Component.literal(info), (b) -> {})
                .bounds(x, curY, w, 20).build();
        infoBtn.active = false;
        widgets.add(infoBtn);
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
                                ConfigManager.save();
                                rebuildGuiThemePanel();
                            })
                    .bounds(x, curY, w, 20).build();
            widgets.add(themeBtn);

            curY += rowH + rowGap;
        }
    }

    private void rebuildGuiThemePanel() {
        AccordionItem theme = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("gui_theme")) { theme = it; break; }
            }
            if (theme != null) break;
        }
        if (theme == null || !theme.expanded) return;

        clearPanelWidgets(theme);
        buildPanelWidgets(theme);
        updateWidgetsVisibility();
    }

    // ===================== CONFIG MANAGER =====================
    private int calcConfigManagerHeight() {
        // 4 строки: open folder, save, load, name+save_as
        int base = 18 + 4 * 28;
        int configsCount = ConfigManager.listConfigs().size();
        if (configsCount > 0) {
            // header + configs * row + (опционально "ещё N")
            base += 22;
            int shown = Math.min(configsCount, 20);
            base += shown * 20;
            if (configsCount > 20) {
                base += 22;
            }
        }
        return base;
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
                        mc.player.displayClientMessage(Component.literal(
                                "§a" + LocalizationManager.get("gui.resistancedlc.message.folder_opened",
                                        ConfigManager.getConfigDirPath())), false);
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
                        mc.player.displayClientMessage(Component.literal(
                                "§a" + LocalizationManager.get("gui.resistancedlc.message.config_saved", "default")), true);
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
                            mc.player.displayClientMessage(Component.literal(
                                    "§a" + LocalizationManager.get("gui.resistancedlc.message.config_loaded", "default")), true);
                        }
                        contentScroll = 0;
                        rebuildConfigManagerPanel();
                    } else {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.literal(
                                    "§c" + LocalizationManager.get("gui.resistancedlc.message.config_not_found", "default")), true);
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
                            mc.player.displayClientMessage(Component.literal(
                                    "§a" + LocalizationManager.get("gui.resistancedlc.message.config_saved", name)), true);
                        }
                        contentScroll = 0;
                        rebuildConfigManagerPanel();
                    } else {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.literal(
                                    "§c" + LocalizationManager.get("gui.resistancedlc.message.config_save_failed", name)), true);
                        }
                    }
                }
        ).bounds(x + w - 85, curY, 85, 20).build());
        curY += rowH + rowGap;

        List<String> configs = ConfigManager.listConfigs();
        if (!configs.isEmpty()) {
            Button headerBtn = Button.builder(
                    Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.config_list")
                            + " (§f" + configs.size() + "§e):"),
                    (b) -> {}
            ).bounds(x, curY, w, 18).build();
            headerBtn.active = false;
            widgets.add(headerBtn);
            curY += 18 + 4;

            int maxShow = Math.min(configs.size(), 20);
            for (int i = 0; i < maxShow; i++) {
                final String cfgName = configs.get(i);
                int rowY = curY;

                Button label = Button.builder(
                        Component.literal("§e" + cfgName), (b) -> {}
                ).bounds(x, rowY, w - 25, 18).build();
                label.active = false;
                widgets.add(label);

                widgets.add(Button.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.remove")), (b) -> {
                    if (ConfigManager.remove(cfgName)) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.literal(
                                    "§a" + LocalizationManager.get("gui.resistancedlc.message.config_removed", cfgName)), true);
                        }
                        contentScroll = 0;
                        rebuildConfigManagerPanel();
                    }
                }).bounds(x + w - 22, rowY, 22, 18).build());

                curY += 20;
            }

            if (configs.size() > 20) {
                Button moreBtn = Button.builder(
                        Component.literal("§7... и ещё §e" + (configs.size() - 20)
                                + " §7(см. §e/cfg list§7)"), (b) -> {}
                ).bounds(x, curY, w, 18).build();
                moreBtn.active = false;
                widgets.add(moreBtn);
                curY += 18 + 4;
            }
        }
    }

    private void rebuildConfigManagerPanel() {
        AccordionItem cfg = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("config_manager")) { cfg = it; break; }
            }
            if (cfg != null) break;
        }
        if (cfg == null || !cfg.expanded) return;

        clearPanelWidgets(cfg);
        cfg.contentHeight = calcConfigManagerHeight();
        contentScroll = 0;
        buildPanelWidgets(cfg);
        updateWidgetsVisibility();
    }

    // ===================== COOLDOWNS =====================
    private void buildCoolDownsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.cooldownsX, () -> ModConfig.cooldownsY,
                (nx, ny) -> { ModConfig.cooldownsX = nx; ModConfig.cooldownsY = ny; },
                10, 200);

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.max_items", ModConfig.cooldownsMaxItems)),
                (ModConfig.cooldownsMaxItems - 1) / 9.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.max_items",
                        ModConfig.cooldownsMaxItems)));
            }
            @Override protected void applyValue() {
                ModConfig.cooldownsMaxItems = 1 + (int)(this.value * 9);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.cooldownsAlpha)),
                ModConfig.cooldownsAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.cooldownsAlpha)));
            }
            @Override protected void applyValue() {
                ModConfig.cooldownsAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.icon_darkening", ModConfig.cooldownsIconDarkening)),
                ModConfig.cooldownsIconDarkening / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.icon_darkening",
                        ModConfig.cooldownsIconDarkening)));
            }
            @Override protected void applyValue() {
                ModConfig.cooldownsIconDarkening = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        int cbW = (w - 6) / 3;
        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.icon")), this.font)
                .pos(x, curY).selected(ModConfig.cooldownsShowIcon)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowIcon = v; ConfigManager.save(); }).build());
        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.name")), this.font)
                .pos(x + cbW + 3, curY).selected(ModConfig.cooldownsShowName)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowName = v; ConfigManager.save(); }).build());
        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.timer")), this.font)
                .pos(x + (cbW + 3) * 2, curY).selected(ModConfig.cooldownsShowTime)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowTime = v; ConfigManager.save(); }).build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.hotbar_only")), this.font)
                .pos(x, curY).selected(ModConfig.cooldownsShowOnlyHotbar)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowOnlyHotbar = v; ConfigManager.save(); }).build());
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
        AccordionItem cd = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("cooldowns")) { cd = it; break; }
            }
            if (cd != null) break;
        }
        if (cd == null || !cd.expanded) return;

        clearPanelWidgets(cd);
        buildPanelWidgets(cd);
        updateWidgetsVisibility();
    }

    private void buildComboPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.comboX, () -> ModConfig.comboY,
                (nx, ny) -> { ModConfig.comboX = nx; ModConfig.comboY = ny; },
                10, 185);

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.timer", ModConfig.comboResetTime)),
                (ModConfig.comboResetTime - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.timer",
                        ModConfig.comboResetTime)));
            }
            @Override protected void applyValue() {
                ModConfig.comboResetTime = 1 + (int)(this.value * 4);
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
            } catch (NumberFormatException ignored) {}
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
        AccordionItem combo = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("combo")) { combo = it; break; }
            }
            if (combo != null) break;
        }
        if (combo == null || !combo.expanded) return;

        clearPanelWidgets(combo);
        buildPanelWidgets(combo);
        updateWidgetsVisibility();
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
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.radius",
                        ModConfig.totemLogRadius)));
            }
            @Override protected void applyValue() {
                ModConfig.totemLogRadius = 5 + (int)(this.value * 15.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.sound_notify")), this.font)
                .pos(x, curY).selected(ModConfig.totemLogSound)
                .onValueChange((c, v) -> { ModConfig.totemLogSound = v; ConfigManager.save(); })
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
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.delay_ms",
                        ModConfig.autoSwapOpenDelay)));
            }
            @Override protected void applyValue() {
                ModConfig.autoSwapOpenDelay = 50 + (int)(this.value * 450);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal("Cooldown: " + ModConfig.autoSwapCooldown + " ms"),
                (ModConfig.autoSwapCooldown - 100) / 1900.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal("Cooldown: " + ModConfig.autoSwapCooldown + " ms"));
            }
            @Override protected void applyValue() {
                ModConfig.autoSwapCooldown = 100 + (int)(this.value * 1900);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void rebuildAutoSwapPanel() {
        AccordionItem as = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("auto_swap")) { as = it; break; }
            }
            if (as != null) break;
        }
        if (as == null || !as.expanded) return;

        clearPanelWidgets(as);
        buildPanelWidgets(as);
        updateWidgetsVisibility();
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
        AccordionItem pl = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("pickup_logger")) { pl = it; break; }
            }
            if (pl != null) break;
        }
        if (pl == null || !pl.expanded) return;

        clearPanelWidgets(pl);
        buildPanelWidgets(pl);
        updateWidgetsVisibility();
    }

    // ===================== РЕНДЕР =====================
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        tickAnimations(delta);

        hoverColumn = isMouseOverColumn(mouseX, mouseY);

        boolean anyExpanded = false;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.expanded) { anyExpanded = true; break; }
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
        graphics.drawString(this.font, "§l" + LocalizationManager.get("gui.resistancedlc.title"),
                panelX + 15, panelY + 11, ModConfig.guiColor, true);

        drawColumn(graphics, mouseX, mouseY);
        drawContent(graphics, mouseX, mouseY);

        if (globalSearchOpen || globalSearchClosing) {
            drawGlobalSearchBackground(graphics);
        }

        super.render(graphics, mouseX, mouseY, delta);

        updateWidgetsVisibility();

        drawThemeButtons(graphics, mouseX, mouseY);

        if (globalSearchOpen || globalSearchClosing) {
            drawGlobalSearchForeground(graphics, mouseX, mouseY);
        }
    }

    private static final int[][] THEMES = {
            {0xFF00FF00, 0xFFFFFFFF, 0xFF00FF00},
            {0xFF808080, 0xFFDDDDDD, 0xFF808080},
            {0xFF00FFFF, 0xFF00FF00, 0xFF00FFFF},
            {0xFFFF69B4, 0xFFFFFFFF, 0xFFFF69B4},
            {0xFFFF0000, 0xFFFFFFFF, 0xFFFF0000},
    };

    private final int[][] themeButtonRects = new int[5][3];

    private static final String[] THEME_NAMES = {
            "Vanilla", "Dark", "Neon", "Candy", "Blood"
    };

    private void drawThemeButtons(GuiGraphics graphics, int mouseX, int mouseY) {
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

    private void drawTooltip(GuiGraphics graphics, String text, int mouseX, int mouseY) {
        int w = this.font.width(text) + 8;
        int h = 14;
        int x = mouseX + 10;
        int y = mouseY + 8;

        if (x + w > this.width - 4) x = mouseX - w - 10;
        if (y + h > this.height - 4) y = mouseY - h - 10;
        if (x < 4) x = 4;
        if (y < 4) y = 4;

        graphics.fill(x, y, x + w, y + h, 0xF0100010);
        graphics.fill(x, y, x + w, y + 1, ModConfig.guiColor);
        graphics.fill(x, y + h - 1, x + w, y + h, ModConfig.guiColor);
        graphics.fill(x, y, x + 1, y + h, ModConfig.guiColor);
        graphics.fill(x + w - 1, y, x + w, y + h, ModConfig.guiColor);

        graphics.drawString(this.font, text, x + 4, y + 3, 0xFFFFFFFF, false);
    }

    private void drawPanelBorders(GuiGraphics graphics) {
        int color = ModConfig.guiColor;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 2, color);
        graphics.fill(panelX, panelY + PANEL_HEIGHT - 2, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, color);
        graphics.fill(panelX, panelY, panelX + 2, panelY + PANEL_HEIGHT, color);
        graphics.fill(panelX + PANEL_WIDTH - 2, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, color);
    }

    private void drawColumn(GuiGraphics graphics, int mouseX, int mouseY) {
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

            if (isActive) {
                int bg = (ModConfig.guiColor & 0x00FFFFFF) | 0x60000000;
                graphics.fill(rowLeft, rowTop, rowRight, rowBottom, bg);
                graphics.fill(rowLeft, rowTop, rowLeft + 4, rowBottom, ModConfig.guiColor);
                graphics.fill(rowLeft, rowTop, rowRight, rowTop + 1, ModConfig.guiColor);
                graphics.fill(rowLeft, rowBottom - 1, rowRight, rowBottom, ModConfig.guiColor);
            } else if (isHover) {
                graphics.fill(rowLeft, rowTop, rowRight, rowBottom, 0x40FFFFFF);
                graphics.fill(rowLeft, rowTop, rowRight, rowTop + 1, 0x80FFFFFF);
                graphics.fill(rowLeft, rowBottom - 1, rowRight, rowBottom, 0x80FFFFFF);
            }

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) iconX, (float) iconY);
            float scale = (iconSize == 32) ? 2.0f : 1.5f;
            graphics.pose().scale(scale, scale);
            graphics.renderItem(section.icon, 0, 0);
            graphics.pose().popMatrix();

            if (columnWidth > 100) {
                int textX = iconX + iconSize + 10;
                int nameY = iconY + (iconSize - 8) / 2 - 6;
                int nameColor = isActive ? ModConfig.guiColor : 0xFFEEEEEE;
                graphics.drawString(this.font, "§l" + section.name, textX, nameY, nameColor, true);
                if (columnWidth > 140) {
                    graphics.drawString(this.font, "§7" + section.description,
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
        graphics.renderItem(new ItemStack(Items.SPYGLASS), 0, 0);
        graphics.pose().popMatrix();

        if (columnWidth > 100) {
            int searchTextX = searchIconX + iconSize + 10;
            int searchNameY = searchIconY + (iconSize - 8) / 2 - 6;
            graphics.drawString(this.font, "§l" + LocalizationManager.get("gui.resistancedlc.search"),
                    searchTextX, searchNameY, 0xFFEEEEEE, true);
            if (columnWidth > 140) {
                graphics.drawString(this.font, "§7" + LocalizationManager.get("gui.resistancedlc.search.all"),
                        searchTextX, searchNameY + 12, 0xFFAAAAAA, false);
            }
        }
    }

    private void drawContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int contentLeft = getContentLeft();
        int contentTop = getContentTop();
        int contentRight = getContentRight();
        int contentBottom = getContentBottom();

        graphics.fill(contentLeft, contentTop, contentRight, contentTop + 1, 0xFF303030);
        graphics.fill(contentLeft, contentBottom - 1, contentRight, contentBottom, 0xFF303030);
        graphics.fill(contentLeft, contentTop, contentLeft + 1, contentBottom, 0xFF303030);
        graphics.fill(contentRight - 1, contentTop, contentRight, contentBottom, 0xFF303030);

        Section active = sections.get(activeSectionIndex);
        graphics.drawString(this.font, "§l▸ " + active.name,
                contentLeft + 15, contentTop + 8, ModConfig.guiColor, true);
        graphics.drawString(this.font, "§7" + active.description,
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
            graphics.drawString(this.font, noResults,
                    listLeft + 15, listTop + 10, 0xFFAAAAAA, false);
            maxContentScroll = 0;
            contentScroll = 0;
            return;
        }

        int totalHeight = 0;
        int itemHeight = 26;
        int gap = 4;
        for (AccordionItem item : filteredItems) {
            totalHeight += itemHeight + gap;
            int animH = getAnimatedHeight(item);
            if (animH > 0) totalHeight += animH + gap;
        }
        int visibleHeight = listBottom - listTop;
        maxContentScroll = Math.max(0, totalHeight - visibleHeight);

        if (contentScroll > maxContentScroll) contentScroll = maxContentScroll;
        if (contentScroll < 0) contentScroll = 0;

        int itemY = listTop - contentScroll + (int)sectionSlideOffset;
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
            int thumbHeight = Math.max(15, (int)(barHeight * ratio));
            int thumbY = barTop + (int)((float) contentScroll / maxContentScroll * (barHeight - thumbHeight));

            graphics.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, ModConfig.guiColor);
        }
    }

    private void drawAccordionItemClipped(GuiGraphics graphics, AccordionItem item,
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

        int alpha = (int)(0xFF * sectionFadeProgress);
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
        if (item.expanded) graphics.fill(left, visibleTop, left + 3, visibleBottom, ModConfig.guiColor);

        if (top + 8 >= clipTop && top + 8 <= clipBottom) {
            String arrow = item.expanded ? "▼" : "▶";
            graphics.drawString(this.font, arrow, left + 8, top + 8, ModConfig.guiColor, true);
        }
        if (top + 3 >= clipTop && top + 3 <= clipBottom) {
            graphics.drawString(this.font, "§l" + item.title, left + 22, top + 3, textColor, true);
        }
        if (top + 14 >= clipTop && top + 14 <= clipBottom) {
            graphics.drawString(this.font, "§7" + item.description, left + 22, top + 14, descColor, false);
        }
        if (top + 6 >= clipTop && top + 6 <= clipBottom) {
            boolean status = item.statusGetter.get();
            String statusText = status ? "§a[ON]" : "§7[OFF]";
            int statusWidth = this.font.width(statusText);
            graphics.drawString(this.font, statusText, right - statusWidth - 10, top + 6, textColor, true);
        }
    }

    private void drawItemPanelClipped(GuiGraphics graphics, AccordionItem item,
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

    private void drawAccordionItem(GuiGraphics graphics, AccordionItem item,
                                   int contentLeft, int itemY, int contentRight, int itemHeight,
                                   boolean isHover) {
        int left = contentLeft + 5;
        int right = contentRight - 5;
        int top = itemY;
        int bottom = itemY + itemHeight;

        int alpha = (int)(0xFF * sectionFadeProgress);
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
            graphics.fill(left, top, left + 3, bottom, ModConfig.guiColor);
        } else if (isHover) {
            graphics.fill(left, top, left + 2, bottom, ModConfig.guiColor);
        }

        String arrow = item.expanded ? "▼" : "▶";
        graphics.drawString(this.font, arrow, left + 8, top + 8, ModConfig.guiColor, true);
        graphics.drawString(this.font, "§l" + item.title, left + 22, top + 3, textColor, true);
        graphics.drawString(this.font, "§7" + item.description, left + 22, top + 14, descColor, false);

        boolean status = item.statusGetter.get();
        String statusText = status ? "§a[ON]" : "§7[OFF]";
        int statusWidth = this.font.width(statusText);
        graphics.drawString(this.font, statusText, right - statusWidth - 10, top + 6, textColor, true);

        if (isHover && !item.expanded) {
            int arrowX = right - statusWidth - 26;
            graphics.drawString(this.font, "§7→", arrowX, top + 10, descColor, false);
        }

        if (isHighlightActive(item)) {
            float progress = getHighlightProgress(item);
            float pulse = 0.7f + 0.3f * (float)Math.sin(progress * Math.PI * 6);
            float fade = 1.0f - progress * 0.5f;
            int hiAlpha = (int)(0xFF * pulse * fade);

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
    private void drawGlobalSearchBackground(GuiGraphics graphics) {
        int overlayX = panelX + (PANEL_WIDTH - OVERLAY_W) / 2;
        int overlayY = panelY + (PANEL_HEIGHT - OVERLAY_H) / 2;

        int alpha = (int)(0xFF * globalSearchFadeProgress);
        int bgAlpha = (int)(0xB0 * globalSearchFadeProgress);
        int panelAlpha = (int)(0xF0 * globalSearchFadeProgress);

        graphics.fill(0, 0, this.width, this.height, (bgAlpha << 24));
        graphics.fill(overlayX, overlayY, overlayX + OVERLAY_W, overlayY + OVERLAY_H,
                (panelAlpha << 24) | 0x000000);

        int borderColor = (alpha << 24) | (ModConfig.guiColor & 0x00FFFFFF);
        graphics.fill(overlayX, overlayY, overlayX + OVERLAY_W, overlayY + 2, borderColor);
        graphics.fill(overlayX, overlayY + OVERLAY_H - 2, overlayX + OVERLAY_W, overlayY + OVERLAY_H, borderColor);
        graphics.fill(overlayX, overlayY, overlayX + 2, overlayY + OVERLAY_H, borderColor);
        graphics.fill(overlayX + OVERLAY_W - 2, overlayY, overlayX + OVERLAY_W, overlayY + OVERLAY_H, borderColor);

        graphics.drawString(this.font,
                "§l🔍 " + LocalizationManager.get("gui.resistancedlc.search.global"),
                overlayX + 15, overlayY + 15, borderColor, true);
    }

    private void drawGlobalSearchForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        int overlayX = panelX + (PANEL_WIDTH - OVERLAY_W) / 2;
        int overlayY = panelY + (PANEL_HEIGHT - OVERLAY_H) / 2;

        int alpha = (int)(0xFF * globalSearchFadeProgress);

        graphics.drawString(this.font,
                "§7" + LocalizationManager.get("gui.resistancedlc.search.found", globalSearchResults.size()),
                overlayX + 15, overlayY + 70, (alpha << 24) | 0xAAAAAA, false);

        globalSearchListX = overlayX + 15;
        globalSearchListY = overlayY + 90;
        globalSearchListW = OVERLAY_W - 30;
        globalSearchRowH = 22;

        globalSearchHovered = -1;

        String query = globalSearchField != null ? globalSearchField.getValue().trim() : "";

        if (query.isEmpty()) {
            graphics.drawString(this.font,
                    "§7" + LocalizationManager.get("gui.resistancedlc.search.start"),
                    globalSearchListX + 5, globalSearchListY + 10, (alpha << 24) | 0x888888, false);
        } else if (globalSearchResults.isEmpty()) {
            graphics.drawString(this.font,
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
                            rowY + globalSearchRowH, (int)(0x40 * globalSearchFadeProgress) << 24);
                    graphics.fill(globalSearchListX, rowY, globalSearchListX + 3,
                            rowY + globalSearchRowH, (alpha << 24) | (ModConfig.guiColor & 0x00FFFFFF));
                }

                String sectionName = r[0];
                for (Section s : sections) {
                    if (s.id.equals(r[0])) { sectionName = s.name; break; }
                }

                String text = "§f" + r[2] + " §7· §e" + sectionName;
                graphics.drawString(this.font, text, globalSearchListX + 10, rowY + 6, (alpha << 24) | 0xFFFFFF, false);

                String desc = r[3];
                if (desc.length() > 40) desc = desc.substring(0, 40) + "...";
                graphics.drawString(this.font, "§8" + desc,
                        globalSearchListX + 10 + this.font.width(text) + 8, rowY + 6, (alpha << 24) | 0x888888, false);
            }
        }

        graphics.drawString(this.font,
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
            contentScroll -= (int)(verticalAmount * 20);
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

            if (super.mouseClicked(event, isDoubleClick)) {
                return true;
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

        float currentTargetWidth = hoverColumn ? COLUMN_EXPANDED : COLUMN_COLLAPSED;
        int colLeft = panelX + 2;
        int colTop = panelY + HEADER_HEIGHT;
        int colRight = panelX + (int) currentTargetWidth;
        int colBottom = panelY + PANEL_HEIGHT - 2;

        boolean inColumn = mouseX >= colLeft && mouseX <= colRight
                && mouseY >= colTop && mouseY <= colBottom;

        if (inColumn) {
            int iconSize = (currentTargetWidth > 140) ? 32 : 24;
            int startY = colTop + 12;
            int yStep = (currentTargetWidth > 140) ? 58 : 48;

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

                if (button == 1) {
                    item.toggler.run();
                } else {
                    boolean wasExpanded = item.expanded;

                    closeAllExcept(null);

                    if (!wasExpanded) {
                        contentScroll = 0;
                        item.expanded = true;
                        buildPanelWidgets(item);
                    }
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
        float targetWidth = hoverColumn ? COLUMN_EXPANDED : COLUMN_COLLAPSED;
        int colLeft = panelX + 2;
        int colTop = panelY + HEADER_HEIGHT;
        int colRight = panelX + (int) targetWidth;
        int colBottom = panelY + PANEL_HEIGHT - 2;
        return mouseX >= colLeft && mouseX <= colRight
                && mouseY >= colTop && mouseY <= colBottom;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
    // ===================== AUTO GG =====================
    private void buildAutoGGPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enable")), this.font)
                .pos(x, curY).selected(ModConfig.autoGgEnabled)
                .onValueChange((c, v) -> { ModConfig.autoGgEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_gg_only_players")), this.font)
                .pos(x, curY).selected(ModConfig.autoGgOnlyPlayers)
                .onValueChange((c, v) -> { ModConfig.autoGgOnlyPlayers = v; ConfigManager.save(); })
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
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.auto_gg_delay",
                        ModConfig.autoGgDelay)));
            }
            @Override protected void applyValue() {
                ModConfig.autoGgDelay = (float)(this.value * 5.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    // ===================== STRIKE RANGE =====================
    private void buildStrikeRangePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.enable")), this.font)
                .pos(x, curY).selected(ModConfig.strikeRangeEnabled)
                .onValueChange((c, v) -> { ModConfig.strikeRangeEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.strikeRangeX, () -> ModConfig.strikeRangeY,
                (nx, ny) -> { ModConfig.strikeRangeX = nx; ModConfig.strikeRangeY = ny; },
                10, 300);
        curY += 26 + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha", ModConfig.strikeRangeAlpha)),
                ModConfig.strikeRangeAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.alpha",
                        ModConfig.strikeRangeAlpha)));
            }
            @Override protected void applyValue() {
                ModConfig.strikeRangeAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(LocalizationManager.get("gui.resistancedlc.panel.strike_range_show_time", ModConfig.strikeRangeShowTime)),
                ModConfig.strikeRangeShowTime / 5000.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.strike_range_show_time",
                        ModConfig.strikeRangeShowTime)));
            }
            @Override protected void applyValue() {
                ModConfig.strikeRangeShowTime = 200 + (int)(this.value * 4800);
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
                .onValueChange((c, v) -> { ModConfig.strikeRangeShowBlocks = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.strike_range_show_target")), this.font)
                .pos(x, curY).selected(ModConfig.strikeRangeShowTarget)
                .onValueChange((c, v) -> { ModConfig.strikeRangeShowTarget = v; ConfigManager.save(); })
                .build());
    }

    private void rebuildStrikeRangePanel() {
        AccordionItem sr = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("strike_range")) { sr = it; break; }
            }
            if (sr != null) break;
        }
        if (sr == null || !sr.expanded) return;

        clearPanelWidgets(sr);
        buildPanelWidgets(sr);
        updateWidgetsVisibility();
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

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.checkbox", "Music Player")),
                        this.font)
                .pos(x, curY).selected(ModConfig.musicPlayerEnabled)
                .onValueChange((c, v) -> { ModConfig.musicPlayerEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

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
                        (int)(ModConfig.musicVolume * 100))),
                ModConfig.musicVolume
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_volume",
                        (int)(ModConfig.musicVolume * 100))));
            }
            @Override protected void applyValue() {
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
                .onValueChange((c, v) -> { MusicPlayerManager.toggleShuffle(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_show_hud")),
                        this.font)
                .pos(x, curY).selected(ModConfig.musicShowHud)
                .onValueChange((c, v) -> { ModConfig.musicShowHud = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.music_autoplay")),
                        this.font)
                .pos(x, curY).selected(ModConfig.musicAutoPlay)
                .onValueChange((c, v) -> { ModConfig.musicAutoPlay = v; ConfigManager.save(); })
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
        Button infoBtn = Button.builder(Component.literal(infoText), (b) -> {})
                .bounds(x, curY, w, 18).build();
        infoBtn.active = false;
        widgets.add(infoBtn);
        curY += 18 + rowGap;

        List<MusicTrack> tracks = MusicPlayerManager.getPlaylist();
        if (!tracks.isEmpty()) {
            Button headerBtn = Button.builder(
                            Component.literal("§e" + LocalizationManager.get("gui.resistancedlc.panel.music_tracks")
                                    + " (§f" + tracks.size() + "§e):"),
                            (b) -> {})
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
                        (b) -> {}).bounds(x, curY, w, 18).build();
                moreBtn.active = false;
                widgets.add(moreBtn);
            }
        } else {
            Button emptyBtn = Button.builder(
                    Component.literal("§7" + LocalizationManager.get("gui.resistancedlc.panel.music_empty")),
                    (b) -> {}).bounds(x, curY, w, 18).build();
            emptyBtn.active = false;
            widgets.add(emptyBtn);
        }
    }

    private void rebuildMusicPlayerPanel() {
        AccordionItem mp = null;
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                if (it.id.equals("music_player")) { mp = it; break; }
            }
            if (mp != null) break;
        }
        if (mp == null || !mp.expanded) return;

        clearPanelWidgets(mp);
        mp.contentHeight = calcMusicPlayerHeight();
        contentScroll = 0;
        buildPanelWidgets(mp);
        updateWidgetsVisibility();
    }
    // ===================== EASTER EGG (KILLAURA) =====================
    private void buildKillAuraPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        // === Кнопка "Установить модуль KillAura" ===
        widgets.add(Button.builder(
                        Component.literal(LocalizationManager.get("gui.resistancedlc.panel.killaura_button")),
                        (b) -> EasterEggManager.onKillAuraClick())
                .bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        // === Hint-кнопка ===
        String defaultHint = LocalizationManager.get("gui.resistancedlc.panel.killaura_hint");
        Button hintBtn = Button.builder(
                Component.literal("§7" + defaultHint),
                (b) -> {}
        ).bounds(x, curY, w, 18).build();
        hintBtn.active = false;
        widgets.add(hintBtn);

        EasterEggManager.registerHintButton(hintBtn);
    }
}