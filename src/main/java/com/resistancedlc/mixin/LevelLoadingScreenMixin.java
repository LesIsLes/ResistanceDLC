package com.resistancedlc.mixin;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelLoadingScreenMixin — кастомный фон на экране "Loading terrain".
 *
 * В 1.21.11 LevelLoadingScreen.renderBackground() рисует:
 *   - case 0 (NETHER_PORTAL): кровавый портал
 *   - case 1 (END_PORTAL): фиолетовый портал Края
 *   - case 2 (OTHER): ванильная панорама + blur
 *
 * Перехватываем ВСЕ случаи — рисуем нашу сакуру.
 * Прогресс-бар и текст "Loading terrain" остаются ванильными (рисуются в render()).
 */
@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin {

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBackground(GuiGraphics graphics, int mouseX, int mouseY,
                                    float delta, CallbackInfo ci) {
        if (!ModConfig.customLoadingScreenEnabled) return;

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

        // Глушим ванильный фон (все case'ы)
        ci.cancel();
    }
}