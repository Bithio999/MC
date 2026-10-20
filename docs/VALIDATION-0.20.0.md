# Tensura Myths 0.20.0 validation

Java 21.0.12.1+1 / Gradle 8.14.3 / Minecraft 1.21.1 / NeoForge 21.1.209 /
Tensura 2.0.1.3 / ManasCore 4.0.0.2, using the pinned repository dependencies.

## Native integration and comparisons

The required suite contains **152 native GameTests**, including ten new native
calibration/healing fixtures alongside the existing 142. **11 JUnit tests**
also cover the existing progression and budget invariants. The final native run
passed **all 152 required GameTests** with zero failures in 49.11 seconds;
`gradle build runGameTestServer` completed successfully in 1m 13s. The final
player-facing cost-description text is compiled in the packaged build.

Actual native casts were compared without Godly ownership or another active
skill's elemental toggle:

- Mastered native Musician's Sonic Blast removed **30 HP**, cost **50 MP** and
  applied its **one-second** cooldown. Mastered untoggled Railbolt removed
  **90 HP**, cost **600 AP** and applied its **three-second** cooldown against
  the same replenished recipient/defenses. The target had vanilla Resistance IV
  to survive repeat trials; its hurt timer was reset between isolated casts.
  This establishes a comparable damage-per-cooldown proxy in this fixture,
  not universal DPS parity against all native targets. Storm Mantle, ricochets,
  range and the native skill's own restrictions remain separate considerations.
- Mastered native Healer restored a controlled deficit of **10 HP / 20 SHP**
  for **1,600 MP**. Mastered Grove restored those same points for **1,100 AP**.
  Native Healer can restore immediately; Grove has a paid placement and its
  percentage-per-pulse limits. Resources are distinct and a hunter lineage
  normally gains them at 2 aura : 1 magicule.

Old/new native attack fixtures cover each of the eight Myths Uniques, their
revised aura payments and identical damage at 20M versus 40M base EP. They now
compare against the shipped 0.19 budgets, rather than requiring every future
change to beat an obsolete historical damage floor. Their measured reduction
bands also catch a missed attack path or accidental extra multiplier.

## Living Grove economy

The native fixtures cover:

- The 600-AP placement and separate per-pulse payment, base and mastered
  endpoints, actual HP/SHP recovery, no magicule billing, and free full pools.
- No second heal or bill from two ticks in the same game tick.
- Proportional partial funding for owner and owned pet, both HP and SHP,
  with no healing at zero aura and no negative reserve.
- HP-heal cancellation with a full reservation refund while preserving aura
  independently granted by another native event listener.
- A temporary heal quota that ends after the synchronous call, leaving
  unrelated native healing unaffected. Amplification cannot overrun the funded
  allocation; reduced healing refunds its unused portion.
- Fractional HP/SHP payment without integer rounding into free healing.
- Native Frost suppression when tested without Infinite Regeneration, the
  existing Sovereign Rebirth exception when enabled, and Infinite Imprisonment
  still blocking recovery/payment even with that exception.
- Increased bills for larger actual native pools and additive bills for multiple
  beneficiaries. Animal SHP is read from its actual native attribute value,
  rather than assuming a requested base value overrides native animal defaults.

Mastery improves the rates continuously, including the small mastery gain from
placing the field. Endpoint fixtures explicitly select zero/full mastery so
that this normal gain is not mistaken for incorrect billing. Pulse reservations
are bounded directly by available aura, avoiding a divide/multiply rounding
overshoot for partial funding. Refunds never include the placement payment.

The tests exercise the registered native heal-event listener, real resource
storage, native healing restrictions and the live server tick behavior. The
final production implementation preserves native regeneration exceptions;
no new global status suppression rule was introduced.

Existing Ultimate/Godly, secondary race, awakening, targeting, ally protection,
barrier, reflection, source attribution and lifecycle coverage remains in the
suite. No production hit-immunity reset, EP damage multiplier, new barrier or
stacking exception is added.

## Documentation and release

Individual articles for all eight Uniques show current damage, aura costs and
native damage types. Verdant Keeper documents per-point rates, beneficiary
billing, refunds and partial funding. The 0.19 external audit remains a versioned
historical reference; current tables explain the resulting calibration.

The wiki includes 146 articles plus its Main Page and 66 indexed abilities.
The generated HTML, prepared GitHub Wiki Markdown and offline bundle are built
from the current sources. Browser validation passes **294 desktop/mobile page
checks**, image loading, Living Grove ability search and **66 ability anchors**,
with no overflow or JavaScript errors. Local HTML links/assets and exported Wiki
references are also checked: **7,509** local HTML references and **1,591**
exported Wiki cross-page references across 149 Markdown files. The separate GitHub Wiki service is still unavailable
to this integration; its prepared edition is committed under `wiki/github`.

This change was verified through the native headless integration environment;
a new graphical Minecraft playtest was not performed. The approved visual
geometry and native screens are retained. No runtime compatibility claim is made
for Mysticism or Nightmares: their released code/configurations informed the
[external audit](research/UNIQUE_PARITY_0.19.md), while the runtime comparisons
in this release use pinned native Tensura.

Packaging verifies all **101 compiled classes**, mod version and tier tags,
no temporary attach helpers/dependency JARs, release/build byte identity and
all **631 wiki files** matching the offline ZIP byte for byte.

Release SHA-256: `c1b29e671511fcf7a27ee7017ef717658351d86394168e4ed9e7ffc5b946e305`.
