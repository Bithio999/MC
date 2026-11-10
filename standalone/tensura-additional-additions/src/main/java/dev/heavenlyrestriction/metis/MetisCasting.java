package dev.heavenlyrestriction.metis;

import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.skill.impl.TickingSkill;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Parallel real owned spell instances; all native costs, modes, anti-magic and cooldowns remain.
 */
public final class MetisCasting {
  private record Cast(
      MetisMatrix.Slot slot, ManasSkillInstance spell, TickingSkill held, UUID target) {}

  private static final Map<ServerPlayer, List<Cast>> CASTS = new WeakHashMap<>();

  public static void cancel(LivingEntity e) {
    if (e instanceof ServerPlayer p)
      for (var c : List.copyOf(CASTS.getOrDefault(p, List.of()))) stop(p, c);
  }

  public static void cancel(ServerPlayer p, int slot) {
    for (var c : List.copyOf(CASTS.getOrDefault(p, List.of())))
      if (c.slot.index() == slot) stop(p, c);
  }

  public static void manual(LivingEntity e, ManasSkillInstance spell) {
    if (e instanceof ServerPlayer p && !SplitMindCasting.automatic(p))
      for (var c : List.copyOf(CASTS.getOrDefault(p, List.of())))
        if (c.spell.getSkill() == spell.getSkill()) stop(p, c);
  }

  private static void stop(ServerPlayer p, Cast c) {
    var list = CASTS.get(p);
    if (list != null) {
      list.remove(c);
      if (list.isEmpty()) CASTS.remove(p);
    }
    SkillAPI.getSkillsFrom(p).getHeldSkills().remove(c.held);
    c.spell.removeAttributeModifiers(p, c.slot.mode());
  }

  private static LivingEntity target(ServerPlayer p, Cast c) {
    if (MetisMagic.role(c.spell) != MetisMagic.Role.OFFENSE) return p;
    var e = p.serverLevel().getEntity(c.target);
    return e instanceof LivingEntity t && SplitMindCasting.enemy(p, t) ? t : null;
  }

  public static LivingEntity select(
      ServerPlayer p, MetisMatrix.Slot slot, ManasSkillInstance spell) {
    if (MetisMagic.role(spell) != MetisMagic.Role.OFFENSE)
      return MetisMagic.needed(p, spell) ? p : null;
    var target =
        io.github.manasmods.tensura.util.ObjectSelectionHelper.getTargetingEntity(p, 32, false);
    if (target == null || !SplitMindCasting.enemy(p, target)) target = p.getLastHurtMob();
    if (slot.currentTarget() && target != null && SplitMindCasting.enemy(p, target)) return target;
    return SplitMindCasting.nearest(p);
  }

  public static void update(ServerPlayer p) {
    var mind = MetisMatrix.owned(p);
    if (!p.isAlive()
        || p.isSpectator()
        || mind.isEmpty()
        || !mind.get().getOrCreateTag().getBoolean(MetisMatrix.AUTO)
        || !mind.get().canInteractSkill(p)
        || !mind.get().canActivateSkill(p, 1)) {
      cancel(p);
      return;
    }
    var slots = MetisMatrix.slots(p, mind.get());
    var storage = SkillAPI.getSkillsFrom(p);
    for (var c : List.copyOf(CASTS.getOrDefault(p, List.of()))) {
      var target = target(p, c);
      if (!slots.contains(c.slot)
          || target == null
          || !storage.getHeldSkills().contains(c.held)
          || !c.spell.canInteractSkill(p)
          || !c.spell.canActivateSkill(p, c.slot.mode())
          || !SplitMindCasting.funded(p, c.spell, c.slot.mode())) {
        stop(p, c);
        continue;
      }
      if (!SplitMindModes.sustained(c.spell, c.slot.mode())
          && c.held.getDuration() >= ((Magic) c.spell.getSkill()).getCastingTime(c.spell, p)) {
        try {
          var energy = TensuraStorages.getExistenceFrom(p);
          double aura = energy.getAura(), mp = energy.getMagicule(), mastery = c.spell.getMastery();
          int cooldown = c.spell.getCoolDown(c.slot.mode());
          SplitMindCasting.autoInvoke(
              p,
              c.spell,
              c.slot.mode(),
              target,
              () -> {
                storage.handleSkillRelease(c.spell, 3, c.slot.mode(), false);
                return true;
              });
          if (energy.getAura() < aura
              || energy.getMagicule() < mp
              || c.spell.getMastery() > mastery
              || c.spell.getCoolDown(c.slot.mode()) > cooldown) mind.get().addMasteryPoint(p);
        } finally {
          stop(p, c);
        }
      }
    }
    if (p.tickCount % 5 != 0) return;
    record Ready(
        MetisMatrix.Slot slot, ManasSkillInstance spell, LivingEntity target, double score) {}
    var ready = new ArrayList<Ready>();
    for (var slot : slots) {
      var spell = storage.getSkill(slot.id()).orElseThrow();
      int mode = slot.mode();
      if (CASTS.getOrDefault(p, List.of()).stream().anyMatch(c -> c.slot.index() == slot.index())
          || storage.getHeldSkills().stream().anyMatch(t -> t.getSkill() == spell.getSkill())
          || !spell.canInteractSkill(p)
          || !spell.canActivateSkill(p, mode)
          || spell.onCoolDown(mode) && !spell.canIgnoreCoolDown(p, mode)
          || !MetisMagic.needed(p, spell)) continue;
      var target = select(p, slot, spell);
      if (target == null) continue;
      double score =
          MetisMagic.role(spell) == MetisMagic.Role.OFFENSE
                  && mind.get().isToggled()
                  && MetisMatrix.optimal(mind.get())
              ? MetisMagic.effectiveness(p, target, spell, mode)
              : 1;
      if (score > 0) ready.add(new Ready(slot, spell, target, score));
    }
    ready.sort(
        Comparator.comparingInt((Ready r) -> r.slot.priority())
            .thenComparing(Comparator.comparingDouble(Ready::score).reversed())
            .thenComparingInt(r -> r.slot.index()));
    var recorded = new HashSet<UUID>();
    for (var r : ready) {
      if (!SplitMindCasting.funded(p, r.spell, r.slot.mode())) continue;
      if (r.target != p && mind.get().isToggled() && recorded.add(r.target.getUUID()))
        MetisMagic.record(p, r.target, mind.get());
      if (SplitMindCasting.autoInvoke(
          p,
          r.spell,
          r.slot.mode(),
          r.target,
          () -> storage.startHoldSkill(r.spell, 3, r.slot.mode())))
        storage.getHeldSkills().stream()
            .filter(
                t ->
                    t.getSkill() == r.spell.getSkill()
                        && t.getMode() == r.slot.mode()
                        && t.getKeyNumber() == 3)
            .findFirst()
            .ifPresent(
                t ->
                    CASTS
                        .computeIfAbsent(p, k -> new ArrayList<>())
                        .add(new Cast(r.slot, r.spell, t, r.target.getUUID())));
    }
  }

  public static boolean tickAimed(TickingSkill held, ServerPlayer p, Supplier<Boolean> original) {
    var cast = CASTS.getOrDefault(p, List.of()).stream().filter(c -> c.held == held).findFirst();
    if (cast.isEmpty()) return original.get();
    var c = cast.get();
    var target = target(p, c);
    if (target == null || !SplitMindModes.allowed(p, c.spell, c.slot.mode())) return false;
    var energy = TensuraStorages.getExistenceFrom(p);
    double aura = energy.getAura(), mp = energy.getMagicule();
    boolean keep = SplitMindCasting.autoInvoke(p, c.spell, c.slot.mode(), target, original);
    if (energy.getAura() < aura || energy.getMagicule() < mp)
      MetisMatrix.owned(p).ifPresent(i -> i.addMasteryPoint(p));
    if (!keep
        && SplitMindModes.sustained(c.spell, c.slot.mode())
        && held.getDuration() >= ((Magic) c.spell.getSkill()).getCastingTime(c.spell, p))
      SplitMindCasting.autoInvoke(
          p,
          c.spell,
          c.slot.mode(),
          target,
          () -> {
            SkillAPI.getSkillsFrom(p).handleSkillRelease(c.spell, 3, c.slot.mode(), false);
            return true;
          });
    return keep;
  }

  private MetisCasting() {}
}
