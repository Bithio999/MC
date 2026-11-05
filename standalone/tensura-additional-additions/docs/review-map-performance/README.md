# Map performance and clarity review

Use Java 21 and the normal baseline runtime dependencies. This opt-in profile accepts only the disposable normal-generation save at `/workspace/validation-marauders-map/client/saves/Marauders Map Review`. It changes that review character’s race and skills when the clarity exercise is enabled. Never use a personal save.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null ./gradlew clean runClient \
  -PmapBenchmark=after -PmapClarity=true \
  -I docs/review-map-performance/client.init.gradle
```

Without `-PmapClarity`, the profile benchmarks native-generator calls and exits. The benchmark samples the same previously unvisited Overworld tile near 300,000 / −310,000 at steps 1 and 8. Each cold call runs once, followed by two warm relief calls. It checks the complete 1,024-pixel payload. It does not request chunks or write terrain. Output: `/workspace/validation-marauders-map/performance-after.txt`.

The optional clarity exercise opens the real networked map, centers an unexplored minimum-zoom viewport at 332,144 / −342,144, measures first complete preview coverage and complete refinement, checks no distant full chunk was loaded, and closes/reopens the map. It asserts cached previews are still present before recentering, then reviews recorded terrain at minimum zoom at GUI scales 1 and 2. It compares a displayed recorded mip texel with the corresponding full-resolution native block pixels. Captures and `passed-map-performance.txt` are written to `/workspace/validation-marauders-map/capture`. A failure writes `failed-map-performance.txt`.

The historical baseline was measured from the old implementation using the same generator, seed, coordinates, native dependencies and call order. The coarse baseline sampled 8×8 points and interpolated colors; the new coarse stage samples native biomes at each output pixel. The full baseline sampled all native height columns; the new relief stage samples a cached height grid while retaining independent per-pixel biome colors. These are progressive previews, not claims of exact ungenerated block surfaces. Compare both speed and the different approximation stages.

Also run the existing `docs/review-marauders-map` UI and automatic-survey profiles. They check dimension tabs, shared exploration, the complete structure browser and native validation of an unexplored structure start. Clean between profiles and before producing a release; the distribution audit rejects all review classes and resources.
