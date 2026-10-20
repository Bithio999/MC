# Tensura Myths 0.17.0 validation

Java 21.0.12.1+1, Gradle 8.14.3, Minecraft 1.21.1, NeoForge 21.1.209,
Tensura 2.0.1.3 and ManasCore 4.0.0.2; repository-pinned dependencies.

`gradle build runGameTestServer --console=plain` passed **110 required native
GameTests** and **11 JUnit tests**, zero failures.

## Shared ownership traits

The former Aion damage callback and native resistance mixins now consult
Godly ownership through the common classification helper. Aion, Thoth and
skills in the public Godly tag qualify independently of mode, equipment,
toggle, upkeep or current magicules. Multiple Godly skills apply the outgoing
35% bonus only once. A Godly defender is exempt from this damage bonus, but
remains subject to source-specific penetration.

The revised native defense fixture starts without Godly ownership, confirms
spatial nullification blocks damage, then learns Thoth with its toggle off and
magicules empty. It checks resistance and nullification bypass, the 100-to-135
damage modifier, non-stacking Aion ownership and the Godly-defender exemption.
The complete native damage path verifies a learned spatial nullification and
90% spatial resistance no longer block the owner's hit. Forgetting Aion while
retaining Thoth preserves the trait; forgetting the final Godly skill restores
the native defense gate without changing the defender's saved toggles.

A separate test exercises ordinary melee, owned arrows, generic magic and an
ordinary Unique's elemental attack through the bypass and bonus hooks. It also
covers a projectile-only source's shooter fallback and rejects unrelated
attacker/environmental attribution. The aura isolation fixture confirms an
active Aion still cannot suppress another attacker's spatial nullification or
its target's poison immunity. The target-wide `isToggled` and skill-interaction
suppression mixins are removed. Old suppression leases are discarded; they no
longer influence any defense query. Existing Aion movement/projectile dilation
and barrier fixtures continue to pass.

## Source-aware interference immunity

NeoForge's native effect applicability event reads Tensura's effect-instance
source-ability metadata. Only Magic Interference is rejected, and only when
its origin lacks Godly classification. An ordinary skill used by a Godly
caster remains ordinary; a Godly-origin effect is not rejected by this trait.
Unattributed interference has no Godly authority. Existing ordinary interference
is cleansed on the next server tick after acquisition or loading.

The native effect test verifies an ordinary recipient first accepts interference,
then gains Thoth and loses the existing effect. It rejects an ordinary skill
from a Godly caster, accepts explicit Godly-origin interference, preserves
that provenance through effect save/load and tick cleanup, rejects unattributed
interference, preserves unrelated Silence behavior, and restores ordinary
susceptibility after the last Godly skill is forgotten.

This protects the native Magic Interference effect. It does not claim blanket
immunity to arbitrary third-party casting restrictions or statuses. Source-less
damage cannot inherit ownership traits, and source-less interference cannot
claim a Godly origin. Armor, barriers, native authority and PvP protection retain
their independent rules. Aion's barrier/slow toggle retains its separate upkeep.

## Wiki and release

The shared classification article documents all three passives with damage-type
information and exact scope. Aion, Thoth, the mechanics/damage references, the
ability index and in-game descriptions now distinguish always-active ownership
traits from individual toggles. Gameplay articles contain no development history;
the changelog records release changes separately. The ability directory also
corrects its two stale Thoth summaries to the existing seven-element spectrum.

Both generated wiki editions and the offline bundle are updated. The separate
GitHub Wiki remains unavailable to this integration; the prepared Markdown
edition is committed under `wiki/github`. Responsive checks cover all 144 pages
at desktop/mobile widths (288 views), images, search and ability anchors, with
no overflow or JavaScript errors. Local HTML assets, cross-page links and all
exported Markdown pages are checked.

The release JAR is byte-identical to the build, includes the common Godly
passive helper and updated combat classes, excludes the retired aura-defense
mixin and contains no attach helpers or dependency JARs. The offline ZIP matches
the complete wiki directory file for file. This release changes server combat
and effect behavior; it does not change rendered skill geometry or menu layout.

Release SHA-256: `d8e90cbe422e83740d58faaf8bbb8b2b80004853a57ee4521060c887a7528922`.
