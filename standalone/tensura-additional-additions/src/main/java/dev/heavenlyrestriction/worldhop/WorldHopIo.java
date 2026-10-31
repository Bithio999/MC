package dev.heavenlyrestriction.worldhop;

import java.io.*;
import java.nio.file.*;

/** Bounded retries for briefly shared/locked save files. Never repeats a whole world transfer. */
public final class WorldHopIo {
    @FunctionalInterface public interface Operation<T>{T run()throws IOException;}
    public static <T> T retry(Operation<T> operation)throws IOException{
        for(int attempt=0;;attempt++){
            try{return operation.run();}
            catch(FileSystemException ex){
                if(attempt>=5 || ex instanceof NoSuchFileException || ex instanceof FileAlreadyExistsException || ex instanceof DirectoryNotEmptyException || ex instanceof NotDirectoryException || ex instanceof FileSystemLoopException)throw ex;
                try{Thread.sleep(25L<<attempt);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();var stopped=new InterruptedIOException("World Jump file access was interrupted");stopped.initCause(ex);throw stopped;}
            }
        }
    }
    public static String describe(Throwable error){
        for(int n=0;n<8 && error.getCause()!=null && error.getCause()!=error;n++)error=error.getCause();
        String reason;
        if(error instanceof AccessDeniedException)reason="A save file is locked or access was denied.";
        else if(error instanceof NoSuchFileException)reason="A required save file is missing or disappeared during transfer.";
        else if(error instanceof FileAlreadyExistsException)reason="The destination storage path already exists.";
        else if(error instanceof FileSystemException fs && fs.getReason()!=null)reason=fs.getReason();
        else reason=error.getMessage();
        if(reason==null || reason.isBlank())reason=error.getClass().getSimpleName();
        reason=reason.replaceAll("\\s+"," ").strip();return reason.length()>180?reason.substring(0,177)+"…":reason;
    }
}
