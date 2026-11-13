package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.*;
import io.github.manasmods.tensura.damage.TensuraDamageSource;
import io.github.manasmods.tensura.enchantment.TensuraEnchantmentHelper;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Real items live in the player's saved escrow, never in disposable visual entities or copied
 * skills.
 */
public final class ForgeArsenal {
  private static final Map<ServerPlayer, ArsenalSequence> SEQUENCES = new WeakHashMap<>();
  private static final String ITEMS = "hephaestus_arsenal";

  private static final class Weapon {
    final LivingEntity owner;
    final int index;
    final Weapon previous;
    ItemStack item;

    Weapon(LivingEntity owner, int index, ItemStack item) {
      this.owner = owner;
      this.index = index;
      this.item = item;
      this.previous = CURRENT.get();
    }
  }

  private static final ThreadLocal<Weapon> CURRENT = new ThreadLocal<>();

  public static ItemStack weapon(LivingEntity owner) {
    var w = CURRENT.get();
    return w != null && w.owner == owner ? w.item : owner.getMainHandItem();
  }

  public static LivingEntity wielder(DamageSource source) {
    var w = CURRENT.get();
    return w != null
            && source.getDirectEntity() instanceof ArsenalBlade
            && source.getEntity() == w.owner
        ? w.owner
        : null;
  }

  public static ItemStack vesper(LivingEntity e) {
    if (e instanceof ServerPlayer p)
      for (int n = 0; n < items(p).size(); n++) {
        var item = item(p, n);
        if (item.getItem() instanceof dev.heavenlyrestriction.vesper.VesperItem) return item;
      }
    return ItemStack.EMPTY;
  }

  public static boolean hasVesper(LivingEntity e) {
    return !vesper(e).isEmpty();
  }

  public static boolean active(ServerPlayer p) {
    return count(p) > 0;
  }

  public static int[] roster(ServerPlayer p) {
    var saved = items(p);
    return java.util.stream.IntStream.range(0, saved.size())
        .filter(n -> !saved.getCompound(n).getCompound("stack").isEmpty())
        .toArray();
  }

  public static int count(ServerPlayer p) {
    return roster(p).length;
  }

  public static boolean launch(ServerPlayer p, int index) {
    return SEQUENCES
        .computeIfAbsent(p, ignored -> new ArsenalSequence())
        .launch(index, p.getServer().getTickCount(), roster(p));
  }

  private static ListTag items(ServerPlayer p) {
    return RestrictionData.of(p).getList(ITEMS, Tag.TAG_COMPOUND);
  }

  public static ItemStack item(ServerPlayer p, int index) {
    var list = items(p);
    return index < 0 || index >= list.size()
        ? ItemStack.EMPTY
        : ItemStack.parseOptional(p.registryAccess(), list.getCompound(index).getCompound("stack"));
  }

  private static void store(ServerPlayer p, int index, ItemStack item) {
    var list = items(p).copy();
    if (index < 0 || index >= list.size()) return;
    var entry = list.getCompound(index).copy();
    entry.put("stack", item.saveOptional(p.registryAccess()));
    list.set(index, entry);
    RestrictionData.of(p).put(ITEMS, list);
  }

  public static void bind(ServerPlayer p, int index, ArsenalBlade blade) {
    var list = items(p).copy();
    if (index < 0 || index >= list.size()) return;
    var row = list.getCompound(index).copy();
    row.putUUID("entity", blade.getUUID());
    list.set(index, row);
    RestrictionData.of(p).put(ITEMS, list);
  }

  public static boolean current(ServerPlayer p, int index, ArsenalBlade blade) {
    var list = items(p);
    return index >= 0
        && index < list.size()
        && list.getCompound(index).hasUUID("entity")
        && list.getCompound(index).getUUID("entity").equals(blade.getUUID());
  }

  public static void gainGearEP(ServerPlayer player, double totalEP) {
    if (!active(player) || !Double.isFinite(totalEP) || totalEP <= 0) return;
    for (int index : roster(player)) {
      var hit = CURRENT.get();
      while (hit != null && (hit.owner != player || hit.index != index)) hit = hit.previous;
      boolean striking = hit != null;
      ItemStack before = striking ? hit.item : item(player, index);
      if (before.isEmpty()) continue;
      // Older deployments may predate native initialization; this is idempotent.
      io.github.manasmods.tensura.handler.GearHandler.initiateGearExistence(player.level(), before);
      var grown = ForgeGearGrowth.award(player, before, totalEP);
      io.github.manasmods.tensura.handler.GearHandler.updateTsukumogami(player, grown);
      if (striking) hit.item = grown;
      store(player, index, grown);
      var row = items(player).getCompound(index);
      if (row.hasUUID("entity")
          && player.serverLevel().getEntity(row.getUUID("entity")) instanceof ArsenalBlade blade)
        blade.visual(grown);
    }
  }

  public static boolean deploy(ServerPlayer p, List<Integer> slots) {
    if (ForgeSynthesis.owned(p).filter(i -> i.canInteractSkill(p)).isEmpty()
        || slots.isEmpty()
        || count(p) + slots.size() > 6
        || new HashSet<>(slots).size() != slots.size()) return false;
    for (int slot : slots)
      if (slot < 0
          || slot >= p.getInventory().items.size()
          || !ForgeItems.weapon(p.getInventory().getItem(slot))
          || p.getInventory().getItem(slot).getCount() != 1) return false;
    var ep = TensuraStorages.getExistenceFrom(p);
    try(var equations=dev.heavenlyrestriction.laplace.expand.ExpandedRuntime.open(p,ForgeSynthesis.owned(p).orElseThrow(),1)){if(io.github.manasmods.tensura.util.EnergyHelper.isOutOfEnergy(p,0,1000*slots.size()))return false;}
    var list = items(p).copy();
    var added = new ArrayList<Integer>();
    for (int slot : slots) {
      var entry = new CompoundTag();
      var weapon = p.getInventory().getItem(slot);
      io.github.manasmods.tensura.handler.GearHandler.initiateGearExistence(p.level(), weapon);
      io.github.manasmods.tensura.handler.GearHandler.updateTsukumogami(p, weapon);
      entry.put("stack", weapon.save(p.registryAccess()));
      int index = 0;
      while (index < list.size() && !list.getCompound(index).getCompound("stack").isEmpty())
        index++;
      if (index < list.size()) list.set(index, entry);
      else list.add(entry);
      added.add(index);
    }
    RestrictionData.of(p).put(ITEMS, list);
    for (int slot : slots) p.getInventory().setItem(slot, ItemStack.EMPTY);
    p.getInventory().setChanged();
    for (int n : added) ArsenalBlade.spawn(p, n, count(p));
    p.serverLevel()
        .playSound(
            null, p.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, .8f, .7f);
    return true;
  }

  public static boolean recall(ServerPlayer p, int index) {
    var stack = item(p, index);
    if (stack.isEmpty()) return false;
    var saved = items(p).copy();
    var row = saved.getCompound(index);
    saved.set(index, new CompoundTag());
    RestrictionData.of(p).put(ITEMS, saved);
    if (row.hasUUID("entity")) {
      var e = p.serverLevel().getEntity(row.getUUID("entity"));
      if (e instanceof ArsenalBlade) e.discard();
    }
    if (!p.getInventory().add(stack)) p.drop(stack, false);
    p.getInventory().setChanged();
    if (!active(p)) SEQUENCES.remove(p);
    return true;
  }

  public static void recall(ServerPlayer p) {
    var saved = items(p).copy();
    RestrictionData.of(p).remove(ITEMS);
    SEQUENCES.remove(p);
    // Clear escrow before returning anything, so repeated callbacks cannot return it twice.
    for (var tag : saved) {
      var item =
          ItemStack.parseOptional(p.registryAccess(), ((CompoundTag) tag).getCompound("stack"));
      if (!item.isEmpty() && !p.getInventory().add(item)) p.drop(item, false);
    }
    for (var level : p.getServer().getAllLevels())
      for (var blade : level.getEntitiesOfClass(ArsenalBlade.class, p.getBoundingBox().inflate(96)))
        if (blade.ownerId().equals(p.getUUID())) blade.discard();
    if (!saved.isEmpty()) p.getInventory().setChanged();
  }

  public static void tick(PlayerTickEvent.Post event) {
    if (!(event.getEntity() instanceof ServerPlayer p)) return;
    if (p.tickCount % 20 == 0) {
      ForgeSynthesis.discover(p);
      if (active(p)) {
        int live = 0;
        var saved = items(p);
        for (int n = 0; n < saved.size(); n++)
          if (!item(p, n).isEmpty()) {
            live++;
            var row = saved.getCompound(n);
            var entity =
                row.hasUUID("entity") ? p.serverLevel().getEntity(row.getUUID("entity")) : null;
            if (!(entity instanceof ArsenalBlade) || entity.distanceToSqr(p) > 64 * 64) {
              if (entity != null) entity.discard();
              ArsenalBlade.spawn(p, n, saved.size());
            }
          }
        var i = ForgeSynthesis.owned(p).filter(s -> s.canInteractSkill(p));
        if(live==0||i.isEmpty())recall(p);else try(var equations=dev.heavenlyrestriction.laplace.expand.ExpandedRuntime.open(p,i.get(),1)){if(io.github.manasmods.tensura.util.EnergyHelper.isOutOfEnergy(p,0,live*10))recall(p);}
      }
    }
  }

  public static boolean hit(ServerPlayer p, LivingEntity target, int index, ArsenalBlade blade) {
    var item = item(p, index);
    var skill = ForgeSynthesis.owned(p);
    if (item.isEmpty() || skill.isEmpty() || !RestrictionData.canHarm(p, target)) return false;
    var context = new Weapon(p, index, item);
    DamageSource source =
        new DamageSource(
            p.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.PLAYER_ATTACK),
            blade,
            p) {
          @Override
          public ItemStack getWeaponItem() {
            return context.item;
          }
        };
    var tagged = (TensuraDamageSource) source;
    tagged.tensura$setAbilityInstance(skill.get());
    tagged.tensura$setAbilityMode(1);
    tagged.tensura$setSkillType(io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE);
    ((IndependentHitSource) source).restriction$independentHit();
    var previous = CURRENT.get();
    CURRENT.set(context);
    try (var offense = ForgeOffenseContext.open(p, item)) {
      float damage = (float) (ForgeItems.weaponDamage(item,p,target,source)*dev.heavenlyrestriction.laplace.expand.AddonParameters.value(p,"hephaestus",1,"arsenal_damage",1));
      boolean hit = target.hurt(source, damage);
      if (hit) {
        TensuraEnchantmentHelper.doAdditionalAfterAttack(
            p.serverLevel(), target, p, source, context.item, damage);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(
            p.serverLevel(), target, source, context.item);
        TensuraEnchantmentHelper.doAdditionalAfterDamage(
            p.serverLevel(), target, p, source, context.item, damage);
        context.item.hurtAndBreak(1, p.serverLevel(), p, broken -> {});
        skill.get().addMasteryPoint(p);
        p.serverLevel()
            .sendParticles(
                ParticleTypes.ENCHANTED_HIT,
                target.getX(),
                target.getY() + target.getBbHeight() * .5,
                target.getZ(),
                2,
                .25,
                .3,
                .25,
                .04);
      }
      store(p, index, context.item);
      blade.visual(context.item);
      return hit;
    } finally {
      if (previous == null) CURRENT.remove();
      else CURRENT.set(previous);
    }
  }

  private ForgeArsenal() {}
}
