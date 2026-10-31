package dev.heavenlyrestriction.worldhop;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import com.google.gson.*;
import static org.junit.jupiter.api.Assertions.*;
class WorldHopSaveCompatTest {
    @TempDir Path world;
    Path tracking()throws IOException{Path p=world.resolve("data/starterkit/tracking.json");Files.createDirectories(p.getParent());return p;}
    @Test void worldNamesRemainStableWhenNativeSaveDropsCustomLevelTags()throws Exception{
        var first=WorldHopSaveCompat.advanceHistory(world,"Original world",123,"Original");assertEquals("Original world",first.name());assertEquals(1,first.hop());
        var second=WorldHopSaveCompat.advanceHistory(world,"Original world · Parallel 1",456,"PWJ-first");assertEquals("Original world",second.name());assertEquals(2,second.hop());var json=JsonParser.parseString(Files.readString(world.resolve("heavenlyrestriction-world-hop.json"))).getAsJsonObject();assertEquals("PWJ-first",json.get("previous").getAsString());assertEquals(456,json.get("seed").getAsLong());
    }
    @Test void carriedStarterKitHistoryPreventsNewWorldFromResettingTheInventory()throws Exception{
        Path p=tracking();Files.writeString(p,"{\"singleplayer\":{\"Original\":true,\"Other\":false},\"multiplayer\":{\"person\":true}}");
        WorldHopSaveCompat.carryStarterKitHistory(world,"Original","PWJ-first");WorldHopSaveCompat.carryStarterKitHistory(world,"PWJ-first","PWJ-second");var root=JsonParser.parseString(Files.readString(p)).getAsJsonObject();var single=root.getAsJsonObject("singleplayer");assertTrue(single.get("Original").getAsBoolean());assertTrue(single.get("PWJ-first").getAsBoolean());assertTrue(single.get("PWJ-second").getAsBoolean());assertFalse(single.get("Other").getAsBoolean());assertTrue(root.getAsJsonObject("multiplayer").get("person").getAsBoolean());
    }
    @Test void absentOrUnclaimedKitsAreNotGrantedAndInvalidDataFailsBeforeOpening()throws Exception{
        WorldHopSaveCompat.carryStarterKitHistory(world,"Original","PWJ-next");assertFalse(Files.exists(world.resolve("data/starterkit/tracking.json")));
        Path p=tracking();String data="{\"singleplayer\":{\"Original\":false}}";Files.writeString(p,data);WorldHopSaveCompat.carryStarterKitHistory(world,"Original","PWJ-next");assertEquals(data,Files.readString(p));Files.writeString(p,"broken json");assertThrows(IOException.class,()->WorldHopSaveCompat.carryStarterKitHistory(world,"Original","PWJ-next"));
    }
}
