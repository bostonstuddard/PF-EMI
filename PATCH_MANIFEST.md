# PF-EMI pf.22 — patch-only source manifest

Extract directly into the **PF-EMI pf.21 source root**. Files are stored at their normal source-relative paths, without a nested ZIP folder.

- `xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java` — Wooden Chest world interaction; accurate blast/geology reference; version-gated 3s fuse, 1.25x yield, reach and stone-break odds; returned Water Bucket.
- `gradle.properties` — pf.22.
- `build.bat` — pf.22 JAR output/version.
- `APPLY_PF_EMI_PF_22.bat` — cleanup and build entrypoint.
- `CHANGELOG.md`, `README.md` — versioned release documentation.
- `docs/PF22_PROJECTFLINT_0.31.69_PARITY.md`, `docs/PF22_VERIFICATION.md` — parity and test status.
- `tools/verify_pf_emi_pf22.py` — source assertions.

No ProjectFlint gameplay source is included in this PF-EMI ZIP.
