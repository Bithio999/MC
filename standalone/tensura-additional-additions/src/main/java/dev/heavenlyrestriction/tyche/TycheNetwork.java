package dev.heavenlyrestriction.tyche;

import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.forge.ForgeSynthesis;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class TycheNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};

  private record Session(UUID id, long until, double price) {}

  private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

  public record Request(UUID session, boolean synthesize) implements CustomPacketPayload {
    public static final Type<Request> TYPE = new Type<>(HeavenlyRestriction.id("tyche_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeUUID(p.session);
              b.writeBoolean(p.synthesize);
            },
            b -> new Request(b.readUUID(), b.readBoolean()));

    @Override
    public Type<Request> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(HeavenlyRestriction.id("tyche_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of((b, p) -> b.writeNbt(p.data), b -> new State(b.readNbt()));

    @Override
    public Type<State> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent event) {
    var r = event.registrar("1");
    r.playToServer(
        Request.TYPE,
        Request.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (c.player() instanceof ServerPlayer s) handle(s, p);
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

  public static boolean accessible(ServerPlayer p) {
    return p.isAlive()
        && !p.isSpectator()
        && SkillAPI.getSkillsFrom(p)
            .getSkill(HeavenlyRestriction.TYCHE.get())
            .filter(i -> i.getMastery() < 0 || i.canInteractSkill(p))
            .isPresent();
  }

  public static void open(ServerPlayer p) {
    if (!accessible(p)) return;
    p.closeContainer();
    SESSIONS.put(
        p, new Session(UUID.randomUUID(), p.level().getGameTime() + 1200, TycheSynthesis.cost(p)));
    send(p, true, "");
  }

  private static void send(ServerPlayer p, boolean open, String notice) {
    var t = snapshot(p);
    var session = SESSIONS.get(p);
    if (session != null) t.putUUID("session", session.id);
    t.putBoolean("open", open);
    t.putString("notice", notice);
    PacketDistributor.sendToPlayer(p, new State(t));
  }

  public static void handle(ServerPlayer p, Request request) {
    var session = SESSIONS.get(p);
    long now = p.level().getGameTime();
    if (session == null
        || !session.id.equals(request.session)
        || session.until < now
        || session.until - now > 1200
        || !accessible(p)) return;
    String notice = "";
    if (request.synthesize) {
      if (Double.compare(session.price, TycheSynthesis.cost(p)) != 0)
        notice = "Price changed; reopen this menu to review the new cost.";
      else
        notice = TycheSynthesis.synthesize(p) ? "Synthesis complete." : "Requirements are not met.";
      SESSIONS.put(
          p,
          new Session(
              UUID.randomUUID(),
              now + 1200,
              TycheSynthesis.cost(p))); // Rotate the confirmation nonce.
    }
    if (!request.synthesize)
      SESSIONS.put(
          p,
          Double.compare(session.price, TycheSynthesis.cost(p)) == 0
              ? new Session(session.id, now + 1200, session.price)
              : new Session(UUID.randomUUID(), now + 1200, TycheSynthesis.cost(p)));
    send(p, false, notice);
  }

  public static CompoundTag snapshot(ServerPlayer p) {
    var t = new CompoundTag();
    t.putBoolean("allowed", accessible(p));
    t.putBoolean("learned", Tyche.owned(p).isPresent());
    t.putBoolean(
        "merchant", ForgeSynthesis.mastered(p, HeavenlyRestriction.CRITICAL_MERCHANT.get()));
    t.putBoolean("tuner", ForgeSynthesis.mastered(p, UniqueSkills.TUNER.get()));
    t.putBoolean("awakened", ForgeSynthesis.awakened(p));
    t.putBoolean("settled", ForgeSynthesis.settled(p));
    t.putInt("criticals", RestrictionData.of(p).getInt(TycheSynthesis.CRITS));
    t.putInt("criticalRequired", TycheConfig.CRITICALS.get());
    t.putInt("survivals", TycheSynthesis.types(p));
    t.putInt("survivalRequired", TycheConfig.SURVIVALS.get());
    t.putDouble("cost", TycheSynthesis.cost(p));
    t.putBoolean("affordable", TycheSynthesis.affordable(p));
    t.putBoolean("ready", TycheSynthesis.ready(p));
    Tyche.owned(p)
        .ifPresent(
            i -> {
              t.putInt("timer", i.getCoolDown(2));
              t.putString(
                  "history",
                  String.join(
                      ", ", i.getOrCreateTag().getCompound(FateInsurance.HISTORY).getAllKeys()));
            });
    return t;
  }

  private TycheNetwork() {}
}
