# Heavenly Restriction 1.0.8 validation

All **63 required native GameTests passed**, with Tensura Myths absent from the independent runtime. The real-player client resource review also passed.

```sh
./gradlew build runGameTestServer
python3 scripts/verify-distribution.py
```

Current MP is capped at the effective maximum, saved overfill is reconciled, and legitimate server maximum updates now reach the client. Native exhaustion values, initial birth setup and unrelated owners’ native resource rules are preserved.

Two native half-second regeneration pulses retain **1 magicule** and convert the rest of actual biome/rate/sleep output into aura. Full MP does not stop conversion; low natural rates are not boosted. Complete native recovery in an actual Ancient Forest biome is checked alongside normal aura regeneration.

Both forms of `/tensura edit stat @s ep max add 1000`, including the refill flag, give 900 raw aura capacity and **4,500 effective aura capacity**, with magicule growth blocked. Lowered EP and migrated save data retain the future 5× multiplier; the original birth roll remains saved separately. Ordinary native Humans keep their original command behavior.

The real client confirms **20,000** Ancient Forest area magicules, **10 → 11 MP** and **0 → 409 aura** over two native recovery pulses in the review configuration. Client and server maximum/current MP match at **76**, with total EP **5,076**. [HUD](media/chained-resource-status.png). Run the opt-in review with `gradle runClient -I docs/resource-review/client.init.gradle` in **Restriction Validation**.

Normal packaging excludes optional reviews, Myths classes and bundled dependencies; metadata, assets and every declared mixin class are audited. All prior movement, damage, learning, growth, recovery and flight regressions pass. [Full validation](../../../docs/VALIDATION-0.34.9.md) · [resource reference](../../../docs/research/CHAINED_RESOURCES_0.34.9.md).

Release: `releases/tensura-heavenly-restriction-1.0.8.jar` and checksum in the repository root. Install on client and server instead of Tensura Myths.
