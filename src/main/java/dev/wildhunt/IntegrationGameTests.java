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
        helper.assertTrue(instance.getModes() == 3, "Unique skill modes missing");
        helper.assertTrue(WildHunt.ULTIMATE.get(Apex.UTH_DUNA).get().createDefaultInstance().getModes() == 4, "Ultimate domain mode missing");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void guardConsumesAuraAndHonorsCooldown(GameTestHelper helper) {
        var player = testPlayer(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        RaceAPI.getRaceFrom(player).setRace(WildHunt.HUNTERS.getFirst().get(), false);
        var energy = TensuraStorages.getExistenceFrom(player);
        energy.setAura(10_000);
        var skill = WildHunt.UNIQUE.get(Apex.UTH_DUNA).get();
        var storage = SkillAPI.getSkillsFrom(player);
        helper.assertTrue(HunterData.learnReward(player, skill), "Guard skill reward was rejected");
        var instance = storage.getSkill(skill).orElseThrow();
        skill.onPressed(instance, player, 0, 2);
        helper.assertTrue(player.hasEffect(MobEffects.ABSORPTION), "Water Veil did not activate");
        helper.assertTrue(instance.onCoolDown(2), "Guard cooldown not set");
        helper.assertTrue(energy.getAura() < 10_000, "Guard did not consume aura");
        double remaining = energy.getAura();
        skill.onPressed(instance, player, 0, 2);
        helper.assertTrue(energy.getAura() == remaining, "Cooldown allowed a second cast");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void ultimateAscensionReplacesOnlyMatchingUnique(GameTestHelper helper) throws Exception {
        var player = testPlayer(helper);
        var skills = SkillAPI.getSkillsFrom(player);
        RaceAPI.getRaceFrom(player).setRace(WildHunt.HUNTERS.get(2).get(), false);
        EnergyHelper.setBaseMaxEP(player, 2_000_000);
        var source = WildHunt.UNIQUE.get(Apex.REY_DAU).get();
        var other = WildHunt.UNIQUE.get(Apex.UTH_DUNA).get();
        helper.assertTrue(HunterData.learnReward(player, source), "Source skill reward rejected");
        helper.assertTrue(HunterData.learnReward(player, other), "Other skill reward rejected");
        var instance = skills.getSkill(source).orElseThrow();
        instance.setMastery(instance.getMaxMastery());
        var commands = helper.getLevel().getServer().getCommands();
        commands.getDispatcher().execute("wildhunt ascend rey_dau", player.createCommandSourceStack());
        helper.assertTrue(skills.getSkill(WildHunt.ULTIMATE.get(Apex.REY_DAU).get()).isPresent(), "Ultimate not learned");
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
            config.startingSkills = java.util.List.of("tensura:great_sage", "tensura:great_sage", "wildhunt:storm_crown");
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
            long count = java.util.Arrays.stream(Apex.values()).filter(apex ->
                SkillAPI.getSkillsFrom(player).getSkill(WildHunt.UNIQUE.get(apex).get()).isPresent()).count();
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
        for (Apex apex : Apex.values()) for (boolean ultimate : new boolean[]{false, true}) {
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
    public static void oilStacksDetonateAndRimeShatters(GameTestHelper helper) {
        var player = combatPlayer(helper);
        for (Apex apex : java.util.List.of(Apex.NU_UDRA, Apex.JIN_DAHAAD)) {
            var skill = WildHunt.UNIQUE.get(apex).get();
            HunterData.learnReward(player, skill);
            var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
            var target = durableTarget(helper);
            if (apex == Apex.NU_UDRA) {
                for (int i = 0; i < 3; i++) {
                    target.invulnerableTime = 0; instance.setCoolDown(0, 0);
                    skill.onPressed(instance, player, 0, 0);
                }
                helper.assertTrue(HunterData.of(target).getInt("oil_stacks") == 3, "Oil did not build three stacks");
                target.invulnerableTime = 0;
                float before = target.getHealth();
                skill.onPressed(instance, player, 0, 1);
                helper.assertTrue(before - target.getHealth() > 150, "Oil did not amplify ignition");
                helper.assertTrue(!HunterData.of(target).contains("oil_stacks"), "Ignition did not consume oil");
            } else {
                var data = HunterData.of(target);
                data.putInt("rime_stacks", 3); data.putUUID("rime_owner", player.getUUID());
                data.putLong("rime_until", helper.getLevel().getGameTime() + 200);
                float before = target.getHealth();
                skill.onPressed(instance, player, 0, 0);
                helper.assertTrue(before - target.getHealth() > 150, "Rime did not amplify shatter");
                helper.assertTrue(!data.contains("rime_stacks"), "Shatter did not consume rime");
            }
            target.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void mantlesExpireAndDoNotStackTheirAttributes(GameTestHelper helper) {
        var player = combatPlayer(helper);
        var armor = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        double baseline = armor.getValue();
        for (Apex apex : Apex.values()) {
            var skill = WildHunt.UNIQUE.get(apex).get();
            HunterData.learnReward(player, skill);
            var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
            skill.onPressed(instance, player, 0, 2);
            helper.assertTrue(armor.getValue() > baseline, "Mantle provided no defense: " + apex);
            double boosted = armor.getValue();
            skill.tickActive(instance, player);
            helper.assertTrue(armor.getValue() == boosted, "Mantle stacked on refresh: " + apex);
            instance.getOrCreateTag().putLong("mantle_until", helper.getLevel().getGameTime());
            ApexTimers.onEntityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
            helper.assertTrue(armor.getValue() == baseline, "Expired mantle left armor behind: " + apex);
        }
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void domainUsesOneSecondScheduling(GameTestHelper helper) {
        var player = combatPlayer(helper);
        var skill = WildHunt.ULTIMATE.get(Apex.REY_DAU).get();
        HunterData.learnReward(player, skill);
        var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
        skill.onPressed(instance, player, 0, 3);
        long initial = instance.getOrCreateTag().getLong("domain_next");
        helper.runAfterDelay(21, () -> {
            ApexTimers.onEntityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
            long next = instance.getOrCreateTag().getLong("domain_next");
            helper.assertTrue(next > initial && next == helper.getLevel().getGameTime() + 20, "Domain still waits for ManasCore's five-second tick");
            ApexTimers.onEntityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
            helper.assertTrue(instance.getOrCreateTag().getLong("domain_next") == next, "Domain pulsed twice in one tick");
            helper.runAfterDelay(21, () -> {
                ApexTimers.onEntityTick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
                helper.assertTrue(instance.getOrCreateTag().getLong("domain_next") > next, "Second domain pulse was missed");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void existingCharactersGetCorrectedCooldownsOnLogin(GameTestHelper helper) {
        var player = combatPlayer(helper);
        for (Apex apex : Apex.values()) for (boolean ultimate : new boolean[]{false, true}) {
            var skill = (ultimate ? WildHunt.ULTIMATE : WildHunt.UNIQUE).get(apex).get();
            HunterData.learnReward(player, skill);
            var instance = SkillAPI.getSkillsFrom(player).getSkill(skill).orElseThrow();
            for (int mode = 0; mode < instance.getModes(); mode++) instance.setCoolDown(mode == 3 ? 1200 : 400, mode);
        }
        ApexTimers.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));
        for (var instance : SkillAPI.getSkillsFrom(player).getLearnedSkills()) {
            if (instance.getSkill() instanceof ApexSkill skill) {
                for (int mode = 0; mode < instance.getModes(); mode++) helper.assertTrue(
                    instance.getCoolDown(mode) == skill.balance.cooldown(mode, skill.ultimate), "Legacy cooldown was not corrected");
            }
        }
        helper.succeed();
    }

}
