package com.resistancedlc.mixin;

import com.resistancedlc.SmartChatManager;
import com.resistancedlc.config.ModConfig;

import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GuiMessageLineMixin — добавляет бейдж "×N" и tooltip к строкам чата.
 *
 * Инжектится в GuiMessage.Line.content() — возвращает FormattedCharSequence.
 */
@Mixin(GuiMessage.Line.class)
public class GuiMessageLineMixin {

    @Inject(
            method = "content()Lnet/minecraft/util/FormattedCharSequence;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void modifyContent(CallbackInfoReturnable<FormattedCharSequence> cir) {
        if (!ModConfig.smartChatEnabled) return;
        if (!ModConfig.smartChatGroupingEnabled) return;

        FormattedCharSequence original = cir.getReturnValue();
        if (original == null) return;

        String plain = extractPlainText(original);
        if (plain == null || plain.isEmpty()) return;

        int count = SmartChatManager.getCount(plain);
        if (count < 2) return;

        Component tooltip = SmartChatManager.getTooltip(plain);

        MutableComponent combined = Component.literal(plain);
        MutableComponent badge = Component.literal(" §7×" + count);
        if (tooltip != null) {
            badge = badge.withStyle(Style.EMPTY
                    .withHoverEvent(new HoverEvent.ShowText(tooltip)));
        }
        combined.append(badge);

        cir.setReturnValue(combined.getVisualOrderText());
    }

    private static String extractPlainText(FormattedCharSequence seq) {
        try {
            StringBuilder sb = new StringBuilder();
            seq.accept((index, style, codepoint) -> {
                sb.appendCodePoint(codepoint);
                return true;
            });
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}