package dev.heavenlyrestriction.worldhop;

import net.minecraft.nbt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

public final class WorldHopMetadata {
    public static final String MARKER="heavenlyrestriction:world_hop_landing";
    public static void rewrite(Path world,long seed,UUID player,String previous,String continuation)throws IOException{
        Path file=world.resolve("level.dat");CompoundTag root=read(file),data=root.getCompound("Data");
        if(!data.contains("WorldGenSettings",Tag.TAG_COMPOUND) || !data.contains("Player",Tag.TAG_COMPOUND))throw new IOException("World or single-player character data is missing");
        CompoundTag gen=data.getCompound("WorldGenSettings");if(gen.getLong("seed")==seed)throw new IOException("World Hop requires a different seed");gen.putLong("seed",seed);
        data.putBoolean("initialized",false);data.put("DragonFight",net.minecraft.world.level.dimension.end.EndDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE,net.minecraft.world.level.dimension.end.EndDragonFight.Data.DEFAULT).getOrThrow());data.remove("WanderingTraderId");data.putInt("WanderingTraderSpawnDelay",0);data.putInt("WanderingTraderSpawnChance",0);
        // Scheduled block/function callbacks belong to the terrain that was left behind.
        data.remove("ScheduledEvents");
        var history=WorldHopSaveCompat.advanceHistory(world,data.getString("LevelName"),seed,previous);
        data.putString("LevelName",history.name()+" · Parallel "+history.hop());
        preparePlayer(data.getCompound("Player"),previous);NbtIo.writeCompressed(root,file);
        // Integrated worlds prefer level.dat Player, but also keep the matching UUID file coherent.
        Path playerFile=world.resolve("playerdata").resolve(player+".dat");if(Files.isRegularFile(playerFile)){CompoundTag tag=read(playerFile);preparePlayer(tag,previous);NbtIo.writeCompressed(tag,playerFile);}
        WorldHopSaveCompat.carryStarterKitHistory(world,previous,continuation);
        Files.writeString(world.resolve("WORLD-HOP.txt"),"Fresh-seed continuation. Previous world: "+previous+"\nSeed: "+seed+"\nEvery dimension regenerates. Player data and progression are retained.\n");
    }
    public static CompoundTag read(Path file)throws IOException{return NbtIo.readCompressed(file,NbtAccounter.create(128L*1024*1024));}
    public static void preparePlayer(CompoundTag player,String previous){
        CompoundTag marker=new CompoundTag();marker.putString("Previous",previous);marker.putString("Dimension",player.getString("Dimension"));marker.put("Pos",player.getList("Pos",Tag.TAG_DOUBLE).copy());
        player.getCompound("NeoForgeData").remove(MARKER);
        CompoundTag forge=player.contains("NeoForgeData",Tag.TAG_COMPOUND)?player.getCompound("NeoForgeData"):new CompoundTag();forge.put(MARKER,marker);player.put("NeoForgeData",forge);
        for(String key:new String[]{"SpawnX","SpawnY","SpawnZ","SpawnAngle","SpawnForced","SpawnDimension","RootVehicle","enteredNetherPosition","LastDeathLocation"})player.remove(key);
        player.putFloat("FallDistance",0);ListTag motion=new ListTag();for(int n=0;n<3;n++)motion.add(DoubleTag.valueOf(0));player.put("Motion",motion);
    }
}
