package dev.heavenlyrestriction.worldhop;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class WorldHopJournalTest {
    @TempDir Path saves;
    @Test void libraryNumberDoesNotRestartAfterReturningAndDuplicateLabelsAreRepaired()throws Exception{
        Path active=Files.createDirectory(saves.resolve("Slot"));var original=new WorldHopJournal.Entry(WorldHopJournal.id("original"),"Slot","Home",10);
        var a=new WorldHopJournal.Entry(WorldHopJournal.id("A"),"Slot","Home · Parallel 1",20);var b=new WorldHopJournal.Entry(WorldHopJournal.id("B"),"Old B","Home · Parallel 1",30);
        var old=new WorldHopJournal.Journal(a.id(),java.util.List.of(a,b,original));WorldHopJournal.write(active,old);var repaired=WorldHopJournal.read(active,a.name(),a.seed());
        assertEquals("Home · Parallel 1",repaired.find(a.id()).orElseThrow().name());assertEquals("Home · Parallel 2",repaired.find(b.id()).orElseThrow().name());assertEquals(30,repaired.find(b.id()).orElseThrow().seed());
        assertEquals(3,WorldHopJournal.nextParallelNumber(repaired));var returned=WorldHopJournal.arrive(repaired,original,"Slot");assertEquals(3,WorldHopJournal.nextParallelNumber(returned));
        var next=new WorldHopJournal.Entry(WorldHopJournal.id("C"),"Slot","Home · Parallel 3",40);assertEquals(4,WorldHopJournal.nextParallelNumber(WorldHopJournal.arrive(returned,next,"Slot")));
    }
    @Test void returnKeepsStableIdentityAndLatestFolderWithoutDuplicateWorlds()throws Exception{
        Path original=Files.createDirectory(saves.resolve("Original"));var first=WorldHopJournal.read(original,"Home",123);
        var second=new WorldHopJournal.Entry(WorldHopJournal.id("PWJ-B"),"PWJ-B","Parallel 1",456);
        var there=WorldHopJournal.arrive(first,second,"PWJ-B");Path b=Files.createDirectory(saves.resolve("PWJ-B"));WorldHopJournal.write(b,there);
        var loaded=WorldHopJournal.read(b,"Parallel 1",456);assertEquals(2,loaded.worlds().size());
        var back=WorldHopJournal.arrive(loaded,first.worlds().getFirst(),"PWJ-return");assertEquals(first.current(),back.current());assertEquals(2,back.worlds().size());assertEquals("PWJ-return",back.find(first.current()).orElseThrow().folder());assertEquals("PWJ-B",back.find(second.id()).orElseThrow().folder());
        Path returned=Files.createDirectory(saves.resolve("PWJ-return"));WorldHopJournal.write(returned,back);var resumed=WorldHopJournal.read(returned,"Home",123);assertEquals(back,resumed);
    }
    @Test void malformedAndLinkedJournalsFailWithoutInventingPaths()throws Exception{
        Path world=Files.createDirectory(saves.resolve("World"));Path file=world.resolve(WorldHopJournal.FILE);Files.writeString(file,"{broken");assertThrows(IOException.class,()->WorldHopJournal.read(world,"Home",1));
        Files.writeString(file,"{\"current\":\""+WorldHopJournal.id("World")+"\",\"worlds\":[{\"id\":\""+WorldHopJournal.id("evil")+"\",\"folder\":\"../outside\",\"name\":\"Bad\",\"seed\":1}]}");assertThrows(IOException.class,()->WorldHopJournal.read(world,"Home",1));
        Files.delete(file);Path outside=saves.resolve("outside.json");Files.writeString(outside,"{}");Files.createSymbolicLink(file,outside);assertThrows(IOException.class,()->WorldHopJournal.read(world,"Home",1));assertFalse(WorldHopJournal.validId("../world"));assertFalse(WorldHopJournal.validId(""));
    }
    @Test void capIncludesCurrentWorldAndBlocksOnlyCreation()throws Exception{
        var entries=new java.util.ArrayList<WorldHopJournal.Entry>();for(int n=0;n<128;n++)entries.add(new WorldHopJournal.Entry(WorldHopJournal.id("world"+n),"Slot","World "+n,n));
        var full=new WorldHopJournal.Journal(entries.getFirst().id(),entries);assertFalse(WorldHopJournal.canCreate(full));assertEquals(128,WorldHopJournal.arrive(full,entries.get(1),"Slot").worlds().size());
        assertThrows(IllegalStateException.class,()->WorldHopJournal.arrive(full,new WorldHopJournal.Entry(WorldHopJournal.id("new"),"Slot","New",999),"Slot"));
        Path slot=java.nio.file.Files.createDirectory(saves.resolve("Slot"));WorldHopJournal.write(slot,full);assertEquals(128,WorldHopJournal.read(slot,"Current",0).worlds().size());
        entries.add(new WorldHopJournal.Entry(WorldHopJournal.id("old129"),"Old","Older world",129));var legacy=new WorldHopJournal.Journal(full.current(),entries);WorldHopJournal.write(slot,legacy);assertEquals(129,WorldHopJournal.read(slot,"Current",0).worlds().size());assertFalse(WorldHopJournal.canCreate(legacy));
    }
}
