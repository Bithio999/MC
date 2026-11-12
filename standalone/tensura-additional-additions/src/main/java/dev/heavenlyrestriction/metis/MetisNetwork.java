package dev.heavenlyrestriction.metis;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.forge.ForgeSynthesis;
import dev.heavenlyrestriction.splitmind.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Short-lived, owner-bound editing sessions with bounded learned-only pages. */
public final class MetisNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};

  private record Session(UUID id, long until) {}

  private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

  public record Request(
      UUID session,
      String action,
      String id,
      String query,
      int page,
      int category,
      int slot,
      int mode,
      int priority,
      boolean target)
      implements CustomPacketPayload {
    public static final Type<Request> TYPE = new Type<>(HeavenlyRestriction.id("metis_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeUUID(p.session);
              b.writeUtf(p.action, 16);
              b.writeUtf(p.id, 160);
              b.writeUtf(p.query, 64);
              b.writeInt(p.page);
              b.writeByte(p.category);
              b.writeByte(p.slot);
              b.writeInt(p.mode);
              b.writeByte(p.priority);
              b.writeBoolean(p.target);
            },
            b ->
                new Request(
                    b.readUUID(),
                    b.readUtf(16),
                    b.readUtf(160),
                    b.readUtf(64),
                    b.readInt(),
                    b.readUnsignedByte(),
                    b.readUnsignedByte(),
                    b.readInt(),
                    b.readUnsignedByte(),
                    b.readBoolean()));

    @Override
    public Type<Request> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(HeavenlyRestriction.id("metis_state"));
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
                  if (c.player() instanceof ServerPlayer player) handle(player, p);
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
            .getSkill(HeavenlyRestriction.METIS.get())
            .filter(i -> i.getMastery() < 0 || i.canInteractSkill(p) && i.canActivateSkill(p, 0))
            .isPresent();
  }

  public static void open(ServerPlayer p) {
    if (!accessible(p)) return;
    p.closeContainer();
    SESSIONS.put(p, new Session(UUID.randomUUID(), p.level().getGameTime() + 1200));
    send(p, "", 0, 0, true, "");
  }

  private static void send(
      ServerPlayer p, String query, int category, int page, boolean open, String notice) {
    var t = snapshot(p, query, category, page);
    t.putBoolean("open", open);
    t.putString("notice", notice);
    PacketDistributor.sendToPlayer(p, new State(t));
  }

  public static void handle(ServerPlayer p, Request r) {
    var session = SESSIONS.get(p);
    long now = p.level().getGameTime();
    if (session == null || !session.id.equals(r.session)) return;
    if (session.until < now || session.until - now > 1200 || !accessible(p)) {
      SESSIONS.remove(p);
      var closed = new CompoundTag();
      closed.putUUID("session", session.id);
      closed.putBoolean("allowed", false);
      PacketDistributor.sendToPlayer(p, new State(closed));
      p.displayClientMessage(
          net.minecraft.network.chat.Component.translatable(
              "heavenlyrestriction.metis.session_expired"),
          true);
      return;
    }
    if (r.action.equals("close")) {
      SESSIONS.remove(p);
      return;
    }
    if (r.category > 4 || r.slot > 3 || r.priority > 2) return;
    String notice = "";
    if (r.action.equals("synthesize"))
      notice =
          MetisSynthesis.synthesize(p)
              ? "heavenlyrestriction.metis.synthesized"
              : "heavenlyrestriction.metis.requirements";
    else if (r.action.equals("assign"))
      notice =
          MetisMatrix.assign(p, r.slot, r.id, r.mode, r.priority, r.target)
              ? "heavenlyrestriction.split_mind.saved"
              : "heavenlyrestriction.metis.invalid";
    else if (r.action.equals("optimal")) {
      var mind = MetisMatrix.owned(p);
      if (mind.isEmpty()) return;
      mind.get().getOrCreateTag().putBoolean(MetisMatrix.OPTIMAL, r.target);
      mind.get().markDirty();
    } else if (!r.action.equals("list")) return;
    SESSIONS.put(p, new Session(session.id, now + 1200));
    send(p, r.query, r.category, r.page, false, notice);
  }

  public static CompoundTag snapshot(ServerPlayer p, String query, int category, int page) {
    var t = new CompoundTag();
    t.putBoolean("allowed", accessible(p));
    if (!t.getBoolean("allowed")) return t;
    var session = SESSIONS.get(p);
    if (session != null) t.putUUID("session", session.id);
    var mind = MetisMatrix.owned(p);
    t.putBoolean("learned", mind.isPresent());
    t.putBoolean("split", ForgeSynthesis.mastered(p, HeavenlyRestriction.SPLIT_MIND.get()));
    t.putBoolean("sage", ForgeSynthesis.mastered(p, UniqueSkills.GREAT_SAGE.get()));
    t.putBoolean("awakened", ForgeSynthesis.awakened(p));
    t.putBoolean("settled", ForgeSynthesis.settled(p));
    t.putInt("magicCount", MetisSynthesis.learnedMagic(p));
    t.putInt("magicRequired", MetisConfig.MAGIC_ACHIEVEMENT.get());
    t.putDouble("cost", MetisSynthesis.cost(p));
    t.putBoolean("affordable", MetisSynthesis.affordable(p));
    t.putBoolean("ready", MetisSynthesis.ready(p));
    if (mind.isEmpty()) return t;
    t.merge(MetisAbilities.catalog(p, query, category, page));
    t.putInt("capacity", MetisMatrix.capacity(p, mind.get()));
    t.putDouble("factor", MetisMatrix.costFactor(p));
    t.putBoolean("auto", mind.get().getOrCreateTag().getBoolean(MetisMatrix.AUTO));
    t.putBoolean("optimal", MetisMatrix.optimal(mind.get()));
    var rows = new ListTag();
    for (var s : MetisMatrix.slots(p, mind.get())) {
      var row = new CompoundTag();
      var spell = SkillAPI.getSkillsFrom(p).getSkill(s.id()).orElseThrow();
      row.putInt("slot", s.index());
      row.putString("id", s.id().toString());
      row.putString("name", spell.getSkill().getName().getString());
      row.putString("icon", spell.getSkill().getSkillIcon().toString());
      var modes = MetisAbilities.modes(p, spell);
      row.put("modes", modes);
      row.putInt("mode", s.mode());
      row.putInt("priority", s.priority());
      row.putBoolean("target", s.currentTarget());
      row.putString("role", MetisMagic.role(spell, s.mode()).name());
      rows.add(row);
    }
    t.put("slots", rows);
    t.putInt(
        "dossiers", mind.get().getOrCreateTag().getList("MetisDossiers", Tag.TAG_COMPOUND).size());
    return t;
  }

  private MetisNetwork() {}
}
