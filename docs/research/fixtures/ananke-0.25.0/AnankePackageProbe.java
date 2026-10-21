package dev.wildhunt;
import com.google.gson.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;
@GameTestHolder("wildhunt_audit")
@PrefixGameTestTemplate(false)
public class AnankePackageProbe {
 static JsonObject state(LivingEntity e){
  var j=new JsonObject();j.addProperty("type",e.getType().toString());j.addProperty("hp",e.getHealth());j.addProperty("max_hp",e.getMaxHealth());j.addProperty("max_aura",EnergyHelper.getMaxAura(e));j.addProperty("max_magicule",EnergyHelper.getMaxMagicule(e));j.addProperty("shp",TensuraStorages.getExistenceFrom(e).getSpiritualHealth());j.addProperty("max_shp",e.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH));j.addProperty("melee_dodge",e.getAttributeValue(TensuraAttributes.AUTO_MELEE_DODGE_CHANCE));j.addProperty("projectile_dodge",e.getAttributeValue(TensuraAttributes.AUTO_PROJECTILE_DODGE_CHANCE));j.addProperty("resistance_degradation",e.getAttributeValue(TensuraAttributes.RESISTANCE_DEGRADATION));j.addProperty("space_boost",e.getAttributeValue(TensuraAttributes.SPACE_BOOST));
  var skills=new JsonArray();for(var i:SkillAPI.getSkillsFrom(e).getLearnedSkills())if(i.getMastery()>=0){var r=new JsonObject();r.addProperty("id",i.getSkillId().toString());r.addProperty("mastery",i.getMastery());r.addProperty("toggled",i.isToggled());r.addProperty("toggleable",i.canBeToggled(e));skills.add(r);}j.add("skills",skills);return j;
 }
 @GameTest(template="empty",timeoutTicks=120)
 public static void inspectPackagesAndRecovery(GameTestHelper h){
  var p=MythicGameTests.player(h);var list=new ArrayList<LivingEntity>();var b=AnankeItems.summon(h.getLevel(),p.position().add(8,0,0));h.assertTrue(b!=null,"Boss fixture failed");b.setNoAi(true);list.add(b);
  int index=0;for(String id:List.of("tensura:hinata_sakaguchi","tensura:gazel_dwargo","tensura:charybdis")){var mob=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)).create(h.getLevel());h.assertTrue(mob!=null,"Missing baseline "+id);mob.setPos(p.position().add(24+index++*12,0,20));mob.setNoAi(true);mob.setNoGravity(true);mob.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(mob.blockPosition()),MobSpawnType.COMMAND,null);h.getLevel().addFreshEntity(mob);list.add(mob);}
  var result=new JsonObject();var initial=new JsonArray();for(var e:list)initial.add(state(e));result.add("initial",initial);
  b.setHealth(b.getMaxHealth()-1000);TensuraStorages.getExistenceFrom(b).setSpiritualHealth(b.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)-1000);
  h.runAfterDelay(40,()->{var later=new JsonArray();for(var e:list)later.add(state(e));result.add("after_40_ticks",later);
   var physical=SkillAPI.getSkillsFrom(b).getSkill(ResourceLocation.parse("tensura:physical_attack_resistance")).orElseThrow();var resist=(io.github.manasmods.tensura.ability.skill.resist.ResistSkill)physical.getSkill();var attacker=MythicGameTests.target(h,b.position().add(4,0,0));var source=b.damageSources().mobAttack(attacker);var off=io.github.manasmods.manascore.network.api.util.Changeable.of(100f);boolean offPass=resist.onTakenDamage(physical,b,source,off);physical.setToggled(true);var on=io.github.manasmods.manascore.network.api.util.Changeable.of(100f);boolean onPass=resist.onTakenDamage(physical,b,source,on);physical.setToggled(false);var compare=new JsonObject();compare.addProperty("physical_source_recognized",resist.isDamageResisted(b,source,physical));compare.addProperty("off_allows_hit",offPass);compare.addProperty("off_amount",off.get());compare.addProperty("on_allows_hit",onPass);compare.addProperty("on_amount",on.get());result.add("physical_native_callback",compare);
   var saved=new net.minecraft.nbt.CompoundTag();b.save(saved);b.discard();var loaded=(AnankeEntity)EntityType.loadEntityRecursive(saved,h.getLevel(),e->e);h.getLevel().addFreshEntity(loaded);result.add("reloaded",state(loaded));loaded.setHealth(loaded.getMaxHealth());TensuraStorages.getExistenceFrom(loaded).setSpiritualHealth(119000);
   h.runAfterDelay(20,()->{result.add("full_hp_soul_recovery_20_ticks",state(loaded));try{Files.writeString(Path.of("/tmp/ananke-package-audit/runtime.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception ex){throw new IllegalStateException(ex);}h.succeed();});});
 }
}
