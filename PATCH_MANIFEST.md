# PF-EMI pf.19 — patch manifest

Base: PF-EMI v1.1.23-pf.18 with patch chain applied. Minecraft 1.21.11, Fabric / Java 21.
Upstream reference: ProjectFlint v0.31.61-b, commit `8d8bd1533bf3a1d571ea062a0024fb2c74831aae` (supersedes requested v0.31.60-b).

## Installation

Extract this ZIP directly into your existing PF-EMI project root. No extra parent folder. Run `APPLY_PF_EMI_PF_19.bat` or `build.bat` after extraction.

## Validation scope

- 39/39 source-level regression gates passed.
- 9 Java units passed Java 21 JavacTask parser checks; this is **not** bytecode compilation.
- Full Gradle build failed before configuration because `services.gradle.org` was unreachable (Gradle 9.2.1 could not be downloaded).
- UI, recipe transfer, recipe trees and multiplayer behavior require client/server testing.
- Native custom-meat cooking JSON still exists in ProjectFlint. The EMI display filter does not alter its gameplay.

## Files

- `APPLY_PF_EMI_PF_19.bat` — 782 bytes; SHA-256 `84dfee05b42c8d251483f74bb74b52d74eddfff3e0f6a565fa43040817229903`
- `CHANGELOG.md` — 22106 bytes; SHA-256 `1b161fd62b57ae0ca3d48035ad4ce9233b91240a763996dad353986b8d1607ca`
- `README.md` — 5288 bytes; SHA-256 `3139a86d0f60e6f5cd6b8b8a01a74fc7854439434beeb07da6cdf13dc4ab0eae`
- `build.bat` — 2121 bytes; SHA-256 `21c21baf82c3239d6eab01bf29786625fb2b9ad6d7aba1c4ef38751763d02fe9`
- `gradle.properties` — 562 bytes; SHA-256 `e8b22fd72589ea486950e64ac1218687bed8943882ace2cfb489b3010d9c8b5d`
- `docs/PF19_PROJECTFLINT_0.31.61_PARITY.md` — 3467 bytes; SHA-256 `12a9a932e0e6a78d3c561898fdf4b4d1b857f9632265dceb85d47dc115f4bd12`
- `docs/VERIFICATION_REPORT.md` — 4611 bytes; SHA-256 `7d24c7d4bd849d32f2935b968c895723a0c5e60daa005b4cf9f52b297ad913f1`
- `tools/verify_pf_emi_pf19.py` — 4741 bytes; SHA-256 `12f9f9cbbb215bb3cf3c58c39f5d00b528162d8403d8b79c3ecbed731744f87d`
- `xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java` — 115169 bytes; SHA-256 `70db10be524fcda75a95c289222b18dcbbf73f1994910e5738d6b0b2ef056c6a`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintGridRecipe.java` — 4902 bytes; SHA-256 `5b616771867fac8159a65e925b758b02ce1e3317a8b66e2ef86e4aa32ed854e1`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintProcessRecipe.java` — 4088 bytes; SHA-256 `69eb1bc7764e77c58887ed5fd83bd1119293ecb59f6a93494ec0f14f665f7073`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintBasinRecipe.java` — 2593 bytes; SHA-256 `14ef02fc27d14dacfa1386bc924f5425c2aa871a3f7bbf8c8f91d48514e8424d`
- `xplat/src/main/java/dev/emi/emi/recipe/ProjectFlintLootRecipe.java` — 5797 bytes; SHA-256 `cf318f7180acd885bdb620995a03e8b4448ffc61eb9740f5fb888fdf0f6a199c`
- `xplat/src/main/java/dev/emi/emi/handler/InventoryRecipeHandler.java` — 3839 bytes; SHA-256 `347b0b0efe6f9aa53330dad5dbf64a6b97a4b5ab028546382c451019c9492793`
- `xplat/src/main/java/dev/emi/emi/handler/CoercedRecipeHandler.java` — 2510 bytes; SHA-256 `ce159b4c6d4efc21e41995c791be15a8de83b76fee98efb490858b655d8b2fc0`
- `xplat/src/main/java/dev/emi/emi/handler/ProjectFlintStationRecipeHandler.java` — 2029 bytes; SHA-256 `f9752fb01d9c20653b037f763b0a9d6fb0115670ab98ca3cbd9a5e8d23e7d163`
- `xplat/src/main/java/dev/emi/emi/registry/EmiRecipeFiller.java` — 13037 bytes; SHA-256 `1672898b020b9d7bf979733ecc036518b5efc687b954989640e51740e9528246`
