package dev.heavenlyrestriction.client;
import dev.heavenlyrestriction.*;
import dev.heavenlyrestriction.fusion.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class FusionClient {
    private record Body(CompoundTag data,long received){}
    private static final Map<UUID,Body> BODIES=new HashMap<>();private static final Map<LivingEntity,Boolean> PHYSICS=new IdentityHashMap<>();private static boolean rendering,sentExit;private static Object lastLevel;
    @EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static class Setup {@SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{FusionNetwork.receiver=FusionClient::receive;Fusion.clientHidden=e2->BODIES.values().stream().anyMatch(b->b.data.getInt("hidden")==e2.getId());});}}
    public static void receive(CompoundTag tag){var mc=Minecraft.getInstance();if(tag==null)return;if(lastLevel!=mc.level){BODIES.clear();lastLevel=mc.level;}
        if(tag.getBoolean("invite")){mc.setScreen(new Consent(mc.screen,tag));return;}
        if(!tag.hasUUID("token"))return;if(tag.getBoolean("ended")){BODIES.remove(tag.getUUID("token"));sentExit=false;}else if(mc.level!=null)BODIES.put(tag.getUUID("token"),new Body(tag.copy(),mc.level.getGameTime()));syncPhysics();
    }
    private static void syncPhysics(){var mc=Minecraft.getInstance();
        var it=PHYSICS.entrySet().iterator();while(it.hasNext()){var entry=it.next();if(mc.level!=entry.getKey().level() || !Fusion.hidden(entry.getKey())){entry.getKey().noPhysics=entry.getValue();it.remove();}}
        if(mc.level!=null)for(var body:BODIES.values())if(mc.level.getEntity(body.data.getInt("hidden")) instanceof LivingEntity hidden){PHYSICS.putIfAbsent(hidden,hidden.noPhysics);hidden.noPhysics=true;}
    }
    @SubscribeEvent public static void beforeTick(ClientTickEvent.Pre e){var mc=Minecraft.getInstance();if(mc.level!=lastLevel){BODIES.clear();lastLevel=mc.level;sentExit=false;}syncPhysics();}
    private static CompoundTag local(){var mc=Minecraft.getInstance();if(mc.player==null)return null;for(var b:BODIES.values())if(b.data.getInt("pilot")==mc.player.getId() || b.data.getInt("caster")==mc.player.getId())return b.data;return null;}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){var mc=Minecraft.getInstance();if(mc.level!=lastLevel){BODIES.clear();lastLevel=mc.level;sentExit=false;}if(mc.level==null)return;
        BODIES.values().removeIf(b->mc.level.getGameTime()-b.received>b.data.getInt("ticks")+40);var state=local();
        if(state!=null && !state.getBoolean("subordinate") && mc.player.getId()==state.getInt("caster") && mc.screen==null && mc.options.keyShift.isDown() && !sentExit){sentExit=true;PacketDistributor.sendToServer(new FusionNetwork.End());}
    }
    @SubscribeEvent public static void hidden(RenderLivingEvent.Pre<?,?> e){if(rendering)return;for(var b:BODIES.values())if(b.data.getInt("hidden")==e.getEntity().getId()){e.setCanceled(true);return;}}
    @SubscribeEvent public static void form(RenderPlayerEvent.Pre e){if(rendering)return;var mc=Minecraft.getInstance();for(var b:BODIES.values()){
        var t=b.data;if(!t.getBoolean("subordinate") || e.getEntity().getId()!=t.getInt("pilot"))continue;
        if(mc.level==null || !(mc.level.getEntity(t.getInt("hidden")) instanceof LivingEntity host))return;
        e.setCanceled(true);var player=e.getEntity();boolean invisible=host.isInvisible();rendering=true;
        try{host.setInvisible(false);host.yBodyRot=player.yBodyRot;host.yBodyRotO=player.yBodyRotO;host.yHeadRot=player.yHeadRot;host.yHeadRotO=player.yHeadRotO;host.setYRot(player.getYRot());host.setXRot(player.getXRot());host.walkAnimation.update(player.walkAnimation.speed(),1);host.hurtTime=player.hurtTime;host.attackAnim=player.attackAnim;
            var renderer=mc.getEntityRenderDispatcher().getRenderer(host);renderer.render(host,player.getYRot(),e.getPartialTick(),e.getPoseStack(),e.getMultiBufferSource(),e.getPackedLight());
        }finally{rendering=false;host.setInvisible(invisible);}return;
    }}
    @SubscribeEvent public static void pause(ScreenEvent.Init.Post e){if(e.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen && local()!=null)e.addListener(Button.builder(Component.literal("Separate Fusion"),b->{PacketDistributor.sendToServer(new FusionNetwork.End());Minecraft.getInstance().setScreen(null);}).bounds(8,32,112,20).build());}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post e){var mc=Minecraft.getInstance();var t=local();if(t==null || mc.options.hideGui)return;var g=e.getGuiGraphics();int x=mc.getWindow().getGuiScaledWidth()/2-135,y=12;
        var body=BODIES.get(t.getUUID("token"));int sec=Math.max(0,(t.getInt("ticks")-(int)(mc.level.getGameTime()-body.received)+19)/20);
        g.fill(x,y,x+270,y+45,0xe80c1726);g.fill(x,y,x+3,y+45,0xff65e7ce);g.fill(x+3,y+43,x+270,y+45,0xffd9b56c);
        g.drawString(mc.font,"FUSION  /  "+sec/60+":"+String.format(Locale.ROOT,"%02d",sec%60),x+10,y+6,0xfff5dfa6,false);
        g.drawString(mc.font,"EP "+String.format(Locale.ROOT,"%,.0f",t.getDouble("ep"))+"  |  Regen ×10  |  Damage ×2",x+10,y+18,0xff9df1e0,false);
        g.drawString(mc.font,t.getBoolean("subordinate") || mc.player.getId()==t.getInt("pilot")?"Press Fuse again to separate":"Receiving player controls  •  Sneak to separate",x+10,y+31,0xffbccad5,false);
    }
    public static final class Consent extends Screen {
        private final Screen parent;private final CompoundTag invite;private int age;private boolean replied;
        Consent(Screen parent,CompoundTag invite){super(Component.literal("Fusion invitation"));this.parent=parent;this.invite=invite;}
        @Override public boolean isPauseScreen(){return false;}
        @Override protected void init(){int x=width/2-160,y=height/2-95;
            addRenderableWidget(Button.builder(Component.literal("Accept fusion"),b->reply(true)).bounds(x+12,y+164,145,20).build());
            addRenderableWidget(Button.builder(Component.literal("Decline"),b->reply(false)).bounds(x+163,y+164,145,20).build());
        }
        private void reply(boolean accept){if(!replied){replied=true;PacketDistributor.sendToServer(new FusionNetwork.Reply(invite.getUUID("nonce"),accept));}Minecraft.getInstance().setScreen(parent);}
        @Override public void onClose(){reply(false);}
        @Override public void tick(){if(++age>=400)reply(false);}
        @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g,mx,my,partial);int x=width/2-160,y=height/2-95;
            g.fill(x-1,y-1,x+321,y+191,0xffd5b46e);g.fill(x,y,x+320,y+190,0xff0e1b2a);g.fill(x,y,x+320,y+3,0xff66e5ce);
            g.drawCenteredString(font,"FUSION INVITATION",width/2,y+12,0xfff4dda3);g.drawCenteredString(font,invite.getString("name")+" wants to fuse with you.",width/2,y+30,0xfff3f6f4);
            String[] lines={"You control the fused body for 3 minutes.","Combined EP ×1.5  •  Regeneration ×10  •  Damage ×2", "Temporarily receive their skills.","After separation: Paralysis II + Fragility II for 5 minutes.","Either player can end Fusion early."};int lineY=y+49;
            for(String line:lines){for(var wrapped:font.split(Component.literal(line),296)){g.drawString(font,wrapped,width/2-font.width(wrapped)/2,lineY,0xffb9d5dc,false);lineY+=11;}lineY+=3;}for(var widget:renderables)widget.render(g,mx,my,partial);
        }
    }
}
