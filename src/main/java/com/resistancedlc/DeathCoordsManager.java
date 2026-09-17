package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Логика DeathCoords — сохранение координат смерти.
 * Все сообщения локализованы.
 */
public class DeathCoordsManager {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT);

    public static void onPlayerDeath(Player player) {
        if (!ModConfig.deathCoordsEnabled) return;
        if (player == null) return;

        ModConfig.lastDeathX = (int) player.getX();
        ModConfig.lastDeathY = (int) player.getY();
        ModConfig.lastDeathZ = (int) player.getZ();
        ModConfig.lastDeathTime = System.currentTimeMillis();

        String dim = player.level().dimension().identifier().getPath();
        ModConfig.lastDeathDimension = normalizeDimension(dim);

        ConfigManager.save();

        String msg = "§c" + LocalizationManager.get("gui.resistancedlc.command.dc.death_saved",
                ModConfig.lastDeathX, ModConfig.lastDeathY, ModConfig.lastDeathZ);

        player.displayClientMessage(Component.literal(msg), false);
    }

    private static String normalizeDimension(String path) {
        if (path == null) return "Unknown";
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path;
        };
    }

    public static void showLastDeath() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (ModConfig.lastDeathTime == 0) {
            sendMsg("§7" + LocalizationManager.get("gui.resistancedlc.command.dc.empty"));
            return;
        }

        String timeStr = DATE_FORMAT.format(new Date(ModConfig.lastDeathTime));

        sendMsg("§6§l" + LocalizationManager.get("gui.resistancedlc.command.dc.last_title"));
        sendMsg("§7" + LocalizationManager.get("gui.resistancedlc.command.dc.time") + "§f" + timeStr);
        sendMsg("§7" + LocalizationManager.get("gui.resistancedlc.command.dc.coords") + "§e"
                + ModConfig.lastDeathX + ", " + ModConfig.lastDeathY + ", " + ModConfig.lastDeathZ);
        sendMsg("§7" + LocalizationManager.get("gui.resistancedlc.command.dc.dimension") + "§b"
                + ModConfig.lastDeathDimension);
        sendMsg("§6§l══════════════════════════");
    }

    public static void clearLastDeath() {
        ModConfig.lastDeathX = 0;
        ModConfig.lastDeathY = 0;
        ModConfig.lastDeathZ = 0;
        ModConfig.lastDeathDimension = "";
        ModConfig.lastDeathTime = 0;
        ConfigManager.save();

        sendMsg("§a" + LocalizationManager.get("gui.resistancedlc.command.dc.cleared"));
    }

    public static void showHelp() {
        sendMsg("§6§l" + LocalizationManager.get("gui.resistancedlc.command.dc.title"));
        sendMsg("§e" + LocalizationManager.get("gui.resistancedlc.command.dc.show"));
        sendMsg("§e" + LocalizationManager.get("gui.resistancedlc.command.dc.last"));
        sendMsg("§e" + LocalizationManager.get("gui.resistancedlc.command.dc.clear"));
        sendMsg("§e" + LocalizationManager.get("gui.resistancedlc.command.dc.toggle"));
        sendMsg("§e" + LocalizationManager.get("gui.resistancedlc.command.dc.help"));
        sendMsg("§6§l═══════════════════════════════════");
    }

    private static void sendMsg(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}