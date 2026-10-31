package dev.heavenlyrestriction.worldhop;
import dev.heavenlyrestriction.HeavenlyRestriction;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class WorldHopNetwork {
    public static Consumer<UUID> receiver=id->{};
    public static Consumer<Boolean> statusReceiver=ok->{};
    public static Consumer<CompoundTag> worldsReceiver=state->{};
    public record Worlds(CompoundTag state) implements CustomPacketPayload {
        public static final Type<Worlds> TYPE=new Type<>(HeavenlyRestriction.id("world_select_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Worlds> CODEC=StreamCodec.of((b,p)->b.writeNbt(p.state),b->new Worlds(b.readNbt()));
        @Override public Type<Worlds> type(){return TYPE;}
    }
    public record Select(String id) implements CustomPacketPayload {
        public static final Type<Select> TYPE=new Type<>(HeavenlyRestriction.id("world_select"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Select> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.id,36),b->new Select(b.readUtf(36)));
        @Override public Type<Select> type(){return TYPE;}
    }
    public record Status(boolean success) implements CustomPacketPayload {
        public static final Type<Status> TYPE=new Type<>(HeavenlyRestriction.id("world_hop_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Status> CODEC=StreamCodec.of((b,p)->b.writeBoolean(p.success),b->new Status(b.readBoolean()));
        @Override public Type<Status> type(){return TYPE;}
    }
    public record Ready(UUID token) implements CustomPacketPayload {
        public static final Type<Ready> TYPE=new Type<>(HeavenlyRestriction.id("world_hop_ready"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Ready> CODEC=StreamCodec.of((b,p)->b.writeUUID(p.token),b->new Ready(b.readUUID()));
        @Override public Type<Ready> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("1");r.playToClient(Ready.TYPE,Ready.CODEC,(p,c)->c.enqueueWork(()->receiver.accept(p.token)));r.playToClient(Status.TYPE,Status.CODEC,(p,c)->c.enqueueWork(()->statusReceiver.accept(p.success)));
        r.playToClient(Worlds.TYPE,Worlds.CODEC,(p,c)->c.enqueueWork(()->worldsReceiver.accept(p.state)));
        r.playToServer(Select.TYPE,Select.CODEC,(payload,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer p){boolean ok=WorldSelect.select(p,payload.id);WorldSelect.send(p,false,ok?"Next jump updated.":"World unavailable. Reopen World Select.");}}));
    }
    public static void send(ServerPlayer p,UUID token){PacketDistributor.sendToPlayer(p,new Ready(token));}
}
