package dev.heavenlyrestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.function.Consumer;
/** Clients submit six effect IDs and a revision; the server owns skill state and all arithmetic. */
public final class SchemaNetwork {
    public static Consumer<CompoundTag> receiver=state->{};
    public record Save(int[] tree,int revision) implements CustomPacketPayload {
        public static final Type<Save> TYPE=new Type<>(HeavenlyRestriction.id("schema_save"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Save> CODEC=StreamCodec.of((b,p)->{if(p.tree.length!=6)throw new IllegalArgumentException("Schema requires six slots");for(int v:p.tree)b.writeByte(v);b.writeInt(p.revision);},b->{int[] slots=new int[6];for(int n=0;n<6;n++)slots[n]=b.readUnsignedByte();return new Save(slots,b.readInt());});
        @Override public Type<Save> type(){return TYPE;}
    }
    public record State(CompoundTag data) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(HeavenlyRestriction.id("schema_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,p)->b.writeNbt(p.data),b->new State(b.readNbt()));
        @Override public Type<State> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("1");
        registrar.playToServer(Save.TYPE,Save.CODEC,(payload,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer p){boolean ok=SchemaArt.save(p,payload.tree,payload.revision);send(p,false,ok?"Build applied.":"Build could not be applied. Please reopen Build.");}}));
        registrar.playToClient(State.TYPE,State.CODEC,(payload,context)->context.enqueueWork(()->receiver.accept(payload.data)));
    }
    public static void send(ServerPlayer p,boolean open,String message){var state=SchemaArt.snapshot(p);state.putBoolean("open",open);state.putString("message",message);PacketDistributor.sendToPlayer(p,new State(state));}
}
