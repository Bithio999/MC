package dev.heavenlyrestriction.worldhop;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class WorldHopFilesTest {
    @TempDir Path saves;
    Path source()throws IOException{Path p=Files.createDirectory(saves.resolve("Original"));Files.writeString(p.resolve("level.dat"),"original metadata");return p;}
    void write(Path root,String file,String content)throws IOException{Path p=root.resolve(file);Files.createDirectories(p.getParent());Files.writeString(p,content);}
    @Test void recoveryOnlyResolvesAnExistingSiblingWorld()throws Exception{
        Path original=source();assertEquals(Optional.of(original),WorldHopFiles.previous(saves,"Original"));assertTrue(WorldHopFiles.previous(saves,"../outside").isEmpty());assertTrue(WorldHopFiles.previous(saves,"missing").isEmpty());assertTrue(WorldHopFiles.previous(saves,"").isEmpty());
    }
    @Test void rejectsDedicatedLanGuestsAndExtraPlayers(){
        assertTrue(WorldHopFiles.available(true,false,true,1));
        assertFalse(WorldHopFiles.available(false,false,true,1));assertFalse(WorldHopFiles.available(true,true,true,1));assertFalse(WorldHopFiles.available(true,false,false,1));assertFalse(WorldHopFiles.available(true,false,true,2));assertFalse(WorldHopFiles.available(true,false,true,0));
    }
    @Test void retainsCharacterAndPackStateButRegeneratesEveryDimensionsTerrain()throws Exception{
        Path old=source();for(String file:List.of("playerdata/person.dat","advancements/person.json","stats/person.json","data/ftbteams.dat","data/manascore_world_storage.dat","serverconfig/example.toml","datapacks/example/pack.mcmeta","datapacks/example/data/demo/structure/region/test.nbt","data/foreign/entities/progress.dat"))write(old,file,"keep:"+file);
        for(String file:List.of("region/r.0.0.mca","entities/r.0.0.mca","poi/r.0.0.mca","DIM-1/region/r.0.0.mca","DIM1/entities/r.0.0.mca","dimensions/mod/sky/data/world_state.dat","dimensions/mod/sky/poi/r.0.0.mca","data/map_1.dat","data/forcedchunks.dat","DIM1/data/random_sequences.dat","session.lock","level.dat_old","icon.png"))write(old,file,"old:"+file);
        Path fresh=WorldHopFiles.fork(old,"PWJ-test",p->Files.writeString(p.resolve("level.dat"),"new seed"));
        assertEquals("original metadata",Files.readString(old.resolve("level.dat")));assertEquals("new seed",Files.readString(fresh.resolve("level.dat")));
        try(var files=Files.walk(old)){for(Path f:files.filter(Files::isRegularFile).toList()){Path rel=old.relativize(f);if(rel.toString().equals("level.dat"))continue;if(WorldHopFiles.keep(rel,false))assertEquals(Files.readString(f),Files.readString(fresh.resolve(rel)));else assertFalse(Files.exists(fresh.resolve(rel)),rel.toString());}}
    }
    @Test void failuresLeaveOriginalAndExistingWorldsUnchangedAndRemoveStaging()throws Exception{
        Path old=source();assertThrows(IOException.class,()->WorldHopFiles.fork(old,"PWJ-broken",p->{throw new IOException("full disk");}));assertFalse(Files.exists(saves.resolve("PWJ-broken")));
        assertEquals("original metadata",Files.readString(old.resolve("level.dat")));try(var dirs=Files.list(saves)){assertEquals(List.of(old),dirs.toList());}
        Path existing=Files.createDirectory(saves.resolve("PWJ-existing"));write(existing,"sentinel","keep");assertThrows(IOException.class,()->WorldHopFiles.fork(old,"PWJ-existing",p->fail("overwrote world")));assertEquals("keep",Files.readString(existing.resolve("sentinel")));
    }
    @Test void refusesEscapingPathsAndLinkedState()throws Exception{
        Path old=source();assertThrows(IOException.class,()->WorldHopFiles.fork(old,"../escape",p->{}));write(saves,"private.txt","untouched");Files.createSymbolicLink(old.resolve("linked.dat"),saves.resolve("private.txt"));assertThrows(IOException.class,()->WorldHopFiles.fork(old,"PWJ-links",p->{}));assertEquals("untouched",Files.readString(saves.resolve("private.txt")));
    }
}
