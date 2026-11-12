# Tyche native-client review

Opt-in review harness; excluded from release builds. Requires Java 21 and the disposable `Archive Review 3` client save under `/workspace/validation-mentor/client`. The harness refuses other save paths, but replaces the review character’s skills, stats and inventory and creates a small platform. It does not reset the persistent mentorship ledger. Do not use a valued save.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null ./gradlew runClient -I docs/review-tyche/client.init.gradle
```

The client checks normal and compact widget bounds, a confirmation held through repeated network refreshes, actual synthesis payment and parent replacement, Dividend, timed Against All Odds, the native Ultimate selection screen, and a real fatal player hit. It verifies full HP/SHP/aura/magicule restoration and the other cooldown resets while retaining the insurance timer. Screenshots are saved in `/workspace/validation-tyche`; `passed.txt` is written only after all checks. An error writes `failed.txt` and stops the client. A successful Gradle exit alone does not establish a review pass.

The initial click simulation was corrected to render native SimpleButton hover state before clicking. The reused review player also had an old entity invulnerability flag from earlier previews; the harness explicitly clears it before the real damage test. These were fixture issues, not alterations to production damage rules.

Reviewed captures: [normal synthesis](../../../../wiki/media/tyche-synthesis.png), [compact synthesis](../../../../wiki/media/tyche-synthesis-compact.png), [confirmation](../../../../wiki/media/tyche-confirmation.png), [Against All Odds](../../../../wiki/media/tyche-odds.png), [passive timers](../../../../wiki/media/tyche-odds-status.png), [revival](../../../../wiki/media/tyche-revival.png), [live history](../../../../wiki/media/tyche-insurance-history.png), [native Ultimate menu](../../../../wiki/media/tyche-native-ultimate-menu.png). Bounds remain inside both 640×360 and 320×180 GUI layouts, text is contained, the price is legible, confirmation remains stable, and the removed field is absent and revival remains visible. The live status page shows Against All Odds duration and cooldown.
