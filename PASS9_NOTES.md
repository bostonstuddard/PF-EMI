# PF-EMI 1.21.2 - Runtime Pass 9

## Rendering correctness

- Disabled EMI's legacy batched item renderer for the 1.21.2 port. The batcher was reconstructed from pre-render-rewrite code and did not preserve 1.21.2 GUI model/depth/lighting state consistently.
- All item icons now go through Minecraft 1.21.2 `DrawContext` rendering on Fabric and NeoForge.
- Removed the manual `DiffuseLighting.enableGuiDepthLighting()` call before normal item draws so item rendering no longer leaks lighting state into later EMI widgets.
- Tightened the tag fallback renderer so it draws only the representative icon; tag amount/remainder/tag overlays are rendered exactly once by the tag ingredient itself.

This pass targets the visual corruption/missing-model symptoms seen after the port first became runtime-stable.
