package dev.heavenlyrestriction;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.item.misc.BattlewillManualItem;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import static dev.heavenlyrestriction.TestFixtures.*;
import static dev.heavenlyrestriction.ChainedGameTests.near;
@GameTestHolder(HeavenlyRestriction.ID) @PrefixGameTestTemplate(false)
public final class SchemaManualGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeRandomManualPoolIncludesSchema(GameTestHelper h){var p=player(h);var stack=BattlewillManualItem.createForBattlewill(HeavenlyRestriction.SCHEMA.get());var book=(BattlewillManualItem)stack.getItem();try{var method=BattlewillManualItem.class.getDeclaredMethod("getRandomBattlewill",net.minecraft.world.entity.LivingEntity.class);method.setAccessible(true);p.getRandom().setSeed(1510);boolean found=false;for(int n=0;n<2000 && !found;n++)found=method.invoke(book,p)==HeavenlyRestriction.SCHEMA.get();h.assertTrue(found,"Schema missing from native random manual pool");}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}h.succeed();}
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeSchemaManualStartsThousandPointLearningAndKeepsProgress(GameTestHelper h){var p=player(h);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).isEmpty(),"Schema auto-discovered without manual");var stack=BattlewillManualItem.createForBattlewill(HeavenlyRestriction.SCHEMA.get());var book=(BattlewillManualItem)stack.getItem();book.releaseUsing(stack,p.level(),p,190);var instance=SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow();near(h,instance.getMastery(),-1000,"Book bypassed learning requirement");h.assertTrue(stack.isEmpty(),"Book was not consumed");h.assertTrue(p.getCooldowns().isOnCooldown(book),"Native book cooldown missing");instance.setMastery(-450);var again=BattlewillManualItem.createForBattlewill(HeavenlyRestriction.SCHEMA.get());book.releaseUsing(again,p.level(),p,190);near(h,SkillAPI.getSkillsFrom(p).getSkill(HeavenlyRestriction.SCHEMA.get()).orElseThrow().getMastery(),-450,"Repeated book reset progress");h.succeed();}
}
