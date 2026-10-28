package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.TensuraSkillInstance;
import io.github.manasmods.tensura.menu.*;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import static dev.wildhunt.MythicGameTests.*;
import static dev.wildhunt.ChainedGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class ChainedCreatorGameTests {
    private static ServerPlayer owner(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);
        h.assertTrue(reincarnated(p,UniqueSkills.CREATOR.get()),"Native reincarnation Creator grant failed");return p;
    }
    private static boolean create(ServerPlayer p,ManasSkill skill){
        return new SkillCreationMenu(0,UniqueSkills.CREATOR.get(),1,List.of(skill.getRegistryName())).clickMenuButton(p,0);
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void inheritedCreatorUsesNativeMenuToCreateReplaceAndRenewTemporarySkills(GameTestHelper h){
        var p=owner(h);var storage=SkillAPI.getSkillsFrom(p);var root=storage.getSkill(UniqueSkills.CREATOR.get()).orElseThrow();double mp=EnergyHelper.getMaxMagicule(p);
        // Reproduce the formerly rejected handoff: a Creator tag alone carries no trusted scope.
        var unscoped=new TensuraSkillInstance(UniqueSkills.FIGHTER.get());unscoped.getOrCreateTag().putString("Creator","tensura:creator");
        h.assertTrue(!SkillHelper.learnSkill(p,unscoped,1200),"A bare metadata tag bypassed Chained");
        h.assertTrue(create(p,UniqueSkills.FIGHTER.get()),"Native creation menu rejected the click");
        var first=storage.getSkill(UniqueSkills.FIGHTER.get()).orElseThrow();h.assertTrue(first.isTemporarySkill() && first.getRemoveTime()==UniqueSkills.CREATOR.get().getCreatedSkillTimer(root,p),"Native creation duration/temporary flag changed");
        h.assertTrue(first.getOrCreateTag().getString("Creator").equals("tensura:creator") && ChainedState.inherited(p,first.getSkill()),"Generated skill lost Creator provenance");
        h.assertTrue(root.getCoolDown(1)==UniqueSkills.CREATOR.get().getCreationCooldown(root,p),"Native creation cooldown changed");near(h,EnergyHelper.getMaxMagicule(p),mp,"Temporary creation spent permanent MP");
        h.assertTrue(!ChainedContext.inherited(p) && !HunterData.learnReward(p,UniqueSkills.MUSICIAN.get()),"Creation leaked a blanket learning exemption");
        root.setCoolDown(0,1);root.setMastery(root.getMaxMastery());h.assertTrue(create(p,UniqueSkills.OBSERVER.get()),"Replacement menu failed");
        h.assertTrue(storage.getSkill(UniqueSkills.FIGHTER.get()).isEmpty(),"Native replacement left the previous temporary skill");var second=storage.getSkill(UniqueSkills.OBSERVER.get()).orElseThrow();h.assertTrue(second.isTemporarySkill(),"Replacement became permanent");
        second.setRemoveTime(60);root.setCoolDown(0,1);h.assertTrue(create(p,UniqueSkills.OBSERVER.get()) && second.getRemoveTime()==1200,"Native mastered renewal failed");
        h.assertTrue(!ChainedContext.inherited(p),"Renewal leaked its scope");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void forgedCreatorMetadataAndMissingCreatorCannotAuthorizeLearning(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);
        var fake=new TensuraSkillInstance(UniqueSkills.FIGHTER.get());fake.getOrCreateTag().putString("Creator","tensura:creator");fake.getOrCreateTag().putBoolean("NoMagiculeCost",true);
        h.assertTrue(!SkillHelper.learnSkill(p,fake,1200),"Forged metadata authorized a generated skill");
        h.assertTrue(!create(p,UniqueSkills.FIGHTER.get()) && SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.FIGHTER.get()).isEmpty(),"Menu without an owned Creator bypassed the gate");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void grandfatheredAndTemporaryCreatorsDoNotAuthorizeNewSkills(GameTestHelper h){
        var p=player(h);HunterData.learnReward(p,UniqueSkills.CREATOR.get());vow(p,false);var root=SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.CREATOR.get()).orElseThrow();
        h.assertTrue(!ChainedState.inherited(p,root.getSkill()),"Fixture marked a non-reincarnation Creator inherited");create(p,UniqueSkills.FIGHTER.get());
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.FIGHTER.get()).isEmpty(),"Grandfathered Creator gained a new inheritance exemption");
        ChainedState.remember(p,root.getSkill());root.setRemoveTime(60);root.setCoolDown(0,1);create(p,UniqueSkills.OBSERVER.get());
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.OBSERVER.get()).isEmpty() && !ChainedContext.inherited(p),"A temporary Creator authorized generation or leaked scope");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ordinaryCreatorWithoutChainedRetainsNativeGeneration(GameTestHelper h){
        var p=player(h);HunterData.learnReward(p,UniqueSkills.CREATOR.get());h.assertTrue(create(p,UniqueSkills.FIGHTER.get()),"Ordinary menu rejected creation");
        h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(UniqueSkills.FIGHTER.get()).orElseThrow().isTemporarySkill() && !ChainedContext.inherited(p),"Ordinary generation changed behavior");h.succeed();
    }
}
