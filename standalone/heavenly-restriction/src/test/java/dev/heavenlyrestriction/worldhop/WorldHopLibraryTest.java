package dev.heavenlyrestriction.worldhop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class WorldHopLibraryTest {
    @TempDir Path saves;
    private static final String A=WorldHopJournal.id("A"),B=WorldHopJournal.id("B");
    private Path active()throws Exception{Path p=Files.createDirectory(saves.resolve("My World"));Files.writeString(p.resolve("level.dat"),"A");Files.writeString(p.resolve("build.txt"),"A build");Files.writeString(WorldHopLibrary.library(p).resolve("owned.txt"),"library");return p;}
    private WorldHopLibrary.Transaction prepare(Path active)throws Exception{var tx=WorldHopLibrary.begin(active,A);WorldHopFiles.stage(active,tx.next(),p->{Files.writeString(p.resolve("level.dat"),"B");Files.writeString(p.resolve("build.txt"),"B build");},false);return tx;}
    private void emptyTransactions()throws Exception{try(var paths=Files.list(saves.resolve(".pwj-transactions"))){assertEquals(0,paths.count());}}
    @Test void fixedSlotCarriesLibraryOnceAndKeepsLatestWorldSnapshots()throws Exception{
        Path active=active();var first=prepare(active);assertFalse(Files.exists(first.next().resolve(WorldHopLibrary.DIRECTORY)));WorldHopLibrary.install(first);assertEquals("B",Files.readString(active.resolve("level.dat")));WorldHopLibrary.finish(active);
        Path a=WorldHopLibrary.archived(active,A).orElseThrow();assertEquals("A build",Files.readString(a.resolve("build.txt")));assertFalse(Files.exists(a.resolve(WorldHopLibrary.DIRECTORY)));assertEquals("library",Files.readString(active.resolve(WorldHopLibrary.DIRECTORY).resolve("owned.txt")));
        var second=WorldHopLibrary.begin(active,B);WorldHopFiles.stage(a,second.next(),p->{},true);WorldHopLibrary.install(second);WorldHopLibrary.finish(active);
        assertEquals("A",Files.readString(active.resolve("level.dat")));assertEquals("B build",Files.readString(WorldHopLibrary.archived(active,B).orElseThrow().resolve("build.txt")));assertFalse(WorldHopLibrary.pending(active));emptyTransactions();
        try(var paths=Files.list(saves)){assertEquals(1,paths.filter(p->Files.isRegularFile(p.resolve("level.dat"))).count());}
    }
    @Test void interruptedPreparationAndEveryInstallBoundaryRestorePreviousSlot()throws Exception{
        for(int boundary=0;boundary<4;boundary++){
            Path active=active();var tx=prepare(active);
            if(boundary==3)WorldHopLibrary.install(tx);
            else if(boundary>0){Files.move(active,tx.previous());if(boundary==2)Files.move(tx.previous().resolve(WorldHopLibrary.DIRECTORY),tx.next().resolve(WorldHopLibrary.DIRECTORY));}
            WorldHopLibrary.recover(saves);assertEquals("A",Files.readString(active.resolve("level.dat")));assertEquals("library",Files.readString(active.resolve(WorldHopLibrary.DIRECTORY).resolve("owned.txt")));emptyTransactions();
            try(var files=Files.walk(active)){for(Path f:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(f);}
        }
    }
    @Test void failedPreparationAndExplicitRollbackKeepOriginalData()throws Exception{
        Path active=active();var tx=WorldHopLibrary.begin(active,A);var failed=tx;assertThrows(IOException.class,()->WorldHopFiles.stage(active,failed.next(),p->{throw new IOException("Full disk");},false));WorldHopLibrary.rollback(active);assertEquals("A",Files.readString(active.resolve("level.dat")));emptyTransactions();
        tx=prepare(active);WorldHopLibrary.install(tx);WorldHopLibrary.rollback(active);assertEquals("A build",Files.readString(active.resolve("build.txt")));emptyTransactions();
    }
    @Test void recordedLegacySavesMoveInsideOwnerButUnrelatedSavesRemain()throws Exception{
        Path active=active(),legacy=Files.createDirectory(saves.resolve("Old Parallel")),other=Files.createDirectory(saves.resolve("Unrelated"));Files.writeString(legacy.resolve("level.dat"),"legacy");Files.writeString(other.resolve("level.dat"),"other");
        WorldHopLibrary.importWorld(active,legacy,B,true);assertFalse(Files.exists(legacy));assertEquals("legacy",Files.readString(WorldHopLibrary.archived(active,B).orElseThrow().resolve("level.dat")));assertEquals("other",Files.readString(other.resolve("level.dat")));
        assertThrows(IOException.class,()->WorldHopLibrary.importWorld(active,active,A,true));assertTrue(WorldHopLibrary.archived(active,"../Unrelated").isEmpty());
        Path outside=Files.createDirectory(saves.resolve("outside"));Files.createSymbolicLink(WorldHopLibrary.worlds(active).resolve(A),outside);assertTrue(WorldHopLibrary.archived(active,A).isEmpty());
    }
    @Test void recoveryRefusesToOverwriteAnUnrelatedReplacementSave()throws Exception{
        Path active=active();var tx=prepare(active);Files.move(active,tx.previous());Files.createDirectory(active);Files.writeString(active.resolve("level.dat"),"unrelated replacement");
        assertThrows(IOException.class,()->WorldHopLibrary.recover(saves));assertEquals("unrelated replacement",Files.readString(active.resolve("level.dat")));assertEquals("A",Files.readString(tx.previous().resolve("level.dat")));
    }
}
