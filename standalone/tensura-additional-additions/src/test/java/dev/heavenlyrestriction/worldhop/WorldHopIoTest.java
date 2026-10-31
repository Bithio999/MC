package dev.heavenlyrestriction.worldhop;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class WorldHopIoTest {
    @TempDir Path directory;
    @Test void temporaryFileAccessFailureRetriesAndMovesTheSavedDataOnce()throws Exception{
        Path source=directory.resolve("old"),target=directory.resolve("new");Files.writeString(source,"saved world data");var attempts=new AtomicInteger();
        WorldHopIo.retry(()->{if(attempts.incrementAndGet()<3)throw new AccessDeniedException(source.toString());return Files.move(source,target);});
        assertEquals(3,attempts.get());assertFalse(Files.exists(source));assertEquals("saved world data",Files.readString(target));
    }
    @Test void permanentFailuresDoNotRetryAndPersistentLocksHaveABoundedBudget(){
        var attempts=new AtomicInteger();assertThrows(NoSuchFileException.class,()->WorldHopIo.retry(()->{attempts.incrementAndGet();throw new NoSuchFileException("missing");}));assertEquals(1,attempts.get());
        attempts.set(0);assertThrows(FileAlreadyExistsException.class,()->WorldHopIo.retry(()->{attempts.incrementAndGet();throw new FileAlreadyExistsException("existing");}));assertEquals(1,attempts.get());
        attempts.set(0);assertThrows(AccessDeniedException.class,()->WorldHopIo.retry(()->{attempts.incrementAndGet();throw new AccessDeniedException("locked");}));assertEquals(6,attempts.get());
    }
    @Test void recoveryReasonUnwrapsAsyncErrorsAndKeepsTheFilesystemReasonReadable(){
        assertEquals("A save file is locked or access was denied.",WorldHopIo.describe(new CompletionException(new AccessDeniedException("private path"))));
        assertEquals("No space left on device",WorldHopIo.describe(new CompletionException(new FileSystemException("save",null,"No space left on device"))));
        assertEquals("World or single-player character data is missing",WorldHopIo.describe(new java.io.IOException("World or single-player character data is missing")));
    }
}
