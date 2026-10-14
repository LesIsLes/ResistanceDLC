package com.resistancedlc.mixin;

import com.resistancedlc.LocalizationManager;
import com.resistancedlc.SmartChatCoordParser;
import com.resistancedlc.SmartChatManager;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SmartChatMixin — перехват addMessage в ChatComponent.
 *
 * 1. Отменяет добавление повторов (ci.cancel()).
 * 2. Парсит координаты в новом сообщении.
 */
@Mixin(ChatComponent.class)
public class SmartChatMixin {

    /**
     * Отмена добавления повторов.
     * Инжектимся в addMessage(Component, MessageSignature, GuiMessageTag).
     */
    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessage(Component message, MessageSignature sig, GuiMessageSource source, GuiMessageTag tag,
                              CallbackInfo ci) {
        if (!ModConfig.smartChatEnabled) return;
        if (!ModConfig.smartChatGroupingEnabled) return;
        if (message == null) return;

        String plain = message.getString();
        if (SmartChatManager.processMessage(plain)) {
            // Повтор — отменяем добавление
            ci.cancel();
        }
    }

    /**
     * Парсинг координат в новом сообщении.
     * Модифицируем первый аргумент (Component).
     */
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
            require = 0
    )
    private Component modifyAddMessage(Component message) {
        if (!ModConfig.smartChatEnabled) return message;
        if (!ModConfig.smartChatCoordClickEnabled) return message;
        if (message == null) return null;
        return SmartChatCoordParser.parse(message);
    }
}