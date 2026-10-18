package com.resistancedlc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static KeyMapping openGuiKey;
    public static KeyMapping zoomKey;
    public static KeyMapping tapeMouseKey;
    public static KeyMapping autoSwapKey;
    public static KeyMapping customHitSoundsKey;
    public static KeyMapping fastExpKey;
    public static KeyMapping shiftTapKey;
    public static KeyMapping comboKey;
    public static KeyMapping effectWarningsKey;
    public static KeyMapping waypointsKey;
    public static KeyMapping totemLogKey;
    public static KeyMapping pickupLogKey;
    public static KeyMapping hudEditorKey;
    public static KeyMapping freelookKey;

    // ===== MACROS =====
    public static KeyMapping macro1Key;
    public static KeyMapping macro2Key;
    public static KeyMapping macro3Key;
    public static KeyMapping macro4Key;
    public static KeyMapping macro5Key;

    private static KeyMapping.Category category;
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;

        if (category == null) {
            category = KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(ResistanceDLC.MOD_ID, "main")
            );
        }

        // ============================================================
        // ВСЕ РЕГИСТРАЦИИ — ТОЛЬКО ЗДЕСЬ, В register()
        // ============================================================

        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.open_gui", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G, category));

        zoomKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.zoom", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C, category));

        tapeMouseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.tape_mouse", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R, category));

        autoSwapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.auto_swap", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H, category));

        customHitSoundsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.custom_hit_sounds", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J, category));

        fastExpKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.fast_exp", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K, category));

        shiftTapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.shift_tap", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L, category));

        comboKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.combo", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M, category));

        effectWarningsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.effect_warnings", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N, category));

        waypointsKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.waypoints", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B, category));

        totemLogKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.totem_log", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O, category));

        pickupLogKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.pickup_log", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P, category));

        // ===== MACROS =====
        macro1Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.macro1", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F1, category));
        macro2Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.macro2", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F2, category));
        macro3Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.macro3", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F3, category));
        macro4Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.macro4", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4, category));
        macro5Key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.macro5", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F5, category));

        // ===== HUD EDITOR (F6) =====
        hudEditorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.hud_editor", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6, category));

        // ===== FREELOOK (LCTRL) =====
        freelookKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.resistancedlc.freelook", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL, category));

        // ============================================================
        // ТЕПЕРЬ — tick-лямбды. ТОЛЬКО consumeClick, БЕЗ registerKeyBinding!
        // ============================================================

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                Minecraft.getInstance().gui.setScreen(new AccordionScreen());
            }
        });

        // ===== HUD EDITOR (F6) =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (hudEditorKey == null) return;
            while (hudEditorKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new com.resistancedlc.hud.HudEditScreen());
                }
            }
        });
    }

    // ============================================================
    // СЕТТЕРЫ (для binding в GUI)
    // ============================================================

    public static void setKey(int keyCode) {
        if (openGuiKey != null) {
            openGuiKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setZoomKey(int keyCode) {
        if (zoomKey != null) {
            zoomKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setTapeMouseKey(int keyCode) {
        if (tapeMouseKey != null) {
            tapeMouseKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setAutoSwapKey(int keyCode) {
        if (autoSwapKey != null) {
            autoSwapKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setCustomHitSoundsKey(int keyCode) {
        if (customHitSoundsKey != null) {
            customHitSoundsKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setFastExpKey(int keyCode) {
        if (fastExpKey != null) {
            fastExpKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setShiftTapKey(int keyCode) {
        if (shiftTapKey != null) {
            shiftTapKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setComboKey(int keyCode) {
        if (comboKey != null) {
            comboKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setEffectWarningsKey(int keyCode) {
        if (effectWarningsKey != null) {
            effectWarningsKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setWaypointsKey(int keyCode) {
        if (waypointsKey != null) {
            waypointsKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setTotemLogKey(int keyCode) {
        if (totemLogKey != null) {
            totemLogKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setPickupLogKey(int keyCode) {
        if (pickupLogKey != null) {
            pickupLogKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setMacro1Key(int keyCode) {
        if (macro1Key != null) {
            macro1Key.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setMacro2Key(int keyCode) {
        if (macro2Key != null) {
            macro2Key.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setMacro3Key(int keyCode) {
        if (macro3Key != null) {
            macro3Key.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setMacro4Key(int keyCode) {
        if (macro4Key != null) {
            macro4Key.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setMacro5Key(int keyCode) {
        if (macro5Key != null) {
            macro5Key.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    public static void setFreelookKey(int keyCode) {
        if (freelookKey != null) {
            freelookKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            forceUpdateKeyMappings();
        }
    }

    private static void forceUpdateKeyMappings() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.save();
            // Принудительно обновляем кэш клавиш в игре, чтобы изменения вступили в силу немедленно
            KeyMapping.resetMapping();
        }
    }
}