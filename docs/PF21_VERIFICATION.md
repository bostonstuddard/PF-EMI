# PF-EMI pf.21 — Verification

**Target:** planned ProjectFlint v0.31.68-b; current source baseline is v0.31.67-b at commit `b0d13129e38fdc3e38fd373bce97e9d41cbebc90`.

## Completed

- `python tools/verify_pf_emi_pf21.py` — 21/21 static/source-contract/arithmetic checks passed.
- Java 21 compiler's `JavacTask.parse()` — `ProjectFlintPlugin.java` parsed as one compilation unit, zero syntax errors. This is **syntax validation, not a successful Java compilation**.
- The patch is limited to updated PF-EMI source plus metadata, docs, tests and apply/build scripts; no ProjectFlint gameplay classes are replaced.
- The archive is checked for top-level source-relative paths, file integrity and duplicate names.

## Not completed

- Full `:fabric:compileJava`: Gradle wrapper attempted to download `gradle-9.2.1-bin.zip`; the machine cannot resolve `services.gradle.org` (`UnknownHostException`).
- Runtime client/server validation, EMI A/U navigation, 3×3 crafting fill, future Dynamite yields and Dripstone mining behavior.
- Audit of the final v0.31.68-b source, because that push was not on the GitHub main branch at patch preparation time.

**Important:** The 20% chance boost is applied to EMI's *displayed* yields only when the installed ProjectFlint mod reports v0.31.68 or later. PF-EMI does not alter the explosion radius, block-breaking permissions or actual gameplay loot rolls. Those are changes to ProjectFlint itself.
