package dev.heavenlyrestriction;

import static dev.heavenlyrestriction.TestFixtures.*;

import dev.heavenlyrestriction.chromatic.*;
import dev.heavenlyrestriction.highrealm.*;
import dev.heavenlyrestriction.highrealm.boss.*;
import io.github.manasmods.tensura.enchantment.EngravingHelper;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class ChromaticGameTests {
  private static Holder.Reference<Enchantment> enchantment(GameTestHelper h, String id) {
    return h.getLevel()
        .registryAccess()
        .registryOrThrow(Registries.ENCHANTMENT)
        .getHolderOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(id)));
  }

  private static ItemStack sword(GameTestHelper h) {
    var stack = new ItemStack(Items.DIAMOND_SWORD);
    var engraving = enchantment(h, "tensura:crushing");
    stack.enchant(engraving, engraving.value().getMaxLevel());
    stack.setDamageValue(100);
    return stack;
  }

  private static int level(GameTestHelper h, ItemStack stack, String id) {
    return EnchantmentHelper.getItemEnchantmentLevel(enchantment(h, id), stack);
  }

  private static AnvilMenu anvil(ServerPlayer p, ItemStack left, ItemStack right) {
    var menu = new AnvilMenu(77, p.getInventory(), ContainerLevelAccess.NULL);
    menu.getSlot(0).set(left);
    menu.getSlot(1).set(right);
    menu.createResult();
    return menu;
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticRaisesAllEngravingsPastCapsButNeverCursesBlessingsOrVanilla(
      GameTestHelper h) {
    var p = player(h);
    var sword = sword(h);
    for (String id :
        List.of(
            "tensura:severance",
            "heavenlyrestriction:test_blessing",
            "heavenlyrestriction:test_tagged_benefit",
            "tensura:enervation",
            "tensura:tsukumogami",
            "minecraft:binding_curse",
            "minecraft:sharpness")) sword.enchant(enchantment(h, id), 1);
    var data = new CompoundTag();
    data.putString("kept", "equipment");
    sword.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    sword.set(
        DataComponents.CUSTOM_NAME,
        Component.literal("Keepsake")
            .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
    sword.set(TensuraDataComponents.EP.get(), 123456d);
    var original = sword.copy();
    var menu = anvil(p, sword, new ItemStack(ChromaticContent.ORB.get()));
    menu.setItemName("Keepsake");
    var result = menu.getSlot(2).getItem();
    h.assertTrue(
        !result.isEmpty() && ChromaticOrb.infused(result),
        "Native anvil did not offer an infused result");
    for (String id : List.of("tensura:crushing", "tensura:severance"))
      h.assertTrue(
          level(h, result, id) == level(h, original, id) + 1, "Engraving not raised: " + id);
    for (String id :
        List.of(
            "heavenlyrestriction:test_blessing",
            "heavenlyrestriction:test_tagged_benefit",
            "tensura:enervation",
            "tensura:tsukumogami",
            "minecraft:binding_curse",
            "minecraft:sharpness"))
      h.assertTrue(
          level(h, result, id) == level(h, original, id), "Protected enchantment changed: " + id);
    h.assertTrue(
        result.getDamageValue() == 100
            && result.get(TensuraDataComponents.EP.get()) == 123456d
            && result.get(DataComponents.CUSTOM_NAME).getString().equals("Keepsake")
            && result
                .get(DataComponents.CUSTOM_DATA)
                .copyTag()
                .getString("kept")
                .equals("equipment"),
        "Upgrade reset equipment data");
    h.assertTrue(ItemStack.isSameItemSameComponents(sword, original), "Preview mutated the input");
    menu.removed(p);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticConsumesOneOrbAndOneLevelOnlyOnTakeAndCannotRepeat(GameTestHelper h) {
    var p = player(h);
    p.experienceLevel = 10;
    var menu = anvil(p, sword(h), new ItemStack(ChromaticContent.ORB.get(), 3));
    h.assertTrue(
        menu.getSlot(1).getItem().getCount() == 3 && p.experienceLevel == 10,
        "Preview spent resources");
    menu.setItemName("Prismatic Edge");
    var output = menu.getSlot(2);
    h.assertTrue(output.mayPickup(p), "Valid survival result cannot be taken");
    var result = output.remove(1);
    output.onTake(p, result);
    h.assertTrue(
        menu.getSlot(0).getItem().isEmpty()
            && menu.getSlot(1).getItem().getCount() == 2
            && p.experienceLevel == 9,
        "Wrong actual anvil consumption");
    h.assertTrue(
        result.getHoverName().getString().equals("Prismatic Edge"), "Anvil rename not retained");
    var again = anvil(p, result, new ItemStack(ChromaticContent.ORB.get(), 2));
    h.assertTrue(
        again.getSlot(2).getItem().isEmpty() && again.getSlot(1).getItem().getCount() == 2,
        "Second orb allowed or spent");
    menu.removed(p);
    again.removed(p);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticRejectsNoopBooksProtectedOnlyAndConfiguredCurses(GameTestHelper h) {
    var p = player(h);
    var curse = new ItemStack(Items.DIAMOND_SWORD);
    curse.enchant(enchantment(h, "tensura:enervation"), 1);
    var blessing = new ItemStack(Items.DIAMOND_SWORD);
    blessing.enchant(enchantment(h, "heavenlyrestriction:test_tagged_benefit"), 1);
    var book = new ItemStack(Items.ENCHANTED_BOOK);
    EnchantmentHelper.setEnchantments(book, sword(h).get(DataComponents.ENCHANTMENTS));
    var maxed = sword(h);
    maxed.enchant(enchantment(h, "tensura:crushing"), 255);
    for (var input : List.of(new ItemStack(Items.DIAMOND_SWORD), curse, blessing, book, maxed)) {
      var menu = anvil(p, input, new ItemStack(ChromaticContent.ORB.get()));
      h.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Invalid/no-op input accepted: " + input);
      menu.removed(p);
    }
    var old = EngravingHelper.CONFIG.curseEngraving;
    try {
      var changed = new ArrayList<>(old);
      changed.add("tensura:crushing");
      EngravingHelper.CONFIG.curseEngraving = changed;
      h.assertTrue(
          ChromaticOrb.upgrade(sword(h)).isEmpty(), "Native configured curse list ignored");
    } finally {
      EngravingHelper.CONFIG.curseEngraving = old;
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticNormalAnvilRepairPreservesOvercapAndHistoryInBothSlots(
      GameTestHelper h) {
    var p = player(h);
    var boosted = ChromaticOrb.upgrade(sword(h));
    int expected = level(h, boosted, "tensura:crushing");
    for (boolean left : List.of(true, false)) {
      var fresh = sword(h);
      fresh.setDamageValue(200);
      var menu = anvil(p, left ? boosted.copy() : fresh, left ? fresh : boosted.copy());
      var result = menu.getSlot(2).getItem();
      h.assertTrue(
          !result.isEmpty() && ChromaticOrb.infused(result), "Repair washed infusion history");
      h.assertTrue(
          level(h, result, "tensura:crushing") == expected,
          "Repair capped or multiplied the enhanced engraving");
      h.assertTrue(ChromaticOrb.upgrade(result).isEmpty(), "Repair enabled another orb");
      menu.removed(p);
    }
    var renamed = anvil(p, boosted.copy(), ItemStack.EMPTY);
    renamed.setItemName("Remembered");
    h.assertTrue(
        ChromaticOrb.infused(renamed.getSlot(2).getItem())
            && level(h, renamed.getSlot(2).getItem(), "tensura:crushing") == expected,
        "Rename lost enhancement");
    renamed.removed(p);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticHistorySurvivesGrindstoneAndCraftingRepair(GameTestHelper h) {
    var p = player(h);
    var boosted = ChromaticOrb.upgrade(sword(h));
    var grind = new GrindstoneMenu(78, p.getInventory(), ContainerLevelAccess.NULL);
    grind.getSlot(0).set(sword(h));
    grind.getSlot(1).set(boosted.copy());
    var result = grind.getSlot(2).getItem();
    h.assertTrue(
        !result.isEmpty() && ChromaticOrb.infused(result),
        "Grindstone washed history from the second input");
    h.assertTrue(
        level(h, result, "tensura:crushing") == 0,
        "Grindstone no longer removes engravings normally");
    result.enchant(enchantment(h, "tensura:crushing"), 1);
    h.assertTrue(
        ChromaticOrb.upgrade(result).isEmpty(), "Re-engraving ground gear allowed another orb");
    var repair = new RepairItemRecipe(CraftingBookCategory.MISC);
    var crafted =
        repair.assemble(
            CraftingInput.of(2, 1, List.of(sword(h), boosted.copy())), p.registryAccess());
    h.assertTrue(
        !crafted.isEmpty() && ChromaticOrb.infused(crafted), "Crafting repair washed history");
    grind.removed(p);
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticEnhancementSurvivesSaveSyncSmithingAndNativeSpearInitialization(
      GameTestHelper h) {
    var p = player(h);
    var boosted = ChromaticOrb.upgrade(sword(h));
    var ops = p.registryAccess().createSerializationContext(NbtOps.INSTANCE);
    var saved = ItemStack.CODEC.encodeStart(ops, boosted).getOrThrow();
    var restored = ItemStack.CODEC.parse(ops, saved).getOrThrow();
    h.assertTrue(
        ItemStack.isSameItemSameComponents(boosted, restored),
        "Item save lost component or overcap levels");
    var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), p.registryAccess());
    try {
      ItemStack.STREAM_CODEC.encode(buffer, boosted);
      h.assertTrue(
          ItemStack.isSameItemSameComponents(boosted, ItemStack.STREAM_CODEC.decode(buffer)),
          "Network sync lost upgrade state");
    } finally {
      buffer.release();
    }
    var input =
        new SmithingRecipeInput(
            new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            boosted,
            new ItemStack(Items.NETHERITE_INGOT));
    var recipe =
        h.getLevel()
            .getRecipeManager()
            .getRecipeFor(RecipeType.SMITHING, input, h.getLevel())
            .orElseThrow();
    var evolved = recipe.value().assemble(input, p.registryAccess());
    h.assertTrue(
        evolved.is(Items.NETHERITE_SWORD)
            && ChromaticOrb.infused(evolved)
            && level(h, evolved, "tensura:crushing") == level(h, boosted, "tensura:crushing"),
        "Smithing lost upgrade");
    var spear = ChromaticOrb.upgrade(InvertedSpearItem.create(p.registryAccess()));
    InvertedSpearItem.engrave(spear, p.registryAccess());
    h.assertTrue(
        level(h, spear, "heavenlyrestriction:nullification") == 2 && ChromaticOrb.infused(spear),
        "Native spear setup reset its enhanced engraving");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticRecipeUsesAllArchetypesAndCoreAndKaelDropsOneCore(GameTestHelper h) {
    var ingredients =
        List.of(
            new ItemStack(ChromaticContent.SKIRMISHER_SHARD.get()),
            new ItemStack(ChromaticContent.STARLESS_CORE.get()),
            new ItemStack(ChromaticContent.BULWARK_SHARD.get()),
            new ItemStack(ChromaticContent.SENTINEL_SHARD.get()));
    var input = CraftingInput.of(2, 2, ingredients);
    var recipe =
        h.getLevel()
            .getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
            .orElseThrow();
    var result = recipe.value().assemble(input, h.getLevel().registryAccess());
    h.assertTrue(
        result.is(ChromaticContent.ORB.get()) && result.getCount() == 1,
        "Four ingredients did not craft one orb");
    var wrong =
        CraftingInput.of(
            2,
            2,
            List.of(
                ingredients.get(0), ingredients.get(0), ingredients.get(2), ingredients.get(3)));
    h.assertTrue(
        !recipe.value().matches(wrong, h.getLevel()),
        "Recipe accepted duplicate shards without Kael's core");
    var boss = WayfarerContent.KAEL.get().create(h.getLevel());
    boss.initialize();
    var params =
        new LootParams.Builder(h.getLevel())
            .withParameter(LootContextParams.THIS_ENTITY, boss)
            .withParameter(LootContextParams.ORIGIN, boss.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, boss.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
    var table =
        h.getLevel()
            .getServer()
            .reloadableRegistries()
            .getLootTable(
                ResourceKey.create(
                    Registries.LOOT_TABLE, HeavenlyRestriction.id("entities/kael_veyr")));
    int cores =
        table.getRandomItems(params).stream()
            .filter(s -> s.is(ChromaticContent.STARLESS_CORE.get()))
            .mapToInt(ItemStack::getCount)
            .sum();
    h.assertTrue(cores == 1, "Kael loot table did not drop exactly one Starless Core");
    boss.discard();
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void chromaticEliteDropsMatchThreeKindsAndRequireCreditAndUnownedElites(
      GameTestHelper h) {
    var p = player(h);
    var realm = HighRealmGameTests.realm(h);
    for (String id : List.of("tensura:orc", "tensura:greater_daemon", "tensura:blade_tiger")) {
      var mob = (Mob) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)).create(realm);
      mob.getPersistentData().putBoolean(HighRealmElites.MARKER, true);
      mob.getRandom().setSeed(913);
      var expected = ChromaticContent.shard(HighRealmElites.kind(mob));
      int count = 0;
      for (int n = 0; n < 800; n++) {
        var drops = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(
            new LivingDropsEvent(mob, p.damageSources().playerAttack(p), drops, true));
        for (var drop : drops)
          for (var kind : HighRealmElites.Kind.values())
            if (drop.getItem().is(ChromaticContent.shard(kind))) {
              h.assertTrue(drop.getItem().is(expected), "Wrong archetype shard dropped");
              count += drop.getItem().getCount();
            }
      }
      h.assertTrue(count > 140 && count < 260, "Shard rate not near 25%: " + count);
      TensuraStorages.getExistenceFrom(mob).setPermanentOwner(p.getUUID());
      for (int n = 0; n < 40; n++) {
        var drops = new ArrayList<ItemEntity>();
        NeoForge.EVENT_BUS.post(
            new LivingDropsEvent(mob, p.damageSources().playerAttack(p), drops, true));
        h.assertTrue(
            drops.stream().noneMatch(d -> d.getItem().is(expected)),
            "Owned subordinate dropped a shard");
      }
      TensuraStorages.getExistenceFrom(mob).setPermanentOwner(null);
      var uncredited = new ArrayList<ItemEntity>();
      NeoForge.EVENT_BUS.post(
          new LivingDropsEvent(mob, mob.damageSources().generic(), uncredited, false));
      h.assertTrue(
          uncredited.stream().noneMatch(d -> d.getItem().is(expected)),
          "Uncredited death dropped a shard");
      mob.getPersistentData().remove(HighRealmElites.MARKER);
      var ordinary = new ArrayList<ItemEntity>();
      NeoForge.EVENT_BUS.post(
          new LivingDropsEvent(mob, p.damageSources().playerAttack(p), ordinary, true));
      h.assertTrue(
          ordinary.stream().noneMatch(d -> d.getItem().is(expected)),
          "Ordinary mob dropped a shard");
      mob.discard();
    }
    h.succeed();
  }
}
