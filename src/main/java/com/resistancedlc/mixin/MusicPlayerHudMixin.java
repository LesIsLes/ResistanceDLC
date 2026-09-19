package com.resistancedlc.mixin;

import com.resistancedlc.MusicPlayerHud;
import com.resistancedlc.MusicPlayerManager;
import com.resistancedlc.ResistanceDLC;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MusicPlayerHudMixin — обработка кликов по HUD-виджету MusicPlayer.
 *
 * В 1.21.11 MouseHandler.onButton:
 *   private void onButton(long window, MouseButtonInfo buttonInfo, @Action int action)
 *
 * Дескриптор метода: (JLnet/minecraft/client/input/MouseButtonInfo;I)V
 *
 * ВАЖНО: используем require = 0 — если метод не найдётся, мод не упадёт.
 */
@Mixin(MouseHandler.class)
public class MusicPlayerHudMixin {

    private static boolean draggingVolume = false;

    // ===================== ON_BUTTON =====================

    @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void onButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {

        if (!ModConfig.musicPlayerEnabled) return;
        if (!ModConfig.musicShowHud) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Разрешаем клики поверх чата, но не поверх других экранов
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) return;

        int button = buttonInfo.button();

        // === ОТПУСКАНИЕ ЛКМ ===
        if (action == GLFW.GLFW_RELEASE && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (draggingVolume) {
                draggingVolume = false;
                ci.cancel();
            }
            return;
        }

        // === НАЖАТИЕ ЛКМ ===
        if (action != GLFW.GLFW_PRESS) return;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        double mouseX = getScaledMouseX(mc);
        double mouseY = getScaledMouseY(mc);

        // === Проверка попадания в виджет ===
        if (!MusicPlayerHud.isPointOverWidget(mouseX, mouseY)) {
            return;
        }

        // === ПАУЗА / ПЛЕЙ ===
        if (MusicPlayerHud.isPointOverPauseButton(mouseX, mouseY)) {
            ResistanceDLC.LOGGER.info("[MusicHUD] Pause/Play clicked");
            MusicPlayerManager.togglePause();
            ci.cancel();
            return;
        }

        // === NEXT ===
        if (MusicPlayerHud.isPointOverNextButton(mouseX, mouseY)) {
            ResistanceDLC.LOGGER.info("[MusicHUD] Next clicked");
            MusicPlayerManager.next();
            ci.cancel();
            return;
        }

        // === НАСТРОЙКИ ===
        if (MusicPlayerHud.isPointOverSettingsButton(mouseX, mouseY)) {
            ResistanceDLC.LOGGER.info("[MusicHUD] Settings clicked");
            openMusicSettings();
            ci.cancel();
            return;
        }

        // === REPEAT ===
        if (MusicPlayerHud.isPointOverRepeatButton(mouseX, mouseY)) {
            ResistanceDLC.LOGGER.info("[MusicHUD] Repeat clicked");
            MusicPlayerManager.cycleRepeat();
            ci.cancel();
            return;
        }

        // === СЛАЙДЕР ГРОМКОСТИ ===
        if (MusicPlayerHud.isPointOverVolumeSlider(mouseX, mouseY)) {
            ResistanceDLC.LOGGER.info("[MusicHUD] Volume slider clicked");
            draggingVolume = true;
            updateVolumeFromMouse(mouseX);
            ci.cancel();
            return;
        }

        ResistanceDLC.LOGGER.info("[MusicHUD] No button matched");
    }

    // ===================== ON_MOVE (DRAG) =====================

    @Inject(method = "onMove(JDD)V", at = @At("HEAD"), require = 0)
    private void onMove(long window, double xpos, double ypos, CallbackInfo ci) {
        if (!draggingVolume) return;
        if (!ModConfig.musicPlayerEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            draggingVolume = false;
            return;
        }

        double mouseX = xpos * mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getWidth();
        updateVolumeFromMouse(mouseX);
    }

    // ===================== ХЕЛПЕРЫ =====================

    private static double getScaledMouseX(Minecraft mc) {
        double rawX = mc.mouseHandler.xpos();
        int windowW = mc.getWindow().getWidth();
        int guiW = mc.getWindow().getGuiScaledWidth();
        if (windowW <= 0) return rawX;
        return rawX * guiW / (double) windowW;
    }

    private static double getScaledMouseY(Minecraft mc) {
        double rawY = mc.mouseHandler.ypos();
        int windowH = mc.getWindow().getHeight();
        int guiH = mc.getWindow().getGuiScaledHeight();
        if (windowH <= 0) return rawY;
        return rawY * guiH / (double) windowH;
    }

    private static void updateVolumeFromMouse(double mouseX) {
        int sx = MusicPlayerHud.getVolumeSliderX();
        int sw = MusicPlayerHud.getVolumeSliderW();
        if (sw <= 0) return;

        float v = (float) ((mouseX - sx) / (double) sw);
        v = Math.max(0.0f, Math.min(1.0f, v));
        MusicPlayerManager.setVolume(v);
    }

    private static void openMusicSettings() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        com.resistancedlc.AccordionScreen.pendingJumpToSection = "music";
        com.resistancedlc.AccordionScreen.pendingJumpToItem = "music_player";

        mc.setScreen(new com.resistancedlc.AccordionScreen());
    }
}