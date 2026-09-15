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
 * Сообщение показывается ТОЛЬКО локально (displayClientMessage).
 * На сервер ничего не отправляется.
 */
public class DeathCoordsManager {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT);

    /**
     * Вызывается из ResistanceDLCClient при обнаружении смерти игрока.
     */
    public static void onPlayerDeath(Player player) {
        if (!ModConfig.deathCoordsEnabled) return;
        if (player == null) return;

        // Сохраняем координаты
        ModConfig.lastDeathX = (int) player.getX();
        ModConfig.lastDeathY = (int) player.getY();
        ModConfig.lastDeathZ = (int) player.getZ();
        ModConfig.lastDeathTime = System.currentTimeMillis();

        // Определяем измерение
        String dim = player.level().dimension().identifier().getPath();
        ModConfig.lastDeathDimension = normalizeDimension(dim);

        ConfigManager.save();

        // Приватное сообщение (видит только игрок)
        String msg = ModConfig.deathCoordsRussian
                ? "§c[DeathCoords] §fКоординаты смерти §e"
                + ModConfig.lastDeathX + ", "
                + ModConfig.lastDeathY + ", "
                + ModConfig.lastDeathZ
                + " §fбыли сохранены в §a/dc"
                : "§c[DeathCoords] §fDeath coords §e"
                + ModConfig.lastDeathX + ", "
                + ModConfig.lastDeathY + ", "
                + ModConfig.lastDeathZ
                + " §fwere saved to §a/dc";

        player.displayClientMessage(Component.literal(msg), false);
    }

    /**
     * Нормализует имя измерения: overworld / the_nether / the_end → Overworld / Nether / End
     */
    private static String normalizeDimension(String path) {
        if (path == null) return "Unknown";
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path;
        };
    }

    /**
     * Показывает последнюю точку смерти в чат (команда /dc last).
     */
    public static void showLastDeath() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (ModConfig.lastDeathTime == 0) {
            sendMsg(ModConfig.deathCoordsRussian
                    ? "§7[DeathCoords] Точек смерти пока нет."
                    : "§7[DeathCoords] No death points yet.");
            return;
        }

        String timeStr = DATE_FORMAT.format(new Date(ModConfig.lastDeathTime));

        sendMsg("§6§l══════ DeathCoords ══════");
        sendMsg(ModConfig.deathCoordsRussian
                ? "§7Последняя смерть: §f" + timeStr
                : "§7Last death: §f" + timeStr);
        sendMsg(ModConfig.deathCoordsRussian
                ? "§7Координаты: §e" + ModConfig.lastDeathX + ", "
                + ModConfig.lastDeathY + ", " + ModConfig.lastDeathZ
                : "§7Coords: §e" + ModConfig.lastDeathX + ", "
                + ModConfig.lastDeathY + ", " + ModConfig.lastDeathZ);
        sendMsg(ModConfig.deathCoordsRussian
                ? "§7Измерение: §b" + ModConfig.lastDeathDimension
                : "§7Dimension: §b" + ModConfig.lastDeathDimension);
        sendMsg("§6§l══════════════════════════");
    }

    /**
     * Очищает сохранённую точку смерти (команда /dc clear).
     */
    public static void clearLastDeath() {
        ModConfig.lastDeathX = 0;
        ModConfig.lastDeathY = 0;
        ModConfig.lastDeathZ = 0;
        ModConfig.lastDeathDimension = "";
        ModConfig.lastDeathTime = 0;
        ConfigManager.save();

        sendMsg(ModConfig.deathCoordsRussian
                ? "§a[DeathCoords] Точка смерти очищена."
                : "§a[DeathCoords] Death point cleared.");
    }

    /**
     * Справка по команде /dc.
     */
    public static void showHelp() {
        sendMsg("§6§l══════ DeathCoords — команды ══════");
        sendMsg("§e/dc §7— показать последнюю точку смерти");
        sendMsg("§e/dc last §7— то же самое");
        sendMsg("§e/dc clear §7— очистить сохранённую точку");
        sendMsg("§e/dc toggle §7— вкл/выкл фичу");
        sendMsg("§e/dc help §7— эта справка");
        sendMsg("§6§l═══════════════════════════════════");
    }

    private static void sendMsg(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}