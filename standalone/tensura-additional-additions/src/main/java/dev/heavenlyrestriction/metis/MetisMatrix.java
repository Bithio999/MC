package dev.heavenlyrestriction.metis;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.skill.api.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** The skill instance owns its matrix; requests never grant or clone spells. */
public final class MetisMatrix {
  public static final String SLOTS = "MetisMatrix",
      AUTO = "MetisAutonomous",
      OPTIMAL = "MetisOptimal";

  public record Slot(
      int index, ResourceLocation id, int mode, int priority, boolean currentTarget) {}

  public static Optional<ManasSkillInstance> owned(LivingEntity e) {
    return SkillAPI.getSkillsFrom(e)
        .getSkill(HeavenlyRestriction.METIS.get())
        .filter(i -> i.getMastery() >= 0);
  }

  public static boolean active(LivingEntity e) {
    return owned(e).filter(i -> i.isToggled() && i.canInteractSkill(e)).isPresent();
  }

  public static int capacity(LivingEntity e, ManasSkillInstance i) {
    return i.isMastered(e) ? 4 : 3;
  }

  public static boolean optimal(ManasSkillInstance i) {
    return !i.getOrCreateTag().contains(OPTIMAL) || i.getOrCreateTag().getBoolean(OPTIMAL);
  }

  public static List<Slot> slots(LivingEntity e, ManasSkillInstance i) {
    var result = new ArrayList<Slot>();
    var indices = new HashSet<Integer>();
    var skills = new HashSet<ResourceLocation>();
    for (var t : i.getOrCreateTag().getList(SLOTS, Tag.TAG_COMPOUND)) {
      var row = (CompoundTag) t;
      var id = ResourceLocation.tryParse(row.getString("id"));
      int index = row.getInt("slot"), mode = row.getInt("mode");
      if (id == null
          || index < 0
          || index >= capacity(e, i)
          || !indices.add(index)
          || !skills.add(id)) continue;
      if (SkillAPI.getSkillsFrom(e)
          .getSkill(id)
          .filter(s -> MetisAbilities.allowed(e, s, mode))
          .isEmpty()) continue;
      result.add(
          new Slot(
              index, id, mode, Math.clamp(row.getInt("priority"), 0, 2), row.getBoolean("target")));
    }
    result.sort(Comparator.comparingInt(Slot::index));
    return result;
  }

  public static boolean assign(
      ServerPlayer p, int index, String id, int mode, int priority, boolean target) {
    var mind = owned(p).filter(i -> i.canInteractSkill(p) && i.canActivateSkill(p, 0));
    if (mind.isEmpty()
        || index < 0
        || index >= capacity(p, mind.get())
        || priority < 0
        || priority > 2) return false;
    var key = ResourceLocation.tryParse(id);
    if (!id.isEmpty()
        && (key == null
            || SkillAPI.getSkillsFrom(p)
                .getSkill(key)
                .filter(i -> MetisAbilities.allowed(p, i, mode))
                .isEmpty())) return false;
    if (id.isEmpty() && mode != 0) return false;
    var list = mind.get().getOrCreateTag().getList(SLOTS, Tag.TAG_COMPOUND).copy();
    for (var t : list) {
      var row = (CompoundTag) t;
      if (row.getInt("slot") != index && !id.isEmpty() && row.getString("id").equals(id))
        return false;
    }
    list.removeIf(t -> ((CompoundTag) t).getInt("slot") == index);
    if (!id.isEmpty()) {
      var row = new CompoundTag();
      row.putInt("slot", index);
      row.putString("id", id);
      row.putInt("mode", mode);
      row.putInt("priority", priority);
      row.putBoolean("target", target);
      list.add(row);
    }
    MetisCasting.cancel(p, index);
    mind.get().getOrCreateTag().put(SLOTS, list);
    mind.get().markDirty();
    SkillAPI.getSkillsFrom(p).markDirty();
    return true;
  }

  public static double costFactor(LivingEntity e) {
    return owned(e).map(i -> i.isMastered(e) ? .6 : .7).orElse(1d);
  }

  private MetisMatrix() {}
}
