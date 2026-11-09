package dev.heavenlyrestriction.forge;

import dev.heavenlyrestriction.HeavenlyRestriction;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ForgeNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};
  public static final Map<UUID, UUID> clientWorks = new HashMap<>();

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(HeavenlyRestriction.id("forge_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of((b, p) -> b.writeNbt(p.data), b -> new State(b.readNbt()));

    public Type<State> type() {
      return TYPE;
    }
  }

  public record Request(UUID session, String action, int slot, int value)
      implements CustomPacketPayload {
    public static final Type<Request> TYPE = new Type<>(HeavenlyRestriction.id("forge_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeUUID(p.session);
              b.writeUtf(p.action, 20);
              b.writeVarInt(p.slot);
              b.writeVarInt(p.value);
            },
            b -> new Request(b.readUUID(), b.readUtf(20), b.readVarInt(), b.readVarInt()));

    public Type<Request> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    var r = e.registrar("1");
    r.playToClient(State.TYPE, State.CODEC, (p, c) -> c.enqueueWork(() -> receiver.accept(p.data)));
    r.playToServer(
        Request.TYPE,
        Request.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (c.player() instanceof ServerPlayer player)
                    ForgeService.request(player, p.session, p.action, p.slot, p.value);
                }));
  }

  public static void send(ServerPlayer p, CompoundTag tag) {
    PacketDistributor.sendToPlayer(p, new State(tag));
  }

  public static void ledger(ServerPlayer p) {
    var t = new CompoundTag();
    t.put("ledger", ForgeLedger.get(p.getServer()).snapshot());
    send(p, t);
  }

  public static void ledgerAll(ServerPlayer p) {
    for (var other : p.getServer().getPlayerList().getPlayers()) ledger(other);
  }

  private ForgeNetwork() {}
}
