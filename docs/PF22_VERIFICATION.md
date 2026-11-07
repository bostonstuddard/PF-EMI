# PF-EMI pf.22 — verification report

**Scope:** PF-EMI source-only update for ProjectFlint v0.31.68 and user-specified v0.31.69 changes. Published upstream comparison: ProjectFlint 0.31.68-b commit `acd9a193508efa3ebe2bcfdfc86a5e88f8e2e2b0`. No ProjectFlint gameplay source included.

## Completed

- [x] Read ProjectFlint `WovenChestBlock.useItemOn` and confirmed the Wooden Chest Frame interaction consumes the frame and keeps contents.
- [x] Read the released 0.31.68 Dynamite recipe / block implementation and the current published mining-tier predicate.
- [x] Implemented version-specific presentation for the user-specified 0.31.69 Dynamite stats, including a 1.25× outcome multiplier and overrolls.
- [x] Kept the new Hardwood recipe tied to its authoritative custom 3×3 recipe type or bundled gameplay recipe JSON, rather than inventing a fake craft.
- [x] Version-gated pickaxe recovery references for Packed Mud and Mud Bricks.
- [x] Passed 33/33 offline source checks (`python tools/verify_pf_emi_pf22.py`).
- [x] Parsed the modified Java file with a Java 21 compiler AST parser: 0 syntax errors.
- [x] Validated patch ZIP test CRC, source-relative paths, and file contents.

## Not completed

- [ ] Full Gradle compile: unavailable. Attempted `./gradlew :xplat:compileJava :fabric:compileJava --offline --no-daemon`, but the Gradle 9.2.1 distribution was not present and its distribution host could not be resolved (`java.net.UnknownHostException: services.gradle.org`). This is a build-environment failure, not proof of a successful compilation.
- [ ] In-game EMI recipe-tree, transfer (+), A/U lookup and crafting UI checks on Minecraft 1.21.11 Fabric.
- [ ] Check against final ProjectFlint 0.31.69 recipe JSON and actual gameplay after upstream publication.
- [ ] Multiplayer validation and actual Wooden Chest inventory-preservation playtest.

## Important

The user requested **PF-EMI only**. The preview does not modify Dynamite damage, actual blast radius, Stone mining, recipes, or item drops in ProjectFlint. A PF-EMI recipe view cannot make a missing gameplay recipe craftable.
