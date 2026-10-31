package dev.heavenlyrestriction.worldhop;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Per-continuation itinerary. IDs are opaque; network clients never supply filesystem paths. */
public final class WorldHopJournal {
    public static final String FILE="heavenlyrestriction-worlds.json";
    public static final int LIMIT=128;
    // Read old libraries without discarding worlds that predate the new creation cap.
    public static final int READ_LIMIT=512;
    public static boolean canCreate(Journal journal){return journal.worlds.size()<LIMIT;}
    private static final java.util.regex.Pattern PARALLEL=java.util.regex.Pattern.compile("^(.*) · Parallel (\\d{1,9})$");
    /** Count the whole library, rather than the sidecar of the world we happened to revisit. */
    public static long nextParallelNumber(Journal journal){long next=journal.worlds.size();for(var entry:journal.worlds){var match=PARALLEL.matcher(entry.name);if(match.matches())next=Math.max(next,Long.parseLong(match.group(2))+1);}return next;}
    public record Entry(String id,String folder,String name,long seed){}
    public record Journal(String current,List<Entry> worlds){
        public Journal{worlds=List.copyOf(worlds);}
        public Optional<Entry> find(String id){return worlds.stream().filter(e->e.id.equals(id)).findFirst();}
    }
    public static String id(String folder){return UUID.nameUUIDFromBytes(folder.getBytes(StandardCharsets.UTF_8)).toString();}
    public static boolean validId(String id){try{return UUID.fromString(id).toString().equals(id);}catch(RuntimeException ex){return false;}}
    public static Journal read(Path world,String name,long seed)throws IOException{
        Path file=world.resolve(FILE);List<Entry> entries=new ArrayList<>();String current=id(world.getFileName().toString());
        if(Files.exists(file,LinkOption.NOFOLLOW_LINKS)){
            if(Files.isSymbolicLink(file) || Files.size(file)>1024*1024)throw new IOException("Invalid world journal");
            try{
                var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();current=root.get("current").getAsString();if(!validId(current))throw new IOException("Invalid current world ID");
                var list=root.getAsJsonArray("worlds");if(list.size()>READ_LIMIT)throw new IOException("World journal is full");Set<String> ids=new HashSet<>();
                for(var element:list){var e=element.getAsJsonObject();String key=e.get("id").getAsString(),folder=e.get("folder").getAsString(),label=e.get("name").getAsString();
                    if(!validId(key) || !ids.add(key) || folder.isBlank() || folder.length()>255 || folder.contains("/") || folder.contains("\\") || folder.equals(".") || folder.equals("..") || label.length()>512)throw new IOException("Invalid visited world");
                    entries.add(new Entry(key,folder,label,e.get("seed").getAsLong()));
                }
            }catch(RuntimeException ex){throw new IOException("Could not read visited worlds",ex);}
        }
        String key=current;entries.removeIf(e->e.id.equals(key));entries.addFirst(new Entry(current,world.getFileName().toString(),name,seed));
        if(entries.size()>READ_LIMIT)throw new IOException("World journal is full");
        // Repair old duplicate generated labels while preserving IDs, seeds and unique/custom names.
        Set<String> reserved=new HashSet<>(),seen=new HashSet<>();for(var entry:entries)reserved.add(entry.name);
        for(int n=0;n<entries.size();n++){var entry=entries.get(n);if(seen.add(entry.name))continue;var match=PARALLEL.matcher(entry.name);if(!match.matches())continue;
            int serial=1;String label;do{label=match.group(1)+" · Parallel "+serial++;}while(reserved.contains(label));
            reserved.add(label);seen.add(label);entries.set(n,new Entry(entry.id,entry.folder,label,entry.seed));
        }
        return new Journal(current,entries);
    }
    public static Journal arrive(Journal from,Entry destination,String folder){
        if(from.find(destination.id).isEmpty() && !canCreate(from))throw new IllegalStateException("The 128-world library is full");
        var entries=new ArrayList<>(from.worlds);entries.removeIf(e->e.id.equals(destination.id));entries.addFirst(new Entry(destination.id,folder,destination.name,destination.seed));return new Journal(destination.id,entries);
    }
    public static void write(Path world,Journal journal)throws IOException{
        if(journal.worlds.size()>READ_LIMIT)throw new IOException("World journal is full");
        var root=new JsonObject();root.addProperty("version",1);root.addProperty("current",journal.current);var entries=new JsonArray();
        for(var e:journal.worlds){var row=new JsonObject();row.addProperty("id",e.id);row.addProperty("folder",e.folder);row.addProperty("name",e.name);row.addProperty("seed",e.seed);entries.add(row);}root.add("worlds",entries);
        Path temporary=world.resolve(".pwj-journal-"+UUID.randomUUID()+".tmp");
        try{Files.writeString(temporary,new GsonBuilder().setPrettyPrinting().create().toJson(root));
            try{Files.move(temporary,world.resolve(FILE),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ex){Files.move(temporary,world.resolve(FILE),StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
}
