package dev.heavenlyrestriction;
import dev.heavenlyrestriction.worldhop.*;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.menu.ReincarnationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import static dev.heavenlyrestriction.TestFixtures.*;
@GameTestHolder(HeavenlyRestriction.ID)
@PrefixGameTestTemplate(false)
public final class WorldHopGameTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void dedicatedServersCannotRollLearnCopyOrActivateWorldJumper(GameTestHelper h){
        var p=player(h);var skill=HeavenlyRestriction.PARALLEL_WORLD_JUMPER.get();h.assertTrue(skill.createDefaultInstance().is(net.minecraft.tags.TagKey.create(SkillAPI.getSkillRegistryKey(),net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tensura","unique_skills"))),"Unique classification tag missing");h.assertTrue(!WorldHop.available(p),"Dedicated server was treated as single-player");
        h.assertTrue(ReincarnationMenu.getSkillPool().contains(skill),"Skill missing from registered pool");h.assertTrue(!ReincarnationMenu.getReincarnationSkills(ReincarnationMenu.getSkillPool(),h.getLevel(),true,p).contains(skill),"Multiplayer reincarnation could roll World Jumper");
        h.assertTrue(!SkillHelper.learnSkill(p,skill.createDefaultInstance()),"Multiplayer could acquire the skill");h.assertTrue(!SkillHelper.learnSkill(p,skill.createLearningInstance(p)),"Multiplayer could acquire a learning entry");
        SkillAPI.getSkillsFrom(p).updateSkill(skill.createDefaultInstance(),true);h.assertTrue(SkillAPI.getSkillsFrom(p).getSkill(skill).isEmpty(),"Copy/update bypassed multiplayer block");
        h.assertTrue(WorldHop.claim(p.server,java.util.UUID.randomUUID()).isEmpty(),"Unrequested transfer ticket could be claimed");
        h.assertTrue(!WorldSelect.select(p,"") && !WorldSelect.select(p,"../../world"),"Dedicated server accepted World Select packet");
        var learned=skill.createDefaultInstance();h.assertTrue(learned.getModes()==2 && skill.getModeName(learned,1).getString().equals("World Select"),"Learned World Select mode missing");learned.setMastery(-1);h.assertTrue(learned.getModes()==1 && skill.nextMode(p,learned,0,false)==0,"World Select exposed before learning");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void revisitKeepsDestinationWorldAndTransfersCurrentCharacter(GameTestHelper h){
        try{
            var p=player(h);Path base=Files.createTempDirectory("worldselect-nbt-"),source=Files.createDirectory(base.resolve("Current")),destination=Files.createDirectory(base.resolve("Visited"));
            p.getPersistentData().putString("latest_progress","new");var actor=p.saveWithoutId(new CompoundTag());actor.putInt("XpTotal",456);actor.putString("Dimension","minecraft:the_nether");
            var current=new CompoundTag();current.put("Player",actor);var root=new CompoundTag();root.put("Data",current);NbtIo.writeCompressed(root,source.resolve("level.dat"));
            var oldActor=actor.copy();oldActor.putInt("XpTotal",1);oldActor.putString("Dimension","minecraft:overworld");var pos=new ListTag();for(double n:new double[]{12.5,80,40.5})pos.add(DoubleTag.valueOf(n));oldActor.put("Pos",pos);
            var data=new CompoundTag();data.put("Player",oldActor);var gen=new CompoundTag();gen.putLong("seed",123);data.put("WorldGenSettings",gen);data.putLong("Time",9999);data.putString("LevelName","Home");var fight=new CompoundTag();fight.putBoolean("DragonKilled",true);data.put("DragonFight",fight);root=new CompoundTag();root.put("Data",data);NbtIo.writeCompressed(root,destination.resolve("level.dat"));
            WorldHopMetadata.revisit(destination,source,p.getUUID(),"Current","PWJ-return");var back=WorldHopMetadata.read(destination.resolve("level.dat")).getCompound("Data");var person=back.getCompound("Player");
            h.assertTrue(back.getCompound("WorldGenSettings").getLong("seed")==123 && back.getLong("Time")==9999 && back.getCompound("DragonFight").getBoolean("DragonKilled"),"Destination world metadata was reset");
            h.assertTrue(person.getInt("XpTotal")==456 && person.getCompound("NeoForgeData").getString("latest_progress").equals("new") && person.get("Inventory").equals(actor.get("Inventory")),"Current character was rolled back");
            h.assertTrue(person.getString("Dimension").equals("minecraft:overworld") && person.getList("Pos",Tag.TAG_DOUBLE).equals(pos) && person.getCompound("NeoForgeData").contains(WorldHopMetadata.MARKER),"Return landing did not use last-visited destination position");
            h.assertTrue(WorldHopMetadata.read(destination.resolve("playerdata").resolve(p.getUUID()+".dat")).equals(person),"UUID player save disagrees with level.dat");
            try(var paths=Files.walk(base)){for(Path f:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(f);}
        }catch(java.io.IOException ex){throw new RuntimeException(ex);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void metadataKeepsCharacterAndDimensionGeneratorsButChangesSeed(GameTestHelper h){
        try{
            var p=player(h);p.getPersistentData().putString("foreign_progress","retained");CompoundTag player=p.saveWithoutId(new CompoundTag());player.putInt("SpawnX",999);player.putString("SpawnDimension","minecraft:the_nether");
            CompoundTag data=new CompoundTag(),gen=new CompoundTag(),dimensions=new CompoundTag();dimensions.putString("sentinel","all dimension generators are preserved");gen.putLong("seed",123);gen.put("dimensions",dimensions);data.put("WorldGenSettings",gen);data.put("Player",player);data.putString("LevelName","Validation");data.put("DragonFight",new CompoundTag());
            CompoundTag root=new CompoundTag();root.put("Data",data);Path dir=Files.createTempDirectory("worldhop-nbt-");try{
                NbtIo.writeCompressed(root,dir.resolve("level.dat"));WorldHopMetadata.rewrite(dir,456,p.getUUID(),"Previous","PWJ-validation");var fresh=WorldHopMetadata.read(dir.resolve("level.dat")).getCompound("Data");var actor=fresh.getCompound("Player");
                h.assertTrue(fresh.getCompound("WorldGenSettings").getLong("seed")==456,"Seed not replaced");h.assertTrue(fresh.getCompound("WorldGenSettings").getCompound("dimensions").equals(dimensions),"Dimension generator/preset changed");
                h.assertTrue(actor.getCompound("NeoForgeData").getString("foreign_progress").equals("retained"),"Foreign player progression lost");h.assertTrue(actor.getCompound("NeoForgeData").contains(WorldHopMetadata.MARKER),"Landing marker not stored in actual NeoForge persistent data");
                h.assertTrue(!actor.contains("SpawnX") && !actor.contains("SpawnDimension") && fresh.contains("DragonFight",Tag.TAG_COMPOUND) && !fresh.getCompound("DragonFight").getBoolean("DragonKilled") && !fresh.getBoolean("initialized"),"Stale terrain-specific state retained");h.assertTrue(actor.getList("Motion",Tag.TAG_DOUBLE).size()==3,"Invalid motion vector");h.assertTrue(actor.get("Inventory").equals(player.get("Inventory")),"Inventory changed");
            }finally{Files.deleteIfExists(dir.resolve("level.dat"));Files.deleteIfExists(dir.resolve("WORLD-HOP.txt"));Files.deleteIfExists(dir.resolve("heavenlyrestriction-world-hop.json"));Files.deleteIfExists(dir);}
        }catch(java.io.IOException ex){throw new RuntimeException(ex);}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100)
    public static void landingRejectsWaterHazardsAndBlockedHeadroom(GameTestHelper h){
        var p=player(h);var at=BlockPos.containing(p.position()).offset(0,0,5);var level=h.getLevel();
        level.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(at.above(),Blocks.AIR.defaultBlockState());h.assertTrue(WorldHop.safe(level,p,at),"Solid clear ground rejected");
        level.setBlockAndUpdate(at,Blocks.WATER.defaultBlockState());h.assertTrue(!WorldHop.safe(level,p,at),"Water landing accepted");level.setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(at.below(),Blocks.MAGMA_BLOCK.defaultBlockState());h.assertTrue(!WorldHop.safe(level,p,at),"Magma landing accepted");level.setBlockAndUpdate(at.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(at.above(),Blocks.STONE.defaultBlockState());h.assertTrue(!WorldHop.safe(level,p,at),"Blocked headroom accepted");h.succeed();
    }
}
