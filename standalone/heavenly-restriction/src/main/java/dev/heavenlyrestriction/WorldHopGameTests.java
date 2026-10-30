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
        h.assertTrue(WorldHop.claim(p.server,java.util.UUID.randomUUID()).isEmpty(),"Unrequested transfer ticket could be claimed");h.succeed();
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
