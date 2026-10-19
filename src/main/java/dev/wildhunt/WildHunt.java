package dev.wildhunt;

import dev.architectury.registry.registries.DeferredRegister;
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
    private static final DeferredRegister<ManasRace> RACES = DeferredRegister.create(ID, RaceAPI.getRaceRegistryKey());
    private static final DeferredRegister<ManasSkill> SKILLS = DeferredRegister.create(ID, SkillAPI.getSkillRegistryKey());
    public static final List<RegistrySupplier<HunterRace>> HUNTERS = List.of(
        RACES.register("awakened_hunter", () -> new HunterRace(1)),
        RACES.register("apex_hunter", () -> new HunterRace(2)),
        RACES.register("sovereign_hunter", () -> new HunterRace(3)));
    public static final EnumMap<Apex, RegistrySupplier<ApexSkill>> UNIQUE = new EnumMap<>(Apex.class);
    public static final EnumMap<Apex, RegistrySupplier<ApexSkill>> ULTIMATE = new EnumMap<>(Apex.class);
    static {
        for (Apex apex : Apex.values()) {
            UNIQUE.put(apex, SKILLS.register(apex.uniqueId, () -> new ApexSkill(apex, false)));
            ULTIMATE.put(apex, SKILLS.register(apex.ultimateId, () -> new ApexSkill(apex, true)));
        }
    }
    public WildHunt() {
        RACES.register(); SKILLS.register(); ArtVisualEntity.register();
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, HunterEvents::onKill);
        NeoForge.EVENT_BUS.addListener(HunterEvents::onClone);
        NeoForge.EVENT_BUS.addListener(HunterCommands::register);
        NeoForge.EVENT_BUS.addListener(ApexTimers::onEntityTick);
        NeoForge.EVENT_BUS.addListener(ApexTimers::onLogin);
        NeoForge.EVENT_BUS.addListener(ApexArts::onDamage);
        NeoForge.EVENT_BUS.addListener(ApexArts::onBreathe);
        NeoForge.EVENT_BUS.addListener(ApexArts::onIncomingDamage);
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
}
