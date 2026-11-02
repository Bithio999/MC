# Exponentiation client review

Use a disposable copied save named **Exponentiation Review** under `/workspace/validation-exponentiation/client/saves` and an X11 display. The fixture refuses a different save name. It resets the review player's race, skills, preset and base stats; never use a personal save.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient -I docs/review-exponentiation/client.init.gradle
```

The real connected player learns the skill, finds it in Tensura's actual Unique menu and equips/unequips through its native slot handler and packets. Checks cover translated name/icon, synchronized capacity/attack-speed attributes, unchanged movement, completed HP/aura gains, NBT load without free restoration, and capacity cleanup after unequipping. Captures and a pass/failure marker go to `/workspace/validation-exponentiation/capture`.

The fixture is opt-in and must be excluded from the release JAR. This is a supported-baseline client check, not a complete modpack runtime test.
