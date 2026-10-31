# Native Schema progress review

Optional client fixture; excluded from production. Use Java 21, a display and a disposable world. It selects detached skill instances without editing player skills or stats, then opens the real Tensura Battlewill menu. Checks learning at 0/1000, 500/1000 and 999/1000; mastery at 0/2000, 1000/2000, 1999/2000 and 2000/2000; and the unchanged Aura Slash learning tooltip. Captures unedited screenshots and writes `schema-progress-review/passed.txt`. Normal world ticking/saving may occur.

Full project: copy a disposable `Myths Validation` world into `/workspace/validation-schema-learning/full-client/saves`, then use `gradle clean runClient -I docs/review-schema-progress/client.init.gradle`.

Standalone: use the prepared Neo Otherworld client with a disposable `Schema Validation` world and `gradle clean runClient -PneoOtherworldDirectory=/path/to/pack -PconnectorCleanPath=/path/to/clean-mapped-minecraft.jar -I docs/review-schema-progress/client.init.gradle`.
