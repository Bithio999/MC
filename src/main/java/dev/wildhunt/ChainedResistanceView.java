package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

public interface ChainedResistanceView {
    ManasSkill myths$nullificationForm();
    List<Holder<MobEffect>> myths$immuneEffects(ManasSkillInstance i,LivingEntity owner);
}
