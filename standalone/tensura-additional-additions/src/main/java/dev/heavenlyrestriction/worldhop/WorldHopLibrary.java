package dev.heavenlyrestriction.worldhop;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/** The active slot owns its complete library. Vanilla save deletion therefore deletes all of it. */
public final class WorldHopLibrary {
    public static final String DIRECTORY=".pwj-library";
    private static final String TRANSACTIONS=".pwj-transactions",RECEIPT="transaction.json",TRANSFER="heavenlyrestriction-transfer.txt";
    public record Transaction(Path root,Path active,String departing){public Path next(){return root.resolve("next");}public Path previous(){return root.resolve("previous");}}
    public static boolean pending(Path active){return Files.exists(active.resolve(TRANSFER),LinkOption.NOFOLLOW_LINKS);}
    private static void plainDirectory(Path directory)throws IOException{
        if(Files.isSymbolicLink(directory) || !Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))throw new IOException("Unsafe library directory");
    }
    private static void createDirectory(Path directory)throws IOException{
        if(!Files.exists(directory,LinkOption.NOFOLLOW_LINKS))Files.createDirectory(directory);plainDirectory(directory);
    }
    public static Path library(Path active)throws IOException{plainDirectory(active);Path library=active.resolve(DIRECTORY);createDirectory(library);return library;}
    public static Path worlds(Path active)throws IOException{Path worlds=library(active).resolve("worlds");createDirectory(worlds);return worlds;}
    public static Optional<Path> archived(Path active,String id){
        if(!WorldHopJournal.validId(id))return Optional.empty();
        try{Path lib=active.resolve(DIRECTORY),worlds=lib.resolve("worlds"),world=worlds.resolve(id);plainDirectory(active);plainDirectory(lib);plainDirectory(worlds);plainDirectory(world);
            if(!Files.isRegularFile(world.resolve("level.dat"),LinkOption.NOFOLLOW_LINKS))return Optional.empty();return Optional.of(world);
        }catch(IOException ex){return Optional.empty();}
    }
    public static Optional<Path> resolve(Path active,WorldHopJournal.Entry entry){
        var archived=archived(active,entry.id());return archived.isPresent()?archived:WorldHopFiles.previous(active.getParent(),entry.folder()).filter(p->!p.equals(active));
    }
    /** Move only a caller-validated, closed, recorded sibling save into this slot. */
    public static synchronized void importWorld(Path active,Path legacy,String id,boolean latest)throws IOException{
        if(!WorldHopJournal.validId(id))throw new IOException("Invalid world ID");
        active=active.toRealPath();legacy=legacy.toAbsolutePath().normalize();
        if(Files.isSymbolicLink(legacy) || !legacy.getParent().equals(active.getParent()) || legacy.equals(active))throw new IOException("Not a recorded sibling save");plainDirectory(legacy);
        if(!Files.isRegularFile(legacy.resolve("level.dat"),LinkOption.NOFOLLOW_LINKS))throw new IOException("Recorded world is missing");
        Path target=worlds(active).resolve(id);
        if(!latest || Files.exists(target,LinkOption.NOFOLLOW_LINKS)){Path recovery=library(active).resolve("recovery");createDirectory(recovery);target=recovery.resolve(UUID.randomUUID().toString());}
        move(legacy,target);
    }
    public static synchronized Transaction begin(Path active,String departing)throws IOException{
        active=active.toRealPath();if(!WorldHopJournal.validId(departing))throw new IOException("Invalid departing world");
        Path transactions=active.getParent().resolve(TRANSACTIONS);createDirectory(transactions);
        try(var entries=Files.list(transactions)){for(Path other:entries.toList())if(Files.isRegularFile(other.resolve(RECEIPT),LinkOption.NOFOLLOW_LINKS) && read(other).active.equals(active))throw new IOException("Previous travel still requires recovery");}
        Path root=Files.createDirectory(transactions.resolve(UUID.randomUUID().toString()));var json=new JsonObject();json.addProperty("save",active.getFileName().toString());json.addProperty("departing",departing);Files.writeString(root.resolve(RECEIPT),json.toString());return new Transaction(root,active,departing);
    }
    /** Two short directory renames. Receipt + previous slot allow recovery at every boundary. */
    public static synchronized void install(Transaction tx)throws IOException{
        plainDirectory(tx.next());if(!Files.isRegularFile(tx.next().resolve("level.dat"),LinkOption.NOFOLLOW_LINKS))throw new IOException("Prepared world is missing");
        Files.writeString(tx.next().resolve(TRANSFER),tx.root.getFileName().toString());
        move(tx.active,tx.previous());
        Path library=tx.previous().resolve(DIRECTORY);if(Files.exists(library,LinkOption.NOFOLLOW_LINKS)){plainDirectory(library);move(library,tx.next().resolve(DIRECTORY));}
        move(tx.next(),tx.active);
    }
    /** Called only once the real landing succeeds. The departed slot becomes its latest hidden snapshot. */
    public static synchronized void finish(Path active)throws IOException{
        for(var tx:transactions(active.getParent()))if(tx.active.equals(active.toAbsolutePath().normalize())){
            if(!belongs(tx.active,tx))throw new IOException("Active slot does not match the pending transfer");
            if(Files.exists(tx.previous(),LinkOption.NOFOLLOW_LINKS)){
                Path target=worlds(active).resolve(tx.departing);if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))move(target,tx.root.resolve("stale"));move(tx.previous(),target);
            }
            discard(tx.root);Files.deleteIfExists(active.resolve(TRANSFER));
        }
    }
    public static synchronized void rollback(Path active)throws IOException{for(var tx:transactions(active.getParent()))if(tx.active.equals(active.toAbsolutePath().normalize()))rollback(tx);}
    private static void rollback(Transaction tx)throws IOException{
        if(Files.exists(tx.previous(),LinkOption.NOFOLLOW_LINKS)){
            plainDirectory(tx.previous());Path library=tx.previous().resolve(DIRECTORY);
            if(Files.exists(tx.active,LinkOption.NOFOLLOW_LINKS) && !belongs(tx.active,tx))throw new IOException("The active slot was replaced outside World Jump; recovery left it untouched");
            if(!Files.exists(library,LinkOption.NOFOLLOW_LINKS)){
                Path from=Files.exists(tx.active,LinkOption.NOFOLLOW_LINKS)?tx.active:tx.next();Path moved=from.resolve(DIRECTORY);
                if(Files.exists(moved,LinkOption.NOFOLLOW_LINKS)){plainDirectory(moved);move(moved,library);}
            }
            if(Files.exists(tx.active,LinkOption.NOFOLLOW_LINKS))move(tx.active,tx.root.resolve("failed"));move(tx.previous(),tx.active);
        }
        // Without 'previous', either install never started or finish already archived it.
        if(!Files.isRegularFile(tx.active.resolve("level.dat"),LinkOption.NOFOLLOW_LINKS))throw new IOException("Travel recovery needs manual review: "+tx.root.getFileName());
        if(belongs(tx.active,tx))Files.deleteIfExists(tx.active.resolve(TRANSFER));discard(tx.root);
    }
    /** Run before showing Singleplayer, with no integrated server open. */
    public static synchronized void recover(Path saves)throws IOException{if(!Files.isDirectory(saves))return;for(var tx:transactions(saves))rollback(tx);}
    private static List<Transaction> transactions(Path saves)throws IOException{
        Path root=saves.toAbsolutePath().normalize().resolve(TRANSACTIONS);if(!Files.exists(root,LinkOption.NOFOLLOW_LINKS))return List.of();plainDirectory(root);
        var result=new ArrayList<Transaction>();try(var paths=Files.list(root)){for(Path path:paths.toList()){if(!WorldHopJournal.validId(path.getFileName().toString()))continue;plainDirectory(path);if(Files.isRegularFile(path.resolve(RECEIPT),LinkOption.NOFOLLOW_LINKS))result.add(read(path));}}return result;
    }
    private static Transaction read(Path root)throws IOException{
        plainDirectory(root);Path file=root.resolve(RECEIPT);if(Files.isSymbolicLink(file) || Files.size(file)>4096)throw new IOException("Invalid travel receipt");
        try{var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();String save=json.get("save").getAsString(),departing=json.get("departing").getAsString();
            if(save.isBlank() || save.contains("/") || save.contains("\\") || save.equals(".") || save.equals("..") || !WorldHopJournal.validId(departing))throw new IOException("Invalid travel receipt");
            return new Transaction(root,root.getParent().getParent().resolve(save),departing);
        }catch(RuntimeException ex){throw new IOException("Could not read travel receipt",ex);}
    }
    private static boolean belongs(Path world,Transaction tx)throws IOException{
        plainDirectory(world);Path marker=world.resolve(TRANSFER);return Files.isRegularFile(marker,LinkOption.NOFOLLOW_LINKS) && Files.size(marker)<100 && Files.readString(marker).equals(tx.root.getFileName().toString());
    }
    private static void move(Path source,Path target)throws IOException{WorldHopIo.retry(()->{try{return Files.move(source,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){return Files.move(source,target);}});}
    private static void discard(Path root)throws IOException{
        // Never follow links, even in failed/stale snapshots.
        Files.walkFileTree(root,new SimpleFileVisitor<>(){
            @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs)throws IOException{WorldHopIo.retry(()->Files.deleteIfExists(file));return FileVisitResult.CONTINUE;}
            @Override public FileVisitResult postVisitDirectory(Path dir,IOException error)throws IOException{if(error!=null)throw error;WorldHopIo.retry(()->Files.deleteIfExists(dir));return FileVisitResult.CONTINUE;}
        });
    }
}
