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
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.world.phys.Vec3;

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

        // ===== СОХРАНЕНИЕ ПРИ ВЫХОДЕ ИЗ МИРА + ОЧИСТКА ТРЕКЕРА =====
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ConfigManager.saveNow();
            TotemTracker.clear();
        });

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

            dispatcher.register(
                    ClientCommandManager.literal("wp")
                            .executes(context -> {
                                showWaypointsHelp();
                                return 1;
                            })
                            .then(ClientCommandManager.literal("help")
                                    .executes(context -> {
                                        showWaypointsHelp();
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("add")
                                    .executes(context -> {
                                        Minecraft mc = Minecraft.getInstance();
                                        if (mc.player == null) return 0;

                                        double x = mc.player.getX();
                                        double y = mc.player.getY();
                                        double z = mc.player.getZ();

                                        boolean added = MyCustomScreen.addWaypoint(x, y, z);
                                        if (added) {
                                            int newIndex = MyCustomScreen.getWaypoints().size();
                                            sendMessage(String.format(
                                                    "§a[Waypoints] Добавлена метка §6WP%d§a: §e%d, %d, %d",
                                                    newIndex, (int) x, (int) y, (int) z
                                            ));
                                        } else {
                                            sendMessage("§c[Waypoints] Достигнут лимит ("
                                                    + MyCustomScreen.waypointsMax + ")");
                                        }
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("list")
                                    .executes(context -> {
                                        List<MyCustomScreen.Waypoint> list = MyCustomScreen.getWaypoints();
                                        if (list.isEmpty()) {
                                            sendMessage("§7[Waypoints] Меток нет.");
                                        } else {
                                            sendMessage("§6[Waypoints] Метки (" + list.size() + "/"
                                                    + MyCustomScreen.waypointsMax + "):");
                                            for (int i = 0; i < list.size(); i++) {
                                                MyCustomScreen.Waypoint wp = list.get(i);
                                                sendMessage(String.format(
                                                        "§7  %d. §e%s §7(%d, %d, %d)",
                                                        i + 1, wp.name(),
                                                        (int) wp.x(), (int) wp.y(), (int) wp.z()
                                                ));
                                            }
                                        }
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("clear")
                                    .executes(context -> {
                                        MyCustomScreen.clearWaypoints();
                                        sendMessage("§a[Waypoints] Все метки удалены.");
                                        return 1;
                                    })
                            )
                            .then(ClientCommandManager.literal("remove")
                                    .then(ClientCommandManager.argument("name", StringArgumentType.string())
                                            .executes(context -> {
                                                String name = StringArgumentType.getString(context, "name");
                                                List<MyCustomScreen.Waypoint> list = MyCustomScreen.getWaypoints();
                                                int removed = 0;
                                                for (int i = list.size() - 1; i >= 0; i--) {
                                                    if (list.get(i).name().equals(name)) {
                                                        MyCustomScreen.removeWaypoint(i);
                                                        removed++;
                                                    }
                                                }
                                                if (removed > 0) {
                                                    sendMessage("§a[Waypoints] Удалено меток: §e" + removed);
                                                } else {
                                                    sendMessage("§c[Waypoints] Метка §e" + name + "§c не найдена.");
                                                }
                                                return 1;
                                            })
                                    )
                            )
                            .then(ClientCommandManager.literal("rename")
                                    .then(ClientCommandManager.argument("old", StringArgumentType.string())
                                            .then(ClientCommandManager.argument("new", StringArgumentType.string())
                                                    .executes(context -> {
                                                        String oldName = StringArgumentType.getString(context, "old");
                                                        String newName = StringArgumentType.getString(context, "new");

                                                        if (MyCustomScreen.renameWaypoint(oldName, newName)) {
                                                            sendMessage("§a[Waypoints] Метка §e" + oldName + "§a переименована в §6" + newName);
                                                        } else {
                                                            sendMessage("§c[Waypoints] Метка §e" + oldName + "§c не найдена.");
                                                        }
                                                        return 1;
                                                    })
                                            )
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

        // ===== TOGGLE КЛАВИШАМИ =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (KeyBindings.customHitSoundsKey != null) {
                while (KeyBindings.customHitSoundsKey.consumeClick()) {
                    MyCustomScreen.customHitSoundsEnabled = !MyCustomScreen.customHitSoundsEnabled;
                    ConfigManager.save();
                    String state = MyCustomScreen.customHitSoundsEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Custom Hit Sounds " + state), true);
                }
            }

            if (KeyBindings.fastExpKey != null) {
                while (KeyBindings.fastExpKey.consumeClick()) {
                    MyCustomScreen.fastExpEnabled = !MyCustomScreen.fastExpEnabled;
                    ConfigManager.save();
                    String state = MyCustomScreen.fastExpEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6FastExp " + state), true);
                }
            }

            if (KeyBindings.shiftTapKey != null) {
                while (KeyBindings.shiftTapKey.consumeClick()) {
                    MyCustomScreen.shiftTapEnabled = !MyCustomScreen.shiftTapEnabled;
                    ConfigManager.save();
                    String state = MyCustomScreen.shiftTapEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6ShiftTap " + state), true);
                }
            }

            if (KeyBindings.comboKey != null) {
                while (KeyBindings.comboKey.consumeClick()) {
                    MyCustomScreen.comboEnabled = !MyCustomScreen.comboEnabled;
                    ConfigManager.save();
                    String state = MyCustomScreen.comboEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Combo Counter " + state), true);
                }
            }

            if (KeyBindings.effectWarningsKey != null) {
                while (KeyBindings.effectWarningsKey.consumeClick()) {
                    MyCustomScreen.effectWarningsEnabled = !MyCustomScreen.effectWarningsEnabled;
                    ConfigManager.save();
                    String state = MyCustomScreen.effectWarningsEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Effect Warnings " + state), true);
                }
            }

            // ===== TOTEM LOG (НОВОЕ) =====
            if (KeyBindings.totemLogKey != null) {
                while (KeyBindings.totemLogKey.consumeClick()) {
                    MyCustomScreen.totemLogEnabled = !MyCustomScreen.totemLogEnabled;
                    ConfigManager.save();
                    String state = MyCustomScreen.totemLogEnabled ? "§aвключён" : "§cвыключен";
                    client.player.displayClientMessage(
                            Component.literal("§6Totem Log " + state), true);
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

        // ===== WAYPOINTS: кейбинд B =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KeyBindings.waypointsKey == null) return;
            while (KeyBindings.waypointsKey.consumeClick()) {
                if (client.player == null) return;

                double x = client.player.getX();
                double y = client.player.getY();
                double z = client.player.getZ();

                boolean added = MyCustomScreen.addWaypoint(x, y, z);
                if (added) {
                    int newIndex = MyCustomScreen.getWaypoints().size();
                    client.player.displayClientMessage(
                            Component.literal(String.format(
                                    "§a[Waypoints] Добавлена метка §6WP%d§a: §e%d, %d, %d",
                                    newIndex, (int) x, (int) y, (int) z
                            )), true);
                } else {
                    client.player.displayClientMessage(
                            Component.literal("§c[Waypoints] Достигнут лимит меток ("
                                    + MyCustomScreen.waypointsMax + ")"), true);
                }
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

            if (MyCustomScreen.tapeMouseButton == 1) {
                if (MyCustomScreen.tapeMouseHoldRight) {
                    return;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastAttackTime < (long)(MyCustomScreen.tapeMouseDelay * 1000)) return;
                if (client.gameMode != null) {
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    lastAttackTime = currentTime;
                }
                return;
            }

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

        // ===== TAPEMOUSE: отпускание ПКМ при выключении =====
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

    // ===== HUD =====
    private static void renderHud(GuiGraphics graphics) {
        if (!MyCustomScreen.showHud) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        int alpha = MyCustomScreen.hudAlpha;
        int baseColor = MyCustomScreen.hudColor;
        int color = (baseColor & 0x00FFFFFF) | (alpha << 24);

        renderWaypoints(graphics);

        if (MyCustomScreen.crosshairEnabled) {
            int chCenterX = client.getWindow().getGuiScaledWidth() / 2;
            int chCenterY = client.getWindow().getGuiScaledHeight() / 2;

            int chColor = (MyCustomScreen.crosshairColor & 0x00FFFFFF) | (MyCustomScreen.crosshairAlpha << 24);
            int size = MyCustomScreen.crosshairSize;
            int thick = MyCustomScreen.crosshairThickness;
            int gap = MyCustomScreen.crosshairGap;
            int shape = MyCustomScreen.crosshairShape;

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

        if (MyCustomScreen.showCoords) {
            drawHudString(graphics, client.font,
                    String.format("XYZ: %d / %d / %d",
                            (int) client.player.getX(),
                            (int) client.player.getY(),
                            (int) client.player.getZ()),
                    MyCustomScreen.coordsX, MyCustomScreen.coordsY, color);
        }

        if (MyCustomScreen.showBiome) {
            String biome = client.level.getBiome(client.player.blockPosition())
                    .unwrapKey()
                    .map(key -> key.identifier().getPath())
                    .orElse("unknown");
            drawHudString(graphics, client.font, "Биом: " + biome,
                    MyCustomScreen.biomeX, MyCustomScreen.biomeY, color);
        }

        if (MyCustomScreen.showTime) {
            long time = client.level.getDayTime() % 24000;
            String timeStr = time < 12000 ? "День" : "Ночь";
            drawHudString(graphics, client.font, "Время: " + timeStr,
                    MyCustomScreen.timeX, MyCustomScreen.timeY, color);
        }

        if (MyCustomScreen.showFps) {
            String label = MyCustomScreen.fpsRussian ? "КВС" : "FPS";
            drawHudString(graphics, client.font, label + ": " + client.getFps(),
                    MyCustomScreen.fpsX, MyCustomScreen.fpsY, color);
        }

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

        if (MyCustomScreen.showBps) {
            String label = MyCustomScreen.bpsRussian ? "БВС" : "BPS";
            drawHudString(graphics, client.font,
                    String.format("%s: %.2f", label, MyCustomScreen.currentBps),
                    MyCustomScreen.bpsX, MyCustomScreen.bpsY, color);
        }

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

    private static void renderWaypoints(GuiGraphics graphics) {
        if (!MyCustomScreen.waypointsEnabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (client.options.hideGui) return;

        List<MyCustomScreen.Waypoint> waypoints = MyCustomScreen.getWaypoints();
        if (waypoints.isEmpty()) return;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();

        TrackedWaypoint.Projector projector = client.gameRenderer;

        for (MyCustomScreen.Waypoint wp : waypoints) {
            double dist = wp.distanceTo(px, py, pz);

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

            if (ndcX < -2.0 || ndcX > 2.0 || ndcY < -2.0 || ndcY > 2.0) continue;

            int screenX = (int) ((ndcX + 1.0) * 0.5 * screenW);
            int screenY = (int) ((1.0 - ndcY) * 0.5 * screenH);

            int wpColor;
            if (dist < 50.0) {
                wpColor = 0xFF00FF00;
            } else if (dist < 200.0) {
                wpColor = 0xFFFFFF00;
            } else {
                wpColor = 0xFFFF0000;
            }

            drawWaypointDiamond(graphics, screenX, screenY, 5, wpColor);

            String label = wp.name();
            String distStr = String.format("%.0fm", dist);

            int labelW = client.font.width(label);
            int distW = client.font.width(distStr);

            graphics.drawString(client.font, label,
                    screenX - labelW / 2, screenY - 16, wpColor, true);
            graphics.drawString(client.font, distStr,
                    screenX - distW / 2, screenY + 8, wpColor, true);
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

    private static void sendMessage(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(message), false);
        }
    }
}