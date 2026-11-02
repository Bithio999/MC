# 1.8.1 — Exponentiation without capacity bonuses

Exclude maximum HP, SHP, aura, magicules, absorption, oxygen bonus and water/lava capacity from the attribute lens. Remove the expanded current-aura/magicule hard limit. Positive healing/recovery and native earned pool awards remain enhanced, with normal effective maximums. Permanently earned pool awards are not reverted.

On the first active tick after upgrading, clamp legacy over-cap HP/SHP/aura/magicules and synchronize the storage. The migration marker prevents repeated clamping; saved bases are untouched. The ordinary unequip cleanup remains available.

Clean release build: **30 unit tests** and the distribution audit pass.

All **189 native GameTests** pass, including normal capacities, migration while still equipped, recovery at normal maxima, native skill-callback Fusion transfers and all prior regressions. The runtime registry audit measures each of the **55** remaining supported attributes using actual native getters on an owned/slotted test instance; every sample is exported as before/after data. Three is an artificial test input, not a player default.

The [video assembly source](preview-exponentiation/README.md) produces an animated reference covering all 55 attributes, tested output examples, unchanged pools and exclusions. It is explicitly labeled as a measured-values reference, not gameplay footage. No narration/audio. The [wiki](../../../wiki/pages/Additions-Exponentiation.md) embeds the complete video.

The updated real-client fixture passes native equip/unequip, unchanged HP capacity, retained attack-speed bonus and movement exclusion, healing/resource gains and NBT preservation. The native gameplay suite separately verifies legacy over-cap migration.

[Previous validation](VALIDATION-1.8.0.md)
