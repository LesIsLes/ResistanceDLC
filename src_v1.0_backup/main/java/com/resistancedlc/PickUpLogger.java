package com.resistancedlc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Items;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Логика PickUpLogger — фильтрация подобранных предметов.
 * Использует потокобезопасную очередь для передачи из Netty-потока в рендер-поток.
 */
public class PickUpLogger {

    public static final int MODE_ALL = 0;
    public static final int MODE_VALUABLE = 1;
    public static final int MODE_CATEGORIES = 2;

    /** Очередь сообщений из Netty-потока для рендер-потока. */
    private static final ConcurrentLinkedQueue<String> PENDING_MESSAGES = new ConcurrentLinkedQueue<>();

    /**
     * Вызывается из миксина (Netty-поток). Кладёт сообщение в очередь.
     */
    public static void queuePickup(ItemStack stack) {
        if (!MyCustomScreen.pickupLogEnabled) return;
        if (stack == null || stack.isEmpty()) return;
        if (!shouldLog(stack)) return;

        int count = stack.getCount();
        String name = stack.getHoverName().getString();
        String msg = "§a[PickUp] §f+" + count + " §e" + name;

        PENDING_MESSAGES.offer(msg);
    }

    /**
     * Вызывается из ClientTickEvents (рендер-поток). Отправляет накопленные сообщения в чат.
     */
    public static void tick() {
        if (PENDING_MESSAGES.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            PENDING_MESSAGES.clear();
            return;
        }

        String msg;
        while ((msg = PENDING_MESSAGES.poll()) != null) {
            client.player.displayClientMessage(Component.literal(msg), false);
        }
    }

    public static boolean shouldLog(ItemStack stack) {
        if (!MyCustomScreen.pickupLogEnabled) return false;
        if (stack == null || stack.isEmpty()) return false;

        int mode = MyCustomScreen.pickupLogMode;

        if (mode == MODE_ALL) return true;

        if (mode == MODE_VALUABLE) {
            Rarity rarity = stack.getRarity();
            return rarity == Rarity.RARE || rarity == Rarity.EPIC;
        }

        if (mode == MODE_CATEGORIES) {
            return matchesCategory(stack);
        }

        return false;
    }

    private static boolean matchesCategory(ItemStack stack) {
        if (MyCustomScreen.pickupLogWeapon && isWeapon(stack)) return true;
        if (MyCustomScreen.pickupLogArmor && isArmor(stack)) return true;
        if (MyCustomScreen.pickupLogPotions && isPotion(stack)) return true;
        if (MyCustomScreen.pickupLogTotems && stack.is(Items.TOTEM_OF_UNDYING)) return true;
        if (MyCustomScreen.pickupLogHeads && stack.is(Items.PLAYER_HEAD)) return true;
        if (MyCustomScreen.pickupLogSpawners && stack.is(Items.SPAWNER)) return true;
        if (MyCustomScreen.pickupLogStructureBlocks) {
            if (stack.is(Items.JIGSAW) || stack.is(Items.STRUCTURE_BLOCK)) return true;
        }
        return false;
    }

    private static boolean isWeapon(ItemStack stack) {
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW) || stack.is(Items.TRIDENT)) return true;

        if (stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_AXE)
                || stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.DIAMOND_SHOVEL)
                || stack.is(Items.DIAMOND_HOE)) return true;

        if (stack.is(Items.NETHERITE_SWORD) || stack.is(Items.NETHERITE_AXE)
                || stack.is(Items.NETHERITE_PICKAXE) || stack.is(Items.NETHERITE_SHOVEL)
                || stack.is(Items.NETHERITE_HOE)) return true;

        return false;
    }

    private static boolean isArmor(ItemStack stack) {
        if (stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE)
                || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)) return true;

        if (stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
                || stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS)) return true;

        return false;
    }

    private static boolean isPotion(ItemStack stack) {
        if (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)) return true;
        if (stack.is(Items.TIPPED_ARROW)) return true;
        return false;
    }
}