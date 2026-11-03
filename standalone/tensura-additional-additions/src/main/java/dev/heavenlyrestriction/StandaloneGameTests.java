package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import static dev.heavenlyrestriction.TestFixtures.*;
import static dev.heavenlyrestriction.ChainedGameTests.near;

@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class StandaloneGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void independentlyLoadsExactlyFiveRacesFourteenSkillsAndOneVisualEntity(GameTestHelper h){
        h.assertTrue(!net.neoforged.fml.ModList.get().isLoaded("wildhunt"),"Tensura Myths contaminated the standalone runtime");
        long races=RaceAPI.getRaceRegistry().getIds().stream().filter(id->id.getNamespace().equals(HeavenlyRestriction.ID)).count();
        long skills=SkillAPI.getSkillRegistry().getIds().stream().filter(id->id.getNamespace().equals(HeavenlyRestriction.ID)).count();
        long entities=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(id->id.getNamespace().equals(HeavenlyRestriction.ID)).count();
        h.assertTrue(races==5 && skills==14 && entities==1,"Unrelated or missing standalone content: "+races+" races, "+skills+" skills, "+entities+" entities");
        var p=player(h);var visual=ArtVisualEntity.spawn(h.getLevel(),ArtVisualEntity.CHAINED_RUSH,p.position(),new net.minecraft.world.phys.Vec3(16,0,0),14);
        h.assertTrue(visual.noPhysics && visual.kind()==ArtVisualEntity.CHAINED_RUSH && visual.end().x==16,"Standalone dash visual lost its synced shape");visual.discard();h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ordinaryHumanGrowthAndForeignSaveStateRemainUntouched(GameTestHelper h){
        var p=player(h);var victim=target(h,p.position().add(5,0,0));
        p.getPersistentData().putString("foreign_addon","preserved");RestrictionData.of(p).putInt("sentinel",7);
        p.getAttribute(TensuraAttributes.AURA_GAIN).setBaseValue(100);p.getAttribute(TensuraAttributes.MAGICULE_GAIN).setBaseValue(100);
        double aura=EnergyHelper.getBaseMaxAura(p),magicules=EnergyHelper.getBaseMaxMagicule(p);
        double auraGain=90*p.getAttributeValue(TensuraAttributes.AURA_GAIN)/100,mpGain=90*p.getAttributeValue(TensuraAttributes.MAGICULE_GAIN)/100;
        io.github.manasmods.tensura.handler.DeathHandler.gainEPEntity(p,victim,90);
        near(h,EnergyHelper.getBaseMaxAura(p)-aura,auraGain*2,"Non-lineage native aura split changed");near(h,EnergyHelper.getBaseMaxMagicule(p)-magicules,mpGain,"Non-lineage native MP split changed");
        h.assertTrue(p.getPersistentData().getString("foreign_addon").equals("preserved") && !p.getPersistentData().contains("wildhunt"),"Standalone state leaked into Myths/foreign storage");
        var clone=player(h);RestrictionEvents.onClone(new net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone(clone,p,true));
        h.assertTrue(RestrictionData.of(clone).getInt("sentinel")==7,"Standalone pact state did not survive clone");h.succeed();
    }
}
