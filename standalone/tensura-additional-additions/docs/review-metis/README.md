# Metis actual-client review

Opt-in harness; excluded from production builds. Use only the disposable `Wayfarer Review` save in `/workspace/validation-wayfarer/client`. The harness refuses other save paths. It replaces this test character's magic/Metis, grants review skills and resources and synthesizes Metis; never use a valued save.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null ./gradlew runClient -I docs/review-metis/client.init.gradle
```

Checks native synthesis/confirmation, a real magic assignment packet, three/four-thread capacity, mixed skill/magic assignment through the Skills filter, a Blink-only Spatial Motion dropdown, and normal/compact bounds. It verifies that both native container-open overloads are blocked inside an automatic callback while ordinary manual containers still open. Native Ultimate selection is captured too. GUI scale and HUD visibility are restored after successful review.

Output: `/workspace/validation-wayfarer/capture`. A successful review writes `passed-metis.txt`; any error writes `failed-metis.txt` and stops. Gradle's exit alone does not establish that all client checks passed.

Current illustrations: [skill assignment](../../../../wiki/media/metis-skill-assignment.png), [permitted modes](../../../../wiki/media/metis-safe-skill-modes.png), [compact matrix](../../../../wiki/media/metis-compact-matrix.png). The selected skill occupies the fourth thread alongside three magics. Spatial Motion's dropdown contains Blink, with no Warp menu entry. Panels, icons and labels were inspected; small rows omit the decorative stripe to keep it from crossing text.
