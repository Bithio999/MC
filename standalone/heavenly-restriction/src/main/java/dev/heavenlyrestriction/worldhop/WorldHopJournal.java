package dev.heavenlyrestriction.worldhop;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Per-continuation itinerary. IDs are opaque; network clients never supply filesystem paths. */
public final class WorldHopJournal {
    public static final String FILE="heavenlyrestriction-worlds.json";
    public static final int LIMIT=512;
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
                var list=root.getAsJsonArray("worlds");if(list.size()>LIMIT)throw new IOException("World journal is full");Set<String> ids=new HashSet<>();
                for(var element:list){var e=element.getAsJsonObject();String key=e.get("id").getAsString(),folder=e.get("folder").getAsString(),label=e.get("name").getAsString();
                    if(!validId(key) || !ids.add(key) || folder.isBlank() || folder.length()>255 || folder.contains("/") || folder.contains("\\") || folder.equals(".") || folder.equals("..") || label.length()>512)throw new IOException("Invalid visited world");
                    entries.add(new Entry(key,folder,label,e.get("seed").getAsLong()));
                }
            }catch(RuntimeException ex){throw new IOException("Could not read visited worlds",ex);}
        }
        String key=current;entries.removeIf(e->e.id.equals(key));entries.addFirst(new Entry(current,world.getFileName().toString(),name,seed));
        if(entries.size()>LIMIT)throw new IOException("World journal is full");return new Journal(current,entries);
    }
    public static Journal arrive(Journal from,Entry destination,String folder){
        var entries=new ArrayList<>(from.worlds);entries.removeIf(e->e.id.equals(destination.id));entries.addFirst(new Entry(destination.id,folder,destination.name,destination.seed));return new Journal(destination.id,entries);
    }
    public static void write(Path world,Journal journal)throws IOException{
        if(journal.worlds.size()>LIMIT)throw new IOException("World journal is full");
        var root=new JsonObject();root.addProperty("version",1);root.addProperty("current",journal.current);var entries=new JsonArray();
        for(var e:journal.worlds){var row=new JsonObject();row.addProperty("id",e.id);row.addProperty("folder",e.folder);row.addProperty("name",e.name);row.addProperty("seed",e.seed);entries.add(row);}root.add("worlds",entries);
        Files.writeString(world.resolve(FILE),new GsonBuilder().setPrettyPrinting().create().toJson(root));
    }
}
