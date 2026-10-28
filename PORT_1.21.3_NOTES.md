# PF-EMI 1.21.3 - Initial Forward Port

Initial forward-port baseline from the working 1.21.2 source.

Changes in this pass:

- Minecraft target: `1.21.2` -> `1.21.3`
- Yarn mappings: `1.21.2+build.1` -> `1.21.3+build.2`
- Fabric API: `0.106.1+1.21.2` -> `0.106.1+1.21.3`
- NeoForge: `21.2.0-beta` -> `21.3.0-beta`
- Fabric metadata now requires Minecraft 1.21.3.
- NeoForge metadata now accepts the 21.3.x loader line.
- Resource pack format remains 42 for 1.21.3.
- Clean builder filenames automatically become `PF-EMI-1.21.3.jar` and `PF-EMI-1.21.3-NEOFORGE.jar`.
- The complete 1.21.2 compatibility code is retained as the baseline because 1.21.3 is a bug-fix follow-up to the same rendering/recipe generation.

Next step: run `build.bat` and use the first compiler output as the authoritative API delta for the 1.21.3 pass.
