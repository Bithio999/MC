package dev.heavenlyrestriction.guide;

import com.google.gson.*;
import java.text.Normalizer;
import java.util.*;

/** Small, immutable offline reference. Resource packs can replace the same JSON file. */
public record GuideCatalog(List<Entry> entries) {
    public record Section(String title,List<String> lines){}
    public record Entry(String id,String title,String category,String summary,String icon,List<String> keywords,List<Section> sections,List<String> related){
        String searchable(){return normalize(title+" "+summary+" "+String.join(" ",keywords)+" "+sections.stream().map(s->s.title+" "+String.join(" ",s.lines)).reduce("",(a,b)->a+" "+b));}
    }
    public static String normalize(String text){return Normalizer.normalize(text,Normalizer.Form.NFKD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).strip();}
    public Entry get(String id){return entries.stream().filter(e->e.id.equals(id)).findFirst().orElse(entries.getFirst());}
    public List<String> categories(){return java.util.stream.Stream.of("Start","Skills","Races","Gear","People","Bosses").filter(c->entries.stream().anyMatch(e->e.category.equals(c))).toList();}
    public List<Entry> search(String query,String category){
        var tokens=normalize(query).split("\\s+");
        return entries.stream().filter(e->category.equals("All") || e.category.equals(category))
            .filter(e->Arrays.stream(tokens).allMatch(e.searchable()::contains))
            .sorted(query.isBlank()?Comparator.comparingInt(entries::indexOf):Comparator.<Entry>comparingInt(e->-score(e,normalize(query))).thenComparing(e->e.id.equals("welcome")?"":e.title,String.CASE_INSENSITIVE_ORDER)).toList();
    }
    private static int score(Entry e,String q){if(q.isEmpty())return 0;String t=normalize(e.title);return t.equals(q)?100:t.startsWith(q)?50:t.contains(q)?30:e.keywords.stream().anyMatch(k->normalize(k).contains(q))?10:0;}
    public static GuideCatalog parse(String json){
        var array=JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("entries");
        if(array==null || array.isEmpty() || array.size()>300)throw new IllegalArgumentException("Invalid guide size");
        var all=new ArrayList<Entry>();var ids=new HashSet<String>();
        for(var value:array){var o=value.getAsJsonObject();String id=text(o,"id"),title=text(o,"title"),cat=text(o,"category");
            if(!id.matches("[a-z0-9_/-]+") || !ids.add(id) || !Set.of("Start","Skills","Races","Gear","People","Bosses").contains(cat))throw new IllegalArgumentException("Invalid guide entry "+id);
            var sections=new ArrayList<Section>();for(var raw:o.getAsJsonArray("sections")){var s=raw.getAsJsonObject();sections.add(new Section(text(s,"title"),strings(s,"lines")));}
            all.add(new Entry(id,title,cat,text(o,"summary"),o.has("icon")?o.get("icon").getAsString():"minecraft:textures/item/book.png",strings(o,"keywords"),List.copyOf(sections),strings(o,"related")));
        }
        for(var e:all)for(String id:e.related)if(!ids.contains(id))throw new IllegalArgumentException("Missing related page "+id);
        return new GuideCatalog(List.copyOf(all));
    }
    private static String text(JsonObject o,String key){String s=o.get(key).getAsString().strip();if(s.isEmpty() || s.length()>4000)throw new IllegalArgumentException("Invalid "+key);return s;}
    private static List<String> strings(JsonObject o,String key){if(!o.has(key))return List.of();var result=new ArrayList<String>();for(var s:o.getAsJsonArray(key)){String v=s.getAsString();if(v.length()>4000)throw new IllegalArgumentException("Guide text too long");result.add(v);}return List.copyOf(result);}
}
