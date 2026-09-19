package com.resistancedlc;

import com.resistancedlc.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class AutoToolManager {

    private static int previousSlot = -1;
    private static int switchedSlot = -1;

    public static void onStartDestroyBlock(BlockState state) {
        if (!ModConfig.autoToolEnabled) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        int bestSlot = findBestToolSlot(player, state);
        if (bestSlot < 0) return;

        int currentSlot = player.getInventory().getSelectedSlot();
        if (bestSlot == currentSlot) return;

        if (previousSlot < 0) {
            previousSlot = currentSlot;
        }

        player.getInventory().setSelectedSlot(bestSlot);
        switchedSlot = bestSlot;
    }

    public static void onStopDestroyBlock() {
        if (!ModConfig.autoToolEnabled) return;
        if (!ModConfig.autoToolSwitchBack) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        if (previousSlot >= 0 && switchedSlot >= 0) {
            player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
        switchedSlot = -1;
    }

    private static int findBestToolSlot(LocalPlayer player, BlockState state) {
        int currentSlot = player.getInventory().getSelectedSlot();
        ItemStack currentStack = player.getInventory().getItem(currentSlot);
        float currentSpeed = currentStack.getDestroySpeed(state);

        int bestSlot = -1;
        float bestSpeed = currentSpeed;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    public static void reset() {
        previousSlot = -1;
        switchedSlot = -1;
    }
}