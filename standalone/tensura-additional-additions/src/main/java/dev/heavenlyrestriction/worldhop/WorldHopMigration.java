package dev.heavenlyrestriction.worldhop;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Follow recorded travel links only. Unrelated saves are never scanned or adopted. */
public final class WorldHopMigration {
    @FunctionalInterface public interface Validate {void closedWorld(Path world,long expectedSeed)throws Exception;}
    private record Legacy(Path path,WorldHopJournal.Entry entry,boolean latest){}
    public static void migrate(Path active,WorldHopJournal.Journal journal,Validate validate)throws Exception{
        var latest=new HashMap<Path,WorldHopJournal.Entry>();var queue=new ArrayDeque<Path>();queue.add(active);
        for(var e:journal.worlds())if(!e.id().equals(journal.current()) && WorldHopLibrary.archived(active,e.id()).isEmpty())WorldHopFiles.previous(active.getParent(),e.folder()).filter(p->!p.equals(active)).ifPresent(p->{latest.put(p,e);queue.add(p);});
        var seen=new HashSet<Path>();var imports=new ArrayList<Legacy>();
        while(!queue.isEmpty()){
            Path path=queue.remove();if(!seen.add(path))continue;if(seen.size()>4096)throw new IOException("Travel history needs manual review");
            var data=WorldHopMetadata.read(path.resolve("level.dat")).getCompound("Data");long seed=data.getCompound("WorldGenSettings").getLong("seed");
            var old=WorldHopJournal.read(path,data.getString("LevelName"),seed);
            if(!path.equals(active)){
                var entry=latest.get(path);boolean isLatest=entry!=null;
                if(entry==null)entry=journal.find(old.current()).filter(e->e.seed()==seed).orElse(null);
                if(entry==null || entry.seed()!=seed)continue;
                imports.add(new Legacy(path,entry,isLatest));
            }
            for(var e:old.worlds())if(WorldHopLibrary.archived(active,e.id()).isEmpty())WorldHopFiles.previous(active.getParent(),e.folder()).filter(p->!seen.contains(p)).ifPresent(queue::add);
            Path sidecar=path.resolve("heavenlyrestriction-world-hop.json");
            if(!Files.exists(path.resolve(WorldHopLibrary.DIRECTORY),LinkOption.NOFOLLOW_LINKS) && Files.isRegularFile(sidecar,LinkOption.NOFOLLOW_LINKS) && Files.size(sidecar)<1024*1024){
                try{var json=com.google.gson.JsonParser.parseString(Files.readString(sidecar)).getAsJsonObject();if(json.has("previous"))WorldHopFiles.previous(active.getParent(),json.get("previous").getAsString()).filter(p->!seen.contains(p)).ifPresent(queue::add);}
                catch(RuntimeException ex){throw new IOException("Could not read legacy travel history",ex);}
            }
        }
        // Check every source before moving anything. Native validation refuses worlds open elsewhere.
        for(var legacy:imports)validate.closedWorld(legacy.path,legacy.entry.seed());
        WorldHopJournal.write(active,journal);
        imports.sort(Comparator.comparing((Legacy e)->!e.latest));
        for(var legacy:imports)WorldHopLibrary.importWorld(active,legacy.path,legacy.entry.id(),legacy.latest);
        WorldHopJournal.write(active,journal);
    }
}
