package dev.heavenlyrestriction.fusion;
import dev.heavenlyrestriction.HeavenlyRestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;
import java.util.function.Consumer;
public final class FusionNetwork {
    public static Consumer<CompoundTag> receiver=t->{};
    public record State(CompoundTag data) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(HeavenlyRestriction.id("fusion_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->b.writeNbt(p.data),b->new State(b.readNbt()));
        public Type<State> type(){return TYPE;}
    }
    public record Reply(UUID nonce,boolean accept) implements CustomPacketPayload {
        public static final Type<Reply> TYPE=new Type<>(HeavenlyRestriction.id("fusion_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.nonce);b.writeBoolean(p.accept);},b->new Reply(b.readUUID(),b.readBoolean()));
        public Type<Reply> type(){return TYPE;}
    }
    public record End() implements CustomPacketPayload {
        public static final Type<End> TYPE=new Type<>(HeavenlyRestriction.id("fusion_end"));
        public static final StreamCodec<RegistryFriendlyByteBuf,End> CODEC=StreamCodec.unit(new End());
        public Type<End> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("1");
        r.playToClient(State.TYPE,State.CODEC,(p,c)->c.enqueueWork(()->receiver.accept(p.data)));
        r.playToServer(Reply.TYPE,Reply.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)Fusion.reply(player,p.nonce,p.accept);}));
        r.playToServer(End.TYPE,End.CODEC,(p,c)->c.enqueueWork(()->Fusion.end(c.player(),"Fusion ended.")));
    }
    public static void send(ServerPlayer p,CompoundTag data){PacketDistributor.sendToPlayer(p,new State(data));}
}
