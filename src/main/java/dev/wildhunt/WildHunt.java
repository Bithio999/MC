package dev.wildhunt;

import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import java.util.EnumMap;
import java.util.List;

@Mod(WildHunt.ID)
public final class WildHunt {
    public static final String ID = "wildhunt";
    private static final Registrar<ManasRace> RACES = RaceAPI.getRaceRegistry();
    private static final Registrar<ManasSkill> SKILLS = SkillAPI.getSkillRegistry();
    public static final List<RegistrySupplier<HunterRace>> HUNTERS = List.of(
        RACES.register(id("awakened_hunter"), () -> new HunterRace(1)),
        RACES.register(id("apex_hunter"), () -> new HunterRace(2)),
        RACES.register(id("sovereign_hunter"), () -> new HunterRace(3)));
    public static final List<RegistrySupplier<AuraRegenerationSkill>> REGENERATION = List.of(
        SKILLS.register(id("hunters_recovery"), () -> new AuraRegenerationSkill(1)),
        SKILLS.register(id("apex_renewal"), () -> new AuraRegenerationSkill(2)),
        SKILLS.register(id("sovereign_rebirth"), () -> new AuraRegenerationSkill(3)));
    public static final EnumMap<Apex, RegistrySupplier<ApexSkill>> UNIQUE = new EnumMap<>(Apex.class);
    public static final EnumMap<Apex, RegistrySupplier<ApexSkill>> ULTIMATE = new EnumMap<>(Apex.class);
    static {
        for (Apex apex : Apex.values()) {
            UNIQUE.put(apex, SKILLS.register(id(apex.uniqueId), () -> new ApexSkill(apex, false)));
            ULTIMATE.put(apex, SKILLS.register(id(apex.ultimateId), () -> new RetiredApexSkill(apex)));
        }
    }
    public static final EnumMap<MythicKind, RegistrySupplier<MythicSkill>> MYTHIC = new EnumMap<>(MythicKind.class);
    static {for(var kind:MythicKind.values())MYTHIC.put(kind,SKILLS.register(id(kind.id()),()->new MythicSkill(kind)));}
    public static java.util.List<io.github.manasmods.manascore.skill.api.ManasSkill> rollableUniques(){
        var skills=new java.util.ArrayList<io.github.manasmods.manascore.skill.api.ManasSkill>();UNIQUE.values().forEach(s->skills.add(s.get()));
        MYTHIC.forEach((kind,s)->{if(kind.unique())skills.add(s.get());});return java.util.List.copyOf(skills);
    }
    public static final RegistrySupplier<GodlySkill> GODLY = SKILLS.register(id("aion"), GodlySkill::new);
    public WildHunt(net.neoforged.bus.api.IEventBus modBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,dev.wildhunt.myths.MythsConfig.SPEC,"tensura-myths-server.toml");
        dev.wildhunt.myths.FusionCatalog.init();
        modBus.addListener(dev.wildhunt.myths.MythsNetwork::register);
        NeoForge.EVENT_BUS.addListener(dev.wildhunt.myths.DuoSystem::tick);
        NeoForge.EVENT_BUS.addListener(dev.wildhunt.myths.DuoSystem::login);
        NeoForge.EVENT_BUS.addListener(dev.wildhunt.myths.MythsCommands::register);
        NeoForge.EVENT_BUS.addListener(GodlySkill::damaged);
        NeoForge.EVENT_BUS.addListener(GodlySkill::projectileTick);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,GodlyPassives::effectApplicable);
        io.github.manasmods.manascore.skill.api.SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.register(LughArts::observeDamage);
        io.github.manasmods.manascore.skill.api.SkillEvents.ACTIVATE_SKILL.register(LughArts::activate);
        io.github.manasmods.manascore.skill.api.SkillEvents.SKILL_DAMAGE_PRE_CALCULATION.register(GodlyPassives::modifyDamage);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e)->{GodlySkill.clearTransient(e.getEntity());MythicArts.clearTransient(e.getEntity());});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e)->{GodlySkill.clearTransient(e.getEntity());MythicArts.clearTransient(e.getEntity());});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e)->{GodlySkill.clearTransient(e.getEntity());MythicArts.clearTransient(e.getEntity());});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e)->{if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)dev.wildhunt.myths.SecondaryAbility.clear(p);});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e)->{if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)dev.wildhunt.myths.SecondaryAbility.clear(p);});
        // ManasCore owns these custom registrars, as it does for Tensura itself.
        // Avoid looking them up through Architectury's cross-mod constructor map.
        ArtVisualEntity.register(); HunterOtherworlders.register();
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, HunterEvents::onKill);
        NeoForge.EVENT_BUS.addListener(HunterEvents::onClone);
        NeoForge.EVENT_BUS.addListener(HunterCommands::register);
        NeoForge.EVENT_BUS.addListener(ApexTimers::onEntityTick);
        NeoForge.EVENT_BUS.addListener(ApexTimers::onLogin);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, (net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) -> AuraRegenerationSkill.onDeath(event));
        NeoForge.EVENT_BUS.addListener(ApexArts::onDamage);
        NeoForge.EVENT_BUS.addListener(ApexArts::onBreathe);
        NeoForge.EVENT_BUS.addListener(ApexArts::onIncomingDamage);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
