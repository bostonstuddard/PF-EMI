# PF-EMI 1.21.3 Port Status

Target: Minecraft **1.21.3**, Java **21**, Fabric + NeoForge.

This tree is the forward port of the working PF-EMI 1.21.2 code line. It contains the complete Java source and carries forward the 1.21.2 recipe-sync, rendering, input-mixin, Fabric, and NeoForge compatibility work rather than reverting to upstream 1.21.1 behavior.

## 1.21.3 baseline

- Minecraft: `1.21.3`
- Yarn: `1.21.3+build.2`
- Fabric Loader: `0.17.3`
- Fabric API: `0.106.1+1.21.3`
- NeoForge: `21.3.0-beta`
- Architectury Plugin: `3.4.162`
- Architectury Loom: `1.11.458`
- Gradle: `8.14`
- Java: `21`

Minecraft 1.21.3 is a small follow-up to 1.21.2 and keeps resource/data pack format 42, so the existing 1.21.2 rendering and recipe compatibility layer is retained unless the 1.21.3 compiler/runtime proves a specific API changed.

## Carried forward from 1.21.2

- Full `RecipeEntry` synchronization through PF-EMI's custom recipe sync payload.
- Client-side synced recipe index used by EMI recipe handlers.
- 1.21.2+ ingredient, recipe-display, repairable-component, smithing, registry/tag, tooltip and DrawContext migrations.
- Transmute recipe support.
- Fabric and NeoForge recipe sync/fuel compatibility.
- JEMI disabled for this target because JEI has no 1.21.3 release line compatible with this fork.
- 1.21.2+ mouse/keyboard mixin targets using direct Screen input methods.
- Legacy item batching disabled; vanilla DrawContext item rendering is used for correctness on both loaders.
- Clean release builder output:
  - `PF-EMI-1.21.3.jar`
  - `PF-EMI-1.21.3-NEOFORGE.jar`

## First-pass validation

- Port metadata targets 1.21.3 on Fabric and NeoForge.
- JSON resources parse successfully.
- Java source structure checks pass.
- No active build metadata still targets Minecraft 1.21.2.

A full Gradle compile still needs to be run on the Windows development machine because this execution environment cannot resolve the Gradle/Maven dependency network. Any compiler errors from that build are the next porting pass.
