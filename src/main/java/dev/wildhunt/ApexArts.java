package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.entity.TensuraProjectile;
import io.github.manasmods.tensura.entity.magic.MagicCircle;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import java.util.*;

/** The four arts have different targeting, resources, movement and field behavior. */
public final class ApexArts {
    private ApexArts() {}
    static Vec3 forward(LivingEntity e) {
        Vec3 look = e.getLookAngle().multiply(1, 0, 1).normalize();
        return look.lengthSqr() < 0.01 ? new Vec3(0, 0, 1) : look;
    }
    static Vec3 point(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "_x"), tag.getDouble(key + "_y"), tag.getDouble(key + "_z"));
    }
    static void point(CompoundTag tag, String key, Vec3 p) {
        tag.putDouble(key + "_x", p.x); tag.putDouble(key + "_y", p.y); tag.putDouble(key + "_z", p.z);
    }
    static Vec3 aim(ServerLevel level, LivingEntity caster, double range) {
        Vec3 from = caster.getEyePosition(), to = from.add(caster.getLookAngle().scale(range));
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation().subtract(0, 1.2, 0);
    }
    static List<LivingEntity> enemies(ServerLevel level, LivingEntity caster, Vec3 center, double range) {
        var list = level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(range),
            e -> HunterData.canHarm(caster, e) && e.position().distanceToSqr(center) <= range * range && caster.hasLineOfSight(e));
        list.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));
        return list;
    }
    static void push(LivingEntity target, Vec3 vector) {
        double resistance = Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1);
        target.push(vector.x * (1 - resistance), vector.y * (1 - resistance), vector.z * (1 - resistance));
        target.hurtMarked = true;
    }
    static void charge(ManasSkillInstance instance, int amount) {
        var tag = instance.getOrCreateTag(); tag.putInt("storm_charge", Math.min(3, tag.getInt("storm_charge") + amount));
        instance.markDirty();
    }
    public static void primary(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime();
        switch (skill.apex) {
            case REY_DAU -> {
                int charge = tag.getInt("storm_charge"); tag.putInt("storm_charge", 0);
                skill.railgun(level, instance, caster, 1 + charge * 0.25f, charge);
            }
            case UTH_DUNA, JIN_DAHAAD -> {
                Vec3 direction = forward(caster);
                point(tag, "travel_origin", caster.position()); point(tag, "travel_forward", direction);
                double reach = skill.apex == Apex.UTH_DUNA ? (skill.ultimate ? 26 : 18) : (skill.ultimate ? 56 : 36);
                Vec3 eye = caster.getEyePosition();
                Vec3 end = level.clip(new ClipContext(eye, eye.add(direction.scale(reach)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
                tag.putDouble("travel_reach", Math.min(reach, eye.distanceTo(end)));
                tag.putString("travel_dimension", level.dimension().location().toString());
                tag.putLong("travel_until", now + 16); tag.putInt("travel_step", 0);
                tag.put("travel_hits", new ListTag());
                advance(skill, level, instance, caster);
            }
            case NU_UDRA -> {
                Vec3 origin = caster.getEyePosition(); int hits = 0; Vec3 puddle = aim(level, caster, 10);
                for (var target : enemies(level, caster, caster.position(), skill.ultimate ? 22 : 14)) {
                    if (target.getEyePosition().subtract(origin).normalize().dot(caster.getLookAngle()) < 0.55) continue;
                    if (skill.strike(level, instance, caster, target, skill.balance.primary(), 0, false)) {
                        ApexVisuals.beam(level, skill.apex, caster.position().add(0, 0.6, 0), target.getEyePosition(), skill.ultimate);
                        push(target, caster.position().subtract(target.position()).normalize().scale(0.65));
                        puddle = target.position();
                        if (++hits >= (skill.ultimate ? 6 : 4)) break;
                    }
                }
                addPool(skill, level, instance, caster, puddle, false, 300);
            }
        }
    }
    public static void secondary(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime();
        tag.putLong("field_until", now + 120); tag.putLong("field_next", now + 20);
        tag.putString("field_dimension", level.dimension().location().toString());
        Vec3 center;
        switch (skill.apex) {
            case REY_DAU -> {
                center = caster.position().add(forward(caster).scale(5)); point(tag, "field", center);
                rods(skill, level, instance, caster, skill.balance.burst());
            }
            case UTH_DUNA -> {
                center = aim(level, caster, 6); point(tag, "field", center);
                ApexVisuals.circle(level, caster, skill.apex, center.add(0, 0.1, 0), (float)skill.radius(), 120, false);
                skill.area(level, instance, caster, center, skill.radius(), skill.balance.burst(), 1, false);
            }
            case NU_UDRA -> {
                boolean usable = false;
                for (Tag raw : tag.getList("oil_pools", Tag.TAG_COMPOUND)) {
                    var pool = (CompoundTag)raw;
                    if (validPool(pool, level, now) && point(pool, "pool").distanceToSqr(caster.position()) <= 32 * 32) usable = true;
                }
                if (!usable) addPool(skill, level, instance, caster, aim(level, caster, 6), false, 300);
                for (Tag raw : tag.getList("oil_pools", Tag.TAG_COMPOUND)) {
                    CompoundTag pool = (CompoundTag) raw;
                    if (validPool(pool, level, now) && point(pool, "pool").distanceToSqr(caster.position()) <= 32 * 32) { pool.putBoolean("lit", true); pool.putLong("until", now + 120); }
                }
                pools(skill, level, instance, caster, skill.balance.burst(), false);
            }
            case JIN_DAHAAD -> {
                center = caster.position().add(forward(caster).scale(4)); point(tag, "field", center);
                point(tag, "ward_normal", forward(caster)); tag.putInt("ward_budget", skill.ultimate ? 6 : 3);
                for (int i = -1; i <= 1; i++) ApexVisuals.circle(level, caster, skill.apex,
                    center.add(forward(caster).yRot((float)(Math.PI / 2)).scale(i * 3)).add(0, 1.4, 0), 2.5f, 120, true);
                frostWall(skill, level, instance, caster, skill.balance.burst());
            }
        }
    }
    public static void domain(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime();
        tag.putLong("domain_until", now + 200); tag.putLong("domain_next", now + 20); tag.putLong("domain_start", now);
        tag.putString("domain_dimension", level.dimension().location().toString()); point(tag, "domain", caster.position());
        tag.put("domain_hits", new ListTag()); tag.putInt("tide_phase", 0);
        ApexVisuals.circle(level, caster, skill.apex, caster.position().add(0, 0.1, 0), skill.apex == Apex.NU_UDRA ? 4 : 8, 200, false);
        if (skill.apex == Apex.NU_UDRA) {
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5;
                addPool(skill, level, instance, caster, caster.position().add(Math.cos(a) * 6, 0, Math.sin(a) * 6), true, 200);
            }
        }
        pulseDomain(skill, level, instance, caster, true);
    }
    static boolean validPool(CompoundTag pool, ServerLevel level, long now) {
        return pool.getLong("until") > now && pool.getString("dimension").equals(level.dimension().location().toString());
    }
    static void addPool(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, Vec3 p, boolean lit, int duration) {
        ListTag list = instance.getOrCreateTag().getList("oil_pools", Tag.TAG_COMPOUND);
        while (list.size() >= (skill.ultimate ? 5 : 3)) list.removeFirst();
        var pool = new CompoundTag(); point(pool, "pool", p); pool.putBoolean("lit", lit);
        pool.putLong("until", level.getGameTime() + duration); pool.putString("dimension", level.dimension().location().toString());
        list.add(pool); instance.getOrCreateTag().put("oil_pools", list);
        ApexVisuals.ring(level, skill.apex, p, 2.5, 0.1, skill.ultimate);
        instance.markDirty();
    }
    static void pools(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, float damage, boolean pulse) {
        Set<UUID> hit = new HashSet<>();
        for (Tag raw : instance.getOrCreateTag().getList("oil_pools", Tag.TAG_COMPOUND)) {
            var pool = (CompoundTag)raw;
            if (!validPool(pool, level, level.getGameTime()) || !pool.getBoolean("lit")) continue;
            Vec3 center = point(pool, "pool");
            if (center.distanceToSqr(caster.position()) > 32 * 32) continue;
            for (var enemy : enemies(level, caster, center, skill.ultimate ? 4 : 3)) {
                if (hit.add(enemy.getUUID())) skill.strike(level, instance, caster, enemy, damage, 1, pulse);
            }
            ApexVisuals.impact(level, skill.apex, center, skill.ultimate);
        }
    }
    static void rods(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, float damage) {
        Vec3 center = point(instance.getOrCreateTag(), "field"), side = forward(caster).yRot((float)(Math.PI / 2));
        Set<UUID> hit = new HashSet<>();
        for (int i = -1; i <= 1; i++) {
            Vec3 rod = center.add(side.scale(i * 4));
            ApexVisuals.circle(level, caster, skill.apex, rod, 1.3f, 22, false);
            for (var enemy : enemies(level, caster, rod, 6)) {
                if (!hit.add(enemy.getUUID())) continue;
                if (skill.strike(level, instance, caster, enemy, damage, 1, true)) {
                    charge(instance, 1);
                    ApexVisuals.beam(level, skill.apex, rod.add(0, 6, 0), enemy.getEyePosition(), skill.ultimate);
                }
                break;
            }
        }
    }
    static void advance(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); int step = tag.getInt("travel_step") + 1; tag.putInt("travel_step", step);
        Vec3 origin = point(tag, "travel_origin"), forward = point(tag, "travel_forward"), right = forward.yRot((float)(Math.PI / 2));
        boolean water = skill.apex == Apex.UTH_DUNA;
        double reach = tag.getDouble("travel_reach"), initial = Math.min(2, reach);
        double distance = initial + (reach - initial) * (step - 1) / 7;
        double previousDistance = step == 1 ? 0 : initial + (reach - initial) * (step - 2) / 7;
        Vec3 front = origin.add(forward.scale(distance));
        Vec3 midpoint = origin.add(forward.scale((previousDistance + distance) / 2));
        double halfLength = (distance - previousDistance) / 2 + 0.6, width = water ? 4 : 1.8;
        // Sweep the entire segment: widely spaced Ultimate fronts must not skip enemies.
        double searchRadius = Math.sqrt(halfLength * halfLength + width * width + 2.5 * 2.5);
        ListTag hit = tag.getList("travel_hits", Tag.TAG_STRING);
        for (var enemy : enemies(level, caster, midpoint, searchRadius)) {
            Vec3 d = enemy.position().subtract(midpoint);
            if (Math.abs(d.dot(forward)) > halfLength || Math.abs(d.dot(right)) > width || Math.abs(d.y) > 2.5) continue;
            if (hit.contains(StringTag.valueOf(enemy.getStringUUID()))) continue;
            if (!water && hit.size() >= (skill.ultimate ? 4 : 1)) break;
            if (skill.strike(level, instance, caster, enemy, skill.balance.primary(), 0, false)) {
                hit.add(StringTag.valueOf(enemy.getStringUUID()));
                if (water) push(enemy, forward.scale(0.85).add(0, 0.12, 0));
            }
        }
        tag.put("travel_hits", hit);
        if (water) ApexVisuals.wave(level, front, right, skill.ultimate ? 5 : 4);
        else {
            Vec3 previous = origin.add(forward.scale(previousDistance));
            ApexVisuals.beam(level, skill.apex, previous, front, skill.ultimate);
        }
        instance.markDirty();
    }
    static void frostWall(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, float damage) {
        var tag = instance.getOrCreateTag(); Vec3 center = point(tag, "field"), normal = point(tag, "ward_normal"), right = normal.yRot((float)(Math.PI / 2));
        for (var target : enemies(level, caster, center, 7)) {
            Vec3 relative = target.position().subtract(center);
            if (Math.abs(relative.dot(normal)) <= 2.5 && Math.abs(relative.dot(right)) <= 5)
                skill.strike(level, instance, caster, target, damage, 1, true);
        }
        for (int i = -2; i <= 2; i++) ApexVisuals.crystal(level, center.add(right.scale(i * 2)), 2.5);
    }
    static void intercept(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); int budget = tag.getInt("ward_budget"); if (budget <= 0) return;
        Vec3 center = point(tag, "field"), normal = point(tag, "ward_normal"), right = normal.yRot((float)(Math.PI / 2));
        for (var projectile : level.getEntitiesOfClass(Projectile.class, new AABB(center, center).inflate(8))) {
            if (projectile instanceof MagicCircle || projectile.getDeltaMovement().lengthSqr() < 0.01
                    || !(projectile.getOwner() instanceof LivingEntity shooter) || !HunterData.canHarm(caster, shooter)) continue;
            if (!skill.ultimate && projectile instanceof TensuraProjectile nativeShot && nativeShot.getSkill() != null
                    && nativeShot.getSkill().getSkill() instanceof Skill s && s.getType() == Skill.SkillType.ULTIMATE) continue;
            Vec3 current = projectile.position(), previous = current.subtract(projectile.getDeltaMovement());
            double a = previous.subtract(center).dot(normal), b = current.subtract(center).dot(normal);
            if (a * b > 0 && Math.abs(b) > 0.8) continue;
            double fraction = Math.abs(a - b) < 0.001 ? 1 : Math.clamp(a / (a - b), 0, 1);
            Vec3 crossing = previous.lerp(current, fraction).subtract(center);
            if (Math.abs(crossing.dot(right)) > 5 || crossing.y < -0.5 || crossing.y > 4) continue;
            ApexVisuals.impact(level, skill.apex, projectile.position(), skill.ultimate); projectile.discard();
            if (--budget <= 0) { tag.putLong("field_until", level.getGameTime()); break; }
        }
        tag.putInt("ward_budget", budget); instance.markDirty();
    }
    static void pulseDomain(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster, boolean first) {
        var tag = instance.getOrCreateTag(); float damage = first ? skill.balance.burst() : skill.balance.pulse() * 1.5f;
        switch (skill.apex) {
            case REY_DAU -> {
                var targets = enemies(level, caster, caster.position(), 14);
                targets.sort(Comparator.comparingDouble(LivingEntity::getMaxHealth).reversed());
                for (int i = 0; i < Math.min(5, targets.size()); i++) {
                    var target = targets.get(i);
                    if (skill.strike(level, instance, caster, target, damage, 3, true)) {
                        charge(instance, 1); ApexVisuals.beam(level, skill.apex, target.position().add(0, 8, 0), target.getEyePosition(), true);
                    }
                }
            }
            case UTH_DUNA -> {
                int phase = tag.getInt("tide_phase"); tag.putInt("tide_phase", phase + 1);
                for (var target : enemies(level, caster, caster.position(), 14)) {
                    skill.strike(level, instance, caster, target, damage, 3, true);
                    Vec3 direction = target.position().subtract(caster.position()).multiply(1, 0, 1).normalize();
                    push(target, direction.scale(phase % 2 == 0 ? -0.8 : 1.1).add(0, phase % 2 == 0 ? 0 : 0.2, 0));
                }
            }
            case NU_UDRA -> pools(skill, level, instance, caster, damage, !first);
            case JIN_DAHAAD -> {
                double radius = first ? 2 : Math.min(14, 2 + (level.getGameTime() - tag.getLong("domain_start")) * 12 / 180.0);
                ListTag hits = tag.getList("domain_hits", Tag.TAG_STRING);
                for (var target : enemies(level, caster, point(tag, "domain"), radius)) {
                    if (hits.contains(StringTag.valueOf(target.getStringUUID()))) continue;
                    if (skill.strike(level, instance, caster, target, skill.balance.burst() + 120, 3, true)) {
                        hits.add(StringTag.valueOf(target.getStringUUID()));
                        var rime = HunterData.of(target); rime.putInt("rime_stacks", 3);
                        rime.putUUID("rime_owner", caster.getUUID()); rime.putLong("rime_until", level.getGameTime() + 200);
                    }
                }
                tag.put("domain_hits", hits);
            }
        }
        instance.markDirty();
    }
    public static boolean tick(ApexSkill skill, ServerLevel level, ManasSkillInstance instance, LivingEntity caster) {
        var tag = instance.getOrCreateTag(); long now = level.getGameTime(); boolean active = false;
        if (tag.getLong("travel_until") > now && tag.getInt("travel_step") < 8
                && tag.getString("travel_dimension").equals(level.dimension().location().toString())) {
            active = true; if (now % 2 == 0) advance(skill, level, instance, caster);
        }
        ListTag pools = tag.getList("oil_pools", Tag.TAG_COMPOUND);
        int oldPoolCount = pools.size();
        for (int i = pools.size() - 1; i >= 0; i--) if (!validPool(pools.getCompound(i), level, now)) pools.remove(i);
        if (pools.size() != oldPoolCount) instance.markDirty();
        if (!pools.isEmpty()) {
            active = true;
            if (now % 10 == 0) for (Tag raw : pools) {
                var pool = (CompoundTag)raw;
                ApexVisuals.ring(level, skill.apex, point(pool, "pool"), pool.getBoolean("lit") ? 3 : 2.5, 0.1, skill.ultimate);
            }
        }
        boolean domain = tag.getLong("domain_until") > now && tag.getString("domain_dimension").equals(level.dimension().location().toString());
        if (tag.getLong("field_until") > now && tag.getString("field_dimension").equals(level.dimension().location().toString())) {
            active = true;
            if (skill.apex == Apex.JIN_DAHAAD) intercept(skill, level, instance, caster);
            if (skill.apex == Apex.UTH_DUNA && now % 4 == 0) {
                Vec3 center = point(tag, "field");
                for (var target : enemies(level, caster, center, skill.radius()))
                    push(target, center.subtract(target.position()).multiply(1, 0, 1).normalize().scale(0.22));
            }
            if (now >= tag.getLong("field_next")) {
                switch (skill.apex) {
                    case REY_DAU -> rods(skill, level, instance, caster, skill.balance.pulse());
                    case UTH_DUNA -> skill.area(level, instance, caster, point(tag, "field"), skill.radius(), skill.balance.pulse(), 1, true);
                    case NU_UDRA -> { if (!domain) pools(skill, level, instance, caster, skill.balance.pulse(), true); }
                    case JIN_DAHAAD -> frostWall(skill, level, instance, caster, skill.balance.pulse());
                }
                tag.putLong("field_next", now + 20); instance.markDirty();
            }
            if (now % 8 == 0 && skill.apex == Apex.UTH_DUNA) ApexVisuals.ring(level, skill.apex, point(tag, "field"), skill.radius(), 0.2, skill.ultimate);
        }
        if (domain) {
            active = true;
            if (now >= tag.getLong("domain_next")) {
                pulseDomain(skill, level, instance, caster, false);
                tag.putLong("domain_next", now + 20); instance.markDirty();
            }
            if (now % 8 == 0) {
                Vec3 center = skill.apex == Apex.JIN_DAHAAD ? point(tag, "domain") : caster.position();
                double radius = skill.apex == Apex.JIN_DAHAAD ? Math.min(14, 2 + (now - tag.getLong("domain_start")) * 12 / 180.0) : 14;
                ApexVisuals.domain(level, skill.apex, center, radius);
            }
        }
        return active;
    }
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || event.getNewDamage() <= 0) return;
        LivingEntity victim = event.getEntity();
        if (victim.isAlive() && event.getSource().getEntity() instanceof LivingEntity attacker && HunterData.canHarm(victim, attacker)) {
            for (var instance : SkillAPI.getSkillsFrom(victim).getLearnedSkills()) {
                if (!(instance.getSkill() instanceof ApexSkill skill) || skill.apex != Apex.UTH_DUNA) continue;
                var tag = instance.getOrCreateTag(); long now = level.getGameTime();
                if (tag.getLong("mantle_until") <= now || tag.getLong("counter_next") > now
                        || victim.distanceToSqr(attacker) > 144 || !victim.hasLineOfSight(attacker)) continue;
                tag.putLong("counter_next", now + (skill.ultimate ? 20 : 60)); instance.markDirty();
                skill.strike(level, instance, victim, attacker, 25, 2, false);
                push(attacker, attacker.position().subtract(victim.position()).normalize().scale(0.8));
            }
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !HunterData.canHarm(attacker, victim)) return;
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)
                && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)) return;
        for (var instance : SkillAPI.getSkillsFrom(attacker).getLearnedSkills()) {
            if (instance.getSkill() instanceof ApexSkill skill && skill.apex == Apex.NU_UDRA
                    && instance.getOrCreateTag().getLong("mantle_until") > level.getGameTime()) {
                var data = HunterData.of(victim);
                boolean own = data.hasUUID("oil_owner") && data.getUUID("oil_owner").equals(attacker.getUUID()) && data.getLong("oil_until") > level.getGameTime();
                data.putInt("oil_stacks", Math.min(3, (own ? data.getInt("oil_stacks") : 0) + 1));
                data.putUUID("oil_owner", attacker.getUUID()); data.putLong("oil_until", level.getGameTime() + 300);
            }
        }
    }
}
