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
        HunterData.of(player).putInt("hunts", 20);
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
        helper.assertTrue(HunterData.learnReward(player, skill), "Guard trial reward was rejected");
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
        helper.assertTrue(HunterData.learnReward(player, source), "Source trial reward rejected");
        helper.assertTrue(HunterData.learnReward(player, other), "Other trial reward rejected");
        var instance = skills.getSkill(source).orElseThrow();
        instance.setMastery(instance.getMaxMastery());
        HunterData.of(player).putBoolean("rey_dau_trial", true);
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
    public static void hostileKillCompletesTrialAndGrantsUnique(GameTestHelper helper) {
        var player = testPlayer(helper);
        RaceAPI.getRaceFrom(player).setRace(WildHunt.HUNTERS.getFirst().get(), false);
        HunterData.of(player).putInt("rey_dau_kills", 9);
        var phantom = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PHANTOM, 2, 1, 2);
        phantom.setHealth(1);
        phantom.hurt(player.damageSources().playerAttack(player), 100_000);
        helper.assertTrue(HunterData.of(player).getInt("hunts") == 1, "Hostile death was not counted");
        helper.assertTrue(HunterData.of(player).getBoolean("rey_dau_trial"), "Trial did not complete");
        helper.assertTrue(SkillAPI.getSkillsFrom(player).getSkill(WildHunt.UNIQUE.get(Apex.REY_DAU).get()).isPresent(), "Trial skill was not granted");
        helper.succeed();
    }
}
