package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * FriendListHud — HUD-виджет со списком друзей.
 *
 * Показывает:
 *   - Ник друга
 *   - Онлайн/оффлайн (зелёный/серый кружок)
 *   - Если рядом (< 50 блоков) — расстояние
 *
 * Позиция настраивается через ModConfig.friendListHudX/Y (-1 = авто)
 */
public class FriendListHud {

    private static final int PADDING = 4;
    private static final int ROW_H = 10;

    public static void render(GuiGraphics graphics) {
        if (!ModConfig.friendListEnabled) return;
        if (!ModConfig.friendListShowHud) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen instanceof AccordionScreen) return;
        if (mc.options.hideGui) return;

        List<String> friends = FriendListManager.getFriends();
        if (friends.isEmpty()) return;

        // Собираем строки
        List<FriendLine> lines = new ArrayList<>();
        for (String friendName : friends) {
            boolean online = false;
            double distance = -1.0;
            int ping = 0;

            // Проверяем online через tab list
            if (mc.getConnection() != null) {
                for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                    String name = info.getProfile().name();
                    if (name != null && name.equalsIgnoreCase(friendName)) {
                        online = true;
                        ping = info.getLatency();
                        break;
                    }
                }
            }

            // Если онлайн — ищем entity для расстояния
            if (online) {
                for (Player p : mc.level.players()) {
                    if (p.getName().getString().equalsIgnoreCase(friendName)) {
                        distance = mc.player.distanceTo(p);
                        break;
                    }
                }
            }

            lines.add(new FriendLine(friendName, online, distance, ping));
        }

        // Размер виджета
        int maxTextW = 0;
        for (FriendLine line : lines) {
            String txt = buildLineText(line, mc);
            int w = mc.font.width(txt);
            if (w > maxTextW) maxTextW = w;
        }
        int widgetW = maxTextW + PADDING * 2 + 12;
        int widgetH = lines.size() * ROW_H + PADDING * 2 + 14;

        int screenW = graphics.guiWidth();
        int x = (ModConfig.friendListHudX < 0)
                ? (screenW - widgetW - 6)
                : ModConfig.friendListHudX;
        int y = ModConfig.friendListHudY;

        // Фон
        int alpha = ModConfig.friendListHudAlpha;
        int bgColor = (alpha << 24) | 0x101010;
        graphics.fill(x, y, x + widgetW, y + widgetH, bgColor);

        // Граница
        int borderColor = (alpha << 24) | (ModConfig.friendListTabColor & 0x00FFFFFF);
        graphics.fill(x, y, x + widgetW, y + 1, borderColor);
        graphics.fill(x, y + widgetH - 1, x + widgetW, y + widgetH, borderColor);
        graphics.fill(x, y, x + 1, y + widgetH, borderColor);
        graphics.fill(x + widgetW - 1, y, x + widgetW, y + widgetH, borderColor);

        // Заголовок
        String title = "§l★ " + LocalizationManager.get("gui.resistancedlc.hud.friend_list")
                + " §7(" + lines.size() + ")";
        graphics.drawString(mc.font, title, x + PADDING, y + PADDING,
                ModConfig.friendListTabColor, true);

        // Строки
        int rowY = y + PADDING + 12;
        for (FriendLine line : lines) {
            int nameColor = line.online
                    ? (ModConfig.friendListTabColor & 0x00FFFFFF)
                    : 0xFF888888;
            String name = line.name;
            if (name.length() > 16) name = name.substring(0, 14) + "…";

            // Точка online/offline
            int dotColor = line.online ? 0xFF00FF00 : 0xFF555555;
            graphics.fill(x + PADDING, rowY + 3,
                    x + PADDING + 4, rowY + 7, dotColor);

            // Имя
            graphics.drawString(mc.font, name,
                    x + PADDING + 8, rowY, nameColor, true);

            // Дистанция / пинг
            String info = "";
            if (line.online && line.distance >= 0) {
                info = String.format("%.0fm", line.distance);
            } else if (line.online) {
                info = line.ping + "ms";
            }

            if (!info.isEmpty()) {
                int infoW = mc.font.width(info);
                int infoColor = line.online ? 0xFFAAAAAA : 0xFF666666;
                graphics.drawString(mc.font, info,
                        x + widgetW - PADDING - infoW, rowY, infoColor, false);
            }

            rowY += ROW_H;
        }
    }

    private static String buildLineText(FriendLine line, Minecraft mc) {
        return line.name + "     " + (line.online ? "999m" : "");
    }

    private record FriendLine(String name, boolean online, double distance, int ping) {}
}