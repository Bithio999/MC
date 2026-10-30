package dev.heavenlyrestriction.worldhop;
import dev.heavenlyrestriction.HeavenlyRestriction;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class WorldHopNetwork {
    public static Consumer<UUID> receiver=id->{};
    public static Consumer<Boolean> statusReceiver=ok->{};
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
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("1");r.playToClient(Ready.TYPE,Ready.CODEC,(p,c)->c.enqueueWork(()->receiver.accept(p.token)));r.playToClient(Status.TYPE,Status.CODEC,(p,c)->c.enqueueWork(()->statusReceiver.accept(p.success)));}
    public static void send(ServerPlayer p,UUID token){PacketDistributor.sendToPlayer(p,new Ready(token));}
}
