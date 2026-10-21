package dev.wildhunt;

import com.mojang.datafixers.util.Pair;
import io.github.manasmods.manascore.race.api.*;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.race.TensuraRace;
import io.github.manasmods.tensura.storage.Alignment;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

/** Ananke's encounter lineage; no player evolution or secondary-race route. */
public final class EclipseSeraphRace extends TensuraRace {
    public EclipseSeraphRace(){super(Difficulty.EXTREME);}
    @Override public MutableComponent getName(){return Component.literal("Eclipse Seraph");}
    @Override public MutableComponent getRaceDescription(){return Component.literal("A primordial arbiter whose six wings guard the unwritten end. Ananke's mythic boss lineage; 15 million aura and 15 million magicules.");}
    @Override public ResourceLocation getRaceIcon(){return WildHunt.id("textures/race/eclipse_seraph.png");}
    @Override public Pair<Double,Double> getBaseAuraRange(){return Pair.of(15_000_000d,15_000_000d);}
    @Override public Pair<Double,Double> getBaseMagiculeRange(){return Pair.of(15_000_000d,15_000_000d);}
    @Override public Alignment getAlignment(){return Alignment.CHAOS;}
    @Override public List<ManasRace> getNextEvolutions(ManasRaceInstance i,LivingEntity e){return List.of();}
    @Override public List<ManasRace> getPreviousEvolutions(ManasRaceInstance i,LivingEntity e){return List.of();}
    @Override public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance i,LivingEntity e){return List.of(MythBosses.MOIRA.get(),WildHunt.MYTHIC.get(MythicKind.THOTH).get());}
}
