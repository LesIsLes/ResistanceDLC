package com.resistancedlc;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ResistanceDLCClient implements ClientModInitializer {

    private static long lastAttackTime = 0;
    private static boolean tapeMouseWeHeldRMB = false;

    @Override
    public void onInitializeClient() {
        // ===== ДЕБАУНС СОХРАНЕНИЯ КОНФИГА =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> ConfigManager.tick());

        // ===== ЗАГРУЗКА КОНФИГА + КЛАВИШИ =====
        ConfigManager.load();
        KeyBindings.register();

        // ===== СОХРАНЕНИЕ ПРИ ВЫХОДЕ ИЗ МИРА =====
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ConfigManager.saveNow());

        // ===== КОМАНДЫ =====
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommandManager.literal("resistancedlc")
                            .then(ClientCommandManager.literal("gui")
                                    .executes(context -> {
                                        Minecraft.getInstance().setScreen(new MyCustomScreen());
                                        return 1;
                                    })
                            )
            );

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

        // ===== BPS =====
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

        // ===== ZOOM =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean keyDown = KeyBindings.zoomKey != null && KeyBindings.zoomKey.isDown();
            float targetZoom = 1.0f;
            if (MyCustomScreen.zoomEnabled && keyDown) targetZoom = 1.0f / MyCustomScreen.zoomFactor;
            float smooth = MyCustomScreen.zoomSmoothness;
            if (Math.abs(MyCustomScreen.currentZoom - targetZoom) < 0.001f) {
                MyCustomScreen.currentZoom = targetZoom;
            } else {
                MyCustomScreen.currentZoom += (targetZoom - MyCustomScreen.currentZoom) * smooth;
            }
        });

        // ===== TAPEMOUSE: toggle клавишей =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.tapeMouseKey == null) return;
            while (KeyBindings.tapeMouseKey.consumeClick()) {
                MyCustomScreen.tapeMouseEnabled = !MyCustomScreen.tapeMouseEnabled;
                ConfigManager.save();
                if (client.player != null) {
                    String state = MyCustomScreen.tapeMouseEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(Component.literal("§6TapeMouse " + state), true);
                }
            }
        });

        // ===== AUTOSWAP: нажатие клавиши =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.autoSwapKey == null) return;
            while (KeyBindings.autoSwapKey.consumeClick()) {
                if (!MyCustomScreen.autoSwapEnabled) return;
                if (client.player == null || client.level == null) return;
                if (client.screen != null) return;
                if (MyCustomScreen.autoSwapInProgress) return;
                long now = System.currentTimeMillis();
                if (now - MyCustomScreen.autoSwapLastTime < MyCustomScreen.autoSwapCooldown) return;
                int slotToSwap = findAutoSwapSlot(client.player);
                if (slotToSwap < 0) return;
                MyCustomScreen.autoSwapInProgress = true;
                MyCustomScreen.autoSwapStage = 0;
                MyCustomScreen.autoSwapSlotToSwap = slotToSwap;
                MyCustomScreen.autoSwapNextActionTime = now;
            }
        });

        // ===== AUTOSWAP: этапы =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!MyCustomScreen.autoSwapInProgress) return;
            if (client.player == null) return;
            long now = System.currentTimeMillis();
            if (now < MyCustomScreen.autoSwapNextActionTime) return;
            if (MyCustomScreen.autoSwapStage == 0) {
                client.setScreen(new InventoryScreen(client.player));
                MyCustomScreen.autoSwapStage = 1;
                MyCustomScreen.autoSwapNextActionTime = now + 50;
            } else if (MyCustomScreen.autoSwapStage == 1) {
                int slot = MyCustomScreen.autoSwapSlotToSwap;
                if (slot >= 0 && slot < client.player.getInventory().getContainerSize()) {
                    swapOffhandWithSlot(client.player, slot);
                }
                MyCustomScreen.autoSwapStage = 2;
                MyCustomScreen.autoSwapNextActionTime = now + MyCustomScreen.autoSwapOpenDelay;
            } else if (MyCustomScreen.autoSwapStage == 2) {
                client.setScreen(null);
                MyCustomScreen.autoSwapInProgress = false;
                MyCustomScreen.autoSwapStage = 0;
                MyCustomScreen.autoSwapSlotToSwap = -1;
                MyCustomScreen.autoSwapLastTime = now;
            }
        });

        // ===== FASTEXP =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!MyCustomScreen.fastExpEnabled) return;
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
            if (!MyCustomScreen.autoSprintEnabled) return;
            if (client.player == null) return;
            if (client.screen != null) return;
            if (client.options.keyUp.isDown()) {
                client.player.setSprinting(true);
            }
        });

        // ===== SHIFTTAP =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!MyCustomScreen.shiftTapEnabled) return;
            if (client.player == null) return;
            if (client.screen != null) return;
            long now = System.currentTimeMillis();
            if (MyCustomScreen.shiftTapActive) {
                if (now - MyCustomScreen.shiftTapReleaseTime >= 50) {
                    client.options.keyShift.setDown(true);
                    MyCustomScreen.shiftTapActive = false;
                }
                return;
            }
            if (client.options.keyAttack.isDown() && client.options.keyShift.isDown()) {
                client.options.keyShift.setDown(false);
                MyCustomScreen.shiftTapActive = true;
                MyCustomScreen.shiftTapReleaseTime = now;
            }
        });

        // ===== COMBO: сброс =====
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (!MyCustomScreen.comboEnabled) return;
            if (MyCustomScreen.currentCombo <= 0) return;
            long comboNow = System.currentTimeMillis();
            if (comboNow - MyCustomScreen.lastComboTime > (long) MyCustomScreen.comboResetTime * 1000L) {
                MyCustomScreen.currentCombo = 0;
            }
        });

        // ===== TAPEMOUSE: логика (ЛКМ / ПКМ) =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!MyCustomScreen.tapeMouseEnabled) return;
            if (client.player == null || client.level == null) return;
            if (client.screen != null) return;

            // ===== ВЕТКА ПКМ =====
            if (MyCustomScreen.tapeMouseButton == 1) {
                // Зажать ПКМ — обрабатывается во втором тике, тут просто выходим
                if (MyCustomScreen.tapeMouseHoldRight) {
                    return;
                }

                // Обычный режим ПКМ: useItem с задержкой
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAttackTime < (long)(MyCustomScreen.tapeMouseDelay * 1000)) return;
                if (client.gameMode != null) {
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    lastAttackTime = currentTime;
                }
                return;
            }

            // ===== ВЕТКА ЛКМ (атака) =====
            if (MyCustomScreen.tapeMouseRequireFullAttack) {
                if (client.player.getAttackStrengthScale(0.0f) < 1.0f) return;
            }
            Entity target = client.crosshairPickEntity;
            if (MyCustomScreen.tapeMouseRequireTarget) {
                if (target == null) return;
                boolean isPlayer = target instanceof Player;
                boolean isMob = target instanceof LivingEntity && !isPlayer;
                switch (MyCustomScreen.tapeMouseTarget) {
                    case 1: if (!isMob) return; break;
                    case 2: if (!isPlayer) return; break;
                }
            } else {
                if (target != null) {
                    boolean isPlayer = target instanceof Player;
                    boolean isMob = target instanceof LivingEntity && !isPlayer;
                    switch (MyCustomScreen.tapeMouseTarget) {
                        case 1: if (!isMob) return; break;
                        case 2: if (!isPlayer) return; break;
                    }
                }
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime < (long)(MyCustomScreen.tapeMouseDelay * 1000)) return;
            if (client.gameMode != null) {
                if (target != null) client.gameMode.attack(client.player, target);
                client.player.swing(InteractionHand.MAIN_HAND);
                lastAttackTime = currentTime;
            }
        });

        // ===== TAPEMOUSE: отпускание ПКМ при выключении (флаг) =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean shouldHold = MyCustomScreen.tapeMouseEnabled
                    && MyCustomScreen.tapeMouseButton == 1
                    && MyCustomScreen.tapeMouseHoldRight
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

    // =========================================================
    // HUD
    // =========================================================
    private static void renderHud(GuiGraphics graphics) {
        if (!MyCustomScreen.showHud) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        int alpha = MyCustomScreen.hudAlpha;
        int baseColor = MyCustomScreen.hudColor;
        int color = (baseColor & 0x00FFFFFF) | (alpha << 24);

        // ===== CROSSHAIR =====
        if (MyCustomScreen.crosshairEnabled) {
            int chCenterX = client.getWindow().getGuiScaledWidth() / 2;
            int chCenterY = client.getWindow().getGuiScaledHeight() / 2;

            int chColor = (MyCustomScreen.crosshairColor & 0x00FFFFFF) | (MyCustomScreen.crosshairAlpha << 24);
            int size = MyCustomScreen.crosshairSize;
            int thick = MyCustomScreen.crosshairThickness;
            int gap = MyCustomScreen.crosshairGap;

            graphics.fill(chCenterX - thick / 2, chCenterY - gap - size,
                    chCenterX + (thick + 1) / 2, chCenterY - gap, chColor);
            graphics.fill(chCenterX - thick / 2, chCenterY + gap,
                    chCenterX + (thick + 1) / 2, chCenterY + gap + size, chColor);
            graphics.fill(chCenterX - gap - size, chCenterY - thick / 2,
                    chCenterX - gap, chCenterY + (thick + 1) / 2, chColor);
            graphics.fill(chCenterX + gap, chCenterY - thick / 2,
                    chCenterX + gap + size, chCenterY + (thick + 1) / 2, chColor);
        }

        // ===== ИКОНКА МОДА =====
        if (MyCustomScreen.showModLogo) {
            int logoX = MyCustomScreen.modLogoX;
            int logoY = MyCustomScreen.modLogoY;

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

        // ===== COMBO COUNTER =====
        if (MyCustomScreen.comboEnabled && MyCustomScreen.currentCombo > 0) {
            String comboText = "x" + MyCustomScreen.currentCombo;
            int fontSize = MyCustomScreen.comboFontSize;

            float scale;
            if (fontSize == 0) scale = 1.0f;
            else if (fontSize == 1) scale = 1.5f;
            else scale = 2.0f;

            graphics.pose().pushMatrix();
            graphics.pose().translate(MyCustomScreen.comboX, MyCustomScreen.comboY);
            graphics.pose().scale(scale, scale);

            graphics.drawString(client.font, comboText, 0, 0, MyCustomScreen.comboColor, true);

            graphics.pose().popMatrix();
        }

        // ===== КООРДИНАТЫ =====
        if (MyCustomScreen.showCoords) {
            drawHudString(graphics, client.font,
                    String.format("XYZ: %d / %d / %d",
                            (int) client.player.getX(),
                            (int) client.player.getY(),
                            (int) client.player.getZ()),
                    MyCustomScreen.coordsX, MyCustomScreen.coordsY, color);
        }

        // ===== БИОМ =====
        if (MyCustomScreen.showBiome) {
            String biome = client.level.getBiome(client.player.blockPosition())
                    .unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("unknown");
            drawHudString(graphics, client.font, "Биом: " + biome,
                    MyCustomScreen.biomeX, MyCustomScreen.biomeY, color);
        }

        // ===== ВРЕМЯ =====
        if (MyCustomScreen.showTime) {
            long time = client.level.getDayTime() % 24000;
            String timeStr = time < 12000 ? "День" : "Ночь";
            drawHudString(graphics, client.font, "Время: " + timeStr,
                    MyCustomScreen.timeX, MyCustomScreen.timeY, color);
        }

        // ===== FPS =====
        if (MyCustomScreen.showFps) {
            String label = MyCustomScreen.fpsRussian ? "КВС" : "FPS";
            drawHudString(graphics, client.font, label + ": " + client.getFps(),
                    MyCustomScreen.fpsX, MyCustomScreen.fpsY, color);
        }

        // ===== PING =====
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

        // ===== TPS =====
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

        // ===== BPS =====
        if (MyCustomScreen.showBps) {
            String label = MyCustomScreen.bpsRussian ? "БВС" : "BPS";
            drawHudString(graphics, client.font,
                    String.format("%s: %.2f", label, MyCustomScreen.currentBps),
                    MyCustomScreen.bpsX, MyCustomScreen.bpsY, color);
        }

        // ===== DIRECTION =====
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

        // ===== HITS =====
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

        // ===== POTION EFFECTS =====
        if (MyCustomScreen.showPotionEffects) {
            Collection<MobEffectInstance> effects = client.player.getActiveEffects();
            if (!effects.isEmpty()) {
                if (MyCustomScreen.potionEffectsIcons) {
                    int yOffset = 0;
                    for (MobEffectInstance effect : effects) {
                        MobEffect type = effect.getEffect().value();
                        Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(type);
                        if (effectId == null) continue;

                        int iconX = MyCustomScreen.potionEffectsX;
                        int iconY = MyCustomScreen.potionEffectsY + yOffset;

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
                    String label = MyCustomScreen.potionEffectsRussian ? "Эффекты:" : "Effects:";
                    drawHudString(graphics, client.font, label,
                            MyCustomScreen.potionEffectsX, MyCustomScreen.potionEffectsY, color);

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
                                MyCustomScreen.potionEffectsX,
                                MyCustomScreen.potionEffectsY + textOffset, color);
                        textOffset += 10;
                    }
                }
            }
        }

        // ===== EFFECT WARNINGS =====
        if (MyCustomScreen.effectWarningsEnabled) {
            Collection<MobEffectInstance> warnEffects = client.player.getActiveEffects();
            int warnY = 0;
            for (MobEffectInstance effect : warnEffects) {
                int durationSec = effect.getDuration() / 20;
                if (durationSec > MyCustomScreen.effectWarningsThreshold) continue;

                MobEffect type = effect.getEffect().value();

                int timeColor;
                if (durationSec > MyCustomScreen.effectWarningsThreshold * 2 / 3) {
                    timeColor = 0xFF00FF00;
                } else if (durationSec > MyCustomScreen.effectWarningsThreshold / 3) {
                    timeColor = 0xFFFFFF00;
                } else {
                    timeColor = 0xFFFF0000;
                }

                int finalColor = (timeColor & 0x00FFFFFF) | (MyCustomScreen.effectWarningsAlpha << 24);
                int alphaColor = (MyCustomScreen.effectWarningsColor & 0x00FFFFFF)
                        | (MyCustomScreen.effectWarningsAlpha << 24);

                int x = MyCustomScreen.effectWarningsX;
                int y = MyCustomScreen.effectWarningsY + warnY;

                if (MyCustomScreen.effectWarningsShowIcon) {
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

                if (MyCustomScreen.effectWarningsShowName) {
                    String name = type.getDisplayName().getString();
                    graphics.drawString(client.font, name, x, y + 5, alphaColor, true);
                    x += client.font.width(name) + 4;
                }

                String timeStr = durationSec + "s";
                graphics.drawString(client.font, timeStr, x, y + 5, finalColor, true);

                warnY += 20;
            }
        }

        // ===== EQUIPMENT HUD =====
        if (MyCustomScreen.showEquipmentHud) {
            int guiW = client.getWindow().getGuiScaledWidth();
            int guiH = client.getWindow().getGuiScaledHeight();
            int hotbarRight = (guiW + 182) / 2;
            int hotbarBottom = guiH - 22;
            int slotX = hotbarRight + MyCustomScreen.equipmentHudX;
            int slotY = hotbarBottom + MyCustomScreen.equipmentHudY;

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

    // =========================================================
    // ХЕЛПЕРЫ
    // =========================================================

    private static int findAutoSwapSlot(Player player) {
        ItemStack offhand = player.getOffhandItem();

        boolean offhandIsHead = offhand.is(Items.PLAYER_HEAD);
        boolean offhandIsTotem = offhand.is(Items.TOTEM_OF_UNDYING);

        boolean offhandAllowedHead = false;
        boolean offhandAllowedTotem = false;

        switch (MyCustomScreen.autoSwapMode) {
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

            if (MyCustomScreen.autoSwapMode == 0) {
                if (stack.is(Items.PLAYER_HEAD)) return i;
            } else if (MyCustomScreen.autoSwapMode == 1) {
                if (stack.is(Items.TOTEM_OF_UNDYING)) return i;
            } else {
                if (offhandIsHead && stack.is(Items.TOTEM_OF_UNDYING)) return i;
                if (offhandIsTotem && stack.is(Items.PLAYER_HEAD)) return i;
            }
        }

        return -1;
    }

    private static void swapOffhandWithSlot(Player player, int slotIndex) {
        ItemStack offhandItem = player.getOffhandItem().copy();
        ItemStack inventoryItem = player.getInventory().getItem(slotIndex).copy();

        player.getInventory().setItem(slotIndex, offhandItem);
        player.setItemSlot(EquipmentSlot.OFFHAND, inventoryItem);
    }

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

    private static void drawEquipmentSlot(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);

        if (MyCustomScreen.equipmentShowDurability && stack.isDamageableItem()) {
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

    private void sendMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}