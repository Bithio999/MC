package dev.heavenlyrestriction;
import dev.heavenlyrestriction.client.guide.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
/** Opt-in read-only GUI review. Does not grant skills or edit player stats. */
@EventBusSubscriber(modid=HeavenlyRestriction.ID,value=Dist.CLIENT)
public final class GuideClientReview {
    private static int phase,frames,age;private static Screen parent;
    private static void check(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    private static Object field(GuideScreen screen,String name){try{var f=GuideScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(screen);}catch(Exception e){throw new IllegalStateException(e);}}
    private static int number(GuideScreen s,String n){return (Integer)field(s,n);}
    private static void press(Screen screen,String text){var b=screen.children().stream().filter(c->c instanceof Button button && button.visible && button.active && button.getMessage()!=null && button.getMessage().getString().equals(text)).map(c->(Button)c).findFirst().orElseThrow(()->new IllegalStateException("Missing button "+text));b.onPress();}
    private static void search(GuideScreen s,String text){((EditBox)field(s,"search")).setValue(text);}
    private static void shot(Minecraft mc,String name){try(var pic=Screenshot.takeScreenshot(mc.getMainRenderTarget())){Files.createDirectories(Path.of("guide-review"));pic.writeToFile(Path.of("guide-review",name+".png"));}catch(Exception e){throw new IllegalStateException(e);}}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var mc=Minecraft.getInstance();if(mc.getOverlay()!=null)return;if(++age>2000){mc.stop();throw new IllegalStateException("Guide review timeout "+phase);}
        if(phase==0){if(mc.screen==null && mc.player==null)return;if(mc.player==null && age<60)return;if(mc.player!=null && mc.player.tickCount<100)return;
            parent=mc.screen;if(mc.player!=null)KeyMapping.click(GuideClient.KEY.getKey());else GuideClient.open(parent);phase=1;frames=0;return;}
        if(!(mc.screen instanceof GuideScreen s)){if(phase==10){mc.stop();return;}return;}
        if(++frames<15)return;frames=0;
        if(phase==1){check(((dev.heavenlyrestriction.guide.GuideCatalog)field(s,"catalog")).entries().size()==9,"Wrong catalog");shot(mc,"home");search(s,"schema");press(s,"Schema");check(field(s,"selected").equals("schema"),"Search/selection failed");phase=2;}
        else if(phase==2){shot(mc,"schema");search(s,"counter");for(int n=0;n<60 && s.children().stream().noneMatch(c->c instanceof Button b && b.visible && b.getMessage().getString().equals("Unbound"));n++)s.mouseScrolled(number(s,"x")+20,number(s,"listTop")+20,0,-1);press(s,"Unbound");check(field(s,"selected").equals("unbound"),"Body search failed");phase=3;}
        else if(phase==3){s.mouseScrolled(number(s,"articleX")+20,number(s,"articleY")+20,0,-5);check(number(s,"pageScroll")>0,"Article did not scroll");press(s,"<");press(s,">");check(field(s,"selected").equals("unbound"),"History navigation failed");search(s,"zz_no_matches");check(((java.util.List<?>)field(s,"results")).isEmpty(),"Empty state failed");search(s,"");press(s,"Races");check(((java.util.List<dev.heavenlyrestriction.guide.GuideCatalog.Entry>)field(s,"results")).stream().allMatch(r->r.category().equals("Races")),"Category filter failed");phase=4;}
        else if(phase==4){press(s,"All");search(s,"schema");press(s,"Schema");mc.options.guiScale().set(2);GLFW.glfwSetWindowSize(mc.getWindow().getWindow(),640,480);phase=5;}
        else if(phase==5){check(s.width>=300 && s.height>=220,"Unexpected small viewport");shot(mc,"small-schema");check(number(s,"x")>=0 && number(s,"y")>=0 && number(s,"y")+number(s,"h")<=s.height,"Small layout escaped screen");s.mouseClicked(number(s,"x")+number(s,"w")-12,number(s,"articleBottom")-2,0);s.mouseReleased(0,0,0);check(number(s,"pageScroll")>0,"Scrollbar drag failed");phase=6;}
        else if(phase==6){shot(mc,"small-scrolled");s.onClose();check(mc.screen==parent,"Close did not restore parent");if(mc.player!=null){mc.setScreen(new PauseScreen(true));press(mc.screen,"Additions Guide");phase=7;}else finish(mc);}
        else if(phase==7){s.onClose();check(mc.screen instanceof PauseScreen,"Pause parent lost");mc.setScreen(new io.github.manasmods.tensura.client.screen.MainScreen());press(mc.screen,"Guide");phase=8;}
        else if(phase==8){s.onClose();check(mc.screen instanceof io.github.manasmods.tensura.client.screen.MainScreen,"Status parent lost");finish(mc);}
    }
    private static void finish(Minecraft mc){try{Files.createDirectories(Path.of("guide-review"));Files.writeString(Path.of("guide-review/passed.txt"),"Guide catalog, search/title/body/category/empty state, page selection, history, scrolling/drag, small viewport, parent restoration passed. World review also checks F8, pause and native status buttons.\n");}catch(Exception e){throw new IllegalStateException(e);}phase=10;mc.stop();}
}
