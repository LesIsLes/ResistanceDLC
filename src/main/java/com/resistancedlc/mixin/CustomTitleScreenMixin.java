package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
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
 * В 26.1 renderPanorama переименован в extractPanorama.
 */
@Mixin(Screen.class)
public class CustomTitleScreenMixin {

    @Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
    private void onExtractPanorama(GuiGraphicsExtractor graphics, float delta, CallbackInfo ci) {
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