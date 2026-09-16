package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ResistanceDLCClient implements ClientModInitializer {

    private static long lastAttackTime = 0;
    private static boolean tapeMouseWeHeldRMB = false;

    // ===== PVP SAFE + DEATH COORDS: отслеживание здоровья =====
    private static float lastHealth = -1.0f;

    @Override
    public void onInitializeClient() {
        // ===== ДЕБАУНС СОХРАНЕНИЯ КОНФИГА =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> ConfigManager.tick());

        // ===== ЗАГРУЗКА КОНФИГА + КЛАВИШИ =====
        ConfigManager.load();
        KeyBindings.register();

        // ===== PVP SAFE: блокировка ESC через Fabric API =====
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof PauseScreen)) return;

            ScreenKeyboardEvents.allowKeyPress(screen).register((s, keyEvent) -> {
                if (!ModConfig.pvpSafeEnabled) return true;
                if (!ModConfig.pvpSafeBlockQuit) return true;
                if (!PvPSafeManager.isInCombat()) return true;

                if (keyEvent.key() == 256) {
                    PvPSafeManager.sendQuitBlockedMessage();
                    return false;
                }
                return true;
            });
        });

        // ===== AUTO RECONNECT: отмена ESC =====
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, keyEvent) -> {
                if (!AutoReconnectManager.isReconnecting()) return true;

                if (keyEvent.key() == 256) {
                    AutoReconnectManager.cancel();
                    if (client.player != null) {
                        client.player.displayClientMessage(
                                Component.literal("§c[AutoReconnect] Отменено"), true);
                    }
                    return false;
                }
                return true;
            });
        });

        // ===== СОХРАНЕНИЕ ПРИ ВЫХОДЕ + ОЧИСТКА ТРЕКЕРОВ =====
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ConfigManager.saveNow();
            TotemTracker.clear();
            PvPSafeManager.reset();
            lastHealth = -1.0f;

            // ===== AUTO RECONNECT: запуск таймера =====
            AutoReconnectManager.onDisconnect();
        });

        // ===== AUTO RECONNECT: запоминаем сервер при JOIN =====
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            AutoReconnectManager.onJoin();
        });

        // ===== КОМАНДЫ =====
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("cfg")
                            // /cfg без аргументов → справка
                            .executes(context -> {
                                showCfgHelp();
                                return 1;
                            })
                            .then(ClientCommandManager.literal("help")
                                    .executes(context -> {
                                        showCfgHelp();
                                        return 1;
                                    })
                            )
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
                                            sendMessage("§6Сохранённые конфиги (§e" + configs.size() + "§6):");
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
                                                    if (Minecraft.getInstance().screen instanceof AccordionScreen) {
                                                        Minecraft.getInstance().setScreen(new AccordionScreen());
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

            // ===== КОМАНДА /chatfilter =====
            dispatcher.register(
                    ClientCommandManager.literal("chatfilter")
                            .executes(context -> {
                                sendMessage("§6§l══════ ChatFilter — команды ══════");
                                sendMessage("§e/chatfilter add <слово> §7— добавить стоп-слово");
                                sendMessage("§e/chatfilter remove <слово> §7— удалить стоп-слово");
                                sendMessage("§e/chatfilter list §7— список стоп-слов");
                                sendMessage("§e/chatfilter clear §7— очистить всё");
                                sendMessage("§6§l════════════════════════════════");
                                return 1;
                            })
                            .then(ClientCommandManager.literal("add")
                                    .then(ClientCommandManager.argument("word", StringArgumentType.string())
                                            .executes(context -> {
                                                String word = StringArgumentType.getString(context, "word");
                                                if (ChatFilterManager.addWord(word)) {
                                                    sendMessage("§a[ChatFilter] Добавлено: §e" + word);
                                                } else {
                                                    sendMessage("§c[ChatFilter] Уже есть или пусто: §e" + word);
                                                }
                                                return 1;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("word", StringArgumentType.string())
                                            .executes(context -> {
                                                String word = StringArgumentType.getString(context, "word");
                                                if (ChatFilterManager.removeWord(word)) {
                                                    sendMessage("§a[ChatFilter] Удалено: §e" + word);
                                                } else {
                                                    sendMessage("§c[ChatFilter] Не найдено: §e" + word);
                                                }
                                                return 1;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        List<String> words = ChatFilterManager.getWords();
                                        if (words.isEmpty()) {
                                            sendMessage("§7[ChatFilter] Список пуст.");
                                        } else {
                                            sendMessage("§6[ChatFilter] Стоп-слова (" + words.size() + "):");
                                            sendMessage("§7 - §e" + String.join("§7, §e", words));
                                        }
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("clear")
                                    .executes(context -> {
                                        ChatFilterManager.clearWords();
                                        sendMessage("§a[ChatFilter] Список очищен.");
                                        return 1;
                                    })
                            )
            );

            // ===== КОМАНДА /dc (DeathCoords) =====
            dispatcher.register(
                    ClientCommandManager.literal("dc")
                            .executes(context -> {
                                DeathCoordsManager.showLastDeath();
                                return 1;
                            })
                            .then(ClientCommandManager.literal("last")
                                    .executes(context -> {
                                        DeathCoordsManager.showLastDeath();
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("clear")
                                    .executes(context -> {
                                        DeathCoordsManager.clearLastDeath();
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("toggle")
                                    .executes(context -> {
                                        ModConfig.deathCoordsEnabled = !ModConfig.deathCoordsEnabled;
                                        ConfigManager.save();
                                        String state = ModConfig.deathCoordsEnabled
                                                ? "§aвключён" : "§cвыключен";
                                        sendMessage("§6DeathCoords " + state);
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("help")
                                    .executes(context -> {
                                        DeathCoordsManager.showHelp();
                                        return 1;
                                    })
                            )
            );

            // ===== НОВАЯ КОМАНДА /cooldowns =====
            dispatcher.register(
                    ClientCommandManager.literal("cooldowns")
                            .executes(context -> {
                                ModConfig.cooldownsEnabled = !ModConfig.cooldownsEnabled;
                                ConfigManager.save();
                                sendMessage("§6CoolDowns " + (ModConfig.cooldownsEnabled
                                        ? "§aвключён" : "§cвыключен"));
                                return 1;
                            })
            );
        });

        // ===== CHAT FILTER: перехват сообщений =====
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (!ModConfig.chatFilterEnabled) return true;
            return !ChatFilterManager.shouldHide(message.getString());
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!ModConfig.chatFilterEnabled) return true;
            return !ChatFilterManager.shouldHide(message.getString());
        });

        // ===== BPS =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                double dx = client.player.getX() - ModConfig.lastPlayerX;
                double dy = client.player.getY() - ModConfig.lastPlayerY;
                double dz = client.player.getZ() - ModConfig.lastPlayerZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                ModConfig.currentBps = distance * 20.0;
                ModConfig.lastPlayerX = client.player.getX();
                ModConfig.lastPlayerY = client.player.getY();
                ModConfig.lastPlayerZ = client.player.getZ();
            }
        });

        // ===== PVP SAFE + DEATH COORDS: отслеживание здоровья =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                lastHealth = -1.0f;
                return;
            }

            float currentHealth = client.player.getHealth();

            // Первый тик после захода в мир — запоминаем здоровье
            if (lastHealth < 0) {
                lastHealth = currentHealth;
                return;
            }

            // Смерть: здоровье упало с положительного до нуля/отрицательного
            if (currentHealth <= 0.0f && lastHealth > 0.0f) {
                DeathCoordsManager.onPlayerDeath(client.player);
                lastHealth = currentHealth;
                return;
            }

            // Урон (не смертельный) → PvPSafe
            if (currentHealth < lastHealth && currentHealth > 0.0f) {
                PvPSafeManager.recordHit();
            }

            lastHealth = currentHealth;
        });

        // ===== PICKUP LOGGER: отправка сообщений в чат (рендер-поток) =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> PickUpLogger.tick());

        // ===== AUTO RECONNECT: тик =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> AutoReconnectManager.tick());

        // ===== ZOOM =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean keyDown = KeyBindings.zoomKey != null && KeyBindings.zoomKey.isDown();
            float targetZoom = 1.0f;
            if (ModConfig.zoomEnabled && keyDown) targetZoom = 1.0f / ModConfig.zoomFactor;
            float smooth = ModConfig.zoomSmoothness;
            if (Math.abs(ModConfig.currentZoom - targetZoom) < 0.001f) {
                ModConfig.currentZoom = targetZoom;
            } else {
                ModConfig.currentZoom += (targetZoom - ModConfig.currentZoom) * smooth;
            }
        });

        // ===== TAPEMOUSE: toggle клавишей =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.tapeMouseKey == null) return;
            while (KeyBindings.tapeMouseKey.consumeClick()) {
                ModConfig.tapeMouseEnabled = !ModConfig.tapeMouseEnabled;
                ConfigManager.save();
                if (client.player != null) {
                    String state = ModConfig.tapeMouseEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(Component.literal("§6TapeMouse " + state), true);
                }
            }
        });

        // ===== TOGGLE КЛАВИШАМИ =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (KeyBindings.customHitSoundsKey != null) {
                while (KeyBindings.customHitSoundsKey.consumeClick()) {
                    ModConfig.customHitSoundsEnabled = !ModConfig.customHitSoundsEnabled;
                    ConfigManager.save();
                    String state = ModConfig.customHitSoundsEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Custom Hit Sounds " + state), true);
                }
            }

            if (KeyBindings.fastExpKey != null) {
                while (KeyBindings.fastExpKey.consumeClick()) {
                    ModConfig.fastExpEnabled = !ModConfig.fastExpEnabled;
                    ConfigManager.save();
                    String state = ModConfig.fastExpEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6FastExp " + state), true);
                }
            }

            if (KeyBindings.shiftTapKey != null) {
                while (KeyBindings.shiftTapKey.consumeClick()) {
                    ModConfig.shiftTapEnabled = !ModConfig.shiftTapEnabled;
                    ConfigManager.save();
                    String state = ModConfig.shiftTapEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6ShiftTap " + state), true);
                }
            }

            if (KeyBindings.comboKey != null) {
                while (KeyBindings.comboKey.consumeClick()) {
                    ModConfig.comboEnabled = !ModConfig.comboEnabled;
                    ConfigManager.save();
                    String state = ModConfig.comboEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Combo Counter " + state), true);
                }
            }

            if (KeyBindings.effectWarningsKey != null) {
                while (KeyBindings.effectWarningsKey.consumeClick()) {
                    ModConfig.effectWarningsEnabled = !ModConfig.effectWarningsEnabled;
                    ConfigManager.save();
                    String state = ModConfig.effectWarningsEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Effect Warnings " + state), true);
                }
            }

            if (KeyBindings.totemLogKey != null) {
                while (KeyBindings.totemLogKey.consumeClick()) {
                    ModConfig.totemLogEnabled = !ModConfig.totemLogEnabled;
                    ConfigManager.save();
                    String state = ModConfig.totemLogEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Totem Log " + state), true);
                }
            }

            if (KeyBindings.pickupLogKey != null) {
                while (KeyBindings.pickupLogKey.consumeClick()) {
                    ModConfig.pickupLogEnabled = !ModConfig.pickupLogEnabled;
                    ConfigManager.save();
                    String state = ModConfig.pickupLogEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6PickUpLogger " + state), true);
                }
            }
        });

        // ===== AUTOSWAP: нажатие клавиши =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.autoSwapKey == null) return;
            while (KeyBindings.autoSwapKey.consumeClick()) {
                if (!ModConfig.autoSwapEnabled) return;
                if (client.player == null || client.level == null) return;
                if (client.screen != null) return;
                if (ModConfig.autoSwapInProgress) return;

                long now = System.currentTimeMillis();
                if (now - ModConfig.autoSwapLastTime < ModConfig.autoSwapCooldown) return;

                int slotToSwap = findAutoSwapSlot(client.player);
                if (slotToSwap < 0) return;

                ModConfig.autoSwapInProgress = true;
                ModConfig.autoSwapStage = 0;
                ModConfig.autoSwapSlotToSwap = slotToSwap;
                ModConfig.autoSwapNextActionTime = now + 25;
            }
        });

        // ===== AUTOSWAP: этапы =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.autoSwapInProgress) return;
            if (client.player == null) {
                ModConfig.autoSwapInProgress = false;
                ModConfig.autoSwapStage = 0;
                ModConfig.autoSwapSlotToSwap = -1;
                return;
            }

            long now = System.currentTimeMillis();
            if (now < ModConfig.autoSwapNextActionTime) return;

            if (ModConfig.autoSwapStage == 0) {
                client.setScreen(new InventoryScreen(client.player));
                ModConfig.autoSwapStage = 1;
                ModConfig.autoSwapNextActionTime = now + 75;
            } else if (ModConfig.autoSwapStage == 1) {
                int slot = ModConfig.autoSwapSlotToSwap;
                if (slot >= 0 && slot < client.player.getInventory().getContainerSize()) {
                    swapOffhandWithSlot(client, slot);
                }
                ModConfig.autoSwapStage = 2;
                ModConfig.autoSwapNextActionTime = now + 75;
            } else if (ModConfig.autoSwapStage == 2) {
                client.setScreen(null);
                ModConfig.autoSwapInProgress = false;
                ModConfig.autoSwapStage = 0;
                ModConfig.autoSwapSlotToSwap = -1;
                ModConfig.autoSwapLastTime = now;
            }
        });

        // ===== WAYPOINTS: кейбинд B =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.waypointsKey == null) return;
            while (KeyBindings.waypointsKey.consumeClick()) {
                if (client.player == null) return;

                double x = client.player.getX();
                double y = client.player.getY();
                double z = client.player.getZ();

                // ИЗМЕНЕНО: WaypointManager
                boolean added = WaypointManager.addWaypoint(x, y, z);
                if (added) {
                    int newIndex = WaypointManager.getWaypoints().size();
                    client.player.displayClientMessage(
                            Component.literal(String.format(
                                    "§a[Waypoints] Добавлена метка §6WP%d§a: §e%d, %d, %d",
                                    newIndex, (int) x, (int) y, (int) z
                            )), true);
                } else {
                    client.player.displayClientMessage(
                            Component.literal("§c[Waypoints] Достигнут лимит меток ("
                                    + ModConfig.waypointsMax + ")"), true);
                }
            }
        });

        // ===== FASTEXP =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.fastExpEnabled) return;
            if (client.player == null || client.level == null) return;
            if (client.screen != null) return;
            ItemStack mainHand = client.player.getMainHandItem();
            if (!mainHand.is(Items.EXPERIENCE_BOTTLE)) return;
            if (client.options.keyUse.isDown()) {
                if (client.gameMode != null) {
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                }
            }
        });

        // ===== AUTOSPRINT =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.autoSprintEnabled) return;
            if (client.player == null) return;
            if (client.screen != null) return;
            if (client.options.keyUp.isDown()) {
                client.player.setSprinting(true);
            }
        });

        // ===== SHIFTTAP =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.shiftTapEnabled) return;
            if (client.player == null) return;
            if (client.screen != null) return;
            long now = System.currentTimeMillis();
            if (ModConfig.shiftTapActive) {
                if (now - ModConfig.shiftTapReleaseTime >= 50) {
                    client.options.keyShift.setDown(true);
                    ModConfig.shiftTapActive = false;
                }
                return;
            }
            if (client.options.keyAttack.isDown() && client.options.keyShift.isDown()) {
                client.options.keyShift.setDown(false);
                ModConfig.shiftTapActive = true;
                ModConfig.shiftTapReleaseTime = now;
            }
        });

        // ===== COMBO: сброс =====
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!ModConfig.comboEnabled) return;
            if (ModConfig.currentCombo <= 0) return;
            long comboNow = System.currentTimeMillis();
            if (comboNow - ModConfig.lastComboTime > (long) ModConfig.comboResetTime * 1000L) {
                ModConfig.currentCombo = 0;
            }
        });

        // ===== TAPEMOUSE: логика (ЛКМ / ПКМ) =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.tapeMouseEnabled) return;
            if (client.player == null || client.level == null) return;
            if (client.screen != null) return;

            if (ModConfig.tapeMouseButton == 1) {
                if (ModConfig.tapeMouseHoldRight) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAttackTime < (long)(ModConfig.tapeMouseDelay * 1000)) return;
                if (client.gameMode != null) {
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    lastAttackTime = currentTime;
                }
                return;
            }

            if (ModConfig.tapeMouseRequireFullAttack) {
                if (client.player.getAttackStrengthScale(0.0f) < 1.0f) return;
            }
            Entity target = client.crosshairPickEntity;
            if (ModConfig.tapeMouseRequireTarget) {
                if (target == null) return;
                boolean isPlayer = target instanceof Player;
                boolean isMob = target instanceof LivingEntity && !isPlayer;
                switch (ModConfig.tapeMouseTarget) {
                    case 1: if (!isMob) return; break;
                    case 2: if (!isPlayer) return; break;
                }
            } else {
                if (target != null) {
                    boolean isPlayer = target instanceof Player;
                    boolean isMob = target instanceof LivingEntity && !isPlayer;
                    switch (ModConfig.tapeMouseTarget) {
                        case 1: if (!isMob) return; break;
                        case 2: if (!isPlayer) return; break;
                    }
                }
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime < (long)(ModConfig.tapeMouseDelay * 1000)) return;
            if (client.gameMode != null) {
                if (target != null) client.gameMode.attack(client.player, target);
                client.player.swing(InteractionHand.MAIN_HAND);
                lastAttackTime = currentTime;
            }
        });

        // ===== TAPEMOUSE: отпускание ПКМ при выключении =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean shouldHold = ModConfig.tapeMouseEnabled
                    && ModConfig.tapeMouseButton == 1
                    && ModConfig.tapeMouseHoldRight
                    && client.screen == null;

            if (shouldHold) {
                if (!tapeMouseWeHeldRMB) {
                    client.options.keyUse.setDown(true);
                    tapeMouseWeHeldRMB = true;
                }
            } else {
                if (tapeMouseWeHeldRMB) {
                    client.options.keyUse.setDown(false);
                    tapeMouseWeHeldRMB = false;
                }
            }
        });

        // ===== HUD =====
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "hud"),
                (graphics, tickCounter) -> renderHud(graphics)
        );
    }

    // ===== HUD =====
    private static void renderHud(GuiGraphics graphics) {
        if (!ModConfig.showHud) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        int alpha = ModConfig.hudAlpha;
        int baseColor = ModConfig.hudColor;
        int color = (baseColor & 0x00FFFFFF) | (alpha << 24);

        renderWaypoints(graphics);

        if (ModConfig.crosshairEnabled) {
            int chCenterX = client.getWindow().getGuiScaledWidth() / 2;
            int chCenterY = client.getWindow().getGuiScaledHeight() / 2;

            int chColor = (ModConfig.crosshairColor & 0x00FFFFFF) | (ModConfig.crosshairAlpha << 24);
            int size = ModConfig.crosshairSize;
            int thick = ModConfig.crosshairThickness;
            int gap = ModConfig.crosshairGap;
            int shape = ModConfig.crosshairShape;

            switch (shape) {
                case 0 -> {
                    graphics.fill(chCenterX - thick / 2, chCenterY - gap - size,
                            chCenterX + (thick + 1) / 2, chCenterY - gap, chColor);
                    graphics.fill(chCenterX - thick / 2, chCenterY + gap,
                            chCenterX + (thick + 1) / 2, chCenterY + gap + size, chColor);
                    graphics.fill(chCenterX - gap - size, chCenterY - thick / 2,
                            chCenterX - gap, chCenterY + (thick + 1) / 2, chColor);
                    graphics.fill(chCenterX + gap, chCenterY - thick / 2,
                            chCenterX + gap + size, chCenterY + (thick + 1) / 2, chColor);
                }
                case 1 -> {
                    int dotSize = Math.max(1, thick);
                    int half = dotSize / 2;
                    graphics.fill(chCenterX - half, chCenterY - half,
                            chCenterX - half + dotSize, chCenterY - half + dotSize, chColor);
                }
                case 2 -> {
                    int radius = Math.max(2, size);
                    int points = 24;
                    for (int i = 0; i < points; i++) {
                        double angle = 2 * Math.PI * i / points;
                        int px = chCenterX + (int) Math.round(Math.cos(angle) * radius);
                        int py = chCenterY + (int) Math.round(Math.sin(angle) * radius);
                        int pt = Math.max(1, thick);
                        graphics.fill(px - pt / 2, py - pt / 2,
                                px - pt / 2 + pt, py - pt / 2 + pt, chColor);
                    }
                }
                case 3 -> {
                    drawTriangle(graphics, chCenterX, chCenterY - gap - size, size, thick, 0, chColor);
                    drawTriangle(graphics, chCenterX, chCenterY + gap + size, size, thick, 1, chColor);
                    drawTriangle(graphics, chCenterX - gap - size, chCenterY, size, thick, 2, chColor);
                    drawTriangle(graphics, chCenterX + gap + size, chCenterY, size, thick, 3, chColor);
                }
                case 4 -> {
                    graphics.fill(chCenterX - thick / 2, chCenterY - gap - size,
                            chCenterX + (thick + 1) / 2, chCenterY - gap, chColor);
                    graphics.fill(chCenterX - thick / 2, chCenterY + gap,
                            chCenterX + (thick + 1) / 2, chCenterY + gap + size, chColor);
                    graphics.fill(chCenterX - gap - size, chCenterY - thick / 2,
                            chCenterX - gap, chCenterY + (thick + 1) / 2, chColor);
                    graphics.fill(chCenterX + gap, chCenterY - thick / 2,
                            chCenterX + gap + size, chCenterY + (thick + 1) / 2, chColor);
                    int dotSize = Math.max(1, thick);
                    int half = dotSize / 2;
                    graphics.fill(chCenterX - half, chCenterY - half,
                            chCenterX - half + dotSize, chCenterY - half + dotSize, chColor);
                }
            }
        }

        if (ModConfig.showModLogo) {
            int logoX = ModConfig.modLogoX;
            int logoY = ModConfig.modLogoY;

            Identifier logoId = Identifier.fromNamespaceAndPath(
                    "resistancedlc", "textures/gui/icon.png"
            );

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    logoId,
                    logoX, logoY,
                    0, 0,
                    16, 16,
                    16, 16
            );

            drawHudString(graphics, client.font, "ResistanceDLC",
                    logoX + 20, logoY + 4, color);
        }

        if (ModConfig.comboEnabled && ModConfig.currentCombo > 0) {
            String comboText = "x" + ModConfig.currentCombo;
            int fontSize = ModConfig.comboFontSize;

            float scale;
            if (fontSize == 0) scale = 1.0f;
            else if (fontSize == 1) scale = 1.5f;
            else scale = 2.0f;

            graphics.pose().pushMatrix();
            graphics.pose().translate(ModConfig.comboX, ModConfig.comboY);
            graphics.pose().scale(scale, scale);

            graphics.drawString(client.font, comboText, 0, 0, ModConfig.comboColor, true);

            graphics.pose().popMatrix();
        }

        if (ModConfig.pvpSafeEnabled
                && ModConfig.pvpSafeShowHud
                && PvPSafeManager.isInCombat()) {
            int remaining = PvPSafeManager.getRemainingSeconds();
            String text = "§c⚔ Бой: " + remaining + " сек";
            graphics.drawString(client.font, text,
                    ModConfig.pvpSafeHudX,
                    ModConfig.pvpSafeHudY,
                    ModConfig.pvpSafeHudColor,
                    true);
        }

        if (ModConfig.showCoords) {
            drawHudString(graphics, client.font,
                    String.format("XYZ: %d / %d / %d",
                            (int) client.player.getX(),
                            (int) client.player.getY(),
                            (int) client.player.getZ()),
                    ModConfig.coordsX, ModConfig.coordsY, color);
        }

        if (ModConfig.showBiome) {
            String biome = client.level.getBiome(client.player.blockPosition())
                    .unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("unknown");
            drawHudString(graphics, client.font, "Биом: " + biome,
                    ModConfig.biomeX, ModConfig.biomeY, color);
        }

        if (ModConfig.showTime) {
            long time = client.level.getDayTime() % 24000;
            String timeStr = time < 12000 ? "День" : "Ночь";
            drawHudString(graphics, client.font, "Время: " + timeStr,
                    ModConfig.timeX, ModConfig.timeY, color);
        }

        if (ModConfig.showFps) {
            String label = ModConfig.fpsRussian ? "КВС" : "FPS";
            drawHudString(graphics, client.font, label + ": " + client.getFps(),
                    ModConfig.fpsX, ModConfig.fpsY, color);
        }

        if (ModConfig.showPing) {
            int ping = 0;
            if (client.getConnection() != null
                    && client.getConnection().getPlayerInfo(client.player.getUUID()) != null) {
                ping = client.getConnection().getPlayerInfo(client.player.getUUID()).getLatency();
            }
            String label = ModConfig.pingRussian ? "Пинг" : "Ping";
            drawHudString(graphics, client.font, label + ": " + ping + " ms",
                    ModConfig.pingX, ModConfig.pingY, color);
        }

        if (ModConfig.showTps) {
            float tps = 20.0f;
            if (client.getSingleplayerServer() != null) {
                long tickTime = client.getSingleplayerServer().getAverageTickTimeNanos();
                if (tickTime > 0) {
                    tps = Math.min(20.0f, 1_000_000_000.0f / tickTime);
                }
            }
            String label = ModConfig.tpsRussian ? "ТВС" : "TPS";
            drawHudString(graphics, client.font,
                    String.format("%s: %.1f", label, tps),
                    ModConfig.tpsX, ModConfig.tpsY, color);
        }

        if (ModConfig.showBps) {
            String label = ModConfig.bpsRussian ? "БВС" : "BPS";
            drawHudString(graphics, client.font,
                    String.format("%s: %.2f", label, ModConfig.currentBps),
                    ModConfig.bpsX, ModConfig.bpsY, color);
        }

        if (ModConfig.showDirection) {
            float yaw = client.player.getYRot();
            yaw = ((yaw % 360) + 360) % 360;
            String dir;
            if (ModConfig.directionRussian) {
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
            String label = ModConfig.directionRussian ? "Направление" : "Direction";
            drawHudString(graphics, client.font, label + ": " + dir,
                    ModConfig.directionX, ModConfig.directionY, color);
        }

        if (ModConfig.showHitCounter) {
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
                String label = ModConfig.hitCounterRussian ? "Удары" : "Hits";
                drawHudString(graphics, client.font, label + ": " + hitsLeft,
                        ModConfig.hitCounterX, ModConfig.hitCounterY,
                        hpColor);
            }
        }

        // ===== AUTO RECONNECT HUD =====
        if (AutoReconnectManager.isReconnecting()
                && ModConfig.autoReconnectShowHud) {
            int remaining = AutoReconnectManager.getRemainingSeconds();
            String text = "§c[AutoReconnect] §fЧерез " + remaining + " сек §7(ESC — отмена)";
            graphics.drawString(client.font, text,
                    client.getWindow().getGuiScaledWidth() / 2 - client.font.width(text) / 2,
                    client.getWindow().getGuiScaledHeight() / 2 + 30,
                    0xFFFFFFFF,
                    true);
        }

        if (ModConfig.showPotionEffects) {
            Collection<MobEffectInstance> effects = client.player.getActiveEffects();
            if (!effects.isEmpty()) {
                if (ModConfig.potionEffectsIcons) {
                    int yOffset = 0;
                    for (MobEffectInstance effect : effects) {
                        MobEffect type = effect.getEffect().value();
                        Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(type);
                        if (effectId == null) continue;

                        int iconX = ModConfig.potionEffectsX;
                        int iconY = ModConfig.potionEffectsY + yOffset;

                        Identifier iconId = Identifier.fromNamespaceAndPath(
                                effectId.getNamespace(),
                                "textures/mob_effect/" + effectId.getPath() + ".png"
                        );

                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                iconId,
                                iconX, iconY,
                                0, 0,
                                18, 18,
                                18, 18
                        );

                        String name = type.getDisplayName().getString();
                        int amp = effect.getAmplifier();
                        if (amp > 0) name = name + " " + toRoman(amp + 1);

                        int duration = effect.getDuration();
                        int totalSeconds = duration / 20;
                        int minutes = totalSeconds / 60;
                        int seconds = totalSeconds % 60;
                        String timeStr = String.format("%d:%02d", minutes, seconds);

                        String fullText = name + " " + timeStr;

                        drawHudString(graphics, client.font, fullText,
                                iconX + 21, iconY + 5, color);

                        yOffset += 20;
                    }
                } else {
                    String label = ModConfig.potionEffectsRussian ? "Эффекты:" : "Effects:";
                    drawHudString(graphics, client.font, label,
                            ModConfig.potionEffectsX, ModConfig.potionEffectsY, color);

                    int textOffset = 12;
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
                                ModConfig.potionEffectsX,
                                ModConfig.potionEffectsY + textOffset, color);
                        textOffset += 10;
                    }
                }
            }
        }

        if (ModConfig.effectWarningsEnabled) {
            Collection<MobEffectInstance> warnEffects = client.player.getActiveEffects();
            int warnY = 0;
            for (MobEffectInstance effect : warnEffects) {
                int durationSec = effect.getDuration() / 20;
                if (durationSec > ModConfig.effectWarningsThreshold) continue;

                MobEffect type = effect.getEffect().value();

                int timeColor;
                if (durationSec > ModConfig.effectWarningsThreshold * 2 / 3) {
                    timeColor = 0xFF00FF00;
                } else if (durationSec > ModConfig.effectWarningsThreshold / 3) {
                    timeColor = 0xFFFFFF00;
                } else {
                    timeColor = 0xFFFF0000;
                }

                int finalColor = (timeColor & 0x00FFFFFF) | (ModConfig.effectWarningsAlpha << 24);
                int alphaColor = (ModConfig.effectWarningsColor & 0x00FFFFFF)
                        | (ModConfig.effectWarningsAlpha << 24);

                int x = ModConfig.effectWarningsX;
                int y = ModConfig.effectWarningsY + warnY;

                if (ModConfig.effectWarningsShowIcon) {
                    Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(type);
                    if (effectId != null) {
                        Identifier iconId = Identifier.fromNamespaceAndPath(
                                effectId.getNamespace(),
                                "textures/mob_effect/" + effectId.getPath() + ".png"
                        );

                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                iconId,
                                x, y,
                                0, 0,
                                18, 18,
                                18, 18
                        );
                        x += 21;
                    }
                }

                if (ModConfig.effectWarningsShowName) {
                    String name = type.getDisplayName().getString();
                    graphics.drawString(client.font, name, x, y + 5, alphaColor, true);
                    x += client.font.width(name) + 4;
                }

                String timeStr = durationSec + "s";
                graphics.drawString(client.font, timeStr, x, y + 5, finalColor, true);

                warnY += 20;
            }
        }
        // ===== COOLDOWNS =====
        if (ModConfig.cooldownsEnabled) {
            java.util.List<CooldownsManager.CooldownEntry> cooldowns =
                    CooldownsManager.getActiveCooldowns();

            if (!cooldowns.isEmpty()) {
                int cdColor = (ModConfig.cooldownsColor & 0x00FFFFFF)
                        | (ModConfig.cooldownsAlpha << 24);
                int cdX = ModConfig.cooldownsX;
                int cdY = ModConfig.cooldownsY;

                float fontSize;
                if (ModConfig.cooldownsFontSize == 0) fontSize = 1.0f;
                else if (ModConfig.cooldownsFontSize == 2) fontSize = 1.5f;
                else fontSize = 1.0f;

                int rowHeight = 20;

                for (CooldownsManager.CooldownEntry entry : cooldowns) {
                    int curX = cdX;
                    int curY = cdY;

                    // Иконка предмета
                    if (ModConfig.cooldownsShowIcon) {
                        graphics.renderItem(entry.stack, curX, curY);
                        // Оверлей "потемнения" поверх иконки (настраиваемый)
                        int overlayAlpha = (int) (entry.percent * ModConfig.cooldownsIconDarkening);
                        if (overlayAlpha > 0) {
                            graphics.fill(curX, curY, curX + 16, curY + 16,
                                    (overlayAlpha << 24));
                        }
                        curX += 20;
                    }

                    // Название
                    if (ModConfig.cooldownsShowName) {
                        String name = entry.stack.getHoverName().getString();
                        graphics.drawString(client.font, name,
                                curX, curY + 1, cdColor, true);
                        curX += client.font.width(name) + 6;
                    }

                    // Таймер
                    if (ModConfig.cooldownsShowTime) {
                        String timeStr = CooldownsManager.formatTime(entry.remainingSeconds);
                        graphics.drawString(client.font, timeStr,
                                curX, curY + 1, cdColor, true);
                    }

                    cdY += rowHeight;
                }
            }
        }
        if (ModConfig.showEquipmentHud) {
            int guiW = client.getWindow().getGuiScaledWidth();
            int guiH = client.getWindow().getGuiScaledHeight();
            int hotbarRight = (guiW + 182) / 2;
            int hotbarBottom = guiH - 22;
            int slotX = hotbarRight + ModConfig.equipmentHudX;
            int slotY = hotbarBottom + ModConfig.equipmentHudY;

            List<ItemStack> items = new ArrayList<>();
            ItemStack[] armor = {
                    client.player.getItemBySlot(EquipmentSlot.FEET),
                    client.player.getItemBySlot(EquipmentSlot.LEGS),
                    client.player.getItemBySlot(EquipmentSlot.CHEST),
                    client.player.getItemBySlot(EquipmentSlot.HEAD),
                    client.player.getItemBySlot(EquipmentSlot.OFFHAND),
                    client.player.getItemBySlot(EquipmentSlot.MAINHAND)
            };
            for (ItemStack s : armor) if (!s.isEmpty()) items.add(s);

            int renderY = slotY;
            for (ItemStack stack : items) {
                drawEquipmentSlot(graphics, stack, slotX, renderY);
                renderY -= 20;
            }

            int arrowCount = 0;
            for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = client.player.getInventory().getItem(i);
                if (stack.is(Items.ARROW)) arrowCount += stack.getCount();
            }
            if (arrowCount > 0) {
                drawHudString(graphics, client.font, "➤ " + arrowCount, slotX, renderY + 4, color);
            }
        }
    }

    private static void showWaypointsHelp() {
        sendMessage("§6§l══════ Waypoints — команды ══════");
        sendMessage("§e/wp §7— показать эту справку");
        sendMessage("§e/wp help §7— показать эту справку");
        sendMessage("§e/wp add §7— добавить метку в текущей позиции");
        sendMessage("§e/wp list §7— список всех меток");
        sendMessage("§e/wp remove <имя> §7— удалить метку по имени");
        sendMessage("§e/wp rename <старое> <новое> §7— переименовать метку");
        sendMessage("§e/wp clear §7— удалить все метки");
        sendMessage("§7Клавиша §eB §7— быстро добавить метку");
        sendMessage("§7GUI: §eG §7→ вкладка §6Visual §7→ §6Waypoints");
        sendMessage("§7В GUI у каждой метки есть кнопки §e✎§7 (§7переименовать§7) и §c× §7(удалить)");
        sendMessage("§6§l════════════════════════════════");
    }
    private static void showCfgHelp() {
        sendMessage("§6§l══════ ConfigManager — команды ══════");
        sendMessage("§e/cfg §7— эта справка");
        sendMessage("§e/cfg help §7— эта справка");
        sendMessage("§e/cfg dir §7— открыть папку с конфигами");
        sendMessage("§e/cfg list §7— список сохранённых конфигов");
        sendMessage("§e/cfg save <имя> §7— сохранить конфиг под именем");
        sendMessage("§e/cfg load <имя> §7— загрузить конфиг по имени");
        sendMessage("§e/cfg remove <имя> §7— удалить конфиг по имени");
        sendMessage("§7");
        sendMessage("§7Что такое ConfigManager?");
        sendMessage("§7Это система сохранения всех настроек мода в JSON-файлы.");
        sendMessage("§7Каждый конфиг хранится в §e.run/config/resistancedlc/<имя>.json");
        sendMessage("§7Можно создавать разные пресеты (PvP, PvE, HUD-only)");
        sendMessage("§7и переключаться между ними на лету.");
        sendMessage("§7");
        sendMessage("§7GUI-версия: §eG §7→ раздел §6Misc §7→ §6Config Manager");
        sendMessage("§6§l════════════════════════════════════");
    }
    private static void renderWaypoints(GuiGraphics graphics) {
        if (!ModConfig.waypointsEnabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (client.options.hideGui) return;

        // ИЗМЕНЕНО: List<Waypoint> + WaypointManager
        List<Waypoint> waypoints = WaypointManager.getWaypoints();
        if (waypoints.isEmpty()) return;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        TrackedWaypoint.Projector projector = client.gameRenderer;

        int EDGE_MARGIN = 20;

        // ИЗМЕНЕНО: for (Waypoint wp : waypoints)
        for (Waypoint wp : waypoints) {
            double dist = wp.distanceTo(px, py, pz);

            int wpColor;
            if (dist < 50.0) {
                wpColor = 0xFF00FF00;
            } else if (dist < 200.0) {
                wpColor = 0xFFFFFF00;
            } else {
                wpColor = 0xFFFF0000;
            }

            Vec3 ndc;
            try {
                ndc = projector.projectPointToScreen(new Vec3(wp.x(), wp.y(), wp.z()));
            } catch (Exception e) {
                continue;
            }
            if (ndc == null) continue;

            boolean behind = ndc.z > 1.0;
            double ndcX = ndc.x;
            double ndcY = behind ? -ndc.y : ndc.y;

            int screenX = (int) ((ndcX + 1.0) * 0.5 * screenW);
            int screenY = (int) ((1.0 - ndcY) * 0.5 * screenH);

            boolean onScreen = !behind
                    && ndcX >= -1.0 && ndcX <= 1.0
                    && ndcY >= -1.0 && ndcY <= 1.0;

            if (onScreen) {
                drawWaypointDiamond(graphics, screenX, screenY, 5, wpColor);

                String label = wp.name();
                String distStr = String.format("%.0fm", dist);

                int labelW = client.font.width(label);
                int distW = client.font.width(distStr);

                graphics.drawString(client.font, label,
                        screenX - labelW / 2, screenY - 16, wpColor, true);
                graphics.drawString(client.font, distStr,
                        screenX - distW / 2, screenY + 8, wpColor, true);
            } else {
                boolean onLeft = ndcX < 0;

                int arrowX = onLeft ? EDGE_MARGIN : screenW - EDGE_MARGIN;

                double ndcYClamped = Math.max(-1.0, Math.min(1.0, ndcY));
                int arrowY = (int) ((1.0 - ndcYClamped) * 0.5 * screenH);
                arrowY = Math.max(EDGE_MARGIN, Math.min(screenH - EDGE_MARGIN, arrowY));

                double dirX = onLeft ? -1.0 : 1.0;

                drawWaypointArrow(graphics, arrowX, arrowY, dirX, wpColor);

                String label = wp.name();
                String distStr = String.format("%.0fm", dist);
                String fullText = "§e" + label + " §7" + distStr;

                int textW = client.font.width(fullText);
                int textX = onLeft ? arrowX + 12 : arrowX - textW - 12;
                int textY = arrowY - 4;

                textX = Math.max(4, Math.min(screenW - textW - 4, textX));
                textY = Math.max(4, Math.min(screenH - 12, textY));

                graphics.drawString(client.font, fullText, textX, textY, wpColor, true);
            }
        }
    }

    private static void drawWaypointDiamond(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= 0; dy++) {
            int halfWidth = radius + dy;
            if (halfWidth < 0) halfWidth = 0;
            graphics.fill(cx - halfWidth, cy + dy, cx + halfWidth + 1, cy + dy + 1, color);
        }
        for (int dy = 1; dy <= radius; dy++) {
            int halfWidth = radius - dy;
            if (halfWidth < 0) halfWidth = 0;
            graphics.fill(cx - halfWidth, cy + dy, cx + halfWidth + 1, cy + dy + 1, color);
        }
    }

    private static void drawWaypointArrow(GuiGraphics graphics, int cx, int cy,
                                          double dirX, int color) {
        int arrowSize = 8;
        int baseWidth = 6;

        int tipX = cx + (int) Math.round(dirX * arrowSize);
        int baseX = cx - (int) Math.round(dirX * arrowSize);

        int startX = Math.min(tipX, baseX);
        int endX = Math.max(tipX, baseX);

        for (int x = startX; x <= endX; x++) {
            double t;
            if (dirX > 0) {
                t = (double) (x - baseX) / (tipX - baseX);
            } else {
                t = (double) (baseX - x) / (baseX - tipX);
            }
            t = Math.max(0.0, Math.min(1.0, t));

            int halfH = (int) Math.round(baseWidth * (1.0 - t));

            graphics.fill(x, cy - halfH, x + 1, cy + halfH + 1, color);
        }
    }

    private static void drawTriangle(GuiGraphics graphics, int tipX, int tipY,
                                     int size, int thick, int direction, int color) {
        int baseSize = Math.max(2, thick * 2);
        for (int i = 0; i < size; i++) {
            int width = (int) Math.round((double) baseSize * i / size);
            if (width < 1) width = 1;
            switch (direction) {
                case 0 -> {
                    int y = tipY + i;
                    graphics.fill(tipX - width / 2, y, tipX - width / 2 + width, y + 1, color);
                }
                case 1 -> {
                    int y = tipY - i;
                    graphics.fill(tipX - width / 2, y, tipX - width / 2 + width, y + 1, color);
                }
                case 2 -> {
                    int x = tipX + i;
                    graphics.fill(x, tipY - width / 2, x + 1, tipY - width / 2 + width, color);
                }
                case 3 -> {
                    int x = tipX - i;
                    graphics.fill(x, tipY - width / 2, x + 1, tipY - width / 2 + width, color);
                }
            }
        }
    }

    private static int findAutoSwapSlot(Player player) {
        ItemStack offhand = player.getOffhandItem();

        boolean offhandIsHead = offhand.is(Items.PLAYER_HEAD);
        boolean offhandIsTotem = offhand.is(Items.TOTEM_OF_UNDYING);

        boolean offhandAllowedHead = false;
        boolean offhandAllowedTotem = false;

        switch (ModConfig.autoSwapMode) {
            case 0: offhandAllowedHead = true; break;
            case 1: offhandAllowedTotem = true; break;
            case 2: offhandAllowedHead = true; offhandAllowedTotem = true; break;
            case 3: offhandAllowedHead = true; offhandAllowedTotem = true; break;
        }

        boolean offhandMatches = (offhandAllowedHead && offhandIsHead)
                || (offhandAllowedTotem && offhandIsTotem);
        if (!offhandMatches) return -1;

        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (ModConfig.autoSwapMode == 0) {
                if (stack.is(Items.PLAYER_HEAD)) return i;
            } else if (ModConfig.autoSwapMode == 1) {
                if (stack.is(Items.TOTEM_OF_UNDYING)) return i;
            } else {
                if (offhandIsHead && stack.is(Items.TOTEM_OF_UNDYING)) return i;
                if (offhandIsTotem && stack.is(Items.PLAYER_HEAD)) return i;
            }
        }

        return -1;
    }

    private static void swapOffhandWithSlot(Minecraft client, int slotIndex) {
        if (client.player == null) return;
        if (client.gameMode == null) return;
        if (slotIndex == 40) return;

        ItemStack targetStack = client.player.getInventory().getItem(slotIndex);
        if (targetStack.isEmpty()) return;

        client.gameMode.handleInventoryMouseClick(
                0,
                slotIndex,
                40,
                ClickType.SWAP,
                client.player
        );
    }

    private static void drawHudString(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        if (ModConfig.hudBackgroundEnabled) {
            int textWidth = font.width(text);
            int bgAlpha = ModConfig.hudBackgroundAlpha;
            int bgBaseColor = ModConfig.hudBackgroundColor;
            int bgColor = (bgBaseColor & 0x00FFFFFF) | (bgAlpha << 24);
            int height = ModConfig.hudBackgroundHeight;
            graphics.fill(x - 1, y - 1, x + textWidth + 1, y - 1 + height, bgColor);
        }
        graphics.drawString(font, text, x, y, color, true);
    }

    private static void drawEquipmentSlot(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);

        if (ModConfig.equipmentShowDurability && stack.isDamageableItem()) {
            int damage = stack.getDamageValue();
            int maxDamage = stack.getMaxDamage();
            if (damage > 0 && maxDamage > 0) {
                float percent = 1.0f - (float) damage / maxDamage;
                int barWidth = 16;
                int filled = (int) (barWidth * percent);

                graphics.fill(x, y + 17, x + barWidth, y + 18, 0xFF400000);

                int barColor;
                if (percent > 0.75f) barColor = 0xFF00FF00;
                else if (percent > 0.50f) barColor = 0xFFFFFF00;
                else if (percent > 0.25f) barColor = 0xFFFF8800;
                else barColor = 0xFFFF0000;
                graphics.fill(x, y + 17, x + filled, y + 18, barColor);
            }
        }
    }

    private static String toRoman(int num) {
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (num >= 0 && num < roman.length) return roman[num];
        return String.valueOf(num);
    }

    private static void sendMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}