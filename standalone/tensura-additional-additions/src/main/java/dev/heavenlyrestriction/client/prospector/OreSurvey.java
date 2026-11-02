package dev.heavenlyrestriction.client.prospector;

import dev.heavenlyrestriction.ProspectorOres;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.*;
import java.util.*;

/** Incremental survey of already received chunk sections. Never requests chunks or scans during rendering. */
public final class OreSurvey {
    public static final int RADIUS=32,SECTION_BUDGET=4,CELL_BUDGET=8192,MAX_ORES=8192,MAX_MARKERS=96;
    public record Marker(BlockPos pos,ProspectorOres.Kind kind,int count){}
    private record Job(int x,int y,int z,double distance){}
    private static final Direction[] DIRECTIONS=Direction.values();
    private final Map<BlockPos,ProspectorOres.Kind> ores=new HashMap<>();
    private final ArrayDeque<Job> jobs=new ArrayDeque<>();
    private List<Marker> markers=List.of();private ClientLevel level;private BlockPos anchor;private int ticks,nextSweep,cells,sections;
    private final BlockPos.MutableBlockPos cursor=new BlockPos.MutableBlockPos();
    public List<Marker> markers(){return markers;}
    public int lastCells(){return cells;}public int lastSections(){return sections;}
    public void clear(){ores.clear();jobs.clear();markers=List.of();level=null;anchor=null;nextSweep=0;ticks=0;}
    public void tick(ClientLevel current,Vec3 player){if(level!=current){clear();level=current;}ticks++;cells=sections=0;
        if(anchor==null || anchor.distSqr(BlockPos.containing(player))>64){ores.clear();markers=List.of();start(player);}
        else if(jobs.isEmpty() && ticks>=nextSweep)start(player);
        while(!jobs.isEmpty() && sections<SECTION_BUDGET && cells+4096<=CELL_BUDGET){var j=jobs.removeFirst();sections++;
            ores.keySet().removeIf(p->p.getX()>>4==j.x && p.getY()>>4==j.y && p.getZ()>>4==j.z);
            var chunk=level.getChunkSource().getChunk(j.x,j.z,ChunkStatus.FULL,false);int index=j.y-level.getMinSection();if(chunk==null || index<0 || index>=chunk.getSections().length)continue;
            var section=chunk.getSections()[index];if(section.hasOnlyAir() || !section.maybeHas(s->ProspectorOres.kind(s)!=null))continue;
            for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){cells++;cursor.set((j.x<<4)+x,(j.y<<4)+y,(j.z<<4)+z);if(cursor.distToCenterSqr(player)>RADIUS*RADIUS)continue;var kind=ProspectorOres.kind(section.getBlockState(x,y,z));if(kind!=null && ores.size()<MAX_ORES)ores.put(cursor.immutable(),kind);}
        }
        ores.entrySet().removeIf(entry->entry.getKey().distToCenterSqr(player)>RADIUS*RADIUS || !stillOre(entry.getKey(),entry.getValue()));
        if(ticks%5==0 || markers.isEmpty())group(player);
    }
    private boolean stillOre(BlockPos p,ProspectorOres.Kind expected){var chunk=level.getChunkSource().getChunk(p.getX()>>4,p.getZ()>>4,ChunkStatus.FULL,false);if(chunk==null)return false;var kind=ProspectorOres.kind(chunk.getBlockState(p));return kind!=null && kind.family().equals(expected.family());}
    private void start(Vec3 player){anchor=BlockPos.containing(player);jobs.clear();var sorted=new ArrayList<Job>();int minY=Math.max(level.getMinSection(),(anchor.getY()-RADIUS)>>4),maxY=Math.min(level.getMaxSection()-1,(anchor.getY()+RADIUS)>>4);
        for(int y=minY;y<=maxY;y++)for(int z=(anchor.getZ()-RADIUS)>>4;z<=(anchor.getZ()+RADIUS)>>4;z++)for(int x=(anchor.getX()-RADIUS)>>4;x<=(anchor.getX()+RADIUS)>>4;x++){var box=new AABB(x*16,y*16,z*16,x*16+16,y*16+16,z*16+16);double d=box.distanceToSqr(player);if(d<=RADIUS*RADIUS)sorted.add(new Job(x,y,z,d));}
        sorted.sort(Comparator.comparingDouble(Job::distance));jobs.addAll(sorted);nextSweep=ticks+40;
    }
    private void group(Vec3 player){var pending=new HashSet<>(ores.keySet());var result=new ArrayList<Marker>();var queue=new ArrayDeque<BlockPos>();
        for(var first:ores.keySet()){if(!pending.remove(first))continue;var kind=ores.get(first);queue.add(first);BlockPos nearest=first;int count=0;
            while(!queue.isEmpty()){var at=queue.removeFirst();count++;if(at.distToCenterSqr(player)<nearest.distToCenterSqr(player))nearest=at;for(var dir:DIRECTIONS){var adjacent=at.relative(dir);var other=ores.get(adjacent);if(other!=null && other.family().equals(kind.family()) && pending.remove(adjacent))queue.addLast(adjacent);}}
            result.add(new Marker(nearest,ores.get(nearest),count));
        }
        result.sort(Comparator.comparingDouble(m->m.pos.distToCenterSqr(player)));markers=List.copyOf(result.subList(0,Math.min(MAX_MARKERS,result.size())));
    }
}
