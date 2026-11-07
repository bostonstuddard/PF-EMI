# PF-EMI v1.1.23-pf.21 — patch-only manifest

**Apply over:** PF-EMI v1.1.23-pf.20 source. **Platform:** Minecraft 1.21.11 Fabric, Java 21. **Reference upstream:** ProjectFlint v0.31.67-b; planned v0.31.68-b changes were supplied by the user, not yet present on main.

- Changed source: `xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java` — 20% proposed blast-yield bonus with overroll, conditional activation only for ProjectFlint >=0.31.68, compact placement descriptions, no-drop Dripstone guide on supported versions.
- Shaped Dynamite recipe: already read from ProjectFlint's *actual* 3×3 recipe; no premature hard-coded alternative. Planned shape documented as PFP / PRP / CCC.
- Updated: version/build script, consolidated changelog, README.
- New: `APPLY_PF_EMI_PF_21.bat`, version-pinned preview parity documentation, verification report and regression checks.
- ZIP entries are at source-relative paths, with **no nested parent folder**, compiled files or caches.

**Status:** Source syntax/static checks passed; full Gradle compile and in-game test were not possible. This patch does not change ProjectFlint's actual mining or explosions.

## File list (excluding this manifest)

- `APPLY_PF_EMI_PF_21.bat` — 789 bytes — SHA256 `68c9208508e3ee96abdba9843a00fffcd224ab3ac1070456fdff9236bfe18c28`
- `CHANGELOG.md` — 25,154 bytes — SHA256 `7a609f13738d8cc9bcf39ac84c640b31349b956d82b47ad61ea6aad455e6bce8`
- `README.md` — 6,593 bytes — SHA256 `7296e6c34a1445a1b56db6a7e7ea5dd8514848c3fa181a8550d169bba94a00fd`
- `build.bat` — 2,121 bytes — SHA256 `1f0ba886938ba5920eb1f0cee5a1ae4f3cad34ff6dce2762a26f11df3f4b5ccb`
- `docs/PF21_PROJECTFLINT_03168_PREVIEW.md` — 4,196 bytes — SHA256 `f9904ca34bc2eba7a8d8d49439bf06d28fea3913bcd182a93290bd0993d14406`
- `docs/PF21_VERIFICATION.md` — 1,439 bytes — SHA256 `49a60d55f3c980e3a263176ea19d03e488c698b6fed7806e0f8247e0a0df6af3`
- `gradle.properties` — 562 bytes — SHA256 `cc20f588862eb6472a014c6659504f8a393dfe7269336619705c14e5bf9d964e`
- `tools/verify_pf_emi_pf21.py` — 3,357 bytes — SHA256 `4edefaf386872d2eb10957d6640f11211e03852784988ed0882ed11b87b73526`
- `xplat/src/main/java/dev/emi/emi/ProjectFlintPlugin.java` — 124,380 bytes — SHA256 `c46175dc0cb0716bfd3ce2558e8899020aa59c0204335e6b2fc1c3c4f6a713d4`
