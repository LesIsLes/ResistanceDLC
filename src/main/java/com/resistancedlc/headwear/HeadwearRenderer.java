package com.resistancedlc.headwear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.resistancedlc.FriendListManager;
import com.resistancedlc.config.ModConfig;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class HeadwearRenderer {

    private HeadwearRenderer() {}

    public static void render(WorldRenderContext ctx) {
        if (!ModConfig.headwearEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        HeadwearPreset preset = HeadwearPreset.byId(ModConfig.headwearPreset);
        List<HeadwearQuad> quads = preset.getQuads();
        if (quads.isEmpty()) return;

        PoseStack pose = ctx.matrices();
        if (pose == null) return;

        Vec3 camPos = ctx.worldState().cameraRenderState.pos;

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(RenderTypes.debugQuads());

        // Себе
        renderOnPlayer(pose, buffer, mc.player, camPos, quads);

        // Друзьям
        if (ModConfig.headwearShowOnFriends) {
            for (Player other : mc.level.players()) {
                if (other == mc.player) continue;
                String name = other.getName().getString();
                if (!FriendListManager.getFriends().contains(name)) continue;
                renderOnPlayer(pose, buffer, other, camPos, quads);
            }
        }

        buffers.endBatch(RenderTypes.debugQuads());
    }

    private static void renderOnPlayer(PoseStack pose, VertexConsumer buffer,
                                       Player player, Vec3 camPos, List<HeadwearQuad> quads) {
        double distSq = camPos.distanceToSqr(player.getX(), player.getY(), player.getZ());
        if (distSq > 64.0 * 64.0) return;

        // Получаем partialTick для плавной интерполяции
        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        // Интерполируем позицию (чтобы не дёргалось при движении)
        double px = net.minecraft.util.Mth.lerp(partialTick, player.xo, player.getX());
        double py = net.minecraft.util.Mth.lerp(partialTick, player.yo, player.getY());
        double pz = net.minecraft.util.Mth.lerp(partialTick, player.zo, player.getZ());

        // Интерполируем повороты (чтобы не отставало при резком повороте головы)
        float yaw = net.minecraft.util.Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float pitch = net.minecraft.util.Mth.lerp(partialTick, player.xRotO, player.getXRot());

        float pivotX = (float) (px - camPos.x);
        float pivotY = (float) (py + player.getBbHeight() + 0.01f
                + ModConfig.headwearOffsetY - camPos.y);
        float pivotZ = (float) (pz - camPos.z);

        pose.pushPose();
        pose.translate(pivotX, pivotY, pivotZ);

        // Применяем интерполированные повороты
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));

        pose.scale(ModConfig.headwearScale, ModConfig.headwearScale, ModConfig.headwearScale);

        Matrix4f matrix = pose.last().pose();
        int alpha = ModConfig.headwearAlpha;

        for (HeadwearQuad q : quads) {
            q.emit(buffer, matrix, alpha);
        }

        pose.popPose();
    }
}