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

/** Encounter-only primordial lineage. Not a player evolution or secondary ancestry. */
public final class OriginSovereignRace extends TensuraRace {
    public OriginSovereignRace(){super(Difficulty.EXTREME);}
    @Override public MutableComponent getName(){return Component.literal("Origin Sovereign");}
    @Override public MutableComponent getRaceDescription(){return Component.literal("The first will to give form to a world, and the last to relinquish it. Eschaton's divine spiritual lineage: 500 million aura and 500 million magicules.");}
    @Override public ResourceLocation getRaceIcon(){return WildHunt.id("textures/race/origin_sovereign.png");}
    @Override public Pair<Double,Double> getBaseAuraRange(){return Pair.of(500_000_000d,500_000_000d);}
    @Override public Pair<Double,Double> getBaseMagiculeRange(){return Pair.of(500_000_000d,500_000_000d);}
    @Override public Alignment getAlignment(){return Alignment.CHAOS;}
    @Override public List<ManasRace> getNextEvolutions(ManasRaceInstance i,LivingEntity e){return List.of();}
    @Override public List<ManasRace> getPreviousEvolutions(ManasRaceInstance i,LivingEntity e){return List.of();}
    @Override public List<ManasSkill> getIntrinsicSkills(ManasRaceInstance i,LivingEntity e){return List.of(EschatonContent.DEMIURGE.get(),WildHunt.GODLY.get(),WildHunt.MYTHIC.get(MythicKind.THOTH).get());}
}
