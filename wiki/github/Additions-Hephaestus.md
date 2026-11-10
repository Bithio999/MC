# Hephaestus, Lord of the Forge

[All wikis](https://github.com/Bithio999/MC/wiki/Home) · [TensuraAdditionalAdditions](https://github.com/Bithio999/MC/wiki/TensuraAdditionalAdditions) · [Skills](https://github.com/Bithio999/MC/wiki/Additions-Skills)

![Hephaestus](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/assets/additions/hephaestus.png)

| Skill data | Value |
| --- | --- |
| Tier | Ultimate |
| Components | Fully mastered Prospector + Godly Craftsman |
| Awakening | True Demon Lord or True Hero |
| Achievement | Personally craft or smith Netherite or Hihiirokane equipment |
| Acquisition | 250,000 permanent magicules by default; configurable and affected by Tensura’s skill-cost gamerule |
| EP requirement | None |

![Native Ultimate menu](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-ultimate.png)

## Obtaining

Owning a component reveals an unfinished Hephaestus entry in Tensura’s **Skills → Ultimate** menu. Slot and activate that entry to open **Synthesis**. Complete the listed conditions, then confirm twice to combine the skills.

Awakening bonuses are applied **before** the acquisition cost. Synthesis waits until awakening is finished. Already-awakened players can qualify later. Hephaestus replaces both components and begins learned, unmastered. Engraving knowledge, stored materials and affected presets are retained.

The crafting achievement requires an actual craft or smithing result while you have Godly Craftsman. **Tensura’s Smithing Bench**, the vanilla smithing table and crafting tables are supported, including shift-click. Previewing a recipe does not count. The eligible equipment can be changed with the `heavenlyrestriction:hephaestus_synthesis_equipment` item tag. There is no EP threshold.

![Synthesis requirements](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-synthesis.png)

<a id="master-of-the-crucible"></a>

## Master of the Crucible · Passive, Toggle

- **+100% mining speed**, **Fortune V** added to the tool’s Fortune, and a **3%** material-find chance when properly mining stone or deepslate.
- Subtle ore markers within **32 blocks**, differentiated by material, including magic ore.
- Equipment you personally forge gains **+20% weapon attack damage, armor and armor toughness**, and **50% reduced durability damage**.

Forging bonuses apply once per item and remain on the equipment. They reduce ordinary wear and native EP durability loss; maximum durability is unchanged. **Damage type:** none directly; improved equipment retains its normal damage type.

<a id="divine-forge"></a>

## Divine Forge · Active, Press

Use the native-style side tabs, equipment search and Tools / Metal / Type (temper) dropdowns. Choose the action, then Apply. **Reforge** applies the bonus to equipment crafted before you acquired Hephaestus.

Open the forge to craft, use Tensura’s native refining and engraving menus, access inherited storage, temper equipment or repair it. **Recover** returns the previous Prospector refining ingredients to your inventory.

**Reforge / Temper:** 2,000 magicules and one selected material. Reforging signs unmarked equipment and applies the ordinary forging benefits. Signed equipment from another smith cannot be overwritten.

| Temper | Benefit |
| --- | --- |
| Keen | Increased critical-hit damage |
| Piercing | Ignores part of armor’s damage reduction |
| Conductor | Increased elemental damage |
| Enduring | Further reduces durability damage |

Iron provides **10%**, Diamond **20%**, Netherite **30%**, and Hihiirokane **35%** temper strength. Only one temper applies; replacing it consumes the material again. Tempers are separate from enchantments, engravings and the [Chromatic Orb](https://github.com/Bithio999/MC/wiki/Additions-Chromatic-Orb).

**Repair:** 500 magicules and one selected material. Restores 30% / 60% / 90% / 100% of ordinary durability and native EP durability, respectively, without changing engravings or the Orb’s one-use history. **Damage type:** none directly.

![Divine Forge](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-forge.png)

![Forge at a smaller GUI size](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-forge-compact.png)

<a id="living-arsenal"></a>

## Living Arsenal · Active, Press

Deploy up to **six actual melee weapons** from your inventory. They hover in a compact formation and take turns making curved slashes with short, tapered wakes, inspired by Terraria’s Terraprisma. The blades turn into each cut. A shared **24-tick round** spaces attacks evenly across the deployed weapons; each weapon attacks once per round.

Weapons prioritize your attack target, then visible hostile mobs or combat opponents within **32 blocks**. They use their own damage, enchantments, engravings and applicable wear reduction. Allies are excluded and normal server PvP rules apply.

**Cost:** 1,000 magicules per weapon to deploy, then 10 magicules per weapon each second. Deployed weapons are unavailable for other uses. Activate again to manage the arsenal. Select any combination of weapons before deploying; add weapons while active and recall individual blades. **Shift-activate** or choose **Recall all** to return everything. Death, logout, dimension travel or losing access to the skill recalls the weapons; saved escrow protects them across a restart.

**Damage type:** Physical weapon attacks. Native elemental conversions and engraving follow-ups retain their own types and defenses.

![Arsenal selection](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-arsenal-menu.png)

![Living Arsenal formation](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-arsenal.png)

![Autonomous weapon attack](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-arsenal-strike.png)

<a id="magnum-opus"></a>

## Magnum Opus · Active, Press

Designate **one personally forged item** as your masterpiece. It receives **+50% equipment stats** and **75% reduced durability damage**, replacing the ordinary +20% / 50% forging benefits. The item gains a glint and gold masterpiece identification; a deployed masterpiece has a golden wake.

**Cost:** 50,000 magicules and two selected materials, Diamond or better. Confirming a new masterpiece removes the old item’s designation, including when that item is stored elsewhere. Its ordinary forging benefits remain.

**Damage type:** none directly. There is no additional transformation or Overforge activation.

![Masterpiece selection](https://github.com/Bithio999/MC/raw/refs/heads/main/wiki/media/hephaestus-masterpiece.png)

All forge operation and arsenal costs also respect Schema’s resource-cost penalties.
