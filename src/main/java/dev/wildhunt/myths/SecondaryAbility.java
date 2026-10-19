package dev.wildhunt.myths;

import net.minecraft.server.level.ServerPlayer;

/** Native press/hold/release callbacks, with duration measured by the server. */
public final class SecondaryAbility {
    private SecondaryAbility() {}
    public static void input(ServerPlayer p,String action) {
        var d=DuoSystem.data(p);long now=p.serverLevel().getGameTime();
        if(action.equals("race_release")) {release(p);return;}
        if(action.equals("race_hold")) {if(d.contains("race_start"))d.putLong("race_keepalive",now);return;}
        if(!action.equals("race_press") || d.contains("race_start") || DuoSystem.converged(p) || !p.isAlive() || p.isSpectator())return;
        if(d.contains("race_last_press") && now-d.getLong("race_last_press")<3)return;
        d.putLong("race_last_press",now);
        DuoSystem.secondary(p).ifPresent(s->{
            if(s.isOnCooldown() || !s.canActivateAbility(p))return;
            if(io.github.manasmods.manascore.race.api.RaceEvents.ACTIVATE_ABILITY.invoker().activateAbility(s,p).isFalse())return;
            s.onActivateAbility(p);DuoSystem.saveSecondary(p,s);
            d.putLong("race_start",now);d.putLong("race_keepalive",now);
        });
    }
    public static void tick(ServerPlayer p) {
        var d=DuoSystem.data(p);if(!d.contains("race_start"))return;
        var race=DuoSystem.secondary(p);long now=p.serverLevel().getGameTime();
        if(race.isEmpty() || DuoSystem.converged(p) || !p.isAlive() || p.isSpectator() || now-d.getLong("race_keepalive")>40) {release(p);return;}
        var s=race.get();int held=(int)Math.clamp(now-d.getLong("race_start"),0,12000);
        int max=s.getMaxHeldTime(p);
        if(held>=12000 || (max>=0 && held>=max) || !s.canActivateAbility(p) || !s.onHeldAbility(p,held)) {DuoSystem.saveSecondary(p,s);release(p);return;}
        DuoSystem.saveSecondary(p,s);
    }
    public static void release(ServerPlayer p) {
        var d=DuoSystem.data(p);if(!d.contains("race_start"))return;
        int held=(int)Math.clamp(p.serverLevel().getGameTime()-d.getLong("race_start"),0,12000);
        d.remove("race_start");d.remove("race_keepalive");
        DuoSystem.secondary(p).ifPresent(s->{if(p.isAlive() && !DuoSystem.converged(p)) {var duration=io.github.manasmods.manascore.network.api.util.Changeable.of(held);
            if(!io.github.manasmods.manascore.race.api.RaceEvents.RELEASE_ABILITY.invoker().releaseAbility(s,p,duration).isFalse())s.onReleaseAbility(p,duration.get());}DuoSystem.saveSecondary(p,s);});
    }
    public static void clear(ServerPlayer p) {var d=DuoSystem.data(p);d.remove("race_start");d.remove("race_keepalive");}
}
