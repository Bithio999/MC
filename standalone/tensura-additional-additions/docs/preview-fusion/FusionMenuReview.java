package dev.heavenlyrestriction;
import dev.heavenlyrestriction.fusion.Fusion;
import dev.heavenlyrestriction.client.FusionClient;
import io.github.manasmods.manascore.skill.api.*;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.client.screen.*;
import io.github.manasmods.tensura.registry.skill.UniqueSkills;
import io.github.manasmods.tensura.registry.battlewill.MeleeArts;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;
/** Actual Tensura screen, list clicks and slot packets; no custom selection UI or forced slot assignment. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class FusionMenuReview {
    private static final Path OUT=Path.of("/workspace/validation-fusion/menu-review");private static volatile int phase;private static int role,age,frames;private static volatile String error;private static LivingEntity partner;private static double hp,sp;
    private static final ManasSkill unique(){return UniqueSkills.MARTIAL_MASTER.get();}
    private static void check(boolean ok,String why){if(!ok){error=why;throw new IllegalStateException("FUSION_MENU_REVIEW: "+why);}}
    private static void shot(String name){try{Files.createDirectories(OUT);try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(OUT.resolve(name+".png"));}}catch(Exception e){throw new RuntimeException(e);}}
    private static class Menu extends AbilitySelectionScreen {
        Menu(boolean battlewill){super(battlewill?AbilityCategoriesScreen.AbilityType.BATTLEWILL:AbilityCategoriesScreen.AbilityType.SKILL,battlewill?null:Skill.SkillType.UNIQUE,null);}
        void search(ManasSkill skill){searchField.setValue(skill.getName().getString());slotsDropdown=true;}
        void clickSkill(ManasSkill skill){int index=-1;for(int n=0;n<filtered.size();n++)if(filtered.get(n).getSkill()==skill){index=n;break;}check(index>=0,"Borrowed skill absent from native filtered list: "+skill.getName().getString());
            mouseClicked(guiLeft+12,guiTop+46+13*(index-listStartIndex)+5,0);check(selectedAbility!=null && selectedAbility.getSkill()==skill && selectedAbility.isTemporarySkill(),"Native list click did not select temporary skill");}
        void slot(int index){slotsDropdown=true;mouseClicked(guiLeft+20,guiTop+164+16*index+7,2);check(mouseClicked(guiLeft+20,guiTop+164+16*index+7,0),"Native slot click rejected");}
    }
    private static void setup(ServerPlayer p){p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);for(var i:List.copyOf(SkillAPI.getSkillsFrom(p).getLearnedSkills()))SkillAPI.getSkillsFrom(p).forgetSkill(i);SkillAPI.getSkillsFrom(p).learnSkill(HeavenlyRestriction.FUSION.get().createDefaultInstance());p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);p.setHealth(20);p.getAttribute(TensuraAttributes.MAX_SPIRITUAL_HEALTH).setBaseValue(100);TensuraStorages.getExistenceFrom(p).setSpiritualHealth(50);}
    private static void gifts(LivingEntity owner){SkillAPI.getSkillsFrom(owner).updateSkill(unique().createDefaultInstance(),true);SkillAPI.getSkillsFrom(owner).updateSkill(MeleeArts.AURA_SLASH.get().createDefaultInstance(),true);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){if(!Boolean.getBoolean("fusion.review.menu"))return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;if(++age>3600 || error!=null){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("failure.txt"),error==null?"Timeout phase "+phase:error);}catch(Exception ignored){}mc.stop();return;}var server=mc.getSingleplayerServer();if(server==null || mc.player==null || mc.getOverlay()!=null)return;
        if(phase==0 && mc.player.tickCount>60){phase=1;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();check(server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName().toString().equals("Fusion Preview"),"Not disposable review world");setup(p);
            var mob=new IronGolem(EntityType.IRON_GOLEM,p.level());mob.setPos(p.position().add(3,0,0));mob.setNoAi(true);p.serverLevel().addFreshEntity(mob);TensuraStorages.getExistenceFrom(mob).setPermanentOwner(p.getUUID());gifts(mob);partner=mob;});return;}
        if(phase==1 && ++frames>30){phase=10;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();hp=p.getMaxHealth()+partner.getMaxHealth();sp=p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)+partner.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);check(Fusion.fuseSubordinate(p,partner),"NPC fusion failed");check(Math.abs(p.getMaxHealth()-hp)<.02 && Math.abs(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)-sp)<.02,"Live NPC combined vitals wrong");phase=2;});frames=0;return;}
        if(phase==2 && ++frames>30){var menu=new Menu(false);mc.setScreen(menu);menu.search(unique());phase=3;frames=0;return;}
        if(phase==3 && ++frames>25 && mc.screen instanceof Menu menu){menu.clickSkill(unique());shot((role==0?"subordinate":"player")+"-unique-menu");menu.slot(role==0?2:0);mc.setScreen(null);phase=4;frames=0;return;}
        if(phase==4 && ++frames>20){phase=40;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();var slots=TensuraStorages.getAbilityFrom(p);check(slots.getAbilitySlot(slots.getActivePreset(),role==0?2:0).getSkill()==unique(),"Server did not accept native borrowed-skill slot packet");phase=5;});return;}
        if(phase==5){var menu=new Menu(true);mc.setScreen(menu);menu.search(MeleeArts.AURA_SLASH.get());phase=6;frames=0;return;}
        if(phase==6 && ++frames>25 && mc.screen instanceof Menu menu){menu.clickSkill(MeleeArts.AURA_SLASH.get());shot((role==0?"subordinate":"player")+"-battlewill-menu");mc.setScreen(null);phase=60;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();Fusion.end(p,"Menu review complete.");phase=7;});frames=0;return;}
        if(phase==7 && ++frames>20){check(SkillAPI.getSkillsFrom(mc.player).getSkill(unique()).isEmpty(),"Borrowed skill survived separation on client");if(role==1){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("passed.txt"),"PASS: NPC and consenting-player control roles; combined HP/SHP; native Skills → Unique and Battlewills lists; actual list clicks, slot packets and server assignment; borrowed entries removed afterward. Player donor was a server-side test player.\n");}catch(Exception ex){throw new RuntimeException(ex);}mc.stop();return;}
            role=1;phase=8;frames=0;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();p.removeAllEffects();var guest=net.neoforged.neoforge.common.util.FakePlayerFactory.get(p.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Fusion Donor"));guest.setPos(p.position().add(2,0,0));guest.setGameMode(GameType.SURVIVAL);SkillAPI.getSkillsFrom(guest).learnSkill(HeavenlyRestriction.FUSION.get().createDefaultInstance());gifts(guest);partner=guest;hp=p.getMaxHealth()+guest.getMaxHealth();sp=p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)+guest.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);check(Fusion.invite(guest,p)!=null,"Player invite failed");});return;}
        if(phase==8 && mc.screen instanceof FusionClient.Consent && ++frames>20){((Button)mc.screen.children().stream().filter(c->c instanceof Button b && b.getMessage().getString().equals("Accept fusion")).findFirst().orElseThrow()).onPress();phase=9;frames=0;return;}
        if(phase==9 && ++frames>35){phase=90;server.execute(()->{var p=server.getPlayerList().getPlayers().getFirst();check(Fusion.body(p) && !p.isSpectator(),"Receiving player did not control fusion");check(Math.abs(p.getMaxHealth()-hp)<.02 && Math.abs(p.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH)-sp)<.02,"Live player combined vitals wrong");phase=2;});frames=0;}
    }
}
