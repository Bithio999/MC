package dev.wildhunt;

import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;
import io.github.manasmods.tensura.particle.TensuraParticleHelper;
import io.github.manasmods.tensura.particle.TensuraParticleUtils;
import io.github.manasmods.tensura.registry.particle.TensuraParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Reuse Tensura's registered particles/renderers without copying its assets. */
public final class ApexVisuals {
    private ApexVisuals() {}
    public static ParticleOptions element(Apex apex) {
        return switch (apex) {
            case REY_DAU -> TensuraParticleTypes.YELLOW_LIGHTNING_SPARK.get();
            case UTH_DUNA -> TensuraParticleUtils.getWaterEffect();
            case NU_UDRA -> TensuraParticleTypes.BLACK_FIRE.get();
            case JIN_DAHAAD -> TensuraParticleTypes.SNOWFLAKE_EFFECT.get();
        };
    }
    private static void dot(ServerLevel level, ParticleOptions type, Vec3 pos) {
        TensuraParticleHelper.spawnServerParticles(level, type, pos.x, pos.y, pos.z);
    }
    public static void circle(ServerLevel level, LivingEntity owner, Apex apex, Vec3 pos, float size, int ticks, boolean vertical) {
        var circle = new MagicCircle(level, owner);
        circle.setVariant(switch (apex) {
            case REY_DAU -> MagicCircleVariant.LIGHTNING;
            case UTH_DUNA -> MagicCircleVariant.WATER;
            case NU_UDRA -> MagicCircleVariant.FLAME;
            case JIN_DAHAAD -> MagicCircleVariant.ICE;
        });
        circle.setFollowPosition(false); circle.setFollowViewDistance(-1);
        circle.setSpinning(true); circle.setSize(size); circle.setRemoveIn(ticks);
        circle.setPos(pos); circle.setYRot(owner.getYRot()); circle.setXRot(vertical ? 0 : -90);
        level.addFreshEntity(circle);
    }
    public static void lash(ServerLevel level, Vec3 from, Vec3 to) {
        ArtVisualEntity.spawn(level, ArtVisualEntity.LASH, from, to.subtract(from), 16);
    }
    public static void iceBurst(ServerLevel level, LivingEntity caster, Vec3 center, double radius) {
        circle(level, caster, Apex.JIN_DAHAAD, center.add(0, 0.1, 0), (float)radius, 18, false);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            ArtVisualEntity.spawn(level, ArtVisualEntity.CRYSTAL, center.add(Math.cos(a) * radius * 0.6, 0, Math.sin(a) * radius * 0.6), new Vec3(0, 2 + (i % 3) * 0.5, 0), 18);
        }
    }
    public static void prison(ServerLevel level, LivingEntity caster, Vec3 center, int ticks) {
        circle(level, caster, Apex.JIN_DAHAAD, center.add(0, 0.1, 0), 1.8f, ticks, false);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            ArtVisualEntity.spawn(level, ArtVisualEntity.CRYSTAL, center.add(Math.cos(a) * 1.1, 0, Math.sin(a) * 1.1), new Vec3(0, 2.8, 0), ticks);
        }
    }
    public static void fallingIce(ServerLevel level, Vec3 impact) {
        ArtVisualEntity.spawn(level, ArtVisualEntity.FALLING_ICE, impact.add(0, 6, 0), new Vec3(0, -6, 0), 10);
    }
    public static void beam(ServerLevel level, Apex apex, Vec3 from, Vec3 to, boolean ultimate) {
        if (apex == Apex.REY_DAU) {
            Vec3 delta = to.subtract(from);
            // Two irregular filaments around a straight railgun core.
            for (int i = 0, steps = Math.min(110, Math.max(2, (int)(delta.length() * 3))); i <= steps; i++) {
                Vec3 core = from.add(delta.scale((double)i / steps));
                dot(level, TensuraParticleTypes.LIGHTNING_EFFECT.get(), core);
                if (i % 2 == 0) dot(level, element(apex), core.add(Math.sin(i * 2.3) * 0.28, Math.cos(i * 1.7) * 0.28, 0));
            }
        } else if (apex == Apex.JIN_DAHAAD) {
            Vec3 delta = to.subtract(from);
            for (int i = 0; i <= 20; i++) crystal(level, from.add(delta.scale(i / 20.0)), ultimate ? 2 : 1);
        } else {
            Vec3 delta = to.subtract(from);
            for (int i = 0; i <= 30; i++) dot(level, TensuraParticleUtils.getWaterBubble(), from.add(delta.scale(i / 30.0)));
        }
    }
    public static void wave(ServerLevel level, Vec3 center, Vec3 right, double width) {
        for (int i = -12; i <= 12; i++) {
            Vec3 p = center.add(right.scale(width * i / 12.0));
            for (int h = 0; h < 3; h++) dot(level, TensuraParticleUtils.getWaterBubble(), p.add(0, h * 0.55, 0));
        }
        dot(level, TensuraParticleUtils.getBlueWave(0.85f, 2, 0, false), center.add(0, 0.7, 0));
    }
    public static void crystal(ServerLevel level, Vec3 center, double height) {
        for (int i = 0; i < 8; i++) {
            double y = i * height / 8, width = 0.3 * (1 - i / 8.0);
            dot(level, element(Apex.JIN_DAHAAD), center.add(width, y, width));
            dot(level, ParticleTypes.END_ROD, center.add(-width, y, -width));
        }
    }
    public static void ring(ServerLevel level, Apex apex, Vec3 center, double radius, double height, boolean ultimate) {
        int points = 40;
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            Vec3 p = center.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
            if (apex == Apex.NU_UDRA && i % 2 == 0) dot(level, TensuraParticleUtils.getBlackCloud(0.2f, 0.2f, 0.2f), p);
            else dot(level, element(apex), p);
        }
    }
    public static void impact(ServerLevel level, Apex apex, Vec3 pos, boolean ultimate) {
        level.sendParticles(element(apex), pos.x, pos.y, pos.z, ultimate ? 24 : 12, 0.5, 0.7, 0.5, 0.03);
        if (apex == Apex.UTH_DUNA) dot(level, TensuraParticleUtils.getBlueWave(0.85f, 2, 0, false), pos);
        if (apex == Apex.NU_UDRA) dot(level, TensuraParticleUtils.getBlackWave(0.9f, 2, 0, false), pos);
        if (apex == Apex.JIN_DAHAAD) crystal(level, pos.subtract(0, 0.5, 0), ultimate ? 3 : 1.5);
    }
    public static void mantle(ServerLevel level, Apex apex, Vec3 p, boolean ultimate) {
        switch (apex) {
            case REY_DAU -> {
                dot(level, TensuraParticleUtils.getGoldAura(0.8f, 1.2f, 0), p.add(0, 0.8, 0));
                dot(level, element(apex), p.add(Math.sin(level.getGameTime()) * 0.7, 1.4, 0));
            }
            case UTH_DUNA -> {
                for (int i = 0; i < 12; i++) {
                    double angle = i * Math.PI / 6 + level.getGameTime() * 0.1;
                    dot(level, TensuraParticleUtils.getWaterBubble(), p.add(Math.cos(angle), 0.4 + (i % 3) * 0.6, Math.sin(angle)));
                }
            }
            case NU_UDRA -> {
                dot(level, TensuraParticleUtils.getBlackAura(0.6f, 1.3f, 0), p.add(0, 0.8, 0));
                level.sendParticles(element(apex), p.x, p.y + 0.5, p.z, 6, 0.7, 0.4, 0.7, 0.02);
            }
            case JIN_DAHAAD -> {
                crystal(level, p.add(-0.7, 0, 0), 1.2); crystal(level, p.add(0.7, 0, 0), 1.2);
            }
        }
    }
    public static void domain(ServerLevel level, Apex apex, Vec3 center, double radius) {
        switch (apex) {
            case REY_DAU -> {
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3 + level.getGameTime() * 0.03;
                    dot(level, TensuraParticleTypes.LIGHTNING_EFFECT.get(), center.add(Math.cos(a) * radius, 5, Math.sin(a) * radius));
                }
            }
            case UTH_DUNA -> {
                for (int i = 0; i < 3; i++) ring(level, apex, center, radius * (i + 1) / 3, i * 0.3, true);
            }
            case NU_UDRA -> {
                level.sendParticles(TensuraParticleUtils.getBlackCloud(0.5f, 0.5f, 0.5f), center.x, center.y + 0.4, center.z, 8, radius * 0.5, 0.2, radius * 0.5, 0);
            }
            case JIN_DAHAAD -> {
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    crystal(level, center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 2.5);
                }
            }
        }
    }
}
