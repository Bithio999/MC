package dev.heavenlyrestriction;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

/** Server-authoritative operator overrides for the Fettered Human reincarnation pool. */
public final class ChainedReincarnationConfig {
    private static final ModConfigSpec.Builder B=new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue BLOCK_MAGICULE_COSTS=B.comment("Exclude skills with magicule-consuming abilities from Fettered Human reincarnation rolls. Acquisition costs are ignored.")
        .define("blockMagiculeCosts",true);
    public static final ModConfigSpec.BooleanValue BLOCK_SKILL_TAKING=B.comment("Exclude skills that steal, copy or harvest target skills. Pride is exempt from this automatic rule.")
        .define("blockSkillTaking",true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST=B.comment("Additional skill registry IDs to exclude. This list takes priority over the allowlist.")
        .defineListAllowEmpty("extraBlacklist",List.of(),ChainedReincarnationConfig::validId);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWLIST=B.comment("Skill registry IDs exempt from the automatic magicule-cost and skill-taking rules. Does not add skills to Tensura's configured pools or bypass native uniqueness rules.")
        .defineListAllowEmpty("allowlist",List.of(),ChainedReincarnationConfig::validId);
    public static final ModConfigSpec SPEC=B.build();
    private ChainedReincarnationConfig(){}
    private static boolean validId(Object value){return value instanceof String id && ResourceLocation.tryParse(id)!=null;}
}
