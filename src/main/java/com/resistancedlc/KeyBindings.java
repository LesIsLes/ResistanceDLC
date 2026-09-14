package com.resistancedlc;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
    public static KeyMapping totemLogKey;   // ← НОВОЕ
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

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                category
        ));

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.zoom",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                category
        ));

        tapeMouseKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.tape_mouse",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                category
        ));

        autoSwapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.auto_swap",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category
        ));
        customHitSoundsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.custom_hit_sounds",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                category
        ));

        fastExpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.fast_exp",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                category
        ));

        shiftTapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.shift_tap",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                category
        ));

        comboKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.combo",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                category
        ));

        effectWarningsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.effect_warnings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                category
        ));

        waypointsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.waypoints",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
        ));

        totemLogKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.totem_log",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                Minecraft.getInstance().setScreen(new MyCustomScreen());
            }
        });
    }

    public static void setKey(int keyCode) {
        if (openGuiKey != null) {
            openGuiKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setZoomKey(int keyCode) {
        if (zoomKey != null) {
            zoomKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setTapeMouseKey(int keyCode) {
        if (tapeMouseKey != null) {
            tapeMouseKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setAutoSwapKey(int keyCode) {
        if (autoSwapKey != null) {
            autoSwapKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setCustomHitSoundsKey(int keyCode) {
        if (customHitSoundsKey != null) {
            customHitSoundsKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setFastExpKey(int keyCode) {
        if (fastExpKey != null) {
            fastExpKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setShiftTapKey(int keyCode) {
        if (shiftTapKey != null) {
            shiftTapKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setComboKey(int keyCode) {
        if (comboKey != null) {
            comboKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setEffectWarningsKey(int keyCode) {
        if (effectWarningsKey != null) {
            effectWarningsKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setWaypointsKey(int keyCode) {
        if (waypointsKey != null) {
            waypointsKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }

    public static void setTotemLogKey(int keyCode) {
        if (totemLogKey != null) {
            totemLogKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
            Minecraft.getInstance().options.save();
            Minecraft.getInstance().options.load();
        }
    }
}