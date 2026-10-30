package dev.heavenlyrestriction.worldhop;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** Build a fresh save without ever altering the previous world. No Minecraft/client dependencies. */
public final class WorldHopFiles {
    private static final Set<String> TERRAIN=Set.of("region","entities","poi");
    private static final Set<String> OMIT=Set.of("session.lock","level.dat_old","icon.png");
    @FunctionalInterface public interface Metadata {void rewrite(Path stagedWorld) throws IOException;}
    public static boolean keep(Path relative,boolean directory){
        String first=relative.getName(0).toString(),name=relative.getFileName().toString();
        // Only actual world-storage roots are regenerated. Datapacks may legitimately
        // contain folders named entities/region/poi and must remain byte-identical.
        if(TERRAIN.contains(first) || first.equals("dimensions") || first.matches("DIM-?\\d+"))return false;
        if(relative.getNameCount()==1 && OMIT.contains(name))return false;
        if(relative.getNameCount()==2 && first.equals("data") && (Set.of("random_sequences.dat","forcedchunks.dat","raids.dat","raids_end.dat","idcounts.dat").contains(name) || name.matches("map_\\d+\\.dat")))return false;
        return !Set.of("xaero","xaeroworldmap","journeymap","voxy","lod","distant_horizons","chunk_activity_info").contains(first.toLowerCase(Locale.ROOT));
    }
    public static Path fork(Path source,String destination,Metadata metadata) throws IOException {
        source=source.toRealPath();Path saves=source.getParent();
        if(!destination.matches("PWJ-[A-Za-z0-9_-]{1,100}"))throw new IOException("Invalid parallel-world directory");
        Path result=saves.resolve(destination),staging=saves.resolve(".pwj-"+UUID.randomUUID());
        if(Files.exists(result,LinkOption.NOFOLLOW_LINKS) || !Files.isRegularFile(source.resolve("level.dat"),LinkOption.NOFOLLOW_LINKS))throw new IOException("World unavailable or destination already exists");
        Files.createDirectory(staging);final Path original=source;
        try{
            Files.walkFileTree(original,new SimpleFileVisitor<>(){
                @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs)throws IOException{
                    Path rel=original.relativize(dir);if(!rel.toString().isEmpty() && !keep(rel,true))return FileVisitResult.SKIP_SUBTREE;
                    Files.createDirectories(staging.resolve(rel));return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs)throws IOException{
                    Path rel=original.relativize(file);if(!keep(rel,false))return FileVisitResult.CONTINUE;
                    if(attrs.isSymbolicLink() || !attrs.isRegularFile())throw new IOException("Unsupported linked world file: "+rel);
                    Files.copy(file,staging.resolve(rel));return FileVisitResult.CONTINUE;
                }
            });
            metadata.rewrite(staging);
            try{Files.move(staging,result,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(staging,result);}
            return result;
        }catch(IOException|RuntimeException ex){
            try{discardStaging(staging);}catch(IOException cleanup){ex.addSuppressed(cleanup);}
            throw ex;
        }
    }
    private static void discardStaging(Path root)throws IOException{
        Files.walkFileTree(root,new SimpleFileVisitor<>(){
            @Override public FileVisitResult visitFile(Path f,BasicFileAttributes a)throws IOException{Files.delete(f);return FileVisitResult.CONTINUE;}
            @Override public FileVisitResult postVisitDirectory(Path d,IOException e)throws IOException{if(e!=null)throw e;Files.delete(d);return FileVisitResult.CONTINUE;}
        });
    }
    public static Optional<Path> previous(Path saves,String folder){
        try{Path root=saves.toRealPath(),candidate=root.resolve(folder).normalize();if(folder.isBlank() || !candidate.getParent().equals(root) || !candidate.toRealPath().getParent().equals(root) || !Files.isRegularFile(candidate.resolve("level.dat"),LinkOption.NOFOLLOW_LINKS))return Optional.empty();return Optional.of(candidate.toRealPath());}catch(IOException|RuntimeException ex){return Optional.empty();}
    }
    public static boolean available(boolean integrated,boolean published,boolean owner,int players){return integrated && !published && owner && players==1;}
}
