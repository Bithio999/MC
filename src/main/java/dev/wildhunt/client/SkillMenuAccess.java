package dev.wildhunt.client;

import io.github.manasmods.tensura.ability.skill.Skill;

/** Explicit access to the native menu filter, avoiding reflection or layout guesses. */
public interface SkillMenuAccess { Skill.SkillType myths$filter(); }
