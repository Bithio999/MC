# 1.4.0 — World Select

Parallel World Jumper now has two native modes: **World Jump** and **World Select**. World Select becomes available after learning. Selecting a visited-world card and confirming sets a one-use destination for the next jump. Confirming **New World** clears it. Merely opening the menu or clicking a card does not jump. Both modes have no resource cost or damage.

The atlas supports name/seed search, pagination, mouse-wheel paging, keyboard controls and Ctrl+F search focus. Its header shows the saved next destination; the detail panel distinguishes fresh generation from a return. Missing saves remain identifiable and cannot be selected. The current world is excluded.

## Save behavior

Each continuation has a versioned world journal with opaque stable IDs. On departure its current folder is recorded. Returning updates that world's folder under the same ID, so future visits use its latest saved state without duplicate cards. Existing 1.3.0 history is imported by following recorded, existing sibling saves. This is a travel history, not a list of unrelated saves.

A return first saves and closes the current integrated server. It validates/locks the chosen destination through native level storage, stages a complete copy of that save, transfers the current player's complete NBT/attachments, inventory, skills, race and EP, and copies their current advancements/statistics. Destination terrain, entities, dimensions, world time, maps and End state remain. Landing uses the destination's previous position with the existing safe-ground search/fallback. The preceding saves remain recoverable. Copy failures clean staging and leave the two completed saves intact. Starter Kit first-join protection is retained.

Only a learned permanent skill on a living private integrated-server owner permits selection. Forged, unknown, missing and current-world IDs are rejected. Packets carry an opaque ID, never a filesystem path. The existing one-use local transfer ticket remains mandatory; dedicated/LAN worlds cannot select or jump. Registry IDs and dependencies are unchanged for existing standalone worlds.

## Verification

- **19 JUnit tests passed:** file-copy boundaries, all-dimension return terrain, failure cleanup, journal round trips/stable IDs, malformed/linked history, Starter Kit and guide data.
- **153 native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209, including learning-mode/dedicated-selection gates and current-character versus destination-world NBT preservation.
- **Three real client transfers passed:** B → A → B → fresh world. The native menu's seed search, selection button and server packet were exercised. Both return seeds matched; current inventory, 1M EP, persistent progress and updated XP carried over. A placed diamond block and custom-dimension terrain file survived the return to B. Stable world IDs prevented duplicates. The selection cleared after use; the next unselected jump generated a fresh seed and regenerated terrain. Landing and published-LAN rejection passed.
- Final menu layouts were rendered at GUI scales **2 and 3** after correcting a double background overlay; the optional menu-only review repeats the actual search/selection UI.
- Production build/distribution audit verifies version, assets, dependency ranges, journal/menu classes and exclusion of optional review fixtures. No third-party dependencies are bundled.

[Menu](media/world-select-1.4.0.png) · [Compact menu](media/world-select-compact-1.4.0.png) · [Reproduce the client review](review-world-select/README.md).

The new live review used the standalone and required dependencies. The existing Neo Otherworld compatibility/dependency ranges remain unchanged; its optional integration fixture also forwards the new return-metadata test. This release does not change Tensura Myths.
