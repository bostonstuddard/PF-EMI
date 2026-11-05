# PF-EMI 1.1.23-pf.18 — Patch manifest

Apply to existing PF-EMI **pf.17** source; target ProjectFlint **0.31.57-b** (`7e7321a06daff837866c6ba95c4436f2e83c8824`).
No parent directory is included; all files are source-root-relative.

## Changed/new files

- `APPLY_PF_EMI_PF_18.bat` — SHA-256 `93ef6d945e3fb9e029b1afd05005b5e36f0d10eb981be9ac5e4a9d39435ab5a7`
- `CHANGELOG.md` — SHA-256 `5afbbc1626e81a88aa849c823f5bb6024615f2eba0c9b40294cfacc68c6a74bb`
- `README.md` — SHA-256 `78f5f5938ed600d36746e929b752ddc961150a4ef52eb7e20faef9ded293d264`
- `build.bat` — SHA-256 `fc008fe0619a9f64b08812b33418cd5b964f5d09fa7ecef900a4be1883313982`
- `gradle.properties` — SHA-256 `791beba17eec2d4d8a802c1d9251211b16d0a54612df59aec54f41ea999e4180`
- `docs/PF18_PROJECTFLINT_0.31.57_PARITY.md` — SHA-256 `fca998cc01d4c01ae96c72f4765e53e88a2ef68ca799d60c3905f7e75cf57e97`
- `docs/PROJECTFLINT_EMI_AUDIT_CHECKLIST.md` — SHA-256 `b5975fd2634118f86b3bf4209b0276bb0aa3501d2f250ea85285268bd11f55cf`
- `docs/RECIPE_CATEGORY_AUDIT.md` — SHA-256 `d5a43f828848ede2925c0ed76f6d4c813a0eff00a479e2b1ca77e2c21b57283c`
- `docs/VERIFICATION_REPORT.md` — SHA-256 `46aae4a3bdd9d5021da2f78e0b18f386851f7039e0801bd0cacfcae84babdabf`
- `tools/verify_pf_emi_pf18.py` — SHA-256 `f68a2b18fcd2427c3164166272ae54736481d66662f465986444a257048c33a4`
- `xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java` — SHA-256 `aa932b8d84f3683a620a9b454573c4a12f547ffa768e26b9984900b23ffd0f61`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintBasinRecipe.java` — SHA-256 `c9f6400a3e93c17bb19c290e2489cc6074a4927ae6c394eec4619caecbf79db2`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java` — SHA-256 `763db9a7d3f240d98ef8082abf9efcf44f5855ec84f377700cb8ed28dc1214c0`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintLootRecipe.java` — SHA-256 `c4a2e8e2f3cf4cfb63f7e05c54253eca5a04c766fed931a8d555097c6c53465b`

## Checks

- 62/62 source-contract checks passed (`python tools/verify_pf_emi_pf18.py`).
- Four changed/new Java compilation units parsed with Java 21 AST (zero syntax errors).
- Full Gradle build attempted, but Gradle 9.2.1 cannot be fetched (`UnknownHostException: services.gradle.org`).
- GUI/player-server runtime checks are outstanding; do not report a release-ready status.

## Installation

1. Extract this ZIP **directly into your existing pf.17 source root**, overwriting changed files.
2. Run `APPLY_PF_EMI_PF_18.bat` to clean generated outputs and build Fabric.
3. Test Basin hover odds, stomp counts, custom Hardwood recipes and Forge glass in a ProjectFlint 0.31.57-b client.

This is a **patch-only source archive**, not a deployable mod JAR.
