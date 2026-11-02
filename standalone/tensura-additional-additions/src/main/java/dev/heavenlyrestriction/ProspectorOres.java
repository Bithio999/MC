package dev.heavenlyrestriction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import java.util.*;

/** Native common ore tags plus explicit Tensura magic ore; tags can be refreshed by datapacks. */
public final class ProspectorOres {
    public record Kind(String family,int color,int glyph,Component label){}
    private static final Map<Block,Optional<Kind>> CACHE=new java.util.concurrent.ConcurrentHashMap<>();
    public static void clear(){CACHE.clear();}
    public static Kind kind(BlockState state){return CACHE.computeIfAbsent(state.getBlock(),block->Optional.ofNullable(classify(state))).orElse(null);}
    private static Kind classify(BlockState s){var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());String path=id.getPath(),family=path.replaceFirst("^deepslate_","").replaceFirst("^nether_","");boolean magic=id.getNamespace().equals("tensura") && (path.equals("magic_ore") || path.equals("deepslate_magic_ore"));
        if(!magic && !s.is(Tags.Blocks.ORES))return null;
        int color,glyph;
        if(magic){family="magic_ore";color=0xa18bff;glyph=10;}
        else if(s.is(Tags.Blocks.ORES_COAL)){family="coal";color=0x9aa8b4;glyph=0;}
        else if(s.is(Tags.Blocks.ORES_IRON)){family="iron";color=0xe7c5a3;glyph=1;}
        else if(s.is(Tags.Blocks.ORES_COPPER)){family="copper";color=0xe09b6c;glyph=2;}
        else if(s.is(Tags.Blocks.ORES_GOLD)){family="gold";color=0xf2d671;glyph=3;}
        else if(s.is(Tags.Blocks.ORES_REDSTONE)){family="redstone";color=0xf17b7b;glyph=4;}
        else if(s.is(Tags.Blocks.ORES_LAPIS)){family="lapis";color=0x729eea;glyph=5;}
        else if(s.is(Tags.Blocks.ORES_DIAMOND)){family="diamond";color=0x82e5e5;glyph=6;}
        else if(s.is(Tags.Blocks.ORES_EMERALD)){family="emerald";color=0x8de3a4;glyph=7;}
        else if(s.is(Tags.Blocks.ORES_QUARTZ)){family="quartz";color=0xe9e4db;glyph=8;}
        else if(s.is(Tags.Blocks.ORES_NETHERITE_SCRAP)){family="ancient_debris";color=0xc599b2;glyph=9;}
        else {family=id.getNamespace()+":"+family;int hash=family.hashCode();float hue=(Math.floorMod(hash,360))/360f;color=net.minecraft.util.Mth.hsvToRgb(hue,.32f,.90f);glyph=11+Math.floorMod(hash,3);}
        return new Kind(family,color,glyph,s.getBlock().getName());
    }
    private ProspectorOres(){}
}
