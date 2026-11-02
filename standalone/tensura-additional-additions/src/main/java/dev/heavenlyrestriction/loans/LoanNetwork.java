package dev.heavenlyrestriction.loans;
import dev.heavenlyrestriction.HeavenlyRestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;
import java.util.function.Consumer;
public final class LoanNetwork {
    public static Consumer<CompoundTag> receiver=t->{};
    public record State(CompoundTag data) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(HeavenlyRestriction.id("loan_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->b.writeNbt(p.data),b->new State(b.readNbt()));
        public Type<State> type(){return TYPE;}
    }
    public record Request(UUID session,String action,String value,String query,int page) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(HeavenlyRestriction.id("loan_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.session);b.writeUtf(p.action,12);b.writeUtf(p.value,256);b.writeUtf(p.query,80);b.writeVarInt(p.page);},b->new Request(b.readUUID(),b.readUtf(12),b.readUtf(256),b.readUtf(80),b.readVarInt()));
        public Type<Request> type(){return TYPE;}
    }
    public record Reply(UUID nonce,boolean accept) implements CustomPacketPayload {
        public static final Type<Reply> TYPE=new Type<>(HeavenlyRestriction.id("loan_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.nonce);b.writeBoolean(p.accept);},b->new Reply(b.readUUID(),b.readBoolean()));
        public Type<Reply> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){var r=event.registrar("1");r.playToClient(State.TYPE,State.CODEC,(p,c)->c.enqueueWork(()->receiver.accept(p.data)));
        r.playToServer(Request.TYPE,Request.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)LoanShark.request(player,p.session,p.action,p.value,p.query,p.page);}));
        r.playToServer(Reply.TYPE,Reply.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)LoanShark.reply(player,p.nonce,p.accept);}));}
    public static void send(ServerPlayer p,CompoundTag tag){PacketDistributor.sendToPlayer(p,new State(tag));}
}
