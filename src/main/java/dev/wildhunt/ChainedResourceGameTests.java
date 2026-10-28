package dev.wildhunt;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.ExistenceStorage;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import static dev.wildhunt.MythicGameTests.*;
import static dev.wildhunt.ChainedGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class ChainedResourceGameTests {
    private static void command(net.minecraft.server.level.ServerPlayer p,String text){try{p.getServer().getCommands().getDispatcher().execute(text,p.createCommandSourceStack().withPermission(4).withSuppressedOutput());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}}
    private static void regen(net.minecraft.world.entity.LivingEntity p,double area){try{var method=ExistenceStorage.class.getDeclaredMethod("handleMagiculeRegen",io.github.manasmods.tensura.storage.ep.IExistence.class,net.minecraft.world.entity.LivingEntity.class,double.class,double.class);method.setAccessible(true);method.invoke(null,TensuraStorages.getExistenceFrom(p),p,area,EnergyHelper.getMaxMagicule(p));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    @GameTest(template="empty",timeoutTicks=100)
    public static void magiculeReserveWritesAndOldSavesStayWithinEffectiveCapacity(GameTestHelper h){
        var p=player(h);vow(p,false);p.getAttribute(TensuraAttributes.LIMITED_SPIRITUAL_MAX_MAGICULE).setBaseValue(50);var energy=TensuraStorages.getExistenceFrom(p);
        energy.setMagicule(5000);near(h,energy.getMagicule(),50,"Magicule writer exceeded effective max");energy.setEP(20000);near(h,energy.getMagicule(),50,"Combined EP writer bypassed the current MP cap");energy.setMagicule(-1);near(h,energy.getMagicule(),-1,"Energy exhaustion sentinel was lost");
        var tag=new net.minecraft.nbt.CompoundTag();((ExistenceStorage)energy).save(tag);tag.putDouble("magicule",5000);((ExistenceStorage)energy).load(tag);ChainedState.refresh(p);near(h,energy.getMagicule(),50,"Existing saved overfill was not reconciled");
        var other=player(h);var normal=TensuraStorages.getExistenceFrom(other);double max=EnergyHelper.getMaxMagicule(other);normal.setMagicule(max+5000);near(h,normal.getMagicule(),max+5000,"Unrelated owner's native overfill rules changed");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void actualRegenerationKeepsOneMagiculePerSecondAndConvertsBiomeOutput(GameTestHelper h){
        var p=player(h);vow(p,false);var energy=TensuraStorages.getExistenceFrom(p);var rate=p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);p.getAttribute(TensuraAttributes.LIMITED_SPIRITUAL_MAX_MAGICULE).setBaseValue(50);energy.setSleepModeTime(-1);rate.setBaseValue(1);
        double unit=200*EnergyHelper.CONFIG.areaMagiculeRegen;h.assertTrue(unit>.5,"Expected native area regeneration above the retained budget");energy.setMagicule(0);energy.setAura(0);regen(p,200);regen(p,200);near(h,energy.getMagicule(),1,"Two native half-second pulses did not retain exactly 1 MP");near(h,energy.getAura(),2*(unit-.5),"Biome-based regeneration did not convert into aura");
        rate.setBaseValue(3);energy.setMagicule(0);energy.setAura(0);regen(p,10000);near(h,energy.getMagicule(),.5,"High regeneration area overflowed the MP budget");near(h,energy.getAura(),10000*EnergyHelper.CONFIG.areaMagiculeRegen*3-.5,"High area plus rate bonus conversion");
        energy.setMagicule(50);energy.setAura(0);regen(p,10000);near(h,energy.getMagicule(),50,"Full MP exceeded its cap");near(h,energy.getAura(),10000*EnergyHelper.CONFIG.areaMagiculeRegen*3-.5,"Full MP prevented conversion");
        rate.setBaseValue(1);energy.setSleepModeTime(10);energy.setMagicule(0);energy.setAura(0);regen(p,200);near(h,energy.getMagicule(),.5,"Sleep bonus bypassed the flat MP budget");near(h,energy.getAura(),unit+50*EnergyHelper.CONFIG.sleepModeMagicule-.5,"Sleep regeneration was lost instead of converted");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void exactNativeEPAddCommandUsesNinetyTenAndFivefoldAuraAfterEPReset(GameTestHelper h){
        var p=player(h);ReincarnationMenu.setRace(p,WildHunt.RESTRICTED.getFirst().get(),true,false);var energy=TensuraStorages.getExistenceFrom(p);
        command(p,"tensura edit stat @s ep max set 1000");HunterData.of(p).remove("chained_remaining_starting_aura");SkillAPI.getSkillsFrom(p).getSkill(WildHunt.CHAINED.get()).orElseThrow().getOrCreateTag().remove("remaining_starting_aura");double raw=EnergyHelper.getBaseMaxAura(p),aura=EnergyHelper.getMaxAura(p),ep=EnergyHelper.getMaxEP(p),mp=EnergyHelper.getMaxMagicule(p);energy.setAura(0);energy.setMagicule(0);
        command(p,"tensura edit stat @s ep max add 1000");near(h,EnergyHelper.getBaseMaxAura(p)-raw,900,"Native command did not allocate 90% raw aura");near(h,EnergyHelper.getMaxAura(p)-aura,4500,"Command aura gain missed 5x after reducing EP");near(h,EnergyHelper.getMaxEP(p)-ep,4500,"Total EP did not include aura multiplier");near(h,EnergyHelper.getMaxMagicule(p),mp,"Command bypassed Chained's MP ceiling");near(h,energy.getAura(),0,"Ordinary max-add refilled current aura");
        aura=EnergyHelper.getMaxAura(p);command(p,"tensura edit stat @s ep max add 1000 true");near(h,EnergyHelper.getMaxAura(p)-aura,4500,"Reset-current overload has different growth");near(h,energy.getAura(),EnergyHelper.getMaxAura(p),"Reset-current overload did not fill aura");near(h,energy.getMagicule(),EnergyHelper.getMaxMagicule(p),"Reset-current overload overfilled MP");
        double restoredAura=EnergyHelper.getMaxAura(p);var saved=SkillAPI.getSkillsFrom(p).getSkill(WildHunt.CHAINED.get()).orElseThrow();var restored=io.github.manasmods.manascore.skill.api.ManasSkillInstance.fromNBT(saved.toNBT());SkillAPI.getSkillsFrom(p).updateSkill(restored,true);HunterData.of(p).remove("chained_starting_aura");HunterData.of(p).remove("chained_remaining_starting_aura");ChainedState.initialize(p);near(h,EnergyHelper.getMaxAura(p),restoredAura,"Skill-tag restoration lost the reduced birth exemption");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void commandSplitWithoutChainedAndNormalHumansRemainCorrect(GameTestHelper h){
        var p=player(h);RaceAPI.getRaceFrom(p).setRace(WildHunt.RESTRICTED.get(1).get(),false);double aura=EnergyHelper.getBaseMaxAura(p),mp=EnergyHelper.getBaseMaxMagicule(p);command(p,"tensura edit stat @s ep max add 1000");near(h,EnergyHelper.getBaseMaxAura(p)-aura,900,"Restricted race without Chained lost 90/10");near(h,EnergyHelper.getBaseMaxMagicule(p)-mp,100,"Restricted race without Chained lost the MP share");
        var other=player(h);RaceAPI.getRaceFrom(other).setRace(TensuraRaces.HUMAN.get(),false);aura=EnergyHelper.getBaseMaxAura(other);mp=EnergyHelper.getBaseMaxMagicule(other);command(other,"tensura edit stat @s ep max add 1000");near(h,EnergyHelper.getBaseMaxAura(other)-aura,500,"Native human aura split changed");near(h,EnergyHelper.getBaseMaxMagicule(other)-mp,500,"Native human MP split changed");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void ancientForestBiomeConvertsThroughTheCompleteNativeRegenTick(GameTestHelper h){
        var p=player(h);vow(p,false);var level=h.getLevel();p.setPos((p.getBlockX() & ~15)+8,160,(p.getBlockZ() & ~15)+8);
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tensura","ancient_forest"));var biome=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(key);var chunk=level.getChunkAt(p.blockPosition());
        chunk.fillBiomesFromNoise((x,y,z,sampler)->biome,level.getChunkSource().randomState().sampler());var ambient=TensuraStorages.getChunkFrom(chunk);ambient.reinitialize();ambient.setMagicule(ambient.getMaxMagicule());
        h.assertTrue(level.getBiome(p.blockPosition()).is(key),"Actual Ancient Forest biome was not applied");double area=io.github.manasmods.tensura.storage.AreaMagiculeHelper.getMagicule(p,true);h.assertTrue(area>10000,"Native Ancient Forest magicule bonus missing");
        p.getAttribute(TensuraAttributes.LIMITED_SPIRITUAL_MAX_MAGICULE).setBaseValue(50);p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER).setBaseValue(1);var energy=TensuraStorages.getExistenceFrom(p);energy.setSleepModeTime(-1);energy.setMagicule(10);energy.setAura(0);double nativeAura=EnergyHelper.CONFIG.baseAuraRegen*p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER);
        try{var method=ExistenceStorage.class.getDeclaredMethod("handleEnergyRegen",net.minecraft.server.MinecraftServer.class,net.minecraft.world.level.Level.class,net.minecraft.world.entity.LivingEntity.class,io.github.manasmods.tensura.storage.ep.IExistence.class);method.setAccessible(true);method.invoke(null,p.getServer(),level,p,energy);method.invoke(null,p.getServer(),level,p,energy);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        near(h,energy.getMagicule(),11,"Forest recovery did not retain 1 MP per second");near(h,energy.getAura(),2*(nativeAura+area*EnergyHelper.CONFIG.areaMagiculeRegen-.5),"Complete forest tick lost the area conversion or normal aura recovery");h.succeed();
    }
}
