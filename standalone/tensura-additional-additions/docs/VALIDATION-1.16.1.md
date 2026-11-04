# 1.16.1 — Sortable Locate structure browser

Locate displays every registered structure supplied by the server; the previous 1,024-entry cap is removed. The default Name A–Z order sorts by displayed structure name, with the full registry ID as a deterministic tie-break. Mod A–Z sorts by mod display name, namespace and structure name. Every row shows its originating mod; tooltips include the full registry ID. Datapack namespaces without a matching installed mod fall back to a readable namespace label.

Live search filters structure names, IDs and mod display names without changing the selected structure ID. A filtered-out selection cannot start a search. Clearing the filter restores eligibility for an existing selection. Resizing retains the query and sort mode. Wheel scrolling and a draggable scrollbar reach the full filtered list. Dialogs retain the opaque atlas style and fit the minimum 240-pixel GUI height.

The current integrated review extends `-PmapUiOnly=true` with complete advertised-list coverage, both sort modes, incremental filtering, empty results, stable selection, namespace filtering, resize retention and dragging the scrollbar to the end and back. It also retains the existing dimension/pin/shared-exploration UI exercise. Only a guarded disposable normal-generation world is used.

Locate’s existing 100,000-block range, viewed-map search origin, server-side skill validation, generation worker and travel behavior are unchanged. No new skill, resource cost, save migration or network schema is introduced.

The integrated client completed with `BUILD SUCCESSFUL` against Tensura 2.0.1.3 / NeoForge 21.1.209. Its normal-world registry contained 69 structures; a live village filter reduced the list to 17. All browser assertions passed at GUI scales 2 and 4, including stable ID selection, empty-filter safety and full scrollbar travel. The screenshots are included in wiki media.

The clean production build passed all 49 JVM tests with 0 failures. The standalone JAR passed the distribution audit and excludes review fixtures, test presets and bundled dependencies.
