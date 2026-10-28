package dev.heavenlyrestriction;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.*;
import io.github.manasmods.tensura.storage.ep.*;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Isolated real-player resource check. Excluded from normal builds and releases. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class ChainedResourceReview {
    private static boolean prepared;private static volatile boolean ready;private static volatile double expectedMP,expectedEP;private static int age;
    private static void near(double actual,double expected,String text){if(Math.abs(actual-expected)>Math.max(.001,Math.abs(expected)*.000001))throw new IllegalStateException(text+": "+actual+" != "+expected);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(mc.player==null || server==null)return;
        if(!prepared){prepared=true;server.execute(()->{
            var p=server.getPlayerList().getPlayers().getFirst();var level=p.serverLevel();for(int x=7;x<=9;x++)for(int z=7;z<=9;z++)level.setBlockAndUpdate(new BlockPos(x,70,z),net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());p.teleportTo(level,8.5,71,8.5,-90,0);p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=false;p.onUpdateAbilities();
            for(var owned:java.util.List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(owned);
            ReincarnationMenu.setRace(p,HeavenlyRestriction.RESTRICTED.getFirst().get(),true,false);var storage=TensuraStorages.getExistenceFrom(p);var source=p.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            storage.setMagicule(100000);near(storage.getMagicule(),EnergyHelper.getMaxMagicule(p),"Real-player reserve cap");
            try{server.getCommands().getDispatcher().execute("tensura edit stat @s ep max set 1000",source);double before=EnergyHelper.getMaxAura(p),raw=EnergyHelper.getBaseMaxAura(p),mp=EnergyHelper.getMaxMagicule(p);server.getCommands().getDispatcher().execute("tensura edit stat @s ep max add 1000",source);near(EnergyHelper.getBaseMaxAura(p)-raw,900,"Real-player 90/10 command");near(EnergyHelper.getMaxAura(p)-before,4500,"Real-player 5x gain after EP reset");near(EnergyHelper.getMaxMagicule(p),mp,"Real-player command MP ceiling");System.out.println("CHAINED_RESOURCE_REVIEW: exact command added 900 raw / 4500 effective aura; max MP unchanged at "+mp);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
            var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tensura","ancient_forest"));var biome=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(key);var chunk=level.getChunkAt(p.blockPosition());chunk.fillBiomesFromNoise((x,y,z,sampler)->biome,level.getChunkSource().randomState().sampler());var ambient=TensuraStorages.getChunkFrom(chunk);ambient.reinitialize();ambient.setMagicule(ambient.getMaxMagicule());
            if(!level.getBiome(p.blockPosition()).is(key))throw new IllegalStateException("Resource review is not in the actual Ancient Forest biome");double area=AreaMagiculeHelper.getMagicule(p,true);p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER).setBaseValue(1);storage.setSleepModeTime(-1);storage.setAura(0);storage.setMagicule(10);double natural=EnergyHelper.CONFIG.baseAuraRegen*p.getAttributeValue(TensuraAttributes.AURA_REGENERATION_MULTIPLIER);
            try{var method=ExistenceStorage.class.getDeclaredMethod("handleEnergyRegen",net.minecraft.server.MinecraftServer.class,net.minecraft.world.level.Level.class,net.minecraft.world.entity.LivingEntity.class,IExistence.class);method.setAccessible(true);method.invoke(null,server,level,p,storage);method.invoke(null,server,level,p,storage);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            near(storage.getMagicule(),11,"Real-player one-second MP recovery");near(storage.getAura(),2*(natural+area*EnergyHelper.CONFIG.areaMagiculeRegen-.5),"Real-player Ancient Forest conversion");System.out.println("CHAINED_RESOURCE_REVIEW: actual Ancient Forest area="+area+", two half-second native ticks: MP 10 -> "+storage.getMagicule()+", aura 0 -> "+storage.getAura());
            storage.setMagicule(100000);expectedMP=EnergyHelper.getMaxMagicule(p);near(storage.getMagicule(),expectedMP,"Final reserve cap");storage.setAura(EnergyHelper.getMaxAura(p));storage.markDirty();expectedEP=EnergyHelper.getMaxEP(p);ready=true;
        });}
        if(!ready)return;age++;if(mc.screen!=null)mc.setScreen(null);mc.options.hideGui=false;mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        if(age==30){var storage=TensuraStorages.getExistenceFrom(mc.player);System.out.println("CHAINED_RESOURCE_REVIEW: client rawMP="+EnergyHelper.getBaseMaxMagicule(mc.player)+", effectiveMP="+EnergyHelper.getMaxMagicule(mc.player)+", tag="+SkillAPI.getSkillsFrom(mc.player).getSkill(HeavenlyRestriction.CHAINED.get()).map(i->i.getOrCreateTag().toString()).orElse("missing"));near(storage.getMagicule(),expectedMP,"Client MP reserve");near(EnergyHelper.getMaxMagicule(mc.player),expectedMP,"Client MP cap");near(EnergyHelper.getMaxEP(mc.player),expectedEP,"Client total EP");System.out.println("CHAINED_RESOURCE_REVIEW: client reserve/cap synchronized at "+expectedMP+", total EP="+expectedEP+", Myths absent="+!net.neoforged.fml.ModList.get().isLoaded("wildhunt"));try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())){var dir=java.nio.file.Path.of("../docs/media");java.nio.file.Files.createDirectories(dir);screenshot.writeToFile(dir.resolve("chained-resource-status.png"));}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        if(age==50){System.out.println("CHAINED_RESOURCE_REVIEW: complete");mc.stop();}
    }
}
