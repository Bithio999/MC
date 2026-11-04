package dev.heavenlyrestriction.cartography;

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

public final class MapNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};

  public record Request(UUID token, String action, CompoundTag data)
      implements CustomPacketPayload {
    public static final Type<Request> TYPE = new Type<>(HeavenlyRestriction.id("map_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeUUID(p.token);
              b.writeUtf(p.action, 16);
              b.writeNbt(p.data);
            },
            b -> new Request(b.readUUID(), b.readUtf(16), b.readNbt()));

    @Override
    public Type<Request> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(HeavenlyRestriction.id("map_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of((b, p) -> b.writeNbt(p.data), b -> new State(b.readNbt()));

    @Override
    public Type<State> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    var r = e.registrar("2");
    r.playToServer(
        Request.TYPE,
        Request.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (c.player() instanceof ServerPlayer player && p.data != null)
                    MapService.request(player, p.token, p.action, p.data);
                }));
    r.playToClient(
        State.TYPE,
        State.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (p.data != null) receiver.accept(p.data);
                }));
  }

  public static void send(ServerPlayer p, CompoundTag t) {
    PacketDistributor.sendToPlayer(p, new State(t));
  }

  public static void send(UUID token, String action, CompoundTag t) {
    PacketDistributor.sendToServer(new Request(token, action, t));
  }

  private MapNetwork() {}
}
