package dev.heavenlyrestriction.splitmind;

import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.magic.Magic;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Bounded pages, server-side ownership checks; no registry-wide learnable spell list. */
public final class SplitMindNetwork {
  public static final int PAGE_SIZE = 5;
  public static Consumer<CompoundTag> receiver = t -> {};

  public record Request(String action, String id, String query, int category, int page, int mode)
      implements CustomPacketPayload {
    public static final Type<Request> TYPE =
        new Type<>(HeavenlyRestriction.id("split_mind_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeUtf(p.action, 12);
              b.writeUtf(p.id, 160);
              b.writeUtf(p.query, 64);
              b.writeByte(p.category);
              b.writeInt(p.page);
              b.writeInt(p.mode);
            },
            b ->
                new Request(
                    b.readUtf(12),
                    b.readUtf(160),
                    b.readUtf(64),
                    b.readUnsignedByte(),
                    b.readInt(),
                    b.readInt()));

    @Override
    public Type<Request> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(HeavenlyRestriction.id("split_mind_state"));
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

  public static void open(ServerPlayer p) {
    send(p, snapshot(p, "", 0, 0), true, "");
  }

  private static void send(ServerPlayer p, CompoundTag data, boolean open, String message) {
    data.putBoolean("open", open);
    data.putString("message", message);
    PacketDistributor.sendToPlayer(p, new State(data));
  }

  public static boolean allowed(ServerPlayer p) {
    return SkillAPI.getSkillsFrom(p)
        .getSkill(HeavenlyRestriction.SPLIT_MIND.get())
        .filter(i -> i.getMastery() >= 0 && i.canInteractSkill(p) && i.canActivateSkill(p, 0))
        .isPresent();
  }

  private static void handle(ServerPlayer p, Request request) {
    if (!allowed(p)) {
      send(p, snapshot(p, "", 0, 0), false, "heavenlyrestriction.split_mind.invalid");
      return;
    }
    if (request.category < 0 || request.category > 2) return;
    String message = "";
    if (request.action.equals("assign"))
      message =
          SplitMindCasting.assign(p, request.id, request.mode)
              ? "heavenlyrestriction.split_mind.saved"
              : "heavenlyrestriction.split_mind.invalid";
    else if (!request.action.equals("list")) return;
    send(p, snapshot(p, request.query, request.category, request.page), false, message);
  }

  public static CompoundTag snapshot(ServerPlayer p, String query, int category, int page) {
    var out = new CompoundTag();
    out.putBoolean("allowed", allowed(p));
    if (!out.getBoolean("allowed")) return out;
    query = query.length() > 64 ? query.substring(0, 64) : query;
    category = Math.clamp(category, 0, 2);
    final String search = query.toLowerCase(Locale.ROOT);
    final int type = category;
    var list =
        SkillAPI.getSkillsFrom(p).getLearnedSkills().stream()
            .filter(SplitMindCasting::eligible)
            .filter(
                i ->
                    type == 0
                        || ((Magic) i.getSkill()).getType()
                            == (type == 1 ? Magic.MagicType.ASPECTUAL : Magic.MagicType.SPIRITUAL))
            .filter(
                i ->
                    i.getSkill().getName().getString().toLowerCase(Locale.ROOT).contains(search)
                        || i.getSkillId().toString().contains(search))
            .sorted(
                Comparator.comparing(
                        (ManasSkillInstance i) -> i.getSkill().getName().getString(),
                        String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(i -> i.getSkillId().toString()))
            .toList();
    int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    page = Math.clamp(page, 0, pages - 1);
    var rows = new ListTag();
    for (var i :
        list.subList(page * PAGE_SIZE, Math.min(list.size(), page * PAGE_SIZE + PAGE_SIZE))) {
      var m = (Magic) i.getSkill();
      var row = new CompoundTag();
      row.putString("id", i.getSkillId().toString());
      row.putString("name", m.getName().getString());
      row.putString("icon", m.getSkillIcon().toString());
      row.putString("type", m.getType() == Magic.MagicType.ASPECTUAL ? "Aspectual" : "Spiritual");
      row.putBoolean("mastered", i.isMastered(p));
      row.putInt("castTicks", m.getCastingTime(i, p));
      row.putDouble("mp", m.getMagiculeCost(p, i, 0));
      row.putDouble("ap", m.getAuraCost(p, i, 0));
      row.putInt("cooldown", i.getCoolDown(0));
      var modes = new ListTag();
      var allowed = SplitMindModes.available(p, i);
      for (int mode = 0; mode < SplitMindModes.count(i); mode++) {
        var option = new CompoundTag();
        option.putInt("id", mode);
        option.putString("name", m.getModeName(i, mode).getString());
        option.putBoolean("available", allowed.contains(mode));
        option.putBoolean("channel", SplitMindModes.sustained(i, mode));
        option.putDouble("mp", m.getMagiculeCost(p, i, mode));
        option.putDouble("ap", m.getAuraCost(p, i, mode));
        option.putInt("cooldown", i.getCoolDown(mode));
        option.putInt("castTicks", m.getCastingTime(i, p));
        modes.add(option);
      }
      row.put("modes", modes);
      rows.add(row);
    }
    var mind =
        SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SPLIT_MIND.get()).orElseThrow();
    out.putString("assignment", mind.getOrCreateTag().getString(SplitMindCasting.ASSIGNED));
    out.putInt("assignedMode", SplitMindCasting.assignedMode(mind));
    out.putString(
        "assignedName",
        SplitMindCasting.assigned(p, mind).map(i -> i.getSkill().getName().getString()).orElse(""));
    out.putString(
        "assignedModeName",
        SplitMindCasting.assigned(p, mind)
            .map(
                i ->
                    ((Magic) i.getSkill())
                        .getModeName(i, SplitMindCasting.assignedMode(mind))
                        .getString())
            .orElse(""));
    out.putBoolean("auto", mind.getOrCreateTag().getBoolean(SplitMindCasting.AUTO));
    out.putString("query", query);
    out.putInt("category", category);
    out.putInt("page", page);
    out.putInt("pages", pages);
    out.putInt("total", list.size());
    out.put("rows", rows);
    return out;
  }

  private SplitMindNetwork() {}
}
