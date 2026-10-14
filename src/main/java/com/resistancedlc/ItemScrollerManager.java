package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import com.resistancedlc.mixin.AbstractContainerScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * ItemScroller — быстрый перенос предметов через скролл в GUI контейнера.
 */
public class ItemScrollerManager {

    private static long lastClickTime = 0;

    public static boolean onScroll(AbstractContainerScreen<?> screen, double scrollY) {
        if (!ModConfig.itemScrollerEnabled) return false;
        if (screen == null) return false;
        if (scrollY == 0) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return false;

        Slot hovered = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
        if (hovered == null) return false;
        if (!hovered.hasItem() && client.player.containerMenu.getCarried().isEmpty()) return false;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < ModConfig.itemScrollerDelay) {
            return true;
        }

        boolean shift = isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
        boolean ctrl = isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL);

        if (ctrl && ModConfig.itemScrollerCtrlAll) {
            lastClickTime = now;
            return moveAllMatching(screen, hovered);
        }

        if (shift && ModConfig.itemScrollerShiftStack) {
            lastClickTime = now;
            return moveOne(screen, hovered);
        }

        lastClickTime = now;
        return moveStack(screen, hovered);
    }

    /**
     * Перемещает всю стопку (QUICK_MOVE, как Shift+ЛКМ).
     */
    private static boolean moveStack(AbstractContainerScreen<?> screen, Slot slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return false;

        client.gameMode.handleInventoryButtonClick(slot.index, 0);
        return true;
    }

    /**
     * Перемещает 1 предмет (ПКМ-клик).
     */
    private static boolean moveOne(AbstractContainerScreen<?> screen, Slot slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return false;

        AbstractContainerMenu menu = screen.getMenu();
        ItemStack carried = menu.getCarried();

        if (carried.isEmpty()) {
            if (!slot.hasItem()) return false;
            client.gameMode.handleInventoryButtonClick(slot.index, 1);
            return true;
        }

        return false;
    }

    /**
     * Перемещает все стопки того же типа (Ctrl+скролл).
     */
    private static boolean moveAllMatching(AbstractContainerScreen<?> screen, Slot sourceSlot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return false;
        if (!sourceSlot.hasItem()) return false;

        AbstractContainerMenu menu = screen.getMenu();
        ItemStack reference = sourceSlot.getItem();
        String refItemId = reference.getItem().toString();

        int moved = 0;
        for (Slot slot : menu.slots) {
            if (slot == sourceSlot) continue;
            if (!slot.hasItem()) continue;
            if (!slot.mayPickup(client.player)) continue;
            if (!slot.getItem().getItem().toString().equals(refItemId)) continue;

            client.gameMode.handleInventoryButtonClick(slot.index, 0);
            moved++;
            if (moved >= 1) break;
        }
        return moved > 0;
    }

    /**
     * Проверяет, зажата ли клавиша.
     */
    private static boolean isKeyDown(int keyCode) {
        long window = Minecraft.getInstance().getWindow().handle(); // проверь
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }
}