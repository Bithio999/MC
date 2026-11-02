# 1.8.3 — Schema in the Battlewill Manual

Extend the resolved native `BattlewillManualItem.getRandomBattlewill` pool with Schema. Preserve all configured/resolved entries and avoid a duplicate when Schema is already configured. The ordinary random selection, book charge, effects, consumption and cooldown remain native. No new item or world generation is required.

Remove Schema's automatic per-second/login discovery callbacks. Existing learned instances, unfinished learning/mastery and saved trees are retained. The manual creates the ordinary learning instance at −1,000; it does not fully learn the skill or lower its requirement. The existing `discover` helper remains for opt-in review/test fixtures, without a production tick/login caller.

Native gameplay checks exercise the actual private random-selection method with a fixed random seed, native typed-book release/consumption/cooldown, exact −1,000 starting learning points, and preserving unfinished progress on another book attempt. Existing activation/progression tests still verify learning at exactly 1,000 points and mastery at 2,000.

Clean build, **30 unit tests**, distribution and wiki checks pass. All **192 native GameTests** pass on the baseline runtime. The 2.0.1.4 manual bytecode also retains the same pool-selection injection point.

[Previous validation](VALIDATION-1.8.2.md)
