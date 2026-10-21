# Aion: cinematic singularity references and animation

Research and implementation: 20 October 2026. References were retrieved directly
from their publishers. No movie frames, NASA images or game artwork are packaged
as mod textures; all effect artwork is original procedural work.

## Black holes in media: Interstellar / Gargantua

The primary visual reference is **Gargantua in Interstellar**, verified against
[DNEG’s production page](https://www.dneg.com/show/interstellar) and its linked
production still. The accompanying research by the film’s VFX/science team is
[James, von Tunzelmann, Franklin and Thorne: Gravitational Lensing by Spinning
Black Holes in Astrophysics, and in the Movie Interstellar](https://arxiv.org/abs/1502.03808)
([full paper](https://arxiv.org/pdf/1502.03808)). Sections 4.1–4.2 and figures 13–16
explain both the physical images and the deliberate cinematic changes.

Concrete observations and implementation choices:

- **A dark, readable shadow inside a broad disk.** A few thin wire-like rings do
  not reproduce the disk’s visual mass. The new annulus spans 1.08–2.73 core radii
  with six radial strips, continuous striated texture coordinates and restrained
  additive emission over a full-bright translucent surface.
- **The rear disk appears above and below the shadow.** The paper’s multiple
  lensed disk images inform two camera-dependent arcs. They are geometric
  approximations, not extra tilted physical disks.
- **Brightness is an artistic choice.** Section 4.2 describes suppressing strong
  physical Doppler/frequency-shift asymmetry to keep the film image readable.
  This effect similarly favors warm ivory/gold with mild blue asymmetry rather
  than claiming that a saturated two-color disk is the film’s exact treatment.
- **Soft veiling flare accompanies fine structure.** A low-intensity halo and
  sharp inner photon rim give the disk an emissive edge without washing out
  the horizon. The collision gets a brief original starburst, followed by
  pressure fronts and outward ejecta rather than a sustained white screen.
- **A disk is not a steering wheel.** Its plane is fixed from the per-hole seed
  in world space. Neither flight heading nor speed deformation rotates it.
  Only its material coordinates drift gently; view-dependent lens arcs respond
  to the camera rather than the projectile’s velocity.

[NASA’s black-hole visualization](https://www.nasa.gov/universe/nasa-visualization-shows-a-black-holes-warped-world/)
is the supplemental physical reference for shadow, photon ring, disk warping
and approaching/receding brightness. This renderer uses native shaders and
lensing-inspired meshes. It does **not** ray-trace or refract Minecraft’s world
framebuffer and requires no shader pack.

## Motion and impact animation

[Adobe’s twelve animation principles](https://www.adobe.com/creativecloud/animation/discover/principles-of-animation.html)
inform staggered staging, anticipation, curved paths, acceleration, and
volume-preserving squash/stretch. Twenty airborne origins and Bezier controls
are reproducible from the shared seed. A separate temporal map
`u² × (0.78 + 0.22u)` accelerates toward impact, informed by
[MDN’s easing reference](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/easing-function/cubic-bezier).
[Three.js’s curve documentation](https://threejs.org/docs/pages/Curve.html)
informs the distinction between curve parameter and measured tangent speed.

Flights last 10–15 ticks, with independent arrivals at ticks 32, 34, …, 70.
The endpoint is continuously updated from the target’s current position, including
final approach. There is no six-tick lock or formation-radius tether. Render-frame
fractional ages and the client’s interpolated target position keep the animation
continuous between server updates. Each completed impact records its own location,
so existing explosions never slide after a moving target.

The core’s longitudinal scale comes from measured curve velocity; transverse
scale is `1 / sqrt(longitudinal)` to preserve volume. The disk is excluded from
this deformation and orientation. Tapered trails sample the same curve.

Each impact has a 32-tick tail: fast ignition, a distinct optical burst, expanding
camera-facing and world-oriented pressure fronts, and curved ejecta. Outer light
expands to 10 / 12 blocks while gameplay damage remains 3.75 / 4.5 blocks. The
large light footprint is deliberate cinematic exaggeration, not a physical
black-hole evaporation simulation or additional damage.

## Native damage and synchronization

One visual entity synchronizes seed, server cast time, target UUID/entity ID,
latest target point, completed-impact mask and twenty fixed collision points.
Pending holes always track; only completed detonations stop moving. Loaded,
eligible same-dimension targets can move beyond the original formation. Lost
or dead targets leave the last tracked endpoint; no chunks are forced.

Each collision calls native `LivingEntity.hurt` with its own native
`tensura:space_elemental` skill source and full Aion instance/mode metadata.
A per-source opt-in mixin answers Minecraft’s `BYPASSES_COOLDOWN` tag only for
these collision sources. It does not globally tag Space damage, reset a victim’s
hurt timer, write health, or alter unrelated hits. This allows all twenty holes
to register separately, including hits two ticks apart. Native authority,
resistance handling, Godly ownership traits and friendly/PvP checks still apply.

Fixed raw budgets remain twenty times 480 / 600 = 9,600 / 12,000, before mastery
and shared Godly traits. No target means no cost, cooldown, mastery or Horizon
consumption. Cleanup cancels pending damage and locates owned visuals across
loaded dimensions.

Geometry is batched by material and distant nodes use fewer segments. Reduced
particles remove optional ejecta rather than holes, disks or primary pressure
fronts. Expanded culling bounds include the current destination. The effect
never destroys blocks or changes the player’s camera.
