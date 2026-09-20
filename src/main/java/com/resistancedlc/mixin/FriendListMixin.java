package com.resistancedlc.mixin;

import com.resistancedlc.FriendListManager;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * FriendListMixin — подсветка сообщений от друзей в чате.
 *
 * 1.21.11: ChatComponent.addMessage(Component, MessageSignature, GuiMessageTag)
 * Модифицируем ПЕРВЫЙ параметр (Component) через @ModifyVariable.
 */
@Mixin(ChatComponent.class)
public class FriendListMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1,
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

        int color = ModConfig.friendListChatColor & 0x00FFFFFF;
        MutableComponent colored = Component.empty()
                .append(Component.literal("★ ")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))))
                .append(Component.literal(plain)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));

        return colored;
    }
}