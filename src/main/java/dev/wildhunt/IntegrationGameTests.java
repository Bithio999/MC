package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class IntegrationGameTests {
    private static net.minecraft.server.level.ServerPlayer testPlayer(GameTestHelper helper) {
        // NeoForge fake connections safely discard payloads; vanilla mocks do not
        // negotiate ManasCore's custom network channels.
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "WildHuntTest"));
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeRegistrationAndHumanBranch(GameTestHelper helper) {
        var player = testPlayer(helper);
        var races = RaceAPI.getRaceFrom(player);
        helper.assertTrue(races.setRace(TensuraRaces.HUMAN.get(), false), "Human race could not be set");
        var human = races.getRace().orElseThrow();
        helper.assertTrue(human.getNextEvolutions(player).contains(WildHunt.HUNTERS.getFirst().get()), "Human branch mixin is missing");
        for (Apex apex : Apex.values()) {
            helper.assertTrue(SkillAPI.getSkillRegistry().get(WildHunt.id(apex.uniqueId)) == WildHunt.UNIQUE.get(apex).get(), "Unique skill not registered");
            helper.assertTrue(SkillAPI.getSkillRegistry().get(WildHunt.id(apex.ultimateId)) == WildHunt.ULTIMATE.get(apex).get(), "Ultimate skill not registered");
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeEvolutionAndSkillStorage(GameTestHelper helper) {
        var player = testPlayer(helper);
        var races = RaceAPI.getRaceFrom(player);
        races.setRace(TensuraRaces.HUMAN.get(), false);
        EnergyHelper.setBaseMaxEP(player, 100_000);
        HunterData.recordBossKill(player, net.minecraft.resources.ResourceLocation.parse("minecraft:wither"));
        var human = races.getRace().orElseThrow();
        helper.assertTrue(human.getEvolutionProgress(player, WildHunt.HUNTERS.getFirst().get()) >= 100, "First evolution never reaches 100%");
        helper.assertTrue(races.evolveRace(WildHunt.HUNTERS.getFirst().get()), "Evolution rejected");
        helper.assertTrue(HunterData.stage(player) == 1, "Race storage did not update");
        var skills = SkillAPI.getSkillsFrom(player);
        var unique = WildHunt.UNIQUE.get(Apex.UTH_DUNA).get();
        helper.assertTrue(HunterData.learnReward(player, unique), "Unique skill could not be learned");
        var instance = skills.getSkill(unique).orElseThrow();
        helper.assertTrue(instance.getModes() == 2, "Unique must have two active modes");
        helper.assertTrue(WildHunt.MYTHIC.get(MythicKind.DEMETER).get().createDefaultInstance().getModes() == 3, "Ultimate must have three active modes");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void waterToggleUsesNativeBarriersWithoutAbsorption(GameTestHelper helper) {
        var player = combatPlayer(helper); var skill = WildHunt.UNIQUE.get(Apex.UTH_DUNA).get();
        HunterData.learnReward(player, skill); var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
        var physical = player.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.PHYSICAL_BARRIER);
        var magic = player.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAGIC_BARRIER);
        double p = physical.getValue(), m = magic.getValue(), aura = TensuraStorages.getExistenceFrom(player).getAura();
        instance.setToggled(true); instance.onToggleOn(player);
        helper.assertTrue(instance.isToggled() && instance.canBeToggled(player), "Native passive toggle rejected");
        helper.assertTrue(physical.getValue() == p + 18 && magic.getValue() == m + 18, "Native barriers were not applied");
        helper.assertTrue(!player.hasEffect(MobEffects.ABSORPTION) && player.getAbsorptionAmount() == 0, "Passive still uses absorption hearts");
        helper.assertTrue(TensuraStorages.getExistenceFrom(player).getAura() == aura - skill.balance.upkeep(false), "Initial passive upkeep not consumed");
        instance.setToggled(false); instance.onToggleOff(player);
        helper.assertTrue(physical.getValue() == p && magic.getValue() == m, "Toggle left barrier modifiers behind");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void combinationAscensionConsumesTwoMasteredUniquesAndKeepsOthers(GameTestHelper helper) throws Exception {
        var player = testPlayer(helper);
        var skills = SkillAPI.getSkillsFrom(player);
        RaceAPI.getRaceFrom(player).setRace(WildHunt.HUNTERS.get(2).get(), false);
        EnergyHelper.setBaseMaxEP(player, 2_000_000);
        var source = WildHunt.UNIQUE.get(Apex.REY_DAU).get();
        var other = WildHunt.UNIQUE.get(Apex.UTH_DUNA).get();
        var second=WildHunt.UNIQUE.get(Apex.NU_UDRA).get();HunterData.learnReward(player,second);skills.getSkill(second).orElseThrow().setMastery(second.getMaxMastery());
        helper.assertTrue(HunterData.learnReward(player, source), "Source skill reward rejected");
        helper.assertTrue(HunterData.learnReward(player, other), "Other skill reward rejected");
        var instance = skills.getSkill(source).orElseThrow();
        instance.setMastery(instance.getMaxMastery());
        var commands = helper.getLevel().getServer().getCommands();
        commands.getDispatcher().execute("wildhunt ascend vritra", player.createCommandSourceStack());
        helper.assertTrue(skills.getSkill(WildHunt.MYTHIC.get(MythicKind.VRITRA).get()).isPresent(), "Ultimate not learned");
        helper.assertTrue(skills.getSkill(source).isEmpty(), "Source Unique was not replaced");
        helper.assertTrue(skills.getSkill(other).isPresent(), "Unrelated Unique was removed");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void waterBurstDamagesAndDrenchesEnemy(GameTestHelper helper) {
        var player = testPlayer(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        RaceAPI.getRaceFrom(player).setRace(WildHunt.HUNTERS.getFirst().get(), false);
        TensuraStorages.getExistenceFrom(player).setAura(10_000);
        var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 3, 1, 1);
        float health = zombie.getHealth();
        var skill = WildHunt.UNIQUE.get(Apex.UTH_DUNA).get();
        HunterData.learnReward(player, skill);
        var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
        var placement = ApexArts.cursorPlacement(helper.getLevel(), player, 24);
        zombie.setPos(placement.add(1, 0, 0));
        skill.onPressed(instance, player, 0, 1);
        helper.assertTrue(zombie.getHealth() < health, "Water damage did not reach the enemy");
        helper.assertTrue(HunterData.of(zombie).getLong("drenched_until") > helper.getLevel().getGameTime(), "Drench mark missing");
        helper.assertTrue(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Undertow slow missing");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void onlyDistinctBossTypesCountAndDoNotGrantSkills(GameTestHelper helper) {
        var player = testPlayer(helper);
        RaceAPI.getRaceFrom(player).setRace(TensuraRaces.HUMAN.get(), false);
        var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 2, 1, 2);
        zombie.setHealth(1);
        zombie.hurt(player.damageSources().playerAttack(player), 100_000);
        helper.assertTrue(HunterData.bossKills(player).isEmpty(), "Ordinary hostile counted as a boss");
        for (int i = 0; i < 2; i++) {
            var wither = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.WITHER, 2, 1, 2);
            wither.setHealth(1);
            wither.hurt(player.damageSources().playerAttack(player), 100_000);
        }
        helper.assertTrue(HunterData.bossKills(player).size() == 1, "Repeated Withers counted more than once");
        helper.assertTrue(HunterData.bossKills(player).contains("minecraft:wither"), "Wither credit missing");
        helper.assertTrue(HunterData.recordBossKill(player, WildHunt.id("future_monster")), "A second boss ID did not count");
        helper.assertTrue(HunterData.bossKills(player).size() == 2, "Distinct boss count did not increase");
        for (Apex apex : Apex.values()) helper.assertTrue(
            SkillAPI.getSkillsFrom(player).getSkill(WildHunt.UNIQUE.get(apex).get()).isEmpty(), "Kills still award a Unique skill");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeReincarnationPoolAndRollIncludeApexUniques(GameTestHelper helper) {
        var config = io.github.manasmods.tensura.menu.ReincarnationMenu.CONFIG.Skills;
        var original = config.startingSkills;
        var second = config.secondSkills;
        int number = config.skillNumber;
        double chance = config.additionalUniqueChance;
        try {
            config.startingSkills = java.util.List.of("tensura:great_sage", "tensura:great_sage", "wildhunt:storm_crown", "wildhunt:raijin");
            var pool = io.github.manasmods.tensura.menu.ReincarnationMenu.getSkillPool();
            helper.assertTrue(pool.contains(io.github.manasmods.tensura.registry.skill.UniqueSkills.GREAT_SAGE.get()), "Configured vanilla pool entry lost");
            helper.assertTrue(java.util.Collections.frequency(pool, io.github.manasmods.tensura.registry.skill.UniqueSkills.GREAT_SAGE.get()) == 2, "Configured base-pool weighting changed");
            for (Apex apex : Apex.values()) {
                var unique = WildHunt.UNIQUE.get(apex).get();
                helper.assertTrue(java.util.Collections.frequency(pool, unique) == 1, "Apex Unique missing or duplicated in roll pool");
                helper.assertTrue(!pool.contains(WildHunt.ULTIMATE.get(apex).get()), "Ultimate became a starting skill");
                helper.assertTrue(unique.createDefaultInstance().is(io.github.manasmods.tensura.data.TensuraSkillTags.UNIQUE_SKILLS), "Unique classification tag missing");
            }
            // Restrict only the test's configured base pool: the addon injects all four
            // candidates, then Tensura's real random-selection and grant code runs.
            config.startingSkills = java.util.List.of();
            config.secondSkills = java.util.List.of();
            config.skillNumber = 1;
            config.additionalUniqueChance = 0;
            var player = testPlayer(helper);
            RaceAPI.getRaceFrom(player).setRace(TensuraRaces.HUMAN.get(), false);
            io.github.manasmods.tensura.menu.ReincarnationMenu.randomUniqueSkill(player, true);
            long count = WildHunt.rollableUniques().stream().filter(skill -> SkillAPI.getSkillsFrom(player).getSkill(skill).isPresent()).count();
            helper.assertTrue(count == 1, "Native reincarnation did not grant exactly one rolled apex Unique");
        } finally {
            config.startingSkills = original; config.secondSkills = second;
            config.skillNumber = number; config.additionalUniqueChance = chance;
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void bossHistorySurvivesDeathAndIgnoresLegacyHunts(GameTestHelper helper) {
        var original = testPlayer(helper);
        HunterData.of(original).putInt("hunts", 1000);
        HunterData.of(original).putBoolean("rey_dau_trial", true);
        helper.assertTrue(HunterData.bossKills(original).isEmpty(), "Legacy hunts were converted into boss credit");
        HunterData.recordBossKill(original, net.minecraft.resources.ResourceLocation.parse("minecraft:wither"));
        var replacement = testPlayer(helper);
        HunterEvents.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(replacement, original, true));
        helper.assertTrue(HunterData.bossKills(replacement).equals(HunterData.bossKills(original)), "Death lost boss history");
        HunterData.recordBossKill(replacement, net.minecraft.resources.ResourceLocation.parse("minecraft:ender_dragon"));
        helper.assertTrue(HunterData.bossKills(original).size() == 1, "Clone shares mutable boss history");
        helper.succeed();
    }
    private static net.minecraft.world.entity.monster.Zombie durableTarget(GameTestHelper helper) {
        var target = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 3, 1, 1);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024);
        target.setHealth(target.getMaxHealth());
        return target;
    }
    private static net.minecraft.server.level.ServerPlayer combatPlayer(GameTestHelper helper) {
        var player = testPlayer(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setYRot(-90); player.setXRot(0);
        RaceAPI.getRaceFrom(player).setRace(WildHunt.HUNTERS.getFirst().get(), false);
        TensuraStorages.getExistenceFrom(player).setAura(100_000);
        return player;
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void everySkillDisplaysNamesAndPrimaryAttacksWork(GameTestHelper helper) {
        var player = combatPlayer(helper);
        for (Apex apex : Apex.values()) for (boolean ultimate : new boolean[]{false}) {
            var target = durableTarget(helper);
            var skill = (ultimate ? WildHunt.ULTIMATE : WildHunt.UNIQUE).get(apex).get();
            HunterData.learnReward(player, skill);
            var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
            helper.assertTrue(skill.getColoredName().getString().equals(ultimate ? apex.ultimateName : apex.uniqueName),
                "Incorrect skill name: " + skill.getColoredName().getString());
            float before = target.getHealth();
            skill.onPressed(instance, player, 0, 0);
            helper.assertTrue(target.getHealth() < before, "Primary did not hit: " + apex);
            helper.assertTrue(instance.getCoolDown(0) <= 4 && instance.getCoolDown(0) > 0, "Tick-style cooldown returned: " + apex);
            target.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void legacyStacksDoNotChangeDamageAndAreRemoved(GameTestHelper helper) {
        var player = combatPlayer(helper);
        for (Apex apex : java.util.List.of(Apex.NU_UDRA, Apex.JIN_DAHAAD)) {
            var skill = WildHunt.UNIQUE.get(apex).get(); HunterData.learnReward(player, skill);
            var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
            var clean = durableTarget(helper); var marked = durableTarget(helper); var data = HunterData.of(marked);
            for (String prefix : java.util.List.of("oil", "rime")) {
                data.putInt(prefix + "_stacks", 3); data.putUUID(prefix + "_owner", player.getUUID());
                data.putLong(prefix + "_until", helper.getLevel().getGameTime() + 300);
            }
            float before = clean.getHealth(); int mode = apex == Apex.NU_UDRA ? 1 : 0;
            instance.setMastery(0); skill.strike(helper.getLevel(), instance, player, clean, 75, mode, false);
            instance.setMastery(0); skill.strike(helper.getLevel(), instance, player, marked, 75, mode, false);
            helper.assertTrue(Math.abs(clean.getHealth() - marked.getHealth()) < 0.01 && clean.getHealth() < before, "Legacy stacks still changed damage: " + apex);
            ApexTimers.onEntityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(marked));
            helper.assertTrue(!data.contains("oil_stacks") && !data.contains("rime_stacks"), "Legacy stacks were retained");
            clean.discard(); marked.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allFourApexPassivesStayToggledAndCleanUp(GameTestHelper helper) {
        var player = combatPlayer(helper); var armor = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        double baseline = armor.getValue();
        for (Apex apex : Apex.values()) for (boolean ultimate : new boolean[]{false}) {
            var skill = (ultimate ? WildHunt.ULTIMATE : WildHunt.UNIQUE).get(apex).get(); HunterData.learnReward(player, skill);
            var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); instance.onToggleOn(player);
            helper.assertTrue(instance.isToggled() && armor.getValue() > baseline, "Passive failed to enable: " + apex);
            double boosted = armor.getValue(); instance.getOrCreateTag().putLong("passive_next", helper.getLevel().getGameTime());
            instance.getOrCreateTag().putLong("mantle_until", 1); skill.tickActive(instance, player);
            helper.assertTrue(instance.isToggled() && armor.getValue() == boosted, "Passive expired or attributes accumulated: " + apex);
            instance.onToggleOff(player);
            helper.assertTrue(!instance.isToggled() && armor.getValue() == baseline, "Disabled passive left armor behind: " + apex);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void oldSavesMigrateModesCooldownsAndStacksOnLogin(GameTestHelper helper) {
        var player = combatPlayer(helper);
        for (Apex apex : Apex.values()) for (boolean ultimate : new boolean[]{false}) {
            var skill = (ultimate ? WildHunt.ULTIMATE : WildHunt.UNIQUE).get(apex).get(); HunterData.learnReward(player, skill);
            var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
            instance.setMastery(100); instance.getOrCreateTag().putInt("combat_revision", 4);
            instance.getOrCreateTag().putInt("storm_charge", 3); instance.getOrCreateTag().putInt("ward_budget", 3);
            instance.setCoolDownList(new java.util.ArrayList<>(ultimate ? java.util.List.of(400,400,400,1200) : java.util.List.of(400,400,400)));
        }
        var abilities = TensuraStorages.getAbilityFrom(player);
        abilities.setAbilitySlot(0, WildHunt.UNIQUE.get(Apex.REY_DAU).get(), 2);
        ApexTimers.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(abilities.getAbilitySlot(0).getMode() == 0, "Equipped modes were not migrated");
        for (var instance : SkillAPI.getSkillsFrom(player).getLearnedSkills()) {
            if (instance.getSkill() instanceof ApexSkill skill) {
                helper.assertTrue(instance.getCooldownList().size() == (skill.ultimate ? 3 : 2), "Old mode count survived migration");
                helper.assertTrue(instance.getMastery() == 100, "Migration changed mastery");
                helper.assertTrue(!instance.getOrCreateTag().contains("storm_charge") && !instance.getOrCreateTag().contains("ward_budget"), "Legacy resources survived migration");
                for (int mode = 0; mode < instance.getModes(); mode++) helper.assertTrue(instance.getCoolDown(mode) == skill.balance.cooldown(mode, skill.ultimate), "Cooldown failed to migrate");
            }
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void railboltRicochetsFromEachNewTarget(GameTestHelper helper) {
        var player = combatPlayer(helper); player.setPos(player.position().add(0,40,0));
        var targets = new java.util.ArrayList<net.minecraft.world.entity.monster.Zombie>();
        for (int z : new int[]{0,5,11}) {
            helper.getLevel().getChunk(net.minecraft.core.BlockPos.containing(player.position().add(3,0,z)));
            var enemy = durableTarget(helper); enemy.setNoGravity(true); enemy.teleportTo(player.getX()+3,player.getY(),player.getZ()+z); targets.add(enemy);
        }
        var skill = WildHunt.UNIQUE.get(Apex.REY_DAU).get(); HunterData.learnReward(player,skill);
        var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); skill.onPressed(instance,player,0,0);
        for (var enemy : targets) helper.assertTrue(enemy.getHealth() < enemy.getMaxHealth(), "Railbolt failed its sequential ricochet");
        helper.assertTrue(!instance.getOrCreateTag().contains("storm_charge"), "Railbolt generated charge");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tidalWaveTravelsAndHitsEachTargetOnce(GameTestHelper helper) {
        var player = combatPlayer(helper); var target = durableTarget(helper);
        // Keep long-range checks above neighboring GameTest structure walls.
        player.setPos(player.position().add(0, 60, 0));
        helper.getLevel().getChunk(net.minecraft.core.BlockPos.containing(player.position().add(12, 0, 0)));
        target.setNoGravity(true); target.teleportTo(player.getX() + 12, player.getY(), player.getZ());
        var skill = WildHunt.UNIQUE.get(Apex.UTH_DUNA).get(); HunterData.learnReward(player, skill);
        var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
        float before = target.getHealth(); skill.onPressed(instance, player, 0, 0);
        helper.assertTrue(target.getHealth() == before, "Wave hit a distant target instantly");
        for (int step = 0; step < 7; step++) ApexArts.advance(skill, helper.getLevel(), instance, player);
        helper.assertTrue(target.getHealth() < before, "Travelling wave missed its distant target");
        helper.assertTrue(instance.getOrCreateTag().getList("travel_hits", net.minecraft.nbt.Tag.TAG_STRING).size() == 1, "Wave hit its target more than once");
        helper.assertTrue(target.getDeltaMovement().x > 0, "Tidal wave did not carry its target forward");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void avalancheIsAimedAndLaunchesWithoutRime(GameTestHelper helper) {
        var player = combatPlayer(helper); var target = durableTarget(helper); player.setPos(player.position().add(0,80,0));
        helper.getLevel().getChunk(net.minecraft.core.BlockPos.containing(player.position().add(12,0,0)));
        target.setNoGravity(true); target.teleportTo(player.getX()+12,player.getY(),player.getZ());
        var skill = WildHunt.UNIQUE.get(Apex.JIN_DAHAAD).get(); HunterData.learnReward(player,skill);
        var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); skill.onPressed(instance,player,0,0);
        helper.assertTrue(target.getHealth() < target.getMaxHealth() && target.getDeltaMovement().y > 0, "Avalanche failed to erupt at the aimed target");
        helper.assertTrue(!HunterData.of(target).contains("rime_stacks"), "Avalanche still builds rime");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ArtVisualEntity.class,target.getBoundingBox().inflate(5)).stream().anyMatch(e -> e.kind() == ArtVisualEntity.CRYSTAL), "Avalanche has no crystal geometry");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void oilIgnitionIsLimitedToOwnedPoolLocations(GameTestHelper helper) {
        var player = combatPlayer(helper); var target = durableTarget(helper);
        var remote = durableTarget(helper); remote.setPos(player.getX(), player.getY(), player.getZ() + 10);
        var skill = WildHunt.UNIQUE.get(Apex.NU_UDRA).get(); HunterData.learnReward(player, skill);
        var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
        skill.onPressed(instance, player, 0, 0); target.invulnerableTime = 0;
        float nearby = target.getHealth(), distant = remote.getHealth(); skill.onPressed(instance, player, 0, 1);
        helper.assertTrue(target.getHealth() < nearby, "Owned oil pool did not detonate");
        helper.assertTrue(remote.getHealth() == distant, "Ignition behaved like a generic radial attack");
        for (int n = 0; n < 6; n++) ApexArts.addPool(skill, helper.getLevel(), instance, player, player.position().add(n, 0, 0), false, 300);
        helper.assertTrue(instance.getOrCreateTag().getList("oil_pools", net.minecraft.nbt.Tag.TAG_COMPOUND).size() == 3, "Unique oil pool limit was exceeded");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void glacialPrisonAppliesNativeFrostAndVisibleCage(GameTestHelper helper) {
        var player = combatPlayer(helper); var target = durableTarget(helper);
        var skill = WildHunt.UNIQUE.get(Apex.JIN_DAHAAD).get(); HunterData.learnReward(player,skill);
        var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); skill.onPressed(instance,player,0,1);
        helper.assertTrue(target.hasEffect(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.FROST)), "Prison did not apply Tensura Frost");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ArtVisualEntity.class,target.getBoundingBox().inflate(3)).stream().filter(e -> e.kind() == ArtVisualEntity.CRYSTAL && e.lifetime()==80).count()==6, "Prison has no six-spire cage");
        helper.assertTrue(!instance.getOrCreateTag().contains("ward_budget"), "Prison retained the old ward resource");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void waterPassiveRetaliatesWithoutOilMeleeStacks(GameTestHelper helper) {
        var player = combatPlayer(helper); var target = durableTarget(helper);
        var defender = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
        defender.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024); defender.setHealth(defender.getMaxHealth());
        TensuraStorages.getExistenceFrom(defender).setAura(100_000);
        var water=WildHunt.UNIQUE.get(Apex.UTH_DUNA).get(); HunterData.learnReward(defender,water);
        var veil=SkillAPI.getSkillsFrom(defender).getSkill(water).orElseThrow(); veil.onToggleOn(defender);
        float before=target.getHealth(); defender.hurt(defender.damageSources().mobAttack(target),200);
        helper.assertTrue(target.getHealth()<before && veil.getOrCreateTag().getLong("counter_next")>helper.getLevel().getGameTime(), "Water passive failed to retaliate");
        var fire=WildHunt.UNIQUE.get(Apex.NU_UDRA).get(); HunterData.learnReward(player,fire);
        var mantle=SkillAPI.getSkillsFrom(player).getSkill(fire).orElseThrow(); mantle.onToggleOn(player);
        target.invulnerableTime=0; target.hurt(player.damageSources().playerAttack(player),10);
        helper.assertTrue(!HunterData.of(target).contains("oil_stacks"), "Melee still generated passive oil stacks");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void passiveDisablesWhenAuraRunsOutAndRestoresOnLogin(GameTestHelper helper) {
        var player=combatPlayer(helper); var skill=WildHunt.UNIQUE.get(Apex.JIN_DAHAAD).get(); HunterData.learnReward(player,skill);
        var instance=SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); instance.onToggleOn(player);
        var armor=player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR); double enabled=armor.getValue();
        skill.removePassive(player); ApexTimers.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(instance.isToggled() && armor.getValue()==enabled, "Saved toggle was not restored on login");
        TensuraStorages.getExistenceFrom(player).setAura(0); instance.getOrCreateTag().putLong("passive_next",helper.getLevel().getGameTime());
        ApexTimers.onEntityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
        helper.assertTrue(!instance.isToggled() && armor.getValue()<enabled, "Unfunded passive remained active");
        instance.onToggleOn(player); helper.assertTrue(!instance.isToggled(), "Empty aura allowed enabling the passive");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void thunderStepStopsAtSolidWalls(GameTestHelper helper) {
        // FakePlayer's connection intentionally ignores teleports; use a real
        // living caster so this checks movement as well as collision clipping.
        var player=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
        player.setYRot(-90); player.setXRot(0); player.setNoGravity(true);
        TensuraStorages.getExistenceFrom(player).setAura(100_000);
        player.setPos(player.position().add(0,100,0)); var start=player.position();
        for(int y=0;y<3;y++) for(int z=-1;z<=1;z++) helper.getLevel().setBlockAndUpdate(net.minecraft.core.BlockPos.containing(start.add(4,y,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var skill=WildHunt.UNIQUE.get(Apex.REY_DAU).get(); HunterData.learnReward(player,skill);
        var instance=SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); skill.onPressed(instance,player,0,1);
        helper.assertTrue(player.getX()>start.x && player.getX()<start.x+4 && helper.getLevel().noCollision(player), "Thunder Step crossed or entered a solid wall");
        helper.assertTrue(!instance.getOrCreateTag().contains("field_until"), "Thunder Step retained the fulgurite field");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void oilLashCreatesSyncedGeometryEvenOnAMiss(GameTestHelper helper) {
        var player=combatPlayer(helper); var target=durableTarget(helper);
        var skill=WildHunt.UNIQUE.get(Apex.NU_UDRA).get(); HunterData.learnReward(player,skill);
        var instance=SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); skill.onPressed(instance,player,0,0);
        var lashes=helper.getLevel().getEntitiesOfClass(ArtVisualEntity.class,player.getBoundingBox().inflate(16)).stream().filter(e->e.kind()==ArtVisualEntity.LASH).toList();
        helper.assertTrue(!lashes.isEmpty() && lashes.getFirst().end().length()>1, "Successful lash has no synced geometry");
        for(var lash:lashes) lash.discard(); target.discard(); player.setYRot(90); instance.setCoolDown(0,0); skill.onPressed(instance,player,0,0);
        var empty=helper.getLevel().getEntitiesOfClass(ArtVisualEntity.class,player.getBoundingBox().inflate(16)).stream().filter(e->e.kind()==ArtVisualEntity.LASH).findFirst().orElseThrow();
        helper.assertTrue(empty.end().length()>1 && empty.lifetime()==16, "Missed lash has no visible sweep");
        helper.runAfterDelay(18,()->{helper.assertTrue(empty.isRemoved(), "Tentacle visual never expired");helper.succeed();});
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void waterAndFireUtilitiesFollowTheToggleWithoutPotionResidue(GameTestHelper helper) {
        var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
        TensuraStorages.getExistenceFrom(caster).setAura(100_000);
        var water=WildHunt.UNIQUE.get(Apex.UTH_DUNA).get(); HunterData.learnReward(caster,water);
        var veil=SkillAPI.getSkillsFrom(caster).getSkill(water).orElseThrow(); veil.onToggleOn(caster);
        var breath=new net.neoforged.neoforge.event.entity.living.LivingBreatheEvent(caster,false,1,0);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(breath);
        helper.assertTrue(breath.canBreathe() && breath.getConsumeAirAmount()==0, "Water passive lost underwater breathing");
        veil.onToggleOff(caster);
        var dry=new net.neoforged.neoforge.event.entity.living.LivingBreatheEvent(caster,false,1,0);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(dry);
        helper.assertTrue(!dry.canBreathe(), "Water breathing remained after toggle-off");
        var fire=WildHunt.UNIQUE.get(Apex.NU_UDRA).get(); HunterData.learnReward(caster,fire);
        var mantle=SkillAPI.getSkillsFrom(caster).getSkill(fire).orElseThrow(); mantle.onToggleOn(caster);
        float before=caster.getHealth(); caster.hurt(caster.damageSources().lava(),4);
        helper.assertTrue(caster.getHealth()==before && !caster.hasEffect(MobEffects.FIRE_RESISTANCE), "Fire passive did not protect without potion residue");
        mantle.onToggleOff(caster); caster.invulnerableTime=0; caster.hurt(caster.damageSources().lava(),4);
        helper.assertTrue(caster.getHealth()<before, "Fire protection remained after toggle-off");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void thunderStepFollowsUpwardAndDownwardAim(GameTestHelper helper) {
        var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,140,1);
        caster.setNoGravity(true); caster.setYRot(-90);
        TensuraStorages.getExistenceFrom(caster).setAura(100_000);
        var skill=WildHunt.UNIQUE.get(Apex.REY_DAU).get(); HunterData.learnReward(caster,skill);
        var instance=SkillAPI.getSkillsFrom(caster).getSkill(skill).orElseThrow(); var origin=caster.position();
        for (float pitch : new float[]{-45,45}) {
            caster.setPos(origin); caster.setXRot(pitch); instance.setCoolDown(0,1);
            for(int distance=0;distance<=10;distance++) helper.getLevel().getChunk(net.minecraft.core.BlockPos.containing(origin.add(caster.getLookAngle().scale(distance))));
            skill.onPressed(instance,caster,0,1); var delta=caster.position().subtract(origin);
            helper.assertTrue(delta.x>4 && (pitch<0 ? delta.y>4 : delta.y< -4), "Thunder Step ignored vertical aim: " + delta);
            helper.assertTrue(Math.abs(delta.length()-16)<0.01, "Aimed dash changed its total range");
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void railboltReachesTwelveBlockHopsAndStopsBeyondThem(GameTestHelper helper) {
        // This long-range fixture must not chain through entities from adjacent tests.
        var player=combatPlayer(helper); player.setPos(player.position().add(512,160,512));
        var targets=new java.util.ArrayList<net.minecraft.world.entity.monster.Zombie>();
        for(int z:new int[]{0,11,24}) {
            helper.getLevel().getChunk(net.minecraft.core.BlockPos.containing(player.position().add(3,0,z)));
            var enemy=durableTarget(helper); enemy.setNoGravity(true); enemy.setPos(player.position().add(3,0,z)); targets.add(enemy);
        }
        var skill=WildHunt.UNIQUE.get(Apex.REY_DAU).get(); HunterData.learnReward(player,skill);
        var instance=SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); skill.onPressed(instance,player,0,0);
        helper.assertTrue(targets.get(0).getHealth()<1024 && targets.get(1).getHealth()<1024, "Railbolt did not reach a hop beyond the old eight-block radius");
        helper.assertTrue(targets.get(2).getHealth()==1024, "Railbolt exceeded its twelve-block bounce radius");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeHudBarriersBreakWithoutUpkeepRepairOrForeignCleanup(GameTestHelper helper) {
        for(Apex apex:java.util.List.of(Apex.UTH_DUNA,Apex.JIN_DAHAAD)) for(boolean ultimate:new boolean[]{false}) {
            var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
            caster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024); caster.setHealth(1024);
            var energy=TensuraStorages.getExistenceFrom(caster); energy.setAura(100_000); energy.setMagicule(100_000);
            var skill=(ultimate?WildHunt.ULTIMATE:WildHunt.UNIQUE).get(apex).get(); HunterData.learnReward(caster,skill);
            var instance=SkillAPI.getSkillsFrom(caster).getSkill(skill).orElseThrow(); instance.onToggleOn(caster);
            var barrier=caster.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MULTILAYER_BARRIER);
            helper.assertTrue(barrier.getValue()==skill.barrierCapacity(), "HUD's native attribute did not receive the shield capacity");
            // Isolate the multilayer capacity from the separate armor/resistance bonuses.
            var ownId=WildHunt.id((ultimate?apex.ultimateId:apex.uniqueId)+"_mantle");
            for(var attr:java.util.List.of(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                    io.github.manasmods.tensura.registry.attribute.TensuraAttributes.PHYSICAL_BARRIER,
                    io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAGIC_BARRIER)) caster.getAttribute(attr).removeModifier(ownId);
            float health=caster.getHealth(); caster.hurt(caster.damageSources().generic(),10);
            helper.assertTrue(caster.getHealth()==health && barrier.getValue()==skill.barrierCapacity(), "Native shield did not block a below-capacity hit");
            caster.invulnerableTime=0; caster.hurt(caster.damageSources().generic(),(float)skill.barrierCapacity()+50);
            helper.assertTrue(caster.isAlive() && barrier.getValue()==0 && caster.getHealth()<health, "Native overflow failed to break the HUD barrier");
            instance.getOrCreateTag().putLong("passive_next",helper.getLevel().getGameTime()); skill.tickActive(instance,caster);
            helper.assertTrue(instance.isToggled() && barrier.getValue()==0, "Passive upkeep repaired a broken shield");
            var foreign=WildHunt.id("test_foreign_barrier");
            barrier.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(foreign,7,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
            instance.onToggleOff(caster); helper.assertTrue(barrier.getValue()==7, "Disabling the passive removed another barrier source");
            instance.onToggleOn(caster); helper.assertTrue(barrier.getValue()==7, "Retoggling bypassed recovery delay");
            instance.getOrCreateTag().putLong("barrier_ready",helper.getLevel().getGameTime());
            instance.getOrCreateTag().putLong("passive_next",helper.getLevel().getGameTime());
            skill.tickActive(instance,caster);
            helper.assertTrue(barrier.getValue()==7+skill.barrierCapacity(), "Out-of-combat recovery failed to rearm the shield");
            instance.onToggleOff(caster); caster.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hunterVitalsIncreaseByStageAndUpgradeSavedRaces(GameTestHelper helper) {
        var player=combatPlayer(helper); var races=RaceAPI.getRaceFrom(player);
        races.setRace(TensuraRaces.HUMAN.get(),false); EnergyHelper.setBaseMaxEP(player,2_000_000);
        float baseHP=player.getMaxHealth(); double baseSHP=player.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH);
        for(int i=1;i<=5;i++) HunterData.recordBossKill(player,WildHunt.id("test_boss_"+i));
        var skill=WildHunt.UNIQUE.get(Apex.REY_DAU).get(); HunterData.learnReward(player,skill);
        var unique=SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); unique.setMastery(unique.getMaxMastery());
        for(int stage=1;stage<=3;stage++) {
            helper.assertTrue(races.evolveRace(WildHunt.HUNTERS.get(stage-1).get()), "Native evolution rejected hunter stage "+stage);
            helper.assertTrue(player.getMaxHealth()==baseHP+HunterProgression.HP_BONUS[stage], "Evolution HP bonus did not replace the previous stage");
            helper.assertTrue(player.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH)==baseSHP+HunterProgression.SHP_BONUS[stage], "Evolution did not increase native SHP");
            helper.assertTrue(SkillAPI.getSkillsFrom(player).getSkill(WildHunt.REGENERATION.get(stage-1).get()).isPresent(), "Native evolution omitted its new Extra Skill");
        }
        // Emulate an already-saved Sovereign with the old +30 HP and no SHP modifier.
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).addOrReplacePermanentModifier(
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(WildHunt.id("hunter_health"),30,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        player.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH).removeModifier(WildHunt.id("hunter_spiritual_health"));
        player.setHealth(15); TensuraStorages.getExistenceFrom(player).setSpiritualHealth(11);
        for(int login=0;login<2;login++) ApexTimers.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
        helper.assertTrue(player.getMaxHealth()==baseHP+HunterProgression.HP_BONUS[3] && player.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH)==baseSHP+HunterProgression.SHP_BONUS[3], "Saved hunter did not receive the new HP/SHP caps exactly once");
        helper.assertTrue(player.getHealth()==15 && TensuraStorages.getExistenceFrom(player).getSpiritualHealth()==11, "Relogging provided an unintended heal");
        races.setRace(TensuraRaces.HUMAN.get(),false);
        helper.assertTrue(player.getMaxHealth()==baseHP && player.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH)==baseSHP, "Leaving the hunter branch retained its vitality bonuses");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void railboltRicochetsThroughWallsButInitialShotStops(GameTestHelper helper) {
        var player=combatPlayer(helper); var start=player.position().add(0,180,0);
        for(var pos:java.util.List.of(start.add(0,0,5),start.add(7,0,5))){var chunk=new net.minecraft.world.level.ChunkPos(net.minecraft.core.BlockPos.containing(pos));helper.getLevel().setChunkForced(chunk.x,chunk.z,true);}
        player.setPos(start); player.setYRot(0); player.setXRot(0);
        var first=durableTarget(helper); first.setPos(start.add(0,0,5)); first.setNoGravity(true);
        var second=durableTarget(helper); second.setPos(start.add(7,0,5)); second.setNoGravity(true);
        for(int y=0;y<4;y++) for(int z=2;z<9;z++) helper.getLevel().setBlock(net.minecraft.core.BlockPos.containing(start.add(3,y,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        helper.assertTrue(!first.hasLineOfSight(second), "Test wall did not block the ricochet path");
        var skill=WildHunt.UNIQUE.get(Apex.REY_DAU).get(); HunterData.learnReward(player,skill);
        var instance=SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
        helper.runAfterDelay(5,()->{
        first.setHealth(1024);second.setHealth(1024);
        ApexArts.ricochet(skill,helper.getLevel(),instance,player);
        helper.assertTrue(first.getHealth()<1024 && second.getHealth()<1024,"Ricochet targets: first="+first.getHealth()+" second="+second.getHealth()+" primary="+ApexArts.target(helper.getLevel(),player,36));
        first.setHealth(1024); second.setHealth(1024); first.invulnerableTime=0; second.invulnerableTime=0;
        for(int x=-1;x<=1;x++) for(int y=0;y<4;y++) helper.getLevel().setBlock(net.minecraft.core.BlockPos.containing(start.add(x,y,2)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        ApexArts.ricochet(skill,helper.getLevel(),instance,player);
        helper.assertTrue(first.getHealth()==1024 && second.getHealth()==1024,"Initial Railbolt passed through its blocking wall");
        helper.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void extendedThunderStepPreservesAllMomentum(GameTestHelper helper) {
        for(boolean ultimate:new boolean[]{false}) {
            var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
            var start=caster.position().add(0,200,0); caster.setPos(start); caster.setNoGravity(true); caster.setYRot(0); caster.setXRot(-20);
            var motion=new net.minecraft.world.phys.Vec3(0.42,-0.18,0.73); caster.setDeltaMovement(motion);
            var skill=(ultimate?WildHunt.ULTIMATE:WildHunt.UNIQUE).get(Apex.REY_DAU).get(); HunterData.learnReward(caster,skill);
            ApexArts.thunderStep(skill,helper.getLevel(),SkillAPI.getSkillsFrom(caster).getSkill(skill).orElseThrow(),caster);
            helper.assertTrue(Math.abs(caster.position().distanceTo(start)-(ultimate?24:16))<0.01,"Extended teleport range incorrect");
            helper.assertTrue(caster.getDeltaMovement().distanceToSqr(motion)<0.000001,"Teleport changed momentum");
            helper.assertTrue(skill.balance.cooldown(1,ultimate)==(ultimate?4:5),"Teleport cooldown was not reduced"); caster.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void undertowFollowsCrosshairAndRendersVortex(GameTestHelper helper) {
        var player=combatPlayer(helper); var start=player.position().add(0,220,0); player.setPos(start); player.setYRot(0); player.setXRot(0);
        for(int x=-1;x<=1;x++) for(int y=0;y<4;y++) helper.getLevel().setBlock(net.minecraft.core.BlockPos.containing(start.add(x,y,18)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        for(boolean ultimate:new boolean[]{false}) {
            var skill=(ultimate?WildHunt.ULTIMATE:WildHunt.UNIQUE).get(Apex.UTH_DUNA).get(); HunterData.learnReward(player,skill);
            var instance=SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow(); ApexArts.secondary(skill,helper.getLevel(),instance,player);
            var center=ApexArts.point(instance.getOrCreateTag(),"field");
            helper.assertTrue(center.distanceTo(player.getEyePosition())>17 && center.distanceTo(player.getEyePosition())<18.1,"Undertow was not placed on the crosshair's surface");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ArtVisualEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(1),e->e.kind()==ArtVisualEntity.WHIRLPOOL).size()>0,"Undertow mesh missing");
        }
        player.setYRot(180);
        helper.assertTrue(Math.abs(ApexArts.cursorPlacement(helper.getLevel(),player,24).distanceTo(player.getEyePosition())-24)<0.01,"Cursor miss exceeded its placement limit");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hunterRegenerationIsGrantedByEvolutionAndOnlyStrongestHeals(GameTestHelper helper) {
        var player=combatPlayer(helper); var races=RaceAPI.getRaceFrom(player);
        for(int stage=1;stage<=3;stage++) {
            races.setRace(WildHunt.HUNTERS.get(stage-1).get(),false);
            for(int tier=1;tier<=stage;tier++) helper.assertTrue(SkillAPI.getSkillsFrom(player).getSkill(WildHunt.REGENERATION.get(tier-1).get()).isPresent(),"Race failed to grant regeneration tier "+tier);
        }
        var energy=TensuraStorages.getExistenceFrom(player); energy.setAura(10000); energy.setMagicule(5000);
        energy.setSpiritualHealth(player.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH)-10);
        player.setHealth(player.getMaxHealth()-20); player.tickCount=20;
        AuraRegenerationSkill.tick(player);
        helper.assertTrue(player.getHealth()==player.getMaxHealth(),"Infinite-equivalent recovery did not restore HP");
        helper.assertTrue(energy.getSpiritualHealth()==player.getAttributeValue(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH),"Infinite-equivalent recovery did not restore SHP");
        helper.assertTrue(energy.getAura()==9300 && energy.getMagicule()==5000,"Regeneration stacked or consumed magicules");
        AuraRegenerationSkill.tick(player); helper.assertTrue(energy.getAura()==9300,"Full health regeneration spent aura");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void regenerationTiersSpendOnlyAvailableAura(GameTestHelper helper) {
        for(int tier=1;tier<=3;tier++) {
            var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
            caster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100); caster.setHealth(80);
            var energy=TensuraStorages.getExistenceFrom(caster); energy.setAura(10000); energy.setMagicule(1234);
            var skill=WildHunt.REGENERATION.get(tier-1).get();
            helper.assertTrue(skill.getColoredName().getString().equals(AuraRegenerationSkill.NAMES[tier-1]),"Regeneration name exposed a localization key");
            HunterData.learnReward(caster,skill);
            var instance=SkillAPI.getSkillsFrom(caster).getSkill(skill).orElseThrow(); instance.setToggled(true);
            skill.regenerate(instance,caster);
            helper.assertTrue(caster.getHealth()==(tier==1?84:100),"Regeneration speed mismatch at tier "+tier);
            helper.assertTrue(energy.getAura()==(tier==1?9920:9400) && energy.getMagicule()==1234,"Incorrect recovery resource at tier "+tier);
            caster.setHealth(80); energy.setAura(15); skill.regenerate(instance,caster);
            helper.assertTrue(energy.getAura()>=0 && energy.getAura()<0.001 && energy.getMagicule()==1234,"Recovery overdrew aura or used MP");
            helper.assertTrue(caster.getHealth()>80 && caster.getHealth()<81,"Limited aura was not converted into partial recovery"); caster.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hunterEpGainsAndRaceEnergyKeepTwoToOneSplit(GameTestHelper helper) {
        var player=combatPlayer(helper); var target=durableTarget(helper);
        for(int stage=1;stage<=3;stage++) {
            RaceAPI.getRaceFrom(player).setRace(WildHunt.HUNTERS.get(stage-1).get(),false);
            var race=WildHunt.HUNTERS.get(stage-1).get();
            helper.assertTrue(Math.abs(race.getMinBaseAura()-2*race.getMinBaseMagicule())<0.001,"Race base EP split changed");
            var a=player.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_AURA);
            var m=player.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_MAGICULE);
            double oldA=a.getBaseValue(),oldM=m.getBaseValue();
            io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(player,target,900);
            double aura=a.getBaseValue()-oldA,mp=m.getBaseValue()-oldM;
            helper.assertTrue(aura>0 && mp>0 && Math.abs(aura-2*mp)<0.001,"Actual native kill EP did not split 2/3 aura, 1/3 magicules");
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void hunterOtherworldersUseNativeSpawnDataAndPersistRace(GameTestHelper helper) {
        for(var type:java.util.List.of(HunterOtherworlders.ALMA.get(),HunterOtherworlders.GEMMA.get())) {
            var npc=type.spawn(helper.getLevel(),helper.absolutePos(new net.minecraft.core.BlockPos(2,1,2)),net.minecraft.world.entity.MobSpawnType.COMMAND);
            helper.assertTrue(npc!=null,"Otherworlder could not spawn"); npc.setNoAi(true);
            helper.assertTrue(npc instanceof io.github.manasmods.tensura.entity.template.subclass.IOtherworlder,"NPC is not a native otherworlder");
            helper.assertTrue(HunterData.stage(npc)==1,"Otherworlder did not receive Awakened Hunter race");
            var registry=helper.getLevel().registryAccess().registryOrThrow(io.github.manasmods.tensura.registry.data.TensuraCustomData.ENTITY_EXISTENCE);
            var data=registry.get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type));
            helper.assertTrue(data!=null && data.abilities().orElseThrow().size()==7,"Native existence loadout is missing seven Tensura abilities");
            for(var id:data.abilities().orElseThrow()) helper.assertTrue(SkillAPI.getSkillsFrom(npc).getSkill(SkillAPI.getSkillRegistry().get(id)).isPresent(),"Native spawn omitted ability "+id);
            var saved=new net.minecraft.nbt.CompoundTag(); npc.saveWithoutId(saved);
            var loaded=type.create(helper.getLevel()); loaded.load(saved);
            helper.assertTrue(HunterData.stage(loaded)==1,"Otherworlder lost its race on save/load");
            helper.assertTrue(loaded.getTextureLocation().getNamespace().equals("wildhunt"),"NPC does not have a bundled skin"); npc.discard(); loaded.discard();
        }
        var registry=helper.getLevel().registryAccess().registryOrThrow(io.github.manasmods.tensura.registry.data.TensuraCustomData.OTHERWORLDER_SPAWN_DISTRIBUTION);
        double chance=0;
        for(var entry:registry) chance+=entry.chance();
        helper.assertTrue(Math.abs(chance-1)<0.000001,"Otherworlder spawn distribution is not normalized");
        helper.assertTrue(registry.get(WildHunt.id("alma"))!=null && registry.get(WildHunt.id("gemma"))!=null,"Native random otherworlder pool omits hunters");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void sovereignRegenerationStopsFundedOrdinaryDeathOnly(GameTestHelper helper) {
        var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
        var energy=TensuraStorages.getExistenceFrom(caster); energy.setAura(10000); energy.setMagicule(1234);
        var skill=WildHunt.REGENERATION.get(2).get(); HunterData.learnReward(caster,skill);
        var instance=SkillAPI.getSkillsFrom(caster).getSkill(skill).orElseThrow(); instance.setToggled(true);
        caster.hurt(caster.damageSources().generic(),10000);
        helper.assertTrue(caster.isAlive() && caster.getHealth()==1,"Funded Sovereign Rebirth did not avert ordinary death");
        helper.assertTrue(energy.getAura()==9970 && energy.getMagicule()==1234,"Death prevention used the wrong resource");
        energy.setAura(0); caster.invulnerableTime=0; caster.hurt(caster.damageSources().generic(),10000);
        helper.assertTrue(!caster.isAlive(),"Unfunded Sovereign Rebirth gave free immortality");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 360)
    public static void brokenBarrierRecoversAfterRealNoHitDelay(GameTestHelper helper) {
        var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
        caster.setNoGravity(true); caster.setPos(caster.position().add(0,240,0));
        caster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1024);caster.setHealth(1024);
        EnergyHelper.setBaseMaxEP(caster,300000);
        var energy=TensuraStorages.getExistenceFrom(caster);energy.setAura(100000);energy.setMagicule(100000);
        caster.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(100000);energy.setSpiritualHealth(100000);
        var skill=WildHunt.UNIQUE.get(Apex.UTH_DUNA).get();HunterData.learnReward(caster,skill);
        var instance=SkillAPI.getSkillsFrom(caster).getSkill(skill).orElseThrow();instance.onToggleOn(caster);
        var barrier=caster.getAttribute(io.github.manasmods.tensura.registry.attribute.TensuraAttributes.MULTILAYER_BARRIER);
        caster.hurt(caster.damageSources().generic(),300);
        helper.assertTrue(caster.isAlive() && barrier.getValue()==0,"Test hit did not leave a living unshielded caster: HP="+caster.getHealth()+" SHP="+energy.getSpiritualHealth());
        helper.runAfterDelay(100,()->{
            helper.assertTrue(barrier.getValue()==0,"Shield recovered before the no-hit delay");
            // Another hit, including a small hit, restarts the full recovery timer.
            caster.invulnerableTime=0;caster.hurt(caster.damageSources().generic(),1);
        });
        helper.runAfterDelay(210,()->helper.assertTrue(barrier.getValue()==0,"A later hit failed to postpone recovery"));
        helper.runAfterDelay(321,()->{helper.assertTrue(instance.isToggled() && barrier.getValue()==skill.barrierCapacity(),"Shield did not automatically recover: toggled="+instance.isToggled()+" value="+barrier.getValue()+" active="+HunterData.of(caster).getBoolean("active_skills")+" ready="+instance.getOrCreateTag().getLong("barrier_ready")+" now="+helper.getLevel().getGameTime()+" next="+instance.getOrCreateTag().getLong("passive_next")+" ticks="+caster.tickCount+" aura="+energy.getAura());helper.succeed();});
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void auraInfiniteRecoveryUsesNativeHealingGateWithoutMpDrain(GameTestHelper helper) {
        for(int tier:new int[]{2,3}) {
            var caster=helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG,1,1,1);
            caster.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);caster.setHealth(80);
            var energy=TensuraStorages.getExistenceFrom(caster);energy.setAura(10000);energy.setMagicule(1234);
            var skill=WildHunt.REGENERATION.get(tier-1).get();HunterData.learnReward(caster,skill);
            var instance=SkillAPI.getSkillsFrom(caster).getSkill(skill).orElseThrow();instance.setToggled(true);
            var frost=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.FROST);
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(frost,200));
            helper.assertTrue(caster.hasEffect(frost),"Healing suppression fixture was not applied");
            skill.regenerate(instance,caster);
            helper.assertTrue(caster.getHealth()==(tier==3?100:80),"Aura recovery disagrees with native healing suppression");
            helper.assertTrue(energy.getMagicule()==1234,"Healing gate correction charged native regeneration MP");
            if(tier==2) helper.assertTrue(energy.getAura()==10000,"Suppressed regeneration spent aura");
            else {
                caster.setHealth(80);
                var prison=io.github.manasmods.tensura.registry.effect.TensuraMobEffects.getReference(io.github.manasmods.tensura.registry.effect.TensuraMobEffects.INFINITE_IMPRISONMENT);
                caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(prison,200));
                double aura=energy.getAura();skill.regenerate(instance,caster);
                helper.assertTrue(caster.getHealth()==80 && energy.getAura()==aura,"Infinite Imprisonment did not suppress Sovereign Rebirth");
            }
            caster.discard();
        }
        helper.succeed();
    }

}
