# 1.22.4 — Arsenal EP and engraving-loop fixes

Tensura's kill-reward handler iterates normal equipment slots, so weapons committed to Living Arsenal's saved escrow were never visited. The fix joins that existing reward pass after its native eligibility checks and passes each deployed item through the native private gear-growth routine.

Only that routine's main-hand read and evolution write are redirected to a scoped escrow item. No real equipment swap is performed. Normal held/worn equipment rewards, player EP, gamerule multipliers and native kill exclusions remain intact. The native routine handles EP gain, Vigor/Growth/Lethargy, caps, Stagnation, random engravings and item/unique evolutions. Fresh native gear is initialized using Tensura's own equipment initialization.

A weapon that earns a lethal-hit reward stays connected to its active attack context, including when native evolution replaces the stack. Post-hit wear and final escrow writeback therefore preserve the grown/evolved item. Its synced visual item is refreshed, and recall returns the saved updated weapon. Forge identity, tempers and Chromatic history survive native evolution. The native gear-owner/activity update is also applied.

## Validation

Five new native cases cover:

- A real ordinary kill rewarding held equipment and all six deployed weapons equally, with exactly seven items after recall.
- A real arsenal killing blow retaining its EP through post-hit writeback and the visual-item update.
- Matching native Growth-engraving gains and no growth for a victim marked to skip EP rewards.
- Native item evolution while deployed, matching the ordinary held reference, preserving item data and not replacing that held reference with an escrow item.
- Native initialization of a fresh Hihiirokane sword and enforcement of the native EP cap.

The opt-in review commands are documented in `docs/review-hephaestus/README.md`. Dedicated tests inspect the same synced item stack that the client renderer receives; this patch changes reward/persistence behavior without changing the interface or renderer.


## Engraving escalation fix

Godly Craftsman rejects research entries above an engraving's ordinary maximum. Normal entries from the same donor still follow native research/consumption rules; blocked entries remain intact. Legacy Godly Craftsman engraving knowledge is clamped to ordinary maxima and saved back on read. Ordinary enchantments keep their configured bonus-level rules. These checks also cover direct calls to the native research API.

Lord of the Forge keeps overcap research. It can learn at least the ordinary maximum plus one for allowed engravings while retaining native exclusions and any higher configured allowance. Applying an Orb-eligible overcap automatically marks the output as Chromatic-infused, making another Orb invalid. Both the preview and finalization paths apply this policy; the preview does not mutate the input. Capped normal applications do not consume the allowance. Godly Craftsman cannot inject transferred overcaps through stale/forged selections, but it preserves a legitimate overcap already on the input item. Existing item's engraving levels are not reduced by this knowledge cleanup.

Five additional native tests exercise mixed-donor research and preservation; direct API rejection and persisted legacy cleanup; native menu overcap research/application through Lord of the Forge and repeated Orb-loop attempts; editing existing infused items without lowering or raising their overcaps; and ordinary engraving applications retaining their first Orb use while native blacklists remain enforced.


The final combined baseline suite passed **369 required native tests** on **Tensura 2.0.1.3 / NeoForge 21.1.209**. The **2.0.1.4 / 21.1.248** profile passed **40 focused native forge tests** and **64 JVM tests**. This checks both supported APIs rather than an entire Neo Otherworld modpack run. Existing equipment levels are preserved; Godly Craftsman's stored knowledge is normalized on read, and the receiver's Orb allowance is set on overcap application.

The final clean production build passed all **64 JVM tests** on the baseline profile. The independent distribution audit verified the 1.22.4 JAR, dependency metadata, extracted assets and all new mixins, with no bundled dependencies, Myths classes or review fixtures.
