package dev.heavenlyrestriction.worldhop;

import net.minecraft.nbt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

public final class WorldHopMetadata {
    public static final String MARKER="heavenlyrestriction:world_hop_landing";
    public static void rewrite(Path world,long seed,UUID player,String previous,String continuation)throws IOException{
        rewrite(world,seed,player,previous,continuation,0);
    }
    public static void rewrite(Path world,long seed,UUID player,String previous,String continuation,long libraryNumber)throws IOException{
        Path file=world.resolve("level.dat");CompoundTag root=read(file),data=root.getCompound("Data");
        if(!data.contains("WorldGenSettings",Tag.TAG_COMPOUND) || !data.contains("Player",Tag.TAG_COMPOUND))throw new IOException("World or single-player character data is missing");
        CompoundTag gen=data.getCompound("WorldGenSettings");if(gen.getLong("seed")==seed)throw new IOException("World Hop requires a different seed");gen.putLong("seed",seed);
        data.putBoolean("initialized",false);data.put("DragonFight",net.minecraft.world.level.dimension.end.EndDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE,net.minecraft.world.level.dimension.end.EndDragonFight.Data.DEFAULT).getOrThrow());data.remove("WanderingTraderId");data.putInt("WanderingTraderSpawnDelay",0);data.putInt("WanderingTraderSpawnChance",0);
        // Scheduled block/function callbacks belong to the terrain that was left behind.
        data.remove("ScheduledEvents");
        var history=WorldHopSaveCompat.advanceHistory(world,data.getString("LevelName"),seed,previous,libraryNumber);
        data.putString("LevelName",history.name()+" · Parallel "+history.hop());
        preparePlayer(data.getCompound("Player"),previous);NbtIo.writeCompressed(root,file);
        // Integrated worlds prefer level.dat Player, but also keep the matching UUID file coherent.
        Path playerFile=world.resolve("playerdata").resolve(player+".dat");if(Files.isRegularFile(playerFile)){CompoundTag tag=read(playerFile);preparePlayer(tag,previous);NbtIo.writeCompressed(tag,playerFile);}
        WorldHopSaveCompat.carryStarterKitHistory(world,previous,continuation);
        Files.writeString(world.resolve("WORLD-HOP.txt"),"Fresh-seed continuation. Previous world: "+previous+"\nSeed: "+seed+"\nEvery dimension regenerates. Player data and progression are retained.\n");
    }
    public static CompoundTag read(Path file)throws IOException{return NbtIo.readCompressed(file,NbtAccounter.create(128L*1024*1024));}
    public static void setName(Path world,String name)throws IOException{Path file=world.resolve("level.dat");var root=read(file);root.getCompound("Data").putString("LevelName",name);NbtIo.writeCompressed(root,file);}
    /** Keep destination world metadata, terrain, entities and time; transfer the present character. */
    public static void revisit(Path world,Path source,UUID player,String previous,String continuation)throws IOException{
        var current=read(source.resolve("level.dat")).getCompound("Data");var root=read(world.resolve("level.dat"));var data=root.getCompound("Data");
        if(!current.contains("Player",Tag.TAG_COMPOUND) || !data.contains("Player",Tag.TAG_COMPOUND) || !data.contains("WorldGenSettings",Tag.TAG_COMPOUND))throw new IOException("World or character data is missing");
        var character=current.getCompound("Player").copy();var lastVisit=data.getCompound("Player");
        character.put("Pos",lastVisit.getList("Pos",Tag.TAG_DOUBLE).copy());character.putString("Dimension",lastVisit.getString("Dimension"));
        if(lastVisit.contains("Rotation",Tag.TAG_LIST))character.put("Rotation",lastVisit.getList("Rotation",Tag.TAG_FLOAT).copy());
        preparePlayer(character,previous);data.put("Player",character);NbtIo.writeCompressed(root,world.resolve("level.dat"));
        Files.createDirectories(world.resolve("playerdata"));NbtIo.writeCompressed(character,world.resolve("playerdata").resolve(player+".dat"));
        for(String directory:new String[]{"advancements","stats"}){
            Path original=source.resolve(directory).resolve(player+".json"),target=world.resolve(directory).resolve(player+".json");
            if(Files.isRegularFile(original,LinkOption.NOFOLLOW_LINKS)){Files.createDirectories(target.getParent());Files.copy(original,target,StandardCopyOption.REPLACE_EXISTING);}else Files.deleteIfExists(target);
        }
        // Both departing and destination worlds may have claimed the pack's starter kit.
        Path kits=source.resolve("data/starterkit/tracking.json");if(Files.isRegularFile(kits,LinkOption.NOFOLLOW_LINKS)){
            Path target=world.resolve("data/starterkit/tracking.json");Files.createDirectories(target.getParent());Files.copy(kits,target,StandardCopyOption.REPLACE_EXISTING);
        }
        WorldHopSaveCompat.carryStarterKitHistory(world,previous,continuation);
        Files.writeString(world.resolve("WORLD-HOP.txt"),"Visited-world continuation. Previous world: "+previous+"\nCurrent character retained; destination terrain and world state preserved.\n");
    }
    public static void preparePlayer(CompoundTag player,String previous){
        CompoundTag marker=new CompoundTag();marker.putString("Previous",previous);marker.putString("Dimension",player.getString("Dimension"));marker.put("Pos",player.getList("Pos",Tag.TAG_DOUBLE).copy());
        player.getCompound("NeoForgeData").remove(MARKER);
        CompoundTag forge=player.contains("NeoForgeData",Tag.TAG_COMPOUND)?player.getCompound("NeoForgeData"):new CompoundTag();forge.put(MARKER,marker);player.put("NeoForgeData",forge);
        for(String key:new String[]{"SpawnX","SpawnY","SpawnZ","SpawnAngle","SpawnForced","SpawnDimension","RootVehicle","enteredNetherPosition","LastDeathLocation"})player.remove(key);
        player.putFloat("FallDistance",0);ListTag motion=new ListTag();for(int n=0;n<3;n++)motion.add(DoubleTag.valueOf(0));player.put("Motion",motion);
    }
}
