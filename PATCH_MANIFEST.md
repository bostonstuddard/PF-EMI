# PF-EMI 1.1.23-pf.17 — PATCH MANIFEST

Target PF-EMI source: `1.1.23-pf.16`, Minecraft `1.21.11` Fabric.
ProjectFlint source reference: `0.31.55-b`, GitHub commit `54184ab574bc1999ad616d91518d4681365fca96`.

Unzip into the **existing PF-EMI repository root**, overwrite modified files, then run `APPLY_PF_EMI_PF_17.bat`.
The patch is not a complete source checkout or runnable JAR.

## Source changes

- `APPLY_PF_EMI_PF_17.bat` — SHA256 prefix `6bbbbaa9d12f0c8f`
- `CHANGELOG.md` — SHA256 prefix `accd1d76f01d9c18`
- `README.md` — SHA256 prefix `1fafdc23f78a23e1`
- `build.bat` — SHA256 prefix `bf4eeea8b9ba97d9`
- `docs/PF17_PROJECTFLINT_0.31.55_PARITY.md` — SHA256 prefix `e5f5c20b1eac6e5f`
- `docs/PROJECTFLINT_EMI_AUDIT_CHECKLIST.md` — SHA256 prefix `b4cf69610ab5c491`
- `docs/RECIPE_CATEGORY_AUDIT.md` — SHA256 prefix `77666b77bc700022`
- `docs/VERIFICATION_REPORT.md` — SHA256 prefix `a65315ee3045ddcf`
- `gradle.properties` — SHA256 prefix `3d52d7507422b819`
- `tools/verify_pf_emi_pf17.py` — SHA256 prefix `6f9d942af04164e9`
- `xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java` — SHA256 prefix `410980626574306d`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintLootRecipe.java` — SHA256 prefix `beb1f03f1bb8f2a2`
- `xplat/src/main/resources/assets/emi/lang/en_us.json` — SHA256 prefix `edc7ceb5b5d35606`
- `xplat/src/main/resources/assets/pf_emi/lang/en_us.json` — SHA256 prefix `cc1b1b45dec30219`

## Validation

- 53/53 source-contract assertions passed.
- Two edited Java files parsed with zero JDK 21 syntax errors.
- Gradle compilation and in-game singleplayer/dedicated-server tests were **not** possible here (uncached Gradle 9.2.1; outbound connection unavailable).
- Source-derived behavior is documented in `docs/PF17_PROJECTFLINT_0.31.55_PARITY.md`.
