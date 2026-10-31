# 1.2.0 — In-game guide

A standalone-specific nine-page guide covers the lineage, Chained, Unbound, Schema, Air Walk, Instant Transmission, recovery and the spear. Open with F8, pause/status buttons, or `/restrictionguide`. It is offline, searchable and pauses single-player while reading.

The full and standalone catalog/search tests cover link validity, fixed progression, name/body matching, categories and empty states. Real Neo Otherworld client checks cover F8, menu entry points, navigation and a 640×480 window. Production isolation and review-fixture exclusion are audited. [Full validation](../../../docs/VALIDATION-0.36.0.md).

Both real-client reviews passed; the standalone world review confirmed the real F8 mapping, both menu buttons, correct parent restoration and a 640×480 layout. The pack dedicated server passed 55 integration cases. The normal standalone build includes seven catalog/search JUnit tests, while full Myths runs 18 JUnit tests and 25 dedicated-server checks. Production packaging excludes review fixtures and full-Myths-only pages.

Final normal builds, artifact hashes, catalog/class inclusion and the independent standalone distribution audit passed. Total: **105 automated checks**, plus both real-client GUI reviews.
