package com.resistancedlc.hud;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * HudWidgetRegistry — собирает все HUD-виджеты мода.
 *
 * Итерация 3: поддержка drag-n-drop (raw + effective coordinates).
 */
public final class HudWidgetRegistry {

    private HudWidgetRegistry() {}

    public static List<HudWidget> buildAll() {
        List<HudWidget> list = new ArrayList<>();

        // ===================== ПРОСТЫЕ =====================

        list.add(new SimpleTextWidget(
                "mod_logo", "Mod Logo",
                () -> ModConfig.modLogoX, () -> ModConfig.modLogoY,
                (x, y) -> { ModConfig.modLogoX = x; ModConfig.modLogoY = y; },
                10, 5,
                () -> ModConfig.showModLogo,
                v -> ModConfig.showModLogo = v,
                "ResistanceDLC", 16
        ));

        list.add(new SimpleTextWidget(
                "coords", "XYZ",
                () -> ModConfig.coordsX, () -> ModConfig.coordsY,
                (x, y) -> { ModConfig.coordsX = x; ModConfig.coordsY = y; },
                10, 35,
                () -> ModConfig.showCoords,
                v -> ModConfig.showCoords = v,
                "XYZ: 100, 64, 200", 0
        ));

        list.add(new SimpleTextWidget(
                "biome", "Biome",
                () -> ModConfig.biomeX, () -> ModConfig.biomeY,
                (x, y) -> { ModConfig.biomeX = x; ModConfig.biomeY = y; },
                10, 50,
                () -> ModConfig.showBiome,
                v -> ModConfig.showBiome = v,
                "Biome: plains", 0
        ));

        list.add(new SimpleTextWidget(
                "time", "Time",
                () -> ModConfig.timeX, () -> ModConfig.timeY,
                (x, y) -> { ModConfig.timeX = x; ModConfig.timeY = y; },
                10, 65,
                () -> ModConfig.showTime,
                v -> ModConfig.showTime = v,
                "Time: Day", 0
        ));

        list.add(new SimpleTextWidget(
                "fps", "FPS",
                () -> ModConfig.fpsX, () -> ModConfig.fpsY,
                (x, y) -> { ModConfig.fpsX = x; ModConfig.fpsY = y; },
                10, 80,
                () -> ModConfig.showFps,
                v -> ModConfig.showFps = v,
                "FPS: 120", 0
        ));

        list.add(new SimpleTextWidget(
                "ping", "Ping",
                () -> ModConfig.pingX, () -> ModConfig.pingY,
                (x, y) -> { ModConfig.pingX = x; ModConfig.pingY = y; },
                10, 95,
                () -> ModConfig.showPing,
                v -> ModConfig.showPing = v,
                "Ping: 42 ms", 0
        ));

        list.add(new SimpleTextWidget(
                "tps", "TPS",
                () -> ModConfig.tpsX, () -> ModConfig.tpsY,
                (x, y) -> { ModConfig.tpsX = x; ModConfig.tpsY = y; },
                10, 110,
                () -> ModConfig.showTps,
                v -> ModConfig.showTps = v,
                "TPS: 20.0", 0
        ));

        list.add(new SimpleTextWidget(
                "bps", "BPS",
                () -> ModConfig.bpsX, () -> ModConfig.bpsY,
                (x, y) -> { ModConfig.bpsX = x; ModConfig.bpsY = y; },
                10, 125,
                () -> ModConfig.showBps,
                v -> ModConfig.showBps = v,
                "BPS: 4.32", 0
        ));

        list.add(new SimpleTextWidget(
                "direction", "Direction",
                () -> ModConfig.directionX, () -> ModConfig.directionY,
                (x, y) -> { ModConfig.directionX = x; ModConfig.directionY = y; },
                10, 140,
                () -> ModConfig.showDirection,
                v -> ModConfig.showDirection = v,
                "Direction: North", 0
        ));

        list.add(new SimpleTextWidget(
                "hits", "Hits to kill",
                () -> ModConfig.hitCounterX, () -> ModConfig.hitCounterY,
                (x, y) -> { ModConfig.hitCounterX = x; ModConfig.hitCounterY = y; },
                10, 155,
                () -> ModConfig.showHitCounter,
                v -> ModConfig.showHitCounter = v,
                "Hits: 3", 0
        ));

        list.add(new SimpleTextWidget(
                "combo", "Combo",
                () -> ModConfig.comboX, () -> ModConfig.comboY,
                (x, y) -> { ModConfig.comboX = x; ModConfig.comboY = y; },
                10, 185,
                () -> ModConfig.comboEnabled,
                v -> ModConfig.comboEnabled = v,
                "Combo: 5", 0
        ));

        list.add(new SimpleTextWidget(
                "pvp_safe", "PvP Safe",
                () -> ModConfig.pvpSafeHudX, () -> ModConfig.pvpSafeHudY,
                (x, y) -> { ModConfig.pvpSafeHudX = x; ModConfig.pvpSafeHudY = y; },
                10, 240,
                () -> ModConfig.pvpSafeEnabled && ModConfig.pvpSafeShowHud,
                v -> { ModConfig.pvpSafeEnabled = v; ModConfig.pvpSafeShowHud = v; },
                "In combat: 15s", 0
        ));

        list.add(new SimpleTextWidget(
                "kill_streak", "Kill Streak",
                () -> ModConfig.killStreakHudX, () -> ModConfig.killStreakHudY,
                (x, y) -> { ModConfig.killStreakHudX = x; ModConfig.killStreakHudY = y; },
                -1, 10,
                () -> ModConfig.killStreakEnabled,
                v -> ModConfig.killStreakEnabled = v,
                "Kill Streak: 5 (12s)", 0
        ));

        list.add(new SimpleTextWidget(
                "strike_range", "Strike Range",
                () -> ModConfig.strikeRangeX, () -> ModConfig.strikeRangeY,
                (x, y) -> { ModConfig.strikeRangeX = x; ModConfig.strikeRangeY = y; },
                10, 300,
                () -> ModConfig.strikeRangeEnabled,
                v -> ModConfig.strikeRangeEnabled = v,
                "3.42 blocks", 0
        ));

        list.add(new SimpleTextWidget(
                "low_hp_alert", "Low HP Alert",
                () -> ModConfig.lowHpAlertX, () -> ModConfig.lowHpAlertY,
                (x, y) -> { ModConfig.lowHpAlertX = x; ModConfig.lowHpAlertY = y; },
                -1, -1,
                () -> ModConfig.lowHpAlertEnabled,
                v -> ModConfig.lowHpAlertEnabled = v,
                "LOW HP: 4", 0
        ));

        list.add(new SimpleTextWidget(
                "armor_alert", "Armor Alert",
                () -> ModConfig.armorAlertX, () -> ModConfig.armorAlertY,
                (x, y) -> { ModConfig.armorAlertX = x; ModConfig.armorAlertY = y; },
                -1, -1,
                () -> ModConfig.armorAlertEnabled,
                v -> ModConfig.armorAlertEnabled = v,
                "ARMOR: 12%", 0
        ));

        // ===================== СЛОЖНЫЕ (рамки) =====================

        list.add(new FramedWidget(
                "potion_effects", "Potion Effects",
                () -> ModConfig.potionEffectsX, () -> ModConfig.potionEffectsY,
                (x, y) -> { ModConfig.potionEffectsX = x; ModConfig.potionEffectsY = y; },
                10, 170, 140, 60,
                () -> ModConfig.showPotionEffects,
                v -> ModConfig.showPotionEffects = v
        ));

        list.add(new FramedWidget(
                "equipment_hud", "Equipment HUD",
                () -> ModConfig.equipmentHudX, () -> ModConfig.equipmentHudY,
                (x, y) -> { ModConfig.equipmentHudX = x; ModConfig.equipmentHudY = y; },
                4, -44, 24, 120,
                () -> ModConfig.showEquipmentHud,
                v -> ModConfig.showEquipmentHud = v
        ));

        list.add(new FramedWidget(
                "effect_warnings", "Effect Warnings",
                () -> ModConfig.effectWarningsX, () -> ModConfig.effectWarningsY,
                (x, y) -> { ModConfig.effectWarningsX = x; ModConfig.effectWarningsY = y; },
                300, 200, 160, 60,
                () -> ModConfig.effectWarningsEnabled,
                v -> ModConfig.effectWarningsEnabled = v
        ));

        list.add(new FramedWidget(
                "cooldowns", "Cooldowns",
                () -> ModConfig.cooldownsX, () -> ModConfig.cooldownsY,
                (x, y) -> { ModConfig.cooldownsX = x; ModConfig.cooldownsY = y; },
                10, 200, 180, 100,
                () -> ModConfig.cooldownsEnabled,
                v -> ModConfig.cooldownsEnabled = v
        ));

        list.add(new FramedWidget(
                "friend_list", "Friend List",
                () -> ModConfig.friendListHudX, () -> ModConfig.friendListHudY,
                (x, y) -> { ModConfig.friendListHudX = x; ModConfig.friendListHudY = y; },
                -1, 10, 160, 100,
                () -> ModConfig.friendListEnabled && ModConfig.friendListShowHud,
                v -> { ModConfig.friendListEnabled = v; ModConfig.friendListShowHud = v; }
        ));

        list.add(new FramedWidget(
                "music_player", "Music Player",
                () -> ModConfig.musicHudX, () -> ModConfig.musicHudY,
                (x, y) -> { ModConfig.musicHudX = x; ModConfig.musicHudY = y; },
                -1, 10, 270, 70,
                () -> ModConfig.musicPlayerEnabled && ModConfig.musicShowHud,
                v -> { ModConfig.musicPlayerEnabled = v; ModConfig.musicShowHud = v; }
        ));

        return list;
    }

    // ===================== SimpleTextWidget =====================
    public static class SimpleTextWidget implements HudWidget {
        private final String id, name;
        private final IntSupplier2 rawX, rawY;
        private final Setter setXY;
        private final int defX, defY;
        private final BoolSupplier enabledGet;
        private final BoolSetter enabledSet;
        private final String previewText;
        private final int iconSize;

        private int lastWidth = 50, lastHeight = 10;

        public SimpleTextWidget(String id, String name,
                                IntSupplier2 rawX, IntSupplier2 rawY, Setter setXY,
                                int defX, int defY,
                                BoolSupplier enabledGet, BoolSetter enabledSet,
                                String previewText, int iconSize) {
            this.id = id; this.name = name;
            this.rawX = rawX; this.rawY = rawY; this.setXY = setXY;
            this.defX = defX; this.defY = defY;
            this.enabledGet = enabledGet;
            this.enabledSet = enabledSet;
            this.previewText = previewText;
            this.iconSize = iconSize;
        }

        @Override public String getId() { return id; }
        @Override public String getDisplayName() { return name; }
        @Override public int getRawX() { return rawX.get(); }
        @Override public int getRawY() { return rawY.get(); }

        @Override
        public int getEffectiveX(int screenWidth) {
            int rx = getRawX();
            if (rx >= 0) return rx;
            // -1 = центр по X (для low_hp / armor alert)
            return (screenWidth - lastWidth) / 2;
        }

        @Override
        public int getEffectiveY(int screenHeight) {
            int ry = getRawY();
            if (ry >= 0) return ry;
            // -1 = центр по Y - 40 (как в renderHud)
            return (screenHeight / 2) - 40;
        }

        @Override public void setPosition(int x, int y) { setXY.set(x, y); }
        @Override public int getDefaultX() { return defX; }
        @Override public int getDefaultY() { return defY; }
        @Override public boolean isEnabled() { return enabledGet.get(); }
        @Override public void setEnabled(boolean v) { enabledSet.set(v); }
        @Override public int getWidth() { return lastWidth; }
        @Override public int getHeight() { return lastHeight; }

        @Override
        public boolean hitTest(double mouseX, double mouseY, int screenWidth, int screenHeight) {
            int x = getEffectiveX(screenWidth);
            int y = getEffectiveY(screenHeight);
            // Немного увеличим hitbox — 2 px по краям, чтобы легко схватить
            int pad = 2;
            return mouseX >= x - pad && mouseX <= x + lastWidth + pad
                    && mouseY >= y - pad && mouseY <= y + lastHeight + pad;
        }

        @Override
        public int renderPreview(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
            Minecraft mc = Minecraft.getInstance();
            int textW = mc.font.width(previewText);
            int totalW = textW + (iconSize > 0 ? iconSize + 4 : 0);
            lastWidth = totalW;
            lastHeight = 10;

            int a = (int) (alpha * 255);
            int color = (a << 24) | 0xFFFFFF;

            int x = getEffectiveX(graphics.guiWidth());
            int y = getEffectiveY(graphics.guiHeight());

            if (iconSize > 0) {
                graphics.renderItem(new ItemStack(Items.PAPER), x, y - 4);
                graphics.drawString(mc.font, previewText, x + iconSize + 4, y, color, true);
            } else {
                graphics.drawString(mc.font, previewText, x, y, color, true);
            }
            return totalW;
        }
    }

    // ===================== FramedWidget =====================
    public static class FramedWidget implements HudWidget {
        private final String id, name;
        private final IntSupplier2 rawX, rawY;
        private final Setter setXY;
        private final int defX, defY, w, h;
        private final BoolSupplier enabledGet;
        private final BoolSetter enabledSet;

        public FramedWidget(String id, String name,
                            IntSupplier2 rawX, IntSupplier2 rawY, Setter setXY,
                            int defX, int defY, int w, int h,
                            BoolSupplier enabledGet, BoolSetter enabledSet) {
            this.id = id; this.name = name;
            this.rawX = rawX; this.rawY = rawY; this.setXY = setXY;
            this.defX = defX; this.defY = defY;
            this.w = w; this.h = h;
            this.enabledGet = enabledGet;
            this.enabledSet = enabledSet;
        }

        @Override public String getId() { return id; }
        @Override public String getDisplayName() { return name; }
        @Override public int getRawX() { return rawX.get(); }
        @Override public int getRawY() { return rawY.get(); }

        @Override
        public int getEffectiveX(int screenWidth) {
            int rx = getRawX();
            if (rx >= 0) return rx;
            // -1 = правый край (friend_list / music_player)
            return screenWidth - w - 6;
        }

        @Override
        public int getEffectiveY(int screenHeight) {
            int ry = getRawY();
            if (ry >= 0) return ry;
            return 10;
        }

        @Override public void setPosition(int x, int y) { setXY.set(x, y); }
        @Override public int getDefaultX() { return defX; }
        @Override public int getDefaultY() { return defY; }
        @Override public boolean isEnabled() { return enabledGet.get(); }
        @Override public void setEnabled(boolean v) { enabledSet.set(v); }
        @Override public int getWidth() { return w; }
        @Override public int getHeight() { return h; }

        @Override
        public boolean hitTest(double mouseX, double mouseY, int screenWidth, int screenHeight) {
            int x = getEffectiveX(screenWidth);
            int y = getEffectiveY(screenHeight);
            return mouseX >= x && mouseX <= x + w
                    && mouseY >= y && mouseY <= y + h;
        }

        @Override
        public int renderPreview(GuiGraphics graphics, int mouseX, int mouseY, float alpha) {
            Minecraft mc = Minecraft.getInstance();
            int a = (int) (alpha * 255);
            int frameColor = (a << 24) | 0x60A0FF;
            int textColor = (a << 24) | 0xFFFFFF;

            int x = getEffectiveX(graphics.guiWidth());
            int y = getEffectiveY(graphics.guiHeight());

            graphics.fill(x, y, x + w, y + 1, frameColor);
            graphics.fill(x, y + h - 1, x + w, y + h, frameColor);
            graphics.fill(x, y, x + 1, y + h, frameColor);
            graphics.fill(x + w - 1, y, x + w, y + h, frameColor);

            graphics.drawString(mc.font, "§l" + name, x + 4, y + 4, textColor, true);
            graphics.drawString(mc.font, "§7" + w + "×" + h, x + 4, y + 16, (a << 24) | 0xAAAAAA, false);

            return w;
        }
    }

    // ===================== Функциональные интерфейсы =====================
    @FunctionalInterface public interface IntSupplier2 { int get(); }
    @FunctionalInterface public interface BoolSupplier { boolean get(); }
    @FunctionalInterface public interface BoolSetter { void set(boolean v); }
    @FunctionalInterface public interface Setter { void set(int x, int y); }
}