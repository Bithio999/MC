package dev.wildhunt;

/** The eight new kits share native skill plumbing, never their combat behavior. */
public enum MythicKind {
    THREADWEAVER("Threadweaver", 0xED82D1, "Loom Needle", "Sever Thread", "Silken Instinct",
        new double[]{220,500}, new int[]{4,6}, 15),
    RUNE_SMITH("Rune Smith", 0xF9BA60, "Runic Mine", "Command Detonation", "Runic Circuit",
        new double[]{350,400}, new int[]{6,4}, 20),
    VERDANT_KEEPER("Verdant Keeper", 0x8EE5AE, "Living Grove", "Briar Grasp", "Verdant Heart",
        new double[]{700,300}, new int[]{14,5}, 20),
    MIRRORBOUND("Mirrorbound", 0xB2DDFB, "Prismatic Lance", "Mirror Aegis", "Silvered Soul",
        new double[]{280,600}, new int[]{3,10}, 15),
    VRITRA("Vritra, Lord of Cataclysm", 0xFFAA59, "Plasma Bore", "Thunderhead Crucible", "Cataclysm Front", "Catastrophe Engine",
        new double[]{1400,3600,6000}, new int[]{4,15,24}, 80),
    DEMETER("Demeter, Lady of Sanctuary", 0xBCF4CE, "Winter Orchard", "Crystal Bulwark", "Winter Harvest", "Evergreen Sanctuary",
        new double[]{2200,1800,4000}, new int[]{18,16,20}, 70),
    JANUS("Janus, Lord of Crossroads", 0xCAACFF, "Twin Gates", "Parallax Snare", "Crossroads Edict", "Threshold Dominion",
        new double[]{1600,2200,4500}, new int[]{3,7,18}, 70),
    THOTH("Thoth, God of Magic", 0x92E9F7, "Lexicon of Ruin", "Spellbreak", "Mana Crucible", "Astral Library", "Arcane Sovereignty",
        new double[]{5000,4000,1000,9000}, new int[]{12,15,20,35}, 180);

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
    public boolean godly(){return this==THOTH;}
    public String description(){return switch(this){
        case THREADWEAVER -> "Pin a visible enemy to a six-second silk tether, then sever it for a spatial burst. The tether pulls only after the enemy leaves its three-block leash. Silken Instinct improves movement and attack speed.";
        case RUNE_SMITH -> "Inscribe one remotely armed rune. It erupts when an enemy approaches, or use Command Detonation for a stronger, wider blast. Runic Circuit improves elemental spell power. No passive stacks.";
        case VERDANT_KEEPER -> "Grow an eight-second healing grove for yourself, owned pets and allies. Briar Grasp lashes and webs enemies in a forward fan. Verdant Heart strengthens natural SHP recovery. Healing never benefits hostile strangers.";
        case MIRRORBOUND -> "Split a light lance across three visible enemies. Mirror Aegis turns approaching hostile projectiles back toward their sender for six seconds. Silvered Soul improves light power, attack speed and movement.";
        case VRITRA -> "Storm Crown + Black Flame. Bore a lightning lance through enemies and leave delayed fire detonations. Forge a fiery thunderhead, or send a telegraphed cataclysm front forward. Catastrophe Engine strengthens lightning, flame and melee.";
        case DEMETER -> "Verdant Keeper + Frostbound Colossus. Winter Orchard restores allied HP/SHP and chills enemies. Crystal Bulwark lends native physical, magic and HUD barriers to allies. Winter Harvest consumes the orchard into six dodgeable crystal blooms. Evergreen Sanctuary strengthens natural SHP recovery and knockback resistance.";
        case JANUS -> "Threadweaver + Mirrorbound. Link two safe gates and recast to traverse them without losing momentum. Parallax Snare closes opposing spatial blades around a marked location. Crossroads Edict safely displaces enemies across the field. Threshold Dominion strengthens spatial damage, attack speed and agility.";
        case THOTH -> "[Godly] Unleash a three-chapter fire, ice and lightning spell sequence. Spellbreak disrupts non-Godly enemies and recovers part of drained magicules. Mana Crucible converts two aura into one missing magicule. Astral Library follows you with orbiting books and autonomous spell fire. Arcane Sovereignty strengthens elemental magic, chanting, native magic efficiency. All arts cost magicules; no EP damage scaling.";
    };}
}
