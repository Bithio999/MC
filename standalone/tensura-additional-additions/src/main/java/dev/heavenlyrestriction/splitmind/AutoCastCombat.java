package dev.heavenlyrestriction.splitmind;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;

/** Provenance belongs to a cast/source, never to every attack made by a skill owner. */
public final class AutoCastCombat {
  private static final String TAG = "heavenlyrestriction:auto_cast";
  private static final Map<DamageSource, Origin> SOURCES = new WeakHashMap<>();

  public record Origin(UUID owner, ResourceLocation spell, int mode) {}

  public static void created(
      DamageSource source, LivingEntity owner, ManasSkillInstance spell, int mode) {
    if (SplitMindCasting.automatic(owner, spell) && SplitMindCasting.eligible(spell))
      SOURCES.put(source, new Origin(owner.getUUID(), spell.getSkillId(), mode));
  }

  public static void copy(DamageSource original, DamageSource copy) {
    var origin = origin(original);
    if (origin != null) SOURCES.put(copy, origin);
  }

  public static void spawned(
      Entity entity, LivingEntity owner, ManasSkillInstance spell, int mode) {
    Entity caster =
        entity instanceof net.minecraft.world.entity.projectile.Projectile shot
            ? shot.getOwner()
            : entity instanceof AreaEffectCloud cloud ? cloud.getOwner() : null;
    if (caster != owner
        || !SplitMindCasting.automatic(owner, spell)
        || !SplitMindCasting.eligible(spell)) return;
    var tag = new CompoundTag();
    tag.putUUID("owner", owner.getUUID());
    tag.putString("spell", spell.getSkillId().toString());
    tag.putInt("mode", mode);
    entity.getPersistentData().put(TAG, tag);
  }

  private static Origin origin(DamageSource source) {
    var attacker = source.getEntity();
    if (attacker == null) return null;
    var saved = SOURCES.get(source);
    if (saved != null) return saved.owner.equals(attacker.getUUID()) ? saved : null;
    var direct = source.getDirectEntity();
    if (direct == null || !direct.getPersistentData().contains(TAG)) return null;
    var tag = direct.getPersistentData().getCompound(TAG);
    var id = ResourceLocation.tryParse(tag.getString("spell"));
    if (id == null || !tag.hasUUID("owner") || !tag.getUUID("owner").equals(attacker.getUUID()))
      return null;
    return new Origin(attacker.getUUID(), id, tag.getInt("mode"));
  }

  public static Origin key(DamageSource source) {
    return origin(source);
  }

  public static Window window(Entity target, DamageSource source) {
    if (source.getDirectEntity()
            instanceof io.github.manasmods.tensura.entity.projectile.TensuraFlyingProjectile shot
        && shot.isIgnoreInvulnerabilityOnHit()) return null;
    var key = key(source);
    return key != null && target instanceof AutoCastWindowTarget living
        ? living.additions$autoWindow(key)
        : null;
  }

  /** A transient native hurt window, advanced by the target's clock rather than world time. */
  public static final class Window {
    private int tick, duration;
    private float damage;

    public int remaining(LivingEntity target) {
      int age = target.tickCount - tick;
      return age < 0 ? 0 : Math.max(0, duration - age);
    }

    public void duration(LivingEntity target, int value) {
      tick = target.tickCount;
      duration = value;
    }

    public float damage() {
      return damage;
    }

    public void damage(float value) {
      damage = value;
    }
  }

  private AutoCastCombat() {}
}
