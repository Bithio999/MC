package dev.heavenlyrestriction.mentor;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.chromatic.ChromaticContent;
import dev.heavenlyrestriction.worldhop.WorldHop;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.manascore.storage.api.StorageHolder;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;

public final class MentorService {
  public static final TagKey<ManasSkill> EXCLUDED =
      TagKey.create(SkillAPI.getSkillRegistryKey(), HeavenlyRestriction.id("mentor_excluded"));
  private static final Set<UUID> BUSY = new HashSet<>();

  public static boolean eligible(ServerPlayer p, ManasSkill s) {
    return s instanceof Skill skill
        && skill.getType() == Skill.SkillType.UNIQUE
        && !s.createDefaultInstance().is(EXCLUDED)
        && WorldHop.mayStore(s.createDefaultInstance(), p);
  }

  public static boolean owned(ServerPlayer p, ManasSkill s) {
    return SkillAPI.getSkillsFrom(p)
        .getSkill(s)
        .filter(i -> i.getMastery() >= 0 && i.getRemoveTime() != -3)
        .isPresent();
  }

  public static boolean learnable(ServerPlayer p, ManasSkill s) {
    var i = s.createDefaultInstance();
    i.setMastery(0);
    return eligible(p, s) && !owned(p, s) && ChainedState.canLearn(p, i);
  }

  public static List<ManasSkill> catalog(ServerPlayer p) {
    var list = new ArrayList<ManasSkill>();
    for (var s : SkillAPI.getSkillRegistry()) if (eligible(p, s)) list.add(s);
    list.sort(
        Comparator.comparing(
                (ManasSkill s) -> s.getName().getString(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(s -> s.getRegistryName().toString()));
    return list;
  }

  public static int orbSlot(ServerPlayer p) {
    for (int n = 0; n < p.getInventory().getContainerSize(); n++)
      if (p.getInventory().getItem(n).is(ChromaticContent.ORB.get())) return n;
    return -1;
  }

  public static double reserves(ServerPlayer p) {
    return p.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue()
        + p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
  }

  public static boolean affordable(ServerPlayer p) {
    double reserve = reserves(p);
    return Double.isFinite(reserve)
        && p.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue() >= 0
        && p.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue() >= 0
        && reserve > MentorConfig.COST.get()
        && orbSlot(p) >= 0;
  }

  public static String purchase(ServerPlayer p, ResourceLocation id) {
    if (!p.isAlive() || p.isSpectator() || !MentorLedger.reconcile(p).isEmpty()) return "used";
    var s = SkillAPI.getSkillRegistry().get(id);
    if (s == null || !learnable(p, s)) return "unavailable";
    if (!affordable(p)) return "cost";
    if (!BUSY.add(p.getUUID())) return "unavailable";
    try {
      int orb = orbSlot(p);
      double cost = MentorConfig.COST.get();
      var aura = p.getAttribute(TensuraAttributes.MAX_AURA);
      var magic = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
      double total = reserves(p),
          auraCost = cost * aura.getBaseValue() / total,
          magicCost = cost - auraCost;
      var granted = s.createDefaultInstance();
      granted.setMastery(0);
      granted.getOrCreateTag().putBoolean("NoMagiculeCost", true);
      if (!SkillHelper.learnSkill(p, granted)) return "unavailable";
      if (!owned(p, s)) return "unavailable";
      aura.setBaseValue(Math.max(0, aura.getBaseValue() - auraCost));
      magic.setBaseValue(Math.max(0, magic.getBaseValue() - magicCost));
      p.getInventory().getItem(orb).shrink(1);
      p.getInventory().setChanged();
      var energy = TensuraStorages.getExistenceFrom(p);
      energy.setAura(Math.min(energy.getAura(), EnergyHelper.getMaxAura(p)));
      energy.setMagicule(Math.min(energy.getMagicule(), EnergyHelper.getMaxMagicule(p)));
      energy.markDirty();
      MentorLedger.record(p, id.toString());
      p.inventoryMenu.broadcastChanges();
      ((StorageHolder) p).manasCore$sync();
      return "success";
    } finally {
      BUSY.remove(p.getUUID());
    }
  }

  private MentorService() {}
}
