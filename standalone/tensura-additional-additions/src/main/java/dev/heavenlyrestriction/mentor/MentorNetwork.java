package dev.heavenlyrestriction.mentor;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.manascore.skill.api.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Purchases are bound to a nearby living mentor and an expiring, player-specific session. */
public final class MentorNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};

  private record Session(UUID id, UUID npc, long until, double quote) {}

  private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

  public record Request(UUID session, String action, String id, String query, int page)
      implements CustomPacketPayload {
    public static final Type<Request> TYPE = new Type<>(HeavenlyRestriction.id("mentor_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeUUID(p.session);
              b.writeUtf(p.action, 12);
              b.writeUtf(p.id, 160);
              b.writeUtf(p.query, 64);
              b.writeInt(p.page);
            },
            b ->
                new Request(
                    b.readUUID(), b.readUtf(12), b.readUtf(160), b.readUtf(64), b.readInt()));

    @Override
    public Type<Request> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(HeavenlyRestriction.id("mentor_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of((b, p) -> b.writeNbt(p.data), b -> new State(b.readNbt()));

    @Override
    public Type<State> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    var r = e.registrar("1");
    r.playToServer(
        Request.TYPE,
        Request.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (c.player() instanceof ServerPlayer owner) handle(owner, p);
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

  public static boolean nearby(ServerPlayer p, MentorEntity npc) {
    return p.isAlive()
        && !p.isSpectator()
        && npc != null
        && npc.isAlive()
        && p.level() == npc.level()
        && p.distanceToSqr(npc) <= 64
        && p.hasLineOfSight(npc);
  }

  public static void open(ServerPlayer p, MentorEntity npc) {
    if (!nearby(p, npc)) return;
    p.closeContainer();
    SESSIONS.put(
        p,
        new Session(
            UUID.randomUUID(),
            npc.getUUID(),
            p.level().getGameTime() + 1200,
            MentorConfig.COST.get()));
    send(p, "", 0, true, "");
  }

  public static void close(Player p) {
    SESSIONS.remove(p);
  }

  public static void handle(ServerPlayer p, Request r) {
    var s = SESSIONS.get(p);
    long now = p.level().getGameTime();
    if (s == null || !s.id.equals(r.session)) return;
    if (r.action.equals("close")) {
      close(p);
      return;
    }
    var npc = p.serverLevel().getEntity(s.npc);
    if (s.until < now
        || s.until - now > 1200
        || !(npc instanceof MentorEntity mentor)
        || !nearby(p, mentor)) {
      var t = new CompoundTag();
      t.putUUID("session", s.id);
      t.putBoolean("allowed", false);
      PacketDistributor.sendToPlayer(p, new State(t));
      close(p);
      return;
    }
    String notice = "";
    if (r.action.equals("buy")) {
      var id = ResourceLocation.tryParse(r.id);
      notice =
          Double.compare(s.quote, MentorConfig.COST.get()) != 0
              ? "price"
              : id == null ? "unavailable" : MentorService.purchase(p, id);
    } else if (!r.action.equals("list")) return;
    SESSIONS.put(p, new Session(s.id, s.npc, now + 1200, MentorConfig.COST.get()));
    send(p, r.query, r.page, false, notice);
  }

  private static void send(ServerPlayer p, String query, int page, boolean open, String notice) {
    var t = snapshot(p, query, page);
    var s = SESSIONS.get(p);
    if (s != null) t.putUUID("session", s.id);
    t.putBoolean("open", open);
    t.putString("notice", notice);
    PacketDistributor.sendToPlayer(p, new State(t));
  }

  public static CompoundTag snapshot(ServerPlayer p, String query, int page) {
    var t = new CompoundTag();
    t.putBoolean("allowed", true);
    var session = SESSIONS.get(p);
    if (session != null) t.putUUID("session", session.id);
    t.putBoolean("used", !MentorLedger.reconcile(p).isEmpty());
    t.putString("redeemed", MentorLedger.reconcile(p));
    t.putDouble("cost", MentorConfig.COST.get());
    t.putDouble("reserves", MentorService.reserves(p));
    t.putBoolean("orb", MentorService.orbSlot(p) >= 0);
    t.putBoolean("affordable", MentorService.affordable(p));
    var q = query.toLowerCase(Locale.ROOT).strip();
    var list =
        MentorService.catalog(p).stream()
            .filter(
                s ->
                    s.getName().getString().toLowerCase(Locale.ROOT).contains(q)
                        || s.getRegistryName().toString().contains(q))
            .toList();
    int pages = Math.max(1, (list.size() + 7) / 8), current = Math.clamp(page, 0, pages - 1);
    t.putInt("page", current);
    t.putInt("pages", pages);
    t.putInt("total", list.size());
    var rows = new ListTag();
    for (var s : list.subList(current * 8, Math.min(list.size(), current * 8 + 8))) {
      var row = new CompoundTag();
      row.putString("id", s.getRegistryName().toString());
      row.putString("name", s.getName().getString());
      String description = s.getSkillDescription().getString();
      row.putString("description", description.substring(0, Math.min(16000, description.length())));
      row.putString("icon", s.getSkillIcon().toString());
      row.putBoolean("owned", MentorService.owned(p, s));
      row.putBoolean("learnable", MentorService.learnable(p, s));
      rows.add(row);
    }
    t.put("rows", rows);
    return t;
  }

  private MentorNetwork() {}
}
