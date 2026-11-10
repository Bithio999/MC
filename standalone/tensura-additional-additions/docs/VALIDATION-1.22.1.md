# 1.22.1 — Forge and Living Arsenal fixes

Tensura’s Smithing Bench creates equipment without calling vanilla’s crafting hooks. It now signs the output before inventory transfer and awards the forging achievement only after a real take. Normal pickup and shift-click are covered. Vanilla crafting and smithing quick-transfer also sign the item before it is copied into inventory. Previously crafted equipment can receive the bonus with Reforge. The portable crafting menu now has a live recipe-update context.

Living Arsenal uses one shared 24-tick round, with evenly spaced launches for one through six weapons. Idle time cannot accumulate a burst of overdue attacks. Individual recall leaves the other escrowed weapons intact; incremental deployment reuses free slots without recharging or redeploying the remaining blades. Full recall, generation checks and saved item ownership remain intact.

Weapon rendering aligns each diagonal item model’s blade axis with its curved trajectory, interpolates orientation with quaternions, and banks into the cut. Short tapered trails appear only during the attack. Idle rings, flares, rainbow colors and return-path tethers were removed. The compact formation stays behind and above the owner.

The Forge screen uses Tensura’s native 254×163 ability-selection frame, blue equipment rows, search and side tabs. Tools, materials, tempers and actions use dropdowns. Living Arsenal supports choosing a group, adding weapons while deployed, and recalling a selected weapon or the whole arsenal. Reopening the ability manages deployed weapons; shift-activation quickly recalls everything. Selected tabs remain highlighted. Details scroll within their pane and action feedback is clipped with a full hover tooltip. The replacement icon uses native 32×32 framing, transparent corners and centered padding.

## Validation

- **353 / 353 native regression tests** passed on Tensura 2.0.1.3 / NeoForge 21.1.209.
- **24 / 24 focused forge tests** passed after the final bench-hook adjustment on both 2.0.1.3 / 21.1.209 and **2.0.1.4 / 21.1.248**. These cover real bench pickup and shift-click, Hihiirokane attack bonuses and synthesis credit, equipped armor/toughness, vanilla quick crafting/smithing, item escrow, individual recall and incremental deployment.
- **64 JVM tests** passed on both API profiles, including every roster size, reversed entity tick order, idle gaps and roster changes for the shared attack sequence.
- The final disposable-save client review passed native Ultimate activation, stable two-click synthesis, exact acquisition cost, tempering, masterpiece designation, two GUI sizes, six-weapon selection/deployment, **individual recall and incremental redeployment through the actual menu**, autonomous strikes and exact six-item recall. Captures were inspected for frame fit, HUD icon padding, sword size and cutting orientation.

This tests both supported Tensura APIs with the addon’s native dependencies; it is not a complete run of the Neo Otherworld modpack. The final clean baseline production build and distribution audit exclude review fixtures, alternative-profile resources and bundled dependencies.
