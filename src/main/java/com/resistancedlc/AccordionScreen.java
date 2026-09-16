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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * AccordionScreen — новый GUI мода.
 * Шаг 5: все панели настроек.
 * Шаг 6: скролл контента + клиппинг + сохранение expanded.
 */
public class AccordionScreen extends Screen {

    // ===================== РАЗМЕРЫ ПАНЕЛИ =====================
    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 460;
    private static final int COLUMN_COLLAPSED = 60;
    private static final int COLUMN_EXPANDED = 220;
    private static final int HEADER_HEIGHT = 30;

    // ===================== РЕАЛИЗОВАННЫЕ ПАНЕЛИ =====================
    private static final Set<String> IMPLEMENTED_PANELS = Set.of(
            "no_hurt_cam", "no_bobbing", "cooldowns", "combo",
            "potion_effects", "equipment_hud", "effect_warnings", "extra_hud",
            "totem_log", "auto_swap", "custom_hit_sounds", "fast_exp",
            "shift_tap", "auto_sprint", "pvp_safe", "pickup_logger",
            "tape_mouse", "item_scroller",
            "zoom", "crosshair", "custom_hitbox", "item_physics",
            "aspect_ratio", "low_fire_shield", "particle_blocker",
            "chat_filter", "auto_reconnect", "death_coords"
    );

    // ===================== ПОЗИЦИИ =====================
    private int panelX;
    private int panelY;

    // ===================== СОСТОЯНИЕ =====================
    private int activeSectionIndex = 0;
    private float columnWidth = COLUMN_COLLAPSED;
    private boolean hoverColumn = false;

    // ===================== КЭШ ВИДЖЕТОВ =====================
    private final Map<String, List<AbstractWidget>> panelWidgets = new HashMap<>();
    private boolean keybindListenerRegistered = false;

    // ===================== СКРОЛЛ КОНТЕНТА =====================
    private int contentScroll = 0;
    private int maxContentScroll = 0;

    // ===================== EXTRA HUD: АКТИВНАЯ НАСТРОЙКА =====================
    /** 0=FPS, 1=Ping, 2=TPS, 3=BPS, 4=Dir, 5=Hits, -1=ничего. */
    private int activeExtraHudSetting = -1;

    // ===================== СОХРАНЕНИЕ EXPANDED =====================
    private final Map<String, Boolean> savedExpanded = new HashMap<>();

    // ===================== МОДЕЛЬ: ФУНКЦИЯ =====================
    public static class AccordionItem {
        public final String id;
        public final String title;
        public final String description;
        public final Supplier<Boolean> statusGetter;
        public final Runnable toggler;
        public boolean expanded = false;
        public int contentHeight = 100;

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

    // ===================== МОДЕЛЬ: РАЗДЕЛ =====================
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
        super(Component.literal("Resistance DLC"));
    }

    // ===================== INIT =====================
    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        panelWidgets.clear();
        sections.clear();
        initSections();

        Button langBtn = Button.builder(
                        Component.literal(ModConfig.modLogoRussian ? "EN" : "RU"),
                        (b) -> {
                            ModConfig.modLogoRussian = !ModConfig.modLogoRussian;
                            ConfigManager.save();
                            rebuildWidgetsPreservingState();
                        })
                .bounds(panelX + PANEL_WIDTH - 70, panelY + 6, 30, 18).build();
        this.addRenderableWidget(langBtn);

        Button closeBtn = Button.builder(
                        Component.literal("×"),
                        (b) -> this.onClose())
                .bounds(panelX + PANEL_WIDTH - 35, panelY + 6, 30, 18).build();
        this.addRenderableWidget(closeBtn);

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

        // Восстанавливаем expanded-флаги после rebuildWidgets()
        restoreExpanded();

        rebuildAllPanelWidgets();
    }

    /** Сохраняет expanded-флаги всех функций. */
    private void saveExpanded() {
        savedExpanded.clear();
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                savedExpanded.put(it.id, it.expanded);
            }
        }
    }

    /** Восстанавливает expanded-флаги. */
    private void restoreExpanded() {
        for (Section s : sections) {
            for (AccordionItem it : s.items) {
                Boolean saved = savedExpanded.get(it.id);
                if (saved != null) it.expanded = saved;
            }
        }
    }

    /** rebuildWidgets() с сохранением expanded-флагов. */
    private void rebuildWidgetsPreservingState() {
        saveExpanded();
        this.rebuildWidgets();
    }

    // ===================== РАЗДЕЛЫ =====================
    private void initSections() {
        sections.clear();

        // ===================== HUD =====================
        Section hud = new Section("hud", "HUD",
                ModConfig.modLogoRussian ? "Координаты, FPS, эффекты" : "Coords, FPS, effects",
                new ItemStack(Items.COMPASS));

        hud.items.add(new AccordionItem(
                "no_hurt_cam", "No Hurt Cam",
                ModConfig.modLogoRussian ? "Убирает тряску при уроне" : "Removes damage shake",
                () -> ModConfig.noHurtCamEnabled,
                () -> { ModConfig.noHurtCamEnabled = !ModConfig.noHurtCamEnabled; ConfigManager.save(); }
        ));
        hud.items.add(new AccordionItem(
                "no_bobbing", "No Bobbing",
                ModConfig.modLogoRussian ? "Убирает покачивание камеры" : "Removes camera bobbing",
                () -> ModConfig.noBobbingEnabled,
                () -> { ModConfig.noBobbingEnabled = !ModConfig.noBobbingEnabled; ConfigManager.save(); }
        ));

        AccordionItem cdItem = new AccordionItem(
                "cooldowns", "CoolDowns",
                ModConfig.modLogoRussian ? "Кулдауны предметов" : "Item cooldowns",
                () -> ModConfig.cooldownsEnabled,
                () -> { ModConfig.cooldownsEnabled = !ModConfig.cooldownsEnabled; ConfigManager.save(); }
        );
        cdItem.contentHeight = 260;
        hud.items.add(cdItem);

        AccordionItem comboItem = new AccordionItem(
                "combo", "Combo Counter",
                ModConfig.modLogoRussian ? "Счётчик комбо" : "Combo counter",
                () -> ModConfig.comboEnabled,
                () -> { ModConfig.comboEnabled = !ModConfig.comboEnabled; ConfigManager.save(); }
        );
        comboItem.contentHeight = 170;
        hud.items.add(comboItem);

        AccordionItem peItem = new AccordionItem(
                "potion_effects", "Potion Effects",
                ModConfig.modLogoRussian ? "Эффекты зелий" : "Potion effects",
                () -> ModConfig.showPotionEffects,
                () -> { ModConfig.showPotionEffects = !ModConfig.showPotionEffects; ConfigManager.save(); }
        );
        peItem.contentHeight = 110;
        hud.items.add(peItem);

        AccordionItem eqItem = new AccordionItem(
                "equipment_hud", "Equipment HUD",
                ModConfig.modLogoRussian ? "Экипировка игрока" : "Player equipment",
                () -> ModConfig.showEquipmentHud,
                () -> { ModConfig.showEquipmentHud = !ModConfig.showEquipmentHud; ConfigManager.save(); }
        );
        eqItem.contentHeight = 110;
        hud.items.add(eqItem);

        AccordionItem ewItem = new AccordionItem(
                "effect_warnings", "Effect Warnings",
                ModConfig.modLogoRussian ? "Предупреждение о конце эффектов" : "Effect expiring warnings",
                () -> ModConfig.effectWarningsEnabled,
                () -> { ModConfig.effectWarningsEnabled = !ModConfig.effectWarningsEnabled; ConfigManager.save(); }
        );
        ewItem.contentHeight = 200;
        hud.items.add(ewItem);

        AccordionItem ehItem = new AccordionItem(
                "extra_hud", "Extra HUD",
                ModConfig.modLogoRussian ? "FPS, Ping, TPS, BPS, Direction, Hits" : "FPS, Ping, TPS, BPS, Direction, Hits",
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
        ehItem.contentHeight = 230;
        hud.items.add(ehItem);

        sections.add(hud);

        // ===================== PVP =====================
        Section pvp = new Section("pvp", "PvP",
                ModConfig.modLogoRussian ? "Бой, свап, звуки" : "Combat, swap, sounds",
                new ItemStack(Items.DIAMOND_SWORD));

        AccordionItem chsItem = new AccordionItem(
                "custom_hit_sounds", "Custom Hit Sounds",
                ModConfig.modLogoRussian ? "Кастомные звуки удара" : "Custom hit sounds",
                () -> ModConfig.customHitSoundsEnabled,
                () -> { ModConfig.customHitSoundsEnabled = !ModConfig.customHitSoundsEnabled; ConfigManager.save(); }
        );
        chsItem.contentHeight = 200;
        pvp.items.add(chsItem);

        AccordionItem totemItem = new AccordionItem(
                "totem_log", "Totem Log",
                ModConfig.modLogoRussian ? "Лог сбитых тотемов" : "Log of broken totems",
                () -> ModConfig.totemLogEnabled,
                () -> { ModConfig.totemLogEnabled = !ModConfig.totemLogEnabled; ConfigManager.save(); }
        );
        totemItem.contentHeight = 120;
        pvp.items.add(totemItem);

        AccordionItem asItem = new AccordionItem(
                "auto_swap", "AutoSwap",
                ModConfig.modLogoRussian ? "Свап offhand ↔ инвентарь" : "Offhand ↔ inventory swap",
                () -> ModConfig.autoSwapEnabled,
                () -> { ModConfig.autoSwapEnabled = !ModConfig.autoSwapEnabled; ConfigManager.save(); }
        );
        asItem.contentHeight = 170;
        pvp.items.add(asItem);

        AccordionItem feItem = new AccordionItem(
                "fast_exp", "FastExp",
                ModConfig.modLogoRussian ? "Быстрое использование опыта" : "Fast exp usage",
                () -> ModConfig.fastExpEnabled,
                () -> { ModConfig.fastExpEnabled = !ModConfig.fastExpEnabled; ConfigManager.save(); }
        );
        feItem.contentHeight = 70;
        pvp.items.add(feItem);

        AccordionItem stItem = new AccordionItem(
                "shift_tap", "ShiftTap",
                ModConfig.modLogoRussian ? "Крит через шифт" : "Crit via shift",
                () -> ModConfig.shiftTapEnabled,
                () -> { ModConfig.shiftTapEnabled = !ModConfig.shiftTapEnabled; ConfigManager.save(); }
        );
        stItem.contentHeight = 70;
        pvp.items.add(stItem);

        AccordionItem aspItem = new AccordionItem(
                "auto_sprint", "AutoSprint",
                ModConfig.modLogoRussian ? "Автобег" : "Auto sprint",
                () -> ModConfig.autoSprintEnabled,
                () -> { ModConfig.autoSprintEnabled = !ModConfig.autoSprintEnabled; ConfigManager.save(); }
        );
        aspItem.contentHeight = 70;
        pvp.items.add(aspItem);

        AccordionItem psItem = new AccordionItem(
                "pvp_safe", "PvPSafe",
                ModConfig.modLogoRussian ? "Защита от выхода в бою" : "Combat quit protection",
                () -> ModConfig.pvpSafeEnabled,
                () -> { ModConfig.pvpSafeEnabled = !ModConfig.pvpSafeEnabled; ConfigManager.save(); }
        );
        psItem.contentHeight = 170;
        pvp.items.add(psItem);

        AccordionItem plItem = new AccordionItem(
                "pickup_logger", "PickUpLogger",
                ModConfig.modLogoRussian ? "Лог подобранных предметов" : "Pickup log",
                () -> ModConfig.pickupLogEnabled,
                () -> { ModConfig.pickupLogEnabled = !ModConfig.pickupLogEnabled; ConfigManager.save(); }
        );
        plItem.contentHeight = 220;
        pvp.items.add(plItem);

        sections.add(pvp);

        // ===================== PVE =====================
        Section pve = new Section("pve", "PvE",
                ModConfig.modLogoRussian ? "Кликеры, скролл" : "Clickers, scroller",
                new ItemStack(Items.CARROT));

        AccordionItem tmItem = new AccordionItem(
                "tape_mouse", "TapeMouse",
                ModConfig.modLogoRussian ? "Автокликер" : "Autoclicker",
                () -> ModConfig.tapeMouseEnabled,
                () -> { ModConfig.tapeMouseEnabled = !ModConfig.tapeMouseEnabled; ConfigManager.save(); }
        );
        tmItem.contentHeight = 200;
        pve.items.add(tmItem);

        AccordionItem isItem = new AccordionItem(
                "item_scroller", "ItemScroller",
                ModConfig.modLogoRussian ? "Быстрый перенос скроллом" : "Fast scroll transfer",
                () -> ModConfig.itemScrollerEnabled,
                () -> { ModConfig.itemScrollerEnabled = !ModConfig.itemScrollerEnabled; ConfigManager.save(); }
        );
        isItem.contentHeight = 120;
        pve.items.add(isItem);

        sections.add(pve);

        // ===================== VISUAL =====================
        Section visual = new Section("visual", "Visual",
                ModConfig.modLogoRussian ? "Зум, прицел, эффекты" : "Zoom, crosshair, effects",
                new ItemStack(Items.ENDER_EYE));

        AccordionItem zoomItem = new AccordionItem(
                "zoom", "Zoom",
                ModConfig.modLogoRussian ? "Плавное приближение" : "Smooth zoom",
                () -> ModConfig.zoomEnabled,
                () -> { ModConfig.zoomEnabled = !ModConfig.zoomEnabled; ConfigManager.save(); }
        );
        zoomItem.contentHeight = 120;
        visual.items.add(zoomItem);

        AccordionItem chItem = new AccordionItem(
                "crosshair", "Custom Crosshair",
                ModConfig.modLogoRussian ? "Кастомный прицел" : "Custom crosshair",
                () -> ModConfig.crosshairEnabled,
                () -> { ModConfig.crosshairEnabled = !ModConfig.crosshairEnabled; ConfigManager.save(); }
        );
        chItem.contentHeight = 260;
        visual.items.add(chItem);

        AccordionItem hbItem = new AccordionItem(
                "custom_hitbox", "Custom Hitbox",
                ModConfig.modLogoRussian ? "Цвет debug-хитбоксов" : "Debug hitbox color",
                () -> ModConfig.customHitboxEnabled,
                () -> { ModConfig.customHitboxEnabled = !ModConfig.customHitboxEnabled; ConfigManager.save(); }
        );
        hbItem.contentHeight = 90;
        visual.items.add(hbItem);

        visual.items.add(new AccordionItem(
                "item_physics", "ItemPhysics",
                ModConfig.modLogoRussian ? "Предметы лежат плашмя" : "Items lie flat",
                () -> ModConfig.itemPhysicsEnabled,
                () -> { ModConfig.itemPhysicsEnabled = !ModConfig.itemPhysicsEnabled; ConfigManager.save(); }
        ));

        AccordionItem arItem = new AccordionItem(
                "aspect_ratio", "Aspect Ratio",
                ModConfig.modLogoRussian ? "Растяг FOV" : "FOV stretch",
                () -> ModConfig.aspectRatioEnabled,
                () -> { ModConfig.aspectRatioEnabled = !ModConfig.aspectRatioEnabled; ConfigManager.save(); }
        );
        arItem.contentHeight = 120;
        visual.items.add(arItem);

        AccordionItem lfsItem = new AccordionItem(
                "low_fire_shield", "Low Fire / Shield",
                ModConfig.modLogoRussian ? "Низкий огонь и щит" : "Low fire and shield",
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
                "particle_blocker", "Particle Blocker",
                ModConfig.modLogoRussian ? "Отключение частиц" : "Particle blocking",
                () -> ModConfig.particleBlockerEnabled,
                () -> { ModConfig.particleBlockerEnabled = !ModConfig.particleBlockerEnabled; ConfigManager.save(); }
        );
        pbItem.contentHeight = 170;
        visual.items.add(pbItem);

        sections.add(visual);

        // ===================== MISC =====================
        Section misc = new Section("misc", "Misc",
                ModConfig.modLogoRussian ? "Фильтр, автореконнект" : "Filter, auto-reconnect",
                new ItemStack(Items.REDSTONE));

        AccordionItem cfItem = new AccordionItem(
                "chat_filter", "ChatFilter",
                ModConfig.modLogoRussian ? "Фильтр чата" : "Chat filter",
                () -> ModConfig.chatFilterEnabled,
                () -> { ModConfig.chatFilterEnabled = !ModConfig.chatFilterEnabled; ConfigManager.save(); }
        );
        cfItem.contentHeight = 180;
        misc.items.add(cfItem);

        AccordionItem arcItem = new AccordionItem(
                "auto_reconnect", "AutoReconnect",
                ModConfig.modLogoRussian ? "Авто-переподключение" : "Auto reconnection",
                () -> ModConfig.autoReconnectEnabled,
                () -> { ModConfig.autoReconnectEnabled = !ModConfig.autoReconnectEnabled; ConfigManager.save(); }
        );
        arcItem.contentHeight = 90;
        misc.items.add(arcItem);

        AccordionItem dcItem = new AccordionItem(
                "death_coords", "DeathCoords",
                ModConfig.modLogoRussian ? "Координаты смерти" : "Death coords",
                () -> ModConfig.deathCoordsEnabled,
                () -> { ModConfig.deathCoordsEnabled = !ModConfig.deathCoordsEnabled; ConfigManager.save(); }
        );
        dcItem.contentHeight = 110;
        misc.items.add(dcItem);

        sections.add(misc);
    }

    // ===================== ГРАНИЦЫ КОНТЕНТА =====================
    private int getContentLeft() { return panelX + (int) columnWidth + 10; }
    private int getContentTop() { return panelY + HEADER_HEIGHT + 10; }
    private int getContentRight() { return panelX + PANEL_WIDTH - 12; }
    private int getContentBottom() { return panelY + PANEL_HEIGHT - 12; }

    private int[] getPanelBounds(AccordionItem target) {
        int contentLeft = getContentLeft();
        int contentTop = getContentTop();
        int contentRight = getContentRight();

        int itemY = contentTop + 42 - contentScroll;
        int itemHeight = 26;
        int gap = 2;

        Section active = sections.get(activeSectionIndex);
        for (AccordionItem item : active.items) {
            if (item == target && item.expanded) {
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
        for (Section section : sections) {
            for (AccordionItem item : section.items) {
                if (item.expanded) {
                    buildPanelWidgets(item);
                }
            }
        }
    }

    private void buildPanelWidgets(AccordionItem item) {
        int[] bounds = getPanelBounds(item);
        if (bounds == null) return;

        int left = bounds[0];
        int top = bounds[1];
        int right = bounds[2];

        int innerX = left + 15;
        int innerY = top + 12;
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
            case "chat_filter" -> buildChatFilterPanel(widgets, innerX, innerY, innerRight);
            case "auto_reconnect" -> buildAutoReconnectPanel(widgets, innerX, innerY, innerRight);
            case "death_coords" -> buildDeathCoordsPanel(widgets, innerX, innerY, innerRight);
            default -> { }
        }

        for (AbstractWidget w : widgets) {
            this.addRenderableWidget(w);
        }
        panelWidgets.put(item.id, widgets);
    }

    // ===================== ПАНЕЛИ: ПРОСТЫЕ =====================
    private void buildNoHurtCamPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить No Hurt Cam" : "Enable No Hurt Cam"),
                        this.font)
                .pos(x, y).selected(ModConfig.noHurtCamEnabled)
                .onValueChange((c, v) -> { ModConfig.noHurtCamEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildNoBobbingPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить No Bobbing" : "Enable No Bobbing"),
                        this.font)
                .pos(x, y).selected(ModConfig.noBobbingEnabled)
                .onValueChange((c, v) -> { ModConfig.noBobbingEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildItemPhysicsPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить ItemPhysics" : "Enable ItemPhysics"),
                        this.font)
                .pos(x, y).selected(ModConfig.itemPhysicsEnabled)
                .onValueChange((c, v) -> { ModConfig.itemPhysicsEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildAutoSprintPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить AutoSprint" : "Enable AutoSprint"),
                        this.font)
                .pos(x, y).selected(ModConfig.autoSprintEnabled)
                .onValueChange((c, v) -> { ModConfig.autoSprintEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildFastExpPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить FastExp" : "Enable FastExp"),
                        this.font)
                .pos(x, y).selected(ModConfig.fastExpEnabled)
                .onValueChange((c, v) -> { ModConfig.fastExpEnabled = v; ConfigManager.save(); })
                .build());
    }

    private void buildShiftTapPanel(List<AbstractWidget> widgets, int x, int y) {
        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить ShiftTap" : "Enable ShiftTap"),
                        this.font)
                .pos(x, y).selected(ModConfig.shiftTapEnabled)
                .onValueChange((c, v) -> { ModConfig.shiftTapEnabled = v; ConfigManager.save(); })
                .build());
    }

    // ===================== УТИЛИТА: ПОЗИЦИЯ =====================
    private int addPosEditorRow(List<AbstractWidget> widgets,
                                int x, int y, int right,
                                Supplier<Integer> getX, Supplier<Integer> getY,
                                java.util.function.BiConsumer<Integer, Integer> setXY,
                                int defX, int defY) {
        int w = right - x;
        int rowH = 20;
        int gap = 6;

        EditBox posField = new EditBox(this.font, x, y, w - 50, 18,
                Component.literal("X, Y"));
        posField.setMaxLength(20);
        posField.setValue(getX.get() + ", " + getY.get());
        widgets.add(posField);

        widgets.add(Button.builder(Component.literal("OK"), (b) -> {
            try {
                String[] parts = posField.getValue().split(",");
                if (parts.length == 2) {
                    int nx = Integer.parseInt(parts[0].trim());
                    int ny = Integer.parseInt(parts[1].trim());
                    setXY.accept(nx, ny);
                    ConfigManager.save();
                }
            } catch (Exception ignored) {}
        }).bounds(x + w - 45, y, 20, 18).build());

        widgets.add(Button.builder(Component.literal("↺"), (b) -> {
            setXY.accept(defX, defY);
            posField.setValue(defX + ", " + defY);
            ConfigManager.save();
        }).bounds(x + w - 22, y, 22, 18).build());

        return y + rowH + gap;
    }

    // ===================== ПАНЕЛИ: HUD =====================

    private void buildPotionEffectsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать эффекты" : "Show effects"), this.font)
                .pos(x, curY).selected(ModConfig.showPotionEffects)
                .onValueChange((c, v) -> { ModConfig.showPotionEffects = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать иконки" : "Show icons"), this.font)
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
                        Component.literal(ModConfig.modLogoRussian ? "Показывать экипировку" : "Show equipment"), this.font)
                .pos(x, curY).selected(ModConfig.showEquipmentHud)
                .onValueChange((c, v) -> { ModConfig.showEquipmentHud = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать прочность" : "Show durability"), this.font)
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
                        Component.literal(ModConfig.modLogoRussian ? "Включить Effect Warnings" : "Enable Effect Warnings"), this.font)
                .pos(x, curY).selected(ModConfig.effectWarningsEnabled)
                .onValueChange((c, v) -> { ModConfig.effectWarningsEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать название" : "Show name"), this.font)
                .pos(x, curY).selected(ModConfig.effectWarningsShowName)
                .onValueChange((c, v) -> { ModConfig.effectWarningsShowName = v; ConfigManager.save(); })
                .build());

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать иконку" : "Show icon"), this.font)
                .pos(x + w / 2, curY).selected(ModConfig.effectWarningsShowIcon)
                .onValueChange((c, v) -> { ModConfig.effectWarningsShowIcon = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Порог: " : "Threshold: ")
                        + ModConfig.effectWarningsThreshold
                        + (ModConfig.modLogoRussian ? " сек" : " sec")),
                (ModConfig.effectWarningsThreshold - 3) / 12.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Порог: " : "Threshold: ")
                        + ModConfig.effectWarningsThreshold
                        + (ModConfig.modLogoRussian ? " сек" : " sec")));
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

        String[] namesRu = {"КВС", "Пинг", "ТВС", "БВС", "Направление", "Удары"};
        String[] namesEn = {"FPS", "Ping", "TPS", "BPS", "Direction", "Hits"};
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
                            Component.literal(ModConfig.modLogoRussian ? namesRu[i] : namesEn[i]), this.font)
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
                    Component.literal("§6⚙"),
                    (b) -> {
                        activeExtraHudSetting = (activeExtraHudSetting == gearIdx) ? -1 : gearIdx;
                        rebuildWidgetsPreservingState();
                    }
            ).bounds(gearX, gearY, gearSize, gearSize).build());
        }
        curY += rowH * 2 + rowGap;

        if (activeExtraHudSetting >= 0) {
            final int idx = activeExtraHudSetting;
            String settingName = ModConfig.modLogoRussian ? namesRu[idx] : namesEn[idx];

            Button labelBtn = Button.builder(
                    Component.literal((ModConfig.modLogoRussian ? "§eНастройка: §f" : "§eSetting: §f") + settingName),
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

    // ===================== ПАНЕЛИ: PVP =====================

    private void buildCustomHitSoundsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 6;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить" : "Enable"), this.font)
                .pos(x, curY).selected(ModConfig.customHitSoundsEnabled)
                .onValueChange((c, v) -> { ModConfig.customHitSoundsEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        String keyName = KeyBindings.customHitSoundsKey != null
                ? KeyBindings.customHitSoundsKey.getTranslatedKeyMessage().getString() : "J";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 4
                        ? (ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key...")
                        : (ModConfig.modLogoRussian ? "Клавиша: " : "Key: ") + keyName),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 4;
                    b.setMessage(Component.literal(ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key..."));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        int btnW = (w - 18) / 7;
        for (int i = 1; i <= 7; i++) {
            final int preset = i;
            widgets.add(Button.builder(Component.literal(String.valueOf(i)),
                            (b) -> {
                                ModConfig.customHitSoundPreset = preset;
                                ConfigManager.save();
                                rebuildWidgetsPreservingState();
                            })
                    .bounds(x + (i - 1) * (btnW + 3), curY, btnW, 20).build());
        }
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Громкость: %.1f" : "Volume: %.1f",
                        ModConfig.customHitSoundVolume)),
                (ModConfig.customHitSoundVolume - 0.1f) / 1.9f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Громкость: %.1f" : "Volume: %.1f",
                        ModConfig.customHitSoundVolume)));
            }
            @Override protected void applyValue() {
                ModConfig.customHitSoundVolume = 0.1f + (float)(this.value * 1.9f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Тон: %.1f" : "Pitch: %.1f",
                        ModConfig.customHitSoundPitch)),
                (ModConfig.customHitSoundPitch - 0.5f) / 1.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Тон: %.1f" : "Pitch: %.1f",
                        ModConfig.customHitSoundPitch)));
            }
            @Override protected void applyValue() {
                ModConfig.customHitSoundPitch = 0.5f + (float)(this.value * 1.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void buildPvPSafePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int curY = y;
        int rowH = 22, rowGap = 6;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Включить PvPSafe" : "Enable PvPSafe"), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeEnabled)
                .onValueChange((c, v) -> { ModConfig.pvpSafeEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Таймер боя: " : "Combat timer: ")
                        + ModConfig.pvpSafeTimer
                        + (ModConfig.modLogoRussian ? " сек" : " sec")),
                (ModConfig.pvpSafeTimer - 10) / 50.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Таймер боя: " : "Combat timer: ")
                        + ModConfig.pvpSafeTimer + (ModConfig.modLogoRussian ? " сек" : " sec")));
            }
            @Override protected void applyValue() {
                ModConfig.pvpSafeTimer = 10 + (int)(this.value * 50);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Блокировать выход (ESC)" : "Block quit (ESC)"), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeBlockQuit)
                .onValueChange((c, v) -> { ModConfig.pvpSafeBlockQuit = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Блокировать команды" : "Block commands"), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeBlockCommands)
                .onValueChange((c, v) -> { ModConfig.pvpSafeBlockCommands = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать HUD-таймер" : "Show HUD timer"), this.font)
                .pos(x, curY).selected(ModConfig.pvpSafeShowHud)
                .onValueChange((c, v) -> { ModConfig.pvpSafeShowHud = v; ConfigManager.save(); })
                .build());
    }

    // ===================== ПАНЕЛИ: PVE =====================

    private void buildTapeMousePanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Button.builder(
                Component.literal(ModConfig.tapeMouseButton == 0
                        ? (ModConfig.modLogoRussian ? "Кнопка: ЛКМ (атака)" : "Button: LMB (attack)")
                        : (ModConfig.modLogoRussian ? "Кнопка: ПКМ (использование)" : "Button: RMB (use)")),
                (b) -> {
                    ModConfig.tapeMouseButton = (ModConfig.tapeMouseButton + 1) % 2;
                    ConfigManager.save();
                    rebuildWidgetsPreservingState();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        if (ModConfig.tapeMouseButton == 0) {
            String[] targetsRu = {"Все", "Только мобы", "Только игроки"};
            String[] targetsEn = {"All", "Mobs only", "Players only"};
            int tgt = Math.max(0, Math.min(2, ModConfig.tapeMouseTarget));
            widgets.add(Button.builder(
                    Component.literal((ModConfig.modLogoRussian ? "Цель: " : "Target: ")
                            + (ModConfig.modLogoRussian ? targetsRu[tgt] : targetsEn[tgt])),
                    (b) -> {
                        ModConfig.tapeMouseTarget = (ModConfig.tapeMouseTarget + 1) % 3;
                        ConfigManager.save();
                        rebuildWidgetsPreservingState();
                    }
            ).bounds(x, curY, w, 20).build());
            curY += rowH + rowGap;
        } else {
            widgets.add(Checkbox.builder(
                            Component.literal(ModConfig.modLogoRussian ? "Зажать ПКМ" : "Hold RMB"), this.font)
                    .pos(x, curY).selected(ModConfig.tapeMouseHoldRight)
                    .onValueChange((c, v) -> { ModConfig.tapeMouseHoldRight = v; ConfigManager.save(); })
                    .build());
            curY += rowH + rowGap;
        }

        String tmKeyName = KeyBindings.tapeMouseKey != null
                ? KeyBindings.tapeMouseKey.getTranslatedKeyMessage().getString() : "R";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 2
                        ? (ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key...")
                        : (ModConfig.modLogoRussian ? "Клавиша: " : "Key: ") + tmKeyName),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 2;
                    b.setMessage(Component.literal(ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key..."));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal(String.format(ModConfig.modLogoRussian ? "Задержка: %.1f сек" : "Delay: %.1f sec",
                        ModConfig.tapeMouseDelay)),
                (ModConfig.tapeMouseDelay - 0.1f) / 4.9f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Задержка: %.1f сек" : "Delay: %.1f sec",
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
                            Component.literal(ModConfig.modLogoRussian ? "Бить только при наведении" : "Only when aiming"), this.font)
                    .pos(x, curY).selected(ModConfig.tapeMouseRequireTarget)
                    .onValueChange((c, v) -> { ModConfig.tapeMouseRequireTarget = v; ConfigManager.save(); })
                    .build());
            curY += rowH;

            widgets.add(Checkbox.builder(
                            Component.literal(ModConfig.modLogoRussian ? "Бить только при заряженной атаке" : "Only on full charge"), this.font)
                    .pos(x, curY).selected(ModConfig.tapeMouseRequireFullAttack)
                    .onValueChange((c, v) -> { ModConfig.tapeMouseRequireFullAttack = v; ConfigManager.save(); })
                    .build());
        }
    }

    private void buildItemScrollerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Задержка: " : "Delay: ")
                        + ModConfig.itemScrollerDelay
                        + (ModConfig.modLogoRussian ? " мс" : " ms")),
                (ModConfig.itemScrollerDelay - 100) / 400.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Задержка: " : "Delay: ")
                        + ModConfig.itemScrollerDelay
                        + (ModConfig.modLogoRussian ? " мс" : " ms")));
            }
            @Override protected void applyValue() {
                ModConfig.itemScrollerDelay = 100 + (int)(this.value * 400);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Shift → вся стопка" : "Shift → whole stack"), this.font)
                .pos(x, curY).selected(ModConfig.itemScrollerShiftStack)
                .onValueChange((c, v) -> { ModConfig.itemScrollerShiftStack = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Ctrl → все стопки" : "Ctrl → all stacks"), this.font)
                .pos(x, curY).selected(ModConfig.itemScrollerCtrlAll)
                .onValueChange((c, v) -> { ModConfig.itemScrollerCtrlAll = v; ConfigManager.save(); })
                .build());
    }

    // ===================== ПАНЕЛИ: VISUAL =====================

    private void buildZoomPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal(String.format(ModConfig.modLogoRussian ? "Сила зума: %.1fx" : "Zoom factor: %.1fx",
                        ModConfig.zoomFactor)),
                (ModConfig.zoomFactor - 1.5f) / 8.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Сила зума: %.1fx" : "Zoom factor: %.1fx",
                        ModConfig.zoomFactor)));
            }
            @Override protected void applyValue() {
                ModConfig.zoomFactor = 1.5f + (float) (this.value * 8.5f);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(
                x, curY, w, 20,
                Component.literal(String.format(ModConfig.modLogoRussian ? "Плавность: %.2f" : "Smoothness: %.2f",
                        ModConfig.zoomSmoothness)),
                (ModConfig.zoomSmoothness - 0.05f) / 0.95f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Плавность: %.2f" : "Smoothness: %.2f",
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
                        ? (ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key...")
                        : (ModConfig.modLogoRussian ? "Клавиша: " : "Key: ") + keyName),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 1;
                    b.setMessage(Component.literal(ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key..."));
                }
        ).bounds(x, curY, w, 20).build());
    }

    private void buildCrosshairPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String[] shapesRu = {"Крест", "Точка", "Круг", "Стрелки", "Крест + Точка"};
        String[] shapesEn = {"Cross", "Dot", "Circle", "Arrows", "Cross + Dot"};
        int shape = Math.max(0, Math.min(4, ModConfig.crosshairShape));
        widgets.add(Button.builder(
                Component.literal((ModConfig.modLogoRussian ? "Форма: " : "Shape: ")
                        + (ModConfig.modLogoRussian ? shapesRu[shape] : shapesEn[shape])),
                (b) -> {
                    ModConfig.crosshairShape = (ModConfig.crosshairShape + 1) % 5;
                    ConfigManager.save();
                    rebuildWidgetsPreservingState();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Размер: " : "Size: ") + ModConfig.crosshairSize + " px"),
                (ModConfig.crosshairSize - 4) / 16.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Размер: " : "Size: ")
                        + ModConfig.crosshairSize + " px"));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairSize = 4 + (int)(this.value * 16);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Толщина: " : "Thickness: ") + ModConfig.crosshairThickness + " px"),
                (ModConfig.crosshairThickness - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Толщина: " : "Thickness: ")
                        + ModConfig.crosshairThickness + " px"));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairThickness = 1 + (int)(this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Зазор: " : "Gap: ") + ModConfig.crosshairGap + " px"),
                ModConfig.crosshairGap / 10.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Зазор: " : "Gap: ")
                        + ModConfig.crosshairGap + " px"));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairGap = (int)(this.value * 10);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Прозрачность: " : "Alpha: ") + ModConfig.crosshairAlpha),
                ModConfig.crosshairAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Прозрачность: " : "Alpha: ")
                        + ModConfig.crosshairAlpha));
            }
            @Override protected void applyValue() {
                ModConfig.crosshairAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18, Component.literal("#RRGGBB"));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.crosshairColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal("OK"), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.crosshairColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
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

    private void buildCustomHitboxPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18, Component.literal("#RRGGBB"));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.customHitboxColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal("OK"), (b) -> {
            String hex = hexField.getValue().replace("#", "").trim();
            try {
                ModConfig.customHitboxColor = 0xFF000000 | Integer.parseInt(hex, 16);
                ConfigManager.save();
            } catch (NumberFormatException ignored) {}
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
                Component.literal((ModConfig.modLogoRussian ? "Прозрачность: " : "Alpha: ") + ModConfig.customHitboxAlpha),
                ModConfig.customHitboxAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Прозрачность: " : "Alpha: ")
                        + ModConfig.customHitboxAlpha));
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
                        Component.literal(ModConfig.modLogoRussian ? "Включить растяг" : "Enable stretch"), this.font)
                .pos(x, curY).selected(ModConfig.aspectRatioEnabled)
                .onValueChange((c, v) -> { ModConfig.aspectRatioEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(String.format(ModConfig.modLogoRussian ? "Соотношение: %.2f" : "Ratio: %.2f",
                        ModConfig.aspectRatio)),
                (ModConfig.aspectRatio - 0.5f) / 1.5f
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Соотношение: %.2f" : "Ratio: %.2f",
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
        widgets.add(Button.builder(Component.literal("4:3"), (b) -> {
            ModConfig.aspectRatio = 1.33f; ConfigManager.save(); rebuildWidgetsPreservingState();
        }).bounds(x, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal("16:9"), (b) -> {
            ModConfig.aspectRatio = 1.0f; ConfigManager.save(); rebuildWidgetsPreservingState();
        }).bounds(x + btnW + 3, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal("21:9"), (b) -> {
            ModConfig.aspectRatio = 0.75f; ConfigManager.save(); rebuildWidgetsPreservingState();
        }).bounds(x + (btnW + 3) * 2, curY, btnW, 20).build());
        widgets.add(Button.builder(Component.literal("1:1"), (b) -> {
            ModConfig.aspectRatio = 1.78f; ConfigManager.save(); rebuildWidgetsPreservingState();
        }).bounds(x + (btnW + 3) * 3, curY, btnW, 20).build());
    }

    private void buildLowFireShieldPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Низкий огонь" : "Low Fire"), this.font)
                .pos(x, curY).selected(ModConfig.lowFireEnabled)
                .onValueChange((c, v) -> { ModConfig.lowFireEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(String.format(ModConfig.modLogoRussian ? "Смещение огня: %.2f" : "Fire offset: %.2f",
                        ModConfig.lowFireOffset)),
                ModConfig.lowFireOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Смещение огня: %.2f" : "Fire offset: %.2f",
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
                        Component.literal(ModConfig.modLogoRussian ? "Низкий щит" : "Low Shield"), this.font)
                .pos(x, curY).selected(ModConfig.lowShieldEnabled)
                .onValueChange((c, v) -> { ModConfig.lowShieldEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal(String.format(ModConfig.modLogoRussian ? "Смещение щита: %.2f" : "Shield offset: %.2f",
                        ModConfig.lowShieldOffset)),
                ModConfig.lowShieldOffset
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal(String.format(
                        ModConfig.modLogoRussian ? "Смещение щита: %.2f" : "Shield offset: %.2f",
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
                        Component.literal(ModConfig.modLogoRussian ? "Включить Particle Blocker" : "Enable Particle Blocker"), this.font)
                .pos(x, curY).selected(ModConfig.particleBlockerEnabled)
                .onValueChange((c, v) -> { ModConfig.particleBlockerEnabled = v; ConfigManager.save(); })
                .build());
        curY += rowH + rowGap;

        String[][] cats = {
                {"Огонь", "Fire"},
                {"Дым", "Smoke"},
                {"Взрывы", "Explosions"},
                {"Зелья", "Potions"},
                {"Вода", "Water"},
                {"Редстоун", "Redstone"},
                {"Портал", "Portal"},
                {"Криты", "Crits"}
        };
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
                            Component.literal(ModConfig.modLogoRussian ? cats[i][0] : cats[i][1]), this.font)
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

    // ===================== ПАНЕЛИ: MISC =====================

    private void buildChatFilterPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        EditBox wordField = new EditBox(this.font, x, curY, w - 30, 20,
                Component.literal(ModConfig.modLogoRussian ? "Стоп-слово..." : "Stop word..."));
        wordField.setMaxLength(30);
        widgets.add(wordField);

        widgets.add(Button.builder(Component.literal("+"), (b) -> {
            String word = wordField.getValue().trim();
            if (!word.isEmpty()) {
                ChatFilterManager.addWord(word);
                wordField.setValue("");
                rebuildWidgetsPreservingState();
            }
        }).bounds(x + w - 25, curY, 25, 20).build());
        curY += rowH + rowGap;

        java.util.List<String> words = ChatFilterManager.getWords();
        int maxShow = 4;
        int rowListH = 18;

        for (int i = 0; i < Math.min(words.size(), maxShow); i++) {
            final String word = words.get(i);
            Button wordLabel = Button.builder(Component.literal("§e" + word), (b) -> {})
                    .bounds(x, curY, w - 25, rowListH).build();
            wordLabel.active = false;
            widgets.add(wordLabel);

            widgets.add(Button.builder(Component.literal("×"), (b) -> {
                ChatFilterManager.removeWord(word);
                rebuildWidgetsPreservingState();
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
                Component.literal(ModConfig.modLogoRussian ? "Очистить всё" : "Clear all"),
                (b) -> { ChatFilterManager.clearWords(); rebuildWidgetsPreservingState(); }
        ).bounds(x, curY, w, 20).build());
    }

    private void buildAutoReconnectPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Задержка: " : "Delay: ")
                        + ModConfig.autoReconnectDelay + (ModConfig.modLogoRussian ? " сек" : " sec")),
                (ModConfig.autoReconnectDelay - 1) / 29.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Задержка: " : "Delay: ")
                        + ModConfig.autoReconnectDelay + (ModConfig.modLogoRussian ? " сек" : " sec")));
            }
            @Override protected void applyValue() {
                ModConfig.autoReconnectDelay = 1 + (int)(this.value * 29);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Показывать HUD-таймер" : "Show HUD timer"), this.font)
                .pos(x, curY).selected(ModConfig.autoReconnectShowHud)
                .onValueChange((c, v) -> { ModConfig.autoReconnectShowHud = v; ConfigManager.save(); })
                .build());
    }

    private void buildDeathCoordsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        widgets.add(Button.builder(
                Component.literal(ModConfig.modLogoRussian ? "Показать последнюю точку смерти" : "Show last death point"),
                (b) -> DeathCoordsManager.showLastDeath()
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(ModConfig.modLogoRussian ? "Очистить сохранённую точку" : "Clear saved death point"),
                (b) -> DeathCoordsManager.clearLastDeath()
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        String info;
        if (ModConfig.lastDeathTime > 0) {
            info = (ModConfig.modLogoRussian ? "§7Последняя: §e" : "§7Last: §e")
                    + ModConfig.lastDeathX + ", " + ModConfig.lastDeathY + ", " + ModConfig.lastDeathZ;
        } else {
            info = ModConfig.modLogoRussian ? "§8Точек смерти пока нет" : "§8No death points yet";
        }
        Button infoBtn = Button.builder(Component.literal(info), (b) -> {})
                .bounds(x, curY, w, 20).build();
        infoBtn.active = false;
        widgets.add(infoBtn);
    }

    // ===================== ПАНЕЛИ: СЛОЖНЫЕ (HUD) =====================

    private void buildCoolDownsPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        curY = addPosEditorRow(widgets, x, curY, right,
                () -> ModConfig.cooldownsX, () -> ModConfig.cooldownsY,
                (nx, ny) -> { ModConfig.cooldownsX = nx; ModConfig.cooldownsY = ny; },
                10, 200);

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Макс. элементов: " : "Max items: ") + ModConfig.cooldownsMaxItems),
                (ModConfig.cooldownsMaxItems - 1) / 9.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Макс. элементов: " : "Max items: ")
                        + ModConfig.cooldownsMaxItems));
            }
            @Override protected void applyValue() {
                ModConfig.cooldownsMaxItems = 1 + (int)(this.value * 9);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Прозрачность: " : "Alpha: ") + ModConfig.cooldownsAlpha),
                ModConfig.cooldownsAlpha / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Прозрачность: " : "Alpha: ")
                        + ModConfig.cooldownsAlpha));
            }
            @Override protected void applyValue() {
                ModConfig.cooldownsAlpha = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Затемнение иконки: " : "Icon darkening: ") + ModConfig.cooldownsIconDarkening),
                ModConfig.cooldownsIconDarkening / 255.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Затемнение иконки: " : "Icon darkening: ")
                        + ModConfig.cooldownsIconDarkening));
            }
            @Override protected void applyValue() {
                ModConfig.cooldownsIconDarkening = (int)(this.value * 255);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        int cbW = (w - 6) / 3;
        widgets.add(Checkbox.builder(Component.literal(ModConfig.modLogoRussian ? "Иконка" : "Icon"), this.font)
                .pos(x, curY).selected(ModConfig.cooldownsShowIcon)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowIcon = v; ConfigManager.save(); }).build());
        widgets.add(Checkbox.builder(Component.literal(ModConfig.modLogoRussian ? "Название" : "Name"), this.font)
                .pos(x + cbW + 3, curY).selected(ModConfig.cooldownsShowName)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowName = v; ConfigManager.save(); }).build());
        widgets.add(Checkbox.builder(Component.literal(ModConfig.modLogoRussian ? "Таймер" : "Timer"), this.font)
                .pos(x + (cbW + 3) * 2, curY).selected(ModConfig.cooldownsShowTime)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowTime = v; ConfigManager.save(); }).build());
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(Component.literal(ModConfig.modLogoRussian ? "Только хотбар" : "Hotbar only"), this.font)
                .pos(x, curY).selected(ModConfig.cooldownsShowOnlyHotbar)
                .onValueChange((c, v) -> { ModConfig.cooldownsShowOnlyHotbar = v; ConfigManager.save(); }).build());
        curY += rowH + rowGap;

        String[] sizesRu = {"Малый", "Средний", "Крупный"};
        String[] sizesEn = {"Small", "Medium", "Large"};
        int fs = Math.max(0, Math.min(2, ModConfig.cooldownsFontSize));
        widgets.add(Button.builder(
                Component.literal((ModConfig.modLogoRussian ? "Размер: " : "Size: ")
                        + (ModConfig.modLogoRussian ? sizesRu[fs] : sizesEn[fs])),
                (b) -> {
                    ModConfig.cooldownsFontSize = (ModConfig.cooldownsFontSize + 1) % 3;
                    ConfigManager.save();
                    rebuildWidgetsPreservingState();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(Button.builder(
                Component.literal(ModConfig.modLogoRussian ? "Сбросить позицию" : "Reset position"),
                (b) -> {
                    ModConfig.cooldownsX = 10;
                    ModConfig.cooldownsY = 200;
                    ConfigManager.save();
                    rebuildWidgetsPreservingState();
                }
        ).bounds(x, curY, w, 20).build());
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
                Component.literal((ModConfig.modLogoRussian ? "Время сброса: " : "Reset time: ")
                        + ModConfig.comboResetTime + (ModConfig.modLogoRussian ? " сек" : " sec")),
                (ModConfig.comboResetTime - 1) / 4.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Время сброса: " : "Reset time: ")
                        + ModConfig.comboResetTime + (ModConfig.modLogoRussian ? " сек" : " sec")));
            }
            @Override protected void applyValue() {
                ModConfig.comboResetTime = 1 + (int)(this.value * 4);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        String[] sizesRu = {"Малый", "Средний", "Крупный"};
        String[] sizesEn = {"Small", "Medium", "Large"};
        int fs = Math.max(0, Math.min(2, ModConfig.comboFontSize));
        widgets.add(Button.builder(
                Component.literal((ModConfig.modLogoRussian ? "Размер: " : "Size: ")
                        + (ModConfig.modLogoRussian ? sizesRu[fs] : sizesEn[fs])),
                (b) -> {
                    ModConfig.comboFontSize = (ModConfig.comboFontSize + 1) % 3;
                    ConfigManager.save();
                    rebuildWidgetsPreservingState();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        EditBox hexField = new EditBox(this.font, x, curY, 80, 18, Component.literal("#RRGGBB"));
        hexField.setMaxLength(7);
        hexField.setValue(String.format("#%06X", ModConfig.comboColor & 0xFFFFFF));
        widgets.add(hexField);

        widgets.add(Button.builder(Component.literal("OK"), (b) -> {
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

    // ===================== ПАНЕЛИ: СЛОЖНЫЕ (PVP) =====================

    private void buildTotemLogPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String keyName = KeyBindings.totemLogKey != null
                ? KeyBindings.totemLogKey.getTranslatedKeyMessage().getString() : "O";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 10
                        ? (ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key...")
                        : (ModConfig.modLogoRussian ? "Клавиша: " : "Key: ") + keyName),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 10;
                    b.setMessage(Component.literal(ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key..."));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Радиус: " : "Radius: ")
                        + ModConfig.totemLogRadius + (ModConfig.modLogoRussian ? " блоков" : " blocks")),
                (ModConfig.totemLogRadius - 5) / 15.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Радиус: " : "Radius: ")
                        + ModConfig.totemLogRadius + (ModConfig.modLogoRussian ? " блоков" : " blocks")));
            }
            @Override protected void applyValue() {
                ModConfig.totemLogRadius = 5 + (int)(this.value * 15.0);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(Checkbox.builder(
                        Component.literal(ModConfig.modLogoRussian ? "Звук-уведомление" : "Sound notification"), this.font)
                .pos(x, curY).selected(ModConfig.totemLogSound)
                .onValueChange((c, v) -> { ModConfig.totemLogSound = v; ConfigManager.save(); })
                .build());
    }

    private void buildAutoSwapPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String[] modesRu = {"Шар ↔ Шар", "Тотем ↔ Тотем", "Шар ↔ Тотем", "Тотем ↔ Шар"};
        String[] modesEn = {"Head ↔ Head", "Totem ↔ Totem", "Head ↔ Totem", "Totem ↔ Head"};
        int halfW = (w - 3) / 2;
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            int col = i % 2;
            int row = i / 2;
            widgets.add(Button.builder(
                    Component.literal(ModConfig.modLogoRussian ? modesRu[i] : modesEn[i]),
                    (b) -> {
                        ModConfig.autoSwapMode = idx;
                        ConfigManager.save();
                        rebuildWidgetsPreservingState();
                    }
            ).bounds(x + col * (halfW + 3), curY + row * (rowH - 2), halfW, 20).build());
        }
        curY += rowH * 2 + rowGap - 2;

        String keyName = KeyBindings.autoSwapKey != null
                ? KeyBindings.autoSwapKey.getTranslatedKeyMessage().getString() : "H";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 3
                        ? (ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key...")
                        : (ModConfig.modLogoRussian ? "Клавиша: " : "Key: ") + keyName),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 3;
                    b.setMessage(Component.literal(ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key..."));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Задержка открытия: " : "Open delay: ")
                        + ModConfig.autoSwapOpenDelay + (ModConfig.modLogoRussian ? " мс" : " ms")),
                (ModConfig.autoSwapOpenDelay - 50) / 450.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Задержка открытия: " : "Open delay: ")
                        + ModConfig.autoSwapOpenDelay + (ModConfig.modLogoRussian ? " мс" : " ms")));
            }
            @Override protected void applyValue() {
                ModConfig.autoSwapOpenDelay = 50 + (int)(this.value * 450);
                this.updateMessage();
                ConfigManager.save();
            }
        });
        curY += rowH + rowGap;

        widgets.add(new AbstractSliderButton(x, curY, w, 20,
                Component.literal((ModConfig.modLogoRussian ? "Cooldown: " : "Cooldown: ")
                        + ModConfig.autoSwapCooldown + (ModConfig.modLogoRussian ? " мс" : " ms")),
                (ModConfig.autoSwapCooldown - 100) / 1900.0
        ) {
            @Override protected void updateMessage() {
                this.setMessage(Component.literal((ModConfig.modLogoRussian ? "Cooldown: " : "Cooldown: ")
                        + ModConfig.autoSwapCooldown + (ModConfig.modLogoRussian ? " мс" : " ms")));
            }
            @Override protected void applyValue() {
                ModConfig.autoSwapCooldown = 100 + (int)(this.value * 1900);
                this.updateMessage();
                ConfigManager.save();
            }
        });
    }

    private void buildPickUpLoggerPanel(List<AbstractWidget> widgets, int x, int y, int right) {
        int w = right - x;
        int rowH = 22, rowGap = 6;
        int curY = y;

        String keyName = KeyBindings.pickupLogKey != null
                ? KeyBindings.pickupLogKey.getTranslatedKeyMessage().getString() : "P";
        widgets.add(Button.builder(
                Component.literal(ModConfig.isBindingKey && ModConfig.bindingTarget == 11
                        ? (ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key...")
                        : (ModConfig.modLogoRussian ? "Клавиша: " : "Key: ") + keyName),
                (b) -> {
                    ModConfig.isBindingKey = true;
                    ModConfig.bindingTarget = 11;
                    b.setMessage(Component.literal(ModConfig.modLogoRussian ? "Нажмите клавишу..." : "Press a key..."));
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        String[] modesRu = {"Все предметы", "Только ценные", "По категориям"};
        String[] modesEn = {"All items", "Valuable only", "By categories"};
        int mode = Math.max(0, Math.min(2, ModConfig.pickupLogMode));
        widgets.add(Button.builder(
                Component.literal((ModConfig.modLogoRussian ? "Режим: " : "Mode: ")
                        + (ModConfig.modLogoRussian ? modesRu[mode] : modesEn[mode])),
                (b) -> {
                    ModConfig.pickupLogMode = (ModConfig.pickupLogMode + 1) % 3;
                    ConfigManager.save();
                    rebuildWidgetsPreservingState();
                }
        ).bounds(x, curY, w, 20).build());
        curY += rowH + rowGap;

        if (ModConfig.pickupLogMode == 2) {
            int cbW = (w - 6) / 3;
            String[][] cats = {
                    {"Оружие", "Weapon"}, {"Броня", "Armor"}, {"Зелья", "Potions"},
                    {"Талисманы", "Totems"}, {"Головы", "Heads"}, {"Спавнеры", "Spawners"},
                    {"Блоки структур", "Structure blocks"}
            };
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
                                Component.literal(ModConfig.modLogoRussian ? cats[i][0] : cats[i][1]), this.font)
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

    // ===================== РЕНДЕР =====================
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        hoverColumn = isMouseOverColumn(mouseX, mouseY);
        float targetWidth = hoverColumn ? COLUMN_EXPANDED : COLUMN_COLLAPSED;
        columnWidth += (targetWidth - columnWidth) * 0.25f;

        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xC0000000);

        drawPanelBorders(graphics);
        graphics.drawString(this.font, "§lResistance DLC", panelX + 15, panelY + 11, ModConfig.guiColor, true);

        drawColumn(graphics, mouseX, mouseY);
        drawContent(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, delta);
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
            graphics.drawString(this.font, ModConfig.modLogoRussian ? "§lПоиск" : "§lSearch",
                    searchTextX, searchNameY, 0xFFEEEEEE, true);
            if (columnWidth > 140) {
                graphics.drawString(this.font, ModConfig.modLogoRussian ? "§7Найти настройку" : "§7Find a setting",
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

        int listTop = contentTop + 42;
        int listBottom = contentBottom - 4;
        int listLeft = contentLeft + 5;
        int listRight = contentRight - 5;

        int totalHeight = 0;
        int itemHeight = 26;
        int gap = 2;
        for (AccordionItem item : active.items) {
            totalHeight += itemHeight + gap;
            if (item.expanded) totalHeight += item.contentHeight + gap;
        }
        int visibleHeight = listBottom - listTop;
        maxContentScroll = Math.max(0, totalHeight - visibleHeight);

        if (contentScroll > maxContentScroll) contentScroll = maxContentScroll;

        int itemY = listTop - contentScroll;

        for (AccordionItem item : active.items) {
            int itemTop = itemY;
            int itemBottom = itemY + itemHeight;

            if (itemBottom > listTop && itemTop < listBottom) {
                boolean isHover = mouseX >= listLeft && mouseX <= listRight
                        && mouseY >= itemTop && mouseY <= itemBottom
                        && mouseY >= listTop && mouseY <= listBottom;

                drawAccordionItemClipped(graphics, item, listLeft, itemTop, listRight, itemHeight,
                        isHover, listTop, listBottom);
            }

            itemY += itemHeight + gap;

            if (item.expanded) {
                int panelTop = itemY;
                int panelBottom = itemY + item.contentHeight;

                if (panelBottom > listTop && panelTop < listBottom) {
                    drawItemPanelClipped(graphics, item, listLeft - 10, panelTop,
                            listRight + 10, listTop, listBottom);
                }

                itemY += item.contentHeight + gap;
            }
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

        graphics.fill(left, visibleTop, right, visibleBottom, 0x50000000);
        graphics.fill(left, visibleTop, right, visibleTop + 1, 0xFF404040);
        graphics.fill(left, visibleBottom - 1, right, visibleBottom, 0xFF404040);
        graphics.fill(left, visibleTop, left + 1, visibleBottom, 0xFF404040);
        graphics.fill(right - 1, visibleTop, right, visibleBottom, 0xFF404040);

        if (isHover) graphics.fill(left, visibleTop, right, visibleBottom, 0x30FFFFFF);
        if (item.expanded) graphics.fill(left, visibleTop, left + 3, visibleBottom, ModConfig.guiColor);

        if (top + 8 >= clipTop && top + 8 <= clipBottom) {
            String arrow = item.expanded ? "▼" : "▶";
            graphics.drawString(this.font, arrow, left + 8, top + 8, ModConfig.guiColor, true);
        }
        if (top + 3 >= clipTop && top + 3 <= clipBottom) {
            graphics.drawString(this.font, "§l" + item.title, left + 22, top + 3, 0xFFFFFFFF, true);
        }
        if (top + 14 >= clipTop && top + 14 <= clipBottom) {
            graphics.drawString(this.font, "§7" + item.description, left + 22, top + 14, 0xFFAAAAAA, false);
        }
        if (top + 6 >= clipTop && top + 6 <= clipBottom) {
            boolean status = item.statusGetter.get();
            String statusText = status ? "§a[ON]" : "§7[OFF]";
            int statusWidth = this.font.width(statusText);
            graphics.drawString(this.font, statusText, right - statusWidth - 10, top + 6, 0xFFFFFFFF, true);
        }
    }

    private void drawItemPanelClipped(GuiGraphics graphics, AccordionItem item,
                                      int left, int panelY, int right,
                                      int clipTop, int clipBottom) {
        int top = panelY;
        int bottom = panelY + item.contentHeight;

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

        graphics.fill(left, top, right, bottom, 0x50000000);
        graphics.fill(left, top, right, top + 1, 0xFF404040);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF404040);
        graphics.fill(left, top, left + 1, bottom, 0xFF404040);
        graphics.fill(right - 1, top, right, bottom, 0xFF404040);

        if (isHover) graphics.fill(left, top, right, bottom, 0x30FFFFFF);
        if (item.expanded) graphics.fill(left, top, left + 3, bottom, ModConfig.guiColor);

        String arrow = item.expanded ? "▼" : "▶";
        graphics.drawString(this.font, arrow, left + 8, top + 8, ModConfig.guiColor, true);
        graphics.drawString(this.font, "§l" + item.title, left + 22, top + 3, 0xFFFFFFFF, true);
        graphics.drawString(this.font, "§7" + item.description, left + 22, top + 14, 0xFFAAAAAA, false);

        boolean status = item.statusGetter.get();
        String statusText = status ? "§a[ON]" : "§7[OFF]";
        int statusWidth = this.font.width(statusText);
        graphics.drawString(this.font, statusText, right - statusWidth - 10, top + 6, 0xFFFFFFFF, true);
    }

    // ===================== СКРОЛЛ =====================
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        int contentLeft = getContentLeft();
        int contentTop = getContentTop();
        int contentRight = getContentRight();
        int contentBottom = getContentBottom();

        boolean overContent = mouseX >= contentLeft && mouseX <= contentRight
                && mouseY >= contentTop && mouseY <= contentBottom;

        if (overContent && maxContentScroll > 0) {
            contentScroll -= (int)(verticalAmount * 20);
            if (contentScroll < 0) contentScroll = 0;
            if (contentScroll > maxContentScroll) contentScroll = maxContentScroll;
            rebuildAllPanelWidgets();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // ===================== КЛИКИ =====================
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

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
                        }
                    }
                    activeSectionIndex = i;
                    contentScroll = 0;
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
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("§e[Search] §7Поиск — в разработке (Шаг 8)"), true);
                }
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

        Section active = sections.get(activeSectionIndex);

        // Защита: клик внутри раскрытой панели — не наш
        int checkY = contentTop + 42 - contentScroll;
        int itemHeight = 26;
        int gap = 2;

        for (AccordionItem item : active.items) {
            checkY += itemHeight + gap;
            if (item.expanded) {
                int panelTop = checkY;
                int panelBottom = checkY + item.contentHeight;
                if (mouseX >= contentLeft + 15 && mouseX <= contentRight - 15
                        && mouseY >= panelTop && mouseY <= panelBottom) {
                    return false;
                }
                checkY += item.contentHeight + gap;
            }
        }

        int itemY = contentTop + 42 - contentScroll;

        for (AccordionItem item : active.items) {
            int top = itemY;
            int bottom = itemY + itemHeight;

            if (mouseX >= contentLeft + 5 && mouseX <= contentRight - 5
                    && mouseY >= top && mouseY <= bottom) {

                if (bottom < contentTop + 42 || top > contentBottom) {
                    itemY += itemHeight + gap;
                    if (item.expanded) itemY += item.contentHeight + gap;
                    continue;
                }

                if (button == 1) {
                    item.toggler.run();
                } else {
                    item.expanded = !item.expanded;
                    if (item.expanded) {
                        buildPanelWidgets(item);
                    } else {
                        clearPanelWidgets(item);
                    }
                }
                return true;
            }

            itemY += itemHeight + gap;
            if (item.expanded) {
                itemY += item.contentHeight + gap;
            }
        }

        return false;
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
}