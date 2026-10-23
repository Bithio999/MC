package dev.wildhunt;

import net.minecraft.world.entity.LivingEntity;

/** Applies arena membership and encounter bounds to every outgoing skill, including auxiliaries. */
public interface MythEncounter {
    boolean inEncounter(LivingEntity target);
}
