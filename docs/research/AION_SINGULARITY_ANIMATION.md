# Aion: twenty-singularity animation research

Research and implementation: 20 October 2026. References were retrieved directly
from their publishers; no external artwork or game textures were copied.

## Animation references and their application

[Adobe: The 12 principles of animation](https://www.adobe.com/creativecloud/animation/discover/principles-of-animation.html)
explains anticipation, arcs, staging, timing, follow-through, ease-in/ease-out,
and preserving volume under squash and stretch. This finisher applies those
principles as follows:

- **Staging and timing:** twenty spatially separated holes appear with small
  offsets in birth time. Their dark silhouettes remain readable inside the
  luminous bands. Four-hole salvos make the convergence legible without turning
  every impact into one uninterrupted white flash.
- **Anticipation:** an approaching launch compresses the horizon slightly along
  its departure axis. The compression relaxes continuously into acceleration. The body already faces
  the departure tangent while eased launch speed is zero, avoiding an orientation
  snap at the handoff.
- **Arcs:** every hole has two seeded cubic Bezier handles, its own launch time
  and its own flight duration. The controls first pull the hole outward/tangentially,
  then curl it into its assigned blast position.
- **Squash and stretch:** the actual curve velocity determines the longitudinal
  deformation. The two transverse dimensions use `1 / sqrt(longitudinal)`, so
  their product with the longitudinal scale stays one. It is not a timed pulse
  unrelated to the motion.
- **Follow-through:** the wake samples the same flight curve over its preceding
  6.5 ticks. It tapers behind the body; the explosion compresses inward before
  its pressure rings expand and its finer streaks dissipate.

[MDN: cubic-bezier()](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/easing-function/cubic-bezier)
describes separate temporal easing with control points. The implementation
separates the spatial curve from the temporal map `u² × (0.78 + 0.22u)`:
launch speed begins near zero and grows into the collision, rather than easing
slowly to a stop just before the blast. Positions are evaluated at render-frame
fractional ages; the server's 20 Hz tick is not the animation frame rate.

[Three.js: Curve](https://threejs.org/docs/pages/Curve.html) documents tangents,
curve evaluation and arc-length sampling. Its distinction between a curve's
parameter and distance traveled informs the velocity calculation here. This
implementation deliberately accelerates; it does not claim constant arc-length
speed. Centered finite differences measure the actual speed used for deformation.

## Optical reference

[NASA: Visualization Shows a Black Hole's Warped World](https://www.nasa.gov/universe/nasa-visualization-shows-a-black-holes-warped-world/)
explains the dark shadow, thin photon ring, warped rear view of an accretion disk,
and the brightness asymmetry between approaching and receding material.

The renderer uses an opaque 3D horizon, a screen-projected photon ellipse matching
its deformed body, tilted accretion bands, a raised rear-disk arch, and warm/blue
brightness variation. Fine striations come from an original periodic texture
created by `scripts/prepare-singularity-effects.py`. The disk combines a controlled translucent emissive layer with selective
additive emission so overlapping bands retain detail; photon rings,
wakes and impact optics use additive light separately.

This is a lensing-inspired geometric effect, not ray tracing or refraction of the
actual world framebuffer. It works with native Minecraft shaders and needs no
external shader pack. It does not change the player's camera or require a
fullscreen post-processing pass for each of the twenty holes.

## Integration and performance decisions

- One synchronized visual entity carries the seed, authoritative cast time,
  tracked target identity and five salvo destinations. Twenty independent
  networked projectiles are unnecessary.
- A salvo's final destination locks six ticks before impact. Client rendering
  follows the interpolated target before that lock; damage uses the authoritative
  locked position. The same seeded offset defines each visible detonation and
  its damage area.
- Four blast contributions are summed into one native hit per recipient every
  twelve ticks. No code resets `invulnerableTime`, assigns target health, or
  bypasses native hit-immunity timers to manufacture twenty accepted hits.
- The fixed budgets remain 9,600 raw damage, or 12,000 when empowered by consuming
  Event Horizon. Mastery and the existing Godly classification traits are separate.
- Geometry is batched by material. Distant core meshes and ring/trail segments
  use lower detail. Particle settings reduce fine sparks, while all twenty bodies,
  their flight paths and the impact silhouettes remain.
- Entity lifetime, range tether and culling bounds are finite. Cleanup cancels
  damage and locates owned visuals across dimensions; the renderer cannot hurt
  entities, force chunks or edit terrain.
