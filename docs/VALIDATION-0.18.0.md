# Tensura Myths 0.18.0 validation

Java 21.0.12.1+1, Gradle 8.14.3, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2; repository-pinned dependencies.

`gradle build runGameTestServer --console=plain` passed **130 required native
GameTests** and **11 JUnit tests**, zero failures. The final native run includes
the common combat, progression and cross-dimension cleanup changes. Client-only
presentation polish was built and replayed in the native development client.

## Addon references and balance

[The balance reference](research/ULTIMATE_BALANCE_0.18.md) records the inspected
Nightmares release, code/default examples, acquisition/type caveats, Mysticism
project metadata and its legacy-versus-current 1.21.1 Ultimate distinction.
Neither third-party addon was installed into this runtime, and no third-party
source or binaries are redistributed. This is a functional budget comparison,
not a claim of exact PvP parity across addon configurations.

All five Myths Ultimates retain native Ultimate classification, the existing
2M/two-mastered-Unique gate and three active modes. Their hits carry native
level-1 resistance penetration, with native attribute defenses, nullification
fallback, barriers, authority, hit immunity and PvP rules retained. Ordinary
positive elemental modifiers still use their strongest source. Godly ownership
traits remain separate. No new art multiplies damage by EP or writes health
directly to bypass damage handling.

## Progression and native menus

New native tests synthesize Anansi and Aegir from their exact two ingredients,
verify consumption, all-preset reference transfer and remaining cooldown
inheritance, and confirm five server recipe rows. Lugh ascension rejects a
missing mastered Threadweaver, accepts the complete true-awakening/EP/mastery
route, consumes both sources and retains native Ultimate plus Godly tags,
four modes, cooldowns, preset references and server menu state. Existing Aion,
Thoth, reincarnation, race progression and migration tests remain passing.

The native client shows Aion, Lugh and Thoth in its Godly list. Lugh's icon and
description use the normal native detail pane and toggle. The Ascension footer
cycles three destinations; its source list shows all five Ultimates. The
Synthesis footer was clicked through all five recipes to Aegir, showing the
correct ingredients and native Combine control. Existing backgrounds and
button styles are retained. Screenshots are included in the wiki.

## Lugh

The archive starts with three usable origin forms. Tests verify native NBT
round-tripping, selection that works during cooldown without payment/mastery
or cooldown reset, actual native damage observation, bounded reconstruction
power and duplicate-source refresh without extra entries. Invalid/stale
appraisal and attempted Godly technique sealing are free.

Shape/source probes verify Blade's forward fan, Lance's aim line, Crown's
cursor area, correct owner/skill attribution and normalization of a special
source. A deliberately oversized stored value remains bounded. The native
seal fixture confirms that Countermand blocks the precise source ability
through activation checks/native input, preserves learned state and cooldowns,
leaves unrelated and Godly casting available, expires automatically and is
released when Lugh is forgotten. A Godly owner can still have an ordinary
technique sealed by this Godly art.

Grand Synthesis snapshots the three records: modifying the live archive does
not change the pending attack. It waits twenty ticks and executes at ticks
20, 36 and 52 with the expected three native sources, then clears its pending
state. Lifecycle cleanup cancels attacks while retaining the learned archive.
Existing shared Godly damage/penetration/interference fixtures remain passing.

## Ultimate behavior

- Vritra's native damage fixture checks its opening hit, delayed fire and fixed
  warned front. Its complete combo retains all 21 pending detonations. Durable
  recipients now use vanilla Resistance IV so the stronger initial attack does
  not kill a fixture before its delayed-hit assertions; native immunity rules
  are not reset in production. An additional shape probe confirms the storm's
  second ray works when the aimed endpoint is a solid block.
- Demeter's revised fixture checks a twelve-second Perennial Seed, one-use 40%
  HP/SHP recovery and no added native barrier. Separate coverage verifies ally
  eligibility, ordinary cleanse and forgetting cleanup. Existing orchard,
  fixed-harvest and old ward-expiry compatibility fixtures remain passing.
- Janus retains native gate destination safety and momentum coverage. Its
  displacement fixture now checks the initial reposition without immediate
  damage, followed by warned spatial damage. Blocked gates remain free.
- Anansi's tests cover anchor arming, link intersections, once-per-second
  victim gating, the three-anchor limit, safe momentum-preserving anchor
  exchange and a fixed triangular Space/Earth finisher that consumes its nodes.
- Aegir's tests cover three-angle unioned targeting without triple hits,
  water sources, hostile projectile ownership transfer with friendly shots
  preserved, and an expanding annulus that hits each victim only once.

The teleport fixture uses a tracked world actor, matching the established gate
fixture rather than relying on fake-player connection acknowledgements.
A separate real-player client check moved the caster by the selected anchor's
six-block X / negative-five-block Z offset and preserved the exact requested
`(0.3, 0.1, 0.0)` momentum.

Source/shape probes intentionally observe attempted hits without dying; real
native damage/defense paths remain covered separately. Cosmetic entity tests
use actual world ticking to verify expiry, rather than assuming manually
calling `Entity.tick()` increments the external native tick counter.

The final cross-dimension fixture places a tracked recipient and formation in
the Nether while its owner is in the Overworld, then verifies cleanup removes
the owner's seed, exact skill seal and pending visual across levels. The common
visual lookup also handles previous-dimension formations. Marker leases remove
stale seed/seal visuals, including replaced or consumed markers.

## Native visuals

The final development client was exercised in the isolated **Myths Validation**
world, at 1280 × 800 with software OpenGL. The user's **New World** was untouched.
Actual learned skill instances produced Anansi's anchor network and Checkmate,
Aegir's basin/harpoon/Breaker, Lugh's three reconstructed forms, Appraisal,
Countermand and all three synthesis phases. Screenshots follow native visual
age. A durable test recipient keeps long visual sequences observable; this is
not a gameplay damage benchmark. Appraisal/Countermand use a real native damage
source observation from the fixture actor.

Lugh uses solid recessed ivory/gold rune tablets, three armament forms and a
synchronized three-tablet formation. Anansi uses articulated anchors, braided
threads and a closing shape. Aegir uses a circular, radially mapped caustic
surface, glass lenses and a continuous crest matching its expanding damage rim.
The final continuous-crest renderer was reloaded into the development client
and recaptured. Effects share material batching and distance/particle detail
controls; no shader pack is required. Original standalone icons are exported
at 64 × 64 with safe margins, without atlas neighbor sampling.

Temporary recipients were removed, the camera/HUD restored and the world saved
through a graceful client shutdown. Attach/capture helpers remain in `/tmp`
and are excluded from the release. No new renderer exceptions occurred.
Existing base-mod model warnings, unavailable OpenAL and development service-key
failures are unrelated. This is not a hardware performance measurement.

## Wiki and packaging

The wiki has **146 articles**, a Main Page and **66 indexed abilities**, each
linked to damage-type and gameplay information. New individual skill articles,
revised Ultimate articles, native menu/visual screenshots, synthesis routes and
damage references are updated. The Markdown ability directory is generated
from the same records used by offline search, avoiding stale summaries.
Gameplay pages contain no development changelog; research and history remain
under `docs`.

Both generated wiki editions and the self-contained ZIP are updated. Responsive
validation covers all **294 desktop/mobile page views**, loaded images, search,
ability anchors and JavaScript errors. Local HTML asset/link and exported Wiki
cross-page references are checked. The separate GitHub Wiki remains unavailable
to this integration; its prepared Markdown edition is committed in `wiki/github`.

Packaging verifies release/build JAR identity, every compiled class, mod version,
new Godly/Ultimate tags and icons, no attach helpers or dependency JARs, exact
SHA-256 and file-for-file offline ZIP parity with the final wiki tree.

Release SHA-256: `d405aabc78e30d49c9b9322810156ca555c0a3fabef22efa5643e0b3d687185c`.
