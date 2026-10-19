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
            ULTIMATE.put(apex, SKILLS.register(id(apex.ultimateId), () -> new ApexSkill(apex, true)));
        }
    }
    public WildHunt() {
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
