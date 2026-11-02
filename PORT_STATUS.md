# PF-EMI Port Status

Current target: **Minecraft 1.21.6**

Port base: working Minecraft 1.21.5 pass-4 tree.

## Minecraft 1.21.6 — Pass 1

- Retargeted Fabric, xplat, and NeoForge metadata to Minecraft 1.21.6.
- Updated Yarn to `1.21.6+build.1`, Fabric API to `0.128.2+1.21.6`, Mod Menu to `15.0.0`, and NeoForge to `21.6.20-beta`.
- Updated resource-pack metadata to format 63.
- Preserved the Java-21-enforcing builder.
- Preserved clean release output naming: `PF-EMI-1.21.6.jar` and `PF-EMI-1.21.6-NEOFORGE.jar`.
- Ported the core 1.21.6 GUI rewrite: 2D `Matrix3x2fStack` transforms, GUI root-layer ordering, RenderPipeline-backed texture/sprite draws, ARGB text colors, and the new tooltip text path.
- Moved legacy EMI GUI transforms off the global model-view stack and onto `DrawContext` so they participate in the deferred GUI render state.
- Awaiting the first local Minecraft 1.21.6 compiler pass for remaining source-level API deltas.

See `PORT_1.21.6_NOTES.md` for the detailed pass notes.
