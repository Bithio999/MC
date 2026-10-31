package dev.heavenlyrestriction.worldhop;
import java.nio.file.*;
import java.io.IOException;
public final class WorldHopSaveCompat {
    public record History(String name,long hop){}
    /** Native level.dat serialization drops unknown fields, so keep hop names in a sidecar. */
    public static History advanceHistory(Path world,String currentName,long seed,String previous)throws IOException{
        return advanceHistory(world,currentName,seed,previous,0);
    }
    public static History advanceHistory(Path world,String currentName,long seed,String previous,long libraryNumber)throws IOException{
        Path file=world.resolve("heavenlyrestriction-world-hop.json");
        try{
            var data=Files.isRegularFile(file)?com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject():new com.google.gson.JsonObject();
            String name=data.has("name")?data.get("name").getAsString():currentName.replaceFirst(" · Parallel \\d+$","");long hop=libraryNumber>0?libraryNumber:Math.addExact(Math.max(0,data.has("hop")?data.get("hop").getAsLong():0),1);
            data.addProperty("version",1);data.addProperty("name",name);data.addProperty("hop",hop);data.addProperty("seed",seed);data.addProperty("previous",previous);Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(data));return new History(name,hop);
        }catch(RuntimeException ex){throw new IOException("Could not preserve parallel-world history",ex);}
    }
    /** Starter Kit keys its single-player first-join record by save-folder name, not player UUID. */
    public static void carryStarterKitHistory(Path world,String previous,String continuation)throws IOException{
        Path file=world.resolve("data/starterkit/tracking.json");if(!Files.isRegularFile(file))return;
        try{
            var root=com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();var single=root.getAsJsonObject("singleplayer");
            if(single!=null && single.has(previous) && single.get(previous).isJsonPrimitive() && single.get(previous).getAsBoolean()){single.addProperty(continuation,true);Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));}
        }catch(RuntimeException ex){throw new IOException("Could not preserve Starter Kit first-join history",ex);}
    }
}
