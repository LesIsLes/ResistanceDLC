package com.resistancedlc.headwear;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

/**
 * Один 3D-квад (4 вершины + цвет). Не хранит ссылок на игрока/мир,
 * полностью иммутабелен — можно кешировать и переиспользовать.
 */
public record HeadwearQuad(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int color
) {
    public void emit(VertexConsumer buffer, Matrix4f pose, int tintAlpha) {
        int c = (tintAlpha << 24) | (color & 0x00FFFFFF);
        buffer.addVertex(pose, x0, y0, z0).setColor(c);
        buffer.addVertex(pose, x1, y1, z1).setColor(c);
        buffer.addVertex(pose, x2, y2, z2).setColor(c);
        buffer.addVertex(pose, x3, y3, z3).setColor(c);
    }
}