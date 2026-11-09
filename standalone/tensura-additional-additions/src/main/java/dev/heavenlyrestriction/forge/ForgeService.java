package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.*;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/**
 * Inventory mutations and synthesis are exclusively server-authoritative, session-bound actions.
 */
public final class ForgeService {
  public record Session(UUID id, String mode, long expires) {}

  private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

  public record Material(Item item, double quality, String name) {}

  public static List<Material> materials() {
    var hihi =
        BuiltInRegistries.ITEM
            .getOptional(ResourceLocation.parse("tensura:hihiirokane_ingot"))
            .orElse(Items.NETHERITE_INGOT);
    return List.of(
        new Material(Items.IRON_INGOT, .1, "Iron"),
        new Material(Items.DIAMOND, .2, "Diamond"),
        new Material(Items.NETHERITE_INGOT, .3, "Netherite"),
        new Material(hihi, .35, "Hihiirokane"));
  }

  public static boolean accessible(ServerPlayer p) {
    return p.isAlive()
        && !p.isSpectator()
        && (ForgeSynthesis.owned(p).filter(i -> i.canInteractSkill(p)).isPresent()
            || ForgeSynthesis.owned(p).isEmpty()
                && io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(p)
                    .getSkill(HeavenlyRestriction.HEPHAESTUS.get())
                    .isPresent());
  }

  public static void open(ServerPlayer p, String mode) {
    if (!accessible(p)) return;
    p.closeContainer();
    SESSIONS.put(p, new Session(UUID.randomUUID(), mode, p.level().getGameTime() + 1200));
    state(p, true, "");
  }

  private static CompoundTag stack(ItemStack s, ServerPlayer p) {
    return (CompoundTag) s.save(p.registryAccess());
  }

  public static void state(ServerPlayer p, boolean open, String message) {
    var session = SESSIONS.get(p);
    if (session == null) return;
    var t = new CompoundTag();
    t.putUUID("session", session.id);
    t.putBoolean("open", open);
    t.putString("mode", session.mode);
    t.putString("message", message);
    t.put("ledger", ForgeLedger.get(p.getServer()).snapshot());
    boolean learned = ForgeSynthesis.owned(p).isPresent();
    t.putBoolean("learned", learned);
    t.putBoolean("prospector", ForgeSynthesis.mastered(p, HeavenlyRestriction.PROSPECTOR.get()));
    t.putBoolean("craftsman", ForgeSynthesis.mastered(p, UniqueSkills.GODLY_CRAFTSMAN.get()));
    t.putBoolean("awakened", ForgeSynthesis.awakened(p));
    t.putBoolean("settled", ForgeSynthesis.settled(p));
    t.putBoolean("crafted", RestrictionData.of(p).getBoolean("hephaestus_forged_high_tier"));
    t.putBoolean("ready", ForgeSynthesis.ready(p));
    t.putDouble("cost", ForgeSynthesis.cost(p));
    t.putBoolean("affordable", ForgeSynthesis.affordable(p));
    t.putBoolean("deployed", ForgeArsenal.active(p));
    var items = new ListTag();
    for (int slot = 0; slot < p.getInventory().items.size(); slot++) {
      var s = p.getInventory().getItem(slot);
      if (!ForgeItems.equipment(s)) continue;
      var row = new CompoundTag();
      row.putInt("slot", slot);
      row.put("stack", stack(s, p));
      row.putString("name", s.getHoverName().getString());
      row.putBoolean("own", ForgeItems.own(p, s));
      row.putBoolean("weapon", ForgeItems.weapon(s));
      row.putBoolean("opus", ForgeItems.masterpiece(s));
      row.putInt("temper", ForgeItems.temper(s).ordinal());
      row.putDouble("quality", ForgeItems.strength(s));
      items.add(row);
    }
    t.put("items", items);
    var materials = new ListTag();
    for (var m : materials()) {
      var row = new CompoundTag();
      row.putString("name", m.name);
      row.putDouble("quality", m.quality);
      row.putInt("count", count(p, m.item));
      materials.add(row);
    }
    t.put("materials", materials);
    ForgeNetwork.send(p, t);
  }

  public static int count(ServerPlayer p, Item item) {
    int n = 0;
    for (var s : p.getInventory().items) if (s.is(item)) n += s.getCount();
    return n;
  }

  private static void consume(ServerPlayer p, Item item, int amount) {
    for (var s : p.getInventory().items) {
      if (!s.is(item)) continue;
      int use = Math.min(amount, s.getCount());
      s.shrink(use);
      amount -= use;
      if (amount == 0) return;
    }
  }

  public static double bill(ServerPlayer p, double base) {
    return SchemaArt.cost(p, base);
  }

  private static boolean canPay(ServerPlayer p, double cost) {
    return Double.isFinite(cost)
        && cost >= 0
        && TensuraStorages.getExistenceFrom(p).getMagicule() >= cost;
  }

  private static void pay(ServerPlayer p, double cost) {
    var ep = TensuraStorages.getExistenceFrom(p);
    ep.setMagicule(ep.getMagicule() - cost);
    ep.markDirty();
  }

  public static boolean work(ServerPlayer p, int slot, String action, int selected) {
    if (slot < 0 || slot >= p.getInventory().items.size() || ForgeSynthesis.owned(p).isEmpty())
      return false;
    var s = p.getInventory().getItem(slot);
    if (!ForgeItems.equipment(s)) return false;
    if (action.equals("deploy")) return ForgeArsenal.deploy(p, List.of(slot));
    int material = Math.floorMod(selected, 10), temper = selected / 10;
    if (material < 0 || material >= materials().size()) return false;
    var m = materials().get(material);
    boolean opus = action.equals("opus"), repair = action.equals("repair");
    if (!List.of("reforge", "temper", "opus", "repair").contains(action)) return false;
    if (!repair && ForgeItems.forged(s) && !ForgeItems.own(p, s)
        || opus && (!ForgeItems.own(p, s) || material == 0)) return false;
    if (action.equals("temper") && (temper < 1 || temper >= ForgeItems.Temper.values().length))
      return false;
    int quantity = opus ? 2 : 1;
    double cost = bill(p, opus ? 50000 : repair ? 500 : 2000);
    if (count(p, m.item) < quantity || !canPay(p, cost)) return false;
    // A worn native item keeps exactly its EP, max EP, engravings and Chromatic history.
    consume(p, m.item, quantity);
    pay(p, cost);
    if (repair) {
      int restored = (int) Math.ceil(s.getMaxDamage() * Math.min(1, m.quality * 3));
      s.setDamageValue(Math.max(0, s.getDamageValue() - restored));
      if (s.has(TensuraDataComponents.EP_DURABILITY.get())) {
        double max = s.getOrDefault(TensuraDataComponents.EP.get(), 0d),
            current = s.getOrDefault(TensuraDataComponents.EP_DURABILITY.get(), 0d);
        s.set(
            TensuraDataComponents.EP_DURABILITY.get(),
            Math.min(max, current + max * Math.min(1, m.quality * 3)));
      }
    } else {
      ForgeItems.stamp(p, s);
      var data = ForgeItems.data(s).copy();
      data.putBoolean("benefits", true);
      if (action.equals("temper")) {
        data.putInt("temper", temper);
        data.putDouble("quality", m.quality);
      }
      s.set(ForgeContent.WORK.get(), data);
      if (opus) {
        ForgeLedger.get(p.getServer()).designate(p.getUUID(), data.getUUID("work"));
        RestrictionData.of(p).putUUID("hephaestus_masterpiece", data.getUUID("work"));
        ForgeNetwork.ledgerAll(p);
      }
    }
    p.getInventory().setChanged();
    p.containerMenu.broadcastChanges();
    ForgeSynthesis.owned(p).ifPresent(i -> i.addMasteryPoint(p));
    return true;
  }

  public static void request(ServerPlayer p, UUID id, String action, int slot, int value) {
    var session = SESSIONS.get(p);
    if (session == null
        || !session.id.equals(id)
        || session.expires < p.level().getGameTime()
        || !accessible(p)) return;
    SESSIONS.put(p, new Session(id, session.mode, p.level().getGameTime() + 1200));
    if (action.equals("close")) {
      SESSIONS.remove(p);
      return;
    }
    if (action.equals("refresh")) {
      state(p, false, "");
      return;
    }
    if (action.equals("synthesize")) {
      boolean done = ForgeSynthesis.synthesize(p);
      if (done) SESSIONS.put(p, new Session(id, "forge", p.level().getGameTime() + 1200));
      state(
          p,
          false,
          done ? "Synthesis complete." : "Requirements are not met. Nothing was consumed.");
      return;
    }
    var learned = ForgeSynthesis.owned(p).filter(i -> i.canInteractSkill(p));
    if (learned.isEmpty()) return;
    var skill = HeavenlyRestriction.HEPHAESTUS.get();
    var i = learned.get();
    switch (action) {
      case "craft" -> {
        SESSIONS.remove(p);
        p.openMenu(
            new SimpleMenuProvider(
                (container, inv, player) ->
                    new CraftingMenu(container, inv, ContainerLevelAccess.NULL),
                Component.translatable("heavenlyrestriction.forge.title")));
      }
      case "refine" -> {
        SESSIONS.remove(p);
        skill.openRefiningMenu(p, i);
      }
      case "engrave" -> {
        SESSIONS.remove(p);
        skill.openEnchantingMenu(p, i);
      }
      case "storage" -> {
        SESSIONS.remove(p);
        skill.openSpatialStorage(p, i);
      }
      case "recover" -> {
        var list = i.getOrCreateTag().getList("ProspectorStorage", Tag.TAG_COMPOUND).copy();
        i.getOrCreateTag().remove("ProspectorStorage");
        i.markDirty();
        var oldStorage =
            new io.github.manasmods.tensura.menu.container.SpatialStorageContainer(20, 128);
        oldStorage.fromTag(list, p.registryAccess());
        for (int index = 0; index < oldStorage.getContainerSize(); index++) {
          var item = oldStorage.getItem(index);
          if (!item.isEmpty() && !p.getInventory().add(item)) p.drop(item, false);
        }
        state(p, false, "Previous refining ingredients returned.");
      }
      case "recall" -> {
        ForgeArsenal.recall(p);
        state(p, false, "Arsenal recalled.");
      }
      case "deploy_all" -> {
        var slots = new ArrayList<Integer>();
        for (int n = 0; n < p.getInventory().items.size() && slots.size() < 6; n++)
          if (ForgeItems.weapon(p.getInventory().getItem(n))) slots.add(n);
        boolean done = ForgeArsenal.deploy(p, slots);
        state(
            p,
            false,
            done ? "Living Arsenal deployed." : "Select a weapon and ensure enough magicules.");
      }
      default -> {
        boolean done = work(p, slot, action, value);
        state(
            p,
            false,
            done
                ? "Work completed."
                : "Check the selected item, materials and magicules. Nothing was consumed.");
      }
    }
  }

  public static void close(ServerPlayer p) {
    SESSIONS.remove(p);
  }

  private ForgeService() {}
}
