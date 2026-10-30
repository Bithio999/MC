package dev.heavenlyrestriction.worldhop;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class WorldSelect {
    public static final String SELECTION="heavenlyrestriction:next_world";
    public static boolean allowed(ServerPlayer p){return WorldHop.available(p) && p.isAlive() && !p.getPersistentData().contains(WorldHopMetadata.MARKER) && SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get()).filter(i->i.getMastery()>=0 && !i.isTemporarySkill() && !i.isSubInstance() && i.canInteractSkill(p) && HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get().canActivateSkill(i,p,1)).isPresent();}
    /** Upgrade pre-journal saves by following only their recorded, existing sibling saves. */
    public static WorldHopJournal.Journal journal(Path world,String name,long seed)throws IOException{
        var journal=WorldHopJournal.read(world,name,seed);if(Files.exists(world.resolve(WorldHopJournal.FILE)))return journal;
        var worlds=new ArrayList<>(journal.worlds());Set<Path> seen=new HashSet<>();seen.add(world.toRealPath());Path cursor=world;
        while(worlds.size()<WorldHopJournal.LIMIT){
            Path sidecar=cursor.resolve("heavenlyrestriction-world-hop.json");if(!Files.isRegularFile(sidecar,LinkOption.NOFOLLOW_LINKS) || Files.size(sidecar)>1024*1024)break;
            String previous;
            try{var json=com.google.gson.JsonParser.parseString(Files.readString(sidecar)).getAsJsonObject();if(!json.has("previous"))break;previous=json.get("previous").getAsString();}catch(RuntimeException ex){throw new IOException("Could not import previous worlds",ex);}
            var old=WorldHopFiles.previous(world.getParent(),previous);if(old.isEmpty() || !seen.add(old.get()))break;cursor=old.get();var data=WorldHopMetadata.read(cursor.resolve("level.dat")).getCompound("Data");
            worlds.add(new WorldHopJournal.Entry(WorldHopJournal.id(previous),previous,data.getString("LevelName"),data.getCompound("WorldGenSettings").getLong("seed")));
        }
        return new WorldHopJournal.Journal(journal.current(),worlds);
    }
    public static WorldHopJournal.Journal journal(ServerPlayer p)throws IOException{return journal(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize(),p.server.getWorldData().getLevelName(),p.server.overworld().getSeed());}
    public static WorldHopJournal.Entry destination(ServerPlayer p)throws IOException{
        String id=p.getPersistentData().getString(SELECTION);if(id.isEmpty())return null;
        var j=journal(p);var entry=j.find(id).filter(e->!e.id().equals(j.current())).orElseThrow(()->new IOException("Destination not visited"));
        if(WorldHopFiles.previous(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getParent(),entry.folder()).isEmpty())throw new IOException("Destination no longer exists");return entry;
    }
    public static boolean select(ServerPlayer p,String id){
        if(!allowed(p))return false;
        try{if(id.isEmpty()){p.getPersistentData().remove(SELECTION);return true;}
            if(!WorldHopJournal.validId(id))return false;var j=journal(p);var entry=j.find(id).filter(e->!e.id().equals(j.current())).orElse(null);
            if(entry==null || WorldHopFiles.previous(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getParent(),entry.folder()).isEmpty())return false;
            p.getPersistentData().putString(SELECTION,id);return true;
        }catch(IOException ex){return false;}
    }
    public static void send(ServerPlayer p,boolean open,String message){
        if(!allowed(p))return;var state=new CompoundTag();state.putBoolean("open",open);state.putString("message",message);state.putString("selected",p.getPersistentData().getString(SELECTION));
        try{var j=journal(p);var list=new ListTag();for(var e:j.worlds())if(!e.id().equals(j.current())){var row=new CompoundTag();row.putString("id",e.id());row.putString("name",e.name());row.putLong("seed",e.seed());row.putBoolean("available",WorldHopFiles.previous(p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getParent(),e.folder()).isPresent());list.add(row);}state.put("worlds",list);}
        catch(IOException ex){state.putString("message","Could not read world history. Your saves are unchanged.");state.putBoolean("error",true);}
        PacketDistributor.sendToPlayer(p,new WorldHopNetwork.Worlds(state));
    }
}
