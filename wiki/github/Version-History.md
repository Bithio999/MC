# Version history

[Main Page](https://github.com/Bithio999/MC/wiki/Home) · [Skills](https://github.com/Bithio999/MC/wiki/Skills) · [Evolution](https://github.com/Bithio999/MC/wiki/Evolution) · [Mechanics](https://github.com/Bithio999/MC/wiki/Mechanics) · [Commands](https://github.com/Bithio999/MC/wiki/Commands)

## 0.7.2 — Alma front hairline

- Corrected Alma's front hairline: replaced the deep triangular exposed forehead with a short, even fringe and a narrow forehead.
- Reused ManasCore's native race/skill registrars directly to prevent a constructor-time registry lookup failure.
- Changed only the upper half of her front head texture. Her glasses, eyes, nose, mouth, remaining head faces, outfit, and Gemma's complete skin are preserved from 0.7.1.


## 0.7.1 — Otherworlder hair corrections

- Replaced Gemma's side, top and back head textures to remove the black cutout artifact.
- Reworked Alma's hair into a coherent swept-back style with a low bun.
- Preserved both existing front faces, outfits and equipment pixels exactly.
- The skin packer now rejects transparent head-face tiles rather than forcing cutouts opaque.


## 0.7.0

- Railbolt ricochets ignore blocks after the initial block-clipped shot. Horns and portrait gutters are corrected; each icon is isolated and centered.
- Thunder Step reaches 16/24 blocks, keeps player momentum, and has 5/4 second cooldowns.
- All art hits, fields, counters and signatures receive a 10% damage reduction. Physical/magic barrier bonuses fall 10%. Native multilayer barriers recover after ten seconds without a hit while funded and enabled; retoggling cannot bypass recovery.
- Water and ice toggles stop emitting constant custom particles. Crushing Current gets a curled water mesh; Undertow gets rotating spiral geometry and crosshair placement within 24/36 blocks.
- Hunter HP bonuses become +100/+500/+1,000; SHP +400/+3,200/+6,600, benchmarked against Enlightened Human/Human Saint/Divine Human.
- Three race-granted aura regeneration Extra Skills preserve the native 2:1 aura/magicule EP gain split.
- Alma and Gemma join the native otherworlder distribution as Awakened Hunters, with seven character-fitting Tensura abilities each, native humanoid AI and bundled original skins based on online visual references.


## 0.6.0 — Aim, shield HUD and hunter vitality

- Thunder Step now follows vertical aim as well as horizontal direction, retaining body collision and build-height checks.
- Extended Railbolt’s hop distance from eight to twelve blocks.
- Added real native Multilayer Barrier capacities to the water/ice passives so Tensura’s shield HUD counter appears. Native damage, magicule costs and break rules apply; upkeep does not repair a broken shield.
- Increased hunter HP bonuses to +40 / +100 / +200 and added +20 / +60 / +120 native SHP.
- Existing hunters receive the updated caps on login; existing enabled passives receive their initial HUD barrier without a reroll.

## 0.5.0 — Direct combat and toggleable passives

- Removed Storm charge, oil stacks, rime stacks and EP damage scaling.
- Made Railbolt ricochet sequentially; replaced Fulgurite Array with collision-checked Thunder Step.
- Removed the last buff mode from all eight skills; added native toggleable passives with ongoing aura upkeep.
- Replaced absorption hearts with native physical and magic barriers.
- Added rendered tentacles for Oil Lash and faceted ice geometry.
- Replaced Jin Dahaad’s fissure/rime/ward/frostfront kit with Avalanche Crash, Glacial Prison, Colossus Carapace and Ymir’s telegraphed Heavenfall Glacier.
- Preserved race gates, reincarnation rolls, learned skills, mastery, boss history and the centered portraits.

## 0.4.0 — Distinct arts and wiki

- Centered all eight existing portraits with consistent transparent padding.
- Replaced shared attack/field behavior with charge-and-rods lightning, a travelling tidal wave and reactive veil, owned oil-pool ignition and melee coating, and ice fissures with finite projectile wards.
- Gave each Ultimate a signature behavior: targeted judgments, alternating tides, a burning pool network, or an expanding frostfront.
- Integrated native Tensura particles and animated elemental magic circles.
- Added this linked wiki and offline visual edition.

## 0.3.0 — Combat and presentation corrections

Corrected seconds-versus-ticks cooldowns, missing skill names and slow domain timing. Added damage progression, original monster portraits, combo mechanics and native skill-rank damage metadata.

## 0.2.0 — Reincarnation and distinct bosses

Removed ordinary hunts and skill trials. Added one/three/five distinct boss gates and rollable reincarnation Uniques.

## 0.1.0 — Initial prototype

Added the human hunter branch and the eight skill registrations. Its ordinary-hunt/trial design was superseded. Old hunt totals are not converted into boss credit.

---

**Categories:** Tensura: Wild Hunt · Version 0.7.0
