package dev.heavenlyrestriction;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.registry.race.TensuraRaces;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native fixtures deliberately use only vanilla/Tensura entities and races. */
final class TestFixtures {
    private static final java.util.concurrent.atomic.AtomicInteger ARENAS=new java.util.concurrent.atomic.AtomicInteger();
    static ServerPlayer player(GameTestHelper h){
        var origin=h.absolutePos(BlockPos.ZERO);Vec3 at=new Vec3(origin.getX()+7000+ARENAS.getAndIncrement()*256,160,origin.getZ()+7000);
        for(int x=-1;x<=2;x++)for(int z=-1;z<=1;z++){var pos=BlockPos.containing(at).offset(x*16,0,z*16);h.getLevel().getChunk(pos);h.getLevel().setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);}
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"RestrictionTest"));
        p.setPos(at);p.setYRot(-90);p.setXRot(0);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        RaceAPI.getRaceFrom(p).setRace(TensuraRaces.HUMAN.get(),false);EnergyHelper.setBaseMaxEP(p,20_000_000);
        var energy=TensuraStorages.getExistenceFrom(p);energy.setTrueHero(true);energy.setAura(1_000_000);energy.setMagicule(1_000_000);return p;
    }
    @SuppressWarnings("unchecked")
    static List<ServerPlayer> reviewRoster(ServerPlayer p) {
        try {
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            field.setAccessible(true);
            return (List<ServerPlayer>)field.get(p.server.getPlayerList());
        } catch(ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
    }
    static net.minecraft.world.entity.monster.Husk target(GameTestHelper h,Vec3 at){
        var e=new net.minecraft.world.entity.monster.Husk(net.minecraft.world.entity.EntityType.HUSK,h.getLevel());e.setPos(at);e.setNoAi(true);e.setNoGravity(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);e.setHealth(1024);h.getLevel().addFreshEntity(e);return e;
    }
    static void afterTracking(GameTestHelper h,List<? extends LivingEntity> targets,Runnable checks){
        h.runAfterDelay(5,()->h.startSequence().thenWaitUntil(()->{for(var target:targets)h.assertTrue(h.getLevel().getEntity(target.getUUID())==target && h.getLevel().getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(.1)).contains(target),"Target is not tracked yet");}).thenExecute(checks));
    }
}
