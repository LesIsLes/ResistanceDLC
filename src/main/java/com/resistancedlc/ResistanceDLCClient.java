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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import com.resistancedlc.JumpCirclesManager;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
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
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ResistanceDLCClient implements ClientModInitializer {

    private static long lastAttackTime = 0;
    private static boolean tapeMouseWeHeldRMB = false;
    private static float lastHealth = -1.0f;
    private static boolean crosshairHeatmapAttackWasDown = false;

    /** Флаг: смерть уже записана в DeathRecap. */
    private static boolean deathRecapCaptured = false;

    // ===== TargetEsp =====
    private static final com.resistancedlc.targetesp.TargetESP TARGET_ESP =
            new com.resistancedlc.targetesp.TargetESP(
                    com.resistancedlc.targetesp.TargetManagerHolder.MANAGER);

    @Override
    public void onInitializeClient() {
        // ===== ЛОКАЛИЗАЦИЯ =====
        LocalizationManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> ConfigManager.tick());

        ConfigManager.load();
        KeyBindings.register();

        MusicPlayerManager.init();

        // ===== ATTACK CALLBACK =====
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (ModConfig.strikeRangeEnabled && entity != null) {
                double dist = player.distanceTo(entity);
                StrikeRangeManager.onHit(dist);
                ModConfig.strikeRangeLastTarget = entity.getName().getString();
            }

            if (ModConfig.pvpSafeEnabled && entity instanceof Player && entity != player) {
                PvPSafeManager.recordHit();
            }

            if (ModConfig.targetEspEnabled && entity instanceof LivingEntity) {
                com.resistancedlc.targetesp.TargetManagerHolder.MANAGER.setCurrentTarget(entity);
            }

            if (ModConfig.killStreakEnabled && entity != null) {
                KillStreakManager.onAttack(entity);
            }

            if (ModConfig.crosshairHeatmapEnabled && entity instanceof LivingEntity) {
                boolean isPlayer = entity instanceof Player;
                boolean isMob = !isPlayer;

                boolean track = (isPlayer && ModConfig.crosshairHeatmapTrackPlayers)
                        || (isMob && ModConfig.crosshairHeatmapTrackMobs);

                if (track) {
                    recordHeatmapHit(entity);
                }
            }

            return InteractionResult.PASS;
        });

        // ===== PVP SAFE + AutoReconnect =====
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

            net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
                    .allowMouseClick(screen).register((s, mouseEvent) -> {
                        String disconnectText = Component.translatable("menu.disconnect").getString();
                        double mx = mouseEvent.x();
                        double my = mouseEvent.y();
                        for (Object child : s.children()) {
                            if (!(child instanceof net.minecraft.client.gui.components.Button btn)) continue;
                            if (!btn.active) continue;
                            if (!btn.getMessage().getString().equals(disconnectText)) continue;
                            if (mx >= btn.getX() && mx <= btn.getX() + btn.getWidth()
                                    && my >= btn.getY() && my <= btn.getY() + btn.getHeight()) {
                                AutoReconnectManager.setManualDisconnect(true);
                                break;
                            }
                        }
                        return true;
                    });
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, keyEvent) -> {
                if (!AutoReconnectManager.isReconnecting()) return true;

                if (keyEvent.key() == 256) {
                    AutoReconnectManager.cancel();
                    if (client.player != null) {
                        client.player.displayClientMessage(
                                Component.literal("§c" + LocalizationManager.get("gui.resistancedlc.autoreconnect.cancelled")), true);
                    }
                    return false;
                }
                return true;
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ConfigManager.saveNow();
            TotemTracker.clear();
            PvPSafeManager.reset();
            AutoGGManager.reset();
            StrikeRangeManager.reset();
            com.resistancedlc.targetesp.TargetManagerHolder.MANAGER.reset();
            JumpCirclesManager.reset();
            GammaUtilManager.reset();
            MusicPlayerManager.stop();
            lastHealth = -1.0f;
            LowHPAlertManager.reset();
            AutoRespawnManager.reset();
            ArmorAlertManager.reset();
            AutoTPAcceptManager.reset();
            KillStreakManager.reset();
            CrosshairHeatmapManager.reset();
            PingIndicatorManager.reset();
            DeathRecapManager.clearArmorSnapshot();
            AutoReconnectManager.onDisconnect();
            deathRecapCaptured = false;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            AutoReconnectManager.onJoin();

            MusicPlayerManager.rescan();
            if (ModConfig.musicPlayerEnabled && ModConfig.musicAutoPlay) {
                MusicPlayerManager.play();
            }

            GammaUtilManager.reset();
            deathRecapCaptured = false;
        });

        // ===== КОМАНДЫ =====
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // ===== WAYPOINTS COMMAND =====
            dispatcher.register(
                    ClientCommandManager.literal("wp")
                            .then(ClientCommandManager.literal("add")
                                    .then(ClientCommandManager.argument("x",
                                                    com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                            .then(ClientCommandManager.argument("y",
                                                            com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                    .then(ClientCommandManager.argument("z",
                                                                    com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                            .executes(context -> {
                                                                int x = com.mojang.brigadier.arguments.IntegerArgumentType
                                                                        .getInteger(context, "x");
                                                                int y = com.mojang.brigadier.arguments.IntegerArgumentType
                                                                        .getInteger(context, "y");
                                                                int z = com.mojang.brigadier.arguments.IntegerArgumentType
                                                                        .getInteger(context, "z");
                                                                boolean added = WaypointManager.addWaypoint(x, y, z);
                                                                if (added) {
                                                                    sendMessage("§a[Waypoints] Добавлена метка §e" + x + ", " + y + ", " + z);
                                                                } else {
                                                                    sendMessage("§c[Waypoints] Достигнут лимит (" + ModConfig.waypointsMax + ")");
                                                                }
                                                                return 1;
                                                            })))))
            );
            dispatcher.register(
                    ClientCommandManager.literal("cfg")
                            .executes(context -> { showCfgHelp(); return 1; })
                            .then(ClientCommandManager.literal("help")
                                    .executes(context -> { showCfgHelp(); return 1; }))
                            .then(ClientCommandManager.literal("dir")
                                    .executes(context -> {
                                        ConfigManager.openFolder();
                                        sendMessage("§a" + LocalizationManager.get("gui.resistancedlc.message.folder_opened",
                                                ConfigManager.getConfigDirPath()));
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        List<String> configs = ConfigManager.listConfigs();
                                        if (configs.isEmpty()) {
                                            sendMessage("§7" + LocalizationManager.get("gui.resistancedlc.command.cfg.none"));
                                        } else {
                                            sendMessage("§6" + LocalizationManager.get("gui.resistancedlc.command.cfg.saved_count",
                                                    configs.size()));
                                            for (String name : configs) {
                                                sendMessage("§7 - §e" + name);
                                            }
                                        }
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("save")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                if (ConfigManager.saveAs(name)) {
                                                    sendMessage("§a" + LocalizationManager.get("gui.resistancedlc.message.config_saved", name));
                                                } else {
                                                    sendMessage("§c" + LocalizationManager.get("gui.resistancedlc.message.config_save_failed", name));
                                                }
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("load")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                if (ConfigManager.loadFrom(name)) {
                                                    sendMessage("§a" + LocalizationManager.get("gui.resistancedlc.message.config_loaded", name));
                                                    if (Minecraft.getInstance().screen instanceof AccordionScreen) {
                                                        Minecraft.getInstance().setScreen(new AccordionScreen());
                                                    }
                                                } else {
                                                    sendMessage("§c" + LocalizationManager.get("gui.resistancedlc.message.config_not_found", name));
                                                }
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                if (ConfigManager.remove(name)) {
                                                    sendMessage("§a" + LocalizationManager.get("gui.resistancedlc.message.config_removed", name));
                                                } else {
                                                    sendMessage("§c" + LocalizationManager.get("gui.resistancedlc.message.config_not_found", name));
                                                }
                                                return 1;
                                            })))
            );

            dispatcher.register(
                    ClientCommandManager.literal("chatfilter")
                            .executes(context -> {
                                sendMessage("§6§l" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.title"));
                                sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.add"));
                                sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.remove"));
                                sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.list"));
                                sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.clear"));
                                sendMessage("§6§l════════════════════════════════");
                                return 1;
                            })
                            .then(ClientCommandManager.literal("add")
                                    .then(ClientCommandManager.argument("word", StringArgumentType.string())
                                            .executes(context -> {
                                                String word = StringArgumentType.getString(context, "word");
                                                if (ChatFilterManager.addWord(word)) {
                                                    sendMessage("§a" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.added") + "§e" + word);
                                                } else {
                                                    sendMessage("§c" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.already") + "§e" + word);
                                                }
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("word", StringArgumentType.string())
                                            .executes(context -> {
                                                String word = StringArgumentType.getString(context, "word");
                                                if (ChatFilterManager.removeWord(word)) {
                                                    sendMessage("§a" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.removed") + "§e" + word);
                                                } else {
                                                    sendMessage("§c" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.notfound") + "§e" + word);
                                                }
                                                return 1;
                                            })))
                            .then(ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        List<String> words = ChatFilterManager.getWords();
                                        if (words.isEmpty()) {
                                            sendMessage("§7" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.empty"));
                                        } else {
                                            sendMessage("§6" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.words", words.size()));
                                            sendMessage("§7 - §e" + String.join("§7, §e", words));
                                        }
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("clear")
                                    .executes(context -> {
                                        ChatFilterManager.clearWords();
                                        sendMessage("§a" + LocalizationManager.get("gui.resistancedlc.command.chatfilter.cleared"));
                                        return 1;
                                    }))
            );

            dispatcher.register(
                    ClientCommandManager.literal("dc")
                            .executes(context -> { DeathCoordsManager.showLastDeath(); return 1; })
                            .then(ClientCommandManager.literal("last")
                                    .executes(context -> { DeathCoordsManager.showLastDeath(); return 1; }))
                            .then(ClientCommandManager.literal("clear")
                                    .executes(context -> { DeathCoordsManager.clearLastDeath(); return 1; }))
                            .then(ClientCommandManager.literal("toggle")
                                    .executes(context -> {
                                        ModConfig.deathCoordsEnabled = !ModConfig.deathCoordsEnabled;
                                        ConfigManager.save();
                                        String state = ModConfig.deathCoordsEnabled
                                                ? LocalizationManager.get("gui.resistancedlc.command.dc.on")
                                                : LocalizationManager.get("gui.resistancedlc.command.dc.off");
                                        sendMessage("§6DeathCoords " + state);
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("help")
                                    .executes(context -> { DeathCoordsManager.showHelp(); return 1; }))
            );

            dispatcher.register(
                    ClientCommandManager.literal("cooldowns")
                            .executes(context -> {
                                ModConfig.cooldownsEnabled = !ModConfig.cooldownsEnabled;
                                ConfigManager.save();
                                String state = ModConfig.cooldownsEnabled
                                        ? LocalizationManager.get("gui.resistancedlc.command.dc.on")
                                        : LocalizationManager.get("gui.resistancedlc.command.dc.off");
                                sendMessage("§6" + LocalizationManager.get("gui.resistancedlc.command.cooldowns.toggle") + state);
                                return 1;
                            })
            );

            dispatcher.register(
                    ClientCommandManager.literal("music")
                            .executes(context -> {
                                ModConfig.musicPlayerEnabled = !ModConfig.musicPlayerEnabled;
                                ConfigManager.save();
                                String state = ModConfig.musicPlayerEnabled ? "§aвключён" : "§cвыключен";
                                sendMessage("§6MusicPlayer " + state);
                                return 1;
                            })
                            .then(ClientCommandManager.literal("play")
                                    .executes(context -> { MusicPlayerManager.play(); return 1; }))
                            .then(ClientCommandManager.literal("pause")
                                    .executes(context -> { MusicPlayerManager.pause(); return 1; }))
                            .then(ClientCommandManager.literal("stop")
                                    .executes(context -> { MusicPlayerManager.stop(); return 1; }))
                            .then(ClientCommandManager.literal("next")
                                    .executes(context -> { MusicPlayerManager.next(); return 1; }))
                            .then(ClientCommandManager.literal("prev")
                                    .executes(context -> { MusicPlayerManager.prev(); return 1; }))
                            .then(ClientCommandManager.literal("rescan")
                                    .executes(context -> {
                                        MusicPlayerManager.rescan();
                                        sendMessage("§a[Music] Плейлист обновлён: §e"
                                                + MusicPlayerManager.getPlaylist().size() + " §aтреков");
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("folder")
                                    .executes(context -> {
                                        MusicPlayerManager.openMusicFolder();
                                        sendMessage("§a[Music] Папка открыта: §e"
                                                + MusicPlayerManager.getMusicDirPath());
                                        return 1;
                                    }))
                            .then(ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        List<MusicTrack> tracks = MusicPlayerManager.getPlaylist();
                                        if (tracks.isEmpty()) {
                                            sendMessage("§7[Music] Плейлист пуст.");
                                        } else {
                                            sendMessage("§6[Music] Треки (" + tracks.size() + "):");
                                            for (int i = 0; i < tracks.size(); i++) {
                                                sendMessage("§7 " + (i + 1) + ". §e" + tracks.get(i).displayFull());
                                            }
                                        }
                                        return 1;
                                    }))
            );
        });

        // ===== CHAT FILTER =====
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            if (!ModConfig.chatFilterEnabled) return true;
            return !ChatFilterManager.shouldHide(message.getString());
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!ModConfig.chatFilterEnabled) return true;
            return !ChatFilterManager.shouldHide(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            PvPSafeManager.onChatMessage(message.getString());
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            PvPSafeManager.onChatMessage(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            AutoTPAcceptManager.onChatMessage(message.getString());
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            AutoTPAcceptManager.onChatMessage(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            StatsTrackerManager.onChatMessage(message.getString());
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            StatsTrackerManager.onChatMessage(message.getString());
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

        // ===== DEATH COORDS =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                lastHealth = -1.0f;
                return;
            }
            float currentHealth = client.player.getHealth();
            if (lastHealth < 0) { lastHealth = currentHealth; return; }
            if (currentHealth <= 0.0f && lastHealth > 0.0f) {
                DeathCoordsManager.onPlayerDeath(client.player);
                lastHealth = currentHealth;
                return;
            }
            lastHealth = currentHealth;
        });

        // ===== DEATH RECAP — СНИМОК БРОНИ КАЖДЫЙ ТИК =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (client.player.getHealth() <= 0.0f) return;   // не перезаписываем снимок после смерти

            String head  = itemSnapshotOf(client.player.getItemBySlot(EquipmentSlot.HEAD));
            String chest = itemSnapshotOf(client.player.getItemBySlot(EquipmentSlot.CHEST));
            String legs  = itemSnapshotOf(client.player.getItemBySlot(EquipmentSlot.LEGS));
            String feet  = itemSnapshotOf(client.player.getItemBySlot(EquipmentSlot.FEET));
            String off   = itemSnapshotOf(client.player.getItemBySlot(EquipmentSlot.OFFHAND));

            DeathRecapManager.updateArmorSnapshot(
                    new DeathRecapManager.ArmorSnapshot(head, chest, legs, feet, off));
        });

        // ===== DEATH RECAP — ДЕТЕКТ СМЕРТИ ЧЕРЕЗ DeathScreen =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                deathRecapCaptured = false;
                return;
            }
            boolean onDeathScreen = client.screen instanceof net.minecraft.client.gui.screens.DeathScreen;
            if (!onDeathScreen) { deathRecapCaptured = false; return; }
            if (deathRecapCaptured) return;
            deathRecapCaptured = true;

            try {
                net.minecraft.client.gui.screens.DeathScreen ds =
                        (net.minecraft.client.gui.screens.DeathScreen) client.screen;

                Component deathMsg = ((com.resistancedlc.mixin.DeathScreenAccessor) ds).getCauseOfDeath();
                String reason = deathMsg != null ? deathMsg.getString() : "";

                String killer = "";
                net.minecraft.world.damagesource.DamageSource lastSource =
                        client.player.getLastDamageSource();
                if (lastSource != null) {
                    net.minecraft.world.entity.Entity attacker = lastSource.getEntity();
                    if (attacker != null) killer = attacker.getName().getString();
                    else {
                        net.minecraft.world.entity.Entity direct = lastSource.getDirectEntity();
                        if (direct != null) killer = direct.getName().getString();
                    }
                }

                int x = (int) client.player.getX();
                int y = (int) client.player.getY();
                int z = (int) client.player.getZ();

                String dim = client.player.level().dimension().identifier().getPath();
                dim = switch (dim) {
                    case "overworld" -> "Overworld";
                    case "the_nether" -> "Nether";
                    case "the_end" -> "End";
                    default -> dim;
                };

                long ts = System.currentTimeMillis();

                DeathRecapManager.ArmorSnapshot snap = DeathRecapManager.getLastArmorSnapshot();
                String head  = snap != null && snap.head  != null ? snap.head  : "";
                String chest = snap != null && snap.chest != null ? snap.chest : "";
                String legs  = snap != null && snap.legs  != null ? snap.legs  : "";
                String feet  = snap != null && snap.feet  != null ? snap.feet  : "";
                String off   = snap != null && snap.offhand != null ? snap.offhand : "";

                DeathRecapEntry entry = new DeathRecapEntry(
                        reason, killer, x, y, z, dim, ts,
                        head, chest, legs, feet, off);

                DeathRecapManager.addDeath(entry);
            } catch (Throwable t) {
                ResistanceDLC.LOGGER.error("[DeathRecap] Failed: " + t.getMessage());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> PickUpLogger.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> AutoReconnectManager.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> AutoGGManager.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> MusicPlayerManager.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> LowHPAlertManager.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> AutoRespawnManager.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ArmorAlertManager.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> AutoTPAcceptManager.tick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> KillStreakManager.tick());

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

        // ===== CROSSHAIR HEATMAP — детект промаха =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.crosshairHeatmapEnabled) {
                crosshairHeatmapAttackWasDown = false;
                return;
            }
            if (client.player == null || client.level == null) {
                crosshairHeatmapAttackWasDown = false;
                return;
            }
            if (client.screen != null) {
                crosshairHeatmapAttackWasDown = client.options.keyAttack.isDown();
                return;
            }

            boolean isDown = client.options.keyAttack.isDown();
            boolean justPressed = isDown && !crosshairHeatmapAttackWasDown;
            crosshairHeatmapAttackWasDown = isDown;

            if (justPressed && client.crosshairPickEntity == null) {
                Vec3 eyePos = client.player.getEyePosition();
                Vec3 look = client.player.getLookAngle();
                Vec3 missPoint = eyePos.add(look.scale(3.1));
                CrosshairHeatmapManager.recordMiss(missPoint);
            }
        });

        // ===== TAPEMOUSE toggle =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.tapeMouseKey == null) return;
            while (KeyBindings.tapeMouseKey.consumeClick()) {
                ModConfig.tapeMouseEnabled = !ModConfig.tapeMouseEnabled;
                ConfigManager.save();
                if (client.player != null) {
                    String state = ModConfig.tapeMouseEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
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
                    String state = ModConfig.customHitSoundsEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
                    client.player.displayClientMessage(
                            Component.literal("§6Custom Hit Sounds " + state), true);
                }
            }

            if (KeyBindings.fastExpKey != null) {
                while (KeyBindings.fastExpKey.consumeClick()) {
                    ModConfig.fastExpEnabled = !ModConfig.fastExpEnabled;
                    ConfigManager.save();
                    String state = ModConfig.fastExpEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
                    client.player.displayClientMessage(
                            Component.literal("§6FastExp " + state), true);
                }
            }

            if (KeyBindings.shiftTapKey != null) {
                while (KeyBindings.shiftTapKey.consumeClick()) {
                    ModConfig.shiftTapEnabled = !ModConfig.shiftTapEnabled;
                    ConfigManager.save();
                    String state = ModConfig.shiftTapEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
                    client.player.displayClientMessage(
                            Component.literal("§6ShiftTap " + state), true);
                }
            }

            if (KeyBindings.comboKey != null) {
                while (KeyBindings.comboKey.consumeClick()) {
                    ModConfig.comboEnabled = !ModConfig.comboEnabled;
                    ConfigManager.save();
                    String state = ModConfig.comboEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
                    client.player.displayClientMessage(
                            Component.literal("§6Combo Counter " + state), true);
                }
            }

            if (KeyBindings.effectWarningsKey != null) {
                while (KeyBindings.effectWarningsKey.consumeClick()) {
                    ModConfig.effectWarningsEnabled = !ModConfig.effectWarningsEnabled;
                    ConfigManager.save();
                    String state = ModConfig.effectWarningsEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
                    client.player.displayClientMessage(
                            Component.literal("§6Effect Warnings " + state), true);
                }
            }

            if (KeyBindings.totemLogKey != null) {
                while (KeyBindings.totemLogKey.consumeClick()) {
                    ModConfig.totemLogEnabled = !ModConfig.totemLogEnabled;
                    ConfigManager.save();
                    String state = ModConfig.totemLogEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
                    client.player.displayClientMessage(
                            Component.literal("§6Totem Log " + state), true);
                }
            }

            if (KeyBindings.pickupLogKey != null) {
                while (KeyBindings.pickupLogKey.consumeClick()) {
                    ModConfig.pickupLogEnabled = !ModConfig.pickupLogEnabled;
                    ConfigManager.save();
                    String state = ModConfig.pickupLogEnabled
                            ? LocalizationManager.get("gui.resistancedlc.message.toggle_on")
                            : LocalizationManager.get("gui.resistancedlc.message.toggle_off");
                    client.player.displayClientMessage(
                            Component.literal("§6PickUpLogger " + state), true);
                }
            }
        });

        // ===== AUTOSWAP =====
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

        // ===== WAYPOINTS keybind =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.waypointsKey == null) return;
            while (KeyBindings.waypointsKey.consumeClick()) {
                if (client.player == null) return;
                double x = client.player.getX();
                double y = client.player.getY();
                double z = client.player.getZ();
                boolean added = WaypointManager.addWaypoint(x, y, z);
                if (added) {
                    int newIndex = WaypointManager.getWaypoints().size();
                    client.player.displayClientMessage(
                            Component.literal("§a" + String.format(
                                    LocalizationManager.get("gui.resistancedlc.waypoints.added_msg"),
                                    LocalizationManager.get("gui.resistancedlc.waypoints.wp_prefix") + newIndex,
                                    (int) x, (int) y, (int) z)), true);
                } else {
                    client.player.displayClientMessage(
                            Component.literal("§c" + LocalizationManager.get(
                                    "gui.resistancedlc.message.waypoints_limit", ModConfig.waypointsMax)), true);
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

        // ===== COMBO reset =====
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!ModConfig.comboEnabled) return;
            if (ModConfig.currentCombo <= 0) return;
            long comboNow = System.currentTimeMillis();
            if (comboNow - ModConfig.lastComboTime > (long) ModConfig.comboResetTime * 1000L) {
                ModConfig.currentCombo = 0;
            }
        });

        // ===== TAPEMOUSE logic =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ModConfig.tapeMouseEnabled) return;
            if (client.player == null || client.level == null) return;
            if (client.screen != null) return;

            if (ModConfig.tapeMouseButton == 1) {
                if (ModConfig.tapeMouseHoldRight) return;
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

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "music_hud"),
                (graphics, tickCounter) -> MusicPlayerHud.render(graphics)
        );
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "friend_list_hud"),
                (graphics, tickCounter) -> FriendListHud.render(graphics)
        );
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "crosshair_heatmap_hud"),
                (graphics, tickCounter) -> CrosshairHeatmapHud.render(graphics)
        );

        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_EXTRACTION.register(TARGET_ESP::extract);
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_MAIN.register(TARGET_ESP::draw);

        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_MAIN.register(
                PredictionsRenderer::render
        );

        ClientTickEvents.END_CLIENT_TICK.register(mc -> JumpCirclesManager.tick());

        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.END_MAIN.register(
                JumpCirclesManager::render
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> PingIndicatorManager.tick());

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level != null) {
                com.resistancedlc.targetesp.TargetManagerHolder.MANAGER.tick(mc.level);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (KeyBindings.macro1Key.consumeClick()) MacroManager.executeMacro(0);
            while (KeyBindings.macro2Key.consumeClick()) MacroManager.executeMacro(1);
            while (KeyBindings.macro3Key.consumeClick()) MacroManager.executeMacro(2);
            while (KeyBindings.macro4Key.consumeClick()) MacroManager.executeMacro(3);
            while (KeyBindings.macro5Key.consumeClick()) MacroManager.executeMacro(4);
        });
    }

    // ===== Хелпер: ID предмета =====
    /**
     * Сериализация ItemStack для DeathRecap: "itemId|displayName".
     * Формат: "minecraft:diamond_helmet|Diamond Helmet"
     * Или:    "minecraft:diamond_helmet|Моё имя"
     */
    private static String itemSnapshotOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        try {
            net.minecraft.resources.Identifier id =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) return "";

            String customName = "";
            if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                customName = stack.getHoverName().getString();
                // ⚠ Заменяем разделители ТОЛЬКО в customName, не в id!
                customName = customName.replace("|", "/").replace("~~", "_");
            }
            return id + "|" + customName;
        } catch (Exception e) {
            return "";
        }
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
            Identifier logoId = Identifier.fromNamespaceAndPath("resistancedlc", "textures/gui/icon.png");
            graphics.blit(RenderPipelines.GUI_TEXTURED, logoId, logoX, logoY, 0, 0, 16, 16, 16, 16);
            drawHudString(graphics, client.font, "ResistanceDLC", logoX + 20, logoY + 4, color);
        }

        if (ModConfig.comboEnabled && ModConfig.currentCombo > 0) {
            String comboText = LocalizationManager.get("gui.resistancedlc.hud.combo", ModConfig.currentCombo);
            float scale;
            if (ModConfig.comboFontSize == 0) scale = 1.0f;
            else if (ModConfig.comboFontSize == 1) scale = 1.5f;
            else scale = 2.0f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(ModConfig.comboX, ModConfig.comboY);
            graphics.pose().scale(scale, scale);
            graphics.drawString(client.font, comboText, 0, 0, ModConfig.comboColor, true);
            graphics.pose().popMatrix();
        }

        if (ModConfig.pvpSafeEnabled && ModConfig.pvpSafeShowHud && PvPSafeManager.isInCombat()) {
            int remaining = PvPSafeManager.getRemainingSeconds();
            String text = "§c" + LocalizationManager.get("gui.resistancedlc.hud.pvp_safe", remaining);
            graphics.drawString(client.font, text, ModConfig.pvpSafeHudX, ModConfig.pvpSafeHudY,
                    ModConfig.pvpSafeHudColor, true);
        }

        if (ModConfig.showCoords) {
            drawHudString(graphics, client.font,
                    LocalizationManager.get("gui.resistancedlc.hud.xyz",
                            (int) client.player.getX(), (int) client.player.getY(), (int) client.player.getZ()),
                    ModConfig.coordsX, ModConfig.coordsY, color);
        }

        if (ModConfig.showBiome) {
            String biome = client.level.getBiome(client.player.blockPosition())
                    .unwrapKey().map(key -> key.identifier().getPath()).orElse("unknown");
            drawHudString(graphics, client.font,
                    LocalizationManager.get("gui.resistancedlc.hud.biome", biome),
                    ModConfig.biomeX, ModConfig.biomeY, color);
        }

        if (ModConfig.showTime) {
            long time = client.level.getDayTime() % 24000;
            String timeStr = time < 12000
                    ? LocalizationManager.get("gui.resistancedlc.hud.day")
                    : LocalizationManager.get("gui.resistancedlc.hud.night");
            drawHudString(graphics, client.font,
                    LocalizationManager.get("gui.resistancedlc.hud.time", timeStr),
                    ModConfig.timeX, ModConfig.timeY, color);
        }

        if (ModConfig.showFps) {
            drawHudString(graphics, client.font,
                    LocalizationManager.get("gui.resistancedlc.hud.fps") + ": " + client.getFps(),
                    ModConfig.fpsX, ModConfig.fpsY, color);
        }

        if (ModConfig.showPing) {
            int ping = 0;
            if (client.getConnection() != null
                    && client.getConnection().getPlayerInfo(client.player.getUUID()) != null) {
                ping = client.getConnection().getPlayerInfo(client.player.getUUID()).getLatency();
            }
            drawHudString(graphics, client.font,
                    LocalizationManager.get("gui.resistancedlc.hud.ping") + ": " + ping + " ms",
                    ModConfig.pingX, ModConfig.pingY, color);
        }

        if (ModConfig.showTps) {
            float tps = 20.0f;
            if (client.getSingleplayerServer() != null) {
                long tickTime = client.getSingleplayerServer().getAverageTickTimeNanos();
                if (tickTime > 0) tps = Math.min(20.0f, 1_000_000_000.0f / tickTime);
            }
            drawHudString(graphics, client.font,
                    String.format("%s: %.1f", LocalizationManager.get("gui.resistancedlc.hud.tps"), tps),
                    ModConfig.tpsX, ModConfig.tpsY, color);
        }

        if (ModConfig.showBps) {
            drawHudString(graphics, client.font,
                    String.format("%s: %.2f", LocalizationManager.get("gui.resistancedlc.hud.bps"), ModConfig.currentBps),
                    ModConfig.bpsX, ModConfig.bpsY, color);
        }

        if (ModConfig.showDirection) {
            float yaw = client.player.getYRot();
            yaw = ((yaw % 360) + 360) % 360;
            String dir;
            if (yaw >= 315 || yaw < 45) dir = LocalizationManager.get("gui.resistancedlc.hud.direction_south");
            else if (yaw >= 45 && yaw < 135) dir = LocalizationManager.get("gui.resistancedlc.hud.direction_west");
            else if (yaw >= 135 && yaw < 225) dir = LocalizationManager.get("gui.resistancedlc.hud.direction_north");
            else dir = LocalizationManager.get("gui.resistancedlc.hud.direction_east");
            drawHudString(graphics, client.font,
                    LocalizationManager.get("gui.resistancedlc.hud.direction") + ": " + dir,
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
                drawHudString(graphics, client.font,
                        LocalizationManager.get("gui.resistancedlc.hud.hits") + ": " + hitsLeft,
                        ModConfig.hitCounterX, ModConfig.hitCounterY, hpColor);
            }
        }

        if (AutoReconnectManager.isReconnecting() && ModConfig.autoReconnectShowHud) {
            int remaining = AutoReconnectManager.getRemainingSeconds();
            String text = "§c" + LocalizationManager.get("gui.resistancedlc.hud.auto_reconnect", remaining);
            graphics.drawString(client.font, text,
                    client.getWindow().getGuiScaledWidth() / 2 - client.font.width(text) / 2,
                    client.getWindow().getGuiScaledHeight() / 2 + 30, 0xFFFFFFFF, true);
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
                        Identifier iconId = Identifier.fromNamespaceAndPath(effectId.getNamespace(),
                                "textures/mob_effect/" + effectId.getPath() + ".png");
                        graphics.blit(RenderPipelines.GUI_TEXTURED, iconId, iconX, iconY, 0, 0, 18, 18, 18, 18);
                        String name = type.getDisplayName().getString();
                        int amp = effect.getAmplifier();
                        if (amp > 0) name = name + " " + toRoman(amp + 1);
                        int duration = effect.getDuration();
                        int totalSeconds = duration / 20;
                        String timeStr = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
                        drawHudString(graphics, client.font, name + " " + timeStr, iconX + 21, iconY + 5, color);
                        yOffset += 20;
                    }
                } else {
                    drawHudString(graphics, client.font,
                            LocalizationManager.get("gui.resistancedlc.hud.effects"),
                            ModConfig.potionEffectsX, ModConfig.potionEffectsY, color);
                    int textOffset = 12;
                    for (MobEffectInstance effect : effects) {
                        MobEffect type = effect.getEffect().value();
                        String name = type.getDisplayName().getString();
                        int duration = effect.getDuration();
                        int totalSeconds = duration / 20;
                        String timeStr = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
                        drawHudString(graphics, client.font, " " + name + " " + timeStr,
                                ModConfig.potionEffectsX, ModConfig.potionEffectsY + textOffset, color);
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
                if (durationSec > ModConfig.effectWarningsThreshold * 2 / 3) timeColor = 0xFF00FF00;
                else if (durationSec > ModConfig.effectWarningsThreshold / 3) timeColor = 0xFFFFFF00;
                else timeColor = 0xFFFF0000;
                int finalColor = (timeColor & 0x00FFFFFF) | (ModConfig.effectWarningsAlpha << 24);
                int alphaColor = (ModConfig.effectWarningsColor & 0x00FFFFFF) | (ModConfig.effectWarningsAlpha << 24);
                int x = ModConfig.effectWarningsX;
                int y = ModConfig.effectWarningsY + warnY;
                if (ModConfig.effectWarningsShowIcon) {
                    Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(type);
                    if (effectId != null) {
                        Identifier iconId = Identifier.fromNamespaceAndPath(effectId.getNamespace(),
                                "textures/mob_effect/" + effectId.getPath() + ".png");
                        graphics.blit(RenderPipelines.GUI_TEXTURED, iconId, x, y, 0, 0, 18, 18, 18, 18);
                        x += 21;
                    }
                }
                if (ModConfig.effectWarningsShowName) {
                    String name = type.getDisplayName().getString();
                    graphics.drawString(client.font, name, x, y + 5, alphaColor, true);
                    x += client.font.width(name) + 4;
                }
                graphics.drawString(client.font, durationSec + "s", x, y + 5, finalColor, true);
                warnY += 20;
            }
        }

        if (ModConfig.cooldownsEnabled) {
            java.util.List<CooldownsManager.CooldownEntry> cooldowns = CooldownsManager.getActiveCooldowns();
            if (!cooldowns.isEmpty()) {
                int cdColor = (ModConfig.cooldownsColor & 0x00FFFFFF) | (ModConfig.cooldownsAlpha << 24);
                int cdX = ModConfig.cooldownsX;
                int cdY = ModConfig.cooldownsY;
                int rowHeight = 20;
                for (CooldownsManager.CooldownEntry entry : cooldowns) {
                    int curX = cdX;
                    int curY = cdY;
                    if (ModConfig.cooldownsShowIcon) {
                        graphics.renderItem(entry.stack, curX, curY);
                        int overlayAlpha = (int) (entry.percent * ModConfig.cooldownsIconDarkening);
                        if (overlayAlpha > 0) graphics.fill(curX, curY, curX + 16, curY + 16, (overlayAlpha << 24));
                        curX += 20;
                    }
                    if (ModConfig.cooldownsShowName) {
                        String name = entry.stack.getHoverName().getString();
                        graphics.drawString(client.font, name, curX, curY + 1, cdColor, true);
                        curX += client.font.width(name) + 6;
                    }
                    if (ModConfig.cooldownsShowTime) {
                        String timeStr = CooldownsManager.formatTime(entry.remainingSeconds);
                        graphics.drawString(client.font, timeStr, curX, curY + 1, cdColor, true);
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

        if (ModConfig.lowHpAlertEnabled && LowHPAlertManager.isAlertActive()) {
            int guiW = client.getWindow().getGuiScaledWidth();
            int guiH = client.getWindow().getGuiScaledHeight();

            float health = client.player.getHealth();
            String text = String.format(LocalizationManager.get("gui.resistancedlc.hud.low_hp_alert"),
                    (int) Math.ceil(health));

            int textW = client.font.width(text);
            int lhaX = (ModConfig.lowHpAlertX < 0)
                    ? (guiW - textW) / 2
                    : ModConfig.lowHpAlertX;
            int lhaY = (ModConfig.lowHpAlertY < 0)
                    ? (guiH / 2 - 40)
                    : ModConfig.lowHpAlertY;

            int lhaAlpha = ModConfig.lowHpAlertAlpha;
            if (ModConfig.lowHpAlertBlink) {
                long t = System.currentTimeMillis() % 800L;
                float pulse = (float) (0.5 + 0.5 * Math.sin(t / 800.0 * Math.PI * 2));
                lhaAlpha = (int) (ModConfig.lowHpAlertAlpha * (0.4f + 0.6f * pulse));
            }

            int lhaColor = (lhaAlpha << 24) | (ModConfig.lowHpAlertColor & 0x00FFFFFF);

            float lhaScale = 2.0f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(lhaX, lhaY);
            graphics.pose().scale(lhaScale, lhaScale);
            graphics.drawString(client.font, text, 0, 0, lhaColor, true);
            graphics.pose().popMatrix();
        }

        if (ModConfig.armorAlertEnabled && ArmorAlertManager.isAlertActive()) {
            int guiW = client.getWindow().getGuiScaledWidth();
            int guiH = client.getWindow().getGuiScaledHeight();

            int pct = ArmorAlertManager.getLowestPercent();
            String text = String.format(LocalizationManager.get("gui.resistancedlc.hud.armor_alert"), pct);

            int textW = client.font.width(text);
            int aaX = (ModConfig.armorAlertX < 0)
                    ? (guiW - textW) / 2
                    : ModConfig.armorAlertX;
            int aaY = (ModConfig.armorAlertY < 0)
                    ? (guiH / 2 + 20)
                    : ModConfig.armorAlertY;

            int aaColor = (ModConfig.armorAlertAlpha << 24) | (ModConfig.armorAlertColor & 0x00FFFFFF);
            graphics.drawString(client.font, text, aaX, aaY, aaColor, true);
        }

        if (ModConfig.killStreakEnabled && KillStreakManager.isActive()) {
            int guiW = client.getWindow().getGuiScaledWidth();

            int streak = KillStreakManager.getCurrentStreak();
            String text;
            if (ModConfig.killStreakShowTimer) {
                int sec = KillStreakManager.getRemainingSeconds();
                text = LocalizationManager.get("gui.resistancedlc.hud.kill_streak_timer", streak, sec);
            } else {
                text = LocalizationManager.get("gui.resistancedlc.hud.kill_streak", streak);
            }

            float scale;
            if (ModConfig.killStreakFontSize == 0) scale = 1.0f;
            else if (ModConfig.killStreakFontSize == 1) scale = 1.5f;
            else scale = 2.0f;

            int textW = (int)(client.font.width(text) * scale);
            int ksX = (ModConfig.killStreakHudX < 0)
                    ? (guiW - textW) / 2
                    : ModConfig.killStreakHudX;
            int ksY = ModConfig.killStreakHudY;

            int ksColor = (ModConfig.killStreakAlpha << 24) | (ModConfig.killStreakColor & 0x00FFFFFF);

            graphics.pose().pushMatrix();
            graphics.pose().translate(ksX, ksY);
            graphics.pose().scale(scale, scale);
            graphics.drawString(client.font, text, 0, 0, ksColor, true);
            graphics.pose().popMatrix();
        }

        if (ModConfig.strikeRangeEnabled && StrikeRangeManager.isActive()) {
            double dist = StrikeRangeManager.getDistance();
            String targetName = ModConfig.strikeRangeLastTarget;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%.2f", dist));
            if (ModConfig.strikeRangeShowBlocks) {
                sb.append(" ").append(LocalizationManager.get("gui.resistancedlc.hud.strike_range_blocks"));
            }
            if (ModConfig.strikeRangeShowTarget && targetName != null && !targetName.isEmpty()) {
                sb.append(" §7(").append(targetName).append(")");
            }

            float fade = StrikeRangeManager.getFadeProgress();
            int srAlpha = (int)(ModConfig.strikeRangeAlpha * fade);
            int srColor = (ModConfig.strikeRangeColor & 0x00FFFFFF) | (srAlpha << 24);

            float scale;
            if (ModConfig.strikeRangeFontSize == 0) scale = 1.0f;
            else if (ModConfig.strikeRangeFontSize == 1) scale = 1.5f;
            else scale = 2.0f;

            graphics.pose().pushMatrix();
            graphics.pose().translate(ModConfig.strikeRangeX, ModConfig.strikeRangeY);
            graphics.pose().scale(scale, scale);
            graphics.drawString(client.font, sb.toString(), 0, 0, srColor, true);
            graphics.pose().popMatrix();
        }
    }

    private static void showCfgHelp() {
        sendMessage("§6§l" + LocalizationManager.get("gui.resistancedlc.command.cfg.title"));
        sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.cfg.help"));
        sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.cfg.dir"));
        sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.cfg.list"));
        sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.cfg.save"));
        sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.cfg.load"));
        sendMessage("§e" + LocalizationManager.get("gui.resistancedlc.command.cfg.remove"));
        sendMessage("§7");
        sendMessage("§7" + LocalizationManager.get("gui.resistancedlc.command.cfg.about"));
        sendMessage("§7" + LocalizationManager.get("gui.resistancedlc.command.cfg.path"));
        sendMessage("§7" + LocalizationManager.get("gui.resistancedlc.command.cfg.gui"));
        sendMessage("§6§l════════════════════════════════════");
    }

    private static void renderWaypoints(GuiGraphics graphics) {
        if (!ModConfig.waypointsEnabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (client.options.hideGui) return;

        List<Waypoint> waypoints = WaypointManager.getWaypoints();
        if (waypoints.isEmpty()) return;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();
        TrackedWaypoint.Projector projector = client.gameRenderer;
        int EDGE_MARGIN = 20;

        for (Waypoint wp : waypoints) {
            double dist = wp.distanceTo(px, py, pz);
            int wpColor;
            if (dist < 50.0) wpColor = 0xFF00FF00;
            else if (dist < 200.0) wpColor = 0xFFFFFF00;
            else wpColor = 0xFFFF0000;

            Vec3 ndc;
            try {
                ndc = projector.projectPointToScreen(new Vec3(wp.x(), wp.y(), wp.z()));
            } catch (Exception e) { continue; }
            if (ndc == null) continue;

            boolean behind = ndc.z > 1.0;
            double ndcX = ndc.x;
            double ndcY = behind ? -ndc.y : ndc.y;
            int screenX = (int) ((ndcX + 1.0) * 0.5 * screenW);
            int screenY = (int) ((1.0 - ndcY) * 0.5 * screenH);
            boolean onScreen = !behind && ndcX >= -1.0 && ndcX <= 1.0 && ndcY >= -1.0 && ndcY <= 1.0;

            if (onScreen) {
                drawWaypointDiamond(graphics, screenX, screenY, 5, wpColor);
                String label = wp.name();
                String distStr = String.format("%.0fm", dist);
                int labelW = client.font.width(label);
                int distW = client.font.width(distStr);
                graphics.drawString(client.font, label, screenX - labelW / 2, screenY - 16, wpColor, true);
                graphics.drawString(client.font, distStr, screenX - distW / 2, screenY + 8, wpColor, true);
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

    private static void drawWaypointArrow(GuiGraphics graphics, int cx, int cy, double dirX, int color) {
        int arrowSize = 8;
        int baseWidth = 6;
        int tipX = cx + (int) Math.round(dirX * arrowSize);
        int baseX = cx - (int) Math.round(dirX * arrowSize);
        int startX = Math.min(tipX, baseX);
        int endX = Math.max(tipX, baseX);
        for (int x = startX; x <= endX; x++) {
            double t;
            if (dirX > 0) t = (double) (x - baseX) / (tipX - baseX);
            else t = (double) (baseX - x) / (baseX - tipX);
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
                case 0 -> { int y = tipY + i; graphics.fill(tipX - width / 2, y, tipX - width / 2 + width, y + 1, color); }
                case 1 -> { int y = tipY - i; graphics.fill(tipX - width / 2, y, tipX - width / 2 + width, y + 1, color); }
                case 2 -> { int x = tipX + i; graphics.fill(x, tipY - width / 2, x + 1, tipY - width / 2 + width, color); }
                case 3 -> { int x = tipX - i; graphics.fill(x, tipY - width / 2, x + 1, tipY - width / 2 + width, color); }
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
        boolean offhandMatches = (offhandAllowedHead && offhandIsHead) || (offhandAllowedTotem && offhandIsTotem);
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
        client.gameMode.handleInventoryMouseClick(0, slotIndex, 40, ClickType.SWAP, client.player);
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

    private static void recordHeatmapHit(Entity target) {
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        double dist = mc.player.distanceTo(target) + 1.0;
        Vec3 rayEnd = eye.add(look.scale(dist));

        AABB box = target.getBoundingBox();
        java.util.Optional<Vec3> hit = box.clip(eye, rayEnd);

        Vec3 pos = hit.orElseGet(() -> new Vec3(
                target.getX(),
                target.getY() + target.getBbHeight() / 2.0,
                target.getZ()
        ));

        CrosshairHeatmapManager.recordHit(pos);
    }
}