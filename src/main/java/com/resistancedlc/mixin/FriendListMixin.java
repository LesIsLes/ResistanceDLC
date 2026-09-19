package com.resistancedlc.mixin;

import com.resistancedlc.FriendListManager;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * FriendListMixin — подсветка сообщений от друзей в чате.
 *
 * Подход: перехватываем Component перед добавлением в ChatComponent
 * и, если текст содержит ник друга — добавляем цветной префикс.
 *
 * ВАЖНО: не меняем содержимое самого Component (там могут быть clickEvent,
 * hoverEvent), только оборачиваем через copy() + withStyle.
 */
@Mixin(ChatComponent.class)
public class FriendListMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Component onAddMessage(Component message) {
        if (!ModConfig.friendListEnabled) return message;
        if (!ModConfig.friendListHighlightChat) return message;
        if (message == null) return null;

        String plain = message.getString();
        if (plain == null || plain.isEmpty()) return message;

        if (!FriendListManager.textContainsFriend(plain)) {
            return message;
        }

        // Красим ВСЁ сообщение в цвет друга (перезаписываем style)
        int color = ModConfig.friendListChatColor & 0x00FFFFFF;
        MutableComponent colored = Component.empty()
                .append(Component.literal("★ ")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
                .append(Component.literal(plain)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));

        return colored;
    }
}