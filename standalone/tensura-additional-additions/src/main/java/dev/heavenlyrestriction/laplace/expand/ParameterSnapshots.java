package dev.heavenlyrestriction.laplace.expand;

import dev.heavenlyrestriction.laplace.*;
import io.github.manasmods.manascore.skill.api.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/** Frozen effects retain both numerical outputs and the budget that funds their upkeep. */
public final class ParameterSnapshots {
  private static final String ENTITY = "heavenlyrestriction:laplace_parameters";

  private record Saved(
      UUID owner,
      String skill,
      int mode,
      EquationModel model,
      Map<String, String> equations,
      EquationModel.Result result) {}

  private record Cached(CompoundTag tag, Saved saved) {}

  private static final Map<Object, Cached> CACHE = new WeakHashMap<>();

  public static CompoundTag encode(ExpandedRuntime.Frame frame) {
    var tag = new CompoundTag();
    tag.putUUID("owner", frame.owner().getUUID());
    tag.putString("skill", frame.skill().getSkillId().toString());
    tag.putInt("mode", frame.mode());
    tag.put("model", LaplaceNetwork.encode(frame.action().model()));
    var eq = new CompoundTag();
    frame.equations().forEach(eq::putString);
    tag.put("equations", eq);
    return tag;
  }

  private static Saved decode(CompoundTag tag) {
    try {
      if (!tag.hasUUID("owner")
          || tag.getCompound("equations").size() > FormulaSet.MAX_EQUATIONS
          || tag.getCompound("model").getList("outputs", 10).size() > FormulaSet.MAX_EQUATIONS)
        return null;
      var model = LaplaceNetwork.decode(tag.getCompound("model"));
      var equations = new LinkedHashMap<String, String>();
      var eq = tag.getCompound("equations");
      for (String key : eq.getAllKeys()) equations.put(key, eq.getString(key));
      return new Saved(
          tag.getUUID("owner"),
          tag.getString("skill"),
          tag.getInt("mode"),
          model,
          equations,
          model.evaluate(equations));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static ExpandedRuntime.Scope scope(
      Object key, CompoundTag tag, net.minecraft.world.level.Level level, LivingEntity fallback) {
    var cached = CACHE.get(key);
    if (cached == null || cached.tag() != tag) {
      cached = new Cached(tag, decode(tag));
      CACHE.put(key, cached);
    }
    var saved = cached.saved();
    if (saved == null) return ExpandedRuntime.parameters(null);
    LivingEntity owner = null;
    if (level instanceof ServerLevel server) {
      owner = server.getServer().getPlayerList().getPlayer(saved.owner());
      if (owner == null && server.getEntity(saved.owner()) instanceof LivingEntity living)
        owner = living;
    }
    if (owner == null && fallback != null && fallback.getUUID().equals(saved.owner()))
      owner = fallback;
    if (owner == null) return ExpandedRuntime.parameters(saved.result().values());
    var id = net.minecraft.resources.ResourceLocation.tryParse(saved.skill());
    var skill = id == null ? null : SkillAPI.getSkillsFrom(owner).getSkill(id).orElse(null);
    if (skill == null && id != null) {
      var registered = SkillAPI.getSkillRegistry().get(id);
      if (registered != null) skill = registered.createDefaultInstance();
    }
    if (skill == null) return ExpandedRuntime.parameters(saved.result().values());
    return ExpandedRuntime.frozen(
        owner, skill, saved.mode(), saved.model(), saved.equations(), saved.result());
  }

  public static void spawned(EntityJoinLevelEvent event) {
    var frame = ExpandedRuntime.current();
    if (!event.getLevel().isClientSide()
        && frame != null
        && LaplaceRuntime.current() == null
        && !(event.getEntity() instanceof LivingEntity))
      event.getEntity().getPersistentData().put(ENTITY, encode(frame));
  }

  public static boolean has(Entity entity) {
    return entity.getPersistentData().contains(ENTITY);
  }

  public static ExpandedRuntime.Scope entity(Entity entity) {
    return scope(
        entity,
        entity.getPersistentData().getCompound(ENTITY),
        entity.level(),
        entity instanceof net.minecraft.world.entity.projectile.Projectile shot
                && shot.getOwner() instanceof LivingEntity living
            ? living
            : null);
  }

  public static void added(MobEffectEvent.Added event) {
    var frame = ExpandedRuntime.current();
    ((EffectParameters) (Object) event.getEffectInstance())
        .additions$parameters(frame == null ? null : encode(frame));
  }

  public static ExpandedRuntime.Scope effect(LivingEntity owner, MobEffectInstance effect) {
    var values = ((EffectParameters) (Object) effect).additions$parameters();
    return values == null
        ? ExpandedRuntime.parameters(null)
        : scope(effect, values, owner.level(), owner);
  }

  private ParameterSnapshots() {}
}
