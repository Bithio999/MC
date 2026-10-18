package dev.wildhunt;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-authored geometry: both local and remote clients see the same effects. */
public final class ApexVisuals {
    private ApexVisuals() {}
    private static ParticleOptions tint(Apex apex, float size) {
        int rgb = apex.color;
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f,
            ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }
    private static void dot(ServerLevel level, ParticleOptions particle, Vec3 p) {
        level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }
    private static ParticleOptions element(Apex apex) {
        return switch (apex) {
            case REY_DAU -> ParticleTypes.ELECTRIC_SPARK;
            case UTH_DUNA -> ParticleTypes.FALLING_WATER;
            case NU_UDRA -> ParticleTypes.SOUL_FIRE_FLAME;
            case JIN_DAHAAD -> ParticleTypes.SNOWFLAKE;
        };
    }
    public static void beam(ServerLevel level, Apex apex, Vec3 from, Vec3 to, boolean ultimate) {
        Vec3 delta = to.subtract(from);
        Vec3 side = delta.normalize().cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 0.01) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(delta.normalize()).normalize();
        int steps = Math.min(160, Math.max(2, (int) (delta.length() * 3)));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 p = from.add(delta.scale(t));
            dot(level, tint(apex, ultimate ? 1.4f : 0.9f), p);
            double angle = i * 0.9, width = ultimate ? 0.45 : 0.24;
            Vec3 spiral = side.scale(Math.cos(angle) * width).add(up.scale(Math.sin(angle) * width));
            dot(level, element(apex), p.add(spiral));
            if (ultimate || i % 2 == 0) dot(level, tint(apex, 0.7f), p.subtract(spiral));
        }
    }
    public static void ring(ServerLevel level, Apex apex, Vec3 center, double radius, double height, boolean ultimate) {
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48;
            Vec3 p = center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            dot(level, tint(apex, ultimate ? 1.3f : 0.8f), p);
            if (i % 4 == 0) dot(level, element(apex), p.add(0, 0.3, 0));
        }
    }
    public static void impact(ServerLevel level, Apex apex, Vec3 p, boolean ultimate) {
        level.sendParticles(element(apex), p.x, p.y, p.z, ultimate ? 36 : 18, 0.6, 0.8, 0.6, 0.08);
        level.sendParticles(tint(apex, 1.5f), p.x, p.y, p.z, 12, 0.4, 0.4, 0.4, 0);
        if (ultimate) level.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }
    public static void mantle(ServerLevel level, Apex apex, Vec3 p, boolean ultimate) {
        double spin = level.getGameTime() * 0.15;
        for (int i = 0; i < 12; i++) {
            double a = spin + i * Math.PI / 6;
            Vec3 point = p.add(Math.cos(a) * 0.75, 0.4 + (i % 4) * 0.4, Math.sin(a) * 0.75);
            dot(level, i % 2 == 0 ? element(apex) : tint(apex, ultimate ? 1.2f : 0.7f), point);
        }
    }
    public static void domain(ServerLevel level, Apex apex, Vec3 center, double radius) {
        ring(level, apex, center, radius, 0.2, true);
        ring(level, apex, center, radius * 0.55, 0.3, true);
        if (level.getGameTime() % 20 == 0) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                Vec3 base = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                beam(level, apex, base, base.add(0, 4, 0), true);
            }
        }
    }
}
