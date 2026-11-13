package dev.heavenlyrestriction.laplace;

import dev.heavenlyrestriction.HeavenlyRestriction;
import dev.heavenlyrestriction.forge.ForgeSynthesis;
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

/** Owner-bound sessions, bounded equation packets, and live server-side validation. */
public final class LaplaceNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};

  private record Session(
      UUID id,
      long until,
      net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
      String selected) {}

  private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

  public record Request(
      UUID session,
      String operation,
      String action,
      boolean enabled,
      Map<String, String> equations,
      PaymentPlan payment)
      implements CustomPacketPayload {
    public Request(
        UUID session,
        String operation,
        String action,
        boolean enabled,
        Map<String, String> equations) {
      this(session, operation, action, enabled, equations, PaymentPlan.MAGICULES);
    }

    public static final Type<Request> TYPE = new Type<>(HeavenlyRestriction.id("laplace_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeUUID(p.session);
              b.writeUtf(p.operation, 16);
              b.writeUtf(p.action, 180);
              b.writeBoolean(p.enabled);
              b.writeDouble(p.payment.magicules());
              b.writeDouble(p.payment.aura());
              b.writeDouble(p.payment.hp());
              b.writeDouble(p.payment.shp());
              if (p.equations.size() > FormulaSet.MAX_EQUATIONS)
                throw new IllegalArgumentException("Too many equations");
              b.writeVarInt(p.equations.size());
              p.equations.forEach(
                  (k, v) -> {
                    b.writeUtf(k, 32);
                    b.writeUtf(v, 160);
                  });
            },
            b -> {
              var id = b.readUUID();
              var op = b.readUtf(16);
              var action = b.readUtf(180);
              boolean on = b.readBoolean();
              var plan =
                  new PaymentPlan(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble());
              int n = b.readVarInt();
              if (n < 0 || n > FormulaSet.MAX_EQUATIONS)
                throw new IllegalArgumentException("Too many equations");
              var eq = new LinkedHashMap<String, String>();
              for (int x = 0; x < n; x++) {
                String k = b.readUtf(32), v = b.readUtf(160);
                if (eq.put(k, v) != null) throw new IllegalArgumentException("Duplicate equation");
              }
              return new Request(id, op, action, on, eq, plan);
            });

    public Type<Request> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(HeavenlyRestriction.id("laplace_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of((b, p) -> b.writeNbt(p.data), b -> new State(b.readNbt()));

    public Type<State> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    var r = e.registrar("3");
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
            .getSkill(HeavenlyRestriction.LAPLACE.get())
            .filter(i -> i.getMastery() < 0 || i.canInteractSkill(p) && i.canActivateSkill(p, 0))
            .isPresent();
  }

  public static void open(ServerPlayer p) {
    if (!accessible(p)) return;
    p.closeContainer();
    SESSIONS.put(
        p,
        new Session(UUID.randomUUID(), p.level().getGameTime() + 12000, p.level().dimension(), ""));
    send(p, true, "");
  }

  public static void handle(ServerPlayer p, Request r) {
    var s = SESSIONS.get(p);
    if (s == null || !s.id.equals(r.session)) return;
    long now = p.level().getGameTime();
    if (s.until < now
        || s.until - now > 12000
        || s.dimension != p.level().dimension()
        || !accessible(p)) {
      SESSIONS.remove(p);
      var t = new CompoundTag();
      t.putUUID("session", s.id);
      PacketDistributor.sendToPlayer(p, new State(t));
      return;
    }
    if (r.operation.equals("close")) {
      SESSIONS.remove(p);
      return;
    }
    String notice = "";
    String selected = s.selected;
    try {
      switch (r.operation) {
        case "save" -> notice = Laplace.save(p, r.action, r.equations, r.enabled, r.payment);
        case "apply" -> {
          notice = Laplace.save(p, r.action, r.equations, true, r.payment);
          if (notice.startsWith("Saved.")) {
            var laplace = Laplace.owned(p).orElseThrow();
            if (!laplace.isToggled()) laplace.onToggleOn(p);
            SkillAPI.getSkillsFrom(p).checkAndMarkDirty(laplace);
            notice = "Applied. Formula is active.";
          }
        }
        case "disable" -> {
          var own = Laplace.owned(p);
          var action = LaplaceActions.byKey(p, r.action);
          if (own.isEmpty() || action == null) return;
          var equations = Laplace.equations(own.get(), action);
          try {
            action.model().evaluate(equations);
          } catch (IllegalArgumentException ex) {
            equations = new LinkedHashMap<>(action.model().defaults());
          }
          notice = Laplace.save(p, r.action, equations, false);
          if (notice.startsWith("Saved."))
            notice = "Disabled. This action uses its native formula.";
        }
        case "synthesize" ->
            notice =
                LaplaceSynthesis.synthesize(p)
                    ? "Synthesis complete. Your parents have become Laplace."
                    : "Requirements changed. Check the list before confirming.";
        case "select" -> {
          if (LaplaceActions.byKey(p, r.action) == null) return;
          selected = r.action;
        }
        case "refresh" -> {}
        default -> {
          return;
        }
      }
    } catch (IllegalArgumentException ex) {
      notice = ex.getMessage();
    }
    SESSIONS.put(p, new Session(s.id, now + 12000, s.dimension, selected));
    send(p, false, notice, r.operation);
  }

  private static void send(ServerPlayer p, boolean open, String notice) {
    send(p, open, notice, "");
  }

  private static void send(ServerPlayer p, boolean open, String notice, String operation) {
    var t = snapshot(p);
    t.putString("operation", operation);
    t.putBoolean("open", open);
    t.putString("notice", notice);
    PacketDistributor.sendToPlayer(p, new State(t));
  }

  public static CompoundTag snapshot(ServerPlayer p) {
    var t = new CompoundTag();
    t.putBoolean("allowed", accessible(p));
    var s = SESSIONS.get(p);
    if (s != null) t.putUUID("session", s.id);
    var own = Laplace.owned(p);
    t.putBoolean("learned", own.isPresent());
    t.putBoolean("parent1", ForgeSynthesis.mastered(p, HeavenlyRestriction.EXPONENTIATION.get()));
    t.putBoolean("parent2", ForgeSynthesis.mastered(p, UniqueSkills.MATHEMATICIAN.get()));
    t.putBoolean("awakened", ForgeSynthesis.awakened(p) && ForgeSynthesis.settled(p));
    t.putInt("achievement", LaplaceSynthesis.learnedMagic(p));
    t.putInt("required", LaplaceConfig.ACHIEVEMENT.get());
    t.putDouble("cost", LaplaceSynthesis.cost(p));
    t.putBoolean("affordable", LaplaceSynthesis.affordable(p));
    t.putBoolean("ready", LaplaceSynthesis.ready(p));
    if (own.isEmpty()) return t;
    t.putInt("capacity", Laplace.capacity(p));
    t.putBoolean("passive", own.get().isToggled());
    t.putLong("wait", Laplace.remaining(p, own.get()));
    var rows = new ListTag();
    for (var a : LaplaceActions.learned(p)) {
      var row = new CompoundTag();
      row.putString("key", a.key());
      row.putString("title", a.title());
      row.putString("detail", a.detail());
      row.putBoolean(
          "fixed",
          a.model().powerOutputs().isEmpty()
              && a.model().inputs().getOrDefault("base_cost", 0d) == 0
              && !a.model().inputs().containsKey("__deferred_cost"));
      row.putString("icon", a.skill().getSkill().getSkillIcon().toString());
      row.putBoolean("enabled", Laplace.effectiveEnabled(p, own.get(), a.key()));
      boolean full = LaplaceActions.legacy(a.skill()) || s != null && s.selected.equals(a.key());
      if (full) row.put("model", encode(a.model()));
      row.putBoolean(
          "requiresStop", dev.heavenlyrestriction.laplace.expand.ExpandedStates.requiresStop(p, a));
      var eq = new CompoundTag();
      Laplace.equations(own.get(), a).forEach(eq::putString);
      if (full) row.put("equations", eq);
      rows.add(row);
    }
    t.put("rows", rows);
    return t;
  }

  public static CompoundTag encode(EquationModel m) {
    var t = new CompoundTag();
    var inputs = new CompoundTag();
    m.inputs()
        .forEach(
            (k, v) -> {
              var b = new CompoundTag();
              b.putDouble("value", v);
              b.putDouble("low", m.bounds().get(k).low());
              b.putDouble("high", m.bounds().get(k).high());
              inputs.put(k, b);
            });
    t.put("inputs", inputs);
    var outputs = new ListTag();
    m.defaults()
        .forEach(
            (k, v) -> {
              var row = new CompoundTag();
              row.putString("key", k);
              row.putString("formula", v);
              row.putString("label", m.labels().get(k));
              row.putString("unit", m.units().get(k));
              row.putBoolean("power", m.powerOutputs().contains(k));
              outputs.add(row);
            });
    t.put("outputs", outputs);
    t.putDouble("limit", m.powerLimit());
    t.put("payment", Laplace.encodePlan(m.payment().plan()));
    t.putDouble("perAura", m.payment().rates().magiculesPerAura());
    t.putDouble("perHP", m.payment().rates().magiculesPerHP());
    t.putDouble("perSHP", m.payment().rates().magiculesPerSHP());
    t.putBoolean("allowVital", m.payment().allowVital());
    t.putBoolean("integerBill", m.payment().integerBill());
    return t;
  }

  public static EquationModel decode(CompoundTag t) {
    var inputs = new LinkedHashMap<String, Double>();
    var bounds = new LinkedHashMap<String, FormulaSet.Range>();
    var it = t.getCompound("inputs");
    for (String k : it.getAllKeys()) {
      var row = it.getCompound(k);
      inputs.put(k, row.getDouble("value"));
      bounds.put(k, new FormulaSet.Range(row.getDouble("low"), row.getDouble("high")));
    }
    var defaults = new LinkedHashMap<String, String>();
    var labels = new LinkedHashMap<String, String>();
    var units = new LinkedHashMap<String, String>();
    var power = new ArrayList<String>();
    var costs = new ArrayList<String>();
    for (var v : t.getList("outputs", 10)) {
      var row = (CompoundTag) v;
      String k = row.getString("key");
      defaults.put(k, row.getString("formula"));
      labels.put(k, row.getString("label"));
      units.put(k, row.getString("unit"));
      if (row.getBoolean("power")) power.add(k);
      else costs.add(k);
    }
    return new EquationModel(
        inputs,
        bounds,
        defaults,
        labels,
        units,
        power,
        costs,
        t.getDouble("limit"),
        t.contains("payment")
            ? new PaymentPlan.Policy(
                Laplace.readPlan(t),
                new PaymentPlan.Rates(
                    t.getDouble("perAura"), t.getDouble("perHP"), t.getDouble("perSHP")),
                t.getBoolean("allowVital"),
                t.getBoolean("integerBill"))
            : PaymentPlan.Policy.DEFAULT);
  }

  private LaplaceNetwork() {}
}
