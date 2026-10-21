package dev.wildhunt;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.List;

public final class AnankeItems {
    private AnankeItems(){}
    public static AnankeEntity summon(ServerLevel level,Vec3 at){
        var boss=MythBosses.ANANKE.get().create(level);if(boss==null)return null;boss.setPos(at);
        if(!level.getWorldBorder().isWithinBounds(boss.blockPosition()) || !level.noCollision(boss,boss.getBoundingBox()))return null;
        boss.finalizeSpawn(level,level.getCurrentDifficultyAt(boss.blockPosition()),MobSpawnType.MOB_SUMMONED,null);
        if(!level.addFreshEntity(boss))return null;
        ArtVisualEntity.spawn(level,ArtVisualEntity.ANANKE_ASCENSION,at,new Vec3(12,1,0),60);
        level.playSound(null,boss.blockPosition(),SoundEvents.END_PORTAL_SPAWN,SoundSource.HOSTILE,1,.6f);return boss;
    }
    public static final class Writ extends Item {
        public Writ(Properties p){super(p);}
        @Override public InteractionResult useOn(UseOnContext context){
            var level=context.getLevel();var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
            if(!level.getBlockState(context.getClickedPos()).is(Blocks.LODESTONE))return InteractionResult.PASS;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            Vec3 at=Vec3.atBottomCenterOf(context.getClickedPos().above());
            if(level.getDifficulty()==Difficulty.PEACEFUL || !level.getEntitiesOfClass(AnankeEntity.class,new AABB(at,at).inflate(128)).isEmpty()){
                player.displayClientMessage(Component.literal("The writ requires a hostile difficulty and no Ananke within 128 blocks."),true);return InteractionResult.FAIL;
            }
            if(summon((ServerLevel)level,at)==null){player.displayClientMessage(Component.literal("Ananke requires open space above the lodestone."),true);return InteractionResult.FAIL;}
            if(!player.hasInfiniteMaterials())context.getItemInHand().shrink(1);return InteractionResult.CONSUME;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> text,TooltipFlag flag){text.add(Component.literal("Use on a lodestone to challenge Ananke, the Unwritten End."));text.add(Component.literal("30,000,000 EP · 48,000 HP · 120,000 SHP").withColor(0xF2C36D));text.add(Component.literal("Prepare a wide arena. The writ is consumed on a successful summon."));}
    }
    public static final class Testament extends Item {
        public Testament(Properties p){super(p);}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return player.getCooldowns().isOnCooldown(this)?InteractionResultHolder.fail(player.getItemInHand(hand)):ItemUtils.startUsingInstantly(level,player,hand);}
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 48;}
        @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
        @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity user){
            if(!level.isClientSide()){
                user.heal(user.getMaxHealth()*.5f);var e=TensuraStorages.getExistenceFrom(user);double max=user.getAttributeValue(TensuraAttributes.MAX_SPIRITUAL_HEALTH);
                e.setSpiritualHealth(Math.min(max,e.getSpiritualHealth()+max*.5));e.setAura(EnergyHelper.getMaxAura(user));e.setMagicule(EnergyHelper.getMaxMagicule(user));e.markDirty();
                if(user instanceof Player p){p.getCooldowns().addCooldown(this,6000);if(!p.hasInfiniteMaterials())stack.shrink(1);}else stack.shrink(1);
                level.playSound(null,user.blockPosition(),SoundEvents.BEACON_ACTIVATE,SoundSource.PLAYERS,1.2f,1.4f);
            }return stack;
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> text,TooltipFlag flag){text.add(Component.literal("Ananke's guaranteed trophy. Drink for 2.4 seconds."));text.add(Component.literal("Restore 50% maximum HP/SHP and refill aura/magicules.").withColor(0xF2C36D));text.add(Component.literal("Five-minute cooldown. Native healing rules still apply."));}
    }
}
