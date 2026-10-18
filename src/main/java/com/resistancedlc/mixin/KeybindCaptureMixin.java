package com.resistancedlc.mixin;

import com.resistancedlc.KeyBindings;
import com.resistancedlc.config.ModConfig;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeybindCaptureMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void resistancedlc$captureKeybind(long window, int key, KeyEvent event, CallbackInfo ci) {
        // Если мы не в режиме бинда — пропускаем
        if (!ModConfig.isBindingKey) return;

        // Проверяем, что клавиша реально нажата (защита от автоповтора)
        int state = GLFW.glfwGetKey(window, key);
        if (state != GLFW.GLFW_PRESS) return;

        int target = ModConfig.bindingTarget;

        // Сбрасываем флаг СРАЗУ, чтобы не сработало дважды
        ModConfig.isBindingKey = false;
        ModConfig.bindingTarget = 0;

        // Применяем бинд на следующем тике, чтобы GUI успел обновиться
        Minecraft.getInstance().execute(() -> {
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

            // Обновляем кнопку в GUI
            var screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof com.resistancedlc.AccordionScreen acc) {
                acc.refreshAfterKeybind();
            }
        });

        // Отменяем стандартную обработку клавиши игрой, чтобы она не открыла меню/инвентарь
        ci.cancel();
    }
}