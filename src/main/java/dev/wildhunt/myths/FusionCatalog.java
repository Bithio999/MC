package dev.wildhunt.myths;

import com.google.gson.*;
import dev.wildhunt.WildHunt;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class FusionCatalog {
    public record Profile(String key, String name, RaceFamily first, RaceFamily second, double aura, double magicules, String trait) {
        public DuoRules.Gains gains() { return new DuoRules.Gains(aura,magicules); }
        public String description() { return name + " unites " + first.title + " and " + second.title + ". " + trait; }
    }
    public static final Map<String, Profile> PROFILES = new LinkedHashMap<>();
    public static final Map<String, RegistrySupplier<FusionRace>> RACES = new LinkedHashMap<>();
    static {
        try(var stream=Objects.requireNonNull(FusionCatalog.class.getResourceAsStream("/assets/wildhunt/myths/fusions.json"));
            var reader=new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            for(var element:JsonParser.parseReader(reader).getAsJsonArray()) {
                var o=element.getAsJsonObject();
                var a=RaceFamily.byKey(o.get("first").getAsString()).orElseThrow();
                var b=RaceFamily.byKey(o.get("second").getAsString()).orElseThrow();
                var key=DuoRules.pairKey(a.key(),b.key());
                var p=new Profile(key,o.get("name").getAsString(),a,b,o.get("aura").getAsDouble(),o.get("magicules").getAsDouble(),o.get("trait").getAsString());
                if(PROFILES.put(key,p)!=null)throw new IllegalStateException("Duplicate fusion "+key);
                p.gains();
                RACES.put(key,RaceAPI.getRaceRegistry().register(WildHunt.id("convergence/"+key),()->new FusionRace(p)));
            }
            if(PROFILES.size()!=105)throw new IllegalStateException("Every pair of native families must have a fusion");
        } catch(Exception e) { throw new IllegalStateException("Cannot load Tensura Myths fusion definitions",e); }
    }
    public static Profile profile(RaceFamily a, RaceFamily b) { return PROFILES.get(DuoRules.pairKey(a.key(),b.key())); }
    public static void init() {}
    private FusionCatalog() {}
}
