package com.resistancedlc.mixin;

import com.resistancedlc.AutoToolManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class AutoToolMixin {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), require = 0)
    private void onStartDestroyBlock(BlockPos pos, Direction direction,
                                     CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        BlockState state = client.level.getBlockState(pos);
        AutoToolManager.onStartDestroyBlock(state);
    }
}