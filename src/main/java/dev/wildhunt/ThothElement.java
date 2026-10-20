package dev.wildhunt;

import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.entity.variant.MagicCircleVariant;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

/** Thoth's passive-backed spectrum, shared by server damage and client chapter choreography. */
public enum ThothElement {
    FIRE("fire",TensuraDamageTypes.FIRE_ELEMENTAL,MagicCircleVariant.FLAME,0xFFFF925B),
    WATER("water",TensuraDamageTypes.WATER_ELEMENTAL,MagicCircleVariant.WATER,0xFF67DBEC),
    LIGHTNING("lightning",TensuraDamageTypes.LIGHTNING_ELEMENTAL,MagicCircleVariant.LIGHTNING,0xFFFFD875),
    EARTH("earth",TensuraDamageTypes.EARTH_ELEMENTAL,MagicCircleVariant.EARTH,0xFFD7AD79),
    WIND("wind",TensuraDamageTypes.WIND_ELEMENTAL,MagicCircleVariant.WIND,0xFFACF3B6),
    LIGHT("light",TensuraDamageTypes.LIGHT_ELEMENTAL,MagicCircleVariant.LIGHT,0xFFFFF3CA),
    SPACE("space",TensuraDamageTypes.SPACE_ELEMENTAL,MagicCircleVariant.SPACE,0xFFD0A5FF);

    public final String id;
    public final ResourceKey<DamageType> damage;
    public final MagicCircleVariant circle;
    public final int color;
    private static final ThothElement[] SPECTRUM=values();
    public static final int COUNT=SPECTRUM.length,LEXICON_LIFETIME=70,LEXICON_FIRST=12,LEXICON_LAST=52,
        LIBRARY_DURATION=200,LIBRARY_FIRST=10,LIBRARY_INTERVAL=5;
    public static final float LEXICON_DAMAGE=1100,LIBRARY_DAMAGE=800;
    ThothElement(String id,ResourceKey<DamageType> damage,MagicCircleVariant circle,int color){this.id=id;this.damage=damage;this.circle=circle;this.color=color;}
    public static ThothElement at(int chapter){return SPECTRUM[Math.floorMod(chapter,COUNT)];}
    public static ThothElement named(String id){for(var element:SPECTRUM)if(element.id.equals(id))return element;return null;}
    public static int lexiconTick(int chapter){return LEXICON_FIRST+Math.clamp(chapter,0,COUNT-1)*(LEXICON_LAST-LEXICON_FIRST)/(COUNT-1);}
    public static int lexiconChapter(float age){for(int n=COUNT-1;n>0;n--)if(age>=lexiconTick(n))return n;return 0;}
    public static int libraryBook(int chapter){return Math.floorMod(chapter,3);}
}
