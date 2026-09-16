package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * AccordionScreen — новый GUI мода с боковой панелью разделов
 * и аккордеон-меню функций.
 *
 * Шаг 1: каркас + колонка + hover.
 * Шаг 4: список функций внутри раздела + раскрытие + ПКМ toggle.
 */
public class AccordionScreen extends Screen {

    // ===================== РАЗМЕРЫ ПАНЕЛИ =====================
    private static final int PANEL_WIDTH = 620;
    private static final int PANEL_HEIGHT = 460;
    private static final int COLUMN_COLLAPSED = 60;
    private static final int COLUMN_EXPANDED = 220;
    private static final int HEADER_HEIGHT = 30;

    // ===================== ПОЗИЦИИ =====================
    private int panelX;
    private int panelY;

    // ===================== СОСТОЯНИЕ =====================
    private int activeSectionIndex = 0;
    private float columnWidth = COLUMN_COLLAPSED;
    private boolean hoverColumn = false;

    // ===================== МОДЕЛЬ: ФУНКЦИЯ =====================
    public static class AccordionItem {
        public final String id;
        public final String title;
        public final String description;
        public final Supplier<Boolean> statusGetter;
        public final Runnable toggler;
        public boolean expanded = false;
        public int contentHeight = 100;
        public final java.util.function.BiConsumer<AccordionScreen, int[]> panelRenderer;

        public AccordionItem(String id, String title, String description,
                             Supplier<Boolean> statusGetter,
                             Runnable toggler,
                             java.util.function.BiConsumer<AccordionScreen, int[]> panelRenderer) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.statusGetter = statusGetter;
            this.toggler = toggler;
            this.panelRenderer = panelRenderer;
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

    // ===================== КОНСТРУКТОР =====================
    public AccordionScreen() {
        super(Component.literal("Resistance DLC"));
    }

    // ===================== ПЕРЕСОЗДАНИЕ РАЗДЕЛОВ =====================
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
                () -> { ModConfig.noHurtCamEnabled = !ModConfig.noHurtCamEnabled; ConfigManager.save(); },
                null
        ));
        hud.items.add(new AccordionItem(
                "no_bobbing", "No Bobbing",
                ModConfig.modLogoRussian ? "Убирает покачивание камеры" : "Removes camera bobbing",
                () -> ModConfig.noBobbingEnabled,
                () -> { ModConfig.noBobbingEnabled = !ModConfig.noBobbingEnabled; ConfigManager.save(); },
                null
        ));
        hud.items.add(new AccordionItem(
                "cooldowns", "CoolDowns",
                ModConfig.modLogoRussian ? "Показывает кулдауны предметов" : "Shows item cooldowns",
                () -> ModConfig.cooldownsEnabled,
                () -> { ModConfig.cooldownsEnabled = !ModConfig.cooldownsEnabled; ConfigManager.save(); },
                null
        ));
        hud.items.add(new AccordionItem(
                "combo", "Combo Counter",
                ModConfig.modLogoRussian ? "Счётчик комбо" : "Combo counter",
                () -> ModConfig.comboEnabled,
                () -> { ModConfig.comboEnabled = !ModConfig.comboEnabled; ConfigManager.save(); },
                null
        ));
        sections.add(hud);

        // ===================== PVP =====================
        Section pvp = new Section("pvp", "PvP",
                ModConfig.modLogoRussian ? "Тотем-лог, автосвап" : "Totem log, autoswap",
                new ItemStack(Items.DIAMOND_SWORD));

        pvp.items.add(new AccordionItem(
                "totem_log", "Totem Log",
                ModConfig.modLogoRussian ? "Лог сбитых тотемов" : "Log of broken totems",
                () -> ModConfig.totemLogEnabled,
                () -> { ModConfig.totemLogEnabled = !ModConfig.totemLogEnabled; ConfigManager.save(); },
                null
        ));
        pvp.items.add(new AccordionItem(
                "auto_swap", "AutoSwap",
                ModConfig.modLogoRussian ? "Свап offhand ↔ инвентарь" : "Offhand ↔ inventory swap",
                () -> ModConfig.autoSwapEnabled,
                () -> { ModConfig.autoSwapEnabled = !ModConfig.autoSwapEnabled; ConfigManager.save(); },
                null
        ));
        sections.add(pvp);

        // ===================== PVE =====================
        Section pve = new Section("pve", "PvE",
                ModConfig.modLogoRussian ? "Кликеры, скролл" : "Clickers, scroller",
                new ItemStack(Items.CARROT));

        pve.items.add(new AccordionItem(
                "tape_mouse", "TapeMouse",
                ModConfig.modLogoRussian ? "Автокликер" : "Autoclicker",
                () -> ModConfig.tapeMouseEnabled,
                () -> { ModConfig.tapeMouseEnabled = !ModConfig.tapeMouseEnabled; ConfigManager.save(); },
                null
        ));
        pve.items.add(new AccordionItem(
                "item_scroller", "ItemScroller",
                ModConfig.modLogoRussian ? "Быстрый перенос скроллом" : "Fast scroll transfer",
                () -> ModConfig.itemScrollerEnabled,
                () -> { ModConfig.itemScrollerEnabled = !ModConfig.itemScrollerEnabled; ConfigManager.save(); },
                null
        ));
        sections.add(pve);

        // ===================== VISUAL =====================
        Section visual = new Section("visual", "Visual",
                ModConfig.modLogoRussian ? "Зум, прицел, хитбоксы" : "Zoom, crosshair, hitboxes",
                new ItemStack(Items.ENDER_EYE));

        visual.items.add(new AccordionItem(
                "zoom", "Zoom",
                ModConfig.modLogoRussian ? "Плавное приближение" : "Smooth zoom",
                () -> ModConfig.zoomEnabled,
                () -> { ModConfig.zoomEnabled = !ModConfig.zoomEnabled; ConfigManager.save(); },
                null
        ));
        visual.items.add(new AccordionItem(
                "crosshair", "Custom Crosshair",
                ModConfig.modLogoRussian ? "Кастомный прицел" : "Custom crosshair",
                () -> ModConfig.crosshairEnabled,
                () -> { ModConfig.crosshairEnabled = !ModConfig.crosshairEnabled; ConfigManager.save(); },
                null
        ));
        visual.items.add(new AccordionItem(
                "custom_hitbox", "Custom Hitbox",
                ModConfig.modLogoRussian ? "Цвет debug-хитбоксов" : "Debug hitbox color",
                () -> ModConfig.customHitboxEnabled,
                () -> { ModConfig.customHitboxEnabled = !ModConfig.customHitboxEnabled; ConfigManager.save(); },
                null
        ));
        visual.items.add(new AccordionItem(
                "item_physics", "ItemPhysics",
                ModConfig.modLogoRussian ? "Предметы лежат плашмя" : "Items lie flat",
                () -> ModConfig.itemPhysicsEnabled,
                () -> { ModConfig.itemPhysicsEnabled = !ModConfig.itemPhysicsEnabled; ConfigManager.save(); },
                null
        ));
        sections.add(visual);

        // ===================== MISC =====================
        Section misc = new Section("misc", "Misc",
                ModConfig.modLogoRussian ? "Фильтр, автореконнект" : "Filter, auto-reconnect",
                new ItemStack(Items.REDSTONE));

        misc.items.add(new AccordionItem(
                "chat_filter", "ChatFilter",
                ModConfig.modLogoRussian ? "Фильтр чата" : "Chat filter",
                () -> ModConfig.chatFilterEnabled,
                () -> { ModConfig.chatFilterEnabled = !ModConfig.chatFilterEnabled; ConfigManager.save(); },
                null
        ));
        misc.items.add(new AccordionItem(
                "auto_reconnect", "AutoReconnect",
                ModConfig.modLogoRussian ? "Авто-переподключение" : "Auto reconnection",
                () -> ModConfig.autoReconnectEnabled,
                () -> { ModConfig.autoReconnectEnabled = !ModConfig.autoReconnectEnabled; ConfigManager.save(); },
                null
        ));
        misc.items.add(new AccordionItem(
                "death_coords", "DeathCoords",
                ModConfig.modLogoRussian ? "Координаты смерти" : "Death coords",
                () -> ModConfig.deathCoordsEnabled,
                () -> { ModConfig.deathCoordsEnabled = !ModConfig.deathCoordsEnabled; ConfigManager.save(); },
                null
        ));
        sections.add(misc);
    }

    // ===================== INIT =====================
    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        // ГАРАНТИРОВАННО пересоздаём секции с текущим языком
        sections.clear();
        initSections();

        // Кнопка RU/EN
        Button langBtn = Button.builder(
                        Component.literal(ModConfig.modLogoRussian ? "EN" : "RU"),
                        (b) -> {
                            ModConfig.modLogoRussian = !ModConfig.modLogoRussian;
                            ConfigManager.save();
                            this.rebuildWidgets();
                        })
                .bounds(panelX + PANEL_WIDTH - 70, panelY + 6, 30, 18).build();
        this.addRenderableWidget(langBtn);

        // Кнопка [×]
        Button closeBtn = Button.builder(
                        Component.literal("×"),
                        (b) -> this.onClose())
                .bounds(panelX + PANEL_WIDTH - 35, panelY + 6, 30, 18).build();
        this.addRenderableWidget(closeBtn);
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

        graphics.drawString(this.font, "§lResistance DLC",
                panelX + 15, panelY + 11, ModConfig.guiColor, true);

        drawColumn(graphics, mouseX, mouseY);
        drawContent(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawPanelBorders(GuiGraphics graphics) {
        int color = ModConfig.guiColor;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 2, color);
        graphics.fill(panelX, panelY + PANEL_HEIGHT - 2,
                panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, color);
        graphics.fill(panelX, panelY, panelX + 2, panelY + PANEL_HEIGHT, color);
        graphics.fill(panelX + PANEL_WIDTH - 2, panelY,
                panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, color);
    }

    // ===================== БОКОВАЯ КОЛОНКА =====================
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

                graphics.drawString(this.font, "§l" + section.name,
                        textX, nameY, nameColor, true);

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

            graphics.drawString(this.font,
                    ModConfig.modLogoRussian ? "§lПоиск" : "§lSearch",
                    searchTextX, searchNameY, 0xFFEEEEEE, true);

            if (columnWidth > 140) {
                graphics.drawString(this.font,
                        ModConfig.modLogoRussian ? "§7Найти настройку" : "§7Find a setting",
                        searchTextX, searchNameY + 12, 0xFFAAAAAA, false);
            }
        }
    }

    // ===================== КОНТЕНТ СПРАВА =====================
    private void drawContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int contentLeft = panelX + (int) columnWidth + 10;
        int contentTop = panelY + HEADER_HEIGHT + 10;
        int contentRight = panelX + PANEL_WIDTH - 12;
        int contentBottom = panelY + PANEL_HEIGHT - 12;

        graphics.fill(contentLeft, contentTop, contentRight, contentTop + 1, 0xFF303030);
        graphics.fill(contentLeft, contentBottom - 1, contentRight, contentBottom, 0xFF303030);
        graphics.fill(contentLeft, contentTop, contentLeft + 1, contentBottom, 0xFF303030);
        graphics.fill(contentRight - 1, contentTop, contentRight, contentBottom, 0xFF303030);

        Section active = sections.get(activeSectionIndex);

        graphics.drawString(this.font, "§l▸ " + active.name,
                contentLeft + 15, contentTop + 8, ModConfig.guiColor, true);
        graphics.drawString(this.font, "§7" + active.description,
                contentLeft + 15, contentTop + 22, 0xFFAAAAAA, false);

        int itemY = contentTop + 42;
        int itemHeight = 26;
        int gap = 2;

        for (AccordionItem item : active.items) {
            boolean isHover = mouseX >= contentLeft + 5 && mouseX <= contentRight - 5
                    && mouseY >= itemY && mouseY <= itemY + itemHeight;

            drawAccordionItem(graphics, item, contentLeft, itemY, contentRight, itemHeight, isHover);
            itemY += itemHeight + gap;

            if (item.expanded) {
                drawItemPanel(graphics, item, contentLeft, itemY, contentRight);
                itemY += item.contentHeight + gap;
            }
        }
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

        if (isHover) {
            graphics.fill(left, top, right, bottom, 0x30FFFFFF);
        }

        if (item.expanded) {
            graphics.fill(left, top, left + 3, bottom, ModConfig.guiColor);
        }

        String arrow = item.expanded ? "▼" : "▶";
        graphics.drawString(this.font, arrow, left + 8, top + 8, ModConfig.guiColor, true);

        graphics.drawString(this.font, "§l" + item.title,
                left + 22, top + 3, 0xFFFFFFFF, true);

        graphics.drawString(this.font, "§7" + item.description,
                left + 22, top + 14, 0xFFAAAAAA, false);

        boolean status = item.statusGetter.get();
        String statusText = status ? "§a[ON]" : "§7[OFF]";
        int statusWidth = this.font.width(statusText);
        graphics.drawString(this.font, statusText,
                right - statusWidth - 10, top + 6, 0xFFFFFFFF, true);
    }

    private void drawItemPanel(GuiGraphics graphics, AccordionItem item,
                               int contentLeft, int panelY, int contentRight) {
        int left = contentLeft + 15;
        int right = contentRight - 15;
        int top = panelY;
        int bottom = panelY + item.contentHeight;

        graphics.fill(left, top, right, bottom, 0x30000000);
        graphics.fill(left, top, right, top + 1, 0xFF505050);
        graphics.fill(left, bottom - 1, right, bottom, 0xFF505050);
        graphics.fill(left, top, left + 1, bottom, 0xFF505050);
        graphics.fill(right - 1, top, right, bottom, 0xFF505050);

        if (item.panelRenderer != null) {
            int[] bounds = new int[]{left + 10, top + 10, right - 10};
            item.panelRenderer.accept(this, bounds);
        } else {
            graphics.drawString(this.font,
                    ModConfig.modLogoRussian
                            ? "§8[Панель настроек — Шаг 5]"
                            : "§8[Settings panel — Step 5]",
                    left + 15, top + 12, 0xFF606060, false);
        }
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
                    activeSectionIndex = i;
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

// Клик нигде не поглощён — отдаём в super (для кнопок [RU], [×])
        return super.mouseClicked(event, isDoubleClick);
    }

    private boolean handleContentClick(double mouseX, double mouseY, int button) {
        int contentLeft = panelX + (int) columnWidth + 10;
        int contentTop = panelY + HEADER_HEIGHT + 10;
        int contentRight = panelX + PANEL_WIDTH - 12;

        if (mouseX < contentLeft || mouseX > contentRight) return false;

        Section active = sections.get(activeSectionIndex);
        int itemY = contentTop + 42;
        int itemHeight = 26;
        int gap = 2;

        for (AccordionItem item : active.items) {
            int top = itemY;
            int bottom = itemY + itemHeight;

            if (mouseX >= contentLeft + 5 && mouseX <= contentRight - 5
                    && mouseY >= top && mouseY <= bottom) {

                if (button == 1) {
                    item.toggler.run();
                } else {
                    item.expanded = !item.expanded;
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

    // ===================== УТИЛИТЫ =====================
    private boolean isMouseOverColumn(double mouseX, double mouseY) {
        float targetWidth = hoverColumn ? COLUMN_EXPANDED : COLUMN_COLLAPSED;
        int colLeft = panelX + 2;
        int colTop = panelY + HEADER_HEIGHT;
        int colRight = panelX + (int) targetWidth;
        int colBottom = panelY + PANEL_HEIGHT - 2;
        return mouseX >= colLeft && mouseX <= colRight
                && mouseY >= colTop && mouseY <= colBottom;
    }

    // ===================== OVERRIDES =====================
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}