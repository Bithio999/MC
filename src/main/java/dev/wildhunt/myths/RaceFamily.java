package dev.wildhunt.myths;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Fifteen native ancestral families. Hunter evolutions remain part of Human. */
public enum RaceFamily {
    BEASTFOLK("Beastfolk", "beastfolk", "divine_beast", "beast_lord spirit_beast divine_beast", "Feral", "Instinct", 0xD9A663),
    HARPY("Harpy", "harpy", "divine_bird", "harpy_queen spirit_bird divine_bird", "Sky", "Tempest", 0x91CEE8),
    OGRE("Ogre", "ogre", "divine_oni", "kijin enlightened_ogre mystic_oni wicked_oni spirit_oni death_oni divine_oni divine_fighter", "Oni", "Wrath", 0xD55863),
    GOBLIN("Goblin", "goblin", "divine_oni", "hobgoblin enlightened_hobgoblin hobgoblin_saint", "Verdant", "Guile", 0x82B55B),
    GIANT("Giant", "giant", "divine_giant", "ancient_giant divine_giant", "Titan", "Might", 0xC4A88C),
    ORC("Orc", "orc", "divine_boar", "high_orc spirit_boar orc_lord orc_disaster divine_boar", "Iron", "Hunger", 0x8E9270),
    LIZARDMAN("Lizardman", "lizardman", "divine_dragon", "dragonewt true_dragonewt divine_dragon", "Drake", "Scales", 0x69B899),
    MERFOLK("Merfolk", "merfolk", "divine_fish", "enlightened_merfolk merfolk_saint divine_fish", "Tidal", "Depths", 0x63C8C9),
    ELF("Elf", "elf", "divine_elf", "enlightened_elf elf_saint divine_elf", "Astral", "Wisdom", 0xB0C5FF),
    HUMAN("Human", "human", "divine_human", "enlightened_human human_saint divine_human", "Sovereign", "Will", 0xE6CD9F),
    DWARF("Dwarf", "dwarf", "divine_dwarf", "enlightened_dwarf dwarf_saint divine_dwarf", "Runic", "Forge", 0xC78B56),
    SLIME("Slime", "slime", "god_slime", "metal_slime demon_slime god_slime", "Protean", "Genesis", 0x79C9F5),
    VAMPIRE("Vampire", "ghoul", "divine_vampire", "vampire vampire_overcomer vampire_lord divine_vampire", "Crimson", "Eclipse", 0xBD6AA7),
    WIGHT("Wight", "wight", "divine_skeleton", "wight_king spirit_skeleton divine_skeleton", "Pale", "Eternity", 0xC7CED7),
    DAEMON("Daemon", "lesser_daemon", "devil_lord", "greater_daemon arch_daemon daemon_lord devil_lord", "Abyssal", "Dominion", 0xA281C3);

    public final String title, base, terminal, lineage, epithet, domain;
    public final int color;
    RaceFamily(String title, String base, String terminal, String lineage, String epithet, String domain, int color) {
        this.title=title; this.base=base; this.terminal=terminal; this.lineage=lineage; this.epithet=epithet; this.domain=domain; this.color=color;
    }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public ManasRace baseRace() { return RaceAPI.getRaceRegistry().get(ResourceLocation.fromNamespaceAndPath("tensura", base)); }
    public ManasRace finalRace() { return RaceAPI.getRaceRegistry().get(ResourceLocation.fromNamespaceAndPath("tensura", terminal)); }
    public static Optional<RaceFamily> from(ManasRace race) {
        var id=race.getRegistryName();
        if(id==null) return Optional.empty();
        if(id.getNamespace().equals("wildhunt") && Set.of("awakened_hunter","apex_hunter","sovereign_hunter").contains(id.getPath()))return Optional.of(HUMAN);
        if(!id.getNamespace().equals("tensura"))return Optional.empty();
        return Arrays.stream(values()).filter(f->f.base.equals(id.getPath()) || Arrays.asList(f.lineage.split(" ")).contains(id.getPath())).findFirst();
    }
    public static Optional<RaceFamily> byKey(String key) { return Arrays.stream(values()).filter(f->f.key().equals(key)).findFirst(); }
}
