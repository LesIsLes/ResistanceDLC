package com.resistancedlc;

import com.resistancedlc.config.ModConfig;

import com.mojang.blaze3d.platform.InputConstants;
import com.resistancedlc.mixin.AbstractContainerScreenAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * ItemScroller — быстрый перенос предметов через скролл в GUI контейнера.
 *
 * Логика:
 *   - Скролл над слотом → перемещает всю стопку (QUICK_MOVE, как Shift+ЛКМ)
 *   - Shift + скролл → перемещает 1 предмет (ПКМ-стиль)
 *   - Ctrl + скролл → перемещает все стопки такого типа
 *
 * Задержка (itemScrollerDelay) — между КАЖДЫМ эмулированным кликом.
 * По умолчанию 200ms = 5 кликов/сек.
 */
public class ItemScrollerManager {

    /** Время последнего эмулированного клика (мс). */
    private static long lastClickTime = 0;

    /**
     * Вызывается из миксина при скролле в GUI контейнера.
     */
    public static boolean onScroll(AbstractContainerScreen<?> screen, double scrollY) {
        if (!ModConfig.itemScrollerEnabled) return false;
        if (screen == null) return false;
        if (scrollY == 0) return false;

        // Единственный лог — для отладки, что миксин работает
        ResistanceDLC.LOGGER.info("[ItemScroller] scrollY=" + scrollY);

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return false;

        Slot hovered = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
        if (hovered == null) return false;
        if (!hovered.hasItem() && client.player.containerMenu.getCarried().isEmpty()) return false;

        // Проверяем задержку
        long now = System.currentTimeMillis();
        if (now - lastClickTime < ModConfig.itemScrollerDelay) {
            return true; // отменяем ванильный скролл, но клик не делаем
        }

        boolean shift = isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
        boolean ctrl = isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL);

        // Ctrl → все стопки (приоритет выше всех)
        if (ctrl && ModConfig.itemScrollerCtrlAll) {
            lastClickTime = now;
            return moveAllMatching(screen, hovered);
        }

        // Shift → 1 предмет (ПКМ-стиль)
        if (shift && ModConfig.itemScrollerShiftStack) {
            lastClickTime = now;
            return moveOne(screen, hovered);
        }

        // Обычный скролл → вся стопка (QUICK_MOVE, как Shift+ЛКМ)
        lastClickTime = now;
        return moveStack(screen, hovered);
    }

    /**
     * Перемещает всю стопку (QUICK_MOVE, как Shift+ЛКМ).
     */
    private static boolean moveStack(AbstractContainerScreen<?> screen, Slot slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return false;

        client.gameMode.handleInventoryMouseClick(
                screen.getMenu().containerId,
                slot.index,
                0,
                ClickType.QUICK_MOVE,
                client.player
        );
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

        // Если курсор пустой — берём 1 предмет из слота
        if (carried.isEmpty()) {
            if (!slot.hasItem()) return false;
            client.gameMode.handleInventoryMouseClick(
                    menu.containerId,
                    slot.index,
                    1,  // ПКМ = 1 предмет
                    ClickType.PICKUP,
                    client.player
            );
            return true;
        }

        // Если в курсоре что-то есть — кладём 1 предмет
        client.gameMode.handleInventoryMouseClick(
                menu.containerId,
                slot.index,
                1,  // ПКМ = 1 предмет
                ClickType.PICKUP,
                client.player
        );
        return true;
    }

    /**
     * Перемещает все стопки того же типа (Ctrl+скролл).
     * За один скролл — один слот (задержка между кликами).
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

            client.gameMode.handleInventoryMouseClick(
                    menu.containerId,
                    slot.index,
                    0,
                    ClickType.QUICK_MOVE,
                    client.player
            );
            moved++;
            if (moved >= 1) break;  // за один скролл — один слот
        }
        return moved > 0;
    }

    /**
     * Проверяет, зажата ли клавиша через ванильный InputConstants.
     * В 1.21.11 isKeyDown принимает Window, а не long-handle.
     */
    private static boolean isKeyDown(int keyCode) {
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) return false;
        return InputConstants.isKeyDown(client.getWindow(), keyCode);
    }
}