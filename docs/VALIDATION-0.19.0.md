# Tensura Myths 0.19.0 validation

Repository-pinned Java 21.0.12.1+1 / Gradle 8.14.3 / Minecraft 1.21.1 /
NeoForge 21.1.209 / Tensura 2.0.1.3 / ManasCore 4.0.0.2.

`gradle build runGameTestServer --console=plain` passed **142 required native
GameTests** and **11 JUnit tests**, zero failures. The final successful native
run completed all 142 fixtures in 43.02 seconds; total build 1m 25s.

## Combat and progression

Twelve new native fixtures supplement the existing 130:

- Each of the four apex Uniques compares its actual cast damage against its
  previous raw budget through the same native damage pipeline. Each verifies
  its new aura cost and identical damage after raising base EP from 20M to
  40M. The original extra 0.90 penalty is included in the previous baseline.
- Each of Threadweaver, Rune Smith, Verdant Keeper and Mirrorbound compares
  its native attack with its own former budget, checks actual aura payment
  and repeats at higher EP. Rune Smith includes placement plus commanded
  detonation, rather than treating its command as a free standalone attack.
- Living Grove verifies a 7% HP / 6% SHP recovery pulse against real owner
  pools. Demeter's orchard remains stronger in both pools under the same setup.
- Aion runs its full native six-second held channel: twelve damaging pulses,
  exactly 15,600 magicules, no aura payment, followed by its release cooldown.
- Thoth runs its full ten-second Library against a replenishing native combat
  target and a matched old-budget target, using the same seven types and native
  timing. All 38 attempts execute; measured damage grows with the new budget,
  with the same successful-hit behavior as the old-budget control. This fixture
  permits Tensura's actual typed-hit handling; it does not assume a global
  ten-tick cooldown must suppress alternating elemental hits.
- Lugh loads an old saved archive, upgrades old origin power, clamps an appraised
  entry to the new minimum and preserves its source, name and selected slot.

The existing seven-element native comparison also uses the old release's
chapter/volley budgets with matched native types and timing. Stronger hits need
more durable test recipients: vanilla Resistance IV and replenished fixture
health preserve multi-hit observations without resetting immunity in production.
Tests that isolate separate casts may reset their fixture's hurt timer between
trials. Full channel/sequence comparisons retain real native timers.

Existing coverage remains passing for all five Ultimate kits, elemental source
selection, defensive cleanup, target caps, ally protection, native status effects,
Godly ownership/authority/source-scoped bypass, free manual and automatic Causal
Reversal, post-payment checkpoints, increase-only reserve restoration, uninterrupted
2:1 Mana Crucible, native progression, secondary races and awakening.

Causal Reversal fixtures now expect 88,000 MP after recording from 100,000 MP,
matching its 12,000 initial cost. Return remains free even while unfunded or
suppressed. The Crucible follow/cleanup test waits for its entity to enter the
UUID index before inspecting it, eliminating a fixture tracking race without
changing runtime channel behavior.

These are controlled native integration checks, not a claim of perfect PvP
parity across arbitrary addon/server configuration. Damage still depends on
native authority, defenses, attributes, mastery, positioning and hit handling.

## Live native client

The development client loaded the isolated **Myths Validation** world. A runtime
audit learned all eight Uniques and three Godly skills, checked their actual
native display names, every active-mode cost and the correct aura/magicule pool,
and synchronized learned state. All eleven kits passed. The Godly category
displayed Aion, Lugh and Thoth using the native menu. Grand Synthesis was cast
in the client and captured at ages 17, 28, 44 and 60; its saved origin records
used 3,800 damage and its saved appraised Rune Smith record used the new 3,500
floor. Temporary recipients were removed, the camera/HUD restored and the
client closed gracefully. The user's separate
**New World** save was not opened.

Runtime test helpers stay outside the repository and release JAR. Sound-device
and base-mod model warnings in this software-rendered environment are unrelated
to the balance changes. No external addon runtime compatibility is asserted.

## Wiki and release

All eleven skill articles and the common mechanics page reflect current damage,
costs, upkeep, healing and mastery rules, with per-ability native damage IDs.
The approved Ultimate gameplay remains intact; old comparative multiplier prose
is removed from its gameplay articles. Development history stays under `docs`.

The wiki contains 146 articles plus its Main Page and 66 indexed abilities.
Both generated editions and the offline ZIP are updated. Browser validation
passes **294 desktop/mobile page checks**, images, search and ability anchors,
with no overflow or JavaScript errors. Link validation passes **7,508** local
HTML links/assets and **1,590** cross-page references across 149 exported Markdown
files. The prepared GitHub Wiki edition is committed under `wiki/github`; this
does not claim publication to the unavailable separate GitHub Wiki service.

The release JAR matches the build byte for byte, all **97 compiled classes** match,
version metadata is 0.19.0, and no temporary attach helpers or dependency JARs
are included. All **631 wiki files** match the offline ZIP byte for byte.

Release SHA-256:
`0c82ecdd0c2c385da526556d462adc0c3e6f777defc044d3d2d8c867d93be258`.
