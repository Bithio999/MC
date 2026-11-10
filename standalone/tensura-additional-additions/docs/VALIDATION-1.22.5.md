# Additions 1.22.5 — Crucible curse protection

Master of the Crucible now prevents new curse engraving rolls while the learned Lord of the Forge passive is toggled on and usable. Protection applies to the acting owner’s equipment without weapon, armor, tool or forge-ownership restrictions. This covers native EP growth, deployed Living Arsenal weapons and the native engraving menu.

The native `EngravingHelper.applyCurseEngraving` roll is cancelled for protected owners. The native `increaseEngraving` path also rejects newly generated curse-tagged/configured curse engravings. Tsukumogami-tagged engravings are explicitly exempt from this latter check, and the normal god-tier Tsukumogami grant is untouched. Positive engraving rewards and growth-tier tracking continue normally. Existing curses are not removed; explicit research transfer and ordinary enchantment application remain unchanged. Toggling the passive off or using equipment as an unprotected owner restores native behavior.

Four native regression cases cover all armor slots, weapons, tools, shields and bows; preserved existing curses; toggle-off and unrelated-player controls; forced curse rolls during native EP engraving generation with positive rewards and god-tier Tsukumogami retained; the real engraving menu with a forced curse chance; and deployed arsenal EP rewards retaining Tsukumogami without curses. Modified test configuration is restored after each case.

Baseline verification: **44 required native forge tests** and **64 JVM tests** passed on **Tensura 2.0.1.3 / NeoForge 21.1.209**.

Compatibility verification: **44 required native forge tests** and **64 JVM tests** passed on **Tensura 2.0.1.4 / NeoForge 21.1.248**. This is an API/runtime check, not a complete modpack playthrough.

The clean baseline production build passed all **64 JVM tests**. The independent distribution audit verified the 1.22.5 JAR, native dependency metadata, assets and mixin classes, with no bundled dependencies, Myths classes or review fixtures.
