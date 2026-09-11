package com.resistancedlc;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ResistanceDLCClient implements ClientModInitializer {

    private static long lastAttackTime = 0;

    @Override
    public void onInitializeClient() {
        // 1. Загружаем конфиг при запуске
        ConfigManager.load();

        // 2. Регистрируем привязку клавиш
        KeyBindings.register();

        // 3. Регистрируем клиентские команды
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // Команда /resistancedlc gui
            dispatcher.register(
                    ClientCommandManager.literal("resistancedlc")
                            .then(ClientCommandManager.literal("gui")
                                    .executes(context -> {
                                        Minecraft.getInstance().setScreen(new MyCustomScreen());
                                        return 1;
                                    })
                            )
            );

            // Команда /cfg
            dispatcher.register(
                    ClientCommandManager.literal("cfg")
                            .then(ClientCommandManager.literal("dir")
                                    .executes(context -> {
                                        ConfigManager.openFolder();
                                        sendMessage("§aПапка с конфигами открыта: §7" + ConfigManager.getConfigDirPath());
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        List<String> configs = ConfigManager.listConfigs();
                                        if (configs.isEmpty()) {
                                            sendMessage("§7Сохранённых конфигов нет.");
                                        } else {
                                            sendMessage("§6Сохранённые конфиги:");
                                            for (String name : configs) {
                                                sendMessage("§7 - §e" + name);
                                            }
                                        }
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("save")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                if (ConfigManager.saveAs(name)) {
                                                    sendMessage("§aКонфиг §e" + name + " §aсохранён!");
                                                } else {
                                                    sendMessage("§cНе удалось сохранить конфиг §e" + name);
                                                }
                                                return 1;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("load")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                if (ConfigManager.loadFrom(name)) {
                                                    sendMessage("§aКонфиг §e" + name + " §aзагружен!");
                                                    if (Minecraft.getInstance().screen instanceof MyCustomScreen) {
                                                        Minecraft.getInstance().setScreen(new MyCustomScreen());
                                                    }
                                                } else {
                                                    sendMessage("§cКонфиг §e" + name + " §cне найден!");
                                                }
                                                return 1;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                if (ConfigManager.remove(name)) {
                                                    sendMessage("§aКонфиг §e" + name + " §aудалён!");
                                                } else {
                                                    sendMessage("§cКонфиг §e" + name + " §cне найден!");
                                                }
                                                return 1;
                                            })
                                    )
                            )
            );
        });
        // 4. BPS (скорость)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                double dx = client.player.getX() - MyCustomScreen.lastPlayerX;
                double dy = client.player.getY() - MyCustomScreen.lastPlayerY;
                double dz = client.player.getZ() - MyCustomScreen.lastPlayerZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                MyCustomScreen.currentBps = distance * 20.0;

                MyCustomScreen.lastPlayerX = client.player.getX();
                MyCustomScreen.lastPlayerY = client.player.getY();
                MyCustomScreen.lastPlayerZ = client.player.getZ();
            }
        });

        // 5. TAPEMOUSE (автокликер)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!MyCustomScreen.tapeMouseEnabled) return;
            if (client.player == null || client.level == null) return;
            if (client.screen != null) return;

            Entity target = client.crosshairPickEntity;
            if (target == null) return;

            boolean isPlayer = target instanceof Player;
            boolean isMob = target instanceof LivingEntity && !isPlayer;

            switch (MyCustomScreen.tapeMouseTarget) {
                case 1: if (!isMob) return; break;
                case 2: if (!isPlayer) return; break;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime < (long)(MyCustomScreen.tapeMouseDelay * 1000)) return;

            if (client.gameMode != null) {
                client.gameMode.attack(client.player, target);
                client.player.swing(InteractionHand.MAIN_HAND);
                lastAttackTime = currentTime;
            }
        });

        // 6. HUD
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "hud"),
                (graphics, tickCounter) -> {
                    if (MyCustomScreen.showHud) {
                        Minecraft client = Minecraft.getInstance();
                        if (client.player != null && !client.options.hideGui) {

                            int alpha = MyCustomScreen.hudAlpha;
                            int baseColor = MyCustomScreen.hudColor;
                            int color = (baseColor & 0x00FFFFFF) | (alpha << 24);

                            // Координаты
                            if (MyCustomScreen.showCoords) {
                                drawHudString(graphics, client.font,
                                        String.format("XYZ: %d / %d / %d",
                                                (int) client.player.getX(),
                                                (int) client.player.getY(),
                                                (int) client.player.getZ()),
                                        MyCustomScreen.coordsX, MyCustomScreen.coordsY, color);
                            }

                            // Биом
                            if (MyCustomScreen.showBiome) {
                                String biome = client.level.getBiome(client.player.blockPosition())
                                        .unwrapKey()
                                        .map(key -> key.identifier().getPath())
                                        .orElse("unknown");
                                drawHudString(graphics, client.font, "Биом: " + biome,
                                        MyCustomScreen.biomeX, MyCustomScreen.biomeY, color);
                            }

                            // Время
                            if (MyCustomScreen.showTime) {
                                long time = client.level.getDayTime() % 24000;
                                String timeStr = time < 12000 ? "День" : "Ночь";
                                drawHudString(graphics, client.font, "Время: " + timeStr,
                                        MyCustomScreen.timeX, MyCustomScreen.timeY, color);
                            }

                            // FPS
                            if (MyCustomScreen.showFps) {
                                String label = MyCustomScreen.fpsRussian ? "КВС" : "FPS";
                                drawHudString(graphics, client.font, label + ": " + client.getFps(),
                                        MyCustomScreen.fpsX, MyCustomScreen.fpsY, color);
                            }

                            // Ping
                            if (MyCustomScreen.showPing) {
                                int ping = 0;
                                if (client.getConnection() != null
                                        && client.getConnection().getPlayerInfo(client.player.getUUID()) != null) {
                                    ping = client.getConnection().getPlayerInfo(client.player.getUUID()).getLatency();
                                }
                                String label = MyCustomScreen.pingRussian ? "Пинг" : "Ping";
                                drawHudString(graphics, client.font, label + ": " + ping + " ms",
                                        MyCustomScreen.pingX, MyCustomScreen.pingY, color);
                            }

                            // TPS
                            if (MyCustomScreen.showTps) {
                                float tps = 20.0f;
                                if (client.getSingleplayerServer() != null) {
                                    long tickTime = client.getSingleplayerServer().getAverageTickTimeNanos();
                                    if (tickTime > 0) {
                                        tps = Math.min(20.0f, 1_000_000_000.0f / tickTime);
                                    }
                                }
                                String label = MyCustomScreen.tpsRussian ? "ТВС" : "TPS";
                                drawHudString(graphics, client.font,
                                        String.format("%s: %.1f", label, tps),
                                        MyCustomScreen.tpsX, MyCustomScreen.tpsY, color);
                            }

                            // BPS
                            if (MyCustomScreen.showBps) {
                                String label = MyCustomScreen.bpsRussian ? "БВС" : "BPS";
                                drawHudString(graphics, client.font,
                                        String.format("%s: %.2f", label, MyCustomScreen.currentBps),
                                        MyCustomScreen.bpsX, MyCustomScreen.bpsY, color);
                            }

                            // Направление
                            if (MyCustomScreen.showDirection) {
                                float yaw = client.player.getYRot();
                                yaw = ((yaw % 360) + 360) % 360;

                                String dir;
                                if (MyCustomScreen.directionRussian) {
                                    if (yaw >= 315 || yaw < 45) dir = "Юг";
                                    else if (yaw >= 45 && yaw < 135) dir = "Запад";
                                    else if (yaw >= 135 && yaw < 225) dir = "Север";
                                    else dir = "Восток";
                                } else {
                                    if (yaw >= 315 || yaw < 45) dir = "South";
                                    else if (yaw >= 45 && yaw < 135) dir = "West";
                                    else if (yaw >= 135 && yaw < 225) dir = "North";
                                    else dir = "East";
                                }

                                String label = MyCustomScreen.directionRussian ? "Направление" : "Direction";
                                drawHudString(graphics, client.font, label + ": " + dir,
                                        MyCustomScreen.directionX, MyCustomScreen.directionY, color);
                            }

                            // Hits (счётчик ударов)
                            if (MyCustomScreen.showHitCounter) {
                                Entity target = client.crosshairPickEntity;
                                if (target instanceof LivingEntity living) {
                                    float hp = living.getHealth();
                                    float maxHp = living.getMaxHealth();
                                    float percent = hp / maxHp;

                                    int hpColor;
                                    if (percent > 0.75f) hpColor = 0xFF00FF00;
                                    else if (percent > 0.50f) hpColor = 0xFFFFFF00;
                                    else if (percent > 0.25f) hpColor = 0xFFFF8800;
                                    else hpColor = 0xFFFF0000;

                                    float damage = 1.0f;
                                    var attr = client.player.getAttribute(Attributes.ATTACK_DAMAGE);
                                    if (attr != null) damage = (float) attr.getValue();

                                    int hitsLeft = (int) Math.ceil(hp / Math.max(damage, 0.1f));
                                    String label = MyCustomScreen.hitCounterRussian ? "Удары" : "Hits";
                                    drawHudString(graphics, client.font, label + ": " + hitsLeft,
                                            MyCustomScreen.hitCounterX, MyCustomScreen.hitCounterY,
                                            hpColor);
                                }
                            }
                            // === POTION EFFECTS ===
                            if (MyCustomScreen.showPotionEffects) {
                                Collection<MobEffectInstance> effects = client.player.getActiveEffects();
                                if (!effects.isEmpty()) {
                                    String label = MyCustomScreen.potionEffectsRussian ? "Эффекты:" : "Effects:";
                                    drawHudString(graphics, client.font, label,
                                            MyCustomScreen.potionEffectsX, MyCustomScreen.potionEffectsY, color);

                                    int yOffset = 12;
                                    for (MobEffectInstance effect : effects) {
                                        MobEffect type = effect.getEffect().value();
                                        String name = type.getDisplayName().getString();
                                        int duration = effect.getDuration();

                                        int totalSeconds = duration / 20;
                                        int minutes = totalSeconds / 60;
                                        int seconds = totalSeconds % 60;
                                        String timeStr = String.format("%d:%02d", minutes, seconds);

                                        String text = " " + name + " " + timeStr;
                                        drawHudString(graphics, client.font, text,
                                                MyCustomScreen.potionEffectsX, MyCustomScreen.potionEffectsY + yOffset, color);
                                        yOffset += 10;
                                    }
                                }
                            }

                        }
                    }
                }
        );
    }

    // ===== Вспомогательный метод: рисует фон под текстом (если включено) и сам текст =====
    private static void drawHudString(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        if (MyCustomScreen.hudBackgroundEnabled) {
            int textWidth = font.width(text);
            int bgAlpha = MyCustomScreen.hudBackgroundAlpha;
            int bgBaseColor = MyCustomScreen.hudBackgroundColor;
            int bgColor = (bgBaseColor & 0x00FFFFFF) | (bgAlpha << 24);
            int height = MyCustomScreen.hudBackgroundHeight;
            graphics.fill(x - 1, y - 1, x + textWidth + 1, y - 1 + height, bgColor);
        }
        graphics.drawString(font, text, x, y, color, true);
    }

    private void sendMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}