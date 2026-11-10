package dev.heavenlyrestriction.forge;

import dev.architectury.event.EventResult;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.*;
import net.neoforged.fml.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ForgeEvents {
  public static void register(IEventBus bus, ModContainer container) {
    ForgeContent.register(bus);
    bus.addListener(ForgeNetwork::register);
    container.registerConfig(
        net.neoforged.fml.config.ModConfig.Type.SERVER,
        ForgeConfig.SPEC,
        "tensura-additions-forge.toml");
    NeoForge.EVENT_BUS.addListener(ForgeArsenal::tick);
    NeoForge.EVENT_BUS.addListener(ForgeItems::crafted);
    NeoForge.EVENT_BUS.addListener(ForgeItems::tooltip);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ForgeItems::attributes);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ForgeTempering::melee);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ForgeTempering::damage);
    NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ForgeTempering::incoming);
    TensuraEntityEvents.SPIRITUAL_HURT_EVENT.register(
        (target, attacker, amount, resistance, damage, source) -> {
          ForgeTempering.spiritual(target, source.get(), damage);
          return EventResult.pass();
        });
    NeoForge.EVENT_BUS.addListener(
        (net.neoforged.neoforge.event.tick.EntityTickEvent.Post e) -> {
          if (e.getEntity() instanceof net.minecraft.world.entity.LivingEntity living
              && !living.level().isClientSide()
              && living.tickCount % 10 == 0) {
            for (var s : living.getAllSlots()) ForgeItems.reconcile(s);
            if (living instanceof ServerPlayer p)
              for (var s : p.getInventory().items) ForgeItems.reconcile(s);
          }
        });
    NeoForge.EVENT_BUS.addListener(
        EventPriority.LOWEST,
        (LivingDeathEvent e) -> {
          if (e.getEntity() instanceof ServerPlayer p) ForgeArsenal.recall(p);
        });
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerLoggedInEvent e) -> {
          if (e.getEntity() instanceof ServerPlayer p) {
            ForgeArsenal.recall(p);
            ForgeSynthesis.discover(p);
            var data = dev.heavenlyrestriction.RestrictionData.of(p);
            if (data.hasUUID("hephaestus_masterpiece"))
              ForgeLedger.get(p.getServer())
                  .designate(p.getUUID(), data.getUUID("hephaestus_masterpiece"));
            ForgeNetwork.ledgerAll(p);
          }
        });
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerLoggedOutEvent e) -> {
          if (e.getEntity() instanceof ServerPlayer p) {
            ForgeArsenal.recall(p);
            ForgeService.close(p);
          }
        });
    NeoForge.EVENT_BUS.addListener(
        (PlayerEvent.PlayerChangedDimensionEvent e) -> {
          if (e.getEntity() instanceof ServerPlayer p) {
            ForgeArsenal.recall(p);
            ForgeService.close(p);
            ForgeNetwork.ledger(p);
          }
        });
    TensuraEntityEvents.AWAKENING_EVENT.register(
        (e, hero) -> {
          ForgeSynthesis.awakening(e);
          return EventResult.pass();
        });
  }

  private ForgeEvents() {}
}
