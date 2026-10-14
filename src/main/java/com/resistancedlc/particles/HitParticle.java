package com.resistancedlc.particles;

import net.minecraft.world.phys.Vec3;

public final class HitParticle {

    public enum Shape {
        CRYSTAL,   // октаэдр
        STAR,      // 4-конечная звезда
        SNOWFLAKE, // 6-лучевая снежинка
        PYRAMID    // пирамидка
    }

    public final Vec3 position;      // текущая
    public final Vec3 velocity;      // скорость
    public final long spawnTime;     // когда заспавнилась
    public final long lifetimeMs;    // сколько живёт
    public final Shape shape;        // форма
    public final int color;          // ARGB
    public final float size;         // базовый размер
    public final float rotationSpeed; // вращение (для кристаллов и пирамид)

    public HitParticle(Vec3 position, Vec3 velocity, long spawnTime,
                       long lifetimeMs, Shape shape, int color,
                       float size, float rotationSpeed) {
        this.position = position;
        this.velocity = velocity;
        this.spawnTime = spawnTime;
        this.lifetimeMs = lifetimeMs;
        this.shape = shape;
        this.color = color;
        this.size = size;
        this.rotationSpeed = rotationSpeed;
    }

    public float getAge(long now) {
        return (float)(now - spawnTime) / lifetimeMs; // 0.0 .. 1.0
    }

    public boolean isDead(long now) {
        return now - spawnTime >= lifetimeMs;
    }

    public Vec3 getCurrentPosition(long now) {
        // Простая физика: position + velocity * t + gravity
        double t = (now - spawnTime) / 1000.0; // в секундах
        double gravity = -4.0; // blocks/sec²
        return new Vec3(
                position.x + velocity.x * t,
                position.y + velocity.y * t + 0.5 * gravity * t * t,
                position.z + velocity.z * t
        );
    }
}