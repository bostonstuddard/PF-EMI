# PF-EMI pf.20 verification results

Target: ProjectFlint v0.31.67-b (`b0d13129e38fdc3e38fd373bce97e9d41cbebc90`).

**Static checks:** `python tools/verify_pf_emi_pf20.py` passed **26/26** checks. The edited `ProjectFlintPlugin.java` parsed successfully using the Java 21 compiler API (one compilation unit, zero syntax errors).

**Build status:** Gradle compilation was attempted with `:fabric:compileJava --offline --no-daemon`. It stopped **before project configuration** because `gradle-9.2.1-bin.zip` is not cached and `services.gradle.org` could not be resolved (`UnknownHostException`). This is not a successful build; run on the user's Windows Java 21 environment.

**In-game:** not tested. Pay particular attention to synced custom Hardwood durable recipes, Forge carbon processing across multiple charcoal charges, true original item roll odds on dynamite blasts, and the custom recipe-transfer handlers.

**No gameplay modification:** this ZIP updates PF-EMI only, not ProjectFlint itself. Any missing in-game recipes must be fixed upstream rather than hidden as fake PF-EMI crafting entries.
