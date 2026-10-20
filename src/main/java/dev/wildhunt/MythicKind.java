package dev.wildhunt;

/** Independent kits share native skill plumbing, never their combat behavior. */
public enum MythicKind {
    THREADWEAVER("Threadweaver", 0xED82D1, "Loom Needle", "Sever Thread", "Silken Instinct",
        new double[]{600,1000}, new int[]{4,6}, 25),
    RUNE_SMITH("Rune Smith", 0xF9BA60, "Runic Mine", "Command Detonation", "Runic Circuit",
        new double[]{600,800}, new int[]{6,4}, 30),
    VERDANT_KEEPER("Verdant Keeper", 0x8EE5AE, "Living Grove", "Briar Grasp", "Verdant Heart",
        new double[]{1600,700}, new int[]{14,5}, 35),
    MIRRORBOUND("Mirrorbound", 0xB2DDFB, "Prismatic Lance", "Mirror Aegis", "Silvered Soul",
        new double[]{750,1400}, new int[]{3,10}, 25),
    VRITRA("Vritra, Lord of Cataclysm", 0xFFAA59, "Plasma Bore", "Thunderhead Crucible", "Cataclysm Front", "Catastrophe Engine",
        new double[]{6000,15000,25000}, new int[]{5,16,26}, 120),
    DEMETER("Demeter, Lady of Sanctuary", 0xBCF4CE, "Winter Orchard", "Perennial Seed", "Winter Harvest", "Evergreen Sanctuary",
        new double[]{12000,10000,20000}, new int[]{20,24,24}, 110),
    JANUS("Janus, Lord of Crossroads", 0xCAACFF, "Twin Gates", "Parallax Snare", "Crossroads Edict", "Threshold Dominion",
        new double[]{4000,9000,16000}, new int[]{2,8,18}, 110),
    ANANSI("Anansi, Lord of Schemes", 0xD9A4FF, "Fate Stitch", "Puppet Passage", "Checkmate Loom", "Schemer's Instinct",
        new double[]{5000,3500,22000}, new int[]{2,5,24}, 100),
    AEGIR("Aegir, Lord of the Abyss", 0x65E4EC, "Refracted Harpoon", "Mirrorsea", "Sovereign Breaker", "Abyssal Majesty",
        new double[]{7000,12000,20000}, new int[]{5,16,24}, 110),
    LUGH("Lugh, God of Skills", 0xF4E7A1, "Skill Appraisal", "Reconstructed Art", "Countermand", "Grand Synthesis", "All-Skilled",
        new double[]{1200,10000,9000,30000}, new int[]{2,4,18,40}, 240),
    THOTH("Thoth, God of Magic", 0x92E9F7, "Lexicon of Ruin", "Spellbreak", "Mana Crucible", "Astral Library", "Arcane Sovereignty",
        new double[]{12000,6500,0,24000}, new int[]{12,15,0,35}, 180);

    public final String title, passive;
    public final String[] modes;
    public final int color;
    public final double[] costs;
    public final int[] cooldowns;
    public final double upkeep;
    MythicKind(String title,int color,String a,String b,String passive,double[] costs,int[] cooldowns,double upkeep) {
        this(title,color,new String[]{a,b},passive,costs,cooldowns,upkeep);
    }
    MythicKind(String title,int color,String a,String b,String c,String passive,double[] costs,int[] cooldowns,double upkeep) {
        this(title,color,new String[]{a,b,c},passive,costs,cooldowns,upkeep);
    }
    MythicKind(String title,int color,String a,String b,String c,String d,String passive,double[] costs,int[] cooldowns,double upkeep) {
        this(title,color,new String[]{a,b,c,d},passive,costs,cooldowns,upkeep);
    }
    MythicKind(String title,int color,String[] modes,String passive,double[] costs,int[] cooldowns,double upkeep) {
        this.title=title;this.color=color;this.modes=modes;this.passive=passive;this.costs=costs;this.cooldowns=cooldowns;this.upkeep=upkeep;
    }
    public String id(){return name().toLowerCase(java.util.Locale.ROOT);}
    public boolean unique(){return ordinal()<VRITRA.ordinal();}
    public boolean godly(){return this==THOTH || this==LUGH;}
    public String description(){return switch(this){
        case THREADWEAVER -> "Pin a visible enemy to a six-second silk tether, then sever it for a spatial burst. The tether pulls only after the enemy leaves its three-block leash. Silken Instinct improves movement and attack speed.";
        case RUNE_SMITH -> "Inscribe one remotely armed rune. It erupts when an enemy approaches, or use Command Detonation for a stronger, wider blast. Runic Circuit improves elemental spell power. No passive stacks.";
        case VERDANT_KEEPER -> "Grow an eight-second healing grove for yourself, owned pets and allies. Briar Grasp lashes and webs enemies in a forward fan. Verdant Heart strengthens natural SHP recovery. Healing never benefits hostile strangers.";
        case MIRRORBOUND -> "Split a light lance across three visible enemies. Mirror Aegis turns approaching hostile projectiles back toward their sender for six seconds. Silvered Soul improves light power, attack speed and movement.";
        case VRITRA -> "Storm Crown + Black Flame. Plasma Bore leaves delayed fire fuses and refracts through your thunderhead for a second firing angle. Thunderhead Crucible calls eight seconds of lightning/fire strikes. Cataclysm Front consumes the storm for wider, stronger eruptions. Native resistance penetration; no EP damage scaling.";
        case DEMETER -> "Verdant Keeper + Frostbound Colossus. Winter Orchard restores allied HP/SHP and freezes approaches. Perennial Seed plants one-use rescue blooms on allies: when wounded below 35% HP, restore 40% HP/SHP and cleanse selected afflictions. Winter Harvest consumes the orchard into six warned crystal blooms and restores allies. No passive barrier.";
        case JANUS -> "Threadweaver + Mirrorbound. Link safe gates and recast to traverse them without losing momentum. Parallax Snare also opens at your opposite gate when cast near an endpoint. Crossroads Edict mirrors enemies across a field, then closes its delayed spatial cut. Native resistance penetration; no EP damage scaling.";
        case ANANSI -> "Threadweaver + Rune Smith. Place up to three armed anchors; their linking threads cut enemies that cross them. Puppet Passage traverses to the aimed anchor, moving that anchor to your former position. Checkmate Loom consumes the network into a warned triangular spatial collapse and delayed earth detonation. No passive stacks.";
        case AEGIR -> "Scarlet Deluge + Mirrorbound. Refracted Harpoon fires a water lance; Mirrorsea adds two angled firing origins and redirects incoming hostile projectiles. Sovereign Breaker launches an expanding water shock front, consuming Mirrorsea to originate there with greater reach and force.";
        case LUGH -> "[Godly] Appraise an enemy's recent damaging technique into a three-entry archive. Crouch + Reconstructed Art cycles its selected entry for free; press normally to reconstruct its damage type and attack form. Countermand seals a target's recent non-Godly technique for six seconds. Grand Synthesis weaves all three archived forms into a warned sequence. Three origin techniques are available immediately. All-Skilled strengthens attack speed, movement and native mastery gain. Magicules only; no EP damage scaling.";
        case THOTH -> "[Godly] Unleash a seven-chapter Fire, Water, Lightning, Earth, Wind, Light and Space spell sequence. Spellbreak disrupts non-Godly enemies and recovers part of drained magicules. Hold Mana Crucible to convert two aura into one magicule at 5% of maximum magicules per second, with no cast cost, cooldown or total cap. Astral Library follows you with orbiting books and autonomous seven-element spell fire. Arcane Sovereignty strengthens elemental magic, chanting and native magic efficiency. Offensive arts cost magicules; no EP damage scaling.";
    };}
}
