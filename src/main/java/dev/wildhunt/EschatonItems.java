package dev.wildhunt;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

public final class EschatonItems {
    private EschatonItems(){}
    private static final net.minecraft.resources.ResourceLocation CROWN=WildHunt.id("mythbreakers_legacy");
    private record Arrival(UUID id,Vec3 position,long expires){}
    // Bridge native section-index registration for simultaneous ritual packets.
    // Values contain no level/entity references; stale arrivals expire after ten ticks.
    private static final Map<ServerLevel,List<Arrival>> ARRIVALS=new WeakHashMap<>();
    static void released(ServerLevel level,UUID id){var arrivals=ARRIVALS.get(level);if(arrivals!=null)arrivals.removeIf(a->a.id().equals(id));}
    private static boolean arriving(ServerLevel level,Vec3 at){var arrivals=ARRIVALS.get(level);if(arrivals==null)return false;arrivals.removeIf(a->a.expires()<level.getGameTime());return arrivals.stream().anyMatch(a->a.position().distanceToSqr(at)<=256*256);}
    public static EschatonEntity summon(ServerLevel l,Vec3 at){
        var b=EschatonContent.ESCHATON.get().create(l);if(b==null)return null;b.setPos(at);
        if(l.getDifficulty()==Difficulty.PEACEFUL || !l.hasChunkAt(b.blockPosition()) || !l.getWorldBorder().isWithinBounds(b.getBoundingBox()) || b.getBoundingBox().maxY>=l.getMaxBuildHeight() || !l.noCollision(b,b.getBoundingBox()) || l.containsAnyLiquid(b.getBoundingBox()))return null;
        b.finalizeSpawn(l,l.getCurrentDifficultyAt(b.blockPosition()),MobSpawnType.MOB_SUMMONED,null);if(!l.addFreshEntity(b))return null;
        ARRIVALS.computeIfAbsent(l,k->new ArrayList<>()).add(new Arrival(b.getUUID(),at,l.getGameTime()+10));
        ArtVisualEntity.spawn(l,ArtVisualEntity.ESCHATON_ASCENSION,at,new Vec3(24,1,0),100).setCaster(b);l.playSound(null,b.blockPosition(),SoundEvents.END_PORTAL_SPAWN,SoundSource.HOSTILE,1.8f,.45f);return b;
    }
    public static final class Invitation extends Item {
        public Invitation(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext c){
            var l=c.getLevel();var p=c.getPlayer();if(p==null || !l.getBlockState(c.getClickedPos()).is(Blocks.BEACON))return InteractionResult.PASS;if(l.isClientSide())return InteractionResult.SUCCESS;
            Vec3 at=Vec3.atBottomCenterOf(c.getClickedPos().above());
            if(arriving((ServerLevel)l,at)){p.displayClientMessage(Component.literal("Another Last Myth is already within 256 blocks."),true);return InteractionResult.FAIL;}
            // Include freshly added entities before their section becomes tick-visible.
            for(var entity:((ServerLevel)l).getAllEntities())if(entity instanceof EschatonEntity boss && boss.isAlive() && boss.distanceToSqr(at)<=256*256){p.displayClientMessage(Component.literal("Another Last Myth is already within 256 blocks."),true);return InteractionResult.FAIL;}
            if(summon((ServerLevel)l,at)==null){p.displayClientMessage(Component.literal("The Last Convergence needs hostile difficulty, open space and at least seven blocks of height above the beacon."),true);return InteractionResult.FAIL;}
            if(!p.hasInfiniteMaterials())c.getItemInHand().shrink(1);return InteractionResult.CONSUME;
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){text.add(Component.literal("Use on a beacon to challenge Eschaton, the Last Myth.").withColor(0x9DF7ED));text.add(Component.literal("1,000,000,000 EP · three forms · Godly authority"));text.add(Component.literal("Prepare a wide endgame arena. Break creation seals to drain his reserves."));text.add(Component.literal("Consumed only after a successful summon."));}
    }
    public static final class Crown extends Item {
        public Crown(Properties p){super(p);}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
            var stack=p.getItemInHand(hand);if(HunterData.of(p).getBoolean("mythbreakers_legacy")){if(!l.isClientSide())p.displayClientMessage(Component.literal("You already carry the legacy of the Last Myth."),true);return InteractionResultHolder.fail(stack);}
            if(!l.isClientSide()){
                double ep=EnergyHelper.getBaseMaxEP(p);if(!Double.isFinite(ep) || ep<0)return InteractionResultHolder.fail(stack);
                var alignment=io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(p).getAlignment();
                var growth=dev.wildhunt.myths.DuoSystem.gains(p,alignment.getAuraGainMultiplier(),alignment.getMagiculeGainMultiplier());
                double auraShare=growth.aura()/(growth.aura()+growth.magicules());
                if(!Double.isFinite(auraShare) || auraShare<0 || auraShare>1)return InteractionResultHolder.fail(stack);
                // Native increaseMaxEP adds its argument to BOTH pools. Allocate the fixed
                // 25M total explicitly, retaining the primary/convergent growth split.
                EnergyHelper.setMaxAura(p,EnergyHelper.getBaseMaxAura(p)+25_000_000*auraShare);
                EnergyHelper.setMaxMagicule(p,EnergyHelper.getBaseMaxMagicule(p)+25_000_000*(1-auraShare));HunterData.of(p).putBoolean("mythbreakers_legacy",true);applyCrown(p);if(!p.hasInfiniteMaterials())stack.shrink(1);
                p.displayClientMessage(Component.literal("Mythbreaker — the last myth is yours. +25,000,000 base EP; +20% maximum HP and SHP.").withColor(0x9DF7ED),false);
                l.playSound(null,p.blockPosition(),SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.PLAYERS,1,.8f);
            }return InteractionResultHolder.sidedSuccess(stack,l.isClientSide());
        }
        @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> text,TooltipFlag f){text.add(Component.literal("Eschaton's guaranteed victory crown.").withColor(0x9DF7ED));text.add(Component.literal("Use once: +25M base EP; permanent +20% max HP/SHP modifiers."));text.add(Component.literal("Uses native EP allocation and attribute limits. Persists through death; cannot stack."));}
    }
    private static void applyCrown(LivingEntity e){for(var h:List.of(Attributes.MAX_HEALTH,TensuraAttributes.MAX_SPIRITUAL_HEALTH)){var a=e.getAttribute(h);if(a!=null)a.addOrReplacePermanentModifier(new AttributeModifier(CROWN,.2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}}
    public static void restoreCrown(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e){if(e.getEntity() instanceof ServerPlayer p && p.tickCount%40==0 && HunterData.of(p).getBoolean("mythbreakers_legacy"))applyCrown(p);}
}
