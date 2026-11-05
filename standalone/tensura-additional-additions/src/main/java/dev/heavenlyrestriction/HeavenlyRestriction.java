package dev.heavenlyrestriction;

import dev.architectury.registry.registries.*;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.manascore.skill.api.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import java.util.List;

/** Independently registered addon: no Tensura Myths classes or registries are required. */
@Mod(HeavenlyRestriction.ID)
public final class HeavenlyRestriction {
    public static final String ID="heavenlyrestriction";
    private static final Registrar<ManasRace> RACES=RaceAPI.getRaceRegistry();
    private static final Registrar<ManasSkill> SKILLS=SkillAPI.getSkillRegistry();
    public static final RegistrySupplier<UnboundSkill> UNBOUND=SKILLS.register(id("unbound"),UnboundSkill::new);
    public static final RegistrySupplier<ChainedSkill> CHAINED=SKILLS.register(id("chained"),ChainedSkill::new);
    public static final RegistrySupplier<ParallelWorldJumperSkill> PARALLEL_WORLD_JUMPER=SKILLS.register(id("parallel_world_jumper"),ParallelWorldJumperSkill::new);
    public static final RegistrySupplier<ExponentiationSkill> EXPONENTIATION=SKILLS.register(id("exponentiation"),ExponentiationSkill::new);
    public static final RegistrySupplier<ProspectorSkill> PROSPECTOR=SKILLS.register(id("prospector"),ProspectorSkill::new);
    public static final RegistrySupplier<LoanSharkSkill> LOAN_SHARK=SKILLS.register(id("loan_shark"),LoanSharkSkill::new);
    public static final RegistrySupplier<SplitMindSkill> SPLIT_MIND=SKILLS.register(id("split_mind"),SplitMindSkill::new);
    public static final RegistrySupplier<MaraudersMapSkill> MARAUDERS_MAP=SKILLS.register(id("marauders_map"),MaraudersMapSkill::new);
    public static final RegistrySupplier<FusionSkill> FUSION=SKILLS.register(id("fusion"),FusionSkill::new);
    public static final RegistrySupplier<SchemaArt> SCHEMA=SKILLS.register(id("schema"),SchemaArt::new);
    public static final RegistrySupplier<InstantTransmissionArt> INSTANT_TRANSMISSION=SKILLS.register(id("instant_transmission"),InstantTransmissionArt::new);
    public static final RegistrySupplier<AirSovereigntyArt> AIR_SOVEREIGNTY=SKILLS.register(id("air_sovereignty"),AirSovereigntyArt::new);
    public static final List<RegistrySupplier<RestrictedRace>> RESTRICTED=java.util.stream.IntStream.range(0,5).mapToObj(stage->RACES.register(id(RestrictedRace.IDS[stage]),()->new RestrictedRace(stage))).toList();
    public static final List<RegistrySupplier<AuraRegenerationSkill>> REGENERATION=List.of(
        SKILLS.register(id("hunters_recovery"),()->new AuraRegenerationSkill(1)),
        SKILLS.register(id("apex_renewal"),()->new AuraRegenerationSkill(2)),
        SKILLS.register(id("sovereign_rebirth"),()->new AuraRegenerationSkill(3)));
    public HeavenlyRestriction(net.neoforged.bus.api.IEventBus modBus,net.neoforged.fml.ModContainer container){
        HeavenlyArsenal.register(modBus);
        dev.heavenlyrestriction.chromatic.ChromaticContent.register(modBus);
        modBus.addListener(dev.heavenlyrestriction.cartography.MapNetwork::register);
        NeoForge.EVENT_BUS.addListener(dev.heavenlyrestriction.cartography.MapService::tick);
        NeoForge.EVENT_BUS.addListener(dev.heavenlyrestriction.cartography.MapService::loaded);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e)->{if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)dev.heavenlyrestriction.cartography.MapService.login(p);});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e)->{if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p){dev.heavenlyrestriction.cartography.MapService.changedDimension(p);}});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->{if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)dev.heavenlyrestriction.cartography.MapService.close(p);});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e)->dev.heavenlyrestriction.cartography.MapService.clear());
        modBus.addListener(dev.heavenlyrestriction.splitmind.SplitMindNetwork::register);
        NeoForge.EVENT_BUS.addListener(dev.heavenlyrestriction.splitmind.SplitMindCasting::tick);
        NeoForge.EVENT_BUS.addListener(dev.heavenlyrestriction.splitmind.SplitMindCasting::spawned);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->dev.heavenlyrestriction.splitmind.SplitMindCasting.cancel(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e)->dev.heavenlyrestriction.splitmind.SplitMindCasting.cancel(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e)->dev.heavenlyrestriction.splitmind.SplitMindCasting.cancel(e.getEntity()));
        dev.heavenlyrestriction.highrealm.HighRealm.register(modBus,container);
        dev.heavenlyrestriction.loans.LoanShark.register();
        modBus.addListener(dev.heavenlyrestriction.loans.LoanNetwork::register);
        NeoForge.EVENT_BUS.addListener(dev.heavenlyrestriction.exponentiation.Exponentiation::tick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,dev.heavenlyrestriction.exponentiation.ExponentiationRewards::block);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,dev.heavenlyrestriction.exponentiation.ExponentiationRewards::mob);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,dev.heavenlyrestriction.exponentiation.ExponentiationRewards::xp);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent event)->ProspectorOres.clear());
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,ProspectorMining::speed);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,ProspectorMining::finds);
        InstantTransmissionArt.register();
        SchemaArt.register();
        dev.heavenlyrestriction.fusion.Fusion.register();
        modBus.addListener(dev.heavenlyrestriction.fusion.FusionNetwork::register);
        dev.heavenlyrestriction.worldhop.WorldHop.register();
        modBus.addListener(dev.heavenlyrestriction.worldhop.WorldHopNetwork::register);
        modBus.addListener(SchemaNetwork::register);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,ChainedReincarnationConfig.SPEC,"tensura-heavenly-restriction-reincarnation.toml");
        ArtVisualEntity.register();
        SkillEvents.ACTIVATE_SKILL.register(dev.heavenlyrestriction.splitmind.SplitMindCasting::manualActivation);
        SkillEvents.UNLOCK_SKILL.register(ChainedState::unlock);
        SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.register(ChainedState::damage);
        NeoForge.EVENT_BUS.addListener(RestrictionEvents::tick);
        NeoForge.EVENT_BUS.addListener(RestrictionEvents::login);
        NeoForge.EVENT_BUS.addListener(RestrictionEvents::onClone);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,AirSovereigntyArt::clonePlayer);
        NeoForge.EVENT_BUS.addListener(AirSovereigntyArt::login);
        NeoForge.EVENT_BUS.addListener(ChainedCommands::register);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,UnboundCounter::incoming);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,UnboundCritical::critical);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e)->UnboundCounter.clear(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->UnboundCounter.clear(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e)->UnboundCounter.clear(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e)->UnboundCounter.clear(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event)->AuraRegenerationSkill.onDeath(event));
    }
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(ID,path);}
}
