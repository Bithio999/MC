# Real-client Loan Shark review

Use a disposable copied save named **Loan Shark Review** under `/workspace/validation-loan/client/saves`. The fixture refuses any other save name, then resets the review player's race, skills, inventory and energy and places a subordinate near the origin. Do not use a personal save. Use Java 21 and an X11 display.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient -I docs/review-loan/client.init.gradle
```

A server-side test sender offers Prospector to the real connected player. The client receives the actual gift packet, captures its sender/skill/mastery-only invitation, and presses Accept through its native button, sending the real C2S nonce reply. The server checks mastery 550 and the contract marker.

The fixture selects Loan Shark in Tensura's actual Unique screen, opens the skill's Loan handler aimed at the prepared subordinate, searches for Schema and presses Send gift through the real menu packet. The server checks mastery 1,600, activates the copy and awards 240 permanent EP: 120 must remain with the borrower and 120 must reach the lender.

For a fast, repeatable claim check, **the fixture advances the ledger's server clock and samples usage programmatically**. It does not claim ten minutes of real play elapsed. The report captures actual server state at two GUI scales. A two-step Claim button sends the actual C2S request, recalls Schema and removes Hunter's Recovery from the target, giving it to the lender with mastery 321. Screenshots and pass/failure markers go to `/workspace/validation-loan/capture`.

These are actual gameplay/UI captures of a prepared scenario, not mockups. Opt-in review classes must be excluded from the release JAR.
