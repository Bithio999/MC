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
  private static final String ITEMS = "hephaestus_arsenal";

  private record Weapon(LivingEntity owner, ItemStack item) {}

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
    return !items(p).isEmpty();
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

  public static boolean deploy(ServerPlayer p, List<Integer> slots) {
    if (active(p)
        || ForgeSynthesis.owned(p).filter(i -> i.canInteractSkill(p)).isEmpty()
        || slots.isEmpty()
        || slots.size() > 6
        || new HashSet<>(slots).size() != slots.size()) return false;
    for (int slot : slots)
      if (slot < 0
          || slot >= p.getInventory().items.size()
          || !ForgeItems.weapon(p.getInventory().getItem(slot))
          || p.getInventory().getItem(slot).getCount() != 1) return false;
    double cost = ForgeService.bill(p, 1000 * slots.size());
    var ep = TensuraStorages.getExistenceFrom(p);
    if (ep.getMagicule() < cost) return false;
    var list = new ListTag();
    for (int slot : slots) {
      var entry = new CompoundTag();
      entry.put("stack", p.getInventory().getItem(slot).save(p.registryAccess()));
      list.add(entry);
    }
    RestrictionData.of(p).put(ITEMS, list);
    for (int slot : slots) p.getInventory().setItem(slot, ItemStack.EMPTY);
    ep.setMagicule(ep.getMagicule() - cost);
    ep.markDirty();
    p.getInventory().setChanged();
    for (int n = 0; n < list.size(); n++) ArsenalBlade.spawn(p, n, list.size());
    p.serverLevel()
        .playSound(
            null, p.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, .8f, .7f);
    return true;
  }

  public static void recall(ServerPlayer p) {
    var saved = items(p).copy();
    RestrictionData.of(p).remove(ITEMS);
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
        double cost = ForgeService.bill(p, live * 10);
        var ep = TensuraStorages.getExistenceFrom(p);
        if (live == 0 || i.isEmpty() || ep.getMagicule() < cost) recall(p);
        else {
          ep.setMagicule(ep.getMagicule() - cost);
          ep.markDirty();
        }
      }
    }
  }

  public static boolean hit(ServerPlayer p, LivingEntity target, int index, ArsenalBlade blade) {
    var item = item(p, index);
    var skill = ForgeSynthesis.owned(p);
    if (item.isEmpty() || skill.isEmpty() || !RestrictionData.canHarm(p, target)) return false;
    DamageSource source =
        new DamageSource(
            p.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.PLAYER_ATTACK),
            blade,
            p) {
          @Override
          public ItemStack getWeaponItem() {
            return item;
          }
        };
    var tagged = (TensuraDamageSource) source;
    tagged.tensura$setAbilityInstance(skill.get());
    tagged.tensura$setAbilityMode(1);
    tagged.tensura$setSkillType(io.github.manasmods.tensura.ability.skill.Skill.SkillType.ULTIMATE);
    ((IndependentHitSource) source).restriction$independentHit();
    var previous = CURRENT.get();
    CURRENT.set(new Weapon(p, item));
    try (var offense = ForgeOffenseContext.open(p, item)) {
      float damage = (float) ForgeItems.weaponDamage(item, p, target, source);
      boolean hit = target.hurt(source, damage);
      if (hit) {
        TensuraEnchantmentHelper.doAdditionalAfterAttack(
            p.serverLevel(), target, p, source, item, damage);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(p.serverLevel(), target, source, item);
        TensuraEnchantmentHelper.doAdditionalAfterDamage(
            p.serverLevel(), target, p, source, item, damage);
        item.hurtAndBreak(1, p.serverLevel(), p, broken -> {});
        skill.get().addMasteryPoint(p);
        p.serverLevel()
            .sendParticles(
                ParticleTypes.ENCHANTED_HIT,
                target.getX(),
                target.getY() + target.getBbHeight() * .5,
                target.getZ(),
                7,
                .25,
                .3,
                .25,
                .04);
      }
      store(p, index, item);
      blade.visual(item);
      return hit;
    } finally {
      if (previous == null) CURRENT.remove();
      else CURRENT.set(previous);
    }
  }

  private ForgeArsenal() {}
}
