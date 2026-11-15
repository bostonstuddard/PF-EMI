# PF-EMI pf.24 Verification

**Baseline:** reconstructed PF-EMI pf.23 source (pf.11 full + pf.12–pf.23 patch overlays).
**Target:** ProjectFlint 0.32.3, GitHub commit `fe574310f03f33701dd481622f1655a4c70126bb`.

## Performed

- Source comparison: `docs/releases/0.32.2.md`, `docs/releases/0.32.3.md`, `RetiredRecipePolicy.java`, `RecipeManagerMixin.java`, `ClayGrinderRecipeGuideRegistry.java`, and Grinder menu changes from the pinned commit.
- Targeted PF-EMI source regression checks: `python tools/verify_pf_emi_pf24.py`.
- Java 21 parser: `JavacTask.parse()` on the modified `ProjectFlintPlugin.java` (syntax only).
- ZIP integrity, root-relative paths and exact changed-file comparison to reconstructed pf.23.

## Not confirmed

- Full Gradle compilation, Minecraft startup, in-game screen display, singleplayer/multiplayer recipe sync, and plus-button transfer. They require a working build environment and live server/client.
- This patch does not make ProjectFlint's v0.32.3 upstream compilation hotfix itself; use the updated ProjectFlint mod JAR separately.

## Current attempt results

- **46/46** focused source-contract checks passed.
- **1 modified Java file** parsed using Java 21 `JavacTask`: **0 syntax errors**.
- A full Gradle compile was **attempted but blocked**: Gradle 9.2.1 was not cached and `services.gradle.org` failed DNS lookup (`UnknownHostException`). This means neither type-checking nor an executable JAR was verified.
