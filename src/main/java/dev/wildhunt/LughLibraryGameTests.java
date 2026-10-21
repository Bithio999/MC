package dev.wildhunt;

import dev.wildhunt.myths.MythsNetwork;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import static dev.wildhunt.MythicGameTests.*;

@GameTestHolder(WildHunt.ID)
@PrefixGameTestTemplate(false)
public final class LughLibraryGameTests {
    private static CompoundTag reading(int mode,float damage){
        var t=new CompoundTag();t.putString("name","Rune Smith");t.putString("source","wildhunt:rune_smith");t.putInt("mode",mode);t.putString("mode_name","Rune technique "+mode);t.putString("type","tensura:earth_elemental");t.putString("source_type","tensura:earth_elemental");t.putInt("form",2);t.putFloat("damage",damage);t.putFloat("observed_damage",damage/0.8f);return t;
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void legacyArchiveMigratesWithoutLosingLearnedArtsOrSelection(GameTestHelper h){
        var i=learn(player(h),MythicKind.LUGH);var old=LughArts.archive(i).copy();var custom=reading(0,4800);old.set(1,custom);
        old.getCompound(0).putFloat("damage",2200);var tag=i.getOrCreateTag();tag.remove("art_library");tag.remove("art_bindings");tag.put("archive",old);tag.putInt("selected_art",1);
        h.assertTrue(LughArts.library(i).size()==4 && LughArts.archive(i).size()==3 && LughArts.selectedName(i).equals("Rune technique 0") && LughArts.archive(i).getCompound(0).getFloat("damage")==3800,"Migration erased a custom binding, old selection or origin upgrade");
        var copy=ManasSkillInstance.fromNBT(i.toNBT());h.assertTrue(LughArts.library(copy).equals(LughArts.library(i)) && LughArts.selectedName(copy).equals("Rune technique 0") && !tag.contains("archive"),"Library or stable bindings failed native save/load");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void libraryAccumulatesDistinctModesAndRefreshesBoundEntryByStableId(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);LughArts.remember(i,reading(0,4000));LughArts.remember(i,reading(1,5000));
        var list=LughArts.library(i);String id=list.getCompound(3).getString("entry");
        h.assertTrue(list.size()==5 && LughArts.selectedName(i).equals("Sovereign Edge"),"Appraisal replaced a binding or merged distinct source modes");
        h.assertTrue(LughArts.configure(p,"lugh_bind","1|"+id) && LughArts.selectedName(i).equals("Rune technique 0"),"Server did not bind the recorded technique");
        LughArts.remember(i,reading(0,5800));h.assertTrue(LughArts.library(i).size()==5 && LughArts.archive(i).getCompound(1).getString("entry").equals(id) && LughArts.archive(i).getCompound(1).getFloat("damage")==5800,"Refresh duplicated a record or broke its binding");
        var copy=ManasSkillInstance.fromNBT(i.toNBT());h.assertTrue(LughArts.archive(copy).getCompound(1).getFloat("damage")==5800 && copy.getOrCreateTag().getInt("selected_art")==1,"Refreshed binding did not persist");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void bindingRejectsForgedIdsInvalidSlotsAndMissingOwnership(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);String id=LughArts.library(i).getCompound(0).getString("entry");var saved=i.toNBT();
        for(var argument:List.of("-1|"+id,"3|"+id,"0|forged","0|"+id+"|90000000","bad|"+id,""))h.assertTrue(!LughArts.configure(p,"lugh_bind",argument),"Invalid binding accepted: "+argument);
        h.assertTrue(!LughArts.configure(p,"lugh_select","0|extra") && !LughArts.configure(p,"lugh_damage","2") && i.toNBT().equals(saved),"Rejected intent changed stored parameters");
        SkillAPI.getSkillsFrom(p).forgetSkill(i);h.assertTrue(!LughArts.configure(p,"lugh_bind","0|"+id) && !LughArts.snapshot(p).getBoolean("owned"),"Forgotten Lugh still accepted its library");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void normalSkillBankAndBindingAreFreeWhileCoolingDownAtZeroMagicules(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);var s=(MythicSkill)i.getSkill();var energy=TensuraStorages.getExistenceFrom(p);energy.setMagicule(0);i.setCoolDown(8,0);i.setCoolDown(9,1);double mastery=i.getMastery();
        p.setShiftKeyDown(false);h.assertTrue(s.getMagiculeCost(p,i,0)==0 && s.canIgnoreCoolDown(i,p,0),"Native input blocked free menu access");cast(p,i,0);p.setShiftKeyDown(false);
        String id=LughArts.library(i).getCompound(2).getString("entry");h.assertTrue(LughArts.configure(p,"lugh_bind","1|"+id) && LughArts.configure(p,"lugh_select","2"),"Free binding or active selection was blocked");
        h.assertTrue(energy.getMagicule()==0 && i.getCoolDown(0)==8 && i.getCoolDown(1)==9 && i.getMastery()==mastery,"Menu/binding spent/refunded energy, reset cooldown or awarded mastery");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void nativeObservationAndMenuExposeActualTechniqueAndSeparateDamageValues(GameTestHelper h){
        var p=player(h);var actor=target(h,p.position().add(8,0,0));var victim=target(h,p.position().add(10,0,0));var i=learn(p,MythicKind.LUGH);var nativeSkill=learn(actor,MythicKind.RUNE_SMITH);
        var source=((MythicSkill)nativeSkill.getSkill()).createSource(nativeSkill,actor,TensuraDamageTypes.EARTH_ELEMENTAL,1);
        LughArts.observeDamage(SkillAPI.getSkillsFrom(victim),victim,source,Changeable.of(6500f));LughArts.remember(i,HunterData.of(actor).getCompound("last_technique"));
        i.setMastery(i.getMaxMastery());var state=LughArts.snapshot(p);var row=state.getList("entries",Tag.TAG_COMPOUND).getCompound(3);
        h.assertTrue(state.getBoolean("lugh_library") && state.getFloat("mastery_scale")==1.5f && state.getList("bindings",Tag.TAG_STRING).size()==3,"Library state missed ownership, mastery or bindings");
        h.assertTrue(row.getFloat("observed_damage")==6500 && row.getFloat("damage")==5200 && row.getInt("mode")==1 && row.getString("mode_name").equals("Command Detonation") && row.getString("source_type").equals("tensura:earth_elemental"),"Menu parameters do not match the actual observed source/mode");
        row.putFloat("damage",999999);h.assertTrue(LughArts.library(i).getCompound(3).getFloat("damage")==5200,"Menu state aliased authoritative server records");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void fullLibraryPreservesExistingEntriesAndAllowsRefresh(GameTestHelper h){
        var i=learn(player(h),MythicKind.LUGH);for(int mode=0;mode<LughArts.LIBRARY_LIMIT-3;mode++)h.assertTrue(LughArts.remember(i,reading(mode,4000)),"Library filled prematurely");
        h.assertTrue(!LughArts.remember(i,reading(999,5000)) && LughArts.library(i).size()==LughArts.LIBRARY_LIMIT,"Full library erased a record or exceeded its storage bound");
        String id=LughArts.library(i).getCompound(3).getString("entry");h.assertTrue(LughArts.remember(i,reading(0,5500)) && LughArts.library(i).getCompound(3).getString("entry").equals(id),"Full library refused to refresh a known technique");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void synthesisSnapshotsBindingsWhileLibraryRemainsEditable(GameTestHelper h){
        var p=player(h);var i=learn(p,MythicKind.LUGH);LughArts.remember(i,reading(0,5900));String id=LughArts.library(i).getCompound(3).getString("entry");LughArts.configure(p,"lugh_bind","0|"+id);cast(p,i,3);
        var plan=i.getOrCreateTag().getList("synthesis_plan",Tag.TAG_COMPOUND);LughArts.remember(i,reading(0,3600));LughArts.configure(p,"lugh_bind","0|"+LughArts.library(i).getCompound(0).getString("entry"));
        h.assertTrue(plan.getCompound(0).getFloat("damage")==5900 && plan.getCompound(0).getString("source").equals("wildhunt:rune_smith") && LughArts.archive(i).getCompound(0).getString("name").equals("Sovereign Edge"),"Editing library/bindings rewrote a pending Synthesis");h.succeed();
    }
}
