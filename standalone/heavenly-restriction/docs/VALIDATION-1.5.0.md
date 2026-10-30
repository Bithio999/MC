# 1.5.0 — Hidden world library

Parallel World Jumper keeps **one stable active save** in Singleplayer. Visited terrain/world state is stored below that save’s `.pwj-library` directory and reached through World Select. A library can create up to **128 unique worlds, including the active world**. Reaching the cap blocks new-world activation before transfer/mastery gain, while return travel remains available. Older journals above the cap remain readable and cannot create more; no existing worlds are dropped.

**Deleting the active save using Singleplayer Delete removes its entire hidden library**, including archived worlds and internal recovery copies. Minecraft’s native recursive save deletion supplies this behavior; no global deletion hook is needed. Whole-save backups include the library. Unrelated saves are not part of it.

## Transfers, upgrades and recovery

Travel saves and closes the integrated server, holds native storage access while preparing the new world, and swaps the prepared terrain into the same save folder. Current character/inventory/skills/EP/XP/progression transfer as before. Returns retain the latest visited terrain/builds/entities, world time and dimension state. The library is moved once between slots and excluded from all world snapshots, preventing recursive copies. Starter Kit tracking keeps the stable folder key.

Completed departure snapshots are archived only after safe landing. A transaction receipt and preceding slot permit rollback after preparation failure, interrupted directory renames or a failed landing. Startup and the Singleplayer screen recover pending transfers before world selection. Receipt validation refuses to overwrite an unrelated replacement save. Library IDs/path checks reject escaping or linked storage. Journal writes use atomic replacement where supported.

On the next successful jump, recorded pre-hidden-library worlds migrate into the owner’s library. Older travel copies reached through their recorded journals/history become hidden recovery copies. Only recorded links with matching world identity/seed are followed; unrelated saves are not scanned. A restored/renamed library resolves its own archive IDs instead of adopting an original sibling save. Migrated files remain owned by the active slot, so deleting the slot removes those copies too.

## Verification

- **25 JUnit tests passed:** stable-slot round trips, library exclusion, every tested install boundary, failed preparation/explicit rollback, unrelated replacement protection, legacy imports, linked IDs, cap enforcement, journal serialization and guide data.
- **156 native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209. New checks cover native world discovery and deletion, latest/recovery legacy-copy migration, and restored-backup isolation.
- **Real client review:** B → A → B → fresh world through the same folder. Current 1M EP, inventory, updated XP and saved progress survived; B’s placed diamond block and custom-dimension terrain survived return. The native UI search/selection packet worked at GUI scales 2 and 3. The 128-entry journal blocked native new-world activation while permitting return selection. LAN remained rejected.
- The client’s native save discovery showed only active + unrelated. Its native Delete call removed the owner and all its nested world data, leaving the unrelated save available. Screenshots record the world list before/after deletion. Tests used copied disposable worlds.
- Production packaging verifies version/dependencies, all library/menu classes and assets, with review code and third-party JARs excluded. Registry IDs and supported dependency ranges remain unchanged.

[World Select](media/world-select-1.5.0.png) · [Singleplayer before deletion](media/library-singleplayer-1.5.0.png) · [After deletion](media/library-deleted-1.5.0.png) · [Reproduce the live review](review-world-library/README.md).

The new client/native checks used the standalone with its required dependencies. This release does not change Tensura Myths. Earlier World Hop videos document their recorded versions and show the save/reload transition; older multiple-save behavior has been replaced by the hidden library.
