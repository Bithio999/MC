# 1.5.2 — World Jump cooldown

World Jump now starts a **300-second cooldown** when a valid transfer request is accepted. New-seed jumps and returns share this cooldown. World Select uses its own mode and remains available to browse or choose the next destination during that time.

The normal ManasCore skill cooldown is marked dirty before the save/reload transition. Current skill state travels with the character, so changing worlds does not reset the timer. The server rejects requests during cooldown, including direct calls to the transfer helper. Requests rejected because the library is full or a destination is unavailable do not start a cooldown.

No registry IDs or save formats change. Existing worlds are supported; this updates only TensuraAdditionalAdditions.

## Verification

- Build and **30 JUnit tests passed**.
- **158 native GameTests passed** on Tensura 2.0.1.3 / NeoForge 21.1.209, including native serialization of the 300-second timer and independent World Select availability.
- **Live standalone client passed:** B → A → B → fresh. Each arrival retained the active World Jump cooldown; another request during cooldown was rejected, while World Select still accepted return selections. The fixture clears the verified timer between scripted hops only, to avoid waiting ten minutes. Character, builds, numbering, cap, LAN and deletion checks also passed.
- Distribution audit and wiki link/anchor checks passed.

[Previous validation](VALIDATION-1.5.1.md).
