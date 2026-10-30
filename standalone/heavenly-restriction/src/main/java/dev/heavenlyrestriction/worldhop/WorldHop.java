package dev.heavenlyrestriction.worldhop;

import dev.heavenlyrestriction.*;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class WorldHop {
    public record Ticket(UUID token,Path source,long seed,UUID player){}
    private static final Map<MinecraftServer,Ticket> REQUESTS=new ConcurrentHashMap<>();
    private static final Map<UUID,Landing> LANDINGS=new HashMap<>();
    public static Predicate<LivingEntity> clientAvailable=e->false;
    public static boolean available(LivingEntity e){
        if(e==null)return false;if(e.level().isClientSide())return clientAvailable.test(e);
        if(!(e instanceof ServerPlayer p))return false;var server=p.server;
        return WorldHopFiles.available(server.isSingleplayer(),server.isPublished(),server.isSingleplayerOwner(p.getGameProfile()),server.getPlayerList().getPlayerCount());
    }
    public static boolean mayStore(ManasSkillInstance i,LivingEntity e){return !(i.getSkill() instanceof ParallelWorldJumperSkill) || e.level().isClientSide() || available(e);}
    public static void request(ServerPlayer p,ManasSkillInstance i){
        if(!available(p)){p.sendSystemMessage(Component.literal("World Hop is only available in a private single-player world."));return;}
        if(p.getPersistentData().contains(WorldHopMetadata.MARKER) || REQUESTS.containsKey(p.server))return;
        long old=p.server.getWorldData().worldGenOptions().seed(),seed;do{seed=new java.security.SecureRandom().nextLong();}while(seed==old);
        var ticket=new Ticket(UUID.randomUUID(),p.server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize(),seed,p.getUUID());
        if(REQUESTS.putIfAbsent(p.server,ticket)!=null)return;
        p.unRide();p.closeContainer();i.addMasteryPoint(p);
        WorldHopNetwork.send(p,ticket.token());
    }
    /** A remote server cannot create or claim a local transfer ticket. */
    public static Optional<Ticket> claim(MinecraftServer server,UUID token){
        var ticket=REQUESTS.get(server);if(ticket==null || !ticket.token().equals(token) || !server.isSingleplayer() || server.isPublished() || server.getPlayerList().getPlayerCount()!=1)return Optional.empty();
        return REQUESTS.remove(server,ticket)?Optional.of(ticket):Optional.empty();
    }
    public static void register(){
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e)->{REQUESTS.remove(e.getServer());LANDINGS.entrySet().removeIf(v->v.getValue().player.server==e.getServer());});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e)->{if(e.getEntity() instanceof ServerPlayer p){var landing=LANDINGS.remove(p.getUUID());if(landing!=null)landing.restore();}});
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof ServerPlayer p && p.getPersistentData().contains(WorldHopMetadata.MARKER)){
            if(!available(p)){var saved=p.getPersistentData().getCompound(WorldHopMetadata.MARKER);if(saved.contains("Invulnerable"))p.setInvulnerable(saved.getBoolean("Invulnerable"));if(saved.contains("NoGravity"))p.setNoGravity(saved.getBoolean("NoGravity"));p.getPersistentData().remove(WorldHopMetadata.MARKER);return;}
            LANDINGS.put(p.getUUID(),new Landing(p));
        }});
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e)->{
            for(var landing:List.copyOf(LANDINGS.values()))if(landing.player.server==e.getServer()){
                try{if(landing.tick())LANDINGS.remove(landing.player.getUUID());}catch(RuntimeException ex){landing.fallback();}
            }
        });
    }
    public static boolean safe(ServerLevel level,ServerPlayer p,BlockPos feet){
        if(feet.getY()<level.getMinBuildHeight()+1 || feet.getY()+Math.ceil(p.getBbHeight())>=level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(feet))return false;
        var floor=level.getBlockState(feet.below());
        if(!floor.isFaceSturdy(level,feet.below(),Direction.UP) || !floor.getFluidState().isEmpty())return false;
        for(Block b:new Block[]{Blocks.MAGMA_BLOCK,Blocks.CACTUS,Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE,Blocks.POWDER_SNOW,Blocks.WITHER_ROSE})if(floor.is(b))return false;
        var box=p.getBoundingBox().move(feet.getX()+.5-p.getX(),feet.getY()-p.getY(),feet.getZ()+.5-p.getZ());
        if(level.getBlockState(feet).is(Blocks.SWEET_BERRY_BUSH) || level.getBlockState(feet).is(Blocks.WITHER_ROSE) || level.getBlockState(feet).is(Blocks.POWDER_SNOW))return false;
        return level.noCollision(p,box) && !level.containsAnyLiquid(box) && !level.getBlockState(feet).is(Blocks.FIRE) && !level.getBlockState(feet).is(Blocks.SOUL_FIRE);
    }
    private static final class Landing {
        final ServerPlayer player;ServerLevel level;Vec3 target;final boolean invulnerable,noGravity;
        final List<ChunkPos> chunks=new ArrayList<>();int next;BlockPos best;double bestDistance=Double.POSITIVE_INFINITY;boolean fallback,failed;
        Landing(ServerPlayer p){
            player=p;var marker=p.getPersistentData().getCompound(WorldHopMetadata.MARKER);var pos=marker.getList("Pos",net.minecraft.nbt.Tag.TAG_DOUBLE);
            target=pos.size()==3?new Vec3(pos.getDouble(0),pos.getDouble(1),pos.getDouble(2)):p.position();
            var id=ResourceLocation.tryParse(marker.getString("Dimension"));level=id==null?null:p.server.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id));if(level==null)level=p.server.overworld();
            invulnerable=marker.contains("Invulnerable")?marker.getBoolean("Invulnerable"):p.isInvulnerable();noGravity=marker.contains("NoGravity")?marker.getBoolean("NoGravity"):p.isNoGravity();
            marker.putBoolean("Invulnerable",invulnerable);marker.putBoolean("NoGravity",noGravity);p.setInvulnerable(true);p.setNoGravity(true);prepare();
        }
        void prepare(){
            chunks.clear();next=0;best=null;bestDistance=Double.POSITIVE_INFINITY;
            double limit=Math.max(0,Math.min(29_999_000,level.getWorldBorder().getSize()/2-2));target=new Vec3(Math.clamp(target.x,level.getWorldBorder().getCenterX()-limit,level.getWorldBorder().getCenterX()+limit),target.y,Math.clamp(target.z,level.getWorldBorder().getCenterZ()-limit,level.getWorldBorder().getCenterZ()+limit));
            int cx=net.minecraft.util.Mth.floor(target.x)>>4,cz=net.minecraft.util.Mth.floor(target.z)>>4;
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)chunks.add(new ChunkPos(cx+dx,cz+dz));chunks.sort(Comparator.comparingDouble(this::minimumDistance));
            player.teleportTo(level,target.x,level.getMaxBuildHeight()+16,target.z,Set.of(),player.getYRot(),player.getXRot());
        }
        double minimumDistance(ChunkPos c){double x=Math.clamp(target.x,c.getMinBlockX()+.5,c.getMaxBlockX()+.5),z=Math.clamp(target.z,c.getMinBlockZ()+.5,c.getMaxBlockZ()+.5);return (x-target.x)*(x-target.x)+(z-target.z)*(z-target.z);}
        boolean tick(){
            player.setDeltaMovement(Vec3.ZERO);player.resetFallDistance();if(failed)return false;
            if(next>=chunks.size() || best!=null && minimumDistance(chunks.get(next))>bestDistance){if(best!=null){finish();return true;}fallback();return false;}
            ChunkPos chunk=chunks.get(next++);level.getChunk(chunk.x,chunk.z);
            for(int x=chunk.getMinBlockX();x<=chunk.getMaxBlockX();x++)for(int z=chunk.getMinBlockZ();z<=chunk.getMaxBlockZ();z++){
                double distance=(x+.5-target.x)*(x+.5-target.x)+(z+.5-target.z)*(z+.5-target.z);if(distance>=bestDistance)continue;
                int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                BlockPos pos=new BlockPos(x,y,z);
                if(level.dimensionType().hasCeiling()){
                    int center=(int)Math.clamp(target.y,level.getMinBuildHeight()+1,level.getMinBuildHeight()+level.dimensionType().logicalHeight()-3);pos=null;
                    for(int d=0;d<level.dimensionType().logicalHeight() && pos==null;d++)for(int yy:new int[]{center-d,center+d})if(yy<level.getMinBuildHeight()+level.dimensionType().logicalHeight()-2 && safe(level,player,new BlockPos(x,yy,z)))pos=new BlockPos(x,yy,z);
                }
                if(pos!=null && safe(level,player,pos)){best=pos;bestDistance=distance;}
            }
            if(best!=null && (next==chunks.size() || minimumDistance(chunks.get(next))>bestDistance)){finish();return true;}
            return false;
        }
        void fallback(){
            if(!fallback){fallback=true;level=player.server.overworld();target=Vec3.atCenterOf(level.getSharedSpawnPos());prepare();player.sendSystemMessage(Component.literal("No safe ground nearby. World Hop is finding an Overworld landing."));}
            else if(chunks.size()>81){failed=true;net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new WorldHopNetwork.Status(false));}
            else {next=0;chunks.clear();int cx=net.minecraft.util.Mth.floor(target.x)>>4,cz=net.minecraft.util.Mth.floor(target.z)>>4;int radius=8;for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++)chunks.add(new ChunkPos(cx+dx,cz+dz));chunks.sort(Comparator.comparingDouble(this::minimumDistance));}
        }
        void restore(){player.setInvulnerable(invulnerable);player.setNoGravity(noGravity);}
        void finish(){net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new WorldHopNetwork.Status(true));player.teleportTo(level,best.getX()+.5,best.getY(),best.getZ()+.5,Set.of(),player.getYRot(),player.getXRot());player.resetFallDistance();player.setDeltaMovement(Vec3.ZERO);restore();player.getPersistentData().remove(WorldHopMetadata.MARKER);player.sendSystemMessage(Component.literal("World Hop complete. Your previous world is still available in Singleplayer."));}
    }
}
