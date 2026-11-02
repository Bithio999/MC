package dev.heavenlyrestriction.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.heavenlyrestriction.HeavenlyRestriction;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.item.misc.BattlewillManualItem;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Extend the resolved native manual pool without replacing server-configured entries. */
@Mixin(BattlewillManualItem.class)
public abstract class SchemaManualMixin {
    @ModifyExpressionValue(method="getRandomBattlewill",at=@At(value="INVOKE",target="Ljava/util/stream/Stream;toList()Ljava/util/List;"),remap=false)
    private List<ManasSkill> schema$manualPool(List<ManasSkill> configured){if(configured.contains(HeavenlyRestriction.SCHEMA.get()))return configured;var pool=new ArrayList<>(configured);pool.add(HeavenlyRestriction.SCHEMA.get());return List.copyOf(pool);}
}
