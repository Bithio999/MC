# Chained dash — terrain and headroom failures

## Reproduction

The reported screenshot shows level aiming through a birch forest. A native test reproduces this with **no dummy or enemy**: a birch trunk 12 blocks ahead, persistent leaves two blocks above the floor, and a grounded caster aiming level. The old resolver rejects the cast because Martial Master’s adjacent-cell landing snaps the feet one block above ground, putting the caster’s head inside the canopy.

Before changing the resolver, native GameTests also reproduced three failures: slight upward aiming in a traversable two-block corridor; a level aim ending on a slab/one-block rise; and a short enemy stop near one block away. These four scenarios failed against the previous gameplay implementation. The short-stop failure came from both a hard 0.25-block travel minimum and floating-point width calculations.

## Resolution

Successful native destinations are retained. If a preferred destination fails, the resolver samples closer positions at 0.25-block intervals and queries actual nearby collision surfaces within 1.5 vertical blocks. It checks exact support heights and the caster’s current standing height rather than rounding partial blocks to whole cells. Full-body clearance, build bounds, loaded destination chunks, world border and native non-teleportable tags still apply.

Fallback candidates require forward movement and an unobstructed ray from the caster’s eye to the landing eye position. This prevents a height adjustment from landing through a wall or on the far side/top of a canopy. For a block-centered point behind the caster, the resolver can retry the exact hit contact with body clearance. The minimum travel is reduced to 0.001 blocks so a valid short stop is not rounded away; zero/backward movements remain rejected. The hit calculation remains protected from zero-length travel.

The true blocked-destination test now places the caster’s body against the wall. A body centered in a one-block cell still has forward room and is no longer a valid fixture for a fully blocked dash after short stops become legal.

## Scope

This changes landing selection only. Enemy selection still chooses the nearest eligible aimed target before the first block hit; allies, off-axis enemies and occluded targets do not capture it. The skill remains grounded/in-water, Physical-only, paid with 2% current aura on success and a 1-second cooldown. Exclusive Battlewill/Physical damage branches remain unchanged. No safe destination means no aura payment or cooldown.

Native regressions cover the forest, corridor, slab/full-block rise, close enemy, close wall, truly blocked destination, native downward targeting, open-air upward aiming, fluid rays and normal enemy stops. The opt-in client fixture repeats forest/headroom/terrain cases with a real server player in the isolated **Restriction Validation** world.
