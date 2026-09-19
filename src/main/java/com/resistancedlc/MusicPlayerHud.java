package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/**
 * MusicPlayerHud — рендер HUD-виджета MusicPlayer.
 * Виджет НЕ обрабатывает клики — это делает MusicPlayerHudMixin.
 */
public class MusicPlayerHud {

    // ===================== РАЗМЕРЫ (исправлены) =====================
    private static final int WIDGET_W = 270;
    private static final int WIDGET_H = 70;

    private static final int VINYL_SIZE = 32;
    private static final int VINYL_PADDING = 8;

    private static final int BTN_SIZE = 16;
    private static final int BTN_GAP = 4;
    private static final int BTN_Y_OFFSET = 48;

    private static final int VOL_SLIDER_W = 60;
    private static final int VOL_SLIDER_H = 6;
    private static final int VOL_SLIDER_X_OFFSET = 175;
    private static final int VOL_SLIDER_Y_OFFSET = 53;

    // ===================== ТЕКСТУРА =====================
    private static final Identifier VINYL_TEXTURE =
            Identifier.fromNamespaceAndPath("resistancedlc", "textures/gui/vinyl.png");

    // ===================== СОСТОЯНИЕ =====================
    private static float vinylRotation = 0.0f;

    // Hitbox-области (обновляются в render())
    private static int widgetX = 0, widgetY = 0;
    private static final int[] btnPauseX = {0}, btnPauseY = {0};
    private static final int[] btnNextX = {0}, btnNextY = {0};
    private static final int[] btnSettingsX = {0}, btnSettingsY = {0};
    private static final int[] btnRepeatX = {0}, btnRepeatY = {0};
    private static final int[] volSliderX = {0}, volSliderY = {0}, volSliderW = {0}, volSliderH = {0};

    // ===================== РЕНДЕР =====================

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!ModConfig.musicPlayerEnabled) return;
        if (!ModConfig.musicShowHud) return;
        if (mc.screen instanceof AccordionScreen) return;

        int screenW = graphics.guiWidth();
        int baseX = (ModConfig.musicHudX < 0)
                ? (screenW - WIDGET_W - 10)
                : ModConfig.musicHudX;
        int baseY = ModConfig.musicHudY;

        widgetX = baseX;
        widgetY = baseY;

        drawBackground(graphics, baseX, baseY);
        drawVinyl(graphics, baseX, baseY);
        drawText(graphics, baseX, baseY);
        drawButtons(graphics, baseX, baseY);
        drawVolumeSlider(graphics, baseX, baseY);
    }

    private static void drawBackground(GuiGraphics graphics, int x, int y) {
        int alpha = ModConfig.musicHudAlpha;
        int bgColor = (alpha << 24) | 0x101010;
        graphics.fill(x, y, x + WIDGET_W, y + WIDGET_H, bgColor);

        int borderColor = (alpha << 24) | (ModConfig.guiColor & 0x00FFFFFF);
        graphics.fill(x, y, x + WIDGET_W, y + 1, borderColor);
        graphics.fill(x, y + WIDGET_H - 1, x + WIDGET_W, y + WIDGET_H, borderColor);
        graphics.fill(x, y, x + 1, y + WIDGET_H, borderColor);
        graphics.fill(x + WIDGET_W - 1, y, x + WIDGET_W, y + WIDGET_H, borderColor);
    }

    private static void drawVinyl(GuiGraphics graphics, int x, int y) {
        int vx = x + VINYL_PADDING;
        int vy = y + (WIDGET_H - VINYL_SIZE) / 2 - 8;

        if (MusicPlayerManager.isPlaying()) {
            vinylRotation += 1.5f;
            if (vinylRotation >= 360.0f) vinylRotation -= 360.0f;
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(vx + VINYL_SIZE / 2.0f, vy + VINYL_SIZE / 2.0f);
        graphics.pose().rotate((float) Math.toRadians(vinylRotation));
        graphics.pose().translate(-VINYL_SIZE / 2.0f, -VINYL_SIZE / 2.0f);
        graphics.pose().scale(0.5f, 0.5f);
        graphics.blit(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                VINYL_TEXTURE,
                0, 0,
                0, 0,
                64, 64,
                64, 64
        );
        graphics.pose().popMatrix();
    }

    private static void drawText(GuiGraphics graphics, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        MusicTrack track = MusicPlayerManager.getCurrentTrack();

        int textX = x + VINYL_PADDING + VINYL_SIZE + 8;

        if (track == null) {
            graphics.drawString(mc.font, "§7♪ Music Player",
                    textX, y + 10, 0xFFAAAAAA, false);
            graphics.drawString(mc.font, "§8Плейлист пуст",
                    textX, y + 24, 0xFF666666, false);
            graphics.drawString(mc.font, "§8Закинь .ogg в папку",
                    textX, y + 34, 0xFF666666, false);
            return;
        }

        String name = track.displayName();
        if (name.length() > 28) name = name.substring(0, 26) + "…";
        graphics.drawString(mc.font, "§l♪ " + name,
                textX, y + 8, ModConfig.guiColor, true);

        String artist = track.displayArtist();
        if (!artist.isEmpty()) {
            if (artist.length() > 30) artist = artist.substring(0, 28) + "…";
            graphics.drawString(mc.font, artist,
                    textX, y + 22, 0xFFCCCCCC, false);
        }

        int posSec = MusicPlayerManager.getPositionSeconds();
        int durSec = MusicPlayerManager.getDurationSeconds();
        String timeStr = MusicPlayerManager.formatTime(posSec)
                + " / " + MusicPlayerManager.formatTime(durSec);

        int timeColor = MusicPlayerManager.isPaused() ? 0xFFFFAA00 : 0xFF888888;
        graphics.drawString(mc.font, timeStr, textX, y + 34, timeColor, false);

        if (MusicPlayerManager.isPaused()) {
            String status = "§e⏸";
            int sw = mc.font.width(status);
            graphics.drawString(mc.font, status,
                    x + WIDGET_W - sw - 6, y + 6, 0xFFFFAA00, true);
        } else if (MusicPlayerManager.isPlaying()) {
            String status = "§a▶";
            int sw = mc.font.width(status);
            graphics.drawString(mc.font, status,
                    x + WIDGET_W - sw - 6, y + 6, 0xFF00FF00, true);
        }
    }

    private static void drawButtons(GuiGraphics graphics, int x, int y) {
        Minecraft mc = Minecraft.getInstance();

        int startX = x + VINYL_PADDING;
        int btnY = y + BTN_Y_OFFSET;

        boolean playing = MusicPlayerManager.isPlaying();
        boolean paused = MusicPlayerManager.isPaused();
        String pauseIcon = paused ? "▶" : (playing ? "⏸" : "▶");
        int pauseColor = paused ? 0xFFFFAA00 : (playing ? 0xFF00FF00 : 0xFFAAAAAA);
        drawButton(graphics, mc, startX, btnY, pauseIcon, pauseColor);
        btnPauseX[0] = startX;
        btnPauseY[0] = btnY;

        int nextX = startX + BTN_SIZE + BTN_GAP;
        drawButton(graphics, mc, nextX, btnY, "⏭", 0xFFCCCCCC);
        btnNextX[0] = nextX;
        btnNextY[0] = btnY;

        int settingsX = nextX + BTN_SIZE + BTN_GAP;
        drawButton(graphics, mc, settingsX, btnY, "⚙", ModConfig.guiColor);
        btnSettingsX[0] = settingsX;
        btnSettingsY[0] = btnY;

        int repeatX = settingsX + BTN_SIZE + BTN_GAP;
        String repeatIcon;
        int repeatColor;
        switch (ModConfig.musicRepeat) {
            case 1 -> { repeatIcon = "🔂"; repeatColor = 0xFF00FF00; }
            case 2 -> { repeatIcon = "🔁"; repeatColor = 0xFF00FFFF; }
            default -> { repeatIcon = "🔁"; repeatColor = 0xFF666666; }
        }
        drawButton(graphics, mc, repeatX, btnY, repeatIcon, repeatColor);
        btnRepeatX[0] = repeatX;
        btnRepeatY[0] = btnY;
    }

    private static void drawButton(GuiGraphics graphics, Minecraft mc,
                                   int x, int y, String icon, int color) {
        graphics.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, 0x80000000);
        graphics.fill(x, y, x + BTN_SIZE, y + 1, color);
        graphics.fill(x, y + BTN_SIZE - 1, x + BTN_SIZE, y + BTN_SIZE, color);
        graphics.fill(x, y, x + 1, y + BTN_SIZE, color);
        graphics.fill(x + BTN_SIZE - 1, y, x + BTN_SIZE, y + BTN_SIZE, color);

        int iconW = mc.font.width(icon);
        int ix = x + (BTN_SIZE - iconW) / 2;
        int iy = y + (BTN_SIZE - 8) / 2;
        graphics.drawString(mc.font, icon, ix, iy, 0xFFFFFFFF, false);
    }

    private static void drawVolumeSlider(GuiGraphics graphics, int x, int y) {
        int sx = x + VOL_SLIDER_X_OFFSET;
        int sy = y + VOL_SLIDER_Y_OFFSET;

        volSliderX[0] = sx;
        volSliderY[0] = sy;
        volSliderW[0] = VOL_SLIDER_W;
        volSliderH[0] = VOL_SLIDER_H;

        graphics.fill(sx, sy, sx + VOL_SLIDER_W, sy + VOL_SLIDER_H, 0xFF202020);

        int filled = (int) (VOL_SLIDER_W * ModConfig.musicVolume);
        int fillColor = (ModConfig.guiColor & 0x00FFFFFF) | 0xC0000000;
        graphics.fill(sx, sy, sx + filled, sy + VOL_SLIDER_H, fillColor);

        graphics.fill(sx, sy, sx + VOL_SLIDER_W, sy + 1, 0xFF505050);
        graphics.fill(sx, sy + VOL_SLIDER_H - 1, sx + VOL_SLIDER_W, sy + VOL_SLIDER_H, 0xFF505050);
        graphics.fill(sx, sy, sx + 1, sy + VOL_SLIDER_H, 0xFF505050);
        graphics.fill(sx + VOL_SLIDER_W - 1, sy, sx + VOL_SLIDER_W, sy + VOL_SLIDER_H, 0xFF505050);

        Minecraft mc = Minecraft.getInstance();
        graphics.drawString(mc.font, "🔊", sx - 14, sy - 3, 0xFFCCCCCC, false);

        // Фикс: убрали %, оставили просто число
        String volText = String.valueOf((int)(ModConfig.musicVolume * 100));
        graphics.drawString(mc.font, volText,
                sx + VOL_SLIDER_W + 4, sy - 3, 0xFFAAAAAA, false);
    }

    // ===================== HITBOX-ГЕТТЕРЫ =====================

    public static boolean isPointOverPauseButton(double mx, double my) {
        return hit(mx, my, btnPauseX[0], btnPauseY[0], BTN_SIZE, BTN_SIZE);
    }

    public static boolean isPointOverNextButton(double mx, double my) {
        return hit(mx, my, btnNextX[0], btnNextY[0], BTN_SIZE, BTN_SIZE);
    }

    public static boolean isPointOverSettingsButton(double mx, double my) {
        return hit(mx, my, btnSettingsX[0], btnSettingsY[0], BTN_SIZE, BTN_SIZE);
    }

    public static boolean isPointOverRepeatButton(double mx, double my) {
        return hit(mx, my, btnRepeatX[0], btnRepeatY[0], BTN_SIZE, BTN_SIZE);
    }

    public static boolean isPointOverVolumeSlider(double mx, double my) {
        return hit(mx, my, volSliderX[0], volSliderY[0] - 4, volSliderW[0], volSliderH[0] + 8);
    }
    public static boolean isPointOverWidget(double mx, double my) {
        return hit(mx, my, widgetX, widgetY, WIDGET_W, WIDGET_H);
    }
    public static int getWidgetX() { return widgetX; }
    public static int getWidgetY() { return widgetY; }
    public static int getWidgetW() { return WIDGET_W; }
    public static int getWidgetH() { return WIDGET_H; }

    public static int getVolumeSliderX() { return volSliderX[0]; }
    public static int getVolumeSliderW() { return volSliderW[0]; }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}