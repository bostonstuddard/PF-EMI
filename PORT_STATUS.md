# PF-EMI Port Status

Current target: **Minecraft 1.21.5**

Port base: working 1.21.4 pass-4 tree.

## Pass 1
- Retargeted Fabric, xplat, and NeoForge build metadata to Minecraft 1.21.5.
- Updated Yarn, Fabric API, Mod Menu, NeoForge, and resource-pack format.
- Preserved the 1.21.4 recipe-stream, rendering, reload-listener, atlas, and Java-21 fixes.
- Preserved clean release output naming: `PF-EMI-1.21.5.jar` and `PF-EMI-1.21.5-NEOFORGE.jar`.
- Awaiting the first local 1.21.5 compiler pass for source-level API deltas.

## Pass 2
- Ported the first 1.21.5 rendering API breakage from the local compiler log.
- Removed stale `ShaderProgramKeys` and `BufferRenderer` dependencies.
- Migrated `VertexFormat.DrawMode` to Mojang's `com.mojang.blaze3d.vertex` package.
- Moved config-list primitive rendering onto `DrawContext.fill(...)`.
- Moved tinted sprite buffer submission onto `RenderLayer.draw(...)`.
- Removed obsolete direct Identifier-based shader texture binding.
- Awaiting the next local compiler pass.
## Minecraft 1.21.5 - Pass 4

- xplat compile confirmed clean by local build after pass 3.
- NeoForge `getTickDelta` calls migrated to `getTickProgress`.
- NeoForge enchantment applicability migrated from removed `isBookEnchantable` to `supportsEnchantment`.
- Awaiting next local `build.bat` result.

