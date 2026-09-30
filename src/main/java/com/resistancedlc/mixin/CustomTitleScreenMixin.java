package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CustomTitleScreenMixin — кастомный фон на всех экранах меню.
 *
 * renderPanorama объявлен в Screen (родитель TitleScreen).
 * Заменяем фон везде, КРОМЕ игровых экранов (Pause, Inventory, Chat, DeathScreen, Loading).
 *
 * Если нужен только TitleScreen — оставь проверку `instanceof TitleScreen`.
 */
@Mixin(Screen.class)
public class CustomTitleScreenMixin {

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true, require = 0)
    private void onRenderPanorama(GuiGraphics graphics, float delta, CallbackInfo ci) {
        if (!ModConfig.customMainMenuEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) return;

        // ❌ НЕ заменяем фон в игровых экранах
        if (screen instanceof PauseScreen) return;
        if (screen instanceof AbstractContainerScreen) return;
        if (screen instanceof ChatScreen) return;
        if (screen instanceof DeathScreen) return;
        if (screen instanceof LevelLoadingScreen) return;

        // ✅ Заменяем фон во всех остальных (TitleScreen, Options, SelectWorld, Server list...)
        Identifier bg = Identifier.fromNamespaceAndPath(
                "resistancedlc", "textures/gui/title/custom_background.png"
        );

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                bg,
                0, 0,
                0f, 0f,
                graphics.guiWidth(), graphics.guiHeight(),
                graphics.guiWidth(), graphics.guiHeight()
        );

        ci.cancel();
    }
}