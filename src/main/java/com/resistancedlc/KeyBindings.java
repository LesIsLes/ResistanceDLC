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
    private static KeyMapping.Category category;

    public static void register() {
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
}