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

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.open_gui", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G, category));

        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.zoom", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C, category));

        tapeMouseKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.tape_mouse", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R, category));

        autoSwapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.auto_swap", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H, category));

        customHitSoundsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.custom_hit_sounds", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J, category));

        fastExpKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.fast_exp", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K, category));

        shiftTapKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.shift_tap", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L, category));

        comboKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.combo", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M, category));

        effectWarningsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.effect_warnings", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N, category));

        waypointsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.waypoints", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B, category));

        totemLogKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.totem_log", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O, category));

        pickupLogKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.pickup_log", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P, category));

        // ===== MACROS =====
        macro1Key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.macro1", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F1, category));
        macro2Key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.macro2", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F2, category));
        macro3Key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.macro3", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F3, category));
        macro4Key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.macro4", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F4, category));
        macro5Key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.macro5", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F5, category));

        // ===== HUD EDITOR (F6) =====
        hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.hud_editor", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6, category));

        // ===== FREELOOK (LCTRL) =====
        freelookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resistancedlc.freelook", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL, category));

        // ============================================================
        // ТЕПЕРЬ — tick-лямбды. ТОЛЬКО consumeClick, БЕЗ registerKeyBinding!
        // ============================================================

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                Minecraft.getInstance().setScreen(new AccordionScreen());
            }
        });

        // ===== HUD EDITOR (F6) =====
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (hudEditorKey == null) return;
            while (hudEditorKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new com.resistancedlc.hud.HudEditScreen());
                }
            }
        });
    }

    // ============================================================
    // [v3.4.0 FIX] ЕДИНАЯ ТОЧКА ПРИМЕНЕНИЯ БИНДА
    //
    // В 1.21.11 setKey() НЕ сбрасывает внутренний кеш маппинга.
    // Без KeyMapping.resetMapping() новый бинд вступит в силу только
    // после перезапуска клиента (или смены языка).
    // ============================================================

    private static void applyBinding(KeyMapping mapping, int keyCode) {
        if (mapping == null) return;
        mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        KeyMapping.resetMapping();
        saveOptions();
    }

    // ============================================================
    // СЕТТЕРЫ (для binding в GUI)
    // ============================================================

    public static void setKey(int keyCode)              { applyBinding(openGuiKey, keyCode); }
    public static void setZoomKey(int keyCode)          { applyBinding(zoomKey, keyCode); }
    public static void setTapeMouseKey(int keyCode)     { applyBinding(tapeMouseKey, keyCode); }
    public static void setAutoSwapKey(int keyCode)      { applyBinding(autoSwapKey, keyCode); }
    public static void setCustomHitSoundsKey(int k)     { applyBinding(customHitSoundsKey, k); }
    public static void setFastExpKey(int keyCode)       { applyBinding(fastExpKey, keyCode); }
    public static void setShiftTapKey(int keyCode)      { applyBinding(shiftTapKey, keyCode); }
    public static void setComboKey(int keyCode)         { applyBinding(comboKey, keyCode); }
    public static void setEffectWarningsKey(int k)      { applyBinding(effectWarningsKey, k); }
    public static void setWaypointsKey(int keyCode)     { applyBinding(waypointsKey, keyCode); }
    public static void setTotemLogKey(int keyCode)      { applyBinding(totemLogKey, keyCode); }
    public static void setPickupLogKey(int keyCode)     { applyBinding(pickupLogKey, keyCode); }
    public static void setMacro1Key(int keyCode)        { applyBinding(macro1Key, keyCode); }
    public static void setMacro2Key(int keyCode)        { applyBinding(macro2Key, keyCode); }
    public static void setMacro3Key(int keyCode)        { applyBinding(macro3Key, keyCode); }
    public static void setMacro4Key(int keyCode)        { applyBinding(macro4Key, keyCode); }
    public static void setMacro5Key(int keyCode)        { applyBinding(macro5Key, keyCode); }
    public static void setFreelookKey(int keyCode)      { applyBinding(freelookKey, keyCode); }

    private static void saveOptions() {
        Minecraft.getInstance().options.save();
    }
}