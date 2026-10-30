package dev.wildhunt.guide;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
public class GuideCatalogTest {
    private GuideCatalog load() throws Exception {try(var in=getClass().getResourceAsStream("/assets/wildhunt/guide/entries.json")){assertNotNull(in);return GuideCatalog.parse(new String(in.readAllBytes(),StandardCharsets.UTF_8));}}
    @Test void bundledPagesHaveValidNavigationAndGameplaySections() throws Exception {var c=load();assertFalse(c.entries().isEmpty());for(var e:c.entries()){assertFalse(e.sections().isEmpty());for(var id:e.related())assertEquals(id,c.get(id).id());}}
    @Test void exactTitleRanksFirstAndSearchFindsAbilityText() throws Exception {var c=load();assertEquals("schema",c.search("Schema","All").getFirst().id());assertTrue(c.search("Counter","Skills").stream().anyMatch(e->e.id().equals("unbound")));assertTrue(c.search("30 seconds","All").stream().anyMatch(e->e.id().equals("schema")));}
    @Test void categoriesAndAllTokensAreAppliedTogether() throws Exception {var c=load();assertTrue(c.search("schema","Gear").isEmpty());assertTrue(c.search("no_such_entry","All").isEmpty());assertTrue(c.search("aura regeneration","Skills").stream().allMatch(e->e.category().equals("Skills")));}
    @Test void normalizationHandlesCaseAccentsAndSpaces(){assertEquals("eschaton",GuideCatalog.normalize("  ÉSCHATON  "));}
    @Test void shippedLineageUsesTheCurrentFixedGates() throws Exception {var c=load();String text=c.get("fettered-lineage").sections().toString();assertTrue(text.contains("100,000"));assertTrue(text.contains("8,000,000"));assertFalse(text.contains("Tempered Vessel: 50,000"));}
    @Test void invalidAndDanglingCatalogsAreRejected(){assertThrows(RuntimeException.class,()->GuideCatalog.parse("{\"entries\":[]}"));assertThrows(RuntimeException.class,()->GuideCatalog.parse("{\"entries\":[{\"id\":\"bad\",\"title\":\"Bad\",\"category\":\"Start\",\"summary\":\"Bad\",\"sections\":[],\"related\":[\"missing\"]}]}"));}
    @Test void emptySearchShowsCoreTopicsBeforeAdvancedCombinations() throws Exception {var c=load();assertEquals("welcome",c.search("","All").getFirst().id());assertEquals("fettered-lineage",c.search("","Races").getFirst().id());}
}
