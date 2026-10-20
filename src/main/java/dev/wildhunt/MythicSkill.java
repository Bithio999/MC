package dev.wildhunt;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

/** Native modes, mastery, costs, cooldowns and toggles for the new skill generation. */
public final class MythicSkill extends Skill {
    public final MythicKind kind;
    public MythicSkill(MythicKind kind){super(kind.unique()?SkillType.UNIQUE:SkillType.ULTIMATE);this.kind=kind;}
    @Override public MutableComponent getName(){return Component.translatableWithFallback("wildhunt.skill."+kind.id(),kind.title);}
    @Override public MutableComponent getColoredName(){return kind.godly()?getName().withColor(0xE8C56F):getName().withStyle(getType().getChatFormatting());}
    @Override public ResourceLocation getSkillIcon(){return WildHunt.id("textures/skill/"+kind.id()+".png");}
    @Override public MutableComponent getSkillDescription(){return Component.literal(kind.description()+(kind.godly()?GodlyPassives.DESCRIPTION:"")+(kind==MythicKind.RUNE_SMITH || kind==MythicKind.MIRRORBOUND || kind==MythicKind.VRITRA || kind==MythicKind.JANUS?" Only the strongest ordinary elemental bonus applies; Godly bonuses apply separately.":""));}
    @Override public int getMaxMastery(){return kind.godly()?50000:kind.unique()?1000:10000;}
    @Override public int getModes(ManasSkillInstance i){return kind.modes.length;}
    @Override public String getModeId(ManasSkillInstance i,int mode){return kind.id()+".mode_"+mode;}
    @Override public Component getModeName(ManasSkillInstance i,int mode){return Component.literal(kind==MythicKind.LUGH && mode==1?"Reconstructed Art — "+LughArts.selectedName(i):kind.modes[Math.clamp(mode,0,kind.modes.length-1)]);}
    @Override public int nextMode(LivingEntity e,ManasSkillInstance i,int mode,boolean reverse){return Math.floorMod(mode+(reverse?-1:1),kind.modes.length);}
    @Override public double getDefaultAcquiringMagiculeCost(){return kind.unique()?25000:0;}
    @Override public double getAuraCost(LivingEntity e,ManasSkillInstance i,int mode){return kind.godly()?0:kind==MythicKind.JANUS && mode==0 && i.getOrCreateTag().getLong("gates_until")>e.level().getGameTime()?400:cost(mode);}
    @Override public double getMagiculeCost(LivingEntity e,ManasSkillInstance i,int mode){return kind.godly()?(kind==MythicKind.LUGH && (mode==0 || mode==1) && e.isShiftKeyDown()?0:cost(mode)):0;}
    private double cost(int mode){return mode>=0 && mode<kind.costs.length?kind.costs[mode]:0;}
    @Override public boolean canBeToggled(ManasSkillInstance i,LivingEntity e){return true;}
    @Override public boolean canTick(ManasSkillInstance i,LivingEntity e){return false;}
    private boolean crucible(int mode){return kind==MythicKind.THOTH && mode==2;}
    @Override public boolean canIgnoreCoolDown(ManasSkillInstance i,LivingEntity e,int mode){return crucible(mode) || kind==MythicKind.LUGH && (mode==0 || mode==1) && e.isShiftKeyDown() || super.canIgnoreCoolDown(i,e,mode);}
    // ManasCore's explicit -1 sentinel is unlimited, with no artificial hold duration.
    @Override public int getMaxHeldTime(ManasSkillInstance i,LivingEntity e){return kind==MythicKind.THOTH?-1:super.getMaxHeldTime(i,e);}
    @Override public boolean shouldTriggerReleaseOnHeldInterrupt(ManasSkillInstance i,LivingEntity e,int key,int mode){return crucible(mode);}
    @Override public boolean onHeld(ManasSkillInstance i,LivingEntity e,int heldTicks,int mode){return crucible(mode) && ManaCrucible.held(this,i,e);}
    @Override public void onRelease(ManasSkillInstance i,LivingEntity e,int ticks,int key,int mode){if(crucible(mode))ManaCrucible.stop(i,e);}
    @Override public void onPressed(ManasSkillInstance i,LivingEntity e,int slot,int mode){
        if(e.level() instanceof ServerLevel && kind==MythicKind.LUGH && (mode==0 || mode==1) && e.isShiftKeyDown()){
            if(e.isAlive() && canInteractSkill(i,e) && canActivateSkill(i,e,mode)){
                if(mode==0 && e instanceof net.minecraft.server.level.ServerPlayer p)dev.wildhunt.myths.MythsNetwork.sendLugh(p,true);else if(mode==1)LughArts.cycle(i,e);
            }return;
        }
        if(!(e.level() instanceof ServerLevel level) || mode<0 || mode>=kind.modes.length || !crucible(mode) && i.onCoolDown(mode) || !canInteractSkill(i,e) || !canActivateSkill(i,e,mode))return;
        if(crucible(mode)){ManaCrucible.begin(i,e);return;}
        // Failed targeting and blocked gates are free, with no cooldown or mastery award.
        if(!MythicArts.canCast(this,level,i,e,mode) || EnergyHelper.isOutOfEnergy(e,i,mode))return;
        MythicArts.cast(this,level,i,e,mode);i.setCoolDown(kind.cooldowns[mode],mode);addMasteryPoint(i,e);
        i.markDirty();HunterData.of(e).putBoolean("active_skills",true);
    }
    @Override public void onToggleOn(ManasSkillInstance i,LivingEntity e){
        if(e.level().isClientSide())return;
        if(!canInteractSkill(i,e) || !canActivateSkill(i,e,0) || payUpkeep(e)){onToggleOff(i,e);return;}
        i.setToggled(true);i.getOrCreateTag().putLong("upkeep",e.level().getGameTime()+20);restore(i,e);
        HunterData.of(e).putBoolean("active_skills",true);i.markDirty();
    }
    private boolean payUpkeep(LivingEntity e){return EnergyHelper.isOutOfEnergy(e,kind.godly()?0:kind.upkeep,kind.godly()?kind.upkeep:0);}
    private ResourceLocation modifier(){return WildHunt.id(kind.id()+"_passive");}
    private void modify(LivingEntity e,Holder<Attribute> holder,double amount,boolean multiply){
        var a=e.getAttribute(holder);if(a!=null)a.addOrUpdateTransientModifier(new AttributeModifier(modifier(),amount,multiply?AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL:AttributeModifier.Operation.ADD_VALUE));
    }
    void clearLegacyDefense(LivingEntity e){
        for(var h:java.util.List.of(TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MAGIC_BARRIER,TensuraAttributes.MULTILAYER_BARRIER)){var a=e.getAttribute(h);if(a!=null)a.removeModifier(modifier());}
    }
    private void restore(ManasSkillInstance i,LivingEntity e){
        clearLegacyDefense(e);
        switch(kind){
            case THREADWEAVER->{modify(e,Attributes.MOVEMENT_SPEED,.3,true);modify(e,Attributes.ATTACK_SPEED,.25,true);}
            case RUNE_SMITH->{modify(e,TensuraAttributes.EARTH_BOOST,.5,true);modify(e,TensuraAttributes.FLAME_BOOST,.5,true);modify(e,TensuraAttributes.LIGHTNING_BOOST,.5,true);}
            case VERDANT_KEEPER->modify(e,TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION,1,true);
            case MIRRORBOUND->{modify(e,TensuraAttributes.LIGHT_BOOST,.5,true);modify(e,Attributes.ATTACK_SPEED,.25,true);modify(e,Attributes.MOVEMENT_SPEED,.2,true);}
            case VRITRA->{modify(e,TensuraAttributes.FLAME_BOOST,1,true);modify(e,TensuraAttributes.LIGHTNING_BOOST,1,true);modify(e,Attributes.ATTACK_DAMAGE,.5,true);}
            case DEMETER->{modify(e,TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION,2,true);modify(e,Attributes.KNOCKBACK_RESISTANCE,.6,false);modify(e,TensuraAttributes.WATER_BOOST,1,true);}
            case JANUS->{modify(e,Attributes.MOVEMENT_SPEED,.35,true);modify(e,Attributes.KNOCKBACK_RESISTANCE,.5,false);modify(e,TensuraAttributes.SPACE_BOOST,1,true);modify(e,Attributes.ATTACK_SPEED,.3,true);}
            case ANANSI->{modify(e,TensuraAttributes.SPACE_BOOST,1,true);modify(e,TensuraAttributes.EARTH_BOOST,1,true);modify(e,Attributes.MOVEMENT_SPEED,.3,true);}
            case AEGIR->{modify(e,TensuraAttributes.WATER_BOOST,1,true);modify(e,TensuraAttributes.LIGHT_BOOST,.75,true);modify(e,Attributes.KNOCKBACK_RESISTANCE,.5,false);}
            case LUGH->{modify(e,Attributes.ATTACK_SPEED,.5,true);modify(e,Attributes.MOVEMENT_SPEED,.35,true);modify(e,TensuraAttributes.ABILITY_MASTERY_GAIN,1,true);}
            case THOTH->{
                for(var a:java.util.List.of(TensuraAttributes.FLAME_BOOST,TensuraAttributes.WATER_BOOST,TensuraAttributes.LIGHTNING_BOOST,TensuraAttributes.EARTH_BOOST,TensuraAttributes.WIND_BOOST,TensuraAttributes.LIGHT_BOOST,TensuraAttributes.SPACE_BOOST))modify(e,a,.75,true);
                modify(e,TensuraAttributes.CHANT_SPEED,.75,true);modify(e,TensuraAttributes.MAGIC_COST_MULTIPLIER,-.25,true);
            }
        }

    }
    @Override public void onToggleOff(ManasSkillInstance i,LivingEntity e){
        i.setToggled(false);
        for(var a:attributes()){var value=e.getAttribute(a);if(value!=null)value.removeModifier(modifier());}i.markDirty();
    }
    private static java.util.List<Holder<Attribute>> attributes(){return java.util.List.of(Attributes.MOVEMENT_SPEED,Attributes.ATTACK_SPEED,Attributes.ATTACK_DAMAGE,Attributes.KNOCKBACK_RESISTANCE,
        TensuraAttributes.MAGIC_BARRIER,TensuraAttributes.PHYSICAL_BARRIER,TensuraAttributes.MULTILAYER_BARRIER,TensuraAttributes.SPIRITUAL_HEALTH_REGENERATION,
        TensuraAttributes.EARTH_BOOST,TensuraAttributes.FLAME_BOOST,TensuraAttributes.LIGHTNING_BOOST,TensuraAttributes.WATER_BOOST,TensuraAttributes.WIND_BOOST,TensuraAttributes.LIGHT_BOOST,TensuraAttributes.SPACE_BOOST,TensuraAttributes.CHANT_SPEED,TensuraAttributes.MAGIC_COST_MULTIPLIER,TensuraAttributes.ABILITY_MASTERY_GAIN);}
    public boolean tickActive(ManasSkillInstance i,LivingEntity e){
        if(!(e.level() instanceof ServerLevel level))return false;
        var tag=i.getOrCreateTag();long now=level.getGameTime();
        if(!e.isAlive() || !canInteractSkill(i,e) || tag.contains("dimension") && !tag.getString("dimension").equals(level.dimension().location().toString())){
            MythicArts.clear(i,e);if(i.isToggled())onToggleOff(i,e);return false;
        }
        if(i.isToggled() && now>=tag.getLong("upkeep")){
            if(!canActivateSkill(i,e,0) || payUpkeep(e))onToggleOff(i,e);
            else {restore(i,e);tag.putLong("upkeep",now+20);i.markDirty();}
        }
        boolean active=MythicArts.tick(this,level,i,e)|i.isToggled();
        if(kind==MythicKind.THOTH)active|=ManaCrucible.tick(this,i,e);
        if(active)i.markDirty();return active;
    }
    @Override public void onForgetSkill(ManasSkillInstance i,LivingEntity e){onToggleOff(i,e);MythicArts.clear(i,e);super.onForgetSkill(i,e);}
}
