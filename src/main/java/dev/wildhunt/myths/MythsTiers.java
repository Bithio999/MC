package dev.wildhunt.myths;

import dev.wildhunt.*;
import io.github.manasmods.manascore.skill.api.*;
import net.minecraft.tags.TagKey;

/** Public additive classification; never replace the native Ultimate enum. */
public final class MythsTiers {
    public static final TagKey<ManasSkill> GODLY=TagKey.create(SkillAPI.getSkillRegistryKey(),WildHunt.id("godly_skills"));
    private MythsTiers() {}
    public static boolean isGodly(ManasSkill skill) {
        if(skill instanceof GodlySkill || skill instanceof MythicSkill s && s.kind.godly())return true;
        var id=skill.getRegistryName();return id!=null && SkillAPI.getSkillRegistry().delegate(id).is(GODLY);
    }
}
