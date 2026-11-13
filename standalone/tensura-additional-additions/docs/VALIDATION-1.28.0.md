# Additions 1.28.0 — Laplace equation board

Equation Editor now presents native equations as a draggable board of named outputs, nested operator/input blocks and linked helper results. Library operations wrap an existing expression and retain it as their first input; new sockets stay explicitly empty until filled. Output and cost previews update with every edit. Palette drag, click-to-place, editable number dialogs, occupied-slot swaps, Shift-copy, undo/redo, reset, advanced text, helper links, pan, zoom, fit and a compact drawer are provided. Existing saved equations retain their exact source until edited; the serializer preserves operator precedence and associativity.

The UI distinguishes a local draft from its saved live action. Apply & Enable validates/saves the action, enables it and turns Universal Formula on through an owner-bound server request. Failed validation does not toggle the passive or write profiles. Disable operates on the server-owned saved equations, ignores packet equation contents and retains the passive/other actions. The existing five-second edit timer, ownership, learning, capacity, mastery, formula validation and balance limits remain.

Server preview inputs refresh without rebuilding the board or replacing a draft. Pan/zoom survive library tools and dialogs. Refresh requests are distinguished from apply responses; an apply submitted during refresh is queued instead of lost. Number/text dialogs and structural edits retain normal invalid-draft validation. Draft switching and closure offer Keep editing or Discard; keeping returns to the board. Hover explanations are delayed to keep the canvas readable.

## Verification

- **52 native baseline cases passed** on Tensura 2.0.1.3 / NeoForge 21.1.209: 18 Laplace cases and 34 existing Metis/automatic-casting cases. The new cases verify Apply & Enable with the passive initially off, persisted equations, independent Disable using stored values, and rejected circular input without state mutation.
- **18 Laplace native cases passed** on Tensura 2.0.1.4 / NeoForge 21.1.248. Native servers saved and shut down normally; these are focused compatibility checks rather than a complete modpack playthrough.
- Actual Minecraft client interactions cover native synthesis, action search, dragging Multiply onto Wind Cutter’s base component, filling its new socket, changed output/bill, undo/redo, a number dialog, a real Apply request, resulting enabled action/passive, linked helper output, rejected circular graphs, advanced text, custom number placement, keeping an unapplied draft when switching actions, and compact drawer drag/scroll. Normal and forced 320×180 layout bounds and control overlaps were checked. The small library intentionally overlays the canvas and is hit-tested first, then hides while dragging. The forced size is a layout stress test; Minecraft may select a larger minimum GUI size for a given real window.
- Twelve new JVM board cases cover structural wrapping, explicit holes, operator precedence/function round-trips, exact preservation of untouched formulas, swaps/copies/moves, ancestor-cycle rejection, dependency propagation, helper removal/recovery, reset/unwrap/undo, rejected edits, numeric validation, immutable addresses and bounded history. The original formula/balance tests remain.

```sh
./gradlew runGameTestServer -I docs/review-high-realm/server.init.gradle -PlaplaceFocus -PlaplaceRegression -PmentorFreshReview=board-baseline
```

For the focused API profile omit laplaceRegression and add `-PneoOtherworldDirectory=/tmp/realm-api-profile -I docs/review-high-realm/api-runtime.init.gradle` with a separate disposable save. Run profiles sequentially. Use `docs/review-laplace/client.init.gradle` only with its isolated review save; it is an opt-in client harness, not production content.

The final clean baseline production build passed **103 JVM tests**, with zero failures, errors or skips. Distribution validation passed for 1.28.0 metadata, required production board/model/network classes, registered mixins and dependencies, source-matching assets and the 25-entry guide, with opt-in review fixtures excluded. The unchanged archive context audit passed. Release SHA-256: `7dd3b297de341e34f2aa864b4daf483b500a1da54d9148918db3830f6d53738d`.

The updated Laplace guide matches its generator literal. Wiki verification covers 212 articles, 142 ability records and 213 exported Wiki pages, with board instructions, actual-client captures and the offline edition. The tracked-tree audit and an entry-by-entry new-JAR scan (including compiled classes) check private identity strings and credential patterns. Public commits use the generic contributor identity. The discontinued Myths gameplay/guide is unchanged.
