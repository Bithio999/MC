# Fusion preview and client review

Use only a disposable copied save named `Fusion Preview` in `/workspace/validation-fusion/client/saves`. The fixture checks that folder before editing. It changes skills, inventory, EP, difficulty and gamerules, clears test actors within 512 blocks, builds an arena, performs Fusion and combat tests, and saves the altered review world. Never point it at a player save you want to keep.

With Java 21 and a 1280×720 X11 display:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient -I docs/preview-fusion/preview.init.gradle
python3 docs/preview-fusion/assemble_preview.py /workspace/validation-fusion/capture ../../wiki/media/fusion.mp4
```

The recording uses the actual Fuse press handler and native skill slots. It verifies combined EP, borrowed Aura Slash, 2× physical/magical/direct Spiritual damage, early separation, original EP and loan removal. The merged form is moved using native player input. `capture-metrics.json` records verified values. Captions and title cards are added to genuine footage; no gameplay frames are replaced. Recording is muted. A failed check writes `failure.txt` and prevents assembly.

To review the real consent menu and separation button:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient -I docs/preview-fusion/preview.init.gradle -PfusionConsentReview
```

The inviter uses a server-side test player; the receiver is the connected native client. The fixture clicks the actual Accept button, validates control after the reply packet, then clicks Pause → Separate Fusion. Results and screenshots are written to `/workspace/validation-fusion/ui-review`. Both fixture classes are excluded from normal builds.

To verify borrowed skills through Tensura's actual Ability Selection screen for both control roles:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient -I docs/preview-fusion/preview.init.gradle -PfusionMenuReview
```

This checks combined HP/SHP, actual list selection in Skills → Unique and Battlewills, native slot packets accepted by the server, and removal of borrowed entries afterward. Results/screenshots are saved under `/workspace/validation-fusion/menu-review`. The player donor is a server-side test player. The menu-review fixture is also excluded from production builds.
